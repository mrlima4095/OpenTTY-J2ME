#!/bin/lua

local shell_builtin = { "builtin", "source", "gc", "ps", "uptime", "su", "whoami", "lognmae", "id", "exit", "xterm", "warn", "title", "alias", "unalias", "env", "set", "export", "unset", "eval", "echo", "date", "clear", "pwd", "cd", "cat", "ls", "buff", "open", "false", "true" }

if arg[1] then
    for _,v in shell_builtin do
        if arg[1] == v then
            print("sudo: " .. arg[1] .. ": is a shell builtin")
            os.exit(127)
        end
    end

    if os.getuid() == 0 then
        local res = io.popen(os.join(arg[2]), arg[3])
        os.exit(tonumber(res))
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
            local scope = os.scope()
            local username = scope["USER"]
            local status = os.su("root", query)
            graphics.display(previous)

            if status == 0 then
                local program = arg[1]
                if not string.startswith(program, "/") then
                    program = "/bin/" .. arg[1]
                end
                local res = io.popen(program, arg[2])
                scope["USER"] = username
                os.exit(tonumber(res))
            elseif status == 13 then
                print("sudo: permission denied")
                os.exit(13)
            end
        end
    })
    graphics.display(screen)
else
    print("sudo [program] [args**]")
    os.exit(2)
end
