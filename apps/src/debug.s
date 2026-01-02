.global _start
_start:
    /* Teste write funciona - usando adr em vez de ldr = */
    mov r0, #1
    adr r1, test_msg      /* ADR em vez de LDR = */
    mov r2, #23           /* Corrigido: 23 bytes, não 20 */
    mov r7, #4
    swi #0
    
    /* Chamar gettimeofday */
    adr r0, tv            /* &tv - usando adr */
    adr r1, tz            /* &tz - usando adr */
    mov r7, #78           /* syscall 78 */
    swi #0
    
    /* Salvar retorno */
    mov r8, r0
    
    /* Verificar erro */
    cmp r8, #0
    blt error
    
    /* Sucesso */
    mov r0, #1
    adr r1, success_msg
    mov r2, #26           /* 26 bytes */
    mov r7, #4
    swi #0
    
    /* Mostrar retorno */
    mov r0, #1
    adr r1, ret_msg
    mov r2, #20
    mov r7, #4
    swi #0
    
    /* Converter retorno para ASCII (apenas 0-9) */
    add r0, r8, #'0'
    adr r1, digit
    strb r0, [r1]
    
    mov r0, #1
    adr r1, digit
    mov r2, #1
    mov r7, #4
    swi #0
    
    /* Nova linha */
    mov r0, #1
    adr r1, newline
    mov r2, #1
    mov r7, #4
    swi #0
    
    /* Sair */
    mov r0, #0
    mov r7, #1
    swi #0

error:
    /* Erro */
    mov r0, #1
    adr r1, error_msg
    mov r2, #34           /* 34 bytes */
    mov r7, #4
    swi #0
    
    /* Mostrar erro */
    neg r8, r8
    add r0, r8, #'0'
    adr r1, digit
    strb r0, [r1]
    
    mov r0, #1
    adr r1, digit
    mov r2, #1
    mov r7, #4
    swi #0
    
    /* Nova linha e sair com erro */
    mov r0, #1
    adr r1, newline
    mov r2, #1
    mov r7, #4
    swi #0
    
    mov r0, #1
    mov r7, #1
    swi #0

/* Strings e dados */
test_msg:     .ascii "Testing gettimeofday...\n"
success_msg:  .ascii "gettimeofday succeeded!\n"
error_msg:    .ascii "gettimeofday failed with error: "
ret_msg:      .ascii "Return value was: "
newline:      .ascii "\n"
digit:        .ascii "0"
.align 2
tv:           .word 0, 0
tz:           .word 0, 0