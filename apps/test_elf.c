/* test_simple.c - Versão simplificada */
/* Para compilar: clang -target arm-linux-gnueabi -nostdlib test_simple.c -o test_simple */

void _start() {
    const char *msg = "Hello from ARM ELF Emulator!\n";
    int len = 28;
    
    /* SYS_WRITE - inline assembly */
    asm volatile (
        "mov r7, #4\n\t"        /* SYS_WRITE */
        "mov r0, #1\n\t"        /* stdout */
        "mov r1, %0\n\t"        /* buffer */
        "mov r2, %1\n\t"        /* length */
        "svc #0"
        :
        : "r" (msg), "r" (len)
        : "r0", "r1", "r2", "r7"
    );
    
    /* SYS_EXIT */
    asm volatile (
        "mov r7, #1\n\t"        /* SYS_EXIT */
        "mov r0, #0\n\t"        /* status 0 */
        "svc #0"
        :
        :
        : "r7", "r0"
    );
}