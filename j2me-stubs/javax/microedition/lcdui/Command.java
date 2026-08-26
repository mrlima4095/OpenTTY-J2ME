package javax.microedition.lcdui;
public class Command {
    public static final int BACK = 2;
    public static final int CANCEL = 3;
    public static final int EXIT = 7;
    public static final int HELP = 5;
    public static final int ITEM = 8;
    public static final int OK = 4;
    public static final int SCREEN = 1;
    public static final int STOP = 6;
    private String label;
    public Command(String label, int commandType, int priority) {}
    public String getLabel() { return label; }
}
