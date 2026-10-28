# libc.s (RISC-V RV32IM) - C library for the OpenTTY ELF emulator
#
# Most functions do NOT implement logic in assembly: each one is a 3-instruction
# wrapper (li a7, #LIB_*; ecall; ret) that hands the work to the emulator
# (src/ELF.java -> handleLibraryCall). The emulator implements strings, printf,
# mem*, the heap allocator, atoi/abs and the RISC-V division helpers, plus the
# LCDUI widget ABI declared in lcdui.h.
#
# LINK:  ./build-elf.sh demo.c -stdlib
# (-stdlib links this file before the program; the program defines main; the
#  _start below reads argc/argv/envp from the emulator CRT stack and calls main.)
#
# Emulator syscall numbers (src/ELF.java):
#   ecall with a7 = N (N != 0) -> handleSyscall(N), or if N >= LIB_BASE
#   handleLibraryCall(N - LIB_BASE).
#
# RISC-V ABI: args in a0-a7 (x10-x17), return value in a0, syscall number in a7.
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
.equ LIB_UDIV32,           LIB_BASE + 30
.equ LIB_SDIV32,           LIB_BASE + 31
.equ LIB_UDIVMOD32,        LIB_BASE + 32
.equ LIB_SDIVMOD32,        LIB_BASE + 33
.equ LIB_UDIVMOD64,        LIB_BASE + 34
.equ LIB_SDIVMOD64,        LIB_BASE + 35
.equ LIB_MEMCLR,           LIB_BASE + 36
.equ LIB_MEMCPY_ALIGN,     LIB_BASE + 37
.equ LIB_MEMSET_ALIGN,     LIB_BASE + 38
.equ LIB_UI_NEW,           LIB_BASE + 39
.equ LIB_UI_APPEND_TEXT,   LIB_BASE + 40
.equ LIB_UI_APPEND_FIELD,  LIB_BASE + 41
.equ LIB_UI_LIST_APPEND,   LIB_BASE + 42
.equ LIB_UI_COMMAND,       LIB_BASE + 43
.equ LIB_UI_ADD_COMMAND,   LIB_BASE + 44
.equ LIB_UI_DISPLAY,       LIB_BASE + 45
.equ LIB_UI_SET_TEXT,      LIB_BASE + 46
.equ LIB_UI_GET_TEXT,      LIB_BASE + 47
.equ LIB_UI_SET_TITLE,     LIB_BASE + 48
.equ LIB_UI_CLEAR,         LIB_BASE + 49
.equ LIB_UI_WAIT_EVENT,    LIB_BASE + 50
.equ LIB_UI_DESTROY,       LIB_BASE + 51
.equ LIB_UI_TASKMNGR,      LIB_BASE + 52
.equ LIB_PROC_SET,         LIB_BASE + 53
.equ LIB_PROC_SPAWN,       LIB_BASE + 54
.equ LIB_PROC_WAITPID,     LIB_BASE + 55
.equ LIB_PROC_SHELL,       LIB_BASE + 56
.equ LIB_PROC_GETENV,      LIB_BASE + 57
.equ LIB_UI_SET_LABEL,     LIB_BASE + 58
.equ LIB_PROC_EXPAND_ENV,  LIB_BASE + 59
.equ LIB_GC,               LIB_BASE + 60
.equ LIB_MEM_FREE,         LIB_BASE + 61
.equ LIB_MEM_TOTAL,        LIB_BASE + 62
.equ LIB_MEM_USED,         LIB_BASE + 63
.equ LIB_SOCKET_READER_START, LIB_BASE + 64
.equ LIB_SOCKET_READER_STOP,  LIB_BASE + 65
.equ LIB_STRSTR,           LIB_BASE + 66
.equ LIB_STRRCHR,          LIB_BASE + 67
.equ LIB_STRCASECMP,       LIB_BASE + 68
.equ LIB_STRNCASECMP,      LIB_BASE + 69
.equ LIB_STRTOL,           LIB_BASE + 70
.equ LIB_STRTOUL,          LIB_BASE + 71
.equ LIB_UI_ALERT,         LIB_BASE + 72
.equ LIB_UI_SET_TICKER,    LIB_BASE + 73
.equ LIB_UI_APPEND_GAUGE,  LIB_BASE + 74
.equ LIB_UI_SET_GAUGE,     LIB_BASE + 75
.equ LIB_UI_GET_GAUGE,     LIB_BASE + 76
.equ LIB_UI_APPEND_SPACER, LIB_BASE + 77
.equ LIB_UI_APPEND_IMAGE,  LIB_BASE + 78
.equ LIB_UI_LIST_APPEND_IMAGE, LIB_BASE + 79
.equ LIB_UI_APPEND_CHOICE, LIB_BASE + 80
.equ LIB_UI_CHOICE_APPEND, LIB_BASE + 81
.equ LIB_UI_CHOICE_SELECTED, LIB_BASE + 82
.equ LIB_UI_CHOICE_SET_SELECTED, LIB_BASE + 83
.equ LIB_UI_CHOICE_COUNT,  LIB_BASE + 84
.equ LIB_UI_APPEND_BUTTON, LIB_BASE + 85
.equ LIB_UI_ITEM_COMMAND,  LIB_BASE + 86
.equ LIB_UI_SET_DEFAULT_COMMAND, LIB_BASE + 87
.equ LIB_UI_GET_CURRENT,   LIB_BASE + 88
.equ LIB_UI_VIBRATE,       LIB_BASE + 89
.equ LIB_UI_GET_LABEL,     LIB_BASE + 90
.equ LIB_UI_DELETE,        LIB_BASE + 91
.equ LIB_UI_INSERT_TEXT,   LIB_BASE + 92
.equ LIB_UI_SIZE,          LIB_BASE + 93
.equ LIB_UI_LIST_SELECTED, LIB_BASE + 94
.equ LIB_UI_SET_FONT,      LIB_BASE + 95
.equ LIB_UI_SET_LAYOUT,    LIB_BASE + 96
.equ LIB_UI_SHOW_ALERT,    LIB_BASE + 97

