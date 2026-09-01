#!/bin/lua

local version = "0.7"
local release_date = "2026-09-01"

os.setproc("name", "x11")
os.setproc("version", version)

local function warn(key)
    local time = string.split(os.date())
    local log = io.read("/tmp/logs")
    io.write(string.trim((log or "") .. "\n[WARN] " .. time[4] .. " An error occurred, '" .. tostring(key) .. "' not found"), "/tmp/logs", "a")
end

local function num(value, fallback)
    if value and value ~= "" then
        local ok, n = pcall(tonumber, string.trim(value))
        if ok and n then return n end
    end
    return fallback
end

local function expand(conf)
    local out = {}
    for k, v in pairs(conf) do
        if type(k) == "string" then out[k] = string.env(tostring(v)) end
    end
    return out
end

local function loadconf(file)
    local handle = io.open(os.join(file))
    if handle then
        return expand(table.decode(io.read(handle)))
    end
    print("x11: " .. file .. ": not found")
    return nil
end

local function source(target, inline)
    if target == nil then
        print("x11: missing file")
        return nil
    elseif target == "-e" then
        if inline then return expand(table.decode(inline)) end
        print("x11: missing inline content after -e")
        return nil
    end
    return loadconf(target)
end

local function xterm()
    local prev = graphics.db["xterm"]
    if prev then graphics.display(prev) end
end

local function runCommand(cmd)
    if not cmd then return end
    local steps = string.split(cmd, ";")
    for i = 1, #steps do
        local step = string.trim(steps[i])
        if step ~= "" then
            if string.startswith(step, "execute ") then
                step = string.trim(string.sub(step, string.len("execute ")))
            end
            pcall(os.execute, step)
        end
    end
end

local function buildScreen(conf)
    if not conf then return end
    local screen = graphics.new("screen", conf["screen.title"] or "OpenTTY")

    if conf["screen.content"] then
        graphics.append(screen, { type = "text", label = "", value = conf["screen.content"] })
    end

    if conf["screen.fields"] then
        local ids = string.split(conf["screen.fields"], ",")
        for i = 1, #ids do
            local id = string.trim(ids[i])
            if id ~= "" then
                local ftype = string.lower(string.trim(conf[id .. ".type"] or "text"))
                if ftype == "image" then
                    if conf[id .. ".img"] then
                        graphics.append(screen, { type = "image", img = conf[id .. ".img"] })
                    end
                elseif ftype == "text" then
                    graphics.append(screen, { type = "text", label = conf[id .. ".label"] or "", value = conf[id .. ".value"] or "", style = conf[id .. ".style"] or "default" })
                elseif ftype == "item" then
                    local cmd = conf[id .. ".cmd"]
                    graphics.append(screen, { type = "item", label = conf[id .. ".label"] or cmd or "(button)", style = conf[id .. ".style"] or "default", root = function () if cmd then runCommand(cmd) else warn(id) end end })
                elseif ftype == "field" then
                    graphics.append(screen, { type = "field", label = conf[id .. ".label"] or "", value = conf[id .. ".value"] or "", mode = conf[id .. ".mode"] or "any" })
                elseif ftype == "choice" then
                    local opts = {}
                    if conf[id .. ".options"] then
                        local parts = string.split(conf[id .. ".options"], ",")
                        for j = 1, #parts do opts[j] = string.trim(parts[j]) end
                    end
                    graphics.append(screen, { type = "choice", label = conf[id .. ".label"] or "", mode = conf[id .. ".mode"] or "exclusive", options = opts, root = function () end })
                elseif ftype == "gauge" then
                    graphics.append(screen, { type = "gauge", label = conf[id .. ".label"] or "", interactive = true, maxValue = num(conf[id .. ".max"], 100), value = num(conf[id .. ".value"], 0) })
                elseif ftype == "spacer" then
                    graphics.append(screen, { type = "spacer", width = num(conf[id .. ".w"], 1), height = num(conf[id .. ".h"], 10) })
                end
            end
        end
    end

    local back = graphics.new("command", { label = conf["screen.back.label"] or "Back", type = "back", priority = 1 })
    graphics.addCommand(screen, back)
    local handlers = {
        [back] = function ()
            if conf["screen.back"] then runCommand(conf["screen.back"]) end
            xterm()
            os.exit(0)
        end
    }

    if conf["screen.button"] then
        local menu = graphics.new("command", { label = conf["screen.button"], type = "ok", priority = 2 })
        graphics.addCommand(screen, menu)
        handlers[menu] = function ()
            if conf["screen.button.cmd"] then runCommand(conf["screen.button.cmd"]) end
            xterm()
            os.exit(0)
        end
    end

    graphics.handler(screen, handlers)
    graphics.display(screen)
end

local function buildList(conf)
    if not conf then return end
    local list = graphics.new("list", conf["list.title"] or "OpenTTY")
    local icon
    if conf["list.icon"] then
        local ok, img = pcall(graphics.render, os.join(conf["list.icon"]))
        if ok and img then icon = img end
    end

    if conf["list.content"] then
        local items = string.split(conf["list.content"], ",")
        for i = 1, #items do
            local v = string.trim(items[i])
            if v ~= "" then
                if icon then graphics.append(list, v, icon) else graphics.append(list, v) end
            end
        end
    end

    if conf["list.source"] then
        local src = io.read(os.join(conf["list.source"]))
        if src and string.trim(src) ~= "" then
            local lines = string.split(string.env(src), "\n")
            for i = 1, #lines do
                local line = string.trim(lines[i])
                if line ~= "" then
                    local eq = string.find(line, "=")
                    local name, cmd
                    if eq then
                        name = string.trim(string.sub(line, 1, eq - 1))
                        cmd = string.trim(string.sub(line, eq + 1))
                    else
                        name = line
                        cmd = "true"
                    end
                    if icon then graphics.append(list, name, icon) else graphics.append(list, name) end
                    conf[name] = cmd
                end
            end
        end
    end

    local back = graphics.new("command", { label = conf["list.back.label"] or "Back", type = "back", priority = 1 })
    local go = graphics.new("command", { label = conf["list.button"] or "Select", type = "ok", priority = 2 })

    local function run(selected)
        if selected then
            local cmd = conf[selected]
            if cmd then
                xterm()
                runCommand(cmd)
            else
                warn(selected)
            end
        end
        os.exit(0)
    end

    graphics.addCommand(list, back)
    graphics.addCommand(list, go)
    graphics.handler(list, {
        [back] = function ()
            if conf["list.back"] then runCommand(conf["list.back"]) end
            xterm()
            os.exit(0)
        end,
        [go] = run, [graphics.fire] = run
    })
    graphics.display(list)
