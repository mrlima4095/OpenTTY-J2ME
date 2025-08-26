--[[
name=LuaEdit
version=1.0
description=Alternative Editor

api.version=1.16
api.error=execute warn LuaEdit requires OpenTTY 1.16; true
api.match=minimum

config=execute lua edit.lua
command=nano

nano=execute lua edit.lua; true
]]


local g = require("graphics")
local edit, menu, questing = {
    title = "LuaEdit",
    key = "LEDIT_TXT",
    cmd = "execute set LEDIT_STATE=MENU; lua /home/edit.lua; true",
    ["cmd.label"] = "Menu",
    back = "execute unset LEDIT_STATE; true"
}, {
    title = "Menu",
    back = "execute unset LEDIT_STATE; lua /home/edit.lua; true",
    itens = {
        ["Clear"] = "execute unset LEDIT_TXT LEDIT_STATE; lua /home/edit.lua; true",
        ["Save"] = "execute set LEDIT_STATE=SAVE; lua /home/edit.lua; true",
        ["Save as"] = "execute set LEDIT_STATE=SAVE; lua /home/edit.lua; true",
        ["Open file"] = "execute set LEDIT_STATE=OPEN; lua /home/edit.lua; true",
        ["View info"] = "execute unset LEDIT_STATE; warn LuaEdit v1\nAlternative Editor"
    }
}, { title = "LuaEdit" }

local function main()
    local txt = os.getenv("LEDIT_TXT")
    edit["content"] = txt or ""

    g.BuildEdit(edit)
    
    return nil
end

local function savefile()
    local file = os.getenv("LEDIT_FILE")
    if file == nil then
        questing["key"] = "LEDIT_FILE"
        questing["cmd"] = "execute set LEDIT_STATE=SAVE"
        questing["label"] = "(Save as) file name"

        g.BuildQuest(questing)
    end

    io.write(os.getenv("LEDIT_TXT") or "", os.getenv("LEDIT_FILE") or "nano", "w")
    os.execute("execute unset LEDIT_STATE; lua /home/edit.lua; true")

    return nil
end
local function openfile()
    questing["key"] = "LEDIT_FILE"
    questing["cmd"] = "execute unset LEDIT_STATE; read LEDIT_TXT $LEDIT_FILE; lua /home/edit.lua; true"
    questing["label"] = "(Open) file name"

    g.BuildQuest(questing)
    
    return nil
end

local function build()
    local state = os.getenv("LEDIT_STATE")

    if state == nil then
        main()
    elseif state == "MENU" then
        g.BuildList(menu)
    elseif state == "SAVE" then
        savefile()
    elseif state == "OPEN" then
        openfile()
    end

    return nil
end

build()