-- rock: name=re
-- rock: version=1.0.0
-- rock: description=Pattern helpers: findall, matchall, split, replace, escape, count
-- rock: here=/lib/re.lua
-- rock: depends=string

-- Lua patterns are already powerful; this module adds the convenience calls
-- most scripts reimplement (findall, split, replace, escape) on top of the
-- `string` rock, which provides find/match/gsub/gmatch with a full pattern
-- engine.
--
--   local re = require("re")
--   print(re.replace("a1 b2", "%d", "#"))     --> a# b#
--   for _, word in ipairs(re.findall("a1 b2", "%a+")) do print(word) end
local R = re or {}

local S = string
local ok, loaded = pcall(require, "string")
if ok and type(loaded) == "table" then S = loaded end

local function captures_of(...)
    -- The runtime stores `...` in one table; stock Lua keeps real varargs.
    local first = ...
    local total = select('#', ...)
    if type(first) == "table" and total == #first then return first end
    local out = {}
    for i = 1, total do out[i] = select(i, ...) end
    return out
end

local function require_gmatch()
    if type(S.gmatch) ~= "function" then
        error("re: the string rock is required (run `luarocks install string`)")
    end
    return S.gmatch
end

local function require_gsub()
    if type(S.gsub) ~= "function" then
        error("re: the string rock is required (run `luarocks install string`)")
    end
    return S.gsub
end

function R.find(text, pattern, init)
    return S.find(text, pattern, init)
end

function R.test(text, pattern)
    return S.find(text, pattern, 1) ~= nil
end

-- Every match, as yielded by gmatch: the whole match, or its captures when the
-- pattern has any.
function R.findall(text, pattern)
    local gmatch = require_gmatch()
    local out = {}
    for value in gmatch(text, pattern) do out[#out + 1] = value end
    return out
end

-- Every match as a table of captures; patterns without captures produce a
-- one-element table holding the whole match.
function R.matchall(text, pattern)
    text, pattern = tostring(text), tostring(pattern)
    local out = {}
    local pos = 1
    local anchored = string.sub(pattern, 1, 1) == "^"

    while pos <= #text + 1 do
        local found = captures_of(S.find(text, pattern, pos))
        if found[1] == nil then break end

        local entry = {}
        for i = 3, #found do entry[#entry + 1] = found[i] end
        if #entry == 0 then entry[1] = string.sub(text, found[1], found[2]) end
        out[#out + 1] = entry

        if found[2] < found[1] then pos = found[1] + 1 else pos = found[2] + 1 end
        if anchored then break end
    end

    return out
end

function R.count(text, pattern)
    text, pattern = tostring(text), tostring(pattern)
    local total = 0
    local pos = 1
    local anchored = string.sub(pattern, 1, 1) == "^"

    while pos <= #text + 1 do
        local found = captures_of(S.find(text, pattern, pos))
        if found[1] == nil then break end
        total = total + 1
        if found[2] < found[1] then pos = found[1] + 1 else pos = found[2] + 1 end
        if anchored then break end
    end

    return total
end

-- `re.split("a,b,,c", ",")` -> { "a", "b", "", "c" }.
-- Options: `keep_empty = false`, `max = N` (last field keeps the rest).
function R.split(text, pattern, options)
    options = options or {}
    text, pattern = tostring(text), tostring(pattern)

    if pattern == "" then
        local chars = {}
        for i = 1, #text do chars[i] = string.sub(text, i, i) end
        return chars
    end

    local out = {}
    local pos = 1
    local last = 1
    local max = options.max or 0
    local anchored = string.sub(pattern, 1, 1) == "^"

    while pos <= #text do
        if max > 0 and #out >= max - 1 then break end
        local found = captures_of(S.find(text, pattern, pos))
        if found[1] == nil then break end

        out[#out + 1] = string.sub(text, last, found[1] - 1)
        if found[2] < found[1] then
            pos = found[1] + 1
            last = found[1]
        else
            pos = found[2] + 1
            last = pos
        end
        if anchored then break end
    end
    out[#out + 1] = string.sub(text, last)

    if options.keep_empty == false then
        local filtered = {}
        for i = 1, #out do
            if out[i] ~= "" then filtered[#filtered + 1] = out[i] end
        end
        return filtered
    end
    return out
end

-- Returns the new text and the number of replacements (like string.gsub).
function R.replace(text, pattern, replacement, limit)
    local gsub = require_gsub()
    return gsub(text, pattern, replacement, limit)
end

-- First match's captures (or the whole match when there are none).
function R.groups(text, pattern)
    local found = captures_of(S.find(text, pattern, 1))
    if found[1] == nil then return nil end
    local out = {}
    for i = 3, #found do out[#out + 1] = found[i] end
    if #out == 0 then out[1] = string.sub(text, found[1], found[2]) end
    return out
end

local MAGIC = "^$()%.[]*+-?"

function R.escape(text)
    text = tostring(text)
    local out = {}
    for i = 1, #text do
        local c = string.sub(text, i, i)
        if string.find(MAGIC, c, 1, true) then out[#out + 1] = "%" .. c
        else out[#out + 1] = c end
    end
    return table.concat(out)
end

return R
