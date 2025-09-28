#!/bin/lua


local browser = { }

local function fetch_url(url)
    local res, status = socket.http.get(url)
    return res
end

local function join_styles(styles)
    if #styles == 0 then return "default" end
    local s = ""
    for i = 1, #styles do
        if s == "" then s = styles[i] else s = s .. " " .. styles[i] end
    end
    return s
end

local function parse_html(html)
    local fields = {}
    local i = 1
    local styles = {} -- pilha de estilos
    local in_head = false

    while i <= #html do
        local start_tag = string.match(html, "<", i)
        if not start_tag then
            if not in_head then
                local text = string.trim(string.sub(html, i))
                if text ~= "" then
                    fields[#fields + 1] = { type = "text", value = text, style = join_styles(styles) }
                end
            end
            break
        end

        -- Texto antes da tag
        if start_tag > i and not in_head then
            local text = string.trim(string.sub(html, i, start_tag - 1))
            if text ~= "" then
                fields[#fields + 1] = { type = "text", value = text, style = join_styles(styles) }
            end
        end

        -- Final da tag
        local end_tag = string.match(html, ">", start_tag)
        if not end_tag then break end

        local tag = string.lower(string.trim(string.sub(html, start_tag + 1, end_tag - 1)))

        -- Controle head
        if tag == "head" then in_head = true
        elseif tag == "/head" then in_head = false
        elseif not in_head then
            if tag == "b" then styles[#styles + 1] = "bold"
            elseif tag == "/b" then if #styles > 0 and styles[#styles] == "bold" then styles[#styles] = nil end
            elseif tag == "i" then styles[#styles + 1] = "italic"
            elseif tag == "/i" then if #styles > 0 and styles[#styles] == "italic" then styles[#styles] = nil end
            elseif tag == "small" then styles[#styles + 1] = "small"
            elseif tag == "/small" then if #styles > 0 and styles[#styles] == "small" then styles[#styles] = nil end
            elseif tag == "large" then styles[#styles + 1] = "large"
            elseif tag == "/large" then if #styles > 0 and styles[#styles] == "large" then styles[#styles] = nil end
            elseif tag == "p" then styles[#styles + 1] = "small"
            elseif tag == "/p" then if #styles > 0 and styles[#styles] == "small" then styles[#styles] = nil end
            elseif tag == "h1" then styles[#styles + 1] = "large bold"
            elseif tag == "/h1" then if #styles > 0 and styles[#styles] == "large bold" then styles[#styles] = nil end
            elseif tag == "h2" then styles[#styles + 1] = "bold"
            elseif tag == "/h2" then if #styles > 0 and styles[#styles] == "bold" then styles[#styles] = nil end
            elseif tag == "h3" then styles[#styles + 1] = "bold small"
            elseif tag == "/h3" then if #styles > 0 and styles[#styles] == "bold small" then styles[#styles] = nil end
            elseif tag == "h4" then styles[#styles + 1] = "small"
            elseif tag == "/h4" then if #styles > 0 and styles[#styles] == "small" then styles[#styles] = nil end
            elseif tag == "h5" then styles[#styles + 1] = "small"
            elseif tag == "/h5" then if #styles > 0 and styles[#styles] == "small" then styles[#styles] = nil end
            elseif tag == "h6" then styles[#styles + 1] = "small"
            elseif tag == "/h6" then if #styles > 0 and styles[#styles] == "small" then styles[#styles] = nil end
            elseif tag == "br" then
                fields[#fields + 1] = { type = "text", value = "\n", style = join_styles(styles) }
            end
        end

        i = end_tag + 1
    end

    return fields
end



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
        fields = { "Open URL", "Tabs", "Bookmarks", "Settings", "Exit" },
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

if #arg == 1 then browser.main()
else browser.load(arg[1]) end