package javax.microedition.media;

import java.io.IOException;
import java.io.InputStream;

/** Manager (subset of JSR-135): creates Players from streams. */
public abstract class Manager {
    public static final String TONE_DEVICE_LOCATOR = "device://tone";

    public static Player createPlayer(String locator) throws IOException, MediaException {
        if (locator == null) { throw new IllegalArgumentException("Null locator"); }
        if (locator.equalsIgnoreCase(TONE_DEVICE_LOCATOR)) {
            return new DesktopPlayer(new byte[0]);
        }
        throw new MediaException("Unsupported locator: " + locator);
    }

    public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
        if (stream == null) { throw new IllegalArgumentException("Null stream"); }
        byte[] data = readAll(stream);
        return new DesktopPlayer(data);
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) { bos.write(buf, 0, n); }
        return bos.toByteArray();
    }

    public static long getTimeBase(Player player) { return System.currentTimeMillis() * 1000L; }
    private Manager() { }
}