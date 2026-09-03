#!/bin/lua

if arg[1] then
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "OpenTTY " .. getAppProperty("MIDlet-Version"))
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    local run = graphics.new("command", { label = "Run", type = "screen", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })
    
    graphics.append(screen, { type = "field", label = "[sudo] password for " .. java.midlet.username, value = "", mode = "password" })
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, run)
    graphics.addCommand(screen, switch)
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
                local i = 1
                local cmd = ""

                while i < #arg - 1 do
                    cmd = cmd .. arg[i + 1] .. " "
                    i = i + 1
                end
                
                local cmd_status = os.execute(arg[1])

                os.su(username)

                os.exit(cmd_status)
            else
                print("Permission denied!")
                os.exit(13)
            end
        end,
        [switch] = graphics.taskmngr
    })
    graphics.display(screen)
else
    print("sudo [command]")
end