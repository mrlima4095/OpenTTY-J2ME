-- rock: name=graphics
-- rock: version=1.1.0
-- rock: description=Markdown and HTML viewers built on the LCDUI graphics package
-- rock: here=/lib/graphics.lua

-- The runtime ships `graphics.new/append/display/...`; this module adds
-- document viewers on top of them without replacing the native functions.
local G = graphics or {}

local function replace_plain(s, find, repl)
    if find == "" then return s end
    local out = {}
    local pos = 1
    while true do
        local i = string.find(s, find, pos)
        if not i then break end
        out[#out + 1] = string.sub(s, pos, i - 1)
        out[#out + 1] = repl
        pos = i + #find
    end
    out[#out + 1] = string.sub(s, pos)
    return table.concat(out)
end

local function split_lines(text)
    local lines = {}
    local pos = 1
    if #text == 0 then lines[1] = "" return lines end
    while pos <= #text do
        local nl = string.find(text, "\n", pos)
        if not nl then
            lines[#lines + 1] = string.sub(text, pos)
            break
        end
        lines[#lines + 1] = string.sub(text, pos, nl - 1)
        pos = nl + 1
    end
    return lines
end

-- Drop the inline markup the small LCDUI items cannot render.
local function strip_inline(s)
    s = replace_plain(s, "**", "")
    s = replace_plain(s, "`", "")
    local out = {}
    local i = 1
    while i <= #s do
        local open = string.find(s, "[", i)
        if not open then
            out[#out + 1] = string.sub(s, i)
            break
        end
        local mid = string.find(s, "](", open + 1)
        local close = mid and string.find(s, ")", mid + 2)
        if mid and close then
            out[#out + 1] = string.sub(s, i, open - 1)
            out[#out + 1] = string.sub(s, open + 1, mid - 1)
            i = close + 1
        else
            out[#out + 1] = string.sub(s, i, open)
            i = open + 1
        end
    end
    s = table.concat(out)
    s = replace_plain(s, "*", "")
    return s
end

local function add_block(screen, text, style)
    text = string.trim(text)
    if text == "" then return end
    local item = G.new("buffer", { label = "", value = text, style = style or "default" })
    G.append(screen, item)
end

local function add_commands(screen, previous)
    local back = G.new("command", { label = "Back", type = "back", priority = 1 })
    G.addCommand(screen, back)
    G.handler(screen, {
        [back] = function()
            if previous then G.display(previous) else os.exit(0) end
        end
    })
end

local function show_document(lines, title)
    local previous = G.getCurrent()
    local screen = G.new("screen", title)

    local paragraph = {}
    local code = {}
    local in_code = false

    local function flush_paragraph()
        if #paragraph == 0 then return end
        add_block(screen, table.concat(paragraph, " "), "default")
        paragraph = {}
    end

    local function flush_code()
        if #code == 0 then return end
        add_block(screen, table.concat(code, "\n"), "monospace")
        code = {}
    end

    for i = 1, #lines do
        local line = lines[i]
        local trimmed = string.trim(line)

        if string.startswith(trimmed, "```") then
            if in_code then
                flush_code()
                in_code = false
            else
                flush_paragraph()
                in_code = true
            end
        elseif in_code then
            code[#code + 1] = line
        elseif trimmed == "" then
            flush_paragraph()
        elseif string.startswith(trimmed, "#") then
            flush_paragraph()
            local level = 0
            while string.sub(trimmed, level + 1, level + 1) == "#" do level = level + 1 end
            local heading = string.trim(string.sub(trimmed, level + 1))
            local style = level <= 1 and "bold large" or "bold"
            add_block(screen, strip_inline(heading), style)
        elseif string.startswith(trimmed, "- ") or string.startswith(trimmed, "* ") or string.startswith(trimmed, "+ ") then
            flush_paragraph()
            add_block(screen, "• " .. strip_inline(string.sub(trimmed, 3)), "default")
        elseif string.startswith(trimmed, "> ") then
            flush_paragraph()
            add_block(screen, "| " .. strip_inline(string.sub(trimmed, 3)), "italic")
        elseif trimmed == "---" or trimmed == "***" or trimmed == "___" then
            flush_paragraph()
            add_block(screen, "────────────────", "default")
        else
            paragraph[#paragraph + 1] = strip_inline(trimmed)
        end
    end

    flush_paragraph()
    flush_code()

    add_commands(screen, previous)
    os.setproc("screen", screen)
    G.display(screen)
    return screen
end

function G.RenderMarkdown(markdown, title)
    markdown = markdown or ""
    return show_document(split_lines(markdown), title or "Markdown")
end

local function strip_tags(html)
    local out = {}
    local i = 1
    while i <= #html do
        local open = string.find(html, "<", i)
        if not open then
            out[#out + 1] = string.sub(html, i)
            break
        end
        local close = string.find(html, ">", open + 1)
        if not close then
            out[#out + 1] = string.sub(html, i)
            break
        end
        out[#out + 1] = string.sub(html, i, open - 1)
        i = close + 1
    end
    return table.concat(out)
end

local function hex_value(s)
    local value = 0
    s = string.lower(s)
    for i = 1, #s do
        local c = string.sub(s, i, i)
        local d = string.byte(c)
        local digit
        if d >= 48 and d <= 57 then digit = d - 48
        elseif d >= 97 and d <= 102 then digit = d - 87
        else return nil end
        value = value * 16 + digit
    end
    return value
end

local function decode_entities(text)
    text = replace_plain(text, "&amp;", "&")
    text = replace_plain(text, "&lt;", "<")
    text = replace_plain(text, "&gt;", ">")
    text = replace_plain(text, "&quot;", '"')
    text = replace_plain(text, "&apos;", "'")
    text = replace_plain(text, "&#39;", "'")
    text = replace_plain(text, "&nbsp;", " ")

    local out = {}
    local i = 1
    while i <= #text do
        local open = string.find(text, "&#", i)
        if not open then
            out[#out + 1] = string.sub(text, i)
            break
        end
        local close = string.find(text, ";", open + 2)
        if not close then
            out[#out + 1] = string.sub(text, i)
            break
        end
        out[#out + 1] = string.sub(text, i, open - 1)
        local body = string.sub(text, open + 2, close - 1)
        local value
        if string.sub(body, 1, 1) == "x" or string.sub(body, 1, 1) == "X" then
            value = hex_value(string.sub(body, 2))
        else
            value = tonumber(body)
        end
        if value and value >= 0 and value < 1114112 then out[#out + 1] = string.char(value) end
        i = close + 1
    end
    return table.concat(out)
end

local function html_to_text(html)
    html = replace_plain(html, "\r\n", "\n")
    html = replace_plain(html, "\r", "\n")
    local blocks = {
        "<br>", "<br/>", "<br />", "</p>", "</div>", "</li>", "</tr>",
        "</h1>", "</h2>", "</h3>", "</h4>", "</h5>", "</h6>",
        "<p>", "<div>", "<tr>", "<table>", "</table>", "<ul>", "</ul>", "<ol>", "</ol>"
    }
    for i = 1, #blocks do html = replace_plain(html, blocks[i], "\n") end
    html = replace_plain(html, "<li>", "• ")
    html = strip_tags(html)
    html = decode_entities(html)
    return html
end

function G.RenderHTML(html, title)
    html = html or ""
    return show_document(split_lines(html_to_text(html)), title or "HTML")
end

-- Re-present the current screen (useful after replacing its items).
function G.Reload()
    local current = G.getCurrent()
    if current then G.display(current) end
    return current
end

-- Render a file by extension: .html/.htm use the HTML viewer, anything else
-- is treated as Markdown.
function G.Open(path, title)
    local text = io.read(path)
    if text == nil then return nil end
    local lower = string.lower(path)
    if string.endswith(lower, ".html") or string.endswith(lower, ".htm") then
        return G.RenderHTML(text, title or path)
    end
    return G.RenderMarkdown(text, title or path)
end

return G
