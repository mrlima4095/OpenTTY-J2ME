    private int C2ME(String code, boolean root) {
        Hashtable program = build(code);
        if (program.containsKey("error")) { echoCommand((String) program.get("error")); return 1; }

        Hashtable functions = (Hashtable) program.get("functions");
        Vector globals = (Vector) program.get("globals");
        if (!functions.containsKey("main")) { echoCommand("main() missing"); return 1; }

        Hashtable variables = new Hashtable();
        for (int i = 0; i < globals.size(); i++) {
            Hashtable g = (Hashtable) globals.elementAt(i);
            String name = (String) g.get("name"), type = (String) g.get("type");
            String val = g.containsKey("value") ? (String) g.get("value") : (type.equals("char") ? "' '" : "0");

            Hashtable var = new Hashtable();
            var.put("type", type);
            if (type.equals("int") || type.equals("float") || type.equals("double")) {
                String expr = exprCommand((val));

                if (expr.startsWith("expr: ")) { echoCommand("error: invalid '" + type + "' expression for '" + name + "'"); return 2; }
                else { var.put("value", expr); }
            } 
            else if (type.equals("char")) { var.put("value", val); } 
            else { echoCommand("error: unknown type '" + type + "' for variable '" + name + "'"); return 2; }

            variables.put(name, var);
        }

        Hashtable main = (Hashtable) functions.get("main");
        Vector decls = (Vector) main.get("declarations");
        Vector instrs = (Vector) main.get("instructions");

        for (int i = 0; i < decls.size(); i++) {
            Hashtable d = (Hashtable) decls.elementAt(i);
            String name = (String) d.get("name"), type = (String) d.get("type");
            String val = d.containsKey("value") ? (String) d.get("value") : (type.equals("char") ? "' '" : "0");

            Hashtable var = new Hashtable();
            var.put("type", type);
            if (type.equals("int") || type.equals("float") || type.equals("double")) {
                String expr = exprCommand(val);

                if (expr.startsWith("expr: ")) { echoCommand("error: invalid '" + type + "' expression for '" + name + "'"); return 2; }
                else { var.put("value", expr); }
            } 
            else if (type.equals("char")) { var.put("value", val); }
            else { echoCommand("error: unknown type '" + type + "' for variable '" + name + "'"); return 2; }

            variables.put(name, var);
        }

        for (int i = 0; i < instrs.size(); i++) {
            Hashtable cmd = (Hashtable) instrs.elementAt(i);
            String tipo = (String) cmd.get("cmd");

            if (tipo.equals("assign")) {
                String name = (String) cmd.get("name");
                String val = (String) cmd.get("value");
                Hashtable v = (Hashtable) variables.get(name);

                if (v == null) { echoCommand("error: undeclared variable: " + name); return 2; }

                String type = (String) v.get("type");

                Hashtable nv = new Hashtable();
                nv.put("type", type);
                if (type.equals("int") || type.equals("float") || type.equals("double")) {
                    val = substituteVars(val, "", variables);
                    String expr = exprCommand(val);
                    if (expr.startsWith("expr: ")) { echoCommand("build: invalid value for " + type + " '" + name + "'."); return 2; }
                    nv.put("value", expr);
                } 
                else if (type.equals("char")) { nv.put("value", val); } 
                else { echoCommand("error: unknown type '" + type + "' for variable '" + name + "'"); return 2; }
                variables.put(name, nv);
            }

            else if (tipo.equals("printf") || tipo.equals("exec")) {
                String fmt = (String) cmd.get("args");
                fmt = substituteVars(fmt, "%", variables);
                fmt = replace(fmt, "\\n", "\n");
                if (fmt.startsWith("\"") && fmt.endsWith("\"")) fmt = fmt.substring(1, fmt.length() - 1);
                if (tipo.equals("printf")) echoCommand(fmt);
                else processCommand((String) cmd.get("args"), false, root);
            }

            else if (tipo.equals("return")) {
                String fmt = (String) cmd.get("args");
                
                try { return Integer.parseInt(substituteVars(fmt, "", variables)); }
                catch (Exception e) { return 2; }
            }

            else if (tipo.equals("call") || tipo.equals("call_inline")) {
                String fname = (String) cmd.get("func");
                String args = (String) cmd.get("args");
                String dest = tipo.equals("call") ? (String) cmd.get("name") : null;

                if (!functions.containsKey(fname)) {
                    echoCommand("function '" + fname + "' not found");
                    return 127;
                }

                Hashtable f = (Hashtable) functions.get(fname);
                Vector fdecl = (Vector) f.get("declarations");
                Vector finst = (Vector) f.get("instructions");
                String rettype = (String) f.get("type");

                Hashtable fvars = new Hashtable();
                String[] argv = split(args, ',');
                for (int j = 0; j < fdecl.size(); j++) {
                    Hashtable p = (Hashtable) fdecl.elementAt(j);
                    String pname = (String) p.get("name");
                    String ptype = (String) p.get("type");

                    if (j >= argv.length) {
                        echoCommand(fname + "(): missing " + ptype + " '" + pname + "'");
                        return 2;
                    }

                    String val = argv[j].trim();
                    val = substituteVars(val, "", variables);

                    Hashtable fvar = new Hashtable();
                    fvar.put("type", ptype);
                    if (ptype.equals("int") || ptype.equals("float") || ptype.equals("double")) {
                        fvar.put("value", exprCommand(val));
                    } else {
                        fvar.put("value", val);
                    }

                    fvars.put(pname, fvar);
                }

                String ret = rettype.equals("char") ? "' '" : "0";
                for (int j = 0; j < finst.size(); j++) {
                    Hashtable fcmd = (Hashtable) finst.elementAt(j);
                    String ftype = (String) fcmd.get("cmd");

                    if (ftype.equals("return")) {
                        ret = substituteVars((String) fcmd.get("value"), "", fvars);
                        if (!rettype.equals("char")) ret = exprCommand(ret);
                        break;
                    } else if (ftype.equals("printf")) {
                        String fmt = (String) fcmd.get("args");
                        fmt = substituteVars(fmt, "%", fvars);
                        fmt = replace(fmt, "\\n", "\n");
                        if (fmt.startsWith("\"") && fmt.endsWith("\"")) fmt = fmt.substring(1, fmt.length() - 1);
                        echoCommand(fmt);
                    }
                }

                if (dest != null) {
                    Hashtable destVar = new Hashtable();
                    if (variables.containsKey(dest)) {
                        destVar = (Hashtable) variables.get(dest);
                        destVar.put("value", ret);
                    } else {
                        destVar.put("type", rettype);
                        destVar.put("value", ret);
                    }
                    variables.put(dest, destVar);
                }
            }
        }

        return 0;
    }

    private Hashtable build(String code) {
        Hashtable program = new Hashtable();
        Hashtable functions = new Hashtable();
        Vector globals = new Vector();
        Hashtable includes = new Hashtable();

        code = removeComments(code);
        code = replace(code, "\t", " ");
        code = replace(code, "\n", " ");
        code = replace(code, "\r", " ");
        while (code.indexOf("  ") != -1) code = replace(code, "  ", " ");

        String[] lines = split(code, ';');
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.startsWith("#include \"") && line.endsWith("\"")) {
                String path = line.substring(10, line.length() - 1);
                String content = getcontent(path);
                if (!content.equals("")) {
                    Hashtable included = build(content);
                    if (included.containsKey("error")) {
                        program.put("error", "error in include: " + path);
                        return program;
                    }
                    includes.put(path, included);
                    Hashtable incFuncs = (Hashtable) included.get("functions");
                    for (Enumeration e = incFuncs.keys(); e.hasMoreElements(); ) {
                        String k = (String) e.nextElement();
                        functions.put(k, incFuncs.get(k));
                    }
                    Vector incGlobals = (Vector) included.get("globals");
                    for (int g = 0; g < incGlobals.size(); g++) globals.addElement(incGlobals.elementAt(g));
                }
            }
        }

        String raw = code;
        while (true) {
            int idx = findFunctionStart(raw);
            if (idx == -1) break;

            int nameStart = raw.indexOf(' ', idx) + 1;
            int nameEnd = raw.indexOf('(', nameStart);
            String type = raw.substring(idx, nameStart).trim();
            String fname = raw.substring(nameStart, nameEnd).trim();

            int argsEnd = raw.indexOf(')', nameEnd);
            int bodyStart = raw.indexOf('{', argsEnd);
            if (argsEnd == -1 || bodyStart == -1) break;

            String bodyFull = getBlock(raw.substring(bodyStart));
            if (bodyFull == null) {
                program.put("error", "EOF");
                return program;
            }

            String body = bodyFull.substring(1, bodyFull.length() - 1).trim();
            Vector decls = new Vector();
            Vector instrs = new Vector();
            String[] parts = split(body, ';');

            for (int j = 0; j < parts.length; j++) {
                String line = parts[j].trim();

                if (line.equals("")) { }
                else if (startsWithAny(line, new String[]{ "int ", "char ", "float ", "double " })) {
                    Hashtable decl = new Hashtable();
                    int sp = line.indexOf(' ');
                    String declType = line.substring(0, sp).trim();
                    String rest = line.substring(sp + 1).trim();
                    if (rest.indexOf('=') != -1) {
                        int eq = rest.indexOf('=');
                        decl.put("type", declType);
                        decl.put("name", rest.substring(0, eq).trim());
                        decl.put("value", rest.substring(eq + 1).trim());
                    } else {
                        decl.put("type", declType);
                        decl.put("name", rest);
                    }
                    decls.addElement(decl);
                    continue;
                } else if (line.startsWith("printf(") || line.startsWith("exec(")) {
                    Hashtable cmd = new Hashtable();
                    cmd.put("cmd", line.startsWith("printf(") ? "printf" : "exec");
                    cmd.put("args", extractBetween(line, '(', ')'));
                    instrs.addElement(cmd);
                    continue;
                } else if (line.startsWith("return ")) {
                    Hashtable cmd = new Hashtable();
                    cmd.put("cmd", "return");
                    cmd.put("value", line.substring(7).trim());
                    instrs.addElement(cmd);
                    continue;
                } else if (line.indexOf('=') != -1 && line.indexOf('(') == -1) {
                    Hashtable cmd = new Hashtable();
                    int eq = line.indexOf('=');
                    cmd.put("cmd", "assign");
                    cmd.put("name", line.substring(0, eq).trim());
                    cmd.put("value", line.substring(eq + 1).trim());
                    instrs.addElement(cmd);
                    continue;
                } else if (line.indexOf('=') != -1 && line.indexOf('(') != -1) {
                    Hashtable cmd = new Hashtable();
                    int eq = line.indexOf('=');
                    String name = line.substring(0, eq).trim();
                    String call = line.substring(eq + 1).trim();
                    int p1 = call.indexOf('(');
                    String fname2 = call.substring(0, p1);
                    String args = extractBetween(call, '(', ')');

                    cmd.put("cmd", "call");
                    cmd.put("name", name);
                    cmd.put("func", fname2);
                    cmd.put("args", args);
                    instrs.addElement(cmd);
                    continue;
                } else if (line.endsWith(")")) {
                    int p1 = line.indexOf('(');
                    if (p1 != -1) {
                        String fname2 = line.substring(0, p1).trim();
                        String args = extractBetween(line, '(', ')');
                        Hashtable cmd = new Hashtable();
                        cmd.put("cmd", "call_inline");
                        cmd.put("func", fname2);
                        cmd.put("args", args);
                        instrs.addElement(cmd);
                        continue;
                    }
                } else {
                    program.put("error", "invalid syntax: " + line);
                    return program;
                }
            }

            Vector argsDecls = new Vector();
            String argsLine = raw.substring(nameEnd + 1, argsEnd).trim();
            if (!argsLine.equals("")) {
                String[] argsSplit = split(argsLine, ',');
                for (int i = 0; i < argsSplit.length; i++) {
                    String a = argsSplit[i].trim();
                    int space = a.indexOf(' ');
                    if (space != -1) {
                        Hashtable d = new Hashtable();
                        d.put("type", a.substring(0, space));
                        d.put("name", a.substring(space + 1).trim());
                        argsDecls.addElement(d);
                    }
                }
            }

            for (int i = 0; i < argsDecls.size(); i++) decls.addElement(argsDecls.elementAt(i));

            Hashtable func = new Hashtable();
            func.put("type", type);
            func.put("declarations", decls);
            func.put("instructions", instrs);
            functions.put(fname, func);

            int funcEnd = bodyStart + bodyFull.length();
            raw = raw.substring(0, idx) + raw.substring(funcEnd);
        }

        String[] gdecls = split(raw, ';');
        for (int i = 0; i < gdecls.length; i++) {
            String line = gdecls[i].trim();
            if (startsWithAny(line, new String[]{"int ", "char ", "float ", "double "})) {
                Hashtable g = new Hashtable();
                int sp = line.indexOf(' ');
                String type = line.substring(0, sp).trim();
                String rest = line.substring(sp + 1).trim();
                String[] parts = split(rest, '=');
                g.put("type", type);
                g.put("name", parts[0].trim());
                if (parts.length > 1) g.put("value", parts[1].trim());
                globals.addElement(g);
            }
        }

        program.put("functions", functions);
        program.put("globals", globals);
        program.put("includes", includes);
        return program;
    }


    private String substituteVars(String expr, String prefix, Hashtable vars) {
        for (Enumeration e = vars.keys(); e.hasMoreElements(); ) {
            String k = (String) e.nextElement();
            Hashtable v = (Hashtable) vars.get(k);
            String val = String.valueOf(v.get("value"));
            expr = replace(expr, prefix + k, val);
        }
        return expr;
    }
    private String extractBetween(String text, char open, char close) { int start = text.indexOf(open), end = text.lastIndexOf(close); if (start == -1 || end == -1 || end <= start) { return ""; } String result = text.substring(start + 1, end).trim(); if (result.startsWith("\"") && result.endsWith("\"")) { result = result.substring(1, result.length() - 1); } return result; }
    private String getBlock(String code) { int depth = 0; for (int i = 0; i < code.length(); i++) { char c = code.charAt(i); if (c == '{') { depth++; } else if (c == '}') { depth--; } if (depth == 0) { return code.substring(0, i + 1); } } return null; }
    private String removeComments(String code) { while (true) { int idx = code.indexOf("//"); if (idx == -1) { break; } int endl = code.indexOf("\n", idx); if (endl == -1) { endl = code.length(); } code = code.substring(0, idx) + code.substring(endl); } while (true) { int start = code.indexOf("/*"); if (start == -1) { break; } int end = code.indexOf("*/", start + 2); if (end == -1) { code = code.substring(0, start); break; } code = code.substring(0, start) + code.substring(end + 2); } return code; }
    private boolean startsWithAny(String text, String[] options) { for (int i = 0; i < options.length; i++) { if (text.startsWith(options[i])) return true; } return false; }
    private int findFunctionStart(String code) {
        String[] types = { "int", "char", "float", "double" };

        for (int i = 0; i < code.length(); i++) {
            for (int t = 0; t < types.length; t++) {
                String type = types[t];
                if (code.startsWith(type + " ", i)) {
                    int nameStart = i + type.length() + 1;
                    int p1 = code.indexOf('(', nameStart);
                    if (p1 == -1) continue;
                    int p2 = code.indexOf(')', p1);
                    if (p2 == -1) continue;
                    int brace = code.indexOf('{', p2);
                    if (brace == -1) continue;

                    // Verifica se não é declaração de variável: deve ter nome(), não nome;
                    String maybeName = code.substring(nameStart, p1).trim();
                    if (maybeName.indexOf(' ') != -1) continue; // nome inválido

                    // Certifica-se que não é uma linha só (ex: "int x = 0;")
                    String beforeBrace = code.substring(p2 + 1, brace).trim();
                    if (beforeBrace.length() > 0) continue;

                    return i;
                }
            }
        }

        return -1; // não encontrado
    }