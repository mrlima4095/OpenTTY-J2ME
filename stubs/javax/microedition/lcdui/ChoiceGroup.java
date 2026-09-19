package javax.microedition.lcdui;

public class ChoiceGroup extends Item implements Choice {
    private final int choiceType;
    private final java.util.Vector elements = new java.util.Vector();   // String
    private final java.util.Vector images = new java.util.Vector();     // Image|null
    private final java.util.Vector selected = new java.util.Vector();   // Boolean

    public ChoiceGroup(String label, int choiceType) {
        setLabel(label);
        this.choiceType = choiceType;
    }

    public int append(String element, Image image) {
        elements.addElement(element == null ? "" : element);
        images.addElement(image);
        selected.addElement(Boolean.FALSE);
        _touch();
        return elements.size() - 1;
    }

    public void insert(int elementNum, String element, Image image) {
        elements.insertElementAt(element == null ? "" : element, elementNum);
        images.insertElementAt(image, elementNum);
        selected.insertElementAt(Boolean.FALSE, elementNum);
        _touch();
    }

    public void delete(int elementNum) {
        elements.removeElementAt(elementNum);
        images.removeElementAt(elementNum);
        selected.removeElementAt(elementNum);
        _touch();
    }

    public void set(int elementNum, String element, Image image) {
        elements.setElementAt(element == null ? "" : element, elementNum);
        images.setElementAt(image, elementNum);
        _touch();
    }

    public String getString(int elementNum) { return (String) elements.elementAt(elementNum); }
    public Image getImage(int elementNum) { return (Image) images.elementAt(elementNum); }
    public int size() { return elements.size(); }

    public boolean isSelected(int elementNum) {
        return elementNum >= 0 && elementNum < selected.size()
            && ((Boolean) selected.elementAt(elementNum)).booleanValue();
    }

    public int getSelectedIndex() {
        for (int i = 0; i < selected.size(); i++) {
            if (((Boolean) selected.elementAt(i)).booleanValue()) { return i; }
        }
        return -1;
    }

    public void setSelectedIndex(int elementNum, boolean state) {
        if (elementNum < 0 || elementNum >= selected.size()) { return; }
        if (choiceType == Choice.EXCLUSIVE && state) {
            for (int i = 0; i < selected.size(); i++) { selected.setElementAt(Boolean.FALSE, i); }
        }
        selected.setElementAt(state ? Boolean.TRUE : Boolean.FALSE, elementNum);
        _touch();
        _changed();
    }

    public int getSelectedFlags(boolean[] flags) {
        if (flags == null || flags.length < selected.size()) {
            throw new IllegalArgumentException("flags too small");
        }
        int count = 0;
        for (int i = 0; i < selected.size(); i++) {
            boolean s = ((Boolean) selected.elementAt(i)).booleanValue();
            flags[i] = s;
            if (s) { count++; }
        }
        return count;
    }

    public void setSelectedFlags(boolean[] flags) {
        if (flags == null || flags.length < selected.size()) {
            throw new IllegalArgumentException("flags too small");
        }
        for (int i = 0; i < selected.size(); i++) {
            boolean s = flags[i];
            if (choiceType == Choice.EXCLUSIVE && s) {
                for (int j = 0; j < selected.size(); j++) { selected.setElementAt(Boolean.FALSE, j); }
            }
            selected.setElementAt(s ? Boolean.TRUE : Boolean.FALSE, i);
        }
        _touch();
        _changed();
    }

    int _type() { return choiceType; }
}