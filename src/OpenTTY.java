import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.Hashtable;
import java.io.*;


public class OpenTTY extends MIDlet implements CommandListener {
    private boolean app;
    private String version = "1.3";
    private String username = "";
    private String nanoContent = "";
    private String hostname = "unknown";
    private String tty = "/java/optty1";
    private RecordStore userStore = null;
    private Hashtable aliases = new Hashtable();
    private Display display = Display.getDisplay(this);
    private Form form = new Form("OpenTTY " + version);
    private Command enterCommand = new Command("Send", Command.OK, 1);
    private Command helpCommand = new Command("Help", Command.SCREEN, 2);
    private Command nanoCommand = new Command("Nano", Command.SCREEN, 3);
    private Command clearCommand = new Command("Clear", Command.SCREEN, 4);
    private Command loginCommand = new Command("Login", Command.SCREEN, 5);
    private TextField commandInput = new TextField("Command", "", 256, TextField.ANY);
    private StringItem output = new StringItem("", "Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n");
    
    public void startApp() {
        if (!app == true) {
            loadUsername();
            
            form.append(output);
            form.append(commandInput);
            form.addCommand(enterCommand); form.addCommand(helpCommand); form.addCommand(nanoCommand); 
            form.addCommand(clearCommand); form.addCommand(loginCommand); 
            form.setCommandListener(this);
            
            display.setCurrent(form);
        }    
    }

    public void pauseApp() { app = true; }
    public void destroyApp(boolean unconditional) { }

    public void commandAction(Command c, Displayable d) {
        if (c == enterCommand) { processCommand(commandInput.getString()); commandInput.setString(""); }
        
        else if (c == clearCommand) { output.setText(""); }
        else if (c == helpCommand) { processCommand("help"); } 
        else if (c == loginCommand) { if (username == "") { commandInput.setString("login "); } else { commandInput.setString("logout"); } }
        else if (c == nanoCommand) { nano(); }
    }
    
    // OpenTTY Command Processor
    private void processCommand(String command) {
        command = command.trim();
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (aliases.containsKey(mainCommand)) { mainCommand = (String) aliases.get(mainCommand); }
        
        if (mainCommand.equals("")) { }
        else if (mainCommand.equals("!")) { echoCommand("OpenTTY Java Edition"); }
        else if (mainCommand.equals("nano")) { nano(); }
        else if (mainCommand.equals("netstat")) { netstat(); }
        else if (mainCommand.equals("date")) { dateCommand(); } 
        else if (mainCommand.equals("lock")) { lockCommand(); }
        else if (mainCommand.equals("htop")) { htopCommand(); }
        else if (mainCommand.equals("tty")) { echoCommand(tty); }
        else if (mainCommand.equals("login")) { login(argument); }
        else if (mainCommand.equals("exit")) { notifyDestroyed(); }
        else if (mainCommand.equals("ping")) { pingCommand(argument); }
        else if (mainCommand.equals("curl")) { curlCommand(argument); } 
        else if (mainCommand.equals("wget")) { wgetCommand(argument); } 
        else if (mainCommand.equals("call")) { callCommand(argument); }
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("open")) { openCommand(argument); }
        else if (mainCommand.equals("uname")) { unameCommand(argument); }
        else if (mainCommand.equals("alias")) { aliasCommand(argument); }
        else if (mainCommand.equals("hostname")) { echoCommand(hostname); } 
        else if (mainCommand.equals("unalias")) { unaliasCommand(argument); }
        else if (mainCommand.equals("execute")) { processCommand(argument); }
        else if (mainCommand.equals("asset")) { echoCommand(read(argument)); }
        else if (mainCommand.equals("true") || mainCommand.equals("false")) { }
        else if (mainCommand.equals("version")) { echoCommand("OpenTTY " + version); }
        else if (mainCommand.equals("help")) { viewer("OpenTTY Help", read("/help.txt")); }
        else if (mainCommand.equals("ipconfig")) { curlCommand("http://checkip.amazonaws.com"); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { output.setText(""); } 
        else if (mainCommand.equals("locale")) { echoCommand(System.getProperty("microedition.locale")); }
        else if (mainCommand.equals("title")) { if (argument.equals("") ) { form.setTitle("OpenTTY " + version); } else { form.setTitle(argument); } }
        else if (mainCommand.equals("logout")) { if (username.equals("")) { echoCommand("logout: not logged"); } else { username = ""; deleteUsername(); }  } 
        else if (mainCommand.equals("whoami")) { if (username.equals("")) { echoCommand("whoami: not logged"); } else { echoCommand(username + "@" + hostname); } } 
        else if (mainCommand.equals("sh")) { form.setTitle("OpenTTY " + version); output.setText("Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n"); }
        else { echoCommand(mainCommand + ": unknown command"); }
        
    }
    
