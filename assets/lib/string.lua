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

function string.hash(text)
    if text == nil then error("string.hash cannot get from nil") end
    
    local hash = 0
    for i = 1, string.len(text) do
        local charCode = string.byte(text, i)
        hash = (31 * hash + charCode) % 2 ^ 32
        if hash >= 2 ^ 31 then hash = hash - 2 ^ 32 end
    end
    return hash
end
