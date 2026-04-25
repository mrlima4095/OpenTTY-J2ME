#!/bin/lua

--[[

    // Build APP Shell
    if (PKG.containsKey("shell.name") && PKG.containsKey("shell.args")) { 
    String[] args = split((String) PKG.get("shell.args"), ','); 
    Hashtable TABLE = new Hashtable(); 
    for (int i = 0; i < args.length; i++) { 
    String NAME = args[i].trim(), VALUE = (String) PKG.get(NAME); 
    TABLE.put(NAME, (VALUE != null) ? VALUE : ""); } 
    if (PKG.containsKey("shell.unknown")) { TABLE.put("shell.unknown", (String) PKG.get("shell.unknown")); } shell.put(((String) PKG.get("shell.name")).trim(), TABLE); }

    return 0;
}
]]

local function launch(source, content)
    local pkg = table.decode(content)

    if pkg["api.version"] then
        local version = os.getenv("VERSION")
        local matching = pkg["api.match"] or "exact-prefix"
        local ok = true

        if matching == "exact-prefix" then
            ok = string.startswith(version, pkg["api.version"])
        elseif matching == "minimum" or matching == "maximum" then
            
        elseif matching == "exact-full" then
            ok = version == pkg["api.version"]
        end

        if not ok then
            if pkg["api.error"] then
                os.execute(pkg["api.error"])
            end
            os.exit(3)
        end
    end
    if pkg["api.require"] then
        local nodes = string.split(pkg["api.require"], ",")
        local ok = true
        for _, node in pairs(nodes) do
            if node == "lua" and not java.class("Lua") then ok = false
            elseif node == "canvas" and not java.class("LuaCanvas") then ok = false
            elseif node == "devicefs" and not java.class("javax.microedition.io.FileConnection") then ok = false
            elseif node == "prg" and not java.class("javax.microedition.io.PushRegistry") then ok = false
            end
        end

        if not ok then
            if pkg["api.error"] then
                os.execute(pkg["api.error"])
            end
            os.exit(3)
        end
    end

    if pkg["include"] then
        local apps = string.split(pkg["api.require"], ",")
        for _, app in pairs(apps) do
            local file = io.open(os.join(app))
            if file then
                launch(io.read(file))
            else
                if pkg["include.error"] then
                    os.execute(pkg["include.error"])
                end
                os.exit(127)
            end
        end
    end

    if pkg["process.name"] then
        os.setproc("name", pkg["process.name"])
        os.setproc(false)
    end

    if pkg["process.port"] then

    end

    if pkg["config"] then
        local status = os.execute(pkg["config"])
        if status > 0 then
            os.exit(status)
        end
    end
    if pkg["mod"] and pkg["process.name"] then
        local function background()
            local status = os.execute(pkg["mod"])
            if status > 0 then
                os.exit(status)
            end
        end
        java.run(background, "MIDlet-MOD")
    end

    if pkg["command"] then
        local scope = os.scope()
        local keys = string.split(pkg["command"], ",")
        for _, key in pairs(keys) do
            if pkg[key] then
                scope["ALIAS"][key] = pkg[key]
            else
                print("launch: " .. source .. ": no content for alias '" .. key .. "'")
                os.exit(1)
            end
        end
    end
    if pkg["file"] then
        local files = string.split(pkg["command"], ",")
        for _, f in pairs(files) do
            if pkg[f] then
                io.write(pkg[f], "/home/" .. string.env(f))
            else
                print("launch: " .. source .. ": no content for file '" .. f .. "'")
                os.exit(1)
            end
        end
        
    end

    if pkg["shell.name"] and pkg["shell.args"] then
        
    end
end

if arg[1] then
    local file = io.open(os.join(arg[1]))
    if file then
        launch(io.read(file))
    else
        print("launch: " .. arg[1] .. ": not found")
        os.exit(127)
    end
else
    print("launch [source]")
end