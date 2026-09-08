/* bb_filetype.c - applet file: detecta o tipo de um arquivo pelos bytes de
 * cabecalho (magic). Funciona com a libc do emulador (sem floats/long long).
 */
#include "busybox.h"

static int is_ascii_text(const unsigned char *b, int n)
{
    int i;
    if (n >= 3 && b[0] == 0xEF && b[1] == 0xBB && b[2] == 0xBF) return 1; /* UTF-8 BOM */
    for (i = 0; i < n; i++) {
        unsigned char c = b[i];
        if (c == 0) return 0;
        if (c < 32 && c != '\t' && c != '\n' && c != '\r' && c != '\f' && c != 27) return 0;
        if (c == 0x7F) return 0;
    }
    return 1;
}

/* preenche out com a descricao do tipo; out deve ter espaco suficiente */
static void file_type(const unsigned char *b, int n, char *out)
{
    if (n <= 0) { strcpy(out, "empty"); return; }

    /* ELF: 7f E L F */
    if (n >= 4 && b[0] == 0x7F && b[1] == 'E' && b[2] == 'L' && b[3] == 'F') {
        int etype = b[16] | (b[17] << 8);
        int mach  = b[18] | (b[19] << 8);
        const char *t;
        if (etype == 1)      t = "ELF relocatable";
        else if (etype == 2) t = "ELF executable";
        else if (etype == 3) t = "ELF shared object";
        else                 t = "ELF object";
        strcpy(out, t);
        if (mach == 40) strcat(out, " (ARM)");
        return;
    }
    /* shebang */
    if (n >= 2 && b[0] == '#' && b[1] == '!') {
        int i;
        strcpy(out, "script: ");
        for (i = 2; i < n && i < 40 && b[i] != '\n'; i++) ;
        if (i > 2) { memcpy(out + 8, b + 2, i - 2); out[8 + (i - 2)] = 0; }
        return;
    }
    /* PNG: 89 P N G \r \n 1a \n */
    if (n >= 8 && b[0] == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G' &&
        b[4] == '\r' && b[5] == '\n' && b[6] == 0x1A && b[7] == '\n') {
        strcpy(out, "PNG image"); return;
    }
    if (n >= 3 && b[0] == 0xFF && b[1] == 0xD8 && b[2] == 0xFF) {
        strcpy(out, "JPEG image"); return;
    }
    if (n >= 4 && b[0] == 'G' && b[1] == 'I' && b[2] == 'F' && b[3] == '8') {
        strcpy(out, "GIF image"); return;
    }
    if (n >= 2 && b[0] == 0x1F && b[1] == 0x8B) {
        strcpy(out, "gzip compressed data"); return;
    }
    if (n >= 4 && b[0] == 'P' && b[1] == 'K' &&
        ((b[2] == 3 && b[3] == 4) || (b[2] == 5 && b[3] == 6))) {
        strcpy(out, "ZIP archive"); return;
    }
    if (n >= 5 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F' && b[4] == '-') {
        strcpy(out, "PDF document"); return;
    }
    if (n >= 262 && b[257] == 'u' && b[258] == 's' && b[259] == 't' &&
        b[260] == 'a' && b[261] == 'r') {
        strcpy(out, "tar archive"); return;
    }
    if (n >= 2 && b[0] == 'B' && b[1] == 'M') {
        strcpy(out, "BMP image"); return;
    }
    if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F' &&
        b[8] == 'W' && b[9] == 'A' && b[10] == 'V' && b[11] == 'E') {
        strcpy(out, "WAV audio"); return;
    }
    if (is_ascii_text(b, n)) { strcpy(out, "ASCII text"); return; }
    strcpy(out, "data");
}

int app_file(int argc, char **argv)
{
    int i = 1;
    while (i < argc && argv[i][0] == '-' && strcmp(argv[i], "-") != 0) {
        if (strcmp(argv[i], "--help") == 0 || strcmp(argv[i], "-h") == 0) {
            bb_out("file: uso: file ARQUIVO...\n");
            return 0;
        }
        i++;
    }
    if (i >= argc) { bb_err("file: uso: file ARQUIVO...\n"); return 1; }
    for (; i < argc; i++) {
        char b[512];
        int fd = open(argv[i], 0, 0), n = 0, r;
        char out[96];
        if (fd < 0) {
            bb_err("file: "); bb_err(argv[i]); bb_err(": No such file or directory\n");
            continue;
        }
        while (n < (int)sizeof(b) && (r = read(fd, b + n, sizeof(b) - n)) > 0)
            n += r;
        close(fd);
        file_type((const unsigned char *)b, n, out);
        bb_out(argv[i]); bb_out(": "); bb_out(out); bb_putc('\n');
    }
    return 0;
}