# lib32.s (RISC-V RV32IM) - Biblioteca basica C para o emulador ELF do OpenTTY
# Port do antigo libc_arm32.s. Compativel com as syscalls do emulador.
# Compilar: riscv64-unknown-elf-as -march=rv32im -mabi=ilp32 -o libc.o lib32.s
# Linkar:  riscv64-unknown-elf-ld -m elf32lriscv -Ttext=0x8000 -o programa libc.o programa.o
#
# ABI RISC-V: args a0-a7 (x10-x17), retorno a0, syscall number em a7, ecall.

# ============================================================
# Constantes de syscall (do emulador)
# ============================================================
.equ SYS_EXIT,      1
.equ SYS_READ,      3
.equ SYS_WRITE,     4
.equ SYS_OPEN,      5
.equ SYS_CLOSE,     6
.equ SYS_BRK,       45
.equ SYS_GETPID,    20

# ============================================================
# Flags de open
# ============================================================
.equ O_RDONLY,      0
.equ O_WRONLY,      1
.equ O_RDWR,        2
.equ O_CREAT,       64
.equ O_TRUNC,       512
.equ O_APPEND,      1024

# ============================================================
# Secao de dados
# ============================================================
.section .rodata
stdin_path:  .asciz "/dev/stdin"
stdout_path: .asciz "/dev/stdout"
stderr_path: .asciz "/dev/stderr"

.section .bss
.lcomm heap_start, 4      # Ponteiro inicial do heap
.lcomm heap_end, 4        # Ponteiro final do heap
.lcomm errno_var, 4       # Variavel errno

.section .text

# ============================================================
# _start - Entry point
#   [sp]=argc, [sp+4]=argv (ponteiros, NULL no fim), envp apos argv
# ============================================================
.globl _start
_start:
    lw      a0, 0(sp)          # argc
    addi    a1, sp, 4          # argv
    slli    a2, a0, 2          # argv + argc*4
    add     a2, a1, a2         # aponta pro NULL do argv
    addi    a2, a2, 4          # envp (apos o NULL do argv)

    # Inicializa heap
    li      a0, 0
    li      a7, SYS_BRK
    ecall
    la      t0, heap_start
    sw      a0, 0(t0)          # heap_start = current break
    sw      a0, 4(t0)          # heap_end = current break

    # Chama main(argc, argv, envp)
    lw      a0, 0(sp)          # argc
    addi    a1, sp, 4          # argv
    jal     main

    # Exit com o retorno de main
    li      a7, SYS_EXIT
    ecall

# ============================================================
# System call wrapper: syscall(num, a1, a2, a3, arg4..arg6 na stack)
# ============================================================
.globl syscall
syscall:
    mv      a7, a0             # syscall number
    mv      a0, a1             # arg1
    mv      a1, a2             # arg2
    mv      a2, a3             # arg3
    lw      a3, 0(sp)          # arg4 da stack
    lw      a4, 4(sp)          # arg5 da stack
    lw      a5, 8(sp)          # arg6 da stack
    ecall
    ret

# ============================================================
# write(fd, buf, count)
# ============================================================
.globl write
write:
    li      a7, SYS_WRITE
    ecall
    bge     a0, zero, 1f
    li      a0, -1
1:
    ret

# ============================================================
# write_string - Escreve string terminada em null no stdout
# ============================================================
.globl write_string
write_string:
    addi    sp, sp, -16
    sw      ra, 12(sp)
    sw      s1, 8(sp)
    mv      s1, a0             # inicio da string
    mv      a1, a0
1:
    lbu     t0, 0(a1)
    addi    a1, a1, 1
    bnez    t0, 1b
    addi    a1, a1, -1
    sub     a2, a1, s1         # count = fim - inicio
    li      a0, 1              # stdout
    mv      a1, s1
    jal     write
    lw      s1, 8(sp)
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret

# ============================================================
# puts(str) - Escreve string + newline
# ============================================================
.globl puts
puts:
    addi    sp, sp, -16
    sw      ra, 12(sp)
    jal     write_string
    li      a0, 10             # '\n'
    jal     putchar
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret

