import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.media.control.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;

/**
 * Python 3 Runtime for J2ME
 * Implementa um subconjunto do Python 3 para dispositivos embarcados
 */
public class Python {
    // Estados de execução
    public boolean breakLoop = false, doreturn = false, kill = true, gc = true;
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0, indentLevel = 0;
    public Hashtable globals = new Hashtable(), father, requireCache = new Hashtable();
    public Vector tokens;
    public int status = 0;
    
    // Tipos de token Python
    public static final int EOF = 0, NUMBER = 1, STRING = 2, BOOLEAN = 3, NONE = 4, 
        IDENTIFIER = 5, INDENT = 6, DEDENT = 7, NEWLINE = 8,
        PLUS = 9, MINUS = 10, MULTIPLY = 11, DIVIDE = 12, MODULO = 13, FLOORDIV = 14,
        EQ = 15, NE = 16, LT = 17, GT = 18, LE = 19, GE = 20,
        AND = 21, OR = 22, NOT = 23, IS = 24, IN = 25,
        ASSIGN = 26, ADD_ASSIGN = 27, SUB_ASSIGN = 28, MUL_ASSIGN = 29,
        IF = 30, ELIF = 31, ELSE = 32, WHILE = 33, FOR = 34,
        DEF = 35, RETURN = 36, BREAK = 37, CONTINUE = 38,
        LPAREN = 39, RPAREN = 40, LBRACKET = 41, RBRACKET = 42,
        LBRACE = 43, RBRACE = 44, COMMA = 45, DOT = 46, COLON = 47,
        CLASS = 48, IMPORT = 49, FROM = 50, AS = 51,
        TRY = 52, EXCEPT = 53, FINALLY = 54, RAISE = 55,
        WITH = 56, AS_CONTEXT = 57, YIELD = 58;
    
    // Constantes
    public static final Boolean TRUE = Boolean.TRUE, FALSE = Boolean.FALSE;
    public static final Object PY_NONE = new Object();
    public static final Object ELLIPSIS = new Object();
    
    // Token
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
    
    // Built-in functions
    public static final int PY_PRINT = 0, PY_LEN = 1, PY_RANGE = 2, PY_LIST = 3, 
        PY_DICT = 4, PY_STR = 5, PY_INT = 6, PY_FLOAT = 7, PY_BOOL = 8,
        PY_TYPE = 9, PY_ISINSTANCE = 10, PY_ISSUNBCLASS = 11, PY_HASATTR = 12,
        PY_GETATTR = 13, PY_SETATTR = 14, PY_DIR = 15, PY_HELP = 16,
        PY_OPEN = 17, PY_READ = 18, PY_WRITE = 19, PY_CLOSE = 20,
        PY_INPUT = 21, PY_SUM = 22, PY_MAX = 23, PY_MIN = 24,
        PY_ABS = 25, PY_ROUND = 26, PY_POW = 27, PY_SORTED = 28,
        PY_ENUMERATE = 29, PY_ZIP = 30, PY_MAP = 31, PY_FILTER = 32,
        PY_ANY = 33, PY_ALL = 34, PY_CHR = 35, PY_ORD = 36,
        PY_HEX = 37, PY_OCT = 38, PY_BIN = 39, PY_FORMAT = 40,
        PY_PROPERTY = 41, PY_STATICMETHOD = 42, PY_CLASSMETHOD = 43;
    
    // Métodos de string Python
    public static final int STR_UPPER = 100, STR_LOWER = 101, STR_STRIP = 102,
        STR_SPLIT = 103, STR_JOIN = 104, STR_REPLACE = 105, STR_FIND = 106,
        STR_STARTSWITH = 107, STR_ENDSWITH = 108, STR_ISALPHA = 109,
        STR_ISDIGIT = 110, STR_ISSPACE = 111, STR_ISUPPER = 112, STR_ISLOWER = 113;
    
    // Métodos de list Python
    public static final int LIST_APPEND = 200, LIST_EXTEND = 201, LIST_INSERT = 202,
        LIST_REMOVE = 203, LIST_POP = 204, LIST_INDEX = 205, LIST_COUNT = 206,
        LIST_SORT = 207, LIST_REVERSE = 208, LIST_CLEAR = 209, LIST_COPY = 210;
    
    // Métodos de dict Python
    public static final int DICT_KEYS = 300, DICT_VALUES = 301, DICT_ITEMS = 302,
        DICT_UPDATE = 303, DICT_GET = 304, DICT_POP = 305, DICT_POPITEM = 306,
        DICT_CLEAR = 307, DICT_COPY = 308, DICT_SETDEFAULT = 309;
    
    // Métodos de set Python
    public static final int SET_ADD = 400, SET_REMOVE = 401, SET_DISCARD = 402,
        SET_UNION = 403, SET_INTERSECTION = 404, SET_DIFFERENCE = 405,
        SET_SYMMETRIC_DIFFERENCE = 406, SET_ISSUBSET = 407, SET_ISSUPERSET = 408;
    
    // Sistema
    public static final int SYS_PATH = 500, SYS_ARGV = 501, SYS_EXIT = 502,
        SYS_GETREFCOUNT = 503, SYS_GETSIZEOF = 504;
    
    // IO
    public static final int OS_GETCWD = 600, OS_CHDIR = 601, OS_LISTDIR = 602,
        OS_MKDIR = 603, OS_RMDIR = 604, OS_REMOVE = 605, OS_RENAME = 606,
        OS_ENVIRON = 607, OS_SYSTEM = 608;
    
    // Time
    public static final int TIME_SLEEP = 700, TIME_TIME = 701, TIME_CTIME = 702;
    
    // Math
    public static final int MATH_SQRT = 800, MATH_SIN = 801, MATH_COS = 802,
        MATH_TAN = 803, MATH_PI = 804, MATH_E = 805;
    
    // Principal
    public Python(OpenPy midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet;
        this.id = id;
        this.PID = pid;
        this.proc = proc;
        this.stdout = stdout;
        this.father = scope;
        this.tokenIndex = 0;
        
        // Módulos Python
        Hashtable builtins = new Hashtable();
        Hashtable sys = new Hashtable();
        Hashtable os = new Hashtable();
        Hashtable time = new Hashtable();
        Hashtable math = new Hashtable();
        
        // Built-in functions
        String[] funcs = {"print", "len", "range", "list", "dict", "str", "int", 
            "float", "bool", "type", "isinstance", "issubclass", "hasattr",
            "getattr", "setattr", "dir", "help", "open", "sum", "max", "min",
            "abs", "round", "pow", "sorted", "enumerate", "zip", "map", "filter",
            "any", "all", "chr", "ord", "hex", "oct", "bin", "format"};
        int[] loaders = {PY_PRINT, PY_LEN, PY_RANGE, PY_LIST, PY_DICT, PY_STR, PY_INT,
            PY_FLOAT, PY_BOOL, PY_TYPE, PY_ISINSTANCE, PY_ISSUNBCLASS, PY_HASATTR,
            PY_GETATTR, PY_SETATTR, PY_DIR, PY_HELP, PY_OPEN, PY_SUM, PY_MAX, PY_MIN,
            PY_ABS, PY_ROUND, PY_POW, PY_SORTED, PY_ENUMERATE, PY_ZIP, PY_MAP, PY_FILTER,
            PY_ANY, PY_ALL, PY_CHR, PY_ORD, PY_HEX, PY_OCT, PY_BIN, PY_FORMAT};
        
        for (int i = 0; i < funcs.length; i++) {
            builtins.put(funcs[i], new PyFunction(loaders[i]));
        }
        
        builtins.put("True", TRUE);
        builtins.put("False", FALSE);
        builtins.put("None", PY_NONE);
        builtins.put("Ellipsis", ELLIPSIS);
        
        globals.put("__builtins__", builtins);
        globals.put("__name__", "__main__");
        globals.put("__doc__", null);
        
        // Sistema
        sys.put("path", new PyList()); // sys.path
        sys.put("argv", new PyList()); // sys.argv
        sys.put("exit", new PyFunction(SYS_EXIT));
        sys.put("getrefcount", new PyFunction(SYS_GETREFCOUNT));
        sys.put("getsizeof", new PyFunction(SYS_GETSIZEOF));
        sys.put("version", "Python 3.8 for J2ME");
        sys.put("platform", System.getProperty("microedition.platform"));
        
        globals.put("sys", sys);
        
        // OS
        os.put("getcwd", new PyFunction(OS_GETCWD));
        os.put("chdir", new PyFunction(OS_CHDIR));
        os.put("listdir", new PyFunction(OS_LISTDIR));
        os.put("mkdir", new PyFunction(OS_MKDIR));
        os.put("rmdir", new PyFunction(OS_RMDIR));
        os.put("remove", new PyFunction(OS_REMOVE));
        os.put("rename", new PyFunction(OS_RENAME));
        os.put("environ", new PyFunction(OS_ENVIRON));
        os.put("system", new PyFunction(OS_SYSTEM));
        os.put("name", "j2me");
        
        globals.put("os", os);
        
        // Time
        time.put("sleep", new PyFunction(TIME_SLEEP));
        time.put("time", new PyFunction(TIME_TIME));
        time.put("ctime", new PyFunction(TIME_CTIME));
        
        globals.put("time", time);
        
        // Math
        math.put("sqrt", new PyFunction(MATH_SQRT));
        math.put("sin", new PyFunction(MATH_SIN));
        math.put("cos", new PyFunction(MATH_COS));
        math.put("tan", new PyFunction(MATH_TAN));
        math.put("pi", new Double(Math.PI));
        math.put("e", new Double(Math.E));
        
        globals.put("math", math);
        
        // Classes built-in
        globals.put("str", new PyStringMeta());
        globals.put("list", new PyListMeta());
        globals.put("dict", new PyDictMeta());
        globals.put("set", new PySetMeta());
        
        globals.put("object", new PyObjectMeta());
        globals.put("type", new PyTypeMeta());
    }
    
    // ==================== PARSER PYTHON ====================
    
