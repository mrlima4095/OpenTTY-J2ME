import javax.microedition.lcdui.*;
import java.util.*;
// |
// MIDletCanvas
public class MIDletCanvas extends Canvas implements CommandListener {
    private OpenTTY midlet;
    private Hashtable PKG;
    private Command BACK, USER;
    private int id;
    private Graphics graphicsBuffer;
    private Image bufferImage;
    
    public MIDletCanvas(OpenTTY midlet, Hashtable PKG, int id) {
        this.midlet = midlet; this.PKG = PKG; this.id = id;
        
        // Configurar título
        String title = getValue(PKG, "title", "Canvas");
        setTitle(title);
        
        // Configurar comandos
        Object backObj = PKG.get("back");
        if (backObj instanceof Hashtable) {
            Hashtable backTable = (Hashtable) backObj;
            String backLabel = getValue(backTable, "label", "Back");
            addCommand(BACK = new Command(backLabel, Command.BACK, 1));
        } else {
            addCommand(BACK = new Command("Back", Command.BACK, 1));
        }
        
        Object buttonObj = PKG.get("button");
        if (buttonObj instanceof Hashtable) {
            Hashtable buttonTable = (Hashtable) buttonObj;
            String buttonLabel = getValue(buttonTable, "label", "Menu");
            addCommand(USER = new Command(buttonLabel, Command.SCREEN, 2));
        }
        
        // Configurar tela cheia
        Object fullscreenObj = PKG.get("fullscreen");
        if (fullscreenObj instanceof Boolean) {
            setFullScreenMode(((Boolean)fullscreenObj).booleanValue());
        } else if (fullscreenObj instanceof String) {
            setFullScreenMode("true".equals(fullscreenObj));
        }
        
        setCommandListener(this);
        
        // Criar buffer para desenho
        bufferImage = Image.createImage(getWidth(), getHeight());
        graphicsBuffer = bufferImage.getGraphics();
    }
    
    protected void paint(Graphics g) {
        // Limpar o buffer
        graphicsBuffer.setColor(0, 0, 0);
        graphicsBuffer.fillRect(0, 0, getWidth(), getHeight());
        
        // Desenhar fundo
        Object background = PKG.get("background");
        if (background != null) {
            drawBackground(background);
        }
        
        // Chamar função paint personalizada se existir
        Object paintFunc = PKG.get("paint");
        if (paintFunc instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(graphicsBuffer);
                ((Lua.LuaFunction)paintFunc).call(args);
            } catch (Exception e) {
                midlet.print("Error in paint function: " + e.getMessage(), midlet.stdout);
            }
        }
        
        // Copiar buffer para a tela
        g.drawImage(bufferImage, 0, 0, Graphics.TOP | Graphics.LEFT);
    }
    
    protected void keyPressed(int keyCode) {
        Object keyFunc = PKG.get("keyPressed");
        if (keyFunc instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(keyCode));
                ((Lua.LuaFunction)keyFunc).call(args);
                repaint();
            } catch (Exception e) {
                midlet.print("Error in keyPressed function: " + e.getMessage(), midlet.stdout);
            }
        }
    }
    
    protected void pointerPressed(int x, int y) {
        Object pointerFunc = PKG.get("pointerPressed");
        if (pointerFunc instanceof Lua.LuaFunction) {
            try {
                Vector args = new Vector();
                args.addElement(new Double(x));
                args.addElement(new Double(y));
                ((Lua.LuaFunction)pointerFunc).call(args);
                repaint();
            } catch (Exception e) {
                midlet.print("Error in pointerPressed function: " + e.getMessage(), midlet.stdout);
            }
        }
    }
    
    public void commandAction(Command c, Displayable d) {
        if (c == BACK) {
            Object backObj = PKG.get("back");
            if (backObj instanceof Hashtable) {
                Hashtable backTable = (Hashtable) backObj;
                Object backFunc = backTable.get("root");
                if (backFunc instanceof Lua.LuaFunction) {
                    try {
                        ((Lua.LuaFunction)backFunc).call(new Vector());
                    } catch (Exception e) {
                        midlet.print("Error in back function: " + e.getMessage(), midlet.stdout);
                    }
                }
            }
            midlet.processCommand("xterm", true, id);
        } else if (c == USER) {
            Object buttonObj = PKG.get("button");
            if (buttonObj instanceof Hashtable) {
                Hashtable buttonTable = (Hashtable) buttonObj;
                Object buttonFunc = buttonTable.get("root");
                if (buttonFunc instanceof Lua.LuaFunction) {
                    try {
                        ((Lua.LuaFunction)buttonFunc).call(new Vector());
                    } catch (Exception e) {
                        midlet.print("Error in button function: " + e.getMessage(), midlet.stdout);
                    }
                }
            }
        }
    }
    
    private void drawBackground(Object background) {
        if (background instanceof Image) {
            graphicsBuffer.drawImage((Image)background, 0, 0, Graphics.TOP | Graphics.LEFT);
        } else if (background instanceof Hashtable) {
            Hashtable bgTable = (Hashtable) background;
            Object rObj = bgTable.get("r"), gObj = bgTable.get("g"), bObj = bgTable.get("b");
            if (rObj instanceof Double && gObj instanceof Double && bObj instanceof Double) {
                int r = ((Double)rObj).intValue();
                int g = ((Double)gObj).intValue();
                int b = ((Double)bObj).intValue();
                graphicsBuffer.setColor(r, g, b);
                graphicsBuffer.fillRect(0, 0, getWidth(), getHeight());
            }
        }
    }
    
    private String getValue(Hashtable table, String key, String fallback) {
        Object value = table.get(key);
        return value != null ? value.toString() : fallback;
    }
    
    // Método para obter o Graphics para desenho em tempo real
    public Graphics getGraphicsBuffer() {
        return graphicsBuffer;
    }
    
    // Método para forçar repaint
    public void refresh() {
        repaint();
    }
}
// |
// EOF