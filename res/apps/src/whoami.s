.global _start
.section .text

_start:
    @ Abrir arquivo /home/OpenRMS
    mov r7, #5              @ syscall open
    ldr r0, =filename       @ caminho do arquivo
    mov r1, #0              @ O_RDONLY
    mov r2, #0              @ mode
    svc #0
    
    cmp r0, #0
    blt error               @ erro ao abrir
    
    mov r4, r0              @ salvar fd
    
    @ Ler arquivo
    mov r7, #3              @ syscall read
    mov r0, r4              @ fd
    ldr r1, =buffer         @ buffer
    ldr r2, =BUFFER_SIZE    @ tamanho
    svc #0
    
    cmp r0, #0
    ble close_file
    
    @ Escrever no stdout
    mov r7, #4              @ syscall write
    mov r0, #1              @ stdout
    ldr r1, =buffer         @ buffer
    svc #0
    
close_file:
    @ Fechar arquivo
    mov r7, #6              @ syscall close
    mov r0, r4
    svc #0
    
exit:
    mov r7, #1              @ syscall exit
    mov r0, #0
    svc #0

error:
    @ Mensagem de erro
    mov r7, #4
    mov r0, #1
    ldr r1, =err_msg
    ldr r2, =err_len
    svc #0
    b exit

.section .data
filename:
    .asciz "/home/OpenRMS"
err_msg:
    .asciz "Error: Cannot open /home/OpenRMS\n"
err_len = . - err_msg

.section .bss
buffer:
    .space 4096
BUFFER_SIZE = . - buffer