import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;


public class OpenTTY extends MIDlet implements CommandListener {
    private boolean app;
    private int currentIndex = 0;
    private String logs = "";
    private String path = "/";
    private Random random = new Random();
    private Hashtable paths = new Hashtable();
    private Hashtable shell = new Hashtable();
    private Hashtable aliases = new Hashtable();
    private Vector commandHistory = new Vector();
    private Hashtable attributes = new Hashtable();
    private String username = loadRMS("OpenRMS", 1);
    private String nanoContent = loadRMS("nano", 1);
    private Display display = Display.getDisplay(this);
    private Command enterCommand = new Command("Send", Command.OK, 1);
    private Command helpCommand = new Command("Help", Command.SCREEN, 2);
    private Command nanoCommand = new Command("Nano", Command.SCREEN, 3);
    private Command clearCommand = new Command("Clear", Command.SCREEN, 4);
    private Command historyCommand = new Command("History", Command.SCREEN, 5);
    private Form form = new Form("OpenTTY " + getAppProperty("MIDlet-Version"));
    private TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    private StringItem stdout = new StringItem("", "Welcome to OpenTTY " + getAppProperty("MIDlet-Version") + "\nCopyright (C) 2024 - Mr. Lima\n");
    
    public void startApp() {
        if (!app == true) {
            mount(read("/java/etc/fstab")); stdin.setLabel(username + " " + path + " $"); app = true;
            
            attributes.put("PATCH", "Misc Update"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.5");
            attributes.put("TTY", "/java/optty1"); attributes.put("HOSTNAME", "localhost"); attributes.put("PORT", "31522"); attributes.put("RESPONSE", "/java/etc/index.html"); attributes.put("QUERY", "nano");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));
            attributes.put("OUTPUT", "");  
        
            form.append(stdout); form.append(stdin);
            form.addCommand(enterCommand); form.addCommand(helpCommand); 
            form.addCommand(nanoCommand); form.addCommand(clearCommand);  
            form.addCommand(historyCommand);
            form.setCommandListener(this);
            
            if (username.equals("")) { login(); }
            else { display.setCurrent(form); runScript(read("/java/etc/initd.sh")); runScript(loadRMS("initd", 1)); }
        }    
    }

    public void pauseApp() { app = true; }
    public void destroyApp(boolean unconditional) { writeRMS("nano", nanoContent); }

    public void commandAction(Command c, Displayable d) {
        if (c == enterCommand) { String command = stdin.getString().trim(); if (!command.equals("")) { commandHistory.addElement(command.trim()); } stdin.setString(""); processCommand(command); stdin.setLabel(username + " " + path + " $"); } 
            
        else if (c == clearCommand) { stdout.setText(""); }
        else if (c == helpCommand) { processCommand("help"); } 
        else if (c == historyCommand) { showHistory(); }
        else if (c == nanoCommand) { nano(""); }
        
        else if (c.getCommandType() == Command.BACK) { processCommand("xterm"); }
        else if (c.getCommandType() == Command.EXIT) { processCommand("exit"); }
    }
    
    
    // OpenTTY Command Processor
    private void processCommand(String command) { processCommand(command, true); }
    private void processCommand(String command, boolean ignore) {
        command = command.startsWith("execute") ? command.trim() : env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (shell.containsKey(mainCommand) && ignore) { Hashtable args = (Hashtable) shell.get(mainCommand); if (argument.equals("")) { if (aliases.containsKey(mainCommand)) { processCommand((String) aliases.get(mainCommand)); } } else if (args.containsKey(getCommand(argument).toLowerCase())) { processCommand((String) args.get(getCommand(argument)) + " " + getArgument(argument)); } else { echoCommand(mainCommand + ": " + getCommand(argument) + ": not found"); } return; }
        if (aliases.containsKey(mainCommand) && ignore) { processCommand((String) aliases.get(mainCommand) + " " + argument); return; }
        

        if (mainCommand.equals("")) { }
        
        // Network Utilities
        else if (mainCommand.equals("query")) { query(argument); }
        else if (mainCommand.equals("ping")) { pingCommand(argument); }
        else if (mainCommand.equals("prscan")) { portScanner(argument); }
        else if (mainCommand.equals("gaddr")) { new GetAddress(argument); }
        else if (mainCommand.equals("server")) { runServer(env("$PORT"));  }
        else if (mainCommand.equals("gobuster")) { new GoBuster(argument); }
        else if (mainCommand.equals("nc")) { new RemoteConnection(argument); }
        else if (mainCommand.equals("curl")) { if (argument.equals("")) { return; } else { echoCommand(request(argument)); } }
        else if (mainCommand.equals("wget")) { if (argument.equals("")) { return; } else { nanoContent = request(argument); } }
        else if (mainCommand.equals("fw")) { echoCommand(request("http://ipinfo.io/" + (argument.equals("") ? "json" : argument))); }
        else if (mainCommand.equals("org")) { echoCommand(request("http://ipinfo.io/" + (argument.equals("") ? "org" : argument + "/org"))); }
        else if (mainCommand.equals("genip")) { echoCommand(random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256) + "." + random.nextInt(256)); }
        else if (mainCommand.equals("netstat")) { try { HttpConnection conn = (HttpConnection) Connector.open("http://ipinfo.io/ip"); conn.setRequestMethod(HttpConnection.GET); if (conn.getResponseCode() == HttpConnection.HTTP_OK) { echoCommand("true"); } else { echoCommand("false"); } conn.close(); } catch (Exception e) { echoCommand("false"); } }
        else if (mainCommand.equals("ifconfig")) { try { SocketConnection socketConnection = (SocketConnection) Connector.open("socket://1.1.1.1:53"); echoCommand(socketConnection.getLocalAddress()); socketConnection.close(); } catch (IOException e) { echoCommand("null"); } }
        
        // File Utilities
        else if (mainCommand.equals("nano")) { nano(argument); }
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("rm")) { deleteFile(argument); }
        else if (mainCommand.equals("install")) { install(argument); }
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("unmount")) { paths = new Hashtable(); }
        else if (mainCommand.equals("rraw")) { stdout.setText(nanoContent); }
        else if (mainCommand.equals("getty")) { nanoContent = stdout.getText(); }
        else if (mainCommand.equals("pjnc")) { nanoContent = parseJson(nanoContent); }
        else if (mainCommand.equals("ls")) { viewer("Resources", read("/java/resources.txt")); }
        else if (mainCommand.equals("html")) { viewer(extractTitle(env(nanoContent)), html2text(env(nanoContent))); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(argument.equals("") ? nanoContent : loadRMS(argument, 1))); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent.equals("") ? argument + "\n" : nanoContent + "\n" + argument; }
        else if (mainCommand.equals("touch") || mainCommand.equals("rnano")) { if (argument.equals("")) { nanoContent = ""; } else { writeRMS(argument, ""); } }
        else if (mainCommand.equals("fdisk")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { echoCommand((String) roots.nextElement()); } }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { echoCommand(read(argument)); } else { echoCommand(loadRMS(argument, 1)); } } }
        else if (mainCommand.equals("get")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { nanoContent = read(argument); } else { nanoContent = loadRMS(argument, 1); } } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { writeRMS(getArgument(argument).equals("") ? getCommand(argument) + "-copy" : getArgument(argument), loadRMS(getCommand(argument), 1)); } }
        else if (mainCommand.equals("du")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { echoCommand(basename(argument) + " (Size: " + read(argument).length() + " B)"); } else if (argument.equals("nano")) { echoCommand(argument + " (Size: " + nanoContent.length() + " B)"); } else { echoCommand(argument + " (Size: " + loadRMS(argument, 1).length() + " B)"); } } }
        else if (mainCommand.equals("ph2s")) { StringBuffer script = new StringBuffer(); for (int i = 0; i < commandHistory.size() - 1; i++) { script.append(commandHistory.elementAt(i)); if (i < commandHistory.size() - 1) { script.append("\n"); } } if (argument.equals("") || argument.equals("nano")) { nanoContent = "#!/java/bin/sh\n\n" + script.toString(); } else { writeRMS(argument, "#!/java/bin/sh\n\n" + script.toString()); } }
        else if (mainCommand.equals("cd")) { if (argument.equals("")) { path = "/"; } else { if (argument.startsWith("/")) { if (paths.containsKey(argument)) { path = argument; } else { echoCommand("cd: " + basename(argument) + ": not found"); } } else if (argument.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { processCommand(path.equals("/") ? "cd " + "/" + argument : "cd " + path + "/" + argument); } } }
        else if (mainCommand.equals("find")) {if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); String file; if (args[1].startsWith("/")) { file = read(args[1]); } else if (args[1].equals("nano")) { file = nanoContent; } else { file = loadRMS(args[1], 1); } String value = (String) parseProperties(file).get(args[0]); echoCommand(value != null ? value : "null"); } }
        else if (mainCommand.equals("grep")) {if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); String file; if (args[1].startsWith("/")) { file = read(args[1]); } else if (args[1].equals("nano")) { file = nanoContent; } else { file = loadRMS(args[1], 1); } echoCommand(file.indexOf(args[0]) != -1 ? "true" : "false"); } }
        else if (mainCommand.equals("dir")) { if (argument.equals("f")) { explorer(); } else if (argument.equals("s")) { new FileExplorer(display, form); } else { String[] files = (String[]) paths.get(path); for (int i = 0; i < files.length; i++) { if (!files[i].equals("..")) { echoCommand(files[i].trim()); } } } }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { mount(read(argument)); } else if (argument.equals("nano")) { mount(nanoContent); } else { mount(loadRMS(argument, 1)); } } }


        // General 
        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("buff")) { stdin.setString(argument); }
        else if (mainCommand.equals("bg")) { final String bgCommand = argument; new Thread(new Runnable() { public void run() { processCommand(bgCommand); } }).start(); }
        else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { processCommand(argument, false); }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
        else if (mainCommand.equals("break")) { app = false; }
        else if (mainCommand.equals("cal")) { calendar(); }
        else if (mainCommand.equals("call")) { callCommand(argument); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { stdout.setText(""); } 
        else if (mainCommand.equals("date")) { echoCommand(new java.util.Date().toString()); } 
        else if (mainCommand.equals("debug")) { runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("env")) { if (attributes.containsKey(argument)) { echoCommand(argument + "=" + (String) attributes.get(argument)); } else { Enumeration keys = attributes.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) attributes.get(key); if (!key.equals("OUTPUT") && !value.equals("")) { echoCommand(key + "=" + value.trim()); } } } }
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("exit") || mainCommand.equals("quit")) { writeRMS("nano", nanoContent); notifyDestroyed(); }
        else if (mainCommand.equals("export")) { if (argument.equals("")) { processCommand("env"); } else { attributes.put(argument, ""); } }
        else if (mainCommand.equals("execute")) { String[] commands = split(argument, ';'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
        else if (mainCommand.equals("forget")) { commandHistory = new Vector(); }
        else if (mainCommand.equals("gc")) { Runtime.getRuntime().gc(); }
        else if (mainCommand.equals("hostname")) { echoCommand(env("$HOSTNAME")); } 
        else if (mainCommand.equals("htop")) { htopCommand(); }
        else if (mainCommand.equals("help")) { viewer("OpenTTY Help", read("/java/help.txt")); }
        else if (mainCommand.equals("hash")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { echoCommand("" + read(argument).hashCode()); } else if (argument.equals("nano")) { echoCommand("" + nanoContent.hashCode()); } else { echoCommand("" + loadRMS(argument, 1).hashCode()); } } }
        else if (mainCommand.equals("history")) { showHistory(); }
        else if (mainCommand.equals("if")) { ifCommand(argument); }
        else if (mainCommand.equals("log")) { MIDletLogs(argument); }
        else if (mainCommand.equals("logcat")) { echoCommand(logs); }
        else if (mainCommand.equals("logout")) { writeRMS("OpenRMS", ""); processCommand("exit"); }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("lock")) { lockCommand(); }
        else if (mainCommand.equals("open")) { openCommand(argument); }
        else if (mainCommand.equals("pkg")) { echoCommand(argument.equals("") ? getAppProperty("MIDlet-Name") : getAppProperty(argument)); }
        else if (mainCommand.equals("run")) { if (argument.equals("")) { runScript(nanoContent); } else { runScript(loadRMS(argument, 1)); } }
        else if (mainCommand.equals("reset")) { try { long alarmTime = System.currentTimeMillis() + argument.length() == 0 ? 5000 : Integer.parseInt(argument); PushRegistry.registerAlarm(getClass().getName(), alarmTime); processCommand("exit"); } catch (Exception e) { echoCommand("AutoRunError: " + e.getMessage()); } }
        else if (mainCommand.equals("sleep")) { if (argument.equals("")) { } else { try { Thread.sleep(Integer.parseInt(argument) * 1000); } catch (InterruptedException e) { } catch (IOException e) { echoCommand(e.getMessage()); } } }
        else if (mainCommand.equals("seed")) { echoCommand("" +  random.nextInt(999) + ""); }
        else if (mainCommand.equals("set")) { setCommand(argument); }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { processCommand("import /java/bin/sh"); }
        else if (mainCommand.equals("true") || mainCommand.equals("false") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("time")) { echoCommand(split(new java.util.Date().toString(), ' ')[3]); }
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(stdout.getText().length() + " B"); }
        else if (mainCommand.equals("trim")) { stdout.setText(stdout.getText().trim()); }
        else if (mainCommand.equals("title")) { form.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument); }
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        else if (mainCommand.equals("uname")) { echoCommand(env("$TYPE $CONFIG $PROFILE")); }
        else if (mainCommand.equals("unset")) { unsetCommand(argument); }
        else if (mainCommand.equals("vendor")) { echoCommand(getAppProperty("MIDlet-Vendor")); }
        else if (mainCommand.equals("version")) { echoCommand(env("OpenTTY $VERSION")); }
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { echoCommand(username); } 
        else if (mainCommand.equals("warn")) { warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("xterm")) { display.setCurrent(form); }
        else if (mainCommand.equals("x11")) { xserver(argument); }
        
        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { importScript(argument); }

        else if (mainCommand.equals("github")) { openCommand(getAppProperty("MIDlet-Info-URL")); }
        //else if (mainCommand.equals("")) {  }
        
        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE"));  }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { runScript(read(argument)); } else { runScript(read(path + "/" + argument)); } } }
        
        else { echoCommand(mainCommand + ": not found"); }

    }
    
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    private String extractTitle(String htmlContent) { int titleStart = htmlContent.indexOf("<title>"); int titleEnd = htmlContent.indexOf("</title>"); if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) { return htmlContent.substring(titleStart + 7, titleEnd).trim(); } return "HTML Viewer"; }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false; boolean inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inTitle) { text.append(c); } } return text.toString().trim(); }
    private String parseJson(String text) { Hashtable properties = parseProperties(text); if (properties.isEmpty()) { return "{}"; } Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\n  \"").append(key).append("\": "); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("\n}"); return jsonBuffer.toString(); }
    private String loadRMS(String recordStoreName, int recordId) { RecordStore recordStore = null; String result = ""; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); if (recordStore.getNumRecords() >= recordId) { byte[] data = recordStore.getRecord(recordId); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { result = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return result; }
    private String read(String filename) { try { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } catch (IOException e) { return e.getMessage(); } }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0; int end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); for (Enumeration e = attributes.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } return text; }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String request(String url) { if (url == null || url.length() == 0) { return ""; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } is.close(); conn.close(); return new String(baos.toByteArray(), "UTF-8"); } catch (IOException e) { return e.getMessage(); } }

    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }
    private Hashtable parseFrom(String script) { if (script.startsWith("/")) { script = read(script); } else if (script.equals("nano")) { script = nanoContent; } else { script = loadRMS(script, 1); } return parseProperties(script); }

    private void aliasCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (aliases.containsKey(argument)) { echoCommand("alias " + argument + "='" + (String) aliases.get(argument) + "'"); } else { Enumeration keys = aliases.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) aliases.get(key); echoCommand("alias " + key + "='" + value.trim() + "'"); } } return; } String aliasName = argument.substring(0, equalsIndex).trim(); String aliasCommand = argument.substring(equalsIndex + 1).trim(); aliases.put(aliasName, aliasCommand); }
    private void unaliasCommand(String aliasName) { if (aliasName == null || aliasName.length() == 0) { echoCommand("unalias: missing [alias]"); return; } if (aliases.containsKey(aliasName)) { aliases.remove(aliasName); } else { echoCommand("unalias: " + aliasName + ": not found"); } }
    private void setCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (attributes.containsKey(argument)) { echoCommand(argument + "=" + (String) attributes.get(argument)); } else { /* Exportation File */ } return; } String key = argument.substring(0, equalsIndex).trim(); String value = argument.substring(equalsIndex + 1).trim(); attributes.put(key, value); }
    private void unsetCommand(String key) { if (key == null || key.length() == 0) { return; } if (attributes.containsKey(key)) { attributes.remove(key); } }
    
    private void echoCommand(String message) { echoCommand(message, stdout); attributes.put("OUTPUT", message); }
    private void echoCommand(String message, StringItem console) { console.setText(console.getText().equals("") ? message.trim() : console.getText() + "\n" + message.trim()); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); }
    private void lockCommand() { final Form lock = new Form(form.getTitle() + " - Locked"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); lock.append(userField); lock.addCommand(new Command("Unlock", Command.OK, 1)); lock.addCommand(new Command("Exit", Command.SCREEN, 2)); lock.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c.getCommandType() == Command.OK) { if (userField.getString().equals(username)) { processCommand("xterm"); } } else if (c.getCommandType() == Command.SCREEN) { processCommand("exit"); } } } ); display.setCurrent(lock); }
    private void htopCommand() { Runtime runtime = Runtime.getRuntime(); viewer(form.getTitle(), "Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\nFree Memory: " + runtime.freeMemory() / 1024 + " KB\nTotal Memory: " + runtime.totalMemory() / 1024 + " KB"); }
    
    private void showHistory() { final List historyList = new List(form.getTitle(), List.IMPLICIT); final Command back = new Command("Back", Command.BACK, 1); final Command run = new Command("Run", Command.OK, 2); final Command edit = new Command("Edit", Command.OK, 2); for (int i = 0; i < commandHistory.size(); i++) { historyList.append((String) commandHistory.elementAt(i), null); } historyList.addCommand(back); historyList.addCommand(run); historyList.addCommand(edit); historyList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { processCommand("xterm"); } else if (c == run) { int index = historyList.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); processCommand(historyList.getString(index)); } } else if (c == edit) { int index = historyList.getSelectedIndex(); if (index >= 0) { processCommand("xterm"); stdin.setString(historyList.getString(index)); } } } }); display.setCurrent(historyList); }
    
    
    private void nano(String file) { file = file == null || file.length() == 0 ? nanoContent : loadRMS(file, 1); final TextBox editor = new TextBox("Nano", file, 4096, TextField.ANY); final Command back = new Command("Back", Command.BACK, 1); final Command clear = new Command("Clear", Command.SCREEN, 2); final Command run = new Command("View as HTML", Command.SCREEN, 3); editor.addCommand(back); editor.addCommand(clear); editor.addCommand(run); editor.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { nanoContent = editor.getString(); processCommand("xterm"); } else if (c == clear) { editor.setString(""); } else if (c == run) { nanoContent = editor.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }); display.setCurrent(editor); }
    private void viewer(String title, String text) { Form viewer = new Form(env(title)); viewer.append(new StringItem(null, env(text))); viewer.addCommand(new Command("Back", Command.BACK, 1)); viewer.setCommandListener(this); display.setCurrent(viewer); }
    
    private void runScript(String script) { String[] commands = split(script, '\n'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
    
    
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.startsWith("#")) { } else if (line.length() != 0) { if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath); start = j; } } String dir = line.substring(start + 1); fullPath += "/" + dir; addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { if (!paths.containsKey(fullPath)) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/')); if (parentPath.length() == 0) { parentPath = "/"; } String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fullPath.substring(fullPath.lastIndexOf('/') + 1)); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } }
    private void ifCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('); int lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("if (expr) [command]"); return; } String expression = argument.substring(firstParenthesis + 1, lastParenthesis).trim(); String command = argument.substring(lastParenthesis + 1).trim(); String[] parts = split(expression, ' '); if (parts.length == 3) { if (parts[1].equals("startswith")) { if (parts[0].startsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("endswith")) { if (parts[0].endsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("!=")) { if (!parts[0].equals(parts[2])) { processCommand(command); } } else if (parts[1].equals("==")) { if (parts[0].equals(parts[2])) { processCommand(command); } } } else if (parts.length == 2) { if (parts[0].equals(parts[1])) { processCommand(command); } } else if (parts.length == 1) { if (!parts[0].equals("")) { processCommand(command); } } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private void deleteFile(String filename) { if (filename == null || filename.length() == 0) { return; } try { RecordStore.deleteRecordStore(filename); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); } }
    private void install(String filename) { if (filename == null || filename.length() == 0) { return; } writeRMS(filename, nanoContent); }
    private void explorer() { final List files = new List(form.getTitle(), List.IMPLICIT); final Command open = new Command("Open", Command.OK, 2); final Command delete = new Command("Delete", Command.OK, 3); final Command run = new Command("Run Script", Command.OK, 4); final Command importfile = new Command("Import File", Command.OK, 5); try { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { files.append((String) recordStores[i], null); } } } catch (RecordStoreException e) { } files.addCommand(new Command("Back", Command.BACK, 1)); files.addCommand(open); files.addCommand(delete); files.addCommand(run); files.addCommand(importfile); files.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c.getCommandType() == Command.BACK) { processCommand("xterm"); } else if (c == delete) { deleteFile(files.getString(files.getSelectedIndex())); explorer(); } else if (c == open) { nano(files.getString(files.getSelectedIndex())); } else if (c == run) { processCommand("xterm"); processCommand("run " + files.getString(files.getSelectedIndex())); } else if (c == importfile) { processCommand("xterm"); importScript(files.getString(files.getSelectedIndex())); } } }); display.setCurrent(files); }

    private void calendar() { final Form cal = new Form(form.getTitle()); cal.append(new DateField(null , DateField.DATE)); cal.addCommand(new Command("Back", Command.BACK, 1)); cal.setCommandListener(this); display.setCurrent(cal); }


    // MIDlet Services Command Processor 
    private void xserver(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2024-10-16\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); } 
        else if (mainCommand.equals("reset")) { form = new Form(""); display.setCurrent(form); }
        else if (mainCommand.equals("title")) { form.setTitle(argument); }
        else if (mainCommand.equals("term")) { display.setCurrent(form); }
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("canvas")) { display.setCurrent(new Canvas() { protected void paint(Graphics g) { g.setColor(255, 255, 255); g.fillRect(0, 0, getWidth(), getHeight()); } protected void keyPressed(int keyCode) { processCommand("xterm"); } }); }
        else if (mainCommand.equals("make")) { if (argument.equals("")) { echoCommand("x11: make: missing file"); } else { final Hashtable lib = parseFrom(argument); if (lib.containsKey("screen.title") && lib.containsKey("screen.button")) { final Form screen = new Form(env((String) lib.get("screen.title"))); final StringItem content = new StringItem("", lib.containsKey("screen.content") ? env((String) lib.get("screen.content")) : ""); final Command backCommand = new Command(lib.containsKey("screen.back.label") ? (String) lib.get("screen.back.label") : "Back", Command.OK, 1); final Command userCommand = new Command(env((String) lib.get("screen.button")), Command.SCREEN, 2); screen.append(content); screen.addCommand(backCommand); screen.addCommand(userCommand); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == backCommand) { processCommand("xterm"); processCommand(lib.containsKey("screen.back") ? env((String) lib.get("screen.back")) : "true"); } else if (c == userCommand) { processCommand("xterm"); if (lib.containsKey("screen.button.cmd")) { processCommand(env((String) lib.get("screen.button.cmd"))); } else { MIDletLogs("add warn An error occurred, 'screen.button.cmd' not found"); } } } }); display.setCurrent(screen); } else { MIDletLogs("add error Screen crashed while init, malformed settings"); } } }
        else if (mainCommand.equals("list")) { if (argument.equals("")) { echoCommand("x11: list: missing file"); } else { final Hashtable lib = parseFrom(argument); if (lib.containsKey("list.title") && lib.containsKey("list.content")) { final List screen = new List(env((String) lib.get("list.title")), List.IMPLICIT); final Command back = new Command(lib.containsKey("list.back.label") ? (String) lib.get("list.back.label") : "Back", Command.OK, 1); final Command run = new Command(lib.containsKey("list.button") ? (String) lib.get("list.button") : "Select", Command.SCREEN, 2); String[] content = split(env((String) lib.get("list.content")), ','); for (int i = 0; i < content.length; i++) { screen.append(content[i], null); } screen.addCommand(back); screen.addCommand(run); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { processCommand("xterm"); processCommand(lib.containsKey("list.back") ? env((String) lib.get("list.back")) : "true"); } else if (c == run) { int index = screen.getSelectedIndex(); if (index >= 0) { if (lib.containsKey(screen.getString(index))) { processCommand("xterm"); processCommand((String) lib.get(screen.getString(index))); } else { MIDletLogs("add warn An error occurred, '" + screen.getString(index) + "' not found"); } } } } }); display.setCurrent(screen); } else { MIDletLogs("add error List crashed while init, malformed settings"); } } }
        else if (mainCommand.equals("quest")) { if (argument.equals("")) { echoCommand("x11: quest: missing file"); } else { final Hashtable lib = parseFrom(argument); if (lib.containsKey("quest.title") && lib.containsKey("quest.label") && lib.containsKey("quest.cmd") && lib.containsKey("quest.key")) { final Form screen = new Form(env((String) lib.get("quest.title"))); final TextField name = new TextField(env((String) lib.get("quest.label")), "", 256, TextField.ANY); final Command save = new Command("Send", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { processCommand("xterm"); processCommand(lib.containsKey("quest.back") ? env((String) lib.get("quest.back")) : "true"); } else if (c == save) { if (!name.getString().trim().equals("")) { processCommand("set " + env((String) lib.get("quest.key")) + "=" + env(name.getString().trim())); processCommand("xterm"); processCommand(env((String) lib.get("quest.cmd"))); } } } }); display.setCurrent(screen); } else { MIDletLogs("add error Quest crashed while init, malformed settings"); } } }

        else { echoCommand("x11: " + mainCommand + ": not found"); }
    }
    private void MIDletLogs(String command) { command = env(command.trim()); String mainCommand = getCommand(command).toLowerCase(); String argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { writeRMS(argument.equals("") ? "logs" : argument, logs); } else if (mainCommand.equals("view")) { viewer(form.getTitle(), logs); } else if (mainCommand.equals("add")) { if (argument.equals("")) { return; } else if (getCommand(argument).toLowerCase().equals("info")) { if (!getArgument(command).equals("")) { logs = logs + "[INFO] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("warn")) { if (!getArgument(command).equals("")) { logs = logs + "[WARN] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("debug")) { if (!getArgument(command).equals("")) { logs = logs + "[DEBUG] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("error")) { if (!getArgument(command).equals("")) { logs = logs + "[ERROR] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else { echoCommand("log: add: " + getCommand(argument).toLowerCase() + ": level not found"); } } else { echoCommand("log: " + mainCommand + ": not found"); } }

    // Lib API Service
    private void importScript(String script) {
        if (script == null || script.length() == 0) { return; } 
        
        Hashtable lib = parseFrom(script);
        
        if (lib.containsKey("api.version")) { try { if (Integer.parseInt((String) lib.get("api.version")) > Integer.parseInt(env("$VERSION"))) { processCommand(lib.containsKey("api.error" ? (String) lib.get("api.error") : "true")); return; } catch (IOException e) { } }

        if (lib.containsKey("include")) { String[] include = split((String) lib.get("include"), ','); for (int i = 0; i < include.length; i++) { importScript(include[i]); } }
        
        if (lib.containsKey("config")) { processCommand((String) lib.get("config")); }
        if (lib.containsKey("mod")) { final String mod = (String) lib.get("mod"); new Thread(new Runnable() { public void run() { while (app) { processCommand(mod); } app = true; } }).start(); }
        
        if (lib.containsKey("command")) { String[] command = split((String) lib.get("command"), ','); for (int i = 0; i < command.length; i++) { if (lib.containsKey(command[i])) { aliases.put(command[i], env((String) lib.get(command[i]))); } else { MIDletLogs("add error failed to create command '" + command[i] + "' content not found"); } } }
        if (lib.containsKey("file")) { String[] file = split((String) lib.get("file"), ','); for (int i = 0; i < file.length; i++) { if (lib.containsKey(file[i])) { writeRMS(file[i], env((String) lib.get(file[i]))); } else { MIDletLogs("add error failed to create file '" + file[i] + "' content not found"); } } }
        
        if (lib.containsKey("shell.name") && lib.containsKey("shell.args")) { build(lib); }

    }
    private void about(String script) { if (script == null || script.length() == 0) { warnCommand("About", env("OpenTTY $VERSION\n(C) 2024 - Mr. Lima")); return; } Hashtable lib = parseFrom(script); if (lib.containsKey("name")) { echoCommand((String) lib.get("name") + " " + (String) lib.get("version")); } if (lib.containsKey("description")) { echoCommand((String) lib.get("description")); } }
    private void build(Hashtable lib) { String name = (String) lib.get("shell.name"); String[] args = split((String) lib.get("shell.args"), ','); Hashtable shellTable = new Hashtable(); for (int i = 0; i < args.length; i++) { String argName = args[i].trim(); String argValue = (String) lib.get(argName); shellTable.put(argName, (argValue != null) ? argValue : ""); } shell.put(name, shellTable); }


    // Login API Service
    private void login() { final Form login = new Form("Login"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); login.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2024 - Mr. Lima\n\nCreate an user to acess OpenTTY!")); login.append(userField); login.addCommand(new Command("Login", Command.OK, 1)); login.addCommand(new Command("Exit", Command.SCREEN, 2)); login.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c.getCommandType() == Command.OK) { username = userField.getString(); if (!username.equals("")) { writeRMS("OpenRMS", username); processCommand("xterm"); } } else if (c.getCommandType() == Command.SCREEN) { notifyDestroyed(); } } }); display.setCurrent(login); } 
    
    // Network API Service
    private void pingCommand(String url) { if (url == null || url.length() == 0) { return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } }
    private void runServer(final String port) { if (port == null || port.length() == 0 || port.equals("$PORT")) { processCommand("set PORT=31522"); runServer("31522"); return; } new Thread(new Runnable() { public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); while (true) { SocketConnection clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); InputStream is = clientSocket.openInputStream(); OutputStream os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[256]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); String response = env("$RESPONSE"); if (response.startsWith("/")) { os.write(read(response).getBytes()); } else if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(loadRMS(response, 1).getBytes()); } } } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'"); serverSocket.close(); } } }).start(); }
    private void query(String command) { command = env(command.trim()); String mainCommand = getCommand(command).toLowerCase(); String argument = getArgument(command); if (mainCommand.equals("")) { echoCommand("query: missing [host]"); return; } if (argument.equals("")) { echoCommand("query: missing [data]"); return; } try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + mainCommand); OutputStream outputStream = socket.openOutputStream(); outputStream.write((argument + "\n").getBytes()); outputStream.flush(); InputStream inputStream = socket.openInputStream(); byte[] buffer = new byte[2048]; int length = inputStream.read(buffer); if (length != -1) { String data = new String(buffer, 0, length); if (env("$QUERY").equals("$QUERY") || env("$QUERY").equals("")) { echoCommand(data); MIDletLogs("add warn Query storage setting not found"); } else if (env("$QUERY").toLowerCase().equals("show")) { echoCommand(data); } else if (env("$QUERY").toLowerCase().equals("nano")) { nanoContent = data; echoCommand("query: data retrived"); } else { writeRMS(env("$QUERY"), data); } } } catch (IOException e) { echoCommand(e.getMessage()); } }
    private void portScanner(final String host) { if (host == null || host.length() == 0) { return; } final List ports = new List(host + " Ports", List.IMPLICIT); ports.addCommand(new Command("Connect", Command.OK, 1)); ports.addCommand(new Command("Back", Command.BACK, 2)); ports.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c.getCommandType() == Command.OK) { new RemoteConnection(host + ":" + ports.getString(ports.getSelectedIndex())); } else if (c.getCommandType() == Command.BACK) { processCommand("xterm"); } } }); display.setCurrent(ports); new Thread(new Runnable() { public void run() { for (int port = 1; port <= 65535; port++) { try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port); ports.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } } }).start(); }


    public class RemoteConnection implements CommandListener {
        private SocketConnection socket; private InputStream inputStream; private OutputStream outputStream;

        private String host;

        private Form remote = new Form(form.getTitle());
        private TextField inputField = new TextField("Command", "", 256, TextField.ANY);
        private Command sendCommand = new Command("Send", Command.OK, 1);
        private Command backCommand = new Command("Back", Command.SCREEN, 2);
        private Command clearCommand = new Command("Clear", Command.SCREEN, 3);
        private Command infoCommand = new Command("Show info", Command.SCREEN, 4);
        private StringItem console = new StringItem("", "");

        public RemoteConnection(String args) {
            if (args == null || args.length() == 0) { return; } host = args;


            inputField.setLabel("Remote (" + split(args, ':')[0] + ")");
            remote.append(console); remote.append(inputField);
            remote.addCommand(backCommand); remote.addCommand(clearCommand); remote.addCommand(infoCommand); remote.addCommand(sendCommand);
            remote.setCommandListener(this);

            try { socket = (SocketConnection) Connector.open("socket://" + args); inputStream = socket.openInputStream(); outputStream = socket.openOutputStream(); } 
            catch (IOException e) { echoCommand(e.getMessage()); return; }
            
            display.setCurrent(remote);
            
        }
        public void commandAction(Command c, Displayable d) {
            if (c == sendCommand) { String command = inputField.getString().trim(); inputField.setString(""); send(command); } 
            else if (c == backCommand) { writeRMS("remote", console.getText()); processCommand("xterm"); } else if (c == clearCommand) { console.setText(""); }
            else if (c == infoCommand) { try { warnCommand("Informations",  "Host: " +  split(host, ':')[0] + "\nPort: " +  split(host, ':')[1] + "\n\nLocal Port: " + Integer.toString(socket.getLocalPort())); } catch (IOException e) { } }

        }
        private void send(String data) { try { outputStream.write((data + "\n").getBytes()); outputStream.flush(); new Thread(new Runnable() { public void run() { try { byte[] buffer = new byte[1024]; int length = inputStream.read(buffer); if (length != -1) { echoCommand(new String(buffer, 0, length), console); } } catch (IOException e) { processCommand("warn " + e.getMessage()); } } }).start(); } catch (IOException e) { processCommand("warn " + e.getMessage()); } }
        
    }


    public class GetAddress { public GetAddress(String args) { if (args.equals("")) { processCommand("ifconfig"); } else { String result = performNSLookup(args); echoCommand(result); } } private String performNSLookup(String domain) { try { DatagramConnection conn = (DatagramConnection) Connector.open("datagram://1.1.1.1:53"); byte[] query = createDNSQuery(domain); Datagram request = conn.newDatagram(query, query.length); conn.send(request); Datagram response = conn.newDatagram(512); conn.receive(response); conn.close(); return parseDNSResponse(response.getData()); } catch (IOException e) { return e.getMessage(); } } private byte[] createDNSQuery(String domain) throws IOException { ByteArrayOutputStream out = new ByteArrayOutputStream(); out.write(0x12); out.write(0x34); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); out.write(0x00); String[] parts = split(domain, '.'); for (int i = 0; i < parts.length; i++) { out.write(parts[i].length()); out.write(parts[i].getBytes()); } out.write(0x00); out.write(0x00); out.write(0x01); out.write(0x00); out.write(0x01); return out.toByteArray(); } private String parseDNSResponse(byte[] response) { if ((response[3] & 0x0F) != 0) { return "DNS response error"; } int answerOffset = 12; while (response[answerOffset] != 0) { answerOffset++; } answerOffset += 5; if (response[answerOffset + 2] == 0x00 && response[answerOffset + 3] == 0x01) { StringBuffer ip = new StringBuffer(); for (int i = answerOffset + 12; i < answerOffset + 16; i++) { ip.append(response[i] & 0xFF); if (i < answerOffset + 15) ip.append("."); } return ip.toString(); } else { return "not found"; } } }
    public class GoBuster implements CommandListener { private String fullUrl, url; private String[] wordlist; private List pages; private Command openCommand, saveCommand, backCommand; public GoBuster(String args) { if (args == null || args.length() == 0) { return; } this.url = args; pages = new List("GoBuster (" + url + ")", List.IMPLICIT); wordlist = split(loadRMS("gobuster", 1), '\n'); if (wordlist == null || wordlist.length == 0) { wordlist = split(read("/java/etc/gobuster"), '\n'); } openCommand = new Command("Get Request", Command.OK, 1); saveCommand = new Command("Save Result", Command.OK, 1); backCommand = new Command("Back", Command.BACK, 1); pages.addCommand(openCommand); pages.addCommand(saveCommand); pages.addCommand(backCommand); pages.setCommandListener(this); new Thread(new Runnable() { public void run() { for (int i = 0; i < wordlist.length; i++) { if (!wordlist[i].startsWith("#") && !wordlist[i].equals("")) { String fullUrl = url.startsWith("http://") || url.startsWith("https://") ? url + "/" + wordlist[i] : "http://" + url + "/" + wordlist[i]; try { if (GoVerify(fullUrl)) { pages.append("/" + wordlist[i], null); } } catch (IOException e) {  } } } } }).start(); display.setCurrent(pages); } private boolean GoVerify(String fullUrl) throws IOException { HttpConnection conn = null; InputStream is = null; try { conn = (HttpConnection) Connector.open(fullUrl); conn.setRequestMethod(HttpConnection.GET); int responseCode = conn.getResponseCode(); return (responseCode == HttpConnection.HTTP_OK); } finally { if (is != null) { is.close(); } if (conn != null) { conn.close(); } } } private String GoSave(List pages) { StringBuffer sb = new StringBuffer(); for (int i = 0; i < pages.size(); i++) { sb.append(pages.getString(i)); if (i < pages.size() - 1) { sb.append("\n"); } } return replace(sb.toString(), "/", ""); } public void commandAction(Command c, Displayable d) { if (c == openCommand) { processCommand("bg execute wget " + url + pages.getString(pages.getSelectedIndex()) + "; nano;"); } else if (c == saveCommand && pages.size() != 0) { nanoContent = GoSave(pages); nano(""); } else if (c == backCommand) { processCommand("xterm"); } } }

}

