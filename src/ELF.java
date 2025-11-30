// Classe ELF dentro de Lua.java
public class ELF {
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
}