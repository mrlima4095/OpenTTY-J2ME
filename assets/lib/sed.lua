#!/bin/lua
-- Implementação do comando sed para Lua J2ME
-- Uso: sed [OPÇÕES] COMANDO [ARQUIVO]

local args = arg or {}
local command = ""
local filename = ""
local options = ""
local in_place = false
local global_replace = false

-- Parse arguments
local i = 1
while i <= #args do
    local arg = args[i]
    
    if arg:sub(1, 1) == "-" then
        options = arg
        if arg == "-i" then
            in_place = true
        end
    elseif command == "" then
        command = arg
    else
        filename = arg
    end
    i = i + 1
end

-- Verificar se temos um comando válido
if command == "" then print("sed: nenhum comando especificado") os.exit(1) end
if filename == "" then print("sed: nenhum arquivo especificado") os.exit(1) end

-- Função para parse do comando sed
function parse_sed_command(cmd)
    if cmd:sub(1, 1) == "s" then
        local delimiter = cmd:sub(2, 2)
        local parts = {}
        local start_pos = 3
        local part_start = start_pos
        
        -- Parse das partes separadas pelo delimitador
        for i = start_pos, #cmd do
            if cmd:sub(i, i) == delimiter then
                table.insert(parts, cmd:sub(part_start, i-1))
                part_start = i + 1
            end
        end
        
        -- Adicionar a última parte (flags)
        if part_start <= #cmd then
            table.insert(parts, cmd:sub(part_start))
        end
        
        if #parts >= 2 then
            local pattern = parts[1]
            local replacement = parts[2]
            local flags = parts[3] or ""
            
            global_replace = flags:find("g") ~= nil
            
            return {
                type = "substitute",
                pattern = pattern,
                replacement = replacement,
                flags = flags
            }
        end
    end
    
    return nil
end

-- Função de substituição simples (sem regex)
function simple_substitute(text, pattern, replacement, flags)
    local result = text
    local count = 0
    
    if flags:find("g") then
        -- Substituição global
        local start_pos = 1
        while true do
            local pos = string.find(result, pattern, start_pos, true) -- true para plain text
            if not pos then break end
            
            local before = result:sub(1, pos-1)
            local after = result:sub(pos + #pattern)
            result = before .. replacement .. after
            start_pos = pos + #replacement
            count = count + 1
        end
    else
        -- Apenas primeira ocorrência
        local pos = string.find(result, pattern, 1, true)
        if pos then
            local before = result:sub(1, pos-1)
            local after = result:sub(pos + #pattern)
            result = before .. replacement .. after
            count = 1
        end
    end
    
    return result, count
end

-- Processar o comando
local sed_cmd = parse_sed_command(command)
if not sed_cmd then print("sed: comando inválido: " .. command) os.exit(1) end

-- Ler o arquivo
local content = io.read(filename)
if not content then
    print("sed: não foi possível ler o arquivo: " .. filename)
    os.exit(1)
end

-- Processar o conteúdo
local lines = {}
for line in content:gmatch("[^\r\n]+") do
    table.insert(lines, line)
end

local modified_lines = {}
local total_replacements = 0

for _, line in ipairs(lines) do
    if sed_cmd.type == "substitute" then
        local new_line, count = simple_substitute(line, sed_cmd.pattern, sed_cmd.replacement, sed_cmd.flags)
        table.insert(modified_lines, new_line)
        total_replacements = total_replacements + count
    else
        table.insert(modified_lines, line)
    end
end

-- Output ou salvar
if in_place then
    -- Salvar no arquivo original (append=false para sobrescrever)
    local new_content = table.concat(modified_lines, "\n")
    local success = io.write(filename, new_content) -- false para não fazer append
    if not success then print("sed: erro ao salvar arquivo: " .. filename) os.exit(1) end
    print("Arquivo " .. filename .. " modificado com " .. total_replacements .. " substituições")
else
    -- Output para stdout usando print
    for _, line in ipairs(modified_lines) do
        print(line)
    end
end

-- Status de saída
if total_replacements > 0 then
    os.exit(0)
else
    os.exit(1)  -- Nenhuma substituição foi feita
end