    private String getCommand(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); } }
    private String getArgument(String input) { int spaceIndex = input.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); } }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; } else if (c == '>') { inTag = false; } else if (!inTag) { text.append(c); } } return text.toString(); } 
    private String read(String filename) { try { StringBuffer content = new StringBuffer(); InputStream is = getClass().getResourceAsStream(filename); InputStreamReader isr = new InputStreamReader(is, "UTF-8"); int ch; while ((ch = isr.read()) != -1) { content.append((char) ch); } isr.close(); return content.toString(); } catch (IOException e) { return e.getMessage(); } }
    
    private void aliasCommand(String argument) { int spaceIndex = argument.indexOf(' '); if (spaceIndex == -1) { echoCommand("Usage: alias <name> <command>"); return; } String aliasName = argument.substring(0, spaceIndex).trim(); String aliasCommand = argument.substring(spaceIndex + 1).trim(); aliases.put(aliasName, aliasCommand); }
    private void unaliasCommand(String aliasName) { if (aliasName == null || aliasName.length() == 0) { echoCommand("Usage: unalias <alias>"); return; } if (aliases.containsKey(aliasName)) { aliases.remove(aliasName); } else { echoCommand("unalias: " + aliasName + ": not found"); } }
    
    private void dateCommand() { echoCommand(new java.util.Date().toString()); }
    private void echoCommand(String message) { output.setText(output.getText() + "\n" + message); }
    private void unameCommand(String options) { output.setText(output.getText() + "\n" + System.getProperty("microedition.platform") + " " + System.getProperty("microedition.configuration") + " " + System.getProperty("microedition.profiles")); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { echoCommand("Usage: call <phone>"); return; } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { echoCommand("Usage: open <url>"); return; } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void lockCommand() { if (username == null || username.length() == 0) { echoCommand("lock: not logged"); } else { commandInput.setString(""); while (true) { if (commandInput.getString().equals(username)) { break; } } } }
    private void htopCommand() { Runtime runtime = Runtime.getRuntime(); echoCommand("Memory Status:\n\nUsed Memory: " + (runtime.totalMemory() - runtime.freeMemory()) / 1024 + " KB"); echoCommand("Free Memory: " + runtime.freeMemory() / 1024 + " KB"); echoCommand("Total Memory: " + runtime.totalMemory() / 1024 + " KB"); }
    
    private void nano() { final TextBox editor = new TextBox("Nano", nanoContent, 9600, TextField.ANY); final Command back = new Command("Back", Command.OK, 1); final Command clear = new Command("Clear", Command.SCREEN, 2); final Command run = new Command("View as HTML", Command.SCREEN, 3); editor.addCommand(back); editor.addCommand(clear); editor.addCommand(run); editor.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { nanoContent = editor.getString(); display.setCurrent(form); } else if (c == clear) { editor.setString(""); } else if (c == run) { nanoContent = editor.getString(); viewer("HTML Viewer", html2text(nanoContent)); } } }); display.setCurrent(editor); }
    private void viewer(String title, String text) { Form viewer = new Form(title); StringItem contentItem = new StringItem(null, text); final Command back = new Command("Back", Command.OK, 1); viewer.addCommand(back); viewer.append(contentItem); viewer.setCommandListener(new CommandListener() { public void commandAction(Command c, Displayable d) { if (c == back) { display.setCurrent(form); } } }); display.setCurrent(viewer); }
    
    // Login API Service
    private void login(String user) { if (user == null || user.length() == 0) { echoCommand("Usage: login <user>"); } else { if (username.equals("")) { username = user; saveUsername(user); } else { echoCommand("login: already logged"); } } }
    private void saveUsername(String user) { try { userStore = RecordStore.openRecordStore("OpenRMS", true); byte[] userData = user.getBytes(); if (userStore.getNumRecords() > 0) { userStore.setRecord(1, userData, 0, userData.length); } else { userStore.addRecord(userData, 0, userData.length); } userStore.closeRecordStore(); } catch (RecordStoreException e) { } }
    private void loadUsername() { try { userStore = RecordStore.openRecordStore("OpenRMS", true); if (userStore.getNumRecords() > 0) { byte[] userData = userStore.getRecord(1); username = new String(userData); } userStore.closeRecordStore(); } catch (RecordStoreException e) { username = ""; } }
    private void deleteUsername() { try { userStore = RecordStore.openRecordStore("OpenRMS", true); userStore.deleteRecord(1); userStore.closeRecordStore(); } catch (RecordStoreException e) { } }
    
    // Network API Service
    private void netstat() { new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open("http://www.google.com"); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); conn.getResponseCode(); echoCommand("Available"); conn.close(); } catch (IOException e) { echoCommand("Unavailable"); } } }).start(); }
    private void curlCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: curl <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } String response = new String(baos.toByteArray()); echoCommand(response); is.close(); conn.close(); } catch (Exception e) { echoCommand("curl: " + e.getMessage()); } } }).start(); }
    private void wgetCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: wget <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream(); int ch; while ((ch = is.read()) != -1) { baos.write(ch); } nanoContent = new String(baos.toByteArray()); echoCommand("wget: html saved to nano"); is.close(); conn.close(); } catch (Exception e) { echoCommand("wget: " + e.getMessage()); } } }).start(); }
    private void pingCommand(final String url) { if (url == null || url.length() == 0) { echoCommand("Usage: ping <url>"); return; } if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; } new Thread(new Runnable() { public void run() { long startTime = System.currentTimeMillis(); try { HttpConnection conn = (HttpConnection) Connector.open(url); conn.setRequestMethod(HttpConnection.GET); InputStream is = conn.openInputStream(); int responseCode = conn.getResponseCode(); long endTime = System.currentTimeMillis(); echoCommand("Ping to " + url + " successful, time=" + (endTime - startTime) + "ms"); is.close(); conn.close(); } catch (IOException e) { echoCommand("Ping to " + url + " failed: " + e.getMessage()); } } }).start(); }
    
}