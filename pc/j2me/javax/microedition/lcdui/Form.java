package javax.microedition.lcdui;

import java.util.Vector;

public class Form extends Displayable {
    protected final Vector items = new Vector();
    private ItemStateListener itemStateListener = null;

    public Form(String title) { this.title = title == null ? "" : title; }

    public int append(Item item) {
        if (item != null) { items.addElement(item); }
        touch();
        return items.size() - 1;
    }

    public int append(String text) {
        return append(new StringItem(null, text));
    }

    public int append(Image img) {
        if (img != null) { items.addElement(img); }
        touch();
        return items.size() - 1;
    }

    public Item get(int index) {
        if (index < 0 || index >= items.size()) { throw new IndexOutOfBoundsException(); }
        Object o = items.elementAt(index);
        return o instanceof Item ? (Item) o : new StringItem("", o.toString());
    }

    public int size() { return items.size(); }

    public Form set(int index, Item item) {
        if (item != null) { items.setElementAt(item, index); }
        touch();
        return this;
    }

    public void delete(int index) { items.removeElementAt(index); touch(); }

    public void deleteAll() { items.removeAllElements(); touch(); }

    public void setItemStateListener(ItemStateListener listener) {
        itemStateListener = listener;
    }
}