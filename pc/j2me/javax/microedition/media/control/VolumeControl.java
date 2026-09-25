package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface VolumeControl extends Control {
    int getLevel();
    int setLevel(int level);
    boolean isMuted();
    void setMuted(boolean mute);
}
