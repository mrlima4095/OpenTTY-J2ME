OpenTTY Java Edition 1.18.1
Copyright (C) 2026 - Mr. Lima

---

## HEAD (unreleased) — work since tag 1.18

Highlights implemented between the `1.18` tag and the latest commit (`HEAD`). (Current build is still labeled `2026-1.18.1-03x28`.)

### multi-tasking / process management

- `Process` now has its own `Displayable screen` (registered via `os.setproc("screen", ...)`) and the global `midlet.stdout`/`midlet.stdin` were **removed** — each process has its own output buffer (per-process `StringBuffer`) and its own input `TextField`
- New **per-process** socket tracking (`Process.net`) replaces the global `midlet.network`
- New **Task Manager** `graphics.taskmngr()` (a `List` `"Running"` using `List.IMPLICIT`), listing every process as `"title/name [pid]"`:
  - **Back**: if no processes remain, calls `destroyApp` (closes the MIDlet); otherwise returns to the previous screen
  - **Interrupt (SIGTERM)**: sends signal 15 to the selected process's `sighandler` and removes it from the list
  - **SELECT**: switches `display.setCurrent()` to the process's screen (process switching)
- `os.setproc("title", ...)` sets the title shown in the Task Manager (`title [pid]` instead of `name [pid]`)
- Process scope is cloned by `cloneScope()` in `os.popen`/`serve`, so `su`/`cd` in one terminal no longer affect the others
- **`/bin/xterm`** (new): the terminal emulator was extracted from `init` — it creates its own stdout/stdin/screen, `[USER@HOST PWD] #/$` prompt, "Run" command and "Switch to..." button (task manager); shows `/etc/motd` on open
- **`/bin/init`** simplified: it only mounts fstab, configures env, `os.su`, runs `/home/.initrc` and calls `os.execute("xterm")`; it also accepts `--serve=<program>` to spawn daemons
- `graphics.display()` clears `kill` when showing a `Displayable`, keeping the runtime alive with a foreground screen

### elf / network

- **Full socket API** restored in the ELF emulator:
  - `bind`: opens `StreamConnectionNotifier`/`DatagramConnection` on the given port, returns `-EADDRINUSE` on conflict
  - `listen`: marks the socket as listening, with ephemeral-port fallback
  - `accept`: `acceptAndOpen`, new fd with streams + peer `sockaddr_in` returned to the caller
  - `sendto`/`recvfrom`: DGRAM via `DatagramConnection`, TCP via streams, with peer writeback
  - `shutdown` and `nanosleep` restored (success no-ops)
- **Socket options**: `setsockopt`/`getsockopt` with an options table (`SO_REUSEADDR`, `SO_KEEPALIVE`, `SO_OOBINLINE`, `SO_BROADCAST`, `SO_DONTROUTE`, `SO_LINGER`, `SO_SNDBUF`, `SO_RCVBUF`, `TCP_NODELAY`); `SO_ERROR` and `SO_TYPE`; `-ENOPROTOOPT` for unsupported options
- New constants: `SOL_SOCKET`, `SOL_IP`, `TCP_NODELAY`, `SO_*`; new errors `ENOTSOCK`, `ENOPROTOOPT`, `EADDRINUSE`, `EADDRNOTAVAIL`, `EISCONN`
- Fixed `writeSockAddr` (avoids the cast to `short` that broke SDK codegen)
- A failed `connect` sets `socketInfo["error"] = 111`
- ELF `listdir` now sees the virtual `/proc/` (pid entries, `cpuinfo`, `meminfo`, `uptime`, `version`) via `midlet.procEntries()`
- Shutdown cleanup also closes `datagram` connections

### filesystem / vfs / proc

- Virtual `/proc/`: `uptime`, `version`, `meminfo` (now using `Runtime.totalMemory`, since CLDC has no `maxMemory`), `cpuinfo` and `/proc/<pid>/` dirs with `status`, `cmdline`, `comm`, `stat`; regular users only see their own processes (root sees all)
- `/root/` (`OpenRMS` index 6) — protected directory, only root can read/write/enter; `rms` stays root-only
- VFS subdirectories under `/bin/`, `/etc/`, `/lib/` with stable hashing (indices `>= 9`, `VFS_HASH_MOD=97`) and persistence in `/etc/vfs.conf` (restored on mount)
- `fstab` updated: adds `root/` to the root mount, and `mkdir`, `pkg`, `xterm` to `/bin/`

### shell / commands

