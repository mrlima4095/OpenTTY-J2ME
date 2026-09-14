/* otp_main.c — CLI for OTP1 compress/decompress/info
 *
 * Usage:
 *   otp c <input> <output>   compress
 *   otp d <input> <output>   decompress
 *   otp i <file>             show header info
 *
 * Compile:
 *   ./build-elf.sh res/apps/src/otp_main.c res/apps/src/otp_compress.c \
 *       res/apps/src/otp_decompress.c -stdlib -o res/apps/dist/otp
 */
#include "otp.h"

/* ---- CRC32 (Castagnoli, bit-by-bit, no table) ---- */
unsigned int otp_crc32(const unsigned char *data, unsigned int len)
{
    unsigned int crc = 0xFFFFFFFF;
    unsigned int i, j;
    for (i = 0; i < len; i++) {
        crc ^= data[i];
        for (j = 0; j < 8; j++)
            crc = (crc >> 1) ^ (0xEDB88320 & (-(int)(crc & 1)));
    }
    return ~crc;
}

/* ---- read entire file into malloc'd buffer ---- */
static int read_all(const char *path, unsigned char **out, unsigned int *outlen)
{
    unsigned char *buf = 0;
    unsigned int cap = 4096, sz = 0;
    int fd, n;

    fd = open(path, O_RDONLY, 0);
    if (fd < 0) { printf("otp: cannot open '%s'\n", path); return -1; }

    buf = (unsigned char *)malloc(cap);
    if (!buf) { close(fd); printf("otp: out of memory\n"); return -1; }

    for (;;) {
        if (sz >= cap) {
            unsigned int nc = cap * 2;
            unsigned char *nb = (unsigned char *)realloc(buf, nc);
            if (!nb) { free(buf); close(fd); printf("otp: out of memory\n"); return -1; }
            buf = nb;
            cap = nc;
        }
        n = read(fd, buf + sz, cap - sz);
        if (n <= 0) break;
        sz += (unsigned)n;
    }
    close(fd);
    *out = buf;
    *outlen = sz;
    return 0;
}

/* ---- write buffer to file ---- */
static int write_all(const char *path, const unsigned char *buf, unsigned int len)
{
    int fd, written = 0;
    fd = open(path, O_WRONLY | O_CREAT | O_TRUNC, 0644);
    if (fd < 0) { printf("otp: cannot create '%s'\n", path); return -1; }
    while ((unsigned)written < len) {
        int n = write(fd, buf + written, len - (unsigned)written);
        if (n <= 0) break;
        written += n;
    }
    close(fd);
    return (unsigned)written == len ? 0 : -1;
}

/* ---- little-endian helpers ---- */
static void put32(unsigned char *p, unsigned int v) {
    p[0] = v; p[1] = v >> 8; p[2] = v >> 16; p[3] = v >> 24;
}
static unsigned int get32(const unsigned char *p) {
    return (unsigned)p[0] | ((unsigned)p[1] << 8) |
           ((unsigned)p[2] << 16) | ((unsigned)p[3] << 24);
}

/* ---- compress ---- */
static int do_compress(const char *inpath, const char *outpath)
{
    unsigned char *in = 0, *out = 0, hdr[OTP_HDRSZ];
    unsigned int inlen = 0, outlen = 0, crc;
    int r;

    r = read_all(inpath, &in, &inlen);
    if (r) return 1;
    if (inlen == 0) { printf("otp: empty input\n"); free(in); return 1; }
    if (inlen > OTP_MAXINPUT) {
        printf("otp: file too large (%u bytes, max %u)\n", inlen, OTP_MAXINPUT);
        free(in); return 1;
    }

    /* worst-case output: input + 1/8 flag bytes + header */
    out = (unsigned char *)malloc(inlen + inlen / 8 + OTP_HDRSZ + 64);
    if (!out) { printf("otp: out of memory\n"); free(in); return 1; }

    crc = otp_crc32(in, inlen);
    r = otp_pack(in, inlen, out + OTP_HDRSZ, &outlen);
    if (r) { printf("otp: compress failed\n"); free(in); free(out); return 1; }

    /* if packed is not smaller, store raw */
    if (outlen >= inlen) {
        outlen = 0;  /* signals stored mode */
    }

    put32(hdr + 0, 0x3150544F);        /* "OTP1" */
    put32(hdr + 4, inlen);
    put32(hdr + 8, outlen);
    put32(hdr + 12, crc);
    memcpy(out, hdr, OTP_HDRSZ);

    if (outlen == 0) {
        /* stored: copy raw data after header */
        memcpy(out + OTP_HDRSZ, in, inlen);
        r = write_all(outpath, out, OTP_HDRSZ + inlen);
        printf("otp: stored %u bytes (no compression)\n", inlen);
    } else {
        r = write_all(outpath, out, OTP_HDRSZ + outlen);
        printf("otp: %u -> %u bytes (%u%%)\n", inlen, outlen + OTP_HDRSZ,
               (outlen + OTP_HDRSZ) * 100 / inlen);
    }

    free(in); free(out);
    return r;
}

