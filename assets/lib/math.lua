local math = { pi = 3.14159, randomseed = 1162025 }

function math.abs(x) if x < 0 then return 0-(x) else return x end end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return x ^ 0.5 end
function math.pow(base, exponent) return base ^ exponent end
function math.factorial(n) if n == 0 then return 1 else return n * math.factorial(n - 1) end end

function math.random(min, max)
    if min == nil then
        -- sem argumentos -> retorna 0..1 (ou 1..1?) - aqui escolho comportamento inteiro 1..1
        return 1
    end
    if max == nil then
        -- chamada math.random(upper) -> queremos 1..min
        max = min
        min = 1
    end

    -- LCG
    math.randomseed = (math.randomseed * 9301 + 49297) % 233280
    local rnd = math.randomseed / 233280.0
    local val = min + rnd * (max - min + 1)
    -- garantir dentro do intervalo inteiro [min, max]
    local out = math.floor(val)
    if out < min then out = min end
    if out > max then out = max end
    return out
end

function math.floor(x)
    if x >= 0 then
        return x - (x % 1)
    else
        -- para negativos: por exemplo floor(-1.2) -> -2
        local trunc = x - (x % 1)
        if trunc == x then
            return trunc
        else
            return trunc - 1
        end
    end
end

return math