    // Tokenizer Python (com indentação significativa)
    public Vector tokenize(String code) throws Exception {
        if (midlet.cachePython.containsKey(code)) {
            return (Vector) midlet.cachePython.get(code);
        }
        
        Vector tokens = new Vector();
        Vector indentStack = new Vector();
        indentStack.addElement(new Integer(0));
        
        String[] lines = splitLines(code);
        boolean firstLine = true;
        
        for (int lineNum = 0; lineNum < lines.length; lineNum++) {
            String line = lines[lineNum];
            int indent = countIndent(line);
            String content = line.substring(indent).trim();
            
            // Pular linhas vazias
            if (content.length() == 0 || content.startsWith("#")) {
                continue;
            }
            
            // Gerenciar indentação
            int currentIndent = ((Integer) indentStack.lastElement()).intValue();
            
            if (indent > currentIndent) {
                tokens.addElement(new Token(INDENT, new Integer(indent)));
                indentStack.addElement(new Integer(indent));
            } else {
                while (indent < currentIndent) {
                    tokens.addElement(new Token(DEDENT, null));
                    indentStack.removeElementAt(indentStack.size() - 1);
                    currentIndent = ((Integer) indentStack.lastElement()).intValue();
                }
            }
            
            if (!firstLine) {
                tokens.addElement(new Token(NEWLINE, "\n"));
            }
            firstLine = false;
            
            // Tokenizar conteúdo da linha
            tokenizeLine(content, tokens);
        }
        
        // Fechar indentação
        while (indentStack.size() > 1) {
            tokens.addElement(new Token(DEDENT, null));
            indentStack.removeElementAt(indentStack.size() - 1);
        }
        
        tokens.addElement(new Token(EOF, "EOF"));
        
        if (midlet.useCache) {
            if (midlet.cachePython.size() > 100) {
                midlet.cachePython.clear();
            }
            midlet.cachePython.put(code, tokens);
        }
        
        return tokens;
    }
    