- **`/bin/sh`** cut from 284 → 32 lines: builtins are now handled by the kernel/`os.execute`; preserves `-c`, file execution and interactive mode
- **`/bin/pkg`** (rewritten, v1.6.0): uses `socket.http.get`/`rget` instead of raw TCP; server `http://opentty.fun` (`REPO` override); commands `install`, `remove`, `update`, `list`, `info`, `download`, `run`; mirror with 50+ packages
- **`/bin/yang`** slimmed to a wrapper that forwards args to `pkg`
- **`/bin/mkdir`** (new): creates VFS directories; root-only under `/bin`, `/etc`, `/lib`, `/root`
- **`/bin/lua`** now runs files directly (`lua <file>`)
- **`/bin/cp`** supports a single-argument mode (`cp file` copies to `file-copy`)
- **`/bin/rm`** accepts `-r`/`-rf`/`-fr` and multiple files
- **`/bin/nano`**: "Add new line" button (for J2EMU) and "Switch to..." (task manager)
- **`/bin/curl`**: fixed URL parsing (`sub(1,5)` for `"http:"`)

### new /bin apps

- **`xterm`** — terminal emulator (see multi-tasking)
- **`irc`** (`apps/net/irc.lua`) — IRC client with CLI (`connect`/`send`) and GUI modes, join/part/nick/PRIVMSG, PING/PONG, MOTD
- **`play`** (`apps/file/play.lua`) — audio player (play/stop/pause/resume/status/volume/list) driving the `audio-codec` daemon; GUI mode
- **`tree`** (`apps/file/tree.lua`) — directory tree viewer (`-d`, `-L N`, `-a`, counts)
- **`nginx`** (`apps/net/nginx/main.lua`) — nginx-style HTTP server: config `/etc/nginx/nginx.conf`, `mime.types`, `sites-enabled/`, static serving, `proxy_pass`, per-location alias, access/error logging
- **`dns`** (`apps/net/dns/main.lua`) — DNS server daemon: reads `/etc/hosts` and `/etc/dns/*.zone` zones, answers A/AAAA/MX/CNAME, stats, `lookup`/`add`/`remove`/`reload`/`list`
- **`head`, `tail`, `netstat`** — implemented (head/tail print the first/last N lines; netstat tests connectivity via HTTP GET)

### updated apps

- **`jdb`** (`apps/sys/benchmark/main.lua`, +388 lines) — adb-style debug bridge: `ps`, `getproc`, `dumpsys`, `logcat`, `users`, `crash`, `stack`, `meminfo`, `shell`, `connect` (TCP/UDP client); server mode on port 5555
- **`sudo`** — now reads all args (`arg[1]` is the command, the rest are forwarded)
- **`docker`** — expanded container management, images and init scripts
- **`httpd`** (`res/lua/modules/httpd.lua`) — new `httpd.static(root_dir)` function to serve static files with MIME detection

### lua runtime

- **performance optimizations**:
  - `SMALL_NUMBERS[-128..1023]` — cached small `Double`s for loops/indices
  - `ScopeTable` (chained scope) instead of cloning the globals table on every function call
  - `StringBuffer` concatenation; static `Boolean` `TRUE/FALSE`; per-source tokenizer caching; early-returns in `getpattern`/`replace`/`escape`
- **errors / traceback**: new `getTraceback(Throwable)` emits a `Frame` stack trace (name, source, line), `pointerBlock()` with `^---` and `(near '<token>')`, injected into `run()`, `pcall`, `os.request` handlers, background threads and UI callbacks
- **new functions**: `string.startswith`, `string.endswith`, `table.pack`, `graphics.taskmngr`; `os.setproc` with `"screen"`, `"title"`, `"stdout"` attributes
- `os.scope()` with no args returns the current scope; with a table, swaps the scope (`father`)
- `os.execute` refactored: support for `>`, `&&`, and `&` (background via the named class `BGRunner` to avoid the preverifier `NoClassDefFoundError`)

### bug fixes

- `IMPLICT` → `IMPLICIT` in the `List` constructor
- `init` uses the bare command name so `exec` resolves `/bin/` correctly
- `socket.http.rget` was sending POST instead of GET (405 on package downloads) — now GET
- `/proc/meminfo` uses `Runtime.totalMemory` instead of `maxMemory`
- `>` redirection no longer dropped arguments before the operator
- `rm` on VFS subdirectories (correct exit 0, `-r`)
- silent `os.mkdir`/`os.exit`; cast fix in `deleteFile`; OOM handler with memory usage
- `pkg`/`fetch_file` with the `/apps/` URL prefix
- `_G` as the default global instead of `_ENV`

### build / toolchain

- **`build-elf.sh`** (new) — assembles `.s`/`.c` into ARM ELF32 for the emulator (options `-o`, `-T`, `-lib`, `-entry`, `-keep`; Python validation)
- New test ELFs: `netsock`, `netudp`, `whoami`, `cat` in `res/apps/dist/`
- `res/lib/lib32.s` expanded (+602 lines); new sources `netsock.s`, `netudp.s`, `server.s`, `whoami.s`
- Removed the `j2me-lib/` tree (Android stubs)

### deployment / infrastructure

- **`Dockerfile`** (new): PHP 8.3 FPM Alpine + nginx + supervisord; services php-fpm, nginx, Python mirror (`:31522`), pproxy (`:4096` + Flask web `:10141`)
- `docker/`: `nginx.conf`, `php.ini`, `supervisord.conf`; `pproxy` submodule in `.gitmodules`
- `krnl/`: desktop Python kernel reimplementing the runtime (`kernel.py`, `main.py`, `tkgui.py`, `lua/`)
- `index.php` expanded

