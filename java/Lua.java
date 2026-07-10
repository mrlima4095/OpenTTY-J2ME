import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import javax.swing.text.*;

// Lua Runtime - Java 8 Version
public class Lua {
    public boolean breakLoop = false, doreturn = false, kill = true, gc = true;
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0;
    public Hashtable<String, Object> globals = new Hashtable<>(), father, requireCache = new Hashtable<>(), labels = new Hashtable<>();
    public Vector<Token> tokens;

    private static JFrame sharedFrame;
    private static JTabbedPane tabs;
    
    public int status = 0;
    
    // LuaFunction constants
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
    public static final Boolean TRUE = Boolean.TRUE, FALSE = Boolean.FALSE;
    public static final Object LUA_NIL = new Object();
    
    public static class Token { 
        int type; 
        Object value; 
        Token(int type, Object value) { 
            this.type = type; 
            this.value = value; 
        } 
        public String toString() { 
            return "Token(type=" + type + ", value=" + value + ")"; 
        } 
    }
    
    // 
    public Lua(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable<String, Object> scope) {
        this.midlet = midlet; 
        this.id = id; 
        this.PID = pid; 
        this.proc = proc; 
        this.stdout = stdout; 
        this.father = scope;
        this.tokenIndex = 0; 

        Hashtable<String, Object> os = new Hashtable<>(), io = new Hashtable<>(), 
                            string = new Hashtable<>(), table = new Hashtable<>(), 
                            pkg = new Hashtable<>(), graphics = new Hashtable<>(), 
                            socket = new Hashtable<>(), http = new Hashtable<>(), 
                            java = new Hashtable<>(), jdb = new Hashtable<>(), 
                            math = new Hashtable<>(), audio = new Hashtable<>(), 
                            push = new Hashtable<>(), base64 = new Hashtable<>();
        
        // Array de funções e loaders para os pacotes
        String[] funcs;
        int[] loaders;
        
        // Pacote os
        funcs = new String[] { "getenv", "setenv", "clock", "setlocale", "exit", "date", "getpid", "setproc", "getproc", "getcwd", "request", "getuid", "chdir", "open", "su", "remove", "scope", "join", "mkdir" }; 
        loaders = new int[] { GETENV, SETENV, CLOCK, SETLOC, EXIT, DATE, GETPID, SETPROC, GETPROC, GETCWD, REQUEST, GETUID, CHDIR, PREQ, SU, REMOVE, SCOPE, JOIN, MKDIR };
        for (int i = 0; i < funcs.length; i++) { os.put(funcs[i], new LuaFunction(loaders[i])); } 
        
        // Verificar se midlet.shell não é null antes de usar
        if (midlet.shell != null && midlet.shell instanceof LuaFunction) {
            os.put("execute", midlet.shell);
        } else {
            os.put("execute", new LuaFunction(EXEC));
        }
        globals.put("os", os);

        // Pacote io
        funcs = new String[] { "read", "write", "close", "open", "popen", "dirs", "setstdout", "mount", "new", "copy" }; 
        loaders = new int[] { READ, WRITE, CLOSE, OPEN, POPEN, DIRS, SETOUT, MOUNT, GEN, COPY };
        for (int i = 0; i < funcs.length; i++) { io.put(funcs[i], new LuaFunction(loaders[i])); } 
        io.put("stdout", stdout != null ? stdout : new StringBuffer()); 
        
        // Verificar se midlet.stdin não é null
        if (midlet.stdin != null) {
            io.put("stdin", midlet.stdin);
        } else {
            io.put("stdin", new JTextField()); // Ou null
        }
        globals.put("io", io);

        // Pacote table
        funcs = new String[] { "insert", "concat", "remove", "sort", "move", "unpack", "pack", "decode" }; 
        loaders = new int[] { TB_INSERT, TB_CONCAT, TB_REMOVE, TB_SORT, TB_MOVE, TB_UNPACK, TB_PACK, TB_DECODE };
        for (int i = 0; i < funcs.length; i++) { table.put(funcs[i], new LuaFunction(loaders[i])); } 
        globals.put("table", table);

        // Pacote audio
        funcs = new String[] { "load", "play", "pause", "volume", "duration", "time" }; 
        loaders = new int[] { AUDIO_LOAD, AUDIO_PLAY, AUDIO_PAUSE, AUDIO_VOLUME, AUDIO_DURATION, AUDIO_TIME };
        for (int i = 0; i < funcs.length; i++) { audio.put(funcs[i], new LuaFunction(loaders[i])); } 
        globals.put("audio", audio);

        // Pacote base64
        funcs = new String[] { "encode", "decode" }; 
        loaders = new int[] { BASE64_ENCODE, BASE64_DECODE };
        for (int i = 0; i < funcs.length; i++) { base64.put(funcs[i], new LuaFunction(loaders[i])); } 
        globals.put("base64", base64);

        // Pacote http
        funcs = new String[] { "get", "post", "rget", "rpost" }; 
        loaders = new int[] { HTTP_GET, HTTP_POST, HTTP_RGET, HTTP_RPOST };
        for (int i = 0; i < funcs.length; i++) { http.put(funcs[i], new LuaFunction(loaders[i])); } 
        socket.put("http", http);

        // Pacote java
        funcs = new String[] { "class", "getName", "delete", "run", "thread", "sleep" }; 
        loaders = new int[] { CLASS, NAME, DELETE, RUN, THREAD, SLEEP };
        for (int i = 0; i < funcs.length; i++) { java.put(funcs[i], new LuaFunction(loaders[i])); } 
        
        // Verificar se midlet.username não é null
        jdb.put("username", midlet.username != null ? midlet.username : "");
        jdb.put("cache", midlet.cache); 
        jdb.put("build", midlet.build != null ? midlet.build : "unknown"); 
        jdb.put("uptime", new LuaFunction(UPTIME)); 
        java.put("midlet", jdb); 
        globals.put("java", java);

        // Pacote socket
        funcs = new String[] { "connect", "peer", "device", "server", "accept" }; 
        loaders = new int[] { CONNECT, PEER, DEVICE, SERVER, ACCEPT };
        for (int i = 0; i < funcs.length; i++) { socket.put(funcs[i], new LuaFunction(loaders[i])); } 
        globals.put("socket", socket);

        // Pacote push
        funcs = new String[] { "register", "unregister", "list", "pending", "setAlarm" }; 
        loaders = new int[] { PUSH_REGISTER, PUSH_UNREGISTER, PUSH_LIST, PUSH_PENDING, PUSH_SET_ALARM };
        for (int i = 0; i < funcs.length; i++) { push.put(funcs[i], new LuaFunction(loaders[i])); } 
        globals.put("push", push);

        // Pacote graphics
        funcs = new String[] { "display", "new", "render", "append", "addCommand", "handler", "getCurrent", "SetTitle", "SetTicker", "vibrate", "SetLabel", "SetText", "GetLabel", "GetText", "clear" }; 
        loaders = new int[] { DISPLAY, NEW, RENDER, APPEND, ADDCMD, HANDLER, GETCURRENT, TITLE, TICKER, VIBRATE, SETLABEL, SETTEXT, GETLABEL, GETTEXT, CLEAR_SCREEN };
        for (int i = 0; i < funcs.length; i++) { graphics.put(funcs[i], new LuaFunction(loaders[i])); } 
        graphics.put("db", midlet.graphics != null ? midlet.graphics : new Hashtable<>()); 
        globals.put("graphics", graphics);

        // Pacote string
        funcs = new String[] { "upper", "lower", "len", "find", "match", "reverse", "sub", "hash", "byte", "char", "trim", "uuid", "split", "getCommand", "getArgument", "env", "getpattern", "startswith", "endswith" }; 
        loaders = new int[] { UPPER, LOWER, LEN, FIND, MATCH, REVERSE, SUB, HASH, BYTE, CHAR, TRIM, UUID, SPLIT, GETCMD, GETARGS, ENV, GETPATTERN, STARTSWITH, ENDSWITH };
        for (int i = 0; i < funcs.length; i++) { string.put(funcs[i], new LuaFunction(loaders[i])); } 
        globals.put("string", string);

        // Funções globais
        funcs = new String[] { "print", "error", "pcall", "require", "load", "pairs", "ipairs", "collectgarbage", "tostring", "tonumber", "select", "type", "getAppProperty", "setmetatable", "getmetatable" }; 
        loaders = new int[] { PRINT, ERROR, PCALL, REQUIRE, LOADS, PAIRS, IPAIRS, GC, TOSTRING, TONUMBER, SELECT, TYPE, GETPROPERTY, SETMETATABLE, GETMETATABLE };
        for (int i = 0; i < funcs.length; i++) { globals.put(funcs[i], new LuaFunction(loaders[i])); }

        // Pacote package
        pkg.put("loaded", requireCache); 
        pkg.put("loadlib", new LuaFunction(REQUIRE)); 
        globals.put("package", pkg);
        
        // Pacote math
        math.put("random", new LuaFunction(RANDOM)); 
        globals.put("math", math);
        
        // Globais
        globals.put("_VERSION", "Lua J2ME"); 
        globals.put("_G", globals);
    }
    
    // Run Source code
    public Hashtable<String, Object> run(String source, String code, Hashtable<Object, Object> args) { 
        midlet.sys.put(PID, proc); 
        globals.put("arg", args);

        Hashtable<String, Object> ITEM = new Hashtable<>(); 
        
        try { 
            this.tokens = tokenize(code); 
            collectLabels();
            
            while (peek().type != EOF) { 
                Object res = statement(globals); 
                if (doreturn) { 
                    if (res != null) { 
                        ITEM.put("object", res); 
                    } 
                    doreturn = false; 
                    break; 
                } 
            }
        } 
        catch (Exception e) { 
            midlet.print(midlet.getCatch(e), stdout, id, father); 
            status = 1; 
        } 
        catch (Error e) { 
            if (e.getMessage() != null) { 
                midlet.print(e.getMessage(), stdout, id, father); 
            } 
            status = 1; 
        }

        if (kill) { 
            midlet.sys.remove(PID); 
        }
        ITEM.put("status", status);
        return ITEM;
    }
    
