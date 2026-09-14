/* otp_compress.c — LZSS packer for OTP1
 *
 * Algorithm: LZSS with 4096-byte sliding window.
 *   Flag byte: 8 bits, MSB first. 1 = literal, 0 = match.
 *   Match token (2 bytes):
 *     byte0 = dist & 0xFF
 *     byte1 = ((dist >> 8) & 0xF) << 4 | (len - MNLEN)
 *   dist range: 0..4095   len range: 3..18
 *
 * Compile: built via build-elf.sh alongside otp_main.c
 */
#include "otp.h"

/* ---- tiny hash table for match candidate lookup ---- */
static int ht[WSIZE];            /* last-seen position for each 3-byte hash */

static void ht_reset(void) { memset(ht, -1, sizeof(ht)); }

static unsigned int h3(const unsigned char *p) {
    return ((unsigned)p[0] * 31 + p[1]) * 31 + p[2] & (WSIZE - 1);
}

/* ---- pack (compress) ---- */
int otp_pack(const unsigned char *src, unsigned int srclen,
             unsigned char *dst, unsigned int *dstlen)
{
    unsigned int ip = 0, op = 0;
    unsigned int flagpos;
    int bits;
    unsigned char flag;

    ht_reset();

    while (ip < srclen) {
        /* start a new flag group */
        flagpos = op++;
        flag = 0;
        bits = 0;

        while (bits < 8 && ip < srclen) {
            int best_len = 0, best_dist = 0;

            if (ip + MNLEN <= srclen) {
                unsigned int h = h3(src + ip);
                int prev = ht[h];

                if (prev >= 0 && (int)(ip - (unsigned)prev) > 0 &&
                    (int)(ip - (unsigned)prev) <= WSIZE) {
                    /* measure match length */
                    int len = 0;
                    int maxlen = MXLEN;
                    if (ip + (unsigned)maxlen > srclen)
                        maxlen = srclen - ip;
                    while (len < maxlen && src[prev + len] == src[ip + len])
                        len++;
                    if (len >= MNLEN) {
                        best_len = len;
                        best_dist = (int)(ip - (unsigned)prev);
                    }
                }
                ht[h] = (int)ip;
            }

            if (best_len >= MNLEN) {
                /* match: flag bit = 0 (default) */
                dst[op++] = (unsigned char)(best_dist & 0xFF);
                dst[op++] = (unsigned char)(((best_dist >> 8) & 0xF) << 4 |
                                            (best_len - MNLEN));
                /* advance input, updating hash table along the way */
                {
                    int j;
                    for (j = 0; j < best_len; j++) {
                        if (ip + j + 2 < srclen)
                            ht[h3(src + ip + j)] = (int)(ip + j);
                    }
                }
                ip += best_len;
            } else {
                /* literal */
                flag |= (unsigned char)(0x80 >> bits);
                dst[op++] = src[ip++];
            }
            bits++;
        }
        dst[flagpos] = flag;
    }

    *dstlen = op;
    return 0;
}