    private void tokenizeLine(String line, Vector tokens) throws Exception {
        int i = 0;
        int len = line.length();
        
        while (i < len) {
            char c = line.charAt(i);
            
            // Espaços
            if (c == ' ') {
                i++;
                continue;
            }
            
            // Strings
            if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                StringBuffer sb = new StringBuffer();
                
                while (i < len && line.charAt(i) != quote) {
                    if (line.charAt(i) == '\\' && i + 1 < len) {
                        i++;
                        char esc = line.charAt(i);
                        if (esc == 'n') sb.append('\n');
                        else if (esc == 't') sb.append('\t');
                        else if (esc == 'r') sb.append('\r');
                        else if (esc == '\\') sb.append('\\');
                        else if (esc == quote) sb.append(quote);
                        else sb.append(esc);
                    } else {
                        sb.append(line.charAt(i));
                    }
                    i++;
                }
                i++; // Pular quote de fechamento
                tokens.addElement(new Token(STRING, sb.toString()));
                continue;
            }
            
            // Strings triplas
            if (i + 2 < len && line.charAt(i) == '"' && line.charAt(i+1) == '"' && line.charAt(i+2) == '"') {
                i += 3;
                StringBuffer sb = new StringBuffer();
                while (i + 2 < len && !(line.charAt(i) == '"' && line.charAt(i+1) == '"' && line.charAt(i+2) == '"')) {
                    sb.append(line.charAt(i));
                    i++;
                }
                i += 3;
                tokens.addElement(new Token(STRING, sb.toString()));
                continue;
            }
            
            // Números
            if (isDigit(c) || (c == '.' && i+1 < len && isDigit(line.charAt(i+1)))) {
                StringBuffer sb = new StringBuffer();
                boolean hasDecimal = false;
                boolean hasExponent = false;
                
                while (i < len && (isDigit(line.charAt(i)) || line.charAt(i) == '.' || 
                       line.charAt(i) == 'e' || line.charAt(i) == 'E' || 
                       (hasExponent && (line.charAt(i) == '+' || line.charAt(i) == '-')))) {
                    if (line.charAt(i) == '.') {
                        if (hasDecimal) break;
                        hasDecimal = true;
                    }
                    if (line.charAt(i) == 'e' || line.charAt(i) == 'E') {
                        hasExponent = true;
                    }
                    sb.append(line.charAt(i));
                    i++;
                }
                
                try {
                    double num = Double.parseDouble(sb.toString());
                    tokens.addElement(new Token(NUMBER, new Double(num)));
                } catch (NumberFormatException e) {
                    throw new Exception("Invalid number: " + sb.toString());
                }
                continue;
            }
            
            // Operadores de múltiplos caracteres
            if (i + 1 < len) {
                String twoChars = line.substring(i, i+2);
                
                if (twoChars.equals("==")) {
                    tokens.addElement(new Token(EQ, "=="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("!=")) {
                    tokens.addElement(new Token(NE, "!="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("<=")) {
                    tokens.addElement(new Token(LE, "<="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals(">=")) {
                    tokens.addElement(new Token(GE, ">="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("+=")) {
                    tokens.addElement(new Token(ADD_ASSIGN, "+="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("-=")) {
                    tokens.addElement(new Token(SUB_ASSIGN, "-="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("*=")) {
                    tokens.addElement(new Token(MUL_ASSIGN, "*="));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("//")) {
                    tokens.addElement(new Token(FLOORDIV, "//"));
                    i += 2;
                    continue;
                }
                if (twoChars.equals("**")) {
                    tokens.addElement(new Token(POWER, "**"));
                    i += 2;
                    continue;
                }
            }
            
            // Operadores de um caractere
            switch (c) {
                case '+': tokens.addElement(new Token(PLUS, "+")); i++; break;
                case '-': tokens.addElement(new Token(MINUS, "-")); i++; break;
                case '*': tokens.addElement(new Token(MULTIPLY, "*")); i++; break;
                case '/': tokens.addElement(new Token(DIVIDE, "/")); i++; break;
                case '%': tokens.addElement(new Token(MODULO, "%")); i++; break;
                case '(': tokens.addElement(new Token(LPAREN, "(")); i++; break;
                case ')': tokens.addElement(new Token(RPAREN, ")")); i++; break;
                case '[': tokens.addElement(new Token(LBRACKET, "[")); i++; break;
                case ']': tokens.addElement(new Token(RBRACKET, "]")); i++; break;
                case '{': tokens.addElement(new Token(LBRACE, "{")); i++; break;
                case '}': tokens.addElement(new Token(RBRACE, "}")); i++; break;
                case ',': tokens.addElement(new Token(COMMA, ",")); i++; break;
                case '.': tokens.addElement(new Token(DOT, ".")); i++; break;
                case ':': tokens.addElement(new Token(COLON, ":")); i++; break;
                case '=': tokens.addElement(new Token(ASSIGN, "=")); i++; break;
                case '<': tokens.addElement(new Token(LT, "<")); i++; break;
                case '>': tokens.addElement(new Token(GT, ">")); i++; break;
                case '@': tokens.addElement(new Token(DOT, "@")); i++; break;
                
                // Palavras-chave e identificadores
                default:
                    if (isLetter(c) || c == '_') {
                        StringBuffer sb = new StringBuffer();
                        while (i < len && (isLetterOrDigit(line.charAt(i)) || line.charAt(i) == '_')) {
                            sb.append(line.charAt(i));
                            i++;
                        }
                        String word = sb.toString();
                        
                        // Palavras-chave Python
                        if (word.equals("True")) {
                            tokens.addElement(new Token(BOOLEAN, TRUE));
                        } else if (word.equals("False")) {
                            tokens.addElement(new Token(BOOLEAN, FALSE));
                        } else if (word.equals("None")) {
                            tokens.addElement(new Token(NONE, PY_NONE));
                        } else if (word.equals("and")) {
                            tokens.addElement(new Token(AND, "and"));
                        } else if (word.equals("or")) {
                            tokens.addElement(new Token(OR, "or"));
                        } else if (word.equals("not")) {
                            tokens.addElement(new Token(NOT, "not"));
                        } else if (word.equals("is")) {
                            tokens.addElement(new Token(IS, "is"));
                        } else if (word.equals("in")) {
                            tokens.addElement(new Token(IN, "in"));
                        } else if (word.equals("if")) {
                            tokens.addElement(new Token(IF, "if"));
                        } else if (word.equals("elif")) {
                            tokens.addElement(new Token(ELIF, "elif"));
                        } else if (word.equals("else")) {
                            tokens.addElement(new Token(ELSE, "else"));
                        } else if (word.equals("while")) {
                            tokens.addElement(new Token(WHILE, "while"));
                        } else if (word.equals("for")) {
                            tokens.addElement(new Token(FOR, "for"));
                        } else if (word.equals("def")) {
                            tokens.addElement(new Token(DEF, "def"));
                        } else if (word.equals("return")) {
                            tokens.addElement(new Token(RETURN, "return"));
                        } else if (word.equals("break")) {
                            tokens.addElement(new Token(BREAK, "break"));
                        } else if (word.equals("continue")) {
                            tokens.addElement(new Token(CONTINUE, "continue"));
                        } else if (word.equals("class")) {
                            tokens.addElement(new Token(CLASS, "class"));
                        } else if (word.equals("import")) {
                            tokens.addElement(new Token(IMPORT, "import"));
                        } else if (word.equals("from")) {
                            tokens.addElement(new Token(FROM, "from"));
                        } else if (word.equals("as")) {
                            tokens.addElement(new Token(AS, "as"));
                        } else if (word.equals("try")) {
                            tokens.addElement(new Token(TRY, "try"));
                        } else if (word.equals("except")) {
                            tokens.addElement(new Token(EXCEPT, "except"));
                        } else if (word.equals("finally")) {
                            tokens.addElement(new Token(FINALLY, "finally"));
                        } else if (word.equals("raise")) {
                            tokens.addElement(new Token(RAISE, "raise"));
                        } else if (word.equals("with")) {
                            tokens.addElement(new Token(WITH, "with"));
                        } else if (word.equals("yield")) {
                            tokens.addElement(new Token(YIELD, "yield"));
                        } else {
                            tokens.addElement(new Token(IDENTIFIER, word));
                        }
                    } else {
                        throw new Exception("Unexpected character: " + c);
                    }
                    break;
            }
        }
    }
    
    private int countIndent(String line) {
        int spaces = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                spaces++;
            } else if (line.charAt(i) == '\t') {
                spaces += 4;
            } else {
                break;
            }
        }
        return spaces;
    }
    
    private String[] splitLines(String code) {
        Vector lines = new Vector();
        int start = 0;
        for (int i = 0; i < code.length(); i++) {
            if (code.charAt(i) == '\n') {
                lines.addElement(code.substring(start, i));
                start = i + 1;
            }
        }
        if (start < code.length()) {
            lines.addElement(code.substring(start));
        }
        String[] result = new String[lines.size()];
        lines.copyInto(result);
        return result;
    }
    
    // ==================== EXECUÇÃO ====================
    
    public Hashtable run(String source, String code, Hashtable args) {
        midlet.sys.put(PID, proc);
        globals.put("__file__", source);
        if (args != null) {
            globals.put("sys_argv", args);
        }
        
        Hashtable result = new Hashtable();
        
        try {
            this.tokens = tokenize(code);
            
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
            midlet.print(midlet.getCatch(e), stdout, id, father);
            status = 1;
        } catch (Error e) {
            if (e.getMessage() != null) {
                midlet.print(e.getMessage(), stdout, id, father);
            }
            status = 1;
        }
        
        if (kill) {
            midlet.sys.remove(PID);
        }
        result.put("status", new Integer(status));
        return result;
    }
    
    public Token peek() {
        if (tokenIndex < tokens.size()) {
            return (Token) tokens.elementAt(tokenIndex);
        }
        return new Token(EOF, "EOF");
    }
    
    public Token peekNext() {
        if (tokenIndex + 1 < tokens.size()) {
            return (Token) tokens.elementAt(tokenIndex + 1);
        }
        return new Token(EOF, "EOF");
    }
    
    private Token consume() {
        if (tokenIndex < tokens.size()) {
            return (Token) tokens.elementAt(tokenIndex++);
        }
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
    
    // ==================== STATEMENTS ====================
    
    private Object statement(Hashtable scope) throws Exception {
        Token current = peek();
        
        if (status != 0) {
            midlet.sys.remove(PID);
            throw new Error();
        }
        
        if (midlet.sys.containsKey(PID)) {
        } else {
            throw new Error("Process killed");
        }
        
        // Comentários já foram ignorados
        
        // If statement
        if (current.type == IF) {
            return ifStatement(scope);
        }
        
        // While loop
        if (current.type == WHILE) {
            return whileStatement(scope);
        }
        
        // For loop
        if (current.type == FOR) {
            return forStatement(scope);
        }
        
        // Break
        if (current.type == BREAK) {
            if (loopDepth == 0) {
                throw new Exception("'break' outside loop");
            }
            consume(BREAK);
            breakLoop = true;
            return null;
        }
        
        // Continue
        if (current.type == CONTINUE) {
            if (loopDepth == 0) {
                throw new Exception("'continue' outside loop");
            }
            consume(CONTINUE);
            breakLoop = true; // Será tratado como continue
            return null;
        }
        
        // Return
        if (current.type == RETURN) {
            consume(RETURN);
            doreturn = true;
            
            if (peek().type == EOF || peek().type == DEDENT) {
                return PY_NONE;
            }
            
            return expression(scope);
        }
        
        // Function definition
        if (current.type == DEF) {
            return functionDef(scope);
        }
        
        // Class definition
        if (current.type == CLASS) {
            return classDef(scope);
        }
        
        // Import
        if (current.type == IMPORT) {
            return importStatement(scope);
        }
        
        // From ... import
        if (current.type == FROM) {
            return fromImport(scope);
        }
        
        // Expression (pode ser assignment)
        return expression(scope);
    }
    
    private Object ifStatement(Hashtable scope) throws Exception {
        consume(IF);
        Object condition = expression(scope);
        consume(COLON);
        
        Object result = null;
        boolean taken = false;
        
        // Corpo do if
        if (isTruthy(condition)) {
            taken = true;
            while (peek().type != ELIF && peek().type != ELSE && 
                   peek().type != DEDENT && peek().type != EOF) {
                result = statement(scope);
                if (doreturn) return result;
            }
        } else {
            skipUntilElifOrElse();
        }
        
        // Elif blocks
        while (peek().type == ELIF) {
            consume(ELIF);
            condition = expression(scope);
            consume(COLON);
            
            if (!taken && isTruthy(condition)) {
                taken = true;
                while (peek().type != ELIF && peek().type != ELSE &&
                       peek().type != DEDENT && peek().type != EOF) {
                    result = statement(scope);
                    if (doreturn) return result;
                }
            } else {
                skipUntilElifOrElse();
            }
        }
        
        // Else block
        if (peek().type == ELSE) {
            consume(ELSE);
            consume(COLON);
            
            if (!taken) {
                while (peek().type != DEDENT && peek().type != EOF) {
                    result = statement(scope);
                    if (doreturn) return result;
                }
            }
        }
        
        return result;
    }
    
    private Object whileStatement(Hashtable scope) throws Exception {
        consume(WHILE);
        int conditionStart = tokenIndex;
        
        loopDepth++;
        
        while (true) {
            tokenIndex = conditionStart;
            Object condition = expression(scope);
            consume(COLON);
            
            if (!isTruthy(condition) || breakLoop) {
                // Pular corpo
                int depth = 1;
                while (depth > 0) {
                    Token t = consume();
                    if (t.type == IF || t.type == WHILE || t.type == FOR || t.type == DEF) {
                        depth++;
                    } else if (t.type == DEDENT) {
                        depth--;
                        if (depth == 0) break;
                    } else if (t.type == EOF) {
                        throw new Exception("Unmatched 'while'");
                    }
                }
                break;
            }
            
            boolean continueLoop = false;
            
            while (peek().type != DEDENT) {
                Object result = statement(scope);
                if (doreturn) {
                    loopDepth--;
                    return result;
                }
                if (breakLoop) {
                    breakLoop = false;
                    break;
                }
                if (breakLoop && breakLoop == true) { // Continue
                    continueLoop = true;
                    break;
                }
            }
            
            if (continueLoop) {
                breakLoop = false;
                continue;
            }
            
            if (breakLoop) {
                breakLoop = false;
                break;
            }
            
            tokenIndex = conditionStart;
        }
        
        loopDepth--;
        return null;
    }
    
    private Object forStatement(Hashtable scope) throws Exception {
        consume(FOR);
        
        String varName = (String) consume(IDENTIFIER).value;
        consume(IN);
        Object iterable = expression(scope);
        consume(COLON);
        
        loopDepth++;
        
        // Coletar corpo
        Vector bodyTokens = new Vector();
        int depth = 1;
        while (depth > 0) {
            Token t = consume();
            if (t.type == IF || t.type == WHILE || t.type == FOR || t.type == DEF) {
                depth++;
            } else if (t.type == DEDENT) {
                depth--;
            } else if (t.type == EOF) {
                throw new Exception("Unmatched 'for'");
            }
            if (depth > 0) {
                bodyTokens.addElement(t);
            }
        }
        
        // Iterar
        Object iterator = getIterator(iterable);
        Object result = null;
        
        while (true) {
            Object nextValue = nextValue(iterator);
            if (nextValue == null) break;
            
            if (breakLoop) {
                breakLoop = false;
                break;
            }
            
            scope.put(varName, nextValue);
            
            int savedIndex = tokenIndex;
            Vector savedTokens = tokens;
            tokens = bodyTokens;
            tokenIndex = 0;
            
            while (peek().type != EOF) {
                result = statement(scope);
                if (doreturn) {
                    loopDepth--;
                    return result;
                }
                if (breakLoop) break;
            }
            
            tokenIndex = savedIndex;
            tokens = savedTokens;
        }
        
        loopDepth--;
        return result;
    }
    
    private Object functionDef(Hashtable scope) throws Exception {
        consume(DEF);
        String funcName = (String) consume(IDENTIFIER).value;
        consume(LPAREN);
        
        Vector params = new Vector();
        Vector defaults = new Vector();
        
        while (peek().type != RPAREN) {
            if (peek().type == IDENTIFIER) {
                String param = (String) consume(IDENTIFIER).value;
                Object defaultValue = null;
                
                if (peek().type == ASSIGN) {
                    consume(ASSIGN);
                    defaultValue = expression(scope);
                    defaults.addElement(defaultValue);
                }
                
                params.addElement(param);
                
                if (peek().type == COMMA) {
                    consume(COMMA);
                }
            }
        }
        
        consume(RPAREN);
        consume(COLON);
        
        // Coletar corpo
        Vector bodyTokens = new Vector();
        int depth = 1;
        while (depth > 0) {
            Token t = consume();
            if (t.type == DEF || t.type == CLASS || t.type == IF || 
                t.type == WHILE || t.type == FOR) {
                depth++;
            } else if (t.type == DEDENT) {
                depth--;
            } else if (t.type == EOF) {
                throw new Exception("Unmatched 'def'");
            }
            if (depth > 0) {
                bodyTokens.addElement(t);
            }
        }
        
        PyFunction func = new PyFunction(params, defaults, bodyTokens, scope);
        scope.put(funcName, func);
        
        return null;
    }
    
    private Object classDef(Hashtable scope) throws Exception {
        consume(CLASS);
        String className = (String) consume(IDENTIFIER).value;
        
        Vector bases = new Vector();
        
        if (peek().type == LPAREN) {
            consume(LPAREN);
            while (peek().type != RPAREN) {
                Object base = expression(scope);
                bases.addElement(base);
                if (peek().type == COMMA) {
                    consume(COMMA);
                }
            }
            consume(RPAREN);
        }
        
        consume(COLON);
        
        // Coletar corpo
        Vector bodyTokens = new Vector();
        int depth = 1;
        while (depth > 0) {
            Token t = consume();
            if (t.type == DEF || t.type == CLASS) {
                depth++;
            } else if (t.type == DEDENT) {
                depth--;
            } else if (t.type == EOF) {
                throw new Exception("Unmatched 'class'");
            }
            if (depth > 0) {
                bodyTokens.addElement(t);
            }
        }
        
        // Executar corpo no namespace da classe
        Hashtable classDict = new Hashtable();
        int savedIndex = tokenIndex;
        Vector savedTokens = tokens;
        tokens = bodyTokens;
        tokenIndex = 0;
        
        while (peek().type != EOF) {
            statement(classDict);
        }
        
        tokenIndex = savedIndex;
        tokens = savedTokens;
        
        // Criar classe
        PyClass cls = new PyClass(className, bases, classDict);
        scope.put(className, cls);
        
        return null;
    }
    
    private Object importStatement(Hashtable scope) throws Exception {
        consume(IMPORT);
        
        Vector modules = new Vector();
        Vector aliases = new Vector();
        
        modules.addElement(consume(IDENTIFIER).value);
        
        while (peek().type == COMMA) {
            consume(COMMA);
            modules.addElement(consume(IDENTIFIER).value);
        }
        
        // Importar módulos
        for (int i = 0; i < modules.size(); i++) {
            String moduleName = (String) modules.elementAt(i);
            importModule(moduleName, moduleName, scope);
        }
        
        return null;
    }
    
    private Object fromImport(Hashtable scope) throws Exception {
        consume(FROM);
        String moduleName = (String) consume(IDENTIFIER).value;
        consume(IMPORT);
        
        if (peek().type == MULTIPLY) {
            consume(MULTIPLY);
            importAll(moduleName, scope);
        } else {
            Vector names = new Vector();
            Vector aliases = new Vector();
            
            names.addElement(consume(IDENTIFIER).value);
            
            if (peek().type == AS) {
                consume(AS);
                aliases.addElement(consume(IDENTIFIER).value);
            } else {
                aliases.addElement(names.lastElement());
            }
            
            while (peek().type == COMMA) {
                consume(COMMA);
                String name = (String) consume(IDENTIFIER).value;
                names.addElement(name);
                
                if (peek().type == AS) {
                    consume(AS);
                    aliases.addElement(consume(IDENTIFIER).value);
                } else {
                    aliases.addElement(name);
                }
            }
            
            Object module = importModule(moduleName, null, scope);
            
            for (int i = 0; i < names.size(); i++) {
                String name = (String) names.elementAt(i);
                String alias = (String) aliases.elementAt(i);
                Object value = getAttribute(module, name);
                scope.put(alias, value);
            }
        }
        
        return null;
    }
    
    private Object importModule(String name, String alias, Hashtable scope) throws Exception {
        Object cached = requireCache.get(name);
        if (cached != null) {
            if (alias != null) {
                scope.put(alias, cached);
            }
            return cached;
        }
        
        String code = midlet.getcontent("/lib/" + name + ".py", father);
        if (code.equals("")) {
            throw new Exception("Module '" + name + "' not found");
        }
        
        Hashtable moduleScope = new Hashtable();
        moduleScope.put("__name__", name);
        moduleScope.put("__file__", "/lib/" + name + ".py");
        
        Python subPython = new Python(midlet, id, PID + "." + name, proc, stdout, fd);
        subPython.kill = false;
        
        subPython.run(name, code, null);
        
        requireCache.put(name, subPython.globals);
        
        if (alias != null) {
            scope.put(alias, subPython.globals);
        }
        
        return subPython.globals;
    }
    
    private void importAll(String moduleName, Hashtable scope) throws Exception {
        Object module = importModule(moduleName, null, scope);
        
        if (module instanceof Hashtable) {
            Hashtable moduleDict = (Hashtable) module;
            Enumeration keys = moduleDict.keys();
            
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (!key.startsWith("_")) {
                    scope.put(key, moduleDict.get(key));
                }
            }
        }
    }
    
    // ==================== EXPRESSIONS ====================
    
    private Object expression(Hashtable scope) throws Exception {
        return assignment(scope);
    }
    
    private Object assignment(Hashtable scope) throws Exception {
        Object left = logicalOr(scope);
        
        if (peek().type == ASSIGN) {
            consume(ASSIGN);
            Object right = assignment(scope);
            
            if (left instanceof String) {
                String varName = (String) left;
                scope.put(varName, right);
            } else if (left instanceof PyAttribute) {
                PyAttribute attr = (PyAttribute) left;
                setAttribute(attr.obj, attr.name, right);
            } else {
                throw new Exception("Cannot assign to " + getTypeName(left));
            }
            
            return right;
        } else if (peek().type == ADD_ASSIGN || peek().type == SUB_ASSIGN || 
                   peek().type == MUL_ASSIGN) {
            Token op = consume();
            Object right = assignment(scope);
            Object result = null;
            
            if (op.type == ADD_ASSIGN) {
                result = add(left, right);
            } else if (op.type == SUB_ASSIGN) {
                result = subtract(left, right);
            } else if (op.type == MUL_ASSIGN) {
                result = multiply(left, right);
            }
            
            if (left instanceof String) {
                scope.put((String) left, result);
            } else if (left instanceof PyAttribute) {
                PyAttribute attr = (PyAttribute) left;
                setAttribute(attr.obj, attr.name, result);
            }
            
            return result;
        }
        
        return left;
    }
    
    private Object logicalOr(Hashtable scope) throws Exception {
        Object left = logicalAnd(scope);
        
        while (peek().type == OR) {
            consume(OR);
            Object right = logicalAnd(scope);
            left = isTruthy(left) ? left : right;
        }
        
        return left;
    }
    
    private Object logicalAnd(Hashtable scope) throws Exception {
        Object left = comparison(scope);
        
        while (peek().type == AND) {
            consume(AND);
            Object right = comparison(scope);
            left = isTruthy(left) ? right : left;
        }
        
        return left;
    }
    
    private Object comparison(Hashtable scope) throws Exception {
        Object left = addition(scope);
        
        while (true) {
            int opType = peek().type;
            
            if (opType == EQ || opType == NE || opType == LT || opType == GT ||
                opType == LE || opType == GE || opType == IS || opType == IN ||
                opType == NOT) {
                
                boolean isNot = false;
                
                if (opType == NOT) {
                    consume(NOT);
                    opType = peek().type;
                    isNot = true;
                }
                
                Token op = consume();
                Object right = addition(scope);
                boolean result = false;
                
                if (op.type == EQ) {
                    result = equals(left, right);
                } else if (op.type == NE) {
                    result = !equals(left, right);
                } else if (op.type == LT) {
                    result = compare(left, right) < 0;
                } else if (op.type == GT) {
                    result = compare(left, right) > 0;
                } else if (op.type == LE) {
                    result = compare(left, right) <= 0;
                } else if (op.type == GE) {
                    result = compare(left, right) >= 0;
                } else if (op.type == IS) {
                    result = (left == right);
                } else if (op.type == IN) {
                    result = contains(right, left);
                }
                
                if (isNot) {
                    result = !result;
                }
                
                left = result ? TRUE : FALSE;
            } else {
                break;
            }
        }
        
        return left;
    }
    
    private Object addition(Hashtable scope) throws Exception {
        Object left = multiplication(scope);
        
        while (peek().type == PLUS || peek().type == MINUS) {
            Token op = consume();
            Object right = multiplication(scope);
            
            if (op.type == PLUS) {
                left = add(left, right);
            } else {
                left = subtract(left, right);
            }
        }
        
        return left;
    }
    
    private Object multiplication(Hashtable scope) throws Exception {
        Object left = power(scope);
        
        while (peek().type == MULTIPLY || peek().type == DIVIDE || 
               peek().type == MODULO || peek().type == FLOORDIV) {
            Token op = consume();
            Object right = power(scope);
            
            if (op.type == MULTIPLY) {
                left = multiply(left, right);
            } else if (op.type == DIVIDE) {
                left = divide(left, right);
            } else if (op.type == MODULO) {
                left = modulo(left, right);
            } else if (op.type == FLOORDIV) {
                left = floorDivide(left, right);
            }
        }
        
        return left;
    }
    
    private Object power(Hashtable scope) throws Exception {
        Object left = factor(scope);
        
        if (peek().type == POWER) {
            consume(POWER);
            Object right = power(scope);
            left = pow(left, right);
        }
        
        return left;
    }
    
    private Object factor(Hashtable scope) throws Exception {
        Token current = peek();
        
        // Literais
        if (current.type == NUMBER) {
            consume(NUMBER);
            return current.value;
        }
        
        if (current.type == STRING) {
            consume(STRING);
            return current.value;
        }
        
        if (current.type == BOOLEAN) {
            consume(BOOLEAN);
            return current.value;
        }
        
        if (current.type == NONE) {
            consume(NONE);
            return PY_NONE;
        }
        
        // Not
        if (current.type == NOT) {
            consume(NOT);
            Object value = factor(scope);
            return isTruthy(value) ? FALSE : TRUE;
        }
        
        // Unary minus
        if (current.type == MINUS) {
            consume(MINUS);
            Object value = factor(scope);
            if (value instanceof Double) {
                return new Double(-((Double) value).doubleValue());
            }
            throw new Exception("Bad operand type for unary -: " + getTypeName(value));
        }
        
        // Parentheses
        if (current.type == LPAREN) {
            consume(LPAREN);
            Object value = expression(scope);
            consume(RPAREN);
            return value;
        }
        
        // List literal
        if (current.type == LBRACKET) {
            return listLiteral(scope);
        }
        
        // Dict literal
        if (current.type == LBRACE) {
            return dictLiteral(scope);
        }
        
        // Lambda
        if (current.type == IDENTIFIER && current.value.equals("lambda")) {
            return lambdaExpr(scope);
        }
        
        // Identificador
        if (current.type == IDENTIFIER) {
            String name = (String) consume(IDENTIFIER).value;
            
            // Atributo (obj.attr)
            if (peek().type == DOT) {
                consume(DOT);
                String attr = (String) consume(IDENTIFIER).value;
                Object obj = lookup(name, scope);
                return new PyAttribute(obj, attr);
            }
            
            // Chamada de função (func(args))
            if (peek().type == LPAREN) {
                Object func = lookup(name, scope);
                return callFunction(func, scope);
            }
            
            // Subscrição (list[index])
            if (peek().type == LBRACKET) {
                consume(LBRACKET);
                Object index = expression(scope);
                consume(RBRACKET);
                Object obj = lookup(name, scope);
                return getItem(obj, index);
            }
            
            return lookup(name, scope);
        }
        
        throw new Exception("Unexpected token: " + current);
    }
    
    private Object listLiteral(Hashtable scope) throws Exception {
        consume(LBRACKET);
        PyList list = new PyList();
        
        if (peek().type != RBRACKET) {
            while (true) {
                Object value = expression(scope);
                list.append(value);
                
                if (peek().type == COMMA) {
                    consume(COMMA);
                } else {
                    break;
                }
            }
        }
        
        consume(RBRACKET);
        return list;
    }
    
    private Object dictLiteral(Hashtable scope) throws Exception {
        consume(LBRACE);
        PyDict dict = new PyDict();
        
        if (peek().type != RBRACE) {
            while (true) {
                Object key = expression(scope);
                consume(COLON);
                Object value = expression(scope);
                dict.put(key, value);
                
                if (peek().type == COMMA) {
                    consume(COMMA);
                } else {
                    break;
                }
            }
        }
        
        consume(RBRACE);
        return dict;
    }
    
    private Object lambdaExpr(Hashtable scope) throws Exception {
        consume(LAMBDA);
        
        Vector params = new Vector();
        
        if (peek().type == IDENTIFIER) {
            params.addElement(consume(IDENTIFIER).value);
            
            while (peek().type == COMMA) {
                consume(COMMA);
                params.addElement(consume(IDENTIFIER).value);
            }
        }
        
        consume(COLON);
        
        int savedIndex = tokenIndex;
        Object result = expression(scope);
        
        // Criar função lambda
        PyFunction lambda = new PyFunction(params, new Vector(), result, scope);
        return lambda;
    }
    
    private Object callFunction(Object func, Hashtable scope) throws Exception {
        consume(LPAREN);
        Vector args = new Vector();
        
        if (peek().type != RPAREN) {
            while (true) {
                args.addElement(expression(scope));
                
                if (peek().type == COMMA) {
                    consume(COMMA);
                } else {
                    break;
                }
            }
        }
        
        consume(RPAREN);
        
        if (func instanceof PyFunction) {
            return ((PyFunction) func).call(args);
        } else if (func instanceof PyClass) {
            return ((PyClass) func).instantiate(args);
        } else if (func instanceof PyBoundMethod) {
            return ((PyBoundMethod) func).call(args);
        } else if (func instanceof PyBuiltinFunction) {
            return ((PyBuiltinFunction) func).call(args);
        } else {
            throw new Exception("'" + getTypeName(func) + "' object is not callable");
        }
    }
    
    private Object lookup(String name, Hashtable scope) {
        if (scope.containsKey(name)) {
            return scope.get(name);
        }
        
        if (globals.containsKey(name)) {
            return globals.get(name);
        }
        
        Hashtable builtins = (Hashtable) globals.get("__builtins__");
        if (builtins.containsKey(name)) {
            return builtins.get(name);
        }
        
        return null;
    }
    
    // ==================== HELPER FUNCTIONS ====================
    
    private int compare(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            double da = ((Double) a).doubleValue();
            double db = ((Double) b).doubleValue();
            return da < db ? -1 : (da > db ? 1 : 0);
        }
        
        if (a instanceof String && b instanceof String) {
            return ((String) a).compareTo((String) b);
        }
        
        throw new Exception("Unorderable types: " + getTypeName(a) + " and " + getTypeName(b));
    }
    
    private boolean equals(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof PyObject && b instanceof PyObject) {
            return ((PyObject) a).__eq__(b);
        }
        return a.equals(b);
    }
    
    private Object add(Object a, Object b) throws Exception {
        if (a instanceof String && b instanceof String) {
            return (String) a + (String) b;
        }
        if (a instanceof Double && b instanceof Double) {
            return new Double(((Double) a).doubleValue() + ((Double) b).doubleValue());
        }
        if (a instanceof PyList && b instanceof PyList) {
            PyList result = new PyList();
            result.extend((PyList) a);
            result.extend((PyList) b);
            return result;
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " + " + getTypeName(b));
    }
    
    private Object subtract(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            return new Double(((Double) a).doubleValue() - ((Double) b).doubleValue());
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " - " + getTypeName(b));
    }
    
    private Object multiply(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            return new Double(((Double) a).doubleValue() * ((Double) b).doubleValue());
        }
        if (a instanceof String && b instanceof Double) {
            int times = ((Double) b).intValue();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < times; i++) {
                sb.append((String) a);
            }
            return sb.toString();
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " * " + getTypeName(b));
    }
    
    private Object divide(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            double divisor = ((Double) b).doubleValue();
            if (divisor == 0) {
                throw new Exception("Division by zero");
            }
            return new Double(((Double) a).doubleValue() / divisor);
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " / " + getTypeName(b));
    }
    
    private Object floorDivide(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            double divisor = ((Double) b).doubleValue();
            if (divisor == 0) {
                throw new Exception("Division by zero");
            }
            return new Double(Math.floor(((Double) a).doubleValue() / divisor));
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " // " + getTypeName(b));
    }
    
    private Object modulo(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            double divisor = ((Double) b).doubleValue();
            if (divisor == 0) {
                throw new Exception("Modulo by zero");
            }
            return new Double(((Double) a).doubleValue() % divisor);
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " % " + getTypeName(b));
    }
    
    private Object pow(Object a, Object b) throws Exception {
        if (a instanceof Double && b instanceof Double) {
            return new Double(Math.pow(((Double) a).doubleValue(), ((Double) b).doubleValue()));
        }
        throw new Exception("Unsupported operand types: " + getTypeName(a) + " ** " + getTypeName(b));
    }
    
    private boolean isTruthy(Object value) {
        if (value == null || value == PY_NONE) return false;
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Double) return ((Double) value).doubleValue() != 0;
        if (value instanceof String) return ((String) value).length() > 0;
        if (value instanceof PyList) return ((PyList) value).size() > 0;
        if (value instanceof PyDict) return ((PyDict) value).size() > 0;
        return true;
    }
    
