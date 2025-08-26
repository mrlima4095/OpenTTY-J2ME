local g = require("graphics.lua")
local edit, menu, explore = {
    title = "LuaEdit",
    content = os.getenv("LEDIT_TXT") or "",
    key = "LEDIT_TXT",
    cmd = "execute set LEDIT_STATE=MENU; lua edit.lua; unset LEDIT_STATE; true",
    ["cmd.label"] = "Menu",
    back = "execute unset LEDIT_STATE;",
    ["back.label"] = "Back",
}, {
    title = "Menu",
    back = "execute unset LEDIT_STATE; lua edit.lua; true"
    itens = {
        ["Clear"] = "execute unset LEDIT_TXT LEDIT_TXT; lua edit.lua; true",
        ["Save"] = "execute set LEDIT_STATE=SAVE; lua edit.lua; true",
        ["Save as"] = "execute set LEDIT_STATE=SAVEAS; lua edit.lua; true",
        ["Open file"] = "execute set LEDIT_STATE=OPEN; lua edit.lua; true",
        ["About"] = "execute unset LEDIT_STATE; lua edit.lua; warn LuaEdit J2ME v1; true"
    }
}, {
    title = "LuaEdit",
}

local function load() 
    local state = os.getenv("LEDIT_STATE")
    
    if state == nil then
        main()
    elseif state == "SAVE" then
        save(os.getenv("LEDIT_FILE"))
    elseif state == "SAVEAS" then
        save()
    elseif state == "OPEN" then
        open()
    elseif state == "MENU" then
        
    else 
        main()
    end
end

local function main() g.BuildEdit(edit) end
local function save(filename)
    if filename == nil then
        explore["label"] = "File name"
        explore["key"]
        
    else 
        io.write(os.getenv("LEDIT_TXT"), os.getenv("LEDIT_FILE"), "w")
        os.execute("execute unset LEDIT_STATE; lua edit.lua; true")
    end
end