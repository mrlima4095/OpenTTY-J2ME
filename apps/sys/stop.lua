#!/bin/lua

if arg[1] then
    local libcore = require("libcore")

    for i = 1, #arg - 1 do
        local pattern = arg[i]
        local sys = os.getproc()

        for k,v in pairs(sys) do
            if v == pattern then
                local status = os.request(1, "sendsig", { pid = k, signal = 9 })
                if status > 0 then
                    print("stop: " .. arg[i] .. ": " .. libcore.errormsg(status))
                    os.exit(tonumber(status))
                end
            end
        end
    end
else
    print("stop: usage: stop [name...]")
end