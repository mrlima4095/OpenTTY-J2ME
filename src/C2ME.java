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

import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.media.control.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;

public class C2ME {
    public boolean breakLoop = false, doreturn = false, kill = true;
    public Process proc;
    private OpenTTY midlet;
    private Object stdout;
    public String PID = "";
    private long uptime = System.currentTimeMillis();
    private int id = 1000, tokenIndex, loopDepth = 0;
    public Hashtable globals = new Hashtable(), father, labels = new Hashtable();
    public Vector tokens;
    public int status = 0;
    
    // C token types
    public static final int TOKEN_EOF = 0, TOKEN_IDENTIFIER = 1, TOKEN_NUMBER = 2, TOKEN_STRING = 3, TOKEN_CHAR = 4;
    public static final int TOKEN_AUTO = 10, TOKEN_BREAK = 11, TOKEN_CASE = 12, TOKEN_CHAR_KEY = 13;
    public static final int TOKEN_CONST = 14, TOKEN_CONTINUE = 15, TOKEN_DEFAULT = 16, TOKEN_DO = 17;
    public static final int TOKEN_DOUBLE = 18, TOKEN_ELSE = 19, TOKEN_ENUM = 20, TOKEN_EXTERN = 21;
    public static final int TOKEN_FLOAT = 22, TOKEN_FOR = 23, TOKEN_GOTO = 24, TOKEN_IF = 25;
    public static final int TOKEN_INT = 26, TOKEN_LONG = 27, TOKEN_REGISTER = 28, TOKEN_RETURN = 29;
    public static final int TOKEN_SHORT = 30, TOKEN_SIGNED = 31, TOKEN_SIZEOF = 32, TOKEN_STATIC = 33;
    public static final int TOKEN_STRUCT = 34, TOKEN_SWITCH = 35, TOKEN_TYPEDEF = 36, TOKEN_UNION = 37;
    public static final int TOKEN_UNSIGNED = 38, TOKEN_VOID = 39, TOKEN_VOLATILE = 40, TOKEN_WHILE = 41;
    
    public static final int TOKEN_PLUS = 50, TOKEN_MINUS = 51, TOKEN_STAR = 52, TOKEN_SLASH = 53;
    public static final int TOKEN_PERCENT = 54, TOKEN_ASSIGN = 55, TOKEN_EQ = 56, TOKEN_NE = 57;
    public static final int TOKEN_LT = 58, TOKEN_GT = 59, TOKEN_LE = 60, TOKEN_GE = 61;
    public static final int TOKEN_AND = 62, TOKEN_OR = 63, TOKEN_NOT = 64, TOKEN_BITAND = 65;
    public static final int TOKEN_BITOR = 66, TOKEN_BITXOR = 67, TOKEN_BITNOT = 68;
    public static final int TOKEN_LSHIFT = 69, TOKEN_RSHIFT = 70, TOKEN_INCREMENT = 71;
    public static final int TOKEN_DECREMENT = 72, TOKEN_ARROW = 73;
    
    public static final int TOKEN_LPAREN = 80, TOKEN_RPAREN = 81, TOKEN_LBRACE = 82, TOKEN_RBRACE = 83;
    public static final int TOKEN_LBRACKET = 84, TOKEN_RBRACKET = 85, TOKEN_SEMICOLON = 86;
    public static final int TOKEN_COMMA = 87, TOKEN_DOT = 88, TOKEN_COLON = 89, TOKEN_QUESTION = 90;
    public static final int TOKEN_ELLIPSIS = 91;
    public static final int TOKEN_PREPROCESSOR = 100;
    
    public static class CToken { int type; Object value; CToken(int type, Object value) { this.type = type; this.value = value; } }
    
    public C2ME(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; this.id = id; this.PID = pid; this.proc = proc; this.stdout = stdout; this.father = scope;
        this.tokenIndex = 0;
        initializeGlobals();
    }
    
    private void initializeGlobals() {
        globals.put("NULL", null);
        globals.put("EOF", new Integer(-1));
    }
    
