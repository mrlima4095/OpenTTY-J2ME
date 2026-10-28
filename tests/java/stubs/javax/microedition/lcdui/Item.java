package javax.microedition.lcdui;
public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0, LAYOUT_LEFT = 1, LAYOUT_RIGHT = 2, LAYOUT_CENTER = 3;
    public static final int LAYOUT_VCENTER = 16, LAYOUT_BOTTOM = 32, LAYOUT_TOP = 64;
    public static final int LAYOUT_NEWLINE_BEFORE = 0x100, LAYOUT_NEWLINE_AFTER = 0x200;
    public static final int LAYOUT_EXPAND = 0x800, LAYOUT_VEXPAND = 0x1000;

    protected String label = "";
    protected Command defaultCommand;
    protected ItemCommandListener itemCommandListener;
    protected int layout;
    protected Font font;

    public void setLabel(String l) { label = l == null ? "" : l; }
    public String getLabel() { return label; }
    public void addCommand(Command c) { }
    public void removeCommand(Command c) { }
    public void setDefaultCommand(Command c) { defaultCommand = c; }
    public void setItemCommandListener(ItemCommandListener l) { itemCommandListener = l; }
    public void setLayout(int l) { layout = l; }
    public int getLayout() { return layout; }
}
