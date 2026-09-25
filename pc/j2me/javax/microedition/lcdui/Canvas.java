package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public static final int UP = 1, DOWN = 6, LEFT = 2, RIGHT = 5, FIRE = 8;
    public static final int GAME_A = 9, GAME_B = 10, GAME_C = 11, GAME_D = 12;
    public static final int KEY_NUM0 = 48, KEY_NUM1 = 49, KEY_NUM2 = 50, KEY_NUM3 = 51,
        KEY_NUM4 = 52, KEY_NUM5 = 53, KEY_NUM6 = 54, KEY_NUM7 = 55, KEY_NUM8 = 56, KEY_NUM9 = 57;
    public static final int KEY_POUND = 35, KEY_STAR = 42;

    protected Canvas() { }

    public void repaint() { touch(); }
    public void repaint(int x, int y, int w, int h) { touch(); }
    public void serviceRepaints() { }
    public void setFullScreenMode(boolean mode) { }
    protected abstract void paint(Graphics g);
    public boolean hasPointerEvents() { return false; }
    public boolean hasPointerMotionEvents() { return false; }
    public boolean hasRepeatEvents() { return false; }
    public void setCommandListener(CommandListener l) { super.setCommandListener(l); }
}
