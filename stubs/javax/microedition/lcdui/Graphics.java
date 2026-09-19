package javax.microedition.lcdui;

/**
 * Minimal desktop stub of javax.microedition.lcdui.Graphics.
 * OpenTTY does not paint directly; this exists for API completeness.
 */
public class Graphics {
    public static final int TOP = 0;
    public static final int LEFT = 0;
    public static final int HCENTER = 1;
    public static final int RIGHT = 2;
    public static final int VCENTER = 4;
    public static final int BOTTOM = 8;
    public static final int BASELINE = 16;

    public static final int SOLID = 0;
    public static final int DOTTED = 1;

    public static final int DASHED = 2;

    public static final int HOLLOW = 0;
    public static final int FILLED = 1;

    private Image target;

    Graphics(Image target) { this.target = target; }

    public int getTranslateX() { return 0; }
    public int getTranslateY() { return 0; }
    public void translate(int x, int y) { }
    public int getClipX() { return 0; }
    public int getClipY() { return 0; }
    public int getClipWidth() { return target == null ? 0 : target.getWidth(); }
    public int getClipHeight() { return target == null ? 0 : target.getHeight(); }
    public void clipRect(int x, int y, int width, int height) { }
    public void setClip(int x, int y, int width, int height) { }
    public void setColor(int RGB) { }
    public void setColor(int red, int green, int blue) { }
    public int getColor() { return 0; }
    public int getRedComponent() { return 0; }
    public int getGreenComponent() { return 0; }
    public int getBlueComponent() { return 0; }
    public void setGrayScale(int value) { }
    public int getGrayScale() { return 0; }
    public int getPixel(int x, int y) { return 0; }
    public void drawLine(int x1, int y1, int x2, int y2) { }
    public void fillRect(int x, int y, int width, int height) { }
    public void drawRect(int x, int y, int width, int height) { }
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { }
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) { }
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) { }
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) { }
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) { }
    public void drawString(String str, int x, int y, int anchor) { }
    public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) { }
    public void drawChar(char character, int x, int y, int anchor) { }
    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) { }
    public void drawImage(Image img, int x, int y, int anchor) { }
    public void setFont(Font font) { }
    public Font getFont() { return Font.getDefaultFont(); }
    public int getStrokeStyle() { return SOLID; }
    public void setStrokeStyle(int style) { }
}