import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import java.util.*;
import java.io.*;

// ELF Lite - sem emulador ARM32
// Apenas valida o arquivo ELF e informa que o emulador nao esta
// presente nesta build. Usado pelo res/swap_lite.sh.
public class ELF {
    private Object stdout;
    private OpenTTY midlet;
    private Hashtable scope;
    private int id = 1000;

    private static final int ELFCLASS32 = 1;
    private static final int ELFDATA2LSB = 1;
    private static final int EM_ARM = 40;
    private static final int ET_EXEC = 2;

    public ELF(OpenTTY midlet, Hashtable args, Object stdout, Hashtable scope, int id, String pid, Process proc) { this.midlet = midlet; this.stdout = stdout; this.scope = scope; this.id = id; }

    public boolean load(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }
        is.close();
        return load(baos.toByteArray());
    }

    public boolean load(byte[] elfData) throws Exception {
        if (elfData.length < 4 || elfData[0] != 0x7F || elfData[1] != 'E' || elfData[2] != 'L' || elfData[3] != 'F') { midlet.print("ELF invalid: not an ELF file", stdout, id, scope); return false; }
        if (elfData[4] != ELFCLASS32) { midlet.print("ELF invalid: only 32-bit ELF supported", stdout, id, scope); return false; }
        if (elfData[5] != ELFDATA2LSB) { midlet.print("ELF invalid: only little-endian ELF supported", stdout, id, scope); return false; }

        int e_type = readShortLE(elfData, 16), e_machine = readShortLE(elfData, 18);

        if (e_type != ET_EXEC) { midlet.print("ELF invalid: not an executable ELF", stdout, id, scope); return false; }
        if (e_machine != EM_ARM) { midlet.print("ELF invalid: not an ARM executable", stdout, id, scope); return false; }

        midlet.print("ELF valid, but not supported (build without ARM emulator)", stdout, id, scope);
        return true;
    }

    public Hashtable run() {
        Hashtable ITEM = new Hashtable();

        midlet.print("ELF not supported (lite build)", stdout, id, scope);

        ITEM.put("status", new Double(69));
        return ITEM;
    }

    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); }
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
}