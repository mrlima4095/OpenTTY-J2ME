# C and RISC-V ELF Development

OpenTTY runs small native RISC-V programs in `src/ELF.java`. This guide covers
the target, build process, C libraries, LCDUI, shared objects, and packaging.
It describes an OpenTTY guest ABI, not Linux, glibc, newlib, or generic RISC-V.

## Contents

- [Target](#target)
- [Quick start](#quick-start)
- [Build script](#build-script)
- [C runtime rules](#c-runtime-rules)
- [OpenTTY C library](#opentty-c-library)
- [Files and console I/O](#files-and-console-io)
- [Processes and environment](#processes-and-environment)
- [LCDUI applications](#lcdui-applications)
- [Dynamic libraries](#dynamic-libraries)
- [Assembly and raw syscalls](#assembly-and-raw-syscalls)
- [On-device compiler](#on-device-compiler)
- [Packaging](#packaging)
- [Testing and troubleshooting](#testing-and-troubleshooting)

## Target

OpenTTY accepts:

- ELF32, little-endian, `EM_RISCV` (`243`).
- `ET_EXEC` applications and `ET_DYN` shared objects.
- RV32I plus the M extension: `-march=rv32im -mabi=ilp32`.
- Executable entry addresses below `0x100000`; total guest memory is 1 MiB.

Do not target ARM, x86, RVC/compressed instructions, floating point, atomics,

The runtime starts a normal C entry point:

```c
int main(int argc, char **argv)
{
    return 0;
}
```

`argv[0]` is the program path and `argv[argc]` is null. `envp` follows the
`argv` terminator on the initial stack. Returning from `main` exits with that
status.

Two project runtimes exist:

| Option | Runtime | Use |
| --- | --- | --- |
| `-stdlib` | `res/lib/libc.s` | Recommended for new C code. Provides startup and wrappers for OpenTTY libraries. |
| `-lib` | `res/lib/lib32.s` | Legacy full assembly runtime. Keep it for existing code whose symbols or behavior require it. |

`-stdlib` and `-lib` cannot be combined.

## Quick start

Install a RISC-V GNU toolchain. On Debian/Ubuntu:

```sh
sudo apt install gcc-riscv64-unknown-elf binutils-riscv64-unknown-elf
```

`build-elf.sh` also recognizes `riscv64-unknown-linux-gnu-`,
`riscv64-linux-gnu-`, and `riscv32-unknown-elf-`. Set `CROSS` if needed:

```sh
CROSS=riscv64-unknown-elf- ./build-elf.sh res/apps/src/hello.c -stdlib \
  -o res/apps/dist/hello
```

Keep project sources under `res/apps/src/` so the supplied headers can be
included relatively:

```c
/* res/apps/src/hello.c */
#include "../../lib/opentty.h"

int main(int argc, char **argv)
{
    printf("Hello from OpenTTY, pid=%d\n", getpid());
    if (argc > 1) printf("Argument: %s\n", argv[1]);
    return 0;
}
```

Build it from the repository root:

```sh
./build-elf.sh res/apps/src/hello.c -stdlib -o res/apps/dist/hello
```

The script validates ELF class, byte order, type, machine, and entry address.
Copy the result to `/bin/` on the device, or package it as described below.

## Build script

```text
./build-elf.sh input.c -stdlib -o output
```

| Option | Meaning |
| --- | --- |
| `-o <file>` | Output filename. Default: first source basename. |
| `-T <addr>` | Start `.text` at an address. Default: `0x10000` for executables and `0x20000` for shared objects. |
| `-stdlib` | Link `res/lib/libc.s`; use for most C programs. |
| `-lib` | Link legacy `res/lib/lib32.s`. |
| `-shared` | Produce an `ET_DYN` shared object. |
| `-entry <symbol>` | Executable entry symbol; default `_start`. Keep it for ordinary `main` programs. |
| `-keep` | Keep intermediate objects beside the output. |

The script compiles C using `-march=rv32im -mabi=ilp32 -nostdlib -static` and
`-fno-builtin`. It does not accept arbitrary compiler flags or include paths.
For external projects, copy `opentty.h` and `lcdui.h` into the source tree.

Multiple C or assembly sources can be compiled together:

```sh
./build-elf.sh res/apps/src/otp_main.c res/apps/src/otp_compress.c \
  res/apps/src/otp_decompress.c -stdlib -o res/apps/dist/otp
```

## C runtime rules

There is no host C library. Do not include `<stdio.h>`, `<stdlib.h>`,
`<string.h>`, `<unistd.h>`, `<errno.h>`, or other system headers. Include:

```c
#include "../../lib/opentty.h"
```

- `int`, `long`, and pointers are 32-bit (`ilp32`).
- `long long` is supported by the supplied RV32 runtime helpers.
- Integer multiplication, division, and remainder are supported.
- Do not use `float` or `double`. `%f`, `%e`, and `%g` consume an argument but
  print `0`.
- Code, data, stack, heap, and loaded libraries share 1 MiB of guest RAM.
- `malloc` is first-fit and does not coalesce free blocks. Free temporary
  buffers and avoid repeated mixed-size allocations.
- `read(0, ...)` currently returns zero bytes. Use arguments, files, or LCDUI
  fields for user input.

`-stdlib` supplies `_start`, which calls `main` and exits when it returns.

## OpenTTY C library

`res/lib/opentty.h` is the public C API. `res/lib/libc.s` turns these calls into
private OpenTTY library syscalls implemented by `src/ELF.java`; a `-stdlib`
binary cannot run directly on Linux, QEMU user mode, or a hardware board.

### Strings and memory

```c
int strlen(const char *s);
char *strcpy(char *dst, const char *src);
char *strncpy(char *dst, const char *src, size_t n);
int strcmp(const char *a, const char *b);
int strncmp(const char *a, const char *b, size_t n);
char *strcat(char *dst, const char *src);
char *strncat(char *dst, const char *src, size_t n);
char *strchr(const char *s, int c);
char *strdup(const char *s);
void *memcpy(void *dst, const void *src, size_t n);
void *memmove(void *dst, const void *src, size_t n);
void *memset(void *dst, int c, size_t n);
int memcmp(const void *a, const void *b, size_t n);
void *memchr(const void *s, int c, size_t n);
```

`strcpy`, `strcat`, and `sprintf` do not know destination capacity. Allocate
enough space yourself; prefer `snprintf` for bounded formatting.

### Conversion, output, and heap

```c
int atoi(const char *s);
int abs(int value);
int toupper(int c);
int tolower(int c);
int putchar(int c);
int puts(const char *s);
int printf(const char *format, ...);
int sprintf(char *dst, const char *format, ...);
int snprintf(char *dst, size_t size, const char *format, ...);
void *malloc(size_t size);
void *calloc(size_t count, size_t size);
void *realloc(void *ptr, size_t size);
void free(void *ptr);
int getpid(void);
void exit(int status);
void _exit(int status);
void abort(void);
```

Formatting supports `%s`, `%c`, `%d`, `%i`, `%u`, `%x`, `%X`, `%o`, `%p`, and
`%%`. `h` and `l` are accepted for 32-bit values; `ll` reads a 64-bit integer.
Flags, width, and precision are parsed but ignored. `puts` appends a newline;
`printf` does not.

```c
char text[48];
snprintf(text, sizeof(text), "pid=%d hex=%08x", getpid(), 0x2a);
puts(text);
```

Always check allocations:

```c
char *copy = strdup("OpenTTY");
if (copy == 0) return 1;
puts(copy);
free(copy);
```

## Files and console I/O

The standard runtime exposes a small OpenTTY file ABI:

```c
int read(int fd, void *buf, size_t count);
int write(int fd, const void *buf, size_t count);
int open(const char *path, int flags, int mode);
int close(int fd);
int brk(void *address);
```

Use `O_RDONLY`, `O_WRONLY`, `O_RDWR`, `O_CREAT`, `O_TRUNC`, `O_APPEND`, and
`O_DIRECTORY` from `opentty.h`. Paths are OpenTTY VFS paths and normal VFS
permissions and chroot redirection apply. Check negative results; this ABI does
not provide portable `errno` details.

```c
#include "../../lib/opentty.h"

int main(void)
{
    const char text[] = "saved by OpenTTY\n";
    int fd = open("/home/note.txt", O_CREAT | O_WRONLY | O_TRUNC, 0644);
    int written;
    if (fd < 0) return 1;
    written = write(fd, text, sizeof(text) - 1);
    close(fd);
    return written == sizeof(text) - 1 ? 0 : 1;
}
```

Descriptors `1` and `2` are console output. `write(1, data, size)` is the
low-level alternative to `printf`.

## Processes and environment

```c
int opentty_spawn(const char *path, int *pid_out);
int opentty_waitpid(int pid, int *status_out);
int opentty_shell(const char *command);
int opentty_getenv(const char *key, char *buffer, size_t size);
int opentty_expand_env(const char *text, char *buffer, size_t size);
```

`opentty_spawn` starts a Lua or ELF program asynchronously. On success it
returns the child PID and writes it to `pid_out`. `opentty_waitpid` returns the
reaped PID on success, `-11` while running, `-3` for an unknown PID, and `-13`
when the target is not the caller's child.

```c
int child, status;
if (opentty_spawn("/bin/other-app", &child) > 0) {
    while (opentty_waitpid(child, &status) == -11) { }
    printf("child exited with %d\n", status);
}
```

`opentty_shell("ls /bin")` delegates to the configured OpenTTY shell.
`opentty_getenv` reads values such as `USER`, `PWD`, and `ROOT` into a caller
buffer; `opentty_expand_env` expands `$NAME` references into another buffer.

## LCDUI applications

ELF programs can create MIDP LCDUI forms, lists, text boxes, alerts, and
commands. Include:

```c
#include "../../lib/opentty.h"
#include "../../lib/lcdui.h"
```

The UI uses positive integer handles, not Java object pointers. Build UI apps
with `-stdlib`. Its public functions are:

```c
int lcdui_new(int kind, const char *title, const char *content, int mode);
int lcdui_append_text(int form, const char *label, const char *text);
int lcdui_append_field(int form, const char *label, const char *value,
                       int max_length, int mode);
int lcdui_list_append(int list, const char *text);
int lcdui_command(const char *label, int type, int priority);
int lcdui_add_command(int screen, int command);
int lcdui_display(int screen);
int lcdui_set_text(int item, const char *text);
int lcdui_get_text(int item, char *buffer, int size);
int lcdui_set_title(int screen, const char *title);
int lcdui_set_label(int item, const char *label);
int lcdui_clear(int screen);
int lcdui_wait_event(struct lcdui_event *event);
int lcdui_destroy(int handle);
int graphics_taskmngr(void);
int opentty_setproc(const char *key, ...);
```

Minimal event loop:

```c
#include "../../lib/lcdui.h"

int main(void)
{
    struct lcdui_event event;
    int form = lcdui_new(LCDUI_FORM, "Native app", 0, 0);
    int close = lcdui_command("Close", LCDUI_COMMAND_EXIT, 1);

    opentty_setproc("name", "native-app");
    lcdui_append_text(form, "Status", "Ready");
    lcdui_add_command(form, close);
    lcdui_display(form);
    for (;;) {
        if (!lcdui_wait_event(&event)) continue;
        if (event.type == LCDUI_EVENT_COMMAND && event.command == close) {
            return 0;
        }
    }
}
```

When no event exists, `lcdui_wait_event` returns `0` and suspends the ELF
process. Do not exit then; call it again when resumed. `lcdui_display` registers
the screen with the task manager. A `LCDUI_COMMAND_SCREEN` command can call
`graphics_taskmngr()` to implement **Switch to...**. See [LCDUI.md](LCDUI.md)
and `res/apps/src/hello-lcdui.c` for all constants and event details.

## Dynamic libraries

OpenTTY supports a limited RISC-V dynamic loader for `ET_DYN` shared objects.
It handles `DT_NEEDED` dependencies and supported symbols/relocations eagerly.

```sh
./build-elf.sh res/apps/src/libutil.c -shared -o res/apps/dist/libutil.so
./build-elf.sh res/apps/src/dcalc.c -stdlib res/apps/dist/libutil.so \
  -o res/apps/dist/dcalc
```

The loader searches the program directory, `/lib/`, and `/bin/`. Package every
dependency. See `libutil.c`, `dcalc.c`, `libinit.c`, and `elflife.c` for working
imports, transitive dependencies, constructors, and destructors.

Use shared objects for reusable app code, not as a replacement for `-stdlib`.
An executable that calls `printf`, allocation, files, or LCDUI still needs
`-stdlib`.

## Assembly and raw syscalls

Prefer `opentty.h` from C. RV32IM assembly passes arguments in `a0` through
`a6`, the syscall number in `a7`, and receives the result in `a0`:

```asm
    li  a0, 1          # stdout
    la  a1, message
    li  a2, 6
    li  a7, 4          # SYS_WRITE
    ecall
```

Common raw syscall numbers are `1` (`exit`), `3` (`read`), `4` (`write`), `5`
(`open`), `6` (`close`), `20` (`getpid`), and `45` (`brk`). Other filesystem,
process, timing, and socket syscalls are implementation details; inspect
`src/ELF.java` before using them. Examples: `hello.s`, `cat.s`, `netsock.s`,

`-stdlib` uses private `ecall` IDs starting at `1000`. Never hard-code those
from an app; use the headers and runtime wrappers.

## On-device compiler

`c4cc` is a small native compiler packaged at `/bin/c4cc`. It writes an ELF32
little-endian `ET_EXEC` RV32IM file itself, so no host toolchain is needed on
the device:

```c
int main(void) {
    // integer arithmetic and output calls are supported
    puts("Hello from c4cc");
    printf("answer=%d\n", (2 + 3) * 4);
    putchar(10);
    return 0;
}
```

Use `c4cc hello.c [hello]`. Its intended subset is one `int main(...)`,
integer literals and `+`, `-`, `*`, `/` expressions, `return`, `puts`,
`putchar`, and `printf` with a literal format plus one integer expression.
It accepts `//` and `/* ... */` comments. It does not support declarations,
variables, control flow, includes, character literals, or arbitrary calls.

## Packaging

For the current `1.18.2` store:

1. Keep source in `res/apps/src/<name>.c`.
2. Build to `res/apps/dist/<name>`.
3. Copy the exact binary to `apps/1.18.2/dev/<name>`.
4. Add an entry to both `apps/1.18.2/sources.lua` and `src/etc/sources`.
5. Set `riscv = true` and list shared-library dependencies.

```lua
["hello"] = {
    remote = "dev/hello",
    here = "/bin/hello",
    riscv = true,
    description = "Example RISC-V C application"
},
```

The two catalog files must stay byte-identical. For a new major release, copy
the previous `apps/<major>/` tree first.

## Testing and troubleshooting

Start from these working examples:

| Source | Covers |
| --- | --- |
| `hello.c` | Arguments, output, basic library calls. |
| `libc-demo.c` | Strings, memory, heap, formatting, and 32/64-bit arithmetic. |
| `brainfuck.c` | File reads, dynamic buffers, and cleanup. |
| `hello-lcdui.c` | Fields, commands, events, alerts, and task manager integration. |
| `dcalc.c` | Shared-library imports. |

```sh
./build-elf.sh res/apps/src/hello.c -stdlib -o /tmp/hello
file /tmp/hello
riscv64-unknown-elf-readelf -h -l /tmp/hello
```

If loading fails, confirm ELF32 little-endian, `ET_EXEC`, `EM_RISCV`, RV32IM,
and an entry below `0x100000`. If an app crashes, check every allocation and I/O
result, remove unsupported host-library dependencies, reduce memory use, avoid
floating point, and add `printf` checkpoints. `src/ELF.java` is the final
reference for loader, heap, syscall, process, and LCDUI behavior.
