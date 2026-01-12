// contador_loop_estatico.c
// Compilar: arm-linux-gnueabi-gcc -static -nostdlib -o contador_loop contador_loop_estatico.c

void print_number(int n) {
    char buffer[16];
    char* p = buffer + 15;
    *p = '\n';
    p--;
    
    // Converter número para string (backwards)
    do {
        *p = (n % 10) + '0';
        n /= 10;
        p--;
    } while (n > 0);
    
    p++; // Ajustar para início do número
    
    // Calcular comprimento
    int len = (buffer + 15) - p + 1; // +1 para incluir \n
    
    // Syscall write
    asm volatile (
        "mov r0, #1\n"      // fd = stdout
        "mov r1, %0\n"      // buffer
        "mov r2, %1\n"      // length
        "mov r7, #4\n"      // syscall write
        "svc #0\n"
        :
        : "r" (p), "r" (len)
        : "r0", "r1", "r2", "r7"
    );
}

void _start() {
    // Contar de 1 a 10
    int i;
    for (i = 1; i <= 10; i++) {
        print_number(i);
    }
    
    // Exit
    asm volatile (
        "mov r0, #0\n"
        "mov r7, #1\n"
        "svc #0\n"
    );
}