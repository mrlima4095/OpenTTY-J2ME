// count.c - Mostra números de 0 a 5 (RISC-V RV32IM, syscalls crus).
// Compilar:
//   ./build-elf.sh res/apps/src/count.c -o res/apps/dist/count

void write_syscall(int fd, const char* buf, int count) {
    __asm__ volatile (
        "li a7, 4\n"        // syscall write
        "mv a0, %0\n"       // fd
        "mv a1, %1\n"       // buffer
        "mv a2, %2\n"       // count
        "ecall\n"
        : : "r"(fd), "r"(buf), "r"(count)
        : "a0", "a1", "a2", "a7"
    );
}

void exit_syscall(int status) {
    __asm__ volatile (
        "li a7, 1\n"        // syscall exit
        "mv a0, %0\n"       // status
        "ecall\n"
        : : "r"(status)
        : "a0", "a7"
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