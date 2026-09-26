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

## First boot (like the real MIDlet)

A first run with no credentials opens the **OpenTTY - Login** form and asks you
to create a user and password — exactly like the J2ME MIDlet. After saving
them you get the *Reopen MIDlet* alert, close the window and reopen: OpenTTY
then boots to the console. Everything (credentials, stores, the VFS index)
persists under `data/rms`, so the second boot skips the login.

For headless/scripted runs, pre-seed the credentials instead:

```
pc/run.sh --user opentty --pass opentty          # first boot straight to console
pc/run.sh --smoke --user opentty --pass opentty  # smoke needs credentials too
```

Without `--user`/`--pass` the first run shows the login form (device behavior).

## Boot menu

Like the device, a **OpenTTY - Boot** list menu appears whenever `/boot/grub.cfg`
contains more than one `menuentry` (the shipped default has one and boots
straight). Boot entries written by the guest (e.g. a root `init=` script doing
`io.write("/boot/grub.cfg", cfg)`) are persisted in the RMS and shown on the
next start.

| Option | Meaning |
| --- | --- |
| `root=PATH` | Boot root: `"/"` (default, normal OpenTTY) or a host directory copied into `data/mnt/<name>` and used as a chroot (guest path `/mnt/<name>/...`). |
| `init=PATH` | Boot init program (guest path or host file, default `/bin/init`). Runs as PID 1 and must be a Lua script. |
| `--user NAME` | OpenTTY username. First boot with no credentials opens the login form (device behavior); `--user` seeds the username (password prompt only). Default `$OPENTTY_USER` (empty). |
| `--pass PASS` | Seed the password together with `--user` to skip the login form (headless/smoke runs). |
| `--smoke` | Headless boot check: boot, dump display + processes, drive the console with the command (or `echo hello`), exit. |
| `--watchdog` | Dump all thread stacks and exit 9 if boot hangs; exit 3 on a boot throw. |
| `--cmd CMD` | Execute one command string at boot (same as the token form). |
| `--` | Everything after this is the command, even if it looks like an option. |
| `-h`, `--help` | Show help. |

JVM options go through `OPENTTY_JVM_OPTS`, e.g.:

```
OPENTTY_JVM_OPTS="-Dopentty.watchdog=1 -Dopentty.execdbg=1" pc/run.sh
```

## Desktop window quirks

- Form rows fill the full column width: the console area starts flush at the
  left edge (no large empty margin), and the input row sits right below it.
- **Enter** in the xterm input row runs the command (same as pressing the
  focused **Run** button); the Run button itself also answers Enter/Space when
  focused.
- When the MIDlet is destroyed (Exiting, typing `exit`, emulator end),
  `MIDlet.notifyDestroyed()` closes the window and exits the JVM — no zombie
  process is left behind.

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

> Non-reparenting window managers (bspwm, i3, dwm, awesome, xmonad...) never
> send OpenJDK AWT the `ReparentNotify` it waits for, so the window can open as
> a blank grey canvas. `pc/run.sh` sets `_JAVA_AWT_WM_NONREPARENTING=1`
> automatically; when launching the JAR with `java -jar`, set it yourself:
>
> ```
> _JAVA_AWT_WM_NONREPARENTING=1 java -jar dist/OpenTTY-desktop-1.18.2.jar
> ```
>
> It is harmless on reparenting WMs (GNOME/Cinnamon/KDE).

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