import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

// OpenTTY MIDlet - Java 8 Version
public class OpenTTY {
    // Behavior Settings
    public long uptime = System.currentTimeMillis();
    public boolean useCache = true, debug = false;
    
    // System Objects
    public int lastID = 1000;
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Object shell;
    
    public Hashtable<String, Object> attributes = new Hashtable<>(), fs = new Hashtable<>(), sys = new Hashtable<>(), tmp = new Hashtable<>(), cache = new Hashtable<>(), cacheLua = new Hashtable<>(), graphics = new Hashtable<>(), servers = new Hashtable<>(), globals = new Hashtable<>(), userID = new Hashtable<>();
    public String username = "", build = "2026-1.18.1-03x27";
    
    // Graphics - Java 8 Swing
    public JFrame frame;
    public JTextArea stdout;
    public JTextField stdin;
    public JScrollPane scrollPane;
    
    // Root directory - current working directory
    public String rootDir = System.getProperty("user.dir");
    public String homeDir = rootDir + File.separator + "home";
    public String binDir = rootDir + File.separator + "bin";
    public String etcDir = rootDir + File.separator + "etc";
    public String libDir = rootDir + File.separator + "lib";
    
    // Keys file
    public String keysFile = System.getProperty("user.home") + File.separator + ".opentty-keys";
    
    // MIDlet Loader
    public static void main(String[] args) {
        new OpenTTY().startApp();
    }
    
    public void startApp() {
        username = read("/home/OpenRMS", globals);
        
        if (sys.containsKey("1")) { }
        else {
            boolean user = username.equals(""), pword = passwd().equals("");
            if (user || pword) {
                showLoginScreen(user, pword);
            } else {
                try {
                    Hashtable<Object, Object> args = new Hashtable<>(); 
                    args.put(0, "/bin/init");
                    globals.put("PWD", "/home/"); 
                    globals.put("USER", "root"); 
                    globals.put("ROOT", "/"); 
                    globals.put("ALIAS", new Hashtable<>()); 
                    userID.put(username, 1000);
                    
                    Process proc = new Process(this, "init", "/bin/init", "root", 0, "1", stdout, globals);

                    sys.put("1", proc); 
                    proc.lua.globals.put("arg", args); 
                    proc.handler = proc.lua.getKernel();
                    proc.lua.tokens = proc.lua.tokenize(read("/bin/init", globals)); 

                    while (proc.lua.peek().type != 0) { 
                        Object res = proc.lua.statement(globals); 
                        if (proc.lua.doreturn) { break; } 
                    }
                }
                catch (IllegalStateException e) { 
                    showError("Illegal State", e.getMessage());
                }
                catch (OutOfMemoryError e) {
                    showError("Insufficient Memory", 
                        "Used Memory: " + ((runtime.totalMemory() / 1024) - (runtime.freeMemory())) + " KB\n" +
                        "Free Memory: " + (runtime.freeMemory() / 1024) + " KB\n" +
                        "Total Memory: " + (runtime.totalMemory() / 1024) + "KB total");
                }
                catch (Throwable e) {
                    showError(e instanceof Exception ? "SandBox" : "Kernel Panic", 
                        "An error occurred while OpenTTY tried to start!\n\nError: " + getCatch(e) + "\n" +
                        (e instanceof Exception ? "If you tried to install a program in /bin/init it can be the error" : 
                        "Try to clear your data or update OpenTTY"));
                }
            }
        }
    }
    
    public void pauseApp() { }
    public void destroyApp(boolean unconditional) { 
        if (frame != null) {
            frame.dispose();
        }
        System.exit(0);
    }
    
