.global _start
.section .text

_start:
    @ syscall socket(domain=AF_INET(2), type=SOCK_DGRAM(2), protocol=0)
    mov r0, #2             @ AF_INET
    mov r1, #2             @ SOCK_DGRAM
    mov r2, #17            @ IPPROTO_UDP
    mov r7, #281           @ SYS_SOCKET
    svc #0

    cmp r0, #0
    blt sock_error

    mov r9, r0             @ salvar fd (uso futuro)

    @ syscall sendto(fd, buf, len, flags=0, dest_addr, addrlen)
    @ r0=fd, r1=buf, r2=len, r3=flags
    mov r7, #290           @ SYS_SENDTO
    mov r0, r9             @ fd
    ldr r1, =msg           @ buffer de dados
    mov r2, #7             @ len ("HELLOUP" = 7)
    mov r3, #0             @ flags
    ldr r4, =sockaddr      @ dest_addr (stack via getSyscallParam)
    ldr r5, =SOCKADDR_LEN  @ addrlen
    svc #0

    cmp r0, #0
    blt send_error

    mov r7, #4             @ SYS_WRITE
    mov r0, #1             @ stdout
    ldr r1, =ok_msg
    ldr r2, =ok_len
    svc #0
    b exit

sock_error:
    mov r7, #4
    mov r0, #1
    ldr r1, =serr_msg
    ldr r2, =serr_len
    svc #0
    b exit

send_error:
    mov r7, #4
    mov r0, #1
    ldr r1, =derr_msg
    ldr r2, =derr_len
    svc #0
    b exit

exit:
    mov r7, #1             @ SYS_EXIT
    mov r0, #0
    svc #0

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
    @ struct sockaddr_in (16 bytes): family(2) port(2) addr(4) zero(8)
    .short 2              @ AF_INET
    .short 0x1F90         @ port 8080 (big-endian no wire)
    .byte 127, 0, 0, 1    @ 127.0.0.1
    .byte 0,0,0,0,0,0,0,0 @ padding zero
SOCKADDR_LEN = . - sockaddr
