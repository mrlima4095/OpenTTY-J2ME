import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;
// |
// OpenTTY Java 8 Desktop (Swing Port)
public class OpenTTY {
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
    public String username = "", build = "2026.1.18.1-03x27";
    // |
    // Directories
    public String rootDir = "", homeDir = "", binDir = "", etcDir = "", libDir = "";
    // |
    // Swing UI
    public JFrame frame;
    public JTextArea stdout;
    public JTextField stdin;
    public JScrollPane scrollPane;
    // |
    // Main Entry Point
    public static void main(String[] args) {
        final OpenTTY app = new OpenTTY();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                app.init();
                app.startApp();
            }
        });
    }
    // |
    // Constructor
    public OpenTTY() {
        rootDir = detectRootDir();
        homeDir = rootDir + "home" + File.separator;
        binDir = rootDir + "bin" + File.separator;
        etcDir = rootDir + "etc" + File.separator;
        libDir = rootDir + "lib" + File.separator;
        new File(homeDir).mkdirs();
        username = loadRMS("OpenRMS", 1);
    }
    // |
    private String detectRootDir() {
        if (new File("bin").isDirectory() && new File("home").isDirectory()) return "";
        if (new File("java/bin").isDirectory() && new File("java/home").isDirectory()) return "java" + File.separator;
        try {
            String classPath = OpenTTY.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File classDir = new File(classPath).getParentFile();
            if (classDir != null) {
                if (new File(classDir, "bin").isDirectory() && new File(classDir, "home").isDirectory()) return classDir.getAbsolutePath() + File.separator;
                File javaDir = new File(classDir, "java");
                if (new File(javaDir, "bin").isDirectory() && new File(javaDir, "home").isDirectory()) return javaDir.getAbsolutePath() + File.separator;
            }
        } catch (Exception e) { }
        return "";
    }
    // |
    // Swing Initialization
    public void init() {
        frame = new JFrame("OpenTTY");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        stdout = new JTextArea();
        stdout.setEditable(false);
        stdout.setFont(new Font("Monospaced", Font.PLAIN, 14));
        stdout.setBackground(Color.BLACK);
        stdout.setForeground(new Color(0, 255, 0));
        stdout.setCaretColor(new Color(0, 255, 0));
        stdout.setMargin(new Insets(4, 4, 4, 4));
        scrollPane = new JScrollPane(stdout);
        frame.add(scrollPane, BorderLayout.CENTER);
        stdin = new JTextField();
        stdin.setFont(new Font("Monospaced", Font.PLAIN, 14));
        stdin.setBackground(new Color(40, 40, 40));
        stdin.setForeground(new Color(0, 255, 0));
        stdin.setCaretColor(new Color(0, 255, 0));
        stdin.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0, 100, 0)),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        frame.add(stdin, BorderLayout.SOUTH);
        stdin.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String input = stdin.getText().trim();
                stdin.setText("");
                if (!input.isEmpty()) { print("$ " + input, stdout); }
            }
        });
        try { frame.setIconImage(new ImageIcon(new File(rootDir + "icon.png").getAbsolutePath()).getImage()); } catch (Exception e) { }
        frame.setVisible(true);
    }
    // |
    // MIDlet Lifecycle Equivalent
    public void startApp() {
        if (sys.containsKey("1")) { return; }
        boolean user = username.equals(""), pword = passwd().equals("");
        if (user || pword) {
            String msg = "Create " + (user && pword ? "your credentials (user and password)" : user ? "an username" : "a password") + " for your account";
            if (user && pword) {
                JPanel panel = new JPanel(new GridLayout(3, 2, 4, 4));
                panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                panel.add(new JLabel(msg));
                panel.add(new JLabel(""));
                panel.add(new JLabel("Username:"));
                JTextField userField = new JTextField(20);
                panel.add(userField);
                panel.add(new JLabel("Password:"));
                JPasswordField passField = new JPasswordField(20);
                panel.add(passField);
                int result = JOptionPane.showConfirmDialog(frame, panel, "OpenTTY - Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String u = userField.getText().trim();
                    String p = new String(passField.getPassword()).trim();
                    if (u.equals("") || p.equals("")) { warn("Login", "Missing Credentials!"); startApp(); return; }
                    if (u.equals("root")) { warn("Login", "Invalid user name!"); startApp(); return; }
                    writeRMS("OpenRMS", u.getBytes(), 1);
                    writeRMS("OpenRMS", String.valueOf(p.hashCode()).getBytes(), 2);
                    username = u;
                } else { System.exit(0); }
            } else if (user) {
                JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
                panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                panel.add(new JLabel(msg));
                panel.add(new JLabel(""));
                panel.add(new JLabel("Username:"));
                JTextField userField = new JTextField(20);
                panel.add(userField);
                int result = JOptionPane.showConfirmDialog(frame, panel, "OpenTTY - Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String u = userField.getText().trim();
                    if (u.equals("")) { warn("Login", "Missing Credentials!"); startApp(); return; }
                    if (u.equals("root")) { warn("Login", "Invalid user name!"); startApp(); return; }
                    writeRMS("OpenRMS", u.getBytes(), 1);
                    username = u;
                } else { System.exit(0); }
            } else {
                JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
                panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
                panel.add(new JLabel(msg));
                panel.add(new JLabel(""));
                panel.add(new JLabel("Password:"));
                JPasswordField passField = new JPasswordField(20);
                panel.add(passField);
                int result = JOptionPane.showConfirmDialog(frame, panel, "OpenTTY - Login", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (result == JOptionPane.OK_OPTION) {
                    String p = new String(passField.getPassword()).trim();
                    if (p.equals("")) { warn("Login", "Missing Credentials!"); startApp(); return; }
                    writeRMS("OpenRMS", String.valueOf(p.hashCode()).getBytes(), 2);
                } else { System.exit(0); }
            }
        }
        try {
            Hashtable args = new Hashtable();
            args.put(new Double(0), "/bin/init");
            globals.put("PWD", "/home/");
            globals.put("USER", "root");
            globals.put("ROOT", "/");
            globals.put("ALIAS", new Hashtable());
            userID.put(username, 1000);
            Process proc = new Process(this, "init", "/bin/init", "root", 0, "1", stdout, globals);
            sys.put("1", proc);
            proc.lua.globals.put("arg", args);
            proc.handler = proc.lua.getKernel();
            proc.lua.currentSource = "/bin/init";
            proc.lua.tokens = proc.lua.tokenize(read("/bin/init", globals));
            while (proc.lua.peek().type != 0) { Object res = proc.lua.statement(globals); if (proc.lua.doreturn) { break; } }
        }
        catch (IllegalStateException e) { }
        catch (OutOfMemoryError e) {
            JOptionPane.showMessageDialog(frame,
                "Insufficient Memory\nUsed Memory: " + ((runtime.totalMemory() / 1024) - (runtime.freeMemory())) +
                " KB\nFree Memory: " + (runtime.freeMemory() / 1024) + " KB\nTotal Memory: " + (runtime.totalMemory() / 1024) + " KB",
                "SandBox", JOptionPane.ERROR_MESSAGE);
        }
        catch (Throwable e) {
            String title = e instanceof Exception ? "SandBox" : "Kernel Panic";
            String message = "An error occurred while OpenTTY tried to start!\n\nError: " + getCatch(e);
            String detail = e instanceof Exception ?
                "If you tried to install a program in /bin/init it can be the error" :
                "Try to clear your data or update OpenTTY";
            JOptionPane.showMessageDialog(frame, message + "\n\n" + detail, title, JOptionPane.ERROR_MESSAGE);
        }
    }
    // |
    public void destroyApp(boolean unconditional) { System.exit(0); }
    // |
    public Display display = new Display();
    public String getAppProperty(String key) { return System.getProperty(key); }
    public boolean platformRequest(String url) { return false; }
    public Image readImg(String filename, Hashtable scope) { try { InputStream is = getInputStream(filename, scope); Image img = Image.createImage(is); is.close(); return img; } catch (Exception e) { return Image.createImage(16, 16); } }
    // |
    public OpenTTY getInstance() { return this; }
    public String getThreadName(Thread thr) {
        String name = thr.getName();
        String[] generic = { "Thread-0", "Thread-1", "main", "AWT-EventQueue-0", "Signal Dispatcher", "Finalizer" };
        for (int i = 0; i < generic.length; i++) { if (name.equals(generic[i])) { name = "OpenTTY"; break; } }
        return name;
    }
    // |
    // Password
    public String passwd() { return loadRMS("OpenRMS", 2); }
    public boolean passwd(String query) { return query != null && String.valueOf(query.hashCode()).equals(loadRMS("OpenRMS", 2)); }
    // |
    // String Utils
    public String getCommand(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return text; } else { return text.substring(0, spaceIndex); } }
    public String getArgument(String text) { int spaceIndex = text.indexOf(' '); if (spaceIndex == -1) { return ""; } else { return text.substring(spaceIndex + 1).trim(); } }
    // |
    public String replace(String source, String target, String replacement) {
        StringBuffer result = new StringBuffer();
        int start = 0, end;
        while ((end = source.indexOf(target, start)) >= 0) {
            result.append(source.substring(start, end));
            result.append(replacement);
            start = end + target.length();
        }
        result.append(source.substring(start));
        return result.toString();
    }
    public String env(String text, Hashtable scope) {
        if (scope != null) {
            text = replace(text, "$PATH", (String) scope.get("PWD"));
            for (Enumeration keys = scope.keys(); keys.hasMoreElements();) {
                String key = (String) keys.nextElement();
                text = replace(text, "$" + key, (String) scope.get(key));
            }
        }
        return env(text);
    }
    public String env(String text) {
        text = replace(text, "$USER", username);
        for (Enumeration keys = attributes.keys(); keys.hasMoreElements();) {
            String key = (String) keys.nextElement();
            text = replace(text, "$" + key, (String) attributes.get(key));
        }
        text = replace(text, "\\.", "\\");
        return escape(text);
    }
    public String escape(String text) {
        text = replace(text, "\\n", "\n");
        text = replace(text, "\\r", "\r");
        text = replace(text, "\\t", "\t");
        text = replace(text, "\\b", "\b");
        text = replace(text, "\\\\", "\\");
        text = replace(text, "\\.", "\\");
        return text;
    }
    public String getCatch(Throwable e) {
        String message = e.getMessage();
        return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : e.getClass().getName() + ": " + message;
    }
    // |
    public String getcontent(String file, Hashtable scope) { return file.startsWith("/") ? read(file, scope) : read(((String) scope.get("PWD")) + file, scope); }
    public String getpattern(String text) { return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? text.substring(1, text.length() - 1) : text.trim(); }
    // |
    // Arrays
    public String[] split(String content, char div) {
        Vector lines = new Vector();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == div) { lines.addElement(content.substring(start, i)); start = i + 1; }
        }
        if (start < content.length()) { lines.addElement(content.substring(start)); }
        String[] result = new String[lines.size()];
        lines.copyInto(result);
        return result;
    }
    public String[] splitArgs(String input) {
        Vector result = new Vector();
        StringBuffer current = new StringBuffer();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;
        boolean escaped = false;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (escaped) { current.append(c); escaped = false; continue; }
            if (c == '\\') { if (inDoubleQuotes || inSingleQuotes) { escaped = true; } else { current.append(c); } continue; }
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
    // Generators
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    // |
    // User Manager
    public int getUserID(String user) { return user.equals("root") ? 0 : user.equals(username) ? 1000 : userID.containsKey(user) ? ((Integer) userID.get(user)).intValue() : -1; }
    public String getUser(int uid) {
        if (uid == 0) { return "root"; } else if (uid == 1000) { return username; }
        for (Enumeration keys = sys.keys(); keys.hasMoreElements();) {
            String user = (String) keys.nextElement();
            Integer id = (Integer) userID.get(user);
            if (id != null && id.intValue() == uid) { return user; }
        }
        return null;
    }
    // |
    // Trackers
    public String getpid(String name) {
        for (Enumeration KEYS = sys.keys(); KEYS.hasMoreElements();) {
            String PID = (String) KEYS.nextElement();
            Process process = (Process) sys.get(PID);
            if (process != null && process.name != null && name != null && name.equals(process.name)) { return PID; }
        }
        return null;
    }
    // |
    // Window-Based Interfaces
    public int warn(String title, String message) {
        if (message == null || message.length() == 0) { return 2; }
        JOptionPane.showMessageDialog(frame, message, title, JOptionPane.WARNING_MESSAGE);
        return 0;
    }
    // |
    public void print(String message, Object stdout) { print(message, stdout, 1000, globals); }
    public void print(String message, Object stdout, int id, Hashtable scope) {
        if (stdout == null) { }
        else if (stdout instanceof JTextArea) {
            JTextArea area = (JTextArea) stdout;
            String current = area.getText();
            String output = (current == null || current.length() == 0) ? message : current + "\n" + message;
            area.setText(output);
            area.setCaretPosition(area.getDocument().getLength());
        }
        else if (stdout instanceof StringBuffer) { ((StringBuffer) stdout).append("\n").append(message); }
        else if (stdout instanceof String) { write((String) stdout, read((String) stdout, scope) + "\n" + message, 1000, scope); }
        else if (stdout instanceof OutputStream) { try { ((OutputStream) stdout).write((message + "\n").getBytes("UTF-8")); ((OutputStream) stdout).flush(); } catch (Exception e) { } }
    }
    // |
    // API 003 - File System
    // | (Read)
    public InputStream getInputStream(String filename, Hashtable scope) throws Exception {
        if ((filename = solvepath(filename, scope)).startsWith("/home/")) {
            String name = filename.substring(6);
            if (name.equals("")) { return null; }
            File f = new File(homeDir + name);
            if (f.exists() && f.isFile()) { return new FileInputStream(f); }
            return null;
        }
        else if (filename.startsWith("/mnt/")) {
            String path = filename.substring(5);
            if (path.equals("")) { return null; }
            File f = new File(path);
            if (f.exists() && f.isFile()) { return new FileInputStream(f); }
            return null;
        }
        else if (filename.startsWith("/tmp/")) {
            String key = filename.substring(5);
            return tmp.containsKey(key) ? new ByteArrayInputStream((byte[]) tmp.get(key)) : null;
        }
        else {
            if (filename.startsWith("/dev/")) {
                filename = filename.substring(5);
                String content = filename.equals("random") ? String.valueOf(random.nextInt(256))
                    : filename.equals("stdin") ? stdin.getText()
                    : filename.equals("stdout") ? stdout.getText()
                    : filename.equals("null") ? "\r"
                    : filename.equals("zero") ? "\0"
                    : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }
                filename = "/dev/" + filename;
            }
            else if (filename.startsWith("/bin/")) {
                String basename = filename.substring(5);
                if (useCache && cache.containsKey("/bin/" + basename)) { return new ByteArrayInputStream((byte[]) cache.get("/bin/" + basename)); }
                byte[] content = read(basename, loadRMS("OpenRMS", 3));
                if (content != null) { if (useCache) { cache.put("/bin/" + basename, content); } return new ByteArrayInputStream(content); }
                File f = new File(binDir + basename);
                if (f.exists() && f.isFile()) {
                    FileInputStream fis = new FileInputStream(f);
                    if (useCache) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096]; int n;
                        while ((n = fis.read(buf)) != -1) { baos.write(buf, 0, n); }
                        byte[] data = baos.toByteArray();
                        cache.put("/bin/" + basename, data);
                        fis.close();
                        return new ByteArrayInputStream(data);
                    }
                    return fis;
                }
                filename = "/bin/" + basename;
            }
            else if (filename.startsWith("/etc/")) {
                String basename = filename.substring(5);
                if (useCache && cache.containsKey("/etc/" + basename)) { return new ByteArrayInputStream((byte[]) cache.get("/etc/" + basename)); }
                byte[] content = read(basename, loadRMS("OpenRMS", 5));
                if (content != null) { if (useCache) { cache.put("/etc/" + basename, content); } return new ByteArrayInputStream(content); }
                File f = new File(etcDir + basename);
                if (f.exists() && f.isFile()) {
                    FileInputStream fis = new FileInputStream(f);
                    if (useCache) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096]; int n;
                        while ((n = fis.read(buf)) != -1) { baos.write(buf, 0, n); }
                        byte[] data = baos.toByteArray();
                        cache.put("/etc/" + basename, data);
                        fis.close();
                        return new ByteArrayInputStream(data);
                    }
                    return fis;
                }
                filename = "/etc/" + basename;
            }
            else if (filename.startsWith("/lib/")) {
                String basename = filename.substring(5);
                if (useCache && cache.containsKey("/lib/" + basename)) { return new ByteArrayInputStream((byte[]) cache.get("/lib/" + basename)); }
                byte[] content = read(basename, loadRMS("OpenRMS", 4));
                if (content != null) { if (useCache) { cache.put("/lib/" + basename, content); } return new ByteArrayInputStream(content); }
                File f = new File(libDir + basename);
                if (f.exists() && f.isFile()) {
                    FileInputStream fis = new FileInputStream(f);
                    if (useCache) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buf = new byte[4096]; int n;
                        while ((n = fis.read(buf)) != -1) { baos.write(buf, 0, n); }
                        byte[] data = baos.toByteArray();
                        cache.put("/lib/" + basename, data);
                        fis.close();
                        return new ByteArrayInputStream(data);
                    }
                    return fis;
                }
                filename = "/lib/" + basename;
            }
            else if (filename.startsWith("/proc/")) {
                String procfile = filename.substring(6);
                String content = procfile.equals("uptime") ? "" + ((System.currentTimeMillis() - uptime) / 1000) : null;
                if (content != null) { return new ByteArrayInputStream(content.getBytes("UTF-8")); }
                filename = "/proc/" + procfile;
            }
            InputStream is = getClass().getResourceAsStream(filename);
            if (is != null) { return is; }
            File f = new File(filename);
            if (f.exists() && f.isFile()) { return new FileInputStream(f); }
            return null;
        }
    }
    // |
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
    public String loadRMS(String filename, int index) {
        try {
            String path = getRMSFilePath(filename, index);
            if (path == null) { return ""; }
            File f = new File(path);
            if (!f.exists()) { return ""; }
            FileInputStream fis = new FileInputStream(f);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = fis.read(buf)) != -1) { baos.write(buf, 0, n); }
            fis.close();
            return new String(baos.toByteArray(), "UTF-8");
        } catch (Exception e) { return ""; }
    }
    // |
    // | (Write)
    public int write(String filename, String data, int id, Hashtable scope) {
        try { return write(filename, data.getBytes("UTF-8"), id, scope); } catch (Exception e) { return 1; }
    }
    public int write(String filename, byte[] data, int id, Hashtable scope) {
        if ((filename = solvepath(filename, scope)) == null || filename.length() == 0) { return 2; }
        else if (filename.startsWith("/mnt/")) {
            try {
                String path = filename.substring(5);
                if (path.equals("")) { return 2; }
                File f = new File(path);
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) { parent.mkdirs(); }
                FileOutputStream out = new FileOutputStream(f);
                out.write(data);
                out.flush();
                out.close();
            } catch (Exception e) { return (e instanceof SecurityException) ? 13 : 1; }
        }
        else if (filename.startsWith("/home/")) {
            String name = filename.substring(6);
            if (name.equals("")) { return 2; }
            try {
                File f = new File(homeDir + name);
                FileOutputStream out = new FileOutputStream(f);
                out.write(data);
                out.flush();
                out.close();
            } catch (Exception e) { return 1; }
        }
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4);
            String basename = filename.substring(5);
            if (basename.equals("")) { return 2; }
            else if (id != 0) { return 13; }
            else {
                if (useCache) { cache.put("/" + base + "/" + basename, data); }
                int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
                String archive = loadRMS("OpenRMS", index);
                return addFile(basename, data, archive, base);
            }
        }
        else if (filename.startsWith("/dev/")) {
            String dev = filename.substring(5);
            if (dev.equals("")) { return 2; }
            else if (dev.equals("null")) { }
            else if (dev.equals("stdin")) { try { stdin.setText(new String(data, "UTF-8")); } catch (Exception e) { } }
            else if (dev.equals("stdout")) { stdout.setText(new String(data, "UTF-8")); }
            else { return 5; }
        }
        else if (filename.startsWith("/tmp/")) {
            String key = filename.substring(5);
            if (key.equals("")) { return 2; }
            else { tmp.put(key, data); }
        }
        else if (filename.startsWith("/")) { return 5; }
        return 0;
    }
    public int writeRMS(String filename, byte[] data, int index) {
        try {
            String path = getRMSFilePath(filename, index);
            if (path == null) {
                File f = new File(homeDir + filename);
                FileOutputStream out = new FileOutputStream(f);
                out.write(data);
                out.flush();
                out.close();
            } else {
                File f = new File(path);
                File parent = f.getParentFile();
                if (parent != null && !parent.exists()) { parent.mkdirs(); }
                FileOutputStream out = new FileOutputStream(f);
                out.write(data);
                out.flush();
                out.close();
            }
        } catch (Exception e) { return 1; }
        return 0;
    }
    // |
    private String getRMSFilePath(String storeName, int index) {
        if (storeName.equals("OpenRMS")) {
            switch (index) {
                case 1: return homeDir + "username";
                case 2: return homeDir + "passwd";
                case 3: return homeDir + "bin.archive";
                case 4: return homeDir + "lib.archive";
                case 5: return homeDir + "etc.archive";
            }
        }
        return null;
    }
    // |
    public int deleteFile(String filename, int id, Hashtable scope) {
        if ((filename = solvepath(filename, scope)) == null || filename.length() == 0) { return 2; }
        else if (filename.startsWith("/home/")) {
            try {
                String name = filename.substring(6);
                if (name.equals("")) { return 2; }
                if (name.equals("OpenRMS")) { return 13; }
                File f = new File(homeDir + name);
                if (!f.exists()) { return 127; }
                if (!f.delete()) { return 1; }
            } catch (Exception e) { return 1; }
        }
        else if (filename.startsWith("/mnt/")) {
            try {
                File f = new File(filename.substring(5));
                if (!f.exists()) { return 127; }
                if (!f.delete()) { return 1; }
            } catch (Exception e) { return (e instanceof SecurityException) ? 13 : 1; }
        }
        else if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
            String base = filename.substring(1, 4);
            String name = filename.substring(5);
            if (name.equals("")) { return 2; }
            if (id != 0) { return 13; }
            int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
            String content = loadRMS("OpenRMS", index);
            if (content.indexOf("[\1BEGIN:" + name + "\1]") == -1) { return 5; }
            if (useCache) { cache.remove("/" + base + "/" + name); }
            return writeRMS("OpenRMS", delFile(name, content).getBytes(), index);
        }
        else if (filename.startsWith("/tmp/")) {
            String key = filename.substring(5);
            if (key.equals("")) { }
            else if (tmp.containsKey(key)) { tmp.remove(key); }
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
            if (part.equals(".")) { continue; }
            else if (part.equals("..")) {
                if (components.size() > 0) {
                    if (!components.lastElement().equals("")) { components.removeElementAt(components.size() - 1); }
                }
            } else { components.addElement(part); }
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
        if (fullPath.endsWith("/") && !result.toString().endsWith("/")) { result.append("/"); }
        return result.toString();
    }
    public String solvepath(String path, Hashtable scope) {
        String root = scope.containsKey("ROOT") ? (String) scope.get("ROOT") : "";
        if (path == null) { return "/"; }
        else if (root.equals("/") || path.startsWith("/dev/") || path.startsWith("/mnt/") || path.startsWith("/proc/") || path.startsWith("/tmp/")) { return path; }
        else if (path.startsWith("/")) { return root.endsWith("/") ? (root.length() > 1 ? root + path.substring(1) : root) : root + path; }
        return path;
    }
    // | (Archive Structures)
    public int addFile(String filename, String content, String archive, String base) {
        try { return addFile(filename, content.getBytes("UTF-8"), archive, base); } catch (Exception e) { return 1; }
    }
    public int addFile(String filename, byte[] data, String archive, String base) {
        int index = base.equals("bin") ? 3 : base.equals("etc") ? 5 : 4;
        String newArchive = delFile(filename, archive) + ("[\1BEGIN:" + filename + "\1]\n" + (isPureText(data) ? new String(data, "UTF-8") : "[B64]" + encodeBase64(data)) + "\n[\1END\1]\n");
        return writeRMS("OpenRMS", newArchive.getBytes(), index);
    }
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
        try {
            if (content.startsWith("[B64]")) { return decodeBase64(content.substring(5)); }
            else { return content.getBytes("UTF-8"); }
        } catch (Exception e) { return content.getBytes(); }
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
            if (c != '\n' && c != '\r' && c != ' ' && c != '\t') { clean.append(c); }
        }
        data = clean.toString();
        if (data.length() % 4 != 0) { return null; }
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
    // |
    public boolean isPureText(byte[] data) {
        int textCount = 0;
        int sampleSize = Math.min(data.length, 100);
        for (int i = 0; i < sampleSize; i++) {
            byte b = data[i];
            if ((b >= 32 && b <= 126) || b == 9 || b == 10 || b == 13) { textCount++; }
        }
        return (textCount * 100) > (sampleSize * 95);
    }
    // |
    // Java Virtual Machine
    public int javaClass(String name) { try { Class.forName(name); return 0; } catch (ClassNotFoundException e) { return 3; } }
    public String getName() {
        String s;
        StringBuffer BUFFER = new StringBuffer();
        if ((s = System.getProperty("java.vm.name")) != null) {
            BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor"));
            if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); }
            if ((s = System.getProperty("java.vm.specification.name")) != null) { BUFFER.append('\n').append(s); }
        } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) {
            BUFFER.append("J9 VM, IBM (").append(s).append(')');
            if ((s = System.getProperty("java.fullversion")) != null) { BUFFER.append("\n\n").append(s); }
        } else {
            BUFFER.append("Java HotSpot(TM)");
            if ((s = System.getProperty("java.vm.version")) != null) { BUFFER.append('\n').append(s); }
        }
        return BUFFER.append('\n').toString();
    }
}
// |
// Process
class Process {
    public OpenTTY midlet = null;
    public String name, owner, pid, cmd;
    public Hashtable scope, db = new Hashtable(), net = new Hashtable();
    public final long startTime;
    public int uid = 1000, priority = DEFAULT_PRIORITY;
    public static final int MIN_PRIORITY = 0, DEFAULT_PRIORITY = 10, MAX_PRIORITY = 20;
    public Object stdout, stderr;
    public Object handler = null, sighandler = null;
    public Lua lua = null;
    public ELF elf = null;
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable scope) {
        this.lua = new Lua(midlet, uid, pid, this, stdout, scope);
        this.name = name; this.owner = owner; this.uid = uid; this.pid = pid;
        this.stdout = stdout; this.stderr = stdout; this.scope = scope;
        this.startTime = System.currentTimeMillis();
    }
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable args, Hashtable scope) {
        this.elf = new ELF(midlet, args, stdout, scope, uid, pid, this);
        this.name = name; this.owner = owner; this.uid = uid; this.pid = pid;
        this.stdout = stdout; this.stderr = stdout; this.scope = scope;
        this.startTime = System.currentTimeMillis();
    }
    public String toString() {
        return "{ name=" + name + ", owner=" + owner + ", uid=" + uid + ", pid=" + pid + ", " +
            (lua != null ? "lua=" + lua + ", " : elf != null ? "elf=" + elf + ", " : "") +
            (handler != null ? "handler=" + handler + ", " : "") +
            "priority=" + priority + ", scope=" + scope + ", db=" + db + " }";
    }
}
// |
// EOF
