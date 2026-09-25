package javax.microedition.lcdui;

import java.util.Vector;

public class List extends Displayable {
    public static final int EXCLUSIVE = 1;
    public static final int MULTIPLE = 2;
    public static final int IMPLICIT = 3;
    public static final Command SELECT_COMMAND = new Command("", Command.OK, 0);

    protected final Vector elements = new Vector();
    private final int listType;
    protected boolean[] flags;
    private int selected = 0;
    private Command selectCommand = SELECT_COMMAND;

    public List(String title, int listType) {
        this.title = title == null ? "" : title;
        this.listType = listType;
    }

    public int size() { return elements.size(); }

    public int append(String word, Image img) {
        elements.addElement(word == null ? "" : word);
        int n = elements.size();
        boolean[] f = new boolean[n];
        if (flags != null) { System.arraycopy(flags, 0, f, 0, Math.min(flags.length, n)); }
        flags = f;
        if (listType == IMPLICIT) { flags[selected] = true; }
        touch();
        return n - 1;
    }

    public void set(int index, String word, Image img) {
        if (index >= 0 && index < elements.size()) { elements.setElementAt(word == null ? "" : word, index); }
        touch();
    }

    public void delete(int index) {
        if (index >= 0 && index < elements.size()) {
            elements.removeElementAt(index);
            boolean[] f = new boolean[elements.size()];
            if (flags != null) { System.arraycopy(flags, 0, f, 0, Math.min(flags.length, f.length)); }
            flags = f;
        }
        touch();
    }

    public void deleteAll() { elements.removeAllElements(); flags = new boolean[0]; selected = 0; touch(); }

    public String getString(int index) { return index >= 0 && index < elements.size() ? (String) elements.elementAt(index) : ""; }

    public Image getImage(int index) { return null; }

    public int getSelectedIndex() { return selected; }

    public void setSelectedIndex(int index, boolean select) {
        if (index >= 0 && index < elements.size()) {
            if (flags != null) {
                if (listType == EXCLUSIVE || listType == IMPLICIT) { for (int i = 0; i < flags.length; i++) { flags[i] = false; } }
                flags[index] = select;
            }
            if (select) { selected = index; }
        }
    }

    public void setSelectedFlags(boolean[] sel) {
        if (sel != null && flags != null && sel.length == flags.length) { flags = sel; }
    }

    public boolean[] getSelectedFlags(boolean[] returned) {
        if (returned != null && flags != null && returned.length >= flags.length) { System.arraycopy(flags, 0, returned, 0, flags.length); }
        return returned;
    }

    public boolean isSelected(int index) { return flags != null && index >= 0 && index < flags.length && flags[index]; }

    public void setSelectCommand(Command c) { selectCommand = c == null ? SELECT_COMMAND : c; }
    public Command getSelectCommand() { return selectCommand; }

    public int getListType() { return listType; }

    void setSelectionFromUi(int index) {
        if (index >= 0 && index < elements.size()) {
            selected = index;
            if (flags != null && listType != MULTIPLE) {
                for (int i = 0; i < flags.length; i++) { flags[i] = i == index; }
            } else if (flags != null) { flags[index] = true; }
        }
    }
}