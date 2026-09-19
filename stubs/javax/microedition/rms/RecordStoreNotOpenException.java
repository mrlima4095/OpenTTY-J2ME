package javax.microedition.rms;

public class RecordStoreNotOpenException extends RecordStoreException {
    private static final long serialVersionUID = 1L;

    public RecordStoreNotOpenException(String message) { super(message); }
    public RecordStoreNotOpenException() { }
}