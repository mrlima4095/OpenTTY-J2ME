/*
 Lua.java
 Minimal Lua subset interpreter targeted for CLDC 1.0 / MIDP 2.0
 For embedding into OpenTTY.

 Supported features (minimal subset):
  - Numbers (double), strings ("..."), booleans (true/false), nil
  - Variables (global and local via function args)
  - Arithmetic: + - * / %
  - Comparison: == ~= < > <= >=
  - Logical: and, or, not
  - Statements: assignment, if-then[-else], while, return
  - Functions: define with `function name(args) ... end`, call with positional args
  - Builtin: print(...)

 Limitations:
  - No tables/metatables
  - No coroutines
  - No standard libs (io, os) beyond `print`
  - Parser is simple and not fully Lua-compliant (but handles common cases)
  - Error messages are basic

 Integration notes:
  - Replace TinyLuaOutput.println(...) with OpenTTY's terminal write method.
  - Keep the class in the MIDlet project, compile with CLDC-targeting javac.

 Usage example (in a resource script):
  function fact(n)
    if n <= 1 then return 1 end
    return n * fact(n-1)
  end
  print("fact(6)", fact(6))

*/

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.Hashtable;
import javax.microedition.lcdui.*;

/* === Lexer === */
class TLToken {
    public String type; // e.g. IDENT, NUMBER, STRING, SYMBOL, KEYWORD, EOF
    public String text;
    public TLToken(String type, String text) { this.type = type; this.text = text; }
}

class TLLexer {
    private String src;
    private int pos;
    private int len;
    private static final String[] keywords = {"function","end","if","then","else","while","do","return","true","false","nil","and","or","not"};

    public TLLexer(String s) {
        this.src = s;
        this.pos = 0;
        this.len = s.length();
    }

    private char peek() { return pos < len ? src.charAt(pos) : '\0'; }
    private char next() { return pos < len ? src.charAt(pos++) : '\0'; }
    private void skipSpace() {
        while (pos < len) {
            char c = peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') { pos++; continue; }
            // comments: -- to end of line
            if (c == '-' && pos+1 < len && src.charAt(pos+1) == '-') {
                pos += 2;
                while (pos < len && src.charAt(pos) != '\n') pos++;
                continue;
            }
            break;
        }
    }

