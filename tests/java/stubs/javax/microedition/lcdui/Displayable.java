package javax.microedition.lcdui;
public abstract class Displayable {
    public void addCommand(Command cmd) { }
    public void setCommandListener(CommandListener l) { }
    public void setTitle(String s) { }
    public String getTitle() { return null; }
}