public class FileExplorer implements CommandListener { private String currentPath = "file:///"; private Display display; private List files; private Form form; private Command openCommand, backCommand; public FileExplorer(Display display, Form form) { this.display = display; this.form = form; files = new List(form.getTitle(), List.IMPLICIT); openCommand = new Command("Open", Command.OK, 1); backCommand = new Command("Back", Command.BACK, 1); files.addCommand(openCommand); files.addCommand(backCommand); files.setCommandListener(this); display.setCurrent(files); listFiles(currentPath); } private void listFiles(String path) { files.deleteAll(); try { if (path.equals("file:///")) { Enumeration roots = FileSystemRegistry.listRoots(); while (roots.hasMoreElements()) { files.append((String) roots.nextElement(), null); } } else { FileConnection dir = (FileConnection) Connector.open(path, Connector.READ); Enumeration fileList = dir.list(); Vector dirs = new Vector(); Vector filesOnly = new Vector(); while (fileList.hasMoreElements()) { String fileName = (String) fileList.nextElement(); if (fileName.endsWith("/")) { dirs.addElement(fileName); } else { filesOnly.addElement(fileName); } } while (!dirs.isEmpty()) { files.append(getFirstString(dirs), null); } while (!filesOnly.isEmpty()) { files.append(getFirstString(filesOnly), null); } dir.close(); } } catch (IOException e) { } } private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } } public void commandAction(Command c, Displayable d) { if (c == openCommand) { int selectedIndex = files.getSelectedIndex(); if (selectedIndex >= 0) { String selected = files.getString(selectedIndex); String newPath = currentPath + selected; if (selected.endsWith("/")) { currentPath = newPath; listFiles(newPath); } else { writeRMS(selected, read(newPath)); Alert alert = new Alert(null, "File '" + selected + "' successfully saved!", null, AlertType.INFO); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); } } } else if (c == backCommand) { if (!currentPath.equals("file:///")) { int lastSlash = currentPath.lastIndexOf('/', currentPath.length() - 2); if (lastSlash != -1) { currentPath = currentPath.substring(0, lastSlash + 1); listFiles(currentPath); } } else { display.setCurrent(form); } } } private static String getFirstString(Vector v) { String result = null; for (int i = 0; i < v.size(); i++) { String cur = (String) v.elementAt(i); if (result == null || cur.compareTo(cur) < 0) { result = cur; } } v.removeElement(result); return result; } private String read(String file) { try { FileConnection fileConn = (FileConnection) Connector.open(file, Connector.READ); InputStream is = fileConn.openInputStream(); StringBuffer content = new StringBuffer(); int ch; while ((ch = is.read()) != -1) { content.append((char) ch); } is.close(); fileConn.close(); return content.toString(); } catch (IOException e) { return ""; } } }


