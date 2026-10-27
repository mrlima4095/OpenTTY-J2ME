# Lua Canvas — `graphics.canvas`

`graphics.canvas` gives Lua scripts a free-form drawing surface on the J2ME
`Canvas`: pixels, lines, shapes, images, text, fonts, a text cursor, held keys,
pointer input, LCDUI commands, and a game loop. It is implemented in
`src/LuaCanvas.java` and exposed by `src/Lua.java`.

Everything here is device-side MIDP-2.0 / CLDC-1.0, and works the same in the
desktop shim (`pc/`) with or without a window.

## Contents

- [Quick start](#quick-start)
- [The handle](#the-handle)
- [Creating a canvas](#creating-a-canvas)
- [Colors](#colors)
- [Drawing](#drawing)
- [Text, fonts and the cursor](#text-fonts-and-the-cursor)
- [Transforms and clipping](#transforms-and-clipping)
- [Keys and game actions](#keys-and-game-actions)
- [Pointer input](#pointer-input)
- [Events and callbacks](#events-and-callbacks)
- [Commands](#commands)
- [Loop mode](#loop-mode)
- [Threading rules](#threading-rules)
- [Testing without a window](#testing-without-a-window)
- [Method reference](#method-reference)
- [Constants](#constants)
- [Limitations](#limitations)

---

## Quick start

```lua
#!/bin/lua
local c = graphics.canvas.new({ title = "Hello", background = 0x000018, foreground = 0xFFFFFF })

c:clear()
c:color(0x00FF88)
c:rect(10, 10, 100, 40, true)
c:text("Hello, OpenTTY", 4, 60)
c:display()

-- Draw ten frames, then leave.
local n = 0
c:loop(function(canvas, dt)
    n = n + 1
    canvas:clear()
    canvas:circle(20 + n * 6, 100, 8, true)
    if n >= 10 then return false end
end, 33)
```

A complete, runnable example ships as the `canvasdemo` app
(`apps/1.19/games/canvasdemo`): a paddle game with a background loop, held-key
movement, a score, lives and a game-over screen.

```bash
canvasdemo        # play
canvasdemo 60     # run 60 frames and return (used by the tests)
```

---

## The handle

`graphics.canvas.new` returns a **handle**: a Lua table whose metatable holds the
Java object (`__canvas`) and whose `__index` is the `graphics.canvas` module
itself. That gives two equivalent call styles:

```lua
c:text("hi", 4, 4)                  -- method style
graphics.canvas.text(c, "hi", 4, 4) -- module style
```

Methods that change state return the handle, so calls chain:

```lua
c:color(0xFF0000):font("monospace bold large"):text("red", 4, 4)
```

Writing a function to a callback name installs it; writing a value to one of the
writable properties applies it:

```lua
c.keyPressed = function(canvas, key, action) print("key " .. key) end
c.title  = "New title"
c.font   = "proportional bold small"
c.background = "#202030"
c.foreground = "white"
c.color      = 0x00FF00
```

Callbacks are stored next to the Java object, not in the table, so a callback can
never shadow a method such as `size` or `command`.

---

## Creating a canvas

```lua
local c = graphics.canvas.new({
    title      = "Canvas",            -- screen title (default "Canvas")
    background = 0xFFFFFF,            -- cleared color (default white)
    foreground = 0x000000,            -- initial drawing color (default black)
    font       = "monospace medium",  -- font spec (default "medium")
    fullscreen = false,               -- hide the title/command area
    paint      = function(c, w, h) end,      -- any callback can be given here
    keyPressed = function(c, k, a) end,
})
```

`new()` takes no arguments too (`graphics.canvas.new()`), and ignores unknown
keys.

Font specs are case-insensitive words separated by spaces:

| Word | Meaning |
|------|---------|
| `system`, `monospace`, `proportional` | face |
| `plain`, `bold`, `italic`, `underlined` | style (several allowed) |
| `small`, `medium`, `large` | size |

Anything unknown falls back to the default font. A numeric face/style/size triple
also works: `c:font(graphics.canvas.fonts.MONOSPACE, graphics.canvas.fonts.BOLD, graphics.canvas.fonts.LARGE)`.

---

## Colors

Colors are `0xRRGGBB` numbers, `"#RRGGBB"` strings, decimal strings, or names:

```lua
c:color(0xFF8800)
c:color("#ff8800")
c:color("orange")
c:color(255, 136, 0)          -- r, g, b
graphics.canvas.rgb(255, 136, 0)  -- same number
```

Named colors live in `graphics.canvas.colors`: `black white red green blue
yellow cyan magenta gray grey orange purple brown pink lime navy teal silver
maroon olive`.

`background` and `foreground` are properties of the canvas; `color` is the
current pen color used by every drawing call.

---

## Drawing

Every drawing call uses the current color and draws into a persistent off-screen
buffer of the canvas size; the buffer is blitted on paint, so a frame is never
half-drawn.

```lua
c:clear()                      -- fill the whole buffer with the background
c:clear(x, y, w, h)            -- ...or just a rectangle

c:line(x1, y1, x2, y2)
c:rect(x, y, w, h [, filled])
c:roundRect(x, y, w, h, arcW, arcH [, filled])
c:arc(x, y, w, h, startDeg, arcDeg [, filled])
c:circle(cx, cy, radius [, filled])
c:ellipse(x, y, w, h [, filled])
c:triangle(x1, y1, x2, y2, x3, y3 [, filled])
c:pixel(x, y)
c:image(imgOrPath, x, y [, anchor])
```

Polygons accept a flat pair list or a list of points:

```lua
c:polygon({ 10, 60, 40, 60, 25, 90 }, true)
c:polygon({ { x = 60, y = 70 }, { x = 90, y = 70 }, { x = 75, y = 95 } })
c:polygon({ { 60, 70 }, { 90, 70 }, { 75, 95 } }, true)
```

`image` takes an `Image` object (from `graphics.new("image", ...)`) or a path
that is loaded through the usual image loader.

---

## Text, fonts and the cursor

```lua
c:font("monospace bold large")   -- set; returns the spec with no argument
c:font()                         -- -> "monospace bold large"

local h, baseline = c:metrics()  -- font height and baseline offset
local w = c:textWidth("Hello")
local cw = c:charWidth("A")

c:text("left", 4, 100)                     -- anchor defaults to top|left
c:text("center", w / 2, 120, "center")
c:text("right", w - 4, 140, "right")
c:text("bottom", 4, 200, "bottom left")
```

Anchors are `graphics.canvas.anchor.*` numbers or strings containing the words
`left`/`right`/`center`, `top`/`bottom`/`baseline`/`center` — e.g. `"center"`,
`"top left"`, `"bottom right"`.

The cursor is a small text-printing helper:

```lua
c:cursor(x, y)          -- set (returns the handle)
local x, y = c:cursor() -- read
c:print("text")         -- draw at the cursor and advance it
c:nl()                  -- newline: next line, back to the cursor column
c:backspace(3)          -- erase three characters
c:caret()               -- draw a caret at the cursor
```

This is handy for log-style output on a canvas without computing positions.

---

## Transforms and clipping

```lua
c:translate(dx, dy)     -- moves the origin for all later calls
c:clip(x, y, w, h)      -- restrict drawing
local x, y, w, h = c:clipRect()
c:resetClip()           -- clip back to the whole canvas

c:save()                -- push color, font, clip and translation
c:restore()             -- pop them
c:reset()               -- back to the initial state (color = foreground,
                        -- font, clip and translation reset)
```

`save`/`restore` nest: each `save` pushes one state.

---

## Keys and game actions

Key codes are the MIDP codes in `graphics.canvas.key`:

`UP DOWN LEFT RIGHT FIRE GAME_A GAME_B GAME_C GAME_D NUM0..NUM9 STAR POUND`

`graphics.canvas.action` holds the same names as **game actions** (the
device-independent meaning of a key).

```lua
c:keyDown(keyCode)             -- true while held
c:gameAction(keyCode)          -- game action for a key (0 = none)
c:keyName(keyCode)             -- "Up", "Fire", "1"...

local keys = c:keys()          -- array of the codes held right now
for i = 1, #keys do print(keys[i]) end
c:resetKeys()                  -- forget held keys (after a pause)
```

`keys()` is the polling API used by games in loop mode. In callback mode, use
`keyPressed` / `keyReleased` / `keyRepeated` instead.

---

## Pointer input

```lua
c.pointerPressed  = function(canvas, x, y) end
c.pointerReleased = function(canvas, x, y) end
c.pointerDragged  = function(canvas, x, y) end

local x, y = c:pointer()   -- last known pointer position
```

Touch/stylus events are only delivered on devices (or emulators) that support
them; on a plain keypad they simply never fire.

---

## Events and callbacks

Callbacks are plain Lua functions receiving the handle as the first argument:

| Name | Arguments | When |
|------|-----------|------|
| `paint` | `canvas, width, height` | the screen needs repainting |
| `keyPressed` | `canvas, keyCode, gameAction` | key down |
| `keyReleased` | `canvas, keyCode` | key up |
| `keyRepeated` | `canvas, keyCode, gameAction` | auto-repeat |
| `pointerPressed` / `pointerReleased` / `pointerDragged` | `canvas, x, y` | pointer |
| `show` / `hide` | `canvas, width, height` | screen shown/hidden |
| `size` | `canvas, width, height` | canvas size changed |
| `tick` | `canvas, dtMs` | fallback frame callback for `loop` |
| `command` | `canvas, label` | a command with no handler fired |

Install and remove them either way:

```lua
c.keyPressed = function(canvas, key, action) ... end
c:on("keyPressed", function(canvas, key, action) ... end)
c:off("keyPressed")
```

You can also pass them in the options table of `new()`.

`paint` is **not** called while a loop is running: the loop thread owns the
buffer and the LCDUI thread only blits it, so a slow `paint` can never race the
game.

---

## Commands

Commands are the LCDUI soft keys/menu entries of the canvas:

```lua
c:command("Exit", "exit", function(canvas, label) c:stop(); c:close() end)
c:command("Pause", "screen", 2, function(canvas, label) ... end)  -- priority 2
c:clearCommands()
```

The second argument is the command type: `screen` (default), `back`, `cancel`,
`ok`, `help`, `stop`, `exit`, `item`. A numeric third argument sets the priority
(default 1). If no handler is given, the `command` callback receives
`(canvas, label)`.

---

## Loop mode

`c:loop` drives the canvas frame by frame:

```lua
c:loop(function(canvas, dt)   -- dt = milliseconds since the previous frame
    canvas:clear()
    canvas:text("frame", 4, 4)
    if quit then return false end   -- returning false stops the loop
end, 33)                            -- frame delay in ms (default 20)
```

Without a function, the `tick` callback is used:

```lua
c.tick = function(canvas, dt) ... end
c:loop(nil, 16)
```

Related calls:

```lua
c:animate(stepFn, delay)   -- same loop, on a background thread
c:stop()                   -- ask the loop to end
c:running()                -- true while a loop is active
c:age()                    -- milliseconds since the canvas was created
c:waitKey([timeoutMs])     -- block for a key press -> code, action (or nil)
```

`animate` is the convenient form for shell-launched games: the loop runs on its
own thread, so the shell, commands and other processes stay responsive.

`waitKey` cannot be used from inside a loop (it returns `nil` there); poll
`keys()` instead.

The loop also ends by itself when the owning Lua process disappears (for example
`os.exit()`), so a game never keeps drawing after its shell command is gone.

---

## Threading rules

MIDP gives Lua two threads that matter here:

- the **LCDUI thread**, which delivers `keyPressed`, `paint`, `command`, and
  calls `show`/`hide`;
- the **Lua thread**, which runs your script (the shell command) and any
  `java.run(...)` background function.

Rules that keep both modes safe:

1. **Callback mode** — no `loop`. Draw from inside the callbacks (`paint`,
   `keyPressed`, ...) and call `c:repaint()` / `c:flush()` when you change the
   buffer.
2. **Loop mode** — draw only from the loop function. Events are queued and
   delivered by the loop thread at the top of each frame, so `keyPressed` still
   works while the loop runs, but on the loop's thread.
3. Never call `c:loop` **from the LCDUI thread** (a key handler, `paint`): it
   would block the UI. Start it from the Lua thread — a script run by the shell
   already is one — or use `c:animate`, which always spawns its own thread.
4. `c:flush()` repaints immediately; the loop calls it once per frame.

```lua
-- Safe: script runs on the Lua thread, loop runs in the background.
java.run(function() c:animate(step, 33) end)
```

---

## Testing without a window

The desktop shim can run the whole MIDlet without opening a window:

```bash
OPENTTY_JVM_OPTS="-Dopentty.headless=1" \
  bash pc/run.sh --user opentty --pass opentty --smoke -- /mnt/opentty/test.lua
```

`-Dopentty.headless=1` (or `-Djava.awt.headless=true`) keeps every screen and
canvas fully functional — sizes, drawing, keys, loops — it simply is not
painted. Write results to `/mnt/opentty/<file>` and read them from
`data/mnt/opentty/<file>` on the host.

Useful for automated checks:

```lua
local c = graphics.canvas.new()
local n = 0
c:loop(function(canvas, dt) n = n + 1; if n >= 20 then return false end end, 16)
io.write("frames=" .. n .. "\n", "/mnt/opentty/result.txt")
```

`tests/` uses this recipe, and it is the recommended way to validate a game
before running it on a device.

---

## Method reference

| Method | Returns | Notes |
|--------|---------|-------|
| `new([options])` | handle | see [Creating a canvas](#creating-a-canvas) |
| `rgb(r, g, b)` | number | packs a color |
| `display()` | handle | show the canvas |
| `close()` | handle | restore the previous screen |
| `title([text])` | handle / string | |
| `fullscreen([on])` | boolean | set and return the current state |
| `size()` | `w, h` | canvas size in pixels |
| `repaint([x, y, w, h])` | handle | request a repaint |
| `flush()` | handle | repaint now |
| `clear([x, y, w, h])` | handle | fill with the background |
| `color([c] \| [r, g, b])` | handle / number | pen color |
| `background([c])` | handle / number | |
| `foreground([c])` | handle / number | also sets the pen color |
| `font([spec] \| [face, style, size])` | handle / string | |
| `metrics()` | `height, baseline` | |
| `textWidth(s)` / `charWidth(ch)` | number | |
| `text(s, x, y [, anchor])` | handle | |
| `print(s)` / `nl()` / `backspace([n])` | handle | cursor text |
| `cursor([x, y])` | handle / `x, y` | |
| `caret()` | handle | draw a caret at the cursor |
| `line`, `rect`, `roundRect`, `arc`, `circle`, `ellipse`, `triangle`, `polygon`, `pixel`, `image` | handle | drawing, see [Drawing](#drawing) |
| `clip(x, y, w, h)` / `resetClip()` / `clipRect()` | handle / `x, y, w, h` | |
| `translate(dx, dy)` | handle | |
| `save()` / `restore()` / `reset()` | handle | state stack |
| `keyDown(code)` | boolean | |
| `keyName(code)` | string | |
| `gameAction(code)` | number | |
| `keys()` | array | held key codes |
| `resetKeys()` | handle | |
| `pointer()` | `x, y` | last pointer position |
| `waitKey([ms])` | `code, action` or nil | not inside a loop |
| `loop([fn] [, delay])` | handle | blocking loop |
| `animate([fn] [, delay])` | handle | loop on a background thread |
| `stop()` | handle | |
| `running()` / `age()` | boolean / number | |
| `on(name, fn)` / `off(name)` | handle | callbacks |
| `command(label [, type] [, priority] [, fn])` | handle | |
| `clearCommands()` | handle | |

---

## Constants

```lua
graphics.canvas.anchor   -- LEFT RIGHT HCENTER TOP BOTTOM VCENTER BASELINE CENTER
graphics.canvas.key      -- UP DOWN LEFT RIGHT FIRE GAME_A..GAME_D NUM0..NUM9 STAR POUND
graphics.canvas.action   -- UP DOWN LEFT RIGHT FIRE GAME_A..GAME_D
graphics.canvas.fonts    -- SYSTEM MONOSPACE PROPORTIONAL
                         -- PLAIN BOLD ITALIC UNDERLINED
                         -- SMALL MEDIUM LARGE
graphics.canvas.colors   -- black white red green blue yellow cyan magenta
                         -- gray grey orange purple brown pink lime navy
                         -- teal silver maroon olive
```

---

## Limitations

- The buffer is one `Image` the size of the canvas: on very small heaps prefer
  `clear(rect)` over full clears when you can, and avoid many canvases at once.
- No alpha blending, no gradients, no rotation, no anti-aliasing — MIDP-2.0
  `Graphics` only.
- `arc` angles are degrees, clockwise, `0` = 3 o'clock, like `Graphics.drawArc`.
- No font metrics for wrapped text: use `textWidth` and split lines yourself.
- `image` paths go through the regular image loader, so only supported formats
  work (PNG on most devices, JPEG where the device allows it).
- `waitKey` cannot run inside a loop; use `keys()` polling there.
- One loop per canvas: `loop`/`animate` are ignored while a loop is running.
