import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import javax.bluetooh.*;
import java.util.*;
import java.io.*;


public class OpenTTY extends MIDlet implements CommandListener {
    private int cursorX = 10, cursorY = 10;
    private Random random = new Random();
    private Runtime runtime = Runtime.getRuntime();
    private Hashtable attributes = new Hashtable(), aliases = new Hashtable(), shell = new Hashtable(),
                      paths = new Hashtable(), desktops = new Hashtable(), trace = new Hashtable(),
                      thr = new Hashtable();
    private String username = loadRMS("OpenRMS", 1);
    private String nanoContent = loadRMS("nano", 1);
    private String logs = "", path = "/", 
                   build = "2025-1.14.2-01x75";
    private Vector stack = new Vector(), 
                   history = new Vector();
    private Display display = Display.getDisplay(this);
    private Form form = new Form("OpenTTY " + getAppProperty("MIDlet-Version"));
    private TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    private StringItem stdout = new StringItem("", "Welcome to OpenTTY " + getAppProperty("MIDlet-Version") + "\nCopyright (C) 2025 - Mr. Lima\n");
    private Command EXECUTE = new Command("Send", Command.OK, 1), HELP = new Command("Help", Command.SCREEN, 2), NANO = new Command("Nano", Command.SCREEN, 3),
                    CLEAR = new Command("Clear", Command.SCREEN, 4), HISTORY = new Command("History", Command.SCREEN, 5);

    public void startApp() {
        if (!trace.containsKey("sh")) {
            attributes.put("PATCH", "Renders Update"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "beta"); attributes.put("XVERSION", "0.6.1");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));

            runScript(read("/java/etc/initd.sh")); stdin.setLabel(username + " " + path + " $");

