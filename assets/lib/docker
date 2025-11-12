#!/bin/lua
-- Docker para OpenTTY
-- Implementa gerenciamento de contêineres leves usando o sistema de processos do OpenTTY

local docker = {}
local containers = {}
local current_id = 1

-- Função para gerar ID único
local function generate_id()
    local id = "ct_" .. current_id
    current_id = current_id + 1
    return id
end

-- Função para salvar estado dos contêineres
local function save_state()
    local state = {}
    for id, container in pairs(containers) do
        state[id] = {
            name = container.name,
            script = container.script,
            status = container.status,
            pid = container.pid
        }
    end
    os.write("/tmp/docker_state", table.serialize(state))
end

-- Função para carregar estado dos contêineres
local function load_state()
    local state_data = os.read("/tmp/docker_state")
    if state_data and state_data ~= "" then
        containers = table.unserialize(state_data) or {}
    end
end

-- Inicializar modo daemon
if arg[1] == "--deamon" then 
    os.setproc("name", "dockerd") 
    
    -- Carregar estado anterior
    load_state()
    
    -- Restaurar contêineres que estavam rodando
    for id, container in pairs(containers) do
        if container.status == "running" and not container.pid then
            -- Tentar reiniciar contêiner
            local success, new_pid = pcall(function()
                return os.execute("bg " .. container.script .. " &")
            end)
            if success and new_pid then
                containers[id].pid = new_pid
                print("Restarted container: " .. id)
            else
                containers[id].status = "stopped"
            end
        end
    end
    
    save_state()
    
    return function(payload, source, pid, id)
        local cmd = payload:match("^(%S+)") or ""
        local args = payload:sub(#cmd + 2)
        
        if cmd == "ps" then return containers
        elseif cmd == "run" then
            local name = args:match("^--name=(%S+)")
            local script = name and args:sub(#name + 9) or args
            
            if script == "" then
                return "Error: No script specified"
            end
            
            local container_id = generate_id()
            local success, new_pid = pcall(function()
                return os.execute("bg " .. script .. " &")
            end)
            
            if success and new_pid then
                containers[container_id] = {
                    name = name or container_id,
                    script = script,
                    status = "running",
                    pid = new_pid
                }
                save_state()
                return "Started container: " .. container_id .. " (PID: " .. new_pid .. ")"
            else
                return "Error: Failed to start container"
            end
            
        elseif cmd == "exec" then
            local container_id = args:match("^(%S+)")
            local command = args:sub(#container_id + 2)
            
            if not containers[container_id] then
                return "Error: Container not found: " .. container_id
            end
            
            local container = containers[container_id]
            if container.status ~= "running" then
                return "Error: Container is not running: " .. container_id
            end
            
            -- Executar comando no contexto do contêiner
            local result = os.execute("svchost " .. container.pid .. " " .. command)
            return result or "Command executed"
            
        elseif cmd == "stop" then
            local container_id = args
            if container_id == "all" then
                local count = 0
                for id, container in pairs(containers) do
                    if container.status == "running" and container.pid then
                        os.execute("kill " .. container.pid)
                        containers[id].status = "stopped"
                        containers[id].pid = nil
                        count = count + 1
                    end
                end
                save_state()
                return "Stopped " .. count .. " containers"
            else
                if not containers[container_id] then
                    return "Error: Container not found: " .. container_id
                end
                
                local container = containers[container_id]
                if container.status == "running" and container.pid then
                    os.execute("kill " .. container.pid)
                    container.status = "stopped"
                    container.pid = nil
                    save_state()
                    return "Stopped container: " .. container_id
                else
                    return "Container already stopped: " .. container_id
                end
            end
            
        elseif cmd == "rm" then
            local container_id = args
            if container_id == "all" then
                local count = 0
                for id, container in pairs(containers) do
                    if container.status == "stopped" then
                        containers[id] = nil
                        count = count + 1
                    end
                end
                save_state()
                return "Removed " .. count .. " containers"
            else
                if containers[container_id] and containers[container_id].status == "stopped" then
                    containers[container_id] = nil
                    save_state()
                    return "Removed container: " .. container_id
                else
                    return "Error: Container not found or still running: " .. container_id
                end
            end
            
        elseif cmd == "logs" then
            local container_id = args
            if not containers[container_id] then
                return "Error: Container not found: " .. container_id
            end
            
            -- Retornar logs do contêiner (implementação básica)
            return "Logs for " .. container_id .. ":\n" ..
                   "Name: " .. (containers[container_id].name or "unnamed") .. "\n" ..
                   "Status: " .. (containers[container_id].status or "unknown") .. "\n" ..
                   "Script: " .. (containers[container_id].script or "none")
                   
        elseif cmd == "get" then
            local container_id = args
            if containers[container_id] then
                return table.serialize(containers[container_id])
            else
                return "Error: Container not found: " .. container_id
            end
            
        else
            return "Unknown docker command: " .. cmd
        end
    end
end

local host = os.getpid("dockerd")

if arg[1] == nil or arg[1] == "help" then
elseif not host then print("Docker Service not runnning")
elseif arg[1] == "ps" then
    containers = os.request(host, "ps")

    local output = "CONTAINER ID\tNAME\tSTATUS\tPID\n"
    for container_id, container in pairs(containers) do
        output = output .. container_id .. "\t" .. (container.name or "unnamed") .. "\t" .. (container.status or "unknown") .. "\t" .. (container.pid or "none") .. "\n"
    end
    return output
elseif arg[1] == "run" then
    local name = arg[2] and arg[2]:match("^--name=(.+)")
    local script
    
    if name then
        script = table.concat(arg, " ", 3)
    else
        script = table.concat(arg, " ", 2)
    end
    
    if not script or script == "" then
        print("Error: No script specified")
        print("Usage: docker run [--name=NAME] SCRIPT")
        return
    end
    
    local result = os.execute("svchost dockerd run " .. (name and "--name=" .. name .. " " or "") .. script)
    print(result or "")
    
elseif arg[1] == "exec" then
    if not arg[2] or not arg[3] then
        print("Error: Container ID and command required")
        print("Usage: docker exec CONTAINER_ID COMMAND")
        return
    end
    
    local container_id = arg[2]
    local command = table.concat(arg, " ", 3)
    local result = os.execute("svchost dockerd exec " .. container_id .. " " .. command)
    print(result or "")
    
elseif arg[1] == "stop" then
    local target = arg[2] or "all"
    local result = os.execute("svchost dockerd stop " .. target)
    print(result or "")
    
elseif arg[1] == "rm" then
    local target = arg[2] or "all"
    local result = os.execute("svchost dockerd rm " .. target)
    print(result or "")
    
elseif arg[1] == "logs" then
    if not arg[2] then
        print("Error: Container ID required")
        print("Usage: docker logs CONTAINER_ID")
        return
    end
    
    local result = os.execute("svchost dockerd logs " .. arg[2])
    print(result or "")
    
else
    print("docker: '" .. arg[1] .. "' is not a docker command.")
    print("See 'docker help'")
end

-- Inicializar daemon se não estiver rodando
if arg[1] ~= "--deamon" and not arg[1]:match("^%-") then
    local daemon_status = os.execute("trace check dockerd")
    if daemon_status ~= "true" then
        print("Starting docker daemon...")
        os.execute("start dockerd")
        os.execute("sleep 1") -- Dar tempo para o daemon inicializar
    end
end