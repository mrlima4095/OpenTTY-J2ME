/* brainfuck.c - Interprete Brainfuck (ELF RV32IM para o emulador do OpenTTY).
 *
 * Compilar:
 *   ./build-elf.sh res/apps/src/brainfuck.c -stdlib -o res/apps/dist/brainfuck
 *
 * Uso no dispositivo: brainfuck <prog.bf>
 *
 * Notas do port:
 *   - ',' (ler byte de stdin) ainda nao e' implementado no emulador
 *     (read(0) retorna 0 bytes), entao a celula atual recebe 0.
 *   - usa so a -stdlib do emulador (printf/putchar/malloc/...): nenhum
 *     header de sistema. Evita div/mod assinados (que gerariam helpers
 *     __divsi3/__modsi3) - o core RV32IM tem M-ext, entao * e + sao
 *     instrucoes nativas.
 */

int printf(const char *fmt, ...);
int putchar(int c);
void *malloc(unsigned int size);
void *realloc(void *p, unsigned int size);
void free(void *p);
void *memset(void *s, int c, unsigned int n);
int open(const char *path, int flags, int mode);
int read(int fd, void *buf, int count);
int close(int fd);

#define O_RDONLY   0
#define CELLS      30000      /* tape */

/* Le o arquivo inteiro para um buffer alocado. Retorna 0 em erro. */
static char *read_all(const char *path, int *len)
{
    char *buf = 0;
    int cap = 0, sz = 0, fd, n;

    fd = open(path, O_RDONLY, 0);
    if (fd < 0) return 0;

    for (;;) {
        if (sz == cap) {
            char *nb;
            int ncap = (cap == 0) ? 4096 : cap * 2;
            nb = (char *)realloc(buf, ncap);
            if (!nb) { if (buf) free(buf); close(fd); return 0; }
            buf = nb;
            cap = ncap;
        }
        n = read(fd, buf + sz, cap - sz);
        if (n <= 0) break;
        sz += n;
    }

    close(fd);
    if (len) *len = sz;
    return buf;
}

int main(int argc, char **argv)
{
    char *prog;
    unsigned char *cells;
    int *match;      /* tabela de pares '[' <-> ']' (=-1 sem par) */
    int *build;      /* pilha usada so na construcao da tabela */
    int len = 0, pc = 0, cp = 0, sp = 0, i;

    if (argc < 2) {
        printf("Usage: brainfuck <file.bf>\n");
        printf("Brainfuck interpreter for OpenTTY (RISC-V RV32IM)\n");
        return 1;
    }

    prog = read_all(argv[1], &len);
    if (!prog) {
        printf("brainfuck: cannot open '%s'\n", argv[1]);
        return 1;
    }

    cells = (unsigned char *)malloc(CELLS);
    match = (int *)malloc(len * 4);
    build = (int *)malloc(len * 4);
    if (!cells || !match || !build) {
        printf("brainfuck: out of memory\n");
        return 1;
    }
    memset(cells, 0, CELLS);
    memset(match, 0xFF, len * 4);   /* -1 = colchete sem par */

    /* 1o passe: casa cada '[' com seu ']' (index tem a mesma ordem). */
    for (i = 0; i < len; i++) {
        if (prog[i] == '[') {
            build[sp++] = i;
        } else if (prog[i] == ']') {
            if (sp > 0) {
                sp--;
                build[build[sp]] = i;   /* match[abertura] = fecho   */
                match[i] = build[sp];   /* match[fecho]   = abertura */
            }
        }
    }
    for (i = 0; i < sp; i++) match[build[i]] = -1;   /* '[' nao fechado */

    /* 2o passe: interpreta. match[] = -1 faz '['/'[' ignorar o colchete. */
    while (pc < len) {
        char op = prog[pc];

        if (op == '>') { cp++; if (cp >= CELLS) cp = 0; }
        else if (op == '<') { cp--; if (cp < 0) cp = CELLS - 1; }
        else if (op == '+') { cells[cp] = (unsigned char)(cells[cp] + 1); }
        else if (op == '-') { cells[cp] = (unsigned char)(cells[cp] - 1); }
        else if (op == '.') { putchar(cells[cp]); }
        else if (op == ',') {
            int nb = read(0, &cells[cp], 1);
            if (nb != 1) cells[cp] = 0;   /* stdin ainda nao disponivel */
        }
        else if (op == '[') {
            if (cells[cp] == 0 && match[pc] >= 0) pc = match[pc];
        }
        else if (op == ']') {
            if (cells[cp] != 0 && match[pc] >= 0) pc = match[pc];
        }

        pc++;
    }

    printf("\n");
    return 0;
}