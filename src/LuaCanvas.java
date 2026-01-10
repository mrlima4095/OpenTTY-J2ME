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
        if (paintHandler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(g);
                ((LuaFunction) paintHandler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
        
        // Se não houver paint handler, chama repaint se existir
        else {
            Object repaintHandler = handlerTable.get("repaint");
            if (repaintHandler instanceof LuaFunction) {
                try {
                    Vector args = new Vector();
                    args.addElement(g);
                    ((LuaFunction) repaintHandler).call(args);
                } catch (Exception e) {
                    midlet.print(midlet.getCatch(e), midlet.stdout);
                }
            }
        }
    }
    
    protected void keyPressed(int keyCode) {
        Object handler = handlerTable.get("keyPressed");
        if (handler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void keyReleased(int keyCode) {
        Object handler = handlerTable.get("keyReleased");
        if (handler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void keyRepeated(int keyCode) {
        Object handler = handlerTable.get("keyRepeated");
        if (handler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerDragged(int x, int y) {
        Object handler = handlerTable.get("pointerDragged");
        if (handler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerPressed(int x, int y) {
        Object handler = handlerTable.get("pointerPressed");
        if (handler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    protected void pointerReleased(int x, int y) {
        Object handler = handlerTable.get("pointerReleased");
        if (handler instanceof LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((LuaFunction) handler).call(args);
            } catch (Exception e) {
                midlet.print(midlet.getCatch(e), midlet.stdout);
            }
        }
    }
    
    public void requestRepaint() {
        needsRepaint = true;
    }
    
    private String getKeyName(int keyCode) {
        String name = getKeyName(keyCode);
        if (name != null) return name;
        
        switch (keyCode) {
            case KEY_NUM0: return "0";
            case KEY_NUM1: return "1";
            case KEY_NUM2: return "2";
            case KEY_NUM3: return "3";
            case KEY_NUM4: return "4";
            case KEY_NUM5: return "5";
            case KEY_NUM6: return "6";
            case KEY_NUM7: return "7";
            case KEY_NUM8: return "8";
            case KEY_NUM9: return "9";
            case KEY_STAR: return "*";
            case KEY_POUND: return "#";
            case UP: return "UP";
            case DOWN: return "DOWN";
            case LEFT: return "LEFT";
            case RIGHT: return "RIGHT";
            case FIRE: return "FIRE";
            case GAME_A: return "GAME_A";
            case GAME_B: return "GAME_B";
            case GAME_C: return "GAME_C";
            case GAME_D: return "GAME_D";
            default: return "KEY_" + keyCode;
        }
    }
}