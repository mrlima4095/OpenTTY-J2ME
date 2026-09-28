#ifndef OPENTTY_H
#define OPENTTY_H

/* Minimal C API provided by res/lib/libc.s with build-elf.sh -stdlib.
 * This is an OpenTTY ABI, not a Linux or glibc header. */

typedef unsigned int size_t;
typedef unsigned short uint16_t;
typedef unsigned char uint8_t;

#define O_RDONLY    0
#define O_WRONLY    1
#define O_RDWR      2
#define O_CREAT     64
#define O_TRUNC     512
#define O_APPEND    1024
#define O_DIRECTORY 0x10000

int strlen(const char *s);
char *strcpy(char *dst, const char *src);
char *strncpy(char *dst, const char *src, size_t n);
int strcmp(const char *a, const char *b);
int strncmp(const char *a, const char *b, size_t n);
char *strcat(char *dst, const char *src);
char *strncat(char *dst, const char *src, size_t n);
char *strchr(const char *s, int c);
char *strdup(const char *s);

void *memcpy(void *dst, const void *src, size_t n);
void *memmove(void *dst, const void *src, size_t n);
void *memset(void *dst, int c, size_t n);
int memcmp(const void *a, const void *b, size_t n);
void *memchr(const void *s, int c, size_t n);

int atoi(const char *s);
int abs(int value);
int toupper(int c);
int tolower(int c);

int putchar(int c);
int puts(const char *s);
int printf(const char *format, ...);
int sprintf(char *dst, const char *format, ...);
int snprintf(char *dst, size_t size, const char *format, ...);

void *malloc(size_t size);
void *calloc(size_t count, size_t size);
void *realloc(void *ptr, size_t size);
void free(void *ptr);

int getpid(void);
void exit(int status);
void _exit(int status);
void abort(void);

int read(int fd, void *buf, size_t count);
int write(int fd, const void *buf, size_t count);
int open(const char *path, int flags, int mode);
int close(int fd);
int brk(void *address);

/* IPv4 sockets use the OpenTTY raw-syscall ABI.  sin_port is stored in
 * network byte order; use sockaddr_in_init() rather than filling it directly. */
#define AF_INET       2
#define SOCK_STREAM   1
#define SOCK_DGRAM    2
#define IPPROTO_TCP   6
#define IPPROTO_UDP   17
#define SOL_SOCKET    1
#define SO_REUSEADDR  2

struct sockaddr_in {
    uint16_t sin_family;
    uint8_t sin_port[2];
    uint8_t sin_addr[4];
    uint8_t sin_zero[8];
};

static int opentty_syscall3(int number, int arg0, int arg1, int arg2) {
    register int a0 __asm__("a0") = arg0;
    register int a1 __asm__("a1") = arg1;
    register int a2 __asm__("a2") = arg2;
    register int a7 __asm__("a7") = number;
    __asm__ volatile ("ecall" : "+r"(a0) : "r"(a1), "r"(a2), "r"(a7) : "memory");
    return a0;
}

static int opentty_syscall4(int number, int arg0, int arg1, int arg2, int arg3) {
    register int a0 __asm__("a0") = arg0;
    register int a1 __asm__("a1") = arg1;
    register int a2 __asm__("a2") = arg2;
    register int a3 __asm__("a3") = arg3;
    register int a7 __asm__("a7") = number;
    __asm__ volatile ("ecall" : "+r"(a0) : "r"(a1), "r"(a2), "r"(a3), "r"(a7) : "memory");
    return a0;
}

static int opentty_syscall5(int number, int arg0, int arg1, int arg2, int arg3, int arg4) {
    register int a0 __asm__("a0") = arg0;
    register int a1 __asm__("a1") = arg1;
    register int a2 __asm__("a2") = arg2;
    register int a3 __asm__("a3") = arg3;
    register int a4 __asm__("a4") = arg4;
    register int a7 __asm__("a7") = number;
    __asm__ volatile ("ecall" : "+r"(a0) : "r"(a1), "r"(a2), "r"(a3), "r"(a4), "r"(a7) : "memory");
    return a0;
}

static int opentty_syscall6(int number, int arg0, int arg1, int arg2, int arg3, int arg4, int arg5) {
    register int a0 __asm__("a0") = arg0;
    register int a1 __asm__("a1") = arg1;
    register int a2 __asm__("a2") = arg2;
    register int a3 __asm__("a3") = arg3;
    register int a4 __asm__("a4") = arg4;
    register int a5 __asm__("a5") = arg5;
    register int a7 __asm__("a7") = number;
    __asm__ volatile ("ecall" : "+r"(a0) : "r"(a1), "r"(a2), "r"(a3), "r"(a4), "r"(a5), "r"(a7) : "memory");
    return a0;
}

static void sockaddr_in_init(struct sockaddr_in *address, int port, int a, int b, int c, int d) {
    int i;
    address->sin_family = AF_INET;
    address->sin_port[0] = (uint8_t)(port >> 8);
    address->sin_port[1] = (uint8_t)port;
    address->sin_addr[0] = (uint8_t)a;
    address->sin_addr[1] = (uint8_t)b;
    address->sin_addr[2] = (uint8_t)c;
    address->sin_addr[3] = (uint8_t)d;
    for (i = 0; i < 8; i++) { address->sin_zero[i] = 0; }
}

static int socket(int domain, int type, int protocol) { return opentty_syscall3(281, domain, type, protocol); }
static int bind(int fd, const struct sockaddr_in *address, int length) { return opentty_syscall3(282, fd, (int)address, length); }
static int connect(int fd, const struct sockaddr_in *address, int length) { return opentty_syscall3(283, fd, (int)address, length); }
static int listen(int fd, int backlog) { return opentty_syscall3(284, fd, backlog, 0); }
static int accept(int fd, struct sockaddr_in *address, int *length) { return opentty_syscall3(285, fd, (int)address, (int)length); }
static int sendto(int fd, const void *buffer, int length, int flags, const struct sockaddr_in *address, int address_length) { return opentty_syscall6(290, fd, (int)buffer, length, flags, (int)address, address_length); }
static int recvfrom(int fd, void *buffer, int length, int flags, struct sockaddr_in *address, int *address_length) { return opentty_syscall6(292, fd, (int)buffer, length, flags, (int)address, (int)address_length); }
static int setsockopt(int fd, int level, int option, const void *value, int length) { return opentty_syscall5(294, fd, level, option, (int)value, length); }
static int getsockopt(int fd, int level, int option, void *value, int *length) { return opentty_syscall5(295, fd, level, option, (int)value, (int)length); }
int opentty_socket_reader_start(int fd, int output_item);
int opentty_socket_reader_stop(int fd);

/* Clock and memory. time() returns seconds since the Unix epoch (UTC);
 * gc() runs the host garbage collector; mem_total/free/used report the
 * Java heap in KB, as the `free` command does. */
long time(long *tloc);
void gc(void);
int mem_total(void);
int mem_free(void);
int mem_used(void);

/* OpenTTY process ABI. spawn starts a Lua or ELF program asynchronously and
 * returns its PID; waitpid returns -11 while that child is still running. */
int opentty_spawn(const char *path, int *pid_out);
int opentty_waitpid(int pid, int *status_out);
int opentty_shell(const char *command);
int opentty_getenv(const char *key, char *buffer, size_t size);
int opentty_expand_env(const char *text, char *buffer, size_t size);

#endif
