/* c4cc.c - deliberately small, on-device C-to-RV32IM compiler for OpenTTY.
 *
 * Build: ./build-elf.sh res/apps/src/c4cc.c -stdlib -o res/apps/dist/c4cc
 * Use:   c4cc source.c [output]
 *
 * Accepted input is one int main(...) definition.  Its body may contain
 * expression statements, return expressions, puts("text"), putchar(expr),
 * and printf("format", expr).  Expressions are signed integer literals,
 * parentheses, unary +/-, and + - * /.  Both // and block comments work.
 * This is intentionally not a general C compiler: declarations, variables,
 * control flow, escapes other than \n, and arbitrary function calls are out.
 */
int open(const char *path, int flags, int mode);
int read(int fd, void *buf, int count);
int write(int fd, const void *buf, int count);
int close(int fd);
void *malloc(int size);
void free(void *p);
int printf(const char *fmt, ...);

#define BASE 0x10000
#define MAXSRC 32768
#define MAXCODE 16384
#define MAXDATA 16384

char *src, *p;
int line, tok, num, str;
int *code, nc, *patch, np;
char *data;
int nd, failed;

enum { EOF_T = 256, ID, NUM, STR };

void error(char *s)
{
    if (!failed) printf("c4cc:%d: %s\n", line, s);
    failed = 1;
}

void put16(char *b, int at, int n) { b[at] = n; b[at + 1] = n >> 8; }
void put32(char *b, int at, int n)
{
    b[at] = n; b[at + 1] = n >> 8; b[at + 2] = n >> 16; b[at + 3] = n >> 24;
}

void emit(int n)
{
    if (nc >= MAXCODE) { error("generated code is too large"); return; }
    code[nc++] = n;
}

/* RV32I encoders; all generated code stays in the single load segment. */
void addi(int rd, int rs, int imm) { emit(((imm & 4095) << 20) | (rs << 15) | (rd << 7) | 0x13); }
void alu(int op, int rd, int a, int b) { emit(op | (b << 20) | (a << 15) | (rd << 7) | 0x33); }
void loadaddr(int rd, int dataoff)
{
    emit(rd << 7 | 0x37);                 /* patched once text size is known */
    emit(rd << 7 | 0x13);
    if (np >= MAXDATA - 1) error("too many string addresses");
    else patch[np++] = nc - 2, patch[np++] = dataoff;
}
void li(int rd, int n)
{
    int hi = (n + 0x800) >> 12;
    emit((hi << 12) | (rd << 7) | 0x37);
    addi(rd, rd, n - (hi << 12));
}

void next()
{
    int c, q;
again:
    while (*p == ' ' || *p == '\t' || *p == '\r' || *p == '\n') {
        if (*p++ == '\n') line++;
    }
    if (p[0] == '/' && p[1] == '/') { p += 2; while (*p && *p != '\n') p++; goto again; }
    if (p[0] == '/' && p[1] == '*') {
        p += 2;
        while (*p && !(p[0] == '*' && p[1] == '/')) { if (*p++ == '\n') line++; }
        if (!*p) { error("unterminated comment"); tok = EOF_T; return; }
        p += 2; goto again;
    }
    c = *p++;
    if (!c) { tok = EOF_T; return; }
    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
        char *start = p - 1; while ((*p >= 'a' && *p <= 'z') || (*p >= 'A' && *p <= 'Z') || (*p >= '0' && *p <= '9') || *p == '_') p++;
        tok = ID; str = (int)start; return;
    }
    if (c >= '0' && c <= '9') { num = c - '0'; while (*p >= '0' && *p <= '9') num = num * 10 + *p++ - '0'; tok = NUM; return; }
    if (c == '"') {
        q = nd;
        while (*p && *p != '"') {
            c = *p++; if (c == '\\' && *p == 'n') { p++; c = '\n'; }
            if (nd >= MAXDATA - 1) { error("string data is too large"); break; }
            data[nd++] = c;
        }
        if (*p != '"') error("unterminated string"); else p++;
        data[nd++] = 0; tok = STR; str = q; return;
    }
    tok = c;
}

int named(char *s)
{
    char *a = (char *)str;
    while (*s && *a == *s) { s++; a++; }
    return !*s && !((*a >= 'a' && *a <= 'z') || (*a >= 'A' && *a <= 'Z') || (*a >= '0' && *a <= '9') || *a == '_');
}
void need(int t, char *s) { if (tok != t) error(s); else next(); }

void expr();
void primary()
{
    if (tok == NUM) { li(10, num); next(); }
    else if (tok == '(') { next(); expr(); need(')', "expected ')'"); }
    else if (tok == '+') { next(); primary(); }
    else if (tok == '-') { next(); primary(); emit((0x20 << 25) | (10 << 20) | (0 << 15) | (10 << 7) | 0x33); }
    else { error("expected integer expression"); }
}
void term()
{
    int op;
    primary();
    while (tok == '*' || tok == '/') {
        op = tok; next(); addi(2, 2, -4); emit((10 << 20) | (2 << 15) | 0x2023);
        primary(); emit((2 << 15) | (5 << 7) | 0x2283); addi(2, 2, 4);
        alu(op == '*' ? (1 << 25) : ((1 << 25) | (4 << 12)), 10, 5, 10);
    }
}
void expr()
{
    int op;
    term();
    while (tok == '+' || tok == '-') {
        op = tok; next(); addi(2, 2, -4); emit((10 << 20) | (2 << 15) | 0x2023);
        term(); emit((2 << 15) | (5 << 7) | 0x2283); addi(2, 2, 4);
        alu(op == '+' ? 0 : (0x20 << 25), 10, 5, 10);
    }
}

