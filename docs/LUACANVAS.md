# Lua Canvas

`graphics.canvas` draws into a persistent off-screen buffer and presents it on
the next repaint. Create a canvas, install callbacks, then display it.

```lua
local canvas = graphics.canvas.new({
    title = "Paint",
    background = 16777215,
    foreground = 0,
    fullscreen = false
})

graphics.canvas.on(canvas, "paint", function(c, x, y, w, h)
    graphics.canvas.clear(c)
    graphics.canvas.color(c, 0, 80, 180)
    graphics.canvas.rect(c, 8, 8, w - 16, h - 16, true)
    graphics.canvas.color(c, 255, 255, 255)
    graphics.canvas.font(c, "monospace bold small")
    graphics.canvas.text(c, "Tap or use arrows", w / 2, 16,
        graphics.canvas.anchor.TOP + graphics.canvas.anchor.HCENTER)
end)

graphics.canvas.on(canvas, "pointerPressed", function(c, x, y)
    graphics.canvas.color(c, 255, 220, 0)
    graphics.canvas.arc(c, x - 8, y - 8, 16, 16, 0, 360, true)
end)

graphics.canvas.on(canvas, "keyPressed", function(c, key, action)
    if action == graphics.canvas.key.FIRE then
        graphics.canvas.clear(c)
    end
end)

graphics.display(canvas)
graphics.canvas.flush(canvas)
```

## API

- `new(options)`: options are `title`, `background`, `foreground`, `font`,
  `fullscreen`, `callbacks` and event functions directly on the table.
- `on(canvas, event, callback)`: events are `paint`, `keyPressed`,
  `keyReleased`, `pointerPressed`, `pointerReleased`, `pointerDragged`,
  `show`, `hide`, and `sizeChanged`.
- Drawing: `color`, `background`, `foreground`, `font`, `clear`, `line`,
  `rect`, `roundRect`, `arc`, `text`, `image`, and `pixel`.
- State: `clip`, `resetClip`, `translate`, `reset`, `size`, `metrics`, and
  `fullscreen`.
- Presentation: `repaint(canvas[, x, y, width, height])` and `flush(canvas)`.
- Commands: `command(canvas, command, callback)` registers a command created
  with `graphics.new("command", ...)`.

`rect`, `roundRect`, and `arc` use a final optional boolean to fill the shape.
`color` accepts one `0xRRGGBB` number or separate red, green and blue values.
`text` and `image` accept an optional MIDP anchor from `graphics.canvas.anchor`.
`metrics` returns font height, baseline, current translation X and translation Y.

Keep paint and input callbacks short. They run on the LCDUI event path, so
blocking I/O or long loops make the MIDlet unresponsive.
