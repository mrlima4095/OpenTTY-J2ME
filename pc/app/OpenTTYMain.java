import javax.microedition.midlet.MIDlet;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Item;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;
import javax.microedition.lcdui.StringItem;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.swing.SwingUtilities;

public class OpenTTYMain {
    private static boolean flag(String key) {
        String v = System.getProperty(key, "");
        return v.equals("1") || v.equalsIgnoreCase("true");
    }

    /** Parse runner arguments (mirror of pc/run.sh) and translate them into
     *  JVM properties, staging host files into data/mnt/opentty/. */
    private static void parseArgs(String[] args) throws Exception {
        String mntBase = System.getProperty("opentty.mnt", "data/mnt");
        java.io.File opentty = new java.io.File(mntBase, "opentty");
        opentty.mkdirs();
        String command = "";
        java.util.ArrayList<String> tokens = new java.util.ArrayList<String>();
        boolean sep = false;
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (sep) { tokens.add(a); }
            else if (a.equals("-h") || a.equals("--help")) {
                System.out.println("Usage: OpenTTYMain [options] [--] [command tokens...]");
                System.out.println("  root=PATH  init=PATH  --user NAME  --smoke  --watchdog  --cmd CMD  --");
                System.out.println("Host files in the command are staged to /mnt/opentty/ and run by content.");
                System.exit(0);
            }
            else if (a.startsWith("root=")) { System.setProperty("opentty.bootRoot", a.substring(5)); }
            else if (a.startsWith("init=")) { System.setProperty("opentty.bootInit", a.substring(5)); }
            else if (a.equals("--user")) {
                if (i + 1 >= args.length) { System.out.println("--user requires a name"); System.exit(2); }
                System.setProperty("opentty.user", args[++i]);
            }
            else if (a.startsWith("--user=")) { System.setProperty("opentty.user", a.substring(7)); }
            else if (a.equals("--smoke")) { System.setProperty("opentty.smoke", "1"); }
            else if (a.equals("--watchdog")) { System.setProperty("opentty.watchdog", "1"); }
            else if (a.equals("--repro")) { System.setProperty("opentty.repro", "1"); }
            else if (a.equals("--cmd")) {
                if (i + 1 >= args.length) { System.out.println("--cmd requires a command"); System.exit(2); }
                command = args[++i];
            }
            else if (a.equals("--")) { sep = true; }
            else { tokens.add(a); sep = true; }
        }
        if (command.length() == 0) {
            if (!tokens.isEmpty()) {
                java.util.ArrayList<String> out = new java.util.ArrayList<String>();
                boolean firstStaged = false;
                for (int i = 0; i < tokens.size(); i++) {
                    String tok = tokens.get(i);
                    String staged = stage(mntBase, opentty, tok);
                    if (staged != null) {
                        if (i == 0) { firstStaged = true; }
                        out.add(staged);
                    } else {
                        out.add(tok);
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < out.size(); i++) {
                    if (i > 0) { sb.append(' '); }
                    sb.append(out.get(i));
                }
                command = sb.toString();
                if (firstStaged) { command = ". " + command; }
            }
        }
        if (command.length() > 0) { System.setProperty("opentty.cmd", command); }

        String bi = System.getProperty("opentty.bootInit", "");
        if (bi.length() > 0 && !bi.equals("/bin/init") && !bi.startsWith("/mnt/")) {
            String stagedInit = stage(mntBase, opentty, bi);
            if (stagedInit != null) { System.setProperty("opentty.bootInit", stagedInit); }
        }
        String br = System.getProperty("opentty.bootRoot", "");
        if (br.length() > 0 && !br.equals("/") && !br.startsWith("/mnt/")) {
            java.io.File rd = new java.io.File(br);
            if (rd.isDirectory()) {
                java.io.File dst = new java.io.File(new java.io.File(mntBase), rd.getName());
                try {
                    java.nio.file.Files.walk(rd.toPath()).forEach(p -> {
                        try {
                            if (java.nio.file.Files.isDirectory(p)) {
                                new java.io.File(dst, rd.toPath().relativize(p).toString()).mkdirs();
                            } else {
                                java.nio.file.Files.copy(p, new java.io.File(dst, rd.toPath().relativize(p).toString()).toPath(),
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (Exception e) { }
                    });
                    System.setProperty("opentty.bootRoot", "/mnt/" + rd.getName());
                } catch (Exception e) {
                    System.out.println("[stage] root= copy failed: " + e);
                }
            }
        }
    }

    /** Copy a host file into data/mnt/opentty/ and return the guest path
     *  (/mnt/opentty/<name>), or null when the token is not a host file. */
    private static String stage(String mntBase, java.io.File opentty, String hostPath) {
        java.io.File f = new java.io.File(hostPath);
        if (!f.isFile()) { return null; }
        java.io.File dst = new java.io.File(opentty, f.getName());
        try {
            byte[] src = java.nio.file.Files.readAllBytes(f.toPath());
            if (!dst.exists() || dst.length() != src.length) {
                java.nio.file.Files.write(dst.toPath(), src);
            }
        } catch (Exception e) {
            System.out.println("[stage] failed to stage " + hostPath + ": " + e);
            return null;
        }
        return "/mnt/opentty/" + f.getName();
    }

    public static void main(String[] args) throws Exception {
        String root = System.getProperty("opentty.rms", "data/rms");
        String user = System.getProperty("opentty.user", "opentty");
        System.out.println("[OpenTTY] desktop run: user=" + user + " rms=" + root);

        // Same command line as pc/run.sh, so `java -jar OpenTTY-desktop-*.jar
        // [options] [--] [command tokens...]` works out of the box. When a
        // caller already supplied the props (run.sh), leave them alone.
        if (System.getProperty("opentty.cmd") == null) {
            parseArgs(args);
        }

        seedOpenRMS(user);

        final MIDlet midlet = new OpenTTY();
        System.out.println("[stage] new OpenTTY() ok"); System.out.flush();

        if (flag("opentty.watchdog")) {
            System.out.println("[stage] watchdog branch"); System.out.flush();
            // Reproduce the interactive path (startApp on the EDT) while main
            // watches; if no screen appears, dump every thread's stack and quit.
            final Throwable[] err = new Throwable[1];
            final String br = System.getProperty("opentty.bootRoot", "");
            final String bi = System.getProperty("opentty.bootInit", "");
            Thread launcher = new Thread(new Runnable() {
                public void run() {
                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                try { boot(midlet, br, bi); }
                                catch (Throwable e) { err[0] = e; }
                            }
                        });
                    } catch (Throwable e) { err[0] = e; }
                }
            }, "launcher");
            launcher.start();
            System.out.println("[stage] launcher started"); System.out.flush();
            long t0 = System.currentTimeMillis();
            int nullTicks = 0;
            for (;;) {
                try { Thread.sleep(3000); } catch (InterruptedException e) { }
                long el = System.currentTimeMillis() - t0;
                System.out.println("[watchdog] t=" + (el / 1000) + "s current=" + describe(Display.staticCurrent()));
                if (err[0] != null) {
                    System.out.println("[watchdog] startApp threw: " + err[0]);
                    err[0].printStackTrace(System.out);
                    System.exit(3);
                }
                if (Display.staticCurrent() != null) {
                    if (++nullTicks >= 2) { System.out.println("[watchdog] boot OK"); System.exit(0); }
                    continue;
                }
                nullTicks = 0;
                if (el > 15000) {
                    System.out.println("------------------------------------------------");
                    java.util.Map stacks = Thread.getAllStackTraces();
                    java.util.Iterator it = stacks.entrySet().iterator();
                    while (it.hasNext()) {
                        java.util.Map.Entry en = (java.util.Map.Entry) it.next();
                        Thread th = (Thread) en.getKey();
                        StackTraceElement[] els = (StackTraceElement[]) en.getValue();
                        System.out.println("== " + th.getName() + " " + th.getState() + " daemon=" + th.isDaemon());
                        for (int i = 0; i < els.length; i++) { System.out.println("    " + els[i]); }
                    }
                    System.exit(9);
                }
            }
        }

