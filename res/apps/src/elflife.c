/* elflife.c - dynamic-loader lifecycle integration test.
 *
 * libinit.so depends on libutil.so. The expected output is the constructor,
 * then "result=42", then the destructor when this process exits.
 *
 * Compilar:
 *   ./build-elf.sh res/apps/src/elflife.c -stdlib res/apps/dist/libinit.so \
 *       -o res/apps/dist/elflife
 */
int printf(const char *fmt, ...);
extern int init_result(void);

int main(void)
{
    printf("elflife: result=%d\n", init_result());
    return 0;
}
