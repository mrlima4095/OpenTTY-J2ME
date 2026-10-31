-- Lua J2ME - Code examples
-- This is an example of building an interactive list with the graphics API

local previous = graphics.getCurrent() -- Get current screen

local list = graphics.new("list", "Main Menu", "implicit")
list:append("Settings")
list:append("Games")
list:append("Tools")
list:append("Exit")

local select = graphics.new("command", { label = "Select", type = "ok", priority = 1 })
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
list:addCommand(select)
list:addCommand(back)

-- One handler for every command: (screen, label, ...values).
list:handler(function(screen, label, option)
    if label == "Select" then
        print("Selected: " .. tostring(option))
    else
        graphics.display(previous)
    end
end)

list:display()
