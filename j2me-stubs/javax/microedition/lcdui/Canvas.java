package javax.microedition.lcdui;
public class Canvas extends Displayable {
    public static final int UP = 1;
    public static final int DOWN = 6;
    public static final int LEFT = 2;
    public static final int RIGHT = 5;
    public static final int FIRE = 8;
    public static final int GAME_A = 9;
    public static final int GAME_B = 10;
    public static final int GAME_C = 11;
    public static final int GAME_D = 12;
    protected Canvas() {}
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public void repaint() {}
    public void repaint(int x, int y, int w, int h) {}
    public void serviceRepaints() {}
    public int getGameAction(int keyCode) { return 0; }
    protected void paint(Graphics g) {}
}
