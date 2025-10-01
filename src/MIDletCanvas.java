import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
class MIDletCanvas extends Canvas implements CommandListener { 
    private OpenTTY midlet;
    private Hashtable PKG; 
    private Graphics screen; 
    private Command BACK, USER; 
    private Image CURSOR = null; 
    private Vector fields = new Vector(); 
    private boolean root = false;
    private final int cursorSize = 5; 

    public MIDletCanvas(OpenTTY midlet, String code, boolean root) { 
        if (code == null || code.length() == 0) { return; } 

        this.PKG = midlet.parseProperties(code); this.midlet = midlet; this.root = root;
        setTitle(getenv("canvas.title", midlet.form.getTitle())); 

        addCommand(BACK = new Command(getenv("canvas.back.label", "Back"), Command.OK, 1)); 
        if (PKG.containsKey("canvas.button")) { addCommand(USER = new Command(getenv("canvas.button"), Command.SCREEN, 2)); } 
        if (PKG.containsKey("canvas.mouse")) { 
            try { 
                String[] pos = midlet.split(getenv("canvas.mouse"), ','); 

                midlet.cursorX = Integer.parseInt(pos[0]); 
                midlet.cursorY = Integer.parseInt(pos[1]); 
            } 
            catch (NumberFormatException e) { midlet.MIDletLogs("add warn Invalid value for 'canvas.mouse' - (x,y) may be a int number"); midlet.cursorX = 10; midlet.cursorY = 10; } 
        } 
        if (PKG.containsKey("canvas.mouse.img")) { 
            try { CURSOR = Image.createImage(getenv("canvas.mouse.img")); } 
            catch (Exception e) { midlet.MIDletLogs("add warn Malformed Cursor '" + getenv("canvas.mouse.img") + "'"); } 
        } 
        if (PKG.containsKey("canvas.fields")) { 
            String[] names = midlet.split(getenv("canvas.fields"), ','); 

            for (int i = 0; i < names.length; i++) { 
                String id = names[i].trim(), type = getenv("canvas." + id + ".type", "text"); 
                int x = Integer.parseInt(getenv("canvas." + id + ".x", "0")), y = Integer.parseInt(getenv("canvas." + id + ".y", "0")), w = Integer.parseInt(getenv("canvas." + id + ".w", "0")), h = Integer.parseInt(getenv("canvas." + id + ".h", "0")); 

                Hashtable field = new Hashtable(); 
                field.put("type", type); field.put("x", new Integer(x)); field.put("y", new Integer(y)); field.put("w", new Integer(w)); field.put("h", new Integer(h)); 
                field.put("value", getenv("canvas." + id + ".value", "")); 
                field.put("style", getenv("canvas." + id + ".style", "default")); 
                field.put("cmd", getenv("canvas." + id + ".cmd", "")); 

                fields.addElement(field); 
            } 
        } 

        setFullScreenMode(getenv("canvas.fullscreen", "false").equals("true"));
        setCommandListener(this); 
    } 
    public MIDletCanvas(OpenTTY midlet, Hashtable PKG, boolean root) {
        this.PKG = PKG; this.midlet = midlet; this.root = root;

        setTitle(getenv("title", midlet.form.getTitle()));

        Object backObj = PKG.get("back"), buttonObj = PKG.get("button");
        Hashtable backTable = (backObj instanceof Hashtable) ? (Hashtable) backObj : null, buttonTable = (buttonObj instanceof Hashtable) ? (Hashtable) buttonObj : null;
        addCommand(BACK = new Command(backTable != null ? getenv(backTable, "label", "Back") : "Back", Command.OK, 1));

        if (buttonTable != null) { addCommand(USER = new Command(getenv(buttonTable, "label", "Menu"), Command.SCREEN, 2)); }

        if (PKG.containsKey("mouse")) {
            Hashtable mouse = (PKG.get("mouse") instanceof Hashtable) ? (Hashtable) PKG.get("mouse") : null;

            if (mouse == null) { }
            else if (mouse.containsKey("x")) { midlet.cursorX = (mouse.get("x") instanceof Double ? (Double) mouse.get("x") : new Double(10)).intValue(); }
            else if (mouse.containsKey("y")) { midlet.cursorY = (mouse.get("y") instanceof Double ? (Double) mouse.get("y") : new Double(10)).intValue(); }
            else if (mouse.containsKey("img")) { CURSOR = mouse.get("img") instanceof Image ? (Image) mouse.get("img") : midlet.readImg(mouse.get("img").toString()); }
        }


        Object fieldsObj = PKG.get("fields");
        if (fieldsObj != null) {
            if (fieldsObj instanceof String) { } 
            else if (fieldsObj instanceof Hashtable) {
                Hashtable flds = (Hashtable) flds;
                for (Enumeration keys = flds.keys(); keys.hasMoreElements();) {
                    Object f = flds.get(keys.nextElement());
                    if (f instanceof Hashtable) fields.addElement((Hashtable) f);
                }
            }
        }

        setFullScreenMode(getenv("fullscreen", "false").equals("true"));
        setCommandListener(this);
    }

