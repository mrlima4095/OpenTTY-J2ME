# AGENTS.md

OpenTTY: a J2ME MIDlet (CLDC-1.0/MIDP-2.0) that is a Lua-scripted shell + ARM ELF emulator. It also contains a Docker/Coolify deployment of the web services (PHP + Python proxies).

## Source layout — what edits go where

- `src/` — canonical MIDlet source: `OpenTTY.java`, `Lua.java`, `ELF.java`, `LuaCanvas.java`, plus `src/bin` (built-in `/bin` commands, Lua scripts with `#!/bin/lua` shebang), `src/etc`, `src/boot` (kernel/ramdisk seed files), `src/lib/libcore.so` (a Lua module despite the `.so` name).
- `apps/<major>/` — the on-device app-store catalog, versioned by major release. `apps/1.18/` holds the `file/ net/ sys/ games/ dev/` dirs currently shipping with 1.18.x. Apps are registered in the mirror table in `apps/<major>/sources.lua` (e.g. `["docker"] = { remote = "sys/docker/main.lua", here = "/bin/docker", ... }`), which doubles as the `/etc/sources` file on-device (seeded from `src/etc/sources`). Adding an app means updating the app files **and** the sources mirror. New majors: copy the previous `apps/<major>/` dir; the `remote` paths stay relative to the major dir and `version = "<major>"` inside `sources.lua`.
- `res/` — embedded resources (lua modules under `res/lua/modules/`, bundled apps, pages). `res/lib/` holds the guest C runtime for the ELF emulator: `lib32.s` (full asm stdlib, linked with `-lib`) and `libc.s` (thin `svc #LIB_*; bx lr` wrappers, linked with `-stdlib`).
- `build-elf.sh` — builds ARM32 ELFs for the emulator (`ET_EXEC`, ELF32 LE, `EM_ARM`, entry ≤ 1 MB). Flags: `-lib` (link `res/lib/lib32.s`, asm stdlib), `-stdlib` (link `res/lib/libc.s`, emulator stdlib — **mutually exclusive with `-lib`**), `-T <addr>`, `-entry <sym>`, `-keep`. `.c` files compile with `-marm -march=armv5te -fno-builtin` so gcc never emits `sdiv/udiv/movw/movt`.
- `lua/` — the **host-side** ports of the J2ME runtime, all runnable with plain Python 3: `lua/runtime.py` (port of `src/Lua.java`), `lua/run.py` (CLI harness, seeds `/etc/` and `/boot/` from `src/`), and `lua/sys/` (the desktop kernel — ports of `src/OpenTTY.java`/`src/Lua.java`, formerly `krnl/`; run with `python3 -m lua.sys`, mounts a real dir instead of RMS).
- `kernel/` — the **real kernel**: an ARM32 assembly kernel (`vmlinuz.s`) built into an `ET_EXEC`/`EM_ARM` ELF (`kernel/vmlinuz`) that boots on the J2ME ELF emulator (`src/ELF.java`). Build with `./kernel/build.sh` (needs `binutils-arm-none-eabi`; the ELF is validated by `build-elf.sh`). `src/boot/vmlinuz` is its seed placeholder on the RAM disk.
- `dist/archive/<ver>` — per-version filesystem snapshots of the app store.
- `nbproject/project.properties` — NetBeans J2ME project config (MIDlet-Version 1.18.1, jar/jad names).

## Build

- There is **no working CI or desktop build** in-repo right now. `src/` needs a J2ME toolchain. The real build happens **on-device** with the J2ME SDK (see `docs/BUILD.md`), producing `dist/OpenTTY.jar` + `dist/OpenTTY.jad`.
- Sanity-check every Lua script you touch with: `lua -e "assert(loadfile('<file>'))"`.

## Lua runtime gotchas (verified in `src/Lua.java`)

