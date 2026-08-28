#!/usr/bin/env python3
"""
OpenTTY Python 1.18 — desktop launcher.

Boots the OpenTTY kernel (this package, krnl/) on plain Python 3 instead of the
J2ME MIDlet. The filesystem is mounted on a real directory of the host (default:
the current directory; src/{bin,etc,lib} are mirrored into it on first boot) and
the account is stored in ~/.opentty-keys.

Usage:
    python -m krnl                     boot and open the interactive shell
    python -m krnl -e '<command>'      run a single shell command
    python -m krnl <script.lua> [..]   run a Lua script
    python -m krnl --root <dir>        mount the filesystem in <dir>
    python -m krnl --keys <file>       credentials file (default ~/.opentty-keys)
    python -m krnl --user <name>       force the main account name
    python -m krnl --hostname <host>   host name (default opentty)
    python -m krnl --norepl            exit after boot / command / script
    python -m krnl --version
"""

import argparse
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from krnl.kernel import OpenTTYKernel, OPEN_VERSION, OPEN_BUILD  # noqa: E402

__version__ = OPEN_VERSION


def build_parser():
    ap = argparse.ArgumentParser(
        prog="opentty",
        description="OpenTTY Python %s — OpenTTY kernel on Python 3 (no J2ME)." % OPEN_VERSION,
    )
    ap.add_argument("--root", metavar="DIR", default=None,
                    help="device root directory (default: the current directory)")
    ap.add_argument("--keys", metavar="FILE", default=None,
                    help="credentials file (default: ~/.opentty-keys)")
    ap.add_argument("--user", metavar="NAME", default=None,
                    help="main account name (default: session user)")
    ap.add_argument("--hostname", metavar="HOST", default="opentty",
                    help="host name (default: opentty)")
    ap.add_argument("-e", dest="exec_cmd", metavar="CMD", default=None,
                    help="execute one shell command after boot and exit")
    ap.add_argument("--norepl", action="store_true",
                    help="with no command/script, exit after boot instead of opening the shell")
    ap.add_argument("--version", action="store_true", help="print version and exit")
    ap.add_argument("script", nargs="?", default=None, help="Lua script to run")
    ap.add_argument("script_args", nargs=argparse.REMAINDER, help="arguments for the script")
    return ap


def main(argv=None):
    ap = build_parser()
    args = ap.parse_args(argv)

    if args.version:
        print("OpenTTY Python %s (build %s)" % (OPEN_VERSION, OPEN_BUILD))
        return 0

    if args.script is not None and args.script.startswith("-"):
        ap.error("unexpected option: %s" % args.script)

    kernel = OpenTTYKernel(devroot=args.root, keys=args.keys,
                           username=args.user, hostname=args.hostname)

    # First run: create the account (interactive prompt, or auto keys otherwise).
    user = kernel.ensure_login()
    if user is None:
        return 1

    # Boot PID 1 (/bin/init) exactly like the MIDlet entry point does.
    try:
        status = kernel.boot()
    except KeyboardInterrupt:
        print()
        return 130

    # Whatever was started becomes the foreground program, like MIDP startApp.
    if args.exec_cmd is not None:
        if args.exec_cmd.strip():
            return int(float(kernel.os_execute(args.exec_cmd) or 0))
        return int(status or 0)
    if args.script is not None:
        return int(float(kernel.run_script(args.script, args.script_args or ()) or 0))
    if args.norepl:
        return int(status or 0)
    kernel.repl()
    return int(getattr(kernel.runtime, "status", 0) or 0)


if __name__ == "__main__":
    sys.exit(main())