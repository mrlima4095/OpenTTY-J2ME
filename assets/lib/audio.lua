local audio = {}

function audio.play(file)
    if type(file) ~= "string" then error("bad argument #1 to 'play' (string expected, got " .. type(file) .. ")") end

    local status = os.execute("audio play " .. file)

    if status ~= 0 then

    end

    return status
end