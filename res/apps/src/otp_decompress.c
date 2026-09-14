/* otp_decompress.c — LZSS unpacker for OTP1
 *
 * Compile: built via build-elf.sh alongside otp_main.c
 */
#include "otp.h"

int otp_unpack(const unsigned char *src, unsigned int srclen,
               unsigned char *dst, unsigned int *dstlen)
{
    unsigned int ip = 0, op = 0;

    while (ip < srclen) {
        unsigned char flag = src[ip++];
        int bits;

        for (bits = 0; bits < 8 && ip < srclen; bits++) {
            if (flag & (0x80 >> bits)) {
                /* literal */
                dst[op++] = src[ip++];
            } else {
                /* match */
                if (ip + 1 >= srclen) break;
                unsigned int lo = src[ip++];
                unsigned int hi = src[ip++];
                unsigned int dist = lo | ((hi >> 4) << 8);
                unsigned int len  = (hi & 0xF) + MNLEN;
                unsigned int j;

                if (dist == 0) break;           /* safety: avoid infinite loop */

                for (j = 0; j < len; j++)
                    dst[op + j] = dst[op + j - dist];
                op += len;
            }
        }
    }

    *dstlen = op;
    return 0;
}
