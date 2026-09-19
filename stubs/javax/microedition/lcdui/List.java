package javax.microedition.lcdui;

public class List extends Displayable implements Choice {
    public static final Command SELECT_COMMAND = new Command("Select", Command.ITEM, 0);
    public static final int EXCLUSIVE = Choice.EXCLUSIVE;
    public static final int MULTIPLE = Choice.MULTIPLE;
    public static final int IMPLICIT = Choice.IMPLICIT;
    public static final int POPUP = Choice.POPUP;

    private final int listType;
    private final java.util.Vector elements = new java.util.Vector();   // String
    private final java.util.Vector images = new java.util.Vector();      // Image|null
    private boolean[] selected = new boolean[0];
    private int selectedIndex = -1;
    private Command selectCommand = null;

    public List(String title, int listType) {
        setTitle(title);
        this.listType = (listType == Choice.EXCLUSIVE || listType == Choice.MULTIPLE) ? listType : Choice.IMPLICIT;
    }

    public int append(String stringElement, Image imageElement) {
        elements.addElement(stringElement == null ? "" : stringElement);
        images.addElement(imageElement);
        boolean[] ns = new boolean[selected.length + 1];
        System.arraycopy(selected, 0, ns, 0, selected.length);
        selected = ns;
        _repaint();
        return elements.size() - 1;
    }

    public void insert(int elementNum, String element, Image imageElement) {
        elements.insertElementAt(element == null ? "" : element, elementNum);
        images.insertElementAt(imageElement, elementNum);
        boolean[] ns = new boolean[selected.length + 1];
        System.arraycopy(selected, 0, ns, 0, elementNum);
        System.arraycopy(selected, elementNum, ns, elementNum + 1, selected.length - elementNum);
        selected = ns;
        _repaint();
    }

    public void delete(int elementNum) {
        elements.removeElementAt(elementNum);
        images.removeElementAt(elementNum);
        boolean[] ns = new boolean[selected.length - 1];
        System.arraycopy(selected, 0, ns, 0, elementNum);
        System.arraycopy(selected, elementNum + 1, ns, elementNum, selected.length - elementNum - 1);
        selected = ns;
        if (selectedIndex == elementNum) { selectedIndex = -1; }
        _repaint();
    }

    public void deleteAll() {
        elements.removeAllElements();
        images.removeAllElements();
        selected = new boolean[0];
        selectedIndex = -1;
        _repaint();
    }

    public void set(int elementNum, String stringPart, Image imagePart) {
        elements.setElementAt(stringPart == null ? "" : stringPart, elementNum);
        images.setElementAt(imagePart, elementNum);
        _repaint();
    }

    public String getString(int elementNum) { return (String) elements.elementAt(elementNum); }
    public Image getImage(int elementNum) { return (Image) images.elementAt(elementNum); }
    public int size() { return elements.size(); }

    public boolean isSelected(int elementNum) {
        return elementNum >= 0 && elementNum < selected.length && selected[elementNum];
    }

    public int getSelectedIndex() {
        if (selectedIndex >= 0 && selectedIndex < selected.length && selected[selectedIndex]) { return selectedIndex; }
        for (int i = 0; i < selected.length; i++) { if (selected[i]) { return i; } }
        return selectedIndex >= 0 && selectedIndex < selected.length ? selectedIndex : -1;
    }

    public void setSelectedIndex(int elementNum, boolean state) {
        if (elementNum < 0 || elementNum >= selected.length) { return; }
        if (listType != Choice.MULTIPLE) {
            java.util.Arrays.fill(selected, false);
            selectedIndex = elementNum;
        } else {
            if (selectedIndex == elementNum && !state) { selectedIndex = -1; }
        }
        selected[elementNum] = state;
        _repaint();
    }

    public int getSelectedFlags(boolean[] flags) {
        if (flags == null || flags.length < selected.length) { throw new IllegalArgumentException(); }
        int count = 0;
        for (int i = 0; i < selected.length; i++) { flags[i] = selected[i]; if (selected[i]) { count++; } }
        return count;
    }

    public void setSelectedFlags(boolean[] flags) {
        if (flags == null || flags.length < selected.length) { throw new IllegalArgumentException(); }
        if (listType != Choice.MULTIPLE) { java.util.Arrays.fill(selected, false); selectedIndex = -1; }
        for (int i = 0; i < selected.length && i < flags.length; i++) {
            if (flags[i]) {
                selected[i] = true;
                if (listType != Choice.MULTIPLE) { selectedIndex = i; }
            }
        }
        _repaint();
    }

    public void setSelectCommand(Command command) {
        selectCommand = command;
    }

    boolean _selectCommand_is() { return selectCommand != null; }
    boolean _isMultiple() { return listType == Choice.MULTIPLE; }
    Command _selectCommand() { return selectCommand; }
    boolean[] _selectedFlags() { return selected; }

    /** Called by the renderer when the user's selection changes. */
    void _setSelection(boolean[] flags) {
        int old = getSelectedIndex();
        System.arraycopy(flags, 0, selected, 0, Math.min(flags.length, selected.length));
        selectedIndex = -1;
        for (int i = 0; i < selected.length; i++) { if (selected[i]) { selectedIndex = i; break; } }
        if (old != getSelectedIndex()) { _repaint(); }
    }

    /** Called by the renderer on ENTER / double-click: fire the select command. */
    void _select() {
        Command c = selectCommand != null ? selectCommand : List.SELECT_COMMAND;
        if (display != null) { display._fireCommand(this, c); }
    }
}