import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import javax.bluetooh.*;
import java.util.*;
import java.io.*;

public class Registry extends MIDlet implements CommandListener {
    private Display display = Display.getDisplay(this);
    private Form form = new Form("PushRegistry");
    private TextField stdin = new TextField("MIDlet Class", "", 256, TextField.ANY);
    private Command INIT = new Command("Start", Command.OK, 1), 
                    EXIT = new Command("Exit", Command.OK, 1);

    public void startApp() {
        form.append(stdin);

        form.addCommand(INIT);
        form.addCommand(EXIT);

        form.setCommandListener(this);
        display.setCurrent(form);
    }

    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { }

    public void commandAction(Command c, Displayable d) {
        if (c == INIT) {
            try { 
                PushRegistry.registerAlarm(stdin.getString().trim(), System.currentTimeMillis() + 5000); 
            } catch (ClassNotFoundException e) { 
                AlertCommand("PushRegistry", "MIDlet Class '" + stdin.getString().trim() + "' not found!");
            } catch (Exception e) { 
                AlertCommand("PushRegistry", e.getMessage()); 
            }
        
        } else if (c == EXIT) {
            notifyDestroyed();
        }
    }

    private void AlertCommand(String title, String message) { 
        if (message == null || message.length() == 0) { return; } 

        Alert alert = new Alert(title, message, null, AlertType.WARNING); 
        alert.setTimeout(Alert.FOREVER); 

        display.setCurrent(alert); 
    }
    

}