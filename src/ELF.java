// ELF.java - Emulador de CPU para ELF32 (J2ME Compatível)
import java.io.*;
import java.util.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    
    // Registradores da CPU
    private int[] registers = new int[16]; // R0-R15
    private int pc; // Program Counter
    private int sp; // Stack Pointer
    private int fp; // Frame Pointer
    
    // Memória
    private byte[] memory;
    private static final int MEMORY_SIZE = 64 * 1024; // 64KB
    private static final int STACK_BASE = 32 * 1024; // Stack começa em 32KB
    private static final int STACK_SIZE = 8 * 1024; // 8KB para stack
    
    // Estado da CPU
    private boolean running = false;
    private boolean carryFlag = false;
    private boolean zeroFlag = false;
    private boolean negativeFlag = false;
    
    // Estrutura do ELF
    private byte[] elfData;
    private int entryPoint;
    private int programHeaderOffset;
    private int programHeaderEntrySize;
    private int programHeaderCount;
    
    // Syscalls
    private static final int SYS_EXIT = 1;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_BRK = 45;
    private static final int SYS_IOCTL = 54;
    
    public ELF(OpenTTY midlet, Object stdout) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.memory = new byte[MEMORY_SIZE];
        reset();
    }
    
    public void reset() {
        // Preencher registradores com zeros sem usar Arrays.fill
        for (int i = 0; i < registers.length; i++) {
            registers[i] = 0;
        }
        
        // Preencher memória com zeros
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
        
        pc = 0;
        sp = STACK_BASE + STACK_SIZE - 4; // Stack cresce para baixo
        fp = sp;
        running = false;
        carryFlag = false;
        zeroFlag = false;
        negativeFlag = false;
    }
    
    public boolean load(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        
        elfData = baos.toByteArray();
        return parseELFHeader();
    }
    
    private boolean parseELFHeader() {
        if (elfData.length < 52) return false;
        
        // Verificar assinatura ELF
        if (elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') {
            return false;
        }
        
        // Verificar se é ELF32
        if (elfData[4] != 1) return false; // ELFCLASS32
        
        // Verificar endianness (little endian)
        if (elfData[5] != 1) return false;
        
        // Verificar se é executável
        if (elfData[16] != 2) return false; // ET_EXEC
        
        // Verificar arquitetura ARM
        if (readU16(18) != 40) return false; // EM_ARM
        
        entryPoint = readU32(24);
        programHeaderOffset = readU32(28);
        programHeaderEntrySize = readU16(42);
        programHeaderCount = readU16(44);
        
        // Carregar segmentos do programa
        return loadProgramSegments();
    }
    
    private boolean loadProgramSegments() {
        int offset = programHeaderOffset;
        
        for (int i = 0; i < programHeaderCount; i++) {
            int type = readU32(offset);
            
            if (type == 1) { // PT_LOAD
                int fileOffset = readU32(offset + 4);
                int virtualAddr = readU32(offset + 8);
                int fileSize = readU32(offset + 16);
                int memSize = readU32(offset + 20);
                
                // Copiar dados para memória
                if (virtualAddr + memSize > MEMORY_SIZE) {
                    return false; // Memória insuficiente
                }
                
                // Copiar dados do ELF para memória
                for (int j = 0; j < fileSize; j++) {
                    if (fileOffset + j < elfData.length) {
                        memory[virtualAddr + j] = elfData[fileOffset + j];
                    }
                }
                
                // Zerar o restante da memória alocada
                for (int j = fileSize; j < memSize; j++) {
                    memory[virtualAddr + j] = 0;
                }
            }
            
            offset += programHeaderEntrySize;
        }
        
        pc = entryPoint;
        return true;
    }
    
    public void run() {
        running = true;
        midlet.print("Iniciando execução do ELF...", stdout);
        
        try {
            while (running && pc < MEMORY_SIZE - 3) {
                executeInstruction();
            }
            
            if (running) {
                midlet.print("Execução terminada (PC fora da memória)", stdout);
            }
        } catch (Exception e) {
            midlet.print("Erro durante execução: " + e.getMessage(), stdout);
        }
    }
    
    private void executeInstruction() {
        int instruction = readU32(pc);
        pc += 4;
        
        // Decodificação básica de instruções ARM
        int cond = (instruction >>> 28) & 0xF;
        int opcode = (instruction >>> 21) & 0x7;
        int rn = (instruction >>> 16) & 0xF;
        int rd = (instruction >>> 12) & 0xF;
        int operand2 = instruction & 0xFFF;
        
        // Verificar condição
        if (!checkCondition(cond)) {
            return;
        }
        
        switch (opcode) {
            case 0: // AND
                registers[rd] = registers[rn] & decodeOperand2(operand2);
                break;
            case 1: // EOR
                registers[rd] = registers[rn] ^ decodeOperand2(operand2);
                break;
            case 2: // SUB
                int subResult = registers[rn] - decodeOperand2(operand2);
                registers[rd] = subResult;
                updateFlags(subResult);
                break;
            case 3: // RSB
                int rsbResult = decodeOperand2(operand2) - registers[rn];
                registers[rd] = rsbResult;
                updateFlags(rsbResult);
                break;
            case 4: // ADD
                int addResult = registers[rn] + decodeOperand2(operand2);
                registers[rd] = addResult;
                updateFlags(addResult);
                break;
            case 5: // ADC
                int adcResult = registers[rn] + decodeOperand2(operand2) + (carryFlag ? 1 : 0);
                registers[rd] = adcResult;
                updateFlags(adcResult);
                break;
            case 6: // SBC
                int sbcResult = registers[rn] - decodeOperand2(operand2) - (carryFlag ? 0 : 1);
                registers[rd] = sbcResult;
                updateFlags(sbcResult);
                break;
            case 7: // RSC
                int rscResult = decodeOperand2(operand2) - registers[rn] - (carryFlag ? 0 : 1);
                registers[rd] = rscResult;
                updateFlags(rscResult);
                break;
            case 8: // TST
                int tstResult = registers[rn] & decodeOperand2(operand2);
                updateFlags(tstResult);
                break;
            case 9: // TEQ
                int teqResult = registers[rn] ^ decodeOperand2(operand2);
                updateFlags(teqResult);
                break;
            case 10: // CMP
                int cmpResult = registers[rn] - decodeOperand2(operand2);
                updateFlags(cmpResult);
                break;
            case 11: // CMN
                int cmnResult = registers[rn] + decodeOperand2(operand2);
                updateFlags(cmnResult);
                break;
            case 12: // ORR
                registers[rd] = registers[rn] | decodeOperand2(operand2);
                break;
            case 13: // MOV
                registers[rd] = decodeOperand2(operand2);
                updateFlags(registers[rd]);
                break;
            case 14: // BIC
                registers[rd] = registers[rn] & ~decodeOperand2(operand2);
                break;
            case 15: // MVN
                registers[rd] = ~decodeOperand2(operand2);
                updateFlags(registers[rd]);
                break;
            default:
                // Instrução SWI (Syscall)
                if ((instruction & 0x0F000000) == 0x0F000000) {
                    handleSyscall(instruction & 0x00FFFFFF);
                }
                break;
        }
    }
    
    private int decodeOperand2(int operand2) {
        if ((operand2 & 0x02000000) != 0) {
            // Operando imediato
            int imm = operand2 & 0xFF;
            int rotate = (operand2 >>> 8) & 0xF;
            return rotateRight(imm, rotate * 2);
        } else {
            // Operando de registro
            int rm = operand2 & 0xF;
            int shiftType = (operand2 >>> 5) & 0x3;
            int shiftAmount = (operand2 >>> 7) & 0x1F;
            
            int value = registers[rm];
            switch (shiftType) {
                case 0: // LSL
                    return value << shiftAmount;
                case 1: // LSR
                    return value >>> shiftAmount;
                case 2: // ASR
                    return value >> shiftAmount;
                case 3: // ROR
                    return rotateRight(value, shiftAmount);
                default:
                    return value;
            }
        }
    }
    
    // Implementação de rotação à direita sem usar Integer.rotateRight
    private int rotateRight(int value, int shift) {
        shift = shift & 0x1F; // Limitar a 0-31
        return (value >>> shift) | (value << (32 - shift));
    }
    
    private void updateFlags(int result) {
        zeroFlag = (result == 0);
        negativeFlag = (result < 0);
        // Para carry flag, precisaríamos de mais informações sobre a operação
    }
    
    private boolean checkCondition(int cond) {
        switch (cond) {
            case 0: // EQ
                return zeroFlag;
            case 1: // NE
                return !zeroFlag;
            case 2: // CS/HS
                return carryFlag;
            case 3: // CC/LO
                return !carryFlag;
            case 4: // MI
                return negativeFlag;
            case 5: // PL
                return !negativeFlag;
            case 6: // VS
                return false; // Overflow - não implementado
            case 7: // VC
                return true; // No overflow
            case 8: // HI
                return carryFlag && !zeroFlag;
            case 9: // LS
                return !carryFlag || zeroFlag;
            case 10: // GE
                return negativeFlag == false; // Simplificado
            case 11: // LT
                return negativeFlag;
            case 12: // GT
                return !zeroFlag && (negativeFlag == false); // Simplificado
            case 13: // LE
                return zeroFlag || negativeFlag;
            case 14: // AL
                return true;
            default:
                return false;
        }
    }
    
    private void handleSyscall(int syscallNumber) {
        switch (syscallNumber) {
            case SYS_EXIT:
                handleExit();
                break;
            case SYS_READ:
                handleRead();
                break;
            case SYS_WRITE:
                handleWrite();
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
            case SYS_IOCTL:
                handleIoctl();
                break;
            default:
                midlet.print("Syscall não implementada: " + syscallNumber, stdout);
                break;
        }
    }
    
    private void handleExit() {
        int status = registers[0];
        midlet.print("Programa finalizado com status: " + status, stdout);
        running = false;
    }
    
    private void handleRead() {
        int fd = registers[0];
        int bufferAddr = registers[1];
        int count = registers[2];
        
        try {
            String input = "";
            if (fd == 0) { // STDIN
                // Simular entrada do usuário
                input = "entrada_simulada\n";
            }
            
            byte[] data = input.getBytes("UTF-8");
            int bytesToCopy = Math.min(count, data.length);
            
            // Copiar dados para memória sem System.arraycopy
            for (int i = 0; i < bytesToCopy; i++) {
                if (bufferAddr + i < MEMORY_SIZE) {
                    memory[bufferAddr + i] = data[i];
                }
            }
            
            registers[0] = bytesToCopy; // Retornar número de bytes lidos
            
        } catch (Exception e) {
            registers[0] = -1; // Erro
        }
    }
    
    private void handleWrite() {
        int fd = registers[0];
        int bufferAddr = registers[1];
        int count = registers[2];
        
        try {
            // Ler dados da memória sem System.arraycopy
            byte[] data = new byte[count];
            for (int i = 0; i < count; i++) {
                if (bufferAddr + i < MEMORY_SIZE) {
                    data[i] = memory[bufferAddr + i];
                } else {
                    data[i] = 0;
                }
            }
            
            String output = new String(data, "UTF-8");
            
            if (fd == 1 || fd == 2) { // STDOUT ou STDERR
                midlet.print(output, stdout);
            }
            
            registers[0] = count; // Retornar número de bytes escritos
            
        } catch (Exception e) {
            registers[0] = -1; // Erro
        }
    }
    
    private void handleOpen() {
        int filenameAddr = registers[0];
        int flags = registers[1];
        int mode = registers[2];
        
        try {
            String filename = readString(filenameAddr);
            // Simular abertura de arquivo - sempre retorna sucesso
            registers[0] = 3; // Retornar file descriptor (começando em 3)
            
        } catch (Exception e) {
            registers[0] = -1; // Erro
        }
    }
    
    private void handleClose() {
        int fd = registers[0];
        // Simular fechamento - sempre retorna sucesso
        registers[0] = 0;
    }
    
    private void handleBrk() {
        // Simulação simples de brk - sempre retorna sucesso
        registers[0] = STACK_BASE; // Retornar endereço atual do break
    }
    
    private void handleIoctl() {
        // Syscall ioctl não implementada - retornar erro
        registers[0] = -1;
    }
    
    private String readString(int addr) {
        StringBuffer sb = new StringBuffer();
        int offset = 0;
        
        while (addr + offset < MEMORY_SIZE) {
            byte b = memory[addr + offset];
            if (b == 0) break;
            sb.append((char) b);
            offset++;
        }
        
        return sb.toString();
    }
    
    // Métodos auxiliares para leitura de dados da memória
    private int readU32(int offset) {
        if (offset + 3 >= elfData.length) return 0;
        return ((elfData[offset] & 0xFF) |
               ((elfData[offset + 1] & 0xFF) << 8) |
               ((elfData[offset + 2] & 0xFF) << 16) |
               ((elfData[offset + 3] & 0xFF) << 24));
    }
    
    private int readU16(int offset) {
        if (offset + 1 >= elfData.length) return 0;
        return ((elfData[offset] & 0xFF) |
               ((elfData[offset + 1] & 0xFF) << 8));
    }
    
    // Método para escrever na memória (para debug)
    public void writeMemory(int address, byte[] data) {
        for (int i = 0; i < data.length && address + i < MEMORY_SIZE; i++) {
            memory[address + i] = data[i];
        }
    }
    
    // Getters para debug
    public int getPC() { return pc; }
    public int getSP() { return sp; }
    public int[] getRegisters() { 
        int[] copy = new int[registers.length];
        for (int i = 0; i < registers.length; i++) {
            copy[i] = registers[i];
        }
        return copy;
    }
    public boolean isRunning() { return running; }
    public byte[] getMemory() { return memory; }
}