package javax.microedition.lcdui;

public class Font {
    public static final int FACE_SYSTEM = 0, FACE_MONOSPACE = 32, FACE_PROPORTIONAL = 64;
    public static final int STYLE_PLAIN = 0, STYLE_BOLD = 1, STYLE_ITALIC = 2, STYLE_UNDERLINED = 4;
    public static final int SIZE_SMALL = 8, SIZE_MEDIUM = 0, SIZE_LARGE = 16;

    private final java.awt.Font awt;
    private final int face, style, size;

    private Font(java.awt.Font awt, int face, int style, int size) {
        this.awt = awt; this.face = face; this.style = style; this.size = size;
    }

    public static Font getDefaultFont() { return new Font(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12), FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM); }

    public static Font getFont(int face, int style, int size) {
        int awtStyle = (style & STYLE_BOLD) != 0 ? java.awt.Font.BOLD : java.awt.Font.PLAIN;
        if ((style & STYLE_ITALIC) != 0) { awtStyle |= java.awt.Font.ITALIC; }
        int pt = size == SIZE_LARGE ? 16 : size == SIZE_SMALL ? 10 : 12;
        String name = face == FACE_MONOSPACE ? "Monospaced" : "SansSerif";
        return new Font(new java.awt.Font(name, awtStyle, pt), face, style, size);
    }

    public java.awt.Font awt() { return awt; }
    public int getFace() { return face; }
    public int getStyle() { return style; }
    public int getSize() { return size; }
    public int getHeight() { return awt.getSize() + 4; }
    public int stringWidth(String s) { return s == null ? 0 : awt.getSize() * s.length() / 2; }
}
