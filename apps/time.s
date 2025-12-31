.global _start
_start:
    /* gettimeofday */
    ldr r0, =tv
    mov r1, #0        /* timezone = NULL */
    mov r7, #78       /* syscall 78 */
    swi #0
    
    /* write(1, tv, 8) - imprime raw bytes */
    mov r0, #1
    ldr r1, =tv
    mov r2, #8
    mov r7, #4
    swi #0
    
    /* exit */
    mov r0, #0
    mov r7, #1
    swi #0

.section .data
tv: .word 0, 0