# ============================================================
# _start - Entry point (matches the emulator CRT layout):
#   [sp]     = argc
#   [sp,4]   = argv[] (pointers), NULL terminated
#   envp[] follows the argv NULL
# ============================================================
.text

.globl _start
_start:
    lw      a0, 0(sp)            # argc
    addi    a1, sp, 4            # argv
    slli    a2, a0, 2            # argv + argc*4
    add     a2, a1, a2           # points at the argv NULL
    addi    a2, a2, 4            # envp (after the argv NULL)
    jal     main
    # main returned; exit(0)
    li      a7, 1                # SYS_EXIT
    ecall

# ============================================================
# Wrapper generator: li a7, #LIB_*; ecall; ret
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
LIBWRAP LIB_STRSTR,      strstr
LIBWRAP LIB_STRRCHR,     strrchr
LIBWRAP LIB_STRCASECMP,  strcasecmp
LIBWRAP LIB_STRNCASECMP, strncasecmp
LIBWRAP LIB_STRTOL,      strtol
LIBWRAP LIB_STRTOUL,     strtoul
LIBWRAP LIB_ATOI,        atoi
LIBWRAP LIB_ABS,         abs
LIBWRAP LIB_TOUPPER,     toupper
LIBWRAP LIB_TOLOWER,     tolower

# ---- memory --------------------------------------------------
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
LIBWRAP LIB_GC,          gc
LIBWRAP LIB_MEM_FREE,    mem_free
LIBWRAP LIB_MEM_TOTAL,   mem_total
LIBWRAP LIB_MEM_USED,    mem_used

