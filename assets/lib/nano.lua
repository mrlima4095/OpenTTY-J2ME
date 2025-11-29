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
    app.editor = graphics.new("edit", "Nano - New Buffer", "")
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

app.quest = function ()
    local quest = graphics.new("edit", "Save As", "")
    local save = graphics.new("command", { label = "Save", type = "ok", priority = 1 })
    local back = graphics.new("command", { label = "Cancel", type = "back", priority = 2 })

    graphics.addCommand(quest, save)
    graphics.addCommand(quest, back)
    graphics.handler(quest, {
        [save] = function(save_args)
            local new_filename = save_args
            if new_filename and string.len(new_filename) > 0 then
                local result = app.save()
                if result then
                    graphics.display(previous)
                    os.exit()
                end
            else
                graphics.display(graphics.new("alert", "Error", "Filename cannot be empty!"))
            end
        end,

        [back] = function()
            graphics.display(app.editor)
        end
    })
    graphics.display(quest)
end

app.confirm = function ()
    local alert = graphics.new("alert", "Save Changes?", "The file has been modified. Save changes?")

    local yes = graphics.new("command", { label = "Save", type = "ok", priority = 1 })
    local no = graphics.new("command", { label = "Don't Save", type = "screen", priority = 2 })
    local cancel = graphics.new("command", { label = "Cancel", type = "cancel", priority = 3 })

    graphics.handler(alert, {
        [yes] = function ()
            if app.file then
                if app.save() then
                    graphics.display(previous)
                    os.exit()
                end
            else
                app.quest()
            end
        end,
        [no] = function ()
            graphics.display(previous)
            os.exit()
        end,
        [cancel] = function ()
            graphics.display(app.editor)
        end
    })
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
            app.confirm()
        end
    end,
    [app.clear] = function ()
        graphics.SetText(app.editor, "")
    end
})

graphics.display(app.editor)