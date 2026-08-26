package javax.microedition.lcdui;
public class StringItem extends Item {
    public static final int PLAIN = 0;
    public static final int HYPERLINK = 1;
    public static final int BUTTON = 2;
    public StringItem(String label, String text) {}
    public StringItem(String label, String text, int appearanceMode) {}
    public void setText(String text) {}
    public String getText() { return null; }
    public void setFont(Font font) {}
    public void setLayout(int layout) {}
    public void addCommand(Command cmd) {}
    public void setDefaultCommand(Command cmd) {}
    public void setItemCommandListener(ItemCommandListener l) {}
}
