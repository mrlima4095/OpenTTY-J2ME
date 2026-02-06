import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.util.Base64;
import java.util.concurrent.*;

public class Lua {
    public boolean breakLoop = false, doreturn = false, kill = true, gc = true;
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0;
    public Hashtable<String, Object> globals = new Hashtable<>(), father;
    public Hashtable<String, Object> requireCache = new Hashtable<>(), labels = new Hashtable<>();
    public Vector<Token> tokens;
    
    // Token types
    public static final int EOF = 0, NUMBER = 1, STRING = 2, BOOLEAN = 3, NIL = 4, 
        IDENTIFIER = 5, PLUS = 6, MINUS = 7, MULTIPLY = 8, DIVIDE = 9, 
        MODULO = 10, EQ = 11, NE = 12, LT = 13, GT = 14, LE = 15, GE = 16, 
        AND = 17, OR = 18, NOT = 19, ASSIGN = 20, IF = 21, THEN = 22, 
        ELSE = 23, END = 24, WHILE = 25, DO = 26, RETURN = 27, FUNCTION = 28, 
        LPAREN = 29, RPAREN = 30, COMMA = 31, LOCAL = 32, LBRACE = 33, 
        RBRACE = 34, LBRACKET = 35, RBRACKET = 36, CONCAT = 37, DOT = 38, 
        ELSEIF = 39, FOR = 40, IN = 41, POWER = 42, BREAK = 43, LENGTH = 44, 
        VARARG = 45, REPEAT = 46, UNTIL = 47, COLON = 48, LABEL = 49, GOTO = 50;
    
    // Function types
    public static final int PRINT = 0, ERROR = 1, PCALL = 2, REQUIRE = 3, 
        LOADS = 4, PAIRS = 5, GC = 6, TOSTRING = 7, TONUMBER = 8, SELECT = 9, 
        TYPE = 10, GETPROPERTY = 11, SETMETATABLE = 12, GETMETATABLE = 13, 
        IPAIRS = 14, RANDOM = 15;
    
    // OS functions
    public static final int EXEC = 300, GETENV = 301, SETENV = 302, CLOCK = 303, 
        SETLOC = 304, EXIT = 305, DATE = 306, GETPID = 307, SETPROC = 308, 
        GETPROC = 309, GETCWD = 310, GETUID = 311, CHDIR = 312, REQUEST = 313, 
        START = 314, STOP = 315, PREQ = 316, SU = 318, REMOVE = 319, 
        SCOPE = 320, JOIN = 321, MKDIR = 322;
    
    // IO functions
    public static final int READ = 400, WRITE = 401, CLOSE = 402, OPEN = 403, 
        POPEN = 404, DIRS = 405, SETOUT = 406, MOUNT = 407, GEN = 408, COPY = 409;
    
    // String functions
    public static final int UPPER = 100, LOWER = 101, LEN = 102, FIND = 103, 
        MATCH = 104, REVERSE = 105, SUB = 106, HASH = 107, BYTE = 108, 
        CHAR = 109, TRIM = 110, SPLIT = 111, UUID = 112, GETCMD = 113, 
        GETARGS = 114, ENV = 115, BASE64_ENCODE = 116, BASE64_DECODE = 117, 
        GETPATTERN = 118;
    
    // Table functions
    public static final int TB_INSERT = 200, TB_CONCAT = 201, TB_REMOVE = 202, 
        TB_SORT = 203, TB_MOVE = 204, TB_UNPACK = 205, TB_PACK = 206, 
        TB_DECODE = 207;
    
    // HTTP functions
    public static final int HTTP_GET = 500, HTTP_POST = 501, CONNECT = 502, 
        PEER = 503, DEVICE = 504, SERVER = 505, ACCEPT = 506, HTTP_RGET = 507, 
        HTTP_RPOST = 508;
    
    // Graphics functions (simplified for SE)
    public static final int DISPLAY = 600, NEW = 601, RENDER = 602, APPEND = 603, 
        ADDCMD = 604, HANDLER = 605, GETCURRENT = 606, TITLE = 607, TICKER = 608, 
        VIBRATE = 609, SETLABEL = 610, SETTEXT = 611, GETLABEL = 612, 
        GETTEXT = 613, CLEAR_SCREEN = 614;
    
    // Java functions
    public static final int CLASS = 700, NAME = 701, DELETE = 702, UPTIME = 703, 
        RUN = 704, THREAD = 705, SLEEP = 706, KERNEL = 1000;
    
    // Audio functions
    public static final int AUDIO_LOAD = 800, AUDIO_PLAY = 801, AUDIO_PAUSE = 802, 
        AUDIO_VOLUME = 803, AUDIO_DURATION = 804, AUDIO_TIME = 805;
    