    private String getTypeName(Object obj) {
        if (obj == null || obj == PY_NONE) return "NoneType";
        if (obj instanceof String) return "str";
        if (obj instanceof Double) return "int";
        if (obj instanceof Boolean) return "bool";
        if (obj instanceof PyList) return "list";
        if (obj instanceof PyDict) return "dict";
        if (obj instanceof PyFunction) return "function";
        if (obj instanceof PyClass) return "type";
        if (obj instanceof PyObject) return ((PyObject) obj).__class__.__name__;
        return obj.getClass().getSimpleName();
    }
    
    private void setAttribute(Object obj, String name, Object value) throws Exception {
        if (obj instanceof Hashtable) {
            ((Hashtable) obj).put(name, value);
        } else if (obj instanceof PyObject) {
            ((PyObject) obj).__setattr__(name, value);
        } else {
            throw new Exception("'" + getTypeName(obj) + "' object has no attribute '" + name + "'");
        }
    }
    
    private Object getAttribute(Object obj, String name) throws Exception {
        if (obj instanceof Hashtable) {
            return ((Hashtable) obj).get(name);
        } else if (obj instanceof Python.PyObject) {
            return ((Python.PyObject) obj).__getattr__(name);
        }
        return null;
    }
    
    private Object getItem(Object obj, Object index) throws Exception {
        if (obj instanceof String) {
            int idx = ((Double) index).intValue();
            String s = (String) obj;
            if (idx < 0) idx = s.length() + idx;
            if (idx < 0 || idx >= s.length()) {
                throw new Exception("string index out of range");
            }
            return String.valueOf(s.charAt(idx));
        }
        if (obj instanceof PyList) {
            return ((PyList) obj).get(index);
        }
        if (obj instanceof PyDict) {
            return ((PyDict) obj).get(index);
        }
        throw new Exception("'" + getTypeName(obj) + "' object is not subscriptable");
    }
    
