#define STDOUT_FILENO 1

void exit(int code) {
    asm volatile (
        "mov r0, %0\n"
        "mov r7, #1\n"
        "swi #0"
        : : "r"(code) : "r0", "r7"
    );
}

void print(const char *s) {
    int len = 0;
    while (s[len]) len++;
    
    asm volatile (
        "mov r0, %0\n"
        "mov r1, %1\n"
        "mov r2, %2\n"
        "mov r7, #4\n"
        "swi #0"
        : : "r"(STDOUT_FILENO), "r"(s), "r"(len)
        : "r0", "r1", "r2", "r7"
    );
}

void print_num(int num) {
    char buffer[12];
    int i = 0;
    int is_negative = 0;
    
    if (num < 0) {
        is_negative = 1;
        num = -num;
    }
    
    do {
        buffer[i++] = (num % 10) + '0';
        num /= 10;
    } while (num > 0);
    
    if (is_negative) {
        buffer[i++] = '-';
    }
    
    for (int j = 0; j < i/2; j++) {
        char temp = buffer[j];
        buffer[j] = buffer[i-1-j];
        buffer[i-1-j] = temp;
    }
    
    buffer[i] = '\0';
    print(buffer);
}

int main(int argc, char *argv[]) {
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
    
    2exit(0); // Usa _exit da libc
}