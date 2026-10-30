-- rock: name=sha256
-- rock: version=1.0.0
-- rock: description=SHA-256 and HMAC-SHA-256 (pure Lua, 32-bit arithmetic)
-- rock: here=/lib/sha256.lua

-- Same approach as the sha1 rock: nibble lookup tables for the bitwise ops,
-- all arithmetic kept below 2^53. The round constants live in a hex string
-- because the runtime has no 0x literals.
--
--   local sha256 = require("sha256")
--   print(sha256.sha256("abc"))
--   print(sha256.hmac("key", "abc"))
local S = sha256 or {}

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

local function ror(a, n)
    n = n % 32
    if n == 0 then return a % MOD end
    return floor(a / (2 ^ n)) + (a % (2 ^ n)) * (2 ^ (32 - n))
end

local function split_words(text)
    local out = {}
    local pos = 1
    while pos <= #text do
        local space = string.find(text, " ", pos, true)
        if space == nil then
            out[#out + 1] = string.sub(text, pos)
            break
        end
        local word = string.sub(text, pos, space - 1)
        if word ~= "" then out[#out + 1] = word end
        pos = space + 1
    end
    return out
end

local K_HEX = "428a2f98 71374491 b5c0fbcf e9b5dba5 3956c25b 59f111f1 923f82a4 ab1c5ed5 " ..
    "d807aa98 12835b01 243185be 550c7dc3 72be5d74 80deb1fe 9bdc06a7 c19bf174 " ..
    "e49b69c1 efbe4786 0fc19dc6 240ca1cc 2de92c6f 4a7484aa 5cb0a9dc 76f988da " ..
    "983e5152 a831c66d b00327c8 bf597fc7 c6e00bf3 d5a79147 06ca6351 14292967 " ..
    "27b70a85 2e1b2138 4d2c6dfc 53380d13 650a7354 766a0abb 81c2c92e 92722c85 " ..
    "a2bfe8a1 a81a664b c24b8b70 c76c51a3 d192e819 d6990624 f40e3585 106aa070 " ..
    "19a4c116 1e376c08 2748774c 34b0bcb5 391c0cb3 4ed8aa4a 5b9cca4f 682e6ff3 " ..
    "748f82ee 78a5636f 84c87814 8cc70208 90befffa a4506ceb bef9a3f7 c67178f2"

local K = {}
local kwords = split_words(K_HEX)
for i = 1, #kwords do K[i - 1] = tonumber(kwords[i], 16) end

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
    local h = {
        1779033703, 3144134277, 1013904242, 2773480762,
        1359893119, 2600822924, 528734635, 1541459225
    }

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
        for i = 16, 63 do
            local s0 = bxor(bxor(ror(w[i - 15], 7), ror(w[i - 15], 18)), floor(w[i - 15] / 8))
            local s1 = bxor(bxor(ror(w[i - 2], 17), ror(w[i - 2], 19)), floor(w[i - 2] / 1024))
            w[i] = (w[i - 16] + s0 + w[i - 7] + s1) % MOD
        end

        local a, b, c, d, e, f, g, hh = h[1], h[2], h[3], h[4], h[5], h[6], h[7], h[8]
        for i = 0, 63 do
            local S1 = bxor(bxor(ror(e, 6), ror(e, 11)), ror(e, 25))
            local ch = bxor(band(e, f), band(bnot(e), g))
            local temp1 = (hh + S1 + ch + K[i] + w[i]) % MOD
            local S0 = bxor(bxor(ror(a, 2), ror(a, 13)), ror(a, 22))
            local maj = bxor(bxor(band(a, b), band(a, c)), band(b, c))
            local temp2 = (S0 + maj) % MOD
            hh = g
            g = f
            f = e
            e = (d + temp1) % MOD
            d = c
            c = b
            b = a
            a = (temp1 + temp2) % MOD
        end

        h[1] = (h[1] + a) % MOD
        h[2] = (h[2] + b) % MOD
        h[3] = (h[3] + c) % MOD
        h[4] = (h[4] + d) % MOD
        h[5] = (h[5] + e) % MOD
        h[6] = (h[6] + f) % MOD
        h[7] = (h[7] + g) % MOD
        h[8] = (h[8] + hh) % MOD
    end

    return h[1], h[2], h[3], h[4], h[5], h[6], h[7], h[8]
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
    local h1, h2, h3, h4, h5, h6, h7, h8 = digest_words(message)
    return word_bytes(h1) .. word_bytes(h2) .. word_bytes(h3) .. word_bytes(h4) ..
        word_bytes(h5) .. word_bytes(h6) .. word_bytes(h7) .. word_bytes(h8)
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

function S.sha256(message) return hex_of(raw_digest(message)) end
function S.hmac(key, message) return hex_of(hmac_raw(key, message)) end
function S.raw(message) return raw_digest(message) end
function S.hmac_raw(key, message) return hmac_raw(key, message) end
S.hex = hex_of

setmetatable(S, { __call = function(self, message) return S.sha256(message) end })

return S
