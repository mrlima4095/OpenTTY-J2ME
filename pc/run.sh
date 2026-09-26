#!/usr/bin/env bash
# Run the real OpenTTY MIDlet (src/) on the desktop using the pc/j2me
# J2ME bindings. Nothing in src/ is modified: a work copy under pc/work
# receives two mechanical, behavior-preserving patches that modern javac
# needs (CLDC has no java.util.List, so src treats "List" as the LCDUI
# List; and LCDUI types are imported with java.util.*):
#   1. add "import javax.microedition.lcdui.List;" (single-type import wins
#      over the wildcard imports) to OpenTTY.java and Lua.java;
#   2. rename a local "arg" shadowing the method-level "arg" in Lua.java.
#
# The runner accepts boot options (root=/init=) and an optional command
# line to execute on the OpenTTY console once the system is up. Host file
# paths in the command are staged into data/mnt/opentty/ so the guest can
# reach them at /mnt/opentty/. See docs/RUNNER.md for the full reference.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD="$ROOT/pc/build"
WORK="$ROOT/pc/work"
DATA="$ROOT/data"

cd "$ROOT"

usage() {
    cat <<'EOF'
Usage: ./run.sh [options] [--] [command tokens...]

Runs the real OpenTTY MIDlet (src/) on the desktop with the pc/ shim.

The optional command tokens are executed on the OpenTTY console once the
system has booted. Host file paths in the command are staged into
data/mnt/opentty/ and rewritten to /mnt/opentty/<name>; a staged program
is launched automatically whether it is a Lua script or a RISC-V ELF
(resolved by content, exactly like the real shell).

Options:
  root=PATH      Boot root: "/" (default, normal OpenTTY) or a host
                 directory copied into data/mnt and used as a chroot
                 (guest path /mnt/<name>/...).
  init=PATH      Boot init program (guest path, default /bin/init). The
                 program runs as PID 1 and must be a Lua script,
                 e.g.  ./run.sh init=./init.lua
  --user NAME    OpenTTY user (default: $OPENTTY_USER or opentty).
  --smoke        Headless boot check: boot, dump state, drive the console
                 with the command (or 'echo hello opentty desktop'), exit.
  --cmd CMD      Execute one command string at boot (same as token form).
  --             Everything after this is the command, even if a token
                 looks like an option.
  -h, --help     Show this help.

Examples:
  ./run.sh                          interactive boot (default system)
  ./run.sh -- echo hello world      shell builtin after boot
  ./run.sh -- /tmp/app.lua --flag v stage + run a Lua app at boot
  ./run.sh -- /tmp/rvtest.elf       stage + run a RISC-V ELF at boot
  ./run.sh init=./init.lua          boot a Lua script as PID 1
  ./run.sh root=/path/to/rootfs init=/bin/init

JVM options: OPENTTY_JVM_OPTS, e.g.
  OPENTTY_JVM_OPTS="-Dopentty.watchdog=1" ./run.sh   # dump stacks on hang
EOF
}

smoke=0
watchdog=0
user="${OPENTTY_USER:-opentty}"
boot_root="/"
boot_init="/bin/init"
cmd_tokens=()
after_sep=false

i=1
while [ $i -le $# ]; do
    arg="${!i}"
    if [ "$after_sep" = true ]; then
        cmd_tokens+=("$arg")
    else
        case "$arg" in
            -h|--help) usage; exit 0 ;;
            root=*) boot_root="${arg#root=}" ;;
            init=*) boot_init="${arg#init=}" ;;
            --user)
                i=$((i + 1))
                [ $i -le $# ] || { echo "--user requires a name" >&2; exit 2; }
                user="${!i}"
                ;;
            --user=*) user="${arg#--user=}" ;;
            --smoke) smoke=1 ;;
            --watchdog) watchdog=1 ;;
            --cmd|--) after_sep=true ;;
            *)
                cmd_tokens+=("$arg")
                after_sep=true
                ;;
        esac
    fi
    i=$((i + 1))
