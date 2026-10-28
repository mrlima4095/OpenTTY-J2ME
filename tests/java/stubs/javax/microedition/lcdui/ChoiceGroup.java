package javax.microedition.lcdui;
import java.util.Vector;
public class ChoiceGroup extends Item {
    private final int choiceType;
    private final Vector choices = new Vector();
    private final Vector selected = new Vector();
    public ChoiceGroup(String label, int choiceType) { this.label = label; this.choiceType = choiceType; }
    public int append(String text, Image image) {
        choices.addElement(text == null ? "" : text);
        selected.addElement(new Boolean(false));
        return choices.size() - 1;
    }
    public int size() { return choices.size(); }
    public void delete(int index) { choices.removeElementAt(index); selected.removeElementAt(index); }
    public int getSelectedIndex() {
        for (int i = 0; i < selected.size(); i++) { if (((Boolean) selected.elementAt(i)).booleanValue()) { return i; } }
        return -1;
    }
    public boolean isSelected(int index) { return ((Boolean) selected.elementAt(index)).booleanValue(); }
    public void setSelectedIndex(int index, boolean state) { selected.setElementAt(new Boolean(state), index); }
}
