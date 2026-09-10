import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import java.util.*;
import java.io.*;
// |
// ELF RISC-V 32 Emulator (RV32IM)
public class ELF {
    private static final boolean LITE_EDITION = false;

    public static boolean isLiteEdition() { return LITE_EDITION; }

    private OpenTTY midlet;
    private Object stdout;
    private Process proc;
    private Hashtable scope;
    private String pid;
    private int id = 1000;
    
    // Memória e registradores
    private byte[] memory;
    private int[] registers;
    private int[] signalHandlers;
    private int pc;
    private boolean running;
    private int stackPointer;
    
    // File descriptors
    private Hashtable fileDescriptors, socketDescriptors;
    private int nextFd;

    private Hashtable jmpBufs;
    private int nextJmpBufId;

    // Heap management
    private Hashtable allocatedBlocks;
    private int heapStart, heapEnd;

    // Dynamic linking structures
    private Hashtable dynamicSymbols, neededLibraries, globalSymbols;
    private Vector sharedObjectMappings;
    private Vector loadedLibraries, memoryMappings, dynSymNames;
    private Hashtable copyRelocs, libSymSizes; 
    private int pltGotAddr, dynamicSectionAddr, gotBase;

    // heap do stdlib (malloc/calloc/realloc/free): allocador first-fit com
    // blocos escritos na RAM do guest: { int size; int next; ...payload }
    private int libcHeapFree, libcHeapTop, libcHeapRegionEnd;

    
    // Constantes ELF
    private static final int EI_NIDENT = 16, ELFCLASS32 = 1, ELFDATA2LSB = 1, EM_RISCV = 243, ET_EXEC = 2, ET_DYN = 3, PT_LOAD = 1, PT_DYNAMIC = 2, PT_INTERP = 3, PT_NOTE = 4;
    
    // Constantes de Registradores RISC-V (a0-a7 = x10-x17; a7 carrega o numero de syscall)
    private static final int REG_A0 = 10, REG_A1 = 11, REG_A2 = 12, REG_A3 = 13, REG_A7 = 17;
    private static final int REG_SP = 2, REG_LR = 1, REG_PC = 31;
    
    // Opcodes RV32I
    private static final int RV_OP_LUI = 0x37, RV_OP_AUIPC = 0x17, RV_OP_JAL = 0x6F, RV_OP_JALR = 0x67,
        RV_OP_BRANCH = 0x63, RV_OP_LOAD = 0x03, RV_OP_STORE = 0x23, RV_OP_OPIMM = 0x13, RV_OP_OP = 0x33,
        RV_OP_MISCMEM = 0x0F, RV_OP_SYSTEM = 0x73;
    
    // Syscalls do kernel (numeros EABI do emulador)
    private static final int SYS_EXIT = 1, SYS_FORK = 2, SYS_READ = 3, SYS_WRITE = 4, SYS_OPEN = 5, SYS_CLOSE = 6, SYS_CREAT = 8, SYS_UNLINK = 10, SYS_EXECVE = 11, SYS_CHDIR = 12, SYS_TIME = 13, SYS_LSEEK = 19, SYS_GETPID = 20, SYS_KILL = 37, SYS_MKDIR = 39, SYS_RMDIR = 40, SYS_DUP = 41, SYS_PIPE = 42, SYS_IOCTL = 54, SYS_FCNTL = 55, SYS_SIGNAL = 48, SYS_DUP2 = 63, SYS_GETPPID = 64, SYS_SIGACTION = 67, SYS_BRK = 45, SYS_TRUNCATE = 92, SYS_FTRUNCATE = 93, SYS_SETJMP = 96, SYS_LONGJMP = 97, SYS_FSYNC = 118, SYS_SIGRETURN = 119, SYS_UNAME = 122, SYS_MPROTECT = 125, SYS_SIGPROCMASK = 126, SYS_STAT = 106, SYS_FSTAT = 108, SYS_GETTIMEOFDAY = 78, SYS_GETPRIORITY = 140, SYS_SETPRIORITY = 141, SYS_SELECT = 142, SYS_SCHED_YIELD = 158, SYS_NANOSLEEP = 162, SYS_MREMAP = 163, SYS_POLL = 168, SYS_MUNMAP = 91, SYS_GETRLIMIT = 191, SYS_MMAP = 192, SYS_GETCWD = 183, SYS_GETUID32 = 199, SYS_GETEUID32 = 201, SYS_GETDENTS = 217, SYS_GETTID = 224, SYS_FUTEX = 240, SYS_SOCKET = 281, SYS_BIND = 282, SYS_CONNECT = 283, SYS_LISTEN = 284, SYS_ACCEPT = 285, SYS_GETSOCKNAME = 286, SYS_GETPEERNAME = 287, SYS_SEND = 289, SYS_SENDTO = 290, SYS_RECV = 291, SYS_RECVFROM = 292, SYS_SHUTDOWN = 293, SYS_SETSOCKOPT = 294, SYS_GETSOCKOPT = 295, SYS_SYSCALL = 0;
    
    // Constantes para socket
    private static final int SOCK_STREAM = 1, SOCK_DGRAM = 2, AF_INET = 2, IPPROTO_TCP = 6, IPPROTO_UDP = 17;

    // Soluções de socket (level para setsockopt/getsockopt)
    private static final int SOL_SOCKET = 1, SOL_IP = 0;

    // Opções de socket
    private static final int TCP_NODELAY = 1, SO_REUSEADDR = 2, SO_TYPE = 3, SO_ERROR = 4, SO_DONTROUTE = 5, SO_BROADCAST = 6, SO_SNDBUF = 7, SO_RCVBUF = 8, SO_KEEPALIVE = 9, SO_OOBINLINE = 10, SO_LINGER = 13;

    // Erros de rede adicionais
    private static final int ENOTSOCK = 88, ENOPROTOOPT = 92, EADDRINUSE = 98, EADDRNOTAVAIL = 99, EISCONN = 106;

    // Constantes para sinal
    private static final int SIG_ERR = -1, SIG_DFL = 0, SIG_IGN = 1, SIGINT = 2, SIGKILL = 9, SIGSEGV = 11, SIGPIPE = 13, SIGTERM = 15, SIGCHLD = 17, SIGCONT = 18, SIGSTOP = 19, NSIG = 32;

    // Adicionar constantes para flags de ioctl (simplificadas)
    private static final int TCGETS = 0x5401, TCSETS = 0x5402, TIOCGWINSZ = 0x5413, TIOCSWINSZ = 0x5414, FIONREAD = 0x541B;

    // Adicionar constantes para mode de mkdir
    private static final int S_IRWXU = 0700, S_IRUSR = 0400, S_IWUSR = 0200, S_IXUSR = 0100, S_IRWXG = 0070, S_IRGRP = 0040, S_IWGRP = 0020, S_IXGRP = 0010, S_IRWXO = 0007, S_IROTH = 0004, S_IWOTH = 0002, S_IXOTH = 0001, S_IFDIR = 0040000;
    
    // Adicionar constante para SEEK
    private static final int SEEK_SET = 0, SEEK_CUR = 1, SEEK_END = 2;

    // Flags de open
    private static final int O_RDONLY = 0, O_WRONLY = 1, O_RDWR = 2, O_CREAT = 64, O_TRUNC = 512, O_APPEND = 1024, O_DIRECTORY = 0x10000;
    
    // Flags mmap
    private static final int PROT_NONE = 0, PROT_READ = 1, PROT_WRITE = 2, PROT_EXEC = 4, MAP_SHARED = 1, MAP_PRIVATE = 2, MAP_FIXED = 16, MAP_ANONYMOUS = 32;

    // Dynamic linking constants - adicione com as outras constantes ELF
    private static final int DT_NULL = 0, DT_NEEDED = 1, DT_PLTRELSZ = 2, DT_PLTGOT = 3, DT_HASH = 4, DT_STRTAB = 5, DT_SYMTAB = 6, DT_RELA = 7, DT_RELASZ = 8, DT_RELAENT = 9, DT_STRSZ = 10, DT_SYMENT = 11, DT_INIT = 12, DT_FINI = 13, DT_SONAME = 14, DT_RPATH = 15, DT_SYMBOLIC = 16, DT_REL = 17, DT_RELSZ = 18, DT_RELENT = 19, DT_PLTREL = 20, DT_DEBUG = 21, DT_TEXTREL = 22, DT_JMPREL = 23, DT_BIND_NOW = 24, DT_INIT_ARRAY = 25, DT_FINI_ARRAY = 26, DT_INIT_ARRAYSZ = 27, DT_FINI_ARRAYSZ = 28;

    // stdlib do emulador — "library syscalls" (li a7,#LIB_*; ecall) resolvidas
    // no Java por handleLibraryCall(). Numeros > syscall vm max (295) ficam
    // fora do range das syscalls do kernel, entao podem ser usados como ID.
    private static final int LIB_BASE = 1000;
    private static final int LIB_STRLEN = LIB_BASE + 1, LIB_STRCPY = LIB_BASE + 2, LIB_STRCMP = LIB_BASE + 3,
        LIB_STRNCMP = LIB_BASE + 4, LIB_STRCAT = LIB_BASE + 5, LIB_STRCHR = LIB_BASE + 6, LIB_STRDUP = LIB_BASE + 7,
        LIB_STRNCPY = LIB_BASE + 8, LIB_STRNCAT = LIB_BASE + 9, LIB_MEMCPY = LIB_BASE + 10, LIB_MEMMOVE = LIB_BASE + 11,
        LIB_MEMSET = LIB_BASE + 12, LIB_MEMCMP = LIB_BASE + 13, LIB_MEMCHR = LIB_BASE + 14, LIB_ATOI = LIB_BASE + 15,
        LIB_ABS = LIB_BASE + 16, LIB_PUTCHAR = LIB_BASE + 17, LIB_PUTS = LIB_BASE + 18, LIB_PRINTF = LIB_BASE + 19,
        LIB_SPRINTF = LIB_BASE + 20, LIB_SNPRINTF = LIB_BASE + 21, LIB_WRITE_STRING = LIB_BASE + 22,
        LIB_MALLOC = LIB_BASE + 23, LIB_CALLOC = LIB_BASE + 24, LIB_REALLOC = LIB_BASE + 25, LIB_FREE = LIB_BASE + 26,
        LIB_TOUPPER = LIB_BASE + 27, LIB_TOLOWER = LIB_BASE + 28, LIB_GETPID = LIB_BASE + 29,
        LIB_UDIV32 = LIB_BASE + 30, LIB_SDIV32 = LIB_BASE + 31, LIB_UDIVMOD32 = LIB_BASE + 32,
        LIB_SDIVMOD32 = LIB_BASE + 33, LIB_UDIVMOD64 = LIB_BASE + 34, LIB_SDIVMOD64 = LIB_BASE + 35,
        LIB_MEMCLR = LIB_BASE + 36, LIB_MEMCPY_ALIGN = LIB_BASE + 37, LIB_MEMSET_ALIGN = LIB_BASE + 38;

    // Relocation types
    private static final int R_RISCV_NONE = 0, R_RISCV_32 = 1, R_RISCV_RELATIVE = 3, R_RISCV_COPY = 4, R_RISCV_JUMP_SLOT = 5, R_RISCV_GLOB_DAT = 6;
        
    // Constantes fcntl
    private static final int F_GETFL = 3, F_SETFL = 4, O_NONBLOCK = 2048;
    
    // Informações do ELF carregado
    private Hashtable elfInfo;
    
    
    // Stack de sinais
    private Vector signalStack;
    
    // Futex management
    private Hashtable futexWaiters, args;
    
