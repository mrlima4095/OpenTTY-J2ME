import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.control.*;
import javax.microedition.io.file.*;
import javax.wireless.messaging.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.pki.*;
import javax.microedition.io.*;
import javax.bluetooth.*;
import java.util.*;
import java.io.*;
// |
// OpenTTY MIDlet
public class OpenTTY extends MIDlet implements CommandListener {
    public int TTY_MAX_LEN = 0, cursorX = 10, cursorY = 10;
    // |
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Hashtable attributes = new Hashtable(), paths = new Hashtable(), trace = new Hashtable(), filetypes = null,
                     aliases = new Hashtable(), shell = new Hashtable(), functions = new Hashtable(), tmp = new Hashtable();
    public String username = loadRMS("OpenRMS"), nanoContent = loadRMS("nano");
    public String logs = "", path = "/home/", build = "2025-1.17-02x85";
    public Display display = Display.getDisplay(this);
    public TextBox nano = new TextBox("Nano", "", 31522, TextField.ANY);
    public Form form = new Form("OpenTTY " + getAppProperty("MIDlet-Version"));
    public TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    public StringItem stdout = new StringItem("", "");
    private Command EXECUTE = new Command("Send", Command.OK, 0), HELP = new Command("Help", Command.SCREEN, 1), NANO = new Command("Nano", Command.SCREEN, 2), CLEAR = new Command("Clear", Command.SCREEN, 3), HISTORY = new Command("History", Command.SCREEN, 4),
                    BACK = new Command("Back", Command.BACK, 1), RUNS = new Command("Run Script", Command.OK, 1), VIEW = new Command("View as HTML", Command.OK, 1);
    // |
    // MIDlet Loader
    public void startApp() { 
        if (trace.containsKey("1")) { }
        else {
            attributes.put("PATCH", "Fear Fog"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.6.4");
            attributes.put("HOSTNAME", "localhost"); attributes.put("QUERY", "nano");
            // |
            String[] KEYS = { "TYPE", "CONFIG", "PROFILE", "LOCALE" }, SYS = { "platform", "configuration", "profiles", "locale" };
            for (int i = 0; i < KEYS.length; i++) { attributes.put(KEYS[i], System.getProperty("microedition." + SYS[i])); }
            // |
            Command[] NANO_CMDS = { BACK, CLEAR, RUNS, VIEW }; for (int i = 0; i < NANO_CMDS.length; i++) { nano.addCommand(NANO_CMDS[i]); } nano.setCommandListener(this);
            // |
            runScript(read("/etc/init"), true); setLabel();
            // |
            if (username.equals("") || MIDletControl.passwd().equals("")) { new MIDletControl(null); }
            else { runScript(loadRMS(".initrc")); }
        }
    }
    // |
    // | (Triggers)
    public void pauseApp() { processCommand(functions.containsKey("pauseApp()") ? "pauseApp()" : "true"); }
    public void destroyApp(boolean unconditional) { writeRMS("/home/nano", nanoContent); }
    // |
    // | (Main Listener)
    public void commandAction(Command c, Displayable d) {
        if (d == nano) {
            nanoContent = nano.getString(); 

            if (c == CLEAR) { nano.setString(""); } 
            else { processCommand("execute xterm; " + (c == RUNS ? "." : c == VIEW ? "html" : "true")); }
        } else {
            if (c == EXECUTE) { String command = stdin.getString().trim(); add2History(command); stdin.setString(""); processCommand(command); setLabel(); }            
            else { processCommand(c == HELP ? "help" : c == NANO ? "nano" : c == CLEAR ? "clear" : c == HISTORY ? "history" : c == BACK ? "xterm" : "warn Invalid KEY (" + c.getLabel() + ") - " + c.getCommandType()); }
        }
    }
    // |
    // Control Thread
    public String getThreadName(Thread thr) {
        String name = thr.getName();
        String[] generic = { "Thread-0", "Thread-1", "MIDletEventQueue", "main" };
        for (int i = 0; i < generic.length; i++) { name = replace(name, generic[i], "MIDlet"); }

        return name;
    }
    public void setLabel() { stdin.setLabel(username + " " + path + " " + (username.equals("root") ? "#" : "$")); }
    public class MIDletControl implements ItemCommandListener, CommandListener, Runnable {
        private static final int HISTORY = 1, EXPLORER = 2, MONITOR = 3, PROCESS = 4, SIGNUP = 5, REQUEST = 7, LOCK = 8, NC = 9, PRSCAN = 10, GOBUSTER = 11, BIND = 12, SCREEN = 13, LIST = 14, QUEST = 15, WEDIT = 16, BG = 17, ADDON = 18;

        private int MOD = -1, COUNT = 1, start;
        private boolean root = false, ignore = true, asked = false, keep = false, asking_user = username.equals(""), asking_passwd = passwd().equals("");
        private String command = null, pfilter = "", PID = genpid(), DB, address, port, node, proc_name;
        private Vector history = (Vector) getobject("1", "history");
        private Hashtable sessions = (Hashtable) getobject("1", "sessions"), PKG;
        private Alert confirm;
        private Form monitor;
        private List preview;
        private TextBox box;
        private StringItem console, s;
        private TextField USER, PASSWD, remotein;
        private Command BACK = new Command("Back", Command.BACK, 1), RUN, RUNS, IMPORT, OPEN, EDIT, REFRESH, PROPERTY, KILL, LOAD, DELETE, LOGIN, EXIT, FILTER, CONNECT, VIEW, SAVE, YES, NO;

        private SocketConnection CONN;
        private ServerSocketConnection server = null;
        private InputStream IN;
        private OutputStream OUT;

        private String[] wordlist;

        public MIDletControl(String command, boolean root) {
            MOD = command == null || command.length() == 0 || command.equals("monitor") ? MONITOR : command.equals("process") ? PROCESS : command.equals("dir") ? EXPLORER : command.equals("history") ? HISTORY : -1;
            this.root = root;

            if (MOD == MONITOR) {
                monitor = new Form(form.getTitle());
                monitor.append(console = new StringItem("Memory Status:", ""));
                monitor.addCommand(BACK);
                monitor.addCommand(REFRESH = new Command("Refresh", Command.SCREEN, 2));
                monitor.setCommandListener(this);
                load();
                display.setCurrent(monitor);
            } 
            else {
                preview = new List(form.getTitle(), List.IMPLICIT);
                preview.addCommand(BACK);
                preview.addCommand(MOD == EXPLORER ? (OPEN = new Command("Open", Command.OK, 1)) : MOD == PROCESS ? (KILL = new Command("Kill", Command.OK, 1)) : (RUN = new Command("Run", Command.OK, 1)));

                if (MOD == HISTORY) { preview.addCommand(EDIT = new Command("Edit", Command.OK, 1)); } 
                else if (MOD == PROCESS) { 
                    preview.addCommand(LOAD = new Command("Load Screen", Command.OK, 1)); 
                    preview.addCommand(VIEW = new Command("View info", Command.OK, 1)); 
                    preview.addCommand(FILTER = new Command("Filter", Command.OK, 1)); 
                }
                else if (MOD == EXPLORER) {
                    preview.addCommand(DELETE = new Command("Delete", Command.OK, 1));
                    preview.addCommand(RUNS = new Command("Run Script", Command.OK, 1));
                    preview.addCommand(PROPERTY = new Command("Properties", Command.OK, 1));
                    preview.addCommand(REFRESH = new Command("Refresh", Command.OK, 1));
                }

                preview.setCommandListener(this);
                load(); display.setCurrent(preview);
            }
        }
        public MIDletControl(String command) {
            MOD = command == null || command.length() == 0 || command.equals("login") ? SIGNUP : REQUEST;
            monitor = new Form(form.getTitle());

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
        public MIDletControl(String mode, String args, boolean root) {
            MOD = mode == null || mode.length() == 0 || mode.equals("nc") ? NC : mode.equals("prscan") ? PRSCAN : mode.equals("gobuster") ? GOBUSTER : mode.equals("bind") ? BIND : -1;
            this.root = root;


            if (args == null || args.length() == 0) { return; }
            if (MOD == -1) { return; }
            else if (MOD == BIND) {
                String[] argv = splitArgs(args);

                port = argv[0]; 
                DB = argv.length > 1 ? argv[1] : "";
                proc_name = argv.length > 2 ? argv[2] : "bind";


                new Thread(this, "Bind").start();
                return;
            } 

            Hashtable proc = genprocess(MOD == NC ? "remote" : MOD == PRSCAN ? "prscan" : "gobuster", root, null);

            if (MOD == NC) {
                address = args;
                try { CONN = (SocketConnection) Connector.open("socket://" + address); IN = CONN.openInputStream(); OUT = CONN.openOutputStream(); } 
                catch (Exception e) { echoCommand(getCatch(e)); return; }

                monitor = new Form(form.getTitle());
                monitor.append(console = new StringItem("", ""));
                monitor.append(remotein = new TextField("Remote (" + split(address, ':')[0] + ")", "", 256, TextField.ANY));
                monitor.addCommand(EXECUTE);
                monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
                monitor.addCommand(CLEAR);
                monitor.addCommand(VIEW = new Command("View info", Command.SCREEN, 2));
                monitor.setCommandListener(this);

                proc.put("socket", CONN);
                proc.put("in-stream", IN);
                proc.put("out-stream", OUT);
                proc.put("screen", monitor);
                display.setCurrent(monitor);
            } 
            else {
                address = getCommand(args);
                preview = new List(MOD == PRSCAN ? address + " Ports" : "GoBuster (" + address + ")", List.IMPLICIT);

                if (MOD == PRSCAN) { start = getNumber(getArgument(args).equals("") ? "1" : getArgument(args), 1, true); } 
                else {
                    wordlist = split(getArgument(args).equals("") ? loadRMS("gobuster") : getcontent(getArgument(args)), '\n');
                    if (wordlist == null || wordlist.length == 0) { echoCommand("gobuster: blank word list"); return; }
                }

                preview.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
                preview.addCommand(CONNECT = new Command("Connect", Command.BACK, 1));
                preview.addCommand(SAVE = new Command("Save Logs", Command.SCREEN, 2));
                preview.setCommandListener(this);

                proc.put("screen", preview);
                display.setCurrent(preview);
            }

            trace.put(PID, proc);
            new Thread(this, "NET").start();
        }
        public MIDletControl(Form screen, String node, String code, boolean root) {
            if (code == null || code.length() == 0) { return; } 

            this.PKG = parseProperties(code); this.node = node; this.root = root; 

            if (!PKG.containsKey(node + ".label") || !PKG.containsKey(node + ".cmd")) { MIDletLogs("add error Malformed ITEM, missing params"); return; } 

            RUN = new Command(getenv(node + ".label"), Command.ITEM, 1); 
            s = new StringItem(null, getenv(node + ".label"), StringItem.BUTTON); 
            s.setFont(newFont(getenv(node + ".style", "default"))); 
            s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); 
            s.addCommand(RUN); 
            s.setDefaultCommand(RUN); 
            s.setItemCommandListener(this); 
            screen.append(s); 
        }
        public MIDletControl(int MOD, String code, boolean root) {
            if (code == null || code.length() == 0) { return; } 

            this.PKG = parseProperties(code); 
            this.MOD = MOD; this.root = root;

            if (MOD == SCREEN) { 
                monitor = new Form(getenv("screen.title", form.getTitle()));
                BACK = new Command(getenv("screen.back.label", "Back"), Command.OK, 1); 
                RUN = new Command(getenv("screen.button", "Menu"), Command.SCREEN, 2); 
                monitor.addCommand(BACK); 
                if (PKG.containsKey("screen.button")) { monitor.addCommand(RUN); } 
                if (PKG.containsKey("screen.fields")) { 
                    String[] fields = split(getenv("screen.fields"), ','); 

                    for (int i = 0; i < fields.length; i++) { 
                        String field = fields[i].trim(); 
                        String type = getenv("screen." + field + ".type"); 

                        if (type.equals("image") && !getenv("screen." + field + ".img").equals("")) { 
                            try { monitor.append(new ImageItem(null, Image.createImage(getenv("screen." + field + ".img")), ImageItem.LAYOUT_CENTER, null)); } 
                            catch (Exception e) { MIDletLogs("add warn Malformed Image '" + getenv("screen." + field + ".img") + "'"); } 
                        } 
                        else if (type.equals("text") && !getenv("screen." + field + ".value").equals("")) { 
                            StringItem content = new StringItem(getenv("screen." + field + ".label"), getenv("screen." + field + ".value")); 

                            content.setFont(newFont(getenv("screen." + field + ".style", "default"))); 
                            monitor.append(content); 
                        }
                        else if (type.equals("item")) { new MIDletControl(monitor, "screen." + field, code, root); } 
                        else if (type.equals("spacer")) { 
                            int width = Integer.parseInt(getenv("screen." + field + ".w", "1")), height = Integer.parseInt(getenv("screen." + field + ".h", "10")); 
                            monitor.append(new Spacer(width, height)); 
                        }
                    } 
                } 

                monitor.setCommandListener(this); display.setCurrent(monitor); 
            } 
            else if (MOD == LIST) {
                Image IMG = null; 

                if (!PKG.containsKey("list.content")) { MIDletLogs("add error List crashed while init, malformed settings"); return; } 

                preview = new List(getenv("list.title", form.getTitle()), List.IMPLICIT); 
                if (PKG.containsKey("list.icon")) { 
                    try { IMG = Image.createImage(getenv("list.icon")); } 
                    catch (Exception e) { MIDletLogs("add warn Malformed Image '" + getenv("list.icon") + "'"); } 
                } 

                BACK = new Command(getenv("list.back.label", "Back"), Command.OK, 1); 
                RUN = new Command(getenv("list.button", "Select"), Command.SCREEN, 2); 
                preview.addCommand(BACK); preview.addCommand(RUN); 
                
                String[] content = split(getenv("list.content"), ','); 
                for (int i = 0; i < content.length; i++) { preview.append(content[i], IMG); } 

                if (PKG.containsKey("list.source")) {
                    String source = getcontent(getenv("list.source"));
                    
                    if (source.equals("")) { } 
                    else {
                        String[] content = split(source, '\n'); 
                        for (int i = 0; i < content.length; i++) {
                            String key = content[i], value = "true";
                            
                            int index = content[i].indexOf("=");
                            if (index == -1) { }
                            else { value = key.substring(index + 1); key = key.substring(0, index); }
                            
                            preview.append(key, IMG); 
                            PKG.put(key, value);
                        } 

                    }
                }

                preview.setCommandListener(this); display.setCurrent(preview); 
            } 
            else if (MOD == QUEST) {
                if (!PKG.containsKey("quest.label") || !PKG.containsKey("quest.cmd") || !PKG.containsKey("quest.key")) { MIDletLogs("add error Quest crashed while init, malformed settings"); return; } 
                monitor = new Form(getenv("quest.title", form.getTitle())); 

                USER = new TextField(getenv("quest.label"), getenv("quest.content"), 256, getQuest(getenv("quest.type"))); 
                BACK = new Command(getvalue("quest.back.label", "Cancel"), Command.SCREEN, 2); 
                RUN = new Command(getvalue("quest.cmd.label", "Send"), Command.OK, 1); 
                monitor.append(USER); monitor.addCommand(BACK); monitor.addCommand(RUN); 

                monitor.setCommandListener(this); display.setCurrent(monitor); 
            } 
            else if (MOD == WEDIT) {
                if (!PKG.containsKey("edit.cmd") || !PKG.containsKey("edit.key")) { MIDletLogs("add error Editor crashed while init, malformed settings"); return; } 
                
                box = new TextBox(getenv("quest.title", form.getTitle()), PKG.containsKey("edit.content") ? getenv("edit.content") : PKG.containsKey("edit.source") ? getcontent(getenv("edit.source")) : "", 31522, getQuest(getenv("quest.type")));

                BACK = new Command(getenv("edit.back.label", "Back"), Command.OK, 1);
                RUN = new Command(getenv("edit.cmd.label", "Run"), Command.SCREEN, 2);
                box.addCommand(BACK); box.addCommand(RUN);

                box.setCommandListener(this); display.setCurrent(box);

            }
            else { return; } 
        }
        public MIDletControl(String name, String command, boolean ignore, boolean root) { this.MOD = BG; this.command = command; this.ignore = ignore; this.root = root; new Thread(this, name).start(); }
        public MIDletControl(String pid, String name, String command, boolean ignore, boolean root) { this.MOD = ADDON; this.PID = pid; this.command = command; this.ignore = ignore; this.root = root; new Thread(this, name).start(); }

        public void commandAction(Command c, Displayable d) {
            if (c == BACK) { 
                if (d == box || (d == monitor && MOD == EXPLORER)) { display.setCurrent(preview); }
                else if (MOD == NC || MOD == PRSCAN || MOD == GOBUSTER) { back(); } 
                else if (MOD == SCREEN || MOD == LIST || MOD == QUEST || MOD == WEDIT) { processCommand("xterm", true, root); processCommand(getvalue((MOD == SCREEN ? "screen" : MOD == LIST ? "list" : MOD == QUEST ? "quest" : "edit") + ".back", "true"), true, root); }
                else { processCommand("xterm", true, root); } 
                
                return; 
            }
            if (d == confirm) { if (c == YES) { keep = true; processCommand("xterm"); } else { trace.remove(PID); processCommand("xterm"); } }
            if (d == box) { pfilter = box.getString().trim(); load(); display.setCurrent(preview); return; }

            if (MOD == HISTORY) { String selected = preview.getString(preview.getSelectedIndex()); if (selected != null) { processCommand("xterm"); processCommand(c == RUN || c == List.SELECT_COMMAND ? selected : "buff " + selected); } } 
            else if (MOD == EXPLORER) {
                String selected = preview.getString(preview.getSelectedIndex());

                if (c == OPEN || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { if (selected != null) { processCommand(selected.endsWith("..") ? "cd .." : selected.endsWith("/") ? "cd " + path + selected : "nano " + path + selected, false); if (display.getCurrent() == preview) { reload(); } setLabel(); } } 
                else if (c == DELETE) { 
                    if (path.equals("/home/") || path.equals("/tmp/") || (path.startsWith("/mnt/") && !path.equals("/mnt/"))) {
                        int STATUS = deleteFile(path + selected); 
                        if (STATUS != 0) { warnCommand(form.getTitle(), STATUS == 13 ? "Permission denied!" : "java.io.IOException"); } 
                        
                        reload(); 
                    } else { warnCommand(form.getTitle(), "read-only storage"); }
                } 
                else if (c == RUNS) {
                    if (selected.equals("..") || selected.endsWith("/")) { }
                    else { processCommand("xterm", true, root); processCommand(". " + path + selected, true, root); }
                } 
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
                            monitor.append(new StringItem("Size:", String.valueOf(getcontent(path + selected).length())));
                            ChoiceGroup perms = new ChoiceGroup("Permissions", Choice.MULTIPLE);
                            perms.append("Read", null); perms.append("Write", null);

                            perms.setSelectedIndex(0, true);
                            perms.setSelectedIndex(1, (path.startsWith("/home/") || path.startsWith("/tmp/") || (path.startsWith("/mnt/") && !path.equals("/mnt/")) || (selected = path + selected).equals("/dev/null") || selected.equals("/dev/stdin") || selected.equals("/dev/stdout")));

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
                else if (c == REFRESH) { reload(); }

                int index = preview.getSelectedIndex();
                if (index >= 0) {
                    String PID = split(preview.getString(index), '\t')[0];
                    int STATUS = 0;

                    if (c == KILL || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { STATUS = kill(PID, false, root); } 
                    else if (c == VIEW) { processCommand("trace view " + PID, false, root); } 
                    else if (c == LOAD) {
                        if (getowner(PID).equals("root") && !root) { STATUS = 13; }

                        Displayable screen = (Displayable) getobject(PID, "screen");

                        if (screen == null) { STATUS = 69; } 
                        else { display.setCurrent(screen); return; }
                    }

                    if (STATUS != 0) { warnCommand(form.getTitle(), STATUS == 13 ? "Permission denied!" : "No screens for this process!"); }

                    reload();
                }
            } 
            else if (MOD == SIGNUP) {
                if (c == LOGIN) {
                    String password = PASSWD.getString().trim();

                    if (asking_user) { username = USER.getString().trim(); }
                    if (asking_user && username.equals("") || asking_passwd && password.equals("")) { warnCommand(form.getTitle(), "Missing credentials!"); } 
                    else if (username.equals("root")) { USER.setString(""); warnCommand(form.getTitle(), "Invalid username!"); } 
                    else {
                        if (asking_user) { writeRMS("/home/OpenRMS", username); }
                        if (asking_passwd) { writeRMS("OpenRMS", String.valueOf(password.hashCode()).getBytes(), 2); }

                        display.setCurrent(form);
                        runScript(loadRMS(".initrc"));
                        setLabel();
                    }
                } 
                else if (c == EXIT) { processCommand("exit", false); }
            } 
            else if (MOD == REQUEST) {
                String password = PASSWD.getString().trim();

                if (password.equals("")) { } 
                else if (String.valueOf(password.hashCode()).equals(passwd())) { processCommand("xterm"); processCommand(command, true, true); } 
                else { PASSWD.setString(""); warnCommand(form.getTitle(), "Wrong password"); }
            }

            else if (MOD == NC) {
                if (c == EXECUTE) {
                    String PAYLOAD = remotein.getString().trim(); remotein.setString("");

                    try { OUT.write((PAYLOAD + "\n").getBytes()); OUT.flush(); } 
                    catch (Exception e) { warnCommand(form.getTitle(), getCatch(e)); if (keep) { } else { trace.remove(PID); } }
                } 
                else if (c == BACK) { writeRMS("/home/remote", console.getText()); back(); } 
                else if (c == CLEAR) { console.setText(""); }
                else if (c == VIEW) {
                    try { warnCommand("Information", "Host: " + split(address, ':')[0] + "\n" + "Port: " + split(address, ':')[1] + "\n\n" + "Local Address: " + CONN.getLocalAddress() + "\n" + "Local Port: " + CONN.getLocalPort()); } 
                    catch (Exception e) { warnCommand(form.getTitle(), "Couldn't read connection information!"); }
                }
            } 
            else if (MOD == PRSCAN || MOD == GOBUSTER) {
                if (c == CONNECT || c == List.SELECT_COMMAND) {
                    String ITEM = preview.getString(preview.getSelectedIndex());
                    processCommand(MOD == PRSCAN ? "nc " + address + ":" + ITEM : "execute tick Downloading...; wget " + address + "/" + getArgument(ITEM) + "; tick; nano; true");
                } 
                else if (c == SAVE) {
                    StringBuffer BUFFER = new StringBuffer();
                    for (int i = 0; i < preview.size(); i++) { BUFFER.append(MOD == PRSCAN ? preview.getString(i) : getArgument(preview.getString(i))).append("\n"); }

                    nanoContent = BUFFER.toString().trim();
                    processCommand("nano", false);
                }
            }
            
            else {
                if (c == RUN || c == List.SELECT_COMMAND) { 
                    if (MOD == QUEST) { 
                        String value = USER.getString().trim(); 
                        if (value.equals("")) { } 
                        else { 
                            attributes.put(getenv("quest.key"), env(value)); 
                            processCommand("xterm", true, root); 
                            processCommand(getvalue("quest.cmd", "true"), true, root); 
                        } 
                    } 
                    else if (MOD == WEDIT) { 
                        String value = box.getString().trim(); 
                        if (value.equals("")) { }
                        else { 
                            attributes.put(getenv("edit.key"), env(value)); 
                            processCommand("xterm", true, root); 
                            processCommand(getvalue("edit.cmd", "true"), true, root); 
                        } 
                    } 
                    else if (MOD == LIST) { int index = preview.getSelectedIndex(); if (index >= 0) { processCommand("xterm", true, root); String key = env(preview.getString(index)); processCommand(getvalue(key, "log add warn An error occurred, '" + key + "' not found"), true, root); } } 
                    else if (MOD == SCREEN) { processCommand("xterm", true, root); processCommand(getvalue("screen.button.cmd", "log add warn An error occurred, 'screen.button.cmd' not found"), true, root); } 
                } 
            }
        }
        public void commandAction(Command c, Item item) { if (c == RUN) { processCommand("xterm", true, root); processCommand((String) PKG.get(node + ".cmd"), true, root); } }

        public void run() {
            if (MOD == NC) {
                while (trace.containsKey(PID)) {
                    try {
                        if (IN.available() > 0) {
                            byte[] BUFFER = new byte[IN.available()];
                            int LENGTH = IN.read(BUFFER);
                            if (LENGTH > 0) echoCommand((new String(BUFFER, 0, LENGTH)).trim(), console);
                        }
                    } catch (Exception e) { warnCommand(form.getTitle(), getCatch(e)); if (!keep) { trace.remove(PID); } }
                }

                try { IN.close(); OUT.close(); CONN.close(); } catch (Exception e) { }
                return;
            }
            else if (MOD == PRSCAN) {
                for (int port = start; port <= 65535; port++) {
                    try {
                        preview.setTicker(new Ticker("Scanning port " + port + "..."));

                        if (trace.containsKey(PID)) {
                            Connector.open("socket://" + address + ":" + port, Connector.READ_WRITE, true).close();
                            preview.append("" + port, null);
                        }
                        else { break; }
                    } catch (IOException e) { }
                }
                preview.setTicker(null);
                if (keep) { } else { trace.remove(PID); }
                return;
            }
            else if (MOD == GOBUSTER) {
                preview.setTicker(new Ticker("Searching..."));
                for (int i = 0; i < wordlist.length; i++) {
                    String path = wordlist[i].trim();

                    if (trace.containsKey(PID)) {
                        if (!path.equals("") && !path.startsWith("#")) {
                            try {
                                int code = verifyHTTP(address.startsWith("http") ? address + "/" + path : "http://" + address + "/" + path);
                                if (code != 404) { preview.append(code + " /" + path, null); }
                            } catch (IOException e) { }
                        }
                    } else { break; }
                }
                preview.setTicker(null);
                if (keep) { } else { trace.remove(PID); }
                return;
            }
            else if (MOD == BIND) {
                if (sessions.containsKey(port)) { echoCommand("[-] Port '" + port + "' is unavailable"); return; }

                Hashtable proc = genprocess(proc_name, root, null);
                proc.put("port", port); trace.put(PID, proc); sessions.put(port, "nobody");

                while (trace.containsKey(PID)) {
                    try {
                        server = (ServerSocketConnection) Connector.open("socket://:" + port); proc.put("server", server); 
                        if (COUNT == 1) { echoCommand("[+] listening on port " + port); MIDletLogs("add info Server listening on port " + port); COUNT++; }

                        CONN = (SocketConnection) server.acceptAndOpen();
                        address = CONN.getAddress(); echoCommand("[+] " + address + " connected");

                        IN = CONN.openInputStream(); OUT = CONN.openOutputStream();
                        proc.put("in-stream", IN); proc.put("out-stream", OUT);

                        sessions.put(port, address);
                        while (trace.containsKey(PID)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead = IN.read(buffer);
                            if (bytesRead == -1) { echoCommand("[-] " + address + " disconnected"); break; }
                            String PAYLOAD = new String(buffer, 0, bytesRead).trim();
                            echoCommand("[+] " + address + " -> " + env(PAYLOAD));

                            String command = (DB == null || DB.length() == 0 || DB.equals("null")) ? PAYLOAD : DB + " " + PAYLOAD;

                            String before = stdout != null ? stdout.getText() : "";
                            processCommand(command, true, root);
                            String after = stdout != null ? stdout.getText() : "";

                            String output = after.length() >= before.length() ? after.substring(before.length()).trim() + "\n" : after + "\n";

                            OUT.write(output.getBytes()); OUT.flush();
                        }
                    } 
                    catch (IOException e) { echoCommand("[-] " + getCatch(e)); if (COUNT == 1) { echoCommand("[-] Server crashed"); break; } } 
                    finally {
                        try { if (IN != null) IN.close(); } catch (IOException e) { }
                        try { if (OUT != null) OUT.close(); } catch (IOException e) { }
                        try { if (CONN != null) CONN.close(); } catch (IOException e) { }
                        try { if (server != null) server.close(); } catch (IOException e) { }
                        
                        sessions.put(port, "nobody");
                    }
                } 
                trace.remove(PID); sessions.remove(port);
                echoCommand("[-] Server stopped");
                MIDletLogs("add info Server was stopped");
            }
            else if (MOD == BG) { processCommand(command, ignore, root); }
            else if (MOD == ADDON) { while (trace.containsKey(PID)) { if (processCommand(command, true, root) != 0) { kill(PID, false, root); } } }
        }

        private void reload() { if (attributes.containsKey("J2EMU")) { new MIDletControl(MOD == MONITOR ? "monitor" : MOD == PROCESS ? "process" : MOD == EXPLORER ? "dir" : "history", root); } else { load(); } }
        private void load() {
            if (MOD == HISTORY) { preview.deleteAll(); for (int i = 0; i < history.size(); i++) { preview.append((String) history.elementAt(i), null); } } 
            else if (MOD == EXPLORER) {
                if (attributes.containsKey("J2EMU")) { }
                else { preview.setTitle(path); }

                preview.deleteAll();
                if (path.equals("/")) { }
                else { preview.append("..", null); }

                try {
                    if (path.equals("/tmp/")) { for (Enumeration KEYS = tmp.keys(); KEYS.hasMoreElements();) { String file = (String) KEYS.nextElement(); if (!file.startsWith(".")) { preview.append(file, null); } } }
                    else if (path.equals("/mnt/")) { for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { preview.append((String) roots.nextElement(), null); } } 
                    else if (path.startsWith("/mnt/")) {
                        FileConnection CONN = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ);
                        Vector dirs = new Vector(), files = new Vector();

                        for (Enumeration content = CONN.list(); content.hasMoreElements();) {
                            String name = (String) content.nextElement();

                            if (name.endsWith("/")) { dirs.addElement(name); }
                            else { files.addElement(name); }
                        }

                        while (!dirs.isEmpty()) { preview.append(getFirstString(dirs), null); }
                        while (!files.isEmpty()) { preview.append(getFirstString(files), null); }

                        CONN.close();
                    } 
                    else if (path.startsWith("/home/")) {
                        String[] recordStores = RecordStore.listRecordStores();

                        for (int i = 0; i < recordStores.length; i++) {
                            if (!recordStores[i].startsWith(".")) { preview.append(recordStores[i], null); }
                        }
                    }

                    String[] files = (String[]) paths.get(path);
                    if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                            String f = files[i];

                            if (f != null && !f.equals("..") && !f.equals("/")) { preview.append(f, null); }
                        }
                    }
                } catch (IOException e) { }
            } 
            else if (MOD == MONITOR) { console.setText("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\n" + "Free Memory: " + runtime.freeMemory() / 1024 + " KB\n" + "Total Memory: " + runtime.totalMemory() / 1024 + " KB"); } 
            else if (MOD == PROCESS) { preview.deleteAll(); for (Enumeration keys = trace.keys(); keys.hasMoreElements();) { String PID = (String) keys.nextElement(), name = (String) ((Hashtable) trace.get(PID)).get("name"); if (pfilter.equals("") || name.indexOf(pfilter) != -1) { preview.append(PID + "\t" + name, null); } } }
        }

        private void back() { if (trace.containsKey(PID) && !asked) { confirm = new Alert("Background Process", "Keep this process running in background?", null, AlertType.WARNING); confirm.addCommand(YES = new Command("Yes", Command.OK, 1)); confirm.addCommand(NO = new Command("No", Command.BACK, 1)); confirm.setCommandListener(this); asked = true; display.setCurrent(confirm); } else { processCommand("xterm"); } }

        private int verifyHTTP(String fullUrl) throws IOException { HttpConnection H = null; try { H = (HttpConnection) Connector.open(fullUrl); H.setRequestMethod(HttpConnection.GET); return H.getResponseCode(); } finally { try { if (H != null) H.close(); } catch (IOException x) { } } }
        public static String passwd() { try { RecordStore RMS = RecordStore.openRecordStore("OpenRMS", true); if (RMS.getNumRecords() >= 2) { byte[] data = RMS.getRecord(2); if (data != null) { return new String(data); } } if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return ""; }

        public int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = replace(mode, "password", "").trim(); } int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY; return password ? (base | TextField.PASSWORD) : base; } 
        private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; } 
        private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); } 
        private String getenv(String key) { return env(getvalue(key, "")); } 
    }
    // |
    // MIDlet Shell
    public int processCommand(String command) { return processCommand(command, true, false); }
    public int processCommand(String command, boolean ignore) { return processCommand(command, ignore, false); }
    public int processCommand(String command, boolean ignore, boolean root) { 
        command = command.startsWith("exec") ? command.trim() : env(command.trim());
        String mainCommand = getCommand(command), argument = getpattern(getArgument(command));
        String[] args = splitArgs(getArgument(command));

        if (username.equals("root")) { root = true; }

        if (mainCommand.equals("")) { }

        // API 001 - (Registry)
        // |
        // (Calls)
        else if (shell.containsKey(mainCommand) && ignore) { Hashtable args = (Hashtable) shell.get(mainCommand); if (argument.equals("")) { return processCommand(aliases.containsKey(mainCommand) ? (String) aliases.get(mainCommand) : "true", ignore, root); } else if (args.containsKey(getCommand(argument).toLowerCase())) { return processCommand((String) args.get(getCommand(argument)) + " " + getArgument(argument), ignore, root); } else { return processCommand(args.containsKey("shell.unknown") ? (String) args.get(getCommand("shell.unknown")) + " " + getArgument(argument) : "echo " + mainCommand + ": " + getCommand(argument) + ": not found", args.containsKey("shell.unknown") ? true : false, root); } }
        else if (aliases.containsKey(mainCommand) && ignore) { return processCommand((String) aliases.get(mainCommand) + " " + argument, ignore, root); }
        else if (functions.containsKey(mainCommand) && ignore) { return runScript((String) functions.get(mainCommand)); }
        //else if (paths.containsKey("/bin/") && indexOf(mainCommand, (String[]) paths.get("/bin/")) != -1) { processCommand(". /bin/" + command); }
        // |
        // Aliases
        else if (mainCommand.equals("alias")) { if (argument.equals("")) { for (Enumeration KEYS = aliases.keys(); KEYS.hasMoreElements();) { String KEY = (String) KEYS.nextElement(), VALUE = (String) aliases.get(KEY); if (!KEY.equals("xterm") && !VALUE.equals("")) { echoCommand("alias " + KEY + "='" + VALUE.trim() + "'"); } } } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { for (int i = 0; i < args.length; i++) { if (aliases.containsKey(args[i])) { echoCommand("alias " + args[i] + "='" + (String) aliases.get(args[i]) + "'"); } else { echoCommand("alias: " + argument + ": not found"); return 127; } } } else { aliases.put(argument.substring(0, INDEX).trim(), getpattern(argument.substring(INDEX + 1).trim())); } } }  
        else if (mainCommand.equals("unalias")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { if (aliases.containsKey(args[i])) { aliases.remove(args[i]); } else { echoCommand("unalias: "+ args[i] + ": not found"); return 127; } } } }
        // |
        // Environment Keys
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { for (int i = 0; i < args.length; i++) { attributes.put(args[i], ""); } } else { attributes.put(argument.substring(0, INDEX).trim(), getpattern(argument.substring(INDEX + 1).trim())); } } } 
        else if (mainCommand.equals("unset")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { if (attributes.containsKey(args[i])) { attributes.remove(args[i]); } else { } } } }
        else if (mainCommand.equals("export")) { return processCommand(argument.equals("") ? "env" : "set " + argument, false); }
        else if (mainCommand.equals("env")) { if (argument.equals("")) { for (Enumeration KEYS = attributes.keys(); KEYS.hasMoreElements();) { String KEY = (String) KEYS.nextElement(), VALUE = (String) attributes.get(KEY); if (!KEY.equals("OUTPUT") && !VALUE.equals("")) { echoCommand(KEY + "=" + VALUE.trim()); } } } else { for (int i = 0; i < args.length; i++) { if (attributes.containsKey(args[i])) { echoCommand(args[i] + "=" + (String) attributes.get(args[i])); } else { echoCommand("env: " + args[i] + ": not found"); return 127; } } } }

        // API 002 - (Logs)
        // |
        // OpenTTY Logging Manager
        else if (mainCommand.equals("log")) { return MIDletLogs(argument); } 
        else if (mainCommand.equals("logcat")) { echoCommand(logs); }

        // API 003 - (User-Integration)
        // |
        // Session
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { echoCommand(root == true ? "root": username); }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { return argument.equals("") ? processCommand("import /bin/sh", false, root) : runScript(getcontent(argument), true); }
        else if (mainCommand.equals("sudo")) { if (argument.equals("")) { } else if (root) { return processCommand(argument, ignore, root); } else { new MIDletControl(argument); } }
        else if (mainCommand.equals("su")) { if (root) { username = username.equals("root") ? loadRMS("OpenRMS") : "root"; processCommand("sh", false); } else { echoCommand("su: permission denied"); return 13; } }
        else if (mainCommand.equals("passwd")) { if (argument.equals("")) { } else { if (root) { writeRMS("OpenRMS", String.valueOf(argument.hashCode()).getBytes(), 2); } else { echoCommand("passwd: permission denied"); return 13; } } }
        else if (mainCommand.equals("logout")) { if (loadRMS("OpenRMS").equals(username)) { if (root) { writeRMS("/home/OpenRMS", ""); processCommand("exit", false); } else { echoCommand("logout: permission denied"); return 13; } } else { username = loadRMS("OpenRMS"); processCommand("sh", false); } }
        else if (mainCommand.equals("exit") || mainCommand.equals("quit")) { if (loadRMS("OpenRMS").equals(username)) { writeRMS("/home/nano", nanoContent); notifyDestroyed(); } else { username = loadRMS("OpenRMS"); processCommand("sh", false); } }
        
        // API 004 - (LCDUI Interface)
        // |
        // System UI
        else if (mainCommand.equals("xterm")) { display.setCurrent(form); }
        else if (mainCommand.equals("x11")) { return xserver(argument, root); }
        else if (mainCommand.equals("warn")) { return warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("title")) { form.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument.equals("hide") ? null : argument); }
        else if (mainCommand.equals("tick")) { if (argument.equals("label")) { echoCommand(display.getCurrent().getTicker().getString()); } else { return xserver("tick " + argument, root); } }

        // API 005 - (Operators)
        // |
        // Operators
        else if (mainCommand.equals("if") || mainCommand.equals("for") || mainCommand.equals("case")) { return mainCommand.equals("if") ? ifCommand(argument, ignore, root) : mainCommand.equals("for") ? forCommand(argument, ignore, root) : caseCommand(argument, ignore, root); }
        // |
        // Long executors
        else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { return processCommand(argument, false, root); }
        else if (mainCommand.equals("bruteforce")) { String PID = genpid(); start("bruteforce", PID, null, root); while (trace.containsKey(PID)) { int STATUS = processCommand(argument, ignore, root); if (STATUS != 0) { kill(PID, false, root); return STATUS; } } }
        else if (mainCommand.equals("cron")) { if (argument.equals("")) { } else { return processCommand("execute sleep " + getCommand(argument) + "; " + getArgument(argument), ignore, root); } }
        else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (Exception e) { echoCommand(getCatch(e)); return 2; } } }
        else if (mainCommand.equals("time")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); int STATUS = processCommand(argument, ignore, root); echoCommand("at " + (System.currentTimeMillis() - START) + "ms"); return STATUS; } } 
        // |
        // Chain executors
        else if (mainCommand.startsWith("exec")) { String[] CMDS = split(argument, mainCommand.equals("exec") ? '&' : ';'); for (int i = 0; i < CMDS.length; i++) { int STATUS = processCommand(CMDS[i].trim(), ignore, root); if (STATUS != 0) { return STATUS; } } }

        // API 006 - (Process)
        // |
        // Memory
        else if (mainCommand.equals("gc")) { System.gc(); } 
        else if (mainCommand.equals("htop")) { new MIDletControl(MIDletControl.LIST, "list.content=Monitor,Process\nlist.button=Open\nMonitor=execute top;\nProcess=execute top process;", root); }
        else if (mainCommand.equals("top") || mainCommand.equals("trace")) { return kernel(argument, root); }
        // |
        // Process 
        else if (mainCommand.equals("start") || mainCommand.equals("stop") || mainCommand.equals("kill")) { for (int i = 0; i < args.length; i++) { int STATUS = mainCommand.equals("start") ? start(args[i], genpid(), null, root) : mainCommand.equals("stop") ? stop(args[i], root) : kill(args[i], true, root); if (STATUS != 0) { return STATUS; } } } 
        else if (mainCommand.equals("ps")) { echoCommand("PID\tPROCESS"); for (Enumeration KEYS = trace.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); echoCommand(PID + "\t" + (String) ((Hashtable) trace.get(PID)).get("name")); } }

        // API 007 - (Bundle)
        // |
        // Properties
        else if (mainCommand.equals("pkg")) { echoCommand(argument.equals("") ? getAppProperty("MIDlet-Name") : argument.startsWith("/") ? System.getProperty(replace(argument, "/", "")) : getAppProperty(argument)); }
        else if (mainCommand.equals("uname")) { String INFO = argument.equals("") || argument.equals("-i") ? "$TYPE" : argument.equals("-a") || argument.equals("--all") ? "$TYPE (OpenTTY $VERSION) main/$RELEASE " + build + " - $CONFIG $PROFILE" : argument.equals("-r") || argument.equals("--release") ? "$VERSION" : argument.equals("-v") || argument.equals("--build") ? build : argument.equals("-s") ? "J2ME" : argument.equals("-m") ? "$PROFILE" : argument.equals("-p") ? "$CONFIG" : argument.equals("-n") ? "$HOSTNAME" : null; if (INFO == null) { echoCommand("uname: " + argument + ": not found"); return 127; } else { echoCommand(env(INFO)); } }
        // |
        // Device ID
        else if (mainCommand.equals("hostname")) { processCommand(argument.equals("") ? "echo $HOSTNAME" : "set HOSTNAME=" + getCommand(argument), false); }
        else if (mainCommand.equals("hostid")) { String DATA = System.getProperty("microedition.platform") + System.getProperty("microedition.configuration") + System.getProperty("microedition.profiles"); int HASH = 7; for (int i = 0; i < DATA.length(); i++) { HASH = HASH * 31 + DATA.charAt(i); } echoCommand(Integer.toHexString(HASH).toLowerCase()); }

        // API 008 - (Logic I/O) Text
        // |
        // TTY
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(stdout.getText().length() + " B"); }
        else if (mainCommand.equals("stty")) { 
            if (argument.equals("")) { echoCommand("" + TTY_MAX_LEN); }
            else if (argument.indexOf("=") != -1) {
                
            }
            else {
                String source = getcontent(argument);
                if (source.equals("")) { return 2; }

                attributes.put("TTY", argument.equals("nano") ? "nano" : argument.startsWith("/") ? argument : path + "/" + argument);

                Hashtable TTY = parseProperties(source);
                
                if (TTY.containsKey("max-length")) { try { TTY_MAX_LEN = Integer.parseInt((String) TTY.get("max-length")); } catch (Exception e) { notifyDestroyed(); } }
            } 
        }
        // |
        // Text related commands
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("buff")) { stdin.setString(argument); }
        else if (mainCommand.equals("uuid")) { echoCommand(generateUUID()); }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("expr")) { echoCommand(exprCommand(argument)); }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
        else if (mainCommand.equals("getopt")) { echoCommand(getArgument(argument)); }
        else if (mainCommand.equals("trim")) { stdout.setText(stdout.getText().trim()); }
        else if (mainCommand.equals("date")) { echoCommand(new java.util.Date().toString()); }
        else if (mainCommand.equals("clear")) { if (argument.equals("") || argument.equals("stdout")) { stdout.setText(""); } else if (argument.equals("stdin")) { stdin.setString(""); } else if (argument.equals("history")) { getprocess("1").put("history", new Vector()); } else if (argument.equals("logs")) { logs = ""; } else { echoCommand("clear: " + argument + ": not found"); return 127; } }
        else if (mainCommand.equals("seed")) { try { echoCommand("" + random.nextInt(Integer.parseInt(argument)) + ""); } catch (NumberFormatException e) { echoCommand(getCatch(e)); return 2; } }

        // API 009 - (Threads)
        // |
        // MIDlet Tracker
        else if (mainCommand.equals("throw")) { Thread.currentThread().interrupt(); }
        else if (mainCommand.equals("mmspt") || mainCommand.equals("chrt")) {
            if (argument.equals("")) { echoCommand(getThreadName(Thread.currentThread())); }
            else if (argument.equals("priority")) { echoCommand("" + Thread.currentThread().getPriority()); }
            else { 
                int value = getNumber(argument, Thread.NORM_PRIORITY, true); 
                if (value > 10 || value < 1) { return 2; }
                else { Thread.currentThread().setPriority(value); }
            }
            
        }
        else if (mainCommand.equals("bg")) { if (argument.equals("")) { } else { new MIDletControl("Background", argument, ignore, root); } }

        // API 010 - (Requests)
        // |
        // Connecting to Device API
        else if (mainCommand.equals("call")) { if (argument.equals("")) { } else { try { platformRequest("tel:" + argument); } catch (Exception e) { } } }
        else if (mainCommand.equals("open")) { if (argument.equals("")) { } else { try { platformRequest(argument); } catch (Exception e) { echoCommand("open: " + argument + ": not found"); return 127; } } }
        // |
        // PushRegistry
        else if (mainCommand.equals("prg")) { if (argument.equals("")) { argument = "5"; } try { PushRegistry.registerAlarm(getArgument(argument).equals("") ? "OpenTTY" : getArgument(argument), System.currentTimeMillis() + Integer.parseInt(getCommand(argument)) * 1000); } catch (ClassNotFoundException e) { echoCommand("prg: " + getArgument(argument) + ": not found"); return 127; } catch (NumberFormatException e) { echoCommand(getCatch(e)); return 2; } catch (Exception e) { echoCommand(getCatch(e)); return 3; } }

        // API 011 - (Network)
        // |
        // HTTP Interfaces
        else if (mainCommand.equals("pong")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); CONN.close(); echoCommand("Pong to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms"); } catch (IOException e) { echoCommand("Pong to " + argument + " failed: " + getCatch(e)); return 101; } } }
        else if (mainCommand.equals("ping")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); try { HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? "http://" + argument : argument); CONN.setRequestMethod(HttpConnection.GET); int responseCode = CONN.getResponseCode(); CONN.close(); echoCommand("Ping to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms"); } catch (IOException e) { echoCommand("Ping to " + argument + " failed: " + getCatch(e)); return 101; } } }
        else if (mainCommand.equals("curl") || mainCommand.equals("wget") || mainCommand.equals("clone") || mainCommand.equals("proxy")) { if (argument.equals("")) { } else { String URL = getCommand(argument); if (mainCommand.equals("clone") || mainCommand.equals("proxy")) { URL = getAppProperty("MIDlet-Proxy") + URL; } Hashtable HEADERS = getArgument(argument).equals("") ? null : parseProperties(getcontent(getArgument(argument))); String RESPONSE = request(URL, HEADERS); if (mainCommand.equals("curl")) { echoCommand(RESPONSE); } else if (mainCommand.equals("wget") || mainCommand.equals("proxy")) { nanoContent = RESPONSE; } else if (mainCommand.equals("clone")) { return runScript(RESPONSE, root); } } } 
        // |
        // Socket Interfaces
        else if (mainCommand.equals("gaddr")) { return GetAddress(argument); }
        else if (mainCommand.equals("query")) { return query(argument, root); }
        else if (mainCommand.equals("nc") || mainCommand.equals("prscan") || mainCommand.equals("gobuster") || mainCommand.equals("bind")) { new MIDletControl(mainCommand, argument, root); }
        // |
        else if (mainCommand.equals("wrl")) { return wireless(argument); }
        else if (mainCommand.equals("who")) { echoCommand("PORT\tADDRESS"); Hashtable sessions = (Hashtable) getobject("1", "sessions"); boolean all = argument.indexOf("-a") != -1; for (Enumeration KEYS = sessions.keys(); KEYS.hasMoreElements();) { String PORT = (String) KEYS.nextElement(), ADDR = (String) sessions.get(PORT); if (!all && (ADDR.equals("http-cli") || ADDR.equals("nobody"))) { } else { echoCommand(PORT + "\t" + ADDR); } } }
        // |
        // IP Tools
        else if (mainCommand.equals("fw")) { echoCommand(request("http://ipinfo.io/" + (argument.equals("") ? "json" : argument))); }
        else if (mainCommand.equals("genip")) { echoCommand(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256)); }
        else if (mainCommand.equals("ifconfig")) { if (argument.equals("")) { argument = "1.1.1.1:53"; } try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); echoCommand(CONN.getLocalAddress()); CONN.close(); } catch (Exception e) { echoCommand("null"); return 101; } }
        // |
        else if (mainCommand.equals("report")) { return processCommand("open mailto:felipebr4095@gmail.com"); }
        else if (mainCommand.equals("mail")) { echoCommand(request(getAppProperty("MIDlet-Proxy") + "raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/root/mail.txt")); } 
        else if (mainCommand.equals("netstat")) { if (argument.equals("")) { argument = "http://ipinfo.io"; } int STATUS = 0; try { HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? "http://" + argument : argument); CONN.setRequestMethod(HttpConnection.GET); if (CONN.getResponseCode() == HttpConnection.HTTP_OK) { } else { STATUS = 101; } CONN.close(); } catch (SecurityException e) { STATUS = 13; } catch (Exception e) { STATUS = 101; } echoCommand(STATUS == 0 ? "true" : "false"); return STATUS; }
        
        // API 012 - (File)
        // |
        // Directories Manager
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("dir")) { new MIDletControl("dir", root); }
        else if (mainCommand.equals("umount")) { paths = new Hashtable(); }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { mount(getcontent(argument)); } }
        else if (mainCommand.equals("cd") || mainCommand.equals("pushd")) { 
            String old_pwd = path;
            if (argument.equals("") && mainCommand.equals("cd")) { path = "/home/"; } 
            else if (argument.equals("")) { echoCommand(readStack() == null || readStack().length() == 0 ? "pushd: missing directory": readStack()); }
            else if (argument.equals("..")) { 
                if (path.equals("/")) { return 0; } 

                int lastSlashIndex = path.lastIndexOf('/', path.endsWith("/") ? path.length() - 2 : path.length() - 1); 
                path = (lastSlashIndex <= 0) ? "/" : path.substring(0, lastSlashIndex + 1); 
            } 
            else { 
                String TARGET = argument.startsWith("/") ? argument : (path.endsWith("/") ? path + argument : path + "/" + argument); 
                if (!TARGET.endsWith("/")) { TARGET += "/"; } 
                if (paths.containsKey(TARGET)) { path = TARGET; } 

                else if (TARGET.startsWith("/mnt/")) { 
                    try { 
                        FileConnection fc = (FileConnection) Connector.open("file:///" + TARGET.substring(5), Connector.READ); 
                        if (fc.exists() && fc.isDirectory()) { path = TARGET; } 
                        else { echoCommand(mainCommand + ": " + basename(TARGET) + ": not " + (fc.exists() ? "a directory" : "found")); return 127; } 

                        fc.close(); 
                    } 
                    catch (IOException e) { 
                        echoCommand(mainCommand + ": " + basename(TARGET) + ": " + getCatch(e)); 

                        return 1; 
                    } 
                } 
                else if (TARGET.startsWith("/proc/")) {
                    String[] parts = split(TARGET.substring(6), '/');
                    if (parts.length < 1) { echoCommand(mainCommand + ": not found"); return 127; }

                    String pid = parts[0];
                    Hashtable proc = getprocess(pid);
                    Object current = proc;

                    for (int i = 1; i < parts.length; i++) {
                        if (current instanceof Hashtable) {
                            current = ((Hashtable) current).get(parts[i]);

                            if (current == null) { echoCommand(mainCommand + ": " + parts[i] + ": not found"); return 127; }
                        } else { echoCommand(mainCommand + ": " + parts[i] + ": not a directory"); return 127; }
                    }

                    path = TARGET.endsWith("/") ? TARGET : TARGET + "/";
                }
                else { echoCommand(mainCommand + ": " + basename(TARGET) + ": not accessible"); return 127; } 

            } 

            if (mainCommand.equals("pushd")) { ((Vector) getobject("1", "stack")).addElement(old_pwd); echoCommand(readStack()); }
        }
        else if (mainCommand.equals("popd")) { 
            Vector stack = (Vector) getobject("1", "stack");
            if (stack.isEmpty()) { echoCommand("popd: empty stack"); } 
            else { path = (String) stack.lastElement(); stack.removeElementAt(stack.size() - 1); echoCommand(readStack()); } 
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
                else if (PWD.equals("/home/") && verbose) { 
                    String[] FILES = RecordStore.listRecordStores(); 
                    if (FILES != null) { 
                        for (int i = 0; i < FILES.length; i++) { 
                            String NAME = FILES[i]; 
                            if ((all || !NAME.startsWith(".")) && !BUFFER.contains(NAME)) { BUFFER.addElement(NAME); } 
                        } 
                    } 
                } 
                else if (PWD.equals("/home/")) { return processCommand("dir", false, root); }
            } catch (IOException e) { } 

            String[] FILES = (String[]) paths.get(PWD); 
            if (FILES != null) { 
                for (int i = 0; i < FILES.length; i++) { 
                    String file = FILES[i].trim(); 
                    if (file == null || file.equals("..") || file.equals("/")) continue; 
                    if ((all || !file.startsWith(".")) && !BUFFER.contains(file) && !BUFFER.contains(file + "/")) { 
                        BUFFER.addElement(file); 
                    } 
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
                echoCommand(formatted.trim()); 
            } 
        }
        // |
        // Device Files
        else if (mainCommand.equals("fdisk")) { return processCommand("ls /mnt/", false, root); }
        else if (mainCommand.equals("lsblk")) { if (argument.equals("") || argument.equals("-x")) { echoCommand(replace("MIDlet.RMS.Storage", ".", argument.equals("-x") ? ";" : "\t")); } else { echoCommand("lsblk: " + argument + ": not found"); return 127; } }
        // |
        // RMS Files
        else if (mainCommand.equals("rm")) { if (argument.equals("")) { } else { for (int i = 0; i < args.length; i++) { int STATUS = deleteFile(argument); if (STATUS != 0) { return STATUS; } } } }
        else if (mainCommand.equals("install")) { if (argument.equals("")) { } else { return writeRMS(argument, nanoContent); } }
        else if (mainCommand.equals("touch")) { if (argument.equals("")) { nanoContent = ""; } else { for (int i = 0; i < args.length; i++) { int STATUS = writeRMS(argument, ""); if (STATUS != 0) { return STATUS; } } } }
        else if (mainCommand.equals("mkdir")) { if (argument.equals("")) { } else { argument = argument.endsWith("/") ? argument : argument + "/"; argument = argument.startsWith("/") ? argument : path + argument; if (argument.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.mkdir(); CONN.close(); } else { echoCommand("mkdir: " + basename(argument) + ": found"); } CONN.close(); } catch (Exception e) { echoCommand(getCatch(e)); return (e instanceof SecurityException) ? 13 : 1; } } else if (argument.startsWith("/home/") || argument.startsWith("/tmp/")) { echoCommand("Unsupported API"); return 3; } else if (argument.startsWith("/")) { echoCommand("read-only storage"); return 5; } } }
        else if (mainCommand.equals("cp")) {
            if (argument.equals("")) { echoCommand("cp: missing [origin]"); } 
            else {
                try {
                    String origin = args[0], target = (args.length > 1 && !args[1].equals("")) ? args[1] : origin + "-copy";

                    InputStream in = readRaw(origin);
                    if (in == null) { echoCommand("cp: cannot open " + origin); return 1; }

                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    byte[] tmpBuf = new byte[4096];
                    int len;
                    while ((len = in.read(tmpBuf)) != -1) { buffer.write(tmpBuf, 0, len); }
                    in.close();

                    return writeRMS(target, buffer.toByteArray());
                } catch (Exception e) { echoCommand("cp: " + getCatch(e)); return getCatch(e, 1); }
            }
        }
        // |
        // Text Manager
        else if (mainCommand.equals("rraw")) { stdout.setText(nanoContent); }
        else if (mainCommand.equals("sed")) { return StringEditor(argument); }
        else if (mainCommand.equals("getty")) { nanoContent = stdout.getText(); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } 
        else if (mainCommand.equals("du")) { if (argument.equals("")) { } else { processCommand("wc -c " + argument, false); } }
        else if (mainCommand.equals("hash")) { if (argument.equals("")) { } else { echoCommand("" + getcontent(argument).hashCode()); } }
        else if (mainCommand.equals("cat") || mainCommand.equals("raw")) { if (argument.equals("")) { echoCommand(nanoContent); } else { for (int i = 0; i < args.length; i++) { echoCommand(getcontent(args[i])); } } }
        else if (mainCommand.equals("get")) { nanoContent = argument.equals("") || argument.equals("nano") ? loadRMS("nano") : getcontent(argument); }
        else if (mainCommand.equals("read")) { if (argument.equals("") || args.length < 2) { return 2; } else { attributes.put(args[0], getcontent(args[1])); } }
        else if (mainCommand.equals("grep")) { if (argument.equals("") || args.length < 2) { return 2; } else { echoCommand(getcontent(args[1]).indexOf(args[0]) != -1 ? "true" : "false"); } }
        else if (mainCommand.equals("find")) { if (argument.equals("") || args.length < 2) { return 2; } else { String VALUE = (String) parseProperties(getcontent(args[1])).get(args[0]); echoCommand(VALUE != null ? VALUE : "null"); } }
        else if (mainCommand.equals("head")) { if (argument.equals("")) { } else { String CONTENT = getcontent(args[0]); String[] LINES = split(CONTENT, '\n'); int COUNT = Math.min(args.length > 1 ? getNumber(args[1], 10, false) : 10, LINES.length); for (int i = 0; i < COUNT; i++) { echoCommand(LINES[i]); } } }
        else if (mainCommand.equals("tail")) { if (argument.equals("")) { } else { String CONTENT = getcontent(args[0]); String[] LINES = split(CONTENT, '\n'); int COUNT = args.length > 1 ? getNumber(args[1], 10, false) : 10; COUNT = Math.max(0, LINES.length - COUNT); for (int i = COUNT; i < LINES.length; i++) { echoCommand(LINES[i]); } } }
        else if (mainCommand.equals("diff")) { if (argument.equals("") || args.length < 2) { return 2; } else { String[] LINES1 = split(getcontent(args[0]), '\n'), LINES2 = split(getcontent(args[1]), '\n'); int MAX_RANGE = Math.max(LINES1.length, LINES2.length); for (int i = 0; i < MAX_RANGE; i++) { String LINE1 = i < LINES1.length ? LINES1[i] : "", LINE2 = i < LINES2.length ? LINES2[i] : ""; if (!LINE1.equals(LINE2)) { echoCommand("--- Line " + (i + 1) + " ---\n< " + LINE1 + "\n" + "> " + LINE2); } if (i > LINES1.length || i > LINES2.length) { break; } } } }
        else if (mainCommand.equals("wc")) { if (argument.equals("")) { } else { int MODE = args[0].indexOf("-c") != -1 ? 1 : args[0].indexOf("-w") != -1 ? 2 : args[0].indexOf("-l") != -1 ? 3 : 0; if (MODE != 0) { argument = join(args, " ", 1); } String CONTENT = getcontent(argument), FILENAME = basename(argument); int LINES = 0, WORDS = 0, CHARS = CONTENT.length(); String[] LINE_ARRAY = split(CONTENT, '\n'); LINES = LINE_ARRAY.length; for (int i = 0; i < LINE_ARRAY.length; i++) { String[] WORD_ARRAY = split(LINE_ARRAY[i], ' '); for (int j = 0; j < WORD_ARRAY.length; j++) { if (!WORD_ARRAY[j].trim().equals("")) { WORDS++; } } } echoCommand(MODE == 0 ? LINES + "\t" + WORDS + "\t" + CHARS + "\t" + FILENAME : MODE == 1 ? CHARS + "\t" + FILENAME : MODE == 2 ? WORDS + "\t" + FILENAME : LINES + "\t" + FILENAME); } }
        // |
        // Text Parsers
        else if (mainCommand.equals("pjnc")) { nanoContent = parseJson(nanoContent); }
        else if (mainCommand.equals("pinc")) { nanoContent = parseConf(nanoContent); }
        else if (mainCommand.equals("conf")) { echoCommand(parseConf(argument.equals("") ? nanoContent : getcontent(argument))); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(argument.equals("") ? nanoContent : getcontent(argument))); }
        else if (mainCommand.equals("ph2s")) { Vector history = (Vector) getobject("1", "history"); StringBuffer BUFFER = new StringBuffer(); for (int i = 0; i < history.size() - 1; i++) { BUFFER.append(history.elementAt(i)); if (i < history.size() - 1) { BUFFER.append("\n"); } } String script = "#!/bin/sh\n\n" + BUFFER.toString(); if (argument.equals("") || argument.equals("nano")) { nanoContent = script; } else { writeRMS(argument, script); } }
        // |
        // Interfaces
        else if (mainCommand.equals("nano")) { nano.setString(argument.equals("") ? nanoContent : getcontent(argument)); display.setCurrent(nano); }
        else if (mainCommand.equals("view")) { if (argument.equals("")) { } else { viewer(extractTitle(env(argument), form.getTitle()), html2text(env(argument))); } }
        else if (mainCommand.equals("html")) { viewer(extractTitle(env(nanoContent), "HTML Viewer"), html2text(env(nanoContent))); }
        // |
        // Audio Manager
        else if (mainCommand.equals("audio")) { return audio(argument, root); }
        
        // API 013 - (MIDlet)
        // |
        // Java Runtime
        else if (mainCommand.equals("java")) { return java(argument); }
        // |
        // Permissions
        else if (mainCommand.equals("chmod")) { 
            if (argument.equals("")) { } 
            else if (argument.equals("*")) { return processCommand("chmod http socket file prg", false, root); }
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
                    else { echoCommand("chmod: " + args[i] + ": not found"); return 127; } 
                    
                    if (STATUS == 0) { MIDletLogs("add info Permission '" + NODE + "' granted"); } 
                    else if (STATUS == 1) { MIDletLogs("add debug Permission '" + NODE + "' granted with exceptions"); } 
                    else if (STATUS == 13) { MIDletLogs("add error Permission '" + NODE + "' denied"); } 
                    else if (STATUS == 3) { MIDletLogs("add warn Unsupported API '" + NODE + "'"); } 
                    
                    if (STATUS > 1) { break; } 
                }
                return STATUS; 
            } 
        }
        // |
        // General Utilities
        else if (mainCommand.equals("history")) { new MIDletControl("history", root); }
        else if (mainCommand.equals("help")) { viewer(form.getTitle(), read("/res/docs")); }
        else if (mainCommand.equals("man")) { boolean verbose = argument.indexOf("-v") != -1; argument = replace(argument, "-v", "").trim(); if (argument.equals("")) { argument = "sh"; } String content = loadRMS("man.html"); if (content.equals("") || argument.equals("--update")) { int STATUS = processCommand("netstat", false); if (STATUS == 0) { STATUS = processCommand("execute install /home/nano; tick Downloading...; proxy github.com/mrlima4095/OpenTTY-J2ME/raw/refs/heads/main/assets/root/man.html; install /home/man.html; get; tick;", false); if (STATUS == 0 && !argument.equals("--update")) { content = read("/home/man.html"); } else { return STATUS; } } else { echoCommand("man: download error"); return STATUS; } } content = extractTag(content, argument.toLowerCase(), ""); if (content.equals("")) { echoCommand("man: " + argument + ": not found"); return 127; } else { if (verbose) { echoCommand(content); } else { viewer(form.getTitle(), content); } } }
        else if (mainCommand.equals("true") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("false")) { return 255; }

        else if (mainCommand.equals("which")) { if (argument.equals("")) { } else { echoCommand(shell.containsKey(argument) ? "shell" : (aliases.containsKey(argument) ? "alias" : (functions.containsKey(argument) ? "function" : ""))); } }

        // API 014 - (OpenTTY)
        // |
        // Low-level commands
        else if (mainCommand.equals("@exec")) { commandAction(EXECUTE, display.getCurrent()); }
        else if (mainCommand.equals("@alert")) { display.vibrate(argument.equals("") ? 500 : getNumber(argument, 0, true) * 100); }
        else if (mainCommand.equals("@reload")) { aliases = new Hashtable(); shell = new Hashtable(); functions = new Hashtable(); username = loadRMS("OpenRMS"); processCommand("execute log add debug API reloaded; x11 stop; x11 init; x11 term; run initd; sh;"); } 
        else if (mainCommand.startsWith("@")) { display.vibrate(500); } 
        
        else if (mainCommand.equals("lua")) { 
            if (javaClass("Lua") == 0) { 
                Lua lua = new Lua(this, root); 
                
                Hashtable arg = new Hashtable();
                String source, code;
                if (argument.equals("")) { source = "nano"; code = nanoContent; arg.put(new Double(0), "nano"); } 
                else if (args[0].equals("-e")) { source = "stdin"; code = argument.substring(3).trim(); arg.put(new Double(0), "/dev/stdin"); } 
                else { source = args[0]; code = getcontent(source); arg.put(new Double(0), source); for (int i = 1; i < args.length; i++) { arg.put(new Double(i), args[i]); } }
                
                return (Integer) lua.run(source, code, arg).get("status"); 
            } else { echoCommand("This MIDlet Build don't have Lua"); return 3; } 
            
        }
        else if (mainCommand.equals("file")) {
            if (argument.equals("")) { }
            else {
                String target = argument.startsWith("/") ? argument : path + argument;
                
                try {
                    if (target.endsWith("/")) { echoCommand(target + ": directory"); }
                    else if (target.startsWith("/home/")) {
                        String filename = target.substring(6);
                        String[] rms = RecordStore.listRecordStores();
                        boolean found = false;
                        if (rms != null) { for (int i = 0; i < rms.length; i++) { if (rms[i].equals(filename)) { found = true; break; } } }

                        if (found) {
                            String[] info = getExtensionInfo(getExtension(filename));
                            echoCommand(argument + ": " + (info[0].equals("Unknown") ? "Plain Text" : info[0]) + ", " + (info[0].equals("Unknown") ? "text" : info[2]));
                        } else {
                            echoCommand(argument + ": not found");
                            return 127;
                        } 
                    }
                    else if (target.startsWith("/mnt/")) {
                        String filename = target.substring(5);
                        FileConnection fc = (FileConnection) Connector.open("file:///" + filename, Connector.READ);
                        if (fc.exists()) {
                            String[] info = getExtensionInfo(getExtension(filename));
                            echoCommand(argument + (fc.isDirectory() ? ": directory" : ": " + info[0] + ", " + info[2]));
                        } 
                        else { echoCommand(argument + ": not found"); fc.close(); return 127; }

                        fc.close();
                    }
                    else if (target.startsWith("/tmp/")) {
                        String filename = target.substring(5);
                        if (tmp.containsKey(filename)) {
                            String[] info = getExtensionInfo(getExtension(filename));
                            echoCommand(argument + ": " + (info[0].equals("Unknown") ? "Plain Text" : info[0]) + ", " + (info[0].equals("Unknown") ? "text" : info[2]));
                        } 
                        else { echoCommand(argument + ": not found"); return 127; }
                    }
                    else if (target.startsWith("/")) {
                        String parent = target.substring(0, target.lastIndexOf('/') + 1);
                        String name   = target.substring(target.lastIndexOf('/') + 1);
                        
                        if (paths.containsKey(target + "/")) { echoCommand(argument + ": directory"); } 
                        else if (paths.containsKey(parent)) {
                            String[] contents = (String[]) paths.get(parent);
                            for (int i = 0; i < contents.length; i++) {
                                if (contents[i].equals(name)) {
                                    if (parent.equals("/bin/")) { echoCommand(argument + ": Application, bin"); } 
                                    else if (parent.equals("/dev/")) { echoCommand(argument + ": special device"); } 
                                    else if (parent.equals("/lib/")) { echoCommand(argument + ": Shared package, text"); }
                                    else { 
                                        String[] info = getExtensionInfo(getExtension(name));
                                        echoCommand(argument + ": " + (info[0].equals("Unknown") ? "ASCII text" : info[0]) + ", " + (info[0].equals("Unknown") ? "text" : info[2])); 
                                    }
                                    return 0;
                                }
                            }
                            
                            echoCommand(argument + ": not found");
                            return 127;
                        } 
                        else { echoCommand(argument + ": not found"); return 127; }
                    }
                    else { echoCommand(argument + ": unknown"); return 127; }
                } catch (Exception e) { echoCommand(getCatch(e)); return e instanceof SecurityException ? 13 : 1; }
            }
        }

        // API 015 - (Scripts)
        // |
        // OpenTTY Packages
        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { return importScript(getcontent(argument), root); }
        else if (mainCommand.equals("function")) { if (argument.equals("")) { } else { int braceIndex = argument.indexOf('{'), braceEnd = argument.lastIndexOf('}'); if (braceIndex != -1 && braceEnd != -1 && braceEnd > braceIndex) { String name = getCommand(argument).trim(); String body = replace(argument.substring(braceIndex + 1, braceEnd).trim(), ";", "\n"); functions.put(name, body); } else { echoCommand("invalid syntax"); return 2; } } }

        else if (mainCommand.equals("eval")) { if (argument.equals("")) { } else { echoCommand("" + processCommand(argument, ignore, root)); } }
        else if (mainCommand.equals("catch")) { if (argument.equals("")) { } else { try { processCommand(argument, ignore, root); } catch (Throwable e) { echoCommand(getCatch(e)); } } }
        else if (mainCommand.equals("return")) { return getNumber(argument, 2, true); }

        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE")); }
        else if (mainCommand.equals("!!")) { stdin.setString((argument.equals("") ? "" : argument + " ") + getLastHistory()); }
        else if (mainCommand.equals(".")) {
            String content = argument.equals("") ? nanoContent : getcontent(args[0]);

            return (content.startsWith("[ Config ]") || content.startsWith("--[[\n\n[ Config ]")) ? importScript(content, root) : content.startsWith("#!/bin/lua") ? (javaClass("Lua") == 0 ? processCommand("lua " + argument, ignore, root) : importScript(content, root)) : runScript(content, root);
        }

        else { echoCommand(mainCommand + ": not found"); return 127; }

        return 0;
    }
    // |
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    // |
    // Readers
    public InputStream readRaw(String filename) throws Exception {
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

            InputStream is = getClass().getResourceAsStream(filename);
            return is;
        }
    }
    public String read(String filename) {
        try {
            if (filename.startsWith("/tmp/")) {
                return tmp.containsKey(filename = filename.substring(5)) ? (String) tmp.get(filename) : "";
            }
            InputStream is = readRaw(filename);
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
    public Image readImg(String filename) { 
        try { 
            if (filename.startsWith("/home/") || filename.startsWith("/tmp/") || filename.startsWith("/mnt/")) {
                InputStream is = readRaw(filename); 
                Image img = Image.createImage(is); 
                is.close(); return img; 
            } else { return Image.createImage(filename); }
        } catch (Exception e) { return Image.createImage(16, 16); } 
    }
    // |
    public String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    public String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) attributes.get(key)); } text = replace(text, "$.", "$"); return escape(text); }
    public String escape(String text) { text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); text = replace(text, "\\b", "\b"); text = replace(text, "\\\\", "\\"); text = replace(text, "\\.", "\\"); return text; }
    private String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(result) < 0) { result = cur; } } v.removeElement(result); return result; } 
    public String getCatch(Throwable e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : message; }
    // |
    public String getcontent(String file) { return file.startsWith("/") ? read(file) : file.equals("nano") ? nanoContent : read(path + file); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }
    // |
    public String join(String[] array, String spacer, int start) { if (array == null || array.length == 0 || start >= array.length) { return ""; } StringBuffer sb = new StringBuffer(); for (int i = start; i < array.length; i++) { sb.append(array[i]).append(spacer); } return sb.toString().trim(); }
    public String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    public String[] splitArgs(String content) { Vector args = new Vector(); boolean inQuotes = false; int start = 0; for (int i = 0; i < content.length(); i++) { char c = content.charAt(i); if (c == '"') { inQuotes = !inQuotes; continue; } if (!inQuotes && c == ' ') { if (i > start) { args.addElement(getpattern(content.substring(start, i))); } start = i + 1; } } if (start < content.length()) { args.addElement(getpattern(content.substring(start))); } String[] result = new String[args.size()]; args.copyInto(result); return result; }
    // |
    public Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (line.startsWith("#")) { } else { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { properties.put(line.substring(0, equalIndex).trim(), getpattern(line.substring(equalIndex + 1).trim())); } } } return properties; }
    public int getNumber(String s, int fallback, boolean print) { try { return Integer.valueOf(s); } catch (Exception e) { if (print) {echoCommand(getCatch(e)); } return fallback; } }
    public Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    // |
    private int indexOf(String key, String[] array) { for (int i = 0; i < array.length; i++) { if (array[i].equals(key)) { return i; } } return -1; }
    private int getCatch(Exception e, int fallback) { return (e instanceof SecurityException) ? 13 : fallback; }

    // API 002 - (Logs)
    // |
    // OpenTTY Logging Manager
    public int MIDletLogs(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { writeRMS(argument.equals("") ? "logs" : argument, logs); } else if (mainCommand.equals("view")) { viewer(form.getTitle(), logs); } else if (mainCommand.equals("add")) { String LEVEL = getCommand(argument).toLowerCase(), MESSAGE = getArgument(argument); if (!MESSAGE.equals("")) { if (LEVEL.equals("info") || LEVEL.equals("warn") || LEVEL.equals("debug") || LEVEL.equals("error")) { logs += "[" + LEVEL.toUpperCase() + "] " + split(new java.util.Date().toString(), ' ')[3] + " " + MESSAGE + "\n"; } else { echoCommand("log: add: " + LEVEL + ": not found"); return 127; } } } else { echoCommand("log: " + mainCommand + ": not found"); return 127; } return 0; }

    // API 004 - (LCDUI Interface)
    // |
    // System UI
    private int xserver(String command, boolean root) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);
        String[] args = splitArgs(getArgument(command));

        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2025-05-04\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); }
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("buffer")) { echoCommand(display.getCurrent().getWidth() + "x" + display.getCurrent().getHeight()); }
        // |
        // X11 Loader
        else if (mainCommand.equals("term")) { display.setCurrent(form); }
        else if (mainCommand.equals("init")) { start("x11-wm", null, null, true); } 
        else if (mainCommand.equals("stop")) { form.setTitle(""); form.setTicker(null); form.deleteAll(); xserver("cmd hide", root); form.removeCommand(EXECUTE); trace.remove("2"); }
        else if (mainCommand.equals("xfinit")) { if (argument.equals("")) { return xserver("init", root); } if (argument.equals("stdin")) { form.append(stdin); } else if (argument.equals("stdout")) { form.append(stdout); } }
        else if (mainCommand.equals("cmd")) { Command[] CMDS = { HELP, NANO, CLEAR, HISTORY }; for (int i = 0; i < CMDS.length; i++) { if (argument.equals("hide")) { form.removeCommand(CMDS[i]); } else { form.addCommand(CMDS[i]); } } }
        // | 
        // Screen MODs
        else if (mainCommand.equals("title")) { display.getCurrent().setTitle(argument); }
        else if (mainCommand.equals("font")) { stdout.setFont(newFont(argument.equals("") ? "default" : argument)); }
        else if (mainCommand.equals("tick")) { Displayable current = display.getCurrent(); current.setTicker(argument.equals("") ? null : new Ticker(argument)); }
        else if (mainCommand.equals("gauge")) { Alert alert = new Alert(form.getTitle(), argument, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING)); display.setCurrent(alert); }
        // |
        // Screen Manager
        else if (mainCommand.equals("mod")) { new MIDletControl(argument, root); }
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else if (trace.containsKey("2")) { ((Hashtable) getobject("2", "saves")).put(argument, display.getCurrent()); } else { return 69; } }
        else if (mainCommand.equals("load") || mainCommand.equals("unset")) {
            if (argument.equals("")) { }
            else if (trace.containsKey("2")) {
                Hashtable desktops = (Hashtable) getobject("2", "saves");

                if (desktops.containsKey(argument)) {
                    if (mainCommand.equals("load")) { display.setCurrent((Displayable) desktops.get(argument)); }
                    else { desktops.remove(argument); }
                } else { echoCommand("x11: " + mainCommand + ": " + argument + ": not found"); return 127; }
            } 
            else { return 69; }
        }
        else if (mainCommand.equals("import") || mainCommand.equals("export")) { 
            if (argument.equals("")) { } 
            else {
                if (trace.containsKey(argument)) {
                    if (getowner(argument).equals("root") && !root) { echoCommand("Permission denied!"); return 13; }

                    if (mainCommand.equals("import")) {
                        Displayable screen = (Displayable) getobject(argument, "screen");

                        if (screen == null) { echoCommand("x11: import: " + argument + ": no screens"); return 69; }
                        else { display.setCurrent(screen); }
                    } else {
                        ((Hashtable) getprocess(argument)).put("screen", display.getCurrent());
                    }
                } else {
                    echoCommand("x11: " + mainCommand + ": " + argument + ": not found"); return 127;
                }
            } 
        }
        // |
        // Interfaces
        else if (mainCommand.equals("canvas")) { if (javaClass("MIDletCanvas") == 0) { display.setCurrent(new MIDletCanvas(this, argument.equals("") ? "Canvas" : argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), root)); } else { echoCommand("This MIDlet Build don't have MIDletCanvas"); return 3; } }
        else if (mainCommand.equals("make") || mainCommand.equals("list") || mainCommand.equals("quest") || mainCommand.equals("edit")) { new MIDletControl(mainCommand.equals("make") ? MIDletControl.SCREEN : mainCommand.equals("list") ? MIDletControl.LIST : mainCommand.equals("quest") ? MIDletControl.QUEST : MIDletControl.WEDIT, argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), root); }
        else if (mainCommand.equals("item")) { if (argument.equals("") || argument.equals("clear")) { form.deleteAll(); form.append(stdout); form.append(stdin); } else { new MIDletControl(form, "item", argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), root); } }

        else { echoCommand("x11: " + mainCommand + ": not found"); return 127; }

        return 0;
    }
    // |
    private int warnCommand(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    private int viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(env(text)); viewer.addCommand(BACK); viewer.setCommandListener(this); display.setCurrent(viewer); return 0; }
    // |
    // Font Generator
    public Font newFont(String argument) {
        if (argument == null || argument.length() == 0 || argument.equals("default")) { return Font.getDefaultFont(); }

        int face = Font.FACE_SYSTEM;
        int style = Font.STYLE_PLAIN;
        int size = Font.SIZE_MEDIUM;

        String[] tokens = split(argument, ' ');
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i].toLowerCase();

            if (token.equals("system")) face = Font.FACE_SYSTEM;
            else if (token.equals("monospace")) face = Font.FACE_MONOSPACE;
            else if (token.equals("proportional")) face = Font.FACE_PROPORTIONAL;

            else if (token.equals("bold")) style |= Font.STYLE_BOLD;
            else if (token.equals("italic")) style |= Font.STYLE_ITALIC;
            else if (token.equals("ul") || token.equals("underline") || token.equals("underlined"))
                style |= Font.STYLE_UNDERLINED;

            else if (token.equals("small")) size = Font.SIZE_SMALL;
            else if (token.equals("medium")) size = Font.SIZE_MEDIUM;
            else if (token.equals("large")) size = Font.SIZE_LARGE;
        }

        Font f = Font.getFont(face, style, size);

        return f == null ? Font.getDefaultFont() : f;
    }

    // API 005 - (Operators)
    // |
    // Operators
    private int ifCommand(String argument, boolean ignore, boolean root) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("if (expr) [command]"); return 2; } String EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); String[] PARTS = split(EXPR, ' '); if (PARTS.length == 3) { boolean CONDITION = false; boolean NEGATED = PARTS[1].startsWith("!") && !PARTS[1].equals("!="); if (NEGATED) { PARTS[1] = PARTS[1].substring(1); } Double N1 = getNumber(PARTS[0]), N2 = getNumber(PARTS[2]); if (N1 != null && N2 != null) { if (PARTS[1].equals("==")) { CONDITION = N1.doubleValue() == N2.doubleValue(); } else if (PARTS[1].equals("!=")) { CONDITION = N1.doubleValue() != N2.doubleValue(); } else if (PARTS[1].equals(">")) { CONDITION = N1.doubleValue() > N2.doubleValue(); } else if (PARTS[1].equals("<")) { CONDITION = N1.doubleValue() < N2.doubleValue(); } else if (PARTS[1].equals(">=")) { CONDITION = N1.doubleValue() >= N2.doubleValue(); } else if (PARTS[1].equals("<=")) { CONDITION = N1.doubleValue() <= N2.doubleValue(); } } else { if (PARTS[1].equals("startswith")) { CONDITION = PARTS[0].startsWith(PARTS[2]); } else if (PARTS[1].equals("endswith")) { CONDITION = PARTS[0].endsWith(PARTS[2]); } else if (PARTS[1].equals("contains")) { CONDITION = PARTS[0].indexOf(PARTS[2]) != -1; } else if (PARTS[1].equals("==")) { CONDITION = PARTS[0].equals(PARTS[2]); } else if (PARTS[1].equals("!=")) { CONDITION = !PARTS[0].equals(PARTS[2]); } } if (CONDITION != NEGATED) { return processCommand(CMD, ignore, root); } } else if (PARTS.length == 2) { if (PARTS[0].equals(PARTS[1])) { return processCommand(CMD, ignore, root); } } else if (PARTS.length == 1) { if (!PARTS[0].equals("")) { return processCommand(CMD, ignore, root); } } return 0; }
    private int forCommand(String argument, boolean ignore, boolean root) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String KEY = getCommand(argument), FILE = getcontent(argument.substring(firstParenthesis + 1, lastParenthesis).trim()), CMD = argument.substring(lastParenthesis + 1).trim(); if (KEY.startsWith("(")) { return 2; } if (KEY.startsWith("$")) { KEY = replace(KEY, "$", ""); } String[] LINES = split(FILE, '\n'); for (int i = 0; i < LINES.length; i++) { if (LINES[i] != null || LINES[i].length() != 0) { processCommand("set " + KEY + "=" + LINES[i], false, root); int STATUS = processCommand(CMD, ignore, root); processCommand("unset " + KEY, false, root); if (STATUS != 0) { return STATUS; } } } return 0; }
    private int caseCommand(String argument, boolean ignore, boolean root) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String METHOD = getCommand(argument), EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); boolean CONDITION = false, NEGATED = METHOD.startsWith("!"); if (NEGATED) { METHOD = METHOD.substring(1); } if (METHOD.equals("file")) { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(EXPR)) { CONDITION = true; break; } } } } else if (METHOD.equals("root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(EXPR)) { CONDITION = true; break; } } } else if (METHOD.equals("thread")) { CONDITION = getThreadName(Thread.currentThread()).equals(EXPR); } else if (METHOD.equals("screen")) { CONDITION = ((Hashtable) getobject("2", "saves")).containsKey(EXPR); } else if (METHOD.equals("key")) { CONDITION = attributes.containsKey(EXPR); } else if (METHOD.equals("alias")) { CONDITION = aliases.containsKey(EXPR); } else if (METHOD.equals("trace")) { CONDITION = getpid(EXPR) != null ? true : false; } else if (METHOD.equals("passwd")) { CONDITION = String.valueOf(EXPR.hashCode()).equals(MIDletControl.passwd()); } else if (METHOD.equals("user")) { CONDITION = username.equals(EXPR); if (EXPR.equals("root") && root == true) { CONDITION = true; } root = true; } if (CONDITION != NEGATED) { return processCommand(CMD, ignore, root); } return 0; }
    
    // API 006 - (Process)
    // |
    // Process
    public int kill(String pid, boolean print, boolean root) {
        if (pid == null || pid.length() == 0) { return 2; }

        Hashtable proc = (Hashtable) trace.get(pid);
        if (proc == null) { if (print) { echoCommand("PID '" + pid + "' not found"); } return 127; }

        String owner = (String) proc.get("owner");
        Object collector = (String) proc.get("collector");

        if (owner.equals("root") && !root) { if (print) { echoCommand("Permission denied!"); } return 13; }
        if (collector != null && collector instanceof String) { processCommand((String) collector, true, root); }

        trace.remove(pid);
        if (print) { echoCommand("Process with PID " + pid + " terminated"); }

        return 0;
    }
    public int start(String app, String pid, String collector, boolean root) {
        if (app == null || app.length() == 0) { return 2; }

        Hashtable proc = genprocess(app, root, collector);

        if (app.equals("sh") || app.equals("x11-wm")) {
            pid = app.equals("sh") ? "1" : "2"; 
            proc.put("collector", app.equals("sh") ? "exit" : "x11 stop");
            proc.put("screen", form); 

            if (trace.containsKey(pid)) { return 68; }
            else if (app.equals("sh")) { 
                Hashtable sessions = new Hashtable();
                sessions.put(pid, "127.0.0.1");

                proc.put("stack", new Vector());
                proc.put("history", new Vector()); 
                proc.put("sessions", sessions);
                proc.put("servers", new Hashtable());
            }
            else if (app.equals("x11-wm")) { 
                proc.put("saves", new Hashtable()); 

                form.append(stdout); 
                form.append(stdin); 
                form.addCommand(EXECUTE); 
                processCommand("execute title; x11 cmd;"); 
                form.setCommandListener(this); 
            }
        } 
        else if (app.equals("audio")) { echoCommand("usage: audio play [file]"); return 1; }
        else { while (trace.containsKey(pid) || pid == null || pid.length() == 0) { pid = genpid(); } } 

        trace.put(pid, proc);
        return 0;
    }
    public int stop(String app, boolean root) {
        if (app == null || app.length() == 0) return 2;

        int STATUS = 0;

        for (Enumeration keys = trace.keys(); keys.hasMoreElements();) {
            String PID = (String) keys.nextElement(), NAME = (String) ((Hashtable) trace.get(PID)).get("name");

            if (app.equals(NAME)) { if ((STATUS = kill(PID, false, root)) != 0) { break; } }
        }

        return STATUS;
    }
    // | 
    // Kernel
    private int kernel(String command, boolean root) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);
        String[] args = splitArgs(argument);

        if (mainCommand.equals("") || mainCommand.equals("monitor") || mainCommand.equals("process")) { new MIDletControl(mainCommand, root); } 
        else if (mainCommand.equals("pid") || mainCommand.equals("owner") || mainCommand.equals("check")) { if (argument.equals("")) { } else { echoCommand(mainCommand.equals("pid") ? getpid(argument) : mainCommand.equals("owner") ? getowner(getArgument(argument)) : (getpid(getArgument(argument)) != null ? "true" : "false")); } } 
        else if (mainCommand.equals("used")) { echoCommand("" + (runtime.totalMemory() - runtime.freeMemory()) / 1024); } 
        else if (mainCommand.equals("free")) { echoCommand("" + runtime.freeMemory() / 1024); } 
        else if (mainCommand.equals("total")) { echoCommand("" + runtime.totalMemory() / 1024); }
        else if (mainCommand.equals("view") || mainCommand.equals("read")) {
            Hashtable ITEM = argument.equals("") ? trace : getprocess(argument);
                
            if (ITEM == null) {
                if (mainCommand.equals("view")) { warnCommand(form.getTitle(), "PID '" + argument + "' not found"); }
                else { echoCommand("PID '" + argument + "' not found"); }
                    
                return 127;
            } else {
                if (ITEM.get("owner").equals("root") && !root) {
                    if (mainCommand.equals("view")) { warnCommand(form.getTitle(), "Permission denied!"); }
                    else { echoCommand("Permission denied!"); }
                        
                    return 13;
                }
                    
                if (mainCommand.equals("view")) { viewer("Process Viewer", renderJSON(ITEM, 0)); }
                else { echoCommand(renderJSON(ITEM, 0)); }
            }       
        }  
        else if (mainCommand.equals("clean")) {
            if (argument.equals("")) { }
            else {
                if (root) {
                    String PID = getCommand(argument), collector = getArgument(argument);

                    if (PID.equals("")) { }
                    else if (PID.equals("1") || PID.equals("2")) { return 13; }
                    else if (trace.containsKey(PID)) { if (collector.equals("")) { ((Hashtable) getprocess(PID)).remove("collector"); } else { ((Hashtable) getprocess(PID)).put("collector", collector); } }
                    else { echoCommand("top: clean: " + PID + ": not found"); return 127; }
                } else { echoCommand("Permission denied!"); return 13; }
            }
        }
        else if (mainCommand.equals("get")) {
            if (argument.equals("") || args.length < 2) { }
            else {
                if (trace.containsKey(args[0])) {
                    if (args[1].equals("name") || args[1].equals("owner") || args[1].equals("collector")) { echoCommand("Permission denied!"); return 13; }

                    Object hand = getobject(args[0], args[1]);

                    if (hand == null) { echoCommand("top: get: " + args[0] + "." + args[1] + ": not found"); return 127; }
                    else { getprocess("1").put("hand", hand); getprocess("1").put("hand.from", args[0]); }
                } else { echoCommand("top: get: " + args[0] + ": not found"); return 127; }
            }
        }
        else if (mainCommand.equals("drop")) { getprocess("1").remove("hand"); getprocess("1").remove("hand.from"); }
        else if (mainCommand.equals("hand")) {
            if (argument.equals("")) { echoCommand(getobject("1", "hand") == null ? "hand empty" : getobject("1", "hand").toString()); }
            else if (getobject("1", "hand") == null) { echoCommand("top: hand: no itens on hand"); return 1; }
            else {
                Object hand = getobject("1", "hand");

                if (argument.equals("close")) {
                    try {
                        if (hand instanceof StreamConnection) { ((StreamConnection) hand).close(); }
                        else if (hand instanceof ServerSocketConnection) { ((ServerSocketConnection) hand).close(); }
                        else if (hand instanceof InputStream) { ((InputStream) hand).close(); } 
                        else if (hand instanceof OutputStream) { ((OutputStream) hand).close(); }
                        else { echoCommand("top: hand: item cannot be closed"); return 69; }
                    } catch (Exception e) { echoCommand(getCatch(e)); return 1; }
                } else if (argument.indexOf('=') != -1 || args[0].equals("remove")) {
                    if (hand instanceof Hashtable) {
                        int INDEX = argument.indexOf('='); 
                        if (INDEX == -1) { if (args.length > 1) { ((Hashtable) hand).remove(args[1]); } else { return 2; } }
                        else { ((Hashtable) hand).put(argument.substring(0, INDEX).trim(), getpattern(argument.substring(INDEX + 1).trim())); } 
                    } 
                } else { echoCommand("top: hand: item need to be a table"); return 69; }
            }
        }
        else { echoCommand("top: " + mainCommand + ": not found"); return 127; } 
        
        return 0;
    }
    // | 
    // Virtual Objects
    // | (Generators)
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    public Hashtable genprocess(String name, boolean root, String collector) { 
        Hashtable proc = new Hashtable(); 
        
        proc.put("name", name); 
        proc.put("owner", root ? "root" : username); 
        if (collector != null) { proc.put("collector", collector); } 
        
        return proc;
    }
    // | (Trackers)
    public Hashtable getprocess(String pid) { return trace.containsKey(pid) ? (Hashtable) trace.get(pid) : null; }
    public Object getobject(String pid, String item) { return (Object) getprocess(pid).get(item); }
    public String getpid(String name) { for (Enumeration KEYS = trace.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); if (name.equals((String) ((Hashtable) trace.get(PID)).get("name"))) { return PID; } } return null; } 
    public String getowner(String pid) { return trace.containsKey(pid) ? (String) ((Hashtable) trace.get(pid)).get("owner") : null; }
    // | (Viewer)
    private String renderJSON(Object obj, int indent) {
        StringBuffer json = new StringBuffer();
        String pad = "";
        for (int i = 0; i < indent; i++) pad += "  ";

        if (obj instanceof Hashtable) {
            Hashtable map = (Hashtable) obj;
            json.append("{\n");
            Enumeration keys = map.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                Object val = map.get(key);
                json.append(pad + "  \"" + key + "\": " + renderJSON(val, indent + 1));
                if (keys.hasMoreElements()) { json.append(","); }
                json.append("\n");
            }

            json.append(pad + "}");
        }

        else if (obj instanceof Vector) {
            Vector list = (Vector) obj;
            json.append("[\n");
            for (int i = 0; i < list.size(); i++) {
                json.append(pad + "  " + renderJSON(list.elementAt(i), indent + 1));

                if (i < list.size() - 1) json.append(",");
                
                json.append("\n");
            }
            json.append(pad + "]");
        }
        else if (obj instanceof String) {
            String s = (String) obj;
            s = replace(s, "\n", "\\n");
            s = replace(s, "\r", "\\r");
            s = replace(s, "\t", "\\t");
            json.append("\"" + s + "\"");
        }

        else { json.append(String.valueOf(obj)); }

        return json.toString();
    }

    // API 008 - (Logic I/O) Text
    // |
    // Text related commands
    private void echoCommand(String message) { echoCommand(message, stdout); attributes.put("OUTPUT", message); }
    private void echoCommand(String message, StringItem console) { if (message == null) { return; } String current = console.getText(), output = current == null || current.length() == 0 ? message : current + "\n" + message; console.setText(TTY_MAX_LEN >= 0 && output.length() > TTY_MAX_LEN ? output.substring(output.length() - TTY_MAX_LEN) : output); }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String exprCommand(String expr) { char[] tokens = expr.toCharArray(); double[] vals = new double[32]; char[] ops = new char[32]; int valTop = -1, opTop = -1; int i = 0, len = tokens.length; while (i < len) { char c = tokens[i]; if (c == ' ') { i++; continue; } if (c >= '0' && c <= '9') { double num = 0; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { num = num * 10 + (tokens[i++] - '0'); } if (i < len && tokens[i] == '.') { i++; double frac = 0, div = 10; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { frac += (tokens[i++] - '0') / div; div *= 10; } num += frac; } vals[++valTop] = num; } else if (c == '(') { ops[++opTop] = c; i++; } else if (c == ')') { while (opTop >= 0 && ops[opTop] != '(') { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } opTop--; i++; } else if (c == '+' || c == '-' || c == '*' || c == '/') { while (opTop >= 0 && prec(ops[opTop]) >= prec(c)) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } ops[++opTop] = c; i++; } else { return "expr: invalid char '" + c + "'"; } } while (opTop >= 0) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } double result = vals[valTop]; return ((int) result == result) ? String.valueOf((int) result) : String.valueOf(result); } private int prec(char op) { if (op == '+' || op == '-') return 1; if (op == '*' || op == '/') return 2; return 0; } private double applyOpSimple(char op, double a, double b) { if (op == '+') return a + b; if (op == '-') return a - b; if (op == '*') return a * b; if (op == '/') return b == 0 ? 0 : a / b; return 0; }
    private String generateUUID() { String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + random.nextInt(4))); } else { uuid.append(chars.charAt(random.nextInt(16))); } } return uuid.toString(); }

    // API 011 - (Network)
    // |
    // HTTP Interfaces
    private String request(String url, Hashtable headers) { if (url == null || url.length() == 0) { return ""; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); if (headers != null) { Enumeration keys = headers.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) headers.get(key); conn.setRequestProperty(key, value); } } InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } is.close(); conn.close(); return new String(baos.toByteArray(), "UTF-8"); } catch (IOException e) { return getCatch(e); } }
    private String request(String url) { return request(url, null); }
    // |
    // Socket Interfaces
    private int query(String command, boolean root) { 
        command = env(command.trim()); 
        String mainCommand = getCommand(command), argument = getArgument(command); 
        if (mainCommand.equals("")) { echoCommand("query: missing [address]"); return 2; } 
        else { 
            try { 
                StreamConnection CONN = (StreamConnection) Connector.open(mainCommand); 
                InputStream IN = CONN.openInputStream(); OutputStream OUT = CONN.openOutputStream(); 

                if (!argument.equals("")) { OUT.write((argument + "\r\n").getBytes()); OUT.flush(); } 

                ByteArrayOutputStream BAOS = new ByteArrayOutputStream();
                byte[] BUFFER = new byte[1024]; int LENGTH;

                while ((LENGTH = IN.read(BUFFER)) != -1) { BAOS.write(BUFFER, 0, LENGTH); }

                String DATA = new String(BAOS.toByteArray(), "UTF-8"), FILE = env("$QUERY"); 
                if (FILE.equals("$QUERY") || env("$QUERY").equals("")) { echoCommand(DATA); MIDletLogs("add warn Query storage setting not found"); } 
                else if (FILE.equals("show")) { echoCommand(DATA); } 
                else if (FILE.equals("nano")) { nanoContent = DATA; echoCommand("query: data retrieved"); } 
                else { writeRMS(FILE, DATA); } 

                IN.close(); OUT.close(); CONN.close(); 
            } 
            catch (Exception e) { echoCommand(getCatch(e)); return (e instanceof SecurityException) ? 13 : 1; } 
        } 

        return 0; 
    }
    private int wireless(String command) { 
        command = env(command.trim()); 
        String mainCommand = getCommand(command), argument = getArgument(command); 

        if (mainCommand.equals("") || mainCommand.equals("id")) { String ID = System.getProperty("wireless.messaging.sms.smsc"); if (ID == null) { echoCommand("Unsupported API"); return 3; } else { echoCommand(ID); } } 
        else if (mainCommand.equals("send")) {
            if (split(argument, ' ').length < 2) { echoCommand("wrl: missing..."); return 2; }

            String address = getCommand(argument);
            String msg = getArgument(argument);
            try {
                MessageConnection conn = (MessageConnection) Connector.open(address);
                TextMessage message = (TextMessage) conn.newMessage(MessageConnection.TEXT_MESSAGE);
                message.setPayloadText(msg);
                conn.send(message);
                conn.close();
                echoCommand("wrl: message sent to '" + address + "'");
            } catch (Exception e) {
                echoCommand(getCatch(e));
                return 1;
            }
        } 
        else if (mainCommand.equals("listen")) {
            String PID = argument.equals("") ? (attributes.containsKey("PORT") ? (String) attributes.get("PORT") : "31522") : argument;
            MessageConnection conn = null;
            try {
                conn = (MessageConnection) Connector.open("sms://:" + PID);
                echoCommand("[+] listening at port " + PID); MIDletLogs("add info Server listening at port " + PID);
                start("wireless", PID, null, false);
                try {
                    while (trace.containsKey(PID)) {
                        Message msg = conn.receive();
                        String sender = "unknown";
                        if (msg instanceof TextMessage) {
                            TextMessage tmsg = (TextMessage) msg;
                            try { sender = tmsg.getAddress(); } catch (Exception ex) { }
                            String payload = tmsg.getPayloadText();
                            echoCommand("[+] " + sender + " -> " + payload);
                        } else {
                            echoCommand("[+] " + sender + " -> binary payload.");
                        }
                    }
                } catch (Exception e) { echoCommand("[-] " + getCatch(e)); kill(PID, false, false); }
            } catch (Exception e) { echoCommand("[-] " + getCatch(e)); MIDletLogs("add info Server crashed '" + PID + "'"); } 
            finally {
                if (conn != null) { try { conn.close(); } catch (IOException e) { } }
                echoCommand("[-] Server stopped");
                MIDletLogs("add info Server was stopped");
            }
        }
        else { echoCommand("wrl: " + mainCommand + ": not found"); return 127; } 

        return 0; 
    }
    private int GetAddress(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { return processCommand("ifconfig"); } else { try { DatagramConnection CONN = (DatagramConnection) Connector.open("datagram://" + (argument.equals("") ? "1.1.1.1:53" : argument)); ByteArrayOutputStream OUT = new ByteArrayOutputStream(); OUT.write(0x12); OUT.write(0x34); OUT.write(0x01); OUT.write(0x00); OUT.write(0x00); OUT.write(0x01); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); String[] parts = split(mainCommand, '.'); for (int i = 0; i < parts.length; i++) { OUT.write(parts[i].length()); OUT.write(parts[i].getBytes()); } OUT.write(0x00); OUT.write(0x00); OUT.write(0x01); OUT.write(0x00); OUT.write(0x01); byte[] query = OUT.toByteArray(); Datagram REQUEST = CONN.newDatagram(query, query.length); CONN.send(REQUEST); Datagram RESPONSE = CONN.newDatagram(512); CONN.receive(RESPONSE); CONN.close(); byte[] data = RESPONSE.getData(); if ((data[3] & 0x0F) != 0) { echoCommand("not found"); return 127; } int offset = 12; while (data[offset] != 0) { offset++; } offset += 5; if (data[offset + 2] == 0x00 && data[offset + 3] == 0x01) { StringBuffer BUFFER = new StringBuffer(); for (int i = offset + 12; i < offset + 16; i++) { BUFFER.append(data[i] & 0xFF); if (i < offset + 15) BUFFER.append("."); } echoCommand(BUFFER.toString()); } else { echoCommand("not found"); return 127; } } catch (IOException e) { echoCommand(getCatch(e)); return 1; } } return 0; }

    // API 012 - (File)
    // |
    // Directories Manager
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.length() == 0 || line.startsWith("#")) { continue; } if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath + "/"); start = j; } } String finalPart = line.substring(start + 1); fullPath += "/" + finalPart; if (line.endsWith("/")) { addDirectory(fullPath + "/"); } else { addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { boolean isDirectory = fullPath.endsWith("/"); if (!paths.containsKey(fullPath)) { if (isDirectory) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/', fullPath.length() - 2) + 1); String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } String dirName = fullPath.substring(parentPath.length(), fullPath.length() - 1); updatedContents.addElement(dirName + "/"); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } else { String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/') + 1); String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1); String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fileName); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } } }
    private String readStack() { Vector stack = (Vector) getobject("1", "stack"); StringBuffer sb = new StringBuffer(); sb.append(path); for (int i = 0; i < stack.size(); i++) { sb.append(" ").append((String) stack.elementAt(i)); } return sb.toString(); }
    // |
    // RMS Files
    public int deleteFile(String filename) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/home/")) { 
            try { 
                filename = filename.substring(6); 
                if (filename.equals("")) { return 2; }
                if (filename.equals("OpenRMS")) { echoCommand("Permission denied!"); return 13; } 
                
                RecordStore.deleteRecordStore(filename); 
            } 
            catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); return 127; } 
            catch (Exception e) { echoCommand(getCatch(e)); return 1; } 
        } 
        else if (filename.startsWith("/mnt/")) { 
            try { 
                FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); 
                if (CONN.exists()) { CONN.delete(); } 
                else { echoCommand("rm: " + basename(filename) + ": not found"); return 127; } 
                
                CONN.close(); 
            } 
            catch (Exception e) { echoCommand(getCatch(e)); return e instanceof SecurityException ? 13 : 1; } 
        } 
        else if (filename.startsWith("/tmp/")) {
            filename = filename.substring(5);
            if (filename.equals("")) { }
            else if (tmp.containsKey(filename)) { tmp.remove(filename); }
            else {
                echoCommand("rm: " + filename + ": not found");
                return 127;
            }
        }
        else if (filename.startsWith("/")) { echoCommand("read-only storage"); return 5; } 
        else { return deleteFile(path + filename); } 
        
        return 0; 
    }
    public int writeRMS(String filename, byte[] data) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.create(); } OutputStream OUT = CONN.openOutputStream(); OUT.write(data); OUT.flush(); OUT.close(); CONN.close(); } catch (Exception e) { echoCommand(getCatch(e)); return (e instanceof SecurityException) ? 13 : 1; } } 
        else if (filename.startsWith("/home/")) { return writeRMS(filename.substring(6), data, 1); } 
        else if (filename.startsWith("/dev/")) { filename = filename.substring(5); if (filename.equals("")) { return 2; } else if (filename.equals("null")) { } else if (filename.equals("stdin")) { stdin.setString(new String(data)); } else if (filename.equals("stdout")) { stdout.setText(new String(data)); } else { echoCommand("read-only storage"); return 5; } }
        else if (filename.startsWith("/tmp/")) { filename = filename.substring(5); if (filename.equals("")) { return 2; } else { tmp.put(filename, new String(data)); } }
        else if (filename.startsWith("/")) { echoCommand("read-only storage"); return 5; } 
        else { return writeRMS(path + filename, data); } return 0; }
    public int writeRMS(String filename, byte[] data, int index) { try { RecordStore CONN = RecordStore.openRecordStore(filename, true); while (CONN.getNumRecords() < index) { CONN.addRecord("".getBytes(), 0, 0); } CONN.setRecord(index, data, 0, data.length); if (CONN != null) { CONN.closeRecordStore(); } } catch (Exception e) { echoCommand(getCatch(e)); return 1; } return 0; }
    public int writeRMS(String filename, String data) { return writeRMS(filename, data.getBytes()); }
    public String loadRMS(String filename) { return read("/home/" + filename); }
    // |
    // Text Manager
    private int StringEditor(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("-2u")) { nanoContent = nanoContent.toUpperCase(); } else if (mainCommand.equals("-2l")) { nanoContent = nanoContent.toLowerCase(); } else if (mainCommand.equals("-d")) { nanoContent = replace(nanoContent, split(argument, ' ')[0], ""); } else if (mainCommand.equals("-a")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } else if (mainCommand.equals("-r")) { nanoContent = replace(nanoContent, split(argument, ' ')[0], split(argument, ' ')[1]); } else if (mainCommand.equals("-l")) { int i = 0; try { i = Integer.parseInt(argument); } catch (NumberFormatException e) { echoCommand(getCatch(e)); return 2; } echoCommand(split(nanoContent, '\n')[i]); } else if (mainCommand.equals("-s")) { int i = 0; try { i = Integer.parseInt(getCommand(argument)); } catch (NumberFormatException e) { echoCommand(getCatch(e)); return 2; } Vector lines = new Vector(); String div = getArgument(argument); int start = 0, index; while ((index = nanoContent.indexOf(div, start)) != -1) { lines.addElement(nanoContent.substring(start, index)); start = index + div.length(); } if (start < nanoContent.length()) { lines.addElement(nanoContent.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); if (i >= 0 && i < result.length) { echoCommand(result[i]); } else { echoCommand("null"); return 1; } } else if (mainCommand.equals("-p")) { String[] contentLines = split(nanoContent, '\n'); StringBuffer updatedContent = new StringBuffer(); for (int i = 0; i < contentLines.length; i++) { updatedContent.append(argument).append(contentLines[i]).append("\n"); } nanoContent = updatedContent.toString().trim(); } else if (mainCommand.equals("-v")) { String[] lines = split(nanoContent, '\n'); StringBuffer reversed = new StringBuffer(); for (int i = lines.length - 1; i >= 0; i--) { reversed.append(lines[i]).append("\n"); } nanoContent = reversed.toString().trim(); } else { return 127; } return 0; }
    // |
    // Text Parsers
    private String parseJson(String text) { Hashtable properties = parseProperties(text); if (properties.isEmpty()) { return "{}"; } Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\n  \"").append(key).append("\": "); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("\n}"); return jsonBuffer.toString(); }
    private String parseConf(String text) { StringBuffer iniBuffer = new StringBuffer(); text = text.trim(); if (text.startsWith("{") && text.endsWith("}")) { text = text.substring(1, text.length() - 1); } String[] pairs = split(text, ','); for (int i = 0; i < pairs.length; i++) { String pair = pairs[i].trim(); String[] keyValue = split(pair, ':'); if (keyValue.length == 2) { String key = getpattern(keyValue[0].trim()); String value = getpattern(keyValue[1].trim()); iniBuffer.append(key).append("=").append(value).append("\n"); } } return iniBuffer.toString(); }
    // |
    // Interfaces
    private String extractTitle(String htmlContent, String fallback) { return extractTag(htmlContent, "title", fallback); }
    private String extractTag(String htmlContent, String tag, String fallback) { String startTag = "<" + tag + ">", endTag = "</" + tag + ">"; int start = htmlContent.indexOf(startTag), end = htmlContent.indexOf(endTag); if (start != -1 && end != -1 && end > start) { return htmlContent.substring(start + startTag.length(), end).trim(); } else { return fallback; } }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }
    // |
    // Audio Manager
    private int audio(String command, boolean root) { 
        command = env(command.trim()); 
        String mainCommand = getCommand(command), argument = getArgument(command); 

        if (mainCommand.equals("")) { } 
        else if (mainCommand.equals("play")) { 
            if (argument.equals("")) { } 
            else { 
                try {
                    InputStream IN = null;
                    
                    if (argument.startsWith("/mnt/")) { 
                        FileConnection CONN = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ); 
                        IN = CONN.exists() ? CONN.openInputStream() : null; 
                        CONN.close(); 
                    } 
                    else if (argument.startsWith("/home/")) { echoCommand("audio: invalid source."); return 1; } 
                    else if (argument.startsWith("/")) { IN = getClass().getResourceAsStream(argument); }
                    else { return audio("play " + path + argument, root); }
                    
                    if (IN == null) { echoCommand("audio: " + basename(argument) + ": not found"); return 127; }
                    if (trace.containsKey("3")) { audio("stop", root); }
                    
                    Player player = Manager.createPlayer(IN, getMimeType(argument)); 
                    player.prefetch(); player.start(); 

                    Hashtable proc = genprocess("audio", root, "audio stop"); 
                    proc.put("player", player);
                    trace.put("3", proc);
                } catch (Exception e) { 
                    echoCommand(getCatch(e)); 
                    return (e instanceof SecurityException) ? 13 : 1; 
                } 
            }
        }
        else if (mainCommand.equals("volume")) { 
            if (trace.containsKey("3")) { 
                VolumeControl vc = (VolumeControl) ((Player) getprocess("3").get("player")).getControl("VolumeControl"); 
                
                if (argument.equals("")) { echoCommand("" + vc.getLevel()); } 
                else { 
                    try { vc.setLevel(Integer.parseInt(argument)); } 
                    catch (Exception e) { echoCommand(getCatch(e)); return 2; } 
                } 
            } else { echoCommand("audio: not running."); return 69; } 
        } 
        else if (mainCommand.equals("stop") || mainCommand.equals("pause") || mainCommand.equals("resume")) { 
            try { 
                if (trace.containsKey("3")) { 
                    Player player = ((Player) getprocess("3").get("player"));

                    if (mainCommand.equals("pause")) { player.stop(); }
                    else if (mainCommand.equals("resume")) { player.start(); }
                    else { if (player != null) { player.stop(); player.close(); } trace.remove("3"); }
                } 
                else { echoCommand("audio: not running."); return 69; } 
            }
            catch (Exception e) { echoCommand(getCatch(e)); return 1; } 
        }
        else if (mainCommand.equals("status")) { echoCommand(trace.containsKey("3") ? "true" : "false"); } 
        else { echoCommand("audio: " + mainCommand + ": not found"); return 127; } 

        return 0; 
    }
    private String getMimeType(String filename) { return filename.equals("") ? "" : getExtensionInfo(getExtension(filename))[1]; }
    private String getFileType(String filename) { return filename.equals("") ? "" : getExtensionInfo(getExtension(filename))[2]; }
    private String getExtension(String filename) {
        if (filename == null) { return ""; }
        int dot = filename.lastIndexOf('.');
        if (dot >= 0 && dot < filename.length() - 1) { return filename.substring(dot).toLowerCase(); }
        return "";
    }
    public String[] getExtensionInfo(String ext) {
        if (filetypes == null) { filetypes  = parseProperties(getcontent("/res/filetypes")); }
        String value = (String) filetypes.get(ext.toLowerCase());
        if (value == null) { return new String[] { "Unknown", "application/octet-stream", "bin" }; }
        return split(value, ',');
    }

    // API 013 - (MIDlet)
    // |
    // Java Machine
    private int java(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("Java ME", env("Java 1.2 (OpenTTY Edition)\n\nMicroEdition-Config: $CONFIG\nMicroEdition-Profile: $PROFILE")); }
        else if (mainCommand.equals("-class")) { if (argument.equals("")) { } else { int STATUS = javaClass(argument); echoCommand(STATUS == 0 ? "true" : "false"); return STATUS; } } 
        else if (mainCommand.equals("--version")) { String s; StringBuffer BUFFER = new StringBuffer(); if ((s = System.getProperty("java.vm.name")) != null) { BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor")); if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); } if ((s = System.getProperty("java.vm.specification.name")) != null) { BUFFER.append('\n').append(s); } } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) { BUFFER.append("J9 VM, IBM (").append(s).append(')'); if ((s = System.getProperty("java.fullversion")) != null) { BUFFER.append("\n\n").append(s); } } else if ((s = System.getProperty("com.oracle.jwc.version")) != null) { BUFFER.append("OJWC v").append(s).append(", Oracle"); } else if (javaClass("com.sun.cldchi.jvm.JVM") == 0) { BUFFER.append("CLDC Hotspot Implementation, Sun"); } else if (javaClass("com.sun.midp.Main") == 0) { BUFFER.append("KVM, Sun (MIDP)"); } else if (javaClass("com.sun.cldc.io.ConsoleOutputStream") == 0) { BUFFER.append("KVM, Sun (CLDC)"); } else if (javaClass("com.jblend.util.SortedVector") == 0) { BUFFER.append("JBlend, Aplix"); } else if (javaClass("com.jbed.io.CharConvUTF8") == 0) { BUFFER.append("Jbed, Esmertec/Myriad Group"); } else if (javaClass("MahoTrans.IJavaObject") == 0) { BUFFER.append("MahoTrans"); } else { BUFFER.append("Unknown"); } echoCommand(BUFFER.append('\n').toString()); }
        else { echoCommand("java: " + mainCommand + ": not found"); return 127; }

        return 0;
    }
    private int javaClass(String argument) { try { Class.forName(argument); return 0; } catch (ClassNotFoundException e) { return 3; } } 
    // |
    // History
    private void add2History(String command) { if (command.equals("") || command.equals(getLastHistory()) || command.startsWith("!!") || command.startsWith("#")) { } else { ((Vector) getobject("1", "history")).addElement(command.trim()); } }
    private String getLastHistory() { Vector history = (Vector) getobject("1", "history"); return history.size() > 0 ? (String) history.elementAt(history.size() - 1) : ""; }

    // API 015 - (Scripts)
    // |
    // OpenTTY Packages
    private void about(String script) { if (script == null || script.length() == 0) { warnCommand("About", env("OpenTTY $VERSION\n(C) 2025 - Mr. Lima")); return; } Hashtable PKG = parseProperties(getcontent(script)); if (PKG.containsKey("name")) { echoCommand((String) PKG.get("name") + " " + (String) PKG.get("version")); } if (PKG.containsKey("description")) { echoCommand((String) PKG.get("description")); } }
    private int importScript(String script) { return importScript(script, username.equals("root") ? true : false); }
    private int importScript(String script, boolean root) {
        if (script == null || script.length() == 0) { return 2; }

        Hashtable PKG = parseProperties(script);
        final String PID = genpid();
        // |
        // Verify current API version
        if (PKG.containsKey("api.version")) {
            String version = env("$VERSION"), apiVersion = (String) PKG.get("api.version"), mode = (String) PKG.get("api.match");
            if (mode == null || mode.length() == 0) mode = "exact-prefix";

            boolean fail = false;

            if (mode.equals("exact-prefix")) { fail = !version.startsWith(apiVersion); } 
            else if (mode.equals("minimum") || mode.equals("maximum")) {
                String[] currentParts = split(version, '.'), requiredParts = split(apiVersion, '.');
                if (mode.equals("minimum")) { if (currentParts.length < 2 || requiredParts.length < 2) { fail = true; } else { fail = getNumber(requiredParts[1]) > getNumber(currentParts[1]); } }
                else if (mode.equals("maximum")) { if (currentParts.length < 1 || requiredParts.length < 1) { fail = true; } else { fail = getNumber(requiredParts[0]) > getNumber(currentParts[0]); } }
            } 
            else if (mode.equals("exact-full")) { fail = !version.equals(apiVersion); } 
            else { return 1; }

            if (fail) { String error = (String) PKG.get("api.error"); processCommand(error != null ? error : "true", true, root); return 3; }
        }
        if (PKG.containsKey("api.require")) {
            String[] nodes = split((String) PKG.get("api.require"), ',');
            for (int i = 0; i < nodes.length; i++) {
                boolean fail = false;

                if (nodes[i].equals("lua") && javaClass("Lua") != 0) { fail = true; }
                if (nodes[i].equals("canvas") && javaClass("MIDletCanvas") != 0) { fail = true; }

                if (fail) { String error = (String) PKG.get("api.error"); processCommand(error != null ? error : "true", true, root); return 3; }             
            }
        }
        // |
        // Build dependencies
        if (PKG.containsKey("include")) { String[] include = split((String) PKG.get("include"), ','); for (int i = 0; i < include.length; i++) { int STATUS = importScript(getcontent(include[i]), root); if (STATUS != 0) { return STATUS; } } }
        // |
        // Start and handle APP process
        if (PKG.containsKey("process.name")) { start((String) PKG.get("process.name"), PID, (String) PKG.get("process.exit"), root); }
        if (PKG.containsKey("process.port")) { 
            String PORT = (String) PKG.get("process.port"), MOD = (String) PKG.get("process.db"); 
            if (((Hashtable) getobject("1", "sessions")).containsKey(PORT)) { MIDletLogs("add warn Application port is unavailable."); return 68; }
            
            new MIDletControl("bind", env(PORT + " " + (MOD == null ? "" : MOD)), root);
        }
        // |
        // Start Application
        if (PKG.containsKey("config")) { int STATUS = processCommand((String) PKG.get("config"), true, root); if (STATUS != 0) { return STATUS; } }
        if (PKG.containsKey("mod") && PKG.containsKey("process.name")) { new MIDletControl(PID, "MIDlet-MOD", (String) PKG.get("mod"), true, root); }
        // |
        // Generate items - Command & Files
        if (PKG.containsKey("command")) { String[] commands = split((String) PKG.get("command"), ','); for (int i = 0; i < commands.length; i++) { if (PKG.containsKey(commands[i])) { aliases.put(commands[i], env((String) PKG.get(commands[i]))); } else { MIDletLogs("add error Failed to create command '" + commands[i] + "' content not found"); } } }
        if (PKG.containsKey("file")) { String[] files = split((String) PKG.get("file"), ','); for (int i = 0; i < files.length; i++) { if (PKG.containsKey(files[i])) { int STATUS = writeRMS("/home/" + files[i], env((String) PKG.get(files[i]))); } else { MIDletLogs("add error Failed to create file '" + files[i] + "' content not found"); } } }
        // |
        // Build APP Shell
        if (PKG.containsKey("shell.name") && PKG.containsKey("shell.args")) { String[] args = split((String) PKG.get("shell.args"), ','); Hashtable TABLE = new Hashtable(); for (int i = 0; i < args.length; i++) { String NAME = args[i].trim(), VALUE = (String) PKG.get(NAME); TABLE.put(NAME, (VALUE != null) ? VALUE : ""); } if (PKG.containsKey("shell.unknown")) { TABLE.put("shell.unknown", (String) PKG.get("shell.unknown")); } shell.put(((String) PKG.get("shell.name")).trim(), TABLE); }

        return 0;
    }
    private int runScript(String script, boolean root) { String[] CMDS = split(script, '\n'); for (int i = 0; i < CMDS.length; i++) { int STATUS = processCommand(CMDS[i].trim(), true, root); if (STATUS != 0) { return STATUS; } } return 0; }
    private int runScript(String script) { return runScript(script, username.equals("root") ? true : false); }
}
// |
// EOF