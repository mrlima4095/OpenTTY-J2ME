package javax.microedition.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class DatagramImpl implements Datagram {
    private byte[] data;
    private int offset = 0;
    private int length = 0;
    private String address = "datagram://0.0.0.0:0";

    DatagramImpl(byte[] buf) { data = buf; length = buf.length; }

    public String getAddress() { return address; }
    public void setAddress(String addr) { address = addr == null ? "datagram://0.0.0.0:0" : addr; }
    public void setAddress(Datagram reference) { address = reference.getAddress(); }
    public byte[] getData() { return data; }
    public int getLength() { return length; }
    public void setLength(int len) { length = len; }
    public int getOffset() { return offset; }
    public void reset() { offset = 0; }
    public void setData(byte[] buffer, int off, int len) {
        data = buffer; offset = off; length = len; if (length > buffer.length - off) { length = buffer.length - off; }
    }
    public void close() { }

    // DataOutput
    public void write(int b) throws IOException { writeByte(b); }
    public void write(byte[] b) throws IOException { write(b, 0, b.length); }
    public void write(byte[] b, int off, int len) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        new DataOutputStream(bos).write(b, off, len);
        setData(bos.toByteArray(), 0, bos.size());
    }
    public void writeBoolean(boolean v) throws IOException { write(new byte[] { (byte) (v ? 1 : 0) }, 0, 1); }
    public void writeByte(int v) throws IOException { write(new byte[] { (byte) v }, 0, 1); }
    public void writeShort(int v) throws IOException { ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b); d.writeShort(v); setData(b.toByteArray(), 0, b.size()); }
    public void writeChar(int v) throws IOException { writeShort(v); }
    public void writeInt(int v) throws IOException { ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b); d.writeInt(v); setData(b.toByteArray(), 0, b.size()); }
    public void writeLong(long v) throws IOException { ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b); d.writeLong(v); setData(b.toByteArray(), 0, b.size()); }
    public void writeFloat(float v) throws IOException { writeInt(Float.floatToIntBits(v)); }
    public void writeDouble(double v) throws IOException { writeLong(Double.doubleToLongBits(v)); }
    public void writeBytes(String s) throws IOException { write(s.getBytes(), 0, s.length()); }
    public void writeChars(String s) throws IOException { writeShort(s.length()); }
    public void writeUTF(String s) throws IOException { ByteArrayOutputStream b = new ByteArrayOutputStream(); DataOutputStream d = new DataOutputStream(b); d.writeUTF(s); setData(b.toByteArray(), 0, b.size()); }
    public void flush() throws IOException { }

    // DataInput (reads from the current data[] window)
    public void readFully(byte[] b) throws IOException { readFully(b, 0, b.length); }
    public void readFully(byte[] b, int off, int len) throws IOException { System.arraycopy(data, offset, b, off, Math.min(len, length - offset)); offset += Math.min(len, length - offset); }
    public int skipBytes(int n) throws IOException { int s = Math.min(n, length - offset); offset += s; return s; }
    public boolean readBoolean() throws IOException { return readUnsignedByte() != 0; }
    public byte readByte() throws IOException { return (byte) readUnsignedByte(); }
    public int readUnsignedByte() throws IOException { check(1); return data[offset++] & 0xff; }
    public short readShort() throws IOException { check(2); short v = (short) (((data[offset] & 0xff) << 8) | (data[offset + 1] & 0xff)); offset += 2; return v; }
    public int readUnsignedShort() throws IOException { return readShort() & 0xffff; }
    public char readChar() throws IOException { return (char) readShort(); }
    public int readInt() throws IOException { check(4); int v = ((data[offset] & 0xff) << 24) | ((data[offset + 1] & 0xff) << 16) | ((data[offset + 2] & 0xff) << 8) | (data[offset + 3] & 0xff); offset += 4; return v; }
    public long readLong() throws IOException { check(8); long v = 0; for (int i = 0; i < 8; i++) { v = (v << 8) | (data[offset + i] & 0xff); } offset += 8; return v; }
    public float readFloat() throws IOException { return Float.intBitsToFloat(readInt()); }
    public double readDouble() throws IOException { return Double.longBitsToDouble(readLong()); }
    public String readLine() throws IOException { return new DataInputStream(new ByteArrayInputStream(data)).readLine(); }
    public String readUTF() throws IOException { return DataInputStream.readUTF(new DataInputStream(new ByteArrayInputStream(data))); }

    private void check(int n) throws IOException {
        if (offset + n > length) { throw new java.io.EOFException(); }
    }
}
