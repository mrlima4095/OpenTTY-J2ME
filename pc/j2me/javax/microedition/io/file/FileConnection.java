package javax.microedition.io.file;

import javax.microedition.io.Connection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

public interface FileConnection extends Connection {
    long totalSize();
    long availableSize();
    long usedSize();
    void truncate(long byteOffset) throws IOException;
    void setFileConnection(String fileName) throws IOException;
    String getName();
    String getPath();
    String getURL();
    boolean isOpen();
    boolean isHidden();
    boolean canRead();
    boolean canWrite();
    void setReadable(boolean readable) throws IOException;
    void setWritable(boolean writable) throws IOException;
    void setHidden(boolean hidden) throws IOException;
    void setReserved(boolean reserved) throws IOException;
    boolean isDirectory();
    boolean exists();
    void create() throws IOException;
    void mkdir() throws IOException;
    void delete() throws IOException;
    void rename(String newName) throws IOException;
    long fileSize() throws IOException;
    long lastModified();
    Enumeration list() throws IOException;
    InputStream openInputStream() throws IOException;
    DataInputStream openDataInputStream() throws IOException;
    OutputStream openOutputStream() throws IOException;
    DataOutputStream openDataOutputStream() throws IOException;
}
