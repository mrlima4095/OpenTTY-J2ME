import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// MIDlet Controller
public class MIDletControl implements ItemCommandListener, CommandListener, Runnable {
    public static final int HISTORY = 1, EXPLORER = 2, MONITOR = 3, PROCESS = 4, SIGNUP = 5, REQUEST = 7, LOCK = 8, NC = 9, PRSCAN = 10, GOBUSTER = 11, BIND = 12, SCREEN = 13, LIST = 14, QUEST = 15, WEDIT = 16, BG = 17, ADDON = 18;

    private OpenTTY midlet;
    private int MOD = -1, COUNT = 1, id = 1000, start;
    private boolean ignore = true, asked = false, keep = false, asking_user = false, asking_passwd = false;
    private String command = null, pfilter = "", PID = "", DB = "", address = "", port = "", node = "", proc_name = "";
    private Vector history = (Vector) midlet.getobject("1", "history");
    private Hashtable sessions = new Hashtable(), PKG = new Hashtable();
    private Alert confirm;
    private Form monitor;
    private List preview;
    private TextBox box;
    private StringItem console, s;
    private TextField USER, PASSWD, remotein;
    private Command BACK = new Command("Back", Command.BACK, 1), RUN, RUNS, IMPORT, OPEN, EDIT, REFRESH, PROPERTY, KILL, LOAD, DELETE, LOGIN, EXIT, FILTER, CONNECT, VIEW, SAVE, YES, NO, EXECUTE = midlet.EXECUTE, CLEAR = midlet.CLEAR;

    private SocketConnection CONN;
    private ServerSocketConnection server = null;
    private InputStream IN;
    private OutputStream OUT;

    private String[] wordlist;

