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
    // |
    // System Objects
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    
    public Hashtable attributes = new Hashtable(), fs = new Hashtable(), sys = new Hashtable(), filetypes = null, aliases = new Hashtable(), tmp = new Hashtable(), cache = new Hashtable(), globals = new Hashtable();
    public String username = read("/home/OpenRMS"), logs = "", path = "/home/", build = "2025-1.17-03x02";
    // |
    // Graphics
    public Display display = Display.getDisplay(this);
    public Form xterm = new Form(null);
    public TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    public StringItem stdout = new StringItem("", "");
    public Command EXECUTE = new Command("Run", Command.OK, 0);
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
            if (username.equals("") || MIDletControl.passwd().equals("")) { new MIDletControl(null); }
            else { run("/home/.initrc", new String[] { "/home/.initrc" }, 1000, "1", this.stdout, global); }
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
        public static final int HISTORY = 1, EXPLORER = 2, MONITOR = 3, PROCESS = 4, SIGNUP = 5, REQUEST = 6, LOCK = 7, NC = 8, BIND = 11, BG = 12, ADDON = 13;
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
        private Command BACK = new Command("Back", Command.BACK, 1), MENU, RUN, RUNS, IMPORT, OPEN, EDIT, REFRESH, PROPERTY, KILL, LOAD, DELETE, LOGIN, EXIT, FILTER, CONNECT, VIEW, SAVE, YES, NO;

        private SocketConnection CONN;
        private ServerSocketConnection server = null;
        private InputStream IN;
        private OutputStream OUT;

        private String[] wordlist;

        public MIDletControl(String command, int id, Object stdout, Hashtable scope) { MOD = command == null || command.length() == 0 || command.equals("monitor") ? MONITOR : command.equals("process") ? PROCESS : command.equals("dir") ? EXPLORER : command.equals("history") ? HISTORY : -1; this.id = id; this.stdout = stdout; this.scope = scope; if (MOD == MONITOR) { monitor = new Form(xterm.getTitle()); monitor.append(console = new StringItem("Memory Status:", "")); monitor.addCommand(BACK); monitor.addCommand(MENU = new Command("Menu", Command.SCREEN, 1)); monitor.addCommand(REFRESH = new Command("Refresh", Command.SCREEN, 2)); monitor.setCommandListener(this); load(); display.setCurrent(monitor); } else { preview = new List(xterm.getTitle(), List.IMPLICIT); preview.addCommand(BACK); preview.addCommand(MOD == EXPLORER ? (OPEN = new Command("Open", Command.OK, 1)) : MOD == PROCESS ? (KILL = new Command("Kill", Command.OK, 1)) : (RUN = new Command("Run", Command.OK, 1))); if (MOD == HISTORY) { preview.addCommand(EDIT = new Command("Edit", Command.OK, 1)); } if (MOD == PROCESS) { preview.addCommand(LOAD = new Command("Load Screen", Command.OK, 1)); preview.addCommand(VIEW = new Command("View info", Command.OK, 1)); preview.addCommand(REFRESH = new Command("Refresh", Command.OK, 1)); preview.addCommand(FILTER = new Command("Filter", Command.OK, 1)); } else if (MOD == EXPLORER) { preview.addCommand(DELETE = new Command("Delete", Command.OK, 1)); preview.addCommand(RUNS = new Command("Run Script", Command.OK, 1)); preview.addCommand(PROPERTY = new Command("Properties", Command.OK, 1)); preview.addCommand(REFRESH = new Command("Refresh", Command.OK, 1)); } preview.setCommandListener(this); load(); display.setCurrent(preview); } }
        public MIDletControl(String command, Object stdout, Hashtable scope) {
            MOD = command == null || command.length() == 0 || command.equals("login") ? SIGNUP : REQUEST;
            this.stdout = stdout; this.scope = scope;
            monitor = new Form(xterm.getTitle());

            if (MOD == SIGNUP) {
                monitor.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\n" + (asking_user && asking_passwd ? "Create your credentials!" : asking_user ? "Create an user to access OpenTTY!" : asking_passwd ? "Create a password!" : "")).trim());

                if (asking_user) { monitor.append(USER = new TextField("Username", "", 256, TextField.ANY)); }
                if (asking_passwd) { monitor.append(PASSWD = new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); }

                monitor.addCommand(LOGIN = new Command("Login", Command.OK, 1)); monitor.addCommand(EXIT = new Command("Exit", Command.SCREEN, 2));
            } 
            else {
                if (asking_passwd) { new MIDletControl(null); return; }
                this.command = command;

                monitor.append(PASSWD = new TextField("[sudo] password for " + loadRMS("OpenRMS"), "", 256, TextField.ANY | TextField.PASSWORD));
                monitor.addCommand(EXECUTE); monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
            }

            monitor.setCommandListener(this);
            display.setCurrent(monitor);
        }
        public MIDletControl(String mode, String args, int id) {
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
            } 

            Hashtable proc = genprocess(MOD == NC ? "remote" : MOD == PRSCAN ? "prscan" : "gobuster", id, null);

            if (MOD == NC) {
                address = args;
                try { CONN = (SocketConnection) Connector.open("socket://" + address); IN = CONN.openInputStream(); OUT = CONN.openOutputStream(); } 
                catch (Exception e) { echoCommand(getCatch(e)); return; }

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
            }
            
            trace.put(PID, proc);
            new Thread(this, "NET").start();
        }
        public MIDletControl(Form screen, String node, String code, int id) { if (code == null || code.length() == 0) { return; } this.PKG = parseProperties(code); this.node = node; this.id = id; if (!PKG.containsKey(node + ".label") || !PKG.containsKey(node + ".cmd")) { MIDletLogs("add error Malformed ITEM, missing params"); return; } RUN = new Command(getenv(node + ".label"), Command.ITEM, 1); s = new StringItem(null, getenv(node + ".label"), StringItem.BUTTON); s.setFont(newFont(getenv(node + ".style", "default"))); s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(RUN); s.setDefaultCommand(RUN); s.setItemCommandListener(this); screen.append(s); }
        public MIDletControl(String name, String command, boolean enable, int id, Object stdout, Hashtable scope) { this.MOD = BG; this.command = command; this.enable = enable; this.id = id; this.stdout = stdout; this.scope = scope; new Thread(this, name).start(); }
        public MIDletControl(String pid, String name, String command, boolean enable, int id, Object stdout, Hashtable scope) { this.MOD = ADDON; this.PID = pid; this.command = command; this.enable = enable; this.id = id;  this.stdout = stdout; this.scope = scope; new Thread(this, name).start(); }
    
        public void commandAction(Command c, Displayable d) {
            if (c == BACK) { 
                if (d == box || (d == monitor && MOD == EXPLORER)) { display.setCurrent(preview); }
                else if (MOD == NC) { back(); } else { goback(); } 
                
                return; 
            }
            if (d == confirm) { if (c == YES) { keep = true; } else { trace.remove(PID); } goback(); }
            if (d == box) { pfilter = box.getString().trim(); load(); display.setCurrent(preview); return; }

            if (MOD == HISTORY) { String selected = preview.getString(preview.getSelectedIndex()); if (selected != null) { goback(); processCommand(c == RUN || c == List.SELECT_COMMAND ? selected : "buff " + selected, true, id, PID, stdout, scope); } } 
            else if (MOD == EXPLORER) {
                String selected = preview.getString(preview.getSelectedIndex());

                if (c == OPEN || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { if (selected != null) { processCommand(selected.endsWith("..") ? "cd .." : selected.endsWith("/") ? "cd " + path + selected : "nano " + path + selected, false, id, PID, stdout, scope); if (display.getCurrent() == preview) { reload(); } setLabel(); } } 
                else if (c == DELETE) { 
                    if (selected.equals("..")) {  }
                    else if (path.equals("/home/") || path.equals("/tmp/") || (path.startsWith("/mnt/") && !path.equals("/mnt/")) || path.equals("/bin/") || path.equals("/lib/")) {
                        int STATUS = deleteFile(path + selected, id); 
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
                                InputStream in = readRaw(path + selected);
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
                    else if (c == VIEW) { processCommand("trace view " + PID, false, id, PID, stdout, scope); } 
                    else if (c == LOAD) {
                        if (!getowner(PID).equals(username) && id != 0) { STATUS = 13; }

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
                        if (asking_user) { writeRMS("/home/OpenRMS", username.getBytes(), 0); }
                        if (asking_passwd) { writeRMS("OpenRMS", String.valueOf(password.hashCode()).getBytes(), 2, 0); }

                        display.setCurrent(xterm);
                        getInstance().run("/home/.initrc", new String[] { "/home/.initrc" }, 1000);
                        setLabel();
                    }
                } 
                else if (c == EXIT) { processCommand("exit", false); }
            } 
            else if (MOD == REQUEST) {
                String password = PASSWD.getString().trim();

                if (password.equals("")) { } 
                else if (String.valueOf(password.hashCode()).equals(passwd())) { processCommand("xterm"); processCommand(command, true, 0); setLabel(); } 
                else { PASSWD.setString(""); warn(xterm.getTitle(), "Wrong password"); }
            }

            else if (MOD == NC) {
                if (c == EXECUTE) {
                    String PAYLOAD = remotein.getString().trim(); remotein.setString("");

                    try { OUT.write((PAYLOAD + "\n").getBytes()); OUT.flush(); } 
                    catch (Exception e) { warn(xterm.getTitle(), getCatch(e)); if (keep) { } else { trace.remove(PID); } }
                } 
                else if (c == BACK) { write("/home/remote", console.getText(), 0); back(); } 
                else if (c == CLEAR) { console.setText(""); }
                else if (c == VIEW) { try { warn("Information", "Host: " + split(address, ':')[0] + "\n" + "Port: " + split(address, ':')[1] + "\n\n" + "Local Address: " + CONN.getLocalAddress() + "\n" + "Local Port: " + CONN.getLocalPort()); } catch (Exception e) { warn(xterm.getTitle(), "Couldn't read connection information!"); } }
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

                    Vector files = (Vector) paths.get(path);
                    if (files != null) { for (int i = 0; i < files.size(); i++) { String f = (String) files.elementAt(i); if (f != null && !f.equals("..") && !f.equals("/") && !stack.contains(f)) { preview.append(f, null); stack.addElement(f); } } }
                } catch (IOException e) { }
            } 
            else if (MOD == MONITOR) { console.setText("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\n" + "Free Memory: " + runtime.freeMemory() / 1024 + " KB\n" + "Total Memory: " + runtime.totalMemory() / 1024 + " KB"); } 
            else if (MOD == PROCESS) { preview.deleteAll(); for (Enumeration keys = trace.keys(); keys.hasMoreElements();) { String PID = (String) keys.nextElement(), name = (String) ((Hashtable) trace.get(PID)).get("name"); if (pfilter.equals("") || name.indexOf(pfilter) != -1) { preview.append(PID + "\t" + name, null); } } }
        }

        private void back() { if (trace.containsKey(PID) && !asked) { confirm = new Alert("Background Process", "Keep this process running in background?", null, AlertType.WARNING); confirm.addCommand(YES = new Command("Yes", Command.OK, 1)); confirm.addCommand(NO = new Command("No", Command.BACK, 1)); confirm.setCommandListener(this); asked = true; display.setCurrent(confirm); } else { processCommand("xterm"); } }
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
        if (command.endswith("&")) { return processCommand("bg " + command.substring(0, command.length() - 1), enable, id, pid, stdout, scope); }

        if (mainCommand.equals("") || mainCommand.equals("true") || mainCommand.equals("#")) { }
        else if (aliases.containsKey(mainCommand)) { return processCommand(((String) aliases.get(mainCommand)) + " " + argument, enable, id, pid, stdout, scope); }
        else if (classpath && file("/bin/" + mainCommand) && !mainCommand.equals("sh") && !mainCommand.equals("lua") && !mainCommand.startsWith(".")) { return processCommand(". /bin/" + command, enable, id, pid, stdout, scope); }

        else if (mainCommand.equals("")) {  }
        else if (mainCommand.equals("")) {  }
        else if (mainCommand.equals("")) {  }
        else if (mainCommand.equals("")) {  }
        else if (mainCommand.equals("")) {  }
        // |
        else if (mainCommand.equals("gc")) { System.gc(); } 
        else if (mainCommand.equals("htop")) {  }
        else if (mainCommand.equals("top")) {  }
        // |
        else if (mainCommand.equals("start") || mainCommand.equals("kill") || mainCommand.equals("stop")) {  }
        else if (mainCommand.equals("ps")) {  }
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
        else if (mainCommand.equals("title")) { xterm.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument.equals("hide") ? null : argument) }
        else if (mainCommand.equals("tick")) { if (argument.equals("label")) { print(display.getCurrent().getTicker().getString(), stdout); } else { return xcli("tick " + argument, id, stdout, scope); } }
        // |
        else if (mainCommand.equals("log")) { return MIDletLogs(argument); }
        else if (mainCommand.equals("logcat")) { print(logs, stdout); }
        // |
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { print(id == 0 ? "root" : username); }
        else if (mainCommand.equals("sudo")) { if (argument.equals("")) { } else if (id == 0) { return processCommand(argument, enable, id, pid, stdout, scope); } else { } }
        else if (mainCommand.equals("su")) { if (id == 0) { username = username.equals("root") ? read("/home/OpenRMS") : "root"; return processCommand(". /bin/sh", false, id, pid, stdout, scope); } else { print("Permission denied!", stdout); return 13; } }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { return argument.equals("") ? processCommand(". /bin/sh", false, id, pid, stdout, scope) : rurunScript(argument, id, pid, stdout, scope); }
        else if (mainCommand.equals("id")) { String ID = argument.equals("") ? String.valueOf(id) : argument.equals("root") ? "0" : argument.equals(read("/home/OpenRMS")) ? "1000" : null; if (ID == null) { print("id: '" + argument + "': no such user", stdout); return 127; } print(ID, stdout); }
        else if (mainCommand.equals("passwd")) { if (argument.equals("")) { } else if (id == 0) { writeRMS("OpenRMS", argument, 2, id); } else { print("Permission denied!", stdout); return 13; } }
        else if (mainCommand.equals("logout")) { if (read("/home/OpenRMS").equals(username)) { if (id == 0) { writeRMS("/home/OpenRMS", "".getBytes(), id); destroyApp(false); } else { print("Permission denied!", stdout); return 13; } } else { username = read("/home/OpenRMS"); return processCommand(". /bin/sh", false, id, pid, stdout, scope); } }
        else if (mainCommand.equals("exit")) { if (read("/home/OpenRMS").equals(username)) { if (pid.equals("1")) { destroyApp(false); } else { return 254; } } else { username = read("/home/OpenRMS"); return processCommand(". /bin/sh", false, id, pid, stdout, scope); } }
        else if (mainCommand.equals("quit")) { destroyApp(false); }
        // |
        else if (mainCommand.equals("pkg")) {  }
        else if (mainCommand.equals("uname")) {  }
        else if (mainCommand.equals("hostname")) {  }
        else if (mainCommand.equals("hostid")) {  }
        // |
        else if (mainCommand.equals("tty")) { print((String) attributes.get("TTY"), stdout); }
        else if (mainCommand.equals("ttysize")) {  }
        else if (mainCommand.equals("stty")) {  }
        // |
        else if (mainCommand.equals("about")) { if (argument.equals("")) { } else { about(argument); } }
        else if (mainCommand.equals("import")) { return importScript(getcontent(argument), id, stdout, scope); }
        // |
        else if (mainCommand.equals("eval")) { if (argument.equals("")) { } else { print("" + processCommand(argument, enable, id, pid, stdout, scope), stdout); } }
        else if (mainCommand.equals("catch")) { if (argument.equals("")) { } else { try { processCommand(argument, enable, id, pid, stdout, scope); } catch (Throwable e) { print(getCatch(e), stdout); } } }
        else if (mainCommand.equals("false")) { return 255; }
        // |
        else if (mainCommand.equals("!")) { print(env("main/$RELEASE")); }
        else if (mainCommand.equals("!!")) { stdin.setString((argument.equals("") ? "" : argument + " ") + getLastHistory()); } 
        else if (mainCommand.equals(".")) { return run(argument, args, id, stdout, scope); }
        // |
        else { print(mainCommand + ": not found", stdout); return 127; }
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
    public String basename(String path) { }
    public String dirname(String path) { }
    public String expression(String expr) { }
    public String generateUUID() { }
    // |
    public String parseJSON(String text) { }
    public String parseConf(String text) { }
    // | 
    public String extractTitle(String html, String fallback) { }
    public String extractTag(String html, String tag, String fallback) { }
    public String html2text(String html) { }
    // |
    public String getcontent(String file) { return file.startsWith("/") ? read(file) : file.equals("nano") ? nanoContent : read(path + file); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }
    // | (Arrays)
    public String join(String[] array, String spacer, int start) { if (array == null || array.length == 0 || start >= array.length) { return ""; } StringBuffer sb = new StringBuffer(); for (int i = start; i < array.length; i++) { sb.append(array[i]).append(spacer); } return sb.toString().trim(); }
    private int indexOf(String key, String[] array) { for (int i = 0; i < array.length; i++) { if (array[i].equals(key)) { return i; } } return -1; }
    public String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    public String[] splitArgs(String content) { Vector args = new Vector(); boolean inQuotes = false; int start = 0; for (int i = 0; i < content.length(); i++) { char c = content.charAt(i); if (c == '"') { inQuotes = !inQuotes; continue; } if (!inQuotes && c == ' ') { if (i > start) { args.addElement(getpattern(content.substring(start, i))); } start = i + 1; } } if (start < content.length()) { args.addElement(getpattern(content.substring(start))); } String[] result = new String[args.size()]; args.copyInto(result); return result; }
    // | (Converting String on Map)
    public Hashtable parseProperties(String text) { if (text == null) { return new Hashtable(); } Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (line.startsWith("#")) { } else { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { properties.put(line.substring(0, equalIndex).trim(), getpattern(line.substring(equalIndex + 1).trim())); } } } return properties; }
    // | (String <> Number)
    public int getNumber(String s, int fallback, boolean print) { try { return Integer.valueOf(s); } catch (Exception e) { if (print) { echoCommand(getCatch(e)); } return fallback; } }
    public Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    

    // Logging Manager
    public int MIDletLogs(String command) { }

    // Graphics
    public int xcli(String command, int id, Object stdout, Hashtable scope) { }
    // |
    public void print(String message, Object stdout) { print(message, stdout, true); }
    public void print(String message, Object stdout, boolean log) {

    }
    // |
    public int warn(String title, String message) { }
    public int viewer(String title, String text) { }
    // |
    public Font genFont(String params) { }

    // Process
    public int start(String app, int id, String pid, Hashtable signals, Object stdout, Hashtable scope) { }
    public int kill(String pid, int id, Object stdout, Hashtable scope) { }
    public int stop(String pid, int id, Object stdout, Hashtable scope) { }
    // | (Kernel)
    public int kernel(String command, int id, Object stdout, Hashtable scope) { }
    // | (Generators)
    public String genpid() { }
    public Hashtable genprocess(String name, int id, Hashtable signals) { }
    public Hashtable gensignals(String collector) { }
    // | (Trackers)
    public Hashtable getprocess(String pid) { }
    public Object getobject(String pid, Object field) { }
    public String getsignal(String pid, Object signal) { }
    public String getowner(String pid) { }
    public String getpid(String app) { }
    // | (Renders)
    public String renderJSON(Object obj, int indent) { }

    // Connections
    private String request(String url, Hashtable headers) { }
    private String request(String url) { }
    // |
    private int query(String command, int id, Object stdout) { }
    private int GetAddress(String command) { }

    // File System
    private int mount(String struct) { }
    private String readStack() { }
    // | (Read) 
    public InputStream getInputStream(String filename) throws Exception { }
    public Image getImage(String filename) { }
    public String read(String filename) { }
    public static String loadRMS(String filename, int index) { try { RecordStore RMS = RecordStore.openRecordStore(filename, true); if (RMS.getNumRecords() >= index) { byte[] data = RMS.getRecord(index); if (data != null) { return new String(data); } } if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return ""; }
    // | (Write)
    public int write(String filename, String data, int id) { }
    public int write(String filename, byte[] data, int id) { }
    public int writeRMS(String filename, byte[] data, int index, int id) { }
    // |
    // | (Archives Structures)
    public int addFile(String filename, String content, String archive, String base, int id) { }
    public Stirng delFIle(String filename, String archive) { }
    public String read(String filename, String archive) { }
    // |
    public boolean file(String filename) { }
    // |
    public String getMimeType(String filename) { }
    public String getFileType(String filename) { }
    public String getExtension(String filename) { }
    public String[] getExtensionInfo(String ext) { }

    // Audio Manager
    public int audio(String command, int id, String pid, Object stdout, Hashtable scope) { }

    // Java Virtual Machine
    public int java(String command, int id, String pid, Object stdout, Hashtable scope) { }
    public int javaClass(String name) { }
    public String getName() { }

    // History
    public void add2History(String command) { }
    public String getLastHistory() { }

    // Packages
    public void about(String script) { }
    public int importScript(String script, int id, Object stdout, Hashtable scope) { }
    public int runScript(String script, int id, String pid, Object stdout, Hashtable scope) { }
    // |
    public int run(String script, String[] args, int id, String pid, Object stdout, Hashtable scope) { }

    private Object goLua(String script, int id, Object stdout, Hashtable scope) { }
}
// |
// EOF