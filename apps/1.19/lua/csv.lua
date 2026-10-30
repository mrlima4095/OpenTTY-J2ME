-- rock: name=csv
-- rock: version=1.0.0
-- rock: description=CSV parser and writer with quoting and header helpers
-- rock: here=/lib/csv.lua

-- RFC 4180-ish: fields separated by `sep`, quoted with `quote`, a doubled
-- quote escapes itself, and quoted fields may contain separators or newlines.
-- CRLF and LF line endings are both accepted.
--
--   local csv = require("csv")
--   local rows = csv.parse("name,age\nana,3\n")
--   local people = csv.parse_objects("name,age\nana,3\n")
--   print(csv.encode(rows))
local C = csv or {}

local function field_needs_quotes(text, sep, quote)
    if text == "" then return false end
    if string.sub(text, 1, 1) == " " or string.sub(text, -1) == " " then return true end
    if string.find(text, sep, 1, true) then return true end
    if string.find(text, quote, 1, true) then return true end
    if string.find(text, "\n", 1, true) or string.find(text, "\r", 1, true) then return true end
    return false
end

local function quote_field(text, quote)
    local out = { quote }
    for i = 1, #text do
        local c = string.sub(text, i, i)
        if c == quote then out[#out + 1] = quote .. quote
        else out[#out + 1] = c end
    end
    out[#out + 1] = quote
    return table.concat(out)
end

function C.parse(text, options)
    options = options or {}
    local sep = options.sep or ","
    local quote = options.quote or '"'
    text = tostring(text or "")

    local rows = {}
    local row = {}
    local field = {}
    local in_quotes = false
    local i = 1
    local length = #text

    local function push_field()
        row[#row + 1] = table.concat(field)
        field = {}
    end

    local function push_row()
        push_field()
        rows[#rows + 1] = row
        row = {}
    end

    while i <= length do
        local c = string.sub(text, i, i)
        if in_quotes then
            if c == quote then
                if string.sub(text, i + 1, i + 1) == quote then
                    field[#field + 1] = quote
                    i = i + 2
                else
                    in_quotes = false
                    i = i + 1
                end
            else
                field[#field + 1] = c
                i = i + 1
            end
        elseif c == quote and #field == 0 then
            in_quotes = true
            i = i + 1
        elseif c == sep then
            push_field()
            i = i + 1
        elseif c == "\n" then
            push_row()
            i = i + 1
        elseif c == "\r" then
            push_row()
            if string.sub(text, i + 1, i + 1) == "\n" then i = i + 2 else i = i + 1 end
        else
            field[#field + 1] = c
            i = i + 1
        end
    end

    if #field > 0 or #row > 0 then push_row() end
    if options.trim then
        for r = 1, #rows do
            for c = 1, #rows[r] do
                local value = rows[r][c]
                while #value > 0 and string.sub(value, 1, 1) == " " do value = string.sub(value, 2) end
                while #value > 0 and string.sub(value, -1) == " " do value = string.sub(value, 1, -2) end
                rows[r][c] = value
            end
        end
    end
    return rows
end

function C.encode(rows, options)
    options = options or {}
    local sep = options.sep or ","
    local quote = options.quote or '"'
    local newline = options.newline or "\n"

    local out = {}
    for r = 1, #rows do
        local row = rows[r]
        local cells = {}
        for c = 1, #row do
            local value = row[c]
            if value == nil then value = "" end
            value = tostring(value)
            if field_needs_quotes(value, sep, quote) then value = quote_field(value, quote) end
            cells[#cells + 1] = value
        end
        out[#out + 1] = table.concat(cells, sep)
    end
    if #out == 0 then return "" end
    return table.concat(out, newline) .. newline
end

function C.read(path, options)
    local text = io.read(path)
    if text == nil then return nil, "cannot read " .. tostring(path) end
    return C.parse(text, options)
end

function C.write(path, rows, options)
    local ok, err = pcall(io.write, C.encode(rows, options), path)
    if not ok then return false, tostring(err) end
    return true
end

function C.rows_to_objects(rows, options)
    options = options or {}
    local header_row = options.header_row or 1
    local header = rows[header_row]
    if header == nil then return {} end

    local out = {}
    for r = header_row + 1, #rows do
        local object = {}
        local row = rows[r]
        for c = 1, #header do
            object[header[c]] = row[c]
        end
        out[#out + 1] = object
    end
    return out
end

function C.parse_objects(text, options)
    return C.rows_to_objects(C.parse(text, options), options)
end

function C.objects_to_rows(list, headers)
    if headers == nil then
        headers = {}
        if list[1] ~= nil then
            for key in pairs(list[1]) do headers[#headers + 1] = key end
            table.sort(headers, function(a, b) return tostring(a) < tostring(b) end)
        end
    end

    local rows = { headers }
    for i = 1, #list do
        local object = list[i]
        local row = {}
        for c = 1, #headers do
            local value = object[headers[c]]
            if value == nil then value = "" end
            row[c] = tostring(value)
        end
        rows[#rows + 1] = row
    end
    return rows
end

return C
