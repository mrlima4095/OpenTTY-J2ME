package javax.microedition.io;

import java.util.Hashtable;

/**
 * Desktop stub of the PushRegistry. Connections are tracked in memory only
 * (no real push channel). registerAlarm always reports success.
 */
public class PushRegistry {
    private static final Hashtable registrations = new Hashtable();   // connection -> midletClass
    private static final Hashtable alarms = new Hashtable();          // midletClass -> Long time

    private PushRegistry() { }

    public static void registerConnection(String connection, String midletClass, String filter)
            throws ClassNotFoundException {
        if (connection == null || midletClass == null) {
            throw new IllegalArgumentException("Null connection or class");
        }
        checkClass(midletClass);
        registrations.put(connection, midletClass);
    }

    public static boolean unregisterConnection(String connection) {
        return registrations.remove(connection) != null;
    }

    public static String[] listConnections(boolean available) {
        String[] out = new String[registrations.size()];
        int i = 0;
        for (java.util.Enumeration e = registrations.keys(); e.hasMoreElements();) {
            out[i++] = (String) e.nextElement();
        }
        return out;
    }

    public static long registerAlarm(String midletClass, long time) throws ClassNotFoundException, ConnectionNotFoundException {
        checkClass(midletClass);
        alarms.put(midletClass, new Long(time));
        return time;
    }

    public static long getAlarmTime(String midletClass) {
        Long t = (Long) alarms.get(midletClass);
        return t == null ? 0 : t.longValue();
    }

    private static void checkClass(String midletClass) throws ClassNotFoundException {
        try {
            Class.forName(midletClass);
        } catch (ClassNotFoundException e) {
            // Also accept the runtime MIDlet name without a package.
            try {
                Class.forName("javax.microedition.midlet." + midletClass);
            } catch (ClassNotFoundException e2) {
                throw e;
            }
        }
    }
}