    public Hashtable run(String source, String code, Hashtable args) {
        midlet.sys.put(PID, proc);
        globals.put("arg", args);
        Hashtable ITEM = new Hashtable();
        
        try {
            this.tokens = tokenize(code);
            collectLabels();
            
            while (peek().type != TOKEN_EOF) {
                Object res = statement(globals);
                if (doreturn) {
                    if (res != null) { ITEM.put("object", res); }
                    doreturn = false;
                    break;
                }
            }
        } catch (Exception e) {
            midlet.print(midlet.getCatch(e), stdout, id, father);
            status = 1;
        } catch (Error e) {
            if (e.getMessage() != null) { midlet.print(e.getMessage(), stdout, id, father); }
            status = 1;
        }
        
        if (kill) { midlet.sys.remove(PID); }
        ITEM.put("status", status);
        return ITEM;
    }
    
    public Object statement(Hashtable scope) throws Exception {
        CToken current = peek();
        
        if (status != 0) { midlet.sys.remove(PID); throw new Error(); }
        if (!midlet.sys.containsKey(PID)) { throw new Error("Process killed"); }
        
        // Variable declaration or assignment
        if (isTypeSpecifier(current.type)) {
            return declaration(scope);
        }
        else if (current.type == TOKEN_IDENTIFIER) {
            String varName = (String) consume(TOKEN_IDENTIFIER).value;
            
            // Function call
            if (peek().type == TOKEN_LPAREN) {
                callFunction(varName, scope);
                consume(TOKEN_SEMICOLON);
                return null;
            }
            // Assignment
            else if (peek().type == TOKEN_ASSIGN) {
                consume(TOKEN_ASSIGN);
                Object value = expression(scope);
                scope.put(varName, value);
                consume(TOKEN_SEMICOLON);
                return null;
            }
            // Variable access
            else {
                consume(TOKEN_SEMICOLON);
                return unwrap(scope.get(varName));
            }
        }
        else if (current.type == TOKEN_IF) {
            return ifStatement(scope);
        }
        else if (current.type == TOKEN_WHILE) {
            return whileStatement(scope);
        }
        else if (current.type == TOKEN_DO) {
            return doWhileStatement(scope);
        }
        else if (current.type == TOKEN_FOR) {
            return forStatement(scope);
        }
        else if (current.type == TOKEN_SWITCH) {
            return switchStatement(scope);
        }
        else if (current.type == TOKEN_RETURN) {
            return returnStatement(scope);
        }
        else if (current.type == TOKEN_BREAK) {
            return breakStatement(scope);
        }
        else if (current.type == TOKEN_CONTINUE) {
            return continueStatement(scope);
        }
        else if (current.type == TOKEN_GOTO) {
            return gotoStatement(scope);
        }
        else if (current.type == TOKEN_LBRACE) {
            return blockStatement(scope);
        }
        else if (current.type == TOKEN_SEMICOLON) {
            consume(TOKEN_SEMICOLON);
            return null;
        }
        
        throw new Exception("Unexpected token: " + current.type);
    }
    
    private Object declaration(Hashtable scope) throws Exception {
        int typeSpec = peek().type;
        consume(typeSpec);
        
        Vector varNames = new Vector();
        Vector initializers = new Vector();
        
        // Read variable names and initializers
        do {
            String varName = (String) consume(TOKEN_IDENTIFIER).value;
            varNames.addElement(varName);
            
            if (peek().type == TOKEN_ASSIGN) {
                consume(TOKEN_ASSIGN);
                Object initValue = expression(scope);
                initializers.addElement(initValue);
            } else {
                initializers.addElement(getDefaultValue(typeSpec));
            }
            
            if (peek().type == TOKEN_COMMA) {
                consume(TOKEN_COMMA);
            } else {
                break;
            }
        } while (true);
        
        consume(TOKEN_SEMICOLON);
        
        // Declare variables in scope
        for (int i = 0; i < varNames.size(); i++) {
            String name = (String) varNames.elementAt(i);
            Object value = initializers.elementAt(i);
            scope.put(name, value);
        }
        
        return null;
    }
    
    private Object getDefaultValue(int typeSpec) {
        if (typeSpec == TOKEN_INT || typeSpec == TOKEN_LONG) {
            return new Double(0);
        } else if (typeSpec == TOKEN_DOUBLE || typeSpec == TOKEN_FLOAT) {
            return new Double(0.0);
        } else if (typeSpec == TOKEN_CHAR_KEY) {
            return new Character('\0');
        } else {
            return null;
        }
    }
    
