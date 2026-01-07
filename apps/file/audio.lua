#!/bin/lua

if arg[1] == "--deamon" then
    os.setproc("name", "audio-codec")
    local ok, status, player

    return function (payload, args, scope, pid, uid)
        if payload == "play" then
            player = audio.load(args)
            audio.play(player)
            return ":: playing"
        elseif player == "pause" then
            if player == nil then return ": no running audio" end

            status = audio.pause(player)
            return status == 0 and ":: paused" or ":: failed to pause"
        elseif player == "resume" then
            if player == nil then return ":: no running audio" end

            status = pcall(audio.play, player)
            return status == 0 and ":: resumed" or ":: failed to resume"
        elseif player == "stop" then
            if player == nil then return ":: no running audio" end

            ok, status = pcall(io.close, player)
            player = nil
            return ":: stopped"
        elseif player == "volume" then
            if player == nil then return ":: no running audio" end

            if args then
                ok, status = pcall(audio.volume, player, tonumber(args))
                if ok and status == 0 then
                    return ":: volume set"
                else
                    return ":: failed to set volume"
                end
            else
                ok, status = pcall(audio.volume, player)
                if ok then
                    return ":: " .. status
                else
                    return ":: failed to get volume"
                end
            end
        elseif player == "status" then
            if player == nil then
                return ":: no running audio"
            else
                local time, duration = audio.time(player), audio.duration(player)
                if time and duration then
                    return ":: playing (" .. time .. "/" .. duration .. " sec)"
                else
                    return ":: playing (unknown duration)"
                end
            end
        else
            return ":: unknown command"
        end
    end
end

os.setproc("name", "audio-cli")

local codec = os.getpid("audio-codec")
if codec == nil then
    print(":: audio codec not running")
    print(":: loading...")

    os.request("1", "serve", arg[0])
    codec = os.getpid("audio-codec")
    print(":: codec running with pid " .. codec)
end

if arg[1] then
    local message = os.request(codec, arg[1], arg[2])

    if message then
        print(message)
    end

    os.exit(0)
else
    print("audio [option] [file]")
end