package javax.microedition.media;

import javax.microedition.media.control.Control;

/** Controllable (JSR-135). */
public interface Controllable {
    Control getControl(String controlType);
    Control[] getControls();
}