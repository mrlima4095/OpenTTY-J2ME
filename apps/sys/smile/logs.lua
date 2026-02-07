#!/bin/lua

if arg[1] == "add" then
    if arg[2] and arg[3] then
        local file = io.open("/tmp/logs")
        local time = string.split(os.date())
        local hour = time[4]
        local logs
        if file then
            logs = io.read(logs)
        else
            logs = "[" .. string.upper(arg[2]) .. "] " .. hour .. " " .. arg[3]
        end

        os.exit(tonumber(io.write(logs, "/tmp/logs")))
    else
        print("log: usage: log add [level] [message]")
    end
elseif arg[1] == "view" then
    local file = io.open("/tmp/logs")
    if file then
        local previous = graphics.getCurrent()
        local screen = graphics.new("screen", "MIDlet Logs")
        local back = graphics.new("button", {})

        graphics.append(screen, io.read(file))
        graphics.addCommand(screen, back)
        graphics.handler(screen, {
            [back] = function ()
                graphics.display(previous)
                os.exit(0)
            end
        })
        graphics.display(screen)
    else
        print("no logs on session")
    end
elseif arg[1] == "swap" then
    local file = io.open("/tmp/logs")
    if file then
        if arg[2] then
            os.exit(tostring(io.write(io.read(file), os.join(arg[2]))))
        else
            print("log: usage: log swap [file]")
        end
    else
        print("no logs on session")
    end
elseif arg[1] == "read" then
    local file = io.open("/tmp/logs")
    if file then
        print(io.read(file))
    else
        print("no logs on session")
    end
elseif arg[1] == "help" then
    print("log: levels [info|trace|debug|warn|error]")
else
    print("log: usage: log [...]")
end