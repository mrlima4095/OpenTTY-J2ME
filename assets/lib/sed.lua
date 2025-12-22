#!/bin/lua

local function joinpath(pwd)
    if string.sub(pwd, 1, 1) ~= "/" then
        return os.getcwd() .. pwd
    end
    return pwd
end

local cmd, file, in_place = nil, nil, false
local args = {}

-- Parse arguments
for i = 1, #arg do
    local current = arg[i]
    if current == "-i" then
        in_place = true
    elseif current == "-h" or current == "--help" then
        cmd = "-h"
        break
    elseif not cmd then
        cmd = current
    elseif not file then
        file = current
    else
        table.insert(args, current)
    end
end

if cmd == "-h" or cmd == "--help" or not cmd then
    print("Usage: sed [OPTIONS] [PATTERN] [file]")
    print("Options:")
    print("  -i      Edit files in-place")
    print("  -h      Show this help")
    print("Pattern formats:")
    print("  s/search/replace/  - Substitute")
    print("  /pattern/d         - Delete lines matching pattern")
    print("Examples:")
    print("  sed 's/old/new/' file.txt")
    print("  sed -i 's/old/new/' file.txt")
    print("  sed '/hello/d' file.txt")
    return
end

if not file then
    print("sed: missing file operand")
    print("Usage: sed [OPTIONS] [PATTERN] [file]")
    return
end

local full_path = joinpath(file)
local content = io.read(full_path)
if content == "" then 
    print("sed: empty content")
    return 
end

local is_delete = string.sub(cmd, -2) == "/d"
local result_lines = {}

if is_delete then
    local pattern = string.sub(cmd, 1, -3) -- Remove "/d" from the end

    if pattern == "" then
        print("sed: delete pattern cannot be empty")
        return
    end

    local lines = {}
    local current_line = ""
    local i = 1

    -- Split content into lines
    while i <= string.len(content) do
        local char = string.sub(content, i, i)
        if char == "\n" then 
            table.insert(lines, current_line) 
            current_line = ""
        else 
            current_line = current_line .. char 
        end
        i = i + 1
    end
    if current_line ~= "" then table.insert(lines, current_line) end

    for idx, line in ipairs(lines) do
        local contains_pattern = false
        local line_pos = 1
        local pattern_len = string.len(pattern)

        while line_pos <= string.len(line) - pattern_len + 1 do
            local match = true
            for i = 1, pattern_len do
                local content_char = string.sub(line, line_pos + i - 1, line_pos + i - 1)
                local pattern_char = string.sub(pattern, i, i)
                if content_char ~= pattern_char then
                    match = false
                    break
                end
            end
            if match then
                contains_pattern = true
                break
            end
            line_pos = line_pos + 1
        end

        if not contains_pattern then
            table.insert(result_lines, line)
        end
    end

else
    -- SUBSTITUTE COMMAND: s/search/replace/
    if not string.find(cmd, "/") then
        print("sed: invalid pattern format")
        print("Use: s/search/replace/ or /pattern/d")
        return
    end

    local first_slash = string.find(cmd, "/", 1)
    if not first_slash then 
        print("sed: invalid pattern - missing /") 
        return
    end

    local second_slash = string.find(cmd, "/", first_slash + 1)
    if not second_slash then
        print("sed: invalid pattern - missing second /")
        return
    end

    local third_slash = string.find(cmd, "/", second_slash + 1)
    if not third_slash then
        print("sed: invalid pattern - missing third /")
        return
    end

    local search = string.sub(cmd, first_slash + 1, second_slash - 1)
    local replace = string.sub(cmd, second_slash + 1, third_slash - 1)

    if search == "" then
        print("sed: search pattern cannot be empty")
        return
    end

    local lines = {}
    local current_line = ""
    local i = 1

    while i <= string.len(content) do
        local char = string.sub(content, i, i)
        if char == "\n" then 
            table.insert(lines, current_line) 
            current_line = ""
        else 
            current_line = current_line .. char 
        end
        i = i + 1
    end

    if current_line ~= "" then table.insert(lines, current_line) end

    for idx, line in ipairs(lines) do
        local result_line = ""
        local line_pos = 1
        local search_len = string.len(search)

        while line_pos <= string.len(line) do
            local match = true
            for i = 1, search_len do
                local content_char = string.sub(line, line_pos + i - 1, line_pos + i - 1)
                local search_char = string.sub(search, i, i)
                if content_char ~= search_char then
                    match = false
                    break
                end
            end

            if match then
                result_line = result_line .. replace
                line_pos = line_pos + search_len
            else
                result_line = result_line .. string.sub(line, line_pos, line_pos)
                line_pos = line_pos + 1
            end
        end

        table.insert(result_lines, result_line)
    end
end

local result = table.concat(result_lines, "\n")

if in_place then
    -- Edit file in-place
    local status = tonumber(io.write(result, full_path))
    if status ~= 0 then
        print("sed: failed to write to file")
    end
    os.exit(status)
else
    -- Output to stdout (comportamento original)
    print(result)
    os.exit(0)
end