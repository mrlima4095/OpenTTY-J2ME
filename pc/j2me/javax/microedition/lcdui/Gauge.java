package javax.microedition.lcdui;

public class Gauge extends Item {
    public static final int CONTINUOUS_IDLE = 0;
    public static final int INCREMENTAL_IDLE = 1;
    public static final int CONTINUOUS_RUNNING = 2;
    public static final int INCREMENTAL_UPDATING = 3;

    private final boolean interactive_;
    private int maxValue_;
    private int value_;

    public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
        setLabel(label);
        interactive_ = interactive;
        max_ = Math.max(maxValue, 0);
        value_ = clamp(initialValue);
    }

    public boolean isInteractive() { return interactive_; }

    public int getMaxValue() { return max_; }

    public void setMaxValue(int maxValue) {
        max_ = Math.max(maxValue, 0);
        value_ = clamp(value_);
        touch();
    }

    public int getValue() { return value_; }

    public void setValue(int value) {
        value_ = clamp(value);
        touch();
    }

    // A non-interactive gauge with max 0 is the MIDP continuous indicator: its
    // value is one of CONTINUOUS_IDLE / INCREMENTAL_IDLE / CONTINUOUS_RUNNING /
    // INCREMENTAL_UPDATING instead of a number in 0..max.
    private int clamp(int value) {
        if (max_ == 0) { return interactive_ ? 0 : Math.max(0, Math.min(value, 3)); }
        return Math.max(0, Math.min(value, max_));
    }

    private int max_;
}