    private boolean isIdentStart(char c) { return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'; }
    private boolean isIdentPart(char c) { return isIdentStart(c) || (c >= '0' && c <= '9'); }
    private boolean isDigit(char c) { return c >= '0' && c <= '9'; }

    public TLToken nextToken() {
        skipSpace();
        if (pos >= len) return new TLToken("EOF", "");
        char c = peek();
        // strings
        if (c == '"') {
            pos++;
            StringBuffer sb = new StringBuffer();
            while (pos < len) {
                char d = next();
                if (d == '\\') {
                    if (pos < len) {
                        char e = next();
                        if (e == 'n') sb.append('\n');
                        else if (e == 't') sb.append('\t');
                        else sb.append(e);
                    }
                } else if (d == '"') {
                    break;
                } else sb.append(d);
            }
            return new TLToken("STRING", sb.toString());
        }
        // number
        if (isDigit(c) || (c == '.' && pos+1 < len && isDigit(src.charAt(pos+1)))) {
            int st = pos;
            boolean dot = false;
            while (pos < len) {
                char d = peek();
                if (isDigit(d)) { pos++; continue; }
                if (d == '.' && !dot) { dot = true; pos++; continue; }
                break;
            }
            return new TLToken("NUMBER", src.substring(st, pos));
        }
        // identifier or keyword
        if (isIdentStart(c)) {
            int st = pos;
            pos++;
            while (pos < len && isIdentPart(peek())) pos++;
            String t = src.substring(st, pos);
            for (int i=0;i<keywords.length;i++) if (keywords[i].equals(t)) return new TLToken("KEYWORD", t);
            return new TLToken("IDENT", t);
        }
        // symbols: handle multi-char == ~= <= >=
        // two-char
        if (pos+1 < len) {
            String two = src.substring(pos, pos+2);
            if (two.equals("==") || two.equals("~=") || two.equals("<=") || two.equals(">=") ) { pos+=2; return new TLToken("SYMBOL", two); }
        }
        // single char symbols
        pos++;
        return new TLToken("SYMBOL", String.valueOf(c));
    }
}

/* === AST nodes === */
abstract class Node {}

abstract class Expr extends Node {
    abstract Object eval(Environment env);
}

abstract class Stmt extends Node {
    abstract Object execute(Environment env);
}

class NumberExpr extends Expr {
    public double value;
    public NumberExpr(double v) { this.value = v; }
    Object eval(Environment env) { return new Double(value); }
}

class StringExpr extends Expr {
    public String value;
    public StringExpr(String v) { this.value = v; }
    Object eval(Environment env) { return value; }
}

class NilExpr extends Expr { Object eval(Environment env) { return null; } }
class BoolExpr extends Expr { public boolean v; public BoolExpr(boolean b) { v = b; } Object eval(Environment env) { return new Boolean(v); } }

class VarExpr extends Expr {
    public String name; public VarExpr(String n) { name = n; }
    Object eval(Environment env) { return env.get(name); }
}

class BinaryExpr extends Expr {
    public String op; public Expr left, right; public BinaryExpr(String o, Expr a, Expr b){op=o;left=a;right=b;}
    Object eval(Environment env) {
        Object L = left.eval(env);
        Object R = right.eval(env);
        // handle nils
        if (L == null || R == null) {
            if (op.equals("==")) return new Boolean(L==R);
            if (op.equals("~=")) return new Boolean(L!=R);
            // other ops invalid on nil => return false/0
        }
        // numbers
        if (L instanceof Double && R instanceof Double) {
            double a = ((Double)L).doubleValue();
            double b = ((Double)R).doubleValue();
            if (op.equals("+")) return new Double(a+b);
            if (op.equals("-")) return new Double(a-b);
            if (op.equals("*")) return new Double(a*b);
            if (op.equals("/")) return new Double(a/b);
            if (op.equals("%")) return new Double(a % b);
            if (op.equals("<")) return new Boolean(a < b);
            if (op.equals(">")) return new Boolean(a > b);
            if (op.equals("<=")) return new Boolean(a <= b);
            if (op.equals(">=")) return new Boolean(a >= b);
            if (op.equals("==")) return new Boolean(a == b);
            if (op.equals("~=")) return new Boolean(a != b);
        }
        // equality for strings/booleans
        if (op.equals("==")) return new Boolean( (L==null?null:L).equals(R==null?null:R) );
        if (op.equals("~=")) return new Boolean( !(L==null?null:L).equals(R==null?null:R) );
        // string concatenation with .. is not implemented
        // logical and/or
        if (op.equals("and")) return new Boolean( truthy(L) && truthy(R) );
        if (op.equals("or")) return new Boolean( truthy(L) || truthy(R) );
        return null;
    }
    private boolean truthy(Object o) { if (o==null) return false; if (o instanceof Boolean) return ((Boolean)o).booleanValue(); return true; }
}

class CallExpr extends Expr {
    public Expr func; public Vector args;
    public CallExpr(Expr f, Vector a){ func=f; args=a; }
    Object eval(Environment env) {
        Object fn = func.eval(env);
        Vector evaluated = new Vector();
        for (int i=0;i<args.size();i++) evaluated.addElement( ((Expr)args.elementAt(i)).eval(env) );
        if (fn instanceof FunctionValue) {
            FunctionValue fv = (FunctionValue)fn;
            return fv.invoke(evaluated);
        }
        // builtin: allow string name referring to global function
        if (fn instanceof String) {
            Object g = env.get((String)fn);
            if (g instanceof FunctionValue) return ((FunctionValue)g).invoke(evaluated);
        }
        return null;
    }
}

/* === Statements === */
class AssignStmt extends Stmt {
    public String name; public Expr expr; public AssignStmt(String n, Expr e){name=n;expr=e;}
    Object execute(Environment env) { Object v = expr.eval(env); env.set(name, v); return null; }
}

class ReturnStmt extends Stmt {
    public Expr expr; public ReturnStmt(Expr e){expr=e;} Object execute(Environment env) { return new ReturnValue(expr==null?null:expr.eval(env)); }
}

class IfStmt extends Stmt {
    public Expr cond; public Vector thenBlock; public Vector elseBlock;
    public IfStmt(Expr c, Vector t, Vector e){cond=c;thenBlock=t;elseBlock=e;}
    Object execute(Environment env) {
        Object c = cond.eval(env);
        boolean t = truthy(c);
        Vector block = t ? thenBlock : elseBlock;
        if (block == null) return null;
        for (int i=0;i<block.size();i++) {
            Object r = ((Stmt)block.elementAt(i)).execute(env);
            if (r instanceof ReturnValue) return r;
        }
        return null;
    }
    private boolean truthy(Object o) { if (o==null) return false; if (o instanceof Boolean) return ((Boolean)o).booleanValue(); return true; }
}

class WhileStmt extends Stmt {
    public Expr cond; public Vector body;
    public WhileStmt(Expr c, Vector b){cond=c;body=b;}
    Object execute(Environment env) {
        while (truthy(cond.eval(env))) {
            for (int i=0;i<body.size();i++) {
                Object r = ((Stmt)body.elementAt(i)).execute(env);
                if (r instanceof ReturnValue) return r;
            }
        }
        return null;
    }
    private boolean truthy(Object o) { if (o==null) return false; if (o instanceof Boolean) return ((Boolean)o).booleanValue(); return true; }
}

class ExprStmt extends Stmt {
    public Expr e; public ExprStmt(Expr ex){e=ex;} Object execute(Environment env){ e.eval(env); return null; }
}

class FunctionDefStmt extends Stmt {
    public String name; public Vector params; public Vector body;
    public FunctionDefStmt(String n, Vector p, Vector b){name=n;params=p;body=b;}
    Object execute(Environment env) {
        FunctionValue fv = new FunctionValue(params, body, env);
        env.set(name, fv);
        return null;
    }
}

/* Special return carrier */
class ReturnValue { public Object value; public ReturnValue(Object v){value=v;} }

/* Function value */
class FunctionValue {
    public Vector params; public Vector body; public Environment closure;
    public FunctionValue(Vector p, Vector b, Environment c) { params=p; body=b; closure=c; }
    public Object invoke(Vector args) {
        Environment local = new Environment(closure);
        // bind params
        for (int i=0;i<params.size();i++) {
            String pname = (String)params.elementAt(i);
            Object aval = i < args.size() ? args.elementAt(i) : null;
            local.set(pname, aval);
        }
        // execute body
        for (int i=0;i<body.size();i++) {
            Object r = ((Stmt)body.elementAt(i)).execute(local);
            if (r instanceof ReturnValue) return ((ReturnValue)r).value;
        }
        return null;
    }
}

/* Environment (simple chain of Hashtables) */
class Environment {
    private Hashtable table; private Environment parent;
    public Environment(Environment p) { this.parent = p; this.table = new Hashtable(); }
    public Object get(String name) {
        Object v = table.get(name);
        if (v != null) return v;
        if (parent != null) return parent.get(name);
        return null;
    }
    public void set(String name, Object value) { table.put(name, value); }
}

/* === Parser (very small recursive-descent) === */
class TLParser {
    private TLLexer lex; private TLToken cur;
    public TLParser(String s) { this.lex = new TLLexer(s); this.cur = lex.nextToken(); }
    private void next() { cur = lex.nextToken(); }
    private boolean accept(String type, String text) { if (cur.type.equals(type) && (text==null || cur.text.equals(text))) { next(); return true; } return false; }
    private void expect(String type, String text) {
        if (!accept(type, text)) throw new RuntimeException("Parse error expected " + type + ":" + text + " got " + cur.type + ":" + cur.text);
    }

