/* file_main.c - dedicated entrypoint for the /bin/file package.
 *
 * The published package bypasses the busybox dispatcher: some launchers pass
 * "busybox" as argv[0], but file must always execute app_file.
 */
#include "busybox.h"

int bb_out(const char *s) { int n = strlen(s); return n > 0 ? write(1, s, n) : 0; }
int bb_putc(char c) { return write(1, &c, 1); }
int bb_err(const char *s) { int n = strlen(s); return n > 0 ? write(2, s, n) : 0; }
int bb_is_dir(const char *path) { int fd = open(path, 0x10000, 0); if (fd < 0) return 0; close(fd); return 1; }

int main(int argc, char **argv)
{
    return app_file(argc, argv);
}
