package javax.wireless.messaging;

public interface Message {
    String getAddress();
    void setAddress(String address);
    java.util.Date getTimestamp();
}