# ---- LCDUI ---------------------------------------------------
LIBWRAP LIB_UI_NEW,          lcdui_new
LIBWRAP LIB_UI_APPEND_TEXT,  lcdui_append_text
LIBWRAP LIB_UI_APPEND_FIELD, lcdui_append_field
LIBWRAP LIB_UI_LIST_APPEND,  lcdui_list_append
LIBWRAP LIB_UI_COMMAND,      lcdui_command
LIBWRAP LIB_UI_ADD_COMMAND,  lcdui_add_command
LIBWRAP LIB_UI_DISPLAY,      lcdui_display
LIBWRAP LIB_UI_SET_TEXT,     lcdui_set_text
LIBWRAP LIB_UI_GET_TEXT,     lcdui_get_text
LIBWRAP LIB_UI_SET_TITLE,    lcdui_set_title
LIBWRAP LIB_UI_CLEAR,        lcdui_clear
LIBWRAP LIB_UI_WAIT_EVENT,   lcdui_wait_event
LIBWRAP LIB_UI_DESTROY,      lcdui_destroy
LIBWRAP LIB_UI_TASKMNGR,     graphics_taskmngr
LIBWRAP LIB_UI_SET_LABEL,    lcdui_set_label
LIBWRAP LIB_UI_ALERT,        lcdui_alert
LIBWRAP LIB_UI_SET_TICKER,   lcdui_set_ticker
LIBWRAP LIB_UI_APPEND_GAUGE, lcdui_append_gauge
LIBWRAP LIB_UI_SET_GAUGE,    lcdui_set_gauge
LIBWRAP LIB_UI_GET_GAUGE,    lcdui_get_gauge
LIBWRAP LIB_UI_APPEND_SPACER, lcdui_append_spacer
LIBWRAP LIB_UI_APPEND_IMAGE, lcdui_append_image
LIBWRAP LIB_UI_LIST_APPEND_IMAGE, lcdui_list_append_image
LIBWRAP LIB_UI_APPEND_CHOICE, lcdui_append_choice
LIBWRAP LIB_UI_CHOICE_APPEND, lcdui_choice_append
LIBWRAP LIB_UI_CHOICE_SELECTED, lcdui_choice_selected
LIBWRAP LIB_UI_CHOICE_SET_SELECTED, lcdui_choice_set_selected
LIBWRAP LIB_UI_CHOICE_COUNT, lcdui_choice_count
LIBWRAP LIB_UI_APPEND_BUTTON, lcdui_append_button
LIBWRAP LIB_UI_ITEM_COMMAND, lcdui_item_command
LIBWRAP LIB_UI_SET_DEFAULT_COMMAND, lcdui_set_default_command
LIBWRAP LIB_UI_GET_CURRENT,  lcdui_get_current
LIBWRAP LIB_UI_VIBRATE,      lcdui_vibrate
LIBWRAP LIB_UI_GET_LABEL,    lcdui_get_label
LIBWRAP LIB_UI_DELETE,       lcdui_delete
LIBWRAP LIB_UI_INSERT_TEXT,  lcdui_insert_text
LIBWRAP LIB_UI_SIZE,         lcdui_size
LIBWRAP LIB_UI_LIST_SELECTED, lcdui_list_selected
LIBWRAP LIB_UI_SET_FONT,     lcdui_set_font
LIBWRAP LIB_UI_SET_LAYOUT,   lcdui_set_layout
LIBWRAP LIB_UI_SHOW_ALERT,   lcdui_show_alert

# ---- processes / environment ---------------------------------
LIBWRAP LIB_PROC_SET,        opentty_setproc
LIBWRAP LIB_PROC_SPAWN,      opentty_spawn
LIBWRAP LIB_PROC_WAITPID,    opentty_waitpid
LIBWRAP LIB_PROC_SHELL,      opentty_shell
LIBWRAP LIB_PROC_GETENV,     opentty_getenv
LIBWRAP LIB_PROC_EXPAND_ENV, opentty_expand_env
LIBWRAP LIB_SOCKET_READER_START, opentty_socket_reader_start
LIBWRAP LIB_SOCKET_READER_STOP,  opentty_socket_reader_stop

