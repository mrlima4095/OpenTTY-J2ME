package javax.microedition.lcdui;

public class Alert extends Displayable {
    public static final int FOREVER = -2;

    private String text = "";
    private Image image = null;
    private AlertType type = AlertType.INFO;
    private int timeout = FOREVER;

    Displayable alertNext = null;

    public Alert(String title) {
        setTitle(title);
    }

    public Alert(String title, String text, Image image, AlertType alertType) {
        setTitle(title);
        setString(text);
        this.image = image;
        this.type = alertType == null ? AlertType.INFO : alertType;
    }

    public String getString() { return text; }

    public void setString(String text) {
        this.text = text == null ? "" : text;
        _repaint();
    }

    public Image getImage() { return image; }

    public void setImage(Image image) {
        this.image = image;
        _repaint();
    }

    public AlertType getType() { return type; }

    public void setType(AlertType type) {
        this.type = type == null ? AlertType.INFO : type;
    }

    public int getTimeout() { return timeout; }

    public void setTimeout(int timeout) { this.timeout = timeout; }

    public boolean isTimeoutZero() { return timeout == 0; }
}