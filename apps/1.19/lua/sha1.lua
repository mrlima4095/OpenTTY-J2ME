-- rock: name=sha1
-- rock: version=1.0.0
-- rock: description=SHA-1 and HMAC-SHA-1 (pure Lua, 32-bit arithmetic)
-- rock: here=/lib/sha1.lua

-- The runtime has no bit32 library, so the 32-bit operations are built from
-- nibble lookup tables (fast enough on a phone) and all arithmetic stays
-- below 2^53, the exact-integer range of a double.
--
--   local sha1 = require("sha1")
--   print(sha1.sha1("abc"))          -- hex digest
--   print(sha1.hmac("key", "abc"))   -- HMAC-SHA-1 in hex
--   print(sha1.raw("abc"))           -- 20 raw bytes
local S = sha1 or {}

local MOD = 4294967296
local MASK = 4294967295
local HEX = "0123456789abcdef"
local floor = function(x) return x - x % 1 end

local BAND, BOR, BXOR = {}, {}, {}
for a = 0, 15 do
    BAND[a], BOR[a], BXOR[a] = {}, {}, {}
    for b = 0, 15 do
        local x, y, bit = a, b, 1
        local ra, ro, rx = 0, 0, 0
        for i = 1, 4 do
            local xb, yb = x % 2, y % 2
            if xb == 1 and yb == 1 then ra = ra + bit end
            if xb == 1 or yb == 1 then ro = ro + bit end
            if xb ~= yb then rx = rx + bit end
            x = (x - xb) / 2
            y = (y - yb) / 2
            bit = bit * 2
        end
        BAND[a][b], BOR[a][b], BXOR[a][b] = ra, ro, rx
    end
end

local function band(a, b)
    local r, place = 0, 1
    for i = 1, 8 do
        local x, y = a % 16, b % 16
        r = r + BAND[x][y] * place
        a = (a - x) / 16
        b = (b - y) / 16
        place = place * 16
    end
    return r
end

local function bor(a, b)
    local r, place = 0, 1
    for i = 1, 8 do
        local x, y = a % 16, b % 16
        r = r + BOR[x][y] * place
        a = (a - x) / 16
        b = (b - y) / 16
        place = place * 16
    end
    return r
end

local function bxor(a, b)
    local r, place = 0, 1
    for i = 1, 8 do
        local x, y = a % 16, b % 16
        r = r + BXOR[x][y] * place
        a = (a - x) / 16
        b = (b - y) / 16
        place = place * 16
    end
    return r
end

local function bnot(a) return MASK - a end

local function rol(a, n)
    n = n % 32
    if n == 0 then return a % MOD end
    local half = 2 ^ (32 - n)
    local high = floor(a / half)
    local low = (a % half) * (2 ^ n)
    return (low + high) % MOD
end

local function rep(text, count)
    local out = {}
    for i = 1, count do out[i] = text end
    return table.concat(out)
end

local function word_bytes(word)
    return string.char(floor(word / 16777216) % 256, floor(word / 65536) % 256,
        floor(word / 256) % 256, word % 256)
end

local function pad(message)
    local text = tostring(message)
    local length = #text
    local bitlen = length * 8
    local out = { text, string.char(128) }
    local zeros = 56 - ((length + 1) % 64)
    if zeros < 0 then zeros = zeros + 64 end
    if zeros > 0 then out[#out + 1] = rep("\0", zeros) end
    out[#out + 1] = word_bytes(floor(bitlen / MOD))
    out[#out + 1] = word_bytes(bitlen % MOD)
    return table.concat(out)
end

local function digest_words(message)
    local data = pad(message)
    local h0, h1, h2, h3, h4 = 1732584193, 4023233417, 2562383102, 271733878, 3285377520

    for block = 0, floor(#data / 64) - 1 do
        local offset = block * 64
        local w = {}
        for i = 0, 15 do
            local p = offset + i * 4 + 1
            w[i] = (string.byte(data, p) or 0) * 16777216 +
                (string.byte(data, p + 1) or 0) * 65536 +
                (string.byte(data, p + 2) or 0) * 256 +
                (string.byte(data, p + 3) or 0)
        end
        for i = 16, 79 do
            w[i] = rol(bxor(bxor(bxor(w[i - 3], w[i - 8]), w[i - 14]), w[i - 16]), 1)
        end

        local a, b, c, d, e = h0, h1, h2, h3, h4
        for i = 0, 79 do
            local f, k
            if i < 20 then
                f = bor(band(b, c), band(bnot(b), d))
                k = 1518500249
            elseif i < 40 then
                f = bxor(bxor(b, c), d)
                k = 1859775393
            elseif i < 60 then
                f = bor(bor(band(b, c), band(b, d)), band(c, d))
                k = 2400959708
            else
                f = bxor(bxor(b, c), d)
                k = 3395469782
            end
            local temp = (rol(a, 5) + f + e + k + w[i]) % MOD
            e = d
            d = c
            c = rol(b, 30)
            b = a
            a = temp
        end

        h0 = (h0 + a) % MOD
        h1 = (h1 + b) % MOD
        h2 = (h2 + c) % MOD
        h3 = (h3 + d) % MOD
        h4 = (h4 + e) % MOD
    end

    return h0, h1, h2, h3, h4
end

local function hex_of(raw)
    local out = {}
    for i = 1, #raw do
        local b = string.byte(raw, i) or 0
        local hi, lo = floor(b / 16) % 16, b % 16
        out[#out + 1] = string.sub(HEX, hi + 1, hi + 1) .. string.sub(HEX, lo + 1, lo + 1)
    end
    return table.concat(out)
end

local function raw_digest(message)
    local h0, h1, h2, h3, h4 = digest_words(message)
    return word_bytes(h0) .. word_bytes(h1) .. word_bytes(h2) .. word_bytes(h3) .. word_bytes(h4)
end

local function hmac_raw(key, message)
    key = tostring(key)
    if #key > 64 then key = raw_digest(key) end
    local ipad, opad = {}, {}
    for i = 1, 64 do
        local k = string.byte(key, i) or 0
        ipad[i] = string.char(bxor(k, 54))
        opad[i] = string.char(bxor(k, 92))
    end
    local inner = raw_digest(table.concat(ipad) .. tostring(message))
    return raw_digest(table.concat(opad) .. inner)
end

function S.sha1(message) return hex_of(raw_digest(message)) end
function S.hmac(key, message) return hex_of(hmac_raw(key, message)) end
function S.raw(message) return raw_digest(message) end
function S.hmac_raw(key, message) return hmac_raw(key, message) end
S.hex = hex_of

-- `local sha1 = require("sha1")` returns the table; make it callable so
-- `sha1("abc")` also works.
setmetatable(S, { __call = function(self, message) return S.sha1(message) end })

return S
