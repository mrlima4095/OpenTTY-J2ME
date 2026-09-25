package javax.microedition.io.file;
import java.util.Enumeration;
public interface FileConnection extends javax.microedition.io.Connection {
    boolean exists();
    boolean isDirectory();
    Enumeration list();
    void delete();
    void mkdir();
    java.io.InputStream openInputStream() throws java.io.IOException;
    java.io.OutputStream openOutputStream() throws java.io.IOException;
    long fileSize();
}
