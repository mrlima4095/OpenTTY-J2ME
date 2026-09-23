/* tac.c - Imprime as linhas de um arquivo na ordem inversa.
 *
 * Le o arquivo inteiro, guarda o inicio de cada linha e imprime de tras
 * pra frente, como o tac do coreutils. Heap + read/write da libc.
 * Compilar:
 *   ./build-elf.sh res/apps/src/tac.c -stdlib -o res/apps/dist/tac
 */
int printf(const char *fmt, ...);
int putchar(int c);
void *malloc(unsigned int n);
void *calloc(unsigned int nmemb, unsigned int size);
void free(void *p);
int read(int fd, void *buf, unsigned int count);
int write(int fd, const void *buf, unsigned int count);
int open(const char *path, int flags, int mode);
int close(int fd);
void exit(int status);

#define BUFSZ 4096

int main(int argc, char **argv)
{
    int fd, cap = BUFSZ, len = 0, n, i, nl, lin;
    char *buf, *starts;
    int *lens;
    int nlines = 0;

    if (argc < 2) { printf("Usage: tac <file>\n"); exit(1); }
    fd = open(argv[1], 0, 0);
    if (fd < 0) { printf("tac: cannot open %s\n", argv[1]); exit(1); }

    buf = malloc(cap);
    if (buf == 0) { close(fd); exit(1); }
    while (1) {
        n = read(fd, buf + len, cap - len);
        if (n <= 0) { break; }
        len += n;
        if (len == cap) {
            cap *= 2;
            char *nb = malloc(cap);
            if (nb == 0) { free(buf); close(fd); exit(1); }
            for (i = 0; i < len; i++) { nb[i] = buf[i]; }
            free(buf);
            buf = nb;
        }
    }
    close(fd);

    for (i = 0; i < len; i++) { if (buf[i] == '\n') { nlines++; } }
    starts = calloc(nlines + 1, 1);
    lens = (int *) calloc(nlines + 1, 4);
    if (starts == 0 || lens == 0) { free(buf); free(starts); free(lens); exit(1); }

    nl = 0; starts[0] = 0; i = 0;
    while (i < len) {
        if (buf[i] == '\n') {
            int thislen = i - starts[nl];
            if (thislen > 0) { lens[nl] = thislen; }
            nl++;
            i++;
            starts[nl] = i;
        } else { i++; }
    }
    if (len > 0 && buf[len - 1] != '\n') { lens[nl] = len - starts[nl]; }
    else { nl--; }

    for (lin = nl; lin >= 0; lin--) {
        write(1, buf + starts[lin], lens[lin]);
        putchar('\n');
    }

    free(buf); free(starts); free(lens);
    exit(0);
    return 0;
}