#define STDOUT_FILENO 1

void sys_exit(int code) {
    asm volatile ("mov r0,%0; mov r7,#1; swi #0" : : "r"(code) : "r0", "r7");
    while(1);
}

void sys_write(int fd, const char *buf, int count) {
    asm volatile ("mov r0,%0; mov r1,%1; mov r2,%2; mov r7,#4; swi #0" 
                 : : "r"(fd), "r"(buf), "r"(count) : "r0", "r1", "r2", "r7");
}

void print(const char *s) {
    int len = 0;
    while (s[len]) len++;
    sys_write(STDOUT_FILENO, s, len);
}

void _start() {
    int argc;
    char **argv;
    
    asm volatile ("mov %0,r0; mov %1,r1" : "=r"(argc), "=r"(argv));
    
    for (int i = 0; i < argc; i++) {
        print(argv[i]);
        print("\n");
    }
    
    sys_exit(0);
}

asm(".global _start");