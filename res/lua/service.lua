return function (payload, args, scope, pid, uid)
    print("---")
    print("Debugger Service")
    print("* Payload: " .. payload)
    print("* Source: " .. args)
    print("* Request from (PID): " .. pid)
    print("* Who calls? (UID): " .. uid)
    print("---")

    if payload == "ping" then
        print("pong")
    elseif payload == nil then
        print("nothing to process")
    elseif type(payload) == "function" then
        payload()
    end
end