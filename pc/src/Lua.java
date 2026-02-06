import javax.swing.*;
import java.util.*;
import java.io.*;
import java.nio.file.*;
import java.net.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

public class OpenTTY {
    // Behavior Settings
    public long uptime = System.currentTimeMillis();
    public boolean useCache = true, debug = false;
    
    // System Objects
    public int lastID = 1000;
    public Random random = new Random();
    public Runtime runtime = Runtime.getRuntime();
    public Object shell;
    
    // Data structures
    public Hashtable<String, String> attributes = new Hashtable<>();
    public Hashtable<String, Vector<String>> fs = new Hashtable<>();
    public Hashtable<String, Process> sys = new Hashtable<>();
    public Hashtable<String, byte[]> tmp = new Hashtable<>();
    public Hashtable<String, byte[]> cache = new Hashtable<>();
    public Hashtable<String, Vector<Token>> cacheLua = new Hashtable<>();
    public Hashtable<String, Object> graphics = new Hashtable<>();
    public Hashtable<String, ServerSocket> servers = new Hashtable<>();
    public Hashtable<String, String> globals = new Hashtable<>();
    public Hashtable<String, Integer> userID = new Hashtable<>();
    
    public String username = "user";
    public String build = "2026-1.19-SEx10";
    
    // File system root
    public Path rootPath = Paths.get(System.getProperty("user.dir"));
    
    // Main method
    public static void main(String[] args) { OpenTTY opentty = new OpenTTY(); opentty.init(); }
    public void init() {
        try {
            // Initialize globals
            globals.put("PWD", "/home/");
            globals.put("USER", "root");
            globals.put("ROOT", "/");
            
            // Create necessary directories
            createDirectories();
            
            // Check if first run
            if (!Files.exists(rootPath.resolve("home/user"))) {
                showLogin(true, true);
            } else {
                startSystem();
            }
        } catch (Exception e) {
            panic(e);
        }
    }
    
    private void createDirectories() throws IOException {
        String[] dirs = {"bin", "etc", "lib", "home", "tmp", "proc", "dev", "mnt"};
        for (String dir : dirs) {
            Files.createDirectories(rootPath.resolve(dir));
        }
    }
    
    private void showLogin(boolean needUser, boolean needPassword) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel title = new JLabel("OpenTTY - Login");
        title.setFont(title.getFont().deriveFont(16f));
        panel.add(title);
        panel.add(Box.createVerticalStrut(10));
        
        if (needUser) {
            panel.add(new JLabel("Username:"));
            JTextField userField = new JTextField(20);
            panel.add(userField);
            panel.add(Box.createVerticalStrut(5));
        }
        
        if (needPassword) {
            panel.add(new JLabel("Password:"));
            JPasswordField passField = new JPasswordField(20);
            panel.add(passField);
        }
        
