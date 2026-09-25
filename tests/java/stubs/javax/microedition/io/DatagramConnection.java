package javax.microedition.io;
public interface DatagramConnection extends Connection {
    Datagram newDatagram(int size) throws java.io.IOException;
    Datagram newDatagram(byte[] buf, int size) throws java.io.IOException;
    Datagram newDatagram(byte[] buf, int size, String addr) throws java.io.IOException;
    void receive(Datagram dgram) throws java.io.IOException;
    void send(Datagram dgram) throws java.io.IOException;
}
