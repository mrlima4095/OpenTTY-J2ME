package javax.microedition.lcdui;

public class Ticker {
    private String text;
    public Ticker(String text) { this.text = text == null ? "" : text; }
    public String getString() { return text; }
    public void setString(String text) { this.text = text == null ? "" : text; }
}