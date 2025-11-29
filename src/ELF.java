import java.io.*;
import java.util.*;

/**
 * ELF loader + tiny x86-32 interpreter (extended)
 *
 * Extensões:
 *  - MOV r32, r32 (mod=3 subset of 0x89 / 0x8B)
 *  - PUSH r32 (0x50-0x57), POP r32 (0x58-0x5F)
 *  - CALL rel32 (0xE8), JMP rel32 (0xE9), RET (0xC3)
 *  - stack semantics fixed (ESP cresce pra baixo)
 *
 * Limitações:
 *  - interpreta apenas subset de x86 suficiente para muitos "minimal" C compilados com -nostdlib
 *  - modrm handling apenas para mod==3 (register-direct)
 *  - não lida com relocations, PLT/GOT, dynamic linker, SSE, etc.
 */
public class ELF {
    private OpenTTY host;

    private byte[] mem;
    private int memSize;

    // Registers
    public int EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP;
    public int EIP;

    private int entryPoint = 0;
    private boolean halted = false;

    private static final int REG_EAX = 0, REG_ECX = 1, REG_EDX = 2, REG_EBX = 3, REG_ESP = 4, REG_EBP = 5, REG_ESI = 6, REG_EDI = 7;

    public ELF(OpenTTY host) {
        this.host = host;
    }

    public boolean load(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int r;
        while ((r = is.read(tmp)) > 0) baos.write(tmp, 0, r);
        byte[] file = baos.toByteArray();

        if (file.length < 52) { host.print("ELF: arquivo muito pequeno", host.stdout); return false; }
        if (!(file[0] == 0x7f && file[1] == 'E' && file[2] == 'L' && file[3] == 'F')) {
            host.print("ELF: magic inválido", host.stdout); return false;
        }

        int elfClass = unsigned(file[4]); // 1 = 32-bit
        int data = unsigned(file[5]);     // 1 = little endian
        if (elfClass != 1 || data != 1) {
            host.print("ELF: somente ELF32 little-endian suportado (classe=" + elfClass + " data=" + data + ")", host.stdout);
            return false;
        }

        int e_entry   = read32LE(file, 24);
        int e_phoff   = read32LE(file, 28);
        int e_phentsize = read16LE(file, 42);
        int e_phnum   = read16LE(file, 44);

        this.entryPoint = e_entry;

        int highest = 0;
        for (int i = 0; i < e_phnum; i++) {
            int phoff = e_phoff + i * e_phentsize;
            int p_type = read32LE(file, phoff + 0);
            if (p_type != 1) continue; // PT_LOAD
            int p_offset = read32LE(file, phoff + 4);
            int p_vaddr  = read32LE(file, phoff + 8);
            int p_filesz = read32LE(file, phoff + 16);
            int p_memsz  = read32LE(file, phoff + 20);
            int top = p_vaddr + Math.max(p_memsz, p_filesz);
            if (top > highest) highest = top;
        }

        int stackSize = 1024 * 128; // 128KB stack
        memSize = alignUp(Math.max(highest + 1, 1024 * 1024), 4096) + stackSize;
        mem = new byte[memSize];
        Arrays.fill(mem, (byte)0);

        for (int i = 0; i < e_phnum; i++) {
            int phoff = e_phoff + i * e_phentsize;
            int p_type = read32LE(file, phoff + 0);
            if (p_type != 1) continue; // PT_LOAD only
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
        }

        EIP = entryPoint;
        ESP = memSize - 4;
        EBP = ESP;
        host.print("ELF: carregado; entry=0x" + Integer.toHexString(entryPoint) + " memSize=" + memSize, host.stdout);
        return true;
    }

