#!/bin/lua

os.setproc("name", "myapp")

local prev = graphics.getCurrent()
local screen = graphics.new("screen", "Minha App")
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local ok   = graphics.new("command", { label = "OK", type = "ok", priority = 1 })

graphics.append(screen, { type = "field", label = "Nome:", value = "" })
graphics.addCommand(screen, back)
graphics.addCommand(screen, ok)

graphics.handler(screen, {
    [back] = function()
        graphics.display(prev)
        os.exit(0)
    end,
    [ok] = function(value)
        print("Você digitou: " .. tostring(value))
        graphics.display(graphics.new("alert", "OK", "Salvo!"), prev)
    end
})

graphics.display(screen)