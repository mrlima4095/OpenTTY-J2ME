package javax.microedition.midlet;

import java.util.Hashtable;

/**
 * Desktop stub of the MIDP MIDlet lifecycle. The real app classes in src/
 * are compiled unchanged against this. Only the glue (OpenTTYMain) is added.
 */
public abstract class MIDlet {
    private static MIDlet current = null;
    private final Hashtable props = new Hashtable();

    protected MIDlet() {
        current = this;
        props.put("MIDlet-Name", "OpenTTY");
        props.put("MIDlet-Version", "1.18.2");
        props.put("MIDlet-Vendor", "Mr Lima");
        props.put("MIDlet-Description", "Terminal Emulator for J2ME");
        props.put("MIDlet-Info-URL", "https://github.com/mrlima4095/OpenTTY-J2ME");
        props.put("MIDlet-Icon", "/icon.png");
        props.put("microedition.platform", "j2me");
        props.put("microedition.configuration", "CLDC-1.0");
        props.put("microedition.profiles", "MIDP-2.0");
        props.put("microedition.locale", System.getProperty("user.language", "en"));
        String ui = System.getProperty("opentty.user", "opentty");
        props.put("opentty.user", ui);
        String rms = System.getProperty("opentty.rms.dirl", ".");
        props.put("opentty.rms.dir", rms);
    }

    public static MIDlet getCurrent() { return current; }

    /** Returns the property value or null. Leading "/" keys are also tried
     *  without the slash (the init script queries "/microedition.platform"). */
    public final String getAppProperty(String key) {
        if (key == null) { return null; }
        Object v = props.get(key);
        if (v == null && key.length() > 1 && key.startsWith("/")) { v = props.get(key.substring(1)); }
        return v != null ? v.toString() : System.getProperty(key, null);
    }

    public final void notifyDestroyed() { System.out.println("[OpenTTY] MIDlet destroyed"); }

    public final void notifyPaused() { }

    public final boolean platformRequest(String url) throws javax.microedition.io.ConnectionNotFoundException {
        if (url == null || url.length() == 0) { return false; }
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            } else {
                Process p = Runtime.getRuntime().exec(new String[] { "xdg-open", url });
                p.waitFor();
            }
            return true;
        } catch (Throwable e) {
            System.err.println("[OpenTTY] platformRequest(" + url + ") failed: " + e);
            return false;
        }
    }

    public final void resumeRequest() { }

    public abstract void startApp() throws javax.microedition.midlet.MIDletStateChangeException;
    protected abstract void pauseApp();
    protected abstract void destroyApp(boolean unconditional) throws javax.microedition.midlet.MIDletStateChangeException;
}