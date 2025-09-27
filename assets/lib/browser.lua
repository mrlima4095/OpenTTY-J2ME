#!/bin/lua


local browser = {
    _previous = ""
}

local function fetch_url(url)
    local result, status = socket.http.get(url)
    return result
end
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

function browser.load(url)
    if string.sub(url, 0, 7) ~= "http://" then url = "http://" .. url end

    graphics.SetTicker("Loading...")
    local html = fetch_url(url)
    if not html then
        graphics.display(graphics.Alert({
            title = "Browser",
            message = "Rendering failed!",
            back = { root = browser.main }
        }))

        return
    end

    graphics.display(graphics.BuildScreen({
        title = url,
        fields = parse_html(html),
        back = { root = browser.open() },
        button = { root = browser.main() }
    }))
end

function browser.open()
    graphics.display(graphics.BuildQuest({
        title = "Digite a URL",
        label = "URL:",
        content = "http://",
        back = { label = "Back" },
        button = { label = "Go", root = browser.load }
    }))
end


function browser.handle_menu(opt)
    if opt == "Open URL" then browser.open()
    elseif opt == "Exit" then os.exit()
    end
end

function browser.main()
    graphics.display(graphics.BuildList({
        title = "Browser",
        fields = { "Open URL", "Exit" },
        back = { label = "Exit" },
        button = { root = browser.handle_menu }
    }))
end

browser.main()