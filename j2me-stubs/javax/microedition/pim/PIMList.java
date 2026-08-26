package javax.microedition.pim;
import java.util.Enumeration;
public interface PIMList {
    String[] getCategories();
    int maxCategories();
    int maxValues(int field);
    PIMItem createItem();
    void close();
}
