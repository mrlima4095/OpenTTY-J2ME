package javax.microedition.io;

import java.io.DataInput;
import java.io.DataOutput;

public interface Datagram extends Connection, DataInput, DataOutput {
    String getAddress();
    byte[] getData();
    int getLength();
    int getOffset();
    void reset();
    void setAddress(String addr);
    void setAddress(Datagram reference);
    void setData(byte[] buffer, int offset, int len);
    void setLength(int len);
}
