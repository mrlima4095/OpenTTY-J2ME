#!/bin/lua

local proxy = getAppProperty("MIDlet-Proxy") or "opentty.xyz/proxy.php?"
local version = arg[1] or getAppProperty("MIDlet-Version")

local res, status = socket.http.get(proxy .. "raw.githubusercontent.com/mrlima4095/OpenTTY-J2ME/refs/tags/" .. version .. "/CHANGELOG.txt")

if status == 200 then
    graphics.display(graphics.BuildScreen({
        title = version .. " - CHANGELOG",
        back = { root = os.exit },
        fields = { res }
    }))
else
    os.execute("warn " .. status)
end