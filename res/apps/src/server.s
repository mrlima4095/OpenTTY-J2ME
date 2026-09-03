.data
    msg:        .asciz "Hello Network\n"
    msg_len = . - msg

    .align 2
    sockaddr_in:
        .short 2           // AF_INET
        .short 0x0FFF      // Porta 4095 (little‑endian – o emulador lê como 0x0FFF)
        .byte 0,0,0,0      // INADDR_ANY
        .space 8           // preenchimento (sin_zero) para 16 bytes

.text
.global _start
_start:
    // socket(AF_INET, SOCK_STREAM, IPPROTO_TCP)
    mov     r0, #2          // AF_INET
    mov     r1, #1          // SOCK_STREAM
    mov     r2, #6          // IPPROTO_TCP
    mov     r7, #281        // syscall SYS_SOCKET
    swi     0

    cmp     r0, #0
    blt     erro            // se fd < 0, sai com erro

    mov     r4, r0          // guarda o socket fd

    // bind(sockfd, &sockaddr_in, 16)
    mov     r0, r4
    ldr     r1, =sockaddr_in
    mov     r2, #16
    mov     r7, #282        // SYS_BIND
    swi     0

    cmp     r0, #0
    blt     erro

    // listen(sockfd, 5)
    mov     r0, r4
    mov     r1, #5
    mov     r7, #284        // SYS_LISTEN
    swi     0

    cmp     r0, #0
    blt     erro

loop_accept:
    // accept(sockfd, NULL, NULL)
    mov     r0, r4
    mov     r1, #0
    mov     r2, #0
    mov     r7, #285        // SYS_ACCEPT
    swi     0

    cmp     r0, #0
    blt     erro            // se erro, finaliza (pode‑se optar por continuar)

    mov     r5, r0          // guarda o novo socket (cliente)

    // send(new_fd, msg, msg_len, 0)
    mov     r0, r5
    ldr     r1, =msg
    mov     r2, #msg_len
    mov     r3, #0
    mov     r7, #289        // SYS_SEND
    swi     0

    // close(new_fd)
    mov     r0, r5
    mov     r7, #6          // SYS_CLOSE
    swi     0

    b       loop_accept     // aceita nova conexão

erro:
    // exit(1)
    mov     r0, #1
    mov     r7, #1          // SYS_EXIT
    swi     0