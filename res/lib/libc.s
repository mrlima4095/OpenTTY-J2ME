# libc.s (RISC-V RV32IM) - Biblioteca C para o emulador ELF do OpenTTY
#
# Estas funcoes NAO implementam a logica em asm: cada uma e um wrapper de 3
# instrucoes (li a7, #LIB_*; ecall; ret) que entrega o trabalho ao emulador
# (src/ELF.java -> handleLibraryCall). O emulador conhece strings, printf,
# mem*, o alocador de heap, atoi/abs, divisao AEABI, etc.
#
# LINKAR:  ./build-elf.sh demo.c -stdlib
# (o -stdlib linka este arquivo antes do programa; o programa define main;
#  o _start aqui le argc/argv/envp da stack do CRT do emulador e chama main.)
#
# Constantes de syscall do emulador (src/ELF.java):
#   ecall com a7 = N (N != 0) -> handleSyscall(N), ou if N >= LIB_BASE
#   handleLibraryCall(N - LIB_BASE).
#
# ABI RISC-V: args a0-a7 (x10-x17), retorno a0, syscall number em a7.
.equ LIB_BASE,         1000
.equ LIB_STRLEN,       LIB_BASE + 1
.equ LIB_STRCPY,       LIB_BASE + 2
.equ LIB_STRCMP,       LIB_BASE + 3
.equ LIB_STRNCMP,      LIB_BASE + 4
.equ LIB_STRCAT,       LIB_BASE + 5
.equ LIB_STRCHR,       LIB_BASE + 6
.equ LIB_STRDUP,       LIB_BASE + 7
.equ LIB_STRNCPY,      LIB_BASE + 8
.equ LIB_STRNCAT,      LIB_BASE + 9
.equ LIB_MEMCPY,       LIB_BASE + 10
.equ LIB_MEMMOVE,      LIB_BASE + 11
.equ LIB_MEMSET,       LIB_BASE + 12
.equ LIB_MEMCMP,       LIB_BASE + 13
.equ LIB_MEMCHR,       LIB_BASE + 14
.equ LIB_ATOI,         LIB_BASE + 15
.equ LIB_ABS,          LIB_BASE + 16
.equ LIB_PUTCHAR,      LIB_BASE + 17
.equ LIB_PUTS,         LIB_BASE + 18
.equ LIB_PRINTF,       LIB_BASE + 19
.equ LIB_SPRINTF,      LIB_BASE + 20
.equ LIB_SNPRINTF,     LIB_BASE + 21
.equ LIB_WRITE_STRING, LIB_BASE + 22
.equ LIB_MALLOC,       LIB_BASE + 23
.equ LIB_CALLOC,       LIB_BASE + 24
.equ LIB_REALLOC,      LIB_BASE + 25
.equ LIB_FREE,         LIB_BASE + 26
.equ LIB_TOUPPER,      LIB_BASE + 27
.equ LIB_TOLOWER,      LIB_BASE + 28
.equ LIB_GETPID,       LIB_BASE + 29
.equ LIB_AEABI_UIDIV,      LIB_BASE + 30
.equ LIB_AEABI_IDIV,       LIB_BASE + 31
.equ LIB_AEABI_UIDIVMOD,   LIB_BASE + 32
.equ LIB_AEABI_IDIVMOD,    LIB_BASE + 33
.equ LIB_AEABI_ULDIVMOD,   LIB_BASE + 34
.equ LIB_AEABI_LDIVMOD,    LIB_BASE + 35
.equ LIB_AEABI_MEMCLR,     LIB_BASE + 36
.equ LIB_AEABI_MEMCPY,     LIB_BASE + 37
.equ LIB_AEABI_MEMSET,     LIB_BASE + 38

# ============================================================
# _start - Entry point (compativel com o CRT do emulador):
#   [sp]     = argc
#   [sp,4]   = argv[] (ponteiros), NULL no fim
#   apos argv vem envp[]
# ============================================================
.text

.globl _start
_start:
    lw      a0, 0(sp)            # argc
    addi    a1, sp, 4            # argv
    slli    a2, a0, 2            # argv + argc*4
    add     a2, a1, a2           # aponta pro NULL do argv
    addi    a2, a2, 4            # envp (apos o NULL do argv)
    jal     main
    # main retornou; exit(r0)
    li      a7, 1                # SYS_EXIT
    ecall

# ============================================================
# Gerador de wrapper: li a7, #LIB_*; ecall; ret
# ============================================================
.macro LIBWRAP id, name
    .globl \name
    .type \name, %function
\name:
    li      a7, \id
    ecall
    ret
.endm

# ---- strings -------------------------------------------------
LIBWRAP LIB_STRLEN,      strlen
LIBWRAP LIB_STRCPY,      strcpy
LIBWRAP LIB_STRNCPY,     strncpy
LIBWRAP LIB_STRCMP,      strcmp
LIBWRAP LIB_STRNCMP,     strncmp
LIBWRAP LIB_STRCAT,      strcat
LIBWRAP LIB_STRNCAT,     strncat
LIBWRAP LIB_STRCHR,      strchr
LIBWRAP LIB_STRDUP,      strdup
LIBWRAP LIB_ATOI,        atoi
LIBWRAP LIB_ABS,         abs
LIBWRAP LIB_TOUPPER,     toupper
LIBWRAP LIB_TOLOWER,     tolower

