local math = { pi = 3.1415926535898 }

function math.abs(x) if x < 0 then return 0-(x) end return x end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return x ^ 0.5 end

function math.pow(base, exponent) return base ^ exponent end
function math.factorial(x) if x == 0 then return 1 end return x * math.factorial(x - 1) end

function math.floor(x)
    local n = x % 1
    if x >= 0 then
        return x - n
    else
        if n == 0 then return x end
        return x - n - 1
    end
end
function math.ceil(x)
    local n = x % 1
    if n == 0 then
        return x
    elseif x > 0 then
        return x - n + 1
    else
        return x - n
    end
end

function math.min(...)
    local args = ...
    local min_val = args[1]
    for i = 2, #args do
        if args[i] < min_val then
            min_val = args[i]
        end
    end
    return min_val
end
function math.max(...)
    local args = ...
    local max_val = args[1]
    for i = 2, #args do
        if args[i] > max_val then
            max_val = args[i]
        end
    end
    return max_val
end

function math.random(max) return random(max) end
function math.tointeger(x)
    x = tonumber(x)
    
    local n = x % 1
    if n == 0 then return x
    else return nil end
end

return math
