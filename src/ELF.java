import java.util.*;
import java.io.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    
    // Registradores ARM
    private int[] reg = new int[16]; // r0-r15, onde r15 = PC, r14 = LR, r13 = SP
    private boolean cpsrN = false, cpsrZ = false, cpsrC = false, cpsrV = false;
    
    // Memória
    private byte[] memory = new byte[64 * 1024]; // 64KB
    private int memBase = 0x8000;
    
    // Estado
    private boolean running = true;
    
    public ELF(OpenTTY midlet, Object stdout) {
        this.midlet = midlet;
        this.stdout = stdout;
        reset();
    }
    
    private void reset() {
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        reg[13] = memory.length - 4; // SP no topo da memória
        reg[14] = 0; // LR
        reg[15] = 0; // PC
        
        for (int i = 0; i < memory.length; i++) {
            memory[i] = 0;
        }
    }
    
    public boolean load(InputStream is) throws IOException {
        reset();
        
        // Ler cabeçalho ELF
        byte[] header = new byte[52];
        int read = is.read(header);
        if (read != 52) return false;
        
        // Verificar assinatura ELF
        if (header[0] != 0x7F || header[1] != 'E' || header[2] != 'L' || header[3] != 'F') {
            midlet.print("Not an ELF file", stdout);
            return false;
        }
        
        // Verificar se é ARM (0x28)
        int machine = ((header[18] & 0xFF) << 8) | (header[19] & 0xFF);
        if (machine != 0x28) { // EM_ARM
            midlet.print("Not ARM executable", stdout);
            return false;
        }
        
        // Obter entrada do programa
        int e_entry = read32(header, 24);
        reg[15] = e_entry;
        
        // Carregar segmentos (program headers simplificado)
        int phoff = read32(header, 28);
        int phentsize = read16(header, 42);
        int phnum = read16(header, 44);
        
        // Para simplificar, vamos ler todo o arquivo na memória
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] fileData = baos.toByteArray();
        
        // Copiar o arquivo inteiro para a memória a partir do offset base
        System.arraycopy(fileData, 0, memory, memBase, Math.min(fileData.length, memory.length - memBase));
        
        midlet.print("ELF loaded, entry: 0x" + Integer.toHexString(e_entry), stdout);
        return true;
    }
    
    public void run() {
        try {
            while (running && reg[15] < memory.length - 4) {
                int pc = reg[15];
                int instruction = read32(memory, pc);
                
                // Decodificar instrução ARM
                decodeAndExecute(instruction);
                
                // Incrementar PC (4 bytes para ARM)
                reg[15] += 4;
                
                // Limitar ciclos para evitar loop infinito
                if (pc == reg[15]) { // Se PC não mudou
                    midlet.print("Warning: PC stuck at 0x" + Integer.toHexString(pc), stdout);
                    break;
                }
            }
        } catch (Exception e) {
            midlet.print("ELF execution error: " + e.toString(), stdout);
        }
    }
    
    private void decodeAndExecute(int instruction) {
        // Verificar se é instrução de branch (condicional)
        int cond = (instruction >> 28) & 0xF;
        if (!checkCondition(cond)) {
            return; // Condição não satisfeita
        }
        
        int opcode = (instruction >> 21) & 0x7;
        int rn = (instruction >> 16) & 0xF;
        int rd = (instruction >> 12) & 0xF;
        int rm = instruction & 0xF;
        
        // Decodificação básica (apenas algumas instruções)
        if ((instruction & 0x0FC00000) == 0x02800000) { // ADD
            int imm = instruction & 0xFF;
            int shift = ((instruction >> 8) & 0xF) * 2;
            imm = rotateRight(imm, shift);
            reg[rd] = reg[rn] + imm;
        }
        else if ((instruction & 0x0FC00000) == 0x02400000) { // SUB
            int imm = instruction & 0xFF;
            int shift = ((instruction >> 8) & 0xF) * 2;
            imm = rotateRight(imm, shift);
            reg[rd] = reg[rn] - imm;
        }
        else if ((instruction & 0x0FF00000) == 0x01A00000) { // MOV
            int imm = instruction & 0xFF;
            int shift = ((instruction >> 8) & 0xF) * 2;
            reg[rd] = rotateRight(imm, shift);
        }
        else if ((instruction & 0x0F000000) == 0x0A000000) { // B
            int offset = instruction & 0xFFFFFF;
            if ((offset & 0x800000) != 0) {
                offset |= 0xFF000000; // Sinal negativo
            }
            offset <<= 2;
            reg[15] = reg[15] + offset;
        }
        else if ((instruction & 0x0FF000F0) == 0x01200010) { // BX
            reg[15] = reg[rm];
        }
        else if ((instruction & 0x0E000000) == 0x08000000) { // LDR
            int offset = instruction & 0xFFF;
            boolean add = ((instruction >> 23) & 1) == 1;
            boolean wback = ((instruction >> 21) & 1) == 1;
            
            int address = reg[rn];
            if (add) address += offset;
            else address -= offset;
            
            reg[rd] = read32(memory, address);
            
            if (wback) {
                reg[rn] = address;
            }
        }
        else if ((instruction & 0x0E000000) == 0x04000000) { // STR
            int offset = instruction & 0xFFF;
            boolean add = ((instruction >> 23) & 1) == 1;
            boolean wback = ((instruction >> 21) & 1) == 1;
            
            int address = reg[rn];
            if (add) address += offset;
            else address -= offset;
            
            write32(memory, address, reg[rd]);
            
            if (wback) {
                reg[rn] = address;
            }
        }
        else if ((instruction & 0x0F000000) == 0x0F000000) { // SWI (syscall)
            int swiNumber = instruction & 0xFFFFFF;
            handleSyscall(swiNumber);
        }
        else {
            // Instrução não implementada
            midlet.print("Unimplemented instruction: 0x" + Integer.toHexString(instruction), stdout);
            running = false;
        }
        
        // Atualizar flags CPSR (simplificado)
        if (rd == 15) { // Se o destino for PC
            // Branch aconteceu
        }
    }
    
    private void handleSyscall(int swiNumber) {
        // Mapear syscall ARM (r7 contém o número da syscall)
        int syscall = reg[7];
        
        switch (syscall) {
            case 1: // exit
                int status = reg[0];
                midlet.print("Program exited with status: " + status, stdout);
                running = false;
                break;
                
            case 4: // write
                int fd = reg[0];
                int buf = reg[1];
                int count = reg[2];
                
                if (fd == 1 || fd == 2) { // stdout ou stderr
                    StringBuffer sb = new StringBuffer();
                    for (int i = 0; i < count; i++) {
                        byte b = memory[buf + i];
                        sb.append((char)b);
                    }
                    midlet.print(sb.toString(), stdout);
                    reg[0] = count; // Retorna número de bytes escritos
                }
                break;
                
            case 45: // brk (simplificado)
                // Retorna o limite atual do heap
                reg[0] = memory.length;
                break;
                
            default:
                midlet.print("Unsupported syscall: " + syscall, stdout);
                running = false;
        }
    }
    
    private boolean checkCondition(int cond) {
        switch (cond) {
            case 0: // EQ
                return cpsrZ;
            case 1: // NE
                return !cpsrZ;
            case 2: // CS/HS
                return cpsrC;
            case 3: // CC/LO
                return !cpsrC;
            case 4: // MI
                return cpsrN;
            case 5: // PL
                return !cpsrN;
            case 6: // VS
                return cpsrV;
            case 7: // VC
                return !cpsrV;
            case 8: // HI
                return cpsrC && !cpsrZ;
            case 9: // LS
                return !cpsrC || cpsrZ;
            case 10: // GE
                return cpsrN == cpsrV;
            case 11: // LT
                return cpsrN != cpsrV;
            case 12: // GT
                return !cpsrZ && (cpsrN == cpsrV);
            case 13: // LE
                return cpsrZ || (cpsrN != cpsrV);
            case 14: // AL
                return true;
            default:
                return false;
        }
    }
    
    private int rotateRight(int value, int shift) {
        shift &= 31;
        return (value >>> shift) | (value << (32 - shift));
    }
    
    private int read16(byte[] data, int offset) {
        return ((data[offset + 1] & 0xFF) << 8) | (data[offset] & 0xFF);
    }
    
    private int read32(byte[] data, int offset) {
        return ((data[offset + 3] & 0xFF) << 24) |
               ((data[offset + 2] & 0xFF) << 16) |
               ((data[offset + 1] & 0xFF) << 8) |
               (data[offset] & 0xFF);
    }
    
    private void write32(byte[] data, int offset, int value) {
        data[offset] = (byte)(value & 0xFF);
        data[offset + 1] = (byte)((value >> 8) & 0xFF);
        data[offset + 2] = (byte)((value >> 16) & 0xFF);
        data[offset + 3] = (byte)((value >> 24) & 0xFF);
    }
    
    // Método auxiliar para debug
    public void dumpRegisters() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 16; i++) {
            sb.append("r").append(i).append(": 0x").append(Integer.toHexString(reg[i])).append(" ");
            if (i == 7 || i == 15) sb.append("\n");
        }
        midlet.print(sb.toString(), stdout);
    }
}