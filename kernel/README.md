# kernel/ — the real OpenTTY kernel

The RAM disk (`/boot/`) ships a placeholder `vmlinuz`. This directory holds the
real one: a minimal ARM32 kernel that boots directly on the OpenTTY ELF
emulator (`src/ELF.java`), the "CPU" of a J2ME device.

`ELF.java` runs static `ET_EXEC`/`EM_ARM` binaries in a 1 MB virtual RAM, with a
Linux-ARM (EABI) syscall table (`SYS_*` constants in `src/ELF.java`). That means
a kernel is just an ELF that drives the hardware (the emulator) through
`svc #0`. The emulated CPU even reports a uname: Linux 3.2.0/opentty/armv5tejl.

## What `vmlinuz.s` does at boot

1. prints the boot banner,
2. probes the heap via `brk(0)` and reports the memory top,
3. asks `uname` and prints kernel / release / version / machine,
4. opens, reads and shows `/boot/config` (KERNEL=/INIT=/ROOTFS=/SHELL=/...),
5. `execve("/bin/init")` — the Lua PID 0→1 handoff; the emulator runs
   `/bin/init` (a Lua script) exactly as the J2ME boot does. `execve` only
   returns if it fails, so the kernel then prints a halt message and exits.

Syscalls used: `open`, `read`, `close`, `write`, `brk`, `uname`, `execve`,
`exit`.

## Build

```
./kernel/build.sh
```

needs the bare-metal ARM binutils (`binutils-arm-none-eabi`) on `PATH`; it
wraps `build-elf.sh` at the repo root, which assembles and validates the header
(`ET_EXEC`, ELF32, little-endian, `EM_ARM`, entry inside the 1 MB RAM). Output:
`kernel/vmlinuz`.

## Run it on a device

Drop the ELF over the RAM-disk seed and exec it with a path, e.g. from a shell
script or loader (the emulator treats any non-text payload as an ELF):

```lua
io.popen("/boot/vmlinuz")
```

The seeded `/boot/config` already names it (`KERNEL=/boot/vmlinuz`); a future
boot-loader command can exec `KERNEL=` directly. The desktop/`src` warning
stands: only the J2ME ELF emulator can boot this — the Python ports in `lua/`
cannot execute ARM binaries.