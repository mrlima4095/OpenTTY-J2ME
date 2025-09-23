Local app = {
    version = "1.5",

    mirror = os.getenv("REPO") or "opentty.xyz:31522",
    github = "raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/lib/",
    proxy = getAppProperty("MIDlet-Proxy") or "http://opentty.xyz/proxy.php?",

    source = "server",

    repo = {
        ["Android ME"] = "android",
        ["Armitage"] = "armitage",
        ["Auto Clean"] = "autogc",
        ["Auto Syntax"] = "tab",
        ["Back Previous"] = "bprevious",
        ["BoxME"] = "boxme",
        ["Forge"] = "forge",
        ["GoBuster (word list)"] = "gobuster",
        ["JBuntu"] = "jbuntu",
        ["JBenchmark"] = "debuggers",
        ["J2ME Loader"] = "modme",
        ["MobiX Loader"] = "mxos",
        ["PackJ (Proxy)"] = "yang-proxy",
        ["PasteBin"] = "pastebin",
        ["SmartME SDK"] = "sdkme",
        ["Updater"] = "sync",
        ["ViaVersion"] = "viaversion",
        ["WebProxy"] = "proxy.lua"
    }

}

function app.install(package)
    local raw, status = "", 200
    graphics.SetTicker("Installing '" .. package .. "'...")
    if app.source == "server" then
        local conn, i, o = socket.connect("socket://" .. app.mirror)
        io.write("get lib/" .. package, o)
        raw = string.trim(io.read(i))
        graphics.SetTicker(nil)
        
        if raw == "File 'lib/" .. package .. "' not found." then
            print("yang: " .. package .. ": not found")
            os.exit(127)
        end
    elseif app.source == "proxy" then
        raw, status = socket.http.get(app.proxy .. app.github .. package)
        if status == 404 then
            print("yang: " .. package .. ": not found")
            os.exit(127)
        end
    end
    
    os.write(raw, "/home/" .. package)
    print("[ Yang ] " .. package .. " installed")
end
function app.update()
    
end

function app.prefetch(...)
    local query = ...

    for _,v in query do
        if app.repo[v] == nil then
            print("yang: " .. package .. ": not found")
            os.exit(127)
        end

        app.install(app.repo[v])
    end
end

function app.main()
    for k,v in pairs(arg) do
        if v == "--proxy" then
            app.source = "proxy"
            table.remove(arg, k)
        elseif v == "--update" then
            return app.update()
        end
    end

    if #arg == 1 or arg[1] == "list" then
        local list, i = {
            title = "Repository",
            back = { label = "Back", root = os.exit },
            button = { label = "Install", root = app.prefetch },
            fields = {}
        }, 1
        for k,v in pairs(app.repo) do
            list.fields[i], i = k, i + 1
        end

        graphics.display(graphics.BuildList(list))
    elseif arg[1] == "install" then
        for k,v in pairs(arg) do
            app.install(v)
        end
    end
end


os.setproc("name", "yang")
app.main()
