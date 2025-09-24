local app = {
    version = "1.5",

    mirror = os.getenv("REPO") or "opentty.xyz:31522",
    github = "raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/main/assets/lib/",
    proxy = getAppProperty("MIDlet-Proxy") or "http://opentty.xyz/proxy.php?",

    source = "server",

    root = "/home/",
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
        ["PasteBin"] = "pastebin",
        ["SmartME SDK"] = "sdkme",
        ["Updater"] = "sync",
        ["ViaVersion"] = "viaversion",
        ["WebProxy"] = "proxy.lua"
    }

}

function app.install(pkg)
    graphics.SetTicker("Downloading '" .. pkg .. "'")

    if app.source == "server" then
        local conn, i, o = socket.connect("socket://" .. app.mirror); io.write("get lib/" .. pkg, o)
        local raw = io.read(i, 4096)

        graphics.SetTicker(nil)
        io.close(i) io.close(o)
        io.close(conn)

        if string.trim(raw) == "File 'lib/" .. pkg .. "' not found" then print("Unable to locate package: " .. pkg) os.exit(127) end

        io.write(raw, app.root .. pkg)
    elseif app.source == "proxy" then
        local raw, status = socket.http.get(app.proxy .. app.github .. pkg)
        graphics.SetTicker(nil)

        if status == 200 then io.write(raw, app.root .. pkg)
        elseif status == 404 then print("Unable to locate package: " .. pkg) os.exit(127)
        else print("[ Yang ] Downloading failed!\n[ Yang ] HTTP Status: " .. status) os.exit(1) end
    end
end
function app.update()
    graphics.SetTicker("Updating...")

    local this = "/home/yang.lua"
    if string.sub(arg[0], 1, 1) == "/" then this = arg[0]
    else this = os.getcwd() .. arg[0] end

    if app.source == "server" then
        local conn, i, o = socket.connect("socket://" .. app.mirror); io.write("get lib/yang.lua", o)
        local raw = io.read(i, 9999)

        graphics.SetTicker(nil)
        io.close(i) io.close(o)
        io.close(conn)

        io.write(raw, this)
    elseif app.source == "proxy" then
        local raw, status = socket.http.get(app.proxy .. app.github .. "yang.lua")
        graphics.SetTicker(nil)

        if status == 200 then io.write(raw, this)
        else print("[ Yang ] Updating failed!\n[ Yang ] HTTP Status: " .. status) os.exit(1) end
    end
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
        local list, idx = {
            title = "Repository",
            type = "multiple",
            back = { label = "Back", root = os.exit },
            button = { label = "Install", root = app.prefetch },
            fields = {}
        }, 1
        for k,v in pairs(app.repo) do
            list.fields[idx] = k
            idx = idx + 1
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
