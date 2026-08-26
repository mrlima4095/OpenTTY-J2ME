package javax.microedition.media;
import java.io.InputStream;
import javax.microedition.media.control.VolumeControl;
public interface Player {
    int STOPPED = 0;
    int PREFETCHED = 300;
    int STARTED = 400;
    int TIME_UNKNOWN = -1;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void deallocate();
    void close();
    int getState();
    long getDuration();
    long getMediaTime();
    long setMediaTime(long now) throws MediaException;
    javax.microedition.media.control.Control getControl(String controlType);
    javax.microedition.media.control.Control[] getControls();
    void addPlayerListener(PlayerListener listener);
    void removePlayerListener(PlayerListener listener);
    void setLoopCount(int count);
    int getLoopCount();
    String getContentType();
    void setVolumeLevel(int level);
    int getVolumeLevel();
}
