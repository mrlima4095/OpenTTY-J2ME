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