return function (payload, source, pid, id)
    -- payload: requistion for the service (on Lua can be any value, from shell only strings)
    -- source: [lua, mod, shell] where this request was called?
    -- pid: process id of who make this request
    -- id: user id of who make this request

    print("---")
    print("Debugger Service")
    print("* Payload: " .. payload)
    print("* Source: " .. source)
    print("* Request from (PID): " .. pid)
    print("* Who calls? (UID): " .. id)
    print("---")

    if payload == "ping" then
        print("pong")
    elseif payload == nil then
        print("nothing to process")
    elseif type(payload) == "function" then
        payload()
    end
end