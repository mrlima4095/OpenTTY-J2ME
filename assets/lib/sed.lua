#!/bin/lua
-- sed.lua - Implementação simplificada do sed para Lua J2ME
-- Uso: lua sed.lua [comandos] [arquivo]

local args = arg or {}
local cmd = ""
local file = ""

-- Parse arguments
if #args >= 1 then
    cmd = args[1]
end
if #args >= 2 then
    file = args[2]
end

if cmd == "" or cmd == "-h" or cmd == "--help" then
    print("sed.lua - Substituição de texto estilo sed")
    print("Uso: lua sed.lua 's/old/new/g' arquivo.txt")
    print("Uso: lua sed.lua 's/old/new/' arquivo.txt") 
    print("Uso: lua sed.lua '/pattern/d' arquivo.txt")
    return
end

-- Função para dividir string por delimitador
function splitString(str, delimiter)
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

-- Função para encontrar substring
function findSubstring(str, pattern)
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

-- Função de substituição simples
function substitute(line, pattern, replacement, global)
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
        
        -- Prevenir loop infinito
        if start > string.len(result) then break end
    end
    
    return result
end

-- Função para dividir texto em linhas
function splitLines(content)
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

-- Processar comando sed
function processSedCommand(command, content)
    local lines = splitLines(content)
    
    -- Comando de substituição: s/pattern/replacement/flags
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
    
    -- Comando de deletar: /pattern/d
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
    
    -- Reconstruir conteúdo
    local result = ""
    for i = 1, #lines do
        result = result .. lines[i]
        if i < #lines then
            result = result .. "\n"
        end
    end
    
    return result
end

-- Ler arquivo ou stdin
local content = ""
if file ~= "" then
    local success, fileContent = pcall(function()
        return io.read(file)
    end)
    if success and fileContent then
        content = fileContent
    else
        print("Erro: Não foi possível ler o arquivo '" .. file .. "'")
        return
    end
else
    -- Ler da entrada padrão
    content = io.read() or ""
end

-- Processar conteúdo
if content ~= "" then
    local result = processSedCommand(cmd, content)
    io.write(result)
else
    print("Nenhum conteúdo para processar")
end