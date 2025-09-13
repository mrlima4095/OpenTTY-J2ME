local function split(text, sep)
    local result, buffer, cur = {}, "", 1
    
    for i = 1, string.len(text) do
        local char = string.sub(text, i, i)
        
        if char == sep then
            result[cur] = buffer
            cur = cur + 1
            buffer = ""
        else
            buffer = buffer .. char
        end
    end
    
    result[cur] = buffer
    
    return result
end

print(split("gg ez", " "))