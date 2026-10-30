-- rock: name=term
-- rock: version=1.0.0
-- rock: description=Terminal helpers: ANSI colors, tables, progress bars and boxes
-- rock: here=/lib/term.lua

-- The xterm prints plain text; these helpers make CLI output readable without
-- replacing anything in the runtime. All width/padding math ignores ANSI
-- escape sequences, so colored cells still line up.
local T = term or {}

local ESC = string.char(27)
local floor = function(x) return x - x % 1 end

local FG = {
    black = 30, red = 31, green = 32, yellow = 33, blue = 34, magenta = 35, cyan = 36, white = 37,
    gray = 90, grey = 90, bright_red = 91, bright_green = 92, bright_yellow = 93,
    bright_blue = 94, bright_magenta = 95, bright_cyan = 96, bright_white = 97
}

local BG = {
    black = 40, red = 41, green = 42, yellow = 43, blue = 44, magenta = 45, cyan = 46, white = 47,
    gray = 100, grey = 100, bright_red = 101, bright_green = 102, bright_yellow = 103,
    bright_blue = 104, bright_magenta = 105, bright_cyan = 106, bright_white = 107
}

local STYLE = {
    bold = 1, dim = 2, italic = 3, underline = 4, blink = 5, reverse = 7, hidden = 8, strike = 9
}

local function sgr(code) return ESC .. "[" .. tostring(code) .. "m" end
local function reset() return ESC .. "[0m" end

-- `tostring(30.0)` prints "30.0" on stock Lua and "30" on the runtime.
local function int_text(value)
    local text = tostring(value)
    local dot = string.find(text, ".", 1, true)
    if dot then text = string.sub(text, 1, dot - 1) end
    return text
end

local function rep(text, count)
    local out = {}
    for i = 1, count do out[i] = text end
    return table.concat(out)
end

function T.fg(name)
    local code = FG[string.lower(tostring(name))]
    if code == nil then return "" end
    return sgr(code)
end

function T.bg(name)
    local code = BG[string.lower(tostring(name))]
    if code == nil then return "" end
    return sgr(code)
end

function T.style(name)
    local code = STYLE[string.lower(tostring(name))]
    if code == nil then return "" end
    return sgr(code)
end

function T.reset() return reset() end

function T.color(name, text)
    return T.fg(name) .. tostring(text) .. reset()
end

function T.background(name, text)
    return T.bg(name) .. tostring(text) .. reset()
end

function T.styled(name, text)
    return T.style(name) .. tostring(text) .. reset()
end

function T.bold(text) return sgr(1) .. tostring(text) .. reset() end
function T.dim(text) return sgr(2) .. tostring(text) .. reset() end
function T.italic(text) return sgr(3) .. tostring(text) .. reset() end
function T.underline(text) return sgr(4) .. tostring(text) .. reset() end
function T.reverse(text) return sgr(7) .. tostring(text) .. reset() end

