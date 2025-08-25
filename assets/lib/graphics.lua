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
        ["list.title"] = "title",
        ["list.button"] = "label",
        ["list.back"] = "back",
        ["list.back.label"] = "back.label"
        ["list.icon"] = "icon"
    }
    
    if itens == nil then error("missing List itens") end
    
    for k,v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. config[v] end end
    for k,v in pairs(itens) do
        if buffer == "" then
            buffer = k
        else
            buffer = buffer .. "," .. k
        end
        
        file = file .. "\n" .. k .. "=" .. v
    end

    return os.execute("x11 list -e " .. file .. "\n" .. "list.content=" .. buffer)
end
function graphics.BuildScreen(config)
    local file, buffer = "", ""
    local fields, internals = config["fields"], {
        ["screen.title"] = "title",
        ["screen.back"] = "back",
        ["screen.back.label"] = "back.label",
        ["screen.button"] = "label",
        ["screen.button.cmd"] = "run"
    }

    if fields == nil then error("missing Screen fields") end

    for k,v in pairs(internals) do if config[v] ~= nil then file = file .. "\n" .. k .. "=" .. config[v] end end
    for k,v in pairs(fields) do
        if buffer == "" then
            buffer = k
        else
            buffer = buffer .. "," .. k
        end

        for x,y in pairs(v) do
            print(x)
            print(v)
            --[[local type = y["type"]

            if type == nil then 
                error("missing type in field '" .. x .. "'")
            elseif type == "text" then
                local text = y["value"]

                if text == nil then error("missing value for text field '" .. x .. "'") end
                
                file = file .. "\nscreen." .. x .. ".type=text\nscreen." .. x .. ".value=" .. text .. "\nscreen." .. x .. ".style" .. y["style"] or "default"
            elseif type == "image" then
                local image = y["img"]

                if image == nil then error("missing value for text field '" .. x .. "'") end 

                file = file .. "\nscreen." .. x .. ".type=image\nscreen." .. x .. ".img=" .. image
            elseif type == "item" then
                local label, cmd = y["label"], y["cmd"]

                if label == nil or cmd == nil then error("missing ITEM (label or cmd) config") end

                file = file .. "\nscreen." .. x .. ".type=item\nscreen." .. x .. ".label=" .. label .. "\nscreen." .. x .. ".cmd=" .. cmd
            elseif type == "spacer" then
                local w, h = y["w"], y["h"]

                file = file .. "\nscreen." .. x .. ".type=spacer\nscreen." .. x .. ".w=" .. w or "1" .. "\nscreen." .. x .. ".h=" .. h or "10"
            else
                error("invalid field type '" .. type .. "'")
            end]]
        end
    end

    return os.execute("x11 make -e " .. file)
end


return graphics 