/* flags.c - Cobertura de formatadores do printf do emulador.
 *
 * O emulador parser de printf suporta %s %c %d %i %u %x %X %o %p %% e
 * tolera flags/width/precisao (parseia e descarta). Este app so verifica
 * que nada quebra e que os valores saem certos.
 * Compilar:
 *   ./build-elf.sh res/apps/src/flags.c -stdlib -o res/apps/dist/flags
 */
int printf(const char *fmt, ...);
void exit(int status);

int main(int argc, char **argv)
{
    (void) argc; (void) argv;

    printf("[%d][%5d][%-5d][%05d][%+d][% d]\n", 42, 42, 42, 42, 42, -42);
    printf("[%u][%x][%08x][%X][%#x][%#X]\n",
           4000000000U, 0xABCD, 0xABCD, 0xABCD, 0xABCD, 0xABCD);
    printf("[%.3d][%.8u][%5.2d]\n", 7, 123, 7);
    printf("[%s][%12s][%-12s][%o][%p][%%]\n",
           "tty", "tty", "tty", 4000000000U, (void *) 0x12345678);
    printf("[%c][%c%c%c]\n", 'A', 'O', 'K', '!');

    exit(0);
    return 0;
}