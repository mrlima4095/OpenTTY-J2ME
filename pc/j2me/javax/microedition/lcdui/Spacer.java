package javax.microedition.lcdui;

public class Spacer extends Item {
    private int minWidth_;
    private int minHeight_;

    public Spacer(int minWidth, int minHeight) {
        minWidth_ = Math.max(minWidth, 0);
        minHeight_ = Math.max(minHeight, 0);
    }

    public void setMinimumSize(int minWidth, int minHeight) {
        minWidth_ = Math.max(minWidth, 0);
        minHeight_ = Math.max(minHeight, 0);
        touch();
    }

    public int getMinimumWidth() { return minWidth_; }

    public int getMinimumHeight() { return minHeight_; }
}