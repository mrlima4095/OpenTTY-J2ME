package javax.microedition.io;

import java.io.IOException;

public interface SocketConnection extends StreamConnection {
    String getAddress() throws IOException;
    String getLocalAddress() throws IOException;
    int getPort() throws IOException;
    int getLocalPort() throws IOException;
    void setSocketOption(byte option, int value) throws IOException;
    int getSocketOption(byte option) throws IOException;

    byte DELAY = 0;
    byte LINGER = 1;
    byte KEEPALIVE = 2;
    byte RCVBUF = 3;
    byte SNDBUF = 4;
}