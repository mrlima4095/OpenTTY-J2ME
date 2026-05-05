@ libc_arm32.s - Biblioteca básica C para OpenTTY ELF ARM 32 emulator
@ Compatível com as syscalls do emulador
@ Compilar: arm-none-eabi-as -o libc.o libc_arm32.s
@ Linkar: arm-none-eabi-ld -Ttext=0x8000 -o programa libc.o programa.o

.syntax unified
.arm
.text

@ ============================================================
@ Constantes de syscall (do emulador)
@ ============================================================
.equ SYS_EXIT,      1
.equ SYS_READ,      3
.equ SYS_WRITE,     4
.equ SYS_OPEN,      5
.equ SYS_CLOSE,     6
.equ SYS_BRK,       45
.equ SYS_GETPID,    20

@ ============================================================
@ Flags de open
@ ============================================================
.equ O_RDONLY,      0
.equ O_WRONLY,      1
.equ O_RDWR,        2
.equ O_CREAT,       64
.equ O_TRUNC,       512
.equ O_APPEND,      1024

@ ============================================================
@ Seção de dados
@ ============================================================
.section .rodata
stdin_path:  .asciz "/dev/stdin"
stdout_path: .asciz "/dev/stdout"
stderr_path: .asciz "/dev/stderr"

.section .bss
.lcomm heap_start, 4      @ Ponteiro inicial do heap
.lcomm heap_end, 4        @ Ponteiro final do heap
.lcomm errno_var, 4       @ Variável errno

.section .text

