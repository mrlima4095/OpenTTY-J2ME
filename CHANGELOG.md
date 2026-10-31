OpenTTY Java Edition
Copyright (C) 2026 - Mr. Lima

---

[ General ]

- MIDlet version 1.19 with the matching app-store catalog (`apps/1.19`,
  seeded as `/etc/sources`), so the store serves the 1.19 apps
- LGE-MIDlet background and exit attributes in the manifest and JAD
  (`LGE-MIDlet-Allowed-background: YES`, `LGE-MIDlet-Exit: MINIMIZE`), so the
  MIDlet keeps running in the background and minimizes instead of quitting on
  LG phones
- Desktop shim (`pc/`) keeps tracking the MIDlet: the real host name reaches
  `fetch`/`uname`, the Canvas and the new LCDUI widgets have stubs, and the
  headless smoke run exercises the boot/login/console path
- `bench/` benchmark twins: eight identical tests in Lua and in a RISC-V ELF,
  with matching checksums and a best-of-N comparison
- Process IDs are allocated sequentially (last PID + 1) instead of random
  four-digit numbers: `init` keeps PID 1, the first guest process starts at 2,
  and the embedded MIDPlay host owns PID 2 so its first guest process starts
  at 3

[ Shell ]

- `lua <file>` runs a script and `lua -e` errors point at the user chunk instead of the `/bin/lua` wrapper
- `>` truncates its target and `>>` appends to it; `> file` no longer prints a leading blank line
- `fetch` prints `user@host` from the real user name and host name instead of the old `user@localhost`
- clearer messages from `cat`, `curl` and `nice` when a file, URL or priority is missing

[ File System ]

- `/home` folders are real folders: only `/home/` itself keeps plain record stores, while files and nested folders inside `/home/<dir>/` use the same paged VFS as `/bin` (an RMS name cannot hold `/`, so writing a file inside a folder used to fail with `IOException`); folders come back after a restart, `rm` of a folder wipes the whole tree, and `ls /home` lists each entry once
- `mkdir` now works in `/tmp` and `/home` (it only accepted the in-memory mounts before)
- `ls` no longer duplicates entries and listing `/mnt` no longer throws `SecurityException` on phones whose file system is restricted
- `_UID` stays in sync across `su` and nested scopes, so `/proc/<pid>` reports the owner of the process that is actually running
- File reads return bytes verbatim: `io.read`/`require` no longer unescape `\n`/`\t`/`\.`, so Lua sources with backslashes and binary data survive
- `io.write`, `os.remove` and `io.dirs` resolve relative paths against the shell PWD, so `cd /home/x` followed by a bare `note.txt` reaches `/home/x/note.txt` instead of silently doing nothing

[ Network ]

- The `nc` (netcat) app is an interactive terminal: explicit client (`nc HOST PORT`) and listener (`nc -l PORT`) modes, send/clear/disconnect commands, a "Switch to..." list that keeps sessions around, and it survives empty reads instead of closing the connection
- Guest C programs can open sockets now that the ELF socket syscalls reach the host
- `nginx` actually serves requests: clients are answered in the accept thread
  (worker threads could never take the interpreter the accept loop held, so
  every request timed out) and `stop`/`reload` use a control file plus a
  wake-up connection instead of an `os.request` that deadlocked while the
  daemon was idle in `accept`
- `nginx start` seeds `/etc/nginx/{nginx.conf,mime.types,sites-available,sites-enabled}`
  when missing, so the documented config layout exists on a fresh install;
  MIME types now use the nginx `"<mime> <extension>"` order, files larger than
  1 KB are no longer truncated (`io.read(handle)` only returned 1024 bytes),
  `Content-Length` counts UTF-8 bytes, and request paths are traversal-checked
  (no more `/../etc/passwd`); adds `index`, HEAD and 405 support, longest-prefix
  route matching and a `nginx test` report
- `nginx` survives a bad client on phones whose MIDP stack invalidates the
  listening socket once the accepted connection is closed: requests run under
  `pcall`, a failed `accept` is logged and retried instead of ending the daemon,
  and the port is re-bound after every request; the daemon chunk also returns a
  handler (`stop`/`reload`/`status`) so `init` keeps the process and its socket
  alive after the chunk finishes

[ Lua ]

The runtime now follows Lua 5.1 much more closely:

