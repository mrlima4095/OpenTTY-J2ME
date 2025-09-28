#!/bin/lua


local browser = { }

local function has_content(s)
    for i = 1, #s do
        local c = string.sub(s, i, i)
        if c ~= " " and c ~= "\t" and c ~= "\n" and c ~= "\r" then
            return true
        end
    end
    return false
end

local function fetch_url(url)
    local res, status = socket.http.get(url)
    return res
end

local function parse_html(html)
    local fields = {}
    local current_text = ""
    local in_h1, in_a, in_head = false, false, false
    local a_href = nil

    local i = 1
    local len = #html

    local function trim(s)
        -- remove espaços no início e no fim
        local first, last = 1, #s
        while first <= #s and (string.sub(s, first, first) == " " or string.sub(s, first, first) == "\t" or string.sub(s, first, first) == "\n" or string.sub(s, first, first) == "\r") do
            first = first + 1
        end
        while last >= first and (string.sub(s, last, last) == " " or string.sub(s, last, last) == "\t" or string.sub(s, last, last) == "\n" or string.sub(s, last, last) == "\r") do
            last = last - 1
        end
        if last < first then return "" end
        return string.sub(s, first, last)
    end

    local function add_field()
        if in_head then return end
        local text = trim(current_text)
        if text ~= "" or in_a or in_h1 then
            if in_a and a_href then
                local href_copy = a_href
                fields[#fields + 1] = {
                    type = "item",
                    label = text,
                    root = function() print("clicou em link:", href_copy) end
                }
            else
                local style = in_h1 and "bold" or "default"
                fields[#fields + 1] = {
                    type = "text",
                    label = "",
                    value = text,
                    style = style
                }
            end
        end
        current_text = ""
    end

    while i <= len do
        local c = string.sub(html, i, i)
        if c == "<" then
            add_field()

            -- encontra o fechamento da tag
            local j = i + 1
            while j <= len and string.sub(html, j, j) ~= ">" do j = j + 1 end
            if j > len then break end

            local tag_content = string.sub(html, i + 1, j - 1)
            -- pega o nome da tag (até o primeiro espaço ou até o final)
            local space_pos = nil
            for k = 1, #tag_content do
                if string.sub(tag_content, k, k) == " " then
                    space_pos = k
                    break
                end
            end
            local tag = nil
            if space_pos then
                tag = string.sub(tag_content, 1, space_pos - 1)
            else
                tag = tag_content
            end
            -- converte para minúscula manualmente
            local t = ""
            for k = 1, #tag do
                local ch = string.sub(tag, k, k)
                local byte = string.byte(ch)
                if byte >= 65 and byte <= 90 then
                    t = t .. string.char(byte + 32)
                else
                    t = t .. ch
                end
            end
            tag = t

            if tag == "head" then in_head = true
            elseif tag == "/head" then in_head = false
            elseif not in_head then
                if tag == "h1" then in_h1 = true
                elseif tag == "/h1" then in_h1 = false
                elseif tag == "br" then fields[#fields + 1] = { type = "spacer", width = 1, height = 1 }
                elseif tag == "a" then
                    in_a = true
                    a_href = nil
                    local href_pos = nil
                    for k = 1, #tag_content - 5 do
                        if string.sub(tag_content, k, k+4) == 'href=' then
                            href_pos = k + 5
                            break
                        end
                    end
                    if href_pos then
                        local quote_start = nil
                        if string.sub(tag_content, href_pos, href_pos) == '"' then
                            quote_start = href_pos + 1
                        elseif string.sub(tag_content, href_pos, href_pos) == "'" then
                            quote_start = href_pos + 1
                        end
                        if quote_start then
                            local quote_end = quote_start
                            while quote_end <= #tag_content and string.sub(tag_content, quote_end, quote_end) ~= '"' and string.sub(tag_content, quote_end, quote_end) ~= "'" do
                                quote_end = quote_end + 1
                            end
                            a_href = string.sub(tag_content, quote_start, quote_end - 1)
                        end
                    end
                elseif tag == "/a" then in_a = false end
            end

            i = j + 1
        else
            if not in_head then
                current_text = current_text .. c
            end
            i = i + 1
        end
    end

    add_field()

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

if #arg == 1 then browser.main()
else browser.load(arg[1]) end