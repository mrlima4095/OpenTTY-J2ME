#!/bin/lua

local cmd, file = arg[1], arg[2]
if not cmd or cmd == "-h" or cmd == "--help" then 
    print("Usage: sed [PATTERN] [file]")
    print("Pattern format: s/search/replace/")
    print("Example: sed 's/old/new/' file.txt")
    return 
end

if not file then file = "nano" end

local content = io.read(file)
if content == "" then print("sed: empty content") return end

if not string.find(cmd, "/") then
    print("sed: invalid pattern format")
    print("Use: s/search/replace/")
    return
end

local first_slash = string.find(cmd, "/", 1)
if not first_slash then print("sed: invalid pattern - missing /") return
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

if current_line ~= "" then
    table.insert(lines, current_line)
end

local result_lines = {}
for idx, line in ipairs(lines) do
    local result_line = ""
    local line_pos = 1
    local search_len = string.len(search)
    
    while line_pos <= string.len(line) do
        -- Check if search pattern matches at current position
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
            -- Found match, replace with replacement text
            result_line = result_line .. replace
            line_pos = line_pos + search_len
        else
            -- No match, keep original character
            result_line = result_line .. string.sub(line, line_pos, line_pos)
            line_pos = line_pos + 1
        end
    end
    
    table.insert(result_lines, result_line)
end

local result = table.concat(result_lines, "\n")

if not io.write(result, file) then
    print("sed: error writing to file")
    return
end

print("sed: substitution completed successfully")