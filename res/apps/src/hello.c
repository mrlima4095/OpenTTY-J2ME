/* hello.c - "Hello, World!" com a stdlib do emulador (svc #LIB_*).
 *
 * Compilar:
 *   ./build-elf.sh res/apps/src/hello.c -stdlib -o res/apps/dist/hello
 */
int printf(const char *fmt, ...);
int strlen(const char *s);
int getpid(void);
int toupper(int c);
void exit(int status);

int main(int argc, char **argv)
{
    printf("Hello, World!\n");
    printf("getpid=%d argc=%d\n", getpid(), argc);

    if (argc > 1) {
        printf("diga algo: ");
        printf("%c\n", toupper(argv[1][0]));
        printf("(%d letras)\n", strlen(argv[1]));
    }

    printf("bye!\n");
    exit(0);
    return 0;
}