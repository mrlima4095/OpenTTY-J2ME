.syntax unified
.arm

.data
prefix:
    .ascii "UID: "
    prefix_len = . - prefix
newline:
    .ascii "\n"
buf:
    .space 12           // buffer para conversão

.text
.global _start

_start:
    // getuid()
    mov r7, #199        // syscall getuid32
    swi #0
    
    // Converter UID para string
    mov r1, r0          // UID
    ldr r2, =buf        // buffer
    bl int_to_string
    
    mov r3, r2          // salvar ponteiro final
    
    // Imprimir "UID: "
    mov r0, #1
    ldr r1, =prefix
    mov r2, #prefix_len
    mov r7, #4
    swi #0
    
    // Imprimir UID
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
    // r1 = número, r2 = buffer
    push {r4, lr}
    mov r3, #10         // divisor
    mov r4, r2
    
    // Converter para string reversa
1:
    mov r0, r1
    bl divide
    add r0, #'0'        // converter para ASCII
    strb r0, [r4], #1   // armazenar e avançar
    mov r1, r2          // quociente
    cmp r1, #0
    bne 1b
    
    // Inverter string
    ldr r0, [sp, #4]    // início original
    sub r3, r4, r0      // tamanho
    mov r1, r0          // início
    sub r2, r4, #1      // fim
    
2:
    cmp r1, r2
    bge 3f
    ldrb r3, [r1]       // trocar
    ldrb r4, [r2]
    strb r4, [r1], #1
    strb r3, [r2], #-1
    b 2b
    
3:
    mov r2, r0          // retornar início
    pop {r4, pc}

divide:
    // r0 dividido por 10, retorna quociente em r2, resto em r0
    mov r2, #0
1:
    cmp r0, #10
    blt 2f
    sub r0, r0, #10
    add r2, r2, #1
    b 1b
2:
    bx lr