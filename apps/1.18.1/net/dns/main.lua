#!/bin/lua

-- OpenTTY DNS Server
-- Simple DNS server daemon for OpenTTY

local version = "1.0.0"

if arg[1] == "--deamon" then
    os.setproc("name", "dnsd")
    os.setproc(false)

    local running = true
    local zones = {}
    local cache = {}
    local stats = { queries = 0, hits = 0, misses = 0 }

    local function load_zones()
        local hosts = io.read("/etc/hosts")
        if hosts and hosts ~= "" then
            local lines = string.split(hosts, "\n")
            for _, line in pairs(lines) do
                line = string.trim(line)
                if line ~= "" and not string.startswith(line, "#") then
                    local parts = string.split(line, " ")
                    if #parts >= 2 then
                        local ip = parts[1]
                        for i = 2, #parts do
                            zones[parts[i]] = { type = "A", ip = ip, ttl = 3600 }
                        end
                    end
                end
            end
        end

        local zone_dir = io.dirs("/etc/dns/")
        if zone_dir then
            for _, name in pairs(zone_dir) do
                if string.endswith(name, ".zone") then
                    local zone_data = io.read("/etc/dns/" .. name)
                    if zone_data and zone_data ~= "" then
                        local domain = string.sub(name, 1, -6)
                        local zlines = string.split(zone_data, "\n")
                        for _, zline in pairs(zlines) do
                            zline = string.trim(zline)
                            if zline ~= "" and not string.startswith(zline, "#") then
                                local zparts = string.split(zline, " ")
                                if #zparts >= 2 then
                                    local rec_type = zparts[1]
                                    local rec_name = zparts[2]
                                    local rec_value = zparts[3] or ""
                                    local rec_ttl = tonumber(zparts[4]) or 3600
                                    zones[rec_name] = { type = rec_type, ip = rec_value, ttl = rec_ttl }
                                end
                            end
                        end
                    end
                end
            end
        end
    end

    local function build_dns_response(query_id, answers, qname, qtype)
        local resp = {}

        table.insert(resp, math.floor(query_id / 256))
        table.insert(resp, query_id % 256)

        local flags = 0x8180
        if #answers == 0 then
            flags = 0x8183
        end
        table.insert(resp, math.floor(flags / 256))
        table.insert(resp, flags % 256)

        table.insert(resp, 0)
        table.insert(resp, 1)
        table.insert(resp, math.floor(#answers / 256))
        table.insert(resp, #answers % 256)
        table.insert(resp, 0)
        table.insert(resp, 0)

        local name_parts = string.split(qname, ".")
        for _, part in pairs(name_parts) do
            table.insert(resp, string.len(part))
            for j = 1, string.len(part) do
                table.insert(resp, string.byte(part, j))
            end
        end
        table.insert(resp, 0)

        local type_num = 1
        if qtype == "AAAA" then type_num = 28
        elseif qtype == "MX" then type_num = 15
        elseif qtype == "CNAME" then type_num = 5
        elseif qtype == "TXT" then type_num = 16
        end
        table.insert(resp, math.floor(type_num / 256))
        table.insert(resp, type_num % 256)
        table.insert(resp, 0)
        table.insert(resp, 1)

        for _, ans in pairs(answers) do
            for _, ap in pairs(name_parts) do
                table.insert(resp, string.len(ap))
                for j = 1, string.len(ap) do
                    table.insert(resp, string.byte(ap, j))
                end
            end
            table.insert(resp, 0)

            local atype = 1
            if ans.type == "AAAA" then atype = 28
            elseif ans.type == "MX" then atype = 15
            elseif ans.type == "CNAME" then atype = 5
            elseif ans.type == "TXT" then atype = 16
            end
            table.insert(resp, math.floor(atype / 256))
            table.insert(resp, atype % 256)
            table.insert(resp, 0)
            table.insert(resp, 1)
            table.insert(resp, math.floor(ans.ttl / 65536))
            table.insert(resp, math.floor(ans.ttl / 256) % 256)
            table.insert(resp, ans.ttl % 256)
            table.insert(resp, 0)
            table.insert(resp, 4)

            local ip_parts = string.split(ans.ip, ".")
            for _, p in pairs(ip_parts) do
                table.insert(resp, tonumber(p) or 0)
            end
        end

        local result = ""
        for _, b in pairs(resp) do
            result = result .. string.char(b)
        end
        return result
    end

    local function parse_dns_query(data)
        if not data or string.len(data) < 12 then return nil end

        local qid = string.byte(data, 1) * 256 + string.byte(data, 2)
        local qdcount = string.byte(data, 5) * 256 + string.byte(data, 6)

        if qdcount == 0 then return nil end

        local pos = 13
        local qname = ""
        while pos <= string.len(data) do
            local label_len = string.byte(data, pos)
            if label_len == 0 then
                pos = pos + 1
                break
            end
            pos = pos + 1
            if qname ~= "" then qname = qname .. "." end
            qname = qname .. string.sub(data, pos, pos + label_len - 1)
            pos = pos + label_len
        end

        local qtype = "A"
        if pos + 1 <= string.len(data) then
            local qt = string.byte(data, pos) * 256 + string.byte(data, pos + 1)
            if qt == 1 then qtype = "A"
            elseif qt == 28 then qtype = "AAAA"
            elseif qt == 15 then qtype = "MX"
            elseif qt == 5 then qtype = "CNAME"
            elseif qt == 16 then qtype = "TXT"
            end
        end

        return { id = qid, name = qname, type = qtype }
    end

    load_zones()

    local ok, server = pcall(socket.server, 53)
    if not ok then
        return function(payload, args, scope, pid, uid)
            return ":: dnsd failed to bind port 53: " .. tostring(server)
        end
    end

    java.run(function()
        while running do
            local aok, client, client_input, client_output = pcall(socket.accept, server)
            if aok and client then
                java.run(function()
                    local rok, data = pcall(io.read, client_input, 512)
                    if rok and data then
                        local query = parse_dns_query(data)
                        if query then
                            stats.queries = stats.queries + 1
                            local answers = {}

                            local cached = cache[query.name]
                            if cached then
                                stats.hits = stats.hits + 1
                                table.insert(answers, cached)
                            else
                                local zone = zones[query.name]
                                if zone then
                                    stats.misses = stats.misses + 1
                                    table.insert(answers, zone)
                                    cache[query.name] = zone
                                else
                                    stats.misses = stats.misses + 1
                                end
                            end

                            local response = build_dns_response(query.id, answers, query.name, query.type)
                            pcall(io.write, response, client_output)
                        end
                    end
                    pcall(io.close, client)
                end, "dns-worker")
            end
        end
    end, "dns-accept")

    return function(payload, args, scope, pid, uid)
        if payload == "lookup" then
            local qname = args
            if zones[qname] then
                return zones[qname].ip
            end
            return nil
        elseif payload == "add" then
            if type(args) == "table" then
                zones[args.name] = { type = args.type or "A", ip = args.ip, ttl = args.ttl or 3600 }
                return ":: record added"
            end
            return ":: invalid args"
        elseif payload == "remove" then
            if zones[args] then
                zones[args] = nil
                cache[args] = nil
                return ":: record removed"
            end
            return ":: record not found"
        elseif payload == "reload" then
            load_zones()
            return ":: zones reloaded (" .. #zones .. " records)"
        elseif payload == "stats" then
            return ":: queries: " .. stats.queries .. " hits: " .. stats.hits .. " misses: " .. stats.misses
        elseif payload == "stop" then
            running = false
            pcall(io.close, server)
            return ":: dns server stopped"
        elseif payload == "list" then
            local list = {}
            for name, data in pairs(zones) do
                table.insert(list, name .. " " .. data.type .. " " .. data.ip)
            end
            return table.concat(list, "\n")
        else
            return ":: unknown command"
        end
    end
end

local function show_help()
    print("OpenTTY DNS Server v" .. version)
    print("")
    print("Usage: dns [command] [options]")
    print("")
    print("Commands:")
    print("  start           Start DNS server on port 53")
    print("  stop            Stop DNS server")
    print("  reload          Reload zones from /etc/dns/")
    print("  stats           Show query statistics")
    print("  list            List all DNS records")
    print("  lookup <name>   Look up a domain")
    print("  add <name> <ip> Add a DNS record")
    print("  remove <name>   Remove a DNS record")
    print("  help            Show this help")
    print("")
    print("Configuration:")
    print("  /etc/hosts       Host file (IP hostname)")
    print("  /etc/dns/        Zone files (*.zone)")
    print("")
    print("Zone file format:")
    print("  A example.com 192.168.1.1 3600")
    print("  MX example.com mail.example.com 3600")
    print("  CNAME www.example.com example.com 3600")
end

os.setproc("name", "dns")

if arg[1] == "start" then
    local pid = os.getpid("dnsd")
    if pid then
        print("dns: already running (pid " .. pid .. ")")
        os.exit(68)
    end

    print("dns: starting DNS server on port 53...")
    os.request("1", "serve", os.join(arg[0]))
    java.sleep(300)
    pid = os.getpid("dnsd")
    if pid then
        print("dns: started (pid " .. pid .. ")")
    else
        print("dns: failed to start (permission denied?)")
        os.exit(1)
    end

elseif arg[1] == "stop" then
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "stop")
    if result then print("dns: " .. result) end

elseif arg[1] == "reload" then
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "reload")
    if result then print("dns: " .. result) end

elseif arg[1] == "stats" then
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "stats")
    if result then print("dns: " .. result) end

