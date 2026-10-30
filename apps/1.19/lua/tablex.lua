-- rock: name=tablex
-- rock: version=1.0.0
-- rock: description=Table helpers: deepcopy, merge, map/filter/reduce, sortby, keys, range
-- rock: here=/lib/tablex.lua

-- The runtime ships only insert/concat/remove/sort/move/unpack/pack/decode.
-- These helpers never replace `table`; they return new tables unless the name
-- says otherwise (`merge_into`, `each`).
local T = tablex or {}

local floor = function(x) return x - x % 1 end

function T.copy(t)
    local out = {}
    if t == nil then return out end
    for k, v in pairs(t) do out[k] = v end
    return out
end

function T.deepcopy(t, seen)
    if type(t) ~= "table" then return t end
    seen = seen or {}
    if seen[t] then return seen[t] end
    local out = {}
    seen[t] = out
    for k, v in pairs(t) do
        local key = k
        if type(k) == "table" then key = T.deepcopy(k, seen) end
        out[key] = T.deepcopy(v, seen)
    end
    return out
end

function T.merge(a, b)
    local out = T.copy(a)
    if b ~= nil then
        for k, v in pairs(b) do out[k] = v end
    end
    return out
end

function T.merge_into(a, b)
    if b ~= nil then
        for k, v in pairs(b) do
            if type(v) == "table" and type(a[k]) == "table" then T.merge_into(a[k], v)
            else a[k] = v end
        end
    end
    return a
end

function T.keys(t)
    local out = {}
    if t == nil then return out end
    for k in pairs(t) do out[#out + 1] = k end
    return out
end

function T.sorted_keys(t, compare)
    local out = T.keys(t)
    table.sort(out, compare or function(a, b) return tostring(a) < tostring(b) end)
    return out
end

function T.values(t)
    local out = {}
    if t == nil then return out end
    for _, v in pairs(t) do out[#out + 1] = v end
    return out
end

function T.size(t)
    local n = 0
    if t == nil then return 0 end
    for _ in pairs(t) do n = n + 1 end
    return n
end

function T.isempty(t)
    if t == nil then return true end
    return next(t) == nil
end

function T.map(list, fn)
    local out = {}
    for i = 1, #list do out[i] = fn(list[i], i) end
    return out
end

function T.filter(list, fn)
    local out = {}
    for i = 1, #list do
        local value = list[i]
        if fn(value, i) then out[#out + 1] = value end
    end
    return out
end

function T.reduce(list, fn, init)
    local acc, start = init, 1
    if acc == nil then acc, start = list[1], 2 end
    for i = start, #list do acc = fn(acc, list[i], i) end
    return acc
end

function T.each(list, fn)
    for i = 1, #list do fn(list[i], i) end
    return list
end

function T.countif(list, fn)
    local n = 0
    for i = 1, #list do
        if fn(list[i], i) then n = n + 1 end
    end
    return n
end

function T.find(list, fn)
    for i = 1, #list do
        local value = list[i]
        local matched
        if type(fn) == "function" then matched = fn(value, i)
        else matched = (value == fn) end
        if matched then return value, i end
    end
    return nil
end

function T.indexof(list, value)
    return select(2, T.find(list, value))
end

function T.contains(list, value)
    return T.find(list, value) ~= nil
end

function T.sortby(list, fn, descending)
    local out = {}
    for i = 1, #list do out[i] = list[i] end
    local key = fn or function(value) return value end
    table.sort(out, function(a, b)
        local ka, kb = key(a), key(b)
        if descending then return ka > kb end
        return ka < kb
    end)
    return out
end

function T.unique(list)
    local seen, out = {}, {}
    for i = 1, #list do
        local value = list[i]
        local key = type(value) .. ":" .. tostring(value)
        if not seen[key] then
            seen[key] = true
            out[#out + 1] = value
        end
    end
    return out
end

function T.flatten(list, depth)
    depth = depth or 1
    local out = {}
    for i = 1, #list do
        local value = list[i]
        if type(value) == "table" and depth > 0 then
            local sub = T.flatten(value, depth - 1)
            for j = 1, #sub do out[#out + 1] = sub[j] end
        else out[#out + 1] = value end
    end
    return out
end

function T.range(a, b, step)
    if b == nil then a, b = 1, a end
    step = step or 1
    local out = {}
    if step > 0 then
        local i = a
        while i <= b do out[#out + 1] = i i = i + step end
    else
        local i = a
        while i >= b do out[#out + 1] = i i = i + step end
    end
    return out
end

function T.reverse(list)
    local out = {}
    local n = #list
    for i = 1, n do out[i] = list[n - i + 1] end
    return out
end

function T.slice(list, first, last)
    local out = {}
    last = last or #list
    if first < 0 then first = #list + first + 1 end
    if last < 0 then last = #list + last + 1 end
    for i = first, last do out[#out + 1] = list[i] end
    return out
end

function T.invert(t)
    local out = {}
    if t == nil then return out end
    for k, v in pairs(t) do out[v] = k end
    return out
end

function T.pick(t, keys)
    local out = {}
    for i = 1, #keys do out[keys[i]] = t[keys[i]] end
    return out
end

function T.omit(t, keys)
    local skip = {}
    for i = 1, #keys do skip[keys[i]] = true end
    local out = {}
    for k, v in pairs(t) do
        if not skip[k] then out[k] = v end
    end
    return out
end

function T.first(list) return list[1] end
function T.last(list) return list[#list] end

function T.equal(a, b, seen)
    if a == b then return true end
    if type(a) ~= "table" or type(b) ~= "table" then return false end
    seen = seen or {}
    if seen[a] == b then return true end
    seen[a] = b
    for k, v in pairs(a) do
        if not T.equal(v, b[k], seen) then return false end
    end
    for k in pairs(b) do
        if a[k] == nil then return false end
    end
    return true
end

return T
