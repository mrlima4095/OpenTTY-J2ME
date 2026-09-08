/* dcalc.c - Importa funcoes de libutil.so (DT_NEEDED + PLT/GOT eager).
 *
 * Testa o imports dinamicos reais do emulador: cada libresolve o GOT
 * no load, o stub .plt real le o GOT e pula direto para a funcao.
 * Compilar:
 *   ./build-elf.sh res/apps/src/dcalc.c -stdlib res/apps/dist/libutil.so \
 *       -o res/apps/dist/dcalc
 */
int printf(const char *fmt, ...);
void exit(int status);

extern int add(int a, int b);
extern int sub(int a, int b);
extern int mul(int a, int b);
extern int pw2(int n);
extern int sum5(int a, int b, int c, int d, int e);
extern unsigned int rotl(unsigned int v, int n);

int main(int argc, char **argv)
{
    int s = (argc > 2) ? 1 : 0;

    printf("add(12,34)=%d sub(100,7)=%d mul(6,7)=%d pw2(10)=%d\n",
           add(12, 34), sub(100, 7), mul(6, 7), pw2(10));
    printf("sum5(1,2,3,4,5)=%d rotl(0x12345678,8)=0x%08x rot1=0x%x\n",
           sum5(1, 2, 3, 4, 5), rotl(0x12345678, 8), rotl(0x12345678, 1));
    printf("argc=%d s=%d\n", argc, s);

    exit(0);
    return 0;
}