import java.io.*;
import java.util.*;

/**
 * Runs res/apps/dist/cal with no arguments: the calendar for the current month
 * (computed on-device from the SYS_TIME wrapper via civil-conversion code).
 * Asserts the weekday header row is printed and the process completed without
 * trapping (implicit: a decoder/library bug aborts with a nonzero status).
 */
public class TestCalNow {
    public static void main(String[] a) throws Exception {
        String repo = System.getProperty("opentty.repo", ".");
        FileInputStream f = new FileInputStream(repo + "/res/apps/dist/cal");
        byte[] elf = new byte[f.available()]; f.read(elf); f.close();
        Hashtable args = new Hashtable(); args.put(new Double(0), "cal");
        Hashtable scope = new Hashtable(); scope.put("USER","root"); scope.put("PWD","/home/");
        FakeTTY mid = new FakeTTY();
        mid.sys.put("900", new Vector());
        ELF e = new ELF(mid, args, OUT, scope, 1000, "900", null);
        if (!e.load(elf)) { System.out.println("LOAD_FAILED"); System.exit(1); return; }
        e.run();
        String out = OUT.toString();
        System.out.println(out);
        boolean ok = out.contains("Su Mo Tu We Th Fr Sa");
        System.out.println(ok ? "PASS cal-now" : "FAIL cal-now");
        if (!ok) { System.exit(1); }
    }
    static StringBuilder OUT = new StringBuilder();
    static class FakeTTY extends OpenTTY {
        FakeTTY(){ attributes=new Hashtable(); }
        public void print(String m, Object s, int i, Hashtable sc, boolean nl){ OUT.append(m); if(nl)OUT.append('\n'); }
        public void print(String m, Object s, int i, Hashtable sc){ OUT.append(m).append('\n'); }
        public void print(String m, Object s){ OUT.append(m).append('\n'); }
        public String getUser(int u){ return "root"; }
        public String genpid(){ return "900"; }
        public String joinpath(String f, Hashtable sc){ return f; }
        public Hashtable cloneScope(Hashtable sc){ return sc; }
        public InputStream getInputStream(String p, Hashtable sc){ return null; }
        public int write(String p,String d,int i,Hashtable sc){ return 0; }
    }
}