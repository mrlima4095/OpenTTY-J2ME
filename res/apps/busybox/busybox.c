/* busybox.c - multi-tool estilo busybox para o emulador ELF ARM32 do OpenTTY.
 *
 * Um UNICO ELF estatico que despacha pelo basename(argv[0]). Para instalar,
 * copie o mesmo binario em /bin/<comando> (ex.: /bin/cat, /bin/ls, /bin/wc).
 * Tambem funciona como `busybox <applet> [args...]` e `busybox --list`.
 *
 * Compilar:
 *   ./build-elf.sh res/apps/busybox/busybox.c res/apps/busybox/bb_sys.s -stdlib -o res/apps/busybox/busybox
 *   (ou use res/apps/busybox/build.sh)
 *
 * Restricoes do emulador respeitadas: sem float, sem long long, sem short,
 * printf so com especificadores suportados pelo emulador (%d %u %x %X %o %s
 * %c %p %%), sem fork/pipe.
 */
#include "busybox.h"

char **g_envp = 0;

/* ================= helpers compartilhados ================= */

int bb_out(const char *s) { int n = strlen(s); if (n <= 0) return 0; return write(1, s, n); }
int bb_outln(const char *s) { bb_out(s); return bb_putc('\n'); }
int bb_putc(char c) { return write(1, &c, 1); }
int bb_err(const char *s) { int n = strlen(s); if (n <= 0) return 0; return write(2, s, n); }

const char *bb_basename(const char *path)
{
    const char *b = path;
    while (*path) { if (*path == '/') b = path + 1; path++; }
    return b;
}

int bb_is_dir(const char *path)
{
    int fd = open(path, 0x10000, 0);   /* O_DIRECTORY */
    if (fd >= 0) { close(fd); return 1; }
    return 0;
}

char *bb_pathcat(const char *a, const char *b)
{
    int la = strlen(a), lb = strlen(b);
    int slash = (la > 0 && a[la - 1] != '/') ? 1 : 0;
    char *r = malloc(la + slash + lb + 1);
    if (!r) return 0;
    memcpy(r, a, la);
    if (slash) r[la] = '/';
    strcpy(r + la + slash, b);
    return r;
}

int bb_read_all(const char *path, char **data, int *len)
{
    int fd = open(path, 0, 0);          /* O_RDONLY */
    if (fd < 0) return -1;
    int cap = 1024, n = 0;
    char *buf = malloc(cap);
    if (!buf) { close(fd); return -2; }
    for (;;) {
        int r = read(fd, buf + n, cap - n);
        if (r <= 0) break;
        n += r;
        if (n == cap) {
            char *nb = malloc(cap * 2);
            if (!nb) { free(buf); close(fd); return -2; }
            memcpy(nb, buf, n);
            free(buf);
            buf = nb; cap *= 2;
        }
    }
    close(fd);
    *data = buf;
    *len = n;
    return 0;
}

int bb_filesize(const char *path)
{
    char st[108];
    if (bb_stat(path, st) < 0) return 0;
    return (unsigned char)st[44] | ((unsigned char)st[45] << 8) |
           ((unsigned char)st[46] << 16) | ((unsigned char)st[47] << 24);
}

int bb_read_fd(int fd, char **data, int *len)
{
    int cap = 0;
    char *buf = 0;
    *len = 0;
    for (;;) {
        int got;
        if (*len == cap) {
            int ncap = cap ? cap * 2 : 1024;
            char *nb = malloc(ncap);
            if (!nb) { if (buf) free(buf); return -2; }
            if (cap) { memcpy(nb, buf, cap); free(buf); }
            buf = nb; cap = ncap;
        }
        got = read(fd, buf + *len, cap - *len);
        if (got <= 0) break;
        *len += got;
    }
    *data = buf;
    return 0;
}

