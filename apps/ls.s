@ test_open.s - Testa apenas open()
.section .data
dir:    .asciz "/home/"
msg_ok: .asciz "OPEN OK\n"
msg_fail: .asciz "OPEN FAIL\n"

.section .text
.global _start

_start:
    @ Tentar abrir diretório
    ldr r0, =dir
    mov r1, #0x10000   @ O_DIRECTORY
    mov r7, #5         @ SYS_OPEN
    svc 0
    
    @ Verificar resultado
    cmp r0, #0
    bgt open_success
    
open_fail:
    @ Imprimir falha
    ldr r1, =msg_fail
    mov r2, #10
    bl write
    mov r0, #1         @ exit code 1
    b exit

open_success:
    @ Imprimir sucesso
    ldr r1, =msg_ok
    mov r2, #9
    bl write
    
    @ Fechar fd
    mov r7, #6         @ SYS_CLOSE
    svc 0
    
    mov r0, #0         @ exit code 0

exit:
    mov r7, #1         @ SYS_EXIT
    svc 0

write:
    push {r0, r7}
    mov r0, #1
    mov r7, #4
    svc 0
    pop {r0, r7}
    bx lr