.syntax unified
.arm

.data
msg:
    .ascii "Current time: "
msg_len = . - msg
newline:
    .ascii "\n"
buf:
    .space 12
time_val:
    .word 0
len_val:
    .word msg_len

.text
.global _start

_start:
    // time(&time_val)
    ldr r0, =time_val
    mov r7, #13         // syscall time
    swi #0
    
    // Imprimir "Current time: "
    mov r0, #1
    ldr r1, =msg
    ldr r2, =len_val    // carregar endereço
    ldr r2, [r2]        // carregar valor
    mov r7, #4
    swi #0
    
    // Carregar tempo e converter
    ldr r4, time_val    // carregar valor do tempo
    ldr r1, =buf
    mov r0, r4
    bl int_to_string
    
    mov r5, r1          // salvar ponteiro final
    
    // Imprimir tempo
    mov r0, #1
    ldr r1, =buf
    sub r2, r5, r1      // calcular tamanho
    mov r7, #4
    swi #0
    
    // Imprimir nova linha
    mov r0, #1
    ldr r1, =newline
    mov r2, #1
    mov r7, #4
    swi #0
    
    // exit(0)
    mov r0, #0
    mov r7, #1
    swi #0

int_to_string:
    // r0 = número, r1 = buffer, retorna em r1 = fim
    push {r4, r5, lr}
    mov r2, r1          // salvar início
    mov r3, #10
    
    cmp r0, #0
    bne not_zero2
    mov r4, #'0'
    strb r4, [r1], #1
    b done_convert2
    
not_zero2:
    mov r4, r0
convert_loop2:
    mov r0, r4
    bl divide2
    add r0, #'0'
    strb r0, [r1], #1
    mov r4, r2
    cmp r4, #0
    bne convert_loop2
    
    mov r3, r1
    sub r3, r3, #1
    mov r4, r2
    
reverse_loop2:
    cmp r4, r3
    bge done_convert2
    ldrb r0, [r4]
    ldrb r5, [r3]
    strb r5, [r4], #1
    strb r0, [r3], #-1
    b reverse_loop2
    
done_convert2:
    mov r1, r2
    pop {r4, r5, pc}

divide2:
    mov r2, #0
div_loop2:
    cmp r0, #10
    blt div_done2
    sub r0, r0, #10
    add r2, r2, #1
    b div_loop2
div_done2:
    bx lr