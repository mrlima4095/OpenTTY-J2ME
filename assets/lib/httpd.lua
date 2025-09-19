local httpd = {}

httpd._row = 1
httpd._routes = {}

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
        end

        local client, i, o = socket.accept(server)
    end
end