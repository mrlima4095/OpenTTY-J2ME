OpenTTY Java Edition
Copyright (C) 2026 - Mr. Lima

---

### Multi-tasking / process management

- `Process` now has its own `Displayable screen` (registered via `os.setproc("screen", ...)`); the global `midlet.stdout`/`midlet.stdin` were **removed** — each process has its own output buffer (per-process `StringBuffer`) and its own input `TextField`
- New **per-process** socket tracking (`Process.net`) replaces the global `midlet.network`
- New **Task Manager** `graphics.taskmngr()` (a `List` `"Running"` using `List.IMPLICIT`) listing every process as `"title/name [pid]"`:
  - **Back**: if no processes remain, calls `destroyApp` (closes the MIDlet), otherwise returns to the previous screen
  - **Interrupt (SIGTERM)**: sends signal 15 to the selected process's `sighandler` and removes it from the list
  - **SELECT**: switches `display.setCurrent()` to the process's screen (process switching)
- `os.setproc("title", ...)` sets the title shown in the Task Manager (`title [pid]` instead of `name [pid]`)
- Process scope is cloned by `cloneScope()` in `os.popen`/`serve`, so `su`/`cd` in one terminal no longer affect the others
- **`/bin/xterm`** (new): the terminal emulator was extracted from `init` — it creates its own stdout/stdin/screen, `[USER@HOST PWD] #/$` prompt, "Run" command and "Switch to..." button (task manager); shows `/etc/motd` on open
- **`/bin/init`** simplified: it only mounts fstab, sets env, `os.su`, calls `os.execute("xterm")`; also accepts `--serve=<program>` to spawn daemons
- `graphics.display()` clears `kill` when showing a `Displayable`, keeping the runtime alive with a foreground screen
- Allowed multiple terminals

### ELF / network

- **Full socket API** restored in the ELF emulator:
  - `bind`: opens `StreamConnectionNotifier`/`DatagramConnection` on the given port, returns `-EADDRINUSE` on conflict
  - `listen`: marks the socket as listening, with ephemeral-port fallback
  - `accept`: `acceptAndOpen`, new fd with streams + peer `sockaddr_in` returned to the caller
  - `sendto`/`recvfrom`: DGRAM via `DatagramConnection`, TCP via streams, with peer writeback
  - `shutdown` and `nanosleep` restored (success no-ops)
- **Socket options**: `setsockopt`/`getsockopt` with an options table (`SO_REUSEADDR`, `SO_KEEPALIVE`, `SO_OOBINLINE`, `SO_BROADCAST`, `SO_DONTROUTE`, `SO_LINGER`, `SO_SNDBUF`, `SO_RCVBUF`, `TCP_NODELAY`); `SO_ERROR` and `SO_TYPE`; `-ENOPROTOOPT` for unsupported options
- New constants: `SOL_SOCKET`, `SOL_IP`, `TCP_NODELAY`, `SO_*`; new errors `ENOTSOCK`, `ENOPROTOOPT`, `EADDRINUSE`, `EADDRNOTAVAIL`, `EISCONN`
- A failed `connect` sets `socketInfo["error"] = 111`
- ELF `listdir` now sees the virtual `/proc/` (pid entries, `cpuinfo`, `meminfo`, `uptime`, `version`) via `midlet.procEntries()`
- Shutdown cleanup also closes `datagram` connections

### Filesystem / VFS / Proc

