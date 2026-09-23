/* seq.c - Imprime uma sequencia de numeros inteiros.
 *
 * seq LAST            -> 1..LAST
 * seq FIRST LAST      -> FIRST..LAST
 * seq FIRST INCR LAST -> imprime com passo INCR
 * Usa apenas printf()/atoi() da libc do emulador.
 * Compilar:
 *   ./build-elf.sh res/apps/src/seq.c -stdlib -o res/apps/dist/seq
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);

int main(int argc, char **argv)
{
    long first = 1, incr = 1, last, v, guard = 0;

    if (argc < 2 || argc > 4) { printf("Usage: seq [FIRST [INCR]] LAST\n"); exit(1); }

    if (argc == 2) { last = atoi(argv[1]); }
    else if (argc == 3) {
        first = atoi(argv[1]);
        last = atoi(argv[2]);
        incr = first <= last ? 1 : -1;
    } else {
        first = atoi(argv[1]);
        incr = atoi(argv[2]);
        last = atoi(argv[3]);
        if (incr == 0) { printf("seq: invalid increment\n"); exit(1); }
    }

    v = first;
    if (incr > 0) {
        while (v <= last && guard < 100000) {
            printf("%ld", v);
            v += incr;
            guard++;
            if (v <= last) { printf(" "); }
        }
    } else {
        while (v >= last && guard < 100000) {
            printf("%ld", v);
            v += incr;
            guard++;
            if (v >= last) { printf(" "); }
        }
    }
    printf("\n");

    exit(0);
    return 0;
}