import java.util.Hashtable;

/**
 * Runs rv/abitest: exercises the extended guest ABI in res/lib (strstr,
 * strrchr, strcasecmp, strtol/strtoul, qsort/bsearch, the coalescing heap and
 * the LCDUI widget layer) on the real src/ELF.java emulator.
 * The binary is built from tests/java/abitest.c by tests/java/build_rv.sh.
 */
public class TestLibc {
    public static void main(String[] args) throws Exception {
        String repo = System.getProperty("opentty.repo", ".");
        String path = repo + "/tests/java/rv/abitest";
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
        argv.put(new Double(0), "abitest");
        mid.sys.put("901", new java.util.Vector());
        TestELF.OUT.setLength(0);
        ELF run = new ELF(mid, argv, out, scope, 1000, "901", null);
        if (!run.load(elf)) { System.out.println("FAIL load"); System.exit(1); return; }
        run.run();

        String got = TestELF.OUT.toString();
        System.out.println(got);

        int ok = 0, bad = 0;
        String[] lines = got.split("\n");
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("ok ")) { ok++; }
            else if (lines[i].startsWith("FAIL ")) { bad++; System.out.println("  " + lines[i]); }
        }

        boolean suspended = got.endsWith("waiting for an event\n");
        if (bad == 0 && ok > 50 && got.indexOf("abitest done, 0 failure(s)") >= 0 && suspended) {
            System.out.println("PASS abitest (" + ok + " checks)");
        } else {
            System.out.println("FAIL abitest: " + ok + " ok, " + bad + " failed, suspended=" + suspended);
            System.exit(1);
        }
    }
}
