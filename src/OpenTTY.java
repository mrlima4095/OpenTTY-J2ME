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
    private String version = "1.8.6";
    private Hashtable paths = new Hashtable();
    private Hashtable shell = new Hashtable();
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
            mount(read("/java/bin/fstab")); commandInput.setLabel(username + " " + path + " $"); 
            
            attributes.put("PATCH", "Netman Update"); attributes.put("VERSION", version); attributes.put("RELEASE", "stable"); attributes.put("XVERSION", "0.5");
            attributes.put("TTY", "/java/optty1"); attributes.put("HOSTNAME", "localhost"); attributes.put("PORT", "4095"); attributes.put("RESPONSE", "com.opentty.server");
            attributes.put("TYPE", System.getProperty("microedition.platform")); attributes.put("CONFIG", System.getProperty("microedition.configuration")); attributes.put("PROFILE", System.getProperty("microedition.profiles")); attributes.put("LOCALE", System.getProperty("microedition.locale"));
            attributes.put("OUTPUT", "");  
            
            form.append(output);
            form.append(commandInput);
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
        if (c == enterCommand) { String command = commandInput.getString().trim(); if (!command.equals("")) { commandHistory.addElement(command.trim()); } commandInput.setString(""); processCommand(command); commandInput.setLabel(username + " " + path + " $"); } 
            
        else if (c == clearCommand) { output.setText(""); }
        else if (c == helpCommand) { processCommand("help"); } 
        else if (c == historyCommand) { showHistory(); }
        else if (c == nanoCommand) { nano(""); }
        
    }
    
    
    // OpenTTY Command Processor
    private void processCommand(String command) { processCommand(command, true); }
    private void processCommand(String command, boolean ignore) {
        command = env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (shell.containsKey(mainCommand) && ignore) { Hashtable args = (Hashtable) shell.get(mainCommand); if (argument.equals("")) { if (aliases.containsKey(mainCommand)) { processCommand((String) aliases.get(mainCommand)); } } else if (args.containsKey(getCommand(argument).toLowerCase())) { processCommand((String) args.get(getCommand(argument)) + " " + getArgument(argument)); } else { echoCommand(mainCommand + ": " + getCommand(argument) + ": not found"); } return; }
        if (aliases.containsKey(mainCommand) && ignore) { processCommand((String) aliases.get(mainCommand) + " " + argument); return; }
        
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
        else if (mainCommand.equals("ifconfig")) { ifconfig(argument); }
        
        // File Utilities
        else if (mainCommand.equals("nano")) { nano(argument); }
        else if (mainCommand.equals("pwd")) { echoCommand(path); }
        else if (mainCommand.equals("rnano")) { nanoContent = ""; }
        else if (mainCommand.equals("cd")) { changeDisk(argument); }
        else if (mainCommand.equals("install")) { if (argument.equals("")) { } else { writeRMS(argument, nanoContent); } }
        else if (mainCommand.equals("load")) { loadCommand(argument); }
        else if (mainCommand.equals("raw")) { echoCommand(nanoContent); }
        else if (mainCommand.equals("rraw")) { output.setText(nanoContent); }
        else if (mainCommand.equals("getty")) { nanoContent = output.getText(); }
        else if (mainCommand.equals("json")) { echoCommand(parseJson(nanoContent)); }
        else if (mainCommand.equals("pjnc")) { nanoContent = parseJson(nanoContent); }
        else if (mainCommand.equals("ls")) { viewer("Resources", read("/java/resources.txt")); }
        else if (mainCommand.equals("html")) { viewer(extractTitle(env(nanoContent)), html2text(env(nanoContent))); }
        else if (mainCommand.equals("add")) { nanoContent = nanoContent.equals("") ? argument + "\n" : nanoContent + "\n" + argument; }
        else if (mainCommand.equals("cat")) { if (argument.equals("")) { echoCommand("Usage: cat <file>"); } else { if (argument.startsWith("/")) { echoCommand(read(argument)); } else { echoCommand(read(path + "/" + argument)); } } }
        else if (mainCommand.equals("get")) { if (argument.equals("")) { echoCommand("Usage: get <file>"); } else { if (argument.startsWith("/")) { nanoContent = read(argument); } else { nanoContent = read(path + "/" + argument); } } }
        else if (mainCommand.equals("mount")) { if (argument.equals("")) { echoCommand("Usage: mount <drive>"); } else { if (argument.startsWith("/")) { mount(read(argument)); } else if (argument.equals("nano")) { mount(nanoContent); } else { mount(loadRMS(argument, 1)); } } }
        else if (mainCommand.equals("cp")) { if (argument.equals("")) { echoCommand("cp: missing [origin]"); } else { writeRMS(getArgument(argument).equals("") ? getCommand(argument) + "-copy" : getArgument(argument), loadRMS(getCommand(argument), 1)); } }
        else if (mainCommand.equals("du")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { echoCommand(basename(argument) + " (Size: " + read(argument).length() + " B)"); } else if (argument.equals("nano")) { echoCommand(argument + " (Size: " + nanoContent.length() + " B)"); } else { echoCommand(argument + " (Size: " + loadRMS(argument, 1).length() + " B)"); } } }
        else if (mainCommand.equals("ph2s")) { StringBuffer script = new StringBuffer(); for (int i = 0; i < commandHistory.size() - 1; i++) { script.append(commandHistory.elementAt(i)); if (i < commandHistory.size() - 1) { script.append("\n"); } } if (argument.equals("") || argument.equals("nano")) { nanoContent = "#!/java/bin/sh\n\n" + script.toString(); } else { writeRMS(argument, "#!/java/bin/sh\n\n" + script.toString()); } }
        else if (mainCommand.equals("cd")) { if (argument.equals("")) { path = "/"; } else { if (argument.startsWith("/")) { if (paths.containsKey(argument)) { path = argument; } else { echoCommand("cd: " + basename(argument) + ": not found"); } } else if (argument.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { processCommand(path.equals("/") ? "cd " + "/" + argument : "cd " + path + "/" + argument); } } }
        else if (mainCommand.equals("find")) {if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); String file; if (args[1].startsWith("/")) { file = read(args[1]); } else if (args[1].equals("nano")) { file = nanoContent; } else { file = loadRMS(args[1], 1); } String value = (String) parseProperties(file).get(args[0]); echoCommand(value != null ? value : "null"); } }
        else if (mainCommand.equals("grep")) {if (argument.equals("") || split(argument, ' ').length < 2) { } else { String[] args = split(argument, ' '); String file; if (args[1].startsWith("/")) { file = read(args[1]); } else if (args[1].equals("nano")) { file = nanoContent; } else { file = loadRMS(args[1], 1); } echoCommand(file.indexOf(args[0]) != -1 ? "true" : "false"); } }
        else if (mainCommand.equals("dir")) { if (argument.equals("f")) { try { String[] recordStores = RecordStore.listRecordStores(); if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { echoCommand(recordStores[i]); } } } catch (RecordStoreException e) { } } else { String[] files = (String[]) paths.get(path); for (int i = 0; i < files.length; i++) { if (!files[i].equals("..")) { echoCommand(files[i].trim()); } if (files.length == 1) { echoCommand("folder empty"); } } } }
        else if (mainCommand.equals("unmount")) { paths = new Hashtable(); }
        else if (mainCommand.equals("rm")) { deleteFile(argument); }

        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("basename")) { echoCommand(basename(argument)); }
        else if (mainCommand.equals("buff")) { commandInput.setString(argument); }
        else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { processCommand(argument, false); }
        else if (mainCommand.equals("break")) { app = false; }
        else if (mainCommand.equals("call")) { callCommand(argument); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { output.setText(""); } 
        else if (mainCommand.equals("date")) { echoCommand(new java.util.Date().toString()); } 
        else if (mainCommand.equals("debug")) { runScript(read("/scripts/debug.sh")); }
        else if (mainCommand.equals("env")) { Enumeration keys = attributes.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) attributes.get(key); if (!key.equals("OUTPUT") && !value.equals("")) { echoCommand(key + "=" + value.trim()); } } }
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("exit") || mainCommand.equals("quit")) { writeRMS("nano", nanoContent); notifyDestroyed(); }
        else if (mainCommand.equals("export")) { if (argument.equals("")) { echoCommand("Usage: export <name>"); } else { attributes.put(argument, ""); } }
        else if (mainCommand.equals("execute")) { String[] commands = split(argument, ';'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
        else if (mainCommand.equals("exec")) { String[] commands = split(argument, '&'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
        else if (mainCommand.equals("forget")) { commandHistory = new Vector(); }
        else if (mainCommand.equals("gc")) { Runtime.getRuntime().gc(); }
        else if (mainCommand.equals("hostname")) { echoCommand(env("$HOSTNAME")); } 
        else if (mainCommand.equals("htop")) { htopCommand(); }
        else if (mainCommand.equals("help")) { viewer("OpenTTY Help", read("/java/help.txt")); }
        else if (mainCommand.equals("history")) { showHistory(); }
        else if (mainCommand.equals("if")) { ifCommand(argument); }
        else if (mainCommand.equals("log")) { MIDletLogs(argument); }
        else if (mainCommand.equals("logcat")) { echoCommand(logs); }
        else if (mainCommand.equals("logout")) { username = ""; writeRMS("OpenRMS", ""); processCommand("exit"); }
        else if (mainCommand.equals("locale")) { echoCommand(env("$LOCALE")); }
        else if (mainCommand.equals("lock")) { lockCommand(); }
        else if (mainCommand.equals("open")) { openCommand(argument); }
        else if (mainCommand.equals("run")) { if (argument.equals("")) { runScript(nanoContent); } else { runScript(loadRMS(argument, 1)); } }
        else if (mainCommand.equals("set")) { setCommand(argument); }
        else if (mainCommand.equals("sh") || mainCommand.equals("login")) { form.setTitle(env("OpenTTY $VERSION")); output.setText(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2024 - Mr. Lima\n")); path = "/"; }
        else if (mainCommand.equals("touch")) { if (argument.equals("")) { echoCommand("Usage: touch <file>"); } else { writeRMS(argument, ""); } }
        else if (mainCommand.equals("true") || mainCommand.equals("false") || mainCommand.startsWith("#")) { }
        else if (mainCommand.equals("tty")) { echoCommand(env("$TTY")); }
        else if (mainCommand.equals("ttysize")) { echoCommand(output.getText().length() + " B"); }
        else if (mainCommand.equals("title")) { if (argument.equals("") ) { form.setTitle(env("OpenTTY $VERSION")); } else { form.setTitle(argument); } }
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        else if (mainCommand.equals("uname")) { echoCommand(env("$TYPE $CONFIG $PROFILE")); }
        else if (mainCommand.equals("unset")) { unsetCommand(argument); }
        else if (mainCommand.equals("version")) { echoCommand("OpenTTY " + version); }
        else if (mainCommand.equals("whoami")) { if (username.equals("")) { echoCommand("whoami: not logged"); } else { echoCommand(username); } } 
        else if (mainCommand.equals("warn")) { warnCommand(form.getTitle(), argument); }
        else if (mainCommand.equals("xorg")) { if (argument.length() == 0 || argument.equals("help")) { processCommand("x11"); } else if (argument.equals("stop")) { form = new Form(""); display.setCurrent(form); } else { echoCommand("xorg: " + argument + ": not found"); } } 
        else if (mainCommand.equals("x11")) { xserver(argument); }


        else if (mainCommand.equals("about")) { about(argument); }
        else if (mainCommand.equals("import")) { importScript(argument); }
        
        else if (mainCommand.equals("github")) { processCommand("open " + getAppProperty("MIDlet-Info-URL")); }
        else if (mainCommand.equals("tick")) { if (argument.equals("label")) { echoCommand(display.getCurrent().getTicker().getString()); } else { xserver("tick " + argument); } }
        
        else if (mainCommand.equals("@reload")) { shell = new Hashtable(); aliases = new Hashtable(); processCommand("execute break; x11 stop; x11 init; run initd; sh;"); app = true; }
        else if (mainCommand.equals("@exec")) { commandAction(enterCommand, display.getCurrent()); }
                
        else if (mainCommand.equals("!")) { echoCommand(env("1.8.x/$RELEASE LTS\nIn memory of Silvio Santos"));  }
        else if (mainCommand.equals(".")) { if (argument.equals("")) { } else { if (argument.startsWith("/")) { runScript(read(argument)); } else { runScript(read(path + "/" + argument)); } } }
        
        else if (mainCommand.equals("silvio")) { if (env("$LOCALE").equals("pt-BR")) { viewer("Silvio Santos", read("/java/silvio-pt.txt")); } else { viewer("Silvio Santos", read("/java/silvio-en.txt")); } }

        else { echoCommand(mainCommand + ": not found"); }
        
    }
    
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    private String extractTitle(String htmlContent) { int titleStart = htmlContent.indexOf("<title>"); int titleEnd = htmlContent.indexOf("</title>"); if (titleStart != -1 && titleEnd != -1 && titleEnd > titleStart) { return htmlContent.substring(titleStart + 7, titleEnd).trim(); } return "HTML Viewer"; }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }
    private String parseJson(String text) { Hashtable properties = parseProperties(text); Enumeration keys = properties.keys(); StringBuffer jsonBuffer = new StringBuffer(); jsonBuffer.append("{"); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); String value = (String) properties.get(key); jsonBuffer.append("\n  \"").append(key).append("\": "); jsonBuffer.append("\"").append(value).append("\""); if (keys.hasMoreElements()) { jsonBuffer.append(","); } } jsonBuffer.append("\n}"); return jsonBuffer.toString(); }
    private String loadRMS(String recordStoreName, int recordId) { RecordStore recordStore = null; String result = ""; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); if (recordStore.getNumRecords() >= recordId) { byte[] data = recordStore.getRecord(recordId); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { result = ""; } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } return result; }
    private String read(String filename) { try { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return env(content.toString()); } catch (IOException e) { return e.getMessage(); } }
    private String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0; int end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    private String env(String text) { text = replace(text, "$PATH", path); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", form.getTitle()); text = replace(text, "$l", "\n"); text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); for (Enumeration e = attributes.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); String value = (String) attributes.get(key); text = replace(text, "$" + key, value); } return text; }
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    
    private String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    private Hashtable parseProperties(String text) { Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (!line.startsWith("#")) { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { String key = line.substring(0, equalIndex).trim(); String value = line.substring(equalIndex + 1).trim(); properties.put(key, value); } } } return properties; }
    private Hashtable parseFrom(String script) { if (script.startsWith("/")) { script = read(script); } else if (script.equals("nano")) { script = nanoContent; } else { script = loadRMS(script, 1); } return parseProperties(script); }

    private void aliasCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: alias <name> <command>"); return; } String aliasName = argument.substring(0, spaceIndex).trim(); String aliasCommand = argument.substring(spaceIndex + 1).trim(); aliases.put(aliasName, aliasCommand); }
    private void unaliasCommand(String aliasName) { if (aliasName == null || aliasName.length() == 0) { echoCommand("Usage: unalias <alias>"); return; } if (aliases.containsKey(aliasName)) { aliases.remove(aliasName); } else { echoCommand("unalias: " + aliasName + ": not found"); } }
    private void setCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: set <key> <value>"); return; } attributes.put(argument.substring(0, spaceIndex).trim(), argument.substring(spaceIndex + 1).trim()); }
    private void unsetCommand(String key) { if (key == null || key.length() == 0) { echoCommand("Usage: unset <key>"); return; } if (attributes.containsKey(key)) { attributes.remove(key); } else { echoCommand("unalias: " + key + ": not found"); } }
    
    private void echoCommand(String message) { output.setText(output.getText() + "\n" + message); attributes.put("OUTPUT", message); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Phone", "", 16, TextField.NUMERIC); final Command save = new Command("Call", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { callCommand(name.getString().trim()); display.setCurrent(form); } } } } ); display.setCurrent(screen); return; } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("API Url", "", 256, TextField.ANY);  final Command save = new Command("Open", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { openCommand(name.getString().trim()); display.setCurrent(form); } } } } ); display.setCurrent(screen); return; } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void warnCommand(String title, String message) { if (message == null || message.length() == 0) { echoCommand("Usage: warn <message>"); return; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, form); }
    private void lockCommand() { final Form lock = new Form(form.getTitle() + " - Locked"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); final StringItem text = new StringItem("", "OpenTTY was blocked! Insert your\nusername to return to console."); final Command unlock = new Command("Unlock", Command.OK, 1); lock.append(text); lock.append(userField); lock.addCommand(unlock); lock.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == unlock) { if (userField.getString().equals(username)) { display.setCurrent(form); } else if (!userField.getString().equals("")) { Alert alert = new Alert(form.getTitle(), "Wrong username!", null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert, lock); } } } } ); display.setCurrent(lock); }
    private void htopCommand() { Runtime runtime = Runtime.getRuntime(); viewer(form.getTitle(), "Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB\nFree Memory: " + runtime.freeMemory() / 1024 + " KB\nTotal Memory: " + runtime.totalMemory() / 1024 + " KB"); }
    
    private void showHistory() { final List historyList = new List("OpenTTY History", List.IMPLICIT); final Command back = new Command("Back", Command.BACK, 1); final Command run = new Command("Run", Command.OK, 2); final Command edit = new Command("Edit", Command.OK, 2); for (int i = 0; i < commandHistory.size(); i++) { historyList.append((String) commandHistory.elementAt(i), null); } historyList.addCommand(back); historyList.addCommand(run); historyList.addCommand(edit); historyList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == run) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); processCommand(historyList.getString(index)); } } else if (c == edit) { int index = historyList.getSelectedIndex(); if (index >= 0) { display.setCurrent(form); commandInput.setString(historyList.getString(index)); } } } }); display.setCurrent(historyList); }
    
    
    private void nano(String file) { if (file == null || file.length() == 0) { file = nanoContent; } else { file = loadRMS(file, 1); } final TextBox editor = new TextBox("Nano", file, 4096, TextField.ANY); final Command back = new Command("Back", Command.OK, 1); final Command clear = new Command("Clear", Command.SCREEN, 2); final Command run = new Command("View as HTML", Command.SCREEN, 3); editor.addCommand(back); editor.addCommand(clear); editor.addCommand(run); editor.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { nanoContent = editor.getString(); display.setCurrent(form); } else if (c == clear) { editor.setString(""); } else if (c == run) { nanoContent = editor.getString(); viewer(extractTitle(nanoContent), html2text(nanoContent)); } } }); display.setCurrent(editor); }
    private void viewer(String title, String text) { Form viewer = new Form(env(title)); StringItem contentItem = new StringItem(null, env(text)); final Command back = new Command("Back", Command.OK, 1); viewer.addCommand(back); viewer.append(contentItem); viewer.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } } }); display.setCurrent(viewer); }
    
    private void runScript(String script) { String[] commands = split(script, '\n'); for (int i = 0; i < commands.length; i++) { processCommand(commands[i].trim()); } }
    
    
    private void mount(String script) { String[] lines = split(script, '\n'); for (int i = 0; i < lines.length; i++) { String line = ""; if (lines[i] != null) { line = lines[i].trim(); } if (line.startsWith("#")) { } else if (line.length() != 0) { if (line.startsWith("/")) { String fullPath = ""; int start = 0; for (int j = 1; j < line.length(); j++) { if (line.charAt(j) == '/') { String dir = line.substring(start + 1, j); fullPath += "/" + dir; addDirectory(fullPath); start = j; } } String dir = line.substring(start + 1); fullPath += "/" + dir; addDirectory(fullPath); } } } }
    private void addDirectory(String fullPath) { if (!paths.containsKey(fullPath)) { paths.put(fullPath, new String[] { ".." }); String parentPath = fullPath.substring(0, fullPath.lastIndexOf('/')); if (parentPath.length() == 0) { parentPath = "/"; } String[] parentContents = (String[]) paths.get(parentPath); Vector updatedContents = new Vector(); if (parentContents != null) { for (int k = 0; k < parentContents.length; k++) { updatedContents.addElement(parentContents[k]); } } updatedContents.addElement(fullPath.substring(fullPath.lastIndexOf('/') + 1)); String[] newContents = new String[updatedContents.size()]; updatedContents.copyInto(newContents); paths.put(parentPath, newContents); } }
 
    private void changeDisk(String way) { if (way == null || way.length() == 0) { echoCommand("Usage: cd <dir>"); return; } String[] availablePaths = (String[]) paths.get(path); if (availablePaths != null) { boolean pathFound = false; for (int i = 0; i < availablePaths.length; i++) { if (availablePaths[i].equals(way)) { pathFound = true; break; } } if (pathFound) { if (way.equals("..")) { int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == 0) { path = "/"; } else { path = path.substring(0, lastSlashIndex); } } else { path = path.equals("/") ? "/" + way : path + "/" + way; } } else { if (way.equals("/") || way.equals(".") || way.equals("..")) { path = "/"; return; } echoCommand("cd: " + way + ": not found"); } } else { echoCommand("cd: " + way + ": not found"); } }
    private void ifCommand(String argument) { int firstSpaceIndex = argument.indexOf(' '); int secondSpaceIndex = argument.indexOf(' ', firstSpaceIndex + 1); if (firstSpaceIndex == -1) { echoCommand("Usage: if <x> <y> [command] "); return; } if (secondSpaceIndex == -1) { processCommand("warn java.io.IOException: missing operators"); return; } String value1 = argument.substring(0, firstSpaceIndex).trim(); String value2 = argument.substring(firstSpaceIndex + 1, secondSpaceIndex).trim(); String command = argument.substring(secondSpaceIndex + 1).trim(); if (value1.equals(value2)) { processCommand(command); } if (value1.equals("not") && value2.equals("")) { processCommand(command); } if (value1.equals("is") && !value2.equals("") ) { processCommand(command); } }
    private void writeRMS(String recordStoreName, String data) { RecordStore recordStore = null; try { recordStore = RecordStore.openRecordStore(recordStoreName, true); byte[] byteData = data.getBytes(); if (recordStore.getNumRecords() > 0) { recordStore.setRecord(1, byteData, 0, byteData.length); } else { recordStore.addRecord(byteData, 0, byteData.length); } } catch (RecordStoreException e) { } finally { if (recordStore != null) { try { recordStore.closeRecordStore(); } catch (RecordStoreException e) { } } } }
    private void deleteFile(String filename) { if (filename == null || filename.length() == 0) { echoCommand("Usage: rm <filename>"); return; } try { RecordStore.deleteRecordStore(filename); } catch (RecordStoreNotFoundException e) { echoCommand("rm: " + filename + ": not found"); } catch (RecordStoreException e) { echoCommand("rm: " + e.getMessage()); } }
    private void install(String filename) { if (filename == null || filename.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Filename", "", 16, TextField.ANY);  final Command save = new Command("Save", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { writeRMS(name.getString().trim(), nanoContent); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { writeRMS(filename, nanoContent); } }
    private void loadCommand(String filename) { if (filename == null || filename.length() == 0) { final Form screen = new Form(form.getTitle()); final TextField name = new TextField("Filename", "", 16, TextField.ANY); final Command save = new Command("Load", Command.OK, 1); final Command back = new Command("Cancel", Command.SCREEN, 2); screen.append(name); screen.addCommand(save); screen.addCommand(back); screen.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } else if (c == save) { if (name.getString().trim().equals("")) { } else { nanoContent = loadRMS(name.getString().trim(), 1); display.setCurrent(form); } } } } ); display.setCurrent(screen); } else { nanoContent = loadRMS(filename, 1); } }
    
    // X Server 0.5.1 - Project Fork
    private void xserver(String command) {
        command = env(command.trim());
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (mainCommand.equals("")) { viewer("OpenTTY X.Org", env("OpenTTY X.Org - X Server $XVERSION\nRelease Date: 2024-11-22\nX Protocol Version 1, Revision 3\nBuild OS: $TYPE")); } 
        else if (mainCommand.equals("title")) { form.setTitle(argument); }
        else if (mainCommand.equals("term")) { display.setCurrent(form); } 
        else if (mainCommand.equals("version")) { echoCommand(env("X Server $XVERSION")); }
        else if (mainCommand.equals("stop")) { form.setTitle(""); form.setTicker(null); form.deleteAll(); xserver("cmd hide"); form.removeCommand(enterCommand); }
        else if (mainCommand.equals("tick")) { Displayable current = display.getCurrent(); if (argument.equals("")) { current.setTicker(null); } else { current.setTicker(new Ticker(argument)); } }
        else if (mainCommand.equals("init")) { form.setTitle(env("OpenTTY $VERSION")); form.append(output); form.append(commandInput); form.addCommand(enterCommand); xserver("cmd"); form.setCommandListener(this); }
        else if (mainCommand.equals("cmd")) { if (argument.equals("hide")) { form.removeCommand(helpCommand); form.removeCommand(nanoCommand); form.removeCommand(clearCommand); form.removeCommand(historyCommand); } else { form.addCommand(helpCommand); form.addCommand(nanoCommand); form.addCommand(clearCommand); form.addCommand(historyCommand); } }
        else if (mainCommand.equals("canvas")) { display.setCurrent(new Canvas() { private int cursorX = 10, cursorY = 10, cursorSize = 5; protected void paint(Graphics g) { g.setColor(0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); g.setColor(255, 255, 255); g.fillRect(cursorX, cursorY, cursorSize, cursorSize); } protected void keyPressed(int keyCode) { int gameAction = getGameAction(keyCode); if (gameAction == LEFT) { cursorX = Math.max(0, cursorX - 5); } else if (gameAction == RIGHT) { cursorX = Math.min(getWidth() - cursorSize, cursorX + 5); } else if (gameAction == UP) { cursorY = Math.max(0, cursorY - 5); } else if (gameAction == DOWN) { cursorY = Math.min(getHeight() - cursorSize, cursorY + 5); } else { processCommand("xterm"); } repaint(); } }); }
        else if (mainCommand.equals("item")) { if (argument.equals("")) { echoCommand("x11: item: missing file"); } else if (argument.equals("clear")) { form.deleteAll(); form.append(output); form.append(commandInput); } else { final Hashtable lib = parseFrom(argument); if (lib.containsKey("item.label") && lib.containsKey("item.cmd")) { final Command run = new Command((String) lib.get("item.label"), Command.ITEM, 1); final StringItem s = new StringItem(null, (String) lib.get("item.label"), StringItem.BUTTON); s.setFont(Font.getDefaultFont()); s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); s.addCommand(run); s.setDefaultCommand(run); s.setItemCommandListener(new ItemCommandListener() { public void commandAction(Command c, Item item) { if (c == run) { processCommand("xterm"); processCommand((String) lib.get("item.cmd")); } } }); form.append(s); } else { MIDletLogs("add error Malformed ITEM, missing params"); } } }
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
        
        if (lib.containsKey("api.version")) { if (!((String) lib.get("api.version")).equals(env("$VERSION"))) { processCommand(lib.containsKey("api.error") ? (String) lib.get("api.error") : "true"); return; } }

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
    private void login() { final Form login = new Form("Login"); final Command loginCommand = new Command("Login", Command.OK, 1); final Command exitCommand = new Command("Exit", Command.SCREEN, 2); final StringItem text = new StringItem("", "Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n\nCreate an user to acess OpenTTY!"); final TextField userField = new TextField("Username", "", 256, TextField.ANY); login.append(text); login.append(userField); login.addCommand(loginCommand); login.addCommand(exitCommand); login.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == loginCommand) { username = userField.getString(); if (!username.equals("")) { writeRMS("OpenRMS", username); display.setCurrent(form); } } else if (c == exitCommand) { notifyDestroyed(); } } } ); display.setCurrent(login); } 
    
    // Network API Service
    private void netstat() { new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open("http://ipinfo.io/ip"); conn.setRequestMethod(HttpConnection.GET); echoCommand("true"); conn.close(); } catch (Exception e) { echoCommand("false"); } } }).start(); }
    private void fwCommand(final String ip) { if (ip == null || ip.length() == 0) { ip = "json"; } final String url = "http://ipinfo.io/" + ip; new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("fw: " + e.getMessage()); } } }).start(); }
    private void curlCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: curl <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("curl: " + e.getMessage()); } } }).start(); }
    private void wgetCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: wget <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } nanoContent = new String(baos.toByteArray()); echoCommand("wget: html saved to nano"); is.close(); conn.close(); } catch (Exception e) { echoCommand("wget: " + e.getMessage()); } } }).start(); }
    private void pingCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: ping <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); is.close(); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } } }).start(); }
    private void connect(final String host) { if (host == null || host.length() == 0) { echoCommand("Usage: nc <ip:port>"); return; } try { final SocketConnection socket = (SocketConnection) Connector.open("socket://" + host); final InputStream inputStream = socket.openInputStream(); final OutputStream outputStream = socket.openOutputStream(); final Form remote = new Form(form.getTitle()); final TextField inputField = new TextField("Remote", "", 256, TextField.ANY); final Command sendCommand = new Command("Send", Command.OK, 1); final Command backCommand = new Command("Back", Command.SCREEN, 2); final Command clearCommand = new Command("Clear", Command.SCREEN, 3); final StringItem console = new StringItem("", ""); remote.append(console); remote.append(inputField); remote.addCommand(backCommand); remote.addCommand(clearCommand); remote.addCommand(sendCommand); remote.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == sendCommand) { final String data = inputField.getString() + "\n"; inputField.setString(""); try { outputStream.write(data.getBytes()); outputStream.flush(); new Thread(new Runnable() { public void run() { try { byte[] buffer = new byte[1024]; int length = inputStream.read(buffer); if (length != -1) { String response = new String(buffer, 0, length); console.setText(console.getText() + "\n" + response.trim()); } } catch (IOException e) { processCommand("warn " + e.getMessage()); } } } ).start(); } catch (IOException e) { processCommand("warn "  + e.getMessage()); } } else if (c == backCommand) { writeRMS("console", console.getText()); display.setCurrent(form); } else if (c == clearCommand) { console.setText(""); } } }); display.setCurrent(remote); } catch (IOException e) { processCommand("warn " + e.getMessage()); } }
    private void runServer(final String port) { if (port == null || port.length() == 0 || port.equals("$PORT")) { processCommand("set PORT 4095"); runServer("4095"); return; } new Thread(new Runnable() { public void run() { ServerSocketConnection serverSocket = null; try { serverSocket = (ServerSocketConnection) Connector.open("socket://:" + port); echoCommand("[+] listening at port " + port); while (true) { SocketConnection clientSocket = (SocketConnection) serverSocket.acceptAndOpen(); InputStream is = clientSocket.openInputStream(); OutputStream os = clientSocket.openOutputStream(); echoCommand("[+] " + clientSocket.getAddress() + " connected"); byte[] buffer = new byte[256]; int bytesRead = is.read(buffer); String clientData = new String(buffer, 0, bytesRead); echoCommand("[+] " + clientSocket.getAddress() + " -> " + env(clientData.trim())); String response = env("$RESPONSE"); if (response.equals("nano")) { os.write(nanoContent.getBytes()); } else { os.write(response.getBytes()); } } } catch (IOException e) { echoCommand("[-] " + e.getMessage()); serverSocket.close(); } } }).start(); }
    private void portScanner(final String host) { if (host == null || host.length() == 0) { echoCommand("Usage: prscan <ip>"); return; } final List openPortsList = new List(host + " Ports", List.IMPLICIT); final Command connectCommand = new Command("Connect", Command.OK, 1); final Command backCommand = new Command("Back", Command.BACK, 2); openPortsList.addCommand(connectCommand); openPortsList.addCommand(backCommand); openPortsList.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == connectCommand) { int index = openPortsList.getSelectedIndex(); String selectedPort = openPortsList.getString(index); connect(host + ":" + selectedPort); } else if (c == backCommand) { display.setCurrent(form); } } }); new Thread(new Runnable() { public void run() { display.setCurrent(openPortsList); for (int port = 1; port <= 9999; port++) { try { SocketConnection socket = (SocketConnection) Connector.open("socket://" + host + ":" + port); openPortsList.append(Integer.toString(port), null); socket.close(); } catch (IOException e) { } } } }).start(); }
    private void ifconfig(final String ip) { if (ip == null || ip.length() == 0) { ip = "ua"; } final String url = "http://ifconfig.me/" + ip; new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("No Network Connection"); } } }).start(); }
    
}
