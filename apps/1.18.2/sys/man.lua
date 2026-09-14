#!/bin/lua

-- man - Show documentation for OpenTTY apps, from the /etc/sources mirror.
--   man             list all apps
--   man <app>       show details for one app

local version = "1.0.0"

os.setproc("name", "man")

local function is_table(value)
    return type(value) == "table"
end

local function text(value)
    if value == nil then return "" end
    return tostring(value)
end

local function pad(name, width)
    local w = width - #name
    if w < 0 then w = 0 end
    return name .. string.sub("                          ", 1, w)
end

local function load_catalog()
    local content = io.read("/etc/sources")
    if content == nil or content == "" then return nil end

    local ok, data = pcall(load, content)
    if not ok then return nil end
    if not is_table(data) then return nil end
    if not is_table(data.mirror) then return nil end

    return data
end

local catalog = load_catalog()

if not is_table(catalog) then
    print("man: /etc/sources: not found or invalid")
    print("man: run 'pkg update' first")
    os.exit(1)
end

local names = {}
for name in pairs(catalog.mirror) do
    table.insert(names, name)
end
table.sort(names)

if arg[1] == nil or arg[1] == "-l" or arg[1] == "--list" then
    print("OpenTTY Manual (source " .. text(catalog.version) .. ")")
    print("")
    for i = 1, #names do
        local entry = catalog.mirror[names[i]]
        local desc = ""
        if is_table(entry) then desc = text(entry.description) end
        print(pad(names[i], 18) .. desc)
    end
    print("")
    print(#names .. " apps")
    os.exit(0)
end

if arg[1] == "-h" or arg[1] == "--help" then
    print("OpenTTY Manual v" .. version)
    print("")
    print("Usage: man [app]")
    print("  man         list all apps")
    print("  man <app>   show app details")
    os.exit(0)
end

local query = arg[1]
local entry = catalog.mirror[query]

if not is_table(entry) then
    print("man: " .. query .. ": no such app")
    os.exit(127)
end

print(query .. " - " .. text(entry.description))
print("")
print("Command : " .. text(entry.here))
print("Source  : " .. text(entry.remote))

if is_table(entry.depends) then
    print("Depends : " .. table.concat(entry.depends, ", "))
end

if entry.riscv == true then
    print("Type    : RISC-V ELF (C)")
else
    print("Type    : Lua script")
end