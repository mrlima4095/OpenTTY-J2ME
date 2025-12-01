// hello.c - Hello World para ELF emulator
// Compilar com: arm-linux-gnueabi-gcc -nostdlib -march=armv6 -marm -O2 -o hello.elf hello.c

// Syscall numbers (para r7)
#define SYS_exit  1
#define SYS_write 4
#define SYS_brk   45

// Syscall wrapper inline assembly
static inline int syscall(int nr, int arg1, int arg2, int arg3) {
    int ret;
    asm volatile (
        "mov r7, %1\n"
        "mov r0, %2\n"
        "mov r1, %3\n"
        "mov r2, %4\n"
        "swi #0\n"
        "mov %0, r0"
        : "=r"(ret)
        : "r"(nr), "r"(arg1), "r"(arg2), "r"(arg3)
        : "r7", "r0", "r1", "r2", "memory"
    );
    return ret;
}

// Funções específicas
static inline int write(int fd, const void *buf, int count) {
    return syscall(SYS_write, fd, (int)buf, count);
}

static inline void exit(int status) {
    syscall(SYS_exit, status, 0, 0);
}

// Ponto de entrada do programa
void _start() {
    const char msg[] = "Hello World from ARM ELF!\n";
    
    // Escrever para stdout (fd=1)
    write(1, msg, sizeof(msg) - 1);
    
    // Terminar com sucesso
    exit(0);
}