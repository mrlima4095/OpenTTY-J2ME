--[[

config=execute touch /home/.yang-lock; case !key (REPO) set REPO=opentty.xyz:31522; 
command=yang

yang=execute lua /home/yang.lua;

]]

local mirror = os.getenv("REPO") or "opentty.xyz:31522"

local repo = {
    ["Android ME"] = "android",
    ["Armitage"] = "armitage",
    ["Auto Clean"] = "autogc",
    ["Auto Syntax"] = "tab",
    ["Back Previous"] = "bprevious",
    ["BoxME"] = "boxme",
    ["CMatrix"] = "cmatrix",
    ["Discord (MIDlet)"] = { url = "http://146.59.80.3/discord_midp2_beta.jar", author = "gtrxac" },
    ["Forge"] = "forge",
    ["Github (MIDlet)"] = { url = "http://nnp.nnchan.ru/dl/GH2ME.jar", author = "shinovon" },
    ["GoBuster (Word list)"] = "gobuster",
    ["Graphics (Lua)"] = "graphics.lua",
    ["ImmersiveShell"] = "sh2me",
    ["JBuntu"] = "jbuntu",
    ["JBenchmark"] = "debuggers",
    ["J2ME Loader"] = "modme",
    ["Math (Lua)"] = "math.lua",
    ["MobiX Loader"] = "mxos",
    ["PackJ (Update)"] = "yang",
    ["PackJ (Proxy)"] = "yang-proxy",
    ["PasteBin"] = "pastebin",
    ["SmartME SDK"] = "sdkme",
    ["Updater"] = "sync",
    ["ViaVersion"] = "viaversion"
}

local function install(pkg)
    if string.match(pkg, "MIDlet") then
        local MIDlet = repo[pkg]
        
        os.execute("execute warn This is a 3rd MIDlet from '" .. MIDlet.author .. "'; bg exec sleep 3 & open " .. MIDlet.url) os.exit()
    end

    local filename = repo[pkg]
    local conn, i, o = socket.connect("socket://" .. mirror)

    io.write("get lib/" .. filename, o)
    local content = io.read(i)

    if content == "File 'lib/" .. filename .. "' not found." then
        os.execute("warn Error while installing package!") os.exit(1)
    end

    io.write(content, filename)
end

local function menu()
    local m = {
        title = "Repository",

        back = { label = "Back", root = function () os.exit() end },
        button = { label = "Install", root = install },

        fields = {}
    }

    local index = 1
    for k, v in pairs(repo) do
        m.fields[index] = k

        index = index + 1
    end

    graphics.display(graphics.list(m))
end

menu()