    private void writeUserData(String user, String password) {
        try {
            // Write username to /home/OpenRMS for compatibility
            if (!user.equals("")) {
                write("/home/OpenRMS", user, 0, globals);
                username = user;
            }
            
            // Write credentials to ~/.opentty-keys
            Properties props = new Properties();
            File keyFile = new File(keysFile);
            if (keyFile.exists()) {
                props.load(new FileInputStream(keyFile));
            }
            if (!user.equals("")) props.setProperty("username", user);
            if (!password.equals("")) props.setProperty("password", String.valueOf(password.hashCode()));
            props.store(new FileOutputStream(keyFile), "OpenTTY Keys");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void logged() {
        JOptionPane.showMessageDialog(null, "Login successful!\nReopen application to access console", 
            "OpenTTY", JOptionPane.INFORMATION_MESSAGE);
        destroyApp(true);
    }

    private void showLoginScreen(boolean needUser, boolean needPass) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        JLabel message = new JLabel(":: Create " + 
            (needUser && needPass ? "your credentials (user and password)" : 
             needUser ? "an username" : "a password") + " to your account");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        panel.add(message, gbc);
        
        JTextField userField = null;
        JPasswordField passField = null;
        
        if (needUser) {
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            panel.add(new JLabel("Username:"), gbc);
            userField = new JTextField(20);
            gbc.gridx = 1;
            panel.add(userField, gbc);
        }
        
        if (needPass) {
            gbc.gridx = 0;
            gbc.gridy = needUser ? 2 : 1;
            panel.add(new JLabel("Password:"), gbc);
            passField = new JPasswordField(20);
            gbc.gridx = 1;
            panel.add(passField, gbc);
        }
        
        int option = JOptionPane.showConfirmDialog(null, panel, "OpenTTY - Login", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (option == JOptionPane.CANCEL_OPTION || option == JOptionPane.CLOSED_OPTION) {
            destroyApp(true);
            return;
        }
        
        try {
            if (needUser && needPass) {
                String user = userField.getText().trim();
                String password = new String(passField.getPassword()).trim();
                if (user.equals("") || password.equals("")) {
                    warn("Login", "Missing Credentials!");
                    showLoginScreen(needUser, needPass);
                } else if (user.equals("root")) {
                    warn("Login", "Invalid user name!");
                    showLoginScreen(needUser, needPass);
                } else {
                    writeUserData(user, password);
                    logged();
                }
            } else if (needUser) {
                String user = userField.getText().trim();
                if (user.equals("")) {
                    warn("Login", "Missing Credentials!");
                    showLoginScreen(needUser, needPass);
                } else if (user.equals("root")) {
                    warn("Login", "Invalid user name!");
                    showLoginScreen(needUser, needPass);
                } else {
                    writeUserData(user, "");
                    logged();
                }
            } else if (needPass) {
                String password = new String(passField.getPassword()).trim();
                if (password.equals("")) {
                    warn("Login", "Missing Credentials!");
                    showLoginScreen(needUser, needPass);
                } else {
                    writeUserData("", password);
                    logged();
                }
            }
        } catch (Exception e) {
            showError("Login Error", e.getMessage());
        }
    }
    
    private void showError(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }
    
    // Control Thread
    public OpenTTY getInstance() { return this; }
    public String getThreadName(Thread thr) { 
        String name = thr.getName(); 
        String[] generic = { "Thread-0", "Thread-1", "MIDletEventQueue", "main" }; 
        for (int i = 0; i < generic.length; i++) { 
            if (name.equals(generic[i])) { 
                name = "MIDlet"; 
                break; 
            } 
        } 
        return name; 
    }
    
    public static String passwd() { 
        try {
            File keyFile = new File(System.getProperty("user.home") + File.separator + ".opentty-keys");
            if (keyFile.exists()) {
                Properties props = new Properties();
                props.load(new FileInputStream(keyFile));
                return props.getProperty("password", "");
            }
        } catch (Exception e) {}
        return ""; 
    }
    public static boolean passwd(String query) { 
        return query != null && String.valueOf(query.hashCode()).equals(passwd()); 
    }
    
    // String Utils
    public String getCommand(String text) { 
        int spaceIndex = text.indexOf(' '); 
        if (spaceIndex == -1) { 
            return text; 
        } else { 
            return text.substring(0, spaceIndex); 
        } 
    }
    public String getArgument(String text) { 
        int spaceIndex = text.indexOf(' '); 
        if (spaceIndex == -1) { 
            return ""; 
        } else { 
            return text.substring(spaceIndex + 1).trim(); 
        } 
    }
    
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
    
    public String env(String text, Hashtable<String, Object> scope) { 
        if (scope != null) { 
            text = replace(text, "$PATH", (String) scope.get("PWD")); 
            for (Enumeration<String> keys = scope.keys(); keys.hasMoreElements();) { 
                String key = keys.nextElement(); 
                text = replace(text, "$" + key, (String) scope.get(key)); 
            } 
        } 
        return env(text); 
    }
    
    public String env(String text) { 
        text = replace(text, "$USER", username); 
        for (Enumeration<String> keys = attributes.keys(); keys.hasMoreElements();) { 
            String key = keys.nextElement(); 
            text = replace(text, "$" + key, (String) attributes.get(key)); 
        } 
        text = replace(text, "$.", "$"); 
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
        return message == null || message.length() == 0 || message.equals("null") ? 
            e.getClass().getName() : e.getClass().getName() + ": " + message; 
    }
    
    public String getcontent(String file, Hashtable<String, Object> scope) { 
        return file.startsWith("/") ? read(file, scope) : read(((String) scope.get("PWD")) + file, scope); 
    }
    
    public String getpattern(String text) { 
        return text.trim().startsWith("\"") && text.trim().endsWith("\"") ? 
            text.substring(1, text.length() - 1) : text.trim(); 
    }
    
    public String[] split(String content, char div) { 
        Vector<String> lines = new Vector<>(); 
        int start = 0; 
        for (int i = 0; i < content.length(); i++) { 
            if (content.charAt(i) == div) { 
                lines.addElement(content.substring(start, i)); 
                start = i + 1; 
            } 
        } 
        if (start < content.length()) { 
            lines.addElement(content.substring(start)); 
        } 
        String[] result = new String[lines.size()]; 
        lines.copyInto(result); 
        return result; 
    }
    
    public String[] splitArgs(String input) {
        Vector<String> result = new Vector<>();
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
                if (inDoubleQuotes || inSingleQuotes) { 
                    escaped = true; 
                } else { 
                    current.append(c); 
                }
                continue;
            }
            
            if (c == '"' && !inSingleQuotes) { 
                inDoubleQuotes = !inDoubleQuotes; 
                current.append(c); 
                continue; 
            }
            if (c == '\'' && !inDoubleQuotes) { 
                inSingleQuotes = !inSingleQuotes; 
                current.append(c); 
                continue; 
            }
            if (c == ' ' && !inDoubleQuotes && !inSingleQuotes) { 
                if (current.length() > 0) { 
                    result.addElement(current.toString()); 
                    current.setLength(0); 
                } 
                continue; 
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) { 
            result.addElement(current.toString()); 
        }
        
        String[] array = new String[result.size()];
        for (int i = 0; i < result.size(); i++) { 
            array[i] = getpattern(result.elementAt(i)); 
        }
        
        return array;
    }
    
    // Generators
    public String genpid() { 
        return String.valueOf(1000 + random.nextInt(9000)); 
    }
    
    // User Manager
    public int getUserID(String user) { 
        return user.equals("root") ? 0 : 
               user.equals(username) ? 1000 : 
               userID.containsKey(user) ? ((Integer) userID.get(user)).intValue() : -1; 
    }
    
    public String getUser(int uid) {
        if (uid == 0) { 
            return "root"; 
        } else if (uid == 1000) { 
            return username; 
        }
        for (Enumeration<String> keys = sys.keys(); keys.hasMoreElements();) {
            String user = keys.nextElement();
            Integer id = (Integer) userID.get(user);
            if (id.intValue() == uid) { 
                return user; 
            }
        }
        return null;
    }
    
    // Trackers
    public String getpid(String name) { 
        for (Enumeration<String> KEYS = sys.keys(); KEYS.hasMoreElements();) { 
            String PID = KEYS.nextElement(); 
            if (name.equals((String) ((Hashtable) sys.get(PID)).get("name"))) { 
                return PID; 
            } 
        } 
        return null; 
    }
    
    // Window-Based Interfaces
    public int warn(String title, String message) { 
        if (message == null || message.length() == 0) { 
            return 2; 
        } 
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE);
        return 0; 
    }
    
