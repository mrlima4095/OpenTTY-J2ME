/* str.c - Tour das funcoes de string/memoria da stdlib.
 *
 * Demonstra strlen/strcpy/strcat/strncat/strchr/strdup/strcmp/strncmp
 * e o heap (malloc/realloc/free).
 * Compilar:
 *   ./build-elf.sh res/apps/src/str.c -stdlib -o res/apps/dist/str
 */
int printf(const char *fmt, ...);
int sprintf(char *buf, const char *fmt, ...);
int strlen(const char *s);
int strcmp(const char *a, const char *b);
int strncmp(const char *a, const char *b, unsigned int n);
char *strcpy(char *d, const char *s);
char *strcat(char *d, const char *s);
char *strncat(char *d, const char *s, unsigned int n);
char *strchr(const char *s, int c);
char *strdup(const char *s);
void *malloc(unsigned int n);
void *realloc(void *p, unsigned int n);
void free(void *p);
int toupper(int c);
int tolower(int c);
void exit(int status);

int main(int argc, char **argv)
{
    char *s, *q;
    char buf[64];

    s = malloc(8);
    strcpy(s, "OpenTTY");
    printf("strlen(%s)=%d\n", s, strlen(s));

    strcat(s, " C");
    printf("strcat -> %s\n", s);
    printf("strncat -> ");
    s = realloc(s, 32);
    strncat(s, " stdlib!", 7);
    printf("%s\n", s);

    char *p = strchr(s, ' ');              /* primeira palavra */
    printf("espaco em %d\n", p != 0 ? (int) (p - s) : -1);

    q = strdup(s);
    printf("strdup -> %s\n", q);
    printf("strcmp=%d strncmp(3)=%d\n", strcmp(s, q), strncmp(s, q, 3));

    sprintf(buf, "-> %s (%d letras)", q, strlen(q));
    printf("sprintf  %s\n", buf);
    printf("case: %c%c%c%c\n", toupper('o'), tolower('P'), toupper('p'), tolower('E'));

    free(s);
    free(q);
    exit(0);
    return 0;
}