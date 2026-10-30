-- rock: name=string
-- rock: version=1.1.0
-- rock: description=String helpers the runtime lacks: format, rep, gsub, gmatch and a pattern engine
-- rock: here=/lib/string.lua

-- The runtime's `string.find`/`string.match` are plain substring searches and
-- it has no format/rep/gsub/gmatch. This module extends the existing table
-- (so the native helpers stay) and installs a small Lua-pattern engine.
--
-- Compatibility note: after this module loads, `string.find`/`match` follow
-- Lua pattern rules. Pass `plain = true` (4th argument to find) for a literal
-- search. A failed pattern that contains `%` or `[` falls back to a literal
-- search so old code such as `string.find(s, "100%")` keeps working.
local M = string or {}

local function plain_find(s, needle, init)
    local n = #needle
    if n == 0 then
        if init <= #s + 1 then return init end
        return nil
    end
    local i = init
    while i + n - 1 <= #s do
        if string.sub(s, i, i + n - 1) == needle then return i end
        i = i + 1
    end
    return nil
end

local function floor(x)
    local n = x % 1
    if x >= 0 then return x - n end
    if n == 0 then return x end
    return x - n - 1
end

local function is_digit(c) local b = string.byte(c) return b ~= nil and b >= 48 and b <= 57 end
local function is_upper(c) local b = string.byte(c) return b ~= nil and b >= 65 and b <= 90 end
local function is_lower(c) local b = string.byte(c) return b ~= nil and b >= 97 and b <= 122 end
local function is_alpha(c) return is_upper(c) or is_lower(c) end
local function is_alnum(c) return is_alpha(c) or is_digit(c) end
local function is_space(c)
    local b = string.byte(c)
    return b ~= nil and (b == 32 or (b >= 9 and b <= 13))
end
local function is_cntrl(c)
    local b = string.byte(c)
    return b ~= nil and (b < 32 or b == 127)
end
local function is_punct(c)
    local b = string.byte(c)
    if b == nil then return false end
    return (b >= 33 and b <= 47) or (b >= 58 and b <= 64) or (b >= 91 and b <= 96) or (b >= 123 and b <= 126)
end
local function is_xdigit(c)
    local b = string.byte(c)
    if b == nil then return false end
    return (b >= 48 and b <= 57) or (b >= 65 and b <= 70) or (b >= 97 and b <= 102)
end

local function match_class(c, cl)
    if c == "" then return false end
    local lower = string.lower(cl)
    local result
    if lower == "a" then result = is_alpha(c)
    elseif lower == "c" then result = is_cntrl(c)
    elseif lower == "d" then result = is_digit(c)
    elseif lower == "l" then result = is_lower(c)
    elseif lower == "p" then result = is_punct(c)
    elseif lower == "s" then result = is_space(c)
    elseif lower == "u" then result = is_upper(c)
    elseif lower == "w" then result = is_alnum(c)
    elseif lower == "x" then result = is_xdigit(c)
    elseif lower == "z" then result = string.byte(c) == 0
    else return cl == c end
    if cl ~= lower then result = not result end
    return result
end

local function match_bracket(c, p, pi)
    if c == "" then return false end
    local i = pi + 1
    local negate = false
    if string.sub(p, i, i) == "^" then negate = true; i = i + 1 end
    local found = false
    local first = true
    while i <= #p do
        local ch = string.sub(p, i, i)
        if ch == "]" and not first then break end
        first = false
        if ch == "%" then
            local cl = string.sub(p, i + 1, i + 1)
            if cl == "" then break end
            if match_class(c, cl) then found = true end
            i = i + 2
        else
            local nxt = string.sub(p, i + 1, i + 1)
            if nxt == "-" and string.sub(p, i + 2, i + 2) ~= "" and string.sub(p, i + 2, i + 2) ~= "]" then
                local hi = string.sub(p, i + 2, i + 2)
                local b, lo, top = string.byte(c), string.byte(ch), string.byte(hi)
                if b ~= nil and lo ~= nil and top ~= nil and b >= lo and b <= top then found = true end
                i = i + 3
            else
                if ch == c then found = true end
                i = i + 1
            end
        end
    end
    if negate then found = not found end
    return found
