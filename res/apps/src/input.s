.global _start
.section .text

_start:
    mov r7, #3             @ read from stdin
    mov r0, #0
    ldr r1, =buffer
    ldr r2, =BUFSZ
    svc #0
    
    mov r7, #4             @ write to stdout
    mov r0, #1
    ldr r1, =buffer
    svc #0
    
    mov r7, #1
    mov r0, #0
    svc #0

.section .bss
buffer: .space 4096
BUFSZ = 4096