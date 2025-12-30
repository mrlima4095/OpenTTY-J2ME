.syntax unified
.arm

.data
msg:
    .ascii "Hello, World!\n"
    len = . - msg

.text
.global _start

_start:
    // write(1, msg, len)
    mov r0, #1          // stdout
    ldr r1, =msg        // mensagem
    mov r2, #len        // tamanho
    mov r7, #4          // syscall write
    swi #0

    // exit(0)
    mov r0, #0          // código de retorno
    mov r7, #1          // syscall exit
    swi #0