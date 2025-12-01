import java.util.*;
import java.io.*;

// Adicione esta classe no mesmo arquivo Lua.java ou em um arquivo separado ELF.java
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
    private static final int REG_R7 = 7;
    private static final int REG_SP = 13;
    private static final int REG_LR = 14;
    private static final int REG_PC = 15;
    
    // Syscalls Linux ARM
    private static final int SYS_EXIT = 1;
    private static final int SYS_WRITE = 4;
    private static final int SYS_BRK = 45;
    
    public ELF(OpenTTY midlet, Object stdout) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.memory = new byte[64 * 1024]; // 64KB de memória
        this.registers = new int[16];
        this.running = false;
        this.stackPointer = memory.length - 1024; // Stack no final da memória
    }
    
    public boolean load(InputStream is) throws Exception {
        // Ler todo o arquivo para um array de bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
        byte[] elfData = baos.toByteArray();
        
        // Verificar assinatura ELF
        if (elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { midlet.print("Not a valid ELF file", stdout); return false; }
        
        // Verificar classe 32-bit
        if (elfData[4] != ELFCLASS32) { midlet.print("Only 32-bit ELF supported", stdout); return false; }
        
        // Verificar little-endian
        if (elfData[5] != ELFDATA2LSB) { midlet.print("Only little-endian ELF supported", stdout); return false; }
        
        // Ler cabeçalho ELF
        int e_type = readShortLE(elfData, 16);
        int e_machine = readShortLE(elfData, 18);
        int e_entry = readIntLE(elfData, 24);
        int e_phoff = readIntLE(elfData, 28);
        int e_phnum = readShortLE(elfData, 44);
        int e_phentsize = readShortLE(elfData, 42);
        
        // Verificar tipo e arquitetura
        if (e_type != ET_EXEC) { midlet.print("Not an executable ELF", stdout); return false; }
        if (e_machine != EM_ARM) { midlet.print("Not an ARM executable", stdout); return false; }
        
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
        
        try {
            while (running && pc < memory.length - 3) {
                // Buscar instrução
                int instruction = readIntLE(memory, pc);
                pc += 4;
                
                // Decodificar e executar
                executeInstruction(instruction);
            }
        } catch (Exception e) {
            midlet.print("ELF execution error: " + (e.getMessage() == null ? e.getClass().getName() : e.getMessage()), stdout);
            running = false;
        }
    }
    
    private void executeInstruction(int instruction) {
        // Verificar se é instrução de syscall (ARM OABI usa SWI 0)
        if ((instruction & 0xFF000000) == 0xEF000000) {
            int swi_number = instruction & 0x00FFFFFF;
            handleSyscall(swi_number);
            return;
        }
        
        // Verificar se é instrução de syscall EABI (usando R7)
        // Para EABI, o número da syscall está em R7
        if ((instruction & 0xFFF000F0) == 0xE7F000F0) {
            handleSyscall(registers[REG_R7]);
            return;
        }
        
        // Decodificação básica de outras instruções ARM
        int opcode = (instruction >> 21) & 0xF;
        int rn = (instruction >> 16) & 0xF;
        int rd = (instruction >> 12) & 0xF;
        
        // MOV Rd, #imm (Forma ARM)
        if ((instruction & 0x0FF00000) == 0x03A00000) {
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            registers[rd] = rotateRight(imm, rotate);
            return;
        }
        
        // ADD Rd, Rn, #imm
        if ((instruction & 0x0FF00000) == 0x02800000) {
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            registers[rd] = registers[rn] + rotateRight(imm, rotate);
            return;
        }
        
        // LDR Rd, [Rn, #offset]
        if ((instruction & 0x0E500000) == 0x04100000) {
            int offset = instruction & 0xFFF;
            boolean add = (instruction & (1 << 23)) != 0;
            int address = registers[rn];
            
            if (add) {
                address += offset;
            } else {
                address -= offset;
            }
            
            if (address >= 0 && address < memory.length - 3) {
                registers[rd] = readIntLE(memory, address);
            }
            return;
        }
        
        // STR Rd, [Rn, #offset]
        if ((instruction & 0x0E500000) == 0x04000000) {
            int offset = instruction & 0xFFF;
            boolean add = (instruction & (1 << 23)) != 0;
            int address = registers[rn];
            
            if (add) {
                address += offset;
            } else {
                address -= offset;
            }
            
            if (address >= 0 && address < memory.length - 3) {
                writeIntLE(memory, address, registers[rd]);
            }
            return;
        }
        
        // B/BL (branch)
        if ((instruction & 0x0E000000) == 0x0A000000) {
            int offset = instruction & 0x00FFFFFF;
            if ((offset & 0x00800000) != 0) {
                offset |= 0xFF000000; // Sign extend
            }
            offset <<= 2;
            
            if ((instruction & 0x01000000) != 0) {
                // BL (branch with link)
                registers[REG_LR] = pc - 4;
            }
            
            pc = pc + offset;
            return;
        }
        
        // NOP - não fazer nada
        if (instruction == 0xE1A00000) {
            return;
        }
        
        // Instrução não implementada - pular para próxima
        // Em um emulador real, aqui seria necessário implementar mais instruções
    }
    
    private void handleSyscall(int number) {
        switch (number) {
            case SYS_WRITE:
                // write(fd, buf, count)
                int fd = registers[REG_R0];
                int buf = registers[REG_R0 + 1];
                int count = registers[REG_R0 + 2];
                
                if (fd == 1 || fd == 2) { // stdout ou stderr
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < count && buf + i < memory.length; i++) {
                        sb.append((char) memory[buf + i]);
                    }
                    midlet.print(sb.toString(), stdout);
                    registers[REG_R0] = count; // Retorna número de bytes escritos
                }
                break;
                
            case SYS_EXIT:
                // exit(status)
                running = false;
                break;
                
            case SYS_BRK:
                // brk(addr) - simples implementação
                registers[REG_R0] = memory.length;
                break;
                
            default:
                midlet.print("Unsupported syscall: " + number, stdout);
                registers[REG_R0] = -1; // Retorna erro
                break;
        }
    }
    
    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); }
    
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    
    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }
}