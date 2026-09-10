# LCDUI Guest ABI

ELF RV32IM applications can create MIDP LCDUI screens through the wrappers in
`res/lib/lcdui.h`. The Java objects remain in OpenTTY; guest programs use
positive integer handles only. This ABI requires an ELF built with `-stdlib`.

```sh
CROSS=riscv64-unknown-elf- ./build-elf.sh app.c -stdlib -o app
```

Include the header from an app under `res/apps/src/` with:

```c
#include "../../lib/lcdui.h"
```

All creation and mutation functions return `-1` for an invalid handle or
argument. Handles are local to the current ELF process and must not be shared
with another process.

## Form Example

The following app asks for a name and shows an alert after `Say hello`.
The shipping source is `res/apps/src/hello-lcdui.c`.

```c
#include "../../lib/lcdui.h"

int main(void)
{
    struct lcdui_event event;
    int form = lcdui_new(LCDUI_FORM, "Hello", 0, 0);
    int name = lcdui_append_field(form, "Your name", "", 32, LCDUI_TEXT_ANY);
    int greet = lcdui_command("Say hello", LCDUI_COMMAND_OK, 1);

    opentty_setproc("name", "hello");
    lcdui_append_text(form, 0, "What is your name?");
    lcdui_add_command(form, greet);
    lcdui_display(form);

    while (1) {
        if (!lcdui_wait_event(&event)) { continue; }
        if (event.type == LCDUI_EVENT_COMMAND && event.command == greet) {
            char value[33];
            int alert, close;
            lcdui_get_text(name, value, sizeof(value));
            alert = lcdui_new(LCDUI_ALERT, "Hello", value, 0);
            close = lcdui_command("Close", LCDUI_COMMAND_EXIT, 1);
            lcdui_add_command(alert, close);
            lcdui_display(alert);
            while (!lcdui_wait_event(&event)) { }
            if (event.command == close) { return 0; }
        }
    }
}
```

`return 0`, `exit(0)`, a guest crash, or an interruption from the task manager
removes the ELF process. If it had a registered screen, OpenTTY restores a
screen registered by another process, matching Lua process cleanup.

## Screens And Items

Create a screen with `lcdui_new(kind, title, content, mode)`:

| Kind | Result | `content` and `mode` |
| --- | --- | --- |
| `LCDUI_FORM` | `Form` | Ignored. |
| `LCDUI_LIST` | `List` | `mode`: `LCDUI_LIST_IMPLICIT`, `LCDUI_LIST_EXCLUSIVE`, or `LCDUI_LIST_MULTIPLE`. |
| `LCDUI_TEXTBOX` | `TextBox` | Initial text and `LCDUI_TEXT_ANY` or `LCDUI_TEXT_PASSWORD`. |
| `LCDUI_ALERT` | Permanent information `Alert` | Alert message; `mode` is ignored. |

Populate a form or list:

```c
int note = lcdui_append_text(form, "Status", "Ready");
int password = lcdui_append_field(form, "Password", "", 64, LCDUI_TEXT_PASSWORD);
int list = lcdui_new(LCDUI_LIST, "Choices", 0, LCDUI_LIST_IMPLICIT);
lcdui_list_append(list, "First choice");
lcdui_list_append(list, "Second choice");
```

`lcdui_append_text` and `lcdui_append_field` return an item handle. Use it with
`lcdui_set_text(handle, text)` and `lcdui_get_text(handle, buffer, size)`.
`lcdui_list_append` returns the zero-based list index. `lcdui_clear` removes all
items from a `Form` or `List`; `lcdui_set_title` changes a screen title.

## Commands And Events

Commands have a label, one of the `LCDUI_COMMAND_*` types, and a MIDP priority:

```c
int save = lcdui_command("Save", LCDUI_COMMAND_OK, 1);
int back = lcdui_command("Back", LCDUI_COMMAND_BACK, 2);
lcdui_add_command(form, save);
lcdui_add_command(form, back);
```

Show a screen with `lcdui_display(screen)`. It also registers that screen as
the process screen, so it appears in the OpenTTY task manager.

`lcdui_wait_event(&event)` returns `1` after filling this structure:

```c
struct lcdui_event {
    int type;     /* LCDUI_EVENT_COMMAND or LCDUI_EVENT_LIST_SELECT */
    int screen;   /* Displayable handle */
    int command;  /* Command handle, or 0 for an implicit List selection */
    int index;    /* selected List index, otherwise -1 */
};
```

When no event is pending, `lcdui_wait_event` returns `0`, suspends the ELF
process, and returns to the emulator. Do not exit in that case: call it again
after OpenTTY resumes the process for a command event. An implicit `List`
selection has `type == LCDUI_EVENT_LIST_SELECT`; inspect `index`.

Destroy an unused object with `lcdui_destroy(handle)`. Process cleanup destroys
all remaining handles automatically.

## Process And Task Manager

`opentty_setproc(key, value)` is the ELF counterpart of Lua
`os.setproc(key, value)`. It accepts these keys:

| Key | Value |
| --- | --- |
| `"name"` | Process name string. |
| `"screen"` | LCDUI screen handle. Normally `lcdui_display` does this automatically. |
| `"cmd"` | Command string displayed in process metadata. |
| Any other key | String stored in the process database. Pass `0` to remove that database key. |

Examples:

```c
opentty_setproc("name", "contacts");
opentty_setproc("cmd", "contacts --new");
opentty_setproc("screen", form);
opentty_setproc("selected", "alice");
opentty_setproc("selected", 0);
```

Open the system task manager with `graphics_taskmngr()`:

```c
int tasks = lcdui_command("Tasks", LCDUI_COMMAND_SCREEN, 2);

if (event.type == LCDUI_EVENT_COMMAND && event.command == tasks) {
    graphics_taskmngr();
}
```

The process remains registered with its current screen. Selecting it in the
task manager displays that screen again. Interrupting it performs ELF cleanup.

## Process Lifecycle

Every OpenTTY process records its parent PID when it is launched. When a Lua or
ELF process terminates, its PID, parent PID, exit status and process metadata
are retained in the system's completed-process table for parent-side reaping.
This is the basis for the ELF `spawn` and `waitpid` ABI; a completed process is
not considered runnable and its LCDUI handles have already been destroyed.

```c
int child, status;
if (opentty_spawn("/bin/my-app", &child) > 0) {
    while (opentty_waitpid(child, &status) == -11) { }
}
```

`opentty_waitpid` accepts only a child PID of the caller. It returns the reaped
PID on success, `-11` while the child is running, `-3` for an unknown PID, and
`-13` when the target is not a child of the caller.

`opentty_shell(command)` uses the same Lua `os.execute` handler, including a
configured custom shell, and returns its exit status:

```c
if (opentty_shell("ls /bin") != 0) { /* command failed */ }
```