- Numbers: decimal exponents (`1e3`, `1E+2`, `1e-2`), leading and trailing dots (`.5`, `5.`), subtraction written without spaces (`x-1`), unary minus binding below `^` (`-2^2 == -4`) and right-associative `^`
- Strings: real escape decoding (`\n`, `\t`, `\r`, `\a`, `\b`, `\f`, `\v`, `\\`, `\"`, `\'`, `\ddd`, `\xXX`, `\z` and escaped newlines) plus long brackets of any level (`[==[ ... ]==]`) in strings and comments
- Tables: `;`-separated fields, multi-assignment to indexed targets (`t[i], t[j] = t[j], t[i]`), identity comparison (`{} == {}` is false) and `__eq` only firing when both tables share the same handler
- Functions: `function t:m()` with the implicit `self`, call sugar `f{...}` / `f"text"`, varargs and call results carried as value lists (`f(...)`, `return ...`, `{...}`, `{ f() }`, `select`)
- Loops: generic `for` over iterator closures and over an explist (`for k in next, t do`)
- Libraries: new globals `unpack`, `loadstring` and `next`; `tonumber(s, base)`; floored `%`; `table.insert(t, pos, v)` argument order, `table.remove` with correct shifting, `table.sort` with a comparator; `string.byte` ranges returning every value; `os.clock()` returning a number and `os.date`/`os.time` formatting real timestamps instead of `Date.toString()`
- Chunk-level `local`s no longer leak into `_G` (a `local print = ...` used to clobber the global for later chunks) and `load("")` / `load()` inside functions no longer throw
- Full metatables (`__index`/`__newindex`/`__call`/`__tostring`/`__len`, `__eq`/`__lt`/`__le` and the arithmetic/comparison events) and the `debug` library (`traceback`, `getinfo`, `getregistry`) with frames carrying `name`/`source`/`line`
- `return f()` forwards every result of the call
- Anonymous function bodies no longer duplicate a nested `end`, so a `local function` inside a table-constructor handler stops ending at the first one-line `if ... end`
- The interpreter has a single owner: UI callbacks and background jobs take turns instead of corrupting the shared parser state, and `java.yield()` hands control to the other thread
- LCDUI screens are handles: `graphics.new` returns a table, so `screen:append(item)`, `screen:addCommand(cmd)`, `screen:handler({...})`, `screen:handler(fn)` and `screen:display()` work as methods (`type(screen)` stays `"screen"`, and the `graphics.*` functions still accept the raw screen object); `handler(screen, fn)` installs one function for every command, called as `fn(screen, label, ...values)`
- `luarocks` package manager (`/bin/luarocks`) with rock payloads for `math`, `string` (patterns, `format`, `rep`, `gsub`, `gmatch`), `graphics`, `kernel` and `httpd`; `require` loads `/lib/<name>.lua` and installed state lives in `/etc/rocks.installed`
- 14 new rocks in the catalog: `json`, `tablex`, `base64`, `sha1`, `sha256`, `term`, `argparse`, `log`, `config`, `template`, `csv`, `date`, `re` (depends on `string`) and `ui` — JSON/CSV/INI data handling, pure-Lua SHA-1/SHA-256 with HMAC, terminal tables and progress bars, option parsing, levelled logging, Mustache-style templates, date math, pattern helpers and callback-based menus/prompts/file picker; they install through `luarocks install` into `/lib/<name>.lua` and are listed in `docs/LUAROCKS.md`
- `res/lua/j2me.lua` is a complete LuaDoc stub of the runtime (globals, `os`, `io`, `table`, `string`, `math`, `debug`, `socket`, `audio`, `base64`, `push`, `java`, `graphics` and `graphics.canvas`) for editor hints and autocomplete: it covers the screen handles and their methods, `graphics.title`/`ticker`, `handler(screen, fn)` and the rock-provided `string`/`math` extensions, and `docs/lua/README.md` was refreshed to match the current API
- `ui` rock: `ui.prompt` builds its form with the explicit `graphics.new("screen", ...)` type and drops the stray list-mode argument (ui 1.0.1)

[ ELF ]

