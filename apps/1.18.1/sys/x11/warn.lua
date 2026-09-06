#!/bin/lua

if arg[1] and arg[2] then
    local alert = graphics.new("alert", arg[1], arg[2])
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })
    graphics.addCommand(alert, switch)
    graphics.handler(alert, { [switch] = graphics.taskmngr })
    os.setproc("screen", alert)
    graphics.display(alert)
else
    print("warn [title] [body]")
    print("- display an alert window")
end