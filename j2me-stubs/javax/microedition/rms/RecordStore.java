package javax.microedition.rms;
public class RecordStore {
    public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary) throws RecordStoreException { return null; }
    public static void deleteRecordStore(String recordStoreName) throws RecordStoreException {}
    public static String[] listRecordStores() { return null; }
    public int getNumRecords() throws RecordStoreException { return 0; }
    public byte[] getRecord(int recordId) throws RecordStoreException { return null; }
    public void closeRecordStore() throws RecordStoreException {}
    public int addRecord(byte[] data, int offset, int numBytes) throws RecordStoreException { return 0; }
    public void setRecord(int recordId, byte[] newData, int offset, int numBytes) throws RecordStoreException {}
    public int getNextRecordID() throws RecordStoreException { return 0; }
    public void deleteRecord(int recordId) throws RecordStoreException {}
    public int getSize() throws RecordStoreException { return 0; }
    public int getSizeAvailable() throws RecordStoreException { return 0; }
    public long getLastModified() throws RecordStoreException { return 0; }
    public int getVersion() throws RecordStoreException { return 0; }
    public String getName() throws RecordStoreException { return null; }
}
