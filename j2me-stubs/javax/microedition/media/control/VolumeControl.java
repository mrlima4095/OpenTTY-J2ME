package javax.microedition.media.control;
public interface VolumeControl extends Control {
    int setLevel(int level);
    int getLevel();
    boolean setMute(boolean mute);
    boolean isMuted();
}
