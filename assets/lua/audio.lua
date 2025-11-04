local audio = {}

function audio.play(file)
    if type(file) ~= "string" then error("bad argument #1 to 'play' (string expected, got " .. type(file) .. ")") end

    local status = os.execute("audio play " .. file)

    if status == 13 then error("Permission denied!")
    elseif status == 1 then error("Generic error") end

    return status
end


return audio