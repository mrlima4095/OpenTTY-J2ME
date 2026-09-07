#!/bin/lua

os.setproc("name", "uniq")

local function usage()
    print("Usage: uniq [options] [file]")
    print("  -c    prefix lines with count")
    print("  -d    only print duplicate lines")
    print("  -u    only print unique lines")
    print("  -h    show this help")
end

local count_prefix = false
local only_dup = false
local only_unique = false
local file = nil
local i = 1

while arg[i] do
    local a = tostring(arg[i])
    if a == "-h" or a == "--help" then
        usage()
        os.exit(0)
    elseif a == "-c" then
        count_prefix = true
    elseif a == "-d" then
        only_dup = true
    elseif a == "-u" then
        only_unique = true
    elseif not string.startswith(a, "-") then
        file = a
    else
        print("uniq: invalid option -- " .. string.sub(a, 2))
        os.exit(2)
    end
    i = i + 1
end

local content
if file then
    content = io.read(os.join(file))
    if not content then
        print("uniq: " .. file .. ": not found")
        os.exit(127)
    end
else
    local buf = io.stdin
    if not buf then
        print("uniq: no input")
        os.exit(2)
    end
    content = io.read(buf)
    if not content or content == "" then
        os.exit(0)
    end
end

if content == "" then os.exit(0) end

local lines = string.split(content, "\n")
local groups = {}
local current_line = nil
local current_count = 0

for k = 1, #lines do
    if lines[k] == current_line then
        current_count = current_count + 1
    else
        if current_line ~= nil then
            table.insert(groups, { line = current_line, count = current_count })
        end
        current_line = lines[k]
        current_count = 1
    end
end
if current_line ~= nil then
    table.insert(groups, { line = current_line, count = current_count })
end

for k = 1, #groups do
    local g = groups[k]
    if g.count > 1 then
        if not only_unique then
            if count_prefix then
                print("  " .. tostring(g.count) .. " " .. g.line)
            else
                print(g.line)
            end
        end
    else
        if not only_dup then
            if count_prefix then
                print("  1 " .. g.line)
            else
                print(g.line)
            end
        end
    end
end
