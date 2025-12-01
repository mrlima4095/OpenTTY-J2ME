.global _start
.text
_start:
    mov r0, #1      @ file descriptor (stdout)
    ldr r1, =msg    @ buffer
    ldr r2, =len    @ length
    mov r7, #4      @ syscall write
    swi 0           @ syscall
    
    mov r0, #0      @ exit code
    mov r7, #1      @ syscall exit
    swi 0

.data
msg: .asciz "Hello, ARM World!\n"
len = . - msg