    public Vector parseChunk() {
        Vector stmts = new Vector();
        while (!cur.type.equals("EOF") ) {
            stmts.addElement(parseStatement());
        }
        return stmts;
    }

    private Stmt parseStatement() {
        if (cur.type.equals("KEYWORD")) {
            if (cur.text.equals("function")) return parseFunctionDef();
            if (cur.text.equals("if")) return parseIf();
            if (cur.text.equals("while")) return parseWhile();
            if (cur.text.equals("return")) { next(); Expr e = null; if (!cur.type.equals("KEYWORD") || !(cur.text.equals("end") || cur.text.equals("else") )) e = parseExpression(); return new ReturnStmt(e); }
        }
        // assignment or expression
        if (cur.type.equals("IDENT")) {
            String name = cur.text; next();
            if (accept("SYMBOL", "=")) {
                Expr e = parseExpression(); return new AssignStmt(name, e);
            } else {
                // function call as statement
                Expr func = new VarExpr(name);
                Vector args = new Vector();
                if (accept("SYMBOL","(")) {
                    if (!accept("SYMBOL", ")")) {
                        args.addElement(parseExpression());
                        while (accept("SYMBOL", ",")) args.addElement(parseExpression());
                        expect("SYMBOL", ")");
                    }
                }
                return new ExprStmt(new CallExpr(func,args));
            }
        }
        // plain expression
        Expr e = parseExpression(); return new ExprStmt(e);
    }

