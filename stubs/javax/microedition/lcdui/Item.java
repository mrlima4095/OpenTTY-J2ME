package javax.microedition.lcdui;

public class Item {
    public static final int PLAIN = 0;
    public static final int HYPERLINK = 2;
    public static final int BUTTON = 3;

    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_LEFT = 1;
    public static final int LAYOUT_RIGHT = 2;
    public static final int LAYOUT_CENTER = 3;
    public static final int LAYOUT_TOP = 0x10;
    public static final int LAYOUT_VCENTER = 0x20;
    public static final int LAYOUT_BOTTOM = 0x30;
    public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
    public static final int LAYOUT_NEWLINE_AFTER = 0x200;
    public static final int LAYOUT_SHRINK = 0x400;
    public static final int LAYOUT_EXPAND = 0x800;
    public static final int LAYOUT_VSHRINK = 0x1000;
    public static final int LAYOUT_VEXPAND = 0x2000;

    private String label = null;
    private Font font = null;
    private int layout = LAYOUT_DEFAULT;
    private Command defaultCommand = null;
    private ItemCommandListener itemCommandListener = null;
    private final java.util.Vector commands = new java.util.Vector();

    Form _owner = null;
    Object _comp = null;
    boolean _disposed = false;

    public String getLabel() { return label; }

    public void setLabel(String label) {
        this.label = label;
        _touch();
    }

    public Font getFont() { return font == null ? Font.getDefaultFont() : font; }

    public void setFont(Font font) {
        this.font = font;
        _structural();
    }

    public int getLayout() { return layout; }

    public void setLayout(int layout) {
        this.layout = layout;
        _structural();
    }

    public void addCommand(Command cmd) {
        if (cmd != null && !commands.contains(cmd)) { commands.addElement(cmd); }
        _structural();
    }

    public void removeCommand(Command cmd) {
        commands.removeElement(cmd);
        _structural();
    }

    public void setDefaultCommand(Command cmd) {
        defaultCommand = cmd;
        if (cmd != null && !commands.contains(cmd)) { commands.addElement(cmd); }
        _structural();
    }

    public Command getDefaultCommand() { return defaultCommand; }

    public void setItemCommandListener(ItemCommandListener l) {
        itemCommandListener = l;
    }

    ItemCommandListener _itemListener() { return itemCommandListener; }

    java.util.Vector _commands() { return commands; }

    public int getMinimumWidth() { return 8; }
    public int getMinimumHeight() { return 8; }

    public int getPreferredWidth() { return 240; }
    public int getPreferredHeight() { return 40; }

    /** Fires the item default command (used by the renderer for BUTTON items). */
    void _activate() {
        if (itemCommandListener != null && defaultCommand != null) {
            itemCommandListener.commandAction(defaultCommand, this);
        } else if (itemCommandListener != null && commands.size() > 0) {
            itemCommandListener.commandAction((Command) commands.elementAt(0), this);
        }
    }

    /** Notifies the renderer that this item's visual state changed (cheap sync). */
    void _touch() {
        if (DesktopLcdui.headless) { return; }
        DesktopLcdui._itemSync(this);
    }

    /** Structural change (layout/commands): full form rebuild. */
    private void _structural() {
        if (_owner != null) { _owner._repaint(); }
        else { _touch(); }
    }

    /** Notifies the owning Form's ItemStateListener that user input changed. */
    void _changed() {
        if (_owner != null) { _owner._itemStateChanged(this); }
    }
}