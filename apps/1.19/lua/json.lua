-- rock: name=json
-- rock: version=1.0.0
-- rock: description=JSON encoder and decoder with arrays, objects and pretty printing
-- rock: here=/lib/json.lua

-- The runtime has no JSON support. This module is self-contained (it does not
-- require the `string` rock) and keeps insertion-free, deterministic output:
-- object keys are always sorted so two runs produce the same text.
local J = json or {}

-- JSON null has no Lua equivalent; a private sentinel keeps it distinguishable
-- from a missing key and survives a decode/encode round trip.
J.null = setmetatable({}, { __tostring = function() return "null" end })

local floor = function(x) return x - x % 1 end
local HEX = "0123456789abcdef"

local function hex2(b)
    local hi = floor(b / 16) % 16
    local lo = b % 16
    return string.sub(HEX, hi + 1, hi + 1) .. string.sub(HEX, lo + 1, lo + 1)
end

local function is_null(value)
    return value == J.null
end

local function quote(text)
    local out = { '"' }
    for i = 1, #text do
        local c = string.sub(text, i, i)
        local b = string.byte(text, i) or 0
        if c == '"' then out[#out + 1] = '\\"'
        elseif c == "\\" then out[#out + 1] = "\\\\"
        elseif c == "\n" then out[#out + 1] = "\\n"
        elseif c == "\r" then out[#out + 1] = "\\r"
        elseif c == "\t" then out[#out + 1] = "\\t"
        elseif c == "\b" then out[#out + 1] = "\\b"
        elseif c == "\f" then out[#out + 1] = "\\f"
        elseif b < 32 then out[#out + 1] = "\\u00" .. hex2(b)
        else out[#out + 1] = c end
    end
    out[#out + 1] = '"'
    return table.concat(out)
end

local function number_string(value)
    if value ~= value then error("json: cannot encode NaN") end
    if value == floor(value) and value < 9007199254740992 and value > -9007199254740992 then
        local text = tostring(value)
        local dot = string.find(text, ".", 1, true)
        if dot then text = string.sub(text, 1, dot - 1) end
        return text
    end
    local text = tostring(value)
    local dot = string.find(text, ".", 1, true)
    if dot then
        while string.sub(text, -1) == "0" do text = string.sub(text, 1, -2) end
        if string.sub(text, -1) == "." then text = string.sub(text, 1, -2) end
    end
    return text
end

-- A table is an array when every key is a positive integer and there is no
-- hole; anything else (including an empty table) is encoded as an object,
-- except that `{}` is a valid empty array and is treated as one.
local function is_array(t)
    local count, high = 0, 0
    for k in pairs(t) do
        if type(k) ~= "number" or k ~= floor(k) or k < 1 then return false end
        count = count + 1
        if k > high then high = k end
    end
    if count ~= high then return false end
    return true, high
end

local function sorted_keys(t)
    local keys = {}
    for k in pairs(t) do keys[#keys + 1] = k end
    table.sort(keys, function(a, b) return tostring(a) < tostring(b) end)
    return keys
end

local function indent_of(opts, depth)
    local out = ""
    for i = 1, depth * opts.indent do out = out .. " " end
    return out
end

local encode_value

encode_value = function(value, opts, depth, seen)
    if value == nil or is_null(value) then return "null" end
    local kind = type(value)
    if kind == "boolean" then return value and "true" or "false" end
    if kind == "number" then return number_string(value) end
    if kind == "string" then return quote(value) end
    if kind ~= "table" then return quote(tostring(value)) end
    if seen[value] then error("json: cycle detected") end
    seen[value] = true

    local array, n = is_array(value)
    local out = {}

    if array then
        if n == 0 then seen[value] = nil return "[]" end
        if not opts.pretty then
            for i = 1, n do out[i] = encode_value(value[i], opts, depth + 1, seen) end
        else
            local child = indent_of(opts, depth + 1)
            for i = 1, n do out[i] = child .. encode_value(value[i], opts, depth + 1, seen) end
        end
        seen[value] = nil
        if not opts.pretty then return "[" .. table.concat(out, ",") .. "]" end
        return "[\n" .. table.concat(out, ",\n") .. "\n" .. indent_of(opts, depth) .. "]"
    end

    local keys = sorted_keys(value)
    if #keys == 0 then seen[value] = nil return "{}" end
    for i = 1, #keys do
        local key = keys[i]
        local name = type(key) == "string" and key or tostring(key)
        local encoded = encode_value(value[key], opts, depth + 1, seen)
        if opts.pretty then
            out[i] = indent_of(opts, depth + 1) .. quote(name) .. ": " .. encoded
        else
            out[i] = quote(name) .. ":" .. encoded
        end
    end
    seen[value] = nil
    if not opts.pretty then return "{" .. table.concat(out, ",") .. "}" end
    return "{\n" .. table.concat(out, ",\n") .. "\n" .. indent_of(opts, depth) .. "}"
end

local function utf8_char(cp)
    if cp < 128 then return string.char(cp) end
    if cp < 2048 then
        return string.char(192 + floor(cp / 64), 128 + cp % 64)
    end
    if cp < 65536 then
        return string.char(224 + floor(cp / 4096), 128 + floor(cp / 64) % 64, 128 + cp % 64)
    end
    return string.char(240 + floor(cp / 262144), 128 + floor(cp / 4096) % 64,
        128 + floor(cp / 64) % 64, 128 + cp % 64)
end

function J.encode(value, options)
    local opts = { pretty = false, indent = 2 }
    if options == true then opts.pretty = true
    elseif type(options) == "table" then
        if options.pretty ~= nil then opts.pretty = options.pretty end
        if options.indent ~= nil then opts.indent = options.indent end
    end
    return encode_value(value, opts, 0, {})
end

function J.pretty(value, indent)
    return J.encode(value, { pretty = true, indent = indent or 2 })
end

function J.decode(text)
    text = tostring(text)
    local pos, len = 1, #text

    local function fail(message) error("json: " .. message .. " at " .. tostring(pos)) end
    local function peek() return string.sub(text, pos, pos) end
    local function skip()
        while pos <= len do
            local c = peek()
            if c == " " or c == "\t" or c == "\n" or c == "\r" then pos = pos + 1
            else break end
        end
    end

    local parse_string, parse_value

    parse_string = function()
        pos = pos + 1
        local out = {}
        while pos <= len do
            local c = string.sub(text, pos, pos)
            if c == '"' then pos = pos + 1 return table.concat(out) end
            if c == "\\" then
                pos = pos + 1
                local esc = string.sub(text, pos, pos)
                if esc == '"' then out[#out + 1] = '"'
                elseif esc == "\\" then out[#out + 1] = "\\"
                elseif esc == "/" then out[#out + 1] = "/"
                elseif esc == "b" then out[#out + 1] = "\b"
                elseif esc == "f" then out[#out + 1] = "\f"
                elseif esc == "n" then out[#out + 1] = "\n"
                elseif esc == "r" then out[#out + 1] = "\r"
                elseif esc == "t" then out[#out + 1] = "\t"
                elseif esc == "u" then
                    local cp = tonumber(string.sub(text, pos + 1, pos + 4), 16)
                    if cp == nil then fail("bad \\u escape") end
                    pos = pos + 4
                    if cp >= 55296 and cp <= 56319 and string.sub(text, pos + 1, pos + 2) == "\\u" then
                        local low = tonumber(string.sub(text, pos + 3, pos + 6), 16)
                        if low ~= nil and low >= 56320 and low <= 57343 then
                            cp = 65536 + (cp - 55296) * 1024 + (low - 56320)
                            pos = pos + 6
                        end
                    end
                    out[#out + 1] = utf8_char(cp)
                else fail("bad escape '" .. esc .. "'") end
                pos = pos + 1
            else
                out[#out + 1] = c
                pos = pos + 1
            end
        end
        fail("unterminated string")
    end

    local function parse_number()
        local start = pos
        while pos <= len do
            local c = string.sub(text, pos, pos)
            if (c >= "0" and c <= "9") or c == "-" or c == "+" or c == "." or c == "e" or c == "E" then
                pos = pos + 1
            else break end
        end
        local value = tonumber(string.sub(text, start, pos - 1))
        if value == nil then pos = start fail("bad number") end
        return value
    end

    parse_value = function()
        skip()
        local c = peek()
        if c == "" then fail("unexpected end of input") end
        if c == "{" then
            pos = pos + 1
            local obj = {}
            skip()
            if peek() == "}" then pos = pos + 1 return obj end
            while true do
                skip()
                if peek() ~= '"' then fail("expected object key") end
                local key = parse_string()
                skip()
                if peek() ~= ":" then fail("expected ':'") end
                pos = pos + 1
                obj[key] = parse_value()
                skip()
                local next_char = peek()
                if next_char == "," then pos = pos + 1
                elseif next_char == "}" then pos = pos + 1 break
                else fail("expected ',' or '}'") end
            end
            return obj
        elseif c == "[" then
            pos = pos + 1
            local arr = {}
            skip()
            if peek() == "]" then pos = pos + 1 return arr end
            while true do
                arr[#arr + 1] = parse_value()
                skip()
                local next_char = peek()
                if next_char == "," then pos = pos + 1
                elseif next_char == "]" then pos = pos + 1 break
                else fail("expected ',' or ']'") end
            end
            return arr
        elseif c == '"' then
            return parse_string()
        elseif c == "t" then
            if string.sub(text, pos, pos + 3) == "true" then pos = pos + 4 return true end
            fail("invalid literal")
        elseif c == "f" then
            if string.sub(text, pos, pos + 4) == "false" then pos = pos + 5 return false end
            fail("invalid literal")
        elseif c == "n" then
            if string.sub(text, pos, pos + 3) == "null" then pos = pos + 4 return J.null end
            fail("invalid literal")
        end
        return parse_number()
    end

    local value = parse_value()
    skip()
    if pos <= len then fail("trailing characters") end
    return value
end

function J.try_decode(text)
    local ok, value = pcall(J.decode, text)
    if ok then return value end
    return nil, tostring(value)
end

J.is_null = is_null
J.quote = quote

return J
