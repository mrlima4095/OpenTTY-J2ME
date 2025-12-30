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
    mov r2, #msg_len
    mov r7, #4
    swi #0
    
    // Converter tempo para string
    ldr r1, time_val
    ldr r2, =buf
    bl int_to_string
    
    mov r3, r2          // salvar ponteiro final
    
    // Imprimir tempo
    mov r0, #1
    ldr r1, =buf
    sub r2, r3, r1      // calcular tamanho
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
    push {r4, lr}
    mov r3, #10
    mov r4, r2
    
1:
    mov r0, r1
    bl divide
    add r0, #'0'
    strb r0, [r4], #1
    mov r1, r2
    cmp r1, #0
    bne 1b
    
    ldr r0, [sp, #4]
    sub r3, r4, r0
    mov r1, r0
    sub r2, r4, #1
    
2:
    cmp r1, r2
    bge 3f
    ldrb r3, [r1]
    ldrb r4, [r2]
    strb r4, [r1], #1
    strb r3, [r2], #-1
    b 2b
    
3:
    mov r2, r0
    pop {r4, pc}

divide:
    mov r2, #0
1:
    cmp r0, #10
    blt 2f
    sub r0, r0, #10
    add r2, r2, #1
    b 1b
2:
    bx lr