import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// Lua Runtime
public class Lua {
    private boolean breakLoop = false, doreturn = false, kill = true, gc = true;
    private boolean[] attrchanges = new boolean[] { true, true };
    private OpenTTY midlet;
    private String PID = null;
    private long uptime = System.currentTimeMillis();
    private Hashtable globals = new Hashtable(), proc = new Hashtable(), requireCache = new Hashtable();
    private Vector tokens;
    private int id = 1000, tokenIndex, status = 0, loopDepth = 0;
    // |
    public static final int PRINT = 0, ERROR = 1, PCALL = 2, REQUIRE = 3, LOADS = 4, PAIRS = 5, GC = 6, TOSTRING = 7, TONUMBER = 8, SELECT = 9, TYPE = 10, GETPROPERTY = 11, RANDOM = 12, EXEC = 13, GETENV = 14, CLOCK = 15, SETLOC = 16, EXIT = 17, DATE = 18, GETPID = 19, SETPROC = 20, GETPROC = 21, READ = 22, WRITE = 23, CLOSE = 24, TB_INSERT = 25, TB_CONCAT = 26, TB_REMOVE = 27, TB_SORT = 28, TB_MOVE = 29, TB_UNPACK = 30, TB_PACK = 31, TB_DECODE = 32, HTTP_GET = 33, HTTP_POST = 34, CONNECT = 35, PEER = 36, DEVICE = 37, SERVER = 38, ACCEPT = 39, ALERT = 40, SCREEN = 41, LIST = 42, QUEST = 43, EDIT = 44, TITLE = 45, TICKER = 46, WTITLE = 47, DISPLAY = 48, APPEND = 49, UPPER = 50, LOWER = 51, LEN = 52, MATCH = 53, REVERSE = 54, SUB = 55, HASH = 56, BYTE = 57, CHAR = 58, TRIM = 59, GETCWD = 60, OPEN = 61, GETCURRENT = 62, IMG = 63, CLASS = 64, NAME = 65, SETMETATABLE = 66, GETMETATABLE = 67, FIND = 68;
    public static final int EOF = 0, NUMBER = 1, STRING = 2, BOOLEAN = 3, NIL = 4, IDENTIFIER = 5, PLUS = 6, MINUS = 7, MULTIPLY = 8, DIVIDE = 9, MODULO = 10, EQ = 11, NE = 12, LT = 13, GT = 14, LE = 15,  GE = 16, AND = 17, OR = 18, NOT = 19, ASSIGN = 20, IF = 21, THEN = 22, ELSE = 23, END = 24, WHILE = 25, DO = 26, RETURN = 27, FUNCTION = 28, LPAREN = 29, RPAREN = 30, COMMA = 31, LOCAL = 32, LBRACE = 33, RBRACE = 34, LBRACKET = 35, RBRACKET = 36, CONCAT = 37, DOT = 38, ELSEIF = 39, FOR = 40, IN = 41, POWER = 42, BREAK = 43, LENGTH = 44, VARARG = 45, REPEAT = 46, UNTIL = 47, COLON = 48;
    public static final Object LUA_NIL = new Object();
    // |
    private static class Token { int type; Object value; Token(int type, Object value) { this.type = type; this.value = value; } public String toString() { return "Token(type=" + type + ", value=" + value + ")"; } }
    // |
    // Main
    public Lua(OpenTTY midlet, int id) {
        this.midlet = midlet; this.id = id;
        this.tokenIndex = 0; this.PID = midlet.genpid();
        this.proc = midlet.genprocess("lua", id, null);
        
        Hashtable os = new Hashtable(), io = new Hashtable(), string = new Hashtable(), table = new Hashtable(), pkg = new Hashtable(), graphics = new Hashtable(), socket = new Hashtable(), http = new Hashtable(), java = new Hashtable();
        String[] funcs = new String[] { "execute", "getenv", "clock", "setlocale", "exit", "date", "getpid", "setproc", "getproc", "getcwd" }; int[] loaders = new int[] { EXEC, GETENV, CLOCK, SETLOC, EXIT, DATE, GETPID, SETPROC, GETPROC, GETCWD };
        for (int i = 0; i < funcs.length; i++) { os.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("os", os);

        funcs = new String[] { "read", "write", "close", "open" }; loaders = new int[] { READ, WRITE, CLOSE, OPEN };
        for (int i = 0; i < funcs.length; i++) { io.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("io", io);

        funcs = new String[] { "insert", "concat", "remove", "sort", "move", "unpack", "pack", "decode" }; loaders = new int[] { TB_INSERT, TB_CONCAT, TB_REMOVE, TB_SORT, TB_MOVE, TB_UNPACK, TB_PACK, TB_DECODE };
        for (int i = 0; i < funcs.length; i++) { table.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("table", table);

        funcs = new String[] { "get", "post" }; loaders = new int[] { HTTP_GET, HTTP_POST };
        for (int i = 0; i < funcs.length; i++) { http.put(funcs[i], new LuaFunction(loaders[i])); } socket.put("http", http);

        funcs = new String[] { "class", "getName" }; loaders = new int[] { CLASS, NAME };
        for (int i = 0; i < funcs.length; i++) { java.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("java", java);

        funcs = new String[] { "connect", "peer", "device", "server", "accept" }; loaders = new int[] { CONNECT, PEER, DEVICE, SERVER, ACCEPT };
        for (int i = 0; i < funcs.length; i++) { socket.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("socket", socket);

        funcs = new String[] { "Alert", "BuildScreen", "BuildList", "BuildQuest", "BuildEdit", "SetTitle", "SetTicker", "WindowTitle", "display", "append", "getCurrent", "render" }; loaders = new int[] { ALERT, SCREEN, LIST, QUEST, EDIT, TITLE, TICKER, WTITLE, DISPLAY, APPEND, GETCURRENT, IMG };
        for (int i = 0; i < funcs.length; i++) { graphics.put(funcs[i], new LuaFunction(loaders[i])); } graphics.put("xterm", midlet.form); globals.put("graphics", graphics);

        funcs = new String[] { "upper", "lower", "len", "find", "match", "reverse", "sub", "hash", "byte", "char", "trim" }; loaders = new int[] { UPPER, LOWER, LEN, FIND, MATCH, REVERSE, SUB, HASH, BYTE, CHAR, TRIM };
        for (int i = 0; i < funcs.length; i++) { string.put(funcs[i], new LuaFunction(loaders[i])); } globals.put("string", string);

        funcs = new String[] { "print", "error", "pcall", "require", "load", "pairs", "collectgarbage", "tostring", "tonumber", "select", "type", "getAppProperty", "setmetatable", "getmetatable" }; loaders = new int[] { PRINT, ERROR, PCALL, REQUIRE, LOADS, PAIRS, GC, TOSTRING, TONUMBER, SELECT, TYPE, GETPROPERTY, SETMETATABLE, GETMETATABLE };
        for (int i = 0; i < funcs.length; i++) { globals.put(funcs[i], new LuaFunction(loaders[i])); }

        pkg.put("loaded", requireCache); pkg.put("loadlib", new LuaFunction(REQUIRE)); globals.put("package", pkg);
        globals.put("random", new LuaFunction(RANDOM));
        globals.put("_VERSION", "Lua J2ME");
    }
    // | (Run Source code)
    public Hashtable run(String source, String code, Hashtable args) { 
        proc.put("name", ("lua " + source).trim());
        midlet.trace.put(PID, proc); globals.put("arg", args);

        Hashtable ITEM = new Hashtable(); 
        
        try { 
            this.tokens = tokenize(code); 
            
            while (peek().type != EOF) { Object res = statement(globals); if (doreturn) { if (res != null) { ITEM.put("object", res); } doreturn = false; break; } }
        } 
        catch (Exception e) { midlet.echoCommand(midlet.getCatch(e)); status = 1; } 
        catch (Error e) { if (e.getMessage() != null) { midlet.echoCommand(e.getMessage()); } status = 1; }

        if (kill) { midlet.trace.remove(PID); }
        ITEM.put("status", status);
        return ITEM;
    }
    // |
    // Tokenizer
    private Vector tokenize(String code) throws Exception {
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
            else if (c == ':') { tokens.addElement(new Token(COLON, ":")); i++; }

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

            else if (isLetter(c)) { StringBuffer sb = new StringBuffer(); while (i < code.length() && isLetterOrDigit(code.charAt(i))) { sb.append(code.charAt(i)); i++; } String word = sb.toString(); tokens.addElement(new Token((word.equals("true") || word.equals("false")) ? BOOLEAN : word.equals("nil") ? NIL : word.equals("and") ? AND : word.equals("or") ? OR : word.equals("not") ? NOT : word.equals("if") ? IF : word.equals("then") ? THEN : word.equals("else") ? ELSE : word.equals("elseif") ? ELSEIF : word.equals("end") ? END : word.equals("while") ? WHILE : word.equals("do") ? DO : word.equals("return") ? RETURN : word.equals("function") ? FUNCTION : word.equals("local") ? LOCAL : word.equals("for") ? FOR : word.equals("in") ? IN : word.equals("break") ? BREAK : word.equals("repeat") ? REPEAT : word.equals("until") ? UNTIL : IDENTIFIER, word)); }
    
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
        return tokens;
    }
    private Token peek() { if (tokenIndex < tokens.size()) { return (Token) tokens.elementAt(tokenIndex); } return new Token(EOF, "EOF"); }
    private Token peekNext() { if (tokenIndex + 1 < tokens.size()) { return (Token) tokens.elementAt(tokenIndex + 1); } return new Token(EOF, "EOF"); }
    private Token consume() { if (tokenIndex < tokens.size()) { return (Token) tokens.elementAt(tokenIndex++); } return new Token(EOF, "EOF"); }
    private Token consume(int expectedType) throws Exception { Token token = peek(); if (token.type == expectedType) { tokenIndex++; return token; } throw new Exception("Expected token type " + expectedType + " but got " + token.type + " with value " + token.value); }
    // |
    // Statements
    private Object statement(Hashtable scope) throws Exception {
        Token current = peek();

        if (status != 0) { midlet.trace.remove(PID); throw new Error(); }
        if (midlet.trace.containsKey(PID)) { } else { throw new Error("Process killed"); } 

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

                    if (iterSrc instanceof Hashtable) {
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

            if (peek().type == EOF || peek().type == END) return new Vector();

            Vector results = new Vector();
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
                if (token.type == FUNCTION || token.type == IF || token.type == WHILE || token.type == FOR) { depth++; }
                else if (token.type == END) { depth--; }
                else if (token.type == EOF) { throw new RuntimeException("Unmatched 'function' statement: Expected 'end'"); }
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
                    if (token.type == FUNCTION || token.type == IF || token.type == WHILE || token.type == FOR) { depth++; } 
                    else if (token.type == END) { depth--; } 
                    else if (token.type == EOF) { throw new Exception("Unmatched 'function' statement: Expected 'end'"); }

                    if (depth > 0) { bodyTokens.addElement(token); }
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
        else if (current.type == LENGTH) { consume(LENGTH); Object val = factor(scope); if (val == null || val instanceof Boolean) { throw new RuntimeException("attempt to get length of a " + (val == null ? "nil" : "boolean") + " value"); } else if (val instanceof String) { return new Double(((String) val).length()); } else if (val instanceof Hashtable) { return new Double(((Hashtable) val).size()); } else if (val instanceof Vector) { return new Double(((Vector) val).size()); } else { return new Double(0); } }
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

            Vector bodyTokens = new Vector();
            int depth = 1;
            while (depth > 0) {
                Token token = consume();
                if (token.type == FUNCTION || token.type == IF || token.type == WHILE || token.type == FOR) { depth++; }
                else if (token.type == END) { depth--; }
                else if (token.type == EOF) { throw new RuntimeException("Unmatched 'function' statement: Expected 'end'"); }
                if (depth > 0) { bodyTokens.addElement(token); }
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

        Object funcObj = unwrap(scope.get(funcName));
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
    private static boolean isWhitespace(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r'; }
    private static boolean isDigit(char c) { return c >= '0' && c <= '9'; }
    private static boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private static boolean isLetterOrDigit(char c) { return isLetter(c) || isDigit(c); }
    // |
    // Lua Object
    public class LuaFunction implements CommandListener, ItemCommandListener, ItemStateListener {
        private Vector params, bodyTokens;
        private Hashtable closureScope, PKG, ITEM = null, STATE = null; 
        private int MOD = -1, LTYPE = -1;
        // | (Screen)
        private Displayable screen; 
        private Command BACK, USER; 
        private TextField INPUT;
        // | 
        // Config.
        LuaFunction(Vector params, Vector bodyTokens, Hashtable closureScope) { this.params = params; this.bodyTokens = bodyTokens; this.closureScope = closureScope; }
        LuaFunction(int type, Hashtable PKG) { this.MOD = type; this.PKG = PKG; }
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
                if (result instanceof Vector) {
                    returnValue = result;
                } else if (doreturn) {
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

                    midlet.echoCommand(buffer.toString()); 
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

                    String code = midlet.getcontent(name);
                    if (code.equals("")) { throw new Exception("module '" + code + "' not found"); }

                    Object obj = exec(code);
                    requireCache.put(name, (obj == null) ? LUA_NIL : obj);
                    return obj;
                } 
                else { return gotbad(1, "require", "string expected, got " + type(args.elementAt(0))); }
            }
            else if (MOD == LOADS) { if (args.isEmpty() || args.elementAt(0) == null) { } else { return exec(toLuaString(args.elementAt(0))); } }
            else if (MOD == PAIRS) { 
                if (args.isEmpty()) { return gotbad(1, "pairs", "table expected, got no value"); } 
                else {
                    Object t = args.elementAt(0);
                    t = (t == LUA_NIL) ? null : t;
                    if (t == null || t instanceof Hashtable || t instanceof Vector) { return t; }
                    else { return gotbad(1, "pairs", "table expected, got " + type(t)); }
                }
            }
            else if (MOD == GC) {
                if (args.isEmpty()) { System.gc(); }
                else {
                    String opt = toLuaString(args.elementAt(0));

                    if (opt.equals("stop")) { gc = false; }
                    else if (opt.equals("collect") || opt.equals("restart")) { System.gc(); }
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
            else if (MOD == RANDOM) { Double gen = new Double(midlet.random.nextInt(midlet.getNumber(args.isEmpty() ? "100" : toLuaString(args.elementAt(0)), 100, false))); return args.isEmpty() ? new Double(gen.doubleValue() / 100) : gen; }
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
            else if (MOD == EXEC) { if (args.isEmpty()) { } else { return new Double(midlet.processCommand(toLuaString(args.elementAt(0)), true, id)); } }
            else if (MOD == GETENV) { return args.isEmpty() ? gotbad(1, "getenv", "string expected, got no value") : midlet.attributes.get(toLuaString(args.elementAt(0))); }
            else if (MOD == CLOCK) { return System.currentTimeMillis() - uptime; }
            else if (MOD == SETLOC) { if (args.isEmpty()) { } else { midlet.attributes.put("LOCALE", toLuaString(args.elementAt(0))); } }
            else if (MOD == EXIT) { midlet.trace.remove(PID); if (args.isEmpty()) { throw new Error(); } else { status = midlet.getNumber(toLuaString(args.elementAt(0)), 1, false); } }
            else if (MOD == DATE) { return new java.util.Date().toString(); }
            else if (MOD == GETPID) { return args.isEmpty() ? PID : midlet.getpid(toLuaString(args.elementAt(0))); }
            else if (MOD == SETPROC) {
                if (args.isEmpty()) { }
                else if (args.elementAt(0) instanceof Boolean) { kill = ((Boolean) args.elementAt(0)).booleanValue(); }
                else {                    
                    String attribute = toLuaString(args.elementAt(0)).trim().toLowerCase();
                    Object value = args.size() < 2 ? null : args.elementAt(1);

                    if (attribute.equals("owner")) { return gotbad(1, "setproc", "permission denied"); } 
                    else if (attribute.equals("name") || attribute.equals("collector")) {
                        if (value == null) { return gotbad(2, "setproc", "value expected, got nil"); }
                        else {
                            if (attribute.equals("name")) { if (attrchanges[0]) { proc.put("name", toLuaString(value)); attrchanges[0] = false; } } 
                            else { if (attrchanges[1]) { proc.put("collector", toLuaString(value)); attrchanges[1] = false; } }
                        }
                    } 
                    else { if (value == null) { proc.remove(attribute); } else { proc.put(attribute, value); } }
                } 
            }
            else if (MOD == GETPROC) {
                if (args.isEmpty()) { }
                else {
                    String process = toLuaString(args.elementAt(0)).trim();

                    if (midlet.trace.containsKey(process)) {
                        if (!((String) midlet.getobject(process, "owner")).equals(midlet.username) && id != 0) { return gotbad(1, "getproc", "permissiond denied"); }

                        if (args.size() > 1) { return (midlet.getprocess(process)).get(toLuaString(args.elementAt(1)).trim()); } 
                        else { return gotbad(2, "getproc", "field expected, got no value"); }
                    } 
                }
            }
            else if (MOD == GETCWD) { return midlet.path; }
            // Package: io
            else if (MOD == READ) {
                if (args.isEmpty()) { return midlet.stdout.getText(); }
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
                    else if (arg instanceof OutputStream) { return gotbad(1, "read", "input stream expected, got output"); } 
                    else {
                        String target = toLuaString(arg);
                        return target.equals("stdout") ? midlet.stdout.getText() : target.equals("stdin") ? midlet.stdin.getString() : midlet.getcontent(target);
                    }
                }
            }
            else if (MOD == WRITE) {
                if (args.isEmpty()) { }
                else {
                    String content = toLuaString(args.elementAt(0)), out = args.size() == 1 ? "stdout" : toLuaString(args.elementAt(1));
                    boolean mode = args.size() > 2 && toLuaString(args.elementAt(2)).equals("a") ? true : false;

                    if (args.size() > 1 && args.elementAt(1) instanceof InputStream) { return gotbad(2, "write", "output stream expected, got input"); }  
                    else if (args.size() > 1 && args.elementAt(1) instanceof OutputStream) { OutputStream out = (OutputStream) args.elementAt(1); out.write(content.getBytes("UTF-8")); out.flush(); }
                    else if (args.elementAt(0) instanceof OutputStream) { OutputStream o = (OutputStream) args.elementAt(0); o.write(out.getBytes("UTF-8")); o.flush(); }
                    else {
                        if (out.equals("nano")) { midlet.nanoContent = mode ? midlet.nanoContent + content : content; }
                        else { return midlet.writeRMS(out, mode ? midlet.getcontent(out) + content : content, id); }
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
                        else { return gotbad(i + 1, "close", "stream expected, got " + type(arg)); }
                    }
                } 
            }
            else if (MOD == OPEN) { return args.isEmpty() ? gotbad(1, "open", "string expected, got no value") : midlet.readRaw(toLuaString(args.elementAt(0))); }
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
            else if (MOD == TB_DECODE) { return args.isEmpty() ? null : midlet.parseProperties((String) args.elementAt(0)); }
            else if (MOD == TB_PACK) {
                Hashtable packed = new Hashtable();
                for (int i = 0; i < args.size(); i++) {
                    Object val = args.elementAt(i);
                    packed.put(new Double(i + 1), val == null ? LUA_NIL : val);
                }
                packed.put("n", new Double(args.size()));
                return packed;
            }
            // Package: socket.http
            else if (MOD == HTTP_GET || MOD == HTTP_POST) { return (args.isEmpty() || args.elementAt(0) == null ? gotbad(1, MOD == HTTP_GET ? "get" : "post", "string expected, got no value") : (MOD == HTTP_GET ? http("GET", toLuaString(args.elementAt(0)), null, args.size() > 1 ? (Hashtable) args.elementAt(1) : null) : http("POST", toLuaString(args.elementAt(0)), args.size() > 1 ? toLuaString(args.elementAt(1)) : "", args.size() > 2 ? args.elementAt(2) : null))); }            
            // Package: socket
            else if (MOD == CONNECT) {
                if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "connect", "string expected, got no value"); }
                else {
                    Vector result = new Vector();

                    SocketConnection conn = (SocketConnection) Connector.open(toLuaString(args.elementAt(0)));
                        
                    result.addElement(conn);
                    result.addElement(conn.openInputStream());
                    result.addElement(conn.openOutputStream());

                    return result;
                } 
            }
            else if (MOD == PEER || MOD == DEVICE) {
                if (args.isEmpty()) { return gotbad(1, MOD == PEER ? "peer" : "device", "connection expected, got no value"); }
                else {
                    if (args.elementAt(0) instanceof SocketConnection) {
                        SocketConnection conn = (SocketConnection) args.elementAt(0);

                        return toLuaString(MOD == PEER ? conn.getAddress(): conn.getLocalAddress());
                    } else { return gotbad(1, MOD == PEER ? "peer" : "device", "connection expected, got " + type(args.elementAt(0))); }
                }
            }
            else if (MOD == SERVER) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof Double)) { return gotbad(1, "server" , "number expected, got " + (args.isEmpty() ? "no value" : type(args.elementAt(0)))); }
                else { return Connector.open("socket://:" + toLuaString(args.elementAt(0))); }
            }
            else if (MOD == ACCEPT) {
                if (args.isEmpty() || !(args.elementAt(0) instanceof ServerSocketConnection)) { return gotbad(1, "server" , "server expected, got " + (args.isEmpty() ? " no value" : type(args.elementAt(0)))); }
                else {
                    Vector result = new Vector();
                    SocketConnection conn = (SocketConnection) ((ServerSocketConnection) args.elementAt(0)).acceptAndOpen();
                        
                    result.addElement(conn);
                    result.addElement(conn.openInputStream());
                    result.addElement(conn.openOutputStream());
                    return result;
                }
            }
            // Package: graphics
            else if (MOD == ALERT || MOD == SCREEN || MOD == LIST || MOD == QUEST || MOD == EDIT) {
                if (args.isEmpty()) { }
                else {
                    Object table = args.elementAt(0);

                    if (table instanceof Hashtable) { return ((LuaFunction) new LuaFunction(MOD, (Hashtable) table)).BuildScreen(); }
                    else { return gotbad(1, MOD == ALERT ? "Alert" : MOD == SCREEN ? "BuildScreen" : MOD == LIST ? "BuildList" : MOD == QUEST ? "BuildQuest" : "BuildEdit", "table expected, got " + type(table)); }
                }
            }
            else if (MOD == TITLE || MOD == WTITLE || MOD == TICKER) {
                String label = args.isEmpty() || args.elementAt(0) == null ? null : toLuaString(args.elementAt(0));

                if (MOD == TITLE) { midlet.form.setTitle(label); }
                else if (MOD == WTITLE) { if (args.size() > 1) { Object obj2 = args.elementAt(1); if (obj2 instanceof Displayable) { } else { return gotbad(2, "WindowTitle", "screen expected, got" + type(obj2)); } } else { midlet.display.getCurrent().setTitle(label); } }
                else { midlet.display.getCurrent().setTicker(label == null ? null : new Ticker(label)); }
            }
            else if (MOD == DISPLAY) {
                if (args.isEmpty()) { }
                else {
                    Object screen = args.elementAt(0);

                    if (screen instanceof Displayable) { kill = false; midlet.display.setCurrent((Displayable) screen); }
                    else { return gotbad(1, "display", "screen expected, got " + type(screen)); }
                }
            }
            else if (MOD == APPEND) {
                if (args.size() < 2) { return gotbad(1, "append", "wrong number of arguments"); }
                else {
                    Object obj1 = args.elementAt(0), obj2 = args.elementAt(1);
                    if (obj1 instanceof Form) {
                        if (obj2 instanceof Hashtable || obj2 instanceof String) { AppendScreen((Form) obj1, obj2); }
                        else { return gotbad(2, "append", "string expected, got " + type(obj2)); }
                    }
                    else if (obj1 instanceof List) { ((List) obj1).append(toLuaString(obj2), null); }
                }
            }
            else if (MOD == GETCURRENT) { return midlet.display.getCurrent(); }
            else if (MOD == IMG) { return args.isEmpty() || args.elementAt(0) == null ? gotbad(1, "render", "string expected, got" + type(args.elementAt(0))) : midlet.readImg(toLuaString(args.elementAt(0))); }
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

                        int len = text.length(), start = midlet.getNumber(toLuaString(args.elementAt(1)), 1, false), end = args.size() > 2 ? midlet.getNumber(toLuaString(args.elementAt(2)), len, false) : len;

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
                    if (args.size() >= 2) { start = midlet.getNumber(toLuaString(args.elementAt(1)), 1, false); }
                    if (args.size() >= 3) { end = midlet.getNumber(toLuaString(args.elementAt(2)), start, false); }
                    
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
            // Package: java
            else if (MOD == CLASS) { if (args.isEmpty() || args.elementAt(0) == null) { return gotbad(1, "class", "string expected, got no value"); } else { return new Boolean(midlet.javaClass(toLuaString(args.elementAt(0))) == 0); } }
            else if (MOD == NAME) { return midlet.getName(); }  

            return null;
        }
        // |
        private Object exec(String code) throws Exception { int savedIndex = tokenIndex; Vector savedTokens = tokens; Object ret = null; try { tokens = tokenize(code); tokenIndex = 0; Hashtable modScope = new Hashtable(); for (Enumeration e = globals.keys(); e.hasMoreElements();) { String k = (String) e.nextElement(); modScope.put(k, unwrap(globals.get(k))); } while (peek().type != EOF) { Object res = statement(modScope); if (doreturn) { ret = res; doreturn = false; break; } } } finally { tokenIndex = savedIndex; tokens = savedTokens; } return ret; }
        public static String type(Object item) { return item == null || item == LUA_NIL ? "nil" : item instanceof String ? "string" : item instanceof Double ? "number" : item instanceof Boolean ? "boolean" : item instanceof LuaFunction ? "function" : item instanceof Hashtable ? "table" : item instanceof InputStream || item instanceof OutputStream ? "stream" : item instanceof SocketConnection || item instanceof StreamConnection ? "connection" : item instanceof ServerSocketConnection ? "server" : item instanceof Displayable ? "screen" : item instanceof Image ? "image" : "userdata"; }
        private Object gotbad(int pos, String name, String expect) throws Exception { throw new RuntimeException("bad argument #" + pos + " to '" + name + "' (" + expect + ")"); }
        private Object gotbad(String name, String field, String expected) throws Exception { throw new RuntimeException(name + " -> field '" + field + "' (" + expected + ")"); }
        private Object http(String method, String url, String data, Object item) throws Exception {
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

                is = conn.openInputStream();
                baos = new ByteArrayOutputStream();
                int ch;
                while ((ch = is.read()) != -1) { baos.write(ch); }

                Vector result = new Vector();
                result.addElement(new String(baos.toByteArray(), "UTF-8"));
                result.addElement(new Double(conn.getResponseCode()));
                return result;
            } 
            catch (Exception e) { throw e; } 
            finally {
                if (is != null) { try { is.close(); } catch (Exception e) { } }
                if (conn != null) { try { conn.close(); } catch (Exception e) { } }
                if (baos != null) { try { baos.close(); } catch (Exception e) { } }
            }
        }
        private int compareLua(Object a, Object b) { if (a == null && b == null) { return 0; } if (a == null) { return -1; } if (b == null) { return 1; } if (a instanceof Double && b instanceof Double) { double da = ((Double) a).doubleValue(), db = ((Double) b).doubleValue(); return da < db ? -1 : (da > db ? 1 : 0); } String sa = toLuaString(a), sb = toLuaString(b); return sa.compareTo(sb); }
        // |
        private boolean isListTable(Hashtable table) {
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
        private Vector toVector(Hashtable table) throws Exception { Vector vec = new Vector(); if (table == null) { return vec; } for (int i = 1; i <= table.size(); i++) { vec.addElement(table.get(new Double(i))); } return vec; }

        private void AppendScreen(Form f, Object obj) throws Exception {
            if (obj instanceof Hashtable) {
                Hashtable field = (Hashtable) obj;
                String type = getenv(field, "type", "text").trim();

                if (type.equals("image")) {
                    String imgPath = getenv(field, "img", "");
                    
                    if (imgPath.equals("")) { } 
                    else { f.append(midlet.readImg(imgPath)); }
                } 
                else if (type.equals("text")) {
                    String value = getenv(field, "value", ""), layout = getenv(field, "layout", "default");
                    if (value.equals("")) { }
                    else { StringItem si = new StringItem(getenv(field, "label", ""), value, layout.equals("link") ? StringItem.HYPERLINK : layout.equals("button") ? StringItem.BUTTON : Item.LAYOUT_DEFAULT); si.setFont(midlet.newFont(getenv(field, "style", "default"))); f.append(si); }
                } 
                else if (type.equals("item")) {
                    String label = field.containsKey("label") ? toLuaString(field.get("label")) : (String) gotbad("BuildScreen", "item", "missing label");
                    Object rootObj = field.containsKey("root") ? field.get("root") : gotbad("BuildScreen", "item", "missing root"); 

                    Command RUN = new Command(label, Command.ITEM, 1); 
                    StringItem s = new StringItem(null, label, StringItem.BUTTON); 
                    s.setFont(midlet.newFont(field.containsKey("style") ? toLuaString(field.get("style")) : "default"));
                    s.setLayout(Item.LAYOUT_EXPAND | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_NEWLINE_BEFORE); 
                    s.addCommand(RUN); 
                    s.setDefaultCommand(RUN); 
                    s.setItemCommandListener(this); 
                    f.append(s);

                    if (ITEM == null) { ITEM = new Hashtable(); }
                    ITEM.put(s, rootObj);
                }
                else if (type.equals("spacer")) { int w = field.containsKey("width") ? field.get("width") instanceof Double ? ((Double) field.get("width")).intValue() : 1 : 1, h = field.containsKey("heigth") ? field.get("heigth") instanceof Double ? ((Double) field.get("heigth")).intValue() : 10 : 10; f.append(new Spacer(w, h)); }
                else if (type.equals("gauge")) { 
                    Gauge g = new Gauge(getvalue(field, "label", ""), getBoolean(field, "interactive", true), getNumber(field, "max", 100), getNumber(field, "value", 0));
                    f.setItemStateListener(this);
                    f.append(g);
                    if (STATE == null) { STATE = new Hashtable(); }
                    STATE.put(g, field.containsKey("root") ? field.get("root") : LUA_NIL);
                } 
                else if (type.equals("field")) { f.append(new TextField(getvalue(field, "label", midlet.stdin.getLabel()), getvalue(field, "value", ""), getNumber(field, "length", 256), getQuest(getenv(field, "mode", "default")))); }
                else if (type.equals("choice")) { 
                    String choiceType = getvalue(field, "mode", "exclusive");
                    ChoiceGroup cg = new ChoiceGroup(getvalue(field, "label", ""), (LTYPE = choiceType.equals("exclusive") ? Choice.EXCLUSIVE : choiceType.equals("multiple") ? Choice.MULTIPLE : Choice.POPUP));
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

                    f.setItemStateListener(this);
                    f.append(cg);

                    if (ITEM == null) { ITEM = new Hashtable(); }
                    if (STATE == null) { STATE = new Hashtable(); }
                    ITEM.put(cg, new Double(LTYPE));
                    STATE.put(cg, field.containsKey("root") ? field.get("root") : LUA_NIL);
                } 
            } 
            else if (obj instanceof String) { f.append(toLuaString(obj)); }
        }
        
        private int getNumber(Hashtable table, String key, int fallback) { Object val = table.get(key); if (val instanceof Double) { return ((Double) val).intValue(); } try { return Integer.parseInt(toLuaString(val)); } catch (Exception e) { return fallback; } }
        private boolean getBoolean(Hashtable table, String key, boolean fallback) { Object val = table.get(key); if (val instanceof Boolean) { return ((Boolean) val).booleanValue(); } return toLuaString(val).equalsIgnoreCase("true") ? true : fallback; } private String getvalue(Hashtable table, String key, String fallback) { return table.containsKey(key) ? toLuaString(table.get(key)) : fallback; } 
        private String getenv(Hashtable table, String key, String fallback) { return midlet.env(getvalue(table, key, fallback)); }
        private int getQuest(String mode) { if (mode == null || mode.length() == 0) { return TextField.ANY; } boolean password = false; if (mode.indexOf("password") != -1) { password = true; mode = midlet.replace(mode, "password", "").trim(); } int base = mode.equals("number") ? TextField.NUMERIC : mode.equals("email") ? TextField.EMAILADDR : mode.equals("phone") ? TextField.PHONENUMBER : mode.equals("decimal") ? TextField.DECIMAL : TextField.ANY; return password ? (base | TextField.PASSWORD) : base; } 

        private Object BuildScreen() throws Exception {
            if (MOD == ALERT) {
                Alert alert = new Alert(getenv(PKG, "title", midlet.form.getTitle()), getenv(PKG, "message", ""), null, null);
                if (PKG.containsKey("indicator")) { alert.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING)); }

                Object backObj = PKG.get("back");
                Hashtable backTable = (backObj instanceof Hashtable) ? (Hashtable) backObj : null;
                String backLabel = backTable != null ? getenv(backTable, "label", "Back") : "Back";
                alert.addCommand(BACK = new Command(backLabel, Command.BACK, 1));

                Object buttonObj = PKG.get("button");
                Hashtable buttonTable = (buttonObj instanceof Hashtable) ? (Hashtable) buttonObj : null;
                if (buttonTable != null) { alert.addCommand(USER = new Command(getenv(buttonTable, "label", "OK"), Command.SCREEN, 2)); }
                
                Object timeoutObj = PKG.get("timeout");
                alert.setTimeout(timeoutObj instanceof Double ? ((Double) timeoutObj).intValue() : Alert.FOREVER);
            
                this.screen = alert;
            } 
            else if (MOD == SCREEN) {
                Form form = new Form(getenv(PKG, "title", midlet.form.getTitle()));

                Object backObj = PKG.get("back"), buttonObj = PKG.get("button");
                Hashtable backTable = (backObj instanceof Hashtable) ? (Hashtable) backObj : null, buttonTable = (buttonObj instanceof Hashtable) ? (Hashtable) buttonObj : null;
                String backLabel = backTable != null ? getenv(backTable, "label", "Back") : "Back";
                form.addCommand(BACK = new Command(backLabel, Command.OK, 1));

                if (buttonTable != null) { form.addCommand(USER = new Command(getenv(buttonTable, "label", "Menu"), Command.SCREEN, 2)); }

                Object fieldsObj = PKG.get("fields");
                if (fieldsObj != null) {
                    if (fieldsObj instanceof Hashtable) {
                        Hashtable fields = (Hashtable) fieldsObj;

                        if (isListTable(fields)) { Vector fv = toVector(fields); for (int i = 0; i < fields.size(); i++) { AppendScreen(form, fv.elementAt(i)); } } 
                        else { for (Enumeration keys = fields.keys(); keys.hasMoreElements();) { AppendScreen(form, fields.get(keys.nextElement())); } }
                    } 
                    else { return gotbad("BuildScreen", "fields", "table expected, got " + type(fieldsObj)); }
                }

                this.screen = form;
            } 
            else if (MOD == LIST) {
                String ListType = getenv(PKG, "type", "implict");
                List list = new List(getenv(PKG, "title", midlet.form.getTitle()), (LTYPE = ListType.equals("exclusive") ? List.EXCLUSIVE : ListType.equals("multiple") ? List.MULTIPLE : List.IMPLICIT));

                Object backObj = PKG.get("back"), buttonObj = PKG.get("button");
                Hashtable backTable = (backObj instanceof Hashtable) ? (Hashtable) backObj : null, buttonTable = (buttonObj instanceof Hashtable) ? (Hashtable) buttonObj : null;
                list.addCommand(BACK = new Command(backTable != null ? getenv(backTable, "label", "Back") : "Back", Command.BACK, 1));
                if (buttonTable != null) { list.addCommand(USER = new Command(getenv(buttonTable, "label", "Menu"), Command.SCREEN, 2)); }

                Object fieldsObj = PKG.get("fields");
                if (fieldsObj != null) {
                    Image IMG = null;
                    if (PKG.containsKey("icon")) {
                        if (PKG.get("icon") instanceof Image) { IMG = (Image) PKG.get("icon"); }
                        else { IMG = midlet.readImg(getenv(PKG, "icon", "")); }
                    }

                    if (fieldsObj instanceof Hashtable) {
                        Hashtable fields = (Hashtable) fieldsObj;

                        if (isListTable(fields)) {
                            Vector fv = toVector(fields);

                            for (int i = 0; i < fields.size(); i++) { list.append(toLuaString(fv.elementAt(i)), IMG); }
                        } else {
                            for (Enumeration keys = fields.keys(); keys.hasMoreElements();) {
                                list.append(toLuaString(fields.get(keys.nextElement())), IMG);
                            }
                        }
                    } else { return gotbad("BuildList", "fields", "table expected, got " + type(fieldsObj)); }
                }

                this.screen = list;
            } 
            else if (MOD == QUEST) {
                Form form = new Form(getenv(PKG, "title", midlet.form.getTitle()));
                TextField field = new TextField(getenv(PKG, "label", midlet.stdin.getLabel()), getenv(PKG, "content", ""), 256, getQuest(getenv(PKG, "type", "default")));
                form.append(field);

                Object backObj = PKG.get("back"), buttonObj = PKG.get("button");
                Hashtable backTable = (backObj instanceof Hashtable) ? (Hashtable) backObj : null, buttonTable = (buttonObj instanceof Hashtable) ? (Hashtable) buttonObj : (Hashtable) gotbad("BuildQuest", "button", "table expected, got " + type(buttonObj));
                form.addCommand(BACK = new Command(backTable != null ? getenv(backTable, "label", "Back") : "Back", Command.SCREEN, 2));

                if (buttonTable != null) { form.addCommand(USER = new Command(getenv(buttonTable, "label", "Menu"), Command.SCREEN, 2)); }

                this.INPUT = field;
                this.screen = form;
            } 
            else if (MOD == EDIT) {
                TextBox box = new TextBox(getenv(PKG, "title", midlet.form.getTitle()), getenv(PKG, "content", ""), 31522, getQuest(getenv(PKG, "type", "default")));

                Object backObj = PKG.get("back"), buttonObj = PKG.get("button");
                Hashtable backTable = (backObj instanceof Hashtable) ? (Hashtable) backObj : null, buttonTable = (buttonObj instanceof Hashtable) ? (Hashtable) buttonObj : (Hashtable) gotbad("BuildEdit", "button", "table expected, got " + type(buttonObj));
                box.addCommand(BACK = new Command(backTable != null ? getenv(backTable, "label", "Back") : "Back", Command.BACK, 1));
                if (buttonTable != null) { box.addCommand(USER = new Command(getenv(buttonTable, "label", "Menu"), Command.SCREEN, 2)); }

                this.screen = box;
            }

            this.screen.setCommandListener(this);
            return this.screen;
        }
        public void commandAction(Command c, Displayable d) {
            try {
                if (c == BACK) {
                    midlet.processCommand("xterm", true, id);
    
                    Hashtable backTable = (Hashtable) PKG.get("back");
                    if (backTable != null) {
                        Object back = backTable.get("root");
                        if (back instanceof LuaFunction) { ((LuaFunction) back).call(new Vector()); } 
                        else if (back != null) { midlet.processCommand(toLuaString(back), true, id); }
                    }
                } 
                else if (c == USER || c == List.SELECT_COMMAND) {
                    Object fire = PKG.get("button") == null ? null : ((Hashtable) PKG.get("button")).get("root");
    
                    if (MOD == ALERT) {
                        midlet.processCommand("xterm", true, id);
    
                        if (fire instanceof LuaFunction) { ((LuaFunction) fire).call(new Vector()); } 
                        else if (fire != null) { midlet.processCommand(toLuaString(fire), true, id); }
                    } 
                    else if (MOD == QUEST) {
                        String value = INPUT.getString().trim();
                        if (!value.equals("")) {
                            midlet.processCommand("xterm", true, id);
                            if (fire instanceof LuaFunction) {
                                Vector result = new Vector();
                                result.addElement(midlet.env(value));
                                ((LuaFunction) fire).call(result);
                            } else if (fire != null) {
                                midlet.attributes.put(getenv(PKG, "key", ""), midlet.env(value));
                                midlet.processCommand(toLuaString(fire), true, id);
                            }
                        }
                    } 
                    else if (MOD == EDIT) {
                        String value = ((TextBox) screen).getString().trim();
                        if (!value.equals("")) {
                            midlet.processCommand("xterm", true, id);
                            if (fire instanceof LuaFunction) {
                                Vector result = new Vector();
                                result.addElement(midlet.env(value));
                                ((LuaFunction) fire).call(result);
                            } else if (fire != null) {
                                midlet.attributes.put(getenv(PKG, "key", ""), midlet.env(value));
                                midlet.processCommand(toLuaString(fire), true, id);
                            }
                        }
                    } 
                    else if (MOD == LIST) {
                        List list = (List) screen;
                        
                        if (LTYPE == List.MULTIPLE) {
                            Vector args = new Vector();
                            for (int i = 0; i < list.size(); i++) { if (list.isSelected(i)) { args.addElement(midlet.env(list.getString(i))); } }
    
                            if (args.size() > 0) {
                                midlet.processCommand("xterm", true, id);
                                if (fire instanceof LuaFunction) { ((LuaFunction) fire).call(args); } 
                                else {
                                    for (int i = 0; i < args.size(); i++) {
                                        String key = args.elementAt(i).toString();
                                        midlet.processCommand(getvalue(PKG, key, "log add warn An error occurred, '" + key + "' not found"), true, id);
                                    }
                                }
                            }
                        } else {
                            int index = list.getSelectedIndex();
                            if (index >= 0) {
                                midlet.processCommand("xterm", true, id);
                                String key = list.getString(index);
    
                                if (fire instanceof LuaFunction) {
                                    Vector args = new Vector();
                                    args.addElement(midlet.env(key));
                                    ((LuaFunction) fire).call(args);
                                } else if (fire != null) { midlet.processCommand(getvalue(PKG, key, "log add warn An error occurred, '" + key + "' not found"), true, id); }
                            }
                        }
                    }
                    else if (MOD == SCREEN) {
                        Vector formData = new Vector();
                        if (screen instanceof Form) {
                            Form form = (Form) screen;
                            for (int i = 0; i < form.size(); i++) {
                                Item item = form.get(i);

                                if (item instanceof TextField) { formData.addElement(((TextField) item).getString()); } 
                                else if (item instanceof Gauge) { formData.addElement(new Double(((Gauge) item).getValue())); }
                                else if (item instanceof ChoiceGroup) {
                                    ChoiceGroup cg = (ChoiceGroup) item;

                                    if (((Double) ITEM.get(cg)).intValue() == Choice.MULTIPLE) {
                                        Hashtable selTable = new Hashtable();
                                        for (int j = 0; j < cg.size(); j++) { selTable.put(new Double(j + 1), new Boolean(cg.isSelected(j))); }

                                        formData.addElement(selTable);
                                    } else {
                                        int sel = cg.getSelectedIndex();
                                        formData.addElement(sel >= 0 ? cg.getString(sel) : LUA_NIL);
                                    }
                                }
                            }
                        }
                        
                        midlet.processCommand("xterm", true, id);
                        if (fire instanceof LuaFunction) { ((LuaFunction) fire).call(formData); } 
                        else if (fire != null) { midlet.processCommand(toLuaString(fire), true, id); }
                    }
                }
            }
            catch (Exception e) { midlet.echoCommand(midlet.getCatch(e)); midlet.trace.remove(PID); } 
            catch (Error e) { midlet.trace.remove(PID); }
        }
        public void commandAction(Command c, Item item) { try { Object fire = ITEM.get(item); if (fire instanceof LuaFunction) { ((LuaFunction) fire).call(new Vector()); } else if (fire != null) { midlet.processCommand(toLuaString(fire), true, id); } } catch (Exception e) { midlet.echoCommand(midlet.getCatch(e)); midlet.trace.remove(PID); } catch (Error e) { midlet.trace.remove(PID); } }
        public void itemStateChanged(Item item) {
            try {
                Object fire = STATE.get(item); 
                
                if (fire == LUA_NIL) { }
                else if (fire instanceof LuaFunction) { 
                    Vector args = new Vector();

                    if (item instanceof ChoiceGroup) {
                        ChoiceGroup cg = (ChoiceGroup) item;

                        if (((Double) ITEM.get(cg)).intValue() == Choice.MULTIPLE) { for (int j = 0; j < cg.size(); j++) { args.addElement(new Boolean(cg.isSelected(j))); } } 
                        else { int sel = cg.getSelectedIndex(); args.addElement(sel >= 0 ? cg.getString(sel) : LUA_NIL); }
                    }
                    else if (item instanceof Gauge) { args.addElement(new Double(((Gauge) item).getValue())); }

                    ((LuaFunction) fire).call(args); 
                } 
                else if (fire != null) { midlet.echoCommand(toLuaString(fire)); } 
            } catch (Exception e) { midlet.echoCommand(midlet.getCatch(e)); midlet.trace.remove(PID); } catch (Error e) { midlet.trace.remove(PID); } 
        }
    }
} 
// |
// EOF