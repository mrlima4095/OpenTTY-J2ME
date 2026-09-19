package javax.microedition.lcdui;

public class TextBox extends Displayable {
    private String text = "";
    private int maxSize = 31522;
    private int constraints = TextField.ANY;

    public TextBox(String title, String text, int maxSize, int constraints) {
        setTitle(title);
        this.text = text == null ? "" : text;
        this.maxSize = maxSize;
        this.constraints = constraints;
    }

    public String getString() {
        if (DesktopLcdui._isFocused(this)) {
            String live = DesktopLcdui._liveText(this);
            if (live != null) { text = live; }
        }
        return text;
    }

    public void setString(String text) {
        this.text = text == null ? "" : text;
        _repaint();
    }

    String _rawString() { return text; }

    /** Called by the renderer's DocumentListener without a repaint loop. */
    void _setTextNoRepaint(String text) {
        this.text = text == null ? "" : DesktopLcdui._limit(text, maxSize);
    }

    public int getMaxSize() { return maxSize; }

    public int setMaxSize(int maxSize) {
        this.maxSize = Math.max(1, maxSize);
        this.text = DesktopLcdui._limit(text, this.maxSize);
        return this.maxSize;
    }

    public int size() { return text.length(); }

    public String getChars(int offset, int length) {
        return offset < 0 || offset + length > text.length() ? "" : text.substring(offset, offset + length);
    }

    public void setChars(char[] data, int offset, int length) {
        setString(new String(data, offset, length));
    }

    public void insert(String data, int position) { setString(text.substring(0, position) + data + text.substring(position)); }

    public void delete(int offset, int length) {
        setString(text.substring(0, offset) + text.substring(offset + length));
    }

    public int getConstraints() { return constraints; }

    public void setConstraints(int constraints) { this.constraints = constraints; }
}