- `string.format`, `string.rep`, `string.gsub`, `string.gmatch` do **not** exist. Available string funcs are only: `upper lower len find match reverse sub hash byte char trim uuid split getCommand getArgument env getpattern startswith endswith`. The `docker` app crashed with "Attempt to call a non-function value" because it used `string.format`/`string.rep` — do not reintroduce them.
- `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>` (real FS), and exactly `/bin/`, `/etc/`, `/lib/`, `/boot/`, `/home/`; any other path yields an empty table.
- `string.startswith`/`endswith` are native — don't shadow them with Lua reimplementations.
- Daemon convention (matches how `os.request(1, "serve", path)` spawns services in `Lua.java`): daemon apps must check `arg[1] == "--deamon"` (**the typo is the convention** — other daemons in `apps/` use it), name themselves with `os.setproc("name", ...)`, and end with a top-level `return function(payload, args, scope, pid, uid) ... end` as the handler.
- `/bin/init` is PID 1 with the kernel handler: `os.request(1, payload, arg)` implements `sendsig`, `serve`, `rms`, `user`/`useradd`/`userdel`, `setsh`, `netsh`, etc.
- Java exceptions (e.g. `java.lang.NullPointerException`) carry no message. The runtime appends a **Lua-side traceback** — `Lua <file>:<line>`, a caret-backed copy of the offending source line (`^---` + `near '<token>'`), and a `stack traceback:` of the Lua function chain — via `Lua.getTraceback(e)`, used in `run()`, `pcall`, `os.request` handler failures, background threads, and UI callbacks. This depends on `Token.offset` (absolute char offset recorded in `tokenize`), `lastCode`, `frameStack`/`thrownFrames`, `thrownTokens`/`thrownTokenIndex` (set by `recordThrow()` in `run()`/`LuaFunction.call()`), `pointerBlock()`, `tokenLexeme()`, and `LuaFunction.name` — keep these in sync in `src/`. Errors contained by `pcall` (and `require`/`load` call sites via `exec`) reset the thrown state so later tracebacks point at the real spot.

## ELF emulator C stdlib (`src/ELF.java` + `res/lib/libc.s`)

- Guest C library via lightweight SYSCALL wrappers: `svc #(1000 + id); bx lr` (`LIB_BASE=1000`). Implemented in `ELF.java` as `handleLibraryCall`; ids 1000–1038 cover string (`strlen…strdup`), `atoi`/`abs`/`toupper`/`tolower`, memory (`memcpy/memmove/memset/memcmp/memchr`), IO (`putchar/puts/printf/sprintf/snprintf`), heap (`malloc/calloc/realloc/free`), `getpid` + AEABI helpers (`__aeabi_ldivmod`, `__aeabi_udivmod`, `__aeabi_memclr/memcpy/memset/memmove[4|8]`). `exit/_exit/abort` = `svc #1`. Raw Linux-arg syscalls (`write/read/open/close/brk`) = `mov r7,#N; svc #0`.
- Guest `_start` CRT reads args from the stack (not r0): `[sp]=argc`, `[sp+4]=argv`, envp after the NULL.
- `printf`/`sprintf` support `%s %c %d %i %u %x %X %o %p %%` with `l`/`h` (32-bit) and `ll` (64-bit); `%f/%e/%g` consume a 64-bit arg and print `0` (no float). Flags/width/precision parsed and ignored.
- Varargs follow AAPCS: 32-bit args arrive in r1,r2,r3 then `[sp],[sp+4]…`; a 64-bit (`long long`) arg must live in an **even** register pair (printf's first `%lld` goes to r2:r3, skipping r1) and on the stack is 8-aligned **with padding words**. `ELF.java` walks params via `libcArgReg`/`libcArgSpOff` (`libcNext32`/`libcNext64`) — keep in sync.
- Heap: first-fit with split, header `{size,next}` at payload−8, new regions from `findFreeMemoryRegion` (64 KB), no coalescing.
- `ldrd`/`strd` (immediate, pre/post + writeback) are decoded in the halfword/doubleword family: `(instr & 0x0E400090)==0x00400090` with opcode bits 6‑5 (`10`=LDRD, `11`=STRD) and bit 20 = 0 — do not reuse the classic `0x0E40_0F00` masks.
- Demo: `res/apps/src/libc-demo.c` → `./build-elf.sh res/apps/src/libc-demo.c -stdlib -o demo`. Typecheck after edits: `javac -d /tmp/jc -cp . src/ELF.java $STUBS` with stubs in `/tmp/opencode/tty-stubs`.

## Deployment (`docker/` + root)

- `Dockerfile`: `php:8.3-fpm-alpine` + nginx + supervisor. Services started by `supervisord`: php-fpm, nginx, `python3 server.py` (TCP `:31522`, the OpenTTY mirror service), and `pproxy/app.py` (TCP `:4096` + Flask web on `:10141`).
- nginx proxies `/cli` + `/api/` to a Flask upstream at `127.0.0.1:10141`; PHP via fastcgi `:9000`; several dirs served with `autoindex`. EXPOSE: `80, 31522, 4096, 10141`.
- `pproxy` is a **git submodule**; the Dockerfile pip-installs from `pproxy/requirements.txt` plus `flask_cors requests`. Don't rely on the builder cloning submodules: the Dockerfile fell-back-clones `pproxy` from GitHub when `pproxy/app.py` is missing.

## Git workflow

- `origin` is SS//git@ssh.github.com:443/mrlima4095/OpenTTY-J2ME.git main`
  (port 22 is blocked from this host; plain `git push` works since `origin` already points at the 443 URL.)
- User preference: commit and push completed work (via the SSH URL above) without waiting to be asked.