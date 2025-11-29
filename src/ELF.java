import java.io.InputStream;
import java.io.IOException;

/**
 * ELF loader + tiny x86-32 interpreter, reescrito para J2ME (CLDC 1.1 / MIDP 2.0)
 *
 * Restrições J2ME atendidas:
 * - Não usa java.util.Arrays
 * - Não usa ByteArrayOutputStream
 * - Não usa encodings explícitos (UTF-8)
 * - Usa apenas APIs básicas disponíveis no CLDC
 *
 * Funcionalidade:
 * - Carrega ELF32 little-endian (apenas segmentos PT_LOAD)
 * - Emula subset de instruções x86 suficiente para programas "minimal" em C que usam int 0x80:
 *     mov r32, imm32 (0xB8..0xBF)
 *     mov r32, r32 (0x8B /r)  (mod==3)
 *     mov r/m32, r32 (0x89 /r) (mod==3)
 *     push r32 (0x50..0x57), pop r32 (0x58..0x5F)
 *     call rel32 (0xE8), ret (0xC3), jmp rel32 (0xE9)
 *     nop (0x90), int imm8 (0xCD)
 * - Syscalls via int 0x80 (1 exit, 3 read, 4 write, 5 open stub, 6 close stub)
 *
 * Limitações:
 * - Não suporta relocations, PLT/GOT, dynamic linking, ou instruções complexas
 * - modrm suportado apenas quando mod == 3 (registro-direto)
 *
 * Integração: precisa de uma classe OpenTTY com métodos/atributos usados aqui:
 *   host.print(String msg, int stream); host.write(String path, byte[] data, int off);
 *   host.stdin.getString(); host.stdout etc.
 */
public class ELF {
    private OpenTTY host;
    privaestdout

    private byte[] mem;
    private int memSize;

    // Registers
    public int EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP;
    public int EIP;

    private int entryPoint = 0;
    private boolean halted = false;

    private static final int REG_EAX = 0, REG_ECX = 1, REG_EDX = 2, REG_EBX = 3, REG_ESP = 4, REG_EBP = 5, REG_ESI = 6, REG_EDI = 7;

    public ELF(OpenTTY host, Object stdout) {
        this.host = host;
    }

    /**
     * Lê todo InputStream em um buffer (implementação J2ME-safe)
     */
    private byte[] readAll(InputStream is) throws IOException {
        int capacity = 8192;
        byte[] buf = new byte[capacity];
        int total = 0;
        while (true) {
            int toRead = capacity - total;
            if (toRead == 0) {
                // expand
                int newCap = capacity * 2;
                byte[] nb = new byte[newCap];
                System.arraycopy(buf, 0, nb, 0, total);
                buf = nb;
                capacity = newCap;
                toRead = capacity - total;
            }
            int r = is.read(buf, total, toRead);
            if (r < 0) break;
            total += r;
        }
        // trim
        byte[] out = new byte[total];
        System.arraycopy(buf, 0, out, 0, total);
        return out;
    }

