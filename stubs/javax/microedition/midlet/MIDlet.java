package javax.microedition.midlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import javax.microedition.io.ConnectionNotFoundException;

/**
 * Desktop-Java host for the OpenTTY J2ME MIDlet.
 *
 * This is a functional stub of javax.microedition.midlet.MIDlet. The lifecycle
 * methods are declared public (instead of MIDP's protected) so that the
 * desktop launcher (Run.java) can drive them directly.
 */
public abstract class MIDlet {
    private static MIDlet active = null;
    private static final CountDownLatch TERMINATED = new CountDownLatch(1);

    private final Properties props = new Properties();

    public MIDlet() {
        active = this;
        loadAppProperties();
    }

    public static MIDlet _active() { return active; }

    private void loadAppProperties() {
        props.setProperty("MIDlet-Name", "OpenTTY");
        props.setProperty("MIDlet-Version", "1.18.2");
        props.setProperty("MIDlet-Vendor", "Mr. Lima");
        props.setProperty("MIDlet-1", "OpenTTY,,OpenTTY");
        props.setProperty("MIDlet-Description", "OpenTTY J2ME shell + RISC-V emulator (desktop)");
        String jad = System.getProperty("opentty.jad");
        if (jad != null && jad.length() > 0) {
            try {
                InputStream in = new java.io.FileInputStream(jad);
                try { props.load(in); } finally { in.close(); }
            } catch (IOException e) { }
        }
        InputStream in = MIDlet.class.getResourceAsStream("/MIDlet.properties");
        if (in != null) {
            try { props.load(in); } catch (IOException e) { } finally { try { in.close(); } catch (IOException e) { } }
        }
        for (String key : props.stringPropertyNames()) {
            String sysval = System.getProperty("opentty." + key);
            if (sysval != null) { props.setProperty(key, sysval); }
        }
    }

    public abstract void startApp() throws MIDletStateChangeException;
    public abstract void pauseApp();
    public abstract void destroyApp(boolean unconditional) throws MIDletStateChangeException;

    public final String getAppProperty(String key) {
        if (key == null) { return null; }
        String v = props.getProperty(key);
        if (v != null) { return v; }
        // Fall back to java System properties (e.g. microedition.*, MIDlet-*).
        v = System.getProperty(key);
        return v == null ? "" : v;
    }

    public final void notifyDestroyed() {
        TERMINATED.countDown();
    }

    public final void notifyPaused() { }

    public final void resumeRequest() { }

    public boolean platformRequest(String url) throws ConnectionNotFoundException {
        if (url == null) { throw new ConnectionNotFoundException("Null URL"); }
        try {
            if (java.awt.GraphicsEnvironment.isHeadless()) { return false; }
            java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
            if (desktop != null && desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
                desktop.browse(new URI(url));
                return true;
            }
        } catch (Exception e) { }
        return false;
    }

    /** Blocks the launcher until notifyDestroyed() is called. */
    public static void _awaitTermination() throws InterruptedException {
        TERMINATED.await();
    }
}