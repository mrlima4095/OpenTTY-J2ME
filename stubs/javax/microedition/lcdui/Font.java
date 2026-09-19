package javax.microedition.lcdui;

import java.util.Hashtable;

/**
 * Desktop stub of javax.microedition.lcdui.Font backed by a java.awt.Font.
 */
public class Font {
    public static final int FACE_SYSTEM = 0;
    public static final int FACE_MONOSPACE = 32;
    public static final int FACE_PROPORTIONAL = 64;

    public static final int STYLE_PLAIN = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_UNDERLINED = 4;

    public static final int SIZE_SMALL = 8;
    public static final int SIZE_MEDIUM = 0;
    public static final int SIZE_LARGE = 16;

    private static final Font DEFAULT = new Font(FACE_SYSTEM, STYLE_PLAIN, SIZE_MEDIUM);
    private static final Hashtable CACHE = new Hashtable();

    private final int face, style, size;

    private Font(int face, int style, int size) {
        this.face = face;
        this.style = style;
        this.size = size;
    }

    public static Font getDefaultFont() { return DEFAULT; }

    public static Font getFont(int face, int style, int size) {
        Integer key = new Integer(face * 1024 + style * 32 + (size & 0xFF));
        Font f = (Font) CACHE.get(key);
        if (f == null) { f = new Font(face, style, size); CACHE.put(key, f); }
        return f;
    }

    public int getFace() { return face; }
    public int getStyle() { return style; }
    public int getSize() { return size; }

    public boolean isPlain() { return style == STYLE_PLAIN; }
    public boolean isBold() { return (style & STYLE_BOLD) != 0; }
    public boolean isItalic() { return (style & STYLE_ITALIC) != 0; }
    public boolean isUnderlined() { return (style & STYLE_UNDERLINED) != 0; }

    public int getHeight() { return _awt().getSize(); }
    public int getBaselinePosition() { return _awt().getSize(); }
    public int stringWidth(String str) { return _awt() == null || str == null ? 0 : SwingFont.metrics(_awt()).stringWidth(str); }
    public int charsWidth(char[] ch, int offset, int length) {
        java.awt.FontMetrics fm = SwingFont.metrics(_awt());
        return fm == null ? 0 : fm.charsWidth(ch, offset, length);
    }
    public int charWidth(char ch) {
        java.awt.FontMetrics fm = SwingFont.metrics(_awt());
        return fm == null ? 0 : fm.charWidth(ch);
    }
    public int substringWidth(String str, int startOffset, int length) {
        if (str == null) { return 0; }
        String s = str.substring(startOffset, startOffset + length);
        return stringWidth(s);
    }

    /** Maps this font to a java.awt.Font for the Swing renderer. */
    java.awt.Font _awt() {
        String family = (face == FACE_MONOSPACE) ? java.awt.Font.MONOSPACED
            : java.awt.Font.SANS_SERIF;
        int awtStyle = java.awt.Font.PLAIN;
        if (isBold() && isItalic()) { awtStyle = java.awt.Font.BOLD | java.awt.Font.ITALIC; }
        else if (isBold()) { awtStyle = java.awt.Font.BOLD; }
        else if (isItalic()) { awtStyle = java.awt.Font.ITALIC; }
        int px = (size == SIZE_SMALL) ? 11 : (size == SIZE_LARGE) ? 18 : 13;
        return new java.awt.Font(family, awtStyle, px);
    }

    /** Small holder so Font does not import Swing directly. */
    static final class SwingFont {
        static java.awt.FontMetrics metrics(java.awt.Font f) {
            try { return new javax.swing.JLabel().getFontMetrics(f); }
            catch (Throwable t) { return null; }
        }
    }
}