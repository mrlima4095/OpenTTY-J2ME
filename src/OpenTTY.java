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
    private String version = "1.6.2";
    private String xversion = "0.4";
    private String hostname = "unknown";
    private String tty = "/java/optty1";
    private RecordStore userStore = null;
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
            
            attributes.put("PATCH", "FS Update"); attributes.put("VERSION", version); attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("TYPE", System.getProperty("microedition.platform")); 
            attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("XVERSION", xversion); 
            attributes.put("LOCALE", System.getProperty("microedition.locale"));
            
            form.append(output);
            form.append(commandInput);
            form.addCommand(enterCommand); form.addCommand(helpCommand); 
            form.addCommand(nanoCommand); form.addCommand(clearCommand);  
            form.addCommand(historyCommand);
            form.setCommandListener(this);
            
            display.setCurrent(form);
        }    
    }

    public void pauseApp() { app = true; }
    public void destroyApp(boolean unconditional) { writeRMS("nano", nanoContent); }

    public void commandAction(Command c, Displayable d) {
        if (c == enterCommand) { String command = commandInput.getString(); if (!command.equals("")) { commandHistory.addElement(commandInput.getString()); } processCommand(command); commandInput.setLabel(username + " " + path + " $"); attributes.put("PATH", path); commandInput.setString(""); }
        
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
        
        if (aliases.containsKey(mainCommand)) { processCommand((String) aliases.get(mainCommand) + argument); }
        
        if (mainCommand.equals("")) { }
        else if (mainCommand.equals("!")) { echoCommand("OpenTTY Java Edition");  }
        else if (mainCommand.equals("nano")) { nano(); }
        else if (mainCommand.equals("netstat")) { netstat(); }
        else if (mainCommand.equals("date")) { dateCommand(); } 
        else if (mainCommand.equals("lock")) { lockCommand(); }
        else if (mainCommand.equals("htop")) { htopCommand(); }
        else if (mainCommand.equals("tty")) { echoCommand(tty); }
        else if (mainCommand.equals("history")) { showHistory(); }
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("login")) { login(argument); }
        else if (mainCommand.equals("if")) { ifCommand(argument); }
        else if (mainCommand.equals("rnano")) { nanoContent = ""; }
        else if (mainCommand.equals("fw")) { fwCommand(argument); }
        else if (mainCommand.equals("cd")) { changeDisk(argument); }
        else if (mainCommand.equals("ipconfig")) { fwCommand("ip"); }
        else if (mainCommand.equals("set")) { setCommand(argument); }
        else if (mainCommand.equals("install")) { install(argument); }
        else if (mainCommand.equals("load")) { loadCommand(argument); }
        else if (mainCommand.equals("ping")) { pingCommand(argument); }
        else if (mainCommand.equals("curl")) { curlCommand(argument); } 
        else if (mainCommand.equals("wget")) { wgetCommand(argument); } 
        else if (mainCommand.equals("call")) { callCommand(argument); }
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("open")) { openCommand(argument); }
        else if (mainCommand.equals("unset")) { unsetCommand(argument); }
        else if (mainCommand.equals("uname")) { unameCommand(argument); }
        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("hostname")) { echoCommand(hostname); } 
        else if (mainCommand.equals("rraw")) { output.setText(nanoContent); }
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        else if (mainCommand.equals("execute")) { processCommand(argument); }
        else if (mainCommand.equals("true") || mainCommand.equals("false")) { }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("forget")) { commandHistory = new Vector(); }
        else if (mainCommand.equals("getty")) { nanoContent = output.getText(); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(nanoContent)); }
        else if (mainCommand.equals("version")) { echoCommand("OpenTTY " + version); }
        else if (mainCommand.equals("debug")) { runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("warn")) { warnCommand("OpenTTY " + version, argument); }
        else if (mainCommand.equals("html")) { viewer("HTML Viewer", html2text(nanoContent)); }
        else if (mainCommand.equals("help")) { viewer("OpenTTY Help", read("/java/help.txt")); }
        else if (mainCommand.equals("ls")) { viewer("Resources", read("/java/resources.txt")); }
        else if (mainCommand.equals("exit")) { writeRMS("nano", nanoContent); notifyDestroyed(); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { output.setText(""); } 
        else if (mainCommand.equals("ttysize")) { echoCommand(output.getText().length() + " KB"); }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { echoCommand(". /<file>.sh"); } else { runScript(read(argument)); } }
        else if (mainCommand.equals("run")) { if (argument.equals("")) { runScript(nanoContent); } else { runScript(loadRMS(argument, 1)); } }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { echoCommand("Usage: cat <file>"); } else { echoCommand(read(argument)); } }
        else if (mainCommand.equals("get")) { if (argument.equals("")) { echoCommand("Usage: get <file>"); } else { nanoContent = read(argument); } }
        else if (mainCommand.equals("title")) { if (argument.equals("") ) { form.setTitle("OpenTTY " + version); } else { form.setTitle(argument); } }
        else if (mainCommand.equals("whoami")) { if (username.equals("")) { echoCommand("whoami: not logged"); } else { echoCommand(username + "@" + hostname); } } 
        else if (mainCommand.equals("logout")) { if (username.equals("")) { echoCommand("logout: not logged"); } else { echoCommand("[ OpenTTY ] logout disabled"); processCommand("warn You cant charge your username! Read more at Github project Wiki. ");} }
        else if (mainCommand.equals("xorg")) { { if (argument.length() == 0 || argument.equals("help")) { viewer("OpenTTY X.Org", "OpenTTY X.Org - X Server " + xversion + "\nRelease Date: 2024-07-25\nX Protocol Version 1, Revision 3\nBuild OS: " + System.getProperty("microedition.platform")); } else if (argument.equals("stop")) { display.setCurrent(new Form("")); } } }
        else if (mainCommand.equals("sh")) { form.setTitle("OpenTTY " + version); output.setText("Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n"); path = "/"; }
        else { echoCommand(mainCommand + ": unknown command"); }
        
    }
    
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; } else if (c == '>') { inTag = false; } else if (!inTag) { text.append(c); } } return text.toString(); } 
    private String parseJson(String text) { Hashtable properties = parseProperties(text); Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\"").append(key).append("\":"); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("}"); return jsonBuffer.toString(); }
    private String loadRMS(String recordStoreName, int recordId) { RecordStore recordStore = null; String result = ""; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); if (recordStore.getNumRecords() >= recordId) { byte[] data = recordStore.getRecord(recordId); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { result = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return result; }
    private String read(String filename) { try { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } catch (IOException e) { return e.getMessage(); } }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0; int end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { for (Enumeration e = attributes.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); return text; }
    
    private String[] splitLines(String content) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == '\n') { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = splitLines(text); for (int i = 0; i < lines.length; i++) { String line = lines[i]; int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } return properties; }
    
    private void aliasCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: alias <name> <command>"); return; } String aliasName = argument.substring(0, spaceIndex).trim(); String aliasCommand = argument.substring(spaceIndex + 1).trim(); aliases.put(aliasName, aliasCommand); }
    private void unaliasCommand(String aliasName) { if (aliasName == null || aliasName.length() == 0) { echoCommand("Usage: unalias <alias>"); return; } if (aliases.containsKey(aliasName)) { aliases.remove(aliasName); } else { echoCommand("unalias: " + aliasName + ": not found"); } }
    
    private void setCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: set <name> <value>"); return; } attributes.put(argument.substring(0, spaceIndex).trim(), argument.substring(spaceIndex + 1).trim()); }
    private void unsetCommand(String key) { if (key == null || key.length() == 0) { echoCommand("Usage: unset <key>"); return; } if (attributes.containsKey(key)) { attributes.remove(key); } else { echoCommand("unalias: " + key + ": not found"); } }
    
    private void dateCommand() { echoCommand(new java.util.Date().toString()); }
    private void unameCommand(String options) { echoCommand(env("$TYPE $CONFIG $PROFILE")); }
    private void echoCommand(String message) { output.setText(output.getText() + "\n" + message); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { echoCommand("Usage: call <phone>"); return; } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { echoCommand("Usage: open <url>"); return; } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { echoCommand("Usage: warn <message>"); return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, form); }
    private void lockCommand() { if (username == null || username.length() == 0) { echoCommand("lock: not logged"); return; } final Form lock = new Form("OpenTTY " + version + " - Locked"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); final StringItem text = new StringItem("", "OpenTTY was blocked! Insert your\nusername to return to console."); final Command unlock = new Command("Unlock", Command.OK, 1); lock.append(text); lock.append(userField); lock.addCommand(unlock); lock.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == unlock) { if (userField.getString().equals(username)) { display.setCurrent(form); } else if (!userField.getString().equals("")) { Alert alert = new Alert("Error", "Wrong username!", null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, lock); } } } } ); display.setCurrent(lock); }
    private void htopCommand() { Runtime runtime = Runtime.getRuntime(); echoCommand("Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB"); echoCommand("Free Memory: " + runtime.freeMemory() / 1024 + " KB"); echoCommand("Total Memory: " + runtime.totalMemory() / 1024 + " KB"); }
    
    private void showHistory() { final List historyList = new List("OpenTTY History", List.IMPLICIT); final Command back = new Command("Back", Command.BACK, 1); final Command run = new Command("Run", Command.OK, 2); for (int i = 0; i < commandHistory.size(); i++) { historyList.append((String) commandHistory.elementAt(i), null); } historyList.addCommand(back); historyList.addCommand(run); historyList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == run) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); processCommand(historyList.getString(index)); } } } }); display.setCurrent(historyList); }
    
    
    private void nano() { final TextBox editor = new TextBox("Nano", nanoContent, 4096, TextField.ANY); final Command back = new Command("Back", Command.OK, 1); final Command clear = new Command("Clear", Command.SCREEN, 2); final Command run = new Command("View as HTML", Command.SCREEN, 3); editor.addCommand(back); editor.addCommand(clear); editor.addCommand(run); editor.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { nanoContent = editor.getString(); display.setCurrent(form); } else if (c == clear) { editor.setString(""); } else if (c == run) { nanoContent = editor.getString(); viewer("HTML Viewer", html2text(nanoContent)); } } }); display.setCurrent(editor); }
    private void viewer(String title, String text) { Form viewer = new Form(title); StringItem contentItem = new StringItem(null, text); final Command back = new Command("Back", Command.OK, 1); viewer.addCommand(back); viewer.append(contentItem); viewer.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } } }); display.setCurrent(viewer); }
    
    private void runScript(String script) { String[] commands = splitLines(env(script)); for (int i = 0; i < commands.length; i++) { String cmd = commands[i].trim(); if (!cmd.startsWith("#")) { processCommand(cmd); } } }
   
    
    private void mount() {
        paths.put("/", new String[] { "java", "scripts" });
        paths.put("/java", new String[] { "bin", "sounds", ".." });
        paths.put("/java/bin", new String[] { ".." });
        paths.put("/java/sounds", new String[] { ".." });
        paths.put("/scripts", new String[] { ".." });
    }    
    
    private void changeDisk(String way) { if (way == null || way.length() == 0) { echoCommand("Usage: cd <dir>"); return; } String[] availablePaths = (String[]) paths.get(path); if (availablePaths != null) { boolean pathFound = false; for (int i = 0; i < availablePaths.length; i++) { if (availablePaths[i].equals(way)) { pathFound = true; break; } } if (pathFound) { if (way.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { path = path.equals("/") ? "/" + way : path + "/" + way; } } else { if (way.equals("/") || way.equals(".") || way.equals("..")) { path = "/"; return; } echoCommand("cd: " + way + ": not found"); } } else { echoCommand("cd: " + way + ": not found"); } }
    private void ifCommand(String argument) { int firstSpaceIndex = argument.indexOf(' '); int secondSpaceIndex = argument.indexOf(' ', firstSpaceIndex + 1); if (firstSpaceIndex == -1) { echoCommand("Usage: if <x> <y> [command] "); return; } if (secondSpaceIndex == -1) { processCommand("warn java.io.IOException: missing operators"); return; } String value1 = argument.substring(0, firstSpaceIndex).trim(); String value2 = argument.substring(firstSpaceIndex + 1, secondSpaceIndex).trim(); String command = argument.substring(secondSpaceIndex + 1).trim(); if (value1.equals(value2)) { processCommand(command); } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private void install(String filename) { if (filename == null || filename.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Filename", "", 16, TextField.ANY);  final Command save = new Command("Save", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().equals("")) { processCommand("warn java.io.IOException: Insert a valid filename."); } else { writeRMS(name.getString().trim(), nanoContent); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { writeRMS(filename, nanoContent); } }
    private void loadCommand(String filename) { if (filename == null || filename.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Filename", "", 16, TextField.ANY); final Command save = new Command("Load", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().equals("")) { processCommand("warn java.io.IOException: Insert a valid filename."); } else { nanoContent = loadRMS(name.getString().trim(), 1); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { nanoContent = loadRMS(filename, 1); } }
    
    // Login API Service
    private void login(String user) { if (user == null || user.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Username", "", 16, TextField.ANY); final Command save = new Command("Login", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().equals("")) { processCommand("warn java.io.IOException: Insert a valid username."); } else { processCommand("login " + name.getString().trim()); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { username = user; writeRMS("OpenRMS", user); } }
    
    // Network API Service
    private void netstat() { new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open("http://www.google.com"); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); conn.getResponseCode(); echoCommand("Network available"); conn.close(); } catch (IOException e) { echoCommand("Network unavailable"); } } }).start(); }
    private void fwCommand(final String ip) { if (ip == null || ip.length() == 0) { ip = "json"; } final String url = "http://ipinfo.io/" + ip; new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("fw: " + e.getMessage()); } } }).start(); }
    private void curlCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: curl <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("curl: " + e.getMessage()); } } }).start(); }
    private void wgetCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: wget <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } nanoContent = new String(baos.toByteArray()); echoCommand("wget: html saved to nano"); is.close(); conn.close(); } catch (Exception e) { echoCommand("wget: " + e.getMessage()); } } }).start(); }
    private void pingCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: ping <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); is.close(); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } } }).start(); }
    
}