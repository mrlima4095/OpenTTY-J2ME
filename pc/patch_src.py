#!/usr/bin/env python3
"""Build-time patches applied to a work copy of src/ so it compiles with a
modern javac on the desktop. Nothing in src/ itself is modified.

CLDC has no java.util.List, so the real sources treat "List" as the LCDUI
List and re-use local variable names across switch cases (legal only on the
lenient CLDC compiler). Both are fixed here mechanically:
  1. add a single-type import for lcdui.List, which wins over the wildcard
     java.util.* / lcdui.* imports;
  2. wrap every case body of every switch in braces, giving each case its
     own scope (removes the local-shadowing errors from modern javac);
  3. swallow IOException from the bare close() calls in the finally block
     of OpenTTY.write().
"""
import sys

SRC = sys.argv[1]


def patch_file(path, ops):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    for find, repl, name in ops:
        assert find in text, "%s: pattern not found: %s" % (path, name)
        text = text.replace(find, repl, 1)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def add_list_import(path):
    patch_file(path, [(
        "import javax.microedition.lcdui.*;",
        "import javax.microedition.lcdui.List;\nimport javax.microedition.lcdui.*;",
        "list-import",
    )])


import re

LABEL_RUN = re.compile(r"^(\s*)((?:(?:case\s+.*?|default)\s*:)(?:\s+(?:case\s+.*?|default)\s*:)*)(.*)$")


def is_label_line(raw):
    return bool(LABEL_RUN.match(raw))


def wrap_all_switch_cases(path):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    out = []
    i = 0
    n = len(text)
    while i < n:
        sw = text.find("switch (", i)
        if sw < 0:
            out.append(text[i:])
            break
        br = text.find("{", sw)
        if br < 0 or br - sw > 500:
            out.append(text[i:br if br >= 0 else n])
            i = br if br >= 0 else n
            continue
        out.append(text[i:br + 1])
        depth = 1
        j = br + 1
        end = None
        while j < n:
            c = text[j]
            if c == "{":
                depth += 1
            elif c == "}":
                depth -= 1
                if depth == 0:
                    end = j
                    break
            j += 1
        if end is None:
            out.append(text[br + 1:])
            break
        body = text[br + 1:end]
        lines = body.split("\n")
        wrapped = []
        open_block = False
        prev_was_label = False
        for raw in lines:
            m = LABEL_RUN.match(raw)
            if m:
                indent = m.group(1)
                labels = m.group(2)
                rest = m.group(3).strip()
                if open_block and not prev_was_label:
                    wrapped.append("}")
                if rest:
                    wrapped.append("%s%s { %s }" % (indent, labels, rest))
                    open_block = False
                else:
                    wrapped.append("%s%s {" % (indent, labels))
                    open_block = True
                prev_was_label = True
            else:
                wrapped.append(raw)
                prev_was_label = False
        if open_block:
            wrapped.append("}")
        out.append("\n".join(wrapped))
        out.append("}")
        i = end + 1
    with open(path, "w", encoding="utf-8") as f:
        f.write("".join(out))


def fix_write_finally(path):
    patch_file(path, [(
        "finally { out.close(); fs.close(); }",
        "finally { try { out.close(); } catch (Exception e) { } try { fs.close(); } catch (Exception e) { } }",
        "write-finally",
    )])


def fix_token_index(path):
    # Lua.run() reuses the instance cursor without resetting it, so a second
    # run() on the same Lua (e.g. /bin/lua loading a chunk and the caller
    # re-running it) starts at EOF and skips every statement. Reset it before
    # the statement loop.
    patch_file(path, [(
        "frameStack.removeAllElements();\n        clearThrown();\n        silent = false;",
        "frameStack.removeAllElements();\n        clearThrown();\n        tokenIndex = 0;\n        silent = false;",
        "run-token-index-reset",
    )])


def fix_exec_debug(path):
    # Optional exec tracing (opentty.execdbg=1) for the desktop runner.
    patch_file(path, [(
        "String mainCommand = midlet.getCommand(command), argument = midlet.getArgument(command);\n                String[] argv = midlet.splitArgs(argument);",
        "String mainCommand = midlet.getCommand(command), argument = midlet.getArgument(command);\n                String[] argv = midlet.splitArgs(argument);\n                if ((System.getProperty(\"opentty.execdbg\") != null)) { System.out.println(\"[execdbg] main=\" + mainCommand + \" args=\" + java.util.Arrays.toString(argv)); }",
        "exec-debug",
    )])


def fix_exec_dot_shadow(path):
    # On the desktop, "/bin/." resolves as the bin *directory* (an empty
    # stream), so the "/bin/<main>" branch swallows a "." command before the
    # source-branch below can run the file. Keep "." out of that branch.
    patch_file(path, [(
        "else if ((inOut = open(\"/bin/\" + mainCommand, father)) != null) { status = (Integer) popen(\"/bin/\" + mainCommand, midlet.genpid(), argument, id, output, father, inOut).elementAt(0); }",
        "else if (!mainCommand.equals(\".\") && (inOut = open(\"/bin/\" + mainCommand, father)) != null) { status = (Integer) popen(\"/bin/\" + mainCommand, midlet.genpid(), argument, id, output, father, inOut).elementAt(0); }",
        "exec-dot-shadow",
    )])


