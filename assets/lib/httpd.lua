local httpd = {}


local function getHeaders() end
local function getBody() end

function httpd.getStatusMessage(status) end
function httpd.getRoute(request) end

function httpd.getQuery() end
function httpd.handler() end

function httpd.generate(body, headers) end

function httpd.route(path, method, handler) end
function httpd.run(port) end

return httpd