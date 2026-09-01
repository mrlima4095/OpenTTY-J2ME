-- Lua J2ME - Code examples
-- This is an example of building an interactive list with the graphics API

local previous = graphics.getCurrent() -- Get current screen

local list = graphics.new("form", "Main Menu", "implicit")
graphics.append(list, "Settings")
graphics.append(list, "Games")
graphics.append(list, "Tools")
graphics.append(list, "Exit")

local select = graphics.new("command", { label = "Select", type = "ok", priority = 1 })
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
graphics.addCommand(list, select)
graphics.addCommand(list, back)

graphics.handler(list, {
    [select] = function(option)
        print("Selected: " .. option)
    end,
    [back] = function()
        graphics.display(previous)
    end
})

graphics.display(list)
