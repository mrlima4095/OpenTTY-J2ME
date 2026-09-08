/* collatz.c - Sequencia de Collatz ate 1 (passos e pico).
 *
 * Uso: collatz [n]   (default 27)
 * Demonstra % (mod), divisao por 2 e multiplicacao por 3 num loop while.
 * Compilar:
 *   ./build-elf.sh res/apps/src/collatz.c -stdlib -o res/apps/dist/collatz
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);

int main(int argc, char **argv)
{
    int n = (argc > 1) ? atoi(argv[1]) : 27;
    int steps = 0, peak = n;
    int first = 1;

    if (n < 1) { n = 1; }

    printf("seed=%d\n", n);
    while (n != 1) {
        if (!first) { printf(" "); }
        first = 0;
        printf("%d", n);
        if ((n % 2) == 0) { n = n / 2; } else { n = 3 * n + 1; }
        if (n > peak) { peak = n; }
        steps++;
    }
    printf(" 1 (steps=%d peak=%d)\n", steps, peak);

    exit(0);
    return 0;
}