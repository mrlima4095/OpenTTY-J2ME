package javax.microedition.lcdui;

public class Alert extends Displayable {
    public static final int FOREVER = -2;
    public static final Command DISMISS_COMMAND = new Command("OK", Command.OK, 0);

    private String text = "";
    private Image image = null;
    private AlertType type;
    private int timeout = FOREVER;
    private Gauge indicator = null;

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

    // MIDP 2.0: the indicator is one non-interactive gauge shown in the alert.
    public Gauge getIndicator() { return indicator; }

    public void setIndicator(Gauge g) {
        if (g != null && g.isInteractive()) {
            throw new IllegalArgumentException("interactive gauge cannot be an alert indicator");
        }
        indicator = g;
    }
}
