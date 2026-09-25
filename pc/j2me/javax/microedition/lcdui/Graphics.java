package javax.microedition.lcdui;

public class Graphics {
    public static final int TOP = 0, BOTTOM = 32, LEFT = 0, RIGHT = 4, HCENTER = 1, VCENTER = 2, BASELINE = 64;
    public static final int SOLID = 0, DOTTED = 1;

    private final java.awt.Graphics2D g2;

    public Graphics(java.awt.Graphics2D g2) { this.g2 = g2; }

    public void setColor(int rgb) { g2.setColor(new java.awt.Color(rgb)); }
    public void setColor(int r, int g, int b) { g2.setColor(new java.awt.Color(r, g, b)); }
    public void setFont(Font f) { if (f != null) { g2.setFont(f.awt()); } }
    public Font getFont() { return Font.getDefaultFont(); }
    public void drawString(String s, int x, int y, int anchor) { g2.drawString(s == null ? "" : s, x, y); }
    public void drawSubstring(String s, int off, int len, int x, int y, int anchor) { g2.drawString(s.substring(off, off + len), x, y); }
    public void drawLine(int x1, int y1, int x2, int y2) { g2.drawLine(x1, y1, x2, y2); }
    public void fillRect(int x, int y, int w, int h) { g2.fillRect(x, y, w, h); }
    public void drawRect(int x, int y, int w, int h) { g2.drawRect(x, y, w, h); }
    public void fillRoundRect(int x, int y, int w, int h, int r1, int r2) { g2.fillRoundRect(x, y, w, h, r1, r2); }
    public void drawRoundRect(int x, int y, int w, int h, int r1, int r2) { g2.drawRoundRect(x, y, w, h, r1, r2); }
    public void fillArc(int x, int y, int w, int h, int a1, int a2) { g2.fillArc(x, y, w, h, a1, a2); }
    public void drawArc(int x, int y, int w, int h, int a1, int a2) { g2.drawArc(x, y, w, h, a1, a2); }
    public void drawImage(Image img, int x, int y, int anchor) { if (img != null && img.awt() != null) { g2.drawImage(img.awt(), x, y, null); } }
    public void drawChar(char c, int x, int y, int anchor) { g2.drawString(String.valueOf(c), x, y); }
    public void drawChars(char[] data, int off, int len, int x, int y, int anchor) { g2.drawString(new String(data, off, len), x, y); }
    public void translate(int x, int y) { g2.translate(x, y); }
    public void clipRect(int x, int y, int w, int h) { g2.clipRect(x, y, w, h); }
    public void setClip(int x, int y, int w, int h) { g2.setClip(x, y, w, h); }
    public int getClipX() { return g2.getClipBounds() != null ? g2.getClipBounds().x : 0; }
    public int getClipY() { return g2.getClipBounds() != null ? g2.getClipBounds().y : 0; }
    public int getClipWidth() { return g2.getClipBounds() != null ? g2.getClipBounds().width : 0; }
    public int getClipHeight() { return g2.getClipBounds() != null ? g2.getClipBounds().height : 0; }
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
        g2.fill(new java.awt.Polygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3));
    }
}
