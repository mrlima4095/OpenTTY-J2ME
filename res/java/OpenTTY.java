import java.util.*;

public class OpenTTY {
    public long uptime = System.currentTimeMillis();
    public boolean useCache = true, debug = false;
    // |
    // System Objects
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Object shell;
    
    public Hashtable attributes = new Hashtable<String, String>(), sys = new Hashtable<String, Hashtable>(), network = new Hashtable<String, Object>();
    public String username = "", build = "2026-g1.18-01x10";
    // |

    // |
    // String Utils
    // | (Get Command Parts)
    public String getCommand(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return text; } else { return text.substring(0, spaceIndex); } }
    public String getArgument(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return text.substring(spaceIndex + 1).trim(); } }
    // | (Modify String)
    public String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    public String env(String text, Hashtable scope) { if (scope != null) { text = replace(text, "$PATH", (String) scope.get("PWD")); for (Enumeration keys = scope.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) scope.get(key)); } } return env(text); }
    public String env(String text) { text = replace(text, "$USER", username); for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) attributes.get(key)); } text = replace(text, "$.", "$"); return escape(text); }
    public String escape(String text) { text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); text = replace(text, "\\b", "\b"); text = replace(text, "\\\\", "\\"); text = replace(text, "\\.", "\\"); return text; }
    public String getCatch(Throwable e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : e.getClass().getName() + ": " + message; }
    // |
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String dirname(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(0, lastSlashIndex + 1); }
    // |
    public String getcontent(String file, Hashtable scope) { return file.startsWith("/") ? read(file, scope) : read(((String) scope.get("PWD")) + file, scope); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }
    // | (Arrays)
    public String join(String[] array, String spacer, int start) { if (array == null || array.length == 0 || start >= array.length) { return ""; } StringBuffer sb = new StringBuffer(); for (int i = start; i < array.length; i++) { sb.append(array[i]).append(spacer); } return sb.toString().trim(); }
    private int indexOf(String key, String[] array) { for (int i = 0; i < array.length; i++) { if (array[i].equals(key)) { return i; } } return -1; }
    public String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    public String[] splitArgs(String content) { Vector args = new Vector(); boolean inQuotes = false; int start = 0; for (int i = 0; i < content.length(); i++) { char c = content.charAt(i); if (c == '"') { inQuotes = !inQuotes; continue; } if (!inQuotes && c == ' ') { if (i > start) { args.addElement(getpattern(content.substring(start, i))); } start = i + 1; } } if (start < content.length()) { args.addElement(getpattern(content.substring(start))); } String[] result = new String[args.size()]; args.copyInto(result); return result; }
    // | (Converting String on Map)
    public Hashtable parseProperties(String text) { if (text == null) { return new Hashtable(); } Hashtable properties = new Hashtable(); String[] lines = split(text, '\n'); for (int i = 0; i < lines.length; i++) { String line = lines[i]; if (line.startsWith("#")) { } else { int equalIndex = line.indexOf('='); if (equalIndex > 0 && equalIndex < line.length() - 1) { properties.put(line.substring(0, equalIndex).trim(), getpattern(line.substring(equalIndex + 1).trim())); } } } return properties; }
    // | (String <> Number)
    public int getNumber(String s, int fallback, Object stdout) { try { return Integer.valueOf(s); } catch (Exception e) { if (stdout != null) { print(getCatch(e), stdout); } return fallback; } }
    public Double getNumber(String s) { try { return Double.valueOf(s); } catch (NumberFormatException e) { return null; } }
    // |
    // | (Generators)
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    public Hashtable genprocess(String name, int id, Hashtable signal) { 
        Hashtable proc = new Hashtable(); 
        proc.put("name", name); 
        proc.put("owner", id == 0 ? "root" : username); 
        if (signal != null) { proc.put("signals", signal); } 
        
        return proc;
    }
    public Hashtable gensignals(Object collector) {
        Hashtable signal = new Hashtable();

        if (collector != null) { signal.put("TERM", collector); }

        return signal;
    }
    // | (Trackers)
    public Hashtable getprocess(String pid) { return sys.containsKey(pid) ? (Hashtable) sys.get(pid) : null; }
    public Object getobject(String pid, String item) { return sys.containsKey(pid) ? ((Hashtable) sys.get(pid)).get(item) : null; }
    public Object getsignal(String pid, Object signal) { if (sys.containsKey(pid)) { Hashtable signals = (Hashtable) getobject(pid, "signals"); if (signals != null && signals.containsKey(signal)) { return signals.get(signal); } else { return null; } } else { return null; } } 
    public String getpid(String name) { for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); if (name.equals((String) ((Hashtable) sys.get(PID)).get("name"))) { return PID; } } return null; } 
    
}
