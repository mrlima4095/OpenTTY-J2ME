package javax.microedition.io;

import java.io.InputStream;
import java.io.DataInputStream;

public interface InputConnection extends Connection {
    InputStream openInputStream() throws java.io.IOException;
    DataInputStream openDataInputStream() throws java.io.IOException;
}
