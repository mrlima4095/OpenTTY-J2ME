package javax.microedition.io;

import java.io.IOException;

import javax.microedition.io.file.FileConnectionImpl;

/**
 * Desktop implementation of the Generic Connection Framework Connector.
 *
 * Supported schemes:
 *   http:// / https://  -> HttpConnection (java.net.HttpURLConnection)
 *   socket://host:port   -> SocketConnection (client)
 *   socket://:port       -> ServerSocketConnection (bind)
 *   datagram://[:port]   -> DatagramConnection (UDP)
 *   file:///path         -> FileConnection (sandbox-relative)
 */
public final class Connector {
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;

    private Connector() { }

    public static Connection open(String name) throws IOException {
        return open(name, READ_WRITE);
    }

    public static Connection open(String name, int mode) throws IOException {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("Null or empty connection name");
        }
        if (mode != READ && mode != WRITE && mode != READ_WRITE) {
            throw new IllegalArgumentException("Invalid mode: " + mode);
        }
        String lower = name.toLowerCase();
        if (lower.startsWith("file:///")) {
            return new FileConnectionImpl(name.substring(7), mode);   // strip "file://"
        }
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return new HttpConnectionImpl(name);
        }
        if (lower.startsWith("socket://")) {
            String rest = name.substring("socket://".length());
            int colon = rest.indexOf(':');
            if (colon < 0) { throw new IllegalArgumentException("Bad socket URL: " + name); }
            String host = rest.substring(0, colon);
            String portStr = rest.substring(colon + 1);
            int port = parsePort(portStr);
            if (host.length() == 0) {
                return new ServerSocketConnectionImpl(port);
            }
            return new SocketConnectionImpl(host, port);
        }
        if (lower.startsWith("datagram://")) {
            String rest = name.substring("datagram://".length());
            String host = "";
            int port = 0;
            int colon = rest.lastIndexOf(':');
            if (colon >= 0) {
                host = rest.substring(0, colon);
                String portStr = rest.substring(colon + 1);
                if (portStr.length() > 0) { port = parsePort(portStr); }
            }
            return new DatagramConnectionImpl(host, port);
        }
        if (lower.startsWith("tcp://")) {
            String rest = name.substring("tcp://".length());
            int colon = rest.indexOf(':');
            if (colon < 0) { throw new IllegalArgumentException("Bad socket URL: " + name); }
            return new SocketConnectionImpl(rest.substring(0, colon), parsePort(rest.substring(colon + 1)));
        }
        throw new ConnectionNotFoundException("Unsupported scheme in: " + name);
    }

    static int parsePort(String s) {
        try {
            int p = Integer.parseInt(s.trim());
            if (p < 0 || p > 65535) { throw new NumberFormatException(); }
            return p;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad port: " + s);
        }
    }
}