#!/bin/lua

local previous = graphics.getCurrent()
local app = {
    back = graphics.new("command", { label = "Back", type = "back", priority = 1 }),
    clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 }),
    save = graphics.new("command", { label = "Save", type = "screen", priority = 1 })
}

if arg[1] then
    app.content = io.read(os.join(arg[1]))
    app.editor = graphics.new("edit", "Nano - " .. arg[1])
    graphics.SetText(app.editor, app.content)
else
    print("nano: usage: nano [file]")
    os.exit(2)
end

function app.quit()
    graphics.display(previous)
    os.exit(0)
end
function app.write(content)
    local status = io.write(content, os.join(arg[1]))
    local message

    if status == 1 then message = "java.io.IOException" elseif status == 5 then message = "read-only storage" elseif status == 13 then message = "permission denied" end
    if status > 0 then
        graphics.display(graphics.new("alert", "Nano", message))
    end
end

os.setproc("name", "nano")

graphics.addCommand(app.editor, app.back)
graphics.addCommand(app.editor, app.save)
graphics.addCommand(app.editor, app.clear)
graphics.handler(app.editor, { [app.back] = app.quit, [app.clear] = function () graphics.SetText(app.editor, "") end, [app.save] = app.write })
graphics.display(app.editor)