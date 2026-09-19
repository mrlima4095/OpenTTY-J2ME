package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/** SocketConnection backed by java.net.Socket. */
final class SocketConnectionImpl implements SocketConnection {
    private final Socket socket;
    private boolean closed = false;

    SocketConnectionImpl(String host, int port) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 15000);
    }

    SocketConnectionImpl(Socket socket) {
        this.socket = socket;
    }

    public void open() throws IOException { }

    public void close() throws IOException {
        if (closed) { return; }
        closed = true;
        socket.close();
    }

    public String getAddress() throws IOException {
        return socket.getInetAddress() == null ? ""
            : socket.getInetAddress().getHostAddress();
    }

    public String getLocalAddress() throws IOException {
        return socket.getLocalAddress() == null ? "" : socket.getLocalAddress().getHostAddress();
    }

    public int getPort() throws IOException { return socket.getPort(); }
    public int getLocalPort() throws IOException { return socket.getLocalPort(); }

    public void setSocketOption(byte option, int value) throws IOException {
        switch (option) {
            case KEEPALIVE: socket.setKeepAlive(value != 0); break;
            case DELAY: socket.setTcpNoDelay(value != 0); break;
            case LINGER: socket.setSoLinger(value != 0, value); break;
            case RCVBUF: socket.setReceiveBufferSize(value); break;
            case SNDBUF: socket.setSendBufferSize(value); break;
            default: throw new IllegalArgumentException("Bad option");
        }
    }

    public int getSocketOption(byte option) throws IOException {
        switch (option) {
            case KEEPALIVE: return socket.getKeepAlive() ? 1 : 0;
            case DELAY: return socket.getTcpNoDelay() ? 1 : 0;
            case SNDBUF: return socket.getSendBufferSize();
            case RCVBUF: return socket.getReceiveBufferSize();
            case LINGER: return socket.getSoLinger();
            default: throw new IllegalArgumentException("Bad option");
        }
    }

    public InputStream openInputStream() throws IOException {
        return socket.getInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        return socket.getOutputStream();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }
}