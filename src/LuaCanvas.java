import javax.microedition.lcdui.*;
import java.util.*;
// |
// LuaCanvas - free-form drawing surface behind the Lua `graphics.canvas` API.
//
// The canvas owns a persistent off-screen Image (the buffer): Lua code draws
// into it and the buffer is blitted to the screen on every system repaint.
// Two modes share the same primitives:
//
//   callback mode  `canvas.paint = function(c, w, h) ... end` runs on the
//                  LCDUI event thread whenever the system asks for a repaint;
//                  key/pointer callbacks run on that thread as well.
//
//   loop mode      `graphics.canvas.loop(c, fn, delay)` keeps the script thread
//                  inside fn(c, dt) and only blits one frame per iteration.
//                  Input events are queued by the LCDUI thread and dispatched
//                  from the loop thread, so a running canvas never re-enters
//                  the Lua interpreter from two threads at once.
//
// The Lua-visible handle is a plain table (see Lua.canvasNew) whose metatable
// keeps the Java object under "__canvas", the methods under "__index" and a
// "__newindex" hook: `canvas.keyPressed = fn` registers a callback and
// `canvas.title = "Game"` / `canvas.font = "monospace bold"` apply the value,
// so the handle never shadows a method with state of the same name.
public class LuaCanvas extends Canvas implements CommandListener {
    public static final int EVENT_PAINT = 1, EVENT_KEY_PRESSED = 2, EVENT_KEY_RELEASED = 3, EVENT_KEY_REPEATED = 4, EVENT_POINTER_PRESSED = 5, EVENT_POINTER_RELEASED = 6, EVENT_POINTER_DRAGGED = 7, EVENT_SHOW = 8, EVENT_HIDE = 9, EVENT_SIZE = 10, EVENT_TICK = 11, EVENT_COMMAND = 12;
    private static final String[] EVENT_NAMES = { "", "paint", "keyPressed", "keyReleased", "keyRepeated", "pointerPressed", "pointerReleased", "pointerDragged", "show", "hide", "size", "tick", "command" };

