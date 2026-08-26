import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDlet;
import javax.microedition.media.control.*;
import javax.microedition.io.file.*;
import javax.wireless.messaging.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// LuaCanvas - Canvas support for graphics library
class LuaCanvas extends Canvas implements CommandListener {
    private OpenTTY midlet;
    private Hashtable callbacks;
    private Image offscreen;
    private Graphics offgc;
    private int width, height;
    private boolean doubleBuffered = true;
    private Vector commands = new Vector();
    private int backgroundColor = 0xFFFFFF;
    private int foregroundColor = 0x000000;
    private Font currentFont = Font.getDefaultFont();
    private int currentX = 0, currentY = 0;
    
    // Event types
    public static final int EVENT_PAINT = 1;
    public static final int EVENT_KEY_PRESSED = 2;
    public static final int EVENT_KEY_RELEASED = 3;
    public static final int EVENT_POINTER_PRESSED = 4;
    public static final int EVENT_POINTER_RELEASED = 5;
    public static final int EVENT_POINTER_DRAGGED = 6;
    public static final int EVENT_SHOW = 7;
    public static final int EVENT_HIDE = 8;
    public static final int EVENT_SIZE_CHANGED = 9;
    
    public LuaCanvas(OpenTTY midlet, Hashtable callbacks) {
        this.midlet = midlet;
        this.callbacks = callbacks != null ? callbacks : new Hashtable();
        this.width = getWidth();
        this.height = getHeight();
        this.offscreen = Image.createImage(width, height);
        this.offgc = offscreen.getGraphics();
        
        setCommandListener(this);
    }
    
    // Callback registration
    public void setCallback(String event, Lua.LuaFunction func) {
        if (event.equals("paint")) callbacks.put(new Integer(EVENT_PAINT), func);
        else if (event.equals("keyPressed")) callbacks.put(new Integer(EVENT_KEY_PRESSED), func);
        else if (event.equals("keyReleased")) callbacks.put(new Integer(EVENT_KEY_RELEASED), func);
        else if (event.equals("pointerPressed")) callbacks.put(new Integer(EVENT_POINTER_PRESSED), func);
        else if (event.equals("pointerReleased")) callbacks.put(new Integer(EVENT_POINTER_RELEASED), func);
        else if (event.equals("pointerDragged")) callbacks.put(new Integer(EVENT_POINTER_DRAGGED), func);
        else if (event.equals("show")) callbacks.put(new Integer(EVENT_SHOW), func);
        else if (event.equals("hide")) callbacks.put(new Integer(EVENT_HIDE), func);
        else if (event.equals("sizeChanged")) callbacks.put(new Integer(EVENT_SIZE_CHANGED), func);
    }
    
    // Drawing methods
    public void setColor(int rgb) {
        offgc.setColor(rgb);
    }
    
    public void setColor(int r, int g, int b) {
        offgc.setColor(r, g, b);
    }
    
    public void setBackgroundColor(int rgb) {
        backgroundColor = rgb;
        offgc.setColor(rgb);
        offgc.fillRect(0, 0, width, height);
    }
    
    public void setForegroundColor(int rgb) {
        foregroundColor = rgb;
        offgc.setColor(rgb);
    }
    
    public void setFont(Font font) {
        currentFont = font;
        offgc.setFont(font);
    }
    
    public void setFont(String spec) {
        Font font = parseFont(spec);
        if (font != null) {
            currentFont = font;
            offgc.setFont(font);
        }
    }
    
