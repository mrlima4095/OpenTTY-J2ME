import javax.microedition.lcdui.*;
import javax.microedition.io.file.*;
import javax.microedition.media.control.*;
import javax.microedition.media.*;
import javax.microedition.rms.*;
import javax.microedition.io.*;
import java.util.*;
import java.io.*;

// |
// C2ME Runtime - C Language Interpreter for J2ME
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

    // Built-in function IDs
    public static final int BUILTIN_PRINTF = 200;
    public static final int BUILTIN_PUTS = 201;
    public static final int BUILTIN_PUTCHAR = 202;
    public static final int BUILTIN_GETCHAR = 203;
    public static final int BUILTIN_GETS = 204;
    public static final int BUILTIN_SYSTEM = 205;
    public static final int BUILTIN_EXIT = 206;
    public static final int BUILTIN_MALLOC = 207;
    public static final int BUILTIN_FREE = 208;
    public static final int BUILTIN_STRLEN = 209;
    public static final int BUILTIN_STRCMP = 210;
    public static final int BUILTIN_STRCPY = 211;
    public static final int BUILTIN_STRCAT = 212;
    public static final int BUILTIN_ATOI = 213;
    public static final int BUILTIN_ITOA = 214;
    public static final int BUILTIN_ISDIGIT = 215;
    public static final int BUILTIN_ISALPHA = 216;
        
    public static class CToken { 
        int type; 
        Object value; 
        CToken(int type, Object value) { 
            this.type = type; 
            this.value = value; 
        } 
        public String toString() { 
            return "CToken(type=" + type + ", value=" + value + ")"; 
        } 
    }
    
    public C2ME(OpenTTY midlet, int id, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; 
        this.id = id; 
        this.PID = pid; 
        this.proc = proc; 
        this.stdout = stdout; 
        this.father = scope;
        this.tokenIndex = 0;
        //initializeGlobals();
    }
    
    private void initializeGlobals() {
        globals.put("NULL", null);
        globals.put("EOF", new Integer(-1));
        
        // Register built-in C functions
        globals.put("printf", new CFunction(BUILTIN_PRINTF));
        globals.put("puts", new CFunction(BUILTIN_PUTS));
        globals.put("putchar", new CFunction(BUILTIN_PUTCHAR));
        globals.put("getchar", new CFunction(BUILTIN_GETCHAR));
        globals.put("gets", new CFunction(BUILTIN_GETS));
        globals.put("system", new CFunction(BUILTIN_SYSTEM));
        globals.put("exit", new CFunction(BUILTIN_EXIT));
        globals.put("malloc", new CFunction(BUILTIN_MALLOC));
        globals.put("free", new CFunction(BUILTIN_FREE));
        globals.put("strlen", new CFunction(BUILTIN_STRLEN));
        globals.put("strcmp", new CFunction(BUILTIN_STRCMP));
        globals.put("strcpy", new CFunction(BUILTIN_STRCPY));
        globals.put("strcat", new CFunction(BUILTIN_STRCAT));
        globals.put("atoi", new CFunction(BUILTIN_ATOI));
        globals.put("itoa", new CFunction(BUILTIN_ITOA));
        globals.put("isdigit", new CFunction(BUILTIN_ISDIGIT));
        globals.put("isalpha", new CFunction(BUILTIN_ISALPHA));
    }
    
    // |
    // Run Source Code - C style: only function declarations, start at main()
    public Hashtable run(String source, String code, Hashtable args) {
        midlet.sys.put(PID, proc);
        globals.put("arg", args);
        Hashtable ITEM = new Hashtable();
        int exitStatus = 0;
        
        /*try {
            this.tokens = tokenize(code);
            collectLabels();
            
            // First pass: collect all function declarations
            // No code outside functions is allowed (like real C)
            while (peek().type != TOKEN_EOF) {
                if (isFunctionDeclaration()) {
                    // Declare the function (register it in globals)
                    declareFunction(globals);
                } else {
                    // Found code outside function - error (real C doesn't allow this)
                    throw new Exception("syntax error: code outside function");
                }
            }
            
            // Reset token index to find main
            tokenIndex = 0;
            
            // Find and execute main function
            CFunction mainFunc = (CFunction) globals.get("main");
            if (mainFunc == null) {
                throw new Exception("undefined reference to 'main'");
            }
            
            // Prepare argv for main
            Vector mainArgs = new Vector();
            mainArgs.addElement(new Double(args != null ? args.size() : 0)); // argc
            mainArgs.addElement(args != null ? args : new Hashtable()); // argv
            
            // Call main and get return value as exit status
            Object result = mainFunc.call(mainArgs);
            if (result instanceof Double) {
                exitStatus = ((Double) result).intValue();
            } else if (result instanceof Integer) {
                exitStatus = ((Integer) result).intValue();
            }
            
            status = exitStatus;
            
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
        }*/
        
        ITEM.put("status", new Integer(status));
        ITEM.put("exitcode", new Integer(exitStatus));
        return ITEM;
    }
    
    // |
    // Function Declaration Detection
    private boolean isFunctionDeclaration() throws Exception {
        int savedPos = tokenIndex;
        try {
            // Check pattern: type name ( parameters ) {
            if (isTypeSpecifier(peek().type)) {
                consume(); // type
                if (peek().type == TOKEN_IDENTIFIER) {
                    String name = (String) consume(TOKEN_IDENTIFIER).value;
                    if (peek().type == TOKEN_LPAREN) {
                        tokenIndex = savedPos;
                        return true;
                    }
                }
            }
        } catch (Exception e) {}
        tokenIndex = savedPos;
        return false;
    }
    
    private void declareFunction(Hashtable scope) throws Exception {
        int returnType = peek().type;
        consume(returnType);
        
        String funcName = (String) consume(TOKEN_IDENTIFIER).value;
        consume(TOKEN_LPAREN);
        
        // Parse parameters
        Vector params = new Vector();
        if (peek().type != TOKEN_RPAREN) {
            do {
                if (isTypeSpecifier(peek().type)) {
                    consume(); // param type
                    String paramName = (String) consume(TOKEN_IDENTIFIER).value;
                    params.addElement(paramName);
                } else if (peek().type == TOKEN_ELLIPSIS) {
                    consume(TOKEN_ELLIPSIS);
                    params.addElement("...");
                    break;
                } else {
                    throw new Exception("invalid parameter declaration");
                }
                if (peek().type == TOKEN_COMMA) {
                    consume(TOKEN_COMMA);
                } else {
                    break;
                }
            } while (true);
        }
        consume(TOKEN_RPAREN);
        
        // Parse function body
        consume(TOKEN_LBRACE);
        Vector bodyTokens = new Vector();
        int depth = 1;
        
        while (depth > 0 && peek().type != TOKEN_EOF) {
            CToken token = consume();
            
            if (token.type == TOKEN_LBRACE) {
                depth++;
            } else if (token.type == TOKEN_RBRACE) {
                depth--;
                if (depth == 0) {
                    break;
                }
            }
            
            if (depth > 0) {
                bodyTokens.addElement(token);
            }
        }
        
        // Create and register function
        CFunction func = new CFunction(params, bodyTokens, scope, returnType);
        scope.put(funcName, func);
    }
    
    // |
    // Statement Parsing
    public Object statement(Hashtable scope) throws Exception {
        CToken current = peek();
        
        if (status != 0) { 
            midlet.sys.remove(PID); 
            throw new Error(); 
        }
        if (!midlet.sys.containsKey(PID)) { 
            throw new Error("Process killed"); 
        }
        
        // Variable declaration
        if (isTypeSpecifier(current.type)) {
            return declaration(scope);
        }
        // Label
        else if (current.type == TOKEN_IDENTIFIER && peekNext().type == TOKEN_COLON) {
            String labelName = (String) consume(TOKEN_IDENTIFIER).value;
            consume(TOKEN_COLON);
            labels.put(labelName, new Integer(tokenIndex));
            return null;
        }
        else if (current.type == TOKEN_IDENTIFIER) {
            String varName = (String) consume(TOKEN_IDENTIFIER).value;
            
            // Function call
            if (peek().type == TOKEN_LPAREN) {
                Object result = callFunction(varName, scope);
                if (peek().type == TOKEN_SEMICOLON) {
                    consume(TOKEN_SEMICOLON);
                }
                return result;
            }
            // Assignment
            else if (peek().type == TOKEN_ASSIGN) {
                consume(TOKEN_ASSIGN);
                Object value = expression(scope);
                scope.put(varName, value);
                consume(TOKEN_SEMICOLON);
                return value;
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
        
        for (int i = 0; i < varNames.size(); i++) {
            String name = (String) varNames.elementAt(i);
            Object value = initializers.elementAt(i);
            scope.put(name, value);
        }
        
        return null;
    }
    
    private Object getDefaultValue(int typeSpec) {
        if (typeSpec == TOKEN_INT || typeSpec == TOKEN_LONG || typeSpec == TOKEN_SHORT) {
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
        
        if (conditionTrue) {
            result = statement(scope);
            // Skip else/else if blocks
            skipElseBlocks();
        } else {
            skipStatement();
            
            if (peek().type == TOKEN_ELSE) {
                consume(TOKEN_ELSE);
                if (peek().type == TOKEN_IF) {
                    result = ifStatement(scope);
                } else {
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
        if (loopDepth == 0) { 
            throw new Exception("break statement not within loop or switch"); 
        }
        consume(TOKEN_BREAK);
        consume(TOKEN_SEMICOLON);
        breakLoop = true;
        return null;
    }
    
    private Object continueStatement(Hashtable scope) throws Exception {
        if (loopDepth == 0) { 
            throw new Exception("continue statement not within loop"); 
        }
        consume(TOKEN_CONTINUE);
        consume(TOKEN_SEMICOLON);
        return null;
    }
    
    private Object gotoStatement(Hashtable scope) throws Exception {
        consume(TOKEN_GOTO);
        String labelName = (String) consume(TOKEN_IDENTIFIER).value;
        consume(TOKEN_SEMICOLON);
        
        if (!labels.containsKey(labelName)) { 
            throw new Exception("label '" + labelName + "' not found"); 
        }
        
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
    
    // |
    // Expression Parsing
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
            boolean result = (op == TOKEN_EQ) ? valuesEqual(left, right) : !valuesEqual(left, right);
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
            left = new Double(op == TOKEN_LSHIFT ? (lNum << rNum) : (lNum >> rNum));
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
            left = new Double(op == TOKEN_PLUS ? (lNum + rNum) : (lNum - rNum));
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
            if (isTypeSpecifier(peek().type)) {
                int type = peek().type;
                consume(type);
                consume(TOKEN_RPAREN);
                return new Double(getTypeSize(type));
            } else {
                Object expr = expression(scope);
                consume(TOKEN_RPAREN);
                return new Double(1);
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
    
    // |
    // Helper Methods
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
            try { 
                return new Double(Double.parseDouble((String) value)); 
            } catch (NumberFormatException e) { 
                return new Double(0); 
            }
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
            if (t.type == TOKEN_LBRACE) { 
                depth++; 
                consume(TOKEN_LBRACE); 
            }
            else if (t.type == TOKEN_RBRACE) { 
                if (depth == 0) break;
                depth--; 
                consume(TOKEN_RBRACE);
            }
            else if (t.type == TOKEN_SEMICOLON) { 
                consume(TOKEN_SEMICOLON); 
                break; 
            }
            else if (t.type == TOKEN_EOF) { 
                break; 
            }
            else { 
                consume(); 
            }
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
            if (token.type == TOKEN_IDENTIFIER && peekNext().type == TOKEN_COLON) {
                String labelName = (String) consume(TOKEN_IDENTIFIER).value;
                consume(TOKEN_COLON);
                labels.put(labelName, new Integer(tokenIndex));
            } else {
                consume();
            }
        }
        
        tokenIndex = savedTokenIndex;
    }
    
    private Object unwrap(Object v) { 
        return v; 
    }
    
    // |
    // Tokenizer Methods
    public Vector tokenize(String code) throws Exception {
        if (midlet.cacheLua.containsKey(code)) { 
            return (Vector) midlet.cacheLua.get(code); 
        }

        Vector tokens = new Vector();
        int i = 0;
        
        if (code.startsWith("#!")) {
            while (i < code.length() && code.charAt(i) != '\n') { i++; }
            if (i < code.length() && code.charAt(i) == '\n') { i++; }
        }
        
        while (i < code.length()) {
            char c = code.charAt(i);

            if (isWhitespace(c)) { 
                i++; 
            }
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
                StringBuffer sb = new StringBuffer(); 
                i++;
                while (i < code.length() && code.charAt(i) != '"') {
                    if (code.charAt(i) == '\\' && i + 1 < code.length()) { 
                        sb.append(code.charAt(i)); 
                        i++; 
                    }
                    sb.append(code.charAt(i)); 
                    i++;
                }
                if (i < code.length() && code.charAt(i) == '"') i++;
                tokens.addElement(new CToken(TOKEN_STRING, sb.toString()));
            }
            else if (c == '\'') {
                StringBuffer sb = new StringBuffer(); 
                i++;
                while (i < code.length() && code.charAt(i) != '\'') {
                    if (code.charAt(i) == '\\' && i + 1 < code.length()) { 
                        sb.append(code.charAt(i)); 
                        i++; 
                    }
                    sb.append(code.charAt(i)); 
                    i++;
                }
                if (i < code.length() && code.charAt(i) == '\'') i++;
                tokens.addElement(new CToken(TOKEN_CHAR, sb.toString()));
            }
            else if (isDigit(c) || (c == '.' && i + 1 < code.length() && isDigit(code.charAt(i + 1)))) {
                StringBuffer sb = new StringBuffer();
                boolean hasDecimal = false, hasExponent = false;
                while (i < code.length()) {
                    c = code.charAt(i);
                    if (isDigit(c)) { 
                        sb.append(c); 
                        i++; 
                    }
                    else if (c == '.' && !hasDecimal && !hasExponent) { 
                        sb.append(c); 
                        hasDecimal = true; 
                        i++; 
                    }
                    else if ((c == 'e' || c == 'E') && !hasExponent && i + 1 < code.length()) {
                        sb.append(c); 
                        hasExponent = true; 
                        i++;
                        if (i < code.length() && (code.charAt(i) == '+' || code.charAt(i) == '-')) { 
                            sb.append(code.charAt(i)); 
                            i++; 
                        }
                    }
                    else if ((c == 'f' || c == 'F' || c == 'l' || c == 'L') && i + 1 < code.length() && !isLetterOrDigit(code.charAt(i + 1))) {
                        sb.append(c); 
                        i++; 
                        break;
                    }
                    else break;
                }
                try {
                    String numStr = sb.toString();
                    if (numStr.indexOf('.') != -1 || numStr.indexOf('e') != -1 || numStr.indexOf('E') != -1)
                        tokens.addElement(new CToken(TOKEN_NUMBER, new Double(Double.parseDouble(numStr))));
                    else tokens.addElement(new CToken(TOKEN_NUMBER, new Integer(Integer.parseInt(numStr))));
                } catch (NumberFormatException e) { 
                    throw new RuntimeException("Invalid number format '" + sb.toString() + "'"); 
                }
                continue;
            }
            else if (isLetter(c) || c == '_') {
                StringBuffer sb = new StringBuffer();
                while (i < code.length() && (isLetterOrDigit(code.charAt(i)) || code.charAt(i) == '_')) { 
                    sb.append(code.charAt(i)); 
                    i++; 
                }
                String word = sb.toString();
                int type = isKeyword(word);
                tokens.addElement(new CToken(type != -1 ? type : TOKEN_IDENTIFIER, word));
            }
            else if (c == '+' && i + 1 < code.length() && code.charAt(i + 1) == '+') { 
                tokens.addElement(new CToken(TOKEN_INCREMENT, "++")); 
                i += 2; 
            }
            else if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '-') { 
                tokens.addElement(new CToken(TOKEN_DECREMENT, "--")); 
                i += 2; 
            }
            else if (c == '-' && i + 1 < code.length() && code.charAt(i + 1) == '>') { 
                tokens.addElement(new CToken(TOKEN_ARROW, "->")); 
                i += 2; 
            }
            else if (c == '=' && i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                tokens.addElement(new CToken(TOKEN_EQ, "==")); 
                i += 2; 
            }
            else if (c == '!' && i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                tokens.addElement(new CToken(TOKEN_NE, "!=")); 
                i += 2; 
            }
            else if (c == '<' && i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                tokens.addElement(new CToken(TOKEN_LE, "<=")); 
                i += 2; 
            }
            else if (c == '>' && i + 1 < code.length() && code.charAt(i + 1) == '=') { 
                tokens.addElement(new CToken(TOKEN_GE, ">=")); 
                i += 2; 
            }
            else if (c == '<' && i + 1 < code.length() && code.charAt(i + 1) == '<') { 
                tokens.addElement(new CToken(TOKEN_LSHIFT, "<<")); 
                i += 2; 
            }
            else if (c == '>' && i + 1 < code.length() && code.charAt(i + 1) == '>') { 
                tokens.addElement(new CToken(TOKEN_RSHIFT, ">>")); 
                i += 2; 
            }
            else if (c == '&' && i + 1 < code.length() && code.charAt(i + 1) == '&') { 
                tokens.addElement(new CToken(TOKEN_AND, "&&")); 
                i += 2; 
            }
            else if (c == '|' && i + 1 < code.length() && code.charAt(i + 1) == '|') { 
                tokens.addElement(new CToken(TOKEN_OR, "||")); 
                i += 2; 
            }
            else if (c == '.' && i + 2 < code.length() && code.charAt(i + 1) == '.' && code.charAt(i + 2) == '.') { 
                tokens.addElement(new CToken(TOKEN_ELLIPSIS, "...")); 
                i += 3; 
            }
            else if (c == '#') { 
                tokens.addElement(new CToken(TOKEN_PREPROCESSOR, "#")); 
                i++; 
            }
            else if (c == '+') { 
                tokens.addElement(new CToken(TOKEN_PLUS, "+")); 
                i++; 
            }
            else if (c == '-') { 
                tokens.addElement(new CToken(TOKEN_MINUS, "-")); 
                i++; 
            }
            else if (c == '*') { 
                tokens.addElement(new CToken(TOKEN_STAR, "*")); 
                i++; 
            }
            else if (c == '/') { 
                tokens.addElement(new CToken(TOKEN_SLASH, "/")); 
                i++; 
            }
            else if (c == '%') { 
                tokens.addElement(new CToken(TOKEN_PERCENT, "%")); 
                i++; 
            }
            else if (c == '=') { 
                tokens.addElement(new CToken(TOKEN_ASSIGN, "=")); 
                i++; 
            }
            else if (c == '!') { 
                tokens.addElement(new CToken(TOKEN_NOT, "!")); 
                i++; 
            }
            else if (c == '<') { 
                tokens.addElement(new CToken(TOKEN_LT, "<")); 
                i++; 
            }
            else if (c == '>') { 
                tokens.addElement(new CToken(TOKEN_GT, ">")); 
                i++; 
            }
            else if (c == '&') { 
                tokens.addElement(new CToken(TOKEN_BITAND, "&")); 
                i++; 
            }
            else if (c == '|') { 
                tokens.addElement(new CToken(TOKEN_BITOR, "|")); 
                i++; 
            }
            else if (c == '^') { 
                tokens.addElement(new CToken(TOKEN_BITXOR, "^")); 
                i++; 
            }
            else if (c == '~') { 
                tokens.addElement(new CToken(TOKEN_BITNOT, "~")); 
                i++; 
            }
            else if (c == '(') { 
                tokens.addElement(new CToken(TOKEN_LPAREN, "(")); 
                i++; 
            }
            else if (c == ')') { 
                tokens.addElement(new CToken(TOKEN_RPAREN, ")")); 
                i++; 
            }
            else if (c == '{') { 
                tokens.addElement(new CToken(TOKEN_LBRACE, "{")); 
                i++; 
            }
            else if (c == '}') { 
                tokens.addElement(new CToken(TOKEN_RBRACE, "}")); 
                i++; 
            }
            else if (c == '[') { 
                tokens.addElement(new CToken(TOKEN_LBRACKET, "[")); 
                i++; 
            }
            else if (c == ']') { 
                tokens.addElement(new CToken(TOKEN_RBRACKET, "]")); 
                i++; 
            }
            else if (c == ';') { 
                tokens.addElement(new CToken(TOKEN_SEMICOLON, ";")); 
                i++; 
            }
            else if (c == ',') { 
                tokens.addElement(new CToken(TOKEN_COMMA, ",")); 
                i++; 
            }
            else if (c == '.') { 
                tokens.addElement(new CToken(TOKEN_DOT, ".")); 
                i++; 
            }
            else if (c == ':') { 
                tokens.addElement(new CToken(TOKEN_COLON, ":")); 
                i++; 
            }
            else if (c == '?') { 
                tokens.addElement(new CToken(TOKEN_QUESTION, "?")); 
                i++; 
            }
            else { 
                throw new Exception("Unexpected character '" + c + "'"); 
            }
        }

        tokens.addElement(new CToken(TOKEN_EOF, "EOF"));
        
        if (midlet.useCache) { 
            if (midlet.cacheLua.size() > 100) { 
                midlet.cacheLua.clear(); 
            } 
            midlet.cacheLua.put(code, tokens); 
        }
        
        return tokens;
    }

    private int isKeyword(String word) {
        if (word.equals("auto")) return TOKEN_AUTO;
        if (word.equals("break")) return TOKEN_BREAK;
        if (word.equals("case")) return TOKEN_CASE;
        if (word.equals("char")) return TOKEN_CHAR_KEY;
        if (word.equals("const")) return TOKEN_CONST;
        if (word.equals("continue")) return TOKEN_CONTINUE;
        if (word.equals("default")) return TOKEN_DEFAULT;
        if (word.equals("do")) return TOKEN_DO;
        if (word.equals("double")) return TOKEN_DOUBLE;
        if (word.equals("else")) return TOKEN_ELSE;
        if (word.equals("enum")) return TOKEN_ENUM;
        if (word.equals("extern")) return TOKEN_EXTERN;
        if (word.equals("float")) return TOKEN_FLOAT;
        if (word.equals("for")) return TOKEN_FOR;
        if (word.equals("goto")) return TOKEN_GOTO;
        if (word.equals("if")) return TOKEN_IF;
        if (word.equals("int")) return TOKEN_INT;
        if (word.equals("long")) return TOKEN_LONG;
        if (word.equals("register")) return TOKEN_REGISTER;
        if (word.equals("return")) return TOKEN_RETURN;
        if (word.equals("short")) return TOKEN_SHORT;
        if (word.equals("signed")) return TOKEN_SIGNED;
        if (word.equals("sizeof")) return TOKEN_SIZEOF;
        if (word.equals("static")) return TOKEN_STATIC;
        if (word.equals("struct")) return TOKEN_STRUCT;
        if (word.equals("switch")) return TOKEN_SWITCH;
        if (word.equals("typedef")) return TOKEN_TYPEDEF;
        if (word.equals("union")) return TOKEN_UNION;
        if (word.equals("unsigned")) return TOKEN_UNSIGNED;
        if (word.equals("void")) return TOKEN_VOID;
        if (word.equals("volatile")) return TOKEN_VOLATILE;
        if (word.equals("while")) return TOKEN_WHILE;
        return -1;
    }

    private boolean isWhitespace(char c) { 
        return c == ' ' || c == '\t' || c == '\n' || c == '\r'; 
    }
    
    private boolean isLetter(char c) { 
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); 
    }
    
    private boolean isDigit(char c) { 
        return c >= '0' && c <= '9'; 
    }

    private boolean isLetterOrDigit(char c) { 
        return isLetter(c) || isDigit(c); 
    }

    // |
    // Token Navigation
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

    // Helper methods for type conversion
    private String toCString(Object obj) {
        if (obj == null) return "";
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Character) return String.valueOf((Character) obj);
        if (obj instanceof Double) {
            double d = ((Double) obj).doubleValue();
            if (d == (long) d) return String.valueOf((long) d);
            return String.valueOf(d);
        }
        if (obj instanceof Integer) return String.valueOf(((Integer) obj).intValue());
        if (obj instanceof Boolean) return ((Boolean) obj).booleanValue() ? "true" : "false";
        if (obj instanceof Vector) {
            // Treat as string builder simulation
            StringBuffer sb = new StringBuffer();
            for (int i = 1; i < ((Vector) obj).size(); i++) {
                Object elem = ((Vector) obj).elementAt(i);
                if (elem instanceof Integer) {
                    sb.append((char) ((Integer) elem).intValue());
                }
            }
            return sb.toString();
        }
        return obj.toString();
    }

    private int toInteger(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Integer) return ((Integer) obj).intValue();
        if (obj instanceof Double) return ((Double) obj).intValue();
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (obj instanceof Boolean) return ((Boolean) obj).booleanValue() ? 1 : 0;
        if (obj instanceof Character) return (int) ((Character) obj).charValue();
        return 0;
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Double) return ((Double) obj).doubleValue();
        if (obj instanceof Integer) return ((Integer) obj).doubleValue();
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private String formatNumber(int num, String format) {
        if (format.equals("x")) return Integer.toHexString(num);
        if (format.equals("X")) return Integer.toHexString(num).toUpperCase();
        if (format.equals("o")) return Integer.toOctalString(num);
        return String.valueOf(num);
    }
    
    // |
    // CFunction Class
    public class CFunction {
        private Vector params;
        private Vector bodyTokens;
        private Hashtable closureScope;
        private int returnType;
        private int builtinId;
        
        public CFunction(int builtinId) {
            this.builtinId = builtinId;
            this.params = new Vector();
        }
        
        public CFunction(Vector params, Vector bodyTokens, Hashtable closureScope, int returnType) {
            this.params = params;
            this.bodyTokens = bodyTokens;
            this.closureScope = closureScope;
            this.returnType = returnType;
            this.builtinId = -1;
        }
        
        public Object call(Vector args) throws Exception {
            if (builtinId != -1) {
                return builtinCall(args);
            }
            
            Hashtable funcScope = new Hashtable();
            
            // Copy closure scope
            if (closureScope != null) {
                for (Enumeration e = closureScope.keys(); e.hasMoreElements();) {
                    String key = (String) e.nextElement();
                    funcScope.put(key, closureScope.get(key));
                }
            }
            
            // Bind parameters
            for (int i = 0; i < params.size() && i < args.size(); i++) {
                funcScope.put((String) params.elementAt(i), args.elementAt(i));
            }
            
            // Save current state
            int savedTokenIndex = tokenIndex;
            Vector savedTokens = tokens;
            boolean savedDoreturn = doreturn;
            boolean savedBreakLoop = breakLoop;
            
            // Execute function body
            tokens = bodyTokens;
            tokenIndex = 0;
            doreturn = false;
            
            Object returnValue = null;
            
            while (peek().type != TOKEN_EOF && !doreturn) {
                Object result = statement(funcScope);
                if (doreturn) {
                    returnValue = result;
                    break;
                }
            }
            
            // Restore state
            tokenIndex = savedTokenIndex;
            tokens = savedTokens;
            doreturn = savedDoreturn;
            breakLoop = savedBreakLoop;
            
            return returnValue;
        }
        
        private Object builtinCall(Vector args) throws Exception {
            switch (builtinId) {
                case BUILTIN_PRINTF:
                    return builtin_printf(args);
                case BUILTIN_PUTS:
                    return builtin_puts(args);
                case BUILTIN_PUTCHAR:
                    return builtin_putchar(args);
                case BUILTIN_GETCHAR:
                    return builtin_getchar();
                case BUILTIN_GETS:
                    return builtin_gets(args);
                case BUILTIN_SYSTEM:
                    return builtin_system(args);
                case BUILTIN_EXIT:
                    return builtin_exit(args);
                case BUILTIN_MALLOC:
                    return builtin_malloc(args);
                case BUILTIN_FREE:
                    return builtin_free(args);
                case BUILTIN_STRLEN:
                    return builtin_strlen(args);
                case BUILTIN_STRCMP:
                    return builtin_strcmp(args);
                case BUILTIN_STRCPY:
                    return builtin_strcpy(args);
                case BUILTIN_STRCAT:
                    return builtin_strcat(args);
                case BUILTIN_ATOI:
                    return builtin_atoi(args);
                case BUILTIN_ITOA:
                    return builtin_itoa(args);
                case BUILTIN_ISDIGIT:
                    return builtin_isdigit(args);
                case BUILTIN_ISALPHA:
                    return builtin_isalpha(args);
            }
            return null;
        }
        // printf - Formatted output
        private Object builtin_printf(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(0);
            
            StringBuffer output = new StringBuffer();
            String format = toCString(args.elementAt(0));
            int argIndex = 1;
            
            for (int i = 0; i < format.length(); i++) {
                char c = format.charAt(i);
                
                if (c == '%' && i + 1 < format.length()) {
                    i++;
                    char spec = format.charAt(i);
                    
                    switch (spec) {
                        case 'd':
                        case 'i': {
                            if (argIndex < args.size()) {
                                int val = toInteger(args.elementAt(argIndex++));
                                output.append(val);
                            }
                            break;
                        }
                        case 'u': {
                            if (argIndex < args.size()) {
                                int val = toInteger(args.elementAt(argIndex++));
                                output.append(val & 0xFFFFFFFFL);
                            }
                            break;
                        }
                        case 'x': {
                            if (argIndex < args.size()) {
                                int val = toInteger(args.elementAt(argIndex++));
                                output.append(Integer.toHexString(val));
                            }
                            break;
                        }
                        case 'X': {
                            if (argIndex < args.size()) {
                                int val = toInteger(args.elementAt(argIndex++));
                                output.append(Integer.toHexString(val).toUpperCase());
                            }
                            break;
                        }
                        case 'f': {
                            if (argIndex < args.size()) {
                                double val = toDouble(args.elementAt(argIndex++));
                                output.append(val);
                            }
                            break;
                        }
                        case 'c': {
                            if (argIndex < args.size()) {
                                int val = toInteger(args.elementAt(argIndex++));
                                output.append((char) val);
                            }
                            break;
                        }
                        case 's': {
                            if (argIndex < args.size()) {
                                String val = toCString(args.elementAt(argIndex++));
                                output.append(val);
                            }
                            break;
                        }
                        case 'p': {
                            output.append("0x");
                            if (argIndex < args.size()) {
                                int val = toInteger(args.elementAt(argIndex++));
                                output.append(Integer.toHexString(val));
                            }
                            break;
                        }
                        case '%': {
                            output.append('%');
                            break;
                        }
                        default:
                            output.append('%').append(spec);
                            break;
                    }
                } else {
                    output.append(c);
                }
            }
            
            // Print to stdout using OpenTTY's print method
            midlet.print(output.toString(), stdout, id, father);
            return new Integer(output.length());
        }

        // puts - Print string with newline
        private Object builtin_puts(Vector args) throws Exception {
            if (args.isEmpty()) {
                midlet.print("", stdout, id, father);
                return new Integer(1);
            }
            
            String str = toCString(args.elementAt(0));
            midlet.print(str, stdout, id, father);
            return new Integer(str.length() + 1);
        }

        // putchar - Print single character
        private Object builtin_putchar(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(-1);
            
            int c = toInteger(args.elementAt(0));
            midlet.print(String.valueOf((char) c), stdout, id, father);
            return new Integer(c);
        }

        // getchar - Read character from stdin
        private Object builtin_getchar() throws Exception {
            String stdin = midlet.stdin.getString();
            if (stdin == null || stdin.length() == 0) return new Integer(-1);
            
            char c = stdin.charAt(0);
            midlet.stdin.setString(stdin.length() > 1 ? stdin.substring(1) : "");
            return new Integer((int) c);
        }

        // gets - Read string from stdin
        private Object builtin_gets(Vector args) throws Exception {
            String stdin = midlet.stdin.getString();
            if (stdin == null || stdin.length() == 0) return null;
            
            // Read until newline
            int newlinePos = stdin.indexOf('\n');
            String line;
            if (newlinePos != -1) {
                line = stdin.substring(0, newlinePos);
                midlet.stdin.setString(stdin.substring(newlinePos + 1));
            } else {
                line = stdin;
                midlet.stdin.setString("");
            }
            
            return line;
        }

        // system - Execute shell command
        private Object builtin_system(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(0);
            
            String command = toCString(args.elementAt(0));
            
            // Execute command using OpenTTY's shell
            Vector cmdArgs = new Vector();
            cmdArgs.addElement(command);
            
            Object shell = midlet.shell;
            if (shell instanceof Lua.LuaFunction) {
                try {
                    Object result = ((Lua.LuaFunction) shell).call(cmdArgs);
                    if (result instanceof Double) {
                        return new Integer(((Double) result).intValue());
                    }
                } catch (Exception e) {
                    return new Integer(-1);
                }
            } else if (shell instanceof C2ME.CFunction) {
                try {
                    Object result = ((C2ME.CFunction) shell).call(cmdArgs);
                    if (result instanceof Double) {
                        return new Integer(((Double) result).intValue());
                    }
                } catch (Exception e) {
                    return new Integer(-1);
                }
            }
            
            return new Integer(0);
        }

        // exit - Terminate program
        private Object builtin_exit(Vector args) throws Exception {
            int exitCode = 0;
            if (!args.isEmpty()) {
                exitCode = toInteger(args.elementAt(0));
            }
            
            doreturn = true;
            status = exitCode;
            return new Integer(exitCode);
        }

        // malloc - Allocate memory (simulated with Hashtable)
        private Object builtin_malloc(Vector args) throws Exception {
            if (args.isEmpty()) return null;
            
            int size = toInteger(args.elementAt(0));
            // Simulate memory allocation with a Vector
            Vector memory = new Vector();
            memory.addElement(new Integer(size)); // Store size at index 0
            for (int i = 0; i < size; i++) {
                memory.addElement(new Integer(0));
            }
            return memory;
        }

        // free - Free allocated memory
        private Object builtin_free(Vector args) throws Exception {
            if (args.isEmpty()) return null;
            
            Object ptr = args.elementAt(0);
            if (ptr instanceof Vector) {
                // Just let GC collect it
                ptr = null;
            }
            return null;
        }

        // strlen - Get string length
        private Object builtin_strlen(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(0);
            
            String str = toCString(args.elementAt(0));
            return new Integer(str.length());
        }

        // strcmp - Compare strings
        private Object builtin_strcmp(Vector args) throws Exception {
            if (args.size() < 2) return new Integer(0);
            
            String s1 = toCString(args.elementAt(0));
            String s2 = toCString(args.elementAt(1));
            
            return new Integer(s1.compareTo(s2));
        }

        // strcpy - Copy string
        private Object builtin_strcpy(Vector args) throws Exception {
            if (args.size() < 2) return null;
            
            String src = toCString(args.elementAt(1));
            return src; // Return copied string
        }

        // strcat - Concatenate strings
        private Object builtin_strcat(Vector args) throws Exception {
            if (args.size() < 2) return null;
            
            String s1 = toCString(args.elementAt(0));
            String s2 = toCString(args.elementAt(1));
            
            return s1 + s2;
        }

        // atoi - Convert string to integer
        private Object builtin_atoi(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(0);
            
            String str = toCString(args.elementAt(0));
            try {
                return new Integer(Integer.parseInt(str));
            } catch (NumberFormatException e) {
                return new Integer(0);
            }
        }

        // itoa - Convert integer to string (returns as number for simplicity)
        private Object builtin_itoa(Vector args) throws Exception {
            if (args.isEmpty()) return "";
            
            int value = toInteger(args.elementAt(0));
            return Integer.toString(value);
        }

        // isdigit - Check if character is digit
        private Object builtin_isdigit(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(0);
            
            int c = toInteger(args.elementAt(0));
            return new Integer((c >= '0' && c <= '9') ? 1 : 0);
        }

        // isalpha - Check if character is alphabetic
        private Object builtin_isalpha(Vector args) throws Exception {
            if (args.isEmpty()) return new Integer(0);
            
            int c = toInteger(args.elementAt(0));
            return new Integer(((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) ? 1 : 0);
        }

    }
}