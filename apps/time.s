.section .data
msg_sec:    .ascii "Seconds: "
msg_usec:   .ascii " Microseconds: "
newline:    .ascii "\n"
buffer:     .space 16

.section .bss
.tv:        .space 8

.section .text
.global _start

_start:
    /* gettimeofday(&tv, NULL) */
    ldr r0, =.tv
    mov r1, #0
    mov r7, #78
    swi #0

    /* Imprimir "Seconds: " */
    mov r0, #1
    ldr r1, =msg_sec
    mov r2, #9
    mov r7, #4
    swi #0

    /* Converter e imprimir segundos */
    ldr r0, =.tv
    ldr r0, [r0]        /* tv_sec */
    bl print_int

    /* Imprimir " Microseconds: " */
    mov r0, #1
    ldr r1, =msg_usec
    mov r2, #15
    mov r7, #4
    swi #0

    /* Converter e imprimir microssegundos */
    ldr r0, =.tv
    ldr r0, [r0, #4]    /* tv_usec */
    bl print_int

    /* Nova linha */
    mov r0, #1
    ldr r1, =newline
    mov r2, #1
    mov r7, #4
    swi #0

    /* exit(0) */
    mov r0, #0
    mov r7, #1
    swi #0

/* ============================================
   print_int: imprime número decimal em r0
   ============================================ */
print_int:
    push {r4-r6, lr}
    ldr r5, =buffer + 15  /* fim do buffer */
    mov r4, #0            /* flag zero */
    mov r6, #0            /* contador de dígitos */

    /* Caso especial: zero */
    cmp r0, #0
    bne .not_zero
    mov r1, #'0'
    strb r1, [r5]
    sub r5, r5, #1
    add r6, r6, #1
    b .print_done

.not_zero:
    mov r1, #10

.convert_loop:
    cmp r0, #0
    beq .print_done
    
    /* Dividir r0 por 10 */
    mov r2, r0
    mov r0, r2
    bl .divide
    
    add r3, r3, #'0'    /* dígito para ASCII */
    strb r3, [r5]
    sub r5, r5, #1
    add r6, r6, #1
    b .convert_loop

.divide:
    /* Divisão por 10 simples */
    mov r3, #0
.div_loop:
    cmp r2, #10
    blt .div_end
    sub r2, r2, #10
    add r3, r3, #1
    b .div_loop
.div_end:
    mov r3, r2      /* resto em r3 */
    mov r0, r3      /* quociente em r0 (trocado) */
    bx lr

.print_done:
    add r5, r5, #1      /* ajustar para início da string */
    mov r0, #1          /* stdout */
    mov r1, r5          /* buffer */
    mov r2, r6          /* tamanho */
    mov r7, #4          /* write */
    swi #0
    
    pop {r4-r6, pc}