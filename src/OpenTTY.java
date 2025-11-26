import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.control.*;
import javax.microedition.io.file.*;
import javax.wireless.messaging.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// OpenTTY MIDlet
public class OpenTTY extends MIDlet {
    // Behavior Settings
    public long uptime = System.currentTimeMillis();
    public boolean classpath = true, useCache = true;
    // |
    // System Objects
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    
    public Hashtable attributes = new Hashtable(), 
                     fs = new Hashtable(), 
                     sys = new Hashtable(), 
                     tmp = new Hashtable(), 
                     cache = new Hashtable(), cacheLua = new Hashtable(), 
                     sessions = new Hashtable(),
                     globals = new Hashtable();
    public String username = read("/home/OpenRMS"), logs = "", build = "2025-1.17-03x04"; 
    // |
    // Graphics
    public Display display = Display.getDisplay(this);
    public Form xterm = new Form("SandBox");
    public StringItem stdout = new StringItem("", "");
    public TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    public Command BACK = new Command("Back", Command.BACK, 1), EXECUTE = new Command("Run", Command.OK, 0);
    // |
    // MIDlet Loader
    // |
    // | (Triggers)
    public void startApp() {
        if (sys.containsKey("1")) { }
        else {            
            try { 
                Hashtable proc = new Hashtable(), args = new Hashtable(); args.put(new Double(0), "/bin/init"); globals.put("PWD", "/home/");
                proc.put("name", "init"); proc.put("owner", "root");

                //Lua lua = new Lua(this, 0, "1", proc, stdout, globals); 
                warn("hello", "working! but not booting");
                //sys.put("1", proc); lua.globals.put("arg", args);

                //lua.tokens = lua.tokenize(read("/bin/init")); 
                
                //while (lua.peek().type != 0) { Object res = lua.statement(globals); if (lua.doreturn) { break; } }
            } catch (NullPointerException e) {
    // Log detalhado para identificar a variável nula
    String debugInfo = "Debug - midlet: " + (this != null) + 
                      ", stdout: " + (stdout != null) + 
                      ", globals: " + (globals != null) +
                      ", proc: " + (proc != null);
    warn("NPE Debug", debugInfo);
    throw e; // Re-lança para ver stack trace completo
}
            catch (Exception e) { warn("SandBox", getCatch(e)); } 
            catch (Throwable e) { warn("Kernel Panic", e.getMessage() != null ? e.getMessage() : e.getClass().getName()); }
        }
    }
    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { notifyDestroyed(); }
    // |
    // Control Thread
    public OpenTTY getInstance() { return this; }
    public String getThreadName(Thread thr) { String name = thr.getName(); String[] generic = { "Thread-0", "Thread-1", "MIDletEventQueue", "main" }; for (int i = 0; i < generic.length; i++) { if (name.equals(generic[i])) { name = "MIDlet"; break; } } return name; }
    public int setLabel() { stdin.setLabel(username + " " + ((String) globals.get("PWD")) + " " + (username.equals("root") ? "#" : "$")); return 0; }
    public class MIDletControl implements CommandListener {
        public static final int SIGNUP = 1;

        private int MOD = -1;
        private boolean enable = true, asking_user = username.equals(""), asking_passwd = passwd().equals("");
        private Form monitor;
        private Object stdout;
        private TextField USER, PASSWD;
        private Command BACK = new Command("Back", Command.BACK, 1), LOGIN, EXIT;

        public MIDletControl() { MOD = SIGNUP; monitor = new Form("Login"); monitor.append(env("Welcome to OpenTTY $VERSION\nCopyright (C) 2025 - Mr. Lima\n\n" + (asking_user && asking_passwd ? "Create your credentials!" : asking_user ? "Create an user to access OpenTTY!" : asking_passwd ? "Create a password!" : "")).trim()); if (asking_user) { monitor.append(USER = new TextField("Username", "", 256, TextField.ANY)); } if (asking_passwd) { monitor.append(PASSWD = new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); } monitor.addCommand(LOGIN = new Command("Login", Command.OK, 1)); monitor.addCommand(EXIT = new Command("Exit", Command.SCREEN, 2)); monitor.setCommandListener(this); display.setCurrent(monitor); }

