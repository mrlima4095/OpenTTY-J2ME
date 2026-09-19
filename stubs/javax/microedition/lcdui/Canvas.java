package javax.microedition.lcdui;

/**
 * Empty desktop stub of javax.microedition.lcdui.Canvas. Only used for an
 * `instanceof Canvas` type check inside Lua.java.
 */
public abstract class Canvas extends Displayable {
    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;

    public static final int KEY_POUND = 35;
    public static final int KEY_STAR = 42;
    public static final int KEY_NUM0 = 48;
    public static final int KEY_NUM1 = 49;
    public static final int KEY_NUM2 = 50;
    public static final int KEY_NUM3 = 51;
    public static final int KEY_NUM4 = 52;
    public static final int KEY_NUM5 = 53;
    public static final int KEY_NUM6 = 54;
    public static final int KEY_NUM7 = 55;
    public static final int KEY_NUM8 = 56;
    public static final int KEY_NUM9 = 57;
    public static final int KEY_UP = -1;
    public static final int KEY_DOWN = -2;
    public static final int KEY_LEFT = -3;
    public static final int KEY_RIGHT = -4;
    public static final int KEY_SOFTKEY1 = -6;
    public static final int KEY_SOFTKEY2 = -7;
    public static final int KEY_CLEAR = -8;
    public static final int KEY_SEND = -10;
    public static final int KEY_END = -11;
    public static final int KEY_PAUSE = -13;
    public static final int KEY_SOFTKEY3 = -14;
    public static final int KEY_BACK = -29;

    public static int keyName(int keyCode) { return 0; }
    public static int getGameAction(int keyCode) { return 0; }
    public static int getKeyCode(int gameAction) { return gameAction; }

    public void setFullScreenMode(boolean mode) { }
    public Graphics getGraphics() { return null; }
    public void repaint() { }
    public void repaint(int x, int y, int width, int height) { }
    public void serviceRepaints() { }
    public boolean hasPointerEvents() { return true; }
    public boolean hasPointerMotionEvents() { return true; }
    public boolean hasRepeatEvents() { return true; }
    public void setPaintNotify() { }
}