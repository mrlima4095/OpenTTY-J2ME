#!/bin/lua

os.setproc("name", "cut")

local function usage()
    print("Usage: cut [options] [file]")
    print("  -f LIST     select fields (1,3-5)")
    print("  -c LIST     select characters (1-5,8)")
    print("  -d DELIM    set field delimiter (default: TAB)")
    print("  -s          suppress lines without delimiter")
    print("  -h          show this help")
end

local mode = nil
local delim = "\t"
local range_spec = nil
local suppress = false
local file = nil
local i = 1

while arg[i] do
    local a = tostring(arg[i])
    if a == "-h" or a == "--help" then
        usage()
        os.exit(0)
    elseif a == "-f" then
        i = i + 1
        mode = "fields"
        range_spec = tostring(arg[i] or "")
    elseif a == "-c" then
        i = i + 1
        mode = "chars"
        range_spec = tostring(arg[i] or "")
    elseif a == "-d" then
        i = i + 1
        delim = tostring(arg[i] or "\t")
    elseif a == "-s" then
        suppress = true
    elseif not string.startswith(a, "-") then
        file = a
    else
        print("cut: invalid option -- " .. a)
        os.exit(2)
    end
    i = i + 1
end

if not mode or not range_spec or range_spec == "" then
    print("cut: you must specify -f or -c with a list")
    usage()
    os.exit(2)
end

local function parse_ranges(spec)
    local ranges = {}
    for _, part in ipairs(string.split(spec, ",")) do
        local trimmed = string.trim(part)
        if trimmed ~= "" then
            local dash = string.find(trimmed, "-")
            if dash then
                local left = string.trim(string.sub(trimmed, 1, dash - 1))
                local right = string.trim(string.sub(trimmed, dash + 1))
                local s = 1
                local e = nil
                if left ~= "" then s = tonumber(left) end
                if right ~= "" then e = tonumber(right) end
                table.insert(ranges, {from = s, to = e})
            else
                local n = tonumber(trimmed)
                if n then
                    table.insert(ranges, {from = n, to = n})
                end
            end
        end
    end
    return ranges
end

local function in_range(ranges, idx)
    for _, r in ipairs(ranges) do
        local e = r.to
        if not e then
            if idx >= r.from then return true end
        elseif idx >= r.from and idx <= e then
            return true
        end
    end
    return false
end

local ranges = parse_ranges(range_spec)

local content
if file then
    content = io.read(os.join(file))
    if not content then
        print("cut: " .. file .. ": not found")
        os.exit(127)
    end
else
    local buf = io.stdin
    if not buf then
        print("cut: no input")
        os.exit(2)
    end
    content = io.read(buf)
    if not content or content == "" then
        os.exit(0)
    end
end

if content == "" then os.exit(0) end

local lines = string.split(content, "\n")

if mode == "fields" then
    for _, line in ipairs(lines) do
        local has_delim = string.find(line, delim) ~= nil
        if suppress and not has_delim then
        else
            local parts = string.split(line, delim)
            local result = {}
            local first = true
            for j = 1, #parts do
                if in_range(ranges, j) then
                    if not first then
                        table.insert(result, delim)
                    end
                    table.insert(result, parts[j])
                    first = false
                end
            end
            print(table.concat(result, ""))
        end
    end
elseif mode == "chars" then
    for _, line in ipairs(lines) do
        local result = {}
        local len = string.len(line)
        for j = 1, len do
            if in_range(ranges, j) then
                table.insert(result, string.sub(line, j, j))
            end
        end
        print(table.concat(result, ""))
    end
end
