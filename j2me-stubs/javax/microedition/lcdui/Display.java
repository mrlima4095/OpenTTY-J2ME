package javax.microedition.lcdui;
import javax.microedition.midlet.MIDlet;
public class Display {
    public static Display getDisplay(MIDlet m) { return null; }
    public void setCurrent(Displayable d) {}
    public void setCurrent(Alert a, Displayable d) {}
    public Displayable getCurrent() { return null; }
    public boolean vibrate(int duration) { return false; }
}
