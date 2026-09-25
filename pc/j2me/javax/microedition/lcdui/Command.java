package javax.microedition.lcdui;

public class Command {
    public static final int BACK = 2;
    public static final int CANCEL = 3;
    public static final int OK = 4;
    public static final int HELP = 5;
    public static final int STOP = 6;
    public static final int EXIT = 7;
    public static final int SCREEN = 8;
    public static final int ITEM = 1;

    private final String label;
    private final int type;
    private final int priority;

    public Command(String label, int type, int priority) {
        this.label = label;
        this.type = type;
        this.priority = priority;
    }

    public Command(String shortLabel, String longLabel, int type, int priority) {
        this(shortLabel, type, priority);
    }

    public String getLabel() { return label; }
    public String getLongLabel() { return label; }
    public int getCommandType() { return type; }
    public int getPriority() { return priority; }
}
