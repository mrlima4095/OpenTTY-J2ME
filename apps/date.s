.global _start

.section .data
msg_date:   .asciz  "Data: "
msg_time:   .asciz  "\nHora: "
buffer:     .space  64
nl:         .asciz  "\n"

.section .text

_start:
    // Obter tempo atual (syscall time)
    mov r7, #13         // SYS_TIME = 13
    ldr r0, =buffer     // Buffer para armazenar o tempo
    swi #0              // Chamada de sistema
    
    // O tempo retornado está em R0 (segundos desde a época)
    // Vamos converter para um formato legível
    
    // Salvar o timestamp
    push {r0}
    
    // Imprimir "Data: "
    mov r7, #4          // SYS_WRITE = 4
    mov r0, #1          // stdout
    ldr r1, =msg_date
    mov r2, #6          // Tamanho da string
    swi #0
    
    // Recuperar timestamp
    pop {r0}
    push {r0}
    
    // Converter timestamp para data
    bl timestamp_to_date
    
    // Imprimir data (está em buffer)
    mov r7, #4          // SYS_WRITE = 4
    mov r0, #1          // stdout
    ldr r1, =buffer
    mov r2, #10         // Tamanho aproximado
    swi #0
    
    // Imprimir "Hora: "
    mov r7, #4          // SYS_WRITE = 4
    mov r0, #1          // stdout
    ldr r1, =msg_time
    mov r2, #6          // Tamanho da string
    swi #0
    
    // Recuperar timestamp novamente
    pop {r0}
    
    // Converter timestamp para hora
    bl timestamp_to_time
    
    // Imprimir hora (está em buffer)
    mov r7, #4          // SYS_WRITE = 4
    mov r0, #1          // stdout
    ldr r1, =buffer
    mov r2, #8          // Tamanho aproximado
    swi #0
    
    // Nova linha
    mov r7, #4          // SYS_WRITE = 4
    mov r0, #1          // stdout
    ldr r1, =nl
    mov r2, #1          // Tamanho da string
    swi #0
    
    // Sair do programa
    mov r7, #1          // SYS_EXIT = 1
    mov r0, #0          // Código de saída 0
    swi #0

// Função para converter timestamp para data
// Entrada: R0 = timestamp
// Saída: buffer preenchido com data no formato DD/MM/AAAA
timestamp_to_date:
    push {r4-r7, lr}
    
    // Usar R0 como timestamp
    mov r4, r0          // Salvar timestamp em R4
    
    // Calcular dias desde a época (1/1/1970)
    // 86400 segundos por dia
    ldr r5, =86400
    udiv r6, r4, r5     // R6 = dias totais
    
    // Ano base 1970
    mov r7, #1970
    
    // Contador de anos
year_loop:
    // Verificar se é ano bissexto
    mov r0, r7
    bl is_leap_year
    cmp r0, #0
    movne r1, #366      // Dias no ano bissexto
    moveq r1, #365      // Dias no ano normal
    
    // Verificar se temos dias suficientes
    cmp r6, r1
    blt year_done
    
    // Subtrair dias do ano
    sub r6, r6, r1
    
    // Próximo ano
    add r7, r7, #1
    b year_loop

year_done:
    // R7 = ano atual
    // R6 = dia do ano (0-indexed)
    
    // Salvar ano
    push {r7}
    
    // Array de dias por mês (ano não bissexto)
    ldr r5, =month_days_normal
    mov r0, r7
    bl is_leap_year
    cmp r0, #0
    ldrne r5, =month_days_leap   // Usar array para ano bissexto
    
    // Encontrar mês
    mov r1, #0          // Contador de mês (0-indexed)
    mov r2, #0          // Acumulador de dias
    
month_loop:
    ldrb r3, [r5, r1]   // Dias no mês atual
    add r4, r2, r3      // Acumular dias
    
    cmp r6, r4
    blt month_done
    
    // Ainda não chegamos ao mês
    mov r2, r4          // Atualizar acumulador
    add r1, r1, #1      // Próximo mês
    b month_loop

month_done:
    // R1 = mês (0-indexed)
    // R6 = dia do ano
    // R2 = dias acumulados antes deste mês
    
    // Calcular dia do mês
    sub r6, r6, r2      // R6 = dia do mês (0-indexed)
    add r6, r6, #1      // Converter para 1-indexed
    
    // Recuperar ano
    pop {r7}
    
    // Converter para string no buffer
    ldr r0, =buffer
    
    // Dia (2 dígitos)
    mov r1, r6
    bl number_to_string2
    
    // Separador
    mov r1, #'/'
    strb r1, [r0], #1
    
    // Mês (2 dígitos) - R1 já tem mês 0-indexed
    add r1, r1, #1      // Converter para 1-indexed
    bl number_to_string2
    
    // Separador
    mov r1, #'/'
    strb r1, [r0], #1
    
    // Ano (4 dígitos)
    mov r1, r7
    bl number_to_string4
    
    // Terminar string
    mov r1, #0
    strb r1, [r0]
    
    pop {r4-r7, pc}

