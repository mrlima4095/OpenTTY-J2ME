.global _start

.text
_start:
    mov r0, #2
    mov r1, #2
    add r0, r0, r1
    add r0, r0, #48
    
    ldr r1, =buffer
    strb r0, [r1]
    
    mov r0, #1
    ldr r1, =buffer
    mov r2, #1
    mov r7, #4
    swi 0
    
    mov r0, #1
    ldr r1, =newline
    mov r2, #1
    mov r7, #4
    swi 0
    
    mov r0, #0
    mov r7, #1
    swi 0

.data
buffer:
    .byte 0
newline:
    .byte 10