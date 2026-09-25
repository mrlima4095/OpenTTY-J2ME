import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Hashtable;

/**
 * Runs a RISC-V ELF on the real src/ELF.java emulator with stub runtime.
 *
 * The binaries under test are the shipped emulator apps in res/apps/dist/.
 * Every check compares the emulated stdout against a known-good fixture.
 */
public class TestELF {

    public static StringBuilder OUT = new StringBuilder();
    public static Hashtable FILES = new Hashtable(); // path -> String (VFS contents)
    public static int FAILURES = 0;
    public static int PASSES = 0;

    public static void main(String[] args) throws Exception {
        FILES.put("/etc/os-release", "OpenTTY-1.18.2 LTS\ncodename: beetroot\n");
        FILES.put("/etc/issue", "Welcome to OpenTTY!\n");

        check("rev", new String[] { "rev", "/etc/os-release" }, "STL 2.81.1-YTTnepO\ntoorteeb :emanedoc\n");
        check("tac", new String[] { "tac", "/etc/os-release" }, "codename: beetroot\nOpenTTY-1.18.2 LTS\n");
        check("seq", new String[] { "seq", "10" }, "1 2 3 4 5 6 7 8 9 10\n");
        check("seq", new String[] { "seq", "5", "-1", "1" }, "5 4 3 2 1\n");
        check("seq", new String[] { "seq", "3", "3", "12" }, "3 6 9 12\n");
        check("factor", new String[] { "factor", "12" }, "12 = 2 2 3\n");
        check("factor", new String[] { "factor", "97" }, "97 = 97\n");
        check("factor", new String[] { "factor", "1" }, "1 = 1\n");
        check("cal", new String[] { "cal", "2", "2026" },
            "      February 2026\nSu Mo Tu We Th Fr Sa\n1 2 3 4 5 6 7\n8 9 10 11 12 13 14\n15 16 17 18 19 20 21\n22 23 24 25 26 27 28\n\n");
        check("rot13", new String[] { "rot13", "/etc/issue" }, "Jrypbzr gb BcraGGL!\n");

        System.out.println("TestELF: " + PASSES + " passed, " + FAILURES + " failed");
        if (FAILURES > 0) { System.exit(1); }
    }

    static void check(String name, String[] argv, String expected) throws Exception {
        byte[] elf = load(resolve(name));
        OUT.setLength(0);
        runOne(elf, argv);
        String got = OUT.toString();
        boolean ok = got.equals(expected);
        System.out.println((ok ? "PASS" : "FAIL") + " " + java.util.Arrays.toString(argv));
        if (ok) { PASSES++; } else {
            FAILURES++;
            System.out.println("  expected: " + escape(expected));
            System.out.println("  got:      " + escape(got));
        }
    }

    static String resolve(String name) {
        String repo = System.getProperty("opentty.repo", ".");
        String path = repo + "/res/apps/dist/" + name;
        java.io.File f = new java.io.File(path);
        return f.isFile() ? path : name;
    }

    static String escape(String s) { return s.replace("\n", "\\n").replace(" ", "\u00b7"); }

    static byte[] load(String path) throws Exception {
        FileInputStream f = new FileInputStream(path);
        byte[] data = new byte[f.available()];
        f.read(data);
        f.close();
        return data;
    }

    static void runOne(byte[] elf, String[] argv) throws Exception {
        Hashtable args = new Hashtable();
        for (int i = 0; i < argv.length; i++) { args.put(new Double(i), argv[i]); }

        Hashtable scope = new Hashtable();
        scope.put("USER", "root");

        FakeTTY mid = new FakeTTY();
        mid.sys.put("900", new java.util.Vector());
        ELF elfRun = new ELF(mid, args, OUT, scope, 1000, "900", null);
        if (!elfRun.load(elf)) { OUT.append("LOAD_FAILED"); return; }
        elfRun.run();
    }

    static class FakeTTY extends OpenTTY {
        FakeTTY() { attributes = new Hashtable(); }
        public void print(String m, Object stdout, int id, Hashtable scope, boolean nl) {
            TestELF.OUT.append(m);
            if (nl) { TestELF.OUT.append('\n'); }
        }
        public void print(String m, Object stdout, int id, Hashtable scope) { TestELF.OUT.append(m).append('\n'); }
        public void print(String m, Object stdout) { TestELF.OUT.append(m).append('\n'); }
        public String getUser(int uid) { return "root"; }
        public String genpid() { return "900"; }
        public String joinpath(String file, Hashtable scope) { return file; }
        public Hashtable cloneScope(Hashtable scope) { return scope; }
        public InputStream getInputStream(String path, Hashtable scope) {
            Object v = FILES.get(path);
            return v == null ? null : new ByteArrayInputStream(((String) v).getBytes());
        }
        public int write(String path, String data, int id, Hashtable scope) { return 0; }
    }
}