done

# Stage a host path so the emulated guest can read it under /mnt/opentty/.
stage() {
    [ -e "$1" ] || { echo -n ""; return; }
    mkdir -p "$DATA/mnt/opentty"
    local name
    name="$(basename "$1")"
    if [ ! -f "$DATA/mnt/opentty/$name" ] || ! cmp -s "$1" "$DATA/mnt/opentty/$name"; then
        cp -f "$1" "$DATA/mnt/opentty/$name"
    fi
    echo -n "/mnt/opentty/$name"
}

# Rewrite host file paths inside the command to guest /mnt/... paths, then
# join the tokens with plain spaces (the guest shell splits on spaces).
first_was_staged=false
command=""
if [ ${#cmd_tokens[@]} -gt 0 ]; then
    out=()
    n=0
    for tok in "${cmd_tokens[@]}"; do
        staged="$(stage "$tok")"
        if [ -n "$staged" ]; then
            if [ "$n" = "0" ]; then first_was_staged=true; fi
            out+=("$staged")
        else
            out+=("$tok")
        fi
        n=$((n + 1))
    done
    command="${out[*]}"
    # A staged program is launched automatically (Lua or ELF by content).
    if [ "$first_was_staged" = true ]; then
        command=". $command"
    fi
fi

# root= may point at a host directory -> stage it as a chroot under /mnt/.
if [ "$boot_root" != "/" ]; then
    if [[ "$boot_root" == /mnt/* ]]; then
        : # already a guest mount path
    elif [ -d "$boot_root" ]; then
        name="$(basename "$boot_root")"
        mkdir -p "$DATA/mnt/$name"
        cp -rf "$boot_root"/. "$DATA/mnt/$name"/ 2>/dev/null || true
        boot_root="/mnt/$name"
    else
        echo "[run.sh] warning: root=$boot_root is not a host directory" >&2
    fi
fi

# init= may point at a host file -> stage it under /mnt/opentty/.
if [ "$boot_init" != "/bin/init" ] && [[ "$boot_init" != /mnt/* ]] && [ -f "$boot_init" ]; then
    boot_init="$(stage "$boot_init")"
fi

JVM=()
[ "$boot_root" != "/" ] && JVM+=("-Dopentty.bootRoot=$boot_root")
[ "$boot_init" != "/bin/init" ] && JVM+=("-Dopentty.bootInit=$boot_init")
[ -n "$command" ] && JVM+=("-Dopentty.cmd=$command")
[ "$smoke" = "1" ] && JVM+=("-Dopentty.smoke=1")
[ "$watchdog" = "1" ] && JVM+=("-Dopentty.watchdog=1")

mkdir -p "$BUILD" "$DATA/rms" "$DATA/mnt"
rm -rf "$WORK/src"
mkdir -p "$WORK"
cp -r src "$WORK/src"

echo "[run.sh] patching work copy (src/ stays untouched)..."
python3 "$ROOT/pc/patch_src.py" "$WORK/src"

echo "[run.sh] compiling src/ + pc/j2me bindings..."
JAVA_FILES=$(find "$ROOT/pc/j2me" -name '*.java' | sort)
javac -nowarn -encoding UTF-8 -d "$BUILD" \
    "$WORK"/src/*.java \
    "$ROOT/pc/app"/*.java \
    $JAVA_FILES

echo "[run.sh] boot root=$boot_root init=$boot_init command=${command:-<none>}"
echo "[run.sh] launching OpenTTY..."

JAVA=java
if [ -x "$ROOT/pc/java" ]; then
    JAVA="$ROOT/pc/java"
fi

exec "$JAVA" \
    ${OPENTTY_JVM_OPTS:-} \
    "${JVM[@]+"${JVM[@]}"}" \
    -Dopentty.rms="$DATA/rms" \
    -Dopentty.mnt="$DATA/mnt" \
    -Dopentty.user="$user" \
    -cp "$BUILD:$ROOT/src" \
    OpenTTYMain "$@"

exit 0