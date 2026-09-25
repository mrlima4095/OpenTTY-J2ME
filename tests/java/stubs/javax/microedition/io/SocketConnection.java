package javax.microedition.io;
public interface SocketConnection extends StreamConnection {
    String getAddress();
    int getPort();
}
