private int C2ME(String source, String code, boolean root) {
        Hashtable program = build(code == null ? getcontent(source) : code);
        if (program == null) { return 1; }

        Hashtable main = (Hashtable) program.get("main"), proc = genprocess("build " + basename(source), root, null);
        String PID = genpid(); int STATUS = 0;

        trace.put(PID, proc);

        if (main == null) { echoCommand("C2ME: main() missing"); STATUS = 1; }
        else if (((String) main.get("type")).equals("int")) {
            try { STATUS = Integer.valueOf(C2ME(PID, (Vector) main.get("source"), main, program, root, 0)); } 
            catch (Exception e) { echoCommand("C2ME: " + getCatch(e)); STATUS = 1; }
        } 
        else { echoCommand("C2ME: main() need to be an int function"); STATUS = 2; }

        trace.remove(PID);
        return STATUS;
    }
    private String C2ME(String PID, Vector source, Hashtable context, Hashtable program, boolean root, int mode) throws RuntimeException {
        Hashtable vars = (Hashtable) context.get("variables");

        for (int i = 0; i < source.size(); i++) {
            Hashtable cmd = (Hashtable) source.elementAt(i);
            String type = (String) cmd.get("type");

            if (trace.containsKey(PID)) { }
            else { throw new RuntimeException("Process killed"); }

            if (type == null) { }
            else if (type.equals("assign")) {
                String name = (String) cmd.get("name"), value = subst(PID, (String) cmd.get("value"), vars, program, root), instance = (String) cmd.get("instance");
                Hashtable local = new Hashtable();

                if (instance == null) { 
                    if (vars.containsKey(name)) { instance = (String) ((Hashtable) vars.get(name)).get("instance"); } 
                    else { throw new RuntimeException("'" + name + "' undeclared"); } 
                } 
                if (instance.equals("int") && !validInt(value)) { throw new RuntimeException("invalid value for '" + name + "' (expected int)"); } 
                if (instance.equals("char") && !validChar(value)) { value = "\"" + value + "\""; } 

                local.put("value", value == null || value.length() == 0 ? "' '" : value); 
                local.put("instance", instance); 
                vars.put(name, local); 
            }
            else if (type.equals("return")) { 
                type = (String) context.get("type"); 
                String value = subst(PID, (String) cmd.get("value"), vars, program, root); 

                if (type.equals("int")) { 
                    String expr = exprCommand(value); 

                    if (expr.startsWith("expr: ")) { throw new RuntimeException("invalid return value for function of type '" + type + "'"); } 
                    else { return expr; } 
                } 
                else { return value; } 
            } 
            else if (type.equals("if")) {
                String ret = null;
                if (eval(PID, (String) cmd.get("expr"), vars, program, root)) { ret = C2ME(PID, (Vector) cmd.get("source"), context, program, root, mode); } 
                else if (cmd.containsKey("else")) { ret = C2ME(PID, (Vector) cmd.get("else"), context, program, root, mode); }

                if (ret.equals("' '") || ret.equals("0") || ret == null) { continue; }
                else { return ret; }
            }
            else if (type.equals("while")) {
                
                while (eval(PID, subst(PID, (String) cmd.get("expr"), vars, program, root), vars, program, root)) {
                    String ret = C2ME(PID, (Vector) cmd.get("source"), context, program, root, 1);
                    
                    if (ret == null) { break; }
                    else if (ret.equals("+[continue]")) { continue; }
                    else { return ret; }
                }
            }
            else if (type.equals("try")) {
                String ret = null;
                
                try {
                    ret = C2ME(PID, (Vector) cmd.get("source"), context, program, root, mode);
                } catch (Exception e) {
                    if (cmd.containsKey("catch")) {
                        String catchVar = cmd.containsKey("catchVar") ? (String) cmd.get("catchVar") : "";
                        String catchInstance = cmd.containsKey("catchInstance") ? (String) cmd.get("catchInstance") : "char";
                        String catchMsg = getCatch(e);
                        String catchValue;
                        
                        if ("char".equals(catchInstance)) {
                            catchMsg = catchMsg == null ? "" : replace(catchMsg, "\"", "\\\"");
                            catchValue = "\"" + catchMsg + "\"";
                        } else { 
                            catchValue = catchMsg == null ? "0" : catchMsg;
                        }

                        Hashtable oldVar = null;
                        if (!catchVar.equals("")) {
                            if (vars.containsKey(catchVar)) { oldVar = (Hashtable) vars.get(catchVar); }
                            Hashtable newVar = new Hashtable();
                            newVar.put("value", catchValue);
                            newVar.put("instance", catchInstance);
                            vars.put(catchVar, newVar);
                        }

                        ret = C2ME(PID, (Vector) cmd.get("catch"), context, program, root, mode);

                        if (!catchVar.equals("")) {
                            if (oldVar != null) { vars.put(catchVar, oldVar); }
                            else { vars.remove(catchVar); }
                        }
                    } else { throw e; }
                }
            }

            else if (type.equals("continue") || type.equals("break")) { 
                if (mode == 1) { return type.equals("break") ? null : "+[continue]"; } 
                else { throw new RuntimeException("not in a loop"); } 
            } 
            else if (type.equals("call")) { C2ME(PID, (String) cmd.get("function") + "(" + subst(PID, (cmd.containsKey("args") ? (String) cmd.get("args") : ""), vars, program, root) + ")", vars, program, root); }
        }

        return mode == 0 ? (((String) context.get("type")).equals("char") ? "' '" : "0") : mode == 1 ? "+[continue]" : null; 
    }
    private String C2ME(String PID, String code, Hashtable vars, Hashtable program, boolean root) throws RuntimeException {
        int parIndex = code.indexOf('(');
        if (parIndex == -1 || !code.endsWith(")")) { return code; }

        String fname = code.substring(0, parIndex).trim(), argsBlock = code.substring(parIndex + 1, code.length() - 1);
        String[] argList = argsBlock.equals("") ? new String[0] : splitBlock(argsBlock, ',');

        if (fname.equals("")) { return "0"; } 
        else if (fname.equals("printf")) {
            if (argList.length < 1 || argList.length > 2) { throw new RuntimeException("function 'printf' expects 1 argument(s), got " + argList.length); } 
            else { 
                String value = C2ME_format(subst(PID, argList[0], vars, program, root)); 
                int STATUS = 0;
                
                if (argList.length == 2) {
                    if (argList[1].equals("stdin")) { stdin.setString(value); } 
                    else if (argList[1].equals("stdout")) { stdout.setText(stdout.getText() + value); } 
                    else if (argList[1].equals("nano")) { STATUS = processCommand("add " + value, false, root); } 
                    else {
                        String content = getcontent(argList[1]);
                        STATUS = writeRMS(argList[1], content.equals("") ? value : content + "\n" + value);
                    }
                } 
                else { echoCommand(value); }
                
                return String.valueOf(STATUS); 
            }
        }
        else if (fname.equals("readf")) {
            if (argList.length != 1) { throw new RuntimeException("function '" + fname + "' expects 1 argument(s), got " + argList.length); } 
            else { return getcontent(C2ME_format(subst(PID, argList[0], vars, program, root))); }
        }
        else if (fname.equals("exec")) {
            if (argList.length != 1) { throw new RuntimeException("function '" + fname + "' expects 1 argument(s), got " + argList.length); } 
            else { return String.valueOf(processCommand(C2ME_format(subst(PID, argList[0], vars, program, root)), true, root)); }
        }
        else if (fname.equals("throw")) {
            if (argList.length != 1) { throw new RuntimeException("function '" + fname + "' expects 1 argument(s), got " + argList.length); } 
            else { throw new RuntimeException(C2ME_format(subst(PID, argList[0], vars, program, root))); }
        }

        Hashtable fn = (Hashtable) ((Hashtable) program.get("functions")).get(fname);
        if (fn == null) { throw new RuntimeException("function '" + fname + "' not found"); }

        Hashtable newVars = new Hashtable();
        Vector reads = fn.containsKey("read") ? (Vector) fn.get("read") : null;

        if ((reads == null && argList.length > 0) || (reads != null && reads.size() != argList.length)) { throw new RuntimeException("function '" + fname + "' expects " + (reads != null ? reads.size() : 0) + " argument(s), got " + argList.length); }

        for (int j = 0; reads != null && j < reads.size(); j++) {
            Hashtable a = (Hashtable) reads.elementAt(j);
            String argName = (String) a.get("name"), argType = (String) a.get("type");

            String raw = (j < argList.length) ? argList[j].trim() : null, value = (raw == null || raw.length() == 0) ? (argType.equals("char") ? "' '" : "0") : C2ME_format(subst(PID, raw, vars, program, root));

            if (argType.equals("int")) {
                value = exprCommand(value);

                if (value.startsWith("expr: ")) { throw new RuntimeException("invalid argument for '" + argName + "' - expected type 'int'"); }
            }

            Hashtable local = new Hashtable();
            local.put("value", value); local.put("instance", argType);
            newVars.put(argName, local);
        }

        Hashtable newContext = new Hashtable();
        newContext.put("variables", newVars);
        newContext.put("type", fn.get("type"));
        newContext.put("source", fn.get("source"));

        return C2ME(PID, (Vector) fn.get("source"), newContext, program, root, 3);
    }
    private String subst(String PID, String expr, Hashtable vars, Hashtable program, boolean root) throws RuntimeException {
        if (expr == null || expr.length() == 0) { return ""; }

        for (Enumeration e = vars.keys(); e.hasMoreElements();) {
            String name = (String) e.nextElement(), value = (String) ((Hashtable) vars.get(name)).get("value");
            value = value == null || value.length() == 0 || value.equals("null") ? "" : C2ME_format(value);

            if (validChar(expr)) { expr = replace(expr, "%" + name, value.equals("' '") ? "" : value); }
            else {
                StringBuffer out = new StringBuffer();
                int i = 0;
                while (i < expr.length()) {
                    if ((i == 0 || !isFuncChar(expr.charAt(i - 1))) &&
                        expr.startsWith(name, i) &&
                        (i + name.length() == expr.length() || !isFuncChar(expr.charAt(i + name.length())))) {
                        out.append(value);
                        i += name.length();
                    } 
                    else { out.append(expr.charAt(i)); i++; }
                }
                expr = out.toString();
            }
        }

        if (validChar(expr)) { return expr; }

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
            if (close == -1) { throw new RuntimeException("invalid expression - missing ')'"); }

            String value = C2ME(PID, expr.substring(i + 1, close + 1), vars, program, root);

            expr = expr.substring(0, i + 1) + value + expr.substring(close + 1);
        }

        String result = exprCommand(expr);
        return result.startsWith("expr: ") ? expr : result;
    }
    private String C2ME_format(String expr) { if (expr == null || expr.length() == 0) { return "' '"; } if (validChar(expr)) { return env(expr.substring(1, expr.length() - 1)); } return env(expr); }
    private boolean eval(String PID, String expr, Hashtable vars, Hashtable program, boolean root) { 
        String[] ops = {">=", "<=", "==", "!=", ">", "<", "startswith", "endswith", "contains"}; 

        for (int i = 0; i < ops.length; i++) { 
            String op = ops[i]; 
            int idx = expr.indexOf(op); 
            if (idx != -1) { 
                String left = C2ME_format(subst(PID, expr.substring(0, idx).trim(), vars, program, root)), right = C2ME_format(subst(PID, expr.substring(idx + op.length()).trim(), vars, program, root)); 
                Double a = getNumber(left), b = getNumber(right); 

                if (a != null && b != null) { 
                    if (op.equals(">")) { return a > b; } 
                    if (op.equals("<")) { return a < b; } 
                    if (op.equals(">=")) { return a >= b; } 
                    if (op.equals("<=")) { return a <= b; } 
                    if (op.equals("==")) { return a.doubleValue() == b.doubleValue(); } 
                    if (op.equals("!=")) { return a.doubleValue() != b.doubleValue(); } 
                } 
                else { 
                    if (op.equals("==")) { return left.equals(right); } 
                    if (op.equals("!=")) { return !left.equals(right); } 
                    if (op.equals("endswith")) { return left.endsWith(right); } 
                    if (op.equals("startswith")) { return left.startsWith(right); } 
                    if (op.equals("contains")) { return left.indexOf(right) != -1; } 
                } 
            } 
        } 

        expr = expr.trim(); 
        if (expr.equals("0") || expr.equals("") || expr.equals("' '") || expr.equals("\"\"")) { return false; } 

        return true; 
    }
    private boolean validInt(String expr) { return exprCommand(expr).startsWith("expr: ") ? false : true; }
    private boolean validChar(String expr) { return (expr.startsWith("\"") && expr.endsWith("\"")) || (expr.startsWith("'") && expr.endsWith("'")); }
    // | (Building)
    private Hashtable build(String source) {
        Hashtable program = new Hashtable();

        while (true) { int idx = source.indexOf("//"); if (idx == -1) { break; } int endl = source.indexOf("\n", idx); if (endl == -1) { endl = source.length(); } source = source.substring(0, idx) + source.substring(endl); } 
        while (true) { int start = source.indexOf("/*"); if (start == -1) { break; } int end = source.indexOf("*/", start + 2); if (end == -1) { source = source.substring(0, start); break; } source = source.substring(0, start) + source.substring(end + 2); }

        source = source.trim();
        if (source.equals("")) { return null; }

        Hashtable functions = new Hashtable();
        program.put("functions", functions);

        while (source.startsWith("#include")) {
            int endl = source.indexOf('\n');
            if (endl == -1) { echoCommand("build: invalid including"); return null; }

            String line = source.substring(0, endl).trim();
            source = source.substring(endl).trim();

            if (line.startsWith("#include \"") && line.endsWith("\"")) {
                String file = extractBetween(line, '"', '"');
                Hashtable imported = build(getcontent(file));
                if (imported == null) { echoCommand("build: failed to include: " + file); return null; }

                Hashtable importedFunctions = (Hashtable) imported.get("functions");
                for (Enumeration e = importedFunctions.keys(); e.hasMoreElements();) {
                    String k = (String) e.nextElement();
                    if (!functions.containsKey(k)) { functions.put(k, importedFunctions.get(k)); }
                }
            } else { echoCommand("build: invalid include syntax"); return null; }
        }

        while (true) {
            int start = -1;
            String[] types = { "int", "char" };

            for (int i = 0; i < source.length(); i++) {
                for (int t = 0; t < types.length; t++) {
                    String type = types[t];

                    if (source.startsWith(type + " ", i)) {
                        int nameStart = i + type.length() + 1;
                        int p1 = source.indexOf('(', nameStart);
                        if (p1 == -1) { continue; }

                        int p2 = source.indexOf(')', p1);
                        if (p2 == -1) { continue; }

                        int brace = source.indexOf('{', p2);
                        if (brace == -1) { continue; }

                        String maybeName = source.substring(nameStart, p1).trim();
                        if (maybeName.indexOf(' ') != -1) { continue; }

                        String beforeBrace = source.substring(p2 + 1, brace).trim();
                        if (beforeBrace.length() > 0) { continue; }

                        start = i;
                        break;
                    }
                }
                if (start != -1) { break; }
            }

            if (start == -1) { break; }

            int p1 = source.indexOf("(", start), p2 = source.indexOf(")", p1), b1 = source.indexOf("{", p2);

            String type = source.substring(start, start + source.substring(start).indexOf(" ")).trim(), name = source.substring(start + type.length(), p1).trim(), params = extractBetween(source.substring(p1, p2 + 1), '(', ')'), block = getBlock(source.substring(b1));

            if (block == null) { echoCommand("build: invalid block"); return null; }

            source = source.substring(b1 + block.length()).trim();

            Hashtable fn = new Hashtable();
            fn.put("type", type);

            Vector reads = new Vector();
            if (!params.equals("")) {
                String[] paramList = split(params, ',');
                for (int i = 0; i < paramList.length; i++) {
                    String param = paramList[i].trim();
                    String[] parts = split(param, ' ');
                    if (parts.length != 2) { echoCommand("build: invalid reading"); return null; }

                    Hashtable arg = new Hashtable();
                    arg.put("type", parts[0]);
                    arg.put("name", parts[1]);
                    reads.addElement(arg);
                }
            }

            if (!reads.isEmpty()) { fn.put("read", reads); }

            block = block.substring(1, block.length() - 1).trim();
            
            fn.put("variables", new Hashtable());
            fn.put("source", parseBlock(block, fn));

            if (name.equals("main")) { program.put("main", fn); } 
            else { functions.put(name, fn); }
        }

        return program;
    }
    private Vector parseBlock(String block, Hashtable context) {
        Vector source = new Vector();
        String[] lines = splitBlock(block, ';');

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            Hashtable cmd = new Hashtable();

            if (line.equals("") || line.equals("' '")) { }
            else if (line.startsWith("return")) { cmd.put("type", "return"); cmd.put("value", line.substring(7).trim()); }
            else if (line.startsWith("try")) {
                int lineIndexInBlock = block.indexOf(line);
                if (lineIndexInBlock == -1) { echoCommand("build: 'try' token not found"); return null; }

                int braceIndex = -1;
                for (int j = lineIndexInBlock; j < block.length(); j++) { if (block.charAt(j) == '{') { braceIndex = j; break; } }
                if (braceIndex == -1) { echoCommand("build: 'try' block opening '{' not found"); return null; }

                String remaining = block.substring(braceIndex), trySub = getBlock(remaining);
                if (trySub == null) { echoCommand("build: 'try' block not properly closed"); return null; }

                cmd.put("type", "try"); 
                cmd.put("source", parseBlock(trySub.substring(1, trySub.length() - 1).trim(), context));

                int afterTryIndex = braceIndex + trySub.length(), catchIndex = block.indexOf("catch", afterTryIndex);
                if (catchIndex != -1) {
                    int parenStart = block.indexOf('(', catchIndex), parenEnd = (parenStart == -1) ? -1 : block.indexOf(')', parenStart);
                    String catchInside = "";
                    if (parenStart != -1 && parenEnd != -1) { catchInside = block.substring(parenStart + 1, parenEnd).trim(); }

                    String catchVar = "", catchInstance = "char";

                    if (!catchInside.equals("")) {
                        String[] parts = split(catchInside, ' ');
                        if (parts.length == 1) { catchVar = parts[0]; }  
                        else if (parts.length == 2) { catchInstance = parts[0]; catchVar = parts[1]; } 
                        else { echoCommand("build: invalid catch signature"); return null; }
                    }

                    int catchBrace = (parenEnd == -1) ? block.indexOf('{', catchIndex) : block.indexOf('{', parenEnd);
                    if (catchBrace != -1) {
                        String catchRemaining = block.substring(catchBrace);
                        String catchSub = getBlock(catchRemaining);
                        if (catchSub == null) { echoCommand("build: 'catch' block not properly closed"); return null; }

                        cmd.put("catch", parseBlock(catchSub.substring(1, catchSub.length() - 1).trim(), context));
                        cmd.put("catchVar", catchVar);
                        cmd.put("catchInstance", catchInstance);
                    } 
                    else { echoCommand("build: 'catch' block opening '{' not found"); return null; }
                }
            }
            else if (line.startsWith("if")) {
                int lineIndexInBlock = block.indexOf(line);
                if (lineIndexInBlock == -1) { echoCommand("build: 'if' token not found "); return null; }

                int braceIndex = -1;
                for (int j = lineIndexInBlock; j < block.length(); j++) { if (block.charAt(j) == '{') { braceIndex = j; break; } }

                if (braceIndex == -1) { echoCommand("build: 'if' block opening '{' not found"); return null; }

                String remaining = block.substring(braceIndex), subblock = getBlock(remaining);
                if (subblock == null) { echoCommand("build: parse error - 'if' block not properly closed"); return null; }

                cmd.put("type", "if"); cmd.put("expr", extractParens(line, 0));

                int elseIndex = block.indexOf("else", braceIndex + subblock.length());
                if (elseIndex != -1) {
                    int elseBrace = block.indexOf("{", elseIndex);
                    if (elseBrace != -1) {
                        String elseSub = getBlock(block.substring(elseBrace));
                        if (elseSub != null) { cmd.put("else", parseBlock(elseSub.substring(1, elseSub.length() - 1).trim(), context)); } 
                        else { echoCommand("build: 'else' block not properly closed"); return null; }
                    }
                }

                cmd.put("source", parseBlock(subblock.substring(1, subblock.length() - 1).trim(), context));
            }
            else if (line.startsWith("while")) {
                int lineIndexInBlock = block.indexOf(line);
                if (lineIndexInBlock == -1) { echoCommand("build: 'while' token not found"); return null; }

                int braceIndex = -1;
                for (int j = lineIndexInBlock; j < block.length(); j++) { if (block.charAt(j) == '{') { braceIndex = j; break; } }

                if (braceIndex == -1) { echoCommand("build: 'while' block opening '{' not found"); return null; }

                String remaining = block.substring(braceIndex), subblock = getBlock(remaining);
                if (subblock == null) { echoCommand("build: parse error - 'while' block not properly closed"); return null; }

                cmd.put("type", "while");
                cmd.put("expr", extractParens(line, 0));
                cmd.put("source", parseBlock(subblock.substring(1, subblock.length() - 1).trim(), context));
            }
            else if (line.startsWith("else")) { echoCommand("build: unexpected 'else' without matching 'if'"); return null; }
            else if (line.startsWith("catch")) { echoCommand("build: unexpected 'catch' without matching 'try'"); return null; }
            else if (line.equals("break") || line.equals("continue")) { cmd.put("type", line); }
            else if (line.indexOf('(') != -1 && line.lastIndexOf(')') > line.indexOf('(') && line.indexOf('=') == -1 && !startsWithAny(line, new String[]{"int ", "char "}) && line.substring(0, line.indexOf('(')).trim().indexOf(' ') == -1) {
                cmd.put("type", "call");
                cmd.put("function", line.substring(0, line.indexOf('(')).trim());
                cmd.put("args", extractBetween(line, '(', ')'));
            }
            else if (line.indexOf('=') != -1 || startsWithAny(line, new String[]{"int", "char"})) {
                if (startsWithAny(line, new String[]{"int", "char"})) {
                    String varType = line.startsWith("char ") ? "char" : "int";
                    String decls = line.substring(varType.length()).trim();
                    String[] vars = split(decls, ',');

                    for (int j = 0; j < vars.length; j++) {
                        String part = vars[j].trim(), varName, varValue;
                        int eq = part.indexOf('=');

                        if (eq != -1) {
                            varName = part.substring(0, eq).trim();
                            varValue = part.substring(eq + 1).trim();
                        } else { 
                            varName = part;
                            varValue = varType.equals("char") ? "' '" : "0";
                        }

                        if (varName.equals("")) { echoCommand("build: invalid variable declaration '" + part + "'"); return null; }

                        cmd = new Hashtable();
                        cmd.put("type", "assign");
                        cmd.put("name", varName);
                        cmd.put("instance", varType);
                        cmd.put("value", varValue);
                        source.addElement(cmd);
                    }
                    continue;
                }

                String[] parts = split(line, '=');
                if (parts.length == 2) {
                    String varName = parts[0].trim(), value = parts[1].trim();

                    if (varName.indexOf(' ') != -1) { echoCommand("build: invalid assignment type '" + getCommand(varName) + "'"); return null; }

                    cmd.put("type", "assign");
                    cmd.put("name", varName);
                    cmd.put("value", value);
                }
                else { echoCommand("build: invalid assignment syntax"); return null; }
            }
            else { echoCommand("build: invalid statement"); return null; }

            source.addElement(cmd);
        }

        return source;
    }
    private String getBlock(String code) { int depth = 0; for (int i = 0; i < code.length(); i++) { char c = code.charAt(i); if (c == '{') { depth++; } else if (c == '}') { depth--; } if (depth == 0) { return code.substring(0, i + 1); } } return null; }
    private String extractParens(String code, int from) { int start = code.indexOf('(', from); if (start == -1) { return ""; } int depth = 0; for (int i = start; i < code.length(); i++) { char c = code.charAt(i); if (c == '(') { depth++; } else if (c == ')') { depth--; } if (depth == 0) { return code.substring(start + 1, i).trim(); } } return ""; }
    private String extractBetween(String text, char open, char close) { int start = text.indexOf(open), end = text.lastIndexOf(close); if (start == -1 || end == -1 || end <= start) { return ""; } String result = text.substring(start + 1, end).trim(); return result; }
    private String[] splitBlock(String code, char separator) { Vector parts = new Vector(); int depthPar = 0, depthBrace = 0, start = 0; boolean inString = false; for (int i = 0; i < code.length(); i++) { char c = code.charAt(i); if (c == '"') { if (i == 0 || code.charAt(i - 1) != '\\') { inString = !inString; } } else if (!inString) { if (c == '(') { depthPar++; } else if (c == ')') { depthPar--; } else if (c == '{') { depthBrace++; } else if (c == '}') { depthBrace--; } else if (c == separator && depthPar == 0 && depthBrace == 0) { String part = code.substring(start, i).trim(); if (part.equals("")) { part = "' '"; } parts.addElement(part); start = i + 1; } } } String part = code.substring(start).trim(); if (part.equals("")) { part = "' '"; } parts.addElement(part); String[] result = new String[parts.size()]; parts.copyInto(result); return result; }
    private boolean isFuncChar(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'; }
    private boolean startsWithAny(String text, String[] options) { for (int i = 0; i < options.length; i++) { if (text.startsWith(options[i])) { return true; } } return false; }
    