@ ============================================================
@ _start - Entry point
@ ============================================================
.globl _start
_start:
    @ r0 = argc, r1 = argv, r2 = envp
    mov     r4, r0          @ salva argc
    mov     r5, r1          @ salva argv
    mov     r6, r2          @ salva envp
    
    @ Inicializa heap
    mov     r0, #0
    mov     r7, #SYS_BRK
    svc     #0
    ldr     r1, =heap_start
    str     r0, [r1]        @ heap_start = current break
    str     r0, [r1, #4]    @ heap_end = current break
    
    @ Chama main
    mov     r0, r4
    mov     r1, r5
    mov     r2, r6
    bl      main
    
    @ Exit com retorno de main
    mov     r1, r0
    mov     r0, #0
    mov     r7, #SYS_EXIT
    svc     #0
    
@ ============================================================
@ System call wrapper
@ ============================================================
.globl syscall
syscall:
    mov     r7, r0          @ syscall number
    mov     r0, r1          @ arg1
    mov     r1, r2          @ arg2
    mov     r2, r3          @ arg3
    ldr     r3, [sp, #0]    @ arg4 da stack
    ldr     r4, [sp, #4]    @ arg5 da stack
    ldr     r5, [sp, #8]    @ arg6 da stack
    svc     #0
    bx      lr

@ ============================================================
@ write(fd, buf, count)
@ ============================================================
.globl write
write:
    mov     r7, #SYS_WRITE
    svc     #0
    cmp     r0, #0
    movlt   r0, #-1
    bx      lr

@ ============================================================
@ write_string - Escreve string terminada em null
@ ============================================================
.globl write_string
write_string:
    push    {lr}
    mov     r2, r0          @ guarda fd
    mov     r1, r0          @ str
1:
    ldrb    r3, [r1], #1
    cmp     r3, #0
    bne     1b
    sub     r1, r1, #1
    sub     r0, r1, r0      @ r0 = length
    mov     r1, r2          @ str
    mov     r2, r0          @ count
    bl      write
    pop     {pc}
    
@ ============================================================
@ puts(str) - Escreve string + newline
@ ============================================================
.globl puts
puts:
    push    {lr}
    mov     r4, r0          @ salva str
    bl      write_string
    mov     r0, #'\n'
    mov     r1, #1
    bl      putchar
    pop     {pc}

@ ============================================================
@ putchar(c)
@ ============================================================
.globl putchar
putchar:
    push    {lr}
    mov     r4, r0
    sub     sp, sp, #4
    strb    r4, [sp, #0]
    mov     r0, #1          @ stdout
    mov     r1, sp
    mov     r2, #1
    bl      write
    add     sp, sp, #4
    pop     {pc}

@ ============================================================
@ read(fd, buf, count)
@ ============================================================
.globl read
read:
    mov     r7, #SYS_READ
    svc     #0
    cmp     r0, #0
    movlt   r0, #-1
    bx      lr

@ ============================================================
@ read_line - Lê linha do stdin (máx 255 bytes)
@ ============================================================
.globl read_line
read_line:
    push    {r4, lr}
    mov     r4, r0          @ buffer
    mov     r1, #0          @ pos
1:
    mov     r0, #0          @ stdin
    add     r1, r4, r1
    mov     r2, #1
    bl      read
    cmp     r0, #1
    bne     2f
    ldrb    r0, [r4, r1]
    cmp     r0, #'\n'
    beq     2f
    add     r1, r1, #1
    cmp     r1, #255
    blt     1b
2:
    mov     r0, #0
    strb    r0, [r4, r1]    @ null terminator
    pop     {r4, pc}

@ ============================================================
@ open(path, flags, mode)
@ ============================================================
.globl open
open:
    mov     r7, #SYS_OPEN
    svc     #0
    cmp     r0, #0
    movlt   r0, #-1
    bx      lr

@ ============================================================
@ close(fd)
@ ============================================================
.globl close
close:
    mov     r7, #SYS_CLOSE
    svc     #0
    bx      lr

@ ============================================================
@ sbrk(increment) - Aumenta heap
@ ============================================================
.globl sbrk
sbrk:
    push    {r4, lr}
    ldr     r3, =heap_end
    ldr     r4, [r3]        @ current heap_end
    mov     r1, r4
    add     r1, r1, r0      @ new break
    mov     r0, r1
    mov     r7, #SYS_BRK
    svc     #0
    cmp     r0, r1
    bne     1f
    str     r1, [r3]        @ atualiza heap_end
    mov     r0, r4          @ retorna ponteiro antigo
    pop     {r4, pc}
1:
    mov     r0, #-1
    pop     {r4, pc}

@ ============================================================
@ malloc(size)
@ ============================================================
.globl malloc
malloc:
    push    {lr}
    add     r0, r0, #4      @ espaço para header
    bl      sbrk
    cmp     r0, #-1
    beq     1f
    str     r0, [r0]        @ guarda tamanho no header
    add     r0, r0, #4      @ retorna após header
    pop     {pc}
1:
    mov     r0, #0
    pop     {pc}

@ ============================================================
@ free(ptr)
@ ============================================================
.globl free
free:
    @ No sbrk simples, não implementamos free
    @ Mas mantemos stub para compatibilidade
    bx      lr

@ ============================================================
@ getpid()
@ ============================================================
.globl getpid
getpid:
    mov     r7, #SYS_GETPID
    svc     #0
    bx      lr

@ ============================================================
@ exit(status)
@ ============================================================
.globl exit
exit:
    mov     r7, #SYS_EXIT
    svc     #0

@ ============================================================
@ strcpy(dest, src)
@ ============================================================
.globl strcpy
strcpy:
    mov     r2, r0
1:
    ldrb    r3, [r1], #1
    strb    r3, [r2], #1
    cmp     r3, #0
    bne     1b
    bx      lr

@ ============================================================
@ strlen(str)
@ ============================================================
.globl strlen
strlen:
    mov     r1, r0
1:
    ldrb    r2, [r1], #1
    cmp     r2, #0
    bne     1b
    sub     r0, r1, r0
    sub     r0, r0, #1
    bx      lr

@ ============================================================
@ strcmp(a, b)
@ ============================================================
.globl strcmp
strcmp:
1:
    ldrb    r2, [r0], #1
    ldrb    r3, [r1], #1
    cmp     r2, r3
    bne     2f
    cmp     r2, #0
    bne     1b
    mov     r0, #0
    bx      lr
2:
    sub     r0, r2, r3
    bx      lr

@ ============================================================
@ memset(ptr, value, size)
@ ============================================================
.globl memset
memset:
    mov     r3, r0
1:
    subs    r2, r2, #1
    strbpl  r1, [r3], #1
    bpl     1b
    bx      lr

@ ============================================================
@ memcpy(dest, src, size)
@ ============================================================
.globl memcpy
memcpy:
    mov     r3, r0
1:
    subs    r2, r2, #1
    ldrbpl  r4, [r1], #1
    strbpl  r4, [r3], #1
    bpl     1b
    bx      lr

@ ============================================================
@ printf - Formatação simplificada
@ ============================================================
.globl printf
printf:
    push    {r4, r5, r6, lr}
    mov     r4, r0          @ format string
    add     r5, sp, #16     @ argumentos (após 4 registradores)
    
1:
    ldrb    r0, [r4], #1
    cmp     r0, #0
    beq     4f
    
    cmp     r0, #'%'
    bne     3f
    
    @ Processa % format
    ldrb    r0, [r4], #1
    cmp     r0, #'s'
    beq     2f
    cmp     r0, #'d'
    beq     2f
    cmp     r0, #'c'
    beq     2f
    b       1b
    
2:
    @ Pega argumento
    ldr     r1, [r5], #4
    
    cmp     r0, #'s'
    moveq   r0, r1
    bleq    write_string
    
    cmp     r0, #'c'
    moveq   r0, r1
    bleq    putchar
    
    cmp     r0, #'d'
    beq     print_decimal
    
    b       1b
    
3:
    @ Caractere normal
    bl      putchar
    b       1b
    
4:
    pop     {r4, r5, r6, pc}

@ ============================================================
@ print_decimal - Imprime número decimal
@ ============================================================
print_decimal:
    push    {r4, r5, lr}
    mov     r4, r1          @ número a imprimir
    cmp     r4, #0
    bge     1f
    mov     r0, #'-'
    bl      putchar
    neg     r4, r4
    
1:
    mov     r5, #10
    sub     sp, sp, #12
    mov     r2, sp
    mov     r3, #10
    add     r2, r2, #9
    mov     r1, #0
    strb    r1, [r2], #1
    
2:
    mov     r0, r4
    bl      udiv10
    mov     r4, r0
    add     r1, r1, #'0'
    strb    r1, [r2], #1
    mov     r1, r3
    cmp     r4, #0
    bne     2b
    
3:
    subs    r2, r2, #1
    ldrb    r0, [r2]
    cmp     r0, #0
    beq     3b
    bl      putchar
    cmp     r2, sp
    bgt     3b
    
    add     sp, sp, #12
    pop     {r4, r5, pc}

@ ============================================================
@ udiv10 - Divide por 10 (resto em r1)
@ ============================================================
udiv10:
    mov     r1, #0
1:
    cmp     r0, #10
    blt     2f
    sub     r0, r0, #10
    add     r1, r1, #1
    b       1b
2:
    mov     pc, lr

@ ============================================================
@ perror(str)
@ ============================================================
.globl perror
perror:
    push    {lr}
    bl      write_string
    mov     r0, #':'
    bl      putchar
    mov     r0, #' '
    bl      putchar
    ldr     r0, =errno_strings
    ldr     r1, =errno_var
    ldr     r2, [r1]
    lsl     r2, r2, #2
    ldr     r0, [r0, r2]
    bl      write_string
    pop     {pc}

errno_strings:
    .word   err_ok
    .word   err_eperm
    .word   err_enoent
    .word   err_esrch
    .word   err_eintr
    .word   err_eio
err_ok:     .asciz "Success"
err_eperm:  .asciz "Operation not permitted"
err_enoent: .asciz "No such file or directory"
err_esrch:  .asciz "No such process"
err_eintr:  .asciz "Interrupted system call"
err_eio:    .asciz "I/O error"

@ ============================================================
@ Exemplo de programa para testar
@ ============================================================
.section .rodata
hello_msg:  .asciz "Hello, World!\n"
prompt_msg: .asciz "Enter your name: "
reply_msg:  .asciz "Hello, "
test_file:  .asciz "/tmp/test.txt"
write_msg:  .asciz "Writing to file test!\n"

.section .bss
name_buf:   .space 256
file_buf:   .space 1024

.section .text
.globl main
main:
    push    {lr}
    
    @ Teste 1: printf
    ldr     r0, =hello_msg
    bl      write_string
    
    @ Teste 2: puts
    ldr     r0, =prompt_msg
    bl      write_string
    
    @ Teste 3: read_line
    ldr     r0, =name_buf
    bl      read_line
    
    @ Teste 4: print nome
    ldr     r0, =reply_msg
    bl      write_string
    ldr     r0, =name_buf
    bl      write_string
    mov     r0, #'\n'
    bl      putchar
    
    @ Teste 5: arquivo
    ldr     r0, =test_file
    mov     r1, #O_CREAT | O_WRONLY | O_TRUNC
    mov     r2, #0644
    bl      open
    
    cmp     r0, #0
    blt     file_error
    mov     r4, r0          @ salva fd
    
    ldr     r1, =write_msg
    ldr     r0, =write_msg
    bl      strlen
    mov     r2, r0
    mov     r0, r4
    ldr     r1, =write_msg
    bl      write
    
    mov     r0, r4
    bl      close
    
    @ Teste 6: leitura do arquivo
    ldr     r0, =test_file
    mov     r1, #O_RDONLY
    mov     r2, #0
    bl      open
    
    mov     r4, r0
    ldr     r1, =file_buf
    mov     r2, #1024
    bl      read
    
    mov     r0, r4
    bl      close
    
    ldr     r0, =file_buf
    bl      write_string
    
    @ Teste 7: malloc
    mov     r0, #100
    bl      malloc
    cmp     r0, #0
    beq     malloc_error
    mov     r4, r0
    
    @ Usa memória alocada
    mov     r0, #'*'
    mov     r1, r4
    mov     r2, #100
    bl      memset
    
    ldr     r0, =file_buf
    mov     r1, r4
    mov     r2, #100
    bl      memcpy
    
    @ Teste 8: getpid
    bl      getpid
    mov     r1, r0
    ldr     r0, =pid_msg
    bl      printf
    
    mov     r0, #0          @ return 0
    pop     {pc}

file_error:
    ldr     r0, =test_file
    bl      perror
    mov     r0, #1
    pop     {pc}
    
malloc_error:
    ldr     r0, =malloc_err
    bl      write_string
    mov     r0, #1
    pop     {pc}

.section .rodata
pid_msg:    .asciz "PID: %d\n"
malloc_err: .asciz "malloc failed!\n"