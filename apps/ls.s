.data
dir_path:   .asciz "/tmp/"
buffer:     .space 512

.text
.global _start

_start:
    @ Abrir /tmp/
    ldr r0, =dir_path
    mov r1, #0
    mov r7, #5
    swi 0
    
    @ Verificar erro
    cmp r0, #0
    blt _exit_error
    
    mov r4, r0  @ fd
    
read_dir:
    @ getdents
    mov r0, r4
    ldr r1, =buffer
    mov r2, #512
    mov r7, #217
    swi 0
    
    cmp r0, #0
    beq close_and_exit
    blt _exit_error
    
    @ Percorrer entradas
    mov r5, r0  @ bytes lidos
    mov r6, #0  @ offset
    
next_entry:
    cmp r6, r5
    bge read_dir
    
    ldr r8, =buffer
    add r8, r8, r6
    
    @ Obter d_reclen
    ldrb r9, [r8, #8]
    ldrb r10, [r8, #9]
    orr r9, r9, r10, lsl #8
    
    @ Imprimir nome (offset 10)
    add r0, r8, #10
    
    @ Calcular tamanho do nome
    mov r1, r0
    mov r2, #0
calc_len:
    ldrb r3, [r1, r2]
    cmp r3, #0
    beq print_it
    add r2, r2, #1
    b calc_len
    
print_it:
    mov r0, #1  @ stdout
    mov r7, #4  @ SYS_WRITE
    swi 0
    
    @ Imprimir nova linha
    ldr r0, =nl
    mov r1, #1
    mov r7, #4
    swi 0
    
    add r6, r6, r9
    b next_entry

close_and_exit:
    @ Fechar diretório
    mov r0, r4
    mov r7, #6
    swi 0
    
    @ Sair
    mov r0, #0
    mov r7, #1
    swi 0

_exit_error:
    mov r0, #1
    mov r7, #1
    swi 0

.data
nl: .asciz "\n"