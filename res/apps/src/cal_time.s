# cal_time.s - wrapper do syscall time(13) para o cal.
# Sem `time` na res/lib/libc.s, entao o wrapper cru (ecall) resolve o relogio
# UTC do emulador, como o bb_time do busybox. O tloc sofre tratamento em a0.
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