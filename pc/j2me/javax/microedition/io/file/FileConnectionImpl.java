package javax.microedition.io.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Vector;

public class FileConnectionImpl implements FileConnection {
    private final java.net.URL url;
    private final String relPath;
    private final File file;
    private final int mode;
    private boolean closed = false;

    public FileConnectionImpl(String urlSpec) throws IOException {
        if (!urlSpec.startsWith("file://")) { throw new javax.microedition.io.ConnectionNotFoundException("Not a file URL: " + urlSpec); }
        String rest = urlSpec.substring(7);
        while (rest.startsWith("/")) { rest = rest.substring(1); }
        rest = RestUtil.unescape(rest);
        this.relPath = rest;
        this.url = new java.net.URL("file", "", urlSpec);
        this.file = FileSystemRegistry.resolve(rest);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) { parent.mkdirs(); }
        this.mode = javax.microedition.io.Connector.READ_WRITE;
    }

    public FileConnectionImpl(String urlSpec, int mode) throws IOException {
        this(urlSpec);
    }

    public boolean exists() { ensureOpen(); return file.exists(); }
    public boolean isDirectory() { ensureOpen(); return file.isDirectory(); }
    public boolean isHidden() { ensureOpen(); return file.isHidden(); }
    public boolean canRead() { ensureOpen(); return file.canRead(); }
    public boolean canWrite() { ensureOpen(); return file.canWrite(); }
    public String getName() { ensureOpen(); return file.getName(); }
    public String getPath() { ensureOpen(); return file.getParent() == null ? "/" : file.getParent(); }
    public String getURL() { ensureOpen(); return url.toExternalForm(); }
    public boolean isOpen() { return !closed; }
    public long totalSize() { ensureOpen(); return file.getTotalSpace(); }
    public long availableSize() { ensureOpen(); return file.getUsableSpace(); }
    public long usedSize() { ensureOpen(); return file.getTotalSpace() - file.getUsableSpace(); }
    public long fileSize() throws IOException { ensureOpen(); return file.length(); }
    public long lastModified() { ensureOpen(); return file.lastModified(); }
    public void truncate(long byteOffset) throws IOException {
        ensureOpen();
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(file, "rw");
        try { raf.setLength(byteOffset); } finally { raf.close(); }
    }
    public void setReadable(boolean readable) { ensureOpen(); file.setReadable(readable); }
    public void setWritable(boolean writable) { ensureOpen(); file.setWritable(writable); }
    public void setHidden(boolean hidden) { ensureOpen(); }
    public void setReserved(boolean reserved) { ensureOpen(); }
    public void setFileConnection(String fileName) throws IOException { ensureOpen(); }
    public void create() throws IOException {
        ensureOpen();
        if (!file.exists() && !file.createNewFile()) { throw new IOException("cannot create"); }
    }
    public void mkdir() throws IOException {
        ensureOpen();
        if (!file.isDirectory() && !file.mkdirs()) { if (!file.isDirectory()) { throw new IOException("cannot mkdir"); } }
    }
    public void delete() throws IOException {
        ensureOpen();
        if (file.isDirectory()) {
            File[] kids = file.listFiles();
            if (kids != null) { for (int i = 0; i < kids.length; i++) { kids[i].delete(); } }
        }
        if (!file.delete()) { throw new IOException("cannot delete"); }
    }
    public void rename(String newName) throws IOException {
        ensureOpen();
        if (!file.renameTo(new File(file.getParent(), newName))) { throw new IOException("cannot rename"); }
    }
    public Enumeration list() throws IOException {
        ensureOpen();
        Vector v = new Vector();
        if (file.isDirectory()) {
            String[] kids = file.list();
            if (kids != null) { for (int i = 0; i < kids.length; i++) { v.addElement(kids[i]); } }
        }
        return v.elements();
    }

    public InputStream openInputStream() throws IOException {
        ensureOpen();
        if (file.isDirectory()) { throw new IOException("is a directory"); }
        return new FileInputStream(file);
    }
    public DataInputStream openDataInputStream() throws IOException { return new DataInputStream(openInputStream()); }
    public OutputStream openOutputStream() throws IOException {
        ensureOpen();
        if (file.isDirectory()) { throw new IOException("is a directory"); }
        return new FileOutputStream(file);
    }
    public DataOutputStream openDataOutputStream() throws IOException { return new DataOutputStream(openOutputStream()); }

    private void ensureOpen() {
        if (closed) { throw new IllegalStateException("connection closed"); }
    }
    public void close() {
        closed = true;
    }

    static class RestUtil {
        static String unescape(String s) {
            return s.replaceAll("%20", " ").replaceAll("%2F", "/");
        }
    }
}
