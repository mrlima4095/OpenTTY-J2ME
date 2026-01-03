import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import java.util.*;
import java.io.*;

public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    private Hashtable scope, proc;
    private String pid;
    private int id = 1000;
    
    // Memória e registradores
    private byte[] memory;
    private int[] registers;
    private int[] signalHandlers;
    private int pc;
    private boolean running;
    private int stackPointer;
    
    // Registrador de flags CPSR
    private int cpsr;
    
    // File descriptors
    private Hashtable fileDescriptors, socketDescriptors;
    private int nextFd;

    private Hashtable jmpBufs;
    private int nextJmpBufId;

    // Heap management
    private int heapStart, heapEnd;
    private Hashtable allocatedBlocks;
    
    // Mapeamentos de memória
    private Vector memoryMappings;
    
    // Thread management
    private Vector threads;
    private int nextTid;
    
    // Constantes ELF
    private static final int EI_NIDENT = 16;
    private static final int ELFCLASS32 = 1;
    private static final int ELFDATA2LSB = 1;
    private static final int EM_ARM = 40;
    private static final int ET_EXEC = 2;
    private static final int PT_LOAD = 1;
    private static final int PT_DYNAMIC = 2;
    private static final int PT_INTERP = 3;
    private static final int PT_NOTE = 4;
    
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
    
    // Syscalls Linux ARM (EABI)
    private static final int SYS_EXIT = 1;
    private static final int SYS_FORK = 2;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_CREAT = 8;
    private static final int SYS_UNLINK = 10;
    private static final int SYS_EXECVE = 11;
    private static final int SYS_CHDIR = 12;
    private static final int SYS_TIME = 13;
    private static final int SYS_LSEEK = 19;
    private static final int SYS_GETPID = 20;
    private static final int SYS_MKDIR = 39;
    private static final int SYS_RMDIR = 40;
    private static final int SYS_DUP = 41;
    private static final int SYS_DUP2 = 63;
    private static final int SYS_GETPPID = 64;
    private static final int SYS_IOCTL = 54;
    private static final int SYS_KILL = 37;
    private static final int SYS_BRK = 45;
    private static final int SYS_GETTIMEOFDAY = 78;
    private static final int SYS_GETCWD = 183;
    private static final int SYS_GETUID32 = 199;
    private static final int SYS_GETEUID32 = 201;
    private static final int SYS_STAT = 106;
    private static final int SYS_FSTAT = 108;
    private static final int SYS_CLONE = 120;
    private static final int SYS_GETPRIORITY = 140;
    private static final int SYS_SETPRIORITY = 141;
    private static final int SYS_GETDENTS = 217;
    private static final int SYS_SOCKET = 281;
    private static final int SYS_BIND = 282;
    private static final int SYS_CONNECT = 283;
    private static final int SYS_LISTEN = 284;
    private static final int SYS_ACCEPT = 285;
    private static final int SYS_SEND = 289;
    private static final int SYS_RECV = 291;
    private static final int SYS_SHUTDOWN = 293;
    private static final int SYS_SETSOCKOPT = 294;
    private static final int SYS_GETSOCKOPT = 295;
    private static final int SYS_SENDTO = 290;
    private static final int SYS_RECVFROM = 292;
    private static final int SYS_GETSOCKNAME = 286;
    private static final int SYS_GETPEERNAME = 287;
    private static final int SYS_SIGNAL = 48;
    private static final int SYS_SIGACTION = 67;
    private static final int SYS_SIGPROCMASK = 126;
    private static final int SYS_SIGRETURN = 119;
    private static final int SYS_SETJMP = 96;
    private static final int SYS_LONGJMP = 97;
    private static final int SYS_GETTID = 224;
    private static final int SYS_NANOSLEEP = 162;
    private static final int SYS_PIPE = 42;
    private static final int SYS_SELECT = 142;
    private static final int SYS_POLL = 168;
    private static final int SYS_FSYNC = 118;
    private static final int SYS_MMAP = 192;
    private static final int SYS_MUNMAP = 91;
    private static final int SYS_MPROTECT = 125;
    private static final int SYS_MREMAP = 163;
    private static final int SYS_FUTEX = 240;
    private static final int SYS_SCHED_YIELD = 158;
    private static final int SYS_UNAME = 122;
    private static final int SYS_FCNTL = 55;
    private static final int SYS_FTRUNCATE = 93;
    private static final int SYS_TRUNCATE = 92;
    private static final int SYS_GETRLIMIT = 191;
    private static final int SYS_SYSCALL = 0;
    
    // Constantes para socket
    private static final int AF_INET = 2;
    private static final int SOCK_STREAM = 1;
    private static final int SOCK_DGRAM = 2;
    private static final int IPPROTO_TCP = 6;
    private static final int IPPROTO_UDP = 17;

    // Constantes para sinal
    private static final int SIG_DFL = 0;
    private static final int SIG_IGN = 1;
    private static final int SIG_ERR = -1;
    private static final int SIGKILL = 9;
    private static final int SIGTERM = 15;
    private static final int SIGINT = 2;
    private static final int SIGSEGV = 11;
    private static final int SIGPIPE = 13;
    private static final int SIGCHLD = 17;
    private static final int SIGCONT = 18;
    private static final int SIGSTOP = 19;
    private static final int NSIG = 32;

    // Adicionar constantes para flags de ioctl (simplificadas)
    private static final int TCGETS = 0x5401;
    private static final int TCSETS = 0x5402;
    private static final int TIOCGWINSZ = 0x5413;
    private static final int TIOCSWINSZ = 0x5414;
    private static final int FIONREAD = 0x541B;

    // Adicionar constantes para mode de mkdir
    private static final int S_IRWXU = 0700;
    private static final int S_IRUSR = 0400;
    private static final int S_IWUSR = 0200;
    private static final int S_IXUSR = 0100;
    private static final int S_IRWXG = 0070;
    private static final int S_IRGRP = 0040;
    private static final int S_IWGRP = 0020;
    private static final int S_IXGRP = 0010;
    private static final int S_IRWXO = 0007;
    private static final int S_IROTH = 0004;
    private static final int S_IWOTH = 0002;
    private static final int S_IXOTH = 0001;
    private static final int S_IFDIR = 0040000;
    
    // Adicionar constante para SEEK
    private static final int SEEK_SET = 0;
    private static final int SEEK_CUR = 1;
    private static final int SEEK_END = 2;

    // Flags de open
    private static final int O_RDONLY = 0, O_WRONLY = 1, O_RDWR = 2;
    private static final int O_CREAT = 64;
    private static final int O_APPEND = 1024;
    private static final int O_TRUNC = 512;
    private static final int O_DIRECTORY = 0x10000;
    
    // Flags mmap
    private static final int PROT_READ = 1;
    private static final int PROT_WRITE = 2;
    private static final int PROT_EXEC = 4;
    private static final int PROT_NONE = 0;
    private static final int MAP_SHARED = 1;
    private static final int MAP_PRIVATE = 2;
    private static final int MAP_FIXED = 16;
    private static final int MAP_ANONYMOUS = 32;
    
    // Constantes fcntl
    private static final int F_GETFL = 3;
    private static final int F_SETFL = 4;
    private static final int O_NONBLOCK = 2048;
    
    // Cache de instruções para otimização
    private Hashtable instructionCache;
    private int cacheHits, cacheMisses;
    
    // Informações do ELF carregado
    private Hashtable elfInfo;
    
    // Coprocessador (simulado para FPU)
    private float[] fpuRegisters;
    private int fpscr; // FPU Status and Control Register
    
    // Stack de sinais
    private Vector signalStack;
    
    // Futex management
    private Hashtable futexWaiters;
    
    public ELF(OpenTTY midlet, Object stdout, Hashtable scope, int id, String pid, Hashtable proc) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.scope = scope;
        this.proc = proc;
        this.id = id;
        this.pid = pid == null ? midlet.genpid() : pid;
        this.memory = new byte[32 * 1024 * 1024]; // 32MB de memória
        this.registers = new int[16];
        this.fpuRegisters = new float[32]; // S0-S31 (single precision)
        this.cpsr = 0;
        this.fpscr = 0;
        this.running = false;
        this.nextJmpBufId = 1;
        this.stackPointer = memory.length - 1024;
        this.jmpBufs = new Hashtable();
        this.socketDescriptors = new Hashtable();
        this.allocatedBlocks = new Hashtable();
        this.fileDescriptors = new Hashtable();
        this.nextFd = 3; // 0=stdin, 1=stdout, 2=stderr
        this.heapStart = 0x200000; // 2MB - início do heap
        this.heapEnd = heapStart;
        this.instructionCache = new Hashtable();
        this.cacheHits = 0;
        this.cacheMisses = 0;
        this.elfInfo = new Hashtable();
        this.signalStack = new Vector();
        this.futexWaiters = new Hashtable();
        this.memoryMappings = new Vector();
        this.threads = new Vector();
        this.nextTid = 1;

        this.signalHandlers = new int[NSIG];
        for (int i = 0; i < NSIG; i++) { signalHandlers[i] = SIG_DFL; }
        
        // Inicializar file descriptors padrão
        fileDescriptors.put(new Integer(1), stdout); // stdout
        fileDescriptors.put(new Integer(2), stdout); // stderr
        
        // Thread principal
        Hashtable mainThread = new Hashtable();
        mainThread.put("tid", new Integer(nextTid++));
        mainThread.put("pc", new Integer(0));
        mainThread.put("sp", new Integer(stackPointer));
        mainThread.put("registers", registers.clone());
        threads.addElement(mainThread);
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
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { midlet.print("Not a valid ELF file", stdout); return false; }
        if (elfData[4] != ELFCLASS32) { midlet.print("Only 32-bit ELF supported", stdout); return false; }
        if (elfData[5] != ELFDATA2LSB) { midlet.print("Only little-endian ELF supported", stdout); return false; }
        
        int e_type = readShortLE(elfData, 16);
        int e_machine = readShortLE(elfData, 18);
        int e_entry = readIntLE(elfData, 24);
        int e_phoff = readIntLE(elfData, 28);
        int e_shoff = readIntLE(elfData, 32);
        int e_phnum = readShortLE(elfData, 44);
        int e_shnum = readShortLE(elfData, 48);
        int e_phentsize = readShortLE(elfData, 42);
        int e_shentsize = readShortLE(elfData, 46);
        
        if (e_type != ET_EXEC) { midlet.print("Not an executable ELF", stdout); return false; }
        if (e_machine != EM_ARM) { midlet.print("Not an ARM executable", stdout); return false; }
        
        // Armazenar informações do ELF
        elfInfo.put("entry", new Integer(e_entry));
        elfInfo.put("phoff", new Integer(e_phoff));
        elfInfo.put("phnum", new Integer(e_phnum));
        elfInfo.put("shoff", new Integer(e_shoff));
        elfInfo.put("shnum", new Integer(e_shnum));
        
        // Carregar seções primeiro para obter informações de .bss
        Hashtable sectionInfo = loadSections(elfData, e_shoff, e_shnum, e_shentsize);
        
        pc = e_entry;
        registers[REG_SP] = stackPointer;
        registers[REG_LR] = 0xFFFFFFFF;
        
        // Inicializar .bss (zerar memória não inicializada)
        initializeBSS(sectionInfo);
        
        // Carregar segmentos
        for (int i = 0; i < e_phnum; i++) {
            int phdrOffset = e_phoff + i * e_phentsize;
            int p_type = readIntLE(elfData, phdrOffset);
            
            if (p_type == PT_LOAD) {
                int p_offset = readIntLE(elfData, phdrOffset + 4);
                int p_vaddr = readIntLE(elfData, phdrOffset + 8);
                int p_filesz = readIntLE(elfData, phdrOffset + 16);
                int p_memsz = readIntLE(elfData, phdrOffset + 20);
                
                // Carregar dados do arquivo
                for (int j = 0; j < p_filesz && j < memory.length; j++) { 
                    if (p_vaddr + j < memory.length) { 
                        memory[p_vaddr + j] = elfData[p_offset + j]; 
                    } 
                }
                
                // Zerar memória restante (.bss)
                for (int j = p_filesz; j < p_memsz; j++) { 
                    if (p_vaddr + j < memory.length) { 
                        memory[p_vaddr + j] = 0; 
                    } 
                }
            } else if (p_type == PT_DYNAMIC) {
                // Processar dynamic section (para bibliotecas compartilhadas)
                processDynamicSegment(elfData, phdrOffset);
            } else if (p_type == PT_INTERP) {
                // Interpretador (loader dinâmico) - ignorado por enquanto
                int p_offset = readIntLE(elfData, phdrOffset + 4);
                String interp = readString(elfData, p_offset, 256);
                if (midlet.debug) midlet.print("Interpreter: " + interp, stdout);
            }
        }
        
        // Processar símbolos e realocações
        processSymbolsAndRelocations(elfData, sectionInfo);
        
        // Configurar stack para CRT
        setupCRTStack();
        
        return true;
    }
    
    public Hashtable run() {
        running = true;
        Hashtable proc = midlet.genprocess("elf", id, null), ITEM = new Hashtable();
        proc.put("elf", this); 
        midlet.sys.put(pid, proc);
        
        try {
            if (midlet.debug) { 
                midlet.print("=== ELF START DEBUG ===", stdout, id);
                midlet.print("PC start: " + toHex(pc), stdout, id);
                midlet.print("SP: " + toHex(registers[REG_SP]), stdout, id);
                midlet.print("Memory: " + memory.length + " bytes", stdout, id);
            }
            
            int instructionCount = 0;
            while (running && pc < memory.length - 3 && midlet.sys.containsKey(pid)) {
                if (instructionCount++ > 100000) {
                    if (midlet.debug) midlet.print("DEBUG: Stopping after 100000 instructions", stdout, id);
                    break;
                }
                
                // Verificar sinais pendentes
                checkPendingSignals();
                
                // Debug avançado
                if (midlet.debug && instructionCount % 10000 == 0) {
                    midlet.print("DEBUG: PC=" + toHex(pc) + ", R7=" + registers[REG_R7] + 
                                ", Cache hits: " + cacheHits + ", misses: " + cacheMisses, stdout, id);
                }
                
                // Executar instrução com cache
                int instruction = fetchInstruction(pc);
                if (midlet.debug && instructionCount < 10) {
                    midlet.print("DEBUG: Instr at PC " + toHex(pc) + ": " + toHex(instruction), stdout, id);
                }
                pc += 4;
                
                try {
                    executeInstruction(instruction);
                } catch (Exception e) {
                    if (midlet.debug) midlet.print("DEBUG: Exception in executeInstruction: " + e, stdout, id);
                    e.printStackTrace();
                    handleSignal(SIGSEGV);
                    running = false;
                    break;
                }
            }
            
            if (midlet.debug) {
                midlet.print("=== ELF END DEBUG ===", stdout, id);
                midlet.print("Instructions executed: " + instructionCount, stdout, id);
                midlet.print("Cache efficiency: " + (cacheHits * 100 / (cacheHits + cacheMisses)) + "%", stdout, id);
            }
        } 
        catch (Throwable e) { 
            if (midlet.debug) midlet.print("=== ELF CRASH DEBUG ===\nCRASH: " + e.getClass().getName() + ": " + e.getMessage(), stdout, id);
            e.printStackTrace();
            running = false; 
        } 
        finally { 
            if (midlet.debug) midlet.print("=== ELF FINALLY DEBUG ===", stdout, id);
            if (midlet.sys.containsKey(pid)) { midlet.sys.remove(pid); } 
        }

        ITEM.put("status", new Double(0));
        return ITEM;
    }
    
    private int fetchInstruction(int addr) {
        // Verificar cache primeiro
        Integer addrObj = new Integer(addr);
        if (instructionCache.containsKey(addrObj)) {
            cacheHits++;
            return ((Integer)instructionCache.get(addrObj)).intValue();
        }
        
        cacheMisses++;
        int instruction = readIntLE(memory, addr);
        
        // Armazenar no cache (apenas instruções comuns)
        if ((instruction & 0x0E000000) != 0x0A000000) { // Não armazenar branches
            instructionCache.put(addrObj, new Integer(instruction));
        }
        
        return instruction;
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
        if (midlet.debug) {
            midlet.print("[WARN] Unrecognized instruction: " + toHex(instruction) + " at PC: " + toHex(pc-4), stdout);
        }
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

    private Hashtable loadSections(byte[] elfData, int shoff, int shnum, int shentsize) {
        Hashtable sections = new Hashtable();
        
        for (int i = 0; i < shnum; i++) {
            int shdrOffset = shoff + i * shentsize;
            int sh_name = readIntLE(elfData, shdrOffset);
            int sh_type = readIntLE(elfData, shdrOffset + 4);
            int sh_flags = readIntLE(elfData, shdrOffset + 8);
            int sh_addr = readIntLE(elfData, shdrOffset + 12);
            int sh_offset = readIntLE(elfData, shdrOffset + 16);
            int sh_size = readIntLE(elfData, shdrOffset + 20);
            int sh_link = readIntLE(elfData, shdrOffset + 24);
            int sh_info = readIntLE(elfData, shdrOffset + 28);
            int sh_addralign = readIntLE(elfData, shdrOffset + 32);
            int sh_entsize = readIntLE(elfData, shdrOffset + 36);
            
            Hashtable section = new Hashtable();
            section.put("type", new Integer(sh_type));
            section.put("flags", new Integer(sh_flags));
            section.put("addr", new Integer(sh_addr));
            section.put("offset", new Integer(sh_offset));
            section.put("size", new Integer(sh_size));
            section.put("link", new Integer(sh_link));
            section.put("info", new Integer(sh_info));
            section.put("addralign", new Integer(sh_addralign));
            section.put("entsize", new Integer(sh_entsize));
            
            // Ler nome da seção da string table
            if (sh_name != 0 && elfInfo.containsKey(".shstrtab")) {
                int strtabOffset = ((Integer)elfInfo.get(".shstrtab")).intValue();
                String name = readString(elfData, strtabOffset + sh_name, 64);
                sections.put(name, section);
                if (midlet.debug) midlet.print("Section: " + name + " at " + toHex(sh_addr), stdout);
            }
            
            // Armazenar seção de string table
            if (sh_type == 3) { // SHT_STRTAB
                elfInfo.put(".shstrtab", new Integer(sh_offset));
            }
        }
        
        return sections;
    }
    
    private void initializeBSS(Hashtable sections) {
        // Zerar seções .bss e .sbss
        Enumeration keys = sections.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            if (name.equals(".bss") || name.equals(".sbss")) {
                Hashtable section = (Hashtable) sections.get(name);
                int addr = ((Integer)section.get("addr")).intValue();
                int size = ((Integer)section.get("size")).intValue();
                
                // Zerar memória
                for (int i = 0; i < size && addr + i < memory.length; i++) {
                    memory[addr + i] = 0;
                }
                
                if (midlet.debug) midlet.print("Zeroed " + name + " at " + toHex(addr) + " size " + size, stdout);
            }
        }
    }
    
    private void processDynamicSegment(byte[] elfData, int phdrOffset) {
        int p_offset = readIntLE(elfData, phdrOffset + 4);
        int p_vaddr = readIntLE(elfData, phdrOffset + 8);
        int p_filesz = readIntLE(elfData, phdrOffset + 16);
        
        if (midlet.debug) midlet.print("Dynamic segment at " + toHex(p_vaddr), stdout);
        
        // Processar entradas da tabela dinâmica
        for (int offset = 0; offset < p_filesz; offset += 8) {
            int tag = readIntLE(elfData, p_offset + offset);
            int val = readIntLE(elfData, p_offset + offset + 4);
            
            if (tag == 0) break; // DT_NULL
            
            switch (tag) {
                case 1: // DT_NEEDED (biblioteca necessária)
                    String libname = readString(elfData, val, 64);
                    if (midlet.debug) midlet.print("Needs library: " + libname, stdout);
                    break;
                case 5: // DT_STRTAB
                    elfInfo.put("dynstr", new Integer(val));
                    break;
                case 6: // DT_SYMTAB
                    elfInfo.put("dynsym", new Integer(val));
                    break;
            }
        }
    }
    
    private void processSymbolsAndRelocations(byte[] elfData, Hashtable sections) { if (midlet.debug) midlet.print("Symbol processing (simplified)", stdout); }
    
    private void setupCRTStack() {
        // Configurar stack para C Runtime
        // Argumentos: argc, argv[], envp[], auxv[]
        
        int sp = registers[REG_SP];
        
        // Auxiliary vector (simplificado)
        writeIntLE(memory, sp - 4, 0); // AT_NULL
        writeIntLE(memory, sp - 8, 0);
        sp -= 8;
        
        // Environment variables
        Vector envVars = new Vector();
        envVars.addElement("PATH=/bin:/usr/bin");
        envVars.addElement("USER=" + (id == 0 ? "root" : midlet.username));
        envVars.addElement("HOME=/home");
        envVars.addElement("SHELL=/bin/sh");
        envVars.addElement("TERM=vt100");
        
        // Ponteiros para env vars
        int envpStart = sp - (envVars.size() + 1) * 4;
        for (int i = 0; i < envVars.size(); i++) {
            String env = (String) envVars.elementAt(i);
            byte[] envBytes = env.getBytes();
            sp -= envBytes.length + 1;
            for (int j = 0; j < envBytes.length; j++) {
                memory[sp + j] = envBytes[j];
            }
            memory[sp + envBytes.length] = 0;
            writeIntLE(memory, envpStart + i * 4, sp);
        }
        writeIntLE(memory, envpStart + envVars.size() * 4, 0); // NULL terminator
        sp = envpStart;
        
        // Argumentos do programa
        Vector args = new Vector();
        args.addElement("program"); // argv[0]
        
        // Ponteiros para args
        int argvStart = sp - (args.size() + 1) * 4;
        for (int i = 0; i < args.size(); i++) {
            String arg = (String) args.elementAt(i);
            byte[] argBytes = arg.getBytes();
            sp -= argBytes.length + 1;
            for (int j = 0; j < argBytes.length; j++) {
                memory[sp + j] = argBytes[j];
            }
            memory[sp + argBytes.length] = 0;
            writeIntLE(memory, argvStart + i * 4, sp);
        }
        writeIntLE(memory, argvStart + args.size() * 4, 0); // NULL terminator
        sp = argvStart;
        
        // argc
        sp -= 4;
        writeIntLE(memory, sp, args.size());
        
        // Configurar stack pointer
        registers[REG_SP] = sp;
        
        if (midlet.debug) {
            midlet.print("Stack setup: SP=" + toHex(registers[REG_SP]), stdout);
            midlet.print("argc=" + args.size(), stdout);
        }
    }
    
    // Syscalls Handler
    // |
    private void handleSyscall(int number) {
        // Debug
        if (midlet.debug && number != SYS_GETTIMEOFDAY && number != SYS_GETPID) {
            midlet.print("Syscall " + number + " (R7=" + registers[REG_R7] + ")", stdout, id);
        }
        
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
                handleBrk();
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
                
            case SYS_EXECVE:
                handleExecve();
                break;
                
            case SYS_MKDIR:
                handleMkdir();
                break;
                
            case SYS_RMDIR:
                handleRmdir();
                break;
                
            case SYS_STAT:
                handleStat();
                break;
                
            case SYS_FSTAT:
                handleFstat();
                break;
                
            case SYS_IOCTL:
                handleIoctl();
                break;
                
            case SYS_CLONE:
                handleCloneReal();
                break;
                
            case SYS_GETPRIORITY:
                handleGetpriority();
                break;
                
            case SYS_SETPRIORITY:
                handleSetpriority();
                break;
                
            case SYS_LSEEK:
                handleLseek();
                break;
                
            case SYS_GETDENTS:
                handleGetdents();
                break;
                
            case SYS_DUP:
                handleDup();
                break;
                
            case SYS_DUP2:
                handleDup2();
                break;
                
            case SYS_UNLINK:
                handleUnlink();
                break;
                
            case SYS_MMAP:
                handleMmap();
                break;
                
            case SYS_MUNMAP:
                handleMunmap();
                break;
                
            case SYS_MPROTECT:
                handleMprotect();
                break;
                
            case SYS_MREMAP:
                handleMremap();
                break;
                
            case SYS_FUTEX:
                handleFutex();
                break;
                
            case SYS_SCHED_YIELD:
                handleSchedYield();
                break;
                
            case SYS_UNAME:
                handleUname();
                break;
                
            case SYS_FCNTL:
                handleFcntl();
                break;
                
            case SYS_FTRUNCATE:
                handleFtruncate();
                break;
                
            case SYS_TRUNCATE:
                handleTruncate();
                break;
                
            case SYS_GETRLIMIT:
                handleGetrlimit();
                break;
                
            case SYS_SYSCALL:
                // syscall() - chamar syscall por número
                int syscallNum = registers[REG_R0];
                registers[REG_R7] = syscallNum;
                registers[REG_R0] = registers[REG_R1];
                registers[REG_R1] = registers[REG_R2];
                registers[REG_R2] = registers[REG_R3];
                handleSyscall(syscallNum);
                break;
                
            case SYS_SOCKET:
                handleSocket();
                break;
            case SYS_CONNECT:
                handleConnect();
                break;
            case SYS_SEND:
                handleSend();
                break;
            case SYS_RECV:
                handleRecv();
                break;
            case SYS_BIND:
                handleBind();
                break;
            case SYS_LISTEN:
                handleListen();
                break;
            case SYS_ACCEPT:
                handleAccept();
                break;
            case SYS_SHUTDOWN:
                handleShutdown();
                break;
            case SYS_SETSOCKOPT:
                handleSetsockopt();
                break;
            case SYS_GETSOCKOPT:
                handleGetsockopt();
                break;
            case SYS_SENDTO:
                handleSendto();
                break;
            case SYS_RECVFROM:
                handleRecvfrom();
                break;
            case SYS_GETSOCKNAME:
                handleGetsockname();
                break;
            case SYS_GETPEERNAME:
                handleGetpeername();
                break;
            case SYS_SIGNAL:
                handleSignal();
                break;
            case SYS_SIGACTION:
                handleSigaction();
                break;
            case SYS_SETJMP:
                handleSetjmp();
                break;
            case SYS_LONGJMP:
                handleLongjmp();
                break;
            case SYS_GETTID:
                handleGettid();
                break;
            case SYS_NANOSLEEP:
                handleNanosleep();
                break;
            case SYS_PIPE:
                handlePipe();
                break;
            case SYS_SELECT:
                handleSelect();
                break;
            case SYS_POLL:
                handlePoll();
                break;
            case SYS_FSYNC:
                handleFsync();
                break;
            default:
                registers[REG_R0] = -38; // ENOSYS - Syscall não implementada
                if (midlet.debug) midlet.print("Unimplemented syscall: " + number, stdout, id);
                break;
        }
    }
    // |
    // | Kernel
    // | (Process)
    private void handleFork() { registers[REG_R0] = -1; }
    private void handleClone() {
        int flags = registers[REG_R0];
        int child_stack = registers[REG_R1];
        int parent_tid = registers[REG_R2];
        int child_tid = registers[REG_R3];
        int tls = registers[REG_R4];
        
        // Flags importantes
        boolean cloneThread = (flags & 0x00010000) != 0; // CLONE_THREAD
        boolean cloneVm = (flags & 0x00000100) != 0;    // CLONE_VM
        boolean cloneFiles = (flags & 0x00000400) != 0; // CLONE_FILES
        
        if (cloneThread) {
            // Criar nova thread
            Hashtable newThread = new Hashtable();
            newThread.put("tid", new Integer(nextTid++));
            newThread.put("pc", new Integer(pc));
            newThread.put("sp", new Integer(child_stack != 0 ? child_stack : registers[REG_SP] - 4096));
            newThread.put("registers", registers.clone());
            
            threads.addElement(newThread);
            registers[REG_R0] = ((Integer)newThread.get("tid")).intValue();
        } else {
            // Criar novo processo (fork-like)
            registers[REG_R0] = -38; // ENOSYS por enquanto
        }
    }
    
    private void handleExecve() {
        int pathAddr = registers[REG_R0], argvAddr = registers[REG_R1], envpAddr = registers[REG_R2];
        
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();
        
        // Construir argumentos (similar ao io.popen do Lua)
        Vector argsVec = new Vector();
        if (argvAddr != 0) {
            int argPtr = readIntLE(memory, argvAddr);
            int argIndex = 0;
            
            while (argPtr != 0 && argIndex < 64) {
                StringBuffer argBuf = new StringBuffer();
                int j = 0;
                while (argPtr + j < memory.length && memory[argPtr + j] != 0 && j < 256) {
                    argBuf.append((char)(memory[argPtr + j] & 0xFF));
                    j++;
                }
                argsVec.addElement(argBuf.toString());
                
                argvAddr += 4;
                argPtr = readIntLE(memory, argvAddr);
                argIndex++;
            }
        }
        
        // Construir string de argumentos
        StringBuffer argsStr = new StringBuffer();
        for (i = 1; i < argsVec.size(); i++) {
            if (i > 1) argsStr.append(" ");
            argsStr.append((String)argsVec.elementAt(i));
        }
        
        try {
            // Verificar se é ELF ou Lua
            InputStream is = midlet.getInputStream(path);
            if (is == null) { registers[REG_R0] = -2; return; }
            
            byte[] header = new byte[4];
            int bytesRead = is.read(header);
            is.close();
            
            boolean isElf = (bytesRead == 4 && header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F');
            
            if (isElf) {
                // Executar ELF (similar ao fork+exec)
                InputStream elfStream = midlet.getInputStream(path);
                ELF elf = new ELF(midlet, stdout, scope, id, null, null);
                
                if (elf.load(elfStream)) {
                    // Fechar file descriptors (exceto 0,1,2)
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
                                } catch (Exception e) {}
                            }
                        }
                    }
                    fileDescriptors.clear();
                    
                    // Reabrir stdin/stdout/stderr
                    fileDescriptors.put(new Integer(1), stdout);
                    fileDescriptors.put(new Integer(2), stdout);
                    
                    // Executar novo programa
                    Hashtable proc = midlet.genprocess("elf", id, null);
                    proc.put("elf", elf);
                    midlet.sys.put(pid, proc);
                    
                    // Configurar stack para argumentos
                    // (simplificado - na prática precisaria configurar argc/argv na stack)
                    
                    registers[REG_R0] = 0; // Sucesso
                } else {
                    registers[REG_R0] = -8; // ENOEXEC
                }
            } else {
                // Executar Lua
                String code = midlet.read(path);
                if (code == null || code.equals("")) { registers[REG_R0] = -2; return; }
                
                // Fechar file descriptors (exceto 0,1,2)
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
                            } catch (Exception e) {}
                        }
                    }
                }
                fileDescriptors.clear();
                
                // Reabrir stdin/stdout/stderr
                fileDescriptors.put(new Integer(1), stdout);
                fileDescriptors.put(new Integer(2), stdout);
                
                // Executar Lua (similar ao io.popen)
                Hashtable arg = new Hashtable();
                arg.put(new Double(0), path);
                String[] argList = midlet.splitArgs(argsStr.toString());
                for (i = 0; i < argList.length; i++) { arg.put(new Double(i + 1), argList[i]); }
                
                Lua lua = new Lua(midlet, id, pid, null, stdout, scope);
                lua.run(path, code, arg);
                
                registers[REG_R0] = 0;
            }
        } catch (Exception e) { registers[REG_R0] = -1; }
    }
    private void handleGetpriority() { int which = registers[REG_R0], who = registers[REG_R1]; if (who == 0) { Object priorityObj = proc.get("priority"); if (priorityObj instanceof Integer) { registers[REG_R0] = ((Integer) priorityObj).intValue(); } else { registers[REG_R0] = 20; } } else { registers[REG_R0] = -22; } }
    private void handleSetpriority() { int which = registers[REG_R0], who = registers[REG_R1], prio = registers[REG_R2]; if (who == 0) { proc.put("priority", new Integer(prio)); registers[REG_R0] = 0; } else { registers[REG_R0] = -22; } }
    // |
    private void handleSignal() {
        int signum = registers[REG_R0], handler = registers[REG_R1];
        
        if (signum <= 0 || signum >= NSIG) { registers[REG_R0] = SIG_ERR; return; }
        
        int oldHandler = signalHandlers[signum];
        signalHandlers[signum] = handler;
        
        registers[REG_R0] = oldHandler;
    }
    private void handleSignal(int sig) {
        if (sig <= 0 || sig >= NSIG) return;
        
        int handler = signalHandlers[sig];
        
        if (handler == SIG_DFL) {
            // Comportamento padrão
            switch (sig) {
                case SIGSEGV:
                    running = false;
                    midlet.print("Segmentation fault", stdout, id);
                    break;
                case SIGINT:
                    running = false;
                    break;
            }
        } else if (handler == SIG_IGN) {
            // Ignorar sinal
            return;
        } else if (handler != 0) {
            // Chamar handler de sinal
            pushSignalFrame(sig);
            pc = handler;
        }
    }
    private void handleSigaction() {
        int signum = registers[REG_R0], actPtr = registers[REG_R1], oldactPtr = registers[REG_R2];

        if (signum <= 0 || signum >= NSIG) { registers[REG_R0] = -22; return; }
 
        // Salvar o antigo handler se oldactPtr não for nulo
        if (oldactPtr != 0 && oldactPtr + 12 <= memory.length) {
            writeIntLE(memory, oldactPtr, signalHandlers[signum]);
            writeIntLE(memory, oldactPtr + 4, 0); // sa_mask
            writeIntLE(memory, oldactPtr + 8, 0); // sa_flags
        }

        if (actPtr != 0 && actPtr + 4 <= memory.length) {
            int newHandler = readIntLE(memory, actPtr);
            signalHandlers[signum] = newHandler;
        }
        
        registers[REG_R0] = 0;
    }
    private void handleKill() {
        int pid = registers[REG_R0], sig = registers[REG_R1];
        
        String targetPid = String.valueOf(pid);
        
        if (!midlet.sys.containsKey(targetPid)) { registers[REG_R0] = -3; return; }
        if (this.id != 0 && !targetPid.equals(this.pid)) { registers[REG_R0] = -1; return; }
        
        Object procObj = midlet.sys.get(targetPid);
        
        // Para sinais de terminação
        if (sig == SIGKILL || sig == SIGTERM) {
            if (procObj instanceof Hashtable) {
                Hashtable proc = (Hashtable) procObj;
                if (proc.containsKey("elf")) {
                    ELF elf = (ELF) proc.get("elf");
                    elf.kill();
                }
            }
            midlet.sys.remove(targetPid);
            registers[REG_R0] = 0;
            return;
        }
        
        // Para sinais que podem ser ignorados ou manipulados
        if (sig == SIGINT || sig == SIGCONT || sig == SIGSTOP) {
            // Enviar sinal para o processo (simulado)
            if (procObj instanceof Hashtable) {
                Hashtable proc = (Hashtable) procObj;
                if (proc.containsKey("elf")) {
                    ELF elf = (ELF) proc.get("elf");
                    // Em uma implementação real, armazenaríamos o sinal pendente
                    // e o processaríamos na próxima syscall ou no retorno de syscall
                }
            }
            registers[REG_R0] = 0;
            return;
        }
        
        if (sig == 0) { registers[REG_R0] = 0; }
        else { registers[REG_R0] = -22; }
    }
    // |
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

        keys = socketDescriptors.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Hashtable socketInfo = (Hashtable) socketDescriptors.get(key);
            if (socketInfo.containsKey("connection")) { try { ((StreamConnection) socketInfo.get("connection")).close(); } catch (Exception e) { } }
            if (socketInfo.containsKey("server")) { try { ((StreamConnectionNotifier) socketInfo.get("server")).close(); } catch (Exception e) { } }
        }
        
        fileDescriptors.clear(); socketDescriptors.clear();
        allocatedBlocks.clear(); jmpBufs.clear();
    }
    // |
    private void handleGetpid() { try { int pidValue = Integer.parseInt(this.pid); registers[REG_R0] = pidValue; } catch (NumberFormatException e) { registers[REG_R0] = 1; } }
    private void handleGetppid() { registers[REG_R0] = 1; }
    private void handleGetuid() { registers[REG_R0] = id; }
    private void handleGettid() { registers[REG_R0] = id; }
    private void handleGetgid() { registers[REG_R0] = 1000; }
    // | (Threading)
    private void handleNanosleep() {
        int reqPtr = registers[REG_R0], remPtr = registers[REG_R1];
        
        if (reqPtr == 0 || reqPtr + 8 > memory.length) { registers[REG_R0] = -14; return; }
        
        int sec = readIntLE(memory, reqPtr), nsec = readIntLE(memory, reqPtr + 4);
        
        long sleepMillis = sec * 1000 + nsec / 1000000;
        
        if (sleepMillis > 0) {
            try { Thread.sleep(sleepMillis); }
            catch (InterruptedException e) { registers[REG_R0] = -4; return; }
        }
        
        registers[REG_R0] = 0;
    }
    // | (Users)

    // | (Memory)
    private void handleMmap() {
        int addr = registers[REG_R0];
        int length = registers[REG_R1];
        int prot = registers[REG_R2];
        int flags = registers[REG_R3];
        int fd = registers[REG_R4];
        int offset = registers[REG_R5];
        
        if (length <= 0) {
            registers[REG_R0] = -22; // EINVAL
            return;
        }
        
        // Arredondar para múltiplo de página (4096)
        length = (length + 4095) & ~4095;
        
        // Se addr é 0, escolher automaticamente
        if (addr == 0) {
            // Encontrar região livre
            addr = findFreeMemoryRegion(length);
            if (addr == 0) {
                registers[REG_R0] = -12; // ENOMEM
                return;
            }
        }
        
        // Verificar se a região está livre
        if (!isMemoryRegionFree(addr, length)) {
            registers[REG_R0] = -12; // ENOMEM
            return;
        }
        
        // Mapear memória
        Hashtable mapping = new Hashtable();
        mapping.put("addr", new Integer(addr));
        mapping.put("length", new Integer(length));
        mapping.put("prot", new Integer(prot));
        mapping.put("flags", new Integer(flags));
        mapping.put("fd", new Integer(fd));
        mapping.put("offset", new Integer(offset));
        
        memoryMappings.addElement(mapping);
        
        // Se for MAP_ANONYMOUS, zerar a memória
        if ((flags & MAP_ANONYMOUS) != 0) {
            for (int i = 0; i < length && addr + i < memory.length; i++) {
                memory[addr + i] = 0;
            }
        }
        
        registers[REG_R0] = addr;
        
        if (midlet.debug) { midlet.print("mmap: addr=" + toHex(addr) + " length=" + length + " prot=" + prot + " flags=" + flags, stdout, id); }
    }
    private void handleMunmap() {
        int addr = registers[REG_R0];
        int length = registers[REG_R1];
        
        // Encontrar e remover mapeamento
        for (int i = 0; i < memoryMappings.size(); i++) {
            Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
            int maddr = ((Integer)mapping.get("addr")).intValue();
            int mlen = ((Integer)mapping.get("length")).intValue();
            
            if (addr >= maddr && addr < maddr + mlen) {
                // Zerar memória (opcional)
                for (int j = 0; j < mlen && maddr + j < memory.length; j++) {
                    memory[maddr + j] = 0;
                }
                
                memoryMappings.removeElementAt(i);
                registers[REG_R0] = 0;
                return;
            }
        }
        
        registers[REG_R0] = -22; // EINVAL
    }
    
    private void handleMprotect() {
        int addr = registers[REG_R0];
        int len = registers[REG_R1];
        int prot = registers[REG_R2];
        
        // Verificar mapeamento
        for (int i = 0; i < memoryMappings.size(); i++) {
            Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
            int maddr = ((Integer)mapping.get("addr")).intValue();
            int mlen = ((Integer)mapping.get("length")).intValue();
            
            if (addr >= maddr && addr < maddr + mlen) {
                // Atualizar proteção
                mapping.put("prot", new Integer(prot));
                registers[REG_R0] = 0;
                return;
            }
        }
        
        registers[REG_R0] = -22; // EINVAL
    }
    
    private void handleMremap() {
        int old_addr = registers[REG_R0];
        int old_size = registers[REG_R1];
        int new_size = registers[REG_R2];
        int flags = registers[REG_R3];
        int new_addr = registers[REG_R4];
        
        // Implementação simplificada
        if (new_addr == 0) {
            // Tentar expandir no lugar
            for (int i = 0; i < memoryMappings.size(); i++) {
                Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
                int maddr = ((Integer)mapping.get("addr")).intValue();
                int mlen = ((Integer)mapping.get("length")).intValue();
                
                if (maddr == old_addr && mlen == old_size) {
                    // Verificar se há espaço para expandir
                    if (isMemoryRegionFree(maddr + mlen, new_size - old_size)) {
                        mapping.put("length", new Integer(new_size));
                        registers[REG_R0] = maddr;
                        return;
                    }
                }
            }
        }
        
        // Falhou, retornar erro
        registers[REG_R0] = -12; // ENOMEM
    }
    private void handleBrk() {
        int newBrk = registers[REG_R0];
        if (newBrk == 0) { 
            registers[REG_R0] = heapEnd; 
            return; 
        }
        
        if (newBrk < heapStart) { 
            registers[REG_R0] = -1; 
            return; 
        }
        
        // Arredondar para página
        newBrk = (newBrk + 4095) & ~4095;
        
        if (newBrk > memory.length) {
            registers[REG_R0] = -12; // ENOMEM
            return;
        }
        
        // Verificar se estamos liberando memória
        if (newBrk < heapEnd) {
            // Liberar blocos alocados que estão além do novo break
            Vector keysToRemove = new Vector();
            Enumeration keys = allocatedBlocks.keys();
            while (keys.hasMoreElements()) {
                Integer addr = (Integer) keys.nextElement();
                Integer size = (Integer) allocatedBlocks.get(addr);
                if (addr.intValue() + size.intValue() > newBrk) { 
                    keysToRemove.addElement(addr); 
                }
            }
            
            for (int i = 0; i < keysToRemove.size(); i++) { 
                allocatedBlocks.remove(keysToRemove.elementAt(i)); 
            }
        }
        
        heapEnd = newBrk;
        registers[REG_R0] = heapEnd;
    }
    // |
    private void handleSetjmp() {
        int jmpBufPtr = registers[REG_R0];
        if (jmpBufPtr + 48 > memory.length) { registers[REG_R0] = -14; return; }
        
        // Salvar registradores no jmp_buf
        for (int i = 0; i < 16; i++) { writeIntLE(memory, jmpBufPtr + i * 4, registers[i]); }
        
        // Salvar PC atual (é o endereço de retorno de setjmp)
        writeIntLE(memory, jmpBufPtr + 64, pc);
        
        int jmpBufId = nextJmpBufId++;
        jmpBufs.put(new Integer(jmpBufId), new Integer(jmpBufPtr));
        
        registers[REG_R0] = 0; // Primeira chamada retorna 0
    }
    private void handleLongjmp() {
        int jmpBufPtr = registers[REG_R0], val = registers[REG_R1];
        if (jmpBufPtr + 48 > memory.length) { running = false; return; }
        
        // Restaurar registradores
        for (int i = 0; i < 16; i++) { registers[i] = readIntLE(memory, jmpBufPtr + i * 4); }
        
        // Restaurar PC
        pc = readIntLE(memory, jmpBufPtr + 64);
        
        // Retornar valor não-zero
        registers[REG_R0] = (val == 0) ? 1 : val;
    }
    // |
    private void handleIoctl() {
        int fd = registers[REG_R0];
        int request = registers[REG_R1];
        int argp = registers[REG_R2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) {
            registers[REG_R0] = -9; // EBADF
            return;
        }
        
        switch (request) {
            case TCGETS:
            case TIOCGWINSZ:
                // Retornar estrutura terminal (simplificada)
                if (argp >= 0 && argp + 8 < memory.length) {
                    // Preencher com valores padrão
                    for (int i = 0; i < 8; i++) {
                        memory[argp + i] = 0;
                    }
                    // 80x25 terminal
                    memory[argp] = 80; // colunas
                    memory[argp + 2] = 25; // linhas
                }
                registers[REG_R0] = 0;
                break;
                
            case TCSETS:
            case TIOCSWINSZ:
                // Ignorar - terminal não configurável
                registers[REG_R0] = 0;
                break;
                
            case FIONREAD:
                // Retornar bytes disponíveis para leitura
                int bytesAvailable = 0;
                if (fd == 0) {
                    // stdin - sempre 0 por enquanto
                    bytesAvailable = 0;
                } else if (fileDescriptors.containsKey(fdKey)) {
                    Object stream = fileDescriptors.get(fdKey);
                    if (stream instanceof InputStream) {
                        try {
                            bytesAvailable = ((InputStream) stream).available();
                        } catch (Exception e) {
                            bytesAvailable = 0;
                        }
                    }
                }
                if (argp >= 0 && argp + 4 < memory.length) {
                    writeIntLE(memory, argp, bytesAvailable);
                }
                registers[REG_R0] = 0;
                break;
                
            default:
                // IOCTL não suportado
                registers[REG_R0] = -25; // ENOTTY
                break;
        }
    }
    // | (Time)
    private void handleTime() { long currentTime = System.currentTimeMillis() / 1000; registers[REG_R0] = (int) currentTime; int timePtr = registers[REG_R1]; if (timePtr != 0 && timePtr >= 0 && timePtr + 3 < memory.length) { writeIntLE(memory, timePtr, (int) currentTime); } }
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
    // |
    // | File System
    // | (Directories)
    private void handleMkdir() {
        int pathAddr = registers[REG_R0], mode = registers[REG_R1];

        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();

        if (path.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ_WRITE);
                if (conn.exists()) {
                    conn.close();
                    registers[REG_R0] = -17; // EEXIST
                    return;
                }
                conn.mkdir(); conn.close();
                registers[REG_R0] = 0; // Sucesso
            } catch (Exception e) { registers[REG_R0] = -1; }
        } else { registers[REG_R0] = -38; return; }
    }
    private void handleRmdir() {
        int pathAddr = registers[REG_R0];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();
        
        if (path.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ_WRITE);
                if (!conn.exists()) { conn.close(); registers[REG_R0] = -2; return; }
                if (!conn.isDirectory()) { conn.close(); registers[REG_R0] = -20; return; }

                Enumeration list = conn.list();
                if (list != null && list.hasMoreElements()) {
                    conn.close();
                    registers[REG_R0] = -39; 
                    return;
                }
                
                conn.delete(); conn.close();
                registers[REG_R0] = 0; // Sucesso
            } catch (Exception e) { registers[REG_R0] = -1; }
        } else { registers[REG_R0] = -38; }
    }
    private void handleGetcwd() {
        int buf = registers[REG_R0], size = registers[REG_R1];
        
        String cwd = (String) scope.get("PWD");
        if (cwd == null) { cwd = "/home/"; }
        
        byte[] cwdBytes = cwd.getBytes();
        int len = Math.min(cwdBytes.length, size - 1);
        
        for (int i = 0; i < len && buf + i < memory.length; i++) { memory[buf + i] = cwdBytes[i]; }
        
        if (buf + len < memory.length) { memory[buf + len] = 0; }
        
        registers[REG_R0] = buf;
    }
    private void handleChdir() {
        int pathAddr = registers[REG_R0];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();
        
        if (path.equals("") || path.equals(".")) { registers[REG_R0] = 0; return; }
        
        String fullPath = path;
        if (!path.startsWith("/")) {
            String pwd = (String) scope.get("PWD");
            if (pwd == null) { pwd = "/home/"; }
            fullPath = pwd + (pwd.endsWith("/") ? "" : "/") + path;
        }
        
        if (!fullPath.endsWith("/")) { fullPath = fullPath + "/"; }
        
        boolean dirExists = false;
        
        if (fullPath.equals("/home/")) { dirExists = true; }
        else if (fullPath.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + fullPath.substring(5), Connector.READ);
                dirExists = conn.exists() && conn.isDirectory();
                conn.close();
            } catch (Exception e) { dirExists = false; }
        } else if (midlet.fs.containsKey(fullPath)) { dirExists = true; }
        
        if (dirExists) { scope.put("PWD", fullPath); registers[REG_R0] = 0; }
        else { registers[REG_R0] = -2; }
    }
    private void handleGetdents() {
        int fd = registers[REG_R0], dirp = registers[REG_R1], count = registers[REG_R2];
        
        if (dirp < 0 || dirp >= memory.length) { registers[REG_R0] = -1; return; }
        
        Integer fdKey = new Integer(fd);
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) { registers[REG_R0] = -9; return; }
        
        // Obter caminho do diretório a partir do file descriptor
        String dirPath = null;
        if (fileDescriptors.containsKey(fdKey)) {
            Object obj = fileDescriptors.get(fdKey);
            if (obj instanceof String) { dirPath = (String) obj; }
            else if (obj instanceof StringBuffer) {
                // Verificar se há caminho associado
                String pathKey = fd + ":path";
                if (fileDescriptors.containsKey(pathKey)) { dirPath = (String) fileDescriptors.get(pathKey); }
            }
        }
        
        if (dirPath == null) { registers[REG_R0] = -20; return; }
        
        // Normalizar caminho (garantir que termina com /)
        String pwd = midlet.joinpath(dirPath, scope);
        if (!pwd.endsWith("/")) { pwd = pwd + "/"; }
        
        // Coletar arquivos em um Vector
        Vector fileList = new Vector();
        
        try {
            if (pwd.equals("/tmp/")) { for (Enumeration files = midlet.tmp.keys(); files.hasMoreElements();) { fileList.addElement((String) files.nextElement()); } }
            else if (pwd.equals("/mnt/")) { for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { fileList.addElement((String) roots.nextElement()); } }
            else if (pwd.startsWith("/mnt/")) {
                FileConnection CONN = (FileConnection) Connector.open("file:///" + pwd.substring(5), Connector.READ);
                for (Enumeration files = CONN.list(); files.hasMoreElements();) { fileList.addElement((String) files.nextElement()); }
                CONN.close();
            }
            else if (pwd.equals("/bin/") || pwd.equals("/etc/") || pwd.equals("/lib/")) {
                String content = midlet.loadRMS("OpenRMS", pwd.equals("/bin/") ? 3 : pwd.equals("/etc/") ? 5 : 4);
                int i = 0;

                while (true) {
                    int start = content.indexOf("[\1BEGIN:", i);
                    if (start == -1) { break; }

                    int end = content.indexOf("\1]", start);
                    if (end == -1) { break; }

                    fileList.addElement(content.substring(start + "[\1BEGIN:".length(), end));

                    i = content.indexOf("[\1END\1]", end);
                    if (i == -1) { break; }

                    i += "[\1END\1]".length();
                }
            }
            else if (pwd.equals("/home/")) { String[] files = RecordStore.listRecordStores(); if (files != null) { for (int i = 0; i < files.length; i++) { fileList.addElement(files[i]); } } }
            
            if (midlet.fs.containsKey(pwd)) {
                Vector struct = (Vector) midlet.fs.get(pwd);
                for (int i = 0; i < struct.size(); i++) { fileList.addElement(struct.elementAt(i)); }
            }
        } catch (Exception e) { registers[REG_R0] = -1; return; }
        
        // Estrutura linux_dirent simplificada
        // d_ino (4 bytes), d_off (4 bytes), d_reclen (2 bytes), d_name (variável)
        int offset = 0, written = 0;
        
        for (int i = 0; i < fileList.size(); i++) {
            String fileName = (String) fileList.elementAt(i);
            byte[] nameBytes = fileName.getBytes();
            int nameLen = nameBytes.length;
            
            // Tamanho do registro: 4 + 4 + 2 + nameLen + 1 (null terminator)
            int reclen = 10 + nameLen + 1;
            
            // Verificar se cabe no buffer
            if (offset + reclen > count || dirp + offset + reclen > memory.length) { break; }
            
            
            writeIntLE(memory, dirp + offset, i + 1); // d_ino (inode number - simplificado)
            writeIntLE(memory, dirp + offset + 4, offset + reclen); // d_off (offset - simplificado) 
            writeShortLE(memory, dirp + offset + 8, (short)reclen); // d_reclen
            
            // d_name
            for (int j = 0; j < nameLen; j++) { memory[dirp + offset + 10 + j] = nameBytes[j]; }
            
            // Null terminator
            memory[dirp + offset + 10 + nameLen] = 0; offset += reclen; written++;
        }
        
        if (written == 0) { registers[REG_R0] = 0; }
        else { registers[REG_R0] = offset; }
    }
    private void handleDup() {
        int oldfd = registers[REG_R0];
        Integer oldKey = new Integer(oldfd);
        
        if (!fileDescriptors.containsKey(oldKey) && oldfd != 0 && oldfd != 1 && oldfd != 2) { registers[REG_R0] = -9; return; }
        
        // Encontrar novo fd
        int newfd = nextFd++;
        while (fileDescriptors.containsKey(new Integer(newfd))) { newfd++; }

        if (oldfd == 0 || oldfd == 1 || oldfd == 2) { fileDescriptors.put(new Integer(newfd), (oldfd == 1 || oldfd == 2) ? stdout : null); }
        else { fileDescriptors.put(new Integer(newfd), fileDescriptors.get(oldKey)); }
        
        registers[REG_R0] = newfd;
    }
    private void handleDup2() {
        int oldfd = registers[REG_R0], newfd = registers[REG_R1];
        
        Integer oldKey = new Integer(oldfd);
        
        if (!fileDescriptors.containsKey(oldKey) && oldfd != 0 && oldfd != 1 && oldfd != 2) { registers[REG_R0] = -9; return; }
        
        // Fechar newfd se estiver aberto
        Integer newKey = new Integer(newfd);
        if (fileDescriptors.containsKey(newKey)) {
            Object stream = fileDescriptors.get(newKey);
            try {
                if (stream instanceof InputStream) { ((InputStream) stream).close(); }
                else if (stream instanceof OutputStream) { ((OutputStream) stream).close(); }
            } catch (Exception e) { }
            fileDescriptors.remove(newKey);
        }
        
        // Duplicar
        if (oldfd == 0 || oldfd == 1 || oldfd == 2) { fileDescriptors.put(newKey, (oldfd == 1 || oldfd == 2) ? stdout : null); }
        else { fileDescriptors.put(newKey, fileDescriptors.get(oldKey)); }
        
        registers[REG_R0] = newfd;
    }
    // | (Operations)
    private void handleCreat() {
        // creat(path, mode) é equivalente a open(path, O_CREAT | O_WRONLY | O_TRUNC, mode)
        int pathAddr = registers[REG_R0], mode = registers[REG_R1];
        
        registers[REG_R1] = O_CREAT | O_WRONLY | O_TRUNC;
        registers[REG_R2] = mode;
        
        handleOpen();
    }
    private void handleOpen() {
        int pathAddr = registers[REG_R0], flags = registers[REG_R1], mode = registers[REG_R2];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }
        
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();
        
        try {
            boolean forReading = (flags & O_RDONLY) == O_RDONLY || (flags & O_RDWR) == O_RDWR, 
                    forWriting = (flags & O_WRONLY) == O_WRONLY || (flags & O_RDWR) == O_RDWR, 
                    create = (flags & O_CREAT) != 0, append = (flags & O_APPEND) != 0, 
                    truncate = (flags & O_TRUNC) != 0, isDirectory = (flags & O_DIRECTORY) != 0;

            String fullPath = midlet.joinpath(path, scope);
            
            // Se for diretório, tratar diferente
            if (isDirectory) {
                boolean isDir = false;
                
                if (fullPath.equals("/") || fullPath.equals("/home/") || fullPath.equals("/tmp/") || fullPath.equals("/bin/") || fullPath.equals("/etc/") || fullPath.equals("/lib/")) { isDir = true; } 
                else if (fullPath.startsWith("/mnt/")) {
                    try {
                        FileConnection conn = (FileConnection) Connector.open("file:///" + fullPath.substring(5), Connector.READ);
                        isDir = conn.exists() && conn.isDirectory();
                        conn.close();
                    } catch (Exception e) { isDir = false; }
                }
                else if (midlet.fs.containsKey(fullPath)) { isDir = true; }
                
                if (isDir) {
                    Integer fd = new Integer(nextFd++);
                    fileDescriptors.put(fd, fullPath); // Armazenar caminho como String
                    registers[REG_R0] = fd.intValue();
                } 
                else { registers[REG_R0] = -20; }
                return;
            }
            
            // Resto do código para arquivos...
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
                    } else { registers[REG_R0] = -1; }
                } else { registers[REG_R0] = -2; }
            } else if (forWriting) {
                // Para escrita, usamos um ByteArrayOutputStream temporário
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                // Se for append, carregar conteúdo existente
                if (append && !truncate) {
                    InputStream existing = midlet.getInputStream(fullPath);
                    if (existing != null) {
                        int b;
                        while ((b = existing.read()) != -1) { baos.write(b); }
                        existing.close();
                    }
                }
                
                Integer fd = new Integer(nextFd++);
                fileDescriptors.put(fd, baos);
                registers[REG_R0] = fd.intValue();
                
                // Guardar o caminho para uso no close/flush
                fileDescriptors.put(fd + ":path", fullPath);
            } else { registers[REG_R0] = -1; }
        } catch (Exception e) { registers[REG_R0] = -1; }
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
            } catch (Exception e) { registers[REG_R0] = -1; }
        } else { registers[REG_R0] = -1; }
    }
    private void handleUnlink() {
        int pathAddr = registers[REG_R0];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_R0] = -1; return; }
        
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) {
            pathBuf.append((char)(memory[pathAddr + i] & 0xFF));
            i++;
        }
        String path = pathBuf.toString();

        int result = midlet.deleteFile(path, id);
        
        // Converter código de retorno do OpenTTY para errno
        switch (result) {
            case 0:  registers[REG_R0] = 0; break; // Sucesso
            case 2:  registers[REG_R0] = -22; break; // EINVAL
            case 5:  registers[REG_R0] = -2; break; // ENOENT
            case 13: registers[REG_R0] = -13; break; // EACCES
            case 127: registers[REG_R0] = -2; break; // ENOENT
            default: registers[REG_R0] = -1; break; // EPERM
        }
    }
    // |
    private void handleRead() {
        int fd = registers[REG_R0], buf = registers[REG_R1], count = registers[REG_R2];
        if (count <= 0 || buf < 0 || buf >= memory.length) { registers[REG_R0] = -1; return; }
        
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
                } catch (Exception e) { registers[REG_R0] = -1; }
            } else { registers[REG_R0] = -1; }
        } else { registers[REG_R0] = -1; }
    }
    private void handleWrite() {
        int fd = registers[REG_R0], buf = registers[REG_R1], count = registers[REG_R2];
        if (count <= 0 || buf < 0 || buf >= memory.length) { registers[REG_R0] = -1; return; }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 1 || fd == 2) {
            // stdout/stderr - escrever no OpenTTY
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < count && buf + i < memory.length; i++) { sb.append((char)(memory[buf + i] & 0xFF)); }
            
            midlet.print(sb.toString(), stdout, id);
            
            registers[REG_R0] = count;
            
        } else if (fileDescriptors.containsKey(fdKey)) {
            Object stream = fileDescriptors.get(fdKey);
            
            if (stream instanceof OutputStream) {
                try {
                    OutputStream os = (OutputStream) stream;
                    for (int i = 0; i < count && buf + i < memory.length; i++) { os.write(memory[buf + i]); }
                    os.flush();
                    registers[REG_R0] = count;
                } catch (Exception e) { registers[REG_R0] = -1; }
            } else if (stream instanceof StringBuffer) {
                StringBuffer sb = (StringBuffer) stream;
                for (int i = 0; i < count && buf + i < memory.length; i++) { sb.append((char)(memory[buf + i] & 0xFF)); }
                registers[REG_R0] = count;
            } else { registers[REG_R0] = -1; }
        } else { registers[REG_R0] = -1; }
    }
    // | (Informations)
    private void handleStat() {
        int pathAddr = registers[REG_R0], statbufAddr = registers[REG_R1];
        
        if (pathAddr < 0 || pathAddr >= memory.length || statbufAddr < 0 || statbufAddr >= memory.length) { registers[REG_R0] = -1; return; }
        
        // Ler caminho
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) {
            pathBuf.append((char)(memory[pathAddr + i] & 0xFF));
            i++;
        }
        String path = pathBuf.toString();
        
        // Implementação simplificada de struct stat
        // Preencher com valores básicos
        for (i = 0; i < 108; i++) { // Tamanho aproximado de struct stat
            if (statbufAddr + i < memory.length) {
                memory[statbufAddr + i] = 0;
            }
        }
        
        // st_mode
        int st_mode = 0;
        if (path.endsWith("/")) { st_mode |= S_IFDIR | S_IRWXU | S_IRWXG | S_IROTH | S_IXOTH; } 
        else { st_mode |= 0100644; } // Arquivo regular

        writeIntLE(memory, statbufAddr + 16, st_mode);
        
        // st_size
        int st_size = 0;
        if (!path.endsWith("/")) {
            try {
                InputStream is = midlet.getInputStream(path);
                if (is != null) {
                    int available = is.available();
                    if (available > 0) {
                        st_size = available;
                    }
                    is.close();
                }
            } catch (Exception e) {}
        }
        writeIntLE(memory, statbufAddr + 44, st_size);
        
        registers[REG_R0] = 0;
    }
    private void handleFstat() {
        int fd = registers[REG_R0];
        int statbufAddr = registers[REG_R1];
        
        if (statbufAddr < 0 || statbufAddr >= memory.length) {
            registers[REG_R0] = -1; // EFAULT
            return;
        }
        
        // Zerar buffer
        for (int i = 0; i < 108; i++) { if (statbufAddr + i < memory.length) { memory[statbufAddr + i] = 0; } }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 0 || fd == 1 || fd == 2) {
            // stdin/stdout/stderr - dispositivo de caractere
            writeIntLE(memory, statbufAddr + 16, 020000); // st_mode: character device
        } else if (fileDescriptors.containsKey(fdKey)) {
            Object stream = fileDescriptors.get(fdKey);
            
            if (stream instanceof InputStream || stream instanceof OutputStream) {
                // Arquivo regular
                writeIntLE(memory, statbufAddr + 16, 0100644); // st_mode: regular file
                
                // Tentar obter tamanho
                try {
                    if (stream instanceof InputStream) {
                        int available = ((InputStream) stream).available();
                        writeIntLE(memory, statbufAddr + 44, available);
                    }
                } catch (Exception e) {}
            } else {
                // Dispositivo desconhecido
                writeIntLE(memory, statbufAddr + 16, 020000);
            }
        } else {
            registers[REG_R0] = -9; // EBADF
            return;
        }
        
        registers[REG_R0] = 0; // Sucesso
    }
    private void handleLseek() {
        int fd = registers[REG_R0], offset = registers[REG_R1], whence = registers[REG_R2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) { registers[REG_R0] = -9; return; } // EBADF
        
        // Implementação simplificada - sempre retorna sucesso mas não faz nada
        // Em uma implementação real, precisaríamos controlar a posição do arquivo
        registers[REG_R0] = 0; // Sucesso (sempre na posição 0)
    }
    private void handleFsync() {
        int fd = registers[REG_R0];
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) { registers[REG_R0] = -9; return; }

        try {
            if (fileDescriptors.containsKey(fdKey)) {
                Object stream = fileDescriptors.get(fdKey);
                if (stream instanceof OutputStream) { ((OutputStream) stream).flush(); }
            }
            registers[REG_R0] = 0;
        } catch (Exception e) { registers[REG_R0] = -1; }
    }
    // |
    // Network
    // | (Open and Connect)
    private void handleSocket() {
        int domain = registers[REG_R0], type = registers[REG_R1], protocol = registers[REG_R2];
        if (domain != AF_INET) { registers[REG_R0] = -97; return; }
        if (type != SOCK_STREAM && type != SOCK_DGRAM) { registers[REG_R0] = -22; return; }
        
        try {
            String protocolStr = (type == SOCK_STREAM) ? "tcp" : "udp";
            String url = "socket://0.0.0.0";
            
            StreamConnectionNotifier server = null;
            if (type == SOCK_STREAM) { server = (StreamConnectionNotifier) Connector.open("socket://:0"); }
            
            int fd = nextFd++;
            Hashtable socketInfo = new Hashtable();
            socketInfo.put("type", new Integer(type));
            socketInfo.put("protocol", new Integer(protocol));
            socketInfo.put("server", server);
            socketInfo.put("connected", Boolean.FALSE);
            
            socketDescriptors.put(new Integer(fd), socketInfo);
            fileDescriptors.put(new Integer(fd), null); // Placeholder
            
            registers[REG_R0] = fd;
        } catch (Exception e) { registers[REG_R0] = -1; }
    }
    private void handleConnect() {
        int fd = registers[REG_R0], sockaddrPtr = registers[REG_R1], addrlen = registers[REG_R2];
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_R0] = -9; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        
        // Ler estrutura sockaddr_in da memória
        if (sockaddrPtr + 16 > memory.length) { registers[REG_R0] = -14; return; }
        
        int sin_family = readShortLE(memory, sockaddrPtr), sin_port = readShortLE(memory, sockaddrPtr + 2);
        byte[] sin_addr = new byte[4];
        for (int i = 0; i < 4; i++) { sin_addr[i] = memory[sockaddrPtr + 4 + i]; }
        if (sin_family != AF_INET) { registers[REG_R0] = -97; return; }
        
        String host = (sin_addr[0] & 0xFF) + "." + (sin_addr[1] & 0xFF) + "." + (sin_addr[2] & 0xFF) + "." + (sin_addr[3] & 0xFF), port = String.valueOf(sin_port & 0xFFFF);
        
        try {
            SocketConnection conn = (SocketConnection) Connector.open("socket://" + host + ":" + port);
            
            socketInfo.put("connection", conn);
            socketInfo.put("connected", Boolean.TRUE);
            
            if (type == SOCK_STREAM) {
                InputStream is = conn.openInputStream();
                OutputStream os = conn.openOutputStream();
                
                fileDescriptors.put(fdKey, is);
                socketInfo.put("outputStream", os);
            }
            
            registers[REG_R0] = 0;
        } catch (Exception e) { registers[REG_R0] = -111; }
    }
    // | (Read and Write)
    private void handleSend() {
        int fd = registers[REG_R0], buf = registers[REG_R1], len = registers[REG_R2], flags = registers[REG_R3];
        Integer fdKey = new Integer(fd);

        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_R0] = -9; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        if (!((Boolean) socketInfo.get("connected")).booleanValue()) { registers[REG_R0] = -107; return; }
        
        try {
            OutputStream os = (OutputStream) socketInfo.get("outputStream");
            if (os == null) { registers[REG_R0] = -9; return; }
            
            byte[] data = new byte[len];
            for (int i = 0; i < len && buf + i < memory.length; i++) { data[i] = memory[buf + i]; }
            
            os.write(data); os.flush();
            
            registers[REG_R0] = len;
        } catch (Exception e) { registers[REG_R0] = -32; }
    }
    private void handleRecv() {
        int fd = registers[REG_R0], buf = registers[REG_R1], len = registers[REG_R2], flags = registers[REG_R3];
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_R0] = -9; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        if (!((Boolean) socketInfo.get("connected")).booleanValue()) { registers[REG_R0] = -107; return; }
        
        try {
            InputStream is = (InputStream) fileDescriptors.get(fdKey);
            if (is == null) { registers[REG_R0] = -9; return; }
            
            int bytesRead = 0;
            for (int i = 0; i < len && buf + i < memory.length; i++) {
                int b = is.read();
                if (b == -1) {
                    if (bytesRead == 0) { registers[REG_R0] = 0; }
                    else { registers[REG_R0] = bytesRead; }

                    return;
                }
                memory[buf + i] = (byte) b;
                bytesRead++;
            }
            
            registers[REG_R0] = bytesRead;
        } catch (Exception e) { registers[REG_R0] = -104; }
    }
    // |
    private void handleBind() { registers[REG_R0] = -1; } // Não implementado
    private void handleListen() { registers[REG_R0] = -1; } // Não implementado
    private void handleAccept() { registers[REG_R0] = -1; } // Não implementado
    private void handleShutdown() { registers[REG_R0] = -1; } // Não implementado
    private void handleSetsockopt() { registers[REG_R0] = -1; } // Não implementado
    private void handleGetsockopt() { registers[REG_R0] = -1; } // Não implementado
    private void handleSendto() { registers[REG_R0] = -1; } // Não implementado
    private void handleRecvfrom() { registers[REG_R0] = -1; } // Não implementado
    private void handleGetsockname() { registers[REG_R0] = -1; } // Não implementado
    private void handleGetpeername() { registers[REG_R0] = -1; } // Não implementado

    private void handleFutex() {
        int uaddr = registers[REG_R0];
        int op = registers[REG_R1];
        int val = registers[REG_R2];
        int timeout = registers[REG_R3];
        int uaddr2 = registers[REG_R4];
        int val3 = registers[REG_R5];
        
        // Implementação simplificada
        switch (op & 0x7F) { // Mask out flags
            case 0: // FUTEX_WAIT
                int currentVal = readIntLE(memory, uaddr);
                if (currentVal != val) {
                    registers[REG_R0] = -11; // EAGAIN
                } else {
                    // Adicionar à lista de espera
                    Vector waiters = (Vector) futexWaiters.get(new Integer(uaddr));
                    if (waiters == null) {
                        waiters = new Vector();
                        futexWaiters.put(new Integer(uaddr), waiters);
                    }
                    waiters.addElement(new Integer(id));
                    registers[REG_R0] = 0;
                }
                break;
                
            case 1: // FUTEX_WAKE
                Vector waiters = (Vector) futexWaiters.get(new Integer(uaddr));
                if (waiters != null) {
                    int wakeCount = Math.min(val, waiters.size());
                    for (int i = 0; i < wakeCount; i++) {
                        waiters.removeElementAt(0);
                    }
                    registers[REG_R0] = wakeCount;
                } else {
                    registers[REG_R0] = 0;
                }
                break;
                
            default:
                registers[REG_R0] = -38; // ENOSYS
        }
    }
    
    private void handleSchedYield() { registers[REG_R0] = 0; }
    
    private void handleUname() {
        int buf = registers[REG_R0];
        
        if (buf == 0 || buf + 65 * 5 > memory.length) {
            registers[REG_R0] = -14; // EFAULT
            return;
        }

        String sysname = "Linux";
        String nodename = "opentty";
        String release = "3.2.0";
        String version = "#1 " + midlet.build;
        String machine = "armv5tejl";
        String domainname = "";
        
        writeString(memory, buf, sysname, 65);
        writeString(memory, buf + 65, nodename, 65);
        writeString(memory, buf + 130, release, 65);
        writeString(memory, buf + 195, version, 65);
        writeString(memory, buf + 260, machine, 65);
        writeString(memory, buf + 325, domainname, 65);
        
        registers[REG_R0] = 0;
    }
    

    
    private void handleFcntl() {
        int fd = registers[REG_R0];
        int cmd = registers[REG_R1];
        int arg = registers[REG_R2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) {
            registers[REG_R0] = -9; // EBADF
            return;
        }
        
        switch (cmd) {
            case F_GETFL:
                // Retornar flags do arquivo
                registers[REG_R0] = 0; // Por padrão, sem flags especiais
                break;
                
            case F_SETFL:
                // Configurar flags - ignorado por enquanto
                registers[REG_R0] = 0;
                break;
                
            default:
                registers[REG_R0] = -22; // EINVAL
        }
    }
    
    private void handleFtruncate() {
        int fd = registers[REG_R0];
        int length = registers[REG_R1];
        
        // Implementação simplificada
        registers[REG_R0] = 0;
    }
    private void handleTruncate() {
        int path = registers[REG_R0];
        int length = registers[REG_R1];
        
        // Implementação simplificada
        registers[REG_R0] = 0;
    }
    
    private void handleGetrlimit() {
        int resource = registers[REG_R0];
        int rlim = registers[REG_R1];
        
        if (rlim == 0 || rlim + 8 > memory.length) {
            registers[REG_R0] = -14; // EFAULT
            return;
        }
        
        // Valores padrão
        long soft = 0x7FFFFFFFL;
        long hard = 0x7FFFFFFFL;
        
        switch (resource) {
            case 3: // RLIMIT_STACK
                soft = 8 * 1024 * 1024; // 8MB
                hard = soft;
                break;
            case 6: // RLIMIT_AS (virtual memory)
                soft = memory.length;
                hard = soft;
                break;
            case 7: // RLIMIT_CORE
                soft = 0;
                hard = 0;
                break;
        }
        
        writeIntLE(memory, rlim, (int)soft);
        writeIntLE(memory, rlim + 4, (int)hard);
        
        registers[REG_R0] = 0;
    }
    
    private void checkPendingSignals() { }

    private void pushSignalFrame(int sig) {
        // Salvar contexto atual na stack
        int sp = registers[REG_SP];
        
        // Push registers
        for (int i = 0; i < 16; i++) {
            sp -= 4;
            writeIntLE(memory, sp, registers[i]);
        }
        
        // Push signal number
        sp -= 4;
        writeIntLE(memory, sp, sig);
        
        // Push return address (PC atual)
        sp -= 4;
        writeIntLE(memory, sp, pc);
        
        registers[REG_SP] = sp;
        
        // Armazenar frame no stack de sinais
        Hashtable frame = new Hashtable();
        frame.put("sp", new Integer(sp));
        frame.put("old_pc", new Integer(pc));
        signalStack.addElement(frame);
    }
    

    private void handleSelect() { int nfds = registers[REG_R0], readfds = registers[REG_R1], writefds = registers[REG_R2], exceptfds = registers[REG_R3], timeoutPtr = pc + 16; registers[REG_R0] = 0; }
    private void handlePoll() { int fdsPtr = registers[REG_R0], nfds = registers[REG_R1], timeout = registers[REG_R2]; registers[REG_R0] = 0; }

    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); } 
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }

    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    private void writeShortLE(byte[] data, int offset, short value) { if (offset + 1 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); }

    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }

    private int findFreeMemoryRegion(int length) {
        // Começar depois do heap
        int start = heapEnd;
        
        // Encontrar região livre
        while (start + length < memory.length) {
            boolean free = true;
            
            // Verificar mapeamentos existentes
            for (int i = 0; i < memoryMappings.size(); i++) {
                Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
                int maddr = ((Integer)mapping.get("addr")).intValue();
                int mlen = ((Integer)mapping.get("length")).intValue();
                
                if (start < maddr + mlen && start + length > maddr) {
                    free = false;
                    start = maddr + mlen;
                    break;
                }
            }
            
            if (free) {
                // Alinhar para página
                start = (start + 4095) & ~4095;
                return start;
            }
        }
        
        return 0;
    }
    private boolean isMemoryRegionFree(int addr, int length) {
        for (int i = 0; i < memoryMappings.size(); i++) {
            Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
            int maddr = ((Integer)mapping.get("addr")).intValue();
            int mlen = ((Integer)mapping.get("length")).intValue();
            
            if (addr < maddr + mlen && addr + length > maddr) {
                return false;
            }
        }
        return true;
    }

    private String readString(byte[] data, int offset, int maxLen) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < maxLen && offset + i < data.length; i++) {
            byte b = data[offset + i];
            if (b == 0) break;
            sb.append((char)(b & 0xFF));
        }
        return sb.toString();
    }
    private void writeString(byte[] mem, int addr, String str, int maxLen) {
        byte[] bytes = str.getBytes();
        int len = Math.min(bytes.length, maxLen - 1);
        for (int i = 0; i < len; i++) {
            mem[addr + i] = bytes[i];
        }
        mem[addr + len] = 0;
    }

    private void debugMemoryAccess(int addr, int size, boolean write, int value) { if (midlet.debug && addr < 0x10000) { String op = write ? "WRITE" : "READ"; midlet.print("MEM " + op + " at " + toHex(addr) + " size=" + size + (write ? " value=" + toHex(value) : ""), stdout, id); } }
}
