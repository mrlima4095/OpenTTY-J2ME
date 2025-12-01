.text
.global _start

_start:
    mov r7, #4      @ syscall write (4)
    mov r0, #1      @ fd = stdout
    ldr r1, =msg    @ buffer
    mov r2, #13     @ length
    swi 0           @ syscall
    
    mov r7, #1      @ syscall exit (1)
    mov r0, #0      @ status = 0
    swi 0           @ syscall

.data
msg: .asciz "Hello World!\n"