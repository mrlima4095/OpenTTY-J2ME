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
    private int uid = 1000, status = 0;
    public Hashtable globals = new Hashtable();
    public Hashtable father;
    private boolean kill = false;

    public C2ME(OpenTTY midlet, int uid, String pid, Process proc, Object stdout, Hashtable scope) {
        this.midlet = midlet; 
        this.uid = uid; 
        this.PID = pid; 
        this.proc = proc; 
        this.stdout = stdout; 
        this.father = scope;
    }
    
    public Hashtable run(String source, String code, Hashtable args) { 
        midlet.sys.put(PID, proc);
        globals.put("arg", args);
        
        Hashtable result = new Hashtable(); 
        
        try {
            if (code == null && source != null) {
                code = midlet.read(source, father);
            }
            
            if (code == null || code.trim().equals("")) {
                status = 1;
                result.put("status", new Integer(status));
                if (kill) midlet.sys.remove(PID);
                return result;
            }
            
            status = execute(code);
        } catch (Exception e) {
            midlet.print("C2ME Error: " + midlet.getCatch(e), stdout);
            status = 1;
        }
        
        if (kill) { midlet.sys.remove(PID); }
        result.put("status", new Integer(status));
        return result;
    }
    
    private int execute(String code) {
        Hashtable program = build(code);
        if (program == null) { 
            midlet.print("C2ME: build failed", stdout);
            return 1; 
        }

        Hashtable main = (Hashtable) program.get("main");
        int STATUS = 0;

        if (main == null) { 
            midlet.print("C2ME: main() missing", stdout);
            STATUS = 1;
        }
        else if (((String) main.get("type")).equals("int")) {
            try { 
                String result = executeFunction(PID, (Vector) main.get("source"), main, program, 0);
                STATUS = Integer.parseInt(result);
            } 
            catch (Exception e) { 
                midlet.print("C2ME: " + midlet.getCatch(e), stdout);
                STATUS = 1;
            }
        } 
        else { 
            midlet.print("C2ME: main() need to be an int function", stdout);
            STATUS = 2;
        }

        return STATUS;
    }
    
    private String executeFunction(String PID, Vector source, Hashtable context, Hashtable program, int mode) throws RuntimeException {
        Hashtable vars = (Hashtable) context.get("variables");
        if (vars == null) vars = new Hashtable();

        for (int i = 0; i < source.size(); i++) {
            Hashtable cmd = (Hashtable) source.elementAt(i);
            String type = (String) cmd.get("type");

            if (midlet.sys.get(PID) == null) {
                throw new RuntimeException("Process killed");
            }

            if (type == null) { }
            else if (type.equals("assign")) {
                String name = (String) cmd.get("name");
                String value = subst(PID, (String) cmd.get("value"), vars, program);
                String instance = (String) cmd.get("instance");
                Hashtable local = new Hashtable();

                if (instance == null) { 
                    if (vars.containsKey(name)) { 
                        instance = (String) ((Hashtable) vars.get(name)).get("instance"); 
                    } else { 
                        throw new RuntimeException("'" + name + "' undeclared"); 
                    } 
                } 
                
                if (instance.equals("int") && !validInt(value)) { 
                    throw new RuntimeException("invalid value for '" + name + "' (expected int)"); 
                } 
                if (instance.equals("char") && !validChar(value)) { 
                    value = "\"" + value + "\""; 
                } 

                local.put("value", value == null || value.length() == 0 ? "' '" : value); 
                local.put("instance", instance); 
                vars.put(name, local); 
            }
            else if (type.equals("return")) { 
                String retType = (String) context.get("type"); 
                String value = subst(PID, (String) cmd.get("value"), vars, program); 

                if (retType.equals("int")) { 
                    String expr = evaluateExpression(value); 
                    if (expr.startsWith("expr: ")) { 
                        throw new RuntimeException("invalid return value for function of type '" + retType + "'"); 
                    } else { 
                        return expr; 
                    } 
                } else { 
                    return formatValue(value); 
                } 
            } 
            else if (type.equals("if")) {
                String ret = null;
                if (evaluate(PID, (String) cmd.get("expr"), vars, program)) { 
                    ret = executeFunction(PID, (Vector) cmd.get("source"), context, program, mode); 
                } else if (cmd.containsKey("else")) { 
                    ret = executeFunction(PID, (Vector) cmd.get("else"), context, program, mode);
                }

                if (ret == null || ret.equals("' '") || ret.equals("0")) { 
                    continue;
                } else { 
                    return ret;
                }
            }
            else if (type.equals("while")) {
                while (evaluate(PID, (String) cmd.get("expr"), vars, program)) {
                    String ret = executeFunction(PID, (Vector) cmd.get("source"), context, program, 1);
                    
                    if (ret == null) { 
                        break;
                    } else if (ret.equals("+[continue]")) { 
                        continue;
                    } else { 
                        return ret;
                    }
                }
            }
            else if (type.equals("try")) {
                try {
                    String ret = executeFunction(PID, (Vector) cmd.get("source"), context, program, mode);
                    if (ret != null && !ret.equals("+[continue]")) return ret;
                } catch (Exception e) {
                    if (cmd.containsKey("catch")) {
                        String catchVar = cmd.containsKey("catchVar") ? (String) cmd.get("catchVar") : "";
                        String catchInstance = cmd.containsKey("catchInstance") ? (String) cmd.get("catchInstance") : "char";
                        String catchMsg = midlet.getCatch(e);
                        
                        Hashtable oldVar = null;
                        if (!catchVar.equals("")) {
                            if (vars.containsKey(catchVar)) { 
                                oldVar = (Hashtable) vars.get(catchVar); 
                            }
                            Hashtable newVar = new Hashtable();
                            newVar.put("value", catchInstance.equals("char") ? "\"" + escapeString(catchMsg) + "\"" : catchMsg);
                            newVar.put("instance", catchInstance);
                            vars.put(catchVar, newVar);
                        }

                        String ret = executeFunction(PID, (Vector) cmd.get("catch"), context, program, mode);

                        if (!catchVar.equals("") && oldVar != null) {
                            vars.put(catchVar, oldVar);
                        } else if (!catchVar.equals("")) {
                            vars.remove(catchVar);
                        }
                        
                        if (ret != null && !ret.equals("+[continue]")) return ret;
                    } else { 
                        throw e; 
                    }
                }
            }
            else if (type.equals("continue") || type.equals("break")) { 
                if (mode == 1) { 
                    return type.equals("break") ? null : "+[continue]"; 
                } else { 
                    throw new RuntimeException("not in a loop"); 
                } 
            } 
            else if (type.equals("call")) { 
                String args = cmd.containsKey("args") ? (String) cmd.get("args") : "";
                callFunction(PID, (String) cmd.get("function") + "(" + subst(PID, args, vars, program) + ")", vars, program); 
            }
        }

        if (mode == 0) {
            String retType = (String) context.get("type");
            return retType.equals("char") ? "' '" : "0";
        }
        return mode == 1 ? "+[continue]" : null; 
    }
    
    private String callFunction(String PID, String code, Hashtable vars, Hashtable program) throws RuntimeException {
        int parIndex = code.indexOf('(');
        if (parIndex == -1 || !code.endsWith(")")) { 
            return code; 
        }

        String fname = code.substring(0, parIndex).trim();
        String argsBlock = code.substring(parIndex + 1, code.length() - 1);
        String[] argList = argsBlock.equals("") ? new String[0] : splitArgs(argsBlock, ',');

        // Built-in functions
        if (fname.equals("print")) {
            if (argList.length < 1) { 
                throw new RuntimeException("function 'print' expects at least 1 argument"); 
            }
            String value = formatValue(subst(PID, argList[0], vars, program));
            midlet.print(value, stdout);
            return "0";
        }
        else if (fname.equals("read")) {
            if (argList.length != 1) { 
                throw new RuntimeException("function 'read' expects 1 argument"); 
            }
            return midlet.read(formatValue(subst(PID, argList[0], vars, program)), father);
        }
        else if (fname.equals("write")) {
            if (argList.length != 2) { 
                throw new RuntimeException("function 'write' expects 2 arguments"); 
            }
            String path = formatValue(subst(PID, argList[0], vars, program));
            String data = formatValue(subst(PID, argList[1], vars, program));
            int result = midlet.write(path, data, uid, father);
            return String.valueOf(result);
        }
        else if (fname.equals("exec")) {
            if (argList.length != 1) { 
                throw new RuntimeException("function 'exec' expects 1 argument"); 
            }
            String cmd = formatValue(subst(PID, argList[0], vars, program));
            // Execute command through OpenTTY
            midlet.print("[C2ME] exec: " + cmd, stdout);
            return "0";
        }
        else if (fname.equals("toString")) {
            if (argList.length != 1) { 
                throw new RuntimeException("function 'toString' expects 1 argument"); 
            }
            return formatValue(subst(PID, argList[0], vars, program));
        }
        else if (fname.equals("toInt")) {
            if (argList.length != 1) { 
                throw new RuntimeException("function 'toInt' expects 1 argument"); 
            }
            String val = formatValue(subst(PID, argList[0], vars, program));
            try {
                return String.valueOf(Integer.parseInt(val));
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        // User-defined function
        Hashtable functions = (Hashtable) program.get("functions");
        Hashtable fn = functions != null ? (Hashtable) functions.get(fname) : null;
        if (fn == null) { 
            throw new RuntimeException("function '" + fname + "' not found"); 
        }

        Hashtable newVars = new Hashtable();
        Vector reads = fn.containsKey("read") ? (Vector) fn.get("read") : null;

        if ((reads == null && argList.length > 0) || (reads != null && reads.size() != argList.length)) { 
            throw new RuntimeException("function '" + fname + "' expects " + (reads != null ? reads.size() : 0) + " argument(s), got " + argList.length); 
        }

        for (int j = 0; reads != null && j < reads.size(); j++) {
            Hashtable a = (Hashtable) reads.elementAt(j);
            String argName = (String) a.get("name");
            String argType = (String) a.get("type");

            String raw = (j < argList.length) ? argList[j].trim() : null;
            String value = (raw == null || raw.length() == 0) ? (argType.equals("char") ? "' '" : "0") : formatValue(subst(PID, raw, vars, program));

            if (argType.equals("int")) {
                value = evaluateExpression(value);
                if (value.startsWith("expr: ")) { 
                    throw new RuntimeException("invalid argument for '" + argName + "' - expected type 'int'"); 
                }
            }

            Hashtable local = new Hashtable();
            local.put("value", value);
            local.put("instance", argType);
            newVars.put(argName, local);
        }

        Hashtable newContext = new Hashtable();
        newContext.put("variables", newVars);
        newContext.put("type", fn.get("type"));
        newContext.put("source", fn.get("source"));

        return executeFunction(PID, (Vector) fn.get("source"), newContext, program, 3);
    }
    
    private String subst(String PID, String expr, Hashtable vars, Hashtable program) throws RuntimeException {
        if (expr == null || expr.length() == 0) { 
            return ""; 
        }

        // Variable substitution
        for (Enumeration e = vars.keys(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            Hashtable var = (Hashtable) vars.get(name);
            String value = (String) var.get("value");
            value = value == null || value.length() == 0 || value.equals("null") ? "" : formatValue(value);

            StringBuffer out = new StringBuffer();
            int i = 0;
            while (i < expr.length()) {
                if ((i == 0 || !isFuncChar(expr.charAt(i - 1))) &&
                    expr.startsWith(name, i) &&
                    (i + name.length() == expr.length() || !isFuncChar(expr.charAt(i + name.length())))) {
                    out.append(value);
                    i += name.length();
                } else { 
                    out.append(expr.charAt(i)); 
                    i++; 
                }
            }
            expr = out.toString();
        }

        if (validChar(expr)) { 
            return expr; 
        }

        // Function calls in expression
        while (true) {
            int open = expr.indexOf('(');
            if (open == -1) { break; }

            int i = open - 1;
            while (i >= 0) {
                char c = expr.charAt(i);
                if (!isFuncChar(c)) { break; }
                i--;
            }
            while (i >= 0 && expr.charAt(i) == ' ') { i--; }

            String name = expr.substring(i + 1, open).trim();

            int depth = 0, close = -1;
            for (int j = open; j < expr.length(); j++) {
                char c = expr.charAt(j);
                if (c == '(') { depth++; } 
                else if (c == ')') {
                    depth--;
                    if (depth == 0) { close = j; break; }
                }
            }
            if (close == -1) { 
                throw new RuntimeException("invalid expression - missing ')'"); 
            }

            String value = callFunction(PID, expr.substring(i + 1, close + 1), vars, program);
            expr = expr.substring(0, i + 1) + value + expr.substring(close + 1);
        }

        String result = evaluateExpression(expr);
        return result.startsWith("expr: ") ? expr : result;
    }
    
    private String formatValue(String expr) { 
        if (expr == null || expr.length() == 0) { 
            return ""; 
        }
        if (validChar(expr)) { 
            return midlet.env(expr.substring(1, expr.length() - 1), father); 
        }
        return midlet.env(expr, father); 
    }
    
    private boolean evaluate(String PID, String expr, Hashtable vars, Hashtable program) { 
        String[] ops = {">=", "<=", "==", "!=", ">", "<", "startswith", "endswith", "contains"}; 

        for (int i = 0; i < ops.length; i++) { 
            String op = ops[i]; 
            int idx = expr.indexOf(op); 
            if (idx != -1) { 
                String left = formatValue(subst(PID, expr.substring(0, idx).trim(), vars, program));
                String right = formatValue(subst(PID, expr.substring(idx + op.length()).trim(), vars, program));
                Double a = getNumber(left);
                Double b = getNumber(right);

                if (a != null && b != null) { 
                    if (op.equals(">")) return a.doubleValue() > b.doubleValue();
                    if (op.equals("<")) return a.doubleValue() < b.doubleValue();
                    if (op.equals(">=")) return a.doubleValue() >= b.doubleValue();
                    if (op.equals("<=")) return a.doubleValue() <= b.doubleValue();
                    if (op.equals("==")) return a.doubleValue() == b.doubleValue();
                    if (op.equals("!=")) return a.doubleValue() != b.doubleValue();
                } else { 
                    if (op.equals("==")) return left.equals(right);
                    if (op.equals("!=")) return !left.equals(right);
                    if (op.equals("endswith")) return left.endsWith(right);
                    if (op.equals("startswith")) return left.startsWith(right);
                    if (op.equals("contains")) return left.indexOf(right) != -1;
                }
            } 
        } 

        expr = expr.trim(); 
        if (expr.equals("0") || expr.equals("") || expr.equals("' '") || expr.equals("\"\"")) { 
            return false; 
        }
        return true; 
    }
    
    private String evaluateExpression(String expr) {
        expr = expr.trim();
        
        // Simple arithmetic
        try {
            // Addition
            int plusIdx = expr.lastIndexOf('+');
            if (plusIdx > 0) {
                String left = evaluateExpression(expr.substring(0, plusIdx));
                String right = evaluateExpression(expr.substring(plusIdx + 1));
                double l = Double.parseDouble(left);
                double r = Double.parseDouble(right);
                return String.valueOf(l + r);
            }
            
            // Subtraction
            int minusIdx = expr.lastIndexOf('-');
            if (minusIdx > 0) {
                String left = evaluateExpression(expr.substring(0, minusIdx));
                String right = evaluateExpression(expr.substring(minusIdx + 1));
                double l = Double.parseDouble(left);
                double r = Double.parseDouble(right);
                return String.valueOf(l - r);
            }
            
            // Multiplication
            int mulIdx = expr.indexOf('*');
            if (mulIdx > 0) {
                String left = evaluateExpression(expr.substring(0, mulIdx));
                String right = evaluateExpression(expr.substring(mulIdx + 1));
                double l = Double.parseDouble(left);
                double r = Double.parseDouble(right);
                return String.valueOf(l * r);
            }
            
            // Division
            int divIdx = expr.indexOf('/');
            if (divIdx > 0) {
                String left = evaluateExpression(expr.substring(0, divIdx));
                String right = evaluateExpression(expr.substring(divIdx + 1));
                double l = Double.parseDouble(left);
                double r = Double.parseDouble(right);
                if (r == 0) return "expr: division by zero";
                return String.valueOf(l / r);
            }
            
            return expr;
        } catch (NumberFormatException e) {
            return "expr: " + expr;
        }
    }
    
    private boolean validInt(String expr) { 
        try {
            Double.parseDouble(evaluateExpression(expr));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private boolean validChar(String expr) { 
        return (expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'")); 
    }
    
    private Double getNumber(String s) {
        try {
            return new Double(Double.parseDouble(s));
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private String escapeString(String s) {
        if (s == null) return "";
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else sb.append(c);
        }
        return sb.toString();
    }
    
    private boolean isFuncChar(char c) { 
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'; 
    }
    
    private String[] splitArgs(String text, char separator) {
        Vector parts = new Vector();
        int depth = 0, start = 0;
        boolean inString = false;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i-1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '(') depth++;
                else if (c == ')') depth--;
                else if (c == separator && depth == 0) {
                    String part = text.substring(start, i).trim();
                    if (part.length() > 0) parts.addElement(part);
                    start = i + 1;
                }
            }
        }
        
        String lastPart = text.substring(start).trim();
        if (lastPart.length() > 0) parts.addElement(lastPart);
        
        String[] result = new String[parts.size()];
        parts.copyInto(result);
        return result;
    }
    
    // ----------------------------------------------------------------------------
    // Parser - Build AST from C-like source code
    // ----------------------------------------------------------------------------
    
    private Hashtable build(String source) {
        // Remove comments
        source = removeComments(source);
        source = source.trim();
        if (source.equals("")) { return null; }

        Hashtable program = new Hashtable();
        Hashtable functions = new Hashtable();
        program.put("functions", functions);

        // Parse functions
        while (true) {
            int start = findFunctionStart(source);
            if (start == -1) { break; }

            int p1 = source.indexOf("(", start);
            int p2 = source.indexOf(")", p1);
            int b1 = source.indexOf("{", p2);

            String type = source.substring(start, source.indexOf(" ", start)).trim();
            String name = source.substring(start + type.length(), p1).trim();
            String params = source.substring(p1 + 1, p2).trim();
            String block = getBlock(source.substring(b1));

            if (block == null) { 
                midlet.print("build: invalid block", stdout);
                return null; 
            }

            source = source.substring(b1 + block.length()).trim();

            Hashtable fn = new Hashtable();
            fn.put("type", type);

            Vector reads = new Vector();
            if (!params.equals("")) {
                String[] paramList = splitArgs(params, ',');
                for (int i = 0; i < paramList.length; i++) {
                    String param = paramList[i].trim();
                    int spaceIdx = param.indexOf(' ');
                    if (spaceIdx == -1) { 
                        midlet.print("build: invalid parameter", stdout);
                        return null; 
                    }
                    Hashtable arg = new Hashtable();
                    arg.put("type", param.substring(0, spaceIdx));
                    arg.put("name", param.substring(spaceIdx + 1));
                    reads.addElement(arg);
                }
            }

            if (!reads.isEmpty()) { fn.put("read", reads); }

            block = block.substring(1, block.length() - 1).trim();
            fn.put("variables", new Hashtable());
            fn.put("source", parseBlock(block, fn));

            if (name.equals("main")) { 
                program.put("main", fn); 
            } else { 
                functions.put(name, fn); 
            }
        }

        return program;
    }
    
    private int findFunctionStart(String source) {
        String[] types = { "int", "char", "void" };
        for (int i = 0; i < source.length(); i++) {
            for (int t = 0; t < types.length; t++) {
                String type = types[t];
                if (source.startsWith(type + " ", i)) {
                    int nameStart = i + type.length() + 1;
                    int p1 = source.indexOf('(', nameStart);
                    if (p1 == -1) continue;
                    
                    String maybeName = source.substring(nameStart, p1).trim();
                    if (maybeName.indexOf(' ') != -1) continue;
                    
                    return i;
                }
            }
        }
        return -1;
    }
    
    private Vector parseBlock(String block, Hashtable context) {
        Vector source = new Vector();
        String[] lines = splitBlock(block, ';');

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.equals("") || line.equals("' '")) continue;
            
            Hashtable cmd = new Hashtable();

            if (line.startsWith("return")) { 
                cmd.put("type", "return"); 
                cmd.put("value", line.substring(6).trim()); 
            }
            else if (line.startsWith("if")) {
                cmd.put("type", "if");
                cmd.put("expr", extractParens(line));
                
                int braceStart = line.indexOf('{');
                if (braceStart != -1) {
                    String subblock = getBlock(line.substring(braceStart));
                    if (subblock != null) {
                        cmd.put("source", parseBlock(subblock.substring(1, subblock.length() - 1).trim(), context));
                    }
                }
                
                int elseIdx = line.indexOf("else");
                if (elseIdx != -1) {
                    int elseBrace = line.indexOf('{', elseIdx);
                    if (elseBrace != -1) {
                        String elseBlock = getBlock(line.substring(elseBrace));
                        if (elseBlock != null) {
                            cmd.put("else", parseBlock(elseBlock.substring(1, elseBlock.length() - 1).trim(), context));
                        }
                    }
                }
            }
            else if (line.startsWith("while")) {
                cmd.put("type", "while");
                cmd.put("expr", extractParens(line));
                
                int braceStart = line.indexOf('{');
                if (braceStart != -1) {
                    String subblock = getBlock(line.substring(braceStart));
                    if (subblock != null) {
                        cmd.put("source", parseBlock(subblock.substring(1, subblock.length() - 1).trim(), context));
                    }
                }
            }
            else if (line.equals("break") || line.equals("continue")) { 
                cmd.put("type", line); 
            }
            else if (line.indexOf('(') != -1 && line.indexOf('=') == -1 && !line.startsWith("int ") && !line.startsWith("char ")) {
                cmd.put("type", "call");
                cmd.put("function", line.substring(0, line.indexOf('(')).trim());
                cmd.put("args", extractBetween(line, '(', ')'));
            }
            else if (line.indexOf('=') != -1) {
                String[] parts = splitFirst(line, '=');
                if (parts.length == 2) {
                    String varName = parts[0].trim();
                    String value = parts[1].trim();
                    
                    // Check if it's a declaration
                    if (varName.startsWith("int ")) {
                        varName = varName.substring(4).trim();
                        cmd.put("instance", "int");
                    } else if (varName.startsWith("char ")) {
                        varName = varName.substring(5).trim();
                        cmd.put("instance", "char");
                    }
                    
                    cmd.put("type", "assign");
                    cmd.put("name", varName);
                    cmd.put("value", value);
                }
            }
            
            if (cmd.size() > 0) {
                source.addElement(cmd);
            }
        }

        return source;
    }
    
    private String removeComments(String source) {
        // Remove // comments
        StringBuffer result = new StringBuffer();
        int i = 0;
        boolean inString = false;
        
        while (i < source.length()) {
            char c = source.charAt(i);
            
            if (c == '"' && (i == 0 || source.charAt(i-1) != '\\')) {
                inString = !inString;
                result.append(c);
                i++;
                continue;
            }
            
            if (!inString && c == '/' && i + 1 < source.length()) {
                if (source.charAt(i+1) == '/') {
                    int end = source.indexOf('\n', i);
                    if (end == -1) end = source.length();
                    i = end;
                    continue;
                } else if (source.charAt(i+1) == '*') {
                    int end = source.indexOf("*/", i + 2);
                    if (end == -1) end = source.length();
                    i = end + 2;
                    continue;
                }
            }
            
            result.append(c);
            i++;
        }
        
        return result.toString();
    }
    
    private String getBlock(String code) { 
        int depth = 0; 
        for (int i = 0; i < code.length(); i++) { 
            char c = code.charAt(i); 
            if (c == '{') { 
                depth++; 
            } else if (c == '}') { 
                depth--; 
                if (depth == 0) { 
                    return code.substring(0, i + 1); 
                } 
            } 
        } 
        return null; 
    }
    
    private String extractParens(String code) { 
        int start = code.indexOf('('); 
        if (start == -1) return ""; 
        int depth = 0; 
        for (int i = start; i < code.length(); i++) { 
            char c = code.charAt(i); 
            if (c == '(') depth++; 
            else if (c == ')') { 
                depth--; 
                if (depth == 0) { 
                    return code.substring(start + 1, i).trim(); 
                } 
            } 
        } 
        return ""; 
    }
    
    private String extractBetween(String text, char open, char close) { 
        int start = text.indexOf(open); 
        int end = text.lastIndexOf(close); 
        if (start == -1 || end == -1 || end <= start) return ""; 
        return text.substring(start + 1, end).trim(); 
    }
    
    private String[] splitBlock(String code, char separator) { 
        Vector parts = new Vector(); 
        int depthPar = 0, depthBrace = 0, start = 0; 
        boolean inString = false; 
        
        for (int i = 0; i < code.length(); i++) { 
            char c = code.charAt(i); 
            if (c == '"' && (i == 0 || code.charAt(i-1) != '\\')) { 
                inString = !inString; 
            } else if (!inString) { 
                if (c == '(') depthPar++; 
                else if (c == ')') depthPar--; 
                else if (c == '{') depthBrace++; 
                else if (c == '}') depthBrace--; 
                else if (c == separator && depthPar == 0 && depthBrace == 0) { 
                    String part = code.substring(start, i).trim(); 
                    if (part.length() > 0) parts.addElement(part); 
                    start = i + 1; 
                } 
            } 
        } 
        
        String lastPart = code.substring(start).trim(); 
        if (lastPart.length() > 0) parts.addElement(lastPart); 
        
        String[] result = new String[parts.size()]; 
        parts.copyInto(result); 
        return result; 
    }
    
    private String[] splitFirst(String text, char separator) {
        int idx = text.indexOf(separator);
        if (idx == -1) return new String[] { text };
        return new String[] { text.substring(0, idx), text.substring(idx + 1) };
    }
}