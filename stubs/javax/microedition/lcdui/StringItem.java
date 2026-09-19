package javax.microedition.lcdui;

public class StringItem extends Item {
    public static final int HYPERLINK = Item.HYPERLINK;
    public static final int BUTTON = Item.BUTTON;

    private String text = "";
    private final int appearanceMode;

    public StringItem(String label, String text) {
        this(label, text, Item.PLAIN);
    }

    public StringItem(String label, String text, int appearanceMode) {
        setLabel(label);
        this.text = text == null ? "" : text;
        this.appearanceMode = appearanceMode;
    }

    public String getText() { return text; }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        _touch();
    }

    public int getAppearanceMode() { return appearanceMode; }
}