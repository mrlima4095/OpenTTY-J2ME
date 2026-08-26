package javax.microedition.lcdui;
public class ChoiceGroup extends Item implements Choice {
    public ChoiceGroup(String label, int choiceType) {}
    public int append(String stringPart, Image imagePart) { return 0; }
    public int size() { return 0; }
    public boolean isSelected(int elementNum) { return false; }
    public String getString(int elementNum) { return null; }
    public void deleteAll() {}
}