// Função para converter timestamp para hora
// Entrada: R0 = timestamp
// Saída: buffer preenchido com hora no formato HH:MM:SS
timestamp_to_time:
    push {r4-r7, lr}
    
    // Calcular segundos do dia
    ldr r1, =86400
    udiv r2, r0, r1     // R2 = dias
    mul r3, r2, r1      // R3 = segundos dos dias completos
    sub r4, r0, r3      // R4 = segundos do dia atual
    
    // Calcular horas
    mov r5, #3600
    udiv r6, r4, r5     // R6 = horas
    mul r7, r6, r5      // R7 = segundos das horas
    sub r4, r4, r7      // R4 = segundos restantes
    
    // Calcular minutos
    mov r5, #60
    udiv r7, r4, r5     // R7 = minutos
    mul r1, r7, r5      // R1 = segundos dos minutos
    sub r4, r4, r1      // R4 = segundos
    
    // R6 = horas, R7 = minutos, R4 = segundos
    
    // Converter para string no buffer
    ldr r0, =buffer
    
    // Horas (2 dígitos)
    mov r1, r6
    bl number_to_string2
    
    // Separador
    mov r1, #':'
    strb r1, [r0], #1
    
    // Minutos (2 dígitos)
    mov r1, r7
    bl number_to_string2
    
    // Separador
    mov r1, #':'
    strb r1, [r0], #1
    
    // Segundos (2 dígitos)
    mov r1, r4
    bl number_to_string2
    
    // Terminar string
    mov r1, #0
    strb r1, [r0]
    
    pop {r4-r7, pc}

// Função para converter número para string com 2 dígitos
// Entrada: R0 = ponteiro para buffer, R1 = número
// Saída: R0 atualizado (após os 2 dígitos)
number_to_string2:
    push {r2-r3, lr}
    
    // Converter para base 10
    mov r2, #10
    udiv r3, r1, r2     // Dezena
    mul r2, r3, r2      // Recriar divisor
    sub r2, r1, r2      // Unidade
    
    // Converter dígitos para ASCII
    add r3, r3, #'0'
    strb r3, [r0], #1
    
    add r2, r2, #'0'
    strb r2, [r0], #1
    
    pop {r2-r3, pc}

// Função para converter número para string com 4 dígitos
// Entrada: R0 = ponteiro para buffer, R1 = número
// Saída: R0 atualizado (após os 4 dígitos)
number_to_string4:
    push {r2-r4, lr}
    
    // Converter para base 10
    mov r2, #1000
    udiv r3, r1, r2     // Milhar
    mul r4, r3, r2
    sub r1, r1, r4
    
    add r3, r3, #'0'
    strb r3, [r0], #1
    
    mov r2, #100
    udiv r3, r1, r2     // Centena
    mul r4, r3, r2
    sub r1, r1, r4
    
    add r3, r3, #'0'
    strb r3, [r0], #1
    
    mov r2, #10
    udiv r3, r1, r2     // Dezena
    mul r4, r3, r2
    sub r2, r1, r4      // Unidade
    
    add r3, r3, #'0'
    strb r3, [r0], #1
    
    add r2, r2, #'0'
    strb r2, [r0], #1
    
    pop {r2-r4, pc}

// Função para verificar se é ano bissexto
// Entrada: R0 = ano
// Saída: R0 = 1 se bissexto, 0 se não
is_leap_year:
    push {r1-r3, lr}
    
    // Divisível por 400?
    mov r1, #400
    udiv r2, r0, r1
    mul r3, r2, r1
    cmp r0, r3
    moveq r0, #1
    beq leap_done
    
    // Divisível por 100?
    mov r1, #100
    udiv r2, r0, r1
    mul r3, r2, r1
    cmp r0, r3
    moveq r0, #0
    beq leap_done
    
    // Divisível por 4?
    mov r1, #4
    udiv r2, r0, r1
    mul r3, r2, r1
    cmp r0, r3
    moveq r0, #1
    movne r0, #0

leap_done:
    pop {r1-r3, pc}

.section .rodata
month_days_normal:
    .byte 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31

month_days_leap:
    .byte 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31