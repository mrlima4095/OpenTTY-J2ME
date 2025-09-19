local httpd = {}

httpd._row = 1
httpd._routes = {}

local function getMethod(raw)
    
end
local function getHeaders(raw)
    
end
local function getRoute(raw)
    
end

function httpd.route(path, method, handler)
    if path == nil then error("bad argument #1 for 'route' (string expected, got nil)")
    elseif handler == nil then
        handler = method
        method = "GET"
    end
    
    httpd._routes[path] = { method = method, handler = handler }
end
function httpd.run(port)
    while true do
        local ok, server = pcall(socket.server, port)
        if not ok then
            print("httpd - port in use\nhttpd - server stopped")
            pcall(io.close, server)
            break
        elseif httpd._row == 1 then
            print("httpd - listening at port " .. port)
            httpd._row = httpd._row + 1
        end

        local client, i, o = socket.accept(server)
        if client then
            local raw = io.read(i)
            local method = getMethod(raw)
            local headers = getHeaders(raw)
            
            local route = getRoute(raw)
            local handler = httpd._routes[route]
            local status = "200 OK"
            
        end
    end
end


return httpd