    public static final Object LUA_NIL = new Object();
    
    // Token class
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
    
    // Constructor
    public Lua(OpenTTY midlet, int id, String pid, Process proc, Object stdout, 
               Hashtable<String, String> scope) {
        this.midlet = midlet;
        this.id = id;
        this.PID = pid;
        this.proc = proc;
        this.stdout = stdout;
        this.father = new Hashtable<>();
        this.father.putAll(scope);
        
        // Initialize global tables
        Hashtable<String, Object> os = new Hashtable<>();
        Hashtable<String, Object> io = new Hashtable<>();
        Hashtable<String, Object> string = new Hashtable<>();
        Hashtable<String, Object> table = new Hashtable<>();
        Hashtable<String, Object> math = new Hashtable<>();
        Hashtable<String, Object> base64 = new Hashtable<>();
        
        // OS functions
        os.put("getenv", new LuaFunction(GETENV));
        os.put("setenv", new LuaFunction(SETENV));
        os.put("clock", new LuaFunction(CLOCK));
        os.put("exit", new LuaFunction(EXIT));
        os.put("date", new LuaFunction(DATE));
        os.put("execute", new LuaFunction(EXEC));
        os.put("remove", new LuaFunction(REMOVE));
        os.put("mkdir", new LuaFunction(MKDIR));
        globals.put("os", os);
        
        // IO functions
        io.put("read", new LuaFunction(READ));
        io.put("write", new LuaFunction(WRITE));
        io.put("open", new LuaFunction(OPEN));
        io.put("close", new LuaFunction(CLOSE));
        io.put("dirs", new LuaFunction(DIRS));
        globals.put("io", io);
        
        // String functions
        string.put("upper", new LuaFunction(UPPER));
        string.put("lower", new LuaFunction(LOWER));
        string.put("len", new LuaFunction(LEN));
        string.put("sub", new LuaFunction(SUB));
        string.put("reverse", new LuaFunction(REVERSE));
        string.put("trim", new LuaFunction(TRIM));
        string.put("split", new LuaFunction(SPLIT));
        globals.put("string", string);
        
        // Table functions
        table.put("insert", new LuaFunction(TB_INSERT));
        table.put("remove", new LuaFunction(TB_REMOVE));
        table.put("concat", new LuaFunction(TB_CONCAT));
        table.put("sort", new LuaFunction(TB_SORT));
        table.put("pack", new LuaFunction(TB_PACK));
        table.put("unpack", new LuaFunction(TB_UNPACK));
        globals.put("table", table);
        
        // Math functions
        math.put("random", new LuaFunction(RANDOM));
        globals.put("math", math);
        
        // Base64 functions
        base64.put("encode", new LuaFunction(BASE64_ENCODE));
        base64.put("decode", new LuaFunction(BASE64_DECODE));
        globals.put("base64", base64);
        
        // Global functions
        globals.put("print", new LuaFunction(PRINT));
        globals.put("error", new LuaFunction(ERROR));
        globals.put("type", new LuaFunction(TYPE));
        globals.put("tostring", new LuaFunction(TOSTRING));
        globals.put("tonumber", new LuaFunction(TONUMBER));
        globals.put("pairs", new LuaFunction(PAIRS));
        globals.put("ipairs", new LuaFunction(IPAIRS));
        
        globals.put("_VERSION", "Lua J2SE");
        globals.put("_ENV", globals);
    }
    
    // Run source code
    public Hashtable<String, Object> run(String source, String code, Hashtable<String, Object> args) {
        midlet.sys.put(PID, proc);
        globals.put("arg", args);
        
        Hashtable<String, Object> result = new Hashtable<>();
        
        try {
            this.tokens = tokenize(code);
            collectLabels();
            
            while (peek().type != EOF) {
                Object res = statement(globals);
                if (doreturn) {
                    if (res != null) {
                        result.put("object", res);
                    }
                    doreturn = false;
                    break;
                }
            }
        } catch (Exception e) {
            print(midlet.getCatch(e));
            result.put("status", 1);
            return result;
        }
        
        if (kill) {
            midlet.sys.remove(PID);
        }
        
        result.put("status", 0);
        return result;
    }
    