    private Object ifStatement(Hashtable scope) throws Exception {
        consume(TOKEN_IF);
        consume(TOKEN_LPAREN);
        Object condition = expression(scope);
        consume(TOKEN_RPAREN);
        
        boolean conditionTrue = isTruthy(condition);
        Object result = null;
        
        // Save current tokens for else handling
        int savedTokenIndex = tokenIndex;
        
        if (conditionTrue) {
            // Execute if body
            result = statement(scope);
            // Skip else if/else blocks
            skipElseBlocks();
        } else {
            // Skip if body
            skipStatement();
            
            // Check for else if or else
            if (peek().type == TOKEN_ELSE) {
                consume(TOKEN_ELSE);
                if (peek().type == TOKEN_IF) {
                    // else if - recursively handle
                    result = ifStatement(scope);
                } else {
                    // else block
                    result = statement(scope);
                }
            }
        }
        
        return result;
    }
    
    private Object whileStatement(Hashtable scope) throws Exception {
        consume(TOKEN_WHILE);
        consume(TOKEN_LPAREN);
        
        int conditionStartTokenIndex = tokenIndex;
        Object result = null;
        
        loopDepth++;
        
        while (true) {
            tokenIndex = conditionStartTokenIndex;
            Object condition = expression(scope);
            consume(TOKEN_RPAREN);
            
            if (!isTruthy(condition) || breakLoop || doreturn) {
                skipStatement();
                break;
            }
            
            result = statement(scope);
            if (doreturn || breakLoop) { break; }
            tokenIndex = conditionStartTokenIndex;
        }
        
        loopDepth--;
        return result;
    }
    
    private Object doWhileStatement(Hashtable scope) throws Exception {
        consume(TOKEN_DO);
        
        int bodyStartTokenIndex = tokenIndex;
        Object result = null;
        
        loopDepth++;
        
        while (true) {
            tokenIndex = bodyStartTokenIndex;
            
            while (peek().type != TOKEN_WHILE) {
                result = statement(scope);
                if (doreturn || breakLoop) { break; }
            }
            
            consume(TOKEN_WHILE);
            consume(TOKEN_LPAREN);
            Object condition = expression(scope);
            consume(TOKEN_RPAREN);
            consume(TOKEN_SEMICOLON);
            
            if (!isTruthy(condition) || doreturn) { break; }
            if (breakLoop) { breakLoop = false; break; }
        }
        
        loopDepth--;
        return result;
    }
    
    private Object forStatement(Hashtable scope) throws Exception {
        consume(TOKEN_FOR);
        consume(TOKEN_LPAREN);
        
        // Initialization
        if (peek().type != TOKEN_SEMICOLON) {
            if (isTypeSpecifier(peek().type)) {
                declaration(scope);
            } else {
                expression(scope);
                consume(TOKEN_SEMICOLON);
            }
        } else {
            consume(TOKEN_SEMICOLON);
        }
        
        // Condition
        int conditionTokenIndex = tokenIndex;
        Object condition = null;
        boolean hasCondition = peek().type != TOKEN_SEMICOLON;
        if (hasCondition) {
            condition = expression(scope);
        }
        consume(TOKEN_SEMICOLON);
        
        // Increment
        int incrementTokenIndex = tokenIndex;
        boolean hasIncrement = peek().type != TOKEN_RPAREN;
        if (hasIncrement) {
            // Skip increment for now, will execute after each iteration
            skipIncrement();
        }
        consume(TOKEN_RPAREN);
        
        int bodyStartTokenIndex = tokenIndex;
        Object result = null;
        
        loopDepth++;
        
        while (true) {
            if (hasCondition) {
                tokenIndex = conditionTokenIndex;
                condition = expression(scope);
                consume(TOKEN_SEMICOLON);
                if (!isTruthy(condition) || breakLoop) { break; }
            }
            
            tokenIndex = bodyStartTokenIndex;
            result = statement(scope);
            if (doreturn || breakLoop) { break; }
            
            if (hasIncrement) {
                tokenIndex = incrementTokenIndex;
                expression(scope);
            }
            
            tokenIndex = conditionTokenIndex;
        }
        
        loopDepth--;
        return result;
    }
    
