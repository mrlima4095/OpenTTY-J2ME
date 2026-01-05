#!/bin/lua

for k,v in pairs({ ["PATCH"] = "Fear Fog", ["VERSION"] = getAppProperty("MIDlet-Version"), ["RELEASE"] = "stable", ["SHELL"] = "/bin/sh" }) do os.setenv(k, v) end
for k,v in pairs({ ["TYPE"] = "platform", ["CONFIG"] = "configuration", ["PROFILE"] = "profiles", ["LOCALE"] = "locale" }) do os.setenv(k, getAppProperty("/microedition." .. v)) end

local scope = { PWD = "/home/", USER = java.midlet.username }

io.mount(io.read("/etc/fstab"))
print(string.env(io.read("/etc/motd")))

os.request("1", "setsh", require("/bin/sh"))
pcall(io.popen, "/home/.initrc")


local appmenu = graphics.new("list", "OpenTTY " .. getAppProperty("MIDlet-Version"))
local launch = graphics.new("command", { label = "Launch", type = "ok", priority = 1 })
local refresh = graphics.new("command", { label = "Refresh", type = "ok", priority = 1 })
local quit = graphics.new("command", { label = "Quit", type = "ok", priority = 1 })
local menu = graphics.new("command", { label = "Menu", type = "ok", priority = 1 })

local db = {}

local function load()
    graphics.clear(appmenu)

    local file = io.open("/home/.desktop")
    if not file then
        local content = "Terminal,/bin/xterm,\nMonitor,/bin/htop,,"
        io.write(content, "/home/.desktop")
    end

    local content = io.read("/home/.desktop")
    local apps = string.split(content, "\n")
    for i = 1, #apps do
        local entry = apps[i]
        local data = string.split(entry, ",")

        graphics.append(appmenu, entry[1])
        db[entry[1]] = { ["app"] = entry[2], ["args"] = entry[3] }
    end
end
local function launcher(app)
    local status, out = io.popen(db[app]["app"], db[app]["args"])
end

load()

graphics.addCommand(appmenu, menu)
graphics.addCommand(appmenu, launch)
graphics.addCommand(appmenu, refresh)
graphics.addCommand(appmenu, quit)
graphics.handler(appmenu, {
    [menu] = function ()
        graphics.display(graphics.new("alert", "Menu", "In development!"))
    end,
    [quit] = function () os.exit(nil) end,
    [refresh] = load,
    [launch] = launcher,
    [graphics.fire] = launcher,
})


os.su(java.midlet.username)
