[file name]: ELF.java
[file content begin]
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import java.util.*;
import java.io.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    private Hashtable scope;
    private String pid;
    private int id = 1000;
    
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
    
    // Condições ARM
    private static final int COND_EQ = 0;  // Equal (Z=1)
    private static final int COND_NE = 1;  // Not equal (Z=0)
    private static final int COND_CS = 2;  // Carry set (C=1)
    private static final int COND_CC = 3;  // Carry clear (C=0)
    private static final int COND_MI = 4;  // Minus/negative (N=1)
    private static final int COND_PL = 5;  // Plus/positive or zero (N=0)
    private static final int COND_VS = 6;  // Overflow (V=1)
    private static final int COND_VC = 7;  // No overflow (V=0)
    private static final int COND_HI = 8;  // Unsigned higher (C=1 & Z=0)
    private static final int COND_LS = 9;  // Unsigned lower or same (C=0 | Z=1)
    private static final int COND_GE = 10; // Signed greater or equal (N=V)
    private static final int COND_LT = 11; // Signed less than (N!=V)
    private static final int COND_GT = 12; // Signed greater than (Z=0 & N=V)
    private static final int COND_LE = 13; // Signed less or equal (Z=1 | N!=V)
    private static final int COND_AL = 14; // Always (unconditional)
    private static final int COND_NV = 15; // Never
    
    // Syscalls Linux ARM (EABI) - Atualizadas
    private static final int SYS_EXIT = 1, SYS_FORK = 2, SYS_READ = 3, SYS_WRITE = 4, SYS_OPEN = 5, SYS_CLOSE = 6;
    private static final int SYS_CREAT = 8;
    private static final int SYS_TIME = 13;
    private static final int SYS_CHDIR = 12;
    private static final int SYS_GETPID = 20;
    private static final int SYS_KILL = 37;
    private static final int SYS_BRK = 45;
    private static final int SYS_GETCWD = 183;
    private static final int SYS_GETTIMEOFDAY = 78;
    private static final int SYS_GETPPID = 64;
    private static final int SYS_GETUID32 = 199;
    private static final int SYS_GETEUID32 = 201;
    
    // Flags de open
    private static final int O_RDONLY = 0, O_WRONLY = 1, O_RDWR = 2;
    private static final int O_CREAT = 64;
    private static final int O_APPEND = 1024;
    private static final int O_TRUNC = 512;
    
    // Coprocessador (simulado para FPU)
    private float[] fpuRegisters;
    private int fpscr; // FPU Status and Control Register
    
    public ELF(OpenTTY midlet, Object stdout, Hashtable scope, int id, String pid, Hashtable proc) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.scope = scope;
        this.id = id;
        this.pid = pid == null ? midlet.genpid() : pid;
        this.memory = new byte[1024 * 1024]; // 1MB de memória
        this.registers = new int[16];
        this.fpuRegisters = new float[32]; // S0-S31 (single precision)
        this.cpsr = 0;
        this.fpscr = 0;
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
        while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); } 
        is.close();
        return load(baos.toByteArray());
    }
    
    public boolean load(byte[] elfData) throws Exception {
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { 
            midlet.print("Not a valid ELF file", stdout); return false; 
        }
        if (elfData[4] != ELFCLASS32) { 
            midlet.print("Only 32-bit ELF supported", stdout); return false; 
        }
        if (elfData[5] != ELFDATA2LSB) { 
            midlet.print("Only little-endian ELF supported", stdout); return false; 
        }
        
        int e_type = readShortLE(elfData, 16);
        int e_machine = readShortLE(elfData, 18);
        int e_entry = readIntLE(elfData, 24);
        int e_phoff = readIntLE(elfData, 28);
        int e_phnum = readShortLE(elfData, 44);
        int e_phentsize = readShortLE(elfData, 42);
        
        if (e_type != ET_EXEC) { 
            midlet.print("Not an executable ELF", stdout); return false; 
        }
        if (e_machine != EM_ARM) { 
            midlet.print("Not an ARM executable", stdout); return false; 
        }
        
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
                
                for (int j = 0; j < p_filesz && j < memory.length; j++) { 
                    if (p_vaddr + j < memory.length) { 
                        memory[p_vaddr + j] = elfData[p_offset + j]; 
                    } 
                }
                for (int j = p_filesz; j < p_memsz; j++) { 
                    if (p_vaddr + j < memory.length) { 
                        memory[p_vaddr + j] = 0; 
                    } 
                }
            }
        }
        
        return true;
    }
    
    public Hashtable run() {
        running = true;

        Hashtable proc = midlet.genprocess("elf", id, null), ITEM = new Hashtable();
        proc.put("elf", this); 
        midlet.sys.put(pid, proc);
        
        try {
            while (running && pc < memory.length - 3 && midlet.sys.containsKey(pid)) {
                int instruction = readIntLE(memory, pc);
                pc += 4;
                executeInstruction(instruction);
            }
        } 
        catch (Exception e) { 
            midlet.print("ELF execution error: " + e.toString(), stdout); 
            running = false; 
        } 
        finally { 
            if (midlet.sys.containsKey(pid)) { 
                midlet.sys.remove(pid); 
            } 
        }

        ITEM.put("status", new Double(0));
        return ITEM;
    }
    
    private void executeInstruction(int instruction) {
        // Extrair condição (bits 28-31)
        int cond = (instruction >> 28) & 0xF;
        
        // Verificar se a instrução deve ser executada baseada na condição
        if (!checkCondition(cond)) {
            return; // Condição falsa, pular instrução
        }
        
        // Syscall (SWI)
        if ((instruction & 0x0F000000) == 0x0F000000) {
            int swi_number = instruction & 0x00FFFFFF;
            if (swi_number == 0) { 
                handleSyscall(registers[REG_R7]); 
            } else { 
                handleSyscall(swi_number); 
            }
            return;
        }

        // Multiplicação e Multiplicação-Acumulação
        if ((instruction & 0x0FC000F0) == 0x00000090) {
            handleMultiply(instruction);
            return;
        }
        
        // Multiplicação longa (signed/unsigned)
        if ((instruction & 0x0F8000F0) == 0x00800090) {
            handleLongMultiply(instruction);
            return;
        }

        // Load/Store múltiplos
        if ((instruction & 0x0E000000) == 0x08000000) {
            handleLoadStoreMultiple(instruction);
            return;
        }

        // Instruções de coprocessador (incluindo FPU)
        if ((instruction & 0x0E000000) == 0x0C000000) {
            handleCoprocessor(instruction);
            return;
        }

        // Data Processing Instructions
        if ((instruction & 0x0C000000) == 0x00000000) {
            handleDataProcessing(instruction);
            return;
        }
        
        // Load/Store Instructions
        if ((instruction & 0x0C000000) == 0x04000000) {
            handleLoadStore(instruction);
            return;
        }
        
        // Branch Instructions
        if ((instruction & 0x0E000000) == 0x0A000000) {
            handleBranch(instruction);
            return;
        }

        // ADR/SUB pseudo-instructions (ADD/SUB com PC)
        if ((instruction & 0x0F000000) == 0x02800000 || (instruction & 0x0F000000) == 0x02400000) {
            handleAdrSub(instruction);
            return;
        }

        // NOP
        if (instruction == 0xE1A00000) {
            return;
        }
        
        // Instrução não reconhecida
        midlet.print("[WARN] Unrecognized instruction: " + toHex(instruction), stdout);
    }
    
    private boolean checkCondition(int cond) {
        switch (cond) {
            case COND_EQ: // Z = 1
                return (cpsr & Z_MASK) != 0;
            case COND_NE: // Z = 0
                return (cpsr & Z_MASK) == 0;
            case COND_CS: // C = 1
                return (cpsr & C_MASK) != 0;
            case COND_CC: // C = 0
                return (cpsr & C_MASK) == 0;
            case COND_MI: // N = 1
                return (cpsr & N_MASK) != 0;
            case COND_PL: // N = 0
                return (cpsr & N_MASK) == 0;
            case COND_VS: // V = 1
                return (cpsr & V_MASK) != 0;
            case COND_VC: // V = 0
                return (cpsr & V_MASK) == 0;
            case COND_HI: // C = 1 and Z = 0
                return ((cpsr & C_MASK) != 0) && ((cpsr & Z_MASK) == 0);
            case COND_LS: // C = 0 or Z = 1
                return ((cpsr & C_MASK) == 0) || ((cpsr & Z_MASK) != 0);
            case COND_GE: // N = V
                return ((cpsr & N_MASK) != 0) == ((cpsr & V_MASK) != 0);
            case COND_LT: // N != V
                return ((cpsr & N_MASK) != 0) != ((cpsr & V_MASK) != 0);
            case COND_GT: // Z = 0 and N = V
                return ((cpsr & Z_MASK) == 0) && (((cpsr & N_MASK) != 0) == ((cpsr & V_MASK) != 0));
            case COND_LE: // Z = 1 or N != V
                return ((cpsr & Z_MASK) != 0) || (((cpsr & N_MASK) != 0) != ((cpsr & V_MASK) != 0));
            case COND_AL: // Always
                return true;
            case COND_NV: // Never
                return false;
            default:
                return true; // Por segurança
        }
    }
    
    private void handleMultiply(int instruction) {
        boolean accumulate = (instruction & (1 << 21)) != 0;
        boolean setFlags = (instruction & (1 << 20)) != 0;
        int rd = (instruction >> 16) & 0xF;
        int rn = (instruction >> 12) & 0xF;
        int rs = (instruction >> 8) & 0xF;
        int rm = instruction & 0xF;
        
        long result = (long)registers[rm] * (long)registers[rs];
        
        if (accumulate) {
            result += registers[rn];
        }
        
        // Truncar para 32 bits
        registers[rd] = (int)result;
        
        if (setFlags) {
            // Atualizar flags N e Z
            updateFlags(registers[rd], -1);
            // Flag C não é afetada em multiplicações ARM
        }
    }
    
    private void handleLongMultiply(int instruction) {
        boolean signed = (instruction & (1 << 22)) != 0;
        boolean accumulate = (instruction & (1 << 21)) != 0;
        boolean setFlags = (instruction & (1 << 20)) != 0;
        int rdHi = (instruction >> 16) & 0xF;
        int rdLo = (instruction >> 12) & 0xF;
        int rs = (instruction >> 8) & 0xF;
        int rm = instruction & 0xF;
        
        long a = registers[rm];
        long b = registers[rs];
        
        if (signed) {
            // Extensão de sinal para 64 bits
            a = (long)(int)a;
            b = (long)(int)b;
        } else {
            // Zero extend para 64 bits
            a = a & 0xFFFFFFFFL;
            b = b & 0xFFFFFFFFL;
        }
        
        long result = a * b;
        
        if (accumulate) {
            long accumulator = ((long)registers[rdHi] << 32) | (registers[rdLo] & 0xFFFFFFFFL);
            result += accumulator;
        }
        
        registers[rdHi] = (int)(result >> 32);
        registers[rdLo] = (int)result;
        
        if (setFlags) {
            // Atualizar flags N e Z baseado no resultado de 64 bits
            boolean negative = (result >> 63) != 0;
            boolean zero = result == 0;
            
            if (negative) {
                cpsr |= N_MASK;
            } else {
                cpsr &= ~N_MASK;
            }
            
            if (zero) {
                cpsr |= Z_MASK;
            } else {
                cpsr &= ~Z_MASK;
            }
        }
    }
    
    private void handleLoadStoreMultiple(int instruction) {
        boolean load = (instruction & (1 << 20)) != 0;
        boolean writeBack = (instruction & (1 << 21)) != 0;
        boolean userMode = (instruction & (1 << 22)) != 0;
        boolean increment = (instruction & (1 << 23)) != 0;
        boolean before = (instruction & (1 << 24)) != 0;
        int rn = (instruction >> 16) & 0xF;
        int registerList = instruction & 0xFFFF;
        
        int address = registers[rn];
        int startAddress = address;
        
        // Contar número de registradores
        int regCount = 0;
        for (int i = 0; i < 16; i++) {
            if ((registerList & (1 << i)) != 0) {
                regCount++;
            }
        }
        
        // Ajustar endereço base se before = true
        if (before) {
            if (increment) {
                address += 4;
            } else {
                address -= 4 * regCount;
            }
        }
        
        // Executar load/store
        for (int i = 0; i < 16; i++) {
            if ((registerList & (1 << i)) != 0) {
                if (load) {
                    if (address >= 0 && address + 3 < memory.length) {
                        registers[i] = readIntLE(memory, address);
                    }
                } else {
                    if (address >= 0 && address + 3 < memory.length) {
                        writeIntLE(memory, address, registers[i]);
                    }
                }
                
                if (increment) {
                    address += 4;
                } else {
                    address -= 4;
                }
            }
        }
        
        // Write back
        if (writeBack) {
            if (increment) {
                registers[rn] = startAddress + 4 * regCount;
            } else {
                registers[rn] = startAddress - 4 * regCount;
            }
        }
    }
    
    private void handleCoprocessor(int instruction) {
        int cpNum = (instruction >> 8) & 0xF;
        
        // CP10 e CP11 são para FPU (VFP)
        if (cpNum == 10 || cpNum == 11) {
            handleFPU(instruction);
        } else {
            // Outros coprocessadores não implementados
            midlet.print("[WARN] Coprocessor " + cpNum + " not implemented", stdout);
        }
    }
    
    private void handleFPU(int instruction) {
        int opcode1 = (instruction >> 20) & 0xF;
        int opcode2 = (instruction >> 16) & 0xF;
        int crd = (instruction >> 12) & 0xF;
        int crn = (instruction >> 16) & 0xF;
        int crm = instruction & 0xF;
        int cpNum = (instruction >> 8) & 0xF;
        
        // Verificar tipo de instrução FPU
        if ((instruction & 0x0F000000) == 0x0C000000) {
            // Data processing ou register transfer
            if ((instruction & (1 << 4)) != 0) {
                // Register transfer
                handleFPURegisterTransfer(instruction);
            } else {
                // Data processing
                handleFPUDataProcessing(instruction);
            }
        } else if ((instruction & 0x0E000000) == 0x0C400000) {
            // Load/store de coprocessador
            handleFPULoadStore(instruction);
        }
    }
    
    private void handleFPURegisterTransfer(int instruction) {
        boolean load = (instruction & (1 << 20)) != 0;
        int rt = (instruction >> 12) & 0xF;
        int crn = (instruction >> 16) & 0xF;
        
        if (load) {
            // VMRS: mover de FPSCR para registrador ARM
            if (crn == 1) { // FPSCR
                registers[rt] = fpscr;
            } else {
                // Mover de registrador FPU para ARM
                int fpuReg = crn;
                registers[rt] = Float.floatToIntBits(fpuRegisters[fpuReg]);
            }
        } else {
            // VMSR: mover de registrador ARM para FPSCR
            if (crn == 1) { // FPSCR
                fpscr = registers[rt];
            } else {
                // Mover de ARM para registrador FPU
                int fpuReg = crn;
                fpuRegisters[fpuReg] = Float.intBitsToFloat(registers[rt]);
            }
        }
    }
    
    private void handleFPUDataProcessing(int instruction) {
        int opcode = (instruction >> 20) & 0xFF;
        int vd = ((instruction >> 12) & 0xF) | ((instruction & 0x10) << 1);
        int vn = ((instruction >> 16) & 0xF) | ((instruction & 0x100000) >> 15);
        int vm = instruction & 0xF;
        
        float a = fpuRegisters[vn];
        float b = fpuRegisters[vm];
        float result = 0;
        
        switch (opcode) {
            case 0x00: // VADD
                result = a + b;
                break;
            case 0x01: // VSUB
                result = a - b;
                break;
            case 0x02: // VMUL
                result = a * b;
                break;
            case 0x03: // VDIV
                result = a / b;
                break;
            case 0x04: // VNEG
                result = -a;
                break;
            case 0x05: // VABS
                result = Math.abs(a);
                break;
            case 0x06: // VSQRT
                result = (float)Math.sqrt(a);
                break;
            case 0x07: // VCMP
                // Configurar flags FPSCR baseado na comparação
                int flags = 0;
                if (Float.isNaN(a) || Float.isNaN(b)) {
                    flags |= 0x1; // NaN
                } else if (a == b) {
                    flags |= 0x6; // Equal
                } else if (a < b) {
                    flags |= 0x8; // Less than
                } else {
                    flags |= 0x2; // Greater than
                }
                fpscr = (fpscr & ~0xF) | flags;
                return;
            default:
                midlet.print("[WARN] FPU opcode " + opcode + " not implemented", stdout);
                return;
        }
        
        fpuRegisters[vd] = result;
        
        // Atualizar flags FPSCR se necessário
        if (Float.isNaN(result)) {
            fpscr |= 0x1; // NaN flag
        } else if (Float.isInfinite(result)) {
            fpscr |= 0x4; // Overflow flag
        } else if (result == 0) {
            fpscr |= 0x2; // Zero flag
        }
    }
    
    private void handleFPULoadStore(int instruction) {
        boolean load = (instruction & (1 << 20)) != 0;
        boolean increment = (instruction & (1 << 23)) != 0;
        boolean before = (instruction & (1 << 24)) != 0;
        int rn = (instruction >> 16) & 0xF;
        int vd = ((instruction >> 12) & 0xF) | ((instruction & 0x10) << 1);
        int imm8 = instruction & 0xFF;
        
        int address = registers[rn];
        
        if (before) {
            if (increment) {
                address += 4;
            } else {
                address -= 4;
            }
        }
        
        if (load) {
            // Load single
            if (address >= 0 && address + 3 < memory.length) {
                int value = readIntLE(memory, address);
                fpuRegisters[vd] = Float.intBitsToFloat(value);
            }
        } else {
            // Store single
            if (address >= 0 && address + 3 < memory.length) {
                int value = Float.floatToIntBits(fpuRegisters[vd]);
                writeIntLE(memory, address, value);
            }
        }
        
        // Write back
        if ((instruction & (1 << 21)) != 0) {
            if (increment) {
                registers[rn] = address + 4;
            } else {
                registers[rn] = address - 4;
            }
        }
    }
    
    private void handleDataProcessing(int instruction) {
        int opcode = (instruction >> 21) & 0xF;
        int setFlags = (instruction & (1 << 20)) >> 20;
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
                boolean add_overflow = ((rnValue ^ shifter_operand) >= 0) && ((rnValue ^ add_temp) < 0);
                shifter_carry_out = add_overflow ? 1 : 0;
                break;
            case 0x5: // ADC (Add with Carry)
                int adc_temp = rnValue + shifter_operand + carry_in;
                result = adc_temp;
                updateCarry = true;
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
        if (opcode != 0x8 && opcode != 0x9 && opcode != 0xA && opcode != 0xB) { 
            registers[rd] = result; 
        }
        // Atualizar flags se necessário
        if (setFlags != 0) {
            updateFlags(result, updateCarry ? shifter_carry_out : -1);
            
            // Atualizar flags de overflow para operações aritméticas
            if (opcode == 0x2 || opcode == 0x3 || opcode == 0x4 || opcode == 0x5 || 
                opcode == 0x6 || opcode == 0x7 || opcode == 0xA || opcode == 0xB) {
                updateOverflow(rnValue, shifter_operand, result, opcode);
            }
        }
    }
    
    private void handleLoadStore(int instruction) {
        int rn = (instruction >> 16) & 0xF;
        int rd = (instruction >> 12) & 0xF;
        int offset = instruction & 0xFFF;
        boolean isLoad = (instruction & (1 << 20)) != 0;
        boolean isByte = (instruction & (1 << 22)) != 0;
        boolean addOffset = (instruction & (1 << 23)) != 0;
        boolean preIndexed = (instruction & (1 << 24)) != 0;
        boolean writeBack = (instruction & (1 << 21)) != 0;
        
        int baseAddress;
        
        if (rn == REG_PC) { 
            baseAddress = pc + 4; 
        } else { 
            baseAddress = registers[rn]; 
        }
        
        int address = baseAddress;
        
        if (preIndexed) {
            if (addOffset) { 
                address += offset; 
            } else { 
                address -= offset; 
            }
            
            // Write back para pré-indexado
            if (writeBack && rn != REG_PC) { 
                registers[rn] = address; 
            }
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
    }
    
    private void handleBranch(int instruction) {
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
    }
    
    private void handleAdrSub(int instruction) {
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
                
            case SYS_GETTIMEOFDAY:
                handleGettimeofday();
                break;

            case SYS_GETPPID:
                handleGetppid();
                break;
                
            case SYS_GETUID32:
                handleGetuid();
                break;
                
            case SYS_GETEUID32:
                handleGetuid();
                break;
                
            default:
                registers[REG_R0] = -1; // Syscall não implementada
                break;
        }
    }
    
    private void handleFork() {
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
        long currentTime = System.currentTimeMillis() / 1000;
        registers[REG_R0] = (int) currentTime;
        
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
        
        if (path.equals("") || path.equals(".")) {
            registers[REG_R0] = 0; // Sucesso
            return;
        }
        
        String fullPath = path;
        if (!path.startsWith("/")) {
            String pwd = (String) scope.get("PWD");
            if (pwd == null) { pwd = "/home/"; }
            fullPath = pwd + (pwd.endsWith("/") ? "" : "/") + path;
        }
        
        if (!fullPath.endsWith("/")) {
            fullPath = fullPath + "/";
        }
        
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
            registers[REG_R0] = 1;
        }
    }
    
    private void handleKill() {
        int pid = registers[REG_R0];
        int sig = registers[REG_R1];
        
        String targetPid = String.valueOf(pid);
        
        if (!midlet.sys.containsKey(targetPid)) {
            registers[REG_R0] = -3; // ESRCH - No such process
            return;
        }
        
        if (this.id != 0 && !targetPid.equals(this.pid)) {
            registers[REG_R0] = -1; // EPERM - Operation not permitted
            return;
        }
        
        if (sig == 9) {
            Object procObj = midlet.sys.get(targetPid);
            if (procObj instanceof Hashtable) {
                Hashtable proc = (Hashtable) procObj;
                if (proc.containsKey("elf")) {
                    ELF elf = (ELF) proc.get("elf");
                    elf.kill();
                }
            }
            midlet.sys.remove(targetPid);
            registers[REG_R0] = 0;
        } else if (sig == 15) {
            midlet.sys.remove(targetPid);
            registers[REG_R0] = 0;
        } else {
            registers[REG_R0] = -22; // EINVAL - Invalid argument
        }
    }
    
    private void handleExit() {
        int status = registers[REG_R0];
        running = false;
        
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
    
    private void handleGettimeofday() {
        int tvPtr = registers[REG_R0];
        int tzPtr = registers[REG_R1];
        
        long currentTimeMillis = System.currentTimeMillis();
        int seconds = (int)(currentTimeMillis / 1000);
        int microseconds = (int)((currentTimeMillis % 1000) * 1000);
        
        if (tvPtr != 0 && tvPtr >= 0 && tvPtr + 7 < memory.length) {
            writeIntLE(memory, tvPtr, seconds);
            writeIntLE(memory, tvPtr + 4, microseconds);
        }
        
        registers[REG_R0] = 0;
    }

    private void handleGetppid() { registers[REG_R0] = 1; }

    private void handleGetuid() { registers[REG_R0] = id; }

    private void handleGetgid() { registers[REG_R0] = 1000; }


    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); } 
    
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    
    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }
}
