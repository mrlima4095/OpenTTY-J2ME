/**
 * Default-package shim that resolves the List ambiguity for src/OpenTTY.java
 * and src/Lua.java (which import both java.util.* and lcdui.* via wildcards).
 *
 * CLDC has no java.util.List, so on-device the wildcard lcdui.List wins. On
 * desktop Java (which has java.util.List), this same-package type shadows the
 * wildcard-imported collisions and IS-A javax.microedition.lcdui.List.
 */
public class List extends javax.microedition.lcdui.List {
    public List(String title, int listType) {
        super(title, listType);
    }
}