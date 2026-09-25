import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
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
    public static void main(String[] args) throws Exception {
        String root = System.getProperty("opentty.rms", "data/rms");
        String user = System.getProperty("opentty.user", "opentty");
        System.out.println("[OpenTTY] desktop run: user=" + user + " rms=" + root);

        seedOpenRMS(user);

        final MIDlet midlet = new OpenTTY();

        if (Boolean.getBoolean("opentty.smoke")) {
            // Non-interactive boot check: run startApp on a worker thread (as
            // MIDP would) and, after a settle delay, dump the current screen
            // and the PID-1 output buffer, then exit.
            Thread midletThread = new Thread(new Runnable() {
                public void run() {
                    try { midlet.startApp(); }
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
                    public void run() { dispatchRunCommand(); }
                });
                Thread.sleep(4000);
            } catch (Throwable e) {
                System.out.println("[smoke] command dispatch failed: " + e);
            }
            dumpState(midlet);
            System.exit(0);
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try { midlet.startApp(); }
                catch (MIDletStateChangeException e) { }
                catch (Throwable e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        });

        if (System.getProperty("opentty.probe") != null) {
            javax.swing.Timer t = new javax.swing.Timer(2000, new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    System.out.println("[probe] display = " + describe(Display.staticCurrent()));
                }
            });
            t.start();
        }
    }

    private static void dispatchRunCommand() {
        Object cur = Display.staticCurrent();
        if (!(cur instanceof Form)) { System.out.println("[smoke] no Form to drive: " + cur); return; }
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
            if (stdin == null) { System.out.println("[smoke] stdin field not found"); return; }
            stdin.setString("echo hello opentty desktop");

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
                System.out.println("[smoke] run command/listener not found (cmds=" + cmds.size() + ")");
                return;
            }
            System.out.println("[smoke] firing Run: '" + stdin.getString() + "'");
            ((javax.microedition.lcdui.CommandListener) listener).commandAction(run, form);
        } catch (Throwable e) {
            System.out.println("[smoke] driving failed: " + e);
            e.printStackTrace();
        }
    }

    private static void dumpState(MIDlet midlet) {
        System.out.println("[smoke] current display = " + describe(Display.staticCurrent()));
        try {
            java.lang.reflect.Field sf = OpenTTY.class.getDeclaredField("sys");
            sf.setAccessible(true);
            java.util.Hashtable sys = (java.util.Hashtable) sf.get(midlet);
            System.out.println("[smoke] sys.size()=" + sys.size());
            java.util.Enumeration en = sys.keys();
            while (en.hasMoreElements()) {
                Object pid = en.nextElement();
                Object p = sys.get(pid);
                String name = "";
                try {
                    java.lang.reflect.Field nf = p.getClass().getDeclaredField("name");
                    nf.setAccessible(true);
                    Object n = nf.get(p);
                    if (n != null) { name = n.toString(); }
                } catch (Throwable e) { }
                System.out.println("[smoke]   pid=" + pid + " " + p.getClass().getSimpleName() + " name=" + name);
            }
        } catch (Throwable e) {
            System.out.println("[smoke] sys dump failed: " + e);
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