    public void print(String message, Object stdout) { 
        print(message, stdout, 1000, globals); 
    }
    
    public void print(String message, Object stdout, int id, Hashtable<String, Object> scope) { 
        if (stdout == null) { 
            System.out.println(message);
            return;
        }
        else if (stdout instanceof JTextArea) { 
            JTextArea textArea = (JTextArea) stdout;
            String current = textArea.getText();
            // Adicionar a mensagem no final
            textArea.append(message + "\n");
            // Rolar para o final
            textArea.setCaretPosition(textArea.getDocument().getLength());
        }
        else if (stdout instanceof StringBuffer) { 
            ((StringBuffer) stdout).append("\n").append(message); 
        }
        else if (stdout instanceof String) { 
            write((String) stdout, read((String) stdout, scope) + "\n" + message, 1000, scope); 
        }
        else if (stdout instanceof OutputStream) { 
            try { 
                ((OutputStream) stdout).write((message + "\n").getBytes(StandardCharsets.UTF_8)); 
                ((OutputStream) stdout).flush(); 
            } catch (Exception e) { } 
        }
        else {
            System.out.println(message);
        }
    }
    
    // File System Helpers
    public String getRealPath(String virtualPath) {
        // Remove a barra inicial para obter o caminho relativo
        String relativePath = virtualPath.startsWith("/") ? virtualPath.substring(1) : virtualPath;
        
        // Se estiver vazio, retorna o rootDir
        if (relativePath.isEmpty()) {
            return rootDir;
        }
        
        // Para /mnt/ - acesso ao sistema de arquivos real
        if (virtualPath.startsWith("/mnt/")) {
            return virtualPath.substring(5);
        }
        
        // Para /tmp/ - usa o diretório temporário do sistema
        if (virtualPath.startsWith("/tmp/")) {
            return System.getProperty("java.io.tmpdir") + File.separator + relativePath.substring(4);
        }
        
        // Todos os outros caminhos (/, /bin/, /etc/, /home/, /lib/, /proc/, /dev/)
        // são relativos ao rootDir
        return rootDir + File.separator + relativePath;
    }
    