def replace_arg_ranges(path, ranges, name):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    lines = text.split("\n")
    for lo, hi in ranges:
        for ln in range(lo, hi + 1):
            if 1 <= ln <= len(lines):
                lines[ln - 1] = re.sub(r"\barg\b", "arg0", lines[ln - 1])
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("renamed method-level 'arg' -> 'arg0' in", name)


def rename_word_range(path, lo, hi, word, repl, name):
    with open(path, "r", encoding="utf-8") as f:
        text = f.read()
    lines = text.split("\n")
    assert lo >= 1 and hi <= len(lines), "bad range %d..%d in %s" % (lo, hi, name)
    for ln in range(lo, hi + 1):
        lines[ln - 1] = re.sub(r"\b%s\b" % word, repl, lines[ln - 1])
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print("renamed '%s' -> '%s' in %s lines %d..%d" % (word, repl, name, lo, hi))


# Method-level uses of `arg` in Lua.java internals() (all single-scope spans
# verified to NOT contain a local `arg` declaration):
ARG_RANGES = [
    (1417, 1417),            # the declaration itself
    (1740, 1746),            # case READ
    (1816, 1826),            # case CLOSE
    (2163, 2189),            # case BASE64_ENCODE
    (2579, 2598),            # case CHAR
    (2708, 2710),            # case SLEEP
]


def fix_shadowing(path):
    replace_arg_ranges(path, ARG_RANGES, path)
    patch_file(path, [
        # KERNEL case: `pid` in the multi-declaration is never used; nesting it
        # out of the way makes the three nested `String pid` locals legal.
        ("Object payload = args.elementAt(0), arg = args.elementAt(1), scope = args.elementAt(2), pid = args.elementAt(3);",
         "Object payload = args.elementAt(0), arg = args.elementAt(1), scope = args.elementAt(2), kpid = args.elementAt(3);",
         "kernel-pid"),
        # exec(): inner `String[] args` shadows the method-level split axis.
        ("String[] args = new String[sanitize.size()];\n                        sanitize.copyInto(args);",
         "String[] sanitizedArgs = new String[sanitize.size()];\n                        sanitize.copyInto(sanitizedArgs);",
         "exec-sanitized-args"),
        # exec(): the cat() `InputStream in` shadows the method-level `in`.
        ("int status = 0; InputStream in; boolean builtin = args.size() > 1 ? ((Boolean) args.elementAt(1)).booleanValue() : false;",
         "int status = 0; InputStream inOut; boolean builtin = args.size() > 1 ? ((Boolean) args.elementAt(1)).booleanValue() : false;",
         "exec-in-decl"),
        ("else if ((in = open(\"/bin/\" + mainCommand, father)) != null) { status = (Integer) popen(\"/bin/\" + mainCommand, midlet.genpid(), argument, id, output, father, in).elementAt(0); }",
         "else if ((inOut = open(\"/bin/\" + mainCommand, father)) != null) { status = (Integer) popen(\"/bin/\" + mainCommand, midlet.genpid(), argument, id, output, father, inOut).elementAt(0); }",
         "exec-in-use-1"),
        ("else if ((in = open(midlet.joinpath(args[0], father), father)) != null) {",
         "else if ((inOut = open(midlet.joinpath(args[0], father), father)) != null) {",
         "exec-in-use-2"),
        ("status = (Integer) popen(args[0], midlet.genpid(), argument.substring(args[0].length()).trim(), id, output, father, in).elementAt(0);",
         "status = (Integer) popen(args[0], midlet.genpid(), argument.substring(args[0].length()).trim(), id, output, father, inOut).elementAt(0);",
         "exec-in-use-3"),
        # exec(): `String[] args` shadows the `Vector args` parameter once it is
        # declared; on device the array wins from that point on. Rename that
        # local (method-level) `args` -> `argv` for the whole remainder of exec.
        ("String mainCommand = midlet.getCommand(command), argument = midlet.getArgument(command);\n                String[] args = midlet.splitArgs(argument);",
         "String mainCommand = midlet.getCommand(command), argument = midlet.getArgument(command);\n                String[] argv = midlet.splitArgs(argument);",
         "exec-main-args-decl"),
        # KERNEL case: the daemon-spawn locality redeclares `arg`, which would
        # shadow the KERNEL-local `arg` from the switch-case declaration.
        ("Hashtable arg = new Hashtable(); arg.put(new Double(0), program); arg.put(new Double(1), \"--deamon\");",
         "Hashtable dargs = new Hashtable(); dargs.put(new Double(0), program); dargs.put(new Double(1), \"--deamon\");",
         "kernel-daemon-arg-decl"),
        ("Hashtable res = process.lua.run(program, code, arg);",
         "Hashtable res = process.lua.run(program, code, dargs);",
         "kernel-daemon-arg-use"),
    ])
    # exec(): the array-local `String[] args` (now `argv`) shadows the `Vector
    # args` parameter. Original lines 3022/3023 hold mainCommand/argv; every
    # use of `args` after that point refers to the array on device.
    rename_word_range(path, 3024, 3281, "args", "argv", path)


open_ = SRC + "/OpenTTY.java"
lua_ = SRC + "/Lua.java"

fix_shadowing(lua_)
add_list_import(open_)
add_list_import(lua_)
wrap_all_switch_cases(lua_)
fix_token_index(lua_)
fix_exec_debug(lua_)
fix_exec_dot_shadow(lua_)
fix_write_finally(open_)
print("patched", open_, "and", lua_)