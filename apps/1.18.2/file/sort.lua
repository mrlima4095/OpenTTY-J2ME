#!/bin/lua

os.setproc("name", "sort")

local function usage()
    print("Usage: sort [options] [file]")
    print("  -r    reverse order")
    print("  -n    numeric sort")
    print("  -u    unique lines only")
    print("  -h    show this help")
end

local reverse = false
local numeric = false
local unique = false
local files = {}
local i = 1

while arg[i] do
    local a = tostring(arg[i])
    if a == "-h" or a == "--help" then
        usage()
        os.exit(0)
    elseif a == "-r" then
        reverse = true
    elseif a == "-n" then
        numeric = true
    elseif a == "-u" then
        unique = true
    elseif string.startswith(a, "-") and string.len(a) > 1 and not string.startswith(a, "--") then
        local opts = string.sub(a, 2)
        local j = 1
        while j <= string.len(opts) do
            local ch = string.sub(opts, j, j)
            if ch == "r" then reverse = true
            elseif ch == "n" then numeric = true
            elseif ch == "u" then unique = true
            else
                print("sort: invalid option -- " .. ch)
                os.exit(2)
            end
            j = j + 1
        end
    else
        table.insert(files, a)
    end
    i = i + 1
end

local function read_input()
    if #files > 0 then
        local content = io.read(os.join(files[1]))
        if not content then
            print("sort: " .. files[1] .. ": not found")
            os.exit(127)
        end
        return content
    end

    local buf = io.stdin
    if not buf then
        print("sort: no input")
        os.exit(2)
    end
    local content = io.read(buf)
    if not content or content == "" then
        return ""
    end
    return content
end

local content = read_input()
if content == "" then os.exit(0) end

local lines = string.split(content, "\n")

local function compare_less(a, b)
    local less = false
    if numeric then
        local na = tonumber(a) or 0
        local nb = tonumber(b) or 0
        less = na < nb
    else
        less = a < b
    end
    if reverse then
        return not less and a ~= b
    end
    return less
end

local function sort_lines(list)
    for k = 2, #list do
        local key = list[k]
        local j = k - 1
        while j >= 1 and compare_less(key, list[j]) do
            list[j + 1] = list[j]
            j = j - 1
        end
        list[j + 1] = key
    end
    return list
end

sort_lines(lines)

if unique then
    local dedup = {}
    local seen = {}
    for k = 1, #lines do
        if not seen[lines[k]] then
            seen[lines[k]] = true
            table.insert(dedup, lines[k])
        end
    end
    lines = dedup
end

for k = 1, #lines do
    print(lines[k])
end