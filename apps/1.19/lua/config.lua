-- rock: name=config
-- rock: version=1.0.0
-- rock: description=INI configuration: parse, encode, load/save and dotted keys
-- rock: here=/lib/config.lua

-- Configuration files are plain INI text:
--
--   ; comment
--   theme = dark
--
--   [network]
--   host = localhost
--   port = 8080
--
-- `get("network.port")` walks the dotted path, `set` creates missing sections
-- and `save` writes the file back in a stable (sorted) order.
local C = config or {}

local Config = {}
Config.__index = Config

local function trim(text)
    text = tostring(text)
    while #text > 0 and string.sub(text, 1, 1) == " " do text = string.sub(text, 2) end
    while #text > 0 and string.sub(text, -1) == " " do text = string.sub(text, 1, -2) end
    while #text > 0 and string.sub(text, 1, 1) == "\t" do text = string.sub(text, 2) end
    while #text > 0 and string.sub(text, -1) == "\t" do text = string.sub(text, 1, -2) end
    return text
end

local function split_dots(path)
    local parts = {}
    local pos = 1
    path = tostring(path)
    while true do
        local dot = string.find(path, ".", pos, true)
        if dot == nil then
            parts[#parts + 1] = string.sub(path, pos)
            break
        end
        parts[#parts + 1] = string.sub(path, pos, dot - 1)
        pos = dot + 1
    end
    return parts
end

local function int_text(value)
    local text = tostring(value)
    local dot = string.find(text, ".", 1, true)
    if dot then text = string.sub(text, 1, dot - 1) end
    return text
end

local function unquote(value)
    if #value >= 2 then
        local first, last = string.sub(value, 1, 1), string.sub(value, -1)
        if (first == '"' and last == '"') or (first == "'" and last == "'") then
            return string.sub(value, 2, -2)
        end
    end
    return value
end

local function quote(value)
    local text = tostring(value)
    local needs = text == ""
    if not needs then
        local first, last = string.sub(text, 1, 1), string.sub(text, -1)
        if first == " " or last == " " or first == "\t" or last == "\t" then needs = true end
        if string.find(text, "#", 1, true) or string.find(text, ";", 1, true) or
           string.find(text, "=", 1, true) or string.find(text, "\n", 1, true) then
            needs = true
        end
    end
    if not needs then return text end

    local out = { '"' }
    for i = 1, #text do
        local c = string.sub(text, i, i)
        if c == '"' then out[#out + 1] = '\\"'
        elseif c == "\\" then out[#out + 1] = "\\\\"
        elseif c == "\n" then out[#out + 1] = "\\n"
        elseif c == "\t" then out[#out + 1] = "\\t"
        else out[#out + 1] = c end
    end
    out[#out + 1] = '"'
    return table.concat(out)
end

local function value_text(value)
    local kind = type(value)
    if kind == "boolean" then return value and "true" or "false" end
    if kind == "number" then return int_text(value) end
    return quote(value)
end

function C.new(values)
    return setmetatable({ values = values or {} }, Config)
end

function Config.parse(self, text)
    text = tostring(text or "")
    local section = nil
    local line_no = 0
    local pos = 1

    while pos <= #text do
        local nl = string.find(text, "\n", pos, true)
        local line
        if nl == nil then
            line = string.sub(text, pos)
            pos = #text + 1
        else
            line = string.sub(text, pos, nl - 1)
            pos = nl + 1
        end
        line_no = line_no + 1
        if string.sub(line, -1) == "\r" then line = string.sub(line, 1, -2) end
        line = trim(line)

        if line ~= "" and string.sub(line, 1, 1) ~= ";" and string.sub(line, 1, 1) ~= "#" then
            if string.sub(line, 1, 1) == "[" and string.sub(line, -1) == "]" then
                section = trim(string.sub(line, 2, -2))
                if section == "" then
                    error("config: empty section name at line " .. line_no)
                end
                if type(self.values[section]) ~= "table" then self.values[section] = {} end
            else
                local eq = string.find(line, "=", 1, true)
                if eq == nil then
                    error("config: expected key = value at line " .. line_no)
                end
                local key = trim(string.sub(line, 1, eq - 1))
                local value = trim(string.sub(line, eq + 1))
                if key == "" then
                    error("config: empty key at line " .. line_no)
                end
                local target = section ~= nil and self.values[section] or self.values
                target[key] = unquote(value)
            end
        end
    end

    return self
end

-- `config.parse(text)` builds a Config and parses it in one call.
function C.parse(text)
    return C.new():parse(text)
end

function Config.get(self, path, default)
    local parts = split_dots(path)
    local node = self.values
    for i = 1, #parts do
        if type(node) ~= "table" then return default end
        node = node[parts[i]]
        if node == nil then return default end
    end
    return node
end

local MISSING = {}

function Config.has(self, path)
    return Config.get(self, path, MISSING) ~= MISSING
end

function Config.set(self, path, value)
    local parts = split_dots(path)
    local node = self.values
    for i = 1, #parts - 1 do
        local key = parts[i]
        if type(node[key]) ~= "table" then node[key] = {} end
        node = node[key]
    end
    node[parts[#parts]] = value
    return self
end

function Config.unset(self, path)
    local parts = split_dots(path)
    local node = self.values
    for i = 1, #parts - 1 do
        if type(node) ~= "table" then return false end
        node = node[parts[i]]
    end
    if type(node) ~= "table" or node[parts[#parts]] == nil then return false end
    node[parts[#parts]] = nil
    return true
end

function Config.section(self, name)
    if type(self.values[name]) ~= "table" then self.values[name] = {} end
    return self.values[name]
end

function Config.sections(self)
    local out = {}
    for key, value in pairs(self.values) do
        if type(value) == "table" then out[#out + 1] = key end
    end
    table.sort(out)
    return out
end

function Config.keys(self, name)
    local source = name == nil and self.values or self.values[name]
    local out = {}
    if type(source) ~= "table" then return out end
    for key in pairs(source) do out[#out + 1] = key end
    table.sort(out, function(a, b) return tostring(a) < tostring(b) end)
    return out
end

function Config.get_number(self, path, default)
    local value = Config.get(self, path, nil)
    if value == nil then return default end
    local number = tonumber(value)
    if number == nil then return default end
    return number
end

function Config.get_bool(self, path, default)
    local value = Config.get(self, path, nil)
    if value == nil then return default end
    if type(value) == "boolean" then return value end
    local text = string.lower(tostring(value))
    if text == "true" or text == "yes" or text == "on" or text == "1" then return true end
    if text == "false" or text == "no" or text == "off" or text == "0" then return false end
    return default
end

function Config.get_list(self, path, sep)
    local value = Config.get(self, path, nil)
    if value == nil then return {} end
    if type(value) == "table" then return value end
    sep = sep or ","
    local out = {}
    local text = tostring(value)
    local pos = 1
    while true do
        local at = string.find(text, sep, pos, true)
        if at == nil then
            out[#out + 1] = trim(string.sub(text, pos))
            break
        end
        out[#out + 1] = trim(string.sub(text, pos, at - 1))
        pos = at + #sep
    end
    return out
end

function C.encode(values)
    values = values or {}
    local globals, sections = {}, {}
    for key, value in pairs(values) do
        if type(value) == "table" then sections[#sections + 1] = key
        else globals[#globals + 1] = key end
    end
    table.sort(globals, function(a, b) return tostring(a) < tostring(b) end)
    table.sort(sections, function(a, b) return tostring(a) < tostring(b) end)

    local out = {}
    for i = 1, #globals do
        local key = globals[i]
        out[#out + 1] = tostring(key) .. " = " .. value_text(values[key])
    end
    for i = 1, #sections do
        local name = sections[i]
        if #out > 0 then out[#out + 1] = "" end
        out[#out + 1] = "[" .. tostring(name) .. "]"
        local keys = {}
        for key in pairs(values[name]) do keys[#keys + 1] = key end
        table.sort(keys, function(a, b) return tostring(a) < tostring(b) end)
        for j = 1, #keys do
            out[#out + 1] = tostring(keys[j]) .. " = " .. value_text(values[name][keys[j]])
        end
    end

    if #out == 0 then return "" end
    return table.concat(out, "\n") .. "\n"
end

function Config.encode(self)
    return C.encode(self.values)
end

function C.load(path)
    local text = io.read(path)
    if text == nil then return nil, "cannot read " .. tostring(path) end
    local ok, cfg = pcall(C.parse, text)
    if not ok then return nil, tostring(cfg) end
    return cfg
end

function Config.save(self, path)
    local ok, err = pcall(io.write, Config.encode(self), path)
    if not ok then return false, tostring(err) end
    return true
end

function C.merge(a, b)
    local out = C.new()
    for key, value in pairs(a.values or a) do
        if type(value) == "table" then
            local section = {}
            for k, v in pairs(value) do section[k] = v end
            out.values[key] = section
        else out.values[key] = value end
    end
    for key, value in pairs(b.values or b) do
        if type(value) == "table" then
            if type(out.values[key]) ~= "table" then out.values[key] = {} end
            for k, v in pairs(value) do out.values[key][k] = v end
        else out.values[key] = value end
    end
    return out
end

return C
