#!/bin/lua

local version = "1.0.0"

local function ensure_codec()
    local pid = os.getpid("audio-codec")
    if not pid then
        print("play: starting audio codec...")
        os.request("1", "serve", "/bin/audio")
        pid = os.getpid("audio-codec")
        if not pid then
            print("play: failed to start audio codec")
            os.exit(1)
        end
    end
    return pid
end

local function show_help()
    print("OpenTTY Play v" .. version)
    print("")
    print("Usage: play [options] [file]")
    print("")
    print("Commands:")
    print("  play <file>       Play an audio file")
    print("  play --stop       Stop current playback")
    print("  play --pause      Pause current playback")
    print("  play --resume     Resume paused playback")
    print("  play --status     Show playback status")
    print("  play --volume N   Set volume (0-100)")
    print("  play --list       List audio files in current dir")
    print("  play --gui        Open GUI player")
    print("  play --help       Show this help")
    print("")
    print("Supported: MIDI, WAV, MP3 (device dependent)")
end

local function play_file(file)
    local codec = ensure_codec()
    local result = os.request(codec, "play", file)
    if result then
        print("play: " .. result)
    else
        print("play: failed to play " .. file)
    end
end

local function stop_playback()
    local codec = os.getpid("audio-codec")
    if not codec then
        print("play: no audio codec running")
        return
    end
    local result = os.request(codec, "stop")
    if result then print("play: " .. result) end
end

local function pause_playback()
    local codec = os.getpid("audio-codec")
    if not codec then
        print("play: no audio codec running")
        return
    end
    local result = os.request(codec, "pause")
    if result then print("play: " .. result) end
end

local function resume_playback()
    local codec = os.getpid("audio-codec")
    if not codec then
        print("play: no audio codec running")
        return
    end
    local result = os.request(codec, "resume")
    if result then print("play: " .. result) end
end

local function show_status()
    local codec = os.getpid("audio-codec")
    if not codec then
        print("play: no audio codec running")
        return
    end
    local result = os.request(codec, "status")
    if result then print("play: " .. result) end
end

local function set_volume(vol)
    local codec = ensure_codec()
    local result = os.request(codec, "volume", vol)
    if result then print("play: " .. result) end
end

local function list_audio()
    local entries = io.dirs("/home/")
    local audio_exts = { ".mid", ".midi", ".wav", ".mp3", ".amr", ".aac" }
    local found = false

    if entries then
        for _, name in pairs(entries) do
            for _, ext in pairs(audio_exts) do
                if string.endswith(name, ext) then
                    print("  " .. name)
                    found = true
                    break
                end
            end
        end
    end

    entries = io.dirs("/tmp/")
    if entries then
        for _, name in pairs(entries) do
            for _, ext in pairs(audio_exts) do
                if string.endswith(name, ext) then
                    print("  " .. name)
                    found = true
                    break
                end
            end
        end
    end

    if not found then
        print("play: no audio files found")
    end
end

local function gui_player()
    os.setproc("name", "play-gui")
    local previous = graphics.getCurrent()

    local screen = graphics.new("screen", "Play v" .. version)
    local back = graphics.new("command", { label = "Back", type = "back", priority = 1 })
    local play_btn = graphics.new("command", { label = "Play", type = "ok", priority = 1 })
    local stop_btn = graphics.new("command", { label = "Stop", type = "stop", priority = 1 })
    local pause_btn = graphics.new("command", { label = "Pause", type = "screen", priority = 1 })
    local resume_btn = graphics.new("command", { label = "Resume", type = "screen", priority = 1 })

    local file_field = graphics.new("field", { label = "Audio File", value = "", length = 128 })
    local vol_field = graphics.new("field", { label = "Volume (0-100)", value = "80", length = 3 })
    local status_buf = graphics.new("buffer", {})

    graphics.append(screen, status_buf)
    graphics.append(screen, file_field)
    graphics.append(screen, vol_field)
    graphics.addCommand(screen, play_btn)
    graphics.addCommand(screen, stop_btn)
    graphics.addCommand(screen, pause_btn)
    graphics.addCommand(screen, resume_btn)
    graphics.addCommand(screen, back)

    graphics.handler(screen, {
        [back] = function()
            graphics.display(previous)
            os.exit(0)
        end,
        [play_btn] = function(file, vol)
            if file and file ~= "" then
                io.write("Playing: " .. file .. "\n", status_buf, "a")
                play_file(file)
                if vol and vol ~= "" then
                    set_volume(vol)
                end
            else
                io.write("[ERROR] No file specified\n", status_buf, "a")
            end
        end,
        [stop_btn] = function()
            stop_playback()
            io.write("Stopped\n", status_buf, "a")
        end,
        [pause_btn] = function()
            pause_playback()
            io.write("Paused\n", status_buf, "a")
        end,
        [resume_btn] = function()
            resume_playback()
            io.write("Resumed\n", status_buf, "a")
        end,
    })
    graphics.display(screen)
end

os.setproc("name", "play")

if arg[1] == nil or arg[1] == "--help" or arg[1] == "-h" then
    show_help()
elseif arg[1] == "--stop" or arg[1] == "-s" then
    stop_playback()
elseif arg[1] == "--pause" or arg[1] == "-p" then
    pause_playback()
elseif arg[1] == "--resume" or arg[1] == "-r" then
    resume_playback()
elseif arg[1] == "--status" then
    show_status()
elseif arg[1] == "--volume" or arg[1] == "-v" then
    if arg[2] then
        set_volume(arg[2])
    else
        print("play: usage: play --volume <0-100>")
    end
elseif arg[1] == "--list" or arg[1] == "-l" then
    list_audio()
elseif arg[1] == "--gui" or arg[1] == "-g" then
    gui_player()
else
    local file = arg[1]
    if string.sub(file, 1, 1) ~= "/" then
        file = os.getcwd() .. "/" .. file
    end
    play_file(file)
end
