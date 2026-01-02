@ Teste simples e funcional
.text
.global _start

_start:
    @ Primeiro, imprimir algo para saber que começou
    ldr r0, =start_msg
    mov r1, #0
calc_len1:
    ldrb r2, [r0, r1]
    cmp r2, #0
    beq print1
    add r1, r1, #1
    b calc_len1
print1:
    mov r0, #1
    ldr r1, =start_msg
    mov r7, #4
    swi 0
    
    @ Agora tentar abrir um diretório
    ldr r0, =path
    mov r1, #0          @ O_RDONLY
    mov r7, #5          @ SYS_OPEN
    swi 0
    
    @ Verificar resultado
    cmp r0, #0
    blt error
    
    @ Se abriu com sucesso, fechar
    mov r4, r0          @ salvar fd
    mov r0, r4
    mov r7, #6          @ SYS_CLOSE
    swi 0
    
    @ Imprimir sucesso
    ldr r0, =success_msg
    mov r1, #0
calc_len2:
    ldrb r2, [r0, r1]
    cmp r2, #0
    beq print2
    add r1, r1, #1
    b calc_len2
print2:
    mov r0, #1
    ldr r1, =success_msg
    mov r7, #4
    swi 0
    
    @ Sair com sucesso
    mov r0, #0
    mov r7, #1
    swi 0

error:
    @ Imprimir erro
    ldr r0, =error_msg
    mov r1, #0
calc_len3:
    ldrb r2, [r0, r1]
    cmp r2, #0
    beq print3
    add r1, r1, #1
    b calc_len3
print3:
    mov r0, #1
    ldr r1, =error_msg
    mov r7, #4
    swi 0
    
    @ Sair com erro
    mov r0, #1
    mov r7, #1
    swi 0

.data
start_msg:   .asciz "Iniciando teste...\n"
success_msg: .asciz "Sucesso!\n"
error_msg:   .asciz "Erro!\n"
path:        .asciz "/"