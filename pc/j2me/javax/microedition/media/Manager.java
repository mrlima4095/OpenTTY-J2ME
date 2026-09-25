package javax.microedition.media;

import java.io.InputStream;

public class Manager {
    public static final String TONE_DEVICE_LOCATOR = "device://tone";
    public static final String MIDI_DEVICE_LOCATOR = "device://midi";

    public static Player createPlayer(String locator) throws java.io.IOException, MediaException {
        return new SimplePlayer();
    }

    public static Player createPlayer(InputStream stream, String type) throws java.io.IOException, MediaException {
        try {
            stream.close();
        } catch (Exception e) { }
        return new SimplePlayer();
    }

    public static String[] getSupportedContentTypes(String protocol) {
        return new String[] { "audio/mpeg", "audio/x-wav", "audio/basic", "audio/midi" };
    }

    public static String[] getSupportedProtocols(String content) {
        return new String[] { "capture", "http", "file" };
    }

    static final class SimplePlayer implements Player {
        private int state = UNREALIZED;
        private long mediaTime = 0, duration = 0;
        private int level = 50;

        public void realize() { state = REALIZED; }
        public void prefetch() { state = PREFETCHED; }
        public void start() { state = STARTED; }
        public void stop() { state = PREFETCHED; }
        public void deallocate() { state = UNREALIZED; }
        public void close() { state = CLOSED; }
        public long setMediaTime(long now) { mediaTime = now; return now; }
        public long getMediaTime() { return state == STARTED ? mediaTime : TIME_UNKNOWN; }
        public long getDuration() { return duration == 0 ? TIME_UNKNOWN : duration; }
        public void setLoopCount(int count) { }
        public int getState() { return state; }
        public Control getControl(String controlType) {
            if (controlType != null && controlType.equals("VolumeControl")) { return volume; }
            return null;
        }
        public Control[] getControls() { return new Control[] { volume }; }
        public void addPlayerListener(Object l) { }
        public void removePlayerListener(Object l) { }

        private final Control volume = new javax.microedition.media.control.VolumeControl() {
            public int getLevel() { return level; }
            public int setLevel(int l) { level = Math.max(0, Math.min(100, l)); return level; }
            public boolean isMuted() { return false; }
            public void setMuted(boolean mute) { }
        };
    }
}
