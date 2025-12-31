.global _start

_start:
    /* Imprimir Hello World */
    mov r7, #4            @ syscall write = 4
    mov r0, #1            @ fd = stdout (1)
    ldr r1, =message      @ endereço da string
    mov r2, #12           @ tamanho da string
    swi 0                 @ chamada de sistema

message:
    .ascii "Hello World\n"