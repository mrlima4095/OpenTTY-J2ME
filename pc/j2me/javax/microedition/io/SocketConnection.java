package javax.microedition.io;

public interface SocketConnection extends StreamConnection {
    byte DELAY = 0, LINGER = 1, KEEPALIVE = 2, RCVBUF = 3, SNDBUF = 4, BUF_SIZE = 1, TTL = 5;
    String getAddress();
    String getLocalAddress();
    int getPort();
    int getLocalPort();
    byte getSocketOption(byte option) throws IllegalArgumentException, java.io.IOException;
    void setSocketOption(byte option, byte value) throws IllegalArgumentException, java.io.IOException;
}