    // Tokenizer
    public Vector<Token> tokenize(String code) {
        Vector<Token> tokens = new Vector<>();
        int i = 0;
        
        // Skip shebang
        if (code.startsWith("#!")) {
            while (i < code.length() && code.charAt(i) != '\n') i++;
            if (i < code.length()) i++;
        }
        
        while (i < code.length()) {
            char c = code.charAt(i);
            
            // Whitespace
            if (Character.isWhitespace(c) || c == ';') {
                i++;
                continue;
            }
            
            // Comments
            if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '-') {
                i += 2;
                if (i < code.length() && code.charAt(i) == '[' && i + 1 < code.length() && code.charAt(i + 1) == '[') {
                    i += 2;
                    while (i + 1 < code.length() && !(code.charAt(i) == ']' && code.charAt(i + 1) == ']')) i++;
                    if (i + 1 < code.length()) i += 2;
                } else {
                    while (i < code.length() && code.charAt(i) != '\n') i++;
                }
                continue;
            }
            
            // Numbers
            if (Character.isDigit(c) || (c == '.' && i + 1 < code.length() && Character.isDigit(code.charAt(i + 1)))) {
                StringBuilder sb = new StringBuilder();
                boolean hasDecimal = false;
                
                while (i < code.length() && (Character.isDigit(code.charAt(i)) || code.charAt(i) == '.')) {
                    if (code.charAt(i) == '.') {
                        if (hasDecimal) break;
                        if (i + 1 < code.length() && code.charAt(i + 1) == '.') break;
                        hasDecimal = true;
                    }
                    sb.append(code.charAt(i));
                    i++;
                }
                
                try {
                    double num = Double.parseDouble(sb.toString());
                    tokens.add(new Token(NUMBER, num));
                } catch (NumberFormatException e) {
                    throw new RuntimeException("Invalid number: " + sb);
                }
                continue;
            }
            
            // Strings
            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                StringBuilder sb = new StringBuilder();
                
                while (i < code.length() && code.charAt(i) != quote) {
                    sb.append(code.charAt(i));
                    i++;
                }
                
                if (i < code.length()) i++; // Skip closing quote
                tokens.add(new Token(STRING, sb.toString()));
                continue;
            }
            
            // Identifiers and keywords
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (i < code.length() && (Character.isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) {
                    sb.append(code.charAt(i));
                    i++;
                }
                
                String word = sb.toString();
                int type = IDENTIFIER;
                
                switch (word) {
                    case "true": case "false": type = BOOLEAN; break;
                    case "nil": type = NIL; break;
                    case "and": type = AND; break;
                    case "or": type = OR; break;
                    case "not": type = NOT; break;
                    case "if": type = IF; break;
                    case "then": type = THEN; break;
                    case "else": type = ELSE; break;
                    case "elseif": type = ELSEIF; break;
                    case "end": type = END; break;
                    case "while": type = WHILE; break;
                    case "do": type = DO; break;
                    case "repeat": type = REPEAT; break;
                    case "until": type = UNTIL; break;
                    case "for": type = FOR; break;
                    case "in": type = IN; break;
                    case "function": type = FUNCTION; break;
                    case "local": type = LOCAL; break;
                    case "return": type = RETURN; break;
                    case "break": type = BREAK; break;
                }
                
                tokens.add(new Token(type, word));
                continue;
            }
            
