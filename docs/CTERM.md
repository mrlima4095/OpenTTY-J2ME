# cterm — Canvas terminal

`cterm` is the `graphics.canvas` terminal emulator shipped as `/bin/cterm`
(`apps/1.19/sys/cterm`). It is different from `xterm`:

| | `xterm` | `cterm` |
| --- | --- | --- |
| Widget | LCDUI `Form` with a `StringItem` buffer | `graphics.canvas` character grid |
| Colours | plain text | ANSI SGR (16, 256 and truecolour) |
| Command line | text field + Run command | in-canvas editor with history |
| Scrollback | Form scrolling | terminal scrollback with Up/Down, game keys and stylus drag |
| Typing | the Form's text field | direct typing (`abc`) or the phone text box (`123`) |

## Running

```sh
cterm          # interactive
cterm 1        # deterministic demo frame, then return (test suite)
```

The app paints a monospace grid, a coloured prompt on the last row and the
command line. Commands run in the cterm process' scope, so `cd` changes the
prompt and the directory used by the next command. Output is captured through
`io.setstdout` and parsed by the terminal engine, which means any program that
prints ANSI escape sequences is rendered with colour.

## Keys

| Key | Action |
| --- | --- |
| Up / Down | scroll the terminal up / down (scrollback) |
| PageUp / PageDown (GAME_A / GAME_B) | page the scrollback |
| Home / End (GAME_C / GAME_D) | start / end of the input line |
| Left / Right | move the input cursor |
| FIRE | open the phone text box (`123`) or run the line (`abc`) |
| Backspace | delete before the cursor |
| Delete | delete at the cursor |
| Tab | complete a command (`/bin`) or a path |
| printable | insert at the cursor and switch to `abc` |

Up/Down scroll the terminal instead of walking the history: on a phone the
scrollback is what the arrows are for. The history is still reachable through
the **Hist** command.

## Typing modes

The input row ends with a small `abc` / `123` marker:

- `123` — the device has no usable keyboard, so FIRE opens the native text box,
  fills the command line and runs it when you confirm.
- `abc` — direct typing. Any printable key switches to this mode for the rest
  of the session, and FIRE runs the line immediately.

The **QWERTY** command toggles the mode (and the `qwerty` built-in command does
the same). The choice is remembered in `/home/.ctermrc` (`qwerty=1`), so the
next run starts in the same mode; if the file cannot be written the mode is
still changed for the session.

## Commands (soft keys)

| Command | Effect |
| --- | --- |
| **Type** | open the phone text box for the command line |
| **Send** | run the current line |
| **Switch to...** | open the running-apps menu (`graphics.taskmngr`) |
| **Hist** | pick a previous command from a list |
| **Clear** | clear the screen and the scrollback |
| **Font** | cycle the monospace font size |
| **QWERTY** | toggle direct typing (`abc` / `123`) |
| **Exit** | close cterm |

Dragging with the stylus scrolls the scrollback.

## Built-in commands

These are handled by cterm before the shell:

| Command | Effect |
| --- | --- |
| `clear` | clear the screen and the scrollback |
| `colors` | print the ANSI palette and style samples |
| `history` | list the command history |
| `qwerty` | toggle the `abc` / `123` typing mode |
| `help` | key and command reference |
| `exit` / `logout` | close cterm |

Everything else goes through `os.execute` (`/bin/sh` built-ins, `/bin/*`
programs, pipes and redirections included).

## Terminal engine

The file has two halves. The first is a plain Lua engine (`cterm.new`) with no
dependency on the canvas:

- a `cols × rows` cell grid with per-cell foreground, background and attribute
  bits (bold, underline, reverse, dim, hidden) and a `revision` counter bumped
  by every cell mutation;
- ANSI parsing: SGR (including `38;5;n`, `48;5;n`, `38;2;r;g;b`), cursor
  movement (`A B C D E F G H f d`), erase (`J K X`), insert/delete lines and
  characters (`L M @ P`), scroll (`S T`), save/restore (`s u`) and cursor
  visibility (`?25h/l`);
- wrapping, CR/LF/BS/TAB handling, delayed scrollback and a capped history;
- an editable input line (`setInput`, `insertChar`, `backspaceInput`,
  `deleteInput`, `moveInput`, `moveHome`, `moveEnd`, history navigation);
- `screenText`, `scrollbackText`, `allText`, `rowText` and `cellAt` for tests
  and diagnostics.

The second half (`cterm.run`) owns the canvas: it measures the font, creates
the grid, draws background runs and text runs, paints the prompt and cursor,
registers keys/pointer/commands and executes shell commands.

The engine can be loaded without starting the app:

```lua
CTERM_AS_MODULE = true
local cterm = load(io.read("/bin/cterm"))
CTERM_AS_MODULE = nil

local t = cterm.new({ cols = 80, rows = 24 })
t:write("\27[31mred\27[0m and \27[1mbold\27[0m\n")
print(t:screenText())
```

## Staying responsive on a phone

Three rules keep the MIDlet's LCDUI event thread free:

- The grid is redrawn only when the terminal changed: `Terminal.revision` is
  bumped by every cell mutation, so `render()` compares it (plus the scroll
  offset and the geometry) before repainting the whole screen.
- A key press repaints only the input row (prompt, line, cursor, mode marker),
  which is a handful of canvas calls instead of a full grid pass.
- Shell commands run on a background thread (`java.run`), so a slow command
  never freezes the event loop. The canvas is already current, so the job only
  posts a repaint; `c:display()` is left to the UI callbacks.

The prompt is measured with `c:textWidth` per segment instead of
`#text * charWidth`: phones that fall back to a proportional font for
`monospace` would otherwise leave a huge gap between the user, `@`, the host
and the path. When the full `user@host:path$` prompt does not fit in 60% of the
canvas width it compacts to `path$` and finally to a truncated `...tail$`.

## Limits

- Output is captured when the command finishes, not streamed line by line, so
  a long-running command does not repaint until it returns. Output is capped at
  64 KB (`[output truncated]`).
- There is no wide-character cell model: every Unicode code unit occupies one
  monospace cell.
- Scrollback defaults to 200 rows (`scrollback = n` in `cterm.run`/`cterm.new`).
- The font can be changed at runtime with the **Font** command; changing it
  reflows the grid and keeps the bottom rows.

## Tests

`tests/lua/check_modules.lua` loads the app in module mode and checks the ANSI
parser, wrapping, scrollback, line editing and one real headless canvas frame
(including a captured `echo`), plus the phone behaviour: Up/Down scrolling, the
grid revision not changing while typing, FIRE opening the text box in `123`
mode, FIRE running the line on the background thread in `abc` mode, and the
prompt fitting on a narrow canvas. `tests/lua/run_modules.sh` stages the app as
`cterm-app.lua` in the same headless shim run as the rock payloads.
