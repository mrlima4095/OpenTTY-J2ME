package javax.microedition.media.control;

import javax.microedition.media.Control;

public interface ToneControl extends Control {
    byte VERSION = -2;
    byte TEMPO = -3;
    byte RESOLUTION = -4;
    byte BLOCK_START = -5;
    byte BLOCK_END = -6;
    byte PLAY_BLOCK = -7;
    byte SET_VOLUME = -8;
    byte REPEAT = -9;
    byte SILENCE = -10;

    void setSequence(byte[] sequence);
}
