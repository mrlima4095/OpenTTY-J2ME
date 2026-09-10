/* libc-demo.c - Demo da stdlib do emulador ELF ARM32 do OpenTTY.
 *
 * Compilar/linkar:
 *   ./build-elf.sh libc-demo.c -stdlib
 *
 * A stdlib nao e' newlib: cada funcao vira um svc #LIB_* (res/lib/libc.s)
 * implementado no emulador (src/ELF.java, handleLibraryCall). So declaramos
 * prototipos aqui; os corpos estao no emulador.
 */
int printf(const char *fmt, ...);
int sprintf(char *buf, const char *fmt, ...);
int snprintf(char *buf, int n, const char *fmt, ...);
void *malloc(unsigned int n);
void *calloc(unsigned int nmemb, unsigned int size);
void *realloc(void *p, unsigned int n);
void free(void *p);
char *strcpy(char *d, const char *s);
char *strcat(char *d, const char *s);
int strlen(const char *s);
int strcmp(const char *a, const char *b);
int strncmp(const char *a, const char *b, unsigned int n);
char *strchr(const char *s, int c);
char *strdup(const char *s);
int atoi(const char *s);
int abs(int v);
void *memcpy(void *d, const void *s, unsigned int n);
void *memmove(void *d, const void *s, unsigned int n);
void *memset(void *d, int c, unsigned int n);
int memcmp(const void *a, const void *b, unsigned int n);
void *memchr(const void *s, int c, unsigned int n);
int toupper(int c);
int tolower(int c);
int getpid(void);
void exit(int status);

int main(int argc, char **argv) {
    printf("pid=%d\n", getpid());

    /* formatadores */
    printf("d=%d i=%i u=%u x=%x X=%X o=%o c=%c s=%s pct=%%\n",
           -42, -42, 4000000000U, 0xDEADBEEF, 0xDEADBEEF, 4000000000U,
           67, "stdlib", (int)-2147483648);
    printf("min=%d\n", (int)-2147483648);

    /* sprintf e snprintf (truncacao) */
    char buf[64];
    sprintf(buf, "[%d|%s|%x]", 12, "certo", 0xAB);
    printf("sprintf  [%s]\n", buf);
    snprintf(buf, 5, "12345-trunc");
    printf("snprintf [%s]\n", buf);

    /* numerico */
    printf("atoi(%s)=%d abs(%d)=%d\n", "-987", atoi("-987"), -123, abs(-123));

    /* heap: malloc + strings */
    char *p = malloc(100);
    strcpy(p, "heap 1");
    strcat(p, " + strcat");
    p = realloc(p, 256);
    printf("heap [%s] len=%d strlen(strcat)=%d cmp=%d\n", p, strlen(p),
           strlen(strcat(p, "")), strcmp(p, "heap 1 + strcat"));

    /* calloc zera */
    char *z = calloc(1, 50);
    int zi;
    for (zi = 0; zi < 20; zi++) { if (z[zi] != 0) { break; } }
    printf("calloc zeroed=%d\n", zi == 20 ? 1 : 0);

    /* strdup / strchr */
    char *q = strdup(p);
    printf("strdup [%s] strncmp=%d\n", q, strncmp(q, "heap 1 + str", 12));
    printf("strchr %s\n", strchr(q, 's'));

    /* memoria */
    char m[12];
    memset(m, '#', 8);
    memcpy(m + 8, "XY", 2);
    m[10] = 0;
    memmove(m + 1, m, 6);
    printf("mem [%s] mc=%d memchr=%d\n", m, memcmp(m, "######XY", 8),
           memchr(m, '#', 10) != 0 ? 1 : 0);

    /* case */
    printf("case %c%c%c\n", toupper('a'), tolower('Z'), toupper('x'));

    /* divisao 32-bit (usando argc para forcar o caminho de divisao) */
    unsigned int ux = 0xFFFFFFF5U + (unsigned int) argc;
    unsigned int uq = ux / 7u, ur = ux % 7u;
    int sx = -123456 + argc;
    int sq = sx / 7, sr = sx % 7;
    printf("udiv ux=%u q=%u r=%u | sdiv sx=%d q=%d r=%d\n", ux, uq, ur, sx, sq, sr);

    /* long long (__udivmoddi4/__divmoddi4, pares alinhados) */
    long long a64 = (long long) 1000000 * 49000;
    long long q64 = a64 / 7ll;
    long long r64 = a64 % 7ll;
    printf("ldiv a64=%lld q64=%lld r64=%lld\n", a64, q64, r64);
    printf("udiv64 mix=%d %u %lld\n", -7, 4000000000U, q64);

    /* argc/argv vem do CRT (stack do emulador) */
    printf("argc=%d argv0=[%s] argv1=[%s]\n", argc, argv[0], argc > 1 ? argv[1] : "(none)");

    /* ponteiro */
    printf("ptr=%p\n", (void *) p);

    free(p); free(q); free(z);
    exit(0);
    return 0;
}
