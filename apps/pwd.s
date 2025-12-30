.syntax unified
.arm

.data
buffer:
    .space 256          // buffer para getcwd
newline:
    .ascii "\n"

.text
.global _start

_start:
    // getcwd(buffer, 256)
    ldr r0, =buffer     // buffer
    mov r1, #256        // tamanho
    mov r7, #183        // syscall getcwd
    swi #0
    
    // Calcular tamanho da string
    ldr r1, =buffer
    mov r2, #0
    
1:
    ldrb r3, [r1, r2]
    cmp r3, #0
    beq 2f
    add r2, r2, #1
    b 1b
    
2:
    // write(1, buffer, tamanho)
    mov r0, #1
    ldr r1, =buffer
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