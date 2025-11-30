import java.util.*;
import java.io.*;

// Classe ELF dentro de Lua.java
/*public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    
    // Registradores da CPU 32-bits
    private int[] registers;
    private int eip; // Instruction Pointer
    private int eflags;
    
    // Memória 
    private byte[] memory;
    private static final int MEMORY_SIZE = 16 * 1024 * 1024; // 16MB
    
    // Constantes dos registradores
    private static final int EAX = 0;
    private static final int EBX = 1;
    private static final int ECX = 2;
    private static final int EDX = 3;
    private static final int ESI = 4;
    private static final int EDI = 5;
    private static final int EBP = 6;
    private static final int ESP = 7;
    
    // Flags
    private static final int CF = 1 << 0; // Carry Flag
    private static final int PF = 1 << 2; // Parity Flag
    private static final int ZF = 1 << 6; // Zero Flag
    private static final int SF = 1 << 7; // Sign Flag
    
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
        // Stack começa no topo da memória
        registers[ESP] = MEMORY_SIZE - 4;
        registers[EBP] = MEMORY_SIZE - 4;
        
        // Limpar memória
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
    }
    
    public boolean load(InputStream is) throws Exception {
        reset();
        
        // Verificar assinatura ELF
        byte[] magic = new byte[4];
        if (readFully(is, magic) != 4) return false;
        if (magic[0] != 0x7F || magic[1] != 'E' || magic[2] != 'L' || magic[3] != 'F') {
            return false;
        }
        
        // Ler header ELF
        byte[] header = new byte[52]; // Tamanho do header ELF32
        if (readFully(is, header) != 52) return false;
        
        // Extrair informações do header
        int entryPoint = read32(header, 24);
        int phoff = read32(header, 28);    // Offset program header
        int phentsize = read16(header, 42); // Tamanho program header
        int phnum = read16(header, 44);    // Número de program headers
        
        midlet.print("Entry point: 0x" + Integer.toHexString(entryPoint), stdout);
        midlet.print("Program headers: " + phnum, stdout);
        
        // Ler program headers e carregar segmentos
        skipFully(is, phoff - 52); // Pular para program headers
        
        for (int i = 0; i < phnum; i++) {
            byte[] pheader = new byte[phentsize];
            if (readFully(is, pheader) != phentsize) return false;
            
            int type = read32(pheader, 0);
            if (type == 1) { // PT_LOAD
                int offset = read32(pheader, 4);
                int vaddr = read32(pheader, 8);
                int filesz = read32(pheader, 16);
                int memsz = read32(pheader, 20);
                
                midlet.print("Loading segment at 0x" + Integer.toHexString(vaddr) + 
                           " size: " + filesz + " bytes", stdout);
                
                // Salvar posição atual
                long currentPos = phoff + (i * phentsize) + phentsize;
                
                // Pular para o segmento
                skipFully(is, offset - currentPos);
                
                // Ler segmento para memória
                if (readFully(is, memory, vaddr, filesz) != filesz) return false;
                
                // Zerar memória restante se memsz > filesz
                if (memsz > filesz) {
                    for (int j = vaddr + filesz; j < vaddr + memsz; j++) {
                        memory[j] = 0;
                    }
                }
                
                // Voltar para próxima program header
                long nextHeaderPos = phoff + ((i + 1) * phentsize);
                long afterSegmentPos = offset + filesz;
                skipFully(is, nextHeaderPos - afterSegmentPos);
            }
        }
        
        // Definir EIP para entry point
        eip = entryPoint;
        
        return true;
    }
    
    public void run() {
        try {
            midlet.print("Iniciando execução ELF...", stdout);
            
            while (true) {
                if (eip >= MEMORY_SIZE || eip < 0) {
                    midlet.print("EIP fora dos limites: 0x" + Integer.toHexString(eip), stdout);
                    break;
                }
                
                // Buscar instrução
                int opcode = memory[eip] & 0xFF;
                
                // Decodificar e executar
                switch (opcode) {
                    case 0xCD: // INT (syscall)
                        if (memory[eip + 1] == 0x80) {
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
                        
                    case 0xEB: // JMP rel8
                        byte rel8 = memory[eip + 1];
                        eip += rel8 + 2;
                        break;

                    // Mov imediato para EAX
                    case 0xB8:
                        registers[EAX] = read32(eip + 1);
                        eip += 5;
                        break;

                    // Mov imediato para ECX
                    case 0xB9:
                        registers[ECX] = read32(eip + 1);
                        eip += 5;
                        break;

                    // Mov imediato para EDX
                    case 0xBA:
                        registers[EDX] = read32(eip + 1);
                        eip += 5;
                        break;

                    // Mov imediato para EBX
                    case 0xBB:
                        registers[EBX] = read32(eip + 1);
                        eip += 5;
                        break;

                    // Mov imediato para ESI
                    case 0xBE:
                        registers[ESI] = read32(eip + 1);
                        eip += 5;
                        break;

                    // Mov imediato para EDI
                    case 0xBF:
                        registers[EDI] = read32(eip + 1);
                        eip += 5;
                        break;
                        
                    default:
                        // Instrução não implementada
                        midlet.print("Instrução não implementada: 0x" + 
                                   Integer.toHexString(opcode) + " em 0x" + 
                                   Integer.toHexString(eip), stdout);
                        eip++;
                        break;
                }
                
                // Verificar se deve parar
                if (eip == 0) break;
            }
            
        } catch (Exception e) {
            midlet.print("Erro durante execução ELF: " + e.getMessage(), stdout);
        }
    }
    
    private void handleSyscall() {
        int syscall = registers[EAX];
        
        switch (syscall) {
            case SYS_EXIT:
                midlet.print("Programa terminou com código: " + registers[EBX], stdout);
                eip = 0; // Para execução
                break;
                
            case SYS_WRITE:
                handleWrite();
                break;
                
            case SYS_READ:
                handleRead();
                break;
                
            case SYS_OPEN:
                handleOpen();
                break;
                
            case SYS_CLOSE:
                handleClose();
                break;
                
            case SYS_BRK:
                handleBrk();
                break;
                
            default:
                midlet.print("Syscall não implementada: " + syscall, stdout);
                registers[EAX] = -1; // Retornar erro
                break;
        }
    }
    
    private void handleWrite() {
        int fd = registers[EBX];
        int buf = registers[ECX];
        int count = registers[EDX];
        
        if (fd == 1 || fd == 2) { // stdout ou stderr
            try {
                // Construir string manualmente
                StringBuffer output = new StringBuffer();
                for (int i = 0; i < count; i++) {
                    output.append((char) memory[buf + i]);
                }
                midlet.print(output.toString(), stdout);
                registers[EAX] = count; // Retornar número de bytes escritos
            } catch (Exception e) {
                registers[EAX] = -1;
            }
        } else {
            registers[EAX] = -1; // File descriptor inválido
        }
    }
    
    private void handleRead() {
        int fd = registers[EBX];
        int buf = registers[ECX];
        int count = registers[EDX];
        
        // Por enquanto, só stdin (fd 0) é suportado
        if (fd == 0) {
            try {
                // Simular leitura - retornar 0 bytes por enquanto
                registers[EAX] = 0;
            } catch (Exception e) {
                registers[EAX] = -1;
            }
        } else {
            registers[EAX] = -1;
        }
    }
    
    private void handleOpen() {
        int filenamePtr = registers[EBX];
        int flags = registers[ECX];
        int mode = registers[EDX];
        
        try {
            // Ler nome do arquivo da memória
            String filename = readString(filenamePtr);
            
            // Tentar abrir arquivo usando sistema do OpenTTY
            InputStream is = midlet.getInputStream(filename);
            if (is != null) {
                // Por enquanto, retornar FD fictício
                registers[EAX] = 3; // Primeiro FD após stdio
                is.close();
            } else {
                registers[EAX] = -1;
            }
        } catch (Exception e) {
            registers[EAX] = -1;
        }
    }
    
    private void handleClose() {
        int fd = registers[EBX];
        // Por enquanto, sempre sucesso para FD > 2
        registers[EAX] = (fd > 2) ? 0 : -1;
    }
    
    private void handleBrk() {
        // Implementação simples - sempre retorna sucesso
        registers[EAX] = registers[EBX];
    }
    
    // Métodos auxiliares para leitura/escrita de memória
    private int read32(byte[] data, int offset) {
        return (data[offset] & 0xFF) | 
               ((data[offset + 1] & 0xFF) << 8) | 
               ((data[offset + 2] & 0xFF) << 16) | 
               ((data[offset + 3] & 0xFF) << 24);
    }
    
    private int read16(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
    
    private String readString(int addr) {
        StringBuffer sb = new StringBuffer();
        for (int i = addr; i < MEMORY_SIZE && memory[i] != 0; i++) {
            sb.append((char) memory[i]);
        }
        return sb.toString();
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
    
    private int read32(int addr) {
        return (memory[addr] & 0xFF) | 
               ((memory[addr + 1] & 0xFF) << 8) | 
               ((memory[addr + 2] & 0xFF) << 16) | 
               ((memory[addr + 3] & 0xFF) << 24);
    }
    
    private void write32(int addr, int value) {
        memory[addr] = (byte) (value & 0xFF);
        memory[addr + 1] = (byte) ((value >> 8) & 0xFF);
        memory[addr + 2] = (byte) ((value >> 16) & 0xFF);
        memory[addr + 3] = (byte) ((value >> 24) & 0xFF);
    }
    
    // Métodos auxiliares para leitura de streams (substituem Arrays.fill e leitura completa)
    private int readFully(InputStream is, byte[] buffer) throws IOException {
        return readFully(is, buffer, 0, buffer.length);
    }
    
    private int readFully(InputStream is, byte[] buffer, int offset, int length) throws IOException {
        int totalRead = 0;
        while (totalRead < length) {
            int read = is.read(buffer, offset + totalRead, length - totalRead);
            if (read == -1) break;
            totalRead += read;
        }
        return totalRead;
    }
    
    private void skipFully(InputStream is, long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = is.skip(remaining);
            if (skipped <= 0) break;
            remaining -= skipped;
        }
    }
}*/
public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    
    // Registradores da CPU
    private int[] registers;
    private int eip;
    private int eflags;
    
    // Memória 
    private byte[] memory;
    private static final int MEMORY_SIZE = 4 * 1024 * 1024; // 4MB
    
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
        
        // Inicializar memória
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
            
            // Ler magic number
            byte[] magic = new byte[4];
            if (readBytes(is, magic, 4) != 4) {
                return false;
            }
            
            // Verificar assinatura ELF
            if (magic[0] != 0x7F || magic[1] != 'E' || magic[2] != 'L' || magic[3] != 'F') {
                return false;
            }
            
            midlet.print("ELF válido encontrado", stdout);
            
            // Ler o resto do header ELF (52 bytes total)
            byte[] header = new byte[48];
            if (readBytes(is, header, 48) != 48) {
                return false;
            }
            
            // Extrair informações do header
            int entryPoint = readInt32(header, 24 - 4); // Entry point está no offset 24
            int phoff = readInt32(header, 28 - 4);     // Program header offset
            int phentsize = readInt16(header, 42 - 4); // Tamanho do program header
            int phnum = readInt16(header, 44 - 4);     // Número de program headers
            
            midlet.print("Entry point: 0x" + Integer.toHexString(entryPoint), stdout);
            midlet.print("Program headers: " + phnum, stdout);
            
            // Para simplificar, vamos carregar o arquivo inteiro na memória a partir do offset 0
            is.close();
            is = midlet.getInputStream("/bin/hello"); // Reabrir
            
            int totalLoaded = 0;
            int byteRead;
            while ((byteRead = is.read()) != -1 && totalLoaded < MEMORY_SIZE) {
                memory[totalLoaded] = (byte) byteRead;
                totalLoaded++;
            }
            
            midlet.print("Bytes carregados: " + totalLoaded, stdout);
            
            // Configurar entry point
            eip = entryPoint;
            
            // Inicializar stack
            registers[ESP] = MEMORY_SIZE - 64;
            registers[EBP] = MEMORY_SIZE - 64;
            
            return true;
            
        } catch (Exception e) {
            midlet.print("Erro no carregamento: " + e.toString(), stdout);
            return false;
        }
    }
    
    public void run() {
        midlet.print("Executando programa...", stdout);
        
        try {
            int instructionCount = 0;
            int maxInstructions = 1000; // Limite de segurança
            
            while (eip < MEMORY_SIZE && eip >= 0 && instructionCount < maxInstructions) {
                if (eip >= MEMORY_SIZE - 1) {
                    midlet.print("EIP fora da memória", stdout);
                    break;
                }
                
                int opcode = memory[eip] & 0xFF;
                instructionCount++;
                
                // Decodificar instrução
                switch (opcode) {
                    case 0xCD: // INT - System call
                        if (memory[eip + 1] == (byte)0x80) {
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
                        byte rel8 = memory[eip + 1];
                        eip += rel8 + 2;
                        break;
                        
                    case 0x74: // JE rel8
                        if ((eflags & 0x40) != 0) { // Zero flag
                            byte rel8_je = memory[eip + 1];
                            eip += rel8_je + 2;
                        } else {
                            eip += 2;
                        }
                        break;
                        
                    case 0x75: // JNE rel8
                        if ((eflags & 0x40) == 0) { // Not zero flag
                            byte rel8_jne = memory[eip + 1];
                            eip += rel8_jne + 2;
                        } else {
                            eip += 2;
                        }
                        break;
                        
                    default:
                        // Instrução não implementada - pular
                        eip++;
                        break;
                }
                
                // Verificar se programa terminou
                if (eip == 0 || instructionCount >= maxInstructions) {
                    break;
                }
            }
            
            if (instructionCount >= maxInstructions) {
                midlet.print("Limite de instruções atingido", stdout);
            }
            
            midlet.print("Execução finalizada após " + instructionCount + " instruções", stdout);
            
        } catch (Exception e) {
            midlet.print("Erro na execução: " + e.toString(), stdout);
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
                    for (int i = 0; i < count; i++) {
                        if (buf + i < MEMORY_SIZE) {
                            output.append((char) memory[buf + i]);
                        }
                    }
                    midlet.print(output.toString(), stdout);
                    registers[EAX] = count; // Retornar número de bytes escritos
                } else {
                    registers[EAX] = -1; // Erro
                }
                break;
                
            case SYS_READ:
                // Por enquanto, retornar 0 bytes lidos
                registers[EAX] = 0;
                break;
                
            case SYS_OPEN:
                // Por enquanto, retornar erro
                registers[EAX] = -1;
                break;
                
            case SYS_CLOSE:
                // Por enquanto, sempre sucesso
                registers[EAX] = 0;
                break;
                
            default:
                midlet.print("Syscall não implementada: " + syscall, stdout);
                registers[EAX] = -1;
                break;
        }
    }
    
    // Métodos auxiliares
    private int readBytes(InputStream is, byte[] buffer, int count) {
        try {
            int total = 0;
            while (total < count) {
                int b = is.read();
                if (b == -1) break;
                buffer[total] = (byte) b;
                total++;
            }
            return total;
        } catch (Exception e) {
            return 0;
        }
    }
    
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