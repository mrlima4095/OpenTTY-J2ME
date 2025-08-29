function string.trim(text)
    local start_index = 1
    local end_index = string.len(text)

    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)
        if char ~= " " and char ~= "\t" then
            start_index = i
            break
        end
    end

    local reversed = string.reverse(text)

    for i = 1, string.len(reversed) do
        local char = string.sub(reversed, i, i)
        if char ~= " " and char ~= "\t" then
            end_index = string.len(text) - i + 1
            break
        end
    end

    return string.sub(text, start_index, end_index)
end
