/* sort.c - Bubble sort dos argumentos numericos (heap via malloc).
 *
 * Uso: sort 5 3 9 1 4 7
 * Demonstra malloc/calloc/free, atoi e trocas em vetor.
 * Compilar:
 *   ./build-elf.sh res/apps/src/sort.c -stdlib -o res/apps/dist/sort
 */
int printf(const char *fmt, ...);
int atoi(const char *s);
void *malloc(unsigned int n);
void *calloc(unsigned int nmemb, unsigned int size);
void free(void *p);
void *memcpy(void *d, const void *s, unsigned int n);
void *memmove(void *d, const void *s, unsigned int n);
int memcmp(const void *a, const void *b, unsigned int n);
void exit(int status);

void print_arr(int *a, int n)
{
    int i;
    printf("[");
    for (i = 0; i < n; i++) { printf("%d", a[i]); if (i + 1 < n) { printf(" "); } }
    printf("]\n");
}

void bubble(int *a, int n)
{
    int i, j;
    for (i = 0; i < n - 1; i++) {
        for (j = 0; j < n - 1 - i; j++) {
            if (a[j] > a[j + 1]) { int t = a[j]; a[j] = a[j + 1]; a[j + 1] = t; }
        }
    }
}

int main(int argc, char **argv)
{
    int n = argc - 1, i;
    int *a;
    if (n < 1) { n = 1; }
    a = (int *) calloc(n, sizeof(int));
    if (a == 0) { printf("sem memoria\n"); exit(1); }

    for (i = 0; i < argc - 1; i++) { a[i] = atoi(argv[i + 1]); }
    printf("entrada:  "); print_arr(a, n);
    bubble(a, n);
    printf("ordenado: "); print_arr(a, n);

    memcpy(a, a, (unsigned int) n * sizeof(int));   /* copia sobre si mesmo */
    memmove(a, a, (unsigned int) n * sizeof(int));
    printf("memc op=%d\n", memcmp(a, a, (unsigned int) n * sizeof(int)));

    free(a);
    exit(0);
    return 0;
}