    private Object switchStatement(Hashtable scope) throws Exception {
        consume(TOKEN_SWITCH);
        consume(TOKEN_LPAREN);
        Object switchExpr = expression(scope);
        consume(TOKEN_RPAREN);
        consume(TOKEN_LBRACE);
        
        Object result = null;
        boolean caseMatched = false;
        boolean inMatchedBlock = false;
        
        while (peek().type != TOKEN_RBRACE && peek().type != TOKEN_EOF) {
            if (peek().type == TOKEN_CASE) {
                consume(TOKEN_CASE);
                Object caseValue = expression(scope);
                consume(TOKEN_COLON);
                
                if (!caseMatched && valuesEqual(switchExpr, caseValue)) {
                    caseMatched = true;
                    inMatchedBlock = true;
                }
                
                if (inMatchedBlock) {
                    while (peek().type != TOKEN_CASE && peek().type != TOKEN_DEFAULT && peek().type != TOKEN_RBRACE) {
                        result = statement(scope);
                        if (doreturn) { return result; }
                        if (breakLoop) { breakLoop = false; break; }
                    }
                    if (breakLoop) { break; }
                } else {
                    skipUntilNextCaseOrDefault();
                }
            }
            else if (peek().type == TOKEN_DEFAULT) {
                consume(TOKEN_DEFAULT);
                consume(TOKEN_COLON);
                
                if (!caseMatched) {
                    while (peek().type != TOKEN_RBRACE) {
                        result = statement(scope);
                        if (doreturn) { return result; }
                        if (breakLoop) { breakLoop = false; break; }
                    }
                    if (breakLoop) { break; }
                }
            }
            else {
                if (inMatchedBlock) {
                    result = statement(scope);
                    if (doreturn) { return result; }
                    if (breakLoop) { breakLoop = false; break; }
                } else {
                    skipStatement();
                }
            }
        }
        
        consume(TOKEN_RBRACE);
        return result;
    }
    
    private Object returnStatement(Hashtable scope) throws Exception {
        consume(TOKEN_RETURN);
        doreturn = true;
        
        if (peek().type == TOKEN_SEMICOLON) {
            consume(TOKEN_SEMICOLON);
            return null;
        }
        
        Object result = expression(scope);
        consume(TOKEN_SEMICOLON);
        return result;
    }
    
    private Object breakStatement(Hashtable scope) throws Exception {
        if (loopDepth == 0) { throw new Exception("break statement not within loop or switch"); }
        consume(TOKEN_BREAK);
        consume(TOKEN_SEMICOLON);
        breakLoop = true;
        return null;
    }
    
    private Object continueStatement(Hashtable scope) throws Exception {
        if (loopDepth == 0) { throw new Exception("continue statement not within loop"); }
        consume(TOKEN_CONTINUE);
        consume(TOKEN_SEMICOLON);
        // Skip to next iteration will be handled by loop
        return null;
    }
    
    private Object gotoStatement(Hashtable scope) throws Exception {
        consume(TOKEN_GOTO);
        String labelName = (String) consume(TOKEN_IDENTIFIER).value;
        consume(TOKEN_SEMICOLON);
        
        if (!labels.containsKey(labelName)) { throw new Exception("label '" + labelName + "' not found"); }
        
        Integer labelPos = (Integer) labels.get(labelName);
        tokenIndex = labelPos.intValue();
        return null;
    }
    
    private Object blockStatement(Hashtable scope) throws Exception {
        consume(TOKEN_LBRACE);
        
        Object result = null;
        while (peek().type != TOKEN_RBRACE && peek().type != TOKEN_EOF) {
            result = statement(scope);
            if (doreturn || breakLoop) { break; }
        }
        
        consume(TOKEN_RBRACE);
        return result;
    }
    
    private Object expression(Hashtable scope) throws Exception {
        return logicalOr(scope);
    }
    
    private Object logicalOr(Hashtable scope) throws Exception {
        Object left = logicalAnd(scope);
        while (peek().type == TOKEN_OR) {
            consume(TOKEN_OR);
            Object right = logicalAnd(scope);
            left = new Boolean(isTruthy(left) || isTruthy(right));
        }
        return left;
    }
    
    private Object logicalAnd(Hashtable scope) throws Exception {
        Object left = bitwiseOr(scope);
        while (peek().type == TOKEN_AND) {
            consume(TOKEN_AND);
            Object right = bitwiseOr(scope);
            left = new Boolean(isTruthy(left) && isTruthy(right));
        }
        return left;
    }
    
