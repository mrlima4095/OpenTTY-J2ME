/* sum.c - Soma, min, max e media dos argumentos numericos.
 *
 * Uso: sum 10 20 30 5
 * Demonstra atoi, loop em argv e divisao/modo com resto.
 * Compilar:
 *   ./build-elf.sh res/apps/src/sum.c -stdlib -o res/apps/dist/sum
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);

int main(int argc, char **argv)
{
    int n = argc - 1, i, sum = 0, min, max;

    if (n < 1) { printf("uso: sum n1 n2 ...\n"); exit(0); }

    min = atoi(argv[1]);
    max = min;
    for (i = 1; i <= n; i++) {
        int v = atoi(argv[i]);
        sum += v;
        if (v < min) { min = v; }
        if (v > max) { max = v; }
    }

    printf("sum=%d min=%d max=%d med=%d resto=%d\n", sum, min, max,
           sum / n, sum % n);

    exit(0);
    return 0;
}