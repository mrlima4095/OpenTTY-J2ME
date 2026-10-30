OpenTTY Java Edition
Copyright (C) 2026 - Mr. Lima

---

[ Lua ]

Valid Lua 5.1 code that used to be rejected or silently mis-ran now parses
and behaves like the reference interpreter:

- Numbers: decimal exponents (`1e3`, `1E+2`, `1e-2`), leading and trailing
  dots (`.5`, `5.`), subtraction written without spaces (`x-1`), unary minus
  binding below `^` (`-2^2 == -4`) and right-associative `^`
- Strings: real escape decoding (`\n`, `\t`, `\r`, `\a`, `\b`, `\f`, `\v`,
  `\\`, `\"`, `\'`, `\ddd`, `\xXX`, `\z` and escaped newlines) plus long
  brackets of any level (`[==[ ... ]==]`) in strings and comments
- Tables: `;`-separated fields, multi-assignment to indexed targets
  (`t[i], t[j] = t[j], t[i]`), identity comparison (`{} == {}` is false) and
  `__eq` only firing when both tables share the same handler
- Functions: `function t:m()` with the implicit `self`, call sugar
  `f{...}` / `f"text"`, varargs and call results carried as value lists
  (`f(...)`, `return ...`, `{...}`, `{ f() }`, `select`)
- Loops: generic `for` over iterator closures and over an explist
  (`for k in next, t do`)
- Libraries: new globals `unpack`, `loadstring` and `next`; `tonumber(s, base)`;
  floored `%`; `table.insert(t, pos, v)` argument order, `table.remove` with
  correct shifting, `table.sort` with a comparator; `string.byte` ranges
  returning every value
- Chunk-level `local`s no longer leak into `_G` (a `local print = ...` used to
  clobber the global for later chunks) and `load("")` / `load()` inside
  functions no longer throw
