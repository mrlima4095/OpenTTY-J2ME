package javax.microedition.lcdui;
import java.util.Vector;
public class Form extends Displayable {
    private final Vector items = new Vector();
    private ItemStateListener itemStateListener;
    public Form(String title) { this.title = title == null ? "" : title; }
    public int append(Item item) { items.addElement(item); return items.size() - 1; }
    public int append(Image image) { items.addElement(image); return items.size() - 1; }
    public int append(String text) { return append(new StringItem(null, text)); }
    public int insert(int index, Item item) { items.insertElementAt(item, index); return index; }
    public void set(int index, Item item) { items.setElementAt(item, index); }
    public int size() { return items.size(); }
    public void delete(int index) { items.removeElementAt(index); }
    public void deleteAll() { items.removeAllElements(); }
    public void setItemStateListener(ItemStateListener l) { itemStateListener = l; }
}
