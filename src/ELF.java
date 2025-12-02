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
    
    // Syscalls Linux ARM (EABI) - TODAS AS SYSCALLS
    private static final int SYS_EXIT = 1;
    private static final int SYS_FORK = 2;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_CREAT = 8;
    private static final int SYS_LINK = 9;
    private static final int SYS_UNLINK = 10;
    private static final int SYS_EXECVE = 11;
    private static final int SYS_CHDIR = 12;
    private static final int SYS_TIME = 13;
    private static final int SYS_MKNOD = 14;
    private static final int SYS_CHMOD = 15;
    private static final int SYS_LCHOWN = 16;
    private static final int SYS_STAT = 18;
    private static final int SYS_LSEEK = 19;
    private static final int SYS_GETPID = 20;
    private static final int SYS_MOUNT = 21;
    private static final int SYS_UMOUNT = 22;
    private static final int SYS_SETUID = 23;
    private static final int SYS_GETUID = 24;
    private static final int SYS_STIME = 25;
    private static final int SYS_PTRACE = 26;
    private static final int SYS_ALARM = 27;
    private static final int SYS_PAUSE = 29;
    private static final int SYS_UTIME = 30;
    private static final int SYS_ACCESS = 33;
    private static final int SYS_SYNC = 36;
    private static final int SYS_KILL = 37;
    private static final int SYS_RENAME = 38;
    private static final int SYS_MKDIR = 39;
    private static final int SYS_RMDIR = 40;
    private static final int SYS_DUP = 41;
    private static final int SYS_PIPE = 42;
    private static final int SYS_TIMES = 43;
    private static final int SYS_BRK = 45;
    private static final int SYS_SETGID = 46;
    private static final int SYS_GETGID = 47;
    private static final int SYS_GETEUID = 49;
    private static final int SYS_GETEGID = 50;
    private static final int SYS_ACCT = 51;
    private static final int SYS_IOCTL = 54;
    private static final int SYS_FCNTL = 55;
    private static final int SYS_UMASK = 60;
    private static final int SYS_CHROOT = 61;
    private static final int SYS_DUP2 = 63;
    private static final int SYS_GETPPID = 64;
    private static final int SYS_GETPGRP = 65;
    private static final int SYS_SETSID = 66;
    private static final int SYS_SIGACTION = 67;
    private static final int SYS_SIGPENDING = 73;
    private static final int SYS_SIGPROCMASK = 74;
    private static final int SYS_SIGSUSPEND = 75;
    private static final int SYS_SIGRETURN = 76;
    private static final int SYS_WAIT4 = 114;
    private static final int SYS_GETSID = 124;
    private static final int SYS_READLINK = 85;
    private static final int SYS_EXECV = 11;
    private static final int SYS_TRUNCATE = 92;
    private static final int SYS_FTRUNCATE = 93;
    private static final int SYS_FCHMOD = 94;
    private static final int SYS_FCHOWN = 95;
    private static final int SYS_GETPRIORITY = 96;
    private static final int SYS_SETPRIORITY = 97;
    private static final int SYS_STATFS = 99;
    private static final int SYS_FSTATFS = 100;
    private static final int SYS_SYSLOG = 103;
    private static final int SYS_SETITIMER = 104;
    private static final int SYS_GETITIMER = 105;
    private static final int SYS_WAITPID = 106;
    private static final int SYS_SWAPOFF = 115;
    private static final int SYS_SYSINFO = 116;
    private static final int SYS_FSYNC = 118;
    private static final int SYS_FDATASYNC = 119;
    private static final int SYS_TRUNCATE64 = 193;
    private static final int SYS_FTRUNCATE64 = 194;
    private static final int SYS_GETDENTS = 141;
    private static final int SYS_GETDENTS64 = 217;
    private static final int SYS_FCHDIR = 133;
    private static final int SYS_FSTAT = 108;
    private static final int SYS_SELECT = 142;
    private static final int SYS_POLL = 168;
    private static final int SYS_READV = 145;
    private static final int SYS_WRITEV = 146;
    private static final int SYS_PREAD = 180;
    private static final int SYS_PWRITE = 181;
    private static final int SYS_PREADV = 333;
    private static final int SYS_PWRITEV = 334;
    private static final int SYS_MMAP = 192;
    private static final int SYS_MUNMAP = 91;
    private static final int SYS_MLOCK = 149;
    private static final int SYS_MUNLOCK = 150;
    private static final int SYS_MLOCKALL = 151;
    private static final int SYS_MUNLOCKALL = 152;
    private static final int SYS_MPROTECT = 125;
    private static final int SYS_MSYNC = 144;
    private static final int SYS_MINCORE = 218;
    private static final int SYS_MADVISE = 219;
    private static final int SYS_SOCKET = 281;
    private static final int SYS_BIND = 282;
    private static final int SYS_CONNECT = 283;
    private static final int SYS_LISTEN = 284;
    private static final int SYS_ACCEPT = 285;
    private static final int SYS_GETSOCKNAME = 286;
    private static final int SYS_GETPEERNAME = 287;
    private static final int SYS_SOCKETPAIR = 288;
    private static final int SYS_SEND = 289;
    private static final int SYS_RECV = 290;
    private static final int SYS_SENDTO = 291;
    private static final int SYS_RECVFROM = 292;
    private static final int SYS_SHUTDOWN = 293;
    private static final int SYS_SETSOCKOPT = 294;
    private static final int SYS_GETSOCKOPT = 295;
    private static final int SYS_SENDMSG = 296;
    private static final int SYS_RECVMSG = 297;
    private static final int SYS_SEMGET = 299;
    private static final int SYS_SEMOP = 300;
    private static final int SYS_SEMCTL = 301;
    private static final int SYS_MSGGET = 303;
    private static final int SYS_MSGSND = 304;
    private static final int SYS_MSGRCV = 305;
    private static final int SYS_MSGCTL = 306;
    private static final int SYS_SHMGET = 307;
    private static final int SYS_SHMAT = 308;
    private static final int SYS_SHMDT = 309;
    private static final int SYS_SHMCTL = 310;
    private static final int SYS_CLONE = 120;
    private static final int SYS_VFORK = 190;
    private static final int SYS_CAPGET = 184;
    private static final int SYS_CAPSET = 185;
    private static final int SYS_SIGALTSTACK = 186;
    private static final int SYS_SIGTIMEDWAIT = 187;
    private static final int SYS_SIGWAITINFO = 188;
    private static final int SYS_SET_TID_ADDRESS = 256;
    private static final int SYS_TIMER_CREATE = 257;
    private static final int SYS_TIMER_SETTIME = 258;
    private static final int SYS_TIMER_GETTIME = 259;
    private static final int SYS_TIMER_GETOVERRUN = 260;
    private static final int SYS_TIMER_DELETE = 261;
    private static final int SYS_CLOCK_SETTIME = 264;
    private static final int SYS_CLOCK_GETTIME = 265;
    private static final int SYS_CLOCK_GETRES = 266;
    private static final int SYS_CLOCK_NANOSLEEP = 267;
    private static final int SYS_TGKILL = 268;
    private static final int SYS_SET_ROBUST_LIST = 273;
    private static final int SYS_GET_ROBUST_LIST = 274;
    private static final int SYS_FUTEX = 240;
    private static final int SYS_SCHED_SETPARAM = 154;
    private static final int SYS_SCHED_GETPARAM = 155;
    private static final int SYS_SCHED_SETSCHEDULER = 156;
    private static final int SYS_SCHED_GETSCHEDULER = 157;
    private static final int SYS_SCHED_YIELD = 158;
    private static final int SYS_SCHED_GET_PRIORITY_MAX = 159;
    private static final int SYS_SCHED_GET_PRIORITY_MIN = 160;
    private static final int SYS_SCHED_RR_GET_INTERVAL = 161;
    private static final int SYS_NANOSLEEP = 162;
    private static final int SYS_MREMAP = 163;
    private static final int SYS_SETRESUID = 164;
    private static final int SYS_GETRESUID = 165;
    private static final int SYS_SETRESGID = 170;
    private static final int SYS_GETRESGID = 171;
    private static final int SYS_PRCTL = 172;
    private static final int SYS_RT_SIGRETURN = 173;
    private static final int SYS_RT_SIGACTION = 174;
    private static final int SYS_RT_SIGPROCMASK = 175;
    private static final int SYS_RT_SIGPENDING = 176;
    private static final int SYS_RT_SIGTIMEDWAIT = 177;
    private static final int SYS_RT_SIGQUEUEINFO = 178;
    private static final int SYS_RT_SIGSUSPEND = 179;
    private static final int SYS_PERSONALITY = 191;
    private static final int SYS_USTAT = 62;
    private static final int SYS_STATFS64 = 266;
    private static final int SYS_FSTATFS64 = 267;
    private static final int SYS_SYSFS = 135;
    private static final int SYS_GETCPU = 345;
    private static final int SYS_EPOLL_CREATE = 250;
    private static final int SYS_EPOLL_CTL = 251;
    private static final int SYS_EPOLL_WAIT = 252;
    private static final int SYS_REMAP_FILE_PAGES = 253;
    private static final int SYS_SET_TLS = 360;
    private static final int SYS_EXIT_GROUP = 248;
    private static final int SYS_EPOLL_CREATE1 = 357;
    private static final int SYS_SIGNALFD = 349;
    private static final int SYS_TEE = 350;
    private static final int SYS_SYNC_FILE_RANGE = 351;
    private static final int SYS_VMSPLICE = 343;
    private static final int SYS_MOVE_PAGES = 344;
    private static final int SYS_GETPAGESIZE = 64;
    private static final int SYS_MBIND = 319;
    private static final int SYS_GET_MEMPOLICY = 320;
    private static final int SYS_SET_MEMPOLICY = 321;
    private static final int SYS_MIGRATE_PAGES = 322;
    private static final int SYS_SET_THREAD_AREA = 244;
    private static final int SYS_INOTIFY_INIT = 316;
    private static final int SYS_INOTIFY_ADD_WATCH = 317;
    private static final int SYS_INOTIFY_RM_WATCH = 318;
    private static final int SYS_IOPRIO_GET = 314;
    private static final int SYS_IOPRIO_SET = 315;
    private static final int SYS_GET_RANDOM_BYTES = 355;
    private static final int SYS_READAHEAD = 225;
    private static final int SYS_SCHED_SETAFFINITY = 241;
    private static final int SYS_SCHED_GETAFFINITY = 242;
    private static final int SYS_SETXATTR = 226;
    private static final int SYS_LSETXATTR = 227;
    private static final int SYS_FSETXATTR = 228;
    private static final int SYS_GETXATTR = 229;
    private static final int SYS_LGETXATTR = 230;
    private static final int SYS_FGETXATTR = 231;
    private static final int SYS_LISTXATTR = 232;
    private static final int SYS_LLISTXATTR = 233;
    private static final int SYS_FLISTXATTR = 234;
    private static final int SYS_REMOVEXATTR = 235;
    private static final int SYS_LREMOVEXATTR = 236;
    private static final int SYS_FREMOVEXATTR = 237;
    private static final int SYS_TIMERFD_CREATE = 350;
    private static final int SYS_TIMERFD_SETTIME = 351;
    private static final int SYS_TIMERFD_GETTIME = 352;
    private static final int SYS_SIGNALFD4 = 353;
    private static final int SYS_EVENTFD = 354;
    private static final int SYS_EVENTFD2 = 356;
    private static final int SYS_DUP3 = 358;
    private static final int SYS_PIPE2 = 359;
    private static final int SYS_INOTIFY_INIT1 = 332;
    private static final int SYS_RT_TGSIGQUEUEINFO = 335;
    private static final int SYS_PERF_EVENT_OPEN = 364;
    private static final int SYS_RECVMMSG = 365;
    private static final int SYS_FANOTIFY_INIT = 366;
    private static final int SYS_FANOTIFY_MARK = 367;
    private static final int SYS_PRLIMIT64 = 369;
    private static final int SYS_NAME_TO_HANDLE_AT = 370;
    private static final int SYS_OPEN_BY_HANDLE_AT = 371;
    private static final int SYS_CLOCK_ADJTIME = 372;
    private static final int SYS_SYNCFS = 373;
    private static final int SYS_SETNS = 375;
    private static final int SYS_SENDMMSG = 376;
    private static final int SYS_PROCESS_VM_READV = 377;
    private static final int SYS_PROCESS_VM_WRITEV = 378;
    private static final int SYS_KCMP = 379;
    private static final int SYS_FINIT_MODULE = 380;
    private static final int SYS_SCHED_SETATTR = 381;
    private static final int SYS_SCHED_GETATTR = 382;
    private static final int SYS_RENAMEAT2 = 383;
    private static final int SYS_SECCOMP = 384;
    private static final int SYS_GETRANDOM = 385;
    private static final int SYS_MEMFD_CREATE = 386;
    private static final int SYS_BPF = 387;
    private static final int SYS_EXECVEAT = 388;
    private static final int SYS_USERFAULTFD = 389;
    private static final int SYS_MEMBARRIER = 390;
    private static final int SYS_MLOCK2 = 391;
    private static final int SYS_COPY_FILE_RANGE = 392;
    private static final int SYS_PREADV2 = 393;
    private static final int SYS_PWRITEV2 = 394;
    private static final int SYS_PKEY_MPROTECT = 395;
    private static final int SYS_PKEY_ALLOC = 396;
    private static final int SYS_PKEY_FREE = 397;
    private static final int SYS_STATX = 397;
    private static final int SYS_GETCWD = 183;
    
    // Flags de open
    private static final int O_RDONLY = 0;
    private static final int O_WRONLY = 1;
    private static final int O_RDWR = 2;
    private static final int O_CREAT = 64;
    private static final int O_APPEND = 1024;
    private static final int O_TRUNC = 512;
    
    public ELF(OpenTTY midlet, Object stdout, Hashtable scope, int id) {
        this.midlet = midlet;
        this.stdout = stdout;
        this.scope = scope;
        this.id = id;
        this.pid = midlet.genpid();
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
        
        // Registrar como processo ativo
        Hashtable proc = midlet.genprocess("elf", id, null);
        proc.put("elf", this); midlet.sys.put(pid, proc);
        
        try {
            while (running && pc < memory.length - 3 && midlet.sys.containsKey(pid)) {
                int instruction = readIntLE(memory, pc);
                pc += 4;
                executeInstruction(instruction);
            }
            
        } catch (Exception e) {
            midlet.print("ELF execution error: " + e.toString(), stdout);
            running = false;
        } finally {
            if (midlet.sys.containsKey(pid)) { midlet.sys.remove(pid); }
        }
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
                    long add_result = (long)rnValue + (long)shifter_operand;
                    result = (int)add_result;
                    updateCarry = true;
                    shifter_carry_out = (add_result >>> 32) & 0x1;
                    break;
                case 0x5: // ADC (Add with Carry)
                    long adc_result = (long)rnValue + (long)shifter_operand + carry_in;
                    result = (int)adc_result;
                    updateCarry = true;
                    shifter_carry_out = (adc_result >>> 32) & 0x1;
                    break;
                case 0x6: // SBC (Subtract with Carry)
                    long sbc_result = (long)rnValue - (long)shifter_operand - (1 - carry_in);
                    result = (int)sbc_result;
                    updateCarry = true;
                    shifter_carry_out = (rnValue >= (shifter_operand + (1 - carry_in))) ? 1 : 0;
                    break;
                case 0x7: // RSC (Reverse Subtract with Carry)
                    long rsc_result = (long)shifter_operand - (long)rnValue - (1 - carry_in);
                    result = (int)rsc_result;
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
                    long cmn_result_long = (long)rnValue + (long)shifter_operand;
                    result = (int)(cmn_result_long & 0xFFFFFFFFL); // Apenas 32 bits
                    setFlags = 1; // CMN sempre atualiza flags
                    updateCarry = true;
                    shifter_carry_out = (cmn_result_long >>> 32) & 0x1;
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
        // Primeiro, tentar as syscalls já implementadas
        switch (number) {
            case SYS_WRITE:
                handleWrite();
                return;
                
            case SYS_READ:
                handleRead();
                return;
                
            case SYS_OPEN:
                handleOpen();
                return;
                
            case SYS_CLOSE:
                handleClose();
                return;
                
            case SYS_EXIT:
                handleExit();
                return;
                
            case SYS_GETPID:
                try { registers[REG_R0] = Integer.valueOf(this.pid); } catch (NumberFormatException e) { }
                return;
                
            case SYS_GETCWD:
                handleGetcwd();
                return;
                
            case SYS_BRK:
                registers[REG_R0] = memory.length;
                return;
        }
        
        // Para todas as outras syscalls, retornar "não implementado" por enquanto
        midlet.print("Syscall " + number + " (" + getSyscallName(number) + ") not implemented", stdout);
        registers[REG_R0] = -1; // ENOSYS - Function not implemented
    }
    
    private String getSyscallName(int number) {
        switch (number) {
            case SYS_EXIT: return "exit";
            case SYS_FORK: return "fork";
            case SYS_READ: return "read";
            case SYS_WRITE: return "write";
            case SYS_OPEN: return "open";
            case SYS_CLOSE: return "close";
            case SYS_CREAT: return "creat";
            case SYS_LINK: return "link";
            case SYS_UNLINK: return "unlink";
            case SYS_EXECVE: return "execve";
            case SYS_CHDIR: return "chdir";
            case SYS_TIME: return "time";
            case SYS_MKNOD: return "mknod";
            case SYS_CHMOD: return "chmod";
            case SYS_LCHOWN: return "lchown";
            case SYS_STAT: return "stat";
            case SYS_LSEEK: return "lseek";
            case SYS_GETPID: return "getpid";
            case SYS_MOUNT: return "mount";
            case SYS_UMOUNT: return "umount";
            case SYS_SETUID: return "setuid";
            case SYS_GETUID: return "getuid";
            case SYS_STIME: return "stime";
            case SYS_PTRACE: return "ptrace";
            case SYS_ALARM: return "alarm";
            case SYS_PAUSE: return "pause";
            case SYS_UTIME: return "utime";
            case SYS_ACCESS: return "access";
            case SYS_SYNC: return "sync";
            case SYS_KILL: return "kill";
            case SYS_RENAME: return "rename";
            case SYS_MKDIR: return "mkdir";
            case SYS_RMDIR: return "rmdir";
            case SYS_DUP: return "dup";
            case SYS_PIPE: return "pipe";
            case SYS_TIMES: return "times";
            case SYS_BRK: return "brk";
            case SYS_SETGID: return "setgid";
            case SYS_GETGID: return "getgid";
            case SYS_GETEUID: return "geteuid";
            case SYS_GETEGID: return "getegid";
            case SYS_ACCT: return "acct";
            case SYS_IOCTL: return "ioctl";
            case SYS_FCNTL: return "fcntl";
            case SYS_UMASK: return "umask";
            case SYS_CHROOT: return "chroot";
            case SYS_DUP2: return "dup2";
            case SYS_GETPPID: return "getppid";
            case SYS_GETPGRP: return "getpgrp";
            case SYS_SETSID: return "setsid";
            case SYS_SIGACTION: return "sigaction";
            case SYS_SIGPENDING: return "sigpending";
            case SYS_SIGPROCMASK: return "sigprocmask";
            case SYS_SIGSUSPEND: return "sigsuspend";
            case SYS_SIGRETURN: return "sigreturn";
            case SYS_WAIT4: return "wait4";
            case SYS_GETSID: return "getsid";
            case SYS_READLINK: return "readlink";
            case SYS_TRUNCATE: return "truncate";
            case SYS_FTRUNCATE: return "ftruncate";
            case SYS_FCHMOD: return "fchmod";
            case SYS_FCHOWN: return "fchown";
            case SYS_GETPRIORITY: return "getpriority";
            case SYS_SETPRIORITY: return "setpriority";
            case SYS_STATFS: return "statfs";
            case SYS_FSTATFS: return "fstatfs";
            case SYS_SYSLOG: return "syslog";
            case SYS_SETITIMER: return "setitimer";
            case SYS_GETITIMER: return "getitimer";
            case SYS_WAITPID: return "waitpid";
            case SYS_SWAPOFF: return "swapoff";
            case SYS_SYSINFO: return "sysinfo";
            case SYS_FSYNC: return "fsync";
            case SYS_FDATASYNC: return "fdatasync";
            case SYS_TRUNCATE64: return "truncate64";
            case SYS_FTRUNCATE64: return "ftruncate64";
            case SYS_GETDENTS: return "getdents";
            case SYS_GETDENTS64: return "getdents64";
            case SYS_FCHDIR: return "fchdir";
            case SYS_FSTAT: return "fstat";
            case SYS_SELECT: return "select";
            case SYS_POLL: return "poll";
            case SYS_READV: return "readv";
            case SYS_WRITEV: return "writev";
            case SYS_PREAD: return "pread";
            case SYS_PWRITE: return "pwrite";
            case SYS_PREADV: return "preadv";
            case SYS_PWRITEV: return "pwritev";
            case SYS_MMAP: return "mmap";
            case SYS_MUNMAP: return "munmap";
            case SYS_MLOCK: return "mlock";
            case SYS_MUNLOCK: return "munlock";
            case SYS_MLOCKALL: return "mlockall";
            case SYS_MUNLOCKALL: return "munlockall";
            case SYS_MPROTECT: return "mprotect";
            case SYS_MSYNC: return "msync";
            case SYS_MINCORE: return "mincore";
            case SYS_MADVISE: return "madvise";
            case SYS_SOCKET: return "socket";
            case SYS_BIND: return "bind";
            case SYS_CONNECT: return "connect";
            case SYS_LISTEN: return "listen";
            case SYS_ACCEPT: return "accept";
            case SYS_GETSOCKNAME: return "getsockname";
            case SYS_GETPEERNAME: return "getpeername";
            case SYS_SOCKETPAIR: return "socketpair";
            case SYS_SEND: return "send";
            case SYS_RECV: return "recv";
            case SYS_SENDTO: return "sendto";
            case SYS_RECVFROM: return "recvfrom";
            case SYS_SHUTDOWN: return "shutdown";
            case SYS_SETSOCKOPT: return "setsockopt";
            case SYS_GETSOCKOPT: return "getsockopt";
            case SYS_SENDMSG: return "sendmsg";
            case SYS_RECVMSG: return "recvmsg";
            case SYS_SEMGET: return "semget";
            case SYS_SEMOP: return "semop";
            case SYS_SEMCTL: return "semctl";
            case SYS_MSGGET: return "msgget";
            case SYS_MSGSND: return "msgsnd";
            case SYS_MSGRCV: return "msgrcv";
            case SYS_MSGCTL: return "msgctl";
            case SYS_SHMGET: return "shmget";
            case SYS_SHMAT: return "shmat";
            case SYS_SHMDT: return "shmdt";
            case SYS_SHMCTL: return "shmctl";
            case SYS_CLONE: return "clone";
            case SYS_VFORK: return "vfork";
            case SYS_CAPGET: return "capget";
            case SYS_CAPSET: return "capset";
            case SYS_SIGALTSTACK: return "sigaltstack";
            case SYS_SIGTIMEDWAIT: return "sigtimedwait";
            case SYS_SIGWAITINFO: return "sigwaitinfo";
            case SYS_SET_TID_ADDRESS: return "set_tid_address";
            case SYS_TIMER_CREATE: return "timer_create";
            case SYS_TIMER_SETTIME: return "timer_settime";
            case SYS_TIMER_GETTIME: return "timer_gettime";
            case SYS_TIMER_GETOVERRUN: return "timer_getoverrun";
            case SYS_TIMER_DELETE: return "timer_delete";
            case SYS_CLOCK_SETTIME: return "clock_settime";
            case SYS_CLOCK_GETTIME: return "clock_gettime";
            case SYS_CLOCK_GETRES: return "clock_getres";
            case SYS_CLOCK_NANOSLEEP: return "clock_nanosleep";
            case SYS_TGKILL: return "tgkill";
            case SYS_SET_ROBUST_LIST: return "set_robust_list";
            case SYS_GET_ROBUST_LIST: return "get_robust_list";
            case SYS_FUTEX: return "futex";
            case SYS_SCHED_SETPARAM: return "sched_setparam";
            case SYS_SCHED_GETPARAM: return "sched_getparam";
            case SYS_SCHED_SETSCHEDULER: return "sched_setscheduler";
            case SYS_SCHED_GETSCHEDULER: return "sched_getscheduler";
            case SYS_SCHED_YIELD: return "sched_yield";
            case SYS_SCHED_GET_PRIORITY_MAX: return "sched_get_priority_max";
            case SYS_SCHED_GET_PRIORITY_MIN: return "sched_get_priority_min";
            case SYS_SCHED_RR_GET_INTERVAL: return "sched_rr_get_interval";
            case SYS_NANOSLEEP: return "nanosleep";
            case SYS_MREMAP: return "mremap";
            case SYS_SETRESUID: return "setresuid";
            case SYS_GETRESUID: return "getresuid";
            case SYS_SETRESGID: return "setresgid";
            case SYS_GETRESGID: return "getresgid";
            case SYS_PRCTL: return "prctl";
            case SYS_RT_SIGRETURN: return "rt_sigreturn";
            case SYS_RT_SIGACTION: return "rt_sigaction";
            case SYS_RT_SIGPROCMASK: return "rt_sigprocmask";
            case SYS_RT_SIGPENDING: return "rt_sigpending";
            case SYS_RT_SIGTIMEDWAIT: return "rt_sigtimedwait";
            case SYS_RT_SIGQUEUEINFO: return "rt_sigqueueinfo";
            case SYS_RT_SIGSUSPEND: return "rt_sigsuspend";
            case SYS_PERSONALITY: return "personality";
            case SYS_USTAT: return "ustat";
            case SYS_STATFS64: return "statfs64";
            case SYS_FSTATFS64: return "fstatfs64";
            case SYS_SYSFS: return "sysfs";
            case SYS_GETCPU: return "getcpu";
            case SYS_EPOLL_CREATE: return "epoll_create";
            case SYS_EPOLL_CTL: return "epoll_ctl";
            case SYS_EPOLL_WAIT: return "epoll_wait";
            case SYS_REMAP_FILE_PAGES: return "remap_file_pages";
            case SYS_SET_TLS: return "set_tls";
            case SYS_EXIT_GROUP: return "exit_group";
            case SYS_EPOLL_CREATE1: return "epoll_create1";
            case SYS_SIGNALFD: return "signalfd";
            case SYS_TEE: return "tee";
            case SYS_SYNC_FILE_RANGE: return "sync_file_range";
            case SYS_VMSPLICE: return "vmsplice";
            case SYS_MOVE_PAGES: return "move_pages";
            case SYS_GETPAGESIZE: return "getpagesize";
            case SYS_MBIND: return "mbind";
            case SYS_GET_MEMPOLICY: return "get_mempolicy";
            case SYS_SET_MEMPOLICY: return "set_mempolicy";
            case SYS_MIGRATE_PAGES: return "migrate_pages";
            case SYS_SET_THREAD_AREA: return "set_thread_area";
            case SYS_INOTIFY_INIT: return "inotify_init";
            case SYS_INOTIFY_ADD_WATCH: return "inotify_add_watch";
            case SYS_INOTIFY_RM_WATCH: return "inotify_rm_watch";
            case SYS_IOPRIO_GET: return "ioprio_get";
            case SYS_IOPRIO_SET: return "ioprio_set";
            case SYS_GET_RANDOM_BYTES: return "get_random_bytes";
            case SYS_READAHEAD: return "readahead";
            case SYS_SCHED_SETAFFINITY: return "sched_setaffinity";
            case SYS_SCHED_GETAFFINITY: return "sched_getaffinity";
            case SYS_SETXATTR: return "setxattr";
            case SYS_LSETXATTR: return "lsetxattr";
            case SYS_FSETXATTR: return "fsetxattr";
            case SYS_GETXATTR: return "getxattr";
            case SYS_LGETXATTR: return "lgetxattr";
            case SYS_FGETXATTR: return "fgetxattr";
            case SYS_LISTXATTR: return "listxattr";
            case SYS_LLISTXATTR: return "llistxattr";
            case SYS_FLISTXATTR: return "flistxattr";
            case SYS_REMOVEXATTR: return "removexattr";
            case SYS_LREMOVEXATTR: return "lremovexattr";
            case SYS_FREMOVEXATTR: return "fremovexattr";
            case SYS_TIMERFD_CREATE: return "timerfd_create";
            case SYS_TIMERFD_SETTIME: return "timerfd_settime";
            case SYS_TIMERFD_GETTIME: return "timerfd_gettime";
            case SYS_SIGNALFD4: return "signalfd4";
            case SYS_EVENTFD: return "eventfd";
            case SYS_EVENTFD2: return "eventfd2";
            case SYS_DUP3: return "dup3";
            case SYS_PIPE2: return "pipe2";
            case SYS_INOTIFY_INIT1: return "inotify_init1";
            case SYS_RT_TGSIGQUEUEINFO: return "rt_tgsigqueueinfo";
            case SYS_PERF_EVENT_OPEN: return "perf_event_open";
            case SYS_RECVMMSG: return "recvmmsg";
            case SYS_FANOTIFY_INIT: return "fanotify_init";
            case SYS_FANOTIFY_MARK: return "fanotify_mark";
            case SYS_PRLIMIT64: return "prlimit64";
            case SYS_NAME_TO_HANDLE_AT: return "name_to_handle_at";
            case SYS_OPEN_BY_HANDLE_AT: return "open_by_handle_at";
            case SYS_CLOCK_ADJTIME: return "clock_adjtime";
            case SYS_SYNCFS: return "syncfs";
            case SYS_SETNS: return "setns";
            case SYS_SENDMMSG: return "sendmmsg";
            case SYS_PROCESS_VM_READV: return "process_vm_readv";
            case SYS_PROCESS_VM_WRITEV: return "process_vm_writev";
            case SYS_KCMP: return "kcmp";
            case SYS_FINIT_MODULE: return "finit_module";
            case SYS_SCHED_SETATTR: return "sched_setattr";
            case SYS_SCHED_GETATTR: return "sched_getattr";
            case SYS_RENAMEAT2: return "renameat2";
            case SYS_SECCOMP: return "seccomp";
            case SYS_GETRANDOM: return "getrandom";
            case SYS_MEMFD_CREATE: return "memfd_create";
            case SYS_BPF: return "bpf";
            case SYS_EXECVEAT: return "execveat";
            case SYS_USERFAULTFD: return "userfaultfd";
            case SYS_MEMBARRIER: return "membarrier";
            case SYS_MLOCK2: return "mlock2";
            case SYS_COPY_FILE_RANGE: return "copy_file_range";
            case SYS_PREADV2: return "preadv2";
            case SYS_PWRITEV2: return "pwritev2";
            case SYS_PKEY_MPROTECT: return "pkey_mprotect";
            case SYS_PKEY_ALLOC: return "pkey_alloc";
            case SYS_PKEY_FREE: return "pkey_free";
            case SYS_STATX: return "statx";
            case SYS_GETCWD: return "getcwd";
            default: return "unknown(" + number + ")";
        }
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
            
            midlet.print(sb.toString(), stdout);
            
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
                
                fileDescriptors.remove(fdKey);
                fileDescriptors.remove(fd + ":path");
                registers[REG_R0] = 0;
                
            } catch (Exception e) {
                registers[REG_R0] = -1;
            }
        } else {
            registers[REG_R0] = -1;
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