#!/bin/lua

if arg[1] then
    local previous = graphics.getCurrent()
    local viewer = graphics.new("screen", "Viewer")
    local back = graphics.new("command", { label = "Back", type = "ok", priority = 1 })

    graphics.append(viewer, graphics.render(os.join(arg[1])))
    graphics.addCommand(viewer, back)
    graphics.handler(viewer, {
        [back] = function ()
            graphics.display(previous)
            os.exit(0)
        end
    })
    graphics.display(viewer)
else
    print("img: usage: img [image]")
    os.exit(2)
end