/* rot13.c - ROT13: rotaciona as letras A-Z/a-z em 13 posicoes.
 *
 * rot13 <file>  -> imprime o arquivo com as letras rotacionadas.
 * Como o cat.s, usa open/read/write crus + heap da libc do emulador.
 * Compilar:
 *   ./build-elf.sh res/apps/src/rot13.c -stdlib -o res/apps/dist/rot13
 */
int printf(const char *fmt, ...);
int putchar(int c);
void *malloc(unsigned int n);
void free(void *p);
int read(int fd, void *buf, unsigned int count);
int write(int fd, const void *buf, unsigned int count);
int open(const char *path, int flags, int mode);
int close(int fd);
void exit(int status);

static int rotbyte(int c)
{
    if (c >= 'a' && c <= 'z') { return 'a' + (c - 'a' + 13) % 26; }
    if (c >= 'A' && c <= 'Z') { return 'A' + (c - 'A' + 13) % 26; }
    return c;
}

int main(int argc, char **argv)
{
    int fd, n, i;
    char buf[4096];

    if (argc < 2) { printf("Usage: rot13 <file>\n"); exit(1); }
    fd = open(argv[1], 0, 0);
    if (fd < 0) { printf("rot13: cannot open %s\n", argv[1]); exit(1); }

    while ((n = read(fd, buf, 4096)) > 0) {
        for (i = 0; i < n; i++) { buf[i] = rotbyte(buf[i]); }
        write(1, buf, n);
    }
    close(fd);

    exit(0);
    return 0;
}