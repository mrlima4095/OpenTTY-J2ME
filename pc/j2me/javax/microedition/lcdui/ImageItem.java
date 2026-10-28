package javax.microedition.lcdui;

public class ImageItem extends Item {
    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_LEFT = 1;
    public static final int LAYOUT_RIGHT = 2;
    public static final int LAYOUT_CENTER = 3;
    public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
    public static final int LAYOUT_NEWLINE_AFTER = 0x200;

    private Image image;
    private String altText;

    public ImageItem(String label, Image img, int layout, String altText) {
        this.label = label == null ? "" : label;
        this.image = img;
        this.altText = altText;
    }

    public Image getImage() { return image; }
    public void setImage(Image img) { image = img; touch(); }
    public String getAltText() { return altText; }
    public void setAltText(String text) { altText = text; }
}
