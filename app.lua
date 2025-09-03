local app = { }

local function split(str, sep)
    local parts = {}
    local idx, start = 1, 1

    while true do
        local pos = string.match(str, sep, start) 
        if pos == nil then
            parts[idx] = string.sub(str, start)
            break
        end
        parts[idx] = string.sub(str, start, pos - 1)
        idx = idx + 1
        start = pos + string.len(sep)
        if start > string.len(str) then
            parts[idx] = ""
            break
        end
    end

    return parts
end
local function getNumber(s) return tonumber(s) or 0 end

function app.match(required, mode)
    local version = os.getenv("VERSION") or ""
    if required == nil then return true end

    if mode == nil or mode == "" then mode = "exact-prefix" end

    if mode == "exact-prefix" then
        local pos = string.match(version, required, 1)
        return pos == 1
    elseif mode == "minimum" or mode == "maximum" then
        local currentParts, requiredParts = split(version, "."), split(required, ".")

        if mode == "minimum" then
            if currentParts[2] == nil or requiredParts[2] == nil then return false 
            else return getNumber(currentParts[2]) >= getNumber(requiredParts[2]) end
        elseif mode == "maximum" then
            if currentParts[1] == nil or requiredParts[1] == nil then return false
            else return getNumber(currentParts[1]) <= getNumber(requiredParts[1]) end
        end
    elseif mode == "exact-full" then return version == required 
    else error("bad argument #2 to 'match' (invalid mode)") end
end 

return app