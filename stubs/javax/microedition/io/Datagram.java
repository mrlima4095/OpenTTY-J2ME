package javax.microedition.io;

import java.io.IOException;

public interface Datagram {
    String getAddress();
    byte[] getData();
    int getLength();
    int getOffset();
    void reset();
    void setAddress(String addr) throws IOException;
    void setAddress(Datagram reference) throws IOException;
    void setData(byte[] buffer, int offset, int len);
    void setLength(int len);
}