# ============================================================
# putchar(c)
# ============================================================
.globl putchar
putchar:
    addi    sp, sp, -16
    sw      ra, 12(sp)
    sw      a0, 8(sp)          # c na stack
    li      a0, 1              # stdout
    addi    a1, sp, 8
    li      a2, 1
    jal     write
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret

# ============================================================
# read(fd, buf, count)
# ============================================================
.globl read
read:
    li      a7, SYS_READ
    ecall
    bge     a0, zero, 1f
    li      a0, -1
1:
    ret

# ============================================================
# read_line - Le linha do stdin (max 255 bytes)
# ============================================================
.globl read_line
read_line:
    addi    sp, sp, -32
    sw      ra, 28(sp)
    sw      s1, 24(sp)
    sw      s2, 20(sp)
    mv      s1, a0             # buffer
    li      s2, 0              # pos
1:
    li      a0, 0              # stdin
    add     a1, s1, s2
    li      a2, 1
    jal     read
    li      t0, 1
    bne     a0, t0, 2f
    add     t0, s1, s2
    lbu     t0, 0(t0)
    li      t1, 10             # '\n'
    beq     t0, t1, 2f
    addi    s2, s2, 1
    li      t1, 255
    blt     s2, t1, 1b
2:
    li      t0, 0
    add     t1, s1, s2
    sb      t0, 0(t1)          # null terminator
    lw      s2, 20(sp)
    lw      s1, 24(sp)
    lw      ra, 28(sp)
    addi    sp, sp, 32
    ret

# ============================================================
# open(path, flags, mode)
# ============================================================
.globl open
open:
    li      a7, SYS_OPEN
    ecall
    bge     a0, zero, 1f
    li      a0, -1
1:
    ret

# ============================================================
# close(fd)
# ============================================================
.globl close
close:
    li      a7, SYS_CLOSE
    ecall
    ret

# ============================================================
# sbrk(increment) - Aumenta o heap
#   O handleBrk do emulador arredonda o break pra cima (4K); por isso
#   comparamos o break real devolvido (>= pedido) e guardamos ele.
# ============================================================
.globl sbrk
sbrk:
    addi    sp, sp, -16
    sw      ra, 12(sp)
    sw      s1, 8(sp)
    la      t0, heap_end
    lw      s1, 0(t0)          # heap_end atual
    add     t1, s1, a0         # novo break
    mv      a0, t1
    li      a7, SYS_BRK
    ecall
    blt     a0, t1, 1f          # kernel nao alcancou o pedido -> falhou
    la      t0, heap_end
    sw      a0, 0(t0)          # guarda o break real (arredondado)
    mv      a0, s1             # retorna o break antigo
    lw      s1, 8(sp)
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret
1:
    li      a0, -1
    lw      s1, 8(sp)
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret

# ============================================================
# malloc(size)
# ============================================================
.globl malloc
malloc:
    addi    sp, sp, -16
    sw      ra, 12(sp)
    addi    a0, a0, 4          # espaco para header
    jal     sbrk
    li      t0, -1
    beq     a0, t0, 1f
    sw      a0, 0(a0)          # guarda tamanho no header
    addi    a0, a0, 4          # retorna apos o header
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret
1:
    li      a0, 0
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret

# ============================================================
# free(ptr)
# ============================================================
.globl free
free:
    # No sbrk simples, nao implementamos free
    # Mas mantemos stub para compatibilidade
    ret

# ============================================================
# getpid()
# ============================================================
.globl getpid
getpid:
    li      a7, SYS_GETPID
    ecall
    ret

# ============================================================
# exit(status)
# ============================================================
.globl exit
exit:
    li      a7, SYS_EXIT
    ecall

# ============================================================
# strcpy(dest, src)
# ============================================================
.globl strcpy
strcpy:
    mv      t0, a0
1:
    lbu     t1, 0(a1)
    addi    a1, a1, 1
    sb      t1, 0(t0)
    addi    t0, t0, 1
    bnez    t1, 1b
    ret

# ============================================================
# strlen(str)
# ============================================================
.globl strlen
strlen:
    mv      t0, a0
1:
    lbu     t1, 0(t0)
    addi    t0, t0, 1
    bnez    t1, 1b
    sub     a0, t0, a0
    addi    a0, a0, -1
    ret

