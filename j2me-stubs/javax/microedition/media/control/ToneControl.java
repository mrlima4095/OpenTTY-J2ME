package javax.microedition.media.control;
public interface ToneControl extends Control {
    byte START = 1;
    byte STOP = 2;
    byte TEMPo = 3;
    byte BLOCK_START = 4;
    byte BLOCK_END = 5;
    byte PLAY_BLOCK = 6;
    byte REPEAT = 7;
    byte SET_VOLUME = 8;
    byte NOTE = 9;
    void setSequence(byte[] sequence);
}
