package javax.microedition.media.control;

/** VolumeControl (JSR-135). */
public interface VolumeControl extends Control {
    int getLevel();
    int setLevel(int level);
    boolean isMuted();
    void setMuted(boolean mute);
}