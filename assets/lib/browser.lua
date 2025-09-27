#!/bin/lua

-- Mini Navegador em Lua para o runtime J2ME
-- Este script cria um menu simples e um mini browser que pede uma URL,
-- faz HTTP GET, parseia HTML básico (h1 bold, p normal, etc.) e renderiza em um Form.

-- Função para fazer HTTP GET (usando socket.http.get)
local function fetch_url(url)
    local result, status = socket.http.get(url)
    return result
end

-- Função simples para parsear HTML básico
-- Suporta: <h1>texto</h1> (bold), <p>texto</p> (normal), <br> (nova linha), texto plano
-- Ignora o resto por simplicidade (não é um parser full)
local function parse_html(html)
    local fields = {}  -- Lista de campos para o form
    local current_text = ""
    local in_h1 = false
    local in_p = false
    local i = 1
    local len = #html

    while i <= len do
        local c = html:sub(i, i)
        if c == "<" then
            -- Fim de tag anterior
            if current_text ~= "" then
                if in_h1 then
                    fields[#fields + 1] = {type="text", label="", value=current_text, style="bold"}
                elseif in_p then
                    fields[#fields + 1] = {type="text", label="", value=current_text, style="default"}
                else
                    fields[#fields + 1] = {type="text", label="", value=current_text, style="default"}
                end
                current_text = ""
            end

            -- Parse tag
            local tag_end = html:find(">", i)
            if tag_end then
                local tag = html:sub(i+1, tag_end-1):lower():gsub("/%s*", "")  -- Remove espaços e /

                if tag == "h1" then
                    in_h1 = true
                elseif tag == "/h1" then
                    in_h1 = false
                elseif tag == "p" then
                    in_p = true
                elseif tag == "/p" then
                    in_p = false
                elseif tag == "br" then
                    fields[#fields + 1] = {type="spacer", width=1, height=1}  -- Quebra de linha simples
                end

                i = tag_end + 1
            else
                break
            end
        else
            current_text = current_text .. c
            i = i + 1
        end
    end

    -- Adiciona texto restante
    if current_text ~= "" then
        local style = in_h1 and "bold" or "default"
        fields[#fields + 1] = {type="text", label="", value=current_text, style=style}
    end

    return fields
end

-- Função para construir e mostrar o form do browser
local function show_browser_page(title, fields)
    local screen_table = {
        title = title or "Página Carregada",
        fields = fields,  -- Lista de campos já parseados
        back = {label="Voltar"},
        button = {label="Fechar", root="menu"}  -- Volta ao menu
    }
    return graphics.BuildScreen(screen_table)
end

-- Função para carregar URL (chamada após input)
local function load_url(url)
    local url = io.read("stdin") or "http://example.com"  -- Pega o input do quest
    if not string.match(url, "https://") then
        url = "http://" .. url
    end

    print("Carregando: " .. url)
    local html = fetch_url(url)
    if not html then
        print("Erro ao carregar URL")
        return
    end

    local fields = parse_html(html)
    local page_screen = show_browser_page("Conteúdo de " .. url, fields)
    graphics.display(page_screen)
end

-- Função para o browser
local function browser()
    -- Pede URL
    local quest_table = {
        title = "Digite a URL",
        label = "URL:",
        content = "http://",
        type = "default",  -- TextField normal
        back = {label="Cancelar"},
        button = {label="Carregar", root=load_url}
    }
    local quest_screen = graphics.BuildQuest(quest_table)
    graphics.display(quest_screen)

    -- Aqui, o runtime cuida do input via commandAction, mas para simular o fluxo,
    -- assumimos que após input, chamamos load_url com o valor.
    -- No script real, o root="load_url" chama a função abaixo.
end


-- Handler para menu (simulado; no real, usa ITEM ou commandAction)
local function handle_menu(...)
    local selected = select(1, ...) or 1  -- Pega o selecionado da list
    if selected == 1 then
        browser()
    elseif selected == 2 then
        os.exit(0)
    end
end

-- Menu principal
local function menu()
    local menu_items = {"1. Mini Browser", "2. Sair"}
    local list_table = {
        title = "Menu Principal",
        fields = menu_items,
        back = {label="Sair"},
        button = {label="Selecionar", root=handle_menu}
    }
    local menu_screen = graphics.BuildList(list_table)
    graphics.display(menu_screen)
end


-- Inicialização
menu()

-- Nota: Este script assume que o runtime lida com os eventos de tela (commandAction).
-- Para inputs, use graphics.BuildQuest e defina root para funções como load_url.
-- Para múltiplos retornos em funções, use return {val1, val2}.
-- Rode com: lua mini_browser.lua