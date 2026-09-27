#!/bin/lua

os.setproc("name", "wc")

local function usage()
    print("Usage: wc [options] [file ...]")
    print("  -l    count lines")
    print("  -w    count words")
    print("  -c    count characters")
    print("  (no flags: lines, words, chars)")
end

local flags = { l = false, w = false, c = false }
local files = {}
local has_flag = false
local i = 1

while arg[i] do
    local a = tostring(arg[i])
    if a == "-h" or a == "--help" then
        usage()
        os.exit(0)
    elseif string.startswith(a, "-") and string.len(a) > 1 then
        has_flag = true
        local opts = string.sub(a, 2)
        local j = 1
        while j <= string.len(opts) do
            local ch = string.sub(opts, j, j)
            if ch == "l" then flags.l = true
            elseif ch == "w" then flags.w = true
            elseif ch == "c" then flags.c = true
            else
                print("wc: invalid option -- " .. ch)
                os.exit(2)
            end
            j = j + 1
        end
    else
        table.insert(files, a)
    end
    i = i + 1
end

if not has_flag then
    flags.l = true
    flags.w = true
    flags.c = true
end

local function count_words(text)
    local n = 0
    local in_word = false
    local j = 1
    while j <= string.len(text) do
        local ch = string.sub(text, j, j)
        if ch == " " or ch == "\t" or ch == "\n" or ch == "\r" then
            in_word = false
        else
            if not in_word then
                n = n + 1
                in_word = true
            end
        end
        j = j + 1
    end
    return n
end

local function wc_file(path)
    local content = io.read(os.join(path))
    if not content then
        print("wc: " .. path .. ": not found")
        return nil
    end

    local lines = 0
    local words = 0
    local chars = string.len(content)

    if content ~= "" then
        local ls = string.split(content, "\n")
        lines = #ls
        for k = 1, #ls do
            words = words + count_words(ls[k])
        end
    end

    local out = ""
    if flags.l then out = out .. "  " .. tostring(lines) end
    if flags.w then out = out .. "  " .. tostring(words) end
    if flags.c then out = out .. "  " .. tostring(chars) end
    out = out .. "  " .. path
    print(out)

    return { lines = lines, words = words, chars = chars }
end

local total = { lines = 0, words = 0, chars = 0 }
local count = 0

for idx = 1, #files do
    local r = wc_file(files[idx])
    if r then
        total.lines = total.lines + r.lines
        total.words = total.words + r.words
        total.chars = total.chars + r.chars
        count = count + 1
    end
end

if count > 1 then
    local out = ""
    if flags.l then out = out .. "  " .. tostring(total.lines) end
    if flags.w then out = out .. "  " .. tostring(total.words) end
    if flags.c then out = out .. "  " .. tostring(total.chars) end
    out = out .. "  total"
    print(out)
end
