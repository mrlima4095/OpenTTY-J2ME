@ fixed_program.s
.section .data
msg:    .asciz "TEST\n"
dir:    .asciz "/"
buffer: .space 512

.section .text
.global _start

_start:
    @ Configurar stack
    ldr sp, =0x000FFC00
    
    @ 1. Teste write
    mov r0, #1
    ldr r1, =msg
    mov r2, #5
    mov r7, #4
    svc 0
    
    @ 2. Open directory
    ldr r0, =dir
    mov r1, #0x10000   @ O_DIRECTORY
    mov r7, #5
    svc 0
    
    mov r4, r0         @ fd
    
    cmp r4, #0
    ble exit_error     @ Se erro, sair
    
    @ 3. Getdents
    mov r0, r4
    ldr r1, =buffer
    mov r2, #512
    mov r7, #217       @ SYS_GETDENTS EABI
    svc 0
    
    mov r5, r0         @ bytes lidos
    
    @ 4. Close
    mov r0, r4
    mov r7, #6
    svc 0
    
    @ 5. Exit com código baseado no resultado
    @ Se getdents retornou > 0, sair com 0 (sucesso)
    @ Se retornou 0, sair com 2 (diretório vazio)
    @ Se retornou negativo, sair com 1 (erro)
    cmp r5, #0
    bgt exit_success
    beq exit_empty
    
    @ Erro
    mov r0, #1
    b exit

exit_empty:
    mov r0, #2
    b exit

exit_error:
    mov r0, #3
    b exit

exit_success:
    mov r0, #0

exit:
    mov r7, #1         @ SYS_EXIT
    svc 0