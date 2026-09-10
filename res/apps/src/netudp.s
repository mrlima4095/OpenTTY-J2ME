# netudp.s (RISC-V RV32IM) - socket UDP + sendto para 127.0.0.1:8080.
#   socket (281), sendto (290), write (4), exit (1).
# ABI RISC-V: args a0-a7; sendto(fd, buf, len, flags, dest_addr, addrlen)
# vai em a0..a5 (x10..x15), syscall number em a7.
# NOTA: .data antes de .text (llvm-mc nao dobra forward refs em imm).
.global _start
.section .data
msg:
    .asciz "HELLOUP"
ok_msg:
    .asciz "NETUDP OK: socket + sendto succeeded\n"
ok_len = . - ok_msg
serr_msg:
    .asciz "NETUDP ERR: socket() failed\n"
serr_len = . - serr_msg
derr_msg:
    .asciz "NETUDP ERR: sendto() failed\n"
derr_len = . - derr_msg

sockaddr:
    .short 2              # AF_INET
    .short 0x1F90         # port 8080
    .byte 127, 0, 0, 1    # 127.0.0.1
    .byte 0,0,0,0,0,0,0,0 # padding zero
SOCKADDR_LEN = . - sockaddr

.section .text

_start:
    li      a0, 2                # AF_INET
    li      a1, 2                # SOCK_DGRAM
    li      a2, 17               # IPPROTO_UDP
    li      a7, 281              # SYS_SOCKET
    ecall

    bltz    a0, sock_error
    mv      s0, a0               # fd do socket

    li      a7, 290              # SYS_SENDTO
    mv      a0, s0               # fd
    la      a1, msg              # buffer
    li      a2, 7                # len ("HELLOUP" = 7)
    li      a3, 0                # flags
    la      a4, sockaddr         # dest_addr
    li      a5, SOCKADDR_LEN     # addrlen
    ecall

    bltz    a0, send_error

    li      a7, 4                # SYS_WRITE
    li      a0, 1                # stdout
    la      a1, ok_msg
    li      a2, ok_len
    ecall
    j       exit

sock_error:
    li      a7, 4
    li      a0, 1
    la      a1, serr_msg
    li      a2, serr_len
    ecall
    j       exit

send_error:
    li      a7, 4
    li      a0, 1
    la      a1, derr_msg
    li      a2, derr_len
    ecall

exit:
    li      a7, 1                # SYS_EXIT
    li      a0, 0
    ecall