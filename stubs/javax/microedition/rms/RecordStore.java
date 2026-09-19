package javax.microedition.rms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Persistent RecordStore backed by a per-store file in the RMS base dir.
 *
 * Custom on-disk format:
 *   magic "OPTTY1ENC" (8 bytes)
 *   int32 nextId
 *   int32 numRecords
 *   for each record: int32 id, int32 len, len bytes
 */
public class RecordStore {
    public static final int AUTHMODE_PRIVATE = 0;
    public static final int AUTHMODE_ANY = 1;

    private static final String MAGIC = "OPTTY1ENC";
    private static final Hashtable openStores = new Hashtable();   // name -> RecordStore
    private static final Hashtable refCount = new Hashtable();     // name -> Integer

    private final String name;
    private final File file;
    private final java.util.TreeMap records = new java.util.TreeMap();   // Integer(id) -> byte[]
    private int nextId = 1;
    private boolean open = true;
    private int openCount = 0;

    private RecordStore(String name, File file) {
        this.name = name;
        this.file = file;
    }

    private static File baseDir() {
        String prop = System.getProperty("opentty.rms");
        String dir = (prop != null && prop.length() > 0)
            ? prop
            : System.getProperty("user.home") + File.separator + ".opentty" + File.separator + "rms";
        File d = new File(dir);
        if (!d.exists()) { d.mkdirs(); }
        return d;
    }

    private static File storeFile(String name) {
        String safe = name.replace('/', '_').replace('\\', '_').replace("..", "__");
        return new File(baseDir(), safe + ".rms");
    }

    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
            throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
        if (recordStoreName == null) { throw new IllegalArgumentException("Null store name"); }
        if (recordStoreName.length() > 32) { throw new IllegalArgumentException("Record store name too long"); }
        File f = storeFile(recordStoreName);

        RecordStore existing = (RecordStore) openStores.get(recordStoreName);
        if (existing != null) {
            Integer rc = (Integer) refCount.get(recordStoreName);
            refCount.put(recordStoreName, new Integer(rc.intValue() + 1));
            existing.openCount++;
            existing.open = true;
            return existing;
        }

