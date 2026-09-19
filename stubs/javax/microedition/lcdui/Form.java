package javax.microedition.lcdui;

public class Form extends Displayable {
    private final java.util.Vector items = new java.util.Vector();
    private ItemStateListener itemStateListener = null;

    public Form(String title) {
        setTitle(title);
    }

    public int append(String text) {
        return append((Item) new StringItem(null, text == null ? "" : text));
    }

    public int append(Image img) {
        return append((Item) new ImageItem(null, img, Item.LAYOUT_DEFAULT, null));
    }

    public int append(Item item) {
        if (item == null) { return items.size(); }
        item._owner = this;
        items.addElement(item);
        _repaint();
        return items.size() - 1;
    }

    public int size() { return items.size(); }

    public Item get(int index) { return (Item) items.elementAt(index); }

    public void deleteAll() {
        for (int i = 0; i < items.size(); i++) { ((Item) items.elementAt(i))._owner = null; }
        items.removeAllElements();
        _repaint();
    }

    public void set(int index, Item item) {
        if (index < 0 || index >= items.size()) { throw new IndexOutOfBoundsException(); }
        ((Item) items.elementAt(index))._owner = null;
        item._owner = this;
        items.setElementAt(item, index);
        _repaint();
    }

    public void delete(int index) {
        if (index < 0 || index >= items.size()) { throw new IndexOutOfBoundsException(); }
        ((Item) items.elementAt(index))._owner = null;
        items.removeElementAt(index);
        _repaint();
    }

    public void insert(int index, Item item) {
        item._owner = this;
        items.insertElementAt(item, index);
        _repaint();
    }

    public void setItemStateListener(ItemStateListener l) {
        itemStateListener = l;
    }

    void _itemStateChanged(Item item) {
        if (itemStateListener != null) { itemStateListener.itemStateChanged(item); }
    }

    void _itemVisualChanged(Item item) {
        DesktopLcdui._itemSync(item);
        _repaint();
    }
}