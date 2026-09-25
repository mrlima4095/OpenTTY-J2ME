package javax.microedition.lcdui;
public class Command {
    public static final int SCREEN = 1, BACK = 2, OK = 4, CANCEL = 8, HELP = 9, STOP = 10, EXIT = 11, ITEM = 12;
    private final String label; private final int type, priority;
    public Command(String label, int type, int priority) { this.label = label; this.type = type; this.priority = priority; }
    public String getLabel() { return label; }
    public int getCommandType() { return type; }
    public int getPriority() { return priority; }
}
