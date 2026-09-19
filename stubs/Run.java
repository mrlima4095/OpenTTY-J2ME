import javax.microedition.midlet.MIDlet;

/**
 * Desktop launcher for the J2ME OpenTTY MIDlet.
 *
 * Sets the microedition.* system properties (which OpenTTY's /bin/init reads
 * via MIDlet.getAppProperty -> System.getProperty), instantiates the MIDlet,
 * and drives its lifecycle.
 */
public class Run {
    public static void main(String[] args) throws Exception {
        System.setProperty("microedition.profiles", "MIDP-2.0");
        System.setProperty("microedition.configuration", "CLDC-1.0");
        System.setProperty("microedition.platform", "opentty-desktop");
        if (System.getProperty("microedition.locale") == null) {
            System.setProperty("microedition.locale", "en-US");
        }

        MIDlet midlet = new OpenTTY();
        midlet.startApp();

        MIDlet._awaitTermination();
        System.exit(0);
    }
}