package javax.microedition.media;

import javax.microedition.media.control.Control;

/** Player (subset of JSR-135 used by OpenTTY's audio module). */
public interface Player extends Controllable {
    long TIME_UNKNOWN = -2;
    int CLOSED = 0;
    int UNREALIZED = 100;
    int REALIZED = 200;
    int PREFETCHED = 300;
    int STARTED = 400;

    void realize() throws MediaException;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void deallocate();
    void close();
    long setMediaTime(long now) throws MediaException;
    long getMediaTime();
    long getDuration();
    int getState();
    void addPlayerListener(PlayerListener playerListener);
    void removePlayerListener(PlayerListener playerListener);
}