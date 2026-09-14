#ifndef OTP_H
#define OTP_H

/* OTP1 file format:
 *   [0..3]   magic "OTP1"
 *   [4..7]   original_size  (uint32 LE)
 *   [8..11]  packed_size    (uint32 LE, 0 = stored/uncompressed)
 *   [12..15] crc32          (uint32 LE of original data)
 *   [16..]   payload (packed or raw)
 */

#define OTP_MAGIC0 'O'
#define OTP_MAGIC1 'T'
#define OTP_MAGIC2 'P'
#define OTP_MAGIC3 '1'
#define OTP_HDRSZ  16

/* LZSS tuning */
#define WBITS  12
#define WSIZE  (1 << WBITS)          /* 4096 */
#define MXLEN  18                     /* max match length  (4-bit field + 3) */
#define MNLEN  3                      /* min match length */

/* Maximum input size we can handle (fits in ~1 MB with output buffer) */
#define OTP_MAXINPUT (400 * 1024)

/* open() flags (Linux-like, as the ELF emulator expects) */
#ifndef O_RDONLY
#define O_RDONLY  0
#define O_WRONLY  1
#define O_RDWR    2
#define O_CREAT  64
#define O_TRUNC  512
#endif

/* --- libc declarations (provided by -stdlib → libc.s) --- */
int    printf(const char *fmt, ...);
int    puts(const char *s);
int    putchar(int c);
int    strlen(const char *s);
int    strcmp(const char *a, const char *b);
int    strncmp(const char *a, const char *b, unsigned int n);
void  *memcpy(void *d, const void *s, unsigned int n);
void  *memmove(void *d, const void *s, unsigned int n);
void  *memset(void *s, int c, unsigned int n);
int    memcmp(const void *a, const void *b, unsigned int n);
void  *malloc(unsigned int size);
void  *calloc(unsigned int n, unsigned int sz);
void  *realloc(void *p, unsigned int size);
void   free(void *p);
int    open(const char *path, int flags, int mode);
int    read(int fd, void *buf, int count);
int    write(int fd, const void *buf, int count);
int    close(int fd);
void   exit(int status);

/* --- OTP API --- */
unsigned int otp_crc32(const unsigned char *data, unsigned int len);
int  otp_pack(const unsigned char *src, unsigned int srclen,
              unsigned char *dst, unsigned int *dstlen);
int  otp_unpack(const unsigned char *src, unsigned int srclen,
                unsigned char *dst, unsigned int *dstlen);

#endif
