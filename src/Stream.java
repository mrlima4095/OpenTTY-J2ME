import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
public class Stream {
    private int __index = 1, id = 1000;
    private InputStream in;
    private OutputStream out;
    private SocketConnection socket;

    private String filename, source;
    private boolean closed = false;

    public Stream(String filename) {
        if (filename == null) { }
    }
    public Stream(StringItem item) {

    }
    public Stream(SocketConnection socket) {

    }
    public Stream(SocketConnection socket, InputStream in, OutputStream out) {

    }
    public Stream(String filename, InputStream in, OutputStream out) {

    }
    

    public Object read() {
        if (closed) { return null; }
    }
    public Object read(int buffer) {
        if (closed) { return null; }
    }
    
    public Image readImg() {
        if (closed) { return null; }

    }

    public void write(byte[] data) {

    }
    public void write(String data) {

    }

    public void setIndex(int page) { this.__index = page; }
    public void setPermission(int id) { this.id = id; }

    public void close() { closed = false; }
}

public InputStream readRaw(String filename) throws Exception {
        
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
    public String read(String filename) {
        try {
            if (filename.startsWith("/tmp/")) { return tmp.containsKey(filename = filename.substring(5)) ? (String) tmp.get(filename) : ""; }
            InputStream is = readRaw(filename);
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
    public Image readImg(String filename) { try { InputStream is = readRaw(filename); Image img = Image.createImage(is); is.close(); return img; } catch (Exception e) { return Image.createImage(16, 16); } }
    