local httpd = {}
httpd.__routes = {}

local function getRoute(payload) end
local function getHeaders(payload) end
local function getBody(payload) end
local function getQuery(payload) end

function httpd.route(endpoint, method, handler)
    httpd.__routes[endpoint] = { method = method, handler = handler }
end
function httpd.run(port)
    while true do
        local server, input, output, payload
        local ok, err = pcall(function () server = socket.server(port) end)
        if not ok then pcall(io.close, server) error(ok) end

        
    end
end

return httpd