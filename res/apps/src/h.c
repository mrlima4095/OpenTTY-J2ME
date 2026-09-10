// h.c - "Hello from C!" (RISC-V RV32IM, syscalls crus).
// Compilar:
//   ./build-elf.sh res/apps/src/h.c -o res/apps/dist/h

void _start(void) {
    const char msg[] = "Hello from C!\n";
    asm volatile (
        "li a7, 4\n"        // SYS_write
        "li a0, 1\n"        // stdout
        "mv a1, %0\n"       // buffer
        "li a2, 14\n"       // length
        "ecall\n"
        : : "r"(msg)
        : "a0", "a1", "a2", "a7"
    );

    asm volatile (
        "li a7, 1\n"        // SYS_exit
        "li a0, 0\n"
        "ecall\n"
        : : : "a0", "a7"
    );
}