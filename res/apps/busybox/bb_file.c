/* bb_file.c - applets de arquivos do multi-tool busybox (estilo busybox).
 * Usa apenas open/read/write/close (libc.s) + syscalls crus (bb_sys.s)
 * getdents/stat/unlink/mkdir/rmdir.
 */
#include "busybox.h"

/* ================= helpers ================= */

static int copy_file(const char *src, const char *dst)
{
    char *buf = 0;
    int len = 0, fd, rc = 0;
    if (bb_read_all(src, &buf, &len)) return -1;      /* fonte nao abriu */
    fd = open(dst, 1 | 64 | 512, 0644);               /* O_WRONLY|O_CREAT|O_TRUNC */
    if (fd < 0) { if (buf) free(buf); return -2; }    /* destino nao abriu */
    if (len > 0 && write(fd, buf, len) < 0) rc = -3;
    close(fd);                                        /* close flusha no FS */
    if (buf) free(buf);
    return rc;
}

static int cp_mv(int argc, char **argv, int move)
{
    int i, status = 0;
    const char *dest;
    int dest_dir;
    if (argc < 3) {
        bb_err(move ? "mv: uso: mv SRC... DEST\n" : "cp: uso: cp SRC... DEST\n");
        return 1;
    }
    dest = argv[argc - 1];
    dest_dir = bb_is_dir(dest);
    if (argc > 3 && !dest_dir) {
        bb_err(dest); bb_err(": nao e um diretorio\n");
        return 1;
    }
    for (i = 1; i < argc - 1; i++) {
        char *dpath = dest_dir ? bb_pathcat(dest, bb_basename(argv[i]))
                               : strdup(dest);
        if (!dpath) { status = 1; continue; }
        if (copy_file(argv[i], dpath) < 0) {
            bb_err(move ? "mv: " : "cp: ");
            bb_err(argv[i]); bb_err(": falhou ao copiar para "); bb_err(dpath); bb_putc('\n');
            status = 1;
        } else if (move && bb_unlink(argv[i]) < 0) {
            bb_err("mv: "); bb_err(argv[i]); bb_err(": falhou ao remover a origem\n");
        }
        free(dpath);
    }
    return status;
}

int app_cp(int argc, char **argv) { return cp_mv(argc, argv, 0); }
int app_mv(int argc, char **argv) { return cp_mv(argc, argv, 1); }

/* ================= cat / touch / cmp ================= */

