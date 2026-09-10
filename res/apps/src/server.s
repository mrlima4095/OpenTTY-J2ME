# server.s (RISC-V RV32IM) - servidor TCP minimalista:
#   socket(281) -> bind(282, &sockaddr, 16) -> listen(284, 5)
#   -> accept(285) -> send(289, msg) -> close(6), em loop.
# ABI RISC-V: args a0-a7, syscall number em a7.
.data
    msg:        .asciz "Hello Network\n"
    msg_len = . - msg

    .align 2
    sockaddr_in:
        .short 2           # AF_INET
        .short 0x0FFF      # Porta 4095
        .byte 0,0,0,0      # INADDR_ANY
        .space 8           # sin_zero para 16 bytes

.text
.global _start
_start:
    li      a0, 2               # AF_INET
    li      a1, 1               # SOCK_STREAM
    li      a2, 6               # IPPROTO_TCP
    li      a7, 281             # SYS_SOCKET
    ecall

    bltz    a0, erro
    mv      s0, a0              # socket fd

    li      a7, 282             # SYS_BIND
    mv      a0, s0
    la      a1, sockaddr_in
    li      a2, 16
    ecall

    bltz    a0, erro

    li      a7, 284             # SYS_LISTEN
    mv      a0, s0
    li      a1, 5
    ecall

    bltz    a0, erro

loop_accept:
    li      a7, 285             # SYS_ACCEPT
    mv      a0, s0
    li      a1, 0
    li      a2, 0
    ecall

    bltz    a0, erro
    mv      s1, a0              # cliente

    li      a7, 289             # SYS_SEND
    mv      a0, s1
    la      a1, msg
    li      a2, msg_len
    li      a3, 0
    ecall

    li      a7, 6               # SYS_CLOSE
    mv      a0, s1
    ecall

    j       loop_accept         # aceita nova conexao

erro:
    li      a7, 1               # SYS_EXIT
    li      a0, 1
    ecall