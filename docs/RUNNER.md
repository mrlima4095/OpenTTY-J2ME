# Desktop runner (`pc/run.sh`)

`pc/run.sh` runs the *real* OpenTTY MIDlet (`src/`) on a desktop JVM using the
`pc/j2me` bindings. It is the fastest way to boot OpenTTY, run a Lua app or a
RISC-V ELF at startup, try boot entries, and debug without a J2ME device.

Nothing in `src/` is modified. A work copy (`pc/work/`) receives mechanical,
behavior-preserving patches that modern `javac` needs (single-type `List`
import), plus two bug fixes that only bite on the desktop:

- `Lua.run()` re-runs the statement loop from a stale token cursor — reset it.
- `open("/bin/.")` resolves to the *bin directory* (an empty stream) and masks
  a `. file` command — keep `"."` out of the `/bin/<name>` exec branch.

## Requirements

- JDK 17+ (`java`, `javac` on `PATH`)
- Python 3 (for the source patcher)

## Usage

```
pc/run.sh [options] [--] [command tokens...]
```

With no arguments it boots the normal system interactively. Command tokens are
executed on the shell console once the system is up.

| Option | Meaning |
| --- | --- |
| `root=PATH` | Boot root: `"/"` (default, normal OpenTTY) or a host directory copied into `data/mnt/<name>` and used as a chroot (guest path `/mnt/<name>/...`). |
| `init=PATH` | Boot init program (guest path or host file, default `/bin/init`). Runs as PID 1 and must be a Lua script. |
| `--user NAME` | OpenTTY user (default `$OPENTTY_USER` or `opentty`). |
| `--smoke` | Headless boot check: boot, dump display + processes, drive the console with the command (or `echo hello`), exit. |
| `--watchdog` | Dump all thread stacks and exit 9 if boot hangs; exit 3 on a boot throw. |
| `--cmd CMD` | Execute one command string at boot (same as the token form). |
| `--` | Everything after this is the command, even if it looks like an option. |
| `-h`, `--help` | Show help. |

JVM options go through `OPENTTY_JVM_OPTS`, e.g.:

```
OPENTTY_JVM_OPTS="-Dopentty.watchdog=1 -Dopentty.execdbg=1" pc/run.sh
```

## Running a program at boot

Host file paths in the command are **staged** into `data/mnt/opentty/` and
rewritten to the guest path `/mnt/opentty/<name>`. A staged program is launched
automatically, whether it is a Lua script or a RISC-V ELF (resolved by content,
exactly like the real shell):

```
pc/run.sh -- /tmp/app.lua --flag v     # stage + run a Lua app
pc/run.sh -- /tmp/rvtest.elf           # stage + run a RISC-V ELF
pc/run.sh -- /tmp/app.lua              # boot the app, interactive
pc/run.sh --smoke -- /tmp/count.elf    # boot, run the ELF, dump, exit
```

Program output is printed on the shell console (`io.write`/`printf`), and/or the
app can take over the screen with its own LCDUI Form.

## Boot entries

A full grub-style boot can be driven from the command line:

```
pc/run.sh init=./init.lua          # Lua script as PID 1
pc/run.sh root=/path/to/rootfs     # chroot a host directory, /mnt/<name>
pc/run.sh root=/path rc=...        # custom entries via the app store, etc.
pc/run.sh root=/path/to/rootfs init=/bin/init
```

- `init=` files and `root=` directories are staged into `data/mnt/` the same
  way command files are.
- Gain root: `-Dopentty.user=root` or set the MIDlet password.

## Desktop JAR

To build a standalone desktop JAR (all of `src/`, `pc/j2me`, `pc/app` and the
guest `/bin /boot /etc /lib` resources packed together):

```
pc/build-jar.sh
java -jar dist/OpenTTY-desktop-1.18.2.jar -- /tmp/app.lua
```

The JAR accepts the same options as `pc/run.sh`. Per-boot state (RecordStores,
mounts) lives under `data/` by default.

## Smoke / watchdog

- `--smoke` boots headless, fires the command through the console Run handler,
  dumps the current display and every process (name + captured `stdout`), then
  exits 0.
- `--watchdog` boots and polls every 5 s: 2 clean ticks == OK (exit 0); a null
  display after 15 s dumps all stacks and exits 9; a boot exception exits 3.

## Debugging

- `-Dopentty.execdbg=1` traces every `exec()` dispatch (`main` + argv).
- `-Dopentty.repro=1` runs a Lua probe inside the xterm's interpreter that
  reports `io.popen`/`os.execute` results directly to stdout.