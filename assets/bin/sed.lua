#!/bin/lua


local function split(str, sep)
    local result = {}
    local start = 1
    while true do
        local i = string.find(str, sep, start, true)
        if not i then
            table.insert(result, string.sub(str, start))
            break
        end
        table.insert(result, string.sub(str, start, i - 1))
        start = i + #sep
    end
    return result
end

local function replace(s, find, repl)
    local result, i = "", 1
    while i <= #s do
        local sub = string.sub(s, i, i + #find - 1)
        if sub == find then
            result = result .. repl
            i = i + #find
        else
            result = result .. string.sub(s, i, i)
            i = i + 1
        end
    end
    return result
end


local cmd = arg[1]
local filename = arg[2] or "nano"
local buffer = io.read(filename)

if not cmd then os.exit(0)
elseif cmd == "-2u" then buffer = string.upper(buffer)
elseif cmd == "-2l" then buffer = string.lower(buffer)
elseif cmd == "-d" then
    if arg[3] then buffer = replace(buffer, arg[3], "")
    else print(arg[0] .. ": missing args") os.exit(2) end
elseif cmd == "-a" then
    local text = arg[3] or ""
    if buffer == "" then buffer = text
    else buffer = buffer .. "\n" .. text end
elseif cmd == "-r" then
    local old = arg[3]
    local new = arg[4] or ""

    if old then buffer = replace(buffer, old, new)
    else print(arg[0] .. ": missing args") os.exit(2) end
elseif cmd == "-l" then
    local i = tonumber(arg[3])
    if not i then print(arg[0] .. ": invalid number") os.exit(2) end

    local lines = split(buffer, "\n")
    print(lines[i + 1] or "null")
elseif cmd == "-s" then
    local idx = tonumber(arg[3])
    local div = arg[4]
    if not (idx and div) then print(arg[0] .. ": missing args") os.exit(2) end

    local lines = {}
    local start = 1
    while true do
        local index = string.find(buffer, div, start, true)
        if not index then
            table.insert(lines, string.sub(buffer, start))
            break
        end
        table.insert(lines, string.sub(buffer, start, index - 1))
        start = index + #div
    end
    if idx >= 0 and idx < #lines then print(lines[idx + 1])
    else print("null") os.exit(1) end
elseif cmd == "-p" then
    local prefix = arg[3] or ""
    local lines = split(buffer, "\n")
    local result = {}
    for _, line in ipairs(lines) do
        table.insert(result, prefix .. line)
    end
    buffer = table.concat(result, "\n")
elseif cmd == "-v" then
    local lines = split(buffer, "\n")
    local rev = {}
    for i = #lines, 1, -1 do
        table.insert(rev, lines[i])
    end
    buffer = table.concat(rev, "\n")
else print(arg[0] .. ": " .. cmd .. ": not found") os.exit(127) end

io.write(buffer, filename)
