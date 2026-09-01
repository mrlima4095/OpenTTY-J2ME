-- Lua J2ME - Code examples
-- This is an example of building a screen with the graphics API

local previous = graphics.getCurrent() -- Get current screen
local screen = graphics.new("screen", "Screen Title") -- Create a screen object

graphics.append(screen, { type = "text", value = "Welcome to my screen!", layout = "default" })

local ok = graphics.new("command", { label = "OK", type = "ok", priority = 1 })
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
graphics.addCommand(screen, ok)
graphics.addCommand(screen, back)

graphics.handler(screen, {
    [ok] = function()
        print("OK pressed")
    end,
    [back] = function()
        graphics.display(previous)
    end
})

graphics.display(screen)
