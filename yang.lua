local app = {
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
    if app.source == "server" then
        graphics.SetTicker("Installing '" .. package .. "'...")
        local conn, i, o = socket.connect("socket://" .. app.mirror)
        io.write("get lib/" .. package, o)
        local raw = string.trim(io.read(i))
        graphics.SetTicker(nil)
        
        if raw == "File 'lib/" .. package .. "' not found." then
            print("yang: " .. package .. ": not found")
            os.exit(127)
        end
        
        os.write(raw, "/home/" .. package)
        
    elseif app.source == "proxy" then
        
    end
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
    elseif arg[1] == "remove" then

    elseif arg[1] == "update" then

    end
end


os.setproc("name", "yang")
app.main()