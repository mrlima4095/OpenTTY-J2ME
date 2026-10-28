package javax.microedition.lcdui;
public class Display {
    private Displayable current;
    public void setCurrent(Displayable d) { current = d; }
    public void setCurrent(Alert alert, Displayable next) { current = next == null ? alert : next; }
    public Displayable getCurrent() { return current; }
    public void vibrate(int millis) { }
    public void callSerially(Runnable runnable) { runnable.run(); }
}