end

local function class_end(p, pi)
    local ch = string.sub(p, pi, pi)
    if ch == "%" then return pi + 2 end
    if ch == "[" then
        local i = pi + 1
        if string.sub(p, i, i) == "^" then i = i + 1 end
        if string.sub(p, i, i) == "]" then i = i + 1 end
        while i <= #p do
            local c = string.sub(p, i, i)
            if c == "%" then i = i + 2
            elseif c == "]" then return i + 1
            else i = i + 1 end
        end
        return nil
    end
    return pi + 1
end

local function single_match(s, si, p, pi)
    local c = string.sub(s, si, si)
    if c == "" then return nil end
    local ch = string.sub(p, pi, pi)
    if ch == "." then return si + 1 end
    if ch == "%" then
        local cl = string.sub(p, pi + 1, pi + 1)
        if cl == "" then return nil end
        if match_class(c, cl) then return si + 1 end
        return nil
    end
    if ch == "[" then
        if match_bracket(c, p, pi) then return si + 1 end
        return nil
    end
    if ch == c then return si + 1 end
    return nil
end

local function copy_caps(caps)
    local out = {}
    for i = 1, #caps do
        local c = caps[i]
        out[i] = { start = c.start, finish = c.finish, pos = c.pos }
    end
    return out
end

local function match_balance(s, si, open, close)
    if string.sub(s, si, si) ~= open then return nil end
    local depth = 1
    local i = si + 1
    while i <= #s do
        local c = string.sub(s, i, i)
        if c == close then
            depth = depth - 1
            if depth == 0 then return i + 1 end
        elseif c == open then
            depth = depth + 1
        end
        i = i + 1
    end
    return nil
end

local do_match

local function greedy(s, si, p, pi, item_end, min, max, caps)
    local positions = { si }
    local cur = si
    local count = 0
    while true do
        local nsi = single_match(s, cur, p, pi)
        if not nsi then break end
        cur = nsi
        count = count + 1
        positions[count + 1] = cur
        if max ~= nil and count >= max then break end
    end
    for k = count + 1, 1, -1 do
        if (k - 1) >= min then
            local res, rcaps = do_match(s, positions[k], p, item_end + 1, caps)
            if res then return res, rcaps end
        end
    end
    return nil
end

local function lazy(s, si, p, pi, item_end, min, caps)
    local cur = si
    local count = 0
    while count < min do
        local nsi = single_match(s, cur, p, pi)
        if not nsi then return nil end
        cur = nsi
        count = count + 1
    end
    while true do
        local res, rcaps = do_match(s, cur, p, item_end + 1, caps)
        if res then return res, rcaps end
        local nsi = single_match(s, cur, p, pi)
        if not nsi then return nil end
        cur = nsi
    end
end

