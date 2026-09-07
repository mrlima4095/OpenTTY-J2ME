/* math.c - Matematica basica (div mod % 64-bit, abs, casos aritmeticos).
 *
 * CUIDADO: nao fazer multiplicacao de long long (precisa de __aeabi_lmul,
 * que nao existe na lib). Usamos so soma/multiplicacao int (mul nativo) e
 * divisao 64-bit (__aeabi_ldivmod, suportada).
 * Compilar:
 *   ./build-elf.sh res/apps/src/math.c -stdlib -o res/apps/dist/math
 */
int printf(const char *fmt, ...);
int sprintf(char *buf, const char *fmt, ...);
int atoi(const char *s);
int abs(int v);
void exit(int status);

int gcd(int a, int b)
{
    while (b != 0) { int r = a % b; a = b; b = r; }
    return a;
}

int is_even(int n) { return (n & 1) == 0; }

int main(int argc, char **argv)
{
    int a = 42, b = 30, n = 10, i, f = 1;
    if (argc > 2) { a = atoi(argv[1]); b = atoi(argv[2]); }

    printf("gcd(%d,%d)=%d lcm=%d\n", a, b, gcd(a, b),
           a / gcd(a, b) * b);
    printf("abs(-%d)=%d %d%%2=%d\n", a, abs(-a), a, a % 2);

    for (i = 2; i <= n; i++) { f = f * i; }
    printf("%d!=%d is_even(%d)=%d\n", n, f, n, is_even(n));

    /* divisao 64-bit: __aeabi_ldivmod / __aeabi_uldivmod */
    long long big = (long long) 1000000000 * 3;   /* int*int -> ll (sem lmul) */
    long long q = big / 7ll, r = big % 13ll;
    printf("64bit %d*3=%lld /7=%lld %%13=%lld\n", 1000000000,
           big, q, r);

    char buf[48];
    sprintf(buf, "a=0x%x b=0x%o\n", a, b);
    printf("fmt %s", buf);

    exit(0);
    return 0;
}