- Networking works from guest C programs: the socket syscalls follow the RISC-V calling convention (extra arguments arrive in `a4`/`a5`, not on the stack) and `sockaddr_in` is read with a native ` sin_family` plus a network-order `sin_port`, so `socket`/`bind`/`connect`/`send`/`recv` finally reach the host; new TCP and UDP echo samples under `res/apps/src/`
- Native background socket reader (`opentty_socket_reader_start` / `_stop`): received bytes are appended to a `StringItem` through `Display.callSerially`, so the UI keeps repainting while a guest program waits for network data; the native netcat (`nc-c`) uses it and reports connect errors in LCDUI instead of dying silently
- Guest C library grew `strstr`, `strrchr`, `strcasecmp`, `strncasecmp`, `strtol` and `strtoul`, and the heap is a real first-fit allocator again: blocks split on allocation, merge with their free neighbours on `free` (double frees are ignored), a block at the bump pointer is released back to the region, and `realloc` resizes in place when the neighbour allows it
- LCDUI widget parity with the Lua `graphics` package: alerts (plus `show_alert` and a ticker), gauges, spacers, images, choice groups, buttons with item commands, fonts, layouts, labels, `delete` / `insert_text` / `size`, list selection and vibration, with a showcase in `res/apps/src/lcdui-demo.c`
- Correct RISC-V `struct stat` layout (`st_mode` at 8, `st_size` at 32, `st_blksize` / `st_blocks`, directories detected from the VFS), so `stat`/`ls -l` inside guest programs stops reporting garbage
- One-time hint when the guest runs out of memory: instead of a silent 0 from `malloc`, the console explains how to raise the guest RAM with `os.request(1, "memory", 1024)`
- `gethostname` / `uname -n` resolve the host name from `/etc/hostname`, the real machine name (desktop shim) or the `localhost` seed, and `sethostname` persists it back to `/etc/hostname`

[ Graphics ]

- `graphics.canvas`: a persistent off-screen drawing surface with colors, fonts, shapes, cursor/print, clip/translate/save-restore, held keys, pointer input, LCDUI commands and a frame loop (`runLoop`/`animate`), plus the `canvasdemo` app and `docs/LUACANVAS.md`; in loop mode the LCDUI thread only blits the buffer and events are pumped by the loop thread
- Canvas repaints from the LCDUI thread with one coalesced request from a background flush, so drawing no longer freezes the phone
- `graphics.new("gauge")`, `graphics.SetIndicator` / `SetGauge` / `GetGauge` and the `CONTINUOUS_*` constants for gauges, matching LCDUI behavior
- Font specs and image loading follow the real MIDP 2.0 API again, so text sizes and bundled images render on devices
- Desktop shim canvases receive printable keys (including Space), Backspace/Delete/Tab and Home/End/PageUp/PageDown, with Enter still mapped to `FIRE`, so a canvas can host a real text line editor (`cterm` does; see `docs/LUACANVAS.md`)

[ Apps & Games ]

- New 1.19 catalog apps: `snake`, `life`, `matrix`, `clock`, `paint`, `alert`, `pkg`, `cterm` and the `canvasdemo`
- Snake: the apple changes color, relocates to a new free cell when eaten (never a board corner) and never moves on its own, plus an attract mode; `life` was retuned with it
- `paint` draws on a canvas and saves/loads its image
- `cterm`: a Canvas terminal (unlike `xterm`'s LCDUI Form) that paints a monospace cell grid and renders ANSI SGR output — 16/256/truecolour, bold, dim, underline, reverse and hidden, cursor movement, erase, insert/delete lines and characters, scroll and save/restore, wrapping, CR/LF/BS/TAB and a capped scrollback; its in-canvas command line has cursor movement, Backspace/Delete, Up/Down history, Tab completion of `/bin` commands and paths and a coloured `user@host:/path` prompt, built-ins (`clear`, `colors`, `history`, `help`, `exit`) and everything else run through `os.execute` in the same scope as `xterm`, so `cd` persists; PgUp/PgDn, stylus drag, a Font command and a Type box cover keypads without letters; `cterm 1` draws a deterministic demo frame and `CTERM_AS_MODULE` returns the engine without a UI (see `docs/CTERM.md`)
- `cterm` is fast enough for a real phone: rows are a `cols`-wide string plus styled runs instead of one table slot per cell (a native Lua call costs ~20 µs on the device, so per-character work was what made a command feel slow), text is written in segments between control characters, CSI sequences are located eight bytes at a time, parsed CSI parameters and SGR state transitions are cached, and `render()` skips drawing and repainting when nothing changed; output parsing is capped at 16 KB (the scrollback keeps 200 rows) and there is no animation loop or timer, so an idle terminal costs no battery. In the desktop shim a full grid redraw went from 124 ms to 7 ms, a scroll step from 81 ms to 7 ms and 4 KB of plain output from 743 ms to 48 ms (coloured output improved from 1266 ms to 480 ms per 200 lines)
- `alert`: alert type (`-t`), timeout (`-m`), ticker (`-k`), button (`-b`) and vibration (`-v`) options; `-g` keeps the alert on screen with the MIDP continuous gauge, `-c` animates a value gauge, and a timed alert exits cleanly
- `pkg` install screen with a progress gauge and live log, listing only the package names and installing the selection directly
