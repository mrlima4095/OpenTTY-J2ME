# hello.s (RISC-V RV32IM) - "Hello, World!" com syscalls crus.
#   write(1, message, len) + exit(0) — o mesmo que hello.c com -stdlib.
# NOTA: .data antes de .text (llvm-mc nao dobra forward refs em imm).
.global _start
.section .data
message:
    .ascii "Hello, World!\n\0"
len = . - message

.section .text

_start:
    li      a7, 4                # SYS_WRITE
    li      a0, 1                # stdout
    la      a1, message
    li      a2, len
    ecall

    li      a7, 1                # SYS_EXIT
    li      a0, 0
    ecall