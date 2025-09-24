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

        io.close(conn) io.close(i) io.close(o)
    elseif app.source == "proxy" then
        raw, status = socket.http.get(app.proxy .. app.github .. package)
        graphics.SetTicker(nil)

        if status == 404 then
            print("yang: " .. package .. ": not found")
            os.exit(127)
        end
    end

    io.write(raw, "/home/" .. package)
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
    local args, i = {}, 1
    for k,v in pairs(arg) do
        if k ~= 0 then
           args[i] = v
           i = i + 1
        end
    end
    for k,v in pairs(args) do
        if v == "--proxy" then
            app.source = "proxy"
            table.remove(args, k)
        elseif v == "--update" then
            return app.update()
        end
    end

    if #args == 0 or args[1] == "list" then
        local list, i = {
            title = "Repository",
            type = "multiple",
            back = { label = "Back", root = os.exit },
            button = { label = "Install", root = app.prefetch },
            fields = {}
        }, 1
        for k,v in pairs(app.repo) do
            list.fields[i] = k
            i = i + 1
        end

        graphics.display(graphics.BuildList(list))
    elseif args[1] == "install" then
        for _,v in pairs(args) do
            app.install(v)
        end
    elseif args[1] == "remove" then
    elseif args[1] == "update" then end
end


os.setproc("name", "yang")
app.main()
