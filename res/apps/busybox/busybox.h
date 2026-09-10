#ifndef BB_BUSYBOX_H
#define BB_BUSYBOX_H

/* busybox.h - cabecalho comum do multi-tool ELF (res/apps/busybox).
 *
 * Compilado com ./build-elf.sh busybox.c bb_sys.s -stdlib:
 *   -stdlib linka res/lib/libc.s, que fornece (via li a7,#LIB_*; ecall):
 *     open/read/write/close, printf/sprintf/snprintf, malloc/free,
 *     strlen/strcmp/strcpy/strdup/strncmp/memcpy/memset/memmemmove/atoi,
 *     helpers RV32, exit, getpid.
 *   bb_sys.s fornece os syscalls crus extras (getdents/stat/unlink/mkdir/...).
 * Sem floats, sem long long, sem short (idiotas do emulador).
 */

/* ---- prototipos da libc do emulador (res/lib/libc.s) ---------------- */
int open(const char *path, int flags, int mode);
int close(int fd);
int read(int fd, void *buf, int count);
int write(int fd, const void *buf, int count);
int strlen(const char *s);
int strcmp(const char *a, const char *b);
int strncmp(const char *a, const char *b, int n);
char *strcpy(char *dst, const char *src);
char *strncpy(char *dst, const char *src, int n);
char *strcat(char *dst, const char *src);
char *strdup(const char *s);
char *strchr(const char *s, int c);
void *memcpy(void *dst, const void *src, int n);
void *memmove(void *dst, const void *src, int n);
void *memset(void *dst, int c, int n);
int memcmp(const void *a, const void *b, int n);
int atoi(const char *s);
int printf(const char *fmt, ...);
int sprintf(char *buf, const char *fmt, ...);
int snprintf(char *buf, int size, const char *fmt, ...);
int puts(const char *s);
int putchar(int c);
void *malloc(int size);
void *calloc(int n, int size);
void *realloc(void *ptr, int size);
void free(void *ptr);
void exit(int status);

/* ---- syscalls crus (bb_sys.s, li a7,#N; ecall) ----------------------- */
int bb_getdents(int fd, void *dirp, int count);   /* 217 linux_dirent simplificado */
int bb_stat(const char *path, void *st);          /* 106 struct stat (mode@16,size@44) */
int bb_fstat(int fd, void *st);                   /* 108 */
int bb_unlink(const char *path);                  /* 10 */
int bb_mkdir(const char *path, int mode);         /* 39 */
int bb_rmdir(const char *path);                   /* 40 */
int bb_getuid(void);                              /* 199 */
int bb_geteuid(void);                             /* 201 */
int bb_time(void);                                /* 13 (seconds, UTC) */
int bb_uname(void *buf);                          /* 122 (6x65 bytes) */
int bb_getcwd(char *buf, int size);               /* 183 */

/* ---- helpers compartilhados (busybox.c) ------------------------------ */
int bb_out(const char *s);                        /* write(1, s, strlen) */
int bb_outln(const char *s);
int bb_putc(char c);
int bb_err(const char *s);                        /* write(2, s, strlen) */
const char *bb_basename(const char *path);
int bb_is_dir(const char *path);                  /* open(O_DIRECTORY) */
char *bb_pathcat(const char *a, const char *b);   /* malloc a + [/] + b */
int bb_read_all(const char *path, char **data, int *len); /* malloc, 0=ok */
int bb_read_fd(int fd, char **data, int *len);  /* malloc, 0=ok (stdin) */
int bb_filesize(const char *path);                /* stat st_size (44..47) */
extern char **g_envp;                             /* envp passado ao main */

/* ---- applets --------------------------------------------------------- */
typedef int (*bb_applet_fn)(int argc, char **argv);

int app_echo(int argc, char **argv);
int app_true(int argc, char **argv);
int app_false(int argc, char **argv);
int app_seq(int argc, char **argv);
int app_clear(int argc, char **argv);
int app_env(int argc, char **argv);
int app_printf(int argc, char **argv);
int app_basename(int argc, char **argv);
int app_dirname(int argc, char **argv);
int app_id(int argc, char **argv);
int app_whoami(int argc, char **argv);
int app_uname(int argc, char **argv);

int app_cat(int argc, char **argv);
int app_cp(int argc, char **argv);
int app_mv(int argc, char **argv);
int app_rm(int argc, char **argv);
int app_rmdir(int argc, char **argv);
int app_mkdir(int argc, char **argv);
int app_touch(int argc, char **argv);
int app_cmp(int argc, char **argv);
int app_wc(int argc, char **argv);
int app_head(int argc, char **argv);
int app_tail(int argc, char **argv);
int app_sort(int argc, char **argv);
int app_rev(int argc, char **argv);
int app_tr(int argc, char **argv);
int app_du(int argc, char **argv);
int app_ls(int argc, char **argv);

int app_base64(int argc, char **argv);
int app_md5sum(int argc, char **argv);
int app_sum(int argc, char **argv);

int app_date(int argc, char **argv);

int app_file(int argc, char **argv);

#endif
