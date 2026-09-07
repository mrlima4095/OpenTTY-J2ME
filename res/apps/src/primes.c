/* primes.c - Numeros primos por divisao por tentativa (mod / %).
 *
 * Uso: primes [limite]   (default 1000)
 * Demonstra % , %u/%x e loops com make de char map.
 * Compilar:
 *   ./build-elf.sh res/apps/src/primes.c -stdlib -o res/apps/dist/primes
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
int abs(int v);
void exit(int status);

int is_prime(int n)
{
    int i;
    if (n < 2) { return 0; }
    for (i = 2; i * i <= n; i++) { if (n % i == 0) { return 0; } }
    return 1;
}

int main(int argc, char **argv)
{
    int lim = 1000, count = 0, last = 0, i;
    if (argc > 1) { lim = atoi(argv[1]); }
    if (lim < 2) { lim = 2; }
    if (lim > 100000) { lim = 100000; }

    printf("primos ate %d:\n", lim);
    for (i = 2; i <= lim; i++) {
        if (is_prime(i)) { count++; last = i; }
    }
    printf("total=%d ultimo=%d\n", count, last);
    printf("lim=%d em hex=%x oct=%o abs(-13)=%d\n", lim, lim, lim, abs(-13));

    exit(0);
    return 0;
}