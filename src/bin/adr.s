.text
.global _start

_start:
    mov r7, #4      @ write
    mov r0, #1      @ stdout
    adr r1, msg     @ buffer usando endereço relativo
    mov r2, #14     @ length
    swi 0
    
    mov r7, #1      @ exit
    mov r0, #0
    swi 0

msg: .asciz "Hello ADR!\n"