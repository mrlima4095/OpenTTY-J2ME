.global _start
.section .text

_start:
    @ syscall socket(domain=AF_INET(2), type=SOCK_STREAM(1), protocol=0)
    mov r0, #2             @ AF_INET
    mov r1, #1             @ SOCK_STREAM
    mov r2, #0             @ protocol
    mov r7, #281           @ SYS_SOCKET
    svc #0

    cmp r0, #0
    blt sock_error

    @ fd retornado em r0 -> guardar e imprimir sucesso
    mov r9, r0             @ salvar fd

    mov r7, #4             @ SYS_WRITE
    mov r0, #1             @ stdout
    ldr r1, =ok_msg
    ldr r2, =ok_len
    svc #0

    b exit

sock_error:
    mov r7, #4             @ SYS_WRITE
    mov r0, #1             @ stdout
    ldr r1, =err_msg
    ldr r2, =err_len
    svc #0
    b exit

exit:
    mov r7, #1             @ SYS_EXIT
    mov r0, #0
    svc #0

.section .data
ok_msg:
    .asciz "NETSOCK OK: socket() returned a valid fd\n"
ok_len = . - ok_msg
err_msg:
    .asciz "NETSOCK ERR: socket() failed\n"
err_len = . - err_msg
