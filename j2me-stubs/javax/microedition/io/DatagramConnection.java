package javax.microedition.io;
public interface DatagramConnection extends Connection {
    int getMaximumLength() throws java.io.IOException;
    int getNominalLength() throws java.io.IOException;
    void send(Datagram dgram) throws java.io.IOException;
    void receive(Datagram dgram) throws java.io.IOException;
    Datagram newDatagram(int size) throws java.io.IOException;
    Datagram newDatagram(int size, String addr) throws java.io.IOException;
    Datagram newDatagram(byte[] buf, int size) throws java.io.IOException;
    Datagram newDatagram(byte[] buf, int size, String addr) throws java.io.IOException;
}
