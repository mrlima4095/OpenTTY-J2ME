/* libinit.c - verifies shared-library constructors and transitive DT_NEEDED.
 *
 * Compilar:
 *   ./build-elf.sh res/apps/src/libinit.c -shared res/apps/dist/libutil.so \
 *       -o res/apps/dist/libinit.so
 */
int printf(const char *fmt, ...);
extern int add(int a, int b);

static int initialized;

__attribute__((constructor)) static void library_init(void)
{
    initialized = 1;
    printf("libinit: constructor\n");
}

__attribute__((destructor)) static void library_fini(void)
{
    printf("libinit: destructor\n");
}

int init_result(void)
{
    return initialized ? add(20, 22) : -1;
}
