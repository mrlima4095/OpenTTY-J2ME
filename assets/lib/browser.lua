#!/bin/lua


local browser = { }
local trim = string.trim

local function fetch_url(url)
    local res, status = socket.http.get(url)
    return res
end


local function parse_html(html)
    local fields = {}
    local i = 1
    local current_style = "default"
    local in_head = false

    while i <= #html do
        local start_tag = string.match(html, "<", i)
        if not start_tag then
            if not in_head then
                local text = trim(string.sub(html, i))
                if text ~= "" then
                    fields[#fields + 1] = { type = "text", value = text, style = current_style }
                end
            end
            break
        end

        -- Texto antes da tag
        if start_tag > i and not in_head then
            local text = trim(string.sub(html, i, start_tag - 1))
            if text ~= "" then
                fields[#fields + 1] = { type = "text", value = text, style = current_style }
            end
        end

        -- Encontra final da tag
        local end_tag = string.match(html, ">", start_tag)
        if not end_tag then break end

        local tag = string.lower(trim(string.sub(html, start_tag + 1, end_tag - 1)))

        -- Controle head
        if tag == "head" then in_head = true
        elseif tag == "/head" then in_head = false
        elseif not in_head then
            -- Atualiza estilo
            if tag == "b" then current_style = "bold"
            elseif tag == "/b" then current_style = "default"
            elseif tag == "i" then current_style = "italic"
            elseif tag == "/i" then current_style = "default"
            elseif tag == "small" then current_style = "small"
            elseif tag == "/small" then current_style = "default"
            elseif tag == "large" then current_style = "large"
            elseif tag == "/large" then current_style = "default"
            elseif tag == "p" then current_style = "small"
            elseif tag == "/p" then current_style = "default"
            elseif tag == "h1" then current_style = "large bold"
            elseif tag == "/h1" then current_style = "default"
            elseif tag == "h2" then current_style = "bold"
            elseif tag == "/h2" then current_style = "default"
            elseif tag == "h3" then current_style = "bold small"
            elseif tag == "/h3" then current_style = "default"
            elseif tag == "h4" then current_style = "small"
            elseif tag == "/h4" then current_style = "default"
            elseif tag == "h5" then current_style = "small"
            elseif tag == "/h5" then current_style = "default"
            elseif tag == "h6" then current_style = "small"
            elseif tag == "/h6" then current_style = "default"
            elseif tag == "br" then
                fields[#fields + 1] = { type = "text", value = "\n", style = current_style }
            end
        end

        i = end_tag + 1
    end

    return fields
end
=


local function extract_title(html, url)
    local start, finish = string.match(html, "<title>"), string.match(html, "</title>")

    if start and finish then
        local title = string.sub(html, start + 7, finish - 1)
        if not title or title == "" then
            return url
        end
        return string.trim(title)
    end

    return url
end


function browser.load(url)
    local html

    if string.sub(url, 1, 7) == "http://" then
        graphics.SetTicker("Loading...")
        html = fetch_url(url)
    elseif string.sub(url, 1, 7) == "file://" then
        html = io.read(string.sub(url, 8))
    else
        url = "http://" .. url
        graphics.SetTicker("Loading...")
        html = fetch_url(url)
    end

    graphics.SetTicker(nil)

    if not html then
        graphics.display(graphics.Alert({
            title = "Browser",
            message = "Rendering failed!",
            back = { root = browser.main }
        }))
        return
    end

    graphics.display(graphics.BuildScreen({
        title = extract_title(html, url),
        fields = parse_html(html),
        back = { root = function () if arg[1] == nil then browser.open() else os.exit() end end },
        button = { label = "Menu", root = browser.main }
    }))
end

function browser.open()
    graphics.display(graphics.BuildQuest({
        title = "Browser",
        label = "URL:",
        content = "http://",
        back = { label = "Back", root = browser.main },
        button = { label = "Go", root = browser.load }
    }))
end

function browser.main()
    graphics.display(graphics.BuildList({
        title = "Browser",
        fields = { "Open URL", "Exit" },
        back = { label = "Exit", root = os.exit },
        button = {
            label = "Select",
            root = function (opt)
                if opt == "Open URL" then browser.open()
                elseif opt == "Exit" then os.exit()
                end
            end
        }
    }))
end


os.setproc("name", "browser")

if #arg == 1 then
    browser.main()
else
    browser.load(arg[1])
end