#!/bin/lua


local function menu(opt) 
    graphics.display(graphics.BuildList({
        back = { root = function() main() end },
        button = {
            label = "Select",
            root = function(sel)
                if sel == "Add" then
                    os.execute("warn Will open a Quest Window")
                elseif sel == "Delete" then
                    os.execute("warn Deleted " .. opt)
                end
            end
        }
    }))
end 
local screen = {
    title = "Services",
    back = { root = os.exit },
    button = { label = "Menu", root = menu },
    fields = { }
}
local function handler()
    
end
local function main()
    
end

if os.execute("case user (root) false") ~= 255 then
    print("Permission denied!")
    os.exit(13)
end


os.setproc("name", "systemd")
main()