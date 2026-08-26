package javax.microedition.lcdui;
import java.io.InputStream;
public class Image {
    public static Image createImage(String name) throws Exception { return null; }
    public static Image createImage(int width, int height) { return null; }
    public static Image createImage(Image source) { return null; }
    public static Image createImage(InputStream stream) throws Exception { return null; }
    public static Image createImage(byte[] imageData, int imageOffset, int imageLength) { return null; }
    public Graphics getGraphics() { return null; }
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public boolean isMutable() { return false; }
}
