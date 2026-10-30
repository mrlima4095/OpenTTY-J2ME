# cterm — Canvas terminal

`cterm` is the `graphics.canvas` terminal emulator shipped as `/bin/cterm`
(`apps/1.19/sys/cterm`). It is different from `xterm`:

| | `xterm` | `cterm` |
| --- | --- | --- |
| Widget | LCDUI `Form` with a `StringItem` buffer | `graphics.canvas` character grid |
| Colours | plain text | ANSI SGR (16, 256 and truecolour) |
| Command line | text field + Run command | in-canvas editor with history |
| Scrollback | Form scrolling | terminal scrollback with PgUp/PgDn and stylus drag |

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
| printable | insert at the cursor |
| Enter / FIRE | run the command |
| Left / Right | move the cursor |
| Up / Down | previous / next history entry |
| Backspace | delete before the cursor |
| Delete | delete at the cursor |
| F1 / F2 | scrollback up / down |
| F3 / F4 | start / end of the line |
| Tab | complete a command (`/bin`) or a path |

On a phone keypad only digits and the game keys are available, so the **Type**
command opens a native text box, fills the command line and runs it. The other
commands are **Send**, **PgUp**, **PgDn**, **Bottom**, **Clear**, **Font** and
**Exit**. Dragging with the stylus scrolls the scrollback.

## Built-in commands

These are handled by cterm before the shell:

| Command | Effect |
| --- | --- |
| `clear` | clear the screen and the scrollback |
| `colors` | print the ANSI palette and style samples |
| `history` | list the command history |
| `help` | key and command reference |
| `exit` / `logout` | close cterm |

Everything else goes through `os.execute` (`/bin/sh` built-ins, `/bin/*`
programs, pipes and redirections included).

## Terminal engine

The file has two halves. The first is a plain Lua engine (`cterm.new`) with no
dependency on the canvas:

- a `cols × rows` cell grid with per-cell foreground, background and attribute
  bits (bold, underline, reverse, dim, hidden);
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

## Limits

- Output is captured when the command finishes, not streamed line by line, so
  a long-running command does not repaint until it returns. GUI commands
  (`nano`, games, ...) are followed by a `c:display()` that brings cterm back.
- There is no wide-character cell model: every Unicode code unit occupies one
  monospace cell.
- Scrollback defaults to 200 rows (`scrollback = n` in `cterm.run`/`cterm.new`).
- The font can be changed at runtime with the **Font** command; changing it
  reflows the grid and keeps the bottom rows.

## Tests

`tests/lua/check_modules.lua` loads the app in module mode and checks the ANSI
parser, wrapping, scrollback, line editing and one real headless canvas frame
(including a captured `echo`). `tests/lua/run_modules.sh` stages the app as
`cterm-app.lua` in the same headless shim run as the rock payloads.
