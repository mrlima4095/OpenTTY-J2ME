@ Programa SUPER SIMPLES - sem cálculo de string
.text
.global _start

_start:
    @ 1. Escrever "TEST" usando tamanho fixo
    mov r0, #1          @ stdout
    ldr r1, =msg        @ mensagem
    mov r2, #5          @ tamanho FIXO: "TEST\n" = 5 bytes
    mov r7, #4          @ SYS_WRITE
    swi 0
    
    @ 2. Tentar open (mas primeiro vamos testar sem)
    @ Vamos pular o open por agora
    
    @ 3. Sair
    mov r0, #0          @ status
    mov r7, #1          @ SYS_EXIT
    swi 0

.data
msg: .asciz "TEST\n"