        RecordStore rs = new RecordStore(recordStoreName, f);
        if (f.exists()) {
            try {
                rs.load();
            } catch (Exception e) {
                throw new RecordStoreException("Corrupt record store '" + recordStoreName + "'");
            }
        } else if (createIfNecessary) {
            try {
                rs.save();
            } catch (IOException e) {
                throw new RecordStoreException("Cannot create record store: " + e.getMessage());
            }
        } else {
            throw new RecordStoreNotFoundException("Record store not found: " + recordStoreName);
        }
        openStores.put(recordStoreName, rs);
        refCount.put(recordStoreName, new Integer(1));
        rs.openCount = 1;
        return rs;
    }

    public static void deleteRecordStore(String recordStoreName)
            throws RecordStoreException, RecordStoreNotFoundException {
        if (recordStoreName == null) { throw new IllegalArgumentException("Null store name"); }
        if (openStores.containsKey(recordStoreName)) {
            throw new RecordStoreException("Record store still open: " + recordStoreName);
        }
        File f = storeFile(recordStoreName);
        if (!f.exists()) { throw new RecordStoreNotFoundException("Record store not found: " + recordStoreName); }
        if (!f.delete()) { throw new RecordStoreException("Cannot delete store: " + recordStoreName); }
    }

    public static String[] listRecordStores() {
        File[] files = baseDir().listFiles(new java.io.FilenameFilter() {
            public boolean accept(File dir, String n) { return n.endsWith(".rms"); }
        });
        if (files == null || files.length == 0) { return null; }
        String[] out = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            String n = files[i].getName();
            out[i] = n.substring(0, n.length() - 4);
        }
        java.util.Arrays.sort(out);
        return out;
    }

    private void load() throws Exception {
        DataInputStream in = new DataInputStream(new java.io.BufferedInputStream(new FileInputStream(file)));
        try {
            byte[] magic = new byte[8];
            in.readFully(magic);
            if (!new String(magic, "ISO-8859-1").equals(MAGIC)) {
                // Legacy format: each record stored as raw bytes indexed by
                // position; try to at least keep the store usable.
                throw new java.io.IOException("Unknown magic");
            }
            nextId = in.readInt();
            int n = in.readInt();
            for (int i = 0; i < n; i++) {
                int id = in.readInt();
                int len = in.readInt();
                byte[] data = new byte[len];
                in.readFully(data);
                records.put(new Integer(id), data);
                if (id >= nextId) { nextId = id + 1; }
            }
        } finally {
            try { in.close(); } catch (IOException e) { }
        }
    }

    private void save() throws IOException {
        DataOutputStream out = new DataOutputStream(new java.io.BufferedOutputStream(new FileOutputStream(file)));
        try {
            out.write(MAGIC.getBytes("ISO-8859-1"));
            out.writeInt(nextId);
            out.writeInt(records.size());
            for (java.util.Iterator it = records.values().iterator(); it.hasNext();) {
                // values() won't give ids; re-iterate the map
                break;
            }
            for (java.util.Iterator it = records.keySet().iterator(); it.hasNext();) {
                Integer id = (Integer) it.next();
                byte[] data = (byte[]) records.get(id);
                out.writeInt(id.intValue());
                out.writeInt(data.length);
                out.write(data);
            }
            out.flush();
        } finally {
            try { out.close(); } catch (IOException e) { }
        }
    }

    private void checkOpen() throws RecordStoreNotOpenException {
        if (!open) { throw new RecordStoreNotOpenException("Record store closed: " + name); }
    }

    public synchronized void closeRecordStore() throws RecordStoreNotOpenException {
        checkOpen();
        Integer rc = (Integer) refCount.get(name);
        int refs = rc == null ? 0 : rc.intValue();
        openCount--;
        if (openCount <= 0) {
            try { save(); } catch (IOException e) { }
            open = false;
            openStores.remove(name);
            refCount.remove(name);
        } else {
            refCount.put(name, new Integer(refs - 1));
        }
    }

    public synchronized int getNumRecords() throws RecordStoreNotOpenException {
        checkOpen();
        return records.size();
    }

    public synchronized int getNextRecordID() throws RecordStoreNotOpenException {
        checkOpen();
        return nextId;
    }

    public synchronized int getVersion() throws RecordStoreNotOpenException {
        checkOpen();
        return 0;
    }

    public synchronized int getSizeAvailable() throws RecordStoreNotOpenException {
        checkOpen();
        return Integer.MAX_VALUE;
    }

    public synchronized int getRecordSize(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        checkOpen();
        byte[] d = (byte[]) records.get(new Integer(recordId));
        return d == null ? 0 : d.length;
    }

    public synchronized byte[] getRecord(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        checkOpen();
        if (recordId < 1) { throw new InvalidRecordIDException("Bad record id: " + recordId); }
        return (byte[]) records.get(new Integer(recordId));
    }

    public synchronized int addRecord(byte[] data, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
        checkOpen();
        byte[] tmp = (data == null) ? new byte[0]
            : java.util.Arrays.copyOfRange(data, offset, offset + numBytes);
        int id = nextId++;
        records.put(new Integer(id), tmp);
        try { save(); } catch (Exception e) { records.remove(new Integer(id)); throw new RecordStoreFullException("Failed to write store"); }
        return id;
    }

    public synchronized void setRecord(int recordId, byte[] newData, int offset, int numBytes)
            throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
        checkOpen();
        if (recordId < 1) { throw new InvalidRecordIDException("Bad record id: " + recordId); }
        byte[] tmp = (newData == null) ? new byte[0]
            : java.util.Arrays.copyOfRange(newData, offset, offset + numBytes);
        if (!records.containsKey(new Integer(recordId))) {
            if (recordId >= nextId) { nextId = recordId + 1; }
            records.put(new Integer(recordId), tmp);
        } else {
            records.put(new Integer(recordId), tmp);
        }
        try { save(); } catch (Exception e) { throw new RecordStoreFullException("Failed to write store"); }
    }

    public synchronized void deleteRecord(int recordId)
            throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
        checkOpen();
        records.remove(new Integer(recordId));
        try { save(); } catch (Exception e) { throw new RecordStoreException("Failed to write store"); }
    }

    public synchronized byte[] getCategoryRecord() throws RecordStoreNotOpenException {
        checkOpen();
        return null;
    }

    public synchronized void setCategoryRecord(byte[] data, int offset, int len) throws RecordStoreNotOpenException {
        checkOpen();
    }

    public synchronized void setMode(int authmode, boolean writable) { }
    public String getName() { return name; }
}