    private Object bitwiseOr(Hashtable scope) throws Exception {
        Object left = bitwiseXor(scope);
        while (peek().type == TOKEN_BITOR) {
            consume(TOKEN_BITOR);
            Object right = bitwiseXor(scope);
            left = new Double(toNumber(left).intValue() | toNumber(right).intValue());
        }
        return left;
    }
    
    private Object bitwiseXor(Hashtable scope) throws Exception {
        Object left = bitwiseAnd(scope);
        while (peek().type == TOKEN_BITXOR) {
            consume(TOKEN_BITXOR);
            Object right = bitwiseAnd(scope);
            left = new Double(toNumber(left).intValue() ^ toNumber(right).intValue());
        }
        return left;
    }
    
    private Object bitwiseAnd(Hashtable scope) throws Exception {
        Object left = equality(scope);
        while (peek().type == TOKEN_BITAND) {
            consume(TOKEN_BITAND);
            Object right = equality(scope);
            left = new Double(toNumber(left).intValue() & toNumber(right).intValue());
        }
        return left;
    }
    
    private Object equality(Hashtable scope) throws Exception {
        Object left = relational(scope);
        while (peek().type == TOKEN_EQ || peek().type == TOKEN_NE) {
            int op = peek().type;
            consume(op);
            Object right = relational(scope);
            boolean result;
            if (op == TOKEN_EQ) {
                result = valuesEqual(left, right);
            } else {
                result = !valuesEqual(left, right);
            }
            left = new Boolean(result);
        }
        return left;
    }
    
    private Object relational(Hashtable scope) throws Exception {
        Object left = shift(scope);
        while (peek().type == TOKEN_LT || peek().type == TOKEN_GT || 
               peek().type == TOKEN_LE || peek().type == TOKEN_GE) {
            int op = peek().type;
            consume(op);
            Object right = shift(scope);
            double lNum = toNumber(left).doubleValue();
            double rNum = toNumber(right).doubleValue();
            boolean result;
            if (op == TOKEN_LT) result = lNum < rNum;
            else if (op == TOKEN_GT) result = lNum > rNum;
            else if (op == TOKEN_LE) result = lNum <= rNum;
            else result = lNum >= rNum;
            left = new Boolean(result);
        }
        return left;
    }
    
    private Object shift(Hashtable scope) throws Exception {
        Object left = additive(scope);
        while (peek().type == TOKEN_LSHIFT || peek().type == TOKEN_RSHIFT) {
            int op = peek().type;
            consume(op);
            Object right = additive(scope);
            int lNum = toNumber(left).intValue();
            int rNum = toNumber(right).intValue();
            if (op == TOKEN_LSHIFT) {
                left = new Double(lNum << rNum);
            } else {
                left = new Double(lNum >> rNum);
            }
        }
        return left;
    }
    
    private Object additive(Hashtable scope) throws Exception {
        Object left = multiplicative(scope);
        while (peek().type == TOKEN_PLUS || peek().type == TOKEN_MINUS) {
            int op = peek().type;
            consume(op);
            Object right = multiplicative(scope);
            double lNum = toNumber(left).doubleValue();
            double rNum = toNumber(right).doubleValue();
            if (op == TOKEN_PLUS) {
                left = new Double(lNum + rNum);
            } else {
                left = new Double(lNum - rNum);
            }
        }
        return left;
    }
    
    private Object multiplicative(Hashtable scope) throws Exception {
        Object left = unary(scope);
        while (peek().type == TOKEN_STAR || peek().type == TOKEN_SLASH || peek().type == TOKEN_PERCENT) {
            int op = peek().type;
            consume(op);
            Object right = unary(scope);
            double lNum = toNumber(left).doubleValue();
            double rNum = toNumber(right).doubleValue();
            if (op == TOKEN_STAR) {
                left = new Double(lNum * rNum);
            } else if (op == TOKEN_SLASH) {
                if (rNum == 0) throw new Exception("Division by zero");
                left = new Double(lNum / rNum);
            } else {
                if (rNum == 0) throw new Exception("Modulo by zero");
                left = new Double(lNum % rNum);
            }
        }
        return left;
    }
    
