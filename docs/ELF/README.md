# OpenTTY ARM ELF Emulator

OpenTTY executes little-endian ARM32 `ET_EXEC` files in `src/ELF.java`.
It is a compact emulator for J2ME, not a Linux kernel or a complete ARM
userspace. Programs must target ARMv5TE in ARM mode and remain inside the
emulator's 1 MiB guest memory.

## Build

`build-elf.sh` produces binaries accepted by OpenTTY:

```sh
./build-elf.sh docs/ELF/example.c -stdlib -o example
```

Copy `example` to the device and execute it with the OpenTTY ELF launcher:

```text
. example hello
```

The `-stdlib` option links `res/lib/libc.s`. It supplies short ARM wrappers
which call OpenTTY's built-in libc implementation. Include
`res/lib/opentty.h` instead of manually redeclaring its functions.

`-lib` selects the older assembly runtime in `res/lib/lib32.s`; it has a much
smaller API and is not a replacement for `-stdlib`.

## Supported ELF and CPU Features

- ELF32, little-endian, `EM_ARM`, `ET_EXEC`.
- ARM instructions only; compile with `-marm -march=armv5te`.
- Static executables and the project's limited `ET_DYN` loader.
- Dynamic imports using `DT_NEEDED`, PLT/GOT, `R_ARM_ABS32`, `R_ARM_COPY`,
  `R_ARM_GLOB_DAT`, `R_ARM_JUMP_SLOT`, and `R_ARM_RELATIVE`.
- Shared objects receive a load bias in a free guest-memory region. Their
  link-time virtual addresses are not used as runtime addresses, so separate
  `.so` files do not overwrite the executable or each other.
- `DT_INIT` and `DT_INIT_ARRAY` constructors run before the executable entry
  point, including constructors declared by loaded shared objects.
- `DT_FINI` and `DT_FINI_ARRAY` destructors run when the guest process exits.
- The loader resolves `DT_NEEDED` dependencies transitively and searches each
  needed `.so` in the current directory, `/lib/`, then `/bin/`. For example,
  `dcalc` requires `libutil.so`.
- Program arguments are passed through the usual initial stack layout:
  `argc`, `argv[]`, a null pointer, then `envp[]`.

Do not use Thumb code, floats, `short`, division instructions emitted for
newer ARM cores, or `long long` multiplication. `build-elf.sh` already uses
`-fno-builtin` and ARMv5TE flags for C files.

`res/apps/dist/elflife` is the dynamic-loader integration test. It loads
`libinit.so`, which transitively loads `libutil.so`, and should print the
`libinit` constructor, `elflife: result=42`, then the destructor. The matching
packages are `libutil`, `libinit`, and `elflife` in the 1.18.2 catalog.

## OpenTTY libc

`res/lib/opentty.h` declares the available API:

- Strings: `strlen`, `strcpy`, `strncpy`, `strcmp`, `strncmp`, `strcat`,
  `strncat`, `strchr`, `strdup`.
- Memory: `memcpy`, `memmove`, `memset`, `memcmp`, `memchr`.
- Conversion: `atoi`, `abs`, `toupper`, `tolower`.
- Output: `putchar`, `puts`, `printf`, `sprintf`, `snprintf`.
- Heap: `malloc`, `calloc`, `realloc`, `free`.
- Process: `getpid`, `exit`, `_exit`, `abort`.
- Raw file wrappers: `open`, `read`, `write`, `close`, `brk`.

`printf`, `sprintf`, and `snprintf` accept `%s`, `%c`, `%d`, `%i`, `%u`,
`%x`, `%X`, `%o`, `%p`, and `%%`. `l`/`h` modifiers are accepted for 32-bit
values; `ll` reads a 64-bit integer. Float conversions consume the argument
but print `0`. Width, precision, and flags are parsed but ignored.

Guest output is emitted exactly as supplied. Add `\n` in the format string
when a newline is wanted.

## Syscalls

Raw Linux ARM EABI-style syscalls use the normal convention: arguments in
`r0`-`r6`, syscall number in `r7`, then `svc #0`. OpenTTY implements a useful
subset including filesystem, process, time, memory, directory, and socket
operations. Return values are emulator values and are not a complete Linux
kernel contract.

The `-stdlib` libc itself also uses `svc #1001` through `svc #1038`. Those are
private OpenTTY library calls handled in Java. Consequently, a `-stdlib`
binary is **not** runnable directly by Linux, QEMU user mode, or native ARM32.

## Linux ARM32 Portability

The same C source can be compiled separately for Linux ARM32 with a Linux
toolchain and libc. It cannot use the same final ELF as `-stdlib`: Linux does
not implement OpenTTY's `svc #1000+` ABI. Avoid relying on OpenTTY-specific
filesystem paths and keep code within the API listed in `opentty.h` when a
source-level portable program is desired.

## Examples

Basic output and arguments:

```c
#include "opentty.h"

int main(int argc, char **argv) {
    printf("argc=%d\n", argc);
    if (argc > 1) puts(argv[1]);
    return 0;
}
```

File output:

```c
#include "opentty.h"

int main(void) {
    const char text[] = "saved by OpenTTY\n";
    int fd = open("/home/note", O_CREAT | O_WRONLY | O_TRUNC, 0644);
    if (fd < 0) return 1;
    write(fd, text, sizeof(text) - 1);
    close(fd);
    return 0;
}
```

See `res/apps/src/libc-demo.c` for a broader libc demonstration and
`res/apps/src/dcalc.c` with `res/apps/src/libutil.c` for dynamic imports.
