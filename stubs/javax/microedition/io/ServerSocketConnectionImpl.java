package javax.microedition.io;

import java.io.IOException;
import java.net.ServerSocket;

/** ServerSocketConnection backed by java.net.ServerSocket. */
final class ServerSocketConnectionImpl implements ServerSocketConnection {
    private final ServerSocket server;

    ServerSocketConnectionImpl(int port) throws IOException {
        server = new ServerSocket(port);
    }

    public void open() throws IOException { }

    public void close() throws IOException {
        server.close();
    }

    public String getLocalAddress() throws IOException {
        return server.getInetAddress() == null ? "" : server.getInetAddress().getHostAddress();
    }

    public int getLocalPort() throws IOException { return server.getLocalPort(); }

    public StreamConnection acceptAndOpen() throws IOException {
        return new SocketConnectionImpl(server.accept());
    }
}