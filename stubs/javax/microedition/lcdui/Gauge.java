package javax.microedition.lcdui;

public class Gauge extends Item {
    public static final boolean INDEFINITE = false;
    public static final boolean CONTINUOUS_IDLE = false;
    public static final boolean INCREMENTAL_IDLE = false;
    public static final boolean INCREMENTAL_UPDATING = true;
    public static final boolean CONTINUOUS_RUNNING = true;

    private boolean interactive;
    private int maxValue;
    private int value;
    private boolean indefinite;

    public Gauge(String label, boolean interactive, int maxValue, int initialValue) {
        setLabel(label);
        this.interactive = interactive;
        this.maxValue = maxValue;
        this.value = clamp(initialValue);
    }

    public Gauge(String label, boolean interactive, int maxValue, int initialValue, boolean indefinite) {
        this(label, interactive, maxValue, initialValue);
        this.indefinite = indefinite;
    }

    private int clamp(int v) {
        if (v < 0) { return 0; }
        if (v > maxValue && maxValue >= 0) { return maxValue; }
        return v;
    }

    public int getValue() { return value; }

    public void setValue(int value) {
        if (value < 0 && maxValue == 0) {
            // INDEFINITE gauge
            this.value = value;
            _touch();
            _changed();
            return;
        }
        int clamped = clamp(value);
        if (clamped != this.value) {
            this.value = clamped;
            _touch();
            _changed();
        }
    }

    public boolean isInteractive() { return interactive; }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
        this.value = clamp(value);
        _touch();
    }

    public int getMaxValue() { return maxValue; }

    void _setValueSilent(int v) { this.value = clamp(v); }
}