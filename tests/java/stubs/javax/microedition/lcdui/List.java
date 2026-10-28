package javax.microedition.lcdui;
import java.util.Vector;
public class List extends Displayable implements Choice {
    public static final int EXCLUSIVE = 1, MULTIPLE = 2, IMPLICIT = 3;
    public static final Command SELECT_COMMAND = new Command("Select", Command.OK, 0);
    private final Vector entries = new Vector();
    private int selectedIndex = 0;
    public List(String title, int listType) { this.title = title == null ? "" : title; }
    public int append(String stringPart, Image imagePart) { entries.addElement(stringPart == null ? "" : stringPart); return entries.size() - 1; }
    public int size() { return entries.size(); }
    public void delete(int index) { entries.removeElementAt(index); }
    public void deleteAll() { entries.removeAllElements(); }
    public int getSelectedIndex() { return entries.size() == 0 ? -1 : selectedIndex; }
}
