#!/bin/lua

os.setproc("name", "myapp")

local prev = graphics.getCurrent()
local screen = graphics.new("screen", "Minha App")
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local ok   = graphics.new("command", { label = "OK", type = "ok", priority = 1 })
local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

graphics.append(screen, { type = "field", label = "Nome:", value = "" })
graphics.addCommand(screen, back)
graphics.addCommand(screen, ok)
graphics.addCommand(screen, switch)

graphics.handler(screen, {
    [back] = function()
        if prev ~= nil then graphics.display(prev) end
        os.exit(0)
    end,
    [ok] = function(value)
        print("Você digitou: " .. tostring(value))
        graphics.display(graphics.new("alert", "OK", "Salvo!"), prev)
    end,
    [switch] = graphics.taskmngr
})

os.setproc("screen", screen)
graphics.display(screen)