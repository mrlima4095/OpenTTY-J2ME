import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.control.*;
import javax.microedition.io.file.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import javax.bluetooh.*;
import java.util.*;
import java.io.*;


public class OpenTTY extends MIDlet implements CommandListener {
    private int cursorX = 10, cursorY = 10;
    private Player player = null;
    private Random random = new Random();
    private Runtime runtime = Runtime.getRuntime();
    private Hashtable attributes = new Hashtable(), aliases = new Hashtable(), shell = new Hashtable(), functions = new Hashtable(), 
                      paths = new Hashtable(), desktops = new Hashtable(), trace = new Hashtable();
    private Vector stack = new Vector(), history = new Vector(), sessions = new Vector();
    private String username = loadRMS("OpenRMS"), nanoContent = loadRMS("nano");
    private String logs = "", path = "/home/", build = "2025-1.15-02x12"; 
    private Display display = Display.getDisplay(this);
    private Form form = new Form("OpenTTY " + getAppProperty("MIDlet-Version"));
    private TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    private StringItem stdout = new StringItem("", "Welcome to OpenTTY " + getAppProperty("MIDlet-Version") + "\nCopyright (C) 2025 - Mr. Lima\n");
    private Command EXECUTE = new Command("Send", Command.OK, 1), HELP = new Command("Help", Command.SCREEN, 2), NANO = new Command("Nano", Command.SCREEN, 3),
                    CLEAR = new Command("Clear", Command.SCREEN, 4), HISTORY = new Command("History", Command.SCREEN, 5);

