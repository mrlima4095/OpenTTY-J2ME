.syntax unified
.arm

.data
prefix:
    .ascii "UID: "
prefix_len = . - prefix
newline:
    .ascii "\n"
buf:
    .space 12
len_value:
    .word prefix_len    // armazenar tamanho aqui

.text
.global _start

_start:
    // getuid()
    mov r7, #199        // syscall getuid32
    swi #0
    
    // Converter UID para string
    mov r4, r0          // salvar UID em r4
    
    // Imprimir "UID: "
    mov r0, #1
    ldr r1, =prefix
    ldr r2, =len_value  // carregar endereço
    ldr r2, [r2]        // carregar valor
    mov r7, #4
    swi #0
    
    // Converter UID em r4 para string
    ldr r1, =buf
    mov r0, r4
    bl int_to_string
    
    mov r5, r1          // salvar ponteiro final
    
    // Imprimir UID
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
    mov r3, #10         // divisor
    
    // Caso especial: número 0
    cmp r0, #0
    bne not_zero
    mov r4, #'0'
    strb r4, [r1], #1
    b done_convert
    
not_zero:
    // Converter para string reversa
    mov r4, r0
convert_loop:
    mov r0, r4
    bl divide
    add r0, #'0'        // converter para ASCII
    strb r0, [r1], #1   // armazenar e avançar
    mov r4, r2          // quociente
    cmp r4, #0
    bne convert_loop
    
    // Inverter string
    mov r3, r1          // fim
    sub r3, r3, #1      // último caractere
    mov r4, r2          // início
    
reverse_loop:
    cmp r4, r3
    bge done_convert
    ldrb r0, [r4]       // trocar
    ldrb r5, [r3]
    strb r5, [r4], #1
    strb r0, [r3], #-1
    b reverse_loop
    
done_convert:
    mov r1, r2          // retornar início
    pop {r4, r5, pc}

divide:
    // r0 dividido por 10, retorna quociente em r2, resto em r0
    mov r2, #0
div_loop:
    cmp r0, #10
    blt div_done
    sub r0, r0, #10
    add r2, r2, #1
    b div_loop
div_done:
    bx lr