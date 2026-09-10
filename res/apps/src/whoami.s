# whoami.s (RISC-V RV32IM) - imprime o usuario: le /home/OpenRMS.
#   open (5), read (3), write (4), close (6), exit (1).
# NOTA: .data antes de .text (llvm-mc nao dobra forward refs em imm).
.global _start
.section .data
filename:
    .asciz "/home/OpenRMS"
err_msg:
    .asciz "Error: Cannot open /home/OpenRMS\n"
err_len = . - err_msg

.section .text

_start:
    la      a0, filename        # open(filename, O_RDONLY, 0)
    li      a1, 0
    li      a2, 0
    li      a7, 5
    ecall

    bltz    a0, error
    mv      s0, a0              # fd

    li      a7, 3               # read(fd, buffer, BUFFER_SIZE)
    mv      a0, s0
    la      a1, buffer
    li      a2, 4096
    ecall

    blez    a0, close_file

    mv      s1, a0              # n lidos
    li      a7, 4               # write(1, buffer, n)
    li      a0, 1
    la      a1, buffer
    mv      a2, s1
    ecall

close_file:
    li      a7, 6               # close(fd)
    mv      a0, s0
    ecall

exit:
    li      a7, 1               # exit(0)
    li      a0, 0
    ecall

error:
    li      a7, 4               # write(1, err_msg, err_len)
    li      a0, 1
    la      a1, err_msg
    li      a2, err_len
    ecall
    j       exit

.section .bss
buffer:
    .space 4096