package javax.microedition.lcdui;

import java.util.Vector;

public abstract class Item {
    public static final int LAYOUT_DEFAULT = 0;
    public static final int LAYOUT_EXPAND = 0x800;
    public static final int LAYOUT_NEWLINE_BEFORE = 0x100;
    public static final int LAYOUT_NEWLINE_AFTER = 0x200;

    protected String label = "";
    protected Command defaultCommand = null;
    protected ItemCommandListener itemCommandListener = null;
    protected final Vector commands = new Vector();

    public void setLabel(String l) { label = l == null ? "" : l; }
    public String getLabel() { return label; }

    public void setLayout(int layout) {}

    public void addCommand(Command c) { if (c != null && !commands.contains(c)) { commands.addElement(c); } }
    public void removeCommand(Command c) { if (c != null) { commands.removeElement(c); } }
    public void setDefaultCommand(Command c) { defaultCommand = c; }
    public void setItemCommandListener(ItemCommandListener l) { itemCommandListener = l; }

    protected void touch() { Display.touch(); }
}