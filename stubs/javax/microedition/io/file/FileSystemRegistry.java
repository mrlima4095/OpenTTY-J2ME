package javax.microedition.io.file;

import java.io.File;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;

/** FileSystemRegistry: lists the sandbox root as a single mount point. */
public final class FileSystemRegistry {
    private FileSystemRegistry() { }

    public static boolean addFileSystemListener(FileSystemListener listener) { return false; }
    public static boolean removeFileSystemListener(FileSystemListener listener) { return false; }

    public static Enumeration listRoots() {
        final Vector roots = new Vector();
        if (!java.awt.GraphicsEnvironment.isHeadless() || true) {
            File root = FileConnectionImpl.sandboxRoot();
            if (root.isDirectory()) {
                String[] kids = root.list();
                if (kids != null) {
                    for (int i = 0; i < kids.length; i++) {
                        roots.addElement(kids[i] + "/");
                    }
                }
            }
        }
        return new Enumeration() {
            int idx = 0;
            public boolean hasMoreElements() { return idx < roots.size(); }
            public Object nextElement() {
                if (idx >= roots.size()) { throw new NoSuchElementException(); }
                return roots.elementAt(idx++);
            }
        };
    }
}