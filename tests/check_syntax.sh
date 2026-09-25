#!/bin/sh
# Lua syntax check across the whole repo.
#
# Every .lua file and every script with a #!/bin/lua shebang must parse as
# plain Lua (the on-device Lua.java implements the standard grammar). A parse
# error here means the script cannot boot on a real device.
#
# Requires: lua 5.x on PATH (for `assert(loadfile(...))`).

set -u

ROOT="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$ROOT"

command -v lua >/dev/null 2>&1 || { echo "check_syntax: 'lua' not found on PATH" >&2; exit 2; }

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

# Candidates: every *.lua/*.yang, plus non-Lua files whose first line is a
# #!...lua shebang (e.g. the /bin scripts shipped without a .lua suffix).
{
    find src apps res -type f \( -name '*.lua' -o -name '*.yang' \) -print 2>/dev/null
    find src apps res -type f -not -name '*.lua' -print 2>/dev/null | while IFS= read -r f; do
        [ -s "$f" ] || continue
        case "$(head -n 1 "$f")" in
            \#!*lua*) printf '%s\n' "$f" ;;
        esac
    done
} | sort -u > "$WORK/candidates"

fails=0
checked=0
while IFS= read -r f; do
    checked=$((checked + 1))
    if ! lua -e "assert(loadfile('$f'))" >/dev/null 2>&1; then
        echo "SYNTAX FAIL: $f"
        lua -e "assert(loadfile('$f'))" 2>&1 | head -3
        fails=$((fails + 1))
    fi
done < "$WORK/candidates"

echo "check_syntax: $checked candidates parsed, $fails failures"
[ "$fails" -eq 0 ]