    private Object unary(Hashtable scope) throws Exception {
        if (peek().type == TOKEN_PLUS) {
            consume(TOKEN_PLUS);
            return unary(scope);
        } else if (peek().type == TOKEN_MINUS) {
            consume(TOKEN_MINUS);
            Object val = unary(scope);
            return new Double(-toNumber(val).doubleValue());
        } else if (peek().type == TOKEN_NOT) {
            consume(TOKEN_NOT);
            Object val = unary(scope);
            return new Boolean(!isTruthy(val));
        } else if (peek().type == TOKEN_BITNOT) {
            consume(TOKEN_BITNOT);
            Object val = unary(scope);
            return new Double(~toNumber(val).intValue());
        } else if (peek().type == TOKEN_INCREMENT) {
            consume(TOKEN_INCREMENT);
            // Prefix increment
            if (peek().type == TOKEN_IDENTIFIER) {
                String varName = (String) consume(TOKEN_IDENTIFIER).value;
                Object val = scope.get(varName);
                Double newVal = new Double(toNumber(val).doubleValue() + 1);
                scope.put(varName, newVal);
                return newVal;
            }
        } else if (peek().type == TOKEN_DECREMENT) {
            consume(TOKEN_DECREMENT);
            if (peek().type == TOKEN_IDENTIFIER) {
                String varName = (String) consume(TOKEN_IDENTIFIER).value;
                Object val = scope.get(varName);
                Double newVal = new Double(toNumber(val).doubleValue() - 1);
                scope.put(varName, newVal);
                return newVal;
            }
        }
        return postfix(scope);
    }
    
    private Object postfix(Hashtable scope) throws Exception {
        Object primary = primary(scope);
        
        // Postfix increment/decrement
        if (peek().type == TOKEN_INCREMENT) {
            consume(TOKEN_INCREMENT);
            if (primary instanceof String) {
                String varName = (String) primary;
                Object val = scope.get(varName);
                Double newVal = new Double(toNumber(val).doubleValue() + 1);
                scope.put(varName, newVal);
                return val;
            }
        } else if (peek().type == TOKEN_DECREMENT) {
            consume(TOKEN_DECREMENT);
            if (primary instanceof String) {
                String varName = (String) primary;
                Object val = scope.get(varName);
                Double newVal = new Double(toNumber(val).doubleValue() - 1);
                scope.put(varName, newVal);
                return val;
            }
        }
        
        return primary;
    }
    
    private Object primary(Hashtable scope) throws Exception {
        CToken current = peek();
        
        if (current.type == TOKEN_NUMBER) {
            consume(TOKEN_NUMBER);
            return current.value;
        }
        else if (current.type == TOKEN_STRING) {
            consume(TOKEN_STRING);
            return current.value;
        }
        else if (current.type == TOKEN_CHAR) {
            consume(TOKEN_CHAR);
            return current.value;
        }
        else if (current.type == TOKEN_IDENTIFIER) {
            String name = (String) consume(TOKEN_IDENTIFIER).value;
            Object value = unwrap(scope.get(name));
            if (value == null && globals.containsKey(name)) {
                value = unwrap(globals.get(name));
            }
            
            // Array access
            if (peek().type == TOKEN_LBRACKET) {
                consume(TOKEN_LBRACKET);
                Object index = expression(scope);
                consume(TOKEN_RBRACKET);
                
                if (value instanceof Vector) {
                    int idx = toNumber(index).intValue();
                    Vector vec = (Vector) value;
                    if (idx >= 0 && idx < vec.size()) {
                        return vec.elementAt(idx);
                    }
                    return null;
                }
            }
            // Function call
            else if (peek().type == TOKEN_LPAREN) {
                return callFunction(name, scope);
            }
            
            return value;
        }
        else if (current.type == TOKEN_LPAREN) {
            consume(TOKEN_LPAREN);
            Object value = expression(scope);
            consume(TOKEN_RPAREN);
            return value;
        }
        else if (current.type == TOKEN_SIZEOF) {
            consume(TOKEN_SIZEOF);
            consume(TOKEN_LPAREN);
            // For simplicity, return size of type or expression
            if (isTypeSpecifier(peek().type)) {
                int type = peek().type;
                consume(type);
                consume(TOKEN_RPAREN);
                return new Double(getTypeSize(type));
            } else {
                Object expr = expression(scope);
                consume(TOKEN_RPAREN);
                return new Double(1); // Default size
            }
        }
        
        throw new Exception("Unexpected token in primary: " + current.type);
    }
    
