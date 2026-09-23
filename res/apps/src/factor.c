/* factor.c - Fatoracao em numeros primos.
 *
 * factor N            -> mostra os fatores primos de N
 * Usa apenas printf()/atoi() da libc do emulador (RV32IM tem M-end).
 * Compilar:
 *   ./build-elf.sh res/apps/src/factor.c -stdlib -o res/apps/dist/factor
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);

int main(int argc, char **argv)
{
    int n, p, first = 1;

    if (argc < 2) { printf("Usage: factor N\n"); exit(1); }
    n = atoi(argv[1]);
    if (n < 0) { n = -n; }
    if (n == 0) { printf("0\n"); exit(0); }

    printf("%d = ", n);
    p = 2;
    if (n > 1) {
        for (p = 2; p * p <= n; p += (p == 2 ? 1 : 2)) {
            while (n % p == 0) {
                printf("%d ", p);
                first = 0;
                n /= p;
            }
        }
    }
    if (n > 1) { printf("%d", n); }
    else if (first) { printf("1"); }
    printf("\n");

    exit(0);
    return 0;
}