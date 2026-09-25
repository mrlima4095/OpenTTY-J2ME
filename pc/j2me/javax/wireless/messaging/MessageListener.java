package javax.wireless.messaging;

public interface MessageListener {
    void notifyIncomingMessage(MessageConnection conn);
}
