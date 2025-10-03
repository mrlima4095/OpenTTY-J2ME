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
// MIDlet Controller
public class MIDletControl implements ItemCommandListener, CommandListener, Runnable {
    private static final int HISTORY = 1, EXPLORER = 2, MONITOR = 3, PROCESS = 4, SIGNUP = 5, REQUEST = 7, LOCK = 8, NC = 9, PRSCAN = 10, GOBUSTER = 11, BIND = 12, SCREEN = 13, LIST = 14, QUEST = 15, WEDIT = 16, BG = 17, ADDON = 18;

    private OpenTTY midlet;
    private int MOD = -1, COUNT = 1, id = 1, start;
    private boolean ignore = true, asked = false, keep = false, asking_user = midlet.username.equals(""), asking_passwd = passwd().equals("");
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

    public MIDletControl(OpenTTY midlet, String command, int id) {
        MOD = command == null || command.length() == 0 || command.equals("monitor") ? MONITOR : command.equals("process") ? PROCESS : command.equals("dir") ? EXPLORER : command.equals("history") ? HISTORY : -1;
        this.midlet = midlet; this.id = id;

        if (MOD == MONITOR) {
            monitor = new Form(midlet.form.getTitle());
            monitor.append(console = new StringItem("Memory Status:", ""));
            monitor.addCommand(BACK);
            monitor.addCommand(REFRESH = new Command("Refresh", Command.SCREEN, 2));
            monitor.setCommandListener(this);
            load();
            midlet.display.setCurrent(monitor);
        } 
        else {
            preview = new List(midlet.form.getTitle(), List.IMPLICIT);
            preview.addCommand(BACK);
            preview.addCommand(MOD == EXPLORER ? (OPEN = new Command("Open", Command.OK, 1)) : MOD == PROCESS ? (KILL = new Command("Kill", Command.OK, 1)) : (RUN = new Command("Run", Command.OK, 1)));

            if (MOD == HISTORY) { preview.addCommand(EDIT = new Command("Edit", Command.OK, 1)); } 
            else if (MOD == PROCESS) { preview.addCommand(LOAD = new Command("Load Screen", Command.OK, 1)); preview.addCommand(VIEW = new Command("View info", Command.OK, 1)); preview.addCommand(FILTER = new Command("Filter", Command.OK, 1)); }
            else if (MOD == EXPLORER) { preview.addCommand(DELETE = new Command("Delete", Command.OK, 1)); preview.addCommand(RUNS = new Command("Run Script", Command.OK, 1)); preview.addCommand(PROPERTY = new Command("Properties", Command.OK, 1)); preview.addCommand(REFRESH = new Command("Refresh", Command.OK, 1)); }

            preview.setCommandListener(this);
            load(); display.setCurrent(preview);
        }
    }
    public MIDletControl(OpenTTY midlet, String command) {
        MOD = command == null || command.length() == 0 || command.equals("login") ? SIGNUP : REQUEST;
        monitor = new Form(midlet.form.getTitle());
        this.midlet = midlet;

        if (MOD == SIGNUP) {
            monitor.append(midlet.env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\n" + (asking_user && asking_passwd ? "Create your credentials!" : asking_user ? "Create an user to access OpenTTY!" : asking_passwd ? "Create a password!" : "")).trim());

            if (asking_user) { monitor.append(USER = new TextField("Username", "", 256, TextField.ANY)); }
            if (asking_passwd) { monitor.append(PASSWD = new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); }

            monitor.addCommand(LOGIN = new Command("Login", Command.OK, 1)); monitor.addCommand(EXIT = new Command("Exit", Command.SCREEN, 2));
        } 
        else {
            if (asking_passwd) { new MIDletControl(null); return; }
            this.command = command;

            monitor.append(PASSWD = new TextField("[sudo] password for " + midlet.loadRMS("OpenRMS"), "", 256, TextField.ANY | TextField.PASSWORD));
            monitor.addCommand(EXECUTE); monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
        }

        monitor.setCommandListener(this);
        display.setCurrent(monitor);
    }
    public MIDletControl(OpenTTY midlet, String mode, String args, int id) {
        MOD = mode == null || mode.length() == 0 || mode.equals("nc") ? NC : mode.equals("prscan") ? PRSCAN : mode.equals("gobuster") ? GOBUSTER : mode.equals("bind") ? BIND : -1;
        this.midlet = midlet; this.id = id;

        if (args == null || args.length() == 0) { return; }
        if (MOD == -1) { return; }
        else if (MOD == BIND) {
            String[] argv = midlet.splitArgs(args);

            port = argv[0]; 
            DB = argv.length > 1 ? argv[1] : "";
            proc_name = argv.length > 2 ? argv[2] : "bind";


            new Thread(this, "Bind").start();
            return;
        } 

        Hashtable proc = midlet.genprocess(MOD == NC ? "remote" : MOD == PRSCAN ? "prscan" : "gobuster", id, null);

        if (MOD == NC) {
            address = args;
            try { CONN = (SocketConnection) Connector.open("socket://" + address); IN = CONN.openInputStream(); OUT = CONN.openOutputStream(); } 
            catch (Exception e) { midlet.echoCommand(midlet.getCatch(e)); return; }

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
    public MIDletControl(Form screen, String node, String code, int id) {
        if (code == null || code.length() == 0) { return; } 

        this.PKG = parseProperties(code); this.node = node; this.id = id;

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
    public MIDletControl(int MOD, String code, int id) {
        if (code == null || code.length() == 0) { return; } 

        this.PKG = parseProperties(code); 
        this.MOD = MOD; this.id = id;

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
                    else if (type.equals("item")) { new MIDletControl(monitor, "screen." + field, code, id); } 
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
    public MIDletControl(String name, String command, boolean ignore, int id) { this.MOD = BG; this.command = command; this.ignore = ignore; this.id = id; new Thread(this, name).start(); }
    public MIDletControl(String pid, String name, String command, boolean ignore, int id) { this.MOD = ADDON; this.PID = pid; this.command = command; this.ignore = ignore; this.id = id; new Thread(this, name).start(); }

    public void commandAction(Command c, Displayable d) {
        if (c == BACK) { 
            if (d == box || (d == monitor && MOD == EXPLORER)) { display.setCurrent(preview); }
            else if (MOD == NC || MOD == PRSCAN || MOD == GOBUSTER) { back(); } 
            else if (MOD == SCREEN || MOD == LIST || MOD == QUEST || MOD == WEDIT) { processCommand("xterm"); processCommand(getvalue((MOD == SCREEN ? "screen" : MOD == LIST ? "list" : MOD == QUEST ? "quest" : "edit") + ".back", "true"), true, id); }
            else { processCommand("xterm"); } 
            
            return; 
        }
        if (d == confirm) { if (c == YES) { keep = true; } else { trace.remove(PID); } processCommand("xterm"); }
        if (d == box) { pfilter = box.getString().trim(); load(); display.setCurrent(preview); return; }

        if (MOD == HISTORY) { String selected = preview.getString(preview.getSelectedIndex()); if (selected != null) { processCommand("xterm"); processCommand(c == RUN || c == List.SELECT_COMMAND ? selected : "buff " + selected, true, id); } } 
        else if (MOD == EXPLORER) {
            String selected = preview.getString(preview.getSelectedIndex());

            if (c == OPEN || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { if (selected != null) { processCommand(selected.endsWith("..") ? "cd .." : selected.endsWith("/") ? "cd " + path + selected : "nano " + path + selected, false, id); if (display.getCurrent() == preview) { reload(); } setLabel(); } } 
            else if (c == DELETE) { 
                if (selected.equals("..")) {  }
                else if (path.equals("/home/") || path.equals("/tmp/") || (path.startsWith("/mnt/") && !path.equals("/mnt/"))) {
                    int STATUS = deleteFile(path + selected); 
                    if (STATUS != 0) { warnCommand(form.getTitle(), STATUS == 13 ? "Permission denied!" : "java.io.IOException"); } 
                    
                    reload(); 
                } 
                else { warnCommand(form.getTitle(), "read-only storage"); }
            } 
            else if (c == RUNS) { if (selected.equals("..") || selected.endsWith("/")) { } else { processCommand("xterm"); processCommand(". " + path + selected, true, id); } } 
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
                        } catch (Exception e) { warnCommand(form.getTitle(), getCatch(e)); }
                        
                        monitor.append(new StringItem("Size:", String.valueOf(size)));
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

                if (c == KILL || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { STATUS = kill(PID, false, id); } 
                else if (c == VIEW) { processCommand("trace view " + PID, false, id); } 
                else if (c == LOAD) {
                    if (!getowner(PID).equals(username) && id != 0) { STATUS = 13; }

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
                    processCommand(". /home/.initrc");
                    setLabel();
                }
            } 
            else if (c == EXIT) { processCommand("exit", false); }
        } 
        else if (MOD == REQUEST) {
            String password = PASSWD.getString().trim();

            if (password.equals("")) { } 
            else if (String.valueOf(password.hashCode()).equals(passwd())) { processCommand("xterm"); processCommand(command, true, id); setLabel(); } 
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
                        processCommand("xterm", true, id); 
                        processCommand(getvalue("quest.cmd", "true"), true, id); 
                    } 
                } 
                else if (MOD == WEDIT) { 
                    String value = box.getString().trim(); 
                    if (value.equals("")) { }
                    else { 
                        attributes.put(getenv("edit.key"), env(value)); 
                        processCommand("xterm", true, id); 
                        processCommand(getvalue("edit.cmd", "true"), true, id); 
                    } 
                } 
                else if (MOD == LIST) { int index = preview.getSelectedIndex(); if (index >= 0) { processCommand("xterm", true, id); String key = env(preview.getString(index)); processCommand(getvalue(key, "log add warn An error occurred, '" + key + "' not found"), true, id); } } 
                else if (MOD == SCREEN) { processCommand("xterm", true, id); processCommand(getvalue("screen.button.cmd", "log add warn An error occurred, 'screen.button.cmd' not found"), true, id); } 
            } 
        }
    }
    public void commandAction(Command c, Item item) { if (c == RUN) { processCommand("xterm", true, id); processCommand((String) PKG.get(node + ".cmd"), true, id); } }

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

            Hashtable proc = genprocess(proc_name, id, null);
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
                        processCommand(command, true, id);
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
        else if (MOD == BG) { processCommand(command, ignore, id); }
        else if (MOD == ADDON) { while (trace.containsKey(PID)) { if (processCommand(command, true, id) != 0) { kill(PID, false, id); } } }
    }

    private void reload() { if (midlet.attributes.containsKey("J2EMU")) { new MIDletControl(MOD == MONITOR ? "monitor" : MOD == PROCESS ? "process" : MOD == EXPLORER ? "dir" : "history", id); } else { load(); } }
    private void load() {
        if (MOD == HISTORY) { preview.deleteAll(); for (int i = 0; i < history.size(); i++) { preview.append((String) history.elementAt(i), null); } } 
        else if (MOD == EXPLORER) {
            if (midlet.attributes.containsKey("J2EMU")) { }
            else { preview.setTitle(path); }

            preview.deleteAll();
            if (midlet.path.equals("/")) { }
            else { preview.append("..", null); }

            try {
                if (path.equals("/tmp/")) { for (Enumeration KEYS = midlet.tmp.keys(); KEYS.hasMoreElements();) { String file = (String) KEYS.nextElement(); if (!file.startsWith(".")) { preview.append(file, null); } } }
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

                String[] files = (String[]) midlet.paths.get(path);
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String f = files[i];

                        if (f != null && !f.equals("..") && !f.equals("/")) { preview.append(f, null); }
                    }
                }
            } catch (IOException e) { }
        } 
        else if (MOD == MONITOR) { console.setText("Used Memory: " + (midlet.runtime.totalMemory() - midlet.runtime.freeMemory()) / 1024 + " KB\n" + "Free Memory: " + midlet.runtime.freeMemory() / 1024 + " KB\n" + "Total Memory: " + midlet.runtime.totalMemory() / 1024 + " KB"); } 
        else if (MOD == PROCESS) { preview.deleteAll(); for (Enumeration keys = midlet.trace.keys(); keys.hasMoreElements();) { String PID = (String) keys.nextElement(), name = (String) ((Hashtable) trace.get(PID)).get("name"); if (pfilter.equals("") || name.indexOf(pfilter) != -1) { preview.append(PID + "\t" + name, null); } } }
    }

    private void back() { if (midlet.trace.containsKey(PID) && !asked) { confirm = new Alert("Background Process", "Keep this process running in background?", null, AlertType.WARNING); confirm.addCommand(YES = new Command("Yes", Command.OK, 1)); confirm.addCommand(NO = new Command("No", Command.BACK, 1)); confirm.setCommandListener(this); asked = true; display.setCurrent(confirm); } else { midlet.processCommand("xterm"); } }

    private int verifyHTTP(String fullUrl) throws IOException { HttpConnection H = null; try { H = (HttpConnection) Connector.open(fullUrl); H.setRequestMethod(HttpConnection.GET); return H.getResponseCode(); } finally { try { if (H != null) H.close(); } catch (IOException x) { } } }
    public static String passwd() { try { RecordStore RMS = RecordStore.openRecordStore("OpenRMS", true); if (RMS.getNumRecords() >= 2) { byte[] data = RMS.getRecord(2); if (data != null) { return new String(data); } } if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return ""; }

    public int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = replace(mode, "password", "").trim(); } int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY; return password ? (base | TextField.PASSWORD) : base; } 
    private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; } 
    private String getenv(String key, String fallback) { return midlet.env(getvalue(key, fallback)); } 
    private String getenv(String key) { return midlet.env(getvalue(key, "")); } 
}
// |
// EOF