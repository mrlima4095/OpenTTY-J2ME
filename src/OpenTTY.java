import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.control.*;
import javax.microedition.io.file.*;
import javax.wireless.messaging.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// OpenTTY MIDlet
public class OpenTTY extends MIDlet implements CommandListener {
    // Behavior Settings
    public int TTY_MAX_LEN = 0, cursorX = 10, cursorY = 10;
    public boolean classpath = true, useCache = true;
    public long uptime = System.currentTimeMillis();
    // |
    // System Objects
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    
    public Hashtable attributes = new Hashtable(), fs = new Hashtable(), sys = new Hashtable(), filetypes = null, aliases = new Hashtable(), tmp = new Hashtable(), cache = new Hashtable(), globals = new Hashtable();
    public String username = read("/home/OpenRMS"), buffer = "", logs = "", path = "/home/", build = "2025-1.17-03x02";
    // |
    // Graphics
    public Display display = Display.getDisplay(this);
    public Form xterm = new Form(null);
    public TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    public StringItem stdout = new StringItem("", "");
    public Command BACK = new Command("Back", Command.BACK, 1), EXECUTE = new Command("Run", Command.OK, 0);
    // |
    // MIDlet Loader
    public void startApp() {
        if (sys.containsKey("1")) { }
        else {
            attributes.put("PATCH", "Fear Fog"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.6.4");
            attributes.put("HOSTNAME", "localhost"); attributes.put("QUERY", "nano"); attributes.put("SHELL", "/bin/sh");
            // |
            String[] KEYS = { "TYPE", "CONFIG", "PROFILE", "LOCALE" }, SYS = { "platform", "configuration", "profiles", "locale" };
            for (int i = 0; i < KEYS.length; i++) { attributes.put(KEYS[i], System.getProperty("microedition." + SYS[i])); }
            // |
            if (runScript(read("/etc/init"), 0, "1", this.stdout, globals) != 0) { destroyApp(true); }
            setLabel();
            // |
            if (username.equals("") || MIDletControl.passwd().equals("")) { new MIDletControl(); }
            else { run("/home/.initrc", new String[] { "/home/.initrc" }, 1000, "1", this.stdout, globals); }
        }
    }
    // |
    // | (Triggers)
    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { } // false = user | true = system
    // |
    // | (Main Listener)
    public void commandAction(Command c, Displayable d) {
        if (c == EXECUTE) { 
            String command = stdin.getString().trim(); 
            add2History(command); 
            stdin.setString(""); 
            processCommand(command, true, 1000, "0", stdout, globals); 
            setLabel();
        } 
    }
    // |
    // Control Thread
    public OpenTTY getInstance() { return this; }
    public String getThreadName(Thread thr) { String name = thr.getName(); String[] generic = { "Thread-0", "Thread-1", "MIDletEventQueue", "main" }; for (int i = 0; i < generic.length; i++) { if (name.equals(generic[i])) { name = "MIDlet"; break; } } return name; }
    public int setLabel() { stdin.setLabel(username + " " + path + " " + (username.equals("root") ? "#" : "$")); return 0; }
    public class MIDletControl implements ItemCommandListener, CommandListener, Runnable {
        public static final int HISTORY = 1, EXPLORER = 2, MONITOR = 3, PROCESS = 4, SIGNUP = 5, REQUEST = 6, LOCK = 7, NC = 8, BIND = 11, BG = 12, ADDON = 13, NANO = 14;
        public static final String impl = "full";

        private int MOD = -1, COUNT = 1, id = 1000, start;
        private boolean enable = true, asked = false, keep = false, asking_user = username.equals(""), asking_passwd = passwd().equals(""), isOpen = true, closed = false;
        private String command = null, pfilter = "", PID = genpid(), DB, address, port, node, proc_name, filename;
        private Vector history = (Vector) getobject("1", "history");
        private Hashtable sessions = (Hashtable) getobject("1", "sessions"), PKG, scope;
        private Alert confirm;
        private Form monitor;
        private List preview;
        private TextBox box;
        private Object stdout;
        private StringItem console, s;
        private TextField USER, PASSWD, remotein;
        private Command BACK = new Command("Back", Command.BACK, 1), CLEAR, COPY, CUT, PASTE, MENU, RUN, RUNS, IMPORT, OPEN, EDIT, REFRESH, PROPERTY, KILL, LOAD, DELETE, LOGIN, EXIT, FILTER, CONNECT, VIEW, SAVE, YES, NO;

        private SocketConnection CONN;
        private ServerSocketConnection server = null;
        private InputStream IN;
        private OutputStream OUT;

        private String[] wordlist;

        public MIDletControl() { MOD = SIGNUP; monitor = new Form("Login"); monitor.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\n" + (asking_user && asking_passwd ? "Create your credentials!" : asking_user ? "Create an user to access OpenTTY!" : asking_passwd ? "Create a password!" : "")).trim()); if (asking_user) { monitor.append(USER = new TextField("Username", "", 256, TextField.ANY)); } if (asking_passwd) { monitor.append(PASSWD = new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); } monitor.addCommand(LOGIN = new Command("Login", Command.OK, 1)); monitor.addCommand(EXIT = new Command("Exit", Command.SCREEN, 2)); monitor.setCommandListener(this); display.setCurrent(monitor); }
        public MIDletControl(String command, boolean enable, String pid, Object stdout, Hashtable scope) { MOD = REQUEST; this.enable = enable; this.pid = pid; this.stdout = stdout; this.scope = scope; this.command = command; if (asking_passwd) { new MIDletControl(); return; } monitor = new Form(xterm.getTitle()); monitor.append(PASSWD = new TextField("[sudo] password for " + loadRMS("OpenRMS"), "", 256, TextField.ANY | TextField.PASSWORD)); monitor.addCommand(EXECUTE); monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 2)); monitor.setCommandListener(this); display.setCurrent(monitor); }

        public MIDletControl(String command, int id, Object stdout, Hashtable scope) { MOD = command == null || command.length() == 0 || command.equals("monitor") ? MONITOR : command.equals("process") ? PROCESS : command.equals("dir") ? EXPLORER : command.equals("history") ? HISTORY : -1; this.id = id; this.stdout = stdout; this.scope = scope; if (MOD == MONITOR) { monitor = new Form(xterm.getTitle()); monitor.append(console = new StringItem("Memory Status:", "")); monitor.addCommand(BACK); monitor.addCommand(MENU = new Command("Menu", Command.SCREEN, 1)); monitor.addCommand(REFRESH = new Command("Refresh", Command.SCREEN, 2)); monitor.setCommandListener(this); load(); display.setCurrent(monitor); } else { preview = new List(xterm.getTitle(), List.IMPLICIT); preview.addCommand(BACK); preview.addCommand(MOD == EXPLORER ? (OPEN = new Command("Open", Command.OK, 1)) : MOD == PROCESS ? (KILL = new Command("Kill", Command.OK, 1)) : (RUN = new Command("Run", Command.OK, 1))); if (MOD == HISTORY) { preview.addCommand(EDIT = new Command("Edit", Command.OK, 1)); } if (MOD == PROCESS) { preview.addCommand(LOAD = new Command("Load Screen", Command.OK, 1)); preview.addCommand(VIEW = new Command("View info", Command.OK, 1)); preview.addCommand(REFRESH = new Command("Refresh", Command.OK, 1)); preview.addCommand(FILTER = new Command("Filter", Command.OK, 1)); } else if (MOD == EXPLORER) { preview.addCommand(DELETE = new Command("Delete", Command.OK, 1)); preview.addCommand(RUNS = new Command("Run Script", Command.OK, 1)); preview.addCommand(PROPERTY = new Command("Properties", Command.OK, 1)); preview.addCommand(REFRESH = new Command("Refresh", Command.OK, 1)); } preview.setCommandListener(this); load(); display.setCurrent(preview); } }
        public MIDletControl(String mode, String args, int id, Object stdout, Hashtable scope) {
            MOD = mode == null || mode.length() == 0 || mode.equals("nc") ? NC : BIND;
            this.id = id;

            if (args == null || args.length() == 0) { return; }
            else if (MOD == BIND) {
                String[] argv = splitArgs(args);

                port = argv[0]; 
                DB = argv.length > 1 ? argv[1] : "";
                proc_name = argv.length > 2 ? argv[2] : "bind";

                new Thread(this, "Bind").start();
                return;
            } else {
                Hashtable proc = genprocess("remote", id, null);

                address = args;
                try { CONN = (SocketConnection) Connector.open("socket://" + address); IN = CONN.openInputStream(); OUT = CONN.openOutputStream(); } 
                catch (Exception e) { print(getCatch(e), stdout); return; }

                monitor = new Form(xterm.getTitle());
                monitor.append(console = new StringItem("", ""));
                monitor.append(remotein = new TextField("Remote (" + split(address, ':')[0] + ")", "", 256, TextField.ANY));
                monitor.addCommand(EXECUTE);
                monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
                monitor.addCommand(CLEAR);
                monitor.addCommand(VIEW = new Command("View info", Command.SCREEN, 2));
                monitor.setCommandListener(this);

                proc.put("socket", CONN); proc.put("in", IN); proc.put("out", OUT); proc.put("screen", monitor);
                display.setCurrent(monitor);

                sys.put(PID, proc);
                new Thread(this, "Remote").start();
            }
        }
        public MIDletControl(Form screen, String node, String code, int id, Object stdout, Hashtable scope) { if (code == null || code.length() == 0) { return; } this.PKG = parseProperties(code); this.node = node; this.id = id; if (!PKG.containsKey(node + ".label") || !PKG.containsKey(node + ".cmd")) { MIDletLogs("add error Malformed ITEM, missing params", id, stdout); return; } RUN = new Command(getenv(node + ".label"), Command.ITEM, 1); s = new StringItem(null, getenv(node + ".label"), StringItem.BUTTON); s.setFont(newFont(getenv(node + ".style", "default"))); s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(RUN); s.setDefaultCommand(RUN); s.setItemCommandListener(this); screen.append(s); }
        public MIDletControl(String name, String command, boolean enable, int id, Object stdout, Hashtable scope) { this.MOD = BG; this.command = command; this.enable = enable; this.id = id; this.stdout = stdout; this.scope = scope; new Thread(this, name).start(); }
        public MIDletControl(String pid, String name, String command, boolean enable, int id, Object stdout, Hashtable scope) { this.MOD = ADDON; this.PID = pid; this.command = command; this.enable = enable; this.id = id;  this.stdout = stdout; this.scope = scope; new Thread(this, name).start(); }
    
        public MIDletControl(String file, boolean enable, int id, Object stdout, Hashtable scope) {
            MOD = NANO; this.filename = file; this.enable = enable; this.id = id; this.stdout = stdout; this.scope = scope;
            
            box = new TextBox("Nano - " + (file == null || file.equals("") ? "New Buffer" : file), (file == null || file.equals("")) ? "" : getcontent(file), 31522, TextField.ANY);
            box.addCommand(BACK);
            box.addCommand(CLEAR = new Command("Clear", Command.SCREEN, 1));
            box.addCommand(VIEW = new Command("View as HTML", Command.SCREEN, 2));
            box.setCommandListener(this);
            display.setCurrent(box); 
        }

        public void commandAction(Command c, Displayable d) {
            if (c == BACK) { 
                if (d == box) { 
                    confirm = new Alert("Nano Editor", "Save modified buffer?", null, AlertType.WARNING); 
                    confirm.addCommand(YES = new Command("Yes", Command.OK, 1)); confirm.addCommand(NO = new Command("No", Command.BACK, 1)); 
                    confirm.setCommandListener(this); display.setCurrent(confirm);
                }
                if (d == monitor && MOD == EXPLORER) { display.setCurrent(preview); }
                else if (d == monitor && MOD == NANO) { display.setCurrent(box); }
                else if (MOD == NC) { back(); } 
                else { goback(); } 

                return; 
            }
            if (d == box) { pfilter = box.getString().trim(); load(); display.setCurrent(preview); return; }
            if (d == confirm) {
                if (MOD == NC) { if (c == YES) { keep = true; } else { sys.remove(PID); } goback(); }
                else if (MOD == NANO) {
                    if (c == YES) {
                        if (filename == null || filename.equals("")) {
                            monitor = new Form("Nano Editor");
                            monitor.append(USER = new TextField("File Name to Write", "", 256, TextField.ANY));
                            monitor.addCommand(BACK);
                            monitor.addCommand(SAVE = new Command("Save", Command.OK, 1));
                            monitor.setCommandListener(this);
                            display.setCurrent(monitor);
                        } else { write(filename, box.getString(), id); }
                    }
                    else { goback(); }
                }
            }

            if (MOD == HISTORY) { String selected = preview.getString(preview.getSelectedIndex()); if (selected != null) { goback(); processCommand(c == RUN || c == List.SELECT_COMMAND ? selected : "buff " + selected, true, id, PID, stdout, scope); } } 
            else if (MOD == EXPLORER) {
                String selected = preview.getString(preview.getSelectedIndex());

                if (c == OPEN || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { if (selected != null) { processCommand(selected.endsWith("..") ? "cd .." : selected.endsWith("/") ? "cd " + path + selected : "nano " + path + selected, false, id, PID, stdout, scope); if (display.getCurrent() == preview) { reload(); } setLabel(); } } 
                else if (c == DELETE) { 
                    if (selected.equals("..")) {  }
                    else if (path.equals("/home/") || path.equals("/tmp/") || (path.startsWith("/mnt/") && !path.equals("/mnt/")) || path.equals("/bin/") || path.equals("/lib/")) {
                        int STATUS = deleteFile(path + selected, id, stdout); 
                        if (STATUS != 0) { warn(xterm.getTitle(), STATUS == 13 ? "Permission denied!" : "java.io.IOException"); } 
                        
                        reload(); 
                    } 
                    else { warn(m.getTitle(), "read-only storage"); }
                } 
                else if (c == RUNS) { if (selected.equals("..") || selected.endsWith("/")) { } else { goback(); processCommand(". " + path + selected, true, id, PID, stdout, scope); } } 
                else if (c == PROPERTY) {
                    if (selected.equals("..")) { }
                    else {
                        String[] info = getExtensionInfo(getExtension(selected)); 
                        String type = selected.endsWith("/") ? "Directory" : (path.startsWith("/home/") || path.startsWith("/tmp/")) ? (info[0].equals("Unknown") ? "ASCII text" : info[0]) : path.startsWith("/bin/") ? "Application" : path.startsWith("/dev/") ? "Special Device" : path.startsWith("/lib/") ? "Shared Package" : (path.equals("/tmp/") || path.equals("/res/")) ? "ASCII text" : info[0];
                        
                        monitor = new Form(selected + " - Information");
                        monitor.addCommand(BACK);
                        monitor.append(new StringItem("File:", path + selected));
                        monitor.append(new StringItem("Type:", type));
                        if (type.equals("Directory")) { }
                        else {
                            int size = 0;
                            try {
                                InputStream in = getInputStream(path + selected);
                                size = in.available(); in.close();
                            } catch (Exception e) { warn(xterm.getTitle(), getCatch(e)); }
                            
                            monitor.append(new StringItem("Size:", String.valueOf(size)));
                            ChoiceGroup perms = new ChoiceGroup("Permissions", Choice.MULTIPLE);
                            perms.append("Read", null); perms.append("Write", null);

                            perms.setSelectedIndex(0, true);
                            perms.setSelectedIndex(1, (path.startsWith("/home/") || path.startsWith("/tmp/") || (path.startsWith("/mnt/") && !path.equals("/mnt/")) || (selected = path + selected).equals("/dev/null") || selected.equals("/dev/stdin") || selected.equals("/dev/stdout") || ((path.equals("/bin/") || path.equals("/lib/")) && id == 0)));

                            monitor.append(perms);
                            if (info[2].equals("image")) { monitor.append(new ImageItem(null, readImg(path + selected), ImageItem.LAYOUT_DEFAULT, null)); }
                            if (info[2].equals("text") || path.startsWith("/home/") || path.startsWith("/tmp/")) { monitor.addCommand(OPEN); }
                        }
                        monitor.setCommandListener(this);
                        display.setCurrent(monitor);
                    }
                } 
                else if (c == REFRESH) { reload(); }
            } 
            else if (MOD == MONITOR) { System.gc(); reload(); } 
            else if (MOD == PROCESS) {
                if (c == FILTER) {
                    if (box == null) { box = new TextBox("Process Filter", "", 31522, TextField.ANY); box.addCommand(RUN = new Command("Apply", Command.OK, 1)); box.setCommandListener(this); }
                    
                    display.setCurrent(box);
                    return;
                }
                else if (c == REFRESH) { reload(); return; }

                int index = preview.getSelectedIndex();
                if (index >= 0) {
                    String PID = split(preview.getString(index), '\t')[0];
                    int STATUS = 0;

                    if (c == KILL || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { STATUS = kill(PID, false, id); } 
                    else if (c == VIEW) { processCommand("top view " + PID, false, id, PID, stdout, scope); } 
                    else if (c == LOAD) {
                        if (!getobject(PID, "owner").equals(username) && id != 0) { STATUS = 13; }

                        Displayable screen = (Displayable) getobject(PID, "screen");

                        if (screen == null) { STATUS = 69; } 
                        else { display.setCurrent(screen); return; }
                    }

                    if (STATUS != 0) { warn(xterm.getTitle(), STATUS == 13 ? "Permission denied!" : "No screens for this process!"); }

                    reload();
                }
            } 
            else if (MOD == SIGNUP) {
                if (c == LOGIN) {
                    String password = asking_passwd ? PASSWD.getString().trim() : "";

                    if (asking_user) { username = USER.getString().trim(); }
                    if (asking_user && username.equals("") || asking_passwd && password.equals("")) { warn(xterm.getTitle(), "Missing credentials!"); } 
                    else if (username.equals("root")) { USER.setString(""); warn(xterm.getTitle(), "Invalid username!"); } 
                    else {
                        if (asking_user) { write("/home/OpenRMS", username.getBytes(), 0); }
                        if (asking_passwd) { writeRMS("OpenRMS", password.getBytes(), 2); }

                        display.setCurrent(xterm);
                        getInstance().run("/home/.initrc", new String[] { "/home/.initrc" }, 1000);
                        setLabel();
                    }
                } 
                else if (c == EXIT) { destroyApp(true); }
            } 
            else if (MOD == REQUEST) {
                String password = PASSWD.getString().trim();

                if (password.equals("")) { } 
                else if (password.equals(passwd())) { goback(); processCommand(command, enable, 0, pid, stdout, scope); setLabel(); } 
                else { PASSWD.setString(""); warn(xterm.getTitle(), "Wrong password"); }
            }

            else if (MOD == NC) {
                if (c == EXECUTE) {
                    String PAYLOAD = remotein.getString().trim(); remotein.setString("");

                    try { OUT.write((PAYLOAD + "\n").getBytes()); OUT.flush(); } 
                    catch (Exception e) { warn(xterm.getTitle(), getCatch(e)); if (keep) { } else { sys.remove(PID); } }
                } 
                else if (c == BACK) { write("/home/remote", console.getText(), 0); back(); } 
                else if (c == CLEAR) { console.setText(""); }
                else if (c == VIEW) { try { warn("Information", "Host: " + split(address, ':')[0] + "\n" + "Port: " + split(address, ':')[1] + "\n\n" + "Local Address: " + CONN.getLocalAddress() + "\n" + "Local Port: " + CONN.getLocalPort()); } catch (Exception e) { warn(xterm.getTitle(), "Couldn't read connection information!"); } }
            }

            else if (MOD == NANO) {
                if (c == CLEAR) { box.setString(""); }
                else if (c == SAVE) { if (USER == null) { } else { String file = USER.getString().trim(); write(file, box.getString(), id); } }
            }
        }
        public void commandAction(Command c, Item item) { if (c == RUN) { goback(); processCommand((String) PKG.get(node + ".cmd"), true, id, PID, stdout, scope); } }

        private void reload() { if (attributes.containsKey("J2EMU")) { new MIDletControl(MOD == MONITOR ? "monitor" : MOD == PROCESS ? "process" : MOD == EXPLORER ? "dir" : "history", id, stdout, scope); } else { load(); } }
        private void load() {
            if (MOD == HISTORY) { preview.deleteAll(); for (int i = 0; i < history.size(); i++) { preview.append((String) history.elementAt(i), null); } } 
            else if (MOD == EXPLORER) {
                if (attributes.containsKey("J2EMU")) { }
                else { preview.setTitle(path); }

                preview.deleteAll();
                Vector stack = new Vector();
                if (path.equals("/")) { }
                else { preview.append("..", null); }

                try {
                    if (path.equals("/tmp/")) { for (Enumeration KEYS = tmp.keys(); KEYS.hasMoreElements();) { String file = (String) KEYS.nextElement(); if (file.startsWith(".")) { } else { preview.append(file, null); } } }
                    else if (path.equals("/mnt/")) { for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { preview.append((String) roots.nextElement(), null); } }
                    else if (path.startsWith("/mnt/")) {
                        FileConnection CONN = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ);
                        Vector dirs = new Vector(), files = new Vector();

                        for (Enumeration content = CONN.list(); content.hasMoreElements();) {
                            String name = (String) content.nextElement();

                            if (stack.contains(name)) { }
                            else if (name.endsWith("/")) { dirs.addElement(name); }
                            else { files.addElement(name); }
                        }

                        while (!dirs.isEmpty()) { preview.append(getFirstString(dirs), null); }
                        while (!files.isEmpty()) { preview.append(getFirstString(files), null); }

                        CONN.close();
                    } 
                    else if (path.equals("/bin/") || path.equals("/etc/") || path.equals("/lib/")) {
                        String content = loadRMS("OpenRMS", path.equals("/bin/") ? 3 : path.equals("/etc/") ? 5 : 4);
                        int index = 0;

                        while (true) {
                            int start = content.indexOf("[\0BEGIN:", index);
                            if (start == -1) { break; }

                            int end = content.indexOf("\0]", start);
                            if (end == -1) { break; }

                            String filename = content.substring(start + "[\0BEGIN:".length(), end);
                            if (filename.startsWith(".") || stack.contains(filename)) { } else { preview.append(filename, null); stack.addElement(filename); }

                            index = content.indexOf("[\0END\0]", end);
                            if (index == -1) { break; }

                            index += "[\0END\0]".length();
                        }
                    }
                    else if (path.startsWith("/home/")) { String[] recordStores = RecordStore.listRecordStores(); for (int i = 0; i < recordStores.length; i++) { if (!recordStores[i].startsWith(".")) { preview.append(recordStores[i], null); } } }

                    Vector files = (Vector) fs.get(path);
                    if (files != null) { for (int i = 0; i < files.size(); i++) { String f = (String) files.elementAt(i); if (f != null && !f.equals("..") && !f.equals("/") && !stack.contains(f)) { preview.append(f, null); stack.addElement(f); } } }
                } catch (IOException e) { }
            } 
            else if (MOD == MONITOR) { console.setText("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\n" + "Free Memory: " + runtime.freeMemory() / 1024 + " KB\n" + "Total Memory: " + runtime.totalMemory() / 1024 + " KB"); } 
            else if (MOD == PROCESS) { preview.deleteAll(); for (Enumeration keys = sys.keys(); keys.hasMoreElements();) { String PID = (String) keys.nextElement(), name = (String) ((Hashtable) sys.get(PID)).get("name"); if (pfilter.equals("") || name.indexOf(pfilter) != -1) { preview.append(PID + "\t" + name, null); } } }
        }

        private void back() { if (sys.containsKey(PID) && !asked) { confirm = new Alert("Background Process", "Keep this process running in background?", null, AlertType.WARNING); confirm.addCommand(YES = new Command("Yes", Command.OK, 1)); confirm.addCommand(NO = new Command("No", Command.BACK, 1)); confirm.setCommandListener(this); asked = true; display.setCurrent(confirm); } else { processCommand("xterm"); } }
        private void goback() { processCommand("xterm", enable, id, pid, stdout, scope); }

        public static String passwd() { return loadRMS("OpenRMS", 2); }
        private String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(result) < 0) { result = cur; } } v.removeElement(result); return result; } 
    }
    // |
    // MIDlet Shell
    public int processCommand(String command, boolean enable, int id, String pid, Object stdout, Hashtable scope) {
        command = command.startsWith("exec") ? command.trim() : env(command.trim());
        String mainCommand = getCommand(command), argument = getpattern(getArgument(command));
        String[] args = splitArgs(getArgument(command));

        if (username.equals("root")) { id = 0; } if (scope == null) { scope = globals; }
        if (command.endsWith("&")) { return processCommand("bg " + command.substring(0, command.length() - 1), enable, id, pid, stdout, scope); }

        if (mainCommand.equals("") || mainCommand.equals("true") || mainCommand.equals("#")) { }
        else if (aliases.containsKey(mainCommand)) { return processCommand(((String) aliases.get(mainCommand)) + " " + argument, enable, id, pid, stdout, scope); }
        else if (classpath && file("/bin/" + mainCommand) && !mainCommand.equals("sh") && !mainCommand.equals("lua") && !mainCommand.startsWith(".")) { return processCommand(". /bin/" + command, enable, id, pid, stdout, scope); }

        else if (mainCommand.equals("alias")) { if (argument.equals("")) { for (Enumeration KEYS = aliases.keys(); KEYS.hasMoreElements();) { String KEY = (String) KEYS.nextElement(), VALUE = (String) aliases.get(KEY); if (!KEY.equals("xterm") && !VALUE.equals("")) { print("alias " + KEY + "='" + VALUE.trim() + "'", stdout); } } } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { for (int i = 0; i < args.length; i++) { if (aliases.containsKey(args[i])) { print("alias " + args[i] + "='" + (String) aliases.get(args[i]) + "'", stdout); } else { print("alias: " + argument + ": not found", stdout); return 127; } } } else { aliases.put(argument.substring(0, INDEX).trim(), getpattern(argument.substring(INDEX + 1).trim())); } } }  
        else if (mainCommand.equals("unalias")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { if (aliases.containsKey(args[i])) { aliases.remove(args[i]); } else { print("unalias: "+ args[i] + ": not found", stdout); return 127; } } } }
        // |
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { for (int i = 0; i < args.length; i++) { attributes.put(args[i], ""); } } else { attributes.put(argument.substring(0, INDEX).trim(), getpattern(argument.substring(INDEX + 1).trim())); } } } 
        else if (mainCommand.equals("unset")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { if (attributes.containsKey(args[i])) { attributes.remove(args[i]); } else { } } } }
        else if (mainCommand.equals("export")) { return processCommand(argument.equals("") ? "env" : "set " + argument, false, id, pid, stdout, scope); }
        else if (mainCommand.equals("env")) { if (argument.equals("")) { for (Enumeration KEYS = attributes.keys(); KEYS.hasMoreElements();) { String KEY = (String) KEYS.nextElement(), VALUE = (String) attributes.get(KEY); if (!KEY.equals("OUTPUT") && !VALUE.equals("")) { print(KEY + "=" + VALUE.trim(), stdout); } } } else { for (int i = 0; i < args.length; i++) { if (attributes.containsKey(args[i])) { print(args[i] + "=" + (String) attributes.get(args[i]), stdout); } else { print("env: " + args[i] + ": not found", stdout); return 127; } } } }

        else if (mainCommand.equals("if") || mainCommand.equals("for") || mainCommand.equals("case")) { return mainCommand.equals("if") ? ifCommand(argument, enable, id, pid, stdout, scope) : mainCommand.equals("for") ? forCommand(argument, enable, id, pid, stdout, scope) : caseCommand(argument, enable, id, pid, stdout, scope); }
        // |
        else if (mainCommand.equals("echo")) { print(argument, stdout); }
        else if (mainCommand.equals("buff")) { stdin.setString(argument); }
        else if (mainCommand.equals("uuid")) { print(generateUUID(), stdout); }
        else if (mainCommand.equals("expr")) { print(expression(argument), stdout); }
        else if (mainCommand.equals("basename")) { print(basename(argument), stdout); }
        else if (mainCommand.equals("getopt")) { print(getArgument(argument), stdout); }
        else if (mainCommand.equals("trim")) { this.stdout.setText(this.stdout.getText().trim()); }
        else if (mainCommand.equals("locale")) { print((String) attributes.get("LOCALE"), stdout); }
        else if (mainCommand.equals("date")) { print(new java.util.Date().toString(), stdout); }
        else if (mainCommand.equals("clear")) { if (argument.equals("")) { this.stdout.setText(""); } else { for (int i = 0; i < args.length; i++) { if (args[i].equals("stdout")) { this.stdout.setText(""); } else if (args[i].equals("stdin")) { stdin.setString(""); } else if (args[i].equals("history")) { getprocess("1").put("history", new Vector()); } else if (args[i].equals("cache")) { cache = new Hashtable(); } else if (args[i].equals("logs")) { logs = ""; } else { print("clear: " + args[i] + ": not found", stdout); return 127; } } } }
        else if (mainCommand.equals("seed")) { try { print("" + random.nextInt(Integer.parseInt(argument)) + "", stdout); } catch (NumberFormatException e) { print(getCatch(e), stdout); return 2; } }
        // | (Device API)
        else if (mainCommand.equals("call")) { if (argument.equals("")) { } else { try { platformRequest("tel:" + argument); } catch (Exception e) { } } }
        else if (mainCommand.equals("open")) { if (argument.equals("")) { } else { try { platformRequest(argument); } catch (Exception e) { print("open: " + argument + ": not found", stdout); return 127; } } }
        // | (PushRegistry & Wireless)
        else if (mainCommand.equals("prg")) { if (argument.equals("")) { argument = "5"; } try { PushRegistry.registerAlarm(getArgument(argument).equals("") ? "OpenTTY" : getArgument(argument), System.currentTimeMillis() + Integer.parseInt(getCommand(argument)) * 1000); } catch (ClassNotFoundException e) { print("prg: " + getArgument(argument) + ": not found", stdout); return 127; } catch (NumberFormatException e) { print(getCatch(e), stdout); return 2; } catch (Exception e) { print(getCatch(e), stdout); return 3; } }
        else if (mainCommand.equals("wrl")) { String ID = System.getProperty("wireless.messaging.sms.smsc"); if (ID == null) { print("Unsupported API", stdout); return 3; } else { print(ID, stdout); } }
        // | (Network)
        else if (mainCommand.equals("who")) { print("PORT\tADDRESS", stdout); Hashtable sessions = (Hashtable) getobject("1", "sessions"); boolean all = argument.indexOf("-a") != -1; for (Enumeration KEYS = sessions.keys(); KEYS.hasMoreElements();) { String PORT = (String) KEYS.nextElement(), ADDR = (String) sessions.get(PORT); if (!all && ADDR.equals("nobody")) { } else { print(PORT + "\t" + ADDR, stdout); } } }
        else if (mainCommand.equals("genip")) { print(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256), stdout); }
        else if (mainCommand.equals("ifconfig")) { if (argument.equals("")) { argument = "1.1.1.1:53"; } try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); print(CONN.getLocalAddress(), stdout); CONN.close(); } catch (Exception e) { print("null", stdout); return 101; } }
        else if (mainCommand.equals("gaddr")) { return GetAddress(argument, id, pid, stdout, scope); }
        else if (mainCommand.equals("netstat")) { 
            if (argument.equals("") || args[0].equals("-a")) {
                print("Active Connections:\nPID\tName\tLocal\tRemote\tState", stdout);
                
                for (Enumeration keys = sys.keys(); keys.hasMoreElements();) {
                    String pid = (String) keys.nextElement();
                    Hashtable proc = (Hashtable) sys.get(pid);
                    String procName = (String) proc.get("name");
                    
                    if (proc.containsKey("socket")) {
                        String state = "ESTABLISHED", localAddr = "N/A", remoteAddr = "N/A";
                        
                        try { if (proc.get("socket") != null) { SocketConnection conn = (SocketConnection) proc.get("socket"); localAddr = conn.getLocalAddress() + ":" + conn.getLocalPort(); remoteAddr = conn.getAddress() + ":" + conn.getLocalPort(); } } 
                        catch (Exception e) { }
                        
                        print(pid + "\t" + procName + "\t" + localAddr + "\t" + remoteAddr + "\t" + state, stdout);
                    }
                }
                
                print("\nActive Servers:\nPID\tType\tPort\tStatus", stdout);
                
                Hashtable sessions = (Hashtable) getobject("1", "sessions");
                for (Enumeration ports = sessions.keys(); ports.hasMoreElements();) {
                    String port = (String) ports.nextElement(), status = sessions.get(port).equals("nobody") ? "LISTENING" : "ESTABLISHED";
                    
                    for (Enumeration pids = sys.keys(); pids.hasMoreElements();) {
                        String pid = (String) pids.nextElement();
                        Hashtable proc = (Hashtable) sys.get(pid);
                        if (proc.get("port") != null && proc.get("port").equals(port)) { print(pid + "\tbind\t" + port + "\t" + status, stdout); }
                    }
                }
            } else {
                int STATUS = 0; 
                try { 
                    HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? "http://" + argument : argument); 
                    CONN.setRequestMethod(HttpConnection.GET); 
                    if (CONN.getResponseCode() == HttpConnection.HTTP_OK) { } 
                    else { STATUS = 101; } CONN.close(); 
                } 
                catch (SecurityException e) { STATUS = 13; } 
                catch (Exception e) { STATUS = 101; } 
                
                print(STATUS == 0 ? "true" : "false", stdout); 
                return STATUS; 
            }
            
        }
        // |
        else if (mainCommand.equals("pong")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); CONN.close(); print("Pong to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms", stdout); } catch (IOException e) { print("Pong to " + argument + " failed: " + getCatch(e), stdout); return 101; } } }
        else if (mainCommand.equals("ping")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); try { HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? "http://" + argument : argument); CONN.setRequestMethod(HttpConnection.GET); int responseCode = CONN.getResponseCode(); CONN.close(); print("Ping to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms", stdout); } catch (IOException e) { print("Ping to " + argument + " failed: " + getCatch(e), stdout); return 101; } } }
        // |
        else if (mainCommand.equals("bind") || mainCommand.equals("nc")) { new MIDletControl(mainCommand, argument, id, stdout, scope); }
        // | (Files)
        else if (mainCommand.equals("pwd")) { print(path, stdout); }
        else if (mainCommand.equals("dir") || mainCommand.equals("history")) { new MIDletControl(mainCommand, id, stdout, scope); }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { mount(getcontent(argument)); } }
        else if (mainCommand.equals("umount")) { fs = new Hashtable(); }
        else if (mainCommand.equals("popd")) { Vector stack = (Vector) getobject("1", "stack"); if (stack.isEmpty()) { print("popd: empty stack", stdout); } else { path = (String) stack.lastElement(); stack.removeElementAt(stack.size() - 1); print(readStack(), stdout); } }
        else if (mainCommand.equals("cd") || mainCommand.equals("pushd")) { 
            String old_pwd = path;
            if (argument.equals("") && mainCommand.equals("cd")) { path = "/home/"; } 
            else if (argument.equals("")) { print(readStack() == null || readStack().length() == 0 ? "pushd: missing directory" : readStack(), stdout); }
            else if (argument.equals("..")) { 
                if (path.equals("/")) { return 0; } 

                int lastSlashIndex = path.lastIndexOf('/', path.endsWith("/") ? path.length() - 2 : path.length() - 1); 
                path = (lastSlashIndex <= 0) ? "/" : path.substring(0, lastSlashIndex + 1); 
            } 
            else { 
                String TARGET = argument.startsWith("/") ? argument : (path.endsWith("/") ? path + argument : path + "/" + argument); 
                if (!TARGET.endsWith("/")) { TARGET += "/"; } 
                if (fs.containsKey(TARGET)) { path = TARGET; } 

                else if (TARGET.startsWith("/mnt/")) { 
                    try { 
                        FileConnection fc = (FileConnection) Connector.open("file:///" + TARGET.substring(5), Connector.READ); 
                        if (fc.exists() && fc.isDirectory()) { path = TARGET; } 
                        else { print(mainCommand + ": " + basename(TARGET) + ": not " + (fc.exists() ? "a directory" : "found"), stdout); return 127; } 

                        fc.close(); 
                    } 
                    catch (IOException e) { 
                        print(mainCommand + ": " + basename(TARGET) + ": " + getCatch(e), stdout); 

                        return 1; 
                    } 
                } 
                else if (TARGET.startsWith("/proc/")) {
                    String[] parts = split(TARGET.substring(6), '/');
                    if (parts.length < 1) { print(mainCommand + ": not found", stdout); return 127; }

                    String pid = parts[0];
                    Hashtable proc = getprocess(pid);
                    Object current = proc;

                    for (int i = 1; i < parts.length; i++) {
                        if (current instanceof Hashtable) {
                            current = ((Hashtable) current).get(parts[i]);

                            if (current == null) { print(mainCommand + ": " + parts[i] + ": not found", stdout); return 127; }
                        } else { print(mainCommand + ": " + parts[i] + ": not a directory", stdout); return 127; }
                    }

                    path = TARGET.endsWith("/") ? TARGET : TARGET + "/";
                }
                else { print(mainCommand + ": " + basename(TARGET) + ": not accessible", stdout); return 127; } 

            } 

            if (mainCommand.equals("pushd")) { ((Vector) getobject("1", "stack")).addElement(old_pwd); print(readStack(), stdout); }
        }
        else if (mainCommand.equals("ls")) { 
            boolean all = false, verbose = false;

            while (true) {
                if (argument.startsWith("-a")) { all = true; } 
                else if (argument.startsWith("-v")) { verbose = true; }
                else { break; }

                argument = argument.substring(2).trim();
            }

            String PWD = argument.equals("") ? path : argument; 
            if (!PWD.startsWith("/")) { PWD = path + PWD; } 
            if (!PWD.endsWith("/")) { PWD += "/"; }

            Vector BUFFER = new Vector();

            try { 
                if (PWD.equals("/tmp/")) {
                    for (Enumeration KEYS = tmp.keys(); KEYS.hasMoreElements();) {
                        String KEY = (String) KEYS.nextElement();
                        if ((all || !KEY.startsWith(".")) && !BUFFER.contains(KEY)) { BUFFER.addElement(KEY); } 
                    }
                }
                else if (PWD.equals("/mnt/")) { 
                    for (Enumeration ROOTS = FileSystemRegistry.listRoots(); ROOTS.hasMoreElements();) { 
                        String ROOT = (String) ROOTS.nextElement(); if ((all || !ROOT.startsWith(".")) && !BUFFER.contains(ROOT)) { BUFFER.addElement(ROOT); } 
                    }
                } 
                else if (PWD.startsWith("/mnt/")) { 
                    String REALPWD = "file:///" + PWD.substring(5); 
                    if (!REALPWD.endsWith("/")) { REALPWD += "/"; } 
                    FileConnection CONN = (FileConnection) Connector.open(REALPWD, Connector.READ); 
                    for (Enumeration CONTENT = CONN.list(); CONTENT.hasMoreElements();) { 
                        String ITEM = (String) CONTENT.nextElement();
                        if ((all || !ITEM.startsWith(".")) && !BUFFER.contains(ITEM)) {
                            BUFFER.addElement(ITEM); 
                        }
                    } 
                    CONN.close(); 
                } 
                else if (PWD.equals("/bin/") || PWD.equals("/etc/") || PWD.equals("/lib/")) {
                    String content = loadRMS("OpenRMS", PWD.equals("/bin/") ? 3 : PWD.equals("/etc/") ? 5 : 4);
                    int index = 0;

                    while (true) {
                        int start = content.indexOf("[\0BEGIN:", index);
                        if (start == -1) { break; }

                        int end = content.indexOf("\0]", start);
                        if (end == -1) { break; }

                        String filename = content.substring(start + "[\0BEGIN:".length(), end);
                        if (filename.startsWith(".")) { } else { BUFFER.addElement(filename); }

                        index = content.indexOf("[\0END\0]", end);
                        if (index == -1) { break; }

                        index += "[\0END\0]".length();
                    }
                }
                else if (PWD.equals("/home/") && verbose) { 
                    String[] FILES = RecordStore.listRecordStores(); 
                    if (FILES != null) { 
                        for (int i = 0; i < FILES.length; i++) { 
                            String NAME = FILES[i]; 
                            if ((all || !NAME.startsWith(".")) && !BUFFER.contains(NAME)) { BUFFER.addElement(NAME); } 
                        } 
                    } 
                } 
                else if (PWD.equals("/home/")) { return processCommand("dir", false, id, pid, stdout, scope); }
            } catch (IOException e) { } 

            Vector FILES = (Vector) fs.get(PWD); 
            if (FILES != null) { 
                for (int i = 0; i < FILES.size(); i++) { 
                    String file = ((String) FILES.elementAt(i)).trim(); 
                    if (file == null || file.equals("..") || file.equals("/")) { continue; }
                    if ((all || !file.startsWith(".")) && !BUFFER.contains(file) && !BUFFER.contains(file + "/")) { BUFFER.addElement(file); } 
                } 
            } 

            if (!BUFFER.isEmpty()) { 
                String formatted = "";
                for (int i = 0; i < BUFFER.size(); i++) { 
                    String ITEM = (String) BUFFER.elementAt(i); 
                    if (!ITEM.equals("/")) { 
                        formatted += ITEM + (PWD.startsWith("/home/") ? "\n" : "\t"); 
                    } 
                } 
                print(formatted.trim(), stdout); 
            } 
        }
        else if (mainCommand.equals("fdisk")) { return processCommand("ls /mnt/", false, id, pid, stdout, scope); }
        else if (mainCommand.equals("lsblk")) { if (argument.equals("") || argument.equals("-x")) { print(replace("MIDlet.RMS.Storage", ".", argument.equals("-x") ? ";" : "\t"), stdout); } else { print("lsblk: " + argument + ": not found", stdout); return 127; } }
        // | 
        else if (mainCommand.equals("rm")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { int STATUS = deleteFile(args[i], id, stdout); if (STATUS != 0) { return STATUS; } } } }
        else if (mainCommand.equals("install")) { if (argument.equals("")) { } else { return write(argument, buffer, id); } }
        else if (mainCommand.equals("touch")) { if (argument.equals("")) { buffer = ""; } else { for (int i = 0; i < args.length; i++) { int STATUS = write(argument, "", id); if (STATUS != 0) { return STATUS; } } } }
        else if (mainCommand.equals("mkdir")) { if (argument.equals("")) { } else { argument = argument.endsWith("/") ? argument : argument + "/"; argument = argument.startsWith("/") ? argument : path + argument; if (argument.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.mkdir(); CONN.close(); } else { print("mkdir: " + basename(argument) + ": found", stdout); } CONN.close(); } catch (Exception e) { print(getCatch(e), stdout); return (e instanceof SecurityException) ? 13 : 1; } } else if (argument.startsWith("/home/") || argument.startsWith("/tmp/")) { print("Unsupported API", stdout); return 3; } else if (argument.startsWith("/")) { print("read-only storage", stdout); return 5; } } }
        else if (mainCommand.equals("cp")) {
            if (argument.equals("")) { print("cp: missing [origin]", stdout); } 
            else {
                try {
                    String origin = args[0], target = (args.length > 1 && !args[1].equals("")) ? args[1] : origin + "-copy";

                    InputStream in = getInputStream(origin.startsWith("/") ? origin : path + origin);
                    if (in == null) { print("cp: cannot open " + origin, stdout); return 1; }

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] tmpBuf = new byte[4096];
                    int len;
                    while ((len = in.read(tmpBuf)) != -1) { buffer.write(tmpBuf, 0, len); }
                    in.close();

                    return write(target.startsWith("/") ? target : path + target, buffer.toByteArray(), id);
                } catch (Exception e) { print("cp: " + getCatch(e), stdout); return e instanceof SecurityException ? 13 : 1; }
            }
        }
        else if (mainCommand.equals("cache")) { if (argument.equals("")) { print("Cache: " + (useCache ? "enabled (" + cache.size() + " items)" : "disabled"), stdout); } else if (argument.equals("on")) { useCache = true; } else if (argument.equals("off")) { useCache = false; cache = new Hashtable(); } else if (argument.equals("clear")) { cache = new Hashtable(); } else { print("cache: " + args[0] + ": not found", stdout); return 127; } } 
        else if (mainCommand.equals("rmsfix")) {
            if (argument.equals("")) { }
            else if (id != 0) { print("Permission denied!", stdout); return 13; }
            else if (args[0].equals("read")) { if (args.length < 2) { return 2; } else { print(loadRMS("OpenRMS", args[1].equals("/bin/") ? 3 : args[1].equals("/etc/") ? 5 : 4), stdout); } }
            else if (args[0].equals("swap")) { if (args.length < 3) { return 2; } else { write(args[2].startsWith("/") ? args[2] : path + args[2], loadRMS("OpenRMS", args[1].equals("/bin/") ? 3 : 4), id); } }
            else if (args[0].startsWith("/")) {
                args[0] = args[0].endsWith("/") ? args[0] : args[0] + "/";
                if (args[0].equals("/bin/") || args[0].equals("/etc/") || args[0].equals("/lib/")) { return writeRMS("OpenRMS", new byte[0], args[0].equals("/bin/") ? 3 : args[1].equals("/etc/") ? 5 : 4); }
                else { print("rmsfix: " + args[0] + ": not found", stdout); return 127; }
            }
            else { print("rmsfix: " + args[0] + ": not found", stdout); return 127; }
        }
        // |
        else if (mainCommand.equals("add")) { buffer = buffer.equals("") ? argument : buffer + "\n" + argument; }
        else if (mainCommand.equals("getty")) { buffer = stdout instanceof StringItem ? ((StringItem) stdout).getText() : stdout instanceof StringBuffer ? ((StringBuffer) stdout).toString() : stdout instanceof String ? read((String) stdout) : ""; }
        else if (mainCommand.equals("du")) { 
            if (argument.equals("")) { } 
            else { 
                try {
                    InputStream in = getInputStream(argument); 
                    if (in == null) { print("du: " + basename(argument) + ": not found", stdout); return 127; } 
                    else { print("" + in.available(), stdout); } 
                } catch (Exception e) {
                    print("du: " + getCatch(e), stdout);
                    return e instanceof SecurityException ? 13 : 1;
                }
            } 
        }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { print(buffer, stdout); } else { for (int i = 0; i < args.length; i++) { print(getcontent(args[i]), stdout); } } }
        else if (mainCommand.equals("get")) { buffer = argument.equals("") ? "" : getcontent(argument); }
        else if (mainCommand.equals("hash")) { if (argument.equals("")) { } else { print("" + getcontent(argument).hashCode(), stdout); } }
        else if (mainCommand.equals("read")) { if (argument.equals("") || args.length < 2) { return 2; } else { attributes.put(args[0], getcontent(args[1])); } }
        else if (mainCommand.equals("grep")) { if (argument.equals("") || args.length < 2) { return 2; } else { print(getcontent(args[1]).indexOf(args[0]) != -1 ? "true" : "false", stdout); } }
        else if (mainCommand.equals("find")) { if (argument.equals("") || args.length < 2) { return 2; } else { String VALUE = (String) parseProperties(getcontent(args[1])).get(args[0]); print(VALUE != null ? VALUE : "null", stdout); } }
        else if (mainCommand.equals("head")) { if (argument.equals("")) { } else { String CONTENT = getcontent(args[0]); String[] LINES = split(CONTENT, '\n'); int COUNT = Math.min(args.length > 1 ? getNumber(args[1], 10, null) : 10, LINES.length); for (int i = 0; i < COUNT; i++) { print(LINES[i], stdout); } } }
        else if (mainCommand.equals("tail")) { if (argument.equals("")) { } else { String CONTENT = getcontent(args[0]); String[] LINES = split(CONTENT, '\n'); int COUNT = args.length > 1 ? getNumber(args[1], 10, null) : 10; COUNT = Math.max(0, LINES.length - COUNT); for (int i = COUNT; i < LINES.length; i++) { print(LINES[i], stdout); } } }
        else if (mainCommand.equals("diff")) { if (argument.equals("") || args.length < 2) { return 2; } else { String[] LINES1 = split(getcontent(args[0]), '\n'), LINES2 = split(getcontent(args[1]), '\n'); int MAX_RANGE = Math.max(LINES1.length, LINES2.length); for (int i = 0; i < MAX_RANGE; i++) { String LINE1 = i < LINES1.length ? LINES1[i] : "", LINE2 = i < LINES2.length ? LINES2[i] : ""; if (!LINE1.equals(LINE2)) { print("--- Line " + (i + 1) + " ---\n< " + LINE1 + "\n" + "> " + LINE2, stdout); } if (i > LINES1.length || i > LINES2.length) { break; } } } }
        else if (mainCommand.equals("wc")) { if (argument.equals("")) { } else { int MODE = args[0].indexOf("-c") != -1 ? 1 : args[0].indexOf("-w") != -1 ? 2 : args[0].indexOf("-l") != -1 ? 3 : 0; if (MODE != 0) { argument = join(args, " ", 1); } String CONTENT = getcontent(argument), FILENAME = basename(argument); int LINES = 0, WORDS = 0, CHARS = CONTENT.length(); String[] LINE_ARRAY = split(CONTENT, '\n'); LINES = LINE_ARRAY.length; for (int i = 0; i < LINE_ARRAY.length; i++) { String[] WORD_ARRAY = split(LINE_ARRAY[i], ' '); for (int j = 0; j < WORD_ARRAY.length; j++) { if (!WORD_ARRAY[j].trim().equals("")) { WORDS++; } } } print(MODE == 0 ? LINES + "\t" + WORDS + "\t" + CHARS + "\t" + FILENAME : MODE == 1 ? CHARS + "\t" + FILENAME : MODE == 2 ? WORDS + "\t" + FILENAME : LINES + "\t" + FILENAME, stdout); } }
        // |
        else if (mainCommand.equals("nano")) { new MIDletControl(argument, id, stdout, scope); }
        else if (mainCommand.equals("view")) { if (argument.equals("")) { } else { viewer(extractTitle(env(argument), xterm.getTitle()), html2text(env(argument))); } }
        else if (mainCommand.equals("html")) { String content = argument.equals("") ? buffer : getcontent(argument); viewer(extractTitle(env(content), "HTML Viewer"), html2text(env(content))); }
        // |
        else if (mainCommand.equals("audio")) { return audio(argument, id, pid, stdout, scope); }
        else if (mainCommand.equals("java")) { return java(argument, id, pid, stdout, scope); }
        else if (mainCommand.equals("chmod")) { 
            if (argument.equals("")) { } 
            else if (argument.equals("*")) { return processCommand("chmod http socket file prg", false, id, pid, stdout, scope); }
            else { 
                Hashtable NODES = parseProperties("http=javax.microedition.io.Connector.http\nsocket=javax.microedition.io.Connector.socket\nfile=javax.microedition.io.Connector.file\nprg=javax.microedition.io.PushRegistry"); 
                
                int STATUS = 0; 
                for (int i = 0; i < args.length; i++) {
                    String NODE = (String) NODES.get(args[i]);
                    
                    if (NODES.containsKey(args[i])) { 
                        try { 
                            if (args[i].equals("http")) { ((HttpConnection) Connector.open("http://google.com")).close(); } 
                            else if (args[i].equals("socket")) { ((SocketConnection) Connector.open(env("socket://" + ((attributes.containsKey("REPO") && !(env("$REPO").equals("") && !(env("$REPO").equals("$REPO"))) ? "$REPO" : "1.1.1.1:53"))))).close(); } 
                            else if (args[i].equals("file")) { FileSystemRegistry.listRoots(); } 
                            else if (args[i].equals("prg")) { PushRegistry.registerAlarm(getClass().getName(), System.currentTimeMillis() + 1000); } 
                        } 
                        catch (Exception e) { STATUS = (e instanceof SecurityException) ? 13 : (e instanceof IOException) ? 1 : 3; } 
                    } 
                    else { print("chmod: " + args[i] + ": not found", stdout); return 127; } 
                    
                    if (STATUS == 0) { MIDletLogs("add info Permission '" + NODE + "' granted", id, stdout); } 
                    else if (STATUS == 1) { MIDletLogs("add debug Permission '" + NODE + "' granted with exceptions", id, stdout); } 
                    else if (STATUS == 13) { MIDletLogs("add error Permission '" + NODE + "' denied", id, stdout); } 
                    else if (STATUS == 3) { MIDletLogs("add warn Unsupported API '" + NODE + "'", id, stdout); } 
                    
                    if (STATUS > 1) { break; } 
                }
                return STATUS; 
            } 
        }
        else if (mainCommand.equals("which")) { if (argument.equals("")) { } else { print(aliases.containsKey(argument) ? "alias" : file("/bin/" + argument) ? "application" : "", stdout); } }
        // |
        else if (mainCommand.equals("throw")) { Thread.currentThread().interrupt(); }
        else if (mainCommand.equals("mmspt") || mainCommand.equals("chrt")) { if (argument.equals("")) { print(getThreadName(Thread.currentThread()), stdout); } else if (argument.equals("priority")) { print("" + Thread.currentThread().getPriority(), stdout); } else { int value = getNumber(argument, Thread.NORM_PRIORITY, null); if (value > 10 || value < 1) { return 2; } else { Thread.currentThread().setPriority(value); } } }
        else if (mainCommand.equals("bg")) { if (argument.equals("")) { } else { new MIDletControl("Background", argument, enable, id, stdout, scope); } }
        // |
        else if (mainCommand.equals("gc")) { System.gc(); }
        else if (mainCommand.equals("top")) { return kernel(argument, id, stdout, scope);  }
        // |
        else if (mainCommand.equals("start") || mainCommand.equals("stop") || mainCommand.equals("kill")) { for (int i = 0; i < args.length; i++) { int STATUS = mainCommand.equals("start") ? start(args[i], id, genpid(), null, stdout, scope) : mainCommand.equals("stop") ? stop(args[i], id, stdout, scope) : kill(args[i], true, id, stdout, scope); if (STATUS != 0) { return STATUS; } } } 
        else if (mainCommand.equals("ps")) { print("PID\tPROCESS", stdout); for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); print(PID + "\t" + (String) ((Hashtable) sys.get(PID)).get("name"), stdout); } }
        // |
        else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { if (argument.equals("")) { } else { return processCommand(argument, false, id, pid, stdout, scope); } }
        else if (mainCommand.equals("bruteforce")) { String PID = genpid(); start("bruteforce", id, PID, null, stdout, scope); while (sys.containsKey(PID)) { int STATUS = processCommand(argument, enable, id, PID, stdout, scope); if (STATUS != 0) { sys.remove(PID); return STATUS; } } }
        else if (mainCommand.equals("cron")) { if (argument.equals("")) { } else { return processCommand("execute sleep " + getCommand(argument) + "; " + getArgument(argument), enable, id, pid, stdout, scope); } }
        else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (Exception e) { print(getCatch(e), stdout); return 2; } } }
        else if (mainCommand.equals("time")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); int STATUS = processCommand(argument, enable, id, pid, stdout, scope); print("at " + (System.currentTimeMillis() - START) + "ms", stdout); return STATUS; } }
        else if (mainCommand.equals("exec")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { int STATUS = processCommand(args[i].trim(), enable, id, pid, stdout, scope); if (STATUS != 0) { return STATUS; } } } }
        // |
        else if (mainCommand.equals("xterm")) { display.setCurrent(xterm); }
        else if (mainCommand.equals("x11")) { return xcli(argument, id, stdout, scope); }
        else if (mainCommand.equals("warn")) { return warn(xterm.getTitle(), argument); }
        else if (mainCommand.equals("title")) { xterm.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument.equals("hide") ? null : argument); }
        else if (mainCommand.equals("tick")) { if (argument.equals("label")) { print(display.getCurrent().getTicker().getString(), stdout); } else { return xcli("tick " + argument, id, stdout, scope); } }
        // |
        else if (mainCommand.equals("log")) { return MIDletLogs(argument, id, stdout); }
        else if (mainCommand.equals("logcat")) { print(logs, stdout); }
        // |
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { print(id == 0 ? "root" : username, stdout); }
        else if (mainCommand.equals("sudo")) { if (argument.equals("")) { } else if (id == 0) { return processCommand(argument, enable, id, pid, stdout, scope); } else { new MIDletControl(argument, enable, pid, stdout, scope); } }
        else if (mainCommand.equals("su")) { if (id == 0) { username = username.equals("root") ? read("/home/OpenRMS") : "root"; return processCommand(". /bin/sh", false, id, pid, stdout, scope); } else { print("Permission denied!", stdout); return 13; } }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { return argument.equals("") ? processCommand(". /bin/sh", false, id, pid, stdout, scope) : runScript(argument, id, pid, stdout, scope); }
        else if (mainCommand.equals("id")) { String ID = argument.equals("") ? String.valueOf(id) : argument.equals("root") ? "0" : argument.equals(read("/home/OpenRMS")) ? "1000" : null; if (ID == null) { print("id: '" + argument + "': no such user", stdout); return 127; } print(ID, stdout); }
        else if (mainCommand.equals("passwd")) { if (argument.equals("")) { } else if (id == 0) { writeRMS("OpenRMS", argument.getBytes(), 2); } else { print("Permission denied!", stdout); return 13; } }
        else if (mainCommand.equals("logout")) { if (read("/home/OpenRMS").equals(username)) { if (id == 0) { writeRMS("/home/OpenRMS", "".getBytes(), id); destroyApp(false); } else { print("Permission denied!", stdout); return 13; } } else { username = read("/home/OpenRMS"); return processCommand(". /bin/sh", false, id, pid, stdout, scope); } }
        else if (mainCommand.equals("exit")) { if (read("/home/OpenRMS").equals(username)) { if (pid.equals("1")) { destroyApp(false); } else { return 254; } } else { username = read("/home/OpenRMS"); return processCommand(". /bin/sh", false, id, pid, stdout, scope); } }
        else if (mainCommand.equals("quit")) { destroyApp(false); }
        // |
        else if (mainCommand.equals("pkg")) { print(argument.equals("") ? getAppProperty("MIDlet-Name") : argument.startsWith("/") ? System.getProperty(replace(argument, "/", "")) : getAppProperty(argument), stdout); }
        else if (mainCommand.equals("uname")) { String INFO = argument.equals("") || argument.equals("-i") ? "$TYPE" : argument.equals("-a") || argument.equals("--all") ? "$TYPE (OpenTTY $VERSION) main/$RELEASE " + build + " - $CONFIG $PROFILE" : argument.equals("-r") || argument.equals("--release") ? "$VERSION" : argument.equals("-v") || argument.equals("--build") ? build : argument.equals("-s") ? "J2ME" : argument.equals("-m") ? "$PROFILE" : argument.equals("-p") ? "$CONFIG" : argument.equals("-n") ? "$HOSTNAME" : null; if (INFO == null) { print("uname: " + argument + ": not found", stdout); return 127; } else { print(env(INFO), stdout); } }
        else if (mainCommand.equals("hostname")) { return processCommand(argument.equals("") ? "echo $HOSTNAME" : "set HOSTNAME=" + getCommand(argument), false, id, pid, stdout, scope); }
        else if (mainCommand.equals("hostid")) { String DATA = env("$TYPE$CONFIG$PROFILE"); int HASH = 7; for (int i = 0; i < DATA.length(); i++) { HASH = HASH * 31 + DATA.charAt(i); } print(Integer.toHexString(HASH).toLowerCase(), stdout); }
        // |
        else if (mainCommand.equals("tty")) { print((String) attributes.get("TTY"), stdout); }
        else if (mainCommand.equals("ttysize")) { print((stdout instanceof StringItem ? ((StringItem) stdout).getText().length() : stdout instanceof StringBuffer ? ((StringBuffer) stdout).toString().length() : stdout instanceof String ? read((String) stdout).length() : -1) + " B", stdout); }
        else if (mainCommand.equals("stty")) {
            if (argument.equals("")) { print("" + TTY_MAX_LEN, stdout); }
            else {
                String source = argument;
                if (argument.indexOf("=") == -1) { source = getcontent(argument); }

                Hashtable TTY = parseProperties(source);

                if (TTY.containsKey("max-length")) { try { TTY_MAX_LEN = Integer.parseInt((String) TTY.get("max-length")); } catch (Exception e) { destroyApp(false); } }
                if (TTY.containsKey("classpath")) { classpath = ((String) TTY.get("classpath")).equals("true"); }
            }
        }
        // |
        else if (mainCommand.equals("about")) { warn("OpenTTY", env("OpenTTY $VERSION\n(C) 2025 - Mr Lima")); }
        else if (mainCommand.equals("import")) { return importScript(getcontent(argument), id, stdout, scope); }
        // |
        else if (mainCommand.equals("eval")) { if (argument.equals("")) { } else { print("" + processCommand(argument, enable, id, pid, stdout, scope), stdout); } }
        else if (mainCommand.equals("catch")) { if (argument.equals("")) { } else { try { processCommand(argument, enable, id, pid, stdout, scope); } catch (Throwable e) { print(getCatch(e), stdout); } } }
        else if (mainCommand.equals("false")) { return 255; }
        // |
        else if (mainCommand.equals("lua")) { 
            if (javaClass("Lua") == 0) { 
                Lua lua = new Lua(this, id, stdout, scope); 

                Hashtable arg = new Hashtable();
                String source, code;
                if (argument.equals("")) { source = "nano"; code = buffer; arg.put(new Double(0), "nano"); } 
                else if (args[0].equals("-e")) { source = "stdin"; code = argument.substring(3).trim(); arg.put(new Double(0), "/dev/stdin"); } 
                else { source = args[0]; code = getcontent(source); arg.put(new Double(0), source); for (int i = 1; i < args.length; i++) { arg.put(new Double(i), args[i]); } }
                
                return (Integer) lua.run(source, code, arg).get("status"); 
            } 
            else { print("This MIDlet Build don't have Lua", stdout); return 3; } 
        }
        else if (mainCommand.equals("svchost")) {
            if (argument.equals("")) { }
            else if (argument.equals("set")) { print("svchost: set: in dev", stdout); }
            else {
                if (args.length < 2) { print("svchost [pid] [request]", stdout); return 2; }
                else if (sys.containsKey(args[0])) {
                    Hashtable proc = (Hashtable) sys.get(args[0]);
                    if (proc.containsKey("lua") && proc.containsKey("handler")) {
                        Lua lua = (Lua) proc.get("lua");
                        Vector arg = new Vector(); arg.addElement(args[1]); arg.addElement("shell"); arg.addElement("1"); arg.addElement(id);
                        Object response = null;

                        try { response = ((Lua.LuaFunction) proc.get("handler")).call(arg); }
                        catch (Exception e) { print(getCatch(e), stdout); return 1; } 
                        catch (Error e) { if (e.getMessage() != null) { print(e.getMessage(), stdout); } return lua.status; }
                    } else { print("svchost: " + args[0] + ": not a service", stdout); return 2; }
                } else { print("svchost: " + args[0] + ": not found", stdout); return 127; }
            }
        }
        else if (mainCommand.equals("uptime")) { print("" + (System.currentTimeMillis() - uptime), stdout); }
        // |
        else if (mainCommand.equals("@exec")) { commandAction(EXECUTE, display.getCurrent()); }
        else if (mainCommand.equals("@alert")) { display.vibrate(argument.equals("") ? 500 : getNumber(argument, 0, stdout) * 100); }
        else if (mainCommand.startsWith("@")) { print("mod: " + mainCommand.substring(1) + ": not found", stdout); }
        // |
        else if (mainCommand.equals("!")) { print(env("main/$RELEASE"), stdout); }
        else if (mainCommand.equals("!!")) { stdin.setString((argument.equals("") ? "" : argument + " ") + getLastHistory()); } 
        else if (mainCommand.equals(".")) { return run(argument, args, id, pid, stdout, scope); }
        // |
        else { print(mainCommand + ": not found", stdout); return 127; }

        return 0;
    }
    // |
    // String Utils
    public String getCommand(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return text; } else { return text.substring(0, spaceIndex); } }
    public String getArgument(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return text.substring(spaceIndex + 1).trim(); } }
    // |
    public String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    public String env(String text, Hashtable scope) { if (scope != null) { for (Enumeration keys = scope.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) scope.get(key)); } } return env(text); }
    public String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", xterm.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) attributes.get(key)); } text = replace(text, "$.", "$"); return escape(text); }
    public String escape(String text) { text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); text = replace(text, "\\b", "\b"); text = replace(text, "\\\\", "\\"); text = replace(text, "\\.", "\\"); return text; }
    public String getCatch(Throwable e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : message; }
    // |
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String dirname(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(0, lastSlashIndex + 1); }
    private String expression(String expr) { char[] tokens = expr.toCharArray(); double[] vals = new double[32]; char[] ops = new char[32]; int valTop = -1, opTop = -1; int i = 0, len = tokens.length; while (i < len) { char c = tokens[i]; if (c == ' ') { i++; continue; } if (c >= '0' && c <= '9') { double num = 0; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { num = num * 10 + (tokens[i++] - '0'); } if (i < len && tokens[i] == '.') { i++; double frac = 0, div = 10; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { frac += (tokens[i++] - '0') / div; div *= 10; } num += frac; } vals[++valTop] = num; } else if (c == '(') { ops[++opTop] = c; i++; } else if (c == ')') { while (opTop >= 0 && ops[opTop] != '(') { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } opTop--; i++; } else if (c == '+' || c == '-' || c == '*' || c == '/') { while (opTop >= 0 && prec(ops[opTop]) >= prec(c)) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } ops[++opTop] = c; i++; } else { return "expr: invalid char '" + c + "'"; } } while (opTop >= 0) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } double result = vals[valTop]; return ((int) result == result) ? String.valueOf((int) result) : String.valueOf(result); } private int prec(char op) { if (op == '+' || op == '-') return 1; if (op == '*' || op == '/') return 2; return 0; } private double applyOpSimple(char op, double a, double b) { if (op == '+') return a + b; if (op == '-') return a - b; if (op == '*') return a * b; if (op == '/') return b == 0 ? 0 : a / b; return 0; }
    private String generateUUID() { String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + random.nextInt(4))); } else { uuid.append(chars.charAt(random.nextInt(16))); } } return uuid.toString(); }
    // | 
    private String extractTitle(String htmlContent, String fallback) { return extractTag(htmlContent, "title", fallback); }
    private String extractTag(String htmlContent, String tag, String fallback) { String startTag = "<" + tag + ">", endTag = "</" + tag + ">"; int start = htmlContent.indexOf(startTag), end = htmlContent.indexOf(endTag); if (start != -1 && end != -1 && end > start) { return htmlContent.substring(start + startTag.length(), end).trim(); } else { return fallback; } }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }
    // |
    public String getcontent(String file) { return file.startsWith("/") ? read(file) : read(path + file); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }
    // | (Arrays)
    public String join(String[] array, String spacer, int start) { if (array == null || array.length == 0 || start >= array.length) { return ""; } StringBuffer sb = new StringBuffer(); for (int i = start; i < array.length; i++) { sb.append(array[i]).append(spacer); } return sb.toString().trim(); }
    private int indexOf(String key, String[] array) { for (int i = 0; i < array.length; i++) { if (array[i].equals(key)) { return i; } } return -1; }
    public String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    public String[] splitArgs(String content) { Vector args = new Vector(); boolean inQuotes = false; int start = 0; for (int i = 0; i < content.length(); i++) { char c = content.charAt(i); if (c == '"') { inQuotes = !inQuotes; continue; } if (!inQuotes && c == ' ') { if (i > start) { args.addElement(getpattern(content.substring(start, i))); } start = i + 1; } } if (start < content.length()) { args.addElement(getpattern(content.substring(start))); } String[] result = new String[args.size()]; args.copyInto(result); return result; }
    // | (Converting String on Map)
    public Hashtable parseProperties(String text) { if (text == null) { return new Hashtable(); } Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (line.startsWith("#")) { } else { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { properties.put(line.substring(0, equalIndex).trim(), getpattern(line.substring(equalIndex + 1).trim())); } } } return properties; }
    // | (String <> Number)
    public int getNumber(String s, int fallback, Object stdout) { try { return Integer.valueOf(s); } catch (Exception e) { if (stdout != null) { print(getCatch(e), stdout); } return fallback; } }
    public Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    
    // Logging Manager
    public int MIDletLogs(String command, int id, Object stdout) { 
        command = env(command.trim()); 
        String mainCommand = getCommand(command), argument = getArgument(command); 
        
        if (mainCommand.equals("")) { } 
        else if (mainCommand.equals("clear")) { logs = ""; } 
        else if (mainCommand.equals("swap")) { write(argument.equals("") ? "logs" : argument, logs, 1000); } 
        else if (mainCommand.equals("view")) { viewer(xterm.getTitle(), logs); } 
        else if (mainCommand.equals("add")) { 
            String level = getCommand(argument).toLowerCase(), message = getArgument(argument); 
            
            if (message.equals("")) { }
            else {
                if (level.equals("info") || level.equals("warn") || level.equals("debug") || level.equals("error")) { 
                    logs += "[" + level.toUpperCase() + "] " + split(new java.util.Date().toString(), ' ')[3] + " " + message + "\n"; 
                }
                else { print("log: add: " + level + ": not found", stdout); return 127; } 
            } 
        } else { print("log: " + mainCommand + ": not found", stdout); return 127; } 

        return 0; 
    }

    // Graphics
    public int xcli(String command, int id, Object stdout, Hashtable scope) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);
        String[] args = splitArgs(getArgument(command));

        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2025-05-04\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); }
        else if (mainCommand.equals("version")) { print("X Server " + ((String) attributes.get("XVERSION")), stdout); }
        else if (mainCommand.equals("buffer")) { print(display.getCurrent().getWidth() + "x" + display.getCurrent().getHeight(), stdout); }
        // |
        else if (mainCommand.equals("term")) { display.setCurrent(xterm); }
        else if (mainCommand.equals("init")) { if (argument.equals("/dev/stdin")) { xterm.append(stdin); } else if (argument.equals("/dev/stdout")) { xterm.append(this.stdout); } else { start("x11-wm", id, null, null, stdout, globals); } }
        else if (mainCommand.equals("stop")) { xterm.setTitle(""); xterm.setTicker(null); xterm.deleteAll(); xcli("cmd hide", id, stdout, scope); xterm.removeCommand(EXECUTE); sys.remove("2"); }
        else if (mainCommand.equals("cmd")) {  }
        // |
        else if (mainCommand.equals("title")) { display.getCurrent().setTitle(argument); }
        else if (mainCommand.equals("font")) { this.stdout.setFont(genFont(argument.equals("") ? "default" : argument)); }
        else if (mainCommand.equals("tick")) { Displayable current = display.getCurrent(); current.setTicker(argument.equals("") ? null : new Ticker(argument)); }
        else if (mainCommand.equals("gauge")) { Alert alert = new Alert(xterm.getTitle(), argument, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING)); display.setCurrent(alert); }
        // |
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else if (sys.containsKey("2")) { ((Hashtable) getobject("2", "saves")).put(argument, display.getCurrent()); } else { return 69; } }
        else if (mainCommand.equals("load") || mainCommand.equals("unset")) {
            if (argument.equals("")) { }
            else if (sys.containsKey("2")) {
                Hashtable desktops = (Hashtable) getobject("2", "saves");

                if (desktops.containsKey(argument)) {
                    if (mainCommand.equals("load")) { display.setCurrent((Displayable) desktops.get(argument)); }
                    else { desktops.remove(argument); }
                } 
                else { print("x11: " + mainCommand + ": " + argument + ": not found", stdout); return 127; }
            } 
            else { return 69; }
        }
        else if (mainCommand.equals("import") || mainCommand.equals("export")) { 
            if (argument.equals("")) { } 
            else {
                if (sys.containsKey(argument)) {
                    if (!getobject(argument, "owner").equals(username) && id != 0) { print("Permission denied!", stdout); return 13; }

                    if (mainCommand.equals("import")) {
                        Displayable screen = (Displayable) getobject(argument, "screen");

                        if (screen == null) { print("x11: import: " + argument + ": no screens", stdout); return 69; }
                        else { display.setCurrent(screen); }
                    } 
                    else { ((Hashtable) getprocess(argument)).put("screen", display.getCurrent()); }
                } 
                else { print("x11: " + mainCommand + ": " + argument + ": not found", stdout); return 127; }
            } 
        }
        // |
        else if (mainCommand.equals("item")) { if (argument.equals("") || argument.equals("clear")) { xterm.deleteAll(); xterm.append(this.stdout); xterm.append(stdin); } else { new MIDletControl(xterm, "item", argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), id, stdout, scope); } }

        else { print("x11: " + mainCommand + ": not found", stdout); return 127; }

        return 0;
    }
    // |
    public void print(String message, Object stdout) { print(message, stdout, true); }
    public void print(String message, Object stdout, boolean log) {
        if (log) { attributes.put("OUTPUT", message); }

        if (stdout == null) { }
        else if (stdout instanceof StringItem) { String current = ((StringItem) stdout).getText(), output = current == null || current.length() == 0 ? message : current + "\n" + message; ((StringItem) stdout).setText(TTY_MAX_LEN >= 0 && output.length() > TTY_MAX_LEN ? output.substring(output.length() - TTY_MAX_LEN) : output); }
        else if (stdout instanceof StringBuffer) { ((StringBuffer) stdout).append("\n").append(message); }
        else if (stdout instanceof String) { write((String) stdout, read((String) stdout) + "\n" + message, 1000); }
        else if (stdout instanceof OutputStream) {
            try { ((OutputStream) stdout).write((message + "\n").getBytes());  ((OutputStream) stdout).flush(); } 
            catch (Exception e) { }
        }
    }
    // |
    public int warn(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    public int viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(env(text)); viewer.addCommand(BACK); viewer.setCommandListener(this); display.setCurrent(viewer); return 0; }
    // |
    public Font genFont(String params) { if (params == null || params.length() == 0 || params.equals("default")) { return Font.getDefaultFont(); } int face = Font.FACE_SYSTEM, style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; String[] tokens = split(params, ' '); for (int i = 0; i < tokens.length; i++) { String token = tokens[i].toLowerCase(); if (token.equals("system")) { face = Font.FACE_SYSTEM; } else if (token.equals("monospace")) { face = Font.FACE_MONOSPACE; } else if (token.equals("proportional")) { face = Font.FACE_PROPORTIONAL; } else if (token.equals("bold")) { style |= Font.STYLE_BOLD; } else if (token.equals("italic")) { style |= Font.STYLE_ITALIC; } else if (token.equals("ul") || token.equals("underline") || token.equals("underlined")) { style |= Font.STYLE_UNDERLINED; } else if (token.equals("small")) { size = Font.SIZE_SMALL; } else if (token.equals("medium")) { size = Font.SIZE_MEDIUM; } else if (token.equals("large")) { size = Font.SIZE_LARGE; } } Font f = Font.getFont(face, style, size); return f == null ? Font.getDefaultFont() : f; }

    // Process
    public int start(String app, int id, String pid, Hashtable signals, Object stdout, Hashtable scope) {
        if (app == null || app.length() == 0) { return 2; }

        Hashtable proc = genprocess(app, id, signals);
        Lua lua = null; String source = null;

        if (app.equals("sh") || app.equals("x11-wm")) {
            pid = app.equals("sh") ? "1" : "2"; 
            proc.put("signals", gensignals(app.equals("sh") ? "exit" : "x11 stop")); proc.put("screen", xterm); 

            if (sys.containsKey(pid)) { return 68; }
            else if (app.equals("sh")) { 
                Hashtable sessions = new Hashtable(); sessions.put(pid, "127.0.0.1");

                proc.put("stack", new Vector()); proc.put("history", new Vector()); 
                proc.put("sessions", sessions); proc.put("servers", new Hashtable());
            }
            else if (app.equals("x11-wm")) { 
                proc.put("saves", new Hashtable()); 

                xterm.append(this.stdout); xterm.append(stdin); xterm.addCommand(EXECUTE); 
                xterm.setTitle("OpenTTY " + ((String) attributes.get("VERSION")));
                xterm.setCommandListener(this); 
            }
        }
        else if (app.equals("audio")) { print("usage: audio play [file]", stdout); return 1; }
        else if (app.equals("bruteforce")) { }
        else { 
            while (sys.containsKey(pid) || pid == null || pid.length() == 0) { pid = genpid(); } 
            
            Hashtable db = parseProperties(read("/etc/services")); 
            if (db.containsKey(app)) {
                String[] service = split((String) db.get(app), ',');
                if (service.length < 3) { MIDletLogs("add error Malformed Service '" + app + "'", id, stdout); return 1; }
                else {
                    if (service[0].trim().equals("")) { }
                    else {
                        int STATUS = processCommand(service[0], true, id, pid, stdout, scope);
                        if (STATUS != 0) { return STATUS; }
                    }

                    if (service[1].trim().equals("")) { }
                    else { if (javaClass("Lua") == 0) { lua = new Lua(this, id, stdout, scope); source = service[1].trim(); } else { return 3; } }

                    if (service[2].trim().equals("")) { }
                    else { proc.put("signals", gensignals(service[2])); }
                } 
            }
        } 

        sys.put(pid, proc);

        if (lua != null) {
            Hashtable arg = new Hashtable(); arg.put(new Double(0), source); arg.put(new Double(1), "--deamon");
            Hashtable host = lua.run(pid, app, proc, getcontent(source), arg); 
            int STATUS = (Integer) host.get("status");

            if (STATUS != 0) { sys.remove(pid); return STATUS; }

            if (host.get("object") instanceof Vector) { proc.put("lua", lua); proc.put("handler", ((Vector) host.get("object")).elementAt(0)); } 
            else { MIDletLogs("add warn Service '" + app + "' don't provide a valid handler", id, stdout); }
        }
        return 0;
    }
    public int kill(String pid, boolean log, int id, Object stdout, Hashtable scope) {
        if (pid == null || pid.length() == 0) { return 2; }

        Hashtable proc = (Hashtable) sys.get(pid);
        if (proc == null) { if (log) { print("PID '" + pid + "' not found", stdout); } return 127; }

        String owner = (String) proc.get("owner"), collector = getsignal(pid, "TERM");

        if (!owner.equals(username) && id != 0) { if (log) { print("Permission denied!", stdout); } return 13; }
        if (collector != null) { if (collector.equals("exit")) { destroyApp(true); } processCommand((String) collector, true, id, pid, stdout, scope); }

        sys.remove(pid);
        if (print) { print("Process with PID " + pid + " terminated", stdout); }

        return 0;
    }
    public int stop(String pid, int id, Object stdout, Hashtable scope) { }
    // | (Kernel)
    public int kernel(String command, int id, Object stdout, Hashtable scope) { }
    // | (Generators)
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    public Hashtable genprocess(String name, int id, Hashtable signal) { Hashtable proc = new Hashtable(); proc.put("name", name); proc.put("owner", id == 0 ? "root" : username); if (signal != null) { proc.put("signals", signal); } return proc; }
    public Hashtable gensignals(String collector) {
        Hashtable signal = new Hashtable();

        if (collector != null) { signal.put("TERM", collector); }

        return signal;
    }
    // | (Trackers)
    public Hashtable getprocess(String pid) { return sys.containsKey(pid) ? (Hashtable) sys.get(pid) : null; }
    public Object getobject(String pid, String item) { return sys.containsKey(pid) ? ((Hashtable) sys.get(pid)).get(item) : null; }
    public String getsignal(String pid, Object signal) { if (sys.containsKey(pid)) { Hashtable signals = getobject(pid, "signals"); if (signals != null && signals.containsKey(signal)) { return (String) signals.get(signal); } else { return null; } } else { return null; } } 
    public String getpid(String name) { for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); if (name.equals((String) ((Hashtable) sys.get(PID)).get("name"))) { return PID; } } return null; } 
    
    // | (Renders)
    private String renderJSON(Object obj, int indent) { StringBuffer json = new StringBuffer(); String pad = ""; for (int i = 0; i < indent; i++) { pad += "  "; } if (obj instanceof Hashtable) { Hashtable map = (Hashtable) obj; json.append("{\n"); Enumeration keys = map.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); Object val = map.get(key); json.append(pad + "  \"" + key + "\": " + renderJSON(val, indent + 1)); if (keys.hasMoreElements()) { json.append(","); } json.append("\n"); } json.append(pad + "}"); } else if (obj instanceof Vector) { Vector list = (Vector) obj; json.append("[\n"); for (int i = 0; i < list.size(); i++) { json.append(pad + "  " + renderJSON(list.elementAt(i), indent + 1)); if (i < list.size() - 1) { json.append(","); } json.append("\n"); } json.append(pad + "]"); } else if (obj instanceof String) { String s = (String) obj; s = replace(s, "\n", "\\n"); s = replace(s, "\r", "\\r"); s = replace(s, "\t", "\\t"); json.append("\"" + s + "\""); } else { json.append(String.valueOf(obj)); } return json.toString(); }

    // Connections
    private int GetAddress(String command, int id, String pid, Object stdout, Hashtable scope) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { return processCommand("ifconfig", false, id, pid, stdout, scope); } else { try { DatagramConnection CONN = (DatagramConnection) Connector.open("datagram://" + (argument.equals("") ? "1.1.1.1:53" : argument)); ByteArrayOutputStream OUT = new ByteArrayOutputStream(); OUT.write(0x12); OUT.write(0x34); OUT.write(0x01); OUT.write(0x00); OUT.write(0x00); OUT.write(0x01); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); String[] parts = split(mainCommand, '.'); for (int i = 0; i < parts.length; i++) { OUT.write(parts[i].length()); OUT.write(parts[i].getBytes()); } OUT.write(0x00); OUT.write(0x00); OUT.write(0x01); OUT.write(0x00); OUT.write(0x01); byte[] query = OUT.toByteArray(); Datagram REQUEST = CONN.newDatagram(query, query.length); CONN.send(REQUEST); Datagram RESPONSE = CONN.newDatagram(512); CONN.receive(RESPONSE); CONN.close(); byte[] data = RESPONSE.getData(); if ((data[3] & 0x0F) != 0) { print("not found", stdout); return 127; } int offset = 12; while (data[offset] != 0) { offset++; } offset += 5; if (data[offset + 2] == 0x00 && data[offset + 3] == 0x01) { StringBuffer BUFFER = new StringBuffer(); for (int i = offset + 12; i < offset + 16; i++) { BUFFER.append(data[i] & 0xFF); if (i < offset + 15) BUFFER.append("."); } print(BUFFER.toString(), stdout); } else { print("not found", stdout); return 127; } } catch (IOException e) { print(getCatch(e), stdout); return 1; } } return 0; }

    // File System
    private int mount(String struct) { if (struct == null || struct.length() == 0) { return 2; } String[] lines = split(struct, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i].trim(); int div = line.indexOf('='); if (line.startsWith("#") || line.length() == 0 || div == -1) { continue; } else { String base = line.substring(0, div).trim(); String[] files = split(line.substring(div + 1).trim(), ','); Vector content = new Vector(); content.addElement(".."); for (int j = 0; j < files.length; j++) { if (!content.contains(files[j])) { if (files[j].endsWith("/")) { Vector dir = new Vector(); dir.addElement(".."); fs.put(base + files[j], dir); } content.addElement(files[j]); } } fs.put(base, content); } } return 0; }
    private String readStack() { Vector stack = (Vector) getobject("1", "stack"); StringBuffer sb = new StringBuffer(); sb.append(path); for (int i = 0; i < stack.size(); i++) { sb.append(" ").append((String) stack.elementAt(i)); } return sb.toString(); }
    // | (Read) 
    public InputStream getInputStream(String filename) throws Exception {
        if (filename.startsWith("/home/")) {
            RecordStore rs = null;
            try {
                rs = RecordStore.openRecordStore(filename.substring(6), false);
                if (rs.getNumRecords() > 0) { return new ByteArrayInputStream(rs.getRecord(1)); }
            } finally { if (rs != null) { rs.closeRecordStore(); } }

            return null;
        } 
        else if (filename.startsWith("/mnt/")) { return ((FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ)).openInputStream(); } 
        else if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? new ByteArrayInputStream(((String) tmp.get(filename)).getBytes("UTF-8")) : null; } 
        else {
            if (filename.startsWith("/dev/")) {
                filename = filename.substring(5);
                String content = filename.equals("random") ? String.valueOf(random.nextInt(256)) : filename.equals("stdin") ? stdin.getString() : filename.equals("stdout") ? stdout.getText() : filename.equals("null") ? "\r" : filename.equals("zero") ? "\0" : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/dev/" + filename;
            }
            else if (filename.startsWith("/bin/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/bin/" + filename)) { return new ByteArrayInputStream(((String) cache.get("/bin/" + filename)).getBytes("UTF-8")); }

                String content = read(filename, loadRMS("OpenRMS", 3));
                if (content != null) { if (useCache) { cache.put("/bin/" + filename, content); } return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/bin/" + filename;
            }
            else if (filename.startsWith("/etc/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/etc/" + filename)) { return new ByteArrayInputStream(((String) cache.get("/etc/" + filename)).getBytes("UTF-8")); }

                String content = read(filename, loadRMS("OpenRMS", 5));
                if (content != null) { if (useCache) { cache.put("/etc/" + filename, content); } return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/etc/" + filename;
            }
            else if (filename.startsWith("/lib/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/lib/" + filename)) { return new ByteArrayInputStream(((String) cache.get("/etc/" + filename)).getBytes("UTF-8")); }

                String content = read(filename, loadRMS("OpenRMS", 4));
                if (content != null) { if (useCache) { cache.put("/lib/" + filename, content); } return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/lib/" + filename;
            }

            InputStream is = getClass().getResourceAsStream(filename);
            return is;
        }
    }
    public Image readImg(String filename) { try { InputStream is = getInputStream(filename); Image img = Image.createImage(is); is.close(); return img; } catch (Exception e) { return Image.createImage(16, 16); } }
    public String read(String filename) {
        try {
            if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? (String) tmp.get(filename) : ""; }
            InputStream is = getInputStream(filename);
            if (is == null) { return ""; }
            
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = reader.read()) != -1) { sb.append((char) ch); }
            reader.close();
            is.close();
            
            return filename.startsWith("/home/") ? sb.toString() : env(sb.toString());
        } catch (Exception e) { return ""; }
    }
    public static String loadRMS(String filename, int index) { try { RecordStore RMS = RecordStore.openRecordStore(filename, true); if (RMS.getNumRecords() >= index) { byte[] data = RMS.getRecord(index); if (data != null) { return new String(data); } } if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return ""; }
    // | (Write)
    public int write(String filename, String data, int id) { return write(filename, data.getBytes(), id); }
    public int write(String filename, byte[] data, int id) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.create(); } OutputStream OUT = CONN.openOutputStream(); OUT.write(data); OUT.flush(); OUT.close(); CONN.close(); } catch (Exception e) { return (e instanceof SecurityException) ? 13 : 1; } } 
        else if (filename.startsWith("/home/")) { return writeRMS(filename.substring(6), data, 1, id); } 
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4); filename = filename.substring(5);

            if (filename.equals("")) { return 2; } 
            else if (id != 0) { return 13; }
            else if (filename.equals("sh") || filename.equals("lua")) { return 5; }
            else { if (useCache) { cache.put("/" + base + "/" + filename, new String(data)); } return addFile(filename, new String(data), loadRMS("OpenRMS", base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4), base, id); }
        }
        else if (filename.startsWith("/dev/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else if (filename.equals("null")) { } else if (filename.equals("stdin")) { stdin.setString(new String(data)); } else if (filename.equals("stdout")) { stdout.setText(new String(data)); } else { return 5; } }
        else if (filename.startsWith("/tmp/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else { tmp.put(filename, new String(data)); } }
        else if (filename.startsWith("/")) { return 5; } 
        else { return writeRMS(path + filename, data, id); } 
        
        return 0; 
    }
    public int writeRMS(String filename, byte[] data, int index) { try { RecordStore CONN = RecordStore.openRecordStore(filename, true); while (CONN.getNumRecords() < index) { CONN.addRecord("".getBytes(), 0, 0); } CONN.setRecord(index, data, 0, data.length); if (CONN != null) { CONN.closeRecordStore(); } } catch (Exception e) { return 1; } return 0; }
    // |
    public int deleteFile(String filename, int id, Object stdout) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/home/")) { 
            try { 
                filename = filename.substring(6); 
                if (filename.equals("")) { return 2; }
                if (filename.equals("OpenRMS")) { print("Permission denied!", stdout); return 13; } 
                
                RecordStore.deleteRecordStore(filename); 
            } 
            catch (RecordStoreNotFoundException e) { print("rm: " + filename + ": not found", stdout); return 127; } 
            catch (Exception e) { print(getCatch(e), stdout); return 1; } 
        }
        else if (filename.startsWith("/mnt/")) { 
            try { 
                FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); 
                if (CONN.exists()) { CONN.delete(); } 
                else { print("rm: " + basename(filename) + ": not found", stdout); return 127; } 
                
                CONN.close(); 
            } 
            catch (Exception e) { print(getCatch(e), stdout); return e instanceof SecurityException ? 13 : 1; } 
        }
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4), name = filename.substring(5);
            if (name.equals("")) { return 2; }
            if (id != 0) { print("Permission denied!", stdout); return 13; }

            int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
            String content = loadRMS("OpenRMS", index);
            if (content.indexOf("[\0BEGIN:" + name + "\0]") == -1) { print("read-only storage", stdout); return 5; }

            if (useCache) { cache.remove("/" + base + "/" + name); }
            return writeRMS("OpenRMS", delFile(name, content).getBytes(), index);
        }
        else if (filename.startsWith("/tmp/")) {
            filename = filename.substring(5);
            if (filename.equals("")) { }
            else if (tmp.containsKey(filename)) { tmp.remove(filename); }
            else { print("rm: " + filename + ": not found", stdout); return 127; }
        }
        else if (filename.startsWith("/")) { print("read-only storage", stdout); return 5; } 
        else { return deleteFile(path + filename, id, stdout); } 
        
        return 0; 
    }
    // |
    // | (Archives Structures)
    public int addFile(String filename, String content, String archive, String base) { return writeRMS("OpenRMS", (delFile(filename, archive) + "[\0BEGIN:" + filename + "\0]\n" + content + "\n[\0END\0]\n").getBytes(), base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4); }
    public String delFile(String filename, String content) {
        String startTag = "[\0BEGIN:" + filename + "\0]";
        int start = content.indexOf(startTag);
        if (start == -1) { return content; }

        int end = content.indexOf("[\0END\0]", start);
        if (end == -1) { return content; }

        end += "[\0END\0]".length();
        return content.substring(0, start) + content.substring(end);
    }
    public String read(String filename, String content) {
        String startTag = "[\0BEGIN:" + filename + "\0]";
        int start = content.indexOf(startTag);
        if (start == -1) { return null; }

        start += startTag.length() + 1;
        int end = content.indexOf("[\0END\0]", start);
        if (end == -1) { return null; }

        return content.substring(start, end).trim();
    }
    // |
    private boolean file(String filename) {
        filename = filename.startsWith("/") ? filename : path + filename;

        if (filename.startsWith("/home/")) {
            String[] recordStores = RecordStore.listRecordStores();
            if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(filename.substring(6))) { return true; } } }
        }
        else if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename.substring(5)); }
        else if (filename.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ);
                boolean status = conn.exists(); conn.close(); 
                return status;
            } catch (Exception e) { return false; }
        }
        else if (filename.endsWith("/")) { return fs.containsKey(filename); }
        else { 
            String dir = diirname(filename); 
            if (dir.equals("/bin/") || dir.equals("/etc/") || dir.equals("/lib/")) {
                String content = loadRMS("OpenRMS", dir.equals("/bin/") ? 3 : dir.equals("/etc/") ? 5 : 4);
                if (content.indexOf("[\0BEGIN:" + basename(filename) + "\0]") != -1) { return true; }
            }
            
            return (fs.containsKey((dir)) && ((Vector) fs.get(dir)).indexOf(basename(filename)) != -1); 
        }
        
        return false;
    }
    // |
    public String getMimeType(String filename) { return filename.equals("") ? "" : getExtensionInfo(getExtension(filename))[1]; }
    public String getFileType(String filename) { return filename.equals("") ? "" : getExtensionInfo(getExtension(filename))[2]; }
    public String getExtension(String filename) { if (filename == null) { return ""; } int dot = filename.lastIndexOf('.'); if (dot >= 0 && dot < filename.length() - 1) { return filename.substring(dot).toLowerCase(); } return ""; }
    public String[] getExtensionInfo(String ext) { if (filetypes == null) { filetypes  = parseProperties(getcontent("/res/filetypes")); } String value = (String) filetypes.get(ext.toLowerCase()); if (value == null) { return new String[] { "Unknown", "application/octet-stream", "bin" }; } return split(value, ','); }

    // Audio Manager
    public int audio(String command, int id, String pid, Object stdout, Hashtable scope) { 
        command = env(command.trim()); 
        String mainCommand = getCommand(command), argument = getArgument(command); 

        if (mainCommand.equals("")) { } 
        else if (mainCommand.equals("play")) { 
            if (argument.equals("")) { } 
            else { 
                try {
                    InputStream IN = getInputStream(argument.startsWith("/") ? argument : path + argument);
                                        
                    if (IN == null) { print("audio: " + basename(argument) + ": not found", stdout); return 127; }
                    if (sys.containsKey("3")) { audio("stop", id, pid, stdout, scope); }
                    
                    Player player = Manager.createPlayer(IN, getMimeType(argument)); 
                    player.prefetch(); player.start(); 

                    Hashtable proc = genprocess("audio", id, gensignals("audio stop")); 
                    proc.put("player", player);
                    sys.put("3", proc);
                } 
                catch (Exception e) { print(getCatch(e), stdout); return (e instanceof SecurityException) ? 13 : 1; } 
            }
        }
        else if (mainCommand.equals("volume")) { 
            if (sys.containsKey("3")) { 
                VolumeControl vc = (VolumeControl) ((Player) getprocess("3").get("player")).getControl("VolumeControl"); 
                
                if (argument.equals("")) { print("" + vc.getLevel(), stdout); } 
                else { 
                    try { vc.setLevel(Integer.parseInt(argument)); } 
                    catch (Exception e) { print(getCatch(e), stdout); return 2; } 
                } 
            } else { print("audio: not running.", stdout); return 69; } 
        } 
        else if (mainCommand.equals("stop") || mainCommand.equals("pause") || mainCommand.equals("resume")) { 
            try { 
                if (sys.containsKey("3")) { 
                    Player player = ((Player) getprocess("3").get("player"));

                    if (mainCommand.equals("pause")) { player.stop(); }
                    else if (mainCommand.equals("resume")) { player.start(); }
                    else { if (player != null) { player.stop(); player.close(); } sys.remove("3"); }
                } 
                else { print("audio: not running.", stdout); return 69; } 
            }
            catch (Exception e) { print(getCatch(e), stdout); return 1; } 
        }
        else if (mainCommand.equals("status")) { print(sys.containsKey("3") ? "true" : "false", stdout); } 
        else { print("audio: " + mainCommand + ": not found", stdout); return 127; } 

        return 0; 
    }

    private int ifCommand(String argument, boolean enable, int id, String pid, Object stdout, Hashtable scope) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { print("if (expr) [command]", stdout); return 2; } String EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); String[] PARTS = split(EXPR, ' '); if (PARTS.length == 3) { boolean CONDITION = false; boolean NEGATED = PARTS[1].startsWith("!") && !PARTS[1].equals("!="); if (NEGATED) { PARTS[1] = PARTS[1].substring(1); } Double N1 = getNumber(PARTS[0]), N2 = getNumber(PARTS[2]); if (N1 != null && N2 != null) { if (PARTS[1].equals("==")) { CONDITION = N1.doubleValue() == N2.doubleValue(); } else if (PARTS[1].equals("!=")) { CONDITION = N1.doubleValue() != N2.doubleValue(); } else if (PARTS[1].equals(">")) { CONDITION = N1.doubleValue() > N2.doubleValue(); } else if (PARTS[1].equals("<")) { CONDITION = N1.doubleValue() < N2.doubleValue(); } else if (PARTS[1].equals(">=")) { CONDITION = N1.doubleValue() >= N2.doubleValue(); } else if (PARTS[1].equals("<=")) { CONDITION = N1.doubleValue() <= N2.doubleValue(); } } else { if (PARTS[1].equals("startswith")) { CONDITION = PARTS[0].startsWith(PARTS[2]); } else if (PARTS[1].equals("endswith")) { CONDITION = PARTS[0].endsWith(PARTS[2]); } else if (PARTS[1].equals("contains")) { CONDITION = PARTS[0].indexOf(PARTS[2]) != -1; } else if (PARTS[1].equals("==")) { CONDITION = PARTS[0].equals(PARTS[2]); } else if (PARTS[1].equals("!=")) { CONDITION = !PARTS[0].equals(PARTS[2]); } } if (CONDITION != NEGATED) { return processCommand(CMD, ignore, id); } } else if (PARTS.length == 2) { if (PARTS[0].equals(PARTS[1])) { return processCommand(CMD, ignore, id); } } else if (PARTS.length == 1) { if (!PARTS[0].equals("")) { return processCommand(CMD, enable, id, pid, stdout, scope); } } return 0; }
    private int forCommand(String argument, boolean enable, int id, String pid, Object stdout, Hashtable scope) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String KEY = getCommand(argument), FILE = getcontent(argument.substring(firstParenthesis + 1, lastParenthesis).trim()), CMD = argument.substring(lastParenthesis + 1).trim(); if (KEY.startsWith("(")) { return 2; } if (KEY.startsWith("$")) { KEY = replace(KEY, "$", ""); } String[] LINES = split(FILE, '\n'); for (int i = 0; i < LINES.length; i++) { if (LINES[i] != null || LINES[i].length() != 0) { attributes.put(KEY, LINES[i]); int STATUS = processCommand(CMD, ignore, id, pid, stdout, scope); attributes.remove(KEY); if (STATUS != 0) { return STATUS; } } } return 0; }
    private int caseCommand(String argument, boolean enable, int id, String pid, Object stdout, Hashtable scope) { 
        argument = argument.trim(); 
        int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); 
        if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } 
        
        String METHOD = getCommand(argument), EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); 
        boolean CONDITION = false, NEGATED = METHOD.startsWith("!"); 
        
        if (NEGATED) { METHOD = METHOD.substring(1); } 
        if (METHOD.equals("file")) { CONDITION = file(EXPR); } 
        else if (METHOD.equals("root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(EXPR)) { CONDITION = true; break; } } } 
        else if (METHOD.equals("thread")) { CONDITION = getThreadName(Thread.currentThread()).equals(EXPR); } 
        else if (METHOD.equals("screen")) { CONDITION = ((Hashtable) getobject("2", "saves")).containsKey(EXPR); } 
        else if (METHOD.equals("key")) { CONDITION = attributes.containsKey(EXPR); } 
        else if (METHOD.equals("alias")) { CONDITION = aliases.containsKey(EXPR); } 
        else if (METHOD.equals("trace")) { CONDITION = getpid(EXPR) != null ? true : false; } 
        else if (METHOD.equals("passwd")) { CONDITION = String.valueOf(EXPR.hashCode()).equals(MIDletControl.passwd()); } 
        else if (METHOD.equals("user")) { 
            CONDITION = username.equals(EXPR); 
            if (EXPR.equals("root") && id == 0) { CONDITION = true; } 
        } 
        if (CONDITION != NEGATED) { return processCommand(CMD, enable, id, pid, stdout, scope); } 
        
        return 0; 
    
    }

    // Java Virtual Machine
    public int java(String command, int id, String pid, Object stdout, Hashtable scope) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("Java ME", env("Java 1.2 (OpenTTY Edition)\n\nMicroEdition-Config: $CONFIG\nMicroEdition-Profile: $PROFILE")); }
        else if (mainCommand.equals("-class")) { if (argument.equals("")) { } else { int STATUS = javaClass(argument); print(STATUS == 0 ? "true" : "false", stdout); return STATUS; } } 
        else if (mainCommand.equals("--version")) { print(getName(), stdout); }
        else { print("java: " + mainCommand + ": not found", stdout); return 127; }

        return 0;
    }
    public int javaClass(String name) { try { Class.forName(name); return 0; } catch (ClassNotFoundException e) { return 3; } } 
    public String getName() { String s; StringBuffer BUFFER = new StringBuffer(); if ((s = System.getProperty("java.vm.name")) != null) { BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor")); if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); } if ((s = System.getProperty("java.vm.specification.name")) != null) { BUFFER.append('\n').append(s); } } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) { BUFFER.append("J9 VM, IBM (").append(s).append(')'); if ((s = System.getProperty("java.fullversion")) != null) { BUFFER.append("\n\n").append(s); } } else if ((s = System.getProperty("com.oracle.jwc.version")) != null) { BUFFER.append("OJWC v").append(s).append(", Oracle"); } else if (javaClass("com.sun.cldchi.jvm.JVM") == 0) { BUFFER.append("CLDC Hotspot Implementation, Sun"); } else if (javaClass("com.sun.midp.Main") == 0) { BUFFER.append("KVM, Sun (MIDP)"); } else if (javaClass("com.sun.cldc.io.ConsoleOutputStream") == 0) { BUFFER.append("KVM, Sun (CLDC)"); } else if (javaClass("com.jblend.util.SortedVector") == 0) { BUFFER.append("JBlend, Aplix"); } else if (javaClass("com.jbed.io.CharConvUTF8") == 0) { BUFFER.append("Jbed, Esmertec/Myriad Group"); } else if (javaClass("MahoTrans.IJavaObject") == 0) { BUFFER.append("MahoTrans"); } else { BUFFER.append("Unknown"); } return BUFFER.append('\n').toString(); }
    
    // History
    public void add2History(String command) { if (command.equals("") || command.equals(getLastHistory()) || command.startsWith("!!") || command.startsWith("#")) { } else { ((Vector) getobject("1", "history")).addElement(command.trim()); } }
    public String getLastHistory() { Vector history = (Vector) getobject("1", "history"); return history.size() > 0 ? (String) history.elementAt(history.size() - 1) : ""; }

    // Packages
    public int importScript(String script, int id, Object stdout, Hashtable scope) { }
    public int runScript(String script, int id, String pid, Object stdout, Hashtable scope) { }
    // |
    public int run(String script, String[] args, int id, String pid, Object stdout, Hashtable scope) { }

    private Object goLua(String script, int id, Object stdout, Hashtable scope) { }
}
// |
// EOF