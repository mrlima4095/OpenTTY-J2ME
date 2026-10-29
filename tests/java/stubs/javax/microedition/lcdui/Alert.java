package javax.microedition.lcdui;
public class Alert extends Displayable {
    public static final int FOREVER = -2;
    public static final int DEFAULT_TIMEOUT = 2000;
    private Gauge indicator = null;
    public Alert(String title, String text, Image image, AlertType type) { }
    public void setTimeout(int t) { }
    public Gauge getIndicator() { return indicator; }
    public void setIndicator(Gauge g) {
        if (g != null && g.isInteractive()) { throw new IllegalArgumentException("interactive gauge"); }
        indicator = g;
    }
}
