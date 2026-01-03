// Todo em assembly inline
asm(
".section .text\n"
".global _start\n"
".align 2\n"
"_start:\n"
    // argc está em r0, argv em r1 (convenção ARM)
    "mov r4, r0\n"          // r4 = argc
    "mov r5, r1\n"          // r5 = argv
    
    "cmp r4, #0\n"          // Verificar se há argumentos
    "beq exit_program\n"    // Se não, sair
    
    "mov r6, #0\n"          // r6 = i = 0
    
"print_loop:\n"
    "cmp r6, r4\n"          // i < argc?
    "bge exit_program\n"    // Se não, terminar
    
    // Carregar argv[i]
    "ldr r0, [r5, r6, lsl #2]\n"  // r0 = argv[i]
    
    // Calcular comprimento da string
    "mov r1, #0\n"          // r1 = len = 0
"strlen_loop:\n"
    "ldrb r2, [r0, r1]\n"   // r2 = char
    "cmp r2, #0\n"          // fim da string?
    "beq print_string\n"
    "add r1, r1, #1\n"      // len++
    "b strlen_loop\n"
    
"print_string:\n"
    // write(1, argv[i], len)
    "mov r2, r1\n"          // r2 = len
    "mov r1, r0\n"          // r1 = string
    "mov r0, #1\n"          // r0 = STDOUT_FILENO
    "mov r7, #4\n"          // r7 = SYS_write
    "swi #0\n"              // syscall
    
    // Nova linha
    "mov r0, #1\n"          // STDOUT
    "ldr r1, =newline\n"    // string "\n"
    "mov r2, #1\n"          // length = 1
    "mov r7, #4\n"          // SYS_write
    "swi #0\n"              // syscall
    
    // Próximo argumento
    "add r6, r6, #1\n"      // i++
    "b print_loop\n"
    
"exit_program:\n"
    // exit(0)
    "mov r0, #0\n"          // status = 0
    "mov r7, #1\n"          // SYS_exit
    "swi #0\n"              // syscall
    
    // Nunca deve chegar aqui
    "b .\n"                 // loop infinito

// Seção de dados
".section .rodata\n"
".align 2\n"
"newline:\n"
    ".asciz \"\\n\"\n"
);