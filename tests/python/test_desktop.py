#!/usr/bin/env python3
"""Headless end-to-end smoke tests of the OpenTTY desktop kernel (lua/sys).

Boots the Python port of the J2ME kernel in a throwaway device root and drives
real shell commands through it. Mirrors what a MIDP device runs on boot: PID 1
(/bin/init), /etc seeded from src/, /bin commands loaded from src/bin and the
apps mirror.

Each check runs in its own isolated devroot + keys file, so a failure leaves no
state behind and CI runs are reproducible.

Exit code 0 = all checks passed, 1 = at least one failed, 2 = infrastructure.
"""

import os
import re
import subprocess
import sys
import tempfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))


def run_cmd(cmd, cwd, user="tester"):
    """Run one headless -e command. Returns (status, stdout_lines)."""
    devroot = tempfile.mkdtemp(prefix="opentty-test-")
    keys = os.path.join(tempfile.mkdtemp(prefix="opentty-keys-"), "keys")
    try:
        proc = subprocess.run(
            [sys.executable, "-m", "lua.sys",
             "--root", devroot, "--keys", keys, "--user", user,
             "--norepl", "-e", cmd],
            cwd=cwd, capture_output=True, text=True, timeout=120,
        )
        lines = proc.stdout.splitlines()
        # Drop the motd banner that precedes the command output.
        cleaned = [ln for ln in lines
                   if "Welcome to OpenTTY" not in ln and "Copyright (C)" not in ln]
        return proc.returncode, cleaned, proc.stderr
    finally:
        for p in (devroot, os.path.dirname(keys)):
            subprocess.run(["rm", "-rf", p], capture_output=True)


def check(name, cmd, expect_substr=None, expect_line=None, status=0, extra=None):
    rc, out, err = run_cmd(cmd, ROOT)
    joined = "\n".join(out)
    ok = True
    why = []
    if rc != status:
        ok, why = False, ["status=%d (wanted %d)" % (rc, status)]
    elif expect_substr is not None and expect_substr not in joined:
        ok, why = False, ["missing %r in output: %r" % (expect_substr, joined)]
    elif expect_line is not None and expect_line not in out:
        ok, why = False, ["missing line %r in output: %r" % (expect_line, out)]
    if extra:
        for cond, desc in extra:
            if not cond:
                ok, why = False, [desc]
    tag = "PASS" if ok else "FAIL"
    print("%s %s" % (tag, name))
    if not ok:
        for w in why:
            print("   " + w)
        if err:
            print("   stderr: " + err[:300])
    return ok


def main():
    results = []

    results.append(check(
        "boot: uname advertises OpenTTY",
        "uname", expect_substr="OpenTTY", extra=[("1.18.2" in "\n".join(run_cmd("cat /etc/sources", ROOT)[1]),
                                                  "/etc/sources did not list the 1.18.2 catalog")]))

    results.append(check("boot: echo passes args", "echo hello world", expect_line="hello world"))
    results.append(check("boot: ls / lists the virtual mounts",
                         "ls /",
                         expect_substr="bin/\tdev/\tetc/\thome/\tlib/\tmnt/\tproc/\ttmp/"))
    results.append(check("boot: whoami reports the session user", "whoami", expect_line="tester"))
    results.append(check("boot: pwd starts at /home/", "pwd", expect_line="/home/"))
    results.append(check("boot: id shows uid=1000 for the main user", "id", expect_substr="uid=1000"))
    results.append(check("boot: seeded /etc/hostname is read", "cat /etc/hostname", expect_line="localhost"))
    results.append(check("boot: /etc/sources seeds the 1.18.2 appstore",
                         "cat /etc/sources", expect_substr='version = "1.18.2"'))
    results.append(check("boot: /bin seeded from src/bin",
                         "ls /bin/", expect_substr="pkg\t"))
    results.append(check("boot: pkg version reports the 1.18.2 database",
                         "pkg version", expect_substr="1.18.2"))
    results.append(check("boot: expr evaluates (integer)",
                         "expr 2 + 3", expect_substr="5"))

    # A built-in Lua command reading a file through io.* (the same path lazy cat
    # uses), then an ELF-launching path: uname reads /etc/os-release.
    results.append(check("cmd: memelf queries the emulator RAM size",
                         "memelf", expect_substr="max ELF RAM"))

    failed = sum(1 for ok in results if not ok)
    total = len(results)
    print("test_desktop: %d/%d passed" % (total - failed, total))
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()