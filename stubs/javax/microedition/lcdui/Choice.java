package javax.microedition.lcdui;

public interface Choice {
    int EXCLUSIVE = 1;
    int MULTIPLE = 2;
    int IMPLICIT = 3;
    int POPUP = 4;

    int append(String element, Image image);
    void insert(int elementNum, String element, Image image);
    void delete(int elementNum);
    void set(int elementNum, String element, Image image);
    String getString(int elementNum);
    Image getImage(int elementNum);
    int size();
    boolean isSelected(int elementNum);
    int getSelectedIndex();
    void setSelectedIndex(int elementNum, boolean selected);
    int getSelectedFlags(boolean[] flags);
    void setSelectedFlags(boolean[] flags);
}