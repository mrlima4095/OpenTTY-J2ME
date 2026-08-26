package javax.microedition.io;
public interface Datagram extends java.io.DataInput, java.io.DataOutput {
    java.net.InetAddress getAddress();
    String getAddressAsString();
    int getPort();
    byte[] getData();
    int getLength();
    int getOffset();
    void setAddress(java.net.InetAddress addr, int port) throws java.io.IOException;
    void setAddress(String addr) throws java.io.IOException;
    void setData(byte[] buffer, int offset, int len);
    void reset();
    void write(java.io.OutputStream os) throws java.io.IOException;
    void read(java.io.InputStream is) throws java.io.IOException;
}
