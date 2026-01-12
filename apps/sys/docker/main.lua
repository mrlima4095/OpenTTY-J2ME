#!/bin/lua

os.setproc("name", "docker")

local server = os.getpid("conteinerd")
if not server then
    os.request(1, "serve", arg[0])
    server = os.getpid("conteinerd")
    if not server then
        print("docker: failed to up conteiner service")
        os.exit(69)
    end
end

if arg[1] == "run" then
    if arg[2] then
        local image = io.open(os.join(arg[2]))
        if image then
            local res = os.request(server, "run", { file = os.join(arg[2]), image = image })
        else
            print("docker: " .. arg[2] .. ": image not found")
        end
    else
        print("docker: usage: docker run [file]")
    end
elseif arg[1] == "exec" then
    if arg[2] and arg[3] then

    else
        print("docker: usage: docker exec [conteiner] [commands]")
    end
elseif arg[1] == "ps" then

elseif arg[1] == "pull" then
    if arg[2] then

    else
        print("docker: usage: docker pull [source]")
    end
elseif arg[1] == "login" then
    if arg[2] and arg[3] and arg[4] then
        
    else
        print("docker: usage: docker login [conteiner] [user] [password]")
    end
elseif arg[1] == "stop" then
    if arg[2] then

    else
        print("docker: usage: docker stop [conteiner]")
    end
elseif arg[1] == "kill" then
    if arg[2] then

    else
        print("docker: usage: docker kill [conteiner]")
    end
elseif arg[1] == "--help" then
    print("")
elseif arg[1] == "--deamon" then
    os.setproc("name", "conteinerd")
    local db = {}

    return function (payload, args, scope, pid, uid)
        if payload == "run" then
            
        elseif payload == "exec" then
        elseif payload == "ps" then
        elseif payload == "login" then
        elseif payload == "stop" then
        elseif payload == "kill" then
        end
    end
else
    print("docker: '" .. arg[1] .. "' is not a docker command.")
    print("See 'docker --help'")
end