    private Object getIterator(Object iterable) throws Exception {
        if (iterable instanceof String) {
            String s = (String) iterable;
            return new PyStringIterator(s);
        }
        if (iterable instanceof PyList) {
            return ((PyList) iterable).iterator();
        }
        if (iterable instanceof PyDict) {
            return ((PyDict) iterable).keys().iterator();
        }
        if (iterable instanceof PyRange) {
            return ((PyRange) iterable).iterator();
        }
        throw new Exception("'" + getTypeName(iterable) + "' object is not iterable");
    }
    
    private Object nextValue(Object iterator) throws Exception {
        if (iterator instanceof PyIterator) {
            return ((PyIterator) iterator).next();
        }
        return null;
    }
    
    private boolean contains(Object container, Object item) throws Exception {
        if (container instanceof String) {
            return ((String) container).indexOf(item.toString()) != -1;
        }
        if (container instanceof PyList) {
            return ((PyList) container).contains(item);
        }
        if (container instanceof PyDict) {
            return ((PyDict) container).containsKey(item);
        }
        throw new Exception("'" + getTypeName(container) + "' object is not a container");
    }
    
    private void skipUntilElifOrElse() throws Exception {
        int depth = 1;
        while (depth > 0) {
            Token t = consume();
            if (t.type == IF || t.type == WHILE || t.type == FOR || t.type == DEF) {
                depth++;
            } else if (t.type == DEDENT) {
                depth--;
                if (depth == 0) {
                    tokenIndex--;
                    return;
                }
            } else if ((t.type == ELIF || t.type == ELSE) && depth == 1) {
                tokenIndex--;
                return;
            }
        }
    }
    
    // ==================== CLASSES PYTHON ====================
    
    // Classe base para objetos Python
    public static class PyObject {
        public PyClass __class__;
        public Hashtable __dict__ = new Hashtable();
        
        public PyObject(PyClass cls) {
            this.__class__ = cls;
        }
        
        public Object __getattr__(String name) {
            if (__dict__.containsKey(name)) {
                return __dict__.get(name);
            }
            if (__class__ != null && __class__.__dict__.containsKey(name)) {
                Object attr = __class__.__dict__.get(name);
                if (attr instanceof PyFunction) {
                    return new PyBoundMethod(this, (PyFunction) attr);
                }
                return attr;
            }
            return null;
        }
        
        public void __setattr__(String name, Object value) {
            __dict__.put(name, value);
        }
        
        public boolean __eq__(Object other) {
            return this == other;
        }
        
        public String __str__() {
            return "<" + __class__.__name__ + " object at " + hashCode() + ">";
        }
    }
    
    // Classe Python
    public static class PyClass {
        public String __name__;
        public Vector __bases__;
        public Hashtable __dict__ = new Hashtable();
        
        public PyClass(String name, Vector bases, Hashtable dict) {
            this.__name__ = name;
            this.__bases__ = bases;
            this.__dict__ = dict;
        }
        
        public PyObject instantiate(Vector args) throws Exception {
            PyObject instance = new PyObject(this);
            
            // Chamar __init__ se existir
            Object init = __dict__.get("__init__");
            if (init instanceof PyFunction) {
                Vector callArgs = new Vector();
                callArgs.addElement(instance);
                for (int i = 0; i < args.size(); i++) {
                    callArgs.addElement(args.elementAt(i));
                }
                ((PyFunction) init).call(callArgs);
            }
            
            return instance;
        }
        
        public String __str__() {
            return "<class '" + __name__ + "'>";
        }
    }
    
    // Lista Python
    public static class PyList {
        private Vector elements = new Vector();
        
        public void append(Object item) {
            elements.addElement(item);
        }
        
        public void extend(PyList other) {
            for (int i = 0; i < other.size(); i++) {
                elements.addElement(other.get(i));
            }
        }
        
        public void insert(int index, Object item) {
            elements.insertElementAt(item, index);
        }
        
        public Object pop(int index) {
            Object item = elements.elementAt(index);
            elements.removeElementAt(index);
            return item;
        }
        
        public Object pop() {
            return pop(size() - 1);
        }
        
        public void remove(Object item) {
            for (int i = 0; i < elements.size(); i++) {
                if (elements.elementAt(i).equals(item)) {
                    elements.removeElementAt(i);
                    break;
                }
            }
        }
        
