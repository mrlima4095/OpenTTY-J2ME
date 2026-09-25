package javax.microedition.lcdui;
public class List extends Displayable {
    public static final int EXCLUSIVE = 1, MULTIPLE = 2, IMPLICIT = 3;
    public static final Command SELECT_COMMAND = new Command("Select", Command.OK, 0);
    public List(String title, int listType) { }
    public int append(String stringPart, Image imagePart) { return 0; }
    public void deleteAll() { }
    public int getSelectedIndex() { return -1; }
}
