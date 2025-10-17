#!/bin/lua

-- Navegador Web Simples para Lua J2ME
-- Define a URL a ser carregada
local url = arg and arg[1] or "http://opentty.xyz/api/ip"

-- Função para extrair título da página
local function extract_title(html)
    if not html then return nil end
    
    -- Procura <title>
    local title_start, title_end = string.find(html, "<title>")
    if title_start and title_end then
        local close_start, close_end = string.find(html, "</title>", title_end + 1)
        if close_start and close_end then
            return string.sub(html, title_end + 1, close_start - 1)
        end
    end
    
    -- Procura <h1>
    local h1_start, h1_end = string.find(html, "<h1>")
    if h1_start and h1_end then
        local close_start, close_end = string.find(html, "</h1>", h1_end + 1)
        if close_start and close_end then
            return string.sub(html, h1_end + 1, close_start - 1)
        end
    end
    
    return nil
end

-- Função simplificada para remover tags HTML
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

-- Função para extrair conteúdo entre tags (corrigida contra loop infinito)
local function extract_between_tags(html, tag_name)
    local content = {}
    if not html then return content end
    
    local pos = 1
    local html_len = string.len(html)
    local max_iterations = 1000 -- Prevenção contra loop infinito
    local iterations = 0
    
    while pos <= html_len and iterations < max_iterations do
        iterations = iterations + 1
        
        -- Encontra abertura da tag
        local open_tag = "<" .. tag_name .. ">"
        local open_start, open_end = string.find(html, open_tag, pos)
        if not open_start or not open_end then break end
        
        -- Encontra tag de fechamento
        local close_tag = "</" .. tag_name .. ">"
        local close_start, close_end = string.find(html, close_tag, open_end + 1)
        if not close_start or not close_end then 
            -- Se não encontrou fechamento, avança a posição para evitar loop
            pos = open_end + 1
            break 
        end
        
        -- Extrai conteúdo
        local tag_content = string.sub(html, open_end + 1, close_start - 1)
        local clean_content = strip_tags(tag_content)
        clean_content = string.trim(clean_content)
        
        if clean_content and string.len(clean_content) > 0 then
            table.insert(content, {
                type = tag_name,
                text = clean_content
            })
        end
        
        pos = close_end + 1
    end
    
    return content
end

-- Função para extrair todo o conteúdo textual
local function extract_content(html)
    local content = {}
    if not html then return content end

    local h1_content = extract_between_tags(html, "h1")
    for _, item in ipairs(h1_content) do table.insert(content, item) end

    local h2_content = extract_between_tags(html, "h2")
    for _, item in ipairs(h2_content) do table.insert(content, item) end

    local h3_content = extract_between_tags(html, "h3")
    for _, item in ipairs(h3_content) do table.insert(content, item) end

    local p_content = extract_between_tags(html, "p")
    for _, item in ipairs(p_content) do table.insert(content, item) end

    return content
end

local function create_browser_screen(title, content, raw_html)
    local screen_fields = {}

    -- Adiciona o conteúdo
    local content_added = false
    for i, item in ipairs(content) do
        if item and item.text and string.len(item.text) > 0 then
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
        local display_text = raw_html or ""
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
        title = title or url,
        back = { label = "Back", root = "xterm" },
        fields = screen_fields
    })
end

local function main()
    local loading_screen = graphics.BuildScreen({
        title = "Browser",
        fields = { 
            { type = "text", value = "Loading: " .. url }, 
            { type = "text", value = "Please wait..." } 
        }
    })
    graphics.display(loading_screen)

    local ok, html_content, status = pcall(socket.http.get, url)
    if not ok or not html_content then
        graphics.display(graphics.BuildScreen({
            title = "Error",
            fields = { 
                { type = "text", value = "Loading error:" }, 
                {type = "text", value = tostring(html_content) } 
            },
            back = { label = "Back", root = "xterm" }
        }))
        return
    end

    -- Verifica se é conteúdo HTML ou texto puro de forma mais simples
    local is_html = false
    if string.find(html_content, "<html") then is_html = true
    elseif string.find(html_content, "<body") then is_html = true
    elseif string.find(html_content, "<div") then is_html = true
    elseif string.find(html_content, "<p") then is_html = true
    elseif string.find(html_content, "<h1") then is_html = true
    elseif string.find(html_content, "<h2") then is_html = true
    elseif string.find(html_content, "<h3") then is_html = true end

    if is_html then
        local title = extract_title(html_content)
        local content = extract_content(html_content)

        local browser_screen = create_browser_screen(title, content, html_content)
        graphics.display(browser_screen)
    else
        -- É texto puro/JSON - mostra diretamente
        graphics.display(graphics.BuildScreen({
            title = url,
            fields = { { type = "text", value = html_content } },
            back = { label = "Back", root = "xterm" }
        }))
    end
end

os.setproc("name", "browser")
main()