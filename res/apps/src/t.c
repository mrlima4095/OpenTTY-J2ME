/* ultra-mini-libc.c */
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

int main() {
    print("Hello World!\n");
    exit(0);
}