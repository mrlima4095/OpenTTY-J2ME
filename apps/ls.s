@ simple_test_dir.s
.section .data
dir:    .asciz "/home/"
msg1:   .asciz "Opening directory...\n"
msg2:   .asciz "Success! Reading entries...\n"
msg3:   .asciz "Done.\n"
buffer: .space 512

.section .text
.global _start

_start:
    @ Escrever mensagem inicial
    ldr r1, =msg1
    mov r2, #21
    bl write
    
    @ Tentar abrir diretório
    ldr r0, =dir
    mov r1, #0x10000   @ O_DIRECTORY
    mov r7, #5         @ SYS_OPEN
    svc 0
    
    cmp r0, #0
    ble error
    
    mov r4, r0         @ salvar fd
    
    @ Mensagem de sucesso
    ldr r1, =msg2
    mov r2, #28
    bl write
    
    @ Tentar getdents (EABI = 217)
    mov r0, r4
    ldr r1, =buffer
    mov r2, #512
    mov r7, #217       @ SYS_GETDENTS EABI
    svc 0
    
    @ Se retornou > 0, sucesso
    cmp r0, #0
    ble no_entries
    
    @ Imprimir quantos bytes foram lidos
    @ Converter número para string
    mov r5, r0         @ salvar bytes lidos
    
    ldr r1, =msg3
    mov r2, #6
    bl write
    
    @ Fechar
    mov r0, r4
    mov r7, #6         @ SYS_CLOSE
    svc 0
    
    @ Sair com bytes lidos como código
    mov r0, r5
    b exit

no_entries:
    ldr r1, =msg3
    mov r2, #6
    bl write
    mov r0, #0
    b close_and_exit

error:
    mov r0, #1

close_and_exit:
    @ Fechar se estiver aberto
    cmp r4, #0
    ble exit
    mov r0, r4
    mov r7, #6
    svc 0

exit:
    mov r7, #1         @ SYS_EXIT
    svc 0

write:
    push {r0, r7}
    mov r0, #1
    mov r7, #4
    svc 0
    pop {r0, r7}
    bx lr