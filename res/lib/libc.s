@ libc.s - Bibilhoteca C para o emulador ELF ARM 32 do OpenTTY
@
@ Estas funcoes NAO implementam a logica em asm: cada uma e um wrapper de
@ 2 instrucoes (svc #LIB_*; bx lr) que entrega o trabalho ao emulador
@ (src/ELF.java -> handleLibraryCall). O emulador conhece strings, printf,
@ mem*, o alocador de heap, atoi/abs, divisao AEABI, etc.
@
@ LINKAR:  ./build-elf.sh demo.c -stdlib
@ (o -stdlib linka este arquivo antes do programa; o programa define main;
@  o _start aqui le argc/argv/envp da stack do CRT do emulador e chama main.)
@
@ Constantes de syscall do emulador (src/ELF.java):
@   svc #N  (N != 0) -> handleSyscall(N), ou handleLibraryCall(N-1000) se N>=1000
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

@ ============================================================
@ _start - Entry point (compatible com o CRT do emulador):
@   [sp]     = argc
@   [sp,#4]  = argv[] (ponteiros), NULL no fim
@   apos argv vem envp[]
@ ============================================================
.syntax unified
.arm
.text

.globl _start
_start:
    ldr     r0, [sp]            @ argc
    add     r1, sp, #4          @ argv
    add     r2, r1, r0, lsl #2  @ argv + argc
    add     r2, r2, #4          @ envp (apos o NULL do argv)
    bl      main
    @ main retornou; exit(r0)
    svc     #1                  @ SYS_EXIT

@ ============================================================
@ Gerador de wrapper: svc #LIB_*; bx lr
@ ============================================================
.macro LIBWRAP id, name
    .globl \name
    .type \name, %function
\name:
    svc     #\id
    bx      lr
.endm

@ ---- strings -------------------------------------------------
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

@ ---- memoria -------------------------------------------------
LIBWRAP LIB_MEMCPY,      memcpy
LIBWRAP LIB_MEMMOVE,     memmove
LIBWRAP LIB_MEMSET,      memset
LIBWRAP LIB_MEMCMP,      memcmp
LIBWRAP LIB_MEMCHR,      memchr

@ ---- io ------------------------------------------------------
LIBWRAP LIB_PUTCHAR,     putchar
LIBWRAP LIB_PUTS,        puts
LIBWRAP LIB_PRINTF,      printf
LIBWRAP LIB_SPRINTF,     sprintf
LIBWRAP LIB_SNPRINTF,    snprintf

@ ---- heap ----------------------------------------------------
LIBWRAP LIB_MALLOC,      malloc
LIBWRAP LIB_CALLOC,      calloc
LIBWRAP LIB_REALLOC,     realloc
LIBWRAP LIB_FREE,        free

@ ---- misc ----------------------------------------------------
LIBWRAP LIB_GETPID,      getpid

@ ---- AEABI (emitidos pelo gcc) --------------------------------
LIBWRAP LIB_AEABI_UIDIV,      __aeabi_uidiv
LIBWRAP LIB_AEABI_IDIV,       __aeabi_idiv
LIBWRAP LIB_AEABI_UIDIVMOD,   __aeabi_uidivmod
LIBWRAP LIB_AEABI_IDIVMOD,    __aeabi_idivmod
LIBWRAP LIB_AEABI_ULDIVMOD,   __aeabi_uldivmod
LIBWRAP LIB_AEABI_LDIVMOD,    __aeabi_ldivmod
LIBWRAP LIB_AEABI_MEMCLR,     __aeabi_memclr
LIBWRAP LIB_AEABI_MEMCPY,     __aeabi_memcpy
LIBWRAP LIB_AEABI_MEMSET,     __aeabi_memset

@ variantes alinhadas/tipadas usadas pelos builtins do gcc ------
LIBWRAP LIB_AEABI_MEMCLR,     __aeabi_memclr4
LIBWRAP LIB_AEABI_MEMCLR,     __aeabi_memclr8
LIBWRAP LIB_AEABI_MEMCPY,     __aeabi_memcpy4
LIBWRAP LIB_AEABI_MEMCPY,     __aeabi_memcpy8
LIBWRAP LIB_AEABI_MEMSET,     __aeabi_memset4
LIBWRAP LIB_AEABI_MEMSET,     __aeabi_memset8
LIBWRAP LIB_MEMMOVE,          __aeabi_memmove
LIBWRAP LIB_MEMMOVE,          __aeabi_memmove4
LIBWRAP LIB_MEMMOVE,          __aeabi_memmove8

@ ---- saida ---------------------------------------------------
.globl exit
exit:
    svc     #1                  @ SYS_EXIT (nao retorna; r0 = status)

.globl _exit
_exit:
    svc     #1

.globl abort
abort:
    svc     #1

@ ---- wrappers de syscall uteis ---------------------------------
.globl write
write:
    mov     r7, #4              @ SYS_WRITE
    svc     #0
    bx      lr

.globl read
read:
    mov     r7, #3              @ SYS_READ
    svc     #0
    bx      lr

.globl open
open:
    mov     r7, #5              @ SYS_OPEN
    svc     #0
    bx      lr

.globl close
close:
    mov     r7, #6              @ SYS_CLOSE
    svc     #0
    bx      lr

.globl brk
brk:
    mov     r7, #45             @ SYS_BRK
    svc     #0
    bx      lr

.end
