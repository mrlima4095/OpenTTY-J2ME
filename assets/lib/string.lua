function string.trim(text)
    local buffer, x, y = "", 1, 1

    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)

        x = i
        if char ~= " " or char ~= "	" then break end
    end

    text = string.reverse(text)

    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)

        y = i
        if char ~= " " or char ~= "	" then break end
    end

    return buffer
end