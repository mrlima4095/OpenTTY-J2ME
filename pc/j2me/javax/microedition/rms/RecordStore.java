package javax.microedition.rms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;

public class RecordStore {
    private final String name;
    private final File dir;
    private boolean open = true;

    private RecordStore(String name, File dir) { this.name = name; this.dir = dir; }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
            throws RecordStoreException {
        if (recordStoreName == null) { throw new IllegalArgumentException("null store name"); }
        File dir = new File(baseDir(), sanitize(recordStoreName));
        if (!dir.exists()) {
            if (!createIfNecessary) { throw new RecordStoreNotFoundException(recordStoreName); }
            if (!dir.mkdirs()) { throw new RecordStoreException("cannot create store dir"); }
        }
        return new RecordStore(recordStoreName, dir);
    }

    public static RecordStore openRecordStore(String storeName, boolean createIfNecessary,
            int authmode, boolean writable) throws RecordStoreException {
        return openRecordStore(storeName, createIfNecessary);
    }

    public static RecordStore openRecordStore(String storeName, String vendorName,
            String suiteName) throws RecordStoreException {
        return openRecordStore(storeName, true);
    }

    public static void deleteRecordStore(String recordStoreName) throws RecordStoreException {
        File dir = new File(baseDir(), sanitize(recordStoreName));
        if (!dir.exists()) { throw new RecordStoreNotFoundException(recordStoreName); }
        File[] kids = dir.listFiles();
        if (kids != null) { for (int i = 0; i < kids.length; i++) { kids[i].delete(); } }
        if (!dir.delete()) { throw new RecordStoreException("cannot delete store"); }
    }

    public static String[] listRecordStores() {
        File base = new File(baseDir());
        if (!base.exists()) { return null; }
        File[] dirs = base.listFiles();
        Vector v = new Vector();
        if (dirs != null) {
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].isDirectory() && !dirs[i].getName().startsWith(".")) { v.addElement(dirs[i].getName()); }
            }
        }
        if (v.size() == 0) { return null; }
        String[] out = new String[v.size()];
        v.copyInto(out);
        return out;
    }

    private static String baseDir() {
        String r = System.getProperty("opentty.rms", "data/rms");
        File d = new File(r);
        if (!d.exists()) { d.mkdirs(); }
        return r;
    }

    private static String sanitize(String s) {
        String out = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out += (Character.isLetterOrDigit(c) || c == '-' || c == '_') ? c : '_';
        }
        return out.length() == 0 ? "store" : out;
    }

    private File fileFor(int recordId) { return new File(dir, "rec-" + recordId); }

    public String getName() { return name; }

    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreNotOpenException, RecordStoreException {
        ensureOpen();
        int id = nextId();
        writeFile(fileFor(id), data, offset, numBytes);
        return id;
    }

    public void setRecord(int recordId, byte[] data, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException {
        ensureOpen();
        File f = fileFor(recordId);
        if (!f.exists()) { throw new InvalidRecordIDException("record " + recordId); }
        writeFile(f, data, offset, numBytes);
    }

    public byte[] getRecord(int recordId) throws RecordStoreNotOpenException, InvalidRecordIDException {
        ensureOpen();
        File f = fileFor(recordId);
        if (!f.exists()) { throw new InvalidRecordIDException("record " + recordId); }
        return readFile(f);
    }

    public void deleteRecord(int recordId) throws RecordStoreNotOpenException, RecordStoreException {
        ensureOpen();
        File f = fileFor(recordId);
        if (!f.exists()) { throw new InvalidRecordIDException("record " + recordId); }
        f.delete();
    }

    public int getNumRecords() throws RecordStoreNotOpenException {
        ensureOpen();
        File[] kids = dir.listFiles();
        int n = 0;
        if (kids != null) {
            for (int i = 0; i < kids.length; i++) {
                if (kids[i].isFile() && kids[i].getName().startsWith("rec-")) { n++; }
            }
        }
        return n;
    }

    public int getNextRecordID() throws RecordStoreNotOpenException {
        ensureOpen();
        return nextId();
    }

    public long getSize() throws RecordStoreNotOpenException { ensureOpen(); return getNumRecords() * 8; }
    public long getSizeAvailable() throws RecordStoreNotOpenException { return 500000; }
    public int getVersion() throws RecordStoreNotOpenException { ensureOpen(); return 1; }
    public long getLastModified() throws RecordStoreNotOpenException { ensureOpen(); return dir.lastModified(); }
    public int getRecordSize(int recordId) throws RecordStoreNotOpenException, InvalidRecordIDException {
        ensureOpen();
        File f = fileFor(recordId);
        if (!f.exists()) { throw new InvalidRecordIDException("record " + recordId); }
        return (int) f.length();
    }

    private int nextId() {
        int id = 1;
        while (fileFor(id).exists()) { id++; }
        return id;
    }

    private void writeFile(File f, byte[] data, int offset, int numBytes) throws RecordStoreException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(f);
            fos.write(data, offset, numBytes);
            fos.flush();
        } catch (IOException e) {
            throw new RecordStoreException("write failed: " + e.getMessage());
        } finally {
            if (fos != null) { try { fos.close(); } catch (IOException e) { } }
        }
    }

    private byte[] readFile(File f) throws InvalidRecordIDException {
        FileInputStream fis = null;
        try {
            byte[] out = new byte[(int) f.length()];
            fis = new FileInputStream(f);
            int off = 0, n;
            while (off < out.length && (n = fis.read(out, off, out.length - off)) != -1) { off += n; }
            return out;
        } catch (Exception e) {
            throw new InvalidRecordIDException("read failed: " + e.getMessage());
        } finally {
            if (fis != null) { try { fis.close(); } catch (IOException e) { } }
        }
    }

    private void ensureOpen() throws RecordStoreNotOpenException {
        if (!open) { throw new RecordStoreNotOpenException(name); }
    }

    public void closeRecordStore() throws RecordStoreNotOpenException {
        ensureOpen();
        open = false;
    }

    public synchronized void addRecordListener(Object listener) { }
    public synchronized void removeRecordListener(Object listener) { }
}