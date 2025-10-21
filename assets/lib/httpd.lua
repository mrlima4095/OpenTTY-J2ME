local httpd = {}

httpd.__routes = {}

local function getHeaders() end
local function getBody() end

function httpd.getStatusMessage(status) end
function httpd.getRoute(request) end

function httpd.getQuery() end
function httpd.handler() end

function httpd.generate(body, headers) end

function httpd.route(path, method, handler)
    if path then error("bad argument #1 to 'route' (string expected, got no value)") end
    if not handler then
        handler = method
        method = "GET"
    end

    httpd.__routes[path] = { ["method"] = method, ["handler"] = handler }
end
function httpd.run(port)
    if not port then error("bad argument #1 to 'run' (number expected, got no value)") end

    while true do
        local server, conn, instrem, outstream, payload

        local ok, err = pcall(function () server = socket.server(port) end)
        if not ok then pcall(io.close, server) error(err) end

        ok, err = pcall(function () conn, instrem, outstream = socket.accept(server) end)
        if not ok then pcall(io.close, server, conn, instrem, outstream) error(err) end

        ok, err = pcall(function () payload = io.read(instrem) end)
        if not ok then pcall(io.close, server, conn, instrem, outstream) error(err) end

        if payload then
            local endpoint = httpd.getRoute(payload)
            local headers = getHeaders()
            local body = getBody


        end
    end
end

return httpd