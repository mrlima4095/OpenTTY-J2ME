#!/bin/lua

os.setproc("name", "java")

if arg[1] then
    if arg[1] == "-class" then
        if arg[2] then
            print(java.class(arg[2]))
        else
            print("java -class [class]")
        end
    elseif arg[1] == "--version" then
        print(java.getName())
    else
        print("java: " .. arg[1] .. ": not found")
        os.exit(127)
    end
else
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "Java ME")
    local back = graphics.new("command", { label = "Back", type = "ok", priority = 1 })

    graphics.append(screen, string.env("Java 1.2 (OpenTTY Edition)\n\nMicroEdition-Config: $CONFIG\nMicroEdition-Profile: $PROFILE"))
    graphics.addCommand(screen, back)
    graphics.handler(screen, {
        [back] = function ()
            graphics.display(previous)
            os.exit()
        end
    })
    graphics.display(screen)
end