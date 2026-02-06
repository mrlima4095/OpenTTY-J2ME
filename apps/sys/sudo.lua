#!/bin/lua

local shell_builtin = { "builtin", "source", "gc", "ps", "uptime", "su", "whoami", "lognmae", "id", "exit", "xterm", "warn", "title", "alias", "unalias", "env", "set", "export", "unset", "eval", "echo", "date", "clear", "pwd", "cd", "cat", "ls", "buff", "open", "false", "true" }

if arg[1] then
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
                local res = io.popen(os.join(arg[2]), arg[3], true)
            elseif status == 13 then
                print("sudo: permission denied")
                os.exit(13)
            end
        end
    })
else
    print("sudo [program] [args**]")
    os.exit(2)
end
