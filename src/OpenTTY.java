import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;


public class OpenTTY extends MIDlet implements CommandListener {
    private boolean app;
    private int currentIndex = 0;
    private String logs = "";
    private String path = "/";
    private Hashtable paths = new Hashtable();
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
            mount(read("/java/bin/fstab")); stdin.setLabel(username + " " + path + " $"); app = true;
            
            attributes.put("PATCH", "OpenTTY Revolution"); attributes.put("VERSION", getAppProperty("MIDlet-Version")); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.5");
            attributes.put("TTY", "/java/optty1"); attributes.put("HOSTNAME", "localhost"); attributes.put("PORT", "31522"); attributes.put("RESPONSE", "com.opentty.server"); attributes.put("QUERY", "nano");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));
            attributes.put("OUTPUT", "");  
        
            form.append(stdout); form.append(stdin);
            form.addCommand(enterCommand); form.addCommand(helpCommand); 
            form.addCommand(nanoCommand); form.addCommand(clearCommand);  
            form.addCommand(historyCommand);
            form.setCommandListener(this);
            
            if (username.equals("")) { login(); }
            else { display.setCurrent(form); processCommand("run initd"); }
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
        
        else if (c.getCommandType() == Command.BACK) { display.setCurrent(form); }
        else if (c.getCommandType() == Command.EXIT) { processCommand("exit"); }
    }
    
    
    // OpenTTY Command Processor
    private void processCommand(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (aliases.containsKey(mainCommand)) { processCommand((String) aliases.get(mainCommand) + " " + argument); return; }
        
        if (mainCommand.equals("")) { }
        
        // Network Utilities
        else if (mainCommand.equals("netstat")) { netstat(); }
        else if (mainCommand.equals("nc")) { connect(argument); }
        else if (mainCommand.equals("query")) { query(argument); }
        else if (mainCommand.equals("fw")) { fwCommand(argument); }
        else if (mainCommand.equals("ping")) { pingCommand(argument); }
        else if (mainCommand.equals("curl")) { curlCommand(argument); } 
        else if (mainCommand.equals("wget")) { wgetCommand(argument); } 
        else if (mainCommand.equals("prscan")) { portScanner(argument); }
        else if (mainCommand.equals("server")) { runServer(env("$PORT")); }
        else if (mainCommand.equals("ifconfig")) { try { SocketConnection socketConnection = (SocketConnection) Connector.open("socket://8.8.8.8:53"); echoCommand(socketConnection.getLocalAddress()); socketConnection.close(); } catch (IOException e) { try { SocketConnection socketConnection = (SocketConnection) Connector.open("socket://1.1.1.1:53"); echoCommand(socketConnection.getLocalAddress()); socketConnection.close(); } catch (IOException e) { echoCommand("null"); } } }
        
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
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { echoCommand(read(argument)); } else { echoCommand(loadRMS(argument, 1)); } } }
        else if (mainCommand.equals("get")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { nanoContent = read(argument); } else { nanoContent = loadRMS(argument, 1); } } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { writeRMS(getArgument(argument).equals("") ? getCommand(argument) + "-copy" : getArgument(argument), loadRMS(getCommand(argument), 1)); } }
        else if (mainCommand.equals("cd")) { if (argument.equals("")) { path = "/"; } else { if (argument.startsWith("/")) { if (paths.containsKey(argument)) { path = argument; } else { echoCommand("cd: " + basename(argument) + ": not found"); } } else if (argument.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { processCommand(path.equals("/") ? "cd " + "/" + argument : "cd " + path + "/" + argument); } } }
        else if (mainCommand.equals("ph2s")) { StringBuffer script = new StringBuffer(); for (int i = 0; i < commandHistory.size() - 1; i++) { script.append(commandHistory.elementAt(i)); if (i < commandHistory.size() - 1) { script.append("\n"); } } if (argument.equals("") || argument.equals("nano")) { nanoContent = "#!/java/bin/sh\n\n" + script.toString(); } else { writeRMS(argument, "#!/java/bin/sh\n\n" + script.toString()); } }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { mount(read(argument)); } else if (argument.equals("nano")) { mount(nanoContent); } else { mount(loadRMS(argument, 1)); } } }
        else if (mainCommand.equals("dir")) { if (argument.equals("f")) { explorer(); } else { String[] files = (String[]) paths.get(path); for (int i = 0; i < files.length; i++) { if (!files[i].equals("..")) { echoCommand(files[i].trim()); } } } }
        else if (mainCommand.equals("df")) { 
            try {
                String[] recordStores = RecordStore.listRecordStores();
                int totalRecordStores = (recordStores != null) ? recordStores.length : 0;
                echoCommand("Total RMS Files: " + totalRecordStores + "\n");

                int totalSize = 0;
                for (int i = 0; i < totalRecordStores; i++) {
                    String rs = recordStores[i];
                    int size = rs.length();
                    totalSize += size;
                    echoCommand("RMS: " + rs + " | Size: " + size + " B\n");
                }

                echoCommand("Storage Usage: " + totalSize / 1024 + " KB");
            } catch (RecordStoreException e) { }
        }
        

        // General 
        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("buff")) { stdin.setString(argument); }
        else if (mainCommand.equals("break")) { app = false; }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
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
        else if (mainCommand.equals("set")) { setCommand(argument); }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { processCommand("import /java/bin/sh"); }
        else if (mainCommand.equals("true") || mainCommand.equals("false") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("time")) { echoCommand(split(new java.util.Date().toString(), ' ')[3]); }
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(stdout.getText().length() + " KB"); }
        else if (mainCommand.equals("title")) { form.setTitle(argument.equals("") ? env("OpenTTY $VERSION") : argument); }
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        else if (mainCommand.equals("uname")) { echoCommand(env("$TYPE $CONFIG $PROFILE")); }
        else if (mainCommand.equals("unset")) { unsetCommand(argument); }
        else if (mainCommand.equals("vendor")) { echoCommand(getAppProperty("MIDlet-Vendor")); }
        else if (mainCommand.equals("version")) { echoCommand(env("OpenTTY $VERSION")); }
        else if (mainCommand.equals("whoami") || mainCommand.equals("logname")) { echoCommand(username); } 
        else if (mainCommand.equals("warn")) { warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("x11")) { xserver(argument); }
        
        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { importScript(argument); }


        else if (mainCommand.equals("!")) { echoCommand(env("main/$RELEASE"));  }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { runScript(read(argument)); } else { runScript(read(path + "/" + argument)); } } }
        
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
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); for (Enumeration e = attributes.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } return text; }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    
    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }
    
    private void aliasCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (aliases.containsKey(argument)) { echoCommand("alias " + argument + "='" + (String) aliases.get(argument) + "'"); } else { Enumeration keys = aliases.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) aliases.get(key); echoCommand("alias " + key + "='" + value.trim() + "'"); } } return; } String aliasName = argument.substring(0, equalsIndex).trim(); String aliasCommand = argument.substring(equalsIndex + 1).trim(); aliases.put(aliasName, aliasCommand); }
    private void unaliasCommand(String aliasName) { if (aliasName == null || aliasName.length() == 0) { echoCommand("unalias: missing [alias]"); return; } if (aliases.containsKey(aliasName)) { aliases.remove(aliasName); } else { echoCommand("unalias: " + aliasName + ": not found"); } }
    private void setCommand(String argument) { int equalsIndex = argument.indexOf('='); if (equalsIndex == -1) { if (attributes.containsKey(argument)) { echoCommand(argument + "=" + (String) attributes.get(argument)); } else { /* Exportation File */ } return; } String key = argument.substring(0, equalsIndex).trim(); String value = argument.substring(equalsIndex + 1).trim(); attributes.put(key, value); }
    private void unsetCommand(String key) { if (key == null || key.length() == 0) { return; } if (attributes.containsKey(key)) { attributes.remove(key); } }
    
    private void echoCommand(String message) { stdout.setText(stdout.getText().equals("") ? message : stdout.getText() + "\n" + message); attributes.put("OUTPUT", message); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, form); }
    private void lockCommand() { final Form lock = new Form(form.getTitle() + " - Locked"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); final Command unlock = new Command("Unlock", Command.OK, 1); final Command exitCommand = new Command("Exit", Command.SCREEN, 2); lock.append(userField); lock.addCommand(unlock); lock.addCommand(exitCommand); lock.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == unlock) { if (userField.getString().equals(username)) { display.setCurrent(form); } else if (!userField.getString().equals("")) { Alert alert = new Alert(form.getTitle(), "Wrong username!", null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, lock); } } else if (c == exitCommand) { processCommand("exit"); } } } ); display.setCurrent(lock); }
    private void htopCommand() { Runtime runtime = Runtime.getRuntime(); viewer(form.getTitle(), "Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\nFree Memory: " + runtime.freeMemory() / 1024 + " KB\nTotal Memory: " + runtime.totalMemory() / 1024 + " KB"); }
    
    private void showHistory() { final List historyList = new List(form.getTitle(), List.IMPLICIT); final Command back = new Command("Back", Command.BACK, 1); final Command run = new Command("Run", Command.OK, 2); final Command edit = new Command("Edit", Command.OK, 2); for (int i = 0; i < commandHistory.size(); i++) { historyList.append((String) commandHistory.elementAt(i), null); } historyList.addCommand(back); historyList.addCommand(run); historyList.addCommand(edit); historyList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == run) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); processCommand(historyList.getString(index)); } } else if (c == edit) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); stdin.setString(historyList.getString(index)); } } } }); display.setCurrent(historyList); }
    
    
    private void nano(String file) { file = file == null || file.length() == 0 ? nanoContent : loadRMS(file, 1); final TextBox editor = new TextBox("Nano", file, 4096, TextField.ANY); final Command back = new Command("Back", Command.BACK, 1); final Command clear = new Command("Clear", Command.SCREEN, 2); final Command run = new Command("View as HTML", Command.SCREEN, 3); editor.addCommand(back); editor.addCommand(clear); editor.addCommand(run); editor.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { nanoContent = editor.getString(); display.setCurrent(form); } else if (c == clear) { editor.setString(""); } else if (c == run) { nanoContent = editor.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }); display.setCurrent(editor); }
    private void viewer(String title, String text) { Form viewer = new Form(env(title)); StringItem contentItem = new StringItem(null, env(text)); viewer.append(contentItem); viewer.addCommand(new Command("Back", Command.BACK, 1)); viewer.setCommandListener(this); display.setCurrent(viewer); }
    
    private void runScript(String script) { String[] commands = split(script, '\n'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
    
    
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.startsWith("#")) { } else if (line.length() != 0) { if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath); start = j; } } String dir = line.substring(start + 1); fullPath += "/" + dir; addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { if (!paths.containsKey(fullPath)) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/')); if (parentPath.length() == 0) { parentPath = "/"; } String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fullPath.substring(fullPath.lastIndexOf('/') + 1)); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } }
    private void ifCommand(String argument) { argument = argument.trim(); int firstParenthesis = argument.indexOf('('); int lastParenthesis = argument.indexOf(')'); if (firstParenthesis == -1 || lastParenthesis == -1 || firstParenthesis > lastParenthesis) { echoCommand("Usage: if (expr) [command]"); return; } String expression = argument.substring(firstParenthesis + 1, lastParenthesis).trim(); String command = argument.substring(lastParenthesis + 1).trim(); String[] parts = split(expression, ' '); if (parts.length == 3) { if (parts[1].equals("startswith")) { if (parts[0].startsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("endswith")) { if (parts[0].endsWith(parts[2])) { processCommand(command); } } else if (parts[1].equals("!=")) { if (!parts[0].equals(parts[2])) { processCommand(command); } } else if (parts[1].equals("==")) { if (parts[0].equals(parts[2])) { processCommand(command); } } } else if (parts.length == 2) { if (parts[0].equals(parts[1])) { processCommand(command); } } else if (parts.length == 1) { if (!parts[0].equals("")) { processCommand(command); } } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private void deleteFile(String filename) { if (filename == null || filename.length() == 0) { echoCommand("rm: missing [file]"); return; } try { RecordStore.deleteRecordStore(filename); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); } }
    private void install(String filename) { if (filename == null || filename.length() == 0) { } else { writeRMS(filename, nanoContent); } }
    private void explorer() { final List files = new List(form.getTitle(), List.IMPLICIT); final Command back = new Command("Back", Command.BACK, 1); final Command run = new Command("Open", Command.OK, 2); final Command delete = new Command("Delete", Command.OK, 3); try { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { files.append((String) recordStores[i], null); } } } catch (RecordStoreException e) { } files.addCommand(back); files.addCommand(run); files.addCommand(delete); files.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == run) { int index = files.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); processCommand("nano " + files.getString(index)); } } else if (c == delete) { int index = files.getSelectedIndex(); if (index >= 0) { processCommand("rm " + files.getString(index)); explorer(); } } } }); display.setCurrent(files); }
    
    // MIDlet Services Command Processor 
    private void xserver(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2024-10-15\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); } 
        else if (mainCommand.equals("reset")) { form = new Form(""); display.setCurrent(form); }
        else if (mainCommand.equals("title")) { form.setTitle(argument); }
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("make")) { if (argument.equals("")) { echoCommand("x11: make: missing file"); } else { final Hashtable lib; if (argument.startsWith("/")) { lib = parseProperties(read(argument)); } else if (argument.equals("nano")) { lib = parseProperties(nanoContent); } else { lib = parseProperties(loadRMS(argument, 1)); } if (lib.containsKey("screen.title") && lib.containsKey("screen.button")) { final Form screen = new Form(env((String) lib.get("screen.title"))); final StringItem content = new StringItem("", "Screen Content"); final Command backCommand = new Command("Back", Command.OK, 1); final Command userCommand = new Command(env((String) lib.get("screen.button")), Command.SCREEN, 2); screen.append(content); screen.addCommand(backCommand); screen.addCommand(userCommand); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == backCommand) { display.setCurrent(form); if (lib.containsKey("screen.back")) { processCommand(env((String) lib.get("screen.back"))); } } else if (c == userCommand) { display.setCurrent(form); if (lib.containsKey("screen.button.cmd")) { processCommand(env((String) lib.get("screen.button.cmd"))); } else { MIDletLogs("add warn an error was ocurred, screen.button command not found"); } } } }); content.setText(lib.containsKey("screen.content") ? env((String) lib.get("screen.content")) : ""); display.setCurrent(screen); } else { MIDletLogs("add error screen crashed while init, malformed settings"); } } }
        else if (mainCommand.equals("list")) { if (argument.equals("")) { echoCommand("x11: list: missing file"); } else { final Hashtable lib; if (argument.startsWith("/")) { lib = parseProperties(read(argument)); } else if (argument.equals("nano")) { lib = parseProperties(nanoContent); } else { lib = parseProperties(loadRMS(argument, 1)); } if (lib.containsKey("list.title") && lib.containsKey("list.content")) { final List screen = new List(env((String) lib.get("list.title")), List.IMPLICIT); final Command back = new Command("Back", Command.OK, 1); final Command run = new Command("Select", Command.SCREEN, 2); String[] content = split(env((String) lib.get("list.content")), ','); for (int i = 0; i < content.length; i++) { screen.append(content[i], null); } screen.addCommand(back); screen.addCommand(run); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == run) { int index = screen.getSelectedIndex(); if (index >= 0) { if (lib.containsKey(screen.getString(index))) { display.setCurrent(form); processCommand((String) lib.get(screen.getString(index))); } else { MIDletLogs("add warn an error ocurred, " + screen.getString(index) + " item not found"); } } } } }); display.setCurrent(screen); } else { MIDletLogs("add error list crashed while init, malformed settings"); } } }
        else if (mainCommand.equals("quest")) { if (argument.equals("")) { echoCommand("x11: quest: missing file"); } else { final Hashtable lib; if (argument.startsWith("/")) { lib = parseProperties(read(argument)); } else if (argument.equals("nano")) { lib = parseProperties(nanoContent); } else { lib = parseProperties(loadRMS(argument, 1)); } if (lib.containsKey("quest.title") && lib.containsKey("quest.label") && lib.containsKey("quest.cmd") && lib.containsKey("quest.key")) { final Form screen = new Form(env((String) lib.get("quest.title"))); final TextField name = new TextField(env((String) lib.get("quest.label")), "", 256, TextField.ANY); final Command save = new Command("Send", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { app = false; display.setCurrent(form); app = true; } else if (c == save) { if (name.getString().trim().equals("")) { } else { processCommand("set " + env((String) lib.get("quest.key")) + "=" + env(name.getString().trim())); display.setCurrent(form); processCommand(env((String) lib.get("quest.cmd"))); } } } } ); display.setCurrent(screen); } else { MIDletLogs("add error quest crashed while init, malformed settings"); } } }
        
        else { echoCommand("x11: " + mainCommand + ": not found"); }
    }
    private void MIDletLogs(String command) { command = env(command.trim()); String mainCommand = getCommand(command).toLowerCase(); String argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { writeRMS(argument.equals("") ? "logs" : argument, logs); } else if (mainCommand.equals("view")) { viewer(form.getTitle(), logs); } else if (mainCommand.equals("add")) { if (argument.equals("")) { return; } else if (getCommand(argument).toLowerCase().equals("info")) { if (!getArgument(command).equals("")) { logs = logs + "[INFO] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("warn")) { if (!getArgument(command).equals("")) { logs = logs + "[WARN] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("debug")) { if (!getArgument(command).equals("")) { logs = logs + "[DEBUG] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else if (getCommand(argument).toLowerCase().equals("error")) { if (!getArgument(command).equals("")) { logs = logs + "[ERROR] " + split(new java.util.Date().toString(), ' ')[3] + " " + getArgument(argument) + "\n"; } } else { echoCommand("log: add: " + getCommand(argument).toLowerCase() + ": level not found"); } } else { echoCommand("log: " + mainCommand + ": not found"); } }

    
    // Lib API Service
    private void importScript(String script) {
        if (script == null || script.length() == 0) { return; } if (script.startsWith("/")) { script = read(script); } else if (script.equals("nano")) { script = nanoContent; } else { script = loadRMS(script, 1); }
        
        Hashtable lib = parseProperties(script);
        
        if (lib.containsKey("include")) { String[] include = split((String) lib.get("include"), ','); for (int i = 0; i < include.length; i++) { importScript(include[i]); } }
        
        if (lib.containsKey("config")) { processCommand((String) lib.get("config")); }
        if (lib.containsKey("mod")) { final String mod = (String) lib.get("mod"); new Thread(new Runnable() { public void run() { while (app) { processCommand(mod); } app = true; } }).start(); }
        
        if (lib.containsKey("command")) { String[] command = split((String) lib.get("command"), ','); for (int i = 0; i < command.length; i++) { if (lib.containsKey(command[i])) { aliases.put(command[i], env((String) lib.get(command[i]))); } else { MIDletLogs("add error failed to create command '" + command[i] + "' content not found"); } } }
        if (lib.containsKey("file")) { String[] file = split((String) lib.get("file"), ','); for (int i = 0; i < file.length; i++) { if (lib.containsKey(file[i])) { writeRMS(file[i], env((String) lib.get(file[i]))); } else { MIDletLogs("add error failed to create file '" + file[i] + "' content not found"); } } }
       
    }
    private void about(String script) { if (script == null || script.length() == 0) { warnCommand("About", env("OpenTTY $VERSION\n(C) 2024 - Mr. Lima")); return; } if (script.startsWith("/")) { script = read(script); } else if (script.equals("nano")) { script = nanoContent; } else { script = loadRMS(script, 1); } Hashtable lib = parseProperties(script); if (lib.containsKey("name")) { echoCommand((String) lib.get("name") + " " + (String) lib.get("version")); } if (lib.containsKey("description")) { echoCommand((String) lib.get("description")); } }
    
    // Login API Service
    private void login() { final Form login = new Form("Login"); final Command loginCommand = new Command("Login", Command.OK, 1); final Command exitCommand = new Command("Exit", Command.EXIT, 2); final StringItem text = new StringItem("", env("Welcome to OpenTTY $VERSION\nCopyright (C) 2024 - Mr. Lima\n\nCreate an user to acess OpenTTY!")); final TextField userField = new TextField("Username", "", 256, TextField.ANY); login.append(text); login.append(userField); login.addCommand(loginCommand); login.addCommand(exitCommand); login.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == loginCommand) { username = userField.getString(); if (!username.equals("")) { writeRMS("OpenRMS", username); display.setCurrent(form); } } else if (c == exitCommand) { notifyDestroyed(); } } } ); display.setCurrent(login); } 
    
    // Network API Service
    private void netstat() { new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open("http://ipinfo.io/ip"); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } echoCommand("true"); conn.close(); } catch (Exception e) { echoCommand("false"); } } }).start(); }
    private void fwCommand(final String ip) { final String url = "http://ipinfo.io/" + (ip == null || ip.length() == 0 ? "json" : ip); new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand(e.getMessage()); } } }).start(); }
    private void curlCommand(final String url) { if (url == null || url.length() == 0) { return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } echoCommand(new String(baos.toByteArray())); is.close(); conn.close(); } catch (Exception e) { echoCommand(e.getMessage()); } } }).start(); }
    private void wgetCommand(final String url) { if (url == null || url.length() == 0) { return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } nanoContent = new String(baos.toByteArray()); echoCommand("wget: html saved to nano"); is.close(); conn.close(); } catch (Exception e) { echoCommand(e.getMessage()); } } }).start(); }
    private void pingCommand(final String url) { if (url == null || url.length() == 0) { return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); is.close(); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } } }).start(); }
    private void connect(final String host) { if (host == null || host.length() == 0) { return; } try { final SocketConnection socket = (SocketConnection) Connector.open("socket://" + host); final InputStream inputStream = socket.openInputStream(); final OutputStream outputStream = socket.openOutputStream(); final Form remote = new Form(form.getTitle()); final TextField inputField = new TextField("Remote (" + split(host, ':')[0] + ")", "", 256, TextField.ANY); final Command sendCommand = new Command("Send", Command.OK, 1); final Command backCommand = new Command("Back", Command.SCREEN, 2); final Command clearCommand = new Command("Clear", Command.SCREEN, 3); final StringItem console = new StringItem("", ""); remote.append(console); remote.append(inputField); remote.addCommand(backCommand); remote.addCommand(clearCommand); remote.addCommand(sendCommand); remote.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == sendCommand) { final String data = inputField.getString() + "\n"; inputField.setString(""); try { outputStream.write(data.getBytes()); outputStream.flush(); new Thread(new Runnable() { public void run() { try { byte[] buffer = new byte[1024]; int length = inputStream.read(buffer); if (length != -1) { String response = new String(buffer, 0, length); console.setText(console.getText().equals("") ? response.trim() : console.getText() + "\n" + response.trim()); } } catch (IOException e) { processCommand("warn " + e.getMessage()); } } }).start(); } catch (IOException e) { processCommand("warn "  + e.getMessage()); } } else if (c == backCommand) { writeRMS("console", console.getText()); display.setCurrent(form); } else if (c == clearCommand) { console.setText(""); } } }); display.setCurrent(remote); } catch (IOException e) { processCommand("warn " + e.getMessage()); } }
    private void runServer(final String port) { if (port == null || port.length() == 0 || port.equals("$PORT")) { processCommand("set PORT=31522"); runServer("31522"); return; } new Thread(new Runnable() { public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); MIDletLogs("add info Server listening at port " + port); while (true) { SocketConnection clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); InputStream is = clientSocket.openInputStream(); OutputStream os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[256]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); String response = env("$RESPONSE"); if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(response.getBytes()); } } } catch (IOException e) { echoCommand("[-] " + e.getMessage()); MIDletLogs("add error Server crashed '" + e.getMessage() + "'"); serverSocket.close(); } } }).start(); }
    private void query(String command) { command = env(command.trim()); String mainCommand = getCommand(command).toLowerCase(); String argument = getArgument(command); if (mainCommand.equals("")) { echoCommand("query: missing [host]"); return; } if (argument.equals("")) { echoCommand("query: missing [data]"); return; } try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + mainCommand); OutputStream outputStream = socket.openOutputStream(); outputStream.write((argument + "\n").getBytes()); outputStream.flush(); InputStream inputStream = socket.openInputStream(); byte[] buffer = new byte[2048]; int length = inputStream.read(buffer); if (length != -1) { String data = new String(buffer, 0, length); if (env("$QUERY").equals("$QUERY") || env("$QUERY").equals("")) { echoCommand(data); MIDletLogs("add warn Query storage setting not found"); } else if (env("$QUERY").toLowerCase().equals("show")) { echoCommand(data); } else if (env("$QUERY").toLowerCase().equals("nano")) { nanoContent = data; echoCommand("query: data retrived"); } else { writeRMS(env("$QUERY"), data); } } } catch (IOException e) { echoCommand(e.getMessage()); } }
    private void portScanner(final String host) { if (host == null || host.length() == 0) { return; } final List openPortsList = new List(host + " Ports", List.IMPLICIT); final Command connectCommand = new Command("Connect", Command.OK, 1); final Command backCommand = new Command("Back", Command.BACK, 2); openPortsList.addCommand(connectCommand); openPortsList.addCommand(backCommand); openPortsList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == connectCommand) { int index = openPortsList.getSelectedIndex(); String selectedPort = openPortsList.getString(index); connect(host + ":" + selectedPort); } else if (c == backCommand) { display.setCurrent(form); } } }); new Thread(new Runnable() { public void run() { display.setCurrent(openPortsList); for (int port = 1; port <= 99999; port++) { try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port); openPortsList.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } } }).start(); }
    
}
