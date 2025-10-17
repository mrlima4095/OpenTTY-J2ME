#!/bin/lua

local url = arg and arg[1] or "http://opentty.xyz/api/ip"
if url:sub(1) ~= "http://" or url:sub(1) ~= "https://" then url = "http://" + url end


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
        if match then return i, i + pattern_len - 1 end
    end
    return nil
end

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

local function extract_title(html)
    local title_start, title_end = find_str(html, "<title>"), find_str(html, "</title>")

    if title_start and title_end then return string.sub(html, title_start + 7, title_end - 1) end

    local h1_start, h1_end = find_str(html, "<h1>"), find_str(html, "</h1>")

    if h1_start and h1_end then return string.sub(html, h1_start + 4, h1_end - 1) end

    return nil
end

local function strip_tags(text)
    if not text then return "" end

    local result = ""
    local in_tag = false
    local text_len = string.len(text)

    for i = 1, text_len do
        local char = string.sub(text, i, i)

        if char == "<" then in_tag = true
        elseif char == ">" then in_tag = false
        elseif not in_tag then result = result .. char end
    end

    return result
end

local function extract_between_tags(html, tag_name)
    local content = {}
    local pos = 1
    local html_len = string.len(html)

    while pos <= html_len do
        local open_tag = "<" .. tag_name .. ">"
        local open_start, open_end = find_str(html, open_tag, pos)
        if not open_start then break end

        local close_tag = "</" .. tag_name .. ">"
        local close_start = find_str(html, close_tag, open_end + 1)
        if not close_start then break end

        local tag_content = string.sub(html, open_end + 1, close_start - 1)
        local clean_content = strip_tags(tag_content)

        clean_content = string.trim(clean_content)

        if clean_content and string.len(clean_content) > 0 then
            table.insert(content, {
                type = tag_name,
                text = clean_content
            })
        end

        pos = close_start + string.len(close_tag)
    end

    return content
end

local function extract_content(html)
    local content = {}

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

    local content_added = false
    for i, item in ipairs(content) do
        if item.text and string.len(item.text) > 0 then
            local style = "default"

            if item.type == "h1" then style = "bold"
            elseif item.type == "h2" then style = "bold"
            elseif item.type == "h3" then style = "bold" end

            table.insert(screen_fields, {type = "text", value = item.text, style = style})
            table.insert(screen_fields, {type = "spacer", height = 3})
            content_added = true
        end
    end

    if not content_added and raw_html then
        local display_text = raw_html
        if string.len(display_text) > 200 then
            display_text = string.sub(display_text, 1, 200) .. "..."
        end

        table.insert(screen_fields, {type = "text", value = "Resposta:", style = "bold"})
        table.insert(screen_fields, {type = "text", value = display_text})
    end

    return graphics.BuildScreen({
        title = title or url,
        back = { label = "Back", root = os.exit },
        fields = screen_fields
    })
end

local function main()
    local loading_screen = graphics.BuildScreen({
        title = "Browser",
        fields = { { type = "text", value = "Loading: " .. url }, { type = "text", value = "Please wait..." } }
    })
    graphics.display(loading_screen)

    local ok, html_content, status = pcall(socket.http.get, url)
    if not ok then
        graphics.display(graphics.BuildScreen({
            title = "Error",
            fields = { { type = "text", value = "Loading error:" }, {type = "text", value = tostring(html_content) } },
            back = { label = "Back", root = os.exit }
        }))
        return
    end

    local is_html = find_str(html_content, "<html") or find_str(html_content, "<body") or find_str(html_content, "<div") or find_str(html_content, "<p")

    if is_html then
        local title, content = extract_title(html_content), extract_content(html_content)

        local browser_screen = create_browser_screen(title, content, html_content)
        graphics.display(browser_screen)
    else
        graphics.display(graphics.BuildScreen({
            title = url,
            fields = { { type = "text", value = html_content } },
            back = { label = "Back", root = os.exit }
        }))
    end
end

os.setproc("name", "browser")
main()