        public int index(Object item) {
            for (int i = 0; i < elements.size(); i++) {
                if (elements.elementAt(i).equals(item)) {
                    return i;
                }
            }
            return -1;
        }
        
        public int count(Object item) {
            int c = 0;
            for (int i = 0; i < elements.size(); i++) {
                if (elements.elementAt(i).equals(item)) {
                    c++;
                }
            }
            return c;
        }
        
        public void sort() {
            // Bubble sort simples
            for (int i = 0; i < elements.size() - 1; i++) {
                for (int j = 0; j < elements.size() - i - 1; j++) {
                    Object a = elements.elementAt(j);
                    Object b = elements.elementAt(j + 1);
                    if (compareElements(a, b) > 0) {
                        elements.setElementAt(b, j);
                        elements.setElementAt(a, j + 1);
                    }
                }
            }
        }
        
        public void reverse() {
            Vector reversed = new Vector();
            for (int i = elements.size() - 1; i >= 0; i--) {
                reversed.addElement(elements.elementAt(i));
            }
            elements = reversed;
        }
        
        public void clear() {
            elements.removeAllElements();
        }
        
        public PyList copy() {
            PyList newList = new PyList();
            for (int i = 0; i < elements.size(); i++) {
                newList.append(elements.elementAt(i));
            }
            return newList;
        }
        
        public Object get(Object index) {
            int idx = ((Double) index).intValue();
            if (idx < 0) idx = elements.size() + idx;
            return elements.elementAt(idx);
        }
        
        public Object get(int index) {
            return elements.elementAt(index);
        }
        
        public void set(int index, Object value) {
            elements.setElementAt(value, index);
        }
        
        public int size() {
            return elements.size();
        }
        
        public boolean contains(Object item) {
            return elements.contains(item);
        }
        
        public PyIterator iterator() {
            return new PyListIterator(this);
        }
        
        public String __str__() {
            StringBuffer sb = new StringBuffer("[");
            for (int i = 0; i < elements.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(stringify(elements.elementAt(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        
        private int compareElements(Object a, Object b) {
            if (a instanceof Double && b instanceof Double) {
                double da = ((Double) a).doubleValue();
                double db = ((Double) b).doubleValue();
                return da < db ? -1 : (da > db ? 1 : 0);
            }
            return a.toString().compareTo(b.toString());
        }
        
        private String stringify(Object obj) {
            if (obj instanceof String) return "'" + obj + "'";
            if (obj == null) return "None";
            return obj.toString();
        }
    }
    
    // Dicionário Python
    public static class PyDict {
        private Hashtable map = new Hashtable();
        
        public void put(Object key, Object value) {
            map.put(key, value);
        }
        
        public Object get(Object key) {
            return map.get(key);
        }
        
        public Object get(Object key, Object defaultValue) {
            Object value = map.get(key);
            return value == null ? defaultValue : value;
        }
        
        public Object pop(Object key) {
            Object value = map.get(key);
            map.remove(key);
            return value;
        }
        
        public Object popitem() {
            Enumeration keys = map.keys();
            if (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                Object value = map.get(key);
                map.remove(key);
                return new Object[]{key, value};
            }
            return null;
        }
        
        public void update(PyDict other) {
            Enumeration keys = other.map.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                map.put(key, other.map.get(key));
            }
        }
        
        public void clear() {
            map.clear();
        }
        
        public PyDict copy() {
            PyDict newDict = new PyDict();
            Enumeration keys = map.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                newDict.put(key, map.get(key));
            }
            return newDict;
        }
        
        public Object setdefault(Object key, Object defaultValue) {
            if (!map.containsKey(key)) {
                map.put(key, defaultValue);
                return defaultValue;
            }
            return map.get(key);
        }
        
        public PyList keys() {
            PyList keys = new PyList();
            Enumeration e = map.keys();
            while (e.hasMoreElements()) {
                keys.append(e.nextElement());
            }
            return keys;
        }
        
        public PyList values() {
            PyList values = new PyList();
            Enumeration e = map.elements();
            while (e.hasMoreElements()) {
                values.append(e.nextElement());
            }
            return values;
        }
        
        public PyList items() {
            PyList items = new PyList();
            Enumeration keys = map.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                PyList pair = new PyList();
                pair.append(key);
                pair.append(map.get(key));
                items.append(pair);
            }
            return items;
        }
        
        public int size() {
            return map.size();
        }
        
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }
        
        public String __str__() {
            StringBuffer sb = new StringBuffer("{");
            Enumeration keys = map.keys();
            boolean first = true;
            while (keys.hasMoreElements()) {
                if (!first) sb.append(", ");
                Object key = keys.nextElement();
                sb.append(stringify(key)).append(": ").append(stringify(map.get(key)));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        
        private String stringify(Object obj) {
            if (obj instanceof String) return "'" + obj + "'";
            if (obj == null) return "None";
            return obj.toString();
        }
    }
    
    // Range Python
    public static class PyRange {
        private int start, stop, step;
        
        public PyRange(int stop) {
            this(0, stop, 1);
        }
        
        public PyRange(int start, int stop) {
            this(start, stop, 1);
        }
        
        public PyRange(int start, int stop, int step) {
            this.start = start;
            this.stop = stop;
            this.step = step;
        }
        
        public PyIterator iterator() {
            return new PyRangeIterator(this);
        }
        
        public int getStart() { return start; }
        public int getStop() { return stop; }
        public int getStep() { return step; }
        
        public String __str__() {
            return "range(" + start + ", " + stop + ", " + step + ")";
        }
    }
    
    // Iteradores Python
    public static interface PyIterator {
        Object next() throws Exception;
        boolean hasNext();
    }
    
    public static class PyListIterator implements PyIterator {
        private PyList list;
        private int index = 0;
        
        public PyListIterator(PyList list) {
            this.list = list;
        }
        
        public boolean hasNext() {
            return index < list.size();
        }
        
        public Object next() throws Exception {
            if (!hasNext()) return null;
            return list.get(index++);
        }
    }
    
    public static class PyStringIterator implements PyIterator {
        private String str;
        private int index = 0;
        
        public PyStringIterator(String str) {
            this.str = str;
        }
        
        public boolean hasNext() {
            return index < str.length();
        }
        
        public Object next() throws Exception {
            if (!hasNext()) return null;
            return String.valueOf(str.charAt(index++));
        }
    }
    
    public static class PyRangeIterator implements PyIterator {
        private PyRange range;
        private int current;
        
        public PyRangeIterator(PyRange range) {
            this.range = range;
            this.current = range.getStart();
        }
        
        public boolean hasNext() {
            if (range.getStep() > 0) {
                return current < range.getStop();
            } else {
                return current > range.getStop();
            }
        }
        
        public Object next() throws Exception {
            if (!hasNext()) return null;
            int value = current;
            current += range.getStep();
            return new Double(value);
        }
    }
    
    // Método vinculado Python
    public static class PyBoundMethod {
        private PyObject instance;
        private PyFunction function;
        
        public PyBoundMethod(PyObject instance, PyFunction function) {
            this.instance = instance;
            this.function = function;
        }
        
        public Object call(Vector args) throws Exception {
            Vector fullArgs = new Vector();
            fullArgs.addElement(instance);
            for (int i = 0; i < args.size(); i++) {
                fullArgs.addElement(args.elementAt(i));
            }
            return function.call(fullArgs);
        }
    }
    
    // Função Python
    public static class PyFunction {
        private Vector params, defaults, bodyTokens;
        private Hashtable closureScope;
        private Object exprResult; // Para lambdas
        
        public PyFunction(Vector params, Vector defaults, Vector bodyTokens, Hashtable closureScope) {
            this.params = params;
            this.defaults = defaults;
            this.bodyTokens = bodyTokens;
            this.closureScope = closureScope;
            this.exprResult = null;
        }
        
        public PyFunction(Vector params, Vector defaults, Object exprResult, Hashtable closureScope) {
            this.params = params;
            this.defaults = defaults;
            this.exprResult = exprResult;
            this.closureScope = closureScope;
            this.bodyTokens = null;
        }
        
        public Object call(Vector args) throws Exception {
            // Criar escopo local
            Hashtable localScope = new Hashtable();
            
            // Herdar do closure
            Enumeration keys = closureScope.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                localScope.put(key, closureScope.get(key));
            }
            
            // Passar argumentos
            int defaultStart = params.size() - defaults.size();
            
            for (int i = 0; i < params.size(); i++) {
                String param = (String) params.elementAt(i);
                Object value = null;
                
                if (i < args.size()) {
                    value = args.elementAt(i);
                } else if (i >= defaultStart) {
                    value = defaults.elementAt(i - defaultStart);
                } else {
                    throw new Exception("Missing argument: " + param);
                }
                
                localScope.put(param, value);
            }
            
            // Se for lambda, retornar o resultado da expressão
            if (exprResult != null) {
                // Avaliar expressão no escopo local
                Python dummy = new Python(null, 0, null, null, null, null);
                return dummy.evaluateExpression(exprResult, localScope);
            }
            
            // Executar o corpo da função
            Python subPython = new Python(null, 0, null, null, null, null);
            subPython.tokens = bodyTokens;
            subPython.tokenIndex = 0;
            subPython.globals = localScope;
            
            Object result = null;
            while (subPython.peek().type != EOF) {
                Object stmt = subPython.statement(localScope);
                if (subPython.doreturn) {
                    result = stmt;
                    subPython.doreturn = false;
                    break;
                }
            }
            
            return result == null ? PY_NONE : result;
        }
    }
    
    // Função built-in Python
    public static class PyBuiltinFunction {
        private int type;
        
        public PyBuiltinFunction(int type) {
            this.type = type;
        }
        
        public Object call(Vector args) throws Exception {
            switch (type) {
                case PY_PRINT:
                    print(args);
                    return null;
                case PY_LEN:
                    return len(args);
                case PY_RANGE:
                    return range(args);
                case PY_LIST:
                    return list(args);
                case PY_DICT:
                    return dict(args);
                case PY_STR:
                    return str(args);
                case PY_INT:
                    return _int(args);
                case PY_FLOAT:
                    return _float(args);
                case PY_BOOL:
                    return bool(args);
                case PY_TYPE:
                    return _type(args);
                case PY_ISINSTANCE:
                    return isinstance(args);
                case PY_HASATTR:
                    return hasattr(args);
                case PY_GETATTR:
                    return getattr(args);
                case PY_SETATTR:
                    return setattr(args);
                case PY_DIR:
                    return dir(args);
                case PY_SUM:
                    return sum(args);
                case PY_MAX:
                    return max(args);
                case PY_MIN:
                    return min(args);
                case PY_ABS:
                    return abs(args);
                case PY_ROUND:
                    return round(args);
                case PY_POW:
                    return pow(args);
                case PY_SORTED:
                    return sorted(args);
                case PY_ENUMERATE:
                    return enumerate(args);
                case PY_ZIP:
                    return zip(args);
                case PY_MAP:
                    return map(args);
                case PY_FILTER:
                    return filter(args);
                case PY_ANY:
                    return any(args);
                case PY_ALL:
                    return all(args);
                case PY_CHR:
                    return chr(args);
                case PY_ORD:
                    return ord(args);
                case PY_HEX:
                    return hex(args);
                case PY_OCT:
                    return oct(args);
                case PY_BIN:
                    return bin(args);
                case PY_FORMAT:
                    return format(args);
            }
            return null;
        }
        
        // Implementações built-in
        private void print(Vector args) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < args.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(stringify(args.elementAt(i)));
            }
            System.out.println(sb.toString());
        }
        
