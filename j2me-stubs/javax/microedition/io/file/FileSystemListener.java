package javax.microedition.io.file;
public interface FileSystemListener {
    int STATE_ROOTS_ADDED = 0;
    int STATE_ROOTS_REMOVED = 1;
    int STATE_ROOT_CHANGED = 2;
    void filesystemChanged(int state, String path);
}
