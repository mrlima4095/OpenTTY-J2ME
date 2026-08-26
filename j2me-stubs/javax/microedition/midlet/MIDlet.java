package javax.microedition.midlet;
public abstract class MIDlet {
    protected MIDlet() {}
    protected abstract void startApp();
    protected abstract void pauseApp();
    protected abstract void destroyApp(boolean unconditional);
    public final void notifyDestroyed() {}
    public final String getAppProperty(String key) { return null; }
    public final boolean platformRequest(String URL) { return false; }
}
