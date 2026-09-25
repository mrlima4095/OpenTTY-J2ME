package javax.microedition.lcdui;

public class Alert extends Displayable {
    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);

    private String text = "";
    private Image image = null;
    private AlertType type;
    private int timeout = FOREVER;

    public Alert(String title) { this(title, null, null, AlertType.INFO); }

    public Alert(String title, String text, Image image, AlertType type) {
        this.title = title == null ? "" : title;
        this.text = text == null ? "" : text;
        this.image = image;
        this.type = type;
    }

    public void setTimeout(int t) { timeout = t; }
    public int getTimeout() { return timeout; }
    public String getString() { return text; }
    public void setString(String s) { text = s == null ? "" : s; }
    public Image getImage() { return image; }
    public void setImage(Image i) { image = i; }
    public AlertType getType() { return type; }
}