    private OpenTTY midlet;
    private Lua lua;
    private Hashtable screen;
    // | (Callbacks registered from Lua: paint/keyPressed/tick/...)
    private Hashtable callbacks = new Hashtable();
    // | (Frame buffer)
    private Image buffer;
    private Graphics g;
    private int bufferWidth, bufferHeight;
    // | (Drawing state)
    private int background = 0xFFFFFF, foreground = 0x000000, color = 0x000000;
    private String fontSpec = "medium";
    private Font font = Font.getDefaultFont();
    private Vector savedStates = new Vector();
    // | (Text cursor)
    private int cursorX = 0, cursorY = 0, lastAdvance = 0;
    // | (Input)
    private Hashtable keysDown = new Hashtable();
    private Vector events = new Vector();
    // | (Commands queued while a loop runs; ids are handed to pump())
    private Hashtable pendingCommands = new Hashtable();
    private int commandSeq = 0;
    private int pointerX = 0, pointerY = 0;
    private boolean hold = false;
    // | (Commands)
    private Vector commands = new Vector();
    private Hashtable commandHandlers = new Hashtable();
    // | (Loop)
    private Thread loopThread = null;
    private boolean loopPending = false;
    private boolean fullscreen = false;
    private long startMillis = System.currentTimeMillis();
    // |
    public LuaCanvas(OpenTTY midlet, Lua lua, int background, int foreground, String fontSpec) {
        this.midlet = midlet;
        this.lua = lua;
        this.background = background & 0xFFFFFF;
        this.foreground = foreground & 0xFFFFFF;
        this.color = this.foreground;
        this.fontSpec = fontSpec == null ? "medium" : fontSpec;
        this.font = parseFont(this.fontSpec);
        setCommandListener(this);
        ensureBuffer();
    }
    // |
    // Handle / callback plumbing
    public void setScreen(Hashtable screen) { this.screen = screen; }
    public Hashtable getScreen() { return screen; }
    public void setCallback(String name, Object fn) {
        if (name == null) { return; }
        if (fn == null || fn == Lua.LUA_NIL) { callbacks.remove(name); }
        else { callbacks.put(name, fn); }
    }
    public Object getCallback(String name) { return name == null ? null : callbacks.get(name); }
    private Lua.LuaFunction callback(String name) {
        Object fn = callbacks.get(name);
        return fn instanceof Lua.LuaFunction ? (Lua.LuaFunction) fn : null;
    }
    private boolean hasCallback(String name) { return callback(name) != null; }
    private void fire(String name, Vector args) {
        Lua.LuaFunction fn = callback(name);
        if (fn == null) { return; }
        if (lua != null) { lua.invokeCallback(fn, args); }
        else { try { fn.call(args); } catch (Throwable e) { } }
    }
    // | (Event queue: queued while a loop runs or while a blocking read waits)
    private void post(int type, int a, int b, int c, int d) {
        if (loopThread != null || loopPending || hold) {
            synchronized (events) { events.addElement(new int[] { type, a, b, c, d }); events.notifyAll(); }
            return;
        }
        dispatch(type, a, b, c, d);
    }
    private void dispatch(int type, int a, int b, int c, int d) {
        if (type == EVENT_COMMAND) {
            Command queued;
            synchronized (pendingCommands) { queued = (Command) pendingCommands.remove(new Integer(a)); }
            if (queued != null) { invokeCommand(queued); }
            return;
        }
        Vector args = new Vector();
        args.addElement(screen);
        if (type == EVENT_KEY_PRESSED || type == EVENT_KEY_REPEATED) { args.addElement(new Double(a)); args.addElement(new Double(b)); }
        else if (type == EVENT_KEY_RELEASED) { args.addElement(new Double(a)); }
        else if (type == EVENT_POINTER_PRESSED || type == EVENT_POINTER_RELEASED || type == EVENT_POINTER_DRAGGED) { args.addElement(new Double(a)); args.addElement(new Double(b)); }
        else if (type == EVENT_SIZE) { args.addElement(new Double(bufferWidth)); args.addElement(new Double(bufferHeight)); }
        else if (type == EVENT_TICK) { args.addElement(new Double(a)); }
        else { args.addElement(new Double(bufferWidth)); args.addElement(new Double(bufferHeight)); }
        fire(EVENT_NAMES[type], args);
    }
    // | (Dispatch every queued event; returns how many were delivered)
    public int pump() {
        int count = 0;
        while (true) {
            int[] event;
            synchronized (events) {
                if (events.isEmpty()) { return count; }
                event = (int[]) events.elementAt(0);
                events.removeElementAt(0);
            }
            dispatch(event[0], event[1], event[2], event[3], event[4]);
            count++;
        }
    }
    // | (Discard pending events)
    public void clearEvents() {
        synchronized (events) { events.removeAllElements(); }
        synchronized (pendingCommands) { pendingCommands.clear(); }
    }
    // | (Blocking key read used by `canvas:waitKey(ms)`; returns {code, action} or null)
    public int[] waitKey(int timeout) {
        if (loopThread == Thread.currentThread()) { return null; }
        long deadline = timeout > 0 ? System.currentTimeMillis() + timeout : 0;
        hold = true;
        try {
            while (true) {
                int[] key = null;
                synchronized (events) {
                    for (int i = 0; i < events.size(); i++) {
                        int[] event = (int[]) events.elementAt(i);
                        if (event[0] == EVENT_KEY_PRESSED || event[0] == EVENT_KEY_REPEATED) { key = event; events.removeElementAt(i); break; }
                    }
                }
                if (key != null) { return new int[] { key[1], key[2] }; }
                long remaining = timeout <= 0 ? 0 : deadline - System.currentTimeMillis();
                if (timeout > 0 && remaining <= 0) { return null; }
                synchronized (events) {
                    try { events.wait(timeout <= 0 ? 0 : remaining); }
                    catch (InterruptedException e) { return null; }
                }
            }
        } finally { hold = false; }
    }
    // |
    // Loop driving
    public boolean isRunning() { return loopThread != null; }
    public Thread getLoopThread() { return loopThread; }
    public long getAge() { return System.currentTimeMillis() - startMillis; }
    public void runLoop(Lua.LuaFunction fn, int delay) {
        if (loopThread != null) { return; }
        loopThread = Thread.currentThread();
        loopPending = false;
        long last = System.currentTimeMillis();
        try {
            while (loopThread == Thread.currentThread()) {
                pump();
                long now = System.currentTimeMillis();
                Lua.LuaFunction step = fn != null ? fn : callback("tick");
                if (step != null) {
                    Vector args = new Vector();
                    args.addElement(screen);
                    args.addElement(new Double(now - last));
                    Object result = lua != null ? lua.invokeCallback(step, args) : null;
                    // A Lua `return false` arrives as a one-value Vector.
                    if (result instanceof Vector) { Vector list = (Vector) result; result = list.isEmpty() ? null : list.elementAt(0); }
                    if (Boolean.FALSE.equals(result)) { break; }
                }
                last = now;
                flush();
                if (lua != null && lua.PID != null && midlet.sys.get(lua.PID) == null) { break; }
                if (delay > 0) {
                    try { Thread.sleep(delay); }
                    catch (InterruptedException e) { break; }
                }
            }
        } finally {
            loopThread = null;
        }
    }
    public void animate(Lua.LuaFunction fn, int delay) {
        if (loopThread != null || loopPending) { return; }
        // Start queuing input/commands before the thread exists, then give the
        // caller time to unwind: the script that started us must leave the
        // interpreter before the loop thread enters it.
        loopPending = true;
        new Thread(new LoopRunner(this, fn, delay)).start();
    }
    public void stop() { loopThread = null; loopPending = false; }
    // named runner: the SDK preverifier chokes on synthetic anon classes
    static class LoopRunner implements Runnable {
        private LuaCanvas canvas;
        private Lua.LuaFunction fn;
        private int delay;
        LoopRunner(LuaCanvas canvas, Lua.LuaFunction fn, int delay) { this.canvas = canvas; this.fn = fn; this.delay = delay; }
        public void run() {
            try { Thread.sleep(200); } catch (InterruptedException e) { canvas.stop(); return; }
            canvas.runLoop(fn, delay);
        }
    }
    // |
    // Presentation
    public void present() {
        if (lua != null) { lua.kill = false; if (lua.proc != null) { lua.proc.screen = this; } }
        midlet.display.setCurrent(this);
        ensureBuffer();
    }
    public void close() {
        stop();
        clearEvents();
        if (lua != null) {
            midlet.sys.remove(lua.PID);
            if (lua.proc != null) { lua.proc.screen = null; }
        }
        if (midlet.display.getCurrent() == this) {
            Displayable target = null;
            for (Enumeration e = midlet.sys.keys(); e.hasMoreElements();) {
                Process p = (Process) midlet.sys.get(e.nextElement());
                if (p != null && p.screen != null && p.screen != this) { target = p.screen; }
            }
            if (target != null) { midlet.display.setCurrent(target); }
            else { midlet.destroyApp(true); }
        }
    }
    public void flush() { repaint(); serviceRepaints(); }
    // |
    // Frame buffer
    public int getCanvasWidth() { ensureBuffer(); return bufferWidth; }
    public int getCanvasHeight() { ensureBuffer(); return bufferHeight; }
    private void ensureBuffer() {
        int w = getWidth(), h = getHeight();
        if (w <= 0) { w = 1; }
        if (h <= 0) { h = 1; }
        if (g != null && w == bufferWidth && h == bufferHeight) { return; }
        bufferWidth = w;
        bufferHeight = h;
        buffer = Image.createImage(w, h);
        g = buffer.getGraphics();
        g.setFont(font);
        g.setColor(background);
        g.fillRect(0, 0, w, h);
        g.setColor(color);
    }
    // |
    // Size / visibility
    protected void sizeChanged(int w, int h) {
        ensureBuffer();
        post(EVENT_SIZE, w, h, 0, 0);
    }
    protected void showNotify() { post(EVENT_SHOW, 0, 0, 0, 0); }
    protected void hideNotify() { post(EVENT_HIDE, 0, 0, 0, 0); }
    // |
    // Paint
    protected void paint(Graphics screenGraphics) {
        ensureBuffer();
        // In loop mode the loop thread owns the buffer, so only blit it here:
        // re-entering Lua from the LCDUI thread would race the loop.
        if (loopThread == null) { fire(EVENT_NAMES[EVENT_PAINT], paintArgs()); }
        screenGraphics.drawImage(buffer, 0, 0, Graphics.TOP | Graphics.LEFT);
    }
    private Vector paintArgs() {
        Vector args = new Vector();
        args.addElement(screen);
        args.addElement(new Double(bufferWidth));
        args.addElement(new Double(bufferHeight));
        return args;
    }
    // |
    // Colors and fonts
    public void setColorValue(int rgb) { color = rgb & 0xFFFFFF; ensureBuffer(); g.setColor(color); }
    public int getColorValue() { return color; }
    public void setBackgroundValue(int rgb) { background = rgb & 0xFFFFFF; }
    public int getBackgroundValue() { return background; }
    public void setForegroundValue(int rgb) { foreground = rgb & 0xFFFFFF; color = foreground; ensureBuffer(); g.setColor(color); }
    public int getForegroundValue() { return foreground; }
    public void setFontValue(Font value, String spec) {
        font = value == null ? Font.getDefaultFont() : value;
        fontSpec = spec == null ? "medium" : spec;
        ensureBuffer();
        g.setFont(font);
    }
    public Font getFontValue() { return font; }
    public String getFontSpec() { return fontSpec; }
    public int getFontHeight() { return font.getHeight(); }
    public int getBaseline() { return font.getBaselinePosition(); }
    public int getTextWidth(String text) { return text == null ? 0 : font.stringWidth(text); }
    public int getCharWidth(char ch) { return font.charWidth(ch); }
    // |
    // State stack
    public void saveState() {
        ensureBuffer();
        savedStates.addElement(new Object[] { new Integer(g.getTranslateX()), new Integer(g.getTranslateY()), new Integer(g.getClipX()), new Integer(g.getClipY()), new Integer(g.getClipWidth()), new Integer(g.getClipHeight()), new Integer(color), fontSpec, font });
    }
    public void restoreState() {
        ensureBuffer();
        if (savedStates.isEmpty()) { return; }
        Object[] state = (Object[]) savedStates.elementAt(savedStates.size() - 1);
        savedStates.removeElementAt(savedStates.size() - 1);
        int tx = ((Integer) state[0]).intValue(), ty = ((Integer) state[1]).intValue();
        g.translate(tx - g.getTranslateX(), ty - g.getTranslateY());
        g.setClip(((Integer) state[2]).intValue(), ((Integer) state[3]).intValue(), ((Integer) state[4]).intValue(), ((Integer) state[5]).intValue());
        color = ((Integer) state[6]).intValue();
        g.setColor(color);
        fontSpec = (String) state[7];
        font = (Font) state[8];
        g.setFont(font);
    }
    public void resetState() {
        ensureBuffer();
        g.translate(-g.getTranslateX(), -g.getTranslateY());
        g.setClip(0, 0, bufferWidth, bufferHeight);
        color = foreground;
        g.setColor(color);
        font = Font.getDefaultFont();
        fontSpec = "medium";
        g.setFont(font);
        savedStates.removeAllElements();
    }
    // |
    // Shapes
    public void clear() { clear(0, 0, getCanvasWidth(), getCanvasHeight()); }
    public void clear(int x, int y, int w, int h) {
        ensureBuffer();
        int saved = color;
        g.setColor(background);
        g.fillRect(x, y, w, h);
        g.setColor(saved);
    }
    public void drawLine(int x1, int y1, int x2, int y2) { ensureBuffer(); g.drawLine(x1, y1, x2, y2); }
    public void drawRect(int x, int y, int w, int h) { ensureBuffer(); g.drawRect(x, y, w, h); }
    public void fillRect(int x, int y, int w, int h) { ensureBuffer(); g.fillRect(x, y, w, h); }
    public void drawRoundRect(int x, int y, int w, int h, int aw, int ah) { ensureBuffer(); g.drawRoundRect(x, y, w, h, aw, ah); }
    public void fillRoundRect(int x, int y, int w, int h, int aw, int ah) { ensureBuffer(); g.fillRoundRect(x, y, w, h, aw, ah); }
    public void drawArc(int x, int y, int w, int h, int start, int angle) { ensureBuffer(); g.drawArc(x, y, w, h, start, angle); }
    public void fillArc(int x, int y, int w, int h, int start, int angle) { ensureBuffer(); g.fillArc(x, y, w, h, start, angle); }
    public void drawPixel(int x, int y) { ensureBuffer(); g.drawLine(x, y, x, y); }
    public void translate(int x, int y) { ensureBuffer(); g.translate(x, y); }
    public void setClip(int x, int y, int w, int h) { ensureBuffer(); g.setClip(x, y, w, h); }
    public void resetClip() { ensureBuffer(); g.setClip(0, 0, bufferWidth, bufferHeight); }
    public int[] getClip() { ensureBuffer(); return new int[] { g.getClipX(), g.getClipY(), g.getClipWidth(), g.getClipHeight() }; }
    public int[] getCursor() { return new int[] { cursorX, cursorY }; }
    // | (Scanline polygon fill: portable across MIDP 2.0 devices and the desktop shim)
    public void drawPolygon(int[] xs, int[] ys) {
        ensureBuffer();
        if (xs.length < 2) { return; }
        for (int i = 0; i < xs.length; i++) {
            int j = (i + 1) % xs.length;
            g.drawLine(xs[i], ys[i], xs[j], ys[j]);
        }
    }
    public void fillPolygon(int[] xs, int[] ys) {
        ensureBuffer();
        int n = xs.length;
        if (n < 3) { return; }
        int minY = ys[0], maxY = ys[0];
        for (int i = 1; i < n; i++) {
            if (ys[i] < minY) { minY = ys[i]; }
            if (ys[i] > maxY) { maxY = ys[i]; }
        }
        int[] hits = new int[n];
        for (int y = minY; y <= maxY; y++) {
            double scan = y + 0.5;
            int count = 0;
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                double yi = ys[i], yj = ys[j];
                if ((yi <= scan && yj > scan) || (yj <= scan && yi > scan)) {
                    double t = (scan - yi) / (yj - yi);
                    hits[count++] = (int) Math.floor(xs[i] + t * (xs[j] - xs[i]) + 0.5);
                }
            }
            for (int a = 1; a < count; a++) {
                int value = hits[a], b = a - 1;
                while (b >= 0 && hits[b] > value) { hits[b + 1] = hits[b]; b--; }
                hits[b + 1] = value;
            }
            for (int a = 0; a + 1 < count; a += 2) { g.drawLine(hits[a], y, hits[a + 1], y); }
        }
    }
    // |
    // Text
    public void drawText(String text, int x, int y, int anchor) {
        ensureBuffer();
        g.drawString(text == null ? "" : text, x, y, normalizeAnchor(anchor));
    }
    public void setCursor(int x, int y) { cursorX = x; cursorY = y; }
    public void printText(String text) {
        ensureBuffer();
        if (text == null) { return; }
        g.setColor(color);
        g.setFont(font);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\n') { newLine(); continue; }
            if (ch == '\r') { continue; }
            int advance = font.charWidth(ch);
            if (cursorX + advance > bufferWidth) { newLine(); }
            if (cursorY + font.getHeight() > bufferHeight) { cursorY = 0; }
            g.drawChar(ch, cursorX, cursorY, Graphics.TOP | Graphics.LEFT);
            cursorX += advance;
            lastAdvance = advance;
        }
    }
    public void newLine() {
        cursorX = 0;
        cursorY += font.getHeight();
        if (cursorY + font.getHeight() > bufferHeight) { cursorY = 0; }
    }
    public void backspace(int count) {
        ensureBuffer();
        if (count <= 0) { count = 1; }
        int saved = color;
        for (int i = 0; i < count; i++) {
            int advance = lastAdvance > 0 ? lastAdvance : font.charWidth(' ');
            cursorX -= advance;
            if (cursorX < 0) { cursorX = 0; }
            g.setColor(background);
            g.fillRect(cursorX, cursorY, advance, font.getHeight());
        }
        g.setColor(saved);
    }
    public void drawCaret() {
        ensureBuffer();
        g.fillRect(cursorX, cursorY, 1, font.getHeight());
    }
    // |
    // Images
    public void drawImage(Image image, int x, int y, int anchor) {
        ensureBuffer();
        if (image == null) { return; }
        g.drawImage(image, x, y, normalizeAnchor(anchor));
    }
    // |
    // Input
    public boolean isKeyDown(int keyCode) { return keysDown.containsKey(new Integer(keyCode)); }
    public int gameAction(int keyCode) { try { return getGameAction(keyCode); } catch (Throwable e) { return 0; } }
    public String keyName(int keyCode) {
        try { String value = getKeyName(keyCode); return value == null ? "key" + keyCode : value; }
        catch (Throwable e) { return "key" + keyCode; }
    }
    public int getPointerX() { return pointerX; }
    public int getPointerY() { return pointerY; }
    // | (Held keys as a Lua array table, so `local keys = canvas:keys()` can be
    // |  indexed and measured with `#keys`)
    public Hashtable getPressedKeys() {
        Hashtable result = new Hashtable();
        int index = 1;
        for (Enumeration e = keysDown.keys(); e.hasMoreElements();) {
            result.put(new Double(index++), new Double(((Integer) e.nextElement()).intValue()));
        }
        return result;
    }
    public void resetKeys() { keysDown.clear(); }
    protected void keyPressed(int keyCode) { keysDown.put(new Integer(keyCode), Boolean.TRUE); post(EVENT_KEY_PRESSED, keyCode, getGameAction(keyCode), 0, 0); }
    protected void keyReleased(int keyCode) { keysDown.remove(new Integer(keyCode)); post(EVENT_KEY_RELEASED, keyCode, 0, 0, 0); }
    protected void keyRepeated(int keyCode) { post(EVENT_KEY_REPEATED, keyCode, getGameAction(keyCode), 0, 0); }
    protected void pointerPressed(int x, int y) { pointerX = x; pointerY = y; post(EVENT_POINTER_PRESSED, x, y, 0, 0); }
    protected void pointerReleased(int x, int y) { pointerX = x; pointerY = y; post(EVENT_POINTER_RELEASED, x, y, 0, 0); }
    protected void pointerDragged(int x, int y) { pointerX = x; pointerY = y; post(EVENT_POINTER_DRAGGED, x, y, 0, 0); }
    // | (Ask the device for key repeat; harmless where unsupported)
    public boolean hasRepeatEvents() { return true; }
    // |
    // Commands
    public void addCommandHandler(Command command, Lua.LuaFunction fn) {
        addCommand(command);
        commands.addElement(command);
        if (fn != null) { commandHandlers.put(command, fn); }
    }
    public void clearCommands() {
        for (int i = 0; i < commands.size(); i++) { removeCommand((Command) commands.elementAt(i)); }
        commands.removeAllElements();
        commandHandlers.clear();
    }
    public void commandAction(Command command, Displayable displayable) {
        // While a loop runs the loop thread owns the interpreter, so queue the
        // command and let pump() invoke it instead of re-entering Lua here.
        if ((loopThread != null || loopPending) && command != null) {
            int id;
            synchronized (pendingCommands) { id = commandSeq++; pendingCommands.put(new Integer(id), command); }
            synchronized (events) { events.addElement(new int[] { EVENT_COMMAND, id, 0, 0, 0 }); events.notifyAll(); }
            return;
        }
        invokeCommand(command);
    }
    private void invokeCommand(Command command) {
        Object fn = commandHandlers.get(command);
        Vector args = new Vector();
        args.addElement(screen);
        args.addElement(command == null ? "" : command.getLabel());
        if (fn instanceof Lua.LuaFunction) { invoke((Lua.LuaFunction) fn, args); }
        else { fire(EVENT_NAMES[EVENT_COMMAND], args); }
    }
    private void invoke(Lua.LuaFunction fn, Vector args) {
        if (fn == null) { return; }
        if (lua != null) { lua.invokeCallback(fn, args); }
        else { try { fn.call(args); } catch (Throwable e) { } }
    }
    // |
    // Fullscreen / title
    public void setFullscreen(boolean value) {
        fullscreen = value;
        try { setFullScreenMode(value); } catch (Throwable e) { }
        ensureBuffer();
    }
    public boolean isFullscreen() { return fullscreen; }
    // |
    // Helpers
    public static int normalizeAnchor(int anchor) {
        if ((anchor & (Graphics.LEFT | Graphics.RIGHT | Graphics.HCENTER)) == 0) { anchor |= Graphics.LEFT; }
        if ((anchor & (Graphics.TOP | Graphics.BOTTOM | Graphics.VCENTER | Graphics.BASELINE)) == 0) { anchor |= Graphics.TOP; }
        return anchor;
    }
    // | (Font spec parser: "monospace bold large", "default", "small"...)
    public static Font parseFont(String spec) {
        if (spec == null || spec.length() == 0 || spec.equalsIgnoreCase("default")) { return Font.getDefaultFont(); }
        int face = Font.FACE_SYSTEM, style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM;
        String[] tokens = splitSpaces(spec.toLowerCase());
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.equals("system")) { face = Font.FACE_SYSTEM; }
            else if (token.equals("mono") || token.equals("monospace")) { face = Font.FACE_MONOSPACE; }
            else if (token.equals("proportional") || token.equals("prop")) { face = Font.FACE_PROPORTIONAL; }
            else if (token.equals("bold")) { style |= Font.STYLE_BOLD; }
            else if (token.equals("italic")) { style |= Font.STYLE_ITALIC; }
            else if (token.equals("ul") || token.equals("underline") || token.equals("underlined")) { style |= Font.STYLE_UNDERLINED; }
            else if (token.equals("small")) { size = Font.SIZE_SMALL; }
            else if (token.equals("medium") || token.equals("normal")) { size = Font.SIZE_MEDIUM; }
            else if (token.equals("large") || token.equals("big")) { size = Font.SIZE_LARGE; }
        }
        Font value = Font.getFont(face, style, size);
        return value == null ? Font.getDefaultFont() : value;
    }
    private static String[] splitSpaces(String text) {
        Vector parts = new Vector();
        int start = -1;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == ' ' || ch == '\t' || ch == ',') { if (start >= 0) { parts.addElement(text.substring(start, i)); start = -1; } }
            else if (start < 0) { start = i; }
        }
        if (start >= 0) { parts.addElement(text.substring(start)); }
        String[] result = new String[parts.size()];
        for (int i = 0; i < parts.size(); i++) { result[i] = (String) parts.elementAt(i); }
        return result;
    }
    // | (Named colors shared with `graphics.canvas.colors`; see Lua.namedColors)
    public static int namedColor(String name) {
        if (name == null) { return -1; }
        String key = name.trim().toLowerCase();
        if (key.startsWith("#")) { key = key.substring(1); }
        if (key.startsWith("0x")) { key = key.substring(2); }
        if (key.length() == 6) {
            try { return Integer.parseInt(key, 16); } catch (Exception e) { return -1; }
        }
        if (key.equals("black")) { return 0x000000; }
        if (key.equals("white")) { return 0xFFFFFF; }
        if (key.equals("red")) { return 0xFF0000; }
        if (key.equals("green")) { return 0x00FF00; }
        if (key.equals("blue")) { return 0x0000FF; }
        if (key.equals("yellow")) { return 0xFFFF00; }
        if (key.equals("cyan")) { return 0x00FFFF; }
        if (key.equals("magenta")) { return 0xFF00FF; }
        if (key.equals("gray") || key.equals("grey")) { return 0x808080; }
        if (key.equals("orange")) { return 0xFF8000; }
        if (key.equals("purple")) { return 0x800080; }
        if (key.equals("brown")) { return 0x804000; }
        if (key.equals("pink")) { return 0xFF80C0; }
        if (key.equals("lime")) { return 0x80FF00; }
        if (key.equals("navy")) { return 0x000080; }
        if (key.equals("teal")) { return 0x008080; }
        if (key.equals("silver")) { return 0xC0C0C0; }
        if (key.equals("maroon")) { return 0x800000; }
        if (key.equals("olive")) { return 0x808000; }
        return -1;
    }
}
// |
// EOF
