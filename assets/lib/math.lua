local math = {}

function math.abs(x) 
	if x < 0 then
		return x - x + 0-(x)
	else 
		return x
	end
end
function math.sqrt(x) if x < 0 then error("math.sqrt expects a non-negative number") end return x ^ 0.5 end

function math.pow(base, exponent) return base ^ exponent end
function math.factorial(n) if n == 0 then return 1 else return n * math.factorial(n - 1) end end

return math