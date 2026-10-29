#!/bin/lua
-- alert - display an LCDUI alert, optionally with a gauge indicator.
--
-- Usage:
--   alert [title] [text] [options]
--
-- Options:
--   -g, --gauge [value[/max]] show a gauge inside the alert (a bare number is
--                             a percentage: -g 75 is 75/100). Without a value
--                             the gauge runs continuously (MIDP
--                             Gauge.CONTINUOUS_RUNNING).
--   -c, --continuous          animate a value gauge while the alert is open
--   -i, --interactive         let the user drag the gauge (uses a form: MIDP
--                             alerts only accept a non-interactive gauge)
--   -t, --type kind           info, warning, error, alarm or confirm
--   -m, --timeout ms          alert timeout in milliseconds; 0 (default)
--                             keeps the alert until the user dismisses it
--   -k, --ticker text         scrolling ticker on the screen
--   -b, --button label        extra command button, repeatable
--   -v, --vibrate ms          vibrate when the screen opens
--   -h, --help                show this help
--
-- An alert can show one non-interactive gauge as its indicator
-- (Alert.setIndicator), so -g keeps the alert instead of switching to a form.
-- -i needs an interactive gauge, which cannot be an alert indicator, so it
-- falls back to a form. Every -b button prints "alert: <label>" and closes.
--
-- Examples:
--   alert "Build" "Done" -v 300
--   alert "Battery" "Low charge" -t warning -m 2000
--   alert "Install" "Unpacking files" -g 75 -b Cancel
--   alert "Copy" "Please wait" -g -c
--   alert "Sync" "Working" -g 30/64 -c -m 5000

os.setproc("name", "alert")

local function usage()
    print("alert [title] [text] [options]")
    print("  -g, --gauge [value[/max]] show a gauge in the alert, no value = running")
    print("  -c, --continuous         animate a value gauge while the alert is open")
    print("  -i, --interactive        let the user drag the gauge (uses a form)")
    print("  -t, --type kind          info, warning, error, alarm or confirm")
    print("  -m, --timeout ms         alert timeout, 0 = wait for a command (default)")
    print("  -k, --ticker text        scrolling ticker on the screen")
    print("  -b, --button label       extra command button (repeatable)")
    print("  -v, --vibrate ms         vibrate when the screen opens")
    print("  -h, --help               show this help")
end

local positional, buttons = {}, {}
local gaugeMode, gaugeValue, gaugeMax = nil, 0, 100
local interactive, animate = false, false
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
        local spec = arg[index + 1]
        if spec ~= nil then spec = string.trim(spec) end

        if spec == nil or string.startswith(spec, "-") then
            -- Bare -g: the built-in continuous running indicator.
            gaugeMode = "continuous"
        elseif spec == "continuous" or spec == "run" then
            index = index + 1
            gaugeMode = "continuous"
        else
            index = index + 1
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
            gaugeMode = "value"
        end
    elseif token == "-c" or token == "--continuous" then
        animate = true
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
local running = true

local function close()
    running = false
    if previous then graphics.display(previous) end
    os.exit(0)
end

local function addButtons(screen, handler)
    for i = 1, #buttons do
        local label = buttons[i]
        local cmd = graphics.new("command", { label = label, type = "ok", priority = 2 })
        graphics.addCommand(screen, cmd)
        handler[cmd] = function ()
            print("alert: " .. label)
            close()
        end
    end
    graphics.addCommand(screen, switch)
    handler[switch] = graphics.taskmngr
end

local function addHandler(screen)
    local handler = {}
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    graphics.addCommand(screen, back)
    handler[back] = close
    addButtons(screen, handler)
    return handler
end

-- Animation thread: sweep the value from its start up to max, wrapping around,
-- until the alert closes (running = false) or the alert timeout expires.
local function animateGauge(gauge, millis)
    local step = gaugeMax / 20
    if step < 1 then step = 1 end
    step = step - step % 1

    local value = gaugeValue
    local deadline = millis > 0 and (os.clock() + millis) or nil

    while running do
        if deadline ~= nil and os.clock() >= deadline then break end
        graphics.SetGauge(gauge, value)
        value = value + step
        if value > gaugeMax then value = 0 end
        java.sleep(200)
    end
end

if vibrate > 0 then graphics.vibrate(vibrate) end

-- A gauge inside an alert has to be non-interactive; MIDP only supports the
-- indicator in that case. Older runtimes without SetIndicator get the form.
local alertGauge = gaugeMode ~= nil and not interactive and graphics.SetIndicator ~= nil

if gaugeMode == nil or alertGauge then
    local screen = graphics.new("alert", title, text, { type = kind, timeout = timeout })
    if ticker then graphics.SetTicker(screen, ticker) end

    local gauge = nil
    if gaugeMode ~= nil then
        if gaugeMode == "continuous" then
            gauge = graphics.new("gauge", { interactive = false, maxValue = 0, value = graphics.CONTINUOUS_RUNNING or 2 })
        else
            gauge = graphics.new("gauge", { interactive = false, maxValue = gaugeMax, value = gaugeValue })
        end
        graphics.SetIndicator(screen, gauge)
    end

    if timeout > 0 then
        -- A timed alert dismisses itself and returns to the previous screen,
        -- so the process is only kept around while the gauge animates.
        if previous then
            graphics.display(screen, previous)
        else
            graphics.display(screen)
        end

        if gauge ~= nil and animate and gaugeMode == "value" then
            java.run(function () animateGauge(gauge, timeout) end, "alert-gauge")
            java.sleep(timeout + 250)
            running = false
        end

        os.exit(0)
    end

    graphics.handler(screen, addHandler(screen))
    os.setproc("screen", screen)
    graphics.display(screen)

    if gauge ~= nil and animate and gaugeMode == "value" then
        java.run(function () animateGauge(gauge, 0) end, "alert-gauge")
    end

    return 0
end

-- Interactive gauge: a form, because MIDP alerts cannot hold one.
local screen = graphics.new("screen", title)
if text ~= "" then
    graphics.append(screen, { type = "text", label = "", value = text })
end
graphics.append(screen, { type = "gauge", label = "", interactive = true, maxValue = gaugeMax, value = gaugeValue })
if ticker then graphics.SetTicker(screen, ticker) end

graphics.handler(screen, addHandler(screen))
os.setproc("screen", screen)
graphics.display(screen)
