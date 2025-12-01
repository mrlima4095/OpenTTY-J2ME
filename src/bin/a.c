// hello_arch.c
// Compilar com: arm-linux-gnueabihf-gcc -nostdlib -static -Ttext=0x8000 -o hello.elf hello_arch.c

// Função de entrada sem libc
void _start() {
    // Usar inline assembly AT&T syntax ou ARM puro
    asm volatile(
        "mov r0, #1\n\t"          // fd = stdout
        "ldr r1, =msg\n\t"        // buffer
        "mov r2, #13\n\t"         // length
        "mov r7, #4\n\t"          // write syscall
        "swi #0\n\t"              // syscall
        
        "mov r0, #0\n\t"          // status = 0
        "mov r7, #1\n\t"          // exit syscall
        "swi #0\n\t"              // syscall
    );
}

// Declaração da string
const char msg[] = "Hello Arch!\n";