- **Nested VFS under `/bin/`, `/etc/`, `/lib/`**: subdirectories are supported as RMS mounts, each mapping to its own `OpenRMS` page via a stable path hash (indices `>= 9`, `VFS_HASH_MOD=97`)
- Read/write/delete resolve nested paths at any depth (`/bin/tools/sub/file.lua`); `io.dirs` and ELF `getdents` list any `/bin|etc|lib/...` folder
- Writing into a subdir auto-registers it in the VFS (`cd` + `ls` detect it); `rms` can clear subdir stores (`rm -r /bin/tools`)
- Declare subdirs in `/etc/fstab` under their parent line with a trailing `/` (e.g. `tools/`); `addFile` refactored to operate on a store index
- **Persistence**: VFS subdirs survive restarts via `/etc/vfs.conf` (written on mkdir, removed on delete/`rms`, restored on mount)
- **Root's home is `/root/`** (OpenRMS index 6); regular users cannot read/write/enter it; `rms` stays root-only; `su`, `chdir` and listing all enforce the rule
- Virtual `/proc/`: `uptime`, `version`, `meminfo` (now `Runtime.totalMemory`, since CLDC lacks `maxMemory`), `cpuinfo` and `/proc/<pid>/` with `status`, `cmdline`, `comm`, `stat`; regular users only see their own processes (root sees all)
- `fstab` updated: adds `root/` to the root mount, and `mkdir`, `pkg`, `xterm` to `/bin/`

### Shell & commands

- **`/bin/sh`** cut from 284 → 32 lines: builtins are handled by the kernel/`os.execute`; preserves `-c`, file execution and interactive mode
- **`/bin/pkg`** (rewritten, v1.6.0): uses `socket.http.get`/`rget` instead of raw TCP; server `http://opentty.fun` (`REPO` override); commands `install`, `remove`, `update`, `list`, `info`, `download`, `run`; mirror with 50+ packages
- **`/bin/yang`** slimmed to a wrapper that forwards args to `pkg`
- **`/bin/mkdir`** (new): creates VFS directories; root-only under `/bin`, `/etc`, `/lib`, `/root`
- **`/bin/lua`** now runs files directly (`lua <file>`)
- **`/bin/cp`** supports a single-argument mode (`cp file` copies to `file-copy`)
- **`/bin/rm`** accepts `-r`/`-rf`/`-fr` and multiple files
- **`/bin/nano`**: "Add new line" button (for J2EMU) and "Switch to..." (task manager)
- **`/bin/curl`**: fixed URL parsing (`sub(1,5)` for `"http:"`)
- Add `#!/bin/sh` shebang support on `. [file]` run

### New apps

- **`xterm`** — terminal emulator (see multi-tasking)
- **`irc`** (`apps/net/irc.lua`) — IRC client with CLI (`connect`/`send`) and GUI modes, join/part/nick/PRIVMSG, PING/PONG, MOTD
- **`play`** (`apps/file/play.lua`) — audio player (play/stop/pause/resume/status/volume/list) driving the `audio-codec` daemon; GUI mode
- **`tree`** (`apps/file/tree.lua`) — directory tree viewer (`-d`, `-L N`, `-a`, counts)
- **`nginx`** (`apps/net/nginx/main.lua`) — nginx-style HTTP server: config `/etc/nginx/nginx.conf`, `mime.types`, `sites-enabled/`, static serving, `proxy_pass`, per-location alias, access/error logging
- **`dns`** (`apps/net/dns/main.lua`) — DNS server daemon: reads `/etc/hosts` and `/etc/dns/*.zone`, answers A/AAAA/MX/CNAME, stats, `lookup`/`add`/`remove`/`reload`/`list`
- **`head`, `tail`, `netstat`** — implemented (head/tail print the first/last N lines; netstat tests connectivity via HTTP GET)

### Updated apps

- **`jdb`** (`apps/sys/benchmark/main.lua`, +388 lines) — adb-style debug bridge: `ps`, `getproc`, `dumpsys`, `logcat`, `users`, `crash`, `stack`, `meminfo`, `shell`, `connect` (TCP/UDP client); server mode on port 5555
- **`sudo`** — now reads all args (`arg[1]` is the command, the rest are forwarded); executes as root and restores the original user
- **`docker`** — expanded container management, images and init scripts
- **`httpd`** (`res/lua/modules/httpd.lua`) — new `httpd.static(root_dir)` function to serve static files with MIME detection

### Lua runtime

- **performance optimizations**:
  - `SMALL_NUMBERS[-128..1023]` — cached small `Double`s for loops/indices
  - `ScopeTable` (chained scope) instead of cloning the globals table on every function call
  - `StringBuffer` concatenation; static `Boolean TRUE/FALSE`; per-source tokenizer caching; early-returns in `getpattern`/`replace`/`escape`