int app_cat(int argc, char **argv)
{
    int i, status = 0;
    for (i = 1; i < argc; i++) {
        char b[512];
        int n, fd = open(argv[i], 0, 0);
        if (fd < 0) {
            bb_err("cat: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            status = 1;
            continue;
        }
        while ((n = read(fd, b, sizeof(b))) > 0) write(1, b, n);
        close(fd);
    }
    return status;
}

int app_touch(int argc, char **argv)
{
    int i, status = 0;
    for (i = 1; i < argc; i++) {
        int fd = open(argv[i], 0, 0);
        if (fd >= 0) { close(fd); continue; }         /* ja existe */
        fd = open(argv[i], 1 | 64, 0644);             /* O_WRONLY|O_CREAT */
        if (fd >= 0) close(fd);
        else { bb_err("touch: "); bb_err(argv[i]); bb_err(": cannot create\n"); status = 1; }
    }
    return status;
}

int app_cmp(int argc, char **argv)
{
    char *b1 = 0, *b2 = 0;
    int l1, l2, common, i;
    if (argc < 3) { bb_err("cmp: uso: cmp FILE1 FILE2\n"); return 1; }
    if (bb_read_all(argv[1], &b1, &l1)) {
        bb_err("cmp: "); bb_err(argv[1]); bb_err(": No such file\n"); return 2;
    }
    if (bb_read_all(argv[2], &b2, &l2)) {
        if (b1) free(b1);
        bb_err("cmp: "); bb_err(argv[2]); bb_err(": No such file\n"); return 2;
    }
    common = l1 < l2 ? l1 : l2;
    for (i = 0; i < common; i++) {
        if (b1[i] != b2[i]) {
            printf("%s %s differ: byte %d\n", argv[1], argv[2], i + 1);
            free(b1); free(b2); return 1;
        }
    }
    if (l1 != l2) {
        printf("%s %s differ: byte %d of %d (EOF em %s)\n", argv[1], argv[2],
               common + 1, l1 < l2 ? l2 : l1, l1 < l2 ? argv[1] : argv[2]);
        free(b1); free(b2); return 1;
    }
    free(b1); free(b2);
    return 0;
}

/* ================= rm / rmdir / mkdir ================= */

static int rm_dirent(const char *path, int recurse)
{
    if (recurse && bb_is_dir(path)) {
        char d[2048];
        int n, off = 0, fd = open(path, 0x10000, 0);
        if (fd < 0) return -1;
        n = bb_getdents(fd, d, sizeof(d));
        close(fd);
        while (off + 10 <= n) {
            int reclen = (unsigned char)d[off+8] | ((unsigned char)d[off+9] << 8);
            char *name = d + off + 10;
            if (reclen < 10 || off + reclen > n) break;
            if (name[0] && strcmp(name, ".") && strcmp(name, "..")) {
                char *full = bb_pathcat(path, name);
                if (full) { rm_dirent(full, recurse); free(full); }
            }
            off += reclen;
        }
        if (bb_rmdir(path) < 0) return -1;
        return 0;
    }
    return bb_unlink(path);
}

int app_rm(int argc, char **argv)
{
    int rec = 0, i = 1, status = 0;
    while (i < argc && argv[i][0] == '-' && strcmp(argv[i], "--") != 0) {
        const char *p = argv[i] + 1;
        while (*p) { if (*p == 'r' || *p == 'R') rec = 1; p++; }
        i++;
    }
    for (; i < argc; i++) {
        if (rm_dirent(argv[i], rec) < 0) {
            bb_err("rm: "); bb_err(argv[i]); bb_err(": cannot remove\n");
            status = 1;
        }
    }
    return status;
}

int app_rmdir(int argc, char **argv)
{
    int i, status = 0;
    for (i = 1; i < argc; i++) {
        if (bb_rmdir(argv[i]) < 0) {
            bb_err("rmdir: "); bb_err(argv[i]); bb_err(": failed\n");
            status = 1;
        }
    }
    return status;
}

int app_mkdir(int argc, char **argv)
{
    int i = 1, status = 0;
    while (i < argc && argv[i][0] == '-' && argv[i][1]) i++;  /* ignora -p */
    for (; i < argc; i++) {
        if (bb_mkdir(argv[i], 0777) < 0) {
            bb_err("mkdir: "); bb_err(argv[i]); bb_err(": failed\n");
            status = 1;
        }
    }
    return status;
}

/* ================= wc / head / tail ================= */

static void wc_file(const char *path, int *lines, int *words, int *bytes)
{
    int fd = open(path, 0, 0), inw = 0;
    char b[512];
    *lines = *words = *bytes = 0;
    if (fd < 0) { *lines = -1; return; }
    for (;;) {
        int n = read(fd, b, sizeof(b));
        int k;
        if (n <= 0) break;
        *bytes += n;
        for (k = 0; k < n; k++) {
            char ch = b[k];
            if (ch == '\n') (*lines)++;
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\v') {
                if (inw) (*words)++;
                inw = 0;
            } else inw = 1;
        }
    }
    if (inw) (*words)++;
    close(fd);
}

static void wc_print(const char *name, int l, int w, int c, int showl, int showw, int showc)
{
    char nb[48]; int p = 0;
    if (showl) { sprintf(nb + p, "%d ", l); while (nb[p]) p++; }
    if (showw) { sprintf(nb + p, "%d ", w); while (nb[p]) p++; }
    if (showc) { sprintf(nb + p, "%d ", c); while (nb[p]) p++; }
    nb[p] = 0;
    bb_out(nb);
    if (name) bb_out(name);
    bb_putc('\n');
}