        int result = JOptionPane.showConfirmDialog(null, panel, "Login", 
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        
        if (result == JOptionPane.OK_OPTION) {
            // For now, just accept any credentials
            if (needUser) {
                username = "user";
                try {
                    Files.writeString(rootPath.resolve("home/user"), username);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            startSystem();
        } else {
            System.exit(0);
        }
    }
    
    private void startSystem() {
        try {
            Hashtable<String, Object> args = new Hashtable<>();
            args.put("0", "/bin/init");
            
            Process proc = new Process(this, "init", "/bin/init", "root", 0, "1", 
                System.out, globals);
            
            sys.put("1", proc);
            proc.lua.globals.put("arg", args);
            proc.handler = proc.lua.getKernel();
            
            String initScript = read("/bin/init", globals);
            if (initScript.isEmpty()) {
                // Create default init script
                initScript = "print('OpenTTY SE v" + build + "\\n')\n" +
                           "print('Type \\'help\\' for commands')\n" +
                           "shell = function(cmd) return os.execute(cmd) end";
                write("/bin/init", initScript, 0, globals);
                initScript = read("/bin/init", globals);
            }
            
            proc.lua.tokens = proc.lua.tokenize(initScript);
            
            // Run init script
            while (proc.lua.peek().type != 0) {
                Object res = proc.lua.statement(proc.lua.globals);
                if (proc.lua.doreturn) break;
            }
            
        } catch (Exception e) {
            panic(e);
        }
    }
    
    private void panic(Throwable e) {
        String message = "An error occurred while OpenTTY tried to start!\n\n" +
                        "Error: " + getCatch(e) + "\n\n" +
                        (e instanceof Exception ? 
                         "If you tried to install a program in /bin/init it can be the error" :
                         "Try to clear your data or update OpenTTY");
        
        String[] options = {"Exit", "Recovery"};
        int choice = JOptionPane.showOptionDialog(null, message, "Error",
            JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null,
            options, options[0]);
        
        if (choice == 1) {
            recovery();
        } else {
            System.exit(1);
        }
    }
    
    private void recovery() {
        String[] options = {
            "Retry boot", "Update", "Clear data", 
            "Reset config", "Factory reset", "System Info", "Exit"
        };
        
        int choice = JOptionPane.showOptionDialog(null, 
            "Recovery Options", "Recovery",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null,
            options, options[0]);
        
        switch (choice) {
            case 0: // Retry boot
                startSystem();
                break;
            case 1: // Update
                try {
                    Desktop.getDesktop().browse(new URI("http://opentty.xyz/dist/"));
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Cannot open browser");
                }
                break;
            case 2: // Clear data
                try {
                    deleteFile("/bin/init", 0, globals);
                    Files.write(rootPath.resolve("home/user"), "".getBytes());
                    JOptionPane.showMessageDialog(null, "User data cleared. Restart OpenTTY.");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
                }
                break;
            case 3: // Reset config
                String[] files = {"fstab", "hostname", "motd", "os-release", "services"};
                for (String file : files) {
                    deleteFile("/etc/" + file, 0, globals);
                }
                JOptionPane.showMessageDialog(null, "Configuration reset to defaults.");
                break;
            case 4: // Factory reset
                try {
                    deleteFile("/bin/init", 0, globals);
                    Files.write(rootPath.resolve("home/user"), "".getBytes());
                    JOptionPane.showMessageDialog(null, "All data cleared. Restart OpenTTY.");
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
                }
                break;
            case 5: // System Info
                String info = "OpenTTY " + build + "\n" +
                            "Uptime: " + ((System.currentTimeMillis() - uptime) / 1000) + "s\n" +
                            "User: " + username + "\n" +
                            "Memory: " + (runtime.freeMemory() / 1024) + " KB free\n" +
                            "Processes: " + sys.size() + "\n" +
                            "\nJava: " + System.getProperty("java.version");
                JOptionPane.showMessageDialog(null, info, "System Information", 
                    JOptionPane.INFORMATION_MESSAGE);
                break;
            default:
                System.exit(0);
        }
    }
    
    // String utilities
    public String getCatch(Throwable e) { String message = e.getMessage(); return message == null || message.length() == 0 || message.equals("null") ? e.getClass().getName() : e.getClass().getName() + ": " + message; }    
    public String escape(String text) { return text.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t").replace("\\b", "\b").replace("\\\\", "\\"); }
    
    // File system operations
    public InputStream getInputStream(String filename, Hashtable<String, String> scope) throws Exception {
        Path resolved = resolvePath(filename, scope);
        
        if (Files.exists(resolved)) {
            return Files.newInputStream(resolved);
        }
        
        // Special files
        if (filename.startsWith("/dev/")) {
            String dev = filename.substring(5);
            switch (dev) {
                case "random":
                    return new ByteArrayInputStream(String.valueOf(random.nextInt(256)).getBytes());
                case "stdin":
                    return System.in;
                case "stdout":
                    return new ByteArrayInputStream("".getBytes());
                case "null":
                    return new ByteArrayInputStream(new byte[0]);
                case "zero":
                    return new ByteArrayInputStream(new byte[] {0});
                default:
                    return null;
            }
        }

        InputStream is = getClass().getResourceAsStream(filename);
        return is;
    }
    
    public String read(String filename, Hashtable<String, String> scope) {
        try {
            Path resolved = resolvePath(filename, scope);
            if (Files.exists(resolved)) {
                return new String(Files.readAllBytes(resolved));
            }
            return "";
        } catch (Exception e) {
            return "";
        }
    }
    
    public int write(String filename, String data, int id, Hashtable<String, String> scope) { return write(filename, data.getBytes(), id, scope); }
    public int write(String filename, byte[] data, int id, Hashtable<String, String> scope) {
        try {
            Path resolved = resolvePath(filename, scope);
            
            // Check permissions for system directories
            if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
                if (id != 0) return 13; // Permission denied
            }
            
            Files.createDirectories(resolved.getParent());
            Files.write(resolved, data);
            return 0;
        } catch (Exception e) {
            return 1;
        }
    }
    public int deleteFile(String filename, int id, Hashtable<String, String> scope) {
        try {
            Path resolved = resolvePath(filename, scope);
            
            // Check permissions for system directories
            if (filename.startsWith("/bin/") || filename.startsWith("/etc/") || filename.startsWith("/lib/")) {
                if (id != 0) return 13; // Permission denied
            }
            
            if (Files.exists(resolved)) {
                Files.delete(resolved);
                return 0;
            }
            return 127; // File not found
        } catch (Exception e) {
            return 1;
        }
    }
    
    private Path resolvePath(String path, Hashtable<String, String> scope) { String pwd = scope.getOrDefault("PWD", "/home/"); return path.startsWith("/") ? rootPath.resolve(path.substring(1)) : rootPath.resolve(pwd.substring(1)).resolve(path); }
    
    // Base64 encoding/decoding
    public String encodeBase64(byte[] data) { return Base64.getEncoder().encodeToString(data); }
    public byte[] decodeBase64(String data) { return Base64.getDecoder().decode(data); }
    
    // Other utility methods
    public String getCommand(String text) { int spaceIndex = text.indexOf(' '); return spaceIndex == -1 ? text : text.substring(0, spaceIndex); }
    public String getArgument(String text) { int spaceIndex = text.indexOf(' '); return spaceIndex == -1 ? "" : text.substring(spaceIndex + 1).trim(); }
    
    public String[] split(String content, char div) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == div) {
                parts.add(content.substring(start, i));
                start = i + 1;
            }
        }
        if (start < content.length()) {
            parts.add(content.substring(start));
        }
        return parts.toArray(new String[0]);
    }
    public String[] splitArgs(String content) {
        List<String> args = new ArrayList<>();
        boolean inQuotes = false;
        int start = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes && c == ' ') {
                if (i > start) {
                    args.add(content.substring(start, i).replace("\"", ""));
                }
                start = i + 1;
            }
        }
        if (start < content.length()) {
            args.add(content.substring(start).replace("\"", ""));
        }
        return args.toArray(new String[0]);
    }
    
    public String genpid() { return String.valueOf(1000 + random.nextInt(9000)); }
    public boolean platformRequest(String url) { try { Desktop.getDesktop().browse(new URI(url)); return true; } catch (Exception e) { return false; } }
    
    // User management
    public int getUserID(String user) { if (user.equals("root")) { return 0; } if (user.equals(username)) { return 1000; } return userID.getOrDefault(user, -1); }
    public String getUser(int uid) { if (uid == 0) return "root"; if (uid == 1000) return username; for (Map.Entry<String, Integer> entry : userID.entrySet()) { if (entry.getValue() == uid) return entry.getKey(); } return null; }
}

// Process class
class Process {
    public OpenTTY midlet;
    public String name, owner, pid, cmd;
    public Hashtable<String, Object> scope, db = new Hashtable<>(), net = new Hashtable<>();
    public final long startTime;
    public int uid = 1000, priority = 10;
    
    public Object stdout, stderr;
    public Object handler = null, sighandler = null;
    public Lua lua = null;
    
    public Process(OpenTTY midlet, String name, String command, String owner, 
                   int uid, String pid, Object stdout, Hashtable<String, String> scope) {
        this.midlet = midlet;
        this.name = name;
        this.owner = owner;
        this.uid = uid;
        this.pid = pid;
        this.stdout = stdout;
        this.stderr = stdout;
        this.scope = new Hashtable<>();
        this.scope.putAll(scope);
        this.startTime = System.currentTimeMillis();
        this.lua = new Lua(midlet, uid, pid, this, stdout, this.scope);
    }
    
    public String toString() {
        return "{ name=" + name + ", owner=" + owner + ", uid=" + uid + 
               ", pid=" + pid + ", priority=" + priority + " }";
    }
}