// contador.c - Mostra números de 0 a 5
// Compilar: arm-none-eabi-gcc -nostdlib -static -o contador.elf contador.c

void write_syscall(int fd, const char* buf, int count) {
    __asm__ volatile (
        "mov r7, #4\n"      // syscall write
        "mov r0, %0\n"      // fd
        "mov r1, %1\n"      // buffer
        "mov r2, %2\n"      // count
        "swi 0\n"
        : : "r"(fd), "r"(buf), "r"(count)
        : "r0", "r1", "r2", "r7"
    );
}

void exit_syscall(int status) {
    __asm__ volatile (
        "mov r7, #1\n"      // syscall exit
        "mov r0, %0\n"      // status
        "swi 0\n"
        : : "r"(status)
        : "r0", "r7"
    );
}

void print_number(int num) {
    char buf[3];
    buf[0] = '0' + num;  // converte para caractere
    buf[1] = '\n';       // newline
    buf[2] = '\0';       // null terminator (opcional)
    write_syscall(1, buf, 2);
}

void _start(void) {
    int i;
    
    for (i = 0; i <= 5; i++) {
        print_number(i);
    }
    
    exit_syscall(0);
}