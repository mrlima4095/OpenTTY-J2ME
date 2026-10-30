import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.media.control.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// Lua Runtime
public class Lua {
    public boolean breakLoop = false, doreturn = false, kill = true, gc = true;
    // > 0 while the right operand of a short-circuited `and`/`or` is parsed:
    // the tokens are consumed (so positions stay exact) but nothing runs.
    private int suppress = 0;
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0;
    public Hashtable globals = new Hashtable(), father, requireCache = new Hashtable(), labels = new Hashtable();
    public Vector tokens;
    public String currentSource = "";
    public Vector frameStack = new Vector();
    public Vector thrownFrames = new Vector();
    public String lastCode = "";
    public Vector lineOffsets = new Vector();
    public Vector thrownTokens = null;
    public int thrownTokenIndex = -1;
    public String thrownSource = "";
    public String thrownCode = "";
    public Vector thrownLineOffsets = null;
    // |
    public int status = 0;
    public boolean silent = false;
    // | (Interpreter ownership)
    // The script state above is shared by every function of this Lua instance,
    // so only one thread may execute it at a time. UI callbacks (LCDUI event
    // thread) and background jobs (java.run) both call into it; java.yield()
    // releases ownership so the other side can run between steps.
    private Thread luaOwner = null;
    private int luaDepth = 0;
    // | (LuaFunction)
    public static final int PRINT = 0, ERROR = 1, PCALL = 2, REQUIRE = 3, LOADS = 4, PAIRS = 5, GC = 6, TOSTRING = 7, TONUMBER = 8, SELECT = 9, TYPE = 10, GETPROPERTY = 11, SETMETATABLE = 12, GETMETATABLE = 13, IPAIRS = 14, RANDOM = 15, ASSERT = 16, RAWGET = 17, RAWSET = 18, RAWEQUAL = 19, RAWLEN = 20;
    public static final int DEBUG_TRACEBACK = 1100, DEBUG_GETINFO = 1101, DEBUG_GETREGISTRY = 1102;
    public static final int UPPER = 100, LOWER = 101, LEN = 102, FIND = 103, MATCH = 104, REVERSE = 105, SUB = 106, HASH = 107, BYTE = 108, CHAR = 109, TRIM = 110, SPLIT = 111, UUID = 112, GETCMD = 113, GETARGS = 114, ENV = 115, BASE64_ENCODE = 116, BASE64_DECODE = 117, GETPATTERN = 118, STARTSWITH = 119, ENDSWITH = 120;
    public static final int TB_INSERT = 200, TB_CONCAT = 201, TB_REMOVE = 202, TB_SORT = 203, TB_MOVE = 204, TB_UNPACK = 205, TB_PACK = 206, TB_DECODE = 207;
    public static final int EXEC = 300, GETENV = 301, SETENV = 302, CLOCK = 303, SETLOC = 304, EXIT = 305, DATE = 306, GETPID = 307, SETPROC = 308, GETPROC = 309, GETCWD = 310, GETUID = 311, CHDIR = 312, REQUEST = 313, START = 314, STOP = 315, PREQ = 316, SU = 318, REMOVE = 319, SCOPE = 320, JOIN = 321, MKDIR = 322;
    public static final int READ = 400, WRITE = 401, CLOSE = 402, OPEN = 403, POPEN = 404, DIRS = 405, SETOUT = 406, MOUNT = 407, GEN = 408, COPY = 409;
    public static final int HTTP_GET = 500, HTTP_POST = 501, CONNECT = 502, PEER = 503, DEVICE = 504, SERVER = 505, ACCEPT = 506, HTTP_RGET = 507, HTTP_RPOST = 508;
    public static final int DISPLAY = 600, NEW = 601, RENDER = 602, APPEND = 603, ADDCMD = 604, HANDLER = 605, GETCURRENT = 606, TITLE = 607, TICKER = 608, VIBRATE = 609, SETLABEL = 610, SETTEXT = 611, GETLABEL = 612, GETTEXT = 613, CLEAR_SCREEN = 614, TASKMNGR = 615, CANVAS = 616, CANVAS_ASSIGN = 617, SETINDICATOR = 618, SETGAUGE = 619, GETGAUGE = 620;
    public static final int CLASS = 700, NAME = 701, DELETE = 702, UPTIME = 703, RUN = 704, THREAD = 705, SLEEP = 706, YIELD = 707, KERNEL = 1000;
    public static final int AUDIO_LOAD = 800, AUDIO_PLAY = 801, AUDIO_PAUSE = 802, AUDIO_VOLUME = 803, AUDIO_DURATION = 804, AUDIO_TIME = 805;
    public static final int PUSH_REGISTER = 900, PUSH_UNREGISTER = 901, PUSH_LIST = 902, PUSH_PENDING = 903, PUSH_SET_ALARM = 904;

