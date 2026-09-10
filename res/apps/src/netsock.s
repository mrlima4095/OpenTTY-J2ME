# netsock.s (RISC-V RV32IM) - socket(AF_INET, SOCK_STREAM) e reporta.
#   socket (281) -> fd; write(1, msg) + exit.
# NOTA: .data antes de .text (llvm-mc nao dobra forward refs em imm).
.global _start
.section .data
ok_msg:
    .asciz "NETSOCK OK: socket() returned a valid fd\n"
ok_len = . - ok_msg
err_msg:
    .asciz "NETSOCK ERR: socket() failed\n"
err_len = . - err_msg

.section .text

_start:
    li      a0, 2                # AF_INET
    li      a1, 1                # SOCK_STREAM
    li      a2, 0                # protocol
    li      a7, 281              # SYS_SOCKET
    ecall

    bltz    a0, sock_error
    mv      s0, a0               # fd do socket

    li      a7, 4                # SYS_WRITE
    li      a0, 1                # stdout
    la      a1, ok_msg
    li      a2, ok_len
    ecall
    j       exit

sock_error:
    li      a7, 4
    li      a0, 1
    la      a1, err_msg
    li      a2, err_len
    ecall

exit:
    li      a7, 1                # SYS_EXIT
    li      a0, 0
    ecall