package javax.microedition.media;

import javax.microedition.media.control.*;

public interface Player extends javax.microedition.media.Control {
    int CLOSED = 0, UNREALIZED = 100, REALIZED = 200, PREFETCHED = 300, STARTED = 400;
    long TIME_UNKNOWN = Long.MAX_VALUE;

    void realize() throws MediaException;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void deallocate();
    void close();
    long setMediaTime(long now) throws MediaException;
    long getMediaTime();
    long getDuration();
    void setLoopCount(int count);
    int getState();
    Control getControl(String controlType);
    Control[] getControls();
    void addPlayerListener(Object listener);
    void removePlayerListener(Object listener);
}