    public String getVirtualPath(String realPath) {
        if (realPath.startsWith(rootDir)) {
            return "/" + realPath.substring(rootDir.length() + 1);
        } else if (realPath.startsWith(System.getProperty("java.io.tmpdir"))) {
            return "/tmp/" + realPath.substring(System.getProperty("java.io.tmpdir").length() + 1);
        } else {
            return "/mnt/" + realPath;
        }
    }
    
    // File System - Read
    public InputStream getInputStream(String filename, Hashtable<String, Object> scope) throws Exception {
        filename = solvepath(filename, scope);
        
        // Special files
        if (filename.startsWith("/dev/")) {
            String dev = filename.substring(5);
            String content = dev.equals("random") ? String.valueOf(random.nextInt(256)) : 
                           dev.equals("stdin") ? (stdin != null ? stdin.getText() : "") : 
                           dev.equals("stdout") ? (stdout != null ? stdout.getText() : "") : 
                           dev.equals("null") ? "" : 
                           dev.equals("zero") ? "\0" : null;
            if (content != null) {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }
        
        if (filename.startsWith("/proc/")) {
            String proc = filename.substring(6);
            String content = proc.equals("uptime") ? "" + ((System.currentTimeMillis() - uptime) / 1000) : null;
            if (content != null) {
                return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            }
            return null;
        }
        
        // Temporary files
        if (filename.startsWith("/tmp/")) {
            String key = filename.substring(5);
            if (tmp.containsKey(key)) {
                return new ByteArrayInputStream((byte[]) tmp.get(key));
            }
            return null;
        }
        
        // Real filesystem
        String realPath = getRealPath(filename);
        File file = new File(realPath);
        if (file.exists() && file.isFile()) {
            return new FileInputStream(file);
        }
        
        return null;
    }
    
    public Object readImg(String filename, Hashtable<String, Object> scope) { 
        try { 
            InputStream is = getInputStream(filename, scope); 
            if (is == null) return null;
            byte[] buffer = new byte[is.available()];
            is.read(buffer);
            is.close();
            javax.swing.ImageIcon icon = new javax.swing.ImageIcon(buffer);
            return icon;
        } catch (Exception e) { 
            return null; 
        } 
    }
    
    public String read(String filename, Hashtable<String, Object> scope) {
        try {
            System.out.println("Reading file: " + filename);
            InputStream is = getInputStream(filename, scope);
            if (is == null) { 
                System.out.println("File not found: " + filename);
                return ""; 
            }
            
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            StringBuffer sb = new StringBuffer();
            int ch;
            while ((ch = reader.read()) != -1) { 
                sb.append((char) ch); 
            }
            reader.close();
            is.close();
            
            String content = sb.toString();
            System.out.println("File content length: " + content.length());
            return filename.startsWith("/home/") ? content : env(content);
        } catch (Exception e) { 
            System.out.println("Error reading file: " + filename);
            e.printStackTrace();
            return ""; 
        }
    }
    
    public String read(InputStream in, int chunkSize, boolean consume) {
        try {
            if (in == null) { 
                return ""; 
            }
            if (consume) {
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
                StringBuffer sb = new StringBuffer();
                int ch;
                while ((ch = reader.read()) != -1) { 
                    sb.append((char) ch); 
                }
                reader.close();
                return sb.toString();
            } else {
                byte[] buffer = new byte[chunkSize];
                int bytesRead = in.read(buffer, 0, chunkSize);
                if (bytesRead == -1) { 
                    return null; 
                }
                return new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            }
        } catch (Exception e) { 
            return ""; 
        }
    }
    
    // File System - Write
    public int write(String filename, String data, int id, Hashtable<String, Object> scope) { 
        return write(filename, data.getBytes(StandardCharsets.UTF_8), id, scope); 
    }
    
    public int write(String filename, byte[] data, int id, Hashtable<String, Object> scope) {
        filename = solvepath(filename, scope);
        if (filename == null || filename.length() == 0) { 
            return 2; 
        }
        
        // Check permissions - only root can write to /bin/, /etc/, /lib/
        if ((filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) && id != 0) {
            return 13; // Permission denied
        }
        
        // Special files
        if (filename.startsWith("/dev/")) {
            String dev = filename.substring(5);
            if (dev.equals("null")) {
                return 0;
            } else if (dev.equals("stdin") && stdin != null) {
                stdin.setText(new String(data));
                return 0;
            } else if (dev.equals("stdout") && stdout != null) {
                stdout.setText(new String(data));
                return 0;
            }
            return 5; // Not supported
        }
        
        // Temporary files
        if (filename.startsWith("/tmp/")) {
            String key = filename.substring(5);
            if (key.equals("")) return 2;
            tmp.put(key, data);
            return 0;
        }
        
        // Real filesystem
        try {
            String realPath = getRealPath(filename);
            File file = new File(realPath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            
            FileOutputStream out = new FileOutputStream(file);
            out.write(data);
            out.flush();
            out.close();
            return 0;
        } catch (Exception e) {
            return 1;
        }
    }
    
    public int deleteFile(String filename, int id, Hashtable<String, Object> scope) { 
        filename = solvepath(filename, scope);
        if (filename == null || filename.length() == 0) { 
            return 2; 
        }
        
        // Check permissions - only root can delete from /bin/, /etc/, /lib/
        if ((filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) && id != 0) {
            return 13; // Permission denied
        }
        
        // Temporary files
        if (filename.startsWith("/tmp/")) {
            String key = filename.substring(5);
            if (key.equals("")) return 2;
            if (tmp.containsKey(key)) {
                tmp.remove(key);
                return 0;
            }
            return 127; // Not found
        }
        
        // Real filesystem
        try {
            String realPath = getRealPath(filename);
            File file = new File(realPath);
            if (file.exists()) {
                if (file.delete()) {
                    return 0;
                }
                return 1;
            }
            return 127; // Not found
        } catch (Exception e) {
            return 1;
        }
    }
    
    // Normalize Path
    public String joinpath(String file, Hashtable<String, Object> scope) {
        String pwd = scope.containsKey("PWD") ? (String) scope.get("PWD") : "/";
        if (file.startsWith("/")) { 
            return file; 
        }
        String fullPath = pwd + file;
        Vector<String> components = new Vector<>();
        String[] parts = split(fullPath, '/');
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.equals(".")) {
                continue;
            } else if (part.equals("..")) {
                if (components.size() > 0) {
                    // Don't allow going above root
                    if (!components.lastElement().equals("")) {
                        components.removeElementAt(components.size() - 1);
                    }
                }
            } else {
                components.addElement(part);
            }
        }
        if (components.size() == 0) { 
            return "/"; 
        }
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < components.size(); i++) {
            String comp = components.elementAt(i);
            if (i == 0 && comp.equals("")) { 
                result.append("/"); 
            } 
            else if (i > 0 || !comp.equals("")) {
                result.append(comp);
                if (i < components.size() - 1) { 
                    result.append("/"); 
                }
            }
        }
        if (fullPath.endsWith("/") && !result.toString().endsWith("/")) {
            result.append("/");
        }
        return result.toString();
    }
    
    public String solvepath(String path, Hashtable<String, Object> scope) { 
        String root = scope.containsKey("ROOT") ? (String) scope.get("ROOT") : "";
        if (path == null) { 
            return "/"; 
        }
        
        // Handle virtual paths that should not be translated
        if (path.startsWith("/dev/") || path.startsWith("/proc/") || path.startsWith("/tmp/")) {
            return path;
        }
        
        // /mnt/ goes to real filesystem
        if (path.startsWith("/mnt/")) {
            return path;
        }
        
        // Normal path resolution
        if (path.startsWith("/")) {
            if (!root.equals("/") && root.length() > 0) {
                return root.endsWith("/") ? root + path.substring(1) : root + path;
            }
            return path;
        }
        
        return path;
    }
    
    // Base64 (simplified for compatibility)
    public String encodeBase64(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }
    
    public byte[] decodeBase64(String data) {
        try {
            return Base64.getDecoder().decode(data);
        } catch (Exception e) {
            return null;
        }
    }
    
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
    
    // Java Virtual Machine
    public int javaClass(String name) { 
        try { 
            Class.forName(name); 
            return 0; 
        } catch (ClassNotFoundException e) { 
            return 3; 
        } 
    }
    
    public String getName() { 
        String s; 
        StringBuffer BUFFER = new StringBuffer(); 
        if ((s = System.getProperty("java.vm.name")) != null) { 
            BUFFER.append(s).append(", ").append(System.getProperty("java.vm.vendor")); 
            if ((s = System.getProperty("java.vm.version")) != null) { 
                BUFFER.append('\n').append(s); 
            } 
            if ((s = System.getProperty("java.vm.specification.name")) != null) { 
                BUFFER.append('\n').append(s); 
            } 
        } else if ((s = System.getProperty("com.ibm.oti.configuration")) != null) { 
            BUFFER.append("J9 VM, IBM (").append(s).append(')'); 
            if ((s = System.getProperty("java.fullversion")) != null) { 
                BUFFER.append("\n\n").append(s); 
            } 
        } else if ((s = System.getProperty("com.oracle.jwc.version")) != null) { 
            BUFFER.append("OJWC v").append(s).append(", Oracle"); 
        } else if (javaClass("com.sun.cldchi.jvm.JVM") == 0) { 
            BUFFER.append("CLDC Hotspot Implementation, Sun"); 
        } else if (javaClass("com.sun.midp.Main") == 0) { 
            BUFFER.append("KVM, Sun (MIDP)"); 
        } else if (javaClass("com.sun.cldc.io.ConsoleOutputStream") == 0) { 
            BUFFER.append("KVM, Sun (CLDC)"); 
        } else if (javaClass("com.jblend.util.SortedVector") == 0) { 
            BUFFER.append("JBlend, Aplix"); 
        } else if (javaClass("com.jbed.io.CharConvUTF8") == 0) { 
            BUFFER.append("Jbed, Esmertec/Myriad Group"); 
        } else if (javaClass("MahoTrans.IJavaObject") == 0) { 
            BUFFER.append("MahoTrans"); 
        } else { 
            BUFFER.append("Unknown"); 
        } 
        return BUFFER.append('\n').toString(); 
    }
}

