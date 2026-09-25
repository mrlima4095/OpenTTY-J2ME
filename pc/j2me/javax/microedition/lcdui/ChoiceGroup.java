package javax.microedition.lcdui;

import java.util.Vector;

public class ChoiceGroup extends Item implements Choice {
    private final String label_;
    private final int type_;
    private final Vector labels = new Vector();
    private final Vector flags = new Vector();

    public ChoiceGroup(String label, int type) {
        label_ = label == null ? "" : label;
        type_ = type;
    }

    public int append(String label, Image image) {
        labels.addElement(label == null ? "" : label);
        flags.addElement(FALSE);
        touch();
        return labels.size() - 1;
    }

    public void insert(int index, String label, Image image) {
        labels.insertElementAt(label == null ? "" : label, index);
        flags.insertElementAt(FALSE, index);
        touch();
    }

    public void delete(int index) {
        labels.removeElementAt(index);
        flags.removeElementAt(index);
        touch();
    }

    public void deleteAll() {
        labels.removeAllElements();
        flags.removeAllElements();
        touch();
    }

    public int size() { return labels.size(); }

    public String getString(int index) { return (String) labels.elementAt(index); }

    public void set(int index, String label, Image image) {
        labels.setElementAt(label == null ? "" : label, index);
        touch();
    }

    public boolean isSelected(int index) { return flags.elementAt(index) == TRUE; }

    public int getSelectedIndex() {
        for (int i = 0; i < flags.size(); i++) {
            if (flags.elementAt(i) == TRUE) { return i; }
        }
        return -1;
    }

    public int getSelectedFlags(boolean[] flagsOut) {
        int n = flags.size();
        for (int i = 0; i < flagsOut.length; i++) { flagsOut[i] = i < n && flags.elementAt(i) == TRUE; }
        return flagsOwnCount();
    }

    private int flagsOwnCount() {
        int c = 0;
        for (int i = 0; i < flags.size(); i++) { if (flags.elementAt(i) == TRUE) { c++; } }
        return c;
    }

    public void setSelectedIndex(int index, boolean selected) {
        if (type_ == Choice.EXCLUSIVE || type_ == Choice.IMPLICIT || type_ == Choice.POPUP) {
            for (int i = 0; i < flags.size(); i++) { flags.setElementAt(FALSE, i); }
        }
        flags.setElementAt(selected ? TRUE : FALSE, index);
        touch();
    }

    public void setSelectedFlags(boolean[] selected) {
        for (int i = 0; i < flags.size(); i++) {
            flags.setElementAt(i < selected.length && selected[i] ? TRUE : FALSE, i);
        }
        touch();
    }

    private static final Object TRUE = Boolean.TRUE;
    private static final Object FALSE = Boolean.FALSE;
}