    protected void paint(Graphics g) { 
        if (screen == null) { screen = g; } 

        g.setColor(0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); 

        if (PKG.containsKey("canvas.background")) { 
            String backgroundType = getenv("canvas.background.type", "default"); 

            if (backgroundType.equals("color") || backgroundType.equals("default")) { setpallete("background", g, 0, 0, 0); g.fillRect(0, 0, getWidth(), getHeight()); } 
            else if (backgroundType.equals("image")) { 
                try { 
                    Image content = midlet.readImg(getenv("canvas.background")); 

                    g.drawImage(content, (getWidth() - content.getWidth()) / 2, (getHeight() - content.getHeight()) / 2, Graphics.TOP | Graphics.LEFT); 
                } 
                catch (Exception e) { midlet.processCommand("xterm"); midlet.processCommand("execute log add error Malformed Image, " + midlet.getCatch(e)); } 
            } 
        } 
        if (PKG.containsKey("canvas.fields")) { 
            for (int i = 0; i < fields.size(); i++) { 
                Hashtable f = (Hashtable) fields.elementAt(i); 

                String type = (String) f.get("type"), val = (String) f.get("value"); 
                int x = ((Integer) f.get("x")).intValue(), y = ((Integer) f.get("y")).intValue(), w = ((Integer) f.get("w")).intValue(), h = ((Integer) f.get("h")).intValue(); 

                if (type.equals("text")) { 
                    setpallete("text.color", g, 255, 255, 255); 
                    g.setFont(midlet.newFont((String) f.get("style"))); 

                    g.drawString(val, x, y, Graphics.TOP | Graphics.LEFT); 
                } 
                else if (type.equals("image")) { 
                    try { 
                        Image IMG = midlet.readImg(val); 

                        g.drawImage(IMG, x, y, Graphics.TOP | Graphics.LEFT); 

                        if (w == 0) { f.put("w", new Integer(IMG.getWidth())); } 
                        if (h == 0) { f.put("h", new Integer(IMG.getHeight())); } 
                    } 
                    catch (Exception e) { midlet.MIDletLogs("add error Malformed Image, " + midlet.getCatch(e)); } 
                } 
                else if (type.equals("rect")) { setpallete("rect.color", g, 0, 0, 255); g.drawRect(x, y, w, h); } 
                else if (type.equals("circle")) { setpallete("circle.color", g, 0, 255, 0); g.drawArc(x - w, y - w, w * 2, w * 2, 0, 360); } 
                else if (type.equals("line")) { setpallete("line.color", g, 255, 255, 255); g.drawLine(x, y, w, h); } 
            } 
        } 

        if (CURSOR != null) { g.drawImage(CURSOR, midlet.cursorX, midlet.cursorY, Graphics.TOP | Graphics.LEFT); } 
        else { setpallete("mouse.color", g, 255, 255, 255); g.fillRect(midlet.cursorX, midlet.cursorY, cursorSize, cursorSize); } 
    } 
    protected void keyPressed(int keyCode) { 
        int gameAction = getGameAction(keyCode); 

        if (gameAction == LEFT) { midlet.cursorX = Math.max(0, midlet.cursorX - 5); } 
        else if (gameAction == RIGHT) { midlet.cursorX = Math.min(getWidth() - cursorSize, midlet.cursorX + 5); } 
        else if (gameAction == UP) { midlet.cursorY = Math.max(0, midlet.cursorY - 5); } 
        else if (gameAction == DOWN) { midlet.cursorY = Math.min(getHeight() - cursorSize, midlet.cursorY + 5); } 
        else if (gameAction == FIRE) { 
            for (int i = 0; i < fields.size(); i++) { 
                Hashtable f = (Hashtable) fields.elementAt(i); 

                int x = ((Integer) f.get("x")).intValue(), y = ((Integer) f.get("y")).intValue(), w = ((Integer) f.get("w")).intValue(), h = ((Integer) f.get("h")).intValue(); 
                String type = (String) f.get("type"), cmd = (String) f.get("cmd"), val = (String) f.get("value"); 

                if (cmd != null && !cmd.equals("")) { 
                    boolean hit = false; 

                    if (type.equals("circle")) { int dx = midlet.cursorX - x, dy = midlet.cursorY - y; hit = (dx * dx + dy * dy) <= (w * w); } 
                    else if (type.equals("text")) { 
                        Font font = midlet.newFont(getenv((String) f.get("style"), "default")); 

                        int textW = font.stringWidth(val), textH = font.getHeight(); 
                        hit = midlet.cursorX + cursorSize > x && midlet.cursorX < x + textW && midlet.cursorY + cursorSize > y && midlet.cursorY < y + textH; 
                    } 
                    else if (type.equals("line")) { continue; } 
                    else { hit = midlet.cursorX + cursorSize > x && midlet.cursorX < x + w && midlet.cursorY + cursorSize > y && midlet.cursorY < y + h; } 

                    if (hit) { midlet.processCommand(cmd, true, root); break; } 
                } 
            } 
        } 

        repaint(); 
    }
    protected void pointerPressed(int x, int y) { midlet.cursorX = x; midlet.cursorY = y; keyPressed(-5); } 

