-- Probe: can the listening port be re-bound after the server closes a client?
--
-- Run as root on the device:   lua /tmp/portprobe.lua
-- It answers its own requests on 127.0.0.1 and writes /tmp/portprobe.log.
--
-- Strategy tested: accept -> write -> close input -> close output -> close
-- client -> close listener -> bind the same port again (retried for 20 s).

local PORT = 5021
local LOG = "/tmp/portprobe.log"

local function note(text)
    local line = tostring(text) .. "\n"
    pcall(io.write, line, LOG, "a")
    print(line)
end

pcall(io.write, "", LOG)
note("start port=" .. tostring(PORT))

local ok, server = pcall(socket.server, PORT)
note("bind " .. tostring(ok) .. " " .. tostring(server))
if not ok then return end

local function bind()
    local bound, fresh = pcall(socket.server, PORT)
    if bound then return true, fresh end
    return false, fresh
end

java.run(function()
    for i = 1, 3 do
        local aok, client, input, output = pcall(socket.accept, server)
        note("accept " .. i .. " " .. tostring(aok) .. " " .. tostring(client))

        if aok and client then
            pcall(io.write, "HTTP/1.1 200 OK\r\nContent-Length: 4\r\nConnection: close\r\n\r\nokay", output)
            pcall(io.close, input)
            pcall(io.close, output)
            pcall(io.close, client)
        end

        pcall(io.close, server)
        local start = os.time()
        local fresh
        local bound = false
        while os.time() - start < 20 do
            bound, fresh = bind()
            if bound then break end
            java.sleep(500)
        end
        note("rebind " .. tostring(bound) .. " after " .. tostring(os.time() - start) .. "s " .. tostring(fresh))
        if not bound then break end
        server = fresh
    end
    note("probe loop done")
end)

java.sleep(300)
local url = "http://127.0.0.1:" .. tostring(PORT) .. "/"
for i = 1, 3 do
    local gok, body = pcall(socket.http.get, url)
    local size = "nil"
    if body then size = #body end
    note("get " .. i .. " ok=" .. tostring(gok) .. " bytes=" .. tostring(size))
    java.sleep(1200)
end
note("main done")
