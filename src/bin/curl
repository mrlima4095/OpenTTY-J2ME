#!/bin/lua

local function parse_url(url)
    if string.sub(url, 1, 4) == "http" then return url end
    return "http://" .. url
end
local function parse_headers(headers_str)
    if not headers_str or headers_str == "" then return nil end

    local headers = {}
    local lines = string.split(headers_str, "\n")
end

local function errors(code)
    if code == 13 then
        return "permission denied"
    elseif code == 5 then
        return "read-only storage"
    elseif code ~= 0 then
        return "java.io.IOException"
    end
end

os.setproc("name", "curl")

local function http_request(method, url, data, headers)
    if method == "GET" then
        if headers and type(headers) == "table" then
            return socket.http.get(url, headers)
        else
            return socket.http.get(url)
        end
    elseif method == "POST" then
        if headers and type(headers) == "table" then
            return socket.http.post(url, data or "", headers)
        else
            return socket.http.post(url, data or "")
        end
    end
end

local args = {}
for i = 0, #arg do
    if arg[i] then
        args[#args + 1] = arg[i]
    end
end

if #args == 0 or args[1] == "-h" or args[1] == "--help" then
    print("curl [options] [url]")
    os.exit(0)
end

local method = "GET"
local url, port, data, headers, output_file = nil, nil, nil, nil, nil
local mode = "http"

local i = 1
while i < #args do
    local arg = args[i]

    if arg == "-X" or arg == "--method" then
        i = i + 1
        if args[i] then
            method = string.upper(args[i])
            if method ~= "GET" and method ~= "POST" then
                print("curl: invalid HTTP method: " .. method)
                os.exit(2)
            end
        else
            print("curl -X [method] [url]")
            os.exit(2)
        end
    elseif arg == "-d" or arg == "--data" then
        i = i + 1
        if args[i] then
            data = args[i]
            method = "POST"
        else
            print("curl -d [data] [url]")
            os.exit(2)
        end
    elseif arg == "-H" or arg == "--header" then
        i = i + 1
        if args[i] then
            local header_line = args[i]
            local colon = string.find(header_line, ":")
            if colon then
                headers = headers or {}
                local key = string.trim(string.sub(header_line, 1, colon - 1))
                local value = string.trim(string.sub(header_line, colon + 1))
                headers[key] = value
            else
                print("curl: invalid header: " .. header_line)
                os.exit(2)
            end
        else
            print("curl -H [header] [url]")
                os.exit(2)
        end
    elseif arg == "-o" or arg == "--output" then
        i = i + 1
        if args[i] then
            output_file = args[i]
        else
            print("curl -o [file] [url]")
            os.exit(2)
        end
    elseif arg == "-s" or arg == "--service" then
        mode = "service"

        i = i + 1
        if args[i] and args[i + 1] then
            url = args[i]
            port = args[i + 1]
            data = args[i + 2]

            break
        else
            print("curl -s [pid] [request] [args]")
            os.exit(2)
        end
    elseif arg == "-t" or arg == "--tcp" then
        mode = "tcp"

        i = i + 1
        if args[i] and args[i + 1] then
            url = args[i]
            port = args[i + 1]
            data = args[i + 2]

            break
        else
            print("curl -t [host] [port] [data]")
            os.exit(2)
        end
    else
        if mode == "http" then
            url = parse_url(url)
        end
    end
end

if mode == "http" and url then
    local ok, result, status = pcall(http_request, method, url, data, headers)

    if ok then
        if status ~= 200 then
            print("HTTP " .. tostring(status))
            print("---")
        end

        if output_file then
            local code = io.write(result, os.join(output_file))
            if code > 0 then
                print("curl: " .. errors(code))
            end

            os.exit(tonumber(code))
        else
            print(result)
            os.exit(0)
        end
    else
        print("curl: error: " .. tostring(result))
        os.exit(101)
    end
elseif mode == "http" then
    print("curl: URL not specified")
    os.exit(1)
elseif mode == "service" then
    local ok, response = pcall(os.request, url, port, data)
    if ok then
        if output_file then
            local code = io.write(response, os.join(output_file))
            if code > 0 then
                print("curl: " .. errors(code))
            end

            os.exit(tonumber(code))
        else
            print(response)
            os.exit(0)
        end
    end
elseif mode == "tcp" then
    local ok, conn, input, output = pcall(socket.connect, "socket://" .. url .. ":" .. port)
    if ok then
        if data then
            local write_ok, write_err = pcall(io.write, data, output)
            if not write_ok then
                print("curl: sending error: " .. tostring(write_err))
                pcall(io.close, conn, input, output)
                os.exit(101)
            end
        end

        local read_ok, response = pcall(io.read, input, 4096)
        pcall(io.close, conn, input, output)
        if read_ok then
            if output_file then
                local code = io.write(response, os.join(output_file))
                if code > 0 then
                    print("curl: " .. errors(code))
                end

                os.exit(tonumber(code))
            else
                print(response)
                os.exit(0)
            end
        else
            print("curl: reading error: " .. tostring(response))
            os.exit(101)
        end
    else
        print("curl: error: " .. tostring(conn))
        os.exit(101)
    end
end