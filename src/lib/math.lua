-- rock: name=math
-- rock: version=1.1.0
-- rock: description=Numeric helpers missing from the native runtime (trig, logs, rounding, gcd)
-- rock: here=/lib/math.lua

-- The runtime ships a very small `math` table (only `random`). This module
-- extends that table instead of replacing it, so the native random keeps
-- working and every program shares one table.
local M = math or {}

-- `1 / 0` raises "Division by zero" in the OpenTTY runtime, so grow a number
-- until it stops changing: that value is infinity in both the runtime and
-- stock Lua.
local function infinity()
    local value = 1
    while true do
        local doubled = value * 2
        if doubled == value then return value end
        value = doubled
    end
end

M.pi = 3.141592653589793
M.huge = infinity()
M.e = 2.718281828459045

local PI = M.pi
local TWO_PI = 6.283185307179586
local E = M.e
local LN2 = 0.6931471805599453
local EPS = 0.000000000001

local function abs(x) if x < 0 then return -x end return x end

function M.abs(x) return abs(x) end
function M.sqrt(x)
    if x < 0 then error("math.sqrt expects a non-negative number") end
    return x ^ 0.5
end

function M.pow(base, exponent) return base ^ exponent end

function M.factorial(x)
    if x < 0 then error("math.factorial expects a non-negative number") end
    local result = 1
    for i = 2, x do result = result * i end
    return result
end

-- The OpenTTY runtime's `%` is a C-style remainder (fmod): -2.5 % 1 == -0.5.
-- Stock Lua uses floor modulo: -2.5 % 1 == 0.5. Detect once and adapt, so the
-- same file also runs under the host test suite.
local C_MOD = (-2.5 % 1) == -0.5

function M.fmod(x, y)
    if y == 0 then return M.huge - M.huge end
    if C_MOD then return x % y end
    return x - M.trunc(x / y) * y
end

function M.floor(x)
    local n = x % 1
    if C_MOD then
        if x >= 0 then return x - n end
        if n == 0 then return x end
        return x - n - 1
    end
    return x - n
end

function M.ceil(x)
    local n = x % 1
    if n == 0 then return x end
    if C_MOD and x < 0 then return x - n end
    return x - n + 1
end

function M.trunc(x)
    if C_MOD then return x - (x % 1) end
    if x >= 0 then return M.floor(x) end
    return M.ceil(x)
end

function M.round(x)
    if x >= 0 then return M.floor(x + 0.5) end
    return M.ceil(x - 0.5)
end

function M.modf(x)
    local integer = M.trunc(x)
    return integer, x - integer
end

function M.sign(x)
    if x > 0 then return 1 end
    if x < 0 then return -1 end
    return 0
end

function M.exp(x) return E ^ x end

function M.log(x, base)
    -- The runtime throws on `n / 0`, so build -inf and nan by arithmetic.
    if x == 0 then return -M.huge end
    if x < 0 then return M.huge - M.huge end
    -- x = m * 2^k with m in [1, 2), then log(m) via the atanh series.
    local k = 0
    while x >= 2 do x = x / 2; k = k + 1 end
    while x < 1 do x = x * 2; k = k - 1 end
    local z = (x - 1) / (x + 1)
    local z2 = z * z
    local term = z
    local sum = z
    local n = 3
    while abs(term) > EPS and n < 400 do
        term = term * z2
        sum = sum + term / n
        n = n + 2
    end
    local result = 2 * sum + k * LN2
    if base then result = result / M.log(base) end
    return result
end

function M.log10(x) return M.log(x, 10) end
function M.log2(x) return M.log(x, 2) end

-- sin/cos via Taylor after reducing to [-pi, pi]; good to ~1e-12 here.
function M.sin(x)
    x = x % TWO_PI
    if x > PI then x = x - TWO_PI elseif x < -PI then x = x + TWO_PI end
    local term = x
    local sum = x
    local n = 1
    while abs(term) > EPS and n < 40 do
        term = -term * x * x / ((2 * n) * (2 * n + 1))
        sum = sum + term
        n = n + 1
    end
    return sum
end

function M.cos(x) return M.sin(x + PI / 2) end
function M.tan(x) return M.sin(x) / M.cos(x) end

function M.atan(x)
    if x ~= x then return x end
    local sign = 1
    if x < 0 then sign = -1; x = -x end
    local invert = false
    if x > 1 then invert = true; x = 1 / x end
    -- atan(x) = 2 * atan(x / (1 + sqrt(1 + x*x))) shrinks the argument fast.
    local k = 0
    while x > 0.2 and k < 8 do
        x = x / (1 + M.sqrt(1 + x * x))
        k = k + 1
    end
    local term = x
    local sum = x
    local n = 1
    while abs(term) > EPS and n < 60 do
        term = -term * x * x
        sum = sum + term / (2 * n + 1)
        n = n + 1
    end
    local result = sum * (2 ^ k)
    if invert then result = PI / 2 - result end
    return sign * result
end

function M.atan2(y, x)
    if x > 0 then return M.atan(y / x) end
    if x < 0 and y >= 0 then return M.atan(y / x) + PI end
    if x < 0 and y < 0 then return M.atan(y / x) - PI end
    if x == 0 and y > 0 then return PI / 2 end
    if x == 0 and y < 0 then return -PI / 2 end
    return 0
end

function M.asin(x)
    if x > 1 or x < -1 then return M.huge - M.huge end
    if x == 1 then return PI / 2 end
    if x == -1 then return -PI / 2 end
    return M.atan(x / M.sqrt(1 - x * x))
end

function M.acos(x) return PI / 2 - M.asin(x) end

function M.deg(x) return x * 180 / PI end
function M.rad(x) return x * PI / 180 end

-- The runtime stores `...` in one table; stock Lua keeps real varargs.
-- The check is inlined on purpose: passing `...` to a helper would wrap the
-- runtime's argument table in a second table.
local function arg_table(first, total)
    if type(first) == "table" and total == #first then return first end
    return nil
end

function M.min(...)
    local args
    local first = ...
    local total = select('#', ...)
    args = arg_table(first, total)
    if args == nil then
        args = {}
        for i = 1, total do args[i] = select(i, ...) end
    end
    if args[1] == nil then return nil end
    local value = args[1]
    for i = 2, #args do if args[i] < value then value = args[i] end end
    return value
end

function M.max(...)
    local args
    local first = ...
    local total = select('#', ...)
    args = arg_table(first, total)
    if args == nil then
        args = {}
        for i = 1, total do args[i] = select(i, ...) end
    end
    if args[1] == nil then return nil end
    local value = args[1]
    for i = 2, #args do if args[i] > value then value = args[i] end end
    return value
end

function M.clamp(x, low, high)
    if x < low then return low end
    if x > high then return high end
    return x
end

function M.gcd(a, b)
    a, b = abs(a), abs(b)
    while b ~= 0 do a, b = b, a % b end
    return a
end

function M.lcm(a, b)
    if a == 0 or b == 0 then return 0 end
    return abs(a * b) / M.gcd(a, b)
end

function M.hypot(x, y) return M.sqrt(x * x + y * y) end

function M.isclose(a, b, rel, abs_tol)
    rel = rel or 0.0000001
    abs_tol = abs_tol or 0
    local diff = abs(a - b)
    if diff <= abs_tol then return true end
    return diff <= rel * M.max(abs(a), abs(b))
end

function M.tointeger(x)
    x = tonumber(x)
    if x == nil then return nil end
    if x % 1 == 0 then return x end
    return nil
end

return M
