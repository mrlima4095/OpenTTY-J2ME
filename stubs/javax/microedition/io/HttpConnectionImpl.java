package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;

/** HttpConnection backed by java.net.HttpURLConnection. */
final class HttpConnectionImpl implements HttpConnection {
    private final String url;
    private String requestMethod = GET;
    private final Hashtable requestProps = new Hashtable();
    private HttpURLConnection con = null;
    private boolean closed = false;

    HttpConnectionImpl(String url) {
        this.url = url;
    }

    private HttpURLConnection connect() throws IOException {
        if (con == null) {
            con = (HttpURLConnection) new URL(url).openConnection();
            con.setConnectTimeout(15000);
            con.setReadTimeout(120000);
            con.setInstanceFollowRedirects(true);
        }
        if (!requestMethod.equals(con.getRequestMethod())) {
            con.setRequestMethod(requestMethod);
        }
        return con;
    }

    public void open() throws IOException {
        connect();
    }

    public void close() throws IOException {
        if (closed) { return; }
        closed = true;
        if (con != null) { con.disconnect(); }
    }

    public String getURL() { return url; }
    public String getQuery() { return ""; }
    public String getRequestMethod() { return requestMethod; }

    public void setRequestMethod(String method) throws IOException {
        if (con != null && con.getResponseCode() != -1) { throw new IOException("Already connected"); }
        this.requestMethod = method == null ? GET : method.toUpperCase();
    }

    public String getRequestProperty(String key) {
        return (String) requestProps.get(key);
    }

    public void setRequestProperty(String key, String value) throws IOException {
        if (con != null && con.getResponseCode() != -1) { throw new IOException("Already connected"); }
        requestProps.put(key, value);
    }

    public int getResponseCode() throws IOException {
        HttpURLConnection c = connect();
        for (java.util.Enumeration e = requestProps.keys(); e.hasMoreElements();) {
            String k = (String) e.nextElement();
            c.setRequestProperty(k, (String) requestProps.get(k));
        }
        c.connect();
        return c.getResponseCode();
    }

    public String getResponseMessage() throws IOException {
        HttpURLConnection c = connect(); c.connect();
        return c.getResponseMessage();
    }

    public long getExpiration() throws IOException { return -1; }
    public long getDate() throws IOException { return -1; }
    public long getLastModified() throws IOException { return -1; }

    public String getHeaderField(String name) throws IOException {
        HttpURLConnection c = connect(); c.connect();
        return c.getHeaderField(name);
    }

    public String getHeaderField(int index) throws IOException {
        HttpURLConnection c = connect(); c.connect();
        return c.getHeaderField(index);
    }

    public String getHeaderFieldKey(int index) throws IOException {
        HttpURLConnection c = connect(); c.connect();
        return c.getHeaderFieldKey(index);
    }

    public long getHeaderFieldDate(String name, long def) throws IOException {
        HttpURLConnection c = connect(); c.connect();
        long d = c.getHeaderFieldDate(name, def);
        return d;
    }

    public int getHeaderFieldInt(String name, int def) throws IOException {
        HttpURLConnection c = connect(); c.connect();
        return c.getHeaderFieldInt(name, def);
    }

    public String getEncoding() { return null; }
    public String getContentType() { return null; }
    public long getLength() { return -1; }

    public InputStream openInputStream() throws IOException {
        HttpURLConnection c = connect();
        // force connect so request method / properties are applied before streams
        c.setRequestMethod(requestMethod);
        for (java.util.Enumeration e = requestProps.keys(); e.hasMoreElements();) {
            String k = (String) e.nextElement();
            c.setRequestProperty(k, (String) requestProps.get(k));
        }
        c.connect();
        java.io.InputStream error = c.getErrorStream();
        return error != null ? error : c.getInputStream();
    }

    public OutputStream openOutputStream() throws IOException {
        HttpURLConnection c = connect();
        c.setRequestMethod(requestMethod);
        c.setDoOutput(true);
        for (java.util.Enumeration e = requestProps.keys(); e.hasMoreElements();) {
            String k = (String) e.nextElement();
            c.setRequestProperty(k, (String) requestProps.get(k));
        }
        return c.getOutputStream();
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }
}