@ vmlinuz.s — OpenTTY real kernel (ARM32 ELF for src/ELF.java)
@ Boots on the J2ME ELF emulator; reads /boot/config; execve /bin/init.
@ Build: ./build-elf.sh kernel/vmlinuz.s -o kernel/vmlinuz

.syntax unified
.arm
.text

@ ---- Syscall numbers (from ELF.java SYS_*) ----
.equ SYS_EXIT,   1
.equ SYS_READ,   3
.equ SYS_WRITE,  4
.equ SYS_OPEN,   5
.equ SYS_CLOSE,  6
.equ SYS_EXECVE, 11
.equ SYS_BRK,    45
.equ SYS_UNAME,  122

.equ O_RDONLY, 0

@ ---- Entry ----
.globl _start
_start:
    @ Banner
    ldr   r0, =banner_start
    bl    w_str

    @ Memory probe via brk(0): heap_end = brk(0)
    mov   r0, #0
    mov   r7, #SYS_BRK
    svc   #0
    mov   r4, r0                       @ r4 = heap end
    ldr   r0, =msg_mem_top
    bl    w_str
    mov   r0, r4
    bl    print_dec
    ldr   r0, =msg_mem_unit
    bl    w_str

    @ uname into uts_buf
    ldr   r0, =uts_buf
    mov   r7, #SYS_UNAME
    svc   #0
    cmp   r0, #0
    bne   1f

    ldr   r0, =msg_sysname
    bl    w_str
    ldr   r0, =uts_buf                  @ +0  sysname
    bl    w_str
    ldr   r0, =msg_release
    bl    w_str
    ldr   r0, =uts_buf+130              @ +130 release
    bl    w_str
    ldr   r0, =msg_version
    bl    w_str
    ldr   r0, =uts_buf+195              @ +195 version
    bl    w_str
    ldr   r0, =msg_machine
    bl    w_str
    ldr   r0, =uts_buf+260              @ +260 machine
    bl    w_str
1:

    @ Open /boot/config
    ldr   r0, =path_config
    mov   r1, #O_RDONLY
    mov   r2, #0
    mov   r7, #SYS_OPEN
    svc   #0
    mov   r5, r0                        @ r5 = fd
    cmp   r5, #0
    blt   no_cfg

    ldr   r1, =cfg_buf
    mov   r2, #256
    mov   r7, #SYS_READ
    svc   #0
    mov   r6, r0                        @ r6 = bytes read
    mov   r0, r5
    mov   r7, #SYS_CLOSE
    svc   #0
    cmp   r6, #0
    ble   no_cfg

    ldr   r0, =msg_config
    bl    w_str
    ldr   r0, =cfg_buf
    mov   r1, r6
    bl    w_len
    ldr   r0, =msg_newline
    bl    w_str
    b     exec_init
no_cfg:
    ldr   r0, =msg_noconfig
    bl    w_str

exec_init:
    @ argv = { "/bin/init", NULL }
    ldr   r0, =exec_argv
    ldr   r1, =path_init
    str   r1, [r0]
    mov   r1, #0
    str   r1, [r0, #4]

    ldr   r0, =msg_startinit
    bl    w_str
    ldr   r0, =path_init
    ldr   r1, =exec_argv
    mov   r2, #0                        @ envp = NULL
    mov   r7, #SYS_EXECVE
    svc   #0

    @ execve only returns on failure
    ldr   r0, =msg_exec_fail
    bl    w_str
    mov   r0, #1
    mov   r7, #SYS_EXIT
    svc   #0

@ ============================================================
@ Helpers
@ ============================================================

@ w_str(r0) — write null-terminated string to stdout
w_str:
    push  {r4, lr}
    mov   r4, r0
    mov   r1, r0
1:  ldrb  r3, [r1], #1
    cmp   r3, #0
    bne   1b
    sub   r2, r1, #1
    sub   r2, r2, r4
    mov   r1, r4
    mov   r0, #1
    mov   r7, #SYS_WRITE
    svc   #0
    pop   {r4, pc}

@ w_len(r0=ptr, r1=len) — write raw bytes to stdout
w_len:
    push  {lr}
    mov   r2, r1
    mov   r1, r0
    mov   r0, #1
    mov   r7, #SYS_WRITE
    svc   #0
    pop   {pc}

@ putc(r0) — write single byte to stdout
putc:
    push  {r4, lr}
    mov   r4, r0
    sub   sp, sp, #4
    strb  r4, [sp]
    mov   r0, #1
    mov   r1, sp
    mov   r2, #1
    mov   r7, #SYS_WRITE
    svc   #0
    add   sp, sp, #4
    pop   {r4, pc}

@ print_dec(r0) — unsigned decimal
print_dec:
    push  {r4, r5, lr}
    mov   r4, r0
    sub   sp, sp, #12
    mov   r2, sp
    add   r2, r2, #9
    mov   r1, #0
    strb  r1, [r2], #1                 @ null terminator
1:  mov   r0, r4
    bl    udiv10
    mov   r4, r0
    add   r1, r1, #'0'
    strb  r1, [r2], #1
    mov   r1, #10
    cmp   r4, #0
    bne   1b
2:  subs  r2, r2, #1
    ldrb  r0, [r2]
    cmp   r0, #0
    beq   2b
    bl    putc
    cmp   r2, sp
    bgt   2b
    add   sp, sp, #12
    pop   {r4, r5, pc}

@ udiv10: r0 = r0/10, r1 = remainder
udiv10:
    mov   r1, #0
1:  cmp   r0, #10
    blt   2f
    sub   r0, r0, #10
    add   r1, r1, #1
    b     1b
2:  bx    lr

@ ============================================================
@ Data
@ ============================================================
.section .rodata

banner_start:
    .asciz "OpenTTY Kernel v2026.1.18.2 (armv5tejl)\n"
msg_mem_top:
    .asciz "Heap break at boot: "
msg_mem_unit:
    .asciz " bytes\n"
msg_sysname:
    .asciz "Kernel:  "
msg_release:
    .asciz "Release: "
msg_version:
    .asciz "Version: "
msg_machine:
    .asciz "Machine: "
msg_config:
    .asciz "boot config:\n"
msg_noconfig:
    .asciz "boot config: none found (defaulting to /bin/init)\n"
msg_startinit:
    .asciz "starting /bin/init ...\n"
msg_exec_fail:
    .asciz "kernel: /bin/init exec failed, halted.\n"
msg_newline:
    .asciz "\n"
path_config:
    .asciz "/boot/config"
path_init:
    .asciz "/bin/init"

@ ============================================================
@ BSS
@ ============================================================
.section .bss

.lcomm uts_buf,   390                  @ 6 * 65 bytes  (uname)
.lcomm cfg_buf,   256                  @ /boot/config read buffer
.lcomm exec_argv,  8                   @ { char *argv[2]; }
