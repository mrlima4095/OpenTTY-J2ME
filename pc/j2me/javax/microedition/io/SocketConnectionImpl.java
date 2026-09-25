package javax.microedition.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class SocketConnectionImpl implements SocketConnection {
    final Socket socket;

    public SocketConnectionImpl(Socket socket) { this.socket = socket; }
    public SocketConnectionImpl(String host, int port) throws IOException {
        this.socket = new Socket(host, port);
    }

    public String getAddress() { return socket.getInetAddress() != null ? socket.getInetAddress().getHostAddress() : "0.0.0.0"; }
    public String getLocalAddress() { return socket.getLocalAddress() != null ? socket.getLocalAddress().getHostAddress() : "0.0.0.0"; }
    public int getPort() { return socket.getPort(); }
    public int getLocalPort() { return socket.getLocalPort(); }

    public byte getSocketOption(byte option) { return 0; }
    public void setSocketOption(byte option, byte value) { }

    public InputStream openInputStream() throws IOException { return socket.getInputStream(); }
    public DataInputStream openDataInputStream() throws IOException { return new DataInputStream(socket.getInputStream()); }
    public OutputStream openOutputStream() throws IOException { return socket.getOutputStream(); }
    public DataOutputStream openDataOutputStream() throws IOException { return new DataOutputStream(socket.getOutputStream()); }

    public void close() throws IOException { socket.close(); }
}
