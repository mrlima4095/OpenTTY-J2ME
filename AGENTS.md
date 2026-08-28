# AGENTS.md

OpenTTY: a J2ME MIDlet (CLDC-1.0/MIDP-2.0) that is a Lua-scripted shell + ARM ELF emulator. It also contains a Docker/Coolify deployment of the web services (PHP + Python proxies).

## Source layout — what edits go where

- `src/` — canonical MIDlet source: `OpenTTY.java`, `Lua.java`, `ELF.java`, `LuaCanvas.java`, plus `src/bin` (built-in `/bin` commands, Lua scripts with `#!/bin/lua` shebang), `src/etc`, `src/lib/libcore.so` (a Lua module despite the `.so` name).
- `java/` — desktop port: `j2me.java` (J2ME API stub layer), copies of the same runtime `.java` files, and compiled `.class`. **Runtime logic (Lua.java/OpenTTY.java/ELF.java) is mirrored in both trees — when changing it, update both `src/` and `java/`.** The `java/` port has no `LuaCanvas.java`.
- `apps/` — the on-device app-store catalog (dirs `file/ net/ sys/ games/ dev/`), installed via `yang`/`pkg`. Apps are registered in the mirror tables in `src/bin/pkg` and `java/bin/pkg` (e.g. `["docker"] = { remote = "sys/docker/main.lua", here = "/bin/docker", ... }`). Adding an app means updating the app files **and** the catalog table.
- `res/` — embedded resources (lua modules under `res/lua/modules/`, bundled apps, pages).
- `dist/archive/<ver>` — per-version filesystem snapshots of the app store.
- `nbproject/project.properties` — NetBeans J2ME project config (MIDlet-Version 1.18.1, jar/jad names).

## Build

- There is **no working CI or desktop build** in-repo right now. `src/` needs a J2ME toolchain (`j2me-lib/` stubs require `android.util`, and are not usable standalone); the `java/` port compiles only under its own runtime. The real build happens **on-device** with the J2ME SDK (see `docs/BUILD.md`), producing `dist/OpenTTY.jar` + `dist/OpenTTY.jad`.
- Sanity-check every Lua script you touch with: `lua -e "assert(loadfile('<file>'))"`.

## Lua runtime gotchas (verified in `src/Lua.java`)

- `string.format`, `string.rep`, `string.gsub`, `string.gmatch` do **not** exist. Available string funcs are only: `upper lower len find match reverse sub hash byte char trim uuid split getCommand getArgument env getpattern startswith endswith`. The `docker` app crashed with "Attempt to call a non-function value" because it used `string.format`/`string.rep` — do not reintroduce them.
- `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>` (real FS), and exactly `/bin/`, `/etc/`, `/lib/`, `/home/`; any other path yields an empty table.
- `string.startswith`/`endswith` are native — don't shadow them with Lua reimplementations.
- Daemon convention (matches how `os.request(1, "serve", path)` spawns services in `Lua.java`): daemon apps must check `arg[1] == "--deamon"` (**the typo is the convention** — other daemons in `apps/` use it), name themselves with `os.setproc("name", ...)`, and end with a top-level `return function(payload, args, scope, pid, uid) ... end` as the handler.
- `/bin/init` is PID 1 with the kernel handler: `os.request(1, payload, arg)` implements `sendsig`, `serve`, `rms`, `user`/`useradd`/`userdel`, `setsh`, `netsh`, etc.
- Java exceptions (e.g. `java.lang.NullPointerException`) carry no message. The runtime appends a **Lua-side traceback** — `Lua <file>:<line>`, a caret-backed copy of the offending source line (`^---` + `near '<token>'`), and a `stack traceback:` of the Lua function chain — via `Lua.getTraceback(e)`, used in `run()`, `pcall`, `os.request` handler failures, background threads, and UI callbacks. This depends on `Token.offset` (absolute char offset recorded in `tokenize`), `lastCode`, `frameStack`/`thrownFrames`, `thrownTokens`/`thrownTokenIndex` (set by `recordThrow()` in `run()`/`LuaFunction.call()`), `pointerBlock()`, `tokenLexeme()`, and `LuaFunction.name` — keep these in sync in both `src/` and `java/`. Errors contained by `pcall` (and `require`/`load` call sites via `exec`) reset the thrown state so later tracebacks point at the real spot.

## Deployment (`docker/` + root)

- `Dockerfile`: `php:8.3-fpm-alpine` + nginx + supervisor. Services started by `supervisord`: php-fpm, nginx, `python3 server.py` (TCP `:31522`, the OpenTTY mirror service), and `pproxy/app.py` (TCP `:4096` + Flask web on `:10141`).
- nginx proxies `/cli` + `/api/` to a Flask upstream at `127.0.0.1:10141`; PHP via fastcgi `:9000`; several dirs served with `autoindex`. EXPOSE: `80, 31522, 4096, 10141`.
- `pproxy` is a **git submodule**; the Dockerfile pip-installs from `pproxy/requirements.txt` plus `flask_cors requests`. Don't rely on the builder cloning submodules: the Dockerfile fell-back-clones `pproxy` from GitHub when `pproxy/app.py` is missing.

## Git workflow

- `origin` is HTTPS with no stored credentials here; push auth works via SSH:
  `git push ssh://git@github.com/mrlima4095/OpenTTY-J2ME.git main`
- User preference: commit and push completed work (via the SSH URL above) without waiting to be asked.