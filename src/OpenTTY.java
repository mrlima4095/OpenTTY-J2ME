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
    public boolean useCache = true, debug = false;
    // |
    // System Objects
    public int lastID = 1000;
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Object shell;
    // |
    public Hashtable attributes = new Hashtable(), fs = new Hashtable(), sys = new Hashtable(), tmp = new Hashtable(), cache = new Hashtable(), cacheLua = new Hashtable(), graphics = new Hashtable(), servers = new Hashtable(), globals = new Hashtable(), userID = new Hashtable();
    public String username = read("/home/OpenRMS", globals), build = "2026-1.18.1-03x24";
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
            Hashtable args = new Hashtable(); args.put(new Double(0), "/bin/init");
            globals.put("PWD", "/home/"); globals.put("USER", "root"); globals.put("ROOT", "/");
            Process proc = new Process(this, "init", "/bin/init", "root", 0, "1", stdout, globals);

            sys.put("1", proc); proc.lua.globals.put("arg", args); proc.handler = proc.lua.getKernel();
            proc.lua.tokens = proc.lua.tokenize(read("/bin/init", globals)); 

            while (proc.lua.peek().type != 0) { Object res = proc.lua.statement(globals); if (proc.lua.doreturn) { break; } }
        }
        catch (IllegalStateException e) { }
        catch (Throwable e) { panic(e); }
    }
    // | (Kernel Panic)
    private void panic(Throwable e) {
        Form screen = new Form(e instanceof Exception ? "SandBox" : "Kernel Panic");
        screen.append("An error occurred while OpenTTY tried to start!\n\nError: " + getCatch(e));
        screen.append(e instanceof Exception ? "If you tried to install a program in /bin/init it can be the error" : "Try to clear your data or update OpenTTY");

        screen.addCommand(new Command("Exit", Command.OK, 1));
        screen.addCommand(new Command("Recovery", Command.SCREEN, 1));
        screen.setCommandListener(this);
        display.setCurrent(screen);
    }
    private void recovery() {
        List menu = new List("Recovery", List.IMPLICIT);
        menu.append("Retry boot", null); menu.append("Update", null); menu.append("---", null);
        menu.append("Clear data", null); menu.append("Reset config", null); menu.append("Factory reset", null); menu.append("---", null);
        menu.append("System Info", null); menu.append("Questions", null);
        menu.addCommand(new Command("Exit", Command.BACK, 1));
        menu.addCommand(new Command("Open", Command.OK, 1));
        menu.setCommandListener(this);
        display.setCurrent(menu);
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
        else if (c.getLabel() == "Recovery" || c.getLabel() == "Back") { recovery(); }
        else if (d instanceof List && c.getLabel() == "Open" || c == List.SELECT_COMMAND) {
            List menu = (List) d;
            int selected = menu.getSelectedIndex();
            String item = menu.getString(selected);
            
            if (item.equals("Retry boot")) { login(username.equals(""), passwd().equals("")); }
            else if (item.equals("Update")) { try { platformRequest("http://opentty.xyz/dist/"); } catch (Exception e) { } }
            else if (item.equals("Clear data")) { deleteFile("/bin/init", 0, globals); writeRMS("OpenRMS", "".getBytes(), 1); writeRMS("OpenRMS", "".getBytes(), 2); warn("Clear Data", "User data cleared. Restart OpenTTY."); }
            else if (item.equals("Reset config")) {
                String[] files = { "fstab", "hostname", "motd", "os-release", "services" };
                for (int i = 0; i < files.length; i++) { deleteFile("/etc/" + files[i], 0, globals); }
                warn("Reset Config", "Configuration reset to defaults.");
            }
            else if (item.equals("Factory reset")) {
                deleteFile("/bin/init", 0, globals);
                try { RecordStore.deleteRecordStore("OpenRMS"); } catch (Exception e) { }

                warn("Factory Reset", "All data cleared. Restart OpenTTY.");
            }
            else if (item.equals("System Info")) {
                StringBuffer info = new StringBuffer();
                info.append("OpenTTY " + build + "\n").append("Uptime: " + ((System.currentTimeMillis() - uptime) / 1000) + "s\n").append("User: " + username + "\n").append("Memory: " + (runtime.freeMemory() / 1024) + " KB free\n").append("Processes: " + sys.size() + "\n").append("\nJVM Info:\n" + getName());

                Form infoScreen = new Form("System Information");
                infoScreen.append(info.toString());
                infoScreen.addCommand(new Command("Back", Command.BACK, 1));
                infoScreen.setCommandListener(this);
                display.setCurrent(infoScreen);
            }
            else if (item.equals("Questions")) {
                Form faq = new Form("Frequently Asked Questions");
                faq.append(new StringItem("Why am I seeing a Kernel Panic?", "A bug damage /bin/init - program that initialize OpenTTY or Lua Runtime"));
                faq.append(new StringItem("I installed MIDlet and was already having problems.", "Report this Kernel Panic on Github and wait a bug fix. The package that you download may be corromped."));

                faq.addCommand(new Command("Back", Command.BACK, 1));
                faq.setCommandListener(this);
                display.setCurrent(faq);
            }
        }
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
    public String getcontent(String file, Hashtable scope) { return file.startsWith("/") ? read(file, scope) : read(((String) scope.get("PWD")) + file, scope); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? replace(text, "\"", "") : text.trim(); }
    // | (Arrays)
    public String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    public String[] splitArgs(String content) { Vector args = new Vector(); boolean inQuotes = false; int start = 0; for (int i = 0; i < content.length(); i++) { char c = content.charAt(i); if (c == '"') { inQuotes = !inQuotes; continue; } if (!inQuotes && c == ' ') { if (i > start) { args.addElement(getpattern(content.substring(start, i))); } start = i + 1; } } if (start < content.length()) { args.addElement(getpattern(content.substring(start))); } String[] result = new String[args.size()]; args.copyInto(result); return result; }
    // |
    // | (Generators)
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    // | (User Manager)
    public int getUserID(String user) { return user.equals("root") ? 0 : user.equals(username) ? 1000 : userID.containsKey(user) ? ((Integer) userID.get(user)).intValue() : -1; }
    public String getUser(int uid) {
        if (uid == 0) { return "root"; } else if (uid == 1000) { return username; }
        for (Enumeration keys = sys.keys(); keys.hasMoreElements();) {
            String user = (String) keys.nextElement();
            Integer id = (Integer) userID.get(user);
            if (id.intValue() == uid) { return user; }
        }
        return null;
    }
    // | (Trackers)
    public String getpid(String name) { for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); if (name.equals((String) ((Hashtable) sys.get(PID)).get("name"))) { return PID; } } return null; } 
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // | (Window-Based Interfaces)
    public int warn(String title, String message) { if (message == null || message.length() == 0) { return 2; } Alert alert = new Alert(title, message, null, AlertType.WARNING); alert.setTimeout(Alert.FOREVER); display.setCurrent(alert); return 0; }
    // |
    public void print(String message, Object stdout) { print(message, stdout, 1000, globals); } 
    public void print(String message, Object stdout, int id, Hashtable scope) { 
        if (stdout == null) { }
        else if (stdout instanceof StringItem) { String current = ((StringItem) stdout).getText(), output = current == null || current.length() == 0 ? message : current + "\n" + message; ((StringItem) stdout).setText(output); }
        else if (stdout instanceof StringBuffer) { ((StringBuffer) stdout).append("\n").append(message); }
        else if (stdout instanceof String) { write((String) stdout, read((String) stdout, scope) + "\n" + message, 1000, scope); }
        else if (stdout instanceof OutputStream) { try { ((OutputStream) stdout).write((message + "\n").getBytes()); ((OutputStream) stdout).flush(); } catch (Exception e) { } }
    }
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // API 003 - File System
    // | (Read) 
    public InputStream getInputStream(String filename, Hashtable scope) throws Exception {
        if ((filename = solvepath(filename, scope)).startsWith("/home/")) {
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
            else if (filename.startsWith("/proc/")) {
                filename = filename.substring(6);
                String content = filename.equals("uptime") ? "" + ((System.currentTimeMillis() - uptime) / 1000) : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/proc/" + filename;
            }

            InputStream is = getClass().getResourceAsStream(filename);
            return is;
        }
    }
    public Image readImg(String filename, Hashtable scope) { try { InputStream is = getInputStream(filename, scope); Image img = Image.createImage(is); is.close(); return img; } catch (Exception e) { return Image.createImage(16, 16); } }
    public String read(String filename, Hashtable scope) {
        try {
            InputStream is = getInputStream(filename, scope);
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
    public int write(String filename, String data, int id, Hashtable scope) { return write(filename, data.getBytes(), id, scope); }
    public int write(String filename, byte[] data, int id, Hashtable scope) {
        if ((filename = solvepath(filename, scope)) == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/mnt/")) { FileConnection fs = null; OutputStream out = null; try { fs = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!fs.exists()) { fs.create(); } out = fs.openOutputStream(); out.write(data); out.flush(); } catch (Exception e) { return (e instanceof SecurityException) ? 13 : 1; } finally { out.close(); fs.close(); } } 
        else if (filename.startsWith("/home/")) { return writeRMS(filename.substring(6), data, 1); } 
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4); filename = filename.substring(5);

            if (filename.equals("")) { return 2; } 
            else if (id != 0) { return 13; }
            else { if (useCache) { cache.put("/" + base + "/" + filename, data); } return addFile(filename, new String(data), loadRMS("OpenRMS", base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4), base); }
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
    public int deleteFile(String filename, int id, Hashtable scope) { 
        if ((filename = solvepath(filename, scope)) == null || filename.length() == 0) { return 2; } 
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
    public String solvepath(String path, Hashtable scope) { 
        String root = scope.containsKey("ROOT") ? (String) scope.get("ROOT") : "";

        if (path == null) { return "/"; }
        else if (root.equals("/") || path.startsWith("/dev/") || path.startsWith("/mnt/") || path.startsWith("/proc/") || path.startsWith("/tmp/")) { return path; }
        else if (path.startsWith("/")) { return root.endsWith("/") ? (root.length() > 1 ? root + path.substring(1) : root) : root + path; } return path;
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
        
        for (int i = 0; i < data.length; i += 3) {
            int b1 = data[i] & 0xFF;
            int b2 = (i + 1 < data.length) ? data[i + 1] & 0xFF : 0;
            int b3 = (i + 2 < data.length) ? data[i + 2] & 0xFF : 0;
            
            int triple = (b1 << 16) | (b2 << 8) | b3;
            
            result.append(base64Chars.charAt((triple >> 18) & 0x3F));
            result.append(base64Chars.charAt((triple >> 12) & 0x3F));
            
            if (i + 1 < data.length) { result.append(base64Chars.charAt((triple >> 6) & 0x3F)); } else { result.append('='); }
            if (i + 2 < data.length) { result.append(base64Chars.charAt(triple & 0x3F)); } else { result.append('='); }
        }
        
        return result.toString();
    }
    public byte[] decodeBase64(String data) {
        String base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
        
        StringBuffer clean = new StringBuffer();
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if (c != '\n' && c != '\r' && c != ' ' && c != '\t') {
                clean.append(c);
            }
        }
        data = clean.toString();
        
        if (data.length() % 4 != 0) { return null; }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
        for (int i = 0; i < data.length(); i += 4) {
            int[] sextets = new int[4];
            int padding = 0;
            
            for (int j = 0; j < 4; j++) {
                char c = data.charAt(i + j);
                if (c == '=') { 
                    padding++; 
                    sextets[j] = 0; 
                } else { 
                    sextets[j] = base64Chars.indexOf(c); 
                    if (sextets[j] < 0) { 
                        return null; 
                    } 
                }
            }
            
            int triple = (sextets[0] << 18) | (sextets[1] << 12) | (sextets[2] << 6) | sextets[3];
            
            baos.write((triple >> 16) & 0xFF);
            if (padding < 2) { baos.write((triple >> 8) & 0xFF); }
            if (padding < 1) { baos.write(triple & 0xFF); }
        }
        
        return baos.toByteArray();
    }
    // |
    public boolean isPureText(byte[] data) {
        int textCount = 0;
        int sampleSize = Math.min(data.length, 100);
        
        for (int i = 0; i < sampleSize; i++) {
            byte b = data[i];
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) {
                textCount++;
            }
        }
        
        return (textCount * 100) > (sampleSize * 95);
    }    
    // |
    // | -=-=-=-=-=-=-=-=-=-=-
    // Java Virtual Machine
    public int javaClass(String name) { try { Class.forName(name); return 0; } catch (ClassNotFoundException e) { return 3; } } 
    public String getName() { String s; StringBuffer BUFFER = new StringBuffer(); if ((s = System.getProperty("java.vm.name")) != null) { BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor")); if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); } if ((s = System.getProperty("java.vm.specification.name")) != null) { BUFFER.append('\n').append(s); } } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) { BUFFER.append("J9 VM, IBM (").append(s).append(')'); if ((s = System.getProperty("java.fullversion")) != null) { BUFFER.append("\n\n").append(s); } } else if ((s = System.getProperty("com.oracle.jwc.version")) != null) { BUFFER.append("OJWC v").append(s).append(", Oracle"); } else if (javaClass("com.sun.cldchi.jvm.JVM") == 0) { BUFFER.append("CLDC Hotspot Implementation, Sun"); } else if (javaClass("com.sun.midp.Main") == 0) { BUFFER.append("KVM, Sun (MIDP)"); } else if (javaClass("com.sun.cldc.io.ConsoleOutputStream") == 0) { BUFFER.append("KVM, Sun (CLDC)"); } else if (javaClass("com.jblend.util.SortedVector") == 0) { BUFFER.append("JBlend, Aplix"); } else if (javaClass("com.jbed.io.CharConvUTF8") == 0) { BUFFER.append("Jbed, Esmertec/Myriad Group"); } else if (javaClass("MahoTrans.IJavaObject") == 0) { BUFFER.append("MahoTrans"); } else { BUFFER.append("Unknown"); } return BUFFER.append('\n').toString(); }
}
// | 
// Process
class Process {
    private OpenTTY midlet = null;
    public String name, owner, pid, cmd;
    public Hashtable scope, db = new Hashtable(), net = new Hashtable();
    public final long startTime;
    public int uid = 1000, priority = DEFAULT_PRIORITY;

    public static final int MIN_PRIORITY = 0, DEFAULT_PRIORITY = 10, MAX_PRIORITY = 20;

    public Object stdout, stderr;
    public Object handler = null, sighandler = null;
    public Lua lua = null;
    public ELF elf = null;
    
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable scope) { this.lua = new Lua(midlet, uid, pid, this, stdout, scope); this.name = name; this.owner = owner; this.uid = uid; this.pid = pid; this.stdout = stdout; this.stderr = stdout; this.scope = scope; this.startTime = System.currentTimeMillis(); }
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable args, Hashtable scope) { this.elf = new ELF(midlet, args, stdout, scope, uid, pid, this); this.name = name; this.owner = owner; this.uid = uid; this.pid = pid; this.stdout = stdout; this.stderr = stdout; this.scope = scope; this.startTime = System.currentTimeMillis(); }

    public String toString() { return "{ name=" + name + ", owner=" + owner + ", uid=" + uid + ", pid=" + pid + ", " + (lua != null ? "lua=" + lua + ", " : elf != null ? "elf=" + elf + ", " : "") + (handler != null ? "handler=" + handler + ", " : "") + "priority=" + priority + ", scope=" + scope + ", db=" + db + " }"; }
}
// |
// | 10k commits
// Goodbye 2025
// EOF