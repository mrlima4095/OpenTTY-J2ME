package javax.microedition.lcdui;

public class AlertType {
    public static final AlertType INFO = new AlertType();
    public static final AlertType WARNING = new AlertType();
    public static final AlertType ERROR = new AlertType();
    public static final AlertType ALARM = new AlertType();
    public static final AlertType CONFIRMATION = new AlertType();

    private AlertType() { }

    /** Returns true if this AlertType plays a sound on the current device. */
    public boolean playSound(Display display) { return false; }
}