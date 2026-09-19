package javax.microedition.io;

import java.io.IOException;

/** Datagram backed by a byte[] buffer. */
final class DatagramImpl implements Datagram {
    private byte[] buf;
    private String address = "";
    private int offset = 0;
    private int length = 0;

    DatagramImpl(int size) {
        this.buf = new byte[size];
        this.length = size;
    }

    DatagramImpl(byte[] buffer, int size) {
        this.buf = buffer;
        this.offset = 0;
        this.length = size;
    }

    DatagramImpl(byte[] buffer, int offset, int len) {
        this.buf = buffer;
        this.offset = offset;
        this.length = len;
    }

    public String getAddress() { return address; }

    public void setAddress(String addr) throws IOException {
        if (addr == null) { throw new NullPointerException(); }
        address = addr;
    }

    public void setAddress(Datagram reference) throws IOException {
        address = reference.getAddress();
    }

    public byte[] getData() { return buf; }

    public int getOffset() { return offset; }

    public int getLength() { return length; }

    public void setLength(int len) {
        if (len < 0) { throw new IllegalArgumentException("Negative length"); }
        length = len;
    }

    public void setData(byte[] buffer, int offset, int len) {
        if (buffer == null) { throw new NullPointerException(); }
        buf = buffer;
        this.offset = offset;
        this.length = len;
    }

    public void reset() {
        offset = 0;
        length = 0;
    }
}