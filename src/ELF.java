[file name]: ELF.java
[file content begin]
import java.util.*;
import java.io.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    private Hashtable scope;
    private String pid;
    private int id;
    
    // Memória e registradores
    private byte[] memory;
    private int[] registers;
    private int pc;
    private boolean running;
    private int stackPointer;
    
    // Registrador de flags CPSR
    private int cpsr;
    
    // File descriptors
    private Hashtable fileDescriptors;
    private int nextFd;
    
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
    private static final int REG_R7 = 7;
    private static final int REG_SP = 13;
    private static final int REG_LR = 14;
    private static final int REG_PC = 15;
    
    // Bits do CPSR
    private static final int CPSR_N = 31; // Negative/Less than
    private static final int CPSR_Z = 30; // Zero
    private static final int CPSR_C = 29; // Carry/Borrow/Extend
    private static final int CPSR_V = 28; // Overflow
    
    // Máscaras para bits do CPSR
    private static final int N_MASK = 1 << CPSR_N;
    private static final int Z_MASK = 1 << CPSR_Z;
    private static final int C_MASK = 1 << CPSR_C;
    private static final int V_MASK = 1 << CPSR_V;
    
    // Syscalls Linux ARM (EABI) - Atualizadas
    private static final int SYS_EXIT = 1;
    private static final int SYS_FORK = 2;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_CREAT = 8;
    private static final int SYS_TIME = 13;
    private static final int SYS_CHDIR = 12;
    private static final int SYS_GETPID = 20;
    private static final int SYS_KILL = 37;
    private static final int SYS_BRK = 45;
    private static final int SYS_GETCWD = 183;
    
    // Flags de open
    private static final int O_RDONLY = 0;
    private static final int O_WRONLY = 1;
    private static final int O_RDWR = 2;
    private static final int O_CREAT = 64;
    private static final int O_APPEND = 1024;
    private static final int O_TRUNC = 512;
    
    public ELF(OpenTTY midlet, Object stdout, Hashtable scope, int id, String pid, Hashtable proc) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.scope = scope;
        this.id = id;
        this.pid = pid == null ? midlet.genpid() : pid;
        this.memory = new byte[1024 * 1024]; // 1MB de memória
        this.registers = new int[16];
        this.cpsr = 0;
        this.running = false;
        this.stackPointer = memory.length - 1024;
        this.fileDescriptors = new Hashtable();
        this.nextFd = 3; // 0=stdin, 1=stdout, 2=stderr
        
        // Inicializar file descriptors padrão
        fileDescriptors.put(new Integer(1), stdout); // stdout
        fileDescriptors.put(new Integer(2), stdout); // stderr
    }
    
    public String getPid() { return pid; }
    public void kill() { running = false; handleExit(); }
    
    private String toHex(int value) { String hex = Integer.toHexString(value); while (hex.length() < 8) { hex = "0" + hex; } return "0x" + hex; }
    
    public boolean load(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] elfData = baos.toByteArray();
        
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { midlet.print("Not a valid ELF file", stdout); return false; }
        if (elfData[4] != ELFCLASS32) { midlet.print("Only 32-bit ELF supported", stdout); return false; }
        if (elfData[5] != ELFDATA2LSB) { midlet.print("Only little-endian ELF supported", stdout); return false; }
        
        int e_type = readShortLE(elfData, 16);
        int e_machine = readShortLE(elfData, 18);
        int e_entry = readIntLE(elfData, 24);
        int e_phoff = readIntLE(elfData, 28);
        int e_phnum = readShortLE(elfData, 44);
        int e_phentsize = readShortLE(elfData, 42);
        
        if (e_type != ET_EXEC) { midlet.print("Not an executable ELF", stdout); return false; }
        if (e_machine != EM_ARM) { midlet.print("Not an ARM executable", stdout); return false; }
        
        pc = e_entry;
        registers[REG_SP] = stackPointer;
        registers[REG_LR] = 0xFFFFFFFF;
        
        for (int i = 0; i < e_phnum; i++) {
            int phdrOffset = e_phoff + i * e_phentsize;
            int p_type = readIntLE(elfData, phdrOffset);
            
            if (p_type == PT_LOAD) {
                int p_offset = readIntLE(elfData, phdrOffset + 4);
                int p_vaddr = readIntLE(elfData, phdrOffset + 8);
                int p_filesz = readIntLE(elfData, phdrOffset + 16);
                int p_memsz = readIntLE(elfData, phdrOffset + 20);
                
                for (int j = 0; j < p_filesz && j < memory.length; j++) { if (p_vaddr + j < memory.length) { memory[p_vaddr + j] = elfData[p_offset + j]; } }
                for (int j = p_filesz; j < p_memsz; j++) { if (p_vaddr + j < memory.length) { memory[p_vaddr + j] = 0; } }
            }
        }
        
        return true;
    }
    
    public void run() {
        running = true;
        
        Hashtable proc = midlet.genprocess("elf", id, null);
        proc.put("elf", this); midlet.sys.put(pid, proc);
        
        try {
            while (running && pc < memory.length - 3 && midlet.sys.containsKey(pid)) {
                int instruction = readIntLE(memory, pc);
                pc += 4;
                executeInstruction(instruction);
            }
        } 
        catch (Exception e) { midlet.print("ELF execution error: " + e.toString(), stdout); running = false; } 
        finally { if (midlet.sys.containsKey(pid)) { midlet.sys.remove(pid); } }
    }
    
    private void executeInstruction(int instruction) {
        // Syscall
        if ((instruction & 0x0F000000) == 0x0F000000) {
            int swi_number = instruction & 0x00FFFFFF;
            
            if (swi_number == 0) { handleSyscall(registers[REG_R7]); } else { handleSyscall(swi_number); }
            return;
        }

        // Data Processing Instructions
        if ((instruction & 0x0C000000) == 0x00000000) {
            int opcode = (instruction >> 21) & 0xF;
            int setFlags = (instruction >> 20) & 0x1;
            int rn = (instruction >> 16) & 0xF;
            int rd = (instruction >> 12) & 0xF;
            
            // Extrair shifter_operand baseado no bit I (immediate)
            int shifter_operand;
            int shifter_carry_out = (cpsr & C_MASK) != 0 ? 1 : 0; // carry atual
            
            if ((instruction & (1 << 25)) != 0) {
                // Immediate operand
                int imm = instruction & 0xFF;
                int rotate = ((instruction >> 8) & 0xF) * 2;
                shifter_operand = rotateRight(imm, rotate);
                
                // Para rotações, o carry é o último bit rotacionado para fora
                if (rotate != 0) {
                    int last_bit = (imm >> (rotate - 1)) & 0x1;
                    shifter_carry_out = last_bit;
                }
            } else {
                // Register operand with shift
                int rm = instruction & 0xF;
                int shift_type = (instruction >> 5) & 0x3;
                int shift_amount;
                
                // Verificar se o shift amount é imediato ou registrador
                if ((instruction & (1 << 4)) == 0) {
                    // Shift amount é imediato
                    shift_amount = (instruction >> 7) & 0x1F;
                } else {
                    // Shift amount é registrador
                    int rs = (instruction >> 8) & 0xF;
                    shift_amount = registers[rs] & 0xFF;
                }
                
                int rm_value = registers[rm];
                shifter_operand = applyShift(rm_value, shift_type, shift_amount, shifter_carry_out);
            }
            
            // Para instruções que usam PC como Rn, ajustar o valor
            int rnValue;
            if (rn == REG_PC) {
                // PC arquitetural: endereço da instrução atual + 8
                // A instrução atual está em (pc - 4) porque já incrementamos o pc
                rnValue = pc + 4;
            } else {
                rnValue = registers[rn];
            }
            
            int result = 0;
            boolean updateCarry = false;
            int carry_in = (cpsr & C_MASK) != 0 ? 1 : 0;
            switch (opcode) {
                case 0x0: // AND
                    result = rnValue & shifter_operand;
                    updateCarry = true;
                    break;
                case 0x1: // EOR
                    result = rnValue ^ shifter_operand;
                    updateCarry = true;
                    break;
                case 0x2: // SUB
                    result = rnValue - shifter_operand;
                    // Para SUB, carry é NOT borrow
                    updateCarry = true;
                    shifter_carry_out = (rnValue >= shifter_operand) ? 1 : 0;
                    break;
                case 0x3: // RSB (Reverse Subtract)
                    result = shifter_operand - rnValue;
                    updateCarry = true;
                    shifter_carry_out = (shifter_operand >= rnValue) ? 1 : 0;
                    break;
                case 0x4: // ADD
                    int add_temp = rnValue + shifter_operand;
                    result = add_temp;
                    updateCarry = true;
                    // Verifica overflow para 32 bits
                    boolean add_overflow = ((rnValue ^ shifter_operand) >= 0) && ((rnValue ^ add_temp) < 0);
                    shifter_carry_out = add_overflow ? 1 : 0;
                    break;
                case 0x5: // ADC (Add with Carry)
                    int adc_temp = rnValue + shifter_operand + carry_in;
                    result = adc_temp;
                    updateCarry = true;
                    // Verifica overflow considerando carry
                    long adc_check = (long)rnValue + (long)shifter_operand + (long)carry_in;
                    shifter_carry_out = (adc_check > 0xFFFFFFFFL) ? 1 : 0;
                    break;
                case 0x6: // SBC (Subtract with Carry)
                    int sbc_temp = rnValue - shifter_operand - (1 - carry_in);
                    result = sbc_temp;
                    updateCarry = true;
                    shifter_carry_out = (rnValue >= (shifter_operand + (1 - carry_in))) ? 1 : 0;
                    break;
                case 0x7: // RSC (Reverse Subtract with Carry)
                    int rsc_temp = shifter_operand - rnValue - (1 - carry_in);
                    result = rsc_temp;
                    updateCarry = true;
                    shifter_carry_out = (shifter_operand >= (rnValue + (1 - carry_in))) ? 1 : 0;
                    break;
                case 0x8: // TST (Test - AND sem armazenar resultado)
                    result = rnValue & shifter_operand;
                    setFlags = 1; // TST sempre atualiza flags
                    updateCarry = true;
                    break;
                case 0x9: // TEQ (Test Equivalence - EOR sem armazenar resultado)
                    result = rnValue ^ shifter_operand;
                    setFlags = 1; // TEQ sempre atualiza flags
                    updateCarry = true;
                    break;
                case 0xA: // CMP (Compare - SUB sem armazenar resultado)
                    result = rnValue - shifter_operand;
                    setFlags = 1; // CMP sempre atualiza flags
                    updateCarry = true;
                    shifter_carry_out = (rnValue >= shifter_operand) ? 1 : 0;
                    break;
                case 0xB: // CMN (Compare Negative - ADD sem armazenar resultado)
                    int cmn_temp = rnValue + shifter_operand;
                    result = cmn_temp;
                    setFlags = 1; // CMN sempre atualiza flags
                    updateCarry = true;
                    // Verifica overflow
                    long cmn_check = (long)rnValue + (long)shifter_operand;
                    shifter_carry_out = (cmn_check > 0xFFFFFFFFL) ? 1 : 0;
                    break;
                case 0xC: // ORR
                    result = rnValue | shifter_operand;
                    updateCarry = true;
                    break;
                case 0xD: // MOV
                    result = shifter_operand;
                    updateCarry = true;
                    break;
                case 0xE: // BIC (Bit Clear)
                    result = rnValue & ~shifter_operand;
                    updateCarry = true;
                    break;
                case 0xF: // MVN (Move Not)
                    result = ~shifter_operand;
                    updateCarry = true;
                    break;
            }

            
            // Atualizar registrador de destino (exceto para instruções de teste)
            if (opcode != 0x8 && opcode != 0x9 && opcode != 0xA && opcode != 0xB) { registers[rd] = result; }
            // Atualizar flags se necessário
            if (setFlags != 0) {
                updateFlags(result, updateCarry ? shifter_carry_out : -1);
                
                // Atualizar flags de overflow para operações aritméticas
                if (opcode == 0x2 || opcode == 0x3 || opcode == 0x4 || opcode == 0x5 || 
                    opcode == 0x6 || opcode == 0x7 || opcode == 0xA || opcode == 0xB) {
                    updateOverflow(rnValue, shifter_operand, result, opcode);
                }
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
            boolean writeBack = (instruction & (1 << 21)) != 0;
            
            int baseAddress;
            
            if (rn == REG_PC) { baseAddress = pc + 4; } else { baseAddress = registers[rn]; }
            
            int address = baseAddress;
            
            if (preIndexed) {
                if (addOffset) { address += offset; } else { address -= offset; }
                
                // Write back para pré-indexado
                if (writeBack && rn != REG_PC) { registers[rn] = address; }
            }
            
            if (isLoad) {
                if (address >= 0 && address < memory.length) {
                    if (isByte) {
                        registers[rd] = memory[address] & 0xFF;
                    } else {
                        // Alinhar para palavra (4 bytes)
                        int alignedAddr = address & ~3;
                        if (alignedAddr + 3 < memory.length) {
                            registers[rd] = readIntLE(memory, alignedAddr);
                        }
                    }
                }
            } else {
                if (address >= 0 && address < memory.length) {
                    if (isByte) {
                        memory[address] = (byte)(registers[rd] & 0xFF);
                    } else {
                        writeIntLE(memory, address, registers[rd]);
                    }
                }
            }
            
            if (!preIndexed) {
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
            if ((offset & 0x00800000) != 0) {
                offset |= 0xFF000000;
            }
            offset <<= 2;
            
            boolean link = (instruction & (1 << 24)) != 0;
            
            if (link) {
                registers[REG_LR] = pc;
            }
            
            pc = pc + offset - 4;
            return;
        }

        // ADR/SUB pseudo-instructions (ADD/SUB com PC)
        if ((instruction & 0x0F000000) == 0x02800000 || (instruction & 0x0F000000) == 0x02400000) {
            boolean isAdd = (instruction & 0x0F000000) == 0x02800000;
            int rd = (instruction >> 12) & 0xF;
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            int offset = rotateRight(imm, rotate);
            
            int pcValue = pc + 4;
            
            if (isAdd) { 
                registers[rd] = pcValue + offset; 
            } else { 
                registers[rd] = pcValue - offset; 
            }

            return;
        }

        // NOP
        if (instruction == 0xE1A00000) {
            return;
        }
    }
    
    private int applyShift(int value, int shift_type, int shift_amount, int carry_in) {
        if (shift_amount == 0) {
            return value;
        }
        
        switch (shift_type) {
            case 0: // LSL (Logical Shift Left)
                if (shift_amount >= 32) {
                    cpsr = (cpsr & ~C_MASK) | ((value << (shift_amount - 1)) >>> 31) << CPSR_C;
                    return 0;
                }
                cpsr = (cpsr & ~C_MASK) | ((value << (shift_amount - 1)) >>> 31) << CPSR_C;
                return value << shift_amount;
                
            case 1: // LSR (Logical Shift Right)
                if (shift_amount >= 32) {
                    cpsr = (cpsr & ~C_MASK) | ((value >>> (shift_amount - 1)) & 1) << CPSR_C;
                    return 0;
                }
                cpsr = (cpsr & ~C_MASK) | ((value >>> (shift_amount - 1)) & 1) << CPSR_C;
                return value >>> shift_amount;
                
            case 2: // ASR (Arithmetic Shift Right)
                if (shift_amount >= 32) {
                    int sign_bit = value >>> 31;
                    cpsr = (cpsr & ~C_MASK) | (sign_bit << CPSR_C);
                    return sign_bit == 0 ? 0 : 0xFFFFFFFF;
                }
                cpsr = (cpsr & ~C_MASK) | ((value >>> (shift_amount - 1)) & 1) << CPSR_C;
                return value >> shift_amount;
                
            case 3: // ROR (Rotate Right)
                shift_amount &= 31;
                if (shift_amount == 0) {
                    // RRX (Rotate Right with Extend)
                    int result = (carry_in << 31) | (value >>> 1);
                    cpsr = (cpsr & ~C_MASK) | ((value & 1) << CPSR_C);
                    return result;
                }
                int result = rotateRight(value, shift_amount);
                cpsr = (cpsr & ~C_MASK) | ((value >>> (shift_amount - 1)) & 1) << CPSR_C;
                return result;
        }
        
        return value;
    }
    
    private void updateFlags(int result, int carry) {
        // Atualizar flag N (Negative)
        if ((result & 0x80000000) != 0) {
            cpsr |= N_MASK;
        } else {
            cpsr &= ~N_MASK;
        }
        
        // Atualizar flag Z (Zero)
        if (result == 0) {
            cpsr |= Z_MASK;
        } else {
            cpsr &= ~Z_MASK;
        }
        
        // Atualizar flag C (Carry) se fornecido
        if (carry >= 0) {
            if (carry != 0) {
                cpsr |= C_MASK;
            } else {
                cpsr &= ~C_MASK;
            }
        }
    }
    
    private void updateOverflow(int operand1, int operand2, int result, int opcode) {
        boolean overflow = false;
        
        switch (opcode) {
            case 0x2: // SUB
            case 0xA: // CMP
                overflow = ((operand1 ^ operand2) & (operand1 ^ result) & 0x80000000) != 0;
                break;
            case 0x3: // RSB
                overflow = ((operand2 ^ operand1) & (operand2 ^ result) & 0x80000000) != 0;
                break;
            case 0x4: // ADD
            case 0xB: // CMN
                overflow = ((~(operand1 ^ operand2)) & (operand1 ^ result) & 0x80000000) != 0;
                break;
        }
        
        if (overflow) {
            cpsr |= V_MASK;
        } else {
            cpsr &= ~V_MASK;
        }
    }
    
    private void handleSyscall(int number) {
        switch (number) {
            case SYS_FORK:
                handleFork();
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
                
            case SYS_CREAT:
                handleCreat();
                break;
                
            case SYS_TIME:
                handleTime();
                break;
                
            case SYS_CHDIR:
                handleChdir();
                break;
                
            case SYS_EXIT:
                handleExit();
                break;
                
            case SYS_GETPID:
                handleGetpid();
                break;
                
            case SYS_KILL:
                handleKill();
                break;
                
            case SYS_GETCWD:
                handleGetcwd();
                break;
                
            case SYS_BRK:
                registers[REG_R0] = memory.length;
                break;
                
            default:
                registers[REG_R0] = -1; // Syscall não implementada
                break;
        }
    }
    
    private void handleFork() {
        // Em J2ME não temos fork real, simulamos retornando 0 para o processo filho (simulado)
        // No sistema real, isso criaria um novo processo
        // Por simplicidade, retornamos -1 (erro) indicando que fork não é suportado
        registers[REG_R0] = -1; // ENOSYS - Function not implemented
    }
    
    private void handleWrite() {
        int fd = registers[REG_R0];
        int buf = registers[REG_R1];
        int count = registers[REG_R2];
        
        if (count <= 0 || buf < 0 || buf >= memory.length) {
            registers[REG_R0] = -1;
            return;
        }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 1 || fd == 2) {
            // stdout/stderr - escrever no OpenTTY
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < count && buf + i < memory.length; i++) {
                sb.append((char)(memory[buf + i] & 0xFF));
            }
            
            midlet.print(sb.toString(), stdout, id);
            
            registers[REG_R0] = count;
            
        } else if (fileDescriptors.containsKey(fdKey)) {
            Object stream = fileDescriptors.get(fdKey);
            
            if (stream instanceof OutputStream) {
                try {
                    OutputStream os = (OutputStream) stream;
                    for (int i = 0; i < count && buf + i < memory.length; i++) {
                        os.write(memory[buf + i]);
                    }
                    os.flush();
                    registers[REG_R0] = count;
                } catch (Exception e) {
                    registers[REG_R0] = -1;
                }
            } else if (stream instanceof StringBuffer) {
                StringBuffer sb = (StringBuffer) stream;
                for (int i = 0; i < count && buf + i < memory.length; i++) {
                    sb.append((char)(memory[buf + i] & 0xFF));
                }
                registers[REG_R0] = count;
            } else {
                registers[REG_R0] = -1;
            }
        } else {
            registers[REG_R0] = -1;
        }
    }
    
    private void handleRead() {
        int fd = registers[REG_R0];
        int buf = registers[REG_R1];
        int count = registers[REG_R2];
        
        if (count <= 0 || buf < 0 || buf >= memory.length) {
            registers[REG_R0] = -1;
            return;
        }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 0) {
            // stdin - não implementado por enquanto
            registers[REG_R0] = 0;
        } else if (fileDescriptors.containsKey(fdKey)) {
            Object stream = fileDescriptors.get(fdKey);
            
            if (stream instanceof InputStream) {
                try {
                    InputStream is = (InputStream) stream;
                    int bytesRead = 0;
                    for (int i = 0; i < count && buf + i < memory.length; i++) {
                        int b = is.read();
                        if (b == -1) break;
                        memory[buf + i] = (byte) b;
                        bytesRead++;
                    }
                    registers[REG_R0] = bytesRead;
                } catch (Exception e) {
                    registers[REG_R0] = -1;
                }
            } else {
                registers[REG_R0] = -1;
            }
        } else {
            registers[REG_R0] = -1;
        }
    }
    
    private void handleOpen() {
        int pathAddr = registers[REG_R0];
        int flags = registers[REG_R1];
        int mode = registers[REG_R2];
        
        if (pathAddr < 0 || pathAddr >= memory.length) {
            registers[REG_R0] = -1;
            return;
        }
        
        // Ler o caminho da memória
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) {
            pathBuf.append((char)(memory[pathAddr + i] & 0xFF));
            i++;
        }
        String path = pathBuf.toString();
        
        try {
            boolean forReading = (flags & O_RDONLY) == O_RDONLY || (flags & O_RDWR) == O_RDWR;
            boolean forWriting = (flags & O_WRONLY) == O_WRONLY || (flags & O_RDWR) == O_RDWR;
            boolean create = (flags & O_CREAT) != 0;
            boolean append = (flags & O_APPEND) != 0;
            boolean truncate = (flags & O_TRUNC) != 0;
            
            // Resolver caminho relativo
            String fullPath = path;
            if (!path.startsWith("/")) {
                String pwd = (String) scope.get("PWD");
                if (pwd == null) { pwd = "/home/"; }
                fullPath = pwd + (pwd.endsWith("/") ? "" : "/") + path;
            }
            
            if (forReading) {
                InputStream is = midlet.getInputStream(fullPath);
                if (is != null) {
                    Integer fd = new Integer(nextFd++);
                    fileDescriptors.put(fd, is);
                    registers[REG_R0] = fd.intValue();
                } else if (create) {
                    midlet.write(fullPath, "", id);
                    InputStream is2 = midlet.getInputStream(fullPath);
                    if (is2 != null) {
                        Integer fd = new Integer(nextFd++);
                        fileDescriptors.put(fd, is2);
                        registers[REG_R0] = fd.intValue();
                    } else {
                        registers[REG_R0] = -1;
                    }
                } else {
                    registers[REG_R0] = -2; // ENOENT
                }
            } else if (forWriting) {
                // Para escrita, usamos um ByteArrayOutputStream temporário
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                // Se for append, carregar conteúdo existente
                if (append && !truncate) {
                    InputStream existing = midlet.getInputStream(fullPath);
                    if (existing != null) {
                        int b;
                        while ((b = existing.read()) != -1) {
                            baos.write(b);
                        }
                        existing.close();
                    }
                }
                
                Integer fd = new Integer(nextFd++);
                fileDescriptors.put(fd, baos);
                registers[REG_R0] = fd.intValue();
                
                // Guardar o caminho para uso no close/flush
                fileDescriptors.put(fd + ":path", fullPath);
            } else {
                registers[REG_R0] = -1;
            }
            
        } catch (Exception e) {
            registers[REG_R0] = -1;
        }
    }
    
    private void handleCreat() {
        // creat(path, mode) é equivalente a open(path, O_CREAT | O_WRONLY | O_TRUNC, mode)
        int pathAddr = registers[REG_R0];
        int mode = registers[REG_R1];
        
        // Simular open com flags O_CREAT | O_WRONLY | O_TRUNC
        registers[REG_R1] = O_CREAT | O_WRONLY | O_TRUNC;
        registers[REG_R2] = mode;
        
        handleOpen();
    }
    
    private void handleClose() {
        int fd = registers[REG_R0];
        Integer fdKey = new Integer(fd);
        
        if (fd == 0 || fd == 1 || fd == 2) {
            // Não fechar stdin/stdout/stderr
            registers[REG_R0] = 0;
            return;
        }
        
        if (fileDescriptors.containsKey(fdKey)) {
            Object stream = fileDescriptors.get(fdKey);
            
            try {
                if (stream instanceof InputStream) {
                    ((InputStream) stream).close();
                } else if (stream instanceof OutputStream) {
                    OutputStream os = (OutputStream) stream;
                    os.close();
                    
                    // Se for ByteArrayOutputStream, salvar no arquivo
                    if (stream instanceof ByteArrayOutputStream) {
                        ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
                        String pathKey = fd + ":path";
                        if (fileDescriptors.containsKey(pathKey)) {
                            String path = (String) fileDescriptors.get(pathKey);
                            byte[] data = baos.toByteArray();
                            midlet.write(path, data, id);
                        }
                    }
                }
                
                fileDescriptors.remove(fd);
                fileDescriptors.remove(fd + ":path");
                registers[REG_R0] = 0;
                
            } catch (Exception e) {
                registers[REG_R0] = -1;
            }
        } else {
            registers[REG_R0] = -1;
        }
    }
    
    private void handleTime() {
        // Retornar o tempo atual em segundos desde a época (1970-01-01 00:00:00 UTC)
        long currentTime = System.currentTimeMillis() / 1000;
        registers[REG_R0] = (int) currentTime;
        
        // Se o ponteiro para time_t foi fornecido (R0 != 0), escrever o tempo lá também
        int timePtr = registers[REG_R1];
        if (timePtr != 0 && timePtr >= 0 && timePtr + 3 < memory.length) {
            writeIntLE(memory, timePtr, (int) currentTime);
        }
    }
    
    private void handleChdir() {
        int pathAddr = registers[REG_R0];
        
        if (pathAddr < 0 || pathAddr >= memory.length) {
            registers[REG_R0] = -1; // EFAULT
            return;
        }
        
        // Ler o caminho da memória
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) {
            pathBuf.append((char)(memory[pathAddr + i] & 0xFF));
            i++;
        }
        String path = pathBuf.toString();
        
        // Verificar se o diretório existe
        if (path.equals("") || path.equals(".")) {
            registers[REG_R0] = 0; // Sucesso
            return;
        }
        
        // Resolver caminho relativo
        String fullPath = path;
        if (!path.startsWith("/")) {
            String pwd = (String) scope.get("PWD");
            if (pwd == null) { pwd = "/home/"; }
            fullPath = pwd + (pwd.endsWith("/") ? "" : "/") + path;
        }
        
        // Adicionar barra final se necessário
        if (!fullPath.endsWith("/")) {
            fullPath = fullPath + "/";
        }
        
        // Verificar se é um diretório válido no sistema de arquivos
        boolean dirExists = false;
        
        if (fullPath.equals("/home/")) {
            dirExists = true;
        } else if (fullPath.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + fullPath.substring(5), Connector.READ);
                dirExists = conn.exists() && conn.isDirectory();
                conn.close();
            } catch (Exception e) {
                dirExists = false;
            }
        } else if (midlet.fs.containsKey(fullPath)) {
            dirExists = true;
        }
        
        if (dirExists) {
            scope.put("PWD", fullPath);
            registers[REG_R0] = 0; // Sucesso
        } else {
            registers[REG_R0] = -2; // ENOENT - No such file or directory
        }
    }
    
    private void handleGetpid() {
        try {
            int pidValue = Integer.parseInt(this.pid);
            registers[REG_R0] = pidValue;
        } catch (NumberFormatException e) {
            registers[REG_R0] = 1; // Fallback para PID 1 (init)
        }
    }
    
    private void handleKill() {
        int pid = registers[REG_R0];
        int sig = registers[REG_R1];
        
        // Converter PID para string
        String targetPid = String.valueOf(pid);
        
        // Verificar se o processo existe
        if (!midlet.sys.containsKey(targetPid)) {
            registers[REG_R0] = -3; // ESRCH - No such process
            return;
        }
        
        // Verificar permissões (apenas root ou o próprio processo pode matar)
        if (this.id != 0 && !targetPid.equals(this.pid)) {
            registers[REG_R0] = -1; // EPERM - Operation not permitted
            return;
        }
        
        // Enviar sinal (simulado)
        if (sig == 9) { // SIGKILL
            // Matar processo imediatamente
            Object procObj = midlet.sys.get(targetPid);
            if (procObj instanceof Hashtable) {
                Hashtable proc = (Hashtable) procObj;
                if (proc.containsKey("elf")) {
                    ELF elf = (ELF) proc.get("elf");
                    elf.kill();
                }
            }
            midlet.sys.remove(targetPid);
            registers[REG_R0] = 0; // Sucesso
        } else if (sig == 15) { // SIGTERM
            // Sinal de término normal
            // Em um sistema real, isso permitiria limpeza
            // Aqui apenas matamos o processo
            midlet.sys.remove(targetPid);
            registers[REG_R0] = 0; // Sucesso
        } else {
            // Sinal não suportado
            registers[REG_R0] = -22; // EINVAL - Invalid argument
        }
    }
    
    private void handleExit() {
        int status = registers[REG_R0];
        running = false;
        
        // Fechar todos os file descriptors abertos
        Enumeration keys = fileDescriptors.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof Integer) {
                Integer fd = (Integer) key;
                if (fd.intValue() >= 3) {
                    Object stream = fileDescriptors.get(fd);
                    try {
                        if (stream instanceof InputStream) { ((InputStream) stream).close(); } 
                        else if (stream instanceof OutputStream) { ((OutputStream) stream).close(); }
                    } catch (Exception e) { }
                }
            }
        }
    }
    
    private void handleGetcwd() {
        int buf = registers[REG_R0];
        int size = registers[REG_R1];
        
        String cwd = (String) scope.get("PWD");
        if (cwd == null) cwd = "/home/";
        
        byte[] cwdBytes = cwd.getBytes();
        int len = Math.min(cwdBytes.length, size - 1);
        
        for (int i = 0; i < len && buf + i < memory.length; i++) { memory[buf + i] = cwdBytes[i]; }
        
        if (buf + len < memory.length) { memory[buf + len] = 0; }
        
        registers[REG_R0] = buf;
    }
    
    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); } 
    
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    
    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }
}