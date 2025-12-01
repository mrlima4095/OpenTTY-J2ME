import java.util.*;
import java.io.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    
    // Memória e registradores
    private byte[] memory;
    private int[] registers;
    private int pc;
    private boolean running;
    private int stackPointer;
    
    // Constantes ELF
    private static final int EI_NIDENT = 16;
    private static final int ELFCLASS32 = 1;
    private static final int ELFDATA2LSB = 1;
    private static final int EM_ARM = 40;
    private static final int ET_EXEC = 2;
    private static final int PT_LOAD = 1;
    
    // Constantes ARM
    private static final int REG_R0 = 0;
    private static final int REG_R1 = 1;
    private static final int REG_R2 = 2;
    private static final int REG_R3 = 3;
    private static final int REG_R4 = 4;
    private static final int REG_R5 = 5;
    private static final int REG_R6 = 6;
    private static final int REG_R7 = 7;  // Número da syscall no EABI
    private static final int REG_SP = 13;
    private static final int REG_LR = 14;
    private static final int REG_PC = 15;
    
    // Syscalls Linux ARM (EABI)
    private static final int SYS_EXIT = 1;
    private static final int SYS_WRITE = 4;
    private static final int SYS_BRK = 45;
    
    public ELF(OpenTTY midlet, Object stdout) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.memory = new byte[1024 * 1024]; // 1MB de memória
        this.registers = new int[16];
        this.running = false;
        this.stackPointer = memory.length - 1024; // Stack no final da memória
    }
    
    public boolean load(InputStream is) throws Exception {
        // Ler todo o arquivo para um array de bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] elfData = baos.toByteArray();
        
        // Verificar assinatura ELF
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') {
            midlet.print("Not a valid ELF file", stdout);
            return false;
        }
        
        // Verificar classe 32-bit
        if (elfData[4] != ELFCLASS32) {
            midlet.print("Only 32-bit ELF supported", stdout);
            return false;
        }
        
        // Verificar little-endian
        if (elfData[5] != ELFDATA2LSB) {
            midlet.print("Only little-endian ELF supported", stdout);
            return false;
        }
        
        // Ler cabeçalho ELF
        int e_type = readShortLE(elfData, 16);
        int e_machine = readShortLE(elfData, 18);
        int e_entry = readIntLE(elfData, 24);
        int e_phoff = readIntLE(elfData, 28);
        int e_phnum = readShortLE(elfData, 44);
        int e_phentsize = readShortLE(elfData, 42);
        
        // Verificar tipo e arquitetura
        if (e_type != ET_EXEC) {
            midlet.print("Not an executable ELF", stdout);
            return false;
        }
        
        if (e_machine != EM_ARM) {
            midlet.print("Not an ARM executable", stdout);
            return false;
        }
        
        midlet.print("Loading ELF at entry point: 0x" + Integer.toHexString(e_entry), stdout);
        
        // Configurar PC inicial
        pc = e_entry;
        registers[REG_SP] = stackPointer;
        registers[REG_LR] = 0xFFFFFFFF; // Endereço de retorno inválido
        
        // Carregar segmentos
        for (int i = 0; i < e_phnum; i++) {
            int phdrOffset = e_phoff + i * e_phentsize;
            int p_type = readIntLE(elfData, phdrOffset);
            
            if (p_type == PT_LOAD) {
                int p_offset = readIntLE(elfData, phdrOffset + 4);
                int p_vaddr = readIntLE(elfData, phdrOffset + 8);
                int p_filesz = readIntLE(elfData, phdrOffset + 16);
                int p_memsz = readIntLE(elfData, phdrOffset + 20);
                
                midlet.print("Loading segment at 0x" + Integer.toHexString(p_vaddr) + 
                           ", size: " + p_filesz + " bytes", stdout);
                
                // Copiar dados para a memória
                for (int j = 0; j < p_filesz && j < memory.length; j++) {
                    if (p_vaddr + j < memory.length) {
                        memory[p_vaddr + j] = elfData[p_offset + j];
                    }
                }
                
                // Zerar memória restante (bss)
                for (int j = p_filesz; j < p_memsz; j++) {
                    if (p_vaddr + j < memory.length) {
                        memory[p_vaddr + j] = 0;
                    }
                }
            }
        }
        
        return true;
    }

    public void run() {
        running = true;
        int instructionCount = 0;
        
        try {
            // Dump da área onde está o pool literal
            midlet.print("Dump do pool literal (0x10090):", stdout);
            for (int i = 0; i < 32; i += 4) {
                int addr = 0x10090 + i;
                if (addr < memory.length - 3) {
                    int value = readIntLE(memory, addr);
                    midlet.print(String.format("0x%08x: 0x%08x", addr, value), stdout);
                }
            }
            
            // Dump da área de dados (onde a string deveria estar)
            midlet.print("Dump da área de dados (0x20090):", stdout);
            for (int i = 0; i < 32; i++) {
                int addr = 0x20090 + i;
                if (addr < memory.length) {
                    midlet.print(String.format("0x%08x: 0x%02x '%c'", 
                        addr, memory[addr] & 0xFF, 
                        (memory[addr] >= 32 && memory[addr] < 127) ? (char)memory[addr] : '.'), stdout);
                }
            }
            
            while (running && pc < memory.length - 3 && instructionCount < 10000) {
                // Buscar instrução
                int instruction = readIntLE(memory, pc);
                
                // Debug
                midlet.print(String.format("PC=0x%08x I=0x%08x", pc, instruction), stdout);
                
                pc += 4;
                instructionCount++;
                
                // Decodificar e executar
                executeInstruction(instruction);
            }
            
            if (!running) {
                midlet.print("Program exited", stdout);
            } else if (instructionCount >= 10000) {
                midlet.print("Instruction limit reached", stdout);
            }
            
        } catch (Exception e) {
            midlet.print("ELF execution error: " + e.toString(), stdout);
            e.printStackTrace();
            running = false;
        }
    }

    private void executeInstruction(int instruction) {
        // Primeiro, verificar se é uma instrução de syscall (EABI)
        // ARM EABI usa SWI 0 com número da syscall em R7
        if ((instruction & 0x0F000000) == 0x0F000000) {
            int swi_number = instruction & 0x00FFFFFF;
            
            // Para EABI, swi_number deve ser 0
            if (swi_number == 0) {
                handleSyscall(registers[REG_R7]);
            } else {
                // OABI antigo - usar número direto da instrução
                handleSyscall(swi_number);
            }
            return;
        }
        
        // Decodificação de instruções ARM básicas
        
        // Data Processing Instructions
        if ((instruction & 0x0C000000) == 0x00000000) {
            int opcode = (instruction >> 21) & 0xF;
            int rn = (instruction >> 16) & 0xF;
            int rd = (instruction >> 12) & 0xF;
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            int shifter_operand = rotateRight(imm, rotate);
            
            switch (opcode) {
                case 13: // MOV (immediate)
                    registers[rd] = shifter_operand;
                    break;
                case 4: // ADD (immediate)
                    registers[rd] = registers[rn] + shifter_operand;
                    break;
                case 2: // SUB (immediate)
                    registers[rd] = registers[rn] - shifter_operand;
                    break;
                case 0: // AND (immediate)
                    registers[rd] = registers[rn] & shifter_operand;
                    break;
                case 12: // ORR (immediate)
                    registers[rd] = registers[rn] | shifter_operand;
                    break;
                case 14: // BIC (immediate)
                    registers[rd] = registers[rn] & ~shifter_operand;
                    break;
                case 8: // TST (immediate)
                    // Só atualiza flags, mas não implementado
                    int result = registers[rn] & shifter_operand;
                    break;
                default:
                    // Instrução não implementada
                    break;
            }
            return;
        }
        
        // Load/Store Instructions
        if ((instruction & 0x0C000000) == 0x04000000) {
            int rn = (instruction >> 16) & 0xF;
            int rd = (instruction >> 12) & 0xF;
            int offset = instruction & 0xFFF;
            boolean isLoad = (instruction & (1 << 20)) != 0;
            boolean isByte = (instruction & (1 << 22)) != 0;
            boolean addOffset = (instruction & (1 << 23)) != 0;
            boolean preIndexed = (instruction & (1 << 24)) != 0;
            
            int base;
            if (rn == REG_PC) {
                // ARM: PC tem valor de endereço atual + 8
                base = pc - 4 + 8; // pc já incrementou para próxima instrução
            } else {
                base = registers[rn];
            }
            
            int address = base;
            
            if (preIndexed) {
                if (addOffset) {
                    address += offset;
                } else {
                    address -= offset;
                }
            }
            
            if (isLoad) {
                // LDR ou LDRB
                if (address >= 0 && address < memory.length - 3) {
                    if (isByte) {
                        registers[rd] = memory[address] & 0xFF;
                    } else {
                        // LDR palavra
                        registers[rd] = readIntLE(memory, address);
                        
                        // Debug para R1
                        if (rd == 1) {
                            midlet.print(String.format("LDR R1 from 0x%08x = 0x%08x", 
                                address, registers[rd]), stdout);
                        }
                    }
                } else {
                    midlet.print(String.format("Erro: acesso de memória inválido em 0x%08x", 
                        address), stdout);
                }
            } else {
                // STR ou STRB
                if (address >= 0 && address < memory.length - 3) {
                    if (isByte) {
                        memory[address] = (byte)(registers[rd] & 0xFF);
                    } else {
                        writeIntLE(memory, address, registers[rd]);
                    }
                }
            }
            
            if (!preIndexed) {
                // Post-indexed
                if (addOffset) {
                    registers[rn] += offset;
                } else {
                    registers[rn] -= offset;
                }
            }
            return;
        }
        
        // Branch Instructions
        if ((instruction & 0x0E000000) == 0x0A000000) {
            int offset = instruction & 0x00FFFFFF;
            // Sign extend 24-bit offset
            if ((offset & 0x00800000) != 0) {
                offset |= 0xFF000000;
            }
            offset <<= 2;
            
            boolean link = (instruction & (1 << 24)) != 0;
            
            if (link) {
                // BL (Branch with Link)
                registers[REG_LR] = pc - 4;
            }
            
            // Atualizar PC (já temos +4 no PC da instrução atual)
            pc = pc + offset - 4;
            return;
        }
        
        // LDR/STR com registrador de offset
        if ((instruction & 0x0E000000) == 0x06000000) {
            int rn = (instruction >> 16) & 0xF;
            int rd = (instruction >> 12) & 0xF;
            int rm = instruction & 0xF;
            boolean isLoad = (instruction & (1 << 20)) != 0;
            boolean addOffset = (instruction & (1 << 23)) != 0;
            
            int address = registers[rn];
            int offset = registers[rm];
            
            if (addOffset) {
                address += offset;
            } else {
                address -= offset;
            }
            
            if (isLoad) {
                // LDR
                if (address >= 0 && address < memory.length - 3) {
                    registers[rd] = readIntLE(memory, address);
                }
            } else {
                // STR
                if (address >= 0 && address < memory.length - 3) {
                    writeIntLE(memory, address, registers[rd]);
                }
            }
            return;
        }
        
        // NOP - não fazer nada
        if (instruction == 0xE1A00000) {
            return;
        }
        
        // Instrução não reconhecida
        // Pular para próxima instrução
    }
    
    private void handleSyscall(int number) {
        midlet.print("Syscall " + number + " called", stdout);
        
        switch (number) {
            case SYS_WRITE:  // 4
                // write(fd, buf, count)
                int fd = registers[REG_R0];
                int buf = registers[REG_R1];
                int count = registers[REG_R2];
                
                midlet.print("Write: fd=" + fd + ", buf=0x" + Integer.toHexString(buf) + 
                           ", count=" + count, stdout);
                
                if (fd == 1 || fd == 2) { // stdout ou stderr
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < count && buf + i < memory.length && i < 256; i++) {
                        sb.append((char)(memory[buf + i] & 0xFF));
                    }
                    midlet.print(sb.toString(), stdout);
                    registers[REG_R0] = count; // Retorna número de bytes escritos
                } else {
                    registers[REG_R0] = -1; // Erro
                }
                break;
                
            case SYS_EXIT:  // 1
                // exit(status)
                int status = registers[REG_R0];
                midlet.print("Exit with status: " + status, stdout);
                running = false;
                break;
                
            case SYS_BRK:  // 45
                // brk(addr) - implementação simples
                registers[REG_R0] = memory.length;
                break;
                
            default:
                midlet.print("Unsupported syscall: " + number, stdout);
                registers[REG_R0] = -1; // Retorna erro
                break;
        }
    }

    private void dumpMemory(int address, int length) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < length; i += 4) {
            if (address + i < memory.length - 3) {
                int value = readIntLE(memory, address + i);
                sb.append(String.format("0x%08x: 0x%08x\n", address + i, value));
            }
        }
        midlet.print(sb.toString(), stdout);
    }
    
    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); }
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
    
    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    
    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }
}