    public void run() {
        halted = false;
        int steps = 0;
        try {
            while (!halted) {
                if (EIP < 0 || EIP >= memSize) {
                    host.print("ELF: EIP fora dos limites: 0x" + Integer.toHexString(EIP), host.stdout);
                    break;
                }
                int opcode = unsigned(mem[EIP]);

                steps++;
                if (steps > 10_000_000) {
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
                // MOV r/m32, r32 : 0x89 /r  (we only handle mod==3: reg->rm)
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
                // MOV r32, r/m32 : 0x8B /r (we only handle mod==3: rm->reg)
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
        } catch (Throwable t) {
            host.print("ELF: exceção durante execução: " + t.getMessage(), host.stdout);
        }
        host.print("ELF: execução finalizada", host.stdout);
    }

    // Syscall handling (unchanged logic but keep)
    private void handleSyscall() {
        int nr = EAX;
        switch (nr) {
            case 1: // exit
                halted = true;
                host.print("ELF: sys_exit(" + EBX + ")", host.stdout);
                break;

            case 3: // read(fd, buf, count)
                int fd_r = EBX, buf = ECX, count = EDX;
                if (fd_r == 0) {
                    try {
                        String in = host.stdin.getString();
                        byte[] b = in.getBytes("UTF-8");
                        int n = Math.min(b.length, Math.max(0, count));
                        if (buf >= 0 && buf + n <= memSize) {
                            System.arraycopy(b, 0, mem, buf, n);
                            EAX = n;
                        } else {
                            EAX = -1;
                        }
                    } catch (UnsupportedEncodingException e) {
                        EAX = -1;
                    }
                } else {
                    EAX = -1;
                }
                break;

            case 4: // write(fd, buf, count)
                int fd = EBX;
                int bptr = ECX, len = EDX;
                if (len < 0) { EAX = -1; return; }
                if (bptr < 0 || bptr + len > memSize) { EAX = -1; return; }
                byte[] out = new byte[len];
                System.arraycopy(mem, bptr, out, 0, len);
                try {
                    if (fd == 1 || fd == 2) {
                        String s = new String(out, "UTF-8");
                        host.write("/dev/stdout", s.getBytes("UTF-8"), 0);
                        EAX = len;
                    } else {
                        EAX = -1;
                    }
                } catch (UnsupportedEncodingException e) {
                    EAX = -1;
                }
                break;

            case 5: // open (stub)
                int fnamePtr = EBX;
                String fname = readCStringFromMem(fnamePtr);
                if (fname == null) { EAX = -1; break; }
                EAX = -1;
                break;

            case 6: // close stub
                EAX = 0;
                break;

            default:
                host.print("ELF: syscall não suportada: " + nr, host.stdout);
                EAX = -1;
                break;
        }
    }

    // Helpers: stack, memory access, regs
    private void push32(int v) {
        ESP -= 4;
        if (ESP < 0) { host.print("ELF: stack overflow", host.stdout); halted = true; return; }
        write32LE(mem, ESP, v);
    }

    private int pop32() {
        if (ESP + 4 > memSize) { host.print("ELF: stack underflow", host.stdout); halted = true; return 0; }
        int v = read32LE(mem, ESP);
        ESP += 4;
        return v;
    }

    private int getRegByIndex(int reg) {
        switch (reg) {
            case REG_EAX: return EAX;
            case REG_ECX: return ECX;
            case REG_EDX: return EDX;
            case REG_EBX: return EBX;
            case REG_ESP: return ESP;
            case REG_EBP: return EBP;
            case REG_ESI: return ESI;
            case REG_EDI: return EDI;
            default: return 0;
        }
    }

    private int setRegByIndex(int reg, int value) {
        switch (reg) {
            case REG_EAX: EAX = value; return EAX;
            case REG_ECX: ECX = value; return ECX;
            case REG_EDX: EDX = value; return EDX;
            case REG_EBX: EBX = value; return EBX;
            case REG_ESP: ESP = value; return ESP;
            case REG_EBP: EBP = value; return EBP;
            case REG_ESI: ESI = value; return ESI;
            case REG_EDI: EDI = value; return EDI;
            default: return 0;
        }
    }

    private String readCStringFromMem(int addr) {
        if (addr < 0 || addr >= memSize) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = addr; i < memSize; i++) {
            if (mem[i] == 0) break;
            baos.write(mem[i]);
        }
        try { return new String(baos.toByteArray(), "UTF-8"); } catch (UnsupportedEncodingException e) { return null; }
    }

    private static int unsigned(byte b) { return b & 0xFF; }

    private static int read16LE(byte[] a, int off) {
        return (unsigned(a[off]) | (unsigned(a[off + 1]) << 8));
    }

    private static int read32LE(byte[] a, int off) {
        return (unsigned(a[off]) | (unsigned(a[off + 1]) << 8) | (unsigned(a[off + 2]) << 16) | (unsigned(a[off + 3]) << 24));
    }

    private static void write32LE(byte[] a, int off, int v) {
        a[off] = (byte)(v & 0xFF);
        a[off+1] = (byte)((v >> 8) & 0xFF);
        a[off+2] = (byte)((v >> 16) & 0xFF);
        a[off+3] = (byte)((v >> 24) & 0xFF);
    }

    private static int read16LE(byte[] a, int off, int lenGuard) { return read16LE(a, off); }
    private static int read32LE(byte[] a, int off, int lenGuard) { return read32LE(a, off); }

    private static int alignUp(int v, int align) {
        return ((v + align - 1) / align) * align;
    }

    public byte[] getMemorySnapshot(int start, int len) {
        if (start < 0) start = 0;
        if (start + len > memSize) len = Math.max(0, memSize - start);
        byte[] out = new byte[len];
        System.arraycopy(mem, start, out, 0, len);
        return out;
    }
}
