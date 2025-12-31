.section .data
message:
    .ascii "Hello, World!\n\0"
len = . - message

.section .text
.global _start

_start:
    /* Syscall write(fd, buf, count) */
    mov r0, #1          /* fd = stdout (1) */
    ldr r1, =message    /* buffer */
    ldr r2, =len        /* count */
    mov r7, #4          /* syscall number for write */
    swi #0              /* syscall */

    /* Syscall exit(status) */
    mov r0, #0          /* status = 0 */
    mov r7, #1          /* syscall number for exit */
    swi #0              /* syscall */