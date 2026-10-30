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

-- 9. Alert indicators: a standalone gauge can be attached to an alert, read
--    back, and a max of 0 makes it the MIDP continuous running indicator.
--    (SetGauge posts the update to the event thread, so only the initial
--    value is checked here.)
local alert = graphics.new("alert", "gauge", "body")
local gauge = graphics.new("gauge", { interactive = false, maxValue = 10, value = 3 })
check("gauge initial value", graphics.GetGauge(gauge) == 3, tostring(graphics.GetGauge(gauge)))
graphics.SetIndicator(alert, gauge)
check("SetIndicator accepts a gauge", alert ~= nil)

local running = graphics.new("gauge", { interactive = false, maxValue = 0, value = graphics.CONTINUOUS_RUNNING })
check("continuous gauge value", graphics.GetGauge(running) == 2, tostring(graphics.GetGauge(running)))

-- 10. Anonymous function in a table constructor with a nested
--     `local function` whose body has one-line `if ... end` blocks.
--     Every nested `end` used to be copied twice into the anonymous
--     body, so the nested function body stopped at the first `end` and
--     the rest ran in the outer frame (nil locals, ArithmeticException).
local handlers = {
    run = function()
        local function render(value)
            local w = value
            if w < 4 then w = 4 end
            local bar = 6 + w
            return bar
        end
        return render(1) + render(10)
    end,
    [2] = function() end
}
local okHandler, handlerResult = pcall(handlers.run)
check("anonymous fn + nested local function",
    okHandler and handlerResult == 26,
    tostring(handlerResult))

-- 11. Same shape as the pkg screen: the nested function keeps a table
--     upvalue and loops over it after a one-line guard.
local screens = {
    [1] = function()
        local state = { lines = { "a", "b", "c" } }
        local function draw()
            local rows = #state.lines
            if rows < 1 then rows = 1 end
            local total = 0
            for i = 1, rows do
                if state.lines[i] then total = total + 1 end
            end
            return total
        end
        return draw()
    end,
    [2] = function() end
}
local okScreen, drawn = pcall(screens[1])
check("anonymous fn keeps table upvalue after guard",
    okScreen and drawn == 3,
    tostring(drawn))

-- 13. Text input round-trip: the paint app asks for the image output path in a
--     TextBox, so creating one, displaying it and going back must not break.
local pathBox = graphics.new("edit", "Image path", "/mnt/opentty/paint.png")
check("edit box keeps its text",
    graphics.GetText(pathBox) == "/mnt/opentty/paint.png",
    tostring(graphics.GetText(pathBox)))
local okBox = pcall(function()
    local ok = graphics.new("command", { label = "Save", type = "ok", priority = 1 })
    graphics.addCommand(pathBox, ok)
    graphics.handler(pathBox, { [ok] = function(text) end })
    graphics.display(pathBox)
    canvas:display()
end)
check("text box can be shown and the canvas restored", okBox)

-- 14. Postfix chains after any primary expression: `(f)(x)`, `f()(x)` and
--     `("a" .. "b"):upper()` used to stop at the closing parenthesis.
local schema = {
    name = { label = "schema", run = function(self, v) return self.label .. ":" .. v end },
    build = { label = "build", run = function(self, v) return self.label .. ":" .. v end }
}
local function pick(key) return schema[key] end
local okParen, parenResult = pcall(function() return (pick("name")).run(pick("name"), "x") end)
check("(f)(x) parenthesized call", okParen and parenResult == "schema:x", tostring(parenResult))
local okChain, chainResult = pcall(function() return (pick("build")).run(pick("build"), "y") end)
check("method call on parenthesized table", okChain and chainResult == "build:y", tostring(chainResult))
local okColon, colonResult = pcall(function() return ("ab"):upper() end)
check("method on literal string", okColon and colonResult == "AB", tostring(colonResult))
local okCallField, callField = pcall(function() return pick("name").label end)
check("field after call", okCallField and callField == "schema", tostring(callField))
local okNested, nested = pcall(function() local function g() return { label = "L" } end return g().label end)
check("field after nested call", okNested and nested == "L", tostring(nested))

-- 15. A call returns all of its results; `return f()` still forwards every one
--     of them, while `f().field` keeps only the first.
local function pair() return 1, 2 end
local first, second = pair()
check("call keeps multiple results", first == 1 and second == 2, tostring(first) .. "," .. tostring(second))
local function passthrough() return pair() end
local fwd1, fwd2 = passthrough()
check("return f() forwards results", fwd1 == 1 and fwd2 == 2, tostring(fwd1) .. "," .. tostring(fwd2))
local okSingle, single = pcall(function() local t = { label = "one" } local function make() return t, "extra" end return make().label end)
check("call result keeps only first for indexing", okSingle and single == "one", tostring(single))

-- 16. math.random follows Lua semantics: random(n) -> 1..n, random(m, n) ->
--     m..n, random() -> [0, 1). The old 0..n-1 form made snake index the color
--     palette with 0 (nil) and abort every draw.
local oneToN, whole = true, true
for i = 1, 300 do
    local v = math.random(7)
    if v < 1 or v > 7 then oneToN = false end
    if v % 1 ~= 0 then whole = false end
end
check("math.random(n) returns 1..n", oneToN and whole)
local mToN = true
for i = 1, 300 do
    local v = math.random(3, 5)
    if v < 3 or v > 5 then mToN = false end
end
check("math.random(m, n) returns m..n", mToN)
local unit = math.random()
check("math.random() is in [0, 1)", unit >= 0 and unit < 1, tostring(unit))

-- 17. A call used directly as a condition uses its first value: `if not
--     occupied(x, y) then` was always truthy (a call returns a result list),
--     so snake never placed a new apple after eating.
local function no() return false end
local function yes() return true end
local taken, skipped = 0, 0
for i = 1, 3 do
    if not no() then taken = taken + 1 end
    if yes() then taken = taken + 1 end
    if no() then skipped = skipped + 1 end
end
check("calls in conditions use the first value",
    taken == 6 and skipped == 0,
    tostring(taken) .. "," .. tostring(skipped))
local chosen = no() and "yes-branch" or "no-branch"
check("calls in and/or use the first value", chosen == "no-branch", tostring(chosen))

-- 18. Boot environment: the runtime seeds HOSTNAME (fetch, uname -n and the
--     xterm prompt read it) and the process scope carries the real user name,
--     so `fetch` prints <user>@<host> instead of the old user@localhost.
local host = os.getenv("HOSTNAME")
check("HOSTNAME env is seeded", host ~= nil and host ~= "", tostring(host))
local scopeUser = os.scope()["USER"]
check("scope carries the user name", scopeUser ~= nil and scopeUser ~= "", tostring(scopeUser))

mark("FAILURES=" .. fails)
canvas:close()
