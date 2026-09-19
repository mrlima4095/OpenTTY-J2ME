package javax.microedition.lcdui;

public class Spacer extends Item {
    private int minWidth;
    private int minHeight;

    public Spacer(int minWidth, int minHeight) {
        this.minWidth = Math.max(0, minWidth);
        this.minHeight = Math.max(0, minHeight);
    }

    public void setMinimumSize(int minWidth, int minHeight) {
        this.minWidth = Math.max(0, minWidth);
        this.minHeight = Math.max(0, minHeight);
    }

    public int getMinimumWidth() { return minWidth; }
    public int getMinimumHeight() { return minHeight; }
}