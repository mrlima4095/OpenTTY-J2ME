@ Teste com O_DIRECTORY
.text
.global _start

_start:
    @ 1. Imprimir mensagem
    ldr r0, =msg_start
    mov r1, #0
1:  ldrb r2, [r0, r1]
    cmp r2, #0
    beq 2f
    add r1, r1, #1
    b 1b
2:  mov r0, #1
    ldr r1, =msg_start
    mov r7, #4
    swi 0
    
    @ 2. Abrir /home/ com O_DIRECTORY
    @ O_DIRECTORY = 0x10000, O_RDONLY = 0
    mov r1, #0x10000    @ O_DIRECTORY
    ldr r0, =path
    mov r2, #0
    mov r7, #5          @ SYS_OPEN
    swi 0
    
    @ 3. Ver resultado
    cmp r0, #0
    blt error
    
    @ 4. Se abriu, testar getdents
    mov r4, r0          @ salvar fd
    ldr r1, =buffer
    mov r2, #512
    mov r7, #217        @ SYS_GETDENTS
    swi 0
    
    @ 5. Ver resultado
    cmp r0, #0
    blt getdents_error
    
    @ 6. Sucesso!
    mov r5, r0
    b success

error:
    @ Imprimir erro open
    ldr r0, =msg_open_err
    mov r1, #0
3:  ldrb r2, [r0, r1]
    cmp r2, #0
    beq 4f
    add r1, r1, #1
    b 3b
4:  mov r0, #1
    ldr r1, =msg_open_err
    mov r7, #4
    swi 0
    
    mov r0, #1
    mov r7, #1
    swi 0

getdents_error:
    @ Imprimir erro getdents
    ldr r0, =msg_getdents_err
    mov r1, #0
5:  ldrb r2, [r0, r1]
    cmp r2, #0
    beq 6f
    add r1, r1, #1
    b 5b
6:  mov r0, #1
    ldr r1, =msg_getdents_err
    mov r7, #4
    swi 0
    
    mov r0, #1
    mov r7, #1
    swi 0

success:
    @ Fechar fd
    mov r0, r4
    mov r7, #6
    swi 0
    
    @ Imprimir sucesso
    ldr r0, =msg_success
    mov r1, #0
7:  ldrb r2, [r0, r1]
    cmp r2, #0
    beq 8f
    add r1, r1, #1
    b 7b
8:  mov r0, #1
    ldr r1, =msg_success
    mov r7, #4
    swi 0
    
    @ Sair
    mov r0, #0
    mov r7, #1
    swi 0

.data
msg_start:        .asciz "Testando O_DIRECTORY...\n"
msg_open_err:     .asciz "Erro ao abrir diretorio\n"
msg_getdents_err: .asciz "Erro no getdents\n"
msg_success:      .asciz "Sucesso!\n"
path:             .asciz "/home/"
buffer:           .space 512