    private Object callFunction(String funcName, Hashtable scope) throws Exception {
        consume(TOKEN_LPAREN);
        Vector args = new Vector();
        
        if (peek().type != TOKEN_RPAREN) {
            args.addElement(expression(scope));
            while (peek().type == TOKEN_COMMA) {
                consume(TOKEN_COMMA);
                args.addElement(expression(scope));
            }
        }
        consume(TOKEN_RPAREN);
        
        Object funcObj = unwrap(scope.get(funcName));
        if (funcObj == null && globals.containsKey(funcName)) {
            funcObj = unwrap(globals.get(funcName));
        }
        
        if (funcObj instanceof CFunction) {
            return ((CFunction) funcObj).call(args);
        } else {
            throw new Exception("Attempt to call non-function: " + funcName);
        }
    }
    
    // Helper methods
    private boolean isTypeSpecifier(int type) {
        return (type >= TOKEN_AUTO && type <= TOKEN_VOLATILE) || 
               type == TOKEN_INT || type == TOKEN_CHAR_KEY ||
               type == TOKEN_DOUBLE || type == TOKEN_FLOAT ||
               type == TOKEN_VOID;
    }
    
    private boolean isTruthy(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return ((Boolean) value).booleanValue();
        if (value instanceof Double) return ((Double) value).doubleValue() != 0;
        if (value instanceof String) return ((String) value).length() > 0;
        return true;
    }
    
    private Double toNumber(Object value) {
        if (value == null) return new Double(0);
        if (value instanceof Double) return (Double) value;
        if (value instanceof Boolean) return new Double(((Boolean) value).booleanValue() ? 1 : 0);
        if (value instanceof String) {
            try { return new Double(Double.parseDouble((String) value)); }
            catch (NumberFormatException e) { return new Double(0); }
        }
        return new Double(0);
    }
    
