# Building OpenTTY from Source

This guide explains how to compile OpenTTY in its intended environment and what
to expect from each source tree.

## Build Environments

OpenTTY's canonical source lives in `src/` and is a **MIDlet** (CLDC-1.0 /
MIDP-2.0). The real build is performed **on-device** with a J2ME toolchain.

> **Important:** there is currently **no working CI or desktop build** in the
> repository.
>
> - `src/` requires a J2ME compiler (its `j2me-lib/` stubs depend on
>   `android.util` and are not usable standalone).
> - The `java/` port (the desktop stubs + runtime that mirror `src/`) compiles
>   only under its own runtime and has no `LuaCanvas.java`.
>
> Use the on-device flow below. When you edit runtime logic (`Lua.java`,
> `OpenTTY.java`, `ELF.java`), remember it is mirrored in both `src/` and
> `java/` — keep the copies in sync.

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
- Runtime logic is mirrored in `src/` and `java/` — **always update both trees**.

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