### documentation / config

- `AGENTS.md`; new `CODE_OF_CONDUCT.md`, `CONTRIBUTING.md`, `SECURITY.md`
- `docs/BUILD.md`, `docs/FILESYS.md`, `docs/USERS.md`, `docs/lua/README.md` expanded; 5 new examples in `docs/lua/examples/`
- Proxy/HOME_URL: `opentty.xyz` → `opentty.fun`; `RELEASE` changed from `"stable"` to `"mod"`
- New `res/template.ini`

---

## Build 2026-1.18.1-03x28

filesystem / vfs

- Subdirectories inside `/bin/`, `/etc/`, `/lib/` are now supported as RMS mounts
- Each subdir (e.g. `/bin/tools/`) maps to its own page (index) of the `OpenRMS` store via a stable path hash (indices >= 6), keeping the every-store-in-OpenRMS model (no new RecordStores cluttering `/home/`)
- Read, write and delete now resolve nested paths at any depth (`/bin/tools/sub/file.lua`)
- Directory listing (`io.dirs` and ELF `getdents`) works for any `/bin|etc|lib/...` folder
- Writing into a subdir auto-registers it in the VFS (`cd` + `ls` detect it)
- `rms` handler can clear subdir stores (`rm -r /bin/tools`)
- Declare subdirs in `/etc/fstab` under their parent line using a trailing `/` (e.g. `tools/`)
- Refactored `addFile` to operate on a store index instead of a base string
- Root's home is now `/root/` (OpenRMS index 6, same top-level style as `/bin`→3, `/lib`→4, `/etc`→5); regular users cannot read, write or enter it, and `rms` stays root-only
- VFS hash subdirectories now use indices `>= 9` (`VFS_RESERVED` moved from 6 to 9), leaving indices 7 and 8 free for future top-level mounts without remapping
- `/root/` is declared in `/etc/fstab` root line, `su`, `chdir` and directory listing all enforce the root-home rule
- Fixed `rm` in `/bin|etc|lib/` subdirs: it no longer errors with exit code 5 (read-only) — `deleteFile` now removes a registered VFS subdirectory (clears its OpenRMS store and drops it from the `fs` table), and `rm` accepts `-r`/`-rf`/`-fr`
- Entering `/root/` without permission now returns exit code 13 and the shell prints `cd: <dir>: permission denied`
- Default shell home is now `/home/` for every user (including root after `su`); `/root/` remains as a protected directory but login/boot does not jump to it
- `/proc/` virtual filesystem: `cpuinfo`, `meminfo`, `uptime`, `version` plus per-process dirs `/proc/<pid>/` with `cmdline`, `comm`, `stat`, `status`; regular users only see/read their own processes (root sees all), enforced in file reads, directory listing and `cd`
- VFS subdirs under `/bin/`, `/etc/`, `/lib/` now persist across restarts into `/etc/vfs.conf` (written on mkdir, removed on delete/`rms`, restored on mount) so created folders survive closing the MIDlet

runtime / exit

- `os.exit()` no longer prints `java.lang.Error` on the terminal when it kills a process
- Process death via `os.exit` (any exit code), status abort and `Process killed` is now silent
- Real Lua errors and resource errors are still reported

shell / commands

- New `mkdir` command (`/bin/mkdir`) to create directories, including VFS subdirectories under `/bin/`, `/etc/`, `/lib/`
- `os.mkdir` now creates VFS subdirectory mounts (via `registerVfsDir`) in addition to `/mnt/`
- `mkdir` under `/bin/`, `/etc/`, `/lib/`, `/root/` is now root-only (exit 13 for regular users), consistent with write/delete rules

---

bug fixes

- fixed a bug in id, it can't retrieve id from root and another system virtual users

general

- Added Kernel request `netsh` to get opened objects
- New syscall added `nice` to change process priority
- New log manager `sys/smile/logs.lua`, install with `yang install log`
- Limited Lua cached tokens to 100 files
- Native Shell `os.execute(cmd)` still wrote in Java
- SheBang `#!/bin/sh` on `. [file]` run file with shell
- Added the **Add new line** button in Nano Editor
- Config. file `OpenRMS` doesn't appear in file listings
- New sh label
- Allowed multiple terminals

lua

- Added functions `string.startswith(s, pattern)` and `string.endswith(s, pattern)`
- Read a file or stream with chunk size `-1` will read until end of file/ connection end
- fixed `tonumber` invalid or missing value message

elf

- 

yang - package manager

- Updated to `1.5.1`
- New command `download [pkg] [file]` to download a package without install it
- New command `run [file]` to run installation scripts
- Fixed **not found** message that disappears from stdout
- Command `yang install *` install all available packages
- Indexed new package `du` 
- `yang` linked to `pkg` 
