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

mark("FAILURES=" .. fails)
