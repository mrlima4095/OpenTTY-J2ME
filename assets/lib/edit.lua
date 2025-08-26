local g = require("graphics.lua")
local editor, menu = { title = "LuaEdit", key = "EDIT_TXT", cmd = "execute lua edit.lua" }, { title = "Menu" }

function load()
    local state = os.getenv("EDIT_STATE")

    if state == nil then g.BuildEdit(editor) else g.BuildList(menu) end
end

