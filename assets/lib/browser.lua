#!/bin/lua


local browser = { }

local function fetch_url(url)
    local res, status = socket.http.get(url)
    return res
end
local function join_styles(styles)
    local s = ""
    for k, v in pairs(styles) do
        if v then
            if s == "" then s = k else s = s .. " " .. k end
        end
    end
    if s == "" then return "default" end
    return s
end

local function parse_html(html)
    if not string.match(html, "<") then
        return { string.trim(html) }
        print("unreacheble")
    end

    local fields = {}
    local i = 1
    local styles = {}
    local in_head = false
    local in_script = false
    local in_style = false

    while i <= #html do
        local start_tag = string.match(html, "<", i)
        if start_tag == nil then
            if not in_head and not in_script and not in_style then
                local text = string.trim(string.sub(html, i))
                if text ~= "" then
                    fields[#fields + 1] = { type = "text", value = text, style = join_styles(styles) }
                end
            end
            break
        end

        -- Texto antes da tag
        if start_tag > i and not in_head and not in_script and not in_style then
            local text = string.trim(string.sub(html, i, start_tag - 1))
            if text ~= "" then
                fields[#fields + 1] = { type = "text", value = text, style = join_styles(styles) }
            end
        end

        local end_tag = string.match(html, ">", start_tag)
        if not end_tag then break end

        local tag = string.lower(string.trim(string.sub(html, start_tag + 1, end_tag - 1)))

        -- Controle head/script/style
        if tag == "head" then in_head = true
        elseif tag == "/head" then in_head = false
        elseif tag == "script" then in_script = true
        elseif tag == "/script" then in_script = false
        elseif tag == "style" then in_style = true
        elseif tag == "/style" then in_style = false
        elseif not in_head and not in_script and not in_style then
            if tag == "b" then styles["bold"] = true
            elseif tag == "/b" then styles["bold"] = nil
            elseif tag == "i" then styles["italic"] = true
            elseif tag == "/i" then styles["italic"] = nil
            elseif tag == "u" then styles["ul"] = true
            elseif tag == "/u" then styles["ul"] = nil
            elseif tag == "small" then styles["small"] = true
            elseif tag == "/small" then styles["small"] = nil
            elseif tag == "large" then styles["large"] = true
            elseif tag == "/large" then styles["large"] = nil
            elseif tag == "p" then styles["small"] = true
            elseif tag == "/p" then styles["small"] = nil
            elseif tag == "h1" then styles["large"] = true; styles["bold"] = true
            elseif tag == "/h1" then styles["large"] = nil; styles["bold"] = nil
            elseif tag == "h2" then styles["bold"] = true
            elseif tag == "/h2" then styles["bold"] = nil
            elseif tag == "h3" then styles["bold"] = true; styles["small"] = true
            elseif tag == "/h3" then styles["bold"] = nil; styles["small"] = nil
            elseif tag == "h4" or tag == "h5" or tag == "h6" then styles["small"] = true
            elseif tag == "/h4" or tag == "/h5" or tag == "/h6" then styles["small"] = nil
            elseif tag == "br" then fields[#fields + 1] = { type = "text", value = "\n", style = "default" }
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
    elseif string.sub(url, 1, 7) == "file://" then html = io.read(string.sub(url, 8))
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

function browser.tabs() end
function browser.bookmarks() end
function browser.settings() end

function browser.main()
    graphics.display(graphics.BuildList({
        title = "Browser",
        fields = { "Open URL", "Tabs", "Bookmarks", "Settings", "Exit" },
        back = { label = "Exit", root = os.exit },
        button = {
            label = "Select",
            root = function (opt)
                if opt == "Open URL" then browser.open()
                elseif opt == "Tabs" then browser.tabs()
                elseif opt == "Bookmarks" then browser.bookmarks()
                elseif opt == "Settings" then browser.settings()
                elseif opt == "Exit" then os.exit()
                end
            end
        }
    }))
end


os.setproc("name", "browser")

if #arg == 1 then browser.main()
else browser.load(arg[1]) end