        if (flag("opentty.smoke")) {
            // Non-interactive boot check: run startApp on a worker thread (as
            // MIDP would) and, after a settle delay, dump the current screen
            // and the PID-1 output buffer, then exit.
            final String br = System.getProperty("opentty.bootRoot", "");
            final String bi = System.getProperty("opentty.bootInit", "");
            Thread midletThread = new Thread(new Runnable() {
                public void run() {
                    try { boot(midlet, br, bi); }
                    catch (Throwable e) { e.printStackTrace(); System.exit(1); }
                }
            }, "MIDlet");
            midletThread.start();

            try { Thread.sleep(Long.getLong("opentty.smokeWait", 10000).longValue()); }
            catch (InterruptedException e) { }

            dumpState(midlet);

            // Type into the xterm stdin field and fire the Run handler, which
            // exercises CommandListener -> Lua handler -> os.execute.
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() { fireCommand(System.getProperty("opentty.cmd", "echo hello opentty desktop"), "smoke"); }
                });
                Thread.sleep(4000);
            } catch (Throwable e) {
                System.out.println("[smoke] command dispatch failed: " + e);
            }
            dumpState(midlet);
            System.exit(0);
        }

        String br = System.getProperty("opentty.bootRoot", "");
        String bi = System.getProperty("opentty.bootInit", "");
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try { boot(midlet, br, bi); }
                catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });

        fireBootCommand(System.getProperty("opentty.cmd", ""));

        if (flag("opentty.repro")) { reproDiagnose(midlet); }

        if (System.getProperty("opentty.cmd") != null) {
            try { Thread.sleep(1500); } catch (InterruptedException e) { }
            dumpState(midlet);
        }

        if (System.getProperty("opentty.probe") != null) {
            javax.swing.Timer t = new javax.swing.Timer(2000, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("[probe] display = " + describe(Display.staticCurrent()));
                    Object cur = Display.staticCurrent();
                    try {
                        if (cur instanceof javax.microedition.lcdui.Form) {
                            java.awt.Frame[] fs = java.awt.Frame.getFrames();
                            for (int i = 0; i < fs.length; i++) {
                                java.awt.Frame f = fs[i];
                                System.out.println("[probe] frame=" + f.getName() + " size=" + f.getWidth() + "x" + f.getHeight()
                                    + " visible=" + f.isVisible() + " showing=" + f.isShowing()
                                    + " valid=" + f.isValid());
                                java.awt.Container c = ((javax.swing.JFrame) f).getContentPane();
System.out.println("[probe]   content " + c.getWidth() + "x" + c.getHeight()
                                    + " valid=" + c.isValid());
                            }
                        }
                    } catch (Throwable t2) { System.out.println("[probe] geom error: " + t2); }
                }
            });
            t.start();
        }
    }

    /** Diagnostic mode (opentty.repro=1): run statements inside the xterm's
     *  Lua instance and report os.execute/io.popen results via its stdout. */
    private static void reproDiagnose(final MIDlet midlet) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        java.lang.reflect.Field sf = OpenTTY.class.getDeclaredField("sys");
                        sf.setAccessible(true);
                        java.util.Hashtable sys = (java.util.Hashtable) sf.get(midlet);
                        Object xlua = null;
                        java.util.Enumeration en = sys.keys();
                        while (en.hasMoreElements() && xlua == null) {
                            Object p = sys.get(en.nextElement());
                            Object nm = null;
                            try {
                                java.lang.reflect.Field nf = p.getClass().getDeclaredField("name");
                                nf.setAccessible(true);
                                nm = nf.get(p);
                            } catch (Throwable t) { }
                            if (nm != null && nm.toString().equals("xterm")) {
                                java.lang.reflect.Field lf = p.getClass().getDeclaredField("lua");
                                lf.setAccessible(true);
                                xlua = lf.get(p);
                            }
                        }
                        if (xlua == null) { System.out.println("[repro] no xterm lua"); return; }
                        try {
                            java.lang.reflect.Field sof = xlua.getClass().getDeclaredField("stdout");
                            sof.setAccessible(true);
                            Object so = sof.get(xlua);
                            System.out.println("[repro] xterm lua.stdout=" + System.identityHashCode(so) + " " + so.getClass().getName());
                            Object cur = Display.staticCurrent();
                            if (cur instanceof Form && ((Form) cur).size() > 0) {
                                Object it0 = ((Form) cur).get(0);
                                System.out.println("[repro] form item[0]=" + System.identityHashCode(it0) + " " + it0.getClass().getName());
                            }
                        } catch (Throwable t) { System.out.println("[repro] compare failed: " + t); }
                        java.lang.reflect.Method runMethod = null;
                        try {
                            runMethod = xlua.getClass().getMethod("run", String.class, String.class, java.util.Hashtable.class);
                            System.out.println("[repro] run method=" + runMethod);
                        } catch (Throwable te) { System.out.println("[repro] getMethod failed: " + te); }
                        String cmd = System.getProperty("opentty.cmd", "");
                        String code = "_REPRO = 'HELLO'\n"
                            + "local ok0, r0 = pcall(io.popen, '/mnt/opentty/nosuch.lua')\n"
                            + "_REPRO = _REPRO .. '|P0=' .. tostring(ok0) .. '|' .. tostring(r0) .. ';'\n"
                            + "local ok1, r1 = pcall(io.popen, '/mnt/opentty/zt.lua')\n"
                            + "_REPRO = _REPRO .. '|P1=' .. tostring(ok1) .. '|' .. tostring(r1) .. ';'\n"
                            + "local okc, rc = pcall(os.execute, 'cat /mnt/opentty/nosuch.lua')\n"
                            + "_REPRO = _REPRO .. '|CAT=' .. tostring(okc) .. '|' .. tostring(rc) .. ';'\n"
                            + "local ok2, m2 = pcall(os.execute, '" + cmd.replace("'", "\\'") + "')\n"
                            + "_REPRO = _REPRO .. '|EXEC=' .. tostring(ok2) .. '|' .. tostring(m2) .. ';'\n";
                        java.util.Hashtable dr = (java.util.Hashtable) runMethod
                            .invoke(xlua, "repro", code, new java.util.Hashtable());
                        java.lang.reflect.Field gif = xlua.getClass().getDeclaredField("globals");
                        gif.setAccessible(true);
                        java.util.Hashtable g = (java.util.Hashtable) gif.get(xlua);
                        System.out.println("[repro] _REPRO=[" + g.get("_REPRO") + "]");
                        java.lang.reflect.Field sf2 = OpenTTY.class.getDeclaredField("sys");
                        sf2.setAccessible(true);
                        java.util.Hashtable sys2 = (java.util.Hashtable) sf2.get(midlet);
                        String pids = "";
                        java.util.Enumeration en2 = sys2.keys();
                        while (en2.hasMoreElements()) { Object k = en2.nextElement(); pids += k + " "; }
                        System.out.println("[repro] sys keys after: " + pids);
                    } catch (Throwable t) {
                        System.out.println("[repro] failed: " + t);
                        t.printStackTrace(System.out);
                    }
                }
            });
            System.out.println("------------------------------------------------");
            dumpState(midlet);
            System.exit(0);
        } catch (Throwable t) {
            System.out.println("[repro] outer failed: " + t);
            t.printStackTrace(System.out);
        }
    }

    /** Boot the MIDlet. With default root/init this is a plain startApp();
     *  otherwise it drives the boot like a custom grub entry. */
    private static void boot(MIDlet midlet, String bootRoot, String bootInit) throws Throwable {
        if (bootRoot.length() == 0 && bootInit.length() == 0) {
            midlet.startApp();
            return;
        }
        if (!(midlet instanceof OpenTTY)) { throw new IllegalStateException("runner requires the OpenTTY MIDlet"); }
        java.util.Hashtable entry = new java.util.Hashtable();
        entry.put("title", "(desktop runner)");
        entry.put("root", bootRoot.length() > 0 ? bootRoot : "/");
        entry.put("init", bootInit.length() > 0 ? bootInit : "/bin/init");
        ((OpenTTY) midlet).bootEntry(entry);
    }

    /** After boot, wait for a console Form with a Run command and fire the
     *  runner command through its Run handler (os.execute). */
    private static void fireBootCommand(String cmd) {
        if (cmd == null || cmd.length() == 0) { return; }
        long t0 = System.currentTimeMillis();
        while (System.currentTimeMillis() - t0 < 20000) {
            if (hasRunCommand(Display.staticCurrent())) {
                try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() { fireCommand(cmd, "run"); }
                    });
                } catch (Throwable e) {
                    System.out.println("[run] dispatch failed: " + e);
                }
                return;
            }
            try { Thread.sleep(500); } catch (InterruptedException e) { }
        }
        System.out.println("[run] no runnable console appeared; command not fired: " + cmd);
    }

    private static boolean hasRunCommand(Object cur) {
        if (!(cur instanceof Form)) { return false; }
        try {
            java.lang.reflect.Field f = Displayable.class.getDeclaredField("commands");
            f.setAccessible(true);
            java.util.Vector cmds = (java.util.Vector) f.get(cur);
            for (int i = 0; i < cmds.size(); i++) {
                javax.microedition.lcdui.Command c = (javax.microedition.lcdui.Command) cmds.elementAt(i);
                if (c.getLabel().equals("Run")) { return true; }
            }
        } catch (Throwable e) { }
        return false;
    }

    private static void fireCommand(String command, String tag) {
        Object cur = Display.staticCurrent();
        if (!(cur instanceof Form)) { System.out.println("[" + tag + "] no Form to drive: " + cur); return; }
        Form form = (Form) cur;
        try {
            // stdin is the field whose label ends with the prompt "$"/"#".
            TextField stdin = null;
            for (int i = 0; i < form.size(); i++) {
                Item it = form.get(i);
                if (it instanceof TextField) {
                    String l = it.getLabel();
                    if (l != null && (l.endsWith("$") || l.endsWith("#"))) { stdin = (TextField) it; }
                }
            }
            if (stdin == null) { System.out.println("[" + tag + "] stdin field not found"); return; }
            stdin.setString(command);

            java.lang.reflect.Field f = Displayable.class.getDeclaredField("commands");
            f.setAccessible(true);
            java.util.Vector cmds = (java.util.Vector) f.get(form);

            Object listener = null;
            java.lang.reflect.Field lf = Displayable.class.getDeclaredField("listener");
            lf.setAccessible(true);
            listener = lf.get(form);

            javax.microedition.lcdui.Command run = null;
            for (int i = 0; i < cmds.size(); i++) {
                javax.microedition.lcdui.Command c = (javax.microedition.lcdui.Command) cmds.elementAt(i);
                if (c.getLabel().equals("Run")) { run = c; }
            }
            if (run == null || listener == null) {
                System.out.println("[" + tag + "] run command/listener not found (cmds=" + cmds.size() + ")");
                return;
            }
            System.out.println("[" + tag + "] firing Run: '" + command + "'");
            ((javax.microedition.lcdui.CommandListener) listener).commandAction(run, form);
        } catch (Throwable e) {
            System.out.println("[" + tag + "] driving failed: " + e);
            e.printStackTrace();
        }
    }

    private static void dumpProcesses(String tag, MIDlet midlet, java.util.Hashtable table, String title) {
        System.out.println("[" + tag + "] " + title + " .size()=" + table.size());
        java.util.Enumeration en = table.keys();
        while (en.hasMoreElements()) {
            Object pid = en.nextElement();
            Object p = table.get(pid);
            String name = "";
            String out = "";
            try {
                java.lang.reflect.Field nf = p.getClass().getDeclaredField("name");
                nf.setAccessible(true);
                Object n = nf.get(p);
                if (n != null) { name = n.toString(); }
            } catch (Throwable e) { }
            try {
                java.lang.reflect.Field of = p.getClass().getDeclaredField("stdout");
                of.setAccessible(true);
                Object o = of.get(p);
                if (o instanceof StringBuffer) { out = ((StringBuffer) o).toString(); }
                else if (o instanceof String) { out = (String) o; }
                else if (o instanceof java.io.ByteArrayOutputStream) { out = ((java.io.ByteArrayOutputStream) o).toString("UTF-8"); }
            } catch (Throwable e) { }
            System.out.println("[" + tag + "]   pid=" + pid + " " + p.getClass().getSimpleName() + " name=" + name);
            if (out.length() > 0) {
                System.out.println("[" + tag + "]     stdout=" + out.replace("\n", "\\n"));
            }
        }
    }

    private static void dumpState(MIDlet midlet) {
        System.out.println("[smoke] current display = " + describe(Display.staticCurrent()));
        try {
            java.lang.reflect.Field sf = OpenTTY.class.getDeclaredField("sys");
            sf.setAccessible(true);
            dumpProcesses("smoke", midlet, (java.util.Hashtable) sf.get(midlet), "sys");
        } catch (Throwable e) {
            System.out.println("[smoke] sys dump failed: " + e);
        }
        try {
            java.lang.reflect.Field ef = OpenTTY.class.getDeclaredField("exited");
            ef.setAccessible(true);
            dumpProcesses("smoke", midlet, (java.util.Hashtable) ef.get(midlet), "exited");
        } catch (Throwable e) {
            System.out.println("[smoke] exited dump failed: " + e);
        }
        try {
            RecordStore rs = RecordStore.openRecordStore("OpenRMS", false);
            System.out.println("[smoke] OpenRMS records=" + rs.getNumRecords()
                + " user=" + new String(rs.getRecord(1))
                + " hash=" + new String(rs.getRecord(2)));
            rs.closeRecordStore();
        } catch (Exception e) {
            System.out.println("[smoke] OpenRMS read failed: " + e);
        }
    }

    private static String describe(Displayable d) {
        if (d == null) { return "null"; }
        StringBuffer sb = new StringBuffer();
        sb.append(d.getClass().getSimpleName()).append(" [").append(d.getTitle()).append("]");
        if (d instanceof Form) {
            Form f = (Form) d;
            sb.append(" items=").append(f.size());
            for (int i = 0; i < f.size() && i < 8; i++) {
                Item it = f.get(i);
                String txt = it instanceof TextField ? ((TextField) it).getString()
                    : it instanceof StringItem ? ((StringItem) it).getText() : "";
                String extra = "";
                if (it instanceof StringItem && ((StringItem) it).getText() != null) {
                    extra = ((StringItem) it).getText();
                }
                sb.append("\n  [" + i + "] " + it.getClass().getSimpleName()
                    + " label=" + it.getLabel() + " text=" + txt
                    + (extra.length() > 200 ? extra.substring(0, 200) : extra));
            }
        } else if (d instanceof List) {
            List l = (List) d;
            sb.append(" items=").append(l.size());
        } else if (d instanceof TextBox) {
            sb.append(" text=").append(((TextBox) d).getString());
        }
        return sb.toString();
    }

    /** OpenRMS record 1 = username, 2 = password hash, 3 = VFS index. Seeding
     *  them makes the first boot land directly on the console. */
    private static void seedOpenRMS(String user) {
        RecordStore rs = null;
        try {
            rs = RecordStore.openRecordStore("OpenRMS", true);
            if (rs.getNumRecords() < 1) {
                byte[] u = user.getBytes();
                rs.addRecord(u, 0, u.length);
            }
            if (rs.getNumRecords() < 2) {
                byte[] h = String.valueOf(user.hashCode()).getBytes();
                rs.addRecord(h, 0, h.length);
            }
            if (rs.getNumRecords() < 3) {
                byte[] idx = "VFS3\n".getBytes();
                rs.addRecord(idx, 0, idx.length);
            }
            System.out.println("[OpenTTY] OpenRMS seeded for user '" + user + "'");
        } catch (RecordStoreException e) {
            System.err.println("[OpenTTY] could not seed OpenRMS: " + e);
        } finally {
            if (rs != null) {
                try { rs.closeRecordStore(); } catch (RecordStoreException e) { }
            }
        }
    }
}