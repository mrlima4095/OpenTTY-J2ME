package javax.microedition.lcdui;
public class Form extends Screen {
    public Form(String title) {}
    public int append(String str) { return 0; }
    public int append(Item item) { return 0; }
    public int append(Image img) { return 0; }
    public int append(String str, Image img) { return 0; }
    public int size() { return 0; }
    public Item get(int itemNum) { return null; }
    public void deleteAll() {}
    public void setItemStateListener(ItemStateListener iListener) {}
}
