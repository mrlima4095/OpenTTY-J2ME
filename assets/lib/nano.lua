#!/bin/lua

local previous = graphics.getCurrent()
local app = {
    back = graphics.new("command", { label = "Back", type = "back", priority = 1 }),
    clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
}

os.setproc("name", "nano")

local function joinpath(pwd)
    if string.sub(pwd, 1, 1) ~= "/" then
        return os.getcwd() .. pwd
    end
    return pwd
end


if arg[1] then
    app.content = io.read(joinpath(arg[1]))
    app.editor = graphics.new("edit", "Nano - " .. arg[1], app.content)
else
    print("nano [file]") os.exit(2)
end

app.save = function ()
    local status = io.write(app.modified, joinpath(arg[1]))
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
        io.write(content, joinpath(arg[1]))
        graphics.display(previous)
    end,
    [app.clear] = function()
        graphics.SetText(app.editor, "")
    end
})

graphics.display(app.editor)