static const char *bb_getenv(const char *name)
{
    int nl = strlen(name);
    int i;
    for (i = 0; g_envp && g_envp[i]; i++) {
        if (strncmp(g_envp[i], name, nl) == 0 && g_envp[i][nl] == '=') {
            return g_envp[i] + nl + 1;
        }
    }
    return 0;
}

/* ================= despacho ================= */

static const bb_applet_fn      a_echo   = app_echo;
static const bb_applet_fn      a_true   = app_true;
static const bb_applet_fn      a_false  = app_false;
static const bb_applet_fn      a_seq    = app_seq;
static const bb_applet_fn      a_clear  = app_clear;
static const bb_applet_fn      a_env    = app_env;
static const bb_applet_fn      a_print  = app_printf;
static const bb_applet_fn      a_base   = app_basename;
static const bb_applet_fn      a_dirn   = app_dirname;
static const bb_applet_fn      a_id     = app_id;
static const bb_applet_fn      a_whoami = app_whoami;
static const bb_applet_fn      a_uname  = app_uname;

static const bb_applet_fn      a_cat   = app_cat;
static const bb_applet_fn      a_cp    = app_cp;
static const bb_applet_fn      a_mv    = app_mv;
static const bb_applet_fn      a_rm    = app_rm;
static const bb_applet_fn      a_rmdir = app_rmdir;
static const bb_applet_fn      a_mkdir = app_mkdir;
static const bb_applet_fn      a_touch = app_touch;
static const bb_applet_fn      a_cmp   = app_cmp;
static const bb_applet_fn      a_wc    = app_wc;
static const bb_applet_fn      a_head  = app_head;
static const bb_applet_fn      a_tail  = app_tail;
static const bb_applet_fn      a_sort  = app_sort;
static const bb_applet_fn      a_rev   = app_rev;
static const bb_applet_fn      a_tr    = app_tr;
static const bb_applet_fn      a_du    = app_du;
static const bb_applet_fn      a_ls    = app_ls;

static const bb_applet_fn      a_b64  = app_base64;
static const bb_applet_fn      a_md5  = app_md5sum;
static const bb_applet_fn      a_sum  = app_sum;

static const bb_applet_fn      a_date = app_date;
static const bb_applet_fn      a_file = app_file;

static const struct { const char *name; bb_applet_fn fn; } applets[] = {
    { "basename", a_base }, { "base64",  a_b64 },    { "cat", a_cat },
    { "clear",   a_clear }, { "cmp",     a_cmp },    { "cp", a_cp },
    { "date",    a_date },  { "dirname", a_dirn },   { "du", a_du },
    { "echo",    a_echo },  { "env",     a_env },    { "false", a_false },
    { "file",    a_file },  { "head",    a_head },  { "id",      a_id },     { "ls", a_ls },
    { "md5sum",  a_md5 },   { "mkdir",   a_mkdir },  { "mv", a_mv },
    { "printf",  a_print }, { "rev",     a_rev },    { "rm", a_rm },
    { "rmdir",   a_rmdir }, { "seq",     a_seq },    { "sort", a_sort },
    { "sum",     a_sum },   { "tail",    a_tail },   { "touch", a_touch },
    { "tr",      a_tr },    { "true",    a_true },   { "uname", a_uname },
    { "wc",      a_wc },    { "whoami",  a_whoami }
};
#define N_APPLETS ((int)(sizeof(applets) / sizeof(applets[0])))

static void bb_list_applets(void)
{
    int i;
    for (i = 0; i < N_APPLETS; i++) { bb_out(applets[i].name); bb_putc('\n'); }
}

