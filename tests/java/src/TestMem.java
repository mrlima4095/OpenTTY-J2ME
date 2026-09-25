import java.util.Hashtable;

/**
 * Runs rv/memtest: checks gc()/mem_free/mem_total/mem_used and time() wrappers
 * of the guest libc (res/lib/libc.s -> src/ELF.java handleLibraryCall).
 * The binary is built from memtest.c by tests/java/build_rv.sh.
 */
public class TestMem {
    public static void main(String[] args) throws Exception {
        String repo = System.getProperty("opentty.repo", ".");
        String path = repo + "/tests/java/rv/memtest";
        java.io.FileInputStream f = new java.io.FileInputStream(path);
        byte[] elf = new byte[f.available()];
        f.read(elf);
        f.close();

        TestELF.FILES.clear();
        StringBuilder out = new StringBuilder();
        Hashtable scope = new Hashtable();
        scope.put("USER", "root");
        OpenTTY mid = new TestELF.FakeTTY();
        Hashtable argv = new Hashtable();
        argv.put(new Double(0), "memtest");
        mid.sys.put("900", new java.util.Vector());
        TestELF.OUT.setLength(0);
        ELF run = new ELF(mid, argv, out, scope, 1000, "900", null);
        if (!run.load(elf)) { System.out.println("FAIL load"); System.exit(1); return; }
        run.run();

        String got = TestELF.OUT.toString();
        System.out.println(got);
        if (java.util.regex.Pattern.matches("free=\\d+KB total=\\d+KB used=\\d+KB\\ntime=\\d{6,}\\n", got)) {
            System.out.println("PASS memtest");
        } else {
            System.out.println("FAIL memtest");
            System.exit(1);
        }
    }
}