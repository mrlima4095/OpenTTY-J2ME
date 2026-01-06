#!/bin/lua

local shell_builtin = { "builtin", "source", "gc", "ps", "uptime", "su", "whoami", "lognmae", "id", "exit", "xterm", "warn", "title", "alias", "unalias", "env", "set", "export", "unset", "eval", "echo", "date", "clear", "pwd", "cd", "cat", "ls", "buff", "open", "false", "true" }

if arg[1] then
    if arg[1] == "-p" then
        local passwd = arg[2]

        if passwd then
            local program = arg[3]
            if not program then
                print("sudo: missing program")
                os.exit(2)
            end

            if proge
        else
            print("sudo -y [passwd] [program] [args**]")
            os.exit(2)
        end
    end

    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "OpenTTY " .. os.getenv("VERSION"))
    local back = graphics.new("command", { label = "Back", type = "ok", priority = 1 })
    local run = graphics.new("command", { label = "Run", type = "ok", priority = 1 })

    graphics.append(screen, { type = "field", label = "[sudo] password for " .. java.midlet.username, value = "", mode = "password" })
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, back)
    graphics.handler(screen, {
        [back] = function ()
            graphics.display(previous)
            os.exit()
        end,
        [run] = function (query)
            local status = os.su("root", query)
            graphics.display(previous)

            if status == 0 then
                local res
            elseif status == 13 then
                print("sudo: permission denied")
                os.exit(13)
            end
        end
    })
else
    print("sudo [options] [program] [args**]")
    os.exit(2)
end
