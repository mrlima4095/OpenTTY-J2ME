function string.trim(text)
    local buffer = ""

    for i=1,string.len(text) do
        local cur = string.sub(text, i, i)

        if cur ~= " " or cur ~= "	" then
            buffer = buffer .. cur
        end
    end

    return buffer
end