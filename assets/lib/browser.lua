#!/bin/lua


local browser = { }

local function fetch_url(url)
    local res, _ = socket.http.get(url)
    return res
end
local function parse_html(html)
    local fields = {}
    local current_text = ""
    local in_h1, in_p, in_a = false, false, false
    local a_href = nil
    local i = 1
    local len = #html

    while i <= len do
        local c = string.sub(html, i, i)
        if c == "<" then
            if current_text ~= "" then
                if in_a and a_href then
                    fields[#fields + 1] = {
                        type = "item",
                        label = current_text,
                        root = function() browser.load(a_href) end
                    }
                else
                    local style = in_h1 and "bold" or "default"
                    fields[#fields + 1] = {
                        type = "text",
                        label = "",
                        value = current_text,
                        style = style
                    }
                end
                current_text = ""
            end

            local tag_end = string.match(html, ">", i)
            if tag_end then
                local tag_content = string.sub(html, i + 1, tag_end - 1)
                local tag = tag_content
                local space = string.match(tag_content, " ")
                if space then tag = string.sub(tag_content, 1, space - 1) end
                tag = string.lower(tag)

                if tag == "h1" then in_h1 = true
                elseif tag == "/h1" then in_h1 = false
                elseif tag == "p" then in_p = true
                elseif tag == "/p" then in_p = false
                elseif tag == "br" then fields[#fields + 1] = { type = "spacer", width = 1, height = 1 }
                elseif tag == "a" then
                    in_a = true
                    local href_start = string.match(tag_content, "href=")
                    if href_start then
                        local quote1 = string.match(tag_content, '"', href_start)
                        if quote1 then
                            local quote2 = string.match(tag_content, '"', quote1 + 1)
                            if quote2 then a_href = string.sub(tag_content, quote1 + 1, quote2 - 1) end
                        end
                    end
                elseif tag == "/a" then
                    in_a = false
                    a_href = nil
                end

                i = tag_end + 1
            else break end
        else
            current_text = current_text .. c
            i = i + 1
        end
    end

    if current_text ~= "" then
        if in_a and a_href then
            fields[#fields + 1] = {
                type = "item",
                label = current_text,
                root = function() browser.load(a_href) end
            }
        else
            local style = in_h1 and "bold" or "default"
            fields[#fields + 1] = {
                type = "text",
                label = "",
                value = current_text,
                style = style
            }
        end
    end

    return fields
end

local function extract_title(html, url)
    local start = string.match(html, "<title>", 1)
    local _end = string.match(html, "</title>", 1)

    if start and _end then
        start = start + 7
        _end = _end - 1
        local raw = string.trim(string.sub(html, start, _end))
        if raw ~= "" then
            return raw
        end
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
        back = { root = browser.open },
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
        back = { label = "Exit" },
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

if #arg == 1 then browser.main()
else browser.load(arg[1]) end