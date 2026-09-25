package javax.microedition.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpConnectionImpl implements HttpConnection {
    final HttpURLConnection urlc;

    public HttpConnectionImpl(String url) throws IOException {
        this.urlc = (HttpURLConnection) new URL(url).openConnection();
        urlc.setInstanceFollowRedirects(true);
        urlc.setConnectTimeout(15000);
        urlc.setReadTimeout(30000);
    }

    public String getURL() { return urlc.getURL().toString(); }
    public String getProtocol() { return urlc.getURL().getProtocol(); }
    public String getHost() { return urlc.getURL().getHost(); }
    public String getFile() { return urlc.getURL().getPath(); }
    public String getRef() { return urlc.getURL().getRef(); }
    public String getQuery() { return urlc.getURL().getQuery(); }
    public int getPort() {
        int p = urlc.getURL().getPort();
        return p != -1 ? p : (urlc.getURL().getDefaultPort());
    }
    public String getRequestMethod() { return urlc.getRequestMethod(); }
    public void setRequestMethod(String method) throws IOException { urlc.setRequestMethod(method); }
    public String getRequestProperty(String key) { return urlc.getRequestProperty(key); }
    public void setRequestProperty(String key, String value) { urlc.setRequestProperty(key, value); }

    public int getResponseCode() throws IOException { return urlc.getResponseCode(); }
    public String getResponseMessage() throws IOException { return urlc.getResponseMessage(); }
    public long getExpiration() { return urlc.getExpiration(); }
    public long getDate() { return urlc.getDate(); }
    public long getLastModified() { return urlc.getLastModified(); }
    public String getHeaderField(String name) { return urlc.getHeaderField(name); }
    public int getHeaderFieldInt(String name, int fallback) { return urlc.getHeaderFieldInt(name, fallback); }
    public long getHeaderFieldDate(String name, long fallback) { return urlc.getHeaderFieldDate(name, fallback); }
    public String getHeaderField(int index) { return urlc.getHeaderField(index); }
    public String getHeaderFieldKey(int index) { return urlc.getHeaderFieldKey(index); }

    public InputStream openInputStream() throws IOException {
        return urlc.getResponseCode() >= 400 ? urlc.getErrorStream() : urlc.getInputStream();
    }
    public DataInputStream openDataInputStream() throws IOException { return new DataInputStream(openInputStream()); }
    public OutputStream openOutputStream() throws IOException { return urlc.getOutputStream(); }
    public DataOutputStream openDataOutputStream() throws IOException { return new DataOutputStream(urlc.getOutputStream()); }

    public void close() { urlc.disconnect(); }
}