# ---- memoria -------------------------------------------------
LIBWRAP LIB_MEMCPY,      memcpy
LIBWRAP LIB_MEMMOVE,     memmove
LIBWRAP LIB_MEMSET,      memset
LIBWRAP LIB_MEMCMP,      memcmp
LIBWRAP LIB_MEMCHR,      memchr

# ---- io ------------------------------------------------------
LIBWRAP LIB_PUTCHAR,     putchar
LIBWRAP LIB_PUTS,        puts
LIBWRAP LIB_PRINTF,      printf
LIBWRAP LIB_SPRINTF,     sprintf
LIBWRAP LIB_SNPRINTF,    snprintf

# ---- heap ----------------------------------------------------
LIBWRAP LIB_MALLOC,      malloc
LIBWRAP LIB_CALLOC,      calloc
LIBWRAP LIB_REALLOC,     realloc
LIBWRAP LIB_FREE,        free

# ---- misc ----------------------------------------------------
LIBWRAP LIB_GETPID,      getpid

# ---- AEABI (mantidos para compat; o emulador implementa a divisao) ---
LIBWRAP LIB_AEABI_UIDIV,      __aeabi_uidiv
LIBWRAP LIB_AEABI_IDIV,       __aeabi_idiv
LIBWRAP LIB_AEABI_UIDIVMOD,   __aeabi_uidivmod
LIBWRAP LIB_AEABI_IDIVMOD,    __aeabi_idivmod
LIBWRAP LIB_AEABI_ULDIVMOD,   __aeabi_uldivmod
LIBWRAP LIB_AEABI_LDIVMOD,    __aeabi_ldivmod
LIBWRAP LIB_AEABI_MEMCLR,     __aeabi_memclr
LIBWRAP LIB_AEABI_MEMCPY,     __aeabi_memcpy
LIBWRAP LIB_AEABI_MEMSET,     __aeabi_memset

# variantes alinhadas/tipadas usadas pelos builtins do gcc ------
LIBWRAP LIB_AEABI_MEMCLR,     __aeabi_memclr4
LIBWRAP LIB_AEABI_MEMCLR,     __aeabi_memclr8
LIBWRAP LIB_AEABI_MEMCPY,     __aeabi_memcpy4
LIBWRAP LIB_AEABI_MEMCPY,     __aeabi_memcpy8
LIBWRAP LIB_AEABI_MEMSET,     __aeabi_memset4
LIBWRAP LIB_AEABI_MEMSET,     __aeabi_memset8
LIBWRAP LIB_MEMMOVE,          __aeabi_memmove
LIBWRAP LIB_MEMMOVE,          __aeabi_memmove4
LIBWRAP LIB_MEMMOVE,          __aeabi_memmove8

# ---- RISC-V 64-bit helpers (gerados pelo compilador) ------------------
# clang/gcc RV32IM usa __muldi3/__divdi3/__moddi3/__udivdi3/__umoddi3 para
# aritmetica de long long. Layout dos args (igual ao AEABI do emulador):
#   a0:a1 = dividendo/destino, a2:a3 = divisor/fonte.
# LIB_*LDIVMOD retorna quociente em a0:a1 E resto em a2:a3 (ver libcLdivmod
# em src/ELF.java); as funcoes abaixo selecionam a metade certa.
.globl __udivdi3
.type __udivdi3, %function
__udivdi3:
    li      a7, LIB_AEABI_ULDIVMOD
    ecall
    ret

.globl __divdi3
.type __divdi3, %function
__divdi3:
    li      a7, LIB_AEABI_LDIVMOD
    ecall
    ret

.globl __umoddi3
.type __umoddi3, %function
__umoddi3:
    li      a7, LIB_AEABI_ULDIVMOD
    ecall
    mv      a0, a2               # resto a2:a3 -> a0:a1
    mv      a1, a3
    ret

.globl __moddi3
.type __moddi3, %function
__moddi3:
    li      a7, LIB_AEABI_LDIVMOD
    ecall
    mv      a0, a2               # resto a2:a3 -> a0:a1
    mv      a1, a3
    ret

# __muldi3(a0:a1, a2:a3) -> a0:a1 (32x32 -> 64 via MUL/MULHU, sem lmul)
.globl __muldi3
.type __muldi3, %function
__muldi3:
    mul     t0, a0, a2           # lo32 de Alo*Blo
    mulhu   t3, a0, a2           # carry de Alo*Blo (bit 32+)
    mul     t1, a1, a2           # Ahi*Blo (lo32)
    mul     t2, a0, a3           # Alo*Bhi (lo32)
    add     t1, t1, t2
    add     t1, t1, t3
    mv      a0, t0
    mv      a1, t1
    ret

# ---- saida ---------------------------------------------------
.globl exit
exit:
    li      a7, 1                # SYS_EXIT (nao retorna; a0 = status)
    ecall

.globl _exit
_exit:
    li      a7, 1
    ecall

.globl abort
abort:
    li      a7, 1
    ecall

# ---- wrappers de syscall uteis ---------------------------------
.globl write
write:
    li      a7, 4                # SYS_WRITE
    ecall
    ret

.globl read
read:
    li      a7, 3                # SYS_READ
    ecall
    ret

.globl open
open:
    li      a7, 5                # SYS_OPEN
    ecall
    ret

.globl close
close:
    li      a7, 6                # SYS_CLOSE
    ecall
    ret

.globl brk
brk:
    li      a7, 45               # SYS_BRK
    ecall
    ret

.end