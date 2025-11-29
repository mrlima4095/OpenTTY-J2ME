import javax.microedition.midlet.*;
import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import javax.microedition.io.file.*;
import java.util.*;
import java.io.*;

public class ELFEmulator {
    private MIDlet midlet;
    private Display display;
    private Object stdout;
    
    // Memória e CPU
    private byte[] memory = new byte[1024 * 1024]; // 1MB RAM
    private int[] registers = new int[16]; // R0-R15
    private int pc, sp; // Program Counter, Stack Pointer
    private boolean running = false;
    
    // Syscalls Linux ARM
    private static final int SYS_EXIT = 1;
    private static final int SYS_READ = 3;
    private static final int SYS_WRITE = 4;
    private static final int SYS_OPEN = 5;
    private static final int SYS_CLOSE = 6;
    private static final int SYS_BRK = 45;
    private static final int SYS_MMAP2 = 192;
    private static final int SYS_IOCTL = 54;
    
    // File descriptors
    private Hashtable fileDescriptors = new Hashtable();
    private int nextFd = 3; // 0=stdin, 1=stdout, 2=stderr
    
    public ELFEmulator(MIDlet midlet, Object stdout) {
        this.midlet = midlet;
        this.display = midlet.display;
        this.stdout = stdout;
        initialize();
    }
    
    private void initialize() {
        // Inicializar registradores
        sp = memory.length - 4; // Stack no topo da memória
        registers[13] = sp; // SP
        registers[14] = 0;  // LR
        registers[15] = 0;  // PC será definido pelo ELF
        
        // File descriptors padrão
        fileDescriptors.put(new Integer(0), "stdin");
        fileDescriptors.put(new Integer(1), "stdout"); 
        fileDescriptors.put(new Integer(2), "stderr");
    }
    
    // Método principal para executar ELF
    public int runELF(byte[] elfData) {
        try {
            // 1. Carregar ELF
            int entryPoint = loadELF(elfData);
            if (entryPoint == -1) {
                print("Invalid ELF file");
                return 1;
            }
            
            // 2. Configurar PC
            pc = entryPoint;
            registers[15] = pc;
            
            // 3. Executar
            return execute();
            
        } catch (Exception e) {
            print("ELF execution failed: " + e.getMessage());
            return 1;
        }
    }
    
    public int runELFFile(String filename) {
        try {
            byte[] elfData = readFile(filename);
            if (elfData.length == 0) {
                print("File not found: " + filename);
                return 1;
            }
            return runELF(elfData);
        } catch (Exception e) {
            print("Error reading file: " + e.getMessage());
            return 1;
        }
    }
    
    // ============ LOADER ELF ============
    private int loadELF(byte[] elfData) {
        // Verificar magic number
        if (elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') {
            return -1;
        }
        
        // Ler header ELF
        int entryPoint = readInt(elfData, 24);
        int phoff = readInt(elfData, 28);    // Program header offset
        int phentsize = readShort(elfData, 42); // Program header entry size  
        int phnum = readShort(elfData, 44);  // Number of program headers
        
        print("Loading ELF: entry=0x" + Integer.toHexString(entryPoint) + 
              ", segments=" + phnum);
        
        // Carregar segments
        for (int i = 0; i < phnum; i++) {
            int offset = phoff + (i * phentsize);
            int type = readInt(elfData, offset);
            int vaddr = readInt(elfData, offset + 8);
            int filesz = readInt(elfData, offset + 16);
            int memsz = readInt(elfData, offset + 20);
            int fileOffset = readInt(elfData, offset + 4);
            
            if (type == 1) { // PT_LOAD
                loadSegment(elfData, vaddr, filesz, memsz, fileOffset);
            }
        }
        
        return entryPoint;
    }
    
    private void loadSegment(byte[] elfData, int vaddr, int filesz, int memsz, int fileOffset) {
        // Copiar dados do arquivo para memória
        for (int i = 0; i < filesz && (vaddr + i) < memory.length; i++) {
            if (fileOffset + i < elfData.length) {
                memory[vaddr + i] = elfData[fileOffset + i];
            }
        }
        
        // Zerar resto da memória (bss)
        for (int i = filesz; i < memsz && (vaddr + i) < memory.length; i++) {
            memory[vaddr + i] = 0;
        }
        
        print("Loaded segment: vaddr=0x" + Integer.toHexString(vaddr) + 
              ", size=" + filesz + "/" + memsz + " bytes");
    }
    