    public static final int EOF = 0, NUMBER = 1, STRING = 2, BOOLEAN = 3, NIL = 4, IDENTIFIER = 5, PLUS = 6, MINUS = 7, MULTIPLY = 8, DIVIDE = 9, MODULO = 10, EQ = 11, NE = 12, LT = 13, GT = 14, LE = 15, GE = 16, AND = 17, OR = 18, NOT = 19, ASSIGN = 20, IF = 21, THEN = 22, ELSE = 23, END = 24, WHILE = 25, DO = 26, RETURN = 27, FUNCTION = 28, LPAREN = 29, RPAREN = 30, COMMA = 31, LOCAL = 32, LBRACE = 33, RBRACE = 34, LBRACKET = 35, RBRACKET = 36, CONCAT = 37, DOT = 38, ELSEIF = 39, FOR = 40, IN = 41, POWER = 42, BREAK = 43, LENGTH = 44, VARARG = 45, REPEAT = 46, UNTIL = 47, COLON = 48, LABEL = 49, GOTO = 50;
    public static final Boolean TRUE = Boolean.TRUE, FALSE = Boolean.FALSE;
    public static final Object LUA_NIL = new Object();
    // | (Cached Double for small integers — avoids per-iteration boxing of counters/indices)
    private static final int NUM_MIN = -128, NUM_MAX = 1023;
    private static final Double[] SMALL_NUMBERS = new Double[NUM_MAX - NUM_MIN + 1];
    static { for (int i = NUM_MIN; i <= NUM_MAX; i++) { SMALL_NUMBERS[i - NUM_MIN] = new Double(i); } }
    private static Double luaNumber(double d) { if (d == Math.floor(d) && d >= NUM_MIN && d <= NUM_MAX) { return SMALL_NUMBERS[(int) d - NUM_MIN]; } return new Double(d); }
    // Accepts decimal text and Lua 5.1 style hexadecimal literals ("0x1F", "-0x10").
    static double parseLuaNumber(String text) {
        String s = text == null ? "" : text.trim();
        boolean negative = s.startsWith("-");
        String digits = negative || s.startsWith("+") ? s.substring(1) : s;
        if (digits.length() > 2 && digits.charAt(0) == '0' && (digits.charAt(1) == 'x' || digits.charAt(1) == 'X')) {
            double value = (double) Long.parseLong(digits.substring(2), 16);
            return negative ? -value : value;
        }
        return Double.parseDouble(s);
    }
    // |
    public static class Token { int type; Object value; int offset; Token(int type, Object value) { this.type = type; this.value = value; this.offset = -1; } Token(int type, Object value, int offset) { this.type = type; this.value = value; this.offset = offset; } public String toString() { return "Token(type=" + type + ", value=" + value + ")"; } }
    // |
    public static class Frame { public String name; public String source; public int line; Frame(String n, String s, int l) { name = n; source = s; line = l; } }
    // | (Chained scope: avoids copying closure/globals into a fresh table per call)
    public static class ScopeTable extends Hashtable {
        Hashtable parent, globals;
        ScopeTable(Hashtable parent, Hashtable globals) { this.parent = parent; this.globals = globals; }
        public synchronized Object get(Object key) {
            Object v = super.get(key);
            if (v != null) { return v; }
            if (parent != null) { v = parent.get(key); if (v != null) { return v; } }
            if (globals != null) { v = globals.get(key); if (v != null) { return v; } }
            return null;
        }
        // | (True only when this frame declares the name; used by assignment)
        boolean hasOwn(Object key) { return super.containsKey(key); }
    }
    // |
    // Main
    public Lua(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; this.id = id; this.PID = pid; this.proc = proc; this.stdout = stdout; this.father = scope;
        this.tokenIndex = 0; 

        Hashtable os = new Hashtable(), io = new Hashtable(), string = new Hashtable(), table = new Hashtable(), pkg = new Hashtable(), graphics = new Hashtable(), socket = new Hashtable(), http = new Hashtable(), java = new Hashtable(), jdb = new Hashtable(), math = new Hashtable(), audio = new Hashtable(), push = new Hashtable(), base64 = new Hashtable(), debug = new Hashtable();
        String[] funcs = new String[] { "getenv", "setenv", "clock", "setlocale", "exit", "date", "getpid", "setproc", "getproc", "getcwd", "request", "getuid", "chdir", "open", "su", "remove", "scope", "join", "mkdir" }; 
        int[] loaders = new int[] { GETENV, SETENV, CLOCK, SETLOC, EXIT, DATE, GETPID, SETPROC, GETPROC, GETCWD, REQUEST, GETUID, CHDIR, PREQ, SU, REMOVE, SCOPE, JOIN, MKDIR };
        for (int i = 0; i < funcs.length; i++) { os.put(funcs[i], new LuaFunction(loaders[i])); } os.put("execute", midlet.shell instanceof LuaFunction ? midlet.shell : new LuaFunction(EXEC)); globals.put("os", os);

        funcs = new String[] { "read", "write", "close", "open", "popen", "dirs", "setstdout", "mount", "new", "copy" }; loaders = new int[] { READ, WRITE, CLOSE, OPEN, POPEN, DIRS, SETOUT, MOUNT, GEN, COPY };
        for (int i = 0; i < funcs.length; i++) { io.put(funcs[i], new LuaFunction(loaders[i])); } io.put("stdout", stdout); globals.put("io", io);

        funcs = new String[] { "insert", "concat", "remove", "sort", "move", "unpack", "pack", "decode" }; loaders = new int[] { TB_INSERT, TB_CONCAT, TB_REMOVE, TB_SORT, TB_MOVE, TB_UNPACK, TB_PACK, TB_DECODE };
        for (int i = 0; i < funcs.length; i++) { table.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("table", table);

        funcs = new String[] { "load", "play", "pause", "volume", "duration", "time" }; loaders = new int[] { AUDIO_LOAD, AUDIO_PLAY, AUDIO_PAUSE, AUDIO_VOLUME, AUDIO_DURATION, AUDIO_TIME };
        for (int i = 0; i < funcs.length; i++) { audio.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("audio", audio);

        funcs = new String[] { "encode", "decode" }; loaders = new int[] { BASE64_ENCODE, BASE64_DECODE };
        for (int i = 0; i < funcs.length; i++) { base64.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("base64", base64);

        funcs = new String[] { "get", "post", "rget", "rpost" }; loaders = new int[] { HTTP_GET, HTTP_POST, HTTP_RGET, HTTP_RPOST };
        for (int i = 0; i < funcs.length; i++) { http.put(funcs[i], new LuaFunction(loaders[i])); } socket.put("http", http);

        funcs = new String[] { "class", "getName", "delete", "run", "thread", "sleep", "yield" }; loaders = new int[] { CLASS, NAME, DELETE, RUN, THREAD, SLEEP, YIELD };
        for (int i = 0; i < funcs.length; i++) { java.put(funcs[i], new LuaFunction(loaders[i])); } jdb.put("username", midlet.username); jdb.put("build", midlet.build); jdb.put("lite", OpenTTY.ELF_LITE ? Boolean.TRUE : Boolean.FALSE); jdb.put("uptime", new LuaFunction(UPTIME)); java.put("midlet", jdb); globals.put("java", java);

        funcs = new String[] { "connect", "peer", "device", "server", "accept" }; loaders = new int[] { CONNECT, PEER, DEVICE, SERVER, ACCEPT };
        for (int i = 0; i < funcs.length; i++) { socket.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("socket", socket);

        funcs = new String[] { "register", "unregister", "list", "pending", "setAlarm" }; loaders = new int[] { PUSH_REGISTER, PUSH_UNREGISTER, PUSH_LIST, PUSH_PENDING, PUSH_SET_ALARM };
        for (int i = 0; i< funcs.length; i++) { push.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("push", push);

        funcs = new String[] { "display", "new", "render", "append", "addCommand", "handler", "getCurrent", "SetTitle", "SetTicker", "vibrate", "SetLabel", "SetText", "GetLabel", "GetText", "clear", "taskmngr", "SetIndicator", "SetGauge", "GetGauge" }; 
        loaders = new int[] { DISPLAY, NEW, RENDER, APPEND, ADDCMD, HANDLER, GETCURRENT, TITLE, TICKER, VIBRATE, SETLABEL, SETTEXT, GETLABEL, GETTEXT, CLEAR_SCREEN, TASKMNGR, SETINDICATOR, SETGAUGE, GETGAUGE };
        for (int i = 0; i < funcs.length; i++) { graphics.put(funcs[i], new LuaFunction(loaders[i])); } graphics.put("db", midlet.graphics); graphics.put("fire", List.SELECT_COMMAND); graphics.put("CONTINUOUS_IDLE", new Double(Gauge.CONTINUOUS_IDLE)); graphics.put("INCREMENTAL_IDLE", new Double(Gauge.INCREMENTAL_IDLE)); graphics.put("CONTINUOUS_RUNNING", new Double(Gauge.CONTINUOUS_RUNNING)); graphics.put("INCREMENTAL_UPDATING", new Double(Gauge.INCREMENTAL_UPDATING)); globals.put("graphics", graphics);

        // Package [graphics.canvas] - free-form drawing surface (see LuaCanvas).
        // Every entry is a native tagged with its method name; the canvas table
        // doubles as the __index of each canvas handle, so `c:text(...)` and
        // `graphics.canvas.text(c, ...)` are the same call.
        funcs = new String[] { "new", "rgb", "on", "off", "display", "close", "title", "fullscreen", "size", "repaint", "flush", "clear",
            "color", "background", "foreground", "font", "metrics", "textWidth", "charWidth", "text", "print", "nl", "backspace", "cursor", "caret",
            "line", "rect", "roundRect", "arc", "circle", "ellipse", "triangle", "polygon", "pixel", "image",
            "clip", "resetClip", "clipRect", "translate", "save", "restore", "reset",
            "keyDown", "keyName", "gameAction", "keys", "resetKeys", "waitKey", "pointer",
            "loop", "animate", "stop", "running", "age", "command", "clearCommands" };
        Hashtable canvas = new Hashtable();
        for (int i = 0; i < funcs.length; i++) { LuaFunction method = new LuaFunction(CANVAS); method.nativeName = funcs[i]; canvas.put(funcs[i], method); }

        Hashtable anchor = new Hashtable(), key = new Hashtable(), action = new Hashtable(), fontTable = new Hashtable(), colors = new Hashtable();
        anchor.put("LEFT", luaNumber(Graphics.LEFT)); anchor.put("RIGHT", luaNumber(Graphics.RIGHT)); anchor.put("HCENTER", luaNumber(Graphics.HCENTER));
        anchor.put("TOP", luaNumber(Graphics.TOP)); anchor.put("BOTTOM", luaNumber(Graphics.BOTTOM)); anchor.put("VCENTER", luaNumber(Graphics.VCENTER)); anchor.put("BASELINE", luaNumber(Graphics.BASELINE));
        anchor.put("CENTER", luaNumber(Graphics.HCENTER | Graphics.VCENTER));
        key.put("UP", luaNumber(Canvas.UP)); key.put("DOWN", luaNumber(Canvas.DOWN)); key.put("LEFT", luaNumber(Canvas.LEFT)); key.put("RIGHT", luaNumber(Canvas.RIGHT));
        key.put("FIRE", luaNumber(Canvas.FIRE)); key.put("GAME_A", luaNumber(Canvas.GAME_A)); key.put("GAME_B", luaNumber(Canvas.GAME_B)); key.put("GAME_C", luaNumber(Canvas.GAME_C)); key.put("GAME_D", luaNumber(Canvas.GAME_D));
        key.put("NUM0", luaNumber(Canvas.KEY_NUM0)); key.put("NUM1", luaNumber(Canvas.KEY_NUM1)); key.put("NUM2", luaNumber(Canvas.KEY_NUM2)); key.put("NUM3", luaNumber(Canvas.KEY_NUM3));
        key.put("NUM4", luaNumber(Canvas.KEY_NUM4)); key.put("NUM5", luaNumber(Canvas.KEY_NUM5)); key.put("NUM6", luaNumber(Canvas.KEY_NUM6)); key.put("NUM7", luaNumber(Canvas.KEY_NUM7));
        key.put("NUM8", luaNumber(Canvas.KEY_NUM8)); key.put("NUM9", luaNumber(Canvas.KEY_NUM9)); key.put("STAR", luaNumber(Canvas.KEY_STAR)); key.put("POUND", luaNumber(Canvas.KEY_POUND));
        action.put("UP", luaNumber(Canvas.UP)); action.put("DOWN", luaNumber(Canvas.DOWN)); action.put("LEFT", luaNumber(Canvas.LEFT)); action.put("RIGHT", luaNumber(Canvas.RIGHT));
        action.put("FIRE", luaNumber(Canvas.FIRE)); action.put("GAME_A", luaNumber(Canvas.GAME_A)); action.put("GAME_B", luaNumber(Canvas.GAME_B)); action.put("GAME_C", luaNumber(Canvas.GAME_C)); action.put("GAME_D", luaNumber(Canvas.GAME_D));
        fontTable.put("SYSTEM", luaNumber(Font.FACE_SYSTEM)); fontTable.put("MONOSPACE", luaNumber(Font.FACE_MONOSPACE)); fontTable.put("PROPORTIONAL", luaNumber(Font.FACE_PROPORTIONAL));
        fontTable.put("PLAIN", luaNumber(Font.STYLE_PLAIN)); fontTable.put("BOLD", luaNumber(Font.STYLE_BOLD)); fontTable.put("ITALIC", luaNumber(Font.STYLE_ITALIC)); fontTable.put("UNDERLINED", luaNumber(Font.STYLE_UNDERLINED));
        fontTable.put("SMALL", luaNumber(Font.SIZE_SMALL)); fontTable.put("MEDIUM", luaNumber(Font.SIZE_MEDIUM)); fontTable.put("LARGE", luaNumber(Font.SIZE_LARGE));
        String[] colorNames = { "black", "white", "red", "green", "blue", "yellow", "cyan", "magenta", "gray", "grey", "orange", "purple", "brown", "pink", "lime", "navy", "teal", "silver", "maroon", "olive" };
        for (int i = 0; i < colorNames.length; i++) { colors.put(colorNames[i], luaNumber(LuaCanvas.namedColor(colorNames[i]))); }
        canvas.put("anchor", anchor); canvas.put("key", key); canvas.put("action", action); canvas.put("fonts", fontTable); canvas.put("colors", colors);
        graphics.put("canvas", canvas);

        funcs = new String[] { "upper", "lower", "len", "find", "match", "reverse", "sub", "hash", "byte", "char", "trim", "uuid", "split", "getCommand", "getArgument", "env", "getpattern", "startswith", "endswith" }; loaders = new int[] { UPPER, LOWER, LEN, FIND, MATCH, REVERSE, SUB, HASH, BYTE, CHAR, TRIM, UUID, SPLIT, GETCMD, GETARGS, ENV, GETPATTERN, STARTSWITH, ENDSWITH };
        for (int i = 0; i < funcs.length; i++) { string.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("string", string);

        funcs = new String[] { "print", "error", "pcall", "assert", "require", "load", "pairs", "ipairs", "collectgarbage", "tostring", "tonumber", "select", "type", "getAppProperty", "setmetatable", "getmetatable", "rawget", "rawset", "rawequal", "rawlen" }; 
        loaders = new int[] { PRINT, ERROR, PCALL, ASSERT, REQUIRE, LOADS, PAIRS, IPAIRS, GC, TOSTRING, TONUMBER, SELECT, TYPE, GETPROPERTY, SETMETATABLE, GETMETATABLE, RAWGET, RAWSET, RAWEQUAL, RAWLEN };
        for (int i = 0; i < funcs.length; i++) { globals.put(funcs[i], new LuaFunction(loaders[i])); }

        funcs = new String[] { "traceback", "getinfo", "getregistry" };
        loaders = new int[] { DEBUG_TRACEBACK, DEBUG_GETINFO, DEBUG_GETREGISTRY };
        for (int i = 0; i < funcs.length; i++) { debug.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("debug", debug);

        pkg.put("loaded", requireCache); pkg.put("loadlib", new LuaFunction(REQUIRE)); globals.put("package", pkg);
        math.put("random", new LuaFunction(RANDOM)); globals.put("math", math);
        globals.put("_VERSION", "Lua J2ME"); globals.put("_G", globals);
    }
    // | (Run Source code)
    public Hashtable run(String source, String code, Hashtable args) { 
        midlet.sys.put(PID, proc); globals.put("arg", args);

        currentSource = source == null ? "" : source;
        lastCode = code == null ? "" : code;
        lineOffsets = computeLineOffsets(lastCode);
        frameStack.removeAllElements();
        clearThrown();
        silent = false;

        Hashtable ITEM = new Hashtable(); 
        
        try { 
            this.tokens = tokenize(code); collectLabels();
            
            while (peek().type != EOF) { Object res = statement(globals); if (doreturn) { if (res != null) { ITEM.put("object", res); } doreturn = false; break; } }
        } 
        catch (Exception e) { recordThrow(); midlet.print(getTraceback(e), stdout, id, father); status = 1; } 
        catch (Error e) { if (!silent) { midlet.print(midlet.getCatch(e), stdout, id, father); status = 1; } }

        if (kill) { midlet.sys.remove(PID); }
        ITEM.put("status", status);
        return ITEM;
    }
    // |
    public void recordThrow() { recordThrow(tokenIndex, tokens); }
    public void recordThrow(int idx, Vector toks) {
        if (thrownTokenIndex != -1) { return; }
        thrownTokenIndex = idx; thrownTokens = toks;
        thrownSource = currentSource;
        thrownCode = lastCode;
        thrownLineOffsets = lineOffsets;
    }
    // |
    public void clearThrown() {
        thrownFrames.removeAllElements();
        thrownTokens = null; thrownTokenIndex = -1;
        thrownSource = ""; thrownCode = ""; thrownLineOffsets = null;
    }
    // |
    public static Vector computeLineOffsets(String code) {
        Vector v = new Vector();
        if (code == null) { return v; }
        v.addElement(new Integer(-1));
        for (int k = 0; k < code.length(); k++) { if (code.charAt(k) == '\n') { v.addElement(new Integer(k + 1)); } }
        return v;
    }
    // | (1-based line for a character offset, using cached \n offsets [binary search])
    public static int lineFromOffsets(Vector offs, int offset) {
        if (offs == null || offs.isEmpty()) { return -1; }
        int lo = 0, hi = offs.size() - 1, best = 0;
        while (lo <= hi) {
            int mid = (lo + hi) >> 1;
            int v = ((Integer) offs.elementAt(mid)).intValue();
            if (v <= offset) { best = mid; lo = mid + 1; } else { hi = mid - 1; }
        }
        return best + 1;
    }
    // | (1-based line for a char offset, linear scan fallback)
    public static int lineFromOffset(String code, int offset) {
        if (code == null || offset < 0 || offset > code.length()) { return -1; }
        int line = 1;
        for (int k = 0; k < offset; k++) { if (code.charAt(k) == '\n') { line++; } }
        return line;
    }
    // |
    // |
    // | (Call a Lua callback from a UI/loop thread, reporting errors on stdout)
    public Object invokeCallback(Object fn, Vector args) {
        if (!(fn instanceof LuaFunction)) { return null; }
        try { return ((LuaFunction) fn).call(args); }
        catch (Exception e) { String traceback = getTraceback(e); clearThrown(); midlet.print(traceback, stdout, id, father); status = 1; }
        catch (Error e) { if (!silent) { midlet.print(midlet.getCatch(e), stdout, id, father); } }
        return null;
    }
    public String getTraceback(Throwable e) {
        StringBuffer sb = new StringBuffer(midlet.getCatch(e));

        String src = thrownSource != null ? thrownSource : currentSource;
        String code = thrownCode != null ? thrownCode : lastCode;
        Vector offs = thrownLineOffsets != null ? thrownLineOffsets : lineOffsets;

        int line = -1; String lineText = null; int col = -1; String near = "";
        if (thrownTokenIndex >= 0 && thrownTokens != null && !thrownTokens.isEmpty()) {
            int t = thrownTokenIndex < thrownTokens.size() ? thrownTokenIndex : thrownTokens.size() - 1;
            Token tok = (Token) thrownTokens.elementAt(t);
            if (tok.offset >= 0 && code != null && tok.offset < code.length()) {
                line = lineFromOffsets(offs, tok.offset);
                int ls = tok.offset;
                while (ls > 0 && code.charAt(ls - 1) != '\n') { ls--; }
                int le = tok.offset;
                while (le < code.length() && code.charAt(le) != '\n') { le++; }
                lineText = code.substring(ls, le);
                col = tok.offset - ls;
                near = tokenLexeme(tok);
            }
        }

        if (src != null && src.length() > 0) {
            sb.append("\nLua ").append(src);
            if (line > 0) { sb.append(':').append(line); }
        }
        sb.append(pointerBlock(lineText, col, near));
        if (thrownFrames.isEmpty()) { for (int f = 0; f < frameStack.size(); f++) { thrownFrames.addElement(frameStack.elementAt(f)); } }
        if (!thrownFrames.isEmpty()) {
            sb.append("\nstack traceback:");
            for (int i = thrownFrames.size() - 1; i >= 0; i--) {
                Frame f = (Frame) thrownFrames.elementAt(i);
                sb.append("\n\t").append(f.source != null && f.source.length() > 0 ? f.source : "?");
                if (f.line > 0) { sb.append(':').append(f.line); }
                sb.append(": in function '").append(f.name == null ? "[anonymous]" : f.name).append("'");
            }
        }
        clearThrown();
        return sb.toString();
    }
    // |
    public static String pointerBlock(String lineText, int col, String near) {
        StringBuffer sb = new StringBuffer();
        if (lineText == null || lineText.trim().length() == 0) { return sb.toString(); }
        StringBuffer pad = new StringBuffer();
        int disp = 0;
        for (int k = 0; k < lineText.length(); k++) {
            if (k >= col) { break; }
            char c = lineText.charAt(k);
            if (c == '\t') { disp += 8 - (disp % 8); } else { disp++; }
        }
        for (int k = 0; k < disp; k++) { pad.append(' '); }
        sb.append("\n\t").append(lineText);
        sb.append("\n\t").append(pad).append('^');
        for (int k = col + 1; k < lineText.length(); k++) { sb.append('-'); }
        if (near != null && near.length() > 0) { sb.append(" (near '").append(near).append("')"); }
        return sb.toString();
    }
    // |
    public static String tokenLexeme(Token tok) {
        if (tok == null) { return ""; }
        Object v = tok.value;
        if (v instanceof Double) { double d = ((Double) v).doubleValue(); if (d == Math.floor(d) && !Double.isInfinite(d)) { return String.valueOf((long) d); } return String.valueOf(d); }
        return v == null ? "" : String.valueOf(v);
    }
    // |
    // | (Human readable value: honors __tostring, falls back to the type formatter)
    private Object toDisplayValue(Object value) throws Exception {
        if (hasMeta(value, "__tostring")) { return toLuaString(callMeta("__tostring", new Object[] { value })); }
        return toLuaString(value);
    }
    // | (Line of the token being evaluated, used to keep frame call sites fresh)
    private int currentLine() {
        if (tokens == null || tokenIndex <= 0 || tokenIndex > tokens.size()) { return -1; }
        Token tok = (Token) tokens.elementAt(tokenIndex - 1);
        if (tok == null || tok.offset < 0) { return -1; }
        return lineFromOffsets(lineOffsets, tok.offset);
    }
    // | (Stack dump for debug.traceback: innermost frame first, `level` frames skipped)
    private String tracebackString(String message, int level) {
        StringBuffer sb = new StringBuffer();
        if (message != null && message.length() > 0) { sb.append(message).append('\n'); }

        sb.append("stack traceback:");
        int skip = level - 1;
        if (skip >= frameStack.size()) { sb.append("\n\t[C]: in ?"); return sb.toString(); }

        for (int i = frameStack.size() - 1 - skip; i >= 0; i--) {
            Frame f = (Frame) frameStack.elementAt(i);

            sb.append("\n\t").append(f.source == null || f.source.length() == 0 ? "?" : f.source);
            if (f.line > 0) { sb.append(':').append(f.line); }
            sb.append(": in function '").append(f.name == null ? "[anonymous]" : f.name).append('\'');
        }
        return sb.toString();
    }
    // | (debug.getinfo: describe a function or a live stack frame)
    private void fillInfo(Hashtable info, LuaFunction func, int frameIndex) {
        if (func != null) {
            String source = func.defSource == null || func.defSource.length() == 0 ? "=[C]" : (func.defSource.startsWith("@") || func.defSource.startsWith("=") ? func.defSource : "@" + func.defSource);

            info.put("what", func.MOD == -1 ? "Lua" : "C");
            info.put("source", source);
            info.put("short_src", func.defSource == null || func.defSource.length() == 0 ? "[C]" : func.defSource);
            info.put("linedefined", luaNumber(func.MOD == -1 && func.defLine > 0 ? func.defLine : -1));
            info.put("lastlinedefined", luaNumber(func.MOD == -1 && func.defLine > 0 ? func.defLine : -1));
            info.put("nparams", luaNumber(func.params == null ? 0 : func.params.size()));
            info.put("name", func.name);
            info.put("currentline", luaNumber(-1));
            return;
        }

        Frame f = (Frame) frameStack.elementAt(frameIndex);
        String source = f.source == null || f.source.length() == 0 ? "=?" : (f.source.startsWith("@") || f.source.startsWith("=") ? f.source : "@" + f.source);

        info.put("what", "Lua");
        info.put("source", source);
        info.put("short_src", f.source == null || f.source.length() == 0 ? "?" : f.source);
        info.put("linedefined", luaNumber(-1));
        info.put("lastlinedefined", luaNumber(-1));
        info.put("nparams", luaNumber(0));
        info.put("name", f.name);
        info.put("currentline", luaNumber(f.line));
    }
    // |
    // Tokenizer
    public Vector tokenize(String code) throws Exception {
        if (midlet.cacheLua.containsKey(code)) { return (Vector) midlet.cacheLua.get(code); }

        Vector tokens = new Vector();
        int i = 0;
        if (code.startsWith("#!")) {
            while (i < code.length() && code.charAt(i) != '\n') { i++; }
            if (i < code.length() && code.charAt(i) == '\n') { i++; }
        }
        while (i < code.length()) {
            int start = i;
            char c = code.charAt(i);
    
            if (isWhitespace(c) || c == ';') { i++; }
            else if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '-') {
                i += 2;
                if (i + 1 < code.length() && code.charAt(i) == '[' && code.charAt(i + 1) == '[') {
                    i += 2;
                    while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) i++;
                    if (i + 1 < code.length()) i += 2;
                } 
                else { while (i < code.length() && code.charAt(i) != '\n') i++; }
                continue;
            }
    
            else if (c == '.') {
                if (i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') { tokens.addElement(new Token(VARARG, "...", start)); i += 3; } 
                else if (i + 1 < code.length() && code.charAt(i + 1) == '.') { tokens.addElement(new Token(CONCAT, "..", start)); i += 2; } 
                else { tokens.addElement(new Token(DOT, ".", start)); i++; }
            }
            else if (c == ':') {
                if (i + 1 < code.length() && code.charAt(i + 1) == ':') {
                    i += 2;
                    int nameStart = i;
                    while (i < code.length() && (isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) { i++; }
                    
                    if (i + 1 < code.length() && code.charAt(i) == ':' && code.charAt(i + 1) == ':') { i += 2; tokens.addElement(new Token(LABEL, code.substring(nameStart, i - 2), start)); }
                    else { i -= 2; tokens.addElement(new Token(COLON, ":", start)); i++; }
                } else { tokens.addElement(new Token(COLON, ":", start)); i++; }
            }

            else if (c == '0' && i + 1 < code.length() && (code.charAt(i + 1) == 'x' || code.charAt(i + 1) == 'X')) {
                i += 2;
                int hexStart = i;
                while (i < code.length() && isHexDigit(code.charAt(i))) { i++; }
                if (i == hexStart) { throw new RuntimeException("Malformed number near '" + code.substring(start, Math.min(i + 1, code.length())) + "'"); }
                try { double numValue = (double) Long.parseLong(code.substring(hexStart, i), 16); tokens.addElement(new Token(NUMBER, luaNumber(numValue), start)); }
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + code.substring(start, i) + "'"); }
                continue;
            }
            else if (isDigit(c) || (c == '.' && i + 1 < code.length() && isDigit(code.charAt(i + 1)))) {
                boolean hasDecimal = false;
                while (i < code.length() && (isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) { break; }
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') { break; }
                        hasDecimal = true;
                    }
                    i++;
                }
                try { double numValue = Double.parseDouble(code.substring(start, i)); tokens.addElement(new Token(NUMBER, luaNumber(numValue), start)); } 
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + code.substring(start, i) + "'"); }
                continue;
            }
            else if (c == '-' && i + 1 < code.length() && (isDigit(code.charAt(i + 1)) || (code.charAt(i + 1) == '.' && i + 2 < code.length() && isDigit(code.charAt(i + 2))))) {
                i++; 
                if (i + 1 < code.length() && code.charAt(i) == '0' && (code.charAt(i + 1) == 'x' || code.charAt(i + 1) == 'X')) {
                    i += 2;
                    int negHexStart = i;
                    while (i < code.length() && isHexDigit(code.charAt(i))) { i++; }
                    if (i == negHexStart) { throw new RuntimeException("Malformed number near '" + code.substring(start, Math.min(i + 1, code.length())) + "'"); }
                    try { double numValue = -(double) Long.parseLong(code.substring(negHexStart, i), 16); tokens.addElement(new Token(NUMBER, luaNumber(numValue), start)); }
                    catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + code.substring(start, i) + "'"); }
                    continue;
                }

                boolean hasDecimal = false;
                while (i < code.length() && (isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) { break; }
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') { break; }
                        hasDecimal = true;
                    }
                    i++;
                }
                try { double numValue = Double.parseDouble(code.substring(start, i)); tokens.addElement(new Token(NUMBER, luaNumber(numValue), start)); } 
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + code.substring(start, i) + "'"); }
            }

            else if (c == '"' || c == '\'') { char quoteChar = c; i++; int strStart = i; while (i < code.length() && code.charAt(i) != quoteChar) { i++; } tokens.addElement(new Token(STRING, code.substring(strStart, i), start)); if (i < code.length() && code.charAt(i) == quoteChar) { i++; } }
            else if (c == '[' && i + 1 < code.length() && code.charAt(i + 1) == '[') { i += 2; int strStart = i; while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) { i++; } tokens.addElement(new Token(STRING, code.substring(strStart, i), start)); if (i + 1 < code.length()) { i += 2; } }

            else if (isLetter(c)) { int wordStart = i; while (i < code.length() && isLetterOrDigit(code.charAt(i))) { i++; } String word = code.substring(wordStart, i); tokens.addElement(new Token((word.equals("true") || word.equals("false")) ? BOOLEAN : word.equals("nil") ? NIL : word.equals("and") ? AND : word.equals("or") ? OR : word.equals("not") ? NOT : word.equals("if") ? IF : word.equals("then") ? THEN : word.equals("else") ? ELSE : word.equals("elseif") ? ELSEIF : word.equals("end") ? END : word.equals("while") ? WHILE : word.equals("do") ? DO : word.equals("return") ? RETURN : word.equals("function") ? FUNCTION : word.equals("local") ? LOCAL : word.equals("for") ? FOR : word.equals("in") ? IN : word.equals("break") ? BREAK : word.equals("repeat") ? REPEAT : word.equals("until") ? UNTIL : word.equals("goto") ? GOTO : IDENTIFIER, word, start)); }
    
            else if (c == '+') { tokens.addElement(new Token(PLUS, "+", start)); i++; }
            else if (c == '-') { tokens.addElement(new Token(MINUS, "-", start)); i++; }
            else if (c == '*') { tokens.addElement(new Token(MULTIPLY, "*", start)); i++; }
            else if (c == '/') { tokens.addElement(new Token(DIVIDE, "/", start)); i++; }
            else if (c == '%') { tokens.addElement(new Token(MODULO, "%", start)); i++; }
            else if (c == '(') { tokens.addElement(new Token(LPAREN, "(", start)); i++; }
            else if (c == ')') { tokens.addElement(new Token(RPAREN, ")", start)); i++; }
            else if (c == ',') { tokens.addElement(new Token(COMMA, ",", start)); i++; }
            else if (c == '^') { tokens.addElement(new Token(POWER, "^", start)); i++; }
            else if (c == '#') { tokens.addElement(new Token(LENGTH, "#", start)); i++; }
    
            else if (c == '=') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(EQ, "==", start)); i += 2; } else { tokens.addElement(new Token(ASSIGN, "=", start)); i++; } }
            else if (c == '~') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(NE, "~=", start)); i += 2; } else { throw new Exception("Unexpected character '~'"); } }
            else if (c == '<') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(LE, "<=", start)); i += 2; } else { tokens.addElement(new Token(LT, "<", start)); i++; } }
            else if (c == '>') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(GE, ">=", start)); i += 2; } else { tokens.addElement(new Token(GT, ">", start)); i++; } }

            else if (c == '{') { tokens.addElement(new Token(LBRACE, "{", start)); i++; }
            else if (c == '}') { tokens.addElement(new Token(RBRACE, "}", start)); i++; }
            else if (c == '[') { tokens.addElement(new Token(LBRACKET, "[", start)); i++; }
            else if (c == ']') { tokens.addElement(new Token(RBRACKET, "]", start)); i++; }

            else { throw new Exception("Unexpected character '" + c + "'"); }
        }

        tokens.addElement(new Token(EOF, "EOF", i));
        if (midlet.useCache) { if (midlet.cacheLua.size() > 100) { midlet.cacheLua.clear(); } midlet.cacheLua.put(code, tokens); }
        return tokens;
    }
    public Token peek() { if (tokenIndex < tokens.size()) { return (Token) tokens.elementAt(tokenIndex); } return new Token(EOF, "EOF"); }
    public Token peekNext() { if (tokenIndex + 1 < tokens.size()) { return (Token) tokens.elementAt(tokenIndex + 1); } return new Token(EOF, "EOF"); }
    private Token consume() { if (tokenIndex < tokens.size()) { return (Token) tokens.elementAt(tokenIndex++); } return new Token(EOF, "EOF"); }
    private Token consume(int expectedType) throws Exception { Token token = peek(); if (token.type == expectedType) { tokenIndex++; return token; } throw new Exception("Expected token type " + expectedType + " but got " + token.type + " with value " + token.value); }
    // |
    // Statements
    public Object statement(Hashtable scope) throws Exception {
        Token current = peek();

        if (status != 0) { silent = true; midlet.sys.remove(PID); throw new Error(); }
        if (midlet.sys.containsKey(PID)) { } else { silent = true; throw new Error("Process killed"); } 

        if (current.type == IDENTIFIER) {
            int la = 0;
            boolean patternIsMultiAssign = false;
            if (tokenIndex + la < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la)).type == IDENTIFIER) {
                la++; 
                while (tokenIndex + la < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la)).type == COMMA) {
                    if (!(tokenIndex + la + 1 < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la + 1)).type == IDENTIFIER)) {
                        patternIsMultiAssign = false;
                        break;
                    }
                    la += 2; 
                }
                if (tokenIndex + la < tokens.size() && ((Token)tokens.elementAt(tokenIndex + la)).type == ASSIGN) { patternIsMultiAssign = true; }
            }

            Token next = peekNext();
            if (!patternIsMultiAssign && next.type == LPAREN) { String funcName = (String) consume(IDENTIFIER).value; callFunction(funcName, scope); return null; }
            if (patternIsMultiAssign) {
                Vector varNames = new Vector();
                varNames.addElement(((Token) consume(IDENTIFIER)).value);
                while (peek().type == COMMA) {
                    consume(COMMA);
                    varNames.addElement(((Token) consume(IDENTIFIER)).value);
                }
                consume(ASSIGN);

                Vector values = new Vector();
                values.addElement(expression(scope));
                while (peek().type == COMMA) { consume(COMMA); values.addElement(expression(scope)); }

                Vector assignValues = new Vector();
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.elementAt(i);
                    if (i == values.size() - 1 && v instanceof Vector) {
                        Vector expanded = (Vector) v;
                        for (int j = 0; j < expanded.size(); j++) { assignValues.addElement(expanded.elementAt(j)); }
                    } 
                    else { assignValues.addElement(firstValue(v)); }
                }

                for (int i = 0; i < varNames.size(); i++) {
                    String v = (String) varNames.elementAt(i);
                    Object val = i < assignValues.size() ? assignValues.elementAt(i) : null;
                    assignVariable(scope, v, val);
                }
                return null;
            }

            String varName = (String) consume(IDENTIFIER).value;
            if (peek().type == DOT || peek().type == LBRACKET) {
                Object[] pair = resolveTableAndKey(varName, scope);
                Object targetTable = pair[0];
                Object key = pair[1];
                if (!(targetTable instanceof Hashtable)) { throw new Exception("Attempt to index non-table value"); }

                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object value = firstValue(expression(scope));
                    indexSet(targetTable, key, value);
                    return null;
                } 
                else if (peek().type == LPAREN) { return callFunctionObject(indexValue(targetTable, key), scope); }
                else { return indexValue(targetTable, key); }
            } 
            else if (peek().type == COLON) {
                Object self = unwrap(scope.get(varName));
                if (self == null && globals.containsKey(varName)) { self = unwrap(globals.get(varName)); }
                if (self == null) { throw new Exception("attempt to call method on nil value: " + varName); }

                consume(COLON);
                String methodName = (String) consume(IDENTIFIER).value;
                Object fn = fieldValue(self, methodName);

                if (fn != null) {
                    return callMethod(self, varName, fn, methodName, scope);
                }

                throw new Exception("method '" + methodName + "' not found " + (self instanceof Hashtable ? "in table: " + varName : "for type: " + LuaFunction.type(self)));
            }
            else {
                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object value = firstValue(expression(scope));
                    assignVariable(scope, varName, value);
                    return null;
                } 
                else if (peek().type == LPAREN) { return callFunction(varName, scope); } 
                else { return unwrap(scope.get(varName)); }
            }
        }

        else if (current.type == LABEL) { labels.put(consume(LABEL).value, new Integer(tokenIndex)); return null; }
        else if (current.type == GOTO) {
            consume(GOTO);
            String labelName = (String) consume(IDENTIFIER).value;

            if (labels.containsKey(labelName)) { } else { throw new Exception("undefined label '" + labelName + "'"); }

            Integer labelPos = (Integer) labels.get(labelName);

            tokenIndex = labelPos.intValue();
            return null;
        }

        else if (current.type == IF) {
            consume(IF);
            Object cond = expression(scope);
            consume(THEN);

            Object result = null;
            boolean taken = false;

            if (isTruthy(cond)) {
                taken = true;
                while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                    result = statement(scope);
                    if (doreturn) { return result; }
                }
            } 
            else { skipIfBodyUntilElsePart(); }

            while (peek().type == ELSEIF) {
                consume(ELSEIF);
                cond = expression(scope);
                consume(THEN);

                if (!taken && isTruthy(cond)) {
                    taken = true;
                    while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                        result = statement(scope);
                        if (doreturn) { return result; }
                    }
                }
                else { skipIfBodyUntilElsePart(); }
            }

            if (peek().type == ELSE) {
                consume(ELSE);
                if (!taken) { while (peek().type != END) {  result = statement(scope); if (doreturn) { return result; } } } 
                else { skipUntilMatchingEnd(); }
            }

            consume(END);
            return result;
        }
        
        else if (current.type == FOR) {
            consume(FOR);

            loopDepth++;

            if (peek().type == IDENTIFIER) {
                Token t1 = (Token) peek();
                int save = tokenIndex;
                String name = (String) consume(IDENTIFIER).value;

                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object a = expression(scope);
                    consume(COMMA);
                    Object b = expression(scope);
                    Double start = (a instanceof Double) ? (Double) a : new Double(Double.parseDouble(toLuaString(a)));
                    Double stop  = (b instanceof Double) ? (Double) b : new Double(Double.parseDouble(toLuaString(b)));
                    Double step  = new Double(1.0);
                    if (peek().type == COMMA) {
                        consume(COMMA);
                        Object c = expression(scope);
                        step = (c instanceof Double) ? (Double) c : new Double(Double.parseDouble(toLuaString(c)));
                        if (((Double) step).doubleValue() == 0.0) throw new Exception("for step must not be zero");
                    }
                    consume(DO);

                    Vector bodyTokens = new Vector();
                    int depth = 1;
                    while (depth > 0) {
                        Token tk = consume();
                        if (tk.type == IF || tk.type == WHILE || tk.type == FUNCTION || tk.type == FOR) depth++;
                        else if (tk.type == END) depth--;
                        else if (tk.type == EOF) throw new Exception("Unmatched 'for' statement: Expected 'end'");
                        if (depth > 0) bodyTokens.addElement(tk);
                    }

                    double iVal = start.doubleValue(), stopVal = stop.doubleValue(), stepVal = step.doubleValue();

                    while ((stepVal > 0 && iVal <= stopVal) || (stepVal < 0 && iVal >= stopVal)) {
                        if (breakLoop) { breakLoop = false; break; }

                        scope.put(name, luaNumber(iVal));

                        int originalTokenIndex = tokenIndex;
                        Vector originalTokens = tokens;

                        tokens = bodyTokens;
                        tokenIndex = 0;

                        Object ret = null;
                        boolean returned = false;
                        while (peek().type != EOF) {
                            ret = statement(scope);
                            if (doreturn) { returned = true; break; }
                        }

                        tokenIndex = originalTokenIndex;
                        tokens = originalTokens;

                        // The body's last statement value is not a return: a
                        // call such as `c:rect(...)` returns a handle and used
                        // to abort the loop after its first iteration.
                        if (returned) { return ret; }

                        iVal += stepVal;
                    }

                    loopDepth--;
                    return null;
                } 
                else {
                    tokenIndex = save;
                    Vector names = new Vector();
                    names.addElement(((Token) consume(IDENTIFIER)).value);
                    while (peek().type == COMMA) {
                        consume(COMMA);
                        names.addElement(((Token) consume(IDENTIFIER)).value);
                    }
                    consume(IN);
                    Object iterSrc = expression(scope);
                    consume(DO);

                    Vector bodyTokens = new Vector();
                    int depth2 = 1;
                    while (depth2 > 0) {
                        Token tk = consume();
                        if (tk.type == IF || tk.type == WHILE || tk.type == FUNCTION || tk.type == FOR) depth2++;
                        else if (tk.type == END) depth2--;
                        else if (tk.type == EOF) throw new Exception("Unmatched 'for' statement: Expected 'end'");
                        if (depth2 > 0) bodyTokens.addElement(tk);
                    }

                    if (iterSrc instanceof Hashtable && ((Hashtable) iterSrc).containsKey("__table")) {
                        Hashtable iterator = (Hashtable) iterSrc;
                        Object tableObj = iterator.get("__table");
                        int currentIndex = ((Double) iterator.get("__index")).intValue();
                        
                        if (tableObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tableObj;
                            Vector list = LuaFunction.toVector(table);
                            
                            for (int idx = currentIndex; idx < list.size(); idx++) {
                                Object item = list.elementAt(idx);
                                
                                if (names.size() >= 1) scope.put((String) names.elementAt(0), luaNumber(idx + 1));
                                if (names.size() >= 2) scope.put((String) names.elementAt(1), item == null ? LUA_NIL : item);

                                // Update the iterator index
                                iterator.put("__index", luaNumber(idx + 1));

                                int originalTokenIndex = tokenIndex;
                                Vector originalTokens = tokens;
                                tokens = bodyTokens;
                                tokenIndex = 0;

                                Object ret = null;
                                boolean returned = false;
                                while (peek().type != EOF) {
                                    ret = statement(scope);
                                    if (doreturn) { returned = true; break; }
                                }

                                tokenIndex = originalTokenIndex;
                                tokens = originalTokens;
                                if (returned) { return ret; }

                                if (breakLoop) {
                                    breakLoop = false;
                                    break;
                                }
                            }
                        }
                    }
                    else if (iterSrc instanceof Hashtable) {
                        Hashtable ht = (Hashtable) iterSrc;
                        for (Enumeration e = ht.keys(); e.hasMoreElements();) {
                            Object k = e.nextElement();
                            if ("__metatable".equals(k)) { continue; }
                            Object v = unwrap(ht.get(k));
                            if (names.size() >= 1) scope.put((String) names.elementAt(0), (k == null ? LUA_NIL : k));
                            if (names.size() >= 2) scope.put((String) names.elementAt(1), (v == null ? LUA_NIL : v));

                            int originalTokenIndex = tokenIndex;
                            Vector originalTokens = tokens;
                            tokens = bodyTokens;
                            tokenIndex = 0;

                            Object ret = null;
                            boolean returned = false;
                            while (peek().type != EOF) {
                                ret = statement(scope);
                                if (doreturn) { returned = true; break; }
                            }

                            tokenIndex = originalTokenIndex;
                            tokens = originalTokens;
                            if (returned) { return ret; }

                            if (breakLoop) { breakLoop = false; break; }
                        }
                    } 
                    else if (iterSrc instanceof Vector) {
                        Vector vec = (Vector) iterSrc;
                        for (int idx = 0; idx < vec.size(); idx++) {
                            Object item = vec.elementAt(idx);
                            Object k = null, v = null;
                            if (item instanceof Vector) {
                                Vector pair = (Vector) item;
                                if (pair.size() > 0) k = pair.elementAt(0);
                                if (pair.size() > 1) v = pair.elementAt(1);
                            } else {
                                k = luaNumber(idx + 1);
                                v = item;
                            }
                            if (names.size() >= 1) scope.put((String) names.elementAt(0), (k == null ? LUA_NIL : k));
                            if (names.size() >= 2) scope.put((String) names.elementAt(1), (v == null ? LUA_NIL : v));

                            int originalTokenIndex = tokenIndex;
                            Vector originalTokens = tokens;
                            tokens = bodyTokens;
                            tokenIndex = 0;

                            Object ret = null;
                            boolean returned = false;
                            while (peek().type != EOF) {
                                ret = statement(scope);
                                if (doreturn) { returned = true; break; }
                            }

                            tokenIndex = originalTokenIndex;
                            tokens = originalTokens;
                            if (returned) { return ret; }

                            if (breakLoop) { breakLoop = false; break; }
                        }
                    } 
                    else if (iterSrc == null) { } 
                    else { throw new Exception("Generic for: unsupported iterator source"); }

                    loopDepth--;
                    return null;
                }
            }

            loopDepth--;
            throw new Exception("Malformed 'for' statement");
        }
        else if (current.type == WHILE) {
            consume(WHILE);
            int conditionStartTokenIndex = tokenIndex;

            Object result = null;
            boolean endAlreadyConsumed = false;

            loopDepth++; 

            while (true) {
                tokenIndex = conditionStartTokenIndex;
                Object condition = expression(scope);

                if (!isTruthy(condition) || breakLoop || doreturn) {
                    int depth = 1;
                    while (depth > 0) {
                        Token token = consume();
                        if (token.type == IF || token.type == WHILE || token.type == FUNCTION || token.type == FOR) { depth++; }
                        else if (token.type == END) { depth--; }
                        else if (token.type == EOF) { throw new RuntimeException("Unmatched 'while' statement: Expected 'end'"); }
                    }
                    endAlreadyConsumed = true; 
                    breakLoop = false;
                    break;
                }

                consume(DO);

                while (peek().type != END) {
                    result = statement(scope);
                    if (doreturn || breakLoop) { break; }
                }
                tokenIndex = conditionStartTokenIndex;
            }

            loopDepth--;

            if (!endAlreadyConsumed) consume(END);
            return result;
        }
        else if (current.type == REPEAT) {
            consume(REPEAT);

            int bodyStartTokenIndex = tokenIndex;
            Object result = null;

            loopDepth++;

            while (true) {
                tokenIndex = bodyStartTokenIndex;

                while (peek().type != UNTIL) {
                    result = statement(scope);

                    if (doreturn || breakLoop) { while (peek().type != UNTIL && peek().type != EOF) { consume(); } break; }
                }

                consume(UNTIL);
                Object cond = expression(scope);

                if (isTruthy(cond) || doreturn) { break; }
                else if (breakLoop) { breakLoop = false; break; } 
            }

            loopDepth--;
            return result;
        }
        else if (current.type == RETURN) {
            consume(RETURN);

            if (peek().type == EOF || peek().type == END) { doreturn = true; return new Vector(); }

            // The expressions run first: `return f()` must not abort f's body
            Vector results = new Vector();
            Object first = expression(scope);
            if (peek().type != COMMA && first instanceof Vector) {
                // `return f()` forwards every result of the call, as in Lua
                Vector list = (Vector) first;
                for (int i = 0; i < list.size(); i++) { results.addElement(list.elementAt(i)); }
            } else {
                results.addElement(first);
                while (peek().type == COMMA) { consume(COMMA); results.addElement(expression(scope)); }
            }
            doreturn = true;
            return results;
        }

        else if (current.type == FUNCTION) {
            int funcOffset = current.offset;
            consume(FUNCTION);
            String funcName = (String) consume(IDENTIFIER).value;

            boolean isTableAssignment = (peek().type == DOT || peek().type == LBRACKET);
            Object targetTable = null, key = null;

            if (isTableAssignment) {
                Object[] pair = resolveTableAndKey(funcName, scope);
                targetTable = pair[0];
                key = pair[1];
                if (!(targetTable instanceof Hashtable)) { throw new Exception("Attempt to index non-table value in function definition"); } 
            }
            
            consume(LPAREN);
            Vector params = new Vector();
            while (true) {
                int t = peek().type;

                if (t == IDENTIFIER) { params.addElement(consume(IDENTIFIER).value); } 
                else if (t == VARARG) { consume(VARARG); params.addElement("..."); break; } 
                else { break; } 

                if (peek().type == COMMA) { consume(COMMA); } 
                else { break; }
            }
            consume(RPAREN);

            Vector bodyTokens = new Vector();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                if (token.type == FUNCTION || token.type == IF || token.type == DO) { depth++; }
                else if (token.type == END) { depth--; }
                else if (token.type == EOF) { throw new Exception("Unmatched 'function (" + funcName + ")' statement: Expected 'end'"); }
                if (depth > 0) { bodyTokens.addElement(token); }
            }

            LuaFunction func = new LuaFunction(params, bodyTokens, scope);
            func.name = funcName;
            func.defSource = currentSource;
            func.defCode = lastCode;
            func.defLineOffsets = lineOffsets;
            func.defLine = lineFromOffset(lastCode, funcOffset);

            if (isTableAssignment) { ((Hashtable) targetTable).put(key, func); } 
            else { scope.put(funcName, func); }

            return null;
        } 
        else if (current.type == LOCAL) {
            consume(LOCAL);

            if (peek().type == FUNCTION) {
                int funcOffset = peek().offset;
                consume(FUNCTION);
                String funcName = (String) consume(IDENTIFIER).value;

                consume(LPAREN);
                Vector params = new Vector();
                while (true) {
                    int t = peek().type;
                    if (t == IDENTIFIER) { params.addElement(consume(IDENTIFIER).value); } 
                    else if (t == VARARG) { consume(VARARG); params.addElement("..."); break; } 
                    else { break; }

                    if (peek().type == COMMA) { consume(COMMA); } 
                    else { break; }
                }
                consume(RPAREN);
    
                Vector bodyTokens = new Vector();
                int depth = 1;
                while (depth > 0) {
                    Token token = consume();
    
                    // Tokens that OPEN blocks
                    if (token.type == FUNCTION || token.type == IF || token.type == DO) { 
                        depth++; 
                    }
                    // Tokens that CLOSE blocks  
                    else if (token.type == END) { 
                        depth--; 
                    }
                    else if (token.type == EOF) { 
                        throw new Exception("Unmatched 'function (" + funcName + ")' statement: Expected 'end'"); 
                    }

                    if (depth > 0) { 
                        bodyTokens.addElement(token); 
                    }
                }

                LuaFunction func = new LuaFunction(params, bodyTokens, scope);
                func.name = funcName;
                func.defSource = currentSource;
                func.defCode = lastCode;
                func.defLineOffsets = lineOffsets;
                func.defLine = lineFromOffset(lastCode, funcOffset);
                scope.put(funcName, func);
                return null;
            } 
            else {
                Vector varNames = new Vector();

                varNames.addElement(((Token) consume(IDENTIFIER)).value);
                while (peek().type == COMMA) { consume(COMMA); varNames.addElement(((Token) consume(IDENTIFIER)).value); }

                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Vector values = new Vector();
                    values.addElement(expression(scope));
                    while (peek().type == COMMA) { consume(COMMA); values.addElement(expression(scope)); }
        
                    Vector assignValues = new Vector();
                    for (int i = 0; i < values.size(); i++) {
                        Object v = values.elementAt(i);
                        if (i == values.size() - 1 && v instanceof Vector) {
                            Vector expanded = (Vector) v;
                            for (int j = 0; j < expanded.size(); j++) { assignValues.addElement(expanded.elementAt(j)); }
                        } else { assignValues.addElement(firstValue(v)); }
                    }


                    for (int i = 0; i < varNames.size(); i++) {
                        String v = (String) varNames.elementAt(i);
                        Object val = i < assignValues.size() ? assignValues.elementAt(i) : null;
                        scope.put(v, val == null ? LUA_NIL : val);
                    }
                } 
                else { for (int i = 0; i < varNames.size(); i++) { String v = (String) varNames.elementAt(i); scope.put(v, LUA_NIL); } }

                return null;
            }
        }
        else if (current.type == BREAK) { if (loopDepth == 0) { throw new RuntimeException("break outside loop"); } consume(BREAK); breakLoop = true; return null; }
        else if (current.type == DO) {
            consume(DO);
            
            Vector bodyTokens = new Vector();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                
                if (token.type == FUNCTION || token.type == IF || token.type == DO) { 
                    depth++; 
                }
                else if (token.type == END) { 
                    depth--; 
                }
                else if (token.type == EOF) { 
                    throw new RuntimeException("Unmatched 'do' statement: Expected 'end'"); 
                }
                
                if (depth > 0) { 
                    bodyTokens.addElement(token); 
                }
            }
            
            // Execute the DO block
            int originalTokenIndex = tokenIndex;
            Vector originalTokens = tokens;
            
            tokens = bodyTokens;
            tokenIndex = 0;
            
            Object result = null;
            while (peek().type != EOF) { result = statement(scope); if (doreturn) { break; } }
            
            tokenIndex = originalTokenIndex;
            tokens = originalTokens;
            
            return result;
        }
        else if (current.type == END) { consume(END); return null; }
        else if (current.type == LPAREN || current.type == NUMBER || current.type == STRING || current.type == BOOLEAN || current.type == NIL || current.type == NOT) { return expression(scope); }

        throw new RuntimeException("Unexpected token at statement: " + current.toString() + " - " + peekNext().toString());
    }
    // |
    // Expressions
    private Object expression(Hashtable scope) throws Exception { return logicalOr(scope); }
    // | (A call yields its results as a Vector; operators use the first one)
    private Object firstValue(Object value) {
        if (value instanceof Vector) { Vector list = (Vector) value; return list.isEmpty() ? null : list.elementAt(0); }
        return value;
    }

    private Object logicalOr(Hashtable scope) throws Exception {
        Object left = logicalAnd(scope);
        while (peek().type == OR) {
            consume(OR);
            if (isTruthy(left)) { skipOperand(scope); } else { left = logicalAnd(scope); }
        }
        return left;
    }
    private Object logicalAnd(Hashtable scope) throws Exception {
        Object left = comparison(scope);
        while (peek().type == AND) {
            consume(AND);
            if (isTruthy(left)) { left = comparison(scope); } else { skipOperand(scope); }
        }
        return left;
    }
    // | (Short-circuit: parse the operand we must not evaluate. It is still
    // |  consumed token by token so the parser keeps its place; every value
    // |  it would produce is dropped.)
    private Object skipOperand(Hashtable scope) throws Exception {
        suppress++;
        try { return logicalAnd(scope); }
        catch (Exception e) { return null; }
        catch (Error e) { return null; }
        finally { suppress--; }
    }
    private Object comparison(Hashtable scope) throws Exception {
        Object left = concatenation(scope);
        while (peek().type == EQ || peek().type == NE || peek().type == LT || peek().type == GT || peek().type == LE || peek().type == GE) {
            Token op = consume();
            Object right = concatenation(scope);
            if (suppress > 0) { left = null; continue; }
            left = firstValue(left);
            right = firstValue(right);
            if (op.type == EQ || op.type == NE) {
                boolean equal = (left == null && right == null) || (left != null && left.equals(right));
                if (!equal && left instanceof Hashtable && right instanceof Hashtable && hasAnyMeta(left, right, "__eq")) { equal = isTruthy(metaBinary("__eq", left, right)); }
                left = (op.type == EQ) == equal ? TRUE : FALSE;
            }
            else if (op.type == LT) { left = luaLessThan(left, right) ? TRUE : FALSE; }
            else if (op.type == GT) { left = luaLessThan(right, left) ? TRUE : FALSE; }
            else if (op.type == LE) { left = luaLessEqual(left, right) ? TRUE : FALSE; }
            else if (op.type == GE) { left = luaLessEqual(right, left) ? TRUE : FALSE; }
        }
        return left;
    }
    // | (Ordering: numbers, then strings, then __lt/__le)
    private boolean luaLessThan(Object left, Object right) throws Exception {
        if (left instanceof Double && right instanceof Double) { return ((Double) left).doubleValue() < ((Double) right).doubleValue(); }
        if (left instanceof String && right instanceof String) { return ((String) left).compareTo((String) right) < 0; }

        if (hasAnyMeta(left, right, "__lt")) { return isTruthy(metaBinary("__lt", left, right)); }

        throw new Exception("attempt to compare " + LuaFunction.type(left) + " with " + LuaFunction.type(right));
    }
    private boolean luaLessEqual(Object left, Object right) throws Exception {
        if (left instanceof Double && right instanceof Double) { return ((Double) left).doubleValue() <= ((Double) right).doubleValue(); }
        if (left instanceof String && right instanceof String) { return ((String) left).compareTo((String) right) <= 0; }

        if (hasAnyMeta(left, right, "__le")) { return isTruthy(metaBinary("__le", left, right)); }
        // Lua's `a <= b` falls back to `not (b < a)` when only __lt exists
        if (hasAnyMeta(left, right, "__lt")) { return !isTruthy(metaBinary("__lt", right, left)); }

        throw new Exception("attempt to compare " + LuaFunction.type(left) + " with " + LuaFunction.type(right));
    }
    // |
    // Strings
    private String toLuaString(Object obj) { if (obj == null || obj == LUA_NIL) { return "nil"; } if (obj instanceof Boolean) { return ((Boolean)obj).booleanValue() ? "true" : "false"; } if (obj instanceof Double) { double d = ((Double)obj).doubleValue(); if (d == (long)d) return String.valueOf((long)d); return String.valueOf(d); } return midlet.escape(obj.toString()); }
    private Object concatenation(Hashtable scope) throws Exception {
        Object left = arithmetic(scope);

        while (peek().type == CONCAT) {
            consume(CONCAT);
            Object right = arithmetic(scope);
            if (suppress > 0) { left = null; continue; }
            left = firstValue(left);
            right = firstValue(right);

            if ((left instanceof String || left instanceof Double) && (right instanceof String || right instanceof Double)) { left = toLuaString(left) + toLuaString(right); }
            else if (hasAnyMeta(left, right, "__concat")) { left = metaBinary("__concat", left, right); }
            else { left = toLuaString(left) + toLuaString(right); }
        }
        return left;
    }
    // |
    // Arithmetic
    private Object arithmetic(Hashtable scope) throws Exception {
        Object left = term(scope); 
        while (peek().type == PLUS || peek().type == MINUS) {
            Token op = consume();
            Object right = term(scope); 
            if (suppress > 0) { left = null; continue; }
            left = firstValue(left);
            right = firstValue(right);
            String event = op.type == PLUS ? "__add" : "__sub";

            if (left instanceof Double && right instanceof Double) {
                double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();
                left = luaNumber(op.type == PLUS ? lVal + rVal : lVal - rVal); 
            }
            else if (hasAnyMeta(left, right, event)) { left = metaBinary(event, left, right); }
            else { throw new ArithmeticException("Arithmetic operation on non-number types."); }
        }
        return left;
    }
    private Object term(Hashtable scope) throws Exception {
        Object left = exponentiation(scope);
        while (peek().type == MULTIPLY || peek().type == DIVIDE || peek().type == MODULO) {
            Token op = consume();
            Object right = exponentiation(scope);
            if (suppress > 0) { left = null; continue; }
            left = firstValue(left);
            right = firstValue(right);
            String event = op.type == MULTIPLY ? "__mul" : op.type == DIVIDE ? "__div" : "__mod";

            if (left instanceof Double && right instanceof Double) {
                double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();

                if (op.type == MULTIPLY) { left = luaNumber(lVal * rVal); } 
                else if (op.type == DIVIDE) { if (rVal == 0) { throw new Exception("Division by zero."); } left = luaNumber(lVal / rVal); } 
                else if (op.type == MODULO) { if (rVal == 0) { throw new Exception("Modulo by zero."); } left = luaNumber(lVal % rVal); }
            }
            else if (hasAnyMeta(left, right, event)) { left = metaBinary(event, left, right); }
            else { throw new ArithmeticException("Arithmetic operation on non-number types."); }
        }
        return left;
    }
    private Object exponentiation(Hashtable scope) throws Exception {
        Object left = factor(scope);
        while (peek().type == POWER) {
            consume(POWER);
            Object right = factor(scope);
            if (suppress > 0) { left = null; continue; }

            if (!(left instanceof Double) || !(right instanceof Double)) {
                if (hasAnyMeta(left, right, "__pow")) { left = metaBinary("__pow", left, right); continue; }
                throw new ArithmeticException("Arithmetic operation on non-number types.");  
            }

            double base = ((Double) left).doubleValue(), exponent = ((Double) right).doubleValue(), result;

            if (exponent == 0) { result = 1; } 
            else if (exponent == 0.5) { if (base < 0) { throw new ArithmeticException("Square root of negative number."); } result = Math.sqrt(base); } 
            else if (exponent < 0 && Math.floor(exponent) == exponent) {
                base = 1 / base;
                exponent = -exponent;
                result = 1;
                for (int i = 0; i < (int) exponent; i++) { result *= base; }
            } 
            else if (Math.floor(exponent) == exponent) { result = 1; for (int i = 0; i < (int) exponent; i++) { result *= base; } } 
            else { throw new ArithmeticException("Fractional exponent not supported: " + exponent); }

            left = luaNumber(result);
        }
        return left;
    }
    // |
    // Build Objects
    private Object factor(Hashtable scope) throws Exception {
        Token current = peek();
        
        if (current.type == STRING || current.type == NUMBER || current.type == BOOLEAN || current.type == NIL) {
            Object base = null;

            if (current.type == STRING) { base = consume(STRING).value; } 
            else if (current.type == NUMBER) { base = consume(NUMBER).value; } 
            else if (current.type == BOOLEAN) { consume(BOOLEAN); base = current.value.equals("true") ? TRUE : FALSE; } 
            else if (current.type == NIL) { consume(NIL); base = null; }

            while (peek().type == DOT || peek().type == COLON) {
                if (peek().type == DOT) {
                    consume(DOT);
                    String field = (String) consume(IDENTIFIER).value;

                    if (base == null) { if (suppress > 0) { continue; } throw new Exception("attempt to index a nil value after literal"); }
                    base = fieldValue(base, field);
                } 
                else if (peek().type == COLON) {
                    consume(COLON);
                    String method = (String) consume(IDENTIFIER).value;

                    Object func = base == null ? null : fieldValue(base, method);
                    if (func == null && suppress == 0) { throw new Exception("method '" + method + "' not found for type: " + LuaFunction.type(base)); }

                    base = callMethod(base, null, func, method, scope);
                }
            }

            return base;
        }
        else if (current.type == MINUS) {
            consume(MINUS);
            Object val = factor(scope);
            if (val instanceof Double) { return luaNumber(-((Double) val).doubleValue()); }
            if (hasMeta(val, "__unm")) { return callMeta("__unm", new Object[] { val }); }
            if (suppress > 0) { return null; }
            throw new RuntimeException("attempt to perform arithmetic on a " + LuaFunction.type(val) + " value");
        }
        else if (current.type == NOT) { consume(NOT); return new Boolean(!isTruthy(factor(scope))); } 
        else if (current.type == LPAREN) { consume(LPAREN); Object value = expression(scope); consume(RPAREN); return value; } 
        else if (current.type == LENGTH) { consume(LENGTH); Object val = factor(scope); if (hasMeta(val, "__len")) { return callMeta("__len", new Object[] { val }); } else if (val == null || val instanceof Boolean) { if (suppress > 0) { return null; } throw new RuntimeException("attempt to get length of a " + (val == null ? "nil" : "boolean") + " value"); } else if (val instanceof String) { return luaNumber(((String) val).length()); } else if (val instanceof Hashtable) { int size = luaLength((Hashtable) val); if (val == globals.get("arg") && size > 0) { size -= 1; } return luaNumber(size); } else if (val instanceof Vector) { return luaNumber(((Vector) val).size()); } else if (val instanceof InputStream) { return luaNumber(((InputStream) val).available()); } else { return luaNumber(0); } }
        else if (current.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            Object value = unwrap(scope.get(name));
            if (value == null && scope == globals == false) { }
            if (value == null && globals.containsKey(name)) { value = unwrap(globals.get(name)); }
            while (peek().type == LBRACKET || peek().type == DOT) {
                Object key = null;
                if (peek().type == LBRACKET) { consume(LBRACKET); key = expression(scope); consume(RBRACKET); } 
                else { consume(DOT); key = (String) consume(IDENTIFIER).value; }

                if (value == null || !(value instanceof Hashtable)) {
                    if (suppress == 0) { throw new Exception(value == null ? "attempt to index a nil value" : "attempt to index a non-table value"); }
                    value = null;
                }
                else { value = indexValue(value, key); }
            }

            if (peek().type == COLON) {
                String objectName = (String) ((Token) tokens.elementAt(tokenIndex - 1)).value;
            
                Object self = unwrap(scope.get(objectName));
                if (self == null && globals.containsKey(objectName)) { self = unwrap(globals.get(objectName)); }
                if (self == null && suppress == 0) { throw new Exception("attempt to call method on nil value: " + objectName); }
            
                consume(COLON);
                String methodName = (String) consume(IDENTIFIER).value;
            
                Object func = self == null ? null : fieldValue(self, methodName);
                if (func == null && suppress == 0) { throw new Exception("method '" + methodName + "' not found for type: " + LuaFunction.type(self)); }
            
                return callMethod(self, objectName, func, methodName, scope);
            }
            else if (peek().type == LPAREN) { return callFunctionObject(value, scope); }

            return value;
        }
        else if (current.type == FUNCTION) {
            int funcOffset = current.offset;
            consume(FUNCTION);

            consume(LPAREN);
            Vector params = new Vector();
            while (true) {
                int t = peek().type;

                if (t == IDENTIFIER) { params.addElement(consume(IDENTIFIER).value); } 
                else if (t == VARARG) { consume(VARARG); params.addElement("..."); break; } 
                else { break; }

                if (peek().type == COMMA) { consume(COMMA); } 
                else { break; }
            }
            consume(RPAREN);

            // In the anonymous function part
            Vector bodyTokens = new Vector();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                
                // Tokens that OPEN blocks
                if (token.type == FUNCTION || token.type == IF || token.type == DO) { 
                    depth++; 
                }
                // Tokens that CLOSE blocks
                // (Only one addElement per token: the trailing guard below
                //  already copies the `end` when it still belongs to the body.
                //  Adding it here too duplicated every nested `end`, so a
                //  nested `local function` ended early and its tail ran in the
                //  enclosing frame with bogus upvalues.)
                else if (token.type == END) { 
                    depth--; 
                }
                else if (token.type == EOF) { 
                    throw new RuntimeException("Unmatched 'function' statement: Expected 'end'"); 
                }
                if (depth > 0) { 
                    bodyTokens.addElement(token); 
                }
            }

            LuaFunction func = new LuaFunction(params, bodyTokens, scope);
            func.defSource = currentSource;
            func.defCode = lastCode;
            func.defLineOffsets = lineOffsets;
            func.defLine = lineFromOffset(lastCode, funcOffset);
            return func;
        }
        else if (current.type == VARARG) { consume(VARARG); Object varargs = scope.get("..."); if (varargs == null) { return new Hashtable(); } return varargs; }
        else if (current.type == LBRACE) { 
            consume(LBRACE);
            Hashtable table = new Hashtable();
            int index = 1;

            while (peek().type != RBRACE) {
                Object key = null, value = null;

                if (peek().type == IDENTIFIER && peekNext().type == ASSIGN) {
                    key = consume(IDENTIFIER).value; 
                    consume(ASSIGN); 
                    value = expression(scope); 
                } 
                else if (peek().type == LBRACKET) {
                    consume(LBRACKET);
                    key = expression(scope);
                    consume(RBRACKET);
                    consume(ASSIGN);
                    value = expression(scope);
                } 
                else { value = expression(scope); key = luaNumber(index++); }

                table.put(key, value == null ? LUA_NIL : value);

                if (peek().type == COMMA) { consume(COMMA); } else if (peek().type == RBRACE) { break; } else { throw new Exception("Malformed table syntax."); }
            }

            consume(RBRACE);
            return table;
        }

        throw new Exception("Unexpected token at factor: " + current.toString());
    }
    // |
    // Call LuaFunction
    // | (Call arguments: every expression but the last yields its first value,
    // |  the last one expands all of its results, as in Lua)
    private void readCallArgs(Vector target, Hashtable scope) throws Exception {
        consume(LPAREN);
        Vector raw = new Vector();
        if (peek().type != RPAREN) {
            raw.addElement(expression(scope));
            while (peek().type == COMMA) { consume(COMMA); raw.addElement(expression(scope)); }
        }
        consume(RPAREN);

        for (int i = 0; i < raw.size(); i++) {
            Object value = raw.elementAt(i);
            if (i == raw.size() - 1 && value instanceof Vector) {
                Vector list = (Vector) value;
                for (int j = 0; j < list.size(); j++) { target.addElement(list.elementAt(j)); }
            } else { target.addElement(firstValue(value)); }
        }
    }
    private Object callFunction(String funcName, Hashtable scope) throws Exception {
        Vector args = new Vector();
        readCallArgs(args, scope);

        Object  funcObj = unwrap(scope.get(funcName));
        if (funcObj == null && globals.containsKey(funcName)) { funcObj = unwrap(globals.get(funcName)); }

        if (suppress > 0) { return null; }
        if (isCallable(funcObj)) { return callValue(funcObj, args); }
        else { throw new RuntimeException("Attempt to call a non-function value: " + funcName); }
    }
    private Object callFunctionObject(Object funcObj, Hashtable scope) throws Exception {
        Vector args = new Vector();
        readCallArgs(args, scope);

        if (suppress > 0) { return null; }
        if (isCallable(funcObj)) { return callValue(funcObj, args); }
        else { throw new Exception("Attempt to call a non-function value (by object)."); }
    }
    // |
    private Object resolveMethod(Object obj) {
        if (obj instanceof Hashtable) {
            Hashtable table = (Hashtable) obj;
    
            Object mt = table.get("__metatable");
            if (mt instanceof Hashtable) { Object index = ((Hashtable) mt).get("__index"); if (index instanceof Hashtable || index instanceof LuaFunction) { return index; } }
        }

        String type = LuaFunction.type(obj);
        return type.equals("string") ? globals.get("string") : type.equals("table") ? globals.get("table") : type.equals("stream") ? globals.get("io") : type.equals("connection") || type.equals("server") ? globals.get("socket") : type.equals("screen") || type.equals("image") ? globals.get("graphics") : obj;
    }
    // | (Metatables)
    // | (Metatables live in the table itself under "__metatable": no registry, no per-table leak)
    private static int luaLength(Hashtable table) {
        if (table == null) { return 0; }
        int size = table.size() - (table.containsKey("__metatable") ? 1 : 0);
        return size < 0 ? 0 : size;
    }
    private Hashtable getMetatable(Object obj) {
        if (!(obj instanceof Hashtable)) { return null; }
        Object mt = ((Hashtable) obj).get("__metatable");
        return (mt instanceof Hashtable) ? (Hashtable) mt : null;
    }
    private Object metaField(Object obj, String event) {
        Hashtable mt = getMetatable(obj);
        return mt == null ? null : unwrap(mt.get(event));
    }
    private boolean hasMeta(Object obj, String event) { return metaField(obj, event) instanceof LuaFunction; }
    private boolean hasAnyMeta(Object a, Object b, String event) { return hasMeta(a, event) || hasMeta(b, event); }
    private Object callMeta(String event, Object[] values) throws Exception {
        Object handler = null;
        for (int i = 0; i < values.length && handler == null; i++) { if (hasMeta(values[i], event)) { handler = metaField(values[i], event); } }
        if (!(handler instanceof LuaFunction)) { throw new Exception("metamethod '" + event + "' is not a function"); }

        Vector args = new Vector();
        for (int i = 0; i < values.length; i++) { args.addElement(values[i] == null ? LUA_NIL : values[i]); }

        // A metamethod is a plain expression: only its first result counts.
        Object result = ((LuaFunction) handler).call(args);
        if (result instanceof Vector) { Vector list = (Vector) result; return list.isEmpty() ? LUA_NIL : wrap(list.elementAt(0)); }
        return wrap(result);
    }
    private Object metaBinary(String event, Object left, Object right) throws Exception { return callMeta(event, new Object[] { left, right }); }
    // | (Reads honor __index: own key first, then a table chain or a function)
    private Object indexValue(Object obj, Object key) throws Exception {
        if (!(obj instanceof Hashtable)) { throw new Exception("attempt to index a " + LuaFunction.type(obj) + " value"); }

        Hashtable table = (Hashtable) obj;
        Object value = unwrap(table.get(key));
        if (value != null) { return value; }

        Object handler = metaField(table, "__index");
        if (handler instanceof Hashtable) { return indexValue(handler, key); }
        if (handler instanceof LuaFunction) { return wrap(callMeta("__index", new Object[] { table, key })); }
        return null;
    }
    // | (Writes honor __newindex only when the key is absent, as in Lua)
    private void indexSet(Object obj, Object key, Object value) throws Exception {
        if (!(obj instanceof Hashtable)) { throw new Exception("attempt to index a " + LuaFunction.type(obj) + " value"); }

        Hashtable table = (Hashtable) obj;
        Object current = table.get(key);
        if (current == null || current == LUA_NIL) {
            Object handler = metaField(table, "__newindex");
            if (handler instanceof Hashtable) { indexSet(handler, key, value); return; }
            if (handler instanceof LuaFunction) { callMeta("__newindex", new Object[] { table, key, value }); return; }
        }
        // Assigning nil removes the key (so `t[#t] = nil` really shrinks a list;
        // storing a LUA_NIL placeholder kept it counted by the length operator).
        if (value == null) { table.remove(key); }
        else { table.put(key, value); }
    }
    // | (Field/method lookup: tables go through __index, primitives through their module)
    private Object fieldValue(Object self, String name) throws Exception {
        if (self instanceof Hashtable) { return indexValue(self, name); }

        Object module = resolveMethod(self);
        return module instanceof Hashtable ? unwrap(((Hashtable) module).get(name)) : null;
    }
    private boolean isCallable(Object fn) { return fn instanceof LuaFunction || hasMeta(fn, "__call"); }
    private Object callValue(Object fn, Vector args) throws Exception {
        if (fn instanceof LuaFunction) { return ((LuaFunction) fn).call(args); }

        Object handler = metaField(fn, "__call");
        if (!(handler instanceof LuaFunction)) { throw new Exception("attempt to call a " + LuaFunction.type(fn) + " value"); }

        Vector full = new Vector();
        full.addElement(fn);
        for (int i = 0; i < args.size(); i++) { full.addElement(args.elementAt(i)); }
        return wrap(((LuaFunction) handler).call(full));
    }
    private Object callMethod(Object self, String varName, Object methodObj, String methodName, Hashtable scope) throws Exception {
        if (methodObj == null) {
            methodObj = resolveMethod(self);
            Object table = unwrap(scope.get(varName));
            if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
            Object key = null;
            
            while (peek().type == DOT || peek().type == LBRACKET) {
                if (peek().type == DOT) { consume(DOT); Token field = consume(IDENTIFIER); key = field.value; } 
                else if (peek().type == LBRACKET) { consume(LBRACKET); key = expression(scope); consume(RBRACKET); }
        
                if (table == null || !(table instanceof Hashtable)) {
                    if (suppress == 0) { throw new Exception(table == null ? "attempt to index a nil value" : "attempt to index a non-table value"); }
                }
                else if (peek().type == DOT || peek().type == LBRACKET) { methodObj = unwrap(((Hashtable)table).get(key)); }
            }
        }
        Vector args = new Vector();
        args.addElement(self);
        readCallArgs(args, scope);
        if (suppress > 0) { return null; }
        if (isCallable(methodObj)) { return callValue(methodObj, args); } 
        else { throw new Exception("attempt to call non-function as method: " + methodName); }
    }
    // |
    // Handling NullPointers
    private Object wrap(Object v) { return v == null ? LUA_NIL : v; }
    private Object unwrap(Object v) { return v == LUA_NIL ? null : v; }
    // |
    private void skipIfBodyUntilElsePart() throws Exception { int depth = 1; while (true) { Token t = consume(); if (t.type == IF || t.type == WHILE || t.type == FUNCTION || t.type == FOR) { depth++; } else if (t.type == END) { depth--; if (depth == 0) { tokenIndex--; return; } } else if ((t.type == ELSEIF || t.type == ELSE) && depth == 1) { tokenIndex--; return; } else if (t.type == EOF) { throw new Exception("Unmatched 'if' statement: Expected 'end'"); } } }
    private void skipUntilMatchingEnd() throws Exception { int depth = 1; while (depth > 0) { Token t = consume(); if (t.type == IF || t.type == WHILE || t.type == FUNCTION || t.type == FOR) { depth++; } else if (t.type == END) { depth--; } else if (t.type == EOF) { throw new Exception("Unmatched 'if' statement: Expected 'end'"); } } tokenIndex--; }
    // |
    private boolean isTruthy(Object value) { if (value == null || value == LUA_NIL) { return false; } if (value instanceof Boolean) { return ((Boolean) value).booleanValue(); } return true; }
    // |
    // |
    // | (Plain assignment: write to the nearest frame that already declares the
    // |  name (locals and upvalues), else to the script scope. Without this a
    // |  function-local write would shadow its upvalue for the rest of the call
    // |  and vanish on return, so closures could not keep state.)
    private void assignVariable(Hashtable scope, String name, Object value) {
        Object stored = value == null ? LUA_NIL : value;
        Hashtable current = scope;
        while (current instanceof ScopeTable) {
            if (((ScopeTable) current).hasOwn(name)) { current.put(name, stored); return; }
            current = ((ScopeTable) current).parent;
        }
        if (current != null) { current.put(name, stored); }
        else { globals.put(name, stored); }
    }
    private Object[] resolveTableAndKey(String varName, Hashtable scope) throws Exception {
        Object table = unwrap(scope.get(varName));
        if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
        Object key = null;
    
        while (peek().type == DOT || peek().type == LBRACKET) {
            if (peek().type == DOT) { consume(DOT); Token field = consume(IDENTIFIER); key = field.value; } 
            else if (peek().type == LBRACKET) { consume(LBRACKET); key = expression(scope); consume(RBRACKET); }

            if (table == null) { throw new Exception("attempt to index a nil value"); }
            if (!(table instanceof Hashtable)) { throw new Exception("attempt to index a non-table value"); }
            if (peek().type == DOT || peek().type == LBRACKET) { table = indexValue(table, key); }
        }
        return new Object[]{table, key};
    }
    // |
    private void collectLabels() throws Exception {
        int savedTokenIndex = tokenIndex;
        if (labels.isEmpty()) { } else { labels.clear(); }

        tokenIndex = 0;
        while (peek().type != EOF) {
            Token token = peek();

            if (token.type == LABEL) { consume(LABEL); labels.put(token.value, new Integer(tokenIndex)); }
            else { consume(); }
        }

        tokenIndex = savedTokenIndex;
    }
    // |
    private static boolean isWhitespace(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r'; }
    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isHexDigit(char c) { return isDigit(c) || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'); }
    private static boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private static boolean isLetterOrDigit(char c) { return isLetter(c) || isDigit(c); }
    // |
    public Object getKernel() { return new LuaFunction(KERNEL); }
    // |
    // Interpreter ownership: script functions take the lock, native ones do
    // not (they never touch the shared parser state).
    private void enterLua() {
        Thread current = Thread.currentThread();
        synchronized (this) {
            while (luaOwner != null && luaOwner != current) {
                try { wait(); } catch (InterruptedException e) { }
            }
            luaOwner = current;
            luaDepth++;
        }
    }
    private void exitLua() {
        synchronized (this) {
            if (luaOwner != Thread.currentThread()) { return; }
            luaDepth--;
            if (luaDepth <= 0) { luaDepth = 0; luaOwner = null; notifyAll(); }
        }
    }
    // | (Give the other thread a turn, then take the interpreter back)
    private void yieldLua(long millis) {
        Thread current = Thread.currentThread();
        int saved;
        synchronized (this) {
            if (luaOwner != current) { return; }
            saved = luaDepth;
            luaDepth = 0;
            luaOwner = null;
            notifyAll();
        }
        try { Thread.sleep(millis < 0 ? 0 : millis); } catch (InterruptedException e) { }
        synchronized (this) {
            while (luaOwner != null && luaOwner != current) {
                try { wait(); } catch (InterruptedException e) { }
            }
            luaOwner = current;
            luaDepth = saved;
        }
    }
    // |
    // Lua Object
    public class LuaFunction implements Runnable, CommandListener, ItemCommandListener, ItemStateListener {
        private Vector params, bodyTokens, argv;
        private Hashtable closureScope, cmds = null; 
        private int MOD = -1;
        public String name = null;
        public String nativeName = null;
        public String defSource = null;
        public String defCode = null;
        public Vector defLineOffsets = null;
        public int defLine = -1;
        // | (Screen)
        private Object root = null;
        private String handler = "";
        // | (su)
        private Form suPromptForm = null;
        private Displayable suPromptPrevious = null;
        private Command suPromptBack = null;
        private Command suPromptRun = null;
        // | 
        // Config.
        LuaFunction(Vector params, Vector bodyTokens, Hashtable closureScope) { this.params = params; this.bodyTokens = bodyTokens; this.closureScope = closureScope; }
        LuaFunction(String handler, Object root) { this.handler = handler; this.root = root; }
        LuaFunction(Hashtable cmds) { this.cmds = cmds; }
        LuaFunction(LuaFunction root) { this.root = root; }
        LuaFunction(int type) { this.MOD = type; }
        // |
        // |
        // | (Main)
        public Object call(Vector args) throws Exception {
            if (MOD != -1) { return internals(args); }
            enterLua();
            try { return callLocked(args); } finally { exitLua(); }
        }
        private Object callLocked(Vector args) throws Exception {

            // Chained scope: locals live here; closure + globals resolve on miss (no per-call copy).
            Hashtable functionScope = new ScopeTable(closureScope, globals);

            int paramCount = params.size();
            boolean hasVararg = paramCount > 0 && params.elementAt(paramCount - 1).equals("...");
            int fixedParamCount = hasVararg ? paramCount - 1 : paramCount;
            for (int i = 0; i < fixedParamCount; i++) {
                String paramName = (String) params.elementAt(i);
                Object argValue = (i < args.size()) ? args.elementAt(i) : null; 
                functionScope.put(paramName, argValue == null ? LUA_NIL : argValue);
            }
            if (hasVararg) {
                Hashtable varargValues = new Hashtable();
                int index = 1;
                for (int i = fixedParamCount; i < args.size(); i++) { Object obj = args.elementAt(i); varargValues.put(luaNumber(index++), obj == null ? LUA_NIL : obj); }
                functionScope.put("...", varargValues);
            }

            int originalTokenIndex = tokenIndex;
            Vector originalTokens = tokens;

            String savedSource = currentSource;
            String savedCode = lastCode;
            Vector savedOffsets = lineOffsets;
            if (defSource != null) { currentSource = defSource; }
            if (defCode != null) { lastCode = defCode; }
            if (defLineOffsets != null) { lineOffsets = defLineOffsets; }

            tokens = bodyTokens;
            tokenIndex = 0;

            Object returnValue = null;
            int callLine = currentLine();
            if (!frameStack.isEmpty()) { Frame parent = (Frame) frameStack.lastElement(); if (callLine > 0) { parent.line = callLine; } }

            frameStack.addElement(new Frame(name == null ? "[anonymous]" : name, currentSource, defLine));
            try {
                while (peek().type != EOF) {
                    Object result = statement(functionScope);
                    
                    if (doreturn) {
                        returnValue = result;
                        doreturn = false;
                        break;
                    }
                }
            }
            catch (Exception e) {
                int throwLine = currentLine();
                if (!frameStack.isEmpty() && throwLine > 0) { ((Frame) frameStack.lastElement()).line = throwLine; }

                Vector snapshot = new Vector();
                for (int f = 0; f < frameStack.size(); f++) { snapshot.addElement(frameStack.elementAt(f)); }
                if (thrownFrames.isEmpty()) { thrownFrames = snapshot; }
                recordThrow(tokenIndex, tokens);
                throw e;
            }
            finally {
                tokenIndex = originalTokenIndex;
                tokens = originalTokens;
                currentSource = savedSource;
                lastCode = savedCode;
                lineOffsets = savedOffsets;
                frameStack.setSize(frameStack.size() - 1);
            }

            return returnValue;
        }
        public Object internals(Vector args) throws Exception {
            Object arg;

            switch (MOD) {
                // Package [global]
                case PRINT:
                    if (args.isEmpty()) { }
                    else {
                        StringBuffer buffer = new StringBuffer(); 
                        for (int i = 0; i < args.size(); i++) {
                            Object a = args.elementAt(i);

                            if (a instanceof Vector) {
                                Vector vv = (Vector) a;
                                for (int j = 0; j < vv.size(); j++) {
                                    buffer.append(toDisplayValue(vv.elementAt(j)));
                                    if (j < vv.size() - 1) { buffer.append("\t"); }
                                }
                            } 
                            else { buffer.append(toDisplayValue(a)); }

                            if (i < args.size() - 1) buffer.append("\t");
                        }

                        midlet.print(buffer.toString(), stdout, id, father); 
                    }

                    break;
                case ERROR: String msg = toLuaString((args.size() > 0) ? args.elementAt(0) : null); throw new Exception(msg.equals("nil") ? "error" : msg);
                case PCALL:
                    if (args.isEmpty()) { return gotbad(1, "pcall", "function expected"); }
                    else {
                        Vector result = new Vector(), fnArgs = new Vector();

                        if (args.elementAt(0) instanceof LuaFunction) {
                            LuaFunction func = (LuaFunction) unwrap(args.elementAt(0));
                            for (int i = 1; i < args.size(); i++) { fnArgs.addElement(unwrap(args.elementAt(i))); }

                            try { 
                                Object value = func.call(fnArgs); 
                                result.addElement(TRUE); 

                                if (value instanceof Vector) { Vector v = (Vector) value; for (int i = 0; i < v.size(); i++) { result.addElement(v.elementAt(i)); } }
                                else { result.addElement(value); }
                            }
                            catch (Exception e) { result.addElement(FALSE); result.addElement(getTraceback(e)); clearThrown(); }
                        }
                        else { result.addElement(FALSE); result.addElement("attempt to call a " + type(args.elementAt(0)) + " value"); } 

                        return result;
                    }
                case ASSERT:
                    if (args.isEmpty()) { return gotbad(1, "assert", "bad argument #1 (value expected)"); }
                    Object assertVal = unwrap(args.elementAt(0));
                    if (assertVal == null || assertVal.equals(FALSE) || assertVal.equals(LUA_NIL)) {
                        Object assertMsg = "assertion failed!";
                        if (args.size() > 1) { Object m = unwrap(args.elementAt(1)); if (m != null) assertMsg = m.toString(); }
                        throw new RuntimeException(assertMsg.toString());
                    }
                    Vector assertResult = new Vector();
                    for (int i = 0; i < args.size(); i++) { assertResult.addElement(unwrap(args.elementAt(i))); }
                    return assertResult;
                case REQUIRE:
                    if (args.isEmpty()) { return gotbad(1, "require", "string expected, got no value"); }
                    else if (args.elementAt(0) instanceof String) {
                        String name = toLuaString(args.elementAt(0));

                        Object cached = requireCache.get(name);
                        if (cached != null) { return (cached == LUA_NIL) ? null : cached; }

                        String code = midlet.getcontent(name, father);
                        if (code.equals("")) { if ((code = midlet.getcontent("/lib/" + name + ".lua", father)).equals("")) { if ((code = midlet.getcontent("/lib/" + name + ".so", father)).equals("")) { throw new Exception("module '" + code + "' not found"); } } } 

                        Object obj = exec(code, null, name);
                        requireCache.put(name, (obj == null) ? LUA_NIL : obj);
                        return obj;
                    } 
                    else { return gotbad(1, "require", "string expected, got " + type(args.elementAt(0))); }
                case LOADS: if (args.isEmpty() || args.elementAt(0) == null) { break; } else { return exec(toLuaString(args.elementAt(0)), args.size() > 1 ? (args.elementAt(1) instanceof Hashtable ? (Hashtable) args.elementAt(1) : null) : null); }
                case PAIRS: 
                    if (args.isEmpty()) { return gotbad(1, "pairs", "table expected, got no value"); } 
                    else {
                        Object t = args.elementAt(0);
                        t = (t == LUA_NIL) ? null : t;
                        if (t == null || t instanceof Hashtable || t instanceof Vector) { return t; }
                        else { return gotbad(1, "pairs", "table expected, got " + type(t)); }
                    }
                case IPAIRS:
                    if (args.isEmpty()) { return gotbad(1, "ipairs", "table expected, got no value"); } 
                    else {
                        Object t = args.elementAt(0);
                        t = (t == LUA_NIL) ? null : t;
                        
                        if (t == null || t instanceof Hashtable || t instanceof Vector) {
                            Hashtable iterator = new Hashtable();
                            iterator.put("__table", t); iterator.put("__index", new Double(0));
                            return iterator;
                        } else { return gotbad(1, "ipairs", "table expected, got " + type(t)); }
                    }
                case GC:
                    if (args.isEmpty()) { System.gc(); break; }
                    else {
                        String opt = toLuaString(args.elementAt(0));

                        if (opt.equals("stop")) { gc = false; }
                        else if (opt.equals("collect") || opt.equals("restart")) { System.gc(); }
                        else if (opt.equals("free")) { return new Double(midlet.runtime.freeMemory() / 1024); }
                        else if (opt.equals("total")) { return new Double(midlet.runtime.totalMemory() / 1024); }
                        else if (opt.equals("count")) { return new Double((midlet.runtime.totalMemory() - midlet.runtime.freeMemory()) / 1024); }
                        else if (opt.equals("step")) { return FALSE; }
                        else if (opt.equals("isrunning")) { return new Boolean(gc); }
                        else if (opt.equals("generational") || opt.equals("incremental")) { return "generational"; }
                        else { return gotbad(1, "collectgarbage", "invalid option '" + opt + "'"); }

                        return new Double(0);
                    }
                case TOSTRING: return toDisplayValue(args.isEmpty() ? gotbad(1, "tostring", "value expected") : args.elementAt(0));
                case TONUMBER:
                    if (args.isEmpty()) { return gotbad(1, "tonumber", "value expected"); }
                    else {
                        Object value = args.elementAt(0);
                        if (value instanceof Double) { return value; }
                        // Lua returns nil for anything that is not a number
                        // (Double.parseDouble would throw on "" / "abc").
                        try { return luaNumber(parseLuaNumber(toLuaString(value))); }
                        catch (RuntimeException e) { return null; }
                    }
                case SELECT:
                    if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "select", "number expected, got no value"); } 
                    else {
                        String idx = toLuaString(args.elementAt(0));
                        if (idx.equals("#")) {
                            if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) { return luaNumber(luaLength((Hashtable) args.elementAt(1))); } 
                            else { return luaNumber(args.size() - 1); }
                        } else {
                            if (args.size() == 1) { return null; }

                            int index = 1;
                            try { index = Integer.parseInt(idx); } 
                            catch (NumberFormatException e) { return gotbad(1, "select", "number expected, got " + type(args.elementAt(0))); }
                            
                            Hashtable result = new Hashtable();
                            if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) {
                                Hashtable varargTable = (Hashtable) args.elementAt(1);
                                int varargSize = varargTable.size();
                                if (index < 0) { index = varargSize + index + 1; }
                                if (index < 1 || index > varargSize) { return null; }

                                int resultIndex = 1;
                                for (int i = index; i <= varargSize; i++) {
                                    Object val = varargTable.get(new Double(i));
                                    if (val != null) { result.put(new Double(resultIndex++), val); }
                                }
                            } else {
                                int argCount = args.size() - 1;
                                if (index < 0) { index = argCount + index + 1; }
                                if (index < 1 || index > argCount) { return null; }

                                int resultIndex = 1;
                                for (int i = index; i <= argCount; i++) {
                                    Object val = args.elementAt(i);
                                    result.put(new Double(resultIndex++), val == null ? LUA_NIL : val);
                                }
                            }
                            return result;
                        }
                    }
                case TYPE: return args.isEmpty() ? gotbad(1, "type", "value expected") : type(args.elementAt(0));
                case GETPROPERTY: if (args.isEmpty()) { break; } else { String query = toLuaString(args.elementAt(0)); return query.startsWith("/") ? System.getProperty(query.substring(1)) : midlet.getAppProperty(query); }
                case RANDOM: Double gen = new Double(midlet.random.nextInt(getNumber(args.isEmpty() ? "100" : toLuaString(args.elementAt(0)), 100))); return args.isEmpty() ? new Double(gen.doubleValue() / 100) : gen;
                case SETMETATABLE:
                    if (args.size() < 2) { return gotbad(1, "setmetatable", "table expected, got no value"); }
                    else {
                        Object table = unwrap(args.elementAt(0));
                        Object mt = unwrap(args.elementAt(1));
                    
                        if (!(table instanceof Hashtable))
                            return gotbad(1, "setmetatable", "table expected, got " + type(table));
                        if (mt != null && !(mt instanceof Hashtable))
                            return gotbad(2, "setmetatable", "nil or table expected, got " + type(mt));
                    
                        ((Hashtable) table).put("__metatable", mt == null ? LUA_NIL : mt);
                        return table;
                    }
                case GETMETATABLE:
                    if (args.isEmpty()) { return gotbad(1, "getmetatable", "table expected, got no value"); }
                    else {
                        Object table = unwrap(args.elementAt(0));
                        if (!(table instanceof Hashtable))
                            return gotbad(1, "getmetatable", "table expected, got " + type(table));
                    
                        Object mt = ((Hashtable) table).get("__metatable");
                        return (mt == LUA_NIL || mt == null) ? null : mt;
                    }
                // Raw access [bypasses metatables]
                case RAWGET:
                    if (args.size() < 2) { return gotbad(1, "rawget", "table expected, got no value"); }
                    else if (!(args.elementAt(0) instanceof Hashtable)) { return gotbad(1, "rawget", "table expected, got " + type(args.elementAt(0))); }
                    else { return ((Hashtable) args.elementAt(0)).get(unwrap(args.elementAt(1))); }
                case RAWSET:
                    if (args.size() < 3) { return gotbad(1, "rawset", "table expected, got no value"); }
                    else if (!(args.elementAt(0) instanceof Hashtable)) { return gotbad(1, "rawset", "table expected, got " + type(args.elementAt(0))); }
                    else {
                        Hashtable target = (Hashtable) args.elementAt(0);
                        target.put(unwrap(args.elementAt(1)), args.elementAt(2));
                        return target;
                    }
                case RAWEQUAL:
                    if (args.size() < 2) { return gotbad(1, "rawequal", "value expected"); }
                    else { return args.elementAt(0) == args.elementAt(1) || (args.elementAt(0) != null && args.elementAt(0).equals(args.elementAt(1))) ? TRUE : FALSE; }
                case RAWLEN:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Hashtable)) { return gotbad(1, "rawlen", "table expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); }
                    else { return luaNumber(luaLength((Hashtable) args.elementAt(0))); }
                // Debug
                case DEBUG_TRACEBACK:
                    String message = args.isEmpty() || unwrap(args.elementAt(0)) == null ? null : toLuaString(args.elementAt(0));
                    int traceLevel = args.size() > 1 && args.elementAt(1) instanceof Double ? ((Double) args.elementAt(1)).intValue() : 1;
                    return tracebackString(message, traceLevel < 1 ? 1 : traceLevel);
                case DEBUG_GETINFO:
                    if (args.isEmpty()) { return gotbad(1, "getinfo", "function or level expected"); }
                    else {
                        Object target = unwrap(args.elementAt(0));
                        Hashtable info = new Hashtable();

                        if (target instanceof LuaFunction) { fillInfo(info, (LuaFunction) target, -1); } 
                        else {
                            int level = target instanceof Double ? ((Double) target).intValue() : 1;
                            if (level < 1) {
                                info.put("what", "C"); info.put("source", "=[C]"); info.put("short_src", "[C]"); info.put("currentline", luaNumber(-1));
                            } else if (level > frameStack.size()) {
                                // main chunk: there is no Frame for top-level code
                                info.put("what", "main"); info.put("source", currentSource == null || currentSource.length() == 0 ? "=?" : "@" + currentSource);
                                info.put("short_src", currentSource == null ? "?" : currentSource); info.put("currentline", luaNumber(currentLine())); info.put("linedefined", luaNumber(0));
                            } else {
                                fillInfo(info, null, frameStack.size() - level);
                            }
                        }
                        return info;
                    }
                case DEBUG_GETREGISTRY: return globals;
                // Package [os]
                case EXEC: return exec(args); 
                case GETENV: return args.isEmpty() ? midlet.attributes : midlet.attributes.get(toLuaString(args.elementAt(0)));
                case SETENV: 
                    if (args.isEmpty()) { } 
                    else {
                        Object value = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;

                        if (value == null) { midlet.attributes.remove(toLuaString(args.elementAt(0))); }
                        else { midlet.attributes.put(toLuaString(args.elementAt(0)), value); }
                        break;
                    }
                case CLOCK: return luaNumber(System.currentTimeMillis() - uptime);
                case SETLOC: if (args.isEmpty()) { break; } else { midlet.attributes.put("LOCALE", toLuaString(args.elementAt(0))); break; }
                case EXIT: exit(args); break;
                case DATE: return new java.util.Date().toString();
                case GETPID: return args.isEmpty() || args.elementAt(0) == null ? PID : midlet.getpid(toLuaString(args.elementAt(0)));
                case GETPROC:
                    if (args.isEmpty()) {
                        Hashtable result = new Hashtable();
                        for (Enumeration procs = midlet.sys.keys(); procs.hasMoreElements();) {
                            String pid = (String) procs.nextElement();

                            result.put(pid, ((Process) midlet.sys.get(pid)).name);
                        }
                        return result;
                    }
                    else {
                        String pid = toLuaString(args.elementAt(0)).trim();
                        Process process = (Process) midlet.sys.get(pid);

                        if (process != null) {
                            if (process.uid != id && id != 0) { return gotbad(1, "getproc", "permissiond denied"); }

                            if (args.size() > 1) { return process.db.get(toLuaString(args.elementAt(1)).trim()); } 
                            else { return gotbad(2, "getproc", "field expected, got no value"); }
                        } 
                        else { return gotbad(1, "getproc", "process not found"); }
                    }
                case SETPROC:
                    if (args.isEmpty()) { }
                    else if (args.elementAt(0) instanceof Boolean) { kill = ((Boolean) args.elementAt(0)).booleanValue(); }
                    else {
                        String attribute = toLuaString(args.elementAt(0)).trim().toLowerCase();
                        Object value = args.size() < 2 ? null : args.elementAt(1);

                        if (attribute.equals("owner")) { return gotbad(1, "setproc", "permission denied"); } 
                        else if (attribute.equals("scope")) { if (value instanceof Hashtable) { proc.scope = (Hashtable) value; } else { return gotbad(1, "setproc", "table expected"); } }
                        else if (attribute.equals("name")) { if (value != null) { proc.name = toLuaString(value); } else { return gotbad(1, "setproc", "string expected"); } }
                        else if (attribute.equals("handler")) { if (value instanceof LuaFunction) { proc.handler = value; kill = false; } else { return gotbad(1, "setproc", "function expected"); } }
                        else if (attribute.equals("cmd")) { if (value != null) { proc.cmd = toLuaString(value); } else { return gotbad(1, "setproc", "string expected"); } }
                        else if (attribute.equals("sighandler")) { if (value instanceof LuaFunction) { proc.sighandler = value; } else { return gotbad(1, "setproc", "function expected"); } }
                        else if (attribute.equals("screen")) { if (value instanceof Displayable) { proc.screen = (Displayable) value; } else { return gotbad(1, "setproc", "Displayable expected"); } }
                        else if (attribute.equals("stdout")) { if (value != null) { proc.stdout = value; stdout = value; } }
                        else { if (value == null) { proc.db.remove(attribute); } else { proc.db.put(attribute, value); } }
                    } 
                case GETCWD: return father.get("PWD");
                case REQUEST:
                    if (args.isEmpty()) { return gotbad(1, "request", "string expected, got no value"); }
                    else if (args.size() < 2) { return gotbad(2, "request", "value expected, got no value"); }
                    else if (midlet.sys.containsKey(toLuaString(args.elementAt(0)))) {
                        Process process = (Process) midlet.sys.get(toLuaString(args.elementAt(0)));
                        if (process.lua != null && process.handler != null) {
                            Lua lua = (Lua) process.lua;
                            Vector arg = new Vector(); arg.addElement(toLuaString(args.elementAt(1))); arg.addElement(args.size() > 2 ? args.elementAt(2) : null); arg.addElement(father); arg.addElement(PID); arg.addElement(new Double(id));
                            Object response = null;

                            try { response = ((Lua.LuaFunction) process.handler).call(arg); }
                            catch (Exception e) { return lua.getTraceback(e); } 
                            catch (Error e) { midlet.print(midlet.getCatch(e), stdout, id, father); return new Double(lua.status); }

                            return response;
                        } 
                        else { return gotbad(1, "request", "not a service"); }
                    } 
                    else { return gotbad(1, "request", "process not found"); }
                case GETUID: if (args.isEmpty() || args.elementAt(0) == null) { return new Double(id); } return new Double(midlet.getUserID(toLuaString(args.elementAt(0))));
                case CHDIR: return chdir(args);
                case SU:
                    if (args.isEmpty()) { return gotbad(1, "su", "username and password expected"); } 
                    else {
                        String user = toLuaString(args.elementAt(0)), query = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;
                        if (user.equals(midlet.username)) { id = 1000; father.put("USER", user); father.put("PWD", "/home/"); proc.uid = 1000; return new Double(0); }
                        else if (midlet.userID.containsKey(user)) { id = midlet.getUserID(user); father.put("USER", user); father.put("PWD", "/home/"); proc.uid = id; return new Double(0); }
                        else if (query == null) { return gotbad(2, "su", "string expected, got nil"); }
                        else if (user.equals("root") && midlet.passwd(query)) { id = 0; father.put("USER", "root"); father.put("PWD", "/home/"); proc.uid = 0; return new Double(0); }
                        else { return new Double(13); }
                    }
                case REMOVE: return args.isEmpty() ? (Double) gotbad(1, "remove", "string expected, got no value") : new Double(midlet.deleteFile(toLuaString(args.elementAt(0)), id, father));
                case SCOPE:
                    if (args.isEmpty()) { return father; }
                    else {
                        if (args.elementAt(0) instanceof Hashtable) { 
                            father = (Hashtable) args.elementAt(0);

                            if (father.containsKey("USER")) {
                                String user = (String) father.get("USER");
                                if (user.equals("root")) {
                                    if (id == 1000) {
                                        father.put("USER", midlet.loadRMS("OpenRMS", 1));
                                    }
                                }
                            }

                            break;
                        }
                        else { return gotbad(1, "scope", "table expected, got " + type(args.elementAt(0))); }
                    }
                case JOIN: return args.isEmpty() ? (String) gotbad(1, "join", "string expected, got no value") : (String) midlet.joinpath(toLuaString(args.elementAt(0)), father);
                case MKDIR:
                    if (args.isEmpty()) { }
                    else {
                        String dir = toLuaString(args.elementAt(0));
                        dir = (String) midlet.joinpath(dir, father);
                        if (dir.length() > 1 && !dir.endsWith("/")) { dir = dir + "/"; }

                        String chk = midlet.redirect(dir);
                        if (!chk.equals("/mnt/") && chk.startsWith("/mnt/")) {
                            FileConnection fc = null;
                            try {
                                fc = (FileConnection) Connector.open("file:///" + chk.substring(5), Connector.READ_WRITE);
                                
                                if (fc.exists()) { return new Double(128); } else { fc.mkdir(); return new Double(0); }
                            }
                            catch (Exception e) { return e instanceof SecurityException ? 13 : 1; }
                            finally { if (fc != null) { try { fc.close(); } catch (Exception e) { } } }
                        } else if (midlet.vfsDirIndex(dir) != -1) {
                            if (dir.startsWith("/bin/") || dir.startsWith("/etc/") || dir.startsWith("/lib/") || dir.startsWith("/root/") || dir.startsWith("/boot/")) {
                                if (id != 0) { return new Double(13); }
                            }
                            if (midlet.fs.containsKey(dir)) { return new Double(128); }
                            midlet.registerVfsDir(dir);
                            return new Double(0);
                        } else { return new Double(5); }
                    }
                // Package [io]
                case READ:
                    if (args.isEmpty()) { return stdout instanceof StringItem ? ((StringItem) stdout).getText() : stdout instanceof StringBuffer ? ((StringBuffer) stdout).toString() : stdout instanceof String ? midlet.getcontent((String) stdout, father) : ""; }
                    else {
                        arg = args.elementAt(0);

                        if (arg instanceof InputStream) { return midlet.read((InputStream) arg, args.size() > 1 && args.elementAt(1) instanceof Double ? ((Double) args.elementAt(1)).intValue() : 1024, false); }
                        else if (arg instanceof StringBuffer) { ((StringBuffer) arg).toString(); }
                        else if (arg instanceof OutputStream) { return gotbad(1, "read", "input stream expected, got output"); } 
                        else {
                            String path = toLuaString(arg);
                            if (path.equals("/dev/stdout")) {
                                if (stdout instanceof StringItem) return ((StringItem) stdout).getText();
                                else if (stdout instanceof StringBuffer) return ((StringBuffer) stdout).toString();
                                return "";
                            }
                            if (path.equals("/dev/stdin")) return "";
                            return midlet.getcontent(path, father);
                        }
                    }
                case WRITE:
                    if (args.isEmpty()) { break; }
                    else {
                        Object buffer = args.elementAt(0), target = args.size() > 1 ? args.elementAt(1) : null, how = args.size() > 2 ? args.elementAt(2) : null;
                        boolean mode = how != null && toLuaString(how).equals("a");

                        if (target instanceof OutputStream) {
                            OutputStream outputStream = (OutputStream) target;

                            if (buffer instanceof ByteArrayOutputStream) {
                                ByteArrayOutputStream baos = (ByteArrayOutputStream) buffer;
                                byte[] bytes = baos.toByteArray();
                                outputStream.write(bytes);
                            } else {
                                outputStream.write(toLuaString(buffer).getBytes("UTF-8"));
                            }
                            outputStream.flush();
                            return new Double(0);
                        }
                        else if (buffer instanceof OutputStream) {
                            OutputStream outputStream = (OutputStream) buffer;

                            if (target instanceof ByteArrayOutputStream) {
                                ByteArrayOutputStream baos = (ByteArrayOutputStream) target;
                                byte[] bytes = baos.toByteArray();
                                outputStream.write(bytes);
                            } else {
                                outputStream.write(toLuaString(target).getBytes("UTF-8"));
                            }
                            outputStream.flush();
                            return new Double(0);
                        }
                        else if (target instanceof StringBuffer) { StringBuffer sb = (StringBuffer) target; String content = toLuaString(buffer); sb.append(content); return new Double(0); }
                        else if (buffer instanceof ByteArrayOutputStream) {
                            ByteArrayOutputStream baos = (ByteArrayOutputStream) buffer;
                            byte[] bytes = baos.toByteArray();
                            String filename = target != null ? toLuaString(target) : "/dev/stdout";
                            if (mode) { return new Double(midlet.write(filename, midlet.getcontent(filename, father) + new String(bytes, "UTF-8"), id, father)); }
                            else { return new Double(midlet.write(filename, bytes, id, father)); }
                        }
                        else {
                            String content = toLuaString(buffer), filename = target != null ? toLuaString(target) : "/dev/stdout";
                            if (filename.equals("/dev/stdout")) {
                                if (stdout instanceof StringItem) {
                                    String cur = ((StringItem) stdout).getText();
                                    ((StringItem) stdout).setText(mode ? (cur == null || cur.length() == 0 ? content : cur + "\n" + content) : content);
                                } else if (stdout instanceof StringBuffer) {
                                    if (mode) { ((StringBuffer) stdout).append("\n").append(content); }
                                    else { ((StringBuffer) stdout).setLength(0); ((StringBuffer) stdout).append(content); }
                                }
                                return new Double(0);
                            }
                            if (filename.equals("/dev/stdin")) return new Double(0);
                            return new Double(midlet.write(filename, mode ? midlet.getcontent(filename, father) + content : content, id, father));
                        }
                    }
                case CLOSE:
                    if (args.isEmpty()) { }
                    else {
                        for (int i = 0; i < args.size(); i++) {
                            arg = args.elementAt(i);

                            if (arg instanceof ServerSocketConnection) { ((ServerSocketConnection) arg).close(); }
                            else if (arg instanceof StreamConnection) { ((StreamConnection) arg).close(); }
                            else if (arg instanceof InputStream) { ((InputStream) arg).close(); }
                            else if (arg instanceof OutputStream) { ((OutputStream) arg).close(); }
                            else if (arg instanceof StringBuffer || arg instanceof StringItem) { }
                            else if (arg instanceof Player) { Player player = (Player) arg; player.stop(); player.deallocate(); player.close(); }
                            else { return gotbad(i + 1, "close", "stream expected, got " + type(arg)); }

                            proc.net.remove(arg); break;
                        }
                    }
                    break;
                case OPEN: if (args.isEmpty()) { return new ByteArrayOutputStream(); } else { try { return midlet.getInputStream(toLuaString(args.elementAt(0)), father); } catch (Exception e) { return null; } }
                case POPEN: return popen(args);
                case DIRS: return dirs(args);
                case SETOUT: if (args.isEmpty()) { } else { stdout = args.elementAt(0); }
                    break;
                case MOUNT:
                    if (args.isEmpty()) { break; }
                    else {
                        String struct = toLuaString(args.elementAt(0));
                        
                        if (struct == null || struct.length() == 0) { midlet.fs.clear(); } 
                        String[] lines = midlet.split(struct, '\n'); 
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i].trim(); 
                            int div = line.indexOf('='); 
                            if (line.startsWith("#") || line.length() == 0 || div == -1) { continue; } 
                            else { 
                                String base = line.substring(0, div).trim(); 
                                String[] files = midlet.split(line.substring(div + 1).trim(), ','); 
                                Vector content = new Vector(); 
                                content.addElement(".."); 
                                for (int j = 0; j < files.length; j++) { 
                                    if (!content.contains(files[j])) { 
                                        if (files[j].endsWith("/")) { 
                                            Vector dir = new Vector(); 
                                            dir.addElement(".."); 
                                            midlet.fs.put(base + files[j], dir); 
                                        } 
                                        
                                        content.addElement(files[j]);
                                    }
                                } 
                                midlet.fs.put(base, content);
                            } 
                        }
                        midlet.restoreVfsMounts();
                        break;
                    }
                case GEN: return new StringBuffer();
                case COPY:
                    if (args.size() < 2) { return gotbad(1, "copy", "wrong number of arguments"); }
                
                    Object source = args.elementAt(0), target = args.elementAt(1);

                    if (source instanceof InputStream) {
                        InputStream in = (InputStream) source;

                        if (target instanceof OutputStream) {
                            OutputStream os = (OutputStream) target;
                            
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) { os.write(buffer, 0, bytesRead); }
                            os.flush();
                            return new Double(0);
                        }
                        else if (target instanceof StringBuffer || target instanceof String) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024], data;
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }

                            data = baos.toByteArray(); baos.close();    
                        
                            if (target instanceof StringBuffer) { ((StringBuffer) target).append(new String(data, "UTF-8")); return new Double(0); }
                            else { return new Double(midlet.write(toLuaString(target), data, id, father)); }
                        }
                    }
                    else if (source instanceof StringBuffer) {
                        StringBuffer in = (StringBuffer) source;

                        if (target instanceof OutputStream) {
                            OutputStream os = (OutputStream) target;
                            
                            os.write(in.toString().getBytes("UTF-8")); os.flush();
                            return new Double(0);
                        }
                        else if (target instanceof StringBuffer || target instanceof String) {
                            if (target instanceof StringBuffer) { ((StringBuffer) target).append(in.toString()); return new Double(0); }
                            else { return new Double(midlet.write(toLuaString(target), in.toString().getBytes("UTF-8"), id, father)); }
                        }
                    }
                    else if (source instanceof String) {
                        String file = (String) source;

                        if (target instanceof OutputStream) {
                            OutputStream os = (OutputStream) target;
                            
                            InputStream is = midlet.getInputStream(file, father);
                            if (is == null) { return new Double(127); }
                            
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) { os.write(buffer, 0, bytesRead); }
                            os.flush(); is.close();
                            return new Double(0);
                        }
                        else if (target instanceof StringBuffer || target instanceof String) {
                            if (target instanceof StringBuffer) { ((StringBuffer) target).append(midlet.read(file, father)); }
                            else { 
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                InputStream is = midlet.getInputStream(file, father);
                                if (is == null) { return new Double(127); }

                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) { baos.write(buffer, 0, bytesRead); }

                                return new Double(midlet.write(toLuaString(target), baos.toByteArray(), id, father));
                            }
                        }
                    }
                    else if (source instanceof ByteArrayOutputStream) {
                        ByteArrayOutputStream baos = (ByteArrayOutputStream) source;
                        byte[] data = baos.toByteArray();
                        
                        if (target instanceof OutputStream) { OutputStream os = (OutputStream) target; os.write(data); os.flush(); } 
                        else if (target instanceof StringBuffer) { ((StringBuffer) target).append(new String(data, "UTF-8")); } 
                        else if (target instanceof String) { return new Double(midlet.write(toLuaString(target), data, id, father)); }
                    }

                    return new Double(0);
                // Package [table]
                case TB_INSERT:
                    if (args.size() < 2) { return gotbad(1, "insert", "wrong number of arguments"); }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tObj;
                            if (isListTable(table)) {
                                int pos = luaLength(table) + 1; // default: append
                                Object value = unwrap(args.elementAt(1));
                                if (args.size() >= 3) {
                                    Object posObj = unwrap(args.elementAt(2));
                                    if (!(posObj instanceof Double)) { return gotbad(3, "insert", "number expected, got " + type(posObj)); }
                                    pos = ((Double) posObj).intValue();
                                    if (pos < 0 || pos > luaLength(table) + 1) { return gotbad(3, "insert", "position out of bounds"); }
                                    // value is still args[1]
                                }
                                // Shift elements to make room (shift right)
                                for (int i = luaLength(table); i >= pos; i--) {
                                    Object val = table.get(new Double(i));
                                    if (val != null) { table.put(new Double(i + 1), val); }
                                    else { table.remove(new Double(i)); }
                                }
                                table.put(new Double(pos), value == null ? LUA_NIL : value);
                                return null;
                            } else { return gotbad(1, "insert", "table must be array-like"); }
                        } else { return gotbad(1, "insert", "table expected, got " + type(tObj)); }
                    }
                case TB_CONCAT:
                    if (args.isEmpty()) { return gotbad(1, "concat", "table expected, got no value"); }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tObj;
                            if (isListTable(table)) {
                                Vector list = toVector(table);
                                String sep = args.size() > 1 ? toLuaString(unwrap(args.elementAt(1))) : "";
                                int i = args.size() > 2 ? ((Double) unwrap(args.elementAt(2))).intValue() : 1;
                                int j = args.size() > 3 ? ((Double) unwrap(args.elementAt(3))).intValue() : list.size();
                                if (i < 1 || j > list.size() || i > j) { return ""; }
                                StringBuffer sb = new StringBuffer();
                                for (int k = i - 1; k < j; k++) {
                                    sb.append(toLuaString(list.elementAt(k)));
                                    if (k < j - 1) sb.append(sep);
                                }
                                return sb.toString();
                            } else { return gotbad(1, "concat", "table must be array-like"); }
                        } else { return gotbad(1, "concat", "table expected, got " + type(tObj)); }
                    }
                case TB_REMOVE:
                    if (args.isEmpty()) { return gotbad(1, "remove", "table expected, got no value"); }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tObj;
                            if (isListTable(table)) {
                                int pos = luaLength(table); // default: remove the last
                                if (args.size() >= 2) {
                                    Object posObj = unwrap(args.elementAt(1));
                                    if (!(posObj instanceof Double)) { return gotbad(2, "remove", "number expected, got " + type(posObj)); }
                                    pos = ((Double) posObj).intValue();
                                    if (pos < 1 || pos > luaLength(table)) { return gotbad(2, "remove", "position out of bounds"); }
                                }
                                Object removed = table.get(new Double(pos));
                                if (removed != null) {
                                    table.remove(new Double(pos));
                                    // Shift elements to fill the gap (shift left)
                                    for (int i = pos; i < luaLength(table); i++) {
                                        Object val = table.get(new Double(i + 1));
                                        if (val != null) {
                                            table.put(new Double(i), val);
                                        } else {
                                            table.remove(new Double(i));
                                        }
                                    }
                                    // Remove the last index if empty
                                    if (table.containsKey(new Double(luaLength(table)))) {
                                        table.remove(new Double(luaLength(table)));
                                    }
                                }
                                return removed == null ? LUA_NIL : removed;
                            } else { return gotbad(1, "remove", "table must be array-like"); }
                        } else { return gotbad(1, "remove", "table expected, got " + type(tObj)); }
                    }
                case TB_SORT:
                    if (args.isEmpty()) { return gotbad(1, "sort", "table expected, got no value"); }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tObj;
                            if (isListTable(table)) {
                                Vector list = toVector(table);
                                // Simple bubble sort (no custom comparator)
                                for (int i = 0; i < list.size() - 1; i++) {
                                    for (int j = 0; j < list.size() - i - 1; j++) {
                                        Object a = list.elementAt(j), b = list.elementAt(j + 1);
                                        int cmp = compareLua(a, b);
                                        if (cmp > 0) {
                                            list.setElementAt(b, j);
                                            list.setElementAt(a, j + 1);
                                        }
                                    }
                                }
                                // Rebuild the sorted table (the metatable lives in the table, so keep it)
                                Hashtable guard = getMetatable(table);
                                table.clear();
                                if (guard != null) { table.put("__metatable", guard); }
                                for (int i = 0; i < list.size(); i++) {
                                    table.put(new Double(i + 1), list.elementAt(i));
                                }
                                return null;
                            } else { return gotbad(1, "sort", "table must be array-like"); }
                        } else { return gotbad(1, "sort", "table expected, got " + type(tObj)); }
                    }
                case TB_MOVE:
                    if (args.size() < 4) { return gotbad(1, "move", "insufficient arguments (need table, from, to, len)"); }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tObj;
                            if (isListTable(table)) {
                                Object fromObj = unwrap(args.elementAt(1));
                                Object toObj = unwrap(args.elementAt(2));
                                Object lenObj = unwrap(args.elementAt(3));
                                if (!(fromObj instanceof Double) || !(toObj instanceof Double) || !(lenObj instanceof Double)) {
                                    return gotbad(1, "move", "from/to/len must be numbers");
                                }
                                int from = ((Double) fromObj).intValue();
                                int to = ((Double) toObj).intValue();
                                int len = ((Double) lenObj).intValue();
                                int a = args.size() > 4 ? ((Double) unwrap(args.elementAt(4))).intValue() : 1;
                                int b = a + len - 1;
                                if (from < 1 || to < 1 || len < 0 || a < 1 || b > luaLength(table)) {
                                    return gotbad(1, "move", "bounds out of range");
                                }
                                Vector list = toVector(table);
                                // Extract the slice to move
                                Vector slice = new Vector();
                                for (int i = 0; i < len; i++) {
                                    int idx = from + i - 1;
                                    if (idx >= 0 && idx < list.size()) {
                                        slice.addElement(list.elementAt(idx));
                                    }
                                }
                                // Remove the original block (shift left)
                                for (int i = from + len - 1; i >= from; i--) {
                                    if (i - 1 >= 0 && i - 1 < list.size()) {
                                        list.removeElementAt(i - 1);
                                    }
                                }
                                // Insert the slice at the new position
                                for (int i = 0; i < slice.size(); i++) {
                                    list.insertElementAt(slice.elementAt(i), to + i - 1);
                                }
                                // Rebuild the table (keep the metatable)
                                Hashtable guard = getMetatable(table);
                                table.clear();
                                if (guard != null) { table.put("__metatable", guard); }
                                for (int i = 0; i < list.size(); i++) {
                                    table.put(new Double(i + 1), list.elementAt(i));
                                }
                                return table;
                            } else { return gotbad(1, "move", "table must be array-like"); }
                        } else { return gotbad(1, "move", "table expected, got " + type(tObj)); }
                    }
                case TB_UNPACK:
                    if (args.isEmpty()) { return gotbad(1, "unpack", "table expected, got no value"); }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable table = (Hashtable) tObj;
                            if (isListTable(table)) {
                                Vector list = toVector(table);
                                int i = args.size() > 1 ? ((Double) unwrap(args.elementAt(1))).intValue() : 1;
                                int j = args.size() > 2 ? ((Double) unwrap(args.elementAt(2))).intValue() : list.size();
                                if (i < 1 || j > list.size() || i > j) { return new Vector(); }
                                Vector result = new Vector();
                                for (int k = i - 1; k < j; k++) {
                                    result.addElement(list.elementAt(k));
                                }
                                return result;
                            } else { return gotbad(1, "unpack", "table must be array-like"); }
                        } else { return gotbad(1, "unpack", "table expected, got " + type(tObj)); }
                    }
                case TB_DECODE:
                    if (args.isEmpty()) { return gotbad(1, "decode", "string expected, got no value"); }
                    else {
                        String text = toLuaString(args.elementAt(0));
                        if (text.equals("")) { return new Hashtable(); }
                        Hashtable properties = new Hashtable();

                        String[] lines = midlet.split(text, '\n');
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];
                            if (line.startsWith("#")) { }
                            else { 
                                int equalIndex = line.indexOf('='); 
                                if (equalIndex > 0 && equalIndex < line.length() - 1) { properties.put(line.substring(0, equalIndex).trim(), midlet.getpattern(line.substring(equalIndex + 1).trim())); } 
                            }
                        }
                        return properties;
                    }
                case TB_PACK:
                    Hashtable packed = new Hashtable();
                    for (int i = 0; i < args.size(); i++) {
                        Object val = args.elementAt(i);
                        packed.put(new Double(i + 1), val == null ? LUA_NIL : val);
                    }
                    packed.put("n", new Double(args.size()));
                    return packed;
                // Package [base64]
                case BASE64_ENCODE:
                    if (args.isEmpty()) { return gotbad(1, "encode", "string or table expected, got no value"); }
                    
                    arg = args.elementAt(0);
                    byte[] data;
                    
                    if (arg instanceof Hashtable) {
                        Hashtable table = (Hashtable) arg;
                        if (isListTable(table)) {
                            Vector vec = toVector(table);
                            data = new byte[vec.size()];
                            for (int i = 0; i < vec.size(); i++) {
                                Object val = vec.elementAt(i);
                                if (val instanceof Double) {
                                    double d = ((Double) val).doubleValue();
                                    if (d < 0 || d > 255) {
                                        return gotbad(1, "encode", "byte value out of range (0-255)");
                                    }
                                    data[i] = (byte) d;
                                } else {
                                    return gotbad(1, "encode", "table must contain numbers");
                                }
                            }
                        } else {
                            return gotbad(1, "encode", "table must be array-like");
                        }
                    } else if (arg instanceof String) {
                        data = toLuaString(arg).getBytes("UTF-8");
                    } else {
                        return gotbad(1, "encode", "string or table expected, got " + type(arg));
                    }
                
                    return midlet.encodeBase64(data);
                case BASE64_DECODE:
                    if (args.isEmpty()) { return gotbad(1, "decode", "string expected, got no value"); }
                    else {
                        String encoded = toLuaString(args.elementAt(0));
                        byte[] decoded = midlet.decodeBase64(encoded);

                        if (args.size() > 1) { return new ByteArrayInputStream(decoded); }

                        if (decoded == null) { return null; }

                        Hashtable result = new Hashtable();
                        for (int i = 0; i < decoded.length; i++) { result.put(new Double(i + 1), new Double(decoded[i] & 0xFF)); }
                        return result;
                    }  
                // Package [socket]
                case CONNECT:
                    if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "connect", "string expected, got no value"); }
                    else {
                        Vector result = new Vector();

                        SocketConnection conn = (SocketConnection) Connector.open(toLuaString(args.elementAt(0)));
                            
                        result.addElement(conn); result.addElement(conn.openInputStream()); result.addElement(conn.openOutputStream()); result.addElement(args.elementAt(0)); result.addElement(new Double(id));
                        proc.net.put(toLuaString(args.elementAt(0)), result);

                        return result;
                    } 
                case PEER: case DEVICE:
                    if (args.isEmpty()) { return gotbad(1, MOD == PEER ? "peer" : "device", "connection expected, got no value"); }
                    else {
                        if (args.elementAt(0) instanceof SocketConnection) {
                            SocketConnection conn = (SocketConnection) args.elementAt(0);

                            Vector result = new Vector();
                            result.addElement(MOD == PEER ? conn.getAddress() : conn.getLocalAddress());
                            result.addElement(MOD == PEER ? conn.getPort() : conn.getLocalPort());
                            return result;
                        } else { return gotbad(1, MOD == PEER ? "peer" : "device", "connection expected, got " + type(args.elementAt(0))); }
                    }
                case SERVER:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Double)) { return gotbad(1, "server" , "number expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); }
                    else {
                        ServerSocketConnection server = (ServerSocketConnection) Connector.open("socket://:" + toLuaString(args.elementAt(0)));
                        midlet.servers.put(toLuaString(args.elementAt(0)), server);
                        proc.net.put(toLuaString(args.elementAt(0)), server);
                        return server;
                    }
                case ACCEPT:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof ServerSocketConnection)) { return gotbad(1, "server" , "server expected, got " + (args.isEmpty() ? " no value" : type(args.elementAt(0)))); }
                    else {
                        Vector result = new Vector();

                        SocketConnection conn = (SocketConnection) ((ServerSocketConnection) args.elementAt(0)).acceptAndOpen();
                            
                        result.addElement(conn); result.addElement(conn.openInputStream()); result.addElement(conn.openOutputStream());
                        proc.net.put("socket://:" + ((ServerSocketConnection) args.elementAt(0)).getLocalPort(), result);

                        return result;
                    }
                // Package [socket.http]
                case HTTP_GET: case HTTP_POST: return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : http(MOD == HTTP_GET ? "GET" : "POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null, false));
                case HTTP_RGET: case HTTP_RPOST: return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_RGET ? "rget" : "rpost", "string expected, got no value") : http((MOD == HTTP_GET || MOD == HTTP_RGET) ? "GET" : "POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null, true));
                // Package [push]
                case PUSH_REGISTER:
                    if (args.size() < 3) { return gotbad(1, "register", "insufficient arguments"); }
                    else {
                        String connection = toLuaString(args.elementAt(0)), filter = toLuaString(args.elementAt(1));
                        String midletClass = toLuaString(args.elementAt(2)), sender = args.size() > 3 ? toLuaString(args.elementAt(3)) : null;
                        
                        try { PushRegistry.registerConnection(connection, midletClass, filter); return TRUE; }
                        catch (ClassNotFoundException e) { return gotbad(3, "register", "MIDlet class not found: " + midletClass); }
                        catch (Exception e) { return gotbad(1, "register", midlet.getCatch(e)); }
                    }
                case PUSH_UNREGISTER:
                    if (args.isEmpty()) { return gotbad(1, "unregister", "connection string expected"); }
                    else {
                        String connection = toLuaString(args.elementAt(0));
                        
                        try { return new Boolean(PushRegistry.unregisterConnection(connection)); }
                        catch (Exception e) { return gotbad(1, "unregister", midlet.getCatch(e)); }
                    }
                case PUSH_LIST:
                    if (args.isEmpty()) { return gotbad(1, "list", "connection string expected"); }
                    else {
                        String connection = toLuaString(args.elementAt(0));
                
                        try {
                            String[] connections = PushRegistry.listConnections(false);
                            Hashtable result = new Hashtable(); int j = 1;
                            
                            if (connections != null) { for (int i = 0; i < connections.length; i++) { if (connections[i].startsWith(connection) || connection.equals("*")) { result.put(new Double(j), connections[i]); j++; } } }
                            
                            return result;
                        } catch (Exception e) { return gotbad(1, "list", midlet.getCatch(e)); }
                    }
                case PUSH_PENDING: try { return new Boolean(PushRegistry.listConnections(true).length > 0); } catch (Exception e) { return gotbad(1, "hasPending", midlet.getCatch(e)); }
                case PUSH_SET_ALARM: if (args.size() < 2) { return gotbad(1, "setAlarm", "insufficient arguments"); } else { String midletClass = toLuaString(args.elementAt(0)); long time = ((Double) args.elementAt(1)).longValue(); try { return new Double(PushRegistry.registerAlarm(midletClass, time)); } catch (ClassNotFoundException e) { return gotbad(1, "setAlarm", "MIDlet class not found: " + e.getMessage()); } catch (ConnectionNotFoundException e) { return gotbad(1, "setAlarm", "Connection not found: " + e.getMessage()); } } 
                // Package [graphics]
                case DISPLAY:
                    if (args.isEmpty()) { }
                    else {
                        Object screen = args.elementAt(0);

                        if (screen instanceof Alert && args.size() > 1) { kill = false; midlet.display.setCurrent((Alert) screen, (Displayable) args.elementAt(1)); }
                        else if (screen instanceof Displayable) { kill = false; midlet.display.setCurrent((Displayable) screen); }
                        else { return gotbad(1, "display", "screen expected, got " + type(screen)); }
                        break;
                    }
                case NEW:
                    if (args.size() < 2) { return gotbad(1, "graphics.new", "wrong number of arguments"); }
                    
                    String type = toLuaString(args.elementAt(0)), title = args.elementAt(1) == null ? null : toLuaString(args.elementAt(1));
                    Object content = args.size() > 2 ? args.elementAt(2) : null;

                    if (type.equals("alert")) {
                        // Optional 4th argument: { type = "info|warning|error|alarm|confirm", timeout = ms }
                        AlertType alertType = AlertType.INFO;
                        int alertTimeout = Alert.FOREVER;

                        if (args.size() > 3 && args.elementAt(3) instanceof Hashtable) {
                            Hashtable field = (Hashtable) args.elementAt(3);
                            String kind = getFieldValue(field, "type", "info").toLowerCase();

                            if (kind.equals("warning")) { alertType = AlertType.WARNING; }
                            else if (kind.equals("error")) { alertType = AlertType.ERROR; }
                            else if (kind.equals("alarm")) { alertType = AlertType.ALARM; }
                            else if (kind.equals("confirm") || kind.equals("confirmation")) { alertType = AlertType.CONFIRMATION; }

                            int millis = getFieldNumber(field, "timeout", Alert.FOREVER);
                            if (millis > 0) { alertTimeout = millis; }
                        }

                        Alert alert = new Alert(title, content != null ? toLuaString(content) : "", null, alertType);
                        alert.setTimeout(alertTimeout);
                        return alert;
                    } 
                    else if (type.equals("edit")) { return new TextBox(title, content != null ? toLuaString(content) : "", 31522, TextField.ANY); } 
                    else if (type.equals("list")) { return new List(title, (type = content != null ? toLuaString(content) : "implicit").equals("exclusive") ? List.EXCLUSIVE : type.equals("multiple") ? List.MULTIPLE : List.IMPLICIT); } 
                    else if (type.equals("screen")) { return new Form(title); } 
                    else if (type.equals("command")) {
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable cmdTable = (Hashtable) args.elementAt(1);
                            
                            Object labelObj = cmdTable.get("label");
                            Object typeObj = cmdTable.get("type");
                            Object priorityObj = cmdTable.get("priority");
                            
                            String label = (labelObj != null && labelObj != LUA_NIL) ? toLuaString(labelObj) : "Command";
                            String cmdType = (typeObj != null && typeObj != LUA_NIL) ? toLuaString(typeObj) : "screen";
                            int priority = 1;
                            
                            if (priorityObj != null && priorityObj != LUA_NIL && priorityObj instanceof Double) {
                                priority = ((Double) priorityObj).intValue();
                            }
                            
                            int commandType;
                            if (cmdType.equals("back")) commandType = Command.BACK;
                            else if (cmdType.equals("ok")) commandType = Command.OK;
                            else if (cmdType.equals("cancel")) commandType = Command.CANCEL;
                            else if (cmdType.equals("help")) commandType = Command.HELP;
                            else if (cmdType.equals("stop")) commandType = Command.STOP;
                            else if (cmdType.equals("exit")) commandType = Command.EXIT;
                            else if (cmdType.equals("item")) commandType = Command.ITEM;
                            else commandType = Command.SCREEN;
                            
                            return new Command(label, commandType, priority);
                        } else {
                            return gotbad(2, "new", "table expected, got " + type(args.elementAt(1)));
                        }
                    }
                    else if (type.equals("buffer")) {
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable field = (Hashtable) args.elementAt(1);
                            String layout = getFieldValue(field, "layout", "default");
                            StringItem si = new StringItem(getFieldValue(field, "label", ""), getFieldValue(field, "value", ""), layout.equals("link") ? StringItem.HYPERLINK : layout.equals("button") ? StringItem.BUTTON : Item.LAYOUT_DEFAULT);
                            
                            si.setFont(genFont(getFieldValue(field, "style", "default")));
                            return si;
                        }
                    }
                    else if (type.equals("field")) { 
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable field = (Hashtable) args.elementAt(1);
                            return new TextField(getFieldValue(field, "label", ""), getFieldValue(field, "value", ""), getFieldNumber(field, "length", 256), getQuest(getFieldValue(field, "mode", "")));
                        }
                    }
                    else if (type.equals("gauge")) {
                        // A standalone Gauge, used as an Alert indicator (SetIndicator)
                        // or appended to a Form with graphics.append(form, gauge).
                        // maxValue = 0 + a CONTINUOUS_* value gives the built-in
                        // continuous-running indicator, which needs a non-interactive
                        // gauge.
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable field = (Hashtable) args.elementAt(1);
                            return new Gauge(getFieldValue(field, "label", ""), getFieldBoolean(field, "interactive", false), getFieldNumber(field, "maxValue", 100), getFieldNumber(field, "value", 0));
                        } else { return gotbad(2, "new", "table expected"); }
                    }
                    else { return gotbad(1, "new", "invalid type: " + type); } 
                case RENDER: return args.isEmpty() || args.elementAt(0) == null ? gotbad(1, "render", "string expected, got" + type(args.elementAt(0))) : midlet.readImg(toLuaString(args.elementAt(0)), father);
                case APPEND:
                    if (args.size() < 2) { return gotbad(1, "append", "wrong number of arguments"); }
                    else {
                        Object target = args.elementAt(0), itemObj = args.elementAt(1);
                        
                        if (target instanceof Form) {
                            Form form = (Form) target;
                            
                            if (itemObj instanceof Hashtable) {
                                Hashtable field = (Hashtable) itemObj;
                                String type = getFieldValue(field, "type", "text");
                                
                                if (type.equals("image")) {
                                    if (field.containsKey("img") && field.get("img") instanceof Image) {
                                        form.append((Image) field.get("img"));
                                    } else {
                                        String imgPath = getFieldValue(field, "img", "");
                                        if (!imgPath.equals("")) { form.append(midlet.readImg(imgPath, father)); }
                                    }
                                } 
                                else if (type.equals("text")) {
                                    String layout = getFieldValue(field, "layout", "default");
                                    StringItem si = new StringItem(getFieldValue(field, "label", ""), getFieldValue(field, "value", ""), layout.equals("link") ? StringItem.HYPERLINK : layout.equals("button") ? StringItem.BUTTON : Item.LAYOUT_DEFAULT);
                                    
                                    si.setFont(genFont(getFieldValue(field, "style", "default")));
                                    form.append(si);
                                }
                                else if (type.equals("item")) {
                                    Object rootObj = field.containsKey("root") ? field.get("root") : gotbad("append", "item", "missing root"); 

                                    Command RUN = new Command(getFieldValue(field, "label", (String) gotbad("append", "item", "missing label")), Command.ITEM, 1); 
                                    StringItem s = new StringItem(null, getFieldValue(field, "label", ""), StringItem.BUTTON); 
                                    s.setFont(genFont(field.containsKey("style") ? toLuaString(field.get("style")) : "default"));
                                    s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); 
                                    s.addCommand(RUN); 
                                    s.setDefaultCommand(RUN); 
                                    s.setItemCommandListener((ItemCommandListener) new LuaFunction("item", (Lua) rootObj)); 
                                    form.append(s);
                                }
                                else if (type.equals("choice")) { 
                                    String choiceType = getFieldValue(field, "mode", "exclusive");
                                    ChoiceGroup cg = new ChoiceGroup(getFieldValue(field, "label", ""), choiceType.equals("exclusive") ? Choice.EXCLUSIVE : choiceType.equals("multiple") ? Choice.MULTIPLE : Choice.POPUP);
                                    Object options = field.get("options");
                                    Image IMG = null;

                                    if (options instanceof Hashtable) {
                                        Hashtable fields = (Hashtable) options;

                                        if (isListTable(fields)) {
                                            Vector fv = toVector(fields);

                                            for (int i = 0; i < fields.size(); i++) { cg.append(toLuaString(fv.elementAt(i)), IMG); }
                                        } else {
                                            for (Enumeration keys = fields.keys(); keys.hasMoreElements();) {
                                                cg.append(toLuaString(fields.get(keys.nextElement())), IMG);
                                            }
                                        }
                                    }

                                    form.setItemStateListener((ItemStateListener) new LuaFunction("state", field.containsKey("root") ? field.get("root") : LUA_NIL));
                                    form.append(cg);
                                } 
                                else if (type.equals("field")) { form.append(new TextField(getFieldValue(field, "label", ""), getFieldValue(field, "value", ""), getFieldNumber(field, "length", 256), getQuest(getFieldValue(field, "mode", "")))); } 
                                else if (type.equals("spacer")) { form.append(new Spacer(getFieldNumber(field, "width", 1), getFieldNumber(field, "height", 10))); }
                                else if (type.equals("gauge")) { form.append(new Gauge(getFieldValue(field, "label", ""), getFieldBoolean(field, "interactive", false), getFieldNumber(field, "maxValue", 100), getFieldNumber(field, "value", 0))); } 
                            } 
                            else if (itemObj instanceof Item) { form.append((Item) itemObj); } 
                            else { form.append(new StringItem("", toLuaString(itemObj))); }
                        } 
                        else if (target instanceof List) {
                            List list = (List) target;
                            Image image = null;
                            
                            if (args.size() > 2) {
                                Object imgObj = args.elementAt(2);
                                image = imgObj instanceof Image ? (Image) imgObj : midlet.readImg(toLuaString(imgObj), father);
                            }
                            
                            list.append(toLuaString(itemObj), image);
                        } 
                        else { return gotbad(1, "append", "Form or List expected"); }
                        break;
                    }
                case ADDCMD:
                    if (args.size() < 2) { return gotbad(1, "addCommand", "wrong number of arguments"); }
                    else {
                        Object target = args.elementAt(0), cmdObj = args.elementAt(1);
                        
                        if (!(target instanceof Displayable)) { return gotbad(1, "addCommand", "Displayable expected"); }
                        if (!(cmdObj instanceof Command)) { return gotbad(1, "addCommand", "Command expected"); }
                        
                        ((Displayable) target).addCommand((Command) cmdObj); break;
                    }
                case HANDLER:
                    if (args.size() < 2) { return gotbad(1, "handler", "wrong number of arguments"); }
                    else {
                        Object screen = args.elementAt(0), table = args.elementAt(1);

                        if (!(screen instanceof Displayable)) { return gotbad(1, "handler", "Displayable expected, got " + type(table)); }
                        if (!(table instanceof Hashtable)) { return gotbad(2, "handler", "Hashtable expected, got " + type(table)); }

                        ((Displayable) screen).setCommandListener(new LuaFunction((Hashtable) table)); break;
                    }
                case TITLE: ((Displayable) args.elementAt(0)).setTitle(args.isEmpty() ? null : toLuaString(args.elementAt(1))); break;
                case TICKER: ((Displayable) args.elementAt(0)).setTicker(args.isEmpty() ? null : new Ticker(toLuaString(args.elementAt(1)))); break;
                case GETCURRENT: return midlet.display.getCurrent();
                case VIBRATE: midlet.display.vibrate(args.isEmpty() ? new Integer(500) : args.elementAt(0) instanceof Double ? new Integer(((Double) args.elementAt(0)).intValue()) : (Integer) gotbad(1, "vibrate", "number expected")); break;
                case SETLABEL: if (args.isEmpty()) { break; } else { Item i = args.elementAt(0) instanceof Item ? (Item) args.elementAt(0) : (Item) gotbad(1, "SetLabel", "Item expected"); i.setLabel(args.size() > 1 ? toLuaString(args.elementAt(1)) : null); }
                case GETLABEL: if (args.isEmpty()) { break; } else { Item i = args.elementAt(0) instanceof Item ? (Item) args.elementAt(0) : (Item) gotbad(1, "GetLabel", "Item expected"); return i.getLabel(); }
                case SETTEXT:
                    if (args.isEmpty()) { break; } 
                    else { 
                        Object i = args.elementAt(0); 
                        if (i instanceof StringItem) { ((StringItem) i).setText(args.size() > 1 ? toLuaString(args.elementAt(1)) : ""); } 
                        else if (i instanceof TextField) { ((TextField) i).setString(args.size() > 1 ? toLuaString(args.elementAt(1)) : ""); } 
                        else if (i instanceof TextBox) { ((TextBox) i).setString(args.size() > 1 ? toLuaString(args.elementAt(1)) : ""); }
                        else { return gotbad(1, "SetText", "Item expected"); }
                    } 
                case GETTEXT: if (args.isEmpty()) { break; } else { Object i = args.elementAt(0); return i instanceof StringItem ? ((StringItem) i).getText() : i instanceof TextField ? ((TextField) i).getString() : i instanceof TextBox ? ((TextBox) i).getString() : gotbad(1, "GetText", "Item expected"); } 
                case CLEAR_SCREEN:
                    if (args.isEmpty()) { return gotbad(1, "clear", "screen expected, got no value"); }
                    else {
                        Object screen = args.elementAt(0);

                        if (screen instanceof Form) { ((Form) args.elementAt(0)).deleteAll(); }
                        else if (screen instanceof List) { ((List) args.elementAt(0)).deleteAll(); }
                        else { return gotbad(1, "clear", "screen expected, got" + type(args.elementAt(0))); }
                        break;
                    }
                case TASKMNGR: midlet.showTaskManager(); break;
                case SETINDICATOR:
                    if (args.size() < 2) { return gotbad(1, "SetIndicator", "alert and gauge expected"); }
                    else if (!(args.elementAt(0) instanceof Alert)) { return gotbad(1, "SetIndicator", "Alert expected"); }
                    else if (!(args.elementAt(1) instanceof Gauge)) { return gotbad(2, "SetIndicator", "Gauge expected"); }
                    else {
                        // MIDP alerts cannot hold items, but they can show one
                        // non-interactive gauge as their indicator. The indicator
                        // has to be attached before the alert is displayed.
                        try { ((Alert) args.elementAt(0)).setIndicator((Gauge) args.elementAt(1)); }
                        catch (Exception e) { return gotbad(1, "SetIndicator", midlet.getCatch(e)); }
                        break;
                    }
                case SETGAUGE:
                    if (args.size() < 2) { return gotbad(1, "SetGauge", "gauge and value expected"); }
                    else if (!(args.elementAt(0) instanceof Gauge)) { return gotbad(1, "SetGauge", "Gauge expected"); }
                    else {
                        Gauge target = (Gauge) args.elementAt(0);
                        Object raw = args.elementAt(1);
                        int max = target.getMaxValue();
                        int value = raw instanceof Double ? ((Double) raw).intValue() : getNumber(toLuaString(raw), target.getValue());

                        // A max of 0 means the built-in continuous indicator, where
                        // the value is one of the Gauge.CONTINUOUS_* constants.
                        if (max > 0) { value = Math.max(0, Math.min(value, max)); }
                        else { value = Math.max(0, Math.min(value, 3)); }

                        // Gauge.setValue must run on the event thread.
                        midlet.display.callSerially(new GaugeUpdate(target, value));
                        break;
                    }
                case GETGAUGE: return args.isEmpty() || !(args.elementAt(0) instanceof Gauge) ? gotbad(1, "GetGauge", "Gauge expected") : new Double(((Gauge) args.elementAt(0)).getValue());
                case CANVAS: return canvasApi(nativeName, args);
                case CANVAS_ASSIGN: return canvasAssign(args);
                // Package [string]
                case LOWER: case UPPER: if (args.isEmpty()) { return gotbad(1, MOD == LOWER ? "lower" : "upper", "string expected, got no value"); } else { String text = toLuaString(args.elementAt(0)); return MOD == LOWER ? text.toLowerCase() : text.toUpperCase(); }
                case FIND: case MATCH: case LEN:
                    if (args.isEmpty()) { }
                    else {
                        Object obj = args.elementAt(0);
                        String text = toLuaString(obj), pattern = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;
                    
                        if (MOD == LEN) {
                            if (obj == null) { }
                            else if (obj instanceof String) { return new Double(text.length()); } 
                            else { throw new RuntimeException("string.len expected a string"); }
                        }

                        if (args.elementAt(0) == null || pattern == null) { }
                        else {
                            int startIdx = 0;
                            if (args.size() > 2) {
                                Object startObj = args.elementAt(2);
                                if (!(startObj instanceof Double)) { return gotbad(3, "match", "number expected, got " + type(startObj)); }
                                startIdx = Math.max(0, ((Double) startObj).intValue() - 1);
                            }
                            int pos = text.indexOf(pattern, startIdx);
                            if (pos == -1) { return null; } 
                            else if (MOD == FIND) { return luaNumber(pos + 1); } 
                            else if (MOD == MATCH) { return text.substring(pos, pos + pattern.length()); }
                        }
                    }
                case REVERSE: if (args.isEmpty()) { return gotbad(1, "reverse", "string expected, got no value"); } else { StringBuffer sb = new StringBuffer(toLuaString(args.elementAt(0))); return sb.reverse().toString(); }
                case SUB:
                    if (args.isEmpty()) { return gotbad(1, "sub", "string expected, got no value"); }
                    else {
                        String text = toLuaString(args.elementAt(0));

                        if (args.elementAt(0) == null) { }
                        else {
                            if (args.size() == 1) { return text; }

                            int len = text.length(), start = getNumber(toLuaString(args.elementAt(1)), 1), end = args.size() > 2 ? getNumber(toLuaString(args.elementAt(2)), len) : len;

                            if (start < 0) { start = len + start + 1; }
                            if (end < 0) { end = len + end + 1; }

                            if (start < 1) { start = 1; }
                            if (end > len) { end = len; }

                            if (start > end || start > len) { return ""; }

                            int jBegin = start - 1;

                            return text.substring(jBegin < 0 ? 0 : jBegin, end);
                        }
                    }
                case HASH: return args.isEmpty() || args.elementAt(0) == null ? null : new Double(args.elementAt(0).hashCode());
                case BYTE:
                    if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "byte", "string expected, got no value"); }
                    else {
                        String s = toLuaString(args.elementAt(0));
                        int len = s.length(), start = 1, end = 1;
                        if (args.size() >= 2) { start = getNumber(toLuaString(args.elementAt(1)), 1); end = start; }
                        if (args.size() >= 3) { end = getNumber(toLuaString(args.elementAt(2)), start); }
                        
                        if (start < 0) { start = len + start + 1; }
                        if (end < 0) { end = len + end + 1; }
                        if (start < 1) { start = 1; }
                        if (end > len) { end = len; } 
                        if (start > end || start > len) { return null; }
                        
                        if (end - start + 1 == 1) { return luaNumber(s.charAt(start - 1)); } 
                        else {
                            Hashtable result = new Hashtable();
                            for (int i = start; i <= end; i++) { result.put(luaNumber(i), luaNumber(s.charAt(i - 1))); }

                            return result;
                        }
                    }
                case CHAR:
                    if (args.isEmpty()) { return ""; } 
                    else {
                        Object firstArg = args.elementAt(0);
                        
                        if (firstArg instanceof Hashtable) {
                            Hashtable table = (Hashtable) firstArg;
                            StringBuffer sb = new StringBuffer();
                            for (int i = 0; i <= luaLength(table); i++) {
                                arg = table.get(new Double(i + 1));
                                if (arg == null) { continue; }
                                double num;
                                if (arg instanceof Double) { num = ((Double) arg).doubleValue(); } 
                                else { return gotbad(1, "char", "value out of range"); }
                                int c = (int) num;
                                if (c < 0 || c > 255) { return gotbad(1, "char", "value out of range"); }
                                sb.append((char) c);
                            }
                            return sb.toString();
                        } else {
                            StringBuffer sb = new StringBuffer();
                            for (int i = 0; i < args.size(); i++) {
                                arg = args.elementAt(i);
                                if (arg == null) { return gotbad(1, "char", "number expected, got nil"); }
                                double num;
                                if (arg instanceof Double) { num = ((Double) arg).doubleValue(); } 
                                else {
                                    try { num = Double.parseDouble(toLuaString(arg)); } 
                                    catch (Exception e) { return gotbad(1, "char", "number expected, got " + type(arg)); }
                                }
                                int c = (int) num;
                                if (c < 0 || c > 255) { return gotbad(1, "char", "value out of range"); }
                                sb.append((char) c);
                            }
                            return sb.toString();
                        }
                    }
                case TRIM: return args.isEmpty() ? null : toLuaString(args.elementAt(0)).trim();
                case UUID: String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + midlet.random.nextInt(4))); } else { uuid.append(chars.charAt(midlet.random.nextInt(16))); } } return uuid.toString();
                case SPLIT:
                    if (args.isEmpty()) { return gotbad(1, "split", "string expected, got no value"); } 
                    else if (args.size() > 1 && args.elementAt(1) == null) {
                        String[] array = midlet.splitArgs(toLuaString(args.elementAt(0)));
                        Hashtable result = new Hashtable();
                        for (int i = 0; i < array.length; i++) { result.put(new Double(i + 1), array[i]); }
                        return result;
                    }
                    else {
                        String text = toLuaString(args.elementAt(0));
                        String separator = args.size() > 1 ? toLuaString(args.elementAt(1)) : " ";
                        
                        if (text == null || text.length() == 0) { return new Hashtable(); }
                        if (separator == null || separator.length() == 0) {
                            Hashtable result = new Hashtable();
                            for (int i = 0; i < text.length(); i++) { result.put(new Double(i + 1), String.valueOf(text.charAt(i))); }
                            return result;
                        }
                        
                        Hashtable result = new Hashtable();
                        int index = 1, startPos = 0, sepLength = separator.length();
                        
                        while (startPos < text.length()) {
                            int foundPos = text.indexOf(separator, startPos);
                            
                            if (foundPos == -1) {
                                result.put(new Double(index++), text.substring(startPos));
                                break;
                            } else {
                                result.put(new Double(index++), text.substring(startPos, foundPos));
                                startPos = foundPos + sepLength;
                            }
                        }
                        
                        return result;
                    }
                case GETCMD: return args.isEmpty() ? null : midlet.getCommand(toLuaString(args.elementAt(0)));
                case GETARGS: return args.isEmpty() ? null : midlet.getArgument(toLuaString(args.elementAt(0)));
                case GETPATTERN: return args.isEmpty() ? null : midlet.getpattern(toLuaString(args.elementAt(0)));
                case ENV: return args.isEmpty() ? null : midlet.env(toLuaString(args.elementAt(0)));
                case STARTSWITH: return args.size() < 2 ? (Boolean) gotbad(1, "startswith", "string expected") : new Boolean(toLuaString(args.elementAt(0)).startsWith(toLuaString(args.elementAt(1))));
                case ENDSWITH: return args.size() < 2 ? (Boolean) gotbad(1, "endswith", "string expected") : new Boolean(toLuaString(args.elementAt(0)).endsWith(toLuaString(args.elementAt(1))));
                // Package [audio]
                case AUDIO_LOAD:
                    if (args.isEmpty()) { return gotbad(1, "load", "string expected, got no value"); }
                    else {
                        InputStream is = midlet.getInputStream(toLuaString(args.elementAt(0)), father);
                        if (is != null) {
                            Player player = Manager.createPlayer(is, args.size() > 1 ? toLuaString(args.elementAt(1)) : "audio/mpeg"); 
                            player.prefetch();
                            
                            return player;
                        }
                    }
                case AUDIO_PLAY: if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "play", "audio object expected"); } else { ((Player) args.elementAt(0)).start(); return new Double(0); }
                case AUDIO_PAUSE: if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "pause", "audio object expected"); } else { ((Player) args.elementAt(0)).stop(); return new Double(0); }
                case AUDIO_VOLUME:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "volume", "audio object expected"); }
                    else {
                        Player player = (Player) args.elementAt(0);
                        VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");

                        if (args.size() > 1 && args.elementAt(1) instanceof Double) {
                            int value = ((Double) args.elementAt(1)).intValue();
                            return vc != null ? new Double(vc.setLevel(value)) : new Double(0);
                        }
                        else { return new Double(vc != null ? vc.getLevel() : 0); }
                    }
                case AUDIO_DURATION:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "duration", "audio object expected"); }
                    else {
                        Player player = (Player) args.elementAt(0);
                        long duration = player.getDuration();
                        return new Double(duration == Player.TIME_UNKNOWN ? -1 : duration / 1000.0);
                    }
                case AUDIO_TIME:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "time", "audio object expected"); }
                    else {
                        Player player = (Player) args.elementAt(0);

                        if (args.size() > 1 && args.elementAt(1) instanceof Double) {
                            long time = (long) (((Double) args.elementAt(1)).doubleValue() * 1000);
                            player.setMediaTime(time); return new Double(0);
                        } else {
                            long time = player.getMediaTime();
                            return new Double(time == Player.TIME_UNKNOWN ? -1 : time / 1000.0);
                        }
                    }
                // Package [java]
                case CLASS: if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "class", "string expected, got no value"); } else { return new Boolean(midlet.javaClass(toLuaString(args.elementAt(0))) == 0); }
                case NAME: return midlet.getName(); 
                case DELETE: if (args.isEmpty() || !(args.elementAt(0) instanceof Hashtable)) { return gotbad(1, "delete", "table expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); } else if (args.size() < 2 || args.elementAt(1) == null) { return gotbad(2, "delete", "value expected, got " + (args.size() < 2 ? "no value" : "nil")); } else { ((Hashtable) args.elementAt(0)).remove(args.elementAt(1)); } break;
                case RUN: if (args.isEmpty()) { break; } else if (args.elementAt(0) instanceof LuaFunction) { kill = false; new Thread((Runnable) new LuaFunction((LuaFunction) args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "Background").start(); } else { return gotbad(1, "run", "function expected, got" + type(args.elementAt(0))); } break;
                case PREQ: if (args.isEmpty()) { break; } else { return new Boolean(midlet.platformRequest(toLuaString(args.elementAt(0)))); }
                case THREAD: return midlet.getThreadName(Thread.currentThread());
                case UPTIME: return new Double(System.currentTimeMillis() - midlet.uptime);
                case SLEEP:
                    if (args.isEmpty()) { }
                    else {
                        arg = args.elementAt(0);
                        if (arg instanceof Double) { Thread.sleep(((Double) arg).longValue()); break; }
                        else { return gotbad(1, "sleep", "number expected, got " + type(arg)); }
                    }
                // Releases the interpreter so queued UI callbacks can run (used
                // by background jobs that update a screen while they work).
                case YIELD:
                    yieldLua(args.isEmpty() || !(args.elementAt(0) instanceof Double) ? 20 : ((Double) args.elementAt(0)).longValue());
                    break;
                // Kernel Core
                case KERNEL:
                    Object payload = args.elementAt(0), arg = args.elementAt(1), scope = args.elementAt(2), pid = args.elementAt(3);
                    int uid = ((Double) args.elementAt(4)).intValue();

                    if (payload == null || payload.equals("")) { return null; }
                    if (payload instanceof String) {
                        if (payload.equals("sendsig")) {
                            if (arg == null || !(arg instanceof Hashtable)) { return new Double(2); }
                            else {
                                Hashtable info = (Hashtable) arg;
                                String pid = (String) info.get("pid"), signal = toLuaString(info.get("signal"));

                                if (midlet.sys.containsKey(pid)) {
                                    Process process = (Process) midlet.sys.get(pid);

                                    if (process.uid == uid || uid == 0) {
                                        if (!signal.equals("9") && process.sighandler != null) {
                                            try { 
                                                Vector arguments = new Vector(); arguments.addElement(signal);
                                                ((LuaFunction) process.sighandler).call(arguments);
                                            }
                                            catch (Throwable e) {  }
                                        }

                                        midlet.sys.remove(pid);
                                        if (signal.equals("9") && arg.equals("1")) { midlet.destroyApp(true); }
                                        return new Double(0);
                                    } else { return new Double(13); }
                                }
                                else { return new Double(127); }
                            }
                        }
                        else if (payload.equals("proc")) {
                            if (arg == null || arg.equals("")) { return new Double(2); }
                            else if (midlet.sys.containsKey(arg)) {
                                Process process = (Process) midlet.sys.get(arg);
                                if (process.uid == uid || uid == 0) { return process; }
                                else { return new Double(13); }
                            }
                            else { return new Double(127); }
                        }
                        else if (payload.equals("nice")) {
                            if (arg == null || !(arg instanceof Hashtable)) { return new Double(2); }
                            else {
                                Hashtable info = (Hashtable) arg;
                                String pid = (String) info.get("pid");
                                int priority = ((Double) info.get("priority")).intValue();
                                if (midlet.sys.containsKey(pid)) {
                                    Process process = (Process) midlet.sys.get(pid);

                                    if (process.uid == uid || uid == 0) {
                                        process.priority = Math.max(Process.MIN_PRIORITY, Math.min(Process.MAX_PRIORITY, priority));
                                        return new Double(0);
                                    } else { return new Double(13); }
                                }
                                else { return new Double(127); }
                            }
                        }
                        else if (payload.equals("passwd")) {
                            if (arg instanceof String) { return new Boolean(midlet.passwd((String) arg)); }
                            else if (arg instanceof Hashtable) {
                                Hashtable query = (Hashtable) arg;
                                String old = (String) query.get("old"), newpw = (String) query.get("new");

                                if (old == null || newpw == null || old.equals("") || newpw.equals("")) { return new Double(2); }
                                else if (uid == 0 || midlet.passwd(old)) { return new Double(midlet.writeRMS("OpenRMS", String.valueOf(newpw.hashCode()).getBytes(), 2)); }
                                else { return new Double(13); }
                            }
                        }
                        else if (payload.equals("setsh")) {
                            if (arg == null || arg.equals("")) { midlet.shell = new LuaFunction(EXEC); }
                            else if (arg instanceof LuaFunction) { midlet.shell = arg; }
                            else { return new Double(2); }
                        }
                        else if (payload.equals("cache")) { if (arg == null || arg.equals("")) { return new Boolean(midlet.useCache); } else if (arg == TRUE || toLuaString(arg).equals("true")) { midlet.useCache = true; } else if (arg == FALSE || toLuaString(arg).equals("false")) { midlet.useCache = false; midlet.cacheLua.clear(); } else { return new Double(2); } }
                        else if (payload.equals("memory")) {
                            if (arg == null || arg.equals("")) { return new Double(midlet.memory_size); }
                            if (!(arg instanceof Double)) { return new Double(2); }
                            double value = ((Double) arg).doubleValue();
                            if (value < 512 || value > Integer.MAX_VALUE / 1024 || value != Math.floor(value)) { return new Double(22); }
                            midlet.memory_size = (int) value;
                            return new Double(midlet.memory_size);
                        }
                        else if (payload.equals("debug")) { if (arg == null || arg.equals("")) { return new Boolean(midlet.debug); } else if (arg == TRUE || toLuaString(arg).equals("true")) { midlet.debug = true; } else if (arg == FALSE || toLuaString(arg).equals("false")) { midlet.debug = false; } else { return new Double(2); } } 
                        else if (payload.equals("netsh")) {
                            if (arg == null || arg.equals("")) {
                                Hashtable result = new Hashtable();
                                for (Enumeration procs = midlet.sys.keys(); procs.hasMoreElements();) {
                                    String pid = (String) procs.nextElement();
                                    Process p = (Process) midlet.sys.get(pid);
                                    
                                    if (p.net.isEmpty()) { }
                                    else {
                                        Hashtable map = new Hashtable(); int i = 1;
                                        for (Enumeration sockets = p.net.keys(); sockets.hasMoreElements();) {
                                            map.put(new Double(1), sockets.nextElement());
                                        }
                                        result.put(pid, map);
                                    }
                                }
                                return result;
                            }
                        }

                        else if (payload.equals("serve")) {
                            if (arg == null || arg.equals("")) { return new Double(2); }
                            else {
                                String program = toLuaString(arg), code = midlet.read(program, father);
                                if (code == null || code.length() == 0) { return "service '" + program + "' not found"; }

                                Hashtable childScope = midlet.cloneScope(father);
                                Process process = new Process(midlet, program, "/bin/init --serve=" + program, midlet.getUser(uid), uid, midlet.genpid(), stdout, childScope);
                                process.parentPid = PID;
                                process.lua.kill = false;

                                Hashtable arg = new Hashtable(); arg.put(new Double(0), program); arg.put(new Double(1), "--deamon");
                                Hashtable res = process.lua.run(program, code, arg);

                                Object handler = res.get("object");
                                if (handler instanceof Vector) {
                                    Vector resx = (Vector) handler;
                                    handler = resx.elementAt(0);
                                }

                                if (handler instanceof Lua.LuaFunction) { process.handler = handler; ((Lua.LuaFunction) handler).name = program; }
                            }
                        }

                        else if (payload.equals("rms")) {
                            if (uid == 0) {
                                if (arg == null || arg.equals("")) { return new Double(2); }
                                else if (arg.equals("/bin/") || arg.equals("/etc/") || arg.equals("/lib/") || arg.equals("/boot/")) { midlet.clearVfsDirectory(toLuaString(arg)); }
                                else { String r = toLuaString(arg); int rmi = midlet.vfsDirIndex(r); if (rmi != -1 && rmi >= 6) { String sd = r.endsWith("/") ? r : r + "/"; midlet.clearVfsDirectory(sd); if (midlet.fs.containsKey(sd)) { int base = sd.lastIndexOf('/', sd.length() - 2); String parent = sd.substring(0, base + 1); String entry = sd.substring(base + 1, sd.length() - 1) + "/"; Vector struct = (Vector) midlet.fs.get(parent); if (struct != null) { struct.removeElement(entry); } midlet.fs.remove(sd); midlet.unpersistVfsMount(sd); } } else { return new Double(5); } }
                            } else { return new Double(13); }
                        }
                        else if (payload.equals("useradd")) {
                            if (arg == null || arg.equals("") || arg.equals("root")) { return new Double(2); }
                            else if (uid != 0) { return new Double(13); }
                            else if (midlet.userID.containsKey(arg)) { return new Double(128); }
                            else { midlet.userID.put(arg, new Integer(midlet.lastID + 1)); midlet.lastID++; return new Double(0); }
                        }
                        else if (payload.equals("userdel")) {
                            if (arg == null || arg.equals("") || arg.equals("root") || arg.equals(midlet.username)) { return new Double(13); }
                            else if (midlet.userID.containsKey(arg)) { if (uid == 0) { midlet.userID.remove(arg); return new Double(0); } else { return new Double(13); } }
                            else { return new Double(127); }
                        }
                        else if (payload.equals("user")) {
                            if (arg == null || arg.equals("") || !(arg instanceof Double)) { return new Double(2); }
                            else {
                                String user = midlet.getUser(((Double) arg).intValue());
                                if (user == null) { return new Double(127); }
                                else { return user; }
                            }
                        }
                    }

            }
            return null;
        }
        // |
        // | -=-=-=-=-=-=-=-=-=-=-
        // | API 004 - Lua Canvas [graphics.canvas]
        // |
        // Every canvas function is a native tagged with nativeName (see the
        // graphics.canvas registration in the Lua constructor) and takes the
        // canvas handle as its first argument, so both call styles work:
        //   graphics.canvas.text(c, "hi", 4, 4)   and   c:text("hi", 4, 4)
        private Object canvasApi(String method, Vector args) throws Exception {
            String where = "graphics.canvas." + method;

            if (method.equals("new")) { return canvasNew(args, where); }
            if (method.equals("rgb")) { return args.isEmpty() ? gotbad(1, where, "color expected, got no value") : luaNumber(canvasColor(args.elementAt(0), where)); }

            LuaCanvas canvas = canvasArg(args, where);
            Hashtable handle = canvas.getScreen();

            if (method.equals("on")) {
                if (args.size() < 3 || !(args.elementAt(1) instanceof String) || !(args.elementAt(2) instanceof LuaFunction)) { return gotbad(2, where, "event name and function expected"); }
                canvas.setCallback((String) args.elementAt(1), args.elementAt(2));
                return handle;
            }
            if (method.equals("off")) {
                if (args.size() < 2 || !(args.elementAt(1) instanceof String)) { return gotbad(2, where, "event name expected"); }
                canvas.setCallback((String) args.elementAt(1), null);
                return handle;
            }
            if (method.equals("display")) { canvas.present(); return handle; }
            if (method.equals("close")) { canvas.close(); return handle; }
            if (method.equals("title")) {
                if (args.size() < 2) { return canvas.getTitle(); }
                canvas.setTitle(toLuaString(args.elementAt(1)));
                return handle;
            }
            if (method.equals("fullscreen")) {
                if (args.size() < 2) { return canvas.isFullscreen() ? TRUE : FALSE; }
                canvas.setFullscreen(isTruthy(args.elementAt(1)));
                return canvas.isFullscreen() ? TRUE : FALSE;
            }
            if (method.equals("size")) {
                Vector result = new Vector();
                result.addElement(luaNumber(canvas.getCanvasWidth()));
                result.addElement(luaNumber(canvas.getCanvasHeight()));
                return result;
            }
            if (method.equals("repaint")) {
                if (args.size() >= 5) { canvas.repaint(canvasInt(args, 1, where), canvasInt(args, 2, where), canvasInt(args, 3, where), canvasInt(args, 4, where)); }
                else { canvas.repaint(); }
                return handle;
            }
            if (method.equals("flush")) { canvas.flush(); return handle; }
            if (method.equals("clear")) {
                if (args.size() >= 5) { canvas.clear(canvasInt(args, 1, where), canvasInt(args, 2, where), canvasInt(args, 3, where), canvasInt(args, 4, where)); }
                else { canvas.clear(); }
                return handle;
            }
            if (method.equals("color")) {
                if (args.size() < 2) { return luaNumber(canvas.getColorValue()); }
                if (args.size() >= 4) { canvas.setColorValue(canvasRgb(canvasInt(args, 1, where), canvasInt(args, 2, where), canvasInt(args, 3, where))); }
                else { canvas.setColorValue(canvasColor(args.elementAt(1), where)); }
                return handle;
            }
            if (method.equals("background")) {
                if (args.size() < 2) { return luaNumber(canvas.getBackgroundValue()); }
                canvas.setBackgroundValue(canvasColor(args.elementAt(1), where));
                return handle;
            }
            if (method.equals("foreground")) {
                if (args.size() < 2) { return luaNumber(canvas.getForegroundValue()); }
                canvas.setForegroundValue(canvasColor(args.elementAt(1), where));
                return handle;
            }
            if (method.equals("font")) {
                if (args.size() < 2) { return canvas.getFontSpec(); }
                Object spec = args.elementAt(1);
                if (spec instanceof String) { String text = (String) spec; canvas.setFontValue(genFont(text), text); }
                else if (spec instanceof Double) {
                    int face = ((Double) spec).intValue();
                    int style = args.size() > 2 && args.elementAt(2) instanceof Double ? ((Double) args.elementAt(2)).intValue() : Font.STYLE_PLAIN;
                    int size = args.size() > 3 && args.elementAt(3) instanceof Double ? ((Double) args.elementAt(3)).intValue() : Font.SIZE_MEDIUM;
                    Font value = Font.getFont(face, style, size);
                    canvas.setFontValue(value == null ? Font.getDefaultFont() : value, "custom");
                }
                else { return gotbad(2, where, "font spec string or face number expected, got " + type(spec)); }
                return handle;
            }
            if (method.equals("metrics")) {
                Vector result = new Vector();
                result.addElement(luaNumber(canvas.getFontHeight()));
                result.addElement(luaNumber(canvas.getBaseline()));
                return result;
            }
            if (method.equals("textWidth")) { return luaNumber(canvas.getTextWidth(toLuaString(args.elementAt(1)))); }
            if (method.equals("charWidth")) {
                Object value = args.size() > 1 ? args.elementAt(1) : null;
                if (value instanceof String && ((String) value).length() > 0) { return luaNumber(canvas.getCharWidth(((String) value).charAt(0))); }
                if (value instanceof Double) { return luaNumber(canvas.getCharWidth((char) ((Double) value).intValue())); }
                return gotbad(2, where, "character expected, got " + type(value));
            }
            if (method.equals("text")) {
                if (args.size() < 4 || !(args.elementAt(1) instanceof String)) { return gotbad(2, where, "text, x and y expected"); }
                int anchor = args.size() > 4 ? canvasAnchor(args.elementAt(4), where) : (Graphics.TOP | Graphics.LEFT);
                canvas.drawText((String) args.elementAt(1), canvasInt(args, 2, where), canvasInt(args, 3, where), anchor);
                return handle;
            }
            if (method.equals("print")) { canvas.printText(toLuaString(args.size() > 1 ? args.elementAt(1) : null)); return handle; }
            if (method.equals("nl")) { canvas.newLine(); return handle; }
            if (method.equals("backspace")) { canvas.backspace(args.size() > 1 ? canvasInt(args, 1, where) : 1); return handle; }
            if (method.equals("cursor")) {
                if (args.size() >= 3) { canvas.setCursor(canvasInt(args, 1, where), canvasInt(args, 2, where)); return handle; }
                Vector result = new Vector();
                result.addElement(luaNumber(canvas.getCursor()[0]));
                result.addElement(luaNumber(canvas.getCursor()[1]));
                return result;
            }
            if (method.equals("caret")) { canvas.drawCaret(); return handle; }
            if (method.equals("line")) { canvas.drawLine(canvasInt(args, 1, where), canvasInt(args, 2, where), canvasInt(args, 3, where), canvasInt(args, 4, where)); return handle; }
            if (method.equals("rect")) {
                int x = canvasInt(args, 1, where), y = canvasInt(args, 2, where), w = canvasInt(args, 3, where), h = canvasInt(args, 4, where);
                if (args.size() > 5 && isTruthy(args.elementAt(5))) { canvas.fillRect(x, y, w, h); } else { canvas.drawRect(x, y, w, h); }
                return handle;
            }
            if (method.equals("roundRect")) {
                int x = canvasInt(args, 1, where), y = canvasInt(args, 2, where), w = canvasInt(args, 3, where), h = canvasInt(args, 4, where), aw = canvasInt(args, 5, where), ah = canvasInt(args, 6, where);
                if (args.size() > 7 && isTruthy(args.elementAt(7))) { canvas.fillRoundRect(x, y, w, h, aw, ah); } else { canvas.drawRoundRect(x, y, w, h, aw, ah); }
                return handle;
            }
            if (method.equals("arc")) {
                int x = canvasInt(args, 1, where), y = canvasInt(args, 2, where), w = canvasInt(args, 3, where), h = canvasInt(args, 4, where), start = canvasInt(args, 5, where), angle = canvasInt(args, 6, where);
                if (args.size() > 7 && isTruthy(args.elementAt(7))) { canvas.fillArc(x, y, w, h, start, angle); } else { canvas.drawArc(x, y, w, h, start, angle); }
                return handle;
            }
            if (method.equals("circle")) {
                int cx = canvasInt(args, 1, where), cy = canvasInt(args, 2, where), radius = canvasInt(args, 3, where);
                if (args.size() > 4 && isTruthy(args.elementAt(4))) { canvas.fillArc(cx - radius, cy - radius, radius * 2, radius * 2, 0, 360); }
                else { canvas.drawArc(cx - radius, cy - radius, radius * 2, radius * 2, 0, 360); }
                return handle;
            }
            if (method.equals("ellipse")) {
                int x = canvasInt(args, 1, where), y = canvasInt(args, 2, where), w = canvasInt(args, 3, where), h = canvasInt(args, 4, where);
                if (args.size() > 5 && isTruthy(args.elementAt(5))) { canvas.fillArc(x, y, w, h, 0, 360); } else { canvas.drawArc(x, y, w, h, 0, 360); }
                return handle;
            }
            if (method.equals("triangle")) {
                int[] xs = { canvasInt(args, 1, where), canvasInt(args, 3, where), canvasInt(args, 5, where) };
                int[] ys = { canvasInt(args, 2, where), canvasInt(args, 4, where), canvasInt(args, 6, where) };
                if (args.size() > 7 && isTruthy(args.elementAt(7))) { canvas.fillPolygon(xs, ys); } else { canvas.drawPolygon(xs, ys); }
                return handle;
            }
            if (method.equals("polygon")) {
                int[][] points = canvasPoints(args.size() > 1 ? args.elementAt(1) : null, where);
                if (args.size() > 2 && isTruthy(args.elementAt(2))) { canvas.fillPolygon(points[0], points[1]); } else { canvas.drawPolygon(points[0], points[1]); }
                return handle;
            }
            if (method.equals("pixel")) { canvas.drawPixel(canvasInt(args, 1, where), canvasInt(args, 2, where)); return handle; }
            if (method.equals("image")) {
                if (args.size() < 4) { return gotbad(2, where, "image and x, y expected"); }
                Object source = args.elementAt(1);
                Image image = source instanceof Image ? (Image) source : source instanceof String ? midlet.readImg((String) source, father) : null;
                if (image == null) { return gotbad(2, where, "image or path expected, got " + type(source)); }
                canvas.drawImage(image, canvasInt(args, 2, where), canvasInt(args, 3, where), args.size() > 4 ? canvasAnchor(args.elementAt(4), where) : (Graphics.TOP | Graphics.LEFT));
                return handle;
            }
            if (method.equals("clip")) { canvas.setClip(canvasInt(args, 1, where), canvasInt(args, 2, where), canvasInt(args, 3, where), canvasInt(args, 4, where)); return handle; }
            if (method.equals("resetClip")) { canvas.resetClip(); return handle; }
            if (method.equals("clipRect")) {
                int[] clip = canvas.getClip();
                Vector result = new Vector();
                for (int i = 0; i < clip.length; i++) { result.addElement(luaNumber(clip[i])); }
                return result;
            }
            if (method.equals("translate")) { canvas.translate(canvasInt(args, 1, where), canvasInt(args, 2, where)); return handle; }
            if (method.equals("save")) { canvas.saveState(); return handle; }
            if (method.equals("restore")) { canvas.restoreState(); return handle; }
            if (method.equals("reset")) { canvas.resetState(); return handle; }
            if (method.equals("keyDown")) { return canvas.isKeyDown(canvasInt(args, 1, where)) ? TRUE : FALSE; }
            if (method.equals("keyName")) { return canvas.keyName(canvasInt(args, 1, where)); }
            if (method.equals("gameAction")) { return luaNumber(canvas.gameAction(canvasInt(args, 1, where))); }
            if (method.equals("keys")) { return canvas.getPressedKeys(); }
            if (method.equals("resetKeys")) { canvas.resetKeys(); return handle; }
            if (method.equals("pointer")) {
                Vector result = new Vector();
                result.addElement(luaNumber(canvas.getPointerX()));
                result.addElement(luaNumber(canvas.getPointerY()));
                return result;
            }
            if (method.equals("waitKey")) {
                int[] key = canvas.waitKey(args.size() > 1 ? canvasInt(args, 1, where) : 0);
                if (key == null) { return null; }
                Vector result = new Vector();
                result.addElement(luaNumber(key[0]));
                result.addElement(luaNumber(key[1]));
                return result;
            }
            if (method.equals("loop")) {
                LuaFunction step = null;
                int delay = 20;
                for (int i = 1; i < args.size(); i++) {
                    Object value = args.elementAt(i);
                    if (value instanceof LuaFunction) { step = (LuaFunction) value; }
                    else if (value instanceof Double) { delay = ((Double) value).intValue(); }
                    else if (value != null && value != LUA_NIL) { return gotbad(i + 1, where, "function or frame delay expected, got " + type(value)); }
                }
                canvas.runLoop(step, delay);
                return handle;
            }
            if (method.equals("animate")) {
                LuaFunction step = null;
                int delay = 20;
                for (int i = 1; i < args.size(); i++) {
                    Object value = args.elementAt(i);
                    if (value instanceof LuaFunction) { step = (LuaFunction) value; }
                    else if (value instanceof Double) { delay = ((Double) value).intValue(); }
                    else if (value != null && value != LUA_NIL) { return gotbad(i + 1, where, "function or frame delay expected, got " + type(value)); }
                }
                canvas.animate(step, delay);
                return handle;
            }
            if (method.equals("stop")) { canvas.stop(); return handle; }
            if (method.equals("running")) { return canvas.isRunning() ? TRUE : FALSE; }
            if (method.equals("age")) { return luaNumber(canvas.getAge()); }
            if (method.equals("command")) {
                if (args.size() < 2 || !(args.elementAt(1) instanceof String)) { return gotbad(2, where, "label expected"); }
                String label = (String) args.elementAt(1), kind = "screen";
                int priority = 1;
                LuaFunction handler = null;
                for (int i = 2; i < args.size(); i++) {
                    Object value = args.elementAt(i);
                    if (value instanceof LuaFunction) { handler = (LuaFunction) value; }
                    else if (value instanceof Double) { priority = ((Double) value).intValue(); }
                    else if (value instanceof String) { kind = ((String) value).toLowerCase(); }
                }
                canvas.addCommandHandler(new Command(label, canvasCommandType(kind), priority), handler);
                return handle;
            }
            if (method.equals("clearCommands")) { canvas.clearCommands(); return handle; }

            return gotbad(1, where, "unknown canvas method");
        }
        // | (Create a canvas handle: a table with the Java object in its metatable)
        private Object canvasNew(Vector args, String where) throws Exception {
            Hashtable options = new Hashtable();
            if (args.size() > 0 && args.elementAt(0) != null) {
                if (!(args.elementAt(0) instanceof Hashtable)) { return gotbad(1, where, "table expected, got " + type(args.elementAt(0))); }
                options = (Hashtable) args.elementAt(0);
            }
            int background = options.get("background") == null ? 0xFFFFFF : canvasColor(options.get("background"), where);
            int foreground = options.get("foreground") == null ? 0x000000 : canvasColor(options.get("foreground"), where);
            String spec = options.get("font") instanceof String ? (String) options.get("font") : "medium";
            String title = options.get("title") instanceof String ? (String) options.get("title") : "Canvas";

            LuaCanvas canvas = new LuaCanvas(midlet, Lua.this, background, foreground, spec);
            canvas.setTitle(title);

            Hashtable handle = new Hashtable();
            Hashtable meta = new Hashtable();
            meta.put("__index", canvasModule());
            meta.put("__canvas", canvas);
            LuaFunction assign = new LuaFunction(CANVAS_ASSIGN);
            assign.name = "canvas.__newindex";
            meta.put("__newindex", assign);
            handle.put("__metatable", meta);
            canvas.setScreen(handle);

            String[] events = { "paint", "keyPressed", "keyReleased", "keyRepeated", "pointerPressed", "pointerReleased", "pointerDragged", "show", "hide", "size", "tick", "command" };
            for (int i = 0; i < events.length; i++) {
                Object fn = options.get(events[i]);
                if (fn instanceof LuaFunction) { canvas.setCallback(events[i], fn); }
            }

            if (isTruthy(options.get("fullscreen"))) { canvas.setFullscreen(true); }
            return handle;
        }
        // | (__newindex for a canvas handle: callbacks and writable properties)
        private Object canvasAssign(Vector args) throws Exception {
            if (args.size() < 3) { return null; }
            Object key = args.elementAt(1), value = args.elementAt(2);
            if (!(key instanceof String)) { return null; }
            String name = (String) key;
            LuaCanvas canvas = canvasOf(args.elementAt(0));
            if (canvas == null) { return null; }

            if (value instanceof LuaFunction) { canvas.setCallback(name, value); return null; }
            canvas.setCallback(name, null);

            String text = value == null || value == LUA_NIL ? null : toLuaString(value);
            if (name.equals("title")) { canvas.setTitle(text == null ? "" : text); }
            else if (name.equals("font")) { canvas.setFontValue(LuaCanvas.parseFont(text), text); }
            else if (name.equals("background")) { canvas.setBackgroundValue(canvasColor(value, "graphics.canvas.background")); }
            else if (name.equals("foreground")) { canvas.setForegroundValue(canvasColor(value, "graphics.canvas.foreground")); }
            else if (name.equals("color")) { canvas.setColorValue(canvasColor(value, "graphics.canvas.color")); }
            return null;
        }
        // | (Java object behind a handle, if it is a canvas handle)
        private LuaCanvas canvasOf(Object value) {
            if (value instanceof LuaCanvas) { return (LuaCanvas) value; }
            if (value instanceof Hashtable) {
                Object meta = ((Hashtable) value).get("__metatable");
                if (meta instanceof Hashtable) {
                    Object canvas = ((Hashtable) meta).get("__canvas");
                    if (canvas instanceof LuaCanvas) { return (LuaCanvas) canvas; }
                }
            }
            return null;
        }
        // | (Accept a handle, a raw LuaCanvas, or a table carrying "canvas")
        private LuaCanvas canvasArg(Vector args, String where) throws Exception {
            Object value = args.isEmpty() ? null : args.elementAt(0);
            LuaCanvas canvas = canvasOf(value);
            if (canvas != null) { return canvas; }
            return (LuaCanvas) gotbad(1, where, "canvas expected, got " + (args.isEmpty() ? "no value" : type(value)));
        }
        private Hashtable canvasModule() {
            Object graphics = globals.get("graphics");
            if (graphics instanceof Hashtable) { Object module = ((Hashtable) graphics).get("canvas"); if (module instanceof Hashtable) { return (Hashtable) module; } }
            return new Hashtable();
        }
        // | (Colors: 0xRRGGBB numbers, "#RRGGBB" strings or names such as "red")
        private int canvasColor(Object value, String where) throws Exception {
            if (value instanceof Double) { return ((Double) value).intValue() & 0xFFFFFF; }
            if (value instanceof String) {
                int named = LuaCanvas.namedColor((String) value);
                if (named >= 0) { return named; }
                try { return Integer.parseInt(((String) value).trim()) & 0xFFFFFF; } catch (Exception e) { }
                return ((Integer) gotbad(1, where, "unknown color '" + value + "'")).intValue();
            }
            return ((Integer) gotbad(1, where, "color expected, got " + type(value))).intValue();
        }
        private int canvasRgb(int r, int g, int b) {
            if (r < 0) { r = 0; } if (r > 255) { r = 255; }
            if (g < 0) { g = 0; } if (g > 255) { g = 255; }
            if (b < 0) { b = 0; } if (b > 255) { b = 255; }
            return (r << 16) | (g << 8) | b;
        }
        private int canvasInt(Vector args, int index, String where) throws Exception {
            Object value = index < args.size() ? args.elementAt(index) : null;
            if (!(value instanceof Double)) { return ((Integer) gotbad(index + 1, where, "number expected, got " + (index < args.size() ? type(value) : "no value"))).intValue(); }
            return ((Double) value).intValue();
        }
        // | (Anchor numbers or "center", "top left", "bottom right"...)
        private int canvasAnchor(Object value, String where) throws Exception {
            if (value instanceof Double) { return LuaCanvas.normalizeAnchor(((Double) value).intValue()); }
            if (value instanceof String) {
                String text = ((String) value).toLowerCase();
                int anchor = 0;
                if (text.indexOf("center") >= 0) { anchor |= Graphics.HCENTER | Graphics.VCENTER; }
                if (text.indexOf("left") >= 0) { anchor |= Graphics.LEFT; }
                if (text.indexOf("right") >= 0) { anchor |= Graphics.RIGHT; }
                if (text.indexOf("top") >= 0) { anchor |= Graphics.TOP; }
                if (text.indexOf("bottom") >= 0) { anchor |= Graphics.BOTTOM; }
                if (text.indexOf("baseline") >= 0) { anchor |= Graphics.BASELINE; }
                return LuaCanvas.normalizeAnchor(anchor);
            }
            return ((Integer) gotbad(2, where, "anchor expected, got " + type(value))).intValue();
        }
        private int canvasCommandType(String kind) {
            if (kind.equals("back")) { return Command.BACK; }
            if (kind.equals("cancel")) { return Command.CANCEL; }
            if (kind.equals("ok")) { return Command.OK; }
            if (kind.equals("help")) { return Command.HELP; }
            if (kind.equals("stop")) { return Command.STOP; }
            if (kind.equals("exit")) { return Command.EXIT; }
            if (kind.equals("item")) { return Command.ITEM; }
            return Command.SCREEN;
        }
        // | (Polygon points: {x1, y1, x2, y2...} or {{x=, y=}, ...} / {{x, y}, ...})
        private int[][] canvasPoints(Object value, String where) throws Exception {
            if (!(value instanceof Hashtable)) { return (int[][]) gotbad(2, where, "table of points expected, got " + type(value)); }
            Hashtable table = (Hashtable) value;
            Vector xs = new Vector(), ys = new Vector();
            int count = luaLength(table);
            for (int i = 1; i <= count; i++) {
                Object item = table.get(new Double(i));
                if (item instanceof Hashtable) {
                    Hashtable point = (Hashtable) item;
                    Object x = point.get("x"), y = point.get("y");
                    if (x == null) { x = point.get(new Double(1)); }
                    if (y == null) { y = point.get(new Double(2)); }
                    if (!(x instanceof Double) || !(y instanceof Double)) { return (int[][]) gotbad(2, where, "point table must hold x and y numbers"); }
                    xs.addElement(x); ys.addElement(y);
                }
                else if (item instanceof Double) {
                    Object next = i < count ? table.get(new Double(i + 1)) : null;
                    if (!(next instanceof Double)) { return (int[][]) gotbad(2, where, "flat point list must hold x, y pairs"); }
                    xs.addElement(item); ys.addElement(next);
                    i++;
                }
                else { return (int[][]) gotbad(2, where, "point expected, got " + type(item)); }
            }
            int[] px = new int[xs.size()], py = new int[ys.size()];
            for (int i = 0; i < xs.size(); i++) { px[i] = ((Double) xs.elementAt(i)).intValue(); py[i] = ((Double) ys.elementAt(i)).intValue(); }
            return new int[][] { px, py };
        }
        // |
        private Object exec(String code, Hashtable scope) throws Exception { return exec(code, scope, "[string]"); }
        private Object exec(String code, Hashtable scope, String srcName) throws Exception {
            int savedIndex = tokenIndex; Vector savedTokens = tokens;
            String savedSource = currentSource; String savedCode = lastCode; Vector savedOffsets = lineOffsets;
            currentSource = srcName == null ? "[string]" : srcName;
            lastCode = code;
            lineOffsets = computeLineOffsets(code);
            Object ret = null;
            try {
                tokens = tokenize(code); tokenIndex = 0;
                Hashtable modScope = scope == null ? new Hashtable() : scope;
                for (Enumeration e = globals.keys(); e.hasMoreElements();) { String k = (String) e.nextElement(); modScope.put(k, unwrap(globals.get(k))); }
                while (peek().type != EOF) { Object res = statement(modScope); if (doreturn) { ret = res; doreturn = false; break; } }
            }
            finally {
                tokenIndex = savedIndex; tokens = savedTokens;
                currentSource = savedSource; lastCode = savedCode; lineOffsets = savedOffsets;
            }
            return ret;
        }
        public static String type(Object item) { return item == null || item == LUA_NIL ? "nil" : item instanceof String ? "string" : item instanceof Double ? "number" : item instanceof Boolean ? "boolean" : item instanceof LuaFunction ? "function" : item instanceof Hashtable ? "table" : item instanceof InputStream || item instanceof OutputStream || item instanceof StringBuffer || item instanceof StringItem ? "stream" : item instanceof SocketConnection || item instanceof StreamConnection ? "connection" : item instanceof ServerSocketConnection ? "server" : item instanceof Displayable || item instanceof Canvas ? "screen" : item instanceof Image ? "image" : item instanceof Command ? "button" : item instanceof Player ? "audio" : "userdata"; }
        private Object gotbad(int pos, String name, String expect) throws Exception { throw new RuntimeException("bad argument #" + pos + " to '" + name + "' (" + expect + ")"); }
        private Object gotbad(String name, String field, String expected) throws Exception { throw new RuntimeException(name + " -> field '" + field + "' (" + expected + ")"); }
        private Object http(String method, String url, String data, Object item, boolean toget) throws Exception {
            if (url == null || url.length() == 0) { return ""; }
            if (!url.startsWith("http://") && !url.startsWith("https://")) { url = "http://" + url; }

            HttpConnection conn = null;
            Hashtable headers = (Hashtable) (item instanceof Hashtable ? item : item == null ? new Hashtable() : gotbad("POST".equalsIgnoreCase(method) ? 3 : 2, "POST".equalsIgnoreCase(method) ? "post" : "get", "table expected, got " + type(item)));
            InputStream is = null;
            ByteArrayOutputStream baos = null;

            try {
                conn = (HttpConnection) Connector.open(url);
                conn.setRequestMethod(method.toUpperCase());

                if (headers != null) {
                    Enumeration keys = headers.keys();
                    while (keys.hasMoreElements()) {
                        String key = (String) keys.nextElement();
                        conn.setRequestProperty(key, toLuaString(headers.get(key)));
                    } 
                }

                if ("POST".equalsIgnoreCase(method)) {
                    byte[] postBytes = (data == null) ? new byte[0] : data.getBytes("UTF-8");

                    if (headers == null || headers.get("Content-Type") == null) { conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); }
                    if (headers == null || headers.get("Content-Length") == null) { conn.setRequestProperty("Content-Length", Integer.toString(postBytes.length)); }

                    OutputStream os = conn.openOutputStream();
                    os.write(postBytes);
                    os.flush(); os.close();
                }

                is = conn.openInputStream(); if (toget) { Vector result = new Vector(); result.addElement(is); result.addElement(new Double(new Double(conn.getResponseCode()))); return result; }
                baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = is.read()) != -1) { baos.write(ch); }

                Vector result = new Vector();
                result.addElement(new String(baos.toByteArray(), "UTF-8"));
                result.addElement(new Double(new Double(conn.getResponseCode())));

                if (is != null) { try { is.close(); } catch (Exception e) { } }
                if (conn != null) { try { conn.close(); } catch (Exception e) { } }
                if (baos != null) { try { baos.close(); } catch (Exception e) { } }

                return result;
            } 
            catch (Exception e) { throw e; }
        }
        private int compareLua(Object a, Object b) { if (a == null && b == null) { return 0; } if (a == null) { return -1; } if (b == null) { return 1; } if (a instanceof Double && b instanceof Double) { double da = ((Double) a).doubleValue(), db = ((Double) b).doubleValue(); return da < db ? -1 : (da > db ? 1 : 0); } String sa = toLuaString(a), sb = toLuaString(b); return sa.compareTo(sb); }
        // |
        public static boolean isListTable(Hashtable table) {
            if (table == null) { return false; }
            else if (table.isEmpty()) { return true; }

            int size = luaLength(table);
            for (int i = 1; i <= size; i++) {
                if (!table.containsKey(new Double(i))) { return false; }
            }
            for (Enumeration e = table.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                if ("__metatable".equals(key)) { continue; }
                if (!(key instanceof Double)) { return false; }
                double d = ((Double) key).doubleValue();
                if (d != Math.floor(d) || d < 1 || d > size) { return false; }
            }
            return true;
        }
        public static Vector toVector(Hashtable table) throws Exception { Vector vec = new Vector(); if (table == null) { return vec; } for (int i = 1; i <= luaLength(table); i++) { vec.addElement(table.get(new Double(i))); } return vec; }

        private int getFieldNumber(Hashtable table, String key, int fallback) { Object val = table.get(key); if (val instanceof Double) { return ((Double) val).intValue(); } try { return Integer.parseInt(toLuaString(val)); } catch (Exception e) { return fallback; } }
        private boolean getFieldBoolean(Hashtable table, String key, boolean fallback) { Object val = table.get(key); if (val instanceof Boolean) { return ((Boolean) val).booleanValue(); } return toLuaString(val).equalsIgnoreCase("true") ? true : fallback; } 
        private int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = midlet.replace(mode, "password", "").trim(); } int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY; return password ? (base | TextField.PASSWORD) : base; } 
        public int getNumber(String s, int fallback) { try { return Integer.valueOf(s); } catch (Exception e) { return fallback; } } 
    
        private String getFieldValue(Hashtable table, String key, String fallback) { Object val = table.get(key); return val != null ? toLuaString(val) : fallback; }
        private Font genFont(String params) { if (params == null || params.length() == 0 || params.equals("default")) { return Font.getDefaultFont(); } int face = Font.FACE_SYSTEM, style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; String[] tokens = midlet.split(params, ' '); for (int i = 0; i < tokens.length; i++) { String token = tokens[i].toLowerCase(); if (token.equals("system")) { face = Font.FACE_SYSTEM; } else if (token.equals("monospace")) { face = Font.FACE_MONOSPACE; } else if (token.equals("proportional")) { face = Font.FACE_PROPORTIONAL; } else if (token.equals("bold")) { style |= Font.STYLE_BOLD; } else if (token.equals("italic")) { style |= Font.STYLE_ITALIC; } else if (token.equals("ul") || token.equals("underline") || token.equals("underlined")) { style |= Font.STYLE_UNDERLINED; } else if (token.equals("small")) { size = Font.SIZE_SMALL; } else if (token.equals("medium")) { size = Font.SIZE_MEDIUM; } else if (token.equals("large")) { size = Font.SIZE_LARGE; } } Font f = Font.getFont(face, style, size); return f == null ? Font.getDefaultFont() : f; }

        public void run() { if (root instanceof LuaFunction) { Vector arg = new Vector(); try { ((LuaFunction) root).call(arg); } catch (Throwable e) { if (!silent) { midlet.print(getTraceback(e), stdout, id, father); } } } }

        // named (non-anonymous) background runner: the SDK preverifier and the
        // on-device VM choke on the synthetic anon class that a trailing `&`
        // used to compile into (Lua$LuaFunction$1 -> NoClassDefFoundError)
        private class BGRunner implements Runnable {
            private final String cmd;
            private final String mark;
            BGRunner(String cmd, String mark) { this.cmd = cmd; this.mark = mark; }
            public void run() {
                try {
                    Vector payload = new Vector();
                    payload.addElement(cmd);
                    if (mark.equals("builtin")) { payload.addElement(TRUE); }
                    exec(payload);
                } catch (Exception e) { }
            }
        }

        public Double exec(Vector args) throws Exception {
            if (args.isEmpty()) { return (Double) gotbad(1, "execute", "string expected, got no value"); }
            else {
                int status = 0; InputStream in; boolean builtin = args.size() > 1 ? ((Boolean) args.elementAt(1)).booleanValue() : false;

                String command = midlet.env(toLuaString(args.elementAt(0)));

                Vector chain = splitAmpersands(command);
                if (chain.size() > 1) {
                    int chainStatus = 0;
                    for (int i = 0; i < chain.size(); i++) {
                        String seg = ((String) chain.elementAt(i)).trim();
                        if (seg.length() == 0) { continue; }
                        Vector payload = new Vector();
                        payload.addElement(seg);
                        if (builtin) { payload.addElement(TRUE); }
                        chainStatus = exec(payload).intValue();
                        if (chainStatus != 0) { break; }
                    }
                    return new Double(chainStatus);
                }
                int amp = lastAmpersandIndex(command);
                if (amp != -1) {
                    final String bg = command.substring(0, amp).trim();
                    if (bg.length() > 0) {
                        new Thread(new BGRunner(bg, builtin ? "builtin" : "")).start();
                    }
                    return new Double(0);
                }

                String mainCommand = midlet.getCommand(command), argument = midlet.getArgument(command);
                String[] args = midlet.splitArgs(argument);

                Object output = stdout; Hashtable aliases = (Hashtable) father.get("ALIAS");
                for (int i = 0; i < args.length; i++) {
                    if (args[i].equals(">")) {
                        output = toLuaString(midlet.joinpath(args[i + 1], father));
                        
                        Vector sanitize = new Vector(); StringBuffer buffer = new StringBuffer();
                        for (int j = 0; j < i; j++) { sanitize.addElement(args[j]); if (j > 0) buffer.append(' '); buffer.append(args[j]); }

                        String[] args = new String[sanitize.size()];
                        sanitize.copyInto(args); argument = buffer.toString();

                        break;
                    }
                }

                if (mainCommand.equals("") || mainCommand.equals("true") || mainCommand.startsWith("#")) { }
                else if (aliases.containsKey(mainCommand) && !builtin) { Vector payload = new Vector(); payload.addElement(((String) aliases.get(mainCommand)) + " " + argument); return exec(payload); }
                else if ((in = open("/bin/" + mainCommand, father)) != null) { status = (Integer) popen("/bin/" + mainCommand, midlet.genpid(), argument, id, output, father, in).elementAt(0); }
                else if (mainCommand.equals(".")) {
                    if (args.length == 0) { }
                    else if ((in = open(midlet.joinpath(args[0], father), father)) != null) {
                        status = (Integer) popen(args[0], midlet.genpid(), argument.substring(args[0].length()).trim(), id, output, father, in).elementAt(0);
                    }
                    else {
                        midlet.print(". " + args[0] + ": not found", output, id, father);
                        status = 127;
                    }
                }
                else if (mainCommand.equals("gc")) { System.gc(); }
                else if (mainCommand.equals("cat")) {
                    for (int i = 0; i < args.length; i++) {
                        try {
                            InputStream in = midlet.getInputStream(midlet.joinpath(args[i], father), father);
                            if (in != null) { midlet.print(midlet.read(in, 1024, true), output, id, father); }
                            else { status = 2; break; }
                        } catch (Exception e) {
                            status = 127; break;
                        }
                    }
                }
                else if (mainCommand.equals("ls")) {
                    Vector payload = new Vector(); StringBuffer buffer = new StringBuffer();
                    payload.addElement(args.length == 0 ? (String) father.get("PWD") : midlet.joinpath(args[0], father));
                    Hashtable items = dirs(payload);

                    if (items.isEmpty()) { }
                    else {
                        for (Enumeration keys = items.keys(); keys.hasMoreElements();) {
                            Double i = (Double) keys.nextElement();
                            String file = (String) items.get(i);

                            if (!file.startsWith(".")) { buffer.append(file).append("\t"); }
                        }

                        midlet.print(buffer.toString().trim(), output, id, father);
                    }
                }
                else if (mainCommand.equals("ps")) {
                    midlet.print("PID\tPROCESS", output, id, father);
                    for (Enumeration procs = midlet.sys.keys(); procs.hasMoreElements();) {
                        String pid = (String) procs.nextElement();

                        midlet.print(pid + "\t" + ((Process) midlet.sys.get(pid)).name, output, id, father);
                    }
                }
                else if (mainCommand.equals("su")) {
                    if (args.length == 0) {
                        if (id == 0) { id = 1000; father.put("USER", midlet.username); }
                        else { suPrompt(); }
                    }
                    else if (args[0].equals("root")) {
                        if (args.length >= 2) {
                            if (midlet.passwd(args[1])) { id = 0; father.put("USER", "root"); }
                            else { midlet.print("Permission denied!", output, id, father); status = 13; }
                        } else {
                            suPrompt();
                        }
                    }
                    else if (args.length == 1) {
                        if (midlet.userID.containsKey(args[0])) {
                            id = midlet.getUserID(args[0]);
                            father.put("USER", args[0]);
                        } else {
                            midlet.print("Permission denied!", output, id, father);
                            status = 13;
                        }
                    }
                    else {
                        midlet.print("su: usage: su [username] [passwd]", output, id, father);
                    }
                }
                else if (mainCommand.equals("uptime")) { midlet.print(((System.currentTimeMillis() - midlet.uptime) / 1000) + " ms", output, id, father); }
                else if (mainCommand.equals("time")) {
                    long before = System.currentTimeMillis();
                    Vector payload = new Vector();
                    payload.addElement(argument);
                    status = exec(payload).intValue();

                    midlet.print("at " + (System.currentTimeMillis() - before), output, id, father);
                }
                else if (mainCommand.equals("date")) { midlet.print(new java.util.Date().toString(), output, id, father); }
                else if (mainCommand.equals("whoami")) { midlet.print((String) father.get("USER"), output, id, father); }
                else if (mainCommand.equals("id")) {
                    if (args.length == 0) {
                        midlet.print("uid=" + id + "(" + midlet.getUser(id) + ")", output, id, father);
                    } else {
                        for (int i = 0; i < args.length; i++) {
                            int uid = midlet.getUserID(args[i]);

                            if (uid == -1) { midlet.print("id: " + args[i] + ": not found", output, id, father); status = 127; break; }
                            else { midlet.print("uid=" + uid + "(" + args[i] + ")", output, id, father); }
                        }
                    }
                }
                else if (mainCommand.equals("alias")) {
                    if (args.length == 0) {
                        for (Enumeration keys = aliases.keys(); keys.hasMoreElements();) {
                            String key = (String) keys.nextElement(), value = (String) aliases.get(key);
                            midlet.print("alias " + key + "='" + value + "'", output, id, father);
                        }
                    } 
                    else {
                        for (int i = 0; i < args.length; i++) {
                            int j = args[i].indexOf("="); 
                            if (j != -1) {
                                String key = args[i].substring(0, j);
                                StringBuffer value = new StringBuffer(args[i].substring(j + 1));
                                while (i + 1 < args.length && args[i + 1].indexOf("=") == -1) { value.append(' ').append(args[++i]); }
                                aliases.put(key, midlet.getpattern(value.toString()));
                            } else {
                                if (aliases.containsKey(args[i])) {
                                    midlet.print("alias " + args[i] + "='" + ((String) aliases.get(args[i])) + "'", output, id, father);
                                } else {
                                    midlet.print("alias: " + args[i] + ": not found", output, id, father); status = 127;
                                }
                            }
                        }
                    }
                }
                else if (mainCommand.equals("unalias")) {
                    if (args.length == 0) { midlet.print("unalias: usage: unalias [-a] name [name ...]", output, id, father); }
                    else if (args[0].equals("-a")) { aliases.clear(); }
                    else {
                        for (int i = 0; i < args.length; i++) {
                            if (aliases.containsKey(args[i])) {
                                aliases.remove(args[i]);
                            }
                            else {
                                midlet.print("unalias: " + args[i] + ": not found", output, id, father);
                                status = 127;
                                break;
                            }
                        }
                    }
                }
                else if (mainCommand.equals("clear")) { if (stdout instanceof StringItem) { ((StringItem) stdout).setText(""); } else if (stdout instanceof StringBuffer) { ((StringBuffer) stdout).setLength(0); } }
                else if (mainCommand.equals("env") || mainCommand.equals("export") || mainCommand.equals("set")) {
                    if (args.length == 0) {
                        for (Enumeration keys = midlet.attributes.keys(); keys.hasMoreElements();) {
                            String key = (String) keys.nextElement(), value = (String) midlet.attributes.get(key);
                            midlet.print(key + "=" + value, output, id, father);
                        }
                    } else {
                        for (int i = 0; i < args.length; i++) {
                            int j = args[i].indexOf("="); 
                            if (j != -1) {
                                String key = args[i].substring(0, j), value = midlet.getpattern(args[i].substring(j + 1));
                                midlet.attributes.put(key, value);
                            } else {
                                if (midlet.attributes.containsKey(args[i])) {
                                    midlet.print(args[i] + "=" + ((String) midlet.attributes.get(args[i])), output, id, father);
                                } else {
                                    midlet.print(mainCommand + ": " + args[i] + ": not found", output, id, father); status = 127;
                                }
                            }
                        }
                    }
                }
                else if (mainCommand.equals("unset")) {
                    if (args.length == 0) { }
                    else {
                        for (int i = 0; i < args.length; i++) {
                            if (midlet.attributes.containsKey(args[i])) {
                                midlet.attributes.remove(args[i]);
                            }
                            else {
                                midlet.print("unset: " + args[i] + ": not found", output, id, father);
                                status = 127;
                                break;
                            }
                        }
                    }
                }
                else if (mainCommand.equals("echo")) { midlet.print(argument, output, id, father); }
                else if (mainCommand.equals("exit")) { Vector payload = new Vector(); payload.addElement(args.length == 0 ? "0" : args[0]); payload.addElement(builtin); exit(payload); }
                else if (mainCommand.equals("pwd")) { midlet.print((String) father.get("PWD"), output, id, father); }
                else if (mainCommand.equals("cd")) {
                    Vector payload = new Vector();
                    payload.addElement(args.length == 0 ? "/home/" : args[0]);
                    status = ((Double) chdir(payload)).intValue();

                    if (status == 127) { midlet.print("cd: " + args[0] + ": not found", output, id, father); }
                    else if (status == 20) { midlet.print("cd: " + args[0] + ": found", output, id, father); }
                    else if (status == 13) { midlet.print("cd: " + args[0] + ": permission denied", output, id, father); }
                }
                else if (mainCommand.equals("chroot")) {
                    if (args.length == 0) { midlet.print("chroot: missing operand", output, id, father); status = 127; } 
                    else if (args[0].equals("/") || args[0].startsWith("/mnt/")) { father.put("ROOT", args[0]); }
                    else { midlet.print("chroot: " + args[0] + ": invalid path, usage a mount point", output, id, father); status = 2; }
                }
                else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { Vector payload = new Vector(); payload.addElement(argument); payload.addElement(true); payload.addElement(FALSE); return exec(payload); }
                else if (mainCommand.equals("false")) { status = 255; }
                else if (mainCommand.equals("grandma")) { midlet.print("Press F to respect...", output, id, father); }
                else { midlet.print(mainCommand + ": not found", output, id, father); status = 127; }
                
                return new Double(status);
            }
        }
        public Vector splitAmpersands(String input) {
            Vector segments = new Vector();
            if (input == null) { segments.addElement(""); return segments; }
            StringBuffer current = new StringBuffer();
            char quote = 0; boolean escaped = false;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (escaped) { current.append(c); escaped = false; continue; }
                if (quote == 0 && c == '\\') { current.append(c); escaped = true; continue; }
                if (quote == 0 && (c == '"' || c == '\'')) { current.append(c); quote = c; continue; }
                if (quote != 0 && c == quote) { current.append(c); quote = 0; continue; }
                if (quote == 0 && c == '&' && i + 1 < input.length() && input.charAt(i + 1) == '&') {
                    segments.addElement(current.toString());
                    current.setLength(0);
                    i++;
                    continue;
                }
                current.append(c);
            }
            segments.addElement(current.toString());
            return segments;
        }
        public int lastAmpersandIndex(String input) {
            if (input == null) { return -1; }
            char quote = 0; boolean escaped = false; int last = -1;
            for (int i = 0; i < input.length(); i++) {
                char c = input.charAt(i);
                if (escaped) { escaped = false; continue; }
                if (quote == 0 && c == '\\') { escaped = true; continue; }
                if (quote == 0 && (c == '"' || c == '\'')) { quote = c; continue; }
                if (quote != 0 && c == quote) { quote = 0; continue; }
                if (quote == 0 && c == '&') { last = i; }
            }
            if (last == -1) { return -1; }
            for (int i = last + 1; i < input.length(); i++) {
                char c = input.charAt(i);
                if (c != ' ' && c != '\t') { return -1; }
            }
            return last;
        }
        public Hashtable dirs(Vector args) throws Exception {
            String pwd = args.isEmpty() ? (String) father.get("PWD") : toLuaString(args.elementAt(0));
            int index = 1;
            
            Hashtable list = new Hashtable();
            if (pwd.startsWith("/")) { }
            else { pwd = ((String) father.get("PWD")) + pwd; }
            if (pwd.endsWith("/")) { } 
            else { pwd = pwd + "/"; }

            if (pwd.startsWith("/root/") && id != 0) { return list; }
            else if ((pwd = midlet.redirect(midlet.solvepath(pwd, father))).equals("/tmp/")) { for (Enumeration files = midlet.tmp.keys(); files.hasMoreElements();) { list.put(new Double(index), (String) files.nextElement()); index++; } }
            else if (pwd.equals("/mnt/")) { for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { list.put(new Double(index), (String) roots.nextElement()); index++; } } 
            else if (pwd.startsWith("/mnt/")) { 
                FileConnection CONN = (FileConnection) Connector.open("file:///" + pwd.substring(5), Connector.READ); 
                for (Enumeration files = CONN.list(); files.hasMoreElements();) { list.put(new Double(index), (String) files.nextElement()); index++; } 
                CONN.close(); 
            } 
            else if (pwd.startsWith("/proc/")) {
                String rest = pwd.substring(6);
                if (rest.equals("")) {
                    String[] pf = midlet.procFiles();
                    for (int j = 0; j < pf.length; j++) { list.put(new Double(index), pf[j]); index++; }
                    Vector pe = midlet.procEntries(id);
                    for (int j = 0; j < pe.size(); j++) { list.put(new Double(index), pe.elementAt(j)); index++; }
                } else {
                    String pd = rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
                    Vector pe = midlet.procDirEntries(pd, id);
                    for (int j = 0; j < pe.size(); j++) { list.put(new Double(index), pe.elementAt(j)); index++; }
                }
            }
            else if (midlet.vfsDirIndex(pwd) != -1) { Vector files = midlet.listVfsFiles(pwd); for (int i = 0; i < files.size(); i++) { list.put(new Double(index++), files.elementAt(i)); } }
            else if (pwd.equals("/home/")) { 
                String[] files = RecordStore.listRecordStores(); 
                if (files != null) { 
                    for (int i = 0; i < files.length; i++) { if (files[i].startsWith("OpenRMS")) { continue; } list.put(new Double(index), files[i]); index++; }
                } 
            }
            
            if (midlet.fs.containsKey(pwd)) {
                Vector struct = (Vector) midlet.fs.get(pwd);

                for (int i = 0; i < struct.size(); i++) { list.put(new Double(index), struct.elementAt(i)); index++; }
            }
            
            return list;
        }

        public InputStream open(String uri, Hashtable scope) { try { return midlet.getInputStream(uri, scope); } catch (Exception e) { return null; } }
        public Vector popen(Vector args) throws Exception {
            if (args.isEmpty()) { return null; }
            
            String program = toLuaString(args.elementAt(0));
            Object arguments = args.size() > 1 ? toLuaString(args.elementAt(1)) : "";
            int owner = (args.size() < 3) ? new Integer(id) : ((args.elementAt(2) instanceof Boolean) ? new Integer((Boolean) args.elementAt(2) ? id : 1000) : (Integer) gotbad(3, "popen", "boolean expected, got " + type(args.elementAt(2))));
            Object out = (args.size() < 4) ? new StringBuffer() : args.elementAt(3);
            Hashtable scope = (args.size() < 5) ? father : (args.elementAt(4) instanceof Hashtable ? (Hashtable) args.elementAt(4) : (Hashtable) gotbad(5, "popen", "table expected, got " + type(args.elementAt(4))));
            InputStream is = (args.size() < 6) ? midlet.getInputStream(program, father) : (InputStream) args.elementAt(5);
            
            return popen(program, midlet.genpid(), arguments, owner, out, scope, is);
        }
        public Vector popen(String program, String pid, Object arguments, int owner, Object out, Hashtable scope, InputStream is) throws Exception {
            Vector result = new Vector();
            
            if (is == null) { result.addElement(new Double(127)); return result; }
            
            try {  
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                
                while ((length = is.read(buffer)) != -1) { baos.write(buffer, 0, length); }
                
                byte[] data = baos.toByteArray();
                baos.close();
                
                Hashtable arg = null;
                if (arguments instanceof Hashtable) {
                    arg = (Hashtable) arguments;
                    arg.put(new Double(0), program);
                } else if (arguments instanceof String[]) {
                    arg = new Hashtable();
                    String[] list = (String[]) arguments;
                    for (int i = 0; i < list.length; i++) { arg.put(new Double(i), list[i]); }
                } else if (arguments != null) {
                    arg = new Hashtable();
                    arg.put(new Double(0), program);
                    String argsStr = toLuaString(arguments);
                    String[] list = midlet.splitArgs(argsStr);
                    for (int i = 0; i < list.length; i++) { arg.put(new Double(i + 1), list[i]); }
                }
                
                if (arg == null) { arg = new Hashtable(); arg.put(new Double(0), program); }
                
                if (midlet.isPureText(data)) {
                    String code = new String(data, "UTF-8");
                    if (code.startsWith("#!/bin/sh")) {
                        String[] cmds = midlet.split(code, '\n');
                        int status = 0;
                        for (int i = 0; i < cmds.length; i++) {
                            Vector payload = new Vector();
                            payload.addElement(cmds[i]);
                            status = exec(payload).intValue();
                            if (status != 0) { break; }
                        }

                        result.addElement(new Double(status));
                    } else {
                        Hashtable childScope = midlet.cloneScope(scope);
                        childScope.put("USER", midlet.getUser(owner));
                        Process process = new Process(midlet, ("lua " + program).trim(), midlet.joinpath(program, scope), midlet.getUser(owner), owner, pid, out, childScope);
                        process.parentPid = PID;
                        midlet.sys.put(pid, process);
                        Hashtable digest = process.lua.run(program, code, arg);
                        
                        result.addElement(digest.get("status"));
                    }
                } else {
                    InputStream elfStream = new ByteArrayInputStream(data);
                    Hashtable elfScope = midlet.cloneScope(scope);
                    elfScope.put("USER", midlet.getUser(owner));
                    Process process = new Process(midlet, "elf", midlet.joinpath(program, scope), midlet.getUser(owner), owner, pid, out, arg, elfScope);
                    process.parentPid = PID;
                    midlet.sys.put(pid, process);
                    
                    if (process.getELF().load(elfStream)) {
                        Hashtable digest = process.getELF().run();
                        result.addElement(((Double) digest.get("status")).intValue());
                    } else {
                        result.addElement(1);
                    }
                }
                
                result.addElement(out instanceof StringBuffer ? out.toString() : out);
                return result; 
                
            } catch (Exception e) { result.addElement(new Double(1)); return result; }
        }

        public Object chdir(Vector args) throws Exception {
            if (args.isEmpty()) { return father.get("PWD"); }
            else {
                String pwd = (String) father.get("PWD"), target = midlet.joinpath(toLuaString(args.elementAt(0)), father);
                if (!target.endsWith("/")) {
                    target = target + "/";
                }
                if (target.equals("") || target == null) { father.put("PWD", "/home/"); return new Double(0); }
                else if (target.equals("..")) {
                    if (pwd.equals("/")) { return new Double(1); }
                    
                    int lastSlashIndex = pwd.lastIndexOf('/', pwd.endsWith("/") ? pwd.length() - 2 : pwd.length() - 1);
                    father.put("PWD", (lastSlashIndex <= 0) ? "/" : pwd.substring(0, lastSlashIndex + 1));

                    return new Double(0);
                }

                if (target.equals("/root/") || target.startsWith("/root/")) {
                    if (id != 0) { return new Double(13); }
                }

                if (target.equals("/proc/") || target.startsWith("/proc/")) {
                    String rest = target.substring(6);
                    if (rest.equals("") || rest.equals("/")) { father.put("PWD", "/proc/"); return new Double(0); }
                    String pd = rest.endsWith("/") ? rest.substring(0, rest.length() - 1) : rest;
                    if (pd.indexOf('/') != -1) { return new Double(127); }
                    Process pp = (Process) midlet.sys.get(pd);
                    if (pp == null) { return new Double(127); }
                    if (id != 0 && pp.uid != id) { return new Double(13); }
                    father.put("PWD", target.endsWith("/") ? target : target + "/");
                    return new Double(0);
                }

                if (midlet.fs.containsKey(target)) { father.put("PWD", target); return new Double(0); }
                else {
                    String chk = midlet.redirect(target);
                    if (chk.startsWith("/mnt/")) {
                        FileConnection fc = (FileConnection) Connector.open("file:///" + chk.substring(5), Connector.READ); 
                        boolean exist = fc.exists(), dir = fc.isDirectory();
                        fc.close(); 
                        if (exist && dir) { father.put("PWD", target); return new Double(0); } 
                        else { return new Double(exist ? 20 : 127); }
                    }
                    else if (midlet.getInputStream(chk.substring(chk.length() - 1), father) != null) { return new Double(20); }
                }

                return new Double(127);
            }
        }

        public void exit(Vector args) {
            silent = true;
            if (PID.equals("1")) { midlet.destroyApp(true); }
            else {
                boolean hadScreen = proc.screen != null;
                proc.exitStatus = args.isEmpty() ? 1 : getNumber(toLuaString(args.elementAt(0)), 1);
                proc.exited = true;
                midlet.exited.put(PID, proc);
                midlet.sys.remove(PID);
                if (hadScreen) {
                    Displayable target = null;
                    for (Enumeration e = midlet.sys.keys(); e.hasMoreElements();) {
                        Process p = (Process) midlet.sys.get(e.nextElement());
                        if (p != null && p.screen != null) { target = p.screen; }
                    }
                    if (target != null) { midlet.display.setCurrent(target); }
                    else { midlet.destroyApp(true); }
                }
            }
            if (args.isEmpty()) { throw new Error(); } else { status = getNumber(toLuaString(args.elementAt(0)), 1); }
        }

        public void suPrompt() {
            suPromptPrevious = midlet.display.getCurrent();
            suPromptForm = new Form("su - root");
            suPromptForm.append(new TextField("[su] password for " + midlet.username, "", 256, TextField.ANY | TextField.PASSWORD));
            suPromptBack = new Command("Back", Command.BACK, 1);
            suPromptRun = new Command("Run", Command.SCREEN, 1);
            suPromptForm.addCommand(suPromptBack);
            suPromptForm.addCommand(suPromptRun);
            suPromptForm.setCommandListener(this);
            midlet.display.setCurrent(suPromptForm);
        }

        public void commandAction(Command c, Displayable d) {
            try {
                if (suPromptForm != null && d == suPromptForm) {
                    if (suPromptPrevious != null) { midlet.display.setCurrent(suPromptPrevious); }
                    if (c == suPromptRun) {
                        TextField tf = (TextField) suPromptForm.get(0);
                        String query = tf.getString();
                        if (query == null || !midlet.passwd(query)) {
                            midlet.print("Permission denied!", stdout, id, father);
                        } else {
                            id = 0;
                            father.put("USER", "root");
                        }
                    }
                    suPromptForm = null;
                    return;
                }
                if (cmds.containsKey(c) && cmds.get(c) instanceof LuaFunction) {
                    Vector args = new Vector();
                    if (d instanceof List) {
                        List list = (List) d;
                        for (int i = 0; i < list.size(); i++) { if (list.isSelected(i)) { args.addElement(list.getString(i)); } }
                    } else if (d instanceof TextBox) {
                        args.addElement(((TextBox) d).getString());
                    } else if (d instanceof Form) {
                        Form form = (Form) d;
                        for (int i = 0; i < form.size(); i++) {
                            Item item = form.get(i);

                            if (item instanceof TextField) { args.addElement(((TextField) item).getString()); } 
                            else if (item instanceof Gauge) { args.addElement(new Double(((Gauge) item).getValue())); }
                            else if (item instanceof ChoiceGroup) {
                                ChoiceGroup cg = (ChoiceGroup) item;

                                Hashtable selTable = new Hashtable();
                                for (int j = 0; j < cg.size(); j++) { selTable.put(new Double(j + 1), new Boolean(cg.isSelected(j))); }

                                args.addElement(selTable);
                            }
                        }
                    }

                    ((LuaFunction) cmds.get(c)).call(args);
                }
            }
            catch (Exception e) { midlet.print(getTraceback(e), stdout); midlet.sys.remove(PID); } 
            catch (Error e) { if (!silent) { midlet.print(midlet.getCatch(e), stdout); } midlet.sys.remove(PID); }
        }
        public void commandAction(Command c, Item item) { try { if (root instanceof LuaFunction) { ((LuaFunction) root).call(new Vector()); } } catch (Exception e) { midlet.print(getTraceback(e), stdout, id, father); midlet.sys.remove(PID); } catch (Error e) { if (!silent) { midlet.print(midlet.getCatch(e), stdout, id, father); } midlet.sys.remove(PID); } }
        public void itemStateChanged(Item item) {
            try {
                if (root == LUA_NIL) { }
                else if (root instanceof LuaFunction) { 
                    Vector args = new Vector();

                    if (item instanceof ChoiceGroup) {
                        ChoiceGroup cg = (ChoiceGroup) item;

                        for (int j = 0; j < cg.size(); j++) { args.addElement(new Boolean(cg.isSelected(j))); }
                    }
                    else if (item instanceof Gauge) { args.addElement(new Double(((Gauge) item).getValue())); }

                    ((LuaFunction) root).call(args); 
                }
            } catch (Exception e) { midlet.print(getTraceback(e), stdout, id, father); midlet.sys.remove(PID); } catch (Error e) { if (!silent) { midlet.print(midlet.getCatch(e), stdout, id, father); } midlet.sys.remove(PID); } 
        }
    }

    // Gauge updates from a background thread (graphics.SetGauge in a java.run
    // loop) are posted to the event thread through Display.callSerially. A named
    // class instead of an anonymous one keeps the J2ME preverifier happy.
    private class GaugeUpdate implements Runnable {
        private final Gauge gauge;
        private final int value;
        GaugeUpdate(Gauge gauge, int value) { this.gauge = gauge; this.value = value; }
        public void run() { try { gauge.setValue(value); } catch (Exception e) { } }
    }
}
// |
// EOF