// Process class
class Process {
    private OpenTTY midlet = null;
    public String name, owner, pid, cmd;
    public Hashtable<String, Object> scope, db = new Hashtable<>(), net = new Hashtable<>();
    public final long startTime;
    public int uid = 1000, priority = DEFAULT_PRIORITY;

    public static final int MIN_PRIORITY = 0, DEFAULT_PRIORITY = 10, MAX_PRIORITY = 20;
  
    public Object stdout, stderr;
    public Object handler = null, sighandler = null;
    public Lua lua = null;
    public ELF elf = null;

    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable<String, Object> scope) { 
        this.lua = new Lua(midlet, uid, pid, this, stdout, scope); 
        this.name = name; 
        this.owner = owner; 
        this.uid = uid; 
        this.pid = pid; 
        this.stdout = stdout; 
        this.stderr = stdout; 
        this.scope = scope; 
        this.startTime = System.currentTimeMillis(); 
    }
    
    public Process(OpenTTY midlet, String name, String command, String owner, int uid, String pid, Object stdout, Hashtable<Object, Object> args, Hashtable<String, Object> scope) { 
        this.elf = new ELF(midlet, args, stdout, scope, uid, pid, this); 
        this.name = name; 
        this.owner = owner; 
        this.uid = uid; 
        this.pid = pid; 
        this.stdout = stdout; 
        this.stderr = stdout; 
        this.scope = scope; 
        this.startTime = System.currentTimeMillis(); 
    }

    public String toString() { 
        return "{ name=" + name + ", owner=" + owner + ", uid=" + uid + ", pid=" + pid + ", " + 
               (lua != null ? "lua=" + lua + ", " : elf != null ? "elf=" + elf + ", " : "") + 
               (handler != null ? "handler=" + handler + ", " : "") + 
               "priority=" + priority + ", scope=" + scope + ", db=" + db + " }"; 
    }
}

// Token class
class Token {
    public int type;
    public String value;
    
    public Token(int type, String value) {
        this.type = type;
        this.value = value;
    }
}

// ELF class stub
class ELF {
    public ELF(OpenTTY midlet, Hashtable<Object, Object> args, Object stdout, Hashtable<String, Object> scope, int uid, String pid, Process process) {
    }
}