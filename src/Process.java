import java.util.*;
// |
public class Process {
    private OpenTTY midlet = null;
    public String name, owner, pid, cmd;
    public boolean isAlive = true, isService = false;
    public Hashtable scope, db;
    public final long startTime;
    public int uid, priority = 20;

    public Object stdout, stderr;
    public Object handler = null, sigterm;
    public Lua lua = null;
    public ELF elf = null;
    
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable scope) {
        this.lua = new Lua(midlet, uid, pid, this, stdout, scope);
        this.name = name; this.owner = owner; this.uid = uid; this.pid = pid;
        this.stdout = stdout; this.stderr = stdout; this.scope = scope;
        this.startTime = System.currentTimeMillis();
    }
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable args, Hashtable scope) {
        this.elf = new ELF(midlet, args, stdout, scope, uid, pid, this);
        this.name = name; this.owner = owner; this.uid = uid; this.pid = pid;
        this.stdout = stdout; this.stderr = stdout; this.scope = scope;
        this.startTime = System.currentTimeMillis();
    }


    public String toString() { return "{name=" + name + ",owner=" + owner + ",uid=" + uid, ",pid=" + pid + "," + (lua != null ? "lua=" + lua "," : elf != null ? "elf=" + elf + "," : "") + (handler != null ? "handler=" + handler + "," : "") + "priority=" + priority + ",scope=" + scope + ",db=" + db + "}"; }
}