    public void commandAction(Command c, Displayable d) { 
        midlet.processCommand("xterm", true, root); 
        if (c == BACK) { midlet.processCommand(getvalue("canvas.back", "true"), true, root); } 
        else if (c == USER) { midlet.processCommand(getvalue("canvas.button.cmd", "log add warn An error occurred, 'canvas.button.cmd' not found"), true, root); } 
    } 

    private void setpallete(String node, Graphics screen, int r, int g, int b) { 
        try { 
            String[] pallete = midlet.split(getenv("canvas." + node, "" + r + "," + g + "," + b), ','); 
            screen.setColor(Integer.parseInt(pallete[0]), Integer.parseInt(pallete[1]), Integer.parseInt(pallete[2])); 
        } 
        catch (NumberFormatException e) { midlet.MIDletLogs("add warn Invalid value for 'canvas." + node + "' - (r,g,b) may be a int number"); } 
    } 

    private String getvalue(Hashtable fields, String key, String fallback) { return fields.containsKey(key) ? (String) fields.get(key) : fallback; } 
    private String getenv(Hashtable fields, String key, String fallback) { return midlet.env(getvalue(fields, key, fallback)); } 
    private String getenv(Hashtable fields, String key) { return midlet.env(getvalue(fields, key, "")); } 

    private String getvalue(String key, String fallback) { return PKG.containsKey(key) ? (String) PKG.get(key) : fallback; } 
    private String getenv(String key, String fallback) { return midlet.env(getvalue(key, fallback)); } 
    private String getenv(String key) { return midlet.env(getvalue(key, "")); } 
}
// |
// EOF