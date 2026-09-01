-- Lua J2ME - Code examples
-- This is an example of performing an HTTP GET request

local url = "http://example.com"
local response, code = socket.http.get(url)

print("Status code:", code)

if code == 200 then
    print("Response body:")
    print(response)
else
    print("HTTP request failed with code " .. tostring(code))
end
