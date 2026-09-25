import javax.microedition.lcdui.Displayable;
import java.util.Hashtable;
import java.util.Vector;

public class Process {
    public OpenTTY midlet;
    public String cmd = "", exec, user;
    public int id, uid = 0, priority = 0, exitStatus = 0;
    public String pid = "", parentPid = "";
    public boolean exited = false;
    public Object stdout;
    public Hashtable scope = new Hashtable(), net = new Hashtable();
    public Displayable screen;
    public String name = "";
    public Hashtable db = new Hashtable();
    public Lua lua = null;
    public Object handler = null;
    public String elf = "";

    public Process(OpenTTY midlet, String cmd, String exec, String user, int id, String pid, Object stdout, Hashtable scope) {
        this.midlet = midlet; this.cmd = cmd; this.exec = exec; this.user = user; this.id = id; this.pid = pid; this.stdout = stdout; this.scope = scope;
    }
    public Process(OpenTTY midlet, String cmd, String exec, String user, int id, String pid, Object stdout, Hashtable args, Hashtable scope) {
        this(midlet, cmd, exec, user, id, pid, stdout, scope);
        if (args != null && args.equals(java.util.Collections.EMPTY_MAP) == false) { }
    }
    public ELF getELF() { return null; }
    public void start(long l) { }
    public void requestGC() { }
}