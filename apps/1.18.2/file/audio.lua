#!/bin/lua

if arg[1] == "--deamon" then
    os.setproc("name", "audio-codec")
    local player

    local function mime_of(file)
        if string.endswith(file, ".mid") or string.endswith(file, ".midi") then return "audio/midi" end
        if string.endswith(file, ".wav") then return "audio/x-wav" end
        if string.endswith(file, ".amr") then return "audio/amr" end
        if string.endswith(file, ".aac") then return "audio/aac" end
        if string.endswith(file, ".mp4") or string.endswith(file, ".m4a") then return "audio/mp4" end
        return "audio/mpeg"
    end

    local function intpart(value)
        local digits = string.match(tostring(value), "^(%-?%d+)")
        return tonumber(digits) or 0
    end

    return function (payload, args, scope, pid, uid)
        if payload == "play" then
            if not args then return ":: usage: play [file]" end
            if player ~= nil then
                pcall(audio.pause, player)
                player = nil
            end

            local file = os.join(tostring(args))
            local ok, result = pcall(audio.load, file, mime_of(file))
            if not ok or not result then return ":: failed to load " .. tostring(args) end
            player = result

            local started = pcall(audio.play, player)
            return started and ":: playing " .. tostring(args) or ":: failed to play"
        elseif payload == "pause" then
            if player == nil then return ":: no running audio" end
            local ok = pcall(audio.pause, player)
            return ok and ":: paused" or ":: failed to pause"
        elseif payload == "resume" then
            if player == nil then return ":: no running audio" end
            local ok = pcall(audio.play, player)
            return ok and ":: resumed" or ":: failed to resume"
        elseif payload == "stop" then
            if player == nil then return ":: no running audio" end
            local ok = pcall(io.close, player)
            player = nil
            return ok and ":: stopped" or ":: failed to stop"
        elseif payload == "volume" then
            if player == nil then return ":: no running audio" end
            if args then
                local level = tonumber(args)
                if not level then return ":: invalid volume" end
                local ok, result = pcall(audio.volume, player, level)
                return ok and result == 0 and ":: volume set" or ":: failed to set volume"
            else
                local ok, result = pcall(audio.volume, player)
                return ok and ":: volume " .. tostring(result) or ":: failed to get volume"
            end
        elseif payload == "status" then
            if player == nil then return ":: no running audio" end
            local time = audio.time(player)
            local duration = audio.duration(player)
            if time and time >= 0 and duration and duration >= 0 then
                return ":: playing (" .. intpart(time) .. "/" .. intpart(duration) .. " sec)"
            elseif time and time >= 0 then
                return ":: playing (" .. intpart(time) .. " sec)"
            else
                return ":: playing"
            end
        else
            return ":: unknown command: " .. tostring(payload)
        end
    end
end

os.setproc("name", "audio-cli")

if arg[1] == "-h" or arg[1] == "--help" then
    print("audio [command] [file]")
    print("")
    print("Commands:")
    print("  play [file]     Play an audio file (resolves relative paths)")
    print("  pause           Pause current playback")
    print("  resume          Resume paused playback")
    print("  stop            Stop and release the current player")
    print("  volume [level]  Get or set the volume (0-100)")
    print("  status          Show playback position and duration")
    os.exit(0)
end

local codec = os.getpid("audio-codec")
if codec == nil then
    print(":: audio codec not running")
    print(":: loading...")

    os.request("1", "serve", arg[0])
    codec = os.getpid("audio-codec")
    if codec == nil then
        print(":: failed to start audio codec")
        os.exit(1)
    end
    print(":: codec running with pid " .. codec)
end

if arg[1] then
    local value = arg[2]
    if value and (arg[1] == "play") then
        value = os.join(value)
    end

    local message = os.request(codec, arg[1], value)

    if message then
        print(message)
    end

    os.exit(0)
else
    print("audio [command] [file]")
end