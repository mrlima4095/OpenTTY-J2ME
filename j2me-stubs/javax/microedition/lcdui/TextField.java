package javax.microedition.lcdui;
public class TextField extends Item {
    public static final int ANY = 0;
    public static final int EMAILADDR = 1;
    public static final int NUMERIC = 2;
    public static final int PHONENUMBER = 3;
    public static final int URL = 4;
    public static final int DECIMAL = 5;
    public static final int PASSWORD = 0x10000;
    public TextField(String label, String text, int maxSize, int constraints) {}
    public void setString(String text) {}
    public String getString() { return null; }
    public String getLabel() { return null; }
}
