#!/bin/lua

local ROOT_TITLE = "Explorer"
local ROOTS = { "bin", "dev", "etc", "home", "lib", "mnt", "proc", "tmp" }

local parent = nil
local cwd = "/"
local viewing = nil

local explorer = graphics.new("list", ROOT_TITLE)
local open = graphics.new("command", { label = "Open", type = "ok", priority = 1 })
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local quit = graphics.new("command", { label = "Quit", type = "exit", priority = 1 })
local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

local function join(base, name)
    if base == "" or base == "/" then return "/" .. name end
    if string.sub(base, -1) == "/" then return base .. name end
    return base .. "/" .. name
end

local function countEntries(t)
    if type(t) ~= "table" then return 0 end
    local n = 0
    for _ in pairs(t) do n = n + 1 end
    return n
end

local function showFile(path)
    local content = io.read(path)
    if content == nil or content == "" then
        content = "(empty file)"
    end
    viewing = path

    local box = graphics.new("edit", path, content)
    local close = graphics.new("command", { label = "Close", type = "back", priority = 1 })
    graphics.addCommand(box, close)
    graphics.handler(box, {
        [close] = function() populate(cwd) end,
        [graphics.fire] = function() populate(cwd) end,
    })
    graphics.display(box)
end

local function populate(dir)
    cwd = dir or "/"
    viewing = nil
    graphics.clear(explorer)

    if cwd == "" or cwd == "/" then
        graphics.SetTitle(explorer, ROOT_TITLE)
        for i = 1, #ROOTS do
            graphics.append(explorer, ROOTS[i])
        end
        return
    end

    graphics.SetTitle(explorer, "Explorer: " .. cwd)
    graphics.append(explorer, "..")

    local entries = io.dirs(cwd)
    local names = {}
    if type(entries) == "table" then
        for _, name in pairs(entries) do
            table.insert(names, tostring(name))
        end
        table.sort(names)
        for i = 1, #names do
            graphics.append(explorer, names[i])
        end
    end
end

local function navigate(item)
    if item == nil or item == "" then return end

    if item == ".." then
        if parent then populate(parent) end
        return
    end

    local target = join(cwd, item)
    local isDir = false

    if cwd == "" or cwd == "/" then
        isDir = true
    elseif cwd == "/mnt/" or string.startswith(cwd, "/mnt/") then
        isDir = true
    else
        local sub = io.dirs(target)
        if countEntries(sub) > 0 then isDir = true end
    end

    if isDir then
        parent = cwd
        populate(target)
    else
        showFile(target)
    end
end

local function goUp()
    if viewing then
        populate(cwd)
    elseif parent then
        populate(parent)
    elseif cwd ~= "/" then
        populate("/")
    end
end

local start = arg[1]
if start and start ~= "" and string.sub(start, -1) == "/" then
    populate(start)
else
    populate("/")
end

graphics.addCommand(explorer, open)
graphics.addCommand(explorer, back)
graphics.addCommand(explorer, quit)
graphics.addCommand(explorer, switch)

graphics.handler(explorer, {
    [open] = navigate,
    [graphics.fire] = navigate,
    [back] = goUp,
    [quit] = os.exit,
    [switch] = graphics.taskmngr,
})
os.setproc("screen", explorer)
graphics.display(explorer)
