package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

/**
 * Desktop stub of javax.microedition.lcdui.Image backed by a BufferedImage.
 */
public class Image {
    private final BufferedImage buf;
    private final int width, height;

    private Image(int width, int height) {
        this.width = width;
        this.height = height;
        this.buf = width > 0 && height > 0 ? new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB) : null;
    }

    private Image(BufferedImage buf) {
        this.buf = buf;
        this.width = buf != null ? buf.getWidth() : 0;
        this.height = buf != null ? buf.getHeight() : 0;
    }

    public static Image createImage(int width, int height) {
        return new Image(width, height);
    }

    public static Image createImage(InputStream stream) throws IOException {
        if (stream == null) { throw new IOException("Null image stream"); }
        BufferedImage buf = ImageIO.read(stream);
        if (buf == null) { throw new IOException("Unsupported image format"); }
        return new Image(buf);
    }

    public static Image createImage(String name) throws IOException {
        return createImage(Image.class.getResourceAsStream(name));
    }

    public static Image createImage(byte[] data, int imageOffset, int imageLength) throws IOException {
        java.io.ByteArrayInputStream in = new java.io.ByteArrayInputStream(data, imageOffset, imageLength);
        return createImage(in);
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public Graphics getGraphics() {
        return new Graphics(this);
    }

    public static Image createImage(Image source) {
        if (source == null) { return createImage(16, 16); }
        BufferedImage b = new BufferedImage(source.width, source.height, BufferedImage.TYPE_INT_ARGB);
        if (source.buf != null) {
            b.getGraphics().drawImage(source.buf, 0, 0, null);
        }
        return new Image(b);
    }

    /** Scale a copy to the given maximum bounding box, keeping aspect ratio. */
    Image _scaled(int maxSize) {
        if (buf == null) { return this; }
        int w = width, h = height;
        if (w <= maxSize && h <= maxSize) { return this; }
        double scale = Math.min((double) maxSize / w, (double) maxSize / h);
        int nw = Math.max(1, (int) (w * scale));
        int nh = Math.max(1, (int) (h * scale));
        BufferedImage b = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        b.getGraphics().drawImage(buf, 0, 0, nw, nh, null);
        return new Image(b);
    }

    BufferedImage _buf() { return buf; }
}