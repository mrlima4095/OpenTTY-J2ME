import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;

public class MIDletGod extends MIDlet implements CommandListener {
	private Form screen = new Form("MIDlet God");
	private Command back = new Command("Back", Command.BACK, 1);
	private StringItem text = new StringItem("", "Test MIDlet PushRegistry Transfer");
    private Display display = Display.getDisplay(this);

	public void startApp() {
        screen.append(text);
        screen.addCommand(back);
        screen.setCommandListener(this);

        display.setCurrent(screen);
    }

    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { }

    public void commandAction(Command c, Displayable d) {
        if (c == back) {
        	try { 
        		screen.setTicker(new Ticker("Loading..."));
                PushRegistry.registerAlarm("OpenTTY", 5000); 
            } 
            catch (NumberFormatException e) { echoCommand(e.getMessage()); } 
            catch (ClassNotFoundException e) { echoCommand(e.getMessage()); }
            catch (Exception e) { echoCommand("AutoRunError: " + e.getMessage()); }
        }
    }

    private void echoCommand(String message) { echoCommand(message, text); }
    private void echoCommand(String message, StringItem console) { console.setText(console.getText().equals("") ? message.trim() : console.getText() + "\n" + message.trim()); }
    

}