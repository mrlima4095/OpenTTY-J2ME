package javax.microedition.rms;
public interface RecordEnumeration {
    int numRecords();
    byte[] nextRecord();
    int nextRecordId();
    byte[] previousRecord();
    int previousRecordId();
    boolean hasNextElement();
    boolean hasPreviousElement();
    void reset();
    void destroy();
    int nextRecordIndex();
}
