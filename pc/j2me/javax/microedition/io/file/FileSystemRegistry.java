package javax.microedition.io.file;

import java.util.Enumeration;
import java.util.Vector;

public final class FileSystemRegistry {
    static final String[] ROOTS;
    static {
        String mnt = System.getProperty("opentty.mnt", "data/mnt");
        java.io.File base = new java.io.File(mnt);
        if (!base.exists()) { base.mkdirs(); }
        java.io.File[] dirs = base.listFiles();
        Vector v = new Vector();
        if (dirs != null) {
            for (int i = 0; i < dirs.length; i++) {
                if (dirs[i].isDirectory()) { v.addElement(dirs[i].getName()); }
            }
        }
        v.addElement("mnt");
        ROOTS = new String[v.size()];
        v.copyInto(ROOTS);
    }

    public static boolean isRootLevel(String s) {
        for (int i = 0; i < ROOTS.length; i++) { if (ROOTS[i].equals(s)) { return true; } }
        return false;
    }

    public static Enumeration listRoots() {
        Vector v = new Vector();
        for (int i = 0; i < ROOTS.length; i++) { v.addElement(ROOTS[i]); }
        return v.elements();
    }

    public static java.io.File baseDir() {
        java.io.File base = new java.io.File(System.getProperty("opentty.mnt", "data/mnt"));
        if (!base.exists()) { base.mkdirs(); }
        return base;
    }

    public static java.io.File resolve(String rel) {
        java.io.File base = baseDir();
        try {
            java.io.File abs = new java.io.File(base, rel);
            String basePath = base.getCanonicalPath();
            String fPath = abs.getCanonicalPath();
            if (!fPath.equals(basePath) && !fPath.startsWith(basePath + java.io.File.separator)) {
                return base;
            }
            return abs;
        } catch (Exception e) {
            return new java.io.File(base, sanitize(replaceSlash(rel)));
        }
    }

    private static String sanitize(String s) {
        String out = "";
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '/' || c == '\\') { out += java.io.File.separator; }
            else if (c == 0) { out += '_'; }
            else { out += c; }
        }
        return out;
    }
    private static String replaceSlash(String s) {
        String out = "";
        for (int i = 0; i < s.length(); i++) {
            out += s.charAt(i) == '/' ? java.io.File.separator : s.charAt(i);
        }
        return out;
    }
}
