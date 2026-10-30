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

-- 19. os.time()/os.date() round-trip (os.date no longer returns Date.toString()).
local stamp = os.time({ year = 2024, month = 1, day = 2, hour = 3, min = 4, sec = 5 })
check("os.time accepts a date table", type(stamp) == "number" and stamp > 0, tostring(stamp))
local formatted = os.date("%Y-%m-%d %H:%M:%S", stamp)
check("os.date formats a timestamp", formatted == "2024-01-02 03:04:05", formatted)
local weekday = os.date("%a %j", stamp)
check("os.date %a/%j", weekday == "Tue 002", weekday)

-- 19. Every snippet below is valid Lua 5.1 (checked against the reference
--     implementation); the OpenTTY tokenizer/parser/runner must accept it too.
--     Snippets stay side-effect free because `load` executes the chunk here.
local syntaxCases = {
    { "locals", "local a = 1" },
    { "multi-assign", "local a, b, c = 1, 2, 3" },
    { "swap", "local a, b = 1, 2 a, b = b, a" },
    { "table-trailing-comma", "local t = { 1, 2, 3, }" },
    { "table-semicolon", "local t = { 1; 2; 3 }" },
    { "table-mixed", "local t = { 1, x = 2, [3] = 4; 5 }" },
    { "table-nested", "local t = { a = { b = { c = 1 } } } local v = t.a.b.c" },
    { "table-index-assign", "local t = {} t[1] = 1 t['k'] = 2 t.x, t.y = 3, 4" },
    { "function-stmt", "function f() end" },
    { "local-function", "local function f(a, b) return a + b end" },
    { "recursion", "local function f(n) if n < 2 then return 1 end return n * f(n - 1) end" },
    { "anonymous", "local f = function(a, b) return a + b end" },
    { "anonymous-table", "local t = { [1] = function() return 1 end, [2] = function() end }" },
    { "varargs", "local function f(...) return select('#', ...) end" },
    { "varargs-table", "local function f(...) local a = { ... } return a end" },
    { "if-elseif-else", "local a = 1 if a == 1 then a = 2 elseif a == 3 then a = 4 else a = 5 end" },
    { "if-nested", "local a = 1 if a then if a then a = 2 end end" },
    { "while-break", "local n = 0 while true do n = n + 1 if n > 3 then break end end" },
    { "repeat", "local n = 0 repeat n = n + 1 until n > 3" },
    { "numeric-for", "local s = 0 for i = 1, 10, 2 do s = s + i end" },
    { "numeric-for-down", "local s = 0 for i = 10, 1, -1 do s = s + i end" },
    { "generic-for", "local s = 0 for k, v in pairs({ a = 1 }) do s = s + v end" },
    { "ipairs", "local s = 0 for i, v in ipairs({ 1, 2 }) do s = s + v end" },
    { "nested-loop-break", "for i = 1, 3 do for j = 1, 3 do if j == 2 then break end end end" },
    { "method-call", "local t = { n = 1 } function t.get(self) return self.n end local v = t:get()" },
    { "method-chain", "local t = { n = 1 } function t.get(self) return self end local v = t:get():get()" },
    { "string-escapes", "local s = \"a\\tb\\nc\\\\d\\\"e\\65\"" },
    { "string-single", "local s = 'a\\'b' .. \"c\"" },
    { "long-string", "local s = [[line1\nline2]]" },
    { "long-string-level", "local s = [==[a]]b]==]" },
    { "long-comment", "local a = 1 --[[ block\ncomment ]] local b = 2" },
    { "long-comment-level", "local a = 1 --[==[ x ]] y ]==] local b = 2" },
    { "line-comment", "local a = 1 -- trailing" },
    { "hex", "local a = 0xFF + 0x10" },
    { "exponents", "local a = 1e3 + 1E+3 + 1e-3" },
    { "leading-dot", "local a = .5 + 5. + 0.5" },
    { "unary", "local a = -1 local b = - -1 local c = not not true" },
    { "power", "local a = 2 ^ 10 local b = 2 ^ -2 local c = -2 ^ 2" },
    { "precedence", "local a = 1 + 2 * 3 ^ 2 % 4 - 5 / 6" },
    { "concat", "local s = 'a' .. 'b' .. 'c' .. 1" },
    { "compare", "local a = 1 < 2 local b = (1 < 2) == true local c = 1 ~= 2" },
    { "logic", "local a = 1 and 2 or 3 local b = nil or false or 4" },
    { "length", "local a = #'abc' + #({ 1, 2 })" },
    { "do-block", "do local a = 1 end" },
    { "semicolons", "local a = 1; local b = 2; a = a + b" },
    { "return-multi", "local function f() return 1, 2 end local a, b = f()" },
    { "return-nothing", "local function f(x) if x then return end return 1 end" },
    { "empty-function", "function noop() end noop()" },
    { "call-string", "local function id(v) return v end local a = id 'text'" },
    { "call-table", "local function id(v) return v end local a = id { 1, 2 }" },
    { "call-parens", "local function id(v) return v end local a = (id)(1)" },
    { "call-result-index", "local function mk() return { n = 1 } end local v = mk().n" },
    { "index-result-call", "local t = { f = function() return 1 end } local v = t.f()" },
    { "paren-index", "local v = ({ 10, 20 })[2]" },
    { "adjacent-strings", "local s = 'a' 'b'" },
    { "unpack", "local a, b = unpack({ 1, 2 })" },
    { "setmetatable", "local t = setmetatable({}, { __index = function() return 0 end }) local v = t.missing" },
    { "multiline-call", "local function f(a, b)\n  return a + b\nend\nlocal v = f(\n  1,\n  2\n)" },
    { "long-args", "local function f(...) return select('#', ...) end local v = f(1, 2, 3, 4, 5)" },
    { "comment-only", "-- just a comment" },
    { "empty", "" },
    { "trailing-newline", "local a = 1\n" },
    { "crlf", "local a = 1\r\nlocal b = 2\r\n" },
    { "nested-function-decl", "local t = {} function t.f(a) return a end local v = t.f(1)" },
    { "method-def-in-table", "local t = { n = 1 } t.get = function(self) return self.n end local v = t:get()" },
    { "string-with-percent", "local s = '100%' .. '%d'" },
    { "string-with-quotes", "local s = \"he said \\\"hi\\\"\"" },
    { "number-float-int", "local a = 1.0 local b = 1 + 1.0" },
    { "deep-parens", "local a = ((((1))))" },
    { "empty-block", "if true then end while false do end for i = 1, 0 do end" },
    { "assign-index-expr", "local t = { 1, 2 } local i = 1 t[i + 1] = t[i]" },
    { "table-function-call", "local t = { f = function(x) return x * 2 end } local v = t.f(3)" },
    { "chained-index", "local t = { a = { b = { c = 1 } } } local v = t['a']['b']['c']" },
    { "local-function-before-use", "local f f = function() return 1 end local v = f()" },
    { "concat-numbers", "local s = 1 .. 2 .. 3" },
    { "table-nested-call", "local t = { f = function() return { g = function() return 7 end } end } local v = t.f().g()" }
}
local syntaxFailed = {}
for i = 1, #syntaxCases do
    local name = syntaxCases[i][1]
    local okCase, caseErr = pcall(load, syntaxCases[i][2], "=(case " .. name .. ")")

    if not okCase then
        local message = tostring(caseErr)
        local newline = string.find(message, "\n")
        if newline then message = string.sub(message, 1, newline - 1) end
        syntaxFailed[#syntaxFailed + 1] = name .. ": " .. message
    end
end
check("valid Lua 5.1 syntax parses (" .. #syntaxCases .. " cases)",
    #syntaxFailed == 0,
    #syntaxFailed > 0 and table.concat(syntaxFailed, " | ") or nil)

-- 20. Tokenizer/parser/runner semantics that used to diverge from Lua 5.1.
check("leading-dot and trailing-dot numbers", .5 == 0.5 and 5. == 5, tostring(.5) .. "," .. tostring(5.))
check("decimal exponents", 1e3 == 1000 and 1E+2 == 100 and 1e-2 == 0.01)
check("hex literals", 0xFF == 255 and 0x10 + 1 == 17)
check("subtraction without spaces", (function() local x = 5 return x-1 end)() == 4)
check("unary minus binds looser than ^", -2 ^ 2 == -4, tostring(-2 ^ 2))
check("^ is right associative", 2 ^ 3 ^ 2 == 512, tostring(2 ^ 3 ^ 2))
check("% is floored", -3 % 2 == 1 and 3 % -2 == -1, tostring(-3 % 2) .. "," .. tostring(3 % -2))
check("tonumber takes a base", tonumber("10", 2) == 2 and tonumber("ff", 16) == 255)
check("semicolon table fields", #{ 1; 2; 3 } == 3 and ({ 1, x = 2; 5 })[2] == 5)
check("string escapes decode", "a\tb" == "a" .. string.char(9) .. "b" and "\65\66" == "AB")
check("long bracket levels", [==[a]]b]==] == "a]]b" and #[[x]] == 1)

local swapA, swapB = 1, 2
swapA, swapB = swapB, swapA
check("multi-assign swap", swapA == 2 and swapB == 1)

local cells = { "p", "q" }
cells[1], cells[2] = cells[2], cells[1]
check("indexed multi-assign", cells[1] == "q" and cells[2] == "p", cells[1] .. "," .. cells[2])

local methodHost = { n = 7 }
function methodHost:value() return self.n end
check("function t:m() definition", methodHost:value() == 7)

local function ident(v) return v end
check("call sugar f{...} and f'text'", ident { 4 }[1] == 4 and ident "hi" == "hi")

local function three(...) return select("#", ...) end
check("varargs forward as values", three(1, 2, 3) == 3 and three() == 0)
local function pass(...) return ... end
local p1, p2 = pass("x", "y")
check("return ... forwards", p1 == "x" and p2 == "y")
local packed = { pass(1, 2, 3) }
check("{...} expands the last call", #packed == 3 and packed[3] == 3, tostring(#packed))
check("select returns a result list", ({ select(2, "a", "b", "c") })[1] == "b")

local chunkLocal = 5
check("chunk locals stay out of _G", _G.chunkLocal == nil)
check("load('') is accepted", pcall(load, "") == true)
check("load() inside a function", pcall(function() return load("local z = 2", "=(fn)") end) == true)
check("unpack global exists", select("#", unpack({ 1, 2 })) == 2)

local okTable, errTable = pcall(function() error({ code = 9 }) end)
check("error() keeps a table value", okTable == false and type(errTable) == "table" and errTable.code == 9)
check("pcall without a return yields one value", select("#", pcall(function() end)) == 1)

local insertList = { "a", "b", "c" }
table.insert(insertList, 2, "x")
table.remove(insertList)
check("table.insert/remove positions", table.concat(insertList, ",") == "a,x,b", table.concat(insertList, ","))

local sorted = { 5, 2, 9, 1 }
table.sort(sorted, function(a, b) return a > b end)
check("table.sort comparator", table.concat(sorted, ",") == "9,5,2,1", table.concat(sorted, ","))
check("string.byte returns a list", select("#", string.byte("ABC", 1, 3)) == 3)
check("tables compare by identity", ({} == {}) == false)

local function range(n) local i = 0 return function() i = i + 1 if i <= n then return i end end end
local rangeOut = ""
for v in range(3) do rangeOut = rangeOut .. v end
check("generic for over an iterator closure", rangeOut == "123", rangeOut)
local nextOut = ""
for k in next, { a = 1, b = 2 } do nextOut = nextOut .. k end
check("generic for with next", nextOut == "ab" or nextOut == "ba", nextOut)
-- 21. table.remove must capture the length before shifting: the old loop
--     re-read `#t` after every move and stopped one element early, so
--     `table.remove(t, 1)` left the last element in place and returned nil.
local t = { 1, 2, 3, 4 }
local removed = table.remove(t, 1)
check("table.remove(t, 1) returns the head", removed == 1, tostring(removed))
check("table.remove(t, 1) shifts every element",
    #t == 3 and t[1] == 2 and t[2] == 3 and t[3] == 4,
    "#t=" .. tostring(#t) .. " t=" .. table.concat(t, ","))
local last = table.remove(t)
check("table.remove(t) returns the tail", last == 4, tostring(last))
check("table.remove(t) shrinks the list",
    #t == 2 and t[1] == 2 and t[2] == 3,
    "#t=" .. tostring(#t) .. " t=" .. table.concat(t, ","))
local u = { "a", "b", "c" }
local middle = table.remove(u, 2)
check("table.remove(t, i) returns the middle",
    middle == "b" and #u == 2 and u[1] == "a" and u[2] == "c",
    tostring(middle) .. " #u=" .. tostring(#u) .. " u=" .. table.concat(u, ","))
local v = { 1, 2, 3 }
local gone = table.remove(v, 3)
check("table.remove(t, #t) returns the tail",
    gone == 3 and #v == 2 and v[1] == 1 and v[2] == 2,
    tostring(gone) .. " #v=" .. tostring(#v))

-- 22. `break` inside a numeric for used to leak into the enclosing loop when
--     the broken iteration was the last one (the flag was only cleared at the
--     top of the next iteration). The pattern engine hit this on every
--     capture group, so string.match/gsub stopped after the first one.
local outer = 0
local wi = 1
while wi <= 3 do
    for k = 1, 1 do
        if k == 1 then break end
    end
    outer = outer + 1
    wi = wi + 1
end
check("break in a for keeps the enclosing while going", outer == 3, tostring(outer))
local lastHit = 0
for k = 4, 1, -1 do
    if k == 2 then lastHit = k; break end
end
check("break in a descending for sets the value", lastHit == 2, tostring(lastHit))

-- 23. Generic `for` over a function iterator (standard Lua, used by
--     string.gmatch and io.lines): call until the function returns nil.
local function pairIter()
    local i = 0
    return function()
        i = i + 1
        if i == 1 then return "a", 1 end
        if i == 2 then return "b", 2 end
        return nil
    end
end
local keys, values = {}, {}
for k, v in pairIter() do
    keys[#keys + 1] = k
    values[#values + 1] = v
end
check("function iterators return multiple values",
    table.concat(keys, "") == "ab" and values[1] == 1 and values[2] == 2,
    table.concat(keys, "") .. "/" .. tostring(values[1]) .. tostring(values[2]))

mark("FAILURES=" .. fails)
canvas:close()
