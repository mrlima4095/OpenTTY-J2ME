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
                      paths = new Hashtable(), desktops = new Hashtable(), trace = new Hashtable();
    private String logs = "", path = "/home/", build = "2025-1.14.3-01x93";
    private String username = loadRMS("OpenRMS");
    private String nanoContent = loadRMS("nano");
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
            attributes.put("PATCH", "Renders Update"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "beta"); attributes.put("XVERSION", "0.6.2");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));

            runScript(read("/java/etc/initd.sh")); stdin.setLabel(username + " " + path + " $");

            if (username.equals("")) { new Login(); }
            else { runScript(read("/home/initd")); }
        } 
    }

    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { writeRMS("/home/nano", nanoContent); }

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
        // Environment Keys
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
        else if (mainCommand.equals("logout")) { writeRMS("/home/OpenRMS", ""); processCommand("exit"); }
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
        else if (mainCommand.equals("bruteforce")) { start("bruteforce"); while (trace.containsKey("bruteforce")) { processCommand(argument, ignore); } }
        else if (mainCommand.equals("cron")) { if (argument.equals("")) { } else { processCommand("execute sleep " + getCommand(argument) + "; " + getArgument(argument)); } }
        else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (InterruptedException e) { } catch (NumberFormatException e) { echoCommand(e.getMessage()); } } }
        else if (mainCommand.equals("time")) { if (argument.equals("")) { } else { long startTime = System.currentTimeMillis(); processCommand(argument, ignore); long endTime = System.currentTimeMillis(); echoCommand("at " + (endTime - startTime) + "ms"); } }
        // |
        // Chain executors
        else if (mainCommand.equals("exec")) { String[] commands = split(argument, '&'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim(), ignore); } }
        else if (mainCommand.equals("execute")) { String[] commands = split(argument, ';'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim(), ignore); } }

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
        else if (mainCommand.equals("uuid")) { echoCommand(generateUUID()); }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("expr")) { echoCommand(exprCommand(argument)); }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
        else if (mainCommand.equals("getopt")) { echoCommand(getArgument(argument)); }
        else if (mainCommand.equals("trim")) { stdout.setText(stdout.getText().trim()); }
        else if (mainCommand.equals("date")) { echoCommand(new java.util.Date().toString()); }
        else if (mainCommand.equals("clear")) { if (argument.equals("") || argument.equals("stdout")) { stdout.setText(""); } else if (argument.equals("stdin")) { stdin.setString(""); } else if (argument.equals("history")) { history = new Vector(); } else if (argument.equals("logs")) { logs = ""; } else { echoCommand("clear: " + argument + ": not found"); } }
        else if (mainCommand.equals("seed")) { try { echoCommand("" +  random.nextInt(Integer.parseInt(argument)) + ""); } catch (NumberFormatException e) { echoCommand(e.getMessage()); } }

        // API 009 - (Threads)
        // |
        // MIDlet Tracker
        else if (mainCommand.equals("throw")) { if (argument.equals("")) { } else { MIDletLogs("add warn " + argument); } Thread.currentThread().interrupt(); }
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
        else if (mainCommand.equals("bind")) { new Bind(argument.equals("") ? env("$PORT") : argument); }
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
        // |
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
        else if (mainCommand.equals("ls")) { new Explorer(); }
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("umount")) { paths = new Hashtable(); }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { mount(getcontent(argument)); } }
        else if (mainCommand.equals("cd")) { 
            if (argument.equals("")) { path = "/home/"; } 
            else if (argument.equals("..")) { if (path.equals("/")) { return; } int lastSlashIndex = path.lastIndexOf('/', path.endsWith("/") ? path.length() - 2 : path.length() - 1); path = (lastSlashIndex <= 0) ? "/" : path.substring(0, lastSlashIndex + 1); } 
            else { 
                String target = argument.startsWith("/") ? argument : (path.endsWith("/") ? path + argument : path + "/" + argument); 
                if (!target.endsWith("/")) { target += "/"; } 

                if (paths.containsKey(target)) { path = target; } 
                else if (target.startsWith("/mnt/")) { try { String realPath = "file:///" + target.substring(5); if (!realPath.endsWith("/")) realPath += "/"; FileConnection fc = (FileConnection) Connector.open(realPath, Connector.READ); if (fc.exists() && fc.isDirectory()) { path = target; } else { echoCommand("cd: " + basename(target) + ": not " + (!fc.exists() ? "found" : "a directory")); } fc.close(); } catch (IOException e) { echoCommand("cd: " + basename(target) + ": " + e.getMessage()); } } 
                else { 
                    echoCommand("cd: " + basename(target) + ": not " + (!paths.containsKey(target.substring(0, target.endsWith("/") ? target.length() - 2 : target.length() - 1)) ? "found" : "a directory"));
                }
            } 
        }
        else if (mainCommand.equals("pushd")) { if (argument.equals("")) { echoCommand(readStack() == null || readStack().length() == 0 ? "pushd: missing directory": readStack()); } else { if (!argument.endsWith("/")) { argument += "/"; }if (!paths.containsKey(argument)) { echoCommand("pushd: " + argument + ": not found"); } else { stack.addElement(path); path = argument; echoCommand(readStack()); } } }
        else if (mainCommand.equals("popd")) { if (stack.isEmpty()) { echoCommand("popd: stack empty"); } else { path = (String) stack.lastElement(); stack.removeElementAt(stack.size() - 1); echoCommand(readStack()); } }
        else if (mainCommand.equals("dir")) { Vector results = new Vector(); if (path.equals("/mnt/")) { try { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { String root = (String) roots.nextElement(); if (!results.contains(root)) { results.addElement(root); } } } catch (Exception e) { echoCommand(e.getMessage()); return; } } else if (path.startsWith("/mnt/")) { try { String realPath = "file:///" + path.substring(5); if (!realPath.endsWith("/")) realPath += "/"; FileConnection fc = (FileConnection) Connector.open(realPath, Connector.READ); Enumeration content = fc.list(); while (content.hasMoreElements()) { String item = (String) content.nextElement(); results.addElement(item); } fc.close(); } catch (Exception e) { echoCommand(e.getMessage()); return; } } else if (path.equals("/home/") && argument.indexOf("-v") != -1) { try { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { String name = recordStores[i]; if ((argument.indexOf("-a") != -1 || !name.startsWith(".")) && !results.contains(name)) { results.addElement(name); } } } } catch (RecordStoreException e) { echoCommand("dir: " + e.getMessage()); return; } } else if (path.equals("/home/")) { new Explorer(); return; } String[] files = (String[]) paths.get(path); if (files != null) { for (int i = 0; i < files.length; i++) { String f = files[i].trim(); if (f == null || f.equals("..") || f.equals("/")) { continue; } if (!results.contains(f) && !results.contains(f + "/")) { results.addElement(f); } } } if (!results.isEmpty()) { StringBuffer sb = new StringBuffer(); String newline = path.equals("/home/") ? "\n" : "\t"; for (int i = 0; i < results.size(); i++) { String item = (String) results.elementAt(i); if (!item.equals("/")) { sb.append(item).append(newline); } } echoCommand(sb.toString().trim()); } } 
        // |
        // Device Files
        else if (mainCommand.equals("fdisk")) { processCommand("lsblk -p"); }
        else if (mainCommand.equals("lsblk")) { 
            if (argument.equals("") || argument.equals("-x")) {  echoCommand(replace("MIDlet.RMS.Storage", ".", argument.equals("-x") ? ";" : "\t")); } 
            else if (argument.equals("-p")) { 
                
            } 
            else { echoCommand("lsblk: " + argument + ": not found"); } 
        }
        // |
        // RMS Files
        else if (mainCommand.equals("rm")) { if (argument.equals("")) { } else { deleteFile(argument); } }
        else if (mainCommand.equals("mkdir")) { if (argument.equals("")) { } else { argument = argument.endsWith("/") ? argument : argument + "/"; argument = argument.startsWith("/") ? argument : path + argument; if (argument.startsWith("/mnt/")) { try { FileConnection fc = (FileConnection) Connector.open("file:///" + argument.substring(5), Connector.READ_WRITE); if (!fc.exists()) { fc.mkdir(); fc.close(); } else { echoCommand("mkdir: " + basename(argument) + ": found"); } } catch (IOException e) { echoCommand(e.getMessage()); } } else if (argument.startsWith("/home/")) { echoCommand(""); } else if (argument.startsWith("/")) { echoCommand("read-only storage"); } } }
        else if (mainCommand.equals("install")) { if (argument.equals("")) { } else { writeRMS(argument, nanoContent); } }
        else if (mainCommand.equals("touch")) { if (argument.equals("")) { nanoContent = ""; } else { writeRMS(argument, ""); } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { String origin = getCommand(argument), target = getArgument(argument); writeRMS(target.equals("") ? origin + "-copy" : target, getcontent(origin)); } }
        // |
        // Text Manager
        else if (mainCommand.equals("sed")) { StringEditor(argument); }
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("rraw")) { stdout.setText(nanoContent); }
        else if (mainCommand.equals("getty")) { nanoContent = stdout.getText(); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent.equals("") ? argument : nanoContent + "\n" + argument; } 
        else if (mainCommand.equals("hash")) { if (argument.equals("")) { } else { echoCommand("" + getcontent(argument).hashCode()); } }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { } else { echoCommand(getcontent(argument)); } }
        else if (mainCommand.equals("get")) { if (argument.equals("") || argument.equals("nano")) { nanoContent = loadRMS("nano"); } else { nanoContent = getcontent(argument); } }
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
        else if (mainCommand.equals("view")) { if (argument.equals("")) { } else { viewer(extractTitle(env(argument)), html2text(env(argument))); } }
        // |
        else if (mainCommand.equals("du")) { if (argument.equals("")) { } else { echoCommand("" + getcontent(argument).length() + "\t" + basename(argument)); } }

        // API 013 - (MIDlet)
        // |
        // Java Runtime
        else if (mainCommand.equals("java")) { java(argument); }
        // |
        // Permissions
        else if (mainCommand.equals("chmod")) { chmod(argument); }
        // |
        // General Utilities
        else if (mainCommand.equals("history")) { new History(); }
        else if (mainCommand.equals("debug")) { runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("help")) { viewer(form.getTitle(), read("/java/help.txt")); }
        else if (mainCommand.equals("man")) { boolean verbose = argument.indexOf("-v") != -1; if (verbose) { argument = replace(argument, "-v", "").trim(); } if (argument.equals("")) { processCommand("man sh" + (verbose ? " -v" : ""), false); return; } String content = read("/home/man.html"); if (content.equals("") || argument.equals("--update")) { processCommand("execute install /home/nano; netstat; if ($OUTPUT == true) exec tick Downloading... & proxy github.com/mrlima4095/OpenTTY-J2ME/raw/refs/heads/main/assets/root/man.html & install /home/man.html & get /home/nano & echo [Manual] Resources downloaded! & tick; if ($OUTPUT == false) exec echo [Manual] MIDlet cannot access Internet! & echo [Manual] Verify your connection and try again.", false); return; } String tag = argument.toLowerCase(), startTag = "<" + tag + ">", endTag = "</" + tag + ">"; int start = content.indexOf(startTag), end = content.indexOf(endTag); if (start != -1 && end != -1 && end > start) { String section = content.substring(start + startTag.length(), end).trim(); if (verbose) { echoCommand(section); } else { viewer(form.getTitle(), section); } } else { echoCommand("man: " + argument + ": not found"); } }
        else if (mainCommand.equals("true") || mainCommand.equals("false") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("exit") || mainCommand.equals("quit")) { writeRMS("/home/nano", nanoContent); notifyDestroyed(); }

        //else if (mainCommand.equals("")) {  }
        //else if (mainCommand.equals("")) {  }
        //else if (mainCommand.equals("")) {  }
        //else if (mainCommand.equals("")) {  }

        // API 014 - (OpenTTY)
        // |
        // Low-level commands
        else if (mainCommand.equals("@exec")) { commandAction(EXECUTE, display.getCurrent()); }
        else if (mainCommand.equals("@login")) { if (argument.equals("")) { username = loadRMS("OpenRMS"); } else { username = argument; } }
        else if (mainCommand.equals("@alert")) { try { display.vibrate(argument.equals("") ? 500 : Integer.parseInt(argument) * 100); } catch (NumberFormatException e) { echoCommand(e.getMessage()); } }
        else if (mainCommand.equals("@reload")) { shell = new Hashtable(); aliases = new Hashtable(); username = loadRMS("OpenRMS"); MIDletLogs("add debug API reloaded"); processCommand("execute x11 stop; x11 init; x11 term; run initd; sh;"); }
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

    private String read(String filename) { try { if (filename.startsWith("/mnt/")) { FileConnection fileConn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ); InputStream is = fileConn.openInputStream(); StringBuffer content = new StringBuffer(); int ch; while ((ch = is.read()) != -1) { content.append((char) ch); } is.close(); fileConn.close(); return env(content.toString()); } else if (filename.startsWith("/home/")) { RecordStore recordStore = null; String content = ""; try { recordStore = RecordStore.openRecordStore(filename.substring(6), true); if (recordStore.getNumRecords() >= 1) { byte[] data = recordStore.getRecord(1); if (data != null) { content = new String(data); } } } catch (RecordStoreException e) { content = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return content; } else { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); if (is == null) { return ""; } InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } } catch (IOException e) { return ""; } }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0; int end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); Enumeration e = attributes.keys(); while (e.hasMoreElements()) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } text = replace(text, "$.", "$"); text = replace(text, "\\.", "\\"); return text; }
    
    private String getcontent(String file) { return file.startsWith("/") ? read(file) : file.equals("nano") ? nanoContent : read(path + file); }
    private String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }

    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }
    private Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    
    public class Login implements CommandListener { private Form screen = new Form("Login"); private TextField userField = new TextField("Username", "", 256, TextField.ANY); private Command loginCommand = new Command("Login", Command.OK, 1), exitCommand = new Command("Exit", Command.SCREEN, 2); public Login() { screen.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\nCreate an user to access OpenTTY!")); screen.append(userField); screen.addCommand(loginCommand); screen.addCommand(exitCommand); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == loginCommand) { username = userField.getString(); if (username.equals("")) { } else { writeRMS("/home/OpenRMS", username); display.setCurrent(form); runScript(loadRMS("initd")); } } else if (c == exitCommand) { processCommand("exit"); } } }

    // API 001 - (Registry)
    // |
    // Aliases
    private void aliasCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (argument.equals("")) { Enumeration keys = aliases.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) aliases.get(key); if (!key.equals("xterm") && !value.equals("")) { echoCommand("alias " + key + "='" + value.trim() + "'"); } } } else if (aliases.containsKey(argument)) { echoCommand("alias " + argument + "='" + (String) aliases.get(argument) + "'"); } else { echoCommand("alias: " + argument + ": not found"); } return; } aliases.put(argument.substring(0, equalsIndex).trim(), argument.substring(equalsIndex + 1).trim()); }
    private void unaliasCommand(String key) { if (key == null || key.length() == 0) { return; } if (aliases.containsKey(key)) { aliases.remove(key); } else { echoCommand("unalias: " + key + ": not found"); } }
    // |
    // Environment Keys
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
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("buffer")) { echoCommand("" + display.getCurrent().getWidth() + "x" + display.getCurrent().getHeight() + ""); }
        // |
        // X11 Loader
        else if (mainCommand.equals("term")) { display.setCurrent(form); }
        else if (mainCommand.equals("stop")) { form.setTitle(""); form.setTicker(null); form.deleteAll(); xserver("cmd hide"); xserver("font"); form.removeCommand(EXECUTE); }
        else if (mainCommand.equals("init")) { form.setTitle(env("OpenTTY $VERSION")); form.append(stdout); form.append(stdin); form.addCommand(EXECUTE); xserver("cmd"); form.setCommandListener(this); }
        else if (mainCommand.equals("xfinit")) { if (argument.equals("")) { xserver("init"); } if (argument.equals("stdin")) { form.append(stdin); } else if (argument.equals("stdout")) { form.append(stdout); } }
        else if (mainCommand.equals("cmd")) { Command[] cmds = { HELP, NANO, CLEAR, HISTORY }; for (int i = 0; i < cmds.length; i++) { if (argument.equals("hide")) { form.removeCommand(cmds[i]); } else { form.addCommand(cmds[i]); } } }
        // | 
        // Screen MODs
        else if (mainCommand.equals("title")) { display.getCurrent().setTitle(argument); }
        else if (mainCommand.equals("font")) { if (argument.equals("")) { xserver("font default"); } else { stdout.setFont(newFont(argument)); } }
        else if (mainCommand.equals("tick")) { Displayable current = display.getCurrent(); current.setTicker(argument.equals("") ? null : new Ticker(argument)); }
        else if (mainCommand.equals("gauge")) { Alert alert = new Alert(form.getTitle(), argument, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING)); display.setCurrent(alert); }
        // |
        // Screen Manager
        else if (mainCommand.equals("set")) { if (argument.equals("")) { } else { desktops.put(argument, display.getCurrent()); } }
        else if (mainCommand.equals("load")) { if (argument.equals("")) { } else { if (desktops.containsKey(argument)) { display.setCurrent((Displayable) desktops.get(argument)); } else { echoCommand("x11: load: " + argument + ": not found"); } } }
        // |
        // Interfaces
        else if (mainCommand.equals("canvas")) { display.setCurrent(new MyCanvas(argument.equals("") ? "OpenRMS" : argument)); }
        else if (mainCommand.equals("make") || mainCommand.equals("list") || mainCommand.equals("quest")) { new Screen(mainCommand, argument); }
        else if (mainCommand.equals("item")) { new ItemLoader(form, "item", argument); }

        else { echoCommand("x11: " + mainCommand + ": not found"); }
    }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); }
    private void viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(new StringItem(null, env(text))); viewer.addCommand(new Command("Back", Command.BACK, 1)); viewer.setCommandListener(this); display.setCurrent(viewer); }
    // |
    // Interfaces
    public class Screen implements CommandListener {
        private Hashtable lib;
        private int TYPE = 0, SCREEN = 1, LIST = 2, QUEST = 3;
        private Form screen = new Form(form.getTitle());
        private List list = new List(form.getTitle(), List.IMPLICIT);
        private Command BACK, USER;
        private TextField input;

        public Screen(String type, String args) {
            if (type == null || type.length() == 0 || args == null || args.length() == 0) { return; }

            lib = parseProperties(getcontent(args));

            if (type.equals("make")) {
                TYPE = SCREEN;

                if (lib.containsKey("screen.title")) { screen.setTitle(getenv("screen.title")); }

                BACK = new Command(getenv("screen.back.label", "Back"), Command.OK, 1); USER = new Command(getenv("screen.button", "Menu"), Command.SCREEN, 2); 
                screen.addCommand(BACK); if (lib.containsKey("screen.button")) { screen.addCommand(USER); } 

                if (lib.containsKey("screen.fields")) {
                    String[] fields = split(getenv("screen.fields"), ',');
                    for (int i = 0; i < fields.length; i++) {
                        String field = fields[i].trim();
                        String type = getenv("screen." + field + ".type");

                        if (type.equals("image") && !getenv("screen." + field + ".img").equals("")) { try { screen.append(new ImageItem(null, Image.createImage(getenv("screen." + field + ".img")), ImageItem.LAYOUT_CENTER, null)); } catch (IOException e) { MIDletLogs("add warn Image '" + getenv("screen." + field + ".img") + "' could not be loaded"); } }
                        else if (type.equals("text") && !getenv("screen." + field + ".content").equals("")) { StringItem content = new StringItem(getenv("screen." + field + ".label"), getenv("screen." + field + ".content")); content.setFont(newFont(getenv("screen." + field + ".style", "default"))); screen.append(content); }
                        else if (type.equals("item")) { new ItemLoader(screen, "screen." + field, args); }

                    }
                }
                
                screen.setCommandListener(this);
                display.setCurrent(screen);
            }
            else if (type.equals("list")) {
                TYPE = LIST;
                Image IMG = null;

                if (!lib.containsKey("list.content")) { MIDletLogs("add error List crashed while init, malformed settings"); return; }
                
                if (lib.containsKey("list.title")) { list.setTitle(getenv("list.title")); }
                if (lib.containsKey("list.icon")) { try { IMG = Image.createImage(getenv("list.icon")); } catch (IOException e) { MIDletLogs("add warn Resource '" + getenv("list.icon") + "' cannot be loaded"); } }
                
                BACK = new Command(getenv("list.back.label", "Back"), Command.OK, 1); USER = new Command(getenv("list.button", "Select"), Command.SCREEN, 2);
                list.addCommand(BACK); list.addCommand(USER);

                String[] content = split(getenv("list.content"), ',');

                for (int i = 0; i < content.length; i++) { list.append(content[i], IMG); }

                list.setCommandListener(this);
                display.setCurrent(list);
            }
            else if (type.equals("quest")) {
                TYPE = QUEST;

                if (!lib.containsKey("quest.label") || !lib.containsKey("quest.cmd") || !lib.containsKey("quest.key")) { MIDletLogs("add error Quest crashed while init, malformed settings"); return; }

                if (lib.containsKey("quest.title")) { screen.setTitle(getenv("quest.title")); }
                input = new TextField(getenv("quest.label"), getenv("quest.content"), 256, getQuest(getenv("quest.type")));

                BACK = new Command(getvalue("quest.back.label", "Cancel"), Command.SCREEN, 2); USER = new Command(getvalue("quest.cmd.label", "Send"), Command.OK, 1);

                screen.append(input);
                screen.addCommand(BACK); screen.addCommand(USER);
                screen.setCommandListener(this);
                display.setCurrent(screen);
            } else { return; }

        }

        public void commandAction(Command c, Displayable d) {
            if (c == BACK) {
                processCommand("xterm");
                if (TYPE == SCREEN) { processCommand(getvalue("screen.back", "true")); } 
                else if (TYPE == LIST) { processCommand(getvalue("list.back", "true")); }
                else if (TYPE == QUEST) { processCommand(getvalue("quest.back", "true")); }
            } else if (c == USER) {
                if (TYPE == QUEST) { String value = input.getString().trim(); if (!value.equals("")) { processCommand("set " + getenv("quest.key") + "=" + env(value)); processCommand("xterm"); processCommand(getvalue("quest.cmd", "true")); } } 
                else if (TYPE == LIST) { int index = list.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); String key = env(list.getString(index)); processCommand(getvalue(key, "log add warn An error occurred, '" + key + "' not found")); } } 
                else if (TYPE == SCREEN) { processCommand("xterm"); processCommand(getvalue("screen.button.cmd", "log add warn An error occurred, 'screen.button.cmd' not found")); }
            }
        }

        private String getvalue(String key, String fallback) { return lib.containsKey(key) ? (String) lib.get(key) : fallback; }
        private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); }
        private String getenv(String key) { return env(getvalue(key, "")); }

        private int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = replace(mode, "password", "").trim(); } int base = TextField.ANY; if (mode.equals("number")) { base = TextField.NUMERIC; } else if (mode.equals("email")) { base = TextField.EMAILADDR; } else if (mode.equals("phone")) { base = TextField.PHONENUMBER; } else if (mode.equals("decimal")) { base = TextField.DECIMAL; } return password ? (base | TextField.PASSWORD) : base; }
    }
    public class MyCanvas extends Canvas implements CommandListener {
        private Hashtable lib;
        private Graphics screen;
        private Command BACK, USER;
        private Image CURSOR = null;
        private final int cursorSize = 5;

        public MyCanvas(String args) {
            if (args == null || args.length() == 0) { return; }

            lib = parseProperties(getcontent(args));

            if (lib.containsKey("canvas.title")) { setTitle(getenv("canvas.title")); }

            BACK = new Command(getenv("canvas.back.label", "Back"), Command.OK, 1);
            USER = new Command(getenv("canvas.button"), Command.SCREEN, 2); 

            addCommand(BACK); if (lib.containsKey("canvas.button")) { addCommand(USER); }

            if (lib.containsKey("canvas.mouse")) {
                try { String[] pos = split(getenv("canvas.mouse"), ','); cursorX = Integer.parseInt(pos[0]); cursorY = Integer.parseInt(pos[1]); } 
                catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas.mouse' - (x,y) may be a int number"); cursorX = 10; cursorY = 10; }
            }

            if (lib.containsKey("canvas.mouse.img")) { try { CURSOR = Image.createImage(getenv("canvas.mouse.img")); } catch (IOException e) { MIDletLogs("add warn Cursor " + getenv("canvas.mouse.img") + " could not be loaded"); } }

            setCommandListener(this);
        }

        protected void paint(Graphics g) {
            if (screen == null) { screen = g; }

            g.setColor(0, 0, 0);
            g.fillRect(0, 0, getWidth(), getHeight());

            if (lib.containsKey("canvas.background")) {
                String backgroundType = getenv("canvas.background.type", "default");

                if (backgroundType.equals("color") || backgroundType.equals("default")) {
                    setpallete("background", g, 0, 0, 0);
                    g.fillRect(0, 0, getWidth(), getHeight());
                } else if (backgroundType.equals("image")) {
                    try { Image content = Image.createImage(getenv("canvas.background")); g.drawImage(content, (getWidth() - content.getWidth()) / 2, (getHeight() - content.getHeight()) / 2, Graphics.TOP | Graphics.LEFT); } 
                    catch (IOException e) { processCommand("xterm"); processCommand("execute log add error Malformed Image, " + e.getMessage()); }
                }
            }

            if (lib.containsKey("canvas.content")) {
                String contentType = getenv("canvas.content.type", "default")

                g.setFont(Font.getDefaultFont());

                if (lib.containsKey("canvas.content.style")) { g.setFont(newFont((String) lib.get("canvas.content.style"))); }

                if (contentType.equals("text") || contentType.equals("default")) {
                    g.setColor(255, 255, 255);
                    String content = getenv("canvas.content");
                    int contentWidth = g.getFont().stringWidth(content);
                    int contentHeight = g.getFont().getHeight();
                    g.drawString(content, (getWidth() - contentWidth) / 2, (getHeight() - contentHeight) / 2, Graphics.TOP | Graphics.LEFT);
                } else if (contentType.equals("shape")) {
                    String[] shapes = split(getenv("canvas.content"), ';');
                    for (int i = 0; i < shapes.length; i++) {
                        String[] parts = split(shapes[i], ',');
                        String type = parts[0].toLowerCase();

                        if (type.equals("line") && parts.length == 5) {
                            setpallete("line.color", g, 255, 255, 255);
                            g.drawLine(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                        } else if (type.equals("circle") && parts.length == 4) {
                            setpallete("circle.color", g, 0, 255, 0);
                            int radius = Integer.parseInt(parts[3]);
                            g.drawArc( Integer.parseInt(parts[1]) - radius, Integer.parseInt(parts[2]) - radius, radius * 2, radius * 2, 0, 360);
                        } else if (type.equals("rect") && parts.length == 5) {
                            setpallete("rect.color", g, 0, 0, 255);
                            g.drawRect(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                        } else if (type.equals("text") && parts.length == 4) {
                            setpallete("text.color", g, 255, 255, 255);
                            g.drawString(parts[3], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Graphics.TOP | Graphics.LEFT);
                        }
                    }
                }
            }

            if (CURSOR != null) { g.drawImage(CURSOR, cursorX, cursorY, Graphics.TOP | Graphics.LEFT); } 
            else { g.setColor(255, 255, 255); g.fillRect(cursorX, cursorY, cursorSize, cursorSize); }
        }

        protected void keyPressed(int keyCode) {
            int gameAction = getGameAction(keyCode);

            if (gameAction == LEFT) { cursorX = Math.max(0, cursorX - 5); } 
            else if (gameAction == RIGHT) { cursorX = Math.min(getWidth() - cursorSize, cursorX + 5); } 
            else if (gameAction == UP) { cursorY = Math.max(0, cursorY - 5); } 
            else if (gameAction == DOWN) { cursorY = Math.min(getHeight() - cursorSize, cursorY + 5); } 
            else if (gameAction == FIRE) {
                if (lib.containsKey("canvas.content")) {
                    String content = getenv("canvas.content");
                    int contentWidth = screen.getFont().stringWidth(content);
                    int contentHeight = screen.getFont().getHeight();
                    int textX = (getWidth() - contentWidth) / 2;
                    int textY = (getHeight() - contentHeight) / 2;

                    if (cursorX >= textX && cursorX <= textX + contentWidth &&
                        cursorY >= textY && cursorY <= textY + contentHeight) {
                        processCommand(getvalue("canvas.content.link", "true"));
                    }
                }
            }

            repaint();
        }

        protected void pointerPressed(int x, int y) { cursorX = x; cursorY = y; repaint(); }

        public void commandAction(Command c, Displayable d) {
            if (c == BACK) { processCommand("xterm"); processCommand(getvalue("canvas.back", "true")); } 
            else if (c == USER) { processCommand("xterm"); processCommand(getvalue("canvas.button.cmd", "log add warn An error occurred, 'canvas.button.cmd' not found")); }
        }

        private void setpallete(String node, Graphics g, int r, int g, int b) {
            try { 
                String[] pallete = split(getenv("canvas." + node), ',');
                g.setColor(Integer.parseInt(pallete[0]), Integer.parseInt(pallete[1]), Integer.parseInt(pallete[2])); 
            } 
            catch (NumberFormatException e) { MIDletLogs("add warn Invalid value for 'canvas." + node + "' - (r,g,b) may be a int number"); g.setColor(r, g, b); }
            
        }
        private String getvalue(String key, String fallback) { return lib.containsKey(key) ? (String) lib.get(key) : fallback; }
        private String getenv(String key, String fallback) { return env(getvalue(key, fallback)); }
        private String getenv(String key) { return env(getvalue(key, "")); }
    }
    // |
    // Item MOD Loader
    public class ItemLoader implements ItemCommandListener { private Hashtable lib; private Command run; private StringItem s; private String node; public ItemLoader(Form screen, String prefix, String args) { if (args == null || args.length() == 0) { return; } else if (args.equals("clear")) { form.deleteAll(); form.append(stdout); form.append(stdin); return; } lib = parseProperties(getcontent(args)); node = prefix; if (!lib.containsKey(node + ".label") || !lib.containsKey(node + ".cmd")) { MIDletLogs("add error Malformed ITEM, missing params"); return; } run = new Command(env((String) lib.get(node + ".label")), Command.ITEM, 1); s = new StringItem(null, env((String) lib.get(node + ".label")), StringItem.BUTTON); s.setFont(Font.getDefaultFont()); s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(run); s.setDefaultCommand(run); s.setItemCommandListener(this); screen.append(s); } public void commandAction(Command c, Item item) { if (c == run) { processCommand("xterm"); processCommand((String) lib.get(node + ".cmd")); } } }
    // |
    // Font Generator
    private Font newFont(String argument) { if (argument == null || argument.length() == 0 || argument.equals("default")) { return Font.getDefaultFont(); } int style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; if (argument.equals("bold")) { style = Font.STYLE_BOLD; } else if (argument.equals("italic")) { style = Font.STYLE_ITALIC; } else if (argument.equals("ul")) { style = Font.STYLE_UNDERLINED; } else if (argument.equals("small")) { size = Font.SIZE_SMALL; } else if (argument.equals("large")) { size = Font.SIZE_LARGE; } else { return newFont("default"); } return Font.getFont(Font.FACE_SYSTEM, style, size); }

    // API 005 - (Operators)
    // |
    // Operators
    private void ifCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('); int lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("if (expr) [command]"); return; } String expression = argument.substring(firstParenthesis + 1, lastParenthesis).trim(); String command = argument.substring(lastParenthesis + 1).trim(); String[] parts = split(expression, ' '); if (parts.length == 3) { boolean result = false; boolean negated = parts[1].startsWith("!") && !parts[1].equals("!="); if (negated) parts[1] = parts[1].substring(1); Double n1 = getNumber(parts[0]), n2 = getNumber(parts[2]); if (n1 != null && n2 != null) { if (parts[1].equals("==")) { result = n1.doubleValue() == n2.doubleValue(); } else if (parts[1].equals("!=")) { result = n1.doubleValue() != n2.doubleValue(); } else if (parts[1].equals(">")) { result = n1.doubleValue() > n2.doubleValue(); } else if (parts[1].equals("<")) { result = n1.doubleValue() < n2.doubleValue(); } else if (parts[1].equals(">=")) { result = n1.doubleValue() >= n2.doubleValue(); } else if (parts[1].equals("<=")) { result = n1.doubleValue() <= n2.doubleValue(); } } else { if (parts[1].equals("startswith")) { result = parts[0].startsWith(parts[2]); } else if (parts[1].equals("endswith")) { result = parts[0].endsWith(parts[2]); } else if (parts[1].equals("contains")) { result = parts[0].indexOf(parts[2]) != -1; } else if (parts[1].equals("==")) { result = parts[0].equals(parts[2]); } else if (parts[1].equals("!=")) { result = !parts[0].equals(parts[2]); } } if (result != negated) { processCommand(command); } } else if (parts.length == 2) { if (parts[0].equals(parts[1])) { processCommand(command); } } else if (parts.length == 1) { if (!parts[0].equals("")) { processCommand(command); } } }
    private void forCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('); int lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { return; } String key = getCommand(argument); String file = getcontent(argument.substring(firstParenthesis + 1, lastParenthesis).trim()); String command = argument.substring(lastParenthesis + 1).trim(); if (key.startsWith("(")) { return; } if (key.startsWith("$")) { key = replace(key, "$", ""); } String[] lines = split(file, '\n'); for (int i = 0; i < lines.length; i++) { if (lines[i] != null || lines[i].length() != 0) { processCommand("set " + key + "=" + lines[i]); processCommand(command); processCommand("unset " + key); } } }
    private void caseCommand(String argument) {
        argument = argument.trim();

        int firstParenthesis = argument.indexOf('(');
        int lastParenthesis = argument.indexOf(')');

        if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) return;

        String method = getCommand(argument);
        boolean negated = method.startsWith("!");
        if (negated) method = method.substring(1);

        String expression = argument.substring(firstParenthesis + 1, lastParenthesis).trim();
        String command = argument.substring(lastParenthesis + 1).trim();

        boolean condition = false;

        if (method.equals("file")) { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(expression)) { condition = true; break; } } } } 
        else if (method.equals("root")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { if (((String) roots.nextElement()).equals(expression)) { condition = true; break; } } } 
        else if (method.equals("thread")) { condition = replace(replace(Thread.currentThread().getName(), "MIDletEventQueue", "MIDlet"), "Thread-1", "MIDlet").equals(expression); } 
        else if (method.equals("trace")) { condition = trace.containsKey(expression); }
        else if (method.equals("screen")) { condition = desktops.containsKey(expression); } 
        else if (method.equals("alias")) { condition = aliases.containsKey(expression); } 
        else if (method.equals("key")) { condition = attributes.containsKey(expression); }

        if (condition != negated) { processCommand(command); }
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
    private String exprCommand(String expr) { char[] tokens = expr.toCharArray(); double[] vals = new double[32]; char[] ops = new char[32]; int valTop = -1, opTop = -1; int i = 0, len = tokens.length; while (i < len) { char c = tokens[i]; if (c == ' ') { i++; continue; } if (c >= '0' && c <= '9') { double num = 0; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { num = num * 10 + (tokens[i++] - '0'); } if (i < len && tokens[i] == '.') { i++; double frac = 0, div = 10; while (i < len && tokens[i] >= '0' && tokens[i] <= '9') { frac += (tokens[i++] - '0') / div; div *= 10; } num += frac; } vals[++valTop] = num; } else if (c == '(') { ops[++opTop] = c; i++; } else if (c == ')') { while (opTop >= 0 && ops[opTop] != '(') { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } opTop--; i++; } else if (c == '+' || c == '-' || c == '*' || c == '/') { while (opTop >= 0 && prec(ops[opTop]) >= prec(c)) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } ops[++opTop] = c; i++; } else { return "expr: invalid char '" + c + "'"; } } while (opTop >= 0) { double b = vals[valTop--], a = vals[valTop--]; char op = ops[opTop--]; vals[++valTop] = applyOpSimple(op, a, b); } double result = vals[valTop]; return ((int) result == result) ? String.valueOf((int) result) : String.valueOf(result); } private int prec(char op) { if (op == '+' || op == '-') return 1; if (op == '*' || op == '/') return 2; return 0; } private double applyOpSimple(char op, double a, double b) { if (op == '+') return a + b; if (op == '-') return a - b; if (op == '*') return a * b; if (op == '/') return b == 0 ? 0 : a / b; return 0; }
    private String generateUUID() { String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + random.nextInt(4))); } else { uuid.append(chars.charAt(random.nextInt(16))); } } return uuid.toString(); }

    // API 011 - (Network)
    // |
    // Servers
    public class Bind implements Runnable { private String port, prefix; public Bind(String args) { if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522"); new Bind("31522"); return; } port = getCommand(args); prefix = getArgument(args); new Thread(this, "Bind").start(); } public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); start("bind"); while (trace.containsKey("bind")) { SocketConnection clientSocket = null; InputStream is = null; OutputStream os = null; try { clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); is = clientSocket.openInputStream(); os = clientSocket.openOutputStream(); while (trace.containsKey("bind")) { byte[] buffer = new byte[4096]; int bytesRead = is.read(buffer); if (bytesRead == -1) break; String command = new String(buffer, 0, bytesRead).trim(); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(command)); command = (prefix == null || prefix.length() == 0 || prefix.equals("null")) ? command : prefix + " " + command; String beforeCommand = stdout != null ? stdout.getText() : ""; processCommand(command); String afterCommand = stdout != null ? stdout.getText() : ""; String output = afterCommand.length() >= beforeCommand.length() ? afterCommand.substring(beforeCommand.length()).trim() + "\n" : "\n"; os.write(output.getBytes()); os.flush(); } } catch (IOException e) { echoCommand("[-] " + e.getMessage()); } finally { echoCommand("[-] " + clientSocket.getAddress() + " disconnected"); } } echoCommand("[-] Server stopped"); MIDletLogs("add info Server was stopped"); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'"); } try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) { } } }
    public class Server implements Runnable { private String port, response; public Server(String args) { if (args == null || args.length() == 0 || args.equals("$PORT")) { processCommand("set PORT=31522"); new Server("31522"); return; } port = getCommand(args); response = getArgument(args); new Thread(this, "Server").start(); } public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); start("server"); while (trace.containsKey("server")) { SocketConnection clientSocket = null; InputStream is = null; OutputStream os = null; try { clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); is = clientSocket.openInputStream(); os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[4096]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); if (response.startsWith("/")) { os.write(read(response).getBytes()); } else if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(loadRMS(response).getBytes()); } os.flush(); } catch (IOException e) { } finally { try { if (is != null) is.close(); if (os != null) os.close(); if (clientSocket != null) clientSocket.close(); } catch (IOException e) { } } } echoCommand("[-] Server stopped"); MIDletLogs("add info Server was stopped"); } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'");  } try { if (serverSocket != null) { serverSocket.close(); } } catch (IOException e) { } } }
    // |
    // HTTP Interfaces
    private void pingCommand(String url) { if (url == null || url.length() == 0) { return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } }
    private void pongCommand(String address) { if (address == null || address.length() == 0) { }  else { long startTime = System.currentTimeMillis(); SocketConnection socket = null; try { socket = (SocketConnection) Connector.open("socket://" + address); long endTime = System.currentTimeMillis(); echoCommand("Pong to " + address + " successful, time=" + (endTime - startTime) + "ms"); } catch (IOException e) { echoCommand("Pong to " + address + " failed: " + e.getMessage()); } } }
    public class InjectorHTTP implements CommandListener { private String url; private TextBox screen = new TextBox("HTTP Header", read("/java/etc/headers"), 31522, TextField.ANY); private Command BACK = new Command("Back", Command.BACK, 1), CLEAR = new Command("Clear", Command.OK, 1), CURL = new Command("Run 'CURL'", Command.OK, 1), WGET = new Command("Run 'WGET'", Command.OK, 1); public InjectorHTTP(String args) { if (args == null || args.length() == 0) { return; } url = args; screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(CURL); screen.addCommand(WGET); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == CLEAR) { screen.setString(""); } else if (c == CURL) { processCommand("xterm"); echoCommand(request(url, parseProperties(env(screen.getString())))); } else if (c == WGET) { processCommand("xterm"); nanoContent = request(url, parseProperties(env(screen.getString()))); } } }
    public class GoBuster implements CommandListener, Runnable { private String url; private String[] wordlist; private List screen; private Command BACK = new Command("Back", Command.BACK, 1), OPEN = new Command("Get Request", Command.OK, 1), SAVE = new Command("Save Result", Command.OK, 1); public GoBuster(String args) { if (args == null || args.length() == 0) { return; } url = args; screen = new List("GoBuster (" + url + ")", List.IMPLICIT); wordlist = split(loadRMS("gobuster"), '\n'); if (wordlist == null || wordlist.length == 0) { wordlist = split(read("/java/etc/gobuster"), '\n'); } screen.addCommand(BACK); screen.addCommand(OPEN); screen.addCommand(SAVE); screen.setCommandListener(this); new Thread(this, "GoBuster").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == OPEN) { processCommand("bg execute wget " + url + screen.getString(screen.getSelectedIndex()) + "; nano;"); } else if (c == SAVE && screen.size() != 0) { nanoContent = GoSave(); new NanoEditor(""); } } public void run() { screen.setTicker(new Ticker("Searching...")); for (int i = 0; i < wordlist.length; i++) { if (!wordlist[i].startsWith("#") && !wordlist[i].equals("")) { String fullUrl = url.startsWith("http://") || url.startsWith("https://") ? url + "/" + wordlist[i] : "http://" + url + "/" + wordlist[i]; try { if (GoVerify(fullUrl)) { screen.append("/" + wordlist[i], null); } } catch (IOException e) { } } } screen.setTicker(null); } private boolean GoVerify(String fullUrl) throws IOException { HttpConnection conn = null; InputStream is = null; try { conn = (HttpConnection) Connector.open(fullUrl); conn.setRequestMethod(HttpConnection.GET); int responseCode = conn.getResponseCode(); return (responseCode == HttpConnection.HTTP_OK); } finally { if (is != null) { is.close(); } if (conn != null) { conn.close(); } } } private String GoSave() { StringBuffer sb = new StringBuffer(); for (int i = 0; i < screen.size(); i++) { sb.append(screen.getString(i)); if (i < screen.size() - 1) { sb.append("\n"); } } return replace(sb.toString(), "/", ""); } }
    private String request(String url, Hashtable headers) { if (url == null || url.length() == 0) { return ""; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); if (headers != null) { Enumeration keys = headers.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) headers.get(key); conn.setRequestProperty(key, value); } } InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } is.close(); conn.close(); return new String(baos.toByteArray(), "UTF-8"); } catch (IOException e) { return e.getMessage(); } }
    private String request(String url) { return request(url, null); }
    // |
    // Socket Interfaces
    private void query(String command) { command = env(command.trim()); String mainCommand = getCommand(command); String argument = getArgument(command); if (mainCommand.equals("")) { echoCommand("query: missing [address]"); return; } try { StreamConnection conn = (StreamConnection) Connector.open(mainCommand); InputStream inputStream = conn.openInputStream(); OutputStream outputStream = conn.openOutputStream(); if (!argument.equals("")) { outputStream.write((argument + "\r\n").getBytes()); outputStream.flush(); } byte[] buffer = new byte[31522]; int length = inputStream.read(buffer); if (length != -1) { String data = new String(buffer, 0, length); if (env("$QUERY").equals("$QUERY") || env("$QUERY").equals("")) { echoCommand(data); MIDletLogs("add warn Query storage setting not found"); } else if (env("$QUERY").toLowerCase().equals("show")) { echoCommand(data); } else if (env("$QUERY").toLowerCase().equals("nano")) { nanoContent = data; echoCommand("query: data retrieved"); } else { writeRMS(env("$QUERY"), data); } } inputStream.close(); outputStream.close(); conn.close(); } catch (Exception e) { echoCommand(e.getMessage()); } }
    public class GetAddress { public GetAddress(String args) { if (args == null || args.length() == 0) { processCommand("ifconfig"); } else { echoCommand(performNSLookup(args)); } } private String performNSLookup(String domain) { try { DatagramConnection conn = (DatagramConnection) Connector.open("datagram://1.1.1.1:53"); byte[] query = createDNSQuery(domain); Datagram request = conn.newDatagram(query, query.length); conn.send(request); Datagram response = conn.newDatagram(512); conn.receive(response); conn.close(); return parseDNSResponse(response.getData()); } catch (IOException e) { return e.getMessage(); } } private byte[] createDNSQuery(String domain) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(0x12); out.write(0x34); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); String[] parts = split(domain, '.'); for (int i = 0; i < parts.length; i++) { out.write(parts[i].length()); out.write(parts[i].getBytes()); } out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00); out.write(0x01); return out.toByteArray(); } private String parseDNSResponse(byte[] response) { if ((response[3] & 0x0F) != 0) { return "not found"; } int answerOffset = 12; while (response[answerOffset] != 0) { answerOffset++; } answerOffset += 5; if (response[answerOffset + 2] == 0x00 && response[answerOffset + 3] == 0x01) { StringBuffer ip = new StringBuffer(); for (int i = answerOffset + 12; i < answerOffset + 16; i++) { ip.append(response[i] & 0xFF); if (i < answerOffset + 15) ip.append("."); } return ip.toString(); } else { return "not found"; } } }
    public class PortScanner implements CommandListener, Runnable { private String host; private int start = 1; private List screen; private Command BACK = new Command("Back", Command.BACK, 1), CONNECT = new Command("Connect", Command.OK, 1); public PortScanner(String args) { if (args == null || args.length() == 0) { return; } if (!getArgument(args).equals("")) { try { start = Integer.parseInt(getArgument(args)); } catch (NumberFormatException e) { echoCommand("prscan: " + getArgument(args) + ": invalid start port"); return; } } host = getCommand(args); screen = new List(host + " Ports", List.IMPLICIT); screen.addCommand(BACK); screen.addCommand(CONNECT); screen.setCommandListener(this); new Thread(this, "Port-Scanner").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { processCommand("xterm"); } else if (c == CONNECT) { new RemoteConnection(host + ":" + screen.getString(screen.getSelectedIndex())); } } public void run() { screen.setTicker(new Ticker("Scanning...")); for (int port = start; port <= 65535; port++) { screen.setTicker(new Ticker("Scanning Port " + port + "...")); try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port, Connector.READ_WRITE, false); screen.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } screen.setTicker(null); } }
    public class RemoteConnection implements CommandListener, Runnable { private SocketConnection socket; private InputStream inputStream; private OutputStream outputStream; private String host; private Form screen = new Form(form.getTitle()); private TextField inputField = new TextField("Command", "", 256, TextField.ANY); private Command BACK = new Command("Back", Command.SCREEN, 1), EXECUTE = new Command("Send", Command.OK, 1), CLEAR = new Command("Clear", Command.SCREEN, 1), VIEW = new Command("Show info", Command.SCREEN, 1); private StringItem console = new StringItem("", ""); public RemoteConnection(String args) { if (args == null || args.length() == 0) { return; } host = args; inputField.setLabel("Remote (" + split(args, ':')[0] + ")"); screen.append(console); screen.append(inputField); screen.addCommand(EXECUTE); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(VIEW); screen.setCommandListener(this); try { socket = (SocketConnection) Connector.open("socket://" + args); inputStream = socket.openInputStream(); outputStream = socket.openOutputStream(); } catch (IOException e) { echoCommand(e.getMessage()); return; } start("remote"); new Thread(this, "Remote").start(); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == EXECUTE) { String data = inputField.getString().trim(); inputField.setString(""); try { outputStream.write((data + "\n").getBytes()); outputStream.flush(); } catch (IOException e) { processCommand("warn " + e.getMessage()); } } else if (c == BACK) { try { outputStream.write("".getBytes()); outputStream.flush(); inputStream.close(); outputStream.close(); } catch (IOException e) { } writeRMS("remote", console.getText()); stop("remote"); processCommand("xterm"); } else if (c == CLEAR) { console.setText(""); } else if (c == VIEW) { try { warnCommand("Information", "Host: " + split(host, ':')[0] + "\n" + "Port: " + split(host, ':')[1] + "\n\n" + "Local Address: " + socket.getLocalAddress() + "\n" + "Local Port: " + socket.getLocalPort()); } catch (IOException e) { } } } public void run() { while (trace.containsKey("remote")) { try { byte[] buffer = new byte[4096]; int length = inputStream.read(buffer); if (length != -1) { echoCommand(new String(buffer, 0, length), console); } } catch (IOException e) { processCommand("warn " + e.getMessage()); stop("remote"); } } } }

    // API 012 - (File)
    // |
    // Directories Manager
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.length() == 0 || line.startsWith("#")) { continue; } if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath + "/"); start = j; } } String finalPart = line.substring(start + 1); fullPath += "/" + finalPart; if (line.endsWith("/")) { addDirectory(fullPath + "/"); } else { addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { boolean isDirectory = fullPath.endsWith("/"); if (!paths.containsKey(fullPath)) { if (isDirectory) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/', fullPath.length() - 2) + 1); String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } String dirName = fullPath.substring(parentPath.length(), fullPath.length() - 1); updatedContents.addElement(dirName + "/"); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } else { String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/') + 1); String fileName = fullPath.substring(fullPath.lastIndexOf('/') + 1); String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fileName); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } } }
    public class Explorer implements CommandListener {

        private List screen = new List(form.getTitle(), List.IMPLICIT);
        private Command BACK = new Command("Back", Command.BACK, 1),
                        OPEN = new Command("Open", Command.OK, 1),
                        DELETE = new Command("Delete", Command.OK, 2),
                        RUN = new Command("Run Script", Command.OK, 3),
                        IMPORT = new Command("Import File", Command.OK, 4);
        private Image DIR = null, FILE = null, UP = null;

        public Explorer() {
            screen.addCommand(BACK);
            screen.addCommand(OPEN);
            screen.setCommandListener(this);
            try {
                FILE = Image.createImage("/java/etc/icons/file.png");
                DIR = Image.createImage("/java/etc/icons/dir.png");
                UP = Image.createImage("/java/etc/icons/up.png");
            } catch (IOException e) { }
            load();
            display.setCurrent(screen);
        }

        public void commandAction(Command c, Displayable d) {
            String selected = screen.getString(screen.getSelectedIndex());

            if (c == BACK) {
                processCommand("xterm");
            } else if (c == OPEN) {
                if (selected != null) {
                    if (selected.endsWith("..")) {
                        int lastSlash = path.lastIndexOf('/', path.length() - 2);
                        if (lastSlash != -1) {
                            path = path.substring(0, lastSlash + 1);
                        }
                    } else if (selected.endsWith("/")) {
                        path += selected;
                    } else {
                        new NanoEditor(path + selected);
                    }
                    stdin.setLabel(username + " " + path + " $");
                    load();
                }
            } else if (c == DELETE) {
                deleteFile(path + selected);
                load();
            } else if (c == RUN) {
                processCommand("xterm");
                runScript(getcontent(path + selected));
            } else if (c == IMPORT) {
                processCommand("xterm");
                importScript(path + selected);
            }
        }

        private void load() {
            screen.deleteAll();

            if (!path.equals("/")) {
                screen.append("..", UP);
            }

            if (isWritable(path)) {
                screen.addCommand(DELETE);
            } else {
                screen.removeCommand(DELETE);
            }

            if (isRoot(path)) {
                screen.removeCommand(RUN);
                screen.removeCommand(IMPORT);
            } else {
                screen.addCommand(RUN);
                screen.addCommand(IMPORT);
            }

            try {
                if (path.equals("/mnt/")) {
                    Enumeration roots = FileSystemRegistry.listRoots();
                    while (roots.hasMoreElements()) {
                        screen.append((String) roots.nextElement(), DIR);
                    }
                } else if (path.startsWith("/mnt/")) {
                    try {
                        FileConnection dir = (FileConnection) Connector.open("file:///" + path.substring(5), Connector.READ);
                        Enumeration content = dir.list();
                        Vector dirs = new Vector(), files = new Vector();

                        while (content.hasMoreElements()) {
                            String name = (String) content.nextElement();
                            if (name.endsWith("/")) {
                                dirs.addElement(name);
                            } else {
                                files.addElement(name);
                            }
                        }

                        while (!dirs.isEmpty()) {
                            screen.append(getFirstString(dirs), DIR);
                        }
                        while (!files.isEmpty()) {
                            screen.append(getFirstString(files), FILE);
                        }

                        dir.close();
                    } catch (IOException e) { }
                } else if (path.equals("/home/")) {
                    try {
                        String[] recordStores = RecordStore.listRecordStores();
                        for (int i = 0; i < recordStores.length; i++) {
                            if (!recordStores[i].startsWith(".")) {
                                screen.append(recordStores[i], FILE);
                            }
                        }
                    } catch (RecordStoreException e) { }
                }

                String[] files = (String[]) paths.get(path);
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        String f = files[i];
                        if (f != null && !f.equals("..") && !f.equals("/")) {
                            if (f.endsWith("/")) {
                                screen.append(f, DIR);
                            } else {
                                screen.append(f, FILE);
                            }
                        }
                    }
                }

            } catch (IOException e) { }
        }


        private boolean isWritable(String path) {
            return path.startsWith("/home/") || (path.startsWith("/mnt/") && !path.equals("/mnt/"));
        }

        private boolean isRoot(String path) {
            return path.equals("/") || path.equals("/mnt/");
        }

        private static String getFirstString(Vector v) {
            String result = null;
            for (int i = 0; i < v.size(); i++) {
                String cur = (String) v.elementAt(i);
                if (result == null || cur.compareTo(result) < 0) {
                    result = cur;
                }
            }
            v.removeElement(result);
            return result;
        }
    }

    private String readStack() { StringBuffer sb = new StringBuffer(); sb.append(path); for (int i = 0; i < stack.size(); i++) { sb.append(" ").append((String) stack.elementAt(i)); } return sb.toString(); }
    // |
    // RMS Files
    private void deleteFile(String filename) { if (filename == null || filename.length() == 0) { return; } else if (filename.startsWith("/mnt/")) { try { FileConnection conn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (conn.exists()) { conn.delete(); } else { echoCommand("rm: " + basename(filename) + ": not found"); } conn.close(); } catch (Exception e) { echoCommand(e.getMessage()); } } else if (filename.startsWith("/home/")) { try { RecordStore.deleteRecordStore(filename.substring(6)); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename.substring(6) + ": not found"); } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); } } else if (filename.startsWith("/")) { echoCommand("read-only storage"); } else { deleteFile(path + filename); } }
    private void writeRMS(String filename, String data) { if (filename == null || filename.length() == 0) { return; } if (filename.startsWith("/mnt/")) { try { FileConnection conn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!conn.exists()) { conn.create(); } OutputStream os = conn.openOutputStream(); os.write(data.getBytes()); os.flush(); } catch (Exception e) { echoCommand(e.getMessage()); } } else if (filename.startsWith("/home/")) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(filename.substring(6), true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } }  catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } } else if (filename.startsWith("/")) { echoCommand("read-only storage"); } else { writeRMS(path + filename, data); } }
    private String loadRMS(String filename) { return read("/home/" + filename); }
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
    public class NanoEditor implements CommandListener { private TextBox screen = new TextBox("Nano", "", 31522, TextField.ANY); private Command BACK = new Command("Back", Command.BACK, 1), CLEAR = new Command("Clear", Command.OK, 1), RUN = new Command("Run Script", Command.OK, 1), IMPORT = new Command("Import File", Command.OK, 1), VIEW = new Command("View as HTML", Command.OK, 1); public NanoEditor(String args) { screen.setString((args == null || args.length() == 0) ? nanoContent : getcontent(args)); screen.addCommand(BACK); screen.addCommand(CLEAR); screen.addCommand(RUN); screen.addCommand(IMPORT); screen.addCommand(VIEW); screen.setCommandListener(this); display.setCurrent(screen); } public void commandAction(Command c, Displayable d) { if (c == BACK) { nanoContent = screen.getString(); processCommand("xterm"); } else if (c == CLEAR) { screen.setString(""); } else if (c == RUN) { nanoContent = screen.getString(); processCommand("xterm"); runScript(nanoContent); } else if (c == IMPORT) { nanoContent = screen.getString(); processCommand("xterm"); importScript("nano"); } else if (c == VIEW) { nanoContent = screen.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }
    private String extractTitle(String htmlContent) { int titleStart = htmlContent.indexOf("<title>"); int titleEnd = htmlContent.indexOf("</title>"); if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) { return htmlContent.substring(titleStart + 7, titleEnd).trim(); } return "HTML Viewer"; }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }

    // API 013 - (MIDlet)
    // |
    // Java Runtime
    private void java(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command);
        String argument = getArgument(command);

        if (mainCommand.equals("")) { viewer("Java ME", env("Java 1.2 (OpenTTY Edition)\n\nMicroEdition-Config: $CONFIG\nMicroEdition-Profile: $PROFILE")); }
        else if (mainCommand.equals("-class")) { if (argument.equals("")) { } else { try { Class.forName(argument); echoCommand("true"); } catch (ClassNotFoundException e) { echoCommand("false"); } } } 
        else if (mainCommand.equals("--version")) { echoCommand("Java 1.2 (OpenTTY Edition)"); } 
        else { String code = getcontent(mainCommand); Hashtable objects = new Hashtable(); if (code == null || code.length() == 0) { echoCommand("java: " + mainCommand + ": blank class"); return; } String[] lines = split(code, ';'); for (int i = 0; i < lines.length; i++) { String line = lines[i].trim(); if (line.length() == 0) { continue; } try { if (line.indexOf('=') != -1) { String[] parts = split(line, '='); String objectName = parts[0].trim(); String className = parts[1].trim(); Class clazz = Class.forName(className); Object instance = clazz.newInstance(); objects.put(objectName, instance); } else if (line.indexOf('.') != -1) { String[] parts = split(line, '.'); String objectName = parts[0].trim(); if (!objects.containsKey(objectName)) { throw new IOException("Object not found"); } for (int j = 1; j < parts.length; j++) { Object object = (Object) objects.get(objectName); Class clazz = object.getClass(); echoCommand("Invoke method '" + parts[j] + "' on object '" + objectName + "' of class '" + clazz.getName() + "'."); } } else if (line.startsWith("//")) { } else { throw new IOException("Syntax error"); } } catch (Exception e) { echoCommand(e.getClass().getName() + ": '" + line + "' (" + e.getMessage() + ")"); return; } } } }
    // | 
    // Permissions
    private void chmod(String node) { if (node == null || node.length() == 0) { return; } Hashtable nodes = parseProperties(read("/java/etc/perms.ini")); int status = 1; if (nodes.containsKey(node)) { try { if (node.equals("http")) { ((HttpConnection) Connector.open("http://google.com")).close(); } else if (node.equals("socket")) { ((SocketConnection) Connector.open(env("socket://$REPO"))).close(); } else if (node.equals("file")) { FileSystemRegistry.listRoots(); } else if (node.equals("prg")) { PushRegistry.registerAlarm(getClass().getName(), System.currentTimeMillis() + 1000); } } catch (SecurityException e) { status = 2; } catch (Exception e) { echoCommand("chmod: " + e.getMessage()); return; } } else if (node.equals("*")) { Enumeration keys = nodes.keys(); while (keys.hasMoreElements()) { chmod((String) keys.nextElement()); } return; } else { echoCommand("chmod: " + node + ": not found"); return; } if (status == 1) MIDletLogs("add info Permission '" + (String) nodes.get(node) + "' granted"); else if (status == 2) MIDletLogs("add error Permission '" + (String) nodes.get(node) + "' denied"); }
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
        // Generate items - Command & Files
        if (lib.containsKey("command")) { String[] command = split((String) lib.get("command"), ','); for (int i = 0; i < command.length; i++) { if (lib.containsKey(command[i])) { aliases.put(command[i], env((String) lib.get(command[i]))); } else { MIDletLogs("add error Failed to create command '" + command[i] + "' content not found"); } } }
        if (lib.containsKey("file")) { String[] file = split((String) lib.get("file"), ','); for (int i = 0; i < file.length; i++) { if (lib.containsKey(file[i])) { writeRMS("/home/" + file[i], env((String) lib.get(file[i]))); } else { MIDletLogs("add error Failed to create file '" + file[i] + "' content not found"); } } }
        // |
        // Build APP Shell
        if (lib.containsKey("shell.name") && lib.containsKey("shell.args")) { build(lib); }
    }
    private void build(Hashtable lib) { String name = (String) lib.get("shell.name"); String[] args = split((String) lib.get("shell.args"), ','); Hashtable shellTable = new Hashtable(); for (int i = 0; i < args.length; i++) { String argName = args[i].trim(); String argValue = (String) lib.get(argName); shellTable.put(argName, (argValue != null) ? argValue : ""); } if (lib.containsKey("shell.unknown")) { shellTable.put("shell.unknown", (String) lib.get("shell.unknown")); } shell.put(name, shellTable); }
    private void runScript(String script) { String[] commands = split(script, '\n'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
}