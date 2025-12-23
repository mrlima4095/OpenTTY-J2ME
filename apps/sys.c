/* test_basic.c - Teste mínimo para debug */
/* clang -target arm-linux-gnueabi -nostdlib test_basic.c -o test_basic */

__attribute__((naked)) void _start() {
    asm volatile (
        /* Teste 1: Imprimir mensagem inicial */
        "mov r7, #4\n\t"        /* SYS_WRITE = 4 */
        "mov r0, #1\n\t"        /* stdout */
        "adr r1, msg1\n\t"      /* mensagem */
        "mov r2, #19\n\t"       /* tamanho */
        "svc #0\n\t"
        
        /* Teste 2: getpid */
        "mov r7, #20\n\t"       /* SYS_GETPID = 20 */
        "svc #0\n\t"
        /* r0 agora tem o PID */
        
        /* Converter PID para string e imprimir */
        /* Primeiro, imprimir 'PID: ' */
        "mov r7, #4\n\t"
        "mov r0, #1\n\t"
        "adr r1, msg2\n\t"
        "mov r2, #5\n\t"
        "svc #0\n\t"
        
        /* Imprimir o número do PID (simplificado - apenas um dígito) */
        "add r0, r0, #48\n\t"   /* converter para ASCII */
        "push {r0}\n\t"         /* salvar no stack */
        "mov r1, sp\n\t"        /* ponteiro para o caractere */
        "mov r7, #4\n\t"
        "mov r0, #1\n\t"
        "mov r2, #1\n\t"        /* 1 caractere */
        "svc #0\n\t"
        "add sp, sp, #4\n\t"    /* limpar stack */
        
        /* Nova linha */
        "mov r7, #4\n\t"
        "mov r0, #1\n\t"
        "adr r1, newline\n\t"
        "mov r2, #1\n\t"
        "svc #0\n\t"
        
        /* Teste 3: time */
        "mov r7, #13\n\t"       /* SYS_TIME = 13 */
        "mov r0, #0\n\t"        /* NULL */
        "svc #0\n\t"
        
        /* Imprimir 'Time: ' */
        "mov r7, #4\n\t"
        "mov r0, #1\n\t"
        "adr r1, msg3\n\t"
        "mov r2, #7\n\t"
        "svc #0\n\t"
        
        /* Imprimir tempo (apenas último dígito) */
        "and r0, r0, #0xF\n\t"  /* pegar último dígito */
        "add r0, r0, #48\n\t"   /* para ASCII */
        "push {r0}\n\t"
        "mov r1, sp\n\t"
        "mov r7, #4\n\t"
        "mov r0, #1\n\t"
        "mov r2, #1\n\t"
        "svc #0\n\t"
        "add sp, sp, #4\n\t"
        
        /* Nova linha */
        "mov r7, #4\n\t"
        "mov r0, #1\n\t"
        "adr r1, newline\n\t"
        "mov r2, #1\n\t"
        "svc #0\n\t"
        
        /* Teste 4: exit */
        "mov r7, #1\n\t"        /* SYS_EXIT = 1 */
        "mov r0, #0\n\t"        /* status 0 */
        "svc #0\n\t"
        
        /* Dados */
        "b data_end\n\t"
        "msg1: .asciz \"=== ARM Test Start ===\\n\"\n\t"
        "msg2: .asciz \"PID: \"\n\t"
        "msg3: .asciz \"Time: \"\n\t"
        "newline: .asciz \"\\n\"\n\t"
        ".align 2\n\t"
        "data_end:\n\t"
        
        : : : "memory"
    );
}