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

[ ELF ]

- Networking works from guest C programs: the socket syscalls follow the
  RISC-V calling convention (extra arguments arrive in `a4`/`a5`, not on the
  stack) and `sockaddr_in` is read with a native `sin_family` plus a
  network-order `sin_port`, so `socket`/`bind`/`connect`/`send`/`recv` finally
  reach the host; new TCP and UDP echo samples under `res/apps/src/`
- Native background socket reader (`opentty_socket_reader_start` /
  `_stop`): received bytes are appended to a `StringItem` through
  `Display.callSerially`, so the UI keeps repainting while a guest program
  waits for network data; the native netcat (`nc-c`) uses it and reports
  connect errors in LCDUI instead of dying silently
- Guest C library grew `strstr`, `strrchr`, `strcasecmp`, `strncasecmp`,
  `strtol` and `strtoul`, and the heap is a real first-fit allocator again:
  blocks split on allocation, merge with their free neighbours on `free`
  (double frees are ignored), a block at the bump pointer is released back to
  the region, and `realloc` resizes in place when the neighbour allows it
- LCDUI widget parity with the Lua `graphics` package: alerts (plus
  `show_alert` and a ticker), gauges, spacers, images, choice groups,
  buttons with item commands, fonts, layouts, labels, `delete` / `insert_text`
  / `size`, list selection and vibration, with a showcase in
  `res/apps/src/lcdui-demo.c`
- Correct RISC-V `struct stat` layout (`st_mode` at 8, `st_size` at 32,
  `st_blksize` / `st_blocks`, directories detected from the VFS), so
  `stat`/`ls -l` inside guest programs stops reporting garbage
- One-time hint when the guest runs out of memory: instead of a silent 0 from
  `malloc`, the console explains how to raise the guest RAM with
  `os.request(1, "memory", 1024)`
- `gethostname` / `uname -n` resolve the host name from `/etc/hostname`, the
  real machine name (desktop shim) or the `localhost` seed, and
  `sethostname` persists it back to `/etc/hostname`
