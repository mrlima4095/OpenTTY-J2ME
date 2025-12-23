/* test_final.c - Versão final testada */
/* clang -target arm-linux-gnueabi -nostdlib -O1 test_final.c -o test_final */

#define SYS_WRITE 4
#define SYS_EXIT 1
#define SYS_GETPID 20
#define SYS_TIME 13
#define SYS_MMAP 90
#define SYS_MUNMAP 91

/* Syscall helpers */
static int syscall3(int nr, int a1, int a2, int a3) {
    register int r7 asm("r7") = nr;
    register int r0 asm("r0") = a1;
    register int r1 asm("r1") = a2;
    register int r2 asm("r2") = a3;
    asm volatile("svc #0" : "+r"(r0) : "r"(r7), "r"(r1), "r"(r2) : "memory");
    return r0;
}

static int syscall6(int nr, int a1, int a2, int a3, int a4, int a5, int a6) {
    register int r7 asm("r7") = nr;
    register int r0 asm("r0") = a1;
    register int r1 asm("r1") = a2;
    register int r2 asm("r2") = a3;
    register int r3 asm("r3") = a4;
    register int r4 asm("r4") = a5;
    register int r5 asm("r5") = a6;
    asm volatile("svc #0" : "+r"(r0) : "r"(r7), "r"(r1), "r"(r2), "r"(r3), "r"(r4), "r"(r5) : "memory");
    return r0;
}

/* Syscall wrappers */
static int write(int fd, const char *buf, int len) {
    return syscall3(SYS_WRITE, fd, (int)buf, len);
}

static void exit(int code) {
    syscall3(SYS_EXIT, code, 0, 0);
}

static int getpid(void) {
    return syscall3(SYS_GETPID, 0, 0, 0);
}

static int time(int *t) {
    return syscall3(SYS_TIME, (int)t, 0, 0);
}

static void *mmap(void *addr, int len, int prot, int flags, int fd, int off) {
    return (void*)syscall6(SYS_MMAP, (int)addr, len, prot, flags, fd, off);
}

static int munmap(void *addr, int len) {
    return syscall3(SYS_MUNMAP, (int)addr, len, 0);
}

/* Utility functions */
void print(const char *s) {
    int len = 0;
    while (s[len]) len++;
    write(1, s, len);
}

void print_num(int n) {
    if (n == 0) {
        write(1, "0", 1);
        return;
    }
    
    char buf[12];
    int i = 11;
    buf[i] = 0;
    
    if (n < 0) {
        write(1, "-", 1);
        n = -n;
    }
    
    while (n > 0) {
        buf[--i] = '0' + (n % 10);
        n /= 10;
    }
    
    print(&buf[i]);
}

/* Main */
void _start() {
    print("=== ARM ELF Test ===\n");
    
    print("PID: ");
    print_num(getpid());
    print("\n");
    
    print("Time: ");
    print_num(time(0));
    print("\n");
    
    print("Math: ");
    int sum = 0;
    for (int i = 1; i <= 10; i++) sum += i;
    print_num(sum); /* 55 */
    print("\n");
    
    print("MMAP: ");
    void *m = mmap(0, 4096, 3, 0x22, -1, 0);
    if ((int)m < 0) {
        print("FAIL\n");
    } else {
        char *p = (char*)m;
        p[0] = 'O'; p[1] = 'K'; p[2] = '\n'; p[3] = 0;
        print(p);
        munmap(m, 4096);
        print("Unmapped OK\n");
    }
    
    print("=== DONE ===\n");
    exit(0);
}