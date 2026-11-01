#!/bin/lua

-- lock - password lock screen.
--
-- Replaces the current screen with a "Login" form and deliberately does NOT
-- register a "Switch to..." command: while the lock is up there is no way back
-- to the terminal (or to any other screen) except the account password.
-- "Exit" shuts OpenTTY down.

os.setproc("name", "lock")

local previous = graphics.getCurrent()
local screen = graphics.new("screen", "Login")
local info = graphics.new("buffer", { label = "OpenTTY", style = "monospace" })
local password = graphics.new("field", { label = "Password", value = "", length = 64, mode = "password" })
local login = graphics.new("command", { label = "Login", type = "ok", priority = 1 })
local quit = graphics.new("command", { label = "Exit", type = "exit", priority = 2 })

local function env(name, fallback)
    local value = os.getenv(name)
    if value == nil or value == "" then return fallback end
    return value
end

-- The shell scope follows `su`, so this is the user the terminal runs as.
local function currentUser()
    local ok, scope = pcall(os.scope)
    if ok and type(scope) == "table" then
        local name = scope["USER"]
        if name ~= nil and name ~= "" then return name end
    end
    return env("USER", "user")
end

local function uptimeText()
    local ok, ms = pcall(java.midlet.uptime)
    if not ok or ms == nil or ms < 0 then return "unknown" end
    local secs = (ms - ms % 1000) / 1000
    local mins = (secs - secs % 60) / 60
    return tostring(mins) .. "m " .. tostring(secs - mins * 60) .. "s"
end

local hostname = env("HOSTNAME", "")
if hostname == "" then hostname = string.trim(io.read("/etc/hostname") or "") end
if hostname == "" then hostname = "localhost" end

local user = currentUser()

-- Uptime and memory are read every time the text is built, so a failed login
-- shows fresh values.
local function infoText()
    return table.concat({
        user .. "@" .. hostname,
        "",
        "Host     : " .. hostname,
        "User     : " .. user,
        "OpenTTY  : " .. env("VERSION", "unknown") .. " (" .. env("CONFIG", "CLDC-1.0") .. "/" .. env("PROFILE", "MIDP-2.0") .. ")",
        "Build    : " .. tostring(java.midlet.build or "unknown"),
        "Uptime   : " .. uptimeText(),
        "Memory   : " .. tostring(collectgarbage("count")) .. "Kb used, " .. tostring(collectgarbage("free")) .. "Kb free"
    }, "\n")
end

local function refresh()
    graphics.SetText(info, infoText())
end

-- Timed alert: it dismisses itself after 5 seconds and falls back to the
-- lock form. Failures never touch the information StringItem.
local function warn(message)
    graphics.display(graphics.new("alert", "Login failed", message, { type = "error", timeout = 5000 }), screen)
end

-- Back to the terminal (or whatever was on screen when the lock started).
local function unlock()
    local target = graphics.db["xterm"]
    if target == nil then target = previous end
    if target ~= nil then graphics.display(target) end
    os.exit(0)
end

-- Killing init (PID 1) is the runtime's way to shut the MIDlet down.
local function closeMIDlet()
    local ok, result = pcall(os.request, 1, "sendsig", { pid = "1", signal = "9" })
    if not ok or result ~= 0 then
        refresh()
        warn("Cannot close OpenTTY (permission denied).")
    end
end

graphics.append(screen, info)
graphics.append(screen, password)
graphics.addCommand(screen, login)
graphics.addCommand(screen, quit)
graphics.handler(screen, {
    [login] = function (secret)
        secret = string.trim(secret or "")

        if secret == "" then
            refresh()
            warn("Password required.")
            return
        end

        local ok, granted = pcall(os.request, 1, "passwd", secret)
        if ok and granted == true then
            unlock()
            return
        end

        graphics.SetText(password, "")
        refresh()
        warn("Permission denied: wrong password.")
        pcall(graphics.vibrate, 200)
    end,
    [quit] = closeMIDlet
})

refresh()
os.setproc("screen", screen)
graphics.display(screen)
