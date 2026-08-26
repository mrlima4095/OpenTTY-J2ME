package javax.microedition.pim;
public class PIMException extends Exception {
    public static final int GENERAL_ERROR = 0;
    public static final int LIMITCEEDED_ERROR = 1;
    public static final int MAX_CATEGORIES_EXCEEDED = 2;
    public static final int MAXIMUM_ITEMS_EXCEEDED = 3;
    public static final int ITEM_NOT_FOUND = 4;
    public PIMException() { super(); }
    public PIMException(String message) { super(message); }
    public int getErrorCode() { return GENERAL_ERROR; }
}
