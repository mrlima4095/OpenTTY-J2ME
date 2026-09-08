/* libutil.c - Biblioteca compartilhada (ET_DYN/.so) com funcoes puras.
 *
 * Serve de contraparte para dcalc.c: o app linka contra ela com
 * DT_NEEDED + PLT/GOT e o emulador resolve os simbolos em runtime
 * (imports dinamicos reais).
 * Compilar:
 *   ./build-elf.sh res/apps/src/libutil.c -shared -o res/apps/dist/libutil.so
 */
int add(int a, int b) { return a + b; }
int sub(int a, int b) { return a - b; }
int mul(int a, int b) { return a * b; }
int pw2(int n) { return 1 << n; }

int sum5(int a, int b, int c, int d, int e) { return a + b + c + d + e; }

unsigned int rotl(unsigned int v, int n)
{
    n &= 31;
    if (n == 0) { return v; }
    return (v << n) | (v >> (32 - n));
}