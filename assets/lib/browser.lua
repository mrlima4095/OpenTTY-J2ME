local browser = {
    version = "1.0",

    xback = os.exit,
    xmenucfg = {
        title = "Browser",
        back = { root = os.exit },
        button = {
            root = function (opt)
                if opt == "Open URL" then
                    browser.quest()
                end
            end
        },

        fields = {
            "Open URL"
        }
    },

    headers = {}
}

function browser.render(raw)
    local fields = {} -- cria um vetor para os campos
    local pos = 1
    local len = string.len(raw)

    -- Função auxiliar para extrair tag e conteúdo simples sem regex
    local function parseTag(text, startPos)
        local s = string.find(text, "<", startPos, true)
        if s ~= startPos then return nil end -- tag deve começar exatamente em startPos

        -- procura o fechamento da tag de abertura '>'
        local tagEnd = string.find(text, ">", s, true)
        if not tagEnd then return nil end

        -- extrai o conteúdo da tag de abertura, ex: b, a href="..."
        local tagContent = string.sub(text, s + 1, tagEnd - 1)

        -- separa o nome da tag e os atributos (se houver)
        local spacePos = string.find(tagContent, " ", 1, true)
        local tagName, attr
        if spacePos then
            tagName = string.sub(tagContent, 1, spacePos - 1)
            attr = string.sub(tagContent, spacePos + 1)
        else
            tagName = tagContent
            attr = nil
        end

        -- procura a tag de fechamento correspondente </tagName>
        local closeTag = "</" .. tagName .. ">"
        local closeStart = string.find(text, closeTag, tagEnd + 1, true)
        if not closeStart then return nil end

        -- extrai o conteúdo entre as tags
        local content = string.sub(text, tagEnd + 1, closeStart - 1)

        return tagName, attr, content, closeStart + string.len(closeTag)
    end

    -- Função para extrair href do atributo (procura href="...")
    local function extractHref(attr)
        if not attr then return nil end
        local hrefStart = string.find(attr, 'href="', 1, true)
        if not hrefStart then return nil end
        local hrefEnd = string.find(attr, '"', hrefStart + 6, true)
        if not hrefEnd then return nil end
        return string.sub(attr, hrefStart + 6, hrefEnd - 1)
    end

    -- Função para adicionar texto com estilo
    local function addText(value, style)
        style = style or "default"
        local field = {}
        field.put("type", "text")
        field.put("value", value)
        field.put("style", style)
        fields.addElement(field)
    end

    -- Função para adicionar botão
    local function addButton(label, url)
        local field = {}
        field.put("type", "item")
        field.put("label", label)
        -- root é uma função, mas aqui vamos guardar uma tabela com root = função para chamar browser.load(url)
        -- Como não podemos criar closures facilmente, vamos guardar uma tabela com root = função que chama browser.load(url)
        -- Para isso, criamos uma função anônima que chama browser.load(url)
        local func = function()
            browser.load(url)
        end
        field.put("root", func)
        fields.addElement(field)
    end

    while pos <= len do
        local nextTagStart = string.find(raw, "<", pos, true)
        if not nextTagStart then
            -- Sem mais tags, adiciona o resto como texto normal
            local text = string.sub(raw, pos)
            if string.len(text) > 0 then addText(text) end
            break
        end

        if nextTagStart > pos then
            -- Texto antes da tag
            local text = string.sub(raw, pos, nextTagStart - 1)
            if string.len(text) > 0 then addText(text) end
        end

        -- Tenta parsear tag
        local tag, attr, content, newPos = parseTag(raw, nextTagStart)
        if not tag then
            -- Tag mal formada, adiciona o resto como texto e sai
            local text = string.sub(raw, nextTagStart)
            if string.len(text) > 0 then addText(text) end
            break
        end

        tag = string.lower(tag)

        if tag == "b" then
            addText(content, "bold")
        elseif tag == "i" then
            addText(content, "italic")
        elseif tag == "large" then
            addText(content, "large")
        elseif tag == "a" then
            local href = extractHref(attr)
            if href then
                addButton(content, href)
            else
                addText(content)
            end
        else
            -- Tag desconhecida, adiciona conteúdo como texto normal
            addText(content)
        end

        pos = newPos
    end

    -- Botão personalizado para voltar ao menu principal
    local button = {}
    button.put("label", "Menu")
    button.put("root", browser.main)

    -- Monta a tela com título, botão Back para voltar para browser.quest e botão Menu
    local screen = graphics.BuildScreen({
        title = "Browser - Page",
        back = { root = browser.quest },
        button = button,
        fields = fields
    })

    return screen
end

-- Ajuste na função load para usar browser.render e exibir a tela
function browser.load(url)
    local raw, status = socket.http.get(url, browser.headers)

    if status ~= 200 then
        raw = "<title>404 - Not found</title>\nPage not found"
    end

    local screen = browser.render(raw)
    graphics.display(screen)
end

function browser.quest()
    graphics.display(graphics.BuildQuest({
        title = "Browser",
        label = "WebSite URL",
        content = "http://",
        back = { root = browser.main },
        button = { label = "Go", root = browser.load }
    }))
end

function browser.main()
    if browser.menu == nil then browser.menu = graphics.BuildList(browser.xmenucfg) end

    graphics.display(browser.menu)
end



os.setproc("name", "browser")
browser.quest()
