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
public class OpenTTY extends MIDlet implements CommandListener {
    // Behavior Settings
    public long uptime = System.currentTimeMillis();
    public boolean useCache = true;
    // |
    // System Objects
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Object shell;

    public Hashtable attributes = new Hashtable(), fs = new Hashtable(), sys = new Hashtable(), tmp = new Hashtable(), cache = new Hashtable(), cacheLua = new Hashtable(), graphics = new Hashtable(), network = new Hashtable(), globals = new Hashtable();
    public String username = read("/home/OpenRMS"), build = "2026-1.17-03x08";
    // |
    // Graphics
    public Display display = Display.getDisplay(this);
    public StringItem stdout = new StringItem("", "");
    public TextField stdin = new TextField("Command", "", 256, TextField.ANY);
    // |
    // MIDlet Loader
    // | (Triggers)
    public void startApp() { if (sys.containsKey("1")) { } else { login(username.equals(""), passwd().equals("")); } }
    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { notifyDestroyed(); }
    // | (Boot)
    public void init() {
        try { 
            Hashtable proc = new Hashtable(), args = new Hashtable();
            args.put(new Double(0), "/bin/init");
            globals.put("PWD", "/home/"); globals.put("USER", "root");
            proc.put("name", "init"); proc.put("owner", "root");

            Lua lua = new Lua(this, 0, "1", proc, stdout, globals);
            sys.put("1", proc); lua.globals.put("arg", args);
            proc.put("lua", lua); proc.put("handler", lua.getKernel());

            lua.tokens = lua.tokenize(read("/bin/init"));

            while (lua.peek().type != 0) { Object res = lua.statement(globals); if (lua.doreturn) { break; } }
        }
        catch (IllegalStateException e) { }
        catch (Throwable e) { panic(e); }
    }
    // | (Kernel Panic)
    private void panic(Throwable e) {
        Form screen = new Form(e instanceof Exception ? "SandBox" : "Kernel Panic");
        screen.append("An error occurred while OpenTTY tried to start!\n\nError: " + getCatch(e));
        if (e instanceof Exception) {
            screen.append("If you tried to install a program in /bin/init it can be the error");
        } else {
            screen.append("Try to clear your data or update OpenTTY");
        }
        screen.addCommand(new Command("Exit", Command.OK, 1));
        screen.addCommand(new Command("Update", Command.SCREEN, 1));
        screen.addCommand(new Command("Clear data", Command.SCREEN, 1));
        screen.setCommandListener(this);
        display.setCurrent(screen);
    }
    // | (Installation)
    private void login(boolean user, boolean pword) {
        if (user || pword) {
            Form screen = new Form("OpenTTY - Login");
            screen.append(env(":: Create " + (user && pword ? "your credentials (user and password)" : user ? "an username" : "a password") + " to your account"));
            if (user) { screen.append(new TextField("Username", "", 256, TextField.ANY)); }
            if (pword) { screen.append(new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); }
            screen.addCommand(new Command("Login", Command.OK, 1));
            screen.addCommand(new Command("Exit", Command.SCREEN, 1));
            screen.setCommandListener(this);
            display.setCurrent(screen);
        } 
        else { init(); }
    }
    private void logged() {
        Alert alert = new Alert("OpenTTY", "Reopen MIDlet to access console", null, AlertType.INFO);
        alert.setTimeout(Alert.FOREVER);
        alert.addCommand(new Command("Exit", Command.EXIT, 1));
        alert.setCommandListener(this);
        display.setCurrent(alert);
    }
    // | (Graphical Handler)
    public void commandAction(Command c, Displayable d) {
        if (c.getLabel() == "Exit") { destroyApp(true); }
        if (c.getLabel() == "Update") { try { platformRequest("http://opentty.xyz/dist/"); } catch (Exception e) { } }
        if (c.getLabel() == "Clear data") { deleteFile("/bin/init", 0); destroyApp(true); }
        else {
            int size = ((Form) d).size();
            if (size == 2) {
                TextField userquest = (TextField) ((Form) d).get(1);
                String value = userquest.getString().trim();
                if (value.equals("")) { warn("Login", "Missing Credentials!"); }
                else if (userquest.getLabel().equals("Username")) {
                    if (value.equals("root")) { warn("Login", "Invalid user name!"); }
                    else { writeRMS("OpenRMS", value.getBytes(), 1); logged(); }
                }
                else { writeRMS("OpenRMS", String.valueOf(value.hashCode()).getBytes(), 2); logged(); }
            } else {
                TextField userquest = (TextField) ((Form) d).get(1), pwquest = (TextField) ((Form) d).get(2);
                
                String user = userquest.getString().trim(), password = pwquest.getString().trim();
                if (user.equals("") || password.equals("")) { warn("Login", "Missing Credentials!"); }
                else if (user.equals("root")) { warn("Login", "Invalid user name!"); } 
                else {
                    writeRMS("OpenRMS", user.getBytes(), 1);
                    writeRMS("OpenRMS", String.valueOf(password.hashCode()).getBytes(), 2);
                    logged();
                }
            }
        }
    }
    // |
    // Control Thread
    public OpenTTY getInstance() { return this; }
    public String getThreadName(Thread thr) { String name = thr.getName(); String[] generic = { "Thread-0", "Thread-1", "MIDletEventQueue", "main" }; for (int i = 0; i < generic.length; i++) { if (name.equals(generic[i])) { name = "MIDlet"; break; } } return name; }
    // |
    public static String passwd() { return loadRMS("OpenRMS", 2); }
    public static boolean passwd(String query) { return query != null && String.valueOf(query.hashCode()).equals(loadRMS("OpenRMS", 2)); }
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
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // | (Window-Based Interfaces)
    public int warn(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    // | (Font Generator)
    public Font genFont(String params) { if (params == null || params.length() == 0 || params.equals("default")) { return Font.getDefaultFont(); } int face = Font.FACE_SYSTEM, style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; String[] tokens = split(params, ' '); for (int i = 0; i < tokens.length; i++) { String token = tokens[i].toLowerCase(); if (token.equals("system")) { face = Font.FACE_SYSTEM; } else if (token.equals("monospace")) { face = Font.FACE_MONOSPACE; } else if (token.equals("proportional")) { face = Font.FACE_PROPORTIONAL; } else if (token.equals("bold")) { style |= Font.STYLE_BOLD; } else if (token.equals("italic")) { style |= Font.STYLE_ITALIC; } else if (token.equals("ul") || token.equals("underline") || token.equals("underlined")) { style |= Font.STYLE_UNDERLINED; } else if (token.equals("small")) { size = Font.SIZE_SMALL; } else if (token.equals("medium")) { size = Font.SIZE_MEDIUM; } else if (token.equals("large")) { size = Font.SIZE_LARGE; } } Font f = Font.getFont(face, style, size); return f == null ? Font.getDefaultFont() : f; }
    // |
    public void print(String message, Object stdout) { print(message, stdout, 1000); } 
    public void print(String message, Object stdout, int id) { 
        if (stdout == null) { }
        else if (stdout instanceof StringItem) { 
            String current = ((StringItem) stdout).getText(), output = current == null || current.length() == 0 ? message : current + "\n" + message; 
            ((StringItem) stdout).setText(output); }
        else if (stdout instanceof StringBuffer) { ((StringBuffer) stdout).append("\n").append(message); }
        else if (stdout instanceof String) { write((String) stdout, read((String) stdout) + "\n" + message, 1000); }
        else if (stdout instanceof OutputStream) {
            try { ((OutputStream) stdout).write((message + "\n").getBytes()); ((OutputStream) stdout).flush(); } 
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
        else if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? new ByteArrayInputStream((byte[]) tmp.get(filename)) : null; } 
        else {
            if (filename.startsWith("/dev/")) {
                filename = filename.substring(5);
                String content = filename.equals("random") ? String.valueOf(random.nextInt(256)) : filename.equals("stdin") ? stdin.getString() : filename.equals("stdout") ? stdout.getText() : filename.equals("null") ? "\r" : filename.equals("zero") ? "\0" : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/dev/" + filename;
            }
            else if (filename.startsWith("/bin/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/bin/" + filename)) { return new ByteArrayInputStream((byte[]) cache.get("/bin/" + filename)); }

                byte[] content = read(filename, loadRMS("OpenRMS", 3));
                if (content != null) { if (useCache) { cache.put("/bin/" + filename, content); } return new ByteArrayInputStream(content); }

                filename = "/bin/" + filename;
            }
            else if (filename.startsWith("/etc/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/etc/" + filename)) { return new ByteArrayInputStream((byte[]) cache.get("/etc/" + filename)); }

                byte[] content = read(filename, loadRMS("OpenRMS", 5));
                if (content != null) { if (useCache) { cache.put("/etc/" + filename, content); } return new ByteArrayInputStream(content); }

                filename = "/etc/" + filename;
            }
            else if (filename.startsWith("/lib/")) {
                filename = filename.substring(5);
                if (useCache && cache.containsKey("/lib/" + filename)) { return new ByteArrayInputStream((byte[]) cache.get("/lib/" + filename)); }

                byte[] content = read(filename, loadRMS("OpenRMS", 4));
                if (content != null) { if (useCache) { cache.put("/lib/" + filename, content); } return new ByteArrayInputStream(content); }

                filename = "/lib/" + filename;
            }

            InputStream is = getClass().getResourceAsStream(filename);
            return is;
        }
    }
    public Image readImg(String filename) { try { InputStream is = getInputStream(filename); Image img = Image.createImage(is); is.close(); return img; } catch (Exception e) { return Image.createImage(16, 16); } }
    public String read(String filename) {
        try {
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
            else { if (useCache) { cache.put("/" + base + "/" + filename, new String(data)); } return addFile(filename, new String(data), loadRMS("OpenRMS", base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4), base); }
        }
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4); filename = filename.substring(5);

            if (filename.equals("")) { return 2; } 
            else if (id != 0) { return 13; }
            else { 
                if (useCache) { cache.put("/" + base + "/" + filename, data); } 

                int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
                String archive = loadRMS("OpenRMS", index);
                return addFile(filename, data, archive, base);
            }
        }
        else if (filename.startsWith("/dev/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else if (filename.equals("null")) { } else if (filename.equals("stdin")) { stdin.setString(new String(data)); } else if (filename.equals("stdout")) { stdout.setText(new String(data)); } else { return 5; } }
        else if (filename.startsWith("/tmp/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else { tmp.put(filename, data); } }
        else if (filename.startsWith("/")) { return 5; }
        
        return 0; 
    }
    public int writeRMS(String filename, byte[] data, int index) { try { RecordStore CONN = RecordStore.openRecordStore(filename, true); while (CONN.getNumRecords() < index) { CONN.addRecord("".getBytes(), 0, 0); } CONN.setRecord(index, data, 0, data.length); if (CONN != null) { CONN.closeRecordStore(); } } catch (Exception e) { return 1; } return 0; }
    public int deleteFile(String filename, int id) { 
        if (filename == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/home/")) { 
            try { 
                filename = filename.substring(6); 
                if (filename.equals("")) { return 2; }
                if (filename.equals("OpenRMS")) { return 13; } 
                
                RecordStore.deleteRecordStore(filename); 
            } 
            catch (RecordStoreNotFoundException e) { return 127; } 
            catch (Exception e) { return 1; } 
        }
        else if (filename.startsWith("/mnt/")) { 
            try { 
                FileConnection CONN = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); 
                if (CONN.exists()) { CONN.delete(); } 
                else { return 127; } 
                
                CONN.close(); 
            } 
            catch (Exception e) { return e instanceof SecurityException ? 13 : 1; } 
        }
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4), name = filename.substring(5);
            if (name.equals("")) { return 2; }
            if (id != 0) { return 13; }

            int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
            String content = loadRMS("OpenRMS", index);
            if (content.indexOf("[\1BEGIN:" + name + "\1]") == -1) { return 5; }

            if (useCache) { cache.remove("/" + base + "/" + name); }
            return writeRMS("OpenRMS", delFile(name, content).getBytes(), index);
        }
        else if (filename.startsWith("/tmp/")) {
            filename = filename.substring(5);
            if (filename.equals("")) { }
            else if (tmp.containsKey(filename)) { tmp.remove(filename); }
            else { return 127; }
        }
        else if (filename.startsWith("/")) { return 5; }
        
        return 0; 
    }
    // | (Normalize Path)
    public String joinpath(String file, Hashtable scope) {
        String pwd = scope.containsKey("PWD") ? (String) scope.get("PWD") : "/";
    
        if (file.startsWith("/")) { return file; }
        
        String fullPath = pwd + file;

        Vector components = new Vector();
        String[] parts = split(fullPath, '/');
        
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            
            if (part.equals(".")) {
                continue;
            } else if (part.equals("..")) {
                if (components.size() > 0) {
                    if (!components.lastElement().equals("")) {
                        components.removeElementAt(components.size() - 1);
                    }
                }
            } else {
                components.addElement(part);
            }
        }

        if (components.size() == 0) { return "/"; }
        StringBuffer result = new StringBuffer();

        for (int i = 0; i < components.size(); i++) {
            String comp = (String) components.elementAt(i);
            if (i == 0 && comp.equals("")) { result.append("/"); } 
            else if (i > 0 || !comp.equals("")) {
                result.append(comp);
                if (i < components.size() - 1) { result.append("/"); }
            }
        }

        if (fullPath.endsWith("/") && !result.toString().endsWith("/")) {
            result.append("/");
        }
        
        return result.toString();
    }
    // | (Archive Structures)
    public int addFile(String filename, String content, String archive, String base) { return addFile(filename, content.getBytes(), archive, base); }
    public int addFile(String filename, byte[] data, String archive, String base) { return writeRMS("OpenRMS", (delFile(filename, archive) + ("[\1BEGIN:" + filename + "\1]\n" + (isPureText(data) ? new String(data) : "[B64]" + encodeBase64(data)) + "\n[\1END\1]\n")).getBytes(), base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4); }

    public String delFile(String filename, String content) {
        String startTag = "[\1BEGIN:" + filename + "\1]";
        int start = content.indexOf(startTag);
        if (start == -1) { return content; }
        
        int end = content.indexOf("[\1END\1]", start);
        if (end == -1) { return content; }
        
        end += "[\1END\1]".length();
        
        if (end < content.length() && content.charAt(end) == '\n') { end++; }
        
        return content.substring(0, start) + content.substring(end);
    }
    public byte[] read(String filename, String archive) {
        String startTag = "[\1BEGIN:" + filename + "\1]";
        int start = archive.indexOf(startTag);
        if (start == -1) { return null; }
        
        int headerEnd = archive.indexOf('\n', start);
        if (headerEnd == -1) { return null; }
        headerEnd++; 
        
        int endTag = archive.indexOf("[\1END\1]", headerEnd);
        if (endTag == -1) { return null; }
        
        String content = archive.substring(headerEnd, endTag).trim();
        
        if (content.startsWith("[B64]")) { return decodeBase64(content.substring(5)); } 
        else { return content.getBytes(); }
    }
    // | (Base64)
    public String encodeBase64(byte[] data) {
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        StringBuffer result = new StringBuffer();
        int padding = 0;
        
        for (int i = 0; i < data.length; i += 3) {
            int b1 = data[i] & 0xFF;
            int b2 = (i + 1 < data.length) ? data[i + 1] & 0xFF : 0;
            int b3 = (i + 2 < data.length) ? data[i + 2] & 0xFF : 0;
            
            if (i + 1 >= data.length) padding = 2;
            else if (i + 2 >= data.length) padding = 1;
            else padding = 0;
            
            int triple = (b1 << 16) | (b2 << 8) | b3;
            
            result.append(base64Chars.charAt((triple >> 18) & 0x3F));
            result.append(base64Chars.charAt((triple >> 12) & 0x3F));
            
            if (padding < 2) { result.append(base64Chars.charAt((triple >> 6) & 0x3F)); } else { result.append('='); }
            if (padding < 1) { result.append(base64Chars.charAt(triple & 0x3F)); } else { result.append('='); }
            
            // Adicionar quebra de linha a cada 76 caracteres para legibilidade
            if (((i / 3) * 4) % 76 == 0 && i > 0) { result.append('\n'); }
        }
        
        return result.toString();
    }
    public byte[] decodeBase64(String data) {
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        
        data = replace(data, "\n", "");
        data = replace(data, "\r", "");
        
        if (data.length() % 4 != 0) {
            return null;
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        for (int i = 0; i < data.length(); i += 4) {
            int[] sextets = new int[4];
            int padding = 0;
            
            for (int j = 0; j < 4; j++) {
                char c = data.charAt(i + j);
                if (c == '=') { padding++; sextets[j] = 0; }
                else { sextets[j] = base64Chars.indexOf(c); if (sextets[j] < 0) { return null; } }
            }
            
            int triple = (sextets[0] << 18) | (sextets[1] << 12) | (sextets[2] << 6) | sextets[3];
            
            baos.write((triple >> 16) & 0xFF);
            if (padding < 2) { baos.write((triple >> 8) & 0xFF); }
            if (padding < 1) { baos.write(triple & 0xFF); }
        }
        
        return baos.toByteArray();
    }
    private boolean isPureText(byte[] data) {
        int textCount = 0;
        int sampleSize = Math.min(data.length, 100);
        
        for (int i = 0; i < sampleSize; i++) {
            byte b = data[i];
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                textCount++;
            }
        }
        
        return (textCount * 100 / sampleSize) > 95; // 95% de caracteres textuais
    }    
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // Java Virtual Machine
    public int javaClass(String name) { try { Class.forName(name); return 0; } catch (ClassNotFoundException e) { return 3; } } 
    public String getName() { String s; StringBuffer BUFFER = new StringBuffer(); if ((s = System.getProperty("java.vm.name")) != null) { BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor")); if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); } if ((s = System.getProperty("java.vm.specification.name")) != null) { BUFFER.append('\n').append(s); } } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) { BUFFER.append("J9 VM, IBM (").append(s).append(')'); if ((s = System.getProperty("java.fullversion")) != null) { BUFFER.append("\n\n").append(s); } } else if ((s = System.getProperty("com.oracle.jwc.version")) != null) { BUFFER.append("OJWC v").append(s).append(", Oracle"); } else if (javaClass("com.sun.cldchi.jvm.JVM") == 0) { BUFFER.append("CLDC Hotspot Implementation, Sun"); } else if (javaClass("com.sun.midp.Main") == 0) { BUFFER.append("KVM, Sun (MIDP)"); } else if (javaClass("com.sun.cldc.io.ConsoleOutputStream") == 0) { BUFFER.append("KVM, Sun (CLDC)"); } else if (javaClass("com.jblend.util.SortedVector") == 0) { BUFFER.append("JBlend, Aplix"); } else if (javaClass("com.jbed.io.CharConvUTF8") == 0) { BUFFER.append("Jbed, Esmertec/Myriad Group"); } else if (javaClass("MahoTrans.IJavaObject") == 0) { BUFFER.append("MahoTrans"); } else { BUFFER.append("Unknown"); } return BUFFER.append('\n').toString(); }
}            
// |
// Goodbye 2025
// EOF
