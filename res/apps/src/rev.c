/* rev.c - Inverte cada linha de um arquivo (como o rev do coreutils).
 *
 * Usa read/write crus + heap (malloc/realloc) da libc do emulador.
 * Compilar:
 *   ./build-elf.sh res/apps/src/rev.c -stdlib -o res/apps/dist/rev
 */
int printf(const char *fmt, ...);
int putchar(int c);
void *malloc(unsigned int n);
void *realloc(void *p, unsigned int n);
void free(void *p);
int read(int fd, void *buf, unsigned int count);
int write(int fd, const void *buf, unsigned int count);
int open(const char *path, int flags, int mode);
int close(int fd);
void exit(int status);

#define BUFSZ 4096

static char *readall(const char *path, int *outlen)
{
    int fd = open(path, 0, 0);
    if (fd < 0) { return 0; }
    int cap = BUFSZ, len = 0;
    char *buf = malloc(cap);
    if (buf == 0) { close(fd); return 0; }
    while (1) {
        int n = read(fd, buf + len, cap - len);
        if (n <= 0) { break; }
        len += n;
        if (len == cap) {
            char *nb = realloc(buf, cap * 2);
            if (nb == 0) { free(buf); close(fd); return 0; }
            buf = nb; cap *= 2;
        }
    }
    close(fd);
    *outlen = len;
    return buf;
}

static void printrev(const char *b, int start, int end)
{
    int j;
    for (j = end - 1; j >= start; j--) { putchar(b[j]); }
}

int main(int argc, char **argv)
{
    int len, i, start;
    char *buf;

    if (argc < 2) { printf("Usage: rev <file>\n"); exit(1); }
    buf = readall(argv[1], &len);
    if (buf == 0) { printf("rev: cannot open %s\n", argv[1]); exit(1); }

    start = 0;
    for (i = 0; i < len; i++) {
        if (buf[i] == '\n') {
            printrev(buf, start, i);
            putchar('\n');
            start = i + 1;
        }
    }
    if (start < len) { printrev(buf, start, len); }

    free(buf);
    exit(0);
    return 0;
}