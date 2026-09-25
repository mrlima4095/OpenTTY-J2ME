package javax.microedition.lcdui;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class Image {
    private final java.awt.Image awt;
    private final byte[] raw;

    private Image(java.awt.Image awt, byte[] raw) { this.awt = awt; this.raw = raw; }

    public static Image createImage(int width, int height) {
        return new Image(new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB), null);
    }

    public static Image createImage(String name) {
        if (name == null) { return createImage(1, 1); }
        try {
            InputStream is = Image.class.getClassLoader().getResourceAsStream(name.startsWith("/") ? name.substring(1) : name);
            if (is != null) { return createImage(is); }
        } catch (Throwable t) { }
        return createImage(1, 1);
    }

    public static Image createImage(InputStream stream) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096]; int n;
            while ((n = stream.read(buf)) != -1) { out.write(buf, 0, n); }
            byte[] data = out.toByteArray();
            java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().createImage(data);
            return new Image(img, data);
        } catch (Exception e) {
            return createImage(1, 1);
        }
    }

    public static Image createImage(byte[] data) {
        try {
            java.awt.Image img = java.awt.Toolkit.getDefaultToolkit().createImage(data);
            return new Image(img, data);
        } catch (Throwable t) { return createImage(1, 1); }
    }

    public static Image createImage(Image source) { return new Image(source.awt, source.raw); }

    public java.awt.Image awt() { return awt; }
    public byte[] getBytes() { return raw; }
    public int getWidth() { return awt != null ? awt.getWidth(null) : 1; }
    public int getHeight() { return awt != null ? awt.getHeight(null) : 1; }
}
