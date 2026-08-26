package javax.microedition.media.control;
public interface RecordControl extends Control {
    void setRecordLocation(String path) throws javax.microedition.media.MediaException;
    void startRecord() throws javax.microedition.media.MediaException;
    void stopRecord() throws javax.microedition.media.MediaException;
    void reset() throws javax.microedition.media.MediaException;
    byte[] getRecordedData();
    void setMaxSize(long size) throws javax.microedition.media.MediaException;
    long getRecordSize();
}
