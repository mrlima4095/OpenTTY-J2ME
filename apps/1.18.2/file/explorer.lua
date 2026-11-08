#!/bin/lua

local ROOT_TITLE = "Explorer"
local ROOTS = { "bin", "dev", "etc", "home", "lib", "mnt", "proc", "tmp" }

local cwd = "/"
local viewing = nil

local explorer = graphics.new("list", ROOT_TITLE)
local open = graphics.new("command", { label = "Open", type = "ok", priority = 1 })
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
local quit = graphics.new("command", { label = "Quit", type = "exit", priority = 1 })
local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })

-- Declarada antes de showFile, que a usa nos handlers (antes virava nil global).
local populate

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

local function parentOf(path)
    if path == nil or path == "" or path == "/" then return nil end
    local p = path
    if string.sub(p, -1) == "/" then p = string.sub(p, 1, -2) end
    if p == "" then return nil end
    local i = #p
    while i > 0 do
        if string.sub(p, i, i) == "/" then
            local dir = string.sub(p, 1, i - 1)
            if dir == "" then return "/" end
            return dir
        end
        i = i - 1
    end
    return "/"
end

local function isDirectory(path)
    if path == "" or path == "/" then return true end
    if path == "/mnt" or string.startswith(path, "/mnt/") then return true end
    return countEntries(io.dirs(path)) > 0
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

populate = function(dir)
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
        local up = parentOf(cwd)
        if up then populate(up) end
        return
    end

    local target = join(cwd, item)

    -- Itens da raiz são pontos de montagem/virtuais: sempre diretórios.
    if cwd == "" or cwd == "/" or isDirectory(target) then
        populate(target)
    else
        showFile(target)
    end
end

local function goUp()
    if viewing then
        populate(cwd)
        return
    end
    local up = parentOf(cwd)
    if up then populate(up) end
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
