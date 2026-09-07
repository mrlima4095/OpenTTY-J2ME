/* tower.c - Torres de Hanoi (recursao + contagem de movimentos).
 *
 * Demonstra chamadas recursivas profundas, char ou char %c.
 * Compilar:
 *   ./build-elf.sh res/apps/src/tower.c -stdlib -o res/apps/dist/tower
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void exit(int status);

int hanoi(int n, char a, char b, char c)
{
    int m = 0;
    if (n == 0) { return 0; }
    m += hanoi(n - 1, a, c, b);
    printf("move disco %d de %c para %c\n", n, a, b);
    m += 1;
    m += hanoi(n - 1, c, b, a);
    return m;
}

int main(int argc, char **argv)
{
    int n = 5, moves;
    if (argc > 1) { n = atoi(argv[1]); }
    if (n < 1) { n = 1; }
    if (n > 12) { n = 12; }

    printf("Hanoi com %d discos (A->B):\n", n);
    moves = hanoi(n, 'A', 'B', 'C');
    printf("total de movimentos=%d\n", moves);

    exit(0);
    return 0;
}