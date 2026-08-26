package javax.wireless.messaging;
public interface MessageConnection extends javax.microedition.io.StreamConnection {
    int MAX_PAYLOAD_LENGTH = 140;
    Message newMessage(String type);
    Message newMessage(String type, String address);
    void send(Message message) throws java.io.IOException;
    Message receive() throws java.io.IOException;
    int segmentsAvailable();
}