- **errors / traceback**: new `getTraceback(Throwable)` emits a `Frame` stack trace (name, source, line), `pointerBlock()` with `^---` and `(near '<token>')`, injected into `run()`, `pcall`, `os.request` handlers, background threads and UI callbacks
- **new functions**: `string.startswith`, `string.endswith`, `table.pack`, `graphics.taskmngr`; `os.setproc` with `"screen"`, `"title"`, `"stdout"` attributes
- `os.scope()` with no args returns the current scope; with a table, swaps the scope (`father`)
- `os.execute` refactored: support for `>`, `&&`, and `&` (background via the named class `BGRunner` to avoid the preverifier `NoClassDefFoundError`)
- Reading a file/stream with chunk size `-1` reads until end of file/connection
- `tonumber` invalid/missing-value message fixed

### Package manager (yang/pkg)

- `pkg` rewritten to v1.6.0 (see Shell & commands)
- `yang` now just calls `pkg`
- New package index: `du`, `dns`, `irc`, `nginx`, `play`, `tree` and more (mirror of 50+ packages)

### Kernel / runtime services

- New kernel request `netsh` to list opened objects
- New syscall `nice` to change process priority
- New log manager `sys/smile/logs.lua` (install with `yang install log`)
- `os.exit()`/process-death is now silent (no more `java.lang.Error` printed); real Lua/resource errors are still reported
- Lua cached tokens limited to 100 files
- Config. file `OpenRMS` no longer appears in file listings
- Fixed `id` — couldn't retrieve the id from root and other system virtual users

### Bug fixes

- `IMPLICT` → `IMPLICIT` in the `List` constructor
- `init` uses the bare command name so `exec` resolves `/bin/` correctly
- `socket.http.rget` was sending POST instead of GET (405 on package downloads) — now GET
- `>` redirection no longer dropped arguments before the operator
- `rm` on VFS subdirectories (correct exit 0, `-r`)
- silent `os.mkdir`/`os.exit`; cast fix in `deleteFile`; OOM handler with memory usage
- `pkg`/`fetch_file` with the `/apps/` URL prefix
- `_G` as the default global instead of `_ENV`

### Build / toolchain

- **`build-elf.sh`** (new) — assembles `.s`/`.c` into ARM ELF32 for the emulator (options `-o`, `-T`, `-lib`, `-entry`, `-keep`; Python validation)
- New test ELFs: `netsock`, `netudp`, `whoami`, `cat` in `res/apps/dist/`
- `res/lib/lib32.s` expanded (+602 lines); new sources `netsock.s`, `netudp.s`, `server.s`, `whoami.s`
- Removed the `j2me-lib/` tree (Android stubs)

### Deployment / infrastructure

- **`Dockerfile`** (new): PHP 8.3 FPM Alpine + nginx + supervisord; services php-fpm, nginx, Python mirror (`:31522`), pproxy (`:4096` + Flask web `:10141`)
- `docker/`: `nginx.conf`, `php.ini`, `supervisord.conf`; `pproxy` submodule in `.gitmodules`
- `krnl/`: desktop Python kernel reimplementing the runtime (`kernel.py`, `main.py`, `tkgui.py`, `lua/`)
- `index.php` expanded

### Documentation / config

- `AGENTS.md`; new `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `SECURITY.md`
- `docs/BUILD.md`, `docs/FILESYS.md`, `docs/USERS.md`, `docs/lua/README.md` expanded; 5 new examples in `docs/lua/examples/`
- Proxy/HOME_URL: `opentty.xyz` → `opentty.fun`; `RELEASE` changed from `"stable"` to `"mod"`
- New `res/template.ini`

### Lua runtime

- New global function `assert(v [, msg])`: raises an error with `msg` (default `"assertion failed!"`) when `v` is falsy (`false`/`nil`); returns all arguments unchanged when truthy
