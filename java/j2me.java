import java.util.*;
import java.io.*;
import java.net.*;
// |
// J2ME Compatibility Layer for Java 8
// Provides stub classes matching J2ME API surface used by Lua.java and ELF.java
// |
// | --- Listeners ---
interface CommandListener { void commandAction(Command c, Displayable d); }
interface ItemCommandListener { void commandAction(Command c, Item item); }
interface ItemStateListener { void itemStateChanged(Item item); }
// |
// | --- lcdui ---
class Command {
    public static final int BACK = 1, OK = 4, CANCEL = 3, HELP = 5, STOP = 6, EXIT = 7, ITEM = 8, SCREEN = 1;
    public String label; public int commandType; public int priority;
    public Command(String label, int commandType, int priority) { this.label = label; this.commandType = commandType; this.priority = priority; }
    public String getLabel() { return label; }
}
class Displayable {
    protected String title; protected Vector commands = new Vector(); protected CommandListener commandListener;
    public void addCommand(Command c) { commands.addElement(c); }
    public void setCommandListener(CommandListener l) { this.commandListener = l; }
    public void setTitle(String t) { this.title = t; }
    public void setTicker(Ticker t) { }
    public String getTitle() { return title; }
}
class Item {
    public static final int LAYOUT_DEFAULT = 0, LAYOUT_EXPAND = 0x20, LAYOUT_NEWLINE_AFTER = 0x100, LAYOUT_NEWLINE_BEFORE = 0x200;
    protected String label;
    public String getLabel() { return label; }
    public void setLabel(String l) { this.label = l; }
}
class Spacer extends Item { public Spacer(int w, int h) { } }
class Gauge extends Item {
    private int value;
    public Gauge(String label, boolean interactive, int maxValue, int value) { this.label = label; this.value = value; }
    public int getValue() { return value; }
}
class TextField extends Item {
    public static final int ANY = 0, NUMERIC = 2, EMAILADDR = 1, PHONENUMBER = 3, DECIMAL = 4, PASSWORD = 0x10000;
    private String text;
    public TextField(String label, String text, int maxSize, int constraints) { this.label = label; this.text = text != null ? text : ""; }
    public String getString() { return text; }
    public void setString(String s) { this.text = s != null ? s : ""; }
}
class StringItem extends Item {
    public static final int HYPERLINK = 1, BUTTON = 2;
    private String text; private Font font;
    public StringItem(String label, String text) { this.label = label; this.text = text != null ? text : ""; }
    public StringItem(String label, String text, int layout) { this.label = label; this.text = text != null ? text : ""; }
    public String getText() { return text; }
    public void setText(String t) { this.text = t; }
    public void setFont(Font f) { this.font = f; }
    public void addCommand(Command c) { }
    public void setDefaultCommand(Command c) { }
    public void setItemCommandListener(ItemCommandListener l) { }
    public void setLayout(int l) { }
}
class ChoiceGroup extends Item {
    public static final int EXCLUSIVE = 1, MULTIPLE = 2, POPUP = 3;
    private Vector items = new Vector(); private Vector selected = new Vector();
    public ChoiceGroup(String label, int type) { this.label = label; }
    public void append(String text, Object img) { items.addElement(text); selected.addElement(Boolean.FALSE); }
    public int size() { return items.size(); }
    public boolean isSelected(int index) { return index < selected.size() && ((Boolean) selected.elementAt(index)).booleanValue(); }
    public String getString(int index) { return index < items.size() ? (String) items.elementAt(index) : ""; }
}
class Choice { public static final int EXCLUSIVE = 1, MULTIPLE = 2, POPUP = 3; }
class Ticker { public Ticker(String s) { } }
class Font {
    public static final int FACE_SYSTEM = 0, FACE_MONOSPACE = 1, FACE_PROPORTIONAL = 2;
    public static final int PLAIN = 0, BOLD = 1, ITALIC = 2;
    public static final int STYLE_PLAIN = 0, STYLE_BOLD = 1, STYLE_ITALIC = 2, STYLE_UNDERLINED = 4;
    public static final int SIZE_SMALL = 8, SIZE_MEDIUM = 10, SIZE_LARGE = 12;
    public Font() { }
    public Font(String name, int style, int size) { }
    public static Font getFont(int face, int style, int size) { return new Font(); }
    public static Font getDefaultFont() { return new Font(); }
}
class Image {
    public static Image createImage(String name) throws Exception { return new Image(); }
    public static Image createImage(int w, int h) { return new Image(); }
    public static Image createImage(InputStream is) throws Exception { return new Image(); }
}
class Form extends Displayable {
    private Vector items = new Vector(); private ItemStateListener stateListener;
    public Form(String title) { this.title = title; }
    public void append(Item item) { items.addElement(item); }
    public void append(StringItem si) { items.addElement(si); }
    public void append(Image img) { }
    public void append(TextField tf) { items.addElement(tf); }
    public void append(Gauge g) { items.addElement(g); }
    public void append(Spacer s) { items.addElement(s); }
    public void append(ChoiceGroup cg) { items.addElement(cg); }
    public void deleteAll() { items.removeAllElements(); }
    public int size() { return items.size(); }
    public Item get(int index) { return index < items.size() ? (Item) items.elementAt(index) : null; }
    public void setItemStateListener(ItemStateListener l) { this.stateListener = l; }
}
class Alert extends Displayable {
    public static final int FOREVER = -2;
    public Alert(String title, String text, Object img, AlertType type) { this.title = title; }
    public void setTimeout(int t) { }
}
class AlertType { public static final AlertType INFO = new AlertType(), WARNING = new AlertType(); }
class TextBox extends Displayable {
    private String text;
    public TextBox(String title, String text, int maxSize, int constraints) { this.title = title; this.text = text != null ? text : ""; }
    public String getString() { return text; }
    public void setString(String s) { this.text = s; }
}
class List extends Displayable {
    public static final int EXCLUSIVE = 1, MULTIPLE = 2, IMPLICIT = 3;
    public static final Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);
    private Vector items = new Vector();
    public List(String title, int type) { this.title = title; }
    public void append(String text, Object image) { items.addElement(text); }
    public int size() { return items.size(); }
    public String getString(int index) { return index < items.size() ? (String) items.elementAt(index) : ""; }
    public boolean isSelected(int index) { return false; }
    public void deleteAll() { items.removeAllElements(); }
}
class Canvas extends Displayable { }
class Display {
    private Displayable current;
    public static Display getDisplay(OpenTTY midlet) { return new Display(); }
    public void setCurrent(Displayable d) { this.current = d; }
    public void setCurrent(Alert alert, Displayable d) { this.current = d; }
    public Displayable getCurrent() { return current; }
    public void vibrate(int duration) { }
}
// |
// | --- IO ---
class Connector {
    public static final int READ = 1, WRITE = 2, READ_WRITE = 3;
    public static Object open(String url) throws Exception {
        if (url.startsWith("socket://")) {
            String hostPort = url.substring(9);
            if (hostPort.startsWith(":")) {
                int port = Integer.parseInt(hostPort.substring(1));
                java.net.ServerSocket ss = new java.net.ServerSocket(port);
                return new ServerSocketConnection(ss);
            } else {
                int colon = hostPort.lastIndexOf(':');
                String host = hostPort.substring(0, colon);
                int port = Integer.parseInt(hostPort.substring(colon + 1));
                java.net.Socket s = new java.net.Socket(host, port);
                return new SocketConnection(s);
            }
        } else if (url.startsWith("file:///")) {
            return new FileConnection(url.substring(8));
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            return new HttpConnection(url);
        }
        throw new IOException("Unsupported protocol: " + url);
    }
    public static Object open(String url, int mode) throws Exception { return open(url); }
}
class ConnectionNotFoundException extends Exception { public ConnectionNotFoundException(String msg) { super(msg); } }
interface StreamConnection { InputStream openInputStream() throws Exception; OutputStream openOutputStream() throws Exception; void close() throws Exception; }
interface StreamConnectionNotifier { Object acceptAndOpen() throws Exception; default void close() throws Exception {} }
class SocketConn implements StreamConnection {
    private java.net.Socket socket;
    public SocketConn(java.net.Socket s) { this.socket = s; }
    public InputStream openInputStream() throws Exception { return socket.getInputStream(); }
    public OutputStream openOutputStream() throws Exception { return socket.getOutputStream(); }
    public void close() throws Exception { socket.close(); }
    public String getAddress() { return socket.getInetAddress().getHostAddress(); }
    public String getLocalAddress() { return socket.getLocalAddress().getHostAddress(); }
    public int getPort() { return socket.getPort(); }
    public int getLocalPort() { return socket.getLocalPort(); }
}
class SocketConnection extends SocketConn { public SocketConnection(java.net.Socket s) { super(s); } }
class SocketServer implements StreamConnectionNotifier {
    private java.net.ServerSocket server;
    public SocketServer(java.net.ServerSocket s) { this.server = s; }
    public Object acceptAndOpen() throws Exception { return new SocketConn(server.accept()); }
    public int getLocalPort() { return server.getLocalPort(); }
    public void close() throws Exception { server.close(); }
}
class ServerSocketConnection extends SocketServer { public ServerSocketConnection(java.net.ServerSocket s) { super(s); } }
class FileConn {
    private File file;
    public FileConn(String path) { this.file = new File(path); }
    public boolean exists() { return file.exists(); }
    public boolean isDirectory() { return file.isDirectory(); }
    public void mkdir() { file.mkdirs(); }
    public void delete() { file.delete(); }
    public Enumeration list() {
        String[] files = file.list();
        if (files == null) return new Vector().elements();
        Vector v = new Vector();
        for (int i = 0; i < files.length; i++) v.addElement(files[i]);
        return v.elements();
    }
    public void close() { }
}
class FileConnection extends FileConn { public FileConnection(String path) { super(path); } }
class HttpConn implements StreamConnection {
    private java.net.HttpURLConnection conn; private String url;
    public HttpConn(String url) { this.url = url; }
    public void setRequestMethod(String m) throws Exception { }
    public void setRequestProperty(String k, String v) { }
    public OutputStream openOutputStream() throws Exception {
        java.net.URL u = new java.net.URL(url);
        conn = (java.net.HttpURLConnection) u.openConnection();
        conn.setDoOutput(true);
        return conn.getOutputStream();
    }
    public InputStream openInputStream() throws Exception {
        if (conn == null) { java.net.URL u = new java.net.URL(url); conn = (java.net.HttpURLConnection) u.openConnection(); }
        return conn.getInputStream();
    }
    public int getResponseCode() throws Exception { return conn != null ? conn.getResponseCode() : 200; }
    public void close() throws Exception { if (conn != null) conn.disconnect(); }
}
class HttpConnection extends HttpConn {
    public static final int GET = 1, POST = 2;
    public HttpConnection(String url) { super(url); }
}
class FileSystemRegistry {
    public static Enumeration listRoots() {
        File[] roots = File.listRoots();
        Vector v = new Vector();
        for (int i = 0; i < roots.length; i++) v.addElement(roots[i].getAbsolutePath());
        return v.elements();
    }
}
class RecordStore {
    public static String[] listRecordStores() { return null; }
}
// |
class PushRegistry {
    public static void registerConnection(String connection, String midlet, String filter) throws Exception { }
    public static boolean unregisterConnection(String connection) { return false; }
    public static String[] listConnections(boolean pending) { return new String[0]; }
    public static long registerAlarm(String midlet, long time) throws Exception, ClassNotFoundException { return 0; }
}
// |
// | --- Media ---
interface VolumeControl { int setLevel(int level); int getLevel(); }
interface Player {
    long TIME_UNKNOWN = -1;
    void prefetch() throws Exception;
    void start() throws Exception;
    void stop();
    void deallocate();
    void close();
    Object getControl(String type);
    long getDuration();
    long getMediaTime();
    long setMediaTime(long time) throws Exception;
}
class Manager {
    public static Player createPlayer(InputStream is, String type) throws Exception {
        javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(is);
        javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
        clip.open(ais);
        return new JavaSoundPlayer(clip);
    }
}
class JavaSoundPlayer implements Player {
    private javax.sound.sampled.Clip clip;
    private VolumeControl vc;
    public JavaSoundPlayer(javax.sound.sampled.Clip c) {
        this.clip = c;
        this.vc = new VolumeControl() {
            public int setLevel(int l) { return l; }
            public int getLevel() { return 50; }
        };
    }
    public void prefetch() { }
    public void start() { clip.start(); }
    public void stop() { clip.stop(); }
    public void deallocate() { clip.close(); }
    public void close() { clip.close(); }
    public Object getControl(String type) { return vc; }
    public long getDuration() { return clip.getMicrosecondLength() / 1000; }
    public long getMediaTime() { return clip.getMicrosecondPosition() / 1000; }
    public long setMediaTime(long time) { clip.setMicrosecondPosition(time * 1000); return time; }
}
