-- rock: name=base64
-- rock: version=1.0.0
-- rock: description=Base64 (standard and URL-safe) plus hex helpers
-- rock: here=/lib/base64.lua

-- The runtime exposes a native `base64.encode/decode` that encodes the UTF-8
-- bytes of a Java string. This module extends that table with a pure-Lua
-- implementation (byte exact for 0..255), URL-safe variants and hex helpers,
-- and keeps the native functions untouched under `B.native`.
local B = base64 or {}

B.native = { encode = B.encode, decode = B.decode }

local ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
local URL_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
local ENCODE = {}
local URL_ENCODE = {}
local DECODE = {}

for i = 1, 64 do
    local c = string.sub(ALPHABET, i, i)
    ENCODE[i - 1] = c
    DECODE[c] = i - 1
    URL_ENCODE[i - 1] = string.sub(URL_ALPHABET, i, i)
end

local floor = function(x) return x - x % 1 end
local HEX = "0123456789abcdef"

local function to_bytes(data)
    if type(data) == "table" then
        local out = {}
        for i = 1, #data do
            local value = data[i] % 256
            if value < 0 then value = value + 256 end
            out[i] = string.char(value)
        end
        return table.concat(out)
    end
    return tostring(data)
end

local function encode_impl(data, alphabet)
    data = to_bytes(data)
    local out = {}
    local i = 1
    while i <= #data do
        local b1 = string.byte(data, i) or 0
        local b2 = string.byte(data, i + 1)
        local b3 = string.byte(data, i + 2)
        local n = b1 * 65536 + (b2 or 0) * 256 + (b3 or 0)
        out[#out + 1] = alphabet[floor(n / 262144) % 64]
        out[#out + 1] = alphabet[floor(n / 4096) % 64]
        if b2 ~= nil then out[#out + 1] = alphabet[floor(n / 64) % 64] else out[#out + 1] = "=" end
        if b3 ~= nil then out[#out + 1] = alphabet[n % 64] else out[#out + 1] = "=" end
        i = i + 3
    end
    return table.concat(out)
end

local function decode_impl(text, url_safe)
    text = tostring(text)
    local out = {}
    local acc, bits = 0, 0
    for i = 1, #text do
        local c = string.sub(text, i, i)
        if c ~= "=" and c ~= " " and c ~= "\n" and c ~= "\r" and c ~= "\t" then
            local value = DECODE[c]
            if value == nil and url_safe then
                if c == "-" then value = 62
                elseif c == "_" then value = 63 end
            end
            if value == nil then error("base64: invalid character '" .. c .. "'") end
            acc = acc * 64 + value
            bits = bits + 6
            if bits >= 8 then
                bits = bits - 8
                local divisor = 2 ^ bits
                out[#out + 1] = string.char(floor(acc / divisor) % 256)
                acc = acc % divisor
            end
        end
    end
    return table.concat(out)
end

function B.encode(data)
    return encode_impl(data, ENCODE)
end

function B.decode(text)
    return decode_impl(text, false)
end

function B.encode_url(data)
    local text = encode_impl(data, URL_ENCODE)
    return text
end

function B.decode_url(text)
    return decode_impl(text, true)
end

function B.hex(text)
    text = tostring(text)
    local out = {}
    for i = 1, #text do
        local b = string.byte(text, i) or 0
        local hi = floor(b / 16) % 16
        local lo = b % 16
        out[#out + 1] = string.sub(HEX, hi + 1, hi + 1) .. string.sub(HEX, lo + 1, lo + 1)
    end
    return table.concat(out)
end

function B.unhex(text)
    text = tostring(text)
    if #text % 2 ~= 0 then error("base64.unhex: odd number of hex digits") end
    local out = {}
    for i = 1, #text, 2 do
        local hi = tonumber(string.sub(text, i, i), 16)
        local lo = tonumber(string.sub(text, i + 1, i + 1), 16)
        if hi == nil or lo == nil then error("base64.unhex: invalid hex digit") end
        out[#out + 1] = string.char(hi * 16 + lo)
    end
    return table.concat(out)
end

-- Byte array <-> string, handy for hashes and binary protocols.
function B.bytes(text)
    local out = {}
    for i = 1, #text do out[i] = string.byte(text, i) or 0 end
    return out
end

function B.from_bytes(list)
    return to_bytes(list)
end

return B
