package javax.wireless.messaging;

public interface BinaryMessage extends Message {
    byte[] getPayloadData();
    void setPayloadData(byte[] data);
}
