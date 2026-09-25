package javax.microedition.io;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class DatagramConnectionImpl implements DatagramConnection {
    private final DatagramSocket ds;

    public DatagramConnectionImpl(int port) throws IOException {
        this.ds = new DatagramSocket(port);
    }

    public int getMaximumLength() { return 65507; }
    public int getNominalLength() { return 65507; }

    public void send(Datagram dg) throws IOException {
        String[] t = parse(dg.getAddress());
        java.net.InetAddress addr = java.net.InetAddress.getByName(t[0]);
        int port = Integer.parseInt(t[1]);
        DatagramPacket p = new DatagramPacket(dg.getData(), dg.getOffset(), dg.getLength(), addr, port);
        ds.send(p);
    }

    public void receive(Datagram dg) throws IOException {
        DatagramPacket p = new DatagramPacket(dg.getData(), dg.getData().length);
        ds.receive(p);
        dg.setData(p.getData(), 0, p.getLength());
        dg.setLength(p.getLength());
        String host = p.getAddress() != null ? p.getAddress().getHostAddress() : "0.0.0.0";
        dg.setAddress("datagram://" + host + ":" + p.getPort());
    }

    private String[] parse(String addr) {
        String a = addr == null ? "datagram://lvdalhost:0" : addr;
        if (a.startsWith("datagram://")) { a = a.substring(11); }
        int colon = a.lastIndexOf(':');
        String host = colon >= 0 ? a.substring(0, colon) : a;
        String port = colon >= 0 ? a.substring(colon + 1) : "0";
        if (host.length() == 0) { host = "localhost"; }
        return new String[] { host, port };
    }

    public Datagram newDatagram(int size) { return new DatagramImpl(new byte[Math.max(1, size)]); }
    public Datagram newDatagram(int size, String addr) { Datagram d = newDatagram(size); d.setAddress(addr); return d; }
    public Datagram newDatagram(byte[] buf, int size) { Datagram d = new DatagramImpl(buf); if (size < buf.length) { d.setLength(size); } return d; }
    public Datagram newDatagram(byte[] buf, int size, String addr) { Datagram d = newDatagram(buf, size); d.setAddress(addr); return d; }

    public void close() { ds.close(); }
}
