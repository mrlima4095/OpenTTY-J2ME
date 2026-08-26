package javax.microedition.media;
public interface PlayerListener {
    String STARTED = "started";
    String STOPPED = "stopped";
    String END_OF_MEDIA = "end of media";
    String DEVICE_UNAVAILABLE = "device unavailable";
    String DEVICE_ERROR = "device error";
    String RECORDING_ERROR = "recording error";
    String BUFFERING_STARTED = "buffering started";
    String BUFFERING_STOPPED = "buffering stopped";
    String ERROR_UNKNOWN = "unknown error";
    void playerUpdate(Player p, String event, Object eventData);
}
