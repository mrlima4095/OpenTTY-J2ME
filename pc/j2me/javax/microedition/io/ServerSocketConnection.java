package javax.microedition.io;

public interface ServerSocketConnection extends StreamConnectionNotifier {
    String getLocalAddress();
    int getLocalPort();
}
