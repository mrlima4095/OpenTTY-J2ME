.global _start
.section .text

_start:
    @ O argc está em sp, mas o emulador pode estar usando um layout diferente
    @ Vamos ler diretamente da forma que o setupCRTStack configurou
    
    ldr r0, [sp]           @ argc
    cmp r0, #1
    ble usage
    
    @ argv[1] - o emulador coloca os ponteiros dos argumentos
    ldr r1, [sp, #4]       @ primeiro ponteiro após argc
    
    @ DEBUG: imprimir endereço do argumento (opcional, remover depois)
    @ mov r7, #4
    @ mov r0, #1
    @ svc #0
    
    @ Tentar abrir o arquivo
    mov r0, r1             @ filename pointer
    mov r1, #0             @ O_RDONLY
    mov r7, #5             @ SYS_OPEN
    svc #0
    
    @ Verificar se abriu corretamente
    cmp r0, #0
    blt open_error
    
    mov r4, r0             @ salvar file descriptor
    
read_loop:
    mov r7, #3             @ SYS_READ
    mov r0, r4
    ldr r1, =buffer
    ldr r2, =BUFSZ
    svc #0
    
    cmp r0, #0
    ble close_file
    
    mov r7, #4             @ SYS_WRITE
    mov r0, #1             @ stdout
    ldr r1, =buffer
    svc #0
    b read_loop

close_file:
    mov r7, #6             @ SYS_CLOSE
    mov r0, r4
    svc #0
    b exit

usage:
    mov r7, #4
    mov r0, #1
    ldr r1, =usage_msg
    ldr r2, =usage_len
    svc #0
    b exit

open_error:
    mov r7, #4
    mov r0, #1
    ldr r1, =err_msg
    ldr r2, =err_len
    svc #0
    b exit

exit:
    mov r7, #1
    mov r0, #0
    svc #0

.section .data
usage_msg:
    .asciz "Usage: cat <filename>\n"
usage_len = . - usage_msg

err_msg:
    .asciz "cat: Cannot open file\n"
err_len = . - err_msg

.section .bss
buffer:
    .space 4096
BUFSZ = 4096