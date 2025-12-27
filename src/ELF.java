import javax.microedition.io.file.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// ELF ARM 32 Emulator
public class ELF implements Runnable {
    private OpenTTY midlet;
    private Object stdout;
    private Hashtable scope;
    private String pid;
    private int id;
    // |
    private byte[] memory;
    private int[] registers;
    private int pc, stackPointer;
    private boolean running;
    
    // Registrador de flags CPSR
    private int cpsr;
    
    // File descriptors
    private Hashtable fileDescriptors;
    private int nextFd;
    
    // Constantes ELF
    private static final int EI_NIDENT = 16, ELFCLASS32 = 1, ELFDATA2LSB = 1, EM_ARM = 40, ET_EXEC = 2, PT_LOAD = 1;
    
    // Constantes ARM
    private static final int REG_R0 = 0, REG_R1 = 1, REG_R2 = 2, REG_R3 = 3, REG_R4 = 4, REG_R5 = 5, REG_R6 = 6, REG_R7 = 7, REG_R8 = 8, REG_R9 = 9, REG_R10 = 10, REG_R11 = 11, REG_R12 = 12, REG_SP = 13, REG_LR = 14, REG_PC = 15;

    // Bits do CPSR
    private static final int CPSR_F = 6, CPSR_T = 5, CPSR_I = 7, CPSR_N = 31, CPSR_Z = 30, CPSR_C = 29, CPSR_V = 28; // Overflow
    // Máscaras para bits do CPSR
    private static final int N_MASK = 1 << CPSR_N;
    private static final int Z_MASK = 1 << CPSR_Z;
    private static final int C_MASK = 1 << CPSR_C;
    private static final int V_MASK = 1 << CPSR_V;

    // Adicione estas constantes na seção de constantes:
    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;
    
    // Syscalls Linux ARM (EABI) - Atualizadas
    private static final int SYS_EXIT = 1, SYS_FORK = 2, SYS_READ = 3, SYS_WRITE = 4, SYS_OPEN = 5, SYS_CLOSE = 6;
    private static final int SYS_CREAT = 8;
    private static final int SYS_EXECVE = 11, SYS_CHDIR = 12, SYS_TIME = 13;
    private static final int SYS_GETPID = 20;
    private static final int SYS_KILL = 37;
    private static final int SYS_BRK = 45;
    private static final int SYS_GETCWD = 183;
    private static final int SYS_MMAP = 90, SYS_MUNMAP = 91;
    private static final int SYS_MPROTECT = 125;
    private static final int SYS_WAITPID = 72;
    private static final int SYS_IOCTL = 54;
    private static final int SYS_FSTAT = 108;
    private static final int SYS_STAT = 106;
    private static final int SYS_LSEEK = 19;
    private static final int SYS_GETTIMEOFDAY = 78;
    private static final int SYS_PIPE = 42;
    private static final int SYS_DUP2 = 63;
    private static final int SYS_SIGNAL = 48;
    private static final int SYS_SIGACTION = 67;
    
    // Flags de open
    private static final int O_RDONLY = 0, O_WRONLY = 1, O_RDWR = 2, O_CREAT = 64, O_TRUNC = 512, O_APPEND = 1024;
    
    // Constantes MMU
    private static final int PAGE_SIZE = 4096, PAGE_SHIFT = 12;
    private static final int PAGE_MASK = 0xFFFFF000;
    
    // Bits de proteção de página
    private static final int PROT_READ = 1, PROT_WRITE = 2, PROT_EXEC = 4;
    
    // Flags de mapeamento
    private static final int MAP_PRIVATE = 0x02;
    private static final int MAP_ANONYMOUS = 0x20;
    private static final int MAP_FIXED = 0x10;

    // Modos de processador ARM
    private static final int MODE_USR = 0x10;      // User mode
    private static final int MODE_FIQ = 0x11;      // FIQ mode
    private static final int MODE_IRQ = 0x12;      // IRQ mode
    private static final int MODE_SVC = 0x13;      // Supervisor mode
    private static final int MODE_ABT = 0x17;      // Abort mode
    private static final int MODE_UND = 0x1B;      // Undefined mode
    private static final int MODE_SYS = 0x1F;      // System mode

    private static final int SIGINT = 2;
    private static final int SIGCHLD = 17;
    private static final int SIGCONT = 18;
    private static final int SIGSTOP = 19;
    private static final int SIGTERM = 15;

    // Vetores de exceção ARM
    private static final int VECTOR_RESET = 0x00, VECTOR_UNDEF = 0x04, VECTOR_SWI = 0x08, VECTOR_PREFETCH_ABORT = 0x0C, VECTOR_DATA_ABORT = 0x10, VECTOR_RESERVED = 0x14, VECTOR_IRQ = 0x18, VECTOR_FIQ = 0x1C;

    // Bits de modo no CPSR
    private static final int MODE_MASK = 0x1F;
    
    // Endereços especiais
    private static final int MMAP_BASE = 0x40000000, MMAP_END = 0x80000000;   // Fim para mmap
    // Mascaras para I/F/T
    private static final int I_MASK = 1 << CPSR_I, F_MASK = 1 << CPSR_F, T_MASK = 1 << CPSR_T;

    // Registradores banked por modo
    private int[][] bankedRegisters;
    private int[] bankedSPSR;
    private int currentMode;
    
    // Estruturas MMU
    private Hashtable pageTable, pageProtections;  // Proteções por página
    private Hashtable childProcesses, pendingSignals, signalHandlers;
    private int nextMmapAddr;
    
    // Registradores do CP15 (simplificados)
    private int ttbr0;    // Translation Table Base Register 0
    private int dacr;     // Domain Access Control Register
    private int sctlr;    // System Control Register
    private boolean mmuEnabled;
    
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
        
        // Inicializar registradores banked
        this.bankedRegisters = new int[7][]; // 7 modos: FIQ, IRQ, SVC, ABT, UND, SYS (USR compartilha)
        this.bankedSPSR = new int[7];
        
        // Inicializar cada array de registradores banked
        for (int i = 0; i < bankedRegisters.length; i++) {
            bankedRegisters[i] = new int[16]; // R0-R15, mas apenas alguns são banked
        }
        
        // Inicializar modo atual como Supervisor (SVC) - padrão para kernel
        this.currentMode = MODE_SVC;
        this.cpsr = (cpsr & ~MODE_MASK) | MODE_SVC;
        
        // Inicializar file descriptors padrão
        fileDescriptors.put(new Integer(1), stdout); // stdout
        fileDescriptors.put(new Integer(2), stdout); // stderr

        this.childProcesses = new Hashtable(); // PID -> Hashtable de info
        this.pendingSignals = new Hashtable(); // PID -> Vector de sinais
        this.signalHandlers = new Hashtable(); // signum -> handler address
        
        
        // Inicializar MMU
        this.pageTable = new Hashtable();
        this.pageProtections = new Hashtable();
        this.nextMmapAddr = MMAP_BASE;
        this.ttbr0 = 0;
        this.dacr = 0x55555555;  // Todos domínios como cliente
        this.sctlr = 0;
        this.mmuEnabled = false;

