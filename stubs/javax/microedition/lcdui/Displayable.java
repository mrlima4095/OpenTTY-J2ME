package javax.microedition.lcdui;

import java.util.Hashtable;
import java.util.Vector;

public class Displayable {
    private String title = "";
    private Ticker ticker = null;
    private final Vector commands = new Vector();
    private CommandListener commandListener = null;

    Display display = null;
    boolean shown = false;
    Object _comp = null;   // cached renderer component (JComponent)

    public void addCommand(Command cmd) {
        if (cmd == null) { return; }
        synchronized (commands) {
            if (!commands.contains(cmd)) { commands.addElement(cmd); }
        }
        _repaint();
    }

    public void removeCommand(Command cmd) {
        synchronized (commands) { commands.removeElement(cmd); }
        _repaint();
    }

    public void setCommandListener(CommandListener l) {
        commandListener = l;
    }

    CommandListener _listener() { return commandListener; }

    Command[] _commands() {
        synchronized (commands) {
            Command[] out = new Command[commands.size()];
            commands.copyInto(out);
            return out;
        }
    }

    public void setTitle(String title) {
        this.title = title == null ? "" : title;
        _repaint();
    }

    public String getTitle() { return title; }

    public void setTicker(Ticker ticker) {
        this.ticker = ticker;
        _repaint();
    }

    public Ticker getTicker() { return ticker; }

    public int getWidth() { return Display._screenWidth(); }
    public int getHeight() { return Display._screenHeight(); }

    public boolean isShown() {
        return display != null && display.getCurrent() == this;
    }

    void _fire(Command c) {
        CommandListener l = commandListener;
        if (l != null) { l.commandAction(c, this); }
    }

    void _repaint() {
        if (display != null && display.getCurrent() == this) {
            DesktopLcdui._repaint(this);
        }
    }

    public String toString() {
        return getClass().getName() + "[" + title + "]";
    }
}