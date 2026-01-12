#!/bin/lua

for k,v in pairs({ ["PATCH"] = "Fear Fog", ["VERSION"] = getAppProperty("MIDlet-Version"), ["RELEASE"] = "stable", ["SHELL"] = "/bin/sh" }) do os.setenv(k, v) end
for k,v in pairs({ ["TYPE"] = "platform", ["CONFIG"] = "configuration", ["PROFILE"] = "profiles", ["LOCALE"] = "locale" }) do os.setenv(k, getAppProperty("/microedition." .. v)) end

local scope, db = { PWD = "/home/", USER = java.midlet.username }, {}

io.mount(io.read("/etc/fstab"))
os.request("1", "setsh", require("/bin/sh"))
pcall(io.popen, "/home/.initrc")

local appmenu = graphics.new("list", "OpenTTY " .. getAppProperty("MIDlet-Version"))
local launch = graphics.new("command", { label = "Launch", type = "ok", priority = 1 })
local refresh = graphics.new("command", { label = "Refresh", type = "ok", priority = 1 })
local config = graphics.new("command", { label = "Config", type = "ok", priority = 1 })
local quit = graphics.new("command", { label = "Quit", type = "ok", priority = 1 })
local menu = graphics.new("command", { label = "Menu", type = "ok", priority = 1 })


local function loadApps()
    graphics.clear(appmenu)

    local content = io.read("/home/.desktop")
    if not content or content == "" then
        content = "Lua,/bin/lua,"

        io.write(content, "/home/.desktop")
    end

    local apps = string.split(content, "\n")
    if type(apps) == "table" then
        for i = 1, apps.n do
            local entry = tostring(apps[i])
            if entry and entry ~= "" then
                local data = string.split(entry, ",")
                if type(data) == "table" and data.n >= 3 then
                    local appName, appPath, appArgs = tostring(data[1]), tostring(data[2]), tostring(data[3])

                    if appName and appName ~= "" then
                        graphics.append(appmenu, appName)
                        db[appName] = { ["app"] = appPath, ["args"] = appArgs }
                    end
                end
            end
        end
    end
end

local function launcher(appName)
    if not appName or appName == "" then return end

    local appData = db[appName]
    if not appData then
        graphics.display(graphics.new("alert", "Error", "App data not found: " .. appName))
        return
    end

    local result = io.popen(appData.app, appData.args)
    if result then
        if type(result) == "table" then
            local status = result[1]
            local output = result[2]
            if status and status.status and status.status == 0 then
                graphics.display(graphics.new("alert", "Success", "App launched successfully"))
            else
                graphics.display(graphics.new("alert", "Error", "Failed to launch app"))
            end
        else
            graphics.display(graphics.new("alert", "Result", tostring(result)))
        end
    else
        graphics.display(graphics.new("alert", "Error", "Failed to execute: " .. appData.app))
    end
end

local function showConfig()
    graphics.display(graphics.new("alert", "Config.", "Application Launcher v1.0"))
end

loadApps()

graphics.addCommand(appmenu, menu)
graphics.addCommand(appmenu, launch)
graphics.addCommand(appmenu, refresh)
graphics.addCommand(appmenu, config)
graphics.addCommand(appmenu, quit)

graphics.handler(appmenu, {
    [menu] = function() graphics.display(graphics.new("alert", "Menu", "Application Launcher v1.0")) end,
    [quit] = os.exit,

    [config] = showConfig,
    [refresh] = loadApps,

    [launch] = launcher,
    [graphics.fire] = launcher,
})
graphics.display(appmenu)

os.su(java.midlet.username)
os.scope(scope)