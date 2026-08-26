package javax.microedition.io;
public interface InputConnection extends Connection {
    java.io.DataInputStream openDataInputStream() throws java.io.IOException;
    java.io.InputStream openInputStream() throws java.io.IOException;
}
