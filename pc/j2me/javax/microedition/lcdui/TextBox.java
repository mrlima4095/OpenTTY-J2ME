package javax.microedition.lcdui;

public class TextBox extends Displayable {
    TextField field;

    public TextBox(String title, String text, int maxSize, int constraints) {
        this.title = title == null ? "" : title;
        this.field = new TextField(null, text, maxSize, constraints);
    }

    public String getString() { return field.getString(); }
    public void setString(String text) { field.setString(text); }
    public void insert(String src, int pos) { field.insert(src, pos); }
    public void delete(int offset, int length) { field.delete(offset, length); }
    public int size() { return field.size(); }
    public int getMaxSize() { return field.getMaxSize(); }
    public int setMaxSize(int max) { return field.setMaxSize(max); }
    public void setConstraints(int constraints) { field.setConstraints(constraints); }
    public int getConstraints() { return field.getConstraints(); }
}