    public MIDletControl(OpenTTY midlet, String command, int id) {
        this.midlet = midlet;
        this.id = id;
        MOD = command == null || command.length() == 0 || command.equals("monitor") ? MONITOR : command.equals("process") ? PROCESS : command.equals("dir") ? EXPLORER : command.equals("history") ? HISTORY : -1;

        history = (Vector) midlet.getobject("1", "history"); 
        sessions = (Hashtable) midlet.getobject("1", "sessions"); 
        PID = midlet.genpid(); 

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
            if (MOD == EXPLORER) { preview.addCommand(OPEN = new Command("Open", Command.OK, 1)); } 
            else if (MOD == PROCESS) { preview.addCommand(KILL = new Command("Kill", Command.OK, 1)); } 
            else { preview.addCommand(RUN = new Command("Run", Command.OK, 1)); }

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
            load();
            midlet.display.setCurrent(preview);
        }
    }
    public MIDletControl(OpenTTY midlet, String command) {
        this.midlet = midlet;
        asking_user = midlet.username.equals("");
        asking_passwd = passwd().equals("");
        MOD = command == null || command.length() == 0 || command.equals("login") ? SIGNUP : REQUEST;
        monitor = new Form(midlet.form.getTitle());

        if (MOD == SIGNUP) {
            monitor.append(new StringItem(null, midlet.env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\n" + (asking_user && asking_passwd ? "Create your credentials!" : asking_user ? "Create an user to access OpenTTY!" : asking_passwd ? "Create a password!" : "")).trim(), Item.PLAIN));

            if (asking_user) { monitor.append(USER = new TextField("Username", "", 256, TextField.ANY)); }
            if (asking_passwd) { monitor.append(PASSWD = new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); }

            monitor.addCommand(LOGIN = new Command("Login", Command.OK, 1));
            monitor.addCommand(EXIT = new Command("Exit", Command.SCREEN, 2));
        } 
        else {
            if (asking_passwd) { new MIDletControl(midlet, null); return; }
            this.command = command;

            monitor.append(PASSWD = new TextField("[sudo] password for " + midlet.loadRMS("OpenRMS"), "", 256, TextField.ANY | TextField.PASSWORD));
            monitor.addCommand(EXECUTE = new Command("Execute", Command.OK, 1)); 
            monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
        }

        monitor.setCommandListener(this);
        midlet.display.setCurrent(monitor);
    }
    public MIDletControl(OpenTTY midlet, String mode, String args, int id) {
        this.midlet = midlet; this.id = id;
        MOD = mode == null || mode.length() == 0 || mode.equals("nc") ? NC : mode.equals("prscan") ? PRSCAN : mode.equals("gobuster") ? GOBUSTER : mode.equals("bind") ? BIND : -1;

        if (args == null || args.length() == 0 || MOD == -1) { return; } 
        else if (MOD == BIND) {
            String[] argv = midlet.splitArgs(args);

            port = argv[0];
            DB = argv.length > 1 ? argv[1] : "";
            proc_name = argv.length > 2 ? argv[2] : "bind";

            new Thread(this, "Bind").start();
            return;
        }

        Hashtable proc = midlet.genprocess(MOD == NC ? "remote" : MOD == PRSCAN ? "prscan" : "gobuster", id, null); // Fixed: Delegate

        if (MOD == NC) {
            address = args;
            try {
                CONN = (SocketConnection) Connector.open("socket://" + address);
                IN = CONN.openInputStream(); OUT = CONN.openOutputStream(); 
            } catch (Exception e) { midlet.echoCommand(midlet.getCatch(e)); return; }

            monitor = new Form(midlet.form.getTitle());
            monitor.append(console = new StringItem("", ""));
            monitor.append(remotein = new TextField("Remote (" + midlet.split(address, ':')[0] + ")", "", 256, TextField.ANY)); // Fixed: Delegate split
            monitor.addCommand(EXECUTE);
            monitor.addCommand(BACK = new Command("Back", Command.SCREEN, 1));
            monitor.addCommand(CLEAR);
            monitor.addCommand(VIEW = new Command("View info", Command.SCREEN, 1));
            monitor.setCommandListener(this);

            proc.put("socket", CONN);
            proc.put("in-stream", IN);
            proc.put("out-stream", OUT);
            proc.put("screen", monitor);
            midlet.display.setCurrent(monitor);
        } else {
            address = midlet.getCommand(args);
            preview = new List(MOD == PRSCAN ? address + " Ports" : "GoBuster (" + address + ")", List.IMPLICIT);

            if (MOD == PRSCAN) {
                start = midlet.getNumber(midlet.getArgument(args).equals("") ? "1" : midlet.getArgument(args), 1, true); // Fixed: Delegate
            } else {
                wordlist = midlet.split(midlet.getArgument(args).equals("") ? midlet.loadRMS("gobuster") : midlet.getcontent(midlet.getArgument(args)), '\n'); // Fixed: Delegate
                if (wordlist == null || wordlist.length == 0) {
                    midlet.echoCommand("gobuster: blank word list"); 
                    return;
                }
            }

            preview.addCommand(BACK = new Command("Back", Command.SCREEN, 2));
            preview.addCommand(CONNECT = new Command("Connect", Command.BACK, 1));
            preview.addCommand(SAVE = new Command("Save Logs", Command.SCREEN, 2));
            preview.setCommandListener(this);

            proc.put("screen", preview);
            midlet.display.setCurrent(preview);
        }

        midlet.trace.put(PID, proc);
        new Thread(this, "NET").start();
    }
    public MIDletControl(OpenTTY midlet, Form screen, String node, String code, int id) { this.midlet = midlet; this.node = node; this.id = id; if (code == null || code.length() == 0) { return; } PKG = midlet.parseProperties(code); if (!PKG.containsKey(node + ".label") || !PKG.containsKey(node + ".cmd")) { midlet.MIDletLogs("add error Malformed ITEM, missing params"); return; } RUN = new Command(getenv(node + ".label", ""), Command.ITEM, 1); s = new StringItem(null, getenv(node + ".label", ""), StringItem.BUTTON); s.setFont(midlet.newFont(getenv(node + ".style", "default")));  s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(RUN); s.setDefaultCommand(RUN); s.setItemCommandListener(this); screen.append(s); }
    public MIDletControl(OpenTTY midlet, int MOD, String code, int id) {
        this.midlet = midlet; this.MOD = MOD; this.id = id;

        if (code == null || code.length() == 0) { return; }

        PKG = midlet.parseProperties(code);

        if (MOD == SCREEN) {
            monitor = new Form(getenv("screen.title", midlet.form.getTitle())); 
            BACK = new Command(getenv("screen.back.label", "Back"), Command.OK, 1);
            RUN = new Command(getenv("screen.button", "Menu"), Command.SCREEN, 2);
            monitor.addCommand(BACK);
            if (PKG.containsKey("screen.button")) { monitor.addCommand(RUN); }
            if (PKG.containsKey("screen.fields")) {
                String[] fields = midlet.split(getenv("screen.fields"), ','); 

                for (int i = 0; i < fields.length; i++) {
                    String field = fields[i].trim();
                    String type = getenv("screen." + field + ".type");

                    if (type.equals("image") && !getenv("screen." + field + ".img").equals("")) {
                        try { monitor.append(new ImageItem(null, Image.createImage(getenv("screen." + field + ".img")), ImageItem.LAYOUT_CENTER, null)); } 
                        catch (Exception e) { midlet.MIDletLogs("add warn Malformed Image '" + getenv("screen." + field + ".img") + "'"); }
                    } 
                    else if (type.equals("text") && !getenv("screen." + field + ".value").equals("")) {
                        StringItem content = new StringItem(getenv("screen." + field + ".label"), getenv("screen." + field + ".value"));

                        content.setFont(midlet.newFont(getenv("screen." + field + ".style", "default"))); 
                        monitor.append(content);
                    } 
                    else if (type.equals("item")) { new MIDletControl(midlet, monitor, "screen." + field, code, id); } 
                    else if (type.equals("spacer")) {
                        int width = Integer.parseInt(getenv("screen." + field + ".w", "1")), height = Integer.parseInt(getenv("screen." + field + ".h", "10"));
                        monitor.append(new Spacer(width, height));
                    }
                }
            }

            monitor.setCommandListener(this);
            midlet.display.setCurrent(monitor);
        } 
        else if (MOD == LIST) {
            Image IMG = null;

            if (!PKG.containsKey("list.content")) { midlet.MIDletLogs("add error List crashed while init, malformed settings"); return; }

            preview = new List(getenv("list.title", midlet.form.getTitle()), List.IMPLICIT);
            if (PKG.containsKey("list.icon")) {
                try { IMG = Image.createImage(getenv("list.icon")); } 
                catch (Exception e) { midlet.MIDletLogs("add warn Malformed Image '" + getenv("list.icon") + "'"); }
            }

            preview.addCommand(BACK = new Command(getenv("list.back.label", "Back"), Command.OK, 1));
            preview.addCommand(RUN = new Command(getenv("list.button", "Select"), Command.SCREEN, 2));

            String[] content = midlet.split(getenv("list.content"), ',');
            for (int i = 0; i < content.length; i++) { preview.append(content[i], IMG); }

            if (PKG.containsKey("list.source")) {
                String source = midlet.getcontent(getenv("list.source"));

                if (!source.equals("")) {
                    String[] lines = midlet.split(source, '\n'); 
                    for (int i = 0; i < lines.length; i++) {
                        String key = lines[i], value = "true";
                        
                        int index = lines[i].indexOf("=");
                        if (index != -1) {
                            value = key.substring(index + 1);
                            key = key.substring(0, index);
                        }
                        
                        preview.append(key, IMG);
                        PKG.put(key, value);
                    }
                }
            }

            preview.setCommandListener(this);
            midlet.display.setCurrent(preview);
        } 
        else if (MOD == QUEST) {
            if (!PKG.containsKey("quest.label") || !PKG.containsKey("quest.cmd") || !PKG.containsKey("quest.key")) { midlet.MIDletLogs("add error Quest crashed while init, malformed settings"); return; }
            monitor = new Form(getenv("quest.title", midlet.form.getTitle())); 

            monitor.append(USER = new TextField(getenv("quest.label"), getenv("quest.content", ""), 256, getQuest(getenv("quest.type", ""))));
            monitor.addCommand(BACK = new Command(getvalue("quest.back.label", "Cancel"), Command.SCREEN, 2));
            monitor.addCommand(RUN = new Command(getvalue("quest.cmd.label", "Send"), Command.OK, 1));

            monitor.setCommandListener(this);
            midlet.display.setCurrent(monitor);
        } 
        else if (MOD == WEDIT) {
            if (!PKG.containsKey("edit.cmd") || !PKG.containsKey("edit.key")) { midlet.MIDletLogs("add error Editor crashed while init, malformed settings"); return; }
            
            String content = PKG.containsKey("edit.content") ? getenv("edit.content") : PKG.containsKey("edit.source") ? midlet.getcontent(getenv("edit.source")) : "";
            box = new TextBox(getenv("edit.title", midlet.form.getTitle()), content, 31522, getQuest(getenv("edit.type", ""))); 

            box.addCommand(BACK = new Command(getenv("edit.back.label", "Back"), Command.OK, 1));
            box.addCommand(RUN = new Command(getenv("edit.cmd.label", "Run"), Command.SCREEN, 2));

            box.setCommandListener(this);
            midlet.display.setCurrent(box);
        } 
        else { return; }
    }
    public MIDletControl(OpenTTY midlet, String name, String command, boolean ignore, int id) { this.midlet = midlet; this.MOD = BG; this.command = command; this.ignore = ignore; this.id = id; new Thread(this, name).start(); }
    public MIDletControl(OpenTTY midlet, String pid, String name, String command, boolean ignore, int id) { this.midlet = midlet; this.MOD = ADDON; this.PID = pid; this.command = command; this.ignore = ignore; this.id = id; new Thread(this, name).start(); }

    public void commandAction(Command c, Displayable d) {
        if (c == BACK) {
            if (d == box || (d == monitor && MOD == EXPLORER)) { midlet.display.setCurrent(preview); } 
            else if (MOD == NC || MOD == PRSCAN || MOD == GOBUSTER) { back(); } 
            else if (MOD == SCREEN || MOD == LIST || MOD == QUEST || MOD == WEDIT) { midlet.processCommand("xterm"); midlet.processCommand(getvalue((MOD == SCREEN ? "screen" : MOD == LIST ? "list" : MOD == QUEST ? "quest" : "edit") + ".back", "true"), true, id); } 
            else { midlet.processCommand("xterm"); }
            
            return;
        }
        if (d == confirm) {
            if (c == YES) { keep = true; } 
            else { midlet.trace.remove(PID); }

            midlet.processCommand("xterm"); return;
        }
        if (d == box) { pfilter = box.getString().trim(); load(); midlet.display.setCurrent(preview); return; }

        if (MOD == HISTORY) {
            String selected = preview.getString(preview.getSelectedIndex());
            if (selected != null) {
                midlet.processCommand("xterm"); 
                midlet.processCommand(c == RUN || c == List.SELECT_COMMAND ? selected : "buff " + selected, true, id);
            }
        } 
        else if (MOD == EXPLORER) {
            String selected = preview.getString(preview.getSelectedIndex());

            if (c == OPEN || (c == List.SELECT_COMMAND && !midlet.attributes.containsKey("J2EMU"))) { 
                if (selected != null) {
                    midlet.processCommand(selected.endsWith("..") ? "cd .." : selected.endsWith("/") ? "cd " + midlet.path + selected : "nano " + midlet.path + selected, false, id); 
                    if (midlet.display.getCurrent() == preview) { reload(); }
                    midlet.setLabel();
                }
            } 
            else if (c == DELETE) {
                if (selected.equals("..")) { } 
                else if (midlet.path.equals("/home/") || midlet.path.equals("/tmp/") || (midlet.path.startsWith("/mnt/") && !midlet.path.equals("/mnt/"))) {
                    int STATUS = midlet.deleteFile(midlet.path + selected);
                    if (STATUS != 0) { midlet.warnCommand(midlet.form.getTitle(), STATUS == 13 ? "Permission denied!" : "java.io.IOException"); }

                    reload();
                } 
                else { midlet.warnCommand(midlet.form.getTitle(), "read-only storage"); }
            } 
            else if (c == RUNS) { if (!selected.equals("..") && !selected.endsWith("/")) { midlet.processCommand("xterm"); midlet.processCommand(". " + midlet.path + selected, true, id); } } 
            else if (c == PROPERTY) {
                if (selected.equals("..")) { } 
                else { 
                    String[] info = midlet.getExtensionInfo(midlet.getExtension(selected));
                    String type = selected.endsWith("/") ? "Directory" : 
                                  (midlet.path.startsWith("/home/") || midlet.path.startsWith("/tmp/")) ? (info[0].equals("Unknown") ? "ASCII text" : info[0]) : 
                                  midlet.path.startsWith("/bin/") ? "Application" : midlet.path.startsWith("/dev/") ? "Special Device" : midlet.path.startsWith("/lib/") ? "Shared Package" : 
                                  (midlet.path.equals("/tmp/") || midlet.path.equals("/res/")) ? "ASCII text" : info[0]; 
                    
                    monitor = new Form(selected + " - Information");
                    monitor.addCommand(BACK);
                    monitor.append(new StringItem("File:", midlet.path + selected));
                    monitor.append(new StringItem("Type:", type));
                    if (type.equals("Directory")) { } 
                    else {
                        int size = 0;
                        try {
                            InputStream in = midlet.readRaw(midlet.path + selected); 
                            size = in.available();
                            in.close();
                        } catch (Exception e) { midlet.warnCommand(midlet.form.getTitle(), midlet.getCatch(e)); }
                        
                        monitor.append(new StringItem("Size:", String.valueOf(size)));
                        ChoiceGroup perms = new ChoiceGroup("Permissions", Choice.MULTIPLE);
                        perms.append("Read", null); perms.append("Write", null);

                        perms.setSelectedIndex(0, true);
                        perms.setSelectedIndex(1, (midlet.path.startsWith("/home/") || midlet.path.startsWith("/tmp/") || (midlet.path.startsWith("/mnt/") && !midlet.path.equals("/mnt/")) || 
                                                  (selected = midlet.path + selected).equals("/dev/null") || selected.equals("/dev/stdin") || selected.equals("/dev/stdout"))); // Fixed: Delegate path

                        monitor.append(perms);
                        if (info[2].equals("image")) {
                            try { monitor.append(new ImageItem(null, midlet.readImg(midlet.path + selected), ImageItem.LAYOUT_DEFAULT, null)); } 
                            catch (Exception e) { }
                        }
                        if (info[2].equals("text") || midlet.path.startsWith("/home/") || midlet.path.startsWith("/tmp/")) { monitor.addCommand(OPEN); }
                    }
                    monitor.setCommandListener(this);
                    midlet.display.setCurrent(monitor);
                }
            } 
            else if (c == REFRESH) { reload(); }
        } 
        else if (MOD == MONITOR) {
            System.gc();
            reload();
        } 
        else if (MOD == PROCESS) {
            if (c == FILTER) {
                if (box == null) {
                    box = new TextBox("Process Filter", "", 31522, TextField.ANY);
                    box.addCommand(RUN = new Command("Apply", Command.OK, 1));
                    box.setCommandListener(this);
                }
                midlet.display.setCurrent(box);
                return;
            } 
            else if (c == REFRESH) { reload(); }

            int index = preview.getSelectedIndex();
            if (index >= 0) {
                String PID = midlet.split(preview.getString(index), '\t')[0];
                int STATUS = 0;

                if (c == KILL || (c == List.SELECT_COMMAND && !midlet.attributes.containsKey("J2EMU"))) { STATUS = midlet.kill(PID, false, id); } 
                else if (c == VIEW) { midlet.processCommand("trace view " + PID, false, id); } 
                else if (c == LOAD) { 
                    if (!midlet.getowner(PID).equals(midlet.username) && id != 0) { STATUS = 13; }

                    Displayable screen = (Displayable) midlet.getobject(PID, "screen"); 

                    if (screen == null) { STATUS = 69; } 
                    else { midlet.display.setCurrent(screen); return; }
                }

                if (STATUS != 0) { midlet.warnCommand(midlet.form.getTitle(), STATUS == 13 ? "Permission denied!" : "No screens for this process!"); }

                reload();
            }
        } 
        else if (MOD == SIGNUP) {
            if (c == LOGIN) {
                String password = PASSWD.getString().trim();

                if (asking_user) { midlet.username = USER.getString().trim(); }
                if ((asking_user && midlet.username.equals("")) || (asking_passwd && password.equals(""))) { midlet.warnCommand(monitor.getTitle(), "Missing credentials!"); } 
                else if (midlet.username.equals("root")) { 
                    USER.setString("");
                    midlet.warnCommand(monitor.getTitle(), "Invalid username!"); 
                } else {
                    if (asking_user) { midlet.writeRMS("/home/OpenRMS", midlet.username); }
                    if (asking_passwd) { midlet.writeRMS("OpenRMS", String.valueOf(password.hashCode()).getBytes(), 2); }

                    midlet.display.setCurrent(midlet.form);
                    midlet.processCommand(". /home/.initrc");
                    this.midlet.setLabel(); 
                }
            } 
            else if (c == EXIT) { midlet.processCommand("exit", false); }
        } 
        else if (MOD == REQUEST) {
            String password = PASSWD.getString().trim();

            if (password.equals("")) { } 
            else if (String.valueOf(password.hashCode()).equals(passwd())) { midlet.processCommand("xterm"); midlet.processCommand(command, true, id); midlet.setLabel(); } 
            else { PASSWD.setString(""); midlet.warnCommand(monitor.getTitle(), "Wrong password"); }
        } 
        else if (MOD == NC) {
            if (c == EXECUTE) {
                String PAYLOAD = remotein.getString().trim();
                remotein.setString("");

                try { OUT.write((PAYLOAD + "\n").getBytes()); OUT.flush(); } 
                catch (Exception e) {
                    midlet.warnCommand(monitor.getTitle(), midlet.getCatch(e)); 
                    if (keep) { } 
                    else { midlet.trace.remove(PID); }
                }
            } 
            else if (c == BACK) { midlet.writeRMS("/home/remote", console.getText()); back(); } 
            else if (c == CLEAR) { console.setText(""); } 
            else if (c == VIEW) { 
                try { midlet.warnCommand("Information", "Host: " + midlet.split(address, ':')[0] + "\n" + "Port: " + midlet.split(address, ':')[1] + "\n\n" + "Local Address: " + CONN.getLocalAddress() + "\n" + "Local Port: " + CONN.getLocalPort()); } 
                catch (Exception e) { midlet.warnCommand(monitor.getTitle(), "Couldn't read connection information!"); }
            }
        } 
        else if (MOD == PRSCAN || MOD == GOBUSTER) {
            if (c == CONNECT || c == List.SELECT_COMMAND) {
                String ITEM = preview.getString(preview.getSelectedIndex());
                midlet.processCommand(MOD == PRSCAN ? "nc " + address + ":" + ITEM : "execute tick Downloading...; wget " + address + "/" + midlet.getArgument(ITEM) + "; tick; nano; true", false);
            } 
            else if (c == SAVE) {
                StringBuffer BUFFER = new StringBuffer();
                for (int i = 0; i < preview.size(); i++) { BUFFER.append(MOD == PRSCAN ? preview.getString(i) : midlet.getArgument(preview.getString(i))).append("\n"); }

                midlet.nanoContent = BUFFER.toString().trim(); 
                midlet.processCommand("nano", false); 
            }
        } 
        else {
            if (c == RUN || c == List.SELECT_COMMAND) {
                if (MOD == QUEST) {
                    String value = USER.getString().trim();
                    if (!value.equals("")) {
                        midlet.attributes.put(getenv("quest.key"), midlet.env(value)); 
                        midlet.processCommand("xterm", true, id); 
                        midlet.processCommand(getvalue("quest.cmd", "true"), true, id); 
                    }
                } else if (MOD == WEDIT) {
                    String value = box.getString().trim();
                    if (!value.equals("")) {
                        midlet.attributes.put(getenv("edit.key"), midlet.env(value)); 
                        midlet.processCommand("xterm", true, id);
                        midlet.processCommand(getvalue("edit.cmd", "true"), true, id); 
                    }
                } else if (MOD == LIST) {
                    int index = preview.getSelectedIndex();
                    if (index >= 0) {
                        midlet.processCommand("xterm", true, id); 
                        String key = midlet.env(preview.getString(index)); 
                        midlet.processCommand(getvalue(key, "log add warn An error occurred, '" + key + "' not found"), true, id); 
                    }
                } else if (MOD == SCREEN) {
                    midlet.processCommand("xterm", true, id); 
                    midlet.processCommand(getvalue("screen.button.cmd", "log add warn An error occurred, 'screen.button.cmd' not found"), true, id); 
                }
            }
        }
    }
    public void commandAction(Command c, Item item) { if (c == RUN) { midlet.processCommand("xterm", true, id); midlet.processCommand((String) PKG.get(node + ".cmd"), true, id); } }

    public void run() {
        if (MOD == NC) {
            while (midlet.trace.containsKey(PID)) { 
                try {
                    if (IN.available() > 0) {
                        byte[] BUFFER = new byte[IN.available()];
                        int LENGTH = IN.read(BUFFER);
                        if (LENGTH > 0) { midlet.echoCommand((new String(BUFFER, 0, LENGTH)).trim(), console); }
                    }
                    Thread.sleep(100); 
                } catch (Exception e) {
                    midlet.warnCommand(midlet.form.getTitle(), midlet.getCatch(e)); 
                    if (keep) { } else { midlet.trace.remove(PID); }
                    break;
                }
            }

            try { if (IN != null) { IN.close(); } if (OUT != null) { OUT.close(); } if (CONN != null) { CONN.close(); } } 
            catch (Exception e) { }

            return;
        } 
        else if (MOD == PRSCAN) {
            for (int port = start; port <= 65535; port++) {
                try {
                    preview.setTicker(new Ticker("Scanning port " + port + "..."));

                    if (midlet.trace.containsKey(PID)) { Connector.open("socket://" + address + ":" + port, Connector.READ_WRITE, true).close(); preview.append("" + port, null); }
                    else { break; }
                } catch (IOException e) { }
            }
            preview.setTicker(null);
            if (keep) { } else { midlet.trace.remove(PID); }

            return;
        } else if (MOD == GOBUSTER) {
            preview.setTicker(new Ticker("Searching..."));
            for (int i = 0; i < wordlist.length; i++) {
                String path = wordlist[i].trim();

                if (midlet.trace.containsKey(PID)) { 
                    if (!path.equals("") && !path.startsWith("#")) {
                        try {
                            int code = verifyHTTP(address.startsWith("http") ? address + "/" + path : "http://" + address + "/" + path);
                            if (code != 404) { preview.append(code + " /" + path, null); }
                        } 
                        catch (IOException e) { } }
                } 
                else { break; }
            }
            preview.setTicker(null);
            if (keep) { } else { midlet.trace.remove(PID); }

            return;
        } else if (MOD == BIND) {
            if (sessions.containsKey(port)) { midlet.echoCommand("[-] Port '" + port + "' is unavailable"); return; }

            Hashtable proc = midlet.genprocess(proc_name, id, null);
            proc.put("port", port);
            midlet.trace.put(PID, proc);
            sessions.put(port, "nobody");

            ServerSocketConnection localServer = null;
            SocketConnection localConn = null;
            InputStream localIn = null;
            OutputStream localOut = null;

            try {
                localServer = (ServerSocketConnection) Connector.open("socket://:" + port);
                proc.put("server", localServer);
                if (COUNT == 1) {
                    midlet.echoCommand("[+] listening on port " + port); 
                    midlet.MIDletLogs("add info Server listening on port " + port);
                    COUNT++;
                }

                while (midlet.trace.containsKey(PID)) { 
                    try {
                        localConn = (SocketConnection) localServer.acceptAndOpen();
                        address = localConn.getAddress();
                        midlet.echoCommand("[+] " + address + " connected");

                        localIn = localConn.openInputStream();
                        localOut = localConn.openOutputStream();
                        proc.put("in-stream", localIn);
                        proc.put("out-stream", localOut);

                        sessions.put(port, address);
                        while (midlet.trace.containsKey(PID)) { 
                            byte[] buffer = new byte[4096];
                            int bytesRead = localIn.read(buffer);
                            if (bytesRead == -1) {
                                midlet.echoCommand("[-] " + address + " disconnected"); 
                                break;
                            }
                            String PAYLOAD = new String(buffer, 0, bytesRead).trim();
                            midlet.echoCommand("[+] " + address + " -> " + midlet.env(PAYLOAD));

                            String commandStr = (DB == null || DB.length() == 0 || DB.equals("null")) ? PAYLOAD : DB + " " + PAYLOAD;

                            String before = (midlet.stdout != null) ? midlet.stdout.getText() : ""; 
                            midlet.processCommand(commandStr, true, id); 
                            String after = (midlet.stdout != null) ? midlet.stdout.getText() : ""; 

                            String output = after.length() >= before.length() ? after.substring(before.length()).trim() + "\n" : after + "\n";

                            localOut.write(output.getBytes()); localOut.flush();
                        }
                    } finally {
                        try { if (localIn != null) { localIn.close(); } } catch (IOException e) { }
                        try { if (localOut != null) { localOut.close(); } } catch (IOException e) { }
                        try { if (localConn != null) { localConn.close(); } } catch (IOException e) { }
                        
                        sessions.put(port, "nobody");
                    }
                }
            } catch (IOException e) {
                midlet.echoCommand("[-] " + midlet.getCatch(e)); 
                if (COUNT == 1) { midlet.echoCommand("[-] Server crashed"); }
            } finally {
                try { if (localServer != null) localServer.close(); } 
                catch (IOException e) { }
            }
            
            midlet.trace.remove(PID); 
            sessions.remove(port);
            midlet.echoCommand("[-] Server stopped"); 
            midlet.MIDletLogs("add info Server was stopped"); 
        } 
        else if (MOD == BG) { midlet.processCommand(command, ignore, id); } 
        else if (MOD == ADDON) {
            while (midlet.trace.containsKey(PID)) { if (midlet.processCommand(command, true, id) != 0) { midlet.kill(PID, false, id); } }
        }
    }

    private void reload() {
        if (midlet.attributes.containsKey("J2EMU")) { new MIDletControl(midlet, MOD == MONITOR ? "monitor" : MOD == PROCESS ? "process" : MOD == EXPLORER ? "dir" : "history", id); } 
        else { load(); }
    }

    private void load() {
        if (MOD == HISTORY) {
            if (preview != null) {
                preview.deleteAll();
                for (int i = 0; i < history.size(); i++) { preview.append((String) history.elementAt(i), null); }
            }
        } else if (MOD == EXPLORER) {
            if (midlet.attributes.containsKey("J2EMU")) { } 
            else if (preview != null) { preview.setTitle(midlet.path); }

            if (preview != null) {
                preview.deleteAll();
                if (!midlet.path.equals("/")) { preview.append("..", null); }

                try {
                    if (midlet.path.equals("/tmp/")) { 
                        for (Enumeration KEYS = midlet.tmp.keys(); KEYS.hasMoreElements();) { 
                            String file = (String) KEYS.nextElement();
                            if (!file.startsWith(".")) { preview.append(file, null); }
                        }
                    } 
                    else if (midlet.path.equals("/mnt/")) { for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { preview.append((String) roots.nextElement(), null); } } 
                    else if (midlet.path.startsWith("/mnt/")) {
                        FileConnection CONN = (FileConnection) Connector.open("file:///" + midlet.path.substring(5), Connector.READ); // Fixed: Delegate path
                        Vector dirs = new Vector(), files = new Vector();

                        for (Enumeration content = CONN.list(); content.hasMoreElements();) {
                            String name = (String) content.nextElement();

                            if (name.endsWith("/")) { dirs.addElement(name); } 
                            else { files.addElement(name); }
                        }

                        while (!dirs.isEmpty()) { preview.append(getFirstString(dirs), null); dirs.removeElementAt(0); }
                        while (!files.isEmpty()) { preview.append(getFirstString(files), null); files.removeElementAt(0); }

                        CONN.close();
                    } else if (midlet.path.startsWith("/home/")) { 
                        String[] recordStores = RecordStore.listRecordStores();

                        for (int i = 0; i < recordStores.length; i++) {
                            if (!recordStores[i].startsWith(".")) { preview.append(recordStores[i], null); }
                        }
                    }

                    String[] files = (String[]) midlet.paths.get(midlet.path); 
                    if (files != null) {
                        for (int i = 0; i < files.length; i++) {
                            String f = files[i];

                            if (f != null && !f.equals("..") && !f.equals("/")) { preview.append(f, null); }
                        }
                    }
                } 
                catch (IOException e) { midlet.warnCommand(midlet.form.getTitle(), midlet.getCatch(e)); }
            }
        } 
        else if (MOD == MONITOR) { if (console != null) { console.setText("Used Memory: " + ((midlet.runtime.totalMemory() - midlet.runtime.freeMemory()) / 1024) + " KB\n" + "Free Memory: " + (midlet.runtime.freeMemory() / 1024) + " KB\n" + "Total Memory: " + (midlet.runtime.totalMemory() / 1024) + " KB"); } } 
        else if (MOD == PROCESS) { 
            if (preview != null) {
                preview.deleteAll();
                for (Enumeration keys = midlet.trace.keys(); keys.hasMoreElements();) { 
                    String PID = (String) keys.nextElement();
                    Hashtable procData = (Hashtable) midlet.trace.get(PID); 
                    String name = (procData != null) ? (String) procData.get("name") : "";
                    if (pfilter.equals("") || name.indexOf(pfilter) != -1) { preview.append(PID + "\t" + name, null); }
                }
            }
        }
    }

    private void back() {
        if (midlet.trace.containsKey(PID) && !asked) { 
            confirm = new Alert("Background Process", "Keep this process running in background?", null, AlertType.WARNING);
            confirm.addCommand(YES = new Command("Yes", Command.OK, 1));
            confirm.addCommand(NO = new Command("No", Command.BACK, 1));
            confirm.setCommandListener(this);
            asked = true;
            midlet.display.setCurrent(confirm);
        } 
        else { midlet.processCommand("xterm"); }
    }

    private int verifyHTTP(String fullUrl) throws IOException {
        HttpConnection H = null;
        try {
            H = (HttpConnection) Connector.open(fullUrl);
            H.setRequestMethod(HttpConnection.GET);
            return H.getResponseCode();
        } finally { try { if (H != null) H.close(); } catch (IOException x) { } }
    }

    public static String passwd() {
        try {
            RecordStore RMS = RecordStore.openRecordStore("OpenRMS", true);
            if (RMS.getNumRecords() >= 2) {
                byte[] data = RMS.getRecord(2);
                if (data != null) {
                    String result = new String(data);
                    RMS.closeRecordStore();
                    return result;
                }
            }
            if (RMS != null) { RMS.closeRecordStore(); }
        } catch (RecordStoreException e) { }

        return "";
    }

    public int getQuest(String mode) {
        if (mode == null || mode.length() == 0) { return TextField.ANY; }
        boolean password = false;
        if (mode.indexOf("password") != -1) {
            password = true;
            mode = midlet.replace(mode, "password", "").trim(); 
        }
        int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY;
        return password ? (base | TextField.PASSWORD) : base;
    }

    private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; }
    private String getenv(String key, String fallback) { return midlet.env(getvalue(key, fallback)); }
    private String getenv(String key) { return midlet.env(getvalue(key, "")); }

    private String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(result) < 0) { result = cur; } } v.removeElement(result); return result; } 
}
// |
// EOF