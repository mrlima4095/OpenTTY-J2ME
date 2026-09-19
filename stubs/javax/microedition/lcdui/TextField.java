package javax.microedition.lcdui;

public class TextField extends Item {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int PASSWORD = 0x10000;

    private String text = "";
    private int maxSize = 256;
    private int constraints = ANY;

    public TextField(String label, String text, int maxSize, int constraints) {
        setLabel(label);
        setString(text);
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    public String getString() { return text; }

    /** Called by the Swing renderer's DocumentListener; avoids the touch loop. */
    void _setModelText(String value) {
        if (value == null) { value = ""; }
        this.text = DesktopLcdui._limit(value, maxSize);
        _changed();
    }

    public void setString(String text) {
        if (text == null) { text = ""; }
        if (isNumeric()) {
            StringBuffer out = new StringBuffer();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '-' || (c >= '0' && c <= '9')) { out.append(c); }
            }
            text = out.toString();
        }
        this.text = DesktopLcdui._limit(text, maxSize);
        _touch();
    }

    public boolean isEditable() { return true; }

    public int getConstraints() { return constraints; }

    public void setConstraints(int constraints) { this.constraints = constraints; }

    public int getMaxSize() { return maxSize; }

    public int setMaxSize(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
        this.text = DesktopLcdui._limit(text, this.maxSize);
        return this.maxSize;
    }

    private boolean isNumeric() {
        int base = constraints & ~PASSWORD;
        return base == NUMERIC || base == PHONENUMBER;
    }

    public void insert(String data, int position) {
        if (data == null) { return; }
        StringBuffer sb = new StringBuffer(text);
        if (position < 0) { position = 0; }
        if (position > sb.length()) { position = sb.length(); }
        sb.insert(position, data);
        setString(sb.toString());
    }

    public void delete(int offset, int length) {
        if (offset < 0 || offset >= text.length() || length <= 0) { return; }
        int end = Math.min(offset + length, text.length());
        setString(text.substring(0, offset) + text.substring(end));
    }
}