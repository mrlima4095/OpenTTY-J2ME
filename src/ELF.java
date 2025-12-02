import java.util.*;
import java.io.*;
// |
public class ELF {
    private OpenTTY midlet;
    private Object stdout;
    private Hashtable scope;
    private int id;
    
    // Memória e registradores
    private byte[] memory;
    private int[] registers;
    private int pc;
    private boolean running;
    private int stackPointer;
    
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
    
    // Syscalls Linux ARM (EABI)
    private static final int SYS_EXIT = 1;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_BRK = 45;
    private static final int SYS_GETPID = 20;
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
        this.memory = new byte[1024 * 1024]; // 1MB de memória
        this.registers = new int[16];
        this.running = false;
        this.stackPointer = memory.length - 1024;
        this.fileDescriptors = new Hashtable();
        this.nextFd = 3; // 0=stdin, 1=stdout, 2=stderr
        
        // Inicializar file descriptors padrão
        fileDescriptors.put(new Integer(1), stdout); // stdout
        fileDescriptors.put(new Integer(2), stdout); // stderr
    }
    
    private String toHex(int value) {
        String hex = Integer.toHexString(value);
        while (hex.length() < 8) {
            hex = "0" + hex;
        }
        return "0x" + hex;
    }
    
    public boolean load(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] elfData = baos.toByteArray();
        
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { midlet.print("Not a valid ELF file", stdout); return false; }
        if (elfData[4] != ELFCLASS32) {  midlet.print("Only 32-bit ELF supported", stdout); return false; }
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
        
        try {
            while (running && pc < memory.length - 3) {
                int instruction = readIntLE(memory, pc);
                pc += 4;
                executeInstruction(instruction);
            }
            
        } catch (Exception e) {
            midlet.print("ELF execution error", stdout);
            running = false;
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
            int rn = (instruction >> 16) & 0xF;
            int rd = (instruction >> 12) & 0xF;
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            int shifter_operand = rotateRight(imm, rotate);
            
            switch (opcode) {
                case 13: // MOV (immediate)
                    registers[rd] = shifter_operand;
                    break;
                case 4: // ADD (immediate)
                    registers[rd] = registers[rn] + shifter_operand;
                    break;
                case 2: // SUB (immediate)
                    registers[rd] = registers[rn] - shifter_operand;
                    break;
                case 0: // AND (immediate)
                    registers[rd] = registers[rn] & shifter_operand;
                    break;
                case 12: // ORR (immediate)
                    registers[rd] = registers[rn] | shifter_operand;
                    break;
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
            }
            
            if (isLoad) {
                if (address >= 0 && address < memory.length - 3) {
                    if (isByte) {
                        registers[rd] = memory[address] & 0xFF;
                    } else {
                        int alignedAddr = address & ~3;
                        registers[rd] = readIntLE(memory, alignedAddr);
                    }
                }
            } else {
                if (address >= 0 && address < memory.length - 3) {
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
                registers[REG_LR] = pc - 4;
            }
            
            pc = pc + offset - 4;
            return;
        }
        
        if ((instruction & 0x0FF00000) == 0x02800000 || (instruction & 0x0FF00000) == 0x02800000) {
            int rd = (instruction >> 12) & 0xF;
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            int offset = rotateRight(imm, rotate);
            
            // ADR calcula endereço relativo ao PC
            // PC já aponta para próxima instrução (pc atual)
            registers[rd] = pc - 4 + offset;
            return;
        }

        // NOP
        if (instruction == 0xE1A00000) {
            return;
        }
    }
    
    private void handleSyscall(int number) {
        switch (number) {
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
                
            case SYS_EXIT:
                handleExit();
                break;
                
            case SYS_GETPID:
                registers[REG_R0] = 1000 + midlet.random.nextInt(9000);
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
                        if (stream instanceof InputStream) {
                            ((InputStream) stream).close();
                        } else if (stream instanceof OutputStream) {
                            ((OutputStream) stream).close();
                        }
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
    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) return 0; return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); } 
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
    
    private void writeIntLE(byte[] data, int offset, int value) { if (offset + 3 >= data.length || offset < 0) { return; } data[offset] = (byte)(value & 0xFF); data[offset + 1] = (byte)((value >> 8) & 0xFF); data[offset + 2] = (byte)((value >> 16) & 0xFF); data[offset + 3] = (byte)((value >> 24) & 0xFF); }
    
    private int rotateRight(int value, int amount) { amount &= 31; return (value >>> amount) | (value << (32 - amount)); }
}