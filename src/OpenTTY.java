import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;


public class OpenTTY extends MIDlet implements CommandListener {
    private boolean app;
    private int currentIndex = 0;
    private String path = "/";
    private String version = "1.8.2";
    private Hashtable paths = new Hashtable();
    private Hashtable aliases = new Hashtable();
    private Vector commandHistory = new Vector();
    private Hashtable attributes = new Hashtable();
    private String username = loadRMS("OpenRMS", 1);
    private String nanoContent = loadRMS("nano", 1);
    private Display display = Display.getDisplay(this);
    private Form form = new Form("OpenTTY " + version);
    private Command enterCommand = new Command("Send", Command.OK, 1);
    private Command helpCommand = new Command("Help", Command.SCREEN, 2);
    private Command nanoCommand = new Command("Nano", Command.SCREEN, 3);
    private Command clearCommand = new Command("Clear", Command.SCREEN, 4);
    private Command historyCommand = new Command("History", Command.SCREEN, 5);
    private TextField commandInput = new TextField("Command", "", 256, TextField.ANY);
    private StringItem output = new StringItem("", "Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n");
    
    public void startApp() {
        if (!app == true) {
            mount(); commandInput.setLabel(username + " " + path + " $"); 
            
            attributes.put("PATCH", "Netman Update"); attributes.put("VERSION", version); attributes.put("RELEASE", "mod"); attributes.put("XVERSION", "0.4");
            attributes.put("TTY", "/java/optty1"); attributes.put("HOSTNAME", "localhost"); attributes.put("PORT", "4095"); attributes.put("RESPONSE", "com.opentty.server");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));
            attributes.put("OUTPUT", ""); attributes.put("l", "\n"); 
            
            form.append(output);
            form.append(commandInput);
            form.addCommand(enterCommand); form.addCommand(helpCommand); 
            form.addCommand(nanoCommand); form.addCommand(clearCommand);  
            form.addCommand(historyCommand);
            form.setCommandListener(this);
            
