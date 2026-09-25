package javax.microedition.io;

import java.io.OutputStream;
import java.io.DataOutputStream;

public interface OutputConnection extends Connection {
    OutputStream openOutputStream() throws java.io.IOException;
    DataOutputStream openDataOutputStream() throws java.io.IOException;
}
