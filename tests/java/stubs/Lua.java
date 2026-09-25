import java.util.Hashtable;
import java.util.Vector;

public class Lua {
    public static final int EXEC = 1;
    public OpenTTY midlet;
    public int id;
    public String pid;
    public Process proc;
    public Object stdout;
    public Hashtable scope;
    public boolean kill = false;

    public Lua(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; this.id = id; this.pid = pid; this.proc = proc; this.stdout = stdout; this.scope = scope;
    }
    public Lua(OpenTTY midlet, Hashtable father) { this.midlet = midlet; this.scope = father; }
    public Hashtable run(String path, String code, Hashtable args) { return new Hashtable(); }
    public Hashtable exec(String command, Hashtable args) { return new Hashtable(); }
    public Hashtable exec(Vector args) { return new Hashtable(); }

    public class LuaFunction {
        public String name = "";
        public int code;
        public Lua owner;
        public LuaFunction(int code) { this.code = code; }
        public Object call(Vector args, Hashtable scope) { return null; }
        public Object call(Vector args) { return null; }
    }
}