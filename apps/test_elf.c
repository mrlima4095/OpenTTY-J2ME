/* test_elf_fixed.c - Teste corrigido para Clang */
/* Para compilar: clang -target arm-linux-gnueabi -nostdlib -O1 test_elf_fixed.c -o test_elf */

/* Definições de syscall */
#define SYS_WRITE   4
#define SYS_EXIT    1
#define SYS_GETPID  20
#define SYS_TIME    13
#define SYS_MMAP    90
#define SYS_MUNMAP  91
#define SYS_MPROTECT 125

/* Wrappers individuais mais simples */
static int sys_write(int fd, const char *buf, int count) {
    register int r7 asm("r7") = SYS_WRITE;
    register int r0 asm("r0") = fd;
    register const char *r1 asm("r1") = buf;
    register int r2 asm("r2") = count;
    
    asm volatile (
        "svc #0"
        : "+r"(r0)
        : "r"(r7), "r"(r1), "r"(r2)
        : "memory"
    );
    return r0;
}

static int sys_exit(int status) {
    register int r7 asm("r7") = SYS_EXIT;
    register int r0 asm("r0") = status;
    
    asm volatile (
        "svc #0"
        :
        : "r"(r7), "r"(r0)
        : "memory"
    );
    return r0; /* Nunca retorna */
}

static int sys_getpid(void) {
    register int r7 asm("r7") = SYS_GETPID;
    register int r0 asm("r0");
    
    asm volatile (
        "svc #0"
        : "=r"(r0)
        : "r"(r7)
        : "memory"
    );
    return r0;
}

static int sys_time(int *tloc) {
    register int r7 asm("r7") = SYS_TIME;
    register int r0 asm("r0") = (int)tloc;
    register int result asm("r0");
    
    asm volatile (
        "svc #0"
        : "=r"(result)
        : "r"(r7), "r"(r0)
        : "memory"
    );
    return result;
}

static void *sys_mmap(void *addr, int length, int prot, int flags, int fd, int offset) {
    register int r7 asm("r7") = SYS_MMAP;
    register void *result asm("r0");
    
    asm volatile (
        "svc #0"
        : "=r"(result)
        : "r"(r7), "r"(addr), "r"(length), "r"(prot), "r"(flags), "r"(fd), "r"(offset)
        : "memory"
    );
    return result;
}

/* Funções de utilidade */
void print_string(const char *str) {
    int len = 0;
    while (str[len]) len++;
    sys_write(1, str, len);
}

void print_char(char c) {
    sys_write(1, &c, 1);
}

void print_number(int num) {
    if (num == 0) {
        print_char('0');
        return;
    }
    
    if (num < 0) {
        print_char('-');
        num = -num;
    }
    
    char buffer[12];
    int i = 11;
    buffer[i] = 0;
    
    while (num > 0 && i > 0) {
        buffer[--i] = '0' + (num % 10);
        num /= 10;
    }
    
    print_string(&buffer[i]);
}

/* Função principal simplificada */
void _start(void) {
    /* 1. Teste de escrita */
    print_string("=== Teste ELF ARM ===\n");
    
    /* 2. Teste de SYS_GETPID */
    print_string("PID: ");
    int pid = sys_getpid();
    print_number(pid);
    print_string("\n");
    
    /* 3. Teste de SYS_TIME */
    print_string("Time: ");
    int time_val = sys_time(0);
    print_number(time_val);
    print_string("\n");
    
    /* 4. Teste matemático */
    print_string("Math test: ");
    int a = 6;
    int b = 7;
    int result = 0;
    for (int i = 0; i < b; i++) {
        result += a;
    }
    print_number(result); /* 42 */
    print_string("\n");
    
    /* 5. Teste de memória */
    char test[5];
    test[0] = 'T';
    test[1] = 'E';
    test[2] = 'S';
    test[3] = 'T';
    test[4] = 0;
    print_string("Memory: ");
    print_string(test);
    print_string("\n");
    
    /* 6. Teste simples de MMAP */
    print_string("MMAP test: ");
    void *mem = sys_mmap(0, 4096, 3, 0x22, -1, 0);
    
    if ((int)mem < 0) {
        print_string("FAILED\n");
    } else {
        char *ptr = (char *)mem;
        ptr[0] = 'O';
        ptr[1] = 'K';
        ptr[2] = 0;
        print_string(ptr);
        
        /* Cleanup */
        sys_munmap(mem, 4096);
        print_string(" (unmapped)\n");
    }
    
    /* 7. Teste de controle de fluxo */
    print_string("Branch test: ");
    if (10 < 20) {
        print_string("PASS");
    } else {
        print_string("FAIL");
    }
    print_string("\n");
    
    /* 8. Loop test */
    print_string("Loop: ");
    for (int i = 0; i < 5; i++) {
        print_char('0' + i);
    }
    print_string("\n");
    
    print_string("=== SUCCESS ===\n");
    
    /* Exit */
    sys_exit(0);
}