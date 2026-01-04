#!/bin/lua

os.setproc("name", "audio")

if arg[1] == "--deamon" then
    local ok, status, player

    return function (payload, args, scope, pid, uid)
        if payload == "play" then
            ok, player = pcall(audio.load, args)
            if not ok then
                return ":: loading failed - " .. tostring(player)
            end
            ok, status = pcall(audio.play, player)
            if not ok or status ~= 0 then
                pcall(io.close, player)
                player = nil

                return ":: failed to play"
            end
        elseif payload == "pause" then
            if player == nil then
                return ":: no audio running"
            end

            ok, status = pcall(audio.pause, player)
            if not ok or status ~= 0 then
                return ":: failed to pause"
            end
        elseif payload == "resume" then
            if player == nil then
                return ":: no audio running"
            end

            ok, status = pcall(audio.play, player)
            if not ok or status ~= 0 then
                return ":: failed to play"
            end

        elseif payload == "stop" then
            if player == nil then
                return ":: no audio running"
            end

            ok, status = pcall(audio.pause, player)
            ok, status = pcall(io.close, player)
        elseif payload == "volume" then
            if player == nil then
                return ":: no audio running"
            end

            ok, status = pcall(audio.play, player)
            if not ok or status ~= 0 then
                return ":: failed to play"
            end
        end
    end
end

local codec = os.getpid("audio")
if codec == nil then
    print(":: audio codec not running")
    print(":: loading...")

    os.request("1", "serve", arg[0])
    codec = os.getpid("audio")
    print(":: codec running with pid " .. codec)
end

if arg[1] then
    local message, status os.request(codec, arg[1], arg[2])
    
    print(message)

    os.exit(status)
end