    // ============ CPU EMULATOR ============
    private int execute() throws Exception {
        running = true;
        int instructionCount = 0;
        final int MAX_INSTRUCTIONS = 1000000;
        
        while (running && instructionCount < MAX_INSTRUCTIONS) {
            if (pc >= memory.length - 3) {
                throw new Exception("PC out of bounds: 0x" + Integer.toHexString(pc));
            }
            
            // Buscar instrução (ARM little-endian)
            int instruction = readMemory32(pc);
            
            // Decodificar e executar
            if (decodeAndExecute(instruction)) {
                instructionCount++;
            } else {
                // Instrução não implementada - tentar tratar como syscall
                if ((instruction & 0xFF) == 0xEF) { // SWI no ARM
                    handleSyscall();
                } else {
                    throw new Exception("Unsupported instruction: 0x" + 
                                      Integer.toHexString(instruction) + " at 0x" + 
                                      Integer.toHexString(pc));
                }
            }
            
            // Verificar se ainda está rodando
            if (!running) break;
        }
        
        if (instructionCount >= MAX_INSTRUCTIONS) {
            print("Execution limit reached");
            return 2;
        }
        
        return 0;
    }
    
    private boolean decodeAndExecute(int instruction) {
        // Decodificação básica de instruções ARM
        int opcode = (instruction >> 21) & 0xF;
        int rn = (instruction >> 16) & 0xF;
        int rd = (instruction >> 12) & 0xF;
        int rm = instruction & 0xF;
        
        switch (opcode) {
            case 0x0: // AND
            case 0x1: // EOR
            case 0x2: // SUB
            case 0x3: // RSB
            case 0x4: // ADD
                return executeALU(opcode, rn, rd, rm, instruction);
                
            case 0x5: // ADC
            case 0x6: // SBC  
            case 0x7: // RSC
            case 0x8: // TST
            case 0x9: // TEQ
            case 0xA: // CMP
            case 0xB: // CMN
            case 0xC: // ORR
            case 0xD: // MOV
            case 0xE: // BIC
            case 0xF: // MVN
                // Instruções mais complexas - pular por enquanto
                return false;
                
            default:
                return false;
        }
    }
    
    private boolean executeALU(int opcode, int rn, int rd, int rm, int instruction) {
        int operand1 = registers[rn];
        int operand2 = getOperand2(instruction);
        int result = 0;
        
        switch (opcode) {
            case 0x2: // SUB
                result = operand1 - operand2;
                break;
            case 0x4: // ADD
                result = operand1 + operand2;
                break;
            case 0xD: // MOV
                result = operand2;
                break;
            default:
                return false; // Não implementado
        }
        
        registers[rd] = result;
        updateFlags(result);
        pc += 4;
        return true;
    }
    
    private int getOperand2(int instruction) {
        if ((instruction & (1 << 25)) != 0) {
            // Immediate value
            int imm = instruction & 0xFF;
            int rotate = ((instruction >> 8) & 0xF) * 2;
            return Integer.rotateRight(imm, rotate);
        } else {
            // Register
            int rm = instruction & 0xF;
            return registers[rm];
        }
    }
    
    private void updateFlags(int result) {
        // Implementação simplificada de flags
        // Z (Zero), N (Negative)
        // Para uma implementação real, precisaria de mais flags
    }
    
    // ============ SYSCALL HANDLER ============
    private void handleSyscall() throws Exception {
        int syscallNum = registers[7]; // R7 contém o número da syscall
        int arg1 = registers[0];
        int arg2 = registers[1];
        int arg3 = registers[2];
        
        int result = -1;
        
        switch (syscallNum) {
            case SYS_EXIT:
                result = syscallExit(arg1);
                break;
            case SYS_READ:
                result = syscallRead(arg1, arg2, arg3);
                break;
            case SYS_WRITE:
                result = syscallWrite(arg1, arg2, arg3);
                break;
            case SYS_OPEN:
                result = syscallOpen(arg1, arg2, arg3);
                break;
            case SYS_CLOSE:
                result = syscallClose(arg1);
                break;
            case SYS_BRK:
                result = syscallBrk(arg1);
                break;
            default:
                print("Unsupported syscall: " + syscallNum);
                result = -1; // ENOSYS
        }
        
        // Resultado em R0
        registers[0] = result;
        pc += 4; // Avançar para próxima instrução
    }
    
