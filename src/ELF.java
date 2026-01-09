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

    // Dynamic linking structures
    private Hashtable dynamicSymbols;      // Símbolos dinâmicos: nome -> {value, size, type}
    private Hashtable neededLibraries;     // Bibliotecas necessárias
    private Vector loadedLibraries;        // Bibliotecas carregadas
    private Hashtable globalSymbols;       // Tabela global de símbolos
    private Hashtable pltEntries, libcSymbols;          // Entradas PLT: índice -> {symIndex, gotOffset, resolved}
    private int pltGotAddr;                // Endereço da PLT/GOT
    private int dynamicSectionAddr;        // Endereço da seção dinâmica
    private int gotBase;                   // Base da GOT
    private int pltBase;                   // Base da PLT
    private int resolverCodeAddr;          // Endereço do código do resolvedor
    private int resolveFuncAddr;           // Endereço da função de resolução
        
    // Mapeamentos de memória
    private Vector memoryMappings;
    
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

    // Dynamic linking constants - adicione com as outras constantes ELF
    private static final int DT_NULL = 0;
    private static final int DT_NEEDED = 1;
    private static final int DT_PLTRELSZ = 2;
    private static final int DT_PLTGOT = 3;
    private static final int DT_HASH = 4;
    private static final int DT_STRTAB = 5;
    private static final int DT_SYMTAB = 6;
    private static final int DT_RELA = 7;
    private static final int DT_RELASZ = 8;
    private static final int DT_RELAENT = 9;
    private static final int DT_STRSZ = 10;
    private static final int DT_SYMENT = 11;
    private static final int DT_INIT = 12;
    private static final int DT_FINI = 13;
    private static final int DT_SONAME = 14;
    private static final int DT_RPATH = 15;
    private static final int DT_SYMBOLIC = 16;
    private static final int DT_REL = 17;
    private static final int DT_RELSZ = 18;
    private static final int DT_RELENT = 19;
    private static final int DT_PLTREL = 20;
    private static final int DT_DEBUG = 21;
    private static final int DT_TEXTREL = 22;
    private static final int DT_JMPREL = 23;
    private static final int DT_BIND_NOW = 24;
    private static final int DT_INIT_ARRAY = 25;
    private static final int DT_FINI_ARRAY = 26;
    private static final int DT_INIT_ARRAYSZ = 27;
    private static final int DT_FINI_ARRAYSZ = 28;

    // Relocation types
    private static final int R_ARM_ABS32 = 2;
    private static final int R_ARM_REL32 = 3;
    private static final int R_ARM_GLOB_DAT = 21;
    private static final int R_ARM_JUMP_SLOT = 22;
    private static final int R_ARM_RELATIVE = 23;
        
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
    private Hashtable futexWaiters, args;
    
    public ELF(OpenTTY midlet, Hashtable args, Object stdout, Hashtable scope, int id, String pid, Hashtable proc) {
        this.midlet = midlet; this.stdout = stdout; this.id = id;
        this.scope = scope; this.proc = proc; this.args = args;
        this.pid = pid == null ? midlet.genpid() : pid;
        this.memory = new byte[1 * 1024 * 1024]; 
        this.registers = new int[16];
        this.fpuRegisters = new float[32]; // S0-S31 (single precision)
        this.cpsr = 0; this.fpscr = 0;
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
        this.dynamicSymbols = new Hashtable();
        this.neededLibraries = new Hashtable();
        this.loadedLibraries = new Vector();
        this.globalSymbols = new Hashtable();
        this.pltEntries = new Hashtable();
        this.pltGotAddr = 0;
        this.dynamicSectionAddr = 0;
        this.gotBase = 0;
        this.pltBase = 0;
        this.resolverCodeAddr = 0;
        this.resolveFuncAddr = 0;

        // Carregar bibliotecas padrão
        loadDefaultLibraries();

        this.signalHandlers = new int[NSIG];
        for (int i = 0; i < NSIG; i++) { signalHandlers[i] = SIG_DFL; }
        
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

        // Processar dynamic segment
        processDynamicSection(elfData);
        
        // Carregar bibliotecas necessárias
        loadNeededLibraries();
        
        // Processar símbolos e realocações
        processSymbolsAndRelocations(elfData, sectionInfo);
        
        // Configurar PLT/GOT
        setupPLTGOT();
        
        // Executar funções de inicialização
        executeInitFunctions();
        
        return true;
    }

    private void processDynamicSection(byte[] elfData) {
        int e_phoff = ((Integer)elfInfo.get("phoff")).intValue();
        int e_phnum = ((Integer)elfInfo.get("phnum")).intValue();
        
        for (int i = 0; i < e_phnum; i++) {
            int phdrOffset = e_phoff + i * 32;
            int p_type = readIntLE(elfData, phdrOffset);
            
            if (p_type == PT_DYNAMIC) {
                dynamicSectionAddr = readIntLE(elfData, phdrOffset + 8);
                int p_filesz = readIntLE(elfData, phdrOffset + 16);
                processDynamicEntries(elfData, dynamicSectionAddr, p_filesz);
                break;
            }
        }
    }

    private void processDynamicEntries(byte[] elfData, int dynAddr, int dynSize) {
        int offset = 0;
        
        while (offset < dynSize) {
            int tag = readIntLE(elfData, dynAddr + offset);
            int val = readIntLE(elfData, dynAddr + offset + 4);
            
            if (tag == DT_NULL) break;
            
            switch (tag) {
                case DT_NEEDED:
                    String libName = readString(elfData, val, 256);
                    neededLibraries.put(libName, new Integer(val));
                    if (midlet.debug) midlet.print("Needed library: " + libName, stdout);
                    break;
                    
                case DT_PLTGOT:
                    pltGotAddr = val;
                    if (midlet.debug) midlet.print("PLT/GOT at: " + toHex(val), stdout);
                    break;
                    
                case DT_STRTAB:
                    elfInfo.put("dynstr", new Integer(val));
                    break;
                    
                case DT_SYMTAB:
                    elfInfo.put("dynsym", new Integer(val));
                    break;
                    
                case DT_SYMENT:
                    elfInfo.put("syment", new Integer(val));
                    break;
                    
                case DT_JMPREL:
                    elfInfo.put("jmprel", new Integer(val));
                    break;
                    
                case DT_PLTRELSZ:
                    elfInfo.put("pltrelsz", new Integer(val));
                    break;
                    
                case DT_PLTREL:
                    elfInfo.put("pltrel", new Integer(val));
                    break;
                    
                case DT_REL:
                    elfInfo.put("rel", new Integer(val));
                    break;
                    
                case DT_RELSZ:
                    elfInfo.put("relsz", new Integer(val));
                    break;
                    
                case DT_INIT:
                    elfInfo.put("init", new Integer(val));
                    break;
                    
                case DT_FINI:
                    elfInfo.put("fini", new Integer(val));
                    break;
                    
                case DT_HASH:
                    processHashTable(elfData, val);
                    break;
            }
            
            offset += 8;
        }
    }

    private void processHashTable(byte[] elfData, int hashAddr) {
        int nbucket = readIntLE(elfData, hashAddr);
        int nchain = readIntLE(elfData, hashAddr + 4);
        
        elfInfo.put("nbucket", new Integer(nbucket));
        elfInfo.put("nchain", new Integer(nchain));
        elfInfo.put("buckets", new Integer(hashAddr + 8));
        elfInfo.put("chains", new Integer(hashAddr + 8 + nbucket * 4));
    }

    private void loadNeededLibraries() {
        Enumeration libNames = neededLibraries.keys();
        while (libNames.hasMoreElements()) {
            String libName = (String) libNames.nextElement();
            
            if (!loadedLibraries.contains(libName)) {
                if (loadLibrary(libName)) {
                    loadedLibraries.addElement(libName);
                    if (midlet.debug) midlet.print("Loaded library: " + libName, stdout);
                } else {
                    if (midlet.debug) midlet.print("Failed to load: " + libName, stdout);
                }
            }
        }
    }

    private boolean loadLibrary(String libName) {
        // Tentar caminhos comuns
        String[] paths = { "/lib/" + libName, "/usr/lib/" + libName };
        
        for (int i = 0; i < paths.length; i++) {
            try {
                InputStream is = midlet.getInputStream(paths[i]);
                if (is != null) {
                    // Biblioteca encontrada - criar símbolos simulados
                    Hashtable libSyms = new Hashtable();
                    
                    // Adicionar símbolos básicos baseados no nome da lib
                    if (libName.indexOf("c") != -1) {
                        libSyms.put("printf", globalSymbols.get("libc.so.6"));
                    }
                    if (libName.indexOf("m") != -1) { // math
                        libSyms.put("sin", new Integer(createSimpleStub(32)));
                        libSyms.put("cos", new Integer(createSimpleStub(32)));
                    }
                    
                    globalSymbols.put(libName, libSyms);
                    is.close();
                    return true;
                }
            } catch (Exception e) {}
        }
        
        return false;
    }

    private void loadDefaultLibraries() {
        // Libc simulada
        Hashtable libc = new Hashtable();
        
        // Adicionar funções básicas
        libc.put("printf", new Integer(createSyscallStub("write")));
        libc.put("puts", new Integer(createSyscallStub("write")));
        libc.put("malloc", new Integer(createSyscallStub("brk")));
        libc.put("free", new Integer(createSyscallStub("brk")));
        libc.put("strlen", new Integer(createSimpleStub(16))); // Stub simples
        libc.put("strcpy", new Integer(createSimpleStub(32)));
        libc.put("strcmp", new Integer(createSimpleStub(32)));
        libc.put("memcpy", new Integer(createSimpleStub(48)));
        libc.put("memset", new Integer(createSimpleStub(32)));
        libc.put("exit", new Integer(createSyscallStub("exit")));
        libc.put("open", new Integer(createSyscallStub("open")));
        libc.put("read", new Integer(createSyscallStub("read")));
        libc.put("write", new Integer(createSyscallStub("write")));
        libc.put("close", new Integer(createSyscallStub("close")));
        
        globalSymbols.put("libc.so.6", libc);
        loadedLibraries.addElement("libc.so.6");
        
        if (midlet.debug) {
            midlet.print("Loaded default libraries", stdout);
        }
    }

    private int createSimpleStub(int size) {
        int stubAddr = findFreeMemoryRegion(size);
        if (stubAddr == 0) { return 0; }

        writeIntLE(memory, stubAddr, 0xE3A00000); // mov r0, #0
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E); // bx lr
        
        return stubAddr;
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
                    midlet.print("DEBUG: PC=" + toHex(pc) + ", R7=" + registers[REG_R7], stdout, id);
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
            }
        } 
        catch (Throwable e) { 
            if (midlet.debug) midlet.print("=== ELF CRASH DEBUG ===\nCRASH: " + e.getClass().getName() + ": " + e.getMessage(), stdout, id);
            e.printStackTrace();
            running = false; 
        } 
        finally { 
            if (midlet.debug) midlet.print("=== ELF FINALLY DEBUG ===", stdout, id);
            if (midlet.sys.containsKey(pid)) { 
                midlet.sys.remove(pid); 
            } 
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

        if ((instruction & 0xFF000000) == 0xEB000000) { // bl
            int offset = instruction & 0x00FFFFFF;
            if ((offset & 0x00800000) != 0) offset |= 0xFF000000;
            offset <<= 2;
            
            int target = pc + offset - 4;
            
            if (pltBase != 0 && target >= pltBase && target < pltBase + 4096) {
                handlePLTCall(target);
                return;
            }
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

    private void executeInitFunctions() {
        // .init
        if (elfInfo.containsKey("init")) {
            int initAddr = ((Integer)elfInfo.get("init")).intValue();
            if (initAddr != 0) {
                int savedPC = pc;
                int savedSP = registers[REG_SP];
                
                registers[REG_LR] = savedPC;
                pc = initAddr;
                
                if (midlet.debug) {
                    midlet.print("Calling .init at " + toHex(initAddr), stdout);
                }
                
                // Executar brevemente
                for (int i = 0; i < 100 && running; i++) {
                    int instruction = fetchInstruction(pc);
                    pc += 4;
                    executeInstruction(instruction);
                }
                
                pc = savedPC;
                registers[REG_SP] = savedSP;
            }
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
        
        // Primeiro, encontrar a seção .shstrtab
        int shstrtabIndex = -1;
        int shstrtabOffset = 0;
        
        for (int i = 0; i < shnum; i++) {
            int shdrOffset = shoff + i * shentsize;
            int sh_type = readIntLE(elfData, shdrOffset + 4);
            int sh_name = readIntLE(elfData, shdrOffset);
            
            if (sh_type == 3) { // SHT_STRTAB
                shstrtabIndex = i;
                shstrtabOffset = readIntLE(elfData, shdrOffset + 16); // sh_offset
                break;
            }
        }
        
        if (shstrtabIndex == -1) {
            // Sem string table, usar índices
            for (int i = 0; i < shnum; i++) {
                Hashtable section = new Hashtable();
                sections.put("section_" + i, section);
            }
            return sections;
        }
        
        // Ler nome da seção .shstrtab primeiro
        String shstrtabName = readString(elfData, shstrtabOffset, 256);
        if (midlet.debug && shstrtabName.length() > 0) {
            midlet.print("Found .shstrtab at offset " + toHex(shstrtabOffset), stdout);
        }
        
        // Agora processar todas as seções
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
            
            // Ler nome da seção
            if (shstrtabOffset > 0 && sh_name > 0) {
                String name = readString(elfData, shstrtabOffset + sh_name, 64);
                if (name != null && name.length() > 0) {
                    sections.put(name, section);
                    if (midlet.debug) {
                        midlet.print("Section: " + name + " at " + toHex(sh_addr) + 
                                " (size: " + sh_size + ")", stdout);
                    }
                } else {
                    sections.put("section_" + i, section);
                }
            } else {
                sections.put("section_" + i, section);
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
    
    private void processSymbolsAndRelocations(byte[] elfData, Hashtable sections) {
        if (!elfInfo.containsKey("dynsym") || !elfInfo.containsKey("dynstr")) {
            return;
        }
        
        int dynsymAddr = ((Integer)elfInfo.get("dynsym")).intValue();
        int dynstrAddr = ((Integer)elfInfo.get("dynstr")).intValue();
        int symentSize = elfInfo.containsKey("syment") ? 
            ((Integer)elfInfo.get("syment")).intValue() : 16;
        
        // Processar símbolos
        int symOffset = dynsymAddr;
        while (true) {
            int st_name = readIntLE(elfData, symOffset);
            int st_value = readIntLE(elfData, symOffset + 4);
            int st_size = readIntLE(elfData, symOffset + 8);
            int st_info = elfData[symOffset + 12] & 0xFF;
            
            if (st_name == 0 && st_value == 0 && st_size == 0 && st_info == 0) {
                break;
            }
            
            String symName = readString(elfData, dynstrAddr + st_name, 256);
            
            Hashtable symInfo = new Hashtable();
            symInfo.put("value", new Integer(st_value));
            symInfo.put("size", new Integer(st_size));
            symInfo.put("info", new Integer(st_info));
            symInfo.put("binding", new Integer((st_info >> 4) & 0xF));
            symInfo.put("type", new Integer(st_info & 0xF));
            
            dynamicSymbols.put(symName, symInfo);
            
            symOffset += symentSize;
        }
        
        // Processar realocações
        processRelocations(elfData);
    }

    private void processRelocations(byte[] elfData) {
        // REL
        if (elfInfo.containsKey("rel") && elfInfo.containsKey("relsz")) {
            int relAddr = ((Integer)elfInfo.get("rel")).intValue();
            int relsz = ((Integer)elfInfo.get("relsz")).intValue();
            int relent = 8;
            
            for (int i = 0; i < relsz; i += relent) {
                int offset = relAddr + i;
                int r_offset = readIntLE(elfData, offset);
                int r_info = readIntLE(elfData, offset + 4);
                
                int symIndex = r_info >> 8;
                int type = r_info & 0xFF;
                
                applyRelocation(r_offset, type, symIndex, 0);
            }
        }
        
        // JMPREL (PLT)
        if (elfInfo.containsKey("jmprel") && elfInfo.containsKey("pltrelsz")) {
            int jmprelAddr = ((Integer)elfInfo.get("jmprel")).intValue();
            int pltrelsz = ((Integer)elfInfo.get("pltrelsz")).intValue();
            int pltrel = elfInfo.containsKey("pltrel") ? 
                ((Integer)elfInfo.get("pltrel")).intValue() : DT_REL;
            
            int relent = (pltrel == DT_RELA) ? 12 : 8;
            int numEntries = pltrelsz / relent;
            
            if (gotBase == 0 && pltGotAddr != 0) {
                gotBase = pltGotAddr + 12;
            }
            
            for (int i = 0; i < numEntries; i++) {
                int offset = jmprelAddr + i * relent;
                int r_offset = readIntLE(elfData, offset);
                int r_info = readIntLE(elfData, offset + 4);
                
                int symIndex = r_info >> 8;
                int type = r_info & 0xFF;
                
                if (type == R_ARM_JUMP_SLOT) {
                    setupLazyBinding(r_offset, symIndex, i);
                } else {
                    applyRelocation(r_offset, type, symIndex, 0);
                }
            }
        }
    }

    private void applyRelocation(int r_offset, int type, int symIndex, int addend) {
        switch (type) {
            case R_ARM_ABS32:
            case R_ARM_GLOB_DAT:
                String symName = getSymbolNameByIndex(symIndex);
                Integer symAddr = resolveSymbol(symName);
                
                if (symAddr != null) {
                    writeIntLE(memory, r_offset, symAddr.intValue() + addend);
                    if (midlet.debug) {
                        midlet.print("Reloc: " + symName + " -> " + 
                                toHex(symAddr.intValue()) + " at " + toHex(r_offset), stdout);
                    }
                }
                break;
                
            case R_ARM_RELATIVE:
                int current = readIntLE(memory, r_offset);
                writeIntLE(memory, r_offset, current + addend);
                break;
        }
    }

    private void setupLazyBinding(int gotOffset, int symIndex, int slotIndex) {
        int resolverAddr = setupResolverStub(slotIndex);
        
        if (resolverAddr != 0) {
            writeIntLE(memory, gotOffset, resolverAddr);
            
            Hashtable pltInfo = new Hashtable();
            pltInfo.put("symIndex", new Integer(symIndex));
            pltInfo.put("gotOffset", new Integer(gotOffset));
            pltInfo.put("resolved", Boolean.FALSE);
            
            pltEntries.put("plt_" + slotIndex, pltInfo);
            
            if (midlet.debug) {
                midlet.print("Lazy binding: slot " + slotIndex + " at GOT " + toHex(gotOffset), stdout);
            }
        }
    }

    private int setupResolverStub(int slotIndex) {
        if (pltBase == 0) {
            pltBase = findFreeMemoryRegion(4096);
            if (pltBase == 0) return 0;
        }
        
        int stubAddr = pltBase + slotIndex * 16;
        
        // ldr pc, [pc, #-4]
        writeIntLE(memory, stubAddr, 0xE51FF004);
        
        // Endereço do trampoline
        int trampoline = setupResolverTrampoline();
        writeIntLE(memory, stubAddr + 4, trampoline + slotIndex * 8);
        
        return stubAddr;
    }

    private int setupResolverTrampoline() {
        if (resolverCodeAddr == 0) {
            resolverCodeAddr = findFreeMemoryRegion(256);
            if (resolverCodeAddr == 0) return 0;
            
            // stmfd sp!, {r0-r3, lr}
            writeIntLE(memory, resolverCodeAddr, 0xE92D400F);
            
            // ldr r0, [pc, #4]  ; índice PLT
            writeIntLE(memory, resolverCodeAddr + 4, 0xE59F0004);
            
            // bl resolve_function
            int resolveAddr = setupResolveFunction();
            int offset = ((resolveAddr - (resolverCodeAddr + 8 + 8)) >> 2) & 0x00FFFFFF;
            writeIntLE(memory, resolverCodeAddr + 8, 0xEB000000 | offset);
            
            // ldmfd sp!, {r0-r3, lr}
            writeIntLE(memory, resolverCodeAddr + 12, 0xE8BD400F);
            
            // bx r0
            writeIntLE(memory, resolverCodeAddr + 16, 0xE12FFF10);
            
            // .word plt_index
            writeIntLE(memory, resolverCodeAddr + 20, 0);
        }
        
        return resolverCodeAddr;
    }

    private int setupResolveFunction() {
        if (resolveFuncAddr == 0) {
            resolveFuncAddr = findFreeMemoryRegion(128);
            if (resolveFuncAddr == 0) return 0;
            
            // stmfd sp!, {lr}
            writeIntLE(memory, resolveFuncAddr, 0xE92D4000);
            
            // Implementação da resolução
            // ldr r0, [r0]  ; obter índice
            writeIntLE(memory, resolveFuncAddr + 4, 0xE5900000);
            
            // Aqui viria o código real de resolução
            // Por enquanto, chamar resolvePLTSymbol
            writeIntLE(memory, resolveFuncAddr + 8, 0xE12FFF1E); // bx lr
            
            // Armazenar handler
            elfInfo.put("resolve_handler", new Object() {
                public int handle(int pltIndex) {
                    return resolvePLTSymbol(pltIndex);
                }
            });
        }
        
        return resolveFuncAddr;
    }

    private String getSymbolNameByIndex(int index) {
        Enumeration keys = dynamicSymbols.keys();
        int current = 0;
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            if (current == index) return name;
            current++;
        }
        return null;
    }

    private int resolvePLTSymbol(int pltIndex) {
        String key = "plt_" + pltIndex;
        if (!pltEntries.containsKey(key)) return 0;
        
        Hashtable pltInfo = (Hashtable) pltEntries.get(key);
        int symIndex = ((Integer)pltInfo.get("symIndex")).intValue();
        int gotOffset = ((Integer)pltInfo.get("gotOffset")).intValue();
        
        String symName = getSymbolNameByIndex(symIndex);
        Integer resolvedAddr = resolveSymbol(symName);
        
        if (resolvedAddr != null) {
            writeIntLE(memory, gotOffset, resolvedAddr.intValue());
            pltInfo.put("resolved", Boolean.TRUE);
            
            if (midlet.debug) {
                midlet.print("Resolved: " + symName + " -> " + toHex(resolvedAddr.intValue()), stdout);
            }
            
            return resolvedAddr.intValue();
        }
        
        return 0;
    }

    private Integer resolveSymbol(String name) {
        // Verificar no executável
        if (dynamicSymbols.containsKey(name)) {
            Hashtable symInfo = (Hashtable) dynamicSymbols.get(name);
            int value = ((Integer)symInfo.get("value")).intValue();
            if (value != 0) return new Integer(value);
        }
        
        // Verificar em bibliotecas
        for (int i = 0; i < loadedLibraries.size(); i++) {
            String libName = (String) loadedLibraries.elementAt(i);
            Hashtable lib = (Hashtable) globalSymbols.get(libName);
            
            if (lib != null && lib.containsKey(name)) {
                Object symValue = lib.get(name);
                if (symValue instanceof Hashtable) {
                    return new Integer(((Integer)((Hashtable)symValue).get("value")).intValue());
                } else if (symValue instanceof Integer) {
                    return (Integer) symValue;
                }
            }
        }
        
        // Criar stub se for syscall
        if (name.startsWith("sys_")) {
            return new Integer(createSyscallStub(name));
        }
        
        return null;
    }

    private void setupPLTGOT() {
        if (pltGotAddr == 0) return;
        
        // Primeira entrada: dynamic section
        writeIntLE(memory, pltGotAddr, dynamicSectionAddr);
        
        // Segunda entrada: módulo (0)
        writeIntLE(memory, pltGotAddr + 4, 0);
        
        // Terceira entrada: resolvedor
        writeIntLE(memory, pltGotAddr + 8, resolveFuncAddr);
    }

    /*private void setupCRTStack() {
        // Configurar stack para C Runtime
        // Argumentos: argc, argv[], envp[], auxv[]
        
        int sp = registers[REG_SP];
        
        // Auxiliary vector (simplificado)
        writeIntLE(memory, sp - 4, 0); // AT_NULL
        writeIntLE(memory, sp - 8, 0);
        sp -= 8;
        
        // Environment variables (mantenha como está)
        Vector envVars = new Vector();
        envVars.addElement("PATH=/bin");
        envVars.addElement("USER=" + (id == 0 ? "root" : midlet.username));
        envVars.addElement("HOME=/home");
        envVars.addElement("SHELL=/bin/sh");
        envVars.addElement("TERM=vt100");
        for (Enumeration e = midlet.attributes.keys(); e.hasMoreElements();) {
            envVars.addElement(midlet.attributes.get(e.nextElement()));
        }
        
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
        
        // Argumentos do programa (AGORA USANDO OS ARGUMENTOS REAIS)
        Vector argsVec = new Vector();
        
        for (int i = 0; i < args.size(); i++){
            argsVec.addElement(args.get(new Double(i)));
        }
        
        // Se não houver argumentos, usar o padrão
        if (argsVec.size() == 0) { argsVec.addElement("program"); }
        
        // Ponteiros para args
        int argvStart = sp - (argsVec.size() + 1) * 4;
        for (int i = 0; i < argsVec.size(); i++) {
            String arg = (String) argsVec.elementAt(i);
            byte[] argBytes = arg.getBytes();
            sp -= argBytes.length + 1;
            for (int j = 0; j < argBytes.length; j++) {
                memory[sp + j] = argBytes[j];
            }
            memory[sp + argBytes.length] = 0;
            writeIntLE(memory, argvStart + i * 4, sp);
        }
        writeIntLE(memory, argvStart + argsVec.size() * 4, 0); // NULL terminator
        sp = argvStart;
        
        // argc
        sp -= 4;
        writeIntLE(memory, sp, argsVec.size());
        
        // Armazenar argc e argv[0] para uso posterior (opcional)
        elfInfo.put("argc", new Integer(argsVec.size()));
        if (argsVec.size() > 0) {
            elfInfo.put("argv0", argsVec.elementAt(0));
        }
        
        // Configurar stack pointer
        registers[REG_SP] = sp;
        
        if (midlet.debug) {
            midlet.print("Stack setup: SP=" + toHex(registers[REG_SP]), stdout);
            midlet.print("argc=" + argsVec.size(), stdout);
            for (int i = 0; i < argsVec.size(); i++) {
                midlet.print("argv[" + i + "]=" + argsVec.elementAt(i), stdout);
            }
        }
    }*/
    private void setupCRTStack() {
        int sp = registers[REG_SP];
        
        // Argumentos do programa
        Vector argsVec = new Vector();
        for (int i = 0; i < args.size(); i++) { argsVec.addElement(args.get(new Double(i))); }
        if (argsVec.size() == 0) { argsVec.addElement("program"); }
        
        // Environment variables
        Vector envVars = new Vector();
        envVars.addElement("PATH=/bin");
        envVars.addElement("USER=" + (id == 0 ? "root" : midlet.username));
        envVars.addElement("HOME=/home");
        envVars.addElement("SHELL=/bin/sh");
        envVars.addElement("TERM=vt100");
        
        // AUX Vector (para programas dinâmicos)
        boolean isDynamic = dynamicSectionAddr != 0;
        
        if (isDynamic) {
            // AT_NULL
            writeIntLE(memory, sp - 8, 0);
            writeIntLE(memory, sp - 4, 0);
            sp -= 8;
            
            // AT_ENTRY
            writeIntLE(memory, sp - 8, 9); // AT_ENTRY
            writeIntLE(memory, sp - 4, pc);
            sp -= 8;
            
            // Redirecionar para __libc_start_main
            Integer libcStartMain = resolveSymbol("__libc_start_main");
            if (libcStartMain != null) {
                // Empurrar argumentos para __libc_start_main
                // main function (entry point)
                sp -= 4;
                writeIntLE(memory, sp, pc);
                
                // argc
                sp -= 4;
                writeIntLE(memory, sp, argsVec.size());
                
                // argv
                int argvStart = sp - (argsVec.size() + 1) * 4;
                for (int i = 0; i < argsVec.size(); i++) {
                    String arg = (String) argsVec.elementAt(i);
                    byte[] argBytes = arg.getBytes();
                    sp -= argBytes.length + 1;
                    for (int j = 0; j < argBytes.length; j++) {
                        memory[sp + j] = argBytes[j];
                    }
                    memory[sp + argBytes.length] = 0;
                    writeIntLE(memory, argvStart + i * 4, sp);
                }
                writeIntLE(memory, argvStart + argsVec.size() * 4, 0);
                sp = argvStart;
                
                // envp
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
                writeIntLE(memory, envpStart + envVars.size() * 4, 0);
                sp = envpStart;
                
                // Configurar chamada para __libc_start_main
                registers[REG_R0] = pc;  // main
                registers[REG_R1] = argsVec.size();  // argc
                registers[REG_R2] = argvStart;  // argv
                registers[REG_R3] = 0;  // init (null)
                
                pc = libcStartMain.intValue();
                
                if (midlet.debug) {
                    midlet.print("Redirecting to __libc_start_main at " + 
                            toHex(pc), stdout);
                }
            }
        }
        
        registers[REG_SP] = sp;
    }

    private void handlePLTCall(int stubAddr) {
        int pltIndex = (stubAddr - pltBase) / 16;
        
        if (pltIndex >= 0) {
            String key = "plt_" + pltIndex;
            if (pltEntries.containsKey(key)) {
                Hashtable pltInfo = (Hashtable) pltEntries.get(key);
                if (!((Boolean)pltInfo.get("resolved")).booleanValue()) {
                    resolvePLTSymbol(pltIndex);
                }
            }
            
            int gotOffset = pltGotAddr + 12 + pltIndex * 4;
            int realAddr = readIntLE(memory, gotOffset);
            
            registers[REG_LR] = pc;
            pc = realAddr;
            
            if (midlet.debug) {
                midlet.print("PLT call via slot " + pltIndex + " to " + toHex(realAddr), stdout);
            }
        }
    }

    public void dumpDynamicInfo(Object stdout) {
        midlet.print("=== Dynamic Linking Info ===", stdout);
        midlet.print("PLT/GOT: " + toHex(pltGotAddr), stdout);
        midlet.print("PLT Base: " + toHex(pltBase), stdout);
        midlet.print("GOT Base: " + toHex(gotBase), stdout);
        
        midlet.print("\nLoaded Libraries (" + loadedLibraries.size() + "):", stdout);
        for (int i = 0; i < loadedLibraries.size(); i++) {
            midlet.print("  " + loadedLibraries.elementAt(i), stdout);
        }
        
        midlet.print("\nDynamic Symbols (" + dynamicSymbols.size() + "):", stdout);
        Enumeration keys = dynamicSymbols.keys();
        int count = 0;
        while (keys.hasMoreElements() && count < 20) {
            String name = (String) keys.nextElement();
            Hashtable sym = (Hashtable) dynamicSymbols.get(name);
            int value = ((Integer)sym.get("value")).intValue();
            midlet.print("  " + name + " -> " + toHex(value), stdout);
            count++;
        }
    }


    private void initLibc() {
        libcSymbols = new Hashtable();
        
        // === FUNÇÕES DE I/O ===
        libcSymbols.put("printf", new Integer(createPrintfStub()));
        libcSymbols.put("scanf", new Integer(createScanfStub()));
        libcSymbols.put("puts", new Integer(createPutsStub()));
        libcSymbols.put("putchar", new Integer(createPutcharStub()));
        libcSymbols.put("getchar", new Integer(createGetcharStub()));
        libcSymbols.put("scanf", new Integer(createScanfStub()));
        /*libcSymbols.put("fprintf", new Integer(createFprintfStub()));
        libcSymbols.put("fputs", new Integer(createFputsStub()));
        libcSymbols.put("fputc", new Integer(createFputcStub()));
        libcSymbols.put("fgetc", new Integer(createFgetcStub()));*/
        
        // === MEMÓRIA ===
        libcSymbols.put("malloc", new Integer(createMallocStub()));
        /*libcSymbols.put("free", new Integer(createFreeStub()));
        libcSymbols.put("calloc", new Integer(createCallocStub()));
        libcSymbols.put("realloc", new Integer(createReallocStub()));
        
        // === STRINGS ===
        libcSymbols.put("strlen", new Integer(createStrlenStub()));
        libcSymbols.put("strcpy", new Integer(createStrcpyStub()));
        libcSymbols.put("strncpy", new Integer(createStrncpyStub()));
        libcSymbols.put("strcat", new Integer(createStrcatStub()));
        libcSymbols.put("strncat", new Integer(createStrncatStub()));
        libcSymbols.put("strcmp", new Integer(createStrcmpStub()));
        libcSymbols.put("strncmp", new Integer(createStrncmpStub()));
        libcSymbols.put("strstr", new Integer(createStrstrStub()));
        libcSymbols.put("strchr", new Integer(createStrchrStub()));
        libcSymbols.put("strrchr", new Integer(createStrrchrStub()));
        
        // === MEMÓRIA (raw) ===
        libcSymbols.put("memcpy", new Integer(createMemcpyStub()));
        libcSymbols.put("memmove", new Integer(createMemmoveStub()));
        libcSymbols.put("memset", new Integer(createMemsetStub()));
        libcSymbols.put("memcmp", new Integer(createMemcmpStub()));
        libcSymbols.put("memchr", new Integer(createMemchrStub()));
        
        // === ARQUIVOS ===
        libcSymbols.put("fopen", new Integer(createFopenStub()));
        libcSymbols.put("fclose", new Integer(createFcloseStub()));
        libcSymbols.put("fread", new Integer(createFreadStub()));
        libcSymbols.put("fwrite", new Integer(createFwriteStub()));
        libcSymbols.put("fseek", new Integer(createFseekStub()));
        libcSymbols.put("ftell", new Integer(createFtellStub()));
        libcSymbols.put("rewind", new Integer(createRewindStub()));
        libcSymbols.put("feof", new Integer(createFeofStub()));
        
        // === SISTEMA ===
        libcSymbols.put("exit", new Integer(createExitStub()));
        libcSymbols.put("_exit", new Integer(createExitStub()));
        libcSymbols.put("atexit", new Integer(createAtexitStub()));
        
        // === AMBIENTE ===
        libcSymbols.put("getenv", new Integer(createGetenvStub()));
        libcSymbols.put("setenv", new Integer(createSetenvStub()));
        libcSymbols.put("putenv", new Integer(createPutenvStub()));
        
        // === TEMPO ===
        libcSymbols.put("time", new Integer(createTimeStub()));
        libcSymbols.put("localtime", new Integer(createLocaltimeStub()));
        libcSymbols.put("gmtime", new Integer(createGmtimeStub()));
        libcSymbols.put("strftime", new Integer(createStrftimeStub()));
        
        // === RANDOM ===
        libcSymbols.put("rand", new Integer(createRandStub()));
        libcSymbols.put("srand", new Integer(createSrandStub()));
        
        // === MATH ===
        libcSymbols.put("sin", new Integer(createSinStub()));
        libcSymbols.put("cos", new Integer(createCosStub()));
        libcSymbols.put("tan", new Integer(createTanStub()));
        libcSymbols.put("asin", new Integer(createAsinStub()));
        libcSymbols.put("acos", new Integer(createAcosStub()));
        libcSymbols.put("atan", new Integer(createAtanStub()));
        libcSymbols.put("atan2", new Integer(createAtan2Stub()));
        libcSymbols.put("sqrt", new Integer(createSqrtStub()));
        libcSymbols.put("pow", new Integer(createPowStub()));
        libcSymbols.put("exp", new Integer(createExpStub()));
        libcSymbols.put("log", new Integer(createLogStub()));
        libcSymbols.put("log10", new Integer(createLog10Stub()));
        libcSymbols.put("floor", new Integer(createFloorStub()));
        libcSymbols.put("ceil", new Integer(createCeilStub()));
        libcSymbols.put("fabs", new Integer(createFabsStub()));*/
        
        // === ESPECIAL ===
        libcSymbols.put("__libc_start_main", new Integer(createLibcStartMainStub()));
        /*libcSymbols.put("__errno_location", new Integer(createErrnoLocationStub()));
        libcSymbols.put("abort", new Integer(createAbortStub()));
        libcSymbols.put("abs", new Integer(createAbsStub()));
        libcSymbols.put("labs", new Integer(createLabsStub()));
        libcSymbols.put("qsort", new Integer(createQsortStub()));
        libcSymbols.put("bsearch", new Integer(createBsearchStub()));*/
        
        // Adicionar à tabela global
        globalSymbols.put("libc.so.6", libcSymbols);
        loadedLibraries.addElement("libc.so.6");
        
        // Criar ld-linux.so.3 simulado também
        Hashtable ldLinux = new Hashtable();
        /*ldLinux.put("_dl_start", new Integer(createDlStartStub()));
        ldLinux.put("_dl_sym", new Integer(createDlSymStub()));
        ldLinux.put("_dl_error", new Integer(createDlErrorStub()));*/
        globalSymbols.put("ld-linux.so.3", ldLinux);
        loadedLibraries.addElement("ld-linux.so.3");
        
        if (midlet.debug) {
            midlet.print("Libc emulation initialized with " + libcSymbols.size() + " functions", stdout);
        }
    }
    // Stubs
    // |
    // === I/O FUNCTIONS ===
    private int createPrintfStub() {
        int stubAddr = allocateStubMemory(256);
        if (stubAddr == 0) return 0;
        
        // printf(const char *format, ...)
        // Vamos implementar um printf simples que suporta:
        // %s, %d, %c, %x, %%
        
        // ARM assembly para chamar nossa função Java
        // stmfd sp!, {r0-r3, lr}  // Salvar argumentos e return address
        writeIntLE(memory, stubAddr, 0xE92D400F);
        
        // ldr r0, [sp, #16]  // format string (está na stack após r0-r3,lr)
        writeIntLE(memory, stubAddr + 4, 0xE59D0010);
        
        // add r1, sp, #20  // ponteiro para outros args
        writeIntLE(memory, stubAddr + 8, 0xE28D1014);
        
        // bl printf_impl
        writeIntLE(memory, stubAddr + 12, 0xEB000000);
        
        // ldmfd sp!, {r0-r3, pc}  // Restaurar e retornar
        writeIntLE(memory, stubAddr + 16, 0xE8BD800F);
        
        // Armazenar handler
        elfInfo.put("printf_handler@" + stubAddr, new Object() {
            public int handle(int formatPtr, int argsPtr) {
                return handlePrintf(formatPtr, argsPtr);
            }
        });
        
        return stubAddr;
    }

    private int handlePrintf(int formatPtr, int argsPtr) {
        try {
            // Ler string de formato
            String format = readCString(formatPtr);
            if (format == null) return -1;
            
            StringBuffer result = new StringBuffer();
            int argIndex = 0;
            int written = 0;
            
            for (int i = 0; i < format.length(); i++) {
                char c = format.charAt(i);
                
                if (c == '%' && i + 1 < format.length()) {
                    char specifier = format.charAt(i + 1);
                    i++;
                    
                    switch (specifier) {
                        case 's': // string
                            int strPtr = readIntLE(memory, argsPtr + argIndex * 4);
                            String str = readCString(strPtr);
                            if (str != null) {
                                result.append(str);
                                written += str.length();
                            }
                            argIndex++;
                            break;
                            
                        case 'd': // decimal
                        case 'i':
                            int intValue = readIntLE(memory, argsPtr + argIndex * 4);
                            String intStr = Integer.toString(intValue);
                            result.append(intStr);
                            written += intStr.length();
                            argIndex++;
                            break;
                            
                        case 'x': // hex
                        case 'X':
                            int hexValue = readIntLE(memory, argsPtr + argIndex * 4);
                            String hexStr = Integer.toHexString(hexValue);
                            if (specifier == 'X') hexStr = hexStr.toUpperCase();
                            result.append(hexStr);
                            written += hexStr.length();
                            argIndex++;
                            break;
                            
                        case 'c': // char
                            int charValue = readIntLE(memory, argsPtr + argIndex * 4) & 0xFF;
                            result.append((char)charValue);
                            written++;
                            argIndex++;
                            break;
                            
                        case '%': // literal %
                            result.append('%');
                            written++;
                            break;
                            
                        default:
                            result.append('%').append(specifier);
                            written += 2;
                            break;
                    }
                } else {
                    result.append(c);
                    written++;
                }
            }
            
            // Escrever no stdout do OpenTTY
            midlet.print(result.toString(), stdout, id);
            
            return written;
            
        } catch (Exception e) {
            return -1;
        }
    }

    private int createPutsStub() {
        int stubAddr = allocateStubMemory(64);
        if (stubAddr == 0) return 0;
        
        // puts(const char *s)
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // bl puts_impl
        writeIntLE(memory, stubAddr + 4, 0xEB000000);
        
        // mov r0, #0  // sempre retorna não-negativo
        writeIntLE(memory, stubAddr + 8, 0xE3A00000);
        
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 12, 0xE8BD8000);
        
        elfInfo.put("puts_handler@" + stubAddr, new Object() {
            public void handle(int strPtr) {
                handlePuts(strPtr);
            }
        });
        
        return stubAddr;
    }

    private void handlePuts(int strPtr) {
        try {
            String str = readCString(strPtr);
            if (str != null) {
                midlet.print(str, stdout, id);
            }
        } catch (Exception e) {
            // Ignorar erros
        }
    }

    // === MEMORY MANAGEMENT ===
    private int createMallocStub() {
        int stubAddr = allocateStubMemory(128);
        if (stubAddr == 0) return 0;
        
        // malloc(size_t size) - implementação real com heap
        // stmfd sp!, {r4, lr}
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // mov r4, r0  // salvar tamanho
        writeIntLE(memory, stubAddr + 4, 0xE1A04000);
        
        // Verificar se há memória suficiente
        // ldr r0, =heap_end
        writeIntLE(memory, stubAddr + 8, 0xE59F0020);
        
        // add r0, r0, r4  // novo heap_end
        writeIntLE(memory, stubAddr + 12, 0xE0800004);
        
        // ldr r1, =memory_size
        writeIntLE(memory, stubAddr + 16, 0xE59F1018);
        
        // cmp r0, r1
        writeIntLE(memory, stubAddr + 20, 0xE1500001);
        
        // bhi out_of_memory
        writeIntLE(memory, stubAddr + 24, 0x8A000004);
        
        // ldr r0, =heap_end  // endereço atual
        writeIntLE(memory, stubAddr + 28, 0xE59F0010);
        
        // ldr r1, =heap_end
        writeIntLE(memory, stubAddr + 32, 0xE59F100C);
        
        // add r1, r1, r4  // novo heap_end
        writeIntLE(memory, stubAddr + 36, 0xE0811004);
        
        // str r1, =heap_end  // atualizar
        writeIntLE(memory, stubAddr + 40, 0xE58F1000);
        
        // ldmfd sp!, {r4, pc}  // retornar
        writeIntLE(memory, stubAddr + 44, 0xE8BD8010);
        
        // out_of_memory:
        writeIntLE(memory, stubAddr + 48, 0xE3A00000); // mov r0, #0
        writeIntLE(memory, stubAddr + 52, 0xE8BD8010); // ldmfd sp!, {r4, pc}
        
        // Dados embutidos
        writeIntLE(memory, stubAddr + 56, heapEnd);  // heap_end
        writeIntLE(memory, stubAddr + 60, memory.length);  // memory_size
        writeIntLE(memory, stubAddr + 64, heapEnd);  // heap_end (novamente)
        
        return stubAddr;
    }

    // === STRING FUNCTIONS (com implementação real) ===
    private int createStrlenStub() {
        int stubAddr = allocateStubMemory(48);
        if (stubAddr == 0) return 0;
        
        // strlen(const char *s) - implementação otimizada
        // mov r1, r0
        writeIntLE(memory, stubAddr, 0xE1A01000);
        
        // ands r2, r0, #3  // verificar alinhamento
        writeIntLE(memory, stubAddr + 4, 0xE2102003);
        
        // beq aligned
        writeIntLE(memory, stubAddr + 8, 0x0A000005);
        
        // misaligned: ldrb r3, [r1], #1
        writeIntLE(memory, stubAddr + 12, 0xE4D13001);
        
        // cmp r3, #0
        writeIntLE(memory, stubAddr + 16, 0xE3530000);
        
        // bxeq lr
        writeIntLE(memory, stubAddr + 20, 0x012FFF1E);
        
        // subs r2, r2, #1
        writeIntLE(memory, stubAddr + 24, 0xE2522001);
        
        // bne misaligned
        writeIntLE(memory, stubAddr + 28, 0x1AFFFFF9);
        
        // aligned: ldr r3, [r1], #4
        writeIntLE(memory, stubAddr + 32, 0xE4913004);
        
        // sub r2, r3, #0x01010101
        writeIntLE(memory, stubAddr + 36, 0xE2422201);
        
        // bic r2, r2, r3
        writeIntLE(memory, stubAddr + 40, 0xE1C22003);
        
        // ands r2, r2, #0x80808080
        writeIntLE(memory, stubAddr + 44, 0xE2122C01);
        
        // beq aligned
        writeIntLE(memory, stubAddr + 48, 0x0AFFFFF8);
        
        // sub r1, r1, #4
        writeIntLE(memory, stubAddr + 52, 0xE2411004);
        
        // find_null: tst r3, #0xFF
        writeIntLE(memory, stubAddr + 56, 0xE31300FF);
        
        // subeq r1, r1, #3
        writeIntLE(memory, stubAddr + 60, 0x02411003);
        
        // bxeq lr
        writeIntLE(memory, stubAddr + 64, 0x012FFF1E);
        
        // tst r3, #0xFF00
        writeIntLE(memory, stubAddr + 68, 0xE31308FF);
        
        // subeq r1, r1, #2
        writeIntLE(memory, stubAddr + 72, 0x02411002);
        
        // bxeq lr
        writeIntLE(memory, stubAddr + 76, 0x012FFF1E);
        
        // tst r3, #0xFF0000
        writeIntLE(memory, stubAddr + 80, 0xE31304FF);
        
        // subeq r1, r1, #1
        writeIntLE(memory, stubAddr + 84, 0x02411001);
        
        // sub r0, r1, r0
        writeIntLE(memory, stubAddr + 88, 0xE0400001);
        
        // bx lr
        writeIntLE(memory, stubAddr + 92, 0xE12FFF1E);
        
        return stubAddr;
    }

    // === SYSTEM FUNCTIONS ===
    private int createLibcStartMainStub() {
        int stubAddr = allocateStubMemory(96);
        if (stubAddr == 0) return 0;
        
        // __libc_start_main - ponto de entrada para programas C
        // Parâmetros: main, argc, argv, init, fini, rtld_fini, stack_end
        
        // stmfd sp!, {r4-r6, lr}
        writeIntLE(memory, stubAddr, 0xE92D4070);
        
        // salvar parâmetros
        // mov r4, r0  // main
        writeIntLE(memory, stubAddr + 4, 0xE1A04000);
        // mov r5, r1  // argc
        writeIntLE(memory, stubAddr + 8, 0xE1A05001);
        // mov r6, r2  // argv
        writeIntLE(memory, stubAddr + 12, 0xE1A06002);
        
        // Chamar init se não for null
        // cmp r3, #0  // init
        writeIntLE(memory, stubAddr + 16, 0xE3530000);
        // beq skip_init
        writeIntLE(memory, stubAddr + 20, 0x0A000001);
        // blx r3
        writeIntLE(memory, stubAddr + 24, 0xE12FFF33);
        
        // skip_init:
        // Configurar ambiente para main
        // mov r0, r5  // argc
        writeIntLE(memory, stubAddr + 28, 0xE1A00005);
        // mov r1, r6  // argv
        writeIntLE(memory, stubAddr + 32, 0xE1A01006);
        // ldr r2, [sp, #16]  // envp
        writeIntLE(memory, stubAddr + 36, 0xE59D2010);
        
        // Chamar main
        // blx r4
        writeIntLE(memory, stubAddr + 40, 0xE12FFF34);
        
        // Passar resultado para exit
        // mov r7, #SYS_EXIT
        writeIntLE(memory, stubAddr + 44, 0xE3A07001);
        // swi 0
        writeIntLE(memory, stubAddr + 48, 0xEF000000);
        
        // Nunca alcançado
        // ldmfd sp!, {r4-r6, pc}
        writeIntLE(memory, stubAddr + 52, 0xE8BD8070);
        
        return stubAddr;
    }

    // === HELPER FUNCTIONS ===
    private String readCString(int ptr) {
        if (ptr < 0 || ptr >= memory.length) return null;
        
        StringBuffer sb = new StringBuffer();
        for (int i = 0; ptr + i < memory.length && i < 1024; i++) {
            byte b = memory[ptr + i];
            if (b == 0) break;
            sb.append((char)(b & 0xFF));
        }
        return sb.toString();
    }
    private int writeCString(int ptr, String str) {
        if (ptr < 0 || ptr + str.length() >= memory.length) return -1;
        
        byte[] bytes = str.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            memory[ptr + i] = bytes[i];
        }
        memory[ptr + bytes.length] = 0; // null terminator
        
        return bytes.length;
    }
    
    
    private Integer resolveSymbol(String name) {
        if (name == null) return null;
        
        // 1. Verificar na libc em memória
        if (libcSymbols != null && libcSymbols.containsKey(name)) {
            return (Integer) libcSymbols.get(name);
        }
        
        // 2. Verificar em outras bibliotecas simuladas
        for (int i = 0; i < loadedLibraries.size(); i++) {
            String libName = (String) loadedLibraries.elementAt(i);
            Hashtable lib = (Hashtable) globalSymbols.get(libName);
            
            if (lib != null && lib.containsKey(name)) {
                Object symValue = lib.get(name);
                if (symValue instanceof Integer) {
                    return (Integer) symValue;
                }
            }
        }
        
        // 3. Se for syscall, criar stub
        if (isSyscallName(name)) {
            return new Integer(createSyscallStub(name));
        }
        
        // 4. Criar stub genérico inteligente
        int stubAddr = createGenericStub(name);
        if (stubAddr != 0) {
            // Armazenar para reuso
            if (globalSymbols.containsKey("unknown")) {
                Hashtable unknown = (Hashtable) globalSymbols.get("unknown");
                unknown.put(name, new Integer(stubAddr));
            } else {
                Hashtable unknown = new Hashtable();
                unknown.put(name, new Integer(stubAddr));
                globalSymbols.put("unknown", unknown);
                loadedLibraries.addElement("unknown");
            }
            
            if (midlet.debug) {
                midlet.print("Created generic stub for unknown symbol: " + name + 
                        " at " + toHex(stubAddr), stdout);
            }
            
            return new Integer(stubAddr);
        }
        
        // 5. Último recurso: stub que apenas retorna
        int fallbackAddr = createSimpleStub(0);
        if (fallbackAddr != 0) {
            return new Integer(fallbackAddr);
        }
        
        return null;
    }
    private boolean isSyscallName(String name) {
        // Lista de funções que são wrappers de syscalls
        String[] syscallWrappers = {
            "open", "close", "read", "write", "lseek",
            "fork", "execve", "wait", "waitpid",
            "pipe", "dup", "dup2",
            "socket", "connect", "bind", "listen", "accept",
            "send", "recv", "sendto", "recvfrom",
            "kill", "signal", "sigaction",
            "mmap", "munmap", "mprotect", "brk",
            "getpid", "getppid", "getuid", "getgid",
            "chdir", "mkdir", "rmdir", "unlink", "link",
            "stat", "fstat", "lstat",
            "ioctl", "fcntl",
            "gettimeofday", "nanosleep", "sleep"
        };
        
        for (int i = 0; i < syscallWrappers.length; i++) {
            if (name.equals(syscallWrappers[i])) {
                return true;
            }
        }
        
        return name.startsWith("sys_");
    }

    private int createSyscallStub(String syscallName) {
        int stubSize = 64;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // Primeiro, mapear nome para número de syscall
        int syscallNum = mapSyscallNameToNumber(syscallName);
        
        if (syscallNum <= 0) {
            // Syscall desconhecida - retornar -ENOSYS
            // mov r0, #-38 (ENOSYS = 38)
            writeIntLE(memory, stubAddr, 0xE3E00025); // 0xFFFFFFDA = -38
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
            return stubAddr;
        }
        
        // Stub genérico para syscall ARM EABI
        // Parâmetros: r7 = syscall number, r0-r6 = args
        
        // Salvar registradores que podem ser modificados
        // stmfd sp!, {r4, lr}  // Salvar r4 e return address
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // Configurar número da syscall em r7
        if (syscallNum <= 255) {
            // mov r7, #syscallNum
            writeIntLE(memory, stubAddr + 4, 0xE3A07000 | (syscallNum & 0xFF));
        } else {
            // Para números maiores que 255
            // movw r7, #(syscallNum & 0xFFFF)
            int movwInstr = 0xE3007000 | ((syscallNum & 0xF000) << 4) | (syscallNum & 0x0FFF);
            writeIntLE(memory, stubAddr + 4, movwInstr);
            
            // Se necessário, movt para bits altos (ARMv7)
            if (syscallNum > 0xFFFF) {
                // movt r7, #(syscallNum >> 16)
                int movtInstr = 0xE3407000 | (((syscallNum >> 12) & 0xF000) << 4) | ((syscallNum >> 16) & 0x0FFF);
                writeIntLE(memory, stubAddr + 8, movtInstr);
            }
        }
        
        // Chamar syscall (swi 0)
        int swiOffset = (syscallNum <= 255) ? 8 : (syscallNum <= 0xFFFF ? 12 : 16);
        writeIntLE(memory, stubAddr + swiOffset, 0xEF000000);
        
        // Restaurar registradores e retornar
        // ldmfd sp!, {r4, pc}
        writeIntLE(memory, stubAddr + swiOffset + 4, 0xE8BD8010);
        
        return stubAddr;
    }

    // Mapeamento completo de syscalls ARM EABI
    private int mapSyscallNameToNumber(String name) {
        // Converter para minúsculas para comparação insensível a caso
        String lowerName = name.toLowerCase().trim();
        
        // Mapeamento direto de funções libc para syscalls
        if (lowerName.equals("exit") || lowerName.equals("_exit")) return SYS_EXIT;
        if (lowerName.equals("fork")) return SYS_FORK;
        if (lowerName.equals("read")) return SYS_READ;
        if (lowerName.equals("write")) return SYS_WRITE;
        if (lowerName.equals("open")) return SYS_OPEN;
        if (lowerName.equals("close")) return SYS_CLOSE;
        if (lowerName.equals("creat")) return SYS_CREAT;
        if (lowerName.equals("unlink")) return SYS_UNLINK;
        if (lowerName.equals("execve") || lowerName.equals("exec")) return SYS_EXECVE;
        if (lowerName.equals("chdir")) return SYS_CHDIR;
        if (lowerName.equals("time") || lowerName.equals("gettimeofday")) return SYS_TIME;
        if (lowerName.equals("lseek")) return SYS_LSEEK;
        if (lowerName.equals("getpid")) return SYS_GETPID;
        if (lowerName.equals("mkdir")) return SYS_MKDIR;
        if (lowerName.equals("rmdir")) return SYS_RMDIR;
        if (lowerName.equals("dup")) return SYS_DUP;
        if (lowerName.equals("dup2")) return SYS_DUP2;
        if (lowerName.equals("getppid")) return SYS_GETPPID;
        if (lowerName.equals("ioctl")) return SYS_IOCTL;
        if (lowerName.equals("kill")) return SYS_KILL;
        if (lowerName.equals("brk")) return SYS_BRK;
        if (lowerName.equals("getcwd")) return SYS_GETCWD;
        if (lowerName.equals("getuid")) return SYS_GETUID32;
        if (lowerName.equals("geteuid")) return SYS_GETEUID32;
        if (lowerName.equals("stat")) return SYS_STAT;
        if (lowerName.equals("fstat")) return SYS_FSTAT;
        if (lowerName.equals("getpriority")) return SYS_GETPRIORITY;
        if (lowerName.equals("setpriority")) return SYS_SETPRIORITY;
        if (lowerName.equals("getdents")) return SYS_GETDENTS;
        if (lowerName.equals("socket")) return SYS_SOCKET;
        if (lowerName.equals("bind")) return SYS_BIND;
        if (lowerName.equals("connect")) return SYS_CONNECT;
        if (lowerName.equals("listen")) return SYS_LISTEN;
        if (lowerName.equals("accept")) return SYS_ACCEPT;
        if (lowerName.equals("send")) return SYS_SEND;
        if (lowerName.equals("recv")) return SYS_RECV;
        if (lowerName.equals("sendto")) return SYS_SENDTO;
        if (lowerName.equals("recvfrom")) return SYS_RECVFROM;
        if (lowerName.equals("shutdown")) return SYS_SHUTDOWN;
        if (lowerName.equals("setsockopt")) return SYS_SETSOCKOPT;
        if (lowerName.equals("getsockopt")) return SYS_GETSOCKOPT;
        if (lowerName.equals("getsockname")) return SYS_GETSOCKNAME;
        if (lowerName.equals("getpeername")) return SYS_GETPEERNAME;
        if (lowerName.equals("signal")) return SYS_SIGNAL;
        if (lowerName.equals("sigaction")) return SYS_SIGACTION;
        if (lowerName.equals("sigprocmask")) return SYS_SIGPROCMASK;
        if (lowerName.equals("sigreturn")) return SYS_SIGRETURN;
        if (lowerName.equals("setjmp")) return SYS_SETJMP;
        if (lowerName.equals("longjmp")) return SYS_LONGJMP;
        if (lowerName.equals("gettid")) return SYS_GETTID;
        if (lowerName.equals("nanosleep") || lowerName.equals("sleep")) return SYS_NANOSLEEP;
        if (lowerName.equals("pipe")) return SYS_PIPE;
        if (lowerName.equals("select")) return SYS_SELECT;
        if (lowerName.equals("poll")) return SYS_POLL;
        if (lowerName.equals("fsync")) return SYS_FSYNC;
        if (lowerName.equals("mmap")) return SYS_MMAP;
        if (lowerName.equals("munmap")) return SYS_MUNMAP;
        if (lowerName.equals("mprotect")) return SYS_MPROTECT;
        if (lowerName.equals("mremap")) return SYS_MREMAP;
        if (lowerName.equals("futex")) return SYS_FUTEX;
        if (lowerName.equals("sched_yield")) return SYS_SCHED_YIELD;
        if (lowerName.equals("uname")) return SYS_UNAME;
        if (lowerName.equals("fcntl")) return SYS_FCNTL;
        if (lowerName.equals("ftruncate")) return SYS_FTRUNCATE;
        if (lowerName.equals("truncate")) return SYS_TRUNCATE;
        if (lowerName.equals("getrlimit")) return SYS_GETRLIMIT;
        if (lowerName.equals("syscall")) return SYS_SYSCALL; // syscall() genérico
        
        // Se o nome já é um número, tentar converter
        try {
            // Remover "sys_" se presente
            if (lowerName.startsWith("sys_")) {
                lowerName = lowerName.substring(4);
            }
            
            // Tentar converter para número
            if (lowerName.startsWith("0x")) {
                return Integer.parseInt(lowerName.substring(2), 16);
            } else {
                return Integer.parseInt(lowerName);
            }
        } catch (NumberFormatException e) {
            // Não é um número
        }
        
        return 0; // Syscall desconhecida
    }

    // Versão alternativa: criar stubs específicos para syscalls importantes
    private int createSpecificSyscallStub(String syscallName, int syscallNum) {
        int stubAddr = allocateStubMemory(128);
        if (stubAddr == 0) return 0;
        
        String lowerName = syscallName.toLowerCase();
        
        if (lowerName.equals("write")) {
            // write(int fd, const void *buf, size_t count)
            return createWriteStub(stubAddr);
        } else if (lowerName.equals("read")) {
            // read(int fd, void *buf, size_t count)
            return createReadStub(stubAddr);
        } else if (lowerName.equals("open")) {
            // open(const char *pathname, int flags, mode_t mode)
            return createOpenStub(stubAddr);
        } else if (lowerName.equals("close")) {
            // close(int fd)
            return createCloseStub(stubAddr);
        } else if (lowerName.equals("exit")) {
            // exit(int status)
            return createExitSyscallStub(stubAddr);
        } else if (lowerName.equals("brk")) {
            // brk(void *addr)
            return createBrkStub(stubAddr);
        } else {
            return createGenericSyscallStub(stubAddr, syscallNum);
        }
    }

    private int createWriteStub(int stubAddr) {
        // write(int fd, const void *buf, size_t count)
        // stmfd sp!, {r4-r5, lr}
        writeIntLE(memory, stubAddr, 0xE92D4030);
        
        // Salvar parâmetros
        // mov r4, r1  // buf
        writeIntLE(memory, stubAddr + 4, 0xE1A04001);
        // mov r5, r2  // count
        writeIntLE(memory, stubAddr + 8, 0xE1A05002);
        
        // Verificar se é stdout/stderr (fd = 1 ou 2)
        // cmp r0, #1
        writeIntLE(memory, stubAddr + 12, 0xE3500001);
        // cmpne r0, #2
        writeIntLE(memory, stubAddr + 16, 0x13500002);
        // bne generic_write
        writeIntLE(memory, stubAddr + 20, 0x1A000008);
        
        // stdout/stderr: escrever no OpenTTY
        // ldr r0, =stdout_object
        writeIntLE(memory, stubAddr + 24, 0xE59F0020);
        // mov r1, r4  // buf
        writeIntLE(memory, stubAddr + 28, 0xE1A01004);
        // mov r2, r5  // count
        writeIntLE(memory, stubAddr + 32, 0xE1A02005);
        
        // Chamar função Java para imprimir
        // bl print_buffer
        writeIntLE(memory, stubAddr + 36, 0xEB000000);
        
        // mov r0, r5  // retornar count
        writeIntLE(memory, stubAddr + 40, 0xE1A00005);
        // b done
        writeIntLE(memory, stubAddr + 44, 0xEA000007);
        
        // generic_write:
        // Para outros file descriptors, usar syscall normal
        // mov r7, #SYS_WRITE
        writeIntLE(memory, stubAddr + 48, 0xE3A07004);
        // swi 0
        writeIntLE(memory, stubAddr + 52, 0xEF000000);
        
        // done:
        // ldmfd sp!, {r4-r5, pc}
        writeIntLE(memory, stubAddr + 56, 0xE8BD8030);
        
        // Dados
        // stdout_object: .word stdout
        writeIntLE(memory, stubAddr + 60, 0x00000000); // Será preenchido em runtime
        
        // Armazenar referência ao stdout
        elfInfo.put("write_stdout@" + stubAddr, stdout);
        
        return stubAddr;
    }

    private int createReadStub(int stubAddr) {
        // read(int fd, void *buf, size_t count)
        // Para stdin (fd=0), podemos implementar entrada do OpenTTY
        
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // Verificar se é stdin (fd = 0)
        // cmp r0, #0
        writeIntLE(memory, stubAddr + 4, 0xE3500000);
        // bne generic_read
        writeIntLE(memory, stubAddr + 8, 0x1A000004);
        
        // stdin: não implementado por enquanto, retornar 0
        // mov r0, #0
        writeIntLE(memory, stubAddr + 12, 0xE3A00000);
        // b done
        writeIntLE(memory, stubAddr + 16, 0xEA000003);
        
        // generic_read:
        // mov r7, #SYS_READ
        writeIntLE(memory, stubAddr + 20, 0xE3A07003);
        // swi 0
        writeIntLE(memory, stubAddr + 24, 0xEF000000);
        
        // done:
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 28, 0xE8BD8000);
        
        return stubAddr;
    }

    private int createExitSyscallStub(int stubAddr) {
        // exit(int status)
        // Esta syscall não retorna
        
        // mov r7, #SYS_EXIT
        writeIntLE(memory, stubAddr, 0xE3A07001);
        // swi 0
        writeIntLE(memory, stubAddr + 4, 0xEF000000);
        
        // Loop infinito (nunca alcançado, mas por segurança)
        // b . (branch to self)
        writeIntLE(memory, stubAddr + 8, 0xEAFFFFFE);
        
        return stubAddr;
    }

    private int createBrkStub(int stubAddr) {
        // brk(void *addr) - gerenciador de heap
        
        // stmfd sp!, {r4, lr}
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // Se addr == 0, retornar heap atual
        // cmp r0, #0
        writeIntLE(memory, stubAddr + 4, 0xE3500000);
        // bne set_brk
        writeIntLE(memory, stubAddr + 8, 0x1A000004);
        
        // get_brk:
        // ldr r0, =heap_end
        writeIntLE(memory, stubAddr + 12, 0xE59F0020);
        // b done
        writeIntLE(memory, stubAddr + 16, 0xEA000009);
        
        // set_brk:
        // mov r4, r0  // salvar novo endereço
        writeIntLE(memory, stubAddr + 20, 0xE1A04000);
        
        // Verificar se é válido
        // cmp r4, #0x200000  // mínimo 2MB
        writeIntLE(memory, stubAddr + 24, 0xE3540B02);
        // blo error
        writeIntLE(memory, stubAddr + 28, 0x3A000002);
        
        // ldr r0, =memory_size
        writeIntLE(memory, stubAddr + 32, 0xE59F000C);
        // cmp r4, r0
        writeIntLE(memory, stubAddr + 36, 0xE1540000);
        // bhi error
        writeIntLE(memory, stubAddr + 40, 0x8A000001);
        
        // Atualizar heap_end
        // str r4, =heap_end
        writeIntLE(memory, stubAddr + 44, 0xE58F4004);
        // mov r0, r4  // retornar novo brk
        writeIntLE(memory, stubAddr + 48, 0xE1A00004);
        // b done
        writeIntLE(memory, stubAddr + 52, 0xEA000001);
        
        // error:
        // ldr r0, =heap_end  // retornar brk atual
        writeIntLE(memory, stubAddr + 56, 0xE59F0004);
        
        // done:
        // ldmfd sp!, {r4, pc}
        writeIntLE(memory, stubAddr + 60, 0xE8BD8010);
        
        // Dados
        writeIntLE(memory, stubAddr + 64, heapEnd);      // heap_end
        writeIntLE(memory, stubAddr + 68, memory.length); // memory_size
        writeIntLE(memory, stubAddr + 72, heapEnd);      // heap_end (para store)
        
        return stubAddr;
    }

    private int createGenericSyscallStub(int stubAddr, int syscallNum) {
        // Stub genérico que apenas chama a syscall
        
        // Salvar lr se necessário
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // Configurar número da syscall
        if (syscallNum <= 255) {
            // mov r7, #syscallNum
            writeIntLE(memory, stubAddr + 4, 0xE3A07000 | (syscallNum & 0xFF));
        } else {
            // movw r7, #(syscallNum & 0xFFFF)
            int movwInstr = 0xE3007000 | ((syscallNum & 0xF000) << 4) | (syscallNum & 0x0FFF);
            writeIntLE(memory, stubAddr + 4, movwInstr);
        }
        
        // swi 0
        int swiOffset = (syscallNum <= 255) ? 8 : 12;
        writeIntLE(memory, stubAddr + swiOffset, 0xEF000000);
        
        // Restaurar e retornar
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + swiOffset + 4, 0xE8BD8000);
        
        return stubAddr;
    }

    // Para syscalls que queremos interceptar e implementar em Java
    private int createInterceptedSyscallStub(String syscallName) {
        int stubAddr = allocateStubMemory(96);
        if (stubAddr == 0) return 0;
        
        // Stub que chama nossa implementação Java
        // stmfd sp!, {r0-r3, lr}  // Salvar argumentos
        writeIntLE(memory, stubAddr, 0xE92D400F);
        
        // Preparar chamada para handler Java
        // ldr r0, =handler_address
        writeIntLE(memory, stubAddr + 4, 0xE59F0020);
        
        // ldr r1, =syscall_name
        writeIntLE(memory, stubAddr + 8, 0xE59F1020);
        
        // add r2, sp, #0  // ponteiro para argumentos
        writeIntLE(memory, stubAddr + 12, 0xE28D2000);
        
        // bl syscall_handler
        writeIntLE(memory, stubAddr + 16, 0xEB000000);
        
        // ldmfd sp!, {r0-r3, pc}  // Restaurar e retornar (r0 tem retorno)
        writeIntLE(memory, stubAddr + 20, 0xE8BD800F);
        
        // Dados
        writeIntLE(memory, stubAddr + 24, 0xDEADBEEF); // handler_address placeholder
        // Nome da syscall como string C
        byte[] nameBytes = syscallName.getBytes();
        for (int i = 0; i < nameBytes.length && i < 32; i++) {
            memory[stubAddr + 28 + i] = nameBytes[i];
        }
        memory[stubAddr + 28 + nameBytes.length] = 0;
        
        // Registrar handler
        elfInfo.put("syscall_handler@" + syscallName, new Object() {
            public int handle(int[] args) {
                return handleSyscallIntercept(syscallName, args);
            }
        });
        
        return stubAddr;
    }

    private int handleSyscallIntercept(String syscallName, int[] args) {
        // Implementação Java da syscall
        switch (syscallName) {
            case "write":
                if (args[0] == 1 || args[0] == 2) { // stdout/stderr
                    return handleWriteToStdout(args[1], args[2]);
                }
                break;
            case "read":
                if (args[0] == 0) { // stdin
                    return handleReadFromStdin(args[1], args[2]);
                }
                break;
            case "brk":
                return handleBrk(args[0]);
            case "exit":
                handleExit(args[0]);
                return 0;
        }
        
        // Para outras syscalls, usar implementação padrão
        return -38; // ENOSYS
    }

    private int createPutcharStub() {
        int stubSize = 64;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // putchar(int c) - escreve um caractere no stdout
        
        // stmfd sp!, {r4, lr}  // Salvar r4 e return address
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // Salvar caractere em r4 (r0 contém o caractere)
        // mov r4, r0
        writeIntLE(memory, stubAddr + 4, 0xE1A04000);
        
        // Verificar se precisamos fazer flush de buffer
        // ldr r0, =stdout_buffer
        writeIntLE(memory, stubAddr + 8, 0xE59F0040);
        // ldr r1, =stdout_buffer_index
        writeIntLE(memory, stubAddr + 12, 0xE59F1040);
        // ldr r1, [r1]
        writeIntLE(memory, stubAddr + 16, 0xE5911000);
        
        // strb r4, [r0, r1]  // buffer[index] = c
        writeIntLE(memory, stubAddr + 20, 0xE7C04001);
        
        // add r1, r1, #1  // index++
        writeIntLE(memory, stubAddr + 24, 0xE2811001);
        
        // ldr r0, =stdout_buffer_index
        writeIntLE(memory, stubAddr + 28, 0xE59F0024);
        // str r1, [r0]  // salvar novo índice
        writeIntLE(memory, stubAddr + 32, 0xE5801000);
        
        // Verificar se buffer está cheio ou se é newline
        // cmp r1, #1024  // tamanho do buffer
        writeIntLE(memory, stubAddr + 36, 0xE3510C01);
        // cmpne r4, #10  // '\n'
        writeIntLE(memory, stubAddr + 40, 0x1354000A);
        // bne no_flush
        writeIntLE(memory, stubAddr + 44, 0x1A000004);
        
        // Fazer flush do buffer
        // ldr r0, =stdout_buffer
        writeIntLE(memory, stubAddr + 48, 0xE59F0020);
        // mov r1, r1  // count (já está em r1)
        // bl flush_buffer
        writeIntLE(memory, stubAddr + 52, 0xEB000000);
        
        // Resetar índice do buffer
        // ldr r0, =stdout_buffer_index
        writeIntLE(memory, stubAddr + 56, 0xE59F000C);
        // mov r1, #0
        writeIntLE(memory, stubAddr + 60, 0xE3A01000);
        // str r1, [r0]
        writeIntLE(memory, stubAddr + 64, 0xE5801000);
        
        // no_flush:
        // Retornar o caractere (conforme padrão C)
        // mov r0, r4
        writeIntLE(memory, stubAddr + 68, 0xE1A00004);
        
        // ldmfd sp!, {r4, pc}
        writeIntLE(memory, stubAddr + 72, 0xE8BD8010);
        
        // Dados
        // Endereço do buffer stdout
        int bufferAddr = allocateStubMemory(1024);
        writeIntLE(memory, stubAddr + 76, bufferAddr);
        
        // Variável para índice do buffer
        int indexAddr = bufferAddr + 1024;
        writeIntLE(memory, stubAddr + 80, indexAddr);
        
        // Inicializar índice como 0
        writeIntLE(memory, indexAddr, 0);
        
        // Armazenar informações para flush
        elfInfo.put("putchar_buffer", new Integer(bufferAddr));
        elfInfo.put("putchar_index", new Integer(indexAddr));
        
        // Handler para flush do buffer
        elfInfo.put("flush_buffer_handler", new Object() {
            public void handle(int bufferPtr, int count) {
                flushStdoutBuffer(bufferPtr, count);
            }
        });
        
        return stubAddr;
    }

    private void flushStdoutBuffer(int bufferPtr, int count) {
        if (count <= 0 || bufferPtr <= 0) return;
        
        try {
            // Ler dados do buffer
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < count && bufferPtr + i < memory.length; i++) {
                byte b = memory[bufferPtr + i];
                sb.append((char)(b & 0xFF));
            }
            
            // Escrever no OpenTTY stdout
            if (sb.length() > 0) {
                midlet.print(sb.toString(), stdout, id);
            }
            
            // Limpar buffer
            for (int i = 0; i < count && bufferPtr + i < memory.length; i++) {
                memory[bufferPtr + i] = 0;
            }
            
        } catch (Exception e) {
            // Ignorar erros
        }
    }

    // Versão simplificada de putchar (sem buffer)
    private int createPutcharSimpleStub() {
        int stubSize = 32;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // putchar(int c) - versão simples sem buffering
        
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // Preparar para write syscall
        // mov r7, #SYS_WRITE (4)
        writeIntLE(memory, stubAddr + 4, 0xE3A07004);
        
        // mov r0, #1 (stdout)
        writeIntLE(memory, stubAddr + 8, 0xE3A00001);
        
        // Criar buffer de 1 caractere na stack
        // sub sp, sp, #4
        writeIntLE(memory, stubAddr + 12, 0xE24DD004);
        
        // strb r0, [sp] (armazenar caractere)
        writeIntLE(memory, stubAddr + 16, 0xE54D0004);
        
        // mov r1, sp (ponteiro para buffer)
        writeIntLE(memory, stubAddr + 20, 0xE1A0100D);
        
        // mov r2, #1 (count = 1)
        writeIntLE(memory, stubAddr + 24, 0xE3A02001);
        
        // swi 0
        writeIntLE(memory, stubAddr + 28, 0xEF000000);
        
        // add sp, sp, #4 (limpar stack)
        writeIntLE(memory, stubAddr + 32, 0xE28DD004);
        
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 36, 0xE8BD8000);
        
        return stubAddr;
    }

    // putchar com implementação direta em Java (mais eficiente)
    private int createPutcharJavaStub() {
        int stubSize = 48;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // putchar(int c) - chama implementação Java
        
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // bl putchar_java_impl
        writeIntLE(memory, stubAddr + 4, 0xEB000000);
        
        // Retornar o caractere (r0 já tem o caractere de entrada)
        // bx lr
        writeIntLE(memory, stubAddr + 8, 0xE12FFF1E);
        
        // Implementação Java embutida no código ARM
        // putchar_java_impl:
        // stmfd sp!, {r1-r3, lr}
        writeIntLE(memory, stubAddr + 12, 0xE92D400E);
        
        // Converter caractere para string
        // sub sp, sp, #8  // espaço para string
        writeIntLE(memory, stubAddr + 16, 0xE24DD008);
        
        // strb r0, [sp]  // armazenar caractere
        writeIntLE(memory, stubAddr + 20, 0xE54D0008);
        
        // mov r1, #0
        writeIntLE(memory, stubAddr + 24, 0xE3A01000);
        // strb r1, [sp, #1]  // null terminator
        writeIntLE(memory, stubAddr + 28, 0xE54D1007);
        
        // Chamar print do OpenTTY
        // ldr r0, =stdout_object
        writeIntLE(memory, stubAddr + 32, 0xE59F0010);
        // mov r1, sp
        writeIntLE(memory, stubAddr + 36, 0xE1A0100D);
        // bl print_string
        writeIntLE(memory, stubAddr + 40, 0xEB000000);
        
        // add sp, sp, #8
        writeIntLE(memory, stubAddr + 44, 0xE28DD008);
        
        // ldmfd sp!, {r1-r3, pc}
        writeIntLE(memory, stubAddr + 48, 0xE8BD800E);
        
        // Dados
        writeIntLE(memory, stubAddr + 52, 0x00000000); // stdout_object placeholder
        
        // Registrar handler
        elfInfo.put("putchar_java_handler", new Object() {
            public void handle(char c) {
                // Escrever caractere diretamente
                midlet.print(String.valueOf(c), stdout, id);
            }
        });
        
        return stubAddr;
    }

    // putchar otimizado para caracteres ASCII
    private int createPutcharOptimizedStub() {
        int stubSize = 128;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // putchar(int c) - otimizado com branch para caracteres especiais
        
        // Verificar se é newline, tab, etc.
        // cmp r0, #10  // '\n'
        writeIntLE(memory, stubAddr, 0xE350000A);
        // beq handle_newline
        writeIntLE(memory, stubAddr + 4, 0x0A000010);
        
        // cmp r0, #13  // '\r'
        writeIntLE(memory, stubAddr + 8, 0xE350000D);
        // beq handle_return
        writeIntLE(memory, stubAddr + 12, 0x0A00000E);
        
        // cmp r0, #9   // '\t'
        writeIntLE(memory, stubAddr + 16, 0xE3500009);
        // beq handle_tab
        writeIntLE(memory, stubAddr + 20, 0x0A00000C);
        
        // cmp r0, #8   // '\b' (backspace)
        writeIntLE(memory, stubAddr + 24, 0xE3500008);
        // beq handle_backspace
        writeIntLE(memory, stubAddr + 28, 0x0A00000A);
        
        // Caractere normal: verificar se é imprimível
        // cmp r0, #32
        writeIntLE(memory, stubAddr + 32, 0xE3500020);
        // blo invalid_char
        writeIntLE(memory, stubAddr + 36, 0x3A000008);
        
        // cmp r0, #126
        writeIntLE(memory, stubAddr + 40, 0xE350007E);
        // bhi invalid_char
        writeIntLE(memory, stubAddr + 44, 0x8A000006);
        
        // Caractere válido: escrever
        // b write_char
        writeIntLE(memory, stubAddr + 48, 0xEA000009);
        
        // handle_newline:
        // Implementação para newline
        // mov r0, #10
        writeIntLE(memory, stubAddr + 52, 0xE3A0000A);
        // b write_char
        writeIntLE(memory, stubAddr + 56, 0xEA000006);
        
        // handle_return:
        // Implementação para carriage return
        // mov r0, #13
        writeIntLE(memory, stubAddr + 60, 0xE3A0000D);
        // b write_char
        writeIntLE(memory, stubAddr + 64, 0xEA000003);
        
        // handle_tab:
        // Tab -> 4 espaços
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr + 68, 0xE92D4000);
        // mov r0, #32  // espaço
        writeIntLE(memory, stubAddr + 72, 0xE3A00020);
        // bl putchar (chamar recursivamente 4 vezes)
        writeIntLE(memory, stubAddr + 76, 0xEBFFFFF0);
        // bl putchar
        writeIntLE(memory, stubAddr + 80, 0xEBFFFFEF);
        // bl putchar
        writeIntLE(memory, stubAddr + 84, 0xEBFFFFEE);
        // bl putchar
        writeIntLE(memory, stubAddr + 88, 0xEBFFFFED);
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 92, 0xE8BD8000);
        
        // handle_backspace:
        // Backspace - mover cursor para trás se possível
        // Implementação simplificada: escrever espaço e voltar
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr + 96, 0xE92D4000);
        // mov r0, #8   // backspace char
        writeIntLE(memory, stubAddr + 100, 0xE3A00008);
        // bl write_char_direct
        writeIntLE(memory, stubAddr + 104, 0xEB000002);
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 108, 0xE8BD8000);
        
        // invalid_char:
        // Caractere inválido - substituir por '?'
        // mov r0, #63  // '?'
        writeIntLE(memory, stubAddr + 112, 0xE3A0003F);
        
        // write_char:
        // Escrever caractere
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr + 116, 0xE92D4000);
        
        // Chamar syscall write
        // mov r7, #SYS_WRITE
        writeIntLE(memory, stubAddr + 120, 0xE3A07004);
        // mov r0, #1  // stdout
        writeIntLE(memory, stubAddr + 124, 0xE3A00001);
        // sub sp, sp, #4
        writeIntLE(memory, stubAddr + 128, 0xE24DD004);
        // strb r0, [sp]  // caractere original ainda em r0? Vamos ajustar
        // Na verdade, precisamos salvar o caractere primeiro
        
        // Vamos reescrever essa parte:
        // Retornar para implementação simples
        return createPutcharSimpleStub();
    }

    private int createGetcharStub() {
        int stubSize = 96;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // getchar() - lê um caractere do stdin
        // Retorna o caractere como unsigned char convertido para int, ou EOF (-1)
        
        // stmfd sp!, {r4, lr}  // Salvar r4 e return address
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // Para stdin (fd=0), temos algumas opções:
        // 1. Usar syscall read (bloqueante)
        // 2. Usar buffer pré-carregado
        // 3. Retornar EOF (-1) por enquanto
        
        // Vamos implementar com buffer em memória compartilhada
        
        // Verificar se há caracteres no buffer
        // ldr r0, =stdin_buffer_index
        writeIntLE(memory, stubAddr + 4, 0xE59F0060);
        // ldr r0, [r0]
        writeIntLE(memory, stubAddr + 8, 0xE5900000);
        
        // ldr r1, =stdin_buffer_count
        writeIntLE(memory, stubAddr + 12, 0xE59F1058);
        // ldr r1, [r1]
        writeIntLE(memory, stubAddr + 16, 0xE5911000);
        
        // cmp r0, r1  // index >= count?
        writeIntLE(memory, stubAddr + 20, 0xE1500001);
        
        // blo buffer_has_data
        writeIntLE(memory, stubAddr + 24, 0x3A00000C);
        
        // Buffer vazio: tentar ler mais dados
        // bl refill_stdin_buffer
        writeIntLE(memory, stubAddr + 28, 0xEB00000C);
        
        // Verificar resultado
        // cmp r0, #0
        writeIntLE(memory, stubAddr + 32, 0xE3500000);
        
        // ble eof  // se <= 0, EOF
        writeIntLE(memory, stubAddr + 36, 0xDA00000D);
        
        // buffer_has_data:
        // Ler próximo caractere do buffer
        // ldr r0, =stdin_buffer
        writeIntLE(memory, stubAddr + 40, 0xE59F0034);
        
        // ldr r1, =stdin_buffer_index
        writeIntLE(memory, stubAddr + 44, 0xE59F1020);
        // ldr r1, [r1]
        writeIntLE(memory, stubAddr + 48, 0xE5911000);
        
        // ldrb r0, [r0, r1]  // buffer[index]
        writeIntLE(memory, stubAddr + 52, 0xE7D00001);
        
        // Incrementar índice
        // add r1, r1, #1
        writeIntLE(memory, stubAddr + 56, 0xE2811001);
        
        // ldr r2, =stdin_buffer_index
        writeIntLE(memory, stubAddr + 60, 0xE59F2004);
        // str r1, [r2]
        writeIntLE(memory, stubAddr + 64, 0xE5821000);
        
        // Retornar caractere (zero-extend para int)
        // and r0, r0, #0xFF
        writeIntLE(memory, stubAddr + 68, 0xE20000FF);
        
        // b done
        writeIntLE(memory, stubAddr + 72, 0xEA000003);
        
        // eof:
        // Retornar EOF (-1)
        // mvn r0, #0  // -1 = 0xFFFFFFFF
        writeIntLE(memory, stubAddr + 76, 0xE3E00000);
        
        // done:
        // ldmfd sp!, {r4, pc}
        writeIntLE(memory, stubAddr + 80, 0xE8BD8010);
        
        // Sub-rotina: refill_stdin_buffer
        // refill_stdin_buffer:
        // stmfd sp!, {r4-r5, lr}
        writeIntLE(memory, stubAddr + 84, 0xE92D4030);
        
        // Chamar syscall read
        // mov r7, #SYS_READ (3)
        writeIntLE(memory, stubAddr + 88, 0xE3A07003);
        // mov r0, #0  // stdin
        writeIntLE(memory, stubAddr + 92, 0xE3A00000);
        // ldr r1, =stdin_buffer
        writeIntLE(memory, stubAddr + 96, 0xE59F1050);
        // mov r2, #256  // tamanho do buffer
        writeIntLE(memory, stubAddr + 100, 0xE3A02C01); // 0x100 = 256
        
        // swi 0
        writeIntLE(memory, stubAddr + 104, 0xEF000000);
        
        // Verificar resultado
        // cmp r0, #0
        writeIntLE(memory, stubAddr + 108, 0xE3500000);
        
        // ble read_error  // se <= 0, erro/EOF
        writeIntLE(memory, stubAddr + 112, 0xDA000006);
        
        // Sucesso: atualizar contador
        // ldr r1, =stdin_buffer_count
        writeIntLE(memory, stubAddr + 116, 0xE59F1034);
        // str r0, [r1]
        writeIntLE(memory, stubAddr + 120, 0xE5810000);
        
        // Resetar índice
        // ldr r1, =stdin_buffer_index
        writeIntLE(memory, stubAddr + 124, 0xE59F100C);
        // mov r2, #0
        writeIntLE(memory, stubAddr + 128, 0xE3A02000);
        // str r2, [r1]
        writeIntLE(memory, stubAddr + 132, 0xE5812000);
        
        // b refill_done
        writeIntLE(memory, stubAddr + 136, 0xEA000002);
        
        // read_error:
        // Configurar contador como 0
        // ldr r1, =stdin_buffer_count
        writeIntLE(memory, stubAddr + 140, 0xE59F100A);
        // mov r2, #0
        writeIntLE(memory, stubAddr + 144, 0xE3A02000);
        // str r2, [r1]
        writeIntLE(memory, stubAddr + 148, 0xE5812000);
        // mov r0, #0  // retornar 0 (EOF)
        writeIntLE(memory, stubAddr + 152, 0xE3A00000);
        
        // refill_done:
        // ldmfd sp!, {r4-r5, pc}
        writeIntLE(memory, stubAddr + 156, 0xE8BD8030);
        
        // Dados
        int bufferAddr = allocateStubMemory(256); // Buffer de 256 bytes
        int indexAddr = bufferAddr + 256;        // Índice atual
        int countAddr = indexAddr + 4;           // Contador total
        
        // Escrever endereços nos locais apropriados
        writeIntLE(memory, stubAddr + 160, indexAddr);   // stdin_buffer_index
        writeIntLE(memory, stubAddr + 164, countAddr);   // stdin_buffer_count
        writeIntLE(memory, stubAddr + 168, bufferAddr);  // stdin_buffer
        writeIntLE(memory, stubAddr + 172, indexAddr);   // stdin_buffer_index (novamente)
        writeIntLE(memory, stubAddr + 176, countAddr);   // stdin_buffer_count (novamente)
        writeIntLE(memory, stubAddr + 180, bufferAddr);  // stdin_buffer (novamente)
        
        // Inicializar variáveis
        writeIntLE(memory, indexAddr, 0);  // índice = 0
        writeIntLE(memory, countAddr, 0);  // contador = 0
        
        // Armazenar referências para uso externo
        elfInfo.put("stdin_buffer", new Integer(bufferAddr));
        elfInfo.put("stdin_index", new Integer(indexAddr));
        elfInfo.put("stdin_count", new Integer(countAddr));
        
        // Para testar, pré-carregar buffer com alguns dados
        String testInput = "Hello from getchar!\n";
        byte[] testBytes = testInput.getBytes();
        for (int i = 0; i < testBytes.length && i < 256; i++) {
            memory[bufferAddr + i] = testBytes[i];
        }
        writeIntLE(memory, countAddr, testBytes.length);
        
        return stubAddr;
    }

    // Versão simplificada (não-bloqueante, com dados pré-definidos)
    private int createGetcharSimpleStub() {
        int stubSize = 64;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // getchar() - versão simplificada com buffer fixo
        
        // stmfd sp!, {r4, lr}
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // Verificar se há dados no buffer
        // ldr r0, =input_buffer
        writeIntLE(memory, stubAddr + 4, 0xE59F004C);
        
        // ldr r1, =input_index
        writeIntLE(memory, stubAddr + 8, 0xE59F104C);
        // ldr r1, [r1]
        writeIntLE(memory, stubAddr + 12, 0xE5911000);
        
        // ldrb r4, [r0, r1]  // ler caractere
        writeIntLE(memory, stubAddr + 16, 0xE7D04001);
        
        // Verificar se é fim dos dados (null terminator)
        // cmp r4, #0
        writeIntLE(memory, stubAddr + 20, 0xE3540000);
        
        // bne has_char
        writeIntLE(memory, stubAddr + 24, 0x1A000006);
        
        // EOF: retornar -1
        // mvn r0, #0  // -1
        writeIntLE(memory, stubAddr + 28, 0xE3E00000);
        // b done
        writeIntLE(memory, stubAddr + 32, 0xEA00000A);
        
        // has_char:
        // Incrementar índice
        // add r1, r1, #1
        writeIntLE(memory, stubAddr + 36, 0xE2811001);
        
        // ldr r0, =input_index
        writeIntLE(memory, stubAddr + 40, 0xE59F0014);
        // str r1, [r0]
        writeIntLE(memory, stubAddr + 44, 0xE5801000);
        
        // Retornar caractere
        // mov r0, r4
        writeIntLE(memory, stubAddr + 48, 0xE1A00004);
        // and r0, r0, #0xFF  // zero-extend
        writeIntLE(memory, stubAddr + 52, 0xE20000FF);
        
        // done:
        // ldmfd sp!, {r4, pc}
        writeIntLE(memory, stubAddr + 56, 0xE8BD8010);
        
        // Dados
        // String de entrada pré-definida
        String defaultInput = "Test input for getchar\nABCDEFGHIJKLMNOPQRSTUVWXYZ\n0123456789\n";
        byte[] inputBytes = defaultInput.getBytes();
        
        int inputAddr = allocateStubMemory(inputBytes.length + 1);
        int indexAddr = inputAddr + inputBytes.length + 1;
        
        // Copiar dados
        for (int i = 0; i < inputBytes.length; i++) {
            memory[inputAddr + i] = inputBytes[i];
        }
        memory[inputAddr + inputBytes.length] = 0; // null terminator
        
        // Inicializar índice
        writeIntLE(memory, indexAddr, 0);
        
        // Escrever endereços
        writeIntLE(memory, stubAddr + 60, inputAddr);   // input_buffer
        writeIntLE(memory, stubAddr + 64, indexAddr);   // input_index
        writeIntLE(memory, stubAddr + 68, indexAddr);   // input_index (novamente)
        
        return stubAddr;
    }

    // Versão que usa input do OpenTTY (se disponível)
    private int createGetcharInteractiveStub() {
        int stubSize = 128;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // getchar() - tentar obter input interativo
        
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // Verificar se há input disponível
        // bl check_input_available
        writeIntLE(memory, stubAddr + 4, 0xEB000010);
        
        // cmp r0, #0
        writeIntLE(memory, stubAddr + 8, 0xE3500000);
        
        // beq no_input
        writeIntLE(memory, stubAddr + 12, 0x0A00000C);
        
        // Há input: ler um caractere
        // bl read_input_char
        writeIntLE(memory, stubAddr + 16, 0xEB00000E);
        
        // Verificar se é válido
        // cmp r0, #0
        writeIntLE(memory, stubAddr + 20, 0xE3500000);
        
        // blt eof
        writeIntLE(memory, stubAddr + 24, 0xBA000008);
        
        // Caractere válido: retornar
        // and r0, r0, #0xFF  // zero-extend
        writeIntLE(memory, stubAddr + 28, 0xE20000FF);
        
        // b done
        writeIntLE(memory, stubAddr + 32, 0xEA00000E);
        
        // no_input:
        // Não há input disponível
        // Podemos:
        // 1. Retornar EOF
        // 2. Esperar (bloqueante)
        // 3. Retornar um valor padrão
        
        // Por enquanto, retornar EOF se não houver input
        // b eof
        writeIntLE(memory, stubAddr + 36, 0xEA000005);
        
        // eof:
        // Retornar EOF (-1)
        // mvn r0, #0
        writeIntLE(memory, stubAddr + 40, 0xE3E00000);
        
        // done:
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 44, 0xE8BD8000);
        
        // Sub-rotina: check_input_available
        // check_input_available:
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr + 48, 0xE92D4000);
        
        // Verificar com OpenTTY se há input
        // ldr r0, =midlet_ref
        writeIntLE(memory, stubAddr + 52, 0xE59F0028);
        
        // Chamar método Java
        // bl java_check_input
        writeIntLE(memory, stubAddr + 56, 0xEB000006);
        
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 60, 0xE8BD8000);
        
        // Sub-rotina: read_input_char
        // read_input_char:
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr + 64, 0xE92D4000);
        
        // ldr r0, =midlet_ref
        writeIntLE(memory, stubAddr + 68, 0xE59F0010);
        
        // Chamar método Java para ler caractere
        // bl java_read_char
        writeIntLE(memory, stubAddr + 72, 0xEB000006);
        
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 76, 0xE8BD8000);
        
        // Dados
        writeIntLE(memory, stubAddr + 80, 0x00000000); // midlet_ref placeholder
        
        // Armazenar handlers
        elfInfo.put("getchar_check_handler", new Object() {
            public int check() {
                // Verificar se há input disponível no OpenTTY
                // Por enquanto, sempre retorna 1 (tem input)
                return 1;
            }
        });
        
        elfInfo.put("getchar_read_handler", new Object() {
            public int read() {
                // Ler um caractere do input do OpenTTY
                // Se não houver input, retornar -1 (EOF)
                // Implementação simplificada: usar buffer pré-definido
                return getNextInputChar();
            }
        });
        
        return stubAddr;
    }

    // Método auxiliar para gerenciar input
    private int inputBufferIndex = 0;
    private String inputBuffer = "Hello from getchar!\nPress any key...\n";

    private int getNextInputChar() {
        if (inputBufferIndex >= inputBuffer.length()) {
            return -1; // EOF
        }
        
        char c = inputBuffer.charAt(inputBufferIndex);
        inputBufferIndex++;
        
        return c & 0xFF;
    }

    // Versão mínima funcional (recomendada para começar)
    private int createGetcharMinimalStub() {
        int stubSize = 32;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // getchar() - versão mínima que sempre retorna 'A' (para teste)
        
        // Retornar 'A' (65) sempre
        // mov r0, #65  // 'A'
        writeIntLE(memory, stubAddr, 0xE3A00041);
        
        // bx lr
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        
        return stubAddr;
    }

    // Versão que alterna entre caracteres (para teste de loop)
    private int createGetcharSequentialStub() {
        int stubSize = 64;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // getchar() - retorna sequência 'A', 'B', 'C', ..., EOF
        
        // stmfd sp!, {r4, lr}
        writeIntLE(memory, stubAddr, 0xE92D4010);
        
        // Carregar contador
        // ldr r4, =char_counter
        writeIntLE(memory, stubAddr + 4, 0xE59F4028);
        // ldr r0, [r4]
        writeIntLE(memory, stubAddr + 8, 0xE5940000);
        
        // Verificar se atingiu fim
        // cmp r0, #26  // A-Z
        writeIntLE(memory, stubAddr + 12, 0xE350001A);
        
        // bge eof
        writeIntLE(memory, stubAddr + 16, 0xA0000008);
        
        // Calcular caractere: 'A' + counter
        // add r0, r0, #65  // 'A' = 65
        writeIntLE(memory, stubAddr + 20, 0xE2800041);
        
        // Incrementar contador
        // ldr r1, [r4]
        writeIntLE(memory, stubAddr + 24, 0xE5941000);
        // add r1, r1, #1
        writeIntLE(memory, stubAddr + 28, 0xE2811001);
        // str r1, [r4]
        writeIntLE(memory, stubAddr + 32, 0xE5841000);
        
        // b done
        writeIntLE(memory, stubAddr + 36, 0xEA000003);
        
        // eof:
        // Retornar EOF
        // mvn r0, #0  // -1
        writeIntLE(memory, stubAddr + 40, 0xE3E00000);
        
        // done:
        // ldmfd sp!, {r4, pc}
        writeIntLE(memory, stubAddr + 44, 0xE8BD8010);
        
        // Dados: contador
        int counterAddr = allocateStubMemory(4);
        writeIntLE(memory, counterAddr, 0); // Iniciar em 0
        
        writeIntLE(memory, stubAddr + 48, counterAddr); // char_counter
        
        return stubAddr;
    }

    private int createScanfStub() {
        int stubSize = 32;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // scanf(const char *format, ...) - retornar 0 (nenhum item lido)
        
        // stmfd sp!, {lr}
        writeIntLE(memory, stubAddr, 0xE92D4000);
        
        // Para scanf, podemos:
        // 1. Retornar 0 (nenhum item lido com sucesso)
        // 2. Retornar EOF (-1) se falhou
        // 3. Implementar leitura básica
        
        // Vamos implementar scanf que sempre retorna 0
        // mov r0, #0  // retorna 0 itens lidos
        writeIntLE(memory, stubAddr + 4, 0xE3A00000);
        
        // ldmfd sp!, {pc}
        writeIntLE(memory, stubAddr + 8, 0xE8BD8000);
        
        return stubAddr;
    }

    private int createGenericStub(String symbolName) {
        int stubSize = 64; // Tamanho razoável para um stub
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        // Analisar o nome da função para determinar comportamento
        String lowerName = symbolName.toLowerCase();
        
        // === DETECTAR TIPO DE FUNÇÃO PELO NOME ===
        
        // 1. Funções que retornam void/nada (procedures)
        if (lowerName.startsWith("void_") || 
            lowerName.endsWith("_void") ||
            lowerName.startsWith("proc_") ||
            lowerName.endsWith("_proc") ||
            lowerName.startsWith("do_") ||
            lowerName.endsWith("_do") ||
            lowerName.contains("init") ||
            lowerName.contains("setup") ||
            lowerName.contains("start") ||
            lowerName.contains("stop") ||
            lowerName.contains("close") ||
            lowerName.contains("free") ||
            lowerName.contains("destroy") ||
            lowerName.contains("cleanup") ||
            lowerName.contains("finalize")) {
            
            return createVoidFunctionStub(stubAddr, symbolName);
        }
        
        // 2. Funções getter (retornam valor)
        else if (lowerName.startsWith("get_") || 
                lowerName.startsWith("get") ||
                lowerName.contains("_get") ||
                lowerName.startsWith("read_") ||
                lowerName.contains("_read") ||
                lowerName.startsWith("fetch_") ||
                lowerName.contains("fetch") ||
                lowerName.startsWith("obtain_") ||
                lowerName.startsWith("acquire_")) {
            
            return createGetterStub(stubAddr, symbolName);
        }
        
        // 3. Funções setter (configuram valor)
        else if (lowerName.startsWith("set_") || 
                lowerName.startsWith("set") ||
                lowerName.contains("_set") ||
                lowerName.startsWith("write_") ||
                lowerName.contains("_write") ||
                lowerName.startsWith("store_") ||
                lowerName.contains("store") ||
                lowerName.startsWith("put_") ||
                lowerName.contains("_put") ||
                lowerName.startsWith("assign_")) {
            
            return createSetterStub(stubAddr, symbolName);
        }
        
        // 4. Funções boolean/verificação
        else if (lowerName.startsWith("is_") || 
                lowerName.startsWith("is") ||
                lowerName.startsWith("has_") ||
                lowerName.startsWith("has") ||
                lowerName.startsWith("can_") ||
                lowerName.startsWith("can") ||
                lowerName.startsWith("should_") ||
                lowerName.startsWith("should") ||
                lowerName.startsWith("check_") ||
                lowerName.contains("_check") ||
                lowerName.startsWith("test_") ||
                lowerName.contains("_test") ||
                lowerName.contains("valid") ||
                lowerName.contains("empty") ||
                lowerName.contains("null") ||
                lowerName.contains("exist")) {
            
            return createBooleanStub(stubAddr, symbolName);
        }
        
        // 5. Funções matemáticas/cálculo
        else if (lowerName.contains("calc") ||
                lowerName.contains("compute") ||
                lowerName.contains("math") ||
                lowerName.contains("sum") ||
                lowerName.contains("add") ||
                lowerName.contains("sub") ||
                lowerName.contains("mul") ||
                lowerName.contains("div") ||
                lowerName.contains("avg") ||
                lowerName.contains("mean") ||
                lowerName.contains("min") ||
                lowerName.contains("max") ||
                lowerName.contains("count")) {
            
            return createMathStub(stubAddr, symbolName);
        }
        
        // 6. Funções de string/texto
        else if (lowerName.contains("str") ||
                lowerName.contains("text") ||
                lowerName.contains("char") ||
                lowerName.contains("byte") ||
                lowerName.contains("concat") ||
                lowerName.contains("append") ||
                lowerName.contains("format") ||
                lowerName.contains("parse") ||
                lowerName.contains("encode") ||
                lowerName.contains("decode")) {
            
            return createStringStub(stubAddr, symbolName);
        }
        
        // 7. Funções de erro/status
        else if (lowerName.contains("error") ||
                lowerName.contains("err") ||
                lowerName.contains("status") ||
                lowerName.contains("result") ||
                lowerName.contains("return") ||
                lowerName.contains("code") ||
                lowerName.contains("success") ||
                lowerName.contains("fail")) {
            
            return createStatusStub(stubAddr, symbolName);
        }
        
        // 8. Funções de tempo/data
        else if (lowerName.contains("time") ||
                lowerName.contains("date") ||
                lowerName.contains("delay") ||
                lowerName.contains("sleep") ||
                lowerName.contains("wait") ||
                lowerName.contains("clock") ||
                lowerName.contains("timer") ||
                lowerName.contains("epoch")) {
            
            return createTimeStub(stubAddr, symbolName);
        }
        
        // 9. Funções de memória/alocação
        else if (lowerName.contains("mem") ||
                lowerName.contains("alloc") ||
                lowerName.contains("heap") ||
                lowerName.contains("pool") ||
                lowerName.contains("buffer") ||
                lowerName.contains("cache")) {
            
            return createMemoryStub(stubAddr, symbolName);
        }
        
        // 10. Funções de sistema/OS
        else if (lowerName.contains("sys") ||
                lowerName.contains("os") ||
                lowerName.contains("kernel") ||
                lowerName.contains("driver") ||
                lowerName.contains("io") ||
                lowerName.contains("file") ||
                lowerName.contains("dir") ||
                lowerName.contains("path") ||
                lowerName.contains("device")) {
            
            return createSystemStub(stubAddr, symbolName);
        }
        
        // 11. Funções de debug/log
        else if (lowerName.contains("debug") ||
                lowerName.contains("log") ||
                lowerName.contains("trace") ||
                lowerName.contains("print") ||
                lowerName.contains("dump") ||
                lowerName.contains("assert")) {
            
            return createDebugStub(stubAddr, symbolName);
        }
        
        // 12. Funções de lock/sincronização
        else if (lowerName.contains("lock") ||
                lowerName.contains("mutex") ||
                lowerName.contains("sem") ||
                lowerName.contains("barrier") ||
                lowerName.contains("sync") ||
                lowerName.contains("atomic")) {
            
            return createLockStub(stubAddr, symbolName);
        }
        
        // 13. Funções de lista/array/coleção
        else if (lowerName.contains("list") ||
                lowerName.contains("array") ||
                lowerName.contains("vector") ||
                lowerName.contains("queue") ||
                lowerName.contains("stack") ||
                lowerName.contains("map") ||
                lowerName.contains("hash") ||
                lowerName.contains("tree")) {
            
            return createCollectionStub(stubAddr, symbolName);
        }
        
        // 14. Funções com "create" ou "new"
        else if (lowerName.startsWith("create_") ||
                lowerName.startsWith("create") ||
                lowerName.startsWith("new_") ||
                lowerName.startsWith("new") ||
                lowerName.contains("_create") ||
                lowerName.contains("_new") ||
                lowerName.startsWith("make_") ||
                lowerName.contains("_make")) {
            
            return createFactoryStub(stubAddr, symbolName);
        }
        
        // 15. Funções com "find" ou "search"
        else if (lowerName.startsWith("find_") ||
                lowerName.startsWith("find") ||
                lowerName.startsWith("search_") ||
                lowerName.startsWith("search") ||
                lowerName.contains("_find") ||
                lowerName.contains("_search") ||
                lowerName.startsWith("locate_") ||
                lowerName.contains("_locate")) {
            
            return createSearchStub(stubAddr, symbolName);
        }
        
        // 16. Funções com "compare" ou "cmp"
        else if (lowerName.contains("compare") ||
                lowerName.contains("cmp") ||
                lowerName.contains("diff") ||
                lowerName.contains("equal") ||
                lowerName.contains("match")) {
            
            return createCompareStub(stubAddr, symbolName);
        }
        
        // 17. Funções com "copy" ou "clone"
        else if (lowerName.contains("copy") ||
                lowerName.contains("clone") ||
                lowerName.contains("duplicate") ||
                lowerName.contains("replicate")) {
            
            return createCopyStub(stubAddr, symbolName);
        }
        
        // 18. Funções com "convert" ou "transform"
        else if (lowerName.contains("convert") ||
                lowerName.contains("transform") ||
                lowerName.contains("transcode") ||
                lowerName.contains("translate")) {
            
            return createConvertStub(stubAddr, symbolName);
        }
        
        // 19. Funções com "handle" ou "manager"
        else if (lowerName.contains("handle") ||
                lowerName.contains("manager") ||
                lowerName.contains("controller")) {
            
            return createHandleStub(stubAddr, symbolName);
        }
        
        // 20. Funções com "callback" ou "handler"
        else if (lowerName.contains("callback") ||
                lowerName.contains("handler") ||
                lowerName.contains("listener") ||
                lowerName.contains("observer")) {
            
            return createCallbackStub(stubAddr, symbolName);
        }
        
        // DEFAULT: Função genérica que retorna 0/sucesso
        return createDefaultStub(stubAddr, symbolName);
    }

    // ========== IMPLEMENTAÇÕES ESPECÍFICAS ==========

    private int createVoidFunctionStub(int stubAddr, String name) {
        // Funções que não retornam valor (void)
        // bx lr (apenas retorna)
        writeIntLE(memory, stubAddr, 0xE12FFF1E);
        
        // Registrar para debug
        if (midlet.debug) {
            midlet.print("Created void stub for: " + name + " at " + toHex(stubAddr), stdout);
        }
        
        return stubAddr;
    }

    private int createGetterStub(int stubAddr, String name) {
        // Funções getter - retornam valor "falso"
        // Padrão: retorna 0, 1, ou valor baseado no nome
        
        int returnValue = 0;
        
        // Tentar extrair valor do nome (ex: get_version -> 1)
        if (name.contains("version") || name.contains("ver")) {
            returnValue = 1;
        } else if (name.contains("size") || name.contains("length") || name.contains("count")) {
            returnValue = 0; // Tamanho 0
        } else if (name.contains("id") || name.contains("handle")) {
            returnValue = 0x1234; // ID falso
        } else if (name.contains("time") || name.contains("clock")) {
            returnValue = (int)(System.currentTimeMillis() / 1000) & 0x7FFFFFFF;
        } else if (name.contains("error") || name.contains("errno")) {
            returnValue = 0; // Sem erro
        } else {
            // Valor padrão baseado em hash do nome
            returnValue = name.hashCode() & 0x7FFFFFFF;
        }
        
        // mov r0, #returnValue (ou valor apropriado)
        if (returnValue <= 0xFF) {
            // mov r0, #value
            writeIntLE(memory, stubAddr, 0xE3A00000 | (returnValue & 0xFF));
        } else {
            // Precisa de mais instruções para valores maiores
            // movw r0, #(value & 0xFFFF)
            int movwInstr = 0xE3000000 | ((returnValue & 0xF000) << 4) | (returnValue & 0x0FFF);
            writeIntLE(memory, stubAddr, movwInstr);
            
            if (returnValue > 0xFFFF) {
                // movt r0, #(value >> 16)
                int movtInstr = 0xE3400000 | (((returnValue >> 12) & 0xF000) << 4) | ((returnValue >> 16) & 0x0FFF);
                writeIntLE(memory, stubAddr + 4, movtInstr);
                // bx lr
                writeIntLE(memory, stubAddr + 8, 0xE12FFF1E);
                return stubAddr;
            }
        }
        
        // bx lr
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        
        if (midlet.debug) {
            midlet.print("Created getter stub for: " + name + " returns " + returnValue, stdout);
        }
        
        return stubAddr;
    }

    private int createSetterStub(int stubAddr, String name) {
        // Funções setter - retornam sucesso (0) ou falha (-1)
        // Normalmente retornam void ou int (0=sucesso)
        
        // Retornar 0 (sucesso)
        // mov r0, #0
        writeIntLE(memory, stubAddr, 0xE3A00000);
        // bx lr
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        
        if (midlet.debug) {
            midlet.print("Created setter stub for: " + name, stdout);
        }
        
        return stubAddr;
    }

    private int createBooleanStub(int stubAddr, String name) {
        // Funções booleanas - retornam true (1) ou false (0)
        
        int returnValue = 1; // Por padrão, retorna true
        
        // Algumas funções específicas
        if (name.contains("isnull") || name.contains("is_null") ||
            name.contains("isempty") || name.contains("is_empty") ||
            name.contains("isfull") || name.contains("is_full") ||
            name.contains("haserror") || name.contains("has_error") ||
            name.contains("failed") || name.contains("fail")) {
            returnValue = 0; // false
        }
        
        // mov r0, #returnValue
        writeIntLE(memory, stubAddr, 0xE3A00000 | (returnValue & 0xFF));
        // bx lr
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        
        if (midlet.debug) {
            midlet.print("Created boolean stub for: " + name + " returns " + 
                    (returnValue != 0 ? "true" : "false"), stdout);
        }
        
        return stubAddr;
    }

    private int createMathStub(int stubAddr, String name) {
        // Funções matemáticas - retornam resultado calculado
        
        int returnValue = 0;
        
        if (name.contains("add") || name.contains("sum") || name.contains("total")) {
            // Soma: retorna primeiro argumento (assumindo está em r0)
            // Função já recebe valor em r0, apenas retorna
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
        } else if (name.contains("sub") || name.contains("diff")) {
            // Diferença: retorna 0
            // mov r0, #0
            writeIntLE(memory, stubAddr, 0xE3A00000);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        } else if (name.contains("mul") || name.contains("product")) {
            // Produto: retorna primeiro argumento
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
        } else if (name.contains("div") || name.contains("quotient")) {
            // Quociente: retorna 1
            // mov r0, #1
            writeIntLE(memory, stubAddr, 0xE3A00001);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        } else if (name.contains("min")) {
            // Mínimo: retorna primeiro argumento
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
        } else if (name.contains("max")) {
            // Máximo: retorna 100
            // mov r0, #100
            writeIntLE(memory, stubAddr, 0xE3A00064);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        } else if (name.contains("avg") || name.contains("mean")) {
            // Média: retorna 50
            // mov r0, #50
            writeIntLE(memory, stubAddr, 0xE3A00032);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        } else {
            // Default: retorna 0
            // mov r0, #0
            writeIntLE(memory, stubAddr, 0xE3A00000);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        }
        
        if (midlet.debug) {
            midlet.print("Created math stub for: " + name, stdout);
        }
        
        return stubAddr;
    }

    private int createStringStub(int stubAddr, String name) {
        // Funções de string - comportamento específico
        
        if (name.contains("len") || name.contains("length") || name.contains("size")) {
            // strlen, etc - retorna comprimento
            // Retorna comprimento do primeiro argumento (string em r0)
            // Vamos implementar strlen simplificado
            
            // stmfd sp!, {lr}
            writeIntLE(memory, stubAddr, 0xE92D4000);
            
            // mov r1, r0  // backup do ponteiro
            writeIntLE(memory, stubAddr + 4, 0xE1A01000);
            
            // loop: ldrb r2, [r1], #1
            writeIntLE(memory, stubAddr + 8, 0xE4D12001);
            
            // cmp r2, #0
            writeIntLE(memory, stubAddr + 12, 0xE3520000);
            
            // bne loop
            writeIntLE(memory, stubAddr + 16, 0x1AFFFFFB);
            
            // sub r0, r1, r0
            writeIntLE(memory, stubAddr + 20, 0xE0400001);
            
            // sub r0, r0, #1
            writeIntLE(memory, stubAddr + 24, 0xE2400001);
            
            // ldmfd sp!, {pc}
            writeIntLE(memory, stubAddr + 28, 0xE8BD8000);
            
        } else if (name.contains("copy") || name.contains("cpy")) {
            // strcpy, etc - retorna ponteiro destino
            // Retorna primeiro argumento (dest em r0)
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
            
        } else if (name.contains("cat") || name.contains("append")) {
            // strcat - retorna ponteiro destino
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
            
        } else if (name.contains("cmp") || name.contains("compare")) {
            // strcmp - retorna 0 (iguais)
            // mov r0, #0
            writeIntLE(memory, stubAddr, 0xE3A00000);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
            
        } else {
            // Default: retorna primeiro argumento
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
        }
        
        if (midlet.debug) {
            midlet.print("Created string stub for: " + name, stdout);
        }
        
        return stubAddr;
    }

    private int createDefaultStub(int stubAddr, String name) {
        // Stub genérico padrão
        
        // Funções normalmente retornam 0 (sucesso) ou primeiro argumento
        // Vamos fazer heurística simples:
        
        // Se nome sugere que retorna ponteiro/objeto, retorna primeiro arg
        if (name.contains("ptr") || name.contains("pointer") ||
            name.contains("obj") || name.contains("object") ||
            name.contains("handle") || name.contains("ref") ||
            name.contains("address") || name.contains("addr") ||
            name.startsWith("get") || name.startsWith("create") ||
            name.startsWith("new") || name.startsWith("make") ||
            name.contains("alloc") || name.contains("open")) {
            
            // Retorna primeiro argumento (já em r0)
            // bx lr
            writeIntLE(memory, stubAddr, 0xE12FFF1E);
            
        } else if (name.contains("error") || name.contains("err") ||
                name.contains("fail") || name.contains("invalid") ||
                name.contains("null") || name.contains("empty")) {
            
            // Retorna valor de erro (-1 ou NULL)
            // mov r0, #0  // NULL
            writeIntLE(memory, stubAddr, 0xE3A00000);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
            
        } else {
            // Retorna 0 (sucesso)
            // mov r0, #0
            writeIntLE(memory, stubAddr, 0xE3A00000);
            // bx lr
            writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        }
        
        if (midlet.debug) {
            midlet.print("Created generic stub for: " + name, stdout);
        }
        
        return stubAddr;
    }

    // ========== STUBS PARA OUTROS TIPOS (simplificados) ==========

    private int createStatusStub(int stubAddr, String name) {
        // mov r0, #0 (sucesso)
        writeIntLE(memory, stubAddr, 0xE3A00000);
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        return stubAddr;
    }

    private int createTimeStub(int stubAddr, String name) {
        // Retorna timestamp atual
        int time = (int)(System.currentTimeMillis() / 1000);
        // Usar stub mais complexo ou simplificar
        writeIntLE(memory, stubAddr, 0xE12FFF1E); // bx lr (retorna r0 que pode ter sido setado)
        return stubAddr;
    }

    private int createMemoryStub(int stubAddr, String name) {
        // Para funções de alocação, retorna ponteiro simulado
        // Retorna endereço do heap atual
        // ldr r0, =heap_current
        // bx lr
        // Implementação simplificada:
        writeIntLE(memory, stubAddr, 0xE12FFF1E);
        return stubAddr;
    }

    private int createSystemStub(int stubAddr, String name) {
        // Retorna 0 (sucesso) ou -1 (falha) dependendo do nome
        if (name.contains("error") || name.contains("fail")) {
            // mvn r0, #0  // -1
            writeIntLE(memory, stubAddr, 0xE3E00000);
        } else {
            // mov r0, #0  // sucesso
            writeIntLE(memory, stubAddr, 0xE3A00000);
        }
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        return stubAddr;
    }

    // ========== MÉTODO AUXILIAR PARA CRIAÇÃO RÁPIDA ==========

    private int createSimpleStub(int returnValue) {
        int stubSize = 8;
        int stubAddr = allocateStubMemory(stubSize);
        if (stubAddr == 0) stubAddr = findFreeMemoryRegion(stubSize);
        if (stubAddr == 0) return 0;
        
        if (returnValue == 0) {
            // mov r0, #0
            writeIntLE(memory, stubAddr, 0xE3A00000);
        } else if (returnValue == 1) {
            // mov r0, #1
            writeIntLE(memory, stubAddr, 0xE3A00001);
        } else if (returnValue == -1) {
            // mvn r0, #0  // -1 = 0xFFFFFFFF
            writeIntLE(memory, stubAddr, 0xE3E00000);
        } else {
            // Para outros valores, usar retorno genérico
            writeIntLE(memory, stubAddr, 0xE12FFF1E); // bx lr
            return stubAddr;
        }
        
        // bx lr
        writeIntLE(memory, stubAddr + 4, 0xE12FFF1E);
        
        return stubAddr;
    }

    // Syscalls Handler
    // |
    private void handleSyscall(int number) {
        // Debug
        if (midlet.debug && number != SYS_GETTIMEOFDAY && number != SYS_GETPID) {
            midlet.print("Syscall " + number + " (R7=" + registers[REG_R7] + ")", stdout, id);
        }
        int savedPC = pc;
    
        
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
                registers[REG_R0] = -38; //handlePipe();
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

            Hashtable arg = new Hashtable();
            arg.put(new Double(0), path);
            String[] argList = midlet.splitArgs(argsStr.toString());
            for (i = 0; i < argList.length; i++) { arg.put(new Double(i + 1), argList[i]); }
            
            boolean isElf = (bytesRead == 4 && header[0] == 0x7F && header[1] == 'E' && header[2] == 'L' && header[3] == 'F');
            
            if (isElf) {
                // Executar ELF (similar ao fork+exec)
                InputStream elfStream = midlet.getInputStream(path);
                ELF elf = new ELF(midlet, arg, stdout, scope, id, null, null);
                
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
        cleanup();
    }
    private void cleanup() {
        // Fechar todos os file descriptors
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

        // Fechar sockets
        keys = socketDescriptors.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Hashtable socketInfo = (Hashtable) socketDescriptors.get(key);
            if (socketInfo.containsKey("connection")) { try { ((StreamConnection) socketInfo.get("connection")).close(); } catch (Exception e) { } }
            if (socketInfo.containsKey("server")) { try { ((StreamConnectionNotifier) socketInfo.get("server")).close(); } catch (Exception e) { } }
        }
        
        // Limpar estruturas
        fileDescriptors.clear(); socketDescriptors.clear();
        allocatedBlocks.clear(); instructionCache.clear(); jmpBufs.clear();
        memoryMappings.removeAllElements();
    }
    // |
    private void handleGetpid() { try { int pidValue = Integer.parseInt(this.pid); registers[REG_R0] = pidValue; } catch (NumberFormatException e) { registers[REG_R0] = 1; } }
    private void handleGetppid() { registers[REG_R0] = 1; }
    private void handleGetuid() { registers[REG_R0] = id; }
    private void handleGettid() { registers[REG_R0] = id; }
    // | (Users)

    // | (Memory)
    private void handleMmap() {
        // Parâmetros 1-4 em R0-R3
        int addr = registers[REG_R0];
        int length = registers[REG_R1];
        int prot = registers[REG_R2];
        int flags = registers[REG_R3];
        
        // Parâmetros 5-6 na stack (R4 e R5 não são usados!)
        int fd = getSyscallParam(4);    // Parâmetro 5 (índice 4)
        int offset = getSyscallParam(5); // Parâmetro 6 (índice 5)
        
        if (midlet.debug) {
            midlet.print("mmap: addr=" + toHex(addr) + " length=" + length + 
                        " prot=" + prot + " flags=" + toHex(flags) + 
                        " fd=" + fd + " offset=" + offset, stdout, id);
        }
        
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
        // Parâmetros 1-4 em R0-R3
        int old_addr = registers[REG_R0];
        int old_size = registers[REG_R1];
        int new_size = registers[REG_R2];
        int flags = registers[REG_R3];
        
        // Parâmetro 5 na stack
        int new_addr = getSyscallParam(4);
        
        if (midlet.debug) {
            midlet.print("mremap: old=" + toHex(old_addr) + " oldsize=" + old_size +
                        " newsize=" + new_size + " flags=" + flags +
                        " newaddr=" + toHex(new_addr), stdout, id);
        }
        
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
    private void handleSendto() {
        // Parâmetros 1-6 (R0-R3 + stack)
        int fd = registers[REG_R0];
        int buf = registers[REG_R1];
        int len = registers[REG_R2];
        int flags = registers[REG_R3];
        
        // Parâmetros 5-6 na stack
        int dest_addr = getSyscallParam(4);
        int addrlen = getSyscallParam(5);
        
        // Implementação simplificada - usa send normal
        handleSend();
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
    private void handleRecvfrom() {
        // Parâmetros 1-6 (R0-R3 + stack)
        int fd = registers[REG_R0];
        int buf = registers[REG_R1];
        int len = registers[REG_R2];
        int flags = registers[REG_R3];
        
        // Parâmetros 5-6 na stack
        int src_addr = getSyscallParam(4);
        int addrlen = getSyscallParam(5);
        
        // Implementação simplificada - usa recv normal
        handleRecv();
    }
    // | (Socket Params)
    private void handleSetsockopt() {
        // Parâmetros 1-5 (R0-R3 + stack)
        int fd = registers[REG_R0];
        int level = registers[REG_R1];
        int optname = registers[REG_R2];
        int optval = registers[REG_R3];
        
        // Parâmetro 5 na stack
        int optlen = getSyscallParam(4);
        
        // Implementação simplificada
        registers[REG_R0] = 0;
    }
    private void handleGetsockopt() {
        // Parâmetros 1-5 (R0-R3 + stack)
        int fd = registers[REG_R0];
        int level = registers[REG_R1];
        int optname = registers[REG_R2];
        int optval = registers[REG_R3];
        
        // Parâmetro 5 na stack
        int optlen = getSyscallParam(4);
        
        // Implementação simplificada
        registers[REG_R0] = 0;
    }
    // |
    private void handleBind() { registers[REG_R0] = -1; } // Não implementado
    private void handleListen() { registers[REG_R0] = -1; } // Não implementado
    private void handleAccept() { registers[REG_R0] = -1; } // Não implementado
    private void handleShutdown() { registers[REG_R0] = -1; } // Não implementado
    private void handleNanosleep() { registers[REG_R0] = -1; } // Não implementado
    private void handleGetsockname() { registers[REG_R0] = -1; } // Não implementado
    private void handleGetpeername() { registers[REG_R0] = -1; } // Não implementado

    private void handleFutex() {
        // Parâmetros 1-4 em R0-R3
        int uaddr = registers[REG_R0];
        int op = registers[REG_R1];
        int val = registers[REG_R2];
        int timeout = registers[REG_R3];
        
        // Parâmetros 5-6 na stack
        int uaddr2 = getSyscallParam(4);
        int val3 = getSyscallParam(5);
        
        if (midlet.debug) {
            midlet.print("futex: uaddr=" + toHex(uaddr) + " op=" + op +
                        " val=" + val + " timeout=" + timeout, stdout, id);
        }
        
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
        // Começar após o heap atual
        int start = heapEnd;
        
        // Garantir alinhamento mínimo de 4 bytes
        start = (start + 3) & ~3;
        
        while (start + length < memory.length) {
            boolean free = true;
            
            // Verificar se sobrepõe com PLT
            if (pltBase != 0 && start < pltBase + 4096 && start + length > pltBase) {
                free = false;
                start = pltBase + 4096;
            }
            
            // Verificar se sobrepõe com resolvedor
            if (resolverCodeAddr != 0 && start < resolverCodeAddr + 256 && start + length > resolverCodeAddr) {
                free = false;
                start = resolverCodeAddr + 256;
            }
            
            // Verificar se sobrepõe com stubs existentes
            if (elfInfo.containsKey("stub_area")) {
                int stubStart = ((Integer)elfInfo.get("stub_area")).intValue();
                int stubSize = 65536; // 64KB para stubs
                if (start < stubStart + stubSize && start + length > stubStart) {
                    free = false;
                    start = stubStart + stubSize;
                }
            }
            
            // Verificar mapeamentos de memória
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
                // Alinhar para página (opcional, mas bom para stubs de código)
                if (length >= 4096) {
                    start = (start + 4095) & ~4095;
                }
                return start;
            }
            
            // Se não livre, continuar procurando
            start += 4096; // Pular uma página
        }
        
        // Se não encontrou região livre, tentar expandir heap
        if (heapEnd + length < memory.length) {
            start = heapEnd;
            heapEnd = start + length;
            start = (start + 3) & ~3; // Alinhar
            return start;
        }
        
        // Sem memória
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
    private int getSyscallParam(int paramIndex) {
        // Parâmetros 0-3 estão em R0-R3
        if (paramIndex < 4) {
            return registers[paramIndex];
        }
        
        // Parâmetros 4+ estão na stack
        int sp = registers[REG_SP];
        int offset = 4 + (paramIndex - 4) * 4;
        
        if (sp + offset + 3 < memory.length && sp + offset >= 0) {
            return readIntLE(memory, sp + offset);
        }
        
        return 0;
    }

    private int allocateStubMemory(int size) {
        // Se ainda não tem área de stubs, criar uma
        if (!elfInfo.containsKey("stub_area")) {
            int stubArea = findFreeMemoryRegion(65536); // 64KB para stubs
            if (stubArea == 0) return 0;
            
            elfInfo.put("stub_area", new Integer(stubArea));
            elfInfo.put("stub_next", new Integer(stubArea));
            elfInfo.put("stub_size", new Integer(65536));
            
            if (midlet.debug) {
                midlet.print("Allocated stub area at " + toHex(stubArea), stdout);
            }
        }
        
        // Alocar dentro da área de stubs
        int stubNext = ((Integer)elfInfo.get("stub_next")).intValue();
        int stubArea = ((Integer)elfInfo.get("stub_area")).intValue();
        int stubSize = ((Integer)elfInfo.get("stub_size")).intValue();
        
        // Alinhar para 4 bytes
        stubNext = (stubNext + 3) & ~3;
        
        // Verificar se cabe
        if (stubNext + size <= stubArea + stubSize) {
            elfInfo.put("stub_next", new Integer(stubNext + size));
            return stubNext;
        }
        
        // Não coube, procurar nova região
        return findFreeMemoryRegion(size);
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