#!/bin/lua

-- Navegador Web Simples para Lua J2ME (sem regex)
-- Define a URL a ser carregada
local url = arg and arg[1] or "http://opentty.xyz/api/ip"

-- Função para encontrar substring (substitui string.find simples)
local function find_str(text, pattern, start_pos)
    start_pos = start_pos or 1
    local text_len = string.len(text)
    local pattern_len = string.len(pattern)

    for i = start_pos, text_len - pattern_len + 1 do
        local match = true
        for j = 1, pattern_len do
            if string.sub(text, i + j - 1, i + j - 1) ~= string.sub(pattern, j, j) then
                match = false
                break
            end
        end
        if match then
            return i, i + pattern_len - 1
        end
    end
    return nil
end

-- Função para substituir strings (substitui string.gsub)
local function replace_str(text, old, new)
    local result = ""
    local pos = 1
    local old_len = string.len(old)
    
    while true do
        local start_pos, end_pos = find_str(text, old, pos)
        if not start_pos then
            result = result .. string.sub(text, pos)
            break
        end
        
        result = result .. string.sub(text, pos, start_pos - 1) .. new
        pos = end_pos + 1
    end
    
    return result
end

-- Função para extrair título da página
local function extract_title(html)
    -- Procura <title>
    local title_start = find_str(html, "<title>")
    local title_end = find_str(html, "</title>")
    
    if title_start and title_end then
        return string.sub(html, title_start + 7, title_end - 1)
    end
    
    -- Procura <h1>
    local h1_start = find_str(html, "<h1>")
    local h1_end = find_str(html, "</h1>")
    
    if h1_start and h1_end then
        return string.sub(html, h1_start + 4, h1_end - 1)
    end
    
    return nil
end

-- Função simplificada para remover tags HTML - evita problemas com &
local function strip_tags(text)
    if not text then return "" end
    
    local result = ""
    local in_tag = false
    local text_len = string.len(text)
    
    for i = 1, text_len do
        local char = string.sub(text, i, i)
        
        if char == "<" then
            in_tag = true
        elseif char == ">" then
            in_tag = false
        elseif not in_tag then
            result = result .. char
        end
    end
    
    return result
end

-- Função para extrair conteúdo entre tags (versão simplificada)
local function extract_between_tags(html, tag_name)
    local content = {}
    local pos = 1
    local html_len = string.len(html)
    
    while pos <= html_len do
        -- Encontra abertura da tag
        local open_tag = "<" .. tag_name .. ">"
        local open_start, open_end = find_str(html, open_tag, pos)
        if not open_start then break end
        
        -- Encontra tag de fechamento
        local close_tag = "</" .. tag_name .. ">"
        local close_start = find_str(html, close_tag, open_end + 1)
        if not close_start then break end
        
        -- Extrai conteúdo
        local tag_content = string.sub(html, open_end + 1, close_start - 1)
        local clean_content = strip_tags(tag_content)
        
        -- Limpa espaços em branco
        clean_content = string.trim(clean_content)
        
        if clean_content and string.len(clean_content) > 0 then
            table.insert(content, {
                type = tag_name,
                text = clean_content
            })
        end
        
        pos = close_start + string.len(close_tag)
    end
    
    return content
end

-- Função para extrair todo o conteúdo textual
local function extract_content(html)
    local content = {}
    
    -- Extrai h1
    local h1_content = extract_between_tags(html, "h1")
    for _, item in ipairs(h1_content) do
        table.insert(content, item)
    end
    
    -- Extrai h2
    local h2_content = extract_between_tags(html, "h2")
    for _, item in ipairs(h2_content) do
        table.insert(content, item)
    end
    
    -- Extrai h3
    local h3_content = extract_between_tags(html, "h3")
    for _, item in ipairs(h3_content) do
        table.insert(content, item)
    end
    
    -- Extrai parágrafos
    local p_content = extract_between_tags(html, "p")
    for _, item in ipairs(p_content) do
        table.insert(content, item)
    end
    
    return content
end

-- Função para criar a interface do navegador
local function create_browser_screen(title, content, raw_html)
    local screen_title = title or url
    local screen_fields = {}
    
    -- Adiciona o conteúdo
    local content_added = false
    for i, item in ipairs(content) do
        if item.text and string.len(item.text) > 0 then
            local style = "default"
            
            if item.type == "h1" then style = "bold"
            elseif item.type == "h2" then style = "bold"
            elseif item.type == "h3" then style = "bold" end
            
            table.insert(screen_fields, {type = "text", value = item.text, style = style})
            table.insert(screen_fields, {type = "spacer", height = 3})
            content_added = true
        end
    end
    
    -- Se não encontrou conteúdo formatado, mostra resposta bruta
    if not content_added and raw_html then
        -- Para API que retorna JSON/texto puro
        local display_text = raw_html
        if string.len(display_text) > 500 then
            display_text = string.sub(display_text, 1, 500) .. "..."
        end
        
        table.insert(screen_fields, {type = "text", value = display_text})
    end
    
    -- Se não tem nenhum conteúdo, mostra mensagem
    if #screen_fields == 0 then
        table.insert(screen_fields, {type = "text", value = "Nenhum conteúdo encontrado"})
    end
    
    return graphics.BuildScreen({
        title = screen_title,
        back = {
            label = "Voltar",
            root = "xterm"
        },
        fields = screen_fields
    })
end

-- Função principal
local function main()
    -- Tela de carregamento
    local loading_screen = graphics.BuildScreen({
        title = "Navegador",
        fields = {
            {type = "text", value = "Carregando: " .. url},
            {type = "text", value = "Aguarde..."}
        }
    })
    graphics.display(loading_screen)
    
    -- Faz a requisição HTTP
    local ok, html_content, status = pcall(socket.http.get, url)
    if not ok then
        graphics.display(graphics.BuildScreen({
            title = "Erro",
            fields = {
                {type = "text", value = "Erro ao carregar:"},
                {type = "text", value = tostring(html_content)},
                {type = "spacer", height = 10},
                {
                    type = "item",
                    label = "Voltar",
                    root = "xterm"
                }
            }
        }))
        return
    end
    
    -- Para APIs como opentty.xyz/api/ip que retornam JSON/texto puro
    -- Verifica se é conteúdo HTML ou texto puro
    local is_html = find_str(html_content, "<html") or find_str(html_content, "<body") or find_str(html_content, "<div") or find_str(html_content, "<p")
    
    if is_html then
        -- Processa como HTML
        local title = extract_title(html_content)
        local content = extract_content(html_content)
        
        -- Cria e exibe a tela do navegador
        local browser_screen = create_browser_screen(title, content, html_content)
        graphics.display(browser_screen)
    else
        -- É texto puro/JSON - mostra diretamente
        local screen_title = url
        -- Tenta extrair um título do conteúdo (primeira linha)
        local first_line_end = find_str(html_content, "\n") or (string.len(html_content) + 1)
        local first_line = string.sub(html_content, 1, first_line_end - 1)
        first_line = string.trim(first_line)
        
        -- Se a primeira linha for curta, usa como título
        if string.len(first_line) > 0 and string.len(first_line) < 30 then
            screen_title = first_line
        end
        
        graphics.display(graphics.BuildScreen({
            title = screen_title,
            fields = {
                {type = "text", value = html_content}
            },
            back = {
                label = "Voltar",
                root = "xterm"
            }
        }))
    end
end

-- Inicia o navegador
main()