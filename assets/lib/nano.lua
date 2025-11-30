#!/bin/lua

local previous = graphics.getCurrent()
local app = {
    back = graphics.new("command", { label = "Back", type = "back", priority = 1 }),
    clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
}

os.setproc("name", "nano")


if arg[1] then
    app.filename, app.file = arg[1], arg[1]

    if string.sub(arg[1], 1, 1) ~= "/" then
        app.file = os.getcwd() .. app.file
    end

    app.content = io.read(app.file)
    app.editor = graphics.new("edit", "Nano - " .. app.filename, app.content)
else
    print("nano [file]") os.exit(2)
end

app.save = function ()
    local status = io.write(app.file, app.modified)
    if status == 0 then
        return true
    end

    local message = "java.io.IOException"
    if status == 5 then message = "read-only storage" elseif status == 13 then message = "Permission denied!" end

    graphics.display(graphics.new("alert", "Error", message), app.editor)
    return false
end



graphics.addCommand(app.editor, app.back)
graphics.addCommand(app.editor, app.clear)
graphics.handler(app.editor, {
    [app.back] = function(content)
        app.modified = tostring(content)

        if content == app.content then
            graphics.display(previous)
            os.exit()
        else
            os.exit(io.write(content, app.file))
        end
    end,
    [app.clear] = function()
        graphics.SetText(app.editor, "")
    end
})

graphics.display(app.editor)