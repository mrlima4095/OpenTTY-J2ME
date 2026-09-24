# cal_time.s - time(13) syscall wrapper for cal.
# res/lib/libc.s has no `time`, so this raw (ecall) wrapper reads the
# emulator's UTC clock, like busybox's bb_time. tloc is handled in a0.
.text
.globl time
.type time, %function
time:
    mv      t0, a0          # tloc: NULL -> nao escreve nada
    li      a1, 0
    beq     t0, zero, 1f
    mv      a1, t0
1:
    li      a7, 13          # SYS_TIME: retorna segundos UTC em a0
    ecall
    ret
.size time, .-time