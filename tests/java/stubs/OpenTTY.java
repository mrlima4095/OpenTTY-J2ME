import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Display;
import java.io.InputStream;

public class OpenTTY {
    public static final boolean ELF_LITE = false;
    public String username = "root", build = "1.18.2";
    public int memory_size = 512;
    public boolean debug = false;
    public int lastID = 1000;
    public Display display = new Display();
    public Hashtable attributes = new Hashtable(), fs = new Hashtable(), sys = new Hashtable(), exited = new Hashtable(), tmp = new Hashtable(), graphics = new Hashtable(), globals = new Hashtable();
    public Object shell;

    public Hashtable cloneScope(Hashtable scope) { return scope; }
    public String genpid() { return "999"; }
    public String getUser(int uid) { return "root"; }
    public int getCallerUid(Hashtable scope) { return 0; }
    public void print(String message, Object stdout, int id, Hashtable scope, boolean newLine) { }
    public void print(String message, Object stdout, int id, Hashtable scope) { }
    public void print(String message, Object stdout) { }
    public InputStream getInputStream(String filename, Hashtable scope) throws Exception { return null; }
    public int write(String filename, String data, int id, Hashtable scope) { return 0; }
    public int write(String filename, byte[] data, int id, Hashtable scope) { return 0; }
    public int deleteFile(String filename, int id, Hashtable scope) { return 0; }
    public boolean isPureText(byte[] data) { return false; }
    public int vfsDirIndex(String dir) { return -1; }
    public Vector listVfsFiles(String dir) { return new Vector(); }
    public String[] procFiles() { return new String[0]; }
    public Vector procEntries(int uid) { return new Vector(); }
    public Vector procDirEntries(String pidStr, int uid) { return new Vector(); }
    public void showTaskManager() { }
    public void destroyApp(boolean unconditional) { }

    public String joinpath(String file, Hashtable scope) {
        String pwd = scope.containsKey("PWD") ? (String) scope.get("PWD") : "/";
        if (file.startsWith("/")) { return file; }
        String fullPath = pwd + file;
        Vector components = new Vector();
        String[] parts = split(fullPath, '/');
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals(".")) { continue; }
            else if (part.equals("..")) {
                if (components.size() > 0) { if (!components.lastElement().equals("")) { components.removeElementAt(components.size() - 1); } }
            } else { components.addElement(part); }
        }
        if (components.size() == 0) { return "/"; }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < components.size(); i++) {
            String comp = (String) components.elementAt(i);
            if (i == 0 && comp.equals("")) { result.append("/"); }
            else if (i > 0 || !comp.equals("")) {
                result.append(comp);
                if (i < components.size() - 1) { result.append("/"); }
            }
        }
        if (fullPath.endsWith("/") && !result.toString().endsWith("/")) { result.append("/"); }
        return result.toString();
    }

    public String[] split(String content, char div) {
        Vector v = new Vector();
        StringBuffer cur = new StringBuffer();
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == div) { v.addElement(cur.toString()); cur = new StringBuffer(); }
            else { cur.append(content.charAt(i)); }
        }
        v.addElement(cur.toString());
        String[] r = new String[v.size()];
        for (int i = 0; i < v.size(); i++) { r[i] = (String) v.elementAt(i); }
        return r;
    }
}