/* ---- decompress ---- */
static int do_decompress(const char *inpath, const char *outpath)
{
    unsigned char *in = 0, *out = 0, hdr[OTP_HDRSZ];
    unsigned int inlen = 0, origlen, packedlen, crc_stored, crc_calc;
    int fd, r;

    r = read_all(inpath, &in, &inlen);
    if (r) return 1;
    if (inlen < OTP_HDRSZ) { printf("otp: file too small\n"); free(in); return 1; }

    /* read header from buffer */
    memcpy(hdr, in, OTP_HDRSZ);
    if (hdr[0] != 'O' || hdr[1] != 'T' || hdr[2] != 'P' || hdr[3] != '1') {
        printf("otp: bad magic\n"); free(in); return 1;
    }
    origlen   = get32(hdr + 4);
    packedlen = get32(hdr + 8);
    crc_stored = get32(hdr + 12);

    out = (unsigned char *)malloc(origlen + 256);
    if (!out) { printf("otp: out of memory\n"); free(in); return 1; }

    if (packedlen == 0) {
        /* stored */
        if (inlen - OTP_HDRSZ < origlen) {
            printf("otp: truncated stored data\n"); free(in); free(out); return 1;
        }
        memcpy(out, in + OTP_HDRSZ, origlen);
    } else {
        if (OTP_HDRSZ + packedlen > inlen) {
            printf("otp: truncated packed data\n"); free(in); free(out); return 1;
        }
        r = otp_unpack(in + OTP_HDRSZ, packedlen, out, &origlen);
        if (r) { printf("otp: decompress failed\n"); free(in); free(out); return 1; }
    }

    crc_calc = otp_crc32(out, origlen);
    if (crc_calc != crc_stored) {
        printf("otp: CRC mismatch (expected 0x%08x got 0x%08x)\n",
               crc_stored, crc_calc);
        free(in); free(out); return 1;
    }

    r = write_all(outpath, out, origlen);
    printf("otp: decompressed to %u bytes\n", origlen);
    free(in); free(out);
    return r;
}

/* ---- info ---- */
static int do_info(const char *path)
{
    unsigned char hdr[OTP_HDRSZ];
    int fd, n;
    unsigned int origlen, packedlen, crc;

    fd = open(path, O_RDONLY, 0);
    if (fd < 0) { printf("otp: cannot open '%s'\n", path); return 1; }
    n = read(fd, hdr, OTP_HDRSZ);
    close(fd);
    if (n < OTP_HDRSZ) { printf("otp: file too small\n"); return 1; }

    if (hdr[0] != 'O' || hdr[1] != 'T' || hdr[2] != 'P' || hdr[3] != '1') {
        printf("otp: not an OTP1 file\n"); return 1;
    }

    origlen   = get32(hdr + 4);
    packedlen = get32(hdr + 8);
    crc       = get32(hdr + 12);

    printf("OTP1 file: %s\n", path);
    printf("  original:  %u bytes\n", origlen);
    if (packedlen == 0) {
        printf("  mode:      stored (uncompressed)\n");
    } else {
        printf("  packed:    %u bytes\n", packedlen);
        printf("  ratio:     %u%%\n", (packedlen + OTP_HDRSZ) * 100 / (origlen + OTP_HDRSZ));
    }
    printf("  crc32:     0x%08x\n", crc);
    return 0;
}

/* ---- main ---- */
int main(int argc, char **argv)
{
    if (argc < 2) {
        puts("otp - OpenTTY Packer v1 (LZSS + CRC32)");
        puts("usage:");
        puts("  otp c <input> <output>   compress");
        puts("  otp d <input> <output>   decompress");
        puts("  otp i <file>             show info");
        return 1;
    }

    if (argv[1][0] == 'c' && argv[1][1] == 0) {
        if (argc < 4) { puts("usage: otp c <input> <output>"); return 1; }
        return do_compress(argv[2], argv[3]);
    }
    if (argv[1][0] == 'd' && argv[1][1] == 0) {
        if (argc < 4) { puts("usage: otp d <input> <output>"); return 1; }
        return do_decompress(argv[2], argv[3]);
    }
    if (argv[1][0] == 'i' && argv[1][1] == 0) {
        if (argc < 3) { puts("usage: otp i <file>"); return 1; }
        return do_info(argv[2]);
    }

    printf("otp: unknown command '%s'\n", argv[1]);
    return 1;
}
