# AGENTS.md

OpenTTY: a J2ME MIDlet (CLDC-1.0/MIDP-2.0) that is a Lua-scripted shell + ARM ELF emulator. It also contains a Docker/Coolify deployment of the web services (PHP + Python proxies).

## Source layout — what edits go where

- `src/` — canonical MIDlet source: `OpenTTY.java`, `Lua.java`, `ELF.java`, `LuaCanvas.java`, plus `src/bin` (built-in `/bin` commands, Lua scripts with `#!/bin/lua` shebang), `src/etc`, `src/lib/libcore.so` (a Lua module despite the `.so` name).
- `apps/<major>/` — the on-device app-store catalog, versioned by major release. `apps/1.18/` holds the `file/ net/ sys/ games/ dev/` dirs currently shipping with 1.18.x. Apps are registered in the mirror table in `apps/<major>/sources.lua` (e.g. `["docker"] = { remote = "sys/docker/main.lua", here = "/bin/docker", ... }`), which doubles as the `/etc/sources` file on-device (seeded from `src/etc/sources`). Adding an app means updating the app files **and** the sources mirror. New majors: copy the previous `apps/<major>/` dir; the `remote` paths stay relative to the major dir and `version = "<major>"` inside `sources.lua`.
- `res/` — embedded resources (lua modules under `res/lua/modules/`, bundled apps, pages).
- `dist/archive/<ver>` — per-version filesystem snapshots of the app store.
- `nbproject/project.properties` — NetBeans J2ME project config (MIDlet-Version 1.18.1, jar/jad names).

## Build

- There is **no working CI or desktop build** in-repo right now. `src/` needs a J2ME toolchain. The real build happens **on-device** with the J2ME SDK (see `docs/BUILD.md`), producing `dist/OpenTTY.jar` + `dist/OpenTTY.jad`.
- Sanity-check every Lua script you touch with: `lua -e "assert(loadfile('<file>'))"`.

## Lua runtime gotchas (verified in `src/Lua.java`)

- `string.format`, `string.rep`, `string.gsub`, `string.gmatch` do **not** exist. Available string funcs are only: `upper lower len find match reverse sub hash byte char trim uuid split getCommand getArgument env getpattern startswith endswith`. The `docker` app crashed with "Attempt to call a non-function value" because it used `string.format`/`string.rep` — do not reintroduce them.
- `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>` (real FS), and exactly `/bin/`, `/etc/`, `/lib/`, `/home/`; any other path yields an empty table.
- `string.startswith`/`endswith` are native — don't shadow them with Lua reimplementations.
- Daemon convention (matches how `os.request(1, "serve", path)` spawns services in `Lua.java`): daemon apps must check `arg[1] == "--deamon"` (**the typo is the convention** — other daemons in `apps/` use it), name themselves with `os.setproc("name", ...)`, and end with a top-level `return function(payload, args, scope, pid, uid) ... end` as the handler.
- `/bin/init` is PID 1 with the kernel handler: `os.request(1, payload, arg)` implements `sendsig`, `serve`, `rms`, `user`/`useradd`/`userdel`, `setsh`, `netsh`, etc.
- Java exceptions (e.g. `java.lang.NullPointerException`) carry no message. The runtime appends a **Lua-side traceback** — `Lua <file>:<line>`, a caret-backed copy of the offending source line (`^---` + `near '<token>'`), and a `stack traceback:` of the Lua function chain — via `Lua.getTraceback(e)`, used in `run()`, `pcall`, `os.request` handler failures, background threads, and UI callbacks. This depends on `Token.offset` (absolute char offset recorded in `tokenize`), `lastCode`, `frameStack`/`thrownFrames`, `thrownTokens`/`thrownTokenIndex` (set by `recordThrow()` in `run()`/`LuaFunction.call()`), `pointerBlock()`, `tokenLexeme()`, and `LuaFunction.name` — keep these in sync in `src/`. Errors contained by `pcall` (and `require`/`load` call sites via `exec`) reset the thrown state so later tracebacks point at the real spot.

## Deployment (`docker/` + root)

- `Dockerfile`: `php:8.3-fpm-alpine` + nginx + supervisor. Services started by `supervisord`: php-fpm, nginx, `python3 server.py` (TCP `:31522`, the OpenTTY mirror service), and `pproxy/app.py` (TCP `:4096` + Flask web on `:10141`).
- nginx proxies `/cli` + `/api/` to a Flask upstream at `127.0.0.1:10141`; PHP via fastcgi `:9000`; several dirs served with `autoindex`. EXPOSE: `80, 31522, 4096, 10141`.
- `pproxy` is a **git submodule**; the Dockerfile pip-installs from `pproxy/requirements.txt` plus `flask_cors requests`. Don't rely on the builder cloning submodules: the Dockerfile fell-back-clones `pproxy` from GitHub when `pproxy/app.py` is missing.

## User feedback (2026-09-05)

The user is disappointed with the automated `pkg` rewrite and scored it 0 ("nota 0"); they are rewriting `src/bin/pkg` (and the `krnl/bin/pkg` sync copy) **by hand**. Do not keep rewriting or "fixing" pkg from scratch — follow the user's hand-written version, and only make the minimal requested edits. Previous automated attempts were NOT accepted as the final word.

## Git workflow

- `origin` is SSH-over-443 with no stored credentials here:
  `git push ssh://git@ssh.github.com:443/mrlima4095/OpenTTY-J2ME.git main`
  (port 22 is blocked from this host; plain `git push` works since `origin` already points at the 443 URL.)
- User preference: commit and push completed work (via the SSH URL above) without waiting to be asked.