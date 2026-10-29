package javax.microedition.lcdui;
public class Gauge extends Item {
    public static final int CONTINUOUS_IDLE = 0;
    public static final int INCREMENTAL_IDLE = 1;
    public static final int CONTINUOUS_RUNNING = 2;
    public static final int INCREMENTAL_UPDATING = 3;

    private final boolean interactive;
    private final int maxValue;
    private int value;

    public Gauge(String label, boolean interactive, int maxValue, int value) {
        this.label = label; this.interactive = interactive; this.maxValue = maxValue; this.value = value;
    }

    public boolean isInteractive() { return interactive; }
    public int getMaxValue() { return maxValue; }
    public int getValue() { return value; }
    public void setValue(int v) { value = v; }
}
