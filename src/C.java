import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;

// |
// C Runtime
public class C {
    public boolean breakLoop = false, doreturn = false, kill = true;
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0, switchLevel = 0;
    public Hashtable globals = new Hashtable(), father, requireCache = new Hashtable(), labels = new Hashtable(), structs = new Hashtable(), unions = new Hashtable(), typedefs = new Hashtable();
    public Vector tokens;
    
    public int status = 0;
    
    // C Type System
    public static final int TYPE_VOID = 0, TYPE_INT = 1, TYPE_CHAR = 2, TYPE_LONG = 3, TYPE_FLOAT = 4, TYPE_DOUBLE = 5, TYPE_PTR = 6, TYPE_STRUCT = 7, TYPE_UNION = 8, TYPE_ENUM = 9;
    
    // C Keywords
    public static final int T_AUTO = 256, T_BREAK = 257, T_CASE = 258, T_CHAR = 259, T_CONST = 260, T_CONTINUE = 261, T_DEFAULT = 262, T_DO = 263, T_DOUBLE = 264, T_ELSE = 265, T_ENUM = 266, T_EXTERN = 267, T_FLOAT = 268, T_FOR = 269, T_GOTO = 270, T_IF = 271, T_INT = 272, T_LONG = 273, T_REGISTER = 274, T_RETURN = 275, T_SHORT = 276, T_SIGNED = 277, T_SIZEOF = 278, T_STATIC = 279, T_STRUCT = 280, T_SWITCH = 281, T_TYPEDEF = 282, T_UNION = 283, T_UNSIGNED = 284, T_VOID = 285, T_VOLATILE = 286, T_WHILE = 287;
    
    // Operators
    public static final int INC_OP = 300, DEC_OP = 301, LEFT_OP = 302, RIGHT_OP = 303, LE_OP = 304, GE_OP = 305, EQ_OP = 306, NE_OP = 307, AND_OP = 308, OR_OP = 309, MUL_ASSIGN = 310, DIV_ASSIGN = 311, MOD_ASSIGN = 312, ADD_ASSIGN = 313, SUB_ASSIGN = 314, LEFT_ASSIGN = 315, RIGHT_ASSIGN = 316, AND_ASSIGN = 317, XOR_ASSIGN = 318, OR_ASSIGN = 319, PTR_OP = 320;
    
    // Tokens
    public static final int EOF = 0, IDENTIFIER = 1, CONSTANT = 2, STRING_LITERAL = 3, PUNCTUATOR = 4;
    
    // Values
    public static final Boolean TRUE = Boolean.TRUE, FALSE = Boolean.FALSE;
    public static final Object C_NIL = new Object();
    
    // C Value Object
    public static class CValue {
        public int type;
        public Object value;
        
        public CValue(int type, Object value) {
            this.type = type;
            this.value = value;
        }
        
        public String toString() {
            if (type == TYPE_INT) return String.valueOf(((Integer)value).intValue());
            if (type == TYPE_CHAR) return String.valueOf((char)((Integer)value).intValue());
            if (type == TYPE_DOUBLE) return String.valueOf(((Double)value).doubleValue());
            if (type == TYPE_PTR) return "ptr:" + value;
            if (type == TYPE_STRUCT) return "struct:" + value;
            return String.valueOf(value);
        }
        
        public int asInt() {
            if (value instanceof Integer) return ((Integer)value).intValue();
            if (value instanceof Double) return (int)((Double)value).doubleValue();
            if (value instanceof Boolean) return ((Boolean)value).booleanValue() ? 1 : 0;
            if (value instanceof String) {
                try { return Integer.parseInt((String)value); }
                catch(Exception e) { return 0; }
            }
            return 0;
        }
        
        public char asChar() { return (char)asInt(); }
        public long asLong() { return asInt(); }
        public double asDouble() {
            if (value instanceof Double) return ((Double)value).doubleValue();
            if (value instanceof Integer) return ((Integer)value).doubleValue();
            return 0.0;
        }
    }
    
    public static class Token {
        int type;
        Object value;
        Token(int type, Object value) { this.type = type; this.value = value; }
        public String toString() { return "Token(type=" + type + ", value=" + value + ")"; }
    }
    
    // Symbol Table Entry
    public static class Symbol {
        public String name;
        public int type;
        public int storage; // 0=auto, 1=static, 2=extern, 3=register
        public CValue value;
        public Hashtable members; // for struct/union
        public int arraySize;
        public Symbol next;
        
        public Symbol(String name, int type, int storage) {
            this.name = name;
            this.type = type;
            this.storage = storage;
            this.value = null;
            this.members = null;
            this.arraySize = 0;
        }
    }
    
    // Main Constructor
    public C(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet;
        this.id = id;
        this.PID = pid;
        this.proc = proc;
        this.stdout = stdout;
        this.father = scope;
        this.tokenIndex = 0;
        
        // Initialize built-in functions
        Hashtable stdlib = new Hashtable();
        String[] funcs = new String[] { "printf", "scanf", "malloc", "free", "strlen", "strcpy", "strcat", "strcmp", "memcpy", "memset", "exit", "system" };
        int[] codes = new int[] { 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009, 1010, 1011 };
        for (int i = 0; i < funcs.length; i++) {
            stdlib.put(funcs[i], new CFunction(codes[i]));
        }
        globals.put("stdlib", stdlib);
        
        // Predefine standard types
        globals.put("size_t", new CValue(TYPE_LONG, "unsigned long"));
        globals.put("NULL", new CValue(TYPE_PTR, null));
        globals.put("EOF", new CValue(TYPE_INT, -1));
        
        // Standard I/O
        Hashtable stdio = new Hashtable();
        stdio.put("stdin", new CValue(TYPE_PTR, "stdin"));
        stdio.put("stdout", new CValue(TYPE_PTR, "stdout"));
        stdio.put("stderr", new CValue(TYPE_PTR, "stderr"));
        globals.put("stdio", stdio);
    }
    
