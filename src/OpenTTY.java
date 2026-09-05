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
    public int lastID = 1000, lastGID = 1000;
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Object shell;
    // |
    public Hashtable attributes = new Hashtable(), fs = new Hashtable(), sys = new Hashtable(), tmp = new Hashtable(), cache = new Hashtable(), cacheLua = new Hashtable(), graphics = new Hashtable(), servers = new Hashtable(), globals = new Hashtable(), userID = new Hashtable(), userGID = new Hashtable(), userPass = new Hashtable(), groupID = new Hashtable(), groupMembers = new Hashtable(), groupPass = new Hashtable();
    public boolean accountsLoaded = false;
    public String username = read("/home/OpenRMS", globals), build = "2026-1.18.2-03x30";
    // |
    // Graphics
    public Display display = Display.getDisplay(this);
    public Displayable previous = null;
    public List taskMngr = null;
    private Vector taskMngrPids = null;
    // |
    // MIDlet Loader
    // | (Triggers)
    public void startApp() {
        if (sys.containsKey("1")) { }
        else {
            boolean user = username.equals(""), pword = passwd().equals("");
            if (user || pword) {
                Form screen = new Form("OpenTTY - Login");
                screen.append(env(":: Create " + (user && pword ? "your credentials (user and password)" : user ? "an username" : "a password") + " to your account"));
                if (user) { screen.append(new TextField("Username", "", 256, TextField.ANY)); }
                if (pword) { screen.append(new TextField("Password", "", 256, TextField.ANY | TextField.PASSWORD)); }
                screen.addCommand(new Command("Login", Command.OK, 1));
                screen.addCommand(new Command("Exit", Command.SCREEN, 1));
                screen.setCommandListener(this);
                display.setCurrent(screen);
            } else {
                try {
                    Hashtable args = new Hashtable(); args.put(new Double(0), "/bin/init");
                    globals.put("PWD", "/home/"); globals.put("USER", "root"); globals.put("ROOT", "/"); globals.put("ALIAS", new Hashtable()); userID.put(username, 1000);

                    Process proc = new Process(this, "init", "/bin/init", "root", 0, "1", new StringBuffer(), globals);

                    sys.put("1", proc); proc.lua.globals.put("arg", args); proc.handler = proc.lua.getKernel();
                    proc.lua.currentSource = "/bin/init";
                    proc.lua.tokens = proc.lua.tokenize(read("/bin/init", globals)); 

                    while (proc.lua.peek().type != 0) { Object res = proc.lua.statement(globals); if (proc.lua.doreturn) { break; } }
                }
                catch (IllegalStateException e) { }
                catch (OutOfMemoryError e) {
                    Form screen = new Form("SandBox");
                    screen.append("Insufficient Memory");
                    screen.append("Used Memory: " + ((runtime.totalMemory() / 1024) - (runtime.freeMemory())) + " KB\nFree Memory: " + (runtime.freeMemory() / 1024) + " KB\nTotal Memory: " + (runtime.totalMemory() / 1024) + "KB total");

                    screen.addCommand(new Command("Exit", Command.OK, 1));
                    screen.setCommandListener(this);
                    display.setCurrent(screen);
                }
                catch (Throwable e) {
                    Form screen = new Form(e instanceof Exception ? "SandBox" : "Kernel Panic");
                    screen.append("An error occurred while OpenTTY tried to start!\n\nError: " + getCatch(e));
                    screen.append(e instanceof Exception ? "If you tried to install a program in /bin/init it can be the error" : "Try to clear your data or update OpenTTY");

                    screen.addCommand(new Command("Exit", Command.OK, 1));
                    screen.setCommandListener(this);
                    display.setCurrent(screen);
                }
            }
        }
    }
    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { notifyDestroyed(); }
    // |
    private void logged() { Alert alert = new Alert("OpenTTY", "Reopen MIDlet to access console", null, AlertType.INFO); alert.setTimeout(Alert.FOREVER); alert.addCommand(new Command("Exit", Command.EXIT, 1)); alert.setCommandListener(this); display.setCurrent(alert); }
    // | (Graphical Handler)
    public static Hashtable cloneScope(Hashtable src) {
        Hashtable dst = new Hashtable();
        for (Enumeration e = src.keys(); e.hasMoreElements();) { Object k = e.nextElement(); dst.put(k, src.get(k)); }
        return dst;
    }
    public void showTaskManager() {
        if (taskMngr == null) {
            taskMngr = new List("Running", List.IMPLICIT);
            taskMngr.addCommand(new Command("Back", Command.BACK, 1));
            taskMngr.addCommand(new Command("Interrupt", Command.STOP, 2));
            taskMngr.setSelectCommand(List.SELECT_COMMAND);
            taskMngr.setCommandListener(this);
        } else {
            taskMngr.deleteAll();
        }

        Displayable current = display.getCurrent();
        if (current != taskMngr) { previous = display.getCurrent(); }
        
        taskMngrPids = new Vector();
        for (Enumeration keys = sys.keys(); keys.hasMoreElements();) {
            String pid = (String) keys.nextElement();
            Process p = (Process) sys.get(pid);
            if (p != null && p.screen != null) {
                taskMngr.append((p.screen.getTitle()) + " [" + pid + "]", null);
                taskMngrPids.addElement(pid);
            }
        }

        display.setCurrent(taskMngr);
    }
    public void commandAction(Command c, Displayable d) {
        if (c.getLabel() == "Exit") { destroyApp(true); }
        else if (d == taskMngr) {
            if (c.getLabel() == "Back") { if (taskMngrPids.size() == 0) { destroyApp(true); } else if (previous != null) { display.setCurrent(previous); } }
            else if (c.getLabel() == "Interrupt") {
                int sel = taskMngr.getSelectedIndex();
                if (sel >= 0 && sel < taskMngrPids.size()) {
                    String pid = (String) taskMngrPids.elementAt(sel);
                    Process p = (Process) sys.get(pid);
                    if (p != null) {
                        if (p.sighandler != null) { try { Vector sa = new Vector(); sa.addElement("15"); ((Lua.LuaFunction) p.sighandler).call(sa); } catch (Throwable e) { } }
                        sys.remove(pid);
                        showTaskManager();
                    }
                }
            }
            else if (c == List.SELECT_COMMAND) {
                int sel = taskMngr.getSelectedIndex();
                if (sel >= 0 && sel < taskMngrPids.size()) {
                    String pid = (String) taskMngrPids.elementAt(sel);
                    Process p = (Process) sys.get(pid);
                    if (p != null && p.screen != null) { display.setCurrent(p.screen); }
                }
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
    public String replace(String source, String target, String replacement) { if (target.length() == 0 || source.indexOf(target) < 0) { return source; } StringBuffer result = new StringBuffer(); int start = 0, end; while ((end = source.indexOf(target, start)) >= 0) { result.append(source.substring(start, end)); result.append(replacement); start = end + target.length(); } result.append(source.substring(start)); return result.toString(); }
    public String env(String text, Hashtable scope) { if (scope != null) { text = replace(text, "$PATH", (String) scope.get("PWD")); for (Enumeration keys = scope.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) scope.get(key)); } } return env(text); }
    public String env(String text) { text = replace(text, "$USER", username); for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) { String key = (String) keys.nextElement(); text = replace(text, "$" + key, (String) attributes.get(key)); } text = replace(text, "$.", "$"); return escape(text); }
    public String escape(String text) { if (text.indexOf('\\') < 0 && text.indexOf('.') < 0) { return text; } text = replace(text, "\\n", "\n"); text = replace(text, "\\r", "\r"); text = replace(text, "\\t", "\t"); text = replace(text, "\\b", "\b"); text = replace(text, "\\\\", "\\"); text = replace(text, "\\.", "\\"); return text; }
    public String getCatch(Throwable e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : e.getClass().getName() + ": " + message; }
    // |
    public String getcontent(String file, Hashtable scope) { return file.startsWith("/") ? read(file, scope) : read(((String) scope.get("PWD")) + file, scope); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? text.substring(1, text.length() - 1) : text.trim(); } // replace(text, "\"", "")
    // | (Arrays)
    public String[] split(String content, char div) { Vector lines = new Vector(); int start = 0; for (int i = 0; i < content.length(); i++) { if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; } } if (start < content.length()) { lines.addElement(content.substring(start)); } String[] result = new String[lines.size()]; lines.copyInto(result); return result; }
    public String[] splitArgs(String input) {
        Vector result = new Vector();
        StringBuffer current = new StringBuffer();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        boolean escaped = false;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }
            
            if (c == '\\') {
                if (inDoubleQuotes || inSingleQuotes) { escaped = true; }
                else { current.append(c); }

                continue;
            }
            
            if (c == '"' && !inSingleQuotes) { inDoubleQuotes = !inDoubleQuotes; current.append(c); continue; }
            if (c == '\'' && !inDoubleQuotes) { inSingleQuotes = !inSingleQuotes; current.append(c); continue; }
            if (c == ' ' && !inDoubleQuotes && !inSingleQuotes) { if (current.length() > 0) { result.addElement(current.toString()); current.setLength(0); } continue; }
            
            current.append(c);
        }
        
        if (current.length() > 0) { result.addElement(current.toString()); }
        
        String[] array = new String[result.size()];
        for (int i = 0; i < result.size(); i++) { array[i] = getpattern((String) result.elementAt(i)); }
        
        return array;
    }
    // |
    // | (Generators)
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    // | (User Manager)
    private String[] accountFields(String line) { Vector fields = new Vector(); int start = 0; for (int i = 0; i <= line.length(); i++) { if (i == line.length() || line.charAt(i) == ':') { fields.addElement(line.substring(start, i)); start = i + 1; } } String[] result = new String[fields.size()]; for (int i = 0; i < result.length; i++) { result[i] = (String) fields.elementAt(i); } return result; }
    private String accountValue(String[] fields, int index) { return index < fields.length ? fields[index] : ""; }
    private void loadAccounts() {
        if (accountsLoaded) { return; }
        String passwdFile = read("/etc/passwd", globals), shadowFile = read("/etc/shadow", globals), groupFile = read("/etc/group", globals), gshadowFile = read("/etc/gshadow", globals);
        if (passwdFile.length() == 0) {
            String main = username.length() == 0 ? "user" : username;
            userID.put(main, new Integer(1000)); userGID.put(main, new Integer(1000)); userPass.put(main, String.valueOf(passwd().hashCode()));
            groupID.put("root", new Integer(0)); groupMembers.put("root", new Vector()); groupPass.put("root", "!");
            Vector members = new Vector(); members.addElement(main); groupID.put(main, new Integer(1000)); groupMembers.put(main, members); groupPass.put(main, "!");
            saveAccounts();
        } else {
            String[] lines = split(passwdFile, '\n');
            for (int i = 0; i < lines.length; i++) { String[] f = accountFields(lines[i].trim()); if (f.length >= 7) { try { int uid = Integer.parseInt(f[2]), gid = Integer.parseInt(f[3]); userID.put(f[0], new Integer(uid)); userGID.put(f[0], new Integer(gid)); if (uid > lastID) { lastID = uid; } } catch (Exception e) { } } }
            lines = split(shadowFile, '\n'); for (int i = 0; i < lines.length; i++) { String[] f = accountFields(lines[i].trim()); if (f.length >= 2) { userPass.put(f[0], f[1]); } }
            lines = split(groupFile, '\n'); for (int i = 0; i < lines.length; i++) { String[] f = accountFields(lines[i].trim()); if (f.length >= 4) { try { int gid = Integer.parseInt(f[2]); groupID.put(f[0], new Integer(gid)); if (gid > lastGID) { lastGID = gid; } Vector members = new Vector(); if (f[3].length() > 0) { String[] names = split(f[3], ','); for (int j = 0; j < names.length; j++) { members.addElement(names[j]); } } groupMembers.put(f[0], members); } catch (Exception e) { } } }
            lines = split(gshadowFile, '\n'); for (int i = 0; i < lines.length; i++) { String[] f = accountFields(lines[i].trim()); if (f.length >= 2) { groupPass.put(f[0], f[1]); } }
        }
        if (!userID.containsKey("root")) { userID.put("root", new Integer(0)); userGID.put("root", new Integer(0)); userPass.put("root", String.valueOf(passwd().hashCode())); }
        accountsLoaded = true;
    }
    private String accountLines() { StringBuffer out = new StringBuffer(); for (Enumeration e = userID.keys(); e.hasMoreElements();) { String name = (String) e.nextElement(); int uid = ((Integer) userID.get(name)).intValue(), gid = userGID.containsKey(name) ? ((Integer) userGID.get(name)).intValue() : uid; out.append(name).append(":x:").append(uid).append(":").append(gid).append(":").append(name).append(":").append(name.equals("root") ? "/root/" : "/home/").append(":/bin/sh\n"); } return out.toString(); }
    private String shadowLines() { StringBuffer out = new StringBuffer(); for (Enumeration e = userID.keys(); e.hasMoreElements();) { String name = (String) e.nextElement(); out.append(name).append(":").append(userPass.containsKey(name) ? userPass.get(name) : "!").append(":0:0:99999:7:::\n"); } return out.toString(); }
    private String groupLines() { StringBuffer out = new StringBuffer(); for (Enumeration e = groupID.keys(); e.hasMoreElements();) { String name = (String) e.nextElement(); Vector members = (Vector) groupMembers.get(name); out.append(name).append(":x:").append(groupID.get(name)).append(":"); if (members != null) { for (int i = 0; i < members.size(); i++) { if (i > 0) { out.append(','); } out.append(members.elementAt(i)); } } out.append('\n'); } return out.toString(); }
    private String gshadowLines() { StringBuffer out = new StringBuffer(); for (Enumeration e = groupID.keys(); e.hasMoreElements();) { String name = (String) e.nextElement(); out.append(name).append(":").append(groupPass.containsKey(name) ? groupPass.get(name) : "!").append(":\n"); } return out.toString(); }
    public void saveAccounts() { if (!accountsLoaded) { accountsLoaded = true; } write("/etc/passwd", accountLines(), 0, globals); write("/etc/shadow", shadowLines(), 0, globals); write("/etc/group", groupLines(), 0, globals); write("/etc/gshadow", gshadowLines(), 0, globals); }
    public int getUserID(String user) { loadAccounts(); return user.equals("root") ? 0 : userID.containsKey(user) ? ((Integer) userID.get(user)).intValue() : -1; }
    public int getUserGID(String user) { loadAccounts(); return userGID.containsKey(user) ? ((Integer) userGID.get(user)).intValue() : -1; }
    public String getUser(int uid) {
        loadAccounts();
        if (uid == 0) { return "root"; }
        for (Enumeration keys = userID.keys(); keys.hasMoreElements();) {
            String user = (String) keys.nextElement();
            Integer id = (Integer) userID.get(user);
            if (id != null && id.intValue() == uid) { return user; }
        }
        return null;
    }
    public boolean authenticate(String user, String password) { loadAccounts(); return userPass.containsKey(user) && password != null && String.valueOf(password.hashCode()).equals(userPass.get(user)); }
    public String groupsFor(String user) { loadAccounts(); StringBuffer out = new StringBuffer(); for (Enumeration e = groupID.keys(); e.hasMoreElements();) { String group = (String) e.nextElement(); Vector members = (Vector) groupMembers.get(group); int gid = ((Integer) groupID.get(group)).intValue(); if ((userGID.containsKey(user) && ((Integer) userGID.get(user)).intValue() == gid) || (members != null && members.contains(user))) { if (out.length() > 0) { out.append(' '); } out.append(group); } } return out.toString(); }
    public int addUser(String name) { loadAccounts(); if (name == null || name.length() == 0 || name.equals("root")) { return 2; } if (userID.containsKey(name)) { return 128; } int uid = ++lastID; userID.put(name, new Integer(uid)); userGID.put(name, new Integer(uid)); userPass.put(name, "!"); groupID.put(name, new Integer(uid)); Vector members = new Vector(); members.addElement(name); groupMembers.put(name, members); groupPass.put(name, "!"); saveAccounts(); return 0; }
    public int removeUser(String name) { loadAccounts(); if (name == null || name.length() == 0 || name.equals("root") || name.equals(username)) { return 13; } if (!userID.containsKey(name)) { return 127; } userID.remove(name); userGID.remove(name); userPass.remove(name); groupID.remove(name); groupMembers.remove(name); groupPass.remove(name); for (Enumeration e = groupMembers.elements(); e.hasMoreElements();) { Vector members = (Vector) e.nextElement(); if (members != null) { members.removeElement(name); } } saveAccounts(); return 0; }
    public int setPassword(String name, String oldpw, String newpw, int uid) { loadAccounts(); if (name == null || !userID.containsKey(name) || newpw == null || newpw.length() == 0) { return 2; } if (uid != 0 && (uid != getUserID(name) || !authenticate(name, oldpw))) { return 13; } userPass.put(name, String.valueOf(newpw.hashCode())); saveAccounts(); return 0; }
    public int addGroup(String name) { loadAccounts(); if (name == null || name.length() == 0 || groupID.containsKey(name)) { return 128; } int gid = ++lastGID; while (groupID.containsValue(new Integer(gid))) { gid = ++lastGID; } groupID.put(name, new Integer(gid)); groupMembers.put(name, new Vector()); groupPass.put(name, "!"); saveAccounts(); return 0; }
    public int removeGroup(String name) { loadAccounts(); if (name == null || name.length() == 0 || name.equals("root") || !groupID.containsKey(name)) { return 127; } int gid = ((Integer) groupID.get(name)).intValue(); for (Enumeration e = userGID.elements(); e.hasMoreElements();) { if (((Integer) e.nextElement()).intValue() == gid) { return 13; } } groupID.remove(name); groupMembers.remove(name); groupPass.remove(name); saveAccounts(); return 0; }
    public int modifyUser(String name, String group, boolean add) { loadAccounts(); if (name == null || group == null || !userID.containsKey(name) || !groupID.containsKey(group)) { return 127; } Vector members = (Vector) groupMembers.get(group); if (members == null) { members = new Vector(); groupMembers.put(group, members); } if (add) { if (!members.contains(name)) { members.addElement(name); } } else { members.removeElement(name); } saveAccounts(); return 0; }
    public int setGroupPassword(String name, String password, int uid) { loadAccounts(); if (uid != 0 || name == null || !groupID.containsKey(name)) { return 13; } groupPass.put(name, password == null || password.length() == 0 ? "!" : String.valueOf(password.hashCode())); saveAccounts(); return 0; }
    // | (Trackers)
    public String getpid(String name) { for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) { String PID = (String) KEYS.nextElement(); Process process = (Process) sys.get(PID); if (process != null && process.name != null && name != null && name.equals(process.name)) { return PID; } } return null; } 
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
                String content = filename.equals("random") ? String.valueOf(random.nextInt(256)) : filename.equals("stdin") ? "" : filename.equals("stdout") ? "" : filename.equals("null") ? "\r" : filename.equals("zero") ? "\0" : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }

                filename = "/dev/" + filename;
            }
            else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/") || filename.startsWith("/root/")) {
                if (filename.startsWith("/root/") && !isRootCaller(scope)) { return null; }
                if ((filename.equals("/etc/shadow") || filename.equals("/etc/gshadow")) && (scope == null || !"root".equals(scope.get("USER")))) { return null; }
                String full = filename;
                int slash = filename.lastIndexOf('/');
                String dir = slash < 0 ? filename : filename.substring(0, slash + 1);
                String name = slash < 0 ? filename : filename.substring(slash + 1);
                int idx = vfsDirIndex(dir);
                if (idx != -1) {
                    if (useCache && cache.containsKey(full)) { return new ByteArrayInputStream((byte[]) cache.get(full)); }

                    byte[] content = read(name, loadRMS("OpenRMS", idx));
                    if (content != null) { if (useCache) { cache.put(full, content); } return new ByteArrayInputStream(content); }
                }
                filename = full;
            }
            else if (filename.startsWith("/proc/")) {
                String content = readProc(filename, getCallerUid(scope));
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }
                filename = "/proc/" + filename.substring(6);
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
    public String read(InputStream in, int chunkSize, boolean consume) {
        try {
            if (in == null) { return ""; }
            //if (chunkSize == -1) { consume = true; }
            if (consume) {
                InputStreamReader reader = new InputStreamReader(in, "UTF-8");
                StringBuffer sb = new StringBuffer();
                int ch;
                while ((ch = reader.read()) != -1) { sb.append((char) ch); }
                reader.close();
                
                return sb.toString();
            } else {
                byte[] buffer = new byte[chunkSize];
                int bytesRead = in.read(buffer, 0, chunkSize);
                if (bytesRead == -1) { return ""; }

                return new String(buffer, 0, bytesRead, "UTF-8");
            }
        } catch (Exception e) { return ""; }
    }
    public static String loadRMS(String filename, int index) { String result = ""; RecordStore RMS = null; try { RMS = RecordStore.openRecordStore(filename, true); if (RMS.getNumRecords() >= index) { byte[] data = RMS.getRecord(index); if (data != null) { result = new String(data); } } } catch (RecordStoreException e) { } try { if (RMS != null) { RMS.closeRecordStore(); } } catch (RecordStoreException e) { } return result; }
    // | (Write)
    public int write(String filename, String data, int id, Hashtable scope) { return write(filename, data.getBytes(), id, scope); }
    public int write(String filename, byte[] data, int id, Hashtable scope) {
        if ((filename = solvepath(filename, scope)) == null || filename.length() == 0) { return 2; } 
        else if (filename.startsWith("/mnt/")) { FileConnection fs = null; OutputStream out = null; try { fs = (FileConnection) Connector.open("file:///" + filename.substring(5), Connector.READ_WRITE); if (!fs.exists()) { fs.create(); } out = fs.openOutputStream(); out.write(data); out.flush(); } catch (Exception e) { return (e instanceof SecurityException) ? 13 : 1; } finally { out.close(); fs.close(); } } 
        else if (filename.startsWith("/home/")) { return writeRMS(filename.substring(6), data, 1); } 
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/") || filename.startsWith("/root/")) {
            String full = filename;
            int slash = filename.lastIndexOf('/');
            String dir = slash < 0 ? filename : filename.substring(0, slash + 1);
            String name = slash < 0 ? filename : filename.substring(slash + 1);
            int index = vfsDirIndex(dir);

            if (name.equals("") || index == -1) { return 2; } 
            else if (id != 0) { return 13; }
            else {
                if (index >= 6) { registerVfsDir(dir); }
                if (useCache) { cache.put(full, data); } return addFile(name, data, loadRMS("OpenRMS", index), index);
            }
        }
        else if (filename.startsWith("/dev/")) { if ((filename = filename.substring(5)).equals("")) { return 2; } else if (filename.equals("null")) { } else { return 5; } }
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
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/") || filename.startsWith("/root/")) {
            String full = filename;
            int slash = filename.lastIndexOf('/');
            String dir = slash < 0 ? filename : filename.substring(0, slash + 1);
            String name = slash < 0 ? filename : filename.substring(slash + 1);
            if (name.equals("")) { return 2; }
            if (id != 0) { return 13; }

            String subdir = dir + name + "/";
            if (fs.containsKey(subdir)) {
                int sidx = vfsDirIndex(subdir);
                if (sidx != -1) { writeRMS("OpenRMS", new byte[0], sidx); }
                fs.remove(subdir);
                Vector struct = (Vector) fs.get(dir);
                if (struct != null) { struct.removeElement(name + "/"); }
                unpersistVfsMount(subdir);
                if (useCache) { cache.remove(full); }
                return 0;
            }

            int index = vfsDirIndex(dir);
            if (index == -1) { return 5; }
            String content = loadRMS("OpenRMS", index);
            if (content.indexOf("[\1BEGIN:" + name + "\1]") == -1) { return 5; }

            if (useCache) { cache.remove(full); }
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
    // | (VFS Store Index)
    private static final int VFS_HASH_MOD = 97, VFS_RESERVED = 9;
    public int vfsDirIndex(String dir) {
        while (dir.length() > 1 && dir.endsWith("/")) { dir = dir.substring(0, dir.length() - 1); }
        if (dir.equals("/bin")) { return 3; }
        else if (dir.equals("/lib")) { return 4; }
        else if (dir.equals("/etc")) { return 5; }
        else if (dir.equals("/root")) { return 6; }
        else if (dir.equals("/dev") || dir.equals("/proc") || dir.equals("/tmp") || dir.equals("/home") || dir.equals("/mnt")) { return -1; }
        else if (dir.startsWith("/bin/") || dir.startsWith("/lib/") || dir.startsWith("/etc/") || dir.startsWith("/root/")) {
            int h = dir.hashCode();
            if (h < 0) { h = -h; }
            return VFS_RESERVED + (h % VFS_HASH_MOD);
        }
        return -1;
    }
    public void registerVfsDir(String dir) {
        mountVfsDir(dir);
        if (dir != null && dir.startsWith("/") && dir.endsWith("/") && dir.lastIndexOf('/', dir.length() - 2) > 0) { persistVfsMount(dir); }
    }
    public void mountVfsDir(String dir) {
        if (dir == null || !dir.startsWith("/") || !dir.endsWith("/")) { return; }
        if (!fs.containsKey(dir)) { Vector self = new Vector(); self.addElement(".."); fs.put(dir, self); }

        int base = dir.lastIndexOf('/', dir.length() - 2);
        if (base <= 0) { return; }

        String parent = dir.substring(0, base + 1);
        Vector struct = (Vector) fs.get(parent);
        if (struct == null) { mountVfsDir(parent); struct = (Vector) fs.get(parent); }
        if (struct != null) {
            String entry = dir.substring(base + 1, dir.length() - 1) + "/";
            if (!struct.contains(entry)) { struct.addElement(entry); }
        }
    }
    public void persistVfsMount(String dir) {
        try {
            String mount = dir.trim();
            String cfg = read("/etc/vfs.conf", globals);
            if (cfg.indexOf(mount) == -1) { write("/etc/vfs.conf", cfg.length() == 0 ? mount : cfg + "\n" + mount, 0, globals); }
        } catch (Exception e) { }
    }
    public void unpersistVfsMount(String dir) {
        try {
            String mount = dir.trim();
            String cfg = read("/etc/vfs.conf", globals);
            if (cfg.indexOf(mount) != -1) {
                String out = replace(cfg, "\n" + mount, "");
                out = replace(out, mount, "");
                write("/etc/vfs.conf", out, 0, globals);
            }
        } catch (Exception e) { }
    }
    public void restoreVfsMounts() {
        try {
            String cfg = read("/etc/vfs.conf", globals);
            if (cfg == null || cfg.length() == 0) { return; }
            String[] lines = split(cfg, '\n');
            for (int i = 0; i < lines.length; i++) {
                String d = lines[i].trim();
                if (d.length() == 0) { continue; }
                if (!d.endsWith("/")) { d = d + "/"; }
                mountVfsDir(d);
            }
        } catch (Exception e) { }
    }
    public boolean isRootCaller(Hashtable scope) { try { return scope != null && scope.containsKey("USER") && getUserID((String) scope.get("USER")) == 0; } catch (Exception e) { return false; } }
    public int getCallerUid(Hashtable scope) { try { if (scope != null && scope.containsKey("USER")) { int u = getUserID((String) scope.get("USER")); if (u != -1) { return u; } } } catch (Exception e) { } return 1000; }
    // | (/proc virtual filesystem)
    public String[] procFiles() { return new String[] { "cpuinfo", "meminfo", "uptime", "version" }; }
    public Vector procEntries(int uid) {
        Vector out = new Vector();
        for (Enumeration keys = sys.keys(); keys.hasMoreElements();) {
            String pid = (String) keys.nextElement();
            Process p = (Process) sys.get(pid);
            if (p == null) { continue; }
            if (uid == 0 || p.uid == uid) { out.addElement(pid + "/"); }
        }
        return out;
    }
    public Vector procDirEntries(String pidStr, int uid) {
        Vector out = new Vector();
        Process p = (Process) sys.get(pidStr);
        if (p == null || (uid != 0 && p.uid != uid)) { return out; }
        out.addElement("cmdline"); out.addElement("comm"); out.addElement("stat"); out.addElement("status");
        return out;
    }
    public String readProc(String path, int uid) {
        if (path == null || !path.startsWith("/proc/")) { return null; }
        String rest = path.substring(6);
        if (rest.length() == 0) { return null; }
        String[] parts = split(rest, '/');
        if (parts.length == 0) { return null; }

        String top = parts[0];
        if (parts.length == 1) {
            if (top.equals("uptime")) { return "" + ((System.currentTimeMillis() - uptime) / 1000); }
            else if (top.equals("version")) { return "OpenTTY " + build + " (J2ME Lua)"; }
            else if (top.equals("meminfo")) {
                return "MemTotal:      " + (runtime.totalMemory() / 1024) + " kB\nMemFree:       " + (runtime.freeMemory() / 1024) + " kB\nMemAvailable:  " + (runtime.freeMemory() / 1024) + " kB";
            }
            else if (top.equals("cpuinfo")) { return "processor\t: 0\nmodel name\t: J2ME Virtual CPU\nvendor_id\t: OpenTTY\n"; }
            return null;
        }

        String pidStr = top, file = parts[1];
        Process p = (Process) sys.get(pidStr);
        if (p == null) { return null; }
        if (uid != 0 && p.uid != uid) { return null; }

        long s = (System.currentTimeMillis() - p.startTime) / 1000;
        if (file.equals("status")) {
            return "Name:\t" + p.name + "\nState:\tR (running)\nPid:\t" + p.pid + "\nPPid:\t" + (p.pid.equals("1") ? "0" : "1") + "\nUid:\t" + p.uid + "\nGid:\t" + p.gid + "\nUtime:\t" + s + "\nStime:\t0\nPriority:\t" + p.priority + "\nNice:\t" + (p.priority - 10) + "\nThreads:\t1\nOwner:\t" + p.owner;
        }
        else if (file.equals("cmdline")) { return (p.cmd != null && p.cmd.length() > 0 ? p.cmd : p.name) + "\0"; }
        else if (file.equals("comm")) { return p.name != null ? p.name : ""; }
        else if (file.equals("stat")) { return p.pid + " (" + (p.name != null ? p.name : "") + ") R " + (p.pid.equals("1") ? "0" : "1") + " " + p.uid + " " + p.gid + " 0 0 0 0 " + s + " 0 0 " + s + " 0 0 20"; }
        return null;
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
    public int addFile(String filename, String content, String archive, int index) { return addFile(filename, content.getBytes(), archive, index); }
    public int addFile(String filename, byte[] data, String archive, int index) { return writeRMS("OpenRMS", (delFile(filename, archive) + ("[\1BEGIN:" + filename + "\1]\n" + (isPureText(data) ? new String(data) : "[B64]" + encodeBase64(data)) + "\n[\1END\1]\n")).getBytes(), index); }

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
    public int uid = 1000, gid = 1000, priority = DEFAULT_PRIORITY;

    public static final int MIN_PRIORITY = 0, DEFAULT_PRIORITY = 10, MAX_PRIORITY = 20;
  
    public Object stdout, stderr;
    public Object handler = null, sighandler = null;
    public Displayable screen = null;
    public Lua lua = null;
    public ELF elf = null;

    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable scope) { this.lua = new Lua(midlet, uid, pid, this, stdout, scope); this.name = name; this.owner = owner; this.uid = uid; this.gid = midlet.accountsLoaded && midlet.userGID.containsKey(owner) ? ((Integer) midlet.userGID.get(owner)).intValue() : uid; this.pid = pid; this.stdout = stdout; this.stderr = stdout; this.scope = scope; this.startTime = System.currentTimeMillis(); }
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable args, Hashtable scope) { this.elf = new ELF(midlet, args, stdout, scope, uid, pid, this); this.name = name; this.owner = owner; this.uid = uid; this.gid = midlet.accountsLoaded && midlet.userGID.containsKey(owner) ? ((Integer) midlet.userGID.get(owner)).intValue() : uid; this.pid = pid; this.stdout = stdout; this.stderr = stdout; this.scope = scope; this.startTime = System.currentTimeMillis(); }

    public String toString() { return "{ name=" + name + ", owner=" + owner + ", uid=" + uid + ", pid=" + pid + ", " + (lua != null ? "lua=" + lua + ", " : elf != null ? "elf=" + elf + ", " : "") + (handler != null ? "handler=" + handler + ", " : "") + "priority=" + priority + ", scope=" + scope + ", db=" + db + " }"; }
}
// |
// | 10k commits
// Goodbye 2025
// EOF
