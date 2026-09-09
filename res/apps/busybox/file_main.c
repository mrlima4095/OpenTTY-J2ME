/* file_main.c - entrypoint dedicado para o pacote /bin/file.
 *
 * O pacote publicado nao usa o dispatcher do busybox: alguns launchers
 * apresentam argv[0] como "busybox", enquanto file precisa sempre executar
 * app_file, independentemente do nome recebido.
 */
#include "busybox.h"

int bb_out(const char *s) { int n = strlen(s); return n > 0 ? write(1, s, n) : 0; }
int bb_putc(char c) { return write(1, &c, 1); }
int bb_err(const char *s) { int n = strlen(s); return n > 0 ? write(2, s, n) : 0; }

int main(int argc, char **argv)
{
    return app_file(argc, argv);
}
