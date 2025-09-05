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
    private int MAX_STDOUT_LEN = -1, cursorX = 10, cursorY = 10;
    // |
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Hashtable attributes = new Hashtable(), paths = new Hashtable(), trace = new Hashtable(),
                     aliases = new Hashtable(), shell = new Hashtable(), functions = new Hashtable();
    public String username = loadRMS("OpenRMS"), nanoContent = loadRMS("nano");
    private String logs = "", path = "/home/", build = "2025-1.16.1-02x66";
    private Display display = Display.getDisplay(this);
    private TextBox nano = new TextBox("Nano", "", 31522, TextField.ANY);
    public Form form = new Form("OpenTTY " + getAppProperty("MIDlet-Version"));
    public TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    public StringItem stdout = new StringItem("", "Welcome to OpenTTY " + getAppProperty("MIDlet-Version") + "\nCopyright (C) 2025 - Mr. Lima\n");
    private Command EXECUTE = new Command("Send", Command.OK, 0), HELP = new Command("Help", Command.SCREEN, 1), NANO = new Command("Nano", Command.SCREEN, 2), CLEAR = new Command("Clear", Command.SCREEN, 3), HISTORY = new Command("History", Command.SCREEN, 4),
                    BACK = new Command("Back", Command.BACK, 1), RUNS = new Command("Run Script", Command.OK, 1), IMPORT = new Command("Import File", Command.OK, 1), VIEW = new Command("View as HTML", Command.OK, 1);
    // |
    // MIDlet Loader
    public void startApp() { 
        if (trace.containsKey("1")) { }
        else {
            attributes.put("PATCH", "Absurd Anvil"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.6.3");
            // |
            String[] KEYS = { "TYPE", "CONFIG", "PROFILE", "LOCALE" }, SYS = { "platform", "configuration", "profiles", "locale" };
            for (int i = 0; i < KEYS.length; i++) { attributes.put(KEYS[i], System.getProperty("microedition." + SYS[i])); }
            // |
            Command[] NANO_CMDS = { BACK, CLEAR, RUNS, IMPORT, VIEW }; for (int i = 0; i < NANO_CMDS.length; i++) { nano.addCommand(NANO_CMDS[i]); } nano.setCommandListener(this);
            // |
            runScript(read("/java/etc/initd.sh"), true); stdin.setLabel(username + " " + path + " " + (username.equals("root") ? "#" : "$"));
            // |
            if (username.equals("") || MIDletControl.passwd().equals("")) { new MIDletControl(null); }
            else { runScript(loadRMS("initd")); }
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
            else { processCommand("execute xterm; " + (c == RUNS ? "." : c == IMPORT ? "import nano" : c == VIEW ? "html" : "true")); }
        } else {
            if (c == EXECUTE) { String command = stdin.getString().trim(); add2History(command); stdin.setString(""); processCommand(command); stdin.setLabel(username + " " + path + " " + (username.equals("root") ? "#" : "$")); }            
            else { processCommand(c == HELP ? "help" : c == NANO ? "nano" : c == CLEAR ? "clear" : c == HISTORY ? "history" : c == BACK ? "xterm" : "warn Invalid KEY (" + c.getLabel() + ") - " + c.getCommandType()); }
        }
    }
    // |
    // Control Thread
    public class MIDletControl implements CommandListener {
        private static final int HISTORY = 1, EXPLORER = 2, MONITOR = 3, PROCESS = 4, SIGNUP = 5, REQUEST = 7, LOCK = 8;
        private int MOD = 0;
        private boolean root = false, asking_user = username.equals(""), asking_passwd = passwd().equals(""); 
        private String command = null;
        private Vector history = (Vector) getobject("1", "history");
        private Form monitor = new Form(form.getTitle());
        private List preview = new List(form.getTitle(), List.IMPLICIT);
        private StringItem status = new StringItem("Memory Status:", "");
        private TextField USER = new TextField("Username", "", 256, TextField.ANY), 
                          PASSWD = new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD); 
        private Command BACK = new Command("Back", Command.BACK, 1), RUN = new Command("Run", Command.OK, 1), RUNS = new Command("Run Script", Command.OK, 1), IMPORT = new Command("Import File", Command.OK, 1),
                    OPEN = new Command("Open", Command.OK, 1), EDIT = new Command("Edit", Command.OK, 1), REFRESH = new Command("Refresh", Command.SCREEN, 2), KILL = new Command("Kill", Command.OK, 1), LOAD = new Command("Load Screen", Command.OK, 1), 
                    VIEW = new Command("View info", Command.OK, 1), DELETE = new Command("Delete", Command.OK, 1), LOGIN = new Command("Login", Command.OK, 1), EXIT = new Command("Exit", Command.SCREEN, 2);
        
        public MIDletControl(String command, boolean root) {
            MOD = command == null || command.length() == 0 || command.equals("monitor") ? MONITOR : command.equals("process") ? PROCESS : command.equals("dir") ? EXPLORER : command.equals("history") ? HISTORY : -1;
            this.root = root;
            
            if (MOD == MONITOR) {
                monitor.append(status);
                monitor.addCommand(BACK); monitor.addCommand(REFRESH);
                monitor.setCommandListener(this);
                load(); display.setCurrent(monitor);
            } else {
                preview.addCommand(BACK); 
                
                preview.addCommand(MOD == EXPLORER ? OPEN : MOD == PROCESS ? KILL : RUN);
                if (MOD == HISTORY) { preview.addCommand(EDIT); } 
                else if (MOD == PROCESS) { preview.addCommand(LOAD); preview.addCommand(VIEW); }
    
                preview.setCommandListener(this); 
                load(); display.setCurrent(preview);
            }
        }
        public MIDletControl(String command) {
            MOD = command == null || command.length() == 0 || command.equals("login") ? SIGNUP : REQUEST;

            if (MOD == SIGNUP) { 
                monitor.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\n" + (asking_user && asking_passwd ? "Create your credentials!" : asking_user ? "Create an user to access OpenTTY!" : asking_passwd ? "Create a password!" : "")).trim()); 

                if (asking_user) { asking_user = true; monitor.append(USER); } 
                if (asking_passwd) { monitor.append(PASSWD); } 

                monitor.addCommand(LOGIN); monitor.addCommand(EXIT); 
            } else { 
                if (asking_passwd) { new MIDletControl(null); return; } 
                this.command = command;

                PASSWD.setLabel("[sudo] password for " + loadRMS("OpenRMS")); 
                BACK = new Command("Back", Command.SCREEN, 2);
                monitor.append(PASSWD); 
                monitor.addCommand(RUN); monitor.addCommand(BACK); 
            } 

            
            monitor.setCommandListener(this); 
            display.setCurrent(monitor); 
        }
        
        public void commandAction(Command c, Displayable d) {
            if (c == BACK) { processCommand("xterm"); return; } 
    
            if (MOD == HISTORY) { String selected = preview.getString(preview.getSelectedIndex()); if (selected != null) { processCommand("xterm"); processCommand(c == RUN || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU")) ? selected : "buff " + selected); } } 
            else if (MOD == EXPLORER) {
                String selected = preview.getString(preview.getSelectedIndex()); 

                if (c == OPEN || (c == List.SELECT_COMMAND && !attributes.containsKey("J2EMU"))) { 
                    if (selected != null) { 
                        processCommand(selected.endsWith("..") ? "cd .." : selected.endsWith("/") ? "cd " + path + selected : "nano " + path + selected, false);

                        if (display.getCurrent() == preview) { reload(); }

                        stdin.setLabel(username + " " + path + " $"); 
                    } 
                } 
                else if (c == DELETE) { 
                    int STATUS = deleteFile(path + selected); 
                    if (STATUS != 0) { warnCommand(form.getTitle(), STATUS == 13 ? "Permission denied!" : "java.io.IOException"); } 
    
                    reload(); 
                } 
                else if (c == RUNS) { processCommand("xterm"); runScript(getcontent(path + selected), root); } 
                else if (c == IMPORT) { processCommand("xterm"); importScript(path + selected, root); } 
            } 
            else if (MOD == MONITOR) { System.gc(); reload(); } 
            else if (MOD == PROCESS) {
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
                    else if (username.equals("root")) { warnCommand(form.getTitle(), "Invalid username!"); USER.setString(""); }
                    else {
                        if (asking_user) { writeRMS("/home/OpenRMS", username); }
                        if (asking_passwd) { writeRMS("OpenRMS", String.valueOf(password.hashCode()).getBytes(), 2); }

                        display.setCurrent(form); runScript(loadRMS("initd")); stdin.setLabel(username + " " + path + " " + (username.equals("root") ? "#" : "$")); 
                    }
                }
                else if (c == EXIT) { processCommand("exit", false); }
            }
            else if (MOD == REQUEST) {
                String password = PASSWD.getString().trim();
                
                if (password.equals("")) { }
                else if (String.valueOf(password.hashCode()).equals(passwd())) { processCommand("xterm"); processCommand(command, true, true); } 
                else { warnCommand(form.getTitle(), "Wrong password"); }
            }
        } 
        private void reload() { if (attributes.containsKey("J2EMU")) { new MIDletControl(MOD == MONITOR ? "monitor" : MOD == PROCESS ? "process" : MOD == EXPLORER ? "dir" : "history", root); } else { load(); } }
        private void load() {
            if (MOD == HISTORY) { preview.deleteAll(); for (int i = 0; i < history.size(); i++) { preview.append((String) history.elementAt(i), null); } } 
            else if (MOD == EXPLORER) {
                if (path.startsWith("/home/") || (path.startsWith("/mnt/") && !path.equals("/mnt/"))) { preview.addCommand(DELETE); }
                else { preview.removeCommand(DELETE); }
    
                if (path.equals("/") || path.equals("/mnt/")) { preview.removeCommand(RUNS); preview.removeCommand(IMPORT); }
                else { preview.addCommand(RUNS); preview.addCommand(IMPORT); }
    
                if (attributes.containsKey("J2EMU")) { }
                else { preview.setTitle(path); }
    
                preview.deleteAll();
                if (path.equals("/")) { }
                else { preview.append("..", null); }
    
                try {
                    if (path.equals("/mnt/")) {
                        for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { preview.append((String) roots.nextElement(), null); }
                    } else if (path.startsWith("/mnt/")) {
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
                    } else if (path.startsWith("/home/")) {
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
            else if (MOD == MONITOR) { status.setText("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\n" + "Free Memory: " + runtime.freeMemory() / 1024 + " KB\n" + "Total Memory: " + runtime.totalMemory() / 1024 + " KB"); } 
            else if (MOD == PROCESS) { preview.deleteAll(); for (Enumeration keys = trace.keys(); keys.hasMoreElements();) { String PID = (String) keys.nextElement(); preview.append(PID + "\t" + (String) ((Hashtable) trace.get(PID)).get("name"), null); } }
        }

        public static String passwd() { try { RecordStore RMS = RecordStore.openRecordStore("OpenRMS", true); if (RMS.getNumRecords() >= 2) { byte[] data = RMS.getRecord(2); if (data != null) { return new String(data); } } if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return ""; } 
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
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { return processCommand(argument.equals("") ? "import /java/bin/sh" : ". " + argument, false, root); }
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
        else if (mainCommand.equals("htop")) { new Screen("list", "list.content=Monitor,Process\nlist.button=Open\nMonitor=execute top;\nProcess=execute top process;", root); }
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
        else if (mainCommand.equals("stty")) { if (argument.equals("")) { echoCommand("" + MAX_STDOUT_LEN); } else { MAX_STDOUT_LEN = getNumber(argument, MAX_STDOUT_LEN, true); } }
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
        else if (mainCommand.equals("mmspt")) { echoCommand(replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet")); }
        else if (mainCommand.equals("bg")) { final String CMD = argument; final boolean IGNORE = ignore, ROOT = root; new Thread("Background") { public void run() { processCommand(CMD, IGNORE, ROOT); } }.start(); }

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
        else if (mainCommand.equals("nc") || mainCommand.equals("prscan") || mainCommand.equals("gobuster") || mainCommand.equals("bind") || mainCommand.equals("server")) { new Connect(mainCommand, argument, root); }
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
                else { echoCommand(mainCommand + ": " + basename(TARGET) + ": not accessible"); return 127; } 

            } 

            if (mainCommand.equals("pushd")) { ((Vector) getobject("1", "stack")).addElement(path); echoCommand(readStack()); }
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
                if (PWD.equals("/mnt/")) { 
                    for (Enumeration ROOTS = FileSystemRegistry.listRoots(); ROOTS.hasMoreElements();) { 
                        String ROOT = (String) ROOTS.nextElement(); 
                        if ((all || !ROOT.startsWith(".")) && !BUFFER.contains(ROOT)) { 
                            BUFFER.addElement(ROOT); 
                        } 
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
        else if (mainCommand.equals("mkdir")) { if (argument.equals("")) { } else { argument = argument.endsWith("/") ? argument : argument + "/"; argument = argument.startsWith("/") ? argument : path + argument; if (argument.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.mkdir(); CONN.close(); } else { echoCommand("mkdir: " + basename(argument) + ": found"); } CONN.close(); } catch (Exception e) { echoCommand(getCatch(e)); return (e instanceof SecurityException) ? 13 : 1; } } else if (argument.startsWith("/home/")) { echoCommand("Unsupported API"); return 3; } else if (argument.startsWith("/")) { echoCommand("read-only storage"); return 5; } } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { return writeRMS(args[1].equals("") ? args[0] + "-copy" : args[1], getcontent(args[0])); } }
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
        else if (mainCommand.equals("vnt")) { if (argument.equals("")) { } else { String IN = getcontent(args[0]), OUT = args.length > 1 ? args[1] : ""; if (OUT.equals("")) { nanoContent = text2note(IN); } else { writeRMS(OUT, text2note(IN)); } } }
        else if (mainCommand.equals("ph2s")) { Vector history = (Vector) getobject("1", "history"); StringBuffer BUFFER = new StringBuffer(); for (int i = 0; i < history.size() - 1; i++) { BUFFER.append(history.elementAt(i)); if (i < history.size() - 1) { BUFFER.append("\n"); } } String script = "#!/java/bin/sh\n\n" + BUFFER.toString(); if (argument.equals("") || argument.equals("nano")) { nanoContent = script; } else { writeRMS(argument, script); } }
        // |
        // Interfaces
        else if (mainCommand.equals("nano")) { nano.setString(argument.equals("") ? nanoContent : getcontent(argument)); display.setCurrent(nano); }
        else if (mainCommand.equals("html")) { viewer(extractTitle(env(nanoContent)), html2text(env(nanoContent))); }
        else if (mainCommand.equals("view")) { if (argument.equals("")) { } else { viewer(extractTitle(env(argument)), html2text(env(argument))); } }
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
        else if (mainCommand.equals("debug")) { return runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("help")) { viewer(form.getTitle(), read("/java/etc/help.txt")); }
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
        
        else if (mainCommand.equals("lua")) { Lua lua = new Lua(this, root); return (Integer) lua.run(argument.equals("") ? "" : args[0].equals("-e") ? "stdin" : argument, argument.equals("") ? nanoContent : args[0].equals("-e") ? argument.substring(3).trim() : getcontent(argument)).get("status"); }
        else if (mainCommand.equals("5k")) { echoCommand("1.16 Special - 5k commits at OpenTTY GitHub repository"); }
        
        // API 015 - (Scripts)
        // |
        // OpenTTY Packages
        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { return importScript(argument, root); }
        else if (mainCommand.equals("function")) { if (argument.equals("")) { } else { int braceIndex = argument.indexOf('{'), braceEnd = argument.lastIndexOf('}'); if (braceIndex != -1 && braceEnd != -1 && braceEnd > braceIndex) { String name = getCommand(argument).trim(); String body = replace(argument.substring(braceIndex + 1, braceEnd).trim(), ";", "\n"); functions.put(name, body); } else { echoCommand("invalid syntax"); return 2; } } }

        else if (mainCommand.equals("eval")) { if (argument.equals("")) { } else { echoCommand("" + processCommand(argument, ignore, root)); } }
        else if (mainCommand.equals("catch")) { if (argument.equals("")) { } else { try { processCommand(argument, ignore, root); } catch (Exception e) { echoCommand(getCatch(e)); } } }
        else if (mainCommand.equals("return")) { return getNumber(argument, 2, true); }

        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE LTS")); }
        else if (mainCommand.equals("!!")) { stdin.setString((argument.equals("") ? "" : argument + " ") + getLastHistory()); }
        else if (mainCommand.equals("run") || mainCommand.equals(".")) { return runScript(argument.equals("") ? nanoContent : getcontent(argument), root); }

        else { echoCommand(mainCommand + ": not found"); return 127; }

        return 0;
    }
    // |
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    // |
    private String read(String filename) { try { if (filename.startsWith("/mnt/")) { FileConnection fileConn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ); InputStream is = fileConn.openInputStream(); StringBuffer content = new StringBuffer(); int ch; while ((ch = is.read()) != -1) { content.append((char) ch); } is.close(); fileConn.close(); return env(content.toString()); } else if (filename.startsWith("/home/")) { RecordStore recordStore = null; String content = ""; try { recordStore = RecordStore.openRecordStore(filename.substring(6), true); if (recordStore.getNumRecords() >= 1) { byte[] data = recordStore.getRecord(1); if (data != null) { content = new String(data); } } } catch (RecordStoreException e) { content = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return content; } else { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); if (is == null) { return ""; } InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } } catch (IOException e) { return ""; } }
    public String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); Enumeration e = attributes.keys(); while (e.hasMoreElements()) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } text = replace(text, "$.", "$"); text = replace(text, "\\.", "\\"); return text; }
    private String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(result) < 0) { result = cur; } } v.removeElement(result); return result; } 
    public String getCatch(Exception e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : message; }
    // |
    public String getcontent(String file) { return file.startsWith("/") ? read(file) : file.equals("nano") ? nanoContent : read(path + file); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }
    // |
    private String join(String[] array, String spacer, int start) { if (array == null || array.length == 0 || start >= array.length) { return ""; } StringBuffer sb = new StringBuffer(); for (int i = start; i < array.length; i++) { sb.append(array[i]).append(spacer); } return sb.toString().trim(); }
    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private String[] splitArgs(String content) { Vector args = new Vector(); boolean inQuotes = false; int start = 0; for (int i = 0; i < content.length(); i++) { char c = content.charAt(i); if (c == '"') { inQuotes = !inQuotes; continue; } if (!inQuotes && c == ' ') { if (i > start) { args.addElement(getpattern(content.substring(start, i))); } start = i + 1; } } if (start < content.length()) { args.addElement(getpattern(content.substring(start))); } String[] result = new String[args.size()]; args.copyInto(result); return result; }
    // |
    public Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }
    public int getNumber(String s, int fallback, boolean print) { try { return Integer.valueOf(s); } catch (Exception e) { if (print) {echoCommand(getCatch(e)); } return fallback; } }
    public Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    // |
    private int indexOf(String key, String[] array) { for (int i = 0; i < array.length; i++) { if (array[i].equals(key)) { return i; } } return -1; }
    private int getCatch(Exception e, int fallback) { return (e instanceof SecurityException) ? 13 : fallback; }

    // API 002 - (Logs)
    // |
    // OpenTTY Logging Manager
    private int MIDletLogs(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { writeRMS(argument.equals("") ? "logs" : argument, logs); } else if (mainCommand.equals("view")) { viewer(form.getTitle(), logs); } else if (mainCommand.equals("add")) { String LEVEL = getCommand(argument).toLowerCase(), MESSAGE = getArgument(argument); if (!MESSAGE.equals("")) { if (LEVEL.equals("info") || LEVEL.equals("warn") || LEVEL.equals("debug") || LEVEL.equals("error")) { logs += "[" + LEVEL.toUpperCase() + "] " + split(new java.util.Date().toString(), ' ')[3] + " " + MESSAGE + "\n"; } else { echoCommand("log: add: " + LEVEL + ": not found"); return 127; } } } else { echoCommand("log: " + mainCommand + ": not found"); return 127; } return 0; }

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
        else if (mainCommand.equals("canvas")) { display.setCurrent(new MyCanvas(argument.equals("") ? "Canvas" : argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), root)); }
        else if (mainCommand.equals("make") || mainCommand.equals("list") || mainCommand.equals("quest") || mainCommand.equals("edit")) { new Screen(mainCommand, argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), root); }
        else if (mainCommand.equals("item")) { new ItemLoader(form, "item", argument.equals("clear") ? "clear" : argument.startsWith("-e") ? argument.substring(2).trim() : getcontent(argument), root); }

        else { echoCommand("x11: " + mainCommand + ": not found"); return 127; }

        return 0;
    }
    private int warnCommand(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    private int viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(env(text)); viewer.addCommand(BACK); viewer.setCommandListener(this); display.setCurrent(viewer); return 0; }
    // |
    // Interfaces
    public class ItemLoader implements ItemCommandListener {
        private Hashtable PKG; 
        private boolean root = false;
        private Command RUN; 
        private StringItem s; 
        private String node; 

        public ItemLoader(Form screen, String node, String code, boolean root) {
            if (code == null || code.length() == 0) { return; } 
            else if (code.equals("clear")) { form.deleteAll(); form.append(stdout); form.append(stdin); return; } 

            this.PKG = parseProperties(code); this.root = root; this.node = node; 

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
        public void commandAction(Command c, Item item) { if (c == RUN) { processCommand("xterm", true, root); processCommand((String) PKG.get(node + ".cmd"), true, root); } } 

        private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; } 
        private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); } 
        private String getenv(String key) { return env(getvalue(key, "")); } 
    }
    public class Screen implements CommandListener { 
        private Hashtable PKG; 
        private boolean root = false;
        private int TYPE = 0, SCREEN = 1, LIST = 2, QUEST = 3, EDIT = 4; 
        private Form screen = new Form(form.getTitle()); 
        private List list = new List(form.getTitle(), List.IMPLICIT); 
        private TextBox edit = new TextBox(form.getTitle(), "", 31522, TextField.ANY); 
        private Command BACK, USER; 
        private TextField INPUT;

        public Screen(String type, String code, boolean root) { 
            if (type == null || type.length() == 0 || code == null || code.length() == 0) { return; } 

            this.PKG = parseProperties(code); 
            this.root = root;
            if (type.equals("make")) { 
                TYPE = SCREEN; 

                if (PKG.containsKey("screen.title")) { screen.setTitle(getenv("screen.title")); } 
                BACK = new Command(getenv("screen.back.label", "Back"), Command.OK, 1); 
                USER = new Command(getenv("screen.button", "Menu"), Command.SCREEN, 2); 
                screen.addCommand(BACK); 
                if (PKG.containsKey("screen.button")) { screen.addCommand(USER); } 
                if (PKG.containsKey("screen.fields")) { 
                    String[] fields = split(getenv("screen.fields"), ','); 

                    for (int i = 0; i < fields.length; i++) { 
                        String field = fields[i].trim(); 
                        String type = getenv("screen." + field + ".type"); 

                        if (type.equals("image") && !getenv("screen." + field + ".img").equals("")) { 
                            try { screen.append(new ImageItem(null, Image.createImage(getenv("screen." + field + ".img")), ImageItem.LAYOUT_CENTER, null)); } 
                            catch (Exception e) { MIDletLogs("add warn Malformed Image '" + getenv("screen." + field + ".img") + "'"); } 
                        } 
                        else if (type.equals("text") && !getenv("screen." + field + ".value").equals("")) { 
                            StringItem content = new StringItem(getenv("screen." + field + ".label"), getenv("screen." + field + ".value")); 

                            content.setFont(newFont(getenv("screen." + field + ".style", "default"))); 
                            screen.append(content); 
                        }
                        else if (type.equals("item")) { new ItemLoader(screen, "screen." + field, code, root); } 
                        else if (type.equals("spacer")) { 
                            int width = Integer.parseInt(getenv("screen." + field + ".w", "1")), height = Integer.parseInt(getenv("screen." + field + ".h", "10")); 
                            screen.append(new Spacer(width, height)); 
                        }
                    } 
                } 

                screen.setCommandListener(this); display.setCurrent(screen); 
            } 
            else if (type.equals("list")) { 
                TYPE = LIST; 
                Image IMG = null; 

                if (!PKG.containsKey("list.content")) { MIDletLogs("add error List crashed while init, malformed settings"); return; } 

                if (PKG.containsKey("list.title")) { list.setTitle(getenv("list.title")); } 
                if (PKG.containsKey("list.icon")) { 
                    try { IMG = Image.createImage(getenv("list.icon")); } 
                    catch (Exception e) { MIDletLogs("add warn Malformed Image '" + getenv("list.icon") + "'"); } 
                } 

                BACK = new Command(getenv("list.back.label", "Back"), Command.OK, 1); 
                USER = new Command(getenv("list.button", "Select"), Command.SCREEN, 2); 
                list.addCommand(BACK); list.addCommand(USER); 
                
                String[] content = split(getenv("list.content"), ','); 
                for (int i = 0; i < content.length; i++) { list.append(content[i], IMG); } 

                if (PKG.containsKey("list.source")) {
                    String source = getcontent(getenv("list.source"));
                    
                    if (source.equals("")) { } 
                    else {
                        String[] content = split(source, '\n'); 
                        for (int i = 0; i < content.length; i++) {
                            String key = content[i], value = "true";
                            
                            int index = content[i].indexOf("=");
                            if (index == -1) { }
                            else {
                                value = key.substring(index + 1);
                                key = key.substring(0, index);
                            }
                            
                            list.append(key, IMG); 
                            PKG.put(key, value);
                        } 

                    }
                }

                list.setCommandListener(this); display.setCurrent(list); 
            } 
            else if (type.equals("quest")) { 
                TYPE = QUEST; 
                
                if (!PKG.containsKey("quest.label") || !PKG.containsKey("quest.cmd") || !PKG.containsKey("quest.key")) { MIDletLogs("add error Quest crashed while init, malformed settings"); return; } 
                if (PKG.containsKey("quest.title")) { screen.setTitle(getenv("quest.title")); } 

                INPUT = new TextField(getenv("quest.label"), getenv("quest.content"), 256, getQuest(getenv("quest.type"))); 
                BACK = new Command(getvalue("quest.back.label", "Cancel"), Command.SCREEN, 2); 
                USER = new Command(getvalue("quest.cmd.label", "Send"), Command.OK, 1); 
                screen.append(INPUT); screen.addCommand(BACK); screen.addCommand(USER); 

                screen.setCommandListener(this); display.setCurrent(screen); 
            } 
            else if (type.equals("edit")) {
                TYPE = EDIT;

                if (!PKG.containsKey("edit.cmd") || !PKG.containsKey("edit.key")) { MIDletLogs("add error Editor crashed while init, malformed settings"); return; } 
                if (PKG.containsKey("edit.title")) { edit.setTitle(getenv("edit.title")); }
                edit.setString(PKG.containsKey("edit.content") ? getenv("edit.content") : PKG.containsKey("edit.source") ? getcontent(getenv("edit.source")) : "");

                BACK = new Command(getenv("edit.back.label", "Back"), Command.OK, 1);
                USER = new Command(getenv("edit.cmd.label", "Run"), Command.SCREEN, 2);
                edit.addCommand(BACK); edit.addCommand(USER);

                edit.setCommandListener(this); display.setCurrent(edit);

            }
            else { return; } 
        }  
        public void commandAction(Command c, Displayable d) { 
            if (c == BACK) { 
                processCommand("xterm", true, root); 
                processCommand(getvalue((TYPE == SCREEN ? "screen" : TYPE == LIST ? "list" : TYPE == QUEST ? "quest" : "edit") + ".back", "true"), true, root);
            } 
            else if (c == USER || c == List.SELECT_COMMAND) { 
                if (TYPE == QUEST) { 
                    String value = INPUT.getString().trim(); 
                    if (value.equals("")) { } 
                    else { 
                        attributes.put(getenv("quest.key"), env(value)); 
                        processCommand("xterm", true, root); 
                        processCommand(getvalue("quest.cmd", "true"), true, root); 
                    } 
                } 
                else if (TYPE == EDIT) { 
                    String value = edit.getString().trim(); 
                    if (value.equals("")) { }
                    else { 
                        attributes.put(getenv("edit.key"), env(value)); 
                        processCommand("xterm", true, root); 
                        processCommand(getvalue("edit.cmd", "true"), true, root); 
                    } 
                } 
                else if (TYPE == LIST) { int index = list.getSelectedIndex(); if (index >= 0) { processCommand("xterm", true, root); String key = env(list.getString(index)); processCommand(getvalue(key, "log add warn An error occurred, '" + key + "' not found"), true, root); } } 
                else if (TYPE == SCREEN) { processCommand("xterm", true, root); processCommand(getvalue("screen.button.cmd", "log add warn An error occurred, 'screen.button.cmd' not found"), true, root); } 
            } 
        } 
        private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; } 
        private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); } 
        private String getenv(String key) { return env(getvalue(key, "")); } 

        private int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = replace(mode, "password", "").trim(); } int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY; return password ? (base | TextField.PASSWORD) : base; } 
    }
    public class MyCanvas extends Canvas implements CommandListener { 
        private Hashtable PKG; 
        private Graphics screen; 
        private Command BACK, USER; 
        private Image CURSOR = null; 
        private Vector fields = new Vector(); 
        private boolean root = false;
        private final int cursorSize = 5; 

        public MyCanvas(String code, boolean root) { 
            if (code == null || code.length() == 0) { return; } 

            this.PKG = parseProperties(code);  this.root = root;
            setTitle(getenv("canvas.title", form.getTitle())); 

            BACK = new Command(getenv("canvas.back.label", "Back"), Command.OK, 1); 
            USER = new Command(getenv("canvas.button"), Command.SCREEN, 2); 

            addCommand(BACK); 
            if (PKG.containsKey("canvas.button")) { addCommand(USER); } 
            if (PKG.containsKey("canvas.mouse")) { 
                try { 
                    String[] pos = split(getenv("canvas.mouse"), ','); 

                    cursorX = Integer.parseInt(pos[0]); 
                    cursorY = Integer.parseInt(pos[1]); 
                } 
                catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas.mouse' - (x,y) may be a int number"); cursorX = 10; cursorY = 10; } 
            } 
            if (PKG.containsKey("canvas.mouse.img")) { 
                try { CURSOR = Image.createImage(getenv("canvas.mouse.img")); } 
                catch (Exception e) { MIDletLogs("add warn Malformed Cursor '" + getenv("canvas.mouse.img") + "'"); } 
            } 
            if (PKG.containsKey("canvas.fields")) { 
                String[] names = split(getenv("canvas.fields"), ','); 

                for (int i = 0; i < names.length; i++) { 
                    String id = names[i].trim(), type = getenv("canvas." + id + ".type", "text"); 
                    int x = Integer.parseInt(getenv("canvas." + id + ".x", "0")), y = Integer.parseInt(getenv("canvas." + id + ".y", "0")), w = Integer.parseInt(getenv("canvas." + id + ".w", "0")), h = Integer.parseInt(getenv("canvas." + id + ".h", "0")); 

                    Hashtable field = new Hashtable(); 
                    field.put("type", type); field.put("x", new Integer(x)); field.put("y", new Integer(y)); field.put("w", new Integer(w)); field.put("h", new Integer(h)); 
                    field.put("value", getenv("canvas." + id + ".value", "")); 
                    field.put("style", getenv("canvas." + id + ".style", "default")); 
                    field.put("cmd", getenv("canvas." + id + ".cmd", "")); 

                    fields.addElement(field); 
                } 
            } 

            setCommandListener(this); 
        } 

        protected void paint(Graphics g) { 
            if (screen == null) { screen = g; } 

            g.setColor(0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); 

            if (PKG.containsKey("canvas.background")) { 
                String backgroundType = getenv("canvas.background.type", "default"); 

                if (backgroundType.equals("color") || backgroundType.equals("default")) { setpallete("background", g, 0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); } 
                else if (backgroundType.equals("image")) { 
                    try { 
                        Image content = Image.createImage(getenv("canvas.background")); 

                        g.drawImage(content, (getWidth() - content.getWidth()) / 2, (getHeight() - content.getHeight()) / 2, Graphics.TOP | Graphics.LEFT); 
                    } 
                    catch (Exception e) { processCommand("xterm"); processCommand("execute log add error Malformed Image, " + getCatch(e)); } 
                } 
            } 
            if (PKG.containsKey("canvas.fields")) { 
                for (int i = 0; i < fields.size(); i++) { 
                    Hashtable f = (Hashtable) fields.elementAt(i); 

                    String type = (String) f.get("type"), val = (String) f.get("value"); 
                    int x = ((Integer) f.get("x")).intValue(), y = ((Integer) f.get("y")).intValue(), w = ((Integer) f.get("w")).intValue(), h = ((Integer) f.get("h")).intValue(); 

                    if (type.equals("text")) { 
                        setpallete("text.color", g, 255, 255, 255); 
                        g.setFont(newFont((String) f.get("style"))); 

                        g.drawString(val, x, y, Graphics.TOP | Graphics.LEFT); 
                    } 
                    else if (type.equals("image")) { 
                        try { 
                            Image IMG = Image.createImage(val); 

                            g.drawImage(IMG, x, y, Graphics.TOP | Graphics.LEFT); 

                            if (w == 0) { f.put("w", new Integer(IMG.getWidth())); } 
                            if (h == 0) { f.put("h", new Integer(IMG.getHeight())); } 
                        } 
                        catch (Exception e) { MIDletLogs("add error Malformed Image, " + getCatch(e)); } 
                    } 
                    else if (type.equals("rect")) { setpallete("rect.color", g, 0, 0, 255); g.drawRect(x, y, w, h); } 
                    else if (type.equals("circle")) { setpallete("circle.color", g, 0, 255, 0); g.drawArc(x - w, y - w, w * 2, w * 2, 0, 360); } 
                    else if (type.equals("line")) { setpallete("line.color", g, 255, 255, 255); g.drawLine(x, y, w, h); } 
                } 
            } 

            if (CURSOR != null) { g.drawImage(CURSOR, cursorX, cursorY, Graphics.TOP | Graphics.LEFT); } 
            else { setpallete("mouse.color", g, 255, 255, 255); g.fillRect(cursorX, cursorY, cursorSize, cursorSize); } 
        } 
        protected void keyPressed(int keyCode) { 
            int gameAction = getGameAction(keyCode); 

            if (gameAction == LEFT) { cursorX = Math.max(0, cursorX - 5); } 
            else if (gameAction == RIGHT) { cursorX = Math.min(getWidth() - cursorSize, cursorX + 5); } 
            else if (gameAction == UP) { cursorY = Math.max(0, cursorY - 5); } 
            else if (gameAction == DOWN) { cursorY = Math.min(getHeight() - cursorSize, cursorY + 5); } 
            else if (gameAction == FIRE) { 
                for (int i = 0; i < fields.size(); i++) { 
                    Hashtable f = (Hashtable) fields.elementAt(i); 

                    int x = ((Integer) f.get("x")).intValue(), y = ((Integer) f.get("y")).intValue(), w = ((Integer) f.get("w")).intValue(), h = ((Integer) f.get("h")).intValue(); 
                    String type = (String) f.get("type"), cmd = (String) f.get("cmd"), val = (String) f.get("value"); 

                    if (cmd != null && !cmd.equals("")) { 
                        boolean hit = false; 

                        if (type.equals("circle")) { int dx = cursorX - x, dy = cursorY - y; hit = (dx * dx + dy * dy) <= (w * w); } 
                        else if (type.equals("text")) { 
                            Font font = newFont(getenv((String) f.get("style"), "default")); 

                            int textW = font.stringWidth(val), textH = font.getHeight(); 
                            hit = cursorX + cursorSize > x && cursorX < x + textW && cursorY + cursorSize > y && cursorY < y + textH; 
                        } 
                        else if (type.equals("line")) { continue; } 
                        else { hit = cursorX + cursorSize > x && cursorX < x + w && cursorY + cursorSize > y && cursorY < y + h; } 

                        if (hit) { processCommand(cmd, true, root); break; } 
                    } 
                } 
            } 

            repaint(); 
        } 

        protected void pointerPressed(int x, int y) { cursorX = x; cursorY = y; keyPressed(-5); } 

        public void commandAction(Command c, Displayable d) { 
            if (c == BACK) { processCommand("xterm", true, root); processCommand(getvalue("canvas.back", "true"), true, root); } 
            else if (c == USER) { processCommand("xterm", true, root); processCommand(getvalue("canvas.button.cmd", "log add warn An error occurred, 'canvas.button.cmd' not found"), true, root); } 
        } 
        private void setpallete(String node, Graphics screen, int r, int g, int b) { 
            try { 
                String[] pallete = split(getenv("canvas." + node, "" + r + "," + g + "," + b), ','); 
                screen.setColor(Integer.parseInt(pallete[0]), Integer.parseInt(pallete[1]), Integer.parseInt(pallete[2])); 
            } 
            catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas." + node + "' - (r,g,b) may be a int number"); } 
        } 

        private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; } 
        private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); } 
        private String getenv(String key) { return env(getvalue(key, "")); } 
    }
    // |
    // Font Generator
    private Font newFont(String argument) { if (argument == null || argument.length() == 0 || argument.equals("default")) { return Font.getDefaultFont(); } int style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; if (argument.equals("bold")) { style = Font.STYLE_BOLD; } else if (argument.equals("italic")) { style = Font.STYLE_ITALIC; } else if (argument.equals("ul")) { style = Font.STYLE_UNDERLINED; } else if (argument.equals("small")) { size = Font.SIZE_SMALL; } else if (argument.equals("large")) { size = Font.SIZE_LARGE; } else { return newFont("default"); } return Font.getFont(Font.FACE_SYSTEM, style, size); }

    // API 005 - (Operators)
    // |
    // Operators
    private int ifCommand(String argument, boolean ignore, boolean root) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("if (expr) [command]"); return 2; } String EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); String[] PARTS = split(EXPR, ' '); if (PARTS.length == 3) { boolean CONDITION = false; boolean NEGATED = PARTS[1].startsWith("!") && !PARTS[1].equals("!="); if (NEGATED) { PARTS[1] = PARTS[1].substring(1); } Double N1 = getNumber(PARTS[0]), N2 = getNumber(PARTS[2]); if (N1 != null && N2 != null) { if (PARTS[1].equals("==")) { CONDITION = N1.doubleValue() == N2.doubleValue(); } else if (PARTS[1].equals("!=")) { CONDITION = N1.doubleValue() != N2.doubleValue(); } else if (PARTS[1].equals(">")) { CONDITION = N1.doubleValue() > N2.doubleValue(); } else if (PARTS[1].equals("<")) { CONDITION = N1.doubleValue() < N2.doubleValue(); } else if (PARTS[1].equals(">=")) { CONDITION = N1.doubleValue() >= N2.doubleValue(); } else if (PARTS[1].equals("<=")) { CONDITION = N1.doubleValue() <= N2.doubleValue(); } } else { if (PARTS[1].equals("startswith")) { CONDITION = PARTS[0].startsWith(PARTS[2]); } else if (PARTS[1].equals("endswith")) { CONDITION = PARTS[0].endsWith(PARTS[2]); } else if (PARTS[1].equals("contains")) { CONDITION = PARTS[0].indexOf(PARTS[2]) != -1; } else if (PARTS[1].equals("==")) { CONDITION = PARTS[0].equals(PARTS[2]); } else if (PARTS[1].equals("!=")) { CONDITION = !PARTS[0].equals(PARTS[2]); } } if (CONDITION != NEGATED) { return processCommand(CMD, ignore, root); } } else if (PARTS.length == 2) { if (PARTS[0].equals(PARTS[1])) { return processCommand(CMD, ignore, root); } } else if (PARTS.length == 1) { if (!PARTS[0].equals("")) { return processCommand(CMD, ignore, root); } } return 0; }
    private int forCommand(String argument, boolean ignore, boolean root) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String KEY = getCommand(argument), FILE = getcontent(argument.substring(firstParenthesis + 1, lastParenthesis).trim()), CMD = argument.substring(lastParenthesis + 1).trim(); if (KEY.startsWith("(")) { return 2; } if (KEY.startsWith("$")) { KEY = replace(KEY, "$", ""); } String[] LINES = split(FILE, '\n'); for (int i = 0; i < LINES.length; i++) { if (LINES[i] != null || LINES[i].length() != 0) { processCommand("set " + KEY + "=" + LINES[i], false, root); int STATUS = processCommand(CMD, ignore, root); processCommand("unset " + KEY, false, root); if (STATUS != 0) { return STATUS; } } } return 0; }
    private int caseCommand(String argument, boolean ignore, boolean root) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String METHOD = getCommand(argument), EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); boolean CONDITION = false, NEGATED = METHOD.startsWith("!"); if (NEGATED) { METHOD = METHOD.substring(1); } if (METHOD.equals("file")) { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(EXPR)) { CONDITION = true; break; } } } } else if (METHOD.equals("root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(EXPR)) { CONDITION = true; break; } } } else if (METHOD.equals("thread")) { CONDITION = replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet").equals(EXPR); } else if (METHOD.equals("screen")) { CONDITION = ((Hashtable) getobject("2", "saves")).containsKey(EXPR); } else if (METHOD.equals("key")) { CONDITION = attributes.containsKey(EXPR); } else if (METHOD.equals("alias")) { CONDITION = aliases.containsKey(EXPR); } else if (METHOD.equals("trace")) { CONDITION = getpid(EXPR) != null ? true : false; } else if (METHOD.equals("passwd")) { CONDITION = String.valueOf(EXPR.hashCode()).equals(MIDletControl.passwd()); } else if (METHOD.equals("user")) { CONDITION = username.equals(EXPR); if (EXPR.equals("root") && root == true) { CONDITION = true; } root = true; } if (CONDITION != NEGATED) { return processCommand(CMD, ignore, root); } return 0; }
    
    // API 006 - (Process)
    // |
    // Process
    public int kill(String pid, boolean print, boolean root) {
        if (pid == null || pid.length() == 0) { return 2; }

        Hashtable proc = (Hashtable) trace.get(pid);
        if (proc == null) { if (print) { echoCommand("PID '" + pid + "' not found"); } return 127; }

        String owner = (String) proc.get("owner"), collector = (String) proc.get("collector");

        if (owner.equals("root") && !root) { if (print) { echoCommand("Permission denied!"); } return 13; }
        if (collector != null && !collector.equals("")) { processCommand(collector, true, root); }

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
    private Object getobject(String pid, String item) { return (Object) getprocess(pid).get(item); }
    private String getpid(String name) { for (Enumeration KEYS = trace.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); if (name.equals((String) ((Hashtable) trace.get(PID)).get("name"))) { return PID; } } return null; } 
    private String getowner(String pid) { return trace.containsKey(pid) ? (String) ((Hashtable) trace.get(pid)).get("owner") : null; }
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
    private void echoCommand(String message, StringItem console) { if (message == null) { return; } String current = console.getText(), output = current == null || current.length() == 0 ? message : current + "\n" + message; console.setText(MAX_STDOUT_LEN >= 0 && output.length() > MAX_STDOUT_LEN ? output.substring(output.length() - MAX_STDOUT_LEN) : output); }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String exprCommand(String expr) { char[] tokens = expr.toCharArray(); double[] vals = new double[32]; char[] ops = new char[32]; int valTop = -1, opTop = -1; int i = 0, len = tokens.length; while (i < len) { char c = tokens[i]; if (c == ' ') { i++; continue; } if (c >= '0' && c <= '9') { double num = 0; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { num = num * 10 + (tokens[i++] - '0'); } if (i < len && tokens[i] == '.') { i++; double frac = 0, div = 10; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { frac += (tokens[i++] - '0') / div; div *= 10; } num += frac; } vals[++valTop] = num; } else if (c == '(') { ops[++opTop] = c; i++; } else if (c == ')') { while (opTop >= 0 && ops[opTop] != '(') { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } opTop--; i++; } else if (c == '+' || c == '-' || c == '*' || c == '/') { while (opTop >= 0 && prec(ops[opTop]) >= prec(c)) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } ops[++opTop] = c; i++; } else { return "expr: invalid char '" + c + "'"; } } while (opTop >= 0) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } double result = vals[valTop]; return ((int) result == result) ? String.valueOf((int) result) : String.valueOf(result); } private int prec(char op) { if (op == '+' || op == '-') return 1; if (op == '*' || op == '/') return 2; return 0; } private double applyOpSimple(char op, double a, double b) { if (op == '+') return a + b; if (op == '-') return a - b; if (op == '*') return a * b; if (op == '/') return b == 0 ? 0 : a / b; return 0; }
    private String generateUUID() { String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + random.nextInt(4))); } else { uuid.append(chars.charAt(random.nextInt(16))); } } return uuid.toString(); }

    // API 011 - (Network)
    // |
    // Connector
    public class Connect implements CommandListener, Runnable {
        private static final int NC = 1, PRSCAN = 2, GOBUSTER = 3, SERVER = 4, BIND = 5, DYNAMICS = 6;

        private int MOD, COUNT = 1;
        private boolean root = false, asked = false, keep = false;

        private SocketConnection CONN;
        private ServerSocketConnection server = null;
        private InputStream IN; private OutputStream OUT;
        private String PID = genpid(), DB, address, port;
        private Hashtable sessions = (Hashtable) getobject("1", "sessions");

        private int start;
        private String[] wordlist;

        private Alert confirm = new Alert("Background Process", "Keep this process running in background?", null, AlertType.WARNING);
        private Form screen; private List list;
        private TextField inputField = new TextField("Command", "", 256, TextField.ANY);
        private StringItem console = new StringItem("", "");

        private Command BACK = new Command("Back", Command.SCREEN, 2),
                        EXECUTE = new Command("Send", Command.OK, 1),
                        CONNECT_CMD = new Command("Connect", Command.BACK, 1),
                        CLEAR = new Command("Clear", Command.SCREEN, 2),
                        VIEW = new Command("View info", Command.SCREEN, 2),
                        SAVE = new Command("Save Logs", Command.SCREEN, 2),
                        YES = new Command("Yes", Command.OK, 1),
                        NO = new Command("No", Command.BACK, 1);

        public Connect(String mode, String args, boolean root) {
            MOD = mode == null || mode.length() == 0 || mode.equals("nc") ? NC : mode.equals("prscan") ? PRSCAN : mode.equals("gobuster") ? GOBUSTER : mode.equals("server") ? SERVER : mode.equals("bind") ? BIND : -1;
            this.root = root;
            
            if (MOD == SERVER || MOD == BIND) {
                if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522", false); port = "31522"; DB = ""; } 
                else { port = getCommand(args); DB = getArgument(args); DB = DB.equals("") && MOD == SERVER ? env("$RESPONSE") : DB; }

                new Thread(this, MOD == BIND ? "Bind" : "Server").start();
                return;
            } else if (MOD == -1) { return; } 

            if (args == null || args.length() == 0) { return; }

            Hashtable proc = genprocess(MOD == NC ? "remote" : MOD == PRSCAN ? "prscan" : "gobuster", root, null);

            if (MOD == NC) {
                address = args;
                try {
                    CONN = (SocketConnection) Connector.open("socket://" + address);
                    IN = CONN.openInputStream(); OUT = CONN.openOutputStream();
                } catch (Exception e) { echoCommand(getCatch(e)); return; }

                screen = new Form(form.getTitle());
                inputField.setLabel("Remote (" + split(address, ':')[0] + ")");
                screen.append(console); screen.append(inputField);
                screen.addCommand(EXECUTE); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(VIEW);
                screen.setCommandListener(this);

                proc.put("socket", CONN); proc.put("in-stream", IN); proc.put("out-stream", OUT);
                proc.put("screen", screen);
                display.setCurrent(screen);
            } else {
                address = getCommand(args);
                list = new List(MOD == PRSCAN ? address + " Ports" : "GoBuster (" + address + ")", List.IMPLICIT);

                if (MOD == PRSCAN) { start = getNumber(getArgument(args).equals("") ? "1" : getArgument(args), 1, true); } 
                else { 
                    wordlist = split(getArgument(args).equals("") ? loadRMS("gobuster") : getcontent(getArgument(args)), '\n');
                    if (wordlist == null || wordlist.length == 0) { echoCommand("gobuster: blank word list"); return; }
                }

                list.addCommand(BACK); list.addCommand(CONNECT_CMD); list.addCommand(SAVE); 
                list.setCommandListener(this);
                
                proc.put("screen", list);
                display.setCurrent(list);
            }

            trace.put(PID, proc);
            new Thread(this, "NET").start();
        }

        public void commandAction(Command c, Displayable d) {
            if (d == confirm) {
                processCommand("xterm");
                if (c == NO) { stop(MOD == NC ? "remote" : MOD == PRSCAN ? "prscan" : "gobuster", root); } 
                else { keep = true; }
                return;
            }

            if (MOD == NC) {
                if (c == EXECUTE) {
                    String PAYLOAD = inputField.getString().trim();
                    inputField.setString("");

                    try { OUT.write((PAYLOAD + "\n").getBytes()); OUT.flush(); } 
                    catch (Exception e) { warnCommand(form.getTitle(), getCatch(e)); if (!keep) { trace.remove(PID); } }
                } 
                else if (c == BACK) { writeRMS("/home/remote", console.getText()); back(); } 
                else if (c == CLEAR) { console.setText(""); } 
                else if (c == VIEW) { 
                    try { warnCommand("Information", 
                            "Host: " + split(address, ':')[0] + "\n" +
                            "Port: " + split(address, ':')[1] + "\n\n" +
                            "Local Address: " + CONN.getLocalAddress() + "\n" +
                            "Local Port: " + CONN.getLocalPort());
                    } 
                    catch (Exception e) { warnCommand(form.getTitle(), "Couldn't read connection information!"); }
                }
            } else if (MOD == PRSCAN || MOD == GOBUSTER) {
                if (c == BACK) { back(); } 
                else if (c == CONNECT_CMD || c == List.SELECT_COMMAND) {
                    String ITEM = list.getString(list.getSelectedIndex());
                    if (MOD == PRSCAN) { processCommand("nc " + address + ":" + ITEM); } 
                    else { processCommand("execute tick Downloading...; wget " + address + "/" + getArgument(ITEM) + "; tick; nano; true"); }
                } 
                else if (c == SAVE) {
                    StringBuffer BUFFER = new StringBuffer();
                    for (int i = 0; i < list.size(); i++) { BUFFER.append(MOD == PRSCAN ? list.getString(i) : getArgument(list.getString(i))).append("\n"); }

                    nanoContent = BUFFER.toString().trim();
                    processCommand("nano", false);
                }
            }
        }

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
                        list.setTicker(new Ticker("Scanning port " + port + "..."));
                        if (!trace.containsKey(PID)) { break; }
                        Connector.open("socket://" + address + ":" + port, Connector.READ_WRITE, true).close();
                        list.append("" + port, null);
                    } catch (IOException e) { }
                }
                list.setTicker(null);
                if (!keep) { trace.remove(PID); }
                return;
            }
            else if (MOD == GOBUSTER) {
                list.setTicker(new Ticker("Searching..."));
                for (int i = 0; i < wordlist.length; i++) {
                    String path = wordlist[i].trim();
                    if (!trace.containsKey(PID)) { break; }
                    if (!path.equals("") && !path.startsWith("#")) {
                        try {
                            int code = verifyHTTP(address.startsWith("http") ? address + "/" + path : "http://" + address + "/" + path);
                            if (code != 404) list.append(code + " /" + path, null);
                        } catch (IOException e) { }
                    }
                }
                list.setTicker(null);
                if (!keep) { trace.remove(PID); }
                return;
            }
            else {
                if (sessions.containsKey(port)) { echoCommand("[-] Port '" + port + "' is unavailable"); return; }

                Hashtable proc = genprocess(MOD == SERVER ? "server" : "bind", root, null);
                proc.put("port", port); trace.put(PID, proc); sessions.put(port, MOD == SERVER ? "http-cli" : "nobody");

                while (trace.containsKey(PID)) {
                    try {
                        server = (ServerSocketConnection) Connector.open("socket://:" + port); proc.put("server", server); 
                        if (COUNT == 1) { echoCommand("[+] listening on port " + port); MIDletLogs("add info Server listening on port " + port); COUNT++; }

                        CONN = (SocketConnection) server.acceptAndOpen();
                        address = CONN.getAddress(); echoCommand("[+] " + address + " connected");

                        IN = CONN.openInputStream(); OUT = CONN.openOutputStream();
                        proc.put("in-stream", IN); proc.put("out-stream", OUT);

                        if (MOD == SERVER) {
                            byte[] buffer = new byte[4096];
                            int bytesRead = IN.read(buffer);
                            if (bytesRead == -1) { echoCommand("[-] " + address + " disconnected"); } 
                            else {
                                echoCommand("[+] " + address + " -> " + env(new String(buffer, 0, bytesRead).trim()));
                                OUT.write(getcontent(DB).getBytes()); OUT.flush();
                            }
                        } else {
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
                    } 
                    catch (IOException e) { echoCommand("[-] " + getCatch(e)); if (COUNT == 1) { echoCommand("[-] Server crashed"); break; } } 
                    finally {
                        try { if (IN != null) IN.close(); } catch (IOException e) { }
                        try { if (OUT != null) OUT.close(); } catch (IOException e) { }
                        try { if (CONN != null) CONN.close(); } catch (IOException e) { }
                        try { if (server != null) server.close(); } catch (IOException e) { }
                        
                        sessions.put(port, MOD == SERVER ? "http-cli" : "nobody");
                    }
                } 
                trace.remove(PID); sessions.remove(port);
                echoCommand("[-] Server stopped");
                MIDletLogs("add info Server was stopped");
            }
        }

        private int verifyHTTP(String fullUrl) throws IOException {
            HttpConnection H = null;
            try {
                H = (HttpConnection) Connector.open(fullUrl);
                H.setRequestMethod(HttpConnection.GET);
                return H.getResponseCode();
            } finally {
                try { if (H != null) H.close(); } catch (IOException ignored) {}
            }
        }

        private void back() {
            if (trace.containsKey(PID) && !asked) {
                confirm.addCommand(YES); confirm.addCommand(NO);
                confirm.setCommandListener(this);
                asked = true;
                display.setCurrent(confirm);
            } else { processCommand("xterm"); }
        }
    }
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
    public int deleteFile(String filename) { if (filename == null || filename.length() == 0) { return 2; } else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (CONN.exists()) { CONN.delete(); } else { echoCommand("rm: " + basename(filename) + ": not found"); return 127; } CONN.close(); } catch (SecurityException e) { echoCommand(getCatch(e)); return 13; } catch (Exception e) { echoCommand(getCatch(e)); return 1; } } else if (filename.startsWith("/home/")) { try { filename = filename.substring(6); if (filename.equals("OpenRMS")) { echoCommand("rm: " + filename + ": permission denied"); return 13; } RecordStore.deleteRecordStore(filename); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); return 127; } catch (Exception e) { echoCommand(getCatch(e)); return 1; } } else if (filename.startsWith("/")) { echoCommand("read-only storage"); return 5; } else { return deleteFile(path + filename); } return 0; }
    public int writeRMS(String filename, byte[] data) { if (filename == null || filename.length() == 0) { return 2; } else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.create(); } OutputStream OUT = CONN.openOutputStream(); OUT.write(data); OUT.flush(); OUT.close(); CONN.close(); } catch (Exception e) { echoCommand(getCatch(e)); return (e instanceof SecurityException) ? 13 : 1; } } else if (filename.startsWith("/home/")) { return writeRMS(filename.substring(6), data, 1); } else if (filename.startsWith("/")) { echoCommand("read-only storage"); return 5; } else { return writeRMS(path + filename, data); } return 0; }
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
    private String text2note(String content) { if (content == null || content.length() == 0) { return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:\nEND:VNOTE"; } content = replace(content, "=", "=3D"); content = replace(content, "\n", "=0A"); StringBuffer vnote = new StringBuffer(); vnote.append("BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" + content + "\nEND:VNOTE"); return vnote.toString(); }
    // |
    // Interfaces
    private String extractTitle(String htmlContent) { return extractTag(htmlContent, "title", "HTML Viewer"); }
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
    private String getMimeType(String filename) { filename = filename.toLowerCase(); return filename.endsWith(".amr") ? "audio/amr" : filename.endsWith(".wav") ? "audio/x-wav" : filename.endsWith(".mid") || filename.endsWith(".midi") ? "audio/midi" : "audio/mpeg"; }

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

        Hashtable PKG = parseProperties(getcontent(script));
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
        // |
        // Build dependencies
        if (PKG.containsKey("include")) { String[] include = split((String) PKG.get("include"), ','); for (int i = 0; i < include.length; i++) { int STATUS = importScript(include[i], root); if (STATUS != 0) { return STATUS; } } }
        // |
        // Start and handle APP process
        if (PKG.containsKey("process.name")) { start((String) PKG.get("process.name"), PID, (String) PKG.get("process.exit"), root); }
        if (PKG.containsKey("process.type")) { 
            String TYPE = (String) PKG.get("process.type"), PORT = (String) PKG.get("process.port"), MOD = (String) PKG.get("process." + (TYPE.equals("bind") ? "db" : "host")); 
            if (((Hashtable) getobject("1", "sessions")).containsKey(PORT)) { MIDletLogs("add warn Application port is unavailable."); return 68; }
            
            if (TYPE.equals("bind") || TYPE.equals("server")) { new Connect(TYPE, env(PORT + " " + (MOD == null ? "" : MOD)), root); }
            else { MIDletLogs("add error Invalid process type '" + TYPE + "'"); return 1; }            
        }
        // |
        // Start Application
        if (PKG.containsKey("config")) { int STATUS = processCommand((String) PKG.get("config"), true, root); if (STATUS != 0) { return STATUS; } }
        if (PKG.containsKey("mod") && PKG.containsKey("process.name")) { final String MOD = (String) PKG.get("mod"); final boolean ROOT = root; new Thread("MIDlet-Mod") { public void run() { while (trace.containsKey(PID)) { int STATUS = processCommand(MOD, true, ROOT); if (STATUS != 0) { kill(PID, false, ROOT); } } } }.start(); }
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
// Lua Runtime
class Lua {
    private boolean root, breakLoop = false, doreturn = false;
    private OpenTTY midlet;
    private String PID = null;
    private long uptime = System.currentTimeMillis();
    private Hashtable globals = new Hashtable(), proc = new Hashtable(), requireCache = new Hashtable();
    private Vector tokens;
    private int tokenIndex, status = 0, loopDepth = 0;
    // |
    public static final int PRINT = 0, EXEC = 1, ERROR = 2, PCALL = 3, GETENV = 4, REQUIRE = 5, CLOCK = 6, EXIT = 7, SETLOC = 8, PAIRS = 9, READ = 10, WRITE = 11, GC = 12, TOSTRING = 13, TONUMBER = 14, UPPER = 15, LOWER = 16, LEN = 17, MATCH = 18, REVERSE = 19, SUB = 20, RANDOM = 21, LOADS = 22, HASH = 23, BYTE = 24, SELECT = 25, TYPE = 26, CHAR = 27, TB_DECODE = 28, TB_PACK = 29, CONNECT = 30, SERVER = 31, ACCEPT = 32, CLOSE = 33, HTTP_GET = 34, HTTP_POST = 35;
    public static final int EOF = 0, NUMBER = 1, STRING = 2, BOOLEAN = 3, NIL = 4, IDENTIFIER = 5, PLUS = 6, MINUS = 7, MULTIPLY = 8, DIVIDE = 9, MODULO = 10, EQ = 11, NE = 12, LT = 13, GT = 14, LE = 15,  GE = 16, AND = 17, OR = 18, NOT = 19, ASSIGN = 20, IF = 21, THEN = 22, ELSE = 23, END = 24, WHILE = 25, DO = 26, RETURN = 27, FUNCTION = 28, LPAREN = 29, RPAREN = 30, COMMA = 31, LOCAL = 32, LBRACE = 33, RBRACE = 34, LBRACKET = 35, RBRACKET = 36, CONCAT = 37, DOT = 38, ELSEIF = 39, FOR = 40, IN = 41, POWER = 42, BREAK = 43, LENGTH = 44, VARARG = 45, REPEAT = 46, UNTIL = 47;
    public static final Object LUA_NIL = new Object();
    // |
    private static class Token { int type; Object value; Token(int type, Object value) { this.type = type; this.value = value; } public String toString() { return "Token(type=" + type + ", value=" + value + ")"; } }
    // |
    // Main
    public Lua(OpenTTY midlet, boolean root) {
        this.midlet = midlet; this.root = root;
        this.tokenIndex = 0; this.PID = midlet.genpid();
        this.proc = midlet.genprocess("lua", root, null);
        
        Hashtable os = new Hashtable(), io = new Hashtable(), string = new Hashtable(), table = new Hashtable(), pkg = new Hashtable(), socket = new Hashtable(), http = new Hashtable();
        String[] funcs = new String[] { "execute", "getenv", "clock", "setlocale", "exit" }; int[] loaders = new int[] { EXEC, GETENV, CLOCK, SETLOC, EXIT };
        for (int i = 0; i < funcs.length; i++) { os.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("os", os);

        funcs = new String[] { "loadlib" }; loaders = new int[] { REQUIRE };
        for (int i = 0; i < funcs.length; i++ ) { pkg.put(funcs[i], new LuaFunction(loaders[i])); } pkg.put("loaded", requireCache); globals.put("package", pkg);

        funcs = new String[] { "read", "write", "close" }; loaders = new int[] { READ, WRITE, CLOSE };
        for (int i = 0; i < funcs.length; i++) { io.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("io", io);

        funcs = new String[] { "pack", "decode" }; loaders = new int[] { TB_PACK, TB_DECODE };
        for (int i = 0; i < funcs.length; i++) { table.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("table", table);

        funcs = new String[] { "get", "post" }; loaders = new int[] { HTTP_GET, HTTP_POST };
        for (int i = 0; i < funcs.length; i++) { http.put(funcs[i], new LuaFunction(loaders[i])); } socket.put("http", http);

        funcs = new String[] { "connect", "server", "accept" }; loaders = new int[] { CONNECT, SERVER, ACCEPT };
        for (int i = 0; i < funcs.length; i++) { socket.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("socket", socket);

        funcs = new String[] { "upper", "lower", "len", "match", "reverse", "sub", "hash", "byte", "char" }; loaders = new int[] { UPPER, LOWER, LEN, MATCH, REVERSE, SUB, HASH, BYTE, CHAR };
        for (int i = 0; i < funcs.length; i++) { string.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("string", string);

        funcs = new String[] { "print", "error", "pcall", "require", "load", "pairs", "collectgarbage", "tostring", "tonumber", "select", "type" }; loaders = new int[] { PRINT, ERROR, PCALL, REQUIRE, LOADS, PAIRS, GC, TOSTRING, TONUMBER, SELECT, TYPE };
        for (int i = 0; i < funcs.length; i++) { globals.put(funcs[i], new LuaFunction(loaders[i])); }

        globals.put("random", new LuaFunction(RANDOM));
    }
    // | (Run Source code)
    public Hashtable run(String source, String code) { 
        proc.put("name", ("lua " + source).trim());
        midlet.trace.put(PID, proc);

        Hashtable ITEM = new Hashtable(); ITEM.put("status", status);
        
        try { 
            this.tokens = tokenize(code); 
            
            while (peek().type != EOF) { Object res = statement(globals); if (doreturn) { if (res != null) { ITEM.put("object", res); } doreturn = false; break; } }
        } 
        catch (Exception e) { midlet.processCommand("echo " + midlet.getCatch(e), true, root); status = 1; } 
        catch (Error e) { midlet.processCommand("echo " + (e.getMessage() == null ? e.toString() : e.getMessage()), true, root); status = 1; }

        midlet.trace.remove(PID);
        return ITEM;
    }
    // |
    // Tokenizer
    private Vector tokenize(String code) throws Exception {
        Vector tokens = new Vector();
        int i = 0;
        while (i < code.length()) {
            char c = code.charAt(i);
    
            if (isWhitespace(c) || c == ';') { i++; }
            else if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '-') {
                i += 2;
                if (i + 1 < code.length() && code.charAt(i) == '[' && code.charAt(i + 1) == '[') {
                    i += 2;
                    while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) i++;
                    if (i + 1 < code.length()) i += 2;
                } 
                else { while (i < code.length() && code.charAt(i) != '\n') i++; }
            }
    
            else if (c == '.') {
                if (i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') { tokens.addElement(new Token(VARARG, "...")); i += 3; } 
                else if (i + 1 < code.length() && code.charAt(i + 1) == '.') { tokens.addElement(new Token(CONCAT, "..")); i += 2; } 
                else { tokens.addElement(new Token(DOT, ".")); i++; }
            }
            else if (c == ':') { tokens.addElement(new Token(DOT, ".")); i++; }

            else if (isDigit(c) || (c == '.' && i + 1 < code.length() && isDigit(code.charAt(i + 1)))) {
                StringBuffer sb = new StringBuffer();
                boolean hasDecimal = false;
                while (i < code.length() && (isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) break;
                        // Verifica se ".." logo após o ponto
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') break;
                        hasDecimal = true;
                    }
                    sb.append(code.charAt(i));
                    i++;
                }
                try {
                    double numValue = Double.parseDouble(sb.toString());
                    tokens.addElement(new Token(NUMBER, new Double(numValue)));
                } 
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); }
                continue;
            }
            else if (c == '-' && i + 1 < code.length() && (isDigit(code.charAt(i + 1)) || (code.charAt(i + 1) == '.' && i + 2 < code.length() && isDigit(code.charAt(i + 2))))) {
                i++; // Pula o sinal negativo
                StringBuffer sb = new StringBuffer();
                sb.append('-'); // Adiciona o sinal negativo
                
                boolean hasDecimal = false;
                while (i < code.length() && (isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) break;
                        // Verifica se ".." logo após o ponto
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') break;
                        hasDecimal = true;
                    }
                    sb.append(code.charAt(i));
                    i++;
                }
                try {
                    double numValue = Double.parseDouble(sb.toString());
                    tokens.addElement(new Token(NUMBER, new Double(numValue)));
                } 
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); }
            }

            else if (c == '"' || c == '\'') { char quoteChar = c; StringBuffer sb = new StringBuffer(); i++; while (i < code.length() && code.charAt(i) != quoteChar) { sb.append(code.charAt(i)); i++; } if (i < code.length() && code.charAt(i) == quoteChar) { i++; } tokens.addElement(new Token(STRING, sb.toString())); }
            else if (c == '[' && i + 1 < code.length() && code.charAt(i + 1) == '[') { i += 2; StringBuffer sb = new StringBuffer(); while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) { sb.append(code.charAt(i)); i++; } if (i + 1 < code.length()) { i += 2; } tokens.addElement(new Token(STRING, sb.toString())); }

            else if (isLetter(c)) { StringBuffer sb = new StringBuffer(); while (i < code.length() && isLetterOrDigit(code.charAt(i))) { sb.append(code.charAt(i)); i++; } String word = sb.toString(); tokens.addElement(new Token((word.equals("true") || word.equals("false")) ? BOOLEAN : word.equals("nil") ? NIL : word.equals("and") ? AND : word.equals("or") ? OR : word.equals("not") ? NOT : word.equals("if") ? IF : word.equals("then") ? THEN : word.equals("else") ? ELSE : word.equals("elseif") ? ELSEIF : word.equals("end") ? END : word.equals("while") ? WHILE : word.equals("do") ? DO : word.equals("return") ? RETURN : word.equals("function") ? FUNCTION : word.equals("local") ? LOCAL : word.equals("for") ? FOR : word.equals("in") ? IN : word.equals("break") ? BREAK : word.equals("repeat") ? REPEAT : word.equals("until") ? UNTIL : IDENTIFIER, word)); }
    
            else if (c == '+') { tokens.addElement(new Token(PLUS, "+")); i++; }
            else if (c == '-') { tokens.addElement(new Token(MINUS, "-")); i++; }
            else if (c == '*') { tokens.addElement(new Token(MULTIPLY, "*")); i++; }
            else if (c == '/') { tokens.addElement(new Token(DIVIDE, "/")); i++; }
            else if (c == '%') { tokens.addElement(new Token(MODULO, "%")); i++; }
            else if (c == '(') { tokens.addElement(new Token(LPAREN, "(")); i++; }
            else if (c == ')') { tokens.addElement(new Token(RPAREN, ")")); i++; }
            else if (c == ',') { tokens.addElement(new Token(COMMA, ",")); i++; }
            else if (c == '^') { tokens.addElement(new Token(POWER, "^")); i++; }
            else if (c == '#') { tokens.addElement(new Token(LENGTH, "#")); i++; }
    
            else if (c == '=') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(EQ, "==")); i += 2; } else { tokens.addElement(new Token(ASSIGN, "=")); i++; } }
            else if (c == '~') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(NE, "~=")); i += 2; } else { throw new Exception("Unexpected character '~'"); } }
            else if (c == '<') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(LE, "<=")); i += 2; } else { tokens.addElement(new Token(LT, "<")); i++; } }
            else if (c == '>') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(GE, ">=")); i += 2; } else { tokens.addElement(new Token(GT, ">")); i++; } }

            else if (c == '{') { tokens.addElement(new Token(LBRACE, "{")); i++; }
            else if (c == '}') { tokens.addElement(new Token(RBRACE, "}")); i++; }
            else if (c == '[') { tokens.addElement(new Token(LBRACKET, "[")); i++; }
            else if (c == ']') { tokens.addElement(new Token(RBRACKET, "]")); i++; }

            else { throw new Exception("Unexpected character '" + c + "'"); }
        }

        tokens.addElement(new Token(EOF, "EOF"));
        return tokens;
    }
    private Token peek() { if (tokenIndex < tokens.size()) { return (Token) tokens.elementAt(tokenIndex); } return new Token(EOF, "EOF"); }
    private Token peekNext() { if (tokenIndex + 1 < tokens.size()) { return (Token) tokens.elementAt(tokenIndex + 1); } return new Token(EOF, "EOF"); }
    private Token consume() { if (tokenIndex < tokens.size()) { return (Token) tokens.elementAt(tokenIndex++); } return new Token(EOF, "EOF"); }
    private Token consume(int expectedType) throws Exception { Token token = peek(); if (token.type == expectedType) { tokenIndex++; return token; } throw new Exception("Expected token type " + expectedType + " but got " + token.type + " with value " + token.value); }
    // |
    // Statements
    private Object statement(Hashtable scope) throws Exception {
        Token current = peek();
        
        if (midlet.trace.containsKey(PID)) { } else { throw new Error("Process killed"); } 
        if (status != 0) { throw new Error(); }

        if (current.type == IDENTIFIER) {
            // lookahead seguro: verifica se o padrão é IDENT (COMMA IDENT)* ASSIGN
            int la = 0;
            boolean patternIsMultiAssign = false;
            if (tokenIndex + la < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la)).type == IDENTIFIER) {
                la++; // passou o primeiro IDENT
                // consumir pares ", IDENT"
                while (tokenIndex + la < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la)).type == COMMA) {
                    // next after comma must be IDENT, senão não é múltipla atribuição
                    if (!(tokenIndex + la + 1 < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la + 1)).type == IDENTIFIER)) {
                        patternIsMultiAssign = false;
                        break;
                    }
                    la += 2; // pulando ", IDENT"
                }
                // depois disso, se houver ASSIGN, então é atribuição múltipla
                if (tokenIndex + la < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la)).type == ASSIGN) {
                    patternIsMultiAssign = true;
                }
            }

            // Caso 1: chamada direta: foo(...)
            // (se o próximo token for LPAREN e NÃO faz parte de atribuição múltipla)
            Token next = (tokenIndex + 1 < tokens.size()) ? (Token) tokens.elementAt(tokenIndex + 1) : new Token(EOF, "EOF");
            if (!patternIsMultiAssign && next.type == LPAREN) {
                String funcName = (String) consume(IDENTIFIER).value;
                callFunction(funcName, scope); // consome os parênteses e argumentos
                return null;
            }

            // Caso: atribuição múltipla detectada
            if (patternIsMultiAssign) {
                // Parse lista de variáveis (lado esquerdo)
                Vector varNames = new Vector();
                varNames.addElement(((Token) consume(IDENTIFIER)).value);
                while (peek().type == COMMA) {
                    consume(COMMA);
                    varNames.addElement(((Token) consume(IDENTIFIER)).value);
                }
                consume(ASSIGN);

                // Parse lista de expressões (lado direito)
                Vector values = new Vector();
                values.addElement(expression(scope));
                while (peek().type == COMMA) {
                    consume(COMMA);
                    values.addElement(expression(scope));
                }

                // Expansão da última expressão caso seja Vector (para múltiplos retornos)
                Vector assignValues = new Vector();
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.elementAt(i);
                    if (i == values.size() - 1 && v instanceof Vector) {
                        Vector expanded = (Vector) v;
                        for (int j = 0; j < expanded.size(); j++)
                            assignValues.addElement(expanded.elementAt(j));
                    } else {
                        assignValues.addElement(v);
                    }
                }

                for (int i = 0; i < varNames.size(); i++) {
                    String v = (String) varNames.elementAt(i);
                    Object val = i < assignValues.size() ? assignValues.elementAt(i) : null;
                    // armazenar LUA_NIL se val == null
                    scope.put(v, val == null ? LUA_NIL : val);
                }
                return null;
            }

            // Caso 2: identifier seguido de . ou [ -> pode ser atribuição em tabela OU chamada de função: t.a = ... OU t.a(...)
            String varName = (String) consume(IDENTIFIER).value;
            if (peek().type == DOT || peek().type == LBRACKET) {
                Object[] pair = resolveTableAndKey(varName, scope);
                Object targetTable = pair[0];
                Object key = pair[1];
                if (!(targetTable instanceof Hashtable)) { throw new Exception("Attempt to index non-table value"); }

                if (peek().type == ASSIGN) {
                    // t.a = expr
                    consume(ASSIGN);
                    Object value = expression(scope);
                    ((Hashtable) targetTable).put(key, value == null ? LUA_NIL : value);
                    return null;
                } else if (peek().type == LPAREN) {
                    // t.a(...) -> chamar função armazenada em t[a]
                    Object funcObj = unwrap(((Hashtable) targetTable).get(key));
                    return callFunctionObject(funcObj, scope);
                } else {
                    // Apenas acesso a campo como statement: ignore/avalie se quiser
                    return null;
                }
            } else {
                // Caso 3: atribuição simples: a = expr
                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object value = expression(scope);
                    scope.put(varName, value == null ? LUA_NIL : value);
                    return null;
                } else if (peek().type == LPAREN) {
                    // chamada de função usando nome simples: foo(...)
                    return callFunction(varName, scope);
                } else {
                    // referência isolada como statement (ex: apenas "x" numa linha) - no momento ignora
                    return null;
                }
            }
        }

        else if (current.type == IF) { return ifStatement(scope); } 
        else if (current.type == FOR) { return forStatement(scope); }
        else if (current.type == WHILE) { return whileStatement(scope); }
        else if (current.type == REPEAT) { return repeatStatement(scope); } 
        else if (current.type == RETURN) { consume(RETURN); doreturn = true; return expression(scope); } 
        else if (current.type == FUNCTION) { return functionDefinition(scope); } 
        else if (current.type == LOCAL) {
            consume(LOCAL);
            if (peek().type == FUNCTION) {
                consume(FUNCTION);
                String funcName = (String) consume(IDENTIFIER).value;
                // Leitura dos parâmetros da função, aceitando vararg
                consume(LPAREN);
                Vector params = new Vector();
                while (true) {
                    int t = peek().type;
                    if (t == IDENTIFIER) {
                        params.addElement(consume(IDENTIFIER).value);
                    } else if (t == VARARG) {
                        consume(VARARG);
                        params.addElement("...");
                        break; // vararg deve ser o último parâmetro
                    } else {
                        break;
                    }
                    if (peek().type == COMMA) {
                        consume(COMMA);
                    } else {
                        break;
                    }
                }
                consume(RPAREN);
                // Captura o corpo da função até o END correspondente
                Vector bodyTokens = new Vector();
                int depth = 1;
                while (depth > 0) {
                    Token token = consume();
                    if (token.type == FUNCTION || token.type == IF || token.type == WHILE || token.type == FOR) {
                        depth++;
                    } else if (token.type == END) {
                        depth--;
                    } else if (token.type == EOF) {
                        throw new Exception("Unmatched 'function' statement: Expected 'end'");
                    }
                    if (depth > 0) {
                        bodyTokens.addElement(token);
                    }
                }
                // Cria a função e adiciona no escopo local
                LuaFunction func = new LuaFunction(params, bodyTokens, scope);
                scope.put(funcName, func);
                return null;
            } else {
                // Novo: suportar múltiplas declarações locais: local a, b, c = expr1, expr2, expr3
                Vector varNames = new Vector();
                // deve haver ao menos um IDENTIFIER
                varNames.addElement(((Token) consume(IDENTIFIER)).value);
                while (peek().type == COMMA) {
                    consume(COMMA);
                    varNames.addElement(((Token) consume(IDENTIFIER)).value);
                }
        
                // Se houver atribuição, parsear lista de expressões
                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Vector values = new Vector();
                    values.addElement(expression(scope));
                    while (peek().type == COMMA) {
                        consume(COMMA);
                        values.addElement(expression(scope));
                    }
        
                    // Expansão da última expressão caso seja Vector (para múltiplos retornos)
                    Vector assignValues = new Vector();
                    for (int i = 0; i < values.size(); i++) {
                        Object v = values.elementAt(i);
                        if (i == values.size() - 1 && v instanceof Vector) {
                            Vector expanded = (Vector) v;
                            for (int j = 0; j < expanded.size(); j++)
                                assignValues.addElement(expanded.elementAt(j));
                        } else {
                            assignValues.addElement(v);
                        }
                    }
        
                    // Atribui aos nomes locais (se faltar valor, coloca null)
                    for (int i = 0; i < varNames.size(); i++) {
                        String v = (String) varNames.elementAt(i);
                        Object val = i < assignValues.size() ? assignValues.elementAt(i) : null;
                        scope.put(v, val == null ? LUA_NIL : val);
                    }
                } else {
                    // Sem '=', inicializa todos como nil (LUA_NIL)
                    for (int i = 0; i < varNames.size(); i++) {
                        String v = (String) varNames.elementAt(i);
                        scope.put(v, LUA_NIL);
                    }
                }
                return null;
            }
        }
        else if (current.type == BREAK) {
            if (loopDepth == 0) {
                throw new RuntimeException("Syntax error: 'break' is only valid inside a loop");
            }
            consume(BREAK);
            breakLoop = true; // Sinalizar que o loop deve ser interrompido
            return null;
        }

        else if (current.type == LPAREN || current.type == NUMBER || current.type == STRING || current.type == BOOLEAN || current.type == NIL || current.type == NOT) { expression(scope); return null; }

        throw new RuntimeException("Unexpected token at statement: " + current.value);
    }
    // |
    private Object ifStatement(Hashtable scope) throws Exception {
        consume(IF);
        Object cond = expression(scope);
        consume(THEN);

        Object result = null;
        boolean taken = false;

        // executa OU pula o primeiro bloco
        if (isTruthy(cond)) {
            taken = true;
            while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                result = statement(scope);
                if (result != null && doreturn) { doreturn = false; return result; }
            }
        } else {
            skipIfBodyUntilElsePart(); // posiciona no ELSEIF/ELSE/END correspondente deste if
        }

        // trata zero ou mais ELSEIF
        while (peek().type == ELSEIF) {
            consume(ELSEIF);
            cond = expression(scope);
            consume(THEN);

            if (!taken && isTruthy(cond)) {
                taken = true;
                while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                    result = statement(scope);
                    if (result != null && doreturn) { doreturn = false; return result; }
                }
            } else {
                skipIfBodyUntilElsePart();
            }
        }

        // trata ELSE opcional
        if (peek().type == ELSE) {
            consume(ELSE);
            if (!taken) {
                while (peek().type != END) {
                    result = statement(scope);
                    if (result != null && doreturn) { doreturn = false; return result; }
                }
            } else {
                // já executamos um ramo verdadeiro; apenas pular até o END
                skipUntilMatchingEnd();
            }
        }

        consume(END);
        return result;
    }
    private Object whileStatement(Hashtable scope) throws Exception {
        consume(WHILE);
        int conditionStartTokenIndex = tokenIndex;
    
        Object result = null;
        boolean endAlreadyConsumed = false;
    
        loopDepth++; // Entrou em um loop
    
        while (true) {
            tokenIndex = conditionStartTokenIndex;
            Object condition = expression(scope);
    
            if (!isTruthy(condition) || breakLoop) {
                // Pular o corpo até o END correspondente
                int depth = 1;
                while (depth > 0) {
                    Token token = consume();
                    if (token.type == IF || token.type == WHILE || token.type == FUNCTION || token.type == FOR) depth++;
                    else if (token.type == END) depth--;
                    else if (token.type == EOF) throw new RuntimeException("Unmatched 'while' statement: Expected 'end'");
                }
                endAlreadyConsumed = true; // já consumimos o END acima
                break;
            }
    
            consume(DO);
    
            // Executa corpo até o END do laço
            while (peek().type != END) {
                result = statement(scope);
                if (breakLoop) { breakLoop = false; endAlreadyConsumed = true; break; }
                if (result != null && doreturn) {
                    // "return" dentro do while: consome até o END do laço e retorna
                    int depth = 1;
                    while (depth > 0) {
                        Token token = consume();
                        if (token.type == IF || token.type == WHILE || token.type == FUNCTION || token.type == FOR) depth++;
                        else if (token.type == END) depth--;
                        else if (token.type == EOF) throw new Exception("Unmatched 'while' statement: Expected 'end'");
                    }
                    loopDepth--; // Saindo do loop
                    doreturn = false;
                    return result;
                }
            }
            tokenIndex = conditionStartTokenIndex;
        }
    
        loopDepth--; // Saindo do loop
    
        if (!endAlreadyConsumed) consume(END);
        return null;
    }
    private Object forStatement(Hashtable scope) throws Exception {
        consume(FOR);
    
        loopDepth++; // Entrou em um loop
    
        // Lookahead simples: se tiver IDENT, '=', é for numérico
        if (peek().type == IDENTIFIER) {
            Token t1 = (Token) peek();
            // Salva estado
            int save = tokenIndex;
            String name = (String) consume(IDENTIFIER).value;
    
            // Detecta se é um loop numérico
            if (peek().type == ASSIGN) {
                // ------ for numérico: for i = a, b [, c] do ... end
                consume(ASSIGN);
                Object a = expression(scope);
                consume(COMMA);
                Object b = expression(scope);
                Double start = (a instanceof Double) ? (Double) a : new Double(Double.parseDouble(toLuaString(a)));
                Double stop  = (b instanceof Double) ? (Double) b : new Double(Double.parseDouble(toLuaString(b)));
                Double step  = new Double(1.0);
                if (peek().type == COMMA) {
                    consume(COMMA);
                    Object c = expression(scope);
                    step = (c instanceof Double) ? (Double) c : new Double(Double.parseDouble(toLuaString(c)));
                    if (((Double) step).doubleValue() == 0.0) throw new Exception("for step must not be zero");
                }
                consume(DO);
    
                // Captura corpo até END (com profundidade incluindo FOR)
                Vector bodyTokens = new Vector();
                int depth = 1;
                while (depth > 0) {
                    Token tk = consume();
                    if (tk.type == IF || tk.type == WHILE || tk.type == FUNCTION || tk.type == FOR) depth++;
                    else if (tk.type == END) depth--;
                    else if (tk.type == EOF) throw new Exception("Unmatched 'for' statement: Expected 'end'");
                    if (depth > 0) bodyTokens.addElement(tk);
                }
    
                double iVal = start.doubleValue();
                double stopVal = stop.doubleValue();
                double stepVal = step.doubleValue();
    
                // Executa corpo reusando bodyTokens a cada iteração
                while ((stepVal > 0 && iVal <= stopVal) || (stepVal < 0 && iVal >= stopVal)) {
                    if (breakLoop) {
                        breakLoop = false; // Resetar o estado do break
                        break;
                    }
    
                    scope.put(name, new Double(iVal));
    
                    // Salva estado atual
                    int originalTokenIndex = tokenIndex;
                    Vector originalTokens = tokens;
    
                    // Usa bodyTokens como programa atual
                    tokens = bodyTokens;
                    tokenIndex = 0;
    
                    Object ret = null;
                    while (peek().type != EOF) {
                        ret = statement(scope);
                        if (ret != null && doreturn) { doreturn = false; break; }
                    }
    
                    // Restaura estado
                    tokenIndex = originalTokenIndex;
                    tokens = originalTokens;
    
                    if (ret != null) return ret;
    
                    iVal += stepVal;
                }
    
                loopDepth--; // Saindo do loop
                return null;
            } else {
                // ------ for genérico: for k, v in expr do ... end
                // Restaura estado e parseia nomes corretamente
                tokenIndex = save;
                Vector names = new Vector();
                names.addElement(((Token) consume(IDENTIFIER)).value);
                while (peek().type == COMMA) {
                    consume(COMMA);
                    names.addElement(((Token) consume(IDENTIFIER)).value);
                }
                consume(IN);
                Object iterSrc = expression(scope);
                consume(DO);
    
                // Captura corpo
                Vector bodyTokens = new Vector();
                int depth2 = 1;
                while (depth2 > 0) {
                    Token tk = consume();
                    if (tk.type == IF || tk.type == WHILE || tk.type == FUNCTION || tk.type == FOR) depth2++;
                    else if (tk.type == END) depth2--;
                    else if (tk.type == EOF) throw new Exception("Unmatched 'for' statement: Expected 'end'");
                    if (depth2 > 0) bodyTokens.addElement(tk);
                }
    
                // Itera: se vier de pairs(t), será Hashtable; se vier Vector de pares, também aceita
                if (iterSrc instanceof Hashtable) {
                    Hashtable ht = (Hashtable) iterSrc;
                    for (Enumeration e = ht.keys(); e.hasMoreElements();) {
                        Object k = e.nextElement();
                        Object v = unwrap(ht.get(k));
                        // Bind nomes
                        if (names.size() >= 1) scope.put((String) names.elementAt(0), (k == null ? LUA_NIL : k));
                        if (names.size() >= 2) scope.put((String) names.elementAt(1), (v == null ? LUA_NIL : v));
    
                        // Executa corpo
                        int originalTokenIndex = tokenIndex;
                        Vector originalTokens = tokens;
                        tokens = bodyTokens;
                        tokenIndex = 0;
    
                        Object ret = null;
                        while (peek().type != EOF) {
                            ret = statement(scope);
                            if (ret != null && doreturn) { doreturn = false; return ret; }
                        }
    
                        tokenIndex = originalTokenIndex;
                        tokens = originalTokens;
                        if (ret != null) return ret;
    
                        if (breakLoop) {
                            breakLoop = false; // Resetar o estado do break
                            break;
                        }
                    }
                } else if (iterSrc instanceof Vector) {
                    Vector vec = (Vector) iterSrc;
                    for (int idx = 0; idx < vec.size(); idx++) {
                        Object item = vec.elementAt(idx);
                        Object k = null, v = null;
                        if (item instanceof Vector) {
                            Vector pair = (Vector) item;
                            if (pair.size() > 0) k = pair.elementAt(0);
                            if (pair.size() > 1) v = pair.elementAt(1);
                        } else {
                            k = new Double(idx + 1);
                            v = item;
                        }
                        if (names.size() >= 1) scope.put((String) names.elementAt(0), (k == null ? LUA_NIL : k));
                        if (names.size() >= 2) scope.put((String) names.elementAt(1), (v == null ? LUA_NIL : v));
    
                        int originalTokenIndex = tokenIndex;
                        Vector originalTokens = tokens;
                        tokens = bodyTokens;
                        tokenIndex = 0;
    
                        Object ret = null;
                        while (peek().type != EOF) {
                            ret = statement(scope);
                            if (ret != null && doreturn) { doreturn = false; return ret; }
                        }
    
                        tokenIndex = originalTokenIndex;
                        tokens = originalTokens;
                        if (ret != null) return ret;
    
                        if (breakLoop) {
                            breakLoop = false; // Resetar o estado do break
                            break;
                        }
                    }
                } else if (iterSrc == null) {
                    // Nada a iterar (nil)
                } else {
                    throw new Exception("Generic for: unsupported iterator source");
                }
    
                loopDepth--; // Saindo do loop
                return null;
            }
        }
    
        loopDepth--; // Saindo do loop
        throw new Exception("Malformed 'for' statement");
    }
    private Object repeatStatement(Hashtable scope) throws Exception {
        consume(REPEAT);

        int bodyStartTokenIndex = tokenIndex;
        Object result = null;

        loopDepth++; // Entrou em um loop

        while (true) {
            tokenIndex = bodyStartTokenIndex;

            while (peek().type != UNTIL) {
                result = statement(scope);

                if (breakLoop) {
                    breakLoop = false;
                    while (peek().type != UNTIL && peek().type != EOF) {
                        consume();
                    }
                    break;
                }
                if (result != null && doreturn) {
                    while (peek().type != UNTIL && peek().type != EOF) { consume(); }
                    loopDepth--;
                    doreturn = false;
                    return result;
                }
            }

            consume(UNTIL);
            Object cond = expression(scope);

            if (isTruthy(cond)) { break; }
        }

        loopDepth--; 
        return null;
    }
    private Object functionDefinition(Hashtable scope) throws Exception {
        consume(FUNCTION);
        String funcName = (String) consume(IDENTIFIER).value;

        // Verifica se é atribuição em tabela: x.y ou x[y]
        boolean isTableAssignment = (peek().type == DOT || peek().type == LBRACKET);
        Object targetTable = null, key = null;

        if (isTableAssignment) {
            Object[] pair = resolveTableAndKey(funcName, scope);
            targetTable = pair[0];
            key = pair[1];
            if (!(targetTable instanceof Hashtable)) throw new Exception("Attempt to index non-table value in function definition");
        }


        consume(LPAREN);
        Vector params = new Vector();
        while (true) {
            int t = peek().type;

            if (t == IDENTIFIER) { params.addElement(consume(IDENTIFIER).value); } 
            else if (t == VARARG) { consume(VARARG); params.addElement("..."); break; } 
            else { break; } 

            if (peek().type == COMMA) { consume(COMMA); } 
            else { break; }
        }
        consume(RPAREN);

        // Captura corpo da função até o END correspondente
        Vector bodyTokens = new Vector();
        int depth = 1;
        while (depth > 0) {
            Token token = consume();
            if (token.type == FUNCTION || token.type == IF || token.type == WHILE || token.type == FOR) depth++;
            else if (token.type == END) depth--;
            else if (token.type == EOF) throw new RuntimeException("Unmatched 'function' statement: Expected 'end'");
            if (depth > 0) bodyTokens.addElement(token);
        }

        LuaFunction func = new LuaFunction(params, bodyTokens, scope);

        if (isTableAssignment) { ((Hashtable) targetTable).put(key, func); } 
        else { scope.put(funcName, func); }

        return null;
    }
    // |
    // Expressions
    private Object expression(Hashtable scope) throws Exception { return logicalOr(scope); }
    private Object logicalOr(Hashtable scope) throws Exception { Object left = logicalAnd(scope); while (peek().type == OR) { consume(OR); Object right = logicalAnd(scope); left = isTruthy(left) ? left : right; } return left; }
    private Object logicalAnd(Hashtable scope) throws Exception { Object left = comparison(scope); while (peek().type == AND) { consume(AND); Object right = comparison(scope); left = isTruthy(left) ? right : left; } return left; }
    private Object comparison(Hashtable scope) throws Exception { Object left = concatenation(scope); while (peek().type == EQ || peek().type == NE || peek().type == LT || peek().type == GT || peek().type == LE || peek().type == GE) { Token op = consume(); Object right = concatenation(scope); if (op.type == EQ) { left = new Boolean((left == null && right == null) || (left != null && left.equals(right))); } else if (op.type == NE) { left = new Boolean(!((left == null && right == null) || (left != null && left.equals(right)))); } else if (op.type == LT) { left = new Boolean(((Double) left).doubleValue() < ((Double) right).doubleValue()); } else if (op.type == GT) { left = new Boolean(((Double) left).doubleValue() > ((Double) right).doubleValue()); } else if (op.type == LE) { left = new Boolean(((Double) left).doubleValue() <= ((Double) right).doubleValue()); } else if (op.type == GE) { left = new Boolean(((Double) left).doubleValue() >= ((Double) right).doubleValue()); } } return left; }
    // |
    // Strings
    private String toLuaString(Object obj) { if (obj == null) { return "nil"; } if (obj instanceof Boolean) { return ((Boolean)obj).booleanValue() ? "true" : "false"; } if (obj instanceof Double) { double d = ((Double)obj).doubleValue(); if (d == (long)d) return String.valueOf((long)d); return String.valueOf(d); } return obj.toString(); }
    private Object concatenation(Hashtable scope) throws Exception { Object left = arithmetic(scope); while (peek().type == CONCAT) { consume(CONCAT); Object right = arithmetic(scope); left = toLuaString(left) + toLuaString(right); } return left; }
    // |
    // Arithmetic
    private Object arithmetic(Hashtable scope) throws Exception {
        Object left = term(scope); // Chama o novo método
        while (peek().type == PLUS || peek().type == MINUS) {
            Token op = consume();
            Object right = term(scope); // Chama o novo método
            if (!(left instanceof Double) || !(right instanceof Double)) { throw new ArithmeticException("Arithmetic operation on non-number types."); }

            double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();
            if (op.type == PLUS) { left = new Double(lVal + rVal); } 
            else if (op.type == MINUS) { left = new Double(lVal - rVal); }
        }
        return left;
    }
    private Object term(Hashtable scope) throws Exception {
        Object left = exponentiation(scope); // Agora chama exponentiation em vez de factor
        while (peek().type == MULTIPLY || peek().type == DIVIDE || peek().type == MODULO) {
            Token op = consume();
            Object right = exponentiation(scope); // Agora chama exponentiation em vez de factor
            if (!(left instanceof Double) || !(right instanceof Double)) {
                throw new ArithmeticException("Arithmetic operation on non-number types.");
            }
            double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();

            if (op.type == MULTIPLY) { left = new Double(lVal * rVal); } 
            else if (op.type == DIVIDE) { if (rVal == 0) { throw new Exception("Division by zero."); } left = new Double(lVal / rVal); } 
            else if (op.type == MODULO) { if (rVal == 0) { throw new Exception("Modulo by zero."); } left = new Double(lVal % rVal); }
        }
        return left;
    }
    private Object exponentiation(Hashtable scope) throws Exception {
        Object left = factor(scope);
        while (peek().type == POWER) {
            consume(POWER);
            Object right = factor(scope);
            if (!(left instanceof Double) || !(right instanceof Double)) { 
                throw new ArithmeticException("Arithmetic operation on non-number types."); 
            }

            double base = ((Double) left).doubleValue();
            double exponent = ((Double) right).doubleValue();
            
            double result;
            if (exponent == 0) {
                result = 1; // Qualquer número elevado a 0 é 1
            } else if (exponent == 0.5) {
                // Caso especial: raiz quadrada
                if (base < 0) { throw new ArithmeticException("Square root of negative number."); }
                result = Math.sqrt(base);
            } else if (exponent < 0 && Math.floor(exponent) == exponent) {
                // Expoente negativo inteiro
                base = 1 / base;
                exponent = -exponent;
                result = 1;
                for (int i = 0; i < (int) exponent; i++) {
                    result *= base;
                }
            } else if (Math.floor(exponent) == exponent) {
                // Expoente positivo inteiro
                result = 1;
                for (int i = 0; i < (int) exponent; i++) { result *= base; }
            } 
            else { throw new ArithmeticException("Fractional exponent not supported: " + exponent); }

            left = new Double(result);
        }
        return left;
    }

    private Object factor(Hashtable scope) throws Exception {
        Token current = peek();
        
        if (current.type == NUMBER) { return consume(NUMBER).value; } 
        else if (current.type == STRING) { return consume(STRING).value; } 
        else if (current.type == BOOLEAN) { consume(BOOLEAN); return new Boolean(current.value.equals("true")); } 
        else if (current.type == NIL) { consume(NIL); return null; } 
        else if (current.type == NOT) { consume(NOT); return new Boolean(!isTruthy(factor(scope))); } 
        else if (current.type == LPAREN) { consume(LPAREN); Object value = expression(scope); consume(RPAREN); return value; } 
        if (current.type == LENGTH) {
            consume(LENGTH);
            Object val = factor(scope); // aplica o operador unário ao próximo fator
            if (val == null || val instanceof Boolean) throw new RuntimeException("attempt to get length of a " + (val == null ? "nil" : "boolean") + " value");
            if (val instanceof String) { return new Double(((String) val).length()); } 
            else if (val instanceof Hashtable) { return new Double(((Hashtable) val).size()); } 
            else if (val instanceof Vector) { return new Double(((Vector) val).size()); } 
            else { return new Double(0); }
        }
        else if (current.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            Object value = unwrap(scope.get(name));
            if (value == null && scope == globals == false) {
                // fallback handled below — but keep safe checks
            }
            if (value == null && globals.containsKey(name)) {
                value = unwrap(globals.get(name));
            }
            // Leitura de campos encadeados: t.a.b  e/ou t["x"]
            while (peek().type == LBRACKET || peek().type == DOT) {
                Object key = null;
                if (peek().type == LBRACKET) {
                    consume(LBRACKET);
                    key = expression(scope);
                    consume(RBRACKET);
                } else { // DOT
                    consume(DOT);
                    Token fieldToken = consume(IDENTIFIER);
                    key = (String) fieldToken.value;
                }

                if (value == null) {
                    // No Lua, ler campo de nil retorna nil
                    return null;
                }
                if (!(value instanceof Hashtable)) {
                    // No Lua, tentar indexar não-table lança erro
                    throw new Exception("attempt to index a non-table value");
                }
                value = unwrap(((Hashtable)value).get(key));
            }

            // Se o próximo token for parênteses, chamamos a função (value deve ser um LuaFunction)
            if (peek().type == LPAREN) { return callFunctionObject(value, scope); }

            return value;
        }
        else if (current.type == FUNCTION) {
            consume(FUNCTION);

            // Parâmetros
            consume(LPAREN);
            Vector params = new Vector();
            if (peek().type == IDENTIFIER) {
                params.addElement(consume(IDENTIFIER).value);
                while (peek().type == COMMA) {
                    consume(COMMA);
                    params.addElement(consume(IDENTIFIER).value);
                }
            }
            consume(RPAREN);

            // Corpo da função
            Vector bodyTokens = new Vector();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                if (token.type == FUNCTION || token.type == IF || token.type == WHILE || token.type == FOR) depth++;
                else if (token.type == END) depth--;
                else if (token.type == EOF) throw new RuntimeException("Unmatched 'function' statement: Expected 'end'");
                if (depth > 0) bodyTokens.addElement(token);
            }

            // Cria e retorna a função anônima
            return new LuaFunction(params, bodyTokens, scope);
        }
        else if (current.type == VARARG) {
            consume(VARARG);
            Object varargs = scope.get("...");
            if (varargs == null) return new Hashtable();
            return varargs;
        }
        else if (current.type == LBRACE) { // Table literal
            consume(LBRACE);
            Hashtable table = new Hashtable();
            int index = 1; // Lua tables podem usar números sequenciais automaticamente

            while (peek().type != RBRACE) {
                Object key = null, value = null;

                if (peek().type == IDENTIFIER && peekNext().type == ASSIGN) {
                    // Caso: nome = valor
                    key = consume(IDENTIFIER).value; // Consome o nome
                    consume(ASSIGN); // Consome o '='
                    value = expression(scope); // Avalia o valor
                } else if (peek().type == LBRACKET) {
                    // Caso: [expr] = valor
                    consume(LBRACKET);
                    key = expression(scope);
                    consume(RBRACKET);
                    consume(ASSIGN);
                    value = expression(scope);
                } else {
                    // Caso: apenas valores (modo array-style)
                    value = expression(scope);
                    key = new Double(index++); // Chave numérica automática
                }

                table.put(key, value == null ? LUA_NIL : value);

                if (peek().type == COMMA) {
                    consume(COMMA); // Consome a vírgula opcional
                } else if (peek().type == RBRACE) {
                    break; // Finaliza a tabela
                } else {
                    throw new Exception("Malformed table syntax.");
                }
            }

            consume(RBRACE); // Consome o '}' que fecha a tabela
            return table;
        }

        throw new Exception("Unexpected token at factor: " + current.value);
    }

    private Object callFunction(String funcName, Hashtable scope) throws Exception {
        consume(LPAREN);
        Vector args = new Vector();
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) {
                consume(COMMA);
                args.addElement(expression(scope));
            }
        }
        consume(RPAREN);

        Object funcObj = unwrap(scope.get(funcName));
        if (funcObj == null && globals.containsKey(funcName)) { funcObj = unwrap(globals.get(funcName)); }

        if (funcObj instanceof LuaFunction) { return ((LuaFunction) funcObj).call(args); } 
        else { throw new RuntimeException("Attempt to call a non-function value: " + funcName); }
    }
    private Object callFunctionObject(Object funcObj, Hashtable scope) throws Exception {
        consume(LPAREN);
        Vector args = new Vector();
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) {
                consume(COMMA);
                args.addElement(expression(scope));
            }
        }
        consume(RPAREN);

        if (funcObj instanceof LuaFunction) { return ((LuaFunction) funcObj).call(args); } 
        else { throw new Exception("Attempt to call a non-function value (by object)."); }
    }

    private Object unwrap(Object v) { return v == LUA_NIL ? null : v; }

    private void skipIfBodyUntilElsePart() throws Exception { int depth = 1; while (true) { Token t = consume(); if (t.type == IF || t.type == WHILE || t.type == FUNCTION || t.type == FOR) { depth++; } else if (t.type == END) { depth--; if (depth == 0) { tokenIndex--; return; } } else if ((t.type == ELSEIF || t.type == ELSE) && depth == 1) { tokenIndex--; return; } else if (t.type == EOF) { throw new Exception("Unmatched 'if' statement: Expected 'end'"); } } }
    private void skipUntilMatchingEnd() throws Exception { int depth = 1; while (depth > 0) { Token t = consume(); if (t.type == IF || t.type == WHILE || t.type == FUNCTION || t.type == FOR) { depth++; } else if (t.type == END) { depth--; } else if (t.type == EOF) { throw new Exception("Unmatched 'if' statement: Expected 'end'"); } } tokenIndex--; }

    private boolean isTruthy(Object value) { if (value == null) { return false; } if (value instanceof Boolean) { return ((Boolean) value).booleanValue(); } return true; }
    private Object[] resolveTableAndKey(String varName, Hashtable scope) throws Exception {
        Object table = unwrap(scope.get(varName));
        if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
        Object key = null;
    
        // Suporte a encadeamento t.a.b
        while (peek().type == DOT || peek().type == LBRACKET) {
            if (peek().type == DOT) {
                consume(DOT);
                Token field = consume(IDENTIFIER);
                key = field.value;
            } else if (peek().type == LBRACKET) {
                consume(LBRACKET);
                key = expression(scope);
                consume(RBRACKET);
            }
            // Se table é null, erro igual Lua
            if (table == null) throw new Exception("attempt to index a nil value");
            if (!(table instanceof Hashtable)) throw new Exception("attempt to index a non-table value");
            if (peek().type == DOT || peek().type == LBRACKET) { table = unwrap(((Hashtable)table).get(key)); }
        }
        return new Object[]{table, key};
    }
    
    private static boolean isWhitespace(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r'; }
    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private static boolean isLetterOrDigit(char c) { return isLetter(c) || isDigit(c); }

    public class LuaFunction {
        private Vector params, bodyTokens;
        private Hashtable closureScope;
        private int MOD = -1;
 
        LuaFunction(Vector params, Vector bodyTokens, Hashtable closureScope) { this.params = params; this.bodyTokens = bodyTokens; this.closureScope = closureScope; }
        LuaFunction(int type) { MOD = type; }

        public Object call(Vector args) throws Exception {
            if (MOD != -1) { return internals(args); }

            Hashtable functionScope = new Hashtable();
            // Inherit from closure scope (simple lexical scoping) and globals
            for (Enumeration e = closureScope.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); functionScope.put(key, unwrap(closureScope.get(key))); }
            for (Enumeration e = globals.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); if (!functionScope.containsKey(key)) { functionScope.put(key, unwrap(globals.get(key))); } }

            // Bind arguments to parameters
            int paramCount = params.size();
            boolean hasVararg = paramCount > 0 && params.elementAt(paramCount - 1).equals("...");
            int fixedParamCount = hasVararg ? paramCount - 1 : paramCount;
            for (int i = 0; i < fixedParamCount; i++) {
                String paramName = (String) params.elementAt(i);
                Object argValue = (i < args.size()) ? args.elementAt(i) : null; // Default to nil if no arg
                functionScope.put(paramName, argValue == null ? LUA_NIL : argValue);
            }
            if (hasVararg) {
                Hashtable varargValues = new Hashtable();
                int index = 1;
                for (int i = fixedParamCount; i < args.size(); i++) { Object obj = args.elementAt(i); varargValues.put(new Double(index++), obj == null ? LUA_NIL : obj); }
                functionScope.put("...", varargValues);
            }

            // Save current token state
            int originalTokenIndex = tokenIndex;
            Vector originalTokens = tokens;

            // Set tokens to function body
            tokens = bodyTokens;
            tokenIndex = 0;

            Object returnValue = null;
            while (peek().type != EOF) {
                Object result = statement(functionScope);

                if (peek().type == EOF && result != null) { returnValue = result; break; } 
                else if (result != null && doreturn) { returnValue = result; doreturn = false; break; }
            }

            // Restore original token state
            tokenIndex = originalTokenIndex;
            tokens = originalTokens;

            return returnValue;
        }
        public Object internals(Vector args) throws Exception {
            if (MOD == PRINT || MOD == EXEC || MOD == GC) { if (args.isEmpty()) { } else { return midlet.processCommand(MOD == GC ? "gc" : (MOD == PRINT ? "echo " : "") + toLuaString(args.elementAt(0)), true, root); } }
            else if (MOD == ERROR) { String msg = toLuaString((args.size() > 0) ? args.elementAt(0) : null); throw new Exception(msg.equals("nil") ? "error" : msg); } 
            else if (MOD == PCALL) {
                Vector result = new Vector();

                if (args.size() == 0) {
                    result.addElement(Boolean.FALSE);
                    result.addElement("Function expected for pcall");
                    return result;
                }

                Object rawFunc = args.elementAt(0);
                // Tolerância: se foi passado o nome da função (String), procurar em globals
                Object funcObj = unwrap(rawFunc);
                if (!(funcObj instanceof LuaFunction)) {
                    if (funcObj instanceof String) {
                        String fname = (String) funcObj;
                        // procurar em globals por esse nome (se existir)
                        if (globals.containsKey(fname)) {
                            funcObj = unwrap(globals.get(fname)); // unwrap (pode ser LUA_NIL internamente)
                        }
                    }
                }

                // Ainda não é função? talvez seja uma tabela contendo função no campo (não tratado aqui).
                if (!(funcObj instanceof LuaFunction)) {
                    result.addElement(Boolean.FALSE);
                    result.addElement("Function expected for pcall");
                    return result;
                }

                LuaFunction func = (LuaFunction) funcObj;

                try {
                    Vector fnArgs = new Vector();
                    for (int i = 1; i < args.size(); i++) { fnArgs.addElement(unwrap(args.elementAt(i))); }

                    Object value = func.call(fnArgs);
                    result.addElement(Boolean.TRUE);

                    result.addElement(value);
                } catch (Exception e) {
                    result.addElement(Boolean.FALSE);
                    result.addElement(e.getMessage());
                }
                return result;
            }
            else if (MOD == GETENV) { if (args.isEmpty()) { return gotbad(1, "getenv", "string expected, got no value"); } else { String key = toLuaString(args.elementAt(0)); return midlet.attributes.containsKey(key) ? midlet.attributes.get(key) : null; } }
            else if (MOD == REQUIRE) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "require", "string expected, got no value"); }

                String name = toLuaString(args.elementAt(0));
                if (name == null || name.equals("nil") || name.length() == 0) { return gotbad(1, "require", "string expected, got " + type(args.elementAt(0))); }

                // Cache simples (estilo package.loaded)
                Object cached = requireCache.get(name);
                if (cached != null) { return (cached == LUA_NIL) ? null : cached; }

                String code = midlet.getcontent(name);
                if (code.equals("")) { throw new Exception("module not found: " + name); }

                Object obj = exec(code);
                requireCache.put(name, (obj == null) ? LUA_NIL : obj);
                return obj;
            }
            else if (MOD == LOADS) { if (args.isEmpty() || args.elementAt(0) == null) { } else { return exec(toLuaString(args.elementAt(0))); } }
            else if (MOD == CLOCK) { return System.currentTimeMillis() - uptime; }
            else if (MOD == SETLOC) { if (args.isEmpty()) { } else { midlet.attributes.put("LOCALE", toLuaString(args.elementAt(0))); } }
            else if (MOD == PAIRS) { if (args.isEmpty()) { throw new Exception("pairs: table expected"); } Object t = args.elementAt(0); t = (t == LUA_NIL) ? null : t; if (t == null || t instanceof Hashtable || t instanceof Vector) { return t; } throw new Exception("pairs: table expected"); }
            else if (MOD == EXIT) { if (args.isEmpty()) { throw new Error(); } else { status = midlet.getNumber(toLuaString(args.elementAt(0)), 1, false); } }
            else if (MOD == READ) {
                if (args.isEmpty()) { return midlet.stdout.getText(); }
                else {
                    Object arg = args.elementAt(0);

                    if (arg instanceof InputStream) {
                        InputStream IN = (InputStream) arg;

                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int read;
                        while ((read = IN.read(buffer)) != -1) {
                            baos.write(buffer, 0, read);
                            if (IN.available() == 0) { break; }
                        }
                        return new String(baos.toByteArray(), "UTF-8");
                    } 
                    else if (arg instanceof OutputStream) { return gotbad(1, "read", "input stream expected, got output"); } 
                    else {
                        String target = toLuaString(arg);
                        return target.equals("stdout") ? midlet.stdout.getText() : target.equals("stdin") ? midlet.stdin.getString() : midlet.getcontent(target);
                    }
                }
            }
            else if (MOD == WRITE) { 
                if (args.isEmpty()) { }
                else {
                    String content = toLuaString(args.elementAt(0)), out = args.size() == 1 ? "stdout" : toLuaString(args.elementAt(1));
                    boolean mode = args.size() > 2 && toLuaString(args.elementAt(2)).equals("a") ? true : false;

                    if (args.size() > 1 && args.elementAt(1) instanceof OutputStream) {
                        OutputStream out = (OutputStream) args.elementAt(1);

                        out.write(content.getBytes("UTF-8")); out.flush();
                    }
                    else if (args.size() > 1 && args.elementAt(1) instanceof InputStream) { return gotbad(2, "write", "output stream expected, got input"); }  
                    else {
                        if (out.equals("stdout")) { midlet.stdout.setText(mode ? midlet.stdout.getText() + content : content); }
                        else if (out.equals("stdin")) { midlet.stdin.setString(mode ? midlet.stdin.getString() + content : content); }
                        else { return midlet.writeRMS(out, mode ? midlet.getcontent(out) + content : content); }
                    }
                }
            }
            else if (MOD == CLOSE) {
                if (args.isEmpty()) { }
                else {
                    for (int i = 0; i < args.size(); i++) {
                        Object arg = args.elementAt(i);

                        if (arg instanceof StreamConnection) { ((StreamConnection) arg).close(); }
                        else if (arg instanceof InputStream) { ((InputStream) arg).close(); }
                        else if (arg instanceof OutputStream) { ((OutputStream) arg).close(); }
                        else { gotbad(i + 1, "close", "stream expected, got " + type(arg)); }
                    }
                } 
            }
            else if (MOD == TOSTRING) { return toLuaString(args.isEmpty() ? null : args.elementAt(0)); }
            else if (MOD == TONUMBER) { return args.isEmpty() ? null : new Double(Double.valueOf(toLuaString(args.elementAt(0)))); }
            else if (MOD == LOWER || MOD == UPPER) { if (args.isEmpty()) { return gotbad(1, MOD == LOWER ? "lower" : "upper", "string expected, got no value"); } else { String text = toLuaString(args.elementAt(0)); return MOD == LOWER ? text.toLowerCase() : text.toUpperCase(); } }
            else if (MOD == MATCH || MOD == LEN) {
                if (args.isEmpty()) { }
                else {
                    Object obj = args.elementAt(0);
                    String text = toLuaString(obj), pattern = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;
                
                    if (MOD == LEN) {
                        if (obj == null) { }
                        else if (obj instanceof String) { return new Double(text.length()); } 
                        else { throw new RuntimeException("string.len expected a string"); }
                    }

                    if (args.elementAt(0) == null || pattern == null) { }
                    else {
                        int pos = text.indexOf(pattern);

                        if (pos == -1) { }
                        else { return new Double(pos + 1); }
                    }
                }
            }
            else if (MOD == REVERSE) { if (args.isEmpty()) { return gotbad(1, "reverse", "string expected, got no value"); } else { StringBuffer sb = new StringBuffer(toLuaString(args.elementAt(0))); return sb.reverse().toString(); } }
            else if (MOD == SUB) {
                if (args.isEmpty()) { return gotbad(1, "sub", "string expected, got no value"); }
                else {
                    String text = toLuaString(args.elementAt(0));

                    if (args.elementAt(0) == null) { }
                    else {
                        if (args.size() == 1) { return text; }

                        int len = text.length(), start = midlet.getNumber(toLuaString(args.elementAt(1)), 1, false), end = args.size() > 2 ? midlet.getNumber(toLuaString(args.elementAt(2)), len, false) : len;

                        if (start < 0) { start = len + start + 1; }
                        if (end < 0) { end = len + end + 1; }

                        if (start < 1) { start = 1; }
                        if (end > len) { end = len; }

                        if (start > end || start > len) { return ""; }

                        int jBegin = start - 1;

                        return text.substring(jBegin < 0 ? 0 : jBegin, end);
                    }
                }
            }
            else if (MOD == RANDOM) { Double gen = new Double(midlet.random.nextInt(midlet.getNumber(args.isEmpty() ? "100" : toLuaString(args.elementAt(0)), 100, false))); return args.isEmpty() ? new Double(gen.doubleValue() / 100) : gen; }
            else if (MOD == HASH) { return args.isEmpty() || args.elementAt(0) == null ? null : new Double(args.elementAt(0).hashCode()); }
            else if (MOD == TYPE) { if (args.isEmpty()) { return gotbad(1, "type", "value expected"); } else { return type(args.elementAt(0)); } }
            else if (MOD == BYTE) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "byte", "string expected, got no value"); }
                else {
                    String s = toLuaString(args.elementAt(0));
                    int len = s.length(), start = 1, end = 1;
                    if (args.size() >= 2) { start = midlet.getNumber(toLuaString(args.elementAt(1)), 1, false); }
                    if (args.size() >= 3) { end = midlet.getNumber(toLuaString(args.elementAt(2)), start, false); }
                    
                    if (start < 0) { start = len + start + 1; }
                    if (end < 0) { end = len + end + 1; }
                    if (start < 1) { start = 1; }
                    if (end > len) { end = len; } 
                    if (start > end || start > len) { return null; }
                    
                    if (end - start + 1 == 1) { return new Double((double) s.charAt(start - 1)); } 
                    else {
                        Hashtable result = new Hashtable();
                        for (int i = start; i <= end; i++) { result.put(new Double(i), new Double((double) s.charAt(i - 1))); }

                        return result;
                    }
                }
            }
            else if (MOD == CHAR) {
                if (args.isEmpty()) { return ""; } 
                else {
                    Object firstArg = args.elementAt(0);
                    
                    if (firstArg instanceof Hashtable) {
                        Hashtable table = (Hashtable) firstArg;
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i <= table.size(); i++) {
                            Object arg = table.get(new Double(i + 1));
                            if (arg == null) { continue; }
                            double num;
                            if (arg instanceof Double) { num = ((Double) arg).doubleValue(); } 
                            else { return gotbad(1, "char", "value out of range"); }
                            int c = (int) num;
                            if (c < 0 || c > 255) { return gotbad(1, "char", "value out of range"); }
                            sb.append((char) c);
                        }
                        return sb.toString();
                    } else {
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i < args.size(); i++) {
                            Object arg = args.elementAt(i);
                            if (arg == null) { return gotbad(1, "char", "number expected, got nil"); }
                            double num;
                            if (arg instanceof Double) { num = ((Double) arg).doubleValue(); } 
                            else {
                                try { num = Double.parseDouble(toLuaString(arg)); } 
                                catch (Exception e) { return gotbad(1, "char", "number expected, got " + type(arg)); }
                            }
                            int c = (int) num;
                            if (c < 0 || c > 255) { return gotbad(1, "char", "value out of range"); }
                            sb.append((char) c);
                        }
                        return sb.toString();
                    }
                }
            }
            else if (MOD == SELECT) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "select", "number expected, got no value"); } 
                else {
                    String idx = toLuaString(args.elementAt(0));
                    if (idx.equals("#")) {
                        if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) { return new Double(((Hashtable) args.elementAt(1)).size()); } 
                        else { return new Double(args.size() - 1); }
                    } else {
                        if (args.size() == 1) { return null; }

                        int index = 1;
                        try { index = Integer.parseInt(idx); } 
                        catch (NumberFormatException e) { return gotbad(1, "select", "number expected, got " + type(args.elementAt(0))); }
                        
                        Hashtable result = new Hashtable();
                        if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) {
                            Hashtable varargTable = (Hashtable) args.elementAt(1);
                            int varargSize = varargTable.size();
                            if (index < 0) { index = varargSize + index + 1; }
                            if (index < 1 || index > varargSize) { return null; }

                            int resultIndex = 1;
                            for (int i = index; i <= varargSize; i++) {
                                Object val = varargTable.get(new Double(i));
                                if (val != null) { result.put(new Double(resultIndex++), val); }
                            }
                        } else {
                            int argCount = args.size() - 1;
                            if (index < 0) { index = argCount + index + 1; }
                            if (index < 1 || index > argCount) { return null; }

                            int resultIndex = 1;
                            for (int i = index; i <= argCount; i++) {
                                Object val = args.elementAt(i);
                                result.put(new Double(resultIndex++), val == null ? LUA_NIL : val);
                            }
                        }
                        return result;
                    }
                }
            }
            else if (MOD == TB_DECODE) { return args.isEmpty() ? null : midlet.parseProperties((String) args.elementAt(0)); }
            else if (MOD == TB_PACK) {
                Hashtable packed = new Hashtable();
                for (int i = 0; i < args.size(); i++) {
                    Object val = args.elementAt(i);
                    packed.put(new Double(i + 1), val == null ? LUA_NIL : val);
                }
                packed.put("n", new Double(args.size()));
                return packed;
            }
            else if (MOD == CONNECT) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "connect", "string expected, got no value"); }
                Vector result = new Vector();

                StreamConnection conn = (StreamConnection) Connector.open(toLuaString(args.elementAt(0)));
                    
                result.addElement(conn);
                result.addElement(conn.openInputStream());
                result.addElement(conn.openOutputStream());

                return result;
            }
            else if (MOD == SERVER) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "server", "number expected, got no value"); }
                int port;

                try { port = ((Double) args.elementAt(0)).intValue(); }
                catch (Exception e) { return gotbad(1, "server", "number expected, got " + type(args.elementAt(0))); }
                
                ServerSocketConnection server = (ServerSocketConnection) Connector.open("socket://:" + port);
                return server;
            }
            else if (MOD == ACCEPT) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "accept", "server expected, got no value"); }

                Object serverObj = args.elementAt(0);
                if (!(serverObj instanceof ServerSocketConnection)) { return gotbad(1, "accept", "server expected, got " + type(serverObj)); }

                SocketConnection conn = (SocketConnection) ((ServerSocketConnection) serverObj).acceptAndOpen();
                Vector result = new Vector();

                result.addElement(conn);
                result.addElement(conn.openInputStream());
                result.addElement(conn.openOutputStream());
                
                return result;
            }
            else if (MOD == HTTP_GET || MOD == HTTP_POST) { 
                return args.isEmpty() || args.elementAt(0) == null ? 
                    gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : 
                    request(
                        MOD == HTTP_GET ? "GET" : "POST", 
                        toLuaString(args.elementAt(0)), 
                        MOD == HTTP_GET ? null : args.size() > 1 ? toLuaString(args.elementAt(1)) : "", 
                        args.size() > (MOD == HTTP_GET ? 1 : 2) ? args.elementAt(MOD == HTTP_GET ? 1 : 2) : null
                    ); 
            }

            return null;
        }

        private Object exec(String code) throws Exception { int savedIndex = tokenIndex; Vector savedTokens = tokens; Object ret = null; try { tokens = tokenize(code); tokenIndex = 0; Hashtable modScope = new Hashtable(); for (Enumeration e = globals.keys(); e.hasMoreElements();) { String k = (String) e.nextElement(); modScope.put(k, unwrap(globals.get(k))); } while (peek().type != EOF) { Object res = statement(modScope); if (res != null && doreturn) { ret = res; doreturn = false; break; } } } finally { tokenIndex = savedIndex; tokens = savedTokens; } return ret; }
        private String type(Object item) throws Exception { return item == null || item == LUA_NIL ? "nil" : item instanceof String ? "string" : item instanceof Double ? "number" : item instanceof Boolean ? "boolean" : item instanceof LuaFunction ? "function" : item instanceof Hashtable ? "table" : item instanceof StreamConnection || item instanceof InputStream || item instanceof OutputStream ? "stream" : "userdata"; }
        private Object gotbad(int pos, String name, String expect) throws Exception { throw new RuntimeException("bad argument #" + pos + " to '" + name + "' (" + expect + ")"); }
        private String request(String method, String url, String data, Object item) throws Exception {
            if (url == null || url.length() == 0) { return ""; }
            if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; }
        
            HttpConnection conn = null;
            Hashtable headers = (Hashtable) item;
            InputStream is = null;
            ByteArrayOutputStream baos = null;

            try {
                conn = (HttpConnection) Connector.open(url);
                conn.setRequestMethod(method.toUpperCase());

                if (headers != null) {
                    Enumeration keys = headers.keys();
                    while (keys.hasMoreElements()) {
                        String key = (String) keys.nextElement();
                        conn.setRequestProperty(key, (String) headers.get(key));
                    }                  
                }

                if ("POST".equalsIgnoreCase(method)) {
                    byte[] postBytes = (data == null) ? new byte[0] : data.getBytes("UTF-8");

                    if (headers == null || headers.get("Content-Type") == null) { conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); }
                    if (headers == null || headers.get("Content-Length") == null) { conn.setRequestProperty("Content-Length", Integer.toString(postBytes.length)); }

                    OutputStream os = conn.openOutputStream();
                    os.write(postBytes);
                    os.flush(); os.close();
                }

                is = conn.openInputStream();
                baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = is.read()) != -1) { baos.write(ch); }

                return new String(baos.toByteArray(), "UTF-8");

            } 
            catch (Exception e) { throw e; } 
            finally {
                if (is != null) { try { is.close(); } catch (Exception e) { } }
                if (conn != null) { try { conn.close(); } catch (Exception e) { } }
                if (baos != null) { try { baos.close(); } catch (Exception e) { } }
            }
        }
    }
}
// |
// EOF