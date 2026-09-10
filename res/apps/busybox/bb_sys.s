# bb_sys.s - syscalls crus do emulador (nao cobertos por res/lib/libc.s).
# Cada wrapper usa a0-a2 para argumentos, a7 para o numero e retorna em a0.
# Compilar junto: ./build-elf.sh busybox.c bb_sys.s -stdlib -o busybox

.text

# linux_dirent simplificado: d_ino(4) d_off(4) d_reclen(2) d_name[var]
.globl bb_getdents
.type bb_getdents, %function
bb_getdents:
    li      a7, 217
    ecall
    ret

# struct stat simplificado: st_mode em offset 16, st_size em offset 44
.globl bb_stat
.type bb_stat, %function
bb_stat:
    li      a7, 106
    ecall
    ret

.globl bb_fstat
.type bb_fstat, %function
bb_fstat:
    li      a7, 108
    ecall
    ret

.globl bb_unlink
.type bb_unlink, %function
bb_unlink:
    li      a7, 10
    ecall
    ret

.globl bb_mkdir
.type bb_mkdir, %function
bb_mkdir:
    li      a7, 39
    ecall
    ret

.globl bb_rmdir
.type bb_rmdir, %function
bb_rmdir:
    li      a7, 40
    ecall
    ret

.globl bb_getuid
.type bb_getuid, %function
bb_getuid:
    li      a7, 199
    ecall
    ret

.globl bb_geteuid
.type bb_geteuid, %function
bb_geteuid:
    li      a7, 201
    ecall
    ret

.globl bb_time
.type bb_time, %function
bb_time:
    li      a0, 0           # time(NULL): a0 e o ponteiro de retorno
    li      a7, 13
    ecall
    ret

# utsname: 6 campos de 65 bytes
.globl bb_uname
.type bb_uname, %function
bb_uname:
    li      a7, 122
    ecall
    ret

.globl bb_getcwd
.type bb_getcwd, %function
bb_getcwd:
    li      a7, 183
    ecall
    ret
