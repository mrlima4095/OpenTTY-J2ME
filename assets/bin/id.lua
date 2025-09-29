local request = arg[1]
local id = os.execute("case user (" .. request .. ") false")
if id == 255 then
    return true
else
    print(arg[0] .. ": '" .. request .. "': no such user")
    return false
end