package javax.microedition.media;
import java.io.InputStream;
public class Manager {
    public static Player createPlayer(String protocol, String type) throws MediaException { return null; }
    public static Player createPlayer(InputStream stream, String type) throws MediaException { return null; }
    public static String[] getSupportedContentTypes(String protocol) { return null; }
    public static String[] getSupportedProtocols(String contentLocator) { return null; }
}
