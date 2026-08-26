package javax.microedition.io;
public interface SocketConnection extends StreamConnection {
    String getAddress() throws java.io.IOException;
    int getPort() throws java.io.IOException;
    String getLocalAddress() throws java.io.IOException;
    int getLocalPort() throws java.io.IOException;
    void setSocketOption(byte option, int value) throws java.io.IOException;
    byte getSocketOption(byte option) throws java.io.IOException;
}