    private int syscallExit(int status) {
        print("Program exited with status: " + status);
        running = false;
        return 0;
    }
    
    private int syscallRead(int fd, int buffer, int count) {
        try {
            Object fileObj = fileDescriptors.get(new Integer(fd));
            if (fileObj == null) return -1; // EBADF
            
            if (fd == 0) { // stdin
                // Simular entrada - retornar EOF por enquanto
                return 0;
            } else if (fileObj instanceof String) {
                String filename = (String)fileObj;
                byte[] data = readFile(filename);
                if (data.length > 0) {
                    int bytesToCopy = Math.min(count, data.length);
                    writeMemory(buffer, data, bytesToCopy);
                    return bytesToCopy;
                }
                return 0; // EOF
            }
            
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int syscallWrite(int fd, int buffer, int count) {
        try {
            Object fileObj = fileDescriptors.get(new Integer(fd));
            if (fileObj == null) return -1; // EBADF
            
            if (fd == 1 || fd == 2) { // stdout/stderr
                byte[] data = readMemory(buffer, count);
                String text = new String(data, "UTF-8");
                print(text);
                return count;
            } else if (fileObj instanceof String) {
                String filename = (String)fileObj;
                byte[] data = readMemory(buffer, count);
                // Aqui você implementaria escrita em arquivo
                return count;
            }
            
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int syscallOpen(int filenamePtr, int flags, int mode) {
        try {
            String filename = readCString(filenamePtr);
            if (filename == null) return -1;
            
            // Verificar se arquivo existe
            byte[] data = readFile(filename);
            if (data.length == 0) return -1; // ENOENT
            
            int fd = nextFd++;
            fileDescriptors.put(new Integer(fd), filename);
            return fd;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private int syscallClose(int fd) {
        if (fileDescriptors.remove(new Integer(fd)) != null) {
            return 0;
        }
        return -1; // EBADF
    }
    
    private int syscallBrk(int newBrk) {
        // Implementação simplificada - sempre sucesso
        return newBrk;
    }
    
    // ============ MEMORY ACCESS ============
    private int readMemory32(int address) {
        if (address < 0 || address >= memory.length - 3) return 0;
        
        return (memory[address] & 0xFF) |
               ((memory[address + 1] & 0xFF) << 8) |
               ((memory[address + 2] & 0xFF) << 16) |
               ((memory[address + 3] & 0xFF) << 24);
    }
    
    private byte[] readMemory(int address, int length) {
        byte[] data = new byte[length];
        for (int i = 0; i < length && (address + i) < memory.length; i++) {
            data[i] = memory[address + i];
        }
        return data;
    }
    
    private void writeMemory(int address, byte[] data, int length) {
        for (int i = 0; i < length && (address + i) < memory.length; i++) {
            memory[address + i] = data[i];
        }
    }
    
    private String readCString(int address) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; (address + i) < memory.length; i++) {
            byte b = memory[address + i];
            if (b == 0) break;
            sb.append((char)b);
        }
        return sb.toString();
    }
    
    // ============ UTILITIES ============
    private byte[] readFile(String filename) {
        try {
            if (filename.startsWith("/")) {
                filename = filename.substring(1);
            }
            
            InputStream is = getClass().getResourceAsStream("/" + filename);
            if (is == null) return new byte[0];
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            is.close();
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }
    
    private int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) | 
               ((data[offset + 1] & 0xFF) << 8) |
               ((data[offset + 2] & 0xFF) << 16) | 
               ((data[offset + 3] & 0xFF) << 24);
    }
    
    private int readShort(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
    
    private void print(String message) {
        midlet.print(message, stdout);
    }
    
    // ============ PUBLIC API ============
    public static void main(String[] args) {
        // Exemplo de uso
        if (args.length > 0) {
            ELFEmulator emulator = new ELFEmulator(null, System.out);
            int result = emulator.runELFFile(args[0]);
            System.exit(result);
        }
    }
}