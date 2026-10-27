package javax.microedition.lcdui;

public abstract class Canvas extends Displayable {
    public static final int UP = 1, DOWN = 6, LEFT = 2, RIGHT = 5, FIRE = 8;
    public static final int GAME_A = 9, GAME_B = 10, GAME_C = 11, GAME_D = 12;
    public static final int KEY_NUM0 = 48, KEY_NUM1 = 49, KEY_NUM2 = 50, KEY_NUM3 = 51,
        KEY_NUM4 = 52, KEY_NUM5 = 53, KEY_NUM6 = 54, KEY_NUM7 = 55, KEY_NUM8 = 56, KEY_NUM9 = 57;
    public static final int KEY_POUND = 35, KEY_STAR = 42;

    protected Canvas() { }

    public void repaint() { Display.repaintCanvas(this); }
    public void repaint(int x, int y, int w, int h) { Display.repaintCanvas(this); }
    public void serviceRepaints() { Display.flushCanvas(this); }

    public void setFullScreenMode(boolean mode) { }

    protected abstract void paint(Graphics g);

    public boolean hasPointerEvents() { return true; }
    public boolean hasPointerMotionEvents() { return true; }
    public boolean hasRepeatEvents() { return true; }
    public boolean isDoubleBuffered() { return true; }

    protected void sizeChanged(int w, int h) { }
    protected void showNotify() { }
    protected void hideNotify() { }

    protected void keyPressed(int keyCode) { }
    protected void keyReleased(int keyCode) { }
    protected void keyRepeated(int keyCode) { }

    protected void pointerPressed(int x, int y) { }
    protected void pointerReleased(int x, int y) { }
    protected void pointerDragged(int x, int y) { }

    public int getGameAction(int keyCode) {
        if (keyCode == UP || keyCode == 'w' || keyCode == 'W' || keyCode == KEY_NUM2) { return UP; }
        if (keyCode == DOWN || keyCode == 's' || keyCode == 'S' || keyCode == KEY_NUM8) { return DOWN; }
        if (keyCode == LEFT || keyCode == 'a' || keyCode == 'A' || keyCode == KEY_NUM4) { return LEFT; }
        if (keyCode == RIGHT || keyCode == 'd' || keyCode == 'D' || keyCode == KEY_NUM6) { return RIGHT; }
        if (keyCode == FIRE || keyCode == ' ' || keyCode == '\n' || keyCode == KEY_NUM5) { return FIRE; }
        if (keyCode == GAME_A || keyCode == 'q' || keyCode == 'Q' || keyCode == KEY_NUM1) { return GAME_A; }
        if (keyCode == GAME_B || keyCode == 'e' || keyCode == 'E' || keyCode == KEY_NUM3) { return GAME_B; }
        if (keyCode == GAME_C || keyCode == 'z' || keyCode == 'Z' || keyCode == KEY_NUM7) { return GAME_C; }
        if (keyCode == GAME_D || keyCode == 'c' || keyCode == 'C' || keyCode == KEY_NUM9) { return GAME_D; }
        return 0;
    }

    public int getKeyCode(int gameAction) {
        if (gameAction == UP) { return UP; }
        if (gameAction == DOWN) { return DOWN; }
        if (gameAction == LEFT) { return LEFT; }
        if (gameAction == RIGHT) { return RIGHT; }
        if (gameAction == FIRE) { return FIRE; }
        if (gameAction == GAME_A) { return GAME_A; }
        if (gameAction == GAME_B) { return GAME_B; }
        if (gameAction == GAME_C) { return GAME_C; }
        if (gameAction == GAME_D) { return GAME_D; }
        return 0;
    }

    public String getKeyName(int keyCode) {
        if (keyCode == UP) { return "Up"; }
        if (keyCode == DOWN) { return "Down"; }
        if (keyCode == LEFT) { return "Left"; }
        if (keyCode == RIGHT) { return "Right"; }
        if (keyCode == FIRE) { return "Select"; }
        if (keyCode == KEY_STAR) { return "*"; }
        if (keyCode == KEY_POUND) { return "#"; }
        if (keyCode >= KEY_NUM0 && keyCode <= KEY_NUM9) { return String.valueOf((char) keyCode); }
        if (keyCode >= 32 && keyCode < 127) { return String.valueOf((char) keyCode); }
        return "key" + keyCode;
    }
}
