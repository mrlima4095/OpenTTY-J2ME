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
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0;
    public Hashtable globals = new Hashtable(), father, requireCache = new Hashtable(), labels = new Hashtable();
    public Vector tokens;
    // |
    public int status = 0;
    // | (LuaFunction)
    public static final int PRINT = 0, ERROR = 1, PCALL = 2, REQUIRE = 3, LOADS = 4, PAIRS = 5, GC = 6, TOSTRING = 7, TONUMBER = 8, SELECT = 9, TYPE = 10, GETPROPERTY = 11, SETMETATABLE = 12, GETMETATABLE = 13, IPAIRS = 14, RANDOM = 15;
    public static final int UPPER = 100, LOWER = 101, LEN = 102, FIND = 103, MATCH = 104, REVERSE = 105, SUB = 106, HASH = 107, BYTE = 108, CHAR = 109, TRIM = 110, SPLIT = 111, UUID = 112, GETCMD = 113, GETARGS = 114, ENV = 115, BASE64_ENCODE = 116, BASE64_DECODE = 117, GETPATTERN = 118, STARTSWITH = 119, ENDSWITH = 120;
    public static final int TB_INSERT = 200, TB_CONCAT = 201, TB_REMOVE = 202, TB_SORT = 203, TB_MOVE = 204, TB_UNPACK = 205, TB_PACK = 206, TB_DECODE = 207;
    public static final int EXEC = 300, GETENV = 301, SETENV = 302, CLOCK = 303, SETLOC = 304, EXIT = 305, DATE = 306, GETPID = 307, SETPROC = 308, GETPROC = 309, GETCWD = 310, GETUID = 311, CHDIR = 312, REQUEST = 313, START = 314, STOP = 315, PREQ = 316, SU = 318, REMOVE = 319, SCOPE = 320, JOIN = 321, MKDIR = 322;
    public static final int READ = 400, WRITE = 401, CLOSE = 402, OPEN = 403, POPEN = 404, DIRS = 405, SETOUT = 406, MOUNT = 407, GEN = 408, COPY = 409;
    public static final int HTTP_GET = 500, HTTP_POST = 501, CONNECT = 502, PEER = 503, DEVICE = 504, SERVER = 505, ACCEPT = 506, HTTP_RGET = 507, HTTP_RPOST = 508;
    public static final int DISPLAY = 600, NEW = 601, RENDER = 602, APPEND = 603, ADDCMD = 604, HANDLER = 605, GETCURRENT = 606, TITLE = 607, TICKER = 608, VIBRATE = 609, SETLABEL = 610, SETTEXT = 611, GETLABEL = 612, GETTEXT = 613, CLEAR_SCREEN = 614;
    public static final int CLASS = 700, NAME = 701, DELETE = 702, UPTIME = 703, RUN = 704, THREAD = 705, SLEEP = 706, KERNEL = 1000;
    public static final int AUDIO_LOAD = 800, AUDIO_PLAY = 801, AUDIO_PAUSE = 802, AUDIO_VOLUME = 803, AUDIO_DURATION = 804, AUDIO_TIME = 805;
    public static final int PUSH_REGISTER = 900, PUSH_UNREGISTER = 901, PUSH_LIST = 902, PUSH_PENDING = 903, PUSH_SET_ALARM = 904;

    public static final int EOF = 0, NUMBER = 1, STRING = 2, BOOLEAN = 3, NIL = 4, IDENTIFIER = 5, PLUS = 6, MINUS = 7, MULTIPLY = 8, DIVIDE = 9, MODULO = 10, EQ = 11, NE = 12, LT = 13, GT = 14, LE = 15, GE = 16, AND = 17, OR = 18, NOT = 19, ASSIGN = 20, IF = 21, THEN = 22, ELSE = 23, END = 24, WHILE = 25, DO = 26, RETURN = 27, FUNCTION = 28, LPAREN = 29, RPAREN = 30, COMMA = 31, LOCAL = 32, LBRACE = 33, RBRACE = 34, LBRACKET = 35, RBRACKET = 36, CONCAT = 37, DOT = 38, ELSEIF = 39, FOR = 40, IN = 41, POWER = 42, BREAK = 43, LENGTH = 44, VARARG = 45, REPEAT = 46, UNTIL = 47, COLON = 48, LABEL = 49, GOTO = 50;
    public static final Object LUA_NIL = new Object();
    // |
    public static class Token { int type; Object value; Token(int type, Object value) { this.type = type; this.value = value; } public String toString() { return "Token(type=" + type + ", value=" + value + ")"; } }
    // |
    // Main
    public Lua(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; this.id = id; this.PID = pid; this.proc = proc; this.stdout = stdout; this.father = scope;
        this.tokenIndex = 0; 

        Hashtable os = new Hashtable(), io = new Hashtable(), string = new Hashtable(), table = new Hashtable(), pkg = new Hashtable(), graphics = new Hashtable(), socket = new Hashtable(), http = new Hashtable(), java = new Hashtable(), jdb = new Hashtable(), math = new Hashtable(), audio = new Hashtable(), push = new Hashtable(), base64 = new Hashtable();
        String[] funcs = new String[] { "getenv", "setenv", "clock", "setlocale", "exit", "date", "getpid", "setproc", "getproc", "getcwd", "request", "getuid", "chdir", "open", "su", "remove", "scope", "join", "mkdir" }; 
        int[] loaders = new int[] { GETENV, SETENV, CLOCK, SETLOC, EXIT, DATE, GETPID, SETPROC, GETPROC, GETCWD, REQUEST, GETUID, CHDIR, PREQ, SU, REMOVE, SCOPE, JOIN, MKDIR };
        for (int i = 0; i < funcs.length; i++) { os.put(funcs[i], new LuaFunction(loaders[i])); } os.put("execute", midlet.shell instanceof LuaFunction ? midlet.shell : new LuaFunction(EXEC)); globals.put("os", os);

        funcs = new String[] { "read", "write", "close", "open", "popen", "dirs", "setstdout", "mount", "new", "copy" }; loaders = new int[] { READ, WRITE, CLOSE, OPEN, POPEN, DIRS, SETOUT, MOUNT, GEN, COPY };
        for (int i = 0; i < funcs.length; i++) { io.put(funcs[i], new LuaFunction(loaders[i])); } io.put("stdout", stdout); io.put("stdin", midlet.stdin); globals.put("io", io);

        funcs = new String[] { "insert", "concat", "remove", "sort", "move", "unpack", "pack", "decode" }; loaders = new int[] { TB_INSERT, TB_CONCAT, TB_REMOVE, TB_SORT, TB_MOVE, TB_UNPACK, TB_PACK, TB_DECODE };
        for (int i = 0; i < funcs.length; i++) { table.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("table", table);

        funcs = new String[] { "load", "play", "pause", "volume", "duration", "time" }; loaders = new int[] { AUDIO_LOAD, AUDIO_PLAY, AUDIO_PAUSE, AUDIO_VOLUME, AUDIO_DURATION, AUDIO_TIME };
        for (int i = 0; i < funcs.length; i++) { audio.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("audio", audio);

        funcs = new String[] { "encode", "decode" }; loaders = new int[] { BASE64_ENCODE, BASE64_DECODE };
        for (int i = 0; i < funcs.length; i++) { base64.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("base64", base64);

        funcs = new String[] { "get", "post", "rget", "rpost" }; loaders = new int[] { HTTP_GET, HTTP_POST, HTTP_RGET, HTTP_RPOST };
        for (int i = 0; i < funcs.length; i++) { http.put(funcs[i], new LuaFunction(loaders[i])); } socket.put("http", http);

        funcs = new String[] { "class", "getName", "delete", "run", "thread", "sleep" }; loaders = new int[] { CLASS, NAME, DELETE, RUN, THREAD, SLEEP };
        for (int i = 0; i < funcs.length; i++) { java.put(funcs[i], new LuaFunction(loaders[i])); } jdb.put("username", midlet.username); jdb.put("cache", midlet.cache); jdb.put("build", midlet.build); jdb.put("uptime", new LuaFunction(UPTIME)); java.put("midlet", jdb); globals.put("java", java);

        funcs = new String[] { "connect", "peer", "device", "server", "accept" }; loaders = new int[] { CONNECT, PEER, DEVICE, SERVER, ACCEPT };
        for (int i = 0; i < funcs.length; i++) { socket.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("socket", socket);

        funcs = new String[] { "register", "unregister", "list", "pending", "setAlarm" }; loaders = new int[] { PUSH_REGISTER, PUSH_UNREGISTER, PUSH_LIST, PUSH_PENDING, PUSH_SET_ALARM };
        for (int i = 0; i< funcs.length; i++) { push.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("push", push);

        funcs = new String[] { "display", "new", "render", "append", "addCommand", "handler", "getCurrent", "SetTitle", "SetTicker", "vibrate", "SetLabel", "SetText", "GetLabel", "GetText", "clear" }; loaders = new int[] { DISPLAY, NEW, RENDER, APPEND, ADDCMD, HANDLER, GETCURRENT, TITLE, TICKER, VIBRATE, SETLABEL, SETTEXT, GETLABEL, GETTEXT, CLEAR_SCREEN };
        for (int i = 0; i < funcs.length; i++) { graphics.put(funcs[i], new LuaFunction(loaders[i])); } graphics.put("db", midlet.graphics); graphics.put("fire", List.SELECT_COMMAND); globals.put("graphics", graphics);

        funcs = new String[] { "upper", "lower", "len", "find", "match", "reverse", "sub", "hash", "byte", "char", "trim", "uuid", "split", "getCommand", "getArgument", "env", "getpattern", "startswith", "endswith" }; loaders = new int[] { UPPER, LOWER, LEN, FIND, MATCH, REVERSE, SUB, HASH, BYTE, CHAR, TRIM, UUID, SPLIT, GETCMD, GETARGS, ENV, GETPATTERN, STARTSWITH, ENDSWITH };
        for (int i = 0; i < funcs.length; i++) { string.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("string", string);

        funcs = new String[] { "print", "error", "pcall", "require", "load", "pairs", "ipairs", "collectgarbage", "tostring", "tonumber", "select", "type", "getAppProperty", "setmetatable", "getmetatable" }; 
        loaders = new int[] { PRINT, ERROR, PCALL, REQUIRE, LOADS, PAIRS, IPAIRS, GC, TOSTRING, TONUMBER, SELECT, TYPE, GETPROPERTY, SETMETATABLE, GETMETATABLE };
        for (int i = 0; i < funcs.length; i++) { globals.put(funcs[i], new LuaFunction(loaders[i])); }

        pkg.put("loaded", requireCache); pkg.put("loadlib", new LuaFunction(REQUIRE)); globals.put("package", pkg);
        math.put("random", new LuaFunction(RANDOM)); globals.put("math", math);
        globals.put("_VERSION", "Lua J2ME"); globals.put("_G", globals);
    }
    // | (Run Source code)
    public Hashtable run(String source, String code, Hashtable args) { 
        midlet.sys.put(PID, proc); globals.put("arg", args);

        Hashtable ITEM = new Hashtable(); 
        
        try { 
            this.tokens = tokenize(code); collectLabels();
            
            while (peek().type != EOF) { Object res = statement(globals); if (doreturn) { if (res != null) { ITEM.put("object", res); } doreturn = false; break; } }
        } 
        catch (Exception e) { midlet.print(midlet.getCatch(e), stdout, id, father); status = 1; } 
        catch (Error e) { if (e.getMessage() != null) { midlet.print(e.getMessage(), stdout, id, father); } status = 1; }

        if (kill) { midlet.sys.remove(PID); }
        ITEM.put("status", status);
        return ITEM;
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
            }
    
            else if (c == '.') {
                if (i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') { tokens.addElement(new Token(VARARG, "...")); i += 3; } 
                else if (i + 1 < code.length() && code.charAt(i + 1) == '.') { tokens.addElement(new Token(CONCAT, "..")); i += 2; } 
                else { tokens.addElement(new Token(DOT, ".")); i++; }
            }
            else if (c == ':') {
                if (i + 1 < code.length() && code.charAt(i + 1) == ':') {
                    i += 2;
                    
                    StringBuffer sb = new StringBuffer();
                    while (i < code.length() && (isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) { sb.append(code.charAt(i)); i++; }
                    
                    if (i + 1 < code.length() && code.charAt(i) == ':' && code.charAt(i + 1) == ':') { i += 2; tokens.addElement(new Token(LABEL, sb.toString())); }
                    else { i -= 2; tokens.addElement(new Token(COLON, ":")); i++; }
                } else { tokens.addElement(new Token(COLON, ":")); i++; }
            }

            else if (isDigit(c) || (c == '.' && i + 1 < code.length() && isDigit(code.charAt(i + 1)))) {
                StringBuffer sb = new StringBuffer();
                boolean hasDecimal = false;
                while (i < code.length() && (isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) { break; }
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') { break; }
                        hasDecimal = true;
                    }
                    sb.append(code.charAt(i));
                    i++;
                }
                try { double numValue = Double.parseDouble(sb.toString()); tokens.addElement(new Token(NUMBER, new Double(numValue))); } 
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); }
                continue;
            }
            else if (c == '-' && i + 1 < code.length() && (isDigit(code.charAt(i + 1)) || (code.charAt(i + 1) == '.' && i + 2 < code.length() && isDigit(code.charAt(i + 2))))) {
                i++; 
                StringBuffer sb = new StringBuffer();
                sb.append('-'); 
                
                boolean hasDecimal = false;
                while (i < code.length() && (isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) { break; }
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') { break; }
                        hasDecimal = true;
                    }
                    sb.append(code.charAt(i));
                    i++;
                }
                try { double numValue = Double.parseDouble(sb.toString()); tokens.addElement(new Token(NUMBER, new Double(numValue))); } 
                catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); }
            }

            else if (c == '"' || c == '\'') { char quoteChar = c; StringBuffer sb = new StringBuffer(); i++; while (i < code.length() && code.charAt(i) != quoteChar) { sb.append(code.charAt(i)); i++; } if (i < code.length() && code.charAt(i) == quoteChar) { i++; } tokens.addElement(new Token(STRING, sb.toString())); }
            else if (c == '[' && i + 1 < code.length() && code.charAt(i + 1) == '[') { i += 2; StringBuffer sb = new StringBuffer(); while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) { sb.append(code.charAt(i)); i++; } if (i + 1 < code.length()) { i += 2; } tokens.addElement(new Token(STRING, sb.toString())); }

            else if (isLetter(c)) { StringBuffer sb = new StringBuffer(); while (i < code.length() && isLetterOrDigit(code.charAt(i))) { sb.append(code.charAt(i)); i++; } String word = sb.toString(); tokens.addElement(new Token((word.equals("true") || word.equals("false")) ? BOOLEAN : word.equals("nil") ? NIL : word.equals("and") ? AND : word.equals("or") ? OR : word.equals("not") ? NOT : word.equals("if") ? IF : word.equals("then") ? THEN : word.equals("else") ? ELSE : word.equals("elseif") ? ELSEIF : word.equals("end") ? END : word.equals("while") ? WHILE : word.equals("do") ? DO : word.equals("return") ? RETURN : word.equals("function") ? FUNCTION : word.equals("local") ? LOCAL : word.equals("for") ? FOR : word.equals("in") ? IN : word.equals("break") ? BREAK : word.equals("repeat") ? REPEAT : word.equals("until") ? UNTIL : word.equals("goto") ? GOTO : IDENTIFIER, word)); }
    
            else if (c == '+') { tokens.addElement(new Token(PLUS, "+")); i++; }
            else if (c == '-') { tokens.addElement(new Token(MINUS, "-")); i++; }
            else if (c == '*') { tokens.addElement(new Token(MULTIPLY, "*")); i++; }
            else if (c == '/') { tokens.addElement(new Token(DIVIDE, "/")); i++; }
            else if (c == '%') { tokens.addElement(new Token(MODULO, "%")); i++; }
            else if (c == '(') { tokens.addElement(new Token(LPAREN, "(")); i++; }
            else if (c == ')') { tokens.addElement(new Token(RPAREN, ")")); i++; }
            else if (c == ',') { tokens.addElement(new Token(COMMA, ",")); i++; }
            else if (c == '^') { tokens.addElement(new Token(POWER, "^")); i++; }
            else if (c == '#') { tokens.addElement(new Token(LENGTH, "#")); i++; }
    
            else if (c == '=') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(EQ, "==")); i += 2; } else { tokens.addElement(new Token(ASSIGN, "=")); i++; } }
            else if (c == '~') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(NE, "~=")); i += 2; } else { throw new Exception("Unexpected character '~'"); } }
            else if (c == '<') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(LE, "<=")); i += 2; } else { tokens.addElement(new Token(LT, "<")); i++; } }
            else if (c == '>') { if (i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new Token(GE, ">=")); i += 2; } else { tokens.addElement(new Token(GT, ">")); i++; } }

            else if (c == '{') { tokens.addElement(new Token(LBRACE, "{")); i++; }
            else if (c == '}') { tokens.addElement(new Token(RBRACE, "}")); i++; }
            else if (c == '[') { tokens.addElement(new Token(LBRACKET, "[")); i++; }
            else if (c == ']') { tokens.addElement(new Token(RBRACKET, "]")); i++; }

            else { throw new Exception("Unexpected character '" + c + "'"); }
        }

        tokens.addElement(new Token(EOF, "EOF"));
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

        if (status != 0) { midlet.sys.remove(PID); throw new Error(); }
        if (midlet.sys.containsKey(PID)) { } else { throw new Error("Process killed"); } 

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
                    else { assignValues.addElement(v); }
                }

                for (int i = 0; i < varNames.size(); i++) {
                    String v = (String) varNames.elementAt(i);
                    Object val = i < assignValues.size() ? assignValues.elementAt(i) : null;
                    scope.put(v, val == null ? LUA_NIL : val);
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
                    Object value = expression(scope);
                    ((Hashtable) targetTable).put(key, value == null ? LUA_NIL : value);
                    return null;
                } 
                else if (peek().type == LPAREN) { return callFunctionObject(unwrap(((Hashtable) targetTable).get(key)), scope); }
                else { return unwrap(((Hashtable) targetTable).get(key)); }
            } 
            else if (peek().type == COLON) {
                Object self = unwrap(scope.get(varName));
                if (self == null && globals.containsKey(varName)) { self = unwrap(globals.get(varName)); }
                if (self == null) { throw new Exception("attempt to call method on nil value: " + varName); }

                consume(COLON);
                String methodName = (String) consume(IDENTIFIER).value;
                Object methodObj = resolveMethod(self);

                if (methodObj instanceof Hashtable) {
                    Hashtable table = (Hashtable) methodObj;
                    Object fn = unwrap(table.get(methodName));
                    if (fn == null) { throw new Exception("method '" + methodName + "' not found " + ((methodObj == self && self instanceof Hashtable) ? "in table: " + varName : "for type: " + LuaFunction.type(self))); }
                    
                    return callMethod(self, varName, fn, methodName, scope);
                }

                throw new Exception("attempt to call method on unsupported type: " + LuaFunction.type(self));
            }
            else {
                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object value = expression(scope);
                    scope.put(varName, value == null ? LUA_NIL : value);
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

                        scope.put(name, new Double(iVal));

                        int originalTokenIndex = tokenIndex;
                        Vector originalTokens = tokens;

                        tokens = bodyTokens;
                        tokenIndex = 0;

                        Object ret = null;
                        while (peek().type != EOF) {
                            ret = statement(scope);
                            if (doreturn) { break; }
                        }

                        tokenIndex = originalTokenIndex;
                        tokens = originalTokens;

                        if (ret != null) { return ret; }

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
                                
                                if (names.size() >= 1) scope.put((String) names.elementAt(0), new Double(idx + 1));
                                if (names.size() >= 2) scope.put((String) names.elementAt(1), item == null ? LUA_NIL : item);

                                // Atualiza o índice no iterador
                                iterator.put("__index", new Double(idx + 1));

                                int originalTokenIndex = tokenIndex;
                                Vector originalTokens = tokens;
                                tokens = bodyTokens;
                                tokenIndex = 0;

                                Object ret = null;
                                while (peek().type != EOF) {
                                    ret = statement(scope);
                                    if (doreturn) return ret;
                                }

                                tokenIndex = originalTokenIndex;
                                tokens = originalTokens;
                                if (ret != null) return ret;

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
                            Object v = unwrap(ht.get(k));
                            if (names.size() >= 1) scope.put((String) names.elementAt(0), (k == null ? LUA_NIL : k));
                            if (names.size() >= 2) scope.put((String) names.elementAt(1), (v == null ? LUA_NIL : v));

                            int originalTokenIndex = tokenIndex;
                            Vector originalTokens = tokens;
                            tokens = bodyTokens;
                            tokenIndex = 0;

                            Object ret = null;
                            while (peek().type != EOF) {
                                ret = statement(scope);
                                if (doreturn) { return ret; }
                            }

                            tokenIndex = originalTokenIndex;
                            tokens = originalTokens;
                            if (ret != null) { return ret; }

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
                                k = new Double(idx + 1);
                                v = item;
                            }
                            if (names.size() >= 1) scope.put((String) names.elementAt(0), (k == null ? LUA_NIL : k));
                            if (names.size() >= 2) scope.put((String) names.elementAt(1), (v == null ? LUA_NIL : v));

                            int originalTokenIndex = tokenIndex;
                            Vector originalTokens = tokens;
                            tokens = bodyTokens;
                            tokenIndex = 0;

                            Object ret = null;
                            while (peek().type != EOF) {
                                ret = statement(scope);
                                if (doreturn) { return ret; }
                            }

                            tokenIndex = originalTokenIndex;
                            tokens = originalTokens;
                            if (ret != null) return ret;

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
            doreturn = true;

            if (peek().type == EOF || peek().type == END) { return new Vector(); }

            Vector results = new Vector();
            results.addElement(expression(scope));
            while (peek().type == COMMA) { consume(COMMA); results.addElement(expression(scope)); }
            return results;
        }

        else if (current.type == FUNCTION) {
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

            if (isTableAssignment) { ((Hashtable) targetTable).put(key, func); } 
            else { scope.put(funcName, func); }

            return null;
        } 
        else if (current.type == LOCAL) {
            consume(LOCAL);

            if (peek().type == FUNCTION) {
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
    
                    // Tokens que ABREM blocos
                    if (token.type == FUNCTION || token.type == IF || token.type == DO) { 
                        depth++; 
                    }
                    // Tokens que FECHAM blocos  
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
                        } else { assignValues.addElement(v); }
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
            
            // Executar o bloco DO
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
    private Object logicalOr(Hashtable scope) throws Exception { Object left = logicalAnd(scope); while (peek().type == OR) { consume(OR); Object right = logicalAnd(scope); left = isTruthy(left) ? left : right; } return left; }
    private Object logicalAnd(Hashtable scope) throws Exception { Object left = comparison(scope); while (peek().type == AND) { consume(AND); Object right = comparison(scope); left = isTruthy(left) ? right : left; } return left; }
    private Object comparison(Hashtable scope) throws Exception { Object left = concatenation(scope); while (peek().type == EQ || peek().type == NE || peek().type == LT || peek().type == GT || peek().type == LE || peek().type == GE) { Token op = consume(); Object right = concatenation(scope); if (op.type == EQ) { left = new Boolean((left == null && right == null) || (left != null && left.equals(right))); } else if (op.type == NE) { left = new Boolean(!((left == null && right == null) || (left != null && left.equals(right)))); } else if (op.type == LT) { left = new Boolean(((Double) left).doubleValue() < ((Double) right).doubleValue()); } else if (op.type == GT) { left = new Boolean(((Double) left).doubleValue() > ((Double) right).doubleValue()); } else if (op.type == LE) { left = new Boolean(((Double) left).doubleValue() <= ((Double) right).doubleValue()); } else if (op.type == GE) { left = new Boolean(((Double) left).doubleValue() >= ((Double) right).doubleValue()); } } return left; }
    // |
    // Strings
    private String toLuaString(Object obj) { if (obj == null || obj == LUA_NIL) { return "nil"; } if (obj instanceof Boolean) { return ((Boolean)obj).booleanValue() ? "true" : "false"; } if (obj instanceof Double) { double d = ((Double)obj).doubleValue(); if (d == (long)d) return String.valueOf((long)d); return String.valueOf(d); } return midlet.escape(obj.toString()); }
    private Object concatenation(Hashtable scope) throws Exception { Object left = arithmetic(scope); while (peek().type == CONCAT) { consume(CONCAT); Object right = arithmetic(scope); left = toLuaString(left) + toLuaString(right); } return left; }
    // |
    // Arithmetic
    private Object arithmetic(Hashtable scope) throws Exception {
        Object left = term(scope); 
        while (peek().type == PLUS || peek().type == MINUS) {
            Token op = consume();
            Object right = term(scope); 
            if (!(left instanceof Double) || !(right instanceof Double)) { throw new ArithmeticException("Arithmetic operation on non-number types."); }

            double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();
            if (op.type == PLUS) { left = new Double(lVal + rVal); } 
            else if (op.type == MINUS) { left = new Double(lVal - rVal); }
        }
        return left;
    }
    private Object term(Hashtable scope) throws Exception {
        Object left = exponentiation(scope);
        while (peek().type == MULTIPLY || peek().type == DIVIDE || peek().type == MODULO) {
            Token op = consume();
            Object right = exponentiation(scope);
            if (!(left instanceof Double) || !(right instanceof Double)) { throw new ArithmeticException("Arithmetic operation on non-number types."); }
            double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();

            if (op.type == MULTIPLY) { left = new Double(lVal * rVal); } 
            else if (op.type == DIVIDE) { if (rVal == 0) { throw new Exception("Division by zero."); } left = new Double(lVal / rVal); } 
            else if (op.type == MODULO) { if (rVal == 0) { throw new Exception("Modulo by zero."); } left = new Double(lVal % rVal); }
        }
        return left;
    }
    private Object exponentiation(Hashtable scope) throws Exception {
        Object left = factor(scope);
        while (peek().type == POWER) {
            consume(POWER);
            Object right = factor(scope);
            if (!(left instanceof Double) || !(right instanceof Double)) { throw new ArithmeticException("Arithmetic operation on non-number types.");  }

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

            left = new Double(result);
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
            else if (current.type == BOOLEAN) { consume(BOOLEAN); base = new Boolean(current.value.equals("true")); } 
            else if (current.type == NIL) { consume(NIL); base = null; }

            while (peek().type == DOT || peek().type == COLON) {
                if (peek().type == DOT) {
                    consume(DOT);
                    String field = (String) consume(IDENTIFIER).value;

                    Object module = resolveMethod(base);
                    if (!(module instanceof Hashtable)) { throw new Exception("attempt to index non-table value after literal"); }
                    base = unwrap(((Hashtable) module).get(field));
                } 
                else if (peek().type == COLON) {
                    consume(COLON);
                    String method = (String) consume(IDENTIFIER).value;

                    Object module = resolveMethod(base);
                    if (!(module instanceof Hashtable)) { throw new Exception("attempt to call method on non-table after literal"); }

                    Object func = unwrap(((Hashtable) module).get(method));
                    if (func == null) { throw new Exception("method '" + method + "' not found for type: " + LuaFunction.type(base)); }

                    base = callMethod(base, null, func, method, scope);
                }
            }

            return base;
        }
        else if (current.type == NOT) { consume(NOT); return new Boolean(!isTruthy(factor(scope))); } 
        else if (current.type == LPAREN) { consume(LPAREN); Object value = expression(scope); consume(RPAREN); return value; } 
        else if (current.type == LENGTH) { consume(LENGTH); Object val = factor(scope); if (val == null || val instanceof Boolean) { throw new RuntimeException("attempt to get length of a " + (val == null ? "nil" : "boolean") + " value"); } else if (val instanceof String) { return new Double(((String) val).length()); } else if (val instanceof Hashtable) { return new Double(((Hashtable) val).size()); } else if (val instanceof Vector) { return new Double(((Vector) val).size()); } else if (val instanceof InputStream) { return new Double(((InputStream) val).available()); } else { return new Double(0); } }
        else if (current.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            Object value = unwrap(scope.get(name));
            if (value == null && scope == globals == false) { }
            if (value == null && globals.containsKey(name)) { value = unwrap(globals.get(name)); }
            while (peek().type == LBRACKET || peek().type == DOT) {
                Object key = null;
                if (peek().type == LBRACKET) { consume(LBRACKET); key = expression(scope); consume(RBRACKET); } 
                else { consume(DOT); key = (String) consume(IDENTIFIER).value; }

                if (value == null) { return null; }
                if (!(value instanceof Hashtable)) { throw new Exception("attempt to index a non-table value"); }

                value = unwrap(((Hashtable)value).get(key));
            }

            if (peek().type == COLON) {
                String objectName = (String) ((Token) tokens.elementAt(tokenIndex - 1)).value;
            
                Object self = unwrap(scope.get(objectName));
                if (self == null && globals.containsKey(objectName)) { self = unwrap(globals.get(objectName)); }
                if (self == null) { throw new Exception("attempt to call method on nil value: " + objectName); }
            
                consume(COLON);
                String methodName = (String) consume(IDENTIFIER).value;
            
                Object module = resolveMethod(self), func = null;
            
                if (module == self && self instanceof Hashtable) { func = unwrap(((Hashtable) self).get(methodName)); } 
                else if (module instanceof Hashtable) { func = unwrap(((Hashtable) module).get(methodName)); }
            
                if (func == null) { throw new Exception("method '" + methodName + "' not found for type: " + LuaFunction.type(self)); }
            
                return callMethod(self, objectName, func, methodName, scope);
            }
            else if (peek().type == LPAREN) { return callFunctionObject(value, scope); }

            return value;
        }
        else if (current.type == FUNCTION) {
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

            // Na parte de função anônima
            Vector bodyTokens = new Vector();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                
                // Tokens que ABREM blocos
                if (token.type == FUNCTION || token.type == IF || token.type == DO) { 
                    depth++; 
                }
                // Tokens que FECHAM blocos
                else if (token.type == END) { 
                    depth--; 
                    if (depth > 0) { 
                        bodyTokens.addElement(token); 
                    }
                }
                else if (token.type == EOF) { 
                    throw new RuntimeException("Unmatched 'function' statement: Expected 'end'"); 
                }
                if (depth > 0) { 
                    bodyTokens.addElement(token); 
                }
            }

            return new LuaFunction(params, bodyTokens, scope);
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
                else { value = expression(scope); key = new Double(index++); }

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
    private Object callFunction(String funcName, Hashtable scope) throws Exception {
        consume(LPAREN);
        Vector args = new Vector();
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) { consume(COMMA); args.addElement(expression(scope)); }
        }
        consume(RPAREN);

        Object  funcObj = unwrap(scope.get(funcName));
        if (funcObj == null && globals.containsKey(funcName)) { funcObj = unwrap(globals.get(funcName)); }

        if (funcObj instanceof LuaFunction) { return ((LuaFunction) funcObj).call(args); }
        else { throw new RuntimeException("Attempt to call a non-function value: " + funcName); }
    }
    private Object callFunctionObject(Object funcObj, Hashtable scope) throws Exception {
        consume(LPAREN);
        Vector args = new Vector();
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) { consume(COMMA); args.addElement(expression(scope)); }
        }
        consume(RPAREN);

        if (funcObj instanceof LuaFunction) { return ((LuaFunction) funcObj).call(args); }
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
    private Object callMethod(Object self, String varName, Object methodObj, String methodName, Hashtable scope) throws Exception {
        if (methodObj == null) {
            methodObj = resolveMethod(self);
            Object table = unwrap(scope.get(varName));
            if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
            Object key = null;
            
            while (peek().type == DOT || peek().type == LBRACKET) {
                if (peek().type == DOT) { consume(DOT); Token field = consume(IDENTIFIER); key = field.value; } 
                else if (peek().type == LBRACKET) { consume(LBRACKET); key = expression(scope); consume(RBRACKET); }
        
                if (table == null) { throw new Exception("attempt to index a nil value"); }
                if (!(table instanceof Hashtable)) { throw new Exception("attempt to index a non-table value"); }
                if (peek().type == DOT || peek().type == LBRACKET) { methodObj = unwrap(((Hashtable)table).get(key)); }
            }
        }
        consume(LPAREN);
        Vector args = new Vector();
        
        args.addElement(self);
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) { consume(COMMA); args.addElement(expression(scope)); }
        }
        consume(RPAREN);
        if (methodObj instanceof LuaFunction) { return ((LuaFunction) methodObj).call(args); } 
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
    private Object[] resolveTableAndKey(String varName, Hashtable scope) throws Exception {
        Object table = unwrap(scope.get(varName));
        if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
        Object key = null;
    
        while (peek().type == DOT || peek().type == LBRACKET) {
            if (peek().type == DOT) { consume(DOT); Token field = consume(IDENTIFIER); key = field.value; } 
            else if (peek().type == LBRACKET) { consume(LBRACKET); key = expression(scope); consume(RBRACKET); }

            if (table == null) { throw new Exception("attempt to index a nil value"); }
            if (!(table instanceof Hashtable)) { throw new Exception("attempt to index a non-table value"); }
            if (peek().type == DOT || peek().type == LBRACKET) { table = unwrap(((Hashtable)table).get(key)); }
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
    private static boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private static boolean isLetterOrDigit(char c) { return isLetter(c) || isDigit(c); }
    // |
    public Object getKernel() { return new LuaFunction(KERNEL); }
    // |
    // Lua Object
    public class LuaFunction implements Runnable, CommandListener, ItemCommandListener, ItemStateListener {
        private Vector params, bodyTokens;
        private Hashtable closureScope, cmds = null; 
        private int MOD = -1;
        // | (Screen)
        private Object root = null;
        private String handler = "";
        // | 
        // Config.
        LuaFunction(Vector params, Vector bodyTokens, Hashtable closureScope) { this.params = params; this.bodyTokens = bodyTokens; this.closureScope = closureScope; }
        LuaFunction(String handler, Object root) { this.handler = handler; this.root = root; }
        LuaFunction(Hashtable cmds) { this.cmds = cmds; }
        LuaFunction(LuaFunction root) { this.root = root; }
        LuaFunction(int type) { this.MOD = type; }
        // | (Main)
        public Object call(Vector args) throws Exception {
            if (MOD != -1) { return internals(args); }

            Hashtable functionScope = new Hashtable();
            for (Enumeration e = closureScope.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); functionScope.put(key, unwrap(closureScope.get(key))); }
            for (Enumeration e = globals.keys(); e.hasMoreElements();) { String key = (String) e.nextElement(); if (!functionScope.containsKey(key)) { functionScope.put(key, unwrap(globals.get(key))); } }

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
                for (int i = fixedParamCount; i < args.size(); i++) { Object obj = args.elementAt(i); varargValues.put(new Double(index++), obj == null ? LUA_NIL : obj); }
                functionScope.put("...", varargValues);
            }

            int originalTokenIndex = tokenIndex;
            Vector originalTokens = tokens;

            tokens = bodyTokens;
            tokenIndex = 0;

            Object returnValue = null;
            while (peek().type != EOF) {
                Object result = statement(functionScope);
                
                if (doreturn) {
                    returnValue = result;
                    doreturn = false;
                    break;
                }
            }

            tokenIndex = originalTokenIndex;
            tokens = originalTokens;

            return returnValue;
        }
        public Object internals(Vector args) throws Exception {
            // Globals
            if (MOD == PRINT) { 
                if (args.isEmpty()) { }
                else {
                    StringBuffer buffer = new StringBuffer(); 
                    for (int i = 0; i < args.size(); i++) {
                        Object a = args.elementAt(i);

                        if (a instanceof Vector) {
                            Vector vv = (Vector) a;
                            for (int j = 0; j < vv.size(); j++) {
                                buffer.append(toLuaString(vv.elementAt(j)));
                                if (j < vv.size() - 1) { buffer.append("\t"); }
                            }
                        } 
                        else { buffer.append(toLuaString(a)); }

                        if (i < args.size() - 1) buffer.append("\t");
                    }

                    midlet.print(buffer.toString(), stdout, id, father); 
                } 
            }
            else if (MOD == ERROR) { String msg = toLuaString((args.size() > 0) ? args.elementAt(0) : null); throw new Exception(msg.equals("nil") ? "error" : msg); } 
            else if (MOD == PCALL) {
                if (args.isEmpty()) { return gotbad(1, "pcall", "value expected"); }
                else {
                    Vector result = new Vector(), fnArgs = new Vector();

                    if (args.elementAt(0) instanceof LuaFunction) {
                        LuaFunction func = (LuaFunction) unwrap(args.elementAt(0));
                        for (int i = 1; i < args.size(); i++) { fnArgs.addElement(unwrap(args.elementAt(i))); }

                        try { 
                            Object value = func.call(fnArgs); 
                            result.addElement(Boolean.TRUE); 

                            if (value instanceof Vector) { Vector v = (Vector) value; for (int i = 0; i < v.size(); i++) { result.addElement(v.elementAt(i)); } }
                            else { result.addElement(value); }
                        }
                        catch (Exception e) { result.addElement(Boolean.FALSE); result.addElement(midlet.getCatch(e)); }
                    }
                    else { result.addElement(Boolean.FALSE); result.addElement("attempt to call a " + type(args.elementAt(0)) + " value"); } 

                    return result;
                }
            }
            else if (MOD == REQUIRE) {
                if (args.isEmpty()) { return gotbad(1, "require", "string expected, got no value"); }
                else if (args.elementAt(0) instanceof String) {
                    String name = toLuaString(args.elementAt(0));

                    Object cached = requireCache.get(name);
                    if (cached != null) { return (cached == LUA_NIL) ? null : cached; }

                    String code = midlet.getcontent(name, father);
                    if (code.equals("")) { if ((code = midlet.getcontent("/lib/" + name + ".lua", father)).equals("")) { if ((code = midlet.getcontent("/lib/" + name + ".so", father)).equals("")) { throw new Exception("module '" + code + "' not found"); } } } 

                    Object obj = exec(code, null);
                    requireCache.put(name, (obj == null) ? LUA_NIL : obj);
                    return obj;
                } 
                else { return gotbad(1, "require", "string expected, got " + type(args.elementAt(0))); }
            }
            else if (MOD == LOADS) { if (args.isEmpty() || args.elementAt(0) == null) { } else { return exec(toLuaString(args.elementAt(0)), args.size() > 1 ? (args.elementAt(1) instanceof Hashtable ? (Hashtable) args.elementAt(1) : null) : null); } }
            else if (MOD == PAIRS) { 
                if (args.isEmpty()) { return gotbad(1, "pairs", "table expected, got no value"); } 
                else {
                    Object t = args.elementAt(0);
                    t = (t == LUA_NIL) ? null : t;
                    if (t == null || t instanceof Hashtable || t instanceof Vector) { return t; }
                    else { return gotbad(1, "pairs", "table expected, got " + type(t)); }
                }
            }
            else if (MOD == IPAIRS) { 
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
            }
            else if (MOD == GC) {
                if (args.isEmpty()) { System.gc(); }
                else {
                    String opt = toLuaString(args.elementAt(0));

                    if (opt.equals("stop")) { gc = false; }
                    else if (opt.equals("collect") || opt.equals("restart")) { System.gc(); }
                    else if (opt.equals("free")) { return new Double(midlet.runtime.totalMemory() / 1024); }
                    else if (opt.equals("total")) { return new Double(midlet.runtime.freeMemory() / 1024); }
                    else if (opt.equals("count")) { return new Double((midlet.runtime.totalMemory() - midlet.runtime.freeMemory()) / 1024); }
                    else if (opt.equals("step")) { return Boolean.FALSE; }
                    else if (opt.equals("isrunning")) { return new Boolean(gc); }
                    else if (opt.equals("generational") || opt.equals("incremental")) { return "generational"; }
                    else { return gotbad(1, "collectgarbage", "invalid option '" + opt + "'"); }

                    return new Double(0);
                }
            }
            else if (MOD == TOSTRING) { return toLuaString(args.isEmpty() ? gotbad(1, "tostring", "value expected") : args.elementAt(0)); }
            else if (MOD == TONUMBER) { return args.isEmpty() ? gotbad(1, "tostring", "value expected") : new Double(Double.valueOf(toLuaString(args.elementAt(0)))); }
            else if (MOD == SELECT) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "select", "number expected, got no value"); } 
                else {
                    String idx = toLuaString(args.elementAt(0));
                    if (idx.equals("#")) {
                        if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) { return new Double(((Hashtable) args.elementAt(1)).size()); } 
                        else { return new Double(args.size() - 1); }
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
            }
            else if (MOD == TYPE) { return args.isEmpty() ? gotbad(1, "type", "value expected") : type(args.elementAt(0)); }
            else if (MOD == GETPROPERTY) { if (args.isEmpty()) { } else { String query = toLuaString(args.elementAt(0)); return query.startsWith("/") ? System.getProperty(query.substring(1)) : midlet.getAppProperty(query); } }
            else if (MOD == RANDOM) { Double gen = new Double(midlet.random.nextInt(getNumber(args.isEmpty() ? "100" : toLuaString(args.elementAt(0)), 100))); return args.isEmpty() ? new Double(gen.doubleValue() / 100) : gen; }
            else if (MOD == SETMETATABLE) {
                if (args.size() < 2) return gotbad(1, "setmetatable", "table expected, got no value");
            
                Object table = unwrap(args.elementAt(0));
                Object mt = unwrap(args.elementAt(1));
            
                if (!(table instanceof Hashtable))
                    return gotbad(1, "setmetatable", "table expected, got " + type(table));
                if (mt != null && !(mt instanceof Hashtable))
                    return gotbad(2, "setmetatable", "nil or table expected, got " + type(mt));
            
                ((Hashtable) table).put("__metatable", mt == null ? LUA_NIL : mt);
                return table;
            }
            else if (MOD == GETMETATABLE) {
                if (args.isEmpty())
                    return gotbad(1, "getmetatable", "table expected, got no value");
            
                Object table = unwrap(args.elementAt(0));
                if (!(table instanceof Hashtable))
                    return gotbad(1, "getmetatable", "table expected, got " + type(table));
            
                Object mt = ((Hashtable) table).get("__metatable");
                return (mt == LUA_NIL || mt == null) ? null : mt;
            }
            // Package: os
            else if (MOD == GETENV) { return args.isEmpty() ? midlet.attributes : midlet.attributes.get(toLuaString(args.elementAt(0))); }
            else if (MOD == SETENV) { 
                if (args.isEmpty()) { } 
                else {
                    if (args.size() > 1) {
                        if (args.elementAt(1) == null) { midlet.attributes.remove(toLuaString(args.elementAt(0))); }
                        else { midlet.attributes.put(toLuaString(args.elementAt(0)), toLuaString(args.elementAt(1))); }
                    }
                    else { midlet.attributes.remove(toLuaString(args.elementAt(0))); }
                }
            }
            else if (MOD == CLOCK) { return System.currentTimeMillis() - uptime; }
            else if (MOD == SETLOC) { if (args.isEmpty()) { } else { midlet.attributes.put("LOCALE", toLuaString(args.elementAt(0))); } }
            else if (MOD == EXIT) { if (PID.equals("1")) { midlet.destroyApp(true); } midlet.sys.remove(PID); if (args.isEmpty()) { throw new Error(); } else { status = getNumber(toLuaString(args.elementAt(0)), 1); } }
            else if (MOD == DATE) { return new java.util.Date().toString(); }
            else if (MOD == GETPID) { return args.isEmpty() || args.elementAt(0) == null ? PID : midlet.getpid(toLuaString(args.elementAt(0))); }
            else if (MOD == SETPROC) {
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
                    else { if (value == null) { proc.db.remove(attribute); } else { proc.db.put(attribute, value); } }
                } 
            }
            else if (MOD == GETPROC) {
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
                }
            }
            else if (MOD == GETCWD) { return father.get("PWD"); }
            else if (MOD == REQUEST) {
                if (args.isEmpty()) { return gotbad(1, "request", "string expected, got no value"); }
                else if (args.size() < 2) { return gotbad(2, "request", "value expected, got no value"); }
                else if (midlet.sys.containsKey(toLuaString(args.elementAt(0)))) {
                    Process process = (Process) midlet.sys.get(toLuaString(args.elementAt(0)));
                    if (process.lua != null && process.handler != null) {
                        Lua lua = (Lua) process.lua;
                        Vector arg = new Vector(); arg.addElement(toLuaString(args.elementAt(1))); arg.addElement(args.size() > 2 ? args.elementAt(2) : null); arg.addElement(father); arg.addElement(PID); arg.addElement(new Double(id));
                        Object response = null;

                        try { response = ((Lua.LuaFunction) process.handler).call(arg); }
                        catch (Exception e) { return midlet.getCatch(e); } 
                        catch (Error e) { if (e.getMessage() != null) { midlet.print(e.getMessage(), stdout, id, father); } return new Double(lua.status); }

                        return response;
                    } 
                    else { return gotbad(1, "request", "not a service"); }
                } 
                else { return gotbad(1, "request", "process not found"); }
            }
            else if (MOD == GETUID) { if (args.isEmpty()) { return new Double(id); } return new Double(midlet.getUserID(toLuaString(args.elementAt(0)))); }
            else if (MOD == CHDIR) {
                if (args.isEmpty()) { return father.get("PWD"); }
                else {
                    String pwd = (String) father.get("PWD"), target = toLuaString(args.elementAt(0));
                    if (target.equals("") || target == null) { father.put("PWD", "/home/"); return new Double(0); }
                    else if (target.equals("..")) {
                        if (pwd.equals("/")) { return new Double(1); }
                        
                        int lastSlashIndex = pwd.lastIndexOf('/', pwd.endsWith("/") ? pwd.length() - 2 : pwd.length() - 1);
                        father.put("PWD", (lastSlashIndex <= 0) ? "/" : pwd.substring(0, lastSlashIndex + 1));

                        return new Double(0);
                    }

                    if (midlet.fs.containsKey(target)) { father.put("PWD", target); return new Double(0); }
                    else if (target.startsWith("/mnt/")) {
                        FileConnection fc = (FileConnection) Connector.open("file:///" + target.substring(5), Connector.READ); 
                        boolean exist = fc.exists(), dir = fc.isDirectory();
                        fc.close(); 
                        if (exist && dir) { father.put("PWD", target); return new Double(0); } 
                        else { return new Double(exist ? 20 : 127); }
                    }
                    else if (midlet.getInputStream(target.substring(target.length() - 1), father) != null) { return new Double(20); }

                    return new Double(127);
                }
            }
            else if (MOD == SU) {
                if (args.isEmpty()) { return gotbad(1, "su", "username and password expected"); } 
                else {
                    String user = toLuaString(args.elementAt(0)), query = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;
                    if (user.equals(midlet.username)) { id = 1000; father.put("USER", midlet.username); return new Double(0); }
                    else if (midlet.userID.containsKey(user)) { id = midlet.getUserID(user); father.put("USER", user); return new Double(0); }
                    else if (query == null) { return gotbad(2, "su", "string expected, got nil"); }
                    else if (user.equals("root") && midlet.passwd(query)) { id = 0; father.put("USER", "root"); return new Double(0); }
                    else { return new Double(13); }
                }
            }
            else if (MOD == REMOVE) { return args.isEmpty() ? (Double) gotbad(1, "remove", "string expected, got no value") : new Double(midlet.deleteFile(toLuaString(args.elementAt(0)), id, father)); }
            else if (MOD == SCOPE) {
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
                    }
                    else { return gotbad(1, "scope", "table expected, got " + type(args.elementAt(0))); }
                }
            }
            else if (MOD == JOIN) { return args.isEmpty() ? (String) gotbad(1, "join", "string expected, got no value") : (String) midlet.joinpath(toLuaString(args.elementAt(0)), father); }
            else if (MOD == MKDIR) {
                if (args.isEmpty()) { }
                else {
                    String dir = toLuaString(args.elementAt(0));

                    if (!dir.equals("/mnt/") && dir.startsWith("/mnt/")) {
                        FileConnection fc = null;
                        try {
                            fc = (FileConnection) Connector.open("file:///" + dir.substring(5), Connector.READ_WRITE);
                            
                            if (fc.exists()) { return new Double(128); } else { fc.mkdir(); return new Double(0); }
                        }
                        catch (Exception e) { return e instanceof SecurityException ? 13 : 1; }
                        finally { if (fc != null) { try { fc.close(); } catch (Exception e) { } } }
                    } else { return new Double(5); }
                    
                }
                    
            }
            // Package: io
            else if (MOD == READ) {
                if (args.isEmpty()) { return stdout instanceof StringItem ? ((StringItem) stdout).getText() : stdout instanceof StringBuffer ? ((StringBuffer) stdout).toString() : stdout instanceof String ? midlet.getcontent((String) stdout, father) : ""; }
                else {
                    Object arg = args.elementAt(0);

                    if (arg instanceof InputStream) {
                        InputStream IN = (InputStream) arg;

                        int chunkSize = args.size() > 1 && args.elementAt(1) instanceof Double ? ((Double) args.elementAt(1)).intValue() : 1024;

                        byte[] buffer = new byte[chunkSize];
                        int bytesRead = IN.read(buffer, 0, chunkSize);
                        if (bytesRead == -1) { return null; }

                        return new String(buffer, 0, bytesRead, "UTF-8");
                    }
                    else if (arg instanceof StringBuffer) { ((StringBuffer) arg).toString(); }
                    else if (arg instanceof OutputStream) { return gotbad(1, "read", "input stream expected, got output"); } 
                    else { return midlet.getcontent(toLuaString(arg), father); } 
                }
            }
            else if (MOD == WRITE) {
                if (args.isEmpty()) { }
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
                        return new Double(midlet.write(filename, mode ? midlet.getcontent(filename, father) + content : content, id, father));
                    }
                }
            }
            else if (MOD == CLOSE) {
                if (args.isEmpty()) { }
                else {
                    for (int i = 0; i < args.size(); i++) {
                        Object arg = args.elementAt(i);

                        if (arg instanceof ServerSocketConnection) { ((ServerSocketConnection) arg).close(); }
                        else if (arg instanceof StreamConnection) { ((StreamConnection) arg).close(); }
                        else if (arg instanceof InputStream) { ((InputStream) arg).close(); }
                        else if (arg instanceof OutputStream) { ((OutputStream) arg).close(); }
                        else if (arg instanceof StringBuffer || arg instanceof StringItem) { }
                        else if (arg instanceof Player) { Player player = (Player) arg; player.stop(); player.deallocate(); player.close(); }
                        else { return gotbad(i + 1, "close", "stream expected, got " + type(arg)); }

                        proc.net.remove(arg);
                    }
                } 
            }
            else if (MOD == OPEN) { if (args.isEmpty()) { return new ByteArrayOutputStream(); } else { try { return midlet.getInputStream(toLuaString(args.elementAt(0)), father); } catch (Exception e) { return null; } } } 
            else if (MOD == POPEN) { 
                if (args.isEmpty()) { } 
                else {
                    String program = toLuaString(args.elementAt(0)), pid = midlet.genpid();
                    Object arguments = args.size() > 1 ? toLuaString(args.elementAt(1)) : "";
                    int owner = (args.size() < 3) ? new Integer(id) : ((args.elementAt(2) instanceof Boolean) ? new Integer((Boolean) args.elementAt(2) ? id : 1000) : (Integer) gotbad(3, "popen", "boolean expected, got " + type(args.elementAt(2))));
                    Object out = (args.size() < 4) ? new StringBuffer() : args.elementAt(3);
                    Hashtable scope = (args.size() < 5) ? father : (args.elementAt(4) instanceof Hashtable ? (Hashtable) args.elementAt(4) : (Hashtable) gotbad(5, "popen", "table expected, got " + type(args.elementAt(4))));
                    InputStream is = (args.size() < 6) ? midlet.getInputStream(program, father) : (InputStream) args.elementAt(5);

                    Vector result = new Vector();
                    if (is == null) { return new Double(127); }
                    else {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[1024];
                        int length;
                        
                        while ((length = is.read(buffer)) != -1) { baos.write(buffer, 0, length); }
                        
                        byte[] data = baos.toByteArray(); baos.close();

                        Hashtable arg = null;
                        if (arguments instanceof Hashtable) { arg = (Hashtable) arguments; arg.put(new Double(0), program); }
                        else if (arguments != null) {
                            arg = new Hashtable();
                            arg.put(new Double(0), program);
                            String args = toLuaString(arguments);
                            String[] list = midlet.splitArgs(args);
                            for (int i = 0; i < list.length; i++) { arg.put(new Double(i + 1), list[i]); }
                        } 
                        
                        if (arg == null) { arg = new Hashtable(); }
                        

                        if (midlet.isPureText(data)) {
                            String code = new String(data, "UTF-8");
                            Process process = new Process(midlet, ("lua " + program).trim(), midlet.joinpath(program, father), midlet.getUser(owner), owner, pid, out, scope);
                            midlet.sys.put(pid, process);

                            result.addElement(process.lua.run(program, code, arg));
                        }
                        else {
                            InputStream elfStream = new ByteArrayInputStream(data);
                            Process process = new Process(midlet, "elf", midlet.joinpath(program, father), midlet.getUser(owner), owner, pid, stdout, arg, scope);
                            midlet.sys.put(pid, process);
                            
                            if (process.elf.load(elfStream)) { result.addElement(process.elf.run()); } else { result.addElement(1); }
                        }
                        
                        result.addElement(out instanceof StringBuffer ? out.toString() : out);
                        return result;
                    }
                }
            } 
            else if (MOD == DIRS) {
                String pwd = args.isEmpty() ? (String) father.get("PWD") : toLuaString(args.elementAt(0));
                int index = 1;
                
                Hashtable list = new Hashtable();
                if (pwd.startsWith("/")) { }
                else { pwd = ((String) father.get("PWD")) + pwd; }
                if (pwd.endsWith("/")) { } 
                else { pwd = pwd + "/"; }

                if ((pwd = midlet.solvepath(pwd, father)).equals("/tmp/")) { for (Enumeration files = midlet.tmp.keys(); files.hasMoreElements();) { list.put(new Double(index), (String) files.nextElement()); index++; } }
                else if (pwd.equals("/mnt/")) { for (Enumeration roots = FileSystemRegistry.listRoots(); roots.hasMoreElements();) { list.put(new Double(index), (String) roots.nextElement()); index++; } } 
                else if (pwd.startsWith("/mnt/")) { 
                    FileConnection CONN = (FileConnection) Connector.open("file:///" + pwd.substring(5), Connector.READ); 
                    for (Enumeration files = CONN.list(); files.hasMoreElements();) { list.put(new Double(index), (String) files.nextElement()); index++; } 
                    CONN.close(); 
                } 
                else if (pwd.equals("/bin/") || pwd.equals("/etc/") || pwd.equals("/lib/")) {
                    String content = midlet.loadRMS("OpenRMS", pwd.equals("/bin/") ? 3 : pwd.equals("/etc/") ? 5 : 4);
                    int i = 0;

                    while (true) {
                        int start = content.indexOf("[\1BEGIN:", i);
                        if (start == -1) { break; }

                        int end = content.indexOf("\1]", start);
                        if (end == -1) { break; }

                        list.put(new Double(index), content.substring(start + "[\1BEGIN:".length(), end)); index++;

                        i = content.indexOf("[\1END\1]", end);
                        if (i == -1) { break; }

                        i += "[\1END\1]".length();
                    }
                }
                else if (pwd.equals("/home/")) { 
                    String[] files = RecordStore.listRecordStores(); 
                    if (files != null) { 
                        for (int i = 0; i < files.length; i++) { list.put(new Double(index), files[i]); index++; } 
                    } 
                }
                
                if (midlet.fs.containsKey(pwd)) {
                    Vector struct = (Vector) midlet.fs.get(pwd);

                    for (int i = 0; i < struct.size(); i++) { list.put(new Double(index), struct.elementAt(i)); index++; }
                }
                
                return list;
            }
            else if (MOD == SETOUT) { if (args.isEmpty()) { } else { stdout = args.elementAt(0); } }
            else if (MOD == MOUNT) {
                if (args.isEmpty()) { }
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
                }
            }
            else if (MOD == GEN) { return new StringBuffer(); }
            else if (MOD == COPY) {
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
            }
            // Package: table
            else if (MOD == TB_INSERT) {
                if (args.size() < 2) { return gotbad(1, "insert", "wrong number of arguments"); }
                else {
                    Object tObj = unwrap(args.elementAt(0));
                    if (tObj instanceof Hashtable) {
                        Hashtable table = (Hashtable) tObj;
                        if (isListTable(table)) {
                            int pos = table.size() + 1; // default: append
                            Object value = unwrap(args.elementAt(1));
                            if (args.size() >= 3) {
                                Object posObj = unwrap(args.elementAt(2));
                                if (!(posObj instanceof Double)) { return gotbad(3, "insert", "number expected, got " + type(posObj)); }
                                pos = ((Double) posObj).intValue();
                                if (pos < 0 || pos > table.size() + 1) { return gotbad(3, "insert", "position out of bounds"); }
                                // value continua sendo args[1]
                            }
                            // Desloca elementos para abrir espaço (shift right)
                            for (int i = table.size(); i >= pos; i--) {
                                Object val = table.get(new Double(i));
                                if (val != null) {
                                    table.put(new Double(i + 1), val);
                                } else {
                                    table.remove(new Double(i));
                                }
                            }
                            table.put(new Double(pos), value == null ? LUA_NIL : value);
                            return null;
                        } else { return gotbad(1, "insert", "table must be array-like"); }
                    } else { return gotbad(1, "insert", "table expected, got " + type(tObj)); }
                }
            }
            else if (MOD == TB_CONCAT) {
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
            }
            else if (MOD == TB_REMOVE) {
                if (args.isEmpty()) { return gotbad(1, "remove", "table expected, got no value"); }
                else {
                    Object tObj = unwrap(args.elementAt(0));
                    if (tObj instanceof Hashtable) {
                        Hashtable table = (Hashtable) tObj;
                        if (isListTable(table)) {
                            int pos = table.size(); // default: remove o último
                            if (args.size() >= 2) {
                                Object posObj = unwrap(args.elementAt(1));
                                if (!(posObj instanceof Double)) { return gotbad(2, "remove", "number expected, got " + type(posObj)); }
                                pos = ((Double) posObj).intValue();
                                if (pos < 1 || pos > table.size()) { return gotbad(2, "remove", "position out of bounds"); }
                            }
                            Object removed = table.get(new Double(pos));
                            if (removed != null) {
                                table.remove(new Double(pos));
                                // Desloca elementos para preencher o buraco (shift left)
                                for (int i = pos; i < table.size(); i++) {
                                    Object val = table.get(new Double(i + 1));
                                    if (val != null) {
                                        table.put(new Double(i), val);
                                    } else {
                                        table.remove(new Double(i));
                                    }
                                }
                                // Remove o último índice se vazio
                                if (table.containsKey(new Double(table.size()))) {
                                    table.remove(new Double(table.size()));
                                }
                            }
                            return removed == null ? LUA_NIL : removed;
                        } else { return gotbad(1, "remove", "table must be array-like"); }
                    } else { return gotbad(1, "remove", "table expected, got " + type(tObj)); }
                }
            }
            else if (MOD == TB_SORT) {
                if (args.isEmpty()) { return gotbad(1, "sort", "table expected, got no value"); }
                else {
                    Object tObj = unwrap(args.elementAt(0));
                    if (tObj instanceof Hashtable) {
                        Hashtable table = (Hashtable) tObj;
                        if (isListTable(table)) {
                            Vector list = toVector(table);
                            // Bubble sort simples (sem comparador customizado)
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
                            // Reconstrói a tabela ordenada
                            table.clear();
                            for (int i = 0; i < list.size(); i++) {
                                table.put(new Double(i + 1), list.elementAt(i));
                            }
                            return null;
                        } else { return gotbad(1, "sort", "table must be array-like"); }
                    } else { return gotbad(1, "sort", "table expected, got " + type(tObj)); }
                }
            }
            else if (MOD == TB_MOVE) {
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
                            if (from < 1 || to < 1 || len < 0 || a < 1 || b > table.size()) {
                                return gotbad(1, "move", "bounds out of range");
                            }
                            Vector list = toVector(table);
                            // Extrai o slice a mover
                            Vector slice = new Vector();
                            for (int i = 0; i < len; i++) {
                                int idx = from + i - 1;
                                if (idx >= 0 && idx < list.size()) {
                                    slice.addElement(list.elementAt(idx));
                                }
                            }
                            // Remove o bloco original (shift left)
                            for (int i = from + len - 1; i >= from; i--) {
                                if (i - 1 >= 0 && i - 1 < list.size()) {
                                    list.removeElementAt(i - 1);
                                }
                            }
                            // Insere o slice na nova posição
                            for (int i = 0; i < slice.size(); i++) {
                                list.insertElementAt(slice.elementAt(i), to + i - 1);
                            }
                            // Reconstrói a tabela
                            table.clear();
                            for (int i = 0; i < list.size(); i++) {
                                table.put(new Double(i + 1), list.elementAt(i));
                            }
                            return table;
                        } else { return gotbad(1, "move", "table must be array-like"); }
                    } else { return gotbad(1, "move", "table expected, got " + type(tObj)); }
                }
            }
            else if (MOD == TB_UNPACK) {
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
            }
            else if (MOD == TB_DECODE) {
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
            }
            else if (MOD == TB_PACK) {
                Hashtable packed = new Hashtable();
                for (int i = 0; i < args.size(); i++) {
                    Object val = args.elementAt(i);
                    packed.put(new Double(i + 1), val == null ? LUA_NIL : val);
                }
                packed.put("n", new Double(args.size()));
                return packed;
            }
            // Package: base64
            else if (MOD == BASE64_ENCODE) {
                if (args.isEmpty()) { return gotbad(1, "encode", "string or table expected, got no value"); }
                
                Object arg = args.elementAt(0);
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
                } /*else if (arg instanceof InputStream) {
                    
                } e*/else {
                    return gotbad(1, "encode", "string or table expected, got " + type(arg));
                }
                
                return midlet.encodeBase64(data);
            }
            else if (MOD == BASE64_DECODE) {
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
            }
            // Package: socket.http
            else if (MOD == HTTP_GET || MOD == HTTP_POST) { return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : http(MOD == HTTP_GET ? "GET" : "POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null, false)); }
            else if (MOD == HTTP_RGET || MOD == HTTP_RPOST) { return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : http(MOD == HTTP_GET ? "GET" : "POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null, true)); }
            // Package: socket
            else if (MOD == CONNECT) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "connect", "string expected, got no value"); }
                else {
                    Vector result = new Vector();

                    SocketConnection conn = (SocketConnection) Connector.open(toLuaString(args.elementAt(0)));
                        
                    result.addElement(conn); result.addElement(conn.openInputStream()); result.addElement(conn.openOutputStream()); result.addElement(args.elementAt(0)); result.addElement(new Double(id));
                    proc.net.put(toLuaString(args.elementAt(0)), result);

                    return result;
                } 
            }
            else if (MOD == PEER || MOD == DEVICE) {
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
            }
            else if (MOD == SERVER) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Double)) { return gotbad(1, "server" , "number expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); }
                else {
                    ServerSocketConnection server = (ServerSocketConnection) Connector.open("socket://:" + toLuaString(args.elementAt(0)));
                    midlet.servers.put(toLuaString(args.elementAt(0)), server);
                    proc.net.put(toLuaString(args.elementAt(0)), server);
                    return server;
                }
            }
            else if (MOD == ACCEPT) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof ServerSocketConnection)) { return gotbad(1, "server" , "server expected, got " + (args.isEmpty() ? " no value" : type(args.elementAt(0)))); }
                else {
                    Vector result = new Vector();

                    SocketConnection conn = (SocketConnection) ((ServerSocketConnection) args.elementAt(0)).acceptAndOpen();
                        
                    result.addElement(conn); result.addElement(conn.openInputStream()); result.addElement(conn.openOutputStream());
                    proc.net.put("socket://:" + ((ServerSocketConnection) args.elementAt(0)).getLocalPort(), result);

                    return result;
                }
            }
            // Package: push
            else if (MOD == PUSH_REGISTER) {
                if (args.size() < 3) { return gotbad(1, "register", "insufficient arguments"); }
                
                String connection = toLuaString(args.elementAt(0)), filter = toLuaString(args.elementAt(1));
                String midletClass = toLuaString(args.elementAt(2)), sender = args.size() > 3 ? toLuaString(args.elementAt(3)) : null;
                
                try {
                    PushRegistry.registerConnection(connection, midletClass, filter);
                    
                    return Boolean.TRUE;
                } catch (ClassNotFoundException e) {
                    return gotbad(3, "register", "MIDlet class not found: " + midletClass);
                } catch (Exception e) {
                    return gotbad(1, "register", midlet.getCatch(e));
                }
            }
            else if (MOD == PUSH_UNREGISTER) {
                if (args.isEmpty()) { return gotbad(1, "unregister", "connection string expected"); }
                
                String connection = toLuaString(args.elementAt(0));
                
                try {
                    boolean result = PushRegistry.unregisterConnection(connection);
                    return new Boolean(result);
                } catch (Exception e) {
                    return gotbad(1, "unregister", midlet.getCatch(e));
                }
            }
            else if (MOD == PUSH_LIST) {
                if (args.isEmpty()) { return gotbad(1, "list", "connection string expected"); }
                
                String connection = toLuaString(args.elementAt(0));
                
                try {
                    String[] connections = PushRegistry.listConnections(false);
                    Hashtable result = new Hashtable(); int j = 1;
                    
                    if (connections != null) {
                        for (int i = 0; i < connections.length; i++) {
                            if (connections[i].startsWith(connection) || connection.equals("*")) {
                                result.put(new Double(j), connections[i]); j++;
                            }
                        }
                    }
                    
                    return result;
                } catch (Exception e) {
                    return gotbad(1, "list", midlet.getCatch(e));
                }
            }
            else if (MOD == PUSH_PENDING) {
                try {
                    boolean hasPending = PushRegistry.listConnections(true).length > 0;
                    return new Boolean(hasPending);
                } catch (Exception e) { return gotbad(1, "hasPending", midlet.getCatch(e)); }
            }
            else if (MOD == PUSH_SET_ALARM) {
                if (args.size() < 2) {
                    return gotbad(1, "setAlarm", "insufficient arguments");
                }
                
                String midletClass = toLuaString(args.elementAt(0));
                long time = ((Double) args.elementAt(1)).longValue();
                
                try {
                    long alarmTime = PushRegistry.registerAlarm(midletClass, time);
                    return new Double(alarmTime);
                } catch (ClassNotFoundException e) {
                    return gotbad(1, "setAlarm", "MIDlet class not found: " + e.getMessage());
                } catch (ConnectionNotFoundException e) {
                    return gotbad(1, "setAlarm", "Connection not found: " + e.getMessage());
                }
            }
            // Package: graphics 
            else if (MOD == DISPLAY) {
                if (args.isEmpty()) { }
                else {
                    Object screen = args.elementAt(0);

                    if (screen instanceof Alert && args.size() > 1) { kill = false; midlet.display.setCurrent((Alert) screen, (Displayable) args.elementAt(1)); }
                    else if (screen instanceof Displayable) { kill = false; midlet.display.setCurrent((Displayable) screen); }
                    else { return gotbad(1, "display", "screen expected, got " + type(screen)); }
                }
            }
            else if (MOD == NEW) {
                if (args.size() < 2) { return gotbad(1, "graphics.new", "wrong number of arguments"); }
                
                String type = toLuaString(args.elementAt(0)), title = args.elementAt(1) == null ? null : toLuaString(args.elementAt(1));
                Object content = args.size() > 2 ? args.elementAt(2) : null;

                if (type.equals("alert")) {
                    Alert alert = new Alert(title, content != null ? toLuaString(content) : "", null, AlertType.INFO);
                    alert.setTimeout(Alert.FOREVER);
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
                else { return gotbad(1, "new", "invalid type: " + type); } 
            }
            else if (MOD == RENDER) { return args.isEmpty() || args.elementAt(0) == null ? gotbad(1, "render", "string expected, got" + type(args.elementAt(0))) : midlet.readImg(toLuaString(args.elementAt(0)), father); }
            else if (MOD == APPEND) {
                if (args.size() < 2) { return gotbad(1, "append", "wrong number of arguments"); }
                
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
                } else { return gotbad(1, "append", "Form or List expected"); }
            }
            else if (MOD == ADDCMD) {
                if (args.size() < 2) { return gotbad(1, "addCommand", "wrong number of arguments"); }
                
                Object target = args.elementAt(0), cmdObj = args.elementAt(1);
                
                if (!(target instanceof Displayable)) { return gotbad(1, "addCommand", "Displayable expected"); }
                if (!(cmdObj instanceof Command)) { return gotbad(1, "addCommand", "Command expected"); }
                
                ((Displayable) target).addCommand((Command) cmdObj);
            }
            else if (MOD == HANDLER) {
                if (args.size() < 2) { return gotbad(1, "handler", "wrong number of arguments"); }

                Object screen = args.elementAt(0), table = args.elementAt(1);

                if (!(screen instanceof Displayable)) { return gotbad(1, "handler", "Displayable expected"); }
                if (!(table instanceof Hashtable)) { return gotbad(1, "handler", "Hashtable expected"); }

                ((Displayable) screen).setCommandListener(new LuaFunction((Hashtable) table));
            }
            else if (MOD == TITLE) { ((Displayable) args.elementAt(0)).setTitle(args.isEmpty() ? null : toLuaString(args.elementAt(1))); }
            else if (MOD == TICKER) { ((Displayable) args.elementAt(0)).setTicker(args.isEmpty() ? null : new Ticker(toLuaString(args.elementAt(1)))); }
            else if (MOD == GETCURRENT) { return midlet.display.getCurrent(); }
            else if (MOD == VIBRATE) { midlet.display.vibrate(args.isEmpty() ? new Integer(500) : args.elementAt(0) instanceof Double ? new Integer(((Double) args.elementAt(0)).intValue()) : (Integer) gotbad(1, "vibrate", "number expected")); }
            else if (MOD == SETLABEL) { if (args.isEmpty()) { } else { Item i = args.elementAt(0) instanceof Item ? (Item) args.elementAt(0) : (Item) gotbad(1, "SetLabel", "Item expected"); i.setLabel(args.size() > 1 ? toLuaString(args.elementAt(1)) : null); } } 
            else if (MOD == GETLABEL) { if (args.isEmpty()) { } else { Item i = args.elementAt(0) instanceof Item ? (Item) args.elementAt(0) : (Item) gotbad(1, "GetLabel", "Item expected"); return i.getLabel(); } }
            else if (MOD == SETTEXT) { 
                if (args.isEmpty()) { } 
                else { 
                    Object i = args.elementAt(0); 
                    if (i instanceof StringItem) { ((StringItem) i).setText(args.size() > 1 ? toLuaString(args.elementAt(1)) : ""); } 
                    else if (i instanceof TextField) { ((TextField) i).setString(args.size() > 1 ? toLuaString(args.elementAt(1)) : ""); } 
                    else if (i instanceof TextBox) { ((TextBox) i).setString(args.size() > 1 ? toLuaString(args.elementAt(1)) : ""); }
                    else { return gotbad(1, "SetText", "Item expected"); }
                } 
            }
            else if (MOD == GETTEXT) { 
                if (args.isEmpty()) { } 
                else { 
                    Object i = args.elementAt(0); 
                    return i instanceof StringItem ? ((StringItem) i).getText() : i instanceof TextField ? ((TextField) i).getString() : i instanceof TextBox ? ((TextBox) i).getString() : gotbad(1, "GetText", "Item expected"); 
                } 
            }
            else if (MOD == CLEAR_SCREEN) {
                if (args.isEmpty()) { return gotbad(1, "clear", "screen expected, got no value"); }
                else {
                    Object screen = args.elementAt(0);

                    if (screen instanceof Form) { ((Form) args.elementAt(0)).deleteAll(); }
                    else if (screen instanceof List) { ((List) args.elementAt(0)).deleteAll(); }
                    else { return gotbad(1, "clear", "screen expected, got" + type(args.elementAt(0))); }
                }
            }

            // Package: string
            else if (MOD == LOWER || MOD == UPPER) { if (args.isEmpty()) { return gotbad(1, MOD == LOWER ? "lower" : "upper", "string expected, got no value"); } else { String text = toLuaString(args.elementAt(0)); return MOD == LOWER ? text.toLowerCase() : text.toUpperCase(); } }
            else if (MOD == FIND || MOD == MATCH || MOD == LEN) {
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
                        else if (MOD == FIND) { return new Double(pos + 1); } 
                        else if (MOD == MATCH) { return text.substring(pos, pos + pattern.length()); }
                    }
                }
            }
            else if (MOD == REVERSE) { if (args.isEmpty()) { return gotbad(1, "reverse", "string expected, got no value"); } else { StringBuffer sb = new StringBuffer(toLuaString(args.elementAt(0))); return sb.reverse().toString(); } }
            else if (MOD == SUB) {
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
            }
            else if (MOD == HASH) { return args.isEmpty() || args.elementAt(0) == null ? null : new Double(args.elementAt(0).hashCode()); }
            else if (MOD == BYTE) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "byte", "string expected, got no value"); }
                else {
                    String s = toLuaString(args.elementAt(0));
                    int len = s.length(), start = 1, end = 1;
                    if (args.size() >= 2) { start = getNumber(toLuaString(args.elementAt(1)), 1); }
                    if (args.size() >= 3) { end = getNumber(toLuaString(args.elementAt(2)), start); }
                    
                    if (start < 0) { start = len + start + 1; }
                    if (end < 0) { end = len + end + 1; }
                    if (start < 1) { start = 1; }
                    if (end > len) { end = len; } 
                    if (start > end || start > len) { return null; }
                    
                    if (end - start + 1 == 1) { return new Double((double) s.charAt(start - 1)); } 
                    else {
                        Hashtable result = new Hashtable();
                        for (int i = start; i <= end; i++) { result.put(new Double(i), new Double((double) s.charAt(i - 1))); }

                        return result;
                    }
                }
            }
            else if (MOD == CHAR) {
                if (args.isEmpty()) { return ""; } 
                else {
                    Object firstArg = args.elementAt(0);
                    
                    if (firstArg instanceof Hashtable) {
                        Hashtable table = (Hashtable) firstArg;
                        StringBuffer sb = new StringBuffer();
                        for (int i = 0; i <= table.size(); i++) {
                            Object arg = table.get(new Double(i + 1));
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
                            Object arg = args.elementAt(i);
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
            }
            else if (MOD == TRIM) { return args.isEmpty() ? null : toLuaString(args.elementAt(0)).trim(); }
            else if (MOD == UUID) { String chars = "0123456789abcdef"; StringBuffer uuid = new StringBuffer(); for (int i = 0; i < 36; i++) { if (i == 8 || i == 13 || i == 18 || i == 23) { uuid.append('-'); } else if (i == 14) { uuid.append('4'); } else if (i == 19) { uuid.append(chars.charAt(8 + midlet.random.nextInt(4))); } else { uuid.append(chars.charAt(midlet.random.nextInt(16))); } } return uuid.toString(); }
            else if (MOD == SPLIT) {
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
            }
            else if (MOD == GETCMD) { return args.isEmpty() ? null : midlet.getCommand(toLuaString(args.elementAt(0))); } 
            else if (MOD == GETARGS) { return args.isEmpty() ? null : midlet.getArgument(toLuaString(args.elementAt(0))); }
            else if (MOD == GETPATTERN) { return args.isEmpty() ? null : midlet.getpattern(toLuaString(args.elementAt(0))); }
            else if (MOD == ENV) { return args.isEmpty() ? null : midlet.env(toLuaString(args.elementAt(0))); }
            else if (MOD == STARTSWITH) { return args.size() < 2 ? (Boolean) gotbad(1, "startswith", "string expected") : toLuaString(args.elementAt(0)).startsWith(toLuaString(args.elementAt(1))); }
            else if (MOD == ENDSWITH) { return args.size() < 2 ? (Boolean) gotbad(1, "endswith", "string expected") : toLuaString(args.elementAt(0)).endsWith(toLuaString(args.elementAt(1))); }
            // Package: audio
            else if (MOD == AUDIO_LOAD) {
                if (args.isEmpty()) { return gotbad(1, "load", "string expected, got no value"); }
                else {
                    InputStream is = midlet.getInputStream(toLuaString(args.elementAt(0)), father);
                    if (is != null) {
                        Player player = Manager.createPlayer(is, args.size() > 1 ? toLuaString(args.elementAt(1)) : "audio/mpeg"); 
                        player.prefetch();
                        
                        return player;
                    }
                }
            }
            else if (MOD == AUDIO_PLAY) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "play", "audio object expected"); }
                else { ((Player) args.elementAt(0)).start(); return new Double(0); }
            }
            else if (MOD == AUDIO_PAUSE) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "pause", "audio object expected"); }
                else { ((Player) args.elementAt(0)).stop(); return new Double(0); }
            }
            else if (MOD == AUDIO_VOLUME) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "pause", "audio object expected"); }
                else {
                    Player player = (Player) args.elementAt(0);
                    VolumeControl vc = (VolumeControl) player.getControl("VolumeControl");

                    if (args.size() > 1 || args.elementAt(1) instanceof Double) {
                        int value = ((Double) args.elementAt(1)).intValue();
                        if (vc != null) { vc.setLevel(value); return new Double(0); }
                    }
                    else { return new Double(vc.getLevel()); }
                }
            }
            else if (MOD == AUDIO_DURATION) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "pause", "audio object expected"); }
                else {
                    Player player = (Player) args.elementAt(0);
                    long duration = player.getDuration();
                    return new Double(duration == Player.TIME_UNKNOWN ? -1 : duration / 1000.0);
                }
            }
            else if (MOD == AUDIO_TIME) { 
                if (args.isEmpty() || !(args.elementAt(0) instanceof Player)) { return gotbad(1, "time", "audio object expected"); }
                else {
                    Player player = (Player) args.elementAt(0);

                    if (args.size() > 1 || args.elementAt(1) instanceof Double) {
                        long time = (long) (((Double) args.elementAt(1)).doubleValue() * 1000);
                        player.setMediaTime(time); return new Double(0);
                    } else {
                        long time = player.getMediaTime();
                        return new Double(time == Player.TIME_UNKNOWN ? -1 : time / 1000.0);
                    }
                }
            }
            // Package: java
            else if (MOD == CLASS) { if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "class", "string expected, got no value"); } else { return new Boolean(midlet.javaClass(toLuaString(args.elementAt(0))) == 0); } }
            else if (MOD == NAME) { return midlet.getName(); } 
            else if (MOD == DELETE) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Hashtable)) { return gotbad(1, "delete", "table expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); }
                else if (args.size() < 2 || args.elementAt(1) == null) { return gotbad(2, "delete", "value expected, got " + (args.size() < 2 ? "no value" : "nil")); }
                else { ((Hashtable) args.elementAt(0)).remove(args.elementAt(1)); }
            }
            else if (MOD == RUN) { if (args.isEmpty()) { } else if (args.elementAt(0) instanceof LuaFunction) { kill = false; new Thread((Runnable) new LuaFunction((LuaFunction) args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "Background").start(); } else { return gotbad(1, "run", "function expected, got" + type(args.elementAt(0))); } }
            else if (MOD == PREQ) { if (args.isEmpty()) { } else { return new Boolean(midlet.platformRequest(toLuaString(args.elementAt(0)))); } }
            else if (MOD == THREAD) { return midlet.getThreadName(Thread.currentThread()); }
            else if (MOD == UPTIME) { return new Double(System.currentTimeMillis() - midlet.uptime); }
            else if (MOD == SLEEP) {
                if (args.isEmpty()) { }
                else {
                    Object arg = args.elementAt(0);
                    if (arg instanceof Double) {
                        Thread.sleep(((Double) arg).longValue());
                    } else {
                        return gotbad(1, "sleep", "number expected, got " + type(arg));
                    }
                }
            }

            else if (MOD == KERNEL) {
                Object payload = args.elementAt(0), arg = args.elementAt(1), scope = args.elementAt(2), pid = args.elementAt(3);
                int uid = ((Double) args.elementAt(4)).intValue();

                if (payload == null || payload.equals("")) { return null; }
                if (payload instanceof String) {
                    if (payload.equals("sendsig")) {
                        if (arg == null || !(arg instanceof Hashtable)) { return new Double(2); }
                        else {
                            Hashtable info = (Hashtable) arg;
                            String pid = (String) info.get("pid"), signal = (String) info.get("signal");

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
                        if (arg == null || arg.equals("")) { return new Double(2); }
                        else if (arg instanceof LuaFunction) { midlet.shell = arg; }
                        else { return new Double(2); }
                    }
                    else if (payload.equals("cache")) { if (arg == null || arg.equals("")) { return new Boolean(midlet.useCache); } else if (arg == Boolean.TRUE || toLuaString(arg).equals("true")) { midlet.useCache = true; } else if (arg == Boolean.FALSE || toLuaString(arg).equals("false")) { midlet.useCache = false; midlet.cache.clear(); midlet.cacheLua.clear(); } else { return new Double(2); } }
                    else if (payload.equals("debug")) { if (arg == null || arg.equals("")) { return new Boolean(midlet.debug); } else if (arg == Boolean.TRUE || toLuaString(arg).equals("true")) { midlet.debug = true; } else if (arg == Boolean.FALSE || toLuaString(arg).equals("false")) { midlet.debug = false; } else { return new Double(2); } } 
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
                            Process process = new Process(midlet, program, "/bin/init --serve=" + program, midlet.getUser(uid), uid, midlet.genpid(), stdout, father);
                            process.lua.kill = false;

                            Hashtable arg = new Hashtable(); arg.put(new Double(0), program); arg.put(new Double(1), "--deamon");
                            Hashtable res = process.lua.run(program, code, arg);

                            Object handler = res.get("object");
                            if (handler instanceof Vector) {
                                Vector resx = (Vector) handler;
                                handler = resx.elementAt(0);
                            }

                            if (handler instanceof Lua.LuaFunction) { process.handler = handler; }
                        }
                    }

                    else if (payload.equals("rms")) {
                        if (uid == 0) {
                            if (arg == null || arg.equals("")) { return new Double(2); }
                            else if (arg.equals("/bin/")) { midlet.writeRMS("OpenRMS", new byte[0], 3); }
                            else if (arg.equals("/etc/")) { midlet.writeRMS("OpenRMS", new byte[0], 5); }
                            else if (arg.equals("/lib/")) { midlet.writeRMS("OpenRMS", new byte[0], 4); }
                        } else { return new Double(13); }
                    }
                    else if (payload.equals("useradd")) {
                        if (arg == null || arg.equals("") || arg.equals("root")) { return new Double(2); }
                        else if (midlet.userID.containsKey(arg)) { return new Double(128); }
                        else { midlet.userID.put(arg, new Integer(midlet.lastID + 1)); midlet.lastID++; }
                    }
                    else if (payload.equals("userdel")) {
                        if (arg == null || arg.equals("") || arg.equals("root") || arg.equals(midlet.username)) { return new Double(13); }
                        else if (midlet.userID.containsKey(arg)) { if (uid == 0) { midlet.userID.remove(arg); return new Double(0); } else { return new Double(13); } }
                        else { return new Double(127); }
                    }
                    else if (payload.equals("user")) {
                        if (arg == null || arg.equals("")) { return new Double(2); }
                        else if (midlet.userID.containsKey(arg)) { return midlet.userID.get(arg); }
                        else { return new Double(127); }
                    }
                }
            }

            return null;
        }
        // |
        private Object exec(String code, Hashtable scope) throws Exception { int savedIndex = tokenIndex; Vector savedTokens = tokens; Object ret = null; try { tokens = tokenize(code); tokenIndex = 0; Hashtable modScope = scope == null ? new Hashtable() : scope; for (Enumeration e = globals.keys(); e.hasMoreElements();) { String k = (String) e.nextElement(); modScope.put(k, unwrap(globals.get(k))); } while (peek().type != EOF) { Object res = statement(modScope); if (doreturn) { ret = res; doreturn = false; break; } } } finally { tokenIndex = savedIndex; tokens = savedTokens; } return ret; }
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

            int size = table.size();
            for (int i = 1; i <= size; i++) {
                if (!table.containsKey(new Double(i))) { return false; }
            }
            for (Enumeration e = table.keys(); e.hasMoreElements();) {
                Object key = e.nextElement();
                if (!(key instanceof Double)) { return false; }
                double d = ((Double) key).doubleValue();
                if (d != Math.floor(d) || d < 1 || d > size) { return false; }
            }
            return true;
        }
        public static Vector toVector(Hashtable table) throws Exception { Vector vec = new Vector(); if (table == null) { return vec; } for (int i = 1; i <= table.size(); i++) { vec.addElement(table.get(new Double(i))); } return vec; }

        private int getFieldNumber(Hashtable table, String key, int fallback) { Object val = table.get(key); if (val instanceof Double) { return ((Double) val).intValue(); } try { return Integer.parseInt(toLuaString(val)); } catch (Exception e) { return fallback; } }
        private boolean getFieldBoolean(Hashtable table, String key, boolean fallback) { Object val = table.get(key); if (val instanceof Boolean) { return ((Boolean) val).booleanValue(); } return toLuaString(val).equalsIgnoreCase("true") ? true : fallback; } 
        private int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = midlet.replace(mode, "password", "").trim(); } int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY; return password ? (base | TextField.PASSWORD) : base; } 
        public int getNumber(String s, int fallback) { try { return Integer.valueOf(s); } catch (Exception e) { return fallback; } } 
    
        private String getFieldValue(Hashtable table, String key, String fallback) { Object val = table.get(key); return val != null ? toLuaString(val) : fallback; }
        private Font genFont(String params) { if (params == null || params.length() == 0 || params.equals("default")) { return Font.getDefaultFont(); } int face = Font.FACE_SYSTEM, style = Font.STYLE_PLAIN, size = Font.SIZE_MEDIUM; String[] tokens = midlet.split(params, ' '); for (int i = 0; i < tokens.length; i++) { String token = tokens[i].toLowerCase(); if (token.equals("system")) { face = Font.FACE_SYSTEM; } else if (token.equals("monospace")) { face = Font.FACE_MONOSPACE; } else if (token.equals("proportional")) { face = Font.FACE_PROPORTIONAL; } else if (token.equals("bold")) { style |= Font.STYLE_BOLD; } else if (token.equals("italic")) { style |= Font.STYLE_ITALIC; } else if (token.equals("ul") || token.equals("underline") || token.equals("underlined")) { style |= Font.STYLE_UNDERLINED; } else if (token.equals("small")) { size = Font.SIZE_SMALL; } else if (token.equals("medium")) { size = Font.SIZE_MEDIUM; } else if (token.equals("large")) { size = Font.SIZE_LARGE; } } Font f = Font.getFont(face, style, size); return f == null ? Font.getDefaultFont() : f; }

        public void run() { if (root instanceof LuaFunction) { Vector arg = new Vector(); try { ((LuaFunction) root).call(arg); } catch (Throwable e) { midlet.print(midlet.getCatch(e), stdout, id, father); } } }

        public void commandAction(Command c, Displayable d) {
            try {
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
            catch (Exception e) { midlet.print(midlet.getCatch(e), stdout); midlet.sys.remove(PID); } 
            catch (Error e) { midlet.sys.remove(PID); }
        }
        public void commandAction(Command c, Item item) { try { if (root instanceof LuaFunction) { ((LuaFunction) root).call(new Vector()); } } catch (Exception e) { midlet.print(midlet.getCatch(e), stdout, id, father); midlet.sys.remove(PID); } catch (Error e) { midlet.sys.remove(PID); } }
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
            } catch (Exception e) { midlet.print(midlet.getCatch(e), stdout, id, father); midlet.sys.remove(PID); } catch (Error e) { midlet.sys.remove(PID); } 
        }
    }
}
// |
// EOF