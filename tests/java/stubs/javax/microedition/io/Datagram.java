package javax.microedition.io;
public interface Datagram {
    String getAddress();
    byte[] getData();
    int getLength();
    int getOffset();
    void setAddress(String addr);
    void reset();
}
