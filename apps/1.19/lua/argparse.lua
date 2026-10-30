-- rock: name=argparse
-- rock: version=1.0.0
-- rock: description=Option parser with short/long flags, types, defaults and --help
-- rock: here=/lib/argparse.lua

-- Every /bin script hand-rolls the same `while arg[i] do` loop. This module
-- keeps the same spirit but adds `--name=value`, clustered short flags,
-- type coercion, defaults and a generated help text.
--
--   local argparse = require("argparse")
--   local p = argparse.new("greet", "Say hello")
--   p:option("-n", "--name", { type = "string", default = "world", help = "who to greet" })
--   p:flag("-v", "--verbose", { help = "print more" })
--   p:argument("file", { help = "optional file" })
--   local opts, err = p:parse(arg)
--   if not opts then print(err) print(p:help()) os.exit(2) end
local A = argparse or {}

local function replace_plain(text, find, repl)
    if find == "" then return text end
    local out = {}
    local pos = 1
    while true do
        local i = string.find(text, find, pos, true)
        if i == nil then break end
        out[#out + 1] = string.sub(text, pos, i - 1)
        out[#out + 1] = repl
        pos = i + #find
    end
    out[#out + 1] = string.sub(text, pos)
    return table.concat(out)
end

local function trim(text)
    text = tostring(text)
    while #text > 0 and string.sub(text, 1, 1) == " " do text = string.sub(text, 2) end
    while #text > 0 and string.sub(text, -1) == " " do text = string.sub(text, 1, -2) end
    return text
end

local function rep(text, count)
    local out = {}
    for i = 1, count do out[i] = text end
    return table.concat(out)
end

local function coerce(spec, value)
    local kind = spec.type or "string"
    if kind == "string" then return tostring(value) end
    if kind == "number" then return tonumber(value) end
    if kind == "int" or kind == "integer" then
        local number = tonumber(value)
        if number == nil then return nil end
        return number - number % 1
    end
    if kind == "boolean" then
        local text = string.lower(tostring(value))
        if text == "true" or text == "1" or text == "yes" or text == "on" then return true end
        if text == "false" or text == "0" or text == "no" or text == "off" then return false end
        return nil
    end
    return tostring(value)
end

local Parser = {}
Parser.__index = Parser

function A.new(name, description)
    return setmetatable({
        name = name or "program",
        description = description or "",
        options = {},
        by_name = {},
        order = {},
        positionals = {}
    }, Parser)
end

function Parser.option(self, short, long, spec)
    -- `option("--name", { ... })` is accepted as well as `option("-n", "--name", { ... })`.
    if type(long) == "table" and spec == nil then spec = long long = nil end
    spec = spec or {}

    local aliases = {}
    if short ~= nil then aliases[#aliases + 1] = short end
    if long ~= nil then aliases[#aliases + 1] = long end
    if #aliases == 0 then error("argparse: option needs a name") end

    local name = spec.name
    if name == nil then
        local primary = long or short
        if string.sub(primary, 1, 2) == "--" then name = string.sub(primary, 3)
        else name = string.sub(primary, 2) end
        name = replace_plain(name, "-", "_")
    end

    local option = {
        aliases = aliases,
        name = name,
        type = spec.type or "string",
        default = spec.default,
        help = spec.help or "",
        required = spec.required,
        value_name = spec.value_name
    }
    if option.type == "boolean" and option.default == nil then option.default = false end

    for i = 1, #aliases do self.options[aliases[i]] = option end
    self.by_name[name] = option
    self.order[#self.order + 1] = option
    return self
end

function Parser.flag(self, short, long, spec)
    if type(long) == "table" and spec == nil then spec = long long = nil end
    spec = spec or {}
    spec.type = "boolean"
    return Parser.option(self, short, long, spec)
end

function Parser.argument(self, name, spec)
    spec = spec or {}
    spec.name = name
    self.positionals[#self.positionals + 1] = spec
    return self
end

-- `-h/--help` is added on first use so it never shadows a user option.
local function install_help(self)
    if self.options["-h"] == nil and self.options["--help"] == nil then
        Parser.flag(self, "-h", "--help", { help = "show this help" })
    end
end

function Parser.parse(self, argv)
    install_help(self)
    argv = argv or arg or {}
    local result = { args = {} }

    for i = 1, #self.order do
        local option = self.order[i]
        if option.default ~= nil then result[option.name] = option.default
        elseif option.type == "boolean" then result[option.name] = false end
    end

    local positional_values = {}
    local index = 1
    local only_positional = false

    while index <= #argv do
        local token = tostring(argv[index])
        if only_positional then
            positional_values[#positional_values + 1] = token
        elseif token == "--" then
            only_positional = true
        elseif string.sub(token, 1, 2) == "--" then
            local eq = string.find(token, "=", 1, true)
            local key, inline
            if eq ~= nil then
                key = string.sub(token, 1, eq - 1)
                inline = string.sub(token, eq + 1)
            else key = token end

            local option = self.options[key]
            if option == nil then return nil, "unknown option " .. key end

            if option.type == "boolean" then
                if inline ~= nil then
                    result[option.name] = not (inline == "false" or inline == "0" or inline == "no")
                else result[option.name] = true end
            else
                local value = inline
                if value == nil then
                    index = index + 1
                    value = argv[index]
                    if value == nil then return nil, "option " .. key .. " needs a value" end
                end
                local converted = coerce(option, value)
                if converted == nil then return nil, "invalid value for " .. key .. ": " .. tostring(value) end
                result[option.name] = converted
            end
        elseif string.sub(token, 1, 1) == "-" and #token > 1 then
            local chars = string.sub(token, 2)
            for c = 1, #chars do
                local key = "-" .. string.sub(chars, c, c)
                local option = self.options[key]
                if option == nil then return nil, "unknown option " .. key end
                if option.type == "boolean" then
                    result[option.name] = true
                else
                    local value = string.sub(chars, c + 1)
                    if value == "" then
                        index = index + 1
                        value = argv[index]
                        if value == nil then return nil, "option " .. key .. " needs a value" end
                    end
                    local converted = coerce(option, value)
                    if converted == nil then return nil, "invalid value for " .. key .. ": " .. tostring(value) end
                    result[option.name] = converted
                    break
                end
            end
        else
            positional_values[#positional_values + 1] = token
        end
        index = index + 1
    end

    result.args = positional_values
    for p = 1, #self.positionals do
        local spec = self.positionals[p]
        local value = positional_values[p]
        if value == nil and spec.required then return nil, "missing argument " .. spec.name end
        result[spec.name] = value or spec.default
    end

    return result
end

function Parser.usage(self)
    local out = { "Usage: " .. self.name }
    if #self.order > 0 then out[#out + 1] = "[options]" end
    for i = 1, #self.positionals do
        local spec = self.positionals[i]
        if spec.required then out[#out + 1] = "<" .. spec.name .. ">"
        else out[#out + 1] = "[" .. spec.name .. "]" end
    end
    return table.concat(out, " ")
end

function Parser.help(self)
    install_help(self)
    local lines = { Parser.usage(self) }
    if self.description ~= "" then
        lines[#lines + 1] = ""
        lines[#lines + 1] = self.description
    end

    local rows = {}
    for i = 1, #self.order do
        local option = self.order[i]
        local names = table.concat(option.aliases, ", ")
        if option.type ~= "boolean" then
            local value_name = option.value_name or option.type or "value"
            names = names .. " <" .. value_name .. ">"
        end
        local text = option.help
        if option.default ~= nil then
            local shown = tostring(option.default)
            if shown ~= "" and shown ~= "false" then
                text = text .. " (default: " .. shown .. ")"
            end
        end
        rows[#rows + 1] = { names, text }
    end
    if #rows > 0 then
        local width = 0
        for i = 1, #rows do
            if #rows[i][1] > width then width = #rows[i][1] end
        end
        lines[#lines + 1] = ""
        lines[#lines + 1] = "Options:"
        for i = 1, #rows do
            local pad = width - #rows[i][1]
            lines[#lines + 1] = "  " .. rows[i][1] .. rep(" ", pad) .. "  " .. rows[i][2]
        end
    end

    return table.concat(lines, "\n")
end
return A