    private FunctionDefStmt parseFunctionDef() {
        // function name(params) body end
        expect("KEYWORD","function");
        String name = null;
        if (cur.type.equals("IDENT")) { name = cur.text; next(); }
        expect("SYMBOL","(");
        Vector params = new Vector();
        if (!accept("SYMBOL",")")) {
            if (cur.type.equals("IDENT")) { params.addElement(cur.text); next(); }
            while (accept("SYMBOL",",")) { if (cur.type.equals("IDENT")) { params.addElement(cur.text); next(); } }
            expect("SYMBOL",")");
        }
        Vector body = new Vector();
        while (!(cur.type.equals("KEYWORD") && cur.text.equals("end"))) {
            body.addElement(parseStatement());
        }
        expect("KEYWORD","end");
        return new FunctionDefStmt(name, params, body);
    }

    private IfStmt parseIf() {
        expect("KEYWORD","if"); Expr cond = parseExpression(); expect("KEYWORD","then");
        Vector thenB = new Vector(); Vector elseB = null;
        while (!(cur.type.equals("KEYWORD") && (cur.text.equals("else")||cur.text.equals("end")))) { thenB.addElement(parseStatement()); }
        if (accept("KEYWORD","else")) {
            elseB = new Vector();
            while (!(cur.type.equals("KEYWORD") && cur.text.equals("end"))) { elseB.addElement(parseStatement()); }
        }
        expect("KEYWORD","end");
        return new IfStmt(cond, thenB, elseB);
    }

    private WhileStmt parseWhile() {
        expect("KEYWORD","while"); Expr cond = parseExpression(); expect("KEYWORD","do");
        Vector body = new Vector();
        while (!(cur.type.equals("KEYWORD") && cur.text.equals("end"))) body.addElement(parseStatement());
        expect("KEYWORD","end");
        return new WhileStmt(cond, body);
    }

