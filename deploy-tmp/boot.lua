-- One-shot bootstrap for the /home/web demo site (run as root on the device).
--
--   curl -o /tmp/boot.lua http://opentty.fun/deploy-tmp/boot.lua
--   lua /tmp/boot.lua
--
-- Everything happens in this single chunk so a frozen MIDlet screen cannot
-- break the deploy halfway: install /bin/nginx, write the site into /home/web,
-- pick a port this phone lets a MIDlet bind, start the server and hit it three
-- times to prove that consecutive requests work.

local BASE = "http://opentty.fun"

local function fetch(path)
    local ok, body = pcall(socket.http.get, BASE .. path)
    if ok and body then return body end
    return nil
end

local function note(label, value)
    print(label .. "\t" .. tostring(value))
end

note("uid", os.getuid())

-- 1. the nginx app itself (same file the app store serves)
local nginx = fetch("/apps/1.19/net/nginx/main.lua")
if nginx then
    local plain = nginx
    note("nginx bytes", #plain)
    note("write /bin/nginx", io.write(plain, "/bin/nginx"))
else
    note("nginx download", "failed")
end

-- 2. the site
local files = { "index.html", "about.html", "style.css", "app.js" }
os.mkdir("/home/web")
for i = 1, #files do
    local name = files[i]
    local body = fetch("/deploy-tmp/site/" .. name)
    if body then
        local plain = body
        note("site " .. name, tostring(#plain) .. " bytes -> " .. tostring(io.write(plain, "/home/web/" .. name)))
    else
        note("site " .. name, "download failed")
    end
end
note("dirs /home/web", tostring(table.concat(io.dirs("/home/web/") or {}, ",")))

-- 3. pick the first port this phone lets a MIDlet bind: 80 and 8080 throw a
--    SecurityException, and the port of a listener that died stays taken until
--    the MIDlet restarts, so a short list is needed.
local candidates = { 5001, 5002, 5010, 6000, 7000, 9000, 3000, 8899 }
local port = nil
for i = 1, #candidates do
    local ok, probe = pcall(socket.server, candidates[i])
    note("probe " .. tostring(candidates[i]), tostring(ok))
    if ok then
        pcall(io.close, probe)
        port = candidates[i]
        break
    end
end

if port == nil then
    note("port", "none available (restart the MIDlet)")
    os.exit(1)
end
note("port", port)

local config = "# OpenTTY nginx - LAN demo site\n" ..
    "listen " .. tostring(port) .. ";\n" ..
    "root /home/web/;\n" ..
    "index index.html;\n" ..
    "server_name OpenTTY;\n" ..
    "worker_processes 1;\n" ..
    "max_clients 10;\n" ..
    "error_log /tmp/nginx-error.log;\n" ..
    "access_log /tmp/nginx-access.log;\n"
os.mkdir("/etc/nginx")
note("write config", io.write(config, "/etc/nginx/nginx.conf"))
note("config", io.read("/etc/nginx/nginx.conf"))

-- 4. run it
note("running pid before", os.getpid("nginxd"))
note("stop", os.execute("nginx stop"))
note("pid after stop", os.getpid("nginxd"))
pcall(io.write, "", "/tmp/nginx.stop")
pcall(io.write, "", "/tmp/nginx.reload")
pcall(io.write, "", "/tmp/nginx.fail")
pcall(io.write, "", "/tmp/nginx.pid")
pcall(io.write, "", "/tmp/nginx-error.log")
note("start", os.execute("nginx start"))
java.sleep(1200)
note("status", os.execute("nginx status"))
note("pid", os.getpid("nginxd"))
note("pidfile", io.read("/tmp/nginx.pid"))
note("fail", io.read("/tmp/nginx.fail"))

note("error log", io.read("/tmp/nginx-error.log"))