    private boolean valuesEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        if (a instanceof Double && b instanceof Double) {
            return ((Double) a).doubleValue() == ((Double) b).doubleValue();
        }
        return a.equals(b);
    }
    
    private int getTypeSize(int type) {
        switch (type) {
            case TOKEN_CHAR_KEY: return 1;
            case TOKEN_INT: return 4;
            case TOKEN_LONG: return 8;
            case TOKEN_FLOAT: return 4;
            case TOKEN_DOUBLE: return 8;
            default: return 4;
        }
    }
    
    private void skipStatement() throws Exception {
        int depth = 0;
        while (true) {
            CToken t = peek();
            if (t.type == TOKEN_LBRACE) { depth++; consume(TOKEN_LBRACE); }
            else if (t.type == TOKEN_RBRACE) { 
                if (depth == 0) break;
                depth--; 
                consume(TOKEN_RBRACE);
            }
            else if (t.type == TOKEN_SEMICOLON) { consume(TOKEN_SEMICOLON); break; }
            else if (t.type == TOKEN_EOF) { break; }
            else { consume(); }
            if (depth == 0 && (t.type == TOKEN_SEMICOLON || t.type == TOKEN_RBRACE)) break;
        }
    }
    
    private void skipElseBlocks() throws Exception {
        int savedIndex = tokenIndex;
        try {
            while (peek().type == TOKEN_ELSE) {
                consume(TOKEN_ELSE);
                if (peek().type == TOKEN_IF) {
                    consume(TOKEN_IF);
                    consume(TOKEN_LPAREN);
                    skipExpression();
                    consume(TOKEN_RPAREN);
                    skipStatement();
                } else {
                    skipStatement();
                }
            }
        } catch (Exception e) {
            tokenIndex = savedIndex;
        }
    }
    
    private void skipIncrement() throws Exception {
        int depth = 0;
        while (true) {
            CToken t = peek();
            if (t.type == TOKEN_LPAREN) depth++;
            else if (t.type == TOKEN_RPAREN) {
                if (depth == 0) break;
                depth--;
            }
            else if (t.type == TOKEN_EOF) break;
            consume();
        }
    }
    
    private void skipExpression() throws Exception {
        int depth = 0;
        while (true) {
            CToken t = peek();
            if (t.type == TOKEN_LPAREN) depth++;
            else if (t.type == TOKEN_RPAREN) {
                if (depth == 0) break;
                depth--;
            }
            else if (t.type == TOKEN_SEMICOLON) break;
            else if (t.type == TOKEN_EOF) break;
            consume();
        }
    }
    
    private void skipUntilNextCaseOrDefault() throws Exception {
        int depth = 0;
        while (true) {
            CToken t = peek();
            if (t.type == TOKEN_LBRACE) depth++;
            else if (t.type == TOKEN_RBRACE) {
                if (depth == 0) break;
                depth--;
            }
            else if ((t.type == TOKEN_CASE || t.type == TOKEN_DEFAULT) && depth == 0) {
                break;
            }
            else if (t.type == TOKEN_EOF) break;
            consume();
        }
    }
    
    private void collectLabels() throws Exception {
        int savedTokenIndex = tokenIndex;
        labels.clear();
        
        tokenIndex = 0;
        while (peek().type != TOKEN_EOF) {
            CToken token = peek();
            if (token.type == TOKEN_IDENTIFIER) {
                if (peek().type == TOKEN_COLON) {
                    String labelName = (String) consume(TOKEN_IDENTIFIER).value;
                    consume(TOKEN_COLON);
                    labels.put(labelName, new Integer(tokenIndex));
                } else {
                    consume();
                }
            } else {
                consume();
            }
        }
        
        tokenIndex = savedTokenIndex;
    }
    
    private Object unwrap(Object v) { return v; }
    
    // Tokenizer methods remain as previously implemented...
    
    public CToken peek() {
        if (tokenIndex < tokens.size()) {
            return (CToken) tokens.elementAt(tokenIndex);
        }
        return new CToken(TOKEN_EOF, "EOF");
    }
    
    public CToken peekNext() {
        if (tokenIndex + 1 < tokens.size()) {
            return (CToken) tokens.elementAt(tokenIndex + 1);
        }
        return new CToken(TOKEN_EOF, "EOF");
    }
    
    private CToken consume() {
        if (tokenIndex < tokens.size()) {
            return (CToken) tokens.elementAt(tokenIndex++);
        }
        return new CToken(TOKEN_EOF, "EOF");
    }
    
    private CToken consume(int expectedType) throws Exception {
        CToken token = peek();
        if (token.type == expectedType) {
            tokenIndex++;
            return token;
        }
        throw new Exception("Expected token type " + expectedType + " but got " + token.type);
    }
    
    // CFunction class for built-in and user-defined functions
    public class CFunction {
        private int type;
        private Vector params;
        private Vector bodyTokens;
        private Hashtable closureScope;
        
        public CFunction(int type) {
            this.type = type;
        }
        
        public CFunction(Vector params, Vector bodyTokens, Hashtable closureScope) {
            this.params = params;
            this.bodyTokens = bodyTokens;
            this.closureScope = closureScope;
            this.type = -1;
        }
        
        public Object call(Vector args) throws Exception {
            if (type != -1) {
                return builtinCall(args);
            }
            
            Hashtable funcScope = new Hashtable();
            for (Enumeration e = closureScope.keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                funcScope.put(key, closureScope.get(key));
            }
            
            for (int i = 0; i < params.size() && i < args.size(); i++) {
                funcScope.put((String) params.elementAt(i), args.elementAt(i));
            }
            
            int savedTokenIndex = tokenIndex;
            Vector savedTokens = tokens;
            
            tokens = bodyTokens;
            tokenIndex = 0;
            
            Object returnValue = null;
            while (peek().type != TOKEN_EOF) {
                Object result = statement(funcScope);
                if (doreturn) {
                    returnValue = result;
                    doreturn = false;
                    break;
                }
            }
            
            tokenIndex = savedTokenIndex;
            tokens = savedTokens;
            
            return returnValue;
        }
        
        private Object builtinCall(Vector args) throws Exception {
            // Built-in C functions can be added here
            return null;
        }
    }
    
    // Tokenizer methods (same as previous implementation)
    public Vector tokenize(String code) throws Exception {
        // ... (previous tokenizer implementation)
        return new Vector();
    }
    
    private boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
    
    private boolean isLetter(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }
    
    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
    
    private boolean isLetterOrDigit(char c) {
        return isLetter(c) || isDigit(c);
    }
}