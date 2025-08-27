local math = { pi = 3.14159 }

function math.abs(x) if x < 0 then return 0-(x) end return x end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return x ^ 0.5 end

function math.pow(base, exponent) return base ^ exponent end
function math.factorial(n) if n == 0 then return 1 end return n * math.factorial(n - 1) end

function math.floor(x)
    local n = x % 1
    if x >= 0 then
        return x - n
    else
        if n == 0 then return x end
        return x - n - 1
    end
end


return math