    /**
     * Carrega ELF a partir de InputStream
     */
    public boolean load(InputStream is) throws IOException {
        byte[] file = readAll(is);
        if (file == null || file.length < 52) {
            host.print("ELF: arquivo muito pequeno", host.stdout);
            return false;
        }
        if (!(file[0] == 0x7f && file[1] == 'E' && file[2] == 'L' && file[3] == 'F')) {
            host.print("ELF: magic inválido", host.stdout);
            return false;
        }
        int elfClass = unsigned(file[4]); // 1 = 32-bit
        int data = unsigned(file[5]);     // 1 = little endian
        if (elfClass != 1 || data != 1) {
            host.print("ELF: somente ELF32 little-endian suportado (classe=" + elfClass + " data=" + data + ")", host.stdout);
            return false;
        }

        int e_entry = read32LE(file, 24);
        int e_phoff = read32LE(file, 28);
        int e_phentsize = read16LE(file, 42);
        int e_phnum = read16LE(file, 44);

        this.entryPoint = e_entry;

        // calcula highest used virtual address pelos PT_LOAD
        int highest = 0;
        for (int i = 0; i < e_phnum; i++) {
            int phoff = e_phoff + i * e_phentsize;
            if (phoff + 32 > file.length) continue;
            int p_type = read32LE(file, phoff + 0);
            if (p_type != 1) continue; // PT_LOAD
            int p_offset = read32LE(file, phoff + 4);
            int p_vaddr  = read32LE(file, phoff + 8);
            int p_filesz = read32LE(file, phoff + 16);
            int p_memsz  = read32LE(file, phoff + 20);
            int top = p_vaddr + (p_memsz > p_filesz ? p_memsz : p_filesz);
            if (top > highest) highest = top;
        }

        int stackSize = 128 * 1024; // 128KB stack
        int baseMem = (highest + 1);
        if (baseMem < 1024 * 1024) baseMem = 1024 * 1024;
        memSize = alignUp(baseMem, 4096) + stackSize;
        mem = new byte[memSize];
        // zero fill (for J2ME, array already zeroed, mas deixamos seguro)
        for (int i = 0; i < memSize; i++) mem[i] = 0;

        // carregar segmentos PT_LOAD
        for (int i = 0; i < e_phnum; i++) {
            int phoff = e_phoff + i * e_phentsize;
            if (phoff + 32 > file.length) continue;
            int p_type = read32LE(file, phoff + 0);
            if (p_type != 1) continue;
            int p_offset = read32LE(file, phoff + 4);
            int p_vaddr  = read32LE(file, phoff + 8);
            int p_filesz = read32LE(file, phoff + 16);
            int p_memsz  = read32LE(file, phoff + 20);

            if (p_vaddr < 0 || p_vaddr + p_memsz > memSize) {
                host.print("ELF: segmento fora da memória alocada", host.stdout);
                return false;
            }
            if (p_filesz > 0 && p_offset + p_filesz <= file.length) {
                System.arraycopy(file, p_offset, mem, p_vaddr, p_filesz);
            }
            // rest remains zero (BSS)
        }

        // registradores init
        EIP = entryPoint;
        ESP = memSize - 4;
        EBP = ESP;

        host.print("ELF: carregado; entry=0x" + Integer.toHexString(entryPoint) + " memSize=" + memSize, host.stdout);
        return true;
    }

