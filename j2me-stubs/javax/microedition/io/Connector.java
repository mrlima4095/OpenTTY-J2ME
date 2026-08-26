package javax.microedition.io;
import java.io.*;
public class Connector {
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;
    public static Connection open(String name) throws Exception { return null; }
    public static Connection open(String name, int mode) throws Exception { return null; }
    public static Connection open(String name, int mode, boolean timeouts) throws Exception { return null; }
    public static InputStream openInputStream(String name) throws Exception { return null; }
    public static OutputStream openOutputStream(String name) throws Exception { return null; }
}
