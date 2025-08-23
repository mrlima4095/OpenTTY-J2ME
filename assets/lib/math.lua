local math = {}

function math.abs(x) return x < 0 and -x or x end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return x ^ 0.5 end

function math.pow(base, exponent) return base ^ exponent end

return math