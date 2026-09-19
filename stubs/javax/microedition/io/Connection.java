package javax.microedition.io;

import java.io.IOException;

/** Generic connection from Generic Connection Framework. */
public interface Connection {
    void open() throws IOException;
    void close() throws IOException;
}