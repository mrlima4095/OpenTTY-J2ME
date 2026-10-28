package javax.microedition.lcdui;
public class StringItem extends Item {
    public static final int PLAIN = 0, HYPERLINK = 1, BUTTON = 2;
    private String text;
    public StringItem(String label, String text) { this(label, text, PLAIN); }
    public StringItem(String label, String text, int appearance) { this.label = label == null ? "" : label; this.text = text; }
    public void setText(String t) { text = t; }
    public String getText() { return text; }
    public void setFont(Font f) { font = f; }
    public Font getFont() { return font != null ? font : Font.getDefaultFont(); }
}