    public ELF(OpenTTY midlet, Hashtable args, Object stdout, Hashtable scope, int id, String pid, Process proc) {
        this.midlet = midlet; this.stdout = stdout; this.id = id;
        this.scope = scope; this.proc = proc; this.args = args;
        this.pid = pid == null ? midlet.genpid() : pid;
        this.memory = new byte[1 * 1024 * 1024]; 
        this.registers = new int[32];
        this.running = false;
        this.nextJmpBufId = 1;
        this.stackPointer = memory.length - 1024;
        this.jmpBufs = new Hashtable();
        this.socketDescriptors = new Hashtable();
        this.allocatedBlocks = new Hashtable();
        this.fileDescriptors = new Hashtable();
        this.nextFd = 3; // 0=stdin, 1=stdout, 2=stderr
        this.heapStart = 0x40000; // 256KB - início do heap (dentro dos 1MB de RAM)
        this.heapEnd = heapStart;
        this.signalStack = new Vector();
        this.memoryMappings = new Vector();
        this.loadedLibraries = new Vector();
        this.elfInfo = new Hashtable();
        this.futexWaiters = new Hashtable();
        this.dynamicSymbols = new Hashtable();
        this.neededLibraries = new Hashtable();
        this.globalSymbols = new Hashtable();
        this.sharedObjectMappings = new Vector();
        this.dynSymNames = new Vector();
        this.copyRelocs = new Hashtable();
        this.libSymSizes = new Hashtable();
        this.pltGotAddr = 0;
        this.dynamicSectionAddr = 0;
        this.gotBase = 0;
        this.libcHeapFree = 0;
        this.libcHeapTop = 0;
        this.libcHeapRegionEnd = 0;

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
    
    public boolean load(InputStream is) throws Exception { ByteArrayOutputStream baos = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int bytesRead; while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); } is.close(); return load(baos.toByteArray()); }
    public boolean load(byte[] elfData) throws Exception {
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { midlet.print("Not a valid ELF file", stdout, id, scope); return false; }
        if (elfData[4] != ELFCLASS32) { midlet.print("Only 32-bit ELF supported", stdout, id, scope); return false; }
        if (elfData[5] != ELFDATA2LSB) { midlet.print("Only little-endian ELF supported", stdout, id, scope); return false; }
        
        int e_type = readShortLE(elfData, 16), e_machine = readShortLE(elfData, 18), e_entry = readIntLE(elfData, 24), e_phoff = readIntLE(elfData, 28), e_shoff = readIntLE(elfData, 32), e_phnum = readShortLE(elfData, 44), e_shnum = readShortLE(elfData, 48), e_phentsize = readShortLE(elfData, 42), e_shentsize = readShortLE(elfData, 46);
        
        if (e_type != ET_EXEC) { midlet.print("Not an executable ELF", stdout, id, scope); return false; }
        if (e_machine != EM_RISCV) { midlet.print("Not a RISC-V executable", stdout, id, scope); return false; }
        
        // Armazenar informações do ELF
        elfInfo.put("entry", new Integer(e_entry)); elfInfo.put("phoff", new Integer(e_phoff));
        elfInfo.put("phnum", new Integer(e_phnum)); elfInfo.put("shoff", new Integer(e_shoff));
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
            int phdrOffset = e_phoff + i * e_phentsize, p_type = readIntLE(elfData, phdrOffset);
            
            if (p_type == PT_LOAD) {
                int p_offset = readIntLE(elfData, phdrOffset + 4), p_vaddr = readIntLE(elfData, phdrOffset + 8), p_filesz = readIntLE(elfData, phdrOffset + 16), p_memsz = readIntLE(elfData, phdrOffset + 20);
                
                // Carregar dados do arquivo
                for (int j = 0; j < p_filesz && j < memory.length; j++) { if (p_vaddr + j < memory.length) { memory[p_vaddr + j] = elfData[p_offset + j]; } }
                
                // Zerar memória restante (.bss)
                for (int j = p_filesz; j < p_memsz; j++) { if (p_vaddr + j < memory.length) { memory[p_vaddr + j] = 0; } }
            }
            else if (p_type == PT_DYNAMIC) { processDynamicSegment(elfData, phdrOffset); }
            else if (p_type == PT_INTERP) {
                // Interpretador (loader dinâmico) - ignorado por enquanto
                int p_offset = readIntLE(elfData, phdrOffset + 4);
                String interp = readString(elfData, p_offset, 256);
                if (midlet.debug) { midlet.print("Interpreter: " + interp, stdout, id, scope); }
            }
        }

        processDynamicSection(elfData); processSymbols(elfData); scanCopyRelocations(elfData);
        if (!loadNeededLibraries()) { return false; }
        applyCopyRelocations();
        setupCRTStack();
        executeLibraryInitFunctions();
        processSymbolsAndRelocations(elfData, sectionInfo); setupPLTGOT();
        executeInitFunctions();
        
        return true;
    }
    private void scanCopyRelocations(byte[] elfData) {
        if (!elfInfo.containsKey("rel") || !elfInfo.containsKey("relsz")) { return; }
        int relAddr = ((Integer)elfInfo.get("rel")).intValue(), relsz = ((Integer)elfInfo.get("relsz")).intValue();
        int relent = elfInfo.containsKey("relent") ? ((Integer) elfInfo.get("relent")).intValue() : 8;
        if (relent != 8 && relent != 12) { return; }
        for (int i = 0; i < relsz; i += relent) {
            int offset = relAddr + i, r_offset = readIntLE(memory, offset), r_info = readIntLE(memory, offset + 4), symIndex = r_info >> 8, type = r_info & 0xFF;
            if (type == R_RISCV_COPY) {
                String nm = getSymbolNameByIndex(symIndex);
                if (nm != null) { copyRelocs.put(nm, new Integer(r_offset)); if (midlet.debug) { midlet.print("COPY reloc: " + nm + " home at " + toHex(r_offset), stdout, id, scope); } }
            }
        }
    }
    private void applyCopyRelocations() {
        for (Enumeration e = copyRelocs.keys(); e.hasMoreElements();) {
            String copyName = (String) e.nextElement();
            int home = ((Integer) copyRelocs.get(copyName)).intValue();
            Hashtable libTab = resolveLibrarySymbolTable(copyName);
            if (libTab == null) { continue; }
            Object symValue = libTab.get(copyName);
            if (!(symValue instanceof Integer)) { continue; }
            int src = ((Integer) symValue).intValue(), copySize = librarySymbolSize(copyName);
            for (int k = 0; k < copySize && home + k < memory.length && src + k < memory.length; k++) { memory[home + k] = memory[src + k]; }
            if (dynamicSymbols.containsKey(copyName)) {
                Hashtable dyn = (Hashtable) dynamicSymbols.get(copyName);
                dyn.put("value", new Integer(home));
                dyn.put("size", new Integer(copySize));
            }
            if (midlet.debug) { midlet.print("Reloc COPY: " + copyName + " (" + copySize + " bytes) -> " + toHex(home) + " from " + toHex(src), stdout, id, scope); }
        }
    }

    private void processDynamicSection(byte[] elfData) {
        int e_phoff = ((Integer)elfInfo.get("phoff")).intValue(), e_phnum = ((Integer)elfInfo.get("phnum")).intValue();
        
        for (int i = 0; i < e_phnum; i++) {
            int phdrOffset = e_phoff + i * 32, p_type = readIntLE(elfData, phdrOffset);
            
            if (p_type == PT_DYNAMIC) {
                dynamicSectionAddr = readIntLE(elfData, phdrOffset + 8);
                int p_filesz = readIntLE(elfData, phdrOffset + 16);
                processDynamicEntries(memory, dynamicSectionAddr, p_filesz);
                break;
            }
        }
    }
    private void processDynamicEntries(byte[] mem, int dynAddr, int dynSize) {
        int offset = 0;
        Vector neededOffsets = new Vector();
        
        while (offset < dynSize) {
            int tag = readIntLE(mem, dynAddr + offset), val = readIntLE(mem, dynAddr + offset + 4);
            if (tag == DT_NULL) { break; }
            
            switch (tag) {
                case DT_NEEDED:
                    neededOffsets.addElement(new Integer(val));
                    break;
                    
                case DT_PLTGOT:
                    pltGotAddr = val;
                    if (midlet.debug) { midlet.print("PLT/GOT at: " + toHex(val), stdout, id, scope); }
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

                case DT_RELA:
                    elfInfo.put("rel", new Integer(val));
                    break;

                case DT_RELASZ:
                    elfInfo.put("relsz", new Integer(val));
                    break;

                case DT_RELAENT:
                    elfInfo.put("relent", new Integer(val));
                    break;
                    
                case DT_INIT:
                    elfInfo.put("init", new Integer(val));
                    break;
                    
                case DT_FINI:
                    elfInfo.put("fini", new Integer(val));
                    break;

                case DT_INIT_ARRAY:
                    elfInfo.put("init_array", new Integer(val));
                    break;

                case DT_INIT_ARRAYSZ:
                    elfInfo.put("init_arraysz", new Integer(val));
                    break;

                case DT_FINI_ARRAY:
                    elfInfo.put("fini_array", new Integer(val));
                    break;

                case DT_FINI_ARRAYSZ:
                    elfInfo.put("fini_arraysz", new Integer(val));
                    break;
                    
                case DT_HASH:
                    processHashTable(mem, val);
                    break;
            }
            
            offset += 8;
        }
        
        if (neededOffsets.size() > 0 && elfInfo.containsKey("dynstr")) {
            int dynstrAddr = ((Integer)elfInfo.get("dynstr")).intValue();
            for (int i = 0; i < neededOffsets.size(); i++) {
                String libName = readString(mem, dynstrAddr + ((Integer)neededOffsets.elementAt(i)).intValue(), 256);
                if (libName != null && libName.length() > 0 && !neededLibraries.containsKey(libName)) {
                    neededLibraries.put(libName, new Integer(((Integer)neededOffsets.elementAt(i)).intValue()));
                    if (midlet.debug) { midlet.print("Needed library: " + libName, stdout, id, scope); }
                }
            }
        }
    }
    private void processHashTable(byte[] elfData, int hashAddr) { int nbucket = readIntLE(elfData, hashAddr), nchain = readIntLE(elfData, hashAddr + 4); elfInfo.put("nbucket", new Integer(nbucket)); elfInfo.put("nchain", new Integer(nchain)); elfInfo.put("buckets", new Integer(hashAddr + 8)); elfInfo.put("chains", new Integer(hashAddr + 8 + nbucket * 4)); }

    private boolean loadNeededLibraries() {
        boolean loaded = true;
        Enumeration libNames = neededLibraries.keys();
        while (libNames.hasMoreElements()) {
            String libName = (String) libNames.nextElement();
            
            if (loadedLibraries.contains(libName)) { }
            else { if (loadLibrary(libName)) { if (midlet.debug) { midlet.print("Loaded library: " + libName, stdout, id, scope); } } else { midlet.print("Missing shared library: " + libName, stdout, id, scope); loaded = false; } }
        }
        return loaded;
    }
    private boolean loadLibrary(String libName) {
        if (loadedLibraries.contains(libName)) { return true; }
        String[] paths = { midlet.joinpath(libName, scope), "/lib/" + libName, "/bin/" + libName };
        byte[] libData = null;
        for (int i = 0; i < paths.length; i++) {
            try {
                InputStream is = midlet.getInputStream(paths[i], scope);
                if (is != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
                    is.close();
                    libData = baos.toByteArray();
                    break;
                }
            } catch (Exception e) { if (midlet.debug) { midlet.print("lib read error: " + libName + " (" + e + ")", stdout, id, scope); } }
        }
        if (libData == null || !isSharedElf(libData)) { return false; }
        if (loadSharedObject(libName, libData)) { return true; }
        return false;
    }
    private boolean isSharedElf(byte[] d) {
        if (d.length < 20 || d[0] != 0x7F || d[1] != 'E' || d[2] != 'L' || d[3] != 'F') { return false; }
        if (d[4] != ELFCLASS32 || d[5] != ELFDATA2LSB) { return false; }
        if (readShortLE(d, 16) != ET_DYN) { return false; }
        if (readShortLE(d, 18) != EM_RISCV) { return false; }
        return true;
    }
    private boolean loadSharedObject(String libName, byte[] elfData) {
        int mappingCount = sharedObjectMappings.size(), libraryCount = loadedLibraries.size();
        int e_phoff = readIntLE(elfData, 28), e_phnum = readShortLE(elfData, 44) & 0xFFFF, e_phentsize = readShortLE(elfData, 42) & 0xFFFF;
        int minVaddr = memory.length, maxVaddr = 0, dynAddr = 0, dynSize = 0;
        for (int i = 0; i < e_phnum; i++) {
            int off = e_phoff + i * e_phentsize, p_type = readIntLE(elfData, off);
            if (p_type == PT_LOAD) {
                int p_offset = readIntLE(elfData, off + 4), p_vaddr = readIntLE(elfData, off + 8), p_filesz = readIntLE(elfData, off + 16), p_memsz = readIntLE(elfData, off + 20);
                if (p_offset < 0 || p_filesz < 0 || p_memsz < p_filesz || p_offset + p_filesz > elfData.length || p_vaddr < 0 || p_vaddr + p_memsz < p_vaddr) { return false; }
                if (p_vaddr < minVaddr) { minVaddr = p_vaddr; }
                if (p_vaddr + p_memsz > maxVaddr) { maxVaddr = p_vaddr + p_memsz; }
            } else if (p_type == PT_DYNAMIC) { dynAddr = readIntLE(elfData, off + 8); dynSize = readIntLE(elfData, off + 16); }
        }
        if (minVaddr == memory.length || maxVaddr <= minVaddr || dynAddr == 0 || dynSize <= 0) { return false; }

        int runtimeBase = findFreeMemoryRegion(maxVaddr - minVaddr);
        if (runtimeBase == 0) { return false; }
        int loadBias = runtimeBase - minVaddr;
        Hashtable mapping = new Hashtable();
        mapping.put("addr", new Integer(runtimeBase));
        mapping.put("length", new Integer(maxVaddr - minVaddr));
        sharedObjectMappings.addElement(mapping);

        for (int i = 0; i < e_phnum; i++) {
            int off = e_phoff + i * e_phentsize, p_type = readIntLE(elfData, off);
            if (p_type == PT_LOAD) {
                int p_offset = readIntLE(elfData, off + 4), p_vaddr = readIntLE(elfData, off + 8), p_filesz = readIntLE(elfData, off + 16), p_memsz = readIntLE(elfData, off + 20), target = loadBias + p_vaddr;
                for (int j = 0; j < p_filesz; j++) { memory[target + j] = elfData[p_offset + j]; }
                for (int j = p_filesz; j < p_memsz; j++) { memory[target + j] = 0; }
            }
        }
        dynAddr += loadBias;
        
        int symtab = 0, strtab = 0, syment = 16, rel = 0, relsz = 0, jmprel = 0, pltrelsz = 0, pltrel = DT_REL, hashAddr = 0, init = 0, initArray = 0, initArraySize = 0, fini = 0, finiArray = 0, finiArraySize = 0, libRelent = 8;
        Vector neededOffsets = new Vector();
        int offset = 0;
        while (offset + 8 <= dynSize) {
            int tag = readIntLE(memory, dynAddr + offset), val = readIntLE(memory, dynAddr + offset + 4);
            if (tag == DT_NULL) { break; }
            switch (tag) {
                case DT_NEEDED: neededOffsets.addElement(new Integer(val)); break;
                case DT_SYMTAB: symtab = loadBias + val; break;
                case DT_STRTAB: strtab = loadBias + val; break;
                case DT_SYMENT: syment = val; break;
                case DT_REL: rel = loadBias + val; break;
                case DT_RELSZ: relsz = val; break;
                case DT_RELA: rel = loadBias + val; break;
                case DT_RELASZ: relsz = val; break;
                case DT_RELENT: libRelent = val; break;
                case DT_RELAENT: libRelent = val; break;
                case DT_JMPREL: jmprel = loadBias + val; break;
                case DT_PLTRELSZ: pltrelsz = val; break;
                case DT_PLTREL: pltrel = val; break;
                case DT_HASH: hashAddr = loadBias + val; break;
                case DT_INIT: init = loadBias + val; break;
                case DT_INIT_ARRAY: initArray = loadBias + val; break;
                case DT_INIT_ARRAYSZ: initArraySize = val; break;
                case DT_FINI: fini = loadBias + val; break;
                case DT_FINI_ARRAY: finiArray = loadBias + val; break;
                case DT_FINI_ARRAYSZ: finiArraySize = val; break;
            }
            offset += 8;
        }
        if (symtab == 0 || strtab == 0) { rollbackSharedObjectLoad(mappingCount, libraryCount); return false; }
        
        // Registrar os símbolos exportados (st_shndx != 0, GLOBAL/WEAK) e o índice->nome
        Hashtable libSyms = new Hashtable();
        Hashtable sizeMap = new Hashtable();
        Vector symNames = new Vector();
        int s = symtab, symCountMax = 4096;
        if (hashAddr != 0) {
            int nchain = readIntLE(memory, hashAddr + 4);
            if (nchain > 0 && nchain <= 8192) { symCountMax = nchain; }
        }
        for (int dynSymCount = 0; dynSymCount < symCountMax; dynSymCount++) {
            int st_name = readIntLE(memory, s), st_value = readIntLE(memory, s + 4), st_size = readIntLE(memory, s + 8), st_info = memory[s + 12] & 0xFF, st_shndx = readShortLE(memory, s + 14) & 0xFFFF;
            if (s != symtab && st_name == 0 && st_value == 0 && st_info == 0 && st_shndx == 0) { break; }
            String nm = (st_name == 0) ? "" : readString(memory, strtab + st_name, 256);
            if (nm == null) { break; }
            symNames.addElement(nm);
            int bind = (st_info >> 4) & 0xF;
            if (st_shndx != 0 && (bind == 1 || bind == 2)) { libSyms.put(nm, new Integer(loadBias + st_value)); if (st_size > 0) { sizeMap.put(nm, new Integer(st_size)); } }
            s += syment;
        }
        globalSymbols.put(libName, libSyms);
        loadedLibraries.addElement(libName);
        if (sizeMap.size() > 0) { libSymSizes.put(libName, sizeMap); }
        if (init != 0) { mapping.put("init", new Integer(init)); }
        if (initArray != 0 && initArraySize > 0) { mapping.put("init_array", new Integer(initArray)); mapping.put("init_arraysz", new Integer(initArraySize)); }
        if (fini != 0) { mapping.put("fini", new Integer(fini)); }
        if (finiArray != 0 && finiArraySize > 0) { mapping.put("fini_array", new Integer(finiArray)); mapping.put("fini_arraysz", new Integer(finiArraySize)); }
        if (midlet.debug) { midlet.print("Loaded shared object: " + libName + " (" + symNames.size() + " dynsyms)", stdout, id, scope); }

        for (int i = 0; i < neededOffsets.size(); i++) {
            String needed = readString(memory, strtab + ((Integer) neededOffsets.elementAt(i)).intValue(), 256);
            if (needed.length() > 0 && !loadedLibraries.contains(needed) && !loadLibrary(needed)) { rollbackSharedObjectLoad(mappingCount, libraryCount); return false; }
        }
        
        // Aplicar as relocacoes da propria lib (.rel.dyn/.rela.dyn e .rel.plt/.rela.plt)
        applyLibraryRelocations(rel, relsz, libRelent, symNames, loadBias);
        if (jmprel != 0 && pltrelsz != 0) { applyLibraryRelocations(jmprel, pltrelsz, (pltrel == DT_RELA) ? 12 : 8, symNames, loadBias); }
        return true;
    }
    private void rollbackSharedObjectLoad(int mappingCount, int libraryCount) {
        while (sharedObjectMappings.size() > mappingCount) {
            Hashtable mapping = (Hashtable) sharedObjectMappings.elementAt(sharedObjectMappings.size() - 1);
            int address = ((Integer) mapping.get("addr")).intValue(), length = ((Integer) mapping.get("length")).intValue();
            for (int i = 0; i < length; i++) { memory[address + i] = 0; }
            sharedObjectMappings.removeElementAt(sharedObjectMappings.size() - 1);
        }
        while (loadedLibraries.size() > libraryCount) {
            String library = (String) loadedLibraries.elementAt(loadedLibraries.size() - 1);
            globalSymbols.remove(library);
            libSymSizes.remove(library);
            loadedLibraries.removeElementAt(loadedLibraries.size() - 1);
        }
    }
    private void applyLibraryRelocations(int relAddr, int relsz, int relent, Vector symNames, int loadBias) {
        for (int i = 0; i < relsz && relAddr + i + relent <= memory.length; i += relent) {
            int r_offset = loadBias + readIntLE(memory, relAddr + i), r_info = readIntLE(memory, relAddr + i + 4), symIndex = r_info >> 8, type = r_info & 0xFF, addend = (relent == 12) ? readIntLE(memory, relAddr + i + 8) : 0;
            switch (type) {
                case R_RISCV_32:
                case R_RISCV_GLOB_DAT:
                case R_RISCV_JUMP_SLOT: {
                    String nm = (symIndex < symNames.size()) ? (String) symNames.elementAt(symIndex) : null;
                    Integer addr = (nm != null) ? resolveSymbol(nm) : null;
                    if (addr != null) { writeIntLE(memory, r_offset, addr.intValue() + addend); if (midlet.debug) { midlet.print("lib reloc: " + nm + " -> " + toHex(addr.intValue()) + " at " + toHex(r_offset), stdout, id, scope); } }
                    break;
                }
                case R_RISCV_RELATIVE: {
                    int cur = readIntLE(memory, r_offset);
                    writeIntLE(memory, r_offset, (relent == 12) ? loadBias + addend : loadBias + cur);
                    if (midlet.debug) { midlet.print("lib reloc RELATIVE at " + toHex(r_offset), stdout, id, scope); }
                    break;
                }
            }
        }
    }

    private void loadDefaultLibraries() {
        Hashtable libc = new Hashtable();

        // stdlib do emulador: cada simbolo e um wrapper de 3 instrucoes
        // (li a7,#LIB_*; ecall; ret) cujo payload e implementado em handleLibraryCall().
        libc.put("strlen",   new Integer(createLibraryStub(LIB_STRLEN)));
        libc.put("strcpy",   new Integer(createLibraryStub(LIB_STRCPY)));
        libc.put("strncpy",  new Integer(createLibraryStub(LIB_STRNCPY)));
        libc.put("strcmp",   new Integer(createLibraryStub(LIB_STRCMP)));
        libc.put("strncmp",  new Integer(createLibraryStub(LIB_STRNCMP)));
        libc.put("strcat",   new Integer(createLibraryStub(LIB_STRCAT)));
        libc.put("strncat",  new Integer(createLibraryStub(LIB_STRNCAT)));
        libc.put("strchr",   new Integer(createLibraryStub(LIB_STRCHR)));
        libc.put("strdup",   new Integer(createLibraryStub(LIB_STRDUP)));
        libc.put("memcpy",   new Integer(createLibraryStub(LIB_MEMCPY)));
        libc.put("memmove",  new Integer(createLibraryStub(LIB_MEMMOVE)));
        libc.put("memset",   new Integer(createLibraryStub(LIB_MEMSET)));
        libc.put("memcmp",   new Integer(createLibraryStub(LIB_MEMCMP)));
        libc.put("memchr",   new Integer(createLibraryStub(LIB_MEMCHR)));
        libc.put("atoi",     new Integer(createLibraryStub(LIB_ATOI)));
        libc.put("abs",      new Integer(createLibraryStub(LIB_ABS)));
        libc.put("toupper",  new Integer(createLibraryStub(LIB_TOUPPER)));
        libc.put("tolower",  new Integer(createLibraryStub(LIB_TOLOWER)));
        libc.put("putchar",  new Integer(createLibraryStub(LIB_PUTCHAR)));
        libc.put("puts",     new Integer(createLibraryStub(LIB_PUTS)));
        libc.put("printf",   new Integer(createLibraryStub(LIB_PRINTF)));
        libc.put("sprintf",  new Integer(createLibraryStub(LIB_SPRINTF)));
        libc.put("snprintf", new Integer(createLibraryStub(LIB_SNPRINTF)));
        libc.put("malloc",   new Integer(createLibraryStub(LIB_MALLOC)));
        libc.put("calloc",   new Integer(createLibraryStub(LIB_CALLOC)));
        libc.put("realloc",  new Integer(createLibraryStub(LIB_REALLOC)));
        libc.put("free",     new Integer(createLibraryStub(LIB_FREE)));
        libc.put("getpid",   new Integer(createLibraryStub(LIB_GETPID)));

        // Runtime helpers que compiladores C (clang/gcc RV32IM) emitem
        // implicitamente para divisao por 32/64 bits e blits de memoria.
        libc.put("__udivsi3",     new Integer(createLibraryStub(LIB_UDIV32)));
        libc.put("__divsi3",      new Integer(createLibraryStub(LIB_SDIV32)));
        libc.put("__udivmodsi4",  new Integer(createLibraryStub(LIB_UDIVMOD32)));
        libc.put("__divmodsi4",   new Integer(createLibraryStub(LIB_SDIVMOD32)));
        libc.put("__udivmoddi4",  new Integer(createLibraryStub(LIB_UDIVMOD64)));
        libc.put("__divmoddi4",   new Integer(createLibraryStub(LIB_SDIVMOD64)));
        libc.put("__memclr",      new Integer(createLibraryStub(LIB_MEMCLR)));
        libc.put("__memcpy",      new Integer(createLibraryStub(LIB_MEMCPY_ALIGN)));
        libc.put("__memset",      new Integer(createLibraryStub(LIB_MEMSET_ALIGN)));

        // syscalls diretas (open/read/write/close/exit/brk) como antes
        libc.put("exit",  new Integer(createSyscallStub("exit")));
        libc.put("open",  new Integer(createSyscallStub("open")));
        libc.put("read",  new Integer(createSyscallStub("read")));
        libc.put("write", new Integer(createSyscallStub("write")));
        libc.put("close", new Integer(createSyscallStub("close")));

        globalSymbols.put("libc.so.6", libc); loadedLibraries.addElement("libc.so.6");

        if (midlet.debug) { midlet.print("Loaded default libraries", stdout, id, scope); }
    }

    private int createLibraryStub(int libId) {
        int stubAddr = findFreeMemoryRegion(32);
        if (stubAddr == 0) { return 0; }
        writeIntLE(memory, stubAddr, ((libId & 0xFFF) << 20) | (17 << 7) | 0x13); // addi a7, zero, #libId
        writeIntLE(memory, stubAddr + 4, 0x00000073); // ecall
        writeIntLE(memory, stubAddr + 8, 0x00008067); // ret
        return stubAddr;
    }

    // | stdlib do emulador — implementacao das library syscalls (LIB_*)
    // Argumentos chegam em a0-a3 (e stack nos varargs); retorno em a0.

    private void handleLibraryCall(int libId) {
        switch (libId) {
            case LIB_STRLEN - LIB_BASE: registers[REG_A0] = libcStrlen(registers[REG_A0]); break;
            case LIB_STRCPY - LIB_BASE: libcStrcpy(registers[REG_A0], registers[REG_A1]); break;
            case LIB_STRNCPY - LIB_BASE: libcStrncpy(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_STRCMP - LIB_BASE: registers[REG_A0] = libcStrcmp(registers[REG_A0], registers[REG_A1]); break;
            case LIB_STRNCMP - LIB_BASE: registers[REG_A0] = libcStrncmp(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_STRCAT - LIB_BASE: { int d = registers[REG_A0]; libcStrcpy(d + libcStrlen(d), registers[REG_A1]); registers[REG_A0] = d; break; }
            case LIB_STRNCAT - LIB_BASE: { int d = registers[REG_A0]; libcStrncat(d, registers[REG_A1], registers[REG_A2]); registers[REG_A0] = d; break; }
            case LIB_STRCHR - LIB_BASE: registers[REG_A0] = libcStrchr(registers[REG_A0], registers[REG_A1]); break;
            case LIB_STRDUP - LIB_BASE: registers[REG_A0] = libcStrdup(registers[REG_A0]); break;
            case LIB_MEMCPY - LIB_BASE: registers[REG_A0] = libcMemcpy(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_MEMMOVE - LIB_BASE: registers[REG_A0] = libcMemmove(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_MEMSET - LIB_BASE: registers[REG_A0] = libcMemset(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_MEMCMP - LIB_BASE: registers[REG_A0] = libcMemcmp(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_MEMCHR - LIB_BASE: registers[REG_A0] = libcMemchr(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_ATOI - LIB_BASE: registers[REG_A0] = libcAtoi(registers[REG_A0]); break;
            case LIB_ABS - LIB_BASE: { int v = registers[REG_A0]; registers[REG_A0] = v < 0 ? -v : v; break; }
            case LIB_TOUPPER - LIB_BASE: { int v = registers[REG_A0]; registers[REG_A0] = (v >= 'a' && v <= 'z') ? v - 32 : v; break; }
            case LIB_TOLOWER - LIB_BASE: { int v = registers[REG_A0]; registers[REG_A0] = (v >= 'A' && v <= 'Z') ? v + 32 : v; break; }
            case LIB_PUTCHAR - LIB_BASE: libcWriteChar(registers[REG_A0] & 0xFF); break;
            case LIB_PUTS - LIB_BASE: { String s = libcReadCString(registers[REG_A0]); libcWriteOut(s + "\n"); registers[REG_A0] = s.length() + 1; break; }
            case LIB_WRITE_STRING - LIB_BASE: { String s = libcReadCString(registers[REG_A0]); libcWriteOut(s); registers[REG_A0] = s.length(); break; }
            case LIB_PRINTF - LIB_BASE: { String s = libcFormat(registers[REG_A0], 11); libcWriteOut(s); registers[REG_A0] = s.length(); break; }
            case LIB_SPRINTF - LIB_BASE: { String s = libcFormat(registers[REG_A1], 12); libcWriteCString(registers[REG_A0], s); registers[REG_A0] = s.length(); break; }
            case LIB_SNPRINTF - LIB_BASE: { String s = libcFormat(registers[REG_A2], 13); libcWriteCStringN(registers[REG_A0], s, registers[REG_A1]); registers[REG_A0] = s.length(); break; }
            case LIB_MALLOC - LIB_BASE: registers[REG_A0] = libcMalloc(registers[REG_A0]); break;
            case LIB_CALLOC - LIB_BASE: registers[REG_A0] = libcCalloc(registers[REG_A0], registers[REG_A1]); break;
            case LIB_REALLOC - LIB_BASE: registers[REG_A0] = libcRealloc(registers[REG_A0], registers[REG_A1]); break;
            case LIB_FREE - LIB_BASE: libcFree(registers[REG_A0]); registers[REG_A0] = 0; break;
            case LIB_GETPID - LIB_BASE: { try { registers[REG_A0] = Integer.parseInt(pid); } catch (NumberFormatException e) { registers[REG_A0] = 1; } break; }
            case LIB_UDIV32 - LIB_BASE: registers[REG_A0] = libcUdiv(registers[REG_A0], registers[REG_A1]); break;
            case LIB_SDIV32 - LIB_BASE: registers[REG_A0] = libcSdiv(registers[REG_A0], registers[REG_A1]); break;
            case LIB_UDIVMOD32 - LIB_BASE: libcUdivmod(registers[REG_A0], registers[REG_A1]); break;
            case LIB_SDIVMOD32 - LIB_BASE: libcSdivmod(registers[REG_A0], registers[REG_A1]); break;
            case LIB_UDIVMOD64 - LIB_BASE: libcUldivmod(registers[REG_A0], registers[REG_A1], registers[REG_A2], registers[REG_A3]); break;
            case LIB_SDIVMOD64 - LIB_BASE: libcLdivmod(registers[REG_A0], registers[REG_A1], registers[REG_A2], registers[REG_A3]); break;
            case LIB_MEMCLR - LIB_BASE: registers[REG_A0] = libcMemset(registers[REG_A0], 0, registers[REG_A1]); break;
            case LIB_MEMCPY_ALIGN - LIB_BASE: registers[REG_A0] = libcMemcpy(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            case LIB_MEMSET_ALIGN - LIB_BASE: registers[REG_A0] = libcMemset(registers[REG_A0], registers[REG_A1], registers[REG_A2]); break;
            default: registers[REG_A0] = -1; break;
        }
    }

    private int memByte(int addr) { return (addr >= 0 && addr < memory.length) ? (memory[addr] & 0xFF) : -1; }

    private String libcReadCString(int p) {
        StringBuffer sb = new StringBuffer();
        int i = 0;
        while (p + i >= 0 && p + i < memory.length && i < 65536) {
            int b = memory[p + i] & 0xFF;
            if (b == 0) { break; }
            sb.append((char) b);
            i++;
        }
        return sb.toString();
    }

    private void libcWriteOut(String s) { midlet.print(s, stdout, id, scope, false); }
    private void libcWriteChar(int c) { midlet.print(String.valueOf((char) c), stdout, id, scope, false); }
    private void libcWriteCString(int addr, String s) {
        int n = Math.min(s.length(), 65536);
        for (int i = 0; i < n; i++) {
            if (addr + i >= memory.length) { return; }
            memory[addr + i] = (byte) s.charAt(i);
        }
        if (addr + n < memory.length) { memory[addr + n] = 0; }
    }
    private void libcWriteCStringN(int addr, String s, int max) {
        if (max <= 0) { return; }
        int n = Math.min(s.length(), max - 1);
        for (int i = 0; i < n; i++) {
            if (addr + i >= memory.length) { return; }
            memory[addr + i] = (byte) s.charAt(i);
        }
        if (addr + n < memory.length) { memory[addr + n] = 0; }
    }

    private int libcStrlen(int p) {
        int n = 0;
        while (p + n >= 0 && p + n < memory.length && memory[p + n] != 0 && n < 65536) { n++; }
        return n;
    }
    private void libcStrcpy(int dst, int src) {
        if (dst < 0 || dst >= memory.length) { return; }
        int d = dst;
        while (true) {
            int b = memByte(src++);
            if (b < 0 || d >= memory.length) { break; }
            memory[d++] = (byte) b;
            if (b == 0) { break; }
        }
    }
    private void libcStrncpy(int dst, int src, int n) {
        if (n <= 0) { return; }
        int i = 0;
        while (i < n) {
            int b = memByte(src++);
            if (b == 0) {
                while (i < n) { if (dst + i < memory.length) { memory[dst + i] = 0; } i++; }
                return;
            }
            if (dst + i < memory.length) { memory[dst + i] = (byte) b; }
            i++;
        }
    }
    private int libcStrcmp(int a, int b) {
        while (true) {
            int x = memByte(a++), y = memByte(b++);
            if (x < 0 || y < 0) { return 0; }
            if (x != y) { return x - y; }
            if (x == 0) { return 0; }
        }
    }
    private int libcStrncmp(int a, int b, int n) {
        while (n > 0) {
            int x = memByte(a++), y = memByte(b++);
            if (x < 0 || y < 0) { return 0; }
            if (x != y) { return x - y; }
            if (x == 0) { return 0; }
            n--;
        }
        return 0;
    }
    private void libcStrncat(int dst, int src, int n) {
        int d = dst + libcStrlen(dst);
        int i = 0;
        while (i < n) {
            int b = memByte(src + i);
            if (b == 0) { break; }
            if (d >= memory.length) { break; }
            memory[d++] = (byte) b;
            i++;
        }
        if (d < memory.length) { memory[d] = 0; }
    }
    private int libcStrchr(int s, int c) {
        int i = 0;
        while (true) {
            int b = memByte(s + i);
            if (b < 0) { return 0; }
            if (b == c) { return s + i; }
            if (b == 0) { return 0; }
            i++;
        }
    }
    private int libcStrdup(int s) {
        int len = libcStrlen(s);
        int p = libcMalloc(len + 1);
        if (p == 0) { return 0; }
        libcStrcpy(p, s);
        return p;
    }

    private int libcMemcpy(int d, int s, int n) {
        int p = d;
        while (n > 0) {
            int b = memByte(s++);
            if (b < 0 || d >= memory.length) { break; }
            memory[d++] = (byte) b;
            n--;
        }
        return p;
    }
    private int libcMemmove(int d, int s, int n) {
        if (d < s) { return libcMemcpy(d, s, n); }
        int p = d;
        int src = s + n - 1, dst = d + n - 1;
        while (n > 0) {
            int b = memByte(src--);
            if (b < 0 || dst < 0 || dst >= memory.length) { break; }
            memory[dst--] = (byte) b;
            n--;
        }
        return p;
    }
    private int libcMemset(int p, int c, int n) {
        int r = p;
        int cByte = c & 0xFF;
        while (n > 0) {
            if (p >= 0 && p < memory.length) { memory[p] = (byte) cByte; }
            p++;
            n--;
        }
        return r;
    }
    private int libcMemcmp(int a, int b, int n) {
        while (n > 0) {
            int x = memByte(a++), y = memByte(b++);
            if (x != y) { return x - y; }
            n--;
        }
        return 0;
    }
    private int libcMemchr(int s, int c, int n) {
        while (n > 0) {
            int b = memByte(s);
            if (b == c) { return s; }
            if (b < 0) { return 0; }
            s++;
            n--;
        }
        return 0;
    }

    private int libcAtoi(int p) {
        int sign = 1, r = 0;
        int b = memByte(p);
        while (b == ' ' || b == '\t' || b == '\n' || b == '\r' || b == '\f') { p++; b = memByte(p); }
        if (b == '-') { sign = -1; p++; b = memByte(p); }
        else if (b == '+') { p++; b = memByte(p); }
        while (b >= '0' && b <= '9') {
            r = r * 10 + (b - '0');
            p++;
            b = memByte(p);
        }
        return sign * r;
    }

    private int libcUdiv(int a, int b) { long u = a & 0xFFFFFFFFL, v = b & 0xFFFFFFFFL; if (v == 0) { return 0; } return (int) (u / v); }
    private int libcSdiv(int a, int b) { if (b == 0) { return 0; } return a / b; }
    private void libcUdivmod(int a, int b) {
        long u = a & 0xFFFFFFFFL, v = b & 0xFFFFFFFFL;
        if (v == 0) { registers[REG_A0] = 0; registers[REG_A1] = 0; return; }
        long q = u / v, r = u % v;
        registers[REG_A0] = (int) q; registers[REG_A1] = (int) r;
    }
    private void libcSdivmod(int a, int b) {
        if (b == 0) { registers[REG_A0] = 0; registers[REG_A1] = 0; return; }
        int q = a / b, r = a % b;
        registers[REG_A0] = q; registers[REG_A1] = r;
    }
    private boolean ulongGe(long r, long d) {
        long hi = 0x8000000000000000L;
        boolean rn = (r & hi) != 0, dn = (d & hi) != 0;
        if (rn != dn) { return rn; }
        return r >= d;
    }
    private void libcUldivmod(int loA, int hiA, int loB, int hiB) {
        long n = ((long) hiA & 0xFFFFFFFFL) << 32 | ((long) loA & 0xFFFFFFFFL);
        long d = ((long) hiB & 0xFFFFFFFFL) << 32 | ((long) loB & 0xFFFFFFFFL);
        if (d == 0) { registers[REG_A0] = 0; registers[REG_A1] = 0; registers[REG_A2] = 0; registers[REG_A3] = 0; return; }
        long q = 0, r = 0;
        for (int i = 63; i >= 0; i--) {
            r = (r << 1) | ((n >>> i) & 1);
            if (ulongGe(r, d)) { r -= d; q |= 1L << i; }
        }
        registers[REG_A0] = (int) q; registers[REG_A1] = (int) (q >>> 32);
        registers[REG_A2] = (int) r; registers[REG_A3] = (int) (r >>> 32);
    }
    private void libcLdivmod(int loA, int hiA, int loB, int hiB) {
        long a = ((long) hiA << 32) | ((long) loA & 0xFFFFFFFFL);
        long b = ((long) hiB << 32) | ((long) loB & 0xFFFFFFFFL);
        if (b == 0) { registers[REG_A0] = 0; registers[REG_A1] = 0; registers[REG_A2] = 0; registers[REG_A3] = -1; return; }
        long q = a / b, r = a % b;
        registers[REG_A0] = (int) q; registers[REG_A1] = (int) (q >>> 32);
        registers[REG_A2] = (int) r; registers[REG_A3] = (int) (r >>> 32);
    }

    // | heap do stdlib (malloc/calloc/realloc/free): first-fit com splitting,
    // | blocos na RAM do guest com header { int size; int next; } seguido de payload.

    private int libcMalloc(int n) {
        if (n <= 0) { n = 8; }
        n = (n + 3) & ~3;
        int prev = 0, cur = libcHeapFree;
        while (cur != 0) {
            int size = readIntLE(memory, cur);
            int next = readIntLE(memory, cur + 4);
            if (size >= n) {
                int rem = size - n - 8;
                int freeNext;
                if (rem >= 8) {
                    int rest = cur + 8 + n;
                    writeIntLE(memory, rest, rem);
                    writeIntLE(memory, rest + 4, next);
                    freeNext = rest;
                } else { freeNext = next; }
                if (prev == 0) { libcHeapFree = freeNext; } else { writeIntLE(memory, prev + 4, freeNext); }
                writeIntLE(memory, cur, n);
                return cur + 8;
            }
            prev = cur;
            cur = next;
        }
        if (libcHeapRegionEnd == 0 || libcHeapTop + 8 + n > libcHeapRegionEnd) {
            int len = 0x10000, region = 0;
            while (len >= 0x1000) {
                region = findFreeMemoryRegion(len);
                if (region != 0) { break; }
                len >>= 1;
            }
            if (region == 0) { return 0; }
            libcHeapTop = region;
            libcHeapRegionEnd = region + len;
        }
        int top = libcHeapTop;
        int avail = libcHeapRegionEnd - top;
        if (avail < 8 + n) { return 0; }
        writeIntLE(memory, top, avail - 8);
        writeIntLE(memory, top + 4, libcHeapFree);
        libcHeapFree = top;
        libcHeapTop = top + avail;
        return libcMalloc(n);
    }
    private void libcFree(int p) {
        if (p == 0) { return; }
        int hdr = p - 8;
        if (hdr < 0 || hdr + 7 >= memory.length) { return; }
        int size = readIntLE(memory, hdr);
        if (size <= 0 || hdr + 8 + size > memory.length) { return; }
        writeIntLE(memory, hdr + 4, libcHeapFree);
        libcHeapFree = hdr;
    }
    private int libcCalloc(int nmemb, int size) {
        long total = (long) nmemb * (long) size;
        if (nmemb < 0 || size < 0 || total > 0xFFFFFFF0L) { return 0; }
        int n = (int) total;
        if (nmemb != 0 && size != 0 && n == 0) { return 0; }
        int p = libcMalloc((int) ((total + 3) & ~3L));
        if (p == 0) { return 0; }
        int i = 0;
        while (i < n && p + i < memory.length) { memory[p + i] = 0; i++; }
        return p;
    }
    private int libcRealloc(int p, int n) {
        if (p == 0) { return libcMalloc(n); }
        int hdr = p - 8;
        if (hdr < 0 || hdr + 7 >= memory.length) { return 0; }
        int old = readIntLE(memory, hdr);
        if (old >= n) { return p; }
        int np = libcMalloc(n);
        if (np == 0) { return 0; }
        libcMemcpy(np, p, old);
        libcFree(p);
        return np;
    }

    // | printf/sprintf/snprintf — engine de formatacao sobre a RAM do guest

    private int libcArgReg, libcArgSpOff;

    private void libcArgInit(int regBase) { libcArgReg = regBase; libcArgSpOff = 0; }

    private int libcNext32() {
        if (libcArgReg <= 17) { return registers[libcArgReg++]; }
        int addr = registers[REG_SP] + libcArgSpOff;
        libcArgSpOff += 4;
        return (addr >= 0 && addr + 3 < memory.length) ? readIntLE(memory, addr) : 0;
    }
    private long libcNext64() {
        if (libcArgReg <= 17) {
            if ((libcArgReg & 1) != 0) { libcArgReg++; }
            if (libcArgReg + 1 <= 17) {
                long lo = registers[libcArgReg] & 0xFFFFFFFFL;
                long hi = ((long) registers[libcArgReg + 1]) << 32;
                libcArgReg += 2;
                return lo | hi;
            }
            libcArgReg = 18;
        }
        libcArgSpOff = (libcArgSpOff + 7) & ~7;
        int addr = registers[REG_SP] + libcArgSpOff;
        libcArgSpOff += 8;
        if (addr < 0 || addr + 7 >= memory.length) { return 0; }
        long lo = readIntLE(memory, addr) & 0xFFFFFFFFL;
        long hi = ((long) readIntLE(memory, addr + 4)) << 32;
        return lo | hi;
    }

    private void libcAppendUdec64(StringBuffer sb, long v) {
        if (v == 0) { sb.append('0'); return; }
        StringBuffer tmp = new StringBuffer();
        long a = v;
        while (a > 0) { tmp.append((char) ('0' + (a % 10))); a /= 10; }
        for (int i = tmp.length() - 1; i >= 0; i--) { sb.append(tmp.charAt(i)); }
    }
    private void libcAppendDec(StringBuffer sb, int v) {
        if (v < 0) { sb.append('-'); v = -v; }
        libcAppendUdec(sb, v);
    }
    private void libcAppendUdec(StringBuffer sb, int v) { libcAppendUdec64(sb, v & 0xFFFFFFFFL); }
    private void libcAppendDec64(StringBuffer sb, long v) {
        if (v < 0) { sb.append('-'); v = -v; }
        libcAppendUdec64(sb, v);
    }
    private void libcAppendHex64(StringBuffer sb, long v, boolean upper) {
        if (v == 0) { sb.append('0'); return; }
        StringBuffer tmp = new StringBuffer();
        while (v != 0) {
            int d = (int) (v & 0xF);
            char c = (char) (d < 10 ? ('0' + d) : (upper ? ('A' + d - 10) : ('a' + d - 10)));
            tmp.append(c);
            v >>>= 4;
        }
        for (int i = tmp.length() - 1; i >= 0; i--) { sb.append(tmp.charAt(i)); }
    }
    private void libcAppendHex(StringBuffer sb, int v, boolean upper) { libcAppendHex64(sb, v & 0xFFFFFFFFL, upper); }
    private void libcAppendOct(StringBuffer sb, long v) {
        if (v == 0) { sb.append('0'); return; }
        StringBuffer tmp = new StringBuffer();
        while (v != 0) { tmp.append((char) ('0' + (v & 7))); v >>>= 3; }
        for (int i = tmp.length() - 1; i >= 0; i--) { sb.append(tmp.charAt(i)); }
    }

    private String libcFormat(int fmt, int regBase) {
        StringBuffer out = new StringBuffer();
        libcArgInit(regBase);
        int i = 0;
        while (i < 2048) {
            int c = memByte(fmt + i);
            if (c < 0) { break; }
            i++;
            if (c == 0) { break; }
            if (c != '%') { out.append((char) c); continue; }

            int len = 0;
            int spec = memByte(fmt + i);
            while (spec == 'h' || spec == 'l') {
                if (spec == 'l') { len++; } else { len--; }
                i++;
                spec = memByte(fmt + i);
            }
            while (spec == '-' || spec == '+' || spec == ' ' || spec == '0' || spec == '#') { i++; spec = memByte(fmt + i); }
            while (spec >= '0' && spec <= '9') { i++; spec = memByte(fmt + i); }
            if (spec == '.') { i++; spec = memByte(fmt + i); }
            while (spec >= '0' && spec <= '9') { i++; spec = memByte(fmt + i); }
            i++;

            boolean is64 = len >= 2;
            switch (spec < 0 ? -1 : spec) {
                case 'd': case 'i':
                    if (is64) { libcAppendDec64(out, libcNext64()); }
                    else { libcAppendDec(out, libcNext32()); }
                    break;
                case 'u':
                    if (is64) { libcAppendUdec64(out, libcNext64()); }
                    else { libcAppendUdec(out, libcNext32()); }
                    break;
                case 'x': case 'X':
                    if (is64) { libcAppendHex64(out, libcNext64(), spec == 'X'); }
                    else { libcAppendHex(out, libcNext32(), spec == 'X'); }
                    break;
                case 'o':
                    libcAppendOct(out, (long) libcNext32() & 0xFFFFFFFFL);
                    break;
                case 'c':
                    out.append((char) (libcNext32() & 0xFF));
                    break;
                case 's':
                    out.append(libcReadCString(libcNext32()));
                    break;
                case 'p':
                    out.append("0x");
                    libcAppendHex(out, libcNext32(), false);
                    break;
                case 'f': case 'e': case 'g':
                    libcNext64();
                    out.append('0');
                    break;
                case '%':
                    out.append('%');
                    break;
                case 0:
                    break;
                case -1:
                    break;
                default:
                    out.append('%').append((char) (spec < 0 ? '%' : spec));
                    break;
            }
            if (spec == 0) { break; }
        }
        return out.toString();
    }

    // Runtime
    public Hashtable run() {
        running = true;
        Hashtable ITEM = new Hashtable();
        
        try {
            if (midlet.debug) { 
                midlet.print("=== ELF START DEBUG ===", stdout, id, scope);
                midlet.print("PC start: " + toHex(pc), stdout, id, scope);
                midlet.print("SP: " + toHex(registers[REG_SP]), stdout, id, scope);
                midlet.print("Memory: " + memory.length + " bytes", stdout, id, scope);
            }
            
            int instructionCount = 0;
            while (running && pc < memory.length - 3 && midlet.sys.containsKey(pid)) {
                if (instructionCount++ > 1000000) {
                    if (midlet.debug) midlet.print("DEBUG: Stopping after 1000000 instructions", stdout, id, scope);
                    break;
                }
                
                // Verificar sinais pendentes
                checkPendingSignals();
                
                // Debug avançado
                if (midlet.debug && instructionCount % 10000 == 0) {
                    midlet.print("DEBUG: PC=" + toHex(pc) + ", a7=" + registers[REG_A7], stdout, id, scope);
                }
                
                // Executar instrução com cache
                int instruction = fetchInstruction(pc);
                if (midlet.debug && instructionCount < 10) {
                    midlet.print("DEBUG: Instr at PC " + toHex(pc) + ": " + toHex(instruction), stdout, id, scope);
                }
                pc += 4;
                
                try {
                    executeInstruction(instruction);
                } catch (Exception e) {
                    if (midlet.debug) midlet.print("DEBUG: Exception in executeInstruction: " + e, stdout, id, scope);
                    e.printStackTrace();
                    handleSignal(SIGSEGV);
                    running = false;
                    break;
                }
            }
            
            if (midlet.debug) {
                midlet.print("=== ELF END DEBUG ===\nInstructions executed: " + instructionCount, stdout, id, scope);
            }
        } 
        catch (Throwable e) { 
            if (midlet.debug) midlet.print("=== ELF CRASH DEBUG ===\nCRASH: " + e.getClass().getName() + ": " + e.getMessage(), stdout, id, scope);
            e.printStackTrace();
            running = false; 
        } 
        finally { 
            if (midlet.debug) midlet.print("=== ELF FINALLY DEBUG ===", stdout, id, scope);
            executeFiniFunctions();
            if (midlet.sys.containsKey(pid)) { midlet.sys.remove(pid); } 
        }

        ITEM.put("status", new Double(0));
        return ITEM;
    }

    private int fetchInstruction(int addr) { return readIntLE(memory, addr); }
    // ===== Núcleo RV32IM =====
    private int getReg(int r) { return (r == 0) ? 0 : registers[r]; }
    private void setReg(int r, int v) { if (r != 0) { registers[r] = v; } }

    private int signExtend(int v, int bits) {
        int m = 1 << (bits - 1);
        if ((v & m) != 0) { v |= ~((1 << bits) - 1); }
        return v;
    }
    private int iImm(int instruction) { return signExtend(instruction >> 20, 12); }
    private int sImm(int instruction) { return signExtend(((instruction >> 25) << 5) | ((instruction >> 7) & 0x1F), 12); }
    private int bImm(int instruction) { return signExtend((((instruction >> 31) & 1) << 12) | (((instruction >> 7) & 1) << 11) | (((instruction >> 25) & 0x3F) << 5) | (((instruction >> 8) & 0xF) << 1), 13); }
    private int jImm(int instruction) { return signExtend((((instruction >> 31) & 1) << 20) | (((instruction >> 12) & 0xFF) << 12) | (((instruction >> 20) & 1) << 11) | (((instruction >> 21) & 0x3FF) << 1), 21); }

    private void executeInstruction(int instruction) {
        int opcode = instruction & 0x7F;
        int rd = (instruction >> 7) & 0x1F;
        int funct3 = (instruction >> 12) & 0x7;
        int rs1 = (instruction >> 15) & 0x1F;
        int rs2 = (instruction >> 20) & 0x1F;
        int funct7 = (instruction >> 25) & 0x7F;

        switch (opcode) {
            case RV_OP_LUI: setReg(rd, instruction & 0xFFFFF000); break;
            case RV_OP_AUIPC: setReg(rd, pc - 4 + (instruction & 0xFFFFF000)); break;
            case RV_OP_JAL:
                setReg(rd, pc);
                pc = pc - 4 + jImm(instruction);
                break;
            case RV_OP_JALR: {
                int ret = pc;
                pc = (getReg(rs1) + iImm(instruction)) & ~1;
                setReg(rd, ret);
                break;
            }
            case RV_OP_BRANCH:
                if (branchCond(funct3, getReg(rs1), getReg(rs2))) { pc = pc - 4 + bImm(instruction); }
                break;
            case RV_OP_LOAD: rvLoad(funct3, rd, getReg(rs1) + iImm(instruction)); break;
            case RV_OP_STORE: rvStore(funct3, getReg(rs1) + sImm(instruction), getReg(rs2)); break;
            case RV_OP_OPIMM: rvOpImm(rd, funct3, rs1, iImm(instruction), instruction); break;
            case RV_OP_OP: rvOp(rd, funct3, funct7, rs1, rs2); break;
            case RV_OP_MISCMEM: break; // FENCE
            case RV_OP_SYSTEM:
                if (instruction == 0x00000073) { handleSyscall(registers[REG_A7]); } // ecall
                else if (instruction == 0x00100073) { running = false; } // ebreak
                else if (rd != 0) { registers[rd] = 0; } // CSR (leitura basica = 0)
                break;
            default:
                if (midlet.debug) { midlet.print("[WARN] Unrecognized RISC-V instruction: " + toHex(instruction) + " at PC: " + toHex(pc - 4), stdout, id, scope); }
                break;
        }
    }

    private boolean branchCond(int funct3, int a, int b) {
        long ua = a & 0xFFFFFFFFL, ub = b & 0xFFFFFFFFL;
        switch (funct3) {
            case 0: return a == b;   // BEQ
            case 1: return a != b;   // BNE
            case 4: return a < b;    // BLT
            case 5: return a >= b;   // BGE
            case 6: return ua < ub;  // BLTU
            case 7: return ua >= ub; // BGEU
        }
        return false;
    }
    private void rvLoad(int funct3, int rd, int addr) {
        int v = 0;
        switch (funct3) {
            case 0: v = (addr >= 0 && addr < memory.length) ? (byte) memory[addr] : 0; break; // LB
            case 1: v = (addr >= 0 && addr + 1 < memory.length) ? readShortLE(memory, addr) : 0; break; // LH
            case 2: v = (addr >= 0 && addr + 3 < memory.length) ? readIntLE(memory, addr) : 0; break; // LW
            case 4: v = (addr >= 0 && addr < memory.length) ? (memory[addr] & 0xFF) : 0; break; // LBU
            case 5: v = (addr >= 0 && addr + 1 < memory.length) ? (readShortLE(memory, addr) & 0xFFFF) : 0; break; // LHU
            default: return;
        }
        setReg(rd, v);
    }
    private void rvStore(int funct3, int addr, int v) {
        switch (funct3) {
            case 0: if (addr >= 0 && addr < memory.length) memory[addr] = (byte) v; break; // SB
            case 1: if (addr >= 0 && addr + 1 < memory.length) writeShortLE(memory, addr, (short) v); break; // SH
            case 2: if (addr >= 0 && addr + 3 < memory.length) writeIntLE(memory, addr, v); break; // SW
        }
    }
    private void rvOpImm(int rd, int funct3, int rs1, int imm, int instruction) {
        int a = getReg(rs1), v;
        switch (funct3) {
            case 0: v = a + imm; break; // ADDI
            case 2: v = (a < imm) ? 1 : 0; break; // SLTI
            case 3: v = (((a & 0xFFFFFFFFL) < (imm & 0xFFFFFFFFL)) ? 1 : 0); break; // SLTIU
            case 4: v = a ^ imm; break; // XORI
            case 6: v = a | imm; break; // ORI
            case 7: v = a & imm; break; // ANDI
            case 1: v = a << (imm & 0x1F); break; // SLLI
            case 5: v = ((imm & 0x400) != 0) ? (a >> (imm & 0x1F)) : (a >>> (imm & 0x1F)); break; // SRAI/SRLI
            default: return;
        }
        setReg(rd, v);
    }
    private void rvOp(int rd, int funct3, int funct7, int rs1, int rs2) {
        int a = getReg(rs1), b = getReg(rs2), v;
        if (funct7 == 0x01) { rvMulDiv(rd, funct3, a, b); return; } // Extensao M
        switch (funct3) {
            case 0: v = (funct7 == 0x20) ? a - b : a + b; break; // SUB/ADD
            case 1: v = a << (b & 0x1F); break; // SLL
            case 2: v = (a < b) ? 1 : 0; break; // SLT
            case 3: v = ((a & 0xFFFFFFFFL) < (b & 0xFFFFFFFFL)) ? 1 : 0; break; // SLTU
            case 4: v = a ^ b; break; // XOR
            case 5: v = (funct7 == 0x20) ? (a >> (b & 0x1F)) : (a >>> (b & 0x1F)); break; // SRA/SRL
            case 6: v = a | b; break; // OR
            case 7: v = a & b; break; // AND
            default: return;
        }
        setReg(rd, v);
    }
    private void rvMulDiv(int rd, int funct3, int a, int b) {
        long la = a & 0xFFFFFFFFL, lb = b & 0xFFFFFFFFL;
        int v = 0;
        switch (funct3) {
            case 0: v = (int) ((long) a * (long) b); break; // MUL
            case 1: v = (int) (((long) a * (long) b) >> 32); break; // MULH
            case 2: v = (int) (((long) a * lb) >> 32); break; // MULHSU
            case 3: v = (int) ((la * lb) >>> 32); break; // MULHU
            case 4: v = (b == 0) ? -1 : (a == Integer.MIN_VALUE && b == -1 ? Integer.MIN_VALUE : a / b); break; // DIV
            case 5: v = (b == 0) ? -1 : (int) (la / lb); break; // DIVU
            case 6: v = (b == 0) ? a : (a == Integer.MIN_VALUE && b == -1 ? 0 : a % b); break; // REM
            case 7: v = (b == 0) ? a : (int) (la % lb); break; // REMU
            default: return;
        }
        setReg(rd, v);
    }

    private void executeInitFunctions() {
        if (elfInfo.containsKey("init")) {
            int initAddr = ((Integer)elfInfo.get("init")).intValue();
            callInitFunction(initAddr, ".init");
        }
        if (elfInfo.containsKey("init_array") && elfInfo.containsKey("init_arraysz")) {
            int array = ((Integer)elfInfo.get("init_array")).intValue(), size = ((Integer)elfInfo.get("init_arraysz")).intValue();
            for (int offset = 0; offset + 3 < size; offset += 4) {
                int initAddr = readIntLE(memory, array + offset);
                if (initAddr != 0 && initAddr != -1) { callInitFunction(initAddr, ".init_array"); }
            }
        }
    }
    private void executeLibraryInitFunctions() {
        for (int i = sharedObjectMappings.size() - 1; i >= 0; i--) {
            Hashtable mapping = (Hashtable) sharedObjectMappings.elementAt(i);
            if (mapping.containsKey("init")) { callInitFunction(((Integer) mapping.get("init")).intValue(), "shared .init"); }
            if (mapping.containsKey("init_array") && mapping.containsKey("init_arraysz")) {
                int array = ((Integer) mapping.get("init_array")).intValue(), size = ((Integer) mapping.get("init_arraysz")).intValue();
                for (int offset = 0; offset + 3 < size; offset += 4) {
                    int initAddr = readIntLE(memory, array + offset);
                    if (initAddr != 0 && initAddr != -1) { callInitFunction(initAddr, "shared .init_array"); }
                }
            }
        }
    }
    private void executeFiniFunctions() {
        executeFiniArray(elfInfo, "fini_array");
        if (elfInfo.containsKey("fini")) { callInitFunction(((Integer) elfInfo.get("fini")).intValue(), ".fini"); }
        for (int i = 0; i < sharedObjectMappings.size(); i++) {
            Hashtable mapping = (Hashtable) sharedObjectMappings.elementAt(i);
            executeFiniArray(mapping, "fini_array");
            if (mapping.containsKey("fini")) { callInitFunction(((Integer) mapping.get("fini")).intValue(), "shared .fini"); }
        }
    }
    private void executeFiniArray(Hashtable values, String key) {
        String sizeKey = key + "sz";
        if (!values.containsKey(key) || !values.containsKey(sizeKey)) { return; }
        int array = ((Integer) values.get(key)).intValue(), size = ((Integer) values.get(sizeKey)).intValue();
        for (int offset = size - 4; offset >= 0; offset -= 4) {
            int finiAddr = readIntLE(memory, array + offset);
            if (finiAddr != 0 && finiAddr != -1) { callInitFunction(finiAddr, ".fini_array"); }
        }
    }
    private void callInitFunction(int initAddr, String source) {
        if (initAddr < 0 || initAddr >= memory.length) { return; }
        int savedPC = pc, savedSP = registers[REG_SP], savedLR = registers[REG_LR];
        registers[REG_LR] = savedPC;
        pc = initAddr;
        if (midlet.debug) { midlet.print("Calling " + source + " at " + toHex(initAddr), stdout, id, scope); }
        for (int i = 0; i < 1024 && pc != savedPC && pc >= 0 && pc + 3 < memory.length; i++) {
            int instruction = fetchInstruction(pc);
            pc += 4;
            executeInstruction(instruction);
        }
        pc = savedPC;
        registers[REG_SP] = savedSP;
        registers[REG_LR] = savedLR;
    }
    private Hashtable loadSections(byte[] elfData, int shoff, int shnum, int shentsize) {
        Hashtable sections = new Hashtable();
        
        for (int i = 0; i < shnum; i++) {
            int shdrOffset = shoff + i * shentsize, sh_name = readIntLE(elfData, shdrOffset), sh_type = readIntLE(elfData, shdrOffset + 4), sh_flags = readIntLE(elfData, shdrOffset + 8), sh_addr = readIntLE(elfData, shdrOffset + 12), sh_offset = readIntLE(elfData, shdrOffset + 16), sh_size = readIntLE(elfData, shdrOffset + 20), sh_link = readIntLE(elfData, shdrOffset + 24), sh_info = readIntLE(elfData, shdrOffset + 28), sh_addralign = readIntLE(elfData, shdrOffset + 32), sh_entsize = readIntLE(elfData, shdrOffset + 36);

            Hashtable section = new Hashtable();
            section.put("type", new Integer(sh_type)); section.put("flags", new Integer(sh_flags)); section.put("addr", new Integer(sh_addr));
            section.put("offset", new Integer(sh_offset)); section.put("size", new Integer(sh_size)); section.put("link", new Integer(sh_link));
            section.put("info", new Integer(sh_info)); section.put("addralign", new Integer(sh_addralign)); section.put("entsize", new Integer(sh_entsize));

            if (sh_name != 0 && elfInfo.containsKey(".shstrtab")) { int strtabOffset = ((Integer)elfInfo.get(".shstrtab")).intValue(); String name = readString(elfData, strtabOffset + sh_name, 64); sections.put(name, section); if (midlet.debug) { midlet.print("Section: " + name + " at " + toHex(sh_addr), stdout, id, scope); } }
            if (sh_type == 3) { elfInfo.put(".shstrtab", new Integer(sh_offset)); }
        }
        
        return sections;
    }
    
    private void initializeBSS(Hashtable sections) {
        Enumeration keys = sections.keys();
        while (keys.hasMoreElements()) {
            String name = (String) keys.nextElement();
            if (name.equals(".bss") || name.equals(".sbss")) {
                Hashtable section = (Hashtable) sections.get(name);
                int addr = ((Integer)section.get("addr")).intValue(), size = ((Integer)section.get("size")).intValue();

                for (int i = 0; i < size && addr + i < memory.length; i++) { memory[addr + i] = 0; }
                if (midlet.debug) { midlet.print("Zeroed " + name + " at " + toHex(addr) + " size " + size, stdout, id, scope); }
            }
        }
    }
    
    private void processDynamicSegment(byte[] elfData, int phdrOffset) {
        int p_offset = readIntLE(elfData, phdrOffset + 4), p_vaddr = readIntLE(elfData, phdrOffset + 8), p_filesz = readIntLE(elfData, phdrOffset + 16);
        if (midlet.debug) { midlet.print("Dynamic segment at " + toHex(p_vaddr), stdout, id, scope); } 

        for (int offset = 0; offset < p_filesz; offset += 8) {
            int tag = readIntLE(elfData, p_offset + offset), val = readIntLE(elfData, p_offset + offset + 4);
            if (tag == 0) { break; }
            
            switch (tag) {
                case 1:
                    if (midlet.debug) { midlet.print("Needs library (strtab offset " + val + ")", stdout, id, scope); }
                    break;
                case 5:
                    elfInfo.put("dynstr", new Integer(val));
                    break;
                case 6:
                    elfInfo.put("dynsym", new Integer(val));
                    break;
            }
        }
    }
    private void processSymbolsAndRelocations(byte[] elfData, Hashtable sections) {
        processSymbols(elfData);
        processRelocations(elfData);
    }
    private void processSymbols(byte[] elfData) {
        if (!elfInfo.containsKey("dynsym") || !elfInfo.containsKey("dynstr")) { return; }
        
        int dynsymAddr = ((Integer)elfInfo.get("dynsym")).intValue(), dynstrAddr = ((Integer)elfInfo.get("dynstr")).intValue(), symentSize = elfInfo.containsKey("syment") ? ((Integer) elfInfo.get("syment")).intValue() : 16;
        
        dynSymNames.removeAllElements();
        // Processar símbolos (a entrada 0 do dynsym é o symbolo nulo; o numero
        // de entradas vem do DT_HASH quando presente — nchain).
        int symOffset = dynsymAddr;
        int maxSym = elfInfo.containsKey("nchain") ? ((Integer) elfInfo.get("nchain")).intValue() : 4096;
        if (maxSym <= 0 || maxSym > 4096) { maxSym = 4096; }
        for (int dynSymCount = 0; dynSymCount < maxSym; dynSymCount++) {
            int st_name = readIntLE(memory, symOffset), st_value = readIntLE(memory, symOffset + 4), st_size = readIntLE(memory, symOffset + 8), st_info = memory[symOffset + 12] & 0xFF;
            if (dynSymCount > 0 && st_name == 0 && st_value == 0 && st_size == 0 && st_info == 0) { break; }
            
            String symName = (st_name == 0) ? "" : readString(memory, dynstrAddr + st_name, 256);
            if (symName == null) { break; }
            
            dynSymNames.addElement(symName);
            Hashtable symInfo = new Hashtable();
            symInfo.put("value", new Integer(st_value)); symInfo.put("size", new Integer(st_size)); symInfo.put("info", new Integer(st_info));
            symInfo.put("binding", new Integer((st_info >> 4) & 0xF)); symInfo.put("type", new Integer(st_info & 0xF));
            
            dynamicSymbols.put(symName, symInfo);
            
            symOffset += symentSize;
        }
    }
    private void processRelocations(byte[] elfData) {
        if (elfInfo.containsKey("rel") && elfInfo.containsKey("relsz")) {
            int relAddr = ((Integer) elfInfo.get("rel")).intValue(), relsz = ((Integer) elfInfo.get("relsz")).intValue();
            int relent = elfInfo.containsKey("relent") ? ((Integer) elfInfo.get("relent")).intValue() : 8;
            for (int i = 0; i < relsz; i += relent) {
                int offset = relAddr + i, r_offset = readIntLE(memory, offset), r_info = readIntLE(memory, offset + 4), symIndex = r_info >> 8, type = r_info & 0xFF, addend = (relent == 12) ? readIntLE(memory, offset + 8) : 0;
                if (type == R_RISCV_JUMP_SLOT) {
                    String platoonName = getSymbolNameByIndex(symIndex);
                    Integer readyAddr = resolveSymbol(platoonName);
                    if (readyAddr != null) {
                        writeIntLE(memory, r_offset, readyAddr.intValue() + addend);
                        if (midlet.debug) { midlet.print("PLT eager: " + platoonName + " -> " + toHex(readyAddr.intValue()) + " at GOT " + toHex(r_offset), stdout, id, scope); }
                    } else { setupLazyBinding(r_offset, symIndex, i); }
                } else { applyRelocation(r_offset, type, symIndex, addend); }
            }
        }
        if (elfInfo.containsKey("jmprel") && elfInfo.containsKey("pltrelsz")) {
            int jmprelAddr = ((Integer)elfInfo.get("jmprel")).intValue(), pltrelsz = ((Integer)elfInfo.get("pltrelsz")).intValue(), pltrel = elfInfo.containsKey("pltrel") ? ((Integer) elfInfo.get("pltrel")).intValue() : DT_REL, relent = (pltrel == DT_RELA) ? 12 : 8, numEntries = pltrelsz / relent;
            if (gotBase == 0 && pltGotAddr != 0) { gotBase = pltGotAddr + 8; }
            for (int i = 0; i < numEntries; i++) {
                int offset = jmprelAddr + i * relent, r_offset = readIntLE(memory, offset), r_info = readIntLE(memory, offset + 4), symIndex = r_info >> 8, type = r_info & 0xFF, addend = (relent == 12) ? readIntLE(memory, offset + 8) : 0;
                if (type == R_RISCV_JUMP_SLOT) {
                    String platoonName = getSymbolNameByIndex(symIndex);
                    Integer readyAddr = resolveSymbol(platoonName);
                    if (readyAddr != null) {
                        writeIntLE(memory, r_offset, readyAddr.intValue() + addend);
                        if (midlet.debug) { midlet.print("PLT eager: " + platoonName + " -> " + toHex(readyAddr.intValue()) + " at GOT " + toHex(r_offset), stdout, id, scope); }
                    } else { setupLazyBinding(r_offset, symIndex, i); }
                } else { applyRelocation(r_offset, type, symIndex, addend); }
            }
        }
    }
    private void applyRelocation(int r_offset, int type, int symIndex, int addend) { applyRelocation(r_offset, type, symIndex, addend, null); }
    private void applyRelocation(int r_offset, int type, int symIndex, int addend, Vector names) {
        String symName = null;
        if (names != null) { if (symIndex >= 0 && symIndex < names.size()) { symName = (String) names.elementAt(symIndex); } }
        else { symName = getSymbolNameByIndex(symIndex); }
        switch (type) {
            case R_RISCV_32:
            case R_RISCV_GLOB_DAT:
                Integer symAddr = resolveSymbol(symName);
                
                if (symAddr != null) {
                    writeIntLE(memory, r_offset, symAddr.intValue() + addend);
                    if (midlet.debug) { midlet.print("Reloc: " + symName + " -> " + toHex(symAddr.intValue()) + " at " + toHex(r_offset), stdout, id, scope); }
                }
                break;
            case R_RISCV_RELATIVE:
                int current = readIntLE(memory, r_offset);
                writeIntLE(memory, r_offset, current + addend);
                break;
            case R_RISCV_COPY: {
                String copyName = symName;
                Hashtable libTab = (copyName != null) ? resolveLibrarySymbolTable(copyName) : null;
                if (libTab != null && copyName != null) {
                    Object symValue = libTab.get(copyName);
                    Integer copySrc = (symValue instanceof Integer) ? (Integer) symValue : null;
                    if (copySrc != null) {
                        int copySize = librarySymbolSize(copyName), src = copySrc.intValue();
                        for (int k = 0; k < copySize && r_offset + k < memory.length && src + k < memory.length; k++) { memory[r_offset + k] = memory[src + k]; }
                        copyRelocs.put(copyName, new Integer(r_offset));
                        if (dynamicSymbols.containsKey(copyName)) {
                            Hashtable dyn = (Hashtable) dynamicSymbols.get(copyName);
                            dyn.put("value", new Integer(r_offset));
                            dyn.put("size", new Integer(copySize));
                        }
                        if (midlet.debug) { midlet.print("Reloc COPY: " + copyName + " (" + copySize + " bytes) -> " + toHex(r_offset) + " from " + toHex(src), stdout, id, scope); }
                    }
                }
                break;
            }
        }
    }

    // Binding PLT sem lazy resolver: simbolos nao resolvidos deixam o slot GOT
    // zerado; toda resolucao suportada e feita eager em processRelocations.
    private void setupLazyBinding(int gotOffset, int symIndex, int slotIndex) {
        writeIntLE(memory, gotOffset, 0);
        if (midlet.debug) { midlet.print("PLT unresolved slot " + slotIndex + " at GOT " + toHex(gotOffset), stdout, id, scope); }
    }

    private String getSymbolNameByIndex(int index) { if (index >= 0 && index < dynSymNames.size()) { return (String) dynSymNames.elementAt(index); } return null; }

    private Integer resolveSymbol(String name) {
        if (dynamicSymbols.containsKey(name)) {
            Hashtable symInfo = (Hashtable) dynamicSymbols.get(name);
            int value = ((Integer)symInfo.get("value")).intValue();
            if (value != 0) { return new Integer(value); }
        }

        // R_RISCV_COPY: definicoes do executavel (simbolos copiados) preemptam as libs
        if (copyRelocs.containsKey(name)) { return new Integer(((Integer) copyRelocs.get(name)).intValue()); }

        for (int i = 0; i < loadedLibraries.size(); i++) {
            String libName = (String) loadedLibraries.elementAt(i);
            Hashtable lib = (Hashtable) globalSymbols.get(libName);
            
            if (lib != null && lib.containsKey(name)) {
                Object symValue = lib.get(name);
                if (symValue instanceof Hashtable) { return new Integer(((Integer) ((Hashtable) symValue).get("value")).intValue()); }
                else if (symValue instanceof Integer) { return (Integer) symValue; }
            }
        }
        
        // Criar stub se for syscall
        if (name.startsWith("sys_")) { return new Integer(createSyscallStub(name)); }
        
        return null;
    }
    private Hashtable resolveLibrarySymbolTable(String name) {
        for (int i = 0; i < loadedLibraries.size(); i++) {
            Hashtable lib = (Hashtable) globalSymbols.get((String) loadedLibraries.elementAt(i));
            if (lib != null && lib.containsKey(name)) { return lib; }
        }
        return null;
    }
    private int librarySymbolSize(String name) {
        for (Enumeration e = libSymSizes.keys(); e.hasMoreElements();) {
            Hashtable m = (Hashtable) libSymSizes.get(e.nextElement());
            if (m != null && m.containsKey(name)) { return ((Integer) m.get(name)).intValue(); }
        }
        return 4;
    }

    private void setupPLTGOT() { if (pltGotAddr == 0) { return; } writeIntLE(memory, pltGotAddr, dynamicSectionAddr); }
    private void setupCRTStack() {
        int sp = registers[REG_SP];

        writeIntLE(memory, sp - 4, 0);
        writeIntLE(memory, sp - 8, 0);
        sp -= 8;

        Vector envVars = new Vector();
        envVars.addElement("PATH=/bin"); envVars.addElement("USER=" + midlet.getUser(id)); envVars.addElement("HOME=/home"); envVars.addElement("TERM=vt100");
        for (Enumeration e = midlet.attributes.keys(); e.hasMoreElements();) { envVars.addElement(midlet.attributes.get(e.nextElement())); }

        Vector argsVec = new Vector();
        for (int i = 0; i < args.size(); i++) { argsVec.addElement(args.get(new Double(i))); }
        if (argsVec.size() == 0) { argsVec.addElement("program"); }

        /* String data must live above argv/envp. The CRT expects both pointer
         * tables immediately after argc, so never write strings into them. */
        Vector envAddrs = new Vector(), argAddrs = new Vector();
        for (int i = 0; i < envVars.size(); i++) {
            String env = (String) envVars.elementAt(i);
            byte[] envBytes = env.getBytes();
            sp -= envBytes.length + 1;
            for (int j = 0; j < envBytes.length; j++) { memory[sp + j] = envBytes[j]; }
            memory[sp + envBytes.length] = 0;
            envAddrs.addElement(new Integer(sp));
        }
        for (int i = 0; i < argsVec.size(); i++) {
            String arg = (String) argsVec.elementAt(i);
            byte[] argBytes = arg.getBytes();
            sp -= argBytes.length + 1;
            for (int j = 0; j < argBytes.length; j++) { memory[sp + j] = argBytes[j]; }
            memory[sp + argBytes.length] = 0;
            argAddrs.addElement(new Integer(sp));
        }

        /* Keep the initial ABI stack 16-byte aligned (RISC-V calling convention). */
        int tableSize = (envVars.size() + argsVec.size() + 3) * 4;
        sp = (sp - tableSize) & ~15;
        int argvStart = sp + 4;
        int envpStart = argvStart + (argsVec.size() + 1) * 4;
        for (int i = 0; i < envVars.size(); i++) { writeIntLE(memory, envpStart + i * 4, ((Integer) envAddrs.elementAt(i)).intValue()); }
        writeIntLE(memory, envpStart + envVars.size() * 4, 0); // NULL terminator
        for (int i = 0; i < argsVec.size(); i++) { writeIntLE(memory, argvStart + i * 4, ((Integer) argAddrs.elementAt(i)).intValue()); }
        writeIntLE(memory, argvStart + argsVec.size() * 4, 0); // NULL terminator
        writeIntLE(memory, sp, argsVec.size());

        elfInfo.put("argc", new Integer(argsVec.size()));
        if (argsVec.size() > 0) { elfInfo.put("argv0", argsVec.elementAt(0)); }
        
        // Configurar stack pointer
        registers[REG_SP] = sp;
        
        if (midlet.debug) {
            midlet.print("Stack setup: SP=" + toHex(registers[REG_SP]), stdout, id, scope);
            midlet.print("argc=" + argsVec.size(), stdout, id, scope);
            for (int i = 0; i < argsVec.size(); i++) { midlet.print("argv[" + i + "]=" + argsVec.elementAt(i), stdout, id, scope); }
        }
    }

    public void dumpDynamicInfo(Object stdout) {
        midlet.print("=== Dynamic Linking Info ===", stdout, id, scope);
        midlet.print("PLT/GOT: " + toHex(pltGotAddr), stdout, id, scope);
        midlet.print("GOT Base: " + toHex(gotBase), stdout, id, scope);
        
        midlet.print("\nLoaded Libraries (" + loadedLibraries.size() + "):", stdout, id, scope);
        for (int i = 0; i < loadedLibraries.size(); i++) {
            midlet.print("  " + loadedLibraries.elementAt(i), stdout, id, scope);
        }
        
        midlet.print("\nDynamic Symbols (" + dynamicSymbols.size() + "):", stdout, id, scope);
        Enumeration keys = dynamicSymbols.keys();
        int count = 0;
        while (keys.hasMoreElements() && count < 20) {
            String name = (String) keys.nextElement();
            Hashtable sym = (Hashtable) dynamicSymbols.get(name);
            int value = ((Integer)sym.get("value")).intValue();
            midlet.print("  " + name + " -> " + toHex(value), stdout, id, scope);
            count++;
        }
    }
    // Stubs
    private int createSimpleStub(int size) { int stubAddr = findFreeMemoryRegion(size); if (stubAddr == 0) { return 0; } writeIntLE(memory, stubAddr, 0x00000513); writeIntLE(memory, stubAddr + 4, 0x00000073); writeIntLE(memory, stubAddr + 8, 0x00008067); return stubAddr; }
    private int createSyscallStub(String name) { int stubAddr = findFreeMemoryRegion(32); if (stubAddr == 0) { return 0; } int syscallNum = mapSyscallName(name); writeIntLE(memory, stubAddr, ((syscallNum & 0xFFF) << 20) | (17 << 7) | 0x13); writeIntLE(memory, stubAddr + 4, 0x00000073); writeIntLE(memory, stubAddr + 8, 0x00008067); return stubAddr; }

    private int mapSyscallName(String name) { if (name.equals("exit") || name.indexOf("exit") != -1) { return SYS_EXIT; } if (name.equals("write") || name.indexOf("write") != -1) {  return SYS_WRITE; } if (name.equals("read") || name.indexOf("read") != -1) { return SYS_READ; } if (name.equals("open") || name.indexOf("open") != -1) { return SYS_OPEN; } if (name.equals("close") || name.indexOf("close") != -1) { return SYS_CLOSE; } if (name.equals("brk") || name.indexOf("brk") != -1) { return SYS_BRK; } if (name.equals("fork") || name.indexOf("fork") != -1) { return SYS_FORK; } if (name.equals("execve") || name.indexOf("exec") != -1) { return SYS_EXECVE; } return 0; }
    
    // Syscalls Handler
    // |
    private void handleSyscall(int number) {
        if (midlet.debug && number != SYS_GETTIMEOFDAY && number != SYS_GETPID) { midlet.print("Syscall " + number + " (a7=" + registers[REG_A7] + ")", stdout, id, scope); }
        if (number >= LIB_BASE) { handleLibraryCall(number - LIB_BASE); return; }
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
                int syscallNum = registers[REG_A0];
                registers[REG_A7] = syscallNum;
                registers[REG_A0] = registers[REG_A1];
                registers[REG_A1] = registers[REG_A2];
                registers[REG_A2] = registers[REG_A3];
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
                registers[REG_A0] = -38; //handlePipe();
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
                registers[REG_A0] = -38; // ENOSYS - Syscall não implementada
                if (midlet.debug) { midlet.print("Unimplemented syscall: " + number, stdout, id, scope); }
                break;
        }
    }
    // |
    // | Kernel
    // | (Process)
    private void handleFork() { registers[REG_A0] = -1; }
    private void handleExecve() {
        int pathAddr = registers[REG_A0], argvAddr = registers[REG_A1], envpAddr = registers[REG_A2];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_A0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();

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
        
        StringBuffer argsStr = new StringBuffer();
        for (i = 1; i < argsVec.size(); i++) { if (i > 1) argsStr.append(" "); argsStr.append((String) argsVec.elementAt(i)); }
        try {
            InputStream is = midlet.getInputStream(path, scope);
            if (is == null) { registers[REG_A0] = -2; return; }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            
            while ((length = is.read(buffer)) != -1) { baos.write(buffer, 0, length); }
            
            byte[] data = baos.toByteArray(); baos.close();

            Hashtable arg = new Hashtable();

            if (midlet.isPureText(data)) {
                String code = new String(data, "UTF-8");
                Process process = new Process(midlet, ("lua " + path).trim(), midlet.joinpath(path, scope), midlet.getUser(id), id, midlet.genpid(), stdout, scope);
                process.lua.run(path, code, arg);
                registers[REG_A0] = 0;
            }
            else {
                InputStream elfStream = new ByteArrayInputStream(data);
                Process process = new Process(midlet, "elf", midlet.joinpath(path, scope), midlet.getUser(id), id, midlet.genpid(), stdout, arg, scope);
                
                if (process.elf.load(elfStream)) { process.elf.run(); registers[REG_A0] = 0; } else { registers[REG_A0] = -8; }
            }
        } catch (Exception e) { registers[REG_A0] = -1; }
    }
    private void handleGetpriority() { int which = registers[REG_A0], who = registers[REG_A1]; if (who == 0) { registers[REG_A0] = proc.priority; } else { registers[REG_A0] = -22; } }
    private void handleSetpriority() { int which = registers[REG_A0], who = registers[REG_A1], prio = registers[REG_A2]; if (who == 0) { proc.priority = prio; registers[REG_A0] = 0; } else { registers[REG_A0] = -22; } }
    // |
    private void handleSignal() { int signum = registers[REG_A0], handler = registers[REG_A1]; if (signum <= 0 || signum >= NSIG) { registers[REG_A0] = SIG_ERR; return; } int oldHandler = signalHandlers[signum]; signalHandlers[signum] = handler; registers[REG_A0] = oldHandler; }
    private void handleSignal(int sig) {
        if (sig <= 0 || sig >= NSIG) return;
        int handler = signalHandlers[sig];
        
        if (handler == SIG_DFL) {
            switch (sig) {
                case SIGSEGV:
                    running = false;
                    if (midlet.debug) { midlet.print("Segmentation fault", stdout, id, scope); }
                    break;
                case SIGINT:
                    running = false;
                    break;
            }
        }
        else if (handler == SIG_IGN) { return; }
        else if (handler != 0) { pushSignalFrame(sig); pc = handler; }
    }
    private void handleSigaction() {
        int signum = registers[REG_A0], actPtr = registers[REG_A1], oldactPtr = registers[REG_A2];
        if (signum <= 0 || signum >= NSIG) { registers[REG_A0] = -22; return; }

        if (oldactPtr != 0 && oldactPtr + 12 <= memory.length) { writeIntLE(memory, oldactPtr, signalHandlers[signum]); writeIntLE(memory, oldactPtr + 4, 0); writeIntLE(memory, oldactPtr + 8, 0); }
        if (actPtr != 0 && actPtr + 4 <= memory.length) { int newHandler = readIntLE(memory, actPtr); signalHandlers[signum] = newHandler; }
        
        registers[REG_A0] = 0;
    }
    private void handleKill() {
        int pid = registers[REG_A0], sig = registers[REG_A1];
        String targetPid = String.valueOf(pid);
        
        if (!midlet.sys.containsKey(targetPid)) { registers[REG_A0] = -3; return; }
        if (this.id != 0 && !targetPid.equals(this.pid)) { registers[REG_A0] = -1; return; }
        
        Object procObj = midlet.sys.get(targetPid);

        if (sig == SIGKILL || sig == SIGTERM) {
            if (procObj instanceof Hashtable) {
                Hashtable proc = (Hashtable) procObj;
                if (proc.containsKey("elf")) {
                    ELF elf = (ELF) proc.get("elf");
                    elf.kill();
                }
            }
            midlet.sys.remove(targetPid);
            registers[REG_A0] = 0;
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
            registers[REG_A0] = 0;
            return;
        }
        
        if (sig == 0) { registers[REG_A0] = 0; }
        else { registers[REG_A0] = -22; }
    }
    // |
    private void handleExit() { int status = registers[REG_A0]; running = false; cleanup(); }
    private void cleanup() {
        Enumeration keys = fileDescriptors.keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof Integer) {
                Integer fd = (Integer) key;
                if (fd.intValue() >= 3) {
                    Object stream = fileDescriptors.get(fd);
                    try {
                        if (stream instanceof ByteArrayOutputStream) {
                            ByteArrayOutputStream baos = (ByteArrayOutputStream) stream;
                            String pathKey = fd + ":path";
                            if (fileDescriptors.containsKey(pathKey)) {
                                String path = (String) fileDescriptors.get(pathKey);
                                midlet.write(path, baos.toByteArray(), id, scope);
                            }
                        } else if (stream instanceof InputStream) { ((InputStream) stream).close(); }
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
            if (socketInfo.containsKey("datagram")) { try { ((DatagramConnection) socketInfo.get("datagram")).close(); } catch (Exception e) { } }
        }

        fileDescriptors.clear(); socketDescriptors.clear(); allocatedBlocks.clear(); jmpBufs.clear();
        memoryMappings.removeAllElements();
    }
    // |
    private void handleGetpid() { try { int pidValue = Integer.parseInt(this.pid); registers[REG_A0] = pidValue; } catch (NumberFormatException e) { registers[REG_A0] = 1; } }
    private void handleGetppid() { registers[REG_A0] = 1; }
    private void handleGetuid() { registers[REG_A0] = id; }
    private void handleGettid() { registers[REG_A0] = id; }
    // | (Users)

    // | (Memory)
    private void handleMmap() {
        int addr = registers[REG_A0], length = registers[REG_A1], prot = registers[REG_A2], flags = registers[REG_A3], fd = getSyscallParam(4), offset = getSyscallParam(5);
        
        if (midlet.debug) { midlet.print("mmap: addr=" + toHex(addr) + " length=" + length + " prot=" + prot + " flags=" + toHex(flags) + " fd=" + fd + " offset=" + offset, stdout, id, scope); }
        if (length <= 0) { registers[REG_A0] = -22; return; }

        length = (length + 4095) & ~4095;

        if (addr == 0) { addr = findFreeMemoryRegion(length); if (addr == 0) { registers[REG_A0] = -12; return; }
        }
        if (!isMemoryRegionFree(addr, length)) { registers[REG_A0] = -12; return; }
        
        Hashtable mapping = new Hashtable();
        mapping.put("addr", new Integer(addr)); mapping.put("length", new Integer(length)); mapping.put("prot", new Integer(prot));
        mapping.put("flags", new Integer(flags)); mapping.put("fd", new Integer(fd)); mapping.put("offset", new Integer(offset));

        memoryMappings.addElement(mapping);
        
        if ((flags & MAP_ANONYMOUS) != 0) { for (int i = 0; i < length && addr + i < memory.length; i++) { memory[addr + i] = 0; } }
        
        registers[REG_A0] = addr;
    }
    private void handleMunmap() {
        int addr = registers[REG_A0], length = registers[REG_A1];

        for (int i = 0; i < memoryMappings.size(); i++) {
            Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
            int maddr = ((Integer) mapping.get("addr")).intValue(), mlen = ((Integer) mapping.get("length")).intValue();
            if (addr >= maddr && addr < maddr + mlen) { for (int j = 0; j < mlen && maddr + j < memory.length; j++) { memory[maddr + j] = 0; } memoryMappings.removeElementAt(i); registers[REG_A0] = 0; return; }
        }

        registers[REG_A0] = -22;
    }
    
    private void handleMprotect() {
        int addr = registers[REG_A0], len = registers[REG_A1], prot = registers[REG_A2];

        for (int i = 0; i < memoryMappings.size(); i++) {
            Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
            int maddr = ((Integer)mapping.get("addr")).intValue(), mlen = ((Integer)mapping.get("length")).intValue();
            if (addr >= maddr && addr < maddr + mlen) { mapping.put("prot", new Integer(prot)); registers[REG_A0] = 0; return; }
        }
        
        registers[REG_A0] = -22;
    }
    private void handleMremap() {
        int old_addr = registers[REG_A0], old_size = registers[REG_A1], new_size = registers[REG_A2], flags = registers[REG_A3], new_addr = getSyscallParam(4);
        if (midlet.debug) { midlet.print("mremap: old=" + toHex(old_addr) + " oldsize=" + old_size + " newsize=" + new_size + " flags=" + flags + " newaddr=" + toHex(new_addr), stdout, id, scope); }
        if (new_addr == 0) { 
            for (int i = 0; i < memoryMappings.size(); i++) {
                Hashtable mapping = (Hashtable) memoryMappings.elementAt(i);
                int maddr = ((Integer)mapping.get("addr")).intValue(),mlen = ((Integer)mapping.get("length")).intValue();
                if (maddr == old_addr && mlen == old_size) { if (isMemoryRegionFree(maddr + mlen, new_size - old_size)) { mapping.put("length", new Integer(new_size)); registers[REG_A0] = maddr; return; } }
            }
        }

        registers[REG_A0] = -12;
    }
    private void handleBrk() {
        int newBrk = registers[REG_A0];
        if (newBrk == 0) { registers[REG_A0] = heapEnd; return; }
        if (newBrk < heapStart) { registers[REG_A0] = -1; return; }

        newBrk = (newBrk + 4095) & ~4095;
        if (newBrk > memory.length) { registers[REG_A0] = -12; return; }
        if (newBrk < heapEnd) {
            Vector keysToRemove = new Vector();
            Enumeration keys = allocatedBlocks.keys();
            while (keys.hasMoreElements()) {
                Integer addr = (Integer) keys.nextElement();
                Integer size = (Integer) allocatedBlocks.get(addr);
                if (addr.intValue() + size.intValue() > newBrk) { keysToRemove.addElement(addr); }
            }
            
            for (int i = 0; i < keysToRemove.size(); i++) { allocatedBlocks.remove(keysToRemove.elementAt(i)); }
        }
        
        heapEnd = newBrk;
        registers[REG_A0] = heapEnd;
    }
    // |
    private void handleSetjmp() {
        int jmpBufPtr = registers[REG_A0];
        if (jmpBufPtr + 132 > memory.length) { registers[REG_A0] = -14; return; }
        
        // Salvar registradores no jmp_buf
        for (int i = 0; i < 32; i++) { writeIntLE(memory, jmpBufPtr + i * 4, registers[i]); }
        
        // Salvar PC atual (é o endereço de retorno de setjmp)
        writeIntLE(memory, jmpBufPtr + 128, pc);
        
        int jmpBufId = nextJmpBufId++;
        jmpBufs.put(new Integer(jmpBufId), new Integer(jmpBufPtr));
        
        registers[REG_A0] = 0; // Primeira chamada retorna 0
    }
    private void handleLongjmp() {
        int jmpBufPtr = registers[REG_A0], val = registers[REG_A1];
        if (jmpBufPtr + 132 > memory.length) { running = false; return; }
        
        // Restaurar registradores
        for (int i = 0; i < 32; i++) { registers[i] = readIntLE(memory, jmpBufPtr + i * 4); }
        
        // Restaurar PC
        pc = readIntLE(memory, jmpBufPtr + 128);
        
        // Retornar valor não-zero
        registers[REG_A0] = (val == 0) ? 1 : val;
    }
    // |
    private void handleIoctl() {
        int fd = registers[REG_A0];
        int request = registers[REG_A1];
        int argp = registers[REG_A2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) {
            registers[REG_A0] = -9; // EBADF
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
                registers[REG_A0] = 0;
                break;
                
            case TCSETS:
            case TIOCSWINSZ:
                // Ignorar - terminal não configurável
                registers[REG_A0] = 0;
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
                registers[REG_A0] = 0;
                break;
                
            default:
                // IOCTL não suportado
                registers[REG_A0] = -25; // ENOTTY
                break;
        }
    }
    // | (Time)
    private void handleTime() { long currentTime = System.currentTimeMillis() / 1000; registers[REG_A0] = (int) currentTime; int timePtr = registers[REG_A1]; if (timePtr != 0 && timePtr >= 0 && timePtr + 3 < memory.length) { writeIntLE(memory, timePtr, (int) currentTime); } }
    private void handleGettimeofday() {
        int tvPtr = registers[REG_A0];
        int tzPtr = registers[REG_A1];
        
        long currentTimeMillis = System.currentTimeMillis();
        int seconds = (int)(currentTimeMillis / 1000);
        int microseconds = (int)((currentTimeMillis % 1000) * 1000);
        
        if (tvPtr != 0 && tvPtr >= 0 && tvPtr + 7 < memory.length) {
            writeIntLE(memory, tvPtr, seconds);
            writeIntLE(memory, tvPtr + 4, microseconds);
        }
        
        registers[REG_A0] = 0;
    }
    // |
    // | File System
    // | (Directories)
    private void handleMkdir() {
        int pathAddr = registers[REG_A0], mode = registers[REG_A1];

        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_A0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();

        if (path.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ_WRITE);
                if (conn.exists()) {
                    conn.close();
                    registers[REG_A0] = -17; // EEXIST
                    return;
                }
                conn.mkdir(); conn.close();
                registers[REG_A0] = 0; // Sucesso
            } catch (Exception e) { registers[REG_A0] = -1; }
        } else { registers[REG_A0] = -38; return; }
    }
    private void handleRmdir() {
        int pathAddr = registers[REG_A0];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_A0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();
        
        if (path.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ_WRITE);
                if (!conn.exists()) { conn.close(); registers[REG_A0] = -2; return; }
                if (!conn.isDirectory()) { conn.close(); registers[REG_A0] = -20; return; }

                Enumeration list = conn.list();
                if (list != null && list.hasMoreElements()) {
                    conn.close();
                    registers[REG_A0] = -39;
                    return;
                }
                
                conn.delete(); conn.close();
                registers[REG_A0] = 0; // Sucesso
            } catch (Exception e) { registers[REG_A0] = -1; }
        } else { registers[REG_A0] = -38; }
    }
    private void handleGetcwd() {
        int buf = registers[REG_A0], size = registers[REG_A1];
        
        String cwd = (String) scope.get("PWD");
        if (cwd == null) { cwd = "/home/"; }
        
        byte[] cwdBytes = cwd.getBytes();
        int len = Math.min(cwdBytes.length, size - 1);
        
        for (int i = 0; i < len && buf + i < memory.length; i++) { memory[buf + i] = cwdBytes[i]; }
        
        if (buf + len < memory.length) { memory[buf + len] = 0; }
        
        registers[REG_A0] = buf;
    }
    private void handleChdir() {
        int pathAddr = registers[REG_A0];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_A0] = -1; return; }

        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) { pathBuf.append((char)(memory[pathAddr + i] & 0xFF)); i++; }
        String path = pathBuf.toString();
        
        if (path.equals("") || path.equals(".")) { registers[REG_A0] = 0; return; }
        
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
        
        if (dirExists) { scope.put("PWD", fullPath); registers[REG_A0] = 0; }
        else { registers[REG_A0] = -2; }
    }
    private void handleGetdents() {
        int fd = registers[REG_A0], dirp = registers[REG_A1], count = registers[REG_A2];
        
        if (dirp < 0 || dirp >= memory.length) { registers[REG_A0] = -1; return; }
        
        Integer fdKey = new Integer(fd);
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) { registers[REG_A0] = -9; return; }
        
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
        
        if (dirPath == null) { registers[REG_A0] = -20; return; }
        
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
            else if (pwd.startsWith("/proc/")) {
                String rest = pwd.substring(6);
                int cuid = midlet.getCallerUid(scope);
                if (rest.equals("")) {
                    String[] pf = midlet.procFiles();
                    for (int i2 = 0; i2 < pf.length; i2++) { fileList.addElement(pf[i2]); }
                    Vector pe = midlet.procEntries(cuid);
                    for (int i2 = 0; i2 < pe.size(); i2++) { fileList.addElement(pe.elementAt(i2)); }
                } else {
                    String pd = rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
                    Vector pe = midlet.procDirEntries(pd, cuid);
                    for (int i2 = 0; i2 < pe.size(); i2++) { fileList.addElement(pe.elementAt(i2)); }
                }
            }
            else if (midlet.vfsDirIndex(pwd) != -1) {
                String content = midlet.loadRMS("OpenRMS", midlet.vfsDirIndex(pwd));
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
        } catch (Exception e) { registers[REG_A0] = -1; return; }
        
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
        
        if (written == 0) { registers[REG_A0] = 0; }
        else { registers[REG_A0] = offset; }
    }
    private void handleDup() {
        int oldfd = registers[REG_A0];
        Integer oldKey = new Integer(oldfd);
        
        if (!fileDescriptors.containsKey(oldKey) && oldfd != 0 && oldfd != 1 && oldfd != 2) { registers[REG_A0] = -9; return; }
        
        // Encontrar novo fd
        int newfd = nextFd++;
        while (fileDescriptors.containsKey(new Integer(newfd))) { newfd++; }

        if (oldfd == 0 || oldfd == 1 || oldfd == 2) { fileDescriptors.put(new Integer(newfd), (oldfd == 1 || oldfd == 2) ? stdout : null); }
        else { fileDescriptors.put(new Integer(newfd), fileDescriptors.get(oldKey)); }
        
        registers[REG_A0] = newfd;
    }
    private void handleDup2() {
        int oldfd = registers[REG_A0], newfd = registers[REG_A1];
        
        Integer oldKey = new Integer(oldfd);
        
        if (!fileDescriptors.containsKey(oldKey) && oldfd != 0 && oldfd != 1 && oldfd != 2) { registers[REG_A0] = -9; return; }
        
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
        
        registers[REG_A0] = newfd;
    }
    // | (Operations)
    private void handleCreat() {
        // creat(path, mode) é equivalente a open(path, O_CREAT | O_WRONLY | O_TRUNC, mode)
        int pathAddr = registers[REG_A0], mode = registers[REG_A1];
        
        registers[REG_A1] = O_CREAT | O_WRONLY | O_TRUNC;
        registers[REG_A2] = mode;
        
        handleOpen();
    }
    private void handleOpen() {
        int pathAddr = registers[REG_A0], flags = registers[REG_A1], mode = registers[REG_A2];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_A0] = -1; return; }
        
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
                
                if (fullPath.equals("/") || fullPath.equals("/home/") || fullPath.equals("/tmp/") || fullPath.equals("/bin/") || fullPath.equals("/etc/") || fullPath.equals("/lib/") || fullPath.equals("/boot/")) { isDir = true; } 
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
                    registers[REG_A0] = fd.intValue();
                } 
                else { registers[REG_A0] = -20; }
                return;
            }
            
            // Resto do código para arquivos...
            if (forReading) {
                InputStream is = midlet.getInputStream(fullPath, scope);
                if (is != null) {
                    Integer fd = new Integer(nextFd++);
                    fileDescriptors.put(fd, is);
                    registers[REG_A0] = fd.intValue();
                } else if (create) {
                    midlet.write(fullPath, "", id, scope);
                    InputStream is2 = midlet.getInputStream(fullPath, scope);
                    if (is2 != null) {
                        Integer fd = new Integer(nextFd++);
                        fileDescriptors.put(fd, is2);
                        registers[REG_A0] = fd.intValue();
                    } else { registers[REG_A0] = -1; }
                } else { registers[REG_A0] = -2; }
            } else if (forWriting) {
                // Para escrita, usamos um ByteArrayOutputStream temporário
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                
                // Se for append, carregar conteúdo existente
                if (append && !truncate) {
                    InputStream existing = midlet.getInputStream(fullPath, scope);
                    if (existing != null) {
                        int b;
                        while ((b = existing.read()) != -1) { baos.write(b); }
                        existing.close();
                    }
                }
                
                Integer fd = new Integer(nextFd++);
                fileDescriptors.put(fd, baos);
                registers[REG_A0] = fd.intValue();
                
                // Guardar o caminho para uso no close/flush
                fileDescriptors.put(fd + ":path", fullPath);
            } else { registers[REG_A0] = -1; }
        } catch (Exception e) { registers[REG_A0] = -1; }
    }
    private void handleClose() {
        int fd = registers[REG_A0];
        Integer fdKey = new Integer(fd);
        
        if (fd == 0 || fd == 1 || fd == 2) {
            // Não fechar stdin/stdout/stderr
            registers[REG_A0] = 0;
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
                            midlet.write(path, data, id, scope);
                        }
                    }
                }
                
                fileDescriptors.remove(fd);
                fileDescriptors.remove(fd + ":path");
                registers[REG_A0] = 0;
            } catch (Exception e) { registers[REG_A0] = -1; }
        } else { registers[REG_A0] = -1; }
    }
    private void handleUnlink() {
        int pathAddr = registers[REG_A0];
        if (pathAddr < 0 || pathAddr >= memory.length) { registers[REG_A0] = -1; return; }
        
        StringBuffer pathBuf = new StringBuffer();
        int i = 0;
        while (pathAddr + i < memory.length && memory[pathAddr + i] != 0 && i < 256) {
            pathBuf.append((char)(memory[pathAddr + i] & 0xFF));
            i++;
        }
        String path = pathBuf.toString();

        int result = midlet.deleteFile(path, id, scope);
        
        // Converter código de retorno do OpenTTY para errno
        switch (result) {
            case 0:  registers[REG_A0] = 0; break; // Sucesso
            case 2:  registers[REG_A0] = -22; break; // EINVAL
            case 5:  registers[REG_A0] = -2; break; // ENOENT
            case 13: registers[REG_A0] = -13; break; // EACCES
            case 127: registers[REG_A0] = -2; break; // ENOENT
            default: registers[REG_A0] = -1; break; // EPERM
        }
    }
    // |
    private void handleRead() {
        int fd = registers[REG_A0], buf = registers[REG_A1], count = registers[REG_A2];
        if (count <= 0 || buf < 0 || buf >= memory.length) { registers[REG_A0] = -1; return; }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 0) {
            // stdin - não implementado por enquanto
            registers[REG_A0] = 0;
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
                    registers[REG_A0] = bytesRead;
                } catch (Exception e) { registers[REG_A0] = -1; }
            } else { registers[REG_A0] = -1; }
        } else { registers[REG_A0] = -1; }
    }
    private void handleWrite() {
        int fd = registers[REG_A0], buf = registers[REG_A1], count = registers[REG_A2];
        if (count <= 0 || buf < 0 || buf >= memory.length) { registers[REG_A0] = -1; return; }
        
        Integer fdKey = new Integer(fd);
        
        if (fd == 1 || fd == 2) {
            // stdout/stderr - escrever no OpenTTY
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < count && buf + i < memory.length; i++) { sb.append((char)(memory[buf + i] & 0xFF)); }
            
            midlet.print(sb.toString(), stdout, id, scope, false);
            
            registers[REG_A0] = count;
            
        } else if (fileDescriptors.containsKey(fdKey)) {
            Object stream = fileDescriptors.get(fdKey);
            
            if (stream instanceof OutputStream) {
                try {
                    OutputStream os = (OutputStream) stream;
                    for (int i = 0; i < count && buf + i < memory.length; i++) { os.write(memory[buf + i]); }
                    os.flush();
                    registers[REG_A0] = count;
                } catch (Exception e) { registers[REG_A0] = -1; }
            } else if (stream instanceof StringBuffer) {
                StringBuffer sb = (StringBuffer) stream;
                for (int i = 0; i < count && buf + i < memory.length; i++) { sb.append((char)(memory[buf + i] & 0xFF)); }
                registers[REG_A0] = count;
            } else { registers[REG_A0] = -1; }
        } else { registers[REG_A0] = -1; }
    }
    // | (Informations)
    private void handleStat() {
        int pathAddr = registers[REG_A0], statbufAddr = registers[REG_A1];
        
        if (pathAddr < 0 || pathAddr >= memory.length || statbufAddr < 0 || statbufAddr >= memory.length) { registers[REG_A0] = -1; return; }
        
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
                InputStream is = midlet.getInputStream(path, scope);
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
        
        registers[REG_A0] = 0;
    }
    private void handleFstat() {
        int fd = registers[REG_A0];
        int statbufAddr = registers[REG_A1];
        
        if (statbufAddr < 0 || statbufAddr >= memory.length) {
            registers[REG_A0] = -1; // EFAULT
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
            registers[REG_A0] = -9; // EBADF
            return;
        }
        
        registers[REG_A0] = 0; // Sucesso
    }
    private void handleLseek() {
        int fd = registers[REG_A0], offset = registers[REG_A1], whence = registers[REG_A2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) { registers[REG_A0] = -9; return; } // EBADF
        
        // Implementação simplificada - sempre retorna sucesso mas não faz nada
        // Em uma implementação real, precisaríamos controlar a posição do arquivo
        registers[REG_A0] = 0; // Sucesso (sempre na posição 0)
    }
    private void handleFsync() {
        int fd = registers[REG_A0];
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) { registers[REG_A0] = -9; return; }

        try {
            if (fileDescriptors.containsKey(fdKey)) {
                Object stream = fileDescriptors.get(fdKey);
                if (stream instanceof OutputStream) { ((OutputStream) stream).flush(); }
            }
            registers[REG_A0] = 0;
        } catch (Exception e) { registers[REG_A0] = -1; }
    }
    // |
    // Network
    // | (Open and Connect)
    private void handleSocket() {
        int domain = registers[REG_A0], type = registers[REG_A1], protocol = registers[REG_A2];
        if (domain != AF_INET) { registers[REG_A0] = -97; return; }
        if (type != SOCK_STREAM && type != SOCK_DGRAM) { registers[REG_A0] = -22; return; }
        
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
            socketInfo.put("error", new Integer(0));
            socketInfo.put("options", new Hashtable());
            
            socketDescriptors.put(new Integer(fd), socketInfo);
            fileDescriptors.put(new Integer(fd), null); // Placeholder
            
            registers[REG_A0] = fd;
        } catch (Exception e) { registers[REG_A0] = -1; }
    }
    private void handleConnect() {
        int fd = registers[REG_A0], sockaddrPtr = registers[REG_A1], addrlen = registers[REG_A2];
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -9; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        
        // Ler estrutura sockaddr_in da memória
        if (sockaddrPtr + 16 > memory.length) { registers[REG_A0] = -14; return; }
        
        int sin_family = readShortLE(memory, sockaddrPtr), sin_port = readShortLE(memory, sockaddrPtr + 2);
        byte[] sin_addr = new byte[4];
        for (int i = 0; i < 4; i++) { sin_addr[i] = memory[sockaddrPtr + 4 + i]; }
        if (sin_family != AF_INET) { registers[REG_A0] = -97; return; }
        
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
            
            registers[REG_A0] = 0;
        } catch (Exception e) { socketInfo.put("error", new Integer(111)); registers[REG_A0] = -111; }
    }
    // | (Read and Write)
    private void handleSend() {
        int fd = registers[REG_A0], buf = registers[REG_A1], len = registers[REG_A2], flags = registers[REG_A3];
        Integer fdKey = new Integer(fd);

        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -9; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        if (!((Boolean) socketInfo.get("connected")).booleanValue()) { registers[REG_A0] = -107; return; }
        
        try {
            OutputStream os = (OutputStream) socketInfo.get("outputStream");
            if (os == null) { registers[REG_A0] = -9; return; }
            
            byte[] data = new byte[len];
            for (int i = 0; i < len && buf + i < memory.length; i++) { data[i] = memory[buf + i]; }
            
            os.write(data); os.flush();
            
            registers[REG_A0] = len;
        } catch (Exception e) { registers[REG_A0] = -32; }
    }
    private void handleRecv() {
        int fd = registers[REG_A0], buf = registers[REG_A1], len = registers[REG_A2], flags = registers[REG_A3];
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -9; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        if (!((Boolean) socketInfo.get("connected")).booleanValue()) { registers[REG_A0] = -107; return; }
        
        try {
            InputStream is = (InputStream) fileDescriptors.get(fdKey);
            if (is == null) { registers[REG_A0] = -9; return; }
            
            int bytesRead = 0;
            for (int i = 0; i < len && buf + i < memory.length; i++) {
                int b = is.read();
                if (b == -1) {
                    if (bytesRead == 0) { registers[REG_A0] = 0; }
                    else { registers[REG_A0] = bytesRead; }

                    return;
                }
                memory[buf + i] = (byte) b;
                bytesRead++;
            }
            
            registers[REG_A0] = bytesRead;
        } catch (Exception e) { registers[REG_A0] = -104; }
    }
    private void handleSendto() {
        // sendto(fd, buf, len, flags, dest_addr, addrlen)
        int fd = registers[REG_A0];
        int buf = registers[REG_A1];
        int len = registers[REG_A2];
        int flags = registers[REG_A3];
        
        // Parâmetros 5-6 na stack
        int dest_addr = getSyscallParam(4);
        int addrlen = getSyscallParam(5);
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        
        if (type == SOCK_STREAM && !((Boolean) socketInfo.get("connected")).booleanValue()) { registers[REG_A0] = -107; return; }
        
        byte[] data = new byte[len];
        for (int i = 0; i < len && buf + i < memory.length; i++) { data[i] = memory[buf + i]; }
        
        try {
            if (type == SOCK_DGRAM) {
                // Socket datagrama: enviar datagrama para o destino informado
                if (dest_addr == 0 || dest_addr + 16 > memory.length) { registers[REG_A0] = -14; return; }
                String[] target = readSockAddr(dest_addr);
                if (target == null) { registers[REG_A0] = -97; return; }
                
                DatagramConnection dc = (DatagramConnection) getOrCreateDatagram(socketInfo, 0);
                if (dc == null) { registers[REG_A0] = -1; return; }
                
                Datagram dg = dc.newDatagram(data, len, "datagram://" + target[0] + ":" + target[1]);
                dc.send(dg);
                registers[REG_A0] = dg.getLength();
            } else {
                // Socket conectado (TCP): ignora o destino e envia direto
                OutputStream os = (OutputStream) socketInfo.get("outputStream");
                if (os == null) { registers[REG_A0] = -ENOTSOCK; return; }
                
                os.write(data); os.flush();
                registers[REG_A0] = len;
            }
        } catch (Exception e) { registers[REG_A0] = -32; }
    }
    private void handleRecvfrom() {
        // recvfrom(fd, buf, len, flags, src_addr, addrlen)
        int fd = registers[REG_A0];
        int buf = registers[REG_A1];
        int len = registers[REG_A2];
        int flags = registers[REG_A3];
        
        // Parâmetros 5-6 na stack
        int src_addr = getSyscallParam(4);
        int addrlen = getSyscallParam(5);
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        
        if (type == SOCK_STREAM && !((Boolean) socketInfo.get("connected")).booleanValue()) { registers[REG_A0] = -107; return; }
        
        try {
            if (type == SOCK_DGRAM) {
                DatagramConnection dc = (DatagramConnection) socketInfo.get("datagram");
                if (dc == null) { registers[REG_A0] = -ENOTSOCK; return; }
                
                Datagram dg = dc.newDatagram(len);
                dc.receive(dg);
                
                byte[] data = dg.getData();
                int n = Math.min(dg.getLength(), len);
                for (int i = 0; i < n && buf + i < memory.length; i++) { memory[buf + i] = data[i]; }
                
                if (src_addr != 0) {
                    String[] peer = parseDatagramAddress(dg.getAddress());
                    if (peer != null) { writeSockAddr(memory, src_addr, peer[0], Integer.parseInt(peer[1])); }
                    if (addrlen != 0) { writeIntLE(memory, addrlen, 16); }
                }
                
                registers[REG_A0] = n;
            } else {
                // Socket conectado (TCP): recebe no buffer e reporta o peer
                InputStream is = (InputStream) fileDescriptors.get(fdKey);
                if (is == null) { registers[REG_A0] = -ENOTSOCK; return; }
                
                int bytesRead = 0;
                for (int i = 0; i < len && buf + i < memory.length; i++) {
                    int b = is.read();
                    if (b == -1) {
                        if (bytesRead == 0) { registers[REG_A0] = 0; }
                        else { registers[REG_A0] = bytesRead; }
                        return;
                    }
                    memory[buf + i] = (byte) b;
                    bytesRead++;
                }
                
                if (src_addr != 0) {
                    String[] peer = getSocketPeer(fdKey);
                    if (peer != null) { writeSockAddr(memory, src_addr, peer[0], Integer.parseInt(peer[1])); }
                    if (addrlen != 0) { writeIntLE(memory, addrlen, 16); }
                }
                
                registers[REG_A0] = bytesRead;
            }
        } catch (Exception e) { registers[REG_A0] = -104; }
    }
    // | (Socket Params)
    private void handleSetsockopt() {
        // setsockopt(fd, level, optname, optval, optlen)
        int fd = registers[REG_A0];
        int level = registers[REG_A1];
        int optname = registers[REG_A2];
        int optval = registers[REG_A3];
        
        // Parâmetro 5 na stack
        int optlen = getSyscallParam(4);
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        if (!storeSocketOption(fdKey, level, optname)) { registers[REG_A0] = -ENOPROTOOPT; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        Hashtable options = (Hashtable) socketInfo.get("options");
        if (options == null) { options = new Hashtable(); socketInfo.put("options", options); }
        int value = (optval != 0 && optval + 3 < memory.length) ? readIntLE(memory, optval) : 0;
        options.put(new Integer(level * 1000 + optname), new Integer(value));
        
        registers[REG_A0] = 0;
    }
    private void handleGetsockopt() {
        // getsockopt(fd, level, optname, optval, optlen)
        int fd = registers[REG_A0];
        int level = registers[REG_A1];
        int optname = registers[REG_A2];
        int optval = registers[REG_A3];
        
        // Parâmetro 5 na stack
        int optlen = getSyscallParam(4);
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        
        int value = 0;
        if (level == SOL_SOCKET && optname == SO_TYPE) {
            value = ((Integer) socketInfo.get("type")).intValue();
        } else if (level == SOL_SOCKET && optname == SO_ERROR) {
            Object err = socketInfo.get("error");
            value = (err == null) ? 0 : ((Integer) err).intValue();
            socketInfo.put("error", new Integer(0));
        } else if (!storeSocketOption(fdKey, level, optname)) { registers[REG_A0] = -ENOPROTOOPT; return; }
        else {
            Hashtable options = (Hashtable) socketInfo.get("options");
            Object stored = (options == null) ? null : options.get(new Integer(level * 1000 + optname));
            if (stored != null) { value = ((Integer) stored).intValue(); }
            else {
                switch (level) {
                    case SOL_SOCKET:
                        if (optname == SO_RCVBUF) { value = 65536; }
                        else if (optname == SO_SNDBUF) { value = 65536; }
                        break;
                }
            }
        }
        
        if (optval != 0 && optval + 3 < memory.length) { writeIntLE(memory, optval, value); }
        if (optlen != 0 && optlen + 3 < memory.length) { writeIntLE(memory, optlen, 4); }
        
        registers[REG_A0] = 0;
    }
    private boolean storeSocketOption(Integer fdKey, int level, int optname) {
        switch (level) {
            case SOL_SOCKET:
                switch (optname) {
                    case SO_REUSEADDR:
                    case SO_KEEPALIVE:
                    case SO_OOBINLINE:
                    case SO_BROADCAST:
                    case SO_DONTROUTE:
                    case SO_LINGER:
                    case SO_SNDBUF:
                    case SO_RCVBUF:
                        return true;
                    default:
                        return false;
                }
            case SOL_IP:
                return false;
            case IPPROTO_TCP:
                return optname == TCP_NODELAY;
            case IPPROTO_UDP:
                return false;
            default:
                return false;
        }
    }
    // |
    private void handleBind() {
        // bind(fd, addr, addrlen)
        int fd = registers[REG_A0];
        int sockaddrPtr = registers[REG_A1];
        int addrlen = registers[REG_A2];
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        if (sockaddrPtr == 0 || sockaddrPtr + 16 > memory.length) { registers[REG_A0] = -14; return; }
        
        String[] local = readSockAddr(sockaddrPtr);
        if (local == null) { registers[REG_A0] = -97; return; }
        int port = Integer.parseInt(local[1]);
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        
        try {
            // Fechar placeholder criado pelo socket()
            Object oldServer = socketInfo.get("server");
            if (oldServer != null) { try { ((StreamConnectionNotifier) oldServer).close(); } catch (Exception e) { } socketInfo.remove("server"); }
            Object oldDatagram = socketInfo.get("datagram");
            if (oldDatagram != null) { try { ((DatagramConnection) oldDatagram).close(); } catch (Exception e) { } socketInfo.remove("datagram"); }
            
            if (type == SOCK_STREAM) {
                StreamConnectionNotifier server = (StreamConnectionNotifier) Connector.open("socket://:" + port);
                socketInfo.put("server", server);
            } else {
                DatagramConnection dc = (DatagramConnection) Connector.open("datagram://:" + port);
                socketInfo.put("datagram", dc);
            }
            
            socketInfo.put("bound", Boolean.TRUE);
            socketInfo.put("localPort", new Integer(port));
            socketInfo.put("localIp", local[0]);
            socketInfo.put("error", new Integer(0));
            
            registers[REG_A0] = 0;
        } catch (Exception e) { registers[REG_A0] = -EADDRINUSE; }
    }
    private void handleListen() {
        // listen(fd, backlog)
        int fd = registers[REG_A0];
        int backlog = registers[REG_A1];
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        if (type != SOCK_STREAM) { registers[REG_A0] = -22; return; }
        
        try {
            // "listen" sem bind: vincula porta efêmera (como no Linux)
            StreamConnectionNotifier server = (StreamConnectionNotifier) socketInfo.get("server");
            if (server == null) {
                server = (StreamConnectionNotifier) Connector.open("socket://:0");
                socketInfo.put("server", server);
            }
            socketInfo.put("listening", Boolean.TRUE);
            registers[REG_A0] = 0;
        } catch (Exception e) { registers[REG_A0] = -1; }
    }
    private void handleAccept() {
        // accept(fd, addr, addrlen, flags)
        int fd = registers[REG_A0];
        int addrPtr = registers[REG_A1];
        int addrlenPtr = registers[REG_A2];
        
        Integer fdKey = new Integer(fd);
        
        if (!socketDescriptors.containsKey(fdKey)) { registers[REG_A0] = -ENOTSOCK; return; }
        
        Hashtable socketInfo = (Hashtable) socketDescriptors.get(fdKey);
        int type = ((Integer) socketInfo.get("type")).intValue();
        if (type != SOCK_STREAM) { registers[REG_A0] = -22; return; }
        
        StreamConnectionNotifier server = (StreamConnectionNotifier) socketInfo.get("server");
        if (server == null) { registers[REG_A0] = -22; return; }
        
        try {
            SocketConnection conn = (SocketConnection) server.acceptAndOpen();
            
            int newFd = nextFd++;
            Integer newKey = new Integer(newFd);
            
            Hashtable accepted = new Hashtable();
            accepted.put("type", new Integer(SOCK_STREAM));
            accepted.put("protocol", new Integer(IPPROTO_TCP));
            accepted.put("connection", conn);
            accepted.put("connected", Boolean.TRUE);
            accepted.put("error", new Integer(0));
            accepted.put("options", new Hashtable());
            
            InputStream is = conn.openInputStream();
            OutputStream os = conn.openOutputStream();
            accepted.put("outputStream", os);
            
            socketDescriptors.put(newKey, accepted);
            fileDescriptors.put(newKey, is);
            
            if (addrPtr != 0) {
                String peerIp = conn.getAddress();
                int peerPort = conn.getPort();
                if (peerIp != null) { writeSockAddr(memory, addrPtr, peerIp, peerPort); }
                if (addrlenPtr != 0 && addrlenPtr + 3 < memory.length) { writeIntLE(memory, addrlenPtr, 16); }
            }
            
            registers[REG_A0] = newFd;
        } catch (Exception e) { registers[REG_A0] = -1; }
    }
    private void handleShutdown() { registers[REG_A0] = 0; }
    private void handleNanosleep() { registers[REG_A0] = 0; }
    private void handleGetsockname() { registers[REG_A0] = -1; } // Não implementado
    private void handleGetpeername() { registers[REG_A0] = -1; } // Não implementado

    // Métodos auxiliares para estruturas sockaddr_in
    private String[] readSockAddr(int ptr) {
        if (ptr == 0 || ptr + 16 > memory.length) { return null; }
        if (readShortLE(memory, ptr) != AF_INET) { return null; }
        int port = readShortLE(memory, ptr + 2) & 0xFFFF;
        String ip = (memory[ptr + 4] & 0xFF) + "." + (memory[ptr + 5] & 0xFF) + "." + (memory[ptr + 6] & 0xFF) + "." + (memory[ptr + 7] & 0xFF);
        return new String[] { ip, String.valueOf(port) };
    }
    private void writeSockAddr(byte[] mem, int addr, String ip, int port) {
        if (addr >= 0 && addr + 16 <= mem.length) {
            int family = AF_INET;
            writeShortLE(mem, addr, (short) family);
            writeShortLE(mem, addr + 2, (short) port);
            String[] parts = midlet.split(ip, '.');
            for (int i = 0; i < 4; i++) {
                if (i < parts.length) { try { mem[addr + 4 + i] = (byte) Integer.parseInt(parts[i].trim()); } catch (Exception e) { mem[addr + 4 + i] = 0; } }
                else { mem[addr + 4 + i] = 0; }
            }
            for (int i = 8; i < 16; i++) { mem[addr + i] = 0; }
        }
    }
    private String[] getSocketPeer(Integer fdKey) {
        Hashtable info = (Hashtable) socketDescriptors.get(fdKey);
        if (info == null) { return null; }
        Object conn = info.get("connection");
        if (!(conn instanceof SocketConnection)) { return null; }
        try { return new String[] { ((SocketConnection) conn).getAddress(), String.valueOf(((SocketConnection) conn).getPort()) }; }
        catch (Exception e) { return null; }
    }
    private Object getOrCreateDatagram(Hashtable info, int port) {
        DatagramConnection dc = (DatagramConnection) info.get("datagram");
        if (dc == null) {
            try { dc = (DatagramConnection) Connector.open("datagram://:" + port); info.put("datagram", dc); }
            catch (Exception e) { return null; }
        }
        return dc;
    }
    private String[] parseDatagramAddress(String addr) {
        if (addr == null) { return null; }
        String a = addr;
        if (a.startsWith("datagram://")) { a = a.substring("datagram://".length()); }
        int colon = a.lastIndexOf(':');
        if (colon == -1) { return null; }
        return new String[] { a.substring(0, colon), a.substring(colon + 1) };
    }

    private void handleFutex() {
        // Parametros 1-4 em a0-a3
        int uaddr = registers[REG_A0];
        int op = registers[REG_A1];
        int val = registers[REG_A2];
        int timeout = registers[REG_A3];
        
        // Parâmetros 5-6 na stack
        int uaddr2 = getSyscallParam(4);
        int val3 = getSyscallParam(5);
        
        if (midlet.debug) {
            midlet.print("futex: uaddr=" + toHex(uaddr) + " op=" + op +
                        " val=" + val + " timeout=" + timeout, stdout, id, scope);
        }
        
        // Implementação simplificada
        switch (op & 0x7F) { // Mask out flags
            case 0: // FUTEX_WAIT
                int currentVal = readIntLE(memory, uaddr);
                if (currentVal != val) {
                    registers[REG_A0] = -11; // EAGAIN
                } else {
                    // Adicionar à lista de espera
                    Vector waiters = (Vector) futexWaiters.get(new Integer(uaddr));
                    if (waiters == null) {
                        waiters = new Vector();
                        futexWaiters.put(new Integer(uaddr), waiters);
                    }
                    waiters.addElement(new Integer(id));
                    registers[REG_A0] = 0;
                }
                break;
                
            case 1: // FUTEX_WAKE
                Vector waiters = (Vector) futexWaiters.get(new Integer(uaddr));
                if (waiters != null) {
                    int wakeCount = Math.min(val, waiters.size());
                    for (int i = 0; i < wakeCount; i++) {
                        waiters.removeElementAt(0);
                    }
                    registers[REG_A0] = wakeCount;
                } else {
                    registers[REG_A0] = 0;
                }
                break;
                
            default:
                registers[REG_A0] = -38; // ENOSYS
        }
    }
    
    private void handleSchedYield() { registers[REG_A0] = 0; }
    
    private void handleUname() {
        int buf = registers[REG_A0];
        
        if (buf == 0 || buf + 65 * 5 > memory.length) {
            registers[REG_A0] = -14; // EFAULT
            return;
        }

        String sysname = "Linux";
        String nodename = "opentty";
        String release = "3.2.0";
        String version = "#1 " + midlet.build;
        String machine = "riscv32";
        String domainname = "";
        
        writeString(memory, buf, sysname, 65);
        writeString(memory, buf + 65, nodename, 65);
        writeString(memory, buf + 130, release, 65);
        writeString(memory, buf + 195, version, 65);
        writeString(memory, buf + 260, machine, 65);
        writeString(memory, buf + 325, domainname, 65);
        
        registers[REG_A0] = 0;
    }
    

    
    private void handleFcntl() {
        int fd = registers[REG_A0];
        int cmd = registers[REG_A1];
        int arg = registers[REG_A2];
        
        Integer fdKey = new Integer(fd);
        
        if (!fileDescriptors.containsKey(fdKey) && fd != 0 && fd != 1 && fd != 2) {
            registers[REG_A0] = -9; // EBADF
            return;
        }
        
        switch (cmd) {
            case F_GETFL:
                // Retornar flags do arquivo
                registers[REG_A0] = 0; // Por padrão, sem flags especiais
                break;
                
            case F_SETFL:
                // Configurar flags - ignorado por enquanto
                registers[REG_A0] = 0;
                break;
                
            default:
                registers[REG_A0] = -22; // EINVAL
        }
    }
    
    private void handleFtruncate() {
        int fd = registers[REG_A0];
        int length = registers[REG_A1];
        
        // Implementação simplificada
        registers[REG_A0] = 0;
    }
    private void handleTruncate() {
        int path = registers[REG_A0];
        int length = registers[REG_A1];
        
        // Implementação simplificada
        registers[REG_A0] = 0;
    }
    
    private void handleGetrlimit() {
        int resource = registers[REG_A0];
        int rlim = registers[REG_A1];
        
        if (rlim == 0 || rlim + 8 > memory.length) {
            registers[REG_A0] = -14; // EFAULT
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
        
        registers[REG_A0] = 0;
    }
    
    private void checkPendingSignals() { }

    private void pushSignalFrame(int sig) {
        // Salvar contexto atual na stack
        int sp = registers[REG_SP];
        
        // Push registers
        for (int i = 0; i < 32; i++) {
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
    

    private void handleSelect() { int nfds = registers[REG_A0], readfds = registers[REG_A1], writefds = registers[REG_A2], exceptfds = registers[REG_A3], timeoutPtr = getSyscallParam(4); registers[REG_A0] = 0; }
    private void handlePoll() { int fdsPtr = registers[REG_A0], nfds = registers[REG_A1], timeout = registers[REG_A2]; registers[REG_A0] = 0; }

    // Métodos auxiliares para leitura/escrita little-endian
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); } 
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }

    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    private void writeShortLE(byte[] data, int offset, short value) { if (offset + 1 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); }

    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }

    private int findFreeMemoryRegion(int length) {
        int start = heapEnd;
        
        while (start + length > start && start + length <= stackPointer - 4096) {
            boolean free = true;
            
            for (int i = 0; i < sharedObjectMappings.size(); i++) {
                Hashtable mapping = (Hashtable) sharedObjectMappings.elementAt(i);
                int addr = ((Integer) mapping.get("addr")).intValue(), size = ((Integer) mapping.get("length")).intValue();
                if (start < addr + size && start + length > addr) { free = false; start = addr + size; break; }
            }
            if (free) { start = (start + 4095) & ~4095; return start; }
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
    private int getSyscallParam(int paramIndex) {
        // Parâmetros 0-7 estão em a0-a7 (x10-x17)
        if (paramIndex < 8) {
            return registers[10 + paramIndex];
        }
        
        // Parametros depois de a7 ficam na stack pela convencao RV32.
        int sp = registers[REG_SP];
        int offset = (paramIndex - 8) * 4;
        
        if (sp + offset + 3 < memory.length && sp + offset >= 0) {
            return readIntLE(memory, sp + offset);
        }
        
        return 0;
    }


    private String readString(byte[] data, int offset, int maxLen) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < maxLen && offset + i >= 0 && offset + i < data.length; i++) {
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

    private void debugMemoryAccess(int addr, int size, boolean write, int value) { if (midlet.debug && addr < 0x10000) { String op = write ? "WRITE" : "READ"; midlet.print("MEM " + op + " at " + toHex(addr) + " size=" + size + (write ? " value=" + toHex(value) : ""), stdout, id, scope); } }
}
