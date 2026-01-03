/* exemplo.c - Teste da minilibc */
#include "minilibc.h"

#define STDOUT_FILENO 1

int main() {
    printf("Hello, OpenTTY ELF emulator!\n");
    printf("PID: ");
    
    // Usando syscall diretamente para getpid
    int pid;
    asm volatile (
        "mov r7, #20\n"     // SYS_getpid = 20
        "swi #0\n"
        "mov %0, r0\n"
        : "=r"(pid)
        :
        : "r0", "r7"
    );
    
    // Converter PID para string
    char buf[16];
    char *p = buf + 15;
    *p = '\0';
    int n = pid;
    do {
        *--p = '0' + (n % 10);
        n /= 10;
    } while (n > 0);
    
    printf(p);
    printf("\n");
    
    exit(0);
}