            if (username.equals("")) { new Login(); }
            else { runScript(loadRMS("initd", 1)); }
        }
    }

    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { writeRMS("nano", nanoContent); }

    public void commandAction(Command c, Displayable d) {
        if (c == EXECUTE) { String command = stdin.getString().trim(); if (!command.equals("")) { history.addElement(command.trim()); } stdin.setString(""); processCommand(command); stdin.setLabel(username + " " + path + " $"); } 

        else if (c == HELP) { processCommand("help"); }
        else if (c == NANO) { new NanoEditor(""); }
        else if (c == CLEAR) { stdout.setText(""); }
        else if (c == HISTORY) { new History(); }

        else if (c.getCommandType() == Command.BACK) { processCommand("xterm"); }
        else if (c.getCommandType() == Command.EXIT) { processCommand("exit"); }
    }

    // OpenTTY Command Processor
    private void processCommand(String command) { processCommand(command, true); }
    private void processCommand(String command, boolean ignore) {
        command = command.startsWith("exec") ? command.trim() : env(command.trim());
        String mainCommand = getCommand(command);
        String argument = getArgument(command);

        if (shell.containsKey(mainCommand) && ignore) { Hashtable args = (Hashtable) shell.get(mainCommand); if (argument.equals("")) { if (aliases.containsKey(mainCommand)) { processCommand((String) aliases.get(mainCommand)); } } else if (args.containsKey(getCommand(argument).toLowerCase())) { processCommand((String) args.get(getCommand(argument)) + " " + getArgument(argument)); } else { if (args.containsKey("shell.unknown")) { processCommand((String) args.get(getCommand("shell.unknown")) + " " + getArgument(argument)); } else { echoCommand(mainCommand + ": " + getCommand(argument) + ": not found"); } } return; }
        if (aliases.containsKey(mainCommand) && ignore) { processCommand((String) aliases.get(mainCommand) + " " + argument); return; }

        if (mainCommand.equals("")) { }

        // API 001 - (Registry)
        // |
        // Aliases
        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        // |
        // Envirronment Keys
        else if (mainCommand.equals("set")) { setCommand(argument); }
        else if (mainCommand.equals("unset")) { unsetCommand(argument); }
        else if (mainCommand.equals("export")) { if (argument.equals("")) { processCommand("env"); } else { attributes.put(argument, ""); } }
        else if (mainCommand.equals("env")) { if (argument.equals("")) { Enumeration keys = attributes.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) attributes.get(key); if (!key.equals("OUTPUT") && !value.equals("")) { echoCommand(key + "=" + value.trim()); } } } else if (attributes.containsKey(argument)) { echoCommand(argument + "=" + (String) attributes.get(argument)); } else { echoCommand("env: " + argument + ": not found"); } }

        // API 002 - (Logs)
        // |
        // OpenTTY Logging Manager
        else if (mainCommand.equals("log")) { MIDletLogs(argument); }
        else if (mainCommand.equals("logcat")) { echoCommand(logs); }

        // API 003 - (User-Integration)
        // |
        // Session
        else if (mainCommand.equals("logout")) { writeRMS("OpenRMS", ""); processCommand("exit"); }
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { echoCommand(username); }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { processCommand("import /java/bin/sh"); }

        // API 004 - (LCDUI Interface)
        // |
        // System UI
        else if (mainCommand.equals("x11")) { xserver(argument); }
        else if (mainCommand.equals("xterm")) { display.setCurrent(form); }
        else if (mainCommand.equals("gauge")) { xserver("gauge " + argument); }
        else if (mainCommand.equals("warn")) { warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("title")) { form.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument); }
        else if (mainCommand.equals("tick")) { if (argument.equals("label")) { echoCommand(display.getCurrent().getTicker().getString()); } else { xserver("tick " + argument); } }

        // API 005 - (Operators)
        // |
        // Operators
        else if (mainCommand.equals("if")) { ifCommand(argument); }
        else if (mainCommand.equals("for")) { forCommand(argument); }
        else if (mainCommand.equals("case")) { caseCommand(argument); }
        // |
        // Long executors
        else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { processCommand(argument, false); }
        else if (mainCommand.equals("bruteforce")) { start("bruteforce"); while (trace.containsKey("bruteforce")) { processCommand(argument); } }
        else if (mainCommand.equals("cron")) { if (argument.equals("")) { } else { processCommand("execute sleep " + getCommand(argument) + "; " + getArgument(argument)); } }
        else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (InterruptedException e) { } catch (NumberFormatException e) { echoCommand(e.getMessage()); } } }
        else if (mainCommand.equals("time")) { if (argument.equals("")) { } else { long startTime = System.currentTimeMillis(); processCommand(argument); long endTime = System.currentTimeMillis(); echoCommand("at " + (endTime - startTime) + "ms"); } }
        // |
        // Chain executors
        else if (mainCommand.equals("exec")) { String[] commands = split(argument, '&'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
        else if (mainCommand.equals("execute")) { String[] commands = split(argument, ';'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }

        // API 006 - (Process)
        // |
        // Memory
        else if (mainCommand.equals("gc")) { System.gc(); }
        else if (mainCommand.equals("htop")) { new HTopViewer(); }
        else if (mainCommand.equals("top")) { if (argument.equals("")) { new HTopViewer(); } else if (argument.equals("used")) { echoCommand("" + (runtime.totalMemory() - runtime.freeMemory()) / 1024); } else if (argument.equals("free")) { echoCommand("" + runtime.freeMemory() / 1024); } else if (argument.equals("total")) { echoCommand("" + runtime.totalMemory() / 1024); } else { echoCommand("top: " + getCommand(argument) + ": not found"); } }
        // |
        // Process
        else if (mainCommand.equals("kill")) { kill(argument); }
        else if (mainCommand.equals("stop")) { stop(argument); }
        else if (mainCommand.equals("start")) { start(argument); }
        else if (mainCommand.equals("ps")) { echoCommand("PID\tPROCESS"); Enumeration keys = trace.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String pid = (String) trace.get(key); echoCommand(pid + "\t" + key); } }
        else if (mainCommand.equals("trace")) { if (argument.equals("")) { } else if (getCommand(argument).equals("pid")) { echoCommand(trace.containsKey(getArgument(argument)) ? (String) trace.get(getArgument(argument)) : "null"); } else if (getCommand(argument).equals("check")) { echoCommand(trace.containsKey(getArgument(argument)) ? "true" : "false"); } else { echoCommand("trace: " + getCommand(argument) + ": not found"); } }

        // API 007 - (Bundle)
        // |
        // MIDlet
        else if (mainCommand.equals("build")) { echoCommand(build); }
        else if (mainCommand.equals("version")) { echoCommand(env("OpenTTY $VERSION")); }
        else if (mainCommand.equals("vendor")) { echoCommand(getAppProperty("MIDlet-Vendor")); }
        else if (mainCommand.equals("github")) { processCommand("open " + getAppProperty("MIDlet-Info-URL")); }
        else if (mainCommand.equals("pkg")) { echoCommand(argument.equals("") ? getAppProperty("MIDlet-Name") : argument.startsWith("/") ? System.getProperty(replace(argument, "/", "")) : getAppProperty(argument)); }
        // |
        // Device
        else if (mainCommand.equals("hostname")) { echoCommand(env("$HOSTNAME")); }
        else if (mainCommand.equals("uname")) { echoCommand(env("$TYPE $CONFIG $PROFILE")); }
        else if (mainCommand.equals("hostid")) { String data = System.getProperty("microedition.platform") + System.getProperty("microedition.configuration") + System.getProperty("microedition.profiles"); int hash = 7; for (int i = 0; i < data.length(); i++) { hash = hash * 31 + data.charAt(i); } echoCommand(Integer.toHexString(hash).toLowerCase()); }

        // API 008 - (Logic I/O) Text
        // |
        // TTY
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(stdout.getText().length() + " B"); }
        // |
        // Text related commands
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("buff")) { stdin.setString(argument); }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
        else if (mainCommand.equals("getopt")) { echoCommand(getArgument(argument)); }
        else if (mainCommand.equals("trim")) { stdout.setText(stdout.getText().trim()); }
        else if (mainCommand.equals("date")) { echoCommand(new java.util.Date().toString()); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { stdout.setText(""); }
        else if (mainCommand.equals("seed")) { try { echoCommand("" +  random.nextInt(Integer.parseInt(argument)) + ""); } catch (NumberFormatException e) { echoCommand(e.getMessage()); } }

        // API 009 - (Threads)
        // |
        // MIDlet Tracker
        else if (mainCommand.equals("throw")) { Thread.currentThread().interrupt(); if (argument.equals("")) { } else { MIDletLogs("add warn " + argument); } }
        else if (mainCommand.equals("mmspt")) { echoCommand(replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet")); }
        else if (mainCommand.equals("bg")) { final String bgCommand = argument; new Thread(new Runnable() { public void run() { processCommand(bgCommand); } }, "Background").start(); }

        // API 010 - (Requests)
        // |
        // Connecting to Device API
        else if (mainCommand.equals("call")) { if (argument.equals("")) { } else { try { platformRequest("tel:" + argument); } catch (Exception e) { } } }
        else if (mainCommand.equals("open")) { if (argument.equals("")) { } else { try { platformRequest(argument); } catch (Exception e) { echoCommand("open: " + argument + ": not found"); } } }
        // |
        // PushRegistry
        else if (mainCommand.equals("prg")) { if (argument.equals("")) { argument = "5"; } try { PushRegistry.registerAlarm(getArgument(argument).equals("") ? "OpenTTY" : getArgument(argument), System.currentTimeMillis() + Integer.parseInt(getCommand(argument)) * 1000); } catch (NumberFormatException e) { echoCommand(e.getMessage()); } catch (ClassNotFoundException e) { echoCommand(e.getMessage()); } catch (Exception e) { echoCommand("AutoRunError: " + e.getMessage()); } }

        // API 011 - (Network)
        // |
        // Servers
        else if (mainCommand.equals("bind")) { new Bind(env("$PORT")); }
        else if (mainCommand.equals("server")) { new Server(env("$PORT $RESPONSE")); }
        // |
        // HTTP Interfaces
        else if (mainCommand.equals("pong")) { pongCommand(argument); }
        else if (mainCommand.equals("ping")) { pingCommand(argument); }
        else if (mainCommand.equals("http")) { new InjectorHTTP(argument); }
        else if (mainCommand.equals("gobuster")) { new GoBuster(argument); }
        else if (mainCommand.equals("curl")) { if (argument.equals("")) { } else { echoCommand(request(argument)); } }
        else if (mainCommand.equals("wget")) { if (argument.equals("")) { } else { nanoContent = request(argument); } }
        else if (mainCommand.equals("clone")) { if (argument.equals("")) { } else { runScript(request(getAppProperty("MIDlet-Proxy") + argument)); } }
        else if (mainCommand.equals("proxy")) { if (argument.equals("")) { } else { nanoContent = request(getAppProperty("MIDlet-Proxy") + argument); } }
        // |
        // Socket Interfaces
        else if (mainCommand.equals("query")) { query(argument); }
        else if (mainCommand.equals("gaddr")) { new GetAddress(argument); }
        else if (mainCommand.equals("prscan")) { new PortScanner(argument); }
        else if (mainCommand.equals("nc")) { new RemoteConnection(argument); }
        else if (mainCommand.equals("wrl")) { echoCommand(System.getProperty("wireless.messaging.sms.smsc")); }
        // |
        // IP Tools
        else if (mainCommand.equals("fw")) { echoCommand(request("http://ipinfo.io/" + (argument.equals("") ? "json" : argument))); }
        else if (mainCommand.equals("org")) { echoCommand(request("http://ipinfo.io/" + (argument.equals("") ? "org" : argument + "/org"))); }
        else if (mainCommand.equals("genip")) { echoCommand(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256)); }
        else if (mainCommand.equals("ifconfig")) { if (argument.equals("")) { argument = "1.1.1.1:53"; } try { SocketConnection socketConnection = (SocketConnection) Connector.open("socket://" + argument); echoCommand(socketConnection.getLocalAddress()); socketConnection.close(); } catch (IOException e) { echoCommand("null"); } }
        // |
        else if (mainCommand.equals("report")) { processCommand("open mailto:felipebr4095@gmail.com"); }
        else if (mainCommand.equals("mail")) { echoCommand(request(getAppProperty("MIDlet-Proxy") + "raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/root/mail.txt")); } 
        else if (mainCommand.equals("netstat")) { try { HttpConnection conn = (HttpConnection) Connector.open("http://ipinfo.io/ip"); conn.setRequestMethod(HttpConnection.GET); if (conn.getResponseCode() == HttpConnection.HTTP_OK) { echoCommand("true"); } else { echoCommand("false"); } conn.close(); } catch (Exception e) { echoCommand("false"); } }

        // API 012 - (File)
        // |
        // Directories Manager
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("umount")) { paths = new Hashtable(); }
        else if (mainCommand.equals("local")) { new FileExplorer(argument); }
        else if (mainCommand.equals("ls")) { viewer("Resources", read("/java/resources.txt")); }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { mount(getcontent(argument)); } }
        else if (mainCommand.equals("cd")) { if (argument.equals("")) { path = "/"; } else { if (argument.startsWith("/")) { if (paths.containsKey(argument)) { path = argument; } else { echoCommand("cd: " + basename(argument) + ": not found"); } } else if (argument.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { processCommand(path.equals("/") ? "cd " + "/" + argument : "cd " + path + "/" + argument); } } }
        else if (mainCommand.equals("pushd")) { if (argument.equals("")) { echoCommand(readStack() == null || readStack().length() == 0 ? "pushd: missing directory": readStack()); } else { if (!paths.containsKey(argument)) { echoCommand("pushd: " + argument + ": not found"); } else { stack.addElement(path); path = argument; echoCommand(readStack()); } } }
        else if (mainCommand.equals("popd")) { if (stack.isEmpty()) { echoCommand("popd: stack empty"); } else { path = (String) stack.lastElement(); stack.removeElementAt(stack.size() - 1); echoCommand(readStack()); } }
        else if (mainCommand.equals("dir")) { 
            if (argument.equals("f")) { new Explorer(); } 
            else if (argument.equals("s")) { new FileExplorer(""); } 
            else if (argument.equals("v")) { 
                try { 
                    String[] recordStores = RecordStore.listRecordStores(); 
                    if (recordStores != null) { 
                        for (int i = 0; i < recordStores.length; i++) { 
                            if (recordStores[i].startsWith(".")) { } 
                            else { echoCommand(recordStores[i]); } 
                        } 
                    } 
                } catch (RecordStoreException e) { } } 
            else {
    String base = argument.startsWith("/") ? argument : (argument == null || argument.length() == 0 ? path : (path.endsWith("/") ? path + argument : path + "/" + argument));
    if (!base.endsWith("/")) base += "/";

    String[] entries = split(read("/java/resources.txt"), '\n');
    Vector results = new Vector();

    for (int i = 0; i < entries.length; i++) {
        String entry = entries[i].trim();
        if (!entry.startsWith(base)) continue;

        String relative = entry.substring(base.length());
        if (relative.length() == 0) continue;

        int slashIndex = relative.indexOf('/');
        if (slashIndex == -1) {
            if (!results.contains(relative)) results.addElement(relative); // arquivo
        } else {
            String subdir = relative.substring(0, slashIndex);
            if (!results.contains(subdir + "/")) results.addElement(subdir + "/"); // subpasta
        }
    }

    if (results.isEmpty()) {
        echoCommand("dir: " + base + ": not found");
    } else {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < results.size(); i++) {
            sb.append(results.elementAt(i)).append("\t");
        }
        echoCommand(sb.toString().trim());
    }
}
        }        
        // |
        // Device Files
        else if (mainCommand.equals("fdisk")) { processCommand("lsblk -p"); }
        else if (mainCommand.equals("lsblk")) { boolean hascard = false; StringBuffer roots = new StringBuffer(); Enumeration storage = FileSystemRegistry.listRoots(); while (storage.hasMoreElements()) { String root = (String) storage.nextElement(); roots.append(root).append("\n"); if (root.toLowerCase().indexOf("card") != -1 || root.toLowerCase().indexOf("sd") != -1 || root.toLowerCase().indexOf("e:") != -1 || root.toLowerCase().indexOf("d:") != -1) { hascard = true; } } if (argument.equals("")) { echoCommand("OpenTTY\n| MIDlet\n| RMS\nDevice\n| Storage" + (hascard == true ? "\n| SD Card" : "")); } else if (argument.equals("-x")) { echoCommand("MIDlet;RMS;Storage;" + (hascard == true ? "SD Card;" : "")); } else if (argument.equals("-p")) { echoCommand(roots.toString()); } else { echoCommand("lsblk: " + argument + ": not found"); } }
        else if (mainCommand.equals("dd")) { if (argument.equals("")) { } else { try { String[] args = split(argument, ' '); FileConnection conn = (FileConnection) Connector.open("file:///" + args[0], Connector.READ_WRITE); if (!conn.exists()) conn.create(); OutputStream os = conn.openOutputStream(); String content = args[1]; os.write(getcontent(content).getBytes()); os.flush(); echoCommand("operation finish"); } catch (IOException e) { echoCommand(e.getMessage()); } } }
        // |
        // RMS Files
        else if (mainCommand.equals("rm")) { deleteFile(argument); }
        else if (mainCommand.equals("install")) { if (argument.equals("")) { } else { writeRMS(argument, nanoContent); } }
        else if (mainCommand.equals("touch") || mainCommand.equals("rnano")) { if (argument.equals("")) { nanoContent = ""; } else { writeRMS(argument, ""); } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { writeRMS(getArgument(argument).equals("") ? getCommand(argument) + "-copy" : getArgument(argument), loadRMS(getCommand(argument), 1)); } }
        // |
        // Text Manager
        else if (mainCommand.equals("sed")) { StringEditor(argument); }
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("rraw")) { stdout.setText(nanoContent); }
        else if (mainCommand.equals("getty")) { nanoContent = stdout.getText(); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } 
        else if (mainCommand.equals("hash")) { if (argument.equals("")) { } else { echoCommand("" + getcontent(argument).hashCode()); } }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { echoCommand(read(argument)); } else { echoCommand(loadRMS(argument, 1)); } } }
        else if (mainCommand.equals("get")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { nanoContent = read(argument); } else { nanoContent = loadRMS(argument, 1); } } }
        else if (mainCommand.equals("read")) { if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); attributes.put(args[0], getcontent(args[1])); } }
        else if (mainCommand.equals("grep")) { if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); echoCommand(getcontent(args[1]).indexOf(args[0]) != -1 ? "true" : "false"); } }
        else if (mainCommand.equals("find")) { if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); String file = getcontent(args[1]), value = (String) parseProperties(file).get(args[0]); echoCommand(value != null ? value : "null"); } }
        else if (mainCommand.equals("head")) { if (argument.equals("")) { } else { String content = getcontent(argument); String[] lines = split(content, '\n'); int count = Math.min(10, lines.length); for (int i = 0; i < count; i++) { echoCommand(lines[i]); } } }
        else if (mainCommand.equals("tail")) { if (argument.equals("")) { } else { String content = getcontent(argument); String[] lines = split(content, '\n'); int start = Math.max(0, lines.length - 10); for (int i = start; i < lines.length; i++) { echoCommand(lines[i]); } } }
        // |
        // Text Parsers
        else if (mainCommand.equals("pjnc")) { nanoContent = parseJson(nanoContent); }
        else if (mainCommand.equals("pinc")) { nanoContent = parseConf(nanoContent); }
        else if (mainCommand.equals("conf")) { echoCommand(parseConf(argument.equals("") ? nanoContent : getcontent(argument))); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(argument.equals("") ? nanoContent : getcontent(argument))); }
        else if (mainCommand.equals("vnt")) { if (argument.equals("")) { } else { String in = getcontent(getCommand(argument)); String out = getArgument(argument); if (out.equals("")) { nanoContent = text2note(in); } else { writeRMS(out, text2note(in)); } } }
        else if (mainCommand.equals("ph2s")) { StringBuffer script = new StringBuffer(); for (int i = 0; i < history.size() - 1; i++) { script.append(history.elementAt(i)); if (i < history.size() - 1) { script.append("\n"); } } if (argument.equals("") || argument.equals("nano")) { nanoContent = "#!/java/bin/sh\n\n" + script.toString(); } else { writeRMS(argument, "#!/java/bin/sh\n\n" + script.toString()); } }
        // |
        // Interfaces
        else if (mainCommand.equals("nano")) { new NanoEditor(argument); }
        else if (mainCommand.equals("html")) { viewer(extractTitle(env(nanoContent)), html2text(env(nanoContent))); }
        else if (mainCommand.equals("view")) { if (argument.equals("")) {  } else { viewer(extractTitle(env(argument)), html2text(env(argument))); } }
        // |
        else if (mainCommand.equals("du")) { if (argument.equals("")) { } else { echoCommand("" + getcontent(argument).length() + "\t" + basename(argument)); } }

        // API 013 - (MIDlet)
        // |
        // General Utilities
        else if (mainCommand.equals("java")) { java(argument); }
        else if (mainCommand.equals("chmod")) { chmod(argument); }
        else if (mainCommand.equals("history")) { new History(); }
        else if (mainCommand.equals("forget")) { history = new Vector(); }
        else if (mainCommand.equals("debug")) { runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("help")) { viewer(form.getTitle(), read("/java/help.txt")); }
        else if (mainCommand.equals("true") || mainCommand.equals("false") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("exit") || mainCommand.equals("quit")) { writeRMS("nano", nanoContent); notifyDestroyed(); }

        //else if (mainCommand.equals("")) {  }
        //else if (mainCommand.equals("")) {  }
        //else if (mainCommand.equals("")) {  }
        else if (mainCommand.equals("lang")) {  }
        else if (mainCommand.equals("track")) {  }
        else if (mainCommand.equals("catch")) {  }
        else if (mainCommand.equals("neofetch")) {  }

        // API 014 - (OpenTTY)
        // |
        // Low-level commands
        else if (mainCommand.equals("@exec")) { commandAction(EXECUTE, display.getCurrent()); }
        else if (mainCommand.equals("@login")) { if (argument.equals("")) { username = loadRMS("OpenRMS", 1); } else { username = argument; } }
        else if (mainCommand.equals("@alert")) { try { display.vibrate(argument.equals("") ? 500 : Integer.parseInt(argument) * 100); } catch (NumberFormatException e) { echoCommand(e.getMessage()); } }
        else if (mainCommand.equals("@reload")) { shell = new Hashtable(); aliases = new Hashtable(); username = loadRMS("OpenRMS", 1); MIDletLogs("add debug API reloaded"); processCommand("execute x11 stop; x11 init; x11 term; run initd; sh;"); }
        else if (mainCommand.startsWith("@")) { }

        // API 015 - (Scripts)
        // |
        // OpenTTY Packages
        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { importScript(argument); }
        else if (mainCommand.equals("run")) { processCommand(". " + argument, false); }

        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE"));  }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { runScript(nanoContent); } else { runScript(getcontent(argument)); } }

        else { echoCommand(mainCommand + ": not found"); }

    }

    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return getpattern(input.substring(spaceIndex + 1).trim()); } }

    private String read(String filename) { try { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } catch (IOException e) { return e.getMessage(); } }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0; int end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); Enumeration e = attributes.keys(); while (e.hasMoreElements()) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } text = replace(text, "$.", "$"); text = replace(text, "\\.", "\\"); return text; }
    
    private String getcontent(String file) { return file.startsWith("/") ? read(file) : file.equals("nano") ? nanoContent : loadRMS(file, 1); }
    private String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }

    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }

    public class Login implements CommandListener { private Form screen = new Form("Login"); private TextField userField = new TextField("Username", "", 256, TextField.ANY); private Command loginCommand = new Command("Login", Command.OK, 1), exitCommand = new Command("Exit", Command.SCREEN, 2); public Login() { screen.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\nCreate an user to access OpenTTY!")); screen.append(userField); screen.addCommand(loginCommand); screen.addCommand(exitCommand); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == loginCommand) { username = userField.getString(); if (username.equals("")) { } else { writeRMS("OpenRMS", username); display.setCurrent(form); runScript(loadRMS("initd", 1)); } } else if (c == exitCommand) { processCommand("exit"); } } }

    // API 001 - (Registry)
    // |
    // Aliases
    private void aliasCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (argument.equals("")) { Enumeration keys = aliases.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) aliases.get(key); if (!key.equals("xterm") && !value.equals("")) { echoCommand("alias " + key + "='" + value.trim() + "'"); } } } else if (aliases.containsKey(argument)) { echoCommand("alias " + argument + "='" + (String) aliases.get(argument) + "'"); } else { echoCommand("alias: " + argument + ": not found"); } return; } aliases.put(argument.substring(0, equalsIndex).trim(), argument.substring(equalsIndex + 1).trim()); }
    private void unaliasCommand(String key) { if (key == null || key.length() == 0) { return; } if (aliases.containsKey(key)) { aliases.remove(key); } else { echoCommand("unalias: " + key + ": not found"); } }
    // |
    // Envirronment Keys
    private void setCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (attributes.containsKey(argument)) { echoCommand(argument + "=" + (String) attributes.get(argument)); } else { /* Exportation File */ } return; } String key = argument.substring(0, equalsIndex).trim(); String value = argument.substring(equalsIndex + 1).trim(); attributes.put(key, value); }
    private void unsetCommand(String key) { if (key == null || key.length() == 0) { return; } if (attributes.containsKey(key)) { attributes.remove(key); } }

    // API 002 - (Logs)
    // |
    // OpenTTY Logging Manager
    private void MIDletLogs(String command) { command = env(command.trim()); String mainCommand = getCommand(command); String argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { writeRMS(argument.equals("") ? "logs" : argument, logs); } else if (mainCommand.equals("view")) { viewer(form.getTitle(), logs); } else if (mainCommand.equals("add")) { if (argument.equals("")) { return; } else if (getCommand(argument).toLowerCase().equals("info")) { if (!getArgument(command).equals("")) { logs = logs + "[INFO] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("warn")) { if (!getArgument(command).equals("")) { logs = logs + "[WARN] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("debug")) { if (!getArgument(command).equals("")) { logs = logs + "[DEBUG] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("error")) { if (!getArgument(command).equals("")) { logs = logs + "[ERROR] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else { echoCommand("log: add: " + getCommand(argument).toLowerCase() + ": level not found"); } } else { echoCommand("log: " + mainCommand + ": not found"); } }

    // API 004 - (LCDUI Interface)
    // |
    // System UI
    private void xserver(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command);
        String argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2025-05-04\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); }
        else if (mainCommand.equals("title")) { display.getCurrent().setTitle(argument); }
        else if (mainCommand.equals("term")) { display.setCurrent(form); }
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("buffer")) { echoCommand("" + display.getCurrent().getWidth() + "x" + display.getCurrent().getHeight() + ""); }
        else if (mainCommand.equals("stop")) { form.setTitle(""); form.setTicker(null); form.deleteAll(); xserver("cmd hide"); form.removeCommand(EXECUTE); }
        else if (mainCommand.equals("tick")) { Displayable current = display.getCurrent(); if (argument.equals("")) { current.setTicker(null); } else { current.setTicker(new Ticker(argument)); } }
        else if (mainCommand.equals("init")) { form.setTitle(env("OpenTTY $VERSION")); form.append(stdout); form.append(stdin); form.addCommand(EXECUTE); xserver("cmd"); form.setCommandListener(this); }
        else if (mainCommand.equals("cmd")) { if (argument.equals("hide")) { form.removeCommand(HELP); form.removeCommand(NANO); form.removeCommand(CLEAR); form.removeCommand(HISTORY); } else { form.addCommand(HELP); form.addCommand(NANO); form.addCommand(CLEAR); form.addCommand(HISTORY); } }
        else if (mainCommand.equals("font")) { if (argument.equals("")) { xserver("font default"); } else { stdout.setFont(newFont(argument)); } }
        else if (mainCommand.equals("canvas")) { display.setCurrent(new MyCanvas(argument.equals("") ? "OpenRMS" : argument)); }

        else if (mainCommand.equals("xfinit")) { if (argument.equals("")) { xserver("init"); } if (argument.equals("stdin")) { form.append(stdin); } else if (argument.equals("stdout")) { form.append(stdout); } else { xserver("init"); } }
        else if (mainCommand.equals("gauge")) { Alert alert = new Alert(form.getTitle(), argument, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING)); display.setCurrent(alert); }

        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { desktops.put(argument, display.getCurrent()); } }
        else if (mainCommand.equals("load")) { if (argument.equals("")) { } else { if (desktops.containsKey(argument)) { display.setCurrent((Displayable) desktops.get(argument)); } else { echoCommand("x11: load: " + argument + ": not found"); } } }

        else if (mainCommand.equals("make")) { new Screen(argument); }
        else if (mainCommand.equals("list")) { new ScreenList(argument); }
        else if (mainCommand.equals("item")) { new ItemLoader(argument); }
        else if (mainCommand.equals("quest")) { new ScreenQuest(argument); }

        else { echoCommand("x11: " + mainCommand + ": not found"); }
    }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); }
    private void viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(new StringItem(null, env(text))); viewer.addCommand(new Command("Back", Command.BACK, 1)); viewer.setCommandListener(this); display.setCurrent(viewer); }
    // |
    public class Screen implements CommandListener { private Hashtable lib; private Form screen; private StringItem content; private Command backCommand, userCommand; public Screen(String args) { if (args == null || args.length() == 0) { return; } lib = parseProperties(getcontent(args)); if (!lib.containsKey("screen.title")) { MIDletLogs("add error Screen crashed while init, malformed settings"); return; } screen = new Form(env((String) lib.get("screen.title"))); content = new StringItem("", lib.containsKey("screen.content") ? env((String) lib.get("screen.content")) : ""); if (lib.containsKey("screen.content.style")) { content.setFont(newFont((String) lib.get("screen.content.style"))); } backCommand = new Command(lib.containsKey("screen.back.label") ? env((String) lib.get("screen.back.label")) : "Back", Command.OK, 1); userCommand = new Command(lib.containsKey("screen.button") ? env((String) lib.get("screen.button")) : "Menu", Command.SCREEN, 2); screen.append(content); screen.addCommand(backCommand); screen.addCommand(userCommand); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == backCommand) { processCommand("xterm"); processCommand(lib.containsKey("screen.back") ? (String) lib.get("screen.back") : "true"); } else if (c == userCommand) { processCommand("xterm"); processCommand(lib.containsKey("screen.button.cmd") ? (String) lib.get("screen.button.cmd") : "log add warn An error occurred, 'screen.button.cmd' not found"); } } }
    public class ScreenList implements CommandListener { private Hashtable lib; private List screen; private Command backCommand, userCommand; public ScreenList(String args) { if (args == null || args.length() == 0) { return; } lib = parseProperties(getcontent(args)); if (!lib.containsKey("list.title") && !lib.containsKey("list.content")) { MIDletLogs("add error List crashed while init, malformed settings"); return; } screen = new List(env((String) lib.get("list.title")), List.IMPLICIT); backCommand = new Command(lib.containsKey("list.back.label") ? env((String) lib.get("list.back.label")) : "Back", Command.OK, 1); userCommand = new Command(lib.containsKey("list.button") ? env((String) lib.get("list.button")) : "Select", Command.SCREEN, 2); String[] content = split(env((String) lib.get("list.content")), ','); for (int i = 0; i < content.length; i++) { screen.append(content[i], null); } screen.addCommand(backCommand); screen.addCommand(userCommand); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == backCommand) { processCommand("xterm"); processCommand(lib.containsKey("list.back") ? env((String) lib.get("list.back")) : "true"); } else if (c == userCommand) { int index = screen.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); processCommand(lib.containsKey(screen.getString(index)) ? (String) lib.get(env(screen.getString(index))) : "log add warn An error occurred, '" + env(screen.getString(index)) + "' not found"); } } } }
    public class ScreenQuest implements CommandListener { private Hashtable lib; private Form screen; private TextField content; private Command backCommand, userCommand; public ScreenQuest(String args) { if (args == null || args.length() == 0) { return; } lib = parseProperties(getcontent(args)); if (!lib.containsKey("quest.title") || !lib.containsKey("quest.label") || !lib.containsKey("quest.cmd") || !lib.containsKey("quest.key")) { MIDletLogs("add error Quest crashed while init, malformed settings"); return; } screen = new Form(env((String) lib.get("quest.title"))); content = new TextField(env((String) lib.get("quest.label")), "", 256, TextField.ANY); backCommand = new Command(lib.containsKey("quest.back.label") ? env((String) lib.get("quest.back.label")) : "Cancel", Command.SCREEN, 2); userCommand = new Command("Send", Command.OK, 1); screen.append(content); screen.addCommand(backCommand); screen.addCommand(userCommand); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) {if (c == backCommand) { processCommand("xterm"); processCommand(lib.containsKey("quest.back") ? env((String) lib.get("quest.back")) : "true"); } else if (c == userCommand) { if (!content.getString().trim().equals("")) { processCommand("set " + env((String) lib.get("quest.key")) + "=" + env(content.getString().trim())); processCommand("xterm"); processCommand((String) lib.get("quest.cmd")); } } } }
    public class ItemLoader implements ItemCommandListener { private Hashtable lib; private Command run; private StringItem s; public ItemLoader(String args) { if (args == null || args.length() == 0) { return; } else if (args.equals("clear")) { form.deleteAll(); form.append(stdout); form.append(stdin); return; } lib = parseProperties(getcontent(args)); if (!lib.containsKey("item.label") || !lib.containsKey("item.cmd")) { MIDletLogs("add error Malformed ITEM, missing params"); return; } run = new Command((String) lib.get("item.label"), Command.ITEM, 1); s = new StringItem(null, env((String) lib.get("item.label")), StringItem.BUTTON); s.setFont(Font.getDefaultFont()); s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(run); s.setDefaultCommand(run); s.setItemCommandListener(this); form.append(s); } public void commandAction(Command c, Item item) { if (c == run) { processCommand("xterm"); processCommand((String) lib.get("item.cmd")); } } }
    // |
    public class MyCanvas extends Canvas implements CommandListener { private Hashtable lib; private Graphics screen; private Command backCommand, userCommand; private final int cursorSize = 5; public MyCanvas(String args) { if (args == null || args.length() == 0) { return; } lib = parseProperties(getcontent(args)); backCommand = new Command(lib.containsKey("canvas.back.label") ? env((String) lib.get("canvas.back.label")) : "Back", Command.OK, 1); userCommand = new Command(lib.containsKey("canvas.button") ? env((String) lib.get("canvas.button")) : "Menu", Command.SCREEN, 2); addCommand(backCommand); if (lib.containsKey("canvas.button")) { addCommand(userCommand); } if (lib.containsKey("canvas.mouse")) { try { cursorX = Integer.parseInt(split((String) lib.get("canvas.mouse"), ',')[0]); cursorY = Integer.parseInt(split((String) lib.get("canvas.mouse"), ',')[1]); } catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas.mouse' - (x,y) may be a int number"); cursorX = 10; cursorY = 10; } } setCommandListener(this); } protected void paint(Graphics g) { if (screen == null) { screen = g; } g.setColor(0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); if (lib.containsKey("canvas.background")) { String backgroundType = lib.containsKey("canvas.background.type") ? env((String) lib.get("canvas.background.type")) : "default"; if (backgroundType.equals("color") || backgroundType.equals("default")) { try { g.setColor(Integer.parseInt(split((String) lib.get("canvas.background"), ',')[0]), Integer.parseInt(split((String) lib.get("canvas.background"), ',')[1]), Integer.parseInt(split((String) lib.get("canvas.background"), ',')[2])); } catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas.background' - (x,y,z) may be a int number"); g.setColor(0, 0, 0); } g.fillRect(0, 0, getWidth(), getHeight());  } else if (backgroundType.equals("image")) { try { Image content = Image.createImage(env((String) lib.get("canvas.background"))); g.drawImage(content, (getWidth() - content.getWidth()) / 2, (getHeight() - content.getHeight()) / 2, Graphics.TOP | Graphics.LEFT); } catch (IOException e) { processCommand("xterm"); processCommand("execute log add error Malformed Image, " + e.getMessage()); } } } if (lib.containsKey("canvas.title")) { g.setColor(50, 50, 50); g.fillRect(0, 0, getWidth(), 30); g.setColor(255, 255, 255); g.drawString(env((String) lib.get("canvas.title")), getWidth() / 2, 5, Graphics.TOP | Graphics.HCENTER); g.setColor(50, 50, 50);  g.drawRect(0, 0, getWidth() - 1, getHeight() - 1); g.drawRect(1, 1, getWidth() - 3, getHeight() - 3); } if (lib.containsKey("canvas.content")) { String contentType = lib.containsKey("canvas.content.type") ? env((String) lib.get("canvas.content.type")) : "default"; g.setFont(Font.getDefaultFont()); if (lib.containsKey("canvas.content.style")) { g.setFont(newFont((String) lib.get("canvas.content.style"))); } if (contentType.equals("text") || contentType.equals("default")) { g.setColor(255, 255, 255); String content = env((String) lib.get("canvas.content")); int contentWidth = g.getFont().stringWidth(content); int contentHeight = g.getFont().getHeight(); g.drawString(content, (getWidth() - contentWidth) / 2, (getHeight() - contentHeight) / 2, Graphics.TOP | Graphics.LEFT); } else if (contentType.equals("shape")) { String[] shapes = split(env((String) lib.get("canvas.content")), ';'); for (int i = 0; i < shapes.length; i++) { String[] parts = split(shapes[i], ','); String type = parts[0].toLowerCase(); if (type.equals("line") && parts.length == 5) { g.setColor(255, 255, 255); g.drawLine(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])); } else if (type.equals("circle") && parts.length == 4) { g.setColor(0, 255, 0); int radius = Integer.parseInt(parts[3]); g.drawArc(Integer.parseInt(parts[1]) - radius, Integer.parseInt(parts[2]) - radius, radius * 2, radius * 2, 0, 360); } else if (type.equals("rect") && parts.length == 5) { g.setColor(0, 0, 255); g.drawRect(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4])); } else if (type.equals("text") && parts.length == 4) { g.setColor(255, 255, 255); g.drawString(parts[3], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Graphics.TOP | Graphics.LEFT); } } } } g.setColor(255, 255, 255); g.fillRect(cursorX, cursorY, cursorSize, cursorSize); } protected void keyPressed(int keyCode) { int gameAction = getGameAction(keyCode); if (gameAction == LEFT) { cursorX = Math.max(0, cursorX - 5); } else if (gameAction == RIGHT) { cursorX = Math.min(getWidth() - cursorSize, cursorX + 5); } else if (gameAction == UP) { cursorY = Math.max(0, cursorY - 5); } else if (gameAction == DOWN) { cursorY = Math.min(getHeight() - cursorSize, cursorY + 5); } else if (gameAction == FIRE) { if (lib.containsKey("canvas.content")) { String content = env((String) lib.get("canvas.content")); int contentWidth = screen.getFont().stringWidth(content); int contentHeight = screen.getFont().getHeight(); int textX = (getWidth() - contentWidth) / 2; int textY = (getHeight() - contentHeight) / 2; if (cursorX >= textX && cursorX <= textX + contentWidth && cursorY >= textY && cursorY <= textY + contentHeight) { processCommand(lib.containsKey("canvas.content.link") ? (String) lib.get("canvas.content.link") : "true"); } } } repaint(); } protected void pointerPressed(int x, int y) { cursorX = x; cursorY = y; repaint(); } public void commandAction(Command c, Displayable d) { if (c == backCommand) { processCommand("xterm"); processCommand(lib.containsKey("canvas.back") ? (String) lib.get("canvas.back") : "true"); } else if (c == userCommand) { processCommand("xterm"); processCommand(lib.containsKey("canvas.button.cmd") ? (String) lib.get("canvas.button.cmd") : "log add warn An error occurred, 'canvas.button.cmd' not found"); } } }
    // |
    // Font Generator
    private Font newFont(String style) { if (style == null || style.length() == 0 || style.equals("default")) { return Font.getDefaultFont(); } else if (style.equals("bold")) { return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_MEDIUM); } else if (style.equals("italic")) { return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_ITALIC, Font.SIZE_MEDIUM); } else if (style.equals("ul")) { return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_UNDERLINED, Font.SIZE_MEDIUM); } else if (style.equals("small")) { return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL); } else if (style.equals("large")) { return Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_LARGE); } else { return newFont("default"); } }

    // API 005 - (Operators)
    // |
    // Operators
    private void ifCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('); int lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("if (expr) [command]"); return; } String expression = argument.substring(firstParenthesis + 1, lastParenthesis).trim(); String command = argument.substring(lastParenthesis + 1).trim(); String[] parts = split(expression, ' '); if (parts.length == 3) { if (parts[1].equals("startswith")) { if (parts[0].startsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("!startswith")) { if (!parts[0].startsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("endswith")) { if (parts[0].endsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("!endswith")) { if (!parts[0].endsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("contains")) { if (parts[0].indexOf(parts[2]) != -1) { processCommand(command); } } else if (parts[1].equals("!contains")) { if (parts[0].indexOf(parts[2]) == -1) { processCommand(command); } } else if (parts[1].equals("!=")) { if (!parts[0].equals(parts[2])) { processCommand(command); } } else if (parts[1].equals("==")) { if (parts[0].equals(parts[2])) { processCommand(command); } } } else if (parts.length == 2) { if (parts[0].equals(parts[1])) { processCommand(command); } } else if (parts.length == 1) { if (!parts[0].equals("")) { processCommand(command); } } }
    private void forCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('); int lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return; } String key = getCommand(argument); String file = argument.substring(firstParenthesis + 1, lastParenthesis).trim(); String command = argument.substring(lastParenthesis + 1).trim(); if (key.startsWith("(")) { return; } if (key.startsWith("$")) { key = replace(key, "$", ""); } if (file.startsWith("/")) { file = read(file); } else if (file.equals("nano")) { file = nanoContent; } else { file = loadRMS(file, 1); } String[] lines = split(file, '\n'); for (int i = 0; i < lines.length; i++) { if (lines[i] != null || lines[i].length() == 0) { processCommand("set " + key + "=" + lines[i]); processCommand(command); processCommand("unset " + key); } } }
    private void caseCommand(String argument) {
        argument = argument.trim();

        int firstParenthesis = argument.indexOf('(');
        int lastParenthesis = argument.indexOf(')');

        if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return; }

        String method = getCommand(argument);
        String expression = argument.substring(firstParenthesis + 1, lastParenthesis).trim();
        String command = argument.substring(lastParenthesis + 1).trim();

        if (method.equals("file")) { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(expression)) { processCommand(command); } } } }
        else if (method.equals("root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(expression)) { processCommand(command); } } }
        else if (method.equals("thread")) { if (replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet").equals(expression)) { processCommand(command); } }
        else if (method.equals("trace")) { if (trace.containsKey(expression)) { processCommand(command); } }
        else if (method.equals("screen")) { if (desktops.containsKey(expression)) { processCommand(command); } }
        else if (method.equals("alias")) { if (aliases.containsKey(expression)) { processCommand(command); } }
        else if (method.equals("key")) { if (attributes.containsKey(expression)) { processCommand(command); } }

        else if (method.equals("!file")) { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(expression)) { return; } } } processCommand(command); }
        else if (method.equals("!root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(expression)) { return; } } processCommand(command); }
        else if (method.equals("!thread")) { if (replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet").equals(expression)) { } else { processCommand(command); } }
        else if (method.equals("!trace")) { if (trace.containsKey(expression)) { } else { processCommand(command); } }
        else if (method.equals("!screen")) { if (desktops.containsKey(expression)) { } else { processCommand(command); } }
        else if (method.equals("!alias")) { if (aliases.containsKey(expression)) { } else { processCommand(command); } }
        else if (method.equals("!key")) { if (attributes.containsKey(expression)) { } else { processCommand(command); } }

    }

    // API 006 - (Process)
    // |
    // Memory
    public class HTopViewer implements CommandListener { private Form screen = new Form(form.getTitle()); private Command BACK = new Command("Back", Command.BACK, 1), REFRESH = new Command("Refresh", Command.SCREEN, 2); private StringItem status = new StringItem("", ""); public HTopViewer() { screen.append(status); screen.addCommand(BACK); screen.addCommand(REFRESH); screen.setCommandListener(this); load(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == REFRESH) { Runtime.getRuntime().gc(); load(); } } private void load() { status.setText("Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\nFree Memory: " + runtime.freeMemory() / 1024 + " KB\nTotal Memory: " + runtime.totalMemory() / 1024 + " KB"); } }
    // |
    // Process
    private void kill(String pid) { if (pid == null || pid.length() == 0) { return; } Enumeration keys = trace.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); if (pid.equals(trace.get(key))) { trace.remove(key); echoCommand("Process with PID " + pid + " terminated"); if ("sh".equals(key)) { processCommand("exit"); } return; } } echoCommand("PID '" + pid + "' not found"); }
    private void start(String app) { if (app == null || app.length() == 0) { return; } trace.put(app, String.valueOf(1000 + random.nextInt(9000))); }
    private void stop(String app) { if (app == null || app.length() == 0) { return; } trace.remove(app); if (app.equals("sh")) { processCommand("exit"); } } 

    // API 008 - (Logic I/O) Text
    // |
    // Text related commands
    private void echoCommand(String message) { echoCommand(message, stdout); attributes.put("OUTPUT", message); }
    private void echoCommand(String message, StringItem console) { console.setText(console.getText().equals("") ? message.trim() : console.getText() + "\n" + message.trim()); }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }

    // API 011 - (Network)
    // |
    // Servers
    public class Bind implements Runnable { private String port, prefix; public Bind(String args) { if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522"); new Bind("31522"); return; } port = getCommand(args); prefix = getArgument(args); new Thread(this, "Bind").start(); } public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); start("bind"); while (trace.containsKey("bind")) { SocketConnection clientSocket = null; InputStream is = null; OutputStream os = null; try { clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); is = clientSocket.openInputStream(); os = clientSocket.openOutputStream(); while (trace.containsKey("bind")) { byte[] buffer = new byte[4096]; int bytesRead = is.read(buffer); if (bytesRead == -1) break; String command = new String(buffer, 0, bytesRead).trim(); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(command)); command = (prefix == null || prefix.length() == 0 || prefix.equals("null")) ? command : prefix + " " + command; String beforeCommand = stdout != null ? stdout.getText() : ""; processCommand(command); String afterCommand = stdout != null ? stdout.getText() : ""; String output = afterCommand.length() >= beforeCommand.length() ? afterCommand.substring(beforeCommand.length()).trim() + "\n" : "\n"; os.write(output.getBytes()); os.flush(); } } catch (IOException e) { echoCommand("[-] " + e.getMessage()); } finally { echoCommand("[-] " + clientSocket.getAddress() + " disconnected"); } } echoCommand("[-] Server stopped"); MIDletLogs("add info Server was stopped"); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'"); } try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) { } } }
    public class Server implements Runnable { private String port, response; public Server(String args) { if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522"); new Server("31522"); return; } port = getCommand(args); response = getArgument(args); new Thread(this, "Server").start(); } public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); start("server"); while (trace.containsKey("server")) { SocketConnection clientSocket = null; InputStream is = null; OutputStream os = null; try { clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); is = clientSocket.openInputStream(); os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[4096]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); if (response.startsWith("/")) { os.write(read(response).getBytes()); } else if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(loadRMS(response, 1).getBytes()); } os.flush(); } catch (IOException e) { } finally { try { if (is != null) is.close(); if (os != null) os.close(); if (clientSocket != null) clientSocket.close(); } catch (IOException e) { } } } echoCommand("[-] Server stopped"); MIDletLogs("add info Server was stopped"); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'");  } try { if (serverSocket != null) { serverSocket.close(); } } catch (IOException e) { } } }
    // |
    // HTTP Interfaces
    private void pingCommand(String url) { if (url == null || url.length() == 0) { return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } }
    private void pongCommand(String address) { if (address == null || address.length() == 0) { }  else { long startTime = System.currentTimeMillis(); SocketConnection socket = null; try { socket = (SocketConnection) Connector.open("socket://" + address); long endTime = System.currentTimeMillis(); echoCommand("Pong to " + address + " successful, time=" + (endTime - startTime) + "ms"); } catch (IOException e) { echoCommand("Pong to " + address + " failed: " + e.getMessage()); } } }
    public class InjectorHTTP implements CommandListener { private String url; private TextBox screen = new TextBox("HTTP Header", read("/java/etc/headers"), 31522, TextField.ANY); private Command BACK = new Command("Back", Command.BACK, 1), CLEAR = new Command("Clear", Command.OK, 1), CURL = new Command("Run 'CURL'", Command.OK, 1), WGET = new Command("Run 'WGET'", Command.OK, 1); public InjectorHTTP(String args) { if (args == null || args.length() == 0) { return; } url = args; screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(CURL); screen.addCommand(WGET); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == CLEAR) { screen.setString(""); } else if (c == CURL) { processCommand("xterm"); echoCommand(request(url, parseProperties(env(screen.getString())))); } else if (c == WGET) { processCommand("xterm"); nanoContent = request(url, parseProperties(env(screen.getString()))); } } }
    public class GoBuster implements CommandListener, Runnable { private String url; private String[] wordlist; private List screen; private Command BACK = new Command("Back", Command.BACK, 1), OPEN = new Command("Get Request", Command.OK, 1), SAVE = new Command("Save Result", Command.OK, 1); public GoBuster(String args) { if (args == null || args.length() == 0) { return; } url = args; screen = new List("GoBuster (" + url + ")", List.IMPLICIT); wordlist = split(loadRMS("gobuster", 1), '\n'); if (wordlist == null || wordlist.length == 0) { wordlist = split(read("/java/etc/gobuster"), '\n'); } screen.addCommand(BACK); screen.addCommand(OPEN); screen.addCommand(SAVE); screen.setCommandListener(this); new Thread(this, "GoBuster").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == OPEN) { processCommand("bg execute wget " + url + screen.getString(screen.getSelectedIndex()) + "; nano;"); } else if (c == SAVE && screen.size() != 0) { nanoContent = GoSave(); new NanoEditor(""); } } public void run() { screen.setTicker(new Ticker("Searching...")); for (int i = 0; i < wordlist.length; i++) { if (!wordlist[i].startsWith("#") && !wordlist[i].equals("")) { String fullUrl = url.startsWith("http://") || url.startsWith("https://") ? url + "/" + wordlist[i] : "http://" + url + "/" + wordlist[i]; try { if (GoVerify(fullUrl)) { screen.append("/" + wordlist[i], null); } } catch (IOException e) { } } } screen.setTicker(null); } private boolean GoVerify(String fullUrl) throws IOException { HttpConnection conn = null; InputStream is = null; try { conn = (HttpConnection) Connector.open(fullUrl); conn.setRequestMethod(HttpConnection.GET); int responseCode = conn.getResponseCode(); return (responseCode == HttpConnection.HTTP_OK); } finally { if (is != null) { is.close(); } if (conn != null) { conn.close(); } } } private String GoSave() { StringBuffer sb = new StringBuffer(); for (int i = 0; i < screen.size(); i++) { sb.append(screen.getString(i)); if (i < screen.size() - 1) { sb.append("\n"); } } return replace(sb.toString(), "/", ""); } }
    private String request(String url, Hashtable headers) { if (url == null || url.length() == 0) { return ""; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); if (headers != null) { Enumeration keys = headers.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) headers.get(key); conn.setRequestProperty(key, value); } } InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } is.close(); conn.close(); return new String(baos.toByteArray(), "UTF-8"); } catch (IOException e) { return e.getMessage(); } }
    private String request(String url) { return request(url, null); }
    // |
    // Socket Interfaces
    private void query(String command) { command = env(command.trim()); String mainCommand = getCommand(command); String argument = getArgument(command); if (mainCommand.equals("")) { echoCommand("query: missing [addr]"); return; } try { StreamConnection conn = (StreamConnection) Connector.open(mainCommand); InputStream inputStream = conn.openInputStream(); OutputStream outputStream = conn.openOutputStream(); if (!argument.equals("")) { outputStream.write((argument + "\r\n").getBytes()); outputStream.flush(); } byte[] buffer = new byte[4096]; int length = inputStream.read(buffer); if (length != -1) { String data = new String(buffer, 0, length); if (env("$QUERY").equals("$QUERY") || env("$QUERY").equals("")) { echoCommand(data); MIDletLogs("add warn Query storage setting not found"); } else if (env("$QUERY").toLowerCase().equals("show")) { echoCommand(data); } else if (env("$QUERY").toLowerCase().equals("nano")) { nanoContent = data; echoCommand("query: data retrived"); } else { writeRMS(env("$QUERY"), data); } } inputStream.close(); outputStream.close(); conn.close(); } catch (Exception e) { echoCommand(e.getMessage()); } }
    public class GetAddress { public GetAddress(String args) { if (args == null || args.length() == 0) { processCommand("ifconfig"); } else { echoCommand(performNSLookup(args)); } } private String performNSLookup(String domain) { try { DatagramConnection conn = (DatagramConnection) Connector.open("datagram://1.1.1.1:53"); byte[] query = createDNSQuery(domain); Datagram request = conn.newDatagram(query, query.length); conn.send(request); Datagram response = conn.newDatagram(512); conn.receive(response); conn.close(); return parseDNSResponse(response.getData()); } catch (IOException e) { return e.getMessage(); } } private byte[] createDNSQuery(String domain) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(0x12); out.write(0x34); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); String[] parts = split(domain, '.'); for (int i = 0; i < parts.length; i++) { out.write(parts[i].length()); out.write(parts[i].getBytes()); } out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00); out.write(0x01); return out.toByteArray(); } private String parseDNSResponse(byte[] response) { if ((response[3] & 0x0F) != 0) { return "not found"; } int answerOffset = 12; while (response[answerOffset] != 0) { answerOffset++; } answerOffset += 5; if (response[answerOffset + 2] == 0x00 && response[answerOffset + 3] == 0x01) { StringBuffer ip = new StringBuffer(); for (int i = answerOffset + 12; i < answerOffset + 16; i++) { ip.append(response[i] & 0xFF); if (i < answerOffset + 15) ip.append("."); } return ip.toString(); } else { return "not found"; } } }
    public class PortScanner implements CommandListener, Runnable { private String host; private int start = 1; private List screen; private Command BACK = new Command("Back", Command.BACK, 1), CONNECT = new Command("Connect", Command.OK, 1); public PortScanner(String args) { if (args == null || args.length() == 0) { return; } if (!getArgument(args).equals("")) { try { start = Integer.parseInt(getArgument(args)); } catch (NumberFormatException e) { echoCommand("prscan: " + getArgument(args) + ": invalid start port"); return; } } host = getCommand(args); screen = new List(host + " Ports", List.IMPLICIT); screen.addCommand(BACK); screen.addCommand(CONNECT); screen.setCommandListener(this); new Thread(this, "Port-Scanner").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == CONNECT) { new RemoteConnection(host + ":" + screen.getString(screen.getSelectedIndex())); } } public void run() { screen.setTicker(new Ticker("Scanning...")); for (int port = start; port <= 65535; port++) { screen.setTicker(new Ticker("Scanning Port " + port + "...")); try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port, Connector.READ_WRITE, false); screen.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } screen.setTicker(null); } }
    public class RemoteConnection implements CommandListener, Runnable { private SocketConnection socket; private InputStream inputStream; private OutputStream outputStream; private String host; private Form screen = new Form(form.getTitle()); private TextField inputField = new TextField("Command", "", 256, TextField.ANY); private Command BACK = new Command("Back", Command.SCREEN, 1), EXECUTE = new Command("Send", Command.OK, 1), CLEAR = new Command("Clear", Command.SCREEN, 1), VIEW = new Command("Show info", Command.SCREEN, 1); private StringItem console = new StringItem("", ""); public RemoteConnection(String args) { if (args == null || args.length() == 0) { return; } host = args; inputField.setLabel("Remote (" + split(args, ':')[0] + ")"); screen.append(console); screen.append(inputField); screen.addCommand(EXECUTE); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(VIEW); screen.setCommandListener(this); try { socket = (SocketConnection) Connector.open("socket://" + args); inputStream = socket.openInputStream(); outputStream = socket.openOutputStream(); } catch (IOException e) { echoCommand(e.getMessage()); return; } start("remote"); new Thread(this, "Remote").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == EXECUTE) { String data = inputField.getString().trim(); inputField.setString(""); try { outputStream.write((data + "\n").getBytes()); outputStream.flush(); } catch (IOException e) { processCommand("warn " + e.getMessage()); } } else if (c == BACK) { try { outputStream.write("".getBytes()); outputStream.flush(); inputStream.close(); outputStream.close(); } catch (IOException e) { } writeRMS("remote", console.getText()); stop("remote"); processCommand("xterm"); } else if (c == CLEAR) { console.setText(""); } else if (c == VIEW) { try { warnCommand("Informations", "Host: " + split(host, ':')[0] + "\n" + "Port: " + split(host, ':')[1] + "\n\n" + "Local Address: " + socket.getLocalAddress() + "\n" + "Local Port: " + socket.getLocalPort()); } catch (IOException e) { } } } public void run() { while (trace.containsKey("remote")) { try { byte[] buffer = new byte[4096]; int length = inputStream.read(buffer); if (length != -1) { echoCommand(new String(buffer, 0, length), console); } } catch (IOException e) { processCommand("warn " + e.getMessage()); stop("remote"); } } } }

    // API 012 - (File)
    // |
    // Directories Manager
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.startsWith("#")) { } else if (line.length() != 0) { if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath); start = j; } } String dir = line.substring(start + 1); fullPath += "/" + dir; addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { if (!paths.containsKey(fullPath)) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/')); if (parentPath.length() == 0) { parentPath = "/"; } String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fullPath.substring(fullPath.lastIndexOf('/') + 1)); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } }
    public class Explorer implements CommandListener { private List screen = new List(form.getTitle(), List.IMPLICIT); private Command BACK = new Command("Back", Command.BACK, 1), OPEN = new Command("Open", Command.OK, 1), DELETE = new Command("Delete", Command.OK, 1), RUN = new Command("Run Script", Command.OK, 1), IMPORT = new Command("Import File", Command.OK, 1); public Explorer() { screen.addCommand(BACK); screen.addCommand(OPEN); screen.addCommand(DELETE); screen.addCommand(RUN); screen.addCommand(IMPORT); screen.setCommandListener(this); load(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == OPEN) { new NanoEditor(screen.getString(screen.getSelectedIndex())); } else if (c == DELETE) { deleteFile(screen.getString(screen.getSelectedIndex())); load(); } else if (c == RUN) { processCommand("xterm"); processCommand("run " + screen.getString(screen.getSelectedIndex())); } else if (c == IMPORT) { processCommand("xterm"); importScript(screen.getString(screen.getSelectedIndex())); } } private void load() { screen.deleteAll(); try { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].startsWith(".")) { } else { screen.append((String) recordStores[i], null); } } } } catch (RecordStoreException e) { } } }
    public class FileExplorer implements CommandListener { private String path = "file:///", init; private List screen = new List(form.getTitle(), List.IMPLICIT); private Command BACK = new Command("Back", Command.BACK, 1), OPEN = new Command("Open", Command.OK, 1); public FileExplorer(String args) { if (!args.equals("")) { path = path + args; init = path; } screen.addCommand(BACK); screen.addCommand(OPEN); screen.setCommandListener(this); load(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { if (path.equals("file:///") || init == path) { processCommand("xterm"); } else { int lastSlash = path.lastIndexOf('/', path.length() - 2); if (lastSlash != -1) { path = path.substring(0, lastSlash + 1); load(); } } } else if (c == OPEN) { int selectedIndex = screen.getSelectedIndex(); if (selectedIndex >= 0) { String selected = screen.getString(selectedIndex); if (selected.endsWith("/")) { path = path + selected; load(); } else { writeRMS(selected, read(path + selected)); warnCommand(null, "File '" + selected + "' successfully saved!"); } } } } private void load() { screen.deleteAll(); try { if (path.equals("file:///")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { screen.append((String) roots.nextElement(), null); } } else { FileConnection dir = (FileConnection) Connector.open(path, Connector.READ); Enumeration content = dir.list(); Vector dirs = new Vector(), files = new Vector(); while (content.hasMoreElements()) { String filename = (String) content.nextElement(); if (filename.endsWith("/")) { dirs.addElement(filename); } else { files.addElement(filename); } } while (!dirs.isEmpty()) { screen.append(getFirstString(dirs), null); } while (!files.isEmpty()) { screen.append(getFirstString(files), null); } dir.close(); } } catch (IOException e) { } } private static String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(result) < 0) { result = cur; } } v.removeElement(result); return result; } private String read(String file) { try { FileConnection fileConn = (FileConnection) Connector.open(file, Connector.READ); InputStream is = fileConn.openInputStream(); StringBuffer content = new StringBuffer(); int ch; while ((ch = is.read()) != -1) { content.append((char) ch); } is.close(); fileConn.close(); return content.toString(); } catch (IOException e) { return ""; } } }
    private String readStack() { StringBuffer sb = new StringBuffer(); sb.append(path); for (int i = 0; i < stack.size(); i++) { sb.append(" ").append((String) stack.elementAt(i)); } return sb.toString(); }
    // |
    // RMS Files
    private void deleteFile(String filename) { if (filename == null || filename.length() == 0) { return; } try { RecordStore.deleteRecordStore(filename); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private String loadRMS(String recordStoreName, int recordId) { RecordStore recordStore = null; String result = ""; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); if (recordStore.getNumRecords() >= recordId) { byte[] data = recordStore.getRecord(recordId); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { result = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return result; }
    // |
    // Text Manager
    private void StringEditor(String command) { command = env(command.trim()); String mainCommand = getCommand(command); String argument = getArgument(command); if (mainCommand.equals("-2u")) { nanoContent = nanoContent.toUpperCase(); } else if (mainCommand.equals("-2l")) { nanoContent = nanoContent.toLowerCase(); } else if (mainCommand.equals("-d")) { nanoContent = replace(nanoContent, split(argument, ' ')[0], ""); } else if (mainCommand.equals("-a")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } else if (mainCommand.equals("-r")) { nanoContent = replace(nanoContent, split(argument, ' ')[0], split(argument, ' ')[1]); } else if (mainCommand.equals("-l")) { int i = 0; try { i = Integer.parseInt(argument); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return; } echoCommand(split(nanoContent, '\n')[i]); } else if (mainCommand.equals("-s")) { int i = 0; try { i = Integer.parseInt(getCommand(argument)); } catch (NumberFormatException e) { echoCommand(e.getMessage()); return; } Vector lines = new Vector(); String div = getArgument(argument); int start = 0, index; while ((index = nanoContent.indexOf(div, start)) != -1) { lines.addElement(nanoContent.substring(start, index)); start = index + div.length(); } if (start < nanoContent.length()) { lines.addElement(nanoContent.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); if (i >= 0 && i < result.length) { echoCommand(result[i]); } else { echoCommand("null"); } } else if (mainCommand.equals("-p")) { String[] contentLines = split(nanoContent, '\n'); StringBuffer updatedContent = new StringBuffer(); for (int i = 0; i < contentLines.length; i++) { updatedContent.append(argument).append(contentLines[i]).append("\n"); } nanoContent = updatedContent.toString().trim(); } else if (mainCommand.equals("-v")) { String[] lines = split(nanoContent, '\n'); StringBuffer reversed = new StringBuffer(); for (int i = lines.length - 1; i >= 0; i--) { reversed.append(lines[i]).append("\n"); } nanoContent = reversed.toString().trim(); } }
    // |
    // Text Parsers
    private String parseJson(String text) { Hashtable properties = parseProperties(text); if (properties.isEmpty()) { return "{}"; } Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\n  \"").append(key).append("\": "); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("\n}"); return jsonBuffer.toString(); }
    private String parseConf(String text) { StringBuffer iniBuffer = new StringBuffer(); text = text.trim(); if (text.startsWith("{") && text.endsWith("}")) { text = text.substring(1, text.length() - 1); } String[] pairs = split(text, ','); for (int i = 0; i < pairs.length; i++) { String pair = pairs[i].trim(); String[] keyValue = split(pair, ':'); if (keyValue.length == 2) { String key = getpattern(keyValue[0].trim()); String value = getpattern(keyValue[1].trim()); iniBuffer.append(key).append("=").append(value).append("\n"); } } return iniBuffer.toString(); }
    private String text2note(String content) { if (content == null || content.length() == 0) { return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:\nEND:VNOTE"; } content = replace(content, "=", "=3D"); content = replace(content, "\n", "=0A"); StringBuffer vnote = new StringBuffer(); vnote.append("BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" + content + "\nEND:VNOTE"); return vnote.toString(); }
    // |
    // Interfaces
    public class NanoEditor implements CommandListener { private TextBox screen = new TextBox("Nano", "", 31522, TextField.ANY); private Command BACK = new Command("Back", Command.BACK, 1), CLEAR = new Command("Clear", Command.OK, 1), RUN = new Command("Run Script", Command.OK, 1), IMPORT = new Command("Import File", Command.OK, 1), VIEW = new Command("View as HTML", Command.OK, 1); public NanoEditor(String args) { screen.setString((args == null || args.length() == 0) ? nanoContent : loadRMS(args, 1)); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(RUN); screen.addCommand(IMPORT); screen.addCommand(VIEW); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { nanoContent = screen.getString(); processCommand("xterm"); } else if (c == CLEAR) { screen.setString(""); } else if (c == RUN) { nanoContent = screen.getString(); processCommand("xterm"); runScript(nanoContent); } else if (c == IMPORT) { nanoContent = screen.getString(); processCommand("xterm"); importScript("nano"); } else if (c == VIEW) { nanoContent = screen.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }
    private String extractTitle(String htmlContent) { int titleStart = htmlContent.indexOf("<title>"); int titleEnd = htmlContent.indexOf("</title>"); if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) { return htmlContent.substring(titleStart + 7, titleEnd).trim(); } return "HTML Viewer"; }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }

    // API 013 - (MIDlet)
    // |
    // General Utilities
    private void java(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command);
        String argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("Java ME", env("Java 1.2 (OpenTTY Edition)\n\nMicroEdition-Config: $CONFIG\nMicroEdition-Profile: $PROFILE")); }
        else if (mainCommand.equals("-class")) { if (argument.equals("")) { } else { try { Class.forName(argument); echoCommand("true"); } catch (ClassNotFoundException e) { echoCommand("false"); } } } 
        else if (mainCommand.equals("--version")) { echoCommand("Java 1.2 (OpenTTY Edition)"); } 

        else { String code; Hashtable objects = new Hashtable(); if (mainCommand.startsWith("/")) { code = read(mainCommand); } else if (mainCommand.equals("nano")) { code = nanoContent; } else { code = loadRMS(mainCommand, 1); } if (code == null || code.length() == 0) { echoCommand("java: " + mainCommand + ": blank class"); return; } String[] lines = split(code, ';'); for (int i = 0; i < lines.length; i++) { String line = lines[i].trim(); if (line.length() == 0) { continue; } try { if (line.indexOf('=') != -1) { String[] parts = split(line, '='); String objectName = parts[0].trim(); String className = parts[1].trim(); Class clazz = Class.forName(className); Object instance = clazz.newInstance(); objects.put(objectName, instance); } else if (line.indexOf('.') != -1) { String[] parts = split(line, '.'); String objectName = parts[0].trim(); if (!objects.containsKey(objectName)) { throw new IOException("Object not found"); } for (int j = 1; j < parts.length; j++) { Object object = (Object) objects.get(objectName); Class clazz = object.getClass(); echoCommand("Invoke method '" + parts[j] + "' on object '" + objectName + "' of class '" + clazz.getName() + "'."); } } else if (line.startsWith("//")) { } else { throw new IOException("Invalid syntax"); } } catch (Exception e) { echoCommand(e.getClass().getName() + ": '" + line + "' (" + e.getMessage() + ")"); return; } } }                
    }
    private void chmod(String node) { if (node == null || node.length() == 0) { return; } int status = 0; try { if (node.equals("http")) { node = "javax.microedition.io.Connector.http"; ((HttpConnection) Connector.open("http://ipinfo.io")).close(); status = 1; } else if (node.equals("socket")) { node = "javax.microedition.io.Connector.socket"; ((SocketConnection) Connector.open("socket://1.1.1.1:53")).close(); status = 1; } else if (node.equals("file")) {  node = "javax.microedition.io.Connector.file"; FileSystemRegistry.listRoots(); status = 1; } else if (node.equals("prg")) { node = "javax.microedition.io.PushRegistry"; PushRegistry.registerAlarm(getClass().getName(), System.currentTimeMillis() + 1000); status = 1; } else { echoCommand("chmod: " + node + ": not found"); } } catch (SecurityException e) { status = 2; } catch (Exception e) { echoCommand(e.getMessage());  } if (status == 1) { MIDletLogs("add info Permission '" + node + "' granted"); } else if (status == 2) { MIDletLogs("add error Permission '" + node + "' denied"); } }
    public class History implements CommandListener { private List screen = new List(form.getTitle(), List.IMPLICIT); private Command BACK = new Command("Back", Command.BACK, 1), RUN = new Command("Run", Command.OK, 1), EDIT = new Command("Edit", Command.OK, 1); public History() { screen.addCommand(BACK); screen.addCommand(RUN); screen.addCommand(EDIT); screen.setCommandListener(this); load(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == RUN) { int index = screen.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); processCommand(screen.getString(index)); } } else if (c == EDIT) { int index = screen.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); stdin.setString(screen.getString(index)); } } } private void load() { screen.deleteAll(); for (int i = 0; i < history.size(); i++) { screen.append((String) history.elementAt(i), null); } } }

    // API 015 - (Scripts)
    // |
    // OpenTTY Packages
    private void about(String script) { if (script == null || script.length() == 0) { warnCommand("About", env("OpenTTY $VERSION\n(C) 2025 - Mr. Lima")); return; } Hashtable lib = parseProperties(getcontent(script)); if (lib.containsKey("name")) { echoCommand((String) lib.get("name") + " " + (String) lib.get("version")); } if (lib.containsKey("description")) { echoCommand((String) lib.get("description")); } }
    private void importScript(String script) {
        if (script == null || script.length() == 0) { return; }

        Hashtable lib = parseProperties(getcontent(script));
        // |
        // Verify current API version
        if (lib.containsKey("api.version")) { if (!env("$VERSION").startsWith((String) lib.get("api.version"))) { processCommand(lib.containsKey("api.error") ? (String) lib.get("api.error") : "true"); return; } }
        // |
        // Start and handle APP process
        if (lib.containsKey("process.name")) { start((String) lib.get("process.name")); }
        if (lib.containsKey("process.type")) { String type = (String) lib.get("process.type"); if (type.equals("server")) { } else if (type.equals("bind")) { new Bind(env((String) lib.get("process.port") + " " + (String) lib.get("process.db"))); } else { MIDletLogs("add warn '" + type.toUpperCase() + "' is a invalid value for 'process.type'"); } }
        if (lib.containsKey("process.host") && lib.containsKey("process.port")) { new Server(env((String) lib.get("process.port") + " " + (String) lib.get("process.host"))); }
        // |
        // Build dependencies
        if (lib.containsKey("include")) { String[] include = split((String) lib.get("include"), ','); for (int i = 0; i < include.length; i++) { importScript(include[i]); } }
        // |
        // Start Application
        if (lib.containsKey("config")) { processCommand((String) lib.get("config")); }
        if (lib.containsKey("mod") && lib.containsKey("process.name")) { final String name = (String) lib.get("process.name"); final String mod = (String) lib.get("mod"); new Thread(new Runnable() { public void run() { while (trace.containsKey(name)) { processCommand(mod); } } }, "MIDlet-Mod").start(); }
        // |
        // Generate itens - Command & Files
        if (lib.containsKey("command")) { String[] command = split((String) lib.get("command"), ','); for (int i = 0; i < command.length; i++) { if (lib.containsKey(command[i])) { aliases.put(command[i], env((String) lib.get(command[i]))); } else { MIDletLogs("add error Failed to create command '" + command[i] + "' content not found"); } } }
        if (lib.containsKey("file")) { String[] file = split((String) lib.get("file"), ','); for (int i = 0; i < file.length; i++) { if (lib.containsKey(file[i])) { writeRMS(file[i], env((String) lib.get(file[i]))); } else { MIDletLogs("add error Failed to create file '" + file[i] + "' content not found"); } } }
        // |
        // Build APP Shell
        if (lib.containsKey("shell.name") && lib.containsKey("shell.args")) { build(lib); }
    }
    private void build(Hashtable lib) { String name = (String) lib.get("shell.name"); String[] args = split((String) lib.get("shell.args"), ','); Hashtable shellTable = new Hashtable(); for (int i = 0; i < args.length; i++) { String argName = args[i].trim(); String argValue = (String) lib.get(argName); shellTable.put(argName, (argValue != null) ? argValue : ""); } if (lib.containsKey("shell.unknown")) { shellTable.put("shell.unknown", (String) lib.get("shell.unknown")); } shell.put(name, shellTable); }
    private void runScript(String script) { String[] commands = split(script, '\n'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }

}
