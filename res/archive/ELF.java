public class ELF {
    public ELF(OpenTTY midlet, Object stdout, Hashtable scope, int id, String pid, Hashtable proc) { }
    
    public String getPid() { return ""; }
    public void kill() {  }
    
    public boolean load(InputStream is) throws Exception { return false; }
    public boolean load(byte[] elfData) throws Exception { return false; }
    
    public Hashtable run() {
        Hashtable ITEM = new Hashtable();

        ITEM.put("status", new Double(1));
        return ITEM;
    }
}
