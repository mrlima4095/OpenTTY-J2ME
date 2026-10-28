#!/bin/lua
-- alert - display an LCDUI alert, or a form with a gauge.
--
-- Usage:
--   alert [title] [text] [options]
--
-- Options:
--   -g, --gauge value[/max]   show a form with a gauge (a bare number is a
--                             percentage: -g 75 is 75/100)
--   -i, --interactive         let the user drag the gauge
--   -t, --type kind           info, warning, error, alarm or confirm
--                             (alerts only)
--   -m, --timeout ms          alert timeout in milliseconds; 0 (default)
--                             keeps the alert until the user dismisses it
--   -k, --ticker text         scrolling ticker on the screen
--   -b, --button label        extra command button, repeatable
--   -v, --vibrate ms          vibrate when the screen opens
--   -h, --help                show this help
--
-- MIDP alerts cannot contain items, so -g switches to a Form holding the text
-- and the gauge. Every -b button prints "alert: <label>" (plus the gauge value
-- on the form variant) and closes the screen.
--
-- Examples:
--   alert "Build" "Done" -v 300
--   alert "Battery" "Low charge" -t warning -m 2000
--   alert "Install" "Unpacking files" -g 75 -i -b Retry -b Cancel

os.setproc("name", "alert")

local function usage()
    print("alert [title] [text] [options]")
    print("  -g, --gauge value[/max]  show a form with a gauge instead of an alert")
    print("  -i, --interactive        let the user drag the gauge")
    print("  -t, --type kind          info, warning, error, alarm or confirm")
    print("  -m, --timeout ms         alert timeout, 0 = wait for a command (default)")
    print("  -k, --ticker text        scrolling ticker on the screen")
    print("  -b, --button label       extra command button (repeatable)")
    print("  -v, --vibrate ms         vibrate when the screen opens")
    print("  -h, --help               show this help")
end

local positional, buttons = {}, {}
local gaugeValue, gaugeMax, interactive = nil, 100, false
local kind, timeout, ticker, vibrate = "info", 0, nil, 0
local index = 1

local function nextValue()
    index = index + 1
    return arg[index]
end

while index <= #arg do
    local token = arg[index]

    if token == "-h" or token == "--help" then
        usage()
        return 0
    elseif token == "-g" or token == "--gauge" then
        local spec = string.trim(nextValue() or "")
        local slash = string.find(spec, "/")

        if slash then
            gaugeValue = tonumber(string.trim(string.sub(spec, 1, slash - 1)))
            gaugeMax = tonumber(string.trim(string.sub(spec, slash + 1)))
        else
            gaugeValue, gaugeMax = tonumber(spec), 100
        end

        if gaugeValue == nil then
            print("alert: invalid gauge value: " .. spec)
            return 1
        end
        if gaugeMax == nil or gaugeMax < 1 then gaugeMax = 100 end
        if gaugeValue < 0 then gaugeValue = 0 end
        if gaugeValue > gaugeMax then gaugeValue = gaugeMax end
    elseif token == "-i" or token == "--interactive" then
        interactive = true
    elseif token == "-t" or token == "--type" then
        kind = string.trim(nextValue() or "info")
    elseif token == "-m" or token == "--timeout" then
        timeout = tonumber(nextValue() or "0") or 0
    elseif token == "-k" or token == "--ticker" then
        ticker = nextValue()
    elseif token == "-b" or token == "--button" then
        local label = nextValue()
        if label then buttons[#buttons + 1] = label end
    elseif token == "-v" or token == "--vibrate" then
        vibrate = tonumber(nextValue() or "500") or 500
    elseif string.startswith(token, "-") then
        print("alert: unknown option " .. token)
        usage()
        return 1
    else
        positional[#positional + 1] = token
    end

    index = index + 1
end

local title = positional[1] or "Alert"
local text = ""
for i = 2, #positional do
    if i > 2 then text = text .. " " end
    text = text .. positional[i]
end

local previous = graphics.getCurrent()
local switch = graphics.new("command", { label = "Switch to...", type = "screen", priority = 3 })

local function close()
    if previous then graphics.display(previous) end
    os.exit(0)
end

local function addButtons(screen, handler)
    for i = 1, #buttons do
        local label = buttons[i]
        local cmd = graphics.new("command", { label = label, type = "ok", priority = 2 })
        graphics.addCommand(screen, cmd)
        handler[cmd] = function (value)
            if value == nil then
                print("alert: " .. label)
            else
                print("alert: " .. label .. " " .. tostring(value))
            end
            close()
        end
    end
    graphics.addCommand(screen, switch)
    handler[switch] = graphics.taskmngr
end

if vibrate > 0 then graphics.vibrate(vibrate) end

if gaugeValue == nil then
    -- Plain alert: type and timeout are honoured.
    local options = { type = kind, timeout = timeout }
    local screen = graphics.new("alert", title, text, options)
    if ticker then graphics.SetTicker(screen, ticker) end

    if timeout > 0 then
        -- A timed alert dismisses itself and returns to the previous screen,
        -- so the process does not stay around to handle commands.
        if previous then
            graphics.display(screen, previous)
        else
            graphics.display(screen)
        end
        -- The alert dismisses itself, so the process is not needed anymore.
        os.exit(0)
    end

    local handler = {}
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    graphics.addCommand(screen, back)
    handler[back] = close
    addButtons(screen, handler)

    graphics.handler(screen, handler)
    os.setproc("screen", screen)
    graphics.display(screen)
    return 0
end

-- Gauge variant: MIDP alerts cannot hold items, so build a form instead.
local screen = graphics.new("screen", title)
if text ~= "" then
    graphics.append(screen, { type = "text", label = "", value = text })
end
graphics.append(screen, { type = "gauge", label = "", interactive = interactive, maxValue = gaugeMax, value = gaugeValue })
if ticker then graphics.SetTicker(screen, ticker) end

local handler = {}
local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
graphics.addCommand(screen, back)
handler[back] = close
addButtons(screen, handler)

graphics.handler(screen, handler)
os.setproc("screen", screen)
graphics.display(screen)