        public void commandAction(Command c, Displayable d) {
            if (MOD == SIGNUP) {
                if (c == LOGIN) {
                    String password = asking_passwd ? PASSWD.getString().trim() : "";

                    if (asking_user) { username = USER.getString().trim(); }
                    if (asking_user && username.equals("") || asking_passwd && password.equals("")) { warn(xterm.getTitle(), "Missing credentials!"); } 
                    else if (username.equals("root")) { USER.setString(""); warn(xterm.getTitle(), "Invalid username!"); } 
                    else {
                        if (asking_user) { write("/home/OpenRMS", username.getBytes(), 0); }
                        if (asking_passwd) { writeRMS("OpenRMS", String.valueOf(password.hashCode()).getBytes(), 2); }

                        display.setCurrent(xterm);
                        setLabel();
                    }
                } 
                else if (c == EXIT) { destroyApp(true); }
            }
        }

        public static String passwd() { return loadRMS("OpenRMS", 2); }
    }
    // |
    // String Utils
    // | (Get Command Parts)
    public String getCommand(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return text; } else { return text.substring(0, spaceIndex); } }
    public String getArgument(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return text.substring(spaceIndex + 1).trim(); } }
    // | (Modify String)
    public String replace(String source, String target, String replacement) { StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    public String env(String text, Hashtable scope) { if (scope != null) { text = replace(text, "$PATH", (String) scope.get("PWD")); for (Enumeration keys = scope.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) scope.get(key)); } } return env(text); }
    public String env(String text) { text = replace(text, "$PATH", (String) globals.get("PWD")); text = replace(text, "$USERNAME", username); text = replace(text, "$TITLE", xterm.getTitle()); text = replace(text, "$PROMPT", stdin.getString()); for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) attributes.get(key)); } text = replace(text, "$.", "$"); return escape(text); }
    public String escape(String text) { text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); text = replace(text, "\\b", "\b"); text = replace(text, "\\\\", "\\"); text = replace(text, "\\.", "\\"); return text; }
    public String getCatch(Throwable e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : message; }
    // |
    private String basename(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(lastSlashIndex + 1); }
    private String dirname(String path) { if (path == null || path.length() == 0) { return ""; } if (path.endsWith("/")) { path = path.substring(0, path.length() - 1); } int lastSlashIndex = path.lastIndexOf('/'); if (lastSlashIndex == -1) { return path; } return path.substring(0, lastSlashIndex + 1); }
    // | 
    private String extractTitle(String htmlContent, String fallback) { return extractTag(htmlContent, "title", fallback); }
    private String extractTag(String htmlContent, String tag, String fallback) { String startTag = "<" + tag + ">", endTag = "</" + tag + ">"; int start = htmlContent.indexOf(startTag), end = htmlContent.indexOf(endTag); if (start != -1 && end != -1 && end > start) { return htmlContent.substring(start + startTag.length(), end).trim(); } else { return fallback; } }
    private String html2text(String htmlContent) { StringBuffer text = new StringBuffer(); boolean inTag = false, inStyle = false, inScript = false, inTitle = false; for (int i = 0; i < htmlContent.length(); i++) { char c = htmlContent.charAt(i); if (c == '<') { inTag = true; if (htmlContent.regionMatches(true, i, "<title>", 0, 7)) { inTitle = true; } else if (htmlContent.regionMatches(true, i, "<style>", 0, 7)) { inStyle = true; } else if (htmlContent.regionMatches(true, i, "<script>", 0, 8)) { inScript = true; } else if (htmlContent.regionMatches(true, i, "</title>", 0, 8)) { inTitle = false; } else if (htmlContent.regionMatches(true, i, "</style>", 0, 8)) { inStyle = false; } else if (htmlContent.regionMatches(true, i, "</script>", 0, 9)) { inScript = false; } } else if (c == '>') { inTag = false; } else if (!inTag && !inStyle && !inScript && !inTitle) { text.append(c); } } return text.toString().trim(); }
    // |
    public String getcontent(String file, Hashtable scope) { return file.startsWith("/") ? read(file) : read(((String) scope.get("PWD")) + file); }
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
    public Hashtable gensignals(String collector) {
        Hashtable signal = new Hashtable();

        if (collector != null) { signal.put("TERM", collector); }

        return signal;
    }
    // | (Trackers)
    public Hashtable getprocess(String pid) { return sys.containsKey(pid) ? (Hashtable) sys.get(pid) : null; }
    public Object getobject(String pid, String item) { return sys.containsKey(pid) ? ((Hashtable) sys.get(pid)).get(item) : null; }
    public String getsignal(String pid, Object signal) { if (sys.containsKey(pid)) { Hashtable signals = (Hashtable) getobject(pid, "signals"); if (signals != null && signals.containsKey(signal)) { return (String) signals.get(signal); } else { return null; } } else { return null; } } 
    public String getpid(String name) { for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); if (name.equals((String) ((Hashtable) sys.get(PID)).get("name"))) { return PID; } } return null; } 
    // | (Renders)
    private String renderJSON(Object obj, int indent) { StringBuffer json = new StringBuffer(); String pad = ""; for (int i = 0; i < indent; i++) { pad += "  "; } if (obj instanceof Hashtable) { Hashtable map = (Hashtable) obj; json.append("{\n"); Enumeration keys = map.keys(); while (keys.hasMoreElements()) { String key = (String) keys.nextElement(); Object val = map.get(key); json.append(pad + "  \"" + key + "\": " + renderJSON(val, indent + 1)); if (keys.hasMoreElements()) { json.append(","); } json.append("\n"); } json.append(pad + "}"); } else if (obj instanceof Vector) { Vector list = (Vector) obj; json.append("[\n"); for (int i = 0; i < list.size(); i++) { json.append(pad + "  " + renderJSON(list.elementAt(i), indent + 1)); if (i < list.size() - 1) { json.append(","); } json.append("\n"); } json.append(pad + "]"); } else if (obj instanceof String) { String s = (String) obj; s = replace(s, "\n", "\\n"); s = replace(s, "\r", "\\r"); s = replace(s, "\t", "\\t"); json.append("\"" + s + "\""); } else { json.append(String.valueOf(obj)); } return json.toString(); }
    // | (Logging)
    public int MIDletLogs(String command, int id, Object stdout) { command = env(command.trim()); String mainCommand = getCommand(command), argument = getArgument(command); if (mainCommand.equals("")) { } else if (mainCommand.equals("clear")) { logs = ""; } else if (mainCommand.equals("swap")) { write(argument.equals("") ? "logs" : argument, logs, id); } else if (mainCommand.equals("add")) { String level = getCommand(argument).toLowerCase(), message = getArgument(argument); if (message.equals("")) { } else { if (level.equals("info") || level.equals("warn") || level.equals("debug") || level.equals("error")) { logs += "[" + level.toUpperCase() + "] " + split(new java.util.Date().toString(), ' ')[3] + " " + message + "\n"; } else { print("log: add: " + level + ": not found", stdout); return 127; } } } else { print("log: " + mainCommand + ": not found", stdout); return 127; } return 0; }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // API 002 - X Server
    // | (Client)
    // | (Window-Based Interfaces)
    public int warn(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    // | (Font Generator)
    public Font genFont(String params) { if (params == null || params.length() == 0 || params.equals("default")) { return Font.getDefaultFont(); } int face = Font.FACE_SYSTEM, style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; String[] tokens = split(params, ' '); for (int i = 0; i < tokens.length; i++) { String token = tokens[i].toLowerCase(); if (token.equals("system")) { face = Font.FACE_SYSTEM; } else if (token.equals("monospace")) { face = Font.FACE_MONOSPACE; } else if (token.equals("proportional")) { face = Font.FACE_PROPORTIONAL; } else if (token.equals("bold")) { style |= Font.STYLE_BOLD; } else if (token.equals("italic")) { style |= Font.STYLE_ITALIC; } else if (token.equals("ul") || token.equals("underline") || token.equals("underlined")) { style |= Font.STYLE_UNDERLINED; } else if (token.equals("small")) { size = Font.SIZE_SMALL; } else if (token.equals("medium")) { size = Font.SIZE_MEDIUM; } else if (token.equals("large")) { size = Font.SIZE_LARGE; } } Font f = Font.getFont(face, style, size); return f == null ? Font.getDefaultFont() : f; }
    // |
    public void print(String message, Object stdout) { 
        if (stdout == null) { }
        else if (stdout instanceof StringItem) { 
            String current = ((StringItem) stdout).getText(), output = current == null || current.length() == 0 ? message : current + "\n" + message; 
            ((StringItem) stdout).setText(output); }
        else if (stdout instanceof StringBuffer) { ((StringBuffer) stdout).append("\n").append(message); }
        else if (stdout instanceof String) { write((String) stdout, read((String) stdout) + "\n" + message, 1000); }
        else if (stdout instanceof OutputStream) {
            try { ((OutputStream) stdout).write((message + "\n").getBytes());  ((OutputStream) stdout).flush(); } 
            catch (Exception e) { }
        }
    }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // API 003 - File System
    // |
    private boolean file(String filename) {
        if (filename.startsWith("/home/")) {
            String[] recordStores = RecordStore.listRecordStores();
            if (recordStores != null) { for (int i = 0; i < recordStores.length; i++) { if (recordStores[i].equals(filename.substring(6))) { return true; } } }
        }
        else if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename.substring(5)); }
        else if (filename.startsWith("/mnt/")) {
            try {
                FileConnection conn = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ);
                boolean status = conn.exists(); conn.close(); 
                return status;
            } catch (Exception e) { return false; }
        }
        else if (filename.endsWith("/")) { return fs.containsKey(filename); }
        else { 
            String dir = dirname(filename); 
            if (dir.equals("/bin/") || dir.equals("/etc/") || dir.equals("/lib/")) {
                String content = loadRMS("OpenRMS", dir.equals("/bin/") ? 3 : dir.equals("/etc/") ? 5 : 4);
                if (content.indexOf("[\1BEGIN:" + basename(filename) + "\1]") != -1) { return true; }
            }
            
            return (fs.containsKey((dir)) && ((Vector) fs.get(dir)).indexOf(basename(filename)) != -1); 
        }
        
        return false;
    }    
    // | (Read)
    public InputStream getInputStream(String filename) throws Exception {
        if (filename.startsWith("/home/")) {
            RecordStore rs = null;
            try {
                rs = RecordStore.openRecordStore(filename.substring(6), false);
                if (rs.getNumRecords() > 0) { return new ByteArrayInputStream(rs.getRecord(1)); }
            } finally { if (rs != null) { rs.closeRecordStore(); } }

            return null;
        } 
        else if (filename.startsWith("/mnt/")) { return ((FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ)).openInputStream(); } 
        else if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? new ByteArrayInputStream(((String) tmp.get(filename)).getBytes("UTF-8")) : null; } 
        else {
            if (filename.startsWith("/dev/")) {
                filename = filename.substring(5);
                String content = filename.equals("random") ? String.valueOf(random.nextInt(256)) : filename.equals("stdin") ? stdin.getString() : filename.equals("stdout") ? stdout.getText() : filename.equals("null") ? "\r" : filename.equals("zero") ? "\0" : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/dev/" + filename;
            }
            else if (filename.startsWith("/bin/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/bin/" + filename)) { return new ByteArrayInputStream(((String) cache.get("/bin/" + filename)).getBytes("UTF-8")); }

                String content = read(filename, loadRMS("OpenRMS", 3));
                if (content != null) { if (useCache) { cache.put("/bin/" + filename, content); } return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/bin/" + filename;
            }
            else if (filename.startsWith("/etc/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/etc/" + filename)) { return new ByteArrayInputStream(((String) cache.get("/etc/" + filename)).getBytes("UTF-8")); }

                String content = read(filename, loadRMS("OpenRMS", 5));
                if (content != null) { if (useCache) { cache.put("/etc/" + filename, content); } return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/etc/" + filename;
            }
            else if (filename.startsWith("/lib/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/lib/" + filename)) { return new ByteArrayInputStream(((String) cache.get("/etc/" + filename)).getBytes("UTF-8")); }

                String content = read(filename, loadRMS("OpenRMS", 4));
                if (content != null) { if (useCache) { cache.put("/lib/" + filename, content); } return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/lib/" + filename;
            }

            InputStream is = getClass().getResourceAsStream(filename);
            return is;
        }
    }
    public Image readImg(String filename) { try { InputStream is = getInputStream(filename); Image img = Image.createImage(is); is.close(); return img; } catch (Exception e) { return Image.createImage(16, 16); } }
    public String read(String filename) {
        try {
            if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? (String) tmp.get(filename) : ""; }
            InputStream is = getInputStream(filename);
            if (is == null) { return ""; }
            
            InputStreamReader reader = new InputStreamReader(is, "UTF-8");
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = reader.read()) != -1) { sb.append((char) ch); }
            reader.close();
            is.close();
            
            return filename.startsWith("/home/") ? sb.toString() : env(sb.toString());
        } catch (Exception e) { return ""; }
    }
    public static String loadRMS(String filename, int index) { try { RecordStore RMS = RecordStore.openRecordStore(filename, true); if (RMS.getNumRecords() >= index) { byte[] data = RMS.getRecord(index); if (data != null) { return new String(data); } } if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return ""; }
    // | (Write)
    public int write(String filename, String data, int id) { return write(filename, data.getBytes(), id); }
    public int write(String filename, byte[] data, int id) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/mnt/")) { try { FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!CONN.exists()) { CONN.create(); } OutputStream OUT = CONN.openOutputStream(); OUT.write(data); OUT.flush(); OUT.close(); CONN.close(); } catch (Exception e) { return (e instanceof SecurityException) ? 13 : 1; } } 
        else if (filename.startsWith("/home/")) { return writeRMS(filename.substring(6), data, 1); } 
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4); filename = filename.substring(5);

            if (filename.equals("")) { return 2; } 
            else if (id != 0) { return 13; }
            else if (filename.equals("sh") || filename.equals("lua")) { return 5; }
            else { if (useCache) { cache.put("/" + base + "/" + filename, new String(data)); } return addFile(filename, new String(data), loadRMS("OpenRMS", base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4), base); }
        }
        else if (filename.startsWith("/dev/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else if (filename.equals("null")) { } else if (filename.equals("stdin")) { stdin.setString(new String(data)); } else if (filename.equals("stdout")) { stdout.setText(new String(data)); } else { return 5; } }
        else if (filename.startsWith("/tmp/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else { tmp.put(filename, new String(data)); } }
        else if (filename.startsWith("/")) { return 5; }
        
        return 0; 
    }
    public int writeRMS(String filename, byte[] data, int index) { try { RecordStore CONN = RecordStore.openRecordStore(filename, true); while (CONN.getNumRecords() < index) { CONN.addRecord("".getBytes(), 0, 0); } CONN.setRecord(index, data, 0, data.length); if (CONN != null) { CONN.closeRecordStore(); } } catch (Exception e) { return 1; } return 0; }
    public int deleteFile(String filename, int id, Object stdout) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/home/")) { 
            try { 
                filename = filename.substring(6); 
                if (filename.equals("")) { return 2; }
                if (filename.equals("OpenRMS")) { print("Permission denied!", stdout); return 13; } 
                
                RecordStore.deleteRecordStore(filename); 
            } 
            catch (RecordStoreNotFoundException e) { print("rm: " + filename + ": not found", stdout); return 127; } 
            catch (Exception e) { print(getCatch(e), stdout); return 1; } 
        }
        else if (filename.startsWith("/mnt/")) { 
            try { 
                FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); 
                if (CONN.exists()) { CONN.delete(); } 
                else { print("rm: " + basename(filename) + ": not found", stdout); return 127; } 
                
                CONN.close(); 
            } 
            catch (Exception e) { print(getCatch(e), stdout); return e instanceof SecurityException ? 13 : 1; } 
        }
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4), name = filename.substring(5);
            if (name.equals("")) { return 2; }
            if (id != 0) { print("Permission denied!", stdout); return 13; }

            int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
            String content = loadRMS("OpenRMS", index);
            if (content.indexOf("[\1BEGIN:" + name + "\1]") == -1) { print("read-only storage", stdout); return 5; }

            if (useCache) { cache.remove("/" + base + "/" + name); }
            return writeRMS("OpenRMS", delFile(name, content).getBytes(), index);
        }
        else if (filename.startsWith("/tmp/")) {
            filename = filename.substring(5);
            if (filename.equals("")) { }
            else if (tmp.containsKey(filename)) { tmp.remove(filename); }
            else { print("rm: " + filename + ": not found", stdout); return 127; }
        }
        else if (filename.startsWith("/")) { print("read-only storage", stdout); return 5; }
        
        return 0; 
    }
    // | (Archive Structures)
    public int addFile(String filename, String content, String archive, String base) { return writeRMS("OpenRMS", (delFile(filename, archive) + "[\1BEGIN:" + filename + "\1]\n" + content + "\n[\1END\1]\n").getBytes(), base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4); }
    public String delFile(String filename, String content) {
        String startTag = "[\1BEGIN:" + filename + "\1]";
        int start = content.indexOf(startTag);
        if (start == -1) { return content; }

        int end = content.indexOf("[\1END\1]", start);
        if (end == -1) { return content; }

        end += "[\1END\1]".length();
        return content.substring(0, start) + content.substring(end);
    }
    public String read(String filename, String content) {
        String startTag = "[\1BEGIN:" + filename + "\1]";
        int start = content.indexOf(startTag);
        if (start == -1) { return null; }

        start += startTag.length() + 1;
        int end = content.indexOf("[\1END\1]", start);
        if (end == -1) { return null; }

        return content.substring(start, end).trim();
    }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // Java Virtual Machine
    public int javaClass(String name) { try { Class.forName(name); return 0; } catch (ClassNotFoundException e) { return 3; } } 
    public String getName() { String s; StringBuffer BUFFER = new StringBuffer(); if ((s = System.getProperty("java.vm.name")) != null) { BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor")); if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); } if ((s = System.getProperty("java.vm.specification.name")) != null) { BUFFER.append('\n').append(s); } } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) { BUFFER.append("J9 VM, IBM (").append(s).append(')'); if ((s = System.getProperty("java.fullversion")) != null) { BUFFER.append("\n\n").append(s); } } else if ((s = System.getProperty("com.oracle.jwc.version")) != null) { BUFFER.append("OJWC v").append(s).append(", Oracle"); } else if (javaClass("com.sun.cldchi.jvm.JVM") == 0) { BUFFER.append("CLDC Hotspot Implementation, Sun"); } else if (javaClass("com.sun.midp.Main") == 0) { BUFFER.append("KVM, Sun (MIDP)"); } else if (javaClass("com.sun.cldc.io.ConsoleOutputStream") == 0) { BUFFER.append("KVM, Sun (CLDC)"); } else if (javaClass("com.jblend.util.SortedVector") == 0) { BUFFER.append("JBlend, Aplix"); } else if (javaClass("com.jbed.io.CharConvUTF8") == 0) { BUFFER.append("Jbed, Esmertec/Myriad Group"); } else if (javaClass("MahoTrans.IJavaObject") == 0) { BUFFER.append("MahoTrans"); } else { BUFFER.append("Unknown"); } return BUFFER.append('\n').toString(); }
}            
// |
// EOF