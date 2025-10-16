#!/bin/lua

local function splitString(str, delimiter)
    local result = {}
    local i = 1
    local start = 1
    local delimLen = string.len(delimiter)

    while true do
        local pos = string.find(str, delimiter, start)
        if not pos then
            result[i] = string.sub(str, start)
            break
        end
        result[i] = string.sub(str, start, pos - 1)
        start = pos + delimLen
        i = i + 1
    end

    return result
end

local function findSubstring(str, pattern)
    local len = string.len(str)
    local patternLen = string.len(pattern)

    for i = 1, len - patternLen + 1 do
        local match = true
        for j = 1, patternLen do
            if string.sub(str, i + j - 1, i + j - 1) ~= string.sub(pattern, j, j) then
                match = false
                break
            end
        end
        if match then
            return i
        end
    end
    return nil
end

local function substitute(line, pattern, replacement, global)
    local result = line
    local start = 1

    while true do
        local found = findSubstring(result, pattern)
        if not found then break end

        local before = string.sub(result, 1, found - 1)
        local after = string.sub(result, found + string.len(pattern))
        result = before .. replacement .. after

        if not global then break end
        start = found + string.len(replacement)

        if start > string.len(result) then break end
    end
    
    return result
end

local function splitLines(content)
    local lines = {}
    local i = 1
    local start = 1
    local len = string.len(content)
    
    for pos = 1, len do
        local char = string.sub(content, pos, pos)
        if char == "\n" then
            lines[i] = string.sub(content, start, pos - 1)
            i = i + 1
            start = pos + 1
        end
    end
    
    -- Adicionar última linha
    if start <= len then
        lines[i] = string.sub(content, start)
    end
    
    return lines
end


local function processSedCommand(command, content)
    local lines = splitLines(content)

    if string.sub(command, 1, 2) == "s/" then
        local parts = splitString(command, "/")

        if #parts >= 3 then
            local pattern = parts[2] or ""
            local replacement = parts[3] or ""
            local flags = parts[4] or ""
            local global = findSubstring(flags, "g") ~= nil

            for idx = 1, #lines do
                lines[idx] = substitute(lines[idx], pattern, replacement, global)
            end
        end

    elseif string.sub(command, string.len(command)) == "d" then
        local pattern = string.sub(command, 1, string.len(command) - 1)
        if string.sub(pattern, 1, 1) == "/" then
            pattern = string.sub(pattern, 2)
        end

        local newLines = {}
        local newIndex = 1
        for i = 1, #lines do
            if findSubstring(lines[i], pattern) == nil then
                newLines[newIndex] = lines[i]
                newIndex = newIndex + 1
            end
        end
        lines = newLines
    end

    local result = ""
    for i = 1, #lines do
        result = result .. lines[i]
        if i < #lines then
            result = result .. "\n"
        end
    end

    return result
end

local args = arg or {}
local cmd = ""
local file = ""

if #args >= 1 then cmd = args[1] end
if #args >= 2 then file = args[2] end

if cmd == "" or cmd == "-h" or cmd == "--help" then print("Usage: sed [PATTERN] [file]") return end

local content = ""
if file ~= "" then
    local success, fileContent = pcall(function() return io.read(file) end)
    if success and fileContent then content = fileContent else print("Erro: Não foi possível ler o arquivo '" .. file .. "'") return end
else
    file = "nano"
    content = io.read("nano")
end

if content ~= "" then
    local result = processSedCommand(cmd, content)
    io.write(result, file)
else
    print("Empty content")
end