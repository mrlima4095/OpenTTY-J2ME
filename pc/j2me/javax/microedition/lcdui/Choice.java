package javax.microedition.lcdui;

public interface Choice {
    int EXCLUSIVE = 1;
    int MULTIPLE = 2;
    int IMPLICIT = 3;
    int POPUP = 4;

    int append(String label, Image image);
    void insert(int index, String label, Image image);
    void delete(int index);
    void deleteAll();
    int size();
    String getString(int index);
    void set(int index, String label, Image image);
    boolean isSelected(int index);
    int getSelectedIndex();
    int getSelectedFlags(boolean[] flags);
    void setSelectedIndex(int index, boolean selected);
    void setSelectedFlags(boolean[] flags);
}