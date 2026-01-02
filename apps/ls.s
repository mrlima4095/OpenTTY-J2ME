@ test_full_debug.s
.section .data
dir:    .asciz "/"
step1:  .asciz "1: Before open\n"
step2:  .asciz "2: After open, fd="
step3:  .asciz "3: Before getdents\n"
step4:  .asciz "4: After getdents, ret="
step5:  .asciz "5: Exiting\n"
nl:     .asciz "\n"
buffer: .space 1024
hexchars: .asciz "0123456789ABCDEF"

.section .text
.global _start

_start:
    @ Passo 1
    ldr r1, =step1
    mov r2, #15
    bl write
    
    cmp r4, #0
    ble error_exit
    
    @ Passo 3
    ldr r1, =step3
    mov r2, #19
    bl write
    
    mov r5, r0         @ salvar retorno
    
    @ Passo 5
    ldr r1, =step5
    mov r2, #11
    bl write
    
    @ Fechar e sair
    mov r0, r4
    mov r7, #6         @ SYS_CLOSE
    svc 0
    
    mov r0, #0
    b exit

error_exit:
    mov r0, #1

exit:
    mov r7, #1         @ SYS_EXIT
    svc 0

@ Sub-rotinas
write:
    push {r0, r7}
    mov r0, #1
    mov r7, #4
    svc 0
    pop {r0, r7}
    bx lr

print_nl:
    push {r0-r2, r7, lr}
    ldr r1, =nl
    mov r2, #1
    bl write
    pop {r0-r2, r7, pc}

print_hex:
    push {r0-r5, lr}
    mov r3, r0
    mov r2, #8
    
hex_loop:
    sub r2, r2, #1
    mov r0, r3, lsr #28
    and r0, r0, #0xF
    ldr r1, =hexchars
    ldrb r0, [r1, r0]
    
    push {r0}
    mov r1, sp
    mov r2, #1
    bl write
    pop {r0}
    
    mov r3, r3, lsl #4
    cmp r2, #0
    bne hex_loop
    
    pop {r0-r5, pc}