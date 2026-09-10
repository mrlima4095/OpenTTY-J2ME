# cat.s (RISC-V RV32IM) - cat <arquivo>: imprime um arquivo no stdout.
#
# Usa syscalls crus do emulador (mesmos numeros do ARM-EABI, ecall com a7):
#   open (5), read (3), write (4), close (6), exit (1).
# CRT do emulador: [sp]=argc, [sp+4]=argv[0], [sp+8]=argv[1], ...
#
# NOTA: a .data vem antes do .text porque o llvm-mc nao dobra forward
#       references em imediato (li a2, LEN); manter len = . - msg.
.global _start
.section .data
usage_msg:
    .asciz "Usage: cat <filename>\n"
usage_len = . - usage_msg

err_msg:
    .asciz "cat: Cannot open file\n"
err_len = . - err_msg

.section .text

_start:
    lw      a0, 0(sp)            # argc
    li      t0, 1
    ble     a0, t0, usage        # precisa de argv[1]

    lw      s0, 8(sp)            # argv[1] = nome do arquivo

    mv      a0, s0               # open(filename, O_RDONLY, 0)
    li      a1, 0
    li      a2, 0
    li      a7, 5
    ecall

    bltz    a0, open_error
    mv      s1, a0               # fd

read_loop:
    li      a7, 3                # read(fd, buffer, BUFSZ)
    mv      a0, s1
    la      a1, buffer
    li      a2, 4096
    ecall

    blez    a0, close_file

    mv      s2, a0               # n lidos
    li      a7, 4                # write(1, buffer, n)
    li      a0, 1
    la      a1, buffer
    mv      a2, s2
    ecall
    j       read_loop

close_file:
    li      a7, 6                # close(fd)
    mv      a0, s1
    ecall
    j       exit

usage:
    li      a7, 4                # write(1, usage_msg, usage_len)
    li      a0, 1
    la      a1, usage_msg
    li      a2, usage_len
    ecall
    j       exit

open_error:
    li      a7, 4
    li      a0, 1
    la      a1, err_msg
    li      a2, err_len
    ecall

exit:
    li      a7, 1                # exit(0)
    li      a0, 0
    ecall

.section .bss
buffer:
    .space 4096