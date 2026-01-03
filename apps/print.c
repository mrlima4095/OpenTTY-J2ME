#define STDOUT_FILENO 1

// Ponto de entrada totalmente em assembly
asm(
".global _start\n"
"_start:\n"
    // argc está em r0, argv em r1
    "push {r0, r1, lr}\n"      // Salvar registradores
    
    // Imprimir "argc = "
    "ldr r0, =msg_argc\n"
    "mov r1, #7\n"            // Comprimento da string
    "bl print_msg\n"
    
    // Imprimir número (argc)
    "pop {r0, r1, lr}\n"      // Restaurar argc
    "push {r0, r1, lr}\n"     // Salvar novamente
    "bl print_num\n"
    
    // Imprimir nova linha
    "ldr r0, =newline\n"
    "mov r1, #1\n"
    "bl print_msg\n"
    
    // Imprimir argumentos
    "pop {r0, r1, lr}\n"      // r0=argc, r1=argv
    "cmp r0, #0\n"
    "beq exit\n"
    
    "mov r4, r0\n"            // r4 = argc
    "mov r5, r1\n"            // r5 = argv
    "mov r6, #0\n"            // r6 = i = 0
    
"print_loop:\n"
    // Imprimir "argv["
    "ldr r0, =msg_argv\n"
    "mov r1, #6\n"
    "bl print_msg\n"
    
    // Imprimir índice
    "mov r0, r6\n"
    "bl print_num\n"
    
    // Imprimir "] = "
    "ldr r0, =msg_equals\n"
    "mov r1, #4\n"
    "bl print_msg\n"
    
    // Imprimir string do argumento
    "ldr r0, [r5, r6, lsl #2]\n"  // argv[i]
    "bl print_string\n"
    
    // Nova linha
    "ldr r0, =newline\n"
    "mov r1, #1\n"
    "bl print_msg\n"
    
    // Próximo argumento
    "add r6, r6, #1\n"
    "cmp r6, r4\n"
    "blt print_loop\n"
    
"exit:\n"
    // Sair com código 0
    "mov r0, #0\n"
    "mov r7, #1\n"      // SYS_exit
    "swi #0\n"
    
    // Loop infinito se syscall falhar
    "b .\n"

// Função: print_msg(r0=string, r1=length)
"print_msg:\n"
    "push {r7, lr}\n"
    "mov r2, r1\n"      // length
    "mov r1, r0\n"      // string
    "mov r0, #1\n"      // STDOUT_FILENO
    "mov r7, #4\n"      // SYS_write
    "swi #0\n"
    "pop {r7, pc}\n"

// Função: print_string(r0=string)
"print_string:\n"
    "push {r4, lr}\n"
    "mov r4, r0\n"      // Salvar ponteiro da string
    
    // Calcular comprimento
    "mov r1, #0\n"
"strlen_loop:\n"
    "ldrb r2, [r4, r1]\n"
    "cmp r2, #0\n"
    "beq strlen_done\n"
    "add r1, r1, #1\n"
    "b strlen_loop\n"
"strlen_done:\n"
    
    // Imprimir string
    "mov r0, #1\n"      // STDOUT_FILENO
    "mov r2, r1\n"      // length
    "mov r1, r4\n"      // string
    "mov r7, #4\n"      // SYS_write
    "swi #0\n"
    
    "pop {r4, pc}\n"

// Função: print_num(r0=number)
"print_num:\n"
    "push {r4, r5, r6, r7, lr}\n"
    "sub sp, sp, #16\n"  // Buffer na stack
    
    "mov r4, r0\n"       // Número
    "mov r5, sp\n"       // Buffer
    "mov r6, #0\n"       // Índice
    
    // Tratar negativo
    "cmp r4, #0\n"
    "bge convert\n"
    "mov r0, #1\n"
    "mov r1, #45\n"      // '-'
    "strb r1, [r5]\n"
    "add r5, r5, #1\n"
    "neg r4, r4\n"
    
"convert:\n"
    // Converter dígitos
    "mov r0, r4\n"
    "mov r1, #10\n"
"convert_loop:\n"
    "bl divide\n"        // r0 / 10, resto em r1
    "add r1, r1, #48\n"  // Converter para ASCII
    "strb r1, [r5, r6]\n"
    "add r6, r6, #1\n"
    "cmp r0, #0\n"
    "bne convert_loop\n"
    
    // Inverter string
    "mov r7, #0\n"       // i = 0
"reverse_loop:\n"
    "sub r2, r6, r7\n"
    "sub r2, r2, #1\n"   // j = len-i-1
    "cmp r7, r2\n"
    "bge reverse_done\n"
    
    // Trocar buffer[i] e buffer[j]
    "ldrb r0, [r5, r7]\n"
    "ldrb r1, [r5, r2]\n"
    "strb r1, [r5, r7]\n"
    "strb r0, [r5, r2]\n"
    
    "add r7, r7, #1\n"
    "b reverse_loop\n"
    
"reverse_done:\n"
    // Imprimir número
    "mov r0, #1\n"       // STDOUT_FILENO
    "mov r1, sp\n"       // Buffer
    "mov r2, r6\n"       // Comprimento
    "mov r7, #4\n"       // SYS_write
    "swi #0\n"
    
    "add sp, sp, #16\n"
    "pop {r4, r5, r6, r7, pc}\n"

// Função: divide(r0=dividendo, r1=divisor) -> r0=quociente, r1=resto
"divide:\n"
    "mov r2, #0\n"       // Quociente
"divide_loop:\n"
    "cmp r0, r1\n"
    "blt divide_done\n"
    "sub r0, r0, r1\n"
    "add r2, r2, #1\n"
    "b divide_loop\n"
"divide_done:\n"
    "mov r1, r0\n"       // Resto
    "mov r0, r2\n"       // Quociente
    "bx lr\n"

// Strings
"msg_argc: .asciz \"argc = \"\n"
"msg_argv: .asciz \"argv[\"\n"
"msg_equals: .asciz \"] = \"\n"
"newline: .asciz \"\\n\"\n"
".align 2\n"
);