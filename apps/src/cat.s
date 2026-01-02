.syntax unified
.arm

.data
filename:
    .ascii "/home/OpenRMS\0"
buffer:
    .space 1024         // buffer de leitura

.text
.global _start

_start:
    // open("/home/OpenRMS", O_RDONLY)
    ldr r0, =filename
    mov r1, #0          // O_RDONLY
    mov r2, #0          // modo (não usado)
    mov r7, #5          // syscall open
    swi #0
    
    cmp r0, #0
    blt error           // se erro
    
    mov r4, r0          // salvar file descriptor
    
read_loop:
    // read(fd, buffer, 1024)
    mov r0, r4
    ldr r1, =buffer
    mov r2, #1024
    mov r7, #3          // syscall read
    swi #0
    
    cmp r0, #0
    ble close_file      // se EOF ou erro
    
    // write(1, buffer, bytes_lidos)
    mov r2, r0          // bytes lidos
    mov r0, #1          // stdout
    ldr r1, =buffer
    mov r7, #4          // syscall write
    swi #0
    
    b read_loop

close_file:
    // close(fd)
    mov r0, r4
    mov r7, #6          // syscall close
    swi #0
    
    // exit(0)
    mov r0, #0
    mov r7, #1
    swi #0

error:
    // Em caso de erro, apenas sair
    mov r0, #1
    mov r7, #1
    swi #0