    /**
     * Executa o binário carregado
     */
    public void run() {
        halted = false;
        int steps = 0;
        try {
            while (!halted) {
                host.print("chegou aqui!", host.stdout);
                if (EIP < 0 || EIP >= memSize) {
                    host.print("ELF: EIP fora dos limites: 0x" + Integer.toHexString(EIP), host.stdout);
                    break;
                }
                int opcode = unsigned(mem[EIP]);

                steps++;
                if (steps > 10000000) {
                    host.print("ELF: limite de passos atingido, abortando", host.stdout);
                    break;
                }

                if (opcode == 0x90) { // NOP
                    EIP += 1;
                }
                // MOV r32, imm32: 0xB8 + rd
                else if (opcode >= 0xB8 && opcode <= 0xBF) {
                    int reg = opcode - 0xB8;
                    int imm = read32LE(mem, EIP + 1);
                    setRegByIndex(reg, imm);
                    EIP += 5;
                }
                // MOV r/m32, r32 : 0x89 /r  (mod==3 only)
                else if (opcode == 0x89) {
                    int modrm = unsigned(mem[EIP + 1]);
                    int mod = (modrm >> 6) & 0x3;
                    int reg = (modrm >> 3) & 0x7;
                    int rm  = modrm & 0x7;
                    if (mod == 3) {
                        int val = getRegByIndex(reg);
                        setRegByIndex(rm, val);
                        EIP += 2;
                    } else {
                        host.print("ELF: 0x89 with mod!=3 não suportado", host.stdout);
                        halted = true;
                    }
                }
                // MOV r32, r/m32 : 0x8B /r (mod==3 only)
                else if (opcode == 0x8B) {
                    int modrm = unsigned(mem[EIP + 1]);
                    int mod = (modrm >> 6) & 0x3;
                    int reg = (modrm >> 3) & 0x7;
                    int rm  = modrm & 0x7;
                    if (mod == 3) {
                        int val = getRegByIndex(rm);
                        setRegByIndex(reg, val);
                        EIP += 2;
                    } else {
                        host.print("ELF: 0x8B with mod!=3 não suportado", host.stdout);
                        halted = true;
                    }
                }
                // PUSH r32 : 0x50 + rd
                else if (opcode >= 0x50 && opcode <= 0x57) {
                    int reg = opcode - 0x50;
                    push32(getRegByIndex(reg));
                    EIP += 1;
                }
                // POP r32 : 0x58 + rd
                else if (opcode >= 0x58 && opcode <= 0x5F) {
                    int reg = opcode - 0x58;
                    int val = pop32();
                    setRegByIndex(reg, val);
                    EIP += 1;
                }
                // CALL rel32
                else if (opcode == 0xE8) {
                    int rel = read32LE(mem, EIP + 1);
                    int returnAddr = EIP + 5;
                    push32(returnAddr);
                    EIP = returnAddr + rel;
                }
                // JMP rel32
                else if (opcode == 0xE9) {
                    int rel = read32LE(mem, EIP + 1);
                    EIP = EIP + 5 + rel;
                }
                // INT imm8
                else if (opcode == 0xCD) {
                    int imm8 = unsigned(mem[EIP + 1]);
                    EIP += 2;
                    if (imm8 == 0x80) {
                        handleSyscall();
                    } else {
                        host.print("ELF: int 0x" + Integer.toHexString(imm8) + " não suportado", host.stdout);
                        halted = true;
                    }
                }
                // RET
                else if (opcode == 0xC3) {
                    int addr = pop32();
                    EIP = addr;
                }
                else {
                    host.print("ELF: opcode 0x" + Integer.toHexString(opcode) + " não implementado. Aborting.", host.stdout);
                    halted = true;
                }
            }
        } catch (Exception e) {
            host.print("ELF: exceção durante execução: " + e.getClass().getName() + " " + e.getMessage(), host.stdout);
        }
        host.print("ELF: execução finalizada", host.stdout);
    }

    // -----------------------
    // Syscall handling (simples)
    // -----------------------
    private void handleSyscall() {
        int nr = EAX;
        if (nr == 1) { // exit
            halted = true;
            host.print("ELF: sys_exit(" + EBX + ")", host.stdout);
            return;
        }
        if (nr == 3) { // read(fd, buf, count)
            int fd_r = EBX;
            int buf = ECX;
            int count = EDX;
            if (fd_r == 0) {
                try {
                    String in = host.stdin.getString();
                    if (in == null) in = "";
                    byte[] b = in.getBytes(); // platform encoding
                    int n = b.length;
                    if (n > count) n = count;
                    if (buf >= 0 && buf + n <= memSize) {
                        for (int i = 0; i < n; i++) mem[buf + i] = b[i];
                        EAX = n;
                    } else {
                        EAX = -1;
                    }
                } catch (Exception e) {
                    EAX = -1;
                }
            } else {
                EAX = -1;
            }
            return;
        }
        if (nr == 4) { // write(fd, buf, count)
            int fd = EBX;
            int bptr = ECX;
            int len = EDX;
            if (len < 0) { EAX = -1; return; }
            if (bptr < 0 || bptr + len > memSize) { EAX = -1; return; }
            byte[] out = new byte[len];
            for (int i = 0; i < len; i++) out[i] = mem[bptr + i];
            try {
                if (fd == 1 || fd == 2) {
                    // convert to String using platform default
                    String s = new String(out, 0, len);
                    host.write("/dev/stdout", s.getBytes(), 0);
                    EAX = len;
                } else {
                    EAX = -1;
                }
            } catch (Exception e) {
                EAX = -1;
            }
            return;
        }
        if (nr == 5) { // open stub
            int fnamePtr = EBX;
            String fname = readCStringFromMem(fnamePtr);
            if (fname == null) { EAX = -1; return; }
            EAX = -1;
            return;
        }
        if (nr == 6) { // close stub
            EAX = 0;
            return;
        }
        // default
        host.print("ELF: syscall não suportada: " + nr, host.stdout);
        EAX = -1;
    }

