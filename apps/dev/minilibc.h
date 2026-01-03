/* minilibc.h - LibC mínima para emulador ELF */
#ifndef MINILIBC_H
#define MINILIBC_H

typedef unsigned int size_t;

/* File descriptors padrão */
#define STDIN_FILENO  0
#define STDOUT_FILENO 1
#define STDOUT_FILENO 1
#define STDERR_FILENO 2

/* Funções do sistema */
void exit(int code);
int write(int fd, const void *buf, size_t count);
int read(int fd, void *buf, size_t count);

/* Funções de string */
size_t strlen(const char *s);
void* memset(void *s, int c, size_t n);
void* memcpy(void *dest, const void *src, size_t n);

/* Funções de E/S */
void printf(const char *format, ...);
void puts(const char *s);
int putchar(int c);

/* Helpers */
int atoi(const char *s);
char* itoa(int value, char *str, int base);

#endif