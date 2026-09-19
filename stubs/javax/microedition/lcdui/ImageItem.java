package javax.microedition.lcdui;

public class ImageItem extends Item {
    public static final int LAYOUT_DEFAULT = Item.LAYOUT_DEFAULT;
    public static final int LAYOUT_LEFT = Item.LAYOUT_LEFT;
    public static final int LAYOUT_RIGHT = Item.LAYOUT_RIGHT;
    public static final int LAYOUT_CENTER = Item.LAYOUT_CENTER;
    public static final int LAYOUT_NEWLINE_BEFORE = Item.LAYOUT_NEWLINE_BEFORE;
    public static final int LAYOUT_NEWLINE_AFTER = Item.LAYOUT_NEWLINE_AFTER;

    private Image image;

    public ImageItem(String label, Image img, int layout, String altText) {
        setLabel(label);
        this.image = img;
        setLayout(layout);
    }

    public Image getImage() { return image; }

    public void setImage(Image img) {
        this.image = img;
        _touch();
    }

    public String getAltText() { return ""; }

    public void setAltText(String s) { }
}