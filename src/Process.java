import java.util.*;
// |
public class Process {
    private OpenTTY midlet = null;
    public String name, owner, pid;
    public boolean isAlive = true, isService = false;
    public Hashtable scope;
    public long startTime;
    public int uid;

    public Object stdout, stderr;
    public Lua lua;
    public ELF elf;
    
    public Process(OpenTTY midlet, String name, String owner, int uid, String pid, Object stdout, Hashtable scope) {
        this.lua = new Lua(midlet, uid, pid, new Hashtable(), stdout, scope);
        this.name = name; this.owner = owner; this.uid = uid; this.pid = pid;
        this.stdout = stdout; this.stderr = stdout; this.scope = scope;
        this.startTime = System.currentTimeMillis();
    }
    public Process(OpenTTY midlet, String name, int uid, String pid, Object stdout, Hashtable args, Hashtable scope) {
        this.elf = new ELF(midlet, args, stdout, scope, uid, pid, new Hashtable());
        this.name = name; this.owner = owner; this.uid = uid; this.pid = pid;
        this.stdout = stdout; this.stderr = stdout; this.scope = scope;
        this.startTime = System.currentTimeMillis();
    }

    
}