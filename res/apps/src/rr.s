section .text
global _start

_start:
  li $v0, 12   # Código de entrada (IDT)
  la $v1, hello   # Endereço da string "hello, world!"
  syscall

section .rodata
hello:
  .string "hello, world!"