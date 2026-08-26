package javax.microedition.lcdui;
public class List extends Screen implements Choice {
    public static final int EXCLUSIVE = 1;
    public static final int MULTIPLE = 2;
    public static final int IMPLICIT = 3;
    public static final Command SELECT_COMMAND = new Command("", Command.SCREEN, 0);
    public List(String title, int listType) {}
    public int append(String stringPart, Image imagePart) { return 0; }
    public int size() { return 0; }
    public boolean isSelected(int elementNum) { return false; }
    public String getString(int elementNum) { return null; }
    public void deleteAll() {}
}
