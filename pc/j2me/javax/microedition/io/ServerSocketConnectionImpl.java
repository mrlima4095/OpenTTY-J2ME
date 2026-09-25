package javax.microedition.io;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketConnectionImpl implements ServerSocketConnection {
    final ServerSocket server;

    public ServerSocketConnectionImpl(int port) throws IOException {
        this.server = new ServerSocket(port);
    }

    public SocketConnection acceptAndOpen() throws IOException {
        Socket s = server.accept();
        return new SocketConnectionImpl(s);
    }

    public String getLocalAddress() {
        return server.getInetAddress() != null ? server.getInetAddress().getHostAddress() : "0.0.0.0";
    }
    public int getLocalPort() { return server.getLocalPort(); }
    public void close() throws IOException { server.close(); }
}