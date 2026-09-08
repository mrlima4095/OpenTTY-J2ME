@ bb_sys.s - syscalls crus do emulador (nao cobertos por res/lib/libc.s).
@ Cada wrapper e' mov r7,#N; svc #0; bx lr. Args ja chegam em r0-r2 (AAPCS),
@ retorno em r0. Ver src/ELF.java handleSyscall.
@
@ Compilar junto:  ./build-elf.sh busybox.c bb_sys.s -stdlib -o busybox

.syntax unified
.arm
.text

@ linux_dirent simplificado: d_ino(4) d_off(4) d_reclen(2) d_name[var]
.globl bb_getdents
bb_getdents:
    mov     r7, #217
    svc     #0
    bx      lr

@ struct stat simplificado: st_mode em offset 16, st_size em offset 44
.globl bb_stat
bb_stat:
    mov     r7, #106
    svc     #0
    bx      lr

.globl bb_fstat
bb_fstat:
    mov     r7, #108
    svc     #0
    bx      lr

.globl bb_unlink
bb_unlink:
    mov     r7, #10
    svc     #0
    bx      lr

.globl bb_mkdir
bb_mkdir:
    mov     r7, #39
    svc     #0
    bx      lr

.globl bb_rmdir
bb_rmdir:
    mov     r7, #40
    svc     #0
    bx      lr

.globl bb_getuid
bb_getuid:
    mov     r7, #199
    svc     #0
    bx      lr

.globl bb_geteuid
bb_geteuid:
    mov     r7, #201
    svc     #0
    bx      lr

.globl bb_time
bb_time:
    mov     r0, #0          @ time(NULL): r0 eh o ponteiro de retorno
    mov     r7, #13
    svc     #0
    bx      lr

@ utsname: 6 campos de 65 bytes
.globl bb_uname
bb_uname:
    mov     r7, #122
    svc     #0
    bx      lr

.globl bb_getcwd
bb_getcwd:
    mov     r7, #183
    svc     #0
    bx      lr

.end
