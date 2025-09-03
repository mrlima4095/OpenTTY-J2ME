--[[


]]


local app = { }

function app.main(conf) 
    
end
function split(str, sep)
    local result = {}
    local start = 1
    local i = 1
    while true do
        local idx = string.find(str, sep, start, true)
        if idx == nil then
            result[i] = string.sub(str, start)
            break
        end
        result[i] = string.sub(str, start, idx - 1)
        start = idx + 1
        i = i + 1
    end
    return result
end

function tonumber_safe(s)
    local n = tonumber(s)
    if n == nil then return 0 else return n end
end

function app.api_match(required, matching)
    local version = os.getenv("VERSION")
    if required == nil then return true end
    if matching == nil or matching == "" then
        matching = "exact-prefix"
    end

    if matching == "exact-prefix" then
        return string.sub(version, 1, string.len(required)) == required

    elseif matching == "exact-full" then
        return version == required

    elseif matching == "minimum" then
        local cur_parts = split(version, ".")
        local req_parts = split(required, ".")
        if cur_parts[2] == nil or req_parts[2] == nil then return false end
        return tonumber_safe(cur_parts[2]) >= tonumber_safe(req_parts[2])

    elseif matching == "maximum" then
        local cur_parts = split(version, ".")
        local req_parts = split(required, ".")
        if cur_parts[1] == nil or req_parts[1] == nil then return false end
        return tonumber_safe(cur_parts[1]) <= tonumber_safe(req_parts[1])

    else
        error("bad argument #2 to 'api_match' (invalid matching type)")
    end

    return false
end