do_match = function(s, si, p, pi, caps)
    while pi <= #p do
        local pc = string.sub(p, pi, pi)
        if pc == ")" then
            local newcaps = copy_caps(caps)
            local found = false
            for k = #newcaps, 1, -1 do
                if newcaps[k].finish == nil and newcaps[k].start ~= nil then
                    newcaps[k].finish = si - 1
                    found = true
                    break
                end
            end
            if not found then return nil end
            caps = newcaps
            pi = pi + 1
        elseif pc == "(" then
            local newcaps = copy_caps(caps)
            if string.sub(p, pi + 1, pi + 1) == ")" then
                newcaps[#newcaps + 1] = { pos = si }
                pi = pi + 2
            else
                newcaps[#newcaps + 1] = { start = si }
                pi = pi + 1
            end
            caps = newcaps
        elseif pc == "$" and pi == #p then
            if si > #s then return si, caps end
            return nil
        elseif pc == "%" and string.sub(p, pi + 1, pi + 1) == "b" then
            local open = string.sub(p, pi + 2, pi + 2)
            local close = string.sub(p, pi + 3, pi + 3)
            if open == "" or close == "" then return nil end
            local nsi = match_balance(s, si, open, close)
            if not nsi then return nil end
            si = nsi
            pi = pi + 4
        elseif pc == "%" and string.sub(p, pi + 1, pi + 1) == "f" then
            local setstart = pi + 2
            if string.sub(p, setstart, setstart) ~= "[" then return nil end
            local prev = si > 1 and string.sub(s, si - 1, si - 1) or ""
            local cur = string.sub(s, si, si)
            if prev ~= "" and match_bracket(prev, p, setstart) then return nil end
            if cur == "" or not match_bracket(cur, p, setstart) then return nil end
            local se = class_end(p, setstart)
            if not se then return nil end
            pi = se
        elseif pc == "%" and is_digit(string.sub(p, pi + 1, pi + 1)) then
            local n = tonumber(string.sub(p, pi + 1, pi + 1))
            local cap = caps[n]
            if not cap or cap.start == nil then return nil end
            local text = string.sub(s, cap.start, cap.finish or #s)
            if #text > 0 then
                if string.sub(s, si, si + #text - 1) ~= text then return nil end
                si = si + #text
            end
            pi = pi + 2
        else
            local item_end = class_end(p, pi)
            if not item_end then return nil end
            local quant = string.sub(p, item_end, item_end)
            if quant == "*" then
                local res, rcaps = greedy(s, si, p, pi, item_end, 0, nil, caps)
                if res then return res, rcaps end
                return nil
            elseif quant == "+" then
                local res, rcaps = greedy(s, si, p, pi, item_end, 1, nil, caps)
                if res then return res, rcaps end
                return nil
            elseif quant == "-" then
                local res, rcaps = lazy(s, si, p, pi, item_end, 0, caps)
                if res then return res, rcaps end
                return nil
            elseif quant == "?" then
                local res, rcaps = greedy(s, si, p, pi, item_end, 0, 1, caps)
                if res then return res, rcaps end
                return nil
            else
                local nsi = single_match(s, si, p, pi)
                if not nsi then return nil end
                si = nsi
                pi = item_end
            end
        end
    end
    return si, caps
end

local unpack_values = table.unpack or function(t)
    return t[1], t[2], t[3], t[4], t[5], t[6], t[7], t[8], t[9]
end

local MAGIC = "^$*+?.([%-]"
local function has_magic(p)
    for i = 1, #MAGIC do
        if plain_find(p, string.sub(MAGIC, i, i), 1) then return true end
    end
    return false
end

local function capture_values(s, caps, whole)
    local out = {}
    for k = 1, #caps do
        local cap = caps[k]
        if cap.pos ~= nil then out[#out + 1] = cap.pos
        elseif cap.start ~= nil then out[#out + 1] = string.sub(s, cap.start, cap.finish or #s) end
    end
    if #out == 0 then out[1] = whole end
    return out
end

local function find_impl(s, p, init, plain)
    if type(s) ~= "string" then s = tostring(s) end
    if type(p) ~= "string" then p = tostring(p) end
    init = init or 1
    if init < 0 then init = #s + init + 1 end
    if init < 1 then init = 1 end
    if init > #s + 1 then return nil end

    if plain or not has_magic(p) then
        local i = plain_find(s, p, init)
        if i then return i, i + #p - 1 end
        return nil
    end

    local anchor = false
    local pi = 1
    if string.sub(p, 1, 1) == "^" then anchor = true; pi = 2 end

    local si = init
    while si <= #s + 1 do
        local e, caps = do_match(s, si, p, pi, {})
        if e then
            local whole = string.sub(s, si, e - 1)
            local values = capture_values(s, caps, whole)
            local result = { si, e - 1 }
            for k = 1, #values do result[#result + 1] = values[k] end
            return unpack_values(result)
        end
        if anchor then break end
        si = si + 1
    end

    -- Old callers used `%` and `[` as literals; keep those working.
    if plain_find(p, "%", 1) or plain_find(p, "[", 1) then
        local i = plain_find(s, p, init)
        if i then return i, i + #p - 1 end
    end
    return nil
end

local function match_impl(s, p, init)
    if type(s) ~= "string" then s = tostring(s) end
    if type(p) ~= "string" then p = tostring(p) end
    init = init or 1
    if init < 0 then init = #s + init + 1 end
    if init < 1 then init = 1 end
    if init > #s + 1 then return nil end

    local anchor = false
    local pi = 1
    if string.sub(p, 1, 1) == "^" then anchor = true; pi = 2 end

    local si = init
    while si <= #s + 1 do
        local e, caps = do_match(s, si, p, pi, {})
        if e then
            local whole = string.sub(s, si, e - 1)
            local values = capture_values(s, caps, whole)
            if #caps > 0 then return unpack_values(values) end
            return whole
        end
        if anchor then break end
        si = si + 1
    end
    return nil
end

function M.find(s, p, init, plain)
    return find_impl(s, p, init, plain)
end

function M.match(s, p, init)
    return match_impl(s, p, init)
end

local function expand_replacement(repl, whole, caps)
    local out = {}
    local i = 1
    while i <= #repl do
        local c = string.sub(repl, i, i)
        if c == "%" then
            local n = string.sub(repl, i + 1, i + 1)
            if n == "%" then
                out[#out + 1] = "%"
                i = i + 2
            elseif n == "0" then
                out[#out + 1] = whole
                i = i + 2
            elseif n >= "1" and n <= "9" then
                local v = caps[tonumber(n)]
                if v ~= nil then out[#out + 1] = tostring(v) end
                i = i + 2
            else
                out[#out + 1] = c
                i = i + 1
            end
        else
            out[#out + 1] = c
            i = i + 1
        end
    end
    return table.concat(out)
end

function M.gsub(s, p, repl, limit)
    if type(s) ~= "string" then s = tostring(s) end
    if type(p) ~= "string" then p = tostring(p) end
    limit = limit or (#s + 1)
    local out = {}
    local last = 1
    local pos = 1
    local count = 0
    local anchor = string.sub(p, 1, 1) == "^"
    local pi = anchor and 2 or 1

    while pos <= #s + 1 and count < limit do
        local e, caps = do_match(s, pos, p, pi, {})
        if not e then
            if anchor then break end
            pos = pos + 1
        else
            local start = pos
            local whole = string.sub(s, start, e - 1)
            local caps_values = capture_values(s, caps, whole)
            out[#out + 1] = string.sub(s, last, start - 1)

            local replacement
            if type(repl) == "string" then
                replacement = expand_replacement(repl, whole, caps_values)
            elseif type(repl) == "table" then
                local key = caps_values[1]
                if key == nil then key = whole end
                replacement = repl[key]
                if replacement == nil or replacement == false then replacement = whole end
                replacement = tostring(replacement)
            elseif type(repl) == "function" then
                if #caps > 0 then replacement = repl(unpack_values(caps_values))
                else replacement = repl(whole) end
                if replacement == nil or replacement == false then replacement = whole end
                replacement = tostring(replacement)
            else
                error("bad argument #3 to 'gsub' (string/function/table expected)")
            end

            out[#out + 1] = replacement
            count = count + 1

            if e == start then
                if start <= #s then out[#out + 1] = string.sub(s, start, start) end
                pos = start + 1
                last = start + 1
            else
                pos = e
                last = e
            end
            if anchor then break end
        end
    end

    out[#out + 1] = string.sub(s, last)
    return table.concat(out), count
end

function M.gmatch(s, p)
    if type(s) ~= "string" then s = tostring(s) end
    if type(p) ~= "string" then p = tostring(p) end
    local pos = 1
    local anchor = string.sub(p, 1, 1) == "^"
    local pi = anchor and 2 or 1
    local first = true

    return function()
        if anchor and not first then return nil end
        first = false
        while pos <= #s + 1 do
            local e, caps = do_match(s, pos, p, pi, {})
            if e then
                local start = pos
                local whole = string.sub(s, start, e - 1)
                if e == start then pos = start + 1 else pos = e end
                local values = capture_values(s, caps, whole)
                if #caps > 0 then return unpack_values(values) end
                return whole
            end
            if anchor then return nil end
            pos = pos + 1
        end
        return nil
    end
end

local DIGITS = "0123456789abcdef"

local function int_to_base(n, base, upper)
    n = floor(n)
    if n == 0 then return "0" end
    local neg = n < 0
    if neg then n = -n end
    local out = {}
    while n > 0 do
        local d = n % base
        out[#out + 1] = string.sub(DIGITS, d + 1, d + 1)
        n = floor(n / base)
    end
    local result = string.reverse(table.concat(out))
    if upper then result = string.upper(result) end
    if neg then result = "-" .. result end
    return result
end

local function format_float(x, precision)
    precision = precision or 6
    local neg = x < 0
    if neg then x = -x end
    local scale = 10 ^ precision
    local scaled = floor(x * scale + 0.5)
    local int = floor(scaled / scale)
    local frac = scaled - int * scale
    local fracstr = int_to_base(frac, 10, false)
    while #fracstr < precision do fracstr = "0" .. fracstr end
    local result = int_to_base(int, 10, false)
    if precision > 0 then result = result .. "." .. fracstr end
    if neg then result = "-" .. result end
    return result
end

local function pad_text(text, width, left, zero)
    if #text >= width then return text end
    local missing = width - #text
    if zero and not left then
        local sign = ""
        local first = string.sub(text, 1, 1)
        if first == "-" or first == "+" then
            sign = first
            text = string.sub(text, 2)
        end
        local zeros = ""
        for i = 1, missing do zeros = zeros .. "0" end
        return sign .. zeros .. text
    end
    local fill = ""
    local ch = zero and "0" or " "
    for i = 1, missing do fill = fill .. ch end
    if left then return text .. fill end
    return fill .. text
end

local function quote_string(value)
    local s = tostring(value)
    local out = { '"' }
    for i = 1, #s do
        local c = string.sub(s, i, i)
        if c == '"' or c == "\\" then out[#out + 1] = "\\" .. c
        elseif c == "\n" then out[#out + 1] = "\\n"
        elseif c == "\r" then out[#out + 1] = "\\r"
        elseif c == "\t" then out[#out + 1] = "\\t"
        elseif string.byte(c) < 32 then out[#out + 1] = "\\" .. int_to_base(string.byte(c), 10, false)
        else out[#out + 1] = c end
    end
    out[#out + 1] = '"'
    return table.concat(out)
end

function M.format(fmt, ...)
    fmt = tostring(fmt)
    -- The OpenTTY runtime stores `...` in one table; stock Lua keeps real
    -- varargs. Accept both so the module also runs under the host tests.
    local args
    local first = ...
    local total = select('#', ...)
    if type(first) == "table" and total == #first then
        args = first
    else
        args = {}
        for k = 1, total do args[k] = select(k, ...) end
    end
    local argi = 1
    local out = {}
    local i = 1
    while i <= #fmt do
        local c = string.sub(fmt, i, i)
        if c ~= "%" then
            out[#out + 1] = c
            i = i + 1
        else
            i = i + 1
            local flags = ""
            while true do
                local f = string.sub(fmt, i, i)
                if f == "-" or f == "+" or f == " " or f == "0" or f == "#" then
                    flags = flags .. f
                    i = i + 1
                else break end
            end
            local width = ""
            while is_digit(string.sub(fmt, i, i)) do
                width = width .. string.sub(fmt, i, i)
                i = i + 1
            end
            local precision = nil
            if string.sub(fmt, i, i) == "." then
                i = i + 1
                precision = ""
                while is_digit(string.sub(fmt, i, i)) do
                    precision = precision .. string.sub(fmt, i, i)
                    i = i + 1
                end
                precision = tonumber(precision) or 0
            end
            local spec = string.sub(fmt, i, i)
            i = i + 1
            width = tonumber(width) or 0
            local left = plain_find(flags, "-", 1) ~= nil
            local zero = plain_find(flags, "0", 1) ~= nil
            local plus = plain_find(flags, "+", 1) ~= nil
            local space = plain_find(flags, " ", 1) ~= nil
            local value = args[argi]
            argi = argi + 1
            local text

            if spec == "%" then
                text = "%"
            elseif spec == "d" or spec == "i" then
                local n = tonumber(value) or 0
                if n >= 0 then n = floor(n + 0.5) else n = -floor(-n + 0.5) end
                text = int_to_base(n, 10, false)
                if n >= 0 and plus then text = "+" .. text
                elseif n >= 0 and space then text = " " .. text end
            elseif spec == "u" then
                text = int_to_base(tonumber(value) or 0, 10, false)
            elseif spec == "x" or spec == "X" then
                text = int_to_base(tonumber(value) or 0, 16, spec == "X")
            elseif spec == "o" then
                text = int_to_base(tonumber(value) or 0, 8, false)
            elseif spec == "c" then
                text = string.char(tonumber(value) or 0)
            elseif spec == "s" then
                text = tostring(value)
                if precision ~= nil and #text > precision then text = string.sub(text, 1, precision) end
            elseif spec == "q" then
                text = quote_string(value)
            elseif spec == "f" or spec == "F" then
                text = format_float(tonumber(value) or 0, precision or 6)
            elseif spec == "e" or spec == "E" or spec == "g" or spec == "G" then
                text = format_float(tonumber(value) or 0, precision or 6)
            else
                text = "%" .. spec
            end

            if spec ~= "%" then text = pad_text(text, width, left, zero) end
            out[#out + 1] = text
        end
    end
    return table.concat(out)
end

function M.rep(s, n, sep)
    s = tostring(s)
    n = floor(tonumber(n) or 0)
    if n <= 0 then return "" end
    sep = sep or ""
    local out = {}
    for i = 1, n do
        if i > 1 then out[#out + 1] = sep end
        out[#out + 1] = s
    end
    return table.concat(out)
end

function M.lpad(s, n, ch)
    s = tostring(s)
    ch = ch or " "
    local missing = n - #s
    if missing <= 0 then return s end
    return M.rep(ch, missing) .. s
end

function M.rpad(s, n, ch)
    s = tostring(s)
    ch = ch or " "
    local missing = n - #s
    if missing <= 0 then return s end
    return s .. M.rep(ch, missing)
end

function M.join(t, sep)
    sep = sep or ""
    local out = {}
    for i = 1, #t do out[i] = tostring(t[i]) end
    return table.concat(out, sep)
end

function M.lines(s)
    s = tostring(s)
    local pos = 1
    return function()
        if pos > #s then return nil end
        local nl = plain_find(s, "\n", pos)
        local line
        if nl then
            line = string.sub(s, pos, nl - 1)
            pos = nl + 1
        else
            line = string.sub(s, pos)
            pos = #s + 1
        end
        if string.sub(line, -1) == "\r" then line = string.sub(line, 1, -2) end
        return line
    end
end

function M.replace(s, find, repl)
    s = tostring(s)
    find = tostring(find)
    repl = tostring(repl)
    if find == "" then return s end
    local out = {}
    local pos = 1
    while true do
        local i = plain_find(s, find, pos)
        if not i then break end
        out[#out + 1] = string.sub(s, pos, i - 1)
        out[#out + 1] = repl
        pos = i + #find
    end
    out[#out + 1] = string.sub(s, pos)
    return table.concat(out)
end

function M.text2note(s)
    if s == nil or s == "" then return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:\nEND:VNOTE" end
    s = M.replace(s, "=", "=3D")
    s = M.replace(s, "\n", "=0A")
    return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" .. s .. "\nEND:VNOTE"
end

return M