# ============================================================
# strcmp(a, b)
# ============================================================
.globl strcmp
strcmp:
1:
    lbu     t0, 0(a0)
    lbu     t1, 0(a1)
    addi    a0, a0, 1
    addi    a1, a1, 1
    bne     t0, t1, 2f
    bnez    t0, 1b
    li      a0, 0
    ret
2:
    sub     a0, t0, t1
    ret

# ============================================================
# memset(ptr, value, size)
# ============================================================
.globl memset
memset:
    mv      t0, a0
1:
    beqz    a2, 2f
    sb      a1, 0(t0)
    addi    t0, t0, 1
    addi    a2, a2, -1
    j       1b
2:
    ret

# ============================================================
# memcpy(dest, src, size)
# ============================================================
.globl memcpy
memcpy:
    mv      t0, a0
1:
    beqz    a2, 2f
    lbu     t1, 0(a1)
    sb      t1, 0(t0)
    addi    a1, a1, 1
    addi    t0, t0, 1
    addi    a2, a2, -1
    j       1b
2:
    ret

# ============================================================
# printf - Formatacao simplificada (%s, %d, %c)
#   Varargs RISC-V: a1..a7 em registradores, depois na stack.
#   Copiamos a1..a7 num buffer local e andamos com um ponteiro.
# ============================================================
.globl printf
printf:
    addi    sp, sp, -80
    sw      ra, 76(sp)
    sw      s0, 72(sp)
    sw      s1, 68(sp)
    sw      s2, 64(sp)
    sw      a1, 0(sp)
    sw      a2, 4(sp)
    sw      a3, 8(sp)
    sw      a4, 12(sp)
    sw      a5, 16(sp)
    sw      a6, 20(sp)
    sw      a7, 24(sp)
    mv      s0, a0             # format string
    addi    s1, sp, 0          # ponteiro de args
    li      s2, 7              # args ainda em registradores

1:
    lbu     t0, 0(s0)
    addi    s0, s0, 1
    beqz    t0, 9f

    li      t1, 37             # '%'
    bne     t0, t1, 3f

    # Processa % format
    lbu     t0, 0(s0)
    addi    s0, s0, 1
    li      t1, 115            # 's'
    beq     t0, t1, 4f
    li      t1, 100            # 'd'
    beq     t0, t1, 5f
    li      t1, 99             # 'c'
    beq     t0, t1, 6f
    j       1b

4:  # %s
    bnez    s2, 7f
    addi    s1, sp, 80         # esgotou os regs: le da stack do chamador
7:
    lw      a0, 0(s1)
    addi    s1, s1, 4
    addi    s2, s2, -1
    jal     write_string
    j       1b

5:  # %d
    bnez    s2, 7f
    addi    s1, sp, 80
7:
    lw      a0, 0(s1)
    addi    s1, s1, 4
    addi    s2, s2, -1
    jal     print_decimal
    j       1b

6:  # %c
    bnez    s2, 7f
    addi    s1, sp, 80
7:
    lw      a0, 0(s1)
    addi    s1, s1, 4
    addi    s2, s2, -1
    jal     putchar
    j       1b

3:
    # Caractere normal
    mv      a0, t0
    jal     putchar
    j       1b

9:
    lw      s2, 64(sp)
    lw      s1, 68(sp)
    lw      s0, 72(sp)
    lw      ra, 76(sp)
    addi    sp, sp, 80
    ret

# ============================================================
# print_decimal - Imprime numero decimal (entrada em a0)
# ============================================================
print_decimal:
    addi    sp, sp, -48
    sw      ra, 44(sp)
    sw      s1, 40(sp)
    sw      s2, 36(sp)
    mv      s1, a0             # numero a imprimir
    bge     s1, zero, 1f
    li      a0, 45             # '-'
    jal     putchar
    neg     s1, s1

1:
    addi    s2, sp, 26         # buffer de digitos (terminator no fundo)
    li      t0, 0
    sb      t0, 0(s2)

2:
    mv      a0, s1
    jal     udiv10
    mv      s1, a0             # quociente
    addi    t0, a1, 48         # '0' + resto
    addi    s2, s2, -1
    sb      t0, 0(s2)
    bnez    s1, 2b

