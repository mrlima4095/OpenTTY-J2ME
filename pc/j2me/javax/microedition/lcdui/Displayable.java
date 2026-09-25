package javax.microedition.lcdui;

import java.util.Vector;

public abstract class Displayable {
    protected String title = "";
    protected Ticker ticker = null;
    protected CommandListener listener = null;
    protected final Vector commands = new Vector();

    public Displayable() { }

    public void setTitle(String t) { title = t == null ? "" : t; }
    public String getTitle() { return title; }

    public void setTicker(Ticker t) { ticker = t; }
    public Ticker getTicker() { return ticker; }

    public void addCommand(Command c) {
        if (c != null && !commands.contains(c)) { commands.addElement(c); }
    }

    public void removeCommand(Command c) {
        if (c != null) { commands.removeElement(c); }
    }

    public void setCommandListener(CommandListener l) { listener = l; }

    public CommandListener getCommandListener() { return listener; }

    public int getWidth() { return 240; }
    public int getHeight() { return 320; }

    public boolean isShown() { return Display.isCurrent(this); }

    protected void touch() { Display.touch(); }
}