elseif arg[1] == "list" then
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "list")
    if result then print(tostring(result)) end

elseif arg[1] == "lookup" then
    if not arg[2] then
        print("dns: usage: dns lookup <name>")
        os.exit(2)
    end
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "lookup", arg[2])
    if result then
        print(arg[2] .. " -> " .. result)
    else
        print("dns: " .. arg[2] .. ": not found")
    end

elseif arg[1] == "add" then
    if not arg[2] or not arg[3] then
        print("dns: usage: dns add <name> <ip> [ttl]")
        os.exit(2)
    end
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "add", {
        name = arg[2],
        ip = arg[3],
        ttl = tonumber(arg[4]) or 3600,
    })
    if result then print("dns: " .. result) end

elseif arg[1] == "remove" then
    if not arg[2] then
        print("dns: usage: dns remove <name>")
        os.exit(2)
    end
    local pid = os.getpid("dnsd")
    if not pid then
        print("dns: not running")
        os.exit(1)
    end
    local result = os.request(pid, "remove", arg[2])
    if result then print("dns: " .. result) end

elseif arg[1] == "help" or arg[1] == "--help" or arg[1] == nil then
    show_help()
else
    print("dns: '" .. arg[1] .. "' is not a valid command")
    print("Try 'dns help'")
    os.exit(1)
end
