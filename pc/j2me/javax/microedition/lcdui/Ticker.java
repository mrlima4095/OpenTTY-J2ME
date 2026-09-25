package javax.microedition.lcdui;

public class Ticker {
    private String text;
    public Ticker(String str) { text = str == null ? "" : str; }
    public String getString() { return text; }
    public void setString(String str) { text = str == null ? "" : str; }
}