3:
    lbu     a0, 0(s2)
    beqz    a0, 9f
    jal     putchar
    addi    s2, s2, 1
    j       3b

9:
    lw      s2, 36(sp)
    lw      s1, 40(sp)
    lw      ra, 44(sp)
    addi    sp, sp, 48
    ret

# ============================================================
# udiv10 - Divide por 10 (quociente em a0, resto em a1)
# ============================================================
udiv10:
    li      a1, 0            # quociente = 0
    li      t0, 10
1:
    blt     a0, t0, 2f       # a0 < 10 -> a0 e' o resto
    addi    a0, a0, -10
    addi    a1, a1, 1
    j       1b
2:
    mv      t0, a1           # t0 = quociente
    mv      a1, a0           # a1 = resto
    mv      a0, t0           # a0 = quociente
    ret

# ============================================================
# perror(str)
# ============================================================
.globl perror
perror:
    addi    sp, sp, -16
    sw      ra, 12(sp)
    jal     write_string
    li      a0, ':'            # 58
    jal     putchar
    li      a0, ' '            # 32
    jal     putchar
    la      t0, errno_var
    lw      t1, 0(t0)
    la      t0, errno_strings
    slli    t1, t1, 2
    add     t0, t0, t1
    lw      a0, 0(t0)
    jal     write_string
    lw      ra, 12(sp)
    addi    sp, sp, 16
    ret

.section .rodata
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

# ============================================================
# Exemplo de programa para testar
# ============================================================
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
    addi    sp, sp, -48
    sw      ra, 44(sp)
    sw      s0, 40(sp)
    sw      s1, 36(sp)

    # Teste 1: printf
    la      a0, hello_msg
    jal     write_string

    # Teste 2: prompt
    la      a0, prompt_msg
    jal     write_string

    # Teste 3: read_line
    la      a0, name_buf
    jal     read_line

    # Teste 4: print do nome
    la      a0, reply_msg
    jal     write_string
    la      a0, name_buf
    jal     write_string
    li      a0, 10             # '\n'
    jal     putchar

    # Teste 5: arquivo
    la      a0, test_file
    li      a1, 577            # O_CREAT | O_WRONLY | O_TRUNC
    li      a2, 0644
    jal     open
    blt     a0, zero, file_error
    mv      s0, a0             # salva fd

    la      s1, write_msg
    mv      a0, s1
    jal     strlen
    mv      a2, a0             # count
    mv      a0, s0             # fd
    mv      a1, s1             # buf
    jal     write

    mv      a0, s0
    jal     close

    # Teste 6: leitura do arquivo
    la      a0, test_file
    li      a1, O_RDONLY
    li      a2, 0
    jal     open
    mv      s0, a0
    la      a1, file_buf
    li      a2, 1024
    mv      a0, s0
    jal     read
    mv      a0, s0
    jal     close

    la      a0, file_buf
    jal     write_string

    # Teste 7: malloc
    li      a0, 100
    jal     malloc
    beqz    a0, malloc_error
    mv      s0, a0

    # Usa memoria alocada
    li      a0, 42             # '*'
    mv      a1, s0
    li      a2, 100
    jal     memset

    la      a0, file_buf
    mv      a1, s0
    li      a2, 100
    jal     memcpy

    # Teste 8: getpid
    jal     getpid
    mv      a1, a0
    la      a0, pid_msg
    jal     printf

    li      a0, 0              # return 0
    lw      s1, 36(sp)
    lw      s0, 40(sp)
    lw      ra, 44(sp)
    addi    sp, sp, 48
    ret

file_error:
    la      a0, test_file
    jal     perror
    li      a0, 1
    lw      s1, 36(sp)
    lw      s0, 40(sp)
    lw      ra, 44(sp)
    addi    sp, sp, 48
    ret

malloc_error:
    la      a0, malloc_err
    jal     write_string
    li      a0, 1
    lw      s1, 36(sp)
    lw      s0, 40(sp)
    lw      ra, 44(sp)
    addi    sp, sp, 48
    ret

.section .rodata
pid_msg:    .asciz "PID: %d\n"
malloc_err: .asciz "malloc failed!\n"