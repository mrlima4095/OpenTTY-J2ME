-- Lua runtime semantics checked on the real interpreter (desktop shim, headless).
--   Usage: tests/lua/run_lua.sh
-- Writes /mnt/opentty/runtime-result.txt (host: data/mnt/opentty/).
local OUT = "/mnt/opentty/runtime-result.txt"
local fails = 0

local function mark(line) io.write(line .. "\n", OUT, "a") end
local function check(name, cond, detail)
    if cond then mark("ok   " .. name)
    else fails = fails + 1 mark("FAIL " .. name .. (detail and (": " .. tostring(detail)) or "")) end
end

-- 1. Numeric `for` must not stop when the body's last statement returns a
--    value (canvas methods return the handle; this used to abort after i = 1).
local canvas = graphics.canvas.new({ title = "runtime", background = 0x101010 })
local items = { { x = 1 }, { x = 2 }, { x = 3 } }
local seen = 0
for i = 1, #items do
    local item = items[i]
    canvas:color(0xFFFFFF)
    canvas:rect(item.x, 1, 2, 2, true)
    seen = seen + 1
end
check("numeric for keeps iterating", seen == 3, "iterations=" .. tostring(seen))

-- 2. Same with a generic `for` over the table.
local generic = 0
for i, item in pairs(items) do
    canvas:rect(item.x, 1, 2, 2, true)
    generic = generic + 1
end
check("generic for keeps iterating", generic == 3, "iterations=" .. tostring(generic))

-- 3. `return` inside a loop still leaves the function with its value.
local function firstBig(list)
    for i = 1, #list do
        if list[i] > 2 then return list[i] end
        canvas:rect(1, 1, 2, 2, true)
    end
    return -1
end
check("return from inside for", firstBig({ 1, 3, 5 }) == 3, firstBig({ 1, 3, 5 }))

-- 4. `break` stops the loop and execution continues after it.
local broke = 0
for i = 1, 10 do
    broke = broke + 1
    if i == 2 then break end
end
check("break leaves the loop", broke == 2, "iterations=" .. tostring(broke))

-- 5. `and`/`or` short-circuit (a nil guard must not evaluate the right side).
local nilValue = nil
local guarded = nilValue and nilValue > 0
check("and short-circuits", guarded == nil, tostring(guarded))
local calls = 0
local function bump() calls = calls + 1 return true end
local skipped = nilValue and bump()
check("and skips the call", calls == 0, "calls=" .. tostring(calls))
local kept = "x" or bump()
check("or skips the call", calls == 0 and kept == "x", "calls=" .. tostring(calls))
local taken = nilValue or bump()
check("or evaluates the fallback", calls == 1 and taken == true, "calls=" .. tostring(calls))

-- 6. tonumber returns nil for non-numbers instead of raising.
local bad = tonumber("")
local good = tonumber("42")
check("tonumber('') is nil", bad == nil, tostring(bad))
check("tonumber('42') is 42", good == 42, tostring(good))

-- 7. `t[#t] = nil` shrinks the list.
local list = { 1, 2, 3 }
list[#list] = nil
check("t[#t] = nil shrinks", #list == 2, "#list=" .. tostring(#list))

-- 8. Multi-assignment takes the first value of each call.
local function two() return 1, 2 end
local a, b = two(), two()
check("multi-assignment first values", a == 1 and b == 1, tostring(a) .. "," .. tostring(b))

mark("FAILURES=" .. fails)
canvas:close()
