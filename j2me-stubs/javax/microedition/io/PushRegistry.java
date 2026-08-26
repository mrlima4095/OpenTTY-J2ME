package javax.microedition.io;
public final class PushRegistry {
    public static void registerConnection(String connection, String midlet, String filter) throws Exception {}
    public static boolean unregisterConnection(String connection) throws Exception { return false; }
    public static String[] listConnections(boolean availableOnly) { return new String[0]; }
    public static long registerAlarm(String midlet, long time) throws ClassNotFoundException, javax.microedition.io.ConnectionNotFoundException { return 0; }
    public static String getMIDlet(String connection) { return null; }
    public static String getFilter(String connection) { return null; }
}
