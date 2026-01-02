@ listdir_full.s - Lista diretório completo com EABI
.section .data
dir:        .asciz "/mnt/"
buffer:     .space 4096
newline:    .asciz "\n"
found_msg:  .asciz "Found: "
error_msg:  .asciz "Error opening directory\n"
empty_msg:  .asciz "Directory empty\n"

.section .text
.global _start

@ Syscall numbers (ARM EABI)
.equ SYS_EXIT, 1
.equ SYS_WRITE, 4
.equ SYS_OPEN, 5
.equ SYS_CLOSE, 6
.equ SYS_GETDENTS, 217  @ EABI value

@ Flags
.equ O_RDONLY, 0
.equ O_DIRECTORY, 0x10000

_start:
    @ Abrir diretório /mnt/
    ldr r0, =dir
    ldr r1, =#(O_RDONLY | O_DIRECTORY)
    mov r2, #0
    mov r7, #SYS_OPEN
    svc 0
    
    @ Verificar erro
    cmp r0, #0
    bgt open_ok
    
    @ Erro ao abrir
    ldr r1, =error_msg
    mov r2, #24
    bl write_str
    mov r0, #1
    b exit
    
open_ok:
    mov r8, r0          @ r8 = fd
    
read_dir:
    @ Ler entradas do diretório
    mov r0, r8
    ldr r1, =buffer
    mov r2, #4096
    mov r7, #SYS_GETDENTS
    svc 0
    
    @ Verificar resultado
    cmp r0, #0
    beq directory_empty
    blt read_error
    
    @ r0 = bytes lidos
    mov r9, r0          @ r9 = bytes lidos
    ldr r10, =buffer    @ r10 = ponteiro atual
    
process_entries:
    cmp r9, #0
    ble read_dir        @ Voltar para ler mais
    
    @ Extrair d_reclen (offset 8)
    ldrb r0, [r10, #8]   @ byte baixo
    ldrb r1, [r10, #9]   @ byte alto
    orr r2, r0, r1, lsl #8  @ r2 = d_reclen
    
    cmp r2, #0
    ble read_dir        @ reclen inválido
    
    @ Imprimir "Found: "
    ldr r1, =found_msg
    mov r2, #7
    bl write_str
    
    @ Imprimir nome do arquivo (offset 10)
    add r1, r10, #10
    mov r2, #0
    
count_loop:
    ldrb r0, [r1, r2]
    cmp r0, #0
    beq print_filename
    add r2, r2, #1
    b count_loop
    
print_filename:
    bl write_str
    
    @ Nova linha
    ldr r1, =newline
    mov r2, #1
    bl write_str
    
    @ Avançar para próxima entrada
    add r10, r10, r2    @ buffer += d_reclen
    sub r9, r9, r2      @ bytes_restantes -= d_reclen
    b process_entries

directory_empty:
    @ Diretório vazio
    ldr r1, =empty_msg
    mov r2, #16
    bl write_str
    b close_dir

read_error:
    @ Erro na leitura (não faz nada)

close_dir:
    @ Fechar diretório
    mov r0, r8
    mov r7, #SYS_CLOSE
    svc 0
    
    @ Sair com sucesso
    mov r0, #0
    b exit

@ Função: write_str(r1=string, r2=length)
write_str:
    push {r0, r7, lr}
    mov r0, #1          @ stdout
    mov r7, #SYS_WRITE
    svc 0
    pop {r0, r7, lr}
    bx lr

exit:
    mov r7, #SYS_EXIT
    svc 0