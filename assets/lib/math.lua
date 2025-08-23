local math = {}

function math.abs(x) return x < 0 and 0-x or x end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return math.pow(x, 0.5) end

function math.pow(base, exponent) 
    if exponent == 0 then
        return 1
    else 
        local range = 1
        
        while range <= exponent do
            base = base * base
        end
        
        return base
    end
end
function math.factorial(n) if n == 0 then return 1 else return n * math.factorial(n - 1) end end

return math