    public void startApp() {
        if (!trace.containsKey("sh")) {
            attributes.put("PATCH", "Hidden Void"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.6.2");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));

            runScript(read("/java/etc/initd.sh")); stdin.setLabel(username + " " + path + " $");

            if (username.equals("")) { new Login(); }
            else { runScript(read("/home/initd")); }
        } 
    }

    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { writeRMS("/home/nano", nanoContent); }

    public void commandAction(Command c, Displayable d) {
        if (c == EXECUTE) { String command = stdin.getString().trim(); if (!command.equals("") && !command.startsWith("!!")) { history.addElement(command.trim()); } stdin.setString(""); processCommand(command); stdin.setLabel(username + " " + path + " $"); } 

        else if (c == HELP) { processCommand("help"); }
        else if (c == NANO) { new NanoEditor(""); }
        else if (c == CLEAR) { stdout.setText(""); }
        else if (c == HISTORY) { new History(); }

        else if (c.getCommandType() == Command.BACK) { processCommand("xterm"); }
        else if (c.getCommandType() == Command.EXIT) { processCommand("exit"); }
    }

    // OpenTTY Command Processor
    private int processCommand(String command) { return processCommand(command, true); }
    private int processCommand(String command, boolean ignore) { 
        command = command.startsWith("exec") ? command.trim() : env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);

        if (shell.containsKey(mainCommand) && ignore) { 
            Hashtable args = (Hashtable) shell.get(mainCommand); 
            if (argument.equals("")) { return processCommand(aliases.containsKey(mainCommand) ? (String) aliases.get(mainCommand) : "true"); } 
            else if (args.containsKey(getCommand(argument).toLowerCase())) { return processCommand((String) args.get(getCommand(argument)) + " " + getArgument(argument)); } 
            else { return processCommand(args.containsKey("shell.unknown") ? (String) args.get(getCommand("shell.unknown")) + " " + getArgument(argument) : "echo " + mainCommand + ": " + getCommand(argument) + ": not found", args.containsKey("shell.unknown") ? true : false); } }
        if (aliases.containsKey(mainCommand) && ignore) { return processCommand((String) aliases.get(mainCommand) + " " + argument); }
        if (functions.containsKey(mainCommand) && ignore) { return runScript((String) functions.get(mainCommand)); }
        
        if (mainCommand.equals("")) { }

        // API 001 - (Registry)
        // |
        // Aliases
        else if (mainCommand.equals("alias")) { if (argument.equals("")) { Enumeration KEYS = aliases.keys(); while (KEYS.hasMoreElements()) { String KEY = (String) KEYS.nextElement(), VALUE = (String) aliases.get(KEY); if (!KEY.equals("xterm") && !VALUE.equals("")) { echoCommand("alias " + KEY + "='" + VALUE.trim() + "'"); } } } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { if (aliases.containsKey(argument)) { echoCommand("alias " + argument + "='" + (String) aliases.get(argument) + "'"); } else { echoCommand("alias: " + argument + ": not found"); return 127; } } else { aliases.put(argument.substring(0, INDEX).trim(), argument.substring(INDEX + 1).trim()); } } }  
        else if (mainCommand.equals("unalias")) { if (argument.equals("")) { } else if (aliases.containsKey(argument)) { aliases.remove(argument); } else { echoCommand("unalias: " + argument + ": not found"); return 127; } }
        // |
        // Environment Keys
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { int INDEX = argument.indexOf('='); if (INDEX == -1) { attributes.put(argument, ""); } else { attributes.put(argument.substring(0, INDEX).trim(), argument.substring(INDEX + 1).trim()); } } } 
        else if (mainCommand.equals("unset")) { if (argument.equals("")) { } else if (attributes.containsKey(argument)) { attributes.remove(argument); } else { } }
        else if (mainCommand.equals("export")) { return processCommand(argument.equals("") ? "env" : "set " + argument, false); }
        else if (mainCommand.equals("env")) { if (argument.equals("")) { Enumeration KEYS = attributes.keys(); while (KEYS.hasMoreElements()) { String KEY = (String) KEYS.nextElement(), VALUE = (String) aliases.get(KEY); if (!KEY.equals("OUTPUT") && !VALUE.equals("")) { echoCommand(KEY + "=" + VALUE.trim()); } } } else if (attributes.containsKey(argument)) { echoCommand(argument + "=" + (String) attributes.get(argument)); } else { echoCommand("env: " + argument + ": not found"); return 127; } }

        // API 002 - (Logs)
        // |
        // OpenTTY Logging Manager
        else if (mainCommand.equals("log")) { return MIDletLogs(argument); }
        else if (mainCommand.equals("logcat")) { echoCommand(logs); }

        // API 003 - (User-Integration)
        // |
        // Session
        else if (mainCommand.equals("logout")) { writeRMS("/home/OpenRMS", ""); processCommand("exit"); }
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { echoCommand(username); }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { return processCommand("import /java/bin/sh", false); }

        // API 004 - (LCDUI Interface)
        // |
        // System UI
        else if (mainCommand.equals("x11")) { return xserver(argument); }
        else if (mainCommand.equals("xterm")) { display.setCurrent(form); }
        else if (mainCommand.equals("gauge")) { return xserver("gauge " + argument); }
        else if (mainCommand.equals("warn")) { return warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("title")) { form.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument); }
        else if (mainCommand.equals("tick")) { if (argument.equals("label")) { echoCommand(display.getCurrent().getTicker().getString()); } else { return xserver("tick " + argument); } }

        // API 005 - (Operators)
        // |
        // Operators
        else if (mainCommand.equals("for")) { return forCommand(argument); }
        else if (mainCommand.equals("if")) { return ifCommand(argument); } else if (mainCommand.equals("case")) { return caseCommand(argument); }
        // |
        // Long executors
        else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { return processCommand(argument, false); }
        else if (mainCommand.equals("bruteforce")) { start("bruteforce"); while (trace.containsKey("bruteforce")) { int STATUS = processCommand(argument, ignore); if (STATUS != 0) { stop("bruteforce"); return STATUS; } } }
        else if (mainCommand.equals("cron")) { if (argument.equals("")) { } else { return processCommand("execute sleep " + getCommand(argument) + "; " + getArgument(argument)); } }
        else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (InterruptedException e) { } catch (NumberFormatException e) { echoCommand(e.getMessage()); return 2; } } }
        else if (mainCommand.equals("time")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); int STATUS = processCommand(argument, ignore); echoCommand("at " + (System.currentTimeMillis() - START) + "ms"); return STATUS; } } 
        // |
        // Chain executors
        else if (mainCommand.equals("exec")) { String[] CMDS = split(argument, '&'); for (int i = 0; i < CMDS.length; i++) { processCommand(CMDS[i].trim(), ignore); } }
        else if (mainCommand.equals("execute")) { String[] CMDS = split(argument, ';'); for (int i = 0; i < CMDS.length; i++) { processCommand(CMDS[i].trim(), ignore); } }

        // API 006 - (Process)
        // |
        // Memory
        else if (mainCommand.equals("gc")) { System.gc(); } else if (mainCommand.equals("htop")) { new HTopViewer(argument); }
        else if (mainCommand.equals("top")) { if (argument.equals("")) { new HTopViewer("monitor"); } else if (argument.equals("used")) { echoCommand("" + (runtime.totalMemory() - runtime.freeMemory()) / 1024); } else if (argument.equals("free")) { echoCommand("" + runtime.freeMemory() / 1024); } else if (argument.equals("total")) { echoCommand("" + runtime.totalMemory() / 1024); } else { echoCommand("top: " + getCommand(argument) + ": not found"); return 127; } }
        // |
        // Process
        else if (mainCommand.equals("start")) { start(argument); } else if (mainCommand.equals("kill")) { kill(argument); } else if (mainCommand.equals("stop")) { stop(argument); }
        else if (mainCommand.equals("ps")) { echoCommand("PID\tPROCESS"); Enumeration KEYS = trace.keys(); while (KEYS.hasMoreElements()) { String KEY = (String) KEYS.nextElement(), PID = (String) trace.get(KEY); echoCommand(PID + "\t" + KEY); } }
        else if (mainCommand.equals("trace")) { if (argument.equals("")) { } else if (getCommand(argument).equals("pid")) { echoCommand(trace.containsKey(getArgument(argument)) ? (String) trace.get(getArgument(argument)) : "null"); } else if (getCommand(argument).equals("check")) { echoCommand(trace.containsKey(getArgument(argument)) ? "true" : "false"); } else { echoCommand("trace: " + getCommand(argument) + ": not found"); return 127; } }

        // API 007 - (Bundle)
        // |
        // Properties
        else if (mainCommand.equals("pkg")) { echoCommand(argument.equals("") ? getAppProperty("MIDlet-Name") : argument.startsWith("/") ? System.getProperty(replace(argument, "/", "")) : getAppProperty(argument)); }
        else if (mainCommand.equals("uname")) { String INFO = ""; if (argument.equals("") || argument.equals("-i")) { INFO = "$TYPE"; } else if (argument.equals("-a") || argument.equals("--all")) { INFO = "$TYPE (OpenTTY $VERSION) main/$RELEASE " + build + " - $CONFIG $PROFILE"; } else if (argument.equals("-r") || argument.equals("--release")) { INFO = "$VERSION"; } else if (argument.equals("-v") || argument.equals("--build")) { INFO = build; } else if (argument.equals("-s") ) { INFO = "J2ME"; } else if (argument.equals("-m")) { INFO = "$PROFILE"; } else if (argument.equals("-p")) { INFO = "$CONFIG"; } else if (argument.equals("-n")) { INFO = "$HOSTNAME"; } else { echoCommand("uname: " + argument + ": not found"); return 127; } echoCommand(env(INFO)); }
        // |
        // Device ID
        else if (mainCommand.equals("hostname")) { processCommand(argument.equals("") ? "echo $HOSTNAME" : "set HOSTNAME=" + getCommand(argument), false); }
        else if (mainCommand.equals("hostid")) { String DATA = System.getProperty("microedition.platform") + System.getProperty("microedition.configuration") + System.getProperty("microedition.profiles"); int HASH = 7; for (int i = 0; i < DATA.length(); i++) { HASH = HASH * 31 + DATA.charAt(i); } echoCommand(Integer.toHexString(HASH).toLowerCase()); }

        // API 008 - (Logic I/O) Text
        // |
        // TTY
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(stdout.getText().length() + " B"); }
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
        else if (mainCommand.equals("clear")) { if (argument.equals("") || argument.equals("stdout")) { stdout.setText(""); } else if (argument.equals("stdin")) { stdin.setString(""); } else if (argument.equals("history")) { history = new Vector(); } else if (argument.equals("logs")) { logs = ""; } else { echoCommand("clear: " + argument + ": not found"); return 127; } }
        else if (mainCommand.equals("seed")) { try { echoCommand("" +  random.nextInt(Integer.parseInt(argument)) + ""); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return 2; } }

        // API 009 - (Threads)
        // |
        // MIDlet Tracker
        else if (mainCommand.equals("throw")) { Thread.currentThread().interrupt(); }
        else if (mainCommand.equals("mmspt")) { echoCommand(replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet")); }
        else if (mainCommand.equals("bg")) { final String bgCommand = argument; new Thread("Background") { public void run() { processCommand(bgCommand); } }.start(); }

        // API 010 - (Requests)
        // |
        // Connecting to Device API
        else if (mainCommand.equals("call")) { if (argument.equals("")) { } else { try { platformRequest("tel:" + argument); } catch (Exception e) { } } }
        else if (mainCommand.equals("open")) { if (argument.equals("")) { } else { try { platformRequest(argument); } catch (Exception e) { echoCommand("open: " + argument + ": not found"); return 127; } } }
        // |
        // PushRegistry
        else if (mainCommand.equals("prg")) { if (argument.equals("")) { argument = "5"; } try { PushRegistry.registerAlarm(getArgument(argument).equals("") ? "OpenTTY" : getArgument(argument), System.currentTimeMillis() + Integer.parseInt(getCommand(argument)) * 1000); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return 2; } catch (ClassNotFoundException e) { echoCommand("prg: " + getArgument(argument) + ": not found"); return 127; } catch (Exception e) { echoCommand("AutoRunError: " + e.getMessage()); return 3; } }

        // API 011 - (Network)
        // |
        // Servers
        else if (mainCommand.equals("bind")) { new Bind(argument.equals("") ? env("$PORT") : argument); }
        else if (mainCommand.equals("server")) { new Server(env("$PORT $RESPONSE")); }
        // |
        // HTTP Interfaces
        else if (mainCommand.equals("gobuster")) { new GoBuster(argument); }
        else if (mainCommand.equals("pong")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); echoCommand("Pong to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms"); CONN.close(); } catch (IOException e) { echoCommand("Pong to " + argument + " failed: " + e.getMessage()); return 101; } } }
        else if (mainCommand.equals("ping")) { if (argument.equals("")) { } else { long START = System.currentTimeMillis(); try { HttpConnection CONN = (HttpConnection) Connector.open(!argument.startsWith("http://") && !argument.startsWith("https://") ? argument = "http://" + argument : argument); CONN.setRequestMethod(HttpConnection.GET); int responseCode = CONN.getResponseCode(); CONN.close(); echoCommand("Ping to " + argument + " successful, time=" + (System.currentTimeMillis() - START) + "ms"); } catch (IOException e) { echoCommand("Ping to " + argument + " failed: " + e.getMessage()); return 101; } } }
        else if (mainCommand.equals("curl") || mainCommand.equals("wget") || mainCommand.equals("clone") || mainCommand.equals("proxy")) { if (argument.equals("")) { } else { String URL = getCommand(argument); if (mainCommand.equals("clone") || mainCommand.equals("proxy")) { URL = getAppProperty("MIDlet-Proxy") + URL; } Hashtable HEADERS = getArgument(argument).equals("") ? null : parseProperties(getcontent(getArgument(argument))); String RESPONSE = request(URL, HEADERS); if (mainCommand.equals("curl")) { echoCommand(RESPONSE); } else if (mainCommand.equals("wget") || mainCommand.equals("proxy")) { nanoContent = RESPONSE; } else if (mainCommand.equals("clone")) { return runScript(RESPONSE); } } }
        // |
        // Socket Interfaces
        else if (mainCommand.equals("query")) { return query(argument); }
        else if (mainCommand.equals("prscan")) { new PortScanner(argument); }
        else if (mainCommand.equals("gaddr")) { return GetAddress(argument); }
        else if (mainCommand.equals("nc")) { new RemoteConnection(argument); }
        // |
        else if (mainCommand.equals("wrl")) { echoCommand(System.getProperty("wireless.messaging.sms.smsc")); }
        else if (mainCommand.equals("who")) { StringBuffer SESSIONS = new StringBuffer(); for (int i = 0; i < sessions.size(); i++) { SESSIONS.append((String) sessions.elementAt(i)).append("\n"); } echoCommand(SESSIONS.toString().trim()); }
        // |
        // IP Tools
        else if (mainCommand.equals("fw")) { echoCommand(request("http://ipinfo.io/" + (argument.equals("") ? "json" : argument))); }
        else if (mainCommand.equals("genip")) { echoCommand(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256)); }
        else if (mainCommand.equals("ifconfig")) { if (argument.equals("")) { argument = "1.1.1.1:53"; } try { SocketConnection CONN = (SocketConnection) Connector.open("socket://" + argument); echoCommand(CONN.getLocalAddress()); CONN.close(); } catch (IOException e) { echoCommand("null"); return 101; } }
        // |
        else if (mainCommand.equals("report")) { processCommand("open mailto:felipebr4095@gmail.com"); }
        else if (mainCommand.equals("mail")) { echoCommand(request(getAppProperty("MIDlet-Proxy") + "raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/root/mail.txt")); } 
        else if (mainCommand.equals("netstat")) { int STATUS = 0; try { HttpConnection CONN = (HttpConnection) Connector.open("http://ipinfo.io/ip"); CONN.setRequestMethod(HttpConnection.GET); if (CONN.getResponseCode() == HttpConnection.HTTP_OK) { } else { STATUS = 101; } CONN.close(); } catch (Exception e) { STATUS = 101; } echoCommand(STATUS == 0 ? "true" : "false"); return STATUS; }

        // API 012 - (File)
        // |
        // Directories Manager
        else if (mainCommand.equals("dir")) { new Explorer(); }
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("umount")) { paths = new Hashtable(); }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { mount(getcontent(argument)); } }
        else if (mainCommand.equals("cd")) { 
            if (argument.equals("")) { path = "/home/"; } 
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
                        String REALPWD = "file:///" + TARGET.substring(5); 
                        if (!REALPWD.endsWith("/")) REALPWD += "/"; 

                        FileConnection fc = (FileConnection) Connector.open(REALPWD, Connector.READ); 
                        if (fc.exists() && fc.isDirectory()) { path = TARGET; } 
                        else { echoCommand("cd: " + basename(TARGET) + ": not " + (!fc.exists() ? "found" : "a directory")); return 127; } 

                        fc.close(); 
                    } 
                    catch (IOException e) { echoCommand("cd: " + basename(TARGET) + ": " + e.getMessage()); return 1; } 
                } 
                else { echoCommand("cd: " + basename(TARGET) + ": not accessible"); return 127; } 
            } 
        }
        else if (mainCommand.equals("pushd")) { if (argument.equals("")) { echoCommand(readStack() == null || readStack().length() == 0 ? "pushd: missing directory": readStack()); } else { int STATUS = processCommand("cd " + argument, false); if (STATUS == 0) { stack.addElement(path); echoCommand(readStack()); } return STATUS; } }
        else if (mainCommand.equals("popd")) { if (stack.isEmpty()) { echoCommand("popd: empty stack"); } else { path = (String) stack.lastElement(); stack.removeElementAt(stack.size() - 1); echoCommand(readStack()); } }
        else if (mainCommand.equals("ls")) { 
            Vector BUFFER = new Vector(); 

            if (path.equals("/mnt/")) { 
                try { 
                    Enumeration ROOTS = FileSystemRegistry.listRoots(); 
                    while (ROOTS.hasMoreElements()) { 
                        String ROOT = (String) ROOTS.nextElement(); 
                        if (!BUFFER.contains(ROOT)) { BUFFER.addElement(ROOT); } 
                    } 
                } 
                catch (Exception e) { } 
            } 
            else if (path.startsWith("/mnt/")) { 
                try { 
                    String REALPWD = "file:///" + path.substring(5); 
                    if (!REALPWD.endsWith("/")) { REALPWD += "/"; } 
                    
                    FileConnection CONN = (FileConnection) Connector.open(REALPWD, Connector.READ); 
                    Enumeration CONTENT = CONN.list(); 
                    
                    while (CONTENT.hasMoreElements()) { 
                        String ITEM = (String) CONTENT.nextElement(); 
                        BUFFER.addElement(ITEM); 
                    } 
                    
                    CONN.close(); 
                } 
                catch (Exception e) { } 
            } 
            else if (path.equals("/home/") && argument.indexOf("-v") != -1) { 
                try { 
                    String[] FILES = RecordStore.listRecordStores(); 
                    if (FILES != null) { 
                        for (int i = 0; i < FILES.length; i++) { 
                            String NAME = FILES[i]; 
                            
                            if ((argument.indexOf("-a") != -1 || !NAME.startsWith(".")) && !BUFFER.contains(NAME)) { BUFFER.addElement(NAME); } 
                        } 
                    } 
                } 
                catch (RecordStoreException e) { } 
            } 
            else if (path.equals("/home/")) { new Explorer(); return 0; } 
            
            String[] FILES = (String[]) paths.get(path); 
            
            if (FILES != null) { 
                for (int i = 0; i < FILES.length; i++) { 
                    String f = FILES[i].trim(); 
                    if (f == null || f.equals("..") || f.equals("/")) { continue; } 
                    if (!BUFFER.contains(f) && !BUFFER.contains(f + "/")) { BUFFER.addElement(f); } 
                } 
            } 
            if (!BUFFER.isEmpty()) { 
                StringBuffer FORMATTED = new StringBuffer(); 
                
                for (int i = 0; i < BUFFER.size(); i++) { 
                    String ITEM = (String) BUFFER.elementAt(i); 
                    
                    if (!ITEM.equals("/")) { FORMATTED.append(ITEM).append(path.equals("/home/") ? "\n" : "\t"); } 
                } 
                
                echoCommand(FORMATTED.toString().trim()); 
            } 
        }
        // |
        // Device Files
        else if (mainCommand.equals("fdisk")) { return processCommand("lsblk", false); }
        else if (mainCommand.equals("lsblk")) { if (argument.equals("") || argument.equals("-x")) { echoCommand(replace("MIDlet.RMS.Storage", ".", argument.equals("-x") ? ";" : "\t")); } else { echoCommand("lsblk: " + argument + ": not found"); return 127; } }
        // |
        // RMS Files
        else if (mainCommand.equals("rm")) { if (argument.equals("")) { } else { deleteFile(argument); } }
        else if (mainCommand.equals("install")) { if (argument.equals("")) { } else { writeRMS(argument, nanoContent); } }
        else if (mainCommand.equals("touch")) { if (argument.equals("")) { nanoContent = ""; } else { writeRMS(argument, ""); } }
        else if (mainCommand.equals("mkdir")) { if (argument.equals("")) { } else { argument = argument.endsWith("/") ? argument : argument + "/"; argument = argument.startsWith("/") ? argument : path + argument; if (argument.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.mkdir(); CONN.close(); } else { echoCommand("mkdir: " + basename(argument) + ": found"); } CONN.close(); } catch (SecurityException e) { echoCommand(e.getMessage()); return 13; } catch (IOException e) { echoCommand(e.getMessage()); return 1; } } else if (argument.startsWith("/home/")) { echoCommand("mkdir: 405 Method not allowed"); return 3; } else if (argument.startsWith("/")) { echoCommand("read-only storage"); return 5; } } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { String ORIGIN = getCommand(argument), TARGET = getArgument(argument); writeRMS(TARGET.equals("") ? ORIGIN + "-copy" : TARGET, getcontent(ORIGIN)); } }
        // |
        // Text Manager
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("rraw")) { stdout.setText(nanoContent); }
        else if (mainCommand.equals("sed")) { return StringEditor(argument); }
        else if (mainCommand.equals("getty")) { nanoContent = stdout.getText(); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } 
        else if (mainCommand.equals("du")) { if (argument.equals("")) { } else { processCommand("wc -c " + argument, false); } }
        else if (mainCommand.equals("hash")) { if (argument.equals("")) { } else { echoCommand("" + getcontent(argument).hashCode()); } }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { } else { echoCommand(getcontent(argument)); } }
        else if (mainCommand.equals("get")) { if (argument.equals("") || argument.equals("nano")) { nanoContent = loadRMS("nano"); } else { nanoContent = getcontent(argument); } }
        else if (mainCommand.equals("read")) { if (argument.equals("") || split(argument, ' ').length < 2) { return 2; } else { String[] ARGS = split(argument, ' '); attributes.put(ARGS[0], getcontent(ARGS[1])); } }
        else if (mainCommand.equals("grep")) { if (argument.equals("") || split(argument, ' ').length < 2) { return 2; } else { String[] ARGS = split(argument, ' '); echoCommand(getcontent(ARGS[1]).indexOf(ARGS[0]) != -1 ? "true" : "false"); } }
        else if (mainCommand.equals("find")) { if (argument.equals("") || split(argument, ' ').length < 2) { return 2; } else { String[] ARGS = split(argument, ' '); String CONTENT = getcontent(ARGS[1]), VALUE = (String) parseProperties(CONTENT).get(ARGS[0]); echoCommand(VALUE != null ? VALUE : "null"); } }
        else if (mainCommand.equals("head")) { if (argument.equals("")) { } else { String CONTENT = getcontent(argument); String[] LINES = split(CONTENT, '\n'); int COUNT = Math.min(10, LINES.length); for (int i = 0; i < COUNT; i++) { echoCommand(LINES[i]); } } }
        else if (mainCommand.equals("tail")) { if (argument.equals("")) { } else { String CONTENT = getcontent(argument); String[] LINES = split(CONTENT, '\n'); int COUNT = Math.max(0, LINES.length - 10); for (int i = COUNT; i < LINES.length; i++) { echoCommand(LINES[i]); } } }
        else if (mainCommand.equals("diff")) { if (argument.equals("") || split(argument, ' ').length < 2) { return 2; } else { String[] FILES = split(argument, ' '); String[] LINES1 = split(getcontent(FILES[0]), '\n'), LINES2 = split(getcontent(FILES[1]), '\n'); int MAX_RANGE = Math.max(LINES1.length, LINES2.length); for (int i = 0; i < MAX_RANGE; i++) { String LINE1 = i < LINES1.length ? LINES1[i] : "", LINE2 = i < LINES2.length ? LINES2[i] : ""; if (!LINE1.equals(LINE2)) { echoCommand("--- Line " + (i + 1) + " ---\n< " + LINE1 + "\n" + "> " + LINE2); } if (i > LINES1.length || i > LINES2.length) { break; } } } }
        else if (mainCommand.equals("wc")) { if (argument.equals("")) { } else { boolean SHOW_LINES = false, SHOW_WORDS = false, SHOW_BYTES = false; if (argument.indexOf("-c") != -1) { SHOW_BYTES = true; } else if (argument.indexOf("-w") != -1) { SHOW_WORDS = true; } else if (argument.indexOf("-l") != -1) { SHOW_LINES = true; } argument = replace(argument, "-w", ""); argument = replace(argument, "-c", ""); argument = replace(argument, "-l", "").trim(); String CONTENT = getcontent(argument); int LINES = 0, WORDS = 0, CHARS = CONTENT.length(); String[] LINE_ARRAY = split(CONTENT, '\n'); LINES = LINE_ARRAY.length; for (int i = 0; i < LINE_ARRAY.length; i++) { String[] WORD_ARRAY = split(LINE_ARRAY[i], ' '); for (int j = 0; j < WORD_ARRAY.length; j++) { if (!WORD_ARRAY[j].trim().equals("")) { WORDS++; } } } String FILENAME = basename(argument); if (SHOW_LINES) { echoCommand(LINES + "\t" + FILENAME); } else if (SHOW_WORDS) { echoCommand(WORDS + "\t" + FILENAME); } else if (SHOW_BYTES) { echoCommand(CHARS + "\t" + FILENAME); } else { echoCommand(LINES + "\t" + WORDS + "\t" + CHARS + "\t" + FILENAME); } } }
        // |
        // Text Parsers
        else if (mainCommand.equals("pjnc")) { nanoContent = parseJson(nanoContent); }
        else if (mainCommand.equals("pinc")) { nanoContent = parseConf(nanoContent); }
        else if (mainCommand.equals("conf")) { echoCommand(parseConf(argument.equals("") ? nanoContent : getcontent(argument))); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(argument.equals("") ? nanoContent : getcontent(argument))); }
        else if (mainCommand.equals("vnt")) { if (argument.equals("")) { } else { String IN = getcontent(getCommand(argument)), OUT = getArgument(argument); if (OUT.equals("")) { nanoContent = text2note(IN); } else { writeRMS(OUT, text2note(IN)); } } }
        else if (mainCommand.equals("ph2s")) { StringBuffer BUFFER = new StringBuffer(); for (int i = 0; i < history.size() - 1; i++) { BUFFER.append(history.elementAt(i)); if (i < history.size() - 1) { BUFFER.append("\n"); } } String script = "#!/java/bin/sh\n\n" + BUFFER.toString(); if (argument.equals("") || argument.equals("nano")) { nanoContent = script; } else { writeRMS(argument, script); } }
        // |
        // Interfaces
        else if (mainCommand.equals("nano")) { new NanoEditor(argument); }
        else if (mainCommand.equals("html")) { viewer(extractTitle(env(nanoContent)), html2text(env(nanoContent))); }
        else if (mainCommand.equals("view")) { if (argument.equals("")) { } else { viewer(extractTitle(env(argument)), html2text(env(argument))); } }
        // |
        // Audio Manager
        else if (mainCommand.equals("audio")) { return audio(argument); }
        
        // API 013 - (MIDlet)
        // |
        // Java Runtime
        else if (mainCommand.equals("java")) { return java(argument); }
        // |
        // Permissions
        else if (mainCommand.equals("chmod")) { 
            if (argument.equals("")) { } 
            else { 
                Hashtable NODES = parseProperties("http=javax.microedition.io.Connector.http\nsocket=javax.microedition.io.Connector.socket\nfile=javax.microedition.io.Connector.file\nprg=javax.microedition.io.PushRegistry"); 
                
                int STATUS = 0; 
                if (NODES.containsKey(argument)) { 
                    try { 
                        if (argument.equals("http")) { ((HttpConnection) Connector.open("http://google.com")).close(); } 
                        else if (argument.equals("socket")) { ((SocketConnection) Connector.open(env("socket://127.0.0.1:1"))).close(); } 
                        else if (argument.equals("file")) { FileSystemRegistry.listRoots(); } 
                        else if (argument.equals("prg")) { PushRegistry.registerAlarm(getClass().getName(), System.currentTimeMillis() + 1000); } 
                    } 
                    catch (SecurityException e) { STATUS = 13; } 
                    catch (Exception e) { STATUS = 1; } 
                } 
                else if (argument.equals("*")) { 
                    Enumeration KEYS = NODES.keys(); 
                    
                    while (KEYS.hasMoreElements()) { processCommand("chmod " + (String) KEYS.nextElement(), false); } 
                } 
                else { echoCommand("chmod: " + argument + ": not found"); return 127; } 
                
                if (STATUS == 0) { MIDletLogs("add info Permission '" + (String) NODES.get(argument) + "' granted"); } 
                else if (STATUS == 1) { MIDletLogs("add debug Permission '" + (String) NODES.get(argument) + "' granted with exceptions"); } 
                else if (STATUS == 13) { MIDletLogs("add error Permission '" + (String) NODES.get(argument) + "' denied"); } 
                else if (STATUS == 3) { MIDletLogs("add warn Unsupported API '" + (String) NODES.get(argument) + "'"); } 
                
                return STATUS; 
            } 
        }
        // |
        // General Utilities
        else if (mainCommand.equals("history")) { new History(); }
        else if (mainCommand.equals("debug")) { return runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("help")) { viewer(form.getTitle(), read("/java/help.txt")); }
        else if (mainCommand.equals("man")) { 
            boolean verbose = argument.indexOf("-v") != -1; 
            if (verbose) { argument = replace(argument, "-v", "").trim(); } 
            if (argument.equals("")) { argument = "sh"; } 
            
            String content = read("/home/man.html"); 
            if (content.equals("") || argument.equals("--update")) { 
                int STATUS = processCommand("netstat"); 
                
                if (STATUS == 0) { 
                    STATUS = processCommand("execute install /home/nano; tick Downloading...; proxy proxy github.com/mrlima4095/OpenTTY-J2ME/raw/refs/heads/main/assets/root/man.html; install /home/man.html; get; tick;", false); 
                    if (STATUS == 0) { content = read("/home/man.html"); } 
                    else { return STATUS; } 
                } else { echoCommand("man: download error"); return STATUS; } 
            } 
            
            content = extractTag(content, argument.toLowerCase(), ""); 
            if (content.equals("")) { echoCommand("man: " + argument + ": not found"); return 127; } 
            else { 
                if (verbose) { echoCommand(content); } 
                else { viewer(form.getTitle(), content); } 
            } 
        }
        else if (mainCommand.equals("true") || mainCommand.equals("false") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("exit") || mainCommand.equals("quit")) { writeRMS("/home/nano", nanoContent); notifyDestroyed(); }

        //else if (mainCommand.equals("")) {  }
        //else if (mainCommand.equals("")) {  }
        else if (mainCommand.equals("eval")) { if (argument.equals("")) { } else { echoCommand("" + processCommand(argument)); } }
        else if (mainCommand.equals("return")) { try { return Integer.valueOf(argument); } catch (NumberFormatException e) { return 128; } }

        // API 014 - (OpenTTY)
        // |
        // Low-level commands
        else if (mainCommand.equals("@exec")) { commandAction(EXECUTE, display.getCurrent()); }
        else if (mainCommand.equals("@login")) { if (argument.equals("")) { username = loadRMS("OpenRMS"); } else { username = argument; } }
        else if (mainCommand.equals("@alert")) { try { display.vibrate(argument.equals("") ? 500 : Integer.parseInt(argument) * 100); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return 2; } }
        else if (mainCommand.equals("@reload")) { shell = new Hashtable(); aliases = new Hashtable(); username = loadRMS("OpenRMS"); MIDletLogs("add debug API reloaded"); processCommand("execute x11 stop; x11 init; x11 term; run initd; sh;"); }
        else if (mainCommand.startsWith("@")) { }

        // API 015 - (Scripts)
        // |
        // OpenTTY Packages
        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { return importScript(argument); }
        else if (mainCommand.equals("run")) { return processCommand(". " + argument, false); }
        else if (mainCommand.equals("function")) { if (argument.equals("")) { } else { int braceIndex = argument.indexOf('{'), braceEnd = argument.lastIndexOf('}'); if (braceIndex != -1 && braceEnd != -1 && braceEnd > braceIndex) { String name = getCommand(argument).trim(); String body = replace(argument.substring(braceIndex + 1, braceEnd).trim(), ";", "\n"); functions.put(name, body); } else { echoCommand("invalid syntax"); return 2; } } }

        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE")); }
        else if (mainCommand.equals("!!")) { if (history.size() > 0) { stdin.setString((String) history.elementAt(history.size() - 1)); } }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { return runScript(nanoContent); } else { return runScript(getcontent(argument)); } }

        else { echoCommand(mainCommand + ": not found"); return 127; }

        return 0;
    }

    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return getpattern(input.substring(spaceIndex + 1).trim()); } }

    private String read(String filename) { 
        try { 
            if (filename.startsWith("/mnt/")) { FileConnection fileConn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ); InputStream is = fileConn.openInputStream(); StringBuffer content = new StringBuffer(); int ch; while ((ch = is.read()) != -1) { content.append((char) ch); } is.close(); fileConn.close(); return env(content.toString()); } 
            else if (filename.startsWith("/home/")) { RecordStore recordStore = null; String content = ""; try { recordStore = RecordStore.openRecordStore(filename.substring(6), true); if (recordStore.getNumRecords() >= 1) { byte[] data = recordStore.getRecord(1); if (data != null) { content = new String(data); } } } catch (RecordStoreException e) { content = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return content; } 
            else { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); if (is == null) { return ""; } InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } 
        } 
        catch (IOException e) { return ""; } 
    }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); Enumeration e = attributes.keys(); while (e.hasMoreElements()) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } text = replace(text, "$.", "$"); text = replace(text, "\\.", "\\"); return text; }
    
    private String getcontent(String file) { return file.startsWith("/") ? read(file) : file.equals("nano") ? nanoContent : read(path + file); }
    private String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }

    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }
    private Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    
    public class Login implements CommandListener { private Form screen = new Form("Login"); private TextField USER = new TextField("Username", "", 256, TextField.ANY); private Command LOGIN = new Command("Login", Command.OK, 1), EXIT = new Command("Exit", Command.SCREEN, 2); public Login() { screen.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\nCreate an user to access OpenTTY!")); screen.append(USER); screen.addCommand(LOGIN); screen.addCommand(EXIT); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == LOGIN) { username = USER.getString(); if (username.equals("")) { } else { writeRMS("/home/OpenRMS", username); display.setCurrent(form); runScript(loadRMS("initd")); } } else if (c == EXIT) { processCommand("exit"); } } }

    // API 002 - (Logs)
    // |
    // OpenTTY Logging Manager
    private int MIDletLogs(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { writeRMS(argument.equals("") ? "logs" : argument, logs); } else if (mainCommand.equals("view")) { viewer(form.getTitle(), logs); } else if (mainCommand.equals("add")) { String LEVEL = getCommand(argument).toLowerCase(), MESSAGE = getArgument(argument); if (!MESSAGE.equals("")) { if (LEVEL.equals("info") || LEVEL.equals("warn") || LEVEL.equals("debug") || LEVEL.equals("error")) { logs += "[" + LEVEL.toUpperCase() + "] " + split(new java.util.Date().toString(), ' ')[3] + " " + MESSAGE + "\n"; } else { echoCommand("log: add: " + LEVEL + ": not found"); return 127; } } } else { echoCommand("log: " + mainCommand + ": not found"); return 127; } return 0; }

    // API 004 - (LCDUI Interface)
    // |
    // System UI
    private int xserver(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2025-05-04\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); }
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("buffer")) { echoCommand("" + display.getCurrent().getWidth() + "x" + display.getCurrent().getHeight() + ""); }
        // |
        // X11 Loader
        else if (mainCommand.equals("term")) { display.setCurrent(form); }
        else if (mainCommand.equals("stop")) { form.setTitle(""); form.setTicker(null); form.deleteAll(); xserver("cmd hide"); xserver("font"); form.removeCommand(EXECUTE); }
        else if (mainCommand.equals("init")) { form.setTitle(env("OpenTTY $VERSION")); form.append(stdout); form.append(stdin); form.addCommand(EXECUTE); xserver("cmd"); form.setCommandListener(this); }
        else if (mainCommand.equals("xfinit")) { if (argument.equals("")) { xserver("init"); } if (argument.equals("stdin")) { form.append(stdin); } else if (argument.equals("stdout")) { form.append(stdout); } }
        else if (mainCommand.equals("cmd")) { Command[] CMDS = { HELP, NANO, CLEAR, HISTORY }; for (int i = 0; i < CMDS.length; i++) { if (argument.equals("hide")) { form.removeCommand(CMDS[i]); } else { form.addCommand(CMDS[i]); } } }
        // | 
        // Screen MODs
        else if (mainCommand.equals("title")) { display.getCurrent().setTitle(argument); }
        else if (mainCommand.equals("font")) { if (argument.equals("")) { xserver("font default"); } else { stdout.setFont(newFont(argument)); } }
        else if (mainCommand.equals("tick")) { Displayable current = display.getCurrent(); current.setTicker(argument.equals("") ? null : new Ticker(argument)); }
        else if (mainCommand.equals("gauge")) { Alert alert = new Alert(form.getTitle(), argument, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING)); display.setCurrent(alert); }
        // |
        // Screen Manager
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { desktops.put(argument, display.getCurrent()); } }
        else if (mainCommand.equals("load")) { if (argument.equals("")) { } else { if (desktops.containsKey(argument)) { display.setCurrent((Displayable) desktops.get(argument)); } else { echoCommand("x11: load: " + argument + ": not found"); return 127; } } }
        // |
        // Interfaces
        else if (mainCommand.equals("canvas")) { display.setCurrent(new MyCanvas(argument.equals("") ? "OpenRMS" : argument)); }
        else if (mainCommand.equals("make") || mainCommand.equals("list") || mainCommand.equals("quest")) { new Screen(mainCommand, argument); }
        else if (mainCommand.equals("item")) { new ItemLoader(form, "item", argument); }

        else { echoCommand("x11: " + mainCommand + ": not found"); return 127; }

        return 0;
    }
    private int warnCommand(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    private int viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(new StringItem(null, env(text))); viewer.addCommand(new Command("Back", Command.BACK, 1)); viewer.setCommandListener(this); display.setCurrent(viewer); return 0; }
    // |
    // Interfaces
    public class Screen implements CommandListener { private Hashtable lib; private int TYPE = 0, SCREEN = 1, LIST = 2, QUEST = 3; private Form screen = new Form(form.getTitle()); private List list = new List(form.getTitle(), List.IMPLICIT); private Command BACK, USER; private TextField INPUT; public Screen(String type, String args) { if (type == null || type.length() == 0 || args == null || args.length() == 0) { return; } lib = parseProperties(getcontent(args)); if (type.equals("make")) { TYPE = SCREEN; if (lib.containsKey("screen.title")) { screen.setTitle(getenv("screen.title")); } BACK = new Command(getenv("screen.back.label", "Back"), Command.OK, 1); USER = new Command(getenv("screen.button", "Menu"), Command.SCREEN, 2); screen.addCommand(BACK); if (lib.containsKey("screen.button")) { screen.addCommand(USER); } if (lib.containsKey("screen.fields")) { String[] fields = split(getenv("screen.fields"), ','); for (int i = 0; i < fields.length; i++) { String field = fields[i].trim(); String type = getenv("screen." + field + ".type"); if (type.equals("image") && !getenv("screen." + field + ".img").equals("")) { try { screen.append(new ImageItem(null, Image.createImage(getenv("screen." + field + ".img")), ImageItem.LAYOUT_CENTER, null)); } catch (IOException e) { MIDletLogs("add warn Image '" + getenv("screen." + field + ".img") + "' could not be loaded"); } } else if (type.equals("text") && !getenv("screen." + field + ".value").equals("")) { StringItem content = new StringItem(getenv("screen." + field + ".label"), getenv("screen." + field + ".value")); content.setFont(newFont(getenv("screen." + field + ".style", "default"))); screen.append(content); } else if (type.equals("item")) { new ItemLoader(screen, "screen." + field, args); } else if (type.equals("spacer")) { int width = Integer.parseInt(getenv("screen." + field + ".w", "1")), height = Integer.parseInt(getenv("screen." + field + ".h", "10")); screen.append(new Spacer(width, height)); } } } screen.setCommandListener(this); display.setCurrent(screen); } else if (type.equals("list")) { TYPE = LIST; Image IMG = null; if (!lib.containsKey("list.content")) { MIDletLogs("add error List crashed while init, malformed settings"); return; } if (lib.containsKey("list.title")) { list.setTitle(getenv("list.title")); } if (lib.containsKey("list.icon")) { try { IMG = Image.createImage(getenv("list.icon")); } catch (IOException e) { MIDletLogs("add warn Resource '" + getenv("list.icon") + "' cannot be loaded"); } } BACK = new Command(getenv("list.back.label", "Back"), Command.OK, 1); USER = new Command(getenv("list.button", "Select"), Command.SCREEN, 2); list.addCommand(BACK); list.addCommand(USER); String[] content = split(getenv("list.content"), ','); for (int i = 0; i < content.length; i++) { list.append(content[i], IMG); } list.setCommandListener(this); display.setCurrent(list); } else if (type.equals("quest")) { TYPE = QUEST; if (!lib.containsKey("quest.label") || !lib.containsKey("quest.cmd") || !lib.containsKey("quest.key")) { MIDletLogs("add error Quest crashed while init, malformed settings"); return; } if (lib.containsKey("quest.title")) { screen.setTitle(getenv("quest.title")); } INPUT = new TextField(getenv("quest.label"), getenv("quest.content"), 256, getQuest(getenv("quest.type"))); BACK = new Command(getvalue("quest.back.label", "Cancel"), Command.SCREEN, 2); USER = new Command(getvalue("quest.cmd.label", "Send"), Command.OK, 1); screen.append(INPUT); screen.addCommand(BACK); screen.addCommand(USER); screen.setCommandListener(this); display.setCurrent(screen); } else { return; } } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); if (TYPE == SCREEN) { processCommand(getvalue("screen.back", "true")); } else if (TYPE == LIST) { processCommand(getvalue("list.back", "true")); } else if (TYPE == QUEST) { processCommand(getvalue("quest.back", "true")); } } else if (c == USER) { if (TYPE == QUEST) { String value = INPUT.getString().trim(); if (!value.equals("")) { processCommand("set " + getenv("quest.key") + "=" + env(value)); processCommand("xterm"); processCommand(getvalue("quest.cmd", "true")); } } else if (TYPE == LIST) { int index = list.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); String key = env(list.getString(index)); processCommand(getvalue(key, "log add warn An error occurred, '" + key + "' not found")); } } else if (TYPE == SCREEN) { processCommand("xterm"); processCommand(getvalue("screen.button.cmd", "log add warn An error occurred, 'screen.button.cmd' not found")); } } } private String getvalue(String key, String fallback) { return lib.containsKey(key) ? (String) lib.get(key) : fallback; } private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); } private String getenv(String key) { return env(getvalue(key, "")); } private int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = replace(mode, "password", "").trim(); } int base = TextField.ANY; if (mode.equals("number")) { base = TextField.NUMERIC; } else if (mode.equals("email")) { base = TextField.EMAILADDR; } else if (mode.equals("phone")) { base = TextField.PHONENUMBER; } else if (mode.equals("decimal")) { base = TextField.DECIMAL; } return password ? (base | TextField.PASSWORD) : base; } }
    public class MyCanvas extends Canvas implements CommandListener { private Hashtable lib; private Graphics screen; private Command BACK, USER; private Image CURSOR = null; private Vector fields = new Vector(); private final int cursorSize = 5; public MyCanvas(String args) { if (args == null || args.length() == 0) { return; } lib = parseProperties(getcontent(args)); setTitle(getenv("canvas.title", form.getTitle())); BACK = new Command(getenv("canvas.back.label", "Back"), Command.OK, 1); USER = new Command(getenv("canvas.button"), Command.SCREEN, 2); addCommand(BACK); if (lib.containsKey("canvas.button")) { addCommand(USER); } if (lib.containsKey("canvas.mouse")) { try { String[] pos = split(getenv("canvas.mouse"), ','); cursorX = Integer.parseInt(pos[0]); cursorY = Integer.parseInt(pos[1]); } catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas.mouse' - (x,y) may be a int number"); cursorX = 10; cursorY = 10; } } if (lib.containsKey("canvas.mouse.img")) { try { CURSOR = Image.createImage(getenv("canvas.mouse.img")); } catch (IOException e) { MIDletLogs("add warn Cursor " + getenv("canvas.mouse.img") + " could not be loaded"); } } if (lib.containsKey("canvas.fields")) { String[] names = split(getenv("canvas.fields"), ','); for (int i = 0; i < names.length; i++) { String id = names[i].trim(), type = getenv("canvas." + id + ".type", "text"); int x = Integer.parseInt(getenv("canvas." + id + ".x", "0")), y = Integer.parseInt(getenv("canvas." + id + ".y", "0")), w = Integer.parseInt(getenv("canvas." + id + ".w", "0")), h = Integer.parseInt(getenv("canvas." + id + ".h", "0")); Hashtable field = new Hashtable(); field.put("type", type); field.put("x", new Integer(x)); field.put("y", new Integer(y)); field.put("w", new Integer(w)); field.put("h", new Integer(h)); field.put("value", getenv("canvas." + id + ".value", "")); field.put("style", getenv("canvas." + id + ".style", "default")); field.put("cmd", getenv("canvas." + id + ".cmd", "")); fields.addElement(field); } } setCommandListener(this); } protected void paint(Graphics g) { if (screen == null) { screen = g; } g.setColor(0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); if (lib.containsKey("canvas.background")) { String backgroundType = getenv("canvas.background.type", "default"); if (backgroundType.equals("color") || backgroundType.equals("default")) { setpallete("background", g, 0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); } else if (backgroundType.equals("image")) { try { Image content = Image.createImage(getenv("canvas.background")); g.drawImage(content, (getWidth() - content.getWidth()) / 2, (getHeight() - content.getHeight()) / 2, Graphics.TOP | Graphics.LEFT); } catch (IOException e) { processCommand("xterm"); processCommand("execute log add error Malformed Image, " + e.getMessage()); } } } if (lib.containsKey("canvas.fields")) { for (int i = 0; i < fields.size(); i++) { Hashtable f = (Hashtable) fields.elementAt(i); String type = (String) f.get("type"), val = (String) f.get("value"); int x = ((Integer) f.get("x")).intValue(), y = ((Integer) f.get("y")).intValue(), w = ((Integer) f.get("w")).intValue(), h = ((Integer) f.get("h")).intValue(); if (type.equals("text")) { setpallete("text.color", g, 255, 255, 255); g.setFont(newFont((String) f.get("style"))); g.drawString(val, x, y, Graphics.TOP | Graphics.LEFT); } else if (type.equals("image")) { try { Image IMG = Image.createImage(val); g.drawImage(IMG, x, y, Graphics.TOP | Graphics.LEFT); if (w == 0) { f.put("w", new Integer(IMG.getWidth())); } if (h == 0) { f.put("h", new Integer(IMG.getHeight())); } } catch (IOException e) { MIDletLogs("add error Malformed Image, " + e.getMessage()); } } else if (type.equals("rect")) { setpallete("rect.color", g, 0, 0, 255); g.drawRect(x, y, w, h); } else if (type.equals("circle")) { setpallete("circle.color", g, 0, 255, 0); g.drawArc(x - w, y - w, w * 2, w * 2, 0, 360); } else if (type.equals("line")) { setpallete("line.color", g, 255, 255, 255); g.drawLine(x, y, w, h); } } } if (CURSOR != null) { g.drawImage(CURSOR, cursorX, cursorY, Graphics.TOP | Graphics.LEFT); } else { setpallete("mouse.color", g, 255, 255, 255); g.fillRect(cursorX, cursorY, cursorSize, cursorSize); } } protected void keyPressed(int keyCode) { int gameAction = getGameAction(keyCode); if (gameAction == LEFT) { cursorX = Math.max(0, cursorX - 5); } else if (gameAction == RIGHT) { cursorX = Math.min(getWidth() - cursorSize, cursorX + 5); } else if (gameAction == UP) { cursorY = Math.max(0, cursorY - 5); } else if (gameAction == DOWN) { cursorY = Math.min(getHeight() - cursorSize, cursorY + 5); }  else if (gameAction == FIRE) { for (int i = 0; i < fields.size(); i++) { Hashtable f = (Hashtable) fields.elementAt(i); int x = ((Integer) f.get("x")).intValue(), y = ((Integer) f.get("y")).intValue(), w = ((Integer) f.get("w")).intValue(), h = ((Integer) f.get("h")).intValue(); String type = (String) f.get("type"), cmd = (String) f.get("cmd"), val = (String) f.get("value"); if (cmd != null && !cmd.equals("")) { boolean hit = false; if (type.equals("circle")) { int dx = cursorX - x, dy = cursorY - y; hit = (dx * dx + dy * dy) <= (w * w); } else if (type.equals("text")) { Font font = newFont(getenv((String) f.get("style"), "default")); int textW = font.stringWidth(val), textH = font.getHeight(); hit = cursorX + cursorSize > x && cursorX < x + textW && cursorY + cursorSize > y && cursorY < y + textH; } else if (type.equals("line")) { continue; } else { hit = cursorX + cursorSize > x && cursorX < x + w && cursorY + cursorSize > y && cursorY < y + h; } if (hit) { processCommand(cmd); break; } } } } repaint(); } protected void pointerPressed(int x, int y) { cursorX = x; cursorY = y; keyPressed(-5); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); processCommand(getvalue("canvas.back", "true")); } else if (c == USER) { processCommand("xterm"); processCommand(getvalue("canvas.button.cmd", "log add warn An error occurred, 'canvas.button.cmd' not found")); } } private void setpallete(String node, Graphics screen, int r, int g, int b) { try { String[] pallete = split(getenv("canvas." + node, "" + r + "," + g + "," + b), ','); screen.setColor(Integer.parseInt(pallete[0]), Integer.parseInt(pallete[1]), Integer.parseInt(pallete[2])); } catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas." + node + "' - (r,g,b) may be a int number"); } } private String getvalue(String key, String fallback) { return lib.containsKey(key) ? (String) lib.get(key) : fallback; } private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); } private String getenv(String key) { return env(getvalue(key, "")); } }
    // |
    // Item MOD Loader
    public class ItemLoader implements ItemCommandListener { private Hashtable lib; private Command run; private StringItem s; private String node; public ItemLoader(Form screen, String prefix, String args) { if (args == null || args.length() == 0) { return; } else if (args.equals("clear")) { form.deleteAll(); form.append(stdout); form.append(stdin); return; } lib = parseProperties(getcontent(args)); node = prefix; if (!lib.containsKey(node + ".label") || !lib.containsKey(node + ".cmd")) { MIDletLogs("add error Malformed ITEM, missing params"); return; } run = new Command(env((String) lib.get(node + ".label")), Command.ITEM, 1); s = new StringItem(null, env((String) lib.get(node + ".label")), StringItem.BUTTON); s.setFont(Font.getDefaultFont()); s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(run); s.setDefaultCommand(run); s.setItemCommandListener(this); screen.append(s); } public void commandAction(Command c, Item item) { if (c == run) { processCommand("xterm"); processCommand((String) lib.get(node + ".cmd")); } } }
    // |
    // Font Generator
    private Font newFont(String argument) { if (argument == null || argument.length() == 0 || argument.equals("default")) { return Font.getDefaultFont(); } int style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; if (argument.equals("bold")) { style = Font.STYLE_BOLD; } else if (argument.equals("italic")) { style = Font.STYLE_ITALIC; } else if (argument.equals("ul")) { style = Font.STYLE_UNDERLINED; } else if (argument.equals("small")) { size = Font.SIZE_SMALL; } else if (argument.equals("large")) { size = Font.SIZE_LARGE; } else { return newFont("default"); } return Font.getFont(Font.FACE_SYSTEM, style, size); }

    // API 005 - (Operators)
    // |
    // Operators
    private int ifCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("if (expr) [command]"); return 2; } String EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); String[] PARTS = split(EXPR, ' '); if (PARTS.length == 3) { boolean CONDITION = false; boolean NEGATED = PARTS[1].startsWith("!") && !PARTS[1].equals("!="); if (NEGATED) { PARTS[1] = PARTS[1].substring(1); } Double N1 = getNumber(PARTS[0]), N2 = getNumber(PARTS[2]); if (N1 != null && N2 != null) { if (PARTS[1].equals("==")) { CONDITION = N1.doubleValue() == N2.doubleValue(); } else if (PARTS[1].equals("!=")) { CONDITION = N1.doubleValue() != N2.doubleValue(); } else if (PARTS[1].equals(">")) { CONDITION = N1.doubleValue() > N2.doubleValue(); } else if (PARTS[1].equals("<")) { CONDITION = N1.doubleValue() < N2.doubleValue(); } else if (PARTS[1].equals(">=")) { CONDITION = N1.doubleValue() >= N2.doubleValue(); } else if (PARTS[1].equals("<=")) { CONDITION = N1.doubleValue() <= N2.doubleValue(); } } else { if (PARTS[1].equals("startswith")) { CONDITION = PARTS[0].startsWith(PARTS[2]); } else if (PARTS[1].equals("endswith")) { CONDITION = PARTS[0].endsWith(PARTS[2]); } else if (PARTS[1].equals("contains")) { CONDITION = PARTS[0].indexOf(PARTS[2]) != -1; } else if (PARTS[1].equals("==")) { CONDITION = PARTS[0].equals(PARTS[2]); } else if (PARTS[1].equals("!=")) { CONDITION = !PARTS[0].equals(PARTS[2]); } } if (CONDITION != NEGATED) { return processCommand(CMD); } } else if (PARTS.length == 2) { if (PARTS[0].equals(PARTS[1])) { return processCommand(CMD); } } else if (PARTS.length == 1) { if (!PARTS[0].equals("")) { return processCommand(CMD); } } return 0; }
    private int forCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String KEY = getCommand(argument), FILE = getcontent(argument.substring(firstParenthesis + 1, lastParenthesis).trim()), CMD = argument.substring(lastParenthesis + 1).trim(); if (KEY.startsWith("(")) { return 2; } if (KEY.startsWith("$")) { KEY = replace(KEY, "$", ""); } String[] LINES = split(FILE, '\n'); for (int i = 0; i < LINES.length; i++) { if (LINES[i] != null || LINES[i].length() != 0) { processCommand("set " + KEY + "=" + LINES[i], false); int STATUS = processCommand(CMD); processCommand("unset " + KEY, false); if (STATUS != 0) { return STATUS; } } } return 0; }
    private int caseCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('), lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return 2; } String METHOD = getCommand(argument), EXPR = argument.substring(firstParenthesis + 1, lastParenthesis).trim(), CMD = argument.substring(lastParenthesis + 1).trim(); boolean CONDITION = false; boolean NEGATED = METHOD.startsWith("!"); if (NEGATED) { METHOD = METHOD.substring(1); } if (METHOD.equals("file")) { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(EXPR)) { CONDITION = true; break; } } } } else if (METHOD.equals("root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(EXPR)) { CONDITION = true; break; } } } else if (METHOD.equals("thread")) { CONDITION = replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet").equals(EXPR); } else if (METHOD.equals("screen")) { CONDITION = desktops.containsKey(EXPR); } else if (METHOD.equals("key")) { CONDITION = attributes.containsKey(EXPR); } else if (METHOD.equals("alias")) { CONDITION = aliases.containsKey(EXPR); } else if (METHOD.equals("trace")) { CONDITION = trace.containsKey(EXPR); } if (CONDITION != NEGATED) { return processCommand(CMD); } return 0; }
    
    // API 006 - (Process)
    // |
    // Memory
    public class HTopViewer implements CommandListener { private Form monitor = new Form(form.getTitle()); private List process = new List(form.getTitle(), List.IMPLICIT); private StringItem status = new StringItem("Memory Status:", ""); private Command BACK = new Command("Back", Command.BACK, 1), REFRESH = new Command("Refresh", Command.SCREEN, 2), KILL = new Command("Kill", Command.SCREEN, 2); private int TYPE = 0, MONITOR = 1, PROCESS = 2; public HTopViewer(String args) { if (args == null || args.length() == 0 || args.equals("memory")) { TYPE = MONITOR; monitor.append(status); load(); monitor.addCommand(BACK); monitor.addCommand(REFRESH); monitor.setCommandListener(this); display.setCurrent(monitor); } else if (args.equals("process")) { TYPE = PROCESS; load(); process.addCommand(BACK); process.addCommand(KILL); process.setCommandListener(this); display.setCurrent(process); } else { echoCommand("htop: " + args + ": not found"); } } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == REFRESH) { System.gc(); load(); } else if (c == KILL) { int index = process.getSelectedIndex(); if (index >= 0) { processCommand("kill " + split(process.getString(index), '\t')[0]); load(); } } } private void load() { if (TYPE == MONITOR) { status.setText("Used Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\nFree Memory: " + runtime.freeMemory() / 1024 + " KB\nTotal Memory: " + runtime.totalMemory() / 1024 + " KB"); } else if (TYPE == PROCESS) { process.deleteAll(); Enumeration keys = trace.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String pid = (String) trace.get(key); process.append(pid + "\t" + key, null); } } } }
    // |
    // Process
    private int kill(String pid) { if (pid == null || pid.length() == 0) { return 2; } Enumeration KEYS = trace.keys(); while (KEYS.hasMoreElements()) { String KEY = (String) KEYS.nextElement(); if (pid.equals(trace.get(KEY))) { trace.remove(KEY); echoCommand("Process with PID " + pid + " terminated"); if (KEY.equals("sh")) { processCommand("exit"); } return 0; } } echoCommand("PID '" + pid + "' not found"); return 127; }
    private int start(String app) { if (app == null || app.length() == 0 || trace.containsKey(app)) { return 2; } trace.put(app, String.valueOf(1000 + random.nextInt(9000))); if (app.equals("sh")) { sessions.addElement("127.0.0.1"); } return 0; }
    private int stop(String app) { if (app == null || app.length() == 0) { return 2; } trace.remove(app); if (app.equals("sh")) { processCommand("exit"); } return 0; } 

    // API 008 - (Logic I/O) Text
    // |
    // Text related commands
    private void echoCommand(String message) { echoCommand(message, stdout); attributes.put("OUTPUT", message); }
    private void echoCommand(String message, StringItem console) { console.setText(console.getText().equals("") ? message.trim() : console.getText() + "\n" + message.trim()); }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String exprCommand(String expr) { char[] tokens = expr.toCharArray(); double[] vals = new double[32]; char[] ops = new char[32]; int valTop = -1, opTop = -1; int i = 0, len = tokens.length; while (i < len) { char c = tokens[i]; if (c == ' ') { i++; continue; } if (c >= '0' && c <= '9') { double num = 0; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { num = num * 10 + (tokens[i++] - '0'); } if (i < len && tokens[i] == '.') { i++; double frac = 0, div = 10; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { frac += (tokens[i++] - '0') / div; div *= 10; } num += frac; } vals[++valTop] = num; } else if (c == '(') { ops[++opTop] = c; i++; } else if (c == ')') { while (opTop >= 0 && ops[opTop] != '(') { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } opTop--; i++; } else if (c == '+' || c == '-' || c == '*' || c == '/') { while (opTop >= 0 && prec(ops[opTop]) >= prec(c)) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } ops[++opTop] = c; i++; } else { return "expr: invalid char '" + c + "'"; } } while (opTop >= 0) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } double result = vals[valTop]; return ((int) result == result) ? String.valueOf((int) result) : String.valueOf(result); } private int prec(char op) { if (op == '+' || op == '-') return 1; if (op == '*' || op == '/') return 2; return 0; } private double applyOpSimple(char op, double a, double b) { if (op == '+') return a + b; if (op == '-') return a - b; if (op == '*') return a * b; if (op == '/') return b == 0 ? 0 : a / b; return 0; }
    private String generateUUID() { String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + random.nextInt(4))); } else { uuid.append(chars.charAt(random.nextInt(16))); } } return uuid.toString(); }

    // API 011 - (Network)
    // |
    // Servers
    public class Bind implements Runnable { private String port, prefix; public Bind(String args) { if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522", false); new Bind("31522"); return; } port = getCommand(args); prefix = getArgument(args); new Thread(this, "Bind").start(); } public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); start("bind"); while (trace.containsKey("bind")) { SocketConnection clientSocket = null; InputStream is = null; OutputStream os = null; try { clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); String address = clientSocket.getAddress(); echoCommand("[+] " + address + " connected"); sessions.addElement(address); is = clientSocket.openInputStream(); os = clientSocket.openOutputStream(); while (trace.containsKey("bind")) { byte[] buffer = new byte[4096]; int bytesRead = is.read(buffer); if (bytesRead == -1) { break; } String command = new String(buffer, 0, bytesRead).trim(); echoCommand("[+] " + address + " -> " + env(command)); command = (prefix == null || prefix.length() == 0 || prefix.equals("null")) ? command : prefix + " " + command; String beforeCommand = stdout != null ? stdout.getText() : ""; processCommand(command); String afterCommand = stdout != null ? stdout.getText() : ""; String output = afterCommand.length() >= beforeCommand.length() ? afterCommand.substring(beforeCommand.length()).trim() + "\n" : "\n"; os.write(output.getBytes()); os.flush(); } echoCommand("[-] " + address + " disconnected"); sessions.removeElement(address); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); stop("bind"); } finally { try { if (clientSocket != null) { clientSocket.close(); } if (os != null) { os.close(); } if (is != null) { is.close(); } } catch (IOException e) { stop("bind"); } } } echoCommand("[-] Server stopped"); MIDletLogs("add info Server was stopped"); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'"); } try { if (serverSocket != null) { serverSocket.close(); } } catch (IOException e) { } } }
    public class Server implements Runnable { private String port, response; public Server(String args) { if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522", false); new Server("31522"); return; } port = getCommand(args); response = getArgument(args); new Thread(this, "Server").start(); } public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); start("server"); while (trace.containsKey("server")) { SocketConnection clientSocket = null; InputStream is = null; OutputStream os = null; try { clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); is = clientSocket.openInputStream(); os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[4096]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); if (response.startsWith("/")) { os.write(read(response).getBytes()); } else if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(loadRMS(response).getBytes()); } os.flush(); } catch (IOException e) { } finally { try { if (is != null) is.close(); if (os != null) os.close(); if (clientSocket != null) clientSocket.close(); } catch (IOException e) { } } } echoCommand("[-] Server stopped"); MIDletLogs("add info Server was stopped"); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'");  } try { if (serverSocket != null) { serverSocket.close(); } } catch (IOException e) { } } }
    // |
    // HTTP Interfaces
    public class GoBuster implements CommandListener, Runnable { private String url; private String[] wordlist; private List screen; private Command BACK = new Command("Back", Command.BACK, 1), OPEN = new Command("Get Request", Command.OK, 1), SAVE = new Command("Save Result", Command.OK, 1); public GoBuster(String args) { if (args == null || args.length() == 0) { return; } url = getCommand(args); wordlist = split(getArgument(args).equals("") ? loadRMS("gobuster") : getcontent(getArgument(args)), '\n'); if (wordlist == null || wordlist.length == 0) { echoCommand("gobuster: blank word list"); return; } screen = new List("GoBuster (" + url + ")", List.IMPLICIT); screen.addCommand(BACK); screen.addCommand(OPEN); screen.addCommand(SAVE); screen.setCommandListener(this); new Thread(this, "GoBuster").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == OPEN) { processCommand("bg execute wget " + url + getArgument(screen.getString(screen.getSelectedIndex())) + "; nano;"); } else if (c == SAVE && screen.size() != 0) { nanoContent = GoSave(); new NanoEditor(""); } } public void run() { screen.setTicker(new Ticker("Searching...")); for (int i = 0; i < wordlist.length; i++) { if (!wordlist[i].startsWith("#") && !wordlist[i].equals("")) { String fullUrl = url.startsWith("http://") || url.startsWith("https://") ? url + "/" + wordlist[i] : "http://" + url + "/" + wordlist[i]; try { int code = GoVerify(fullUrl); if (code != 404) { screen.append(code + " /" + wordlist[i], null); } } catch (IOException e) { } } } screen.setTicker(null); } private int GoVerify(String fullUrl) throws IOException { HttpConnection conn = null; InputStream is = null; try { conn = (HttpConnection) Connector.open(fullUrl); conn.setRequestMethod(HttpConnection.GET); return conn.getResponseCode(); } finally { if (is != null) { is.close(); } if (conn != null) { conn.close(); } } } private String GoSave() { StringBuffer sb = new StringBuffer(); for (int i = 0; i < screen.size(); i++) { String line = getArgument(screen.getString(i)).trim(); sb.append(line.substring(1, line.length())).append("\n"); } return sb.toString().trim(); } }
    private String request(String url, Hashtable headers) { if (url == null || url.length() == 0) { return ""; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); if (headers != null) { Enumeration keys = headers.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) headers.get(key); conn.setRequestProperty(key, value); } } InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } is.close(); conn.close(); return new String(baos.toByteArray(), "UTF-8"); } catch (IOException e) { return e.getMessage(); } }
    private String request(String url) { return request(url, null); }
    // |
    // Socket Interfaces
    private int query(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { echoCommand("query: missing [address]"); } else { try { StreamConnection CONN = (StreamConnection) Connector.open(mainCommand); InputStream IN = CONN.openInputStream(); OutputStream OUT = CONN.openOutputStream(); if (!argument.equals("")) { OUT.write((argument + "\r\n").getBytes()); OUT.flush(); } ByteArrayOutputStream BAOS = new ByteArrayOutputStream(); byte[] BUFFER = new byte[1024]; int LENGTH; while ((LENGTH = IN.read(BUFFER)) != -1) { BAOS.write(BUFFER, 0, LENGTH); } String DATA = new String(BAOS.toByteArray(), "UTF-8"); if (env("$QUERY").equals("$QUERY") || env("$QUERY").equals("")) { echoCommand(DATA); MIDletLogs("add warn Query storage setting not found"); } else if (env("$QUERY").toLowerCase().equals("show")) { echoCommand(DATA); } else if (env("$QUERY").toLowerCase().equals("nano")) { nanoContent = DATA; echoCommand("query: data retrieved"); } else { writeRMS(env("$QUERY"), DATA); } IN.close(); OUT.close(); CONN.close(); } catch (Exception e) { echoCommand(e.getMessage()); return 1; } } return 0; }
    private int GetAddress(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { return processCommand("ifconfig"); } else { try { DatagramConnection CONN = (DatagramConnection) Connector.open("datagram://" + (argument.equals("") ? "1.1.1.1:53" : argument)); ByteArrayOutputStream OUT = new ByteArrayOutputStream(); OUT.write(0x12); OUT.write(0x34); OUT.write(0x01); OUT.write(0x00); OUT.write(0x00); OUT.write(0x01); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); OUT.write(0x00); String[] parts = split(mainCommand, '.'); for (int i = 0; i < parts.length; i++) { OUT.write(parts[i].length()); OUT.write(parts[i].getBytes()); } OUT.write(0x00); OUT.write(0x00); OUT.write(0x01); OUT.write(0x00); OUT.write(0x01); byte[] query = OUT.toByteArray(); Datagram REQUEST = CONN.newDatagram(query, query.length); CONN.send(REQUEST); Datagram RESPONSE = CONN.newDatagram(512); CONN.receive(RESPONSE); CONN.close(); byte[] data = RESPONSE.getData(); if ((data[3] & 0x0F) != 0) { echoCommand("not found"); return 127; } int offset = 12; while (data[offset] != 0) { offset++; } offset += 5; if (data[offset + 2] == 0x00 && data[offset + 3] == 0x01) { StringBuffer BUFFER = new StringBuffer(); for (int i = offset + 12; i < offset + 16; i++) { BUFFER.append(data[i] & 0xFF); if (i < offset + 15) BUFFER.append("."); } echoCommand(BUFFER.toString()); } else { echoCommand("not found"); return 127; } } catch (IOException e) { echoCommand(e.getMessage()); return 1; } } return 0; }
    public class PortScanner implements CommandListener, Runnable { private String host; private int start = 1; private List screen; private Command BACK = new Command("Back", Command.BACK, 1), CONNECT = new Command("Connect", Command.OK, 1); public PortScanner(String args) { if (args == null || args.length() == 0) { return; } if (!getArgument(args).equals("")) { try { start = Integer.parseInt(getArgument(args)); } catch (NumberFormatException e) { echoCommand("prscan: " + getArgument(args) + ": invalid start port"); return; } } host = getCommand(args); screen = new List(host + " Ports", List.IMPLICIT); screen.addCommand(BACK); screen.addCommand(CONNECT); screen.setCommandListener(this); new Thread(this, "Port-Scanner").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == CONNECT) { new RemoteConnection(host + ":" + screen.getString(screen.getSelectedIndex())); } } public void run() { screen.setTicker(new Ticker("Scanning...")); for (int port = start; port <= 65535; port++) { screen.setTicker(new Ticker("Scanning Port " + port + "...")); try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port, Connector.READ_WRITE, false); screen.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } screen.setTicker(null); } }
    public class RemoteConnection implements CommandListener, Runnable { private SocketConnection socket; private InputStream inputStream; private OutputStream outputStream; private String host; private Form screen = new Form(form.getTitle()); private TextField inputField = new TextField("Command", "", 256, TextField.ANY); private Command BACK = new Command("Back", Command.SCREEN, 1), EXECUTE = new Command("Send", Command.OK, 1), CLEAR = new Command("Clear", Command.SCREEN, 1), VIEW = new Command("Show info", Command.SCREEN, 1); private StringItem console = new StringItem("", ""); public RemoteConnection(String args) { if (args == null || args.length() == 0) { return; } host = args; inputField.setLabel("Remote (" + split(args, ':')[0] + ")"); screen.append(console); screen.append(inputField); screen.addCommand(EXECUTE); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(VIEW); screen.setCommandListener(this); try { socket = (SocketConnection) Connector.open("socket://" + args); inputStream = socket.openInputStream(); outputStream = socket.openOutputStream(); } catch (IOException e) { echoCommand(e.getMessage()); return; } start("remote"); new Thread(this, "Remote").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == EXECUTE) { String data = inputField.getString().trim(); inputField.setString(""); try { outputStream.write((data + "\n").getBytes()); outputStream.flush(); } catch (IOException e) { processCommand("warn " + e.getMessage()); } } else if (c == BACK) { try { outputStream.write("".getBytes()); outputStream.flush(); inputStream.close(); outputStream.close(); } catch (IOException e) { } writeRMS("remote", console.getText()); stop("remote"); processCommand("xterm"); } else if (c == CLEAR) { console.setText(""); } else if (c == VIEW) { try { warnCommand("Information", "Host: " + split(host, ':')[0] + "\n" + "Port: " + split(host, ':')[1] + "\n\n" + "Local Address: " + socket.getLocalAddress() + "\n" + "Local Port: " + socket.getLocalPort()); } catch (IOException e) { } } } public void run() { while (trace.containsKey("remote")) { try { byte[] buffer = new byte[4096]; int length = inputStream.read(buffer); if (length != -1) { echoCommand(new String(buffer, 0, length), console); } } catch (IOException e) { processCommand("warn " + e.getMessage()); stop("remote"); } } } }

    // API 012 - (File)
    // |
    // Directories Manager
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.length() == 0 || line.startsWith("#")) { continue; } if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath + "/"); start = j; } } String finalPart = line.substring(start + 1); fullPath += "/" + finalPart; if (line.endsWith("/")) { addDirectory(fullPath + "/"); } else { addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { boolean isDirectory = fullPath.endsWith("/"); if (!paths.containsKey(fullPath)) { if (isDirectory) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/', fullPath.length() - 2) + 1); String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } String dirName = fullPath.substring(parentPath.length(), fullPath.length() - 1); updatedContents.addElement(dirName + "/"); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } else { String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/') + 1); String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1); String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fileName); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } } }
    public class Explorer implements CommandListener { private List screen = new List(form.getTitle(), List.IMPLICIT); private Command BACK = new Command("Back", Command.BACK, 1), OPEN = new Command("Open", Command.OK, 1), DELETE = new Command("Delete", Command.OK, 2), RUN = new Command("Run Script", Command.OK, 3), IMPORT = new Command("Import File", Command.OK, 4); private Image DIR = null, FILE = null, UP = null; public Explorer() { screen.addCommand(BACK); screen.addCommand(OPEN); screen.setCommandListener(this); try { FILE = Image.createImage("/java/etc/icons/file.png"); DIR = Image.createImage("/java/etc/icons/dir.png"); UP = Image.createImage("/java/etc/icons/up.png"); } catch (IOException e) { } load(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { String selected = screen.getString(screen.getSelectedIndex()); if (c == BACK) { processCommand("xterm"); } else if (c == OPEN) { if (selected != null) { if (selected.endsWith("..")) { int lastSlash = path.lastIndexOf('/', path.length() - 2); if (lastSlash != -1) { path = path.substring(0, lastSlash + 1); } } else if (selected.endsWith("/")) { path += selected; } else { new NanoEditor(path + selected); } stdin.setLabel(username + " " + path + " $"); load(); } } else if (c == DELETE) { deleteFile(path + selected); load(); } else if (c == RUN) { processCommand("xterm"); runScript(getcontent(path + selected)); } else if (c == IMPORT) { processCommand("xterm"); importScript(path + selected); } } private void load() { screen.deleteAll(); if (!path.equals("/")) { screen.append("..", UP); } if (isWritable(path)) { screen.addCommand(DELETE); } else { screen.removeCommand(DELETE); } if (isRoot(path)) { screen.removeCommand(RUN); screen.removeCommand(IMPORT); } else { screen.addCommand(RUN); screen.addCommand(IMPORT); } try { if (path.equals("/mnt/")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { screen.append((String) roots.nextElement(), DIR); } } else if (path.startsWith("/mnt/")) { try { FileConnection dir = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ); Enumeration content = dir.list(); Vector dirs = new Vector(), files = new Vector(); while (content.hasMoreElements()) { String name = (String) content.nextElement(); if (name.endsWith("/")) { dirs.addElement(name); } else { files.addElement(name); } } while (!dirs.isEmpty()) { screen.append(getFirstString(dirs), DIR); } while (!files.isEmpty()) { screen.append(getFirstString(files), FILE); } dir.close(); } catch (IOException e) { } } else if (path.equals("/home/")) { try { String[] recordStores = RecordStore.listRecordStores(); for (int i = 0; i < recordStores.length; i++) { if (!recordStores[i].startsWith(".")) { screen.append(recordStores[i], FILE); } } } catch (RecordStoreException e) { } } String[] files = (String[]) paths.get(path); if (files != null) { for (int i = 0; i < files.length; i++) { String f = files[i]; if (f != null && !f.equals("..") && !f.equals("/")) { if (f.endsWith("/")) { screen.append(f, DIR); } else { screen.append(f, FILE); } } } } } catch (IOException e) { } } private boolean isWritable(String path) { return path.startsWith("/home/") || (path.startsWith("/mnt/") && !path.equals("/mnt/")); } private boolean isRoot(String path) { return path.equals("/") || path.equals("/mnt/"); } private static String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(result) < 0) { result = cur; } } v.removeElement(result); return result; } }
    private String readStack() { StringBuffer sb = new StringBuffer(); sb.append(path); for (int i = 0; i < stack.size(); i++) { sb.append(" ").append((String) stack.elementAt(i)); } return sb.toString(); }
    // |
    // RMS Files
    private int deleteFile(String filename) { if (filename == null || filename.length() == 0) { return 2; } else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (CONN.exists()) { CONN.delete(); } else { echoCommand("rm: " + basename(filename) + ": not found"); return 127; } CONN.close(); } catch (Exception e) { echoCommand(e.getMessage()); } } else if (filename.startsWith("/home/")) { try { RecordStore.deleteRecordStore(filename.substring(6)); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename.substring(6) + ": not found"); return 127; } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); return 1; } } else if (filename.startsWith("/")) { echoCommand("read-only storage"); return 5; } else { return deleteFile(path + filename); } return 0; }
    private int writeRMS(String filename, String data) { if (filename == null || filename.length() == 0) { return 2; } else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.create(); } OutputStream OUT = CONN.openOutputStream(); OUT.write(data.getBytes()); OUT.flush(); } catch (SecurityException e) { echoCommand(e.getMessage()); return 13; } catch (Exception e) { echoCommand(e.getMessage()); return 1; } } else if (filename.startsWith("/home/")) { RecordStore CONN = null; try { CONN = RecordStore.openRecordStore(filename.substring(6), true); byte[] byteData = data.getBytes(); if (CONN.getNumRecords() > 0) { CONN.setRecord(1, byteData, 0, byteData.length); } else { CONN.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (CONN != null) { try { CONN.closeRecordStore(); } catch (RecordStoreException e) { } } } } else if (filename.startsWith("/")) { echoCommand("read-only storage"); return 5; } else { return writeRMS(path + filename, data); } return 0; }
    private String loadRMS(String filename) { return read("/home/" + filename); }
    // |
    // Text Manager
    private int StringEditor(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("-2u")) { nanoContent = nanoContent.toUpperCase(); } else if (mainCommand.equals("-2l")) { nanoContent = nanoContent.toLowerCase(); } else if (mainCommand.equals("-d")) { nanoContent = replace(nanoContent, split(argument, ' ')[0], ""); } else if (mainCommand.equals("-a")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } else if (mainCommand.equals("-r")) { nanoContent = replace(nanoContent, split(argument, ' ')[0], split(argument, ' ')[1]); } else if (mainCommand.equals("-l")) { int i = 0; try { i = Integer.parseInt(argument); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return 2; } echoCommand(split(nanoContent, '\n')[i]); } else if (mainCommand.equals("-s")) { int i = 0; try { i = Integer.parseInt(getCommand(argument)); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return 2; } Vector lines = new Vector(); String div = getArgument(argument); int start = 0, index; while ((index = nanoContent.indexOf(div, start)) != -1) { lines.addElement(nanoContent.substring(start, index)); start = index + div.length(); } if (start < nanoContent.length()) { lines.addElement(nanoContent.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); if (i >= 0 && i < result.length) { echoCommand(result[i]); } else { echoCommand("null"); return 1; } } else if (mainCommand.equals("-p")) { String[] contentLines = split(nanoContent, '\n'); StringBuffer updatedContent = new StringBuffer(); for (int i = 0; i < contentLines.length; i++) { updatedContent.append(argument).append(contentLines[i]).append("\n"); } nanoContent = updatedContent.toString().trim(); } else if (mainCommand.equals("-v")) { String[] lines = split(nanoContent, '\n'); StringBuffer reversed = new StringBuffer(); for (int i = lines.length - 1; i >= 0; i--) { reversed.append(lines[i]).append("\n"); } nanoContent = reversed.toString().trim(); } else { return 127; } return 0; }
    // |
    // Text Parsers
    private String parseJson(String text) { Hashtable properties = parseProperties(text); if (properties.isEmpty()) { return "{}"; } Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\n  \"").append(key).append("\": "); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("\n}"); return jsonBuffer.toString(); }
    private String parseConf(String text) { StringBuffer iniBuffer = new StringBuffer(); text = text.trim(); if (text.startsWith("{") && text.endsWith("}")) { text = text.substring(1, text.length() - 1); } String[] pairs = split(text, ','); for (int i = 0; i < pairs.length; i++) { String pair = pairs[i].trim(); String[] keyValue = split(pair, ':'); if (keyValue.length == 2) { String key = getpattern(keyValue[0].trim()); String value = getpattern(keyValue[1].trim()); iniBuffer.append(key).append("=").append(value).append("\n"); } } return iniBuffer.toString(); }
    private String text2note(String content) { if (content == null || content.length() == 0) { return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:\nEND:VNOTE"; } content = replace(content, "=", "=3D"); content = replace(content, "\n", "=0A"); StringBuffer vnote = new StringBuffer(); vnote.append("BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" + content + "\nEND:VNOTE"); return vnote.toString(); }
    // |
    // Interfaces
    public class NanoEditor implements CommandListener { private TextBox screen = new TextBox("Nano", "", 31522, TextField.ANY); private Command BACK = new Command("Back", Command.BACK, 1), CLEAR = new Command("Clear", Command.OK, 1), RUN = new Command("Run Script", Command.OK, 1), IMPORT = new Command("Import File", Command.OK, 1), VIEW = new Command("View as HTML", Command.OK, 1); public NanoEditor(String args) { screen.setString((args == null || args.length() == 0) ? nanoContent : getcontent(args)); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(RUN); screen.addCommand(IMPORT); screen.addCommand(VIEW); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { nanoContent = screen.getString(); processCommand("xterm"); } else if (c == CLEAR) { screen.setString(""); } else if (c == RUN) { nanoContent = screen.getString(); processCommand("xterm"); runScript(nanoContent); } else if (c == IMPORT) { nanoContent = screen.getString(); processCommand("xterm"); importScript("nano"); } else if (c == VIEW) { nanoContent = screen.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }
    private String extractTitle(String htmlContent) { return extractTag(htmlContent, "title", "HTML Viewer"); }
    private String extractTag(String htmlContent, String tag, String fallback) { String startTag = "<" + tag + ">", endTag = "</" + tag + ">"; int start = htmlContent.indexOf(startTag), end = htmlContent.indexOf(endTag); if (start != -1 && end != -1 && end > start) { return htmlContent.substring(start + startTag.length(), end).trim(); } else { return fallback; } }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }
    // |
    // Audio Manager
    private int audio(String command) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("volume")) { if (player != null) { VolumeControl vc = (VolumeControl) player.getControl("VolumeControl"); if (argument.equals("")) { echoCommand("" + vc.getLevel()); } else { try { vc.setLevel(Integer.parseInt(argument)); } catch (Exception e) { echoCommand(e.getMessage()); return 2; } } } else { echoCommand("audio: not running."); return 69; } } else if (mainCommand.equals("play")) { if (argument.equals("")) { } else { if (argument.startsWith("/mnt/")) { argument = argument.substring(5); } else if (argument.startsWith("/")) { echoCommand("audio: invalid source."); return 1; } else { return audio("play " + path + argument); } try { FileConnection CONN = (FileConnection) Connector.open("file:///" + argument, Connector.READ); if (!CONN.exists()) { echoCommand("audio: " + basename(argument) + ": not found"); return 127; } InputStream IN = CONN.openInputStream(); CONN.close(); player = Manager.createPlayer(IN, getMimeType(argument)); player.prefetch(); player.start(); start("audio"); } catch (Exception e) { echoCommand(e.getMessage()); return 1; } } } else if (mainCommand.equals("pause")) { try { if (player != null) { player.stop(); } else { echoCommand("audio: not running."); return 69; } } catch (Exception e) { echoCommand(e.getMessage()); return 1; } } else if (mainCommand.equals("resume")) { try { if (player != null) { player.start(); } else { echoCommand("audio: not running."); return 69; } } catch (Exception e) { echoCommand(e.getMessage()); return 1; } } else if (mainCommand.equals("stop")) { try { if (player != null) { player.stop(); player.close(); player = null; stop("audio"); } else { echoCommand("audio: not running."); return 69; } } catch (Exception e) { echoCommand(e.getMessage()); return 1; } } else if (mainCommand.equals("status")) { echoCommand(player != null ? "true" : "false"); } else { echoCommand("audio: " + mainCommand + ": not found"); return 127; } return 0; } private String getMimeType(String filename) { filename = filename.toLowerCase(); if (filename.endsWith(".amr")) { return "audio/amr"; } else if (filename.endsWith(".wav")) { return "audio/x-wav"; } else { return "audio/mpeg"; } }

    // API 013 - (MIDlet)
    // |
    // Java Runtime
    private int java(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command), argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("Java ME", env("Java 1.2 (OpenTTY Edition)\n\nMicroEdition-Config: $CONFIG\nMicroEdition-Profile: $PROFILE")); }
        else if (mainCommand.equals("-class")) { if (argument.equals("")) { } else { try { Class.forName(argument); echoCommand("true"); } catch (ClassNotFoundException e) { echoCommand("false"); } } } 
        else if (mainCommand.equals("--version")) { echoCommand("Java 1.2 (OpenTTY Edition)"); }
        else { 
            String code = getcontent(mainCommand); 
            Hashtable objects = new Hashtable(); 
            if (code == null || code.length() == 0) { echoCommand("java: " + mainCommand + ": blank class"); return 1; } 

            String[] lines = split(code, ';'); 
            for (int i = 0; i < lines.length; i++) { 
                String line = lines[i].trim(); 
                if (line.length() == 0) { continue; } 
                try { 
                    if (line.indexOf('=') != -1) { 
                        String[] parts = split(line, '='); 
                        String objectName = parts[0].trim(); 
                        String className = parts[1].trim(); 
                        Class clazz = Class.forName(className); 
                        Object instance = clazz.newInstance(); 
                        objects.put(objectName, instance); 
                    } 
                    else if (line.indexOf('.') != -1) { 
                        String[] parts = split(line, '.'); 
                        String objectName = parts[0].trim(); 
                        if (!objects.containsKey(objectName)) { throw new IOException("Object not found"); } 
                        for (int j = 1; j < parts.length; j++) { 
                            Object object = (Object) objects.get(objectName); 
                            Class clazz = object.getClass(); 
                            echoCommand("Invoke method '" + parts[j] + "' on object '" + objectName + "' of class '" + clazz.getName() + "'."); 
                        } 
                    } 
                    else if (line.startsWith("//")) { } 
                    else { throw new IOException("Syntax error"); } 
                } catch (Exception e) { echoCommand(e.getClass().getName() + ": '" + line + "' (" + e.getMessage() + ")"); return 2; } 
            } 
        } 
        
        return 0;
    }
    private boolean
    // |
    // History
    public class History implements CommandListener { private List screen = new List(form.getTitle(), List.IMPLICIT); private Command BACK = new Command("Back", Command.BACK, 1), RUN = new Command("Run", Command.OK, 1), EDIT = new Command("Edit", Command.OK, 1); public History() { screen.addCommand(BACK); screen.addCommand(RUN); screen.addCommand(EDIT); screen.setCommandListener(this); load(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == RUN) { int index = screen.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); processCommand(screen.getString(index)); } } else if (c == EDIT) { int index = screen.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); stdin.setString(screen.getString(index)); } } } private void load() { screen.deleteAll(); for (int i = 0; i < history.size(); i++) { screen.append((String) history.elementAt(i), null); } } }

    // API 015 - (Scripts)
    // |
    // OpenTTY Packages
    private void about(String script) { if (script == null || script.length() == 0) { warnCommand("About", env("OpenTTY $VERSION\n(C) 2025 - Mr. Lima")); return; } Hashtable PKG = parseProperties(getcontent(script)); if (PKG.containsKey("name")) { echoCommand((String) PKG.get("name") + " " + (String) PKG.get("version")); } if (PKG.containsKey("description")) { echoCommand((String) PKG.get("description")); } }
    private int importScript(String script) {
        if (script == null || script.length() == 0) { return 2; }

        Hashtable PKG = parseProperties(getcontent(script));
        // |
        // Verify current API version
        if (PKG.containsKey("api.version")) { if (!env("$VERSION").startsWith((String) PKG.get("api.version"))) { processCommand(PKG.containsKey("api.error") ? (String) PKG.get("api.error") : "true"); return 3; } }
        // |
        // Start and handle APP process
        if (PKG.containsKey("process.name")) { start((String) PKG.get("process.name")); }
        if (PKG.containsKey("process.type")) { String TYPE = (String) PKG.get("process.type"); if (TYPE.equals("server")) { } else if (TYPE.equals("bind")) { new Bind(env((String) PKG.get("process.port") + " " + (String) PKG.get("process.db"))); } else { MIDletLogs("add warn '" + TYPE.toUpperCase() + "' is a invalid value for 'process.type'"); } }
        if (PKG.containsKey("process.host") && PKG.containsKey("process.port")) { new Server(env((String) PKG.get("process.port") + " " + (String) PKG.get("process.host"))); }
        // |
        // Build dependencies
        if (PKG.containsKey("include")) { String[] include = split((String) PKG.get("include"), ','); for (int i = 0; i < include.length; i++) { int STATUS = importScript(include[i]); if (STATUS != 0) { return STATUS; } } }
        // |
        // Start Application
        if (PKG.containsKey("config")) { processCommand((String) PKG.get("config")); }
        if (PKG.containsKey("mod") && PKG.containsKey("process.name")) { final String PROCESS = (String) PKG.get("process.name"); final String MOD = (String) PKG.get("mod"); new Thread("MIDlet-Mod") { public void run() { while (trace.containsKey(PROCESS)) { int STATUS = processCommand(MOD); if (STATUS != 0) { return; } } } }.start(); }
        // |
        // Generate items - Command & Files
        if (PKG.containsKey("command")) { String[] commands = split((String) PKG.get("command"), ','); for (int i = 0; i < commands.length; i++) { if (PKG.containsKey(commands[i])) { aliases.put(commands[i], env((String) PKG.get(commands[i]))); } else { MIDletLogs("add error Failed to create command '" + commands[i] + "' content not found"); } } }
        if (PKG.containsKey("file")) { String[] files = split((String) PKG.get("file"), ','); for (int i = 0; i < files.length; i++) { if (PKG.containsKey(files[i])) { int STATUS = writeRMS("/home/" + files[i], env((String) PKG.get(files[i]))); } else { MIDletLogs("add error Failed to create file '" + files[i] + "' content not found"); } } }
        // |
        // Build APP Shell
        if (PKG.containsKey("shell.name") && PKG.containsKey("shell.args")) { 
            String command = (String) PKG.get("shell.name"); 
            String[] args = split((String) PKG.get("shell.args"), ','); 
            Hashtable TABLE = new Hashtable(); 
            for (int i = 0; i < args.length; i++) { 
                String NAME = args[i].trim(), VALUE = (String) PKG.get(NAME); 
                TABLE.put(NAME, (VALUE != null) ? VALUE : ""); 
            } 
            if (PKG.containsKey("shell.unknown")) { TABLE.put("shell.unknown", (String) PKG.get("shell.unknown")); } 
            shell.put(command, TABLE);
        }

        return 0;
    }
    private int runScript(String script) { String[] CMDS = split(script, '\n'); for (int i = 0; i < CMDS.length; i++) { int STATUS = processCommand(CMDS[i].trim()); if (STATUS != 0) { return STATUS; } } return 0; }

}
