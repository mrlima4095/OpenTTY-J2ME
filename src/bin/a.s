.text
.global _start

_start:
    mov r7, #4      @ syscall write
    mov r0, #1      @ fd = stdout
    adr r1, msg     @ buffer
    mov r2, #14     @ length
    swi 0
    
    mov r7, #1      @ syscall exit
    mov r0, #0      @ status
    swi 0

msg: .asciz "Hello World!\n"