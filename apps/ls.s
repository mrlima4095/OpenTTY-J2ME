@ Teste super simples - apenas tenta getdents
.text
.global _start

_start:
    @ Tentar getdents com fd=3 (pode não existir)
    mov r0, #3          @ fd qualquer
    ldr r1, =buffer
    mov r2, #100
    mov r7, #217        @ SYS_GETDENTS
    swi 0
    
    @ Se retornou -9 (EBADF) = syscall funciona
    @ Se retornou 0 = funcionou mas diretório vazio
    @ Se retornou positivo = funcionou com dados
    
    @ Para teste simples, apenas sair
    mov r0, #0          @ sempre sucesso para teste
    mov r7, #1          @ SYS_EXIT
    swi 0

.data
buffer: .space 100