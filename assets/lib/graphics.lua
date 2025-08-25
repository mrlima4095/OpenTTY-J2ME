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
            print(y)
            --[[local type = y["type"]

            ]]
        end
    end

    return os.execute("x11 make -e " .. file)
end


return graphics 