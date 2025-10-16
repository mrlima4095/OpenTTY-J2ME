-- Implementação do comando sed em Lua J2ME
-- Uso: sed [opções] COMANDO [arquivo]

local args = arg or {}
local command = ""
local filename = ""
local in_place = false
local backup_ext = ""
local quiet = false

-- Parse de argumentos
local i = 1
while i <= #args do
    local arg = args[i]
    
    if arg == "-i" then
        in_place = true
        if i + 1 <= #args and not string.sub(args[i + 1], 1, 1) == "-" then
            backup_ext = args[i + 1]
            i = i + 1
        end
    elseif arg == "-n" or arg == "--quiet" or arg == "--silent" then
        quiet = true
    elseif arg == "-e" then
        if i + 1 <= #args then
            command = args[i + 1]
            i = i + 1
        end
    elseif string.sub(arg, 1, 1) ~= "-" then
        if command == "" then
            command = arg
        elseif filename == "" then
            filename = arg
        end
    end
    i = i + 1
end

-- Se não tem comando específico, pega o primeiro argumento não-opção
if command == "" then
    for i = 1, #args do
        local arg = args[i]
        if string.sub(arg, 1, 1) ~= "-" and arg ~= filename then
            command = arg
            break
        end
    end
end

if command == "" then
    if not quiet then
        print("sed: nenhum comando especificado")
        print("Uso: sed [OPÇÕES] COMANDO [ARQUIVO]")
        print("Opções:")
        print("  -i[SUFIXO]  edição in-place (opcionalmente cria backup)")
        print("  -n, --quiet suprimir saída automática")
        print("  -e COMANDO  adicionar comando")
    end
    os.exit(1)
end

-- Funções auxiliares
function string.split(str, delimiter)
    local result = {}
    local from = 1
    local delim_from, delim_to = string.find(str, delimiter, from)
    
    while delim_from do
        table.insert(result, string.sub(str, from, delim_from - 1))
        from = delim_to + 1
        delim_from, delim_to = string.find(str, delimiter, from)
    end
    
    table.insert(result, string.sub(str, from))
    return result
end

function string.trim(str)
    return string.gsub(str, "^%s*(.-)%s*$", "%1")
end

function escape_pattern(text)
    local special_chars = {"%.", "%^", "%$", "%(", "%)", "%[", "%]", "%*", "%+", "%-", "%?"}
    for _, char in ipairs(special_chars) do
        text = string.gsub(text, char, "%%" .. string.sub(char, 2))
    end
    return text
end

-- Processa o comando sed
function process_sed_command(cmd, line, line_num)
    -- Comando de substituição: s/pattern/replacement/flags
    if string.sub(cmd, 1, 2) == "s/" then
        local parts = string.split(cmd, "/")
        if #parts >= 3 then
            local pattern = parts[2]
            local replacement = parts[3]
            local flags = parts[4] or ""
            
            -- Escapa o padrão para busca literal (sem regex)
            local escaped_pattern = escape_pattern(pattern)
            
            -- Processa flags
            local global_replace = string.find(flags, "g") ~= nil
            local case_insensitive = string.find(flags, "i") ~= nil
            
            if case_insensitive then
                -- Para case insensitive, converte ambos para minúsculo
                local lower_line = string.lower(line)
                local lower_pattern = string.lower(pattern)
                escaped_pattern = escape_pattern(lower_pattern)
                
                local result = line
                local lower_result = string.lower(result)
                
                if global_replace then
                    local start_pos = 1
                    while true do
                        local i, j = string.find(lower_result, escaped_pattern, start_pos)
                        if not i then break end
                        
                        -- Substitui mantendo o case original
                        local before = string.sub(result, 1, i - 1)
                        local after = string.sub(result, j + 1)
                        local matched = string.sub(result, i, j)
                        
                        result = before .. replacement .. after
                        lower_result = string.lower(result)
                        start_pos = i + string.len(replacement)
                    end
                    return result
                else
                    local i, j = string.find(lower_result, escaped_pattern, 1)
                    if i then
                        local before = string.sub(result, 1, i - 1)
                        local after = string.sub(result, j + 1)
                        return before .. replacement .. after
                    end
                end
            else
                -- Case sensitive
                if global_replace then
                    return string.gsub(line, escaped_pattern, replacement)
                else
                    local i, j = string.find(line, escaped_pattern, 1)
                    if i then
                        local before = string.sub(line, 1, i - 1)
                        local after = string.sub(line, j + 1)
                        return before .. replacement .. after
                    end
                end
            end
        end
    
    -- Comando de delete: /pattern/d
    elseif string.sub(cmd, -2) == "/d" then
        local pattern = string.sub(cmd, 1, -3)
        local escaped_pattern = escape_pattern(pattern)
        if string.find(line, escaped_pattern) then
            return nil  -- Marca para deletar a linha
        end
    
    -- Comando de print: /pattern/p
    elseif string.sub(cmd, -2) == "/p" then
        local pattern = string.sub(cmd, 1, -3)
        local escaped_pattern = escape_pattern(pattern)
        if string.find(line, escaped_pattern) then
            if not quiet then print(line) end
        end
    
    -- Comando de número de linha: =
    elseif cmd == "=" then
        if not quiet then print(line_num) end
        return line
    
    -- Comando para adicionar texto após linha: a\texto
    elseif string.sub(cmd, 1, 2) == "a\\" then
        local text = string.sub(cmd, 3)
        if not quiet then print(line) end
        if not quiet then print(text) end
        return line
    
    -- Comando para inserir texto antes da linha: i\texto
    elseif string.sub(cmd, 1, 2) == "i\\" then
        local text = string.sub(cmd, 3)
        if not quiet then print(text) end
        if not quiet then print(line) end
        return line
    end
    
    return line
end

-- Lê entrada
local lines = {}
if filename ~= "" then
    local content = io.read(filename)
    if content then
        lines = string.split(content, "\n")
    else
        if not quiet then
            print("sed: não foi possível ler o arquivo: " .. filename)
        end
        os.exit(1)
    end
else
    -- Lê da entrada padrão
    local input = io.read("stdout")
    if input and input ~= "" then
        lines = string.split(input, "\n")
    end
end

-- Processa as linhas
local output_lines = {}
for i, line in ipairs(lines) do
    local processed_line = process_sed_command(command, line, i)
    if processed_line ~= nil then
        if not quiet and not string.find(command, "/p") and not string.find(command, "a\\") and not string.find(command, "i\\") then
            print(processed_line)
        end
        table.insert(output_lines, processed_line)
    end
end

-- Se é edição in-place, escreve de volta no arquivo
if in_place and filename ~= "" then
    if backup_ext ~= "" then
        -- Cria backup
        local backup_name = filename .. backup_ext
        local original_content = io.read(filename)
        if original_content then
            io.write(backup_name, original_content)
        end
    end
    
    -- Escreve o novo conteúdo
    local new_content = table.concat(output_lines, "\n")
    io.write(filename, new_content)
end