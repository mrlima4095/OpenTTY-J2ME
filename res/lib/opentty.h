#ifndef OPENTTY_H
#define OPENTTY_H

/* Minimal C API provided by res/lib/libc.s with build-elf.sh -stdlib.
 * This is an OpenTTY ABI, not a Linux or glibc header. */

typedef unsigned int size_t;

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

/* OpenTTY process ABI. spawn starts a Lua or ELF program asynchronously and
 * returns its PID; waitpid returns -11 while that child is still running. */
int opentty_spawn(const char *path, int *pid_out);
int opentty_waitpid(int pid, int *status_out);
int opentty_shell(const char *command);
int opentty_getenv(const char *key, char *buffer, size_t size);

#endif