        // Mapeamento básico
        mapPage(0x00000000, 0x00000000, PROT_READ | PROT_WRITE | PROT_EXEC);
        int stackPhys = stackPointer & PAGE_MASK;
        mapPage(stackPointer & PAGE_MASK, stackPhys, PROT_READ | PROT_WRITE);
        
        // Inicializar SPs banked com valores padrão
        setBankedRegister(REG_SP, MODE_SVC, stackPointer);
        setBankedRegister(REG_SP, MODE_IRQ, memory.length - 2048);
        setBankedRegister(REG_SP, MODE_ABT, memory.length - 3072);
        setBankedRegister(REG_SP, MODE_UND, memory.length - 4096);
        setBankedRegister(REG_SP, MODE_FIQ, memory.length - 5120);
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
                
                // Mapear esta região na MMU
                int p_flags = readIntLE(elfData, phdrOffset + 24);
                int prot = 0;
                if ((p_flags & 1) != 0) prot |= PROT_EXEC;  // PF_X
                if ((p_flags & 2) != 0) prot |= PROT_WRITE; // PF_W
                if ((p_flags & 4) != 0) prot |= PROT_READ;  // PF_R
                
                // Mapear páginas
                for (int addr = p_vaddr; addr < p_vaddr + p_memsz; addr += PAGE_SIZE) {
                    mapPage(addr, addr, prot);
                }
            }
        }
        
        return true;
    }

    public void run() {
        running = true;
        
        // Inicializar vetores de exceção se necessário
        if (readIntLE(memory, VECTOR_RESET) == 0) {
            // Criar vetores básicos (branch para si mesmo)
            for (int i = 0; i <= 0x1C; i += 4) {
                writeIntLE(memory, i, 0xEAFFFFFE); // B . (loop infinito)
            }
            
            // Vetor de reset aponta para pc atual
            writeIntLE(memory, VECTOR_RESET, pc);
        }
        
        Hashtable proc = midlet.genprocess("elf", id, null);
        proc.put("elf", this); midlet.sys.put(pid, proc);
        
        try {
            while (running && pc < memory.length - 3 && midlet.sys.containsKey(pid)) {
                int instruction = readInstruction(pc);
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
    }

    // Obter índice do modo no array banked
    private int getModeIndex(int mode) {
        switch (mode & MODE_MASK) {
            case MODE_FIQ: return 0;
            case MODE_IRQ: return 1;
            case MODE_SVC: return 2;
            case MODE_ABT: return 3;
            case MODE_UND: return 4;
            case MODE_SYS: return 5;
            case MODE_USR: return 6;
            default: return 2; // Default para SVC
        }
    }


    // Obter modo atual
    private int getCurrentMode() { return cpsr & MODE_MASK; }

    // Mudar para novo modo
    private void changeMode(int newMode) {
        int oldMode = getCurrentMode();
        
        if (oldMode != newMode) {
            // Salvar registradores do modo atual
            saveBankedRegisters(oldMode);
            
            // Atualizar CPSR
            cpsr = (cpsr & ~MODE_MASK) | (newMode & MODE_MASK);
            currentMode = newMode;
            
            // Restaurar registradores do novo modo
            restoreBankedRegisters(newMode);
        }
    }

    // Salvar registradores banked do modo atual
    private void saveBankedRegisters(int mode) {
        int idx = getModeIndex(mode);
        
        // SP e LR são sempre banked (exceto em SYS/USR)
        if (mode != MODE_USR && mode != MODE_SYS) {
            bankedRegisters[idx][REG_SP] = registers[REG_SP];
            bankedRegisters[idx][REG_LR] = registers[REG_LR];
        }
        
        // FIQ tem R8-R12 banked
        if (mode == MODE_FIQ) { for (int i = 8; i <= 12; i++) { bankedRegisters[idx][i] = registers[i]; } }
        
        // Salvar SPSR
        if (mode != MODE_USR && mode != MODE_SYS) { bankedSPSR[idx] = cpsr; }
    }

    // Restaurar registradores banked do modo
    private void restoreBankedRegisters(int mode) {
        int idx = getModeIndex(mode);
        
        // Restaurar SP e LR (exceto em SYS/USR)
        if (mode != MODE_USR && mode != MODE_SYS) {
            registers[REG_SP] = bankedRegisters[idx][REG_SP];
            registers[REG_LR] = bankedRegisters[idx][REG_LR];
        }
        
        // FIQ tem R8-R12 banked
        if (mode == MODE_FIQ) { for (int i = 8; i <= 12; i++) { registers[i] = bankedRegisters[idx][i]; } }
        
        // Restaurar SPSR
        if (mode != MODE_USR && mode != MODE_SYS) {
            // SPSR para CPSR quando voltando de exceção
            // Isso normalmente é feito com instruções explícitas
        }
    }

    // Acessar registrador banked
    private int getBankedRegister(int reg, int mode) { int idx = getModeIndex(mode); return bankedRegisters[getModeIndex(mode)][reg]; }
    private void setBankedRegister(int reg, int mode, int value) { bankedRegisters[getModeIndex(mode)][reg] = value; }

    private void executeInstruction(int instruction) {
        try {
            // Instruções MRS (Move from Status Register to Register)
            if ((instruction & 0x0FF00000) == 0x01000000) {
                int rd = (instruction >> 12) & 0xF;
                boolean spsr = (instruction & (1 << 22)) != 0;
                
                if (spsr) {
                    // MRS Rd, SPSR - ler SPSR do modo atual
                    int mode = getCurrentMode();
                    if (mode != MODE_USR && mode != MODE_SYS) {
                        int idx = getModeIndex(mode);
                        registers[rd] = bankedSPSR[idx];
                    } else {
                        // Em USR/SYS, SPSR não está acessível
                        handleUndefinedInstruction();
                    }
                } else {
                    // MRS Rd, CPSR
                    registers[rd] = cpsr;
                }
                return;
            }
            
            // Instruções MSR (Move Register to Status Register)
            if ((instruction & 0x0FB00000) == 0x01200000) {
                boolean spsr = (instruction & (1 << 22)) != 0;
                int fieldMask = (instruction >> 16) & 0xF;
                int operand;
                
                // Determinar operand
                if ((instruction & (1 << 25)) != 0) {
                    // Immediate operand
                    int imm = instruction & 0xFF;
                    int rotate = ((instruction >> 8) & 0xF) * 2;
                    operand = rotateRight(imm, rotate);
                } else {
                    // Register operand
                    int rm = instruction & 0xF;
                    operand = registers[rm];
                }
                
                int mask = 0;
                if ((fieldMask & 0x1) != 0) mask |= 0x000000FF; // Control field
                if ((fieldMask & 0x2) != 0) mask |= 0x0000FF00; // Extension field
                if ((fieldMask & 0x4) != 0) mask |= 0x00FF0000; // Status field
                if ((fieldMask & 0x8) != 0) mask |= 0xFF000000; // Flags field
                
                if (spsr) {
                    // MSR SPSR, operand
                    int mode = getCurrentMode();
                    if (mode != MODE_USR && mode != MODE_SYS) {
                        int idx = getModeIndex(mode);
                        int oldSPSR = bankedSPSR[idx];
                        int newSPSR = (oldSPSR & ~mask) | (operand & mask);
                        bankedSPSR[idx] = newSPSR;
                    }
                } else {
                    // MSR CPSR, operand
                    int oldCPSR = cpsr;
                    int newCPSR = (oldCPSR & ~mask) | (operand & mask);
                    
                    // Verificar mudança de modo
                    int oldMode = oldCPSR & MODE_MASK;
                    int newMode = newCPSR & MODE_MASK;
                    
                    if (oldMode != newMode) {
                        changeMode(newMode);
                    }
                    
                    cpsr = newCPSR;
                }
                return;
            }

            // Instruções MCR/MRC para CP15 (controle da MMU)
            if ((instruction & 0x0F000010) == 0x0E000010) {
                int cpnum = (instruction >> 8) & 0xF;
                if (cpnum == 15) {  // CP15
                    int opcode1 = (instruction >> 21) & 0x7;
                    int crn = (instruction >> 16) & 0xF;
                    int crm = instruction & 0xF;
                    int opcode2 = (instruction >> 5) & 0x7;
                    int rt = (instruction >> 12) & 0xF;
                    
                    // MCR: mover do registrador ARM para co-processador
                    if ((instruction & (1 << 20)) == 0) {
                        int value = registers[rt];
                        
                        // Configurar TTBR0
                        if (crn == 2 && opcode1 == 0) {
                            ttbr0 = value;
                        }
                        // Configurar DACR
                        else if (crn == 3 && opcode1 == 0) {
                            dacr = value;
                        }
                        // Ativar/desativar MMU (SCTLR)
                        else if (crn == 1 && opcode1 == 0 && crm == 0 && opcode2 == 0) {
                            sctlr = value;
                            mmuEnabled = (value & 1) != 0;  // Bit 0 = M (MMU enable)
                        }
                    }
                    // MRC: mover do co-processador para registrador ARM
                    else {
                        int value = 0;
                        
                        // Ler TTBR0
                        if (crn == 2 && opcode1 == 0) {
                            value = ttbr0;
                        }
                        // Ler DACR
                        else if (crn == 3 && opcode1 == 0) {
                            value = dacr;
                        }
                        // Ler SCTLR
                        else if (crn == 1 && opcode1 == 0 && crm == 0 && opcode2 == 0) {
                            value = sctlr;
                        }
                        
                        registers[rt] = value;
                    }
                }
                return;
            }

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
                    if (isByte) { 
                        registers[rd] = readByte(address) & 0xFF; 
                    } else {
                        // Alinhar para palavra (4 bytes)
                        int alignedAddr = address & ~3;
                        registers[rd] = readIntLEWithMMU(alignedAddr);
                    }
                } 
                else { 
                    if (isByte) { 
                        writeByte(address, (byte)(registers[rd] & 0xFF)); 
                    } else { 
                        writeIntLEWithMMU(address, registers[rd]); 
                    } 
                }
                
                if (!preIndexed) { if (addOffset) { registers[rn] += offset; } else { registers[rn] -= offset; } }
                return;
            }
            
            // Branch Instructions
            if ((instruction & 0x0E000000) == 0x0A000000) {
                int offset = instruction & 0x00FFFFFF;
                if ((offset & 0x00800000) != 0) { offset |= 0xFF000000; }
                offset <<= 2;
                
                boolean link = (instruction & (1 << 24)) != 0;
                
                if (link) { registers[REG_LR] = pc; }
                
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
                
                if (isAdd) { registers[rd] = pcValue + offset; } else { registers[rd] = pcValue - offset; }

                return;
            }

            // NOP
            if (instruction == 0xE1A00000) { return; }
        } catch (Exception e) {
            // Tratar page fault como exceção de data abort
            handleDataAbort(e.getMessage());
        }
    }
    
    // Métodos MMU
    
    // Mapear uma página virtual para física
    private void mapPage(int vaddr, int paddr, int prot) {
        int vpage = vaddr & PAGE_MASK;
        int ppage = paddr & PAGE_MASK;
        
        pageTable.put(new Integer(vpage), new Integer(ppage));
        pageProtections.put(new Integer(vpage), new Integer(prot));
    }
    
    // Desmapear uma página
    private void unmapPage(int vaddr) {
        int vpage = vaddr & PAGE_MASK;
        pageTable.remove(new Integer(vpage));
        pageProtections.remove(new Integer(vpage));
    }
    
    // Traduzir endereço virtual para físico com verificação de proteção
    private int translateAddress(int vaddr, boolean isWrite, boolean isExecute) throws Exception {
        // Se MMU desligada, endereço físico = virtual (para boot)
        if (!mmuEnabled) {
            return vaddr;
        }
        
        int vpage = vaddr & PAGE_MASK;
        Integer physPage = (Integer) pageTable.get(new Integer(vpage));
        
        // Page fault se não mapeado
        if (physPage == null) {
            throw new Exception("Page fault at address " + toHex(vaddr) + " - not mapped");
        }
        
        // Corrigir o cast aqui
        Integer protObj = (Integer) pageProtections.get(new Integer(vpage));
        if (protObj == null) {
            throw new Exception("Page fault at address " + toHex(vaddr) + " - no protection info");
        }
        int prot = protObj.intValue();
    
        // Verificar permissões
        if (isWrite && (prot & PROT_WRITE) == 0) {
            throw new Exception("Page fault at address " + toHex(vaddr) + " - write protection");
        }
        
        if (isExecute && (prot & PROT_EXEC) == 0) {
            throw new Exception("Page fault at address " + toHex(vaddr) + " - execute protection");
        }
        
        if (!isWrite && !isExecute && (prot & PROT_READ) == 0) {
            throw new Exception("Page fault at address " + toHex(vaddr) + " - read protection");
        }
        
        // Calcular endereço físico
        return physPage.intValue() | (vaddr & ~PAGE_MASK);
    }
    
    // Procurar região livre para mmap
    private int findFreeMmapRegion(int size) {
        int addr = nextMmapAddr;
        int endAddr = addr + size;
        
        // Alinhar para página
        addr = (addr + PAGE_SIZE - 1) & PAGE_MASK;
        
        // Verificar se há sobreposição com páginas já mapeadas
        Enumeration keys = pageTable.keys();
        while (keys.hasMoreElements()) {
            Integer vpage = (Integer) keys.nextElement();
            int pageStart = vpage.intValue();
            int pageEnd = pageStart + PAGE_SIZE;
            
            if (addr < pageEnd && endAddr > pageStart) {
                // Sobreposição, pular para após esta página
                addr = pageEnd;
                endAddr = addr + size;
            }
        }
        
        // Verificar limite máximo
        if (endAddr >= MMAP_END) {
            addr = MMAP_BASE;  // Reciclar
            endAddr = addr + size;
        }
        
        nextMmapAddr = endAddr + PAGE_SIZE;  // Deixar margem
        return addr;
    }
    
    // Métodos de acesso à memória com MMU
    
    private byte readByte(int addr) throws Exception {
        int physAddr = translateAddress(addr, false, false);
        if (physAddr >= 0 && physAddr < memory.length) {
            return memory[physAddr];
        }
        return 0;
    }
    
    private void writeByte(int addr, byte value) throws Exception {
        int physAddr = translateAddress(addr, true, false);
        if (physAddr >= 0 && physAddr < memory.length) {
            memory[physAddr] = value;
        }
    }
    
    private int readIntLEWithMMU(int addr) throws Exception {
        int physAddr = translateAddress(addr, false, false);
        return readIntLE(memory, physAddr);
    }
    
    private void writeIntLEWithMMU(int addr, int value) throws Exception {
        int physAddr = translateAddress(addr, true, false);
        writeIntLE(memory, physAddr, value);
    }
    
    // Para leitura de instruções (pode ter proteção EXEC diferente)
    private int readInstruction(int addr) throws Exception {
        int physAddr = translateAddress(addr, false, true);
        return readIntLE(memory, physAddr);
    }
    
    // Handlers de exceção MMU
    
    // Data Abort handler
    private void handleDataAbort(String message) {
        // Salvar estado
        int savedCPSR = cpsr;
        
        // Mudar para modo Abort
        cpsr = (cpsr & ~0x1F) | 0x17;  // Modo Abort
        
        // Salvar PC e CPSR (em registers banked do modo Abort - simplificado)
        registers[REG_LR] = pc - 4;  // PC da instrução que falhou
        // Em ARM real, LR_abort = PC + 4 ou +8 dependendo do tipo
        
        // Ir para vetor de Data Abort (0x10)
        pc = 0x10;
        
        // Em sistema real, faríamos mais aqui...
        midlet.print("Data Abort: " + message, stdout);
        running = false;
    }
    
    // Prefetch Abort handler (para falhas em busca de instrução)
    private void handlePrefetchAbort(String message) {
        cpsr = (cpsr & ~0x1F) | 0x17;  // Modo Abort
        registers[REG_LR] = pc;  // PC da instrução que falhou
        pc = 0x0C;  // Vetor de Prefetch Abort
        
        midlet.print("Prefetch Abort: " + message, stdout);
        running = false;
    }
    
    // Métodos originais (sem modificação)
    
    private int applyShift(int value, int shift_type, int shift_amount, int carry_in) {
        // conteudo original completo
        if (shift_amount == 0) { return value; }
        
        switch (shift_type) {
            case 0: // LSL (Logical Shift Left)
                if (shift_amount >= 32) { cpsr = (cpsr & ~C_MASK) | ((value << (shift_amount - 1)) >>> 31) << CPSR_C; return 0; }
                cpsr = (cpsr & ~C_MASK) | ((value << (shift_amount - 1)) >>> 31) << CPSR_C;
                return value << shift_amount;
                
            case 1: // LSR (Logical Shift Right)
                if (shift_amount >= 32) { cpsr = (cpsr & ~C_MASK) | ((value >>> (shift_amount - 1)) & 1) << CPSR_C; return 0; }
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
        // conteudo original completo
        // Atualizar flag N (Negative)
        if ((result & 0x80000000) != 0) { cpsr |= N_MASK; } else { cpsr &= ~N_MASK; }
        
        // Atualizar flag Z (Zero)
        if (result == 0) { cpsr |= Z_MASK; } else { cpsr &= ~Z_MASK; }
        
        // Atualizar flag C (Carry) se fornecido
        if (carry >= 0) { if (carry != 0) { cpsr |= C_MASK; } else { cpsr &= ~C_MASK; } }
    }
    
    private void updateOverflow(int operand1, int operand2, int result, int opcode) {
        // conteudo original completo
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
        
        if (overflow) { cpsr |= V_MASK; } else { cpsr &= ~V_MASK; }
    }
    
    private void handleSyscall(int number) {
        if (number == SYS_FORK) { handleFork(); }
        else if (number == SYS_WRITE) { handleWrite(); }
        else if (number == SYS_READ) { handleRead(); }
        else if (number == SYS_OPEN) { handleOpen(); }
        else if (number == SYS_CLOSE) { handleClose(); }
        else if (number == SYS_CREAT) { handleCreat();}
        else if (number == SYS_TIME) { handleTime(); }
        else if (number == SYS_CHDIR) { handleChdir(); }
        else if (number == SYS_EXIT) { handleExit(); }
        else if (number == SYS_GETPID) { handleGetpid(); }
        else if (number == SYS_KILL) { handleKill(); }
        else if (number == SYS_GETCWD) { handleGetcwd(); }
        else if (number == SYS_BRK) { registers[REG_R0] = memory.length; }
        else if (number == SYS_MMAP) { handleMmap(); }
        else if (number == SYS_MUNMAP) { handleMunmap(); }
        else if (number == SYS_MPROTECT) { handleMprotect(); }
        else if (number == SYS_IOCTL) { handleIoctl(); }
        else if (number == SYS_STAT) { handleStat(number); }
        else if (number == SYS_LSEEK) { handleLseek(); }
        else if (number == SYS_GETTIMEOFDAY) { handleGettimeofday(); }
        else if (number == SYS_SIGACTION) { handleSignal(number); }
        else { registers[REG_R0] = -1; }
    }
    
    // Handlers de syscalls MMU
    
    private void handleMmap() {
        int addr = registers[REG_R0];      // Endereço solicitado
        int length = registers[REG_R1];    // Tamanho
        int prot = registers[REG_R2];      // Proteção
        int flags = registers[REG_R3];     // Flags
        int fd = registers[REG_R4];        // File descriptor
        int offset = registers[REG_R5];    // Offset
        
        // Arredondar para múltiplo de página
        int alignedLength = (length + PAGE_SIZE - 1) & PAGE_MASK;
        
        // Se addr = 0, kernel escolhe
        if (addr == 0 || (flags & MAP_FIXED) == 0) {
            addr = findFreeMmapRegion(alignedLength);
        }
        
        // Mapear páginas
        for (int i = 0; i < alignedLength; i += PAGE_SIZE) {
            int pageAddr = addr + i;
            
            // Para MAP_ANONYMOUS, criamos páginas zeradas
            if ((flags & MAP_ANONYMOUS) != 0) {
                // Encontrar página física livre (simplificado: usar endereço virtual como físico)
                int physAddr = pageAddr;
                mapPage(pageAddr, physAddr, prot);
                
                // Zerar a página
                for (int j = 0; j < PAGE_SIZE && physAddr + j < memory.length; j++) {
                    memory[physAddr + j] = 0;
                }
            } else {
                // Mapeamento de arquivo (não implementado aqui)
                registers[REG_R0] = -1;  // ENOSYS
                return;
            }
        }
        
        registers[REG_R0] = addr;  // Retornar endereço mapeado
    }
    
    private void handleMunmap() {
        int addr = registers[REG_R0];
        int length = registers[REG_R1];
        
        int alignedAddr = addr & PAGE_MASK;
        int alignedLength = (length + PAGE_SIZE - 1) & PAGE_MASK;
        
        for (int i = 0; i < alignedLength; i += PAGE_SIZE) {
            unmapPage(alignedAddr + i);
        }
        
        registers[REG_R0] = 0;  // Sucesso
    }
    
    private void handleMprotect() {
        int addr = registers[REG_R0];
        int length = registers[REG_R1];
        int prot = registers[REG_R2];
        
        int alignedAddr = addr & PAGE_MASK;
        int alignedLength = (length + PAGE_SIZE - 1) & PAGE_MASK;
        
        for (int i = 0; i < alignedLength; i += PAGE_SIZE) {
            int vpage = alignedAddr + i;
            Integer physPage = (Integer) pageTable.get(new Integer(vpage));
            
            if (physPage != null) {
                pageProtections.put(new Integer(vpage), new Integer(prot));
            }
        }
        
        registers[REG_R0] = 0;  // Sucesso
    }
    
    // Métodos originais de syscall (sem modificação)
    
    private void handleFork() {
        // conteudo original completo
        // Em J2ME não temos fork real, simulamos retornando 0 para o processo filho (simulado)
        // No sistema real, isso criaria um novo processo
        // Por simplicidade, retornamos -1 (erro) indicando que fork não é suportado
        registers[REG_R0] = -1; // ENOSYS - Function not implemented
    }
    
    private void handleWrite() {
        // conteudo original completo
        int fd = registers[REG_R0];
        int buf = registers[REG_R1];
        int count = registers[REG_R2];
        
        if (count <= 0 || buf < 0 || buf >= memory.length) {
            registers[REG_R0] = -1;
            return;
        }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 1 || fd == 2) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < count && buf + i < memory.length; i++) {
                char c = (char)(memory[buf + i] & 0xFF);
                if (c >= 32 || c == '\n' || c == '\r' || c == '\t') {
                    sb.append(c);
                }
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
        // conteudo original completo
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
        // conteudo original completo
        int pathAddr = registers[REG_R0];
        int flags = registers[REG_R1];
        int mode = registers[REG_R2];
        
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }
        
        // Ler o caminho da memória
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
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
        // conteudo original completo
        // creat(path, mode) é equivalente a open(path, O_CREAT | O_WRONLY | O_TRUNC, mode)
        int pathAddr = registers[REG_R0];
        int mode = registers[REG_R1];
        
        // Simular open com flags O_CREAT | O_WRONLY | O_TRUNC
        registers[REG_R1] = O_CREAT | O_WRONLY | O_TRUNC;
        registers[REG_R2] = mode;
        
        handleOpen();
    }
    
    private void handleClose() {
        // conteudo original completo
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
        // conteudo original completo
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
        // conteudo original completo
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
        // conteudo original completo
        try { 
            int pidValue = Integer.parseInt(this.pid); 
            registers[REG_R0] = pidValue; 
        } catch (NumberFormatException e) { 
            registers[REG_R0] = 1; 
        } 
    }
    
    private void handleKill() {
        // conteudo original completo
        int pid = registers[REG_R0];
        int sig = registers[REG_R1];

        String targetPid = String.valueOf(pid);

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
        // conteudo original completo
        int status = registers[REG_R0];
        running = false;

        // Enviar SIGCHLD aos pais dos processos filhos
        Enumeration childPids = childProcesses.keys();
        while (childPids.hasMoreElements()) {
            String childPid = (String) childPids.nextElement();
            Hashtable childInfo = (Hashtable) childProcesses.get(childPid);
            if (childInfo.containsKey("elf")) {
                ELF childElf = (ELF) childInfo.get("elf");
                childElf.kill();
            }
        }
        childProcesses.clear();
        
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
        // conteudo original completo
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

    private void handleReset() { cpsr = (cpsr & ~MODE_MASK) | MODE_SVC; pc = readIntLE(memory, VECTOR_RESET); running = true; }

    // Handler de instrução indefinida
    private void handleUndefinedInstruction() {
        // Salvar estado atual
        int oldCPSR = cpsr;
        
        // Mudar para modo Undefined
        changeMode(MODE_UND);
        
        // Salvar LR e SPSR
        registers[REG_LR] = pc - 4; // Endereço da instrução indefinida
        int idx = getModeIndex(MODE_UND);
        bankedSPSR[idx] = oldCPSR;
        
        // Desabilitar IRQs
        cpsr |= I_MASK;
        
        // Ir para vetor de undefined instruction
        pc = VECTOR_UNDEF;
        
        midlet.print("Undefined Instruction at " + toHex(pc - 4), stdout);
    }

    // Handler de SWI (syscall já implementado)
    private void handleSoftwareInterrupt(int swiNumber) {
        // Salvar estado atual
        int oldCPSR = cpsr;
        
        // Mudar para modo Supervisor
        changeMode(MODE_SVC);
        
        // Salvar LR e SPSR
        registers[REG_LR] = pc - 4; // Endereço da instrução SWI
        int idx = getModeIndex(MODE_SVC);
        bankedSPSR[idx] = oldCPSR;
        
        // Desabilitar IRQs
        cpsr |= I_MASK;
        
        // Chamar handler de syscall (já implementado)
        if (swiNumber == 0) {
            handleSyscall(registers[REG_R7]);
        } else {
            handleSyscall(swiNumber);
        }
        
        // Retornar (instrução MOVS PC, LR)
        // Isso seria feito no handler de syscall
    }

    // Modifique o handler de Data Abort:
    private void handleDataAbort(String message) {
        // Salvar estado atual
        int oldCPSR = cpsr;
        
        // Mudar para modo Abort
        changeMode(MODE_ABT);
        
        // Salvar LR e SPSR
        registers[REG_LR] = pc - 8; // PC da instrução que causou abort + 8
        int idx = getModeIndex(MODE_ABT);
        bankedSPSR[idx] = oldCPSR;
        
        // Desabilitar IRQs
        cpsr |= I_MASK;
        
        // Ir para vetor de Data Abort
        pc = VECTOR_DATA_ABORT;
        
        midlet.print("Data Abort: " + message + " at " + toHex(registers[REG_LR]), stdout);
    }

    // Handler de Prefetch Abort
    private void handlePrefetchAbort(String message) {
        int oldCPSR = cpsr;
        changeMode(MODE_ABT);
        registers[REG_LR] = pc - 4;
        int idx = getModeIndex(MODE_ABT);
        bankedSPSR[idx] = oldCPSR;
        cpsr |= I_MASK;
        pc = VECTOR_PREFETCH_ABORT;
        
        midlet.print("Prefetch Abort: " + message + " at " + toHex(registers[REG_LR]), stdout);
    }

    // Handler de IRQ (simplificado)
    private void handleIRQ() {
        // Salvar estado atual
        int oldCPSR = cpsr;
        
        // Mudar para modo IRQ
        changeMode(MODE_IRQ);
        
        // Salvar LR e SPSR
        registers[REG_LR] = pc + 4; // PC + 4 para retorno
        int idx = getModeIndex(MODE_IRQ);
        bankedSPSR[idx] = oldCPSR;
        
        // Desabilitar IRQs adicionais
        cpsr |= I_MASK;
        
        // Ir para vetor de IRQ
        pc = VECTOR_IRQ;
    }

    // Handler de FIQ (simplificado)
    private void handleFIQ() {
        int oldCPSR = cpsr;
        changeMode(MODE_FIQ);
        registers[REG_LR] = pc + 4;
        int idx = getModeIndex(MODE_FIQ);
        bankedSPSR[idx] = oldCPSR;
        cpsr |= (I_MASK | F_MASK);
        pc = VECTOR_FIQ;
    }

    private void handleIoctl() {
        int fd = registers[REG_R0], request = registers[REG_R1], argp = registers[REG_R2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey)) { registers[REG_R0] = -9; return; }
        
        try {
            Object stream = fileDescriptors.get(fdKey);
            
            // Comandos de terminal (TIOC*)
            if (request == 0x5401) { // TCGETS - get terminal attributes
                // Estrutura termios simplificada (36 bytes)
                for (int i = 0; i < 36; i++) {
                    writeByte(argp + i, (byte)0);
                }
                registers[REG_R0] = 0;
                return;
                
            } else if (request == 0x5402) { // TCSETS - set terminal attributes
                // Ignorar, sempre sucesso
                registers[REG_R0] = 0;
                return;
                
            } else if (request == 0x5413) { // TIOCGWINSZ - get window size
                // winsize struct (8 bytes)
                try {
                    writeShortWithMMU(argp, (short)24);
                    writeShortWithMMU(argp + 2, (short)80);
                    writeShortWithMMU(argp + 4, (short)0);
                    writeShortWithMMU(argp + 6, (short)0);
                } catch (Exception e) {
                    registers[REG_R0] = -1;
                    return;
                }

                registers[REG_R0] = 0;
                return;
                
            } else if (request == 0x541B) { // FIONREAD - bytes disponíveis
                if (stream instanceof InputStream) {
                    int available = ((InputStream) stream).available();
                    writeIntLEWithMMU(argp, available);
                    registers[REG_R0] = 0;
                    return;
                }
            }
            
            // Comandos de arquivo (FIO*)
            if (request == 0xBE01) { // FIONBIO - non-blocking I/O
                // Ignorar, sempre sucesso
                registers[REG_R0] = 0;
                return;
            }
            
            // Dispositivo especial /dev/null
            if (stream instanceof ByteArrayInputStream) {
                ByteArrayInputStream bais = (ByteArrayInputStream) stream;
                if (bais.available() == 0) { // Provavelmente /dev/null
                    registers[REG_R0] = 0;
                    return;
                }
            }
            
            // Não suportado
            registers[REG_R0] = -25; // ENOTTY
            
        } catch (Exception e) {
            registers[REG_R0] = -1; // EPERM
        }
    }

    private void handleStat(int syscall) {
        int pathAddr = registers[REG_R0], statbuf = registers[REG_R1];
        
        try {
            String path = null;
            
            if (syscall == SYS_STAT) {
                path = readString(pathAddr);
            } else if (syscall == SYS_FSTAT) {
                // Para fstat, pathAddr é na verdade o file descriptor
                int fd = pathAddr;
                Integer fdKey = new Integer(fd);
                if (fileDescriptors.containsKey(fdKey)) {
                    Object stream = fileDescriptors.get(fdKey);
                    // Determinar tipo pelo stream
                    if (stream == stdout || fd == 1 || fd == 2) {
                        path = "/dev/stdout";
                    } else if (fd == 0) {
                        path = "/dev/stdin";
                    } else if (stream instanceof ByteArrayInputStream) {
                        ByteArrayInputStream bais = (ByteArrayInputStream) stream;
                        if (bais.available() == 0) {
                            path = "/dev/null";
                        } else {
                            path = "/tmp/fd_" + fd;
                        }
                    } else {
                        path = "/proc/fd/" + fd;
                    }
                } else {
                    registers[REG_R0] = -9; // EBADF
                    return;
                }
            }
            
            if (path == null) {
                registers[REG_R0] = -2; // ENOENT
                return;
            }
            
            // Verificar se existe
            boolean exists = false;
            boolean isDir = false;
            long size = 0;
            long mtime = System.currentTimeMillis() / 1000;
            
            if (path.startsWith("/dev/")) {
                exists = true;
                size = 0;
            } else if (path.startsWith("/tmp/")) {
                String tmpName = path.substring(5);
                exists = midlet.tmp.containsKey(tmpName);
                if (exists) {
                    String content = (String) midlet.tmp.get(tmpName);
                    size = content.length();
                }
            } else if (path.startsWith("/proc/") || path.startsWith("/sys/")) {
                exists = true;
                isDir = path.endsWith("/");
                size = 0;
            } else {
                // Usar infraestrutura do OpenTTY
                InputStream is = midlet.getInputStream(path);
                if (is != null) {
                    exists = true;
                    // Calcular tamanho
                    byte[] buffer = new byte[1024];
                    int total = 0;
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        total += read;
                    }
                    is.close();
                    size = total;
                } else if (path.endsWith("/") || midlet.fs.containsKey(path)) {
                    exists = true;
                    isDir = true;
                }
            }
            
            if (!exists) {
                registers[REG_R0] = -2; // ENOENT
                return;
            }
            
            // Preencher struct stat (48 bytes para ARM EABI)
            int offset = 0;
            
            // st_dev (device)
            writeIntLEWithMMU(statbuf + offset, 2048); // Major/minor
            offset += 4;
            
            // st_ino (inode)
            writeIntLEWithMMU(statbuf + offset, path.hashCode() & 0x7FFFFFFF);
            offset += 4;
            
            // st_mode (type + permissions)
            int mode = isDir ? 0x4000 : 0x8000; // S_IFDIR ou S_IFREG
            if (path.startsWith("/bin/") || path.endsWith(".elf") || 
                path.endsWith(".lua") || path.endsWith(".sh")) {
                mode |= 0755; // rwxr-xr-x
            } else {
                mode |= 0644; // rw-r--r--
            }
            writeIntLEWithMMU(statbuf + offset, mode);
            offset += 4;
            
            // st_nlink (links)
            writeIntLEWithMMU(statbuf + offset, 1);
            offset += 4;
            
            // st_uid (owner)
            writeIntLEWithMMU(statbuf + offset, id == 0 ? 0 : 1000);
            offset += 4;
            
            // st_gid (group)
            writeIntLEWithMMU(statbuf + offset, id == 0 ? 0 : 1000);
            offset += 4;
            
            // st_rdev
            writeIntLEWithMMU(statbuf + offset, 0);
            offset += 4;
            
            // st_size
            writeIntLEWithMMU(statbuf + offset, (int)size);
            offset += 4;
            
            // st_blksize
            writeIntLEWithMMU(statbuf + offset, 4096);
            offset += 4;
            
            // st_blocks
            writeIntLEWithMMU(statbuf + offset, (int)((size + 511) / 512));
            offset += 4;
            
            // st_atime
            writeIntLEWithMMU(statbuf + offset, (int)mtime);
            offset += 4;
            
            // st_mtime
            writeIntLEWithMMU(statbuf + offset, (int)mtime);
            offset += 4;
            
            // st_ctime
            writeIntLEWithMMU(statbuf + offset, (int)mtime);
            offset += 4;
            
            registers[REG_R0] = 0;
            
        } catch (Exception e) {
            registers[REG_R0] = -1; // EPERM
        }
    }

    private void handleLseek() {
        int fd = registers[REG_R0];
        int offset = registers[REG_R1];
        int whence = registers[REG_R2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey)) {
            registers[REG_R0] = -9; // EBADF
            return;
        }
        
        try {
            Object stream = fileDescriptors.get(fdKey);
            
            if (stream instanceof ByteArrayInputStream) {
                ByteArrayInputStream bais = (ByteArrayInputStream) stream;
                
                // Ler todo o conteúdo para array
                byte[] data = new byte[bais.available()];
                int total = 0;
                int read;
                while ((read = bais.read(data, total, data.length - total)) != -1) {
                    total += read;
                    if (total >= data.length) break;
                }
                
                int newPos = 0;
                switch (whence) {
                    case SEEK_SET:
                        newPos = offset;
                        break;
                    case SEEK_CUR:
                        // Não temos posição atual, assumir 0
                        newPos = offset;
                        break;
                    case SEEK_END:
                        newPos = total + offset;
                        break;
                    default:
                        registers[REG_R0] = -22; // EINVAL
                        return;
                }
                
                if (newPos < 0 || newPos > total) {
                    registers[REG_R0] = -22; // EINVAL
                    return;
                }
                
                // Criar novo stream na posição correta
                ByteArrayInputStream newStream = new ByteArrayInputStream(data);
                newStream.skip(newPos);
                fileDescriptors.put(fdKey, newStream);
                
                registers[REG_R0] = newPos;
                return;
                
            } else if (stream instanceof ByteArrayOutputStream) {
                ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
                byte[] data = baos.toByteArray();
                
                int newPos = 0;
                switch (whence) {
                    case SEEK_SET:
                        newPos = offset;
                        break;
                    case SEEK_CUR:
                        // Para output stream, posição é o fim
                        newPos = data.length + offset;
                        break;
                    case SEEK_END:
                        newPos = data.length + offset;
                        break;
                    default:
                        registers[REG_R0] = -22; // EINVAL
                        return;
                }
                
                if (newPos < 0) {
                    registers[REG_R0] = -22; // EINVAL
                    return;
                }
                
                if (newPos > data.length) {
                    // Estender o buffer
                    byte[] newData = new byte[newPos];
                    System.arraycopy(data, 0, newData, 0, data.length);
                    // Preencher com zeros
                    for (int i = data.length; i < newPos; i++) {
                        newData[i] = 0;
                    }
                    ByteArrayOutputStream newStream = new ByteArrayOutputStream();
                    newStream.write(newData, 0, newPos);
                    fileDescriptors.put(fdKey, newStream);
                } else {
                    // Criar stream na posição
                    ByteArrayOutputStream newStream = new ByteArrayOutputStream() {
                        private int position = newPos;
                        public void write(int b) {
                            if (position < data.length) {
                                data[position] = (byte) b;
                            } else {
                                // Extender
                                byte[] newData = new byte[position + 1];
                                System.arraycopy(data, 0, newData, 0, data.length);
                                newData[position] = (byte) b;
                                data = newData;
                            }
                            position++;
                            buf = data;
                            count = position;
                        }
                    };
                    newStream.write(data, 0, newPos);
                    fileDescriptors.put(fdKey, newStream);
                }
                
                registers[REG_R0] = newPos;
                return;
                
            } else {
                // Para outros streams (stdin/stdout, sockets), não suportado
                registers[REG_R0] = -29; // ESPIPE
                return;
            }
            
        } catch (Exception e) {
            registers[REG_R0] = -1; // EPERM
        }
    }

    private void handleGettimeofday() {
        int tvAddr = registers[REG_R0];
        int tzAddr = registers[REG_R1];
        
        try {
            long currentTime = System.currentTimeMillis();
            long seconds = currentTime / 1000;
            long microseconds = (currentTime % 1000) * 1000;
            
            if (tvAddr != 0) {
                // struct timeval { time_t tv_sec; suseconds_t tv_usec; }
                writeIntLEWithMMU(tvAddr, (int)seconds);
                writeIntLEWithMMU(tvAddr + 4, (int)microseconds);
            }
            
            if (tzAddr != 0) {
                // struct timezone { int tz_minuteswest; int tz_dsttime; }
                // UTC+0, sem DST
                writeIntLEWithMMU(tzAddr, 0);
                writeIntLEWithMMU(tzAddr + 4, 0);
            }
            
            registers[REG_R0] = 0;
            
        } catch (Exception e) {
            registers[REG_R0] = -1; // EPERM
        }
    }


    private void handleDup2() {
        int oldfd = registers[REG_R0], newfd = registers[REG_R1];
        Integer oldKey = new Integer(oldfd), newKey = new Integer(newfd);
        
        if (!fileDescriptors.containsKey(oldKey)) { registers[REG_R0] = -9; return; }
        
        try {
            // Fechar newfd se estiver aberto
            if (fileDescriptors.containsKey(newKey)) {
                Object oldStream = fileDescriptors.get(newKey);
                try {
                    if (oldStream instanceof InputStream) {
                        ((InputStream) oldStream).close();
                    } else if (oldStream instanceof OutputStream) {
                        ((OutputStream) oldStream).close();
                    }
                } catch (Exception e) {
                    // Ignorar erro ao fechar
                }
                fileDescriptors.remove(newKey);
            }
            
            // Duplicar referência
            Object original = fileDescriptors.get(oldKey);
            fileDescriptors.put(newKey, original);
            
            registers[REG_R0] = newfd;
            
        } catch (Exception e) {
            registers[REG_R0] = -1; // EPERM
        }
    }

    private void handleSignal(int syscall) {
        int signum = registers[REG_R0], handler = registers[REG_R1], oldact = registers[REG_R2];
        
        try {
            // Sinais suportados
            if (signum == SIGINT || signum == SIGTERM || signum == SIGCHLD) {
                if (syscall == SYS_SIGNAL) {
                    // signal(signum, handler)
                    Integer oldHandler = (Integer) signalHandlers.get(new Integer(signum));
                    if (oldact != 0 && oldHandler != null) {
                        writeIntLEWithMMU(oldact, oldHandler.intValue());
                    }
                    signalHandlers.put(new Integer(signum), new Integer(handler));
                    registers[REG_R0] = handler;
                    
                } else if (syscall == SYS_SIGACTION) {
                    // sigaction(signum, act, oldact)
                    if (oldact != 0) {
                        // Escrever old sigaction (simplificado)
                        writeIntLEWithMMU(oldact, 0); // sa_handler
                        writeIntLEWithMMU(oldact + 4, 0); // sa_mask
                        writeIntLEWithMMU(oldact + 8, 0); // sa_flags
                    }
                    
                    if (handler != 0) {
                        // Ler nova sigaction
                        int sa_handler = readIntLEWithMMU(handler);
                        int sa_mask = readIntLEWithMMU(handler + 4);
                        int sa_flags = readIntLEWithMMU(handler + 8);
                        
                        signalHandlers.put(new Integer(signum), new Integer(sa_handler));
                    }
                    
                    registers[REG_R0] = 0;
                }
            } else {
                registers[REG_R0] = -22; // EINVAL
            }
            
        } catch (Exception e) {
            registers[REG_R0] = -1; // EPERM
        }
    }

    private void deliverSignal(int signum, String targetPid) {
        // Enviar sinal para processo (simplificado)
        if (signum == SIGCHLD && targetPid.equals(pid)) {
            // Verificar se há handler registrado
            Integer handlerAddr = (Integer) signalHandlers.get(new Integer(SIGCHLD));
            if (handlerAddr != null && handlerAddr.intValue() != 0) {
                // Simular chamada do handler
                // Em sistema real, isso seria feito no contexto do processo
                midlet.print("Signal " + signum + " delivered to process " + pid, stdout);
            }
        }
    }

    private void writeShortWithMMU(int addr, short value) throws Exception {
        int physAddr = translateAddress(addr, true, false);
        if (physAddr >= 0 && physAddr + 1 < memory.length) {
            memory[physAddr] = (byte)(value & 0xFF);
            memory[physAddr + 1] = (byte)((value >> 8) & 0xFF);
        }
    }
    private byte readByteFromPath(String path, int offset) {
        try {
            InputStream is = midlet.getInputStream(path);
            if (is != null) {
                // Corrigir: skip retorna long
                long skipped = is.skip(offset);
                int b = is.read();
                is.close();
                if (b != -1) {
                    return (byte) b;  // byte primitivo
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;  // byte primitivo
    }
    private void prepareElfStack(ELF elf, Vector args) {
        try {
            int sp = elf.registers[REG_SP];
            
            // Escrever strings dos argumentos
            int stringPos = sp - 256;
            Vector argPointers = new Vector();
            
            for (int i = 0; i < args.size(); i++) {
                String arg = (String) args.elementAt(i);
                byte[] argBytes = (arg + "\0").getBytes("UTF-8");
                
                for (int j = 0; j < argBytes.length; j++) {
                    elf.writeByte(stringPos + j, argBytes[j]);
                }
                
                argPointers.addElement(new Integer(stringPos));
                stringPos += argBytes.length;
            }
            
            // Escrever array de ponteiros
            int argvPos = stringPos;
            for (int i = 0; i < argPointers.size(); i++) {
                int ptr = ((Integer) argPointers.elementAt(i)).intValue();
                elf.writeIntLEWithMMU(argvPos + i * 4, ptr);
            }
            // NULL terminator
            elf.writeIntLEWithMMU(argvPos + argPointers.size() * 4, 0);
            
            // Ajustar registradores
            elf.registers[REG_R0] = argPointers.size(); // argc
            elf.registers[REG_R1] = argvPos; // argv
            elf.registers[REG_SP] = argvPos - 16; // Ajustar stack pointer
            
        } catch (Exception e) {
            // Ignorar erros
        }
    }
    
    // Método para criar processo ELF
    public void spawnELF(String pid, ELF elf) {
        Hashtable proc = genprocess("elf", self.id, null);
        proc.put("elf", elf);
        sys.put(pid, proc);
    }

    private String readString(int addr) {
        try {
            if (addr == 0) return null;
            StringBuffer sb = new StringBuffer();
            int offset = 0;
            byte b;
            while ((b = readByte(addr + offset)) != 0 && offset < 256) {
                sb.append((char) (b & 0xFF));
                offset++;
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Método para enviar sinal IRQ
    public void triggerIRQ(String pid) {
        Object elfObj = getobject(pid, "elf");
        if (elfObj instanceof ELF) {
            ELF elf = (ELF) elfObj;
            // Em implementação real, isso setaria uma flag de IRQ pendente
            // Aqui simplificamos chamando diretamente
            try {
                java.lang.reflect.Method method = ELF.class.getDeclaredMethod("handleIRQ");
                method.setAccessible(true);
                method.invoke(elf);
            } catch (Exception e) {
                midlet.print("Failed to trigger IRQ: " + getCatch(e), stdout);
            }
        }
    }

    // Método para acessar registrador considerando modo
    private int getRegister(int reg) {
        // R13 (SP) e R14 (LR) são banked
        if (reg == REG_SP || reg == REG_LR) {
            int mode = getCurrentMode();
            if (mode == MODE_USR || mode == MODE_SYS) {
                return registers[reg];
            } else {
                return getBankedRegister(reg, mode);
            }
        }
        
        // R8-R12 são banked apenas no modo FIQ
        if (reg >= 8 && reg <= 12) {
            if (getCurrentMode() == MODE_FIQ) {
                return getBankedRegister(reg, MODE_FIQ);
            }
        }
        
        return registers[reg];
    }

    private void setRegister(int reg, int value) {
        if (reg == REG_SP || reg == REG_LR) {
            int mode = getCurrentMode();
            if (mode == MODE_USR || mode == MODE_SYS) {
                registers[reg] = value;
            } else {
                setBankedRegister(reg, mode, value);
            }
            return;
        }
        
        if (reg >= 8 && reg <= 12) {
            if (getCurrentMode() == MODE_FIQ) {
                setBankedRegister(reg, MODE_FIQ, value);
                return;
            }
        }
        
        registers[reg] = value;
    }

    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { 
        // conteudo original completo
        if (offset + 3 >= data.length || offset < 0) { return 0; } 
        return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); 
    } 
    
    private short readShortLE(byte[] data, int offset) { 
        // conteudo original completo
        if (offset + 1 >= data.length || offset < 0) { return 0; } 
        return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); 
    }
    
    private void writeIntLE(byte[] data, int offset, int value) { 
        // conteudo original completo
        if (offset + 3 >= data.length || offset < 0) { return; } 
        data[offset] = (byte)(value & 0xFF); 
        data[offset + 1] = (byte)((value >> 8) & 0xFF); 
        data[offset + 2] = (byte)((value >> 16) & 0xFF); 
        data[offset + 3] = (byte)((value >> 24) & 0xFF); 
    }
    
    private int rotateRight(int value, int amount) { 
        // conteudo original completo
        amount &= 31; 
        return (value >>> amount) | (value << (32 - amount)); 
    }
}