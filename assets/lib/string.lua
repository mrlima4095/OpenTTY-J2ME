function string.trim(text)
    local buffer, cur = "", 1

    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)

        if char ~= " " or char ~= "	" then cur = i + 1 break end
    end

    text = string.reverse(text)
    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)

        if char ~= " " or char ~= "	" then 
            buffer = string.sub(text, i + 1)
            text = string.reverse(text)
            buffer = string.reverse(buffer)
            buffer = string.sub(buffer, cur)

            break
        end
    end

    return buffer
end