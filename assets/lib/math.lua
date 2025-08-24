local math = { pi = 3.14159, randomseed = 1162025 }

function math.abs(x) if x < 0 then return 0-(x) else return x end end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return x ^ 0.5 end
function math.pow(base, exponent) return base ^ exponent end
function math.factorial(n) if n == 0 then return 1 else return n * math.factorial(n - 1) end end

function math.random(min, max)
    if min == nil then min = 1 end
    if max == nil then max = min min = 1 end
    math.randomseed = (math.randomseed * 9301 + 49297) % 233280
    local rnd = math.randomseed / 233280.0
    return math.floor(min + rnd * (max - min + 1))
end

function math.floor(x) return x >= 0 and x - (x % 1) or x - (x % 1) - 1 end


return math