end

local function buildQuest(conf)
    if not conf then return end
    if not (conf["quest.label"] and conf["quest.key"] and conf["quest.cmd"]) then
        print("x11: quest: malformed quest.conf settings")
        return
    end
    local form = graphics.new("screen", conf["quest.title"] or "OpenTTY")
    graphics.append(form, { type = "field", label = conf["quest.label"], value = conf["quest.content"] or "", mode = conf["quest.type"] or "any" })
    local back = graphics.new("command", { label = conf["quest.back.label"] or "Cancel", type = "back", priority = 2 })
    local send = graphics.new("command", { label = conf["quest.cmd.label"] or "Send", type = "ok", priority = 1 })

    local function done(value)
        if value and string.trim(value) ~= "" then
            pcall(os.setenv, conf["quest.key"], string.env(value))
            xterm()
            runCommand(conf["quest.cmd"])
        end
        os.exit(0)
    end

    graphics.addCommand(form, back)
    graphics.addCommand(form, send)
    graphics.handler(form, {
        [back] = function ()
            if conf["quest.back"] then runCommand(conf["quest.back"]) end
            xterm()
            os.exit(0)
        end,
        [send] = done
    })
    graphics.display(form)
end

local function buildEdit(conf)
    if not conf then return end
    if not (conf["edit.key"] and conf["edit.cmd"]) then
        print("x11: edit: malformed edit.conf settings")
        return
    end
    local content = conf["edit.content"] or ""
    if conf["edit.source"] then
        local src = io.read(os.join(conf["edit.source"]))
        if src then content = src end
    end
    local box = graphics.new("edit", conf["edit.title"] or "OpenTTY", content)
    local back = graphics.new("command", { label = conf["edit.back.label"] or "Back", type = "back", priority = 1 })
    local go = graphics.new("command", { label = conf["edit.cmd.label"] or "Run", type = "ok", priority = 2 })

    local function save(text)
        if text and string.trim(text) ~= "" then
            pcall(os.setenv, conf["edit.key"], string.env(text))
            xterm()
            runCommand(conf["edit.cmd"])
        end
        os.exit(0)
    end

    graphics.addCommand(box, back)
    graphics.addCommand(box, go)
    graphics.handler(box, {
        [back] = function ()
            if conf["edit.back"] then runCommand(conf["edit.back"]) end
            xterm()
            os.exit(0)
        end,
        [go] = save
    })
    graphics.display(box)
end

local function buildItem(conf)
    if not conf then return end
    if not (conf["item.label"] and conf["item.cmd"]) then
        print("x11: item: malformed item.conf settings")
        return
    end
    local prev = graphics.db["xterm"]
    if prev then
        local cmd = conf["item.cmd"]
        graphics.append(prev, { type = "item", label = conf["item.label"], style = conf["item.style"] or "default", root = function () if cmd then runCommand(cmd) end end })
    end
end

local function gauge(text)
    local screen = graphics.new("screen", "Gauge")
    graphics.append(screen, { type = "gauge", label = text or "", interactive = false, maxValue = 100, value = 0 })
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    graphics.addCommand(screen, back)
    graphics.handler(screen, { [back] = function () xterm() os.exit(0) end })
    graphics.display(screen)
end

local function banner()
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "OpenTTY X.Org")
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    graphics.append(screen, "OpenTTY X.Org - X Server " .. version .. "\nRelease Date: " .. release_date .. "\nX Protocol Version 1, Revision 3\nBuild OS: " .. getAppProperty("/microedition.profiles"))
    graphics.addCommand(screen, back)
    graphics.handler(screen, { [back] = function () local prev = graphics.db["xterm"] if prev then graphics.display(prev) else graphics.display(previous) end os.exit(0) end })
    graphics.display(screen)
end

local what = arg[1]
if what == "make" or what == "screen" then
    buildScreen(source(arg[2], arg[3]))
elseif what == "list" then
    buildList(source(arg[2], arg[3]))
elseif what == "quest" then
    buildQuest(source(arg[2], arg[3]))
elseif what == "edit" then
    buildEdit(source(arg[2], arg[3]))
elseif what == "item" then
    buildItem(source(arg[2], arg[3]))
elseif what == "canvas" then
    print("x11: canvas: not available in this build")
elseif what == "version" then
    print("X Server " .. version)
elseif what == "title" then
    graphics.SetTitle(graphics.getCurrent(), arg[2])
elseif what == "tick" then
    graphics.SetTicker(graphics.getCurrent(), arg[2])
elseif what == "term" then
    xterm()
elseif what == "stop" then
    graphics.SetTitle(graphics.getCurrent(), "Terminal")
    xterm()
elseif what == "gauge" then
    gauge(arg[2])
elseif what == nil then
    banner()
else
    print(arg[0] .. ": " .. what .. ": not found")
end