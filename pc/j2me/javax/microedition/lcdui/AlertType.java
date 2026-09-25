package javax.microedition.lcdui;

public class AlertType {
    public static final AlertType INFO = new AlertType("info");
    public static final AlertType WARNING = new AlertType("warning");
    public static final AlertType ERROR = new AlertType("error");
    public static final AlertType ALARM = new AlertType("alarm");
    public static final AlertType CONFIRMATION = new AlertType("confirm");

    private final String name;
    private AlertType(String name) { this.name = name; }
    public boolean playSound(Display display) { return true; }
    public String toString() { return name; }
}
