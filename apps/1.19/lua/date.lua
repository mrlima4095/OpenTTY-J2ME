-- rock: name=date
-- rock: version=1.0.0
-- rock: description=Date and time helpers: format, parse, parts, add, diff, relative
-- rock: here=/lib/date.lua

-- `os.date` in the runtime understands only these specifiers:
--   %Y %y %m %d %e %H %I %M %S %j %A %a %B %b %p %Z %%
-- so this module computes the missing pieces (weekday number, month/day
-- arithmetic) itself and never relies on `os.date("*t")`.
--
-- Timestamps are the usual Lua numbers: seconds since the Unix epoch.
local D = date or {}

local floor = function(x) return x - x % 1 end

local MONTHS = {
    "January", "February", "March", "April", "May", "June",
    "July", "August", "September", "October", "November", "December"
}
local DAYS = {
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
}

local function int_text(value)
    local text = tostring(value)
    local dot = string.find(text, ".", 1, true)
    if dot then text = string.sub(text, 1, dot - 1) end
    return text
end

local function split_spaces(text)
    local out = {}
    local pos = 1
    while pos <= #text do
        local space = string.find(text, " ", pos, true)
        if space == nil then
            out[#out + 1] = string.sub(text, pos)
            break
        end
        out[#out + 1] = string.sub(text, pos, space - 1)
        pos = space + 1
    end
    return out
end

function D.now()
    return os.time()
end

function D.format(timestamp, format)
    return os.date(format or "%Y-%m-%d %H:%M:%S", timestamp or os.time())
end

function D.iso(timestamp)
    return os.date("%Y-%m-%dT%H:%M:%S", timestamp or os.time())
end

function D.date(timestamp)
    return os.date("%Y-%m-%d", timestamp or os.time())
end

function D.time(timestamp)
    return os.date("%H:%M:%S", timestamp or os.time())
end

function D.is_leap(year)
    year = tonumber(year) or 1970
    return (year % 4 == 0 and year % 100 ~= 0) or year % 400 == 0
end

function D.days_in_month(year, month)
    year, month = tonumber(year) or 1970, tonumber(month) or 1
    if month == 2 then
        if D.is_leap(year) then return 29 end
        return 28
    end
    if month == 4 or month == 6 or month == 9 or month == 11 then return 30 end
    return 31
end

function D.month_name(month)
    return MONTHS[tonumber(month) or 1] or ""
end

function D.weekday_name(index)
    return DAYS[tonumber(index) or 1] or ""
end

-- 1 = Sunday, matching `%w` on a stock Lua.
function D.weekday(timestamp)
    return floor((timestamp or os.time()) / 86400 + 4) % 7 + 1
end

function D.parts(timestamp)
    timestamp = timestamp or os.time()
    local fields = split_spaces(os.date("%Y %m %d %H %M %S", timestamp))
    local year, month, day = tonumber(fields[1]), tonumber(fields[2]), tonumber(fields[3])
    local hour, min, sec = tonumber(fields[4]), tonumber(fields[5]), tonumber(fields[6])

    local yday = day
    for m = 1, month - 1 do yday = yday + D.days_in_month(year, m) end

    return {
        year = year, month = month, day = day,
        hour = hour, min = min, sec = sec,
        wday = D.weekday(timestamp), yday = yday
    }
end

local function build(year, month, day, hour, min, sec)
    return os.time({
        year = year, month = month, day = day,
        hour = hour or 0, min = min or 0, sec = sec or 0
    })
end

local function valid(year, month, day, hour, min, sec)
    if month < 1 or month > 12 then return false end
    if day < 1 or day > D.days_in_month(year, month) then return false end
    if hour < 0 or hour > 23 then return false end
    if min < 0 or min > 59 then return false end
    if sec < 0 or sec > 59 then return false end
    return true
end

local function parse_with_format(text, format)
    local year, month, day, hour, min, sec = 1970, 1, 1, 0, 0, 0
    local ti = 1
    local i = 1
    while i <= #format do
        local c = string.sub(format, i, i)
        if c == "%" and i < #format then
            local spec = string.sub(format, i + 1, i + 1)
            local digits = (spec == "Y" or spec == "y") and 4 or 2
            local chunk = string.sub(text, ti, ti + digits - 1)
            local number = tonumber(chunk)
            if number == nil then return nil, "invalid date: expected a number at position " .. ti end
            if spec == "Y" then year = number
            elseif spec == "y" then year = number < 70 and 2000 + number or 1900 + number
            elseif spec == "m" then month = number
            elseif spec == "d" then day = number
            elseif spec == "H" then hour = number
            elseif spec == "M" then min = number
            elseif spec == "S" then sec = number
            else return nil, "unsupported format specifier %" .. spec end
            ti = ti + digits
            i = i + 2
        else
            if string.sub(text, ti, ti) ~= c then
                return nil, "invalid date: expected '" .. c .. "' at position " .. ti
            end
            ti = ti + 1
            i = i + 1
        end
    end
    if not valid(year, month, day, hour, min, sec) then return nil, "invalid date values" end
    return build(year, month, day, hour, min, sec)
end

local function scan_numbers(text)
    local out = {}
    local i = 1
    while i <= #text do
        local c = string.sub(text, i, i)
        if c >= "0" and c <= "9" then
            local start = i
            while i <= #text do
                local d = string.sub(text, i, i)
                if d >= "0" and d <= "9" then i = i + 1 else break end
            end
            out[#out + 1] = tonumber(string.sub(text, start, i - 1))
        else i = i + 1 end
    end
    return out
end

-- `parse("2026-10-30")`, `parse("2026-10-30 12:34:56")`,
-- `parse("30/10/2026 12:34", "%d/%m/%Y %H:%M")`.
function D.parse(text, format)
    text = tostring(text or "")
    if format ~= nil then return parse_with_format(text, format) end

    local numbers = scan_numbers(text)
    if #numbers < 3 then return nil, "invalid date: expected at least year, month and day" end

    local year, month, day = numbers[1], numbers[2], numbers[3]
    local hour = numbers[4] or 0
    local min = numbers[5] or 0
    local sec = numbers[6] or 0
    if not valid(year, month, day, hour, min, sec) then return nil, "invalid date values" end
    return build(year, month, day, hour, min, sec)
end

function D.diff(a, b)
    return (a or os.time()) - (b or os.time())
end

function D.add(timestamp, seconds)
    return (timestamp or os.time()) + (seconds or 0)
end

function D.add_days(timestamp, days)
    return (timestamp or os.time()) + (days or 0) * 86400
end

function D.start_of_day(timestamp)
    local p = D.parts(timestamp)
    return build(p.year, p.month, p.day, 0, 0, 0)
end

function D.end_of_day(timestamp)
    return D.start_of_day(timestamp) + 86399
end

function D.today()
    return D.start_of_day(os.time())
end

function D.same_day(a, b)
    return D.date(a) == D.date(b)
end

function D.relative(timestamp, from)
    timestamp = timestamp or os.time()
    local now = from or os.time()
    local delta = now - timestamp
    local future = delta < 0
    if delta < 0 then delta = -delta end

    local count, unit
    if delta < 5 then
        return future and "in a moment" or "just now"
    elseif delta < 60 then
        count, unit = floor(delta), "second"
    elseif delta < 3600 then
        count, unit = floor(delta / 60), "minute"
    elseif delta < 86400 then
        count, unit = floor(delta / 3600), "hour"
    elseif delta < 2592000 then
        count, unit = floor(delta / 86400), "day"
    elseif delta < 31536000 then
        count, unit = floor(delta / 2592000), "month"
    else
        count, unit = floor(delta / 31536000), "year"
    end

    if count ~= 1 then unit = unit .. "s" end
    if future then return "in " .. int_text(count) .. " " .. unit end
    return int_text(count) .. " " .. unit .. " ago"
end

return D
