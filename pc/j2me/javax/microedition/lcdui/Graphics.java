package javax.microedition.lcdui;

public class Graphics {
    public static final int TOP = 0, BOTTOM = 32, LEFT = 0, RIGHT = 4, HCENTER = 1, VCENTER = 2, BASELINE = 64;
    public static final int SOLID = 0, DOTTED = 1;

    private final java.awt.Graphics2D g2;
    private Font font = Font.getDefaultFont();
    private final java.awt.FontMetrics metrics;
    private int translateX = 0, translateY = 0;

    public Graphics(java.awt.Graphics2D g2) { this.g2 = g2; this.metrics = g2.getFontMetrics(); }

    public void setColor(int rgb) { g2.setColor(new java.awt.Color(rgb)); }
    public void setColor(int r, int g, int b) { g2.setColor(new java.awt.Color(r, g, b)); }
    public void setFont(Font f) { if (f != null) { font = f; g2.setFont(f.awt()); } }
    public Font getFont() { return font; }
    public void setStrokeStyle(int style) { }
    public int getStrokeStyle() { return SOLID; }

    private int anchorX(int x, int width, int anchor) {
        if ((anchor & RIGHT) != 0) { return x - width; }
        if ((anchor & HCENTER) != 0) { return x - width / 2; }
        return x;
    }

    private int anchorY(int y, int height, int anchor) {
        if ((anchor & BOTTOM) != 0) { return y - height; }
        if ((anchor & VCENTER) != 0) { return y - height / 2; }
        if ((anchor & BASELINE) != 0) { return y - g2.getFontMetrics().getAscent(); }
        return y;
    }

    public void drawString(String s, int x, int y, int anchor) {
        String text = s == null ? "" : s;
        java.awt.FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, anchorX(x, fm.stringWidth(text), anchor), anchorY(y, fm.getHeight(), anchor));
    }

    public void drawSubstring(String s, int off, int len, int x, int y, int anchor) { drawString(s.substring(off, off + len), x, y, anchor); }
    public void drawChar(char c, int x, int y, int anchor) { drawString(String.valueOf(c), x, y, anchor); }
    public void drawChars(char[] data, int off, int len, int x, int y, int anchor) { drawString(new String(data, off, len), x, y, anchor); }

    public void drawLine(int x1, int y1, int x2, int y2) { g2.drawLine(x1, y1, x2, y2); }
    public void fillRect(int x, int y, int w, int h) { g2.fillRect(x, y, w, h); }
    public void drawRect(int x, int y, int w, int h) { g2.drawRect(x, y, w, h); }
    public void fillRoundRect(int x, int y, int w, int h, int r1, int r2) { g2.fillRoundRect(x, y, w, h, r1, r2); }
    public void drawRoundRect(int x, int y, int w, int h, int r1, int r2) { g2.drawRoundRect(x, y, w, h, r1, r2); }
    public void fillArc(int x, int y, int w, int h, int a1, int a2) { g2.fillArc(x, y, w, h, a1, a2); }
    public void drawArc(int x, int y, int w, int h, int a1, int a2) { g2.drawArc(x, y, w, h, a1, a2); }

    public void drawImage(Image img, int x, int y, int anchor) {
        if (img == null || img.awt() == null) { return; }
        int w = img.awt().getWidth(null), h = img.awt().getHeight(null);
        g2.drawImage(img.awt(), anchorX(x, w, anchor), anchorY(y, h, anchor), null);
    }

    public void translate(int x, int y) { g2.translate(x, y); translateX += x; translateY += y; }
    public int getTranslateX() { return translateX; }
    public int getTranslateY() { return translateY; }
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
