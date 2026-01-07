#!/bin/lua

local scope, alias = os.scope(), {}

local xterm = graphics.new("screen", "Terminal")
local run = graphics.new("command", { label = "Run", type = "ok", priority = 1 })

local function label() graphics.SetLabel(io.stdin, scope["USER"] .. " " .. os.getcwd() .. " $") end

label()

print(string.env(io.read("/etc/motd")))

graphics.append(xterm, io.stdout)
graphics.append(xterm, io.stdin)
graphics.addCommand(xterm, run)
graphics.handler(xterm, {
    [run] = function(command)
        if command ~= "" then
            graphics.SetText(io.stdin, "")

            local ok, msg = pcall(os.execute, command, true, alias, io.stdout, scope)
            if not ok then
                print(tostring(msg))
            end
            label()
        end
    end
})
graphics.db["xterm"] = xterm
graphics.display(xterm)

