// hello.c
void _start() {
    const char msg[] = "Hello from C!\n";
    asm volatile (
        "mov r0, #1\n"      // stdout
        "mov r1, %0\n"      // buffer
        "mov r2, %1\n"      // length
        "mov r7, #4\n"      // SYS_write
        "swi #0\n"
        : : "r"(msg), "r"(14)
        : "r0", "r1", "r2", "r7"
    );
    
    asm volatile (
        "mov r0, #0\n"      // exit code
        "mov r7, #1\n"      // SYS_exit
        "swi #0\n"
    );
}