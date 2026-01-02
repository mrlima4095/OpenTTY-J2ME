@ simplest_no_loop.s
.section .data
msg: .asciz "A\nB\nC\nDONE\n"

.section .text
.global _start

_start:
    @ Apenas 4 writes e exit
    mov r0, #1
    ldr r1, =msg
    mov r2, #4
    mov r7, #4
    svc 0
    
    mov r0, #1
    add r1, r1, #4
    mov r2, #4
    mov r7, #4
    svc 0
    
    mov r0, #1
    add r1, r1, #4
    mov r2, #4
    mov r7, #4
    svc 0
    
    mov r0, #1
    add r1, r1, #4
    mov r2, #5
    mov r7, #4
    svc 0
    
    @ Exit
    mov r0, #0
    mov r7, #1
    svc 0