# Contributing to OpenTTY

First off, thanks for taking the time to contribute!

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
  - [Reporting Bugs](#reporting-bugs)
  - [Suggesting Features](#suggesting-features)
  - [Development Setup](#development-setup)
- [Source Layout](#source-layout)
- [Code Style & Gotchas](#code-style--gotchas)
- [Pull Request Workflow](#pull-request-workflow)

## Code of Conduct

This project and everyone participating in it is governed by the
[OpenTTY Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are
expected to uphold this code. Please report unacceptable behavior to
**opentty@proton.me**.

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report, please check the issue tracker — you might find
that it has already been reported. When creating a report, include as much
detail as possible:

- a clear, descriptive title
- the exact steps to reproduce the problem
- the behavior you observed after following the steps
- the behavior you expected instead, and why
- screenshots or error messages if possible
- which JVM / MIDP environment you're using
- which OpenTTY version you're running

**Example:**

```markdown
## Bug Report

**OpenTTY Version:** 2026-1.18.1
**Environment:** Nokia S40, J2ME Loader 1.7.2

**Description:**
When I run `/bin/init`, I get a Kernel Panic:
`java.lang.ArrayIndexOutOfBoundsException at ELF.java:2456`

**Steps to Reproduce:**
1. Install OpenTTY
2. Launch the MIDlet
3. See the error immediately

**Expected Behavior:**
OpenTTY should boot normally to the console.
```

### Suggesting Features

Open an issue describing the feature, why it's useful, and — if possible — how
you'd approach implementing it. Larger ideas can also be added to the
[roadmap](ROADMAP.md).

### Development Setup

OpenTTY has **no working CI or desktop build** in-repo; the real build happens
**on-device** with a J2ME SDK (see [docs/BUILD.md](docs/BUILD.md)). When working
on the source:

- The canonical runtime lives in `src/` (`Lua.java`, `OpenTTY.java`, `ELF.java`,
  `LuaCanvas.java`).
- Lua scripts under `src/bin` should be sanity-checked with:
  `lua -e "assert(loadfile('<file>'))"`.

## Source Layout

- `src/` — canonical MIDlet source (Java runtime + built-in `/bin` Lua commands)
- `apps/<major>/` — on-device app-store catalog, versioned by major release (e.g. `apps/1.18/`)
- `res/` — embedded resources (Lua modules, bundled apps, pages)
- `dist/archive/<ver>` — per-version filesystem snapshots of the app store
- `docker/` — web services deployment (PHP + Python proxies)

## Code Style & Gotchas

- **Do not comment** J2ME/Lua runtime code unless the comment is essential.
- Follow the existing naming and formatting style of the file you edit.
- **Lua `string` library**: `string.format`, `string.rep`, `string.gsub`, and
  `string.gmatch` do **not** exist. Don't introduce them. Use native
  `string.startswith` / `string.endswith` rather than Lua reimplementations.
- `io.dirs(path)` returns entries only for `/tmp/`, `/mnt/<sub>`, and exactly
  `/bin/`, `/etc/`, `/lib/`, `/home/`.
- **Daemon convention**: daemon apps must check `arg[1] == "--deamon"` (the typo
  is intentional), name themselves with `os.setproc("name", ...)`, and end with
  a top-level `return function(payload, args, scope, pid, uid) ... end` handler.
- Java exceptions carry no message; the runtime appends a Lua-side traceback.
- When adding an app to the store, update the app files **and** the catalog
  table in the `sources.lua` of your release's `apps/<major>/` dir (mirrored
  on-device at `/etc/sources`, seeded from `src/etc/sources`).

## Pull Request Workflow

1. Fork the repository and create a branch from `main`.
2. Make your changes, keeping commits focused and explaining why.
3. Sanity-check any Lua you touch with `lua -e "assert(loadfile('<file>'))"`.
4. Open a pull request against `main` and describe the change and how to verify it.

Thank you for helping make OpenTTY better!
