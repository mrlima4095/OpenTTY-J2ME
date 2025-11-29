import java.io.*;
import java.util.*;

/**
 * ELF loader + tiny x86-32 interpreter (Proof-of-Concept)
 *
 * - Carrega ELF32 little-endian (apenas PT_LOAD segments)
 * - Emula um conjunto mínimo de instruções x86 (mov r32, imm32; nop; int imm8; ret)
 * - Implementa syscalls básicos via int 0x80:
 *      1 -> exit
 *      3 -> read (very small support)
 *      4 -> write (writes to OpenTTY /dev/stdout)
 *
 * Limitações:
 * - NÃO é um emulador completo de x86. Serve como infraestrutura inicial para testar
 *   execução de pequenos binários muito simples que usam apenas int 0x80 + mov imm32.
 * - Muitos opcodes não são implementados (encerra execução com erro).
 *
 * Integração com OpenTTY:
 * - Recebe uma instância de OpenTTY no construtor para acessar I/O (read, write, read file).
 */
public class ELF {
    // Referência ao host (OpenTTY) para I/O / filesystem / log
    private OpenTTY host;

    // Emulated memory
    private byte[] mem;
    private int memSize;

    // Registers
    public int EAX, EBX, ECX, EDX, ESI, EDI, EBP, ESP;
    public int EIP;

    // Program entry and loaded segments
    private int entryPoint = 0;
    private boolean halted = false;

    // Helper: mapping register index (for mov opcodes 0xB8 + reg)
    private static final int REG_EAX = 0, REG_ECX = 1, REG_EDX = 2, REG_EBX = 3, REG_ESP = 4, REG_EBP = 5, REG_ESI = 6, REG_EDI = 7;

    public ELF(OpenTTY host) {
        this.host = host;
    }

    /**
     * Carrega ELF de um InputStream (por exemplo: host.getInputStream("/bin/foo"))
     */
    public boolean load(InputStream is) throws IOException {
        // Ler todo o arquivo em um buffer
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] tmp = new byte[8192];
        int r;
        while ((r = is.read(tmp)) > 0) baos.write(tmp, 0, r);
        byte[] file = baos.toByteArray();

        if (file.length < 52) { // ELF32 header size minimal
            host.print("ELF: arquivo muito pequeno", host.stdout);
            return false;
        }

        // Verifica magic
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

        // Offsets no header (tudo little-endian)
        int e_entry   = read32LE(file, 24);
        int e_phoff   = read32LE(file, 28);
        int e_phentsize = read16LE(file, 42);
        int e_phnum   = read16LE(file, 44);

        this.entryPoint = e_entry;

        // Primeiro calcula tamanho de memória necessário baseado em segmentos PT_LOAD
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

        // Reserve a memória (adiciona um pouco de stack no topo)
        int stackSize = 1024 * 64; // 64KB de stack por padrão
        memSize = alignUp(Math.max(highest + 1, 1024 * 1024), 4096) + stackSize;
        mem = new byte[memSize];
        for (int i = 0; i < memSize; i++) mem[i] = 0;

        // Carrega segmentos
        for (int i = 0; i < e_phnum; i++) {
            int phoff = e_phoff + i * e_phentsize;
            int p_type = read32LE(file, phoff + 0);
            if (p_type != 1) continue; // PT_LOAD only
            int p_offset = read32LE(file, phoff + 4);
            int p_vaddr  = read32LE(file, phoff + 8);
            int p_filesz = read32LE(file, phoff + 16);
            int p_memsz  = read32LE(file, phoff + 20);

            // bounds check
            if (p_vaddr < 0 || p_vaddr + p_memsz > memSize) {
                host.print("ELF: segmento fora da memória alocada", host.stdout);
                return false;
            }

            // copy file bytes
            if (p_filesz > 0 && p_offset + p_filesz <= file.length) {
                System.arraycopy(file, p_offset, mem, p_vaddr, p_filesz);
            }
            // rest already zero (BSS)
        }

