@ pure_arm.s - Sem libc, controle total
.section .data
msg:    .asciz "TEST\n"

.section .text
.global _start

_start:
    @ Configurar stack (importante!)
    ldr sp, =0x000FFC00  @ Mesmo SP do seu emulador
    
    @ Teste write
    mov r0, #1          @ stdout
    ldr r1, =msg        @ mensagem
    mov r2, #5          @ tamanho
    mov r7, #4          @ SYS_WRITE
    svc 0
    
    @ Teste open diretório
    ldr r0, =dir_path
    mov r1, #0x10000    @ O_DIRECTORY
    mov r7, #5          @ SYS_OPEN
    svc 0
    
    mov r4, r0          @ salvar fd
    
    @ Se fd > 0, teste getdents
    cmp r4, #0
    ble exit_fail
    
    @ getdents
    mov r0, r4
    ldr r1, =buffer
    mov r2, #512
    mov r7, #217        @ SYS_GETDENTS EABI
    svc 0
    
    mov r5, r0          @ salvar resultado
    
    @ Fechar
    mov r0, r4
    mov r7, #6          @ SYS_CLOSE
    svc 0
    
    @ Sair com código baseado no resultado
    mov r0, r5
    b exit

exit_fail:
    mov r0, #1

exit:
    mov r7, #1          @ SYS_EXIT
    svc 0

dir_path: .asciz "/"
buffer:   .space 512