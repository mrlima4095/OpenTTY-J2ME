#!/bin/lua

local previous = graphics.getCurrent()
local screen = graphics.new("screen", "Credentials")
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local go = graphics.new("command", { label = "Change", type = "ok", priority = 1 })

graphics.append(screen, { type = "field", label = "Current Password" })
graphics.append(screen, { type = "field", label = "New Password" })
graphics.addCommand(screen, back)
graphics.addCommand(screen, go)
graphics.handler(screen, {
    [back] = function ()
        graphics.display(previous)
        os.exit(0)
    end,
    [go] = function (old, new)
        if string.trim(old) == "" or string.trim(new) == "" then
            
        end
        
        os.request(1, "passwd", { ["old"] = old, ["new"] = new })
    end
})