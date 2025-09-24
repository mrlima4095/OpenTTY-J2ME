local function replace_all(text, find, repl)
    local result, i = "", 1
    while i <= #text do
        local sub = string.sub(text, i, i + #find - 1)
        if sub == find then
            result = result .. repl
            i = i + #find
        else
            result = result .. string.sub(text, i, i)
            i = i + 1
        end
    end
    return result
end

local function text2note(content)
    if content == nil or content == "" then return "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:\nEND:VNOTE" end

    content = replace_all(content, "=", "=3D")
    content = replace_all(content, "\n", "=0A")

    local vnote = "BEGIN:VNOTE\nVERSION:1.1\nBODY;ENCODING=QUOTED-PRINTABLE;CHARSET=UTF-8:" .. content .. "\nEND:VNOTE"

    return vnote
end

return text2note
