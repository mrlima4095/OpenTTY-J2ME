.section .text
.global _start

_start:
    @ Abrir arquivo /home/OpenRMS
    mov r7, #5              @ sys_open
    ldr r0, =filename       @ nome do arquivo
    mov r1, #0              @ O_RDONLY
    mov r2, #0              @ mode (ignorado)
    svc 0                   @ syscall

    cmp r0, #0              @ verifica se fd < 0 (erro)
    blt error_exit
    mov r4, r0              @ salva fd em r4

read_loop:
    @ Ler do arquivo
    mov r7, #3              @ sys_read
    mov r0, r4              @ fd
    ldr r1, =buffer         @ buffer
    ldr r2, =BUFSIZE        @ tamanho do buffer
    svc 0                   @ syscall

    cmp r0, #0              @ fim do arquivo?
    beq close_file
    blt error_exit

    @ Escrever no stdout
    mov r7, #4              @ sys_write
    mov r0, #1              @ stdout
    ldr r1, =buffer         @ buffer
    mov r2, r0              @ bytes lidos
    svc 0                   @ syscall

    b read_loop             @ continuar lendo

close_file:
    @ Fechar arquivo
    mov r7, #6              @ sys_close
    mov r0, r4              @ fd
    svc 0                   @ syscall

    @ Saída bem-sucedida
    mov r7, #1              @ sys_exit
    mov r0, #0              @ status 0
    svc 0

error_exit:
    @ Escrever mensagem de erro
    mov r7, #4              @ sys_write
    mov r0, #1              @ stdout
    ldr r1, =err_msg
    ldr r2, =err_len
    svc 0                   @ syscall

    @ Saída com erro
    mov r7, #1              @ sys_exit
    mov r0, #1
    svc 0

.section .data
filename:
    .asciz "/home/OpenRMS"
err_msg:
    .asciz "Erro: Nao foi possivel abrir /home/OpenRMS\n"
err_len = . - err_msg

.section .bss
.equ BUFSIZE, 1024
buffer:
    .space BUFSIZE