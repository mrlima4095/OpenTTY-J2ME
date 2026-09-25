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
        value_ = Math.max(0, Math.min(initialValue, max_));
    }

    public boolean isInteractive() { return interactive_; }

    public int getMaxValue() { return max_; }

    public void setMaxValue(int maxValue) {
        max_ = Math.max(maxValue, 0);
        value_ = Math.max(0, Math.min(value_, max_));
        touch();
    }

    public int getValue() { return value_; }

    public void setValue(int value) {
        value_ = max_ == 0 ? 0 : Math.max(0, Math.min(value, max_));
        touch();
    }

    private int max_;
}