        private Object len(Vector args) throws Exception {
            checkArgs("len", args, 1);
            Object obj = args.elementAt(0);
            
            if (obj instanceof String) {
                return new Double(((String) obj).length());
            }
            if (obj instanceof PyList) {
                return new Double(((PyList) obj).size());
            }
            if (obj instanceof PyDict) {
                return new Double(((PyDict) obj).size());
            }
            if (obj instanceof PyRange) {
                PyRange r = (PyRange) obj;
                int len = (r.getStop() - r.getStart()) / r.getStep();
                return new Double(len);
            }
            throw new Exception("object of type '" + getTypeName(obj) + "' has no len()");
        }
        
        private Object range(Vector args) throws Exception {
            if (args.size() == 1) {
                int stop = ((Double) args.elementAt(0)).intValue();
                return new PyRange(stop);
            } else if (args.size() == 2) {
                int start = ((Double) args.elementAt(0)).intValue();
                int stop = ((Double) args.elementAt(1)).intValue();
                return new PyRange(start, stop);
            } else if (args.size() == 3) {
                int start = ((Double) args.elementAt(0)).intValue();
                int stop = ((Double) args.elementAt(1)).intValue();
                int step = ((Double) args.elementAt(2)).intValue();
                return new PyRange(start, stop, step);
            }
            throw new Exception("range expected 1-3 arguments, got " + args.size());
        }
        
        private Object list(Vector args) throws Exception {
            PyList lst = new PyList();
            
            if (args.size() > 0) {
                Object iterable = args.elementAt(0);
                PyIterator it = getIterator(iterable);
                Object value;
                while ((value = nextValue(it)) != null) {
                    lst.append(value);
                }
            }
            
            return lst;
        }
        
        private Object dict(Vector args) throws Exception {
            PyDict d = new PyDict();
            
            if (args.size() > 0) {
                Object iterable = args.elementAt(0);
                PyIterator it = getIterator(iterable);
                Object pair;
                while ((pair = nextValue(it)) != null) {
                    if (pair instanceof PyList && ((PyList) pair).size() == 2) {
                        PyList p = (PyList) pair;
                        d.put(p.get(0), p.get(1));
                    }
                }
            }
            
            return d;
        }
        
        private Object str(Vector args) throws Exception {
            checkArgs("str", args, 1);
            return stringify(args.elementAt(0));
        }
        
        private Object _int(Vector args) throws Exception {
            checkArgs("int", args, 1);
            Object obj = args.elementAt(0);
            
            if (obj instanceof Double) {
                return new Double(((Double) obj).intValue());
            }
            if (obj instanceof String) {
                try {
                    return new Double(Integer.parseInt((String) obj));
                } catch (NumberFormatException e) {
                    throw new Exception("invalid literal for int(): " + obj);
                }
            }
            throw new Exception("int() argument must be a string or a number");
        }
        
        private Object _float(Vector args) throws Exception {
            checkArgs("float", args, 1);
            Object obj = args.elementAt(0);
            
            if (obj instanceof Double) {
                return obj;
            }
            if (obj instanceof String) {
                try {
                    return new Double(Double.parseDouble((String) obj));
                } catch (NumberFormatException e) {
                    throw new Exception("invalid literal for float(): " + obj);
                }
            }
            throw new Exception("float() argument must be a string or a number");
        }
        
        private Object bool(Vector args) throws Exception {
            checkArgs("bool", args, 1);
            return isTruthy(args.elementAt(0)) ? TRUE : FALSE;
        }
        
        private Object _type(Vector args) throws Exception {
            checkArgs("type", args, 1);
            Object obj = args.elementAt(0);
            
            if (obj instanceof PyObject) {
                return ((PyObject) obj).__class__;
            }
            if (obj instanceof PyClass) {
                return obj;
            }
            
            // Criar classe dinâmica para tipos built-in
            Hashtable dict = new Hashtable();
            dict.put("__name__", getTypeName(obj));
            return new PyClass(getTypeName(obj), new Vector(), dict);
        }
        
        private Object isinstance(Vector args) throws Exception {
            checkArgs("isinstance", args, 2);
            Object obj = args.elementAt(0);
            Object classinfo = args.elementAt(1);
            
            if (classinfo instanceof PyClass) {
                return isInstance(obj, (PyClass) classinfo) ? TRUE : FALSE;
            }
            if (classinfo instanceof PyList) {
                PyList classes = (PyList) classinfo;
                for (int i = 0; i < classes.size(); i++) {
                    if (classes.get(i) instanceof PyClass && isInstance(obj, (PyClass) classes.get(i))) {
                        return TRUE;
                    }
                }
                return FALSE;
            }
            throw new Exception("isinstance() arg 2 must be a type or tuple of types");
        }
        
        private boolean isInstance(Object obj, PyClass cls) {
            if (obj instanceof PyObject) {
                PyObject pyobj = (PyObject) obj;
                PyClass objClass = pyobj.__class__;
                
                while (objClass != null) {
                    if (objClass == cls) return true;
                    if (objClass.__bases__ != null) {
                        for (int i = 0; i < objClass.__bases__.size(); i++) {
                            if (isInstanceOfClass(objClass.__bases__.elementAt(i), cls)) return true;
                        }
                    }
                    objClass = null;
                }
            }
            return false;
        }
        
        private boolean isInstanceOfClass(Object obj, PyClass cls) {
            if (obj instanceof PyClass) {
                PyClass c = (PyClass) obj;
                if (c == cls) return true;
                for (int i = 0; i < c.__bases__.size(); i++) {
                    if (isInstanceOfClass(c.__bases__.elementAt(i), cls)) return true;
                }
            }
            return false;
        }
        
        private Object hasattr(Vector args) throws Exception {
            checkArgs("hasattr", args, 2);
            Object obj = args.elementAt(0);
            String name = args.elementAt(1).toString();
            
            return (getAttribute(obj, name) != null) ? TRUE : FALSE;
        }
        
        private Object getattr(Vector args) throws Exception {
            checkArgs("getattr", args, 2);
            Object obj = args.elementAt(0);
            String name = args.elementAt(1).toString();
            
            Object value = getAttribute(obj, name);
            if (value == null && args.size() > 2) {
                return args.elementAt(2);
            }
            if (value == null) {
                throw new Exception("'" + getTypeName(obj) + "' object has no attribute '" + name + "'");
            }
            return value;
        }
        
        private Object setattr(Vector args) throws Exception {
            checkArgs("setattr", args, 3);
            Object obj = args.elementAt(0);
            String name = args.elementAt(1).toString();
            Object value = args.elementAt(2);
            
            setAttribute(obj, name, value);
            return null;
        }
        
        private Object dir(Vector args) throws Exception {
            Object obj = args.size() > 0 ? args.elementAt(0) : null;
            PyList names = new PyList();
            
            if (obj == null) {
                // Dir do escopo atual
                names.append("__builtins__");
                names.append("__name__");
                names.append("__doc__");
            } else if (obj instanceof Hashtable) {
                Hashtable dict = (Hashtable) obj;
                Enumeration keys = dict.keys();
                while (keys.hasMoreElements()) {
                    names.append(keys.nextElement());
                }
            } else if (obj instanceof PyObject) {
                Hashtable dict = ((PyObject) obj).__dict__;
                Enumeration keys = dict.keys();
                while (keys.hasMoreElements()) {
                    names.append(keys.nextElement());
                }
            }
            
            return names;
        }
        
        private Object sum(Vector args) throws Exception {
            checkArgs("sum", args, 1);
            Object iterable = args.elementAt(0);
            double total = 0;
            
            PyIterator it = getIterator(iterable);
            Object value;
            while ((value = nextValue(it)) != null) {
                if (value instanceof Double) {
                    total += ((Double) value).doubleValue();
                } else {
                    throw new Exception("unsupported operand type for sum(): " + getTypeName(value));
                }
            }
            
            return new Double(total);
        }
        
        private Object max(Vector args) throws Exception {
            checkArgs("max", args, 1);
            Object iterable = args.elementAt(0);
            Object maxValue = null;
            
            PyIterator it = getIterator(iterable);
            Object value;
            while ((value = nextValue(it)) != null) {
                if (maxValue == null || compare(value, maxValue) > 0) {
                    maxValue = value;
                }
            }
            
            return maxValue;
        }
        
        private Object min(Vector args) throws Exception {
            checkArgs("min", args, 1);
            Object iterable = args.elementAt(0);
            Object minValue = null;
            
            PyIterator it = getIterator(iterable);
            Object value;
            while ((value = nextValue(it)) != null) {
                if (minValue == null || compare(value, minValue) < 0) {
                    minValue = value;
                }
            }
            
            return minValue;
        }
        
        private Object abs(Vector args) throws Exception {
            checkArgs("abs", args, 1);
            Object x = args.elementAt(0);
            
            if (x instanceof Double) {
                return new Double(Math.abs(((Double) x).doubleValue()));
            }
            throw new Exception("bad operand type for abs(): " + getTypeName(x));
        }
        
        private Object round(Vector args) throws Exception {
            checkArgs("round", args, 1);
            double num = ((Double) args.elementAt(0)).doubleValue();
            int ndigits = args.size() > 1 ? ((Double) args.elementAt(1)).intValue() : 0;
            
            double factor = Math.pow(10, ndigits);
            double rounded = Math.round(num * factor) / factor;
            return new Double(rounded);
        }
        
        private Object pow(Vector args) throws Exception {
            checkArgs("pow", args, 2);
            double base = ((Double) args.elementAt(0)).doubleValue();
            double exp = ((Double) args.elementAt(1)).doubleValue();
            
            return new Double(Math.pow(base, exp));
        }
        
