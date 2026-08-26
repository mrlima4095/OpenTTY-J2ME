package javax.microedition.lcdui;
public class Graphics {
    public static final int HCENTER = 1;
    public static final int VCENTER = 2;
    public static final int LEFT = 4;
    public static final int RIGHT = 8;
    public static final int TOP = 16;
    public static final int BOTTOM = 32;
    public static final int BASELINE = 64;
    public static final int SOLID = 0;
    public static final int DOTTED = 1;
    public void setColor(int RGB) {}
    public void setColor(int red, int green, int blue) {}
    public void fillRect(int x, int y, int width, int height) {}
    public void setFont(Font font) {}
    public void drawLine(int x1, int y1, int x2, int y2) {}
    public void drawRect(int x, int y, int width, int height) {}
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {}
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {}
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {}
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {}
    public void drawString(String str, int x, int y, int anchor) {}
    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {}
    public void drawImage(Image img, int x, int y, int anchor) {}
    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {}
    public void translate(int x, int y) {}
    public void setClip(int x, int y, int width, int height) {}
    public int getClipX() { return 0; }
    public int getClipY() { return 0; }
    public int getClipWidth() { return 0; }
    public int getClipHeight() { return 0; }
}
