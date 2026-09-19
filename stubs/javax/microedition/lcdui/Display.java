package javax.microedition.lcdui;

import javax.microedition.midlet.MIDlet;

public class Display {
    private static Display instance = null;

    private Displayable current = null;
    private Displayable alertFallback = null;

    private Display(MIDlet m) { }

    public static Display getDisplay(MIDlet m) {
        if (instance == null) { instance = new Display(m); }
        return instance;
    }

    public static Display getDisplay() {
        return instance;
    }

    public void setCurrent(Displayable next) {
        if (next == null) { return; }
        Displayable old = current;
        current = next;
        next.display = this;
        next.shown = true;
        if (next instanceof Alert) {
            Alert alert = (Alert) next;
            if (alert.alertNext == null && old != null && !(old instanceof Alert)) {
                alert.alertNext = old;   // implicit "back" for warn()/single-alert usage
            }
        }
        DesktopLcdui._show(next);
    }

    public void setCurrent(Alert alert, Displayable next) {
        if (alert == null) { return; }
        alert.alertNext = next;
        setCurrent((Displayable) alert);
    }

    public Displayable getCurrent() { return current; }

    public void vibrate(int duration) { }

    public void flashBacklight(int duration) { }

    public boolean isColor() { return true; }

    public int numColors() { return 65536; }

    public int numAlphaLevels() { return 256; }

    public int getBestImageWidth(int imageType) { return 256; }

    public int getBestImageHeight(int imageType) { return 256; }

    public void setCurrentItem(Item item) { }

    static int _screenWidth() { return 480; }
    static int _screenHeight() { return 720; }

    /**
     * Invoked by the renderer when a command fires on a screen. Routes the
     * command to the CommandListener and implements Alert auto-dismiss
     * (setCurrent(Alert, next) semantics).
     */
    void _fireCommand(Displayable d, Command c) {
        d._fire(c);
        if (current == d && d instanceof Alert) {
            Alert alert = (Alert) d;
            Displayable next = alert.alertNext;
            alert.alertNext = null;
            if (next != null) { setCurrent(next); }
            else if (alertFallback != null) { Displayable fb = alertFallback; alertFallback = null; setCurrent(fb); }
        }
    }

    /**
     * Alerts shown via setCurrent(Alert) remember the previous screen so a
     * plain dismiss (e.g. warn()) can return to it.
     */
    void _showAlert(Alert alert) {
        alertFallback = (current != null && !(current instanceof Alert)) ? current : alertFallback;
    }

    /** Called when the window is closed by the user. */
    void _windowClosed() {
        MIDlet midlet = MIDlet._active();
        if (midlet != null) {
            try { midlet.destroyApp(true); } catch (Throwable t) { }
        }
    }
}