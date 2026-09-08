/* base64.c - Encoda uma string em Base64 sem libc (loops + tabela).
 *
 * Uso: base64 [texto]    (default "OpenTTY")
 * Demonstra aritmetica de bytes, tabelas const char[] e ponteiros.
 * Compilar:
 *   ./build-elf.sh res/apps/src/base64.c -stdlib -o res/apps/dist/base64
 */
int printf(const char *fmt, ...);
int strlen(const char *s);
void exit(int status);

static const char tbl[] =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

void b64(char *out, const char *in, int len)
{
    int i = 0, o = 0;
    while (i < len) {
        unsigned int a = (unsigned char) in[i], b, c;
        if (i + 1 < len) { b = (unsigned char) in[i + 1]; } else { b = 0; }
        if (i + 2 < len) { c = (unsigned char) in[i + 2]; } else { c = 0; }

        out[o++] = tbl[a >> 2];
        out[o++] = tbl[((a & 3) << 4) | (b >> 4)];
        if (i + 1 < len) { out[o++] = tbl[((b & 15) << 2) | (c >> 6)]; } else { out[o++] = '='; }
        if (i + 2 < len) { out[o++] = tbl[c & 63]; } else { out[o++] = '='; }
        i += 3;
    }
    out[o] = 0;
}

int main(int argc, char **argv)
{
    char out[128];
    const char *s = (argc > 1) ? argv[1] : "OpenTTY";
    int len = strlen(s);

    if (len > 90) { len = 90; }
    b64(out, s, len);
    printf("\"%s\" (%d bytes) -> %s\n", s, len, out);

    exit(0);
    return 0;
}