    private Font parseFont(String spec) {
        int face = Font.FACE_SYSTEM;
        int style = Font.STYLE_PLAIN;
        int size = Font.SIZE_MEDIUM;
        
        String[] parts = midlet.split(spec.toLowerCase(), ' ');
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.equals("system")) face = Font.FACE_SYSTEM;
            else if (p.equals("monospace")) face = Font.FACE_MONOSPACE;
            else if (p.equals("proportional")) face = Font.FACE_PROPORTIONAL;
            else if (p.equals("bold")) style |= Font.STYLE_BOLD;
            else if (p.equals("italic")) style |= Font.STYLE_ITALIC;
            else if (p.equals("underlined") || p.equals("underline") || p.equals("ul")) style |= Font.STYLE_UNDERLINED;
            else if (p.equals("small")) size = Font.SIZE_SMALL;
            else if (p.equals("medium")) size = Font.SIZE_MEDIUM;
            else if (p.equals("large")) size = Font.SIZE_LARGE;
        }
        
        return Font.getFont(face, style, size);
    }
    
    public void drawLine(int x1, int y1, int x2, int y2) {
        offgc.drawLine(x1, y1, x2, y2);
    }
    
    public void drawRect(int x, int y, int width, int height) {
        offgc.drawRect(x, y, width, height);
    }
    
    public void fillRect(int x, int y, int width, int height) {
        offgc.fillRect(x, y, width, height);
    }
    
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        offgc.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }
    
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        offgc.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }
    
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        offgc.drawArc(x, y, width, height, startAngle, arcAngle);
    }
    
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        offgc.fillArc(x, y, width, height, startAngle, arcAngle);
    }
    
    public void drawString(String text, int x, int y, int anchor) {
        offgc.drawString(text, x, y, anchor);
    }
    
    public void drawString(String text, int x, int y) {
        offgc.drawString(text, x, y, Graphics.TOP | Graphics.LEFT);
    }
    
    public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
        offgc.drawChars(data, offset, length, x, y, anchor);
    }
    
    public void drawImage(Image img, int x, int y, int anchor) {
        offgc.drawImage(img, x, y, anchor);
    }
    
    public void drawImage(String resource, int x, int y, Hashtable scope) {
        Image img = midlet.readImg(resource, scope);
        if (img != null) {
            offgc.drawImage(img, x, y, Graphics.TOP | Graphics.LEFT);
        }
    }
    
    public void drawPixel(int x, int y) {
        offgc.drawLine(x, y, x, y);
    }
    
    public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
        offgc.drawRGB(rgbData, offset, scanlength, x, y, width, height, processAlpha);
    }
    
    public void clear() {
        offgc.setColor(backgroundColor);
        offgc.fillRect(0, 0, this.width, this.height);
    }
    
    public void clear(int x, int y, int width, int height) {
        offgc.setColor(backgroundColor);
        offgc.fillRect(x, y, width, height);
    }
    
    public void translate(int x, int y) {
        offgc.translate(x, y);
        currentX += x;
        currentY += y;
    }
    
    public void setClip(int x, int y, int width, int height) {
        offgc.setClip(x, y, width, height);
    }
    
    public void repaint() {
        repaint(0, 0, width, height);
    }
    
    public void repaint(int x, int y, int w, int h) {
        super.repaint(x, y, w, h);
    }
    
    public void serviceRepaints() {
        super.serviceRepaints();
    }
    
    public int getCanvasWidth() {
        return getWidth();
    }
    
    public int getCanvasHeight() {
        return getHeight();
    }
    
    public int getDrawX() {
        return currentX;
    }
    
    public int getDrawY() {
        return currentY;
    }
    
    public Font getCurrentFont() {
        return currentFont;
    }
    
    public int stringWidth(String str) {
        return currentFont.stringWidth(str);
    }
    
    public int charWidth(char ch) {
        return currentFont.charWidth(ch);
    }
    
    public int getBaselinePosition() {
        return currentFont.getBaselinePosition();
    }
    
    public int getHeight(String str) {
        return currentFont.getHeight();
    }
    
    public void flush() {
        repaint();
    }
    
    // Canvas overrides
    protected void paint(Graphics g) {
        // Copy offscreen buffer to display
        g.drawImage(offscreen, 0, 0, Graphics.TOP | Graphics.LEFT);
        
        // Call Lua callback
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_PAINT));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(g.getClipX()));
                args.addElement(new Double(g.getClipY()));
                args.addElement(new Double(g.getClipWidth()));
                args.addElement(new Double(g.getClipHeight()));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void keyPressed(int keyCode) {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_KEY_PRESSED));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(keyCode));
                args.addElement(getGameAction(keyCode) == 0 ? LUA_NIL : new Double(getGameAction(keyCode)));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void keyReleased(int keyCode) {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_KEY_RELEASED));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(keyCode));
                args.addElement(getGameAction(keyCode) == 0 ? LUA_NIL : new Double(getGameAction(keyCode)));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerPressed(int x, int y) {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_POINTER_PRESSED));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerReleased(int x, int y) {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_POINTER_RELEASED));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerDragged(int x, int y) {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_POINTER_DRAGGED));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void showNotify() {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_SHOW));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void hideNotify() {
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_HIDE));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void sizeChanged(int w, int h) {
        this.width = w;
        this.height = h;
        
        // Recreate offscreen buffer
        offscreen = Image.createImage(w, h);
        offgc = offscreen.getGraphics();
        
        Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(new Integer(EVENT_SIZE_CHANGED));
        if (callback != null) {
            try {
                Vector args = new Vector();
                args.addElement(this);
                args.addElement(new Double(w));
                args.addElement(new Double(h));
                callback.call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    // CommandListener interface
    public void commandAction(Command c, Displayable d) {
        if (callbacks.containsKey(c)) {
            Lua.LuaFunction callback = (Lua.LuaFunction) callbacks.get(c);
            if (callback != null) {
                try {
                    Vector args = new Vector();
                    args.addElement(this);
                    args.addElement(c.getLabel());
                    callback.call(args);
                } catch (Exception e) {
                    midlet.print(midlet.getCatch(e), midlet.stdout);
                }
            }
        }
    }
    
    public void addCommand(Command cmd, Lua.LuaFunction handler) {
        super.addCommand(cmd);
        callbacks.put(cmd, handler);
    }
    
    // Game action constants
    public static final int UP = Canvas.UP;
    public static final int DOWN = Canvas.DOWN;
    public static final int LEFT = Canvas.LEFT;
    public static final int RIGHT = Canvas.RIGHT;
    public static final int FIRE = Canvas.FIRE;
    public static final int GAME_A = Canvas.GAME_A;
    public static final int GAME_B = Canvas.GAME_B;
    public static final int GAME_C = Canvas.GAME_C;
    public static final int GAME_D = Canvas.GAME_D;
    
    // Anchor constants
    public static final int HCENTER = Graphics.HCENTER;
    public static final int VCENTER = Graphics.VCENTER;
    public static final int LEFT = Graphics.LEFT;
    public static final int RIGHT = Graphics.RIGHT;
    public static final int TOP = Graphics.TOP;
    public static final int BOTTOM = Graphics.BOTTOM;
    public static final int BASELINE = Graphics.BASELINE;
    
    public static final Object LUA_NIL = new Object();
}