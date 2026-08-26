package javax.wireless.messaging;
public interface Message {
    String getPayloadText();
    void setPayloadText(String text);
    String getAddress();
    void setAddress(String address);
    long getTimestamp();
    void setTimestamp(long timestamp);
}
