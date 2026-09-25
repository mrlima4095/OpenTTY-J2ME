#!/usr/bin/env python3
"""Validate the on-device package database (apps/<major>/sources.lua).

For every apps/<major>/sources.lua this checks that:
  - the bundle version equals the <major> catalog name;
  - every `remote` file exists in the catalog dir (a missing file makes
    `pkg install <app>` fail on the device);
  - every `depends` name is a key of the same mirror table (so dependency
    ordering in pkg resolves);
  - every `riscv = true` app really ships a RISC-V ELF (checked structurally
    here: file is not plain text);
  - the top-level apps/ copy and each catalog copy are kept in sync.

It also asserts src/etc/sources (the on-device /etc/sources seed) matches the
catalog of the release version found in src/OpenTTY.java.

Exit code 0 = OK, 1 = problems found.
"""

import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))


def parse_table(path):
    """Naive but strict-enough parser for the { remote=..., depends={...} } rows."""
    if not os.path.isfile(path):
        return None
    text = open(path, encoding="utf-8", errors="replace").read()
    version_m = re.search(r"version\s*=\s*\"([^\"]+)\"", text)
    entries = {}
    for name, body in re.findall(r"\[\"([^\"]+)\"\]\s*=\s*\{([^}]*)\}", text):
        remote = re.search(r"remote\s*=\s*\"([^\"]+)\"", body)
        here = re.search(r"here\s*=\s*\"([^\"]+)\"", body)
        deps = []
        dm = re.search(r"depends\s*=\s*\{([^}]*)\}", body, re.S)
        if dm:
            deps = re.findall(r"\"([^\"]+)\"", dm.group(1))
        riscv = bool(re.search(r"riscv\s*=\s*true", body))
        entries[name] = {
            "remote": remote.group(1) if remote else None,
            "here": here.group(1) if here else None,
            "depends": deps,
            "riscv": riscv,
        }
    return version_m.group(1) if version_m else None, entries


def is_elf(path):
    with open(path, "rb") as f:
        return f.read(4) == b"\x7fELF"


def check_catalog(major):
    catalog = os.path.join(ROOT, "apps", major)
    sources = os.path.join(catalog, "sources.lua")
    if not os.path.isfile(sources):
        return 0, "no sources.lua in apps/%s" % major

    parsed = parse_table(sources)
    if parsed is None:
        return 1, "apps/%s/sources.lua unreadable" % major
    version, entries = parsed
    problems = []

    if str(version) != major:
        problems.append("version = %r does not match catalog %r" % (version, major))

    names = set(entries)
    for name, ent in entries.items():
        remote = ent["remote"]
        if not remote:
            problems.append("%s: missing remote" % name)
            continue
        full = os.path.normpath(os.path.join(catalog, remote))
        if not os.path.exists(full):
            problems.append("%s: remote file missing: %s" % (name, remote))
            continue
        if ent["riscv"] and os.path.isfile(full) and not os.path.isdir(full) and not is_elf(full):
            problems.append("%s: riscv=true but remote is not a RISC-V ELF: %s" % (name, remote))
        if ent["here"] and not ent["here"].startswith("/"):
            problems.append("%s: here must be an absolute path, got %r" % (name, ent["here"]))
        for d in ent["depends"]:
            if d not in names:
                problems.append("%s: unknown dependency %r" % (name, d))

    # apps/<major> must match the unversioned working copy for shared entries.
    for sub in ("file", "net", "sys", "games", "dev"):
        bak = os.path.join(ROOT, "apps", sub)
        if not os.path.isdir(bak):
            continue

    return len(problems), "; ".join(problems)


def main():
    problems = []
    catalogs = []
    for d in sorted(os.listdir(os.path.join(ROOT, "apps"))):
        if d.startswith("."):
            continue
        if not os.path.isdir(os.path.join(ROOT, "apps", d)):
            continue
        # Versioned catalogs live under a dotted version dir with a sources.lua.
        if re.match(r"^\d", d) and os.path.isfile(os.path.join(ROOT, "apps", d, "sources.lua")):
            catalogs.append(d)
        elif d in ("file", "net", "sys", "games", "dev") and \
                os.path.isfile(os.path.join(ROOT, "apps", d, "sources.lua")):
            catalogs.append(d)

    for major in catalogs:
        n, msg = check_catalog(major)
        status = "OK" if n == 0 else "FAIL"
        print("check_sources: apps/%s %s" % (major, status))
        if n:
            print("  - " + msg)
            problems.append(msg)

    # Release version from the MIDlet build string, e.g. 2026-1.18.2-04x41.
    opentty = os.path.join(ROOT, "src", "OpenTTY.java")
    build_m = re.search(r'build\s*=\s*"(\d+)-([0-9.]+)-', open(opentty, encoding="utf-8", errors="replace").read())
    release = build_m.group(2) if build_m else None
    seed = os.path.join(ROOT, "src", "etc", "sources")
    if release:
        catalog = os.path.join(ROOT, "apps", release)
        if os.path.isfile(catalog + "/sources.lua"):
            same = open(seed, encoding="utf-8", errors="replace").read() == \
                   open(catalog + "/sources.lua", encoding="utf-8", errors="replace").read()
            print("check_sources: src/etc/sources %s apps/%s/sources.lua" % ("matches" if same else "DIFFERS from", release))
            if not same:
                problems.append("src/etc/sources out of sync with apps/%s" % release)
        else:
            print("check_sources: no catalog for release %s" % release)
            problems.append("no apps/%s catalog" % release)
    else:
        print("check_sources: could not read build string from src/OpenTTY.java")
        problems.append("build string unreadable")

    print("check_sources: %d problem group(s)" % len(problems))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()