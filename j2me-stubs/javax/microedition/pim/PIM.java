package javax.microedition.pim;
public class PIM {
    public static final int CONTACT_LIST = 1;
    public static final int EVENT_LIST = 2;
    public static final int TODO_LIST = 3;
    public static PIM getInstance() { return null; }
    public String[] listPIMLists(int listType) { return null; }
    public PIMList openPIMList(int listType, int mode, String name) throws PIMException { return null; }
}