int app_wc(int argc, char **argv)
{
    int showl = 0, showw = 0, showc = 0, any = 0;
    int tl = 0, tw = 0, tc = 0, files = 0;
    int i = 1;
    while (i < argc && argv[i][0] == '-' && argv[i][1] && argv[i][1] != '-') {
        const char *p = argv[i] + 1;
        while (*p) {
            if (*p == 'l') { showl = 1; any = 1; }
            else if (*p == 'w') { showw = 1; any = 1; }
            else if (*p == 'c' || *p == 'm') { showc = 1; any = 1; }
            p++;
        }
        i++;
    }
    if (!any) showl = showw = showc = 1;
    if (i >= argc) {
        char *buf = 0;
        int len = 0, l = 0, w = 0, c = 0, k, inw = 0;
        if (bb_read_fd(0, &buf, &len) != 0) return 1;
        for (k = 0; buf && k < len; k++) {
            char ch = buf[k];
            if (ch == '\n') l++;
            if (ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r' || ch == '\v') {
                if (inw) w++;
                inw = 0;
            } else inw = 1;
        }
        if (inw) w++;
        c = len;
        wc_print(0, l, w, c, showl, showw, showc);
        if (buf) free(buf);
        return 0;
    }
    for (; i < argc; i++) {
        int l, w, c;
        wc_file(argv[i], &l, &w, &c);
        if (l < 0) {
            bb_err("wc: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            continue;
        }
        files++;
        tl += l; tw += w; tc += c;
        wc_print(argv[i], l, w, c, showl, showw, showc);
    }
    if (files > 1) { wc_print("total", tl, tw, tc, showl, showw, showc); }
    return 0;
}

/* -n NUM ou -NUM (primeira opcao apenas); retorna o indice do 1o arquivo */
static int head_num(int argc, char **argv, int *n)
{
    int i = 1;
    *n = 10;
    if (i < argc && argv[i][0] == '-' && argv[i][1]) {
        if (strcmp(argv[i], "-n") == 0) {
            if (i + 1 < argc) { *n = atoi(argv[i + 1]); return 3; }
            return 1;
        }
        if (argv[i][1] >= '0' && argv[i][1] <= '9') {
            *n = atoi(argv[i] + 1);
            return 2;
        }
    }
    return 1;
}

int app_head(int argc, char **argv)
{
    int n, i = head_num(argc, argv, &n);
    if (i >= argc) {
        char *buf = 0;
        int len = 0, left, k;
        if (bb_read_fd(0, &buf, &len) != 0) return 1;
        if (buf) {
            left = n;
            for (k = 0; k < len && left > 0; k++) {
                if (buf[k] == '\n') left--;
                bb_putc(buf[k]);
            }
            free(buf);
        }
        return 0;
    }
    for (; i < argc; i++) {
        char *buf = 0;
        int len = 0, left, k;
        if (bb_read_all(argv[i], &buf, &len)) {
            bb_err("head: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            continue;
        }
        left = n;
        for (k = 0; k < len && left > 0; k++) {
            if (buf[k] == '\n') left--;
            bb_putc(buf[k]);
        }
        if (buf) free(buf);
    }
    return 0;
}

int app_tail(int argc, char **argv)
{
    int n, i = head_num(argc, argv, &n);
    if (i >= argc) {
        char *buf = 0;
        int len = 0, nl = 0, toskip, passed = 0, start = 0, k;
        if (bb_read_fd(0, &buf, &len) != 0) return 1;
        if (!buf) return 0;
        for (k = 0; k < len; k++) if (buf[k] == '\n') nl++;
        toskip = nl - n;
        if (toskip <= 0) start = 0;
        else {
            passed = 0;
            for (k = 0; k < len; k++) {
                if (buf[k] == '\n') {
                    passed++;
                    if (passed == toskip) { start = k + 1; break; }
                }
            }
        }
        if (start < len) write(1, buf + start, len - start);
        free(buf);
        return 0;
    }
    for (; i < argc; i++) {
        char *buf = 0;
        int len, nl = 0, toskip, passed = 0, start = 0, k;
        if (bb_read_all(argv[i], &buf, &len)) {
            bb_err("tail: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            continue;
        }
        for (k = 0; k < len; k++) if (buf[k] == '\n') nl++;
        toskip = nl - n;
        if (toskip <= 0) start = 0;
        else {
            passed = 0;
            for (k = 0; k < len; k++) {
                if (buf[k] == '\n') {
                    passed++;
                    if (passed == toskip) { start = k + 1; break; }
                }
            }
        }
        if (start < len) write(1, buf + start, len - start);
        if (buf) free(buf);
    }
    return 0;
}

/* ================= sort / rev / tr ================= */

/* anexa a linhas[0..count-1] as linhas de buf[0..len-1]; 0=ok */
static int sort_add_lines(char ***p_lines, int *p_count, int *p_cap,
                          const char *buf, int len)
{
    char **lines = *p_lines;
    int count = *p_count, cap = *p_cap;
    int k, start = 0;
    for (k = 0; k <= len; k++) {
        if (k == len || buf[k] == '\n') {
            int sl = k - start;
            char *line;
            if (sl > 0) {
                if (count == cap) {
                    char **nl = malloc((cap ? cap * 2 : 16) * sizeof(char *));
                    if (!nl) return -1;
                    memcpy(nl, lines, count * sizeof(char *));
                    if (lines) free(lines);
                    lines = nl;
                    cap = cap ? cap * 2 : 16;
                }
                line = malloc(sl + 1);
                if (!line) return -1;
                memcpy(line, buf + start, sl);
                line[sl] = 0;
                lines[count++] = line;
            }
            start = k + 1;
        }
    }
    *p_lines = lines;
    *p_count = count;
    *p_cap = cap;
    return 0;
}

int app_sort(int argc, char **argv)
{
    int rev = 0, i = 1;
    int cap = 0, count = 0;
    char **lines = 0;
    if (i < argc && strcmp(argv[i], "-r") == 0) { rev = 1; i++; }
    if (i >= argc) {
        char *buf = 0;
        int len = 0;
        if (bb_read_fd(0, &buf, &len) != 0) return 1;
        sort_add_lines(&lines, &count, &cap, buf, len);
        if (buf) free(buf);
    }
    for (; i < argc; i++) {
        char *buf = 0;
        int len = 0;
        if (bb_read_all(argv[i], &buf, &len)) {
            bb_err("sort: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            return 1;
        }
        sort_add_lines(&lines, &count, &cap, buf, len);
        if (buf) free(buf);
    }
    for (i = 1; i < count; i++) {
        int j = i;
        while (j > 0) {
            int c = strcmp(lines[j - 1], lines[j]);
            if (rev ? (c < 0) : (c > 0)) {
                char *t = lines[j - 1]; lines[j - 1] = lines[j]; lines[j] = t;
                j--;
            } else break;
        }
    }
    for (i = 0; i < count; i++) {
        bb_out(lines[i]); bb_putc('\n'); free(lines[i]);
    }
    if (lines) free(lines);
    return 0;
}

int app_rev(int argc, char **argv)
{
    int i = 1;
    if (i >= argc) {
        char *buf = 0;
        int len = 0;
        if (bb_read_fd(0, &buf, &len) != 0) return 1;
        if (buf) {
            int s = 0, e = -1;
            while (s <= len) {
                if (s == len || buf[s] == '\n') {
                    int k;
                    for (k = s - 1; k > e; k--) bb_putc(buf[k]);
                    if (s < len) bb_putc('\n');
                    e = s;
                }
                s++;
            }
            free(buf);
        }
        return 0;
    }
    for (; i < argc; i++) {
        char *buf = 0;
        int len = 0, e, s;
        if (bb_read_all(argv[i], &buf, &len)) {
            bb_err("rev: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            continue;
        }
        /* inverte cada linha (separador final '\n' descartado) */
        s = 0; e = -1;
        while (s <= len) {
            if (s == len || buf[s] == '\n') {
                int k;
                for (k = s - 1; k > e; k--) bb_putc(buf[k]);
                if (s < len) bb_putc('\n');
                e = s;
            }
            s++;
        }
        if (buf) free(buf);
    }
    return 0;
}

int app_tr(int argc, char **argv)
{
    const char *s1, *s2;
    int l1, l2, k;
    unsigned char map[256];
    char *buf = 0;
    int len = 0;
    if (argc < 3) { bb_err("tr: uso: tr SET1 SET2 [FILE]\n"); return 1; }
    s1 = argv[1]; s2 = argv[2];
    l1 = strlen(s1); l2 = strlen(s2);
    if (l1 == 0 || l2 == 0) { bb_err("tr: conjunto vazio\n"); return 2; }
    for (k = 0; k < 256; k++) map[k] = (unsigned char)k;
    for (k = 0; k < l1; k++) map[(unsigned char)s1[k]] = (unsigned char)s2[k < l2 ? k : l2 - 1];
    if (argc >= 4) {
        if (bb_read_all(argv[3], &buf, &len)) {
            bb_err("tr: "); bb_err(argv[3]); bb_err(": No such file or directory\n");
            return 1;
        }
    } else if (bb_read_fd(0, &buf, &len) != 0) {
        bb_err("tr: falha ao ler stdin\n");
        return 1;
    }
    for (k = 0; k < len; k++) bb_putc((char)map[(unsigned char)buf[k]]);
    if (buf) free(buf);
    return 0;
}

/* ================= du ================= */

static void du_dir(const char *path, int depth, int *total)
{
    char d[2048];
    int fd = open(path, 0x10000, 0);
    int n, off = 0;
    if (fd < 0) { *total += bb_filesize(path); return; }   /* arquivo */
    n = bb_getdents(fd, d, sizeof(d));
    close(fd);
    while (off + 10 <= n) {
        int reclen = (unsigned char)d[off+8] | ((unsigned char)d[off+9] << 8);
        char *name = d + off + 10;
        if (reclen < 10 || off + reclen > n) break;
        if (name[0] && strcmp(name, ".") && strcmp(name, "..")) {
            char *full = bb_pathcat(path, name);
            if (full) {
                if (depth < 16) du_dir(full, depth + 1, total);
                free(full);
            }
        }
        off += reclen;
    }
}

int app_du(int argc, char **argv)
{
    int i = 1;
    if (i >= argc) {
        int t = 0;
        du_dir(".", 0, &t);
        printf("%d .\n", t);
        return 0;
    }
    for (; i < argc; i++) {
        int t = 0;
        du_dir(argv[i], 0, &t);
        printf("%d %s\n", t, argv[i]);
    }
    return 0;
}

/* ================= ls ================= */

static void ls_one(const char *base, const char *name, int longf)
{
    if (longf) {
        char *full = bb_pathcat(base, name);
        int isd = full ? bb_is_dir(full) : 0;
        int sz = full ? bb_filesize(full) : 0;
        printf("%c %d %s\n", isd ? 'd' : '-', sz, name);
        if (full) free(full);
    } else {
        bb_out(name); bb_putc('\n');
    }
}

static void ls_dir(const char *path, int longf, int showdot)
{
    char d[4096];
    int n, off = 0, fd = open(path, 0x10000, 0);
    if (fd < 0) {
        bb_err("ls: "); bb_err(path); bb_err(": No such file or directory\n");
        return;
    }
    n = bb_getdents(fd, d, sizeof(d));
    close(fd);
    while (off + 10 <= n) {
        int reclen = (unsigned char)d[off+8] | ((unsigned char)d[off+9] << 8);
        char *name = d + off + 10;
        if (reclen < 10 || off + reclen > n) break;
        if (!showdot && name[0] == '.') { off += reclen; continue; }
        ls_one(path, name, longf);
        off += reclen;
    }
}

int app_ls(int argc, char **argv)
{
    int longf = 0, showdot = 0, i = 1;
    while (i < argc && argv[i][0] == '-' && argv[i][1] && strcmp(argv[i], "--") != 0) {
        const char *p = argv[i] + 1;
        while (*p) {
            if (*p == 'l') longf = 1;
            else if (*p == 'a') showdot = 1;
            p++;
        }
        i++;
    }
    if (i >= argc) ls_dir(".", longf, showdot);
    else for (; i < argc; i++) ls_dir(argv[i], longf, showdot);
    return 0;
}