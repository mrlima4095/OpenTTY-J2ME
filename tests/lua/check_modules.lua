-- Rock payload checks on the real interpreter (desktop shim, headless).
--   Usage: tests/lua/run_modules.sh
-- The runner stages apps/1.19/lua/*.lua under /mnt/opentty/rocks/ and this
-- script loads each payload the same way `require` does on-device (the file
-- returns a table). Writes /mnt/opentty/modules-result.txt (host:
-- data/mnt/opentty/).
local OUT = "/mnt/opentty/modules-result.txt"
local ROOT = "/mnt/opentty/rocks/"
local fails = 0

local function mark(line) io.write(line .. "\n", OUT, "a") end
local function check(name, cond, detail)
    if cond then mark("ok   " .. name)
    else fails = fails + 1 mark("FAIL " .. name .. (detail and (": " .. tostring(detail)) or "")) end
end

local function load_rock(name)
    local path = ROOT .. name .. ".lua"
    local code = io.read(path)
    if code == nil or code == "" then
        mark("FAIL cannot read " .. path)
        fails = fails + 1
        return nil
    end
    -- The OpenTTY `load` compiles *and runs* the chunk (it returns the chunk's
    -- results, unlike stock Lua), so pcall(load, code) is the whole thing.
    local ok, value = pcall(load, code)
    if not ok then
        mark("FAIL " .. name .. " raised: " .. tostring(value))
        fails = fails + 1
        return nil
    end
    return value
end

-- 1. Every payload parses and returns a table.
local mathrock = load_rock("math")
local graphicsrock = load_rock("graphics")
local kernelrock = load_rock("kernel")
local httpdrock = load_rock("httpd")
check("math rock loads", type(mathrock) == "table")
check("graphics rock loads", type(graphicsrock) == "table")
check("kernel rock loads", type(kernelrock) == "table")
check("httpd rock loads", type(httpdrock) == "table")

-- 2. math: the functions the native runtime lacks.
if type(mathrock) == "table" then
    local M = mathrock
    check("math.floor", M.floor(2.7) == 2 and M.floor(-2.1) == -3,
        tostring(M.floor(2.7)) .. "," .. tostring(M.floor(-2.1)))
    check("math.ceil", M.ceil(2.1) == 3 and M.ceil(-2.7) == -2,
        tostring(M.ceil(2.1)) .. "," .. tostring(M.ceil(-2.7)))
    check("math.round", M.round(2.5) == 3 and M.round(2.4) == 2)
    check("math.trunc", M.trunc(2.9) == 2 and M.trunc(-2.9) == -2)
    check("math.sqrt", M.sqrt(16) == 4)
    check("math.gcd/lcm", M.gcd(12, 18) == 6 and M.lcm(4, 6) == 12)
    check("math.clamp", M.clamp(9, 1, 5) == 5 and M.clamp(-1, 1, 5) == 1)
    check("math.hypot", M.isclose(M.hypot(3, 4), 5, 0.0001))
    check("math.sin(0)", M.sin(0) == 0)
    check("math.cos(0)", M.isclose(M.cos(0), 1, 0.0001))
    check("math.log10", M.isclose(M.log10(1000), 3, 0.0001))
    check("math.min/max", M.min(3, 1, 2) == 1 and M.max(3, 1, 2) == 3)
    check("math.factorial", M.factorial(5) == 120)
    check("math does not replace the native table", math.sqrt(9) == 3)
end

-- 3. graphics: the viewers are installed on the native `graphics` table.
if type(graphicsrock) == "table" then
    local G = graphicsrock
    check("graphics.RenderMarkdown", type(G.RenderMarkdown) == "function")
    check("graphics.RenderHTML", type(G.RenderHTML) == "function")
    check("graphics.Open", type(G.Open) == "function")
    check("graphics keeps native constructors", type(G.new) == "function")
    check("graphics is the shared table", G == graphics)
end

-- 4. kernel: typed wrappers plus the callable form.
if type(kernelrock) == "table" then
    local K = kernelrock
    check("kernel.SIGTERM", K.SIGTERM == "15")
    check("kernel.proc", type(K.proc) == "function")
    check("kernel.signal", type(K.signal) == "function")
    check("kernel.useradd", type(K.useradd) == "function")
    check("kernel is callable", type(K) == "table" and getmetatable(K) ~= nil)
end

-- 5. httpd: routing API.
if type(httpdrock) == "table" then
    local H = httpdrock
    check("httpd.route", type(H.route) == "function")
    check("httpd.get", type(H.get) == "function")
    check("httpd.post", type(H.post) == "function")
    check("httpd.static", type(H.static) == "function")
end

-- 6. string: load last, because it replaces string.find/match globally. The
--    rock must keep the native helpers and add the missing ones.
local stringrock = load_rock("string")
check("string rock loads", type(stringrock) == "table")
if type(stringrock) == "table" then
    local S = stringrock
    check("string.format", S.format("%s=%d", "a", 7) == "a=7", tostring(S.format("%s=%d", "a", 7)))
    check("string.rep", S.rep("ab", 3) == "ababab", tostring(S.rep("ab", 3)))
    check("string.rep with separator", S.rep("ab", 2, "-") == "ab-ab")
    check("string.lpad/rpad", S.lpad("7", 3, "0") == "007" and S.rpad("7", 3, ".") == "7..")
    check("string.join", S.join({ "a", "b" }, ",") == "a,b")
    check("string pattern find", S.find("hello world", "wor") == 7, tostring(S.find("hello world", "wor")))
    check("string plain find", S.find("a.b", ".", 1, true) == 2, tostring(S.find("a.b", ".", 1, true)))
    check("string.match", S.match("key=value", "(%w+)=(%w+)") == "key", tostring(S.match("key=value", "(%w+)=(%w+)")))
    check("string.gsub", S.gsub("a1b2", "%d", "#") == "a#b#", tostring(S.gsub("a1b2", "%d", "#")))
    check("string.gsub with captures", S.gsub("a1b2", "(%a)(%d)", "%2%1") == "1a2b",
        tostring(S.gsub("a1b2", "(%a)(%d)", "%2%1")))
    local words = {}
    for w in S.gmatch("one two three", "%a+") do words[#words + 1] = w end
    check("string.gmatch", table.concat(words, ",") == "one,two,three", table.concat(words, ","))
    check("string keeps native helpers",
        string.upper("ab") == "AB" and string.sub("abc", 2) == "bc" and
        string.startswith("abc", "ab") and string.endswith("abc", "bc"))
    check("string.len still native", string.len("abcd") == 4)
    check("string.reverse still native", string.reverse("abc") == "cba")
end

-- 7. json: deterministic encoder/decoder with a null sentinel.
local jsonrock = load_rock("json")
check("json rock loads", type(jsonrock) == "table")
if type(jsonrock) == "table" then
    local J = jsonrock
    check("json object", J.encode({ b = 2, a = 1 }) == '{"a":1,"b":2}', J.encode({ b = 2, a = 1 }))
    check("json array", J.encode({ 1, 2, 3 }) == "[1,2,3]", J.encode({ 1, 2, 3 }))
    check("json null", J.encode({ n = J.null }) == '{"n":null}', J.encode({ n = J.null }))
    check("json string escapes", J.encode("a\"b\n") == '"a\\"b\\n"', J.encode("a\"b\n"))
    local value = J.decode('{"a":[1,2],"b":null,"c":"x","d":true}')
    check("json decode", value.a[2] == 2 and J.is_null(value.b) and value.c == "x" and value.d == true)
    check("json roundtrip", J.encode(J.decode('{"x":[1,2,3]}')) == '{"x":[1,2,3]}')
    check("json pretty", J.pretty({ a = 1 }) == '{\n  "a": 1\n}', J.pretty({ a = 1 }))
    check("json try_decode", select(1, J.try_decode("{bad}")) == nil)
end

-- 8. tablex: functional helpers.
local tablexrock = load_rock("tablex")
check("tablex rock loads", type(tablexrock) == "table")
if type(tablexrock) == "table" then
    local T = tablexrock
    check("tablex map", table.concat(T.map({ 1, 2, 3 }, function(v) return v * 2 end), ",") == "2,4,6")
    check("tablex filter", table.concat(T.filter({ 1, 2, 3, 4 }, function(v) return v % 2 == 0 end), ",") == "2,4")
    check("tablex reduce", T.reduce({ 1, 2, 3, 4 }, function(a, b) return a + b end) == 10)
    check("tablex reduce init", T.reduce({ 1, 2, 3 }, function(a, b) return a + b end, 10) == 16)
    check("tablex sortby", T.sortby({ { n = 3 }, { n = 1 }, { n = 2 } }, function(v) return v.n end)[1].n == 1)
    check("tablex sortby desc", T.sortby({ 1, 3, 2 }, nil, true)[1] == 3)
    check("tablex unique", table.concat(T.unique({ 1, 2, 1, 3, 2 }), ",") == "1,2,3")
    check("tablex flatten", table.concat(T.flatten({ 1, { 2, 3 }, { 4, { 5 } } }, 2), ",") == "1,2,3,4,5")
    check("tablex range", table.concat(T.range(1, 6, 2), ",") == "1,3,5")
    check("tablex range down", table.concat(T.range(3, 1, -1), ",") == "3,2,1")
    check("tablex indexof", T.indexof({ "a", "b" }, "b") == 2)
    check("tablex contains", T.contains({ "a" }, "a") == true)
    check("tablex equal", T.equal({ a = { 1, 2 } }, { a = { 1, 2 } }) == true)
    check("tablex equal false", T.equal({ a = 1 }, { a = 2 }) == false)
    check("tablex merge", T.merge({ a = 1 }, { b = 2 }).b == 2)
    check("tablex pick/omit", T.pick({ a = 1, b = 2, c = 3 }, { "a", "c" }).b == nil and
        T.omit({ a = 1, b = 2 }, { "b" }).b == nil)
    local tablex_keys = T.keys({ a = 1, b = 2 })
    check("tablex keys/size", #tablex_keys == 2 and T.size({ a = 1, b = 2 }) == 2)
    check("tablex slice", table.concat(T.slice({ 1, 2, 3, 4 }, 2, 3), ",") == "2,3")
    local copy = T.deepcopy({ a = { b = 1 } })
    copy.a.b = 2
    check("tablex deepcopy", copy.a.b == 2)
end

-- 9. base64: standard and URL-safe alphabets plus hex.
local base64rock = load_rock("base64")
check("base64 rock loads", type(base64rock) == "table")
if type(base64rock) == "table" then
    local B = base64rock
    check("base64 encode", B.encode("hello") == "aGVsbG8=", B.encode("hello"))
    check("base64 encode 2", B.encode("ab") == "YWI=")
    check("base64 decode", B.decode("aGVsbG8=") == "hello")
    check("base64 roundtrip", B.decode(B.encode("The quick brown fox")) == "The quick brown fox")
    check("base64 url", B.encode_url(string.char(251, 255)) == "-_8=", B.encode_url(string.char(251, 255)))
    check("base64 decode url", B.decode_url("-_8=") == string.char(251, 255))
    check("base64 hex", B.hex(string.char(0, 255, 97)) == "00ff61")
    check("base64 unhex", B.unhex("00ff61") == string.char(0, 255, 97))
    check("base64 keeps native", type(B.native) == "table")
end

-- 10. sha1 / sha256: standard vectors and HMAC.
local sha1rock = load_rock("sha1")
check("sha1 rock loads", type(sha1rock) == "table")
if type(sha1rock) == "table" then
    local S = sha1rock
    check("sha1 empty", S.sha1("") == "da39a3ee5e6b4b0d3255bfef95601890afd80709", S.sha1(""))
    check("sha1 abc", S.sha1("abc") == "a9993e364706816aba3e25717850c26c9cd0d89d", S.sha1("abc"))
    check("sha1 long", S.sha1("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq") ==
        "84983e441c3bd26ebaae4aa1f95129e5e54670f1")
    check("sha1 callable", S("abc") == "a9993e364706816aba3e25717850c26c9cd0d89d")
    check("sha1 hmac", S.hmac("key", "The quick brown fox jumps over the lazy dog") ==
        "de7c9b85b8b78aa6bc8a7a36f70a90701c9db4d9")
    local sha1_raw = S.raw("abc")
    check("sha1 raw length", #sha1_raw == 20)
end

local sha256rock = load_rock("sha256")
check("sha256 rock loads", type(sha256rock) == "table")
if type(sha256rock) == "table" then
    local S = sha256rock
    check("sha256 empty", S.sha256("") ==
        "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", S.sha256(""))
    check("sha256 abc", S.sha256("abc") ==
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", S.sha256("abc"))
    check("sha256 long", S.sha256("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq") ==
        "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1")
    check("sha256 callable", S("abc") ==
        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    check("sha256 hmac", S.hmac("key", "The quick brown fox jumps over the lazy dog") ==
        "f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8")
    local sha256_raw = S.raw("abc")
    check("sha256 raw length", #sha256_raw == 32)
end

-- 11. term: ANSI-aware widths and simple widgets.
local termrock = load_rock("term")
check("term rock loads", type(termrock) == "table")
if type(termrock) == "table" then
    local T = termrock
    local colored = T.color("red", "abc")
    check("term strip", T.strip(colored) == "abc", T.strip(colored))
    check("term width", T.width(colored) == 3, tostring(T.width(colored)))
    check("term pad", T.pad("abc", 5) == "abc  " and T.pad("abc", 5, "right") == "  abc")
    check("term bar", string.find(T.bar(5, 10, 10), "[#####", 1, true) == 1, T.bar(5, 10, 10))
    check("term hr", T.hr(4) == "----")
    check("term wrap", table.concat(T.wrap("a b c", 3), "|") == "a b|c", table.concat(T.wrap("a b c", 3), "|"))
    check("term list", T.list({ "a", "b" }) == "- a\n- b")
    check("term spinner", T.spinner(0) == "|" and T.spinner(1) == "/")
    check("term table", string.find(T.table({ { "a", "b" }, { "c", "d" } }), "a  b", 1, true) ~= nil)
end

-- 12. argparse: flags, values, defaults and help.
local argparse = load_rock("argparse")
check("argparse rock loads", type(argparse) == "table")
if type(argparse) == "table" then
    local A = argparse
    local p = A.new("greet", "Say hello")
    p:option("-n", "--name", { type = "string", default = "world", help = "who to greet" })
    p:flag("-v", "--verbose", { help = "print more" })
    p:option("-c", "--count", { type = "int", default = 1 })
    p:argument("file", { help = "optional file" })
    local opts, err = p:parse({ "-n", "ana", "-v", "a.txt" })
    check("argparse parse", err == nil and opts.name == "ana" and opts.verbose == true and opts.file == "a.txt")
    check("argparse default", p:parse({}).name == "world")
    check("argparse int", p:parse({ "-c", "7" }).count == 7)
    check("argparse --name=value", p:parse({ "--name=bo" }).name == "bo")
    check("argparse clustered", p:parse({ "-vn", "x" }).name == "x")
    check("argparse separator", p:parse({ "--", "-n" }).args[1] == "-n")
    local bad, message = p:parse({ "--nope" })
    check("argparse unknown", bad == nil and type(message) == "string")
    check("argparse help", string.find(p:help(), "Usage: greet [options] [file]", 1, true) ~= nil)
end

-- 13. log: level filtering, formatting and sinks.
local logrock = load_rock("log")
check("log rock loads", type(logrock) == "table")
if type(logrock) == "table" then
    local L = logrock
    local captured = {}
    local sink = function(line, level, name)
        captured[#captured + 1] = { line = line, level = level, name = name }
    end
    local logger = L.new({ name = "app", level = "warn", sink = sink, timestamps = false })
    check("log filters debug", logger:debug("nope") == nil)
    check("log filters info", logger:info("nope") == nil)
    local line = logger:warn("disk", "full")
    check("log warn text", line == "[app] WARN disk full", tostring(line))
    check("log sink", #captured == 1 and captured[1].level == "warn" and captured[1].name == "app")
    local plain = L.new({ name = "plain", sink = sink, timestamps = false })
    check("log info", plain:info("hi") == "[plain] INFO hi")
    check("log child", logger:child("worker").name == "worker")
    logger:set_level("debug")
    check("log set_level", logger:debug("now") == "[app] DEBUG now")
end

-- 14. config: INI parsing, dotted keys and encoding.
local configrock = load_rock("config")
check("config rock loads", type(configrock) == "table")
if type(configrock) == "table" then
    local C = configrock
    local cfg = C.parse("; c\ntheme = dark\n\n[network]\nhost = localhost\nport = 8080\n")
    check("config global", cfg:get("theme") == "dark")
    check("config section", cfg:get("network.host") == "localhost")
    check("config number", cfg:get_number("network.port") == 8080)
    check("config default", cfg:get("nope", "fallback") == "fallback")
    check("config has", cfg:has("network.host") == true and cfg:has("network.nope") == false)
    cfg:set("network.timeout", 30)
    check("config set", cfg:get("network.timeout") == 30)
    check("config unset", cfg:unset("network.timeout") == true and cfg:has("network.timeout") == false)
    local config_sections = cfg:sections()
    check("config sections", #config_sections == 1)
    check("config encode", C.encode({ b = 2, a = "x" }) == "a = x\nb = 2\n", C.encode({ b = 2, a = "x" }))
    check("config roundtrip", C.parse(C.encode({ s = { y = 1 } })):get("s.y") == "1")
end

-- 15. template: variables, sections, inverted sections and partials.
local templaterock = load_rock("template")
check("template rock loads", type(templaterock) == "table")
if type(templaterock) == "table" then
    local T = templaterock
    check("template var", T.render("hi {{name}}", { name = "ana" }) == "hi ana")
    check("template dotted", T.render("{{user.name}}", { user = { name = "bo" } }) == "bo")
    check("template raw", T.render("{{{x}}}", { x = "<b>" }) == "<b>")
    check("template escape", T.render("{{x}}", { x = "<b>" }, { escape = true }) == "&lt;b&gt;")
    check("template comment", T.render("a{{! x }}b", {}) == "ab")
    check("template list", T.render("{{#items}}{{.}},{{/items}}", { items = { "a", "b" } }) == "a,b,")
    check("template inverted", T.render("{{^items}}empty{{/items}}", {}) == "empty")
    check("template nested", T.render("{{#a}}{{#b}}{{c}}{{/b}}{{/a}}", { a = { b = { c = "d" } } }) == "d")
    check("template partial", T.render("x {{>p}}", { v = 1 }, { partials = { p = "v={{v}}" } }) == "x v=1")
    local compiled = T.compile("{{a}}-{{b}}")
    check("template compile", compiled({ a = 1, b = 2 }) == "1-2" and compiled({ a = 3, b = 4 }) == "3-4")
end

-- 16. csv: quoting, headers and objects.
local csvrock = load_rock("csv")
check("csv rock loads", type(csvrock) == "table")
if type(csvrock) == "table" then
    local V = csvrock
    local rows = V.parse("name,age\nana,3\n")
    check("csv rows", #rows == 2 and rows[2][1] == "ana" and rows[2][2] == "3")
    local quoted = V.parse('a,"b,c","d""e"\n"multi\nline",x,\n')
    check("csv quoted comma", quoted[1][2] == "b,c")
    check("csv doubled quote", quoted[1][3] == 'd"e')
    check("csv multiline", quoted[2][1] == "multi\nline")
    check("csv crlf", V.parse("a,b\r\nc,d\r\n")[2][2] == "d")
    check("csv encode", V.encode({ { "a", "b" }, { "c", "d" } }) == "a,b\nc,d\n")
    check("csv encode quote", V.encode({ { "a,b" } }) == '"a,b"\n')
    check("csv objects", V.rows_to_objects(rows)[1].name == "ana")
    check("csv objects_to_rows", V.objects_to_rows({ { name = "z", age = 9 } }, { "name", "age" })[2][2] == "9")
end

-- 17. date: parsing, parts, arithmetic and relative text.
local daterock = load_rock("date")
check("date rock loads", type(daterock) == "table")
if type(daterock) == "table" then
    local D = daterock
    check("date leap", D.is_leap(2024) == true and D.is_leap(2025) == false and D.is_leap(1900) == false)
    check("date days_in_month", D.days_in_month(2024, 2) == 29 and D.days_in_month(2025, 4) == 30)
    local ts = D.parse("2026-10-30 12:34:56")
    check("date parse", ts ~= nil)
    local p = D.parts(ts)
    check("date parts", p.year == 2026 and p.month == 10 and p.day == 30 and
        p.hour == 12 and p.min == 34 and p.sec == 56 and p.yday == 303)
    check("date weekday", D.weekday_name(p.wday) == os.date("%A", ts),
        D.weekday_name(p.wday) .. "/" .. os.date("%A", ts))
    check("date format", D.format(ts, "%Y-%m-%d %H:%M:%S") == "2026-10-30 12:34:56")
    check("date iso", D.iso(ts) == "2026-10-30T12:34:56")
    check("date custom parse", D.date(D.parse("30/10/2026 08:05", "%d/%m/%Y %H:%M")) == "2026-10-30")
    check("date invalid", select(1, D.parse("2026-13-01")) == nil)
    check("date start_of_day", D.format(D.start_of_day(ts), "%H:%M:%S") == "00:00:00")
    check("date add", D.format(D.add(ts, 60), "%H:%M") == "12:35")
    check("date add_days", D.date(D.add_days(ts, 2)) == "2026-11-01")
    check("date diff", D.diff(ts + 100, ts) == 100)
    check("date relative", D.relative(ts, ts + 120) == "2 minutes ago")
    check("date relative future", D.relative(ts + 7200, ts) == "in 2 hours")
end

-- 18. re: pattern helpers on top of the string rock.
local rerock = load_rock("re")
check("re rock loads", type(rerock) == "table")
if type(rerock) == "table" then
    local R = rerock
    check("re findall", table.concat(R.findall("a1 b22 c333", "%a+"), ",") == "a,b,c")
    check("re findall digits", table.concat(R.findall("a1 b22", "%d+"), ",") == "1,22")
    check("re count", R.count("a1 b22 c333", "%d+") == 3)
    check("re test", R.test("abc", "b") == true and R.test("abc", "z") == false)
    check("re groups", table.concat(R.groups("key=value", "(%w+)=(%w+)"), ",") == "key,value")
    check("re matchall", R.matchall("a=1 b=2", "(%a+)=(%d+)")[2][2] == "2")
    check("re split", table.concat(R.split("a,b,,c", ","), "|") == "a|b||c")
    check("re split pattern", table.concat(R.split("a1b22c", "%d+"), "|") == "a|b|c")
    check("re split keep_empty", table.concat(R.split("a,b,,c", ",", { keep_empty = false }), "|") == "a|b|c")
    local replaced, total = R.replace("a1 b2", "%d", "#")
    check("re replace", replaced == "a# b#" and total == 2, tostring(replaced))
    check("re escape", R.escape("a.b*c") == "a%.b%*c" and R.replace("a.b", R.escape("."), "-") == "a-b")
end

-- 19. ui: callback wiring, checked against a stub `graphics` table so no real
--     screen is displayed (the module looks `graphics` up at call time).
local real_graphics = graphics
local fake_current = { name = "previous" }
local displayed = {}
graphics = {
    getCurrent = function() return fake_current end,
    new = function(kind, title, content, options)
        local data = content
        if kind == "gauge" then data = title end
        local screen = { kind = kind, title = title, content = content, options = options, items = {}, commands = {} }
        if kind == "gauge" and type(data) == "table" then
            screen.label = data.label
            screen.maxValue = data.maxValue
            screen.value = data.value
        end
        return screen
    end,
    append = function(screen, item) screen.items[#screen.items + 1] = item return screen end,
    addCommand = function(screen, cmd) screen.commands[#screen.commands + 1] = cmd return screen end,
    handler = function(screen, handlers) screen.handler = handlers return screen end,
    display = function(screen) displayed[#displayed + 1] = screen fake_current = screen return screen end,
    SetIndicator = function(alert, gauge) alert.indicator = gauge return alert end,
    SetGauge = function(gauge, value) gauge.value = value return gauge end
}
local uirock = load_rock("ui")
check("ui rock loads", type(uirock) == "table")
if type(uirock) == "table" then
    local U = uirock
    local picked, picked_index
    local menu = U.menu("Main", { "Open", { label = "Save", id = 7 } }, {
        on_select = function(label, index) picked, picked_index = label, index end
    })
    check("ui menu", menu.kind == "list" and #menu.items == 2 and menu.items[2] == "Save")
    menu.handler[menu.commands[1]]("Save")
    check("ui menu select", picked == "Save" and picked_index == 2)

    local typed
    local form = U.prompt("Name", "Your name", { on_submit = function(value) typed = value end })
    form.handler[form.commands[1]]("ana")
    check("ui prompt", form.kind == "form" and form.items[1].type == "field" and typed == "ana")

    local answered
    local confirm = U.confirm("Sure?", "Delete?", { on_yes = function() answered = "yes" end })
    confirm.handler[confirm.commands[1]]()
    check("ui confirm", confirm.options.type == "confirm" and answered == "yes")

    local notice = U.notify("saved")
    check("ui notify", notice.kind == "alert" and notice.options.timeout == 1500)

    local before = fake_current
    local progress = U.progress("Copy", "Working", { max = 10, value = 2 })
    progress:set(7)
    progress:inc(2)
    check("ui progress", progress.gauge.value == 9 and progress.gauge.maxValue == 10)
    progress:close()
    check("ui progress close", fake_current == before)
end
graphics = real_graphics

mark("FAILURES=" .. fails)
