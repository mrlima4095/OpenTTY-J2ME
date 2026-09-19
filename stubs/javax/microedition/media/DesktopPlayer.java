package javax.microedition.media;

import java.io.IOException;

import javax.microedition.media.control.Control;
import javax.microedition.media.control.VolumeControl;

/**
 * Desktop Player: plays WAV data via javax.sound if available, otherwise
 * silently no-ops (so the shell's sound commands never crash on headless PCs).
 */
public class DesktopPlayer implements Player {
    private final byte[] data;
    private javax.sound.sampled.Clip clip = null;
    private int state = UNREALIZED;
    private long mediaTimeMicros = 0;
    private final DesktopVolumeControl volume = new DesktopVolumeControl();
    private final java.util.Vector listeners = new java.util.Vector();

    public DesktopPlayer(byte[] data) {
        this.data = data == null ? new byte[0] : data;
    }

    public void realize() throws MediaException {
        if (state == CLOSED) { throw new IllegalStateException("Player closed"); }
        state = REALIZED;
    }

    public void prefetch() throws MediaException {
        if (state == CLOSED) { throw new IllegalStateException("Player closed"); }
        if (state < REALIZED) { realize(); }
        if (clip == null) {
            clip = createClip();
        }
        if (clip != null) {
            try { clip.stop(); clip.setFramePosition(0); } catch (Exception e) { }
        }
        state = PREFETCHED;
    }

    public void start() throws MediaException {
        if (state == CLOSED) { throw new IllegalStateException("Player closed"); }
        if (state < PREFETCHED) { prefetch(); }
        if (clip != null) {
            try {
                clip.setMicrosecondPosition(mediaTimeMicros);
                clip.start();
                state = STARTED;
                if (listeners.size() > 0) {
                    javax.sound.sampled.LineListener ll = new javax.sound.sampled.LineListener() {
                        public void update(javax.sound.sampled.LineEvent ev) {
                            if (ev.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                                fireEvent(PlayerListener.END_OF_MEDIA, null);
                            }
                        }
                    };
                    clip.addLineListener(ll);
                }
            } catch (Exception e) { }
        }
    }

    public void stop() throws MediaException {
        if (clip != null) {
            try { clip.stop(); } catch (Exception e) { }
        }
        if (state == STARTED) {
            state = PREFETCHED;
            fireEvent(PlayerListener.STOPPED, null);
        }
    }

    public void deallocate() {
        if (clip != null) { try { clip.flush(); } catch (Exception e) { } }
        if (state == STARTED) { state = PREFETCHED; }
    }

    public void close() {
        if (clip != null) { try { clip.close(); } catch (Exception e) { } clip = null; }
        state = CLOSED;
        fireEvent(PlayerListener.CLOSED, null);
    }

    public long setMediaTime(long now) throws MediaException {
        mediaTimeMicros = now;
        return now;
    }

    public long getMediaTime() {
        if (clip != null && state == STARTED) {
            try { return clip.getMicrosecondPosition(); } catch (Exception e) { }
        }
        return mediaTimeMicros;
    }

    public long getDuration() {
        if (clip != null) {
            try { return clip.getMicrosecondLength(); } catch (Exception e) { }
        }
        return TIME_UNKNOWN;
    }

    public int getState() { return state; }

    public void addPlayerListener(PlayerListener playerListener) { listeners.addElement(playerListener); }
    public void removePlayerListener(PlayerListener playerListener) { listeners.removeElement(playerListener); }

    public Control getControl(String controlType) {
        if (controlType == null) { return null; }
        String t = controlType.replace("javax.microedition.media.control.", "");
        if (t.equals("VolumeControl") || controlType.equals("VolumeControl") || controlType.equals(VolumeControl.class.getName())) {
            return volume;
        }
        return null;
    }

    public Control[] getControls() {
        return new Control[] { volume };
    }

    private javax.sound.sampled.Clip createClip() {
        try {
            javax.sound.sampled.AudioInputStream ais = javax.sound.sampled.AudioSystem.getAudioInputStream(
                new java.io.ByteArrayInputStream(data));
            javax.sound.sampled.Clip c = (javax.sound.sampled.Clip) javax.sound.sampled.AudioSystem.getLine(
                new javax.sound.sampled.DataLine.Info(javax.sound.sampled.Clip.class, ais.getFormat()));
            c.open(ais);
            return c;
        } catch (Exception e) {
            // Possibly no audio device (headless / no ALSA). Fall back to silent.
            return null;
        }
    }

    private void fireEvent(String event, Object eventData) {
        for (int i = 0; i < listeners.size(); i++) {
            try { ((PlayerListener) listeners.elementAt(i)).playerUpdate(this, event, eventData); } catch (Exception e) { }
        }
    }

    static class DesktopVolumeControl implements VolumeControl {
        private int level = 100;
        private boolean muted = false;

        public int getLevel() { return muted ? 0 : level; }

        public int setLevel(int level) {
            this.level = level < 0 ? 0 : level > 100 ? 100 : level;
            return this.level;
        }

        public boolean isMuted() { return muted; }
        public void setMuted(boolean mute) { this.muted = mute; }
    }
}