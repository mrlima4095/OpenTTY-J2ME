#!/usr/bin/env python3
"""
OpenTTY Lua Runtime — CLI entry point.
Run OpenTTY Lua scripts on your PC.

Usage:
    python lua/run.py <script> [args...]
    python lua/run.py -e "code"
    python lua/run.py          (interactive REPL)
"""

import sys
import os

# Add parent dir to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from lua.runtime import LuaRuntime, LuaError, LuaExit


def main():
    runtime = LuaRuntime()

    # Set up initial scope
    runtime.scope = {
        "USER": os.environ.get("USER", "user"),
        "PWD": "/home/",
        "ROOT": "",
        "VERSION": "1.18.1",
        "SHELL": "/bin/sh",
        "HOSTNAME": "opentty-pc",
        "ALIAS": {},
    }
    runtime._scope = runtime.scope

    # Set up default env
    runtime.attributes["VERSION"] = "1.18.1"
    runtime.attributes["REPO"] = "socket://opentty.fun:31522"
    runtime.attributes["SHELL"] = "/bin/sh"

    # Mount etc from source
    etc_content = ""
    for name in ("motd", "hostname", "fstab"):
        path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "etc", name)
        if os.path.isfile(path):
            with open(path, "r", errors="replace") as f:
                runtime.vfs[f"/etc/{name}"] = f.read()

    if len(sys.argv) < 2:
        repl(runtime)
        return

    if sys.argv[1] == "-e":
        code = sys.argv[2] if len(sys.argv) > 2 else ""
        arg_table = {0.0: "-e"}
        for i, a in enumerate(sys.argv[3:]):
            arg_table[float(i + 1)] = a
        status = runtime.run("-e", code, arg_table)
        sys.exit(status)

    script = sys.argv[1]
    if script == "-":
        code = sys.stdin.read()
        arg_table = {0.0: "-"}
        for i, a in enumerate(sys.argv[2:]):
            arg_table[float(i + 1)] = a
        status = runtime.run("-", code, arg_table)
        sys.exit(status)

    # Read script file
    if not os.path.isfile(script):
        # Try /bin/<script>
        bin_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "src", "bin", script)
        if os.path.isfile(bin_path):
            script = bin_path
        else:
            print(f"{script}: not found", file=sys.stderr)
            sys.exit(127)

    with open(script, "r", errors="replace") as f:
        code = f.read()

    arg_table = {0.0: script}
    for i, a in enumerate(sys.argv[2:]):
        arg_table[float(i + 1)] = a

    status = runtime.run(script, code, arg_table)
    sys.exit(status)


def repl(runtime):
    """Simple interactive REPL."""
    print("OpenTTY Lua REPL (Python runtime)")
    print("Type 'exit' to quit.\n")

    buf = []
    while True:
        try:
            line = input(">> " if not buf else ".. ")
        except (EOFError, KeyboardInterrupt):
            print()
            break

        if line.strip() == "exit":
            break
        if not line.strip() and not buf:
            continue

        buf.append(line)
        code = "\n".join(buf)

        try:
            runtime.last_code = code
            runtime.current_source = "repl"
            runtime.tokens = runtime.tokenize(code)
            runtime.token_index = 0

            while runtime.peek().type != 0:  # EOF
                result = runtime.statement(runtime.scope)
                if runtime.doreturn:
                    runtime.doreturn = False
                    break
                if result is not None:
                    from lua.runtime import LuaFunction
                    if result is not None and not isinstance(result, LuaFunction):
                        print(runtime.to_lua_string(result))
        except LuaExit:
            break
        except LuaError as e:
            print(f"Error: {e}", file=sys.stderr)
            buf.clear()
            continue
        except Exception as e:
            print(f"Error: {e}", file=sys.stderr)
            buf.clear()
            continue

        buf.clear()


if __name__ == "__main__":
    main()
