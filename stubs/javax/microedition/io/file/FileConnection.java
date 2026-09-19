package javax.microedition.io.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import javax.microedition.io.Connection;

/** FileConnection (subset of the JSR-75 FileConnection contract). */
public interface FileConnection extends Connection {
    boolean isOpen();
    InputStream openInputStream() throws IOException;
    OutputStream openOutputStream() throws IOException;
    DataInputStream openDataInputStream() throws IOException;
    DataOutputStream openDataOutputStream() throws IOException;
    void create() throws IOException;
    void mkdir() throws IOException;
    boolean exists();
    boolean isDirectory();
    boolean isFile();
    long fileSize();
    void delete() throws IOException;
    void rename(String newName) throws IOException;
    void truncate(long byteOffset) throws IOException;
    long lastModified();
    long setLastModified(long time) throws IOException;
    String getPath();
    String getName();
    String getURL();
    Enumeration list() throws IOException;
    Enumeration list(String filter, boolean includeHidden) throws IOException;
    boolean isHidden();
    void setHidden(boolean hidden) throws IOException;
    boolean isReadable();
    boolean isWritable();
    long availableSize();
    long totalSize();
    long usedSize();
    void setFileConnection(String fileName) throws IOException;
}