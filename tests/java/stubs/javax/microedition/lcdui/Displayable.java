package javax.microedition.lcdui;
public abstract class Displayable {
    protected String title = "";
    protected Ticker ticker;
    public void addCommand(Command cmd) { }
    public void setCommandListener(CommandListener l) { }
    public void setTitle(String s) { title = s == null ? "" : s; }
    public String getTitle() { return title; }
    public void setTicker(Ticker t) { ticker = t; }
    public Ticker getTicker() { return ticker; }
}
