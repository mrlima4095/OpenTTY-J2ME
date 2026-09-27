#!/bin/lua

-- gobuster - web content discovery (dirb/feroxbuster-style) for OpenTTY.
--   gobuster <url>              quick scan with built-in wordlist
--   gobuster <url> -w <file>    scan with a wordlist file
--   gobuster <url> -t <delay>   delay between requests (ms)
--   gobuster <url> -s           live screen mode
--   gobuster -h | --help

local version = "1.1.0"

os.setproc("name", "gobuster")

local default_wordlist = {
    "admin", "api", "backup", "blog", "cache", "config", "css", "data",
    "dev", "docs", "download", "favicon.ico", "fonts", "home", "images",
    "img", "index.html", "index.php", "js", "login", "media", "old",
    "robots.txt", "sitemap.xml", "src", "static", "test", "tmp", "upload",
    "uploads", "user", "wp-admin"
}

local function normalize(url)
    if string.startswith(url, "http://") or string.startswith(url, "https://") then
        return url
    end
    return "http://" .. url
end

local function show_help()
    print("OpenTTY Content Finder v" .. version)
    print("")
    print("Usage: gobuster <url> [-w file] [-t delay_ms]")
    print("")
    print("Options:")
    print("  -w <file>    wordlist file (one path per line)")
    print("  -t <delay>   delay between requests in ms (default 0)")
    print("  -s           live screen mode")
    print("  --screen     live screen mode")
    print("")
    print("Example:")
    print("  gobuster http://opentty.fun")
    print("  gobuster http://example.com -w /home/words.txt -t 200")
end

if arg[1] == nil or arg[1] == "-h" or arg[1] == "--help" then
    show_help()
    if arg[1] ~= nil then os.exit(0) end
    os.exit(2)
end

local base = normalize(arg[1])
if string.sub(base, -1) == "/" then
    base = string.sub(base, 1, -2)
end

local words = {}
local has_wordlist = false
local delay = 0
local screen_mode = false

local i = 2
while arg[i] ~= nil do
    if arg[i] == "-s" or arg[i] == "--screen" then
        screen_mode = true
    elseif arg[i] == "-w" then
        i = i + 1
        local wf = arg[i]
        if wf == nil then
            print("gobuster: option requires an argument -- w")
            os.exit(2)
        end
        local content = io.read(os.join(wf))
        if content == nil or content == "" then
            print("gobuster: " .. wf .. ": not found")
            os.exit(127)
        end
        local lines = string.split(content, "\n")
        for j = 1, #lines do
            local w = string.trim(lines[j])
            if w ~= "" then table.insert(words, w) end
        end
        has_wordlist = true
    elseif arg[i] == "-t" then
        i = i + 1
        local d = arg[i]
        if d == nil then
            print("gobuster: option requires an argument -- t")
            os.exit(2)
        end
        local ok, dv = pcall(tonumber, d)
        if ok then
            if dv ~= nil and dv >= 0 then delay = dv end
        end
    end
    i = i + 1
end

if not has_wordlist then
    for j = 1, #default_wordlist do
        table.insert(words, default_wordlist[j])
    end
end

local function run_screen()
    local previous = graphics.getCurrent()
    local screen = graphics.new("screen", "gobuster: " .. base)
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    local stop = graphics.new("command", { label = "Stop", type = "stop", priority = 1 })
    local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 2 })
    local status = graphics.new("buffer", { label = "Status", value = "Preparing scan...", style = "monospace" })
    local results = graphics.new("buffer", { label = "Found", value = "", style = "monospace" })
    local running = true

    local function add_result(text)
        local current = graphics.GetText(results) or ""
        if current ~= "" then
            graphics.SetText(results, current .. "\n" .. text)
        else
            graphics.SetText(results, text)
        end
    end

    graphics.append(screen, status)
    graphics.append(screen, results)
    graphics.addCommand(screen, back)
    graphics.addCommand(screen, stop)
    graphics.addCommand(screen, switch)
    graphics.SetTicker(screen, "Scanning " .. base .. "...")
    graphics.handler(screen, {
        [back] = function()
            running = false
            graphics.display(previous)
        end,
        [stop] = function()
            running = false
            graphics.SetTicker(screen, "Stopping gobuster...")
        end,
        [switch] = graphics.taskmngr
    })

    os.setproc("screen", screen)
    graphics.display(screen)

    java.run(function()
        local t0 = java.midlet.uptime()
        local found = 0

        for wi = 1, #words do
            if running then
                local word = words[wi]
                local url = base .. "/" .. word
                local progress = tostring(wi) .. "/" .. tostring(#words) .. ": /" .. word
                graphics.SetText(status, progress)
                graphics.SetTicker(screen, "Scanning " .. progress)

                local ok, body, http_status = pcall(socket.http.get, url)
                if ok then
                    if http_status ~= nil and http_status ~= 404 then
                        local size = 0
                        if body ~= nil then size = #body end
                        found = found + 1
                        add_result(tostring(http_status) .. "  " .. tostring(size) .. "B  /" .. word)
                    end
                end

                if delay > 0 and wi < #words then java.sleep(delay) end
            else
                break
            end
        end

        local ms = java.midlet.uptime() - t0
        if running then
            graphics.SetText(status, "Done: " .. found .. " of " .. #words .. " found in " .. ms .. "ms")
            graphics.SetTicker(screen, "Gobuster complete")
        else
            graphics.SetText(status, "Stopped: " .. found .. " found")
            graphics.SetTicker(screen, "Gobuster stopped")
        end
    end, "gobuster")
end

if screen_mode then
    run_screen()
    return
end

print("Gobuster v" .. version)
print("Target : " .. base)
print("Words  : " .. #words)
print("")

local found = 0
local t0 = java.midlet.uptime()

for i = 1, #words do
    local word = words[i]
    local url = base .. "/" .. word
    local ok, body, status = pcall(socket.http.get, url)

    if ok then
        if status ~= nil and status ~= 404 then
            local size = 0
            if body ~= nil then size = #body end
            found = found + 1
            print(tostring(status) .. "  " .. tostring(size) .. "B  /" .. word)
        end
    end

    if delay > 0 and i < #words then java.sleep(delay) end
end

local ms = java.midlet.uptime() - t0
print("")
print("Done: " .. found .. " of " .. #words .. " found in " .. ms .. "ms")

if found > 0 then os.exit(0) end
os.exit(101)