int main(int argc, char **argv, char **envp)
{
    const char *called;
    int i;

    g_envp = envp;
    called = (argc > 0 && argv[0] && argv[0][0]) ? bb_basename(argv[0]) : "busybox";

    for (i = 0; i < N_APPLETS; i++) {
        if (strcmp(applets[i].name, called) == 0) return applets[i].fn(argc, argv);
    }

    if (argc >= 2 && strcmp(called, "busybox") == 0) {
        if (strcmp(argv[1], "--list") == 0) { bb_list_applets(); return 0; }
        if (strcmp(argv[1], "--help") == 0 || strcmp(argv[1], "-h") == 0) {
            bb_out("busybox 0.1 for OpenTTY - Multi-call binary\n");
            bb_out("uso: busybox <applet> [args...]\napplets:\n");
            bb_list_applets();
            return 0;
        }
        for (i = 0; i < N_APPLETS; i++) {
            if (strcmp(applets[i].name, argv[1]) == 0) return applets[i].fn(argc - 1, argv + 1);
        }
        bb_err("busybox: applet not found: ");
        bb_err(argv[1]);
        bb_putc('\n');
        return 1;
    }

    /* invocado sem nome de applet: lista o que sabe fazer */
    bb_out("busybox for OpenTTY: applets:\n");
    bb_list_applets();
    return 0;
}

/* ================= applets simples ================= */

int app_true(int argc, char **argv)  { return 0; }
int app_false(int argc, char **argv) { return 1; }

int app_clear(int argc, char **argv)
{
    static const char esc[] = "\033[2J\033[H";
    write(1, esc, (int)(sizeof(esc) - 1));
    return 0;
}

int app_echo(int argc, char **argv)
{
    int n = 0, i = 1;
    char nl = '\n';
    if (i < argc && strcmp(argv[i], "-n") == 0) { nl = 0; i++; }
    for (; i < argc; i++) { if (n) bb_putc(' '); bb_out(argv[i]); n = 1; }
    if (nl) bb_putc('\n');
    return 0;
}

int app_seq(int argc, char **argv)
{
    int first = 1, incr = 1, last, i;
    if (argc < 2 || argc > 4) { bb_err("seq: uso: seq [FIRST [INCR]] LAST\n"); return 1; }
    if (argc == 2)      last = atoi(argv[1]);
    else if (argc == 3) { first = atoi(argv[1]); last = atoi(argv[2]); }
    else                { first = atoi(argv[1]); incr = atoi(argv[2]); last = atoi(argv[3]); }
    if (incr == 0) return 1;
    i = first;
    if (incr > 0) { while (i <= last) { printf("%d\n", i); i += incr; } }
    else          { while (i >= last) { printf("%d\n", i); i += incr; } }
    return 0;
}

int app_env(int argc, char **argv)
{
    int i;
    for (i = 0; g_envp && g_envp[i]; i++) { bb_out(g_envp[i]); bb_putc('\n'); }
    return 0;
}

int app_basename(int argc, char **argv)
{
    if (argc < 2) return 1;
    bb_out(bb_basename(argv[1]));
    bb_putc('\n');
    return 0;
}

int app_dirname(int argc, char **argv)
{
    char *p, *last = 0, *q;
    int len;
    if (argc < 2) return 1;
    p = argv[1];
    for (q = p; *q; q++) if (*q == '/') last = q;
    if (!last) { bb_out(".\n"); return 0; }
    len = last - p;
    if (len == 0) { bb_out("/\n"); return 0; }
    while (len > 0 && p[len - 1] == '/') len--;
    if (len == 0) { bb_out("/\n"); return 0; }
    {
        char *d = malloc(len + 1);
        if (!d) return 1;
        memcpy(d, p, len);
        d[len] = 0;
        bb_out(d);
        bb_putc('\n');
        free(d);
    }
    return 0;
}

int app_id(int argc, char **argv)
{
    int u = bb_getuid();
    printf("uid=%d gid=%d\n", u, u);
    return 0;
}

int app_whoami(int argc, char **argv)
{
    const char *u = bb_getenv("USER");
    if (u && u[0]) { bb_out(u); bb_putc('\n'); }
    else { printf("%d\n", bb_getuid()); }
    return 0;
}

