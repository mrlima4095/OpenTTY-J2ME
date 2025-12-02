.text
.global _start

_start:
    mov r7, #4      @ write
    mov r0, #1      @ stdout
    ldr r1, =msg    @ buffer (usa pool literal)
    mov r2, #13     @ length
    swi 0
    
    mov r7, #1      @ exit
    mov r0, #0
    swi 0

.data
msg: .asciz "Hello File!\n"