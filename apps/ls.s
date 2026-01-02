@ minimal.s - Teste absolutamente mínimo
.section .data
msg: .asciz "HELLO\n"

.section .text
.global _start

_start:
    @ write(1, msg, 6)
    mov r0, #1      @ stdout
    ldr r1, =msg
    mov r2, #6      @ tamanho
    mov r7, #4      @ SYS_WRITE
    svc 0
    
    @ exit(0)
    mov r0, #0
    mov r7, #1      @ SYS_EXIT
    svc 0