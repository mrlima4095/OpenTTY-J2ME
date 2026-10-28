package javax.microedition.lcdui;
public class Gauge extends Item {
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
