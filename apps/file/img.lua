#!/bin/lua

if arg[1] then
    local previous = graphics.getCurrent()
    local viewer = graphics.new("screen", "Viewer")
    local back = graphics.new("command", { label = "Back", type = "ok", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

    graphics.append(viewer, { type = "image", img = os.join(arg[1])})
    graphics.addCommand(viewer, back)
    graphics.addCommand(viewer, switch)
    graphics.handler(viewer, {
        [back] = function ()
            graphics.display(previous)
            os.exit(0)
        end,
        [switch] = graphics.taskmngr
    })
    os.setproc("screen", viewer)
    graphics.display(viewer)
else
    print("imgview: usage: imgview [image]")
    os.exit(2)
end