            display.setCurrent(form); processCommand("run initd");
        }    
    }

    public void pauseApp() { app = true; }
    public void destroyApp(boolean unconditional) { writeRMS("nano", nanoContent); }

    public void commandAction(Command c, Displayable d) {
        if (c == enterCommand) { String command = commandInput.getString().trim(); if (!command.equals("")) { commandHistory.addElement(command); } commandInput.setString(""); processCommand(command); commandInput.setLabel(username + " " + path + " $"); }
        
        else if (c == clearCommand) { output.setText(""); }
        else if (c == helpCommand) { processCommand("help"); } 
        else if (c == historyCommand) { showHistory(); }
        else if (c == nanoCommand) { nano(); }
        
    }
    
    // OpenTTY Command Processor
    private void processCommand(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (aliases.containsKey(mainCommand)) { processCommand((String) aliases.get(mainCommand) + argument); return; }
        
        if (mainCommand.equals("")) { }
        
        // Network Utilities
        else if (mainCommand.equals("netstat")) { netstat(); }
        else if (mainCommand.equals("nc")) { connect(argument); }
        else if (mainCommand.equals("fw")) { fwCommand(argument); }
        else if (mainCommand.equals("ipconfig")) { fwCommand("ip"); }
        else if (mainCommand.equals("ping")) { pingCommand(argument); }
        else if (mainCommand.equals("curl")) { curlCommand(argument); } 
        else if (mainCommand.equals("wget")) { wgetCommand(argument); } 
        else if (mainCommand.equals("prscan")) { portScanner(argument); }
        else if (mainCommand.equals("server")) { runServer(env("$PORT")); }
        
        // File Utilities
        else if (mainCommand.equals("nano")) { nano(); }
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("rnano")) { nanoContent = ""; }
        else if (mainCommand.equals("cd")) { changeDisk(argument); }
        else if (mainCommand.equals("install")) { install(argument); }
        else if (mainCommand.equals("load")) { loadCommand(argument); }
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("rraw")) { output.setText(nanoContent); }
        else if (mainCommand.equals("getty")) { nanoContent = output.getText(); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(nanoContent)); }
        else if (mainCommand.equals("pjnc")) { nanoContent = parseJson(nanoContent); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent + "\n" + argument; }
        else if (mainCommand.equals("ls")) { viewer("Resources", read("/java/resources.txt")); }
        else if (mainCommand.equals("html")) { viewer(extractTitle(nanoContent), html2text(nanoContent)); }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { echoCommand("Usage: cat <file>"); } else { if (argument.startsWith("/")) { echoCommand(read(argument)); } else { echoCommand(read(path + "/" + argument)); } } }
        else if (mainCommand.equals("get")) { if (argument.equals("")) { echoCommand("Usage: get <file>"); } else { if (argument.startsWith("/")) { nanoContent = read(argument); } else { nanoContent = read(path + "/" + argument); } } }
        
        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
        else if (mainCommand.equals("call")) { callCommand(argument); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { output.setText(""); } 
        else if (mainCommand.equals("date")) { echoCommand(new java.util.Date().toString()); } 
        else if (mainCommand.equals("debug")) { runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("exit")) { writeRMS("nano", nanoContent); notifyDestroyed(); }
        else if (mainCommand.equals("export")) { if (argument.equals("")) { echoCommand("Usage: export <name>"); } else { attributes.put(argument, ""); } }
        else if (mainCommand.equals("execute")) { processCommand(argument); }
        else if (mainCommand.equals("forget")) { commandHistory = new Vector(); }
        else if (mainCommand.equals("hostname")) { echoCommand(env("$HOSTNAME")); } 
        else if (mainCommand.equals("htop")) { htopCommand(); }
        else if (mainCommand.equals("help")) { viewer("OpenTTY Help", read("/java/help.txt")); }
        else if (mainCommand.equals("history")) { showHistory(); }
        else if (mainCommand.equals("if")) { ifCommand(argument); }
        else if (mainCommand.equals("login")) { login(argument); }
        else if (mainCommand.equals("logout")) { if (username.equals("")) { processCommand("exit"); } else { username = ""; writeRMS("OpenRMS", ""); } }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("lock")) { lockCommand(); }
        else if (mainCommand.equals("open")) { openCommand(argument); }
        else if (mainCommand.equals("run")) { if (argument.equals("")) { runScript(nanoContent); } else { runScript(loadRMS(argument, 1)); } }
        else if (mainCommand.equals("set")) { setCommand(argument); }
        else if (mainCommand.equals("sh")) { form.setTitle(env("OpenTTY $VERSION")); output.setText(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2024 - Mr. Lima\n")); path = "/"; }
        else if (mainCommand.equals("true") || mainCommand.equals("false")) { }
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(output.getText().length() + " KB"); }
        else if (mainCommand.equals("title")) { if (argument.equals("") ) { form.setTitle(env("OpenTTY $VERSION")); } else { form.setTitle(argument); } }
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        else if (mainCommand.equals("uname")) { echoCommand(env("$TYPE $CONFIG $PROFILE")); }
        else if (mainCommand.equals("unset")) { unsetCommand(argument); }
        else if (mainCommand.equals("version")) { echoCommand("OpenTTY " + version); }
        else if (mainCommand.equals("whoami")) { if (username.equals("")) { echoCommand("whoami: not logged"); } else { echoCommand(username); } } 
        else if (mainCommand.equals("warn")) { warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("xorg")) { if (argument.length() == 0 || argument.equals("help")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2024-07-25\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); } else if (argument.equals("stop")) { form = new Form(""); display.setCurrent(form); } else { echoCommand("xorg: " + argument + ": not found"); } } 
            
        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE LTS\nIn memory of Silvio Santos"));  }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { runScript(read(argument)); } else { runScript(read(path + "/" + argument)); } } }
        
        else if (mainCommand.equals("silvio")) { if (env("$LOCALE").equals("pt-BR")) { viewer("Silvio Santos", read("/java/silvio-pt.txt")); } else { viewer("Silvio Santos", read("/java/silvio-en.txt")); } }

        else { echoCommand(mainCommand + ": not found"); }
        
    }
    
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    private String extractTitle(String htmlContent) { int titleStart = htmlContent.indexOf("<title>"); int titleEnd = htmlContent.indexOf("</title>"); if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) { return htmlContent.substring(titleStart + 7, titleEnd).trim(); } return "HTML Viewer"; }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false; boolean inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inTitle) { text.append(c); } } return text.toString().trim(); }
    private String parseJson(String text) { Hashtable properties = parseProperties(text); Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\n  \"").append(key).append("\": "); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("\n}"); return jsonBuffer.toString(); }
    private String loadRMS(String recordStoreName, int recordId) { RecordStore recordStore = null; String result = ""; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); if (recordStore.getNumRecords() >= recordId) { byte[] data = recordStore.getRecord(recordId); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { result = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return result; }
    private String read(String filename) { try { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } catch (IOException e) { return e.getMessage(); } }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0; int end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); for (Enumeration e = attributes.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } return text; }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    
    private String[] splitLines(String content) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == '\n') { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = splitLines(text); for (int i = 0; i < lines.length; i++) { String line = lines[i]; int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } return properties; }
    
    private void aliasCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: alias <name> <command>"); return; } String aliasName = argument.substring(0, spaceIndex).trim(); String aliasCommand = argument.substring(spaceIndex + 1).trim(); aliases.put(aliasName, aliasCommand); }
    private void unaliasCommand(String aliasName) { if (aliasName == null || aliasName.length() == 0) { echoCommand("Usage: unalias <alias>"); return; } if (aliases.containsKey(aliasName)) { aliases.remove(aliasName); } else { echoCommand("unalias: " + aliasName + ": not found"); } }
    private void setCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: set <key> <value>"); return; } attributes.put(argument.substring(0, spaceIndex).trim(), argument.substring(spaceIndex + 1).trim()); }
    private void unsetCommand(String key) { if (key == null || key.length() == 0) { echoCommand("Usage: unset <key>"); return; } if (attributes.containsKey(key)) { attributes.remove(key); } else { echoCommand("unalias: " + key + ": not found"); } }
    
    private void echoCommand(String message) { output.setText(output.getText() + "\n" + message); attributes.put("OUTPUT", message); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Phone", "", 16, TextField.NUMERIC); final Command save = new Command("Call", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { callCommand(name.getString().trim()); display.setCurrent(form); } } } } ); display.setCurrent(screen); return; } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("API Url", "", 256, TextField.ANY);  final Command save = new Command("Open", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { openCommand(name.getString().trim()); display.setCurrent(form); } } } } ); display.setCurrent(screen); return; } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { echoCommand("Usage: warn <message>"); return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, form); }
    private void lockCommand() { if (username == null || username.length() == 0) { echoCommand("lock: not logged"); return; } final Form lock = new Form(form.getTitle() + " - Locked"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); final StringItem text = new StringItem("", "OpenTTY was blocked! Insert your\nusername to return to console."); final Command unlock = new Command("Unlock", Command.OK, 1); lock.append(text); lock.append(userField); lock.addCommand(unlock); lock.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == unlock) { if (userField.getString().equals(username)) { display.setCurrent(form); } else if (!userField.getString().equals("")) { Alert alert = new Alert(form.getTitle(), "Wrong username!", null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, lock); } } } } ); display.setCurrent(lock); }
    private void htopCommand() { Runtime runtime = Runtime.getRuntime(); viewer(form.getTitle(), "Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\nFree Memory: " + runtime.freeMemory() / 1024 + " KB\nTotal Memory: " + runtime.totalMemory() / 1024 + " KB"); }
    
    private void showHistory() { final List historyList = new List("OpenTTY History", List.IMPLICIT); final Command back = new Command("Back", Command.BACK, 1); final Command run = new Command("Run", Command.OK, 2); final Command edit = new Command("Edit", Command.OK, 2); for (int i = 0; i < commandHistory.size(); i++) { historyList.append((String) commandHistory.elementAt(i), null); } historyList.addCommand(back); historyList.addCommand(run); historyList.addCommand(edit); historyList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == run) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); processCommand(historyList.getString(index)); } } else if (c == edit) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); commandInput.setString(historyList.getString(index)); } } } }); display.setCurrent(historyList); }
    
    
    private void nano() { final TextBox editor = new TextBox("Nano", nanoContent, 4096, TextField.ANY); final Command back = new Command("Back", Command.OK, 1); final Command clear = new Command("Clear", Command.SCREEN, 2); final Command run = new Command("View as HTML", Command.SCREEN, 3); editor.addCommand(back); editor.addCommand(clear); editor.addCommand(run); editor.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { nanoContent = editor.getString(); display.setCurrent(form); } else if (c == clear) { editor.setString(""); } else if (c == run) { nanoContent = editor.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }); display.setCurrent(editor); }
    private void viewer(String title, String text) { Form viewer = new Form(title); StringItem contentItem = new StringItem(null, text); final Command back = new Command("Back", Command.OK, 1); viewer.addCommand(back); viewer.append(contentItem); viewer.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } } }); display.setCurrent(viewer); }
    
    private void runScript(String script) { String[] commands = splitLines(script); for (int i = 0; i < commands.length; i++) { String cmd = env(commands[i].trim()); if (!cmd.startsWith("#")) { processCommand(cmd); } } }
    
    
    private void mount() {
        paths.put("/", new String[] { "java", "scripts" });
        paths.put("/java", new String[] { "bin", "sounds", ".." });
        paths.put("/java/bin", new String[] { ".." });
        paths.put("/java/sounds", new String[] { ".." });
        paths.put("/scripts", new String[] { ".." });
    }    
    
    private void changeDisk(String way) { if (way == null || way.length() == 0) { echoCommand("Usage: cd <dir>"); return; } String[] availablePaths = (String[]) paths.get(path); if (availablePaths != null) { boolean pathFound = false; for (int i = 0; i < availablePaths.length; i++) { if (availablePaths[i].equals(way)) { pathFound = true; break; } } if (pathFound) { if (way.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { path = path.equals("/") ? "/" + way : path + "/" + way; } } else { if (way.equals("/") || way.equals(".") || way.equals("..")) { path = "/"; return; } echoCommand("cd: " + way + ": not found"); } } else { echoCommand("cd: " + way + ": not found"); } }
    private void ifCommand(String argument) { int firstSpaceIndex = argument.indexOf(' '); int secondSpaceIndex = argument.indexOf(' ', firstSpaceIndex + 1); if (firstSpaceIndex == -1) { echoCommand("Usage: if <x> <y> [command] "); return; } if (secondSpaceIndex == -1) { processCommand("warn java.io.IOException: missing operators"); return; } String value1 = argument.substring(0, firstSpaceIndex).trim(); String value2 = argument.substring(firstSpaceIndex + 1, secondSpaceIndex).trim(); String command = argument.substring(secondSpaceIndex + 1).trim(); if (value1.equals(value2)) { processCommand(command); } if (value1.equals("not") && value2.equals("")) { processCommand(command); } if (value1.equals("is") && !value2.equals("") ) { processCommand(command); } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private void install(String filename) { if (filename == null || filename.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Filename", "", 16, TextField.ANY);  final Command save = new Command("Save", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { writeRMS(name.getString().trim(), nanoContent); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { writeRMS(filename, nanoContent); } }
    private void loadCommand(String filename) { if (filename == null || filename.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Filename", "", 16, TextField.ANY); final Command save = new Command("Load", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { nanoContent = loadRMS(name.getString().trim(), 1); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { nanoContent = loadRMS(filename, 1); } }
    
    // Login API Service
    private void login(String user) { if (user == null || user.length() == 0 || !username.equals("")) { processCommand("sh"); return; } username = user; writeRMS("OpenRMS", user); } 
    
    // Network API Service
    private void netstat() { new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open("http://www.google.com"); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); conn.getResponseCode(); echoCommand("[+] Network available"); conn.close(); } catch (IOException e) { echoCommand("[-] Network unavailable"); } } }).start(); }
    private void fwCommand(final String ip) { if (ip == null || ip.length() == 0) { ip = "json"; } final String url = "http://ipinfo.io/" + ip; new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("fw: " + e.getMessage()); } } }).start(); }
    private void curlCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: curl <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("curl: " + e.getMessage()); } } }).start(); }
    private void wgetCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: wget <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } nanoContent = new String(baos.toByteArray()); echoCommand("wget: html saved to nano"); is.close(); conn.close(); } catch (Exception e) { echoCommand("wget: " + e.getMessage()); } } }).start(); }
    private void pingCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: ping <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); is.close(); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } } }).start(); }
    private void connect(final String host) { if (host == null || host.length() == 0) { echoCommand("Usage: nc <ip:port>"); return; } try { final SocketConnection socket = (SocketConnection) Connector.open("socket://" + host); final InputStream inputStream = socket.openInputStream(); final OutputStream outputStream = socket.openOutputStream(); final Form remote = new Form(form.getTitle()); final TextField inputField = new TextField("Command", "", 256, TextField.ANY); final Command sendCommand = new Command("Send", Command.OK, 1); final Command backCommand = new Command("Back", Command.SCREEN, 2); final Command clearCommand = new Command("Clear", Command.SCREEN, 3); final StringItem console = new StringItem("", ""); remote.append(console); remote.append(inputField); remote.addCommand(backCommand); remote.addCommand(clearCommand); remote.addCommand(sendCommand); remote.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == sendCommand) { final String data = inputField.getString(); inputField.setString(""); try { outputStream.write(data.getBytes() + "\n"); outputStream.flush(); new Thread(new Runnable() { public void run() { try { byte[] buffer = new byte[1024]; int length = inputStream.read(buffer); if (length != -1) { String response = new String(buffer, 0, length); console.setText(console.getText() + "\n" + response.trim()); } } catch (IOException e) { processCommand("warn " + e.getMessage()); } } } ).start(); } catch (IOException e) { processCommand("warn "  + e.getMessage()); } } else if (c == backCommand) { display.setCurrent(form); } else if (c == clearCommand) { console.setText(""); } } }); display.setCurrent(remote); } catch (IOException e) { processCommand("warn " + e.getMessage()); } }
    private void runServer(final String port) { if (port == null || port.length() == 0 || port.equals("$PORT")) { processCommand("set PORT 4095"); runServer("4095"); return; } new Thread(new Runnable() { public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); while (true) { SocketConnection clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); InputStream is = clientSocket.openInputStream(); OutputStream os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[256]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); String response = env("$RESPONSE"); if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(response.getBytes()); } } } catch (IOException e) { echoCommand("[-] " + e.getMessage()); serverSocket.close(); } } }).start(); }
    private void portScanner(final String host) { if (host == null || host.length() == 0) { echoCommand("Usage: prscan <ip>"); return; } final List openPortsList = new List(host + " Ports", List.IMPLICIT); final Command connectCommand = new Command("Connect", Command.OK, 1); final Command backCommand = new Command("Back", Command.BACK, 2); openPortsList.addCommand(connectCommand); openPortsList.addCommand(backCommand); openPortsList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == connectCommand) { int index = openPortsList.getSelectedIndex(); String selectedPort = openPortsList.getString(index); connect(host + ":" + selectedPort); } else if (c == backCommand) { display.setCurrent(form); } } }); new Thread(new Runnable() { public void run() { display.setCurrent(openPortsList); for (int port = 1; port <= 9999; port++) { try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port); openPortsList.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } } }).start(); }
    
    
}