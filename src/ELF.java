import javax.microedition.io.*;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import java.util.*;
import java.io.*;

public class ELF {
    private Object stdout;
    private OpenTTY midlet;

    private static final int EI_NIDENT = 16;
    private static final int ELFCLASS32 = 1;
    private static final int ELFDATA2LSB = 1;
    private static final int EM_ARM = 40;
    private static final int ET_EXEC = 2;
    private static final int PT_LOAD = 1;
    private static final int PT_DYNAMIC = 2;
    private static final int PT_INTERP = 3;
    private static final int PT_NOTE = 4;

    public ELF(OpenTTY midlet, Hashtable args, Object stdout, Hashtable scope, int id, String pid, Hashtable proc) { this.midlet = midlet; this.stdout = stdout; }
    
    public String getPid() { return ""; }
    public void kill() {  }
    
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
        
        int e_type = readShortLE(elfData, 16), e_machine = readShortLE(elfData, 18), e_entry = readIntLE(elfData, 24), e_phoff = readIntLE(elfData, 28), e_shoff = readIntLE(elfData, 32), e_phnum = readShortLE(elfData, 44), e_shnum = readShortLE(elfData, 48), e_phentsize = readShortLE(elfData, 42), e_shentsize = readShortLE(elfData, 46);
        
        if (e_type != ET_EXEC) { midlet.print("Not an executable ELF", stdout); return false; }
        if (e_machine != EM_ARM) { midlet.print("Not an ARM executable", stdout); return false; }
        
        return true;
    }

    public Hashtable run() {
        Hashtable ITEM = new Hashtable();

        midlet.print("ELF not supported!", stdout, 0);

        ITEM.put("status", new Double(69));
        return ITEM;
    }

    private int readIntLE(byte[] data, int offset) { if (offset + 3 >= data.length || offset < 0) { return 0; } return ((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8) | ((data[offset + 2] & 0xFF) << 16) | ((data[offset + 3] & 0xFF) << 24)); } 
    private short readShortLE(byte[] data, int offset) { if (offset + 1 >= data.length || offset < 0) { return 0; } return (short)((data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8)); }
}
