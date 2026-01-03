/* minilibc.c - Implementação com inline assembly */
#include "minilibc.h"

/* File descriptors (se não quiser modificar o header) */
#define STDIN_FILENO  0
#define STDOUT_FILENO 1
#define STDERR_FILENO 2

/* exit() - Termina o processo */
void exit(int code) {
    asm volatile (
        "mov r0, %0\n"      // Código de saída
        "mov r7, #1\n"      // SYS_exit = 1
        "swi #0\n"
        : : "r"(code)
        : "r0", "r7"
    );
}

/* strlen() - Tamanho da string */
size_t strlen(const char *s) {
    size_t len = 0;
    while (s[len]) len++;
    return len;
}

/* write() - Escreve em file descriptor */
int write(int fd, const void *buf, size_t count) {
    int ret;
    asm volatile (
        "mov r0, %1\n"      // fd
        "mov r1, %2\n"      // buf
        "mov r2, %3\n"      // count
        "mov r7, #4\n"      // SYS_write = 4
        "swi #0\n"
        "mov %0, r0\n"      // retorno
        : "=r"(ret)
        : "r"(fd), "r"(buf), "r"(count)
        : "r0", "r1", "r2", "r7"
    );
    return ret;
}

/* printf() - Versão simplificada */
void printf(const char *format, ...) {
    // Implementação básica - só imprime a string
    write(STDOUT_FILENO, format, strlen(format));
}

/* puts() - Imprime string com nova linha */
void puts(const char *s) {
    write(STDOUT_FILENO, s, strlen(s));
    write(STDOUT_FILENO, "\n", 1);
}

/* putchar() - Imprime um caractere */
int putchar(int c) {
    char ch = c;
    write(STDOUT_FILENO, &ch, 1);
    return c;
}

/* read() - Lê de file descriptor */
int read(int fd, void *buf, size_t count) {
    int ret;
    asm volatile (
        "mov r0, %1\n"      // fd
        "mov r1, %2\n"      // buf
        "mov r2, %3\n"      // count
        "mov r7, #3\n"      // SYS_read = 3
        "swi #0\n"
        "mov %0, r0\n"      // retorno
        : "=r"(ret)
        : "r"(fd), "r"(buf), "r"(count)
        : "r0", "r1", "r2", "r7"
    );
    return ret;
}

/* memset() - Preenche memória */
void* memset(void *s, int c, size_t n) {
    unsigned char *p = s;
    while (n--) *p++ = c;
    return s;
}

/* memcpy() - Copia memória */
void* memcpy(void *dest, const void *src, size_t n) {
    unsigned char *d = dest;
    const unsigned char *s = src;
    while (n--) *d++ = *s++;
    return dest;
}