        private Object sorted(Vector args) throws Exception {
            checkArgs("sorted", args, 1);
            Object iterable = args.elementAt(0);
            PyList lst = new PyList();
            
            PyIterator it = getIterator(iterable);
            Object value;
            while ((value = nextValue(it)) != null) {
                lst.append(value);
            }
            
            lst.sort();
            return lst;
        }
        
        private Object enumerate(Vector args) throws Exception {
            checkArgs("enumerate", args, 1);
            Object iterable = args.elementAt(0);
            PyList enumerated = new PyList();
            int index = 0;
            
            PyIterator it = getIterator(iterable);
            Object value;
            while ((value = nextValue(it)) != null) {
                PyList pair = new PyList();
                pair.append(new Double(index++));
                pair.append(value);
                enumerated.append(pair);
            }
            
            return enumerated;
        }
        
        private Object zip(Vector args) throws Exception {
            PyList zipped = new PyList();
            
            if (args.size() == 0) return zipped;
            
            // Obter iteradores
            PyIterator[] iterators = new PyIterator[args.size()];
            for (int i = 0; i < args.size(); i++) {
                iterators[i] = getIterator(args.elementAt(i));
            }
            
            // Zip
            boolean hasMore = true;
            while (hasMore) {
                PyList tuple = new PyList();
                hasMore = false;
                
                for (int i = 0; i < iterators.length; i++) {
                    Object value = nextValue(iterators[i]);
                    if (value != null) {
                        tuple.append(value);
                        hasMore = true;
                    }
                }
                
                if (hasMore) {
                    zipped.append(tuple);
                }
            }
            
            return zipped;
        }
        
        private Object map(Vector args) throws Exception {
            if (args.size() < 2) {
                throw new Exception("map() requires at least two arguments");
            }
            
            Object func = args.elementAt(0);
            if (!(func instanceof PyFunction)) {
                throw new Exception("map() arg 1 must be a function");
            }
            
            PyFunction function = (PyFunction) func;
            PyList result = new PyList();
            
            // Obter iteradores
            PyIterator[] iterators = new PyIterator[args.size() - 1];
            for (int i = 1; i < args.size(); i++) {
                iterators[i - 1] = getIterator(args.elementAt(i));
            }
            
            // Mapear
            boolean hasMore = true;
            while (hasMore) {
                PyList callArgs = new PyList();
                hasMore = false;
                
                for (int i = 0; i < iterators.length; i++) {
                    Object value = nextValue(iterators[i]);
                    if (value != null) {
                        callArgs.append(value);
                        hasMore = true;
                    }
                }
                
                if (hasMore) {
                    Vector callVec = new Vector();
                    for (int i = 0; i < callArgs.size(); i++) {
                        callVec.addElement(callArgs.get(i));
                    }
                    result.append(function.call(callVec));
                }
            }
            
            return result;
        }
        
        private Object filter(Vector args) throws Exception {
            if (args.size() < 2) {
                throw new Exception("filter() requires at least two arguments");
            }
            
            Object func = args.elementAt(0);
            if (!(func instanceof PyFunction)) {
                throw new Exception("filter() arg 1 must be a function");
            }
            
            PyFunction function = (PyFunction) func;
            PyList result = new PyList();
            
            PyIterator it = getIterator(args.elementAt(1));
            Object value;
            while ((value = nextValue(it)) != null) {
                Vector callArgs = new Vector();
                callArgs.addElement(value);
                Object testResult = function.call(callArgs);
                if (isTruthy(testResult)) {
                    result.append(value);
                }
            }
            
            return result;
        }
        
        private Object any(Vector args) throws Exception {
            checkArgs("any", args, 1);
            PyIterator it = getIterator(args.elementAt(0));
            Object value;
            while ((value = nextValue(it)) != null) {
                if (isTruthy(value)) return TRUE;
            }
            return FALSE;
        }
        
        private Object all(Vector args) throws Exception {
            checkArgs("all", args, 1);
            PyIterator it = getIterator(args.elementAt(0));
            Object value;
            while ((value = nextValue(it)) != null) {
                if (!isTruthy(value)) return FALSE;
            }
            return TRUE;
        }
        
        private Object chr(Vector args) throws Exception {
            checkArgs("chr", args, 1);
            int code = ((Double) args.elementAt(0)).intValue();
            if (code < 0 || code > 0x10FFFF) {
                throw new Exception("chr() arg not in range(0x110000)");
            }
            return String.valueOf((char) code);
        }
        
        private Object ord(Vector args) throws Exception {
            checkArgs("ord", args, 1);
            String s = args.elementAt(0).toString();
            if (s.length() != 1) {
                throw new Exception("ord() expected a character, but string of length " + s.length() + " found");
            }
            return new Double((int) s.charAt(0));
        }
        
        private Object hex(Vector args) throws Exception {
            checkArgs("hex", args, 1);
            int num = ((Double) args.elementAt(0)).intValue();
            return "0x" + Integer.toHexString(num);
        }
        
        private Object oct(Vector args) throws Exception {
            checkArgs("oct", args, 1);
            int num = ((Double) args.elementAt(0)).intValue();
            return "0o" + Integer.toOctalString(num);
        }
        
        private Object bin(Vector args) throws Exception {
            checkArgs("bin", args, 1);
            int num = ((Double) args.elementAt(0)).intValue();
            return "0b" + Integer.toBinaryString(num);
        }
        
        private Object format(Vector args) throws Exception {
            checkArgs("format", args, 2);
            String value = stringify(args.elementAt(0));
            String spec = args.elementAt(1).toString();
            // Formatação simples
            if (spec.equals(">10")) {
                return String.format("%10s", value);
            }
            if (spec.equals("<10")) {
                return String.format("%-10s", value);
            }
            return value;
        }
        
        private void checkArgs(String name, Vector args, int count) throws Exception {
            if (args.size() < count) {
                throw new Exception(name + " expected at least " + count + " arguments, got " + args.size());
            }
        }
        
        private String stringify(Object obj) {
            if (obj == null || obj == PY_NONE) return "None";
            if (obj instanceof String) return (String) obj;
            if (obj instanceof Double) {
                double d = ((Double) obj).doubleValue();
                if (d == (long) d) return String.valueOf((long) d);
                return String.valueOf(d);
            }
            if (obj instanceof Boolean) return ((Boolean) obj) ? "True" : "False";
            if (obj instanceof PyObject) return ((PyObject) obj).__str__();
            if (obj instanceof PyList) return ((PyList) obj).__str__();
            if (obj instanceof PyDict) return ((PyDict) obj).__str__();
            return obj.toString();
        }
    }
    
    // Meta-classes para tipos built-in
    public static class PyStringMeta {
        public PyFunction upper = new PyFunction(new int[]{STR_UPPER});
        public PyFunction lower = new PyFunction(new int[]{STR_LOWER});
        public PyFunction strip = new PyFunction(new int[]{STR_STRIP});
        public PyFunction split = new PyFunction(new int[]{STR_SPLIT});
        public PyFunction join = new PyFunction(new int[]{STR_JOIN});
        public PyFunction replace = new PyFunction(new int[]{STR_REPLACE});
        public PyFunction find = new PyFunction(new int[]{STR_FIND});
        public PyFunction startswith = new PyFunction(new int[]{STR_STARTSWITH});
        public PyFunction endswith = new PyFunction(new int[]{STR_ENDSWITH});
    }
    
    public static class PyListMeta {
        public PyFunction append = new PyFunction(new int[]{LIST_APPEND});
        public PyFunction extend = new PyFunction(new int[]{LIST_EXTEND});
        public PyFunction insert = new PyFunction(new int[]{LIST_INSERT});
        public PyFunction remove = new PyFunction(new int[]{LIST_REMOVE});
        public PyFunction pop = new PyFunction(new int[]{LIST_POP});
        public PyFunction index = new PyFunction(new int[]{LIST_INDEX});
        public PyFunction count = new PyFunction(new int[]{LIST_COUNT});
        public PyFunction sort = new PyFunction(new int[]{LIST_SORT});
        public PyFunction reverse = new PyFunction(new int[]{LIST_REVERSE});
        public PyFunction clear = new PyFunction(new int[]{LIST_CLEAR});
        public PyFunction copy = new PyFunction(new int[]{LIST_COPY});
    }
    
    public static class PyDictMeta {
        public PyFunction keys = new PyFunction(new int[]{DICT_KEYS});
        public PyFunction values = new PyFunction(new int[]{DICT_VALUES});
        public PyFunction items = new PyFunction(new int[]{DICT_ITEMS});
        public PyFunction update = new PyFunction(new int[]{DICT_UPDATE});
        public PyFunction get = new PyFunction(new int[]{DICT_GET});
        public PyFunction pop = new PyFunction(new int[]{DICT_POP});
        public PyFunction clear = new PyFunction(new int[]{DICT_CLEAR});
        public PyFunction copy = new PyFunction(new int[]{DICT_COPY});
    }
    
    public static class PySetMeta {
        public PyFunction add = new PyFunction(new int[]{SET_ADD});
        public PyFunction remove = new PyFunction(new int[]{SET_REMOVE});
        public PyFunction discard = new PyFunction(new int[]{SET_DISCARD});
        public PyFunction union = new PyFunction(new int[]{SET_UNION});
        public PyFunction intersection = new PyFunction(new int[]{SET_INTERSECTION});
    }
    
    public static class PyObjectMeta {
        public PyFunction __init__ = null;
    }
    
    public static class PyTypeMeta {
        public PyFunction __call__ = null;
    }
    
    // Helper methods para o evaluator
    private Object evaluateExpression(Object expr, Hashtable scope) throws Exception {
        if (expr instanceof PyFunction) {
            return expr;
        }
        // Para lambdas simples, expr é o resultado direto
        return expr;
    }
    
    // ==================== UTILIDADES ====================
    
    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private static boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }
    
    private static boolean isLetterOrDigit(char c) {
        return isLetter(c) || isDigit(c);
    }
    
    // Placeholder para PyAttribute
    public static class PyAttribute {
        public Object obj;
        public String name;
        
        public PyAttribute(Object obj, String name) {
            this.obj = obj;
            this.name = name;
        }
    }
    
    // Placeholder para PyFunction com tipo built-in
    public static class PyBuiltinMethod {
        private int type;
        
        public PyBuiltinMethod(int type) {
            this.type = type;
        }
        
        public Object call(Object self, Vector args) throws Exception {
            // Implementar métodos de string, list, dict etc
            return null;
        }
    }
}
