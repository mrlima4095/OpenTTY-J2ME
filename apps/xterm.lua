#!/bin/lua

local scope, alias, timeline = os.scope(), {}, {}

local xterm = graphics.new("screen", "Terminal")
local run = graphics.new("command", { label = "Run", type = "ok", priority = 1 })
local clear = graphics.new("command", { label = "Clear", type = "screen", priority = 1 })
local history = graphics.new("command", { label = "History", type = "screen", priority = 1 })

local function label() graphics.SetLabel(io.stdin, scope["USER"] .. " " .. os.getcwd() .. " $") end

label()

print(string.env(io.read("/etc/motd")))

graphics.append(xterm, io.stdout)
graphics.append(xterm, io.stdin)
graphics.addCommand(xterm, run)
graphics.handler(xterm, {
    [run] = function(command)
        if command ~= "" then
            if timeline[#timeline] ~= command then
                timeline[#timeline + 1] = command
            end
            graphics.SetText(io.stdin, "")

            local ok, msg = pcall(os.execute, command, true, alias, io.stdout, scope)
            if not ok then
                print(tostring(msg))
            end
            label()
        end
    end,
    [clear] = function () graphics.SetText(io.stdout, "") end,
    [history] = function ()
        local list = graphics.new("list", "History")
        local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
        local go = graphics.new("command", { label = "Go", type = "screen", priority = 1 })

        local function fire(command) graphics.SetText(io.stdin, command) end

        for i = 1, #timeline do graphics.append(list, timeline[i]) end
        graphics.addCommand(list, back)
        graphics.addCommand(list, go)
        graphics.handler(list, {
            [back] = function () graphics.display(xterm) end,
            [go] = fire, [graphics.fire] = fire
        })
        graphics.display(list)
    end
})
graphics.db["xterm"] = xterm
graphics.display(xterm)
