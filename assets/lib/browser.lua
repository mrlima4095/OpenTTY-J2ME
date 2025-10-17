#!/bin/lua


-- Navegador Web Simples para Lua J2ME (sem regex)
-- Define a URL a ser carregada
local url = arg and arg[1] or "http://opentty.xyz/api/ip"

-- Função para fazer requisição HTTP
local function http_get(url)
    local result, status = socket.http.get(url)
    if result and status == 200 then
        return result
    else
        return nil, "Erro HTTP: " .. tostring(status)
    end
end

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
    
    return "Sem título"
end

-- Função para remover tags HTML
local function strip_tags(text)
    local result = text
    local changed = true
    
    -- Remove tags iterativamente
    while changed do
        changed = false
        local tag_start = find_str(result, "<")
        local tag_end = find_str(result, ">")
        
        if tag_start and tag_end and tag_start < tag_end then
            result = string.sub(result, 1, tag_start - 1) .. string.sub(result, tag_end + 1)
            changed = true
        end
    end
    
    -- Substitui entidades HTML básicas manualmente
    result = replace_str(result, "&lt;", "<")
    result = replace_str(result, "&gt;", ">")
    result = replace_str(result, "&amp;", "&")
    result = replace_str(result, "&quot;", "\"")
    result = replace_str(result, "&#39;", "'")
    result = replace_str(result, "&nbsp;", " ")
    
    return result
end

-- Função para extrair conteúdo entre tags
local function extract_between_tags(html, tag_name)
    local content = {}
    local pos = 1
    local html_len = string.len(html)
    
    while pos <= html_len do
        -- Encontra abertura da tag
        local open_tag = "<" .. tag_name
        local open_start, open_end = find_str(html, open_tag, pos)
        if not open_start then break end
        
        -- Encontra o final da tag de abertura
        local gt_pos = find_str(html, ">", open_end)
        if not gt_pos then break end
        
        -- Encontra tag de fechamento
        local close_tag = "</" .. tag_name .. ">"
        local close_start = find_str(html, close_tag, gt_pos + 1)
        if not close_start then break end
        
        -- Extrai conteúdo
        local tag_content = string.sub(html, gt_pos + 1, close_start - 1)
        local clean_content = strip_tags(tag_content)
        
        -- Remove espaços em branco excessivos manualmente
        local final_content = ""
        local last_char = ""
        for i = 1, string.len(clean_content) do
            local char = string.sub(clean_content, i, i)
            if char ~= " " or last_char ~= " " then
                final_content = final_content .. char
                last_char = char
            end
        end
        
        final_content = string.trim(final_content)
        
        if final_content and string.len(final_content) > 0 then
            table.insert(content, {
                type = tag_name,
                text = final_content
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
    
    -- Extrai divs (como fallback)
    local div_content = extract_between_tags(html, "div")
    for _, item in ipairs(div_content) do
        table.insert(content, item)
    end
    
    return content
end

-- Função para criar a interface do navegador
local function create_browser_screen(title, content)
    local screen_fields = {
        {type = "text", value = "Navegando: " .. url, style = "bold"},
        {type = "spacer", height = 10}
    }
    
    -- Adiciona o título
    if title and title ~= "Sem título" then
        table.insert(screen_fields, {type = "text", value = title, style = "bold"})
        table.insert(screen_fields, {type = "spacer", height = 5})
    end
    
    -- Adiciona o conteúdo
    local content_added = false
    for i, item in ipairs(content) do
        if item.text and string.len(item.text) > 0 then
            local style = "default"
            
            if item.type == "h1" then
                style = "bold"
            elseif item.type == "h2" then
                style = "bold" 
            elseif item.type == "h3" then
                style = "bold"
            end
            
            table.insert(screen_fields, {type = "text", value = item.text, style = style})
            table.insert(screen_fields, {type = "spacer", height = 3})
            content_added = true
        end
    end
    
    -- Se não encontrou conteúdo formatado, mostra texto bruto
    if not content_added then
        local text_only = strip_tags(html or "")
        -- Limita o tamanho manualmente
        if string.len(text_only) > 500 then
            text_only = string.sub(text_only, 1, 500) .. "..."
        end
        
        if string.len(text_only) > 0 then
            table.insert(screen_fields, {type = "text", value = "Conteúdo bruto:"})
            table.insert(screen_fields, {type = "text", value = text_only})
        end
    end
    
    -- Botões de navegação
    table.insert(screen_fields, {type = "spacer", height = 10})
    table.insert(screen_fields, {
        type = "item",
        label = "Recarregar",
        root = function()
            -- Recarrega executando o script novamente
            os.execute("lua browser.lua " .. url)
        end
    })
    
    table.insert(screen_fields, {
        type = "item", 
        label = "Voltar ao Terminal",
        root = "xterm"
    })
    
    return graphics.BuildScreen({
        title = "Navegador Web",
        back = {
            label = "Sair",
            root = "xterm"
        },
        fields = screen_fields
    })
end

-- Variável global para armazenar HTML
html = nil

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
    local html_content, err = http_get(url)
    html = html_content
    
    if not html then
        graphics.display(graphics.BuildScreen({
            title = "Erro",
            fields = {
                {type = "text", value = "Erro ao carregar:"},
                {type = "text", value = tostring(err)},
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
    
    -- Processa o HTML
    local title = extract_title(html)
    local content = extract_content(html)
    
    -- Cria e exibe a tela do navegador
    local browser_screen = create_browser_screen(title, content)
    graphics.display(browser_screen)
end

-- Inicia o navegador
main()