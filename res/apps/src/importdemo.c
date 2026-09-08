int printf(const char *fmt, ...);
extern int triple(int);
extern int fib(int);
extern int bump(void);
extern int counter;
int main(int argc, char **argv) {
    printf("triple(7)=%d fib(12)=%d\n", triple(7), fib(12));
    bump(); bump(); bump();
    printf("counter=%d counter*3=%d\n", counter, triple(counter));
    return 0;
}