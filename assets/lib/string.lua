function string.startswith(s, pattern)
    if #s == #pattern then return s == pattern
    elseif #s > #pattern then return string.sub(s, 1, #pattern) == pattern
    elseif #s < #pattern then return false end
end
function string.endswith(s, pattern)
    if #s == #pattern then return s == pattern
    elseif #s > #pattern then return string.sub(s, #pattern, #s) == pattern
    elseif #s < #pattern then return false end
end

function string.replace(s, find, repl)
    local result, i = "", 1
    while i <= #s do
        local sub = string.sub(s, i, i + #find - 1)
        if sub == find then
            result = result .. repl
            i = i + #find
        else
            result = result .. string.sub(s, i, i)
            i = i + 1
        end
    end
    return result
end

function string.text2note(s)
    if s == nil or s == "" then return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:\nEND:VNOTE" end

    s = string.replace(s, "=", "=3D")
    s = string.replace(s, "\n", "=0A")

    local vnote = "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" .. s .. "\nEND:VNOTE"

    return vnote
end


return string