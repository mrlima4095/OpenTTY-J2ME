package javax.microedition.io;
import java.io.InputStream;
import java.io.OutputStream;
public interface StreamConnection extends Connection {
    InputStream openInputStream() throws java.io.IOException;
    OutputStream openOutputStream() throws java.io.IOException;
}
