@ test_open_only.s
.section .data
msg1:   .asciz "1: Starting\n"
msg2:   .asciz "2: Testing open('/')\n"
msg3:   .asciz "3: Open returned: "
msg4:   .asciz "4: Exiting\n"
nl:     .asciz "\n"
dir:    .asciz "/"
hex:    .asciz "0123456789ABCDEF"

.section .text
.global _start

_start:
    @ 1. Starting
    mov r0, #1
    ldr r1, =msg1
    mov r2, #12
    mov r7, #4
    svc 0
    
    @ 2. Message before open
    mov r0, #1
    ldr r1, =msg2
    mov r2, #21
    mov r7, #4
    svc 0
    
    @ 3. Open directory (JUST TEST, don't store)
    ldr r0, =dir
    mov r1, #0x10000   @ O_DIRECTORY
    mov r7, #5         @ SYS_OPEN
    svc 0
    
    @ Result is in r0, but we'll just print it
    @ 4. Print "Open returned: "
    mov r4, r0         @ Temporarily save
    mov r0, #1
    ldr r1, =msg3
    mov r2, #19
    mov r7, #4
    svc 0
    
    @ Print the return value in hex
    mov r0, r4
    bl print_hex_short
    
    @ Newline
    mov r0, #1
    ldr r1, =nl
    mov r2, #1
    mov r7, #4
    svc 0
    
    @ 5. If fd > 0, close it
    cmp r4, #0
    ble no_close
    
    mov r0, r4
    mov r7, #6         @ SYS_CLOSE
    svc 0
    
no_close:
    @ 6. Final message
    mov r0, #1
    ldr r1, =msg4
    mov r2, #11
    mov r7, #4
    svc 0
    
    @ 7. Exit with code based on open result
    @ 0 = success (fd > 0)
    @ 1 = failure (fd <= 0)
    cmp r4, #0
    bgt exit_success
    
exit_fail:
    mov r0, #1
    b exit
    
exit_success:
    mov r0, #0

exit:
    mov r7, #1
    svc 0

@ Simple hex print (2 digits)
print_hex_short:
    push {r4-r5, lr}
    mov r4, r0
    ldr r5, =hexbuf
    
    @ High nibble
    mov r0, r4, lsr #4
    and r0, r0, #0xF
    ldr r1, =hex
    ldrb r0, [r1, r0]
    strb r0, [r5, #0]
    
    @ Low nibble
    mov r0, r4
    and r0, r0, #0xF
    ldr r1, =hex
    ldrb r0, [r1, r0]
    strb r0, [r5, #1]
    
    @ Print
    mov r0, #1
    ldr r1, =hexbuf
    mov r2, #2
    mov r7, #4
    svc 0
    
    pop {r4-r5, pc}

.section .bss
hexbuf: .space 3