    // Run Source Code
    public Hashtable run(String source, String code, Hashtable args) {
        midlet.sys.put(PID, proc);
        globals.put("argv", args);
        
        Hashtable ITEM = new Hashtable();
        
        try {
            this.tokens = tokenize(code);
            collectLabels();
            
            // Parse translation unit
            while (peek().type != EOF) {
                Object res = translationUnit();
                if (doreturn) {
                    if (res != null) ITEM.put("object", res);
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
            if (e.getMessage() != null) midlet.print(e.getMessage(), stdout, id, father);
            status = 1;
        }
        
        if (kill) midlet.sys.remove(PID);
        ITEM.put("status", status);
        return ITEM;
    }
    
    // Tokenizer
    public Vector tokenize(String code) throws Exception {
        if (midlet.cacheLua.containsKey(code)) return (Vector)midlet.cacheLua.get(code);
        
        Vector tokens = new Vector();
        int i = 0;
        
        while (i < code.length()) {
            char c = code.charAt(i);
            
            if (c <= ' ') { i++; continue; }
            
            // Preprocessor directives (skip for now)
            if (c == '#' && i == 0) {
                while (i < code.length() && code.charAt(i) != '\n') i++;
                continue;
            }
            
            // Comments
            if (c == '/' && i + 1 < code.length()) {
                if (code.charAt(i + 1) == '/') {
                    i += 2;
                    while (i < code.length() && code.charAt(i) != '\n') i++;
                    continue;
                }
                if (code.charAt(i + 1) == '*') {
                    i += 2;
                    while (i + 1 < code.length() && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                    i += 2;
                    continue;
                }
            }
            
            // String literal
            if (c == '"') {
                i++;
                StringBuffer sb = new StringBuffer();
                while (i < code.length() && code.charAt(i) != '"') {
                    if (code.charAt(i) == '\\' && i + 1 < code.length()) {
                        i++;
                        switch (code.charAt(i)) {
                            case 'n': sb.append('\n'); break;
                            case 't': sb.append('\t'); break;
                            case 'r': sb.append('\r'); break;
                            case '\\': sb.append('\\'); break;
                            case '"': sb.append('"'); break;
                            default: sb.append(code.charAt(i));
                        }
                    } else {
                        sb.append(code.charAt(i));
                    }
                    i++;
                }
                i++;
                tokens.addElement(new Token(STRING_LITERAL, sb.toString()));
                continue;
            }
            
            // Character constant
            if (c == '\'') {
                i++;
                char ch = code.charAt(i);
                if (ch == '\\' && i + 1 < code.length()) {
                    i++;
                    switch (code.charAt(i)) {
                        case 'n': ch = '\n'; break;
                        case 't': ch = '\t'; break;
                        case 'r': ch = '\r'; break;
                        case '0': ch = '\0'; break;
                        default: ch = code.charAt(i);
                    }
                }
                i += 2;
                tokens.addElement(new Token(CONSTANT, new CValue(TYPE_CHAR, (int)ch)));
                continue;
            }
            
            // Identifiers and keywords (CORRIGIDO)
            if (isLetter(c)) {
                StringBuffer sb = new StringBuffer();
                while (i < code.length() && isLetterOrDigit(code.charAt(i))) {
                    sb.append(code.charAt(i));
                    i++;
                }
                String word = sb.toString();
                int kw = keyword(word);
                if (kw != 0) tokens.addElement(new Token(kw, word));
                else tokens.addElement(new Token(IDENTIFIER, word));
                continue;
            }
            
            // Numbers (CORRIGIDO)
            if (c >= '0' && c <= '9') {
                StringBuffer sb = new StringBuffer();
                boolean isHex = false, isFloat = false;
                
                if (c == '0' && i + 1 < code.length() && (code.charAt(i + 1) == 'x' || code.charAt(i + 1) == 'X')) {
                    isHex = true;
                    sb.append(code.charAt(i++));
                    sb.append(code.charAt(i++));
                }
                
                while (i < code.length()) {
                    c = code.charAt(i);
                    if (isHex && ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                        sb.append(c);
                        i++;
                    } else if (!isHex && (c >= '0' && c <= '9')) {
                        sb.append(c);
                        i++;
                    } else if (!isHex && c == '.' && !isFloat) {
                        isFloat = true;
                        sb.append(c);
                        i++;
                    } else if (!isHex && (c == 'e' || c == 'E') && !isFloat) {
                        isFloat = true;
                        sb.append(c);
                        i++;
                        if (i < code.length() && (code.charAt(i) == '+' || code.charAt(i) == '-')) {
                            sb.append(code.charAt(i));
                            i++;
                        }
                    } else {
                        break;
                    }
                }
                
                String num = sb.toString();
                CValue val;
                if (isHex) val = new CValue(TYPE_INT, Integer.parseInt(num.substring(2), 16));
                else if (isFloat) val = new CValue(TYPE_DOUBLE, Double.parseDouble(num));
                else val = new CValue(TYPE_INT, Integer.parseInt(num));
                tokens.addElement(new Token(CONSTANT, val));
                continue;
            }
            
            // Multi-character operators
            if (i + 1 < code.length()) {
                String op = code.substring(i, i + 2);
                int optype = twoCharOp(op);
                if (optype != 0) {
                    tokens.addElement(new Token(optype, op));
                    i += 2;
                    continue;
                }
            }
            
            // Single character operators and punctuators
            char single = c;
            int stype = singleChar(c);
            if (stype != 0) {
                tokens.addElement(new Token(stype, String.valueOf(single)));
                i++;
                continue;
            }
            
            throw new Exception("Unexpected character: " + c);
        }
        
        tokens.addElement(new Token(EOF, "EOF"));
        if (midlet.useCache) {
            if (midlet.cacheLua.size() > 100) midlet.cacheLua.clear();
            midlet.cacheLua.put(code, tokens);
        }
        return tokens;
    }
    
    private int keyword(String word) {
        if (word.equals("auto")) return T_AUTO;
        if (word.equals("break")) return T_BREAK;
        if (word.equals("case")) return T_CASE;
        if (word.equals("char")) return T_CHAR;
        if (word.equals("const")) return T_CONST;
        if (word.equals("continue")) return T_CONTINUE;
        if (word.equals("default")) return T_DEFAULT;
        if (word.equals("do")) return T_DO;
        if (word.equals("double")) return T_DOUBLE;
        if (word.equals("else")) return T_ELSE;
        if (word.equals("enum")) return T_ENUM;
        if (word.equals("extern")) return T_EXTERN;
        if (word.equals("float")) return T_FLOAT;
        if (word.equals("for")) return T_FOR;
        if (word.equals("goto")) return T_GOTO;
        if (word.equals("if")) return T_IF;
        if (word.equals("int")) return T_INT;
        if (word.equals("long")) return T_LONG;
        if (word.equals("register")) return T_REGISTER;
        if (word.equals("return")) return T_RETURN;
        if (word.equals("short")) return T_SHORT;
        if (word.equals("signed")) return T_SIGNED;
        if (word.equals("sizeof")) return T_SIZEOF;
        if (word.equals("static")) return T_STATIC;
        if (word.equals("struct")) return T_STRUCT;
        if (word.equals("switch")) return T_SWITCH;
        if (word.equals("typedef")) return T_TYPEDEF;
        if (word.equals("union")) return T_UNION;
        if (word.equals("unsigned")) return T_UNSIGNED;
        if (word.equals("void")) return T_VOID;
        if (word.equals("volatile")) return T_VOLATILE;
        if (word.equals("while")) return T_WHILE;
        return 0;
    }
    
    private int twoCharOp(String op) {
        if (op.equals("++")) return INC_OP;
        if (op.equals("--")) return DEC_OP;
        if (op.equals("<<")) return LEFT_OP;
        if (op.equals(">>")) return RIGHT_OP;
        if (op.equals("<=")) return LE_OP;
        if (op.equals(">=")) return GE_OP;
        if (op.equals("==")) return EQ_OP;
        if (op.equals("!=")) return NE_OP;
        if (op.equals("&&")) return AND_OP;
        if (op.equals("||")) return OR_OP;
        if (op.equals("*=")) return MUL_ASSIGN;
        if (op.equals("/=")) return DIV_ASSIGN;
        if (op.equals("%=")) return MOD_ASSIGN;
        if (op.equals("+=")) return ADD_ASSIGN;
        if (op.equals("-=")) return SUB_ASSIGN;
        if (op.equals("<<=")) return LEFT_ASSIGN;
        if (op.equals(">>=")) return RIGHT_ASSIGN;
        if (op.equals("&=")) return AND_ASSIGN;
        if (op.equals("^=")) return XOR_ASSIGN;
        if (op.equals("|=")) return OR_ASSIGN;
        if (op.equals("->")) return PTR_OP;
        return 0;
    }
    
    private int singleChar(char c) {
        switch (c) {
            case ';': return ';';
            case '{': return '{';
            case '}': return '}';
            case '(': return '(';
            case ')': return ')';
            case '[': return '[';
            case ']': return ']';
            case ',': return ',';
            case '?': return '?';
            case ':': return ':';
            case '=': return '=';
            case '+': return '+';
            case '-': return '-';
            case '*': return '*';
            case '/': return '/';
            case '%': return '%';
            case '&': return '&';
            case '|': return '|';
            case '^': return '^';
            case '~': return '~';
            case '!': return '!';
            case '<': return '<';
            case '>': return '>';
            case '.': return '.';
            default: return 0;
        }
    }
    
    // Parser
    private Object translationUnit() throws Exception {
        Object result = null;
        while (peek().type != EOF) {
            result = externalDeclaration();
            if (doreturn) break;
        }
        return result;
    }
    
    private Object externalDeclaration() throws Exception {
        int t = peek().type;
        
        if (t == T_TYPEDEF || t == T_STATIC || t == T_EXTERN || t == T_REGISTER || 
            t == T_VOID || t == T_CHAR || t == T_INT || t == T_LONG || t == T_FLOAT || 
            t == T_DOUBLE || t == T_SIGNED || t == T_UNSIGNED || t == T_STRUCT || 
            t == T_UNION || t == T_ENUM) {
            
            Hashtable decl = declarationSpecifiers();
            if (decl == null) return null;
            
            // Function definition or declaration
            if (peek().type == '(') {
                return functionDefinition(decl);
            } else {
                return declaration(decl);
            }
        }
        
        return statement();
    }
    
    private Hashtable declarationSpecifiers() throws Exception {
        Hashtable spec = new Hashtable();
        int type = TYPE_INT;
        boolean isUnsigned = false, isSigned = false, isLong = false, isStatic = false, isExtern = false, isTypedef = false;
        
        while (true) {
            int t = peek().type;
            
            if (t == T_TYPEDEF) { isTypedef = true; consume(); }
            else if (t == T_STATIC) { isStatic = true; consume(); }
            else if (t == T_EXTERN) { isExtern = true; consume(); }
            else if (t == T_REGISTER) { consume(); }
            else if (t == T_CONST) { consume(); }
            else if (t == T_VOLATILE) { consume(); }
            else if (t == T_VOID) { type = TYPE_VOID; consume(); break; }
            else if (t == T_CHAR) { type = TYPE_CHAR; consume(); break; }
            else if (t == T_INT) { type = TYPE_INT; consume(); break; }
            else if (t == T_LONG) { isLong = true; consume(); break; }
            else if (t == T_FLOAT) { type = TYPE_FLOAT; consume(); break; }
            else if (t == T_DOUBLE) { type = TYPE_DOUBLE; consume(); break; }
            else if (t == T_SIGNED) { isSigned = true; consume(); }
            else if (t == T_UNSIGNED) { isUnsigned = true; consume(); }
            else if (t == T_STRUCT) { consume(); type = TYPE_STRUCT; structOrUnion(true); break; }
            else if (t == T_UNION) { consume(); type = TYPE_UNION; structOrUnion(false); break; }
            else if (t == T_ENUM) { consume(); type = TYPE_ENUM; enumSpecifier(); break; }
            else break;
        }
        
        if (isLong && type == TYPE_INT) type = TYPE_LONG;
        spec.put("type", new Integer(type));
        spec.put("unsigned", new Boolean(isUnsigned));
        spec.put("static", new Boolean(isStatic));
        spec.put("extern", new Boolean(isExtern));
        spec.put("typedef", new Boolean(isTypedef));
        
        return spec;
    }
    
    private Hashtable structOrUnion(boolean isStruct) throws Exception {
        Hashtable struct = new Hashtable();
        String name = null;
        
        if (peek().type == IDENTIFIER) {
            name = (String)consume(IDENTIFIER).value;
        }
        
        if (peek().type == '{') {
            consume('{');
            Hashtable members = new Hashtable();
            int offset = 0;
            
            while (peek().type != '}') {
                Hashtable decl = declarationSpecifiers();
                if (decl == null) break;
                
                while (true) {
                    String memName = (String)consume(IDENTIFIER).value;
                    int memType = ((Integer)decl.get("type")).intValue();
                    
                    // Handle array declarations
                    int arraySize = 0;
                    while (peek().type == '[') {
                        consume('[');
                        if (peek().type == CONSTANT) {
                            CValue cv = (CValue)consume(CONSTANT).value;
                            arraySize = cv.asInt();
                        }
                        consume(']');
                    }
                    
                    members.put(memName, new Object[] { new Integer(memType), new Integer(offset), new Integer(arraySize) });
                    offset += typeSize(memType);
                    
                    if (peek().type != ',') break;
                    consume(',');
                }
                consume(';');
            }
            consume('}');
            struct.put("members", members);
            struct.put("size", new Integer(offset));
        }
        
        if (name != null) {
            if (isStruct) structs.put(name, struct);
            else unions.put(name, struct);
        }
        
        return struct;
    }
    
    private void enumSpecifier() throws Exception {
        int value = 0;
        
        if (peek().type == IDENTIFIER) {
            String name = (String)consume(IDENTIFIER).value;
            // Store enum name if needed
        }
        
        if (peek().type == '{') {
            consume('{');
            while (peek().type != '}') {
                String enumName = (String)consume(IDENTIFIER).value;
                
                if (peek().type == '=') {
                    consume('=');
                    CValue cv = (CValue)consume(CONSTANT).value;
                    value = cv.asInt();
                }
                
                globals.put(enumName, new CValue(TYPE_INT, value));
                value++;
                
                if (peek().type == ',') consume(',');
                else break;
            }
            consume('}');
        }
    }
    
    private Object functionDefinition(Hashtable spec) throws Exception {
        // Function name
        String name = (String)consume(IDENTIFIER).value;
        consume('(');
        
        // Parameters
        Vector params = new Vector();
        if (peek().type != ')') {
            while (true) {
                Hashtable pSpec = declarationSpecifiers();
                String pName = (String)consume(IDENTIFIER).value;
                params.addElement(new Object[] { pSpec.get("type"), pName });
                if (peek().type != ',') break;
                consume(',');
            }
        }
        consume(')');
        
        // Function body
        consume('{');
        
        Vector bodyTokens = new Vector();
        int depth = 1;
        while (depth > 0) {
            Token tk = consume();
            if (tk.type == '{') depth++;
            else if (tk.type == '}') depth--;
            else if (tk.type == EOF) throw new Exception("Unmatched '{'");
            if (depth > 0) bodyTokens.addElement(tk);
        }
        
        CFunction func = new CFunction(name, params, bodyTokens, spec);
        globals.put(name, func);
        return null;
    }
    
    private Object declaration(Hashtable spec) throws Exception {
        while (true) {
            String name = (String)consume(IDENTIFIER).value;
            int type = ((Integer)spec.get("type")).intValue();
            
            // Handle array
            int arraySize = 0;
            while (peek().type == '[') {
                consume('[');
                if (peek().type == CONSTANT) {
                    CValue cv = (CValue)consume(CONSTANT).value;
                    arraySize = cv.asInt();
                }
                consume(']');
            }
            
            // Handle initialization
            CValue init = null;
            if (peek().type == '=') {
                consume('=');
                init = initializer(type);
            } else {
                init = new CValue(type, 0);
            }
            
            Symbol sym = new Symbol(name, type, ((Boolean)spec.get("static")).booleanValue() ? 1 : 0);
            sym.value = init;
            sym.arraySize = arraySize;
            globals.put(name, sym);
            
            if (peek().type != ',') break;
            consume(',');
        }
        consume(';');
        return null;
    }
    
    private CValue initializer(int type) throws Exception {
        if (peek().type == '{') {
            consume('{');
            // Array/struct initializer
            Vector values = new Vector();
            while (peek().type != '}') {
                values.addElement(initializer(type));
                if (peek().type == ',') consume(',');
                else break;
            }
            consume('}');
            return new CValue(type, values);
        }
        
        CValue cv = (CValue)consume(CONSTANT).value;
        return new CValue(type, cv.value);
    }
    
    private Object statement() throws Exception {
        int t = peek().type;
        
        if (t == '{') return compoundStatement();
        if (t == T_IF) return ifStatement();
        if (t == T_WHILE) return whileStatement();
        if (t == T_DO) return doStatement();
        if (t == T_FOR) return forStatement();
        if (t == T_SWITCH) return switchStatement();
        if (t == T_BREAK) return breakStatement();
        if (t == T_CONTINUE) return continueStatement();
        if (t == T_GOTO) return gotoStatement();
        if (t == T_RETURN) return returnStatement();
        if (t == T_SIZEOF) return sizeofExpression();
        if (t == T_TYPEDEF || t == T_STATIC || t == T_EXTERN || t == T_INT || t == T_CHAR || t == T_LONG ||
            t == T_FLOAT || t == T_DOUBLE || t == T_STRUCT || t == T_UNION || t == T_ENUM) {
            declaration(declarationSpecifiers());
            return null;
        }
        
        // Expression statement
        if (peek().type != ';') expression();
        consume(';');
        return null;
    }
    
    private Object compoundStatement() throws Exception {
        consume('{');
        while (peek().type != '}') {
            Object res = statement();
            if (doreturn) return res;
        }
        consume('}');
        return null;
    }
    
    private Object ifStatement() throws Exception {
        consume(T_IF);
        consume('(');
        Object cond = expression();
        consume(')');
        
        Object ifBody = statement();
        Object elseBody = null;
        
        if (peek().type == T_ELSE) {
            consume(T_ELSE);
            elseBody = statement();
        }
        
        if (isTruthy(cond)) return ifBody;
        return elseBody;
    }
    
    private Object whileStatement() throws Exception {
        consume(T_WHILE);
        consume('(');
        int condStart = tokenIndex;
        Object cond = expression();
        consume(')');
        
        int bodyStart = tokenIndex;
        int depth = 1;
        Vector bodyTokens = new Vector();
        
        while (depth > 0) {
            Token tk = peek();
            if (tk.type == '{') depth++;
            else if (tk.type == '}') depth--;
            else if (tk.type == EOF) throw new Exception("Unmatched while");
            
            if (depth > 0) bodyTokens.addElement(consume());
        }
        
        Object result = null;
        loopDepth++;
        
        while (isTruthy(cond)) {
            if (breakLoop) { breakLoop = false; break; }
            
            int savedIndex = tokenIndex;
            Vector savedTokens = tokens;
            tokens = bodyTokens;
            tokenIndex = 0;
            
            while (peek().type != EOF) {
                result = statement();
                if (doreturn) break;
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
            if (doreturn) break;
            
            tokenIndex = condStart;
            cond = expression();
            tokenIndex = bodyStart;
        }
        
        loopDepth--;
        return result;
    }
    
    private Object doStatement() throws Exception {
        consume(T_DO);
        int bodyStart = tokenIndex;
        int depth = 1;
        Vector bodyTokens = new Vector();
        
        while (depth > 0) {
            Token tk = peek();
            if (tk.type == '{') depth++;
            else if (tk.type == '}') depth--;
            else if (tk.type == EOF) throw new Exception("Unmatched do");
            
            if (depth > 0) bodyTokens.addElement(consume());
        }
        
        consume(T_WHILE);
        consume('(');
        int condStart = tokenIndex;
        Object cond = expression();
        consume(')');
        consume(';');
        
        Object result = null;
        loopDepth++;
        
        do {
            if (breakLoop) { breakLoop = false; break; }
            
            int savedIndex = tokenIndex;
            Vector savedTokens = tokens;
            tokens = bodyTokens;
            tokenIndex = 0;
            
            while (peek().type != EOF) {
                result = statement();
                if (doreturn) break;
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
            if (doreturn) break;
            
            tokenIndex = condStart;
            cond = expression();
        } while (isTruthy(cond));
        
        loopDepth--;
        return result;
    }
    
    private Object forStatement() throws Exception {
        consume(T_FOR);
        consume('(');
        
        // Initialization
        Object init = null;
        if (peek().type != ';') {
            init = expression();
        }
        consume(';');
        
        // Condition
        int condStart = tokenIndex;
        Object cond = null;
        if (peek().type != ';') {
            cond = expression();
        }
        consume(';');
        
        // Increment
        Vector incTokens = new Vector();
        if (peek().type != ')') {
            while (peek().type != ')') {
                incTokens.addElement(consume());
            }
        }
        consume(')');
        
        // Body
        int bodyStart = tokenIndex;
        int depth = 1;
        Vector bodyTokens = new Vector();
        
        while (depth > 0) {
            Token tk = peek();
            if (tk.type == '{') depth++;
            else if (tk.type == '}') depth--;
            else if (tk.type == EOF) throw new Exception("Unmatched for");
            
            if (depth > 0) bodyTokens.addElement(consume());
        }
        
        Object result = null;
        loopDepth++;
        
        while (true) {
            if (breakLoop) { breakLoop = false; break; }
            
            // Check condition
            if (cond != null) {
                tokenIndex = condStart;
                cond = expression();
                if (!isTruthy(cond)) break;
            }
            
            // Execute body
            int savedIndex = tokenIndex;
            Vector savedTokens = tokens;
            tokens = bodyTokens;
            tokenIndex = 0;
            
            while (peek().type != EOF) {
                result = statement();
                if (doreturn) break;
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
            if (doreturn) break;
            
            // Execute increment
            if (incTokens.size() > 0) {
                int incIndex = tokenIndex;
                Vector incTokensTemp = incTokens;
                tokenIndex = 0;
                tokens = incTokensTemp;
                while (peek().type != EOF) expression();
                tokenIndex = incIndex;
                tokens = savedTokens;
            }
        }
        
        loopDepth--;
        return result;
    }
    
    private Object switchStatement() throws Exception {
        consume(T_SWITCH);
        consume('(');
        Object expr = expression();
        consume(')');
        
        consume('{');
        switchLevel++;
        
        // Collect case labels and body
        Vector cases = new Vector();
        Vector defaultCase = null;
        Vector bodyTokens = new Vector();
        Vector currentCase = null;
        int depth = 1;
        
        while (depth > 0) {
            Token tk = peek();
            
            if (tk.type == T_CASE) {
                consume(T_CASE);
                CValue cv = (CValue)consume(CONSTANT).value;
                consume(':');
                currentCase = new Vector();
                currentCase.addElement(new Integer(cv.asInt()));
                cases.addElement(currentCase);
            } else if (tk.type == T_DEFAULT) {
                consume(T_DEFAULT);
                consume(':');
                defaultCase = new Vector();
                cases.addElement(defaultCase);
                currentCase = defaultCase;
            } else if (tk.type == '{') {
                depth++;
                if (currentCase != null) currentCase.addElement(tk);
                else bodyTokens.addElement(consume());
            } else if (tk.type == '}') {
                depth--;
                if (depth > 0) {
                    if (currentCase != null) currentCase.addElement(tk);
                    else bodyTokens.addElement(consume());
                }
            } else if (tk.type == T_BREAK && currentCase != null) {
                currentCase.addElement(consume());
            } else {
                if (currentCase != null) currentCase.addElement(consume());
                else bodyTokens.addElement(consume());
            }
        }
        
        consume('}');
        switchLevel--;
        
        // Execute matching case
        int matchValue = ((CValue)expr).asInt();
        Vector execTokens = null;
        
        for (int i = 0; i < cases.size(); i++) {
            Vector c = (Vector)cases.elementAt(i);
            if (c.size() > 0 && c.elementAt(0) instanceof Integer) {
                if (((Integer)c.elementAt(0)).intValue() == matchValue) {
                    execTokens = c;
                    break;
                }
            }
        }
        
        if (execTokens == null && defaultCase != null) execTokens = defaultCase;
        
        if (execTokens != null) {
            int savedIndex = tokenIndex;
            Vector savedTokens = tokens;
            tokens = execTokens;
            tokenIndex = 1; // skip case value
            
            while (peek().type != EOF) {
                Object res = statement();
                if (doreturn) return res;
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
        }
        
        return null;
    }
    
    private Object breakStatement() throws Exception {
        if (loopDepth == 0 && switchLevel == 0) {
            throw new RuntimeException("break outside loop or switch");
        }
        consume(T_BREAK);
        consume(';');
        breakLoop = true;
        return null;
    }
    
    private Object continueStatement() throws Exception {
        if (loopDepth == 0) throw new RuntimeException("continue outside loop");
        consume(T_CONTINUE);
        consume(';');
        breakLoop = true;
        return null;
    }
    
    private Object gotoStatement() throws Exception {
        consume(T_GOTO);
        String label = (String)consume(IDENTIFIER).value;
        consume(';');
        
        Integer labelPos = (Integer)labels.get(label);
        if (labelPos == null) throw new Exception("undefined label: " + label);
        
        tokenIndex = labelPos.intValue();
        return null;
    }
    
    private Object returnStatement() throws Exception {
        consume(T_RETURN);
        doreturn = true;
        
        if (peek().type != ';') {
            Object expr = expression();
            consume(';');
            return expr;
        }
        
        consume(';');
        return null;
    }
    
    private Object expression() throws Exception {
        return assignmentExpression();
    }
    
    private Object assignmentExpression() throws Exception {
        Object left = conditionalExpression();
        
        int t = peek().type;
        if (t == '=' || t == MUL_ASSIGN || t == DIV_ASSIGN || t == MOD_ASSIGN ||
            t == ADD_ASSIGN || t == SUB_ASSIGN || t == LEFT_ASSIGN || t == RIGHT_ASSIGN ||
            t == AND_ASSIGN || t == XOR_ASSIGN || t == OR_ASSIGN) {
            consume();
            Object right = assignmentExpression();
            return assignment(left, right, t);
        }
        
        return left;
    }
    
    private Object assignment(Object left, Object right, int op) {
        if (op == '=') return right;
        if (op == ADD_ASSIGN) return binaryOp(left, right, '+');
        if (op == SUB_ASSIGN) return binaryOp(left, right, '-');
        if (op == MUL_ASSIGN) return binaryOp(left, right, '*');
        if (op == DIV_ASSIGN) return binaryOp(left, right, '/');
        if (op == MOD_ASSIGN) return binaryOp(left, right, '%');
        if (op == LEFT_ASSIGN) return binaryOp(left, right, LEFT_OP);
        if (op == RIGHT_ASSIGN) return binaryOp(left, right, RIGHT_OP);
        if (op == AND_ASSIGN) return binaryOp(left, right, '&');
        if (op == OR_ASSIGN) return binaryOp(left, right, '|');
        if (op == XOR_ASSIGN) return binaryOp(left, right, '^');
        return right;
    }
    
    private Object conditionalExpression() throws Exception {
        Object cond = logicalOrExpression();
        
        if (peek().type == '?') {
            consume('?');
            Object trueExpr = expression();
            consume(':');
            Object falseExpr = conditionalExpression();
            return isTruthy(cond) ? trueExpr : falseExpr;
        }
        
        return cond;
    }
    
    private Object logicalOrExpression() throws Exception {
        Object left = logicalAndExpression();
        
        while (peek().type == OR_OP) {
            consume(OR_OP);
            Object right = logicalAndExpression();
            left = new Boolean(isTruthy(left) || isTruthy(right));
        }
        
        return left;
    }
    
    private Object logicalAndExpression() throws Exception {
        Object left = inclusiveOrExpression();
        
        while (peek().type == AND_OP) {
            consume(AND_OP);
            Object right = inclusiveOrExpression();
            left = new Boolean(isTruthy(left) && isTruthy(right));
        }
        
        return left;
    }
    
    private Object inclusiveOrExpression() throws Exception {
        Object left = exclusiveOrExpression();
        
        while (peek().type == '|') {
            consume('|');
            Object right = exclusiveOrExpression();
            int l = ((CValue)left).asInt();
            int r = ((CValue)right).asInt();
            left = new CValue(TYPE_INT, new Integer(l | r));
        }
        
        return left;
    }

    private Object exclusiveOrExpression() throws Exception {
        Object left = andExpression();
        
        while (peek().type == '^') {
            consume('^');
            Object right = andExpression();
            int l = ((CValue)left).asInt();
            int r = ((CValue)right).asInt();
            left = new CValue(TYPE_INT, new Integer(l ^ r));
        }
        
        return left;
    }

    private Object andExpression() throws Exception {
        Object left = equalityExpression();
        
        while (peek().type == '&') {
            consume('&');
            Object right = equalityExpression();
            int l = ((CValue)left).asInt();
            int r = ((CValue)right).asInt();
            left = new CValue(TYPE_INT, new Integer(l & r));
        }
        
        return left;
    }
    
    private Object equalityExpression() throws Exception {
        Object left = relationalExpression();
        
        while (true) {
            int t = peek().type;
            if (t == EQ_OP) {
                consume(EQ_OP);
                Object right = relationalExpression();
                left = new Boolean(compare(left, right) == 0);
            } else if (t == NE_OP) {
                consume(NE_OP);
                Object right = relationalExpression();
                left = new Boolean(compare(left, right) != 0);
            } else break;
        }
        
        return left;
    }
    
    private Object relationalExpression() throws Exception {
        Object left = shiftExpression();
        
        while (true) {
            int t = peek().type;
            if (t == '<') {
                consume('<');
                Object right = shiftExpression();
                left = new Boolean(compare(left, right) < 0);
            } else if (t == '>') {
                consume('>');
                Object right = shiftExpression();
                left = new Boolean(compare(left, right) > 0);
            } else if (t == LE_OP) {
                consume(LE_OP);
                Object right = shiftExpression();
                left = new Boolean(compare(left, right) <= 0);
            } else if (t == GE_OP) {
                consume(GE_OP);
                Object right = shiftExpression();
                left = new Boolean(compare(left, right) >= 0);
            } else break;
        }
        
        return left;
    }
    
    private Object shiftExpression() throws Exception {
        Object left = additiveExpression();
        
        while (true) {
            int t = peek().type;
            if (t == LEFT_OP) {
                consume(LEFT_OP);
                Object right = additiveExpression();
                int l = ((CValue)left).asInt();
                int r = ((CValue)right).asInt();
                left = new CValue(TYPE_INT, new Integer(l << r));
            } else if (t == RIGHT_OP) {
                consume(RIGHT_OP);
                Object right = additiveExpression();
                int l = ((CValue)left).asInt();
                int r = ((CValue)right).asInt();
                left = new CValue(TYPE_INT, new Integer(l >> r));
            } else break;
        }
        
        return left;
    }

    private Object additiveExpression() throws Exception {
        Object left = multiplicativeExpression();
        
        while (true) {
            int t = peek().type;
            if (t == '+') {
                consume('+');
                Object right = multiplicativeExpression();
                if (left instanceof CValue && right instanceof CValue) {
                    int l = ((CValue)left).asInt();
                    int r = ((CValue)right).asInt();
                    left = new CValue(TYPE_INT, new Integer(l + r));
                } else {
                    left = toLuaString(left) + toLuaString(right);
                }
            } else if (t == '-') {
                consume('-');
                Object right = multiplicativeExpression();
                int l = ((CValue)left).asInt();
                int r = ((CValue)right).asInt();
                left = new CValue(TYPE_INT, new Integer(l - r));
            } else break;
        }
        
        return left;
    }
    
    private Object multiplicativeExpression() throws Exception {
        Object left = castExpression();
        
        while (true) {
            int t = peek().type;
            if (t == '*') {
                consume('*');
                Object right = castExpression();
                int l = ((CValue)left).asInt();
                int r = ((CValue)right).asInt();
                left = new CValue(TYPE_INT, new Integer(l * r));
            } else if (t == '/') {
                consume('/');
                Object right = castExpression();
                int l = ((CValue)left).asInt();
                int r = ((CValue)right).asInt();
                if (r == 0) throw new Exception("division by zero");
                left = new CValue(TYPE_INT, new Integer(l / r));
            } else if (t == '%') {
                consume('%');
                Object right = castExpression();
                int l = ((CValue)left).asInt();
                int r = ((CValue)right).asInt();
                if (r == 0) throw new Exception("modulo by zero");
                left = new CValue(TYPE_INT, new Integer(l % r));
            } else break;
        }
        
        return left;
    }
    
    private Object castExpression() throws Exception {
        if (peek().type == '(') {
            consume('(');
            Hashtable type = declarationSpecifiers();
            consume(')');
            return castExpression();
        }
        return unaryExpression();
    }
    
    private Object unaryExpression() throws Exception {
        int t = peek().type;
        
        if (t == INC_OP) {
            consume(INC_OP);
            Object expr = unaryExpression();
            int val = ((CValue)expr).asInt();
            ((CValue)expr).value = new Integer(val + 1);
            return new CValue(TYPE_INT, new Integer(val));
        }
        if (t == DEC_OP) {
            consume(DEC_OP);
            Object expr = unaryExpression();
            int val = ((CValue)expr).asInt();
            ((CValue)expr).value = new Integer(val - 1);
            return new CValue(TYPE_INT, new Integer(val));
        }
        if (t == '&') {
            consume('&');
            return addressOf(unaryExpression());
        }
        if (t == '*') {
            consume('*');
            return indirection(unaryExpression());
        }
        if (t == '+') {
            consume('+');
            return unaryExpression();
        }
        if (t == '-') {
            consume('-');
            Object expr = unaryExpression();
            int val = ((CValue)expr).asInt();
            return new CValue(TYPE_INT, new Integer(-val));
        }
        if (t == '~') {
            consume('~');
            Object expr = unaryExpression();
            int val = ((CValue)expr).asInt();
            return new CValue(TYPE_INT, ~val);
        }
        if (t == '!') {
            consume('!');
            Object expr = unaryExpression();
            return new Boolean(!isTruthy(expr));
        }
        if (t == T_SIZEOF) {
            consume(T_SIZEOF);
            if (peek().type == '(') {
                consume('(');
                Hashtable type = declarationSpecifiers();
                consume(')');
                int size = typeSize(((Integer)type.get("type")).intValue());
                return new CValue(TYPE_INT, size);
            } else {
                Object expr = unaryExpression();
                return new CValue(TYPE_INT, typeSize(typeOf(expr)));
            }
        }
        
        return postfixExpression();
    }
    
    private Object postfixExpression() throws Exception {
        Object expr = primaryExpression();
        
        while (true) {
            int t = peek().type;
            
            if (t == '[') {
                consume('[');
                Object index = expression();
                consume(']');
                expr = arrayAccess(expr, index);
            } else if (t == '(') {
                consume('(');
                Vector args = new Vector();
                if (peek().type != ')') {
                    args.addElement(assignmentExpression());
                    while (peek().type == ',') {
                        consume(',');
                        args.addElement(assignmentExpression());
                    }
                }
                consume(')');
                expr = functionCall(expr, args);
            } else if (t == '.') {
                consume('.');
                String member = (String)consume(IDENTIFIER).value;
                expr = structAccess(expr, member);
            } else if (t == PTR_OP) {
                consume(PTR_OP);
                String member = (String)consume(IDENTIFIER).value;
                expr = ptrAccess(expr, member);
            } else if (t == INC_OP) {
                consume(INC_OP);
                int val = ((CValue)expr).asInt();
                ((CValue)expr).value = new Integer(val + 1);
                expr = new CValue(TYPE_INT, new Integer(val));
            } else if (t == DEC_OP) {
                consume(DEC_OP);
                int val = ((CValue)expr).asInt();
                ((CValue)expr).value = new Integer(val - 1);
                expr = new CValue(TYPE_INT, new Integer(val));
            } else break;
        }
        
        return expr;
    }
    
    private Object primaryExpression() throws Exception {
        int t = peek().type;
        
        if (t == IDENTIFIER) {
            String name = (String)consume(IDENTIFIER).value;
            Symbol sym = (Symbol)globals.get(name);
            if (sym == null) throw new Exception("undefined symbol: " + name);
            return sym.value;
        }
        
        if (t == CONSTANT) {
            return consume(CONSTANT).value;
        }
        
        if (t == STRING_LITERAL) {
            String str = (String)consume(STRING_LITERAL).value;
            return new CValue(TYPE_PTR, str);
        }
        
        if (t == '(') {
            consume('(');
            Object expr = expression();
            consume(')');
            return expr;
        }
        
        throw new Exception("expected primary expression");
    }
    
    private Object sizeofExpression() throws Exception {
        throw new Exception("sizeof expression not implemented");
    }
    
    // Helper methods
    private Token peek() {
        if (tokenIndex < tokens.size()) return (Token)tokens.elementAt(tokenIndex);
        return new Token(EOF, "EOF");
    }
    
    private Token consume() {
        if (tokenIndex < tokens.size()) return (Token)tokens.elementAt(tokenIndex++);
        return new Token(EOF, "EOF");
    }
    
    private Token consume(int expectedType) throws Exception {
        Token token = peek();
        if (token.type == expectedType) {
            tokenIndex++;
            return token;
        }
        throw new Exception("Expected token type " + expectedType + " but got " + token.type);
    }
    
    private int compare(Object a, Object b) {
        int av = ((CValue)a).asInt();
        int bv = ((CValue)b).asInt();
        return av - bv;
    }
    
    private boolean isTruthy(Object value) {
        if (value == null || value == C_NIL) return false;
        if (value instanceof Boolean) return ((Boolean)value).booleanValue();
        if (value instanceof CValue) return ((CValue)value).asInt() != 0;
        if (value instanceof String) return ((String)value).length() > 0;
        return true;
    }
    
    private Object binaryOp(Object left, Object right, int op) {
        int l = ((CValue)left).asInt();
        int r = ((CValue)right).asInt();
        int result;
        switch (op) {
            case '+': result = l + r; break;
            case '-': result = l - r; break;
            case '*': result = l * r; break;
            case '/': result = l / r; break;
            case '%': result = l % r; break;
            case LEFT_OP: result = l << r; break;
            case RIGHT_OP: result = l >> r; break;
            case '&': result = l & r; break;
            case '|': result = l | r; break;
            case '^': result = l ^ r; break;
            default: result = l;
        }
        return new CValue(TYPE_INT, new Integer(result));
    }
    
    private Object addressOf(Object expr) {
        return new CValue(TYPE_PTR, expr);
    }
    
    private Object indirection(Object expr) {
        if (expr instanceof CValue && ((CValue)expr).type == TYPE_PTR) {
            return ((CValue)expr).value;
        }
        throw new RuntimeException("indirection requires pointer");
    }
    
    private Object arrayAccess(Object array, Object index) {
        int idx = ((CValue)index).asInt();
        if (array instanceof CValue && ((CValue)array).value instanceof Vector) {
            Vector vec = (Vector)((CValue)array).value;
            if (idx >= 0 && idx < vec.size()) return vec.elementAt(idx);
        }
        return new CValue(TYPE_INT, 0);
    }
    
    private Object functionCall(Object func, Vector args) throws Exception {
        if (func instanceof CFunction) {
            return ((CFunction)func).call(args);
        }
        throw new Exception("attempt to call non-function");
    }
    
    private Object structAccess(Object struct, String member) {
        if (struct instanceof CValue && ((CValue)struct).value instanceof Hashtable) {
            Hashtable ht = (Hashtable)((CValue)struct).value;
            return ht.get(member);
        }
        return C_NIL;
    }
    
    private Object ptrAccess(Object ptr, String member) {
        if (ptr instanceof CValue && ((CValue)ptr).type == TYPE_PTR) {
            return structAccess(((CValue)ptr).value, member);
        }
        return C_NIL;
    }
    
    private int typeSize(int type) {
        switch (type) {
            case TYPE_CHAR: return 1;
            case TYPE_INT: return 4;
            case TYPE_LONG: return 4;
            case TYPE_FLOAT: return 4;
            case TYPE_DOUBLE: return 8;
            case TYPE_PTR: return 2;
            default: return 0;
        }
    }
    
    private int typeOf(Object expr) {
        if (expr instanceof CValue) return ((CValue)expr).type;
        if (expr instanceof String) return TYPE_PTR;
        if (expr instanceof Boolean) return TYPE_INT;
        return TYPE_INT;
    }
    
    private String toLuaString(Object obj) {
        if (obj == null || obj == C_NIL) return "nil";
        if (obj instanceof Boolean) return ((Boolean)obj).booleanValue() ? "true" : "false";
        if (obj instanceof CValue) return ((CValue)obj).toString();
        return obj.toString();
    }
    
    private void collectLabels() throws Exception {
        int saved = tokenIndex;
        labels.clear();
        tokenIndex = 0;
        
        while (peek().type != EOF) {
            Token tk = peek();
            if (tk.type == IDENTIFIER) {
                if (tokenIndex + 1 < tokens.size() && ((Token)tokens.elementAt(tokenIndex + 1)).type == ':') {
                    consume(IDENTIFIER);
                    consume(':');
                    labels.put(tk.value, new Integer(tokenIndex));
                } else {
                    consume();
                }
            } else {
                consume();
            }
        }
        
        tokenIndex = saved;
    }

    private boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_'; }
    private boolean isLetterOrDigit(char c) { return isLetter(c) || (c >= '0' && c <= '9'); }
    
    // CFunction implementation
    public class CFunction {
        private String name;
        private Vector params;
        private Vector bodyTokens;
        private Hashtable closureScope;
        private int MOD = -1;
        
        CFunction(int type) { this.MOD = type; }
        CFunction(String name, Vector params, Vector bodyTokens, Hashtable closureScope) {
            this.name = name;
            this.params = params;
            this.bodyTokens = bodyTokens;
            this.closureScope = closureScope;
        }
        
        public Object call(Vector args) throws Exception {
            if (MOD != -1) return builtin(args);
            
            Hashtable funcScope = new Hashtable();
            for (Enumeration e = closureScope.keys(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                funcScope.put(key, closureScope.get(key));
            }
            for (Enumeration e = globals.keys(); e.hasMoreElements();) {
                String key = (String)e.nextElement();
                if (!funcScope.containsKey(key)) funcScope.put(key, globals.get(key));
            }
            
            for (int i = 0; i < params.size(); i++) {
                String pname = (String)((Object[])params.elementAt(i))[1];
                Object ptype = ((Object[])params.elementAt(i))[0];
                Object arg = i < args.size() ? args.elementAt(i) : new CValue(TYPE_INT, 0);
                funcScope.put(pname, arg);
            }
            
            int savedIndex = tokenIndex;
            Vector savedTokens = tokens;
            tokens = bodyTokens;
            tokenIndex = 0;
            
            Object ret = null;
            while (peek().type != EOF) {
                Object result = statement();
                if (doreturn) {
                    ret = result;
                    doreturn = false;
                    break;
                }
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
            return ret;
        }
        
        private Object builtin(Vector args) throws Exception {
            if (MOD == 1000) { // printf
                if (args.isEmpty()) return null;
                String fmt = toLuaString(args.elementAt(0));
                String result = fmt;
                for (int i = 1; i < args.size(); i++) {
                    String val = toLuaString(args.elementAt(i));
                    result = result.replaceFirst("%[a-z]", val);
                }
                midlet.print(result, stdout, id, father);
                return new CValue(TYPE_INT, result.length());
            }
            else if (MOD == 1001) { // scanf - simplified
                return new CValue(TYPE_INT, 0);
            }
            else if (MOD == 1002) { // malloc
                int size = ((CValue)args.elementAt(0)).asInt();
                Vector mem = new Vector();
                for (int i = 0; i < size; i++) mem.addElement(new CValue(TYPE_CHAR, 0));
                return new CValue(TYPE_PTR, mem);
            }
            else if (MOD == 1003) { // free
                return null;
            }
            else if (MOD == 1004) { // strlen
                String str = toLuaString(args.elementAt(0));
                return new CValue(TYPE_INT, str.length());
            }
            else if (MOD == 1005) { // strcpy
                return args.elementAt(1);
            }
            else if (MOD == 1006) { // strcat
                String d = toLuaString(args.elementAt(0));
                String s = toLuaString(args.elementAt(1));
                return new CValue(TYPE_PTR, d + s);
            }
            else if (MOD == 1007) { // strcmp
                String s1 = toLuaString(args.elementAt(0));
                String s2 = toLuaString(args.elementAt(1));
                return new CValue(TYPE_INT, s1.compareTo(s2));
            }
            else if (MOD == 1008) { // memcpy
                return args.elementAt(1);
            }
            else if (MOD == 1009) { // memset
                return args.elementAt(0);
            }
            else if (MOD == 1010) { // exit
                int code = args.size() > 0 ? ((CValue)args.elementAt(0)).asInt() : 0;
                status = code;
                doreturn = true;
                throw new Error("exit(" + code + ")");
            }
            else if (MOD == 1011) { // system
                String cmd = toLuaString(args.elementAt(0));
                // Execute system command
                return new CValue(TYPE_INT, 0);
            }
            return null;
        }
    }
}
// EOF