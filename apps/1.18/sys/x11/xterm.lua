#!/bin/lua

local xterm = graphics.new("screen", "OpenTTY " .. getAppProperty("MIDlet-Version"))
local run = graphics.new("command", { label = "Run", type = "ok", priority = 1 })
local historyCmd = graphics.new("command", { label = "history", type = "screen", priority = 2 })
local switchCmd = graphics.new("command", { label = "Switch to...", type = "screen", priority = 3 })
local historyBack = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local history = graphics.new("list", "Command History")

local stdout = graphics.new("buffer", { label = "", value = "", style = "monospace" })
local stdin = graphics.new("field", { label = "Command", value = "", length = 256, mode = "" })

os.setproc("name", "xterm")
os.setproc("stdout", stdout)
os.setproc("screen", xterm)

io.stdout = stdout
io.stdin = stdin

local scope, hostname = os.scope(), io.read("/etc/hostname")

print(string.env(io.read("/etc/motd")))
pcall(io.popen, "/home/.initrc")

local function label() graphics.SetLabel(stdin, "[" .. scope["USER"] .. "@" .. hostname .. " " .. os.getcwd() .. "] " .. (os.getuid() == 0 and "#" or "$")) end

label()

graphics.append(xterm, stdout)
graphics.append(xterm, stdin)
graphics.addCommand(xterm, run)
graphics.addCommand(xterm, historyCmd)
graphics.addCommand(xterm, switchCmd)
graphics.handler(xterm, {
    [run] = function(command)
        if command ~= "" then
            graphics.append(history, command)
            graphics.SetText(stdin, "")
            local ok, msg = pcall(os.execute, command)
            if not ok then
                print(tostring(msg))
            end
            label()
        end
    end,
    [historyCmd] = function()
        graphics.display(history)
    end,
    [switchCmd] = graphics.taskmngr
})
graphics.addCommand(history, historyBack)
graphics.handler(history, {
    [historyBack] = function()
        graphics.display(xterm)
    end
})
graphics.db["xterm"] = xterm
graphics.display(xterm)
