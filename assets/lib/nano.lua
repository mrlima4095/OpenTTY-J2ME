#!/bin/lua

local previous = graphics.getCurrent()
local app = {
    back = graphics.new("command", { label = "Back", type = "back", priority = 1 }),
    clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 }),
    yes = graphics.new("command", { label = "Yes", type = "ok", priority = 1 }),
    no = graphics.new("command", { label = "No", type = "back", priority = 1 })
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
function app.save(content)
    if content ~= app.content then
        local alert = graphics.new("alert", "Nano", "Save Buffer?")
        graphics.addCommand(alert, app.yes)
        graphics.addCommand(alert, app.no)
        graphics.handler(alert, {
            [app.yes] = function ()
                local status = io.write(content, os.join(arg[1]))
                local message

                if status == 1 then message = "java.io.IOException" elseif status == 5 then message = "read-only storage" elseif status == 13 then message = "permission denied" end
                if status > 0 then
                    graphics.display(previous)
                    graphics.display(graphics.new("alert", "Nano", message))
                    os.exit(status)
                end

                graphics.display(previous)
                os.exit(tonumber(status))
            end,
            [app.no] = app.quit()
        })
        graphics.display(alert)

        return
    else
        app.quit()
    end
end

os.setproc("name", "nano")

graphics.addCommand(app.editor, app.back)
graphics.addCommand(app.editor, app.clear)
graphics.handler(app.editor, { [app.back] = app.save, [app.clear] = function () graphics.SetText(app.editor, "") end })
graphics.display(app.editor)