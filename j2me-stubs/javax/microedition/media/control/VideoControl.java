package javax.microedition.media.control;
public interface VideoControl extends Control {
    int ANCHOR_TOP = 1;
    int ANCHOR_BOTTOM = 2;
    int ANCHOR_LEFT = 4;
    int ANCHOR_RIGHT = 8;
    int ANCHOR_TOP_LEFT = 5;
    int ANCHOR_TOP_RIGHT = 9;
    int ANCHOR_BOTTOM_LEFT = 6;
    int ANCHOR_BOTTOM_RIGHT = 10;
    int ANCHOR_BASELINE = 64;
    void setDisplayLocation(int x, int y) throws javax.microedition.media.MediaException;
    void setDisplaySize(int width, int height) throws javax.microedition.media.MediaException;
    void setDisplayFullScreen(boolean fullScreenMode) throws javax.microedition.media.MediaException;
    javax.microedition.lcdui.Displayable initDisplayMode(int mode, java.lang.Object displayable);
    void setVisible(boolean visible);
    byte[] getSnapshot(String imageType) throws javax.microedition.media.MediaException;
    int getSourceWidth();
    int getSourceHeight();
    int getDisplayX();
    int getDisplayY();
    int getDisplayWidth();
    int getDisplayHeight();
}