int app_uname(int argc, char **argv)
{
    char b[6 * 65];
    struct field { int off; char letter; };
    static const struct field fields[5] = {
        { 0, 's' }, { 65, 'n' }, { 130, 'r' }, { 195, 'v' }, { 260, 'm' }
    };
    int all = 0, nw = 0, i = 1, k, printed = 0, first = 1;
    char want[8]; want[0] = 0;
    if (bb_uname(b) < 0) { bb_err("uname: falhou\n"); return 1; }
    while (i < argc && argv[i][0] == '-' && argv[i][1] && argv[i][1] != '-') {
        const char *p = argv[i] + 1;
        while (*p) {
            if (*p == 'a') all = 1;
            else if (nw < 7) want[nw++] = *p;
            p++;
        }
        i++;
    }
    want[nw] = 0;
    for (k = 0; k < 5; k++) {
        int on = all, j;
        if (!on) { for (j = 0; j < nw; j++) if (want[j] == fields[k].letter) { on = 1; break; } }
        if (on) {
            if (!first) bb_putc(' ');
            bb_out(b + fields[k].off);
            first = 0; printed = 1;
        }
    }
    if (!printed) bb_out(b + fields[0].off);
    bb_putc('\n');
    return 0;
}

/* ---- printf: conversao de tipos feita em C (nao delega varargs) ---- */

static unsigned bb_strtou(const char *s, int base)
{
    unsigned r = 0;
    int neg = 0;
    while (*s == ' ' || *s == '\t') s++;
    if (base == 10 && (*s == '-' || *s == '+')) { neg = (*s == '-'); s++; }
    while (*s) {
        int v;
        if (*s >= '0' && *s <= '9') v = *s - '0';
        else if (*s >= 'a' && *s <= 'f') v = *s - 'a' + 10;
        else if (*s >= 'A' && *s <= 'F') v = *s - 'A' + 10;
        else break;
        if (v >= base) break;
        r = r * base + v;
        s++;
    }
    if (neg) r = (unsigned)(0 - (int)r);
    return r;
}

int app_printf(int argc, char **argv)
{
    const char *fmt;
    int ai;
    if (argc < 2) { bb_err("printf: uso: printf FORMAT [ARG...]\n"); return 1; }
    fmt = argv[1];
    ai = 2;
    while (*fmt) {
        if (*fmt != '%') { bb_putc(*fmt++); continue; }
        fmt++;
        while (*fmt == '-' || *fmt == '0' || *fmt == '+' || *fmt == ' ' || *fmt == '#') fmt++;
        while (*fmt >= '0' && *fmt <= '9') fmt++;
        if (*fmt == '.') { fmt++; while (*fmt >= '0' && *fmt <= '9') fmt++; }
        if (*fmt == 'l' || *fmt == 'h') fmt++;
        if (*fmt == '%') { bb_putc('%'); fmt++; continue; }
        {
            const char *arg = (ai < argc) ? argv[ai++] : "";
            switch (*fmt) {
                case 's': bb_out(arg); break;
                case 'c': { char c = arg[0] ? arg[0] : 0; bb_putc(c); } break;
                case 'd':
                case 'i': { char t[16]; sprintf(t, "%d", (int)bb_strtou(arg, 10)); bb_out(t); } break;
                case 'u': { char t[16]; sprintf(t, "%u", bb_strtou(arg, 10)); bb_out(t); } break;
                case 'x': { char t[16]; sprintf(t, "%x", bb_strtou(arg, 10)); bb_out(t); } break;
                case 'X': { char t[16]; sprintf(t, "%X", bb_strtou(arg, 10)); bb_out(t); } break;
                case 'o': { char t[16]; sprintf(t, "%o", bb_strtou(arg, 10)); bb_out(t); } break;
                case 'p': { char t[16]; sprintf(t, "%x", bb_strtou(arg, 10)); bb_out(t); } break;
                default:  bb_putc('%'); if (*fmt) bb_putc(*fmt); break;
            }
        }
        if (*fmt) fmt++;
    }
    return 0;
}