    // Tokenizer
    public Vector<Token> tokenize(String code) throws Exception {
        if (midlet.cacheLua.containsKey(code)) { 
            return (Vector<Token>) midlet.cacheLua.get(code); 
        }

        Vector<Token> tokens = new Vector<>();
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
                if (i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') { 
                    tokens.addElement(new Token(VARARG, "...")); 
                    i += 3; 
                } 
                else if (i + 1 < code.length() && code.charAt(i + 1) == '.') { 
                    tokens.addElement(new Token(CONCAT, "..")); 
                    i += 2; 
                } 
                else { 
                    tokens.addElement(new Token(DOT, ".")); 
                    i++; 
                }
            }
            else if (c == ':') {
                if (i + 1 < code.length() && code.charAt(i + 1) == ':') {
                    i += 2;
                    
                    StringBuffer sb = new StringBuffer();
                    while (i < code.length() && (isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) { 
                        sb.append(code.charAt(i)); 
                        i++; 
                    }
                    
                    if (i + 1 < code.length() && code.charAt(i) == ':' && code.charAt(i + 1) == ':') { 
                        i += 2; 
                        tokens.addElement(new Token(LABEL, sb.toString())); 
                    }
                    else { 
                        i -= 2; 
                        tokens.addElement(new Token(COLON, ":")); 
                        i++; 
                    }
                } else { 
                    tokens.addElement(new Token(COLON, ":")); 
                    i++; 
                }
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
                try { 
                    double numValue = Double.parseDouble(sb.toString()); 
                    tokens.addElement(new Token(NUMBER, numValue)); 
                } 
                catch (NumberFormatException e) { 
                    throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); 
                }
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
                try { 
                    double numValue = Double.parseDouble(sb.toString()); 
                    tokens.addElement(new Token(NUMBER, numValue)); 
                } 
                catch (NumberFormatException e) { 
                    throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); 
                }
            }
            else if (c == '"' || c == '\'') { 
                char quoteChar = c; 
                StringBuffer sb = new StringBuffer(); 
                i++; 
                while (i < code.length() && code.charAt(i) != quoteChar) { 
                    sb.append(code.charAt(i)); 
                    i++; 
                } 
                if (i < code.length() && code.charAt(i) == quoteChar) { 
                    i++; 
                } 
                tokens.addElement(new Token(STRING, sb.toString())); 
            }
            else if (c == '[' && i + 1 < code.length() && code.charAt(i + 1) == '[') { 
                i += 2; 
                StringBuffer sb = new StringBuffer(); 
                while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) { 
                    sb.append(code.charAt(i)); 
                    i++; 
                } 
                if (i + 1 < code.length()) { 
                    i += 2; 
                } 
                tokens.addElement(new Token(STRING, sb.toString())); 
            }
            else if (isLetter(c)) { 
                StringBuffer sb = new StringBuffer(); 
                while (i < code.length() && isLetterOrDigit(code.charAt(i))) { 
                    sb.append(code.charAt(i)); 
                    i++; 
                } 
                String word = sb.toString(); 
                tokens.addElement(new Token(
                    (word.equals("true") || word.equals("false")) ? BOOLEAN : 
                    word.equals("nil") ? NIL : 
                    word.equals("and") ? AND : 
                    word.equals("or") ? OR : 
                    word.equals("not") ? NOT : 
                    word.equals("if") ? IF : 
                    word.equals("then") ? THEN : 
                    word.equals("else") ? ELSE : 
                    word.equals("elseif") ? ELSEIF : 
                    word.equals("end") ? END : 
                    word.equals("while") ? WHILE : 
                    word.equals("do") ? DO : 
                    word.equals("return") ? RETURN : 
                    word.equals("function") ? FUNCTION : 
                    word.equals("local") ? LOCAL : 
                    word.equals("for") ? FOR : 
                    word.equals("in") ? IN : 
                    word.equals("break") ? BREAK : 
                    word.equals("repeat") ? REPEAT : 
                    word.equals("until") ? UNTIL : 
                    word.equals("goto") ? GOTO : 
                    IDENTIFIER, word)); 
            }
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
            else if (c == '=') { 
                if (i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                    tokens.addElement(new Token(EQ, "==")); 
                    i += 2; 
                } else { 
                    tokens.addElement(new Token(ASSIGN, "=")); 
                    i++; 
                } 
            }
            else if (c == '~') { 
                if (i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                    tokens.addElement(new Token(NE, "~=")); 
                    i += 2; 
                } else { 
                    throw new Exception("Unexpected character '~'"); 
                } 
            }
            else if (c == '<') { 
                if (i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                    tokens.addElement(new Token(LE, "<=")); 
                    i += 2; 
                } else { 
                    tokens.addElement(new Token(LT, "<")); 
                    i++; 
                } 
            }
            else if (c == '>') { 
                if (i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                    tokens.addElement(new Token(GE, ">=")); 
                    i += 2; 
                } else { 
                    tokens.addElement(new Token(GT, ">")); 
                    i++; 
                } 
            }
            else if (c == '{') { tokens.addElement(new Token(LBRACE, "{")); i++; }
            else if (c == '}') { tokens.addElement(new Token(RBRACE, "}")); i++; }
            else if (c == '[') { tokens.addElement(new Token(LBRACKET, "[")); i++; }
            else if (c == ']') { tokens.addElement(new Token(RBRACKET, "]")); i++; }
            else { throw new Exception("Unexpected character '" + c + "'"); }
        }

        tokens.addElement(new Token(EOF, "EOF"));
        if (midlet.useCache) { 
            if (midlet.cacheLua.size() > 100) { 
                midlet.cacheLua.clear(); 
            } 
            midlet.cacheLua.put(code, tokens); 
        }
        return tokens;
    }
    
    public Token peek() { 
        if (tokenIndex < tokens.size()) { 
            return tokens.elementAt(tokenIndex); 
        } 
        return new Token(EOF, "EOF"); 
    }
    
    public Token peekNext() { 
        if (tokenIndex + 1 < tokens.size()) { 
            return tokens.elementAt(tokenIndex + 1); 
        } 
        return new Token(EOF, "EOF"); 
    }
    
    private Token consume() { 
        if (tokenIndex < tokens.size()) { 
            return tokens.elementAt(tokenIndex++); 
        } 
        return new Token(EOF, "EOF"); 
    }
    
    private Token consume(int expectedType) throws Exception { 
        Token token = peek(); 
        if (token.type == expectedType) { 
            tokenIndex++; 
            return token; 
        } 
        throw new Exception("Expected token type " + expectedType + " but got " + token.type + " with value " + token.value); 
    }
    
    // Statements
    public Object statement(Hashtable<String, Object> scope) throws Exception {
        Token current = peek();

        if (status != 0) { 
            midlet.sys.remove(PID); 
            throw new Error(); 
        }
        if (midlet.sys.containsKey(PID)) { } else { 
            throw new Error("Process killed"); 
        } 

        if (current.type == IDENTIFIER) {
            int la = 0;
            boolean patternIsMultiAssign = false;
            if (tokenIndex + la < tokens.size() && tokens.elementAt(tokenIndex + la).type == IDENTIFIER) {
                la++; 
                while (tokenIndex + la < tokens.size() && tokens.elementAt(tokenIndex + la).type == COMMA) {
                    if (!(tokenIndex + la + 1 < tokens.size() && tokens.elementAt(tokenIndex + la + 1).type == IDENTIFIER)) {
                        patternIsMultiAssign = false;
                        break;
                    }
                    la += 2; 
                }
                if (tokenIndex + la < tokens.size() && tokens.elementAt(tokenIndex + la).type == ASSIGN) { 
                    patternIsMultiAssign = true; 
                }
            }

            Token next = peekNext();
            if (!patternIsMultiAssign && next.type == LPAREN) { 
                String funcName = (String) consume(IDENTIFIER).value; 
                callFunction(funcName, scope); 
                return null; 
            }
            if (patternIsMultiAssign) {
                Vector<String> varNames = new Vector<>();
                varNames.addElement((String) consume(IDENTIFIER).value);
                while (peek().type == COMMA) {
                    consume(COMMA);
                    varNames.addElement((String) consume(IDENTIFIER).value);
                }
                consume(ASSIGN);

                Vector<Object> values = new Vector<>();
                values.addElement(expression(scope));
                while (peek().type == COMMA) { 
                    consume(COMMA); 
                    values.addElement(expression(scope)); 
                }

                Vector<Object> assignValues = new Vector<>();
                for (int i = 0; i < values.size(); i++) {
                    Object v = values.elementAt(i);
                    if (i == values.size() - 1 && v instanceof Vector) {
                        Vector<Object> expanded = (Vector<Object>) v;
                        for (int j = 0; j < expanded.size(); j++) { 
                            assignValues.addElement(expanded.elementAt(j)); 
                        }
                    } 
                    else { 
                        assignValues.addElement(v); 
                    }
                }

                for (int i = 0; i < varNames.size(); i++) {
                    String v = varNames.elementAt(i);
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
                if (!(targetTable instanceof Hashtable)) { 
                    throw new Exception("Attempt to index non-table value"); 
                }

                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object value = expression(scope);
                    ((Hashtable<Object, Object>) targetTable).put(key, value == null ? LUA_NIL : value);
                    return null;
                } 
                else if (peek().type == LPAREN) { 
                    return callFunctionObject(unwrap(((Hashtable<Object, Object>) targetTable).get(key)), scope); 
                }
                else { 
                    return unwrap(((Hashtable<Object, Object>) targetTable).get(key)); 
                }
            } 
            else if (peek().type == COLON) {
                Object self = unwrap(scope.get(varName));
                if (self == null && globals.containsKey(varName)) { 
                    self = unwrap(globals.get(varName)); 
                }
                if (self == null) { 
                    throw new Exception("attempt to call method on nil value: " + varName); 
                }

                consume(COLON);
                String methodName = (String) consume(IDENTIFIER).value;
                Object methodObj = resolveMethod(self);

                if (methodObj instanceof Hashtable) {
                    Hashtable<Object, Object> table = (Hashtable<Object, Object>) methodObj;
                    Object fn = unwrap(table.get(methodName));
                    if (fn == null) { 
                        throw new Exception("method '" + methodName + "' not found " + ((methodObj == self && self instanceof Hashtable) ? "in table: " + varName : "for type: " + type(self))); 
                    }
                    
                    return callMethod(self, varName, fn, methodName, scope);
                }

                throw new Exception("attempt to call method on unsupported type: " + type(self));
            }
            else {
                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object value = expression(scope);
                    scope.put(varName, value == null ? LUA_NIL : value);
                    return null;
                } 
                else if (peek().type == LPAREN) { 
                    return callFunction(varName, scope); 
                } 
                else { 
                    return unwrap(scope.get(varName)); 
                }
            }
        }
        else if (current.type == LABEL) { 
            labels.put((String) consume(LABEL).value, tokenIndex); 
            return null; 
        }
        else if (current.type == GOTO) {
            consume(GOTO);
            String labelName = (String) consume(IDENTIFIER).value;

            if (labels.containsKey(labelName)) { } else { 
                throw new Exception("undefined label '" + labelName + "'"); 
            }

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
                    if (doreturn) { 
                        return result; 
                    }
                }
            } 
            else { 
                skipIfBodyUntilElsePart(); 
            }

            while (peek().type == ELSEIF) {
                consume(ELSEIF);
                cond = expression(scope);
                consume(THEN);

                if (!taken && isTruthy(cond)) {
                    taken = true;
                    while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                        result = statement(scope);
                        if (doreturn) { 
                            return result; 
                        }
                    }
                }
                else { 
                    skipIfBodyUntilElsePart(); 
                }
            }

            if (peek().type == ELSE) {
                consume(ELSE);
                if (!taken) { 
                    while (peek().type != END) {  
                        result = statement(scope); 
                        if (doreturn) { 
                            return result; 
                        } 
                    } 
                } 
                else { 
                    skipUntilMatchingEnd(); 
                }
            }

            consume(END);
            return result;
        }
        else if (current.type == FOR) {
            consume(FOR);
            loopDepth++;

            if (peek().type == IDENTIFIER) {
                Token t1 = peek();
                int save = tokenIndex;
                String name = (String) consume(IDENTIFIER).value;

                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Object a = expression(scope);
                    consume(COMMA);
                    Object b = expression(scope);
                    Double start = (a instanceof Double) ? (Double) a : Double.parseDouble(toLuaString(a));
                    Double stop  = (b instanceof Double) ? (Double) b : Double.parseDouble(toLuaString(b));
                    Double step  = 1.0;
                    if (peek().type == COMMA) {
                        consume(COMMA);
                        Object c = expression(scope);
                        step = (c instanceof Double) ? (Double) c : Double.parseDouble(toLuaString(c));
                        if (step == 0.0) throw new Exception("for step must not be zero");
                    }
                    consume(DO);

                    Vector<Token> bodyTokens = new Vector<>();
                    int depth = 1;
                    while (depth > 0) {
                        Token tk = consume();
                        if (tk.type == IF || tk.type == WHILE || tk.type == FUNCTION || tk.type == FOR) depth++;
                        else if (tk.type == END) depth--;
                        else if (tk.type == EOF) throw new Exception("Unmatched 'for' statement: Expected 'end'");
                        if (depth > 0) bodyTokens.addElement(tk);
                    }

                    double iVal = start, stopVal = stop, stepVal = step;

                    while ((stepVal > 0 && iVal <= stopVal) || (stepVal < 0 && iVal >= stopVal)) {
                        if (breakLoop) { 
                            breakLoop = false; 
                            break; 
                        }

                        scope.put(name, iVal);

                        int originalTokenIndex = tokenIndex;
                        Vector<Token> originalTokens = tokens;

                        tokens = bodyTokens;
                        tokenIndex = 0;

                        Object ret = null;
                        while (peek().type != EOF) {
                            ret = statement(scope);
                            if (doreturn) { break; }
                        }

                        tokenIndex = originalTokenIndex;
                        tokens = originalTokens;

                        if (ret != null) { 
                            return ret; 
                        }

                        iVal += stepVal;
                    }

                    loopDepth--;
                    return null;
                } 
                else {
                    tokenIndex = save;
                    Vector<String> names = new Vector<>();
                    names.addElement((String) consume(IDENTIFIER).value);
                    while (peek().type == COMMA) {
                        consume(COMMA);
                        names.addElement((String) consume(IDENTIFIER).value);
                    }
                    consume(IN);
                    Object iterSrc = expression(scope);
                    consume(DO);

                    Vector<Token> bodyTokens = new Vector<>();
                    int depth2 = 1;
                    while (depth2 > 0) {
                        Token tk = consume();
                        if (tk.type == IF || tk.type == WHILE || tk.type == FUNCTION || tk.type == FOR) depth2++;
                        else if (tk.type == END) depth2--;
                        else if (tk.type == EOF) throw new Exception("Unmatched 'for' statement: Expected 'end'");
                        if (depth2 > 0) bodyTokens.addElement(tk);
                    }

                    if (iterSrc instanceof Hashtable && ((Hashtable<?, ?>) iterSrc).containsKey("__table")) {
                        Hashtable<Object, Object> iterator = (Hashtable<Object, Object>) iterSrc;
                        Object tableObj = iterator.get("__table");
                        int currentIndex = ((Double) iterator.get("__index")).intValue();
                        
                        if (tableObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tableObj;
                            Vector<Object> list = toVector(table);
                            
                            for (int idx = currentIndex; idx < list.size(); idx++) {
                                Object item = list.elementAt(idx);
                                
                                if (names.size() >= 1) scope.put(names.elementAt(0), (double) (idx + 1));
                                if (names.size() >= 2) scope.put(names.elementAt(1), item == null ? LUA_NIL : item);

                                iterator.put("__index", (double) (idx + 1));

                                int originalTokenIndex = tokenIndex;
                                Vector<Token> originalTokens = tokens;
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
                        Hashtable<Object, Object> ht = (Hashtable<Object, Object>) iterSrc;
                        for (Enumeration<Object> e = ht.keys(); e.hasMoreElements();) {
                            Object k = e.nextElement();
                            Object v = unwrap(ht.get(k));
                            if (names.size() >= 1) scope.put(names.elementAt(0), (k == null ? LUA_NIL : k));
                            if (names.size() >= 2) scope.put(names.elementAt(1), (v == null ? LUA_NIL : v));

                            int originalTokenIndex = tokenIndex;
                            Vector<Token> originalTokens = tokens;
                            tokens = bodyTokens;
                            tokenIndex = 0;

                            Object ret = null;
                            while (peek().type != EOF) {
                                ret = statement(scope);
                                if (doreturn) { 
                                    return ret; 
                                }
                            }

                            tokenIndex = originalTokenIndex;
                            tokens = originalTokens;
                            if (ret != null) { 
                                return ret; 
                            }

                            if (breakLoop) { 
                                breakLoop = false; 
                                break; 
                            }
                        }
                    } 
                    else if (iterSrc instanceof Vector) {
                        Vector<Object> vec = (Vector<Object>) iterSrc;
                        for (int idx = 0; idx < vec.size(); idx++) {
                            Object item = vec.elementAt(idx);
                            Object k = null, v = null;
                            if (item instanceof Vector) {
                                Vector<Object> pair = (Vector<Object>) item;
                                if (pair.size() > 0) k = pair.elementAt(0);
                                if (pair.size() > 1) v = pair.elementAt(1);
                            } else {
                                k = (double) (idx + 1);
                                v = item;
                            }
                            if (names.size() >= 1) scope.put(names.elementAt(0), (k == null ? LUA_NIL : k));
                            if (names.size() >= 2) scope.put(names.elementAt(1), (v == null ? LUA_NIL : v));

                            int originalTokenIndex = tokenIndex;
                            Vector<Token> originalTokens = tokens;
                            tokens = bodyTokens;
                            tokenIndex = 0;

                            Object ret = null;
                            while (peek().type != EOF) {
                                ret = statement(scope);
                                if (doreturn) { 
                                    return ret; 
                                }
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
                    else if (iterSrc == null) { } 
                    else { 
                        throw new Exception("Generic for: unsupported iterator source"); 
                    }

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

                    if (doreturn || breakLoop) { 
                        while (peek().type != UNTIL && peek().type != EOF) { 
                            consume(); 
                        } 
                        break; 
                    }
                }

                consume(UNTIL);
                Object cond = expression(scope);

                if (isTruthy(cond) || doreturn) { 
                    break; 
                }
                else if (breakLoop) { 
                    breakLoop = false; 
                    break; 
                } 
            }

            loopDepth--;
            return result;
        }
        else if (current.type == RETURN) {
            consume(RETURN);
            doreturn = true;

            if (peek().type == EOF || peek().type == END) { 
                return new Vector<Object>(); 
            }

            Vector<Object> results = new Vector<>();
            results.addElement(expression(scope));
            while (peek().type == COMMA) { 
                consume(COMMA); 
                results.addElement(expression(scope)); 
            }
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
                if (!(targetTable instanceof Hashtable)) { 
                    throw new Exception("Attempt to index non-table value in function definition"); 
                } 
            }
            
            consume(LPAREN);
            Vector<String> params = new Vector<>();
            while (true) {
                int t = peek().type;

                if (t == IDENTIFIER) { 
                    params.addElement((String) consume(IDENTIFIER).value); 
                } 
                else if (t == VARARG) { 
                    consume(VARARG); 
                    params.addElement("..."); 
                    break; 
                } 
                else { 
                    break; 
                } 

                if (peek().type == COMMA) { 
                    consume(COMMA); 
                } 
                else { 
                    break; 
                }
            }
            consume(RPAREN);

            Vector<Token> bodyTokens = new Vector<>();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                if (token.type == FUNCTION || token.type == IF || token.type == DO) { depth++; }
                else if (token.type == END) { depth--; }
                else if (token.type == EOF) { 
                    throw new Exception("Unmatched 'function (" + funcName + ")' statement: Expected 'end'"); 
                }
                if (depth > 0) { 
                    bodyTokens.addElement(token); 
                }
            }

            LuaFunction func = new LuaFunction(params, bodyTokens, scope);

            if (isTableAssignment) { 
                ((Hashtable<Object, Object>) targetTable).put(key, func); 
            } 
            else { 
                scope.put(funcName, func); 
            }

            return null;
        } 
        else if (current.type == LOCAL) {
            consume(LOCAL);

            if (peek().type == FUNCTION) {
                consume(FUNCTION);
                String funcName = (String) consume(IDENTIFIER).value;

                consume(LPAREN);
                Vector<String> params = new Vector<>();
                while (true) {
                    int t = peek().type;
                    if (t == IDENTIFIER) { 
                        params.addElement((String) consume(IDENTIFIER).value); 
                    } 
                    else if (t == VARARG) { 
                        consume(VARARG); 
                        params.addElement("..."); 
                        break; 
                    } 
                    else { 
                        break; 
                    }

                    if (peek().type == COMMA) { 
                        consume(COMMA); 
                    } 
                    else { 
                        break; 
                    }
                }
                consume(RPAREN);
    
                Vector<Token> bodyTokens = new Vector<>();
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
                Vector<String> varNames = new Vector<>();

                varNames.addElement((String) consume(IDENTIFIER).value);
                while (peek().type == COMMA) { 
                    consume(COMMA); 
                    varNames.addElement((String) consume(IDENTIFIER).value); 
                }

                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    Vector<Object> values = new Vector<>();
                    values.addElement(expression(scope));
                    while (peek().type == COMMA) { 
                        consume(COMMA); 
                        values.addElement(expression(scope)); 
                    }
        
                    Vector<Object> assignValues = new Vector<>();
                    for (int i = 0; i < values.size(); i++) {
                        Object v = values.elementAt(i);
                        if (i == values.size() - 1 && v instanceof Vector) {
                            Vector<Object> expanded = (Vector<Object>) v;
                            for (int j = 0; j < expanded.size(); j++) { 
                                assignValues.addElement(expanded.elementAt(j)); 
                            }
                        } else { 
                            assignValues.addElement(v); 
                        }
                    }

                    for (int i = 0; i < varNames.size(); i++) {
                        String v = varNames.elementAt(i);
                        Object val = i < assignValues.size() ? assignValues.elementAt(i) : null;
                        scope.put(v, val == null ? LUA_NIL : val);
                    }
                } 
                else { 
                    for (int i = 0; i < varNames.size(); i++) { 
                        String v = varNames.elementAt(i); 
                        scope.put(v, LUA_NIL); 
                    } 
                }

                return null;
            }
        }
        else if (current.type == BREAK) { 
            if (loopDepth == 0) { 
                throw new RuntimeException("break outside loop"); 
            } 
            consume(BREAK); 
            breakLoop = true; 
            return null; 
        }
        else if (current.type == DO) {
            consume(DO);
            
            Vector<Token> bodyTokens = new Vector<>();
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
            
            int originalTokenIndex = tokenIndex;
            Vector<Token> originalTokens = tokens;
            
            tokens = bodyTokens;
            tokenIndex = 0;
            
            Object result = null;
            while (peek().type != EOF) { 
                result = statement(scope); 
                if (doreturn) { 
                    break; 
                } 
            }
            
            tokenIndex = originalTokenIndex;
            tokens = originalTokens;
            
            return result;
        }
        else if (current.type == END) { 
            consume(END); 
            return null; 
        }
        else if (current.type == LPAREN || current.type == NUMBER || current.type == STRING || current.type == BOOLEAN || current.type == NIL || current.type == NOT) { 
            return expression(scope); 
        }

        throw new RuntimeException("Unexpected token at statement: " + current.toString() + " - " + peekNext().toString());
    }
    
    // Expressions
    private Object expression(Hashtable<String, Object> scope) throws Exception { 
        return logicalOr(scope); 
    }
    
    private Object logicalOr(Hashtable<String, Object> scope) throws Exception { 
        Object left = logicalAnd(scope); 
        while (peek().type == OR) { 
            consume(OR); 
            Object right = logicalAnd(scope); 
            left = isTruthy(left) ? left : right; 
        } 
        return left; 
    }
    
    private Object logicalAnd(Hashtable<String, Object> scope) throws Exception { 
        Object left = comparison(scope); 
        while (peek().type == AND) { 
            consume(AND); 
            Object right = comparison(scope); 
            left = isTruthy(left) ? right : left; 
        } 
        return left; 
    }
    
    private Object comparison(Hashtable<String, Object> scope) throws Exception { 
        Object left = concatenation(scope); 
        while (peek().type == EQ || peek().type == NE || peek().type == LT || peek().type == GT || peek().type == LE || peek().type == GE) { 
            Token op = consume(); 
            Object right = concatenation(scope); 
            if (op.type == EQ) { 
                left = (left == null && right == null) || (left != null && left.equals(right)); 
            } else if (op.type == NE) { 
                left = !((left == null && right == null) || (left != null && left.equals(right))); 
            } else if (op.type == LT) { 
                left = ((Double) left) < ((Double) right); 
            } else if (op.type == GT) { 
                left = ((Double) left) > ((Double) right); 
            } else if (op.type == LE) { 
                left = ((Double) left) <= ((Double) right); 
            } else if (op.type == GE) { 
                left = ((Double) left) >= ((Double) right); 
            } 
        } 
        return left; 
    }
    
    // Strings
    private String toLuaString(Object obj) { 
        if (obj == null || obj == LUA_NIL) { 
            return "nil"; 
        } 
        if (obj instanceof Boolean) { 
            return ((Boolean)obj).booleanValue() ? "true" : "false"; 
        } 
        if (obj instanceof Double) { 
            double d = ((Double)obj).doubleValue(); 
            if (d == (long)d) return String.valueOf((long)d); 
            return String.valueOf(d); 
        } 
        return midlet.escape(obj.toString()); 
    }
    
    private Object concatenation(Hashtable<String, Object> scope) throws Exception { 
        Object left = arithmetic(scope); 
        while (peek().type == CONCAT) { 
            consume(CONCAT); 
            Object right = arithmetic(scope); 
            left = toLuaString(left) + toLuaString(right); 
        } 
        return left; 
    }
    
    // Arithmetic
    private Object arithmetic(Hashtable<String, Object> scope) throws Exception {
        Object left = term(scope); 
        while (peek().type == PLUS || peek().type == MINUS) {
            Token op = consume();
            Object right = term(scope); 
            if (!(left instanceof Double) || !(right instanceof Double)) { 
                throw new ArithmeticException("Arithmetic operation on non-number types."); 
            }

            double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();
            if (op.type == PLUS) { 
                left = lVal + rVal; 
            } 
            else if (op.type == MINUS) { 
                left = lVal - rVal; 
            }
        }
        return left;
    }
    
    private Object term(Hashtable<String, Object> scope) throws Exception {
        Object left = exponentiation(scope);
        while (peek().type == MULTIPLY || peek().type == DIVIDE || peek().type == MODULO) {
            Token op = consume();
            Object right = exponentiation(scope);
            if (!(left instanceof Double) || !(right instanceof Double)) { 
                throw new ArithmeticException("Arithmetic operation on non-number types."); 
            }
            double lVal = ((Double) left).doubleValue(), rVal = ((Double) right).doubleValue();

            if (op.type == MULTIPLY) { 
                left = lVal * rVal; 
            } 
            else if (op.type == DIVIDE) { 
                if (rVal == 0) { 
                    throw new Exception("Division by zero."); 
                } 
                left = lVal / rVal; 
            } 
            else if (op.type == MODULO) { 
                if (rVal == 0) { 
                    throw new Exception("Modulo by zero."); 
                } 
                left = lVal % rVal; 
            }
        }
        return left;
    }
    
    private Object exponentiation(Hashtable<String, Object> scope) throws Exception {
        Object left = factor(scope);
        while (peek().type == POWER) {
            consume(POWER);
            Object right = factor(scope);
            if (!(left instanceof Double) || !(right instanceof Double)) { 
                throw new ArithmeticException("Arithmetic operation on non-number types.");  
            }

            double base = ((Double) left).doubleValue(), exponent = ((Double) right).doubleValue(), result;

            if (exponent == 0) { 
                result = 1; 
            } 
            else if (exponent == 0.5) { 
                if (base < 0) { 
                    throw new ArithmeticException("Square root of negative number."); 
                } 
                result = Math.sqrt(base); 
            } 
            else if (exponent < 0 && Math.floor(exponent) == exponent) {
                base = 1 / base;
                exponent = -exponent;
                result = 1;
                for (int i = 0; i < (int) exponent; i++) { 
                    result *= base; 
                }
            } 
            else if (Math.floor(exponent) == exponent) { 
                result = 1; 
                for (int i = 0; i < (int) exponent; i++) { 
                    result *= base; 
                } 
            } 
            else { 
                throw new ArithmeticException("Fractional exponent not supported: " + exponent); 
            }

            left = result;
        }
        return left;
    }
    
    // Build Objects
    private Object factor(Hashtable<String, Object> scope) throws Exception {
        Token current = peek();
        
        if (current.type == STRING || current.type == NUMBER || current.type == BOOLEAN || current.type == NIL) {
            Object base = null;

            if (current.type == STRING) { 
                base = consume(STRING).value; 
            } 
            else if (current.type == NUMBER) { 
                base = consume(NUMBER).value; 
            } 
            else if (current.type == BOOLEAN) { 
                consume(BOOLEAN); 
                base = current.value.equals("true"); 
            } 
            else if (current.type == NIL) { 
                consume(NIL); 
                base = null; 
            }

            while (peek().type == DOT || peek().type == COLON) {
                if (peek().type == DOT) {
                    consume(DOT);
                    String field = (String) consume(IDENTIFIER).value;

                    Object module = resolveMethod(base);
                    if (!(module instanceof Hashtable)) { 
                        throw new Exception("attempt to index non-table value after literal"); 
                    }
                    base = unwrap(((Hashtable<Object, Object>) module).get(field));
                } 
                else if (peek().type == COLON) {
                    consume(COLON);
                    String method = (String) consume(IDENTIFIER).value;

                    Object module = resolveMethod(base);
                    if (!(module instanceof Hashtable)) { 
                        throw new Exception("attempt to call method on non-table after literal"); 
                    }

                    Object func = unwrap(((Hashtable<Object, Object>) module).get(method));
                    if (func == null) { 
                        throw new Exception("method '" + method + "' not found for type: " + type(base)); 
                    }

                    base = callMethod(base, null, func, method, scope);
                }
            }

            return base;
        }
        else if (current.type == NOT) { 
            consume(NOT); 
            return !isTruthy(factor(scope)); 
        } 
        else if (current.type == LPAREN) { 
            consume(LPAREN); 
            Object value = expression(scope); 
            consume(RPAREN); 
            return value; 
        } 
        else if (current.type == LENGTH) { 
            consume(LENGTH); 
            Object val = factor(scope); 
            if (val == null || val instanceof Boolean) { 
                throw new RuntimeException("attempt to get length of a " + (val == null ? "nil" : "boolean") + " value"); 
            } else if (val instanceof String) { 
                return (double) ((String) val).length(); 
            } else if (val instanceof Hashtable) { 
                return (double) ((Hashtable<?, ?>) val).size(); 
            } else if (val instanceof Vector) { 
                return (double) ((Vector<?>) val).size(); 
            } else if (val instanceof InputStream) { 
                return (double) ((InputStream) val).available(); 
            } else { 
                return 0.0; 
            } 
        }
        else if (current.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            Object value = unwrap(scope.get(name));
            if (value == null && scope == globals == false) { }
            if (value == null && globals.containsKey(name)) { 
                value = unwrap(globals.get(name)); 
            }
            while (peek().type == LBRACKET || peek().type == DOT) {
                Object key = null;
                if (peek().type == LBRACKET) { 
                    consume(LBRACKET); 
                    key = expression(scope); 
                    consume(RBRACKET); 
                } 
                else { 
                    consume(DOT); 
                    key = (String) consume(IDENTIFIER).value; 
                }

                if (value == null) { 
                    return null; 
                }
                if (!(value instanceof Hashtable)) { 
                    throw new Exception("attempt to index a non-table value"); 
                }

                value = unwrap(((Hashtable<Object, Object>) value).get(key));
            }

            if (peek().type == COLON) {
                String objectName = (String) tokens.elementAt(tokenIndex - 1).value;
            
                Object self = unwrap(scope.get(objectName));
                if (self == null && globals.containsKey(objectName)) { 
                    self = unwrap(globals.get(objectName)); 
                }
                if (self == null) { 
                    throw new Exception("attempt to call method on nil value: " + objectName); 
                }
            
                consume(COLON);
                String methodName = (String) consume(IDENTIFIER).value;
            
                Object module = resolveMethod(self), func = null;
            
                if (module == self && self instanceof Hashtable) { 
                    func = unwrap(((Hashtable<Object, Object>) self).get(methodName)); 
                } 
                else if (module instanceof Hashtable) { 
                    func = unwrap(((Hashtable<Object, Object>) module).get(methodName)); 
                }
            
                if (func == null) { 
                    throw new Exception("method '" + methodName + "' not found for type: " + type(self)); 
                }
            
                return callMethod(self, objectName, func, methodName, scope);
            }
            else if (peek().type == LPAREN) { 
                return callFunctionObject(value, scope); 
            }

            return value;
        }
        else if (current.type == FUNCTION) {
            consume(FUNCTION);

            consume(LPAREN);
            Vector<String> params = new Vector<>();
            while (true) {
                int t = peek().type;

                if (t == IDENTIFIER) { 
                    params.addElement((String) consume(IDENTIFIER).value); 
                } 
                else if (t == VARARG) { 
                    consume(VARARG); 
                    params.addElement("..."); 
                    break; 
                } 
                else { 
                    break; 
                }

                if (peek().type == COMMA) { 
                    consume(COMMA); 
                } 
                else { 
                    break; 
                }
            }
            consume(RPAREN);

            Vector<Token> bodyTokens = new Vector<>();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                
                if (token.type == FUNCTION || token.type == IF || token.type == DO) { 
                    depth++; 
                }
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
        else if (current.type == VARARG) { 
            consume(VARARG); 
            Object varargs = scope.get("..."); 
            if (varargs == null) { 
                return new Hashtable<Object, Object>(); 
            } 
            return varargs; 
        }
        else if (current.type == LBRACE) { 
            consume(LBRACE);
            Hashtable<Object, Object> table = new Hashtable<>();
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
                else { 
                    value = expression(scope); 
                    key = (double) index++; 
                }

                table.put(key, value == null ? LUA_NIL : value);

                if (peek().type == COMMA) { 
                    consume(COMMA); 
                } else if (peek().type == RBRACE) { 
                    break; 
                } else { 
                    throw new Exception("Malformed table syntax."); 
                }
            }

            consume(RBRACE);
            return table;
        }

        throw new Exception("Unexpected token at factor: " + current.toString());
    }
    
    // Call LuaFunction
    private Object callFunction(String funcName, Hashtable<String, Object> scope) throws Exception {
        consume(LPAREN);
        Vector<Object> args = new Vector<>();
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) { 
                consume(COMMA); 
                args.addElement(expression(scope)); 
            }
        }
        consume(RPAREN);

        Object funcObj = unwrap(scope.get(funcName));
        if (funcObj == null && globals.containsKey(funcName)) { 
            funcObj = unwrap(globals.get(funcName)); 
        }

        if (funcObj instanceof LuaFunction) { 
            return ((LuaFunction) funcObj).call(args); 
        }
        else { 
            throw new RuntimeException("Attempt to call a non-function value: " + funcName); 
        }
    }
    
    private Object callFunctionObject(Object funcObj, Hashtable<String, Object> scope) throws Exception {
        consume(LPAREN);
        Vector<Object> args = new Vector<>();
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) { 
                consume(COMMA); 
                args.addElement(expression(scope)); 
            }
        }
        consume(RPAREN);

        if (funcObj instanceof LuaFunction) { 
            return ((LuaFunction) funcObj).call(args); 
        }
        else { 
            throw new Exception("Attempt to call a non-function value (by object)."); 
        }
    }
    
    private Object resolveMethod(Object obj) {
        if (obj instanceof Hashtable) {
            Hashtable<Object, Object> table = (Hashtable<Object, Object>) obj;

            Object mt = table.get("__metatable");
            if (mt instanceof Hashtable) { 
                Object index = ((Hashtable<Object, Object>) mt).get("__index"); 
                if (index instanceof Hashtable || index instanceof LuaFunction) { 
                    return index; 
                } 
            }
        }

        String type = type(obj);
        return type.equals("string") ? globals.get("string") : 
            type.equals("table") ? globals.get("table") : 
            type.equals("stream") ? globals.get("io") : 
            type.equals("connection") || type.equals("server") ? globals.get("socket") : 
            type.equals("screen") || type.equals("image") ? globals.get("graphics") : obj;
    }
    
    private Object callMethod(Object self, String varName, Object methodObj, String methodName, Hashtable<String, Object> scope) throws Exception {
        if (methodObj == null) {
            methodObj = resolveMethod(self);
            Object table = unwrap(scope.get(varName));
            if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
            Object key = null;
            
            while (peek().type == DOT || peek().type == LBRACKET) {
                if (peek().type == DOT) { 
                    consume(DOT); 
                    Token field = consume(IDENTIFIER); 
                    key = field.value; 
                } 
                else if (peek().type == LBRACKET) { 
                    consume(LBRACKET); 
                    key = expression(scope); 
                    consume(RBRACKET); 
                }
        
                if (table == null) { 
                    throw new Exception("attempt to index a nil value"); 
                }
                if (!(table instanceof Hashtable)) { 
                    throw new Exception("attempt to index a non-table value"); 
                }
                if (peek().type == DOT || peek().type == LBRACKET) { 
                    methodObj = unwrap(((Hashtable<Object, Object>) table).get(key)); 
                }
            }
        }
        consume(LPAREN);
        Vector<Object> args = new Vector<>();
        
        args.addElement(self);
        if (peek().type != RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == COMMA) { 
                consume(COMMA); 
                args.addElement(expression(scope)); 
            }
        }
        consume(RPAREN);
        if (methodObj instanceof LuaFunction) { 
            return ((LuaFunction) methodObj).call(args); 
        } 
        else { 
            throw new Exception("attempt to call non-function as method: " + methodName); 
        }
    }
    
    // Handling NullPointers
    private Object wrap(Object v) { 
        return v == null ? LUA_NIL : v; 
    }
    
    private Object unwrap(Object v) { 
        return v == LUA_NIL ? null : v; 
    }
    
    private void skipIfBodyUntilElsePart() throws Exception { 
        int depth = 1; 
        while (true) { 
            Token t = consume(); 
            if (t.type == IF || t.type == WHILE || t.type == FUNCTION || t.type == FOR) { 
                depth++; 
            } else if (t.type == END) { 
                depth--; 
                if (depth == 0) { 
                    tokenIndex--; 
                    return; 
                } 
            } else if ((t.type == ELSEIF || t.type == ELSE) && depth == 1) { 
                tokenIndex--; 
                return; 
            } else if (t.type == EOF) { 
                throw new Exception("Unmatched 'if' statement: Expected 'end'"); 
            } 
        } 
    }
    
    private void skipUntilMatchingEnd() throws Exception { 
        int depth = 1; 
        while (depth > 0) { 
            Token t = consume(); 
            if (t.type == IF || t.type == WHILE || t.type == FUNCTION || t.type == FOR) { 
                depth++; 
            } else if (t.type == END) { 
                depth--; 
            } else if (t.type == EOF) { 
                throw new Exception("Unmatched 'if' statement: Expected 'end'"); 
            } 
        } 
        tokenIndex--; 
    }
    
    private boolean isTruthy(Object value) { 
        if (value == null || value == LUA_NIL) { 
            return false; 
        } 
        if (value instanceof Boolean) { 
            return ((Boolean) value).booleanValue(); 
        } 
        return true; 
    }
    
    private Object[] resolveTableAndKey(String varName, Hashtable<String, Object> scope) throws Exception {
        Object table = unwrap(scope.get(varName));
        if (table == null && globals.containsKey(varName)) table = unwrap(globals.get(varName));
        Object key = null;
    
        while (peek().type == DOT || peek().type == LBRACKET) {
            if (peek().type == DOT) { 
                consume(DOT); 
                Token field = consume(IDENTIFIER); 
                key = field.value; 
            } 
            else if (peek().type == LBRACKET) { 
                consume(LBRACKET); 
                key = expression(scope); 
                consume(RBRACKET); 
            }

            if (table == null) { 
                throw new Exception("attempt to index a nil value"); 
            }
            if (!(table instanceof Hashtable)) { 
                throw new Exception("attempt to index a non-table value"); 
            }
            if (peek().type == DOT || peek().type == LBRACKET) { 
                table = unwrap(((Hashtable<Object, Object>) table).get(key)); 
            }
        }
        return new Object[]{table, key};
    }
    
    private void collectLabels() throws Exception {
        int savedTokenIndex = tokenIndex;
        if (labels.isEmpty()) { } else { 
            labels.clear(); 
        }

        tokenIndex = 0;
        while (peek().type != EOF) {
            Token token = peek();

            if (token.type == LABEL) { 
                consume(LABEL); 
                labels.put((String) token.value, tokenIndex); 
            }
            else { 
                consume(); 
            }
        }

        tokenIndex = savedTokenIndex;
    }
    
    private static boolean isWhitespace(char c) { 
        return c == ' ' || c == '\t' || c == '\n' || c == '\r'; 
    }
    private static boolean isDigit(char c) { 
        return c >= '0' && c <= '9'; 
    }
    private static boolean isLetter(char c) { 
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; 
    }
    private static boolean isLetterOrDigit(char c) { 
        return isLetter(c) || isDigit(c); 
    }
    
    public Object getKernel() { 
        return new LuaFunction(KERNEL); 
    }
    
    public static String type(Object item) { 
        return item == null || item == LUA_NIL ? "nil" : 
            item instanceof String ? "string" : 
            item instanceof Double ? "number" : 
            item instanceof Boolean ? "boolean" : 
            item instanceof LuaFunction ? "function" : 
            item instanceof Hashtable ? "table" : 
            item instanceof InputStream || item instanceof OutputStream || 
            item instanceof StringBuffer || item instanceof JTextArea ? "stream" : 
            item instanceof JFrame || item instanceof JDialog || 
            item instanceof JPanel || item instanceof JOptionPane ? "screen" : 
            item instanceof ImageIcon ? "image" : 
            item instanceof AbstractButton ? "button" : 
            "userdata"; 
    }
    
    public static Vector<Object> toVector(Hashtable<Object, Object> table) throws Exception { 
        Vector<Object> vec = new Vector<>(); 
        if (table == null) { 
            return vec; 
        } 
        for (int i = 1; i <= table.size(); i++) { 
            vec.addElement(table.get((double) i)); 
        } 
        return vec; 
    }
    
    public static boolean isListTable(Hashtable<Object, Object> table) {
        if (table == null) { return false; }
        else if (table.isEmpty()) { return true; }

        int size = table.size();
        for (int i = 1; i <= size; i++) {
            if (!table.containsKey((double) i)) { return false; }
        }
        for (Enumeration<Object> e = table.keys(); e.hasMoreElements();) {
            Object key = e.nextElement();
            if (!(key instanceof Double)) { return false; }
            double d = ((Double) key).doubleValue();
            if (d != Math.floor(d) || d < 1 || d > size) { return false; }
        }
        return true;
    }
    
    // LuaFunction Inner Class
    public class LuaFunction implements Runnable {
        private Vector<String> params;
        private Vector<Token> bodyTokens;
        private Hashtable<String, Object> closureScope;
        private int MOD = -1;
        private Object root = null;
        private String handler = "";
        private Hashtable<Object, Object> cmds = null;
        
        LuaFunction(Vector<String> params, Vector<Token> bodyTokens, Hashtable<String, Object> closureScope) { 
            this.params = params; 
            this.bodyTokens = bodyTokens; 
            this.closureScope = closureScope; 
        }
                
        LuaFunction(String handler, Object root) { 
            this.handler = handler; 
            this.root = root; 
        }
        
        LuaFunction(Hashtable<Object, Object> cmds) { 
            this.cmds = cmds; 
        }
        
        LuaFunction(LuaFunction root) { 
            this.root = root; 
        }
        
        LuaFunction(int type) { 
            this.MOD = type; 
        }
        
        public Object call(Vector<Object> args) throws Exception {
            if (MOD != -1) { 
                return internals(args); 
            }

            Hashtable<String, Object> functionScope = new Hashtable<>();
            // Usar Enumeration<String> ao invés de Enumeration<Object>
            for (Enumeration<String> e = closureScope.keys(); e.hasMoreElements();) { 
                String key = e.nextElement(); 
                functionScope.put(key, unwrap(closureScope.get(key))); 
            }
            for (Enumeration<String> e = globals.keys(); e.hasMoreElements();) { 
                String key = e.nextElement(); 
                if (!functionScope.containsKey(key)) { 
                    functionScope.put(key, unwrap(globals.get(key))); 
                } 
            }

            int paramCount = params.size();
            boolean hasVararg = paramCount > 0 && params.elementAt(paramCount - 1).equals("...");
            int fixedParamCount = hasVararg ? paramCount - 1 : paramCount;
            for (int i = 0; i < fixedParamCount; i++) {
                String paramName = params.elementAt(i);
                Object argValue = (i < args.size()) ? args.elementAt(i) : null; 
                functionScope.put(paramName, argValue == null ? LUA_NIL : argValue);
            }
            if (hasVararg) {
                Hashtable<Object, Object> varargValues = new Hashtable<>();
                int index = 1;
                for (int i = fixedParamCount; i < args.size(); i++) { 
                    Object obj = args.elementAt(i); 
                    varargValues.put((double) index++, obj == null ? LUA_NIL : obj); 
                }
                functionScope.put("...", varargValues);
            }

            int originalTokenIndex = tokenIndex;
            Vector<Token> originalTokens = tokens;

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
        
        public Object internals(Vector<Object> args) throws Exception {
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
                                Vector<Object> vv = (Vector<Object>) a;
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
                    break;
                    
                case ERROR: 
                    String msg = toLuaString((args.size() > 0) ? args.elementAt(0) : null); 
                    throw new Exception(msg.equals("nil") ? "error" : msg);
                    

                case PCALL:
                    if (args.isEmpty()) { 
                        return gotbad(1, "pcall", "function expected"); 
                    }
                    else {
                        Vector<Object> result = new Vector<>();
                        Vector<Object> fnArgs = new Vector<>();

                        if (args.elementAt(0) instanceof LuaFunction) {
                            LuaFunction func = (LuaFunction) unwrap(args.elementAt(0));
                            for (int i = 1; i < args.size(); i++) { 
                                fnArgs.addElement(unwrap(args.elementAt(i))); 
                            }

                            try { 
                                Object value = func.call(fnArgs); 
                                result.addElement(TRUE); 

                                if (value instanceof Vector) { 
                                    Vector<Object> v = (Vector<Object>) value; 
                                    for (int i = 0; i < v.size(); i++) { 
                                        result.addElement(v.elementAt(i)); 
                                    }
                                }
                                else { 
                                    result.addElement(value); 
                                }
                            }
                            catch (Exception e) { 
                                result.addElement(FALSE); 
                                result.addElement(midlet.getCatch(e)); 
                            }
                        }
                        else { 
                            result.addElement(FALSE); 
                            result.addElement("attempt to call a " + type(args.elementAt(0)) + " value"); 
                        } 

                        return result;
                    }
                    
                case REQUIRE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "require", "string expected, got no value"); 
                    }
                    else if (args.elementAt(0) instanceof String) {
                        String name = toLuaString(args.elementAt(0));

                        Object cached = requireCache.get(name);
                        if (cached != null) { 
                            return (cached == LUA_NIL) ? null : cached; 
                        }

                        String code = midlet.getcontent(name, father);
                        if (code.equals("")) { 
                            if ((code = midlet.getcontent("/lib/" + name + ".lua", father)).equals("")) { 
                                if ((code = midlet.getcontent("/lib/" + name + ".so", father)).equals("")) { 
                                    throw new Exception("module '" + code + "' not found"); 
                                } 
                            } 
                        } 

                        Object obj = exec(code, null);
                        requireCache.put(name, (obj == null) ? LUA_NIL : obj);
                        return obj;
                    } 
                    else { 
                        return gotbad(1, "require", "string expected, got " + type(args.elementAt(0))); 
                    }
                    
                case LOADS: 
                    if (args.isEmpty() || args.elementAt(0) == null) { 
                        break; 
                    } 
                    else { 
                        return exec(toLuaString(args.elementAt(0)), args.size() > 1 ? (args.elementAt(1) instanceof Hashtable ? (Hashtable<String, Object>) args.elementAt(1) : null) : null); 
                    }
                    
                case PAIRS: 
                    if (args.isEmpty()) { 
                        return gotbad(1, "pairs", "table expected, got no value"); 
                    } 
                    else {
                        Object t = args.elementAt(0);
                        t = (t == LUA_NIL) ? null : t;
                        if (t == null || t instanceof Hashtable || t instanceof Vector) { 
                            return t; 
                        }
                        else { 
                            return gotbad(1, "pairs", "table expected, got " + type(t)); 
                        }
                    }
                    
                case IPAIRS:
                    if (args.isEmpty()) { 
                        return gotbad(1, "ipairs", "table expected, got no value"); 
                    } 
                    else {
                        Object t = args.elementAt(0);
                        t = (t == LUA_NIL) ? null : t;
                        
                        if (t == null || t instanceof Hashtable || t instanceof Vector) {
                            Hashtable<Object, Object> iterator = new Hashtable<>();
                            iterator.put("__table", t); 
                            iterator.put("__index", 0.0);
                            return iterator;
                        } else { 
                            return gotbad(1, "ipairs", "table expected, got " + type(t)); 
                        }
                    }

                case GC:
                    if (args.isEmpty()) { 
                        System.gc(); 
                        break; 
                    }
                    else {
                        String opt = toLuaString(args.elementAt(0));

                        if (opt.equals("stop")) { 
                            gc = false; 
                        }
                        else if (opt.equals("collect") || opt.equals("restart")) { 
                            System.gc(); 
                        }
                        else if (opt.equals("free")) { 
                            return (double) (midlet.runtime.totalMemory() / 1024); 
                        }
                        else if (opt.equals("total")) { 
                            return (double) (midlet.runtime.freeMemory() / 1024); 
                        }
                        else if (opt.equals("count")) { 
                            return (double) ((midlet.runtime.totalMemory() - midlet.runtime.freeMemory()) / 1024); 
                        }
                        else if (opt.equals("step")) { 
                            return FALSE; 
                        }
                        else if (opt.equals("isrunning")) { 
                            return gc; 
                        }
                        else if (opt.equals("generational") || opt.equals("incremental")) { 
                            return "generational"; 
                        }
                        else { 
                            return gotbad(1, "collectgarbage", "invalid option '" + opt + "'"); 
                        }
                    }    
                

                case TOSTRING: 
                    return toLuaString(args.isEmpty() ? gotbad(1, "tostring", "value expected") : args.elementAt(0));
                    
                case TONUMBER: 
                    return args.isEmpty() ? gotbad(1, "tonumber", "value expected") : Double.valueOf(toLuaString(args.elementAt(0)));
                    
                case SELECT:
                    if (args.isEmpty() || args.elementAt(0) == null) { 
                        return gotbad(1, "select", "number expected, got no value"); 
                    } 
                    else {
                        String idx = toLuaString(args.elementAt(0));
                        if (idx.equals("#")) {
                            if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) { 
                                return (double) ((Hashtable<?, ?>) args.elementAt(1)).size(); 
                            } 
                            else { 
                                return (double) (args.size() - 1); 
                            }
                        } else {
                            if (args.size() == 1) { 
                                return null; 
                            }

                            int index = 1;
                            try { 
                                index = Integer.parseInt(idx); 
                            } 
                            catch (NumberFormatException e) { 
                                return gotbad(1, "select", "number expected, got " + type(args.elementAt(0))); 
                            }
                            
                            Hashtable<Object, Object> result = new Hashtable<>();
                            if (args.size() > 1 && args.elementAt(1) instanceof Hashtable) {
                                Hashtable<Object, Object> varargTable = (Hashtable<Object, Object>) args.elementAt(1);
                                int varargSize = varargTable.size();
                                if (index < 0) { 
                                    index = varargSize + index + 1; 
                                }
                                if (index < 1 || index > varargSize) { 
                                    return null; 
                                }

                                int resultIndex = 1;
                                for (int i = index; i <= varargSize; i++) {
                                    Object val = varargTable.get((double) i);
                                    if (val != null) { 
                                        result.put((double) resultIndex++, val); 
                                    }
                                }
                            } else {
                                int argCount = args.size() - 1;
                                if (index < 0) { 
                                    index = argCount + index + 1; 
                                }
                                if (index < 1 || index > argCount) { 
                                    return null; 
                                }

                                int resultIndex = 1;
                                for (int i = index; i <= argCount; i++) {
                                    Object val = args.elementAt(i);
                                    result.put((double) resultIndex++, val == null ? LUA_NIL : val);
                                }
                            }
                            return result;
                        }
                    }
                    
                case TYPE: 
                    return args.isEmpty() ? gotbad(1, "type", "value expected") : type(args.elementAt(0));
                    
                case GETPROPERTY: 
                    if (args.isEmpty()) { 
                        break; 
                    } else { 
                        String query = toLuaString(args.elementAt(0)); 
                        return query.startsWith("/") ? System.getProperty(query.substring(1)) : System.getProperty(query); 
                    }
                    
                case RANDOM: 
                    Double gen = (double) midlet.random.nextInt(getNumber(args.isEmpty() ? "100" : toLuaString(args.elementAt(0)), 100)); 
                    return args.isEmpty() ? gen / 100 : gen;
                    
                case SETMETATABLE:
                    if (args.size() < 2) { 
                        return gotbad(1, "setmetatable", "table expected, got no value"); 
                    }
                    else {
                        Object table = unwrap(args.elementAt(0));
                        Object mt = unwrap(args.elementAt(1));
                    
                        if (!(table instanceof Hashtable))
                            return gotbad(1, "setmetatable", "table expected, got " + type(table));
                        if (mt != null && !(mt instanceof Hashtable))
                            return gotbad(2, "setmetatable", "nil or table expected, got " + type(mt));
                    
                        ((Hashtable<Object, Object>) table).put("__metatable", mt == null ? LUA_NIL : mt);
                        return table;
                    }
                    
                case GETMETATABLE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "getmetatable", "table expected, got no value"); 
                    }
                    else {
                        Object table = unwrap(args.elementAt(0));
                        if (!(table instanceof Hashtable))
                            return gotbad(1, "getmetatable", "table expected, got " + type(table));
                    
                        Object mt = ((Hashtable<Object, Object>) table).get("__metatable");
                        return (mt == LUA_NIL || mt == null) ? null : mt;
                    }
                    
                // Package [os]
                case EXEC: 
                    return exec(args);
                    
                case GETENV: 
                    return args.isEmpty() ? midlet.attributes : midlet.attributes.get(toLuaString(args.elementAt(0)));
                    
                case SETENV: 
                    if (args.isEmpty()) { } 
                    else {
                        Object value = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;

                        if (value == null) { 
                            midlet.attributes.remove(toLuaString(args.elementAt(0))); 
                        }
                        else { 
                            midlet.attributes.put(toLuaString(args.elementAt(0)), value); 
                        }
                        break;
                    }
                    
                case CLOCK: 
                    return (double) (System.currentTimeMillis() - uptime);
                    
                case SETLOC: 
                    if (args.isEmpty()) { 
                        break; 
                    } else { 
                        midlet.attributes.put("LOCALE", toLuaString(args.elementAt(0))); 
                    }
                    
                case EXIT: 
                    exit(args);
                    
                case DATE: 
                    return new java.util.Date().toString();
                    
                case GETPID: 
                    return args.isEmpty() || args.elementAt(0) == null ? PID : midlet.getpid(toLuaString(args.elementAt(0)));
                    
                case GETPROC:
                    if (args.isEmpty()) {
                        Hashtable<Object, Object> result = new Hashtable<>();
                        for (Enumeration<String> procs = midlet.sys.keys(); procs.hasMoreElements();) {
                            String pid = procs.nextElement();
                            result.put(pid, ((Process) midlet.sys.get(pid)).name);
                        }
                        return result;
                    }
                    else {
                        String pid = toLuaString(args.elementAt(0)).trim();
                        Process process = (Process) midlet.sys.get(pid);

                        if (process != null) {
                            if (process.uid != id && id != 0) { 
                                return gotbad(1, "getproc", "permission denied"); 
                            }

                            if (args.size() > 1) { 
                                return process.db.get(toLuaString(args.elementAt(1)).trim()); 
                            } 
                            else { 
                                return gotbad(2, "getproc", "field expected, got no value"); 
                            }
                        } 
                    }
                    
                case SETPROC:
                    if (args.isEmpty()) { }
                    else if (args.elementAt(0) instanceof Boolean) { 
                        kill = ((Boolean) args.elementAt(0)).booleanValue(); 
                    }
                    else {
                        String attribute = toLuaString(args.elementAt(0)).trim().toLowerCase();
                        Object value = args.size() < 2 ? null : args.elementAt(1);

                        if (attribute.equals("owner")) { 
                            return gotbad(1, "setproc", "permission denied"); 
                        } 
                        else if (attribute.equals("scope")) { 
                            if (value instanceof Hashtable) { 
                                proc.scope = (Hashtable<String, Object>) value; 
                            } else { 
                                return gotbad(1, "setproc", "table expected"); 
                            } 
                        }
                        else if (attribute.equals("name")) { 
                            if (value != null) { 
                                proc.name = toLuaString(value); 
                            } else { 
                                return gotbad(1, "setproc", "string expected"); 
                            } 
                        }
                        else if (attribute.equals("handler")) { 
                            if (value instanceof LuaFunction) { 
                                proc.handler = value; 
                                kill = false; 
                            } else { 
                                return gotbad(1, "setproc", "function expected"); 
                            } 
                        }
                        else if (attribute.equals("cmd")) { 
                            if (value != null) { 
                                proc.cmd = toLuaString(value); 
                            } else { 
                                return gotbad(1, "setproc", "string expected"); 
                            } 
                        }
                        else if (attribute.equals("sighandler")) { 
                            if (value instanceof LuaFunction) { 
                                proc.sighandler = value; 
                            } else { 
                                return gotbad(1, "setproc", "function expected"); 
                            } 
                        }
                        else { 
                            if (value == null) { 
                                proc.db.remove(attribute); 
                            } else { 
                                proc.db.put(attribute, value); 
                            } 
                        }
                    }
                    
                case GETCWD: 
                    return father.get("PWD");
                    
                case REQUEST:
                    if (args.isEmpty()) { 
                        return gotbad(1, "request", "string expected, got no value"); 
                    }
                    else if (args.size() < 2) { 
                        return gotbad(2, "request", "value expected, got no value"); 
                    }
                    else if (midlet.sys.containsKey(toLuaString(args.elementAt(0)))) {
                        Process process = (Process) midlet.sys.get(toLuaString(args.elementAt(0)));
                        if (process.lua != null && process.handler != null) {
                            Lua lua = process.lua;
                            Vector<Object> argx = new Vector<>(); 
                            argx.addElement(toLuaString(args.elementAt(1))); 
                            argx.addElement(args.size() > 2 ? args.elementAt(2) : null); 
                            argx.addElement(father); 
                            argx.addElement(PID); 
                            argx.addElement((double) id);
                            Object response = null;

                            try { 
                                response = ((LuaFunction) process.handler).call(argx); 
                            }
                            catch (Exception e) { 
                                return midlet.getCatch(e); 
                            } 
                            catch (Error e) { 
                                if (e.getMessage() != null) { 
                                    midlet.print(e.getMessage(), stdout, id, father); 
                                } 
                                return (double) lua.status; 
                            }

                            return response;
                        } 
                        else { 
                            return gotbad(1, "request", "not a service"); 
                        }
                    } 
                    else { 
                        return gotbad(1, "request", "process not found"); 
                    }
                    
                case GETUID: 
                    if (args.isEmpty() || args.elementAt(0) == null) { 
                        return (double) id; 
                    } 
                    return (double) midlet.getUserID(toLuaString(args.elementAt(0)));
                    
                case CHDIR: 
                    return chdir(args);
                    
                case SU:
                    if (args.isEmpty()) { 
                        return gotbad(1, "su", "username and password expected"); 
                    } 
                    else {
                        String user = toLuaString(args.elementAt(0)), query = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;
                        if (user.equals(midlet.username)) { 
                            id = 1000; 
                            father.put("USER", user); 
                            proc.uid = 1000; 
                            return 0.0; 
                        }
                        else if (midlet.userID.containsKey(user)) { 
                            id = midlet.getUserID(user); 
                            father.put("USER", user); 
                            proc.uid = id; 
                            return 0.0; 
                        }
                        else if (query == null) { 
                            return gotbad(2, "su", "string expected, got nil"); 
                        }
                        else if (user.equals("root") && midlet.passwd(query)) { 
                            id = 0; 
                            father.put("USER", "root"); 
                            proc.uid = 0; 
                            return 0.0; 
                        }
                        else { 
                            return 13.0; 
                        }
                    }
                    
                case REMOVE: 
                    return args.isEmpty() ? gotbad(1, "remove", "string expected, got no value") : (double) midlet.deleteFile(toLuaString(args.elementAt(0)), id, father);
                    
                case SCOPE:
                    if (args.isEmpty()) { 
                        return father; 
                    }
                    else {
                        if (args.elementAt(0) instanceof Hashtable) { 
                            father = (Hashtable<String, Object>) args.elementAt(0);

                            if (father.containsKey("USER")) {
                                String user = (String) father.get("USER");
                                if (user.equals("root")) {
                                    if (id == 1000) {
                                        father.put("USER", midlet.username);
                                    }
                                }
                            }
                            break;
                        }
                        else { 
                            return gotbad(1, "scope", "table expected, got " + type(args.elementAt(0))); 
                        }
                    }
                    
                case JOIN: 
                    return args.isEmpty() ? gotbad(1, "join", "string expected, got no value") : midlet.joinpath(toLuaString(args.elementAt(0)), father);
                    
                case MKDIR:
                    if (args.isEmpty()) { }
                    else {
                        String dir = toLuaString(args.elementAt(0));

                        if (!dir.equals("/mnt/") && dir.startsWith("/mnt/")) {
                            try {
                                File file = new File(dir.substring(5));
                                if (file.exists()) { 
                                    return 128.0; 
                                } else { 
                                    file.mkdirs(); 
                                    return 0.0; 
                                }
                            }
                            catch (Exception e) { 
                                return e instanceof SecurityException ? 13.0 : 1.0; 
                            }
                        } else { 
                            return 5.0; 
                        }
                    }
                    
                // Package [io]
                case READ:
                    if (args.isEmpty()) { 
                        return stdout instanceof JTextArea ? ((JTextArea) stdout).getText() : 
                            stdout instanceof StringBuffer ? ((StringBuffer) stdout).toString() : 
                            stdout instanceof String ? midlet.getcontent((String) stdout, father) : ""; 
                    }
                    else {
                        arg = args.elementAt(0);

                        if (arg instanceof InputStream) { 
                            return midlet.read((InputStream) arg, args.size() > 1 && args.elementAt(1) instanceof Double ? ((Double) args.elementAt(1)).intValue() : 1024, false); 
                        }
                        else if (arg instanceof StringBuffer) { 
                            return ((StringBuffer) arg).toString(); 
                        }
                        else if (arg instanceof OutputStream) { 
                            return gotbad(1, "read", "input stream expected, got output"); 
                        } 
                        else { 
                            return midlet.getcontent(toLuaString(arg), father); 
                        } 
                    }
                    
                case WRITE:
                    if (args.isEmpty()) { 
                        break; 
                    }
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
                                outputStream.write(toLuaString(buffer).getBytes(StandardCharsets.UTF_8));
                            }
                            outputStream.flush();
                            return 0.0;
                        }
                        else if (buffer instanceof OutputStream) {
                            OutputStream outputStream = (OutputStream) buffer;

                            if (target instanceof ByteArrayOutputStream) {
                                ByteArrayOutputStream baos = (ByteArrayOutputStream) target;
                                byte[] bytes = baos.toByteArray();
                                outputStream.write(bytes);
                            } else {
                                outputStream.write(toLuaString(target).getBytes(StandardCharsets.UTF_8));
                            }
                            outputStream.flush();
                            return 0.0;
                        }
                        else if (target instanceof StringBuffer) { 
                            StringBuffer sb = (StringBuffer) target; 
                            String content = toLuaString(buffer); 
                            sb.append(content); 
                            return 0.0; 
                        }
                        else if (buffer instanceof ByteArrayOutputStream) {
                            ByteArrayOutputStream baos = (ByteArrayOutputStream) buffer;
                            byte[] bytes = baos.toByteArray();
                            String filename = target != null ? toLuaString(target) : "/dev/stdout";
                            if (mode) { 
                                return (double) midlet.write(filename, midlet.getcontent(filename, father) + new String(bytes, StandardCharsets.UTF_8), id, father); 
                            }
                            else { 
                                return (double) midlet.write(filename, bytes, id, father); 
                            }
                        }
                        else {
                            String content = toLuaString(buffer), filename = target != null ? toLuaString(target) : "/dev/stdout";
                            return (double) midlet.write(filename, mode ? midlet.getcontent(filename, father) + content : content, id, father);
                        }
                    }
                    
                case CLOSE:
                    if (args.isEmpty()) { }
                    else {
                        for (int i = 0; i < args.size(); i++) {
                            arg = args.elementAt(i);

                            if (arg instanceof Socket) { 
                                ((Socket) arg).close(); 
                            }
                            else if (arg instanceof ServerSocket) { 
                                ((ServerSocket) arg).close(); 
                            }
                            else if (arg instanceof InputStream) { 
                                ((InputStream) arg).close(); 
                            }
                            else if (arg instanceof OutputStream) { 
                                ((OutputStream) arg).close(); 
                            }
                            else if (arg instanceof StringBuffer) { }
                            else { 
                                return gotbad(i + 1, "close", "stream expected, got " + type(arg)); 
                            }

                            proc.net.remove(arg); break;
                        }
                    }
                    
                case OPEN: 
                    if (args.isEmpty()) { 
                        return new ByteArrayOutputStream(); 
                    } 
                    else { 
                        try { 
                            return midlet.getInputStream(toLuaString(args.elementAt(0)), father); 
                        } catch (Exception e) { 
                            return null; 
                        } 
                    }
                    
                case POPEN: 
                    return popen(args);
                    
                case DIRS: 
                    return dirs(args);
                    
                case SETOUT: 
                    if (args.isEmpty()) { } 
                    else { 
                        stdout = args.elementAt(0); 
                    }
                    
                case MOUNT:
                    if (args.isEmpty()) { 
                        break; 
                    }
                    else {
                        String struct = toLuaString(args.elementAt(0));
                        
                        if (struct == null || struct.length() == 0) { 
                            midlet.fs.clear(); 
                        } 
                        String[] lines = midlet.split(struct, '\n'); 
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i].trim(); 
                            int div = line.indexOf('='); 
                            if (line.startsWith("#") || line.length() == 0 || div == -1) { 
                                continue; 
                            } 
                            else { 
                                String base = line.substring(0, div).trim(); 
                                String[] files = midlet.split(line.substring(div + 1).trim(), ','); 
                                Vector<Object> content = new Vector<>(); 
                                content.addElement(".."); 
                                for (int j = 0; j < files.length; j++) { 
                                    if (!content.contains(files[j])) { 
                                        if (files[j].endsWith("/")) { 
                                            Vector<Object> dir = new Vector<>(); 
                                            dir.addElement(".."); 
                                            midlet.fs.put(base + files[j], dir); 
                                        } 
                                        content.addElement(files[j]);
                                    }
                                } 
                                midlet.fs.put(base, content);
                            } 
                        }
                        break;
                    }
                    
                case GEN: 
                    return new StringBuffer();
                    
                case COPY:
                    if (args.size() < 2) { 
                        return gotbad(1, "copy", "wrong number of arguments"); 
                    }
                
                    Object source = args.elementAt(0), target = args.elementAt(1);

                    if (source instanceof InputStream) {
                        InputStream in = (InputStream) source;

                        if (target instanceof OutputStream) {
                            OutputStream os = (OutputStream) target;
                            
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) { 
                                os.write(buffer, 0, bytesRead); 
                            }
                            os.flush();
                            return 0.0;
                        }
                        else if (target instanceof StringBuffer || target instanceof String) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) { 
                                baos.write(buffer, 0, bytesRead); 
                            }

                            byte[] data = baos.toByteArray(); 
                            baos.close();    
                        
                            if (target instanceof StringBuffer) { 
                                ((StringBuffer) target).append(new String(data, StandardCharsets.UTF_8)); 
                                return 0.0; 
                            }
                            else { 
                                return (double) midlet.write(toLuaString(target), data, id, father); 
                            }
                        }
                    }
                    else if (source instanceof StringBuffer) {
                        StringBuffer in = (StringBuffer) source;

                        if (target instanceof OutputStream) {
                            OutputStream os = (OutputStream) target;
                            
                            os.write(in.toString().getBytes(StandardCharsets.UTF_8)); 
                            os.flush();
                            return 0.0;
                        }
                        else if (target instanceof StringBuffer || target instanceof String) {
                            if (target instanceof StringBuffer) { 
                                ((StringBuffer) target).append(in.toString()); 
                                return 0.0; 
                            }
                            else { 
                                return (double) midlet.write(toLuaString(target), in.toString().getBytes(StandardCharsets.UTF_8), id, father); 
                            }
                        }
                    }
                    else if (source instanceof String) {
                        String file = (String) source;

                        if (target instanceof OutputStream) {
                            OutputStream os = (OutputStream) target;
                            
                            InputStream is = midlet.getInputStream(file, father);
                            if (is == null) { 
                                return 127.0; 
                            }
                            
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) { 
                                os.write(buffer, 0, bytesRead); 
                            }
                            os.flush(); 
                            is.close();
                            return 0.0;
                        }
                        else if (target instanceof StringBuffer || target instanceof String) {
                            if (target instanceof StringBuffer) { 
                                ((StringBuffer) target).append(midlet.read(file, father)); 
                            }
                            else { 
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                                InputStream is = midlet.getInputStream(file, father);
                                if (is == null) { 
                                    return 127.0; 
                                }

                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = is.read(buffer)) != -1) { 
                                    baos.write(buffer, 0, bytesRead); 
                                }

                                return (double) midlet.write(toLuaString(target), baos.toByteArray(), id, father);
                            }
                        }
                    }
                    else if (source instanceof ByteArrayOutputStream) {
                        ByteArrayOutputStream baos = (ByteArrayOutputStream) source;
                        byte[] data = baos.toByteArray();
                        
                        if (target instanceof OutputStream) { 
                            OutputStream os = (OutputStream) target; 
                            os.write(data); 
                            os.flush(); 
                        } 
                        else if (target instanceof StringBuffer) { 
                            ((StringBuffer) target).append(new String(data, StandardCharsets.UTF_8)); 
                        } 
                        else if (target instanceof String) { 
                            return (double) midlet.write(toLuaString(target), data, id, father); 
                        }
                    }

                    return 0.0;
                    
                // Package [table]
                case TB_INSERT:
                    if (args.size() < 2) { 
                        return gotbad(1, "insert", "wrong number of arguments"); 
                    }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tObj;
                            if (isListTable(table)) {
                                int pos = table.size() + 1;
                                Object value = unwrap(args.elementAt(1));
                                if (args.size() >= 3) {
                                    Object posObj = unwrap(args.elementAt(2));
                                    if (!(posObj instanceof Double)) { 
                                        return gotbad(3, "insert", "number expected, got " + type(posObj)); 
                                    }
                                    pos = ((Double) posObj).intValue();
                                    if (pos < 0 || pos > table.size() + 1) { 
                                        return gotbad(3, "insert", "position out of bounds"); 
                                    }
                                }
                                for (int i = table.size(); i >= pos; i--) {
                                    Object val = table.get((double) i);
                                    if (val != null) { 
                                        table.put((double) (i + 1), val); 
                                    }
                                    else { 
                                        table.remove((double) i); 
                                    }
                                }
                                table.put((double) pos, value == null ? LUA_NIL : value);
                                return null;
                            } else { 
                                return gotbad(1, "insert", "table must be array-like"); 
                            }
                        } else { 
                            return gotbad(1, "insert", "table expected, got " + type(tObj)); 
                        }
                    }
                    
                case TB_CONCAT:
                    if (args.isEmpty()) { 
                        return gotbad(1, "concat", "table expected, got no value"); 
                    }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tObj;
                            if (isListTable(table)) {
                                Vector<Object> list = toVector(table);
                                String sep = args.size() > 1 ? toLuaString(unwrap(args.elementAt(1))) : "";
                                int i = args.size() > 2 ? ((Double) unwrap(args.elementAt(2))).intValue() : 1;
                                int j = args.size() > 3 ? ((Double) unwrap(args.elementAt(3))).intValue() : list.size();
                                if (i < 1 || j > list.size() || i > j) { 
                                    return ""; 
                                }
                                StringBuffer sb = new StringBuffer();
                                for (int k = i - 1; k < j; k++) {
                                    sb.append(toLuaString(list.elementAt(k)));
                                    if (k < j - 1) sb.append(sep);
                                }
                                return sb.toString();
                            } else { 
                                return gotbad(1, "concat", "table must be array-like"); 
                            }
                        } else { 
                            return gotbad(1, "concat", "table expected, got " + type(tObj)); 
                        }
                    }
                    
                case TB_REMOVE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "remove", "table expected, got no value"); 
                    }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tObj;
                            if (isListTable(table)) {
                                int pos = table.size();
                                if (args.size() >= 2) {
                                    Object posObj = unwrap(args.elementAt(1));
                                    if (!(posObj instanceof Double)) { 
                                        return gotbad(2, "remove", "number expected, got " + type(posObj)); 
                                    }
                                    pos = ((Double) posObj).intValue();
                                    if (pos < 1 || pos > table.size()) { 
                                        return gotbad(2, "remove", "position out of bounds"); 
                                    }
                                }
                                Object removed = table.get((double) pos);
                                if (removed != null) {
                                    table.remove((double) pos);
                                    for (int i = pos; i < table.size(); i++) {
                                        Object val = table.get((double) (i + 1));
                                        if (val != null) {
                                            table.put((double) i, val);
                                        } else {
                                            table.remove((double) i);
                                        }
                                    }
                                    if (table.containsKey((double) table.size())) {
                                        table.remove((double) table.size());
                                    }
                                }
                                return removed == null ? LUA_NIL : removed;
                            } else { 
                                return gotbad(1, "remove", "table must be array-like"); 
                            }
                        } else { 
                            return gotbad(1, "remove", "table expected, got " + type(tObj)); 
                        }
                    }
                    
                case TB_SORT:
                    if (args.isEmpty()) { 
                        return gotbad(1, "sort", "table expected, got no value"); 
                    }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tObj;
                            if (isListTable(table)) {
                                Vector<Object> list = toVector(table);
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
                                table.clear();
                                for (int i = 0; i < list.size(); i++) {
                                    table.put((double) (i + 1), list.elementAt(i));
                                }
                                return null;
                            } else { 
                                return gotbad(1, "sort", "table must be array-like"); 
                            }
                        } else { 
                            return gotbad(1, "sort", "table expected, got " + type(tObj)); 
                        }
                    }
                    
                case TB_MOVE:
                    if (args.size() < 4) { 
                        return gotbad(1, "move", "insufficient arguments (need table, from, to, len)"); 
                    }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tObj;
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
                                Vector<Object> list = toVector(table);
                                Vector<Object> slice = new Vector<>();
                                for (int i = 0; i < len; i++) {
                                    int idx = from + i - 1;
                                    if (idx >= 0 && idx < list.size()) {
                                        slice.addElement(list.elementAt(idx));
                                    }
                                }
                                for (int i = from + len - 1; i >= from; i--) {
                                    if (i - 1 >= 0 && i - 1 < list.size()) {
                                        list.removeElementAt(i - 1);
                                    }
                                }
                                for (int i = 0; i < slice.size(); i++) {
                                    list.insertElementAt(slice.elementAt(i), to + i - 1);
                                }
                                table.clear();
                                for (int i = 0; i < list.size(); i++) {
                                    table.put((double) (i + 1), list.elementAt(i));
                                }
                                return table;
                            } else { 
                                return gotbad(1, "move", "table must be array-like"); 
                            }
                        } else { 
                            return gotbad(1, "move", "table expected, got " + type(tObj)); 
                        }
                    }
                    
                case TB_UNPACK:
                    if (args.isEmpty()) { 
                        return gotbad(1, "unpack", "table expected, got no value"); 
                    }
                    else {
                        Object tObj = unwrap(args.elementAt(0));
                        if (tObj instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) tObj;
                            if (isListTable(table)) {
                                Vector<Object> list = toVector(table);
                                int i = args.size() > 1 ? ((Double) unwrap(args.elementAt(1))).intValue() : 1;
                                int j = args.size() > 2 ? ((Double) unwrap(args.elementAt(2))).intValue() : list.size();
                                if (i < 1 || j > list.size() || i > j) { 
                                    return new Vector<Object>(); 
                                }
                                Vector<Object> result = new Vector<>();
                                for (int k = i - 1; k < j; k++) {
                                    result.addElement(list.elementAt(k));
                                }
                                return result;
                            } else { 
                                return gotbad(1, "unpack", "table must be array-like"); 
                            }
                        } else { 
                            return gotbad(1, "unpack", "table expected, got " + type(tObj)); 
                        }
                    }
                    
                case TB_DECODE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "decode", "string expected, got no value"); 
                    }
                    else {
                        String text = toLuaString(args.elementAt(0));
                        if (text.equals("")) { 
                            return new Hashtable<Object, Object>(); 
                        }
                        Hashtable<Object, Object> properties = new Hashtable<>();

                        String[] lines = midlet.split(text, '\n');
                        for (int i = 0; i < lines.length; i++) {
                            String line = lines[i];
                            if (line.startsWith("#")) { }
                            else { 
                                int equalIndex = line.indexOf('='); 
                                if (equalIndex > 0 && equalIndex < line.length() - 1) { 
                                    properties.put(line.substring(0, equalIndex).trim(), midlet.getpattern(line.substring(equalIndex + 1).trim())); 
                                } 
                            }
                        }
                        return properties;
                    }
                    
                case TB_PACK:
                    Hashtable<Object, Object> packed = new Hashtable<>();
                    for (int i = 0; i < args.size(); i++) {
                        Object val = args.elementAt(i);
                        packed.put((double) (i + 1), val == null ? LUA_NIL : val);
                    }
                    packed.put("n", (double) args.size());
                    return packed;
                    
                // Package [base64]
                case BASE64_ENCODE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "encode", "string or table expected, got no value"); 
                    }
                    
                    arg = args.elementAt(0);
                    byte[] data;
                    
                    if (arg instanceof Hashtable) {
                        Hashtable<Object, Object> table = (Hashtable<Object, Object>) arg;
                        if (isListTable(table)) {
                            Vector<Object> vec = toVector(table);
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
                        data = toLuaString(arg).getBytes(StandardCharsets.UTF_8);
                    } else {
                        return gotbad(1, "encode", "string or table expected, got " + type(arg));
                    }
                
                    return Base64.getEncoder().encodeToString(data);
                    
                case BASE64_DECODE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "decode", "string expected, got no value"); 
                    }
                    else {
                        String encoded = toLuaString(args.elementAt(0));
                        byte[] decoded = Base64.getDecoder().decode(encoded);

                        if (args.size() > 1) { 
                            return new ByteArrayInputStream(decoded); 
                        }

                        if (decoded == null) { 
                            return null; 
                        }

                        Hashtable<Object, Object> result = new Hashtable<>();
                        for (int i = 0; i < decoded.length; i++) { 
                            result.put((double) (i + 1), (double) (decoded[i] & 0xFF)); 
                        }
                        return result;
                    }
                    
                // Package [socket] - Java 8 Native Sockets
                case CONNECT:
                    if (args.isEmpty() || args.elementAt(0) == null) { 
                        return gotbad(1, "connect", "string expected, got no value"); 
                    }
                    else {
                        Vector<Object> result = new Vector<>();
                        String url = toLuaString(args.elementAt(0));
                        
                        // Parse URL: host:port
                        String host = url;
                        int port = 80;
                        if (url.contains(":")) {
                            String[] parts = url.split(":");
                            host = parts[0];
                            port = Integer.parseInt(parts[1]);
                        }
                        
                        Socket socket = new Socket(host, port);
                        result.addElement(socket);
                        result.addElement(socket.getInputStream());
                        result.addElement(socket.getOutputStream());
                        result.addElement(url);
                        result.addElement((double) id);
                        proc.net.put(url, result);

                        return result;
                    }
                    
                case PEER:
                case DEVICE:
                    if (args.isEmpty()) { 
                        return gotbad(1, MOD == PEER ? "peer" : "device", "connection expected, got no value"); 
                    }
                    else {
                        if (args.elementAt(0) instanceof Socket) {
                            Socket conn = (Socket) args.elementAt(0);
                            Vector<Object> result = new Vector<>();
                            result.addElement(MOD == PEER ? conn.getInetAddress().getHostAddress() : conn.getLocalAddress().getHostAddress());
                            result.addElement((double) (MOD == PEER ? conn.getPort() : conn.getLocalPort()));
                            return result;
                        } else { 
                            return gotbad(1, MOD == PEER ? "peer" : "device", "connection expected, got " + type(args.elementAt(0))); 
                        }
                    }
                    
                case SERVER:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Double)) { 
                        return gotbad(1, "server" , "number expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); 
                    }
                    else {
                        int port = ((Double) args.elementAt(0)).intValue();
                        ServerSocket server = new ServerSocket(port);
                        midlet.servers.put(String.valueOf(port), server);
                        proc.net.put(String.valueOf(port), server);
                        return server;
                    }
                    
                case ACCEPT:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof ServerSocket)) { 
                        return gotbad(1, "server" , "server expected, got " + (args.isEmpty() ? " no value" : type(args.elementAt(0)))); 
                    }
                    else {
                        Vector<Object> result = new Vector<>();
                        ServerSocket server = (ServerSocket) args.elementAt(0);
                        Socket conn = server.accept();
                        
                        result.addElement(conn);
                        result.addElement(conn.getInputStream());
                        result.addElement(conn.getOutputStream());
                        proc.net.put("socket://:" + server.getLocalPort(), result);

                        return result;
                    }
                    
                // Package [socket.http] - Java 8 HTTP
                case HTTP_GET:
                case HTTP_POST:
                    return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : http(MOD == HTTP_GET ? "GET" : "POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null, false));
                    
                case HTTP_RGET:
                case HTTP_RPOST:
                    return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : http(MOD == HTTP_GET ? "GET" : "POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null, true));
                    
                // Package [graphics] - Swing
                case DISPLAY:
                    if (args.isEmpty()) { }
                    else {
                        Object screen = args.elementAt(0);
                        if (screen instanceof JPanel) {
                            JPanel panel = (JPanel) screen;

                            if (sharedFrame == null) {
                                sharedFrame = new JFrame("OpenTTY");
                                sharedFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                tabs = new JTabbedPane();
                                sharedFrame.getContentPane().add(tabs);
                                sharedFrame.setSize(900, 650);
                            }

                            String tabTitle = midlet.getWindowTitle(panel); // ver função abaixo
                            tabs.addTab(tabTitle, panel);
                            tabs.setSelectedComponent(panel);

                            sharedFrame.pack();
                            sharedFrame.setSize(900, 650);
                            sharedFrame.setVisible(true);
                            sharedFrame.toFront();

                            midlet.frame = sharedFrame;
                        }
                    }
                    break;
                case NEW:
                    if (args.size() < 2) { return gotbad(1, "graphics.new", "wrong number of arguments"); }
                    
                    String newType = toLuaString(args.elementAt(0));
                    String newTitle = args.elementAt(1) == null ? null : toLuaString(args.elementAt(1));
                    Object newContent = args.size() > 2 ? args.elementAt(2) : null;

                    if (newType.equals("alert")) {
                        return new JOptionPane(newContent != null ? toLuaString(newContent) : "", JOptionPane.INFORMATION_MESSAGE);
                    } 
                    else if (newType.equals("edit")) { 
                        JTextField field = new JTextField(newContent != null ? toLuaString(newContent) : "", 30);
                        return field;
                    } 
                    else if (newType.equals("list")) { 
                        String mode = newContent != null ? toLuaString(newContent) : "implicit";
                        JList<String> list = new JList<>(new String[0]);
                        list.setSelectionMode(mode.equals("multiple") ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : 
                                            mode.equals("exclusive") ? ListSelectionModel.SINGLE_SELECTION : 
                                            ListSelectionModel.SINGLE_SELECTION);
                        return list;
                    } 
                    else if (newType.equals("screen")) { 
                        JPanel panel = new JPanel(new BorderLayout());
                        panel.setBackground(Color.BLACK);
                        panel.putClientProperty("windowTitle", newTitle); // guarda pra usar depois, sem desenhar JLabel

                        JPanel centerPanel = new JPanel(new BorderLayout());
                        centerPanel.setBackground(Color.BLACK);
                        panel.add(centerPanel, BorderLayout.CENTER);

                        JPanel southPanel = new JPanel(new BorderLayout());
                        southPanel.setBackground(Color.BLACK);
                        southPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                        panel.add(southPanel, BorderLayout.SOUTH);

                        return panel;
                    }
                    else if (newType.equals("command")) {
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable<Object, Object> cmdTable = (Hashtable<Object, Object>) args.elementAt(1);
                            Object labelObj = cmdTable.get("label");
                            String label = (labelObj != null && labelObj != LUA_NIL) ? toLuaString(labelObj) : "Command";
                            JButton button = new JButton(label);
                            button.setBackground(Color.GRAY);
                            button.setForeground(Color.BLACK);
                            return button;
                        } else {
                            return gotbad(2, "new", "table expected, got " + type(args.elementAt(1)));
                        }
                    }
                    else if (newType.equals("buffer")) {
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable<Object, Object> field = (Hashtable<Object, Object>) args.elementAt(1);
                            String value = getFieldValue(field, "value", "");
                            String layout = getFieldValue(field, "layout", "default");
                            
                            if (layout.equals("link") || layout.equals("button")) {
                                JButton button = new JButton(value);
                                return button;
                            } else {
                                JLabel label = new JLabel(value);
                                return label;
                            }
                        }
                    }
                    else if (newType.equals("field")) { 
                        if (args.elementAt(1) instanceof Hashtable) {
                            Hashtable<Object, Object> field = (Hashtable<Object, Object>) args.elementAt(1);
                            String value = getFieldValue(field, "value", "");
                            String mode = getFieldValue(field, "mode", "");
                            int length = getFieldNumber(field, "length", 256);
                            
                            JTextField textField = new JTextField(value, Math.min(length, 30));
                            if (mode.contains("password")) {
                                textField = new JPasswordField(value, Math.min(length, 30));
                            }
                            return textField;
                        }
                    }
                    else if (newType.equals("choice")) {
                        Hashtable<Object, Object> field = (Hashtable<Object, Object>) args.elementAt(1);
                        String choiceType = getFieldValue(field, "mode", "exclusive");
                        Object options = field.get("options");
                        
                        Vector<String> items = new Vector<>();
                        if (options instanceof Hashtable) {
                            Hashtable<Object, Object> opts = (Hashtable<Object, Object>) options;
                            if (isListTable(opts)) {
                                Vector<Object> fv = toVector(opts);
                                for (int i = 0; i < opts.size(); i++) {
                                    items.addElement(toLuaString(fv.elementAt(i)));
                                }
                            } else {
                                for (Enumeration<Object> keys = opts.keys(); keys.hasMoreElements();) {
                                    items.addElement(toLuaString(opts.get(keys.nextElement())));
                                }
                            }
                        }
                        
                        if (choiceType.equals("multiple")) {
                            JList<String> list = new JList<>(items.toArray(new String[0]));
                            list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                            return list;
                        } else {
                            JComboBox<String> combo = new JComboBox<>(items.toArray(new String[0]));
                            return combo;
                        }
                    }
                    else if (newType.equals("spacer")) {
                        int width = getFieldNumber((Hashtable<Object, Object>) args.elementAt(1), "width", 1);
                        int height = getFieldNumber((Hashtable<Object, Object>) args.elementAt(1), "height", 10);
                        JPanel spacer = new JPanel();
                        spacer.setPreferredSize(new Dimension(width, height));
                        spacer.setMaximumSize(new Dimension(width, height));
                        return spacer;
                    }
                    else if (newType.equals("gauge")) {
                        Hashtable<Object, Object> field = (Hashtable<Object, Object>) args.elementAt(1);
                        int maxValue = getFieldNumber(field, "maxValue", 100);
                        int value = getFieldNumber(field, "value", 0);
                        JProgressBar progress = new JProgressBar(0, maxValue);
                        progress.setValue(value);
                        progress.setStringPainted(true);
                        return progress;
                    }
                    else { 
                        return gotbad(1, "new", "invalid type: " + newType); 
                    }
                    
                case RENDER:
                    if (args.isEmpty() || args.elementAt(0) == null) {
                        return gotbad(1, "render", "string expected, got" + type(args.elementAt(0)));
                    }
                    return midlet.readImg(toLuaString(args.elementAt(0)), father);
                
                case APPEND:
                    if (args.size() < 2) { return gotbad(1, "append", "wrong number of arguments"); }
                    else {
                        Object targetObj = args.elementAt(0);
                        Object itemObj = args.elementAt(1);
                        
                        if (targetObj instanceof JPanel) {
                            JPanel panel = (JPanel) targetObj;
                            
                            // Encontrar os painéis CENTER e SOUTH
                            JPanel centerPanel = null;
                            JPanel southPanel = null;
                            
                            if (panel.getLayout() instanceof BorderLayout) {
                                for (Component comp : panel.getComponents()) {
                                    Object constraints = ((BorderLayout) panel.getLayout()).getConstraints(comp);
                                    if (constraints != null) {
                                        if (constraints.equals(BorderLayout.CENTER) && comp instanceof JPanel) {
                                            centerPanel = (JPanel) comp;
                                        } else if (constraints.equals(BorderLayout.SOUTH) && comp instanceof JPanel) {
                                            southPanel = (JPanel) comp;
                                        }
                                    }
                                }
                            }
                            
                            // Se não encontrou, criar
                            if (centerPanel == null) {
                                centerPanel = new JPanel(new BorderLayout());
                                centerPanel.setBackground(Color.BLACK);
                                panel.add(centerPanel, BorderLayout.CENTER);
                            }
                            if (southPanel == null) {
                                southPanel = new JPanel(new BorderLayout());
                                southPanel.setBackground(Color.BLACK);
                                panel.add(southPanel, BorderLayout.SOUTH);
                            }
                            
                            if (itemObj instanceof JTextArea) {
                                JTextArea textArea = (JTextArea) itemObj;
                                textArea.setEditable(false);
                                textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                                textArea.setBackground(Color.BLACK);
                                textArea.setForeground(Color.GREEN);
                                
                                JScrollPane scrollPane = new JScrollPane(textArea);
                                scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                                scrollPane.setBorder(BorderFactory.createEmptyBorder());
                                
                                centerPanel.add(scrollPane, BorderLayout.CENTER);
                                System.out.println("Added JTextArea to CENTER");
                                
                            } else if (itemObj instanceof JTextField) {
                                JTextField field = (JTextField) itemObj;
                                field.setFont(new Font("Monospaced", Font.PLAIN, 12));
                                field.setBackground(Color.BLACK);
                                field.setForeground(Color.GREEN);
                                field.setCaretColor(Color.GREEN);
                                field.setBorder(BorderFactory.createEmptyBorder());
                                
                                // Action listener para Enter
                                field.addActionListener(e -> {
                                    try {
                                        String command = field.getText();
                                        if (!command.isEmpty() && !command.startsWith("[")) {
                                            field.setText("");
                                            Vector<Object> args2 = new Vector<>();
                                            args2.addElement(command);
                                            Object os = globals.get("os");
                                            if (os instanceof Hashtable) {
                                                Hashtable<String, Object> osTable = (Hashtable<String, Object>) os;
                                                Object execute = osTable.get("execute");
                                                if (execute instanceof LuaFunction) {
                                                    ((LuaFunction) execute).call(args2);
                                                }
                                            }
                                            // Atualizar prompt
                                            Object label = globals.get("label");
                                            if (label instanceof LuaFunction) {
                                                ((LuaFunction) label).call(new Vector<>());
                                            }
                                        }
                                    } catch (Exception ex) {
                                        midlet.print(midlet.getCatch(ex), stdout, id, father);
                                    }
                                });
                                
                                // Painel com prompt
                                JPanel inputPanel = new JPanel(new BorderLayout());
                                inputPanel.setBackground(Color.BLACK);
                                inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                                
                                JLabel promptLabel = new JLabel("$ ");
                                promptLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
                                promptLabel.setForeground(Color.GREEN);
                                inputPanel.add(promptLabel, BorderLayout.WEST);
                                inputPanel.add(field, BorderLayout.CENTER);
                                
                                southPanel.add(inputPanel, BorderLayout.CENTER);
                                System.out.println("Added JTextField to SOUTH");
                                
                            } else if (itemObj instanceof JButton) {
                                JButton button = (JButton) itemObj;
                                button.setBackground(new Color(60, 60, 60));
                                button.setForeground(Color.WHITE);
                                button.setFont(new Font("Monospaced", Font.PLAIN, 12));
                                button.setPreferredSize(new Dimension(80, 28));
                                
                                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                                buttonPanel.setBackground(Color.BLACK);
                                buttonPanel.add(button);
                                
                                southPanel.add(buttonPanel, BorderLayout.EAST);
                                System.out.println("Added JButton to SOUTH.EAST");
                            }
                            
                            panel.revalidate();
                            panel.repaint();
                            
                            System.out.println("After append, panel has " + panel.getComponentCount() + " components");
                        }
                        break;
                    }
                case ADDCMD:
                    if (args.size() < 2) { return gotbad(1, "addCommand", "wrong number of arguments"); }
                    else {
                        Object targetObj = args.elementAt(0);
                        Object cmdObj = args.elementAt(1);
                        
                        if (!(targetObj instanceof JPanel)) { 
                            return gotbad(1, "addCommand", "JPanel expected"); 
                        }
                        if (!(cmdObj instanceof JButton)) { 
                            return gotbad(1, "addCommand", "JButton expected"); 
                        }

                        JPanel panel = (JPanel) targetObj;
                        JButton button = (JButton) cmdObj;

                        // Achar o southPanel já existente (mesma lógica usada no APPEND),
                        // em vez de jogar o botão direto no painel raiz (que quebrava o BorderLayout)
                        JPanel southPanel = null;
                        if (panel.getLayout() instanceof BorderLayout) {
                            for (Component comp : panel.getComponents()) {
                                Object constraints = ((BorderLayout) panel.getLayout()).getConstraints(comp);
                                if (constraints != null && constraints.equals(BorderLayout.SOUTH) && comp instanceof JPanel) {
                                    southPanel = (JPanel) comp;
                                    break;
                                }
                            }
                        }

                        if (southPanel == null) {
                            southPanel = new JPanel(new BorderLayout());
                            southPanel.setBackground(Color.BLACK);
                            panel.add(southPanel, BorderLayout.SOUTH);
                        }

                        button.setBackground(new Color(60, 60, 60));
                        button.setForeground(Color.WHITE);
                        button.setFont(new Font("Monospaced", Font.PLAIN, 12));
                        button.setPreferredSize(new Dimension(80, 28));

                        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                        buttonPanel.setBackground(Color.BLACK);
                        buttonPanel.add(button);

                        southPanel.add(buttonPanel, BorderLayout.EAST);

                        panel.revalidate();
                        panel.repaint();
                        break;
                    }
                    
                case HANDLER:
                    if (args.size() < 2) { return gotbad(1, "handler", "wrong number of arguments"); }
                    else {
                        Object screen = args.elementAt(0);
                        Object table = args.elementAt(1);

                        if (!(screen instanceof Container)) { 
                            return gotbad(1, "handler", "Container expected, got " + type(table)); 
                        }
                        if (!(table instanceof Hashtable)) { 
                            return gotbad(2, "handler", "Hashtable expected, got " + type(table)); 
                        }

                        Hashtable<Object, Object> handlers = (Hashtable<Object, Object>) table;
                        Container container = (Container) screen;

                        // Busca recursiva: o botão agora vive dentro de southPanel/buttonPanel,
                        // não é mais filho direto do painel raiz
                        Vector<JButton> allButtons = new Vector<>();
                        findButtonsRecursive(container, allButtons);

                        for (JButton button : allButtons) {
                            for (Enumeration<Object> keys = handlers.keys(); keys.hasMoreElements();) {
                                Object key = keys.nextElement();
                                if (key instanceof JButton) {
                                    JButton handlerButton = (JButton) key;
                                    if (handlerButton == button || handlerButton.getText().equals(button.getText())) {
                                        Object handler = handlers.get(key);
                                        if (handler instanceof LuaFunction) {
                                            final Container rootContainer = container;
                                            button.addActionListener(e -> {
                                                try {
                                                    JTextField inputField = findTextField(rootContainer);
                                                    String command = inputField != null ? inputField.getText() : "";

                                                    Vector<Object> args2 = new Vector<>();
                                                    args2.addElement(command);
                                                    ((LuaFunction) handler).call(args2);
                                                } catch (Exception ex) {
                                                    midlet.print(midlet.getCatch(ex), stdout, id, father);
                                                }
                                            });
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        break;
                    }
                case TITLE:
                    if (args.isEmpty()) { break; }
                    Object targetTitle = args.elementAt(0);
                    String titleText = args.size() > 1 ? toLuaString(args.elementAt(1)) : "";

                    if (targetTitle instanceof JFrame) {
                        ((JFrame) targetTitle).setTitle(titleText);
                    } else if (targetTitle instanceof JPanel) {
                        JPanel panel = (JPanel) targetTitle;
                        panel.putClientProperty("windowTitle", titleText);

                        Container top = panel.getTopLevelAncestor();
                        if (top instanceof JFrame) {
                            ((JFrame) top).setTitle(titleText);
                        }
                        if (tabs != null) {
                            int idx = tabs.indexOfComponent(panel);
                            if (idx >= 0) tabs.setTitleAt(idx, titleText);
                        }
                    }
                    break;
                case TICKER:
                    // Em Swing, não há equivalente direto ao Ticker J2ME
                    // Podemos usar uma JLabel ou ignorar
                    if (args.size() > 1) {
                        String tickerText = toLuaString(args.elementAt(1));
                        Object targetObj = args.elementAt(0);
                        if (targetObj instanceof JFrame) {
                            ((JFrame) targetObj).setTitle(tickerText);
                        }
                    }
                    break;
                    
                case VIBRATE:
                    // Em Java Desktop, usar beep
                    Toolkit.getDefaultToolkit().beep();
                    break;
                    
                case SETLABEL:
                    if (args.isEmpty()) { break; }
                    Object labelItem = args.elementAt(0);
                    String labelText = args.size() > 1 ? toLuaString(args.elementAt(1)) : "";
                    
                    if (labelItem instanceof JLabel) {
                        ((JLabel) labelItem).setText(labelText);
                    } else if (labelItem instanceof JButton) {
                        ((JButton) labelItem).setText(labelText);
                    } else if (labelItem instanceof JTextField) {
                        // Não sobrescrever o texto do campo de entrada (isso apagava o que
                        // o usuário digitou e fazia o prompt virar "comando"). Em vez disso,
                        // atualiza o JLabel do prompt que fica ao lado (mesmo inputPanel).
                        JTextField field = (JTextField) labelItem;
                        Container parent = field.getParent();
                        JLabel promptLabel = null;
                        if (parent != null) {
                            for (Component comp : parent.getComponents()) {
                                if (comp instanceof JLabel) {
                                    promptLabel = (JLabel) comp;
                                    break;
                                }
                            }
                        }
                        if (promptLabel != null) {
                            promptLabel.setText(labelText);
                        }
                    } else if (labelItem instanceof JTextArea) {
                        // Para o stdout, adicionar o prompt
                        ((JTextArea) labelItem).append(labelText);
                        ((JTextArea) labelItem).setCaretPosition(((JTextArea) labelItem).getDocument().getLength());
                    }
                    break;
                    
                case GETLABEL:
                    if (args.isEmpty()) { break; }
                    Object getLabelItem = args.elementAt(0);
                    if (getLabelItem instanceof JLabel) {
                        return ((JLabel) getLabelItem).getText();
                    } else if (getLabelItem instanceof JButton) {
                        return ((JButton) getLabelItem).getText();
                    } else if (getLabelItem instanceof JTextField) {
                        return ((JTextField) getLabelItem).getText();
                    }
                    return "";
                    
                case SETTEXT:
                    if (args.isEmpty()) { break; }
                    Object textItem = args.elementAt(0);
                    String textValue = args.size() > 1 ? toLuaString(args.elementAt(1)) : "";
                    
                    if (textItem instanceof JLabel) {
                        ((JLabel) textItem).setText(textValue);
                    } else if (textItem instanceof JButton) {
                        ((JButton) textItem).setText(textValue);
                    } else if (textItem instanceof JTextField) {
                        ((JTextField) textItem).setText(textValue);
                    } else if (textItem instanceof JTextArea) {
                        ((JTextArea) textItem).setText(textValue);
                    }
                    break;
                    
                case GETTEXT:
                    if (args.isEmpty()) { break; }
                    Object getTextItem = args.elementAt(0);
                    if (getTextItem instanceof JLabel) {
                        return ((JLabel) getTextItem).getText();
                    } else if (getTextItem instanceof JButton) {
                        return ((JButton) getTextItem).getText();
                    } else if (getTextItem instanceof JTextField) {
                        return ((JTextField) getTextItem).getText();
                    } else if (getTextItem instanceof JTextArea) {
                        return ((JTextArea) getTextItem).getText();
                    }
                    return "";
                    
                case CLEAR_SCREEN:
                    if (args.isEmpty()) { return gotbad(1, "clear", "screen expected, got no value"); }
                    else {
                        Object screen = args.elementAt(0);
                        if (screen instanceof JTextArea) { 
                            ((JTextArea) screen).setText(""); 
                        } else if (screen instanceof JPanel) { 
                            ((JPanel) screen).removeAll(); 
                            ((JPanel) screen).revalidate();
                            ((JPanel) screen).repaint();
                        } else if (screen instanceof DefaultListModel) { 
                            ((DefaultListModel<?>) screen).clear(); 
                        } else { 
                            return gotbad(1, "clear", "screen expected, got " + type(args.elementAt(0))); 
                        }
                        break;
                    }
                    
                case GETCURRENT:
                    // Em Swing, retornar o frame atual ou null
                    return null;
                    
                // Package [string]
                case LOWER:
                case UPPER:
                    if (args.isEmpty()) { 
                        return gotbad(1, MOD == LOWER ? "lower" : "upper", "string expected, got no value"); 
                    } else { 
                        String text = toLuaString(args.elementAt(0)); 
                        return MOD == LOWER ? text.toLowerCase() : text.toUpperCase(); 
                    }
                    
                case FIND:
                case MATCH:
                case LEN:
                    if (args.isEmpty()) { }
                    else {
                        Object obj = args.elementAt(0);
                        String text = toLuaString(obj), pattern = args.size() > 1 ? toLuaString(args.elementAt(1)) : null;
                    
                        if (MOD == LEN) {
                            if (obj == null) { }
                            else if (obj instanceof String) { 
                                return (double) text.length(); 
                            } 
                            else { 
                                throw new RuntimeException("string.len expected a string"); 
                            }
                        }

                        if (args.elementAt(0) == null || pattern == null) { }
                        else {
                            int startIdx = 0;
                            if (args.size() > 2) {
                                Object startObj = args.elementAt(2);
                                if (!(startObj instanceof Double)) { 
                                    return gotbad(3, "match", "number expected, got " + type(startObj)); 
                                }
                                startIdx = Math.max(0, ((Double) startObj).intValue() - 1);
                            }
                            int pos = text.indexOf(pattern, startIdx);
                            if (pos == -1) { 
                                return null; 
                            } 
                            else if (MOD == FIND) { 
                                return (double) (pos + 1); 
                            } 
                            else if (MOD == MATCH) { 
                                return text.substring(pos, pos + pattern.length()); 
                            }
                        }
                    }
                    
                case REVERSE:
                    if (args.isEmpty()) { 
                        return gotbad(1, "reverse", "string expected, got no value"); 
                    } else { 
                        StringBuffer sb = new StringBuffer(toLuaString(args.elementAt(0))); 
                        return sb.reverse().toString(); 
                    }
                    
                case SUB:
                    if (args.isEmpty()) { 
                        return gotbad(1, "sub", "string expected, got no value"); 
                    }
                    else {
                        String text = toLuaString(args.elementAt(0));

                        if (args.elementAt(0) == null) { }
                        else {
                            if (args.size() == 1) { 
                                return text; 
                            }

                            int len = text.length();
                            int start = getNumber(toLuaString(args.elementAt(1)), 1);
                            int end = args.size() > 2 ? getNumber(toLuaString(args.elementAt(2)), len) : len;

                            if (start < 0) { 
                                start = len + start + 1; 
                            }
                            if (end < 0) { 
                                end = len + end + 1; 
                            }

                            if (start < 1) { 
                                start = 1; 
                            }
                            if (end > len) { 
                                end = len; 
                            }

                            if (start > end || start > len) { 
                                return ""; 
                            }

                            return text.substring(start - 1, end);
                        }
                    }
                    
                case HASH:
                    return args.isEmpty() || args.elementAt(0) == null ? null : (double) args.elementAt(0).hashCode();
                    
                case BYTE:
                    if (args.isEmpty() || args.elementAt(0) == null) { 
                        return gotbad(1, "byte", "string expected, got no value"); 
                    }
                    else {
                        String s = toLuaString(args.elementAt(0));
                        int len = s.length(), start = 1, end = 1;
                        if (args.size() >= 2) { 
                            start = getNumber(toLuaString(args.elementAt(1)), 1); 
                        }
                        if (args.size() >= 3) { 
                            end = getNumber(toLuaString(args.elementAt(2)), start); 
                        }
                        
                        if (start < 0) { 
                            start = len + start + 1; 
                        }
                        if (end < 0) { 
                            end = len + end + 1; 
                        }
                        if (start < 1) { 
                            start = 1; 
                        }
                        if (end > len) { 
                            end = len; 
                        } 
                        if (start > end || start > len) { 
                            return null; 
                        }
                        
                        if (end - start + 1 == 1) { 
                            return (double) s.charAt(start - 1); 
                        } 
                        else {
                            Hashtable<Object, Object> result = new Hashtable<>();
                            for (int i = start; i <= end; i++) { 
                                result.put((double) i, (double) s.charAt(i - 1)); 
                            }
                            return result;
                        }
                    }
                    
                case CHAR:
                    if (args.isEmpty()) { 
                        return ""; 
                    } 
                    else {
                        Object firstArg = args.elementAt(0);
                        
                        if (firstArg instanceof Hashtable) {
                            Hashtable<Object, Object> table = (Hashtable<Object, Object>) firstArg;
                            StringBuffer sb = new StringBuffer();
                            for (int i = 0; i <= table.size(); i++) {
                                Object charArg = table.get((double) (i + 1));  // Mudar nome
                                if (charArg == null) { 
                                    continue; 
                                }
                                double num;
                                if (charArg instanceof Double) { 
                                    num = ((Double) charArg).doubleValue(); 
                                } 
                                else { 
                                    return gotbad(1, "char", "value out of range"); 
                                }
                                int c = (int) num;
                                if (c < 0 || c > 255) { 
                                    return gotbad(1, "char", "value out of range"); 
                                }
                                sb.append((char) c);
                            }
                            return sb.toString();
                        } else {
                            StringBuffer sb = new StringBuffer();
                            for (int i = 0; i < args.size(); i++) {
                                Object charArg = args.elementAt(i);  // Mudar nome
                                if (charArg == null) { 
                                    return gotbad(1, "char", "number expected, got nil"); 
                                }
                                double num;
                                if (charArg instanceof Double) { 
                                    num = ((Double) charArg).doubleValue(); 
                                } 
                                else {
                                    try { 
                                        num = Double.parseDouble(toLuaString(charArg)); 
                                    } 
                                    catch (Exception e) { 
                                        return gotbad(1, "char", "number expected, got " + type(charArg)); 
                                    }
                                }
                                int c = (int) num;
                                if (c < 0 || c > 255) { 
                                    return gotbad(1, "char", "value out of range"); 
                                }
                                sb.append((char) c);
                            }
                            return sb.toString();
                        }
                    }
                    
                case TRIM:
                    return args.isEmpty() ? null : toLuaString(args.elementAt(0)).trim();
                    
                case UUID:
                    String chars = "0123456789abcdef";
                    StringBuffer uuid = new StringBuffer(); 
                    for (int i = 0; i < 36; i++) { 
                        if (i == 8 || i == 13 || i == 18 || i == 23) { 
                            uuid.append('-'); 
                        } else if (i == 14) { 
                            uuid.append('4'); 
                        } else if (i == 19) { 
                            uuid.append(chars.charAt(8 + midlet.random.nextInt(4))); 
                        } else { 
                            uuid.append(chars.charAt(midlet.random.nextInt(16))); 
                        } 
                    } 
                    return uuid.toString();
                    
                case SPLIT:
                    if (args.isEmpty()) { 
                        return gotbad(1, "split", "string expected, got no value"); 
                    } 
                    else if (args.size() > 1 && args.elementAt(1) == null) {
                        String[] array = midlet.splitArgs(toLuaString(args.elementAt(0)));
                        Hashtable<Object, Object> result = new Hashtable<>();
                        for (int i = 0; i < array.length; i++) { 
                            result.put((double) (i + 1), array[i]); 
                        }
                        return result;
                    }
                    else {
                        String text = toLuaString(args.elementAt(0));
                        String separator = args.size() > 1 ? toLuaString(args.elementAt(1)) : " ";
                        
                        if (text == null || text.length() == 0) { 
                            return new Hashtable<Object, Object>(); 
                        }
                        if (separator == null || separator.length() == 0) {
                            Hashtable<Object, Object> result = new Hashtable<>();
                            for (int i = 0; i < text.length(); i++) { 
                                result.put((double) (i + 1), String.valueOf(text.charAt(i))); 
                            }
                            return result;
                        }
                        
                        Hashtable<Object, Object> result = new Hashtable<>();
                        int index = 1, startPos = 0, sepLength = separator.length();
                        
                        while (startPos < text.length()) {
                            int foundPos = text.indexOf(separator, startPos);
                            
                            if (foundPos == -1) {
                                result.put((double) index++, text.substring(startPos));
                                break;
                            } else {
                                result.put((double) index++, text.substring(startPos, foundPos));
                                startPos = foundPos + sepLength;
                            }
                        }
                        
                        return result;
                    }
                    
                case GETCMD:
                    return args.isEmpty() ? null : midlet.getCommand(toLuaString(args.elementAt(0)));
                    
                case GETARGS:
                    return args.isEmpty() ? null : midlet.getArgument(toLuaString(args.elementAt(0)));
                    
                case GETPATTERN:
                    return args.isEmpty() ? null : midlet.getpattern(toLuaString(args.elementAt(0)));
                    
                case ENV:
                    return args.isEmpty() ? null : midlet.env(toLuaString(args.elementAt(0)));
                    
                case STARTSWITH:
                    return args.size() < 2 ? gotbad(1, "startswith", "string expected") : toLuaString(args.elementAt(0)).startsWith(toLuaString(args.elementAt(1)));
                    
                case ENDSWITH:
                    return args.size() < 2 ? gotbad(1, "endswith", "string expected") : toLuaString(args.elementAt(0)).endsWith(toLuaString(args.elementAt(1)));
                    
                // Package [audio] - simplificado
                case AUDIO_LOAD:
                    if (args.isEmpty()) { 
                        return gotbad(1, "load", "string expected, got no value"); 
                    }
                    else {
                        // Em Java Desktop, usar javax.sound ou retornar null
                        return null;
                    }
                    
                case AUDIO_PLAY:
                case AUDIO_PAUSE:
                case AUDIO_VOLUME:
                case AUDIO_DURATION:
                case AUDIO_TIME:
                    return 0.0;
                    
                // Package [java]
                case CLASS:
                    if (args.isEmpty() || args.elementAt(0) == null) { 
                        return gotbad(1, "class", "string expected, got no value"); 
                    } else { 
                        try { 
                            Class.forName(toLuaString(args.elementAt(0))); 
                            return true; 
                        } catch (ClassNotFoundException e) { 
                            return false; 
                        } 
                    }
                    
                case NAME:
                    return midlet.getName();
                    
                case DELETE:
                    if (args.isEmpty() || !(args.elementAt(0) instanceof Hashtable)) { 
                        return gotbad(1, "delete", "table expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); 
                    } else if (args.size() < 2 || args.elementAt(1) == null) { 
                        return gotbad(2, "delete", "value expected, got " + (args.size() < 2 ? "no value" : "nil")); 
                    } else { 
                        ((Hashtable<Object, Object>) args.elementAt(0)).remove(args.elementAt(1)); 
                        return null; 
                    }
                    
                case RUN:
                    if (args.isEmpty()) { 
                        break; 
                    } else if (args.elementAt(0) instanceof LuaFunction) { 
                        kill = false; 
                        new Thread((Runnable) args.elementAt(0), args.size() > 1 ? toLuaString(args.elementAt(1)) : "Background").start(); 
                    } else { 
                        return gotbad(1, "run", "function expected, got" + type(args.elementAt(0))); 
                    } 
                    break;
                    
                case PREQ:
                    if (args.isEmpty()) { 
                        break; 
                    } else { 
                        return false; // platformRequest não disponível em Java Desktop
                    }
                    
                case THREAD:
                    return Thread.currentThread().getName();
                    
                case UPTIME:
                    return (double) (System.currentTimeMillis() - midlet.uptime);
                    
                case SLEEP:
                    if (args.isEmpty()) { }
                    else {
                        arg = args.elementAt(0);
                        if (arg instanceof Double) { 
                            Thread.sleep(((Double) arg).longValue()); 
                            break; 
                        }
                        else { 
                            return gotbad(1, "sleep", "number expected, got " + type(arg)); 
                        }
                    }
                    
                // Package [push] - não suportado em Desktop
                case PUSH_REGISTER:
                case PUSH_UNREGISTER:
                case PUSH_LIST:
                case PUSH_PENDING:
                case PUSH_SET_ALARM:
                    return false;
                    
                // Kernel Core
                case KERNEL:
                    Object payload = args.elementAt(0);
                    Object arg2 = args.elementAt(1);
                    Object scope2 = args.elementAt(2);
                    Object pid2 = args.elementAt(3);
                    int uid2 = ((Double) args.elementAt(4)).intValue();

                    if (payload == null || payload.equals("")) { 
                        return null; 
                    }
                    if (payload instanceof String) {
                        if (payload.equals("sendsig")) {
                            if (arg2 == null || !(arg2 instanceof Hashtable)) { 
                                return 2.0; 
                            }
                            else {
                                Hashtable<Object, Object> info = (Hashtable<Object, Object>) arg2;
                                String pid = (String) info.get("pid");
                                String signal = toLuaString(info.get("signal"));

                                if (midlet.sys.containsKey(pid)) {
                                    Process process = (Process) midlet.sys.get(pid);

                                    if (process.uid == uid2 || uid2 == 0) {
                                        if (!signal.equals("9") && process.sighandler != null) {
                                            try { 
                                                Vector<Object> arguments = new Vector<>(); 
                                                arguments.addElement(signal);
                                                ((LuaFunction) process.sighandler).call(arguments);
                                            }
                                            catch (Throwable e) {  }
                                        }

                                        midlet.sys.remove(pid);
                                        if (signal.equals("9") && arg2.equals("1")) { 
                                            midlet.destroyApp(true); 
                                        }
                                        return 0.0;
                                    } else { 
                                        return 13.0; 
                                    }
                                }
                                else { 
                                    return 127.0; 
                                }
                            }
                        }
                        else if (payload.equals("proc")) {
                            if (arg2 == null || arg2.equals("")) { 
                                return 2.0; 
                            }
                            else if (midlet.sys.containsKey(arg2)) {
                                Process process = (Process) midlet.sys.get(arg2);
                                if (process.uid == uid2 || uid2 == 0) { 
                                    return process; 
                                }
                                else { 
                                    return 13.0; 
                                }
                            }
                            else { 
                                return 127.0; 
                            }
                        }
                        else if (payload.equals("nice")) {
                            if (arg2 == null || !(arg2 instanceof Hashtable)) { 
                                return 2.0; 
                            }
                            else {
                                Hashtable<Object, Object> info = (Hashtable<Object, Object>) arg2;
                                String pid = (String) info.get("pid");
                                int priority = ((Double) info.get("priority")).intValue();
                                if (midlet.sys.containsKey(pid)) {
                                    Process process = (Process) midlet.sys.get(pid);

                                    if (process.uid == uid2 || uid2 == 0) {
                                        process.priority = Math.max(Process.MIN_PRIORITY, Math.min(Process.MAX_PRIORITY, priority));
                                        return 0.0;
                                    } else { 
                                        return 13.0; 
                                    }
                                }
                                else { 
                                    return 127.0; 
                                }
                            }
                        }
                        else if (payload.equals("passwd")) {
                            if (arg2 instanceof String) { 
                                return midlet.passwd((String) arg2); 
                            }
                            else if (arg2 instanceof Hashtable) {
                                Hashtable<Object, Object> query = (Hashtable<Object, Object>) arg2;
                                String old = (String) query.get("old");
                                String newpw = (String) query.get("new");

                                if (old == null || newpw == null || old.equals("") || newpw.equals("")) { 
                                    return 2.0; 
                                }
                                else if (uid2 == 0 || midlet.passwd(old)) { 
                                    midlet.write("/home/OpenRMS", newpw, 0, father);
                                    return 0.0;
                                }
                                else { 
                                    return 13.0; 
                                }
                            }
                        }
                        else if (payload.equals("setsh")) {
                            if (arg2 == null || arg2.equals("")) { 
                                midlet.shell = new LuaFunction(EXEC); 
                            }
                            else if (arg2 instanceof LuaFunction) { 
                                midlet.shell = arg2; 
                            }
                            else { 
                                return 2.0; 
                            }
                        }
                        else if (payload.equals("cache")) { 
                            if (arg2 == null || arg2.equals("")) { 
                                return midlet.useCache; 
                            } else if (arg2 == TRUE || toLuaString(arg2).equals("true")) { 
                                midlet.useCache = true; 
                            } else if (arg2 == FALSE || toLuaString(arg2).equals("false")) { 
                                midlet.useCache = false; 
                                midlet.cache.clear(); 
                                midlet.cacheLua.clear(); 
                            } else { 
                                return 2.0; 
                            } 
                        }
                        else if (payload.equals("debug")) { 
                            if (arg2 == null || arg2.equals("")) { 
                                return midlet.debug; 
                            } else if (arg2 == TRUE || toLuaString(arg2).equals("true")) { 
                                midlet.debug = true; 
                            } else if (arg2 == FALSE || toLuaString(arg2).equals("false")) { 
                                midlet.debug = false; 
                            } else { 
                                return 2.0; 
                            } 
                        }
                        else if (payload.equals("netsh")) {
                            if (arg2 == null || arg2.equals("")) {
                                Hashtable<Object, Object> result = new Hashtable<>();
                                for (Enumeration<String> procs = midlet.sys.keys(); procs.hasMoreElements();) {
                                    String pid = procs.nextElement();
                                    Process p = (Process) midlet.sys.get(pid);
                                    
                                    if (p.net.isEmpty()) { }
                                    else {
                                        Hashtable<Object, Object> map = new Hashtable<>(); 
                                        int i = 1;
                                        for (Enumeration<String> sockets = p.net.keys(); sockets.hasMoreElements();) {
                                            map.put((double) i++, sockets.nextElement());
                                        }
                                        result.put(pid, map);
                                    }
                                }
                                return result;
                            }
                        }
                        else if (payload.equals("useradd")) {
                            if (arg2 == null || arg2.equals("") || arg2.equals("root")) { 
                                return 2.0; 
                            }
                            else if (midlet.userID.containsKey(arg2)) { 
                                return 128.0; 
                            }
                            else { 
                                midlet.userID.put((String) arg2, ++midlet.lastID); 
                                return 0.0; 
                            }
                        }
                        else if (payload.equals("userdel")) {
                            if (arg2 == null || arg2.equals("") || arg2.equals("root") || arg2.equals(midlet.username)) { 
                                return 13.0; 
                            }
                            else if (midlet.userID.containsKey(arg2)) { 
                                if (uid2 == 0) { 
                                    midlet.userID.remove(arg2); 
                                    return 0.0; 
                                } else { 
                                    return 13.0; 
                                } 
                            }
                            else { 
                                return 127.0; 
                            }
                        }
                        else if (payload.equals("user")) {
                            if (arg2 == null || arg2.equals("") || !(arg2 instanceof Double)) { 
                                return 2.0; 
                            }
                            else {
                                String user = midlet.getUser(((Double) arg2).intValue());
                                if (user == null) { 
                                    return 127.0; 
                                }
                                else { 
                                    return user; 
                                }
                            }
                        }
                    }
            }
            return null;
        }
        
        private Object exec(String code, Hashtable<String, Object> scope) throws Exception { 
            int savedIndex = tokenIndex; 
            Vector<Token> savedTokens = tokens; 
            Object ret = null; 
            try { 
                tokens = tokenize(code); 
                tokenIndex = 0; 
                Hashtable<String, Object> modScope = scope == null ? new Hashtable<>() : scope; 
                for (Enumeration<String> e = globals.keys(); e.hasMoreElements();) { 
                    String k = e.nextElement(); 
                    modScope.put(k, unwrap(globals.get(k))); 
                } 
                while (peek().type != EOF) { 
                    Object res = statement(modScope); 
                    if (doreturn) { 
                        ret = res; 
                        doreturn = false; 
                        break; 
                    } 
                } 
            } finally { 
                tokenIndex = savedIndex; 
                tokens = savedTokens; 
            } 
            return ret; 
        }
        
        private Object gotbad(int pos, String name, String expect) throws Exception { 
            throw new RuntimeException("bad argument #" + pos + " to '" + name + "' (" + expect + ")"); 
        }
        
        private Object http(String method, String url, String data, Object item, boolean toget) throws Exception {
            if (url == null || url.length() == 0) { 
                return ""; 
            }
            if (!url.startsWith("http://") && !url.startsWith("https://")) { 
                url = "http://" + url; 
            }

            java.net.HttpURLConnection conn = null;
            InputStream is = null;
            ByteArrayOutputStream baos = null;

            try {
                conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                conn.setRequestMethod(method.toUpperCase());
                conn.setDoInput(true);
                
                if ("POST".equalsIgnoreCase(method)) {
                    conn.setDoOutput(true);
                    byte[] postBytes = (data == null) ? new byte[0] : data.getBytes(StandardCharsets.UTF_8);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setRequestProperty("Content-Length", Integer.toString(postBytes.length));
                    
                    OutputStream os = conn.getOutputStream();
                    os.write(postBytes);
                    os.flush();
                    os.close();
                }

                int responseCode = conn.getResponseCode();
                is = conn.getInputStream();
                
                if (toget) { 
                    Vector<Object> result = new Vector<>(); 
                    result.addElement(is); 
                    result.addElement((double) responseCode); 
                    return result; 
                }
                
                baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = is.read()) != -1) { 
                    baos.write(ch); 
                }

                Vector<Object> result = new Vector<>();
                result.addElement(new String(baos.toByteArray(), StandardCharsets.UTF_8));
                result.addElement((double) responseCode);

                return result;
            } 
            catch (Exception e) { 
                throw e; 
            } finally {
                if (is != null) { try { is.close(); } catch (Exception e) { } }
                if (baos != null) { try { baos.close(); } catch (Exception e) { } }
                if (conn != null) { try { conn.disconnect(); } catch (Exception e) { } }
            }
        }
        
        private int compareLua(Object a, Object b) { 
            if (a == null && b == null) { 
                return 0; 
            } 
            if (a == null) { 
                return -1; 
            } 
            if (b == null) { 
                return 1; 
            } 
            if (a instanceof Double && b instanceof Double) { 
                double da = ((Double) a).doubleValue(), db = ((Double) b).doubleValue(); 
                return da < db ? -1 : (da > db ? 1 : 0); 
            } 
            String sa = toLuaString(a), sb = toLuaString(b); 
            return sa.compareTo(sb); 
        }
        
        private String getFieldValue(Hashtable<Object, Object> table, String key, String fallback) {
            Object val = table.get(key);
            return val != null && val != LUA_NIL ? toLuaString(val) : fallback;
        }

        private int getFieldNumber(Hashtable<Object, Object> table, String key, int fallback) {
            Object val = table.get(key);
            if (val instanceof Double) {
                return ((Double) val).intValue();
            }
            try {
                return Integer.parseInt(toLuaString(val));
            } catch (Exception e) {
                return fallback;
            }
        }

        private boolean getFieldBoolean(Hashtable<Object, Object> table, String key, boolean fallback) {
            Object val = table.get(key);
            if (val instanceof Boolean) {
                return ((Boolean) val).booleanValue();
            }
            return toLuaString(val).equalsIgnoreCase("true") ? true : fallback;
        }

        public int getNumber(String s, int fallback) { 
            try { 
                return Integer.valueOf(s); 
            } catch (Exception e) { 
                return fallback; 
            } 
        }
        
        public Object exec(Vector<Object> args) throws Exception {
            if (args.isEmpty()) { 
                return gotbad(1, "execute", "string expected, got no value"); 
            }
            else {
                int status = 0; 
                InputStream in; 
                boolean builtin = args.size() > 1 ? ((Boolean) args.elementAt(1)).booleanValue() : false;

                String command = midlet.env(toLuaString(args.elementAt(0)));
                String mainCommand = midlet.getCommand(command), argument = midlet.getArgument(command);
                String[] argsArray = midlet.splitArgs(argument);

                Object output = stdout; 
                Hashtable<String, Object> aliases = (Hashtable<String, Object>) father.get("ALIAS");
                for (int i = 0; i < argsArray.length; i++) {
                    if (argsArray[i].equals(">")) {
                        output = toLuaString(midlet.joinpath(argsArray[i + 1], father));
                        
                        Vector<String> sanitize = new Vector<>(); 
                        StringBuffer buffer = new StringBuffer();
                        for (int j = 0; j < i - 1; j++) { 
                            sanitize.addElement(argsArray[j]); 
                            buffer.append(argsArray[j]); 
                        }

                        String[] newArgs = new String[sanitize.size()];
                        sanitize.copyInto(newArgs); 
                        argument = buffer.toString();

                        break;
                    }
                }

                if (mainCommand.equals("") || mainCommand.equals("true") || mainCommand.startsWith("#")) { }
                else if (aliases.containsKey(mainCommand) && !builtin) { 
                    Vector<Object> payload = new Vector<>(); 
                    payload.addElement(aliases.get(mainCommand) + " " + argument); 
                    return exec(payload); 
                }
                else if ((in = open("/bin/" + mainCommand, father)) != null) { 
                    Vector<Object> result = popen("/bin/" + mainCommand, midlet.genpid(), argument, id, output, father, in);
                    status = ((Double) result.elementAt(0)).intValue();
                }
                else if (mainCommand.equals(".")) {
                    if (argsArray.length == 0) { }
                    else if ((in = open(midlet.joinpath(argsArray[0], father), father)) != null) {
                        Vector<Object> result = popen(argsArray[0], midlet.genpid(), argument.substring(argsArray[0].length()).trim(), id, output, father, in);
                        status = ((Double) result.elementAt(0)).intValue();
                    }
                    else {
                        midlet.print(". " + argsArray[0] + ": not found", output, id, father);
                        status = 127;
                    }
                }
                else if (mainCommand.equals("gc")) { 
                    System.gc(); 
                }
                else if (mainCommand.equals("cat")) {
                    for (int i = 0; i < argsArray.length; i++) {
                        try {
                            InputStream inStream = midlet.getInputStream(midlet.joinpath(argsArray[i], father), father);
                            if (inStream != null) { 
                                midlet.print(midlet.read(inStream, 1024, true), output, id, father); 
                            }
                            else { 
                                status = 2; 
                                break; 
                            }
                        } catch (Exception e) {
                            status = 127; 
                            break;
                        }
                    }
                }
                else if (mainCommand.equals("ls")) {
                    Vector<Object> payload = new Vector<>(); 
                    StringBuffer buffer = new StringBuffer();
                    payload.addElement(argsArray.length == 0 ? (String) father.get("PWD") : midlet.joinpath(argsArray[0], father));
                    Hashtable<Object, Object> items = dirs(payload);

                    if (items.isEmpty()) { }
                    else {
                        for (Enumeration<Object> keys = items.keys(); keys.hasMoreElements();) {
                            Double i = (Double) keys.nextElement();
                            String file = (String) items.get(i);

                            if (!file.startsWith(".")) { 
                                buffer.append(file).append("\t"); 
                            }
                        }

                        midlet.print(buffer.toString().trim(), output, id, father);
                    }
                }
                else if (mainCommand.equals("ps")) {
                    midlet.print("PID\tPROCESS", output, id, father);
                    for (Enumeration<String> procs = midlet.sys.keys(); procs.hasMoreElements();) {
                        String pid = (String) procs.nextElement();

                        midlet.print(pid + "\t" + ((Process) midlet.sys.get(pid)).name, output, id, father);
                    }
                }
                else if (mainCommand.equals("su")) {
                    if (argsArray.length >= 2) {
                        if (argsArray[0].equals("root") && midlet.passwd(argsArray[1])) { 
                            id = 0; 
                            father.put("USER", "root"); 
                        }
                        else { 
                            midlet.print("Permission denied!", output, id, father); 
                            status = 13; 
                        }
                    } 
                    else if (argsArray.length == 1) {
                        if (midlet.userID.containsKey(argsArray[0])) {
                            id = midlet.getUserID(argsArray[0]);
                            father.put("USER", argsArray[0]);
                        } else {
                            midlet.print("Permission denied!", output, id, father);
                            status = 13;
                        }
                    }
                    else {
                        if (id != 1000) {
                            id = 1000;
                            father.put("USER", midlet.username);
                        } else {
                            midlet.print("su: usage: su [username] [passwd]", output, id, father);
                        }
                    }
                }
                else if (mainCommand.equals("uptime")) { 
                    midlet.print(((System.currentTimeMillis() - midlet.uptime) / 1000) + " ms", output, id, father); 
                }
                else if (mainCommand.equals("time")) {
                    long before = System.currentTimeMillis();
                    Vector<Object> payload = new Vector<>();
                    payload.addElement(argument);
                    status = ((Double) exec(payload)).intValue();

                    midlet.print("at " + (System.currentTimeMillis() - before), output, id, father);
                }
                else if (mainCommand.equals("whoami")) { 
                    midlet.print((String) father.get("USER"), output, id, father); 
                }
                else if (mainCommand.equals("id")) {
                    if (argsArray.length == 0) {
                        midlet.print("uid=" + id + "(" + midlet.getUser(id) + ")", output, id, father);
                    } else {
                        for (int i = 0; i < argsArray.length; i++) {
                            int uid = midlet.getUserID(argsArray[i]);

                            if (uid == -1) { 
                                midlet.print("id: " + argsArray[i] + ": not found", output, id, father); 
                                status = 127; 
                                break; 
                            }
                            else { 
                                midlet.print("uid=" + uid + "(" + argsArray[i] + ")", output, id, father); 
                            }
                        }
                    }
                }
                else if (mainCommand.equals("alias")) {
                    if (argsArray.length == 0) {
                        for (Enumeration<String> keys = aliases.keys(); keys.hasMoreElements();) {
                            String key = keys.nextElement();
                            String value = (String) aliases.get(key);
                            midlet.print("alias " + key + "='" + value + "'", output, id, father);
                        }
                    } 
                    else {
                        for (int i = 0; i < argsArray.length; i++) {
                            int j = argsArray[i].indexOf("="); 
                            if (j != -1) {
                                String key = argsArray[i].substring(0, j);
                                String value = midlet.getpattern(argsArray[i].substring(j + 1));
                                aliases.put(key, value);
                            } else {
                                if (aliases.containsKey(argsArray[i])) {
                                    midlet.print("alias " + argsArray[i] + "='" + aliases.get(argsArray[i]) + "'", output, id, father);
                                } else {
                                    midlet.print("alias: " + argsArray[i] + ": not found", output, id, father); 
                                    status = 127;
                                }
                            }
                        }
                    }
                }
                else if (mainCommand.equals("unalias")) {
                    if (argsArray.length == 0) { 
                        midlet.print("unalias: usage: unalias [-a] name [name ...]", output, id, father); 
                    }
                    else if (argsArray[0].equals("-a")) { 
                        aliases.clear(); 
                    }
                    else {
                        for (int i = 0; i < argsArray.length; i++) {
                            if (aliases.containsKey(argsArray[i])) {
                                aliases.remove(argsArray[i]);
                            }
                            else {
                                midlet.print("unalias: " + argsArray[i] + ": not found", output, id, father);
                                status = 127;
                                break;
                            }
                        }
                    }
                }
                else if (mainCommand.equals("clear")) { 
                    midlet.stdout.setText(""); 
                }
                else if (mainCommand.equals("env") || mainCommand.equals("export") || mainCommand.equals("set")) {
                    if (argsArray.length == 0) {
                        for (Enumeration<String> keys = midlet.attributes.keys(); keys.hasMoreElements();) {
                            String key = keys.nextElement();
                            String value = (String) midlet.attributes.get(key);
                            midlet.print(key + "=" + value, output, id, father);
                        }
                    } else {
                        for (int i = 0; i < argsArray.length; i++) {
                            int j = argsArray[i].indexOf("="); 
                            if (j != -1) {
                                String key = argsArray[i].substring(0, j);
                                String value = midlet.getpattern(argsArray[i].substring(j + 1));
                                midlet.attributes.put(key, value);
                            } else {
                                if (midlet.attributes.containsKey(argsArray[i])) {
                                    midlet.print(argsArray[i] + "=" + midlet.attributes.get(argsArray[i]), output, id, father);
                                } else {
                                    midlet.print(mainCommand + ": " + argsArray[i] + ": not found", output, id, father); 
                                    status = 127;
                                }
                            }
                        }
                    }
                }
                else if (mainCommand.equals("unset")) {
                    if (argsArray.length == 0) { }
                    else {
                        for (int i = 0; i < argsArray.length; i++) {
                            if (midlet.attributes.containsKey(argsArray[i])) {
                                midlet.attributes.remove(argsArray[i]);
                            }
                            else {
                                midlet.print("unset: " + argsArray[i] + ": not found", output, id, father);
                                status = 127;
                                break;
                            }
                        }
                    }
                }
                else if (mainCommand.equals("echo")) { 
                    midlet.print(argument, output, id, father); 
                }
                else if (mainCommand.equals("exit")) { 
                    Vector<Object> payload = new Vector<>(); 
                    payload.addElement(argsArray.length == 0 ? "0" : argsArray[0]); 
                    payload.addElement(builtin); 
                    exit(payload); 
                }
                else if (mainCommand.equals("pwd")) { 
                    midlet.print((String) father.get("PWD"), output, id, father); 
                }
                else if (mainCommand.equals("cd")) {
                    Vector<Object> payload = new Vector<>();
                    payload.addElement(argsArray.length == 0 ? "/home/" : argsArray[0]);
                    status = ((Double) chdir(payload)).intValue();

                    if (status == 127) { 
                        midlet.print("cd: " + argsArray[0] + ": not found", output, id, father); 
                    }
                    else if (status == 20) { 
                        midlet.print("cd: " + argsArray[0] + ": found", output, id, father); 
                    }
                }
                else if (mainCommand.equals("builtin") || mainCommand.equals("command")) { 
                    Vector<Object> payload = new Vector<>(); 
                    payload.addElement(argument); 
                    payload.addElement(true); 
                    payload.addElement(FALSE); 
                    return exec(payload); 
                }
                else if (mainCommand.equals("false")) { 
                    status = 255; 
                }
                else { 
                    midlet.print(mainCommand + ": not found", output, id, father); 
                    status = 127; 
                }
                
                return (double) status;
            }
        }
        
        public Hashtable<Object, Object> dirs(Vector<Object> args) throws Exception {
            String pwd = args.isEmpty() ? (String) father.get("PWD") : toLuaString(args.elementAt(0));
            int index = 1;
            
            Hashtable<Object, Object> list = new Hashtable<>();
            if (pwd.startsWith("/")) { }
            else { 
                pwd = ((String) father.get("PWD")) + pwd; 
            }
            if (pwd.endsWith("/")) { } 
            else { 
                pwd = pwd + "/"; 
            }

            if ((pwd = midlet.solvepath(pwd, father)).equals("/tmp/")) { 
                for (Enumeration<String> files = midlet.tmp.keys(); files.hasMoreElements();) { 
                    list.put((double) index++, (String) files.nextElement()); 
                } 
            }
            else if (pwd.startsWith("/mnt/")) { 
                File dir = new File(pwd.substring(5));
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            list.put((double) index++, file.getName());
                        }
                    }
                }
            } 
            else if (pwd.equals("/home/")) { 
                File homeDir = new File(midlet.homeDir);
                if (homeDir.exists() && homeDir.isDirectory()) {
                    File[] files = homeDir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (!file.getName().equals("OpenRMS")) {
                                list.put((double) index++, file.getName());
                            }
                        }
                    }
                }
            }
            else {
                // Check if directory exists in virtual filesystem
                File dir = new File(midlet.getRealPath(pwd));
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            list.put((double) index++, file.getName() + (file.isDirectory() ? "/" : ""));
                        }
                    }
                }
            }
            
            if (midlet.fs.containsKey(pwd)) {
                Vector<Object> struct = (Vector<Object>) midlet.fs.get(pwd);
                for (int i = 0; i < struct.size(); i++) { 
                    list.put((double) index++, struct.elementAt(i)); 
                }
            }
            
            return list;
        }

        public InputStream open(String uri, Hashtable<String, Object> scope) { 
            try { 
                return midlet.getInputStream(uri, scope); 
            } catch (Exception e) { 
                return null; 
            } 
        }
        
        public Vector<Object> popen(Vector<Object> args) throws Exception {
            if (args.isEmpty()) { 
                return null; 
            }
            
            String program = toLuaString(args.elementAt(0));
            Object arguments = args.size() > 1 ? toLuaString(args.elementAt(1)) : "";
            int owner = (args.size() < 3) ? id : ((args.elementAt(2) instanceof Boolean) ? ((Boolean) args.elementAt(2) ? id : 1000) : ((Double) gotbad(3, "popen", "boolean expected, got " + type(args.elementAt(2)))).intValue());
            Object out = (args.size() < 4) ? new StringBuffer() : args.elementAt(3);
            Hashtable<String, Object> scope = (args.size() < 5) ? father : (args.elementAt(4) instanceof Hashtable ? (Hashtable<String, Object>) args.elementAt(4) : (Hashtable<String, Object>) gotbad(5, "popen", "table expected, got " + type(args.elementAt(4))));
            InputStream is = (args.size() < 6) ? midlet.getInputStream(program, father) : (InputStream) args.elementAt(5);
            
            return popen(program, midlet.genpid(), arguments, owner, out, scope, is);
        }
        
        public Vector<Object> popen(String program, String pid, Object arguments, int owner, Object out, Hashtable<String, Object> scope, InputStream is) throws Exception {
            Vector<Object> result = new Vector<>();
            
            if (is == null) { 
                result.addElement(127.0); 
                return result; 
            }
            
            try {  
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                
                while ((length = is.read(buffer)) != -1) { 
                    baos.write(buffer, 0, length); 
                }
                
                byte[] data = baos.toByteArray();
                baos.close();
                
                Hashtable<Object, Object> arg = null;
                if (arguments instanceof Hashtable) {
                    arg = (Hashtable<Object, Object>) arguments;
                    arg.put(0, program);
                } else if (arguments instanceof String[]) {
                    arg = new Hashtable<>();
                    String[] list = (String[]) arguments;
                    for (int i = 0; i < list.length; i++) { 
                        arg.put((double) i, list[i]); 
                    }
                } else if (arguments != null) {
                    arg = new Hashtable<>();
                    arg.put(0, program);
                    String argsStr = toLuaString(arguments);
                    String[] list = midlet.splitArgs(argsStr);
                    for (int i = 0; i < list.length; i++) { 
                        arg.put((double) (i + 1), list[i]); 
                    }
                }
                
                if (arg == null) { 
                    arg = new Hashtable<>(); 
                    arg.put(0, program); 
                }
                
                if (midlet.isPureText(data)) {
                    String code = new String(data, StandardCharsets.UTF_8);
                    if (code.startsWith("#!/bin/sh")) {
                        String[] cmds = midlet.split(code, '\n');
                        int status = 0;
                        for (int i = 0; i < cmds.length; i++) {
                            Vector<Object> payload = new Vector<>();
                            payload.addElement(cmds[i]);
                            status = ((Double) exec(payload)).intValue();
                            if (status != 0) { break; }
                        }

                        result.addElement((double) status);
                    } else {
                        Process process = new Process(midlet, ("lua " + program).trim(), midlet.joinpath(program, scope), midlet.getUser(owner), owner, pid, out, scope);
                        midlet.sys.put(pid, process);
                        Hashtable<String, Object> digest = process.lua.run(program, code, arg);
                        
                        result.addElement((double) ((Integer) digest.get("status")).intValue());
                    }
                } else {
                    // ELF binary - simplified
                    result.addElement(1.0);
                }
                
                result.addElement(out instanceof StringBuffer ? out.toString() : out);
                return result; 
                
            } catch (Exception e) { 
                result.addElement(1.0); 
                return result; 
            }
        }

        public Object chdir(Vector<Object> args) throws Exception {
            if (args.isEmpty()) { 
                return father.get("PWD"); 
            }
            else {
                String pwd = (String) father.get("PWD"), target = midlet.joinpath(toLuaString(args.elementAt(0)), father);
                if (!target.endsWith("/")) {
                    target = target + "/";
                }
                if (target.equals("") || target == null) { 
                    father.put("PWD", "/home/"); 
                    return 0.0; 
                }
                else if (target.equals("..")) {
                    if (pwd.equals("/")) { 
                        return 1.0; 
                    }
                    
                    int lastSlashIndex = pwd.lastIndexOf('/', pwd.endsWith("/") ? pwd.length() - 2 : pwd.length() - 1);
                    father.put("PWD", (lastSlashIndex <= 0) ? "/" : pwd.substring(0, lastSlashIndex + 1));

                    return 0.0;
                }

                // Check if directory exists
                File dir = new File(midlet.getRealPath(target));
                if (dir.exists() && dir.isDirectory()) { 
                    father.put("PWD", target); 
                    return 0.0; 
                }
                else { 
                    return 127.0; 
                }
            }
        }

        public void exit(Vector<Object> args) { 
            if (PID.equals("1")) { 
                midlet.destroyApp(true); 
            } 
            midlet.sys.remove(PID); 
            if (args.isEmpty()) { 
                throw new Error(); 
            } else { 
                status = getNumber(toLuaString(args.elementAt(0)), 1); 
            } 
        }

        public void run() { 
            if (root instanceof LuaFunction) { 
                Vector<Object> arg = new Vector<>(); 
                try { 
                    ((LuaFunction) root).call(arg); 
                } catch (Throwable e) { 
                    midlet.print(midlet.getCatch(e), stdout, id, father); 
                } 
            } 
        }

        // Método auxiliar para criar componentes
        private Object createSwingComponent(Object config) throws Exception {
            if (!(config instanceof Hashtable)) {
                return gotbad(1, "createComponent", "table expected, got " + type(config));
            }
            
            Hashtable<Object, Object> field = (Hashtable<Object, Object>) config;
            String type = getFieldValue(field, "type", "text");
            String value = getFieldValue(field, "value", "");
            String label = getFieldValue(field, "label", "");
            String layout = getFieldValue(field, "layout", "default");
            
            if (type.equals("text") || type.equals("item")) {
                if (layout.equals("button") || type.equals("item")) {
                    JButton button = new JButton(value.isEmpty() ? label : value);
                    if (field.containsKey("root")) {
                        button.addActionListener(e -> {
                            try {
                                Object rootObj = field.get("root");
                                if (rootObj instanceof LuaFunction) {
                                    ((LuaFunction) rootObj).call(new Vector<>());
                                }
                            } catch (Exception ex) {
                                midlet.print(midlet.getCatch(ex), stdout, id, father);
                            }
                        });
                    }
                    return button;
                } else if (layout.equals("link")) {
                    JLabel labelComp = new JLabel("<html><a href='#'>" + (value.isEmpty() ? label : value) + "</a></html>");
                    labelComp.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    return labelComp;
                } else {
                    return new JLabel(value.isEmpty() ? label : value);
                }
            }
            else if (type.equals("choice")) {
                String choiceType = getFieldValue(field, "mode", "exclusive");
                Object options = field.get("options");
                
                Vector<String> items = new Vector<>();
                if (options instanceof Hashtable) {
                    Hashtable<Object, Object> opts = (Hashtable<Object, Object>) options;
                    if (isListTable(opts)) {
                        Vector<Object> fv = toVector(opts);
                        for (int i = 0; i < opts.size(); i++) {
                            items.addElement(toLuaString(fv.elementAt(i)));
                        }
                    } else {
                        for (Enumeration<Object> keys = opts.keys(); keys.hasMoreElements();) {
                            items.addElement(toLuaString(opts.get(keys.nextElement())));
                        }
                    }
                }
                
                if (choiceType.equals("multiple")) {
                    JList<String> list = new JList<>(items.toArray(new String[0]));
                    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                    return list;
                } else {
                    return new JComboBox<>(items.toArray(new String[0]));
                }
            }
            else if (type.equals("field")) {
                String mode = getFieldValue(field, "mode", "");
                int length = getFieldNumber(field, "length", 256);
                JTextField textField = new JTextField(value, Math.min(length, 30));
                if (mode.contains("password")) {
                    textField = new JPasswordField(value, Math.min(length, 30));
                }
                return textField;
            }
            else if (type.equals("spacer")) {
                int width = getFieldNumber(field, "width", 1);
                int height = getFieldNumber(field, "height", 10);
                JPanel spacer = new JPanel();
                spacer.setPreferredSize(new Dimension(width, height));
                spacer.setMaximumSize(new Dimension(width, height));
                return spacer;
            }
            else if (type.equals("gauge")) {
                int maxValue = getFieldNumber(field, "maxValue", 100);
                int val = getFieldNumber(field, "value", 0);
                JProgressBar progress = new JProgressBar(0, maxValue);
                progress.setValue(val);
                progress.setStringPainted(true);
                return progress;
            }
            else if (type.equals("image")) {
                String imgPath = getFieldValue(field, "img", "");
                if (!imgPath.equals("")) {
                    ImageIcon icon = (ImageIcon) midlet.readImg(imgPath, father);
                    if (icon != null) {
                        return new JLabel(icon);
                    }
                }
                return new JLabel("[Image]");
            }
            else {
                return new JLabel(value.isEmpty() ? label : value);
            }
        }
        // Adicionar este método na classe LuaFunction
        private JTextField findTextField(Container container) {
            for (Component comp : container.getComponents()) {
                if (comp instanceof JTextField) {
                    return (JTextField) comp;
                }
                if (comp instanceof Container) {
                    JTextField found = findTextField((Container) comp);
                    if (found != null) {
                        return found;
                    }
                }
            }
            return null;
        }

        // Busca recursiva por todos os JButtons dentro de um container (necessário
        // porque addCommand agora coloca o botão dentro do southPanel/buttonPanel,
        // não mais como filho direto do painel raiz)
        private void findButtonsRecursive(Container container, Vector<JButton> result) {
            for (Component comp : container.getComponents()) {
                if (comp instanceof JButton) {
                    result.addElement((JButton) comp);
                }
                if (comp instanceof Container) {
                    findButtonsRecursive((Container) comp, result);
                }
            }
        }
    }
}