    // -----------------------
    // Stack helpers
    // -----------------------
    private void push32(int v) {
        ESP = ESP - 4;
        if (ESP < 0) { host.print("ELF: stack overflow", host.stdout); halted = true; return; }
        write32LE(mem, ESP, v);
    }

    private int pop32() {
        if (ESP + 4 > memSize) { host.print("ELF: stack underflow", host.stdout); halted = true; return 0; }
        int v = read32LE(mem, ESP);
        ESP = ESP + 4;
        return v;
    }

    // -----------------------
    // Reg helpers
    // -----------------------
    private int getRegByIndex(int reg) {
        if (reg == REG_EAX) return EAX;
        if (reg == REG_ECX) return ECX;
        if (reg == REG_EDX) return EDX;
        if (reg == REG_EBX) return EBX;
        if (reg == REG_ESP) return ESP;
        if (reg == REG_EBP) return EBP;
        if (reg == REG_ESI) return ESI;
        if (reg == REG_EDI) return EDI;
        return 0;
    }

    private int setRegByIndex(int reg, int value) {
        if (reg == REG_EAX) { EAX = value; return EAX; }
        if (reg == REG_ECX) { ECX = value; return ECX; }
        if (reg == REG_EDX) { EDX = value; return EDX; }
        if (reg == REG_EBX) { EBX = value; return EBX; }
        if (reg == REG_ESP) { ESP = value; return ESP; }
        if (reg == REG_EBP) { EBP = value; return EBP; }
        if (reg == REG_ESI) { ESI = value; return ESI; }
        if (reg == REG_EDI) { EDI = value; return EDI; }
        return 0;
    }

    private String readCStringFromMem(int addr) {
        if (addr < 0 || addr >= memSize) return null;
        int i = addr;
        // find zero byte or end
        int max = memSize;
        int len = 0;
        while (i < max && mem[i] != 0) { i++; len++; }
        if (len == 0) return "";
        try {
            return new String(mem, addr, len);
        } catch (Exception e) {
            return null;
        }
    }

    // -----------------------
    // Byte/LE helpers
    // -----------------------
    private static int unsigned(byte b) { return b & 0xFF; }

    private static int read16LE(byte[] a, int off) {
        int b0 = a[off] & 0xFF;
        int b1 = a[off + 1] & 0xFF;
        return (b0 | (b1 << 8));
    }

    private static int read32LE(byte[] a, int off) {
        int b0 = a[off] & 0xFF;
        int b1 = a[off + 1] & 0xFF;
        int b2 = a[off + 2] & 0xFF;
        int b3 = a[off + 3] & 0xFF;
        return (b0 | (b1 << 8) | (b2 << 16) | (b3 << 24));
    }

    private static void write32LE(byte[] a, int off, int v) {
        a[off] = (byte)(v & 0xFF);
        a[off + 1] = (byte)((v >> 8) & 0xFF);
        a[off + 2] = (byte)((v >> 16) & 0xFF);
        a[off + 3] = (byte)((v >> 24) & 0xFF);
    }

    private static int alignUp(int v, int align) {
        int rem = v % align;
        if (rem == 0) return v;
        return v + (align - rem);
    }

    // Debug / snapshot
    public byte[] getMemorySnapshot(int start, int len) {
        if (start < 0) start = 0;
        if (start + len > memSize) len = memSize - start;
        byte[] out = new byte[len];
        System.arraycopy(mem, start, out, 0, len);
        return out;
    }
}
