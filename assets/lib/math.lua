local math = { pi = 3.14159, randomseed = os.clock() }

function math.abs(x) return x < 0 and -x or x end
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

local function lcg()
    math.randomseed = (math.randomseed * 9301 + 49297) % 233280
    return math.randomseed / 233280.0
end

-- replicar comportamento padrão do Lua
function math.random(a, b)
    if a == nil then
        -- sem argumentos: número real [0,1)
        return lcg()
    elseif b == nil then
        -- um argumento: inteiro 1..a
        return math.floor(lcg() * a) + 1
    else
        -- dois argumentos: inteiro a..b
        return math.floor(lcg() * (b - a + 1)) + a
    end
end

function math.setseed(seed) if seed == nil then seed = os.clock() end math.randomseed = seed end

return math
