#!/bin/lua

-- fetch - System information (neofetch-style) for OpenTTY.
-- Shows a logo + OS/device info, straight from the runtime env vars.

local version = "1.0.0"

os.setproc("name", "fetch")

local function env(name, fallback)
    local value = os.getenv(name)
    if value == nil or value == "" then return fallback end
    return value
end

local function prop(name, fallback)
    local ok, value = pcall(getAppProperty, name)
    if not ok then return fallback end
    if value == nil or value == "" then return fallback end
    return value
end

-- Tiny 5-wide figlet font: only the letters used by the logo.
local GLYPHS = {
    ["O"] = { " ### ", "#   #", "#   #", "#   #", " ### " },
    ["P"] = { "#### ", "#   #", "#### ", "#    ", "#    " },
    ["E"] = { "#####", "#    ", "#### ", "#    ", "#####" },
    ["N"] = { "#   #", "##  #", "# # #", "#  ##", "#   #" },
    ["T"] = { "#####", "  #  ", "  #  ", "  #  ", "  #  " },
    ["Y"] = { "#   #", " # # ", "  #  ", "  #  ", "  #  " }
}

local function render(word)
    local rows = { "", "", "", "", "" }
    for i = 1, #word do
        local glyph = GLYPHS[string.sub(word, i, i)]
        if glyph == nil then glyph = GLYPHS["E"] end
        for r = 1, 5 do
            rows[r] = rows[r] .. glyph[r]
            if i < #word then rows[r] = rows[r] .. " " end
        end
    end
    return rows
end

local function logo()
    local left = render("OPEN")
    local right = render("TTY")
    for r = 1, 5 do
        print(left[r] .. "    " .. right[r])
    end
end

local function uptime_text()
    local ok, ms = pcall(java.midlet.uptime)
    if not ok then return "unknown" end
    if ms < 0 then ms = 0 end
    local secs = (ms - ms % 1000) / 1000
    local mins = (secs - secs % 60) / 60
    local rest = secs - mins * 60
    return tostring(mins) .. "m " .. tostring(rest) .. "s"
end

local function memory_text()
    local total = tonumber(collectgarbage("total")) or 0
    local used = tonumber(collectgarbage("count")) or 0
    local free = tonumber(collectgarbage("free")) or 0
    return tostring(total) .. "Kb total, " .. tostring(used) .. "Kb used, " .. tostring(free) .. "Kb free"
end

local function package_count()
    local content = io.read("/etc/sources")
    if content == nil or content == "" then return "?" end
    local ok, data = pcall(load, content)
    if not ok then return "?" end
    if type(data) ~= "table" then return "?" end
    local n = 0
    if data.mirror ~= nil and type(data.mirror) == "table" then
        for _ in pairs(data.mirror) do n = n + 1 end
    end
    return tostring(n)
end

if arg[1] == "-h" or arg[1] == "--help" then
    print("fetch - system information (neofetch-style), OpenTTY v" .. version)
    os.exit(0)
end

local hostname = env("HOSTNAME", "localhost")
local user = env("USER", "user")
local type_ = env("TYPE", "OpenTTY")
local version_ = env("VERSION", "unknown")
local config = env("CONFIG", "CLDC-1.0")
local profile = env("PROFILE", "MIDP-2.0")
local build = prop("MIDlet-Build", env("MIDlet-Build", ""))

print(user .. "@" .. hostname)
print("")
logo()
print("")

print("OS       : " .. type_ .. " " .. version_)
print("Host     : " .. hostname)
if build ~= "" then print("Kernel   : " .. build) end
print("Uptime   : " .. uptime_text())
print("Packages : " .. package_count())
print("Shell    : " .. tostring(_VERSION))
print("Emulator : RISC-V RV32IM")
print("Memory   : " .. memory_text())
print("Config   : " .. config)
print("Profile  : " .. profile)