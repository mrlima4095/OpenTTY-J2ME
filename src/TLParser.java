import java.io.*;

public class TLParser {
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
            // skip stray semicolons as empty statements
            if (cur.type.equals("SYMBOL") && cur.text.equals(";")) { next(); continue; }
            Stmt s = parseStatement();
            // allow optional semicolon after statement
            if (cur.type.equals("SYMBOL") && cur.text.equals(";")) next();
            stmts.addElement(s);
        }
        return stmts;
    }

    private Stmt parseStatement() {
        // handle empty semicolon
        if (cur.type.equals("SYMBOL") && cur.text.equals(";")) { next(); return new ExprStmt(new NilExpr()); }

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
                Expr e = parseExpression();
                // optional semicolon will be consumed by caller
                return new AssignStmt(name, e);
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
    private Expr parseUnary() { 
        if (cur.type.equals("SYMBOL") && cur.text.equals("-")) { 
            next(); 

            return new BinaryExpr("-", new NumberExpr(0), parseUnary()); 
        } 
        // Em TLParser.java, método parseUnary()
        if (cur.type.equals("KEYWORD") && cur.text.equals("not")) {
            next();
            // Use um operador unário "not", o segundo operando pode ser nulo
            return new BinaryExpr("not", parseUnary(), null);
        }

        return parsePrimary(); 
    }
    private Expr parsePrimary() {
        if (cur.type.equals("NUMBER")) { double v = 0; try { v = Double.valueOf(cur.text).doubleValue(); } catch (Exception ex) { v = 0; } next(); return new NumberExpr(v); }
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