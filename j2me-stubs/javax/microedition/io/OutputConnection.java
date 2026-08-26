package javax.microedition.io;
public interface OutputConnection extends Connection {
    java.io.DataOutputStream openDataOutputStream() throws java.io.IOException;
    java.io.OutputStream openOutputStream() throws java.io.IOException;
}
