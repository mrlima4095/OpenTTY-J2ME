-- Run with: lua scripts/test-canvas.lua
local previous = graphics.getCurrent()
local state = { x = 40, y = 40, taps = 0 }
local anchor = graphics.canvas.anchor

local canvas = graphics.canvas.new({
    title = "Lua Canvas Test",
    background = 16777215,
    foreground = 0,
    font = "monospace bold small"
})

graphics.canvas.on(canvas, "paint", function(c, clipX, clipY, width, height)
    graphics.canvas.clear(c)
    graphics.canvas.color(c, 20, 80, 180)
    graphics.canvas.rect(c, 4, 4, width - 8, height - 8, true)
    graphics.canvas.color(c, 255, 255, 255)
    graphics.canvas.text(c, "Lua Canvas", width / 2, 12, anchor.TOP + anchor.HCENTER)
    graphics.canvas.text(c, "Tap to move the marker", width / 2, 30, anchor.TOP + anchor.HCENTER)

    graphics.canvas.color(c, 255, 220, 0)
    graphics.canvas.arc(c, state.x - 8, state.y - 8, 16, 16, 0, 360, true)
    graphics.canvas.color(c, 255, 255, 255)
    graphics.canvas.text(c, "Taps: " .. state.taps, 8, height - 8, anchor.BOTTOM + anchor.LEFT)
end)

graphics.canvas.on(canvas, "pointerPressed", function(c, x, y)
    state.x = x
    state.y = y
    state.taps = state.taps + 1
end)

graphics.canvas.on(canvas, "keyPressed", function(c, key, action)
    if action == graphics.canvas.key.FIRE then
        state.x = 40
        state.y = 40
        state.taps = 0
    end
end)

local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
graphics.canvas.command(canvas, back, function(c)
    graphics.display(previous)
end)

graphics.display(canvas)
graphics.canvas.flush(canvas)
