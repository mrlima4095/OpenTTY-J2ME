/* bb_crypto.c - base64, md5sum e sum (BSD) para o multi-tool busybox.
 * 32-bit apenas: sem long long, sem floats (restricoes do emulador).
 */
#include "busybox.h"

/* ================= base64 ================= */

static const char b64t[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
static int b64_dec_tbl[256];
static int b64_inited = 0;

static void b64_init(void)
{
    int i;
    if (b64_inited) return;
    for (i = 0; i < 256; i++) b64_dec_tbl[i] = -1;
    for (i = 0; i < 64; i++) b64_dec_tbl[(unsigned char)b64t[i]] = i;
    b64_inited = 1;
}

static int b64_encode(const char *in, int len, char *out)
{
    int i = 0, o = 0;
    while (i < len) {
        unsigned a = (unsigned char)in[i], b, c;
        b = (i + 1 < len) ? (unsigned char)in[i + 1] : 0;
        c = (i + 2 < len) ? (unsigned char)in[i + 2] : 0;
        out[o++] = b64t[a >> 2];
        out[o++] = b64t[((a & 3) << 4) | (b >> 4)];
        out[o++] = (i + 1 < len) ? b64t[((b & 15) << 2) | (c >> 6)] : '=';
        out[o++] = (i + 2 < len) ? b64t[c & 63] : '=';
        i += 3;
    }
    return o;
}

static int b64_decode(const char *in, int len, char *out)
{
    int i, o = 0;
    unsigned acc = 0;
    int nb = 0;
    for (i = 0; i < len; i++) {
        int v;
        unsigned char c = (unsigned char)in[i];
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') continue;
        if (c == '=') break;
        v = b64_dec_tbl[c];
        if (v < 0) continue;
        acc = (acc << 6) | (unsigned)v;
        nb += 6;
        if (nb >= 8) {
            nb -= 8;
            out[o++] = (char)((acc >> nb) & 0xFF);
        }
    }
    return o;
}

int app_base64(int argc, char **argv)
{
    int dec = 0, i = 1;
    const char *file = 0;
    char *buf = 0, *out = 0;
    int len = 0, olen;
    while (i < argc && argv[i][0] == '-' && strcmp(argv[i], "-") != 0) {
        const char *p = argv[i] + 1;
        while (*p) { if (*p == 'd') dec = 1; p++; }
        i++;
    }
    if (i < argc) file = argv[i];
    if (file) {
        if (bb_read_all(file, &buf, &len)) {
            bb_err("base64: "); bb_err(file); bb_err(": No such file or directory\n");
            return 1;
        }
    } else if (bb_read_fd(0, &buf, &len)) {
        bb_err("base64: falha ao ler stdin\n");
        return 1;
    }
    if (!buf) return 0;                 /* stdin vazio uma vez lido */
    if (dec) {
        b64_init();
        out = malloc(len / 4 * 3 + 4);
        if (!out) { free(buf); return 1; }
        olen = b64_decode(buf, len, out);
        if (olen > 0) write(1, out, olen);
    } else {
        out = malloc((len + 2) / 3 * 4 + 1);
        if (!out) { free(buf); return 1; }
        olen = b64_encode(buf, len, out);
        if (olen > 0) write(1, out, olen);
        bb_putc('\n');
    }
    free(out);
    free(buf);
    return 0;
}

/* ================= md5sum ================= */

#define ROTL(x, s) (((x) << (s)) | ((x) >> (32 - (s))))

static const unsigned K[64] = {
    0xd76aa478u, 0xe8c7b756u, 0x242070dbu, 0xc1bdceeeu,
    0xf57c0fafu, 0x4787c62au, 0xa8304613u, 0xfd469501u,
    0x698098d8u, 0x8b44f7afu, 0xffff5bb1u, 0x895cd7beu,
    0x6b901122u, 0xfd987193u, 0xa679438eu, 0x49b40821u,
    0xf61e2562u, 0xc040b340u, 0x265e5a51u, 0xe9b6c7aau,
    0xd62f105du, 0x02441453u, 0xd8a1e681u, 0xe7d3fbc8u,
    0x21e1cde6u, 0xc33707d6u, 0xf4d50d87u, 0x455a14edu,
    0xa9e3e905u, 0xfcefa3f8u, 0x676f02d9u, 0x8d2a4c8au,
    0xfffa3942u, 0x8771f681u, 0x6d9d6122u, 0xfde5380cu,
    0xa4beea44u, 0x4bdecfa9u, 0xf6bb4b60u, 0xbebfbc70u,
    0x289b7ec6u, 0xeaa127fau, 0xd4ef3085u, 0x04881d05u,
    0xd9d4d039u, 0xe6db99e5u, 0x1fa27cf8u, 0xc4ac5665u,
    0xf4292244u, 0x432aff97u, 0xab9423a7u, 0xfc93a039u,
    0x655b59c3u, 0x8f0ccc92u, 0xffeff47du, 0x85845dd1u,
    0x6fa87e4fu, 0xfe2ce6e0u, 0xa3014314u, 0x4e0811a1u,
    0xf7537e82u, 0xbd3af235u, 0x2ad7d2bbu, 0xeb86d391u
};

static const int S[64] = {
    7,12,17,22,  7,12,17,22,  7,12,17,22,  7,12,17,22,
    5, 9,14,20,  5, 9,14,20,  5, 9,14,20,  5, 9,14,20,
    4,11,16,23,  4,11,16,23,  4,11,16,23,  4,11,16,23,
    6,10,15,21,  6,10,15,21,  6,10,15,21,  6,10,15,21
};

static void md5_chunk(unsigned *ctx, const unsigned char *p)
{
    unsigned a = ctx[0], b = ctx[1], c = ctx[2], d = ctx[3];
    unsigned w[16];
    int i;
    for (i = 0; i < 16; i++) {
        w[i] = (unsigned)p[i * 4] | ((unsigned)p[i * 4 + 1] << 8) |
               ((unsigned)p[i * 4 + 2] << 16) | ((unsigned)p[i * 4 + 3] << 24);
    }
    for (i = 0; i < 64; i++) {
        unsigned f;
        int g;
        if (i < 16)      { f = (b & c) | (~b & d); g = i; }
        else if (i < 32) { f = (d & b) | (~d & c); g = (5 * i + 1) & 15; }
        else if (i < 48) { f = b ^ c ^ d;          g = (3 * i + 5) & 15; }
        else             { f = c ^ (b | ~d);       g = (7 * i) & 15; }
        {
            unsigned t = d;
            d = c; c = b;
            b = b + ROTL(a + f + K[i] + w[g], S[i]);
            a = t;
        }
    }
    ctx[0] += a; ctx[1] += b; ctx[2] += c; ctx[3] += d;
}

static void md5_string(const char *buf, int len, unsigned char digest[16])
{
    unsigned ctx[4];
    int need = (len + 1) % 64;
    int zeros = (need <= 56) ? 56 - need : 120 - need;
    int total = len + 1 + zeros + 8;
    unsigned char *m;
    int o, i;
    unsigned lo, hi;

    ctx[0] = 0x67452301u; ctx[1] = 0xefcdab89u;
    ctx[2] = 0x98badcfeu; ctx[3] = 0x10325476u;
    m = malloc(total);
    if (!m) { for (i = 0; i < 16; i++) digest[i] = 0; return; }
    memcpy(m, buf, len);
    m[len] = 0x80;
    for (i = len + 1; i < len + 1 + zeros; i++) m[i] = 0;
    lo = (unsigned)len << 3;                       /* len em bits (32 bits) */
    hi = ((unsigned)len >> 29) & 7u;
    m[total - 8] = (unsigned char)lo;
    m[total - 7] = (unsigned char)(lo >> 8);
    m[total - 6] = (unsigned char)(lo >> 16);
    m[total - 5] = (unsigned char)(lo >> 24);
    m[total - 4] = (unsigned char)hi;
    m[total - 3] = (unsigned char)(hi >> 8);
    m[total - 2] = 0;
    m[total - 1] = 0;
    for (o = 0; o < total; o += 64) md5_chunk(ctx, m + o);
    for (i = 0; i < 4; i++) {
        unsigned w = ctx[i];
        digest[i * 4]     = (unsigned char)w;
        digest[i * 4 + 1] = (unsigned char)(w >> 8);
        digest[i * 4 + 2] = (unsigned char)(w >> 16);
        digest[i * 4 + 3] = (unsigned char)(w >> 24);
    }
    free(m);
}

static void hex2(unsigned char b)
{
    static const char hx[] = "0123456789abcdef";
    bb_putc(hx[b >> 4]);
    bb_putc(hx[b & 15]);
}

int app_md5sum(int argc, char **argv)
{
    int i, status = 0;
    if (argc < 2) { bb_err("md5sum: uso: md5sum FILE...\n"); return 1; }
    for (i = 1; i < argc; i++) {
        unsigned char dg[16];
        char *buf = 0;
        int len = 0, k;
        if (bb_read_all(argv[i], &buf, &len)) {
            bb_err("md5sum: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            status = 1;
            continue;
        }
        md5_string(buf, len, dg);
        if (buf) free(buf);
        for (k = 0; k < 16; k++) hex2(dg[k]);
        bb_out("  "); bb_out(argv[i]); bb_putc('\n');
    }
    return status;
}

/* ================= sum (BSD) ================= */

int app_sum(int argc, char **argv)
{
    int i;
    if (argc < 2) { bb_err("sum: uso: sum FILE...\n"); return 1; }
    for (i = 1; i < argc; i++) {
        char *buf = 0;
        int len = 0, k;
        unsigned sum = 0;
        if (bb_read_all(argv[i], &buf, &len)) {
            bb_err("sum: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            continue;
        }
        for (k = 0; k + 1 < len; k += 2) {
            unsigned w = (unsigned char)buf[k] | ((unsigned)(unsigned char)buf[k + 1] << 8);
            sum = ((sum >> 1) | ((sum & 1) << 15)) + w;
            sum &= 0xFFFFu;
        }
        if (k < len) {
            unsigned w = (unsigned)(unsigned char)buf[k] << 8;
            sum = ((sum >> 1) | ((sum & 1) << 15)) + w;
            sum &= 0xFFFFu;
        }
        if (buf) free(buf);
        printf("%u %d %s\n", sum, (len + 1023) / 1024, argv[i]);
    }
    return 0;
}