function T.strip(text)
    text = tostring(text)
    local out = {}
    local i = 1
    while i <= #text do
        local c = string.sub(text, i, i)
        if c == ESC and string.sub(text, i + 1, i + 1) == "[" then
            local j = i + 2
            while j <= #text do
                local d = string.sub(text, j, j)
                if (d >= "0" and d <= "9") or d == ";" then j = j + 1
                else break end
            end
            if string.sub(text, j, j) == "m" then i = j + 1
            else out[#out + 1] = c i = i + 1 end
        else
            out[#out + 1] = c
            i = i + 1
        end
    end
    return table.concat(out)
end

function T.width(text)
    local plain = T.strip(text)
    return #plain
end

function T.pad(text, width, align)
    text = tostring(text)
    local visible = T.width(text)
    if visible >= width then return text end
    local missing = width - visible
    if align == "right" then return rep(" ", missing) .. text end
    if align == "center" then
        local left = floor(missing / 2)
        return rep(" ", left) .. text .. rep(" ", missing - left)
    end
    return text .. rep(" ", missing)
end

function T.table(rows, opts)
    opts = opts or {}
    local columns = 0
    for r = 1, #rows do
        if #rows[r] > columns then columns = #rows[r] end
    end
    if columns == 0 then return "" end

    local widths = {}
    for c = 1, columns do widths[c] = 0 end
    for r = 1, #rows do
        for c = 1, columns do
            local cell = rows[r][c]
            local w = T.width(cell == nil and "" or cell)
            if w > widths[c] then widths[c] = w end
        end
    end

    local border = opts.border and true or false
    local sep = opts.sep or (border and " | " or "  ")

    local function render_row(row, header)
        local cells = {}
        for c = 1, columns do
            local text = row[c]
            if text == nil then text = "" end
            text = tostring(text)
            if header then text = T.bold(text) end
            local align = opts.align
            if type(align) == "table" then align = align[c] end
            cells[c] = T.pad(text, widths[c], align)
        end
        return table.concat(cells, sep)
    end

    local out = {}
    local total = #sep * (columns - 1)
    for c = 1, columns do total = total + widths[c] end
    local line = "+" .. rep("-", total + 2) .. "+"

    if border then out[#out + 1] = line end
    for r = 1, #rows do
        local header = opts.header and r == 1
        local text = render_row(rows[r], header)
        if border then out[#out + 1] = "| " .. text .. " |"
        else out[#out + 1] = text end
        if border and header then out[#out + 1] = line end
    end
    if border then out[#out + 1] = line end

    return table.concat(out, "\n")
end

function T.bar(value, max, width, opts)
    opts = opts or {}
    width = width or 20
    max = max or 100
    local ratio = 0
    if max > 0 then ratio = value / max end
    if ratio < 0 then ratio = 0 end
    if ratio > 1 then ratio = 1 end
    local filled = floor(ratio * width + 0.5)
    local fill, empty = opts.fill or "#", opts.empty or " "
    local body = rep(fill, filled) .. rep(empty, width - filled)
    if opts.brackets == false then return body end
    local text = "[" .. body .. "]"
    if opts.percent == false then return text end
    return text .. " " .. T.pad(int_text(floor(ratio * 100 + 0.5)) .. "%", 4, "right")
end

function T.box(title, lines, opts)
    opts = opts or {}
    local width = T.width(title or "")
    for i = 1, #lines do
        local w = T.width(lines[i])
        if w > width then width = w end
    end
    width = width + 2
    local out = { "+" .. rep("-", width) .. "+" }
    if title ~= nil and title ~= "" then
        out[#out + 1] = "| " .. T.pad(title, width - 2) .. " |"
        out[#out + 1] = "+" .. rep("-", width) .. "+"
    end
    for i = 1, #lines do out[#out + 1] = "| " .. T.pad(lines[i], width - 2) .. " |" end
    out[#out + 1] = "+" .. rep("-", width) .. "+"
    return table.concat(out, "\n")
end

function T.hr(width, char)
    return rep(char or "-", width or 40)
end

function T.list(items, opts)
    opts = opts or {}
    local bullet = opts.bullet or "- "
    local out = {}
    for i = 1, #items do out[i] = bullet .. tostring(items[i]) end
    return table.concat(out, "\n")
end

function T.keyvalue(pairs, opts)
    opts = opts or {}
    local rows = {}
    for i = 1, #pairs do
        rows[i] = { tostring(pairs[i][1]) .. ":", tostring(pairs[i][2]) }
    end
    opts.sep = opts.sep or " "
    return T.table(rows, opts)
end

function T.wrap(text, width)
    width = width or 40
    local words = {}
    local pos = 1
    text = tostring(text)
    while pos <= #text do
        local space = string.find(text, " ", pos, true)
        if space == nil then
            words[#words + 1] = string.sub(text, pos)
            break
        end
        words[#words + 1] = string.sub(text, pos, space - 1)
        pos = space + 1
    end
    local lines, current = {}, ""
    for i = 1, #words do
        local word = words[i]
        if current == "" then current = word
        elseif #current + 1 + #word <= width then current = current .. " " .. word
        else lines[#lines + 1] = current current = word end
    end
    if current ~= "" then lines[#lines + 1] = current end
    return lines
end

function T.spinner(index)
    local frames = { "|", "/", "-", "\\" }
    return frames[(index or 0) % #frames + 1]
end

return T
