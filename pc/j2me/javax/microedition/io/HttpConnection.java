package javax.microedition.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface HttpConnection extends Connection {
    String HEAD = "HEAD";
    String GET = "GET";
    String POST = "POST";
    int HTTP_OK = 200, HTTP_CREATED = 201, HTTP_ACCEPTED = 202, HTTP_NOT_AUTHORITATIVE = 203,
        HTTP_NO_CONTENT = 204, HTTP_RESET = 205, HTTP_PARTIAL = 206;
    int HTTP_MOVED_PERM = 301, HTTP_MOVED_TEMP = 302, HTTP_SEE_OTHER = 303,
        HTTP_NOT_MODIFIED = 304, HTTP_TEMP_REDIRECT = 307;
    int HTTP_BAD_REQUEST = 400, HTTP_UNAUTHORIZED = 401, HTTP_PAYMENT_REQUIRED = 402,
        HTTP_FORBIDDEN = 403, HTTP_NOT_FOUND = 404, HTTP_NOT_ACCEPTABLE = 406,
        HTTP_CONFLICT = 409, HTTP_GONE = 410, HTTP_LENGTH_REQUIRED = 411,
        HTTP_PRECONDITION_FAILED = 412, HTTP_REQUEST_ENTITY_TOO_LARGE = 413,
        HTTP_REQUEST_URI_TOO_LONG = 414, HTTP_UNSUPPORTED_TYPE = 415,
        HTTP_UNSUPPORTED_RANGE = 416, HTTP_UNAVAILABLE = 503;

    void setRequestMethod(String method) throws IOException;
    void setRequestProperty(String key, String value) throws IOException;
    int getResponseCode() throws IOException;
    String getResponseMessage() throws IOException;
    long getExpiration() throws IOException;
    long getDate() throws IOException;
    long getLastModified() throws IOException;
    String getHeaderField(String name) throws IOException;
    int getHeaderFieldInt(String name, int fallback) throws IOException;
    long getHeaderFieldDate(String name, long fallback) throws IOException;
    String getHeaderField(int index) throws IOException;
    String getHeaderFieldKey(int index) throws IOException;
    String getURL();
    String getProtocol();
    String getHost();
    String getFile();
    String getRef();
    String getQuery();
    int getPort();
    String getRequestMethod();
    String getRequestProperty(String key);

    java.io.InputStream openInputStream() throws java.io.IOException;
    java.io.OutputStream openOutputStream() throws java.io.IOException;
}
