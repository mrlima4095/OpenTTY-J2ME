# Building OpenTTY from Source

This guide explains how to compile OpenTTY in its intended environment and what
to expect from each source tree.

## Build Environments

OpenTTY's canonical source lives in `src/` and is a **MIDlet** (CLDC-1.0 /
MIDP-2.0). The real build is performed **on-device** with a J2ME toolchain.

> **Important:** there is currently **no working CI or desktop build** in the
> repository. `src/` requires a J2ME toolchain. The real build happens
> **on-device** with the J2ME SDK, producing `dist/OpenTTY.jar` + `dist/OpenTTY.jad`.

## On-Device Build

### Requirements

- A J2ME-compatible device (MIDP-2.0, CLDC-1.0)
- The **J2ME SDK Mobile** — [download `SDK.jar`](http://opentty.fun/dl/SDK.jar)
- The **OpenTTY source** (the complete repository)
- A basic file manager to navigate the device directories

### Step 1 — Download the SDK

1. Download [`SDK.jar`](http://opentty.fun/dl/SDK.jar) to your device.
2. Transfer it to the device's main storage or SD card.

### Step 2 — Access the Repository

1. Open your file manager and navigate to the OpenTTY repository folder.
2. Ensure all Java source files and resources are present.

### Step 3 — Compile with the SDK

1. Launch `SDK.jar` on the device.
2. Browse to the OpenTTY repository directory with the SDK's file browser.
3. Press the **Build** button (hammer icon).
4. Wait for compilation to complete.

## Output Files

After a successful build, check the `dist/` folder for:

- **`OpenTTY.jar`** — the main executable application file
- **`OpenTTY.jad`** — the Java application descriptor

## Installation

### Method 1 — Direct (JAR)

1. Locate `OpenTTY.jar` in `dist/`.
2. Open / execute the JAR file.
3. Follow the device's installation prompts.
4. Launch OpenTTY from the applications menu.

### Method 2 — JAD

1. Some devices prefer installation via the descriptor.
2. Open `OpenTTY.jad`; the system handles the JAR automatically.

## Verification

- Find **OpenTTY** in your applications list.
- Launch it; you should reach the command prompt.
- On first run you'll be asked to create a username/password.

---

## Working on the Source

### Sanity-checking Lua scripts

Every Lua script in the repo should load cleanly. From the repository root:

```bash
lua -e "assert(loadfile('src/bin/lua'))"
lua -e "assert(loadfile('src/bin/pkg'))"
```

### Version and project configuration

- `nbproject/project.properties` holds the NetBeans J2ME configuration:
  - `MIDlet-Version: 1.18.1`
  - JAR/JAD names `OpenTTY.jar` / `OpenTTY.jad`
- `src/` is the canonical home for all runtime logic (`Lua.java`, `OpenTTY.java`, `ELF.java`, `LuaCanvas.java`).

---

## Build Numbering

OpenTTY uses a sub-versioning system tracked in `res/build.txt`. Each build
gets a unique number that increments globally across all versions.

### Format

```
[ANO-VERSAO-BLOCOxNUMERO] Descrição curta
```

| Field | Example | Description |
|-------|---------|-------------|
| `ANO` | `2026` | Year of the build |
| `VERSAO` | `1.18.1` | MIDlet version at the time |
| `BLOCO` | `03` | Block number (increments every 100 builds) |
| `NUMERO` | `28` | Build number within the block |

The full build number is `BLOCO * 100 + NUMERO`. Example: `03x28` = build #328.

### When to increment

The build number changes **only when a structural code change occurs**, such as:
- New subsystem or major feature (e.g. ELF emulator, multi-user, VFS rewrite)
- Architecture change in `Lua.java`, `OpenTTY.java`, or `ELF.java`
- New syscall, new global library, or breaking API change in Lua runtime
- File system layout changes

Minor fixes, new apps, documentation edits, and Lua script changes do **not**
increment the build number.

### Commit message convention

When incrementing the build number, use the format:

```
Build #NUMERO - Short summary of the structural change
```

Examples:

```
Build #328 - VFS subdir mounts
Build #229 - Multi-terminal process isolation
Build #230 - ELF network syscalls (bind/listen/accept)
```

Update `res/build.txt` with a new entry at the end:

```
[2026-1.18.1-03x29] Build #229 - Multi-terminal process isolation
```

---

## Troubleshooting

### Build Issues

- Verify all source files are present and accessible.
- Check available memory on the device.
- Ensure Java permissions are set correctly.

### Installation Problems

- Try the alternative method (JAD vs JAR).
- Confirm the device supports MIDP-2.0 / CLDC-1.0.
- Check available storage space.

### Launch Failures

- Verify Java ME support on the device.
- Check application security permissions.
- Reinstall if startup keeps failing.