# ---- RISC-V runtime helpers ----------------------------------
LIBWRAP LIB_UDIV32,           __udivsi3
LIBWRAP LIB_SDIV32,           __divsi3
LIBWRAP LIB_UDIVMOD32,        __udivmodsi4
LIBWRAP LIB_SDIVMOD32,        __divmodsi4
LIBWRAP LIB_UDIVMOD64,        __udivmoddi4
LIBWRAP LIB_SDIVMOD64,        __divmoddi4
LIBWRAP LIB_MEMCLR,           __memclr
LIBWRAP LIB_MEMCPY_ALIGN,     __memcpy
LIBWRAP LIB_MEMSET_ALIGN,     __memset

# aligned/typed variants used by memory routines ----------------
LIBWRAP LIB_MEMCLR,           __memclr4
LIBWRAP LIB_MEMCLR,           __memclr8
LIBWRAP LIB_MEMCPY_ALIGN,     __memcpy4
LIBWRAP LIB_MEMCPY_ALIGN,     __memcpy8
LIBWRAP LIB_MEMSET_ALIGN,     __memset4
LIBWRAP LIB_MEMSET_ALIGN,     __memset8
LIBWRAP LIB_MEMMOVE,          __memmove
LIBWRAP LIB_MEMMOVE,          __memmove4
LIBWRAP LIB_MEMMOVE,          __memmove8

# ---- qsort / bsearch (guest side, comparator is a guest function) ------
# void qsort(void *base, int nmemb, int size,
#            int (*cmp)(const void *, const void *))
# Insertion sort: stable, no extra memory, O(n^2) worst case - a good fit for
# the small arrays that fit in 1 MiB of guest RAM.
.globl qsort
.type qsort, %function
qsort:
    addi    sp, sp, -32
    sw      ra, 28(sp)
    sw      s0, 24(sp)
    sw      s1, 20(sp)
    sw      s2, 16(sp)
    sw      s3, 12(sp)
    sw      s4, 8(sp)
    sw      s5, 4(sp)
    mv      s0, a0                # base
    mv      s1, a1                # nmemb
    mv      s2, a2                # size
    mv      s3, a3                # cmp
    li      s4, 1                 # i = 1
qsort_outer:
    bge     s4, s1, qsort_done
    mv      s5, s4                # j = i
qsort_inner:
    blez    s5, qsort_next        # while (j > 0)
    addi    t0, s5, -1
    mul     t0, t0, s2
    add     t0, s0, t0            # &a[j-1]
    add     t1, t0, s2            # &a[j]
    mv      a0, t0
    mv      a1, t1
    jalr    s3                    # cmp(&a[j-1], &a[j])
    blez    a0, qsort_next        # already ordered
    addi    t0, s5, -1            # t regs are caller-saved: rebuild them
    mul     t0, t0, s2
    add     t0, s0, t0
    add     t1, t0, s2
    mv      t2, s2                # swap size bytes
qsort_swap:
    beqz    t2, qsort_swapped
    lbu     t3, 0(t0)
    lbu     t4, 0(t1)
    sb      t4, 0(t0)
    sb      t3, 0(t1)
    addi    t0, t0, 1
    addi    t1, t1, 1
    addi    t2, t2, -1
    j       qsort_swap
qsort_swapped:
    addi    s5, s5, -1
    j       qsort_inner
qsort_next:
    addi    s4, s4, 1
    j       qsort_outer
qsort_done:
    lw      ra, 28(sp)
    lw      s0, 24(sp)
    lw      s1, 20(sp)
    lw      s2, 16(sp)
    lw      s3, 12(sp)
    lw      s4, 8(sp)
    lw      s5, 4(sp)
    addi    sp, sp, 32
    ret

