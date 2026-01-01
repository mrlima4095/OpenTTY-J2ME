#!/bin/lua

local previous = graphics.getCurrent()
local app = {
    back = graphics.new("command", { label = "Back", type = "back", priority = 1 }),
    clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
}

if arg[1] then
    app.content = io.read(os.join(arg[1]))
    app.editor = graphics.new("edit", "Nano - " .. arg[1])
    graphics.SetText(app.editor, app.content)
else
    print("nano: usage: nano [file]")
    os.exit(2)
end

graphics.addCommand(app.editor, app.back)
graphics.addCommand(app.editor, app.clear)
graphics.handler(app.editor, {
    [app.back] = function (content)
        local status = io.write(content, os.join(arg[1]))
        graphics.display(previous)
        os.exit(tonumber(status))
    end,
    [app.clear] = function ()
        graphics.SetText(app.editor, "")
    end
})
graphics.display(app.editor)