package javax.microedition.media;

/** PlayerListener (JSR-135). */
public interface PlayerListener {
    String STARTED = "started";
    String STOPPED = "stopped";
    String END_OF_MEDIA = "endOfMedia";
    String DURATION_UPDATED = "durationUpdated";
    String VOLUME_CHANGED = "volumeChanged";
    String ERROR = "error";
    String CLOSED = "closed";
    String DEVICE_UNAVAILABLE = "deviceUnavailable";
    void playerUpdate(Player player, String event, Object eventData);
}