package javax.microedition.io;

import java.io.IOException;

public class Connector {
    public static final int READ = 1;
    public static final int WRITE = 2;
    public static final int READ_WRITE = 3;

    private Connector() { }

    public static Connection open(String name) throws IOException {
        return open(name, READ_WRITE, false);
    }

    public static Connection open(String name, int mode) throws IOException {
        return open(name, mode, false);
    }

    public static Connection open(String name, int mode, boolean timeouts) throws IOException {
        if (name == null) { throw new NullPointerException("null URL"); }
        if (name.startsWith("file://")) {
            return new javax.microedition.io.file.FileConnectionImpl(name, mode);
        }
        if (name.startsWith("socket://")) {
            String rest = name.substring(9);
            int colon = rest.lastIndexOf(':');
            String host = colon >= 0 ? rest.substring(0, colon) : rest;
            String portStr = colon >= 0 ? rest.substring(colon + 1) : "0";
            int port = 0;
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException e) { }
            if (host.length() == 0) {
                return new ServerSocketConnectionImpl(port);
            }
            return new SocketConnectionImpl(host, port);
        }
        if (name.startsWith("datagram://") || name.startsWith("udp://")) {
            String rest = name.startsWith("datagram://") ? name.substring(11) : name.substring(6);
            int colon = rest.lastIndexOf(':');
            String portStr = colon >= 0 ? rest.substring(colon + 1) : "0";
            int port = 0;
            try { port = Integer.parseInt(portStr); } catch (NumberFormatException e) { }
            return new DatagramConnectionImpl(port);
        }
        if (name.startsWith("http://") || name.startsWith("https://")) {
            return new HttpConnectionImpl(name);
        }
        throw new ConnectionNotFoundException("Unsupported connection scheme: " + name);
    }
}
