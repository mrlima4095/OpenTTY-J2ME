package javax.wireless.messaging;

import javax.microedition.io.Connection;
import java.io.IOException;

public interface MessageConnection extends Connection {
    Message newMessage(String type);
    Message newMessage(String type, String address);
    int send(Message msg) throws IOException;
    Message receive() throws IOException;
    void setMessageListener(MessageListener l) throws IOException;
    String getMessageConnection() throws IOException;
}
