--[[
name=LuaEdit
version=1.0
description=Alternative Editor

api.version=1.16
api.error=warn LuaEdit requires OpenTTY 1.16
api.match=minimum

config=execute lua edit.lua
command=nano

nano=execute lua edit.lua; true
]]

local g = require("graphics.lua")
local edit, menu, explore = {
    title = "LuaEdit",
    key = "LEDIT_TXT",
    cmd = "execute set LEDIT_STATE=MENU; lua edit.lua; unset LEDIT_STATE; true",
    ["cmd.label"] = "Menu",
    back = "execute unset LEDIT_STATE;",
    ["back.label"] = "Back"
}, {
    title = "Menu",
    back = "execute unset LEDIT_STATE; lua edit.lua; true",
    itens = {
        ["Clear"] = "execute unset LEDIT_TXT LEDIT_TXT; lua edit.lua; true",
        ["Save"] = "execute set LEDIT_STATE=SAVE; lua edit.lua; true",
        ["Save as"] = "execute set LEDIT_STATE=SAVEAS; lua edit.lua; true",
        ["Open file"] = "execute set LEDIT_STATE=OPEN; lua edit.lua; true",
        ["About"] = "execute unset LEDIT_STATE; lua edit.lua; warn LuaEdit J2ME v1; true"
    }
}, {
    title = "LuaEdit"
}

local function load() 
    local state = os.getenv("LEDIT_STATE")
    
    if state == nil then
        local file = os.getenv("LEDIT_FILE")
        
        if file ~= nil then
            os.execute("set LEDIT_TXT=" .. io.read(file))
        end
        
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

local function main() edit["content"] = os.getenv("LEDIT_TXT") or "" g.BuildEdit(edit) end
local function save(filename)
    if filename == nil then
        explore["label"] = "(Save) File name",
        explore["key"] = "LEDIT_FILE",
        explore["cmd"] = "execute set LEDIT_STATE=SAVE; lua edit.lua; true",
        explore["back"] = "execute set LEDIT_STATE=MENU; lua edit.lua; true"
        
        g.BuildQuest(explore)
    else 
        io.write(os.getenv("LEDIT_TXT"), os.getenv("LEDIT_FILE"), "w")
        os.execute("execute unset LEDIT_STATE; lua edit.lua; true")
    end
end
local function open()
    explore["label"] = "(Open) File name",
    explore["key"] = "LEDIT_FILE",
    explore["cmd"] = "execute unset LEDIT_STATE; lua edit.lua; true",
    explore["back"] = "execute set LEDIT_STATE=MENU; lua edit.lua; true"
    
    g.BuidQuest(explore)
end

load()