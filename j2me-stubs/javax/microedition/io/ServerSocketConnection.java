package javax.microedition.io;
public interface ServerSocketConnection extends StreamConnectionNotifier {
    int getLocalPort() throws java.io.IOException;
}