# void *bsearch(const void *key, const void *base, int nmemb, int size,
#               int (*cmp)(const void *, const void *))
# Requires a sorted array; returns a matching element or NULL.
.globl bsearch
.type bsearch, %function
bsearch:
    addi    sp, sp, -32
    sw      ra, 28(sp)
    sw      s0, 24(sp)
    sw      s1, 20(sp)
    sw      s2, 16(sp)
    sw      s3, 12(sp)
    sw      s4, 8(sp)
    sw      s5, 4(sp)
    mv      s0, a0                # key
    mv      s1, a1                # base
    mv      s2, a2                # hi = nmemb
    mv      s3, a3                # size
    mv      s4, a4                # cmp
    li      s5, 0                 # lo = 0
bsearch_loop:
    bge     s5, s2, bsearch_none
    add     t0, s5, s2
    srli    t0, t0, 1             # mid = (lo + hi) / 2
    mul     t1, t0, s3
    add     t1, s1, t1            # &a[mid]
    sw      t0, 0(sp)             # the comparator clobbers t0
    mv      a0, s0
    mv      a1, t1
    jalr    s4
    lw      t0, 0(sp)
    beqz    a0, bsearch_found
    bltz    a0, bsearch_lower
    addi    s5, t0, 1             # lo = mid + 1
    j       bsearch_loop
bsearch_lower:
    mv      s2, t0                # hi = mid
    j       bsearch_loop
bsearch_found:
    mul     t1, t0, s3
    add     a0, s1, t1
    j       bsearch_out
bsearch_none:
    li      a0, 0
bsearch_out:
    lw      ra, 28(sp)
    lw      s0, 24(sp)
    lw      s1, 20(sp)
    lw      s2, 16(sp)
    lw      s3, 12(sp)
    lw      s4, 8(sp)
    lw      s5, 4(sp)
    addi    sp, sp, 32
    ret

# ---- RISC-V 64-bit helpers (emitted by the compiler) ------------------
# clang/gcc RV32IM uses __muldi3/__divdi3/__moddi3/__udivdi3/__umoddi3 for
# long long arithmetic. Argument layout:
#   a0:a1 = dividend/destination, a2:a3 = divisor/source.
# LIB_*LDIVMOD returns the quotient in a0:a1 AND the remainder in a2:a3 (see
# libcLdivmod in src/ELF.java); the functions below pick the right half.
.globl __udivdi3
.type __udivdi3, %function
__udivdi3:
    li      a7, LIB_UDIVMOD64
    ecall
    ret

.globl __divdi3
.type __divdi3, %function
__divdi3:
    li      a7, LIB_SDIVMOD64
    ecall
    ret

.globl __umoddi3
.type __umoddi3, %function
__umoddi3:
    li      a7, LIB_UDIVMOD64
    ecall
    mv      a0, a2               # remainder a2:a3 -> a0:a1
    mv      a1, a3
    ret

.globl __moddi3
.type __moddi3, %function
__moddi3:
    li      a7, LIB_SDIVMOD64
    ecall
    mv      a0, a2               # remainder a2:a3 -> a0:a1
    mv      a1, a3
    ret

# __muldi3(a0:a1, a2:a3) -> a0:a1 (32x32 -> 64 via MUL/MULHU, no lmul)
.globl __muldi3
.type __muldi3, %function
__muldi3:
    mul     t0, a0, a2           # lo32 of Alo*Blo
    mulhu   t3, a0, a2           # carry out of Alo*Blo (bit 32+)
    mul     t1, a1, a2           # Ahi*Blo (lo32)
    mul     t2, a0, a3           # Alo*Bhi (lo32)
    add     t1, t1, t2
    add     t1, t1, t3
    mv      a0, t0
    mv      a1, t1
    ret

# ---- output --------------------------------------------------
.globl exit
exit:
    li      a7, 1                # SYS_EXIT (does not return; a0 = status)
    ecall

.globl _exit
_exit:
    li      a7, 1
    ecall

.globl abort
abort:
    li      a7, 1
    ecall

# ---- useful syscall wrappers ---------------------------------
.globl time
.type time, %function
time:
    mv      a1, a0               # tloc: the emulator reads the pointer from a1
    li      a7, 13               # SYS_TIME (seconds, UTC)
    ecall
    ret

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
