@ direct_syscalls.s
.section .data
dir:    .asciz "/"
buffer: .space 1024
nl:     .asciz "\n"

.section .text
.global _start

_start:
    @ open("/", O_RDONLY|O_DIRECTORY)
    ldr r0, =dir
    mov r1, #0x10000   @ O_DIRECTORY
    mov r7, #5         @ SYS_OPEN
    svc 0
    
    mov r4, r0         @ salvar fd
    
    @ getdents(fd, buffer, 1024)
    mov r0, r4
    ldr r1, =buffer
    mov r2, #1024
    mov r7, #141       @ SYS_GETDENTS
    svc 0
    
    @ Se retornou > 0, escrever algo
    cmp r0, #0
    ble end
    
    @ write(1, "OK\n", 3)
    mov r0, #1
    ldr r1, =nl
    mov r2, #1
    mov r7, #4
    svc 0
    
end:
    @ exit(0)
    mov r0, #0
    mov r7, #1
    svc 0