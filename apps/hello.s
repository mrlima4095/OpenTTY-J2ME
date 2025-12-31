/* hello_clang_asm.s - Assembly otimizado para Clang ARM */
.syntax unified
.cpu arm7tdmi
.arch armv4t
.fpu softvfp

.global _start
.align 2
.section .text,"ax",%progbits

_start:
    /* write(1, message, length) */
    mov r0, #1          @ fd = stdout
    ldr r1, =message    @ buffer
    mov r2, #18         @ length
    mov r7, #4          @ syscall write
    swi #0              @ syscall
    
    /* exit(0) */
    mov r0, #0          @ exit code
    mov r7, #1          @ syscall exit
    swi #0              @ syscall
    
    /* Se falhar, hang */
1:  b 1b

.section .rodata,"a",%progbits
.align 2
message:
    .asciz "Hello Clang ARM!\n"