package javax.microedition.lcdui;

public class StringItem extends Item {
    public static final int HYPERLINK = 0x200;
    public static final int BUTTON = 0x400;

    protected String text = "";
    protected int layout = LAYOUT_DEFAULT;
    protected Font font = null;

    public StringItem(String label, String text) {
        this.label = label == null ? "" : label;
        this.text = text == null ? "" : text;
    }

    public StringItem(String label, String text, int appearanceMode) {
        this(label, text);
        this.layout = appearanceMode;
    }

    public String getText() { return text; }
    public void setText(String t) { text = t == null ? "" : t; touch(); }
    public void setAppearanceMode(int mode) { layout = mode; }
    public void setFont(Font f) { font = f; }
    public Font getFont() { return font != null ? font : Font.getDefaultFont(); }
}
