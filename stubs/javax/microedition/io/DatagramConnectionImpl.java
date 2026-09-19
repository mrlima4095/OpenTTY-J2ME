package javax.microedition.io;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/** DatagramConnection backed by java.net.DatagramSocket. */
final class DatagramConnectionImpl implements DatagramConnection {
    private final DatagramSocket socket;
    private boolean closed = false;

    DatagramConnectionImpl(String host, int port) throws IOException {
        if (host == null || host.length() == 0) {
            socket = new DatagramSocket(port);
        } else {
            socket = new DatagramSocket();
            socket.connect(InetAddress.getByName(host), port);
        }
        socket.setSoTimeout(0);   // blocking receive
    }

    public void open() throws IOException { }

    public void close() throws IOException {
        if (closed) { return; }
        closed = true;
        socket.close();
    }

    public int getMaximumLength() throws IOException { return 65507; }
    public int getNominalLength() throws IOException { return 512; }

    public Datagram newDatagram(int size) throws IOException {
        return new DatagramImpl(size);
    }

    public Datagram newDatagram(int size, String addr) throws IOException {
        DatagramImpl d = new DatagramImpl(size);
        d.setAddress(addr);
        return d;
    }

    public Datagram newDatagram(byte[] buffer, int size) throws IOException {
        if (buffer == null) { throw new NullPointerException(); }
        return new DatagramImpl(buffer, 0, size);
    }

    public Datagram newDatagram(byte[] buffer, int offset, int size) throws IOException {
        if (buffer == null) { throw new NullPointerException(); }
        return new DatagramImpl(buffer, offset, size);
    }

    public Datagram newDatagram(byte[] buffer, int offset, int size, String addr) throws IOException {
        DatagramImpl d = new DatagramImpl(buffer, offset, size);
        d.setAddress(addr);
        return d;
    }

    public Datagram newDatagram(byte[] buffer, int size, String addr) throws IOException {
        DatagramImpl d = new DatagramImpl(buffer, 0, size);
        d.setAddress(addr);
        return d;
    }

    public void send(Datagram dgram) throws IOException {
        if (dgram == null) { throw new NullPointerException(); }
        String addr = dgram.getAddress();
        if (addr == null || addr.length() == 0) {
            throw new IllegalArgumentException("No destination address");
        }
        String a = addr;
        if (a.startsWith("datagram://")) { a = a.substring("datagram://".length()); }
        int colon = a.lastIndexOf(':');
        String host = colon < 0 ? a : a.substring(0, colon);
        int port = colon < 0 ? 0 : Integer.parseInt(a.substring(colon + 1));
        byte[] data = dgram.getData();
        int offset = dgram.getOffset();
        int len = dgram.getLength();
        if (offset + len > data.length) { len = data.length - offset; }
        DatagramPacket pkt = new DatagramPacket(data, offset, len, InetAddress.getByName(host), port);
        socket.send(pkt);
    }

    public void receive(Datagram dgram) throws IOException {
        if (dgram == null) { throw new NullPointerException(); }
        byte[] buf = dgram.getData();
        if (buf == null) { throw new NullPointerException(); }
        DatagramPacket pkt = new DatagramPacket(buf, buf.length);
        socket.receive(pkt);
        dgram.setLength(pkt.getLength());
        dgram.setData(buf, 0, pkt.getLength());
        dgram.setAddress("datagram://" + pkt.getAddress().getHostAddress() + ":" + pkt.getPort());
    }

    public String getLocalAddress() throws IOException {
        return socket.getLocalAddress() == null ? "" : socket.getLocalAddress().getHostAddress();
    }

    public int getLocalPort() throws IOException { return socket.getLocalPort(); }
}