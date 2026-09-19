package javax.microedition.io.file;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.microedition.io.Connector;

/** FileConnection backed by a real filesystem sandbox. */
public class FileConnectionImpl implements FileConnection {
    public File root;
    public File target;
    public int mode;
    private boolean open = true;
    public String url;

    public FileConnectionImpl(String path, int mode) {
        this.mode = mode;
        this.url = "file://" + normalize(path);
        this.root = sandboxRoot();
        this.target = resolve(path);
    }

    private static String normalize(String path) {
        if (path == null) { return "/"; }
        String p = path;
        while (p.startsWith("/")) { p = p.substring(1); }
        return p;
    }

    private File resolve(String path) {
        return new File(root, normalize(path));
    }

    static File sandboxRoot() {
        String prop = System.getProperty("opentty.fileRoot");
        String rootPath = (prop != null && prop.length() > 0)
            ? prop
            : System.getProperty("user.home") + File.separator + ".opentty" + File.separator + "fs";
        File d = new File(rootPath);
        if (!d.exists()) { d.mkdirs(); }
        return d;
    }

    public void open() throws IOException { }

    public void close() throws IOException {
        open = false;
    }

    public boolean isOpen() { return open; }

    public InputStream openInputStream() throws IOException {
        checkReadable();
        return new FileInputStream(target);
    }

    public OutputStream openOutputStream() throws IOException {
        checkWritable();
        File p = target.getParentFile();
        if (p != null) { p.mkdirs(); }
        return new FileOutputStream(target);
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    public void create() throws IOException {
        checkWritable();
        File p = target.getParentFile();
        if (p != null) { p.mkdirs(); }
        if (!target.exists()) {
            if (!target.createNewFile()) { throw new IOException("Cannot create " + target); }
        }
    }

    public void mkdir() throws IOException {
        checkWritable();
        if (!target.exists()) {
            if (!target.mkdirs()) { throw new IOException("Cannot mkdir " + target); }
        } else if (!target.isDirectory()) {
            throw new IOException("Not a directory: " + target);
        }
    }

    public boolean exists() { return target.exists(); }

    public boolean isDirectory() { return target.isDirectory(); }

    public boolean isFile() { return target.isFile(); }

    public long fileSize() { return target.isFile() ? target.length() : -1; }

    public void delete() throws IOException {
        checkWritable();
        if (target.exists() && !target.delete()) { throw new IOException("Cannot delete " + target); }
    }

    public void rename(String newName) throws IOException {
        checkWritable();
        if (newName == null) { throw new NullPointerException(); }
        File nf = new File(target.getParentFile(), newName);
        if (!target.renameTo(nf)) { throw new IOException("Cannot rename"); }
        target = nf;
    }

    public void truncate(long byteOffset) throws IOException {
        checkWritable();
        java.io.RandomAccessFile raf = new java.io.RandomAccessFile(target, "rw");
        try {
            if (byteOffset < raf.length()) { raf.setLength(byteOffset); }
        } finally { raf.close(); }
    }

    public long lastModified() { return target.lastModified(); }

    public long setLastModified(long time) throws IOException {
        checkWritable();
        return target.setLastModified(time) ? time : 0;
    }

    public String getPath() { return target.getParent() == null ? "/" : target.getParent(); }
    public String getName() { return target.getName(); }
    public String getURL() { return url; }

    public Enumeration list() throws IOException {
        return list(null, false);
    }

    public Enumeration list(String filter, boolean includeHidden) throws IOException {
        checkReadable();
        if (!target.isDirectory()) { throw new IOException("Not a directory"); }
        String[] kids = target.list();
        final Vector names = new Vector();
        if (kids != null) {
            for (int i = 0; i < kids.length; i++) {
                if (!includeHidden && looksHidden(kids[i])) { continue; }
                if (filter != null && filter.length() > 0) {
                    if (filter.indexOf(';') != -1) {
                        boolean ok = false;
                        java.util.StringTokenizer st = new java.util.StringTokenizer(filter, ";");
                        while (st.hasMoreTokens()) {
                            String tok = st.nextToken();
                            if (simpleMatch(tok, kids[i])) { ok = true; break; }
                        }
                        if (!ok) { continue; }
                    } else if (!simpleMatch(filter, kids[i])) { continue; }
                }
                File f = new File(target, kids[i]);
                String name = f.isDirectory() ? kids[i] + "/" : kids[i];
                names.addElement(name);
            }
        }
        java.util.Collections.sort(names);
        return new Enumeration() {
            int idx = 0;
            public boolean hasMoreElements() { return idx < names.size(); }
            public Object nextElement() {
                if (idx >= names.size()) { throw new NoSuchElementException(); }
                return names.elementAt(idx++);
            }
        };
    }

    private static boolean looksHidden(String name) {
        return name.startsWith(".");
    }

    private static boolean simpleMatch(String wild, String name) {
        if (wild.indexOf('*') == -1 && wild.indexOf('?') == -1) { return wild.equals(name); }
        return name.toLowerCase().matches(wildToRegex(wild.toLowerCase()));
    }

    private static String wildToRegex(String wild) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < wild.length(); i++) {
            char c = wild.charAt(i);
            if (c == '*') { sb.append(".*"); }
            else if (c == '?') { sb.append(".?"); }
            else { sb.append(java.util.regex.Pattern.quote(String.valueOf(c))); }
        }
        return sb.toString();
    }

    public boolean isHidden() { return root != null && looksHidden(target.getName()); }
    public void setHidden(boolean hidden) throws IOException { }
    public boolean isReadable() { return target.canRead(); }
    public boolean isWritable() { return target.canWrite(); }
    public long availableSize() { return totalSize(); }
    public long totalSize() { return target.getFreeSpace(); }
    public long usedSize() { return target.getTotalSpace() - target.getFreeSpace(); }

    public void setFileConnection(String fileName) throws IOException {
        if (fileName == null || fileName.length() > 0) {
            target = resolve(getPath() + "/" + (fileName == null ? "" : fileName));
        }
    }

    private void checkReadable() throws IOException {
        if (!open) { throw new IOException("Connection closed"); }
        if ((mode & Connector.READ) == 0) { throw new java.io.IOException("Connection not readable"); }
    }

    private void checkWritable() throws IOException {
        if (!open) { throw new IOException("Connection closed"); }
        if ((mode & Connector.WRITE) == 0) { throw new java.io.IOException("Connection not writable"); }
    }
}