#!/bin/lua

os.setproc("name", "passwd")

local previous = graphics.getCurrent()
local screen = graphics.new("screen", "Change Password")
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local go = graphics.new("command", { label = "Change", type = "ok", priority = 1 })
local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

graphics.append(screen, { type = "field", label = "Current Password", value = "", mode = "password" })
graphics.append(screen, { type = "field", label = "New Password", value = "", mode = "password" })
graphics.addCommand(screen, back)
graphics.addCommand(screen, go)
graphics.addCommand(screen, switch)
os.setproc("screen", screen)

local function finish(title, message)
    graphics.display(graphics.new("alert", title, message))
    os.exit(0)
end

graphics.handler(screen, {
    [back] = function ()
        graphics.display(previous)
        os.exit(0)
    end,
    [go] = function (old, new)
        old = string.trim(old)
        new = string.trim(new)

        if old == "" or new == "" then
            finish("Passwd", "Current and new password are required.")
            return
        end

        local ok, status = pcall(os.request, 1, "passwd", { ["old"] = old, ["new"] = new })
        if not ok then
            finish("Passwd", "Failed to change password.")
            return
        end

        if status == 13 then
            finish("Passwd", "Permission denied: wrong current password.")
        elseif status == 2 then
            finish("Passwd", "Invalid password input.")
        else
            finish("Passwd", "Password changed successfully.")
        end
    end,
    [switch] = graphics.taskmngr
})

graphics.display(screen)