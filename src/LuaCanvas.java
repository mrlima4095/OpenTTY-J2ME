import javax.microedition.lcdui.*;
import java.util.*;
import java.io.*;
// |
// Lua Canvas
public class LuaCanvas extends Canvas implements Runnable {
    private OpenTTY midlet;
    private String title;
    private Hashtable handlerTable = new Hashtable();
    private Graphics lastGraphics;
    private boolean needsRepaint = true;
    
    public LuaCanvas(OpenTTY midlet, String title, boolean fullscreen) {
        this.midlet = midlet;
        this.title = title;
        setTitle(title);
        setFullScreenMode(fullscreen);
        
        // Inicia uma thread para repaint periódico
        new Thread(this).start();
    }
    
    public void setHandlerTable(Hashtable handlerTable) {
        this.handlerTable = handlerTable;
        needsRepaint = true;
    }
    
    public void run() {
        while (true) {
            try {
                Thread.sleep(100); // 10 FPS
                if (needsRepaint) {
                    repaint();
                    needsRepaint = false;
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
    
    protected void paint(Graphics g) {
        lastGraphics = g;
        
        // Chama a função paint do handler se existir
        Object paintHandler = handlerTable.get("paint");
        if (paintHandler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(g);
                ((Lua.LuaFunction) paintHandler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
        
        // Se não houver paint handler, chama repaint se existir
        else {
            Object repaintHandler = handlerTable.get("repaint");
            if (repaintHandler instanceof Lua.LuaFunction) {
                try {
                    Vector args = new Vector();
                    args.addElement(g);
                    ((Lua.LuaFunction) repaintHandler).call(args);
                } catch (Exception e) {
                    midlet.print(midlet.getCatch(e), midlet.stdout);
                }
            }
        }
    }
    
    protected void keyPressed(int keyCode) {
        Object handler = handlerTable.get("keyPressed");
        if (handler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((Lua.LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void keyReleased(int keyCode) {
        Object handler = handlerTable.get("keyReleased");
        if (handler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((Lua.LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void keyRepeated(int keyCode) {
        Object handler = handlerTable.get("keyRepeated");
        if (handler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((Lua.LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerDragged(int x, int y) {
        Object handler = handlerTable.get("pointerDragged");
        if (handler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((Lua.LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerPressed(int x, int y) {
        Object handler = handlerTable.get("pointerPressed");
        if (handler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((Lua.LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerReleased(int x, int y) {
        Object handler = handlerTable.get("pointerReleased");
        if (handler instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((Lua.LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    public void requestRepaint() {
        needsRepaint = true;
    }
}