            // Operators
            switch (c) {
                case '+': tokens.add(new Token(PLUS, "+")); i++; break;
                case '-': tokens.add(new Token(MINUS, "-")); i++; break;
                case '*': tokens.add(new Token(MULTIPLY, "*")); i++; break;
                case '/': tokens.add(new Token(DIVIDE, "/")); i++; break;
                case '%': tokens.add(new Token(MODULO, "%")); i++; break;
                case '^': tokens.add(new Token(POWER, "^")); i++; break;
                case '#': tokens.add(new Token(LENGTH, "#")); i++; break;
                case '(': tokens.add(new Token(LPAREN, "(")); i++; break;
                case ')': tokens.add(new Token(RPAREN, ")")); i++; break;
                case '{': tokens.add(new Token(LBRACE, "{")); i++; break;
                case '}': tokens.add(new Token(RBRACE, "}")); i++; break;
                case '[': tokens.add(new Token(LBRACKET, "[")); i++; break;
                case ']': tokens.add(new Token(RBRACKET, "]")); i++; break;
                case ',': tokens.add(new Token(COMMA, ",")); i++; break;
                case '.': 
                    if (i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') {
                        tokens.add(new Token(VARARG, "..."));
                        i += 3;
                    } else if (i + 1 < code.length() && code.charAt(i + 1) == '.') {
                        tokens.add(new Token(CONCAT, ".."));
                        i += 2;
                    } else {
                        tokens.add(new Token(DOT, "."));
                        i++;
                    }
                    break;
                case ':':
                    tokens.add(new Token(COLON, ":"));
                    i++;
                    break;
                case '=':
                    if (i + 1 < code.length() && code.charAt(i + 1) == '=') {
                        tokens.add(new Token(EQ, "=="));
                        i += 2;
                    } else {
                        tokens.add(new Token(ASSIGN, "="));
                        i++;
                    }
                    break;
                case '<':
                    if (i + 1 < code.length() && code.charAt(i + 1) == '=') {
                        tokens.add(new Token(LE, "<="));
                        i += 2;
                    } else {
                        tokens.add(new Token(LT, "<"));
                        i++;
                    }
                    break;
                case '>':
                    if (i + 1 < code.length() && code.charAt(i + 1) == '=') {
                        tokens.add(new Token(GE, ">="));
                        i += 2;
                    } else {
                        tokens.add(new Token(GT, ">"));
                        i++;
                    }
                    break;
                case '~':
                    if (i + 1 < code.length() && code.charAt(i + 1) == '=') {
                        tokens.add(new Token(NE, "~="));
                        i += 2;
                    } else {
                        throw new RuntimeException("Unexpected '~'");
                    }
                    break;
                default:
                    throw new RuntimeException("Unexpected character: " + c);
            }
        }
        
        tokens.add(new Token(EOF, "EOF"));
        return tokens;
    }
    
    // Helper methods for token handling
    private Token peek() { return tokenIndex < tokens.size() ? tokens.get(tokenIndex) : new Token(EOF, "EOF"); }
    private Token consume() { return tokenIndex < tokens.size() ? tokens.get(tokenIndex++) : new Token(EOF, "EOF"); }
    private Token consume(int expected) throws Exception {
        Token token = peek();
        if (token.type == expected) {
            tokenIndex++;
            return token;
        }
        throw new Exception("Expected token type " + expected + ", got " + token.type);
    }
    
    // Statement parser
    public Object statement(Hashtable<String, Object> scope) throws Exception {
        Token current = peek();
        
        if (current.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            
            if (peek().type == ASSIGN) {
                consume(ASSIGN);
                Object value = expression(scope);
                scope.put(name, value == null ? LUA_NIL : value);
                return null;
            } else if (peek().type == LPAREN) {
                return callFunction(name, scope);
            } else {
                return unwrap(scope.get(name));
            }
        } else if (current.type == IF) {
            return parseIf(scope);
        } else if (current.type == WHILE) {
            return parseWhile(scope);
        } else if (current.type == FOR) {
            return parseFor(scope);
        } else if (current.type == LOCAL) {
            consume(LOCAL);
            String name = (String) consume(IDENTIFIER).value;
            
            if (peek().type == ASSIGN) {
                consume(ASSIGN);
                Object value = expression(scope);
                scope.put(name, value == null ? LUA_NIL : value);
            } else {
                scope.put(name, LUA_NIL);
            }
            return null;
        } else if (current.type == FUNCTION) {
            return parseFunction(scope, false);
        } else if (current.type == RETURN) {
            consume(RETURN);
            doreturn = true;
            if (peek().type == EOF || peek().type == END) {
                return new Vector<>();
            }
            Vector<Object> results = new Vector<>();
            results.add(expression(scope));
            while (peek().type == COMMA) {
                consume(COMMA);
                results.add(expression(scope));
            }
            return results;
        } else if (current.type == DO) {
            consume(DO);
            while (peek().type != END && peek().type != EOF) {
                statement(scope);
            }
            consume(END);
            return null;
        } else {
            return expression(scope);
        }
    }
    
    // Expression parser
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
        while (true) {
            Token op = peek();
            if (op.type == EQ || op.type == NE || op.type == LT || 
                op.type == GT || op.type == LE || op.type == GE) {
                consume();
                Object right = concatenation(scope);
                
                boolean result;
                if (op.type == EQ) {
                    result = objectsEqual(left, right);
                } else if (op.type == NE) {
                    result = !objectsEqual(left, right);
                } else {
                    double l = toNumber(left);
                    double r = toNumber(right);
                    switch (op.type) {
                        case LT: result = l < r; break;
                        case GT: result = l > r; break;
                        case LE: result = l <= r; break;
                        case GE: result = l >= r; break;
                        default: result = false;
                    }
                }
                left = result;
            } else {
                break;
            }
        }
        return left;
    }
    
    private Object concatenation(Hashtable<String, Object> scope) throws Exception {
        Object left = term(scope);
        while (peek().type == CONCAT) {
            consume(CONCAT);
            Object right = term(scope);
            left = toString(left) + toString(right);
        }
        return left;
    }
    
    private Object term(Hashtable<String, Object> scope) throws Exception {
        Object left = factor(scope);
        while (peek().type == PLUS || peek().type == MINUS) {
            Token op = consume();
            Object right = factor(scope);
            double l = toNumber(left);
            double r = toNumber(right);
            left = op.type == PLUS ? l + r : l - r;
        }
        return left;
    }
    
    private Object factor(Hashtable<String, Object> scope) throws Exception {
        Object left = power(scope);
        while (peek().type == MULTIPLY || peek().type == DIVIDE || peek().type == MODULO) {
            Token op = consume();
            Object right = power(scope);
            double l = toNumber(left);
            double r = toNumber(right);
            
            if (op.type == MULTIPLY) {
                left = l * r;
            } else if (op.type == DIVIDE) {
                if (r == 0) throw new ArithmeticException("Division by zero");
                left = l / r;
            } else {
                if (r == 0) throw new ArithmeticException("Modulo by zero");
                left = l % r;
            }
        }
        return left;
    }
    
    private Object power(Hashtable<String, Object> scope) throws Exception {
        Object left = unary(scope);
        while (peek().type == POWER) {
            consume(POWER);
            Object right = unary(scope);
            double l = toNumber(left);
            double r = toNumber(right);
            left = Math.pow(l, r);
        }
        return left;
    }
    
    private Object unary(Hashtable<String, Object> scope) throws Exception {
        if (peek().type == MINUS) {
            consume(MINUS);
            Object expr = unary(scope);
            return -toNumber(expr);
        } else if (peek().type == NOT) {
            consume(NOT);
            Object expr = unary(scope);
            return !isTruthy(expr);
        } else if (peek().type == LENGTH) {
            consume(LENGTH);
            Object expr = unary(scope);
            if (expr instanceof String) {
                return (double) ((String) expr).length();
            } else if (expr instanceof Hashtable) {
                return (double) ((Hashtable) expr).size();
            } else if (expr instanceof Vector) {
                return (double) ((Vector) expr).size();
            }
            throw new RuntimeException("attempt to get length of a " + type(expr) + " value");
        }
        return primary(scope);
    }
    
    private Object primary(Hashtable<String, Object> scope) throws Exception {
        Token token = peek();
        
        if (token.type == NUMBER || token.type == STRING || token.type == BOOLEAN) {
            consume();
            return token.value;
        } else if (token.type == NIL) {
            consume();
            return null;
        } else if (token.type == LPAREN) {
            consume(LPAREN);
            Object expr = expression(scope);
            consume(RPAREN);
            return expr;
        } else if (token.type == FUNCTION) {
            return parseFunction(scope, true);
        } else if (token.type == LBRACE) {
            return parseTable(scope);
        } else if (token.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            Object value = unwrap(scope.get(name));
            if (value == null && globals.containsKey(name)) {
                value = unwrap(globals.get(name));
            }
            return value;
        }
        
        throw new RuntimeException("Unexpected token: " + token);
    }
    
    // Helper methods
    private boolean isTruthy(Object obj) {
        if (obj == null || obj == LUA_NIL) return false;
        if (obj instanceof Boolean) return (Boolean) obj;
        return true;
    }
    
    private String toString(Object obj) {
        if (obj == null || obj == LUA_NIL) return "nil";
        if (obj instanceof Boolean) return (Boolean) obj ? "true" : "false";
        if (obj instanceof Double) {
            double d = (Double) obj;
            if (d == (long) d) return Long.toString((long) d);
            return Double.toString(d);
        }
        return obj.toString();
    }
    
    private double toNumber(Object obj) {
        if (obj instanceof Double) return (Double) obj;
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                throw new RuntimeException("cannot convert '" + obj + "' to number");
            }
        }
        throw new RuntimeException("cannot convert " + type(obj) + " to number");
    }
    
    private boolean objectsEqual(Object a, Object b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
    
    private Object wrap(Object obj) {
        return obj == null ? LUA_NIL : obj;
    }
    
    private Object unwrap(Object obj) {
        return obj == LUA_NIL ? null : obj;
    }
    
    // Function call
    private Object callFunction(String name, Hashtable<String, Object> scope) throws Exception {
        consume(LPAREN);
        Vector<Object> args = new Vector<>();
        if (peek().type != RPAREN) {
            args.add(expression(scope));
            while (peek().type == COMMA) {
                consume(COMMA);
                args.add(expression(scope));
            }
        }
        consume(RPAREN);
        
        Object func = unwrap(scope.get(name));
        if (func == null && globals.containsKey(name)) {
            func = unwrap(globals.get(name));
        }
        
        if (func instanceof LuaFunction) {
            return ((LuaFunction) func).call(args);
        }
        throw new RuntimeException("attempt to call a non-function value: " + name);
    }
    
    // Control structures
    private Object parseIf(Hashtable<String, Object> scope) throws Exception {
        consume(IF);
        Object cond = expression(scope);
        consume(THEN);
        
        Object result = null;
        
        if (isTruthy(cond)) {
            while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                result = statement(scope);
                if (doreturn) return result;
            }
        } else {
            skipToElseOrEnd();
        }
        
        while (peek().type == ELSEIF) {
            consume(ELSEIF);
            cond = expression(scope);
            consume(THEN);
            
            if (isTruthy(cond)) {
                while (peek().type != ELSEIF && peek().type != ELSE && peek().type != END) {
                    result = statement(scope);
                    if (doreturn) return result;
                }
            } else {
                skipToElseOrEnd();
            }
        }
        
        if (peek().type == ELSE) {
            consume(ELSE);
            while (peek().type != END) {
                result = statement(scope);
                if (doreturn) return result;
            }
        }
        
        consume(END);
        return result;
    }
    
    private Object parseWhile(Hashtable<String, Object> scope) throws Exception {
        consume(WHILE);
        int condPos = tokenIndex;
        
        loopDepth++;
        Object result = null;
        
        while (true) {
            tokenIndex = condPos;
            Object cond = expression(scope);
            
            if (!isTruthy(cond) || breakLoop || doreturn) {
                skipToEnd();
                break;
            }
            
            consume(DO);
            while (peek().type != END) {
                result = statement(scope);
                if (doreturn || breakLoop) break;
            }
            
            if (breakLoop) {
                breakLoop = false;
                break;
            }
        }
        
        loopDepth--;
        return result;
    }
    
    private Object parseFor(Hashtable<String, Object> scope) throws Exception {
        consume(FOR);
        String varName = (String) consume(IDENTIFIER).value;
        
        if (peek().type == ASSIGN) {
            // Numeric for
            consume(ASSIGN);
            double start = toNumber(expression(scope));
            consume(COMMA);
            double limit = toNumber(expression(scope));
            double step = 1.0;
            
            if (peek().type == COMMA) {
                consume(COMMA);
                step = toNumber(expression(scope));
            }
            
            consume(DO);
            int bodyStart = tokenIndex;
            skipToEnd();
            int bodyEnd = tokenIndex - 1; // Before END token
            tokenIndex = bodyStart;
            
            loopDepth++;
            
            for (double i = start; step > 0 ? i <= limit : i >= limit; i += step) {
                if (breakLoop) {
                    breakLoop = false;
                    break;
                }
                
                scope.put(varName, i);
                int savedIndex = tokenIndex;
                
                while (tokenIndex < bodyEnd) {
                    Object res = statement(scope);
                    if (doreturn) {
                        loopDepth--;
                        return res;
                    }
                }
                
                tokenIndex = savedIndex;
            }
            
            tokenIndex = bodyEnd + 1; // Skip END
            loopDepth--;
        } else {
            // Generic for
            consume(IN);
            Object iterable = expression(scope);
            consume(DO);
            
            int bodyStart = tokenIndex;
            skipToEnd();
            int bodyEnd = tokenIndex - 1;
            tokenIndex = bodyStart;
            
            loopDepth++;
            
            if (iterable instanceof Hashtable) {
                Hashtable table = (Hashtable) iterable;
                for (Object key : table.keySet()) {
                    if (breakLoop) {
                        breakLoop = false;
                        break;
                    }
                    
                    scope.put(varName, key);
                    int savedIndex = tokenIndex;
                    
                    while (tokenIndex < bodyEnd) {
                        Object res = statement(scope);
                        if (doreturn) {
                            loopDepth--;
                            return res;
                        }
                    }
                    
                    tokenIndex = savedIndex;
                }
            }
            
            tokenIndex = bodyEnd + 1;
            loopDepth--;
        }
        
        return null;
    }
    
    private Object parseFunction(Hashtable<String, Object> scope, boolean anonymous) throws Exception {
        consume(FUNCTION);
        
        String name = null;
        if (!anonymous) {
            name = (String) consume(IDENTIFIER).value;
        }
        
        consume(LPAREN);
        Vector<String> params = new Vector<>();
        while (peek().type == IDENTIFIER) {
            params.add((String) consume(IDENTIFIER).value);
            if (peek().type == COMMA) consume(COMMA);
        }
        consume(RPAREN);
        
        int bodyStart = tokenIndex;
        skipToEnd();
        int bodyEnd = tokenIndex - 1;
        
        Vector<Token> bodyTokens = new Vector<>(tokens.subList(bodyStart, bodyEnd));
        
        LuaFunction func = new LuaFunction(params, bodyTokens, scope);
        
        if (!anonymous) {
            scope.put(name, func);
        }
        
        return func;
    }
    
    private Object parseTable(Hashtable<String, Object> scope) throws Exception {
        consume(LBRACE);
        Hashtable<Object, Object> table = new Hashtable<>();
        int index = 1;
        
        while (peek().type != RBRACE) {
            Object key, value;
            
            if (peek().type == LBRACKET) {
                consume(LBRACKET);
                key = expression(scope);
                consume(RBRACKET);
                consume(ASSIGN);
                value = expression(scope);
            } else if (peek().type == IDENTIFIER && peekNext().type == ASSIGN) {
                key = consume(IDENTIFIER).value;
                consume(ASSIGN);
                value = expression(scope);
            } else {
                key = (double) index++;
                value = expression(scope);
            }
            
            table.put(key, wrap(value));
            
            if (peek().type == COMMA) {
                consume(COMMA);
            } else if (peek().type == RBRACE) {
                break;
            }
        }
        
        consume(RBRACE);
        return table;
    }
    
    private Token peekNext() {
        return tokenIndex + 1 < tokens.size() ? tokens.get(tokenIndex + 1) : new Token(EOF, "EOF");
    }
    
    private void skipToElseOrEnd() throws Exception {
        int depth = 1;
        while (depth > 0) {
            Token token = consume();
            if (token.type == IF || token.type == FUNCTION || token.type == DO) depth++;
            else if (token.type == END) depth--;
            else if ((token.type == ELSEIF || token.type == ELSE) && depth == 1) {
                tokenIndex--;
                break;
            }
        }
    }
    
    private void skipToEnd() throws Exception {
        int depth = 1;
        while (depth > 0) {
            Token token = consume();
            if (token.type == IF || token.type == FUNCTION || token.type == DO) depth++;
            else if (token.type == END) depth--;
        }
    }
    
    private void collectLabels() {
        int saved = tokenIndex;
        tokenIndex = 0;
        
        while (peek().type != EOF) {
            Token token = peek();
            if (token.type == LABEL) {
                labels.put((String) token.value, tokenIndex);
            }
            consume();
        }
        
        tokenIndex = saved;
    }
    
    // Print function for Java SE
    private void print(String message) {
        if (stdout == System.out) {
            System.out.println(message);
        } else if (stdout instanceof PrintStream) {
            ((PrintStream) stdout).println(message);
        } else {
            System.out.println(message);
        }
    }
    
    // LuaFunction class
    public class LuaFunction {
        private Vector<String> params;
        private Vector<Token> bodyTokens;
        private Hashtable<String, Object> closureScope;
        private int MOD = -1;
        
        LuaFunction(Vector<String> params, Vector<Token> bodyTokens, Hashtable<String, Object> closureScope) {
            this.params = params;
            this.bodyTokens = bodyTokens;
            this.closureScope = closureScope;
        }
        
        LuaFunction(int type) {
            this.MOD = type;
        }
        
        public Object call(Vector<Object> args) throws Exception {
            if (MOD != -1) {
                return internals(args);
            }
            
            Hashtable<String, Object> functionScope = new Hashtable<>(closureScope);
            for (String key : globals.keySet()) {
                if (!functionScope.containsKey(key)) {
                    functionScope.put(key, unwrap(globals.get(key)));
                }
            }
            
            int paramCount = params.size();
            for (int i = 0; i < paramCount; i++) {
                String param = params.get(i);
                Object value = i < args.size() ? args.get(i) : null;
                functionScope.put(param, wrap(value));
            }
            
            int savedIndex = tokenIndex;
            Vector<Token> savedTokens = tokens;
            
            tokens = bodyTokens;
            tokenIndex = 0;
            
            Object result = null;
            while (peek().type != EOF) {
                Object res = Lua.this.statement(functionScope);
                if (doreturn) {
                    result = res;
                    doreturn = false;
                    break;
                }
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
            
            return result;
        }
        
        public Object internals(Vector<Object> args) throws Exception {
            switch (MOD) {
                case PRINT:
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < args.size(); i++) {
                        sb.append(toString(args.get(i)));
                        if (i < args.size() - 1) sb.append("\t");
                    }
                    print(sb.toString());
                    break;
                    
                case ERROR:
                    String msg = args.isEmpty() ? "error" : toString(args.get(0));
                    throw new Exception(msg);
                    
                case TYPE:
                    return type(args.isEmpty() ? null : args.get(0));
                    
                case TOSTRING:
                    return toString(args.isEmpty() ? null : args.get(0));
                    
                case TONUMBER:
                    if (args.isEmpty()) return null;
                    try {
                        return Double.parseDouble(toString(args.get(0)));
                    } catch (NumberFormatException e) {
                        return null;
                    }
                    
                case EXEC:
                    if (args.isEmpty()) return 0.0;
                    try {
                        String cmd = toString(args.get(0));
                        Process process = Runtime.getRuntime().exec(cmd);
                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                        StringBuilder output = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                        process.waitFor();
                        print(output.toString());
                        return (double) process.exitValue();
                    } catch (Exception e) {
                        return 1.0;
                    }
                    
                case READ:
                    if (args.isEmpty()) return "";
                    String filename = toString(args.get(0));
                    return midlet.read(filename, father);
                    
                case WRITE:
                    if (args.size() < 2) return 1.0;
                    String file = toString(args.get(0));
                    String data = toString(args.get(1));
                    return (double) midlet.write(file, data, id, father);
                    
                case REMOVE:
                    if (args.isEmpty()) return 1.0;
                    return (double) midlet.deleteFile(toString(args.get(0)), id, father);
                    
                case MKDIR:
                    if (args.isEmpty()) return 1.0;
                    String dir = toString(args.get(0));
                    Path path = Paths.get(midlet.rootPath.toString(), dir.substring(1));
                    Files.createDirectories(path);
                    return 0.0;
                    
                case DIRS:
                    String pathStr = args.isEmpty() ? (String) father.get("PWD") : toString(args.get(0));
                    Path dirPath = Paths.get(midlet.rootPath.toString(), pathStr.substring(1));
                    
                    Hashtable<Object, Object> result = new Hashtable<>();
                    int index = 1;
                    
                    if (Files.exists(dirPath) && Files.isDirectory(dirPath)) {
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
                            for (Path entry : stream) {
                                String name = entry.getFileName().toString();
                                if (Files.isDirectory(entry)) {
                                    name += "/";
                                }
                                result.put((double) index++, name);
                            }
                        }
                    }
                    
                    return result;
                    
                case UPPER:
                    if (args.isEmpty()) return "";
                    return toString(args.get(0)).toUpperCase();
                    
                case LOWER:
                    if (args.isEmpty()) return "";
                    return toString(args.get(0)).toLowerCase();
                    
                case LEN:
                    if (args.isEmpty()) return 0.0;
                    Object obj = args.get(0);
                    if (obj instanceof String) {
                        return (double) ((String) obj).length();
                    } else if (obj instanceof Hashtable) {
                        return (double) ((Hashtable) obj).size();
                    }
                    return 0.0;
                    
                case SUB:
                    if (args.isEmpty()) return "";
                    String text = toString(args.get(0));
                    if (args.size() == 1) return text;
                    
                    int start = (int) toNumber(args.get(1));
                    int end = args.size() > 2 ? (int) toNumber(args.get(2)) : text.length();
                    
                    if (start < 0) start = text.length() + start + 1;
                    if (end < 0) end = text.length() + end + 1;
                    
                    start = Math.max(1, start);
                    end = Math.min(text.length(), end);
                    
                    if (start > end) return "";
                    return text.substring(start - 1, end);
                    
                case RANDOM:
                    if (args.isEmpty()) {
                        return Math.random();
                    } else {
                        double max = toNumber(args.get(0));
                        return Math.floor(Math.random() * max);
                    }
                    
                case BASE64_ENCODE:
                    if (args.isEmpty()) return "";
                    String toEncode = toString(args.get(0));
                    return Base64.getEncoder().encodeToString(toEncode.getBytes());
                    
                case BASE64_DECODE:
                    if (args.isEmpty()) return "";
                    String encoded = toString(args.get(0));
                    return new String(Base64.getDecoder().decode(encoded));
                    
                case EXIT:
                    if (PID.equals("1")) {
                        System.exit(0);
                    }
                    midlet.sys.remove(PID);
                    throw new Error("Process terminated");
                    
                case DATE:
                    return new Date().toString();
                    
                case CLOCK:
                    return (double) (System.currentTimeMillis() - uptime);
                    
                default:
                    return null;
            }
            return null;
        }
    }
    
    // Utility method for internal calls
    public Object internals(Vector<Object> args) {
        try {
            return new LuaFunction((int) args.get(0)).internals(
                new Vector<>(args.subList(1, args.size()))
            );
        } catch (Exception e) {
            return null;
        }
    }
    
    public Object getKernel() {
        return new LuaFunction(KERNEL);
    }
    
    // Type checking
    public static String type(Object obj) {
        if (obj == null || obj == LUA_NIL) return "nil";
        if (obj instanceof String) return "string";
        if (obj instanceof Double) return "number";
        if (obj instanceof Boolean) return "boolean";
        if (obj instanceof LuaFunction) return "function";
        if (obj instanceof Hashtable) return "table";
        if (obj instanceof Vector) return "table";
        return "userdata";
    }
}