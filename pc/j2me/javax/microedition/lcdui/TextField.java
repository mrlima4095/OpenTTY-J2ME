package javax.microedition.lcdui;

public class TextField extends Item {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int PASSWORD = 0x10000;
    public static final int NON_PREDICTIVE = 0x20000;
    public static final int SENSITIVE = 0x40000;
    public static final int UNEDITABLE = 0x80000;

    private String value = "";
    private int maxSize;
    private int constraints;

    public TextField(String label, String text, int maxSize, int constraints) {
        this.label = label == null ? "" : label;
        this.value = text == null ? "" : text;
        this.maxSize = maxSize > 0 ? maxSize : Integer.MAX_VALUE;
        this.constraints = constraints;
    }

    public String getString() { return value; }

    public void setString(String text) {
        if (value.equals(text == null ? "" : text)) { return; }
        value = text == null ? "" : text;
        touch();
    }

    /** Model-only write used by the Swing glue while the user types; does not
     *  trigger a repaint (which would destroy focus mid-editing). */
    void updateFromUi(String text) { value = text == null ? "" : text; }

    public int size() { return value.length(); }
    public int getMaxSize() { return maxSize; }
    public int setMaxSize(int n) { maxSize = n > 0 ? n : Integer.MAX_VALUE; return maxSize; }
    public int getConstraints() { return constraints; }
    public void setConstraints(int c) { constraints = c; }
    public char[] getChars(char[] data) { return value.toCharArray(); }
    public void setChars(char[] data, int offset, int length) {
        setString(new String(data, offset, length));
    }
    public void insert(String src, int position) {
        if (src == null) { return; }
        setString(value.substring(0, Math.min(position, value.length())) + src + value.substring(Math.min(position, value.length())));
    }
    public void insert(char[] data, int offset, int length, int position) {
        insert(new String(data, offset, length), position);
    }
    public void delete(int offset, int length) {
        int from = Math.min(offset, value.length()), to = Math.min(offset + length, value.length());
        if (to > from) { setString(value.substring(0, from) + value.substring(to)); }
    }
}
