import javax.microedition.io.*;
import java.util.*;
import java.io.*;
// |
// C2ME Runtime
public class C2ME {
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int uid = 1000, tokenIndex, status = 0;
    public Hashtable globals = new Hashtable(), father;
    public Vector tokens;
    private boolean kill = false;
    // |
    public int status = 0;
    // |
    public static final int EOF = 0;
    public static class CToken { int type; Object value; CToken(int type, Object value) { this.type = type; this.value = value; } public String toString() { return "CToken(type=" + type + ", value=" + value + ")"; } }
    
    public C2ME(OpenTTY midlet, int uid, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; 
        this.uid = uid; 
        this.PID = pid; 
        this.proc = proc; 
        this.stdout = stdout; 
        this.father = scope;
    }
    public Hashtable run(String source, String code, Hashtable args) {

    }

    public Vector tokenize(String code) throws Exception {
        Vector tokens = new Vector();
        int i = 0;
        if (code.startsWith("#!")) {
            while (i < code.length() && code.charAt(i) != '\n') { i++; }
            if (i < code.length() && code.charAt(i) == '\n') { i++; }
        }
        while (i < code.length()) {
            char c = code.charAt(i);

            if (isWhitespace(c)) { i++; }
            else if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '/') {
                i += 2;
                while (i < code.length() && code.charAt(i) != '\n') i++;
            }
            else if (c == '/' && i + 1 < code.length() && code.charAt(i + 1) == '*') {
                i += 2;
                while (i + 1 < code.length() && !(code.charAt(i) == '*' && code.charAt(i + 1) == '/')) i++;
                i += 2;
            }
            else if (c == '"') {
                StringBuffer sb = new StringBuffer(); i++;
                while (i < code.length() && code.charAt(i) != '"') {
                    if (code.charAt(i) == '\\' && i + 1 < code.length()) { sb.append(code.charAt(i)); i++; }
                    sb.append(code.charAt(i)); i++;
                }
                if (i < code.length() && code.charAt(i) == '"') i++;
                tokens.addElement(new CToken(STRING, sb.toString()));
            }
            else if (c == '\'') {
                StringBuffer sb = new StringBuffer(); i++;
                while (i < code.length() && code.charAt(i) != '\'') {
                    if (code.charAt(i) == '\\' && i + 1 < code.length()) { sb.append(code.charAt(i)); i++; }
                    sb.append(code.charAt(i)); i++;
                }
                if (i < code.length() && code.charAt(i) == '\'') i++;
                tokens.addElement(new CToken(CHAR, sb.toString()));
            }
            else if (isDigit(c) || (c == '.' && i + 1 < code.length() && isDigit(code.charAt(i + 1)))) {
                StringBuffer sb = new StringBuffer();
                boolean hasDecimal = false, hasExponent = false;
                while (i < code.length()) {
                    c = code.charAt(i);
                    if (isDigit(c)) { sb.append(c); i++; }
                    else if (c == '.' && !hasDecimal && !hasExponent) { sb.append(c); hasDecimal = true; i++; }
                    else if ((c == 'e' || c == 'E') && !hasExponent && i + 1 < code.length()) {
                        sb.append(c); hasExponent = true; i++;
                        if (i < code.length() && (code.charAt(i) == '+' || code.charAt(i) == '-')) { sb.append(code.charAt(i)); i++; }
                    }
                    else if ((c == 'f' || c == 'F' || c == 'l' || c == 'L') && i + 1 < code.length() && !isLetterOrDigit(code.charAt(i + 1))) {
                        sb.append(c); i++; break;
                    }
                    else break;
                }
                try {
                    String numStr = sb.toString();
                    if (numStr.indexOf('.') != -1 || numStr.indexOf('e') != -1 || numStr.indexOf('E') != -1)
                        tokens.addElement(new CToken(NUMBER, new Double(Double.parseDouble(numStr))));
                    else tokens.addElement(new CToken(NUMBER, new Integer(Integer.parseInt(numStr))));
                } catch (NumberFormatException e) { throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); }
                continue;
            }
            else if (isLetter(c) || c == '_') {
                StringBuffer sb = new StringBuffer();
                while (i < code.length() && (isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) { sb.append(code.charAt(i)); i++; }
                String word = sb.toString();
                int type = isKeyword(word);
                tokens.addElement(new CToken(type != -1 ? type : IDENTIFIER, word));
            }
            else if (c == '+' && i + 1 < code.length() && code.charAt(i + 1) == '+') { tokens.addElement(new CToken(INCREMENT, "++")); i += 2; }
            else if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '-') { tokens.addElement(new CToken(DECREMENT, "--")); i += 2; }
            else if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '>') { tokens.addElement(new CToken(ARROW, "->")); i += 2; }
            else if (c == '=' && i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new CToken(EQ, "==")); i += 2; }
            else if (c == '!' && i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new CToken(NE, "!=")); i += 2; }
            else if (c == '<' && i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new CToken(LE, "<=")); i += 2; }
            else if (c == '>' && i + 1 < code.length() && code.charAt(i + 1) == '=') { tokens.addElement(new CToken(GE, ">=")); i += 2; }
            else if (c == '<' && i + 1 < code.length() && code.charAt(i + 1) == '<') { tokens.addElement(new CToken(LSHIFT, "<<")); i += 2; }
            else if (c == '>' && i + 1 < code.length() && code.charAt(i + 1) == '>') { tokens.addElement(new CToken(RSHIFT, ">>")); i += 2; }
            else if (c == '&' && i + 1 < code.length() && code.charAt(i + 1) == '&') { tokens.addElement(new CToken(AND, "&&")); i += 2; }
            else if (c == '|' && i + 1 < code.length() && code.charAt(i + 1) == '|') { tokens.addElement(new CToken(OR, "||")); i += 2; }
            else if (c == '.' && i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') { tokens.addElement(new CToken(ELLIPSIS, "...")); i += 3; }
            else if (c == '#') { tokens.addElement(new CToken(PREPROCESSOR, "#")); i++; }
            else if (c == '+') { tokens.addElement(new CToken(PLUS, "+")); i++; }
            else if (c == '-') { tokens.addElement(new CToken(MINUS, "-")); i++; }
            else if (c == '*') { tokens.addElement(new CToken(MULTIPLY, "*")); i++; }
            else if (c == '/') { tokens.addElement(new CToken(DIVIDE, "/")); i++; }
            else if (c == '%') { tokens.addElement(new CToken(MODULO, "%")); i++; }
            else if (c == '=') { tokens.addElement(new CToken(ASSIGN, "=")); i++; }
            else if (c == '!') { tokens.addElement(new CToken(NOT, "!")); i++; }
            else if (c == '<') { tokens.addElement(new CToken(LT, "<")); i++; }
            else if (c == '>') { tokens.addElement(new CToken(GT, ">")); i++; }
            else if (c == '&') { tokens.addElement(new CToken(BITAND, "&")); i++; }
            else if (c == '|') { tokens.addElement(new CToken(BITOR, "|")); i++; }
            else if (c == '^') { tokens.addElement(new CToken(BITXOR, "^")); i++; }
            else if (c == '~') { tokens.addElement(new CToken(BITNOT, "~")); i++; }
            else if (c == '(') { tokens.addElement(new CToken(LPAREN, "(")); i++; }
            else if (c == ')') { tokens.addElement(new CToken(RPAREN, ")")); i++; }
            else if (c == '{') { tokens.addElement(new CToken(LBRACE, "{")); i++; }
            else if (c == '}') { tokens.addElement(new CToken(RBRACE, "}")); i++; }
            else if (c == '[') { tokens.addElement(new CToken(LBRACKET, "[")); i++; }
            else if (c == ']') { tokens.addElement(new CToken(RBRACKET, "]")); i++; }
            else if (c == ';') { tokens.addElement(new CToken(SEMICOLON, ";")); i++; }
            else if (c == ',') { tokens.addElement(new CToken(COMMA, ",")); i++; }
            else if (c == '.') { tokens.addElement(new CToken(DOT, ".")); i++; }
            else if (c == ':') { tokens.addElement(new CToken(COLON, ":")); i++; }
            else if (c == '?') { tokens.addElement(new CToken(QUESTION, "?")); i++; }
            else { throw new Exception("Unexpected character '" + c + "'"); }
        }

        tokens.addElement(new CToken(EOF, "EOF"));
        if (midlet.useCache) { if (midlet.cacheLua.size() > 100) { midlet.cacheLua.clear(); } midlet.cacheLua.put(code, tokens); }
        return tokens;
    }

    private int isKeyword(String word) {
        if (word.equals("auto")) return AUTO;
        if (word.equals("break")) return BREAK;
        if (word.equals("case")) return CASE;
        if (word.equals("char")) return CHAR_KEY;
        if (word.equals("const")) return CONST;
        if (word.equals("continue")) return CONTINUE;
        if (word.equals("default")) return DEFAULT;
        if (word.equals("do")) return DO;
        if (word.equals("double")) return DOUBLE;
        if (word.equals("else")) return ELSE;
        if (word.equals("enum")) return ENUM;
        if (word.equals("extern")) return EXTERN;
        if (word.equals("float")) return FLOAT;
        if (word.equals("for")) return FOR;
        if (word.equals("goto")) return GOTO;
        if (word.equals("if")) return IF;
        if (word.equals("int")) return INT;
        if (word.equals("long")) return LONG;
        if (word.equals("register")) return REGISTER;
        if (word.equals("return")) return RETURN;
        if (word.equals("short")) return SHORT;
        if (word.equals("signed")) return SIGNED;
        if (word.equals("sizeof")) return SIZEOF;
        if (word.equals("static")) return STATIC;
        if (word.equals("struct")) return STRUCT;
        if (word.equals("switch")) return SWITCH;
        if (word.equals("typedef")) return TYPEDEF;
        if (word.equals("union")) return UNION;
        if (word.equals("unsigned")) return UNSIGNED;
        if (word.equals("void")) return VOID;
        if (word.equals("volatile")) return VOLATILE;
        if (word.equals("while")) return WHILE;
        return -1;
    }

    private boolean isWhitespace(char c) { return c == ' ' || c == '\t' || c == '\n' || c == '\r'; }
    private boolean isLetter(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    private boolean isLetterOrDigit(char c) { return isLetter(c) || isDigit(c); }

    // C token type constants
    public static final int EOF = 0, IDENTIFIER = 1, NUMBER = 2, STRING = 3, CHAR = 4;
    public static final int AUTO = 10, BREAK = 11, CASE = 12, CHAR_KEY = 13, CONST = 14, CONTINUE = 15;
    public static final int DEFAULT = 16, DO = 17, DOUBLE = 18, ELSE = 19, ENUM = 20, EXTERN = 21;
    public static final int FLOAT = 22, FOR = 23, GOTO = 24, IF = 25, INT = 26, LONG = 27;
    public static final int REGISTER = 28, RETURN = 29, SHORT = 30, SIGNED = 31, SIZEOF = 32, STATIC = 33;
    public static final int STRUCT = 34, SWITCH = 35, TYPEDEF = 36, UNION = 37, UNSIGNED = 38, VOID = 39, VOLATILE = 40, WHILE = 41;
    public static final int PLUS = 50, MINUS = 51, MULTIPLY = 52, DIVIDE = 53, MODULO = 54, ASSIGN = 55;
    public static final int EQ = 56, NE = 57, LT = 58, GT = 59, LE = 60, GE = 61, AND = 62, OR = 63, NOT = 64;
    public static final int BITAND = 65, BITOR = 66, BITXOR = 67, BITNOT = 68, LSHIFT = 69, RSHIFT = 70;
    public static final int INCREMENT = 71, DECREMENT = 72, ARROW = 73;
    public static final int LPAREN = 80, RPAREN = 81, LBRACE = 82, RBRACE = 83, LBRACKET = 84, RBRACKET = 85;
    public static final int SEMICOLON = 86, COMMA = 87, DOT = 88, COLON = 89, QUESTION = 90, ELLIPSIS = 91;
    public static final int PREPROCESSOR = 100;



}
// |
// End