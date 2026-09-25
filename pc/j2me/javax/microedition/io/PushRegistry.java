package javax.microedition.io;

import java.util.Vector;

public class PushRegistry {
    private static final Vector registered = new Vector();

    private PushRegistry() { }

    public static void registerConnection(String connection, String midlet, String filter) throws ClassNotFoundException, ConnectionNotFoundException {
        if (connection == null) { throw new IllegalArgumentException("null connection"); }
        synchronized (registered) {
            if (!registered.contains(connection)) { registered.addElement(connection); }
        }
    }

    public static boolean unregisterConnection(String connection) {
        synchronized (registered) {
            return registered.removeElement(connection);
        }
    }

    public static String[] listConnections(boolean available) {
        synchronized (registered) {
            String[] out = new String[registered.size()];
            registered.copyInto(out);
            return out;
        }
    }

    public static long registerAlarm(String midlet, long time) throws ClassNotFoundException, ConnectionNotFoundException {
        return time;
    }
}