void statement()
{
    int which, fmt;
    if (tok == ID && named("return")) { next(); expr(); need(';', "expected ';' after return"); li(17, 1); emit(0x73); return; }
    if (tok != ID) { error("expected return or supported call"); return; }
    which = named("puts") ? 1 : named("putchar") ? 2 : named("printf") ? 3 : 0;
    if (!which) { error("only puts, putchar, and printf calls are supported"); return; }
    next(); need('(', "expected '(' after function name");
    if (which == 1) {
        if (tok != STR) error("puts requires a string literal"); else { loadaddr(10, str); next(); }
        need(')', "expected ')' after puts argument"); li(17, 1018); emit(0x73);
    } else if (which == 2) {
        expr(); need(')', "expected ')' after putchar argument"); li(17, 1017); emit(0x73);
    } else {
        if (tok != STR) error("printf requires a string literal format"); else { fmt = str; next(); }
        if (tok != ',') error("printf requires one integer expression"); else { next(); expr(); emit((10 << 20) | (0 << 15) | (11 << 7) | 0x33); loadaddr(10, fmt); }
        need(')', "expected ')' after printf arguments"); li(17, 1019); emit(0x73);
    }
    need(';', "expected ';' after call");
}

int main(int argc, char **argv)
{
    int fd, n, out, i, textbytes, addr, at;
    char hdr[256], word[4], *name;
    if (argc < 2 || argc > 3) { printf("usage: c4cc <source.c> [output]\n"); return 1; }
    src = malloc(MAXSRC); code = malloc(MAXCODE * 4); patch = malloc(MAXDATA * 4); data = malloc(MAXDATA);
    if (!src || !code || !patch || !data) { printf("c4cc: out of memory\n"); return 1; }
    fd = open(argv[1], 0, 0); if (fd < 0) { printf("c4cc: cannot open %s\n", argv[1]); return 1; }
    n = read(fd, src, MAXSRC - 1); close(fd); if (n < 0) { printf("c4cc: cannot read %s\n", argv[1]); return 1; } src[n] = 0;
    p = src; line = 1; next();
    if (!(tok == ID && named("int"))) error("expected int main definition"); else next();
    if (!(tok == ID && named("main"))) error("expected main"); else next();
    need('(', "expected '(' after main");
    while (tok != ')' && tok != EOF_T) next();
    need(')', "expected ')' after main parameters"); need('{', "expected '{' before main body");
    while (tok != '}' && tok != EOF_T && !failed) statement();
    need('}', "expected '}' after main body");
    if (tok != EOF_T) error("only one main definition is supported");
    if (!failed) { li(10, 0); li(17, 1); emit(0x73); }
    if (failed) return 1;
    textbytes = nc * 4;
    for (i = 0; i < np; i += 2) {
        at = patch[i]; addr = BASE + textbytes + patch[i + 1];
        code[at] = (((addr + 0x800) >> 12) << 12) | (10 << 7) | 0x37;
        code[at + 1] = (((addr - (((addr + 0x800) >> 12) << 12)) & 4095) << 20) | (10 << 15) | (10 << 7) | 0x13;
    }
    name = argc == 3 ? argv[2] : argv[1];
    if (argc == 2) { for (i = 0; name[i]; i++) {} if (i > 2 && name[i-2] == '.' && name[i-1] == 'c') name[i-2] = 0; }
    out = open(name, 0x241, 0755); if (out < 0) { printf("c4cc: cannot create %s\n", name); return 1; }
    for (i = 0; i < 256; i++) hdr[i] = 0;
    hdr[0] = 0x7f; hdr[1] = 'E'; hdr[2] = 'L'; hdr[3] = 'F'; hdr[4] = 1; hdr[5] = 1; hdr[6] = 1;
    put16(hdr, 16, 2); put16(hdr, 18, 243); put32(hdr, 20, 1); put32(hdr, 24, BASE); put32(hdr, 28, 52); put16(hdr, 40, 52); put16(hdr, 42, 32); put16(hdr, 44, 1);
    put32(hdr, 52, 1); put32(hdr, 56, 0x100); put32(hdr, 60, BASE); put32(hdr, 64, BASE); put32(hdr, 68, textbytes + nd); put32(hdr, 72, textbytes + nd); put32(hdr, 76, 5); put32(hdr, 80, 0x1000);
    write(out, hdr, 256);
    for (i = 0; i < nc; i++) { put32(word, 0, code[i]); write(out, word, 4); }
    if (nd) write(out, data, nd);
    close(out); printf("c4cc: wrote %s (%d bytes)\n", name, 256 + textbytes + nd);
    return 0;
}
