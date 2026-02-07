#!/bin/lua

local shell = require("/bin/sh")
local alias, scope = {}, os.scope()

local version = "0.7"
local release_date = "2026-02-07"

os.setproc("name", "x11")
os.setproc("version", version)

if arg[1] == "screen" then
elseif arg[1] == "list" then
    if arg[2] then
        local file = io.open(os.join(arg[2]))
        if file then
            local conf = table.decode(io.read(file))
            local _list = string.split(conf["list.content"], ",")
            local _icon = conf["list.icon"]
            local previous = graphics.getCurrent()
            local screen = graphics.new("list", conf["list.title"])
            local back = graphics.new("command", { label = conf["list.back.label"] or "Back", type = "back", priority = 1 })
            local go = graphics.new("command", { label = conf["list.button"] or "Select", type = "ok", priority = 1 })

            for _,v in pairs(_list) do
                if _icon then
                    graphics.append(screen, v, _icon)
                else
                    graphics.append(screen, v)
                end
            end

            local function __run(opt)
                if conf[opt] then
                    pcall(shell, conf[opt], true, alias, io.stdout, scope)
                else
                    local time = string.split(os.date())
                    io.write(string.trim(io.read("/tmp/logs") .. "\n[WARN] " .. time[4] .. " An error occurred, '" .. opt .. "' not found"), "/tmp/logs", "a")
               end
            end

            graphics.addCommand(screen, back)
            graphics.addCommand(screen, go)
            graphics.display(screen, {
                [back] = function ()
                    graphics.display(previous)
                    if conf["list.back"] then
                        pcall(shell, conf["list.back"], true, alias, io.stdout, scope)
                    end
                    os.exit(0)
                end,
                [go] = __run, [graphics.fire] = __run
            })
            graphics.display(screen)
        else
            print("x11: list: " .. arg[2] .. ": not found")
        end
    else
        print("x11: list: missing file")
    end
elseif arg[1] == "quest" then
elseif arg[1] == "edit" then
elseif arg[1] == "version" then
    print("X Server " .. version)
else
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "OpenTTY X.Org")
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    graphics.append(screen, "OpenTTY X.Org - X Server " .. version .. "\nRelease Date: " .. release_date .. "\nX Protocol Version 1, Revision 3\nBuild OS: " .. getAppProperty("/microedition.profile"))

    graphics.addCommand(screen, back)
    graphics.handler(screen, { [back] = function () graphics.display(previous) os.exit(0) end })
    graphics.display(screen)
end