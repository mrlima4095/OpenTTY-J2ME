@ Versão simplificada
.text
.global _start

_start:
    @ Alocar buffer na stack (256 bytes)
    sub sp, sp, #256
    
    @ getcwd(buffer, size)
    mov r0, sp          @ buffer
    mov r1, #256        @ size
    mov r7, #183        @ syscall getcwd
    swi 0
    
    @ Escrever resultado
    mov r7, #4          @ write
    mov r0, #1          @ stdout
    mov r1, sp          @ buffer
    mov r2, #256        @ max length
    swi 0
    
    @ Exit
    mov r7, #1
    mov r0, #0
    swi 0