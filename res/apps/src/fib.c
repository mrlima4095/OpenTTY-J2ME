/* fib.c - Sequencia de Fibonacci (iterativo + recursivo).
 *
 * O recursivo fib(20) faz ~21 mil chamadas: bom stress de CALL/stack.
 * Compilar:
 *   ./build-elf.sh res/apps/src/fib.c -stdlib -o res/apps/dist/fib
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);

int fib_iter(int n)
{
    int a = 0, b = 1, i;
    if (n < 2) { return n; }
    for (i = 2; i <= n; i++) { int t = a + b; a = b; b = t; }
    return b;
}

int fib_recur(int n)
{
    if (n < 2) { return n; }
    return fib_recur(n - 1) + fib_recur(n - 2);
}

int main(int argc, char **argv)
{
    int n = 20, i;
    if (argc > 1) { n = atoi(argv[1]); }
    if (n < 0) { n = 0; }
    if (n > 24) { n = 24; }

    for (i = 0; i <= n; i++) {
        printf("fib(%d)=%d\n", i, fib_iter(i));
    }
    printf("fib_iter(%d)=%d fib_recur(%d)=%d\n",
           n, fib_iter(n), n, fib_recur(n));

    exit(0);
    return 0;
}