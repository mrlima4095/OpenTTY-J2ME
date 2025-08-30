local graphics = {}

graphics["version"] = "1.0"

function graphics.Alert(message) return os.execute("warn " .. message) end
function graphics.Gauge(message) return os.execute("x11 gauge " .. message) end

-- Screen MODs
function graphics.SetTitle(text) return os.execute("title " .. text) end
function graphics.WindowTitle(text) return os.execute("x11 title " .. text) end

function graphics.SetTicker(message) return os.execute("x11 tick " .. message) end

-- Screen Stacking
function graphics.SaveWindow(name) return os.execute("x11 set " + name) end
function graphics.DropWindow(name) return os.execute("x11 unset " + name) end
function graphics.LoadWindow(name) return os.execute("x11 load " + name) end
function graphics.LoadProcess(name) return os.execute("x11 import " + name) end

-- Screens Building
function graphics.BuildList(config)
    local file, content, buffer = "", "", ""
    local itens, internals = config["itens"], {
        ["list.title"]            = "title",
        ["list.button"]           = "label",
        ["list.back"]             = "back",
        ["list.back.label"]       = "back.label",
        ["list.icon"]             = "icon"
    }

    if itens == nil then error("missing List itens") end

    for k,v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. config[v] end end
    for k,v in pairs(itens) do
        if buffer == "" then buffer = k else buffer = buffer .. "," .. k end

        file = file .. "\n" .. k .. "=" .. v
    end

    return os.execute("x11 list -e " .. file .. "\n" .. "list.content=" .. buffer)
end
function graphics.BuildScreen(config)
    local file, buffer = "", ""
    local fields, internals = config["fields"], {
        ["canvas.title"]          = "title",
        ["screen.title"]          = "title",
        ["screen.back"]           = "back",
        ["screen.back.label"]     = "back.label",
        ["screen.button"]         = "label",
        ["screen.button.cmd"]     = "run"
    }

    if fields == nil then error("missing Screen fields") end

    for k,v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. config[v] end end
    for k,v in pairs(fields) do
        if buffer == "" then buffer = k else buffer = buffer .. "," .. k end

        local ftype = v["type"]

        if ftype == nil then 
            error("missing type in field '" .. k .. "'")
        elseif ftype == "text" then
            local text = v["value"]
            if text == nil then error("missing value for text field '" .. k .. "'") end

            file = file .. "\nscreen." .. k .. ".type=text" .. "\nscreen." .. k .. ".value=" .. text .. "\nscreen." .. k .. ".style=" .. (v["style"] or "default") 
        elseif ftype == "image" then
            local image = v["img"]
            if image == nil then error("missing img for image field '" .. k .. "'") end

            file = file .. "\nscreen." .. k .. ".type=image" .. "\nscreen." .. k .. ".img=" .. image 
        elseif ftype == "item" then
            local label, cmd = v["label"], v["cmd"]
            if label == nil or cmd == nil then error("missing ITEM (label or cmd) config for field '" .. k .. "'") end

            file = file .. "\nscreen." .. k .. ".type=item" .. "\nscreen." .. k .. ".label=" .. label .. "\nscreen." .. k .. ".cmd=" .. cmd 
        elseif ftype == "spacer" then
            local w, h = v["w"], v["h"]

            file = file .. "\nscreen." .. k .. ".type=spacer" .. "\nscreen." .. k .. ".w=" .. (w or "1") .. "\nscreen." .. k .. ".h=" .. (h or "10")
        else error("invalid field type '" .. ftype .. "'") end
    end

    return os.execute("x11 make -e " .. file .. "\n" .. "screen.fields=" .. buffer)
end
function graphics.BuildCanvas(config)
    local file, buffer = "", ""
    local fields = config["fields"]

    local internals = {
        ["canvas.title"]          = "title",
        ["canvas.button"]         = "label",
        ["canvas.button.cmd"]     = "run",
        ["canvas.back.label"]     = "back.label",
        ["canvas.back"]           = "back",
        ["canvas.mouse"]          = "mouse",
        ["canvas.mouse.img"]      = "mouse.img",
        ["canvas.mouse.color"]    = "mouse.color",
        ["canvas.text.color"]     = "text.color",
        ["canvas.line.color"]     = "line.color",
        ["canvas.rect.color"]     = "rect.color",
        ["canvas.circle.color"]   = "circle.color",
        ["canvas.background"]     = "background",
        ["canvas.background.type"]= "background.type"
    }

    if fields == nil then error("missing Canvas fields") end

    for k, v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. tostring(config[v]) end end
    for id, spec in pairs(fields) do
        if buffer == "" then buffer = id else buffer = buffer .. "," .. id end

        local ftype = spec["type"]
        if ftype == nil then error("missing type in field '" .. id .. "'") end

        file = file .. "\ncanvas." .. id .. ".type=" .. tostring(ftype)

        if spec["link"]  ~= nil then file = file .. "\ncanvas." .. id .. ".link="  .. tostring(spec["link"])  end
        if spec["value"] ~= nil then file = file .. "\ncanvas." .. id .. ".value=" .. tostring(spec["value"]) end

        if spec["style"] ~= nil then file = file .. "\ncanvas." .. id .. ".style=" .. tostring(spec["style"]) 
        elseif ftype == "text" then file = file .. "\ncanvas." .. id .. ".style=default" end

        if spec["x"] ~= nil then file = file .. "\ncanvas." .. id .. ".x=" .. tostring(spec["x"]) end
        if spec["y"] ~= nil then file = file .. "\ncanvas." .. id .. ".y=" .. tostring(spec["y"]) end
        if spec["w"] ~= nil then file = file .. "\ncanvas." .. id .. ".w=" .. tostring(spec["w"]) end
        if spec["h"] ~= nil then file = file .. "\ncanvas." .. id .. ".h=" .. tostring(spec["h"]) end
    end

    return os.execute("x11 canvas -e " .. file .. "\ncanvas.fields=" .. buffer)
end

function graphics.BuildQuest(config)
    local file = ""
    local internals = {
        ["quest.title"]      = "title",
        ["quest.label"]      = "label",
        ["quest.key"]        = "key",
        ["quest.cmd"]        = "cmd",
        ["quest.cmd.label"]  = "cmd.label",
        ["quest.back"]       = "back",
        ["quest.back.label"] = "back.label",
        ["quest.content"]    = "content",
        ["quest.type"]       = "type",
    }

    if config["label"] == nil then error("missing quest.label") end
    if config["key"]   == nil then error("missing quest.key")   end
    if config["cmd"]   == nil then error("missing quest.cmd")   end

    for k, v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. tostring(config[v]) end end

    return os.execute("x11 quest -e " .. file)
end
function graphics.BuildEdit(config)
    local file = ""

    if config["key"] == nil then error("missing edit.key") end
    if config["cmd"] == nil then error("missing edit.cmd") end

    local internals = {
        ["edit.title"]      = "title",
        ["edit.content"]    = "content",
        ["edit.key"]        = "key",
        ["edit.cmd"]        = "cmd",
        ["edit.cmd.label"]  = "cmd.label",
        ["edit.back"]       = "back",
        ["edit.back.label"] = "back.label",
        ["edit.source"]     = "source",
    }

    for k, v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. tostring(config[v]) end end

    return os.execute("x11 edit -e " .. file)
end



return graphics