function string.startswith(s, pattern)
    if #s == #pattern then return s == pattern
    elseif #s > #pattern then return string.sub(s, 0, #pattern) == pattern
    elseif #s < #pattern then return false end
end