    // Expression parsing: implement precedence (very simple)
    private Expr parseExpression() { return parseOr(); }
    private Expr parseOr() {
        Expr e = parseAnd();
        while (cur.type.equals("KEYWORD") && cur.text.equals("or")) { String op = cur.text; next(); e = new BinaryExpr(op, e, parseAnd()); }
        return e;
    }
    private Expr parseAnd() { Expr e = parseComparison(); while (cur.type.equals("KEYWORD") && cur.text.equals("and")) { String op=cur.text; next(); e=new BinaryExpr(op,e,parseComparison()); } return e; }
    private Expr parseComparison() { Expr e = parseAdd(); while (cur.type.equals("SYMBOL") || cur.type.equals("KEYWORD")) {
        String t = cur.text; if (t.equals("==")||t.equals("~=")||t.equals("<")||t.equals(">")||t.equals("<=")||t.equals(">=")) { next(); e = new BinaryExpr(t, e, parseAdd()); } else break; } return e; }
    private Expr parseAdd() { Expr e = parseMul(); while (cur.type.equals("SYMBOL") && (cur.text.equals("+")||cur.text.equals("-"))) { String t=cur.text; next(); e = new BinaryExpr(t,e,parseMul()); } return e; }
    private Expr parseMul() { Expr e = parseUnary(); while (cur.type.equals("SYMBOL") && (cur.text.equals("*")||cur.text.equals("/")||cur.text.equals("%"))) { String t=cur.text; next(); e = new BinaryExpr(t,e,parseUnary()); } return e; }
    private Expr parseUnary() { if (cur.type.equals("SYMBOL") && cur.text.equals("-")) { next(); return new BinaryExpr("-", new NumberExpr(0), parseUnary()); } if (cur.type.equals("KEYWORD") && cur.text.equals("not")) { next(); return new BinaryExpr("and", new BoolExpr(false), parseUnary()); } return parsePrimary(); }
    private Expr parsePrimary() {
        if (cur.type.equals("NUMBER")) { double v = 0; try { v = Double.parseDouble(cur.text); } catch (Exception ex) { v = 0; } next(); return new NumberExpr(v); }
        if (cur.type.equals("STRING")) { String s = cur.text; next(); return new StringExpr(s); }
        if (cur.type.equals("KEYWORD") && cur.text.equals("true")) { next(); return new BoolExpr(true); }
        if (cur.type.equals("KEYWORD") && cur.text.equals("false")) { next(); return new BoolExpr(false); }
        if (cur.type.equals("KEYWORD") && cur.text.equals("nil")) { next(); return new NilExpr(); }
        if (cur.type.equals("IDENT")) {
            String name = cur.text; next(); // possible call
            Expr base = new VarExpr(name);
            if (accept("SYMBOL","(")) {
                Vector args = new Vector();
                if (!accept("SYMBOL",")")) {
                    args.addElement(parseExpression());
                    while (accept("SYMBOL",",")) args.addElement(parseExpression());
                    expect("SYMBOL",")");
                }
                return new CallExpr(base, args);
            }
            return base;
        }
        if (accept("SYMBOL","(")) { Expr e = parseExpression(); expect("SYMBOL",")"); return e; }
        throw new RuntimeException("Unexpected token " + cur.type + ":" + cur.text);
    }
}

/* === Interpreter / Runner === */
public class Lua {
    private Environment global;
    private StringItem console;
    private boolean root;

    public Lua(StringItem console, boolean root) {
        this.console = console; this.root = root;
        this.global = new Environment(null);
        registerBuiltins();
    }

    private void registerBuiltins() {
        // register print
        FunctionValue printFn = new FunctionValue(new Vector(), new Vector(), global) {
            public Object invoke(Vector args) {
                StringBuffer sb = new StringBuffer();
                for (int i=0;i<args.size();i++) {
                    if (i>0) sb.append('\t');
                    Object a = args.elementAt(i);
                    sb.append( a==null ? "nil" : a.toString() );
                }
                print(sb.toString());
                return null;
            }
        };
        global.set("print", printFn);
        // you can add more builtins here
    }

    public void run(String source) {
        try {
            TLParser p = new TLParser(source);
            Vector stmts = p.parseChunk();
            for (int i=0;i<stmts.size();i++) {
                Stmt s = (Stmt)stmts.elementAt(i);
                Object r = s.execute(global);
                if (r instanceof ReturnValue) {
                    // top-level return ignored
                }
            }
        } catch (Throwable t) {
            print("Lua Runtime error: " + t.toString());
        }
    }

    private void print(String text) { console.setText(console.getText().equals("") ? sb.toString() : console.getText() + "\n" + sb.toString()); }
}