        // Inicializa registradores mínimos:
        EIP = entryPoint;
        // Stack pointer: coloque no topo da memória (alto endereço)
        ESP = memSize - 4;
        EBP = ESP;
        host.print("ELF: carregado; entry=0x" + Integer.toHexString(entryPoint) + " memSize=" + memSize, host.stdout);
        return true;
    }

    /**
     * Executa o binário carregado até halt (sys_exit/RET) ou erro.
     */
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

                // Safety: evita loops infinitos exagerados
                steps++;
                if (steps > 5000000) {
                    host.print("ELF: limite de passos atingido, abortando", host.stdout);
                    break;
                }

                if (opcode == 0x90) { // NOP
                    EIP += 1;
                }
                else if (opcode >= 0xB8 && opcode <= 0xBF) {
                    // MOV r32, imm32  -> opcode 0xB8 + rd
                    int reg = opcode - 0xB8;
                    int imm = read32LE(mem, EIP + 1);
                    setRegByIndex(reg, imm);
                    EIP += 5;
                }
                else if (opcode == 0xCD) { // INT imm8
                    int imm8 = unsigned(mem[EIP + 1]);
                    EIP += 2;
                    if (imm8 == 0x80) {
                        // Syscall: uses EAX = number, EBX, ECX, EDX, ESI, EDI
                        handleSyscall();
                    } else {
                        host.print("ELF: int 0x" + Integer.toHexString(imm8) + " não suportado", host.stdout);
                        halted = true;
                    }
                }
                else if (opcode == 0xC3) { // RET
                    // naive: read 4 bytes at ESP, set EIP = popped (but many binaries don't use ret as entry)
                    if (ESP + 4 <= memSize) {
                        EIP = read32LE(mem, ESP);
                        ESP += 4;
                    } else {
                        halted = true;
                    }
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

    // -----------------------
    // Syscall handling (very small)
    // -----------------------
    private void handleSyscall() {
        int nr = EAX;
        switch (nr) {
            case 1: // sys_exit (linux i386)
                // EBX: exit code
                halted = true;
                host.print("ELF: sys_exit(" + EBX + ")", host.stdout);
                break;

            case 3: // sys_read
                // EBX = fd, ECX = buf, EDX = count
                int fd_r = EBX, buf = ECX, count = EDX;
                if (fd_r == 0) { // stdin
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

            case 4: // sys_write
                // EBX = fd, ECX = buf, EDX = count
                int fd = EBX;
                int bptr = ECX, len = EDX;
                if (len < 0) { EAX = -1; return; }
                if (bptr < 0 || bptr + len > memSize) { EAX = -1; return; }
                byte[] out = new byte[len];
                System.arraycopy(mem, bptr, out, 0, len);
                try {
                    if (fd == 1 || fd == 2) { // stdout or stderr => map to OpenTTY stdout
                        // Convert to UTF-8 string (best-effort)
                        String s = new String(out, "UTF-8");
                        host.write("/dev/stdout", s.getBytes("UTF-8"), 0);
                        EAX = len;
                    } else {
                        // For simplicity, other fds are unsupported
                        EAX = -1;
                    }
                } catch (UnsupportedEncodingException e) {
                    EAX = -1;
                }
                break;

            case 5: // sys_open (very naive)
                // EBX = filename (char*), ECX = flags, EDX = mode
                int fnamePtr = EBX;
                String fname = readCStringFromMem(fnamePtr);
                if (fname == null) { EAX = -1; break; }
                // Map simple opens: if path starts with '/', try host filesystem
                if (fname.startsWith("/")) {
                    // For safety, we just return a fake fd or -1
                    // TODO: implement small fd table
                    EAX = -1; // not implemented
                } else {
                    EAX = -1;
                }
                break;

            case 6: // sys_close (stub)
                EAX = 0;
                break;

            default:
                host.print("ELF: syscall não suportada: " + nr, host.stdout);
                EAX = -1;
                break;
        }
    }

    // -----------------------
    // Helpers
    // -----------------------
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

    private static int alignUp(int v, int align) {
        return ((v + align - 1) / align) * align;
    }

    // Expose memory read for debugging
    public byte[] getMemorySnapshot(int start, int len) {
        if (start < 0) start = 0;
        if (start + len > memSize) len = Math.max(0, memSize - start);
        byte[] out = new byte[len];
        System.arraycopy(mem, start, out, 0, len);
        return out;
    }
}
