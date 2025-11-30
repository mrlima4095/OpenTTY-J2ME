import java.util.*;
import java.io.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    
    // Registradores da CPU
    private int[] registers;
    private int eip;
    private int eflags;
    
    // Memória 
    private byte[] memory;
    private static final int MEMORY_SIZE = 256 * 1024;
    
    // Constantes dos registradores
    private static final int EAX = 0;
    private static final int EBX = 1;
    private static final int ECX = 2;
    private static final int EDX = 3;
    private static final int ESI = 4;
    private static final int EDI = 5;
    private static final int EBP = 6;
    private static final int ESP = 7;
    
    // Syscalls Linux
    private static final int SYS_EXIT = 1;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_BRK = 45;
    
    public ELF(OpenTTY midlet, Object stdout) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.registers = new int[8];
        this.memory = new byte[MEMORY_SIZE];
        reset();
    }
    
    private void reset() {
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        eip = 0;
        eflags = 0;
        
        // Inicializar memória com zeros
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
        
        // Stack pointer no topo da memória
        registers[ESP] = MEMORY_SIZE - 4;
        registers[EBP] = MEMORY_SIZE - 4;
    }
    
    public boolean load(InputStream is) {
        try {
            midlet.print("Carregando ELF...", stdout);
            
            // Ler e verificar magic number
            int b1 = is.read();
            int b2 = is.read();
            int b3 = is.read();
            int b4 = is.read();
            
            if (b1 != 0x7F || b2 != 'E' || b3 != 'L' || b4 != 'F') {
                midlet.print("Magic number ELF inválido", stdout);
                return false;
            }
            
            midlet.print("ELF válido detectado", stdout);
            
            // Ler o resto do header ELF (52 bytes)
            byte[] header = new byte[48];
            for (int i = 0; i < 48; i++) {
                int b = is.read();
                if (b == -1) {
                    midlet.print("Header ELF incompleto", stdout);
                    return false;
                }
                header[i] = (byte) b;
            }
            
            // Extrair informações importantes do header
            int entryPoint = readInt32(header, 24 - 4); // Entry point no offset 24
            int phoff = readInt32(header, 28 - 4);      // Program header offset
            int phentsize = readInt16(header, 42 - 4);  // Tamanho program header
            int phnum = readInt16(header, 44 - 4);      // Número de program headers
            
            midlet.print("Entry point: 0x" + Integer.toHexString(entryPoint), stdout);
            midlet.print("Program headers: " + phnum, stdout);
            midlet.print("Program header offset: 0x" + Integer.toHexString(phoff), stdout);
            
            // Pular para a tabela de program headers
            long currentPos = 52; // Já lemos 52 bytes
            long toSkip = phoff - currentPos;
            while (toSkip > 0) {
                long skipped = is.skip(toSkip);
                if (skipped <= 0) break;
                toSkip -= skipped;
            }
            
            // Processar cada program header
            for (int i = 0; i < phnum; i++) {
                byte[] phdr = new byte[phentsize];
                for (int j = 0; j < phentsize; j++) {
                    int b = is.read();
                    if (b == -1) {
                        midlet.print("Program header incompleto", stdout);
                        return false;
                    }
                    phdr[j] = (byte) b;
                }
                
                int type = readInt32(phdr, 0);
                if (type == 1) { // PT_LOAD - Segmento carregável
                    int offset = readInt32(phdr, 4);
                    int vaddr = readInt32(phdr, 8);
                    int filesz = readInt32(phdr, 16);
                    int memsz = readInt32(phdr, 20);
                    
                    midlet.print("Carregando segmento em 0x" + Integer.toHexString(vaddr) + 
                               " tamanho: " + filesz + " bytes", stdout);
                    
                    // Salvar posição atual
                    long savedPos = phoff + (i * phentsize) + phentsize;
                    
                    // Pular para o offset do segmento
                    long segmentSkip = offset - savedPos;
                    while (segmentSkip > 0) {
                        long skipped = is.skip(segmentSkip);
                        if (skipped <= 0) break;
                        segmentSkip -= skipped;
                    }
                    
                    // Ler o segmento para a memória
                    for (int j = 0; j < filesz; j++) {
                        int b = is.read();
                        if (b == -1) break;
                        if (vaddr + j < MEMORY_SIZE) {
                            memory[vaddr + j] = (byte) b;
                        }
                    }
                    
                    // Zerar BSS se necessário
                    if (memsz > filesz) {
                        for (int j = filesz; j < memsz; j++) {
                            if (vaddr + j < MEMORY_SIZE) {
                                memory[vaddr + j] = 0;
                            }
                        }
                    }
                    
                    // Voltar para próxima program header
                    long nextHeaderPos = phoff + ((i + 1) * phentsize);
                    long afterSegmentPos = offset + filesz;
                    long backSkip = nextHeaderPos - afterSegmentPos;
                    while (backSkip > 0) {
                        long skipped = is.skip(backSkip);
                        if (skipped <= 0) break;
                        backSkip -= skipped;
                    }
                }
            }
            
            // Configurar entry point
            eip = entryPoint;
            
            midlet.print("ELF carregado com sucesso. EIP: 0x" + Integer.toHexString(eip), stdout);
            return true;
            
        } catch (Exception e) {
            midlet.print("Erro no carregamento ELF: " + e.toString(), stdout);
            return false;
        }
    }
    
    public void run() {
        midlet.print("Iniciando execução...", stdout);
        
        try {
            int instructionCount = 0;
            int maxInstructions = 10000; // Limite de segurança
            
            while (eip < MEMORY_SIZE && eip >= 0 && instructionCount < maxInstructions) {
                if (eip >= MEMORY_SIZE - 8) {
                    midlet.print("EIP fora dos limites da memória", stdout);
                    break;
                }
                
                int opcode = memory[eip] & 0xFF;
                instructionCount++;
                
                // Decodificar e executar instrução
                switch (opcode) {
                    case 0xCD: // INT (system call)
                        if ((memory[eip + 1] & 0xFF) == 0x80) {
                            handleSyscall();
                            eip += 2;
                        } else {
                            eip++;
                        }
                        break;
                        
                    case 0xC3: // RET
                        eip = pop32();
                        break;
                        
                    case 0x90: // NOP
                        eip++;
                        break;
                        
                    case 0x50: // PUSH EAX
                        push32(registers[EAX]);
                        eip++;
                        break;
                        
                    case 0x53: // PUSH EBX
                        push32(registers[EBX]);
                        eip++;
                        break;
                        
                    case 0x51: // PUSH ECX
                        push32(registers[ECX]);
                        eip++;
                        break;
                        
                    case 0x52: // PUSH EDX
                        push32(registers[EDX]);
                        eip++;
                        break;
                        
                    case 0x58: // POP EAX
                        registers[EAX] = pop32();
                        eip++;
                        break;
                        
                    case 0x5B: // POP EBX
                        registers[EBX] = pop32();
                        eip++;
                        break;
                        
                    case 0x59: // POP ECX
                        registers[ECX] = pop32();
                        eip++;
                        break;
                        
                    case 0x5A: // POP EDX
                        registers[EDX] = pop32();
                        eip++;
                        break;
                        
                    case 0xB8: // MOV EAX, immediate32
                        registers[EAX] = read32(eip + 1);
                        eip += 5;
                        break;
                        
                    case 0xBB: // MOV EBX, immediate32
                        registers[EBX] = read32(eip + 1);
                        eip += 5;
                        break;
                        
                    case 0xB9: // MOV ECX, immediate32
                        registers[ECX] = read32(eip + 1);
                        eip += 5;
                        break;
                        
                    case 0xBA: // MOV EDX, immediate32
                        registers[EDX] = read32(eip + 1);
                        eip += 5;
                        break;
                        
                    case 0xEB: // JMP rel8
                        byte rel8 = (byte) (memory[eip + 1] & 0xFF);
                        eip += rel8 + 2;
                        break;
                        
                    case 0x74: // JE rel8
                        if ((eflags & 0x40) != 0) {
                            byte rel8_je = (byte) (memory[eip + 1] & 0xFF);
                            eip += rel8_je + 2;
                        } else {
                            eip += 2;
                        }
                        break;
                        
                    case 0x75: // JNE rel8
                        if ((eflags & 0x40) == 0) {
                            byte rel8_jne = (byte) (memory[eip + 1] & 0xFF);
                            eip += rel8_jne + 2;
                        } else {
                            eip += 2;
                        }
                        break;
                        
                    case 0x31: // XOR
                        if ((memory[eip + 1] & 0xFF) == 0xC0) { // XOR EAX, EAX
                            registers[EAX] = 0;
                            eflags |= 0x40; // Set zero flag
                            eip += 2;
                        } else {
                            eip++;
                        }
                        break;
                        
                    default:
                        // Instrução não implementada - pular 1 byte
                        eip++;
                        break;
                }
                
                // Verificar se programa terminou
                if (eip == 0 || registers[EAX] == 1) {
                    break;
                }
            }
            
            if (instructionCount >= maxInstructions) {
                midlet.print("Limite de instruções atingido: " + instructionCount, stdout);
            } else {
                midlet.print("Execução concluída após " + instructionCount + " instruções", stdout);
            }
            
        } catch (Exception e) {
            midlet.print("Erro durante execução: " + e.toString(), stdout);
        }
    }
    
    private void handleSyscall() {
        int syscall = registers[EAX];
        
        switch (syscall) {
            case SYS_EXIT:
                midlet.print("Programa finalizado com código: " + registers[EBX], stdout);
                eip = MEMORY_SIZE; // Forçar término
                break;
                
            case SYS_WRITE:
                int fd = registers[EBX];
                int buf = registers[ECX];
                int count = registers[EDX];
                
                if (fd == 1 || fd == 2) { // stdout ou stderr
                    StringBuffer output = new StringBuffer();
                    int bytesWritten = 0;
                    
                    for (int i = 0; i < count; i++) {
                        int addr = buf + i;
                        // Verificação de limites mais rigorosa
                        if (addr >= 0 && addr < MEMORY_SIZE) {
                            byte b = memory[addr];
                            // Converter byte para char de forma segura
                            char c = (char) (b & 0xFF);
                            output.append(c);
                            bytesWritten++;
                        } else {
                            // Se encontrar endereço inválido, para a escrita
                            break;
                        }
                    }
                    
                    if (output.length() > 0) {
                        midlet.print(output.toString(), stdout);
                    }
                    registers[EAX] = bytesWritten; // Retornar número real de bytes escritos
                } else {
                    registers[EAX] = -1; // Erro: file descriptor inválido
                }
                break;
                
            case SYS_READ:
                // Implementação básica de read para stdin
                if (registers[EBX] == 0) { // stdin
                    // Por enquanto, retornar 0 bytes lidos (não bloqueante)
                    registers[EAX] = 0;
                } else {
                    registers[EAX] = -1;
                }
                break;
                
            case SYS_OPEN:
                // Implementação simples para arquivos básicos
                String filename = readStringFromMemory(registers[EBX], 256);
                if ("/dev/stdout".equals(filename) || "/dev/stderr".equals(filename)) {
                    registers[EAX] = 1; // Retorna fd 1
                } else if ("/dev/stdin".equals(filename)) {
                    registers[EAX] = 0; // Retorna fd 0
                } else {
                    registers[EAX] = -1; // Arquivo não encontrado
                }
                break;
                
            case SYS_CLOSE:
                // Sempre sucesso para stdin/stdout/stderr
                registers[EAX] = 0;
                break;
                
            case SYS_BRK:
                // Implementação simples - sempre sucesso
                registers[EAX] = registers[EBX];
                break;
                
            default:
                midlet.print("Syscall não implementada: " + syscall, stdout);
                registers[EAX] = -1;
                break;
        }
    }

    // Método auxiliar para ler strings da memória
    private String readStringFromMemory(int addr, int maxLength) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < maxLength; i++) {
            if (addr + i >= MEMORY_SIZE) break;
            byte b = memory[addr + i];
            if (b == 0) break; // Null terminator
            sb.append((char) (b & 0xFF));
        }
        return sb.toString();
    }
    
    // Métodos auxiliares
    private int readInt32(byte[] data, int offset) {
        return (data[offset] & 0xFF) | 
               ((data[offset + 1] & 0xFF) << 8) | 
               ((data[offset + 2] & 0xFF) << 16) | 
               ((data[offset + 3] & 0xFF) << 24);
    }
    
    private int readInt16(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
    
    private int read32(int addr) {
        if (addr + 3 >= MEMORY_SIZE) return 0;
        return (memory[addr] & 0xFF) | 
               ((memory[addr + 1] & 0xFF) << 8) | 
               ((memory[addr + 2] & 0xFF) << 16) | 
               ((memory[addr + 3] & 0xFF) << 24);
    }
    
    private void push32(int value) {
        registers[ESP] -= 4;
        write32(registers[ESP], value);
    }
    
    private int pop32() {
        int value = read32(registers[ESP]);
        registers[ESP] += 4;
        return value;
    }
    
    private void write32(int addr, int value) {
        if (addr + 3 < MEMORY_SIZE) {
            memory[addr] = (byte) (value & 0xFF);
            memory[addr + 1] = (byte) ((value >> 8) & 0xFF);
            memory[addr + 2] = (byte) ((value >> 16) & 0xFF);
            memory[addr + 3] = (byte) ((value >> 24) & 0xFF);
        }
    }
}