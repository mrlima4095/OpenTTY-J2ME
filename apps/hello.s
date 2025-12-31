/* hello_emulator.s - Específico para seu emulador */
.global _start
.align 2

_start:
    /* write(1, "Hello", 5) */
    mov r0, #1            @ stdout
    adr r1, msg           @ endereço da mensagem
    mov r2, #5            @ tamanho
    mov r7, #4            @ syscall write
    swi #0                @ interrupção
    
    /* exit(0) */
    mov r0, #0            @ código de saída
    mov r7, #1            @ syscall exit
    swi #0

msg:
    .ascii "Hello"