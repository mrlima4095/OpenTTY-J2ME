import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;

public class OpenTTY extends MIDlet implements CommandListener {
    private Display display;
    private Form form;
    private TextField commandInput;
    private StringItem output;
    private Command enterCommand;
    private Command helpCommand;
    private Command executeCommand;
    private Command openCommand;
    private Command clearCommand;
    private Command loginCommand;
    private String username;
    private String version;
    private boolean app;

    public void startApp() {
        
        if (!app == true) {
            // OpenTTY Settings
            version = "1.1";
            username = "";
        
            display = Display.getDisplay(this);
            form = new Form("OpenTTY " + version);
            
            commandInput = new TextField("Command", "", 256, TextField.ANY);
            output = new StringItem("", "Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n");
            
            enterCommand = new Command("Send", Command.OK, 1);
            helpCommand = new Command("Help", Command.SCREEN, 2);
            openCommand = new Command("Open", Command.SCREEN, 3);
            clearCommand = new Command("Clear", Command.SCREEN, 4);
            loginCommand = new Command("Login", Command.SCREEN, 5);
            executeCommand = new Command("Execute", Command.SCREEN, 6);
            
            form.append(output);
            form.append(commandInput);
            form.addCommand(enterCommand); form.addCommand(helpCommand); form.addCommand(openCommand); 
            form.addCommand(clearCommand); form.addCommand(loginCommand); form.addCommand(executeCommand);
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
        else if (c == executeCommand) { commandInput.setString("execute "); }
        else if (c == loginCommand) { commandInput.setString(commandInput.getString() + "login "); }
        else if (c == openCommand) { commandInput.setString(commandInput.getString() + "open "); }
    }
    
    // OpenTTY Command Processor
    private void processCommand(String command) {
        command = command.trim();
        String mainCommand = getCommand(command).toLowerCase();
        String argument = getArgument(command);
        
        if (mainCommand.equals("")) { } 
        else if (mainCommand.equals("!")) { echoCommand("OpenTTY Java Edition"); }
        else if (mainCommand.equals("date")) { dateCommand(); } 
        else if (mainCommand.equals("lock")) { lockCommand(); }
        else if (mainCommand.equals("login")) { login(argument); }
        else if (mainCommand.equals("exit")) { notifyDestroyed(); } 
        else if (mainCommand.equals("call")) { callCommand(argument); }
        else if (mainCommand.equals("echo")) { echoCommand(argument); }
        else if (mainCommand.equals("open")) { openCommand(argument); }
        else if (mainCommand.equals("uname")) { unameCommand(argument); }
        else if (mainCommand.equals("execute")) { processCommand(argument); }
        else if (mainCommand.equals("true") || mainCommand.equals("false")) { }
        else if (mainCommand.equals("version")) { echoCommand("OpenTTY " + version); }
        else if (mainCommand.equals("clear") || mainCommand.equals("cls")) { output.setText(""); } 
        else if (mainCommand.equals("locale")) { echoCommand(System.getProperty("microedition.locale")); }
        else if (mainCommand.equals("hostname")) { echoCommand(System.getProperty("microedition.hostname")); } 
        else if (mainCommand.equals("logout")) { if (username.equals("")) { echoCommand("logout: not logged"); } else { username = ""; }  } 
        else if (mainCommand.equals("title")) { if (argument.equals("") ) { form.setTitle("OpenTTY " + version); } else { form.setTitle(argument); } }
        else if (mainCommand.equals("sh")) { form.setTitle("OpenTTY " + version); output.setText("Welcome to OpenTTY " + version + "\nCopyright (C) 2024 - Mr. Lima\n"); }
        else if (mainCommand.equals("whoami")) { if (username.equals("")) { echoCommand("whoami: not logged"); } else { echoCommand(username + "@" + System.getProperty("microedition.hostname")); } } 
        else if (mainCommand.equals("help")) { echoCommand("[call *number] [clear]\n[echo <text>] [exit]\n[execute (command)]\n[false] [hostname]\n[help] [login <user>]\n[logout] [open <url>]\n[true] [title <text>]\n[uname -a] [version]"); }
        else { echoCommand(mainCommand + ": unknown command"); }
        
    }
    
    private String getCommand(String input) {
        int spaceIndex = input.indexOf(' ');
        if (spaceIndex == -1) { return input; } else { return input.substring(0, spaceIndex); }
    }
    
    private String getArgument(String input) {
        int spaceIndex = input.indexOf(' ');
        if (spaceIndex == -1) { return ""; } else { return input.substring(spaceIndex + 1).trim(); }
    }
    
    private void dateCommand() { echoCommand(new java.util.Date().toString()); }
    private void echoCommand(String message) { output.setText(output.getText() + "\n" + message); }
    private void unameCommand(String options) { output.setText(output.getText() + "\n" + System.getProperty("microedition.platform") + " " + System.getProperty("microedition.configuration") + " " + System.getProperty("microedition.profiles")); }
    private void callCommand(String number) { if (number == null || number.length() == 0) { echoCommand("Usage: call <phone>"); return; } try { platformRequest("tel:" + number); } catch (Exception e) { } }
    private void openCommand(String url) { if (url == null || url.length() == 0) { echoCommand("Usage: open <url>"); return; } try { platformRequest(url); } catch (Exception e) { echoCommand("open: " + url + ": not found"); } }
    private void login(String user) { if (user == null || user.length() == 0) { echoCommand("Usage: login <user>"); } else { if (username.equals("")) { username = user; } else { echoCommand("login: already logged"); } } }
    private void lockCommand() { if (username == null || username.length() == 0) { echoCommand("lock: not logged"); } else { while (true) { if (commandInput.getString().equals(username)) { break; } } } }
    
}