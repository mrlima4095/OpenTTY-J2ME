#define STDOUT_FILENO 1

// Função de saída usando syscall exit
__attribute__((naked, noreturn))
void sys_exit(int code) {
    asm volatile (
        "mov r0, %0\n"    // Código de saída
        "mov r7, #1\n"    // SYS_exit = 1
        "swi #0\n"        // Chamada de sistema
        : : "r"(code)
        : "r0", "r7"
    );
    // Loop infinito se a syscall falhar
    asm volatile ("b .");
}

// Função write usando syscall write
void sys_write(int fd, const char *buf, int count) {
    asm volatile (
        "mov r0, %0\n"    // fd
        "mov r1, %1\n"    // buf
        "mov r2, %2\n"    // count
        "mov r7, #4\n"    // SYS_write = 4
        "swi #0\n"        // Chamada de sistema
        : : "r"(fd), "r"(buf), "r"(count)
        : "r0", "r1", "r2", "r7"
    );
}

// Função para imprimir string
void print(const char *s) {
    int len = 0;
    while (s[len]) len++;
    sys_write(STDOUT_FILENO, s, len);
}

// Função para imprimir número
void print_num(int num) {
    char buffer[12];
    int i = 0;
    int is_negative = 0;
    
    if (num < 0) {
        is_negative = 1;
        num = -num;
    }
    
    // Converter para string (ao contrário)
    do {
        buffer[i++] = (num % 10) + '0';
        num /= 10;
    } while (num > 0);
    
    if (is_negative) {
        buffer[i++] = '-';
    }
    
    // Inverter a string
    for (int j = 0; j < i/2; j++) {
        char temp = buffer[j];
        buffer[j] = buffer[i-1-j];
        buffer[i-1-j] = temp;
    }
    
    buffer[i] = '\0';
    print(buffer);
}

// Ponto de entrada - NAKED (sem prólogo/epílogo do compilador)
__attribute__((naked, noreturn))
void _start() {
    asm volatile (
        // Salvar argc e argv da stack
        "push {r0, r1, lr}\n"
        
        // Verificar se temos argumentos
        "cmp r0, #0\n"
        "beq 1f\n"
        
        // argc > 0, imprimir
        "bl print_args\n"
        
        // Sair
        "1:\n"
        "mov r0, #0\n"      // Código de saída 0
        "mov r7, #1\n"      // SYS_exit
        "swi #0\n"
        
        // Se falhar, loop infinito
        "b .\n"
    );
}

// Função para imprimir argumentos
void print_args(int argc, char **argv) {
    print("argc = ");
    print_num(argc);
    print("\n");
    
    for (int i = 0; i < argc; i++) {
        print("argv[");
        print_num(i);
        print("] = ");
        print(argv[i]);
        print("\n");
    }
    
    print("Programa executado com sucesso!\n");
}

asm(".global _start");