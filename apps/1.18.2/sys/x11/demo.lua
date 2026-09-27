#!/bin/lua

local x11 = os.getpid("x11")
if not x11 then
    print("x11-demo: X11 service is not running")
    print("x11-demo: run 'x11 connect <ip> <port>' first")
    os.exit(1)
end

if os.request(x11, "status") ~= "connected" then
    print("x11-demo: X11 service is disconnected")
    print("x11-demo: run 'x11 connect <ip> <port>' first")
    os.exit(1)
end

local window = "opentty-x11-demo"
local function draw(operation, args)
    local result = os.request(x11, operation, args)
    if result ~= true then
        print("x11-demo: " .. tostring(result))
        os.exit(1)
    end
end

draw("create", { window = window, title = "OpenTTY X11 Proxy Demo", width = 640, height = 360 })
draw("rect", { window = window, x = 0, y = 0, width = 640, height = 72, color = "#283593" })
draw("rect", { window = window, x = 32, y = 116, width = 576, height = 1, color = "#b0bec5" })
draw("rect", { window = window, x = 32, y = 228, width = 576, height = 1, color = "#b0bec5" })
draw("text", { window = window, x = 32, y = 24, text = "OpenTTY X11 Proxy", color = "white" })
draw("text", { window = window, x = 32, y = 88, text = "A Lua app drew this native host window.", color = "#202124" })
draw("text", { window = window, x = 32, y = 144, text = "Transport: OpenTTY socket -> Python proxy -> X11 display", color = "#202124" })
draw("text", { window = window, x = 32, y = 256, text = "Close this window to send a close event back to OpenTTY.", color = "#202124" })
draw("show", { window = window })

print("x11-demo: